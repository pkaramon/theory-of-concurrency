package org.example.lab4;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

interface Buffer {
    void put(Collection<Integer> values) throws InterruptedException;

    Collection<Integer> get(int howMany) throws InterruptedException;

    void removeProducer();

    void removeConsumer();
}


class BufferUsingUtils implements Buffer {
    private final int capacity;
    private final List<Integer> buffer;
    private final Lock lock;
    private final Condition notFull;
    private final Condition notEmpty;
    private int producers;
    private int consumers;

    public BufferUsingUtils(int M, int m, int n) {
        this.capacity = 2 * M;
        this.buffer = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
        this.producers = m;
        this.consumers = n;
    }

    @Override
    public void put(Collection<Integer> values) throws InterruptedException {
        lock.lock();
        try {
            while (buffer.size() + values.size() > capacity) {
                if (consumers == 0) {
                    throw new InterruptedException();
                }
                notFull.await();
            }
            buffer.addAll(values);
//            System.out.println("Producer produced " + values);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<Integer> get(int howMany) throws InterruptedException {
        lock.lock();
        try {
            while (buffer.size() < howMany) {
                if (producers == 0) {
                    throw new InterruptedException();
                }
                notEmpty.await();
            }
            List<Integer> result = new LinkedList<>();
            for (int i = 0; i < howMany; i++) {
                result.add(buffer.removeFirst());
            }
//            System.out.println("Consumer consumed" + result);
            notFull.signalAll();
            return result;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void removeProducer() {
        lock.lock();
        try {
            producers--;
            if (producers == 0) {
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeConsumer() {
        lock.lock();
        try {
            consumers--;
            if (consumers == 0) {
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}

class BufferUsingMonitors implements Buffer {
    private final int capacity;
    private final List<Integer> buffer;
    private int producers;
    private int consumers;

    public BufferUsingMonitors(int M, int m, int n) {
        this.buffer = new LinkedList<>();
        this.capacity = 2 * M;
        producers = m;
        consumers = n;
    }

    @Override
    public synchronized void put(Collection<Integer> values) throws InterruptedException {
        while (buffer.size() + values.size() > capacity) {
            if (consumers == 0) {
                throw new InterruptedException();
            }
            wait();
        }

        buffer.addAll(values);
//        System.out.println("Producer produced " + values);
        notifyAll();
    }

    @Override
    public synchronized Collection<Integer> get(int howMany) throws InterruptedException {
        while (buffer.size() < howMany) {
            if (producers == 0) {
                throw new InterruptedException();
            }
            wait();
        }


        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < howMany; i++) {
            result.add(buffer.removeFirst());
        }

//        System.out.println("Consumer consumed" + result);
        notifyAll();
        return result;
    }


    @Override
    public synchronized void removeProducer() {
        producers--;
        if (producers == 0) {
            notifyAll();
        }
    }

    @Override
    public synchronized void removeConsumer() {
        consumers--;
        if (consumers == 0) {
            notifyAll();
        }
    }
}

class Producer implements Runnable {
    private final Buffer buffer;
    private final int iterations;
    private final int M;

    public Producer(Buffer buffer, int M, int iterations) {
        this.buffer = buffer;
        this.M = M;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            Collection<Integer> values = IntStream
                    .range(0, ThreadLocalRandom.current().nextInt(1, M + 1))
                    .boxed()
                    .toList();

            try {
                buffer.put(values);
            } catch (InterruptedException e) {
                break;
            }

        }

        buffer.removeProducer();
    }
}

class Consumer implements Runnable {
    private final Buffer buffer;
    private final int iterations;
    private final int M;

    public Consumer(Buffer buffer, int M, int iterations) {
        this.buffer = buffer;
        this.M = M;
        this.iterations = iterations;
    }

    @Override
    public void run() {
        for (int i = 0; i < iterations; i++) {
            try {
                buffer.get(ThreadLocalRandom.current().nextInt(1, M + 1));
            } catch (InterruptedException e) {
                break;
            }
        }

        buffer.removeConsumer();
    }
}

public class ProducerConsumerRandomized {
    public static void main(String[] args) {
        int amountOfRuns = 15;

        List<Integer> ms = List.of(3, 9, 3, 12, 20, 1, 20);
        List<Integer> ns = List.of(3, 3, 9, 15, 20, 20, 1);
        List<Integer> Ms = List.of(5, 10, 20, 50, 100);

        System.out.println("M,m,n,avgTimeUsingMonitors,avgTimeUsingUtils");
        for (int i = 0; i < ms.size(); i++) {
            for (var M : Ms) {
                var m = ms.get(i);
                var n = ns.get(i);

                Supplier<Buffer> bufferSupplierUsingMonitors = () -> new BufferUsingMonitors(M, m, n);
                Supplier<Buffer> bufferSupplierUsingUtils = () -> new BufferUsingUtils(M, m, n);
                long avgTimeUsingMonitors = avgTestRun(amountOfRuns, M, m, n, bufferSupplierUsingMonitors);
                long avgTimeUsingUtils = avgTestRun(amountOfRuns, M, m, n, bufferSupplierUsingUtils);
                System.out.println(M + ", " + m + ", " + n + ", " + avgTimeUsingMonitors + ", " + avgTimeUsingUtils);
            }
        }
    }


    private static long avgTestRun(int amountOfRuns, int M, int m, int n, Supplier<Buffer> bufferSupplier) {
        long sum = 0;
        for (int i = 0; i < amountOfRuns; i++) {
            long res = testRun(M, m, n, bufferSupplier.get());
            sum += res;
        }
        return Math.round((double) sum / (double) amountOfRuns);
    }


    private static long testRun(int M, int m, int n, Buffer buffer) {
        var producers = IntStream.range(0, m)
                .mapToObj(i -> new Producer(buffer, M, 500))
                .map(Thread::new)
                .toList();

        var consumers = IntStream.range(0, n)
                .mapToObj(i -> new Consumer(buffer, M, 500))
                .map(Thread::new)
                .toList();

        long startTime = System.currentTimeMillis();

        producers.forEach(Thread::start);
        consumers.forEach(Thread::start);
        Stream.concat(producers.stream(), consumers.stream())
                .forEach(t -> {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }


}
