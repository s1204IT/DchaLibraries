package java.io;

import libcore.io.IoUtils;

public class PipedInputStream extends InputStream {
    static final boolean $assertionsDisabled = false;
    private static final int DEFAULT_PIPE_SIZE = 1024;
    protected static final int PIPE_SIZE = 1024;
    protected byte[] buffer;
    volatile boolean closedByReader;
    boolean closedByWriter;
    boolean connected;
    protected int in;
    protected int out;
    Thread readSide;
    Thread writeSide;

    public PipedInputStream(PipedOutputStream pipedOutputStream) throws IOException {
        this(pipedOutputStream, 1024);
    }

    public PipedInputStream(PipedOutputStream pipedOutputStream, int i) throws IOException {
        this.closedByWriter = $assertionsDisabled;
        this.closedByReader = $assertionsDisabled;
        this.connected = $assertionsDisabled;
        this.in = -1;
        this.out = 0;
        initPipe(i);
        connect(pipedOutputStream);
    }

    public PipedInputStream() {
        this.closedByWriter = $assertionsDisabled;
        this.closedByReader = $assertionsDisabled;
        this.connected = $assertionsDisabled;
        this.in = -1;
        this.out = 0;
        initPipe(1024);
    }

    public PipedInputStream(int i) {
        this.closedByWriter = $assertionsDisabled;
        this.closedByReader = $assertionsDisabled;
        this.connected = $assertionsDisabled;
        this.in = -1;
        this.out = 0;
        initPipe(i);
    }

    private void initPipe(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        this.buffer = new byte[i];
    }

    public void connect(PipedOutputStream pipedOutputStream) throws IOException {
        pipedOutputStream.connect(this);
    }

    protected synchronized void receive(int i) throws IOException {
        checkStateForReceive();
        this.writeSide = Thread.currentThread();
        if (this.in == this.out) {
            awaitSpace();
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        byte[] bArr = this.buffer;
        int i2 = this.in;
        this.in = i2 + 1;
        bArr[i2] = (byte) (i & 255);
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
    }

    synchronized void receive(byte[] bArr, int i, int i2) throws IOException {
        int length;
        checkStateForReceive();
        this.writeSide = Thread.currentThread();
        while (i2 > 0) {
            if (this.in == this.out) {
                awaitSpace();
            }
            if (this.out < this.in) {
                length = this.buffer.length - this.in;
            } else if (this.in < this.out) {
                if (this.in == -1) {
                    this.out = 0;
                    this.in = 0;
                    length = this.buffer.length - this.in;
                } else {
                    length = this.out - this.in;
                }
            } else {
                length = 0;
            }
            if (length > i2) {
                length = i2;
            }
            System.arraycopy(bArr, i, this.buffer, this.in, length);
            i2 -= length;
            i += length;
            this.in += length;
            if (this.in >= this.buffer.length) {
                this.in = 0;
            }
        }
    }

    private void checkStateForReceive() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    private void awaitSpace() throws IOException {
        while (this.in == this.out) {
            checkStateForReceive();
            notifyAll();
            try {
                wait(1000L);
            } catch (InterruptedException e) {
                IoUtils.throwInterruptedIoException();
            }
        }
    }

    synchronized void receivedLast() {
        this.closedByWriter = true;
        notifyAll();
    }

    @Override
    public synchronized int read() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.writeSide != null && !this.writeSide.isAlive() && !this.closedByWriter && this.in < 0) {
            throw new IOException("Write end dead");
        }
        this.readSide = Thread.currentThread();
        int i = 2;
        while (this.in < 0) {
            if (this.closedByWriter) {
                return -1;
            }
            if (this.writeSide != null && !this.writeSide.isAlive() && i - 1 < 0) {
                throw new IOException("Pipe broken");
            }
            notifyAll();
            try {
                wait(1000L);
            } catch (InterruptedException e) {
                IoUtils.throwInterruptedIoException();
            }
        }
        byte[] bArr = this.buffer;
        int i2 = this.out;
        this.out = i2 + 1;
        int i3 = bArr[i2] & Character.DIRECTIONALITY_UNDEFINED;
        if (this.out >= this.buffer.length) {
            this.out = 0;
        }
        if (this.in == this.out) {
            this.in = -1;
        }
        return i3;
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
        int length;
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int i3 = read();
        if (i3 < 0) {
            return -1;
        }
        bArr[i] = (byte) i3;
        int i4 = 1;
        while (this.in >= 0 && i2 > 1) {
            if (this.in > this.out) {
                length = Math.min(this.buffer.length - this.out, this.in - this.out);
            } else {
                length = this.buffer.length - this.out;
            }
            int i5 = i2 - 1;
            if (length > i5) {
                length = i5;
            }
            System.arraycopy(this.buffer, this.out, bArr, i + i4, length);
            this.out += length;
            i4 += length;
            i2 -= length;
            if (this.out >= this.buffer.length) {
                this.out = 0;
            }
            if (this.in == this.out) {
                this.in = -1;
            }
        }
        return i4;
    }

    @Override
    public synchronized int available() throws IOException {
        if (this.in < 0) {
            return 0;
        }
        if (this.in == this.out) {
            return this.buffer.length;
        }
        if (this.in > this.out) {
            return this.in - this.out;
        }
        return (this.in + this.buffer.length) - this.out;
    }

    @Override
    public void close() throws IOException {
        this.closedByReader = true;
        synchronized (this) {
            this.in = -1;
        }
    }
}
