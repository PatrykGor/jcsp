package org.example.jcsp;

import java.io.Serializable;

// Message types for communication
class BufferMessage implements Serializable {
    public enum Type { PUT_REQUEST, PUT_RESPONSE, GET_REQUEST, GET_RESPONSE }
    public Type type;
    public int data;
    public int requestId;
    public boolean success;

    public BufferMessage(Type type, int data, int requestId, boolean success) {
        this.type = type;
        this.data = data;
        this.requestId = requestId;
        this.success = success;
    }

    @Override
    public String toString() {
        return String.format("BufferMessage{type=%s, data=%d, requestId=%d, success=%s}",
                type, data, requestId, success);
    }
}