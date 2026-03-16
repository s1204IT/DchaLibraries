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

    public JPEGInputStream(InputStream in) {
        super(in);
        this.JNIPointer = 0L;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        this.mFormat = -1;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
    }

    public JPEGInputStream(InputStream in, int format) {
        super(in);
        this.JNIPointer = 0L;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        this.mFormat = -1;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        setConfig(format);
    }

    public boolean setConfig(int format) {
        switch (format) {
            case 1:
            case 3:
            case 4:
            case 260:
                this.mFormat = format;
                this.mValidConfig = true;
                this.mConfigChanged = true;
                return true;
            default:
                return false;
        }
    }

    public Point getDimensions() throws IOException {
        if (!this.mValidConfig) {
            return null;
        }
        applyConfigChange();
        return new Point(this.mWidth, this.mHeight);
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
    public synchronized void mark(int readlimit) {
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
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        if (offset < 0 || count < 0 || offset + count > buffer.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(" buffer length %d, offset %d, length %d", Integer.valueOf(buffer.length), Integer.valueOf(offset), Integer.valueOf(count)));
        }
        if (!this.mValidConfig) {
            return 0;
        }
        applyConfigChange();
        int flag = -1;
        try {
            flag = readDecodedBytes(buffer, offset, count);
            if (flag < 0) {
                switch (flag) {
                    case -4:
                        return -1;
                    default:
                        throw new IOException("Error reading jpeg stream");
                }
            }
            return flag;
        } finally {
            if (flag < 0) {
                cleanup();
            }
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Reset not supported.");
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (byteCount <= 0) {
            return 0L;
        }
        int flag = skipDecodedBytes((int) (2147483647L & byteCount));
        if (flag < 0) {
            switch (flag) {
                case -4:
                    return 0L;
                default:
                    throw new IOException("Error skipping jpeg stream");
            }
        }
        return flag;
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
            Point dimens = new Point(0, 0);
            int flag = setup(dimens, this.in, this.mFormat);
            switch (flag) {
                case -2:
                    throw new IllegalArgumentException("Bad arguments to read");
                case -1:
                default:
                    throw new IOException("Error to reading jpeg headers.");
                case 0:
                    this.mWidth = dimens.x;
                    this.mHeight = dimens.y;
                    this.mConfigChanged = false;
                    return;
            }
        }
    }

    static {
        System.loadLibrary("jni_jpegstream");
    }
}
