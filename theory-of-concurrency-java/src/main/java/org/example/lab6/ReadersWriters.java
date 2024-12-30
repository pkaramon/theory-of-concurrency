package org.example.lab6;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

interface Library {
    void read() throws InterruptedException;

    void write() throws InterruptedException;
}

class LibraryWithStats implements Library {
    private final Library library;
    private long totalReadTime = 0;
    private long totalWriteTime = 0;
    private long totalReads = 0;
    private long totalWrites = 0;

    public LibraryWithStats(Library library) {
        this.library = library;
    }

    @Override
    public void read() throws InterruptedException {
        long start = System.nanoTime();
        library.read();
        totalReadTime += System.nanoTime() - start;
        totalReads++;
    }

    @Override
    public void write() throws InterruptedException {
        long start = System.nanoTime();
        library.write();
        totalWriteTime += System.nanoTime() - start;
        totalWrites++;
    }

    public long getAvgReadTimeMicro() {
        return totalReadTime / totalReads / 1_000;
    }

    public long getAvgWriteTimeMicro() {
        return totalWriteTime / totalWrites / 1_000;
    }
}

class SemaphoreLibrary implements Library {
    private final Semaphore readCountMutex = new Semaphore(1);
    private final Semaphore resource = new Semaphore(1);
    private int readCount = 0;


    @Override
    public void read() throws InterruptedException {
        readCountMutex.acquire();
        readCount++;
        if (readCount == 1) {
            resource.acquire();
        }
        readCountMutex.release();

        // reading
        Thread.sleep(1);

        readCountMutex.acquire();
        readCount--;
        if (readCount == 0) {
            resource.release();
        }
        readCountMutex.release();
    }

    @Override
    public void write() throws InterruptedException {
        resource.acquire();

        // writing
        Thread.sleep(1);

        resource.release();
    }
}

class ConditionVariablesLibrary implements Library {
    private final Lock lock = new ReentrantLock();
    private final Condition canRead = lock.newCondition();
    private final Condition canWrite = lock.newCondition();
    private int readCount = 0;
    private int writersWaiting = 0;
    private boolean isWriting = false;

    @Override
    public void read() throws InterruptedException {
        lock.lock();
        try {
            while (isWriting || writersWaiting > 0) {
                canRead.await();
            }
            readCount++;
        } finally {
            lock.unlock();
        }

        // reading
        Thread.sleep(1);

        lock.lock();
        try {
            readCount--;
            if (readCount == 0) {
                canWrite.signal();
            }
        } finally {
            lock.unlock();

        }
    }

    @Override
    public void write() throws InterruptedException {
        lock.lock();
        try {
            writersWaiting++;
            while (isWriting || readCount > 0) {
                canWrite.await();
            }
            writersWaiting--;
            isWriting = true;
        } finally {
            lock.unlock();
        }

        // writing
        Thread.sleep(1);

        try {
            lock.lock();
            isWriting = false;
            if (writersWaiting > 0) {
                canWrite.signal();
            } else {
                canRead.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}

class Reader implements Runnable {
    private final Library library;
    private final int id;
    private final int amountOfReads;

    public Reader(Library library, int id, int amountOfReads) {
        this.library = library;
        this.id = id;
        this.amountOfReads = amountOfReads;
    }

    @Override
    public void run() {
        for (int i = 0; i < amountOfReads; i++) {
            try {
                library.read();
//                System.out.println("Reader id " + id + " read " + i + " times");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Writer implements Runnable {
    private final Library library;
    private final int id;
    private final int amountOfWrites;

    public Writer(Library library, int id, int amountOfWrites) {
        this.library = library;
        this.id = id;
        this.amountOfWrites = amountOfWrites;
    }

    @Override
    public void run() {
        for (int i = 0; i < amountOfWrites; i++) {
            try {
                library.write();
//                System.out.println("Writer id " + id + " wrote " + i + " times");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class ReadersWriters {
    public static void main(String[] args) {
        System.out.println("numberOfReaders,numberOfWriters,semaphoreTime,conditionVariablesTime,semaphoreReadTime,semaphoreWriteTime,conditionVariablesReadTime,conditionVariablesWriteTime");

        for (int i = 1; i <= 100; i++) {
            for (int j = 1; j <= 10; j++) {
                long semaphoreTimes = 0;
                long conditionVariablesTime = 0;
                long semaphoreReadTime = 0;
                long semaphoreWriteTime = 0;
                long conditionVariablesReadTime = 0;
                long conditionVariablesWriteTime = 0;

                final long N = 3;

                for (int k = 0; k < N; k++) {
                    var semLib = new LibraryWithStats(new SemaphoreLibrary());
                    var condLib = new LibraryWithStats(new ConditionVariablesLibrary());
                    semaphoreTimes += testCase(i, j, semLib);
                    conditionVariablesTime += testCase(i, j, condLib);
                    semaphoreReadTime += semLib.getAvgReadTimeMicro();
                    semaphoreWriteTime += semLib.getAvgWriteTimeMicro();
                    conditionVariablesReadTime += condLib.getAvgReadTimeMicro();
                    conditionVariablesWriteTime += condLib.getAvgWriteTimeMicro();
                }

                System.out.printf("%d,%d,%d,%d,%d,%d,%d,%d%n",
                        i,
                        j,
                        semaphoreTimes / N,
                        conditionVariablesTime / N,
                        semaphoreReadTime / N,
                        semaphoreWriteTime / N,
                        conditionVariablesReadTime / N,
                        conditionVariablesWriteTime / N
                );

            }
        }
    }

    private static long avg(List<Long> times) {
        return (long) times.stream().mapToDouble(Long::doubleValue).average().orElseThrow();
    }


    private static long testCase(int amountOfReaders, int amountOfWriters, Library library) {
        var readers = IntStream.range(0, amountOfReaders)
                .mapToObj(i -> new Reader(library, i, 10))
                .toList();
        var writers = IntStream.range(0, amountOfWriters)
                .mapToObj(i -> new Writer(library, i, 10))
                .toList();

        try (var executor = Executors.newFixedThreadPool(amountOfReaders + amountOfWriters)) {
            readers.forEach(executor::submit);
            writers.forEach(executor::submit);

            long startTime = System.currentTimeMillis();
            executor.shutdown();
            if (executor.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES)) {
                long endTime = System.currentTimeMillis();
                return endTime - startTime;
            } else {
                executor.shutdownNow();
                throw new RuntimeException("Executor did not finish in time");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
