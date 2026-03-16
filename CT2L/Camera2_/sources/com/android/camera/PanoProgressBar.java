package com.android.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.camera.debug.Log;

class PanoProgressBar extends ImageView {
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_NONE = 0;
    public static final int DIRECTION_RIGHT = 2;
    private static final Log.Tag TAG = new Log.Tag("PanoProgressBar");
    private final Paint mBackgroundPaint;
    private int mDirection;
    private final Paint mDoneAreaPaint;
    private RectF mDrawBounds;
    private float mHeight;
    private final Paint mIndicatorPaint;
    private float mIndicatorWidth;
    private float mLeftMostProgress;
    private OnDirectionChangeListener mListener;
    private float mMaxProgress;
    private float mProgress;
    private float mProgressOffset;
    private float mRightMostProgress;
    private float mWidth;

    public interface OnDirectionChangeListener {
        void onDirectionChange(int i);
    }

    public PanoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mProgress = 0.0f;
        this.mMaxProgress = 0.0f;
        this.mLeftMostProgress = 0.0f;
        this.mRightMostProgress = 0.0f;
        this.mProgressOffset = 0.0f;
        this.mIndicatorWidth = 0.0f;
        this.mDirection = 0;
        this.mBackgroundPaint = new Paint();
        this.mDoneAreaPaint = new Paint();
        this.mIndicatorPaint = new Paint();
        this.mListener = null;
        this.mDoneAreaPaint.setStyle(Paint.Style.FILL);
        this.mDoneAreaPaint.setAlpha(MotionEventCompat.ACTION_MASK);
        this.mBackgroundPaint.setStyle(Paint.Style.FILL);
        this.mBackgroundPaint.setAlpha(MotionEventCompat.ACTION_MASK);
        this.mIndicatorPaint.setStyle(Paint.Style.FILL);
        this.mIndicatorPaint.setAlpha(MotionEventCompat.ACTION_MASK);
        this.mDrawBounds = new RectF();
    }

    public void setOnDirectionChangeListener(OnDirectionChangeListener l) {
        this.mListener = l;
    }

    private void setDirection(int direction) {
        if (this.mDirection != direction) {
            this.mDirection = direction;
            if (this.mListener != null) {
                this.mListener.onDirectionChange(this.mDirection);
            }
            invalidate();
        }
    }

    public int getDirection() {
        return this.mDirection;
    }

    @Override
    public void setBackgroundColor(int color) {
        this.mBackgroundPaint.setColor(color);
        invalidate();
    }

    public void setDoneColor(int color) {
        this.mDoneAreaPaint.setColor(color);
        invalidate();
    }

    public void setIndicatorColor(int color) {
        this.mIndicatorPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
        this.mDrawBounds.set(0.0f, 0.0f, this.mWidth, this.mHeight);
    }

    public void setMaxProgress(int progress) {
        this.mMaxProgress = progress;
    }

    public void setIndicatorWidth(float w) {
        this.mIndicatorWidth = w;
        invalidate();
    }

    public void setRightIncreasing(boolean rightIncreasing) {
        if (rightIncreasing) {
            this.mLeftMostProgress = 0.0f;
            this.mRightMostProgress = 0.0f;
            this.mProgressOffset = 0.0f;
            setDirection(2);
        } else {
            this.mLeftMostProgress = this.mWidth;
            this.mRightMostProgress = this.mWidth;
            this.mProgressOffset = this.mWidth;
            setDirection(1);
        }
        invalidate();
    }

    public void setProgress(int progress) {
        if (this.mDirection == 0) {
            if (progress > 10) {
                setRightIncreasing(true);
            } else if (progress < -10) {
                setRightIncreasing(false);
            }
        }
        if (this.mDirection != 0) {
            this.mProgress = ((progress * this.mWidth) / this.mMaxProgress) + this.mProgressOffset;
            this.mProgress = Math.min(this.mWidth, Math.max(0.0f, this.mProgress));
            if (this.mDirection == 2) {
                this.mRightMostProgress = Math.max(this.mRightMostProgress, this.mProgress);
            }
            if (this.mDirection == 1) {
                this.mLeftMostProgress = Math.min(this.mLeftMostProgress, this.mProgress);
            }
            invalidate();
        }
    }

    public void reset() {
        this.mProgress = 0.0f;
        this.mProgressOffset = 0.0f;
        setDirection(0);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float l;
        float r;
        canvas.drawRect(this.mDrawBounds, this.mBackgroundPaint);
        if (this.mDirection != 0) {
            canvas.drawRect(this.mLeftMostProgress, this.mDrawBounds.top, this.mRightMostProgress, this.mDrawBounds.bottom, this.mDoneAreaPaint);
            if (this.mDirection == 2) {
                l = Math.max(this.mProgress - this.mIndicatorWidth, 0.0f);
                r = this.mProgress;
            } else {
                l = this.mProgress;
                r = Math.min(this.mProgress + this.mIndicatorWidth, this.mWidth);
            }
            canvas.drawRect(l, this.mDrawBounds.top, r, this.mDrawBounds.bottom, this.mIndicatorPaint);
        }
        super.onDraw(canvas);
    }
}
