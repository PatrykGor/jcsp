package org.example.jcsp;

import org.jcsp.lang.*;

public class DistributedBufferSystem {
    private static final int NUM_BUFFERS = 3;
    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 2;
    private static final int BUFFER_CAPACITY = 5;
    private static final int ITEMS_PER_PRODUCER = 10;
    private static final int ITEMS_PER_CONSUMER = 10;

    public static void main(String[] args) {
        System.out.println("Starting Distributed Buffer System...");
        System.out.println("Buffers: " + NUM_BUFFERS +
                ", Producers: " + NUM_PRODUCERS +
                ", Consumers: " + NUM_CONSUMERS);

        // Create channels for communication
        One2OneChannel<BufferMessage>[][] bufferChannels = new One2OneChannel[NUM_BUFFERS][2];
        One2OneChannel<BufferMessage>[][] producerChannels = new One2OneChannel[NUM_PRODUCERS][2];
        One2OneChannel<BufferMessage>[][] consumerChannels = new One2OneChannel[NUM_CONSUMERS][2];

        // Initialize channels
        for (int i = 0; i < NUM_BUFFERS; i++) {
            bufferChannels[i] = Channel.one2oneArray(2);
        }
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            producerChannels[i] = Channel.one2oneArray(2);
        }
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumerChannels[i] = Channel.one2oneArray(2);
        }

        // Create processes
        CSProcess[] processes = new CSProcess[NUM_BUFFERS + NUM_PRODUCERS + NUM_CONSUMERS];
        int processIndex = 0;

        // Create buffers
        for (int i = 0; i < NUM_BUFFERS; i++) {
            processes[processIndex++] = new DistributedBuffer(
                    bufferChannels[i][0].in(),
                    bufferChannels[i][1].out(),
                    i,
                    BUFFER_CAPACITY
            );
        }

        // Create producers
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            ChannelOutput<BufferMessage>[] bufferInputs = new ChannelOutput[NUM_BUFFERS];
            for (int j = 0; j < NUM_BUFFERS; j++) {
                bufferInputs[j] = bufferChannels[j][0].out();
            }

            processes[processIndex++] = new Producer(
                    bufferInputs,
                    new AltingChannelInput[] { producerChannels[i][1].in() },
                    i,
                    ITEMS_PER_PRODUCER
            );
        }

        // Create consumers
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            ChannelOutput<BufferMessage>[] bufferInputs = new ChannelOutput[NUM_BUFFERS];
            for (int j = 0; j < NUM_BUFFERS; j++) {
                bufferInputs[j] = bufferChannels[j][0].out();
            }

            processes[processIndex++] = new Consumer(
                    bufferInputs,
                    new AltingChannelInput[] { consumerChannels[i][1].in() },
                    i,
                    ITEMS_PER_CONSUMER
            );
        }

        // Create and run the parallel system
        Parallel parallel = new Parallel(processes);
        parallel.run();

        System.out.println("Distributed Buffer System finished!");
    }
}
