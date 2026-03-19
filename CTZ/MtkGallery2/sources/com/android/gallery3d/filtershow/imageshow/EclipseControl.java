package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

public class EclipseControl {
    private float mDownCenterX;
    private float mDownCenterY;
    private float mDownRadiusX;
    private float mDownRadiusY;
    private float mDownX;
    private float mDownY;
    private Rect mImageBounds;
    private Matrix mScrToImg;
    private static int MIN_TOUCH_DIST = 80;
    private static String LOGTAG = "EclipseControl";
    private float mCenterX = Float.NaN;
    private float mCenterY = 0.0f;
    private float mRadiusX = 200.0f;
    private float mRadiusY = 300.0f;
    private float[] handlex = new float[9];
    private float[] handley = new float[9];
    private int mCenterDotSize = 40;
    private boolean mShowReshapeHandles = true;
    public Matrix mMatrixForMove = new Matrix();
    private int mSliderColor = -1;

    public EclipseControl(Context context) {
    }

    public void setRadius(float f, float f2) {
        this.mRadiusX = f;
        this.mRadiusY = f2;
    }

    public void setCenter(float f, float f2) {
        this.mCenterX = f;
        this.mCenterY = f2;
    }

    public int getCloseHandle(float f, float f2) {
        int i = -1;
        float f3 = Float.MAX_VALUE;
        for (int i2 = 0; i2 < this.handlex.length; i2++) {
            float f4 = this.handlex[i2] - f;
            float f5 = this.handley[i2] - f2;
            float f6 = (f4 * f4) + (f5 * f5);
            if (f6 < f3) {
                i = i2;
                f3 = f6;
            }
        }
        if (f3 < MIN_TOUCH_DIST * MIN_TOUCH_DIST) {
            return i;
        }
        for (int i3 = 0; i3 < this.handlex.length; i3++) {
            float f7 = this.handlex[i3] - f;
            float f8 = this.handley[i3] - f2;
            Math.sqrt((f7 * f7) + (f8 * f8));
        }
        return -1;
    }

    public void setScrImageInfo(Matrix matrix, Rect rect) {
        this.mScrToImg = matrix;
        this.mImageBounds = new Rect(rect);
    }

    private boolean centerIsOutside(float f, float f2) {
        float[] fArr = {f, f2};
        this.mMatrixForMove.mapPoints(fArr);
        return true ^ this.mImageBounds.contains((int) fArr[0], (int) fArr[1]);
    }

    public void actionDown(float f, float f2, Oval oval) {
        float[] fArr = {f, f2};
        this.mScrToImg.mapPoints(fArr);
        this.mDownX = fArr[0];
        this.mDownY = fArr[1];
        this.mDownCenterX = oval.getCenterX();
        this.mDownCenterY = oval.getCenterY();
        this.mDownRadiusX = oval.getRadiusX();
        this.mDownRadiusY = oval.getRadiusY();
    }

    public void actionMove(int i, float f, float f2, Oval oval) {
        int i2 = 1;
        float[] fArr = {f, f2};
        this.mScrToImg.mapPoints(fArr);
        float f3 = fArr[0];
        float f4 = fArr[1];
        fArr[0] = 0.0f;
        fArr[1] = 1.0f;
        this.mScrToImg.mapVectors(fArr);
        boolean z = fArr[0] > 0.0f;
        switch (i) {
            case 0:
                float f5 = this.mDownX - this.mDownCenterX;
                float f6 = this.mDownY - this.mDownCenterY;
                if (!centerIsOutside(f3, f4)) {
                    oval.setCenter(f3 - f5, f4 - f6);
                    break;
                }
                break;
            case 1:
                i2 = -1;
                if (!z) {
                    oval.setRadiusX(Math.abs((f4 - oval.getCenterX()) + (i2 * (this.mDownRadiusX - Math.abs(this.mDownY - this.mDownCenterX)))));
                } else {
                    oval.setRadiusX(Math.abs((f3 - oval.getCenterX()) - (i2 * (this.mDownRadiusX - Math.abs(this.mDownX - this.mDownCenterX)))));
                }
                break;
            case 2:
            case 4:
            case 6:
            case 8:
                float fSin = (float) Math.sin(45.0d);
                float fAbs = (Math.abs(this.mDownX - this.mDownCenterX) + Math.abs(this.mDownY - this.mDownCenterY)) - ((this.mDownRadiusX + this.mDownRadiusY) * fSin);
                float radiusX = oval.getRadiusX();
                float radiusY = oval.getRadiusY();
                float fAbs2 = (Math.abs(radiusX) + Math.abs(radiusY)) * fSin;
                float fAbs3 = Math.abs((Math.abs(f3 - oval.getCenterX()) + Math.abs(f4 - oval.getCenterY())) - fAbs);
                oval.setRadius((radiusX * fAbs3) / fAbs2, (radiusY * fAbs3) / fAbs2);
                break;
            case 3:
                if (!z) {
                    oval.setRadiusY(Math.abs((f3 - oval.getCenterY()) + (i2 * (this.mDownRadiusY - Math.abs(this.mDownX - this.mDownCenterY)))));
                } else {
                    oval.setRadiusY(Math.abs((f4 - oval.getCenterY()) + (i2 * (this.mDownRadiusY - Math.abs(this.mDownY - this.mDownCenterY)))));
                }
                break;
            case 5:
                if (!z) {
                }
                break;
            case 7:
                i2 = -1;
                if (!z) {
                }
                break;
        }
    }

    public void paintPoint(Canvas canvas, float f, float f2) {
        if (f == Float.NaN) {
            return;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(-16776961);
        paint.setShader(new RadialGradient(f, f2, this.mCenterDotSize, new int[]{this.mSliderColor, this.mSliderColor, 1711276032, 0}, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(f, f2, this.mCenterDotSize, paint);
    }

    void paintRadius(Canvas canvas, float f, float f2, float f3, float f4) {
        if (f == Float.NaN) {
            return;
        }
        Paint paint = new Paint();
        RectF rectF = new RectF(f - f3, f2 - f4, f + f3, f2 + f4);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6.0f);
        paint.setColor(-16777216);
        paintOvallines(canvas, rectF, paint, f, f2, f3, f4);
        paint.setStrokeWidth(3.0f);
        paint.setColor(-1);
        paintOvallines(canvas, rectF, paint, f, f2, f3, f4);
    }

    public void paintOvallines(Canvas canvas, RectF rectF, Paint paint, float f, float f2, float f3, float f4) {
        canvas.drawOval(rectF, paint);
        if (this.mShowReshapeHandles) {
            paint.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 361; i += 90) {
                float f5 = f3 + 10.0f;
                float f6 = f4 + 10.0f;
                rectF.left = f - f5;
                rectF.top = f2 - f6;
                rectF.right = f + f5;
                rectF.bottom = f2 + f6;
                float f7 = i - 4.0f;
                canvas.drawArc(rectF, f7, 8.0f, false, paint);
                float f8 = f3 - 10.0f;
                float f9 = f4 - 10.0f;
                rectF.left = f - f8;
                rectF.top = f2 - f9;
                rectF.right = f + f8;
                rectF.bottom = f2 + f9;
                canvas.drawArc(rectF, f7, 8.0f, false, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        for (int i2 = 45; i2 < 361; i2 += 90) {
            double d = (3.141592653589793d * ((double) i2)) / 180.0d;
            float fCos = f + ((float) (((double) f3) * Math.cos(d)));
            float fSin = f2 + ((float) (((double) f4) * Math.sin(d)));
            canvas.drawRect(fCos - 8.0f, fSin - 8.0f, fCos + 8.0f, fSin + 8.0f, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        rectF.left = f - f3;
        rectF.top = f2 - f4;
        rectF.right = f + f3;
        rectF.bottom = f2 + f4;
    }

    public void fillHandles(Canvas canvas, float f, float f2, float f3, float f4) {
        this.handlex[0] = f;
        this.handley[0] = f2;
        int i = 1;
        for (int i2 = 0; i2 < 360; i2 += 45) {
            double d = (3.141592653589793d * ((double) i2)) / 180.0d;
            float fCos = ((float) (((double) f3) * Math.cos(d))) + f;
            float fSin = ((float) (((double) f4) * Math.sin(d))) + f2;
            this.handlex[i] = fCos;
            this.handley[i] = fSin;
            i++;
        }
    }

    public void draw(Canvas canvas) {
        paintRadius(canvas, this.mCenterX, this.mCenterY, this.mRadiusX, this.mRadiusY);
        fillHandles(canvas, this.mCenterX, this.mCenterY, this.mRadiusX, this.mRadiusY);
        paintPoint(canvas, this.mCenterX, this.mCenterY);
    }

    public void setMatrix(Matrix matrix) {
        this.mMatrixForMove = matrix;
    }
}
