package com.android.systemui.statusbar;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class ExpandableView extends FrameLayout {
    private static Rect mClipRect = new Rect();
    private int mActualHeight;
    private boolean mChangingPosition;
    protected int mClipBottomAmount;
    private boolean mClipToActualHeight;
    protected int mClipTopAmount;
    private boolean mDark;
    private boolean mInShelf;
    private ArrayList<View> mMatchParentViews;
    private int mMinClipTopAmount;
    protected OnHeightChangedListener mOnHeightChangedListener;
    private boolean mTransformingInShelf;
    private ViewGroup mTransientContainer;
    private boolean mWillBeGone;

    public interface OnHeightChangedListener {
        void onHeightChanged(ExpandableView expandableView, boolean z);

        void onReset(ExpandableView expandableView);
    }

    public abstract void performAddAnimation(long j, long j2, boolean z);

    public abstract void performRemoveAnimation(long j, long j2, float f, boolean z, float f2, Runnable runnable, AnimatorListenerAdapter animatorListenerAdapter);

    public ExpandableView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMatchParentViews = new ArrayList<>();
        this.mMinClipTopAmount = 0;
        this.mClipToActualHeight = true;
        this.mChangingPosition = false;
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int iMakeMeasureSpec;
        int size = View.MeasureSpec.getSize(i2);
        int paddingStart = getPaddingStart() + getPaddingEnd();
        int mode = View.MeasureSpec.getMode(i2);
        int iMin = Integer.MAX_VALUE;
        if (mode != 0 && size != 0) {
            iMin = Math.min(size, Integer.MAX_VALUE);
        }
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(iMin, Integer.MIN_VALUE);
        int childCount = getChildCount();
        int iMax = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
                if (layoutParams.height != -1) {
                    if (layoutParams.height >= 0) {
                        if (layoutParams.height > iMin) {
                            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(iMin, 1073741824);
                        } else {
                            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
                        }
                    } else {
                        iMakeMeasureSpec = iMakeMeasureSpec2;
                    }
                    childAt.measure(getChildMeasureSpec(i, paddingStart, layoutParams.width), iMakeMeasureSpec);
                    iMax = Math.max(iMax, childAt.getMeasuredHeight());
                } else {
                    this.mMatchParentViews.add(childAt);
                }
            }
        }
        if (mode != 1073741824) {
            size = Math.min(iMin, iMax);
        }
        int iMakeMeasureSpec3 = View.MeasureSpec.makeMeasureSpec(size, 1073741824);
        Iterator<View> it = this.mMatchParentViews.iterator();
        while (it.hasNext()) {
            View next = it.next();
            next.measure(getChildMeasureSpec(i, paddingStart, next.getLayoutParams().width), iMakeMeasureSpec3);
        }
        this.mMatchParentViews.clear();
        setMeasuredDimension(View.MeasureSpec.getSize(i), size);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateClipping();
    }

    public boolean pointInView(float f, float f2, float f3) {
        return f >= (-f3) && f2 >= ((float) this.mClipTopAmount) - f3 && f < ((float) (this.mRight - this.mLeft)) + f3 && f2 < ((float) this.mActualHeight) + f3;
    }

    public void setActualHeight(int i, boolean z) {
        this.mActualHeight = i;
        updateClipping();
        if (z) {
            notifyHeightChanged(false);
        }
    }

    public void setDistanceToTopRoundness(float f) {
    }

    public void setActualHeight(int i) {
        setActualHeight(i, true);
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public boolean isExpandAnimationRunning() {
        return false;
    }

    public int getMaxContentHeight() {
        return getHeight();
    }

    public int getMinHeight() {
        return getMinHeight(false);
    }

    public int getMinHeight(boolean z) {
        return getHeight();
    }

    public int getCollapsedHeight() {
        return getHeight();
    }

    public void setDimmed(boolean z, boolean z2) {
    }

    public void setDark(boolean z, boolean z2, long j) {
        this.mDark = z;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public boolean isRemoved() {
        return false;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean z) {
    }

    public void setHideSensitive(boolean z, boolean z2, long j, long j2) {
    }

    public int getIntrinsicHeight() {
        return getHeight();
    }

    public void setClipTopAmount(int i) {
        this.mClipTopAmount = i;
        updateClipping();
    }

    public void setClipBottomAmount(int i) {
        this.mClipBottomAmount = i;
        updateClipping();
    }

    public int getClipTopAmount() {
        return this.mClipTopAmount;
    }

    public int getClipBottomAmount() {
        return this.mClipBottomAmount;
    }

    public void setOnHeightChangedListener(OnHeightChangedListener onHeightChangedListener) {
        this.mOnHeightChangedListener = onHeightChangedListener;
    }

    public boolean isContentExpandable() {
        return false;
    }

    public void notifyHeightChanged(boolean z) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(this, z);
        }
    }

    public boolean isTransparent() {
        return false;
    }

    public void setBelowSpeedBump(boolean z) {
    }

    public int getPinnedHeadsUpHeight() {
        return getIntrinsicHeight();
    }

    public void setTranslation(float f) {
        setTranslationX(f);
    }

    public float getTranslation() {
        return getTranslationX();
    }

    public void onHeightReset() {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onReset(this);
        }
    }

    @Override // android.view.View
    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        rect.left = (int) (rect.left + getTranslationX());
        rect.right = (int) (rect.right + getTranslationX());
        rect.bottom = (int) (rect.top + getTranslationY() + getActualHeight());
        rect.top = (int) (rect.top + getTranslationY() + getClipTopAmount());
    }

    public void getBoundsOnScreen(Rect rect, boolean z) {
        super.getBoundsOnScreen(rect, z);
        if (getTop() + getTranslationY() < 0.0f) {
            rect.top = (int) (rect.top + getTop() + getTranslationY());
        }
        rect.bottom = rect.top + getActualHeight();
        rect.top += getClipTopAmount();
    }

    public boolean isSummaryWithChildren() {
        return false;
    }

    public boolean areChildrenExpanded() {
        return false;
    }

    protected void updateClipping() {
        if (this.mClipToActualHeight && shouldClipToActualHeight()) {
            int clipTopAmount = getClipTopAmount();
            mClipRect.set(0, clipTopAmount, getWidth(), Math.max((getActualHeight() + getExtraBottomPadding()) - this.mClipBottomAmount, clipTopAmount));
            setClipBounds(mClipRect);
            return;
        }
        setClipBounds(null);
    }

    public float getHeaderVisibleAmount() {
        return 1.0f;
    }

    protected boolean shouldClipToActualHeight() {
        return true;
    }

    public void setClipToActualHeight(boolean z) {
        this.mClipToActualHeight = z;
        updateClipping();
    }

    public boolean willBeGone() {
        return this.mWillBeGone;
    }

    public void setWillBeGone(boolean z) {
        this.mWillBeGone = z;
    }

    public void setMinClipTopAmount(int i) {
        this.mMinClipTopAmount = i;
    }

    @Override // android.view.View
    public void setLayerType(int i, Paint paint) {
        if (hasOverlappingRendering()) {
            super.setLayerType(i, paint);
        }
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return super.hasOverlappingRendering() && getActualHeight() <= getHeight();
    }

    public float getShadowAlpha() {
        return 0.0f;
    }

    public void setShadowAlpha(float f) {
    }

    public float getIncreasedPaddingAmount() {
        return 0.0f;
    }

    public boolean mustStayOnScreen() {
        return false;
    }

    public void setFakeShadowIntensity(float f, float f2, int i, int i2) {
    }

    public float getOutlineAlpha() {
        return 0.0f;
    }

    public int getOutlineTranslation() {
        return 0;
    }

    public void setChangingPosition(boolean z) {
        this.mChangingPosition = z;
    }

    public boolean isChangingPosition() {
        return this.mChangingPosition;
    }

    public void setTransientContainer(ViewGroup viewGroup) {
        this.mTransientContainer = viewGroup;
    }

    public ViewGroup getTransientContainer() {
        return this.mTransientContainer;
    }

    public int getExtraBottomPadding() {
        return 0;
    }

    public boolean isGroupExpansionChanging() {
        return false;
    }

    public boolean isGroupExpanded() {
        return false;
    }

    public void setHeadsUpIsVisible() {
    }

    public boolean isChildInGroup() {
        return false;
    }

    public void setActualHeightAnimating(boolean z) {
    }

    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return new ExpandableViewState();
    }

    public boolean hasNoContentHeight() {
        return false;
    }

    public void setInShelf(boolean z) {
        this.mInShelf = z;
    }

    public boolean isInShelf() {
        return this.mInShelf;
    }

    public void setTransformingInShelf(boolean z) {
        this.mTransformingInShelf = z;
    }

    public boolean isTransformingIntoShelf() {
        return this.mTransformingInShelf;
    }

    public boolean isAboveShelf() {
        return false;
    }

    public boolean hasExpandingChild() {
        return false;
    }
}
