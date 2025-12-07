package org.example.jcsp;
import org.jcsp.lang.*;
import java.util.Random;

class Producer implements CSProcess {
    private final ChannelOutput<BufferMessage>[] toBuffers;
    private final AltingChannelInput<BufferMessage>[] fromBuffers;
    private final int producerId;
    private final Random random = new Random();

    public Producer(ChannelOutput<BufferMessage>[] toBuffers,
                    AltingChannelInput<BufferMessage>[] fromBuffers,
                    int producerId) {
        this.toBuffers = toBuffers;
        this.fromBuffers = fromBuffers;
        this.producerId = producerId;
    }

    @Override
    public void run() {
        System.out.println("Producer " + producerId + " STARTED.");
        int itemCounter = 0;
        int requestId = 0;

        while (true) {
            // 1. Losowa przerwa (symulacja produkcji)
            try {
                Thread.sleep(random.nextInt(1000) + 500); // 0.5s - 1.5s
            } catch (InterruptedException e) {}

            // 2. Wybór losowego bufora
            requestId++;
            int item = producerId * 1000 + itemCounter;
            boolean success = false;
            BufferMessage response = null;
            int targetBuf = -1;
            while (!success) {
            targetBuf = random.nextInt(toBuffers.length);

            // 3. Wysłanie żądania (BLOKUJĄCE, jeśli bufor jest zajęty obsługą innego wątku)
            BufferMessage req = new BufferMessage(BufferMessage.Type.PUT_REQUEST, item, requestId, false);
            // System.out.println("Prod " + producerId + " trying to PUT " + item + " to Buf " + targetBuf);
            toBuffers[targetBuf].write(req);

            // 4. Odbiór odpowiedzi (BLOKUJĄCE - czekamy aż ten konkretny bufor odpowie)
            // Nie potrzebujemy Alternative, bo wiemy dokładnie, gdzie wysłaliśmy
            response = fromBuffers[targetBuf].read();

                success = response.type == BufferMessage.Type.PUT_RESPONSE ? response.success : false;
                if (!success) {
                    System.out.println("Prod " + producerId + ": REJECTED (Full) @ Buf " + targetBuf);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Prod " + producerId + ": SUCCESS " + item + " @ Buf " + targetBuf);
        }
    }
}
