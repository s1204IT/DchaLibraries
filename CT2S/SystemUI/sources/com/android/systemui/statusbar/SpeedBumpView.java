package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.R;

public class SpeedBumpView extends ExpandableView {
    private final Interpolator mFastOutSlowInInterpolator;
    private boolean mIsVisible;
    private AlphaOptimizedView mLine;
    private final int mSpeedBumpHeight;

    public SpeedBumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsVisible = true;
        this.mSpeedBumpHeight = getResources().getDimensionPixelSize(R.dimen.speed_bump_height);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_slow_in);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLine = (AlphaOptimizedView) findViewById(R.id.speedbump_line);
    }

    @Override
    protected int getInitialHeight() {
        return this.mSpeedBumpHeight;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mSpeedBumpHeight;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mLine.setPivotX(this.mLine.getWidth() / 2);
        this.mLine.setPivotY(this.mLine.getHeight() / 2);
        setOutlineProvider(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int height = this.mSpeedBumpHeight;
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible, long delay) {
        animateDivider(nowVisible, delay, null);
    }

    public void animateDivider(boolean nowVisible, long delay, Runnable onFinishedRunnable) {
        if (nowVisible != this.mIsVisible) {
            float endValue = nowVisible ? 1.0f : 0.0f;
            this.mLine.animate().alpha(endValue).setStartDelay(delay).scaleX(endValue).scaleY(endValue).setInterpolator(this.mFastOutSlowInInterpolator).withEndAction(onFinishedRunnable);
            this.mIsVisible = nowVisible;
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    public void setInvisible() {
        this.mLine.setAlpha(0.0f);
        this.mLine.setScaleX(0.0f);
        this.mLine.setScaleY(0.0f);
        this.mIsVisible = false;
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {
        performVisibilityAnimation(false, 0L);
    }

    @Override
    public void performAddAnimation(long delay, long duration) {
        performVisibilityAnimation(true, delay);
    }
}
