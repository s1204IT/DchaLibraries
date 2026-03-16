package com.android.printspooler.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.printspooler.R;

public final class PrintOptionsLayout extends ViewGroup {
    private int mColumnCount;

    public PrintOptionsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PrintOptionsLayout);
        this.mColumnCount = typedArray.getInteger(0, 0);
        typedArray.recycle();
    }

    public void setColumnCount(int columnCount) {
        if (this.mColumnCount != columnCount) {
            this.mColumnCount = columnCount;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childIndex;
        int childWidthMeasureSpec;
        View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int columnWidth = widthSize != 0 ? ((widthSize - this.mPaddingLeft) - this.mPaddingRight) / this.mColumnCount : 0;
        int width = 0;
        int height = 0;
        int childState = 0;
        int childCount = getChildCount();
        int rowCount = (childCount / this.mColumnCount) + (childCount % this.mColumnCount);
        for (int row = 0; row < rowCount; row++) {
            int rowWidth = 0;
            int rowHeight = 0;
            for (int col = 0; col < this.mColumnCount && (childIndex = (this.mColumnCount * row) + col) < childCount; col++) {
                View child = getChildAt(childIndex);
                if (child.getVisibility() != 8) {
                    ViewGroup.MarginLayoutParams childParams = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    if (columnWidth > 0) {
                        childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((columnWidth - childParams.getMarginStart()) - childParams.getMarginEnd(), 1073741824);
                    } else {
                        childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingStart() + getPaddingEnd() + width, childParams.width);
                    }
                    int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + height, childParams.height);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    childState = combineMeasuredStates(childState, child.getMeasuredState());
                    rowWidth += child.getMeasuredWidth() + childParams.getMarginStart() + childParams.getMarginEnd();
                    rowHeight = Math.max(rowHeight, child.getMeasuredHeight() + childParams.topMargin + childParams.bottomMargin);
                }
            }
            width = Math.max(width, rowWidth);
            height += rowHeight;
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(width + getPaddingStart() + getPaddingEnd(), getMinimumWidth()), widthMeasureSpec, childState), resolveSizeAndState(Math.max(height + getPaddingTop() + getPaddingBottom(), getMinimumHeight()), heightMeasureSpec, childState << 16));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childIndex;
        int childCount = getChildCount();
        int rowCount = (childCount / this.mColumnCount) + (childCount % this.mColumnCount);
        int cellStart = getPaddingStart();
        int cellTop = getPaddingTop();
        for (int row = 0; row < rowCount; row++) {
            int rowHeight = 0;
            for (int col = 0; col < this.mColumnCount && (childIndex = (this.mColumnCount * row) + col) < childCount; col++) {
                View child = getChildAt(childIndex);
                if (child.getVisibility() != 8) {
                    ViewGroup.MarginLayoutParams childParams = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    int childLeft = cellStart + childParams.getMarginStart();
                    int childTop = cellTop + childParams.topMargin;
                    int childRight = childLeft + child.getMeasuredWidth();
                    int childBottom = childTop + child.getMeasuredHeight();
                    child.layout(childLeft, childTop, childRight, childBottom);
                    cellStart = childRight + childParams.getMarginEnd();
                    rowHeight = Math.max(rowHeight, child.getMeasuredHeight() + childParams.topMargin + childParams.bottomMargin);
                }
            }
            cellStart = getPaddingStart();
            cellTop += rowHeight;
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }
}
