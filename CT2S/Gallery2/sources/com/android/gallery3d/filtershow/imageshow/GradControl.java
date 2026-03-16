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
        Resources res = context.getResources();
        this.mCenterDotSize = (int) res.getDimension(R.dimen.gradcontrol_dot_size);
        this.mMinTouchDist = (int) res.getDimension(R.dimen.gradcontrol_min_touch_dist);
        int grayPointCenterColor = res.getColor(R.color.gradcontrol_graypoint_center);
        int grayPointEdgeColor = res.getColor(R.color.gradcontrol_graypoint_edge);
        int pointCenterColor = res.getColor(R.color.gradcontrol_point_center);
        int pointEdgeColor = res.getColor(R.color.gradcontrol_point_edge);
        int pointShadowStartColor = res.getColor(R.color.gradcontrol_point_shadow_start);
        int pointShadowEndColor = res.getColor(R.color.gradcontrol_point_shadow_end);
        this.mPointColorPatern = new int[]{pointCenterColor, pointEdgeColor, pointShadowStartColor, pointShadowEndColor};
        this.mGrayPointColorPatern = new int[]{grayPointCenterColor, grayPointEdgeColor, pointShadowStartColor, pointShadowEndColor};
        this.mSliderColor = -1;
        this.mLineColor = res.getColor(R.color.gradcontrol_line_color);
        this.mlineShadowColor = res.getColor(R.color.gradcontrol_line_shadow);
    }

    public void setPoint2(float x, float y) {
        this.mPoint2X = x;
        this.mPoint2Y = y;
    }

    public void setPoint1(float x, float y) {
        this.mPoint1X = x;
        this.mPoint1Y = y;
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
        if (min >= this.mMinTouchDist * this.mMinTouchDist) {
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

    private boolean centerIsOutside(float x1, float y1, float x2, float y2) {
        return !this.mImageBounds.contains((int) ((x1 + x2) / 2.0f), (int) ((y1 + y2) / 2.0f));
    }

    public void actionDown(float x, float y, Line line) {
        float[] point = {x, y};
        this.mScrToImg.mapPoints(point);
        this.mDownX = point[0];
        this.mDownY = point[1];
        this.mDownPoint1X = line.getPoint1X();
        this.mDownPoint1Y = line.getPoint1Y();
        this.mDownPoint2X = line.getPoint2X();
        this.mDownPoint2Y = line.getPoint2Y();
    }

    public void actionMove(int handle, float x, float y, Line line) {
        float[] point = {x, y};
        this.mScrToImg.mapPoints(point);
        float x2 = point[0];
        float y2 = point[1];
        point[0] = 0.0f;
        point[1] = 1.0f;
        this.mScrToImg.mapVectors(point);
        if (point[0] <= 0.0f) {
        }
        float dx = x2 - this.mDownX;
        float dy = y2 - this.mDownY;
        switch (handle) {
            case 0:
                if (!centerIsOutside(this.mDownPoint1X + dx, this.mDownPoint1Y + dy, this.mDownPoint2X + dx, this.mDownPoint2Y + dy)) {
                    line.setPoint1(this.mDownPoint1X + dx, this.mDownPoint1Y + dy);
                    line.setPoint2(this.mDownPoint2X + dx, this.mDownPoint2Y + dy);
                }
                break;
            case 1:
                if (!centerIsOutside(this.mDownPoint1X + dx, this.mDownPoint1Y + dy, this.mDownPoint2X, this.mDownPoint2Y)) {
                    line.setPoint1(this.mDownPoint1X + dx, this.mDownPoint1Y + dy);
                }
                break;
            case 2:
                if (!centerIsOutside(this.mDownPoint1X, this.mDownPoint1Y, this.mDownPoint2X + dx, this.mDownPoint2Y + dy)) {
                    line.setPoint2(this.mDownPoint2X + dx, this.mDownPoint2Y + dy);
                }
                break;
        }
    }

    public void paintGrayPoint(Canvas canvas, float x, float y) {
        if (!isUndefined()) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            RadialGradient g = new RadialGradient(x, y, this.mCenterDotSize, this.mGrayPointColorPatern, this.mPointRadialPos, Shader.TileMode.CLAMP);
            paint.setShader(g);
            canvas.drawCircle(x, y, this.mCenterDotSize, paint);
        }
    }

    public void paintPoint(Canvas canvas, float x, float y) {
        if (!isUndefined()) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            RadialGradient g = new RadialGradient(x, y, this.mCenterDotSize, this.mPointColorPatern, this.mPointRadialPos, Shader.TileMode.CLAMP);
            paint.setShader(g);
            canvas.drawCircle(x, y, this.mCenterDotSize, paint);
        }
    }

    void paintLines(Canvas canvas, float p1x, float p1y, float p2x, float p2y) {
        if (!isUndefined()) {
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStyle(Paint.Style.STROKE);
            this.mPaint.setStrokeWidth(6.0f);
            this.mPaint.setColor(this.mlineShadowColor);
            this.mPaint.setPathEffect(this.mDash);
            paintOvallines(canvas, this.mPaint, p1x, p1y, p2x, p2y);
            this.mPaint.setStrokeWidth(3.0f);
            this.mPaint.setColor(this.mLineColor);
            this.mPaint.setPathEffect(this.mDash);
            paintOvallines(canvas, this.mPaint, p1x, p1y, p2x, p2y);
        }
    }

    public void paintOvallines(Canvas canvas, Paint paint, float p1x, float p1y, float p2x, float p2y) {
        canvas.drawLine(p1x, p1y, p2x, p2y, paint);
        float f = (p1x + p2x) / 2.0f;
        float f2 = (p1y + p2y) / 2.0f;
        float len = (float) Math.sqrt((r8 * r8) + (r9 * r9));
        float dx = (p1x - p2x) * (2048.0f / len);
        float dy = (p1y - p2y) * (2048.0f / len);
        canvas.drawLine(p1x + dy, p1y - dx, p1x - dy, p1y + dx, paint);
        canvas.drawLine(p2x + dy, p2y - dx, p2x - dy, p2y + dx, paint);
    }

    public void fillHandles(Canvas canvas, float p1x, float p1y, float p2x, float p2y) {
        float cx = (p1x + p2x) / 2.0f;
        float cy = (p1y + p2y) / 2.0f;
        this.handlex[0] = cx;
        this.handley[0] = cy;
        this.handlex[1] = p1x;
        this.handley[1] = p1y;
        this.handlex[2] = p2x;
        this.handley[2] = p2y;
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

    public void setShowReshapeHandles(boolean showReshapeHandles) {
        this.mShowReshapeHandles = showReshapeHandles;
    }
}
