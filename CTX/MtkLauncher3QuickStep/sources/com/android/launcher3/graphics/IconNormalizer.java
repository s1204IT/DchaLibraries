package com.android.launcher3.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.view.ViewCompat;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.dragndrop.FolderAdaptiveIcon;
import java.nio.ByteBuffer;

public class IconNormalizer {
    private static final float BOUND_RATIO_MARGIN = 0.05f;
    private static final float CIRCLE_AREA_BY_RECT = 0.7853982f;
    private static final boolean DEBUG = false;
    public static final float ICON_VISIBLE_AREA_FACTOR = 0.92f;
    private static final float LINEAR_SCALE_SLOPE = 0.040449437f;
    private static final float MAX_CIRCLE_AREA_FACTOR = 0.6597222f;
    private static final float MAX_SQUARE_AREA_FACTOR = 0.6510417f;
    private static final int MIN_VISIBLE_ALPHA = 40;
    private static final float PIXEL_DIFF_PERCENTAGE_THRESHOLD = 0.005f;
    private static final float SCALE_NOT_INITIALIZED = 0.0f;
    private static final String TAG = "IconNormalizer";
    private float mAdaptiveIconScale;
    private final Bitmap mBitmap;
    private final Canvas mCanvas;
    private final float[] mLeftBorder;
    private final Matrix mMatrix;
    private final int mMaxSize;
    private final Paint mPaintMaskShapeOutline;
    private final byte[] mPixels;
    private final float[] mRightBorder;
    private final Path mShapePath;
    private final Rect mBounds = new Rect();
    private final Rect mAdaptiveIconBounds = new Rect();
    private final Paint mPaintMaskShape = new Paint();

    IconNormalizer(Context context) {
        this.mMaxSize = LauncherAppState.getIDP(context).iconBitmapSize * 2;
        this.mBitmap = Bitmap.createBitmap(this.mMaxSize, this.mMaxSize, Bitmap.Config.ALPHA_8);
        this.mCanvas = new Canvas(this.mBitmap);
        this.mPixels = new byte[this.mMaxSize * this.mMaxSize];
        this.mLeftBorder = new float[this.mMaxSize];
        this.mRightBorder = new float[this.mMaxSize];
        this.mPaintMaskShape.setColor(SupportMenu.CATEGORY_MASK);
        this.mPaintMaskShape.setStyle(Paint.Style.FILL);
        this.mPaintMaskShape.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        this.mPaintMaskShapeOutline = new Paint();
        this.mPaintMaskShapeOutline.setStrokeWidth(2.0f * context.getResources().getDisplayMetrics().density);
        this.mPaintMaskShapeOutline.setStyle(Paint.Style.STROKE);
        this.mPaintMaskShapeOutline.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.mPaintMaskShapeOutline.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mShapePath = new Path();
        this.mMatrix = new Matrix();
        this.mAdaptiveIconScale = 0.0f;
    }

    private boolean isShape(Path path) {
        if (Math.abs((this.mBounds.width() / this.mBounds.height()) - 1.0f) > BOUND_RATIO_MARGIN) {
            return false;
        }
        this.mMatrix.reset();
        this.mMatrix.setScale(this.mBounds.width(), this.mBounds.height());
        this.mMatrix.postTranslate(this.mBounds.left, this.mBounds.top);
        path.transform(this.mMatrix, this.mShapePath);
        this.mCanvas.drawPath(this.mShapePath, this.mPaintMaskShape);
        this.mCanvas.drawPath(this.mShapePath, this.mPaintMaskShapeOutline);
        return isTransparentBitmap();
    }

    private boolean isTransparentBitmap() {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(this.mPixels);
        byteBufferWrap.rewind();
        this.mBitmap.copyPixelsToBuffer(byteBufferWrap);
        int i = this.mBounds.top;
        int i2 = this.mMaxSize * i;
        int i3 = this.mMaxSize - this.mBounds.right;
        int i4 = 0;
        while (i < this.mBounds.bottom) {
            int i5 = i2 + this.mBounds.left;
            for (int i6 = this.mBounds.left; i6 < this.mBounds.right; i6++) {
                if ((this.mPixels[i5] & 255) > 40) {
                    i4++;
                }
                i5++;
            }
            i2 = i5 + i3;
            i++;
        }
        return ((float) i4) / ((float) (this.mBounds.width() * this.mBounds.height())) < PIXEL_DIFF_PERCENTAGE_THRESHOLD;
    }

    public synchronized float getScale(@NonNull Drawable drawable, @Nullable RectF rectF, @Nullable Path path, @Nullable boolean[] zArr) {
        float f;
        Drawable adaptiveIconDrawable = drawable;
        synchronized (this) {
            if (Utilities.ATLEAST_OREO && (adaptiveIconDrawable instanceof AdaptiveIconDrawable)) {
                if (this.mAdaptiveIconScale != 0.0f) {
                    if (rectF != null) {
                        rectF.set(this.mAdaptiveIconBounds);
                    }
                    return this.mAdaptiveIconScale;
                }
                if (adaptiveIconDrawable instanceof FolderAdaptiveIcon) {
                    adaptiveIconDrawable = new AdaptiveIconDrawable(new ColorDrawable(ViewCompat.MEASURED_STATE_MASK), null);
                }
            }
            int intrinsicWidth = adaptiveIconDrawable.getIntrinsicWidth();
            int intrinsicHeight = adaptiveIconDrawable.getIntrinsicHeight();
            if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                if (intrinsicWidth <= 0 || intrinsicWidth > this.mMaxSize) {
                    intrinsicWidth = this.mMaxSize;
                }
                if (intrinsicHeight <= 0 || intrinsicHeight > this.mMaxSize) {
                    intrinsicHeight = this.mMaxSize;
                }
            } else if (intrinsicWidth > this.mMaxSize || intrinsicHeight > this.mMaxSize) {
                int iMax = Math.max(intrinsicWidth, intrinsicHeight);
                intrinsicWidth = (this.mMaxSize * intrinsicWidth) / iMax;
                intrinsicHeight = (this.mMaxSize * intrinsicHeight) / iMax;
            }
            int i = 0;
            this.mBitmap.eraseColor(0);
            adaptiveIconDrawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
            adaptiveIconDrawable.draw(this.mCanvas);
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(this.mPixels);
            byteBufferWrap.rewind();
            this.mBitmap.copyPixelsToBuffer(byteBufferWrap);
            int i2 = this.mMaxSize + 1;
            int i3 = this.mMaxSize - intrinsicWidth;
            int i4 = i2;
            int i5 = 0;
            int i6 = 0;
            int i7 = -1;
            int iMax2 = -1;
            int i8 = -1;
            while (i5 < intrinsicHeight) {
                int i9 = i6;
                int i10 = -1;
                int i11 = i;
                int i12 = -1;
                while (i11 < intrinsicWidth) {
                    Drawable drawable2 = adaptiveIconDrawable;
                    if ((this.mPixels[i9] & 255) > 40) {
                        if (i12 == -1) {
                            i12 = i11;
                        }
                        i10 = i11;
                    }
                    i9++;
                    i11++;
                    adaptiveIconDrawable = drawable2;
                }
                Drawable drawable3 = adaptiveIconDrawable;
                i6 = i9 + i3;
                this.mLeftBorder[i5] = i12;
                this.mRightBorder[i5] = i10;
                if (i12 != -1) {
                    if (i7 == -1) {
                        i7 = i5;
                    }
                    int iMin = Math.min(i4, i12);
                    iMax2 = Math.max(iMax2, i10);
                    i8 = i5;
                    i4 = iMin;
                }
                i5++;
                adaptiveIconDrawable = drawable3;
                i = 0;
            }
            Drawable drawable4 = adaptiveIconDrawable;
            if (i7 != -1 && iMax2 != -1) {
                convertToConvexArray(this.mLeftBorder, 1, i7, i8);
                convertToConvexArray(this.mRightBorder, -1, i7, i8);
                float f2 = 0.0f;
                for (int i13 = 0; i13 < intrinsicHeight; i13++) {
                    if (this.mLeftBorder[i13] > -1.0f) {
                        f2 += (this.mRightBorder[i13] - this.mLeftBorder[i13]) + 1.0f;
                    }
                }
                float f3 = f2 / (((i8 + 1) - i7) * ((iMax2 + 1) - i4));
                if (f3 < CIRCLE_AREA_BY_RECT) {
                    f = MAX_CIRCLE_AREA_FACTOR;
                } else {
                    f = MAX_SQUARE_AREA_FACTOR + (LINEAR_SCALE_SLOPE * (1.0f - f3));
                }
                this.mBounds.left = i4;
                this.mBounds.right = iMax2;
                this.mBounds.top = i7;
                this.mBounds.bottom = i8;
                if (rectF != null) {
                    float f4 = intrinsicWidth;
                    float f5 = intrinsicHeight;
                    rectF.set(this.mBounds.left / f4, this.mBounds.top / f5, 1.0f - (this.mBounds.right / f4), 1.0f - (this.mBounds.bottom / f5));
                }
                if (zArr != null && zArr.length > 0) {
                    zArr[0] = isShape(path);
                }
                float fSqrt = f2 / (intrinsicWidth * intrinsicHeight) > f ? (float) Math.sqrt(f / r8) : 1.0f;
                if (Utilities.ATLEAST_OREO && (drawable4 instanceof AdaptiveIconDrawable) && this.mAdaptiveIconScale == 0.0f) {
                    this.mAdaptiveIconScale = fSqrt;
                    this.mAdaptiveIconBounds.set(this.mBounds);
                }
                return fSqrt;
            }
            return 1.0f;
        }
    }

    private static void convertToConvexArray(float[] fArr, int i, int i2, int i3) {
        float[] fArr2 = new float[fArr.length - 1];
        int i4 = -1;
        float f = Float.MAX_VALUE;
        for (int i5 = i2 + 1; i5 <= i3; i5++) {
            if (fArr[i5] > -1.0f) {
                if (f != Float.MAX_VALUE) {
                    float f2 = ((fArr[i5] - fArr[i4]) / (i5 - i4)) - f;
                    float f3 = i;
                    if (f2 * f3 < 0.0f) {
                        while (i4 > i2) {
                            i4--;
                            if ((((fArr[i5] - fArr[i4]) / (i5 - i4)) - fArr2[i4]) * f3 >= 0.0f) {
                                break;
                            }
                        }
                    }
                } else {
                    i4 = i2;
                }
                f = (fArr[i5] - fArr[i4]) / (i5 - i4);
                for (int i6 = i4; i6 < i5; i6++) {
                    fArr2[i6] = f;
                    fArr[i6] = fArr[i4] + ((i6 - i4) * f);
                }
                i4 = i5;
            }
        }
    }

    public static int getNormalizedCircleSize(int i) {
        return (int) Math.round(Math.sqrt(((double) (4.0f * ((i * i) * MAX_CIRCLE_AREA_FACTOR))) / 3.141592653589793d));
    }
}
