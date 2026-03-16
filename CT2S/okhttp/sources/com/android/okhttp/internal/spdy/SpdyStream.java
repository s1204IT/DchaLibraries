package com.android.okhttp.internal.spdy;

import com.android.okio.BufferedSource;
import com.android.okio.Deadline;
import com.android.okio.OkBuffer;
import com.android.okio.Sink;
import com.android.okio.Source;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public final class SpdyStream {
    static final boolean $assertionsDisabled;
    long bytesLeftInWriteWindow;
    private final SpdyConnection connection;
    private final int id;
    private final int priority;
    private final List<Header> requestHeaders;
    private List<Header> responseHeaders;
    final SpdyDataSink sink;
    private final SpdyDataSource source;
    long unacknowledgedBytesRead = 0;
    private long readTimeoutMillis = 0;
    private ErrorCode errorCode = null;

    static {
        $assertionsDisabled = !SpdyStream.class.desiredAssertionStatus();
    }

    SpdyStream(int id, SpdyConnection connection, boolean outFinished, boolean inFinished, int priority, List<Header> requestHeaders) {
        if (connection == null) {
            throw new NullPointerException("connection == null");
        }
        if (requestHeaders == null) {
            throw new NullPointerException("requestHeaders == null");
        }
        this.id = id;
        this.connection = connection;
        this.bytesLeftInWriteWindow = connection.peerSettings.getInitialWindowSize(65536);
        this.source = new SpdyDataSource(connection.okHttpSettings.getInitialWindowSize(65536));
        this.sink = new SpdyDataSink();
        this.source.finished = inFinished;
        this.sink.finished = outFinished;
        this.priority = priority;
        this.requestHeaders = requestHeaders;
    }

    public int getId() {
        return this.id;
    }

    public synchronized boolean isOpen() {
        boolean z = false;
        synchronized (this) {
            if (this.errorCode == null) {
                if ((this.source.finished || this.source.closed) && (this.sink.finished || this.sink.closed)) {
                    if (this.responseHeaders == null) {
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    public boolean isLocallyInitiated() {
        boolean streamIsClient = (this.id & 1) == 1;
        return this.connection.client == streamIsClient;
    }

    public SpdyConnection getConnection() {
        return this.connection;
    }

    public List<Header> getRequestHeaders() {
        return this.requestHeaders;
    }

    public synchronized List<Header> getResponseHeaders() throws IOException {
        long remaining = 0;
        long start = 0;
        if (this.readTimeoutMillis != 0) {
            start = System.nanoTime() / 1000000;
            remaining = this.readTimeoutMillis;
        }
        while (this.responseHeaders == null && this.errorCode == null) {
            try {
                if (this.readTimeoutMillis == 0) {
                    wait();
                } else if (remaining > 0) {
                    wait(remaining);
                    remaining = (this.readTimeoutMillis + start) - (System.nanoTime() / 1000000);
                } else {
                    throw new SocketTimeoutException("Read response header timeout. readTimeoutMillis: " + this.readTimeoutMillis);
                }
            } catch (InterruptedException e) {
                InterruptedIOException rethrow = new InterruptedIOException();
                rethrow.initCause(e);
                throw rethrow;
            }
        }
        if (this.responseHeaders != null) {
        } else {
            throw new IOException("stream was reset: " + this.errorCode);
        }
        return this.responseHeaders;
    }

    public synchronized ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public void reply(List<Header> responseHeaders, boolean out) throws IOException {
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        boolean outFinished = false;
        synchronized (this) {
            if (responseHeaders == null) {
                throw new NullPointerException("responseHeaders == null");
            }
            if (this.responseHeaders != null) {
                throw new IllegalStateException("reply already sent");
            }
            this.responseHeaders = responseHeaders;
            if (!out) {
                this.sink.finished = true;
                outFinished = true;
            }
        }
        this.connection.writeSynReply(this.id, outFinished, responseHeaders);
        if (outFinished) {
            this.connection.flush();
        }
    }

    public void setReadTimeout(long readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public long getReadTimeoutMillis() {
        return this.readTimeoutMillis;
    }

    public Source getSource() {
        return this.source;
    }

    public Sink getSink() {
        synchronized (this) {
            if (this.responseHeaders == null && !isLocallyInitiated()) {
                throw new IllegalStateException("reply before requesting the sink");
            }
        }
        return this.sink;
    }

    public void close(ErrorCode rstStatusCode) throws IOException {
        if (closeInternal(rstStatusCode)) {
            this.connection.writeSynReset(this.id, rstStatusCode);
        }
    }

    public void closeLater(ErrorCode errorCode) {
        if (closeInternal(errorCode)) {
            this.connection.writeSynResetLater(this.id, errorCode);
        }
    }

    private boolean closeInternal(ErrorCode errorCode) {
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        synchronized (this) {
            if (this.errorCode != null) {
                return false;
            }
            if (this.source.finished && this.sink.finished) {
                return false;
            }
            this.errorCode = errorCode;
            notifyAll();
            this.connection.removeStream(this.id);
            return true;
        }
    }

    void receiveHeaders(List<Header> headers, HeadersMode headersMode) {
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        ErrorCode errorCode = null;
        boolean open = true;
        synchronized (this) {
            if (this.responseHeaders == null) {
                if (headersMode.failIfHeadersAbsent()) {
                    errorCode = ErrorCode.PROTOCOL_ERROR;
                } else {
                    this.responseHeaders = headers;
                    open = isOpen();
                    notifyAll();
                }
            } else if (headersMode.failIfHeadersPresent()) {
                errorCode = ErrorCode.STREAM_IN_USE;
            } else {
                List<Header> newHeaders = new ArrayList<>();
                newHeaders.addAll(this.responseHeaders);
                newHeaders.addAll(headers);
                this.responseHeaders = newHeaders;
            }
        }
        if (errorCode != null) {
            closeLater(errorCode);
        } else if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    void receiveData(BufferedSource in, int length) throws IOException {
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        this.source.receive(in, length);
    }

    void receiveFin() {
        boolean open;
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        synchronized (this) {
            this.source.finished = true;
            open = isOpen();
            notifyAll();
        }
        if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    synchronized void receiveRstStream(ErrorCode errorCode) {
        if (this.errorCode == null) {
            this.errorCode = errorCode;
            notifyAll();
        }
    }

    int getPriority() {
        return this.priority;
    }

    private final class SpdyDataSource implements Source {
        static final boolean $assertionsDisabled;
        private boolean closed;
        private boolean finished;
        private final long maxByteCount;
        private final OkBuffer readBuffer;
        private final OkBuffer receiveBuffer;

        static {
            $assertionsDisabled = !SpdyStream.class.desiredAssertionStatus();
        }

        private SpdyDataSource(long maxByteCount) {
            this.receiveBuffer = new OkBuffer();
            this.readBuffer = new OkBuffer();
            this.maxByteCount = maxByteCount;
        }

        @Override
        public long read(OkBuffer sink, long byteCount) throws IOException {
            long read;
            if (byteCount < 0) {
                throw new IllegalArgumentException("byteCount < 0: " + byteCount);
            }
            synchronized (SpdyStream.this) {
                waitUntilReadable();
                checkNotClosed();
                if (this.readBuffer.size() == 0) {
                    read = -1;
                } else {
                    read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
                    SpdyStream.this.unacknowledgedBytesRead += read;
                    if (SpdyStream.this.unacknowledgedBytesRead >= SpdyStream.this.connection.peerSettings.getInitialWindowSize(65536) / 2) {
                        SpdyStream.this.connection.writeWindowUpdateLater(SpdyStream.this.id, SpdyStream.this.unacknowledgedBytesRead);
                        SpdyStream.this.unacknowledgedBytesRead = 0L;
                    }
                    synchronized (SpdyStream.this.connection) {
                        SpdyStream.this.connection.unacknowledgedBytesRead += read;
                        if (SpdyStream.this.connection.unacknowledgedBytesRead >= SpdyStream.this.connection.peerSettings.getInitialWindowSize(65536) / 2) {
                            SpdyStream.this.connection.writeWindowUpdateLater(0, SpdyStream.this.connection.unacknowledgedBytesRead);
                            SpdyStream.this.connection.unacknowledgedBytesRead = 0L;
                        }
                    }
                }
            }
            return read;
        }

        private void waitUntilReadable() throws IOException {
            long start = 0;
            long remaining = 0;
            if (SpdyStream.this.readTimeoutMillis != 0) {
                start = System.nanoTime() / 1000000;
                remaining = SpdyStream.this.readTimeoutMillis;
            }
            while (this.readBuffer.size() == 0 && !this.finished && !this.closed && SpdyStream.this.errorCode == null) {
                try {
                    if (SpdyStream.this.readTimeoutMillis == 0) {
                        SpdyStream.this.wait();
                    } else if (remaining > 0) {
                        SpdyStream.this.wait(remaining);
                        remaining = (SpdyStream.this.readTimeoutMillis + start) - (System.nanoTime() / 1000000);
                    } else {
                        throw new SocketTimeoutException("Read timed out");
                    }
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
        }

        void receive(BufferedSource in, long byteCount) throws IOException {
            boolean finished;
            boolean flowControlError;
            if (!$assertionsDisabled && Thread.holdsLock(SpdyStream.this)) {
                throw new AssertionError();
            }
            while (byteCount > 0) {
                synchronized (SpdyStream.this) {
                    finished = this.finished;
                    flowControlError = this.readBuffer.size() + byteCount > this.maxByteCount;
                }
                if (flowControlError) {
                    in.skip(byteCount);
                    SpdyStream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
                    return;
                }
                if (finished) {
                    in.skip(byteCount);
                    return;
                }
                long read = in.read(this.receiveBuffer, byteCount);
                if (read == -1) {
                    throw new EOFException();
                }
                byteCount -= read;
                synchronized (SpdyStream.this) {
                    boolean wasEmpty = this.readBuffer.size() == 0;
                    this.readBuffer.write(this.receiveBuffer, this.receiveBuffer.size());
                    if (wasEmpty) {
                        SpdyStream.this.notifyAll();
                    }
                }
            }
        }

        @Override
        public Source mo2deadline(Deadline deadline) {
            return this;
        }

        @Override
        public void close() throws IOException {
            synchronized (SpdyStream.this) {
                this.closed = true;
                this.readBuffer.clear();
                SpdyStream.this.notifyAll();
            }
            SpdyStream.this.cancelStreamIfNecessary();
        }

        private void checkNotClosed() throws IOException {
            if (!this.closed) {
                if (SpdyStream.this.errorCode != null) {
                    throw new IOException("stream was reset: " + SpdyStream.this.errorCode);
                }
                return;
            }
            throw new IOException("stream closed");
        }
    }

    private void cancelStreamIfNecessary() throws IOException {
        boolean cancel;
        boolean open;
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        synchronized (this) {
            cancel = !this.source.finished && this.source.closed && (this.sink.finished || this.sink.closed);
            open = isOpen();
        }
        if (cancel) {
            close(ErrorCode.CANCEL);
        } else if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    final class SpdyDataSink implements Sink {
        static final boolean $assertionsDisabled;
        private boolean closed;
        private boolean finished;

        static {
            $assertionsDisabled = !SpdyStream.class.desiredAssertionStatus();
        }

        SpdyDataSink() {
        }

        @Override
        public void write(OkBuffer source, long byteCount) throws IOException {
            long toWrite;
            if (!$assertionsDisabled && Thread.holdsLock(SpdyStream.this)) {
                throw new AssertionError();
            }
            while (byteCount > 0) {
                synchronized (SpdyStream.this) {
                    while (SpdyStream.this.bytesLeftInWriteWindow <= 0) {
                        try {
                            SpdyStream.this.wait();
                        } catch (InterruptedException e) {
                            throw new InterruptedIOException();
                        }
                    }
                    SpdyStream.this.checkOutNotClosed();
                    toWrite = Math.min(SpdyStream.this.bytesLeftInWriteWindow, byteCount);
                    SpdyStream.this.bytesLeftInWriteWindow -= toWrite;
                }
                byteCount -= toWrite;
                SpdyStream.this.connection.writeData(SpdyStream.this.id, false, source, toWrite);
            }
        }

        @Override
        public void flush() throws IOException {
            if (!$assertionsDisabled && Thread.holdsLock(SpdyStream.this)) {
                throw new AssertionError();
            }
            synchronized (SpdyStream.this) {
                SpdyStream.this.checkOutNotClosed();
            }
            SpdyStream.this.connection.flush();
        }

        @Override
        public Sink mo2deadline(Deadline deadline) {
            return this;
        }

        @Override
        public void close() throws IOException {
            if (!$assertionsDisabled && Thread.holdsLock(SpdyStream.this)) {
                throw new AssertionError();
            }
            synchronized (SpdyStream.this) {
                if (!this.closed) {
                    if (!SpdyStream.this.sink.finished) {
                        SpdyStream.this.connection.writeData(SpdyStream.this.id, true, null, 0L);
                    }
                    synchronized (SpdyStream.this) {
                        this.closed = true;
                    }
                    SpdyStream.this.connection.flush();
                    SpdyStream.this.cancelStreamIfNecessary();
                }
            }
        }
    }

    void addBytesToWriteWindow(long delta) {
        this.bytesLeftInWriteWindow += delta;
        if (delta > 0) {
            notifyAll();
        }
    }

    private void checkOutNotClosed() throws IOException {
        if (!this.sink.closed) {
            if (this.sink.finished) {
                throw new IOException("stream finished");
            }
            if (this.errorCode != null) {
                throw new IOException("stream was reset: " + this.errorCode);
            }
            return;
        }
        throw new IOException("stream closed");
    }
}
