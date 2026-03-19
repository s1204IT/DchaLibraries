package android.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.TemporaryBuffer;
import android.text.GraphicsOperations;
import android.text.PrecomputedText;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import dalvik.annotation.optimization.FastNative;

public class RecordingCanvas extends Canvas {
    @FastNative
    private static native void nDrawArc(long j, float f, float f2, float f3, float f4, float f5, float f6, boolean z, long j2);

    @FastNative
    private static native void nDrawBitmap(long j, Bitmap bitmap, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, long j2, int i, int i2);

    @FastNative
    private static native void nDrawBitmap(long j, Bitmap bitmap, float f, float f2, long j2, int i, int i2, int i3);

    @FastNative
    private static native void nDrawBitmap(long j, int[] iArr, int i, int i2, float f, float f2, int i3, int i4, boolean z, long j2);

    @FastNative
    private static native void nDrawBitmapMatrix(long j, Bitmap bitmap, long j2, long j3);

    @FastNative
    private static native void nDrawBitmapMesh(long j, Bitmap bitmap, int i, int i2, float[] fArr, int i3, int[] iArr, int i4, long j2);

    @FastNative
    private static native void nDrawCircle(long j, float f, float f2, float f3, long j2);

    @FastNative
    private static native void nDrawColor(long j, int i, int i2);

    @FastNative
    private static native void nDrawLine(long j, float f, float f2, float f3, float f4, long j2);

    @FastNative
    private static native void nDrawLines(long j, float[] fArr, int i, int i2, long j2);

    @FastNative
    private static native void nDrawNinePatch(long j, long j2, long j3, float f, float f2, float f3, float f4, long j4, int i, int i2);

    @FastNative
    private static native void nDrawOval(long j, float f, float f2, float f3, float f4, long j2);

    @FastNative
    private static native void nDrawPaint(long j, long j2);

    @FastNative
    private static native void nDrawPath(long j, long j2, long j3);

    @FastNative
    private static native void nDrawPoint(long j, float f, float f2, long j2);

    @FastNative
    private static native void nDrawPoints(long j, float[] fArr, int i, int i2, long j2);

    @FastNative
    private static native void nDrawRect(long j, float f, float f2, float f3, float f4, long j2);

    @FastNative
    private static native void nDrawRegion(long j, long j2, long j3);

    @FastNative
    private static native void nDrawRoundRect(long j, float f, float f2, float f3, float f4, float f5, float f6, long j2);

    @FastNative
    private static native void nDrawText(long j, String str, int i, int i2, float f, float f2, int i3, long j2);

    @FastNative
    private static native void nDrawText(long j, char[] cArr, int i, int i2, float f, float f2, int i3, long j2);

    @FastNative
    private static native void nDrawTextOnPath(long j, String str, long j2, float f, float f2, int i, long j3);

    @FastNative
    private static native void nDrawTextOnPath(long j, char[] cArr, int i, int i2, long j2, float f, float f2, int i3, long j3);

    @FastNative
    private static native void nDrawTextRun(long j, String str, int i, int i2, int i3, int i4, float f, float f2, boolean z, long j2);

    @FastNative
    private static native void nDrawTextRun(long j, char[] cArr, int i, int i2, int i3, int i4, float f, float f2, boolean z, long j2, long j3);

    @FastNative
    private static native void nDrawVertices(long j, int i, int i2, float[] fArr, int i3, float[] fArr2, int i4, int[] iArr, int i5, short[] sArr, int i6, int i7, long j2);

    public RecordingCanvas(long j) {
        super(j);
    }

    @Override
    public final void drawArc(float f, float f2, float f3, float f4, float f5, float f6, boolean z, Paint paint) {
        nDrawArc(this.mNativeCanvasWrapper, f, f2, f3, f4, f5, f6, z, paint.getNativeInstance());
    }

    @Override
    public final void drawArc(RectF rectF, float f, float f2, boolean z, Paint paint) {
        drawArc(rectF.left, rectF.top, rectF.right, rectF.bottom, f, f2, z, paint);
    }

    @Override
    public final void drawARGB(int i, int i2, int i3, int i4) {
        drawColor(Color.argb(i, i2, i3, i4));
    }

    @Override
    public final void drawBitmap(Bitmap bitmap, float f, float f2, Paint paint) {
        throwIfCannotDraw(bitmap);
        nDrawBitmap(this.mNativeCanvasWrapper, bitmap, f, f2, paint != null ? paint.getNativeInstance() : 0L, this.mDensity, this.mScreenDensity, bitmap.mDensity);
    }

    @Override
    public final void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        nDrawBitmapMatrix(this.mNativeCanvasWrapper, bitmap, matrix.ni(), paint != null ? paint.getNativeInstance() : 0L);
    }

    @Override
    public final void drawBitmap(Bitmap bitmap, Rect rect, Rect rect2, Paint paint) {
        int i;
        int height;
        int i2;
        int width;
        if (rect2 == null) {
            throw new NullPointerException();
        }
        throwIfCannotDraw(bitmap);
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

    @Override
    public final void drawBitmap(Bitmap bitmap, Rect rect, RectF rectF, Paint paint) {
        float height;
        float f;
        float width;
        float f2;
        if (rectF == null) {
            throw new NullPointerException();
        }
        throwIfCannotDraw(bitmap);
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

    @Override
    @Deprecated
    public final void drawBitmap(int[] iArr, int i, int i2, float f, float f2, int i3, int i4, boolean z, Paint paint) {
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
        if (i3 != 0 && i4 != 0) {
            nDrawBitmap(this.mNativeCanvasWrapper, iArr, i, i2, f, f2, i3, i4, z, paint != null ? paint.getNativeInstance() : 0L);
        }
    }

    @Override
    @Deprecated
    public final void drawBitmap(int[] iArr, int i, int i2, int i3, int i4, int i5, int i6, boolean z, Paint paint) {
        drawBitmap(iArr, i, i2, i3, i4, i5, i6, z, paint);
    }

    @Override
    public final void drawBitmapMesh(Bitmap bitmap, int i, int i2, float[] fArr, int i3, int[] iArr, int i4, Paint paint) {
        if ((i | i2 | i3 | i4) < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
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

    @Override
    public final void drawCircle(float f, float f2, float f3, Paint paint) {
        nDrawCircle(this.mNativeCanvasWrapper, f, f2, f3, paint.getNativeInstance());
    }

    @Override
    public final void drawColor(int i) {
        nDrawColor(this.mNativeCanvasWrapper, i, PorterDuff.Mode.SRC_OVER.nativeInt);
    }

    @Override
    public final void drawColor(int i, PorterDuff.Mode mode) {
        nDrawColor(this.mNativeCanvasWrapper, i, mode.nativeInt);
    }

    @Override
    public final void drawLine(float f, float f2, float f3, float f4, Paint paint) {
        nDrawLine(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    @Override
    public final void drawLines(float[] fArr, int i, int i2, Paint paint) {
        nDrawLines(this.mNativeCanvasWrapper, fArr, i, i2, paint.getNativeInstance());
    }

    @Override
    public final void drawLines(float[] fArr, Paint paint) {
        drawLines(fArr, 0, fArr.length, paint);
    }

    @Override
    public final void drawOval(float f, float f2, float f3, float f4, Paint paint) {
        nDrawOval(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    @Override
    public final void drawOval(RectF rectF, Paint paint) {
        if (rectF == null) {
            throw new NullPointerException();
        }
        drawOval(rectF.left, rectF.top, rectF.right, rectF.bottom, paint);
    }

    @Override
    public final void drawPaint(Paint paint) {
        nDrawPaint(this.mNativeCanvasWrapper, paint.getNativeInstance());
    }

    @Override
    public final void drawPatch(NinePatch ninePatch, Rect rect, Paint paint) {
        Bitmap bitmap = ninePatch.getBitmap();
        throwIfCannotDraw(bitmap);
        nDrawNinePatch(this.mNativeCanvasWrapper, bitmap.getNativeInstance(), ninePatch.mNativeChunk, rect.left, rect.top, rect.right, rect.bottom, paint == null ? 0L : paint.getNativeInstance(), this.mDensity, ninePatch.getDensity());
    }

    @Override
    public final void drawPatch(NinePatch ninePatch, RectF rectF, Paint paint) {
        Bitmap bitmap = ninePatch.getBitmap();
        throwIfCannotDraw(bitmap);
        nDrawNinePatch(this.mNativeCanvasWrapper, bitmap.getNativeInstance(), ninePatch.mNativeChunk, rectF.left, rectF.top, rectF.right, rectF.bottom, paint == null ? 0L : paint.getNativeInstance(), this.mDensity, ninePatch.getDensity());
    }

    @Override
    public final void drawPath(Path path, Paint paint) {
        if (path.isSimplePath && path.rects != null) {
            nDrawRegion(this.mNativeCanvasWrapper, path.rects.mNativeRegion, paint.getNativeInstance());
        } else {
            nDrawPath(this.mNativeCanvasWrapper, path.readOnlyNI(), paint.getNativeInstance());
        }
    }

    @Override
    public final void drawPicture(Picture picture) {
        picture.endRecording();
        int iSave = save();
        picture.draw(this);
        restoreToCount(iSave);
    }

    @Override
    public final void drawPicture(Picture picture, Rect rect) {
        save();
        translate(rect.left, rect.top);
        if (picture.getWidth() > 0 && picture.getHeight() > 0) {
            scale(rect.width() / picture.getWidth(), rect.height() / picture.getHeight());
        }
        drawPicture(picture);
        restore();
    }

    @Override
    public final void drawPicture(Picture picture, RectF rectF) {
        save();
        translate(rectF.left, rectF.top);
        if (picture.getWidth() > 0 && picture.getHeight() > 0) {
            scale(rectF.width() / picture.getWidth(), rectF.height() / picture.getHeight());
        }
        drawPicture(picture);
        restore();
    }

    @Override
    public final void drawPoint(float f, float f2, Paint paint) {
        nDrawPoint(this.mNativeCanvasWrapper, f, f2, paint.getNativeInstance());
    }

    @Override
    public final void drawPoints(float[] fArr, int i, int i2, Paint paint) {
        nDrawPoints(this.mNativeCanvasWrapper, fArr, i, i2, paint.getNativeInstance());
    }

    @Override
    public final void drawPoints(float[] fArr, Paint paint) {
        drawPoints(fArr, 0, fArr.length, paint);
    }

    @Override
    @Deprecated
    public final void drawPosText(char[] cArr, int i, int i2, float[] fArr, Paint paint) {
        if (i < 0 || i + i2 > cArr.length || i2 * 2 > fArr.length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = i3 * 2;
            drawText(cArr, i + i3, 1, fArr[i4], fArr[i4 + 1], paint);
        }
    }

    @Override
    @Deprecated
    public final void drawPosText(String str, float[] fArr, Paint paint) {
        drawPosText(str.toCharArray(), 0, str.length(), fArr, paint);
    }

    @Override
    public final void drawRect(float f, float f2, float f3, float f4, Paint paint) {
        nDrawRect(this.mNativeCanvasWrapper, f, f2, f3, f4, paint.getNativeInstance());
    }

    @Override
    public final void drawRect(Rect rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    @Override
    public final void drawRect(RectF rectF, Paint paint) {
        nDrawRect(this.mNativeCanvasWrapper, rectF.left, rectF.top, rectF.right, rectF.bottom, paint.getNativeInstance());
    }

    @Override
    public final void drawRGB(int i, int i2, int i3) {
        drawColor(Color.rgb(i, i2, i3));
    }

    @Override
    public final void drawRoundRect(float f, float f2, float f3, float f4, float f5, float f6, Paint paint) {
        nDrawRoundRect(this.mNativeCanvasWrapper, f, f2, f3, f4, f5, f6, paint.getNativeInstance());
    }

    @Override
    public final void drawRoundRect(RectF rectF, float f, float f2, Paint paint) {
        drawRoundRect(rectF.left, rectF.top, rectF.right, rectF.bottom, f, f2, paint);
    }

    @Override
    public final void drawText(char[] cArr, int i, int i2, float f, float f2, Paint paint) {
        if ((i | i2 | (i + i2) | ((cArr.length - i) - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        nDrawText(this.mNativeCanvasWrapper, cArr, i, i2, f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    @Override
    public final void drawText(CharSequence charSequence, int i, int i2, float f, float f2, Paint paint) {
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
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

    @Override
    public final void drawText(String str, float f, float f2, Paint paint) {
        nDrawText(this.mNativeCanvasWrapper, str, 0, str.length(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    @Override
    public final void drawText(String str, int i, int i2, float f, float f2, Paint paint) {
        if ((i | i2 | (i2 - i) | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        nDrawText(this.mNativeCanvasWrapper, str, i, i2, f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    @Override
    public final void drawTextOnPath(char[] cArr, int i, int i2, Path path, float f, float f2, Paint paint) {
        if (i < 0 || i + i2 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        nDrawTextOnPath(this.mNativeCanvasWrapper, cArr, i, i2, path.readOnlyNI(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
    }

    @Override
    public final void drawTextOnPath(String str, Path path, float f, float f2, Paint paint) {
        if (str.length() > 0) {
            nDrawTextOnPath(this.mNativeCanvasWrapper, str, path.readOnlyNI(), f, f2, paint.mBidiFlags, paint.getNativeInstance());
        }
    }

    @Override
    public final void drawTextRun(char[] cArr, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
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
        nDrawTextRun(this.mNativeCanvasWrapper, cArr, i, i2, i3, i4, f, f2, z, paint.getNativeInstance(), 0L);
    }

    @Override
    public final void drawTextRun(CharSequence charSequence, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
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

    @Override
    public final void drawVertices(Canvas.VertexMode vertexMode, int i, float[] fArr, int i2, float[] fArr2, int i3, int[] iArr, int i4, short[] sArr, int i5, int i6, Paint paint) {
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
        nDrawVertices(this.mNativeCanvasWrapper, vertexMode.nativeInt, i, fArr, i2, fArr2, i7, iArr, i8, sArr, i9, i10, paint.getNativeInstance());
    }
}
