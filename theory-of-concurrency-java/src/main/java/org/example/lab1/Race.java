
package org.example.lab1;

// Race.java
// Wyscig

import java.util.stream.IntStream;

class Counter {
    private int _val;

    public Counter(int n) {
        _val = n;
    }

    public void inc() {
        _val++;
    }

    public void dec() {
        _val--;
    }

    public int value() {
        return _val;
    }
}

// Watek, ktory inkrementuje licznik 100.000 razy
class IThread extends Thread {
    private final Counter counter;

    public IThread(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100_000; i++) {
            while(true) {
                if (Race.turn == 0) {
                    counter.inc();
                    Race.turn = 1;
                    break;
                }
            }

        }
    }
}

// Watek, ktory dekrementuje licznik 100.000 razy
class DThread extends Thread {
    private final Counter counter;

    public DThread(Counter counter) {
        this.counter = counter;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100_000; i++) {
            while(true) {
                if (Race.turn == 1) {
                    counter.dec();
                    Race.turn = 0;
                    break;
                }
            }
        }
    }
}

public class Race {
    public static volatile int turn = 0;

    public static void main(String[] args) {
        var allZeros = IntStream.range(0, 200).map(i -> runSample()).allMatch(i -> i == 0);
        System.out.println("all zeros = "+allZeros);
    }

    private static int runSample() {
        Counter cnt = new Counter(0);

        Race.turn = 0;

        IThread incThread = new IThread(cnt);
        DThread decThread = new DThread(cnt);

        incThread.start();
        decThread.start();

        try {
            incThread.join();
            decThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cnt.value();
    }


    public static void first(String[] args) {
        Counter cnt = new Counter(0);

        IThread incThread = new IThread(cnt);
        DThread decThread = new DThread(cnt);

        incThread.start();
        decThread.start();

        try {
            incThread.join();
            decThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("stan=" + cnt.value());
    }
}
