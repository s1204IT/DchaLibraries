package java.io;

import java.util.Arrays;
import libcore.io.Streams;

@Deprecated
public class LineNumberInputStream extends FilterInputStream {
    private int lastChar;
    private int lineNumber;
    private int markedLastChar;
    private int markedLineNumber;

    public LineNumberInputStream(InputStream in) {
        super(in);
        this.markedLineNumber = -1;
        this.lastChar = -1;
    }

    @Override
    public int available() throws IOException {
        return (this.lastChar == -1 ? 0 : 1) + (this.in.available() / 2);
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public void mark(int readlimit) {
        this.in.mark(readlimit);
        this.markedLineNumber = this.lineNumber;
        this.markedLastChar = this.lastChar;
    }

    @Override
    public int read() throws IOException {
        int currentChar = this.lastChar;
        if (currentChar == -1) {
            currentChar = this.in.read();
        } else {
            this.lastChar = -1;
        }
        switch (currentChar) {
            case 10:
                this.lineNumber++;
                return currentChar;
            case 11:
            case 12:
            default:
                return currentChar;
            case 13:
                currentChar = 10;
                this.lastChar = this.in.read();
                if (this.lastChar == 10) {
                    this.lastChar = -1;
                }
                this.lineNumber++;
                return currentChar;
        }
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        for (int i = 0; i < byteCount; i++) {
            try {
                int currentChar = read();
                if (currentChar == -1) {
                    if (i == 0) {
                        return -1;
                    }
                    return i;
                }
                buffer[byteOffset + i] = (byte) currentChar;
            } catch (IOException e) {
                if (i == 0) {
                    throw e;
                }
                return i;
            }
        }
        return byteCount;
    }

    @Override
    public void reset() throws IOException {
        this.in.reset();
        this.lineNumber = this.markedLineNumber;
        this.lastChar = this.markedLastChar;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return Streams.skipByReading(this, byteCount);
    }
}
