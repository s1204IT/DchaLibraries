package java.io;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BufferedReader extends Reader {
    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;
    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;
    private char[] cb;
    private Reader in;
    private int markedChar;
    private boolean markedSkipLF;
    private int nChars;
    private int nextChar;
    private int readAheadLimit;
    private boolean skipLF;

    public BufferedReader(Reader reader, int i) {
        super(reader);
        this.markedChar = -1;
        this.readAheadLimit = 0;
        this.skipLF = false;
        this.markedSkipLF = false;
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = reader;
        this.cb = new char[i];
        this.nChars = 0;
        this.nextChar = 0;
    }

    public BufferedReader(Reader reader) {
        this(reader, defaultCharBufferSize);
    }

    private void ensureOpen() throws IOException {
        if (this.in == null) {
            throw new IOException("Stream closed");
        }
    }

    private void fill() throws IOException {
        int i;
        int i2 = 0;
        if (this.markedChar > -1) {
            int i3 = this.nextChar - this.markedChar;
            if (i3 >= this.readAheadLimit) {
                this.markedChar = -2;
                this.readAheadLimit = 0;
            } else {
                if (this.readAheadLimit <= this.cb.length) {
                    System.arraycopy((Object) this.cb, this.markedChar, (Object) this.cb, 0, i3);
                    this.markedChar = 0;
                } else {
                    int length = this.cb.length * 2;
                    if (length > this.readAheadLimit) {
                        length = this.readAheadLimit;
                    }
                    char[] cArr = new char[length];
                    System.arraycopy((Object) this.cb, this.markedChar, (Object) cArr, 0, i3);
                    this.cb = cArr;
                    this.markedChar = 0;
                }
                this.nChars = i3;
                this.nextChar = i3;
                i2 = i3;
            }
        }
        do {
            i = this.in.read(this.cb, i2, this.cb.length - i2);
        } while (i == 0);
        if (i > 0) {
            this.nChars = i + i2;
            this.nextChar = i2;
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            while (true) {
                if (this.nextChar >= this.nChars) {
                    fill();
                    if (this.nextChar >= this.nChars) {
                        return -1;
                    }
                }
                if (!this.skipLF) {
                    break;
                }
                this.skipLF = false;
                if (this.cb[this.nextChar] != '\n') {
                    break;
                }
                this.nextChar++;
            }
            char[] cArr = this.cb;
            int i = this.nextChar;
            this.nextChar = i + 1;
            return cArr[i];
        }
    }

    private int read1(char[] cArr, int i, int i2) throws IOException {
        if (this.nextChar >= this.nChars) {
            if (i2 >= this.cb.length && this.markedChar <= -1 && !this.skipLF) {
                return this.in.read(cArr, i, i2);
            }
            fill();
        }
        if (this.nextChar >= this.nChars) {
            return -1;
        }
        if (this.skipLF) {
            this.skipLF = false;
            if (this.cb[this.nextChar] == '\n') {
                this.nextChar++;
                if (this.nextChar >= this.nChars) {
                    fill();
                }
                if (this.nextChar >= this.nChars) {
                    return -1;
                }
            }
        }
        int iMin = Math.min(i2, this.nChars - this.nextChar);
        System.arraycopy((Object) this.cb, this.nextChar, (Object) cArr, i, iMin);
        this.nextChar += iMin;
        return iMin;
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            ensureOpen();
            if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) > cArr.length || i3 < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (i2 == 0) {
                return 0;
            }
            int i4 = read1(cArr, i, i2);
            if (i4 <= 0) {
                return i4;
            }
            while (i4 < i2 && this.in.ready()) {
                int i5 = read1(cArr, i + i4, i2 - i4);
                if (i5 <= 0) {
                    break;
                }
                i4 += i5;
            }
            return i4;
        }
    }

    String readLine(boolean z) throws IOException {
        boolean z2;
        String string;
        synchronized (this.lock) {
            ensureOpen();
            boolean z3 = z || this.skipLF;
            StringBuffer stringBuffer = null;
            while (true) {
                if (this.nextChar >= this.nChars) {
                    fill();
                }
                if (this.nextChar >= this.nChars) {
                    if (stringBuffer == null || stringBuffer.length() <= 0) {
                        return null;
                    }
                    return stringBuffer.toString();
                }
                if (z3 && this.cb[this.nextChar] == '\n') {
                    this.nextChar++;
                }
                this.skipLF = false;
                int i = this.nextChar;
                char c = 0;
                while (i < this.nChars) {
                    c = this.cb[i];
                    if (c != '\n' && c != '\r') {
                        i++;
                    }
                    z2 = true;
                }
                z2 = false;
                int i2 = this.nextChar;
                this.nextChar = i;
                if (z2) {
                    if (stringBuffer == null) {
                        string = new String(this.cb, i2, i - i2);
                    } else {
                        stringBuffer.append(this.cb, i2, i - i2);
                        string = stringBuffer.toString();
                    }
                    this.nextChar++;
                    if (c == '\r') {
                        this.skipLF = true;
                    }
                    return string;
                }
                if (stringBuffer == null) {
                    stringBuffer = new StringBuffer(defaultExpectedLineLength);
                }
                stringBuffer.append(this.cb, i2, i - i2);
                z3 = false;
            }
        }
    }

    public String readLine() throws IOException {
        return readLine(false);
    }

    @Override
    public long skip(long j) throws IOException {
        long j2;
        long j3 = 0;
        if (j < 0) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (this.lock) {
            ensureOpen();
            long j4 = j;
            while (j4 > 0) {
                if (this.nextChar >= this.nChars) {
                    fill();
                }
                if (this.nextChar >= this.nChars) {
                    break;
                }
                if (this.skipLF) {
                    this.skipLF = false;
                    if (this.cb[this.nextChar] == '\n') {
                        this.nextChar++;
                    }
                }
                long j5 = this.nChars - this.nextChar;
                if (j4 <= j5) {
                    this.nextChar = (int) (((long) this.nextChar) + j4);
                    break;
                }
                j4 -= j5;
                this.nextChar = this.nChars;
            }
            j3 = j4;
            j2 = j - j3;
        }
        return j2;
    }

    @Override
    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            z = false;
            if (this.skipLF) {
                if (this.nextChar >= this.nChars && this.in.ready()) {
                    fill();
                }
                if (this.nextChar < this.nChars) {
                    if (this.cb[this.nextChar] == '\n') {
                        this.nextChar++;
                    }
                    this.skipLF = false;
                }
            }
            if (this.nextChar < this.nChars || this.in.ready()) {
                z = true;
            }
        }
        return z;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int i) throws IOException {
        if (i < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (this.lock) {
            ensureOpen();
            this.readAheadLimit = i;
            this.markedChar = this.nextChar;
            this.markedSkipLF = this.skipLF;
        }
    }

    @Override
    public void reset() throws IOException {
        String str;
        synchronized (this.lock) {
            ensureOpen();
            if (this.markedChar < 0) {
                if (this.markedChar == -2) {
                    str = "Mark invalid";
                } else {
                    str = "Stream not marked";
                }
                throw new IOException(str);
            }
            this.nextChar = this.markedChar;
            this.skipLF = this.markedSkipLF;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.in == null) {
                return;
            }
            try {
                this.in.close();
            } finally {
                this.in = null;
                this.cb = null;
            }
        }
    }

    public Stream<String> lines() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (this.nextLine != null) {
                    return true;
                }
                try {
                    this.nextLine = BufferedReader.this.readLine();
                    return this.nextLine != null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public String next() {
                if (this.nextLine != null || hasNext()) {
                    String str = this.nextLine;
                    this.nextLine = null;
                    return str;
                }
                throw new NoSuchElementException();
            }
        }, 272), false);
    }
}
