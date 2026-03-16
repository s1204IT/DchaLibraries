package org.apache.http.impl.conn;

import java.io.IOException;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class LoggingSessionInputBuffer implements SessionInputBuffer {
    private final SessionInputBuffer in;
    private final Wire wire;

    public LoggingSessionInputBuffer(SessionInputBuffer in, Wire wire) {
        this.in = in;
        this.wire = wire;
    }

    @Override
    public boolean isDataAvailable(int timeout) throws IOException {
        return this.in.isDataAvailable(timeout);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int l = this.in.read(b, off, len);
        if (this.wire.enabled() && l > 0) {
            this.wire.input(b, off, l);
        }
        return l;
    }

    @Override
    public int read() throws IOException {
        int l = this.in.read();
        if (this.wire.enabled() && l > 0) {
            this.wire.input(l);
        }
        return l;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int l = this.in.read(b);
        if (this.wire.enabled() && l > 0) {
            this.wire.input(b, 0, l);
        }
        return l;
    }

    @Override
    public String readLine() throws IOException {
        String s = this.in.readLine();
        if (this.wire.enabled() && s != null) {
            this.wire.input(s + "[EOL]");
        }
        return s;
    }

    @Override
    public int readLine(CharArrayBuffer buffer) throws IOException {
        int l = this.in.readLine(buffer);
        if (this.wire.enabled() && l > 0) {
            int pos = buffer.length() - l;
            String s = new String(buffer.buffer(), pos, l);
            this.wire.input(s + "[EOL]");
        }
        return l;
    }

    @Override
    public HttpTransportMetrics getMetrics() {
        return this.in.getMetrics();
    }
}
