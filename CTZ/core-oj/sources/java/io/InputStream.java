package java.io;

public abstract class InputStream implements Closeable {
    private static final int MAX_SKIP_BUFFER_SIZE = 2048;

    public abstract int read() throws IOException;

    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        int i3 = read();
        if (i3 == -1) {
            return -1;
        }
        bArr[i] = (byte) i3;
        int i4 = 1;
        while (i4 < i2) {
            try {
                int i5 = read();
                if (i5 == -1) {
                    break;
                }
                bArr[i + i4] = (byte) i5;
                i4++;
            } catch (IOException e) {
            }
        }
        return i4;
    }

    public long skip(long j) throws IOException {
        int i;
        if (j <= 0) {
            return 0L;
        }
        int iMin = (int) Math.min(2048L, j);
        byte[] bArr = new byte[iMin];
        long j2 = j;
        while (j2 > 0 && (i = read(bArr, 0, (int) Math.min(iMin, j2))) >= 0) {
            j2 -= (long) i;
        }
        return j - j2;
    }

    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }

    public synchronized void mark(int i) {
    }

    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }
}
