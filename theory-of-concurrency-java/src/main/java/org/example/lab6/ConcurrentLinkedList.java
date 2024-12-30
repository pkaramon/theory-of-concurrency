package org.example.lab6;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

interface MyList {
    boolean remove(Object value);

    boolean contains(Object value);

    void add(Object value);

    List<Object> toList();
}

class Node {
    Object value;
    Node next;
    Lock lock = new ReentrantLock();

    public Node(Object value) {
        this(value, null);
    }

    public Node(Object value, Node next) {
        this.value = value;
        this.next = next;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}

class SingleLockList implements MyList {
    private final Node head = new Node(null, null);
    private final int compareTime;
    private final int insertTime;

    public SingleLockList(int compareTime, int insertTime) {
        this.compareTime = compareTime;
        this.insertTime= insertTime;
    }

    @Override
    public synchronized boolean remove(Object value) {
        Node pred = head;
        Node curr = head.getNext();
        while (curr != null) {
            try {
                Thread.sleep(compareTime);
            } catch (Exception ignored) {
            }

            if (curr.getValue().equals(value)) {
                pred.setNext(curr.getNext());
                return true;
            }

            pred = curr;
            curr = curr.getNext();
        }
        return false;
    }

    @Override
    public synchronized boolean contains(Object value) {
        Node curr = head.getNext();
        while (curr != null) {
            try {
                Thread.sleep(compareTime);
            } catch (Exception ignored) {
            }

            if (curr.getValue().equals(value)) {
                return true;
            }

            curr = curr.getNext();
        }
        return false;
    }

    @Override
    public synchronized void add(Object value) {
        Node last = head;
        while (last.getNext() != null) {
            last = last.getNext();
        }

        try {
            Thread.sleep(insertTime);
        } catch (Exception ignored) {
        }

        last.setNext(new Node(value));
    }

    @Override
    public List<Object> toList() {
        List<Object> list = new ArrayList<>();
        Node curr = head.getNext();
        while (curr != null) {
            list.add(curr.getValue());
            curr = curr.getNext();
        }
        return list;
    }
}

class LockPerElementList implements MyList {
    private final Node head = new Node(null, null);
    private final int compareTime;
    private final int insertTime;

    public LockPerElementList(int compareTime, int insertTime) {
        this.compareTime = compareTime;
        this.insertTime = insertTime;
    }

    @Override
    public boolean remove(Object value) {
        Node prev = head;
        Node curr = null;
        try {
            prev.lock();
            curr = prev.getNext();
            while (curr != null) {
                curr.lock();
                try {
                    Thread.sleep(compareTime);
                } catch (Exception ignored) {
                }

                if (curr.getValue().equals(value)) {
                    prev.setNext(curr.getNext());
                    return true;
                }

                prev.unlock();
                prev = curr;
                curr = prev.getNext();
            }
        } finally {
            if (curr != null) {
                curr.unlock();
            }
            prev.unlock();
        }

        return false;
    }

    @Override
    public boolean contains(Object value) {
        Node prev = head;
        Node curr = null;

        try {
            prev.lock();
            curr = prev.getNext();
            while (curr != null) {
                curr.lock();
                try {
                    Thread.sleep(compareTime);
                } catch (Exception ignored) {
                }

                if (curr.getValue().equals(value)) {
                    return true;
                }

                prev.unlock();
                prev = curr;
                curr = prev.getNext();
            }
        } finally {
            if (curr != null) {
                curr.unlock();
            }
            prev.unlock();
        }
        return false;
    }

    @Override
    public void add(Object value) {
        Node curr = head;

        try {
            curr.lock();
            Node next = curr.getNext();


            while (next != null) {
                next.lock();

                curr.unlock();
                curr = next;
                next = curr.getNext();
            }

            try {
                Thread.sleep(insertTime);
            } catch (Exception ignored) {
            }

            curr.setNext(new Node(value));
        } finally {
            curr.unlock();
        }
    }

    @Override
    public List<Object> toList() {
        // no synchronization, because it's only for testing
        List<Object> list = new ArrayList<>();
        Node curr = head.getNext();
        while (curr != null) {
            list.add(curr.getValue());
            curr = curr.getNext();
        }
        return list;
    }

}

class Worker implements Runnable {
    private final MyList list;
    private final List<Character> operations;

    public Worker(MyList list, List<Character> operations) {
        this.list = list;
        this.operations = operations;
    }

    @Override
    public void run() {
        for (Character operation : operations) {
            switch (operation) {
                case 'a' -> list.add(ThreadLocalRandom.current().nextInt(30));
                case 'r' -> list.remove(ThreadLocalRandom.current().nextInt(30));
                case 'c' -> list.contains(ThreadLocalRandom.current().nextInt(30));
            }
        }

    }
}

public class ConcurrentLinkedList {
    public static void main(String[] args) {
        System.out.println("insertTime,compareTime,SingleLockList,LockPerElementList");

        for (int insertTime = 1; insertTime <= 10; insertTime++) {
            for (int compareTime = 1; compareTime <= 10; compareTime++) {
                var singleLockList = new SingleLockList(compareTime, insertTime);
                var lockPerElementList = new LockPerElementList(compareTime, insertTime);
                long singleLockListTime = avgTestCase(singleLockList);

                long lockPerElementListTime = avgTestCase(lockPerElementList);

                System.out.println(insertTime + "," + compareTime + "," + singleLockListTime + "," + lockPerElementListTime);
            }
        }


    }


    private static long avgTestCase(MyList list) {
        final int amountOfTests = 3;
        long sum = 0;
        for (int i = 0; i < amountOfTests; i++) {
            sum += testCase(list);
        }
        return sum / amountOfTests;
    }

    private static long testCase(MyList list) {
        var threads = new ArrayList<Thread>();
        final int amountOfThreads = 4;
        final int amountOfOperations = 25;
        Random random = new Random();


        for (int i = 0; i < amountOfThreads; i++) {
            var operations = new ArrayList<Character>();
            for (int j = 0; j < amountOfOperations; j++) {
                final int operation = random.nextInt(3);
                switch (operation) {
                    case 0 -> operations.add('a');
                    case 1 -> operations.add('r');
                    case 2 -> operations.add('c');
                }
            }
            threads.add(new Thread(new Worker(list, operations)));
        }

        var startTime = System.currentTimeMillis();
        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        var endTime = System.currentTimeMillis();
        return endTime - startTime;
    }


}
