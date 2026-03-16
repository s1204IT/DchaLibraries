package java.io;

public class PipedWriter extends Writer {
    private PipedReader destination;
    private boolean isClosed;

    public PipedWriter() {
    }

    public PipedWriter(PipedReader destination) throws IOException {
        super(destination);
        connect(destination);
    }

    @Override
    public void close() throws IOException {
        PipedReader reader = this.destination;
        if (reader != null) {
            reader.done();
            this.isClosed = true;
            this.destination = null;
        }
    }

    public void connect(PipedReader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader == null");
        }
        synchronized (reader) {
            if (this.destination != null) {
                throw new IOException("Pipe already connected");
            }
            reader.establishConnection();
            this.lock = reader;
            this.destination = reader;
        }
    }

    @Override
    public void flush() throws IOException {
        PipedReader reader = this.destination;
        if (this.isClosed) {
            throw new IOException("Pipe is closed");
        }
        if (reader != null) {
            synchronized (reader) {
                if (reader.isClosed) {
                    throw new IOException("Pipe is broken");
                }
                reader.notifyAll();
            }
        }
    }

    @Override
    public void write(char[] buffer, int offset, int count) throws IOException {
        PipedReader reader = this.destination;
        if (reader == null) {
            throw new IOException("Pipe not connected");
        }
        reader.receive(buffer, offset, count);
    }

    @Override
    public void write(int c) throws IOException {
        PipedReader reader = this.destination;
        if (reader == null) {
            throw new IOException("Pipe not connected");
        }
        reader.receive((char) c);
    }
}
