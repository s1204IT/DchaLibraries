package com.android.launcher2;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;

public class PagedViewGridLayout extends GridLayout implements Page {
    private int mCellCountX;
    private int mCellCountY;
    private Runnable mOnLayoutListener;

    public PagedViewGridLayout(Context context, int cellCountX, int cellCountY) {
        super(context, null, 0);
        this.mCellCountX = cellCountX;
        this.mCellCountY = cellCountY;
    }

    int getCellCountX() {
        return this.mCellCountX;
    }

    int getCellCountY() {
        return this.mCellCountY;
    }

    public void resetChildrenOnKeyListeners() {
        int childCount = getChildCount();
        for (int j = 0; j < childCount; j++) {
            getChildAt(j).setOnKeyListener(null);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = Math.min(getSuggestedMinimumWidth(), View.MeasureSpec.getSize(widthMeasureSpec));
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(widthSpecSize, 1073741824), heightMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mOnLayoutListener = null;
    }

    public void setOnLayoutListener(Runnable r) {
        this.mOnLayoutListener = r;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mOnLayoutListener != null) {
            this.mOnLayoutListener.run();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        int count = getPageChildCount();
        if (count > 0) {
            View child = getChildOnPageAt(count - 1);
            int bottom = child.getBottom();
            return result || event.getY() < ((float) bottom);
        }
        return result;
    }

    @Override
    public void removeAllViewsOnPage() {
        removeAllViews();
        this.mOnLayoutListener = null;
        setLayerType(0, null);
    }

    @Override
    public int getPageChildCount() {
        return getChildCount();
    }

    public View getChildOnPageAt(int i) {
        return getChildAt(i);
    }
}
