package org.example.lab7;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


class Buffer {
    private final int capacity;
    private final List<Integer> buffer = new LinkedList<>();

    public Buffer(int capacity) {
        this.capacity = capacity;
    }

    public void put(int value) {
        buffer.add(value);
    }

    public int get() {
        return buffer.removeFirst();
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public boolean isFull() {
        return buffer.size() == capacity;
    }
}

abstract class MethodRequest<T> {
    protected final Buffer buffer;
    protected final CompletableFuture<T> future = new CompletableFuture<>();

    public MethodRequest(Buffer buffer) {
        this.buffer = buffer;
    }

    public abstract void execute();

    public abstract boolean guard();
}


class PutRequest extends MethodRequest<Void> {
    private final int value;

    public PutRequest(Buffer buffer, int value) {
        super(buffer);
        this.value = value;
    }

    @Override
    public void execute() {
        System.out.println("PUT REQUEST " + value);
        buffer.put(value);
        future.complete(null);
    }

    @Override
    public boolean guard() {
        return !buffer.isFull();
    }
}

class GetRequest extends MethodRequest<Integer> {
    public GetRequest(Buffer buffer) {
        super(buffer);
    }

    @Override
    public void execute() {
        System.out.println("GET REQUEST");
        future.complete(buffer.get());
    }

    @Override
    public boolean guard() {
        return !buffer.isEmpty();
    }
}

class Scheduler {
    private final Queue<MethodRequest<?>> requests = new ConcurrentLinkedQueue<>();

    public void enqueue(MethodRequest<?> request) {
        requests.add(request);
    }

    public void run() {
        while (true) {
            MethodRequest<?> request = requests.poll();
            if (request != null) {
                if (request.guard()) {
                    request.execute();
                } else {
                    System.out.println("Request is not ready, re-enqueueing");
                    requests.add(request);
                }
            }
        }
    }
}

class BufferProxy {
    private final Scheduler scheduler = new Scheduler();
    private final Buffer buffer;

    public BufferProxy(int capacity) {
        this.buffer = new Buffer(capacity);

        var thread = new Thread(scheduler::run);
        thread.setDaemon(true);
        thread.start();
    }

    public CompletableFuture<Void> put(int value) {
        var request = new PutRequest(buffer, value);
        scheduler.enqueue(request);
        return request.future;
    }

    public CompletableFuture<Integer> get() {
        var request = new GetRequest(buffer);
        scheduler.enqueue(request);
        return request.future;
    }
}

class Producer implements Runnable {
    private final int toProduce;
    private final BufferProxy buffer;
    private final int id;

    public Producer(int id, int toProduce, BufferProxy buffer) {
        this.id = id;
        this.toProduce = toProduce;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < toProduce; i++) {
            futures.add(buffer.put(i));

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("Producer %d finished sending requests%n", id);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.printf("Producer %d: all actions have been done%n", id);
    }
}

class Consumer implements Runnable {
    private final int toConsume;
    private final BufferProxy buffer;
    private final int id;

    public Consumer(int id, int toConsume, BufferProxy buffer) {
        this.id = id;
        this.toConsume = toConsume;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < toConsume; i++) {
            futures.add(buffer.get());

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("Consumer %d finished sending requests%n", id);

        var results = futures.stream().map(CompletableFuture::join).map(String::valueOf).collect(Collectors.joining(", "));
        System.out.printf("Consumer %d: received values: %s%n", id, results);

        System.out.printf("Consumer %d: all actions have been done%n", id);
    }

}

public class ActiveObjectPattern {
public static void main(String[] args) throws ExecutionException, InterruptedException {
    BufferProxy buffer = new BufferProxy(5);

    var producers = IntStream.range(0, 3)
            .mapToObj(i -> new Producer(i, 10, buffer))
            .map(Thread::new)
            .toList();
    var consumers = IntStream.range(0, 2)
            .mapToObj(i -> new Consumer(i, 15, buffer))
            .map(Thread::new)
            .toList();

    try (var executor = Executors.newFixedThreadPool(7)) {
        producers.forEach(executor::submit);
        consumers.forEach(executor::submit);

        executor.shutdown();
        boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
        if (terminated) {
            System.out.println("All threads have finished");
        } else {
            System.out.println("Program has timed out");
        }
    }
}
}
