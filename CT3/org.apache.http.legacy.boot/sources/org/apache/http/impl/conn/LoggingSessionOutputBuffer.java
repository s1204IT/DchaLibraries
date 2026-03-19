package org.apache.http.impl.conn;

import java.io.IOException;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class LoggingSessionOutputBuffer implements SessionOutputBuffer {
    private final SessionOutputBuffer out;
    private final Wire wire;

    public LoggingSessionOutputBuffer(SessionOutputBuffer out, Wire wire) {
        this.out = out;
        this.wire = wire;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
        if (!this.wire.enabled()) {
            return;
        }
        this.wire.output(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
        if (!this.wire.enabled()) {
            return;
        }
        this.wire.output(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.out.write(b);
        if (!this.wire.enabled()) {
            return;
        }
        this.wire.output(b);
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void writeLine(CharArrayBuffer buffer) throws IOException {
        this.out.writeLine(buffer);
        if (!this.wire.enabled()) {
            return;
        }
        String s = new String(buffer.buffer(), 0, buffer.length());
        this.wire.output(s + "[EOL]");
    }

    @Override
    public void writeLine(String s) throws IOException {
        this.out.writeLine(s);
        if (!this.wire.enabled()) {
            return;
        }
        this.wire.output(s + "[EOL]");
    }

    @Override
    public HttpTransportMetrics getMetrics() {
        return this.out.getMetrics();
    }
}
