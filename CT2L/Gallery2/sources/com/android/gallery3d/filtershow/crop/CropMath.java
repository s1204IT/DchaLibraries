package com.android.gallery3d.filtershow.crop;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class CropMath {
    public static float[] getCornersFromRect(RectF r) {
        float[] corners = {r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom};
        return corners;
    }

    public static boolean inclusiveContains(RectF r, float x, float y) {
        return x <= r.right && x >= r.left && y <= r.bottom && y >= r.top;
    }

    public static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = array[i - 1];
            float y = array[i];
            r.left = x < r.left ? x : r.left;
            r.top = y < r.top ? y : r.top;
            if (x <= r.right) {
                x = r.right;
            }
            r.right = x;
            if (y <= r.bottom) {
                y = r.bottom;
            }
            r.bottom = y;
        }
        r.sort();
        return r;
    }

    public static void getEdgePoints(RectF imageBound, float[] array) {
        if (array.length >= 2) {
            for (int x = 0; x < array.length; x += 2) {
                array[x] = GeometryMathUtils.clamp(array[x], imageBound.left, imageBound.right);
                array[x + 1] = GeometryMathUtils.clamp(array[x + 1], imageBound.top, imageBound.bottom);
            }
        }
    }

    public static float[] closestSide(float[] point, float[] corners) {
        int len = corners.length;
        float oldMag = Float.POSITIVE_INFINITY;
        float[] bestLine = null;
        for (int i = 0; i < len; i += 2) {
            float[] line = {corners[i], corners[(i + 1) % len], corners[(i + 2) % len], corners[(i + 3) % len]};
            float mag = GeometryMathUtils.vectorLength(GeometryMathUtils.shortestVectorFromPointToLine(point, line));
            if (mag < oldMag) {
                oldMag = mag;
                bestLine = line;
            }
        }
        return bestLine;
    }

    public static void fixAspectRatioContained(RectF r, float w, float h) {
        float origW = r.width();
        float origH = r.height();
        float origA = origW / origH;
        float a = w / h;
        if (origA < a) {
            float finalH = origW / a;
            r.top = r.centerY() - (finalH / 2.0f);
            r.bottom = r.top + finalH;
        } else {
            float finalW = origH * a;
            r.left = r.centerX() - (finalW / 2.0f);
            r.right = r.left + finalW;
        }
    }

    public static RectF getScaledCropBounds(RectF cropBounds, RectF photoBounds, RectF displayBounds) {
        Matrix m = new Matrix();
        m.setRectToRect(photoBounds, displayBounds, Matrix.ScaleToFit.FILL);
        RectF trueCrop = new RectF(cropBounds);
        if (!m.mapRect(trueCrop)) {
            return null;
        }
        return trueCrop;
    }

    public static int getBitmapSize(Bitmap bmap) {
        return bmap.getRowBytes() * bmap.getHeight();
    }

    public static int constrainedRotation(float rotation) {
        int r = (int) ((rotation % 360.0f) / 90.0f);
        if (r < 0) {
            r += 4;
        }
        return r * 90;
    }
}
