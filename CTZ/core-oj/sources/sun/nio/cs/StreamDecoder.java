package sun.nio.cs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import sun.nio.ch.ChannelInputStream;

public class StreamDecoder extends Reader {
    static final boolean $assertionsDisabled = false;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private static final int MIN_BYTE_BUFFER_SIZE = 32;
    private static volatile boolean channelsAvailable = true;
    private ByteBuffer bb;
    private ReadableByteChannel ch;
    private Charset cs;
    private CharsetDecoder decoder;
    private boolean haveLeftoverChar;
    private InputStream in;
    private volatile boolean isOpen;
    private char leftoverChar;
    private boolean needsFlush;

    private void ensureOpen() throws IOException {
        if (!this.isOpen) {
            throw new IOException("Stream closed");
        }
    }

    public static StreamDecoder forInputStreamReader(InputStream inputStream, Object obj, String str) throws UnsupportedEncodingException {
        if (str == null) {
            str = Charset.defaultCharset().name();
        }
        try {
            if (Charset.isSupported(str)) {
                return new StreamDecoder(inputStream, obj, Charset.forName(str));
            }
        } catch (IllegalCharsetNameException e) {
        }
        throw new UnsupportedEncodingException(str);
    }

    public static StreamDecoder forInputStreamReader(InputStream inputStream, Object obj, Charset charset) {
        return new StreamDecoder(inputStream, obj, charset);
    }

    public static StreamDecoder forInputStreamReader(InputStream inputStream, Object obj, CharsetDecoder charsetDecoder) {
        return new StreamDecoder(inputStream, obj, charsetDecoder);
    }

    public static StreamDecoder forDecoder(ReadableByteChannel readableByteChannel, CharsetDecoder charsetDecoder, int i) {
        return new StreamDecoder(readableByteChannel, charsetDecoder, i);
    }

    public String getEncoding() {
        if (isOpen()) {
            return encodingName();
        }
        return null;
    }

    @Override
    public int read() throws IOException {
        return read0();
    }

    private int read0() throws IOException {
        synchronized (this.lock) {
            if (this.haveLeftoverChar) {
                this.haveLeftoverChar = $assertionsDisabled;
                return this.leftoverChar;
            }
            char[] cArr = new char[2];
            int i = read(cArr, 0, 2);
            if (i == -1) {
                return -1;
            }
            switch (i) {
                case 1:
                    break;
                case 2:
                    this.leftoverChar = cArr[1];
                    this.haveLeftoverChar = true;
                    break;
                default:
                    return -1;
            }
            return cArr[0];
        }
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            ensureOpen();
            if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
                throw new IndexOutOfBoundsException();
            }
            int i4 = 0;
            if (i2 == 0) {
                return 0;
            }
            if (this.haveLeftoverChar) {
                cArr[i] = this.leftoverChar;
                i++;
                i2--;
                this.haveLeftoverChar = $assertionsDisabled;
                if (i2 != 0 && implReady()) {
                    i4 = 1;
                }
                return 1;
            }
            if (i2 == 1) {
                int i5 = read0();
                if (i5 == -1) {
                    if (i4 == 0) {
                        i4 = -1;
                    }
                    return i4;
                }
                cArr[i] = (char) i5;
                return i4 + 1;
            }
            return i4 + implRead(cArr, i, i2 + i);
        }
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            z = (this.haveLeftoverChar || implReady()) ? true : $assertionsDisabled;
        }
        return z;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.isOpen) {
                implClose();
                this.isOpen = $assertionsDisabled;
            }
        }
    }

    private boolean isOpen() {
        return this.isOpen;
    }

    private static FileChannel getChannel(FileInputStream fileInputStream) {
        if (!channelsAvailable) {
            return null;
        }
        try {
            return fileInputStream.getChannel();
        } catch (UnsatisfiedLinkError e) {
            channelsAvailable = $assertionsDisabled;
            return null;
        }
    }

    StreamDecoder(InputStream inputStream, Object obj, Charset charset) {
        this(inputStream, obj, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    StreamDecoder(InputStream inputStream, Object obj, CharsetDecoder charsetDecoder) {
        super(obj);
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.needsFlush = $assertionsDisabled;
        this.cs = charsetDecoder.charset();
        this.decoder = charsetDecoder;
        if (this.ch == null) {
            this.in = inputStream;
            this.ch = null;
            this.bb = ByteBuffer.allocate(8192);
        }
        this.bb.flip();
    }

    StreamDecoder(ReadableByteChannel readableByteChannel, CharsetDecoder charsetDecoder, int i) {
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.needsFlush = $assertionsDisabled;
        this.in = null;
        this.ch = readableByteChannel;
        this.decoder = charsetDecoder;
        this.cs = charsetDecoder.charset();
        if (i < 0) {
            i = 8192;
        } else if (i < 32) {
            i = 32;
        }
        this.bb = ByteBuffer.allocate(i);
        this.bb.flip();
    }

    private int readBytes() throws IOException {
        this.bb.compact();
        try {
            if (this.ch != null) {
                int i = ChannelInputStream.read(this.ch, this.bb);
                if (i < 0) {
                    return i;
                }
            } else {
                int iLimit = this.bb.limit();
                int iPosition = this.bb.position();
                int i2 = this.in.read(this.bb.array(), this.bb.arrayOffset() + iPosition, iPosition <= iLimit ? iLimit - iPosition : 0);
                if (i2 < 0) {
                    return i2;
                }
                if (i2 == 0) {
                    throw new IOException("Underlying input stream returned zero bytes");
                }
                this.bb.position(iPosition + i2);
            }
            this.bb.flip();
            return this.bb.remaining();
        } finally {
            this.bb.flip();
        }
    }

    int implRead(char[] cArr, int i, int i2) throws IOException {
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr, i, i2 - i);
        if (charBufferWrap.position() != 0) {
            charBufferWrap = charBufferWrap.slice();
        }
        if (this.needsFlush) {
            CoderResult coderResultFlush = this.decoder.flush(charBufferWrap);
            if (coderResultFlush.isOverflow()) {
                return charBufferWrap.position();
            }
            if (coderResultFlush.isUnderflow()) {
                if (charBufferWrap.position() == 0) {
                    return -1;
                }
                return charBufferWrap.position();
            }
            coderResultFlush.throwException();
        }
        boolean z = $assertionsDisabled;
        while (true) {
            CoderResult coderResultDecode = this.decoder.decode(this.bb, charBufferWrap, z);
            if (coderResultDecode.isUnderflow()) {
                if (z || !charBufferWrap.hasRemaining() || (charBufferWrap.position() > 0 && !inReady())) {
                    break;
                }
                if (readBytes() < 0) {
                    z = true;
                }
            } else {
                if (coderResultDecode.isOverflow()) {
                    break;
                }
                coderResultDecode.throwException();
            }
        }
        if (z) {
            CoderResult coderResultFlush2 = this.decoder.flush(charBufferWrap);
            if (coderResultFlush2.isOverflow()) {
                this.needsFlush = true;
                return charBufferWrap.position();
            }
            this.decoder.reset();
            if (!coderResultFlush2.isUnderflow()) {
                coderResultFlush2.throwException();
            }
        }
        if (charBufferWrap.position() == 0 && z) {
            return -1;
        }
        return charBufferWrap.position();
    }

    String encodingName() {
        if (this.cs instanceof HistoricallyNamedCharset) {
            return ((HistoricallyNamedCharset) this.cs).historicalName();
        }
        return this.cs.name();
    }

    private boolean inReady() {
        try {
            if (this.in == null || this.in.available() <= 0) {
                if (!(this.ch instanceof FileChannel)) {
                    return $assertionsDisabled;
                }
            }
            return true;
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    boolean implReady() {
        if (this.bb.hasRemaining() || inReady()) {
            return true;
        }
        return $assertionsDisabled;
    }

    void implClose() throws IOException {
        if (this.ch != null) {
            this.ch.close();
        } else {
            this.in.close();
        }
    }
}
