package com.android.printspooler.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.printspooler.R;

public final class PrintOptionsLayout extends ViewGroup {
    private int mColumnCount;

    public PrintOptionsLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.PrintOptionsLayout);
        this.mColumnCount = typedArrayObtainStyledAttributes.getInteger(0, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setColumnCount(int i) {
        if (this.mColumnCount != i) {
            this.mColumnCount = i;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int childMeasureSpec;
        int i6;
        View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        if (size != 0) {
            i3 = ((size - ((View) this).mPaddingLeft) - ((View) this).mPaddingRight) / this.mColumnCount;
        } else {
            i3 = 0;
        }
        int childCount = getChildCount();
        int i7 = (childCount / this.mColumnCount) + (childCount % this.mColumnCount);
        int i8 = 0;
        int iMax = 0;
        int i9 = 0;
        int i10 = 0;
        while (i8 < i7) {
            int iCombineMeasuredStates = i10;
            int i11 = 0;
            int measuredWidth = 0;
            int iMax2 = 0;
            while (i11 < this.mColumnCount && (i4 = (this.mColumnCount * i8) + i11) < childCount) {
                View childAt = getChildAt(i4);
                if (childAt.getVisibility() == 8) {
                    i5 = i3;
                    i6 = childCount;
                } else {
                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) childAt.getLayoutParams();
                    if (i3 > 0) {
                        i5 = i3;
                        childMeasureSpec = View.MeasureSpec.makeMeasureSpec((i3 - marginLayoutParams.getMarginStart()) - marginLayoutParams.getMarginEnd(), 1073741824);
                    } else {
                        i5 = i3;
                        childMeasureSpec = getChildMeasureSpec(i, getPaddingStart() + getPaddingEnd() + iMax, ((ViewGroup.LayoutParams) marginLayoutParams).width);
                    }
                    i6 = childCount;
                    childAt.measure(childMeasureSpec, getChildMeasureSpec(i2, getPaddingTop() + getPaddingBottom() + i9, ((ViewGroup.LayoutParams) marginLayoutParams).height));
                    iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, childAt.getMeasuredState());
                    measuredWidth += childAt.getMeasuredWidth() + marginLayoutParams.getMarginStart() + marginLayoutParams.getMarginEnd();
                    iMax2 = Math.max(iMax2, childAt.getMeasuredHeight() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin);
                }
                i11++;
                i3 = i5;
                childCount = i6;
            }
            iMax = Math.max(iMax, measuredWidth);
            i9 += iMax2;
            i8++;
            i10 = iCombineMeasuredStates;
            i3 = i3;
            childCount = childCount;
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(iMax + getPaddingStart() + getPaddingEnd(), getMinimumWidth()), i, i10), resolveSizeAndState(Math.max(i9 + getPaddingTop() + getPaddingBottom(), getMinimumHeight()), i2, i10 << 16));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int childCount = getChildCount();
        int i6 = (childCount / this.mColumnCount) + (childCount % this.mColumnCount);
        boolean zIsLayoutRtl = isLayoutRtl();
        int paddingStart = getPaddingStart();
        int paddingTop = getPaddingTop();
        int paddingStart2 = paddingStart;
        for (int i7 = 0; i7 < i6; i7++) {
            int i8 = paddingStart2;
            int iMax = 0;
            for (int i9 = 0; i9 < this.mColumnCount; i9++) {
                if (zIsLayoutRtl) {
                    i5 = (this.mColumnCount * i7) + ((this.mColumnCount - i9) - 1);
                } else {
                    i5 = (this.mColumnCount * i7) + i9;
                }
                if (i5 >= childCount) {
                    break;
                }
                View childAt = getChildAt(i5);
                if (childAt.getVisibility() != 8) {
                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) childAt.getLayoutParams();
                    int marginStart = i8 + marginLayoutParams.getMarginStart();
                    int i10 = marginLayoutParams.topMargin + paddingTop;
                    int measuredWidth = childAt.getMeasuredWidth() + marginStart;
                    childAt.layout(marginStart, i10, measuredWidth, childAt.getMeasuredHeight() + i10);
                    int marginEnd = measuredWidth + marginLayoutParams.getMarginEnd();
                    iMax = Math.max(iMax, childAt.getMeasuredHeight() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin);
                    i8 = marginEnd;
                }
            }
            paddingStart2 = getPaddingStart();
            paddingTop += iMax;
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ViewGroup.MarginLayoutParams(getContext(), attributeSet);
    }
}
