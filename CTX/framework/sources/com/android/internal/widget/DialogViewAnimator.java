package com.android.internal.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;
import java.util.ArrayList;

public class DialogViewAnimator extends ViewAnimator {
    private final ArrayList<View> mMatchParentChildren;

    public DialogViewAnimator(Context context) {
        super(context);
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    public DialogViewAnimator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int childMeasureSpec;
        int childMeasureSpec2;
        int measuredHeightAndState;
        int i3;
        boolean z = (View.MeasureSpec.getMode(i) == 1073741824 && View.MeasureSpec.getMode(i2) == 1073741824) ? false : true;
        int childCount = getChildCount();
        int iCombineMeasuredStates = 0;
        int i4 = 0;
        int iMax = 0;
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            if (getMeasureAllChildren() || childAt.getVisibility() != 8) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
                boolean z2 = layoutParams.width == -1;
                boolean z3 = layoutParams.height == -1;
                if (z && (z2 || z3)) {
                    this.mMatchParentChildren.add(childAt);
                }
                int i6 = iCombineMeasuredStates;
                int i7 = i4;
                measureChildWithMargins(childAt, i, 0, i2, 0);
                if (z && !z2) {
                    iMax = Math.max(iMax, childAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin);
                    measuredHeightAndState = (childAt.getMeasuredWidthAndState() & (-16777216)) | 0;
                } else {
                    measuredHeightAndState = 0;
                }
                if (!z || z3) {
                    i3 = i7;
                } else {
                    int iMax2 = Math.max(i7, childAt.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin);
                    measuredHeightAndState |= (childAt.getMeasuredHeightAndState() >> 16) & (-256);
                    i3 = iMax2;
                }
                iCombineMeasuredStates = combineMeasuredStates(i6, measuredHeightAndState);
                i4 = i3;
            }
        }
        int i8 = iCombineMeasuredStates;
        int paddingLeft = iMax + getPaddingLeft() + getPaddingRight();
        int iMax3 = Math.max(i4 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());
        int iMax4 = Math.max(paddingLeft, getSuggestedMinimumWidth());
        Drawable foreground = getForeground();
        if (foreground != null) {
            iMax3 = Math.max(iMax3, foreground.getMinimumHeight());
            iMax4 = Math.max(iMax4, foreground.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(iMax4, i, i8), resolveSizeAndState(iMax3, i2, i8 << 16));
        int size = this.mMatchParentChildren.size();
        for (int i9 = 0; i9 < size; i9++) {
            View view = this.mMatchParentChildren.get(i9);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            if (marginLayoutParams.width == -1) {
                childMeasureSpec = View.MeasureSpec.makeMeasureSpec((((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight()) - marginLayoutParams.leftMargin) - marginLayoutParams.rightMargin, 1073741824);
            } else {
                childMeasureSpec = getChildMeasureSpec(i, getPaddingLeft() + getPaddingRight() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, marginLayoutParams.width);
            }
            if (marginLayoutParams.height == -1) {
                childMeasureSpec2 = View.MeasureSpec.makeMeasureSpec((((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom()) - marginLayoutParams.topMargin) - marginLayoutParams.bottomMargin, 1073741824);
            } else {
                childMeasureSpec2 = getChildMeasureSpec(i2, getPaddingTop() + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, marginLayoutParams.height);
            }
            view.measure(childMeasureSpec, childMeasureSpec2);
        }
        this.mMatchParentChildren.clear();
    }
}
