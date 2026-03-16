package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;

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
    private int mSliderColor = -1;

    public EclipseControl(Context context) {
    }

    public void setRadius(float x, float y) {
        this.mRadiusX = x;
        this.mRadiusY = y;
    }

    public void setCenter(float x, float y) {
        this.mCenterX = x;
        this.mCenterY = y;
    }

    public int getCloseHandle(float x, float y) {
        float min = Float.MAX_VALUE;
        int handle = -1;
        for (int i = 0; i < this.handlex.length; i++) {
            float dx = this.handlex[i] - x;
            float dy = this.handley[i] - y;
            float dist = (dx * dx) + (dy * dy);
            if (dist < min) {
                min = dist;
                handle = i;
            }
        }
        if (min >= MIN_TOUCH_DIST * MIN_TOUCH_DIST) {
            for (int i2 = 0; i2 < this.handlex.length; i2++) {
                float dx2 = this.handlex[i2] - x;
                float dy2 = this.handley[i2] - y;
            }
            return -1;
        }
        return handle;
    }

    public void setScrImageInfo(Matrix scrToImg, Rect imageBounds) {
        this.mScrToImg = scrToImg;
        this.mImageBounds = new Rect(imageBounds);
    }

    private boolean centerIsOutside(float x1, float y1) {
        return !this.mImageBounds.contains((int) x1, (int) y1);
    }

    public void actionDown(float x, float y, Oval oval) {
        float[] point = {x, y};
        this.mScrToImg.mapPoints(point);
        this.mDownX = point[0];
        this.mDownY = point[1];
        this.mDownCenterX = oval.getCenterX();
        this.mDownCenterY = oval.getCenterY();
        this.mDownRadiusX = oval.getRadiusX();
        this.mDownRadiusY = oval.getRadiusY();
    }

    public void actionMove(int handle, float x, float y, Oval oval) {
        float[] point = {x, y};
        this.mScrToImg.mapPoints(point);
        float x2 = point[0];
        float y2 = point[1];
        point[0] = 0.0f;
        point[1] = 1.0f;
        this.mScrToImg.mapVectors(point);
        boolean swapxy = point[0] > 0.0f;
        int sign = 1;
        switch (handle) {
            case 0:
                float ctrdx = this.mDownX - this.mDownCenterX;
                float ctrdy = this.mDownY - this.mDownCenterY;
                if (!centerIsOutside(x2 - ctrdx, y2 - ctrdy)) {
                    oval.setCenter(x2 - ctrdx, y2 - ctrdy);
                }
                break;
            case 1:
                sign = -1;
                if (!swapxy) {
                    float raddy = this.mDownRadiusX - Math.abs(this.mDownY - this.mDownCenterX);
                    oval.setRadiusX(Math.abs((y2 - oval.getCenterX()) + (sign * raddy)));
                } else {
                    float raddx = this.mDownRadiusX - Math.abs(this.mDownX - this.mDownCenterX);
                    oval.setRadiusX(Math.abs((x2 - oval.getCenterX()) - (sign * raddx)));
                }
                break;
            case 2:
            case 4:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                float sin45 = (float) Math.sin(45.0d);
                float dr = (this.mDownRadiusX + this.mDownRadiusY) * sin45;
                float ctr_dx = this.mDownX - this.mDownCenterX;
                float ctr_dy = this.mDownY - this.mDownCenterY;
                float downRad = (Math.abs(ctr_dx) + Math.abs(ctr_dy)) - dr;
                float rx = oval.getRadiusX();
                float ry = oval.getRadiusY();
                float r = (Math.abs(rx) + Math.abs(ry)) * sin45;
                float dx = x2 - oval.getCenterX();
                float dy = y2 - oval.getCenterY();
                float nr = Math.abs((Math.abs(dx) + Math.abs(dy)) - downRad);
                oval.setRadius((rx * nr) / r, (ry * nr) / r);
                break;
            case 3:
                if (!swapxy) {
                    float raddx2 = this.mDownRadiusY - Math.abs(this.mDownX - this.mDownCenterY);
                    oval.setRadiusY(Math.abs((x2 - oval.getCenterY()) + (sign * raddx2)));
                } else {
                    float raddy2 = this.mDownRadiusY - Math.abs(this.mDownY - this.mDownCenterY);
                    oval.setRadiusY(Math.abs((y2 - oval.getCenterY()) + (sign * raddy2)));
                }
                break;
            case 5:
                if (!swapxy) {
                }
                break;
            case 7:
                sign = -1;
                if (!swapxy) {
                }
                break;
        }
    }

    public void paintPoint(Canvas canvas, float x, float y) {
        if (x != Float.NaN) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(-16776961);
            int[] colors3 = {this.mSliderColor, this.mSliderColor, 1711276032, 0};
            RadialGradient g = new RadialGradient(x, y, this.mCenterDotSize, colors3, new float[]{0.0f, 0.3f, 0.31f, 1.0f}, Shader.TileMode.CLAMP);
            paint.setShader(g);
            canvas.drawCircle(x, y, this.mCenterDotSize, paint);
        }
    }

    void paintRadius(Canvas canvas, float cx, float cy, float rx, float ry) {
        if (cx != Float.NaN) {
            Paint paint = new Paint();
            RectF rect = new RectF(cx - rx, cy - ry, cx + rx, cy + ry);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6.0f);
            paint.setColor(-16777216);
            paintOvallines(canvas, rect, paint, cx, cy, rx, ry);
            paint.setStrokeWidth(3.0f);
            paint.setColor(-1);
            paintOvallines(canvas, rect, paint, cx, cy, rx, ry);
        }
    }

    public void paintOvallines(Canvas canvas, RectF rect, Paint paint, float cx, float cy, float rx, float ry) {
        canvas.drawOval(rect, paint);
        float arclen = 4.0f + 4.0f;
        if (this.mShowReshapeHandles) {
            paint.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 361; i += 90) {
                float dx = rx + 10.0f;
                float dy = ry + 10.0f;
                rect.left = cx - dx;
                rect.top = cy - dy;
                rect.right = cx + dx;
                rect.bottom = cy + dy;
                canvas.drawArc(rect, i - 4.0f, arclen, false, paint);
                float dx2 = rx - 10.0f;
                float dy2 = ry - 10.0f;
                rect.left = cx - dx2;
                rect.top = cy - dy2;
                rect.right = cx + dx2;
                rect.bottom = cy + dy2;
                canvas.drawArc(rect, i - 4.0f, arclen, false, paint);
            }
        }
        float da = 4.0f * 2.0f;
        paint.setStyle(Paint.Style.FILL);
        for (int i2 = 45; i2 < 361; i2 += 90) {
            double angle = (3.141592653589793d * ((double) i2)) / 180.0d;
            float x = cx + ((float) (((double) rx) * Math.cos(angle)));
            float y = cy + ((float) (((double) ry) * Math.sin(angle)));
            canvas.drawRect(x - da, y - da, x + da, y + da, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        rect.left = cx - rx;
        rect.top = cy - ry;
        rect.right = cx + rx;
        rect.bottom = cy + ry;
    }

    public void fillHandles(Canvas canvas, float cx, float cy, float rx, float ry) {
        this.handlex[0] = cx;
        this.handley[0] = cy;
        int k = 1;
        for (int i = 0; i < 360; i += 45) {
            double angle = (3.141592653589793d * ((double) i)) / 180.0d;
            float x = cx + ((float) (((double) rx) * Math.cos(angle)));
            float y = cy + ((float) (((double) ry) * Math.sin(angle)));
            this.handlex[k] = x;
            this.handley[k] = y;
            k++;
        }
    }

    public void draw(Canvas canvas) {
        paintRadius(canvas, this.mCenterX, this.mCenterY, this.mRadiusX, this.mRadiusY);
        fillHandles(canvas, this.mCenterX, this.mCenterY, this.mRadiusX, this.mRadiusY);
        paintPoint(canvas, this.mCenterX, this.mCenterY);
    }
}
