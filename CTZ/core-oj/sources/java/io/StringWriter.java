package java.io;

public class StringWriter extends Writer {
    private StringBuffer buf;

    public StringWriter() {
        this.buf = new StringBuffer();
        this.lock = this.buf;
    }

    public StringWriter(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Negative buffer size");
        }
        this.buf = new StringBuffer(i);
        this.lock = this.buf;
    }

    @Override
    public void write(int i) {
        this.buf.append((char) i);
    }

    @Override
    public void write(char[] cArr, int i, int i2) {
        int i3;
        if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        this.buf.append(cArr, i, i2);
    }

    @Override
    public void write(String str) {
        this.buf.append(str);
    }

    @Override
    public void write(String str, int i, int i2) {
        this.buf.append(str.substring(i, i2 + i));
    }

    @Override
    public StringWriter append(CharSequence charSequence) {
        if (charSequence == null) {
            write("null");
        } else {
            write(charSequence.toString());
        }
        return this;
    }

    @Override
    public StringWriter append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        write(charSequence.subSequence(i, i2).toString());
        return this;
    }

    @Override
    public StringWriter append(char c) {
        write(c);
        return this;
    }

    public String toString() {
        return this.buf.toString();
    }

    public StringBuffer getBuffer() {
        return this.buf;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws IOException {
    }
}
