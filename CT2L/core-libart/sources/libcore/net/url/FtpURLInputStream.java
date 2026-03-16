package libcore.net.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import libcore.io.IoUtils;

class FtpURLInputStream extends InputStream {
    private Socket controlSocket;
    private InputStream is;

    public FtpURLInputStream(InputStream is, Socket controlSocket) {
        this.is = is;
        this.controlSocket = controlSocket;
    }

    @Override
    public int read() throws IOException {
        return this.is.read();
    }

    @Override
    public int read(byte[] buf, int off, int nbytes) throws IOException {
        return this.is.read(buf, off, nbytes);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.is.reset();
    }

    @Override
    public synchronized void mark(int limit) {
        this.is.mark(limit);
    }

    @Override
    public boolean markSupported() {
        return this.is.markSupported();
    }

    @Override
    public void close() {
        IoUtils.closeQuietly(this.is);
        IoUtils.closeQuietly(this.controlSocket);
    }

    @Override
    public int available() throws IOException {
        return this.is.available();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return this.is.skip(byteCount);
    }
}
