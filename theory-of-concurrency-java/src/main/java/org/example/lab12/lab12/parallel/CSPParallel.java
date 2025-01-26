package org.example.lab12.parallel;


import org.jcsp.lang.*;

import java.util.Arrays;


class Buffer implements CSProcess {
    private final One2OneChannelInt in;
    private final One2OneChannelInt out;
    private final One2OneChannelInt jeszcze;

    public Buffer(One2OneChannelInt in,
                  One2OneChannelInt out,
                  One2OneChannelInt jeszcze) {
        this.out = out;
        this.in = in;
        this.jeszcze = jeszcze;
    }

    public void run() {
        while (true) {
            jeszcze.out().write(0);
            out.out().write(in.in().read());
        }
    }
}

class Producer implements CSProcess {
    private final One2OneChannelInt[] out;
    private final One2OneChannelInt[] jeszcze;
    private final int N;

    public Producer(One2OneChannelInt[] out, One2OneChannelInt[] jeszcze, int N) {
        this.out = out;
        this.jeszcze = jeszcze;
        this.N = N;
    }

    public void run() {
        var guards = Arrays.stream(jeszcze).map(c -> c.in()).toArray(Guard[]::new);
        var alternative = new Alternative(guards);
        for (int i = 0; i < N; i++) {
            var index = alternative.select();
            jeszcze[index].in().read();
            var item = (int) (Math.random() * 100) + 1;
            out[index].out().write(item);
        }
    }
}

class Consumer implements CSProcess {
    private final One2OneChannelInt[] in;
    private final int N;

    public Consumer(final One2OneChannelInt[] in, int n) {
        this.in = in;
        this.N = n;
    }

    public void run() {
        var start = System.currentTimeMillis();

        var guards = Arrays.stream(in).map(c -> c.in()).toArray(Guard[]::new);
        var alt = new Alternative(guards);
        for (int i = 0; i < N; i++) {
            int index = alt.select();
            int item = in[index].in().read();
        }

        var end = System.currentTimeMillis();
        System.out.printf("Time elapsed: %d ms\n", end - start);
        System.exit(0);
    }
}


public class CSPParallel {
    public static void main(String[] args) {
        int bufferLength = 15;
        int itemsToProduce = 15000;

        var channelIntFactory = new StandardChannelIntFactory();
        var producerChannels = channelIntFactory.createOne2One(bufferLength);
        var consumerChannels = channelIntFactory.createOne2One(bufferLength);
        var jeszczeChannels = channelIntFactory.createOne2One(bufferLength);

        var procList = new CSProcess[bufferLength + 2];
        procList[0] = new Producer(producerChannels, jeszczeChannels, itemsToProduce);
        procList[1] = new Consumer(consumerChannels, itemsToProduce);
        for (int i = 0; i < bufferLength; i++) {
            procList[i + 2] = new Buffer(producerChannels[i], consumerChannels[i], jeszczeChannels[i]);
        }


        new Parallel(procList).run();
    }
}
