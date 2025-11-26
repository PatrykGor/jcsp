package org.example.jcsp;

import org.example.condition.Buffer;
import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.ChannelOutput;

class Producer implements CSProcess {
    private final ChannelOutput<BufferMessage>[] bufferInputs;
    private final AltingChannelInput<BufferMessage>[] producerInputs;
    private final int producerId;
    private final int totalItems;
    private int itemsProduced = 0;
    private int requestId = 0;

    public Producer(ChannelOutput<BufferMessage>[] bufferInputs, AltingChannelInput<BufferMessage>[] producerInputs,
                    int producerId, int totalItems) {
        this.bufferInputs = bufferInputs;
        this.producerInputs = producerInputs;
        this.producerId = producerId;
        this.totalItems = totalItems;
    }

    @Override
    public void run() {
        final Alternative alt = new Alternative(producerInputs);
        final int[] pendingRequests = new int[bufferInputs.length]; // Track which buffer has pending requests

        System.out.println("Producer " + producerId + " started, will produce " + totalItems + " items");

        while (itemsProduced < totalItems) {
            // Try to produce new items if we have capacity
            while (itemsProduced + countPendingRequests(pendingRequests) < totalItems &&
                    itemsProduced < totalItems) {

                int randomBuffer = (int) (Math.random() * bufferInputs.length);
                int item = producerId * 1000 + itemsProduced;
                requestId++;

                // Send PUT request to random buffer
                BufferMessage request = new BufferMessage(
                        BufferMessage.Type.PUT_REQUEST,
                        item,
                        requestId,
                        false
                );

                bufferInputs[randomBuffer].write(request);
                pendingRequests[randomBuffer]++;

                System.out.println("Producer " + producerId + ": sent PUT request#" +
                        requestId + " to Buffer " + randomBuffer + ", item=" + item);

                // Small delay between requests
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }

            // Wait for responses
            if (hasPendingRequests(pendingRequests)) {
                int index = alt.select();
                BufferMessage response = (BufferMessage) producerInputs[index].read();

                if (response.type == BufferMessage.Type.PUT_RESPONSE) {
                    pendingRequests[index]--;

                    if (response.success) {
                        itemsProduced++;
                        System.out.println("Producer " + producerId + ": PUT confirmed for request#" +
                                response.requestId + ", total produced=" + itemsProduced);
                    } else {
                        System.out.println("Producer " + producerId + ": PUT rejected for request#" +
                                response.requestId + ", will retry");
                    }
                }
            }
        }

        System.out.println("Producer " + producerId + " finished, produced " + itemsProduced + " items");
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