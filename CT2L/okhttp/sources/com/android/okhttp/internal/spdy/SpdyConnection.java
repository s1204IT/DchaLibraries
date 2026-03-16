package com.android.okhttp.internal.spdy;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.NamedRunnable;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.spdy.FrameReader;
import com.android.okio.BufferedSink;
import com.android.okio.BufferedSource;
import com.android.okio.ByteString;
import com.android.okio.OkBuffer;
import com.android.okio.Okio;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SpdyConnection implements Closeable {
    static final boolean $assertionsDisabled;
    private static final ExecutorService executor;
    long bytesLeftInWriteWindow;
    final boolean client;
    private final Set<Integer> currentPushRequests;
    final FrameReader frameReader;
    final FrameWriter frameWriter;
    private final IncomingStreamHandler handler;
    private final String hostName;
    private long idleStartTimeNs;
    private int lastGoodStreamId;
    final long maxFrameSize;
    private int nextPingId;
    private int nextStreamId;
    final Settings okHttpSettings;
    final Settings peerSettings;
    private Map<Integer, Ping> pings;
    final Protocol protocol;
    private final PushObserver pushObserver;
    final Reader readerRunnable;
    private boolean receivedInitialPeerSettings;
    private boolean shutdown;
    private final Map<Integer, SpdyStream> streams;
    long unacknowledgedBytesRead;

    static {
        $assertionsDisabled = !SpdyConnection.class.desiredAssertionStatus();
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp SpdyConnection", true));
    }

    private SpdyConnection(Builder builder) {
        Variant variant;
        this.streams = new HashMap();
        this.idleStartTimeNs = System.nanoTime();
        this.unacknowledgedBytesRead = 0L;
        this.okHttpSettings = new Settings();
        this.peerSettings = new Settings();
        this.receivedInitialPeerSettings = false;
        this.currentPushRequests = new LinkedHashSet();
        this.protocol = builder.protocol;
        this.pushObserver = builder.pushObserver;
        this.client = builder.client;
        this.handler = builder.handler;
        this.nextStreamId = builder.client ? 1 : 2;
        this.nextPingId = builder.client ? 1 : 2;
        if (builder.client) {
            this.okHttpSettings.set(7, 0, 16777216);
        }
        this.hostName = builder.hostName;
        if (this.protocol == Protocol.HTTP_2) {
            variant = new Http20Draft09();
        } else if (this.protocol == Protocol.SPDY_3) {
            variant = new Spdy3();
        } else {
            throw new AssertionError(this.protocol);
        }
        this.bytesLeftInWriteWindow = this.peerSettings.getInitialWindowSize(65536);
        this.frameReader = variant.newReader(builder.source, this.client);
        this.frameWriter = variant.newWriter(builder.sink, this.client);
        this.maxFrameSize = variant.maxFrameSize();
        this.readerRunnable = new Reader();
        new Thread(this.readerRunnable).start();
    }

    public Protocol getProtocol() {
        return this.protocol;
    }

    public synchronized int openStreamCount() {
        return this.streams.size();
    }

    synchronized SpdyStream getStream(int id) {
        return this.streams.get(Integer.valueOf(id));
    }

    synchronized SpdyStream removeStream(int streamId) {
        SpdyStream stream;
        stream = this.streams.remove(Integer.valueOf(streamId));
        if (stream != null && this.streams.isEmpty()) {
            setIdle(true);
        }
        return stream;
    }

    private synchronized void setIdle(boolean value) {
        this.idleStartTimeNs = value ? System.nanoTime() : Long.MAX_VALUE;
    }

    public synchronized boolean isIdle() {
        return this.idleStartTimeNs != Long.MAX_VALUE;
    }

    public synchronized long getIdleStartTimeNs() {
        return this.idleStartTimeNs;
    }

    public SpdyStream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
        if (this.client) {
            throw new IllegalStateException("Client cannot push requests.");
        }
        if (this.protocol != Protocol.HTTP_2) {
            throw new IllegalStateException("protocol != HTTP_2");
        }
        return newStream(associatedStreamId, requestHeaders, out, false);
    }

    public SpdyStream newStream(List<Header> requestHeaders, boolean out, boolean in) throws IOException {
        return newStream(0, requestHeaders, out, in);
    }

    private SpdyStream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out, boolean in) throws IOException {
        int streamId;
        SpdyStream stream;
        boolean outFinished = !out;
        boolean inFinished = !in;
        synchronized (this.frameWriter) {
            synchronized (this) {
                if (this.shutdown) {
                    throw new IOException("shutdown");
                }
                streamId = this.nextStreamId;
                this.nextStreamId += 2;
                stream = new SpdyStream(streamId, this, outFinished, inFinished, -1, requestHeaders);
                if (stream.isOpen()) {
                    this.streams.put(Integer.valueOf(streamId), stream);
                    setIdle(false);
                }
            }
            if (associatedStreamId == 0) {
                this.frameWriter.synStream(outFinished, inFinished, streamId, associatedStreamId, -1, 0, requestHeaders);
            } else {
                if (this.client) {
                    throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
                }
                this.frameWriter.pushPromise(associatedStreamId, streamId, requestHeaders);
            }
        }
        if (!out) {
            this.frameWriter.flush();
        }
        return stream;
    }

    void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) throws IOException {
        this.frameWriter.synReply(outFinished, streamId, alternating);
    }

    public void writeData(int streamId, boolean outFinished, OkBuffer buffer, long byteCount) throws IOException {
        int toWrite;
        if (byteCount == 0) {
            this.frameWriter.data(outFinished, streamId, buffer, 0);
            return;
        }
        while (byteCount > 0) {
            synchronized (this) {
                while (this.bytesLeftInWriteWindow <= 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException();
                    }
                }
                toWrite = (int) Math.min(Math.min(byteCount, this.bytesLeftInWriteWindow), this.maxFrameSize);
                this.bytesLeftInWriteWindow -= (long) toWrite;
            }
            byteCount -= (long) toWrite;
            this.frameWriter.data(outFinished && byteCount == 0, streamId, buffer, toWrite);
        }
    }

    void addBytesToWriteWindow(long delta) {
        this.bytesLeftInWriteWindow += delta;
        if (delta > 0) {
            notifyAll();
        }
    }

    void writeSynResetLater(final int streamId, final ErrorCode errorCode) {
        executor.submit(new NamedRunnable("OkHttp %s stream %d", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            @Override
            public void execute() {
                try {
                    SpdyConnection.this.writeSynReset(streamId, errorCode);
                } catch (IOException e) {
                }
            }
        });
    }

    void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
        this.frameWriter.rstStream(streamId, statusCode);
    }

    void writeWindowUpdateLater(final int streamId, final long unacknowledgedBytesRead) {
        executor.submit(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            @Override
            public void execute() {
                try {
                    SpdyConnection.this.frameWriter.windowUpdate(streamId, unacknowledgedBytesRead);
                } catch (IOException e) {
                }
            }
        });
    }

    public Ping ping() throws IOException {
        int pingId;
        Ping ping = new Ping();
        synchronized (this) {
            if (this.shutdown) {
                throw new IOException("shutdown");
            }
            pingId = this.nextPingId;
            this.nextPingId += 2;
            if (this.pings == null) {
                this.pings = new HashMap();
            }
            this.pings.put(Integer.valueOf(pingId), ping);
        }
        writePing(false, pingId, 1330343787, ping);
        return ping;
    }

    private void writePingLater(final boolean reply, final int payload1, final int payload2, final Ping ping) {
        executor.submit(new NamedRunnable("OkHttp %s ping %08x%08x", new Object[]{this.hostName, Integer.valueOf(payload1), Integer.valueOf(payload2)}) {
            @Override
            public void execute() {
                try {
                    SpdyConnection.this.writePing(reply, payload1, payload2, ping);
                } catch (IOException e) {
                }
            }
        });
    }

    private void writePing(boolean reply, int payload1, int payload2, Ping ping) throws IOException {
        synchronized (this.frameWriter) {
            if (ping != null) {
                ping.send();
                this.frameWriter.ping(reply, payload1, payload2);
            } else {
                this.frameWriter.ping(reply, payload1, payload2);
            }
        }
    }

    private synchronized Ping removePing(int id) {
        return this.pings != null ? this.pings.remove(Integer.valueOf(id)) : null;
    }

    public void flush() throws IOException {
        this.frameWriter.flush();
    }

    public void shutdown(ErrorCode statusCode) throws IOException {
        synchronized (this.frameWriter) {
            synchronized (this) {
                if (!this.shutdown) {
                    this.shutdown = true;
                    int lastGoodStreamId = this.lastGoodStreamId;
                    this.frameWriter.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
    }

    private void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
        if (!$assertionsDisabled && Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        IOException thrown = null;
        try {
            shutdown(connectionCode);
        } catch (IOException e) {
            thrown = e;
        }
        SpdyStream[] streamsToClose = null;
        Ping[] pingsToCancel = null;
        synchronized (this) {
            if (!this.streams.isEmpty()) {
                streamsToClose = (SpdyStream[]) this.streams.values().toArray(new SpdyStream[this.streams.size()]);
                this.streams.clear();
                setIdle(false);
            }
            if (this.pings != null) {
                pingsToCancel = (Ping[]) this.pings.values().toArray(new Ping[this.pings.size()]);
                this.pings = null;
            }
        }
        if (streamsToClose != null) {
            SpdyStream[] arr$ = streamsToClose;
            for (SpdyStream stream : arr$) {
                try {
                    stream.close(streamCode);
                } catch (IOException e2) {
                    if (thrown != null) {
                        thrown = e2;
                    }
                }
            }
        }
        if (pingsToCancel != null) {
            Ping[] arr$2 = pingsToCancel;
            for (Ping ping : arr$2) {
                ping.cancel();
            }
        }
        try {
            this.frameReader.close();
        } catch (IOException e3) {
            thrown = e3;
        }
        try {
            this.frameWriter.close();
        } catch (IOException e4) {
            if (thrown == null) {
                thrown = e4;
            }
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    public void sendConnectionHeader() throws IOException {
        this.frameWriter.connectionHeader();
        this.frameWriter.settings(this.okHttpSettings);
    }

    public static class Builder {
        private boolean client;
        private IncomingStreamHandler handler;
        private String hostName;
        private Protocol protocol;
        private PushObserver pushObserver;
        private BufferedSink sink;
        private BufferedSource source;

        public Builder(boolean client, Socket socket) throws IOException {
            this(((InetSocketAddress) socket.getRemoteSocketAddress()).getHostName(), client, socket);
        }

        public Builder(String hostName, boolean client, Socket socket) throws IOException {
            this.handler = IncomingStreamHandler.REFUSE_INCOMING_STREAMS;
            this.protocol = Protocol.SPDY_3;
            this.pushObserver = PushObserver.CANCEL;
            this.hostName = hostName;
            this.client = client;
            this.source = Okio.buffer(Okio.source(socket.getInputStream()));
            this.sink = Okio.buffer(Okio.sink(socket.getOutputStream()));
        }

        public Builder handler(IncomingStreamHandler handler) {
            this.handler = handler;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder pushObserver(PushObserver pushObserver) {
            this.pushObserver = pushObserver;
            return this;
        }

        public SpdyConnection build() {
            return new SpdyConnection(this);
        }
    }

    class Reader extends NamedRunnable implements FrameReader.Handler {
        private Reader() {
            super("OkHttp %s", SpdyConnection.this.hostName);
        }

        @Override
        protected void execute() {
            ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
            ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;
            try {
                try {
                    if (!SpdyConnection.this.client) {
                        SpdyConnection.this.frameReader.readConnectionHeader();
                    }
                    while (SpdyConnection.this.frameReader.nextFrame(this)) {
                    }
                    connectionErrorCode = ErrorCode.NO_ERROR;
                    streamErrorCode = ErrorCode.CANCEL;
                } catch (IOException e) {
                    ErrorCode connectionErrorCode2 = ErrorCode.PROTOCOL_ERROR;
                    ErrorCode streamErrorCode2 = ErrorCode.PROTOCOL_ERROR;
                    try {
                        SpdyConnection.this.close(connectionErrorCode2, streamErrorCode2);
                    } catch (IOException e2) {
                    }
                }
            } finally {
                try {
                    SpdyConnection.this.close(connectionErrorCode, streamErrorCode);
                } catch (IOException e3) {
                }
            }
        }

        @Override
        public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException {
            if (SpdyConnection.this.pushedStream(streamId)) {
                SpdyConnection.this.pushDataLater(streamId, source, length, inFinished);
                return;
            }
            SpdyStream dataStream = SpdyConnection.this.getStream(streamId);
            if (dataStream == null) {
                SpdyConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
                source.skip(length);
            } else {
                dataStream.receiveData(source, length);
                if (inFinished) {
                    dataStream.receiveFin();
                }
            }
        }

        @Override
        public void headers(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, int priority, List<Header> headerBlock, HeadersMode headersMode) {
            if (SpdyConnection.this.pushedStream(streamId)) {
                SpdyConnection.this.pushHeadersLater(streamId, headerBlock, inFinished);
                return;
            }
            synchronized (SpdyConnection.this) {
                if (!SpdyConnection.this.shutdown) {
                    SpdyStream stream = SpdyConnection.this.getStream(streamId);
                    if (stream == null) {
                        if (!headersMode.failIfStreamAbsent()) {
                            if (streamId > SpdyConnection.this.lastGoodStreamId) {
                                if (streamId % 2 != SpdyConnection.this.nextStreamId % 2) {
                                    final SpdyStream newStream = new SpdyStream(streamId, SpdyConnection.this, outFinished, inFinished, priority, headerBlock);
                                    SpdyConnection.this.lastGoodStreamId = streamId;
                                    SpdyConnection.this.streams.put(Integer.valueOf(streamId), newStream);
                                    SpdyConnection.executor.submit(new NamedRunnable("OkHttp %s stream %d", new Object[]{SpdyConnection.this.hostName, Integer.valueOf(streamId)}) {
                                        @Override
                                        public void execute() {
                                            try {
                                                SpdyConnection.this.handler.receive(newStream);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    });
                                }
                            }
                        } else {
                            SpdyConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
                        }
                    } else if (headersMode.failIfStreamPresent()) {
                        stream.closeLater(ErrorCode.PROTOCOL_ERROR);
                        SpdyConnection.this.removeStream(streamId);
                    } else {
                        stream.receiveHeaders(headerBlock, headersMode);
                        if (inFinished) {
                            stream.receiveFin();
                        }
                    }
                }
            }
        }

        @Override
        public void rstStream(int streamId, ErrorCode errorCode) {
            if (SpdyConnection.this.pushedStream(streamId)) {
                SpdyConnection.this.pushResetLater(streamId, errorCode);
                return;
            }
            SpdyStream rstStream = SpdyConnection.this.removeStream(streamId);
            if (rstStream != null) {
                rstStream.receiveRstStream(errorCode);
            }
        }

        @Override
        public void settings(boolean clearPrevious, Settings newSettings) {
            long delta = 0;
            SpdyStream[] streamsToNotify = null;
            synchronized (SpdyConnection.this) {
                int priorWriteWindowSize = SpdyConnection.this.peerSettings.getInitialWindowSize(65536);
                if (clearPrevious) {
                    SpdyConnection.this.peerSettings.clear();
                }
                SpdyConnection.this.peerSettings.merge(newSettings);
                if (SpdyConnection.this.getProtocol() == Protocol.HTTP_2) {
                    ackSettingsLater();
                }
                int peerInitialWindowSize = SpdyConnection.this.peerSettings.getInitialWindowSize(65536);
                if (peerInitialWindowSize != -1 && peerInitialWindowSize != priorWriteWindowSize) {
                    delta = peerInitialWindowSize - priorWriteWindowSize;
                    if (!SpdyConnection.this.receivedInitialPeerSettings) {
                        SpdyConnection.this.addBytesToWriteWindow(delta);
                        SpdyConnection.this.receivedInitialPeerSettings = true;
                    }
                    if (!SpdyConnection.this.streams.isEmpty()) {
                        streamsToNotify = (SpdyStream[]) SpdyConnection.this.streams.values().toArray(new SpdyStream[SpdyConnection.this.streams.size()]);
                    }
                }
            }
            if (streamsToNotify != null && delta != 0) {
                for (SpdyStream stream : SpdyConnection.this.streams.values()) {
                    synchronized (stream) {
                        stream.addBytesToWriteWindow(delta);
                    }
                }
            }
        }

        private void ackSettingsLater() {
            SpdyConnection.executor.submit(new NamedRunnable("OkHttp %s ACK Settings", SpdyConnection.this.hostName) {
                @Override
                public void execute() {
                    try {
                        SpdyConnection.this.frameWriter.ackSettings();
                    } catch (IOException e) {
                    }
                }
            });
        }

        @Override
        public void ackSettings() {
        }

        @Override
        public void ping(boolean reply, int payload1, int payload2) {
            if (reply) {
                Ping ping = SpdyConnection.this.removePing(payload1);
                if (ping != null) {
                    ping.receive();
                    return;
                }
                return;
            }
            SpdyConnection.this.writePingLater(true, payload1, payload2, null);
        }

        @Override
        public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
            if (debugData.size() > 0) {
            }
            synchronized (SpdyConnection.this) {
                SpdyConnection.this.shutdown = true;
                Iterator<Map.Entry<Integer, SpdyStream>> i = SpdyConnection.this.streams.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<Integer, SpdyStream> entry = i.next();
                    int streamId = entry.getKey().intValue();
                    if (streamId > lastGoodStreamId && entry.getValue().isLocallyInitiated()) {
                        entry.getValue().receiveRstStream(ErrorCode.REFUSED_STREAM);
                        i.remove();
                    }
                }
            }
        }

        @Override
        public void windowUpdate(int streamId, long windowSizeIncrement) {
            if (streamId == 0) {
                synchronized (SpdyConnection.this) {
                    SpdyConnection.this.bytesLeftInWriteWindow += windowSizeIncrement;
                    SpdyConnection.this.notifyAll();
                }
                return;
            }
            SpdyStream stream = SpdyConnection.this.getStream(streamId);
            if (stream != null) {
                synchronized (stream) {
                    stream.addBytesToWriteWindow(windowSizeIncrement);
                }
            }
        }

        @Override
        public void priority(int streamId, int priority) {
        }

        @Override
        public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) {
            SpdyConnection.this.pushRequestLater(promisedStreamId, requestHeaders);
        }
    }

    private boolean pushedStream(int streamId) {
        return this.protocol == Protocol.HTTP_2 && streamId != 0 && (streamId & 1) == 0;
    }

    private void pushRequestLater(final int streamId, final List<Header> requestHeaders) {
        synchronized (this) {
            if (this.currentPushRequests.contains(Integer.valueOf(streamId))) {
                writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
            } else {
                this.currentPushRequests.add(Integer.valueOf(streamId));
                executor.submit(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
                    @Override
                    public void execute() {
                        boolean cancel = SpdyConnection.this.pushObserver.onRequest(streamId, requestHeaders);
                        if (cancel) {
                            try {
                                SpdyConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
                                synchronized (SpdyConnection.this) {
                                    SpdyConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
                                }
                            } catch (IOException e) {
                            }
                        }
                    }
                });
            }
        }
    }

    private void pushHeadersLater(final int streamId, final List<Header> requestHeaders, final boolean inFinished) {
        executor.submit(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            @Override
            public void execute() {
                boolean cancel = SpdyConnection.this.pushObserver.onHeaders(streamId, requestHeaders, inFinished);
                if (cancel) {
                    try {
                        SpdyConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
                    } catch (IOException e) {
                        return;
                    }
                }
                if (cancel || inFinished) {
                    synchronized (SpdyConnection.this) {
                        SpdyConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
                    }
                }
            }
        });
    }

    private void pushDataLater(final int streamId, BufferedSource source, final int byteCount, final boolean inFinished) throws IOException {
        final OkBuffer buffer = new OkBuffer();
        source.require(byteCount);
        source.read(buffer, byteCount);
        if (buffer.size() != byteCount) {
            throw new IOException(buffer.size() + " != " + byteCount);
        }
        executor.submit(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            @Override
            public void execute() {
                try {
                    boolean cancel = SpdyConnection.this.pushObserver.onData(streamId, buffer, byteCount, inFinished);
                    if (cancel) {
                        SpdyConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
                    }
                    if (cancel || inFinished) {
                        synchronized (SpdyConnection.this) {
                            SpdyConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
                        }
                    }
                } catch (IOException e) {
                }
            }
        });
    }

    private void pushResetLater(final int streamId, final ErrorCode errorCode) {
        executor.submit(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[]{this.hostName, Integer.valueOf(streamId)}) {
            @Override
            public void execute() {
                SpdyConnection.this.pushObserver.onReset(streamId, errorCode);
                synchronized (SpdyConnection.this) {
                    SpdyConnection.this.currentPushRequests.remove(Integer.valueOf(streamId));
                }
            }
        });
    }
}
