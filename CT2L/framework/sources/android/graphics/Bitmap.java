package android.graphics;

import android.graphics.NinePatch;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import android.util.DisplayMetrics;
import dalvik.system.VMRuntime;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class Bitmap implements Parcelable {
    public static final int DENSITY_NONE = 0;
    private static final int WORKING_COMPRESS_STORAGE = 4096;
    private static volatile Matrix sScaleMatrix;
    private byte[] mBuffer;
    int mDensity;
    private final BitmapFinalizer mFinalizer;
    private int mHeight;
    private final boolean mIsMutable;
    public final long mNativeBitmap;
    private byte[] mNinePatchChunk;
    private NinePatch.InsetStruct mNinePatchInsets;
    private boolean mRecycled;
    private boolean mRequestPremultiplied;
    private int mWidth;
    private static volatile int sDefaultDensity = -1;
    public static final Parcelable.Creator<Bitmap> CREATOR = new Parcelable.Creator<Bitmap>() {
        @Override
        public Bitmap createFromParcel(Parcel p) {
            Bitmap bm = Bitmap.nativeCreateFromParcel(p);
            if (bm == null) {
                throw new RuntimeException("Failed to unparcel Bitmap");
            }
            return bm;
        }

        @Override
        public Bitmap[] newArray(int size) {
            return new Bitmap[size];
        }
    };

    private static native boolean nativeCompress(long j, int i, int i2, OutputStream outputStream, byte[] bArr);

    private static native int nativeConfig(long j);

    private static native Bitmap nativeCopy(long j, int i, boolean z);

    private static native void nativeCopyPixelsFromBuffer(long j, Buffer buffer);

    private static native void nativeCopyPixelsToBuffer(long j, Buffer buffer);

    private static native Bitmap nativeCreate(int[] iArr, int i, int i2, int i3, int i4, int i5, boolean z);

    private static native Bitmap nativeCreateFromParcel(Parcel parcel);

    private static native void nativeDestructor(long j);

    private static native void nativeErase(long j, int i);

    private static native Bitmap nativeExtractAlpha(long j, long j2, int[] iArr);

    private static native int nativeGenerationId(long j);

    private static native int nativeGetPixel(long j, int i, int i2);

    private static native void nativeGetPixels(long j, int[] iArr, int i, int i2, int i3, int i4, int i5, int i6);

    private static native boolean nativeHasAlpha(long j);

    private static native boolean nativeHasMipMap(long j);

    private static native boolean nativeIsPremultiplied(long j);

    private static native void nativePrepareToDraw(long j);

    private static native void nativeReconfigure(long j, int i, int i2, int i3, int i4, boolean z);

    private static native boolean nativeRecycle(long j);

    private static native int nativeRowBytes(long j);

    private static native boolean nativeSameAs(long j, long j2);

    private static native void nativeSetHasAlpha(long j, boolean z, boolean z2);

    private static native void nativeSetHasMipMap(long j, boolean z);

    private static native void nativeSetPixel(long j, int i, int i2, int i3);

    private static native void nativeSetPixels(long j, int[] iArr, int i, int i2, int i3, int i4, int i5, int i6);

    private static native void nativeSetPremultiplied(long j, boolean z);

    private static native boolean nativeWriteToParcel(long j, boolean z, int i, Parcel parcel);

    public static void setDefaultDensity(int density) {
        sDefaultDensity = density;
    }

    static int getDefaultDensity() {
        if (sDefaultDensity >= 0) {
            return sDefaultDensity;
        }
        sDefaultDensity = DisplayMetrics.DENSITY_DEVICE;
        return sDefaultDensity;
    }

    Bitmap(long nativeBitmap, byte[] buffer, int width, int height, int density, boolean isMutable, boolean requestPremultiplied, byte[] ninePatchChunk, NinePatch.InsetStruct ninePatchInsets) {
        this.mDensity = getDefaultDensity();
        if (nativeBitmap == 0) {
            throw new RuntimeException("internal error: native bitmap is 0");
        }
        this.mWidth = width;
        this.mHeight = height;
        this.mIsMutable = isMutable;
        this.mRequestPremultiplied = requestPremultiplied;
        this.mBuffer = buffer;
        this.mNativeBitmap = nativeBitmap;
        this.mNinePatchChunk = ninePatchChunk;
        this.mNinePatchInsets = ninePatchInsets;
        if (density >= 0) {
            this.mDensity = density;
        }
        int nativeAllocationByteCount = buffer == null ? getByteCount() : 0;
        this.mFinalizer = new BitmapFinalizer(nativeBitmap, nativeAllocationByteCount);
    }

    void reinit(int width, int height, boolean requestPremultiplied) {
        this.mWidth = width;
        this.mHeight = height;
        this.mRequestPremultiplied = requestPremultiplied;
    }

    public int getDensity() {
        return this.mDensity;
    }

    public void setDensity(int density) {
        this.mDensity = density;
    }

    public void reconfigure(int width, int height, Config config) {
        checkRecycled("Can't call reconfigure() on a recycled bitmap");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (!isMutable()) {
            throw new IllegalStateException("only mutable bitmaps may be reconfigured");
        }
        if (this.mBuffer == null) {
            throw new IllegalStateException("native-backed bitmaps may not be reconfigured");
        }
        nativeReconfigure(this.mNativeBitmap, width, height, config.nativeInt, this.mBuffer.length, this.mRequestPremultiplied);
        this.mWidth = width;
        this.mHeight = height;
    }

    public void setWidth(int width) {
        reconfigure(width, getHeight(), getConfig());
    }

    public void setHeight(int height) {
        reconfigure(getWidth(), height, getConfig());
    }

    public void setConfig(Config config) {
        reconfigure(getWidth(), getHeight(), config);
    }

    public void setNinePatchChunk(byte[] chunk) {
        this.mNinePatchChunk = chunk;
    }

    public void recycle() {
        if (!this.mRecycled && this.mFinalizer.mNativeBitmap != 0) {
            if (nativeRecycle(this.mNativeBitmap)) {
                this.mBuffer = null;
                this.mNinePatchChunk = null;
            }
            this.mRecycled = true;
        }
    }

    public final boolean isRecycled() {
        return this.mRecycled;
    }

    public int getGenerationId() {
        return nativeGenerationId(this.mNativeBitmap);
    }

    private void checkRecycled(String errorMessage) {
        if (this.mRecycled) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private static void checkXYSign(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
    }

    private static void checkWidthHeight(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
    }

    public enum Config {
        ALPHA_8(1),
        RGB_565(3),
        ARGB_4444(4),
        ARGB_8888(5);

        final int nativeInt;
        private static Config[] sConfigs = {null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888};

        Config(int ni) {
            this.nativeInt = ni;
        }

        static Config nativeToConfig(int ni) {
            return sConfigs[ni];
        }
    }

    public void copyPixelsToBuffer(Buffer dst) {
        int shift;
        int elements = dst.remaining();
        if (dst instanceof ByteBuffer) {
            shift = 0;
        } else if (dst instanceof ShortBuffer) {
            shift = 1;
        } else if (dst instanceof IntBuffer) {
            shift = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }
        long bufferSize = ((long) elements) << shift;
        long pixelSize = getByteCount();
        if (bufferSize < pixelSize) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }
        nativeCopyPixelsToBuffer(this.mNativeBitmap, dst);
        int position = dst.position();
        dst.position((int) (((long) position) + (pixelSize >> shift)));
    }

    public void copyPixelsFromBuffer(Buffer src) {
        int shift;
        checkRecycled("copyPixelsFromBuffer called on recycled bitmap");
        int elements = src.remaining();
        if (src instanceof ByteBuffer) {
            shift = 0;
        } else if (src instanceof ShortBuffer) {
            shift = 1;
        } else if (src instanceof IntBuffer) {
            shift = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }
        long bufferBytes = ((long) elements) << shift;
        long bitmapBytes = getByteCount();
        if (bufferBytes < bitmapBytes) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }
        nativeCopyPixelsFromBuffer(this.mNativeBitmap, src);
        int position = src.position();
        src.position((int) (((long) position) + (bitmapBytes >> shift)));
    }

    public Bitmap copy(Config config, boolean isMutable) {
        checkRecycled("Can't copy a recycled bitmap");
        Bitmap b = nativeCopy(this.mNativeBitmap, config.nativeInt, isMutable);
        if (b != null) {
            b.setPremultiplied(this.mRequestPremultiplied);
            b.mDensity = this.mDensity;
        }
        return b;
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        Matrix m;
        synchronized (Bitmap.class) {
            m = sScaleMatrix;
            sScaleMatrix = null;
        }
        if (m == null) {
            m = new Matrix();
        }
        int width = src.getWidth();
        int height = src.getHeight();
        float sx = dstWidth / width;
        float sy = dstHeight / height;
        m.setScale(sx, sy);
        Bitmap b = createBitmap(src, 0, 0, width, height, m, filter);
        synchronized (Bitmap.class) {
            if (sScaleMatrix == null) {
                sScaleMatrix = m;
            }
        }
        return b;
    }

    public static Bitmap createBitmap(Bitmap src) {
        return createBitmap(src, 0, 0, src.getWidth(), src.getHeight());
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        return createBitmap(source, x, y, width, height, (Matrix) null, false);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter) {
        Bitmap bitmap;
        Paint paint;
        checkXYSign(x, y);
        checkWidthHeight(width, height);
        if (x + width > source.getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > source.getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (source.isMutable() || x != 0 || y != 0 || width != source.getWidth() || height != source.getHeight() || (m != null && !m.isIdentity())) {
            Canvas canvas = new Canvas();
            Rect srcR = new Rect(x, y, x + width, y + height);
            RectF dstR = new RectF(0.0f, 0.0f, width, height);
            Config newConfig = Config.ARGB_8888;
            Config config = source.getConfig();
            if (config != null) {
                switch (config) {
                    case RGB_565:
                        newConfig = Config.RGB_565;
                        break;
                    case ALPHA_8:
                        newConfig = Config.ALPHA_8;
                        break;
                    default:
                        newConfig = Config.ARGB_8888;
                        break;
                }
            }
            if (m == null || m.isIdentity()) {
                bitmap = createBitmap(width, height, newConfig, source.hasAlpha());
                paint = null;
            } else {
                boolean transformed = !m.rectStaysRect();
                RectF deviceR = new RectF();
                m.mapRect(deviceR, dstR);
                int neww = Math.round(deviceR.width());
                int newh = Math.round(deviceR.height());
                if (transformed) {
                    newConfig = Config.ARGB_8888;
                }
                bitmap = createBitmap(neww, newh, newConfig, transformed || source.hasAlpha());
                canvas.translate(-deviceR.left, -deviceR.top);
                canvas.concat(m);
                paint = new Paint();
                paint.setFilterBitmap(filter);
                if (transformed) {
                    paint.setAntiAlias(true);
                }
            }
            bitmap.mDensity = source.mDensity;
            bitmap.setHasAlpha(source.hasAlpha());
            bitmap.setPremultiplied(source.mRequestPremultiplied);
            canvas.setBitmap(bitmap);
            canvas.drawBitmap(source, srcR, dstR, paint);
            canvas.setBitmap(null);
            return bitmap;
        }
        return source;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return createBitmap(width, height, config, true);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int width, int height, Config config) {
        return createBitmap(display, width, height, config, true);
    }

    private static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha) {
        return createBitmap((DisplayMetrics) null, width, height, config, hasAlpha);
    }

    private static Bitmap createBitmap(DisplayMetrics display, int width, int height, Config config, boolean hasAlpha) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        Bitmap bm = nativeCreate(null, 0, width, width, height, config.nativeInt, true);
        if (display != null) {
            bm.mDensity = display.densityDpi;
        }
        bm.setHasAlpha(hasAlpha);
        if (config == Config.ARGB_8888 && !hasAlpha) {
            nativeErase(bm.mNativeBitmap, -16777216);
        }
        return bm;
    }

    public static Bitmap createBitmap(int[] colors, int offset, int stride, int width, int height, Config config) {
        return createBitmap((DisplayMetrics) null, colors, offset, stride, width, height, config);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int[] colors, int offset, int stride, int width, int height, Config config) {
        checkWidthHeight(width, height);
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int lastScanline = offset + ((height - 1) * stride);
        int length = colors.length;
        if (offset < 0 || offset + width > length || lastScanline < 0 || lastScanline + width > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        Bitmap bm = nativeCreate(colors, offset, stride, width, height, config.nativeInt, false);
        if (display != null) {
            bm.mDensity = display.densityDpi;
        }
        return bm;
    }

    public static Bitmap createBitmap(int[] colors, int width, int height, Config config) {
        return createBitmap((DisplayMetrics) null, colors, 0, width, width, height, config);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int[] colors, int width, int height, Config config) {
        return createBitmap(display, colors, 0, width, width, height, config);
    }

    public byte[] getNinePatchChunk() {
        return this.mNinePatchChunk;
    }

    public void getOpticalInsets(Rect outInsets) {
        if (this.mNinePatchInsets == null) {
            outInsets.setEmpty();
        } else {
            outInsets.set(this.mNinePatchInsets.opticalRect);
        }
    }

    public NinePatch.InsetStruct getNinePatchInsets() {
        return this.mNinePatchInsets;
    }

    public enum CompressFormat {
        JPEG(0),
        PNG(1),
        WEBP(2);

        final int nativeInt;

        CompressFormat(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    public boolean compress(CompressFormat format, int quality, OutputStream stream) {
        checkRecycled("Can't compress a recycled bitmap");
        if (stream == null) {
            throw new NullPointerException();
        }
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, "Bitmap.compress");
        boolean result = nativeCompress(this.mNativeBitmap, format.nativeInt, quality, stream, new byte[4096]);
        Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
        return result;
    }

    public final boolean isMutable() {
        return this.mIsMutable;
    }

    public final boolean isPremultiplied() {
        return nativeIsPremultiplied(this.mNativeBitmap);
    }

    public final void setPremultiplied(boolean premultiplied) {
        this.mRequestPremultiplied = premultiplied;
        nativeSetPremultiplied(this.mNativeBitmap, premultiplied);
    }

    public final int getWidth() {
        return this.mWidth;
    }

    public final int getHeight() {
        return this.mHeight;
    }

    public int getScaledWidth(Canvas canvas) {
        return scaleFromDensity(getWidth(), this.mDensity, canvas.mDensity);
    }

    public int getScaledHeight(Canvas canvas) {
        return scaleFromDensity(getHeight(), this.mDensity, canvas.mDensity);
    }

    public int getScaledWidth(DisplayMetrics metrics) {
        return scaleFromDensity(getWidth(), this.mDensity, metrics.densityDpi);
    }

    public int getScaledHeight(DisplayMetrics metrics) {
        return scaleFromDensity(getHeight(), this.mDensity, metrics.densityDpi);
    }

    public int getScaledWidth(int targetDensity) {
        return scaleFromDensity(getWidth(), this.mDensity, targetDensity);
    }

    public int getScaledHeight(int targetDensity) {
        return scaleFromDensity(getHeight(), this.mDensity, targetDensity);
    }

    public static int scaleFromDensity(int size, int sdensity, int tdensity) {
        return (sdensity == 0 || tdensity == 0 || sdensity == tdensity) ? size : ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }

    public final int getRowBytes() {
        return nativeRowBytes(this.mNativeBitmap);
    }

    public final int getByteCount() {
        return getRowBytes() * getHeight();
    }

    public final int getAllocationByteCount() {
        return this.mBuffer == null ? getByteCount() : this.mBuffer.length;
    }

    public final Config getConfig() {
        return Config.nativeToConfig(nativeConfig(this.mNativeBitmap));
    }

    public final boolean hasAlpha() {
        return nativeHasAlpha(this.mNativeBitmap);
    }

    public void setHasAlpha(boolean hasAlpha) {
        nativeSetHasAlpha(this.mNativeBitmap, hasAlpha, this.mRequestPremultiplied);
    }

    public final boolean hasMipMap() {
        return nativeHasMipMap(this.mNativeBitmap);
    }

    public final void setHasMipMap(boolean hasMipMap) {
        nativeSetHasMipMap(this.mNativeBitmap, hasMipMap);
    }

    public void eraseColor(int c) {
        checkRecycled("Can't erase a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException("cannot erase immutable bitmaps");
        }
        nativeErase(this.mNativeBitmap, c);
    }

    public int getPixel(int x, int y) {
        checkRecycled("Can't call getPixel() on a recycled bitmap");
        checkPixelAccess(x, y);
        return nativeGetPixel(this.mNativeBitmap, x, y);
    }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        checkRecycled("Can't call getPixels() on a recycled bitmap");
        if (width != 0 && height != 0) {
            checkPixelsAccess(x, y, width, height, offset, stride, pixels);
            nativeGetPixels(this.mNativeBitmap, pixels, offset, stride, x, y, width, height);
        }
    }

    private void checkPixelAccess(int x, int y) {
        checkXYSign(x, y);
        if (x >= getWidth()) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (y >= getHeight()) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }

    private void checkPixelsAccess(int x, int y, int width, int height, int offset, int stride, int[] pixels) {
        checkXYSign(x, y);
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (x + width > getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int lastScanline = offset + ((height - 1) * stride);
        int length = pixels.length;
        if (offset < 0 || offset + width > length || lastScanline < 0 || lastScanline + width > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public void setPixel(int x, int y, int color) {
        checkRecycled("Can't call setPixel() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        checkPixelAccess(x, y);
        nativeSetPixel(this.mNativeBitmap, x, y, color);
    }

    public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        checkRecycled("Can't call setPixels() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        if (width != 0 && height != 0) {
            checkPixelsAccess(x, y, width, height, offset, stride, pixels);
            nativeSetPixels(this.mNativeBitmap, pixels, offset, stride, x, y, width, height);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        checkRecycled("Can't parcel a recycled bitmap");
        if (!nativeWriteToParcel(this.mNativeBitmap, this.mIsMutable, this.mDensity, p)) {
            throw new RuntimeException("native writeToParcel failed");
        }
    }

    public Bitmap extractAlpha() {
        return extractAlpha(null, null);
    }

    public Bitmap extractAlpha(Paint paint, int[] offsetXY) {
        checkRecycled("Can't extractAlpha on a recycled bitmap");
        long nativePaint = paint != null ? paint.mNativePaint : 0L;
        Bitmap bm = nativeExtractAlpha(this.mNativeBitmap, nativePaint, offsetXY);
        if (bm == null) {
            throw new RuntimeException("Failed to extractAlpha on Bitmap");
        }
        bm.mDensity = this.mDensity;
        return bm;
    }

    public boolean sameAs(Bitmap other) {
        return this == other || (other != null && nativeSameAs(this.mNativeBitmap, other.mNativeBitmap));
    }

    public void prepareToDraw() {
        nativePrepareToDraw(this.mNativeBitmap);
    }

    private static class BitmapFinalizer {
        private final int mNativeAllocationByteCount;
        private long mNativeBitmap;

        BitmapFinalizer(long nativeBitmap, int nativeAllocationByteCount) {
            this.mNativeBitmap = nativeBitmap;
            this.mNativeAllocationByteCount = nativeAllocationByteCount;
            if (this.mNativeAllocationByteCount != 0) {
                VMRuntime.getRuntime().registerNativeAllocation(this.mNativeAllocationByteCount);
            }
        }

        public void finalize() {
            try {
                super.finalize();
                if (this.mNativeAllocationByteCount != 0) {
                    VMRuntime.getRuntime().registerNativeFree(this.mNativeAllocationByteCount);
                }
                Bitmap.nativeDestructor(this.mNativeBitmap);
                this.mNativeBitmap = 0L;
            } catch (Throwable th) {
                if (this.mNativeAllocationByteCount != 0) {
                    VMRuntime.getRuntime().registerNativeFree(this.mNativeAllocationByteCount);
                }
                Bitmap.nativeDestructor(this.mNativeBitmap);
                this.mNativeBitmap = 0L;
            }
        }
    }

    final long ni() {
        return this.mNativeBitmap;
    }
}
