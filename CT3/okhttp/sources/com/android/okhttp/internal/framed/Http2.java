package com.android.okhttp.internal.framed;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.framed.FrameReader;
import com.android.okhttp.internal.framed.Hpack;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.ByteString;
import com.android.okhttp.okio.Source;
import com.android.okhttp.okio.Timeout;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Http2 implements Variant {
    static final byte FLAG_ACK = 1;
    static final byte FLAG_COMPRESSED = 32;
    static final byte FLAG_END_HEADERS = 4;
    static final byte FLAG_END_PUSH_PROMISE = 4;
    static final byte FLAG_END_STREAM = 1;
    static final byte FLAG_NONE = 0;
    static final byte FLAG_PADDED = 8;
    static final byte FLAG_PRIORITY = 32;
    static final int INITIAL_MAX_FRAME_SIZE = 16384;
    static final byte TYPE_CONTINUATION = 9;
    static final byte TYPE_DATA = 0;
    static final byte TYPE_GOAWAY = 7;
    static final byte TYPE_HEADERS = 1;
    static final byte TYPE_PING = 6;
    static final byte TYPE_PRIORITY = 2;
    static final byte TYPE_PUSH_PROMISE = 5;
    static final byte TYPE_RST_STREAM = 3;
    static final byte TYPE_SETTINGS = 4;
    static final byte TYPE_WINDOW_UPDATE = 8;
    private static final Logger logger = Logger.getLogger(FrameLogger.class.getName());
    private static final ByteString CONNECTION_PREFACE = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");

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

    static final class Reader implements FrameReader {
        private final boolean client;
        private final ContinuationSource continuation;
        final Hpack.Reader hpackReader;
        private final BufferedSource source;

        Reader(BufferedSource source, int headerTableSize, boolean client) {
            this.source = source;
            this.client = client;
            this.continuation = new ContinuationSource(this.source);
            this.hpackReader = new Hpack.Reader(headerTableSize, this.continuation);
        }

        @Override
        public void readConnectionPreface() throws IOException {
            if (this.client) {
                return;
            }
            ByteString connectionPreface = this.source.readByteString(Http2.CONNECTION_PREFACE.size());
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(String.format("<< CONNECTION %s", connectionPreface.hex()));
            }
            if (Http2.CONNECTION_PREFACE.equals(connectionPreface)) {
            } else {
                throw Http2.ioException("Expected a connection header but was %s", connectionPreface.utf8());
            }
        }

        @Override
        public boolean nextFrame(FrameReader.Handler handler) throws IOException {
            try {
                this.source.require(9L);
                int length = Http2.readMedium(this.source);
                if (length < 0 || length > Http2.INITIAL_MAX_FRAME_SIZE) {
                    throw Http2.ioException("FRAME_SIZE_ERROR: %s", Integer.valueOf(length));
                }
                byte type = (byte) (this.source.readByte() & 255);
                byte flags = (byte) (this.source.readByte() & 255);
                int streamId = this.source.readInt() & Integer.MAX_VALUE;
                if (Http2.logger.isLoggable(Level.FINE)) {
                    Http2.logger.fine(FrameLogger.formatHeader(true, streamId, length, type, flags));
                }
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
                        readWindowUpdate(handler, length, flags, streamId);
                        return true;
                    default:
                        this.source.skip(length);
                        return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        private void readHeaders(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (streamId == 0) {
                throw Http2.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0", new Object[0]);
            }
            boolean endStream = (flags & 1) != 0;
            short s = (flags & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            if ((flags & 32) != 0) {
                readPriority(handler, streamId);
                length -= 5;
            }
            handler.headers(false, endStream, streamId, -1, readHeaderBlock(Http2.lengthWithoutPadding(length, flags, s), s, flags, streamId), HeadersMode.HTTP_20_HEADERS);
        }

        private List<Header> readHeaderBlock(int length, short padding, byte flags, int streamId) throws IOException {
            ContinuationSource continuationSource = this.continuation;
            this.continuation.left = length;
            continuationSource.length = length;
            this.continuation.padding = padding;
            this.continuation.flags = flags;
            this.continuation.streamId = streamId;
            this.hpackReader.readHeaders();
            return this.hpackReader.getAndResetHeaderList();
        }

        private void readData(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            boolean inFinished = (flags & 1) != 0;
            boolean gzipped = (flags & 32) != 0;
            if (gzipped) {
                throw Http2.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA", new Object[0]);
            }
            short padding = (flags & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            handler.data(inFinished, streamId, this.source, Http2.lengthWithoutPadding(length, flags, padding));
            this.source.skip(padding);
        }

        private void readPriority(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (length != 5) {
                throw Http2.ioException("TYPE_PRIORITY length: %d != 5", Integer.valueOf(length));
            }
            if (streamId == 0) {
                throw Http2.ioException("TYPE_PRIORITY streamId == 0", new Object[0]);
            }
            readPriority(handler, streamId);
        }

        private void readPriority(FrameReader.Handler handler, int streamId) throws IOException {
            int w1 = this.source.readInt();
            boolean exclusive = (Integer.MIN_VALUE & w1) != 0;
            int streamDependency = w1 & Integer.MAX_VALUE;
            int weight = (this.source.readByte() & 255) + 1;
            handler.priority(streamId, streamDependency, weight, exclusive);
        }

        private void readRstStream(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (length != 4) {
                throw Http2.ioException("TYPE_RST_STREAM length: %d != 4", Integer.valueOf(length));
            }
            if (streamId == 0) {
                throw Http2.ioException("TYPE_RST_STREAM streamId == 0", new Object[0]);
            }
            int errorCodeInt = this.source.readInt();
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw Http2.ioException("TYPE_RST_STREAM unexpected error code: %d", Integer.valueOf(errorCodeInt));
            }
            handler.rstStream(streamId, errorCode);
        }

        private void readSettings(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (streamId != 0) {
                throw Http2.ioException("TYPE_SETTINGS streamId != 0", new Object[0]);
            }
            if ((flags & 1) != 0) {
                if (length != 0) {
                    throw Http2.ioException("FRAME_SIZE_ERROR ack frame should be empty!", new Object[0]);
                }
                handler.ackSettings();
                return;
            }
            if (length % 6 != 0) {
                throw Http2.ioException("TYPE_SETTINGS length %% 6 != 0: %s", Integer.valueOf(length));
            }
            Settings settings = new Settings();
            for (int i = 0; i < length; i += 6) {
                short id = this.source.readShort();
                int value = this.source.readInt();
                switch (id) {
                    case 1:
                    case 6:
                        break;
                    case 2:
                        if (value != 0 && value != 1) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1", new Object[0]);
                        }
                        break;
                        break;
                    case 3:
                        id = 4;
                        break;
                    case 4:
                        id = 7;
                        if (value < 0) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1", new Object[0]);
                        }
                        break;
                        break;
                    case 5:
                        if (value < Http2.INITIAL_MAX_FRAME_SIZE || value > 16777215) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", Integer.valueOf(value));
                        }
                        break;
                        break;
                    default:
                        throw Http2.ioException("PROTOCOL_ERROR invalid settings id: %s", Short.valueOf(id));
                }
                settings.set(id, 0, value);
            }
            handler.settings(false, settings);
            if (settings.getHeaderTableSize() < 0) {
                return;
            }
            this.hpackReader.headerTableSizeSetting(settings.getHeaderTableSize());
        }

        private void readPushPromise(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (streamId == 0) {
                throw Http2.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0", new Object[0]);
            }
            short s = (flags & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            int promisedStreamId = this.source.readInt() & Integer.MAX_VALUE;
            handler.pushPromise(streamId, promisedStreamId, readHeaderBlock(Http2.lengthWithoutPadding(length - 4, flags, s), s, flags, streamId));
        }

        private void readPing(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (length != 8) {
                throw Http2.ioException("TYPE_PING length != 8: %s", Integer.valueOf(length));
            }
            if (streamId != 0) {
                throw Http2.ioException("TYPE_PING streamId != 0", new Object[0]);
            }
            int payload1 = this.source.readInt();
            int payload2 = this.source.readInt();
            boolean ack = (flags & 1) != 0;
            handler.ping(ack, payload1, payload2);
        }

        private void readGoAway(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (length < 8) {
                throw Http2.ioException("TYPE_GOAWAY length < 8: %s", Integer.valueOf(length));
            }
            if (streamId != 0) {
                throw Http2.ioException("TYPE_GOAWAY streamId != 0", new Object[0]);
            }
            int lastStreamId = this.source.readInt();
            int errorCodeInt = this.source.readInt();
            int opaqueDataLength = length - 8;
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw Http2.ioException("TYPE_GOAWAY unexpected error code: %d", Integer.valueOf(errorCodeInt));
            }
            ByteString debugData = ByteString.EMPTY;
            if (opaqueDataLength > 0) {
                debugData = this.source.readByteString(opaqueDataLength);
            }
            handler.goAway(lastStreamId, errorCode, debugData);
        }

        private void readWindowUpdate(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
            if (length != 4) {
                throw Http2.ioException("TYPE_WINDOW_UPDATE length !=4: %s", Integer.valueOf(length));
            }
            long increment = ((long) this.source.readInt()) & 2147483647L;
            if (increment == 0) {
                throw Http2.ioException("windowSizeIncrement was 0", Long.valueOf(increment));
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
        private final Buffer hpackBuffer = new Buffer();
        private final Hpack.Writer hpackWriter = new Hpack.Writer(this.hpackBuffer);
        private int maxFrameSize = Http2.INITIAL_MAX_FRAME_SIZE;
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
        public synchronized void ackSettings(Settings peerSettings) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.maxFrameSize = peerSettings.getMaxFrameSize(this.maxFrameSize);
            frameHeader(0, 0, (byte) 4, (byte) 1);
            this.sink.flush();
        }

        @Override
        public synchronized void connectionPreface() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (this.client) {
                if (Http2.logger.isLoggable(Level.FINE)) {
                    Http2.logger.fine(String.format(">> CONNECTION %s", Http2.CONNECTION_PREFACE.hex()));
                }
                this.sink.write(Http2.CONNECTION_PREFACE.toByteArray());
                this.sink.flush();
            }
        }

        @Override
        public synchronized void synStream(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> list) throws IOException {
            if (inFinished) {
                throw new UnsupportedOperationException();
            }
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(outFinished, streamId, list);
        }

        @Override
        public synchronized void synReply(boolean outFinished, int streamId, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(outFinished, streamId, list);
        }

        @Override
        public synchronized void headers(int streamId, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(false, streamId, list);
        }

        @Override
        public synchronized void pushPromise(int streamId, int promisedStreamId, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.hpackWriter.writeHeaders(list);
            long byteCount = this.hpackBuffer.size();
            int length = (int) Math.min(this.maxFrameSize - 4, byteCount);
            byte flags = byteCount == ((long) length) ? (byte) 4 : (byte) 0;
            frameHeader(streamId, length + 4, Http2.TYPE_PUSH_PROMISE, flags);
            this.sink.writeInt(Integer.MAX_VALUE & promisedStreamId);
            this.sink.write(this.hpackBuffer, length);
            if (byteCount > length) {
                writeContinuationFrames(streamId, byteCount - ((long) length));
            }
        }

        void headers(boolean outFinished, int streamId, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.hpackWriter.writeHeaders(list);
            long byteCount = this.hpackBuffer.size();
            int length = (int) Math.min(this.maxFrameSize, byteCount);
            byte flags = byteCount == ((long) length) ? (byte) 4 : (byte) 0;
            if (outFinished) {
                flags = (byte) (flags | 1);
            }
            frameHeader(streamId, length, (byte) 1, flags);
            this.sink.write(this.hpackBuffer, length);
            if (byteCount > length) {
                writeContinuationFrames(streamId, byteCount - ((long) length));
            }
        }

        private void writeContinuationFrames(int streamId, long byteCount) throws IOException {
            while (byteCount > 0) {
                int length = (int) Math.min(this.maxFrameSize, byteCount);
                byteCount -= (long) length;
                frameHeader(streamId, length, Http2.TYPE_CONTINUATION, byteCount == 0 ? (byte) 4 : (byte) 0);
                this.sink.write(this.hpackBuffer, length);
            }
        }

        @Override
        public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.httpCode == -1) {
                throw new IllegalArgumentException();
            }
            frameHeader(streamId, 4, Http2.TYPE_RST_STREAM, (byte) 0);
            this.sink.writeInt(errorCode.httpCode);
            this.sink.flush();
        }

        @Override
        public int maxDataLength() {
            return this.maxFrameSize;
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, Buffer source, int byteCount) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            byte flags = outFinished ? (byte) 1 : (byte) 0;
            dataFrame(streamId, flags, source, byteCount);
        }

        void dataFrame(int streamId, byte flags, Buffer buffer, int byteCount) throws IOException {
            frameHeader(streamId, byteCount, (byte) 0, flags);
            if (byteCount <= 0) {
                return;
            }
            this.sink.write(buffer, byteCount);
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            int length = settings.size() * 6;
            frameHeader(0, length, (byte) 4, (byte) 0);
            for (int i = 0; i < 10; i++) {
                if (settings.isSet(i)) {
                    int id = i;
                    if (i == 4) {
                        id = 3;
                    } else if (id == 7) {
                        id = 4;
                    }
                    this.sink.writeShort(id);
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
            frameHeader(0, 8, Http2.TYPE_PING, flags);
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
                throw Http2.illegalArgument("errorCode.httpCode == -1", new Object[0]);
            }
            int length = debugData.length + 8;
            frameHeader(0, length, Http2.TYPE_GOAWAY, (byte) 0);
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
                throw Http2.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", Long.valueOf(windowSizeIncrement));
            }
            frameHeader(streamId, 4, (byte) 8, (byte) 0);
            this.sink.writeInt((int) windowSizeIncrement);
            this.sink.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            this.closed = true;
            this.sink.close();
        }

        void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(FrameLogger.formatHeader(false, streamId, length, type, flags));
            }
            if (length > this.maxFrameSize) {
                throw Http2.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", Integer.valueOf(this.maxFrameSize), Integer.valueOf(length));
            }
            if ((Integer.MIN_VALUE & streamId) != 0) {
                throw Http2.illegalArgument("reserved bit set: %s", Integer.valueOf(streamId));
            }
            Http2.writeMedium(this.sink, length);
            this.sink.writeByte(type & 255);
            this.sink.writeByte(flags & 255);
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
        short padding;
        private final BufferedSource source;
        int streamId;

        public ContinuationSource(BufferedSource source) {
            this.source = source;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            while (this.left == 0) {
                this.source.skip(this.padding);
                this.padding = (short) 0;
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
        public Timeout timeout() {
            return this.source.timeout();
        }

        @Override
        public void close() throws IOException {
        }

        private void readContinuationHeader() throws IOException {
            int previousStreamId = this.streamId;
            int medium = Http2.readMedium(this.source);
            this.left = medium;
            this.length = medium;
            byte type = (byte) (this.source.readByte() & 255);
            this.flags = (byte) (this.source.readByte() & 255);
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(FrameLogger.formatHeader(true, this.streamId, this.length, type, this.flags));
            }
            this.streamId = this.source.readInt() & Integer.MAX_VALUE;
            if (type != 9) {
                throw Http2.ioException("%s != TYPE_CONTINUATION", Byte.valueOf(type));
            }
            if (this.streamId != previousStreamId) {
                throw Http2.ioException("TYPE_CONTINUATION streamId changed", new Object[0]);
            }
        }
    }

    private static int lengthWithoutPadding(int length, byte flags, short padding) throws IOException {
        if ((flags & 8) != 0) {
            length--;
        }
        if (padding > length) {
            throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", Short.valueOf(padding), Integer.valueOf(length));
        }
        return (short) (length - padding);
    }

    static final class FrameLogger {
        private static final String[] TYPES = {"DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION"};
        private static final String[] FLAGS = new String[64];
        private static final String[] BINARY = new String[256];

        FrameLogger() {
        }

        static String formatHeader(boolean inbound, int streamId, int length, byte type, byte flags) {
            String formattedType = type < TYPES.length ? TYPES[type] : String.format("0x%02x", Byte.valueOf(type));
            String formattedFlags = formatFlags(type, flags);
            Object[] objArr = new Object[5];
            objArr[0] = inbound ? "<<" : ">>";
            objArr[1] = Integer.valueOf(streamId);
            objArr[2] = Integer.valueOf(length);
            objArr[3] = formattedType;
            objArr[4] = formattedFlags;
            return String.format("%s 0x%08x %5d %-13s %s", objArr);
        }

        static String formatFlags(byte type, byte flags) {
            if (flags == 0) {
                return "";
            }
            switch (type) {
                case 2:
                case 3:
                case 7:
                case 8:
                    return BINARY[flags];
                case 4:
                case 6:
                    return flags == 1 ? "ACK" : BINARY[flags];
                case 5:
                default:
                    String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
                    if (type == 5 && (flags & 4) != 0) {
                        return result.replace("HEADERS", "PUSH_PROMISE");
                    }
                    if (type == 0 && (flags & 32) != 0) {
                        return result.replace("PRIORITY", "COMPRESSED");
                    }
                    return result;
            }
        }

        static {
            for (int i = 0; i < BINARY.length; i++) {
                BINARY[i] = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            }
            FLAGS[0] = "";
            FLAGS[1] = "END_STREAM";
            int[] prefixFlags = {1};
            FLAGS[8] = "PADDED";
            for (int prefixFlag : prefixFlags) {
                FLAGS[prefixFlag | 8] = FLAGS[prefixFlag] + "|PADDED";
            }
            FLAGS[4] = "END_HEADERS";
            FLAGS[32] = "PRIORITY";
            FLAGS[36] = "END_HEADERS|PRIORITY";
            int[] frameFlags = {4, 32, 36};
            for (int frameFlag : frameFlags) {
                for (int prefixFlag2 : prefixFlags) {
                    FLAGS[prefixFlag2 | frameFlag] = FLAGS[prefixFlag2] + '|' + FLAGS[frameFlag];
                    FLAGS[prefixFlag2 | frameFlag | 8] = FLAGS[prefixFlag2] + '|' + FLAGS[frameFlag] + "|PADDED";
                }
            }
            for (int i2 = 0; i2 < FLAGS.length; i2++) {
                if (FLAGS[i2] == null) {
                    FLAGS[i2] = BINARY[i2];
                }
            }
        }
    }

    private static int readMedium(BufferedSource source) throws IOException {
        return ((source.readByte() & 255) << 16) | ((source.readByte() & 255) << 8) | (source.readByte() & 255);
    }

    private static void writeMedium(BufferedSink sink, int i) throws IOException {
        sink.writeByte((i >>> 16) & 255);
        sink.writeByte((i >>> 8) & 255);
        sink.writeByte(i & 255);
    }
}
