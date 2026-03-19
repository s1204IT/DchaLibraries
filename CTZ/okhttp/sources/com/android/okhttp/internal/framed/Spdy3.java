package com.android.okhttp.internal.framed;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.framed.FrameReader;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.ByteString;
import com.android.okhttp.okio.DeflaterSink;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Sink;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.util.List;
import java.util.zip.Deflater;

public final class Spdy3 implements Variant {
    static final byte[] DICTIONARY;
    static final int FLAG_FIN = 1;
    static final int FLAG_UNIDIRECTIONAL = 2;
    static final int TYPE_DATA = 0;
    static final int TYPE_GOAWAY = 7;
    static final int TYPE_HEADERS = 8;
    static final int TYPE_PING = 6;
    static final int TYPE_RST_STREAM = 3;
    static final int TYPE_SETTINGS = 4;
    static final int TYPE_SYN_REPLY = 2;
    static final int TYPE_SYN_STREAM = 1;
    static final int TYPE_WINDOW_UPDATE = 9;
    static final int VERSION = 3;

    @Override
    public Protocol getProtocol() {
        return Protocol.SPDY_3;
    }

    static {
        try {
            DICTIONARY = "\u0000\u0000\u0000\u0007options\u0000\u0000\u0000\u0004head\u0000\u0000\u0000\u0004post\u0000\u0000\u0000\u0003put\u0000\u0000\u0000\u0006delete\u0000\u0000\u0000\u0005trace\u0000\u0000\u0000\u0006accept\u0000\u0000\u0000\u000eaccept-charset\u0000\u0000\u0000\u000faccept-encoding\u0000\u0000\u0000\u000faccept-language\u0000\u0000\u0000\raccept-ranges\u0000\u0000\u0000\u0003age\u0000\u0000\u0000\u0005allow\u0000\u0000\u0000\rauthorization\u0000\u0000\u0000\rcache-control\u0000\u0000\u0000\nconnection\u0000\u0000\u0000\fcontent-base\u0000\u0000\u0000\u0010content-encoding\u0000\u0000\u0000\u0010content-language\u0000\u0000\u0000\u000econtent-length\u0000\u0000\u0000\u0010content-location\u0000\u0000\u0000\u000bcontent-md5\u0000\u0000\u0000\rcontent-range\u0000\u0000\u0000\fcontent-type\u0000\u0000\u0000\u0004date\u0000\u0000\u0000\u0004etag\u0000\u0000\u0000\u0006expect\u0000\u0000\u0000\u0007expires\u0000\u0000\u0000\u0004from\u0000\u0000\u0000\u0004host\u0000\u0000\u0000\bif-match\u0000\u0000\u0000\u0011if-modified-since\u0000\u0000\u0000\rif-none-match\u0000\u0000\u0000\bif-range\u0000\u0000\u0000\u0013if-unmodified-since\u0000\u0000\u0000\rlast-modified\u0000\u0000\u0000\blocation\u0000\u0000\u0000\fmax-forwards\u0000\u0000\u0000\u0006pragma\u0000\u0000\u0000\u0012proxy-authenticate\u0000\u0000\u0000\u0013proxy-authorization\u0000\u0000\u0000\u0005range\u0000\u0000\u0000\u0007referer\u0000\u0000\u0000\u000bretry-after\u0000\u0000\u0000\u0006server\u0000\u0000\u0000\u0002te\u0000\u0000\u0000\u0007trailer\u0000\u0000\u0000\u0011transfer-encoding\u0000\u0000\u0000\u0007upgrade\u0000\u0000\u0000\nuser-agent\u0000\u0000\u0000\u0004vary\u0000\u0000\u0000\u0003via\u0000\u0000\u0000\u0007warning\u0000\u0000\u0000\u0010www-authenticate\u0000\u0000\u0000\u0006method\u0000\u0000\u0000\u0003get\u0000\u0000\u0000\u0006status\u0000\u0000\u0000\u0006200 OK\u0000\u0000\u0000\u0007version\u0000\u0000\u0000\bHTTP/1.1\u0000\u0000\u0000\u0003url\u0000\u0000\u0000\u0006public\u0000\u0000\u0000\nset-cookie\u0000\u0000\u0000\nkeep-alive\u0000\u0000\u0000\u0006origin100101201202205206300302303304305306307402405406407408409410411412413414415416417502504505203 Non-Authoritative Information204 No Content301 Moved Permanently400 Bad Request401 Unauthorized403 Forbidden404 Not Found500 Internal Server Error501 Not Implemented503 Service UnavailableJan Feb Mar Apr May Jun Jul Aug Sept Oct Nov Dec 00:00:00 Mon, Tue, Wed, Thu, Fri, Sat, Sun, GMTchunked,text/html,image/png,image/jpg,image/gif,application/xml,application/xhtml+xml,text/plain,text/javascript,publicprivatemax-age=gzip,deflate,sdchcharset=utf-8charset=iso-8859-1,utf-,*,enq=0.".getBytes(Util.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    @Override
    public FrameReader newReader(BufferedSource bufferedSource, boolean z) {
        return new Reader(bufferedSource, z);
    }

    @Override
    public FrameWriter newWriter(BufferedSink bufferedSink, boolean z) {
        return new Writer(bufferedSink, z);
    }

    static final class Reader implements FrameReader {
        private final boolean client;
        private final NameValueBlockReader headerBlockReader;
        private final BufferedSource source;

        Reader(BufferedSource bufferedSource, boolean z) {
            this.source = bufferedSource;
            this.headerBlockReader = new NameValueBlockReader(this.source);
            this.client = z;
        }

        @Override
        public void readConnectionPreface() {
        }

        @Override
        public boolean nextFrame(FrameReader.Handler handler) throws IOException {
            boolean z = false;
            try {
                int i = this.source.readInt();
                int i2 = this.source.readInt();
                int i3 = ((-16777216) & i2) >>> 24;
                int i4 = i2 & 16777215;
                if ((Integer.MIN_VALUE & i) != 0 ? true : Spdy3.TYPE_DATA) {
                    int i5 = (2147418112 & i) >>> 16;
                    int i6 = i & 65535;
                    if (i5 != 3) {
                        throw new ProtocolException("version != 3: " + i5);
                    }
                    switch (i6) {
                        case 1:
                            readSynStream(handler, i3, i4);
                            return true;
                        case 2:
                            readSynReply(handler, i3, i4);
                            return true;
                        case 3:
                            readRstStream(handler, i3, i4);
                            return true;
                        case Spdy3.TYPE_SETTINGS:
                            readSettings(handler, i3, i4);
                            return true;
                        case 5:
                        default:
                            this.source.skip(i4);
                            return true;
                        case Spdy3.TYPE_PING:
                            readPing(handler, i3, i4);
                            return true;
                        case Spdy3.TYPE_GOAWAY:
                            readGoAway(handler, i3, i4);
                            return true;
                        case Spdy3.TYPE_HEADERS:
                            readHeaders(handler, i3, i4);
                            return true;
                        case Spdy3.TYPE_WINDOW_UPDATE:
                            readWindowUpdate(handler, i3, i4);
                            return true;
                    }
                }
                int i7 = i & Integer.MAX_VALUE;
                if ((i3 & 1) != 0) {
                    z = true;
                }
                handler.data(z, i7, this.source, i4);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private void readSynStream(FrameReader.Handler handler, int i, int i2) throws IOException {
            int i3 = this.source.readInt() & Integer.MAX_VALUE;
            int i4 = this.source.readInt() & Integer.MAX_VALUE;
            this.source.readShort();
            handler.headers((i & 2) != 0 ? true : Spdy3.TYPE_DATA, (i & 1) != 0 ? true : Spdy3.TYPE_DATA, i3, i4, this.headerBlockReader.readNameValueBlock(i2 - 10), HeadersMode.SPDY_SYN_STREAM);
        }

        private void readSynReply(FrameReader.Handler handler, int i, int i2) throws IOException {
            handler.headers(false, (i & 1) != 0 ? true : Spdy3.TYPE_DATA, this.source.readInt() & Integer.MAX_VALUE, -1, this.headerBlockReader.readNameValueBlock(i2 - 4), HeadersMode.SPDY_REPLY);
        }

        private void readRstStream(FrameReader.Handler handler, int i, int i2) throws IOException {
            if (i2 != Spdy3.TYPE_HEADERS) {
                throw ioException("TYPE_RST_STREAM length: %d != 8", Integer.valueOf(i2));
            }
            int i3 = this.source.readInt() & Integer.MAX_VALUE;
            int i4 = this.source.readInt();
            ErrorCode errorCodeFromSpdy3Rst = ErrorCode.fromSpdy3Rst(i4);
            if (errorCodeFromSpdy3Rst == null) {
                throw ioException("TYPE_RST_STREAM unexpected error code: %d", Integer.valueOf(i4));
            }
            handler.rstStream(i3, errorCodeFromSpdy3Rst);
        }

        private void readHeaders(FrameReader.Handler handler, int i, int i2) throws IOException {
            handler.headers(false, false, this.source.readInt() & Integer.MAX_VALUE, -1, this.headerBlockReader.readNameValueBlock(i2 - 4), HeadersMode.SPDY_HEADERS);
        }

        private void readWindowUpdate(FrameReader.Handler handler, int i, int i2) throws IOException {
            if (i2 != Spdy3.TYPE_HEADERS) {
                throw ioException("TYPE_WINDOW_UPDATE length: %d != 8", Integer.valueOf(i2));
            }
            int i3 = this.source.readInt() & Integer.MAX_VALUE;
            long j = this.source.readInt() & Integer.MAX_VALUE;
            if (j == 0) {
                throw ioException("windowSizeIncrement was 0", Long.valueOf(j));
            }
            handler.windowUpdate(i3, j);
        }

        private void readPing(FrameReader.Handler handler, int i, int i2) throws IOException {
            if (i2 != Spdy3.TYPE_SETTINGS) {
                throw ioException("TYPE_PING length: %d != 4", Integer.valueOf(i2));
            }
            int i3 = this.source.readInt();
            handler.ping(this.client != ((i3 & 1) == 1 ? true : Spdy3.TYPE_DATA) ? Spdy3.TYPE_DATA : true, i3, Spdy3.TYPE_DATA);
        }

        private void readGoAway(FrameReader.Handler handler, int i, int i2) throws IOException {
            if (i2 != Spdy3.TYPE_HEADERS) {
                throw ioException("TYPE_GOAWAY length: %d != 8", Integer.valueOf(i2));
            }
            int i3 = this.source.readInt() & Integer.MAX_VALUE;
            int i4 = this.source.readInt();
            ErrorCode errorCodeFromSpdyGoAway = ErrorCode.fromSpdyGoAway(i4);
            if (errorCodeFromSpdyGoAway == null) {
                throw ioException("TYPE_GOAWAY unexpected error code: %d", Integer.valueOf(i4));
            }
            handler.goAway(i3, errorCodeFromSpdyGoAway, ByteString.EMPTY);
        }

        private void readSettings(FrameReader.Handler handler, int i, int i2) throws IOException {
            int i3 = this.source.readInt();
            if (i2 != Spdy3.TYPE_SETTINGS + (Spdy3.TYPE_HEADERS * i3)) {
                throw ioException("TYPE_SETTINGS length: %d != 4 + 8 * %d", Integer.valueOf(i2), Integer.valueOf(i3));
            }
            Settings settings = new Settings();
            for (int i4 = Spdy3.TYPE_DATA; i4 < i3; i4++) {
                int i5 = this.source.readInt();
                settings.set(i5 & 16777215, ((-16777216) & i5) >>> 24, this.source.readInt());
            }
            handler.settings((i & 1) != 0, settings);
        }

        private static IOException ioException(String str, Object... objArr) throws IOException {
            throw new IOException(String.format(str, objArr));
        }

        @Override
        public void close() throws IOException {
            this.headerBlockReader.close();
        }
    }

    static final class Writer implements FrameWriter {
        private final boolean client;
        private boolean closed;
        private final Buffer headerBlockBuffer;
        private final BufferedSink headerBlockOut;
        private final BufferedSink sink;

        Writer(BufferedSink bufferedSink, boolean z) {
            this.sink = bufferedSink;
            this.client = z;
            Deflater deflater = new Deflater();
            deflater.setDictionary(Spdy3.DICTIONARY);
            this.headerBlockBuffer = new Buffer();
            this.headerBlockOut = Okio.buffer(new DeflaterSink((Sink) this.headerBlockBuffer, deflater));
        }

        @Override
        public void ackSettings(Settings settings) {
        }

        @Override
        public void pushPromise(int i, int i2, List<Header> list) throws IOException {
        }

        @Override
        public synchronized void connectionPreface() {
        }

        @Override
        public synchronized void flush() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            this.sink.flush();
        }

        @Override
        public synchronized void synStream(boolean z, boolean z2, int i, int i2, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            writeNameValueBlockToBuffer(list);
            int size = (int) (10 + this.headerBlockBuffer.size());
            int i3 = (z ? 1 : 0) | (z2 ? 2 : Spdy3.TYPE_DATA);
            this.sink.writeInt(-2147287039);
            this.sink.writeInt(((i3 & 255) << 24) | (size & 16777215));
            this.sink.writeInt(i & Integer.MAX_VALUE);
            this.sink.writeInt(Integer.MAX_VALUE & i2);
            this.sink.writeShort(Spdy3.TYPE_DATA);
            this.sink.writeAll(this.headerBlockBuffer);
            this.sink.flush();
        }

        @Override
        public synchronized void synReply(boolean z, int i, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            writeNameValueBlockToBuffer(list);
            int size = (int) (this.headerBlockBuffer.size() + 4);
            this.sink.writeInt(-2147287038);
            this.sink.writeInt((((z ? 1 : 0) & 255) << 24) | (size & 16777215));
            this.sink.writeInt(i & Integer.MAX_VALUE);
            this.sink.writeAll(this.headerBlockBuffer);
            this.sink.flush();
        }

        @Override
        public synchronized void headers(int i, List<Header> list) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            writeNameValueBlockToBuffer(list);
            int size = (int) (this.headerBlockBuffer.size() + 4);
            this.sink.writeInt(-2147287032);
            this.sink.writeInt((size & 16777215) | Spdy3.TYPE_DATA);
            this.sink.writeInt(i & Integer.MAX_VALUE);
            this.sink.writeAll(this.headerBlockBuffer);
        }

        @Override
        public synchronized void rstStream(int i, ErrorCode errorCode) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.spdyRstCode == -1) {
                throw new IllegalArgumentException();
            }
            this.sink.writeInt(-2147287037);
            this.sink.writeInt(Spdy3.TYPE_HEADERS);
            this.sink.writeInt(i & Integer.MAX_VALUE);
            this.sink.writeInt(errorCode.spdyRstCode);
            this.sink.flush();
        }

        @Override
        public int maxDataLength() {
            return 16383;
        }

        @Override
        public synchronized void data(boolean z, int i, Buffer buffer, int i2) throws IOException {
            sendDataFrame(i, z ? 1 : 0, buffer, i2);
        }

        void sendDataFrame(int i, int i2, Buffer buffer, int i3) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            long j = i3;
            if (j > 16777215) {
                throw new IllegalArgumentException("FRAME_TOO_LARGE max size is 16Mib: " + i3);
            }
            this.sink.writeInt(i & Integer.MAX_VALUE);
            this.sink.writeInt(((i2 & 255) << 24) | (16777215 & i3));
            if (i3 > 0) {
                this.sink.write(buffer, j);
            }
        }

        private void writeNameValueBlockToBuffer(List<Header> list) throws IOException {
            this.headerBlockOut.writeInt(list.size());
            int size = list.size();
            for (int i = Spdy3.TYPE_DATA; i < size; i++) {
                ByteString byteString = list.get(i).name;
                this.headerBlockOut.writeInt(byteString.size());
                this.headerBlockOut.write(byteString);
                ByteString byteString2 = list.get(i).value;
                this.headerBlockOut.writeInt(byteString2.size());
                this.headerBlockOut.write(byteString2);
            }
            this.headerBlockOut.flush();
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            int size = settings.size();
            int i = Spdy3.TYPE_SETTINGS + (size * Spdy3.TYPE_HEADERS);
            this.sink.writeInt(-2147287036);
            BufferedSink bufferedSink = this.sink;
            bufferedSink.writeInt((i & 16777215) | Spdy3.TYPE_DATA);
            this.sink.writeInt(size);
            for (int i2 = Spdy3.TYPE_DATA; i2 <= 10; i2++) {
                if (settings.isSet(i2)) {
                    this.sink.writeInt(((settings.flags(i2) & 255) << 24) | (i2 & 16777215));
                    this.sink.writeInt(settings.get(i2));
                }
            }
            this.sink.flush();
        }

        @Override
        public synchronized void ping(boolean z, int i, int i2) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (z != (this.client != ((i & 1) == 1 ? true : Spdy3.TYPE_DATA))) {
                throw new IllegalArgumentException("payload != reply");
            }
            this.sink.writeInt(-2147287034);
            this.sink.writeInt(Spdy3.TYPE_SETTINGS);
            this.sink.writeInt(i);
            this.sink.flush();
        }

        @Override
        public synchronized void goAway(int i, ErrorCode errorCode, byte[] bArr) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (errorCode.spdyGoAwayCode == -1) {
                throw new IllegalArgumentException("errorCode.spdyGoAwayCode == -1");
            }
            this.sink.writeInt(-2147287033);
            this.sink.writeInt(Spdy3.TYPE_HEADERS);
            this.sink.writeInt(i);
            this.sink.writeInt(errorCode.spdyGoAwayCode);
            this.sink.flush();
        }

        @Override
        public synchronized void windowUpdate(int i, long j) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            if (j == 0 || j > 2147483647L) {
                throw new IllegalArgumentException("windowSizeIncrement must be between 1 and 0x7fffffff: " + j);
            }
            this.sink.writeInt(-2147287031);
            this.sink.writeInt(Spdy3.TYPE_HEADERS);
            this.sink.writeInt(i);
            this.sink.writeInt((int) j);
            this.sink.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            this.closed = true;
            Util.closeAll(this.sink, this.headerBlockOut);
        }
    }
}
