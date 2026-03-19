package org.apache.http.conn;

import java.io.IOException;
import java.io.InputStream;

@Deprecated
public class EofSensorInputStream extends InputStream implements ConnectionReleaseTrigger {
    private EofSensorWatcher eofWatcher;
    private boolean selfClosed;
    protected InputStream wrappedStream;

    public EofSensorInputStream(InputStream inputStream, EofSensorWatcher eofSensorWatcher) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Wrapped stream may not be null.");
        }
        this.wrappedStream = inputStream;
        this.selfClosed = false;
        this.eofWatcher = eofSensorWatcher;
    }

    protected boolean isReadAllowed() throws IOException {
        if (this.selfClosed) {
            throw new IOException("Attempted read on closed stream.");
        }
        return this.wrappedStream != null;
    }

    @Override
    public int read() throws IOException {
        if (isReadAllowed()) {
            try {
                int i = this.wrappedStream.read();
                checkEOF(i);
                return i;
            } catch (IOException e) {
                checkAbort();
                throw e;
            }
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (isReadAllowed()) {
            try {
                int i3 = this.wrappedStream.read(bArr, i, i2);
                checkEOF(i3);
                return i3;
            } catch (IOException e) {
                checkAbort();
                throw e;
            }
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        if (isReadAllowed()) {
            try {
                int i = this.wrappedStream.read(bArr);
                checkEOF(i);
                return i;
            } catch (IOException e) {
                checkAbort();
                throw e;
            }
        }
        return -1;
    }

    @Override
    public int available() throws IOException {
        if (isReadAllowed()) {
            try {
                return this.wrappedStream.available();
            } catch (IOException e) {
                checkAbort();
                throw e;
            }
        }
        return 0;
    }

    @Override
    public void close() throws IOException {
        this.selfClosed = true;
        checkClose();
    }

    protected void checkEOF(int i) throws IOException {
        if (this.wrappedStream != null && i < 0) {
            boolean zEofDetected = true;
            try {
                if (this.eofWatcher != null) {
                    zEofDetected = this.eofWatcher.eofDetected(this.wrappedStream);
                }
                if (zEofDetected) {
                    this.wrappedStream.close();
                }
            } finally {
                this.wrappedStream = null;
            }
        }
    }

    protected void checkClose() throws IOException {
        if (this.wrappedStream != null) {
            boolean zStreamClosed = true;
            try {
                if (this.eofWatcher != null) {
                    zStreamClosed = this.eofWatcher.streamClosed(this.wrappedStream);
                }
                if (zStreamClosed) {
                    this.wrappedStream.close();
                }
            } finally {
                this.wrappedStream = null;
            }
        }
    }

    protected void checkAbort() throws IOException {
        if (this.wrappedStream != null) {
            boolean zStreamAbort = true;
            try {
                if (this.eofWatcher != null) {
                    zStreamAbort = this.eofWatcher.streamAbort(this.wrappedStream);
                }
                if (zStreamAbort) {
                    this.wrappedStream.close();
                }
            } finally {
                this.wrappedStream = null;
            }
        }
    }

    @Override
    public void releaseConnection() throws IOException {
        close();
    }

    @Override
    public void abortConnection() throws IOException {
        this.selfClosed = true;
        checkAbort();
    }
}
