package java.io;

import java.util.Arrays;

public class StringWriter extends Writer {
    private StringBuffer buf;

    public StringWriter() {
        this.buf = new StringBuffer(16);
        this.lock = this.buf;
    }

    public StringWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("initialSize < 0: " + initialSize);
        }
        this.buf = new StringBuffer(initialSize);
        this.lock = this.buf;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() {
    }

    public StringBuffer getBuffer() {
        return this.buf;
    }

    public String toString() {
        return this.buf.toString();
    }

    @Override
    public void write(char[] chars, int offset, int count) {
        Arrays.checkOffsetAndCount(chars.length, offset, count);
        if (count != 0) {
            this.buf.append(chars, offset, count);
        }
    }

    @Override
    public void write(int oneChar) {
        this.buf.append((char) oneChar);
    }

    @Override
    public void write(String str) {
        this.buf.append(str);
    }

    @Override
    public void write(String str, int offset, int count) {
        String sub = str.substring(offset, offset + count);
        this.buf.append(sub);
    }

    @Override
    public StringWriter append(char c) {
        write(c);
        return this;
    }

    @Override
    public StringWriter append(CharSequence csq) {
        if (csq == null) {
            csq = "null";
        }
        write(csq.toString());
        return this;
    }

    @Override
    public StringWriter append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        String output = csq.subSequence(start, end).toString();
        write(output, 0, output.length());
        return this;
    }
}
