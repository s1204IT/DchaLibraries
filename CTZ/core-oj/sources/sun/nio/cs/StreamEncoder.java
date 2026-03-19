package sun.nio.cs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;

public class StreamEncoder extends Writer {
    static final boolean $assertionsDisabled = false;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private ByteBuffer bb;
    private WritableByteChannel ch;
    private Charset cs;
    private CharsetEncoder encoder;
    private boolean haveLeftoverChar;
    private volatile boolean isOpen;
    private CharBuffer lcb;
    private char leftoverChar;
    private final OutputStream out;

    private void ensureOpen() throws IOException {
        if (!this.isOpen) {
            throw new IOException("Stream closed");
        }
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream outputStream, Object obj, String str) throws UnsupportedEncodingException {
        if (str == null) {
            str = Charset.defaultCharset().name();
        }
        try {
            if (Charset.isSupported(str)) {
                return new StreamEncoder(outputStream, obj, Charset.forName(str));
            }
        } catch (IllegalCharsetNameException e) {
        }
        throw new UnsupportedEncodingException(str);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream outputStream, Object obj, Charset charset) {
        return new StreamEncoder(outputStream, obj, charset);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream outputStream, Object obj, CharsetEncoder charsetEncoder) {
        return new StreamEncoder(outputStream, obj, charsetEncoder);
    }

    public static StreamEncoder forEncoder(WritableByteChannel writableByteChannel, CharsetEncoder charsetEncoder, int i) {
        return new StreamEncoder(writableByteChannel, charsetEncoder, i);
    }

    public String getEncoding() {
        if (isOpen()) {
            return encodingName();
        }
        return null;
    }

    public void flushBuffer() throws IOException {
        synchronized (this.lock) {
            if (isOpen()) {
                implFlushBuffer();
            } else {
                throw new IOException("Stream closed");
            }
        }
    }

    @Override
    public void write(int i) throws IOException {
        write(new char[]{(char) i}, 0, 1);
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            ensureOpen();
            if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (i2 == 0) {
                return;
            }
            implWrite(cArr, i, i2);
        }
    }

    @Override
    public void write(String str, int i, int i2) throws IOException {
        if (i2 < 0) {
            throw new IndexOutOfBoundsException();
        }
        char[] cArr = new char[i2];
        str.getChars(i, i + i2, cArr, 0);
        write(cArr, 0, i2);
    }

    @Override
    public void flush() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            implFlush();
        }
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

    private StreamEncoder(OutputStream outputStream, Object obj, Charset charset) {
        this(outputStream, obj, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    private StreamEncoder(OutputStream outputStream, Object obj, CharsetEncoder charsetEncoder) {
        super(obj);
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.lcb = null;
        this.out = outputStream;
        this.ch = null;
        this.cs = charsetEncoder.charset();
        this.encoder = charsetEncoder;
        if (this.ch == null) {
            this.bb = ByteBuffer.allocate(8192);
        }
    }

    private StreamEncoder(WritableByteChannel writableByteChannel, CharsetEncoder charsetEncoder, int i) {
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.lcb = null;
        this.out = null;
        this.ch = writableByteChannel;
        this.cs = charsetEncoder.charset();
        this.encoder = charsetEncoder;
        this.bb = ByteBuffer.allocate(i < 0 ? 8192 : i);
    }

    private void writeBytes() throws IOException {
        this.bb.flip();
        int iLimit = this.bb.limit();
        int iPosition = this.bb.position();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (i > 0) {
            if (this.ch != null) {
                if (this.ch.write(this.bb) != i) {
                }
            } else {
                this.out.write(this.bb.array(), this.bb.arrayOffset() + iPosition, i);
            }
        }
        this.bb.clear();
    }

    private void flushLeftoverChar(CharBuffer charBuffer, boolean z) throws IOException {
        if (!this.haveLeftoverChar && !z) {
            return;
        }
        if (this.lcb == null) {
            this.lcb = CharBuffer.allocate(2);
        } else {
            this.lcb.clear();
        }
        if (this.haveLeftoverChar) {
            this.lcb.put(this.leftoverChar);
        }
        if (charBuffer != null && charBuffer.hasRemaining()) {
            this.lcb.put(charBuffer.get());
        }
        this.lcb.flip();
        while (true) {
            if (!this.lcb.hasRemaining() && !z) {
                break;
            }
            CoderResult coderResultEncode = this.encoder.encode(this.lcb, this.bb, z);
            if (coderResultEncode.isUnderflow()) {
                if (this.lcb.hasRemaining()) {
                    this.leftoverChar = this.lcb.get();
                    if (charBuffer != null && charBuffer.hasRemaining()) {
                        flushLeftoverChar(charBuffer, z);
                        return;
                    }
                    return;
                }
            } else if (coderResultEncode.isOverflow()) {
                writeBytes();
            } else {
                coderResultEncode.throwException();
            }
        }
        this.haveLeftoverChar = $assertionsDisabled;
    }

    void implWrite(char[] cArr, int i, int i2) throws IOException {
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr, i, i2);
        if (this.haveLeftoverChar) {
            flushLeftoverChar(charBufferWrap, $assertionsDisabled);
        }
        while (charBufferWrap.hasRemaining()) {
            CoderResult coderResultEncode = this.encoder.encode(charBufferWrap, this.bb, $assertionsDisabled);
            if (coderResultEncode.isUnderflow()) {
                if (charBufferWrap.remaining() == 1) {
                    this.haveLeftoverChar = true;
                    this.leftoverChar = charBufferWrap.get();
                    return;
                }
                return;
            }
            if (coderResultEncode.isOverflow()) {
                writeBytes();
            } else {
                coderResultEncode.throwException();
            }
        }
    }

    void implFlushBuffer() throws IOException {
        if (this.bb.position() > 0) {
            writeBytes();
        }
    }

    void implFlush() throws IOException {
        implFlushBuffer();
        if (this.out != null) {
            this.out.flush();
        }
    }

    void implClose() throws IOException {
        flushLeftoverChar(null, true);
        while (true) {
            try {
                CoderResult coderResultFlush = this.encoder.flush(this.bb);
                if (coderResultFlush.isUnderflow()) {
                    break;
                } else if (coderResultFlush.isOverflow()) {
                    writeBytes();
                } else {
                    coderResultFlush.throwException();
                }
            } catch (IOException e) {
                this.encoder.reset();
                throw e;
            }
        }
        if (this.bb.position() > 0) {
            writeBytes();
        }
        if (this.ch != null) {
            this.ch.close();
        } else {
            this.out.close();
        }
    }

    String encodingName() {
        if (this.cs instanceof HistoricallyNamedCharset) {
            return ((HistoricallyNamedCharset) this.cs).historicalName();
        }
        return this.cs.name();
    }
}
