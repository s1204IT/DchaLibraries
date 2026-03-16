package java.io;

import java.util.Arrays;

public class BufferedReader extends Reader {
    private char[] buf;
    private int end;
    private Reader in;
    private boolean lastWasCR;
    private int mark;
    private int markLimit;
    private boolean markedLastWasCR;
    private int pos;

    public BufferedReader(Reader in) {
        this(in, 8192);
    }

    public BufferedReader(Reader in, int size) {
        super(in);
        this.mark = -1;
        this.markLimit = -1;
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        this.in = in;
        this.buf = new char[size];
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (!isClosed()) {
                this.in.close();
                this.buf = null;
            }
        }
    }

    private int fillBuf() throws IOException {
        if (this.mark == -1 || this.pos - this.mark >= this.markLimit) {
            int result = this.in.read(this.buf, 0, this.buf.length);
            if (result > 0) {
                this.mark = -1;
                this.pos = 0;
                this.end = result;
            }
            return result;
        }
        if (this.mark == 0 && this.markLimit > this.buf.length) {
            int newLength = this.buf.length * 2;
            if (newLength > this.markLimit) {
                newLength = this.markLimit;
            }
            char[] newbuf = new char[newLength];
            System.arraycopy(this.buf, 0, newbuf, 0, this.buf.length);
            this.buf = newbuf;
        } else if (this.mark > 0) {
            System.arraycopy(this.buf, this.mark, this.buf, 0, this.buf.length - this.mark);
            this.pos -= this.mark;
            this.end -= this.mark;
            this.mark = 0;
        }
        int count = this.in.read(this.buf, this.pos, this.buf.length - this.pos);
        if (count != -1) {
            this.end += count;
            return count;
        }
        return count;
    }

    private boolean isClosed() {
        return this.buf == null;
    }

    @Override
    public void mark(int markLimit) throws IOException {
        if (markLimit < 0) {
            throw new IllegalArgumentException("markLimit < 0:" + markLimit);
        }
        synchronized (this.lock) {
            checkNotClosed();
            this.markLimit = markLimit;
            this.mark = this.pos;
            this.markedLastWasCR = this.lastWasCR;
        }
    }

    private void checkNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("BufferedReader is closed");
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        int ch;
        synchronized (this.lock) {
            checkNotClosed();
            ch = readChar();
            if (this.lastWasCR && ch == 10) {
                ch = readChar();
            }
            this.lastWasCR = false;
        }
        return ch;
    }

    private int readChar() throws IOException {
        if (this.pos >= this.end && fillBuf() == -1) {
            return -1;
        }
        char[] cArr = this.buf;
        int i = this.pos;
        this.pos = i + 1;
        return cArr[i];
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            Arrays.checkOffsetAndCount(buffer.length, offset, length);
            if (length == 0) {
                return 0;
            }
            maybeSwallowLF();
            int outstanding = length;
            while (true) {
                if (outstanding <= 0) {
                    break;
                }
                int available = this.end - this.pos;
                if (available > 0) {
                    int count = available >= outstanding ? outstanding : available;
                    System.arraycopy(this.buf, this.pos, buffer, offset, count);
                    this.pos += count;
                    offset += count;
                    outstanding -= count;
                }
                if (outstanding == 0 || (outstanding < length && !this.in.ready())) {
                    break;
                }
                if ((this.mark == -1 || this.pos - this.mark >= this.markLimit) && outstanding >= this.buf.length) {
                    int count2 = this.in.read(buffer, offset, outstanding);
                    if (count2 > 0) {
                        outstanding -= count2;
                        this.mark = -1;
                    }
                } else if (fillBuf() == -1) {
                    break;
                }
            }
            int count3 = length - outstanding;
            if (count3 > 0) {
                return count3;
            }
            return -1;
        }
    }

    final void chompNewline() throws IOException {
        if ((this.pos != this.end || fillBuf() != -1) && this.buf[this.pos] == '\n') {
            this.pos++;
        }
    }

    private void maybeSwallowLF() throws IOException {
        if (this.lastWasCR) {
            chompNewline();
            this.lastWasCR = false;
        }
    }

    public String readLine() throws IOException {
        String line;
        synchronized (this.lock) {
            checkNotClosed();
            maybeSwallowLF();
            for (int i = this.pos; i < this.end; i++) {
                char ch = this.buf[i];
                if (ch == '\n' || ch == '\r') {
                    line = new String(this.buf, this.pos, i - this.pos);
                    this.pos = i + 1;
                    this.lastWasCR = ch == '\r';
                }
            }
            StringBuilder result = new StringBuilder((this.end - this.pos) + 80);
            result.append(this.buf, this.pos, this.end - this.pos);
            loop1: while (true) {
                this.pos = this.end;
                if (fillBuf() == -1) {
                    line = result.length() > 0 ? result.toString() : null;
                } else {
                    int i2 = this.pos;
                    while (i2 < this.end) {
                        char ch2 = this.buf[i2];
                        if (ch2 == '\n' || ch2 == '\r') {
                            break loop1;
                        }
                        i2++;
                    }
                    result.append(this.buf, this.pos, this.end - this.pos);
                }
            }
        }
        return line;
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            checkNotClosed();
            z = this.end - this.pos > 0 || this.in.ready();
        }
        return z;
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            checkNotClosed();
            if (this.mark == -1) {
                throw new IOException("Invalid mark");
            }
            this.pos = this.mark;
            this.lastWasCR = this.markedLastWasCR;
        }
    }

    @Override
    public long skip(long charCount) throws IOException {
        if (charCount < 0) {
            throw new IllegalArgumentException("charCount < 0: " + charCount);
        }
        synchronized (this.lock) {
            checkNotClosed();
            if (this.end - this.pos >= charCount) {
                this.pos = (int) (((long) this.pos) + charCount);
                return charCount;
            }
            long read = this.end - this.pos;
            this.pos = this.end;
            while (read < charCount) {
                if (fillBuf() == -1) {
                    return read;
                }
                if (this.end - this.pos >= charCount - read) {
                    this.pos = (int) (((long) this.pos) + (charCount - read));
                    return charCount;
                }
                read += (long) (this.end - this.pos);
                this.pos = this.end;
            }
            return charCount;
        }
    }
}
