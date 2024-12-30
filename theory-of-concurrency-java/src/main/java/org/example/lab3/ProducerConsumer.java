package org.example.lab3;

import org.example.common.BinarySemaphore;
import org.example.common.CountingSemaphore;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Producer extends Thread {
    private final BufferUsingSemaphores buffer;
    private final int numberOfIterations;
    private final int delayMs;

    public Producer(int delayMs,
                    BufferUsingSemaphores buffer, int numberOfIterations) {
        this.buffer = buffer;
        this.numberOfIterations = numberOfIterations;
        this.delayMs = delayMs;
    }


    public void run() {
        for (int i = 0; i < numberOfIterations; ++i) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.put(i);
        }
    }
}

class Consumer extends Thread {
    private final BufferUsingSemaphores buffer;
    private final int numberOfIterations;
    private final int delayMs;

    public Consumer(
            int delayMs,
            BufferUsingSemaphores buffer, int numberOfIterations) {
        this.buffer = buffer;
        this.numberOfIterations = numberOfIterations;
        this.delayMs = delayMs;
    }

    public void run() {
        for (int i = 0; i < numberOfIterations; ++i) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.get();
        }
    }
}

class BufferUsingMonitor {
    private final int capacity;
    private final LinkedList<Integer> buffer = new LinkedList<>();

    public BufferUsingMonitor(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(int i) {
        while (buffer.size() >= capacity) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        buffer.add(i);
        notifyAll();
    }

    public synchronized int get() {
        while (buffer.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int element = buffer.removeFirst();
        notifyAll();
        return element;

    }

    public int size() {
        return buffer.size();
    }
}

class BufferUsingSemaphores {
    private final int capacity;
    private final LinkedList<Integer> buffer = new LinkedList<>();
    private final BinarySemaphore mutex = new BinarySemaphore();
    private final CountingSemaphore empty;
    private final CountingSemaphore full;

    public BufferUsingSemaphores(int capacity) {
        this.capacity = capacity;
        empty = new CountingSemaphore(capacity);
        full = new CountingSemaphore(0);
    }

    public void put(int i) {
        empty.P();
        mutex.P();

        buffer.add(i);

        mutex.V();
        full.V();

    }

    public int get() {
//        System.out.println("called get");
        full.P();
        mutex.P();


//        System.out.println("about to remove first ");
        int result = buffer.removeFirst();

        mutex.V();
        empty.V();
        return result;
    }

    public int size() {
        return buffer.size();
    }
}

public class ProducerConsumer {
    public static void main(String[] args) {
//        int delay = 3;
//
//        System.out.println("Producer is as fast as consumer");
//
//        runTimed(1000, 1, delay, 1, delay);
//        runTimed(1000, 5, delay, 2, delay);
//        runTimed(1000, 2, delay, 5, delay);
//        runTimed(1000, 5, delay, 5, delay);
//
//        System.out.println("Producer is 3x slower than consumer");
//
//        runTimed(1000, 1, delay * 3, 1, delay);
//        runTimed(1000, 5, delay * 3, 2, delay);
//        runTimed(1000, 2, delay * 3, 5, delay);
//        runTimed(1000, 5, delay * 3, 5, delay);

        run(1, 1);
        run(5, 2);
        run(2, 5);
        run(5, 5);
    }

    public static void runTimed(
            int products,
            int producers,
            int producersDelay,
            int consumers,
            int consumersDelay) {
        int producersIterations = products / producers;
        int consumersIterations = products / consumers;

        System.out.println("Products = " + products);
        System.out.printf("Producers = %s, Consumers = %s, Total Products = %s %n", producers, consumers, products);
        System.out.printf("One producers produces = %s items, One consumer consumes = %s item%n",
                producersIterations, consumersIterations);

        BufferUsingSemaphores buffer = new BufferUsingSemaphores(10);
        List<? extends Thread> threads = Stream.concat(
                        IntStream
                                .range(0, producers)
                                .mapToObj(i -> new Producer(producersDelay, buffer, producersIterations)),
                        IntStream
                                .range(0, consumers)
                                .mapToObj(i -> new Consumer(consumersDelay, buffer, consumersIterations)))
                .toList();

        long startTime = System.currentTimeMillis();

        threads.forEach(Thread::start);
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        System.out.println();
    }

    public static void run(int producers, int consumers) {
        int products = producers * 20;
        int producersIterations = products / producers;
        int consumersIterations = products / consumers;

        System.out.printf("Producers = %s, Consumers = %s, Total Products = %s %n", producers, consumers, products);
        System.out.printf("One producers produces = %s items, One consumer consumes = %s item%n",
                producersIterations, consumersIterations);


        BufferUsingSemaphores buffer = new BufferUsingSemaphores(10);
        List<? extends Thread> threads = Stream.concat(
                        IntStream.range(0, producers).mapToObj(i -> new Producer(0, buffer, producersIterations)),
                        IntStream.range(0, consumers).mapToObj(i -> new Consumer(0, buffer, consumersIterations)))
                .toList();

        threads.forEach(Thread::start);
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("All done");
        System.out.println("Buffer size: " + buffer.size());
        System.out.println();
    }
}
