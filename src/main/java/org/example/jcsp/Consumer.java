package org.example.jcsp;
import org.jcsp.lang.*;
import java.util.Random;

class Consumer implements CSProcess {
    private final ChannelOutput<BufferMessage>[] toBuffers;
    private final AltingChannelInput<BufferMessage>[] fromBuffers;
    private final int consumerId;
    private final Random random = new Random();
    private volatile boolean running = true;

    public Consumer(ChannelOutput<BufferMessage>[] toBuffers,
                    AltingChannelInput<BufferMessage>[] fromBuffers,
                    int consumerId) {
        this.toBuffers = toBuffers;
        this.fromBuffers = fromBuffers;
        this.consumerId = consumerId;
    }
    public static int getNext(int max) {
        Random rand = new Random();
        return (int) (Math.log(1-rand.nextDouble())/(-0.5) % max);
    }

    @Override
    public void run() {
        System.out.println("Consumer " + consumerId + " STARTED.");
        int requestId = 0;

        while (running) {
            // 1. Losowa przerwa (symulacja konsumpcji)
            try {
                Thread.sleep(random.nextInt(1000) + 500); // 0.5s - 1.5s
            } catch (InterruptedException e) {
            }

            // 2. Wybór losowego bufora
            requestId++;
            boolean success = false;
            BufferMessage response = null;
            int targetBuf = -1;
            while (!success) {
                //targetBuf = getNext(toBuffers.length);
                targetBuf = random.nextInt(toBuffers.length);

                // 3. Wysłanie żądania
                BufferMessage req = new BufferMessage(BufferMessage.Type.GET_REQUEST, -1, requestId, false);
                toBuffers[targetBuf].write(req);

                // 4. Odbiór odpowiedzi (czekamy na ten konkretny bufor)
                response = fromBuffers[targetBuf].read();

                // 5. Obsługa wyniku
                success = response.type == BufferMessage.Type.GET_RESPONSE ? response.success : false;
                if (!success) {
                    //System.out.println("Cons " + consumerId + ": REJECTED (Empty) @ Buf " + targetBuf);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
                //System.out.println("Cons " + consumerId + ": ATE " + response.data + " from Buf " + targetBuf);
            }
    }
    public void stop() {
        running = false;
    }
}