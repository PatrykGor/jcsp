package org.example.jcsp;
import org.jcsp.lang.*;

class DistributedBuffer implements CSProcess {
    private final AltingChannelInput<BufferMessage> in;
    private final ChannelOutput<BufferMessage> out;
    private final int bufferId;
    private final int capacity;
    private final int[] buffer;
    private int count = 0;
    private int front = 0;
    private int rear = 0;

    public DistributedBuffer(AltingChannelInput<BufferMessage> in, ChannelOutput<BufferMessage> out,
                             int bufferId, int capacity) {
        this.in = in;
        this.out = out;
        this.bufferId = bufferId;
        this.capacity = capacity;
        this.buffer = new int[capacity];
    }

    @Override
    public void run() {
        final Alternative alt = new Alternative(new Guard[] { in });

        System.out.println("Buffer " + bufferId + " started with capacity " + capacity);

        while (true) {
            // Wait for incoming messages
            int index = alt.select();

            if (index == 0) {
                BufferMessage msg = (BufferMessage) in.read();

                switch (msg.type) {
                    case PUT_REQUEST:
                        handlePutRequest(msg);
                        break;
                    case GET_REQUEST:
                        handleGetRequest(msg);
                        break;
                    default:
                        System.out.println("Buffer " + bufferId + ": Unknown message type");
                }
            }
        }
    }

    private void handlePutRequest(BufferMessage msg) {
        boolean success = false;

        if (count < capacity) {
            buffer[rear] = msg.data;
            rear = (rear + 1) % capacity;
            count++;
            success = true;
            System.out.println("Buffer " + bufferId + ": PUT item=" + msg.data +
                    " (count=" + count + "/" + capacity + ")");
        } else {
            System.out.println("Buffer " + bufferId + ": PUT FAILED - buffer full");
        }

        // Send response
        BufferMessage response = new BufferMessage(
                BufferMessage.Type.PUT_RESPONSE,
                msg.data,
                msg.requestId,
                success
        );
        out.write(response);
    }

    private void handleGetRequest(BufferMessage msg) {
        boolean success = false;
        int data = -1;

        if (count > 0) {
            data = buffer[front];
            front = (front + 1) % capacity;
            count--;
            success = true;
            System.out.println("Buffer " + bufferId + ": GET item=" + data +
                    " (count=" + count + "/" + capacity + ")");
        } else {
            System.out.println("Buffer " + bufferId + ": GET FAILED - buffer empty");
        }

        // Send response
        BufferMessage response = new BufferMessage(
                BufferMessage.Type.GET_RESPONSE,
                data,
                msg.requestId,
                success
        );
        out.write(response);
    }
}