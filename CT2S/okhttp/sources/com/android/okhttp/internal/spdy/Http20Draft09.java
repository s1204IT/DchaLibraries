package com.android.okhttp.internal.spdy;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.spdy.FrameReader;
import com.android.okhttp.internal.spdy.HpackDraft05;
import com.android.okio.BufferedSink;
import com.android.okio.BufferedSource;
import com.android.okio.ByteString;
import com.android.okio.Deadline;
import com.android.okio.OkBuffer;
import com.android.okio.Source;
import java.io.IOException;
import java.util.List;

public final class Http20Draft09 implements Variant {
    private static final ByteString CONNECTION_HEADER = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
    static final byte FLAG_ACK = 1;
    static final byte FLAG_END_HEADERS = 4;
    static final byte FLAG_END_PUSH_PROMISE = 4;
    static final byte FLAG_END_STREAM = 1;
    static final byte FLAG_NONE = 0;
    static final byte FLAG_PRIORITY = 8;
    static final byte TYPE_CONTINUATION = 10;
    static final byte TYPE_DATA = 0;
    static final byte TYPE_GOAWAY = 7;
    static final byte TYPE_HEADERS = 1;
    static final byte TYPE_PING = 6;
    static final byte TYPE_PRIORITY = 2;
    static final byte TYPE_PUSH_PROMISE = 5;
    static final byte TYPE_RST_STREAM = 3;
    static final byte TYPE_SETTINGS = 4;
    static final byte TYPE_WINDOW_UPDATE = 9;

    @Override
    public Protocol getProtocol() {
        return Protocol.HTTP_2;
    }

    @Override
    public FrameReader newReader(BufferedSource source, boolean client) {
        return new Reader(source, 4096, client);
    }

    @Override
    public FrameWriter newWriter(BufferedSink sink, boolean client) {
        return new Writer(sink, client);
    }

    @Override
    public int maxFrameSize() {
        return 16383;
    }

    static final class Reader implements FrameReader {
        private final boolean client;
        private final ContinuationSource continuation;
        final HpackDraft05.Reader hpackReader;
        private final BufferedSource source;

        Reader(BufferedSource source, int headerTableSize, boolean client) {
            this.source = source;
            this.client = client;
            this.continuation = new ContinuationSource(this.source);
            this.hpackReader = new HpackDraft05.Reader(client, headerTableSize, this.continuation);
        }

        @Override
        public void readConnectionHeader() throws IOException {
            if (!this.client) {
                ByteString connectionHeader = this.source.readByteString(Http20Draft09.CONNECTION_HEADER.size());
                if (!Http20Draft09.CONNECTION_HEADER.equals(connectionHeader)) {
                    throw Http20Draft09.ioException("Expected a connection header but was %s", connectionHeader.utf8());
                }
            }
        }

        @Override
        public boolean nextFrame(FrameReader.Handler handler) throws IOException {
            try {
                int w1 = this.source.readInt();
                int w2 = this.source.readInt();
                short length = (short) ((1073676288 & w1) >> 16);
                byte type = (byte) ((65280 & w1) >> 8);
                byte flags = (byte) (w1 & 255);
                int streamId = w2 & Integer.MAX_VALUE;
                switch (type) {
                    case 0:
                        readData(handler, length, flags, streamId);
                        return true;
                    case 1:
                        readHeaders(handler, length, flags, streamId);
                        return true;
                    case 2:
                        readPriority(handler, length, flags, streamId);
                        return true;
                    case 3:
                        readRstStream(handler, length, flags, streamId);
                        return true;
                    case 4:
                        readSettings(handler, length, flags, streamId);
                        return true;
                    case 5:
                        readPushPromise(handler, length, flags, streamId);
                        return true;
                    case 6:
                        readPing(handler, length, flags, streamId);
                        return true;
                    case 7:
                        readGoAway(handler, length, flags, streamId);
                        return true;
                    case 8:
                    default:
                        this.source.skip(length);
                        return true;
                    case 9:
                        readWindowUpdate(handler, length, flags, streamId);
                        return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        private void readHeaders(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (streamId == 0) {
                throw Http20Draft09.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0", new Object[0]);
            }
            boolean endStream = (flags & 1) != 0;
            int priority = -1;
            if ((flags & Http20Draft09.FLAG_PRIORITY) != 0) {
                priority = this.source.readInt() & Integer.MAX_VALUE;
                length = (short) (length - 4);
            }
            List<Header> headerBlock = readHeaderBlock(length, flags, streamId);
            handler.headers(false, endStream, streamId, -1, priority, headerBlock, HeadersMode.HTTP_20_HEADERS);
        }

        private List<Header> readHeaderBlock(short length, byte flags, int streamId) throws IOException {
            ContinuationSource continuationSource = this.continuation;
            this.continuation.left = length;
            continuationSource.length = length;
            this.continuation.flags = flags;
            this.continuation.streamId = streamId;
            this.hpackReader.readHeaders();
            this.hpackReader.emitReferenceSet();
            return this.hpackReader.getAndReset();
        }

        private void readData(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            boolean inFinished = (flags & 1) != 0;
            handler.data(inFinished, streamId, this.source, length);
        }

        private void readPriority(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (length != 4) {
                throw Http20Draft09.ioException("TYPE_PRIORITY length: %d != 4", Short.valueOf(length));
            }
            if (streamId == 0) {
                throw Http20Draft09.ioException("TYPE_PRIORITY streamId == 0", new Object[0]);
            }
            int w1 = this.source.readInt();
            int priority = w1 & Integer.MAX_VALUE;
            handler.priority(streamId, priority);
        }

        private void readRstStream(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (length != 4) {
                throw Http20Draft09.ioException("TYPE_RST_STREAM length: %d != 4", Short.valueOf(length));
            }
            if (streamId == 0) {
                throw Http20Draft09.ioException("TYPE_RST_STREAM streamId == 0", new Object[0]);
            }
            int errorCodeInt = this.source.readInt();
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw Http20Draft09.ioException("TYPE_RST_STREAM unexpected error code: %d", Integer.valueOf(errorCodeInt));
            }
            handler.rstStream(streamId, errorCode);
        }

        private void readSettings(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (streamId != 0) {
                throw Http20Draft09.ioException("TYPE_SETTINGS streamId != 0", new Object[0]);
            }
            if ((flags & 1) != 0) {
                if (length != 0) {
                    throw Http20Draft09.ioException("FRAME_SIZE_ERROR ack frame should be empty!", new Object[0]);
                }
                handler.ackSettings();
            } else {
                if (length % 8 != 0) {
                    throw Http20Draft09.ioException("TYPE_SETTINGS length %% 8 != 0: %s", Short.valueOf(length));
                }
                Settings settings = new Settings();
                for (int i = 0; i < length; i += 8) {
                    int w1 = this.source.readInt();
                    int value = this.source.readInt();
                    int id = w1 & 16777215;
                    settings.set(id, 0, value);
                }
                handler.settings(false, settings);
                if (settings.getHeaderTableSize() >= 0) {
                    this.hpackReader.maxHeaderTableByteCount(settings.getHeaderTableSize());
                }
            }
        }

        private void readPushPromise(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (streamId == 0) {
                throw Http20Draft09.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0", new Object[0]);
            }
            int promisedStreamId = this.source.readInt() & Integer.MAX_VALUE;
            List<Header> headerBlock = readHeaderBlock((short) (length - 4), flags, streamId);
            handler.pushPromise(streamId, promisedStreamId, headerBlock);
        }

        private void readPing(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (length != 8) {
                throw Http20Draft09.ioException("TYPE_PING length != 8: %s", Short.valueOf(length));
            }
            if (streamId != 0) {
                throw Http20Draft09.ioException("TYPE_PING streamId != 0", new Object[0]);
            }
            int payload1 = this.source.readInt();
            int payload2 = this.source.readInt();
            boolean ack = (flags & 1) != 0;
            handler.ping(ack, payload1, payload2);
        }

        private void readGoAway(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (length < 8) {
                throw Http20Draft09.ioException("TYPE_GOAWAY length < 8: %s", Short.valueOf(length));
            }
            if (streamId != 0) {
                throw Http20Draft09.ioException("TYPE_GOAWAY streamId != 0", new Object[0]);
            }
            int lastStreamId = this.source.readInt();
            int errorCodeInt = this.source.readInt();
            int opaqueDataLength = length - 8;
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw Http20Draft09.ioException("TYPE_GOAWAY unexpected error code: %d", Integer.valueOf(errorCodeInt));
            }
            ByteString debugData = ByteString.EMPTY;
            if (opaqueDataLength > 0) {
                debugData = this.source.readByteString(opaqueDataLength);
            }
            handler.goAway(lastStreamId, errorCode, debugData);
        }

        private void readWindowUpdate(FrameReader.Handler handler, short length, byte flags, int streamId) throws IOException {
            if (length != 4) {
                throw Http20Draft09.ioException("TYPE_WINDOW_UPDATE length !=4: %s", Short.valueOf(length));
            }
            long increment = this.source.readInt() & Integer.MAX_VALUE;
            if (increment == 0) {
                throw Http20Draft09.ioException("windowSizeIncrement was 0", Long.valueOf(increment));
            }
            handler.windowUpdate(streamId, increment);
        }

        @Override
        public void close() throws IOException {
            this.source.close();
        }
    }

    static final class Writer implements FrameWriter {
        private final boolean client;
        private boolean closed;
        private final OkBuffer hpackBuffer = new OkBuffer();
        private final HpackDraft05.Writer hpackWriter = new HpackDraft05.Writer(this.hpackBuffer);
        private final BufferedSink sink;

        Writer(BufferedSink sink, boolean client) {
            this.sink = sink;
            this.client = client;
        }

        @Override
        public synchronized void flush() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.sink.flush();
        }

        @Override
        public synchronized void ackSettings() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            frameHeader(0, (byte) 4, (byte) 1, 0);
            this.sink.flush();
        }

        @Override
        public synchronized void connectionHeader() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (this.client) {
                this.sink.write(Http20Draft09.CONNECTION_HEADER.toByteArray());
                this.sink.flush();
            }
        }

        @Override
        public synchronized void synStream(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, int priority, int slot, List<Header> headerBlock) throws IOException {
            if (inFinished) {
                throw new UnsupportedOperationException();
            }
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(outFinished, streamId, priority, headerBlock);
        }

        @Override
        public synchronized void synReply(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(outFinished, streamId, -1, headerBlock);
        }

        @Override
        public synchronized void headers(int streamId, List<Header> headerBlock) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(false, streamId, -1, headerBlock);
        }

        @Override
        public synchronized void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (this.hpackBuffer.size() != 0) {
                throw new IllegalStateException();
            }
            this.hpackWriter.writeHeaders(requestHeaders);
            int length = (int) (4 + this.hpackBuffer.size());
            frameHeader(length, Http20Draft09.TYPE_PUSH_PROMISE, (byte) 4, streamId);
            this.sink.writeInt(Integer.MAX_VALUE & promisedStreamId);
            this.sink.write(this.hpackBuffer, this.hpackBuffer.size());
        }

        private void headers(boolean outFinished, int streamId, int priority, List<Header> headerBlock) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (this.hpackBuffer.size() != 0) {
                throw new IllegalStateException();
            }
            this.hpackWriter.writeHeaders(headerBlock);
            int length = (int) this.hpackBuffer.size();
            byte flags = outFinished ? (byte) 5 : (byte) 4;
            if (priority != -1) {
                flags = (byte) (flags | Http20Draft09.FLAG_PRIORITY);
            }
            if (priority != -1) {
                length += 4;
            }
            frameHeader(length, (byte) 1, flags, streamId);
            if (priority != -1) {
                this.sink.writeInt(Integer.MAX_VALUE & priority);
            }
            this.sink.write(this.hpackBuffer, this.hpackBuffer.size());
        }

        @Override
        public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.spdyRstCode == -1) {
                throw new IllegalArgumentException();
            }
            frameHeader(4, Http20Draft09.TYPE_RST_STREAM, (byte) 0, streamId);
            this.sink.writeInt(errorCode.httpCode);
            this.sink.flush();
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, OkBuffer source) throws IOException {
            data(outFinished, streamId, source, (int) source.size());
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, OkBuffer source, int byteCount) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            byte flags = outFinished ? (byte) 1 : (byte) 0;
            dataFrame(streamId, flags, source, byteCount);
        }

        void dataFrame(int streamId, byte flags, OkBuffer buffer, int byteCount) throws IOException {
            frameHeader(byteCount, (byte) 0, flags, streamId);
            if (byteCount > 0) {
                this.sink.write(buffer, byteCount);
            }
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            int length = settings.size() * 8;
            frameHeader(length, (byte) 4, (byte) 0, 0);
            for (int i = 0; i < 10; i++) {
                if (settings.isSet(i)) {
                    this.sink.writeInt(16777215 & i);
                    this.sink.writeInt(settings.get(i));
                }
            }
            this.sink.flush();
        }

        @Override
        public synchronized void ping(boolean ack, int payload1, int payload2) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            byte flags = ack ? (byte) 1 : (byte) 0;
            frameHeader(8, Http20Draft09.TYPE_PING, flags, 0);
            this.sink.writeInt(payload1);
            this.sink.writeInt(payload2);
            this.sink.flush();
        }

        @Override
        public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode, byte[] debugData) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.httpCode == -1) {
                throw Http20Draft09.illegalArgument("errorCode.httpCode == -1", new Object[0]);
            }
            int length = debugData.length + 8;
            frameHeader(length, Http20Draft09.TYPE_GOAWAY, (byte) 0, 0);
            this.sink.writeInt(lastGoodStreamId);
            this.sink.writeInt(errorCode.httpCode);
            if (debugData.length > 0) {
                this.sink.write(debugData);
            }
            this.sink.flush();
        }

        @Override
        public synchronized void windowUpdate(int streamId, long windowSizeIncrement) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (windowSizeIncrement == 0 || windowSizeIncrement > 2147483647L) {
                throw Http20Draft09.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", Long.valueOf(windowSizeIncrement));
            }
            frameHeader(4, Http20Draft09.TYPE_WINDOW_UPDATE, (byte) 0, streamId);
            this.sink.writeInt((int) windowSizeIncrement);
            this.sink.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            this.closed = true;
            this.sink.close();
        }

        void frameHeader(int length, byte type, byte flags, int streamId) throws IOException {
            if (length > 16383) {
                throw Http20Draft09.illegalArgument("FRAME_SIZE_ERROR length > 16383: %s", Integer.valueOf(length));
            }
            if ((Integer.MIN_VALUE & streamId) != 0) {
                throw Http20Draft09.illegalArgument("reserved bit set: %s", Integer.valueOf(streamId));
            }
            this.sink.writeInt(((length & 16383) << 16) | ((type & 255) << 8) | (flags & 255));
            this.sink.writeInt(Integer.MAX_VALUE & streamId);
        }
    }

    private static IllegalArgumentException illegalArgument(String message, Object... args) {
        throw new IllegalArgumentException(String.format(message, args));
    }

    private static IOException ioException(String message, Object... args) throws IOException {
        throw new IOException(String.format(message, args));
    }

    static final class ContinuationSource implements Source {
        byte flags;
        int left;
        int length;
        private final BufferedSource source;
        int streamId;

        public ContinuationSource(BufferedSource source) {
            this.source = source;
        }

        @Override
        public long read(OkBuffer sink, long byteCount) throws IOException {
            while (this.left == 0) {
                if ((this.flags & 4) != 0) {
                    return -1L;
                }
                readContinuationHeader();
            }
            long read = this.source.read(sink, Math.min(byteCount, this.left));
            if (read == -1) {
                return -1L;
            }
            this.left = (int) (((long) this.left) - read);
            return read;
        }

        @Override
        public Source mo2deadline(Deadline deadline) {
            this.source.mo2deadline(deadline);
            return this;
        }

        @Override
        public void close() throws IOException {
        }

        private void readContinuationHeader() throws IOException {
            int previousStreamId = this.streamId;
            int w1 = this.source.readInt();
            int w2 = this.source.readInt();
            short s = (short) ((1073676288 & w1) >> 16);
            this.left = s;
            this.length = s;
            byte type = (byte) ((65280 & w1) >> 8);
            this.flags = (byte) (w1 & 255);
            this.streamId = Integer.MAX_VALUE & w2;
            if (type != 10) {
                throw Http20Draft09.ioException("%s != TYPE_CONTINUATION", Byte.valueOf(type));
            }
            if (this.streamId != previousStreamId) {
                throw Http20Draft09.ioException("TYPE_CONTINUATION streamId changed", new Object[0]);
            }
        }
    }
}
