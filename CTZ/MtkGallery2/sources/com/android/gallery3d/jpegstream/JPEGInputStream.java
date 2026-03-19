package com.android.gallery3d.jpegstream;

import android.graphics.Point;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JPEGInputStream extends FilterInputStream {
    private long JNIPointer;
    private boolean mConfigChanged;
    private int mFormat;
    private int mHeight;
    private byte[] mTmpBuffer;
    private boolean mValidConfig;
    private int mWidth;

    private native void cleanup();

    private native int readDecodedBytes(byte[] bArr, int i, int i2);

    private native int setup(Point point, InputStream inputStream, int i);

    private native int skipDecodedBytes(int i);

    public JPEGInputStream(InputStream inputStream) {
        super(inputStream);
        this.JNIPointer = 0L;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        this.mFormat = -1;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
    }

    public JPEGInputStream(InputStream inputStream, int i) {
        super(inputStream);
        this.JNIPointer = 0L;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        this.mFormat = -1;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        setConfig(i);
    }

    public boolean setConfig(int i) {
        if (i != 1 && i != 260) {
            switch (i) {
                case 3:
                case 4:
                    break;
                default:
                    return false;
            }
        }
        this.mFormat = i;
        this.mValidConfig = true;
        this.mConfigChanged = true;
        return true;
    }

    public Point getDimensions() throws IOException {
        if (this.mValidConfig) {
            applyConfigChange();
            return new Point(this.mWidth, this.mHeight);
        }
        return null;
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public void close() throws IOException {
        cleanup();
        super.close();
    }

    @Override
    public synchronized void mark(int i) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        read(this.mTmpBuffer, 0, 1);
        return this.mTmpBuffer[0] & 255;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (i < 0 || i2 < 0 || i + i2 > bArr.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(" buffer length %d, offset %d, length %d", Integer.valueOf(bArr.length), Integer.valueOf(i), Integer.valueOf(i2)));
        }
        if (!this.mValidConfig) {
            return 0;
        }
        applyConfigChange();
        try {
            int decodedBytes = readDecodedBytes(bArr, i, i2);
            if (decodedBytes < 0) {
            }
            if (decodedBytes < 0) {
                if (decodedBytes == -4) {
                    return -1;
                }
                throw new IOException("Error reading jpeg stream");
            }
            return decodedBytes;
        } finally {
            cleanup();
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Reset not supported.");
    }

    @Override
    public long skip(long j) throws IOException {
        if (j <= 0) {
            return 0L;
        }
        int iSkipDecodedBytes = skipDecodedBytes((int) (j & 2147483647L));
        if (iSkipDecodedBytes < 0) {
            if (iSkipDecodedBytes == -4) {
                return 0L;
            }
            throw new IOException("Error skipping jpeg stream");
        }
        return iSkipDecodedBytes;
    }

    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }

    private void applyConfigChange() throws IOException {
        if (this.mConfigChanged) {
            cleanup();
            Point point = new Point(0, 0);
            int upVar = setup(point, this.in, this.mFormat);
            if (upVar == -2) {
                throw new IllegalArgumentException("Bad arguments to read");
            }
            if (upVar != 0) {
                throw new IOException("Error to reading jpeg headers.");
            }
            this.mWidth = point.x;
            this.mHeight = point.y;
            this.mConfigChanged = false;
        }
    }

    static {
        System.loadLibrary("jni_jpegstream_mtk");
    }
}
