package org.apache.http.impl.io;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class AbstractSessionOutputBuffer implements SessionOutputBuffer {
    private static final byte[] CRLF = {13, 10};
    private static final int MAX_CHUNK = 256;
    private ByteArrayBuffer buffer;
    private HttpTransportMetricsImpl metrics;
    private OutputStream outstream;
    private String charset = "US-ASCII";
    private boolean ascii = true;

    protected void init(OutputStream outstream, int buffersize, HttpParams params) {
        boolean zEqualsIgnoreCase;
        if (outstream == null) {
            throw new IllegalArgumentException("Input stream may not be null");
        }
        if (buffersize <= 0) {
            throw new IllegalArgumentException("Buffer size may not be negative or zero");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.outstream = outstream;
        this.buffer = new ByteArrayBuffer(buffersize);
        this.charset = HttpProtocolParams.getHttpElementCharset(params);
        if (this.charset.equalsIgnoreCase("US-ASCII")) {
            zEqualsIgnoreCase = true;
        } else {
            zEqualsIgnoreCase = this.charset.equalsIgnoreCase(HTTP.ASCII);
        }
        this.ascii = zEqualsIgnoreCase;
        this.metrics = new HttpTransportMetricsImpl();
    }

    protected void flushBuffer() throws IOException {
        int len = this.buffer.length();
        if (len <= 0) {
            return;
        }
        this.outstream.write(this.buffer.buffer(), 0, len);
        this.buffer.clear();
        this.metrics.incrementBytesTransferred(len);
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        this.outstream.flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            return;
        }
        if (len > MAX_CHUNK || len > this.buffer.capacity()) {
            flushBuffer();
            this.outstream.write(b, off, len);
            this.metrics.incrementBytesTransferred(len);
        } else {
            int freecapacity = this.buffer.capacity() - this.buffer.length();
            if (len > freecapacity) {
                flushBuffer();
            }
            this.buffer.append(b, off, len);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b == null) {
            return;
        }
        write(b, 0, b.length);
    }

    @Override
    public void write(int b) throws IOException {
        if (this.buffer.isFull()) {
            flushBuffer();
        }
        this.buffer.append(b);
    }

    @Override
    public void writeLine(String s) throws IOException {
        if (s == null) {
            return;
        }
        if (s.length() > 0) {
            write(s.getBytes(this.charset));
        }
        write(CRLF);
    }

    @Override
    public void writeLine(CharArrayBuffer s) throws IOException {
        if (s == null) {
            return;
        }
        if (this.ascii) {
            int off = 0;
            int remaining = s.length();
            while (remaining > 0) {
                int chunk = Math.min(this.buffer.capacity() - this.buffer.length(), remaining);
                if (chunk > 0) {
                    this.buffer.append(s, off, chunk);
                }
                if (this.buffer.isFull()) {
                    flushBuffer();
                }
                off += chunk;
                remaining -= chunk;
            }
        } else {
            byte[] tmp = s.toString().getBytes(this.charset);
            write(tmp);
        }
        write(CRLF);
    }

    @Override
    public HttpTransportMetrics getMetrics() {
        return this.metrics;
    }
}
