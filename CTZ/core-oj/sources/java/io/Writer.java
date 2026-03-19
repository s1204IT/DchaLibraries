package java.io;

public abstract class Writer implements Appendable, Closeable, Flushable {
    private static final int WRITE_BUFFER_SIZE = 1024;
    protected Object lock;
    private char[] writeBuffer;

    public abstract void close() throws IOException;

    public abstract void flush() throws IOException;

    public abstract void write(char[] cArr, int i, int i2) throws IOException;

    protected Writer() {
        this.lock = this;
    }

    protected Writer(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        this.lock = obj;
    }

    public void write(int i) throws IOException {
        synchronized (this.lock) {
            if (this.writeBuffer == null) {
                this.writeBuffer = new char[1024];
            }
            this.writeBuffer[0] = (char) i;
            write(this.writeBuffer, 0, 1);
        }
    }

    public void write(char[] cArr) throws IOException {
        write(cArr, 0, cArr.length);
    }

    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    public void write(String str, int i, int i2) throws IOException {
        char[] cArr;
        synchronized (this.lock) {
            try {
                if (i2 <= 1024) {
                    if (this.writeBuffer == null) {
                        this.writeBuffer = new char[1024];
                    }
                    cArr = this.writeBuffer;
                } else {
                    cArr = new char[i2];
                }
                str.getChars(i, i + i2, cArr, 0);
                write(cArr, 0, i2);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override
    public Writer append(CharSequence charSequence) throws IOException {
        if (charSequence == null) {
            write("null");
        } else {
            write(charSequence.toString());
        }
        return this;
    }

    @Override
    public Writer append(CharSequence charSequence, int i, int i2) throws IOException {
        if (charSequence == null) {
            charSequence = "null";
        }
        write(charSequence.subSequence(i, i2).toString());
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }
}
