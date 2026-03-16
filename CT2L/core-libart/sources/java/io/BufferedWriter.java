package java.io;

import java.util.Arrays;
import libcore.util.SneakyThrow;

public class BufferedWriter extends Writer {
    private char[] buf;
    private Writer out;
    private int pos;

    public BufferedWriter(Writer out) {
        this(out, 8192);
    }

    public BufferedWriter(Writer out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.out = out;
        this.buf = new char[size];
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (!isClosed()) {
                Throwable thrown = null;
                try {
                    flushInternal();
                } catch (Throwable e) {
                    thrown = e;
                }
                this.buf = null;
                try {
                    this.out.close();
                } catch (Throwable e2) {
                    if (thrown == null) {
                        thrown = e2;
                    }
                }
                this.out = null;
                if (thrown != null) {
                    SneakyThrow.sneakyThrow(thrown);
                }
            }
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            flushInternal();
            this.out.flush();
        }
    }

    private void checkNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("BufferedWriter is closed");
        }
    }

    private void flushInternal() throws IOException {
        if (this.pos > 0) {
            this.out.write(this.buf, 0, this.pos);
        }
        this.pos = 0;
    }

    private boolean isClosed() {
        return this.out == null;
    }

    public void newLine() throws IOException {
        write(System.lineSeparator());
    }

    @Override
    public void write(char[] buffer, int offset, int count) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            if (buffer == null) {
                throw new NullPointerException("buffer == null");
            }
            Arrays.checkOffsetAndCount(buffer.length, offset, count);
            if (this.pos == 0 && count >= this.buf.length) {
                this.out.write(buffer, offset, count);
                return;
            }
            int available = this.buf.length - this.pos;
            if (count < available) {
                available = count;
            }
            if (available > 0) {
                System.arraycopy(buffer, offset, this.buf, this.pos, available);
                this.pos += available;
            }
            if (this.pos == this.buf.length) {
                this.out.write(this.buf, 0, this.buf.length);
                this.pos = 0;
                if (count > available) {
                    int offset2 = offset + available;
                    int available2 = count - available;
                    if (available2 >= this.buf.length) {
                        this.out.write(buffer, offset2, available2);
                    } else {
                        System.arraycopy(buffer, offset2, this.buf, this.pos, available2);
                        this.pos += available2;
                    }
                }
            }
        }
    }

    @Override
    public void write(int oneChar) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            if (this.pos >= this.buf.length) {
                this.out.write(this.buf, 0, this.buf.length);
                this.pos = 0;
            }
            char[] cArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            cArr[i] = (char) oneChar;
        }
    }

    @Override
    public void write(String str, int offset, int count) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            if (count > 0) {
                if (offset < 0 || offset > str.length() - count) {
                    throw new StringIndexOutOfBoundsException(str, offset, count);
                }
                if (this.pos == 0 && count >= this.buf.length) {
                    char[] chars = new char[count];
                    str.getChars(offset, offset + count, chars, 0);
                    this.out.write(chars, 0, count);
                    return;
                }
                int available = this.buf.length - this.pos;
                if (count < available) {
                    available = count;
                }
                if (available > 0) {
                    str.getChars(offset, offset + available, this.buf, this.pos);
                    this.pos += available;
                }
                if (this.pos == this.buf.length) {
                    this.out.write(this.buf, 0, this.buf.length);
                    this.pos = 0;
                    if (count > available) {
                        int offset2 = offset + available;
                        int available2 = count - available;
                        if (available2 >= this.buf.length) {
                            char[] chars2 = new char[count];
                            str.getChars(offset2, offset2 + available2, chars2, 0);
                            this.out.write(chars2, 0, available2);
                            return;
                        }
                        str.getChars(offset2, offset2 + available2, this.buf, this.pos);
                        this.pos += available2;
                    }
                }
            }
        }
    }
}
