package org.example.jcsp;

import org.jcsp.lang.*;

public class DistributedBufferSystem {
    private static final int NUM_BUFFERS = 3;
    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 2;
    private static final int BUFFER_CAPACITY = 5;

    public static void main(String[] args) {
        System.out.println("System: Starting Infinite Loop Simulation...");

        // 1. MACIERZE KANAŁÓW [ID_Procesu][ID_Bufora]
        One2OneChannel<BufferMessage>[][] prodReq = new One2OneChannel[NUM_PRODUCERS][NUM_BUFFERS];
        One2OneChannel<BufferMessage>[][] prodResp = new One2OneChannel[NUM_PRODUCERS][NUM_BUFFERS];

        One2OneChannel<BufferMessage>[][] consReq = new One2OneChannel[NUM_CONSUMERS][NUM_BUFFERS];
        One2OneChannel<BufferMessage>[][] consResp = new One2OneChannel[NUM_CONSUMERS][NUM_BUFFERS];

        // Inicjalizacja kanałów
        for (int b = 0; b < NUM_BUFFERS; b++) {
            for (int p = 0; p < NUM_PRODUCERS; p++) {
                prodReq[p][b] = Channel.one2one();
                prodResp[p][b] = Channel.one2one();
            }
            for (int c = 0; c < NUM_CONSUMERS; c++) {
                consReq[c][b] = Channel.one2one();
                consResp[c][b] = Channel.one2one();
            }
        }

        CSProcess[] processes = new CSProcess[NUM_BUFFERS + NUM_PRODUCERS + NUM_CONSUMERS];
        int index = 0;

        // 2. TWORZENIE BUFORÓW
        for (int b = 0; b < NUM_BUFFERS; b++) {
            AltingChannelInput<BufferMessage>[] inputs = new AltingChannelInput[NUM_PRODUCERS + NUM_CONSUMERS];
            ChannelOutput<BufferMessage>[] outputs = new ChannelOutput[NUM_PRODUCERS + NUM_CONSUMERS];

            int k = 0;
            for (int p = 0; p < NUM_PRODUCERS; p++) inputs[k++] = prodReq[p][b].in();
            for (int c = 0; c < NUM_CONSUMERS; c++) inputs[k++] = consReq[c][b].in();

            k = 0;
            for (int p = 0; p < NUM_PRODUCERS; p++) outputs[k++] = prodResp[p][b].out();
            for (int c = 0; c < NUM_CONSUMERS; c++) outputs[k++] = consResp[c][b].out();

            processes[index++] = new DistributedBuffer(inputs, outputs, b, BUFFER_CAPACITY);
        }

        // 3. TWORZENIE PRODUCENTÓW
        for (int p = 0; p < NUM_PRODUCERS; p++) {
            ChannelOutput<BufferMessage>[] toBuffers = new ChannelOutput[NUM_BUFFERS];
            AltingChannelInput<BufferMessage>[] fromBuffers = new AltingChannelInput[NUM_BUFFERS];

            for (int b = 0; b < NUM_BUFFERS; b++) {
                toBuffers[b] = prodReq[p][b].out();
                fromBuffers[b] = prodResp[p][b].in();
            }
            processes[index++] = new Producer(toBuffers, fromBuffers, p);
        }

        // 4. TWORZENIE KONSUMENTÓW
        for (int c = 0; c < NUM_CONSUMERS; c++) {
            ChannelOutput<BufferMessage>[] toBuffers = new ChannelOutput[NUM_BUFFERS];
            AltingChannelInput<BufferMessage>[] fromBuffers = new AltingChannelInput[NUM_BUFFERS];

            for (int b = 0; b < NUM_BUFFERS; b++) {
                toBuffers[b] = consReq[c][b].out();
                fromBuffers[b] = consResp[c][b].in();
            }
            processes[index++] = new Consumer(toBuffers, fromBuffers, c);
        }

        new Parallel(processes).run();
    }
}