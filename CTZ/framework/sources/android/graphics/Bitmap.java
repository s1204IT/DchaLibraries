package android.graphics;

import android.content.res.ResourcesImpl;
import android.graphics.ColorSpace;
import android.graphics.NinePatch;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayListCanvas;
import android.view.ThreadedRenderer;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import libcore.util.NativeAllocationRegistry;

public final class Bitmap implements Parcelable {
    public static final int DENSITY_NONE = 0;
    private static final long NATIVE_ALLOCATION_SIZE = 32;
    private static final String TAG = "Bitmap";
    private static final int WORKING_COMPRESS_STORAGE = 4096;
    public static volatile int sPreloadTracingNumInstantiatedBitmaps;
    public static volatile long sPreloadTracingTotalBitmapsSize;
    private ColorSpace mColorSpace;
    public int mDensity;
    private int mHeight;
    private final boolean mIsMutable;
    private final long mNativePtr;
    private byte[] mNinePatchChunk;
    private NinePatch.InsetStruct mNinePatchInsets;
    private boolean mRecycled;
    private boolean mRequestPremultiplied;
    private int mWidth;
    private static volatile int sDefaultDensity = -1;
    public static final Parcelable.Creator<Bitmap> CREATOR = new Parcelable.Creator<Bitmap>() {
        @Override
        public Bitmap createFromParcel(Parcel parcel) {
            Bitmap bitmapNativeCreateFromParcel = Bitmap.nativeCreateFromParcel(parcel);
            if (bitmapNativeCreateFromParcel == null) {
                throw new RuntimeException("Failed to unparcel Bitmap");
            }
            return bitmapNativeCreateFromParcel;
        }

        @Override
        public Bitmap[] newArray(int i) {
            return new Bitmap[i];
        }
    };

    private static native boolean nativeCompress(long j, int i, int i2, OutputStream outputStream, byte[] bArr);

    private static native int nativeConfig(long j);

    private static native Bitmap nativeCopy(long j, int i, boolean z);

    private static native Bitmap nativeCopyAshmem(long j);

    private static native Bitmap nativeCopyAshmemConfig(long j, int i);

    private static native void nativeCopyColorSpace(long j, long j2);

    private static native void nativeCopyPixelsFromBuffer(long j, Buffer buffer);

    private static native void nativeCopyPixelsToBuffer(long j, Buffer buffer);

    private static native Bitmap nativeCopyPreserveInternalConfig(long j);

    private static native Bitmap nativeCreate(int[] iArr, int i, int i2, int i3, int i4, int i5, boolean z, float[] fArr, ColorSpace.Rgb.TransferParameters transferParameters);

    private static native Bitmap nativeCreateFromParcel(Parcel parcel);

    private static native GraphicBuffer nativeCreateGraphicBufferHandle(long j);

    private static native Bitmap nativeCreateHardwareBitmap(GraphicBuffer graphicBuffer);

    private static native void nativeErase(long j, int i);

    private static native Bitmap nativeExtractAlpha(long j, long j2, int[] iArr);

    private static native int nativeGenerationId(long j);

    private static native int nativeGetAllocationByteCount(long j);

    private static native boolean nativeGetColorSpace(long j, float[] fArr, float[] fArr2);

    private static native long nativeGetNativeFinalizer();

    private static native int nativeGetPixel(long j, int i, int i2);

    private static native void nativeGetPixels(long j, int[] iArr, int i, int i2, int i3, int i4, int i5, int i6);

    private static native boolean nativeHasAlpha(long j);

    private static native boolean nativeHasMipMap(long j);

    private static native boolean nativeIsPremultiplied(long j);

    private static native boolean nativeIsSRGB(long j);

    private static native boolean nativeIsSRGBLinear(long j);

    private static native void nativePrepareToDraw(long j);

    private static native void nativeReconfigure(long j, int i, int i2, int i3, boolean z);

    private static native boolean nativeRecycle(long j);

    private static native int nativeRowBytes(long j);

    private static native boolean nativeSameAs(long j, long j2);

    private static native void nativeSetHasAlpha(long j, boolean z, boolean z2);

    private static native void nativeSetHasMipMap(long j, boolean z);

    private static native void nativeSetPixel(long j, int i, int i2, int i3);

    private static native void nativeSetPixels(long j, int[] iArr, int i, int i2, int i3, int i4, int i5, int i6);

    private static native void nativeSetPremultiplied(long j, boolean z);

    private static native boolean nativeWriteToParcel(long j, boolean z, int i, Parcel parcel);

    public static void setDefaultDensity(int i) {
        sDefaultDensity = i;
    }

    static int getDefaultDensity() {
        if (sDefaultDensity >= 0) {
            return sDefaultDensity;
        }
        sDefaultDensity = DisplayMetrics.DENSITY_DEVICE;
        return sDefaultDensity;
    }

    Bitmap(long j, int i, int i2, int i3, boolean z, boolean z2, byte[] bArr, NinePatch.InsetStruct insetStruct) {
        this.mDensity = getDefaultDensity();
        if (j == 0) {
            throw new RuntimeException("internal error: native bitmap is 0");
        }
        this.mWidth = i;
        this.mHeight = i2;
        this.mIsMutable = z;
        this.mRequestPremultiplied = z2;
        this.mNinePatchChunk = bArr;
        this.mNinePatchInsets = insetStruct;
        if (i3 >= 0) {
            this.mDensity = i3;
        }
        this.mNativePtr = j;
        long allocationByteCount = 32 + ((long) getAllocationByteCount());
        new NativeAllocationRegistry(Bitmap.class.getClassLoader(), nativeGetNativeFinalizer(), allocationByteCount).registerNativeAllocation(this, j);
        if (ResourcesImpl.TRACE_FOR_DETAILED_PRELOAD) {
            sPreloadTracingNumInstantiatedBitmaps++;
            sPreloadTracingTotalBitmapsSize += allocationByteCount;
        }
    }

    public long getNativeInstance() {
        return this.mNativePtr;
    }

    void reinit(int i, int i2, boolean z) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mRequestPremultiplied = z;
        this.mColorSpace = null;
    }

    public int getDensity() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getDensity() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return this.mDensity;
    }

    public void setDensity(int i) {
        this.mDensity = i;
    }

    public void reconfigure(int i, int i2, Config config) {
        checkRecycled("Can't call reconfigure() on a recycled bitmap");
        if (i <= 0 || i2 <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (!isMutable()) {
            throw new IllegalStateException("only mutable bitmaps may be reconfigured");
        }
        nativeReconfigure(this.mNativePtr, i, i2, config.nativeInt, this.mRequestPremultiplied);
        this.mWidth = i;
        this.mHeight = i2;
        this.mColorSpace = null;
    }

    public void setWidth(int i) {
        reconfigure(i, getHeight(), getConfig());
    }

    public void setHeight(int i) {
        reconfigure(getWidth(), i, getConfig());
    }

    public void setConfig(Config config) {
        reconfigure(getWidth(), getHeight(), config);
    }

    public void setNinePatchChunk(byte[] bArr) {
        this.mNinePatchChunk = bArr;
    }

    public void recycle() {
        if (!this.mRecycled && this.mNativePtr != 0) {
            if (nativeRecycle(this.mNativePtr)) {
                this.mNinePatchChunk = null;
            }
            this.mRecycled = true;
        }
    }

    public final boolean isRecycled() {
        return this.mRecycled;
    }

    public int getGenerationId() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getGenerationId() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return nativeGenerationId(this.mNativePtr);
    }

    private void checkRecycled(String str) {
        if (this.mRecycled) {
            throw new IllegalStateException(str);
        }
    }

    private void checkHardware(String str) {
        if (getConfig() == Config.HARDWARE) {
            throw new IllegalStateException(str);
        }
    }

    private static void checkXYSign(int i, int i2) {
        if (i < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (i2 < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
    }

    private static void checkWidthHeight(int i, int i2) {
        if (i <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (i2 <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
    }

    public enum Config {
        ALPHA_8(1),
        RGB_565(3),
        ARGB_4444(4),
        ARGB_8888(5),
        RGBA_F16(6),
        HARDWARE(7);

        final int nativeInt;
        private static Config[] sConfigs = {null, ALPHA_8, null, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE};

        Config(int i) {
            this.nativeInt = i;
        }

        static Config nativeToConfig(int i) {
            return sConfigs[i];
        }
    }

    public void copyPixelsToBuffer(Buffer buffer) {
        char c;
        checkHardware("unable to copyPixelsToBuffer, pixel access is not supported on Config#HARDWARE bitmaps");
        int iRemaining = buffer.remaining();
        if (buffer instanceof ByteBuffer) {
            c = 0;
        } else if (buffer instanceof ShortBuffer) {
            c = 1;
        } else if (buffer instanceof IntBuffer) {
            c = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }
        long j = ((long) iRemaining) << c;
        long byteCount = getByteCount();
        if (j < byteCount) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }
        nativeCopyPixelsToBuffer(this.mNativePtr, buffer);
        buffer.position((int) (((long) buffer.position()) + (byteCount >> c)));
    }

    public void copyPixelsFromBuffer(Buffer buffer) {
        char c;
        checkRecycled("copyPixelsFromBuffer called on recycled bitmap");
        checkHardware("unable to copyPixelsFromBuffer, Config#HARDWARE bitmaps are immutable");
        int iRemaining = buffer.remaining();
        if (buffer instanceof ByteBuffer) {
            c = 0;
        } else if (buffer instanceof ShortBuffer) {
            c = 1;
        } else if (buffer instanceof IntBuffer) {
            c = 2;
        } else {
            throw new RuntimeException("unsupported Buffer subclass");
        }
        long j = ((long) iRemaining) << c;
        long byteCount = getByteCount();
        if (j < byteCount) {
            throw new RuntimeException("Buffer not large enough for pixels");
        }
        nativeCopyPixelsFromBuffer(this.mNativePtr, buffer);
        buffer.position((int) (((long) buffer.position()) + (byteCount >> c)));
    }

    private void noteHardwareBitmapSlowCall() {
        if (getConfig() == Config.HARDWARE) {
            StrictMode.noteSlowCall("Warning: attempt to read pixels from hardware bitmap, which is very slow operation");
        }
    }

    public Bitmap copy(Config config, boolean z) {
        checkRecycled("Can't copy a recycled bitmap");
        if (config == Config.HARDWARE && z) {
            throw new IllegalArgumentException("Hardware bitmaps are always immutable");
        }
        noteHardwareBitmapSlowCall();
        Bitmap bitmapNativeCopy = nativeCopy(this.mNativePtr, config.nativeInt, z);
        if (bitmapNativeCopy != null) {
            bitmapNativeCopy.setPremultiplied(this.mRequestPremultiplied);
            bitmapNativeCopy.mDensity = this.mDensity;
        }
        return bitmapNativeCopy;
    }

    public Bitmap createAshmemBitmap() {
        checkRecycled("Can't copy a recycled bitmap");
        noteHardwareBitmapSlowCall();
        Bitmap bitmapNativeCopyAshmem = nativeCopyAshmem(this.mNativePtr);
        if (bitmapNativeCopyAshmem != null) {
            bitmapNativeCopyAshmem.setPremultiplied(this.mRequestPremultiplied);
            bitmapNativeCopyAshmem.mDensity = this.mDensity;
        }
        return bitmapNativeCopyAshmem;
    }

    public Bitmap createAshmemBitmap(Config config) {
        checkRecycled("Can't copy a recycled bitmap");
        noteHardwareBitmapSlowCall();
        Bitmap bitmapNativeCopyAshmemConfig = nativeCopyAshmemConfig(this.mNativePtr, config.nativeInt);
        if (bitmapNativeCopyAshmemConfig != null) {
            bitmapNativeCopyAshmemConfig.setPremultiplied(this.mRequestPremultiplied);
            bitmapNativeCopyAshmemConfig.mDensity = this.mDensity;
        }
        return bitmapNativeCopyAshmemConfig;
    }

    public static Bitmap createHardwareBitmap(GraphicBuffer graphicBuffer) {
        return nativeCreateHardwareBitmap(graphicBuffer);
    }

    public static Bitmap createScaledBitmap(Bitmap bitmap, int i, int i2, boolean z) {
        Matrix matrix = new Matrix();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width != i || height != i2) {
            matrix.setScale(i / width, i2 / height);
        }
        return createBitmap(bitmap, 0, 0, width, height, matrix, z);
    }

    public static Bitmap createBitmap(Bitmap bitmap) {
        return createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public static Bitmap createBitmap(Bitmap bitmap, int i, int i2, int i3, int i4) {
        return createBitmap(bitmap, i, i2, i3, i4, (Matrix) null, false);
    }

    public static Bitmap createBitmap(Bitmap bitmap, int i, int i2, int i3, int i4, Matrix matrix, boolean z) {
        Bitmap bitmapCreateBitmap;
        Paint paint;
        checkXYSign(i, i2);
        checkWidthHeight(i3, i4);
        int i5 = i + i3;
        if (i5 > bitmap.getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        int i6 = i2 + i4;
        if (i6 > bitmap.getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (!bitmap.isMutable() && i == 0 && i2 == 0 && i3 == bitmap.getWidth() && i4 == bitmap.getHeight() && (matrix == null || matrix.isIdentity())) {
            return bitmap;
        }
        boolean z2 = bitmap.getConfig() == Config.HARDWARE;
        if (z2) {
            bitmap.noteHardwareBitmapSlowCall();
            bitmap = nativeCopyPreserveInternalConfig(bitmap.mNativePtr);
        }
        Rect rect = new Rect(i, i2, i5, i6);
        RectF rectF = new RectF(0.0f, 0.0f, i3, i4);
        RectF rectF2 = new RectF();
        Config config = Config.ARGB_8888;
        Config config2 = bitmap.getConfig();
        if (config2 != null) {
            switch (config2) {
                case RGB_565:
                    config = Config.RGB_565;
                    break;
                case ALPHA_8:
                    config = Config.ALPHA_8;
                    break;
                case RGBA_F16:
                    config = Config.RGBA_F16;
                    break;
                default:
                    config = Config.ARGB_8888;
                    break;
            }
        }
        if (matrix == null || matrix.isIdentity()) {
            bitmapCreateBitmap = createBitmap(i3, i4, config, bitmap.hasAlpha());
            paint = null;
        } else {
            boolean z3 = !matrix.rectStaysRect();
            matrix.mapRect(rectF2, rectF);
            int iRound = Math.round(rectF2.width());
            int iRound2 = Math.round(rectF2.height());
            if (z3 && config != Config.ARGB_8888 && config != Config.RGBA_F16) {
                config = Config.ARGB_8888;
            }
            bitmapCreateBitmap = createBitmap(iRound, iRound2, config, z3 || bitmap.hasAlpha());
            paint = new Paint();
            paint.setFilterBitmap(z);
            if (z3) {
                paint.setAntiAlias(true);
            }
        }
        nativeCopyColorSpace(bitmap.mNativePtr, bitmapCreateBitmap.mNativePtr);
        bitmapCreateBitmap.mDensity = bitmap.mDensity;
        bitmapCreateBitmap.setHasAlpha(bitmap.hasAlpha());
        bitmapCreateBitmap.setPremultiplied(bitmap.mRequestPremultiplied);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate(-rectF2.left, -rectF2.top);
        canvas.concat(matrix);
        canvas.drawBitmap(bitmap, rect, rectF, paint);
        canvas.setBitmap(null);
        if (z2) {
            return bitmapCreateBitmap.copy(Config.HARDWARE, false);
        }
        return bitmapCreateBitmap;
    }

    public static Bitmap createBitmap(int i, int i2, Config config) {
        return createBitmap(i, i2, config, true);
    }

    public static Bitmap createBitmap(DisplayMetrics displayMetrics, int i, int i2, Config config) {
        return createBitmap(displayMetrics, i, i2, config, true);
    }

    public static Bitmap createBitmap(int i, int i2, Config config, boolean z) {
        return createBitmap((DisplayMetrics) null, i, i2, config, z);
    }

    public static Bitmap createBitmap(int i, int i2, Config config, boolean z, ColorSpace colorSpace) {
        return createBitmap((DisplayMetrics) null, i, i2, config, z, colorSpace);
    }

    public static Bitmap createBitmap(DisplayMetrics displayMetrics, int i, int i2, Config config, boolean z) {
        return createBitmap(displayMetrics, i, i2, config, z, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public static Bitmap createBitmap(DisplayMetrics displayMetrics, int i, int i2, Config config, boolean z, ColorSpace colorSpace) {
        Bitmap bitmapNativeCreate;
        if (i <= 0 || i2 <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (config == Config.HARDWARE) {
            throw new IllegalArgumentException("can't create mutable bitmap with Config.HARDWARE");
        }
        if (colorSpace == null) {
            throw new IllegalArgumentException("can't create bitmap without a color space");
        }
        if (config != Config.ARGB_8888 || colorSpace == ColorSpace.get(ColorSpace.Named.SRGB)) {
            bitmapNativeCreate = nativeCreate(null, 0, i, i, i2, config.nativeInt, true, null, null);
        } else {
            if (!(colorSpace instanceof ColorSpace.Rgb)) {
                throw new IllegalArgumentException("colorSpace must be an RGB color space");
            }
            ColorSpace.Rgb rgb = (ColorSpace.Rgb) colorSpace;
            ColorSpace.Rgb.TransferParameters transferParameters = rgb.getTransferParameters();
            if (transferParameters == null) {
                throw new IllegalArgumentException("colorSpace must use an ICC parametric transfer function");
            }
            bitmapNativeCreate = nativeCreate(null, 0, i, i, i2, config.nativeInt, true, ((ColorSpace.Rgb) ColorSpace.adapt(rgb, ColorSpace.ILLUMINANT_D50)).getTransform(), transferParameters);
        }
        if (displayMetrics != null) {
            bitmapNativeCreate.mDensity = displayMetrics.densityDpi;
        }
        bitmapNativeCreate.setHasAlpha(z);
        if ((config == Config.ARGB_8888 || config == Config.RGBA_F16) && !z) {
            nativeErase(bitmapNativeCreate.mNativePtr, -16777216);
        }
        return bitmapNativeCreate;
    }

    public static Bitmap createBitmap(int[] iArr, int i, int i2, int i3, int i4, Config config) {
        return createBitmap((DisplayMetrics) null, iArr, i, i2, i3, i4, config);
    }

    public static Bitmap createBitmap(DisplayMetrics displayMetrics, int[] iArr, int i, int i2, int i3, int i4, Config config) {
        checkWidthHeight(i3, i4);
        if (Math.abs(i2) < i3) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int i5 = ((i4 - 1) * i2) + i;
        int length = iArr.length;
        if (i < 0 || i + i3 > length || i5 < 0 || i5 + i3 > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (i3 > 0 && i4 > 0) {
            Bitmap bitmapNativeCreate = nativeCreate(iArr, i, i2, i3, i4, config.nativeInt, false, null, null);
            if (displayMetrics != null) {
                bitmapNativeCreate.mDensity = displayMetrics.densityDpi;
            }
            return bitmapNativeCreate;
        }
        throw new IllegalArgumentException("width and height must be > 0");
    }

    public static Bitmap createBitmap(int[] iArr, int i, int i2, Config config) {
        return createBitmap((DisplayMetrics) null, iArr, 0, i, i, i2, config);
    }

    public static Bitmap createBitmap(DisplayMetrics displayMetrics, int[] iArr, int i, int i2, Config config) {
        return createBitmap(displayMetrics, iArr, 0, i, i, i2, config);
    }

    public static Bitmap createBitmap(Picture picture) {
        return createBitmap(picture, picture.getWidth(), picture.getHeight(), Config.HARDWARE);
    }

    public static Bitmap createBitmap(Picture picture, int i, int i2, Config config) {
        if (i <= 0 || i2 <= 0) {
            throw new IllegalArgumentException("width & height must be > 0");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config must not be null");
        }
        picture.endRecording();
        if (picture.requiresHardwareAcceleration() && config != Config.HARDWARE) {
            StrictMode.noteSlowCall("GPU readback");
        }
        if (config == Config.HARDWARE || picture.requiresHardwareAcceleration()) {
            android.view.RenderNode renderNodeCreate = android.view.RenderNode.create("BitmapTemporary", null);
            renderNodeCreate.setLeftTopRightBottom(0, 0, i, i2);
            renderNodeCreate.setClipToBounds(false);
            DisplayListCanvas displayListCanvasStart = renderNodeCreate.start(i, i2);
            if (picture.getWidth() != i || picture.getHeight() != i2) {
                displayListCanvasStart.scale(i / picture.getWidth(), i2 / picture.getHeight());
            }
            displayListCanvasStart.drawPicture(picture);
            renderNodeCreate.end(displayListCanvasStart);
            Bitmap bitmapCreateHardwareBitmap = ThreadedRenderer.createHardwareBitmap(renderNodeCreate, i, i2);
            if (config != Config.HARDWARE) {
                return bitmapCreateHardwareBitmap.copy(config, false);
            }
            return bitmapCreateHardwareBitmap;
        }
        Bitmap bitmapCreateBitmap = createBitmap(i, i2, config);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        if (picture.getWidth() != i || picture.getHeight() != i2) {
            canvas.scale(i / picture.getWidth(), i2 / picture.getHeight());
        }
        canvas.drawPicture(picture);
        canvas.setBitmap(null);
        bitmapCreateBitmap.makeImmutable();
        return bitmapCreateBitmap;
    }

    public byte[] getNinePatchChunk() {
        return this.mNinePatchChunk;
    }

    public void getOpticalInsets(Rect rect) {
        if (this.mNinePatchInsets == null) {
            rect.setEmpty();
        } else {
            rect.set(this.mNinePatchInsets.opticalRect);
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

        CompressFormat(int i) {
            this.nativeInt = i;
        }
    }

    public boolean compress(CompressFormat compressFormat, int i, OutputStream outputStream) {
        checkRecycled("Can't compress a recycled bitmap");
        if (outputStream == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        StrictMode.noteSlowCall("Compression of a bitmap is slow");
        Trace.traceBegin(8192L, "Bitmap.compress");
        boolean zNativeCompress = nativeCompress(this.mNativePtr, compressFormat.nativeInt, i, outputStream, new byte[4096]);
        Trace.traceEnd(8192L);
        return zNativeCompress;
    }

    public final boolean isMutable() {
        return this.mIsMutable;
    }

    public final void makeImmutable() {
    }

    public final boolean isPremultiplied() {
        if (this.mRecycled) {
            Log.w(TAG, "Called isPremultiplied() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return nativeIsPremultiplied(this.mNativePtr);
    }

    public final void setPremultiplied(boolean z) {
        checkRecycled("setPremultiplied called on a recycled bitmap");
        this.mRequestPremultiplied = z;
        nativeSetPremultiplied(this.mNativePtr, z);
    }

    public final int getWidth() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getWidth() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return this.mWidth;
    }

    public final int getHeight() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getHeight() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return this.mHeight;
    }

    public int getScaledWidth(Canvas canvas) {
        return scaleFromDensity(getWidth(), this.mDensity, canvas.mDensity);
    }

    public int getScaledHeight(Canvas canvas) {
        return scaleFromDensity(getHeight(), this.mDensity, canvas.mDensity);
    }

    public int getScaledWidth(DisplayMetrics displayMetrics) {
        return scaleFromDensity(getWidth(), this.mDensity, displayMetrics.densityDpi);
    }

    public int getScaledHeight(DisplayMetrics displayMetrics) {
        return scaleFromDensity(getHeight(), this.mDensity, displayMetrics.densityDpi);
    }

    public int getScaledWidth(int i) {
        return scaleFromDensity(getWidth(), this.mDensity, i);
    }

    public int getScaledHeight(int i) {
        return scaleFromDensity(getHeight(), this.mDensity, i);
    }

    public static int scaleFromDensity(int i, int i2, int i3) {
        if (i2 == 0 || i3 == 0 || i2 == i3) {
            return i;
        }
        return ((i * i3) + (i2 >> 1)) / i2;
    }

    public final int getRowBytes() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getRowBytes() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return nativeRowBytes(this.mNativePtr);
    }

    public final int getByteCount() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getByteCount() on a recycle()'d bitmap! This is undefined behavior!");
            return 0;
        }
        return getRowBytes() * getHeight();
    }

    public final int getAllocationByteCount() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getAllocationByteCount() on a recycle()'d bitmap! This is undefined behavior!");
            return 0;
        }
        return nativeGetAllocationByteCount(this.mNativePtr);
    }

    public final Config getConfig() {
        if (this.mRecycled) {
            Log.w(TAG, "Called getConfig() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return Config.nativeToConfig(nativeConfig(this.mNativePtr));
    }

    public final boolean hasAlpha() {
        if (this.mRecycled) {
            Log.w(TAG, "Called hasAlpha() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return nativeHasAlpha(this.mNativePtr);
    }

    public void setHasAlpha(boolean z) {
        checkRecycled("setHasAlpha called on a recycled bitmap");
        nativeSetHasAlpha(this.mNativePtr, z, this.mRequestPremultiplied);
    }

    public final boolean hasMipMap() {
        if (this.mRecycled) {
            Log.w(TAG, "Called hasMipMap() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return nativeHasMipMap(this.mNativePtr);
    }

    public final void setHasMipMap(boolean z) {
        checkRecycled("setHasMipMap called on a recycled bitmap");
        nativeSetHasMipMap(this.mNativePtr, z);
    }

    public final ColorSpace getColorSpace() {
        Bitmap bitmap;
        if (getConfig() == Config.RGBA_F16) {
            this.mColorSpace = null;
            return ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB);
        }
        if (this.mColorSpace == null) {
            if (nativeIsSRGB(this.mNativePtr)) {
                this.mColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            } else if (getConfig() == Config.HARDWARE && nativeIsSRGBLinear(this.mNativePtr)) {
                this.mColorSpace = ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB);
            } else {
                float[] fArr = new float[9];
                if (nativeGetColorSpace(this.mNativePtr, fArr, new float[7])) {
                    ColorSpace.Rgb.TransferParameters transferParameters = new ColorSpace.Rgb.TransferParameters(r2[0], r2[1], r2[2], r2[3], r2[4], r2[5], r2[6]);
                    ColorSpace colorSpaceMatch = ColorSpace.match(fArr, transferParameters);
                    if (colorSpaceMatch != null) {
                        bitmap = this;
                        bitmap.mColorSpace = colorSpaceMatch;
                    } else {
                        bitmap = this;
                        bitmap.mColorSpace = new ColorSpace.Rgb("Unknown", fArr, transferParameters);
                    }
                }
            }
            bitmap = this;
        } else {
            bitmap = this;
        }
        return bitmap.mColorSpace;
    }

    public void eraseColor(int i) {
        checkRecycled("Can't erase a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException("cannot erase immutable bitmaps");
        }
        nativeErase(this.mNativePtr, i);
    }

    public int getPixel(int i, int i2) {
        checkRecycled("Can't call getPixel() on a recycled bitmap");
        checkHardware("unable to getPixel(), pixel access is not supported on Config#HARDWARE bitmaps");
        checkPixelAccess(i, i2);
        return nativeGetPixel(this.mNativePtr, i, i2);
    }

    public void getPixels(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6) {
        checkRecycled("Can't call getPixels() on a recycled bitmap");
        checkHardware("unable to getPixels(), pixel access is not supported on Config#HARDWARE bitmaps");
        if (i5 == 0 || i6 == 0) {
            return;
        }
        checkPixelsAccess(i3, i4, i5, i6, i, i2, iArr);
        nativeGetPixels(this.mNativePtr, iArr, i, i2, i3, i4, i5, i6);
    }

    private void checkPixelAccess(int i, int i2) {
        checkXYSign(i, i2);
        if (i >= getWidth()) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (i2 >= getHeight()) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }

    private void checkPixelsAccess(int i, int i2, int i3, int i4, int i5, int i6, int[] iArr) {
        checkXYSign(i, i2);
        if (i3 < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (i4 < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (i + i3 > getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (i2 + i4 > getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (Math.abs(i6) < i3) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int i7 = ((i4 - 1) * i6) + i5;
        int length = iArr.length;
        if (i5 < 0 || i5 + i3 > length || i7 < 0 || i7 + i3 > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public void setPixel(int i, int i2, int i3) {
        checkRecycled("Can't call setPixel() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        checkPixelAccess(i, i2);
        nativeSetPixel(this.mNativePtr, i, i2, i3);
    }

    public void setPixels(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6) {
        checkRecycled("Can't call setPixels() on a recycled bitmap");
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        if (i5 == 0 || i6 == 0) {
            return;
        }
        checkPixelsAccess(i3, i4, i5, i6, i, i2, iArr);
        nativeSetPixels(this.mNativePtr, iArr, i, i2, i3, i4, i5, i6);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        checkRecycled("Can't parcel a recycled bitmap");
        noteHardwareBitmapSlowCall();
        if (!nativeWriteToParcel(this.mNativePtr, this.mIsMutable, this.mDensity, parcel)) {
            throw new RuntimeException("native writeToParcel failed");
        }
    }

    public Bitmap extractAlpha() {
        return extractAlpha(null, null);
    }

    public Bitmap extractAlpha(Paint paint, int[] iArr) {
        checkRecycled("Can't extractAlpha on a recycled bitmap");
        long nativeInstance = paint != null ? paint.getNativeInstance() : 0L;
        noteHardwareBitmapSlowCall();
        Bitmap bitmapNativeExtractAlpha = nativeExtractAlpha(this.mNativePtr, nativeInstance, iArr);
        if (bitmapNativeExtractAlpha == null) {
            throw new RuntimeException("Failed to extractAlpha on Bitmap");
        }
        bitmapNativeExtractAlpha.mDensity = this.mDensity;
        return bitmapNativeExtractAlpha;
    }

    public boolean sameAs(Bitmap bitmap) {
        checkRecycled("Can't call sameAs on a recycled bitmap!");
        noteHardwareBitmapSlowCall();
        if (this == bitmap) {
            return true;
        }
        if (bitmap == null) {
            return false;
        }
        bitmap.noteHardwareBitmapSlowCall();
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("Can't compare to a recycled bitmap!");
        }
        return nativeSameAs(this.mNativePtr, bitmap.mNativePtr);
    }

    public void prepareToDraw() {
        checkRecycled("Can't prepareToDraw on a recycled bitmap!");
        nativePrepareToDraw(this.mNativePtr);
    }

    public GraphicBuffer createGraphicBufferHandle() {
        return nativeCreateGraphicBufferHandle(this.mNativePtr);
    }
}
