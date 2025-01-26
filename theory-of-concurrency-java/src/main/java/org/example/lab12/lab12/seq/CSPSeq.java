package org.example.lab12.seq;

import org.jcsp.lang.CSProcess;
import org.jcsp.lang.One2OneChannelInt;
import org.jcsp.lang.Parallel;
import org.jcsp.lang.StandardChannelIntFactory;



class Buffer implements CSProcess {
    private final One2OneChannelInt in;
    private final One2OneChannelInt out;

    public Buffer(One2OneChannelInt in,
                  One2OneChannelInt out) {
        this.out = out;
        this.in = in;
    }

    public void run() {
        while (true) {
            out.out().write(in.in().read());
        }
    }
}

class Producer implements CSProcess {
    private final One2OneChannelInt out;
    private final int N;

    public Producer(One2OneChannelInt out, int n) {
        this.out = out;
        this.N = n;
    }

    public void run() {
        for (int i = 0; i < N; i++) {
            var item = (int) (Math.random() * 100) + 1;
            out.out().write(item);
        }
    }
}

class Consumer implements CSProcess {
    private final One2OneChannelInt in;
    private final int N;

    public Consumer(final One2OneChannelInt in, int n) {
        this.in = in;
        this.N = n;
    }

    public void run() {
        var start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            int item = in.in().read();
        }

        var end = System.currentTimeMillis();
        System.out.printf("Time elapsed: %d ms%n", end - start);
        System.exit(0);
    }
}

public class CSPSeq {

    public static void main(String[] args) {
        int bufferLength = 15;
        int itemsToProduce = 15000;

        var channelIntFactory = new StandardChannelIntFactory();
        var channels = channelIntFactory.createOne2One(bufferLength + 1);

        var procList = new CSProcess[bufferLength + 2];
        procList[0] = new Producer(channels[0], itemsToProduce);
        procList[1] = new Consumer(channels[bufferLength], itemsToProduce);
        for (int i = 0; i < bufferLength; i++) {
            procList[i + 2] = new Buffer(channels[i], channels[i + 1]);
        }

        new Parallel(procList).run();
    }
}
