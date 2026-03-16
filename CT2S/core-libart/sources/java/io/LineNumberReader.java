package java.io;

public class LineNumberReader extends BufferedReader {
    private boolean lastWasCR;
    private int lineNumber;
    private boolean markedLastWasCR;
    private int markedLineNumber;

    public LineNumberReader(Reader in) {
        super(in);
        this.markedLineNumber = -1;
    }

    public LineNumberReader(Reader in, int size) {
        super(in, size);
        this.markedLineNumber = -1;
    }

    public int getLineNumber() {
        int i;
        synchronized (this.lock) {
            i = this.lineNumber;
        }
        return i;
    }

    @Override
    public void mark(int readlimit) throws IOException {
        synchronized (this.lock) {
            super.mark(readlimit);
            this.markedLineNumber = this.lineNumber;
            this.markedLastWasCR = this.lastWasCR;
        }
    }

    @Override
    public int read() throws IOException {
        int ch;
        synchronized (this.lock) {
            ch = super.read();
            if (ch == 10 && this.lastWasCR) {
                ch = super.read();
            }
            this.lastWasCR = false;
            switch (ch) {
                case 10:
                    this.lineNumber++;
                    break;
                case 13:
                    ch = 10;
                    this.lastWasCR = true;
                    this.lineNumber++;
                    break;
            }
        }
        return ch;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        synchronized (this.lock) {
            int read = super.read(buffer, offset, count);
            if (read == -1) {
                return -1;
            }
            for (int i = 0; i < read; i++) {
                char ch = buffer[offset + i];
                if (ch == '\r') {
                    this.lineNumber++;
                    this.lastWasCR = true;
                } else if (ch == '\n') {
                    if (!this.lastWasCR) {
                        this.lineNumber++;
                    }
                    this.lastWasCR = false;
                } else {
                    this.lastWasCR = false;
                }
            }
            return read;
        }
    }

    @Override
    public String readLine() throws IOException {
        String result;
        synchronized (this.lock) {
            if (this.lastWasCR) {
                chompNewline();
                this.lastWasCR = false;
            }
            result = super.readLine();
            if (result != null) {
                this.lineNumber++;
            }
        }
        return result;
    }

    @Override
    public void reset() throws IOException {
        synchronized (this.lock) {
            super.reset();
            this.lineNumber = this.markedLineNumber;
            this.lastWasCR = this.markedLastWasCR;
        }
    }

    public void setLineNumber(int lineNumber) {
        synchronized (this.lock) {
            this.lineNumber = lineNumber;
        }
    }

    @Override
    public long skip(long charCount) throws IOException {
        if (charCount < 0) {
            throw new IllegalArgumentException("charCount < 0: " + charCount);
        }
        synchronized (this.lock) {
            int i = 0;
            while (true) {
                if (i >= charCount) {
                    break;
                }
                if (read() != -1) {
                    i++;
                } else {
                    charCount = i;
                    break;
                }
            }
        }
        return charCount;
    }
}
