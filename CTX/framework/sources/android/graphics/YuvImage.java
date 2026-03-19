package android.graphics;

import java.io.OutputStream;

public class YuvImage {
    private static final int WORKING_COMPRESS_STORAGE = 4096;
    private byte[] mData;
    private int mFormat;
    private int mHeight;
    private int[] mStrides;
    private int mWidth;

    private static native boolean nativeCompressToJpeg(byte[] bArr, int i, int i2, int i3, int[] iArr, int[] iArr2, int i4, OutputStream outputStream, byte[] bArr2);

    public YuvImage(byte[] bArr, int i, int i2, int i3, int[] iArr) {
        if (i != 17 && i != 20) {
            throw new IllegalArgumentException("only support ImageFormat.NV21 and ImageFormat.YUY2 for now");
        }
        if (i2 <= 0 || i3 <= 0) {
            throw new IllegalArgumentException("width and height must large than 0");
        }
        if (bArr == null) {
            throw new IllegalArgumentException("yuv cannot be null");
        }
        if (iArr == null) {
            this.mStrides = calculateStrides(i2, i);
        } else {
            this.mStrides = iArr;
        }
        this.mData = bArr;
        this.mFormat = i;
        this.mWidth = i2;
        this.mHeight = i3;
    }

    public boolean compressToJpeg(Rect rect, int i, OutputStream outputStream) {
        if (!new Rect(0, 0, this.mWidth, this.mHeight).contains(rect)) {
            throw new IllegalArgumentException("rectangle is not inside the image");
        }
        if (i < 0 || i > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("stream cannot be null");
        }
        adjustRectangle(rect);
        return nativeCompressToJpeg(this.mData, this.mFormat, rect.width(), rect.height(), calculateOffsets(rect.left, rect.top), this.mStrides, i, outputStream, new byte[4096]);
    }

    public byte[] getYuvData() {
        return this.mData;
    }

    public int getYuvFormat() {
        return this.mFormat;
    }

    public int[] getStrides() {
        return this.mStrides;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    int[] calculateOffsets(int i, int i2) {
        if (this.mFormat == 17) {
            return new int[]{(this.mStrides[0] * i2) + i, (this.mHeight * this.mStrides[0]) + ((i2 / 2) * this.mStrides[1]) + ((i / 2) * 2)};
        }
        if (this.mFormat == 20) {
            return new int[]{(i2 * this.mStrides[0]) + ((i / 2) * 4)};
        }
        return null;
    }

    private int[] calculateStrides(int i, int i2) {
        if (i2 == 17) {
            return new int[]{i, i};
        }
        if (i2 == 20) {
            return new int[]{i * 2};
        }
        return null;
    }

    private void adjustRectangle(Rect rect) {
        int iWidth = rect.width();
        int iHeight = rect.height();
        if (this.mFormat == 17) {
            iWidth &= -2;
            rect.left &= -2;
            rect.top &= -2;
            rect.right = rect.left + iWidth;
            rect.bottom = rect.top + (iHeight & (-2));
        }
        if (this.mFormat == 20) {
            rect.left &= -2;
            rect.right = rect.left + (iWidth & (-2));
        }
    }
}
