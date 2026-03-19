package com.android.gallery3d.jpegstream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JPEGOutputStream extends FilterOutputStream {
    private long JNIPointer;
    private boolean mConfigChanged;
    private int mFormat;
    private int mHeight;
    private int mQuality;
    private byte[] mTmpBuffer;
    private boolean mValidConfig;
    private int mWidth;

    private native void cleanup();

    private native int setup(OutputStream outputStream, int i, int i2, int i3, int i4);

    private native int writeInputBytes(byte[] bArr, int i, int i2);

    public JPEGOutputStream(OutputStream outputStream) {
        super(outputStream);
        this.JNIPointer = 0L;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        this.mQuality = 0;
        this.mFormat = -1;
        this.mValidConfig = false;
        this.mConfigChanged = false;
    }

    public JPEGOutputStream(OutputStream outputStream, int i, int i2, int i3, int i4) {
        super(outputStream);
        this.JNIPointer = 0L;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        this.mQuality = 0;
        this.mFormat = -1;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        setConfig(i, i2, i3, i4);
    }

    public boolean setConfig(int i, int i2, int i3, int i4) {
        int iMax = Math.max(Math.min(i3, 100), 1);
        if (i4 != 1 && i4 != 260) {
            switch (i4) {
                case 3:
                case 4:
                    break;
                default:
                    return false;
            }
        }
        if (i <= 0 || i2 <= 0) {
            return false;
        }
        this.mWidth = i;
        this.mHeight = i2;
        this.mFormat = i4;
        this.mQuality = iMax;
        this.mValidConfig = true;
        this.mConfigChanged = true;
        return this.mValidConfig;
    }

    @Override
    public void close() throws IOException {
        cleanup();
        super.close();
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (i < 0 || i2 < 0 || i + i2 > bArr.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(" buffer length %d, offset %d, length %d", Integer.valueOf(bArr.length), Integer.valueOf(i), Integer.valueOf(i2)));
        }
        if (!this.mValidConfig) {
            return;
        }
        if (this.mConfigChanged) {
            cleanup();
            int upVar = setup(((FilterOutputStream) this).out, this.mWidth, this.mHeight, this.mFormat, this.mQuality);
            if (upVar == -2) {
                throw new IllegalArgumentException("Bad arguments to write");
            }
            if (upVar != 0) {
                throw new IOException("Error to writing jpeg headers.");
            }
            this.mConfigChanged = false;
        }
        try {
            int iWriteInputBytes = writeInputBytes(bArr, i, i2);
            if (iWriteInputBytes < 0) {
            }
            if (iWriteInputBytes < 0) {
                throw new IOException("Error writing jpeg stream");
            }
        } finally {
            cleanup();
        }
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        write(bArr, 0, bArr.length);
    }

    @Override
    public void write(int i) throws IOException {
        this.mTmpBuffer[0] = (byte) i;
        write(this.mTmpBuffer);
    }

    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }

    static {
        System.loadLibrary("jni_jpegstream_mtk");
    }
}
