package org.example.jcsp;
import org.jcsp.lang.*;

class DistributedBuffer implements CSProcess {
    private final AltingChannelInput<BufferMessage>[] inputs;
    private final ChannelOutput<BufferMessage>[] outputs;
    private final int bufferId;
    private final int capacity;
    private final int[] buffer;
    private int count = 0;
    private int front = 0;
    private int rear = 0;

    private final OperationStats stats;
    private volatile boolean running = true;

    public DistributedBuffer(AltingChannelInput<BufferMessage>[] inputs,
                             ChannelOutput<BufferMessage>[] outputs,
                             int bufferId, int capacity, OperationStats stats) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.bufferId = bufferId;
        this.capacity = capacity;
        this.buffer = new int[capacity];
        this.stats = stats;
    }

    @Override
    public void run() {
        final Alternative alt = new Alternative(inputs);
        System.out.println("Buffer " + bufferId + " STARTED.");

        while (running) {
            int clientIndex = alt.select();
            BufferMessage msg = inputs[clientIndex].read();

            boolean success = false;
            int responseData = -1;

            switch (msg.type) {
                case PUT_REQUEST:
                    if (count < capacity) {
                        buffer[rear] = msg.data;
                        rear = (rear + 1) % capacity;
                        count++;
                        success = true;
                        stats.incSuccessPut();
                        //System.out.println("  -> Buffer " + bufferId + " PUT " + msg.data + " (Size: " + count + ")");
                    }else{
                        stats.incFailedPut();
                    }
                    responseData = msg.data;
                    break;

                case GET_REQUEST:
                    if (count > 0) {
                        responseData = buffer[front];
                        front = (front + 1) % capacity;
                        count--;
                        success = true;
                        stats.incSuccessGet();
                        //System.out.println("  <- Buffer " + bufferId + " GET " + responseData + " (Size: " + count + ")");
                    }else{
                        stats.incFailedGet();
                    }
                    break;
            }

            BufferMessage response = new BufferMessage(
                    msg.type == BufferMessage.Type.PUT_REQUEST ? BufferMessage.Type.PUT_RESPONSE : BufferMessage.Type.GET_RESPONSE,
                    responseData,
                    msg.requestId,
                    success
            );
            outputs[clientIndex].write(response);
        }
    }

    public  void stop() {
        running = false;
    }
}