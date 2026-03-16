package java.io;

public class PipedOutputStream extends OutputStream {
    private PipedInputStream target;

    public PipedOutputStream() {
    }

    public PipedOutputStream(PipedInputStream target) throws IOException {
        connect(target);
    }

    @Override
    public void close() throws IOException {
        PipedInputStream stream = this.target;
        if (stream != null) {
            stream.done();
            this.target = null;
        }
    }

    public void connect(PipedInputStream stream) throws IOException {
        if (stream == null) {
            throw new NullPointerException("stream == null");
        }
        synchronized (stream) {
            if (this.target != null) {
                throw new IOException("Already connected");
            }
            if (stream.isConnected) {
                throw new IOException("Pipe already connected");
            }
            stream.establishConnection();
            this.target = stream;
        }
    }

    @Override
    public void flush() throws IOException {
        PipedInputStream stream = this.target;
        if (stream != null) {
            synchronized (stream) {
                stream.notifyAll();
            }
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        super.write(buffer, offset, count);
    }

    @Override
    public void write(int oneByte) throws IOException {
        PipedInputStream stream = this.target;
        if (stream == null) {
            throw new IOException("Pipe not connected");
        }
        stream.receive(oneByte);
    }
}
