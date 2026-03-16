package java.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public class InputStreamReader extends Reader {
    private final ByteBuffer bytes;
    private CharsetDecoder decoder;
    private boolean endOfInput;
    private InputStream in;

    public InputStreamReader(InputStream in) {
        this(in, Charset.defaultCharset());
    }

    public InputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException {
        super(in);
        this.endOfInput = false;
        this.bytes = ByteBuffer.allocate(8192);
        if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        this.in = in;
        try {
            this.decoder = Charset.forName(charsetName).newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.bytes.limit(0);
        } catch (IllegalArgumentException e) {
            throw ((UnsupportedEncodingException) new UnsupportedEncodingException(charsetName).initCause(e));
        }
    }

    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in);
        this.endOfInput = false;
        this.bytes = ByteBuffer.allocate(8192);
        dec.averageCharsPerByte();
        this.in = in;
        this.decoder = dec;
        this.bytes.limit(0);
    }

    public InputStreamReader(InputStream in, Charset charset) {
        super(in);
        this.endOfInput = false;
        this.bytes = ByteBuffer.allocate(8192);
        this.in = in;
        this.decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.bytes.limit(0);
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.decoder != null) {
                this.decoder.reset();
            }
            this.decoder = null;
            if (this.in != null) {
                this.in.close();
                this.in = null;
            }
        }
    }

    public String getEncoding() {
        if (isOpen()) {
            return this.decoder.charset().name();
        }
        return null;
    }

    @Override
    public int read() throws IOException {
        int i;
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new IOException("InputStreamReader is closed");
            }
            char[] cArr = new char[1];
            i = read(cArr, 0, 1) != -1 ? cArr[0] : -1;
        }
        return i;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        int iPosition = 0;
        synchronized (this.lock) {
            if (!isOpen()) {
                throw new IOException("InputStreamReader is closed");
            }
            Arrays.checkOffsetAndCount(buffer.length, offset, count);
            if (count != 0) {
                CharBuffer out = CharBuffer.wrap(buffer, offset, count);
                CoderResult result = CoderResult.UNDERFLOW;
                boolean needInput = this.bytes.hasRemaining() ? false : true;
                while (true) {
                    if (!out.hasRemaining()) {
                        break;
                    }
                    if (needInput) {
                        if (this.in.available() == 0 && out.position() > offset) {
                            break;
                        }
                        int desiredByteCount = this.bytes.capacity() - this.bytes.limit();
                        int off = this.bytes.arrayOffset() + this.bytes.limit();
                        int actualByteCount = this.in.read(this.bytes.array(), off, desiredByteCount);
                        if (actualByteCount == -1) {
                            this.endOfInput = true;
                            break;
                        }
                        if (actualByteCount == 0) {
                            break;
                        }
                        this.bytes.limit(this.bytes.limit() + actualByteCount);
                        result = this.decoder.decode(this.bytes, out, false);
                        if (result.isUnderflow()) {
                            break;
                        }
                        if (this.bytes.limit() == this.bytes.capacity()) {
                            this.bytes.compact();
                            this.bytes.limit(this.bytes.position());
                            this.bytes.position(0);
                        }
                        needInput = true;
                    } else {
                        result = this.decoder.decode(this.bytes, out, false);
                        if (result.isUnderflow()) {
                        }
                    }
                }
                if (result == CoderResult.UNDERFLOW && this.endOfInput) {
                    result = this.decoder.decode(this.bytes, out, true);
                    if (result == CoderResult.UNDERFLOW) {
                        result = this.decoder.flush(out);
                    }
                    this.decoder.reset();
                }
                if (result.isMalformed() || result.isUnmappable()) {
                    result.throwException();
                }
                iPosition = out.position() - offset == 0 ? -1 : out.position() - offset;
            }
            return iPosition;
        }
    }

    private boolean isOpen() {
        return this.in != null;
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (this.lock) {
            if (this.in == null) {
                throw new IOException("InputStreamReader is closed");
            }
            try {
                if (!this.bytes.hasRemaining()) {
                    z = this.in.available() > 0;
                }
            } catch (IOException e) {
            }
        }
        return z;
    }
}
