package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;

public abstract class StackScrollerDecorView extends ExpandableView {
    private boolean mAnimating;
    protected View mContent;
    private boolean mIsVisible;

    protected abstract View findContentView();

    public StackScrollerDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findContentView();
        setInvisible();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setOutlineProvider(null);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible) {
        animateText(nowVisible, null);
    }

    public void performVisibilityAnimation(boolean nowVisible, Runnable onFinishedRunnable) {
        animateText(nowVisible, onFinishedRunnable);
    }

    public boolean isVisible() {
        if (this.mIsVisible) {
            return true;
        }
        return this.mAnimating;
    }

    private void animateText(boolean nowVisible, final Runnable onFinishedRunnable) {
        Interpolator interpolator;
        if (nowVisible != this.mIsVisible) {
            float endValue = nowVisible ? 1.0f : 0.0f;
            if (nowVisible) {
                interpolator = Interpolators.ALPHA_IN;
            } else {
                interpolator = Interpolators.ALPHA_OUT;
            }
            this.mAnimating = true;
            this.mContent.animate().alpha(endValue).setInterpolator(interpolator).setDuration(260L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    StackScrollerDecorView.this.mAnimating = false;
                    if (onFinishedRunnable == null) {
                        return;
                    }
                    onFinishedRunnable.run();
                }
            });
            this.mIsVisible = nowVisible;
            return;
        }
        if (onFinishedRunnable == null) {
            return;
        }
        onFinishedRunnable.run();
    }

    public void setInvisible() {
        this.mContent.setAlpha(0.0f);
        this.mIsVisible = false;
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {
        performVisibilityAnimation(false);
    }

    @Override
    public void performAddAnimation(long delay, long duration) {
        performVisibilityAnimation(true);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void cancelAnimation() {
        this.mContent.animate().cancel();
    }
}
