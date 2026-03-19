package android.media;

import java.io.IOException;
import java.io.InputStream;

public final class ResampleInputStream extends InputStream {
    private static final String TAG = "ResampleInputStream";
    private static final int mFirLength = 29;
    private byte[] mBuf;
    private int mBufCount;
    private InputStream mInputStream;
    private final byte[] mOneByte = new byte[1];
    private final int mRateIn;
    private final int mRateOut;

    private static native void fir21(byte[] bArr, int i, byte[] bArr2, int i2, int i3);

    static {
        System.loadLibrary("media_jni");
    }

    public ResampleInputStream(InputStream inputStream, int i, int i2) {
        if (i != i2 * 2) {
            throw new IllegalArgumentException("only support 2:1 at the moment");
        }
        this.mInputStream = inputStream;
        this.mRateIn = 2;
        this.mRateOut = 1;
    }

    @Override
    public int read() throws IOException {
        if (read(this.mOneByte, 0, 1) == 1) {
            return 255 & this.mOneByte[0];
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.mInputStream == null) {
            throw new IllegalStateException("not open");
        }
        int i3 = i2 / 2;
        int i4 = (((this.mRateIn * i3) / this.mRateOut) + 29) * 2;
        if (this.mBuf == null) {
            this.mBuf = new byte[i4];
        } else if (i4 > this.mBuf.length) {
            byte[] bArr2 = new byte[i4];
            System.arraycopy(this.mBuf, 0, bArr2, 0, this.mBufCount);
            this.mBuf = bArr2;
        }
        while (true) {
            int i5 = ((((this.mBufCount / 2) - 29) * this.mRateOut) / this.mRateIn) * 2;
            if (i5 > 0) {
                if (i5 >= i2) {
                    i5 = i3 * 2;
                }
                fir21(this.mBuf, 0, bArr, i, i5 / 2);
                int i6 = (this.mRateIn * i5) / this.mRateOut;
                this.mBufCount -= i6;
                if (this.mBufCount > 0) {
                    System.arraycopy(this.mBuf, i6, this.mBuf, 0, this.mBufCount);
                }
                return i5;
            }
            int i7 = this.mInputStream.read(this.mBuf, this.mBufCount, this.mBuf.length - this.mBufCount);
            if (i7 == -1) {
                return -1;
            }
            this.mBufCount += i7;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.mInputStream != null) {
                this.mInputStream.close();
            }
        } finally {
            this.mInputStream = null;
        }
    }

    protected void finalize() throws Throwable {
        if (this.mInputStream != null) {
            close();
            throw new IllegalStateException("someone forgot to close ResampleInputStream");
        }
    }
}
