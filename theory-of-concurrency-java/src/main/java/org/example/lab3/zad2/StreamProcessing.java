package org.example.lab3.zad2;

import org.example.common.BinarySemaphore;
import org.example.common.CountingSemaphore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamProcessing {
    public static void main(String[] args) {
        Buffer buffer = new Buffer(100, 5);

        var producer = new Producer(buffer);
        var transformer1 = new Transformer(0, buffer, x -> x + 1, 3);
        var transformer2 = new Transformer(1, buffer, x -> x * 2, 3);
        var transformer3 = new Transformer(2, buffer, x -> x * 3, 1);
        var transformer4 = new Transformer(3, buffer, x -> x - 100, 10);
        var transformer5 = new Transformer(4, buffer, x -> x + 10_000, 5);
        var consumer = new Consumer(buffer);

        List<Integer> expectedResults = IntStream.range(0, 500).map(
                x -> (x + 1) * 2 * 3 - 100 + 10_000
        ).boxed().toList();

        var threads = Stream.of(
                producer,
                transformer1,
                transformer2,
                transformer3,
                transformer4,
                transformer5,
                consumer
        ).map(Thread::new).toList();

        threads.forEach(Thread::start);
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("All done");
        System.out.println("Results are equal: " + consumer.results.equals(expectedResults));
    }
}

// each transformer thread will

class Buffer {
    private final int capacity;
    private final BinarySemaphore mutex;
    private final CountingSemaphore empty;
    private final List<CountingSemaphore> transformers;
    private final int[] transformersIndexes;

    private final int[] buffer;
    private int bufferHead = 0;
    private int bufferTail = 0;

    public Buffer(int capacity, int transformersCount) {
        this.capacity = capacity;
        buffer = new int[capacity];
        mutex = new BinarySemaphore();
        empty = new CountingSemaphore(capacity);
        transformers = IntStream.range(0, transformersCount + 1).mapToObj(i -> new CountingSemaphore(0)).toList();
        transformersIndexes = new int[transformersCount];
    }

    public void add(int x) {
        empty.P();
        mutex.P();

        buffer[bufferHead] = x;
        bufferHead = (bufferHead + 1) % capacity;

        mutex.V();
        transformers.getFirst().V();
    }

    public void transform(int transformerIndex, Function<Integer, Integer> transformation) {
        transformers.get(transformerIndex).P();

        int elementIndex = transformersIndexes[transformerIndex];
        buffer[elementIndex] = transformation.apply(buffer[elementIndex]);
        transformersIndexes[transformerIndex] = (elementIndex + 1) % capacity;

        transformers.get(transformerIndex + 1).V();
    }

    public int get() {
        transformers.getLast().P();
        mutex.P();

        int result = buffer[bufferTail];
        bufferTail = (bufferTail + 1) % capacity;

        mutex.V();
        empty.V();
        return result;
    }
}

class Producer implements Runnable {
    private final Buffer buffer;

    public Producer(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        for (int i = 0; i < 500; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.add(i);
        }
    }
}

class Transformer implements Runnable {
    private final Buffer buffer;
    private final Function<Integer, Integer> transformation;
    private final int transformerIndex;
    private final int delayMs;

    public Transformer(
            int transformerIndex,
            Buffer buffer,
            Function<Integer, Integer> transformation,
            int delayMs) {
        this.buffer = buffer;
        this.transformerIndex = transformerIndex;
        this.transformation = transformation;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        for (int i = 0; i < 500; i++) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.transform(transformerIndex, transformation);
        }
    }
}

class Consumer implements Runnable {
    public final List<Integer> results = new ArrayList<>();
    private final Buffer buffer;

    public Consumer(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        for (int i = 0; i < 500; i++) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int result = buffer.get();
            results.add(result);
        }
    }
}