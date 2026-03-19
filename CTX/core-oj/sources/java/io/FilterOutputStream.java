package java.io;

public class FilterOutputStream extends OutputStream {
    protected OutputStream out;

    public FilterOutputStream(OutputStream outputStream) {
        this.out = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        this.out.write(i);
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        write(bArr, 0, bArr.length);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        int i3 = i2 + i;
        if ((i | i2 | (bArr.length - i3) | i3) < 0) {
            throw new IndexOutOfBoundsException();
        }
        for (int i4 = 0; i4 < i2; i4++) {
            write(bArr[i + i4]);
        }
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws Throwable {
        Throwable th;
        OutputStream outputStream = this.out;
        try {
            flush();
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Throwable th2) {
            th = th2;
            th = null;
            if (outputStream != null) {
            }
            throw th;
        }
    }
}
