package org.example.lab5;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

class Fork {
    private final Semaphore _semaphore = new Semaphore(1);

    public void take() {
        try {
            _semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean tryTake() {
        return _semaphore.tryAcquire();
    }

    public void put() {
        _semaphore.release();
    }
}

class Butler {
    private final Semaphore _semaphore = new Semaphore(4);

    public void acquire() {
        try {
            _semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void release() {
        _semaphore.release();
    }
}

class Philosopher extends Thread {
    private final Fork _left;
    private final Fork _right;
    private final int _id;
    private final Butler _butler;
    private int _counter = 0;
    private long waitingTime = 0;

    public Philosopher(int id, Fork left, Fork right
            ,
                       Butler butler
    ) {
        _id = id;
        _left = left;
        _right = right;
        _butler = butler;
    }

    public void run() {
        while (_counter < 100) {
            think();
            eat();
        }
    }

    private void think() {
        try {
            sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void eat() {
        _counter++;


        long startTime = System.currentTimeMillis();
        _butler.acquire();
        _left.take();
        _right.take();

        long endTime = System.currentTimeMillis();
        waitingTime += endTime - startTime;

        _counter++;
        try {
            sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        _right.put();
        _left.put();
        _butler.release();

//        long startTime = System.currentTimeMillis();
//        do {
//            if (_left.tryTake()) {
//                if (_right.tryTake()) {
//                    break;
//                } else {
//                    _left.put();
//                }
//            }
//        } while (true);
//
//        long endTime = System.currentTimeMillis();
//        waitingTime += endTime - startTime;
//
//        try {
//            sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        _right.put();
//        _left.put();


//        long startTime = System.currentTimeMillis();
//        _left.take();
//        _right.take();
//
//        long endTime = System.currentTimeMillis();
//        waitingTime += endTime - startTime;
//
//        try {
//            sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        _right.put();
//        _left.put();
    }

    public long getWaitingTime() {
        return waitingTime;
    }
}

public class Fil5mon {
    public static void main(String[] args) {
        var times = new ArrayList<List<Long>>();
        IntStream.range(0, 10).forEach(i -> times.add(testCase()));

        var avgTimes = new ArrayList<Long>();
        for (int i = 0; i < 5; i++) {
            long sum = 0;
            for (List<Long> time : times) {
                sum += time.get(i);
            }
            avgTimes.add(sum / times.size());
        }

        IntStream.range(1, 6).forEach(i -> System.out.printf("%d, %d ms%n", i, avgTimes.get(i - 1)));
    }

    private static List<Long> testCase() {
        int N = 5;
        Fork[] forks = new Fork[N];
        Philosopher[] philosophers = new Philosopher[N];
        for (int i = 0; i < N; i++) {
            forks[i] = new Fork();
        }
        Butler butler = new Butler();
        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i + 1, forks[i], forks[(i + 1) % N], butler);
        }

        var executor = Executors.newFixedThreadPool(N);
        Arrays.stream(philosophers).forEach(executor::submit);
        executor.shutdown();

        try {
            executor.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Arrays.stream(philosophers).map(Philosopher::getWaitingTime).toList();
    }

}
