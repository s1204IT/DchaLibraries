package com.android.settings.applications;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class LinearColorBar extends LinearLayout {
    final Paint mColorGradientPaint;
    final Path mColorPath;
    private int mColoredRegions;
    final Paint mEdgeGradientPaint;
    final Path mEdgePath;
    private float mGreenRatio;
    int mLastInterestingLeft;
    int mLastInterestingRight;
    int mLastLeftDiv;
    int mLastRegion;
    int mLastRightDiv;
    private int mLeftColor;
    int mLineWidth;
    private int mMiddleColor;
    private OnRegionTappedListener mOnRegionTappedListener;
    final Paint mPaint;
    final Rect mRect;
    private float mRedRatio;
    private int mRightColor;
    private boolean mShowIndicator;
    private boolean mShowingGreen;
    private float mYellowRatio;

    public interface OnRegionTappedListener {
        void onRegionTapped(int i);
    }

    public LinearColorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLeftColor = -16738680;
        this.mMiddleColor = -16738680;
        this.mRightColor = -3221541;
        this.mShowIndicator = true;
        this.mColoredRegions = 7;
        this.mRect = new Rect();
        this.mPaint = new Paint();
        this.mColorPath = new Path();
        this.mEdgePath = new Path();
        this.mColorGradientPaint = new Paint();
        this.mEdgeGradientPaint = new Paint();
        setWillNotDraw(false);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mColorGradientPaint.setStyle(Paint.Style.FILL);
        this.mColorGradientPaint.setAntiAlias(true);
        this.mEdgeGradientPaint.setStyle(Paint.Style.STROKE);
        this.mLineWidth = getResources().getDisplayMetrics().densityDpi >= 240 ? 2 : 1;
        this.mEdgeGradientPaint.setStrokeWidth(this.mLineWidth);
        this.mEdgeGradientPaint.setAntiAlias(true);
    }

    public void setOnRegionTappedListener(OnRegionTappedListener listener) {
        if (listener == this.mOnRegionTappedListener) {
            return;
        }
        this.mOnRegionTappedListener = listener;
        setClickable(listener != null);
    }

    public void setColoredRegions(int regions) {
        this.mColoredRegions = regions;
        invalidate();
    }

    public void setRatios(float red, float yellow, float green) {
        this.mRedRatio = red;
        this.mYellowRatio = yellow;
        this.mGreenRatio = green;
        invalidate();
    }

    public void setColors(int red, int yellow, int green) {
        this.mLeftColor = red;
        this.mMiddleColor = yellow;
        this.mRightColor = green;
        updateIndicator();
        invalidate();
    }

    public void setShowIndicator(boolean showIndicator) {
        this.mShowIndicator = showIndicator;
        updateIndicator();
        invalidate();
    }

    private void updateIndicator() {
        int off = getPaddingTop() - getPaddingBottom();
        if (off < 0) {
            off = 0;
        }
        this.mRect.top = off;
        this.mRect.bottom = getHeight();
        if (!this.mShowIndicator) {
            return;
        }
        if (this.mShowingGreen) {
            this.mColorGradientPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, off - 2, this.mRightColor & 16777215, this.mRightColor, Shader.TileMode.CLAMP));
        } else {
            this.mColorGradientPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, off - 2, this.mMiddleColor & 16777215, this.mMiddleColor, Shader.TileMode.CLAMP));
        }
        this.mEdgeGradientPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, off / 2, 10526880, -6250336, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateIndicator();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mOnRegionTappedListener != null) {
            switch (event.getAction()) {
                case DefaultWfcSettingsExt.RESUME:
                    int x = (int) event.getX();
                    if (x < this.mLastLeftDiv) {
                        this.mLastRegion = 1;
                    } else if (x < this.mLastRightDiv) {
                        this.mLastRegion = 2;
                    } else {
                        this.mLastRegion = 4;
                    }
                    invalidate();
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        invalidate();
    }

    @Override
    public boolean performClick() {
        if (this.mOnRegionTappedListener != null && this.mLastRegion != 0) {
            this.mOnRegionTappedListener.onRegionTapped(this.mLastRegion);
            this.mLastRegion = 0;
        }
        return super.performClick();
    }

    private int pickColor(int color, int region) {
        if (isPressed() && (this.mLastRegion & region) != 0) {
            return -1;
        }
        if ((this.mColoredRegions & region) == 0) {
            return -11184811;
        }
        return color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int indicatorLeft;
        int indicatorRight;
        super.onDraw(canvas);
        int width = getWidth();
        int left = 0;
        int right = ((int) (width * this.mRedRatio)) + 0;
        int right2 = right + ((int) (width * this.mYellowRatio));
        int right3 = right2 + ((int) (width * this.mGreenRatio));
        if (this.mShowingGreen) {
            indicatorLeft = right2;
            indicatorRight = right3;
        } else {
            indicatorLeft = right;
            indicatorRight = right2;
        }
        if (this.mLastInterestingLeft != indicatorLeft || this.mLastInterestingRight != indicatorRight) {
            this.mColorPath.reset();
            this.mEdgePath.reset();
            if (this.mShowIndicator && indicatorLeft < indicatorRight) {
                int midTopY = this.mRect.top;
                this.mColorPath.moveTo(indicatorLeft, this.mRect.top);
                this.mColorPath.cubicTo(indicatorLeft, 0.0f, -2.0f, midTopY, -2.0f, 0.0f);
                this.mColorPath.lineTo((width + 2) - 1, 0.0f);
                this.mColorPath.cubicTo((width + 2) - 1, midTopY, indicatorRight, 0.0f, indicatorRight, this.mRect.top);
                this.mColorPath.close();
                float lineOffset = this.mLineWidth + 0.5f;
                this.mEdgePath.moveTo((-2.0f) + lineOffset, 0.0f);
                this.mEdgePath.cubicTo((-2.0f) + lineOffset, midTopY, indicatorLeft + lineOffset, 0.0f, indicatorLeft + lineOffset, this.mRect.top);
                this.mEdgePath.moveTo(((width + 2) - 1) - lineOffset, 0.0f);
                this.mEdgePath.cubicTo(((width + 2) - 1) - lineOffset, midTopY, indicatorRight - lineOffset, 0.0f, indicatorRight - lineOffset, this.mRect.top);
            }
            this.mLastInterestingLeft = indicatorLeft;
            this.mLastInterestingRight = indicatorRight;
        }
        if (!this.mEdgePath.isEmpty()) {
            canvas.drawPath(this.mEdgePath, this.mEdgeGradientPaint);
            canvas.drawPath(this.mColorPath, this.mColorGradientPaint);
        }
        if (right > 0) {
            this.mRect.left = 0;
            this.mRect.right = right;
            this.mPaint.setColor(pickColor(this.mLeftColor, 1));
            canvas.drawRect(this.mRect, this.mPaint);
            width -= right + 0;
            left = right;
        }
        this.mLastLeftDiv = right;
        this.mLastRightDiv = right2;
        if (left < right2) {
            this.mRect.left = left;
            this.mRect.right = right2;
            this.mPaint.setColor(pickColor(this.mMiddleColor, 2));
            canvas.drawRect(this.mRect, this.mPaint);
            width -= right2 - left;
            left = right2;
        }
        int right4 = left + width;
        if (left >= right4) {
            return;
        }
        this.mRect.left = left;
        this.mRect.right = right4;
        this.mPaint.setColor(pickColor(this.mRightColor, 4));
        canvas.drawRect(this.mRect, this.mPaint);
    }
}
