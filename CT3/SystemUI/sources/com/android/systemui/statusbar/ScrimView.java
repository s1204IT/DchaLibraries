package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

public class ScrimView extends View {
    private ValueAnimator mAlphaAnimator;
    private ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener;
    private Runnable mChangeRunnable;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mDrawAsSrc;
    private Rect mExcludedRect;
    private boolean mHasExcludedArea;
    private boolean mIsEmpty;
    private final Paint mPaint;
    private int mScrimColor;
    private float mViewAlpha;

    public ScrimView(Context context) {
        this(context, null);
    }

    public ScrimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScrimView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPaint = new Paint();
        this.mIsEmpty = true;
        this.mViewAlpha = 1.0f;
        this.mExcludedRect = new Rect();
        this.mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ScrimView.this.mViewAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                ScrimView.this.invalidate();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ScrimView.this.mAlphaAnimator = null;
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.mDrawAsSrc && (this.mIsEmpty || this.mViewAlpha <= 0.0f)) {
            return;
        }
        PorterDuff.Mode mode = this.mDrawAsSrc ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER;
        int color = getScrimColorWithAlpha();
        if (!this.mHasExcludedArea) {
            canvas.drawColor(color, mode);
            return;
        }
        this.mPaint.setColor(color);
        if (this.mExcludedRect.top > 0) {
            canvas.drawRect(0.0f, 0.0f, getWidth(), this.mExcludedRect.top, this.mPaint);
        }
        if (this.mExcludedRect.left > 0) {
            canvas.drawRect(0.0f, this.mExcludedRect.top, this.mExcludedRect.left, this.mExcludedRect.bottom, this.mPaint);
        }
        if (this.mExcludedRect.right < getWidth()) {
            canvas.drawRect(this.mExcludedRect.right, this.mExcludedRect.top, getWidth(), this.mExcludedRect.bottom, this.mPaint);
        }
        if (this.mExcludedRect.bottom >= getHeight()) {
            return;
        }
        canvas.drawRect(0.0f, this.mExcludedRect.bottom, getWidth(), getHeight(), this.mPaint);
    }

    public int getScrimColorWithAlpha() {
        int color = this.mScrimColor;
        return Color.argb((int) (Color.alpha(color) * this.mViewAlpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    public void setDrawAsSrc(boolean asSrc) {
        this.mDrawAsSrc = asSrc;
        this.mPaint.setXfermode(new PorterDuffXfermode(this.mDrawAsSrc ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER));
        invalidate();
    }

    public void setScrimColor(int color) {
        if (color == this.mScrimColor) {
            return;
        }
        this.mIsEmpty = Color.alpha(color) == 0;
        this.mScrimColor = color;
        invalidate();
        if (this.mChangeRunnable == null) {
            return;
        }
        this.mChangeRunnable.run();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void animateViewAlpha(float alpha, long durationOut, Interpolator interpolator) {
        if (this.mAlphaAnimator != null) {
            this.mAlphaAnimator.cancel();
        }
        this.mAlphaAnimator = ValueAnimator.ofFloat(this.mViewAlpha, alpha);
        this.mAlphaAnimator.addUpdateListener(this.mAlphaUpdateListener);
        this.mAlphaAnimator.addListener(this.mClearAnimatorListener);
        this.mAlphaAnimator.setInterpolator(interpolator);
        this.mAlphaAnimator.setDuration(durationOut);
        this.mAlphaAnimator.start();
    }

    public void setExcludedArea(Rect area) {
        boolean z = false;
        if (area == null) {
            this.mHasExcludedArea = false;
            invalidate();
            return;
        }
        int left = Math.max(area.left, 0);
        int top = Math.max(area.top, 0);
        int right = Math.min(area.right, getWidth());
        int bottom = Math.min(area.bottom, getHeight());
        this.mExcludedRect.set(left, top, right, bottom);
        if (left < right && top < bottom) {
            z = true;
        }
        this.mHasExcludedArea = z;
        invalidate();
    }

    public void setChangeRunnable(Runnable changeRunnable) {
        this.mChangeRunnable = changeRunnable;
    }
}
