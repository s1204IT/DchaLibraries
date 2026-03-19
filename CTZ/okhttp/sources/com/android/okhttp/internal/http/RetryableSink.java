package com.android.okhttp.internal.http;

import com.android.okhttp.internal.Util;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Timeout;
import java.io.IOException;
import java.net.ProtocolException;

public final class RetryableSink implements Sink {
    private boolean closed;
    private final Buffer content;
    private final int limit;

    public RetryableSink(int i) {
        this.content = new Buffer();
        this.limit = i;
    }

    public RetryableSink() {
        this(-1);
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.content.size() < this.limit) {
            throw new ProtocolException("content-length promised " + this.limit + " bytes, but received " + this.content.size());
        }
    }

    @Override
    public void write(Buffer buffer, long j) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        Util.checkOffsetAndCount(buffer.size(), 0L, j);
        if (this.limit != -1 && this.content.size() > ((long) this.limit) - j) {
            throw new ProtocolException("exceeded content-length limit of " + this.limit + " bytes");
        }
        this.content.write(buffer, j);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    public long contentLength() throws IOException {
        return this.content.size();
    }

    public void writeToSocket(Sink sink) throws IOException {
        Buffer buffer = new Buffer();
        this.content.copyTo(buffer, 0L, this.content.size());
        sink.write(buffer, buffer.size());
    }
}
