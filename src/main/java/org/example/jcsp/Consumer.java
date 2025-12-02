package org.example.jcsp;
import org.jcsp.lang.*;
import java.util.Random;

class Consumer implements CSProcess {
    private final ChannelOutput<BufferMessage>[] toBuffers;
    private final AltingChannelInput<BufferMessage>[] fromBuffers;
    private final int consumerId;
    private final Random random = new Random();

    public Consumer(ChannelOutput<BufferMessage>[] toBuffers,
                    AltingChannelInput<BufferMessage>[] fromBuffers,
                    int consumerId) {
        this.toBuffers = toBuffers;
        this.fromBuffers = fromBuffers;
        this.consumerId = consumerId;
    }

    @Override
    public void run() {
        System.out.println("Consumer " + consumerId + " STARTED.");
        int requestId = 0;

        while (true) {
            // 1. Losowa przerwa (symulacja konsumpcji)
            try {
                Thread.sleep(random.nextInt(1000) + 500); // 0.5s - 1.5s
            } catch (InterruptedException e) {}

            // 2. Wybór losowego bufora
            int targetBuf = random.nextInt(toBuffers.length);
            requestId++;

            // 3. Wysłanie żądania
            BufferMessage req = new BufferMessage(BufferMessage.Type.GET_REQUEST, -1, requestId, false);
            // System.out.println("Cons " + consumerId + " trying to GET from Buf " + targetBuf);
            toBuffers[targetBuf].write(req);

            // 4. Odbiór odpowiedzi (czekamy na ten konkretny bufor)
            BufferMessage response = fromBuffers[targetBuf].read();

            // 5. Obsługa wyniku
            if (response.success && response.type == BufferMessage.Type.GET_RESPONSE) {
                System.out.println("Cons " + consumerId + ": ATE " + response.data + " from Buf " + targetBuf);
            } else {
                System.out.println("Cons " + consumerId + ": REJECTED (Empty) @ Buf " + targetBuf);
            }
        }
    }
}