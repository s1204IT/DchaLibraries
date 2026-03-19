package java.io;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;

public class BufferedWriter extends Writer {
    private static int defaultCharBufferSize = 8192;
    private char[] cb;
    private String lineSeparator;
    private int nChars;
    private int nextChar;
    private Writer out;

    public BufferedWriter(Writer writer) {
        this(writer, defaultCharBufferSize);
    }

    public BufferedWriter(Writer writer, int i) {
        super(writer);
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.out = writer;
        this.cb = new char[i];
        this.nChars = i;
        this.nextChar = 0;
        this.lineSeparator = (String) AccessController.doPrivileged(new GetPropertyAction("line.separator"));
    }

    private void ensureOpen() throws IOException {
        if (this.out == null) {
            throw new IOException("Stream closed");
        }
    }

    void flushBuffer() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.nextChar == 0) {
                return;
            }
            this.out.write(this.cb, 0, this.nextChar);
            this.nextChar = 0;
        }
    }

    @Override
    public void write(int i) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.nextChar >= this.nChars) {
                flushBuffer();
            }
            char[] cArr = this.cb;
            int i2 = this.nextChar;
            this.nextChar = i2 + 1;
            cArr[i2] = (char) i;
        }
    }

    private int min(int i, int i2) {
        return i < i2 ? i : i2;
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            ensureOpen();
            if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (i2 == 0) {
                return;
            }
            if (i2 >= this.nChars) {
                flushBuffer();
                this.out.write(cArr, i, i2);
                return;
            }
            while (i < i3) {
                int iMin = min(this.nChars - this.nextChar, i3 - i);
                System.arraycopy((Object) cArr, i, (Object) this.cb, this.nextChar, iMin);
                i += iMin;
                this.nextChar += iMin;
                if (this.nextChar >= this.nChars) {
                    flushBuffer();
                }
            }
        }
    }

    @Override
    public void write(String str, int i, int i2) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            int i3 = i2 + i;
            while (i < i3) {
                int iMin = min(this.nChars - this.nextChar, i3 - i);
                int i4 = i + iMin;
                str.getChars(i, i4, this.cb, this.nextChar);
                this.nextChar += iMin;
                if (this.nextChar >= this.nChars) {
                    flushBuffer();
                }
                i = i4;
            }
        }
    }

    public void newLine() throws IOException {
        write(this.lineSeparator);
    }

    @Override
    public void flush() throws IOException {
        synchronized (this.lock) {
            flushBuffer();
            this.out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        Throwable th;
        synchronized (this.lock) {
            if (this.out == null) {
                return;
            }
            try {
                Writer writer = this.out;
                try {
                    flushBuffer();
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    if (writer != null) {
                    }
                }
            } finally {
                this.out = null;
                this.cb = null;
            }
        }
    }
}
