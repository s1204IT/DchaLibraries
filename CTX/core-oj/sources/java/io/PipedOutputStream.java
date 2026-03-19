package java.io;

public class PipedOutputStream extends OutputStream {
    private PipedInputStream sink;

    public PipedOutputStream(PipedInputStream pipedInputStream) throws IOException {
        connect(pipedInputStream);
    }

    public PipedOutputStream() {
    }

    public synchronized void connect(PipedInputStream pipedInputStream) throws IOException {
        if (pipedInputStream == null) {
            throw new NullPointerException();
        }
        if (this.sink != null || pipedInputStream.connected) {
            throw new IOException("Already connected");
        }
        this.sink = pipedInputStream;
        pipedInputStream.in = -1;
        pipedInputStream.out = 0;
        pipedInputStream.connected = true;
    }

    @Override
    public void write(int i) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(i);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i > bArr.length || i2 < 0 || (i3 = i + i2) > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        this.sink.receive(bArr, i, i2);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
