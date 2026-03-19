package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;

class ControlBar extends LinearLayout {
    private int mChildMarginFromCenter;
    boolean mDefaultFocusToMiddle;
    int mLastFocusIndex;
    private OnChildFocusedListener mOnChildFocusedListener;

    public interface OnChildFocusedListener {
        void onChildFocusedListener(View view, View view2);
    }

    public ControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastFocusIndex = -1;
        this.mDefaultFocusToMiddle = true;
    }

    public ControlBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLastFocusIndex = -1;
        this.mDefaultFocusToMiddle = true;
    }

    int getDefaultFocusIndex() {
        if (this.mDefaultFocusToMiddle) {
            return getChildCount() / 2;
        }
        return 0;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (getChildCount() > 0) {
            int index = (this.mLastFocusIndex < 0 || this.mLastFocusIndex >= getChildCount()) ? getDefaultFocusIndex() : this.mLastFocusIndex;
            if (getChildAt(index).requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (direction == 33 || direction == 130) {
            if (this.mLastFocusIndex >= 0 && this.mLastFocusIndex < getChildCount()) {
                views.add(getChildAt(this.mLastFocusIndex));
                return;
            } else {
                if (getChildCount() > 0) {
                    views.add(getChildAt(getDefaultFocusIndex()));
                    return;
                }
                return;
            }
        }
        super.addFocusables(views, direction, focusableMode);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        this.mLastFocusIndex = indexOfChild(child);
        if (this.mOnChildFocusedListener != null) {
            this.mOnChildFocusedListener.onChildFocusedListener(child, focused);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mChildMarginFromCenter <= 0) {
            return;
        }
        int totalExtraMargin = 0;
        for (int i = 0; i < getChildCount() - 1; i++) {
            View first = getChildAt(i);
            View second = getChildAt(i + 1);
            int measuredWidth = first.getMeasuredWidth() + second.getMeasuredWidth();
            int marginStart = this.mChildMarginFromCenter - (measuredWidth / 2);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) second.getLayoutParams();
            int extraMargin = marginStart - lp.getMarginStart();
            lp.setMarginStart(marginStart);
            second.setLayoutParams(lp);
            totalExtraMargin += extraMargin;
        }
        int i2 = getMeasuredWidth();
        setMeasuredDimension(i2 + totalExtraMargin, getMeasuredHeight());
    }
}
