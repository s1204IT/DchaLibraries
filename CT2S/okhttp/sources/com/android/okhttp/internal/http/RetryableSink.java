package com.android.okhttp.internal.http;

import com.android.okhttp.internal.Util;
import com.android.okio.BufferedSink;
import com.android.okio.Deadline;
import com.android.okio.OkBuffer;
import com.android.okio.Sink;
import java.io.IOException;
import java.net.ProtocolException;

final class RetryableSink implements Sink {
    private boolean closed;
    private final OkBuffer content;
    private final int limit;

    public RetryableSink(int limit) {
        this.content = new OkBuffer();
        this.limit = limit;
    }

    public RetryableSink() {
        this(-1);
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.content.size() < this.limit) {
                throw new ProtocolException("content-length promised " + this.limit + " bytes, but received " + this.content.size());
            }
        }
    }

    @Override
    public void write(OkBuffer source, long byteCount) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("closed");
        }
        Util.checkOffsetAndCount(source.size(), 0L, byteCount);
        if (this.limit != -1 && this.content.size() > ((long) this.limit) - byteCount) {
            throw new ProtocolException("exceeded content-length limit of " + this.limit + " bytes");
        }
        this.content.write(source, byteCount);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public Sink mo2deadline(Deadline deadline) {
        return this;
    }

    public long contentLength() throws IOException {
        return this.content.size();
    }

    public void writeToSocket(BufferedSink socketOut) throws IOException {
        socketOut.write(this.content.m1clone(), this.content.size());
    }
}
