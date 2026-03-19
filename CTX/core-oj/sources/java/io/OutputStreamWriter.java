package java.io;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import sun.nio.cs.StreamEncoder;

public class OutputStreamWriter extends Writer {
    private final StreamEncoder se;

    public OutputStreamWriter(OutputStream outputStream, String str) throws UnsupportedEncodingException {
        super(outputStream);
        if (str == null) {
            throw new NullPointerException("charsetName");
        }
        this.se = StreamEncoder.forOutputStreamWriter(outputStream, this, str);
    }

    public OutputStreamWriter(OutputStream outputStream) {
        super(outputStream);
        try {
            this.se = StreamEncoder.forOutputStreamWriter(outputStream, this, (String) null);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public OutputStreamWriter(OutputStream outputStream, Charset charset) {
        super(outputStream);
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.se = StreamEncoder.forOutputStreamWriter(outputStream, this, charset);
    }

    public OutputStreamWriter(OutputStream outputStream, CharsetEncoder charsetEncoder) {
        super(outputStream);
        if (charsetEncoder == null) {
            throw new NullPointerException("charset encoder");
        }
        this.se = StreamEncoder.forOutputStreamWriter(outputStream, this, charsetEncoder);
    }

    public String getEncoding() {
        return this.se.getEncoding();
    }

    void flushBuffer() throws IOException {
        this.se.flushBuffer();
    }

    @Override
    public void write(int i) throws IOException {
        this.se.write(i);
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        this.se.write(cArr, i, i2);
    }

    @Override
    public void write(String str, int i, int i2) throws IOException {
        this.se.write(str, i, i2);
    }

    @Override
    public void flush() throws IOException {
        this.se.flush();
    }

    @Override
    public void close() throws IOException {
        this.se.close();
    }
}
