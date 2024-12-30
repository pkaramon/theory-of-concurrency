package org.example.common;


public class CountingSemaphore {
    private final BinarySemaphore gate;
    private final BinarySemaphore mutex;
    private int counter;

    public CountingSemaphore(int n) {
        mutex = new BinarySemaphore();
        gate = new BinarySemaphore();
        counter = n;
        if (counter == 0) {
            gate.P();
        }
    }

    public void P() {
        gate.P();
        mutex.P();
        counter--;
        if (counter > 0) {
            gate.V();
        }
        mutex.V();
    }

    public void V() {
        mutex.P();
        counter++;
        if (counter >= 1) {
            gate.V();
        }
        mutex.V();
    }
}
