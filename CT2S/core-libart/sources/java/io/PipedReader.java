package java.io;

import java.util.Arrays;
import libcore.io.IoUtils;

public class PipedReader extends Reader {
    private static final int PIPE_SIZE = 1024;
    private char[] buffer;
    private int in;
    boolean isClosed;
    boolean isConnected;
    private Thread lastReader;
    private Thread lastWriter;
    private int out;

    public PipedReader() {
        this.in = -1;
    }

    public PipedReader(PipedWriter out) throws IOException {
        this.in = -1;
        connect(out);
    }

    public PipedReader(int pipeSize) {
        this.in = -1;
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("pipe size " + pipeSize + " too small");
        }
        this.buffer = new char[pipeSize];
    }

    public PipedReader(PipedWriter out, int pipeSize) throws IOException {
        this(pipeSize);
        connect(out);
    }

    @Override
    public synchronized void close() throws IOException {
        this.buffer = null;
        this.isClosed = true;
        notifyAll();
    }

    public void connect(PipedWriter src) throws IOException {
        src.connect(this);
    }

    synchronized void establishConnection() throws IOException {
        if (this.isConnected) {
            throw new IOException("Pipe already connected");
        }
        if (this.isClosed) {
            throw new IOException("Pipe is closed");
        }
        if (this.buffer == null) {
            this.buffer = new char[1024];
        }
        this.isConnected = true;
    }

    @Override
    public int read() throws IOException {
        char[] chars = new char[1];
        int result = read(chars, 0, 1);
        return result != -1 ? chars[0] : result;
    }

    @Override
    public synchronized int read(char[] buffer, int offset, int count) throws IOException {
        int copyLength = 0;
        synchronized (this) {
            if (!this.isConnected) {
                throw new IOException("Pipe not connected");
            }
            if (this.buffer == null) {
                throw new IOException("Pipe is closed");
            }
            Arrays.checkOffsetAndCount(buffer.length, offset, count);
            if (count != 0) {
                this.lastReader = Thread.currentThread();
                boolean first = true;
                while (this.in == -1) {
                    try {
                        if (this.isClosed) {
                            copyLength = -1;
                            break;
                        }
                        if (!first && this.lastWriter != null && !this.lastWriter.isAlive()) {
                            throw new IOException("Pipe broken");
                        }
                        first = false;
                        notifyAll();
                        wait(1000L);
                    } catch (InterruptedException e) {
                        IoUtils.throwInterruptedIoException();
                    }
                    copyLength = 0;
                    if (this.out >= this.in) {
                        copyLength = count > this.buffer.length - this.out ? this.buffer.length - this.out : count;
                        System.arraycopy(this.buffer, this.out, buffer, offset, copyLength);
                        this.out += copyLength;
                        if (this.out == this.buffer.length) {
                            this.out = 0;
                        }
                        if (this.out == this.in) {
                            this.in = -1;
                            this.out = 0;
                        }
                    }
                    if (copyLength != count && this.in != -1) {
                        int charsCopied = copyLength;
                        int copyLength2 = this.in - this.out <= count - copyLength ? count - copyLength : this.in - this.out;
                        System.arraycopy(this.buffer, this.out, buffer, offset + charsCopied, copyLength2);
                        this.out += copyLength2;
                        if (this.out == this.in) {
                            this.in = -1;
                            this.out = 0;
                        }
                        copyLength = copyLength2 + charsCopied;
                    }
                }
                copyLength = 0;
                if (this.out >= this.in) {
                }
                if (copyLength != count) {
                    int charsCopied2 = copyLength;
                    if (this.in - this.out <= count - copyLength) {
                    }
                    System.arraycopy(this.buffer, this.out, buffer, offset + charsCopied2, copyLength2);
                    this.out += copyLength2;
                    if (this.out == this.in) {
                    }
                    copyLength = copyLength2 + charsCopied2;
                }
            }
        }
        return copyLength;
    }

    @Override
    public synchronized boolean ready() throws IOException {
        if (!this.isConnected) {
            throw new IOException("Pipe not connected");
        }
        if (this.buffer == null) {
            throw new IOException("Pipe is closed");
        }
        return this.in != -1;
    }

    synchronized void receive(char oneChar) throws IOException {
        if (this.buffer == null) {
            throw new IOException("Pipe is closed");
        }
        if (this.lastReader != null && !this.lastReader.isAlive()) {
            throw new IOException("Pipe broken");
        }
        this.lastWriter = Thread.currentThread();
        while (this.buffer != null && this.out == this.in) {
            try {
                notifyAll();
                wait(1000L);
                if (this.lastReader != null && !this.lastReader.isAlive()) {
                    throw new IOException("Pipe broken");
                }
            } catch (InterruptedException e) {
                IoUtils.throwInterruptedIoException();
            }
            if (this.buffer != null) {
                throw new IOException("Pipe is closed");
            }
            if (this.in == -1) {
                this.in = 0;
            }
            char[] cArr = this.buffer;
            int i = this.in;
            this.in = i + 1;
            cArr[i] = oneChar;
            if (this.in == this.buffer.length) {
                this.in = 0;
            }
        }
        if (this.buffer != null) {
        }
    }

    synchronized void receive(char[] chars, int offset, int count) throws IOException {
        Arrays.checkOffsetAndCount(chars.length, offset, count);
        if (this.buffer == null) {
            throw new IOException("Pipe is closed");
        }
        if (this.lastReader != null && !this.lastReader.isAlive()) {
            throw new IOException("Pipe broken");
        }
        this.lastWriter = Thread.currentThread();
        while (count > 0) {
            while (this.buffer != null && this.out == this.in) {
                try {
                    notifyAll();
                    wait(1000L);
                    if (this.lastReader != null && !this.lastReader.isAlive()) {
                        throw new IOException("Pipe broken");
                    }
                } catch (InterruptedException e) {
                    IoUtils.throwInterruptedIoException();
                }
                if (this.buffer != null) {
                    throw new IOException("Pipe is closed");
                }
                if (this.in == -1) {
                    this.in = 0;
                }
                if (this.in >= this.out) {
                    int length = this.buffer.length - this.in;
                    if (count < length) {
                        length = count;
                    }
                    System.arraycopy(chars, offset, this.buffer, this.in, length);
                    offset += length;
                    count -= length;
                    this.in += length;
                    if (this.in == this.buffer.length) {
                        this.in = 0;
                    }
                }
                if (count > 0 && this.in != this.out) {
                    int length2 = this.out - this.in;
                    if (count < length2) {
                        length2 = count;
                    }
                    System.arraycopy(chars, offset, this.buffer, this.in, length2);
                    offset += length2;
                    count -= length2;
                    this.in += length2;
                }
            }
            if (this.buffer != null) {
            }
        }
    }

    synchronized void done() {
        this.isClosed = true;
        notifyAll();
    }
}
