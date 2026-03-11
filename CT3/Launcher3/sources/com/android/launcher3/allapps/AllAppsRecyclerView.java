package com.android.launcher3.allapps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Stats;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import java.util.List;

public class AllAppsRecyclerView extends BaseRecyclerView implements Stats.LaunchSourceProvider {
    private AlphabeticalAppsList mApps;
    private HeaderElevationController mElevationController;
    private AllAppsBackgroundDrawable mEmptySearchBackground;
    private int mEmptySearchBackgroundTopOffset;
    private AllAppsFastScrollHelper mFastScrollHelper;
    private int mIconHeight;
    private int mNumAppsPerRow;
    private int mPredictionIconHeight;
    private BaseRecyclerView.ScrollPositionState mScrollPosState;

    public AllAppsRecyclerView(Context context) {
        this(context, null);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        this.mScrollPosState = new BaseRecyclerView.ScrollPositionState();
        Resources res = getResources();
        addOnItemTouchListener(this);
        this.mScrollbar.setDetachThumbOnFastScroll();
        this.mEmptySearchBackgroundTopOffset = res.getDimensionPixelSize(R.dimen.all_apps_empty_search_bg_top_offset);
    }

    public void setApps(AlphabeticalAppsList apps) {
        this.mApps = apps;
        this.mFastScrollHelper = new AllAppsFastScrollHelper(this, apps);
    }

    public void setElevationController(HeaderElevationController elevationController) {
        this.mElevationController = elevationController;
    }

    public void setNumAppsPerRow(DeviceProfile grid, int numAppsPerRow) {
        this.mNumAppsPerRow = numAppsPerRow;
        RecyclerView.RecycledViewPool pool = getRecycledViewPool();
        int approxRows = (int) Math.ceil(grid.availableHeightPx / grid.allAppsIconSizePx);
        pool.setMaxRecycledViews(3, 1);
        pool.setMaxRecycledViews(4, 1);
        pool.setMaxRecycledViews(5, 1);
        pool.setMaxRecycledViews(1, this.mNumAppsPerRow * approxRows);
        pool.setMaxRecycledViews(2, this.mNumAppsPerRow);
        pool.setMaxRecycledViews(0, approxRows);
    }

    public void setPremeasuredIconHeights(int predictionIconHeight, int iconHeight) {
        this.mPredictionIconHeight = predictionIconHeight;
        this.mIconHeight = iconHeight;
    }

    public void scrollToTop() {
        if (this.mScrollbar.isThumbDetached()) {
            this.mScrollbar.reattachThumbToScroll();
        }
        scrollToPosition(0);
        if (this.mElevationController == null) {
            return;
        }
        this.mElevationController.reset();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.clipRect(this.mBackgroundPadding.left, this.mBackgroundPadding.top, getWidth() - this.mBackgroundPadding.right, getHeight() - this.mBackgroundPadding.bottom);
        super.dispatchDraw(canvas);
    }

    @Override
    public void onDraw(Canvas c) {
        if (this.mEmptySearchBackground != null && this.mEmptySearchBackground.getAlpha() > 0) {
            c.clipRect(this.mBackgroundPadding.left, this.mBackgroundPadding.top, getWidth() - this.mBackgroundPadding.right, getHeight() - this.mBackgroundPadding.bottom);
            this.mEmptySearchBackground.draw(c);
        }
        super.onDraw(c);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (who != this.mEmptySearchBackground) {
            return super.verifyDrawable(who);
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateEmptySearchBackgroundBounds();
    }

    @Override
    public void fillInLaunchSourceData(View v, Bundle sourceData) {
        sourceData.putString("container", "all_apps");
        if (this.mApps.hasFilter()) {
            sourceData.putString("sub_container", "search");
            return;
        }
        if (v instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) v;
            int position = getChildPosition(icon);
            if (position != -1) {
                List<AlphabeticalAppsList.AdapterItem> items = this.mApps.getAdapterItems();
                AlphabeticalAppsList.AdapterItem item = items.get(position);
                if (item.viewType == 2) {
                    sourceData.putString("sub_container", "prediction");
                    return;
                }
            }
        }
        sourceData.putString("sub_container", "a-z");
    }

    public void onSearchResultsChanged() {
        scrollToTop();
        if (this.mApps.hasNoFilteredResults()) {
            if (this.mEmptySearchBackground == null) {
                this.mEmptySearchBackground = new AllAppsBackgroundDrawable(getContext());
                this.mEmptySearchBackground.setAlpha(0);
                this.mEmptySearchBackground.setCallback(this);
                updateEmptySearchBackgroundBounds();
            }
            this.mEmptySearchBackground.animateBgAlpha(1.0f, 150);
            return;
        }
        if (this.mEmptySearchBackground == null) {
            return;
        }
        this.mEmptySearchBackground.setBgAlpha(0.0f);
    }

    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        int rowCount = this.mApps.getNumAppRows();
        if (rowCount == 0) {
            return "";
        }
        stopScroll();
        List<AlphabeticalAppsList.FastScrollSectionInfo> fastScrollSections = this.mApps.getFastScrollerSections();
        AlphabeticalAppsList.FastScrollSectionInfo lastInfo = fastScrollSections.get(0);
        for (int i = 1; i < fastScrollSections.size(); i++) {
            AlphabeticalAppsList.FastScrollSectionInfo info = fastScrollSections.get(i);
            if (info.touchFraction > touchFraction) {
                break;
            }
            lastInfo = info;
        }
        int scrollY = getScrollTop(this.mScrollPosState);
        int availableScrollHeight = getAvailableScrollHeight(this.mApps.getNumAppRows());
        this.mFastScrollHelper.smoothScrollToSection(scrollY, availableScrollHeight, lastInfo);
        return lastInfo.sectionName;
    }

    @Override
    public void onFastScrollCompleted() {
        super.onFastScrollCompleted();
        this.mFastScrollHelper.onFastScrollCompleted();
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        this.mFastScrollHelper.onSetAdapter((AllAppsGridAdapter) adapter);
    }

    @Override
    public void onUpdateScrollbar(int dy) {
        int scrollBarX;
        int thumbScrollY;
        List<AlphabeticalAppsList.AdapterItem> items = this.mApps.getAdapterItems();
        if (items.isEmpty() || this.mNumAppsPerRow == 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int rowCount = this.mApps.getNumAppRows();
        getCurScrollState(this.mScrollPosState, -1);
        if (this.mScrollPosState.rowIndex < 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight(this.mApps.getNumAppRows());
        if (availableScrollHeight <= 0) {
            this.mScrollbar.setThumbOffset(-1, -1);
            return;
        }
        int scrollY = getScrollTop(this.mScrollPosState);
        int scrollBarY = this.mBackgroundPadding.top + ((int) ((scrollY / availableScrollHeight) * availableScrollBarHeight));
        if (this.mScrollbar.isThumbDetached()) {
            if (Utilities.isRtl(getResources())) {
                scrollBarX = this.mBackgroundPadding.left;
            } else {
                scrollBarX = (getWidth() - this.mBackgroundPadding.right) - this.mScrollbar.getThumbWidth();
            }
            if (this.mScrollbar.isDraggingThumb()) {
                this.mScrollbar.setThumbOffset(scrollBarX, (int) this.mScrollbar.getLastTouchY());
                return;
            }
            int thumbScrollY2 = this.mScrollbar.getThumbOffset().y;
            int diffScrollY = scrollBarY - thumbScrollY2;
            if (diffScrollY * dy > 0.0f) {
                if (dy < 0) {
                    int offset = (int) ((dy * thumbScrollY2) / scrollBarY);
                    thumbScrollY = thumbScrollY2 + Math.max(offset, diffScrollY);
                } else {
                    int offset2 = (int) (((availableScrollBarHeight - thumbScrollY2) * dy) / (availableScrollBarHeight - scrollBarY));
                    thumbScrollY = thumbScrollY2 + Math.min(offset2, diffScrollY);
                }
                int thumbScrollY3 = Math.max(0, Math.min(availableScrollBarHeight, thumbScrollY));
                this.mScrollbar.setThumbOffset(scrollBarX, thumbScrollY3);
                if (scrollBarY != thumbScrollY3) {
                    return;
                }
                this.mScrollbar.reattachThumbToScroll();
                return;
            }
            this.mScrollbar.setThumbOffset(scrollBarX, thumbScrollY2);
            return;
        }
        synchronizeScrollBarThumbOffsetToViewScroll(this.mScrollPosState, rowCount);
    }

    protected void getCurScrollState(BaseRecyclerView.ScrollPositionState stateOut, int viewTypeMask) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.itemPos = -1;
        List<AlphabeticalAppsList.AdapterItem> items = this.mApps.getAdapterItems();
        if (items.isEmpty() || this.mNumAppsPerRow == 0) {
            return;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getChildPosition(child);
            if (position != -1) {
                AlphabeticalAppsList.AdapterItem item = items.get(position);
                if ((item.viewType & viewTypeMask) != 0) {
                    stateOut.rowIndex = item.rowIndex;
                    stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
                    stateOut.itemPos = position;
                    return;
                }
            }
        }
    }

    @Override
    protected boolean supportsFastScrolling() {
        return !this.mApps.hasFilter();
    }

    @Override
    protected int getTop(int rowIndex) {
        if (getChildCount() == 0 || rowIndex <= 0) {
            return 0;
        }
        return this.mPredictionIconHeight + ((rowIndex - 1) * this.mIconHeight);
    }

    private void updateEmptySearchBackgroundBounds() {
        if (this.mEmptySearchBackground == null) {
            return;
        }
        int x = (getMeasuredWidth() - this.mEmptySearchBackground.getIntrinsicWidth()) / 2;
        int y = this.mEmptySearchBackgroundTopOffset;
        this.mEmptySearchBackground.setBounds(x, y, this.mEmptySearchBackground.getIntrinsicWidth() + x, this.mEmptySearchBackground.getIntrinsicHeight() + y);
    }
}
