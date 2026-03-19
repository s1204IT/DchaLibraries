package com.android.okhttp.internal.framed;

import com.android.okhttp.okio.AsyncTimeout;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Source;
import com.android.okhttp.okio.Timeout;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public final class FramedStream {

    static final boolean f4assertionsDisabled;
    long bytesLeftInWriteWindow;
    private final FramedConnection connection;
    private final int id;
    private final List<Header> requestHeaders;
    private List<Header> responseHeaders;
    final FramedDataSink sink;
    private final FramedDataSource source;
    long unacknowledgedBytesRead = 0;
    private final StreamTimeout readTimeout = new StreamTimeout();
    private final StreamTimeout writeTimeout = new StreamTimeout();
    private ErrorCode errorCode = null;

    static {
        f4assertionsDisabled = !FramedStream.class.desiredAssertionStatus();
    }

    FramedStream(int id, FramedConnection connection, boolean outFinished, boolean inFinished, List<Header> list) {
        FramedDataSource framedDataSource = null;
        if (connection == null) {
            throw new NullPointerException("connection == null");
        }
        if (list == null) {
            throw new NullPointerException("requestHeaders == null");
        }
        this.id = id;
        this.connection = connection;
        this.bytesLeftInWriteWindow = connection.peerSettings.getInitialWindowSize(65536);
        this.source = new FramedDataSource(this, connection.okHttpSettings.getInitialWindowSize(65536), framedDataSource);
        this.sink = new FramedDataSink();
        this.source.finished = inFinished;
        this.sink.finished = outFinished;
        this.requestHeaders = list;
    }

    public int getId() {
        return this.id;
    }

    public synchronized boolean isOpen() {
        if (this.errorCode != null) {
            return false;
        }
        if ((this.source.finished || this.source.closed) && (this.sink.finished || this.sink.closed)) {
            if (this.responseHeaders != null) {
                return false;
            }
        }
        return true;
    }

    public boolean isLocallyInitiated() {
        boolean streamIsClient = (this.id & 1) == 1;
        return this.connection.client == streamIsClient;
    }

    public FramedConnection getConnection() {
        return this.connection;
    }

    public List<Header> getRequestHeaders() {
        return this.requestHeaders;
    }

    public synchronized List<Header> getResponseHeaders() throws IOException {
        this.readTimeout.enter();
        while (this.responseHeaders == null && this.errorCode == null) {
            try {
                waitForIo();
            } catch (Throwable th) {
                this.readTimeout.exitAndThrowIfTimedOut();
                throw th;
            }
        }
        this.readTimeout.exitAndThrowIfTimedOut();
        if (this.responseHeaders == null) {
            throw new IOException("stream was reset: " + this.errorCode);
        }
        return this.responseHeaders;
    }

    public synchronized ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public void reply(List<Header> list, boolean out) throws IOException {
        if (!f4assertionsDisabled) {
            if (!(Thread.holdsLock(this) ? false : true)) {
                throw new AssertionError();
            }
        }
        boolean outFinished = false;
        synchronized (this) {
            if (list == null) {
                throw new NullPointerException("responseHeaders == null");
            }
            if (this.responseHeaders != null) {
                throw new IllegalStateException("reply already sent");
            }
            this.responseHeaders = list;
            if (!out) {
                this.sink.finished = true;
                outFinished = true;
            }
        }
        this.connection.writeSynReply(this.id, outFinished, list);
        if (!outFinished) {
            return;
        }
        this.connection.flush();
    }

    public Timeout readTimeout() {
        return this.readTimeout;
    }

    public Timeout writeTimeout() {
        return this.writeTimeout;
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
        if (!closeInternal(rstStatusCode)) {
            return;
        }
        this.connection.writeSynReset(this.id, rstStatusCode);
    }

    public void closeLater(ErrorCode errorCode) {
        if (!closeInternal(errorCode)) {
            return;
        }
        this.connection.writeSynResetLater(this.id, errorCode);
    }

    private boolean closeInternal(ErrorCode errorCode) {
        if (!f4assertionsDisabled) {
            if (!(!Thread.holdsLock(this))) {
                throw new AssertionError();
            }
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

    void receiveHeaders(List<Header> list, HeadersMode headersMode) {
        if (!f4assertionsDisabled) {
            if (!(!Thread.holdsLock(this))) {
                throw new AssertionError();
            }
        }
        ErrorCode errorCode = null;
        boolean open = true;
        synchronized (this) {
            if (this.responseHeaders == null) {
                if (headersMode.failIfHeadersAbsent()) {
                    errorCode = ErrorCode.PROTOCOL_ERROR;
                } else {
                    this.responseHeaders = list;
                    open = isOpen();
                    notifyAll();
                }
            } else if (headersMode.failIfHeadersPresent()) {
                errorCode = ErrorCode.STREAM_IN_USE;
            } else {
                List<com.squareup.okhttp.internal.framed.Header> newHeaders = new ArrayList<>();
                newHeaders.addAll(this.responseHeaders);
                newHeaders.addAll(list);
                this.responseHeaders = newHeaders;
            }
        }
        if (errorCode != null) {
            closeLater(errorCode);
        } else {
            if (open) {
                return;
            }
            this.connection.removeStream(this.id);
        }
    }

    void receiveData(BufferedSource in, int length) throws IOException {
        if (!f4assertionsDisabled) {
            if (!(!Thread.holdsLock(this))) {
                throw new AssertionError();
            }
        }
        this.source.receive(in, length);
    }

    void receiveFin() {
        boolean open;
        if (!f4assertionsDisabled) {
            if (!(Thread.holdsLock(this) ? false : true)) {
                throw new AssertionError();
            }
        }
        synchronized (this) {
            this.source.finished = true;
            open = isOpen();
            notifyAll();
        }
        if (open) {
            return;
        }
        this.connection.removeStream(this.id);
    }

    synchronized void receiveRstStream(ErrorCode errorCode) {
        if (this.errorCode == null) {
            this.errorCode = errorCode;
            notifyAll();
        }
    }

    private final class FramedDataSource implements Source {

        static final boolean f6assertionsDisabled;
        final boolean $assertionsDisabled;
        private boolean closed;
        private boolean finished;
        private final long maxByteCount;
        private final Buffer readBuffer;
        private final Buffer receiveBuffer;

        FramedDataSource(FramedStream this$0, long maxByteCount, FramedDataSource framedDataSource) {
            this(maxByteCount);
        }

        static {
            f6assertionsDisabled = !FramedDataSource.class.desiredAssertionStatus();
        }

        private FramedDataSource(long maxByteCount) {
            this.receiveBuffer = new Buffer();
            this.readBuffer = new Buffer();
            this.maxByteCount = maxByteCount;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            if (byteCount < 0) {
                throw new IllegalArgumentException("byteCount < 0: " + byteCount);
            }
            synchronized (FramedStream.this) {
                waitUntilReadable();
                checkNotClosed();
                if (this.readBuffer.size() == 0) {
                    return -1L;
                }
                long read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
                FramedStream.this.unacknowledgedBytesRead += read;
                if (FramedStream.this.unacknowledgedBytesRead >= FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2) {
                    FramedStream.this.connection.writeWindowUpdateLater(FramedStream.this.id, FramedStream.this.unacknowledgedBytesRead);
                    FramedStream.this.unacknowledgedBytesRead = 0L;
                }
                synchronized (FramedStream.this.connection) {
                    FramedStream.this.connection.unacknowledgedBytesRead += read;
                    if (FramedStream.this.connection.unacknowledgedBytesRead >= FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2) {
                        FramedStream.this.connection.writeWindowUpdateLater(0, FramedStream.this.connection.unacknowledgedBytesRead);
                        FramedStream.this.connection.unacknowledgedBytesRead = 0L;
                    }
                }
                return read;
            }
        }

        private void waitUntilReadable() throws IOException {
            FramedStream.this.readTimeout.enter();
            while (this.readBuffer.size() == 0 && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
                try {
                    FramedStream.this.waitForIo();
                } finally {
                    FramedStream.this.readTimeout.exitAndThrowIfTimedOut();
                }
            }
        }

        void receive(BufferedSource in, long byteCount) throws IOException {
            boolean finished;
            boolean flowControlError;
            if (!f6assertionsDisabled) {
                if (!(!Thread.holdsLock(FramedStream.this))) {
                    throw new AssertionError();
                }
            }
            while (byteCount > 0) {
                synchronized (FramedStream.this) {
                    finished = this.finished;
                    flowControlError = this.readBuffer.size() + byteCount > this.maxByteCount;
                }
                if (flowControlError) {
                    in.skip(byteCount);
                    FramedStream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
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
                synchronized (FramedStream.this) {
                    boolean wasEmpty = this.readBuffer.size() == 0;
                    this.readBuffer.writeAll(this.receiveBuffer);
                    if (wasEmpty) {
                        FramedStream.this.notifyAll();
                    }
                }
            }
        }

        @Override
        public Timeout timeout() {
            return FramedStream.this.readTimeout;
        }

        @Override
        public void close() throws IOException {
            synchronized (FramedStream.this) {
                this.closed = true;
                this.readBuffer.clear();
                FramedStream.this.notifyAll();
            }
            FramedStream.this.cancelStreamIfNecessary();
        }

        private void checkNotClosed() throws IOException {
            if (this.closed) {
                throw new IOException("stream closed");
            }
            if (FramedStream.this.errorCode == null) {
            } else {
                throw new IOException("stream was reset: " + FramedStream.this.errorCode);
            }
        }
    }

    private void cancelStreamIfNecessary() throws IOException {
        boolean cancel;
        boolean open;
        if (!f4assertionsDisabled) {
            if (!(!Thread.holdsLock(this))) {
                throw new AssertionError();
            }
        }
        synchronized (this) {
            if (this.source.finished || !this.source.closed) {
                cancel = false;
            } else {
                cancel = !this.sink.finished ? this.sink.closed : true;
            }
            open = isOpen();
        }
        if (cancel) {
            close(ErrorCode.CANCEL);
        } else {
            if (open) {
                return;
            }
            this.connection.removeStream(this.id);
        }
    }

    final class FramedDataSink implements Sink {

        static final boolean f5assertionsDisabled;
        private static final long EMIT_BUFFER_SIZE = 16384;
        final boolean $assertionsDisabled;
        private boolean closed;
        private boolean finished;
        private final Buffer sendBuffer = new Buffer();

        static {
            f5assertionsDisabled = !FramedDataSink.class.desiredAssertionStatus();
        }

        FramedDataSink() {
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            if (!f5assertionsDisabled) {
                if (!(!Thread.holdsLock(FramedStream.this))) {
                    throw new AssertionError();
                }
            }
            this.sendBuffer.write(source, byteCount);
            while (this.sendBuffer.size() >= EMIT_BUFFER_SIZE) {
                emitDataFrame(false);
            }
        }

        private void emitDataFrame(boolean outFinished) throws IOException {
            long toWrite;
            boolean z = false;
            synchronized (FramedStream.this) {
                FramedStream.this.writeTimeout.enter();
                while (FramedStream.this.bytesLeftInWriteWindow <= 0 && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
                    try {
                        FramedStream.this.waitForIo();
                    } finally {
                    }
                }
                FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
                FramedStream.this.checkOutNotClosed();
                toWrite = Math.min(FramedStream.this.bytesLeftInWriteWindow, this.sendBuffer.size());
                FramedStream.this.bytesLeftInWriteWindow -= toWrite;
            }
            FramedStream.this.writeTimeout.enter();
            try {
                FramedConnection framedConnection = FramedStream.this.connection;
                int i = FramedStream.this.id;
                if (outFinished && toWrite == this.sendBuffer.size()) {
                    z = true;
                }
                framedConnection.writeData(i, z, this.sendBuffer, toWrite);
            } finally {
            }
        }

        @Override
        public void flush() throws IOException {
            if (!f5assertionsDisabled) {
                if (!(!Thread.holdsLock(FramedStream.this))) {
                    throw new AssertionError();
                }
            }
            synchronized (FramedStream.this) {
                FramedStream.this.checkOutNotClosed();
            }
            while (this.sendBuffer.size() > 0) {
                emitDataFrame(false);
                FramedStream.this.connection.flush();
            }
        }

        @Override
        public Timeout timeout() {
            return FramedStream.this.writeTimeout;
        }

        @Override
        public void close() throws IOException {
            if (!f5assertionsDisabled) {
                if (!(!Thread.holdsLock(FramedStream.this))) {
                    throw new AssertionError();
                }
            }
            synchronized (FramedStream.this) {
                if (this.closed) {
                    return;
                }
                if (!FramedStream.this.sink.finished) {
                    if (this.sendBuffer.size() > 0) {
                        while (this.sendBuffer.size() > 0) {
                            emitDataFrame(true);
                        }
                    } else {
                        FramedStream.this.connection.writeData(FramedStream.this.id, true, null, 0L);
                    }
                }
                synchronized (FramedStream.this) {
                    this.closed = true;
                }
                FramedStream.this.connection.flush();
                FramedStream.this.cancelStreamIfNecessary();
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
        if (this.sink.closed) {
            throw new IOException("stream closed");
        }
        if (this.sink.finished) {
            throw new IOException("stream finished");
        }
        if (this.errorCode == null) {
        } else {
            throw new IOException("stream was reset: " + this.errorCode);
        }
    }

    private void waitForIo() throws InterruptedIOException {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    class StreamTimeout extends AsyncTimeout {
        StreamTimeout() {
        }

        @Override
        protected void timedOut() {
            FramedStream.this.closeLater(ErrorCode.CANCEL);
        }

        @Override
        protected IOException newTimeoutException(IOException cause) {
            SocketTimeoutException socketTimeoutException = new SocketTimeoutException("timeout");
            if (cause != null) {
                socketTimeoutException.initCause(cause);
            }
            return socketTimeoutException;
        }

        public void exitAndThrowIfTimedOut() throws IOException {
            if (exit()) {
                throw newTimeoutException(null);
            }
        }
    }
}
