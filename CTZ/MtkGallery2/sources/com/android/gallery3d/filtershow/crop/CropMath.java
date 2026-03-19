package com.android.gallery3d.filtershow.crop;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class CropMath {
    public static float[] getCornersFromRect(RectF rectF) {
        return new float[]{rectF.left, rectF.top, rectF.right, rectF.top, rectF.right, rectF.bottom, rectF.left, rectF.bottom};
    }

    public static boolean inclusiveContains(RectF rectF, float f, float f2) {
        return f <= rectF.right && f >= rectF.left && f2 <= rectF.bottom && f2 >= rectF.top;
    }

    public static RectF trapToRect(float[] fArr) {
        RectF rectF = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < fArr.length; i += 2) {
            float f = fArr[i - 1];
            float f2 = fArr[i];
            rectF.left = f < rectF.left ? f : rectF.left;
            rectF.top = f2 < rectF.top ? f2 : rectF.top;
            if (f <= rectF.right) {
                f = rectF.right;
            }
            rectF.right = f;
            if (f2 <= rectF.bottom) {
                f2 = rectF.bottom;
            }
            rectF.bottom = f2;
        }
        rectF.sort();
        return rectF;
    }

    public static void getEdgePoints(RectF rectF, float[] fArr) {
        if (fArr.length < 2) {
            return;
        }
        for (int i = 0; i < fArr.length; i += 2) {
            fArr[i] = GeometryMathUtils.clamp(fArr[i], rectF.left, rectF.right);
            int i2 = i + 1;
            fArr[i2] = GeometryMathUtils.clamp(fArr[i2], rectF.top, rectF.bottom);
        }
    }

    public static float[] closestSide(float[] fArr, float[] fArr2) {
        int length = fArr2.length;
        float[] fArr3 = null;
        float f = Float.POSITIVE_INFINITY;
        int i = 0;
        while (i < length) {
            int i2 = i + 2;
            float[] fArr4 = {fArr2[i], fArr2[(i + 1) % length], fArr2[i2 % length], fArr2[(i + 3) % length]};
            float fVectorLength = GeometryMathUtils.vectorLength(GeometryMathUtils.shortestVectorFromPointToLine(fArr, fArr4));
            if (fVectorLength < f) {
                f = fVectorLength;
                fArr3 = fArr4;
            }
            i = i2;
        }
        return fArr3;
    }

    public static void fixAspectRatioContained(RectF rectF, float f, float f2) {
        float fWidth = rectF.width();
        float fHeight = rectF.height();
        float f3 = f / f2;
        if (fWidth / fHeight < f3) {
            float f4 = fWidth / f3;
            rectF.top = rectF.centerY() - (f4 / 2.0f);
            rectF.bottom = rectF.top + f4;
        } else {
            float f5 = fHeight * f3;
            rectF.left = rectF.centerX() - (f5 / 2.0f);
            rectF.right = rectF.left + f5;
        }
    }

    public static RectF getScaledCropBounds(RectF rectF, RectF rectF2, RectF rectF3) {
        Matrix matrix = new Matrix();
        matrix.setRectToRect(rectF2, rectF3, Matrix.ScaleToFit.FILL);
        RectF rectF4 = new RectF(rectF);
        if (!matrix.mapRect(rectF4)) {
            return null;
        }
        return rectF4;
    }

    public static int getBitmapSize(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static int constrainedRotation(float f) {
        int i = (int) ((f % 360.0f) / 90.0f);
        if (i < 0) {
            i += 4;
        }
        return i * 90;
    }
}
