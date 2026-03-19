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
    public FrameReader newReader(BufferedSource bufferedSource, boolean z) {
        return new Reader(bufferedSource, 4096, z);
    }

    @Override
    public FrameWriter newWriter(BufferedSink bufferedSink, boolean z) {
        return new Writer(bufferedSink, z);
    }

    static final class Reader implements FrameReader {
        private final boolean client;
        private final ContinuationSource continuation;
        final Hpack.Reader hpackReader;
        private final BufferedSource source;

        Reader(BufferedSource bufferedSource, int i, boolean z) {
            this.source = bufferedSource;
            this.client = z;
            this.continuation = new ContinuationSource(this.source);
            this.hpackReader = new Hpack.Reader(i, this.continuation);
        }

        @Override
        public void readConnectionPreface() throws IOException {
            if (this.client) {
                return;
            }
            ByteString byteString = this.source.readByteString(Http2.CONNECTION_PREFACE.size());
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(String.format("<< CONNECTION %s", byteString.hex()));
            }
            if (!Http2.CONNECTION_PREFACE.equals(byteString)) {
                throw Http2.ioException("Expected a connection header but was %s", byteString.utf8());
            }
        }

        @Override
        public boolean nextFrame(FrameReader.Handler handler) throws IOException {
            try {
                this.source.require(9L);
                int medium = Http2.readMedium(this.source);
                if (medium < 0 || medium > Http2.INITIAL_MAX_FRAME_SIZE) {
                    throw Http2.ioException("FRAME_SIZE_ERROR: %s", Integer.valueOf(medium));
                }
                byte b = (byte) (this.source.readByte() & 255);
                byte b2 = (byte) (this.source.readByte() & 255);
                int i = this.source.readInt() & Integer.MAX_VALUE;
                if (Http2.logger.isLoggable(Level.FINE)) {
                    Http2.logger.fine(FrameLogger.formatHeader(true, i, medium, b, b2));
                }
                switch (b) {
                    case 0:
                        readData(handler, medium, b2, i);
                        return true;
                    case 1:
                        readHeaders(handler, medium, b2, i);
                        return true;
                    case 2:
                        readPriority(handler, medium, b2, i);
                        return true;
                    case 3:
                        readRstStream(handler, medium, b2, i);
                        return true;
                    case 4:
                        readSettings(handler, medium, b2, i);
                        return true;
                    case 5:
                        readPushPromise(handler, medium, b2, i);
                        return true;
                    case 6:
                        readPing(handler, medium, b2, i);
                        return true;
                    case 7:
                        readGoAway(handler, medium, b2, i);
                        return true;
                    case 8:
                        readWindowUpdate(handler, medium, b2, i);
                        return true;
                    default:
                        this.source.skip(medium);
                        return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        private void readHeaders(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i2 == 0) {
                throw Http2.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0", new Object[0]);
            }
            boolean z = (b & 1) != 0;
            short s = (b & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            if ((b & 32) != 0) {
                readPriority(handler, i2);
                i -= 5;
            }
            handler.headers(false, z, i2, -1, readHeaderBlock(Http2.lengthWithoutPadding(i, b, s), s, b, i2), HeadersMode.HTTP_20_HEADERS);
        }

        private List<Header> readHeaderBlock(int i, short s, byte b, int i2) throws IOException {
            ContinuationSource continuationSource = this.continuation;
            this.continuation.left = i;
            continuationSource.length = i;
            this.continuation.padding = s;
            this.continuation.flags = b;
            this.continuation.streamId = i2;
            this.hpackReader.readHeaders();
            return this.hpackReader.getAndResetHeaderList();
        }

        private void readData(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            boolean z = (b & 1) != 0;
            if ((b & 32) != 0) {
                throw Http2.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA", new Object[0]);
            }
            short s = (b & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            handler.data(z, i2, this.source, Http2.lengthWithoutPadding(i, b, s));
            this.source.skip(s);
        }

        private void readPriority(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i != 5) {
                throw Http2.ioException("TYPE_PRIORITY length: %d != 5", Integer.valueOf(i));
            }
            if (i2 == 0) {
                throw Http2.ioException("TYPE_PRIORITY streamId == 0", new Object[0]);
            }
            readPriority(handler, i2);
        }

        private void readPriority(FrameReader.Handler handler, int i) throws IOException {
            boolean z;
            int i2 = this.source.readInt();
            if ((Integer.MIN_VALUE & i2) == 0) {
                z = false;
            } else {
                z = true;
            }
            handler.priority(i, i2 & Integer.MAX_VALUE, (this.source.readByte() & 255) + 1, z);
        }

        private void readRstStream(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i != 4) {
                throw Http2.ioException("TYPE_RST_STREAM length: %d != 4", Integer.valueOf(i));
            }
            if (i2 == 0) {
                throw Http2.ioException("TYPE_RST_STREAM streamId == 0", new Object[0]);
            }
            int i3 = this.source.readInt();
            ErrorCode errorCodeFromHttp2 = ErrorCode.fromHttp2(i3);
            if (errorCodeFromHttp2 == null) {
                throw Http2.ioException("TYPE_RST_STREAM unexpected error code: %d", Integer.valueOf(i3));
            }
            handler.rstStream(i2, errorCodeFromHttp2);
        }

        private void readSettings(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i2 != 0) {
                throw Http2.ioException("TYPE_SETTINGS streamId != 0", new Object[0]);
            }
            if ((b & 1) != 0) {
                if (i != 0) {
                    throw Http2.ioException("FRAME_SIZE_ERROR ack frame should be empty!", new Object[0]);
                }
                handler.ackSettings();
                return;
            }
            if (i % 6 != 0) {
                throw Http2.ioException("TYPE_SETTINGS length %% 6 != 0: %s", Integer.valueOf(i));
            }
            Settings settings = new Settings();
            for (int i3 = 0; i3 < i; i3 += 6) {
                short s = this.source.readShort();
                int i4 = this.source.readInt();
                switch (s) {
                    case 1:
                    case 6:
                        break;
                    case 2:
                        if (i4 != 0 && i4 != 1) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1", new Object[0]);
                        }
                        break;
                        break;
                    case 3:
                        s = 4;
                        break;
                    case 4:
                        s = 7;
                        if (i4 < 0) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1", new Object[0]);
                        }
                        break;
                        break;
                    case 5:
                        if (i4 < Http2.INITIAL_MAX_FRAME_SIZE || i4 > 16777215) {
                            throw Http2.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", Integer.valueOf(i4));
                        }
                        break;
                        break;
                    default:
                        throw Http2.ioException("PROTOCOL_ERROR invalid settings id: %s", Short.valueOf(s));
                }
                settings.set(s, 0, i4);
            }
            handler.settings(false, settings);
            if (settings.getHeaderTableSize() >= 0) {
                this.hpackReader.headerTableSizeSetting(settings.getHeaderTableSize());
            }
        }

        private void readPushPromise(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i2 == 0) {
                throw Http2.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0", new Object[0]);
            }
            short s = (b & 8) != 0 ? (short) (this.source.readByte() & 255) : (short) 0;
            handler.pushPromise(i2, this.source.readInt() & Integer.MAX_VALUE, readHeaderBlock(Http2.lengthWithoutPadding(i - 4, b, s), s, b, i2));
        }

        private void readPing(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            boolean z = false;
            if (i != 8) {
                throw Http2.ioException("TYPE_PING length != 8: %s", Integer.valueOf(i));
            }
            if (i2 != 0) {
                throw Http2.ioException("TYPE_PING streamId != 0", new Object[0]);
            }
            int i3 = this.source.readInt();
            int i4 = this.source.readInt();
            if ((b & 1) != 0) {
                z = true;
            }
            handler.ping(z, i3, i4);
        }

        private void readGoAway(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i < 8) {
                throw Http2.ioException("TYPE_GOAWAY length < 8: %s", Integer.valueOf(i));
            }
            if (i2 != 0) {
                throw Http2.ioException("TYPE_GOAWAY streamId != 0", new Object[0]);
            }
            int i3 = this.source.readInt();
            int i4 = this.source.readInt();
            int i5 = i - 8;
            ErrorCode errorCodeFromHttp2 = ErrorCode.fromHttp2(i4);
            if (errorCodeFromHttp2 == null) {
                throw Http2.ioException("TYPE_GOAWAY unexpected error code: %d", Integer.valueOf(i4));
            }
            ByteString byteString = ByteString.EMPTY;
            if (i5 > 0) {
                byteString = this.source.readByteString(i5);
            }
            handler.goAway(i3, errorCodeFromHttp2, byteString);
        }

        private void readWindowUpdate(FrameReader.Handler handler, int i, byte b, int i2) throws IOException {
            if (i != 4) {
                throw Http2.ioException("TYPE_WINDOW_UPDATE length !=4: %s", Integer.valueOf(i));
            }
            long j = ((long) this.source.readInt()) & 2147483647L;
            if (j == 0) {
                throw Http2.ioException("windowSizeIncrement was 0", Long.valueOf(j));
            }
            handler.windowUpdate(i2, j);
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

        Writer(BufferedSink bufferedSink, boolean z) {
            this.sink = bufferedSink;
            this.client = z;
        }

        @Override
        public synchronized void flush() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.sink.flush();
        }

        @Override
        public synchronized void ackSettings(Settings settings) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.maxFrameSize = settings.getMaxFrameSize(this.maxFrameSize);
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
        public synchronized void synStream(boolean z, boolean z2, int i, int i2, List<Header> list) throws IOException {
            if (z2) {
                throw new UnsupportedOperationException();
            }
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(z, i, list);
        }

        @Override
        public synchronized void synReply(boolean z, int i, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(z, i, list);
        }

        @Override
        public synchronized void headers(int i, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            headers(false, i, list);
        }

        @Override
        public synchronized void pushPromise(int i, int i2, List<Header> list) throws IOException {
            byte b;
            if (this.closed) {
                throw new IOException("closed");
            }
            this.hpackWriter.writeHeaders(list);
            long size = this.hpackBuffer.size();
            int iMin = (int) Math.min(this.maxFrameSize - 4, size);
            long j = iMin;
            if (size != j) {
                b = 0;
            } else {
                b = 4;
            }
            frameHeader(i, iMin + 4, Http2.TYPE_PUSH_PROMISE, b);
            this.sink.writeInt(i2 & Integer.MAX_VALUE);
            this.sink.write(this.hpackBuffer, j);
            if (size > j) {
                writeContinuationFrames(i, size - j);
            }
        }

        void headers(boolean z, int i, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.hpackWriter.writeHeaders(list);
            long size = this.hpackBuffer.size();
            int iMin = (int) Math.min(this.maxFrameSize, size);
            long j = iMin;
            byte b = size == j ? (byte) 4 : (byte) 0;
            if (z) {
                b = (byte) (b | 1);
            }
            frameHeader(i, iMin, (byte) 1, b);
            this.sink.write(this.hpackBuffer, j);
            if (size > j) {
                writeContinuationFrames(i, size - j);
            }
        }

        private void writeContinuationFrames(int i, long j) throws IOException {
            while (j > 0) {
                int iMin = (int) Math.min(this.maxFrameSize, j);
                long j2 = iMin;
                j -= j2;
                frameHeader(i, iMin, Http2.TYPE_CONTINUATION, j == 0 ? (byte) 4 : (byte) 0);
                this.sink.write(this.hpackBuffer, j2);
            }
        }

        @Override
        public synchronized void rstStream(int i, ErrorCode errorCode) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.httpCode == -1) {
                throw new IllegalArgumentException();
            }
            frameHeader(i, 4, Http2.TYPE_RST_STREAM, (byte) 0);
            this.sink.writeInt(errorCode.httpCode);
            this.sink.flush();
        }

        @Override
        public int maxDataLength() {
            return this.maxFrameSize;
        }

        @Override
        public synchronized void data(boolean z, int i, Buffer buffer, int i2) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            dataFrame(i, z ? (byte) 1 : (byte) 0, buffer, i2);
        }

        void dataFrame(int i, byte b, Buffer buffer, int i2) throws IOException {
            frameHeader(i, i2, (byte) 0, b);
            if (i2 > 0) {
                this.sink.write(buffer, i2);
            }
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            int i;
            if (this.closed) {
                throw new IOException("closed");
            }
            int i2 = 0;
            frameHeader(0, settings.size() * 6, (byte) 4, (byte) 0);
            while (i2 < 10) {
                if (settings.isSet(i2)) {
                    if (i2 == 4) {
                        i = 3;
                    } else {
                        i = i2 == 7 ? 4 : i2;
                    }
                    this.sink.writeShort(i);
                    this.sink.writeInt(settings.get(i2));
                }
                i2++;
            }
            this.sink.flush();
        }

        @Override
        public synchronized void ping(boolean z, int i, int i2) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            frameHeader(0, 8, Http2.TYPE_PING, z ? (byte) 1 : (byte) 0);
            this.sink.writeInt(i);
            this.sink.writeInt(i2);
            this.sink.flush();
        }

        @Override
        public synchronized void goAway(int i, ErrorCode errorCode, byte[] bArr) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.httpCode == -1) {
                throw Http2.illegalArgument("errorCode.httpCode == -1", new Object[0]);
            }
            frameHeader(0, 8 + bArr.length, Http2.TYPE_GOAWAY, (byte) 0);
            this.sink.writeInt(i);
            this.sink.writeInt(errorCode.httpCode);
            if (bArr.length > 0) {
                this.sink.write(bArr);
            }
            this.sink.flush();
        }

        @Override
        public synchronized void windowUpdate(int i, long j) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (j == 0 || j > 2147483647L) {
                throw Http2.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", Long.valueOf(j));
            }
            frameHeader(i, 4, (byte) 8, (byte) 0);
            this.sink.writeInt((int) j);
            this.sink.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            this.closed = true;
            this.sink.close();
        }

        void frameHeader(int i, int i2, byte b, byte b2) throws IOException {
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(FrameLogger.formatHeader(false, i, i2, b, b2));
            }
            if (i2 > this.maxFrameSize) {
                throw Http2.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", Integer.valueOf(this.maxFrameSize), Integer.valueOf(i2));
            }
            if ((Integer.MIN_VALUE & i) == 0) {
                Http2.writeMedium(this.sink, i2);
                this.sink.writeByte(b & 255);
                this.sink.writeByte(b2 & 255);
                this.sink.writeInt(i & Integer.MAX_VALUE);
                return;
            }
            throw Http2.illegalArgument("reserved bit set: %s", Integer.valueOf(i));
        }
    }

    private static IllegalArgumentException illegalArgument(String str, Object... objArr) {
        throw new IllegalArgumentException(String.format(str, objArr));
    }

    private static IOException ioException(String str, Object... objArr) throws IOException {
        throw new IOException(String.format(str, objArr));
    }

    static final class ContinuationSource implements Source {
        byte flags;
        int left;
        int length;
        short padding;
        private final BufferedSource source;
        int streamId;

        public ContinuationSource(BufferedSource bufferedSource) {
            this.source = bufferedSource;
        }

        @Override
        public long read(Buffer buffer, long j) throws IOException {
            while (this.left == 0) {
                this.source.skip(this.padding);
                this.padding = (short) 0;
                if ((this.flags & 4) != 0) {
                    return -1L;
                }
                readContinuationHeader();
            }
            long j2 = this.source.read(buffer, Math.min(j, this.left));
            if (j2 == -1) {
                return -1L;
            }
            this.left = (int) (((long) this.left) - j2);
            return j2;
        }

        @Override
        public Timeout timeout() {
            return this.source.timeout();
        }

        @Override
        public void close() throws IOException {
        }

        private void readContinuationHeader() throws IOException {
            int i = this.streamId;
            int medium = Http2.readMedium(this.source);
            this.left = medium;
            this.length = medium;
            byte b = (byte) (this.source.readByte() & 255);
            this.flags = (byte) (this.source.readByte() & 255);
            if (Http2.logger.isLoggable(Level.FINE)) {
                Http2.logger.fine(FrameLogger.formatHeader(true, this.streamId, this.length, b, this.flags));
            }
            this.streamId = this.source.readInt() & Integer.MAX_VALUE;
            if (b != 9) {
                throw Http2.ioException("%s != TYPE_CONTINUATION", Byte.valueOf(b));
            }
            if (this.streamId != i) {
                throw Http2.ioException("TYPE_CONTINUATION streamId changed", new Object[0]);
            }
        }
    }

    private static int lengthWithoutPadding(int i, byte b, short s) throws IOException {
        if ((b & 8) != 0) {
            i--;
        }
        if (s <= i) {
            return (short) (i - s);
        }
        throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", Short.valueOf(s), Integer.valueOf(i));
    }

    static final class FrameLogger {
        private static final String[] TYPES = {"DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION"};
        private static final String[] FLAGS = new String[64];
        private static final String[] BINARY = new String[256];

        FrameLogger() {
        }

        static String formatHeader(boolean z, int i, int i2, byte b, byte b2) {
            String str = b < TYPES.length ? TYPES[b] : String.format("0x%02x", Byte.valueOf(b));
            String flags = formatFlags(b, b2);
            Object[] objArr = new Object[5];
            objArr[0] = z ? "<<" : ">>";
            objArr[1] = Integer.valueOf(i);
            objArr[2] = Integer.valueOf(i2);
            objArr[3] = str;
            objArr[4] = flags;
            return String.format("%s 0x%08x %5d %-13s %s", objArr);
        }

        static String formatFlags(byte b, byte b2) {
            if (b2 == 0) {
                return "";
            }
            switch (b) {
                case 2:
                case 3:
                case 7:
                case 8:
                    return BINARY[b2];
                case 4:
                case 6:
                    return b2 == 1 ? "ACK" : BINARY[b2];
                case 5:
                default:
                    String str = b2 < FLAGS.length ? FLAGS[b2] : BINARY[b2];
                    if (b == 5 && (b2 & 4) != 0) {
                        return str.replace("HEADERS", "PUSH_PROMISE");
                    }
                    if (b == 0 && (b2 & 32) != 0) {
                        return str.replace("PRIORITY", "COMPRESSED");
                    }
                    return str;
            }
        }

        static {
            for (int i = 0; i < BINARY.length; i++) {
                BINARY[i] = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            }
            FLAGS[0] = "";
            FLAGS[1] = "END_STREAM";
            int[] iArr = {1};
            FLAGS[8] = "PADDED";
            for (int i2 : iArr) {
                FLAGS[i2 | 8] = FLAGS[i2] + "|PADDED";
            }
            FLAGS[4] = "END_HEADERS";
            FLAGS[32] = "PRIORITY";
            FLAGS[36] = "END_HEADERS|PRIORITY";
            for (int i3 : new int[]{4, 32, 36}) {
                for (int i4 : iArr) {
                    int i5 = i4 | i3;
                    FLAGS[i5] = FLAGS[i4] + '|' + FLAGS[i3];
                    FLAGS[i5 | 8] = FLAGS[i4] + '|' + FLAGS[i3] + "|PADDED";
                }
            }
            for (int i6 = 0; i6 < FLAGS.length; i6++) {
                if (FLAGS[i6] == null) {
                    FLAGS[i6] = BINARY[i6];
                }
            }
        }
    }

    private static int readMedium(BufferedSource bufferedSource) throws IOException {
        return (bufferedSource.readByte() & 255) | ((bufferedSource.readByte() & 255) << 16) | ((bufferedSource.readByte() & 255) << 8);
    }

    private static void writeMedium(BufferedSink bufferedSink, int i) throws IOException {
        bufferedSink.writeByte((i >>> 16) & 255);
        bufferedSink.writeByte((i >>> 8) & 255);
        bufferedSink.writeByte(i & 255);
    }
}
