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

    public static BitmapRegionDecoder newInstance(byte[] data, int offset, int length, boolean isShareable) throws IOException {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return nativeNewInstance(data, offset, length, isShareable);
    }

    public static BitmapRegionDecoder newInstance(FileDescriptor fd, boolean isShareable) throws IOException {
        return nativeNewInstance(fd, isShareable);
    }

    public static BitmapRegionDecoder newInstance(InputStream is, boolean isShareable) throws IOException {
        if (is instanceof AssetManager.AssetInputStream) {
            return nativeNewInstance(((AssetManager.AssetInputStream) is).getNativeAsset(), isShareable);
        }
        byte[] tempStorage = new byte[16384];
        return nativeNewInstance(is, tempStorage, isShareable);
    }

    public static BitmapRegionDecoder newInstance(String pathName, boolean isShareable) throws Throwable {
        InputStream stream;
        InputStream stream2 = null;
        try {
            stream = new FileInputStream(pathName);
        } catch (Throwable th) {
            th = th;
        }
        try {
            BitmapRegionDecoder decoder = newInstance(stream, isShareable);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            return decoder;
        } catch (Throwable th2) {
            th = th2;
            stream2 = stream;
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }

    private BitmapRegionDecoder(long decoder) {
        this.mNativeBitmapRegionDecoder = decoder;
    }

    public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) {
        Bitmap bitmapNativeDecodeRegion;
        synchronized (this.mNativeLock) {
            checkRecycled("decodeRegion called on recycled region decoder");
            if (rect.right <= 0 || rect.bottom <= 0 || rect.left >= getWidth() || rect.top >= getHeight()) {
                throw new IllegalArgumentException("rectangle is outside the image");
            }
            bitmapNativeDecodeRegion = nativeDecodeRegion(this.mNativeBitmapRegionDecoder, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, options);
        }
        return bitmapNativeDecodeRegion;
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

    private void checkRecycled(String errorMessage) {
        if (this.mRecycled) {
            throw new IllegalStateException(errorMessage);
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
