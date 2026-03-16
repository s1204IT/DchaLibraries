package android.media;

import android.os.BatteryStats;
import java.io.IOException;
import java.io.InputStream;

public final class AmrInputStream extends InputStream {
    private static final int SAMPLES_PER_FRAME = 160;
    private static final String TAG = "AmrInputStream";
    private InputStream mInputStream;
    private final byte[] mBuf = new byte[320];
    private int mBufIn = 0;
    private int mBufOut = 0;
    private byte[] mOneByte = new byte[1];
    private long mGae = GsmAmrEncoderNew();

    private static native void GsmAmrEncoderCleanup(long j);

    private static native void GsmAmrEncoderDelete(long j);

    private static native int GsmAmrEncoderEncode(long j, byte[] bArr, int i, byte[] bArr2, int i2) throws IOException;

    private static native void GsmAmrEncoderInitialize(long j);

    private static native long GsmAmrEncoderNew();

    static {
        System.loadLibrary("media_jni");
    }

    public AmrInputStream(InputStream inputStream) {
        this.mInputStream = inputStream;
        GsmAmrEncoderInitialize(this.mGae);
    }

    @Override
    public int read() throws IOException {
        int rtn = read(this.mOneByte, 0, 1);
        if (rtn == 1) {
            return this.mOneByte[0] & BatteryStats.HistoryItem.CMD_NULL;
        }
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (this.mGae == 0) {
            throw new IllegalStateException("not open");
        }
        if (this.mBufOut >= this.mBufIn) {
            this.mBufOut = 0;
            this.mBufIn = 0;
            int i = 0;
            while (i < 320) {
                int n = this.mInputStream.read(this.mBuf, i, 320 - i);
                if (n == -1) {
                    return -1;
                }
                i += n;
            }
            this.mBufIn = GsmAmrEncoderEncode(this.mGae, this.mBuf, 0, this.mBuf, 0);
        }
        if (length > this.mBufIn - this.mBufOut) {
            length = this.mBufIn - this.mBufOut;
        }
        System.arraycopy(this.mBuf, this.mBufOut, b, offset, length);
        this.mBufOut += length;
        return length;
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.mInputStream != null) {
                this.mInputStream.close();
            }
            this.mInputStream = null;
            try {
                if (this.mGae != 0) {
                    GsmAmrEncoderCleanup(this.mGae);
                }
                try {
                    if (this.mGae != 0) {
                        GsmAmrEncoderDelete(this.mGae);
                    }
                } finally {
                }
            } catch (Throwable th) {
                try {
                    if (this.mGae != 0) {
                        GsmAmrEncoderDelete(this.mGae);
                    }
                    throw th;
                } finally {
                }
            }
        } catch (Throwable th2) {
            this.mInputStream = null;
            try {
                if (this.mGae != 0) {
                    GsmAmrEncoderCleanup(this.mGae);
                }
                try {
                    if (this.mGae != 0) {
                        GsmAmrEncoderDelete(this.mGae);
                    }
                    throw th2;
                } finally {
                }
            } catch (Throwable th3) {
                try {
                    if (this.mGae != 0) {
                        GsmAmrEncoderDelete(this.mGae);
                    }
                    throw th3;
                } finally {
                }
            }
        }
    }

    protected void finalize() throws Throwable {
        if (this.mGae != 0) {
            close();
            throw new IllegalStateException("someone forgot to close AmrInputStream");
        }
    }
}
