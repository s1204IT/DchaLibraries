package javax.obex;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class PrivateOutputStream extends OutputStream {
    private static final String TAG = "PrivateOutputStream";
    private static final boolean V = ObexHelper.VDBG;
    private int mMaxPacketSize;
    private BaseStream mParent;
    private ByteArrayOutputStream mArray = new ByteArrayOutputStream();
    private boolean mOpen = true;

    public PrivateOutputStream(BaseStream baseStream, int i) {
        this.mParent = baseStream;
        this.mMaxPacketSize = i;
    }

    public int size() {
        return this.mArray.size();
    }

    @Override
    public synchronized void write(int i) throws IOException {
        ensureOpen();
        this.mParent.ensureNotDone();
        this.mArray.write(i);
        if (this.mArray.size() == this.mMaxPacketSize) {
            this.mParent.continueOperation(true, false);
        }
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        write(bArr, 0, bArr.length);
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
        if (V) {
            Log.d(TAG, "write buffer = " + i2);
        }
        if (bArr == null) {
            throw new IOException("buffer is null");
        }
        if ((i | i2) < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException("index outof bound");
        }
        ensureOpen();
        this.mParent.ensureNotDone();
        while (this.mArray.size() + i2 >= this.mMaxPacketSize) {
            int size = this.mMaxPacketSize - this.mArray.size();
            this.mArray.write(bArr, i, size);
            i += size;
            i2 -= size;
            this.mParent.continueOperation(true, false);
        }
        if (i2 > 0) {
            this.mArray.write(bArr, i, i2);
        }
    }

    public synchronized byte[] readBytes(int i) {
        if (this.mArray.size() > 0) {
            byte[] byteArray = this.mArray.toByteArray();
            this.mArray.reset();
            byte[] bArr = new byte[i];
            System.arraycopy(byteArray, 0, bArr, 0, i);
            if (byteArray.length != i) {
                this.mArray.write(byteArray, i, byteArray.length - i);
            }
            return bArr;
        }
        return null;
    }

    private void ensureOpen() throws IOException {
        this.mParent.ensureOpen();
        if (!this.mOpen) {
            throw new IOException("Output stream is closed");
        }
    }

    @Override
    public void close() throws IOException {
        this.mOpen = false;
        this.mParent.streamClosed(false);
    }

    public boolean isClosed() {
        return !this.mOpen;
    }
}
