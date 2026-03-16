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

    public JPEGOutputStream(OutputStream out) {
        super(out);
        this.JNIPointer = 0L;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        this.mQuality = 0;
        this.mFormat = -1;
        this.mValidConfig = false;
        this.mConfigChanged = false;
    }

    public JPEGOutputStream(OutputStream out, int width, int height, int quality, int format) {
        super(out);
        this.JNIPointer = 0L;
        this.mTmpBuffer = new byte[1];
        this.mWidth = 0;
        this.mHeight = 0;
        this.mQuality = 0;
        this.mFormat = -1;
        this.mValidConfig = false;
        this.mConfigChanged = false;
        setConfig(width, height, quality, format);
    }

    public boolean setConfig(int width, int height, int quality, int format) {
        int quality2 = Math.max(Math.min(quality, 100), 1);
        switch (format) {
            case 1:
            case 3:
            case 4:
            case 260:
                if (width > 0 && height > 0) {
                    this.mWidth = width;
                    this.mHeight = height;
                    this.mFormat = format;
                    this.mQuality = quality2;
                    this.mValidConfig = true;
                    this.mConfigChanged = true;
                    break;
                }
                break;
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        cleanup();
        super.close();
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException(String.format(" buffer length %d, offset %d, length %d", Integer.valueOf(buffer.length), Integer.valueOf(offset), Integer.valueOf(length)));
        }
        if (this.mValidConfig) {
            if (this.mConfigChanged) {
                cleanup();
                int flag = setup(this.out, this.mWidth, this.mHeight, this.mFormat, this.mQuality);
                switch (flag) {
                    case -2:
                        throw new IllegalArgumentException("Bad arguments to write");
                    case -1:
                    default:
                        throw new IOException("Error to writing jpeg headers.");
                    case 0:
                        this.mConfigChanged = false;
                        break;
                }
            }
            int returnCode = -1;
            try {
                returnCode = writeInputBytes(buffer, offset, length);
                if (returnCode < 0) {
                    throw new IOException("Error writing jpeg stream");
                }
            } finally {
                if (returnCode < 0) {
                    cleanup();
                }
            }
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override
    public void write(int oneByte) throws IOException {
        this.mTmpBuffer[0] = (byte) oneByte;
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
        System.loadLibrary("jni_jpegstream");
    }
}
