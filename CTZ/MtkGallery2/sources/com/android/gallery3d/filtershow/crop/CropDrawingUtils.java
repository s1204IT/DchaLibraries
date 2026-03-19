package com.android.gallery3d.filtershow.crop;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

public abstract class CropDrawingUtils {
    public static void drawRuleOfThird(Canvas canvas, RectF rectF) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(128, 255, 255, 255));
        paint.setStrokeWidth(2.0f);
        float fWidth = rectF.width() / 3.0f;
        float fHeight = rectF.height() / 3.0f;
        float f = rectF.left + fWidth;
        float f2 = rectF.top + fHeight;
        float f3 = f;
        for (int i = 0; i < 2; i++) {
            canvas.drawLine(f3, rectF.top, f3, rectF.bottom, paint);
            f3 += fWidth;
        }
        for (int i2 = 0; i2 < 2; i2++) {
            canvas.drawLine(rectF.left, f2, rectF.right, f2, paint);
            f2 += fHeight;
        }
    }

    public static void drawCropRect(Canvas canvas, RectF rectF) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(-1);
        paint.setStrokeWidth(3.0f);
        canvas.drawRect(rectF, paint);
    }

    public static void drawShade(Canvas canvas, RectF rectF) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(-2013265920);
        RectF rectF2 = new RectF();
        float f = width;
        rectF2.set(0.0f, 0.0f, f, rectF.top);
        canvas.drawRect(rectF2, paint);
        float f2 = height;
        rectF2.set(0.0f, rectF.top, rectF.left, f2);
        canvas.drawRect(rectF2, paint);
        rectF2.set(rectF.left, rectF.bottom, f, f2);
        canvas.drawRect(rectF2, paint);
        rectF2.set(rectF.right, rectF.top, f, rectF.bottom);
        canvas.drawRect(rectF2, paint);
    }

    public static void drawIndicator(Canvas canvas, Drawable drawable, int i, float f, float f2) {
        int i2 = i / 2;
        int i3 = ((int) f) - i2;
        int i4 = ((int) f2) - i2;
        drawable.setBounds(i3, i4, i3 + i, i + i4);
        drawable.draw(canvas);
    }

    public static void drawIndicators(Canvas canvas, Drawable drawable, int i, RectF rectF, boolean z, int i2) {
        boolean z2;
        if (i2 != 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        if (z) {
            if (i2 == 3 || z2) {
                drawIndicator(canvas, drawable, i, rectF.left, rectF.top);
            }
            if (i2 == 6 || z2) {
                drawIndicator(canvas, drawable, i, rectF.right, rectF.top);
            }
            if (i2 == 9 || z2) {
                drawIndicator(canvas, drawable, i, rectF.left, rectF.bottom);
            }
            if (i2 == 12 || z2) {
                drawIndicator(canvas, drawable, i, rectF.right, rectF.bottom);
                return;
            }
            return;
        }
        if ((i2 & 2) != 0 || z2) {
            drawIndicator(canvas, drawable, i, rectF.centerX(), rectF.top);
        }
        if ((i2 & 8) != 0 || z2) {
            drawIndicator(canvas, drawable, i, rectF.centerX(), rectF.bottom);
        }
        if ((i2 & 1) != 0 || z2) {
            drawIndicator(canvas, drawable, i, rectF.left, rectF.centerY());
        }
        if ((i2 & 4) != 0 || z2) {
            drawIndicator(canvas, drawable, i, rectF.right, rectF.centerY());
        }
    }

    public static void drawWallpaperSelectionFrame(Canvas canvas, RectF rectF, float f, float f2, Paint paint, Paint paint2) {
        float fWidth = rectF.width() * f;
        float fHeight = rectF.height() * f2;
        float fCenterX = rectF.centerX();
        float fCenterY = rectF.centerY();
        float f3 = fWidth / 2.0f;
        float f4 = fHeight / 2.0f;
        RectF rectF2 = new RectF(fCenterX - f3, fCenterY - f4, fCenterX + f3, fCenterY + f4);
        RectF rectF3 = new RectF(fCenterX - f4, fCenterY - f3, fCenterX + f4, fCenterY + f3);
        canvas.save();
        canvas.clipRect(rectF);
        canvas.clipRect(rectF2, Region.Op.DIFFERENCE);
        canvas.clipRect(rectF3, Region.Op.DIFFERENCE);
        canvas.drawPaint(paint2);
        canvas.restore();
        Path path = new Path();
        path.moveTo(rectF2.left, rectF2.top);
        path.lineTo(rectF2.right, rectF2.top);
        path.moveTo(rectF2.left, rectF2.top);
        path.lineTo(rectF2.left, rectF2.bottom);
        path.moveTo(rectF2.left, rectF2.bottom);
        path.lineTo(rectF2.right, rectF2.bottom);
        path.moveTo(rectF2.right, rectF2.top);
        path.lineTo(rectF2.right, rectF2.bottom);
        path.moveTo(rectF3.left, rectF3.top);
        path.lineTo(rectF3.right, rectF3.top);
        path.moveTo(rectF3.right, rectF3.top);
        path.lineTo(rectF3.right, rectF3.bottom);
        path.moveTo(rectF3.left, rectF3.bottom);
        path.lineTo(rectF3.right, rectF3.bottom);
        path.moveTo(rectF3.left, rectF3.top);
        path.lineTo(rectF3.left, rectF3.bottom);
        canvas.drawPath(path, paint);
    }

    public static void drawShadows(Canvas canvas, Paint paint, RectF rectF, RectF rectF2) {
        canvas.drawRect(rectF2.left, rectF2.top, rectF.right, rectF.top, paint);
        canvas.drawRect(rectF.right, rectF2.top, rectF2.right, rectF.bottom, paint);
        canvas.drawRect(rectF.left, rectF.bottom, rectF2.right, rectF2.bottom, paint);
        canvas.drawRect(rectF2.left, rectF.top, rectF.left, rectF2.bottom, paint);
    }

    public static boolean setImageToScreenMatrix(Matrix matrix, RectF rectF, RectF rectF2, int i) {
        RectF rectF3 = new RectF();
        float f = i;
        matrix.setRotate(f, rectF.centerX(), rectF.centerY());
        matrix.mapRect(rectF3, rectF);
        return matrix.setRectToRect(rectF3, rectF2, Matrix.ScaleToFit.CENTER) && matrix.preRotate(f, rectF.centerX(), rectF.centerY());
    }
}
