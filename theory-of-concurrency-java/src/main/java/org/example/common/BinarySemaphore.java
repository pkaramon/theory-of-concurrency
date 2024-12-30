package org.example.common;

public class BinarySemaphore {
    private boolean isAvailable = true;
    private int waiting = 0;



    public BinarySemaphore() {
    }

    public synchronized void P() {
        while (!isAvailable) {
            waiting++;
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiting--;
        }
        isAvailable = false;

    }

    public synchronized void V() {
        isAvailable = true;
        if (waiting > 0) {
            notify();
        }
    }
}
