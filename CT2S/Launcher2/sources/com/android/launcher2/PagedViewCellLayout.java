package com.android.launcher2;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import com.android.launcher.R;

public class PagedViewCellLayout extends ViewGroup implements Page {
    private int mCellCountX;
    private int mCellCountY;
    private int mCellHeight;
    private int mCellWidth;
    protected PagedViewCellLayoutChildren mChildren;
    private int mHeightGap;
    private int mMaxGap;
    private int mOriginalCellHeight;
    private int mOriginalCellWidth;
    private int mOriginalHeightGap;
    private int mOriginalWidthGap;
    private int mWidthGap;

    public PagedViewCellLayout(Context context) {
        this(context, null);
    }

    public PagedViewCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAlwaysDrawnWithCacheEnabled(false);
        Resources resources = context.getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.apps_customize_cell_width);
        this.mCellWidth = dimensionPixelSize;
        this.mOriginalCellWidth = dimensionPixelSize;
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.apps_customize_cell_height);
        this.mCellHeight = dimensionPixelSize2;
        this.mOriginalCellHeight = dimensionPixelSize2;
        this.mCellCountX = LauncherModel.getCellCountX();
        this.mCellCountY = LauncherModel.getCellCountY();
        this.mHeightGap = -1;
        this.mWidthGap = -1;
        this.mOriginalHeightGap = -1;
        this.mOriginalWidthGap = -1;
        this.mMaxGap = resources.getDimensionPixelSize(R.dimen.apps_customize_max_gap);
        this.mChildren = new PagedViewCellLayoutChildren(context);
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight);
        this.mChildren.setGap(this.mWidthGap, this.mHeightGap);
        addView(this.mChildren);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params) {
        if (params.cellX < 0 || params.cellX > this.mCellCountX - 1 || params.cellY < 0 || params.cellY > this.mCellCountY - 1) {
            return false;
        }
        if (params.cellHSpan < 0) {
            params.cellHSpan = this.mCellCountX;
        }
        if (params.cellVSpan < 0) {
            params.cellVSpan = this.mCellCountY;
        }
        child.setId(childId);
        this.mChildren.addView(child, index, params);
        return true;
    }

    @Override
    public void removeAllViewsOnPage() {
        this.mChildren.removeAllViews();
        setLayerType(0, null);
    }

    public void resetChildrenOnKeyListeners() {
        int childCount = this.mChildren.getChildCount();
        for (int j = 0; j < childCount; j++) {
            this.mChildren.getChildAt(j).setOnKeyListener(null);
        }
    }

    @Override
    public int getPageChildCount() {
        return this.mChildren.getChildCount();
    }

    public PagedViewCellLayoutChildren getChildrenLayout() {
        return this.mChildren;
    }

    public View getChildOnPageAt(int i) {
        return this.mChildren.getChildAt(i);
    }

    public int getCellCountX() {
        return this.mCellCountX;
    }

    public int getCellCountY() {
        return this.mCellCountY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == 0 || heightSpecMode == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        int numWidthGaps = this.mCellCountX - 1;
        int numHeightGaps = this.mCellCountY - 1;
        if (this.mOriginalWidthGap < 0 || this.mOriginalHeightGap < 0) {
            int hSpace = (widthSpecSize - getPaddingLeft()) - getPaddingRight();
            int vSpace = (heightSpecSize - getPaddingTop()) - getPaddingBottom();
            int hFreeSpace = hSpace - (this.mCellCountX * this.mOriginalCellWidth);
            int vFreeSpace = vSpace - (this.mCellCountY * this.mOriginalCellHeight);
            this.mWidthGap = Math.min(this.mMaxGap, numWidthGaps > 0 ? hFreeSpace / numWidthGaps : 0);
            this.mHeightGap = Math.min(this.mMaxGap, numHeightGaps > 0 ? vFreeSpace / numHeightGaps : 0);
            this.mChildren.setGap(this.mWidthGap, this.mHeightGap);
        } else {
            this.mWidthGap = this.mOriginalWidthGap;
            this.mHeightGap = this.mOriginalHeightGap;
        }
        int newWidth = widthSpecSize;
        int newHeight = heightSpecSize;
        if (widthSpecMode == Integer.MIN_VALUE) {
            newWidth = getPaddingLeft() + getPaddingRight() + (this.mCellCountX * this.mCellWidth) + ((this.mCellCountX - 1) * this.mWidthGap);
            newHeight = getPaddingTop() + getPaddingBottom() + (this.mCellCountY * this.mCellHeight) + ((this.mCellCountY - 1) * this.mHeightGap);
            setMeasuredDimension(newWidth, newHeight);
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((newWidth - getPaddingLeft()) - getPaddingRight(), 1073741824);
            int childheightMeasureSpec = View.MeasureSpec.makeMeasureSpec((newHeight - getPaddingTop()) - getPaddingBottom(), 1073741824);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }
        setMeasuredDimension(newWidth, newHeight);
    }

    int getContentWidth() {
        return getWidthBeforeFirstLayout() + getPaddingLeft() + getPaddingRight();
    }

    int getContentHeight() {
        if (this.mCellCountY <= 0) {
            return 0;
        }
        return (Math.max(0, this.mHeightGap) * (this.mCellCountY - 1)) + (this.mCellCountY * this.mCellHeight);
    }

    int getWidthBeforeFirstLayout() {
        if (this.mCellCountX <= 0) {
            return 0;
        }
        return (Math.max(0, this.mWidthGap) * (this.mCellCountX - 1)) + (this.mCellCountX * this.mCellWidth);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.layout(getPaddingLeft(), getPaddingTop(), (r - l) - getPaddingRight(), (b - t) - getPaddingBottom());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        int count = getPageChildCount();
        if (count > 0) {
            View child = getChildOnPageAt(count - 1);
            int bottom = child.getBottom();
            int numRows = (int) Math.ceil(getPageChildCount() / getCellCountX());
            if (numRows < getCellCountY()) {
                bottom += this.mCellHeight / 2;
            }
            return result || event.getY() < ((float) bottom);
        }
        return result;
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawingCacheEnabled(enabled);
    }

    public void setCellCount(int xCount, int yCount) {
        this.mCellCountX = xCount;
        this.mCellCountY = yCount;
        requestLayout();
    }

    public void setGap(int widthGap, int heightGap) {
        this.mWidthGap = widthGap;
        this.mOriginalWidthGap = widthGap;
        this.mHeightGap = heightGap;
        this.mOriginalHeightGap = heightGap;
        this.mChildren.setGap(widthGap, heightGap);
    }

    public int estimateCellHSpan(int width) {
        int availWidth = width - (getPaddingLeft() + getPaddingRight());
        int n = Math.max(1, (this.mWidthGap + availWidth) / (this.mCellWidth + this.mWidthGap));
        return n;
    }

    public int estimateCellVSpan(int height) {
        int availHeight = height - (getPaddingTop() + getPaddingBottom());
        int n = Math.max(1, (this.mHeightGap + availHeight) / (this.mCellHeight + this.mHeightGap));
        return n;
    }

    public int[] estimateCellPosition(int x, int y) {
        return new int[]{getPaddingLeft() + (this.mCellWidth * x) + (this.mWidthGap * x) + (this.mCellWidth / 2), getPaddingTop() + (this.mCellHeight * y) + (this.mHeightGap * y) + (this.mCellHeight / 2)};
    }

    public void calculateCellCount(int width, int height, int maxCellCountX, int maxCellCountY) {
        this.mCellCountX = Math.min(maxCellCountX, estimateCellHSpan(width));
        this.mCellCountY = Math.min(maxCellCountY, estimateCellVSpan(height));
        requestLayout();
    }

    public int estimateCellWidth(int hSpan) {
        return this.mCellWidth * hSpan;
    }

    public int estimateCellHeight(int vSpan) {
        return this.mCellHeight * vSpan;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        @ViewDebug.ExportedProperty
        public int cellHSpan;

        @ViewDebug.ExportedProperty
        public int cellVSpan;

        @ViewDebug.ExportedProperty
        public int cellX;

        @ViewDebug.ExportedProperty
        public int cellY;

        @ViewDebug.ExportedProperty
        int x;

        @ViewDebug.ExportedProperty
        int y;

        public LayoutParams() {
            super(-1, -1);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(-1, -1);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, int hStartPadding, int vStartPadding) {
            int myCellHSpan = this.cellHSpan;
            int myCellVSpan = this.cellVSpan;
            int myCellX = this.cellX;
            int myCellY = this.cellY;
            this.width = (((myCellHSpan * cellWidth) + ((myCellHSpan - 1) * widthGap)) - this.leftMargin) - this.rightMargin;
            this.height = (((myCellVSpan * cellHeight) + ((myCellVSpan - 1) * heightGap)) - this.topMargin) - this.bottomMargin;
            if (LauncherApplication.isScreenLarge()) {
                this.x = ((cellWidth + widthGap) * myCellX) + hStartPadding + this.leftMargin;
                this.y = ((cellHeight + heightGap) * myCellY) + vStartPadding + this.topMargin;
            } else {
                this.x = ((cellWidth + widthGap) * myCellX) + this.leftMargin;
                this.y = ((cellHeight + heightGap) * myCellY) + this.topMargin;
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ", " + this.cellHSpan + ", " + this.cellVSpan + ")";
        }
    }
}
