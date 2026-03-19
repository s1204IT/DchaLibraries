package com.mediatek.camera.common.utils;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class CoordinatesTransform {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CoordinatesTransform.class.getSimpleName());
    private static boolean sIsDebugMode = false;

    public static Rect uiToNormalizedPreview(Point point, Rect rect, float f, boolean z, int i) {
        int iHeight = rect.height();
        int iWidth = rect.width();
        int iHeight2 = rect.height();
        int iWidth2 = rect.width();
        if (i == 0 || i == 180) {
            iHeight2 = iWidth > iHeight ? iHeight : iWidth;
            iWidth2 = iWidth > iHeight ? iWidth : iHeight;
        } else if (i == 90 || i == 270) {
            iHeight2 = iWidth > iHeight ? iWidth : iHeight;
            if (iWidth > iHeight) {
            }
        }
        coordinatesLog(TAG, "uiToNormalizedPreview, p.x = " + point.x + ", p.y = " + point.y + ", orientation = " + i + ", mirror = " + z);
        int iMin = (int) (((float) Math.min(iHeight2, iWidth2)) * f);
        coordinatesLog(TAG, "uiToNormalizedPreview, preview area = (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + "), w = " + iHeight + ", h = " + iWidth);
        RectF rectF = new RectF(rect);
        Matrix matrix = new Matrix();
        matrix.postTranslate((float) (-rect.left), (float) (-rect.top));
        matrix.mapRect(rectF);
        point.x = point.x - rect.left;
        point.y = point.y - rect.top;
        int i2 = iMin / 2;
        int iClamp = clamp(point.x - i2, (int) rectF.left, ((int) rectF.right) - iMin);
        int iClamp2 = clamp(point.y - i2, (int) rectF.top, ((int) rectF.bottom) - iMin);
        RectF rectF2 = new RectF((float) iClamp, (float) iClamp2, (float) (iClamp + iMin), (float) (iClamp2 + iMin));
        coordinatesLog(TAG, "uiToNormalizedPreview, focus_rect = (" + iClamp + ", " + iClamp2 + "),size = " + iMin);
        Matrix matrix2 = new Matrix();
        prepareMatrix(matrix2, z, i, iWidth2, iHeight2);
        Matrix matrix3 = new Matrix();
        matrix2.invert(matrix3);
        matrix3.mapRect(rectF2);
        Rect rect2 = new Rect();
        rectF2.round(rect2);
        if (!checkRectValidiate(rect2)) {
            LogHelper.i(TAG, "uiToNormalizedPreview, p.x = " + point.x + ", p.y = " + point.y + ", orientation = " + i + ", mirror = " + z);
            LogHelper.i(TAG, "uiToNormalizedPreview, preview area = (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + "), w = " + iHeight + ", h = " + iWidth);
            LogUtil.Tag tag = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("uiToNormalizedPreview, focus_rect = (");
            sb.append(iClamp);
            sb.append(", ");
            sb.append(iClamp2);
            sb.append("),size = ");
            sb.append(iMin);
            LogHelper.i(tag, sb.toString());
            LogHelper.i(TAG, "uiToNormalizedPreview, result_rect = (" + rect2.left + ", " + rect2.top + ", " + rect2.right + ", " + rect2.bottom + ")");
            throw new IllegalArgumentException("camera app set invalid coordinate");
        }
        return rect2;
    }

    public static Rect normalizedPreviewToUi(Rect rect, int i, int i2, int i3, boolean z) {
        int i4;
        int i5 = 0;
        if (i3 == 0 || i3 == 180) {
            i5 = i2 > i ? i : i2;
            i4 = i2 > i ? i2 : i;
        } else if (i3 == 90 || i3 == 270) {
            i5 = i2 > i ? i2 : i;
            if (i2 > i) {
            }
        } else {
            i4 = 0;
        }
        coordinatesLog(TAG, "normalizedPreviewToUi, w = " + i + ", h = " + i2 + ", orientation = " + i3 + ", mirror = " + z);
        coordinatesLog(TAG, "normalizedPreviewToUi, rect = (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")");
        Matrix matrix = new Matrix();
        prepareMatrix(matrix, z, i3, i4, i5);
        RectF rectF = new RectF(rect);
        matrix.mapRect(rectF);
        Rect rect2 = new Rect();
        rectF.round(rect2);
        coordinatesLog(TAG, "normalizedPreviewToUi, result_rect = (" + rect2.left + ", " + rect2.top + ", " + rect2.right + ", " + rect2.bottom + ")");
        return rect2;
    }

    @TargetApi(21)
    public static Rect sensorToNormalizedPreview(Rect rect, int i, int i2, Rect rect2) {
        double d;
        coordinatesLog(TAG, "sensorToNormalizedPreview, w = " + i + ", h = " + i2);
        coordinatesLog(TAG, "sensorToNormalizedPreview, rect = (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")");
        coordinatesLog(TAG, "cropRegion = " + rect2.left + "," + rect2.top + "," + rect2.right + "," + rect2.bottom);
        if (i > i2) {
            d = ((double) i) / ((double) i2);
        } else {
            d = ((double) i2) / ((double) i);
        }
        double dWidth = ((double) rect2.width()) / ((double) rect2.height());
        int iWidth = rect2.width();
        int iHeight = rect2.height();
        if (d > dWidth) {
            iHeight = (int) (((double) iWidth) / d);
        } else {
            iWidth = (int) (((double) iHeight) * d);
        }
        int iAbs = Math.abs(iWidth - rect2.width());
        int iAbs2 = Math.abs(iHeight - rect2.height());
        RectF rectF = new RectF(rect);
        Matrix matrix = new Matrix();
        matrix.postTranslate((-rect2.left) - (iAbs / 2), (-rect2.top) - (iAbs2 / 2));
        matrix.postTranslate((-iWidth) / 2, (-iHeight) / 2);
        matrix.postScale(2000.0f / iWidth, 2000.0f / iHeight);
        matrix.mapRect(rectF);
        Rect rect3 = new Rect();
        rectF.round(rect3);
        coordinatesLog(TAG, "sensorToNormalizedPreview, resultRect = (" + rect3.left + ", " + rect3.top + ", " + rect3.right + ", " + rect3.bottom + ")");
        return rect3;
    }

    @TargetApi(21)
    public static Rect uiToSensor(Point point, Rect rect, int i, float f, Rect rect2, CameraCharacteristics cameraCharacteristics) {
        LogHelper.d(TAG, "uiToSensor1, point = (" + point.x + ", " + point.y + "); previewArea = (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + "); cropRegion = (" + rect2.width() + ", " + rect2.height() + ")");
        float[] fArr = {((float) (point.x - rect.left)) / ((float) rect.width()), ((float) (point.y - rect.top)) / ((float) rect.height())};
        Matrix matrix = new Matrix();
        matrix.setRotate((float) i, 0.5f, 0.5f);
        matrix.mapPoints(fArr);
        if (((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
            fArr[0] = 1.0f - fArr[0];
        }
        Rect rectNormalizedPreviewTransformedToSensor = normalizedPreviewTransformedToSensor(normalizedSensorCoordsForNormalizedDisplayCoords(fArr[0], fArr[1], ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue()), f, rect, rect2);
        LogHelper.d(TAG, "uiToSensor1, resultRegion = (" + rectNormalizedPreviewTransformedToSensor.left + ", " + rectNormalizedPreviewTransformedToSensor.top + ", " + rectNormalizedPreviewTransformedToSensor.right + ", " + rectNormalizedPreviewTransformedToSensor.bottom + ")");
        return rectNormalizedPreviewTransformedToSensor;
    }

    private static Rect normalizedPreviewTransformedToSensor(PointF pointF, float f, Rect rect, Rect rect2) {
        double dHeight;
        int iMin = (int) (0.5f * f * Math.min(rect2.width(), rect2.height()));
        if (rect.width() > rect.height()) {
            dHeight = ((double) rect.width()) / ((double) rect.height());
        } else {
            dHeight = ((double) rect.height()) / ((double) rect.width());
        }
        double dWidth = ((double) rect2.width()) / ((double) rect2.height());
        int iWidth = rect2.width();
        int iHeight = rect2.height();
        if (dHeight > dWidth) {
            iHeight = (int) (((double) iWidth) / dHeight);
        } else {
            iWidth = (int) (((double) iHeight) * dHeight);
        }
        int iWidth2 = (rect2.width() - iWidth) / 2;
        int iHeight2 = (rect2.height() - iHeight) / 2;
        int i = (int) (rect2.left + (pointF.x * iWidth) + iWidth2);
        int i2 = (int) (rect2.top + (pointF.y * iHeight) + iHeight2);
        Rect rect3 = new Rect(rect2.left + iWidth2, rect2.top + iHeight2, rect2.right - iWidth2, rect2.bottom - iHeight2);
        return new Rect(clamp(i - iMin, rect3.left, rect3.right), clamp(i2 - iMin, rect3.top, rect3.bottom), clamp(i + iMin, rect3.left, rect3.right), clamp(i2 + iMin, rect3.top, rect3.bottom));
    }

    private static void prepareMatrix(Matrix matrix, boolean z, int i, int i2, int i3) {
        matrix.setScale(z ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate(i);
        float f = i2;
        float f2 = i3;
        matrix.postScale(f / 2000.0f, f2 / 2000.0f);
        matrix.postTranslate(f / 2.0f, f2 / 2.0f);
    }

    private static PointF normalizedSensorCoordsForNormalizedDisplayCoords(float f, float f2, int i) {
        if (i == 0) {
            return new PointF(f, f2);
        }
        if (i == 90) {
            return new PointF(f2, 1.0f - f);
        }
        if (i == 180) {
            return new PointF(1.0f - f, 1.0f - f2);
        }
        if (i == 270) {
            return new PointF(1.0f - f2, f);
        }
        return null;
    }

    private static int clamp(int i, int i2, int i3) {
        if (i > i3) {
            return i3;
        }
        if (i < i2) {
            return i2;
        }
        return i;
    }

    private static void coordinatesLog(LogUtil.Tag tag, String str) {
        if (sIsDebugMode) {
            LogHelper.d(tag, str);
        }
    }

    private static boolean checkRectValidiate(Rect rect) {
        return rect.left <= 1000 && rect.left >= -1000 && rect.top <= 1000 && rect.top >= -1000 && rect.right <= 1000 && rect.right >= -1000 && rect.bottom <= 1000 && rect.bottom >= -1000;
    }
}
