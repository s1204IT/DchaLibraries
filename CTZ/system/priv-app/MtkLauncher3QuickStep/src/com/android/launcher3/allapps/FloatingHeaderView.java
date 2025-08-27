package com.android.launcher3.allapps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;

/* loaded from: classes.dex */
public class FloatingHeaderView extends LinearLayout implements ValueAnimator.AnimatorUpdateListener {
    private boolean mAllowTouchForwarding;
    private final ValueAnimator mAnimator;
    private final Rect mClip;
    private AllAppsRecyclerView mCurrentRV;
    private boolean mForwardToRecyclerView;
    private boolean mHeaderCollapsed;
    private AllAppsRecyclerView mMainRV;
    private boolean mMainRVActive;
    protected int mMaxTranslation;
    private final RecyclerView.OnScrollListener mOnScrollListener;
    private ViewGroup mParent;
    private int mSnappedScrolledY;
    protected ViewGroup mTabLayout;
    protected boolean mTabsHidden;
    private final Point mTempOffset;
    private int mTranslationY;
    private AllAppsRecyclerView mWorkRV;

    public FloatingHeaderView(@NonNull Context context) {
        this(context, null);
    }

    public FloatingHeaderView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mClip = new Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.mAnimator = ValueAnimator.ofInt(0, 0);
        this.mTempOffset = new Point();
        this.mOnScrollListener = new RecyclerView.OnScrollListener() { // from class: com.android.launcher3.allapps.FloatingHeaderView.1
            @Override // android.support.v7.widget.RecyclerView.OnScrollListener
            public void onScrollStateChanged(RecyclerView recyclerView, int i) {
            }

            @Override // android.support.v7.widget.RecyclerView.OnScrollListener
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                if (recyclerView == FloatingHeaderView.this.mCurrentRV) {
                    if (FloatingHeaderView.this.mAnimator.isStarted()) {
                        FloatingHeaderView.this.mAnimator.cancel();
                    }
                    FloatingHeaderView.this.moved(-FloatingHeaderView.this.mCurrentRV.getCurrentScrollY());
                    FloatingHeaderView.this.apply();
                }
            }
        };
        this.mMainRVActive = true;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTabLayout = (ViewGroup) findViewById(R.id.tabs);
    }

    public void setup(AllAppsContainerView.AdapterHolder[] adapterHolderArr, boolean z) {
        this.mTabsHidden = z;
        this.mTabLayout.setVisibility(z ? 8 : 0);
        this.mMainRV = setupRV(this.mMainRV, adapterHolderArr[0].recyclerView);
        boolean z2 = true;
        this.mWorkRV = setupRV(this.mWorkRV, adapterHolderArr[1].recyclerView);
        this.mParent = (ViewGroup) this.mMainRV.getParent();
        if (!this.mMainRVActive && this.mWorkRV != null) {
            z2 = false;
        }
        setMainActive(z2);
        reset(false);
    }

    private AllAppsRecyclerView setupRV(AllAppsRecyclerView allAppsRecyclerView, AllAppsRecyclerView allAppsRecyclerView2) {
        if (allAppsRecyclerView != allAppsRecyclerView2 && allAppsRecyclerView2 != null) {
            allAppsRecyclerView2.addOnScrollListener(this.mOnScrollListener);
        }
        return allAppsRecyclerView2;
    }

    public void setMainActive(boolean z) {
        this.mCurrentRV = z ? this.mMainRV : this.mWorkRV;
        this.mMainRVActive = z;
    }

    public int getMaxTranslation() {
        if (this.mMaxTranslation == 0 && this.mTabsHidden) {
            return getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_bottom_padding);
        }
        if (this.mMaxTranslation > 0 && this.mTabsHidden) {
            return this.mMaxTranslation + getPaddingTop();
        }
        return this.mMaxTranslation;
    }

    private boolean canSnapAt(int i) {
        return Math.abs(i) <= this.mMaxTranslation;
    }

    private void moved(int i) {
        if (this.mHeaderCollapsed) {
            if (i <= this.mSnappedScrolledY) {
                if (canSnapAt(i)) {
                    this.mSnappedScrolledY = i;
                }
            } else {
                this.mHeaderCollapsed = false;
            }
            this.mTranslationY = i;
            return;
        }
        if (!this.mHeaderCollapsed) {
            this.mTranslationY = (i - this.mSnappedScrolledY) - this.mMaxTranslation;
            if (this.mTranslationY >= 0) {
                this.mTranslationY = 0;
                this.mSnappedScrolledY = i - this.mMaxTranslation;
            } else if (this.mTranslationY <= (-this.mMaxTranslation)) {
                this.mHeaderCollapsed = true;
                this.mSnappedScrolledY = -this.mMaxTranslation;
            }
        }
    }

    protected void applyScroll(int i, int i2) {
    }

    protected void apply() {
        int i = this.mTranslationY;
        this.mTranslationY = Math.max(this.mTranslationY, -this.mMaxTranslation);
        applyScroll(i, this.mTranslationY);
        this.mTabLayout.setTranslationY(this.mTranslationY);
        this.mClip.top = this.mMaxTranslation + this.mTranslationY;
        this.mMainRV.setClipBounds(this.mClip);
        if (this.mWorkRV != null) {
            this.mWorkRV.setClipBounds(this.mClip);
        }
    }

    public void reset(boolean z) {
        if (this.mAnimator.isStarted()) {
            this.mAnimator.cancel();
        }
        if (z) {
            this.mAnimator.setIntValues(this.mTranslationY, 0);
            this.mAnimator.addUpdateListener(this);
            this.mAnimator.setDuration(150L);
            this.mAnimator.start();
        } else {
            this.mTranslationY = 0;
            apply();
        }
        this.mHeaderCollapsed = false;
        this.mSnappedScrolledY = -this.mMaxTranslation;
        this.mCurrentRV.scrollToTop();
    }

    public boolean isExpanded() {
        return !this.mHeaderCollapsed;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.mTranslationY = ((Integer) valueAnimator.getAnimatedValue()).intValue();
        apply();
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!this.mAllowTouchForwarding) {
            this.mForwardToRecyclerView = false;
            return super.onInterceptTouchEvent(motionEvent);
        }
        calcOffset(this.mTempOffset);
        motionEvent.offsetLocation(this.mTempOffset.x, this.mTempOffset.y);
        this.mForwardToRecyclerView = this.mCurrentRV.onInterceptTouchEvent(motionEvent);
        motionEvent.offsetLocation(-this.mTempOffset.x, -this.mTempOffset.y);
        return this.mForwardToRecyclerView || super.onInterceptTouchEvent(motionEvent);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mForwardToRecyclerView) {
            calcOffset(this.mTempOffset);
            motionEvent.offsetLocation(this.mTempOffset.x, this.mTempOffset.y);
            try {
                return this.mCurrentRV.onTouchEvent(motionEvent);
            } finally {
                motionEvent.offsetLocation(-this.mTempOffset.x, -this.mTempOffset.y);
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    private void calcOffset(Point point) {
        point.x = (getLeft() - this.mCurrentRV.getLeft()) - this.mParent.getLeft();
        point.y = (getTop() - this.mCurrentRV.getTop()) - this.mParent.getTop();
    }

    public void setContentVisibility(boolean z, boolean z2, PropertySetter propertySetter) {
        propertySetter.setViewAlpha(this, z2 ? 1.0f : 0.0f, Interpolators.LINEAR);
        allowTouchForwarding(z2);
    }

    protected void allowTouchForwarding(boolean z) {
        this.mAllowTouchForwarding = z;
    }

    public boolean hasVisibleContent() {
        return false;
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
