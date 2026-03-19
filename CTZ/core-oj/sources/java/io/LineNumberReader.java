package java.io;

public class LineNumberReader extends BufferedReader {
    private static final int maxSkipBufferSize = 8192;
    private int lineNumber;
    private int markedLineNumber;
    private boolean markedSkipLF;
    private char[] skipBuffer;
    private boolean skipLF;

    public LineNumberReader(Reader reader) {
        super(reader);
        this.lineNumber = 0;
        this.skipBuffer = null;
    }

    public LineNumberReader(Reader reader, int i) {
        super(reader, i);
        this.lineNumber = 0;
        this.skipBuffer = null;
    }

    public void setLineNumber(int i) {
        this.lineNumber = i;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public int read() throws IOException {
        synchronized (this.lock) {
            int i = super.read();
            if (this.skipLF) {
                if (i == 10) {
                    i = super.read();
                }
                this.skipLF = false;
            }
            if (i != 10) {
                if (i != 13) {
                    return i;
                }
                this.skipLF = true;
            }
            this.lineNumber++;
            return 10;
        }
    }

    @Override
    public int read(char[] cArr, int i, int i2) throws IOException {
        int i3;
        synchronized (this.lock) {
            i3 = super.read(cArr, i, i2);
            for (int i4 = i; i4 < i + i3; i4++) {
                char c = cArr[i4];
                if (this.skipLF) {
                    this.skipLF = false;
                    if (c != '\n') {
                        if (c == '\n') {
                            this.lineNumber++;
                        } else if (c == '\r') {
                            this.skipLF = true;
                            this.lineNumber++;
                        }
                    }
                }
            }
        }
        return i3;
    }

    @Override
    public String readLine() throws IOException {
        String line;
        synchronized (this.lock) {
            line = super.readLine(this.skipLF);
            this.skipLF = false;
            if (line != null) {
                this.lineNumber++;
            }
        }
        return line;
    }

    @Override
    public long skip(long j) throws IOException {
        long j2;
        int i;
        if (j < 0) {
            throw new IllegalArgumentException("skip() value is negative");
        }
        int iMin = (int) Math.min(j, 8192L);
        synchronized (this.lock) {
            if (this.skipBuffer == null || this.skipBuffer.length < iMin) {
                this.skipBuffer = new char[iMin];
            }
            long j3 = j;
            while (j3 > 0 && (i = read(this.skipBuffer, 0, (int) Math.min(j3, iMin))) != -1) {
                j3 -= (long) i;
            }
            j2 = j - j3;
        }
        return j2;
    }

    @Override
    public void mark(int i) throws IOException {
        synchronized (this.lock) {
            super.mark(i);
            this.markedLineNumber = this.lineNumber;
            this.markedSkipLF = this.skipLF;
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            super.reset();
            this.lineNumber = this.markedLineNumber;
            this.skipLF = this.markedSkipLF;
        }
    }
}
