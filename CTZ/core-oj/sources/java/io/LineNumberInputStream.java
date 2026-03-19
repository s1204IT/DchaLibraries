package java.io;

@Deprecated
public class LineNumberInputStream extends FilterInputStream {
    int lineNumber;
    int markLineNumber;
    int markPushBack;
    int pushBack;

    public LineNumberInputStream(InputStream inputStream) {
        super(inputStream);
        this.pushBack = -1;
        this.markPushBack = -1;
    }

    @Override
    public int read() throws IOException {
        int i = this.pushBack;
        if (i != -1) {
            this.pushBack = -1;
        } else {
            i = this.in.read();
        }
        if (i != 10) {
            if (i == 13) {
                this.pushBack = this.in.read();
                if (this.pushBack == 10) {
                    this.pushBack = -1;
                }
            } else {
                return i;
            }
        }
        this.lineNumber++;
        return 10;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i > bArr.length || i2 < 0 || (i3 = i + i2) > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int i4 = read();
        if (i4 == -1) {
            return -1;
        }
        bArr[i] = (byte) i4;
        int i5 = 1;
        while (i5 < i2) {
            try {
                int i6 = read();
                if (i6 == -1) {
                    break;
                }
                if (bArr != null) {
                    bArr[i + i5] = (byte) i6;
                }
                i5++;
            } catch (IOException e) {
            }
        }
        return i5;
    }

    @Override
    public long skip(long j) throws IOException {
        int i;
        if (j <= 0) {
            return 0L;
        }
        byte[] bArr = new byte[2048];
        long j2 = j;
        while (j2 > 0 && (i = read(bArr, 0, (int) Math.min(2048, j2))) >= 0) {
            j2 -= (long) i;
        }
        return j - j2;
    }

    public void setLineNumber(int i) {
        this.lineNumber = i;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public int available() throws IOException {
        return this.pushBack == -1 ? super.available() / 2 : (super.available() / 2) + 1;
    }

    @Override
    public void mark(int i) {
        this.markLineNumber = this.lineNumber;
        this.markPushBack = this.pushBack;
        this.in.mark(i);
    }

    @Override
    public void reset() throws IOException {
        this.lineNumber = this.markLineNumber;
        this.pushBack = this.markPushBack;
        this.in.reset();
    }
}
