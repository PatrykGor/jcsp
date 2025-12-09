package org.example.jcsp;

public class OperationStats {
    public long BufferID;
    public long successfulPut = 0;
    public long failedPut = 0;
    public long successfulGet = 0;
    public long failedGet = 0;

    public OperationStats(long bufferID) {
        this.BufferID = bufferID;
    }

    public synchronized void incSuccessPut() { successfulPut++; }
    public synchronized void incFailedPut()  { failedPut++; }
    public synchronized void incSuccessGet() { successfulGet++; }
    public synchronized void incFailedGet()  { failedGet++; }
}
