package java.io;

@Deprecated
public class StringBufferInputStream extends InputStream {
    protected String buffer;
    protected int count;
    protected int pos;

    public StringBufferInputStream(String str) {
        this.buffer = str;
        this.count = str.length();
    }

    @Override
    public synchronized int read() {
        int iCharAt;
        if (this.pos < this.count) {
            String str = this.buffer;
            int i = this.pos;
            this.pos = i + 1;
            iCharAt = str.charAt(i) & 255;
        } else {
            iCharAt = -1;
        }
        return iCharAt;
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) {
        int i3;
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i > bArr.length || i2 < 0 || (i3 = i + i2) > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (this.pos >= this.count) {
            return -1;
        }
        int i4 = this.count - this.pos;
        if (i2 > i4) {
            i2 = i4;
        }
        if (i2 <= 0) {
            return 0;
        }
        String str = this.buffer;
        int i5 = i;
        int i6 = i2;
        while (true) {
            i6--;
            if (i6 < 0) {
                return i2;
            }
            int i7 = i5 + 1;
            int i8 = this.pos;
            this.pos = i8 + 1;
            bArr[i5] = (byte) str.charAt(i8);
            i5 = i7;
        }
    }

    @Override
    public synchronized long skip(long j) {
        if (j < 0) {
            return 0L;
        }
        if (j > this.count - this.pos) {
            j = this.count - this.pos;
        }
        this.pos = (int) (((long) this.pos) + j);
        return j;
    }

    @Override
    public synchronized int available() {
        return this.count - this.pos;
    }

    @Override
    public synchronized void reset() {
        this.pos = 0;
    }
}
