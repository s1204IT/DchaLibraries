package java.io;

import java.util.Arrays;

public class CharArrayWriter extends Writer {
    protected char[] buf;
    protected int count;

    public CharArrayWriter() {
        this(32);
    }

    public CharArrayWriter(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Negative initial size: " + i);
        }
        this.buf = new char[i];
    }

    @Override
    public void write(int i) {
        synchronized (this.lock) {
            int i2 = this.count + 1;
            if (i2 > this.buf.length) {
                this.buf = Arrays.copyOf(this.buf, Math.max(this.buf.length << 1, i2));
            }
            this.buf[this.count] = (char) i;
            this.count = i2;
        }
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
        synchronized (this.lock) {
            int i4 = this.count + i2;
            if (i4 > this.buf.length) {
                this.buf = Arrays.copyOf(this.buf, Math.max(this.buf.length << 1, i4));
            }
            System.arraycopy((Object) cArr, i, (Object) this.buf, this.count, i2);
            this.count = i4;
        }
    }

    @Override
    public void write(String str, int i, int i2) {
        synchronized (this.lock) {
            int i3 = this.count + i2;
            if (i3 > this.buf.length) {
                this.buf = Arrays.copyOf(this.buf, Math.max(this.buf.length << 1, i3));
            }
            str.getChars(i, i2 + i, this.buf, this.count);
            this.count = i3;
        }
    }

    public void writeTo(Writer writer) throws IOException {
        synchronized (this.lock) {
            writer.write(this.buf, 0, this.count);
        }
    }

    @Override
    public CharArrayWriter append(CharSequence charSequence) {
        String string = charSequence == null ? "null" : charSequence.toString();
        write(string, 0, string.length());
        return this;
    }

    @Override
    public CharArrayWriter append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        String string = charSequence.subSequence(i, i2).toString();
        write(string, 0, string.length());
        return this;
    }

    @Override
    public CharArrayWriter append(char c) {
        write(c);
        return this;
    }

    public void reset() {
        this.count = 0;
    }

    public char[] toCharArray() {
        char[] cArrCopyOf;
        synchronized (this.lock) {
            cArrCopyOf = Arrays.copyOf(this.buf, this.count);
        }
        return cArrCopyOf;
    }

    public int size() {
        return this.count;
    }

    public String toString() {
        String str;
        synchronized (this.lock) {
            str = new String(this.buf, 0, this.count);
        }
        return str;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
