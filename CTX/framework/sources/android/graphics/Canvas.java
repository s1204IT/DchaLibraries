package android.graphics;

import android.graphics.PorterDuff;
import android.graphics.Region;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.microedition.khronos.opengles.GL;
import libcore.util.NativeAllocationRegistry;

public class Canvas extends BaseCanvas {
    public static final int ALL_SAVE_FLAG = 31;
    public static final int CLIP_SAVE_FLAG = 2;
    public static final int CLIP_TO_LAYER_SAVE_FLAG = 16;
    public static final int FULL_COLOR_LAYER_SAVE_FLAG = 8;
    public static final int HAS_ALPHA_LAYER_SAVE_FLAG = 4;
    public static final int MATRIX_SAVE_FLAG = 1;
    private static final int MAXMIMUM_BITMAP_SIZE = 32766;
    private static final long NATIVE_ALLOCATION_SIZE = 525;
    private Bitmap mBitmap;
    private DrawFilter mDrawFilter;
    private Runnable mFinalizer;
    private static int sCompatiblityVersion = 0;
    public static boolean sCompatibilityRestore = false;
    public static boolean sCompatibilitySetBitmap = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Saveflags {
    }

    @CriticalNative
    private static native boolean nClipPath(long j, long j2, int i);

    @CriticalNative
    private static native boolean nClipRect(long j, float f, float f2, float f3, float f4, int i);

    @CriticalNative
    private static native void nConcat(long j, long j2);

    private static native void nFreeCaches();

    private static native void nFreeTextLayoutCaches();

    @FastNative
    private static native boolean nGetClipBounds(long j, Rect rect);

    @CriticalNative
    private static native int nGetHeight(long j);

    @CriticalNative
    private static native void nGetMatrix(long j, long j2);

    private static native long nGetNativeFinalizer();

    @CriticalNative
    private static native int nGetSaveCount(long j);

    @CriticalNative
    private static native int nGetWidth(long j);

    private static native long nInitRaster(Bitmap bitmap);

    @CriticalNative
    private static native boolean nIsOpaque(long j);

    @CriticalNative
    private static native boolean nQuickReject(long j, float f, float f2, float f3, float f4);

    @CriticalNative
    private static native boolean nQuickReject(long j, long j2);

    @CriticalNative
    private static native boolean nRestore(long j);

    @CriticalNative
    private static native void nRestoreToCount(long j, int i);

    @CriticalNative
    private static native void nRotate(long j, float f);

    @CriticalNative
    private static native int nSave(long j, int i);

    @CriticalNative
    private static native int nSaveLayer(long j, float f, float f2, float f3, float f4, long j2, int i);

    @CriticalNative
    private static native int nSaveLayerAlpha(long j, float f, float f2, float f3, float f4, int i, int i2);

    @CriticalNative
    private static native void nScale(long j, float f, float f2);

    @FastNative
    private static native void nSetBitmap(long j, Bitmap bitmap);

    private static native void nSetCompatibilityVersion(int i);

    @CriticalNative
    private static native void nSetDrawFilter(long j, long j2);

    @CriticalNative
    private static native void nSetMatrix(long j, long j2);

    @CriticalNative
    private static native void nSkew(long j, float f, float f2);

    @CriticalNative
    private static native void nTranslate(long j, float f, float f2);

    public long getNativeCanvasWrapper() {
        return this.mNativeCanvasWrapper;
    }

    public boolean isRecordingFor(Object obj) {
        return false;
    }

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Canvas.class.getClassLoader(), Canvas.nGetNativeFinalizer(), Canvas.NATIVE_ALLOCATION_SIZE);

        private NoImagePreloadHolder() {
        }
    }

    public Canvas() {
        if (!isHardwareAccelerated()) {
            this.mNativeCanvasWrapper = nInitRaster(null);
            this.mFinalizer = NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeCanvasWrapper);
        } else {
            this.mFinalizer = null;
        }
    }

    public Canvas(Bitmap bitmap) {
        if (!bitmap.isMutable()) {
            throw new IllegalStateException("Immutable bitmap passed to Canvas constructor");
        }
        throwIfCannotDraw(bitmap);
        this.mNativeCanvasWrapper = nInitRaster(bitmap);
        this.mFinalizer = NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeCanvasWrapper);
        this.mBitmap = bitmap;
        this.mDensity = bitmap.mDensity;
    }

    public Canvas(long j) {
        if (j == 0) {
            throw new IllegalStateException();
        }
        this.mNativeCanvasWrapper = j;
        this.mFinalizer = NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeCanvasWrapper);
        this.mDensity = Bitmap.getDefaultDensity();
    }

    @Deprecated
    protected GL getGL() {
        return null;
    }

    @Override
    public boolean isHardwareAccelerated() {
        return false;
    }

    public void setBitmap(Bitmap bitmap) {
        Matrix matrix;
        if (isHardwareAccelerated()) {
            throw new RuntimeException("Can't set a bitmap device on a HW accelerated canvas");
        }
        if (bitmap != null && sCompatibilitySetBitmap) {
            matrix = getMatrix();
        } else {
            matrix = null;
        }
        if (bitmap == null) {
            nSetBitmap(this.mNativeCanvasWrapper, null);
            this.mDensity = 0;
        } else {
            if (!bitmap.isMutable()) {
                throw new IllegalStateException();
            }
            throwIfCannotDraw(bitmap);
            nSetBitmap(this.mNativeCanvasWrapper, bitmap);
            this.mDensity = bitmap.mDensity;
        }
        if (matrix != null) {
            setMatrix(matrix);
        }
        this.mBitmap = bitmap;
    }

    public void insertReorderBarrier() {
    }

    public void insertInorderBarrier() {
    }

    public boolean isOpaque() {
        return nIsOpaque(this.mNativeCanvasWrapper);
    }

    public int getWidth() {
        return nGetWidth(this.mNativeCanvasWrapper);
    }

    public int getHeight() {
        return nGetHeight(this.mNativeCanvasWrapper);
    }

    public int getDensity() {
        return this.mDensity;
    }

    public void setDensity(int i) {
        if (this.mBitmap != null) {
            this.mBitmap.setDensity(i);
        }
        this.mDensity = i;
    }

    public void setScreenDensity(int i) {
        this.mScreenDensity = i;
    }

    public int getMaximumBitmapWidth() {
        return MAXMIMUM_BITMAP_SIZE;
    }

    public int getMaximumBitmapHeight() {
        return MAXMIMUM_BITMAP_SIZE;
    }

    private static void checkValidSaveFlags(int i) {
        if (sCompatiblityVersion >= 28 && i != 31) {
            throw new IllegalArgumentException("Invalid Layer Save Flag - only ALL_SAVE_FLAGS is allowed");
        }
    }

    public int save() {
        return nSave(this.mNativeCanvasWrapper, 3);
    }

    public int save(int i) {
        return nSave(this.mNativeCanvasWrapper, i);
    }

    public int saveLayer(RectF rectF, Paint paint, int i) {
        if (rectF == null) {
            rectF = new RectF(getClipBounds());
        }
        checkValidSaveFlags(i);
        return saveLayer(rectF.left, rectF.top, rectF.right, rectF.bottom, paint, 31);
    }

    public int saveLayer(RectF rectF, Paint paint) {
        return saveLayer(rectF, paint, 31);
    }

    public int saveUnclippedLayer(int i, int i2, int i3, int i4) {
        return nSaveLayer(this.mNativeCanvasWrapper, i, i2, i3, i4, 0L, 0);
    }

    public int saveLayer(float f, float f2, float f3, float f4, Paint paint, int i) {
        checkValidSaveFlags(i);
        return nSaveLayer(this.mNativeCanvasWrapper, f, f2, f3, f4, paint != null ? paint.getNativeInstance() : 0L, 31);
    }

    public int saveLayer(float f, float f2, float f3, float f4, Paint paint) {
        return saveLayer(f, f2, f3, f4, paint, 31);
    }

    public int saveLayerAlpha(RectF rectF, int i, int i2) {
        if (rectF == null) {
            rectF = new RectF(getClipBounds());
        }
        checkValidSaveFlags(i2);
        return saveLayerAlpha(rectF.left, rectF.top, rectF.right, rectF.bottom, i, 31);
    }

    public int saveLayerAlpha(RectF rectF, int i) {
        return saveLayerAlpha(rectF, i, 31);
    }

    public int saveLayerAlpha(float f, float f2, float f3, float f4, int i, int i2) {
        checkValidSaveFlags(i2);
        return nSaveLayerAlpha(this.mNativeCanvasWrapper, f, f2, f3, f4, Math.min(255, Math.max(0, i)), 31);
    }

    public int saveLayerAlpha(float f, float f2, float f3, float f4, int i) {
        return saveLayerAlpha(f, f2, f3, f4, i, 31);
    }

    public void restore() {
        if (!nRestore(this.mNativeCanvasWrapper)) {
            if (!sCompatibilityRestore || !isHardwareAccelerated()) {
                throw new IllegalStateException("Underflow in restore - more restores than saves");
            }
        }
    }

    public int getSaveCount() {
        return nGetSaveCount(this.mNativeCanvasWrapper);
    }

    public void restoreToCount(int i) {
        if (i < 1) {
            if (!sCompatibilityRestore || !isHardwareAccelerated()) {
                throw new IllegalArgumentException("Underflow in restoreToCount - more restores than saves");
            }
            i = 1;
        }
        nRestoreToCount(this.mNativeCanvasWrapper, i);
    }

    public void translate(float f, float f2) {
        if (f == 0.0f && f2 == 0.0f) {
            return;
        }
        nTranslate(this.mNativeCanvasWrapper, f, f2);
    }

    public void scale(float f, float f2) {
        if (f == 1.0f && f2 == 1.0f) {
            return;
        }
        nScale(this.mNativeCanvasWrapper, f, f2);
    }

    public final void scale(float f, float f2, float f3, float f4) {
        if (f == 1.0f && f2 == 1.0f) {
            return;
        }
        translate(f3, f4);
        scale(f, f2);
        translate(-f3, -f4);
    }

    public void rotate(float f) {
        if (f == 0.0f) {
            return;
        }
        nRotate(this.mNativeCanvasWrapper, f);
    }

    public final void rotate(float f, float f2, float f3) {
        if (f == 0.0f) {
            return;
        }
        translate(f2, f3);
        rotate(f);
        translate(-f2, -f3);
    }

    public void skew(float f, float f2) {
        if (f == 0.0f && f2 == 0.0f) {
            return;
        }
        nSkew(this.mNativeCanvasWrapper, f, f2);
    }

    public void concat(Matrix matrix) {
        if (matrix != null) {
            nConcat(this.mNativeCanvasWrapper, matrix.native_instance);
        }
    }

    public void setMatrix(Matrix matrix) {
        nSetMatrix(this.mNativeCanvasWrapper, matrix == null ? 0L : matrix.native_instance);
    }

    @Deprecated
    public void getMatrix(Matrix matrix) {
        nGetMatrix(this.mNativeCanvasWrapper, matrix.native_instance);
    }

    @Deprecated
    public final Matrix getMatrix() {
        Matrix matrix = new Matrix();
        getMatrix(matrix);
        return matrix;
    }

    private static void checkValidClipOp(Region.Op op) {
        if (sCompatiblityVersion >= 28 && op != Region.Op.INTERSECT && op != Region.Op.DIFFERENCE) {
            throw new IllegalArgumentException("Invalid Region.Op - only INTERSECT and DIFFERENCE are allowed");
        }
    }

    @Deprecated
    public boolean clipRect(RectF rectF, Region.Op op) {
        checkValidClipOp(op);
        return nClipRect(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom, op.nativeInt);
    }

    @Deprecated
    public boolean clipRect(Rect rect, Region.Op op) {
        checkValidClipOp(op);
        return nClipRect(this.mNativeCanvasWrapper, rect.left, rect.top, rect.right, rect.bottom, op.nativeInt);
    }

    public boolean clipRectUnion(Rect rect) {
        return nClipRect(this.mNativeCanvasWrapper, rect.left, rect.top, rect.right, rect.bottom, Region.Op.UNION.nativeInt);
    }

    public boolean clipRect(RectF rectF) {
        return nClipRect(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom, Region.Op.INTERSECT.nativeInt);
    }

    public boolean clipOutRect(RectF rectF) {
        return nClipRect(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom, Region.Op.DIFFERENCE.nativeInt);
    }

    public boolean clipRect(Rect rect) {
        return nClipRect(this.mNativeCanvasWrapper, rect.left, rect.top, rect.right, rect.bottom, Region.Op.INTERSECT.nativeInt);
    }

    public boolean clipOutRect(Rect rect) {
        return nClipRect(this.mNativeCanvasWrapper, rect.left, rect.top, rect.right, rect.bottom, Region.Op.DIFFERENCE.nativeInt);
    }

    @Deprecated
    public boolean clipRect(float f, float f2, float f3, float f4, Region.Op op) {
        checkValidClipOp(op);
        return nClipRect(this.mNativeCanvasWrapper, f, f2, f3, f4, op.nativeInt);
    }

    public boolean clipRect(float f, float f2, float f3, float f4) {
        return nClipRect(this.mNativeCanvasWrapper, f, f2, f3, f4, Region.Op.INTERSECT.nativeInt);
    }

    public boolean clipOutRect(float f, float f2, float f3, float f4) {
        return nClipRect(this.mNativeCanvasWrapper, f, f2, f3, f4, Region.Op.DIFFERENCE.nativeInt);
    }

    public boolean clipRect(int i, int i2, int i3, int i4) {
        return nClipRect(this.mNativeCanvasWrapper, i, i2, i3, i4, Region.Op.INTERSECT.nativeInt);
    }

    public boolean clipOutRect(int i, int i2, int i3, int i4) {
        return nClipRect(this.mNativeCanvasWrapper, i, i2, i3, i4, Region.Op.DIFFERENCE.nativeInt);
    }

    @Deprecated
    public boolean clipPath(Path path, Region.Op op) {
        checkValidClipOp(op);
        return nClipPath(this.mNativeCanvasWrapper, path.readOnlyNI(), op.nativeInt);
    }

    public boolean clipPath(Path path) {
        return clipPath(path, Region.Op.INTERSECT);
    }

    public boolean clipOutPath(Path path) {
        return clipPath(path, Region.Op.DIFFERENCE);
    }

    @Deprecated
    public boolean clipRegion(Region region, Region.Op op) {
        return false;
    }

    @Deprecated
    public boolean clipRegion(Region region) {
        return false;
    }

    public DrawFilter getDrawFilter() {
        return this.mDrawFilter;
    }

    public void setDrawFilter(DrawFilter drawFilter) {
        long j;
        if (drawFilter != null) {
            j = drawFilter.mNativeInt;
        } else {
            j = 0;
        }
        this.mDrawFilter = drawFilter;
        nSetDrawFilter(this.mNativeCanvasWrapper, j);
    }

    public enum EdgeType {
        BW(0),
        AA(1);

        public final int nativeInt;

        EdgeType(int i) {
            this.nativeInt = i;
        }
    }

    public boolean quickReject(RectF rectF, EdgeType edgeType) {
        return nQuickReject(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom);
    }

    public boolean quickReject(Path path, EdgeType edgeType) {
        return nQuickReject(this.mNativeCanvasWrapper, path.readOnlyNI());
    }

    public boolean quickReject(float f, float f2, float f3, float f4, EdgeType edgeType) {
        return nQuickReject(this.mNativeCanvasWrapper, f, f2, f3, f4);
    }

    public boolean getClipBounds(Rect rect) {
        return nGetClipBounds(this.mNativeCanvasWrapper, rect);
    }

    public final Rect getClipBounds() {
        Rect rect = new Rect();
        getClipBounds(rect);
        return rect;
    }

    public void drawPicture(Picture picture) {
        picture.endRecording();
        int iSave = save();
        picture.draw(this);
        restoreToCount(iSave);
    }

    public void drawPicture(Picture picture, RectF rectF) {
        save();
        translate(rectF.left, rectF.top);
        if (picture.getWidth() > 0 && picture.getHeight() > 0) {
            scale(rectF.width() / picture.getWidth(), rectF.height() / picture.getHeight());
        }
        drawPicture(picture);
        restore();
    }

    public void drawPicture(Picture picture, Rect rect) {
        save();
        translate(rect.left, rect.top);
        if (picture.getWidth() > 0 && picture.getHeight() > 0) {
            scale(rect.width() / picture.getWidth(), rect.height() / picture.getHeight());
        }
        drawPicture(picture);
        restore();
    }

    public enum VertexMode {
        TRIANGLES(0),
        TRIANGLE_STRIP(1),
        TRIANGLE_FAN(2);

        public final int nativeInt;

        VertexMode(int i) {
            this.nativeInt = i;
        }
    }

    public void release() {
        this.mNativeCanvasWrapper = 0L;
        if (this.mFinalizer != null) {
            this.mFinalizer.run();
            this.mFinalizer = null;
        }
    }

    public static void freeCaches() {
        nFreeCaches();
    }

    public static void freeTextLayoutCaches() {
        nFreeTextLayoutCaches();
    }

    public static void setCompatibilityVersion(int i) {
        sCompatiblityVersion = i;
        nSetCompatibilityVersion(i);
    }

    @Override
    public void drawArc(RectF rectF, float f, float f2, boolean z, Paint paint) {
        super.drawArc(rectF, f, f2, z, paint);
    }

    @Override
    public void drawArc(float f, float f2, float f3, float f4, float f5, float f6, boolean z, Paint paint) {
        super.drawArc(f, f2, f3, f4, f5, f6, z, paint);
    }

    @Override
    public void drawARGB(int i, int i2, int i3, int i4) {
        super.drawARGB(i, i2, i3, i4);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, float f, float f2, Paint paint) {
        super.drawBitmap(bitmap, f, f2, paint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect rect, RectF rectF, Paint paint) {
        super.drawBitmap(bitmap, rect, rectF, paint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect rect, Rect rect2, Paint paint) {
        super.drawBitmap(bitmap, rect, rect2, paint);
    }

    @Override
    @Deprecated
    public void drawBitmap(int[] iArr, int i, int i2, float f, float f2, int i3, int i4, boolean z, Paint paint) {
        super.drawBitmap(iArr, i, i2, f, f2, i3, i4, z, paint);
    }

    @Override
    @Deprecated
    public void drawBitmap(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, boolean z, Paint paint) {
        super.drawBitmap(iArr, i, i2, i3, i4, i5, i6, z, paint);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        super.drawBitmap(bitmap, matrix, paint);
    }

    @Override
    public void drawBitmapMesh(Bitmap bitmap, int i, int i2, float[] fArr, int i3, int[] iArr, int i4, Paint paint) {
        super.drawBitmapMesh(bitmap, i, i2, fArr, i3, iArr, i4, paint);
    }

    @Override
    public void drawCircle(float f, float f2, float f3, Paint paint) {
        super.drawCircle(f, f2, f3, paint);
    }

    @Override
    public void drawColor(int i) {
        super.drawColor(i);
    }

    @Override
    public void drawColor(int i, PorterDuff.Mode mode) {
        super.drawColor(i, mode);
    }

    @Override
    public void drawLine(float f, float f2, float f3, float f4, Paint paint) {
        super.drawLine(f, f2, f3, f4, paint);
    }

    @Override
    public void drawLines(float[] fArr, int i, int i2, Paint paint) {
        super.drawLines(fArr, i, i2, paint);
    }

    @Override
    public void drawLines(float[] fArr, Paint paint) {
        super.drawLines(fArr, paint);
    }

    @Override
    public void drawOval(RectF rectF, Paint paint) {
        super.drawOval(rectF, paint);
    }

    @Override
    public void drawOval(float f, float f2, float f3, float f4, Paint paint) {
        super.drawOval(f, f2, f3, f4, paint);
    }

    @Override
    public void drawPaint(Paint paint) {
        super.drawPaint(paint);
    }

    @Override
    public void drawPatch(NinePatch ninePatch, Rect rect, Paint paint) {
        super.drawPatch(ninePatch, rect, paint);
    }

    @Override
    public void drawPatch(NinePatch ninePatch, RectF rectF, Paint paint) {
        super.drawPatch(ninePatch, rectF, paint);
    }

    @Override
    public void drawPath(Path path, Paint paint) {
        super.drawPath(path, paint);
    }

    @Override
    public void drawPoint(float f, float f2, Paint paint) {
        super.drawPoint(f, f2, paint);
    }

    @Override
    public void drawPoints(float[] fArr, int i, int i2, Paint paint) {
        super.drawPoints(fArr, i, i2, paint);
    }

    @Override
    public void drawPoints(float[] fArr, Paint paint) {
        super.drawPoints(fArr, paint);
    }

    @Override
    @Deprecated
    public void drawPosText(char[] cArr, int i, int i2, float[] fArr, Paint paint) {
        super.drawPosText(cArr, i, i2, fArr, paint);
    }

    @Override
    @Deprecated
    public void drawPosText(String str, float[] fArr, Paint paint) {
        super.drawPosText(str, fArr, paint);
    }

    @Override
    public void drawRect(RectF rectF, Paint paint) {
        super.drawRect(rectF, paint);
    }

    @Override
    public void drawRect(Rect rect, Paint paint) {
        super.drawRect(rect, paint);
    }

    @Override
    public void drawRect(float f, float f2, float f3, float f4, Paint paint) {
        super.drawRect(f, f2, f3, f4, paint);
    }

    @Override
    public void drawRGB(int i, int i2, int i3) {
        super.drawRGB(i, i2, i3);
    }

    @Override
    public void drawRoundRect(RectF rectF, float f, float f2, Paint paint) {
        super.drawRoundRect(rectF, f, f2, paint);
    }

    @Override
    public void drawRoundRect(float f, float f2, float f3, float f4, float f5, float f6, Paint paint) {
        super.drawRoundRect(f, f2, f3, f4, f5, f6, paint);
    }

    @Override
    public void drawText(char[] cArr, int i, int i2, float f, float f2, Paint paint) {
        super.drawText(cArr, i, i2, f, f2, paint);
    }

    @Override
    public void drawText(String str, float f, float f2, Paint paint) {
        super.drawText(str, f, f2, paint);
    }

    @Override
    public void drawText(String str, int i, int i2, float f, float f2, Paint paint) {
        super.drawText(str, i, i2, f, f2, paint);
    }

    @Override
    public void drawText(CharSequence charSequence, int i, int i2, float f, float f2, Paint paint) {
        super.drawText(charSequence, i, i2, f, f2, paint);
    }

    @Override
    public void drawTextOnPath(char[] cArr, int i, int i2, Path path, float f, float f2, Paint paint) {
        super.drawTextOnPath(cArr, i, i2, path, f, f2, paint);
    }

    @Override
    public void drawTextOnPath(String str, Path path, float f, float f2, Paint paint) {
        super.drawTextOnPath(str, path, f, f2, paint);
    }

    @Override
    public void drawTextRun(char[] cArr, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
        super.drawTextRun(cArr, i, i2, i3, i4, f, f2, z, paint);
    }

    @Override
    public void drawTextRun(CharSequence charSequence, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
        super.drawTextRun(charSequence, i, i2, i3, i4, f, f2, z, paint);
    }

    @Override
    public void drawVertices(VertexMode vertexMode, int i, float[] fArr, int i2, float[] fArr2, int i3, int[] iArr, int i4, short[] sArr, int i5, int i6, Paint paint) {
        super.drawVertices(vertexMode, i, fArr, i2, fArr2, i3, iArr, i4, sArr, i5, i6, paint);
    }
}
