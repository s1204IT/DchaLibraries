package java.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public class OutputStreamWriter extends Writer {
    private ByteBuffer bytes;
    private CharsetEncoder encoder;
    private final OutputStream out;

    public OutputStreamWriter(OutputStream out) {
        this(out, Charset.defaultCharset());
    }

    public OutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
        super(out);
        this.bytes = ByteBuffer.allocate(8192);
        if (charsetName == null) {
            throw new NullPointerException("charsetName == null");
        }
        this.out = out;
        try {
            this.encoder = Charset.forName(charsetName).newEncoder();
            this.encoder.onMalformedInput(CodingErrorAction.REPLACE);
            this.encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(charsetName);
        }
    }

    public OutputStreamWriter(OutputStream out, Charset cs) {
        super(out);
        this.bytes = ByteBuffer.allocate(8192);
        this.out = out;
        this.encoder = cs.newEncoder();
        this.encoder.onMalformedInput(CodingErrorAction.REPLACE);
        this.encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    public OutputStreamWriter(OutputStream out, CharsetEncoder charsetEncoder) {
        super(out);
        this.bytes = ByteBuffer.allocate(8192);
        charsetEncoder.charset();
        this.out = out;
        this.encoder = charsetEncoder;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.encoder != null) {
                drainEncoder();
                flushBytes(false);
                this.out.close();
                this.encoder = null;
                this.bytes = null;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushBytes(true);
    }

    private void flushBytes(boolean flushUnderlyingStream) throws IOException {
        synchronized (this.lock) {
            checkStatus();
            int position = this.bytes.position();
            if (position > 0) {
                this.bytes.flip();
                this.out.write(this.bytes.array(), this.bytes.arrayOffset(), position);
                this.bytes.clear();
            }
            if (flushUnderlyingStream) {
                this.out.flush();
            }
        }
    }

    private void convert(CharBuffer chars) throws IOException {
        CoderResult result;
        while (true) {
            result = this.encoder.encode(chars, this.bytes, false);
            if (!result.isOverflow()) {
                break;
            } else {
                flushBytes(false);
            }
        }
        if (result.isError()) {
            result.throwException();
        }
    }

    private void drainEncoder() throws IOException {
        CharBuffer chars = CharBuffer.allocate(0);
        while (true) {
            CoderResult result = this.encoder.encode(chars, this.bytes, true);
            if (result.isError()) {
                result.throwException();
                break;
            } else if (!result.isOverflow()) {
                break;
            } else {
                flushBytes(false);
            }
        }
        CoderResult result2 = this.encoder.flush(this.bytes);
        while (!result2.isUnderflow()) {
            if (result2.isOverflow()) {
                flushBytes(false);
                result2 = this.encoder.flush(this.bytes);
            } else {
                result2.throwException();
            }
        }
    }

    private void checkStatus() throws IOException {
        if (this.encoder == null) {
            throw new IOException("OutputStreamWriter is closed");
        }
    }

    public String getEncoding() {
        if (this.encoder == null) {
            return null;
        }
        return this.encoder.charset().name();
    }

    @Override
    public void write(char[] buffer, int offset, int count) throws IOException {
        synchronized (this.lock) {
            checkStatus();
            Arrays.checkOffsetAndCount(buffer.length, offset, count);
            CharBuffer chars = CharBuffer.wrap(buffer, offset, count);
            convert(chars);
        }
    }

    @Override
    public void write(int oneChar) throws IOException {
        synchronized (this.lock) {
            checkStatus();
            CharBuffer chars = CharBuffer.wrap(new char[]{(char) oneChar});
            convert(chars);
        }
    }

    @Override
    public void write(String str, int offset, int count) throws IOException {
        synchronized (this.lock) {
            if (count < 0) {
                throw new StringIndexOutOfBoundsException(str, offset, count);
            }
            if (str == null) {
                throw new NullPointerException("str == null");
            }
            if ((offset | count) < 0 || offset > str.length() - count) {
                throw new StringIndexOutOfBoundsException(str, offset, count);
            }
            checkStatus();
            CharBuffer chars = CharBuffer.wrap(str, offset, count + offset);
            convert(chars);
        }
    }

    @Override
    boolean checkError() {
        return this.out.checkError();
    }
}
