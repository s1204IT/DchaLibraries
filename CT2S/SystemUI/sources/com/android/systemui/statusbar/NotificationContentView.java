package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class NotificationContentView extends FrameLayout {
    private int mActualHeight;
    private boolean mAnimate;
    private final Rect mClipBounds;
    private int mClipTopAmount;
    private View mContractedChild;
    private boolean mContractedVisible;
    private NotificationViewWrapper mContractedWrapper;
    private boolean mDark;
    private ViewTreeObserver.OnPreDrawListener mEnableAnimationPredrawListener;
    private View mExpandedChild;
    private final Paint mFadePaint;
    private final Interpolator mLinearInterpolator;
    private int mSmallHeight;

    public NotificationContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mClipBounds = new Rect();
        this.mLinearInterpolator = new LinearInterpolator();
        this.mContractedVisible = true;
        this.mFadePaint = new Paint();
        this.mEnableAnimationPredrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                NotificationContentView.this.mAnimate = true;
                NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mFadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        reset(true);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateClipping();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public void reset(boolean resetActualHeight) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
        }
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
        }
        removeAllViews();
        this.mContractedChild = null;
        this.mExpandedChild = null;
        this.mSmallHeight = getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mContractedVisible = true;
        if (resetActualHeight) {
            this.mActualHeight = this.mSmallHeight;
        }
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public void setContractedChild(View child) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
            removeView(this.mContractedChild);
        }
        sanitizeContractedLayoutParams(child);
        addView(child);
        this.mContractedChild = child;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), child);
        selectLayout(false, true);
        this.mContractedWrapper.setDark(this.mDark, false, 0L);
    }

    public void setExpandedChild(View child) {
        if (this.mExpandedChild != null) {
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
        }
        addView(child);
        this.mExpandedChild = child;
        selectLayout(false, true);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    private void setVisible(boolean isVisible) {
        if (isVisible) {
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
        } else {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            this.mAnimate = false;
        }
    }

    public void setActualHeight(int actualHeight) {
        this.mActualHeight = actualHeight;
        selectLayout(this.mAnimate, false);
        updateClipping();
    }

    public int getMaxHeight() {
        return getHeight();
    }

    public int getMinHeight() {
        return this.mSmallHeight;
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        updateClipping();
    }

    private void updateClipping() {
        this.mClipBounds.set(0, this.mClipTopAmount, getWidth(), this.mActualHeight);
        setClipBounds(this.mClipBounds);
    }

    private void sanitizeContractedLayoutParams(View contractedChild) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) contractedChild.getLayoutParams();
        lp.height = this.mSmallHeight;
        contractedChild.setLayoutParams(lp);
    }

    private void selectLayout(boolean animate, boolean force) {
        if (this.mContractedChild != null) {
            boolean showContractedChild = showContractedChild();
            if (showContractedChild != this.mContractedVisible || force) {
                if (animate && this.mExpandedChild != null) {
                    runSwitchAnimation(showContractedChild);
                } else if (this.mExpandedChild != null) {
                    this.mContractedChild.setVisibility(showContractedChild ? 0 : 4);
                    this.mContractedChild.setAlpha(showContractedChild ? 1.0f : 0.0f);
                    this.mExpandedChild.setVisibility(showContractedChild ? 4 : 0);
                    this.mExpandedChild.setAlpha(showContractedChild ? 0.0f : 1.0f);
                }
            }
            this.mContractedVisible = showContractedChild;
        }
    }

    private void runSwitchAnimation(final boolean showContractedChild) {
        this.mContractedChild.setVisibility(0);
        this.mExpandedChild.setVisibility(0);
        this.mContractedChild.setLayerType(2, this.mFadePaint);
        this.mExpandedChild.setLayerType(2, this.mFadePaint);
        setLayerType(2, null);
        this.mContractedChild.animate().alpha(showContractedChild ? 1.0f : 0.0f).setDuration(170L).setInterpolator(this.mLinearInterpolator);
        this.mExpandedChild.animate().alpha(showContractedChild ? 0.0f : 1.0f).setDuration(170L).setInterpolator(this.mLinearInterpolator).withEndAction(new Runnable() {
            @Override
            public void run() {
                NotificationContentView.this.mContractedChild.setLayerType(0, null);
                NotificationContentView.this.mExpandedChild.setLayerType(0, null);
                NotificationContentView.this.setLayerType(0, null);
                NotificationContentView.this.mContractedChild.setVisibility(showContractedChild ? 0 : 4);
                NotificationContentView.this.mExpandedChild.setVisibility(showContractedChild ? 4 : 0);
            }
        });
    }

    private boolean showContractedChild() {
        return this.mActualHeight <= this.mSmallHeight || this.mExpandedChild == null;
    }

    public void notifyContentUpdated() {
        selectLayout(false, true);
        if (this.mContractedChild != null) {
            this.mContractedWrapper.notifyContentUpdated();
            this.mContractedWrapper.setDark(this.mDark, false, 0L);
        }
    }

    public boolean isContentExpandable() {
        return this.mExpandedChild != null;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mDark != dark && this.mContractedChild != null) {
            this.mDark = dark;
            this.mContractedWrapper.setDark(dark, fade, delay);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
