package org.example.lab2;


// Race2.java
// wyscig

import org.example.common.BinarySemaphore;
import org.example.common.CountingSemaphore;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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

class IThread extends Thread {
    private final Counter _cnt;
    private final BinarySemaphore _sem;

    public IThread(Counter c, BinarySemaphore s) {
        _cnt = c;
        _sem = s;
    }

    public void run() {
        for (int i = 0; i < 100000000; ++i) {
            _sem.P();
            _cnt.inc();
            _sem.V();
        }
    }
}

class DThread extends Thread {
    private final Counter _cnt;
    private final BinarySemaphore _sem;

    public DThread(Counter c, BinarySemaphore s) {
        _cnt = c;
        _sem = s;
    }

    public void run() {
        for (int i = 0; i < 100000000; ++i) {
            _sem.P();
            _cnt.dec();
            _sem.V();
        }
    }
}


// implement CountingSemaphore using BinarySemaphores


class OpenNetworkSocketThread extends Thread {
    private final CountingSemaphore _sem;
    private final int id;

    public OpenNetworkSocketThread(int id, CountingSemaphore s) {
        this.id = id;
        _sem = s;
    }

    public void run() {
        _sem.P();
        System.out.println("Opened network socket in " + id);
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Closing network socket in " + id);
        _sem.V();
    }
}

class Race2 {
    public static void main(String[] args) {
        var sem = new CountingSemaphore(3);
        List<OpenNetworkSocketThread> threads =
                IntStream.range(0, 10)
                        .mapToObj(i -> new OpenNetworkSocketThread(i, sem))
                        .toList();

        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}