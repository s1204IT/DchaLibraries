package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.model.WidgetsModel;

public class WidgetsRecyclerView extends BaseRecyclerView {
    private BaseRecyclerView.ScrollPositionState mScrollPosState;
    private WidgetsModel mWidgets;

    public WidgetsRecyclerView(Context context) {
        this(context, null);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mScrollPosState = new BaseRecyclerView.ScrollPositionState();
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnItemTouchListener(this);
    }

    @Override
    public int getFastScrollerTrackColor(int defaultTrackColor) {
        return -1;
    }

    public void setWidgets(WidgetsModel widgets) {
        this.mWidgets = widgets;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.clipRect(this.mBackgroundPadding.left, this.mBackgroundPadding.top, getWidth() - this.mBackgroundPadding.right, getHeight() - this.mBackgroundPadding.bottom);
        super.dispatchDraw(canvas);
    }

    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        int rowCount;
        if (this.mWidgets == null || (rowCount = this.mWidgets.getPackageSize()) == 0) {
            return "";
        }
        stopScroll();
        getCurScrollState(this.mScrollPosState, -1);
        float pos = rowCount * touchFraction;
        int availableScrollHeight = getAvailableScrollHeight(rowCount);
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        layoutManager.scrollToPositionWithOffset(0, (int) (-(availableScrollHeight * touchFraction)));
        if (touchFraction == 1.0f) {
            pos -= 1.0f;
        }
        int posInt = (int) pos;
        PackageItemInfo p = this.mWidgets.getPackageItemInfo(posInt);
        return p.titleSectionName;
    }

    @Override
    public void onUpdateScrollbar(int dy) {
        if (this.mWidgets == null) {
            return;
        }
        int rowCount = this.mWidgets.getPackageSize();
        if (rowCount == 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        getCurScrollState(this.mScrollPosState, -1);
        if (this.mScrollPosState.rowIndex < 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
        } else {
            synchronizeScrollBarThumbOffsetToViewScroll(this.mScrollPosState, rowCount);
        }
    }

    protected void getCurScrollState(BaseRecyclerView.ScrollPositionState stateOut, int viewTypeMask) {
        View child;
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.itemPos = -1;
        if (this.mWidgets == null) {
            return;
        }
        int rowCount = this.mWidgets.getPackageSize();
        if (rowCount == 0 || (child = getChildAt(0)) == null) {
            return;
        }
        int position = getChildPosition(child);
        stateOut.rowIndex = position;
        stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
        stateOut.itemPos = position;
    }

    @Override
    protected int getTop(int rowIndex) {
        if (getChildCount() == 0) {
            return 0;
        }
        View child = getChildAt(0);
        return child.getMeasuredHeight() * rowIndex;
    }
}
