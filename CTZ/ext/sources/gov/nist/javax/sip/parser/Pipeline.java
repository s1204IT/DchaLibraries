package gov.nist.javax.sip.parser;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.stack.SIPStackTimerTask;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class Pipeline extends InputStream {
    private LinkedList buffList = new LinkedList();
    private Buffer currentBuffer;
    private boolean isClosed;
    private TimerTask myTimerTask;
    private InputStream pipe;
    private int readTimeout;
    private Timer timer;

    class MyTimer extends SIPStackTimerTask {
        private boolean isCancelled;
        Pipeline pipeline;

        protected MyTimer(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        protected void runTask() {
            if (this.isCancelled) {
                return;
            }
            try {
                this.pipeline.close();
            } catch (IOException e) {
                InternalErrorHandler.handleException(e);
            }
        }

        @Override
        public boolean cancel() {
            boolean zCancel = super.cancel();
            this.isCancelled = true;
            return zCancel;
        }
    }

    class Buffer {
        byte[] bytes;
        int length;
        int ptr = 0;

        public Buffer(byte[] bArr, int i) {
            this.length = i;
            this.bytes = bArr;
        }

        public int getNextByte() {
            byte[] bArr = this.bytes;
            int i = this.ptr;
            this.ptr = i + 1;
            return bArr[i] & 255;
        }
    }

    public void startTimer() {
        if (this.readTimeout == -1) {
            return;
        }
        this.myTimerTask = new MyTimer(this);
        this.timer.schedule(this.myTimerTask, this.readTimeout);
    }

    public void stopTimer() {
        if (this.readTimeout != -1 && this.myTimerTask != null) {
            this.myTimerTask.cancel();
        }
    }

    public Pipeline(InputStream inputStream, int i, Timer timer) {
        this.timer = timer;
        this.pipe = inputStream;
        this.readTimeout = i;
    }

    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (this.isClosed) {
            throw new IOException("Closed!!");
        }
        Buffer buffer = new Buffer(bArr, i2);
        buffer.ptr = i;
        synchronized (this.buffList) {
            this.buffList.add(buffer);
            this.buffList.notifyAll();
        }
    }

    public void write(byte[] bArr) throws IOException {
        if (this.isClosed) {
            throw new IOException("Closed!!");
        }
        Buffer buffer = new Buffer(bArr, bArr.length);
        synchronized (this.buffList) {
            this.buffList.add(buffer);
            this.buffList.notifyAll();
        }
    }

    @Override
    public void close() throws IOException {
        this.isClosed = true;
        synchronized (this.buffList) {
            this.buffList.notifyAll();
        }
        this.pipe.close();
    }

    @Override
    public int read() throws IOException {
        synchronized (this.buffList) {
            if (this.currentBuffer != null && this.currentBuffer.ptr < this.currentBuffer.length) {
                int nextByte = this.currentBuffer.getNextByte();
                if (this.currentBuffer.ptr == this.currentBuffer.length) {
                    this.currentBuffer = null;
                }
                return nextByte;
            }
            if (this.isClosed && this.buffList.isEmpty()) {
                return -1;
            }
            while (this.buffList.isEmpty()) {
                try {
                    this.buffList.wait();
                    if (this.isClosed) {
                        return -1;
                    }
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                } catch (NoSuchElementException e2) {
                    e2.printStackTrace();
                    throw new IOException(e2.getMessage());
                }
            }
            this.currentBuffer = (Buffer) this.buffList.removeFirst();
            int nextByte2 = this.currentBuffer.getNextByte();
            if (this.currentBuffer.ptr == this.currentBuffer.length) {
                this.currentBuffer = null;
            }
            return nextByte2;
        }
    }
}
