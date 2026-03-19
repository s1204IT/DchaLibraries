package android.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.text.GraphicsOperations;
import android.text.PrecomputedText;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;

public abstract class BaseCanvas {
    protected long mNativeCanvasWrapper;
    protected int mScreenDensity = 0;
    protected int mDensity = 0;
    private boolean mAllowHwBitmapsInSwMode = false;

    private static native void nDrawArc(long j, float f, float f2, float f3, float f4, float f5, float f6, boolean z, long j2);

    private static native void nDrawBitmap(long j, Bitmap bitmap, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, long j2, int i, int i2);

    private static native void nDrawBitmap(long j, Bitmap bitmap, float f, float f2, long j2, int i, int i2, int i3);

    private static native void nDrawBitmap(long j, int[] iArr, int i, int i2, float f, float f2, int i3, int i4, boolean z, long j2);

    private static native void nDrawBitmapMatrix(long j, Bitmap bitmap, long j2, long j3);

    private static native void nDrawBitmapMesh(long j, Bitmap bitmap, int i, int i2, float[] fArr, int i3, int[] iArr, int i4, long j2);

    private static native void nDrawCircle(long j, float f, float f2, float f3, long j2);

    private static native void nDrawColor(long j, int i, int i2);

    private static native void nDrawLine(long j, float f, float f2, float f3, float f4, long j2);

    private static native void nDrawLines(long j, float[] fArr, int i, int i2, long j2);

    private static native void nDrawNinePatch(long j, long j2, long j3, float f, float f2, float f3, float f4, long j4, int i, int i2);

    private static native void nDrawOval(long j, float f, float f2, float f3, float f4, long j2);

    private static native void nDrawPaint(long j, long j2);

    private static native void nDrawPath(long j, long j2, long j3);

    private static native void nDrawPoint(long j, float f, float f2, long j2);

    private static native void nDrawPoints(long j, float[] fArr, int i, int i2, long j2);

    private static native void nDrawRect(long j, float f, float f2, float f3, float f4, long j2);

    private static native void nDrawRegion(long j, long j2, long j3);

    private static native void nDrawRoundRect(long j, float f, float f2, float f3, float f4, float f5, float f6, long j2);

    private static native void nDrawText(long j, String str, int i, int i2, float f, float f2, int i3, long j2);

    private static native void nDrawText(long j, char[] cArr, int i, int i2, float f, float f2, int i3, long j2);

    private static native void nDrawTextOnPath(long j, String str, long j2, float f, float f2, int i, long j3);

    private static native void nDrawTextOnPath(long j, char[] cArr, int i, int i2, long j2, float f, float f2, int i3, long j3);

    private static native void nDrawTextRun(long j, String str, int i, int i2, int i3, int i4, float f, float f2, boolean z, long j2);

    private static native void nDrawTextRun(long j, char[] cArr, int i, int i2, int i3, int i4, float f, float f2, boolean z, long j2, long j3);

    private static native void nDrawVertices(long j, int i, int i2, float[] fArr, int i3, float[] fArr2, int i4, int[] iArr, int i5, short[] sArr, int i6, int i7, long j2);

    protected void throwIfCannotDraw(Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            throw new RuntimeException("Canvas: trying to use a recycled bitmap " + bitmap);
        }
        if (!bitmap.isPremultiplied() && bitmap.getConfig() == Bitmap.Config.ARGB_8888 && bitmap.hasAlpha()) {
            throw new RuntimeException("Canvas: trying to use a non-premultiplied bitmap " + bitmap);
        }
        throwIfHwBitmapInSwMode(bitmap);
    }

    protected static final void checkRange(int i, int i2, int i3) {
        if ((i2 | i3) < 0 || i2 + i3 > i) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public boolean isHardwareAccelerated() {
        return false;
    }

    public void drawArc(float f, float f2, float f3, float f4, float f5, float f6, boolean z, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawArc(this.mNativeCanvasWrapper, f, f2, f3, f4, f5, f6, z, paint.getNativeInstance());
    }

    public void drawArc(RectF rectF, float f, float f2, boolean z, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawArc(rectF.left, rectF.top, rectF.right, rectF.bottom, f, f2, z, paint);
    }

    public void drawARGB(int i, int i2, int i3, int i4) {
        drawColor(Color.argb(i, i2, i3, i4));
    }

    public void drawBitmap(Bitmap bitmap, float f, float f2, Paint paint) {
        throwIfCannotDraw(bitmap);
        throwIfHasHwBitmapInSwMode(paint);
        nDrawBitmap(this.mNativeCanvasWrapper, bitmap, f, f2, paint != null ? paint.getNativeInstance() : 0L, this.mDensity, this.mScreenDensity, bitmap.mDensity);
    }

    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawBitmapMatrix(this.mNativeCanvasWrapper, bitmap, matrix.ni(), paint != null ? paint.getNativeInstance() : 0L);
    }

    public void drawBitmap(Bitmap bitmap, Rect rect, Rect rect2, Paint paint) {
        int i;
        int height;
        int i2;
        int width;
        if (rect2 == null) {
            throw new NullPointerException();
        }
        throwIfCannotDraw(bitmap);
        throwIfHasHwBitmapInSwMode(paint);
        long nativeInstance = paint == null ? 0L : paint.getNativeInstance();
        if (rect == null) {
            i2 = 0;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            i = 0;
        } else {
            int i3 = rect.left;
            int i4 = rect.right;
            i = rect.top;
            height = rect.bottom;
            i2 = i3;
            width = i4;
        }
        nDrawBitmap(this.mNativeCanvasWrapper, bitmap, i2, i, width, height, rect2.left, rect2.top, rect2.right, rect2.bottom, nativeInstance, this.mScreenDensity, bitmap.mDensity);
    }

    public void drawBitmap(Bitmap bitmap, Rect rect, RectF rectF, Paint paint) {
        float height;
        float f;
        float width;
        float f2;
        if (rectF == null) {
            throw new NullPointerException();
        }
        throwIfCannotDraw(bitmap);
        throwIfHasHwBitmapInSwMode(paint);
        long nativeInstance = paint == null ? 0L : paint.getNativeInstance();
        if (rect != null) {
            float f3 = rect.left;
            float f4 = rect.right;
            float f5 = rect.top;
            height = rect.bottom;
            f = f3;
            width = f4;
            f2 = f5;
        } else {
            f = 0.0f;
            f2 = 0.0f;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }
        nDrawBitmap(this.mNativeCanvasWrapper, bitmap, f, f2, width, height, rectF.left, rectF.top, rectF.right, rectF.bottom, nativeInstance, this.mScreenDensity, bitmap.mDensity);
    }

    @Deprecated
    public void drawBitmap(int[] iArr, int i, int i2, float f, float f2, int i3, int i4, boolean z, Paint paint) {
        if (i3 < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (i4 < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (Math.abs(i2) < i3) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int i5 = ((i4 - 1) * i2) + i;
        int length = iArr.length;
        if (i < 0 || i + i3 > length || i5 < 0 || i5 + i3 > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        if (i3 == 0 || i4 == 0) {
            return;
        }
        nDrawBitmap(this.mNativeCanvasWrapper, iArr, i, i2, f, f2, i3, i4, z, paint != null ? paint.getNativeInstance() : 0L);
    }

    @Deprecated
    public void drawBitmap(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, boolean z, Paint paint) {
        drawBitmap(iArr, i, i2, i3, i4, i5, i6, z, paint);
    }

    public void drawBitmapMesh(Bitmap bitmap, int i, int i2, float[] fArr, int i3, int[] iArr, int i4, Paint paint) {
        if ((i | i2 | i3 | i4) < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        if (i == 0 || i2 == 0) {
            return;
        }
        int i5 = (i + 1) * (i2 + 1);
        checkRange(fArr.length, i3, i5 * 2);
        if (iArr != null) {
            checkRange(iArr.length, i4, i5);
        }
        nDrawBitmapMesh(this.mNativeCanvasWrapper, bitmap, i, i2, fArr, i3, iArr, i4, paint != null ? paint.getNativeInstance() : 0L);
    }

    public void drawCircle(float f, float f2, float f3, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawCircle(this.mNativeCanvasWrapper, f, f2, f3, paint.getNativeInstance());
    }

    public void drawColor(int i) {
        nDrawColor(this.mNativeCanvasWrapper, i, PorterDuff.Mode.SRC_OVER.nativeInt);
    }

    public void drawColor(int i, PorterDuff.Mode mode) {
        nDrawColor(this.mNativeCanvasWrapper, i, mode.nativeInt);
    }

    public void drawLine(float f, float f2, float f3, float f4, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawLine(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    public void drawLines(float[] fArr, int i, int i2, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawLines(this.mNativeCanvasWrapper, fArr, i, i2, paint.getNativeInstance());
    }

    public void drawLines(float[] fArr, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawLines(fArr, 0, fArr.length, paint);
    }

    public void drawOval(float f, float f2, float f3, float f4, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawOval(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    public void drawOval(RectF rectF, Paint paint) {
        if (rectF == null) {
            throw new NullPointerException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        drawOval(rectF.left, rectF.top, rectF.right, rectF.bottom, paint);
    }

    public void drawPaint(Paint paint) {
        nDrawPaint(this.mNativeCanvasWrapper, paint.getNativeInstance());
    }

    public void drawPatch(NinePatch ninePatch, Rect rect, Paint paint) {
        Bitmap bitmap = ninePatch.getBitmap();
        throwIfCannotDraw(bitmap);
        throwIfHasHwBitmapInSwMode(paint);
        nDrawNinePatch(this.mNativeCanvasWrapper, bitmap.getNativeInstance(), ninePatch.mNativeChunk, rect.left, rect.top, rect.right, rect.bottom, paint == null ? 0L : paint.getNativeInstance(), this.mDensity, ninePatch.getDensity());
    }

    public void drawPatch(NinePatch ninePatch, RectF rectF, Paint paint) {
        Bitmap bitmap = ninePatch.getBitmap();
        throwIfCannotDraw(bitmap);
        throwIfHasHwBitmapInSwMode(paint);
        nDrawNinePatch(this.mNativeCanvasWrapper, bitmap.getNativeInstance(), ninePatch.mNativeChunk, rectF.left, rectF.top, rectF.right, rectF.bottom, paint == null ? 0L : paint.getNativeInstance(), this.mDensity, ninePatch.getDensity());
    }

    public void drawPath(Path path, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        if (path.isSimplePath && path.rects != null) {
            nDrawRegion(this.mNativeCanvasWrapper, path.rects.mNativeRegion, paint.getNativeInstance());
        } else {
            nDrawPath(this.mNativeCanvasWrapper, path.readOnlyNI(), paint.getNativeInstance());
        }
    }

    public void drawPoint(float f, float f2, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawPoint(this.mNativeCanvasWrapper, f, f2, paint.getNativeInstance());
    }

    public void drawPoints(float[] fArr, int i, int i2, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawPoints(this.mNativeCanvasWrapper, fArr, i, i2, paint.getNativeInstance());
    }

    public void drawPoints(float[] fArr, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawPoints(fArr, 0, fArr.length, paint);
    }

    @Deprecated
    public void drawPosText(char[] cArr, int i, int i2, float[] fArr, Paint paint) {
        if (i < 0 || i + i2 > cArr.length || i2 * 2 > fArr.length) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = i3 * 2;
            drawText(cArr, i + i3, 1, fArr[i4], fArr[i4 + 1], paint);
        }
    }

    @Deprecated
    public void drawPosText(String str, float[] fArr, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawPosText(str.toCharArray(), 0, str.length(), fArr, paint);
    }

    public void drawRect(float f, float f2, float f3, float f4, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawRect(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    public void drawRect(Rect rect, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawRect(RectF rectF, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawRect(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom, paint.getNativeInstance());
    }

    public void drawRGB(int i, int i2, int i3) {
        drawColor(Color.rgb(i, i2, i3));
    }

    public void drawRoundRect(float f, float f2, float f3, float f4, float f5, float f6, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawRoundRect(this.mNativeCanvasWrapper, f, f2, f3, f4, f5, f6, paint.getNativeInstance());
    }

    public void drawRoundRect(RectF rectF, float f, float f2, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        drawRoundRect(rectF.left, rectF.top, rectF.right, rectF.bottom, f, f2, paint);
    }

    public void drawText(char[] cArr, int i, int i2, float f, float f2, Paint paint) {
        if ((i | i2 | (i + i2) | ((cArr.length - i) - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        nDrawText(this.mNativeCanvasWrapper, cArr, i, i2, f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    public void drawText(CharSequence charSequence, int i, int i2, float f, float f2, Paint paint) {
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        if ((charSequence instanceof String) || (charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            nDrawText(this.mNativeCanvasWrapper, charSequence.toString(), i, i2, f, f2, paint.mBidiFlags, paint.getNativeInstance());
            return;
        }
        if (charSequence instanceof GraphicsOperations) {
            ((GraphicsOperations) charSequence).drawText(this, i, i2, f, f2, paint);
            return;
        }
        char[] cArrObtain = TemporaryBuffer.obtain(i3);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        nDrawText(this.mNativeCanvasWrapper, cArrObtain, 0, i3, f, f2, paint.mBidiFlags, paint.getNativeInstance());
        TemporaryBuffer.recycle(cArrObtain);
    }

    public void drawText(String str, float f, float f2, Paint paint) {
        throwIfHasHwBitmapInSwMode(paint);
        nDrawText(this.mNativeCanvasWrapper, str, 0, str.length(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    public void drawText(String str, int i, int i2, float f, float f2, Paint paint) {
        if ((i | i2 | (i2 - i) | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        nDrawText(this.mNativeCanvasWrapper, str, i, i2, f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    public void drawTextOnPath(char[] cArr, int i, int i2, Path path, float f, float f2, Paint paint) {
        if (i < 0 || i + i2 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        nDrawTextOnPath(this.mNativeCanvasWrapper, cArr, i, i2, path.readOnlyNI(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    public void drawTextOnPath(String str, Path path, float f, float f2, Paint paint) {
        if (str.length() > 0) {
            throwIfHasHwBitmapInSwMode(paint);
            nDrawTextOnPath(this.mNativeCanvasWrapper, str, path.readOnlyNI(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
        }
    }

    public void drawTextRun(char[] cArr, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
        if (cArr == null) {
            throw new NullPointerException("text is null");
        }
        if (paint == null) {
            throw new NullPointerException("paint is null");
        }
        int i5 = i3 + i4;
        if ((i | i2 | i3 | i4 | (i - i3) | (i5 - (i + i2)) | (cArr.length - i5)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        nDrawTextRun(this.mNativeCanvasWrapper, cArr, i, i2, i3, i4, f, f2, z, paint.getNativeInstance(), 0L);
    }

    public void drawTextRun(CharSequence charSequence, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
        long nativePtr;
        if (charSequence == null) {
            throw new NullPointerException("text is null");
        }
        if (paint == null) {
            throw new NullPointerException("paint is null");
        }
        int i5 = i - i3;
        int i6 = i2 - i;
        if ((i | i2 | i3 | i4 | i5 | i6 | (i4 - i2) | (charSequence.length() - i4)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        throwIfHasHwBitmapInSwMode(paint);
        if ((charSequence instanceof String) || (charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            nDrawTextRun(this.mNativeCanvasWrapper, charSequence.toString(), i, i2, i3, i4, f, f2, z, paint.getNativeInstance());
            return;
        }
        if (charSequence instanceof GraphicsOperations) {
            ((GraphicsOperations) charSequence).drawTextRun(this, i, i2, i3, i4, f, f2, z, paint);
            return;
        }
        int i7 = i4 - i3;
        char[] cArrObtain = TemporaryBuffer.obtain(i7);
        TextUtils.getChars(charSequence, i3, i4, cArrObtain, 0);
        if (charSequence instanceof PrecomputedText) {
            PrecomputedText precomputedText = (PrecomputedText) charSequence;
            int iFindParaIndex = precomputedText.findParaIndex(i);
            if (i2 > precomputedText.getParagraphEnd(iFindParaIndex)) {
                nativePtr = 0;
            } else {
                nativePtr = precomputedText.getMeasuredParagraph(iFindParaIndex).getNativePtr();
            }
        }
        nDrawTextRun(this.mNativeCanvasWrapper, cArrObtain, i5, i6, 0, i7, f, f2, z, paint.getNativeInstance(), nativePtr);
        TemporaryBuffer.recycle(cArrObtain);
    }

    public void drawVertices(Canvas.VertexMode vertexMode, int i, float[] fArr, int i2, float[] fArr2, int i3, int[] iArr, int i4, short[] sArr, int i5, int i6, Paint paint) {
        int i7;
        int i8;
        int i9;
        int i10;
        checkRange(fArr.length, i2, i);
        if (isHardwareAccelerated()) {
            return;
        }
        if (fArr2 != null) {
            i7 = i3;
            checkRange(fArr2.length, i7, i);
        } else {
            i7 = i3;
        }
        if (iArr != null) {
            i8 = i4;
            checkRange(iArr.length, i8, i / 2);
        } else {
            i8 = i4;
        }
        if (sArr != null) {
            i9 = i5;
            i10 = i6;
            checkRange(sArr.length, i9, i10);
        } else {
            i9 = i5;
            i10 = i6;
        }
        throwIfHasHwBitmapInSwMode(paint);
        nDrawVertices(this.mNativeCanvasWrapper, vertexMode.nativeInt, i, fArr, i2, fArr2, i7, iArr, i8, sArr, i9, i10, paint.getNativeInstance());
    }

    public void setHwBitmapsInSwModeEnabled(boolean z) {
        this.mAllowHwBitmapsInSwMode = z;
    }

    public boolean isHwBitmapsInSwModeEnabled() {
        return this.mAllowHwBitmapsInSwMode;
    }

    protected void onHwBitmapInSwMode() {
        if (!this.mAllowHwBitmapsInSwMode) {
            throw new IllegalArgumentException("Software rendering doesn't support hardware bitmaps");
        }
    }

    private void throwIfHwBitmapInSwMode(Bitmap bitmap) {
        if (!isHardwareAccelerated() && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            onHwBitmapInSwMode();
        }
    }

    private void throwIfHasHwBitmapInSwMode(Paint paint) {
        if (isHardwareAccelerated() || paint == null) {
            return;
        }
        throwIfHasHwBitmapInSwMode(paint.getShader());
    }

    private void throwIfHasHwBitmapInSwMode(Shader shader) {
        if (shader == null) {
            return;
        }
        if (shader instanceof BitmapShader) {
            throwIfHwBitmapInSwMode(((BitmapShader) shader).mBitmap);
        }
        if (shader instanceof ComposeShader) {
            ComposeShader composeShader = (ComposeShader) shader;
            throwIfHasHwBitmapInSwMode(composeShader.mShaderA);
            throwIfHasHwBitmapInSwMode(composeShader.mShaderB);
        }
    }
}
