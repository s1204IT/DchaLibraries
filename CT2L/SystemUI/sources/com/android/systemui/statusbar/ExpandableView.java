package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.R;
import java.util.ArrayList;

public abstract class ExpandableView extends FrameLayout {
    private int mActualHeight;
    private boolean mActualHeightInitialized;
    protected int mClipTopAmount;
    private boolean mDark;
    private ArrayList<View> mMatchParentViews;
    private final int mMaxNotificationHeight;
    private OnHeightChangedListener mOnHeightChangedListener;

    public interface OnHeightChangedListener {
        void onHeightChanged(ExpandableView expandableView);

        void onReset(ExpandableView expandableView);
    }

    public abstract void performAddAnimation(long j, long j2);

    public abstract void performRemoveAnimation(long j, float f, Runnable runnable);

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMatchParentViews = new ArrayList<>();
        this.mMaxNotificationHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ownMaxHeight = this.mMaxNotificationHeight;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        if (hasFixedHeight || isHeightLimited) {
            int size = View.MeasureSpec.getSize(heightMeasureSpec);
            ownMaxHeight = Math.min(ownMaxHeight, size);
        }
        int newHeightSpec = View.MeasureSpec.makeMeasureSpec(ownMaxHeight, Integer.MIN_VALUE);
        int maxChildHeight = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int childHeightSpec = newHeightSpec;
            ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
            if (layoutParams.height != -1) {
                if (layoutParams.height >= 0) {
                    childHeightSpec = layoutParams.height > ownMaxHeight ? View.MeasureSpec.makeMeasureSpec(ownMaxHeight, 1073741824) : View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
                }
                child.measure(getChildMeasureSpec(widthMeasureSpec, 0, layoutParams.width), childHeightSpec);
                int childHeight = child.getMeasuredHeight();
                maxChildHeight = Math.max(maxChildHeight, childHeight);
            } else {
                this.mMatchParentViews.add(child);
            }
        }
        int ownHeight = hasFixedHeight ? ownMaxHeight : maxChildHeight;
        int newHeightSpec2 = View.MeasureSpec.makeMeasureSpec(ownHeight, 1073741824);
        for (View child2 : this.mMatchParentViews) {
            child2.measure(getChildMeasureSpec(widthMeasureSpec, 0, child2.getLayoutParams().width), newHeightSpec2);
        }
        this.mMatchParentViews.clear();
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, ownHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int initialHeight;
        super.onLayout(changed, left, top, right, bottom);
        if (!this.mActualHeightInitialized && this.mActualHeight == 0 && (initialHeight = getInitialHeight()) != 0) {
            setActualHeight(initialHeight);
        }
    }

    protected void resetActualHeight() {
        this.mActualHeight = 0;
        this.mActualHeightInitialized = false;
        requestLayout();
    }

    protected int getInitialHeight() {
        return getHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (filterMotionEvent(ev)) {
            return super.dispatchTouchEvent(ev);
        }
        return false;
    }

    protected boolean filterMotionEvent(MotionEvent event) {
        return event.getActionMasked() != 0 || (event.getY() > ((float) this.mClipTopAmount) && event.getY() < ((float) this.mActualHeight));
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        this.mActualHeightInitialized = true;
        this.mActualHeight = actualHeight;
        if (notifyListeners) {
            notifyHeightChanged();
        }
    }

    public void setActualHeight(int actualHeight) {
        setActualHeight(actualHeight, true);
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public int getMaxHeight() {
        return getHeight();
    }

    public int getMinHeight() {
        return getHeight();
    }

    public void setDimmed(boolean dimmed, boolean fade) {
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
    }

    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
    }

    public int getIntrinsicHeight() {
        return getHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
    }

    public int getClipTopAmount() {
        return this.mClipTopAmount;
    }

    public void setOnHeightChangedListener(OnHeightChangedListener listener) {
        this.mOnHeightChangedListener = listener;
    }

    public boolean isContentExpandable() {
        return false;
    }

    public void notifyHeightChanged() {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(this);
        }
    }

    public boolean isTransparent() {
        return false;
    }

    public void setBelowSpeedBump(boolean below) {
    }

    public void onHeightReset() {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onReset(this);
        }
    }

    @Override
    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        outRect.left = (int) (outRect.left + getTranslationX());
        outRect.right = (int) (outRect.right + getTranslationX());
        outRect.bottom = (int) (outRect.top + getTranslationY() + getActualHeight());
        outRect.top = (int) (outRect.top + getTranslationY() + getClipTopAmount());
    }
}
