package java.io;

public class PipedWriter extends Writer {
    private boolean closed = false;
    private PipedReader sink;

    public PipedWriter(PipedReader pipedReader) throws IOException {
        connect(pipedReader);
    }

    public PipedWriter() {
    }

    public synchronized void connect(PipedReader pipedReader) throws IOException {
        if (pipedReader == null) {
            throw new NullPointerException();
        }
        if (this.sink != null || pipedReader.connected) {
            throw new IOException("Already connected");
        }
        if (pipedReader.closedByReader || this.closed) {
            throw new IOException("Pipe closed");
        }
        this.sink = pipedReader;
        pipedReader.in = -1;
        pipedReader.out = 0;
        pipedReader.connected = true;
    }

    @Override
    public void write(int i) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(i);
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        int i3 = i + i2;
        if ((i | i2 | i3 | (cArr.length - i3)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.sink.receive(cArr, i, i2);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            if (this.sink.closedByReader || this.closed) {
                throw new IOException("Pipe closed");
            }
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
