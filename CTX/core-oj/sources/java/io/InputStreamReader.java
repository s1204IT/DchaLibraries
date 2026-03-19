package java.io;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import sun.nio.cs.StreamDecoder;

public class InputStreamReader extends Reader {
    private final StreamDecoder sd;

    public InputStreamReader(InputStream inputStream) {
        super(inputStream);
        try {
            this.sd = StreamDecoder.forInputStreamReader(inputStream, this, (String) null);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public InputStreamReader(InputStream inputStream, String str) throws UnsupportedEncodingException {
        super(inputStream);
        if (str == null) {
            throw new NullPointerException("charsetName");
        }
        this.sd = StreamDecoder.forInputStreamReader(inputStream, this, str);
    }

    public InputStreamReader(InputStream inputStream, Charset charset) {
        super(inputStream);
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.sd = StreamDecoder.forInputStreamReader(inputStream, this, charset);
    }

    public InputStreamReader(InputStream inputStream, CharsetDecoder charsetDecoder) {
        super(inputStream);
        if (charsetDecoder == null) {
            throw new NullPointerException("charset decoder");
        }
        this.sd = StreamDecoder.forInputStreamReader(inputStream, this, charsetDecoder);
    }

    public String getEncoding() {
        return this.sd.getEncoding();
    }

    @Override
    public int read() throws IOException {
        return this.sd.read();
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        return this.sd.read(cArr, i, i2);
    }

    @Override
    public boolean ready() throws IOException {
        return this.sd.ready();
    }

    @Override
    public void close() throws IOException {
        this.sd.close();
    }
}
