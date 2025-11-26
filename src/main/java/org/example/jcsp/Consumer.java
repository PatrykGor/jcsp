package org.example.jcsp;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.ChannelOutput;

class Consumer implements CSProcess {
    private final ChannelOutput<BufferMessage>[] bufferInputs;
    private final AltingChannelInput<BufferMessage>[] consumerInputs;
    private final int consumerId;
    private final int totalItems;
    private int itemsConsumed = 0;
    private int requestId = 0;

    public Consumer(ChannelOutput<BufferMessage>[] bufferInputs, AltingChannelInput<BufferMessage>[] consumerInputs,
                    int consumerId, int totalItems) {
        this.bufferInputs = bufferInputs;
        this.consumerInputs = consumerInputs;
        this.consumerId = consumerId;
        this.totalItems = totalItems;
    }

    @Override
    public void run() {
        final Alternative alt = new Alternative(consumerInputs);
        final int[] pendingRequests = new int[bufferInputs.length];

        System.out.println("Consumer " + consumerId + " started, will consume " + totalItems + " items");

        while (itemsConsumed < totalItems) {
            // Send GET requests to random buffers
            while (itemsConsumed + countPendingRequests(pendingRequests) < totalItems &&
                    itemsConsumed < totalItems) {

                int randomBuffer = (int) (Math.random() * bufferInputs.length);
                requestId++;

                // Send GET request to random buffer
                BufferMessage request = new BufferMessage(
                        BufferMessage.Type.GET_REQUEST,
                        -1,
                        requestId,
                        false
                );

                bufferInputs[randomBuffer].write(request);
                pendingRequests[randomBuffer]++;

                System.out.println("Consumer " + consumerId + ": sent GET request#" +
                        requestId + " to Buffer " + randomBuffer);

                // Small delay between requests
                try { Thread.sleep(150); } catch (InterruptedException e) {}
            }

            // Wait for responses
            if (hasPendingRequests(pendingRequests)) {
                int index = alt.select();
                BufferMessage response = (BufferMessage) consumerInputs[index].read();

                if (response.type == BufferMessage.Type.GET_RESPONSE) {
                    pendingRequests[index]--;

                    if (response.success) {
                        itemsConsumed++;
                        System.out.println("Consumer " + consumerId + ": GET successful, item=" +
                                response.data + ", total consumed=" + itemsConsumed);
                    } else {
                        System.out.println("Consumer " + consumerId + ": GET failed for request#" +
                                response.requestId + ", buffer empty");
                    }
                }
            }
        }

        System.out.println("Consumer " + consumerId + " finished, consumed " + itemsConsumed + " items");
    }

    private int countPendingRequests(int[] pendingRequests) {
        int count = 0;
        for (int req : pendingRequests) {
            count += req;
        }
        return count;
    }

    private boolean hasPendingRequests(int[] pendingRequests) {
        for (int req : pendingRequests) {
            if (req > 0) return true;
        }
        return false;
    }
}