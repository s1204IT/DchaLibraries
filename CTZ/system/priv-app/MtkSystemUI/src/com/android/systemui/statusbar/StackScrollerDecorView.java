package com.android.systemui.statusbar;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Interpolators;

/* loaded from: classes.dex */
public abstract class StackScrollerDecorView extends ExpandableView {
    protected View mContent;
    private boolean mContentAnimating;
    private final Runnable mContentVisibilityEndRunnable;
    private boolean mContentVisible;
    private int mDuration;
    private boolean mIsSecondaryVisible;
    private boolean mIsVisible;
    protected View mSecondaryView;

    protected abstract View findContentView();

    protected abstract View findSecondaryView();

    public static /* synthetic */ void lambda$new$0(StackScrollerDecorView stackScrollerDecorView) {
        stackScrollerDecorView.mContentAnimating = false;
        if (stackScrollerDecorView.getVisibility() != 8 && !stackScrollerDecorView.mIsVisible) {
            stackScrollerDecorView.setVisibility(8);
            stackScrollerDecorView.setWillBeGone(false);
            stackScrollerDecorView.notifyHeightChanged(false);
        }
    }

    public StackScrollerDecorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsVisible = true;
        this.mContentVisible = true;
        this.mIsSecondaryVisible = true;
        this.mDuration = 260;
        this.mContentVisibilityEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.-$$Lambda$StackScrollerDecorView$Z-vSQgmaL3Ks4ALN1cxIjM1burs
            @Override // java.lang.Runnable
            public final void run() {
                StackScrollerDecorView.lambda$new$0(this.f$0);
            }
        };
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findContentView();
        this.mSecondaryView = findSecondaryView();
        setVisible(false, false);
        setSecondaryVisible(false, false);
    }

    @Override // com.android.systemui.statusbar.ExpandableView, android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setOutlineProvider(null);
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isTransparent() {
        return true;
    }

    public void setContentVisible(boolean z) {
        setContentVisible(z, true);
    }

    private void setContentVisible(boolean z, boolean z2) {
        if (this.mContentVisible != z) {
            this.mContentAnimating = z2;
            setViewVisible(this.mContent, z, z2, this.mContentVisibilityEndRunnable);
            this.mContentVisible = z;
        }
        if (!this.mContentAnimating) {
            this.mContentVisibilityEndRunnable.run();
        }
    }

    public boolean isContentVisible() {
        return this.mContentVisible;
    }

    public void setVisible(boolean z, boolean z2) {
        if (this.mIsVisible != z) {
            this.mIsVisible = z;
            if (z2) {
                if (z) {
                    setVisibility(0);
                    setWillBeGone(false);
                    notifyHeightChanged(false);
                } else {
                    setWillBeGone(true);
                }
                setContentVisible(z, true);
                return;
            }
            setVisibility(z ? 0 : 8);
            setContentVisible(z, false);
            setWillBeGone(false);
            notifyHeightChanged(false);
        }
    }

    public void setSecondaryVisible(boolean z, boolean z2) {
        if (this.mIsSecondaryVisible != z) {
            setViewVisible(this.mSecondaryView, z, z2, null);
            this.mIsSecondaryVisible = z;
        }
    }

    @VisibleForTesting
    boolean isSecondaryVisible() {
        return this.mIsSecondaryVisible;
    }

    public boolean isVisible() {
        return this.mIsVisible;
    }

    private void setViewVisible(View view, boolean z, boolean z2, Runnable runnable) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        float f = z ? 1.0f : 0.0f;
        if (!z2) {
            view.setAlpha(f);
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        view.animate().alpha(f).setInterpolator(z ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT).setDuration(this.mDuration).withEndAction(runnable);
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void performRemoveAnimation(long j, long j2, float f, boolean z, float f2, Runnable runnable, AnimatorListenerAdapter animatorListenerAdapter) {
        setContentVisible(false);
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void performAddAnimation(long j, long j2, boolean z) {
        setContentVisible(true);
    }

    @Override // com.android.systemui.statusbar.ExpandableView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
