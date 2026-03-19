package javax.obex;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

public final class PrivateInputStream extends InputStream {
    private static final String TAG = "PrivateInputStream";
    private static final boolean V = ObexHelper.VDBG;
    private byte[] mData = new byte[0];
    private int mIndex = 0;
    private boolean mOpen = true;
    private BaseStream mParent;

    public PrivateInputStream(BaseStream baseStream) {
        this.mParent = baseStream;
    }

    @Override
    public synchronized int available() throws IOException {
        ensureOpen();
        return this.mData.length - this.mIndex;
    }

    @Override
    public synchronized int read() throws IOException {
        ensureOpen();
        while (this.mData.length == this.mIndex) {
            if (!this.mParent.continueOperation(true, true)) {
                return -1;
            }
        }
        byte[] bArr = this.mData;
        int i = this.mIndex;
        this.mIndex = i + 1;
        return bArr[i] & 255;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public synchronized int read(byte[] bArr, int i, int i2) throws IOException {
        if (V) {
            Log.d(TAG, "Starting read() length = " + i2);
        }
        if (bArr == null) {
            throw new IOException("buffer is null");
        }
        if ((i | i2) < 0 || i2 > bArr.length - i) {
            throw new ArrayIndexOutOfBoundsException("index outof bound");
        }
        ensureOpen();
        int length = this.mData.length - this.mIndex;
        int i3 = 0;
        while (length <= i2) {
            System.arraycopy(this.mData, this.mIndex, bArr, i, length);
            this.mIndex += length;
            i += length;
            i3 += length;
            i2 -= length;
            if (!this.mParent.continueOperation(true, true)) {
                if (i3 == 0) {
                    i3 = -1;
                }
                return i3;
            }
            length = this.mData.length - this.mIndex;
        }
        if (i2 > 0) {
            System.arraycopy(this.mData, this.mIndex, bArr, i, i2);
            this.mIndex += i2;
            i3 += i2;
        }
        if (V) {
            Log.d(TAG, "Stoping read() result = " + i3);
        }
        return i3;
    }

    public synchronized void writeBytes(byte[] bArr, int i) {
        int length = (bArr.length - i) + (this.mData.length - this.mIndex);
        byte[] bArr2 = new byte[length];
        if (V) {
            Log.d(TAG, "writeBytes length = " + length);
        }
        System.arraycopy(this.mData, this.mIndex, bArr2, 0, this.mData.length - this.mIndex);
        System.arraycopy(bArr, i, bArr2, this.mData.length - this.mIndex, bArr.length - i);
        this.mData = bArr2;
        this.mIndex = 0;
        notifyAll();
    }

    private void ensureOpen() throws IOException {
        this.mParent.ensureOpen();
        if (!this.mOpen) {
            throw new IOException("Input stream is closed");
        }
    }

    @Override
    public void close() throws IOException {
        this.mOpen = false;
        this.mParent.streamClosed(true);
    }
}
