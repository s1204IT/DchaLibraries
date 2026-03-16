package com.android.settings.dashboard;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;

public class DashboardContainerView extends ViewGroup {
    private float mCellGapX;
    private float mCellGapY;
    private int mNumColumns;
    private int mNumRows;

    public DashboardContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        this.mCellGapX = res.getDimension(R.dimen.dashboard_cell_gap_x);
        this.mCellGapY = res.getDimension(R.dimen.dashboard_cell_gap_y);
        this.mNumColumns = res.getInteger(R.integer.dashboard_num_columns);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = (int) (((width - getPaddingLeft()) - getPaddingRight()) - ((this.mNumColumns - 1) * this.mCellGapX));
        float cellWidth = (float) Math.ceil(availableWidth / this.mNumColumns);
        int N = getChildCount();
        int cellHeight = 0;
        int cursor = 0;
        for (int i = 0; i < N; i++) {
            DashboardTileView v = (DashboardTileView) getChildAt(i);
            if (v.getVisibility() != 8) {
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                int colSpan = v.getColumnSpan();
                lp.width = (int) ((colSpan * cellWidth) + ((colSpan - 1) * this.mCellGapX));
                int newWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
                int newHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
                v.measure(newWidthSpec, newHeightSpec);
                if (cellHeight <= 0) {
                    cellHeight = v.getMeasuredHeight();
                }
                lp.height = cellHeight;
                cursor += colSpan;
            }
        }
        this.mNumRows = (int) Math.ceil(cursor / this.mNumColumns);
        int newHeight = ((int) ((this.mNumRows * cellHeight) + ((this.mNumRows - 1) * this.mCellGapY))) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int N = getChildCount();
        boolean isLayoutRtl = isLayoutRtl();
        int width = getWidth();
        int x = getPaddingStart();
        int y = getPaddingTop();
        int cursor = 0;
        for (int i = 0; i < N; i++) {
            DashboardTileView child = (DashboardTileView) getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (child.getVisibility() != 8) {
                int col = cursor % this.mNumColumns;
                int colSpan = child.getColumnSpan();
                int childWidth = lp.width;
                int childHeight = lp.height;
                int row = cursor / this.mNumColumns;
                if (row == this.mNumRows - 1) {
                    child.setDividerVisibility(false);
                }
                if (col + colSpan > this.mNumColumns) {
                    x = getPaddingStart();
                    y = (int) (y + childHeight + this.mCellGapY);
                    row++;
                }
                int childLeft = isLayoutRtl ? (width - x) - childWidth : x;
                int childRight = childLeft + childWidth;
                int childTop = y;
                int childBottom = childTop + childHeight;
                child.layout(childLeft, childTop, childRight, childBottom);
                cursor += child.getColumnSpan();
                if (cursor < (row + 1) * this.mNumColumns) {
                    x = (int) (x + childWidth + this.mCellGapX);
                } else {
                    x = getPaddingStart();
                    y = (int) (y + childHeight + this.mCellGapY);
                }
            }
        }
    }
}
