package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import com.android.gallery3d.R;

public class GradControl {
    private int mCenterDotSize;
    private float mDownPoint1X;
    private float mDownPoint1Y;
    private float mDownPoint2X;
    private float mDownPoint2Y;
    private float mDownX;
    private float mDownY;
    private int[] mGrayPointColorPatern;
    Rect mImageBounds;
    private int mLineColor;
    private int mMinTouchDist;
    private int[] mPointColorPatern;
    private Matrix mScrToImg;
    private int mSliderColor;
    private int mlineShadowColor;
    private float mPoint1X = Float.NaN;
    private float mPoint1Y = 0.0f;
    private float mPoint2X = 200.0f;
    private float mPoint2Y = 300.0f;
    private float[] handlex = new float[3];
    private float[] handley = new float[3];
    Paint mPaint = new Paint();
    DashPathEffect mDash = new DashPathEffect(new float[]{30.0f, 30.0f}, 0.0f);
    private boolean mShowReshapeHandles = true;
    private float[] mPointRadialPos = {0.0f, 0.3f, 0.31f, 1.0f};

    public GradControl(Context context) {
        this.mMinTouchDist = 80;
        Resources resources = context.getResources();
        this.mCenterDotSize = (int) resources.getDimension(R.dimen.gradcontrol_dot_size);
        this.mMinTouchDist = (int) resources.getDimension(R.dimen.gradcontrol_min_touch_dist);
        int color = resources.getColor(R.color.gradcontrol_graypoint_center);
        int color2 = resources.getColor(R.color.gradcontrol_graypoint_edge);
        int color3 = resources.getColor(R.color.gradcontrol_point_center);
        int color4 = resources.getColor(R.color.gradcontrol_point_edge);
        int color5 = resources.getColor(R.color.gradcontrol_point_shadow_start);
        int color6 = resources.getColor(R.color.gradcontrol_point_shadow_end);
        this.mPointColorPatern = new int[]{color3, color4, color5, color6};
        this.mGrayPointColorPatern = new int[]{color, color2, color5, color6};
        this.mSliderColor = -1;
        this.mLineColor = resources.getColor(R.color.gradcontrol_line_color);
        this.mlineShadowColor = resources.getColor(R.color.gradcontrol_line_shadow);
    }

    public void setPoint2(float f, float f2) {
        this.mPoint2X = f;
        this.mPoint2Y = f2;
    }

    public void setPoint1(float f, float f2) {
        this.mPoint1X = f;
        this.mPoint1Y = f2;
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
        if (f3 < this.mMinTouchDist * this.mMinTouchDist) {
            return i;
        }
        for (int i3 = 0; i3 < this.handlex.length; i3++) {
            Math.hypot(this.handlex[i3] - f, this.handley[i3] - f2);
        }
        return -1;
    }

    public void setScrImageInfo(Matrix matrix, Rect rect) {
        this.mScrToImg = matrix;
        this.mImageBounds = new Rect(rect);
    }

    private boolean centerIsOutside(float f, float f2, float f3, float f4) {
        return !this.mImageBounds.contains((int) ((f + f3) / 2.0f), (int) ((f2 + f4) / 2.0f));
    }

    public void actionDown(float f, float f2, Line line) {
        float[] fArr = {f, f2};
        this.mScrToImg.mapPoints(fArr);
        this.mDownX = fArr[0];
        this.mDownY = fArr[1];
        this.mDownPoint1X = line.getPoint1X();
        this.mDownPoint1Y = line.getPoint1Y();
        this.mDownPoint2X = line.getPoint2X();
        this.mDownPoint2Y = line.getPoint2Y();
    }

    public void actionMove(int i, float f, float f2, Line line) {
        float[] fArr = {f, f2};
        this.mScrToImg.mapPoints(fArr);
        float f3 = fArr[0];
        float f4 = fArr[1];
        fArr[0] = 0.0f;
        fArr[1] = 1.0f;
        this.mScrToImg.mapVectors(fArr);
        int i2 = (fArr[0] > 0.0f ? 1 : (fArr[0] == 0.0f ? 0 : -1));
        float f5 = f3 - this.mDownX;
        float f6 = f4 - this.mDownY;
        switch (i) {
            case 0:
                if (!centerIsOutside(this.mDownPoint1X + f5, this.mDownPoint1Y + f6, this.mDownPoint2X + f5, this.mDownPoint2Y + f6)) {
                    line.setPoint1(this.mDownPoint1X + f5, this.mDownPoint1Y + f6);
                    line.setPoint2(this.mDownPoint2X + f5, this.mDownPoint2Y + f6);
                    break;
                }
                break;
            case 1:
                if (!centerIsOutside(this.mDownPoint1X + f5, this.mDownPoint1Y + f6, this.mDownPoint2X, this.mDownPoint2Y)) {
                    line.setPoint1(this.mDownPoint1X + f5, this.mDownPoint1Y + f6);
                    break;
                }
                break;
            case 2:
                if (!centerIsOutside(this.mDownPoint1X, this.mDownPoint1Y, this.mDownPoint2X + f5, this.mDownPoint2Y + f6)) {
                    line.setPoint2(this.mDownPoint2X + f5, this.mDownPoint2Y + f6);
                    break;
                }
                break;
        }
    }

    public void paintGrayPoint(Canvas canvas, float f, float f2) {
        if (isUndefined()) {
            return;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(f, f2, this.mCenterDotSize, this.mGrayPointColorPatern, this.mPointRadialPos, Shader.TileMode.CLAMP));
        canvas.drawCircle(f, f2, this.mCenterDotSize, paint);
    }

    public void paintPoint(Canvas canvas, float f, float f2) {
        if (isUndefined()) {
            return;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(f, f2, this.mCenterDotSize, this.mPointColorPatern, this.mPointRadialPos, Shader.TileMode.CLAMP));
        canvas.drawCircle(f, f2, this.mCenterDotSize, paint);
    }

    void paintLines(Canvas canvas, float f, float f2, float f3, float f4) {
        if (isUndefined()) {
            return;
        }
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(6.0f);
        this.mPaint.setColor(this.mlineShadowColor);
        this.mPaint.setPathEffect(this.mDash);
        paintOvallines(canvas, this.mPaint, f, f2, f3, f4);
        this.mPaint.setStrokeWidth(3.0f);
        this.mPaint.setColor(this.mLineColor);
        this.mPaint.setPathEffect(this.mDash);
        paintOvallines(canvas, this.mPaint, f, f2, f3, f4);
    }

    public void paintOvallines(Canvas canvas, Paint paint, float f, float f2, float f3, float f4) {
        canvas.drawLine(f, f2, f3, f4, paint);
        float f5 = f - f3;
        float f6 = f2 - f4;
        float fHypot = 2048.0f / ((float) Math.hypot(f5, f6));
        float f7 = f5 * fHypot;
        float f8 = f6 * fHypot;
        canvas.drawLine(f + f8, f2 - f7, f - f8, f2 + f7, paint);
        canvas.drawLine(f3 + f8, f4 - f7, f3 - f8, f4 + f7, paint);
    }

    public void fillHandles(Canvas canvas, float f, float f2, float f3, float f4) {
        this.handlex[0] = (f + f3) / 2.0f;
        this.handley[0] = (f2 + f4) / 2.0f;
        this.handlex[1] = f;
        this.handley[1] = f2;
        this.handlex[2] = f3;
        this.handley[2] = f4;
    }

    public void draw(Canvas canvas) {
        paintLines(canvas, this.mPoint1X, this.mPoint1Y, this.mPoint2X, this.mPoint2Y);
        fillHandles(canvas, this.mPoint1X, this.mPoint1Y, this.mPoint2X, this.mPoint2Y);
        paintPoint(canvas, this.mPoint2X, this.mPoint2Y);
        paintPoint(canvas, this.mPoint1X, this.mPoint1Y);
        paintPoint(canvas, (this.mPoint1X + this.mPoint2X) / 2.0f, (this.mPoint1Y + this.mPoint2Y) / 2.0f);
    }

    public boolean isUndefined() {
        return Float.isNaN(this.mPoint1X);
    }

    public void setShowReshapeHandles(boolean z) {
        this.mShowReshapeHandles = z;
    }
}
