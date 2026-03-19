package android.graphics;

import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BitmapRegionDecoder {
    private long mNativeBitmapRegionDecoder;
    private final Object mNativeLock = new Object();
    private boolean mRecycled = false;

    private static native void nativeClean(long j);

    private static native Bitmap nativeDecodeRegion(long j, int i, int i2, int i3, int i4, BitmapFactory.Options options);

    private static native int nativeGetHeight(long j);

    private static native int nativeGetWidth(long j);

    private static native BitmapRegionDecoder nativeNewInstance(long j, boolean z);

    private static native BitmapRegionDecoder nativeNewInstance(FileDescriptor fileDescriptor, boolean z);

    private static native BitmapRegionDecoder nativeNewInstance(InputStream inputStream, byte[] bArr, boolean z);

    private static native BitmapRegionDecoder nativeNewInstance(byte[] bArr, int i, int i2, boolean z);

    public static BitmapRegionDecoder newInstance(byte[] bArr, int i, int i2, boolean z) throws IOException {
        if ((i | i2) < 0 || bArr.length < i + i2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return nativeNewInstance(bArr, i, i2, z);
    }

    public static BitmapRegionDecoder newInstance(FileDescriptor fileDescriptor, boolean z) throws IOException {
        return nativeNewInstance(fileDescriptor, z);
    }

    public static BitmapRegionDecoder newInstance(InputStream inputStream, boolean z) throws IOException {
        if (inputStream instanceof AssetManager.AssetInputStream) {
            return nativeNewInstance(((AssetManager.AssetInputStream) inputStream).getNativeAsset(), z);
        }
        return nativeNewInstance(inputStream, new byte[16384], z);
    }

    public static BitmapRegionDecoder newInstance(String str, boolean z) throws Throwable {
        FileInputStream fileInputStream = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(str);
            try {
                BitmapRegionDecoder bitmapRegionDecoderNewInstance = newInstance(fileInputStream2, z);
                try {
                    fileInputStream2.close();
                } catch (IOException e) {
                }
                return bitmapRegionDecoderNewInstance;
            } catch (Throwable th) {
                th = th;
                fileInputStream = fileInputStream2;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private BitmapRegionDecoder(long j) {
        this.mNativeBitmapRegionDecoder = j;
    }

    public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) {
        BitmapFactory.Options.validate(options);
        checkRecycled("decodeRegion called on recycled region decoder");
        if (rect.right <= 0 || rect.bottom <= 0 || rect.left >= getWidth() || rect.top >= getHeight()) {
            throw new IllegalArgumentException("rectangle is outside the image");
        }
        return nativeDecodeRegion(this.mNativeBitmapRegionDecoder, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, options);
    }

    public int getWidth() {
        int iNativeGetWidth;
        synchronized (this.mNativeLock) {
            checkRecycled("getWidth called on recycled region decoder");
            iNativeGetWidth = nativeGetWidth(this.mNativeBitmapRegionDecoder);
        }
        return iNativeGetWidth;
    }

    public int getHeight() {
        int iNativeGetHeight;
        synchronized (this.mNativeLock) {
            checkRecycled("getHeight called on recycled region decoder");
            iNativeGetHeight = nativeGetHeight(this.mNativeBitmapRegionDecoder);
        }
        return iNativeGetHeight;
    }

    public void recycle() {
        synchronized (this.mNativeLock) {
            if (!this.mRecycled) {
                nativeClean(this.mNativeBitmapRegionDecoder);
                this.mRecycled = true;
            }
        }
    }

    public final boolean isRecycled() {
        return this.mRecycled;
    }

    private void checkRecycled(String str) {
        if (this.mRecycled) {
            throw new IllegalStateException(str);
        }
    }

    protected void finalize() throws Throwable {
        try {
            recycle();
        } finally {
            super.finalize();
        }
    }
}
