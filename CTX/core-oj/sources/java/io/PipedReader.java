package java.io;

import libcore.io.IoUtils;

public class PipedReader extends Reader {
    private static final int DEFAULT_PIPE_SIZE = 1024;
    char[] buffer;
    boolean closedByReader;
    boolean closedByWriter;
    boolean connected;
    int in;
    int out;
    Thread readSide;
    Thread writeSide;

    public PipedReader(PipedWriter pipedWriter) throws IOException {
        this(pipedWriter, 1024);
    }

    public PipedReader(PipedWriter pipedWriter, int i) throws IOException {
        this.closedByWriter = false;
        this.closedByReader = false;
        this.connected = false;
        this.in = -1;
        this.out = 0;
        initPipe(i);
        connect(pipedWriter);
    }

    public PipedReader() {
        this.closedByWriter = false;
        this.closedByReader = false;
        this.connected = false;
        this.in = -1;
        this.out = 0;
        initPipe(1024);
    }

    public PipedReader(int i) {
        this.closedByWriter = false;
        this.closedByReader = false;
        this.connected = false;
        this.in = -1;
        this.out = 0;
        initPipe(i);
    }

    private void initPipe(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Pipe size <= 0");
        }
        this.buffer = new char[i];
    }

    public void connect(PipedWriter pipedWriter) throws IOException {
        pipedWriter.connect(this);
    }

    synchronized void receive(int i) throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
        this.writeSide = Thread.currentThread();
        while (this.in == this.out) {
            if (this.readSide != null && !this.readSide.isAlive()) {
                throw new IOException("Pipe broken");
            }
            notifyAll();
            try {
                wait(1000L);
            } catch (InterruptedException e) {
                IoUtils.throwInterruptedIoException();
            }
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        char[] cArr = this.buffer;
        int i2 = this.in;
        this.in = i2 + 1;
        cArr[i2] = (char) i;
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
    }

    synchronized void receive(char[] cArr, int i, int i2) throws IOException {
        while (true) {
            i2--;
            if (i2 >= 0) {
                int i3 = i + 1;
                receive(cArr[i]);
                i = i3;
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
        char[] cArr = this.buffer;
        int i2 = this.out;
        this.out = i2 + 1;
        char c = cArr[i2];
        if (this.out >= this.buffer.length) {
            this.out = 0;
        }
        if (this.in == this.out) {
            this.in = -1;
        }
        return c;
    }

    @Override
    public synchronized int read(char[] cArr, int i, int i2) throws IOException {
        int i3;
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.writeSide != null && !this.writeSide.isAlive() && !this.closedByWriter && this.in < 0) {
            throw new IOException("Write end dead");
        }
        if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int i4 = read();
        if (i4 < 0) {
            return -1;
        }
        cArr[i] = (char) i4;
        int i5 = 1;
        while (this.in >= 0 && i2 - 1 > 0) {
            char[] cArr2 = this.buffer;
            int i6 = this.out;
            this.out = i6 + 1;
            cArr[i + i5] = cArr2[i6];
            i5++;
            if (this.out >= this.buffer.length) {
                this.out = 0;
            }
            if (this.in == this.out) {
                this.in = -1;
            }
        }
        return i5;
    }

    @Override
    public synchronized boolean ready() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.writeSide != null && !this.writeSide.isAlive() && !this.closedByWriter && this.in < 0) {
            throw new IOException("Write end dead");
        }
        if (this.in < 0) {
            return false;
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        this.in = -1;
        this.closedByReader = true;
    }
}
