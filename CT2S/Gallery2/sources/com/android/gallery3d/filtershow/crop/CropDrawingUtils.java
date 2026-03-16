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
    public static void drawRuleOfThird(Canvas canvas, RectF bounds) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.argb(128, 255, 255, 255));
        p.setStrokeWidth(2.0f);
        float stepX = bounds.width() / 3.0f;
        float stepY = bounds.height() / 3.0f;
        float x = bounds.left + stepX;
        float y = bounds.top + stepY;
        for (int i = 0; i < 2; i++) {
            canvas.drawLine(x, bounds.top, x, bounds.bottom, p);
            x += stepX;
        }
        for (int j = 0; j < 2; j++) {
            canvas.drawLine(bounds.left, y, bounds.right, y, p);
            y += stepY;
        }
    }

    public static void drawCropRect(Canvas canvas, RectF bounds) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(-1);
        p.setStrokeWidth(3.0f);
        canvas.drawRect(bounds, p);
    }

    public static void drawShade(Canvas canvas, RectF bounds) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(-2013265920);
        RectF r = new RectF();
        r.set(0.0f, 0.0f, w, bounds.top);
        canvas.drawRect(r, p);
        r.set(0.0f, bounds.top, bounds.left, h);
        canvas.drawRect(r, p);
        r.set(bounds.left, bounds.bottom, w, h);
        canvas.drawRect(r, p);
        r.set(bounds.right, bounds.top, w, bounds.bottom);
        canvas.drawRect(r, p);
    }

    public static void drawIndicator(Canvas canvas, Drawable indicator, int indicatorSize, float centerX, float centerY) {
        int left = ((int) centerX) - (indicatorSize / 2);
        int top = ((int) centerY) - (indicatorSize / 2);
        indicator.setBounds(left, top, left + indicatorSize, top + indicatorSize);
        indicator.draw(canvas);
    }

    public static void drawIndicators(Canvas canvas, Drawable cropIndicator, int indicatorSize, RectF bounds, boolean fixedAspect, int selection) {
        boolean notMoving = selection == 0;
        if (fixedAspect) {
            if (selection == 3 || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize, bounds.left, bounds.top);
            }
            if (selection == 6 || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize, bounds.right, bounds.top);
            }
            if (selection == 9 || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize, bounds.left, bounds.bottom);
            }
            if (selection == 12 || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize, bounds.right, bounds.bottom);
                return;
            }
            return;
        }
        if ((selection & 2) != 0 || notMoving) {
            drawIndicator(canvas, cropIndicator, indicatorSize, bounds.centerX(), bounds.top);
        }
        if ((selection & 8) != 0 || notMoving) {
            drawIndicator(canvas, cropIndicator, indicatorSize, bounds.centerX(), bounds.bottom);
        }
        if ((selection & 1) != 0 || notMoving) {
            drawIndicator(canvas, cropIndicator, indicatorSize, bounds.left, bounds.centerY());
        }
        if ((selection & 4) != 0 || notMoving) {
            drawIndicator(canvas, cropIndicator, indicatorSize, bounds.right, bounds.centerY());
        }
    }

    public static void drawWallpaperSelectionFrame(Canvas canvas, RectF cropBounds, float spotX, float spotY, Paint p, Paint shadowPaint) {
        float sx = cropBounds.width() * spotX;
        float sy = cropBounds.height() * spotY;
        float cx = cropBounds.centerX();
        float cy = cropBounds.centerY();
        RectF r1 = new RectF(cx - (sx / 2.0f), cy - (sy / 2.0f), (sx / 2.0f) + cx, (sy / 2.0f) + cy);
        RectF r2 = new RectF(cx - (sy / 2.0f), cy - (sx / 2.0f), (sy / 2.0f) + cx, (sx / 2.0f) + cy);
        canvas.save();
        canvas.clipRect(cropBounds);
        canvas.clipRect(r1, Region.Op.DIFFERENCE);
        canvas.clipRect(r2, Region.Op.DIFFERENCE);
        canvas.drawPaint(shadowPaint);
        canvas.restore();
        Path path = new Path();
        path.moveTo(r1.left, r1.top);
        path.lineTo(r1.right, r1.top);
        path.moveTo(r1.left, r1.top);
        path.lineTo(r1.left, r1.bottom);
        path.moveTo(r1.left, r1.bottom);
        path.lineTo(r1.right, r1.bottom);
        path.moveTo(r1.right, r1.top);
        path.lineTo(r1.right, r1.bottom);
        path.moveTo(r2.left, r2.top);
        path.lineTo(r2.right, r2.top);
        path.moveTo(r2.right, r2.top);
        path.lineTo(r2.right, r2.bottom);
        path.moveTo(r2.left, r2.bottom);
        path.lineTo(r2.right, r2.bottom);
        path.moveTo(r2.left, r2.top);
        path.lineTo(r2.left, r2.bottom);
        canvas.drawPath(path, p);
    }

    public static void drawShadows(Canvas canvas, Paint p, RectF innerBounds, RectF outerBounds) {
        canvas.drawRect(outerBounds.left, outerBounds.top, innerBounds.right, innerBounds.top, p);
        canvas.drawRect(innerBounds.right, outerBounds.top, outerBounds.right, innerBounds.bottom, p);
        canvas.drawRect(innerBounds.left, innerBounds.bottom, outerBounds.right, outerBounds.bottom, p);
        canvas.drawRect(outerBounds.left, innerBounds.top, innerBounds.left, outerBounds.bottom, p);
    }

    public static boolean setImageToScreenMatrix(Matrix dst, RectF image, RectF screen, int rotation) {
        RectF rotatedImage = new RectF();
        dst.setRotate(rotation, image.centerX(), image.centerY());
        if (!dst.mapRect(rotatedImage, image)) {
            return false;
        }
        boolean rToR = dst.setRectToRect(rotatedImage, screen, Matrix.ScaleToFit.CENTER);
        boolean rot = dst.preRotate(rotation, image.centerX(), image.centerY());
        return rToR && rot;
    }
}
