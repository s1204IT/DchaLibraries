package java.io;

import java.util.Arrays;
import libcore.io.IoUtils;

public class PipedInputStream extends InputStream {
    protected static final int PIPE_SIZE = 1024;
    protected byte[] buffer;
    protected int in;
    private boolean isClosed;
    boolean isConnected;
    private Thread lastReader;
    private Thread lastWriter;
    protected int out;

    public PipedInputStream() {
        this.in = -1;
    }

    public PipedInputStream(PipedOutputStream out) throws IOException {
        this.in = -1;
        connect(out);
    }

    public PipedInputStream(int pipeSize) {
        this.in = -1;
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("pipe size " + pipeSize + " too small");
        }
        this.buffer = new byte[pipeSize];
    }

    public PipedInputStream(PipedOutputStream out, int pipeSize) throws IOException {
        this(pipeSize);
        connect(out);
    }

    @Override
    public synchronized int available() throws IOException {
        int length;
        if (this.buffer == null || this.in == -1) {
            length = 0;
        } else {
            length = this.in <= this.out ? (this.buffer.length - this.out) + this.in : this.in - this.out;
        }
        return length;
    }

    @Override
    public synchronized void close() throws IOException {
        this.buffer = null;
        notifyAll();
    }

    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    synchronized void establishConnection() throws IOException {
        if (this.isConnected) {
            throw new IOException("Pipe already connected");
        }
        if (this.buffer == null) {
            this.buffer = new byte[1024];
        }
        this.isConnected = true;
    }

    @Override
    public synchronized int read() throws IOException {
        int i = -1;
        synchronized (this) {
            if (!this.isConnected) {
                throw new IOException("Not connected");
            }
            if (this.buffer == null) {
                throw new IOException("InputStream is closed");
            }
            this.lastReader = Thread.currentThread();
            int attempts = 3;
            while (true) {
                try {
                    int attempts2 = attempts;
                    if (this.in != -1) {
                        break;
                    }
                    if (this.isClosed) {
                        break;
                    }
                    attempts = attempts2 - 1;
                    if (attempts2 <= 0) {
                        try {
                            if (this.lastWriter != null && !this.lastWriter.isAlive()) {
                                throw new IOException("Pipe broken");
                            }
                        } catch (InterruptedException e) {
                            IoUtils.throwInterruptedIoException();
                            byte[] bArr = this.buffer;
                            int i2 = this.out;
                            this.out = i2 + 1;
                            i = bArr[i2] & Character.DIRECTIONALITY_UNDEFINED;
                            if (this.out == this.buffer.length) {
                                this.out = 0;
                            }
                            if (this.out == this.in) {
                                this.in = -1;
                                this.out = 0;
                            }
                            notifyAll();
                            return i;
                        }
                    }
                    notifyAll();
                    wait(1000L);
                } catch (InterruptedException e2) {
                }
            }
        }
        return i;
    }

    @Override
    public synchronized int read(byte[] bytes, int byteOffset, int byteCount) throws IOException {
        int totalCopied = 0;
        synchronized (this) {
            Arrays.checkOffsetAndCount(bytes.length, byteOffset, byteCount);
            if (byteCount != 0) {
                if (!this.isConnected) {
                    throw new IOException("Not connected");
                }
                if (this.buffer == null) {
                    throw new IOException("InputStream is closed");
                }
                this.lastReader = Thread.currentThread();
                int attempts = 3;
                while (true) {
                    try {
                        int attempts2 = attempts;
                        if (this.in != -1) {
                            break;
                        }
                        if (this.isClosed) {
                            totalCopied = -1;
                            break;
                        }
                        attempts = attempts2 - 1;
                        if (attempts2 <= 0) {
                            try {
                                if (this.lastWriter != null && !this.lastWriter.isAlive()) {
                                    throw new IOException("Pipe broken");
                                }
                            } catch (InterruptedException e) {
                                IoUtils.throwInterruptedIoException();
                                totalCopied = 0;
                                if (this.out >= this.in) {
                                    int leftInBuffer = this.buffer.length - this.out;
                                    int length = leftInBuffer < byteCount ? leftInBuffer : byteCount;
                                    System.arraycopy(this.buffer, this.out, bytes, byteOffset, length);
                                    this.out += length;
                                    if (this.out == this.buffer.length) {
                                        this.out = 0;
                                    }
                                    if (this.out == this.in) {
                                        this.in = -1;
                                        this.out = 0;
                                    }
                                    totalCopied = 0 + length;
                                }
                                if (totalCopied < byteCount && this.in != -1) {
                                    int leftInBuffer2 = this.in - this.out;
                                    int leftToCopy = byteCount - totalCopied;
                                    int length2 = leftToCopy < leftInBuffer2 ? leftToCopy : leftInBuffer2;
                                    System.arraycopy(this.buffer, this.out, bytes, byteOffset + totalCopied, length2);
                                    this.out += length2;
                                    if (this.out == this.in) {
                                        this.in = -1;
                                        this.out = 0;
                                    }
                                    totalCopied += length2;
                                }
                                notifyAll();
                                return totalCopied;
                            }
                        }
                        notifyAll();
                        wait(1000L);
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }
        return totalCopied;
    }

    protected synchronized void receive(int oneByte) throws IOException {
        if (this.buffer == null || this.isClosed) {
            throw new IOException("Pipe is closed");
        }
        this.lastWriter = Thread.currentThread();
        while (this.buffer != null && this.out == this.in) {
            try {
                if (this.lastReader != null && !this.lastReader.isAlive()) {
                    throw new IOException("Pipe broken");
                }
                notifyAll();
                wait(1000L);
            } catch (InterruptedException e) {
                IoUtils.throwInterruptedIoException();
            }
            if (this.buffer != null) {
                throw new IOException("Pipe is closed");
            }
            if (this.in == -1) {
                this.in = 0;
            }
            if (this.lastReader != null && !this.lastReader.isAlive()) {
                throw new IOException("Pipe broken");
            }
            byte[] bArr = this.buffer;
            int i = this.in;
            this.in = i + 1;
            bArr[i] = (byte) oneByte;
            if (this.in == this.buffer.length) {
                this.in = 0;
            }
            notifyAll();
        }
        if (this.buffer != null) {
        }
    }

    synchronized void done() {
        this.isClosed = true;
        notifyAll();
    }
}
