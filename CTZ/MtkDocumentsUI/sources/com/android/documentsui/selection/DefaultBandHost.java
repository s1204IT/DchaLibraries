package com.android.documentsui.selection;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.documentsui.selection.BandSelectionHelper;
import com.android.internal.util.Preconditions;

public final class DefaultBandHost extends BandSelectionHelper.BandHost {
    private final Drawable mBand;
    private boolean mIsOverlayShown;
    private final RecyclerView mRecView;

    public DefaultBandHost(RecyclerView recyclerView, int i) {
        Preconditions.checkArgument(recyclerView != null);
        this.mRecView = recyclerView;
        this.mBand = this.mRecView.getContext().getTheme().getDrawable(i);
        Preconditions.checkArgument(this.mBand != null);
    }

    @Override
    public int getAdapterPositionAt(int i) {
        return this.mRecView.getChildAdapterPosition(this.mRecView.getChildAt(i));
    }

    @Override
    public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        this.mRecView.addOnScrollListener(onScrollListener);
    }

    @Override
    public void removeOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        this.mRecView.removeOnScrollListener(onScrollListener);
    }

    @Override
    public Point createAbsolutePoint(Point point) {
        return new Point(point.x + this.mRecView.computeHorizontalScrollOffset(), point.y + this.mRecView.computeVerticalScrollOffset());
    }

    @Override
    public Rect getAbsoluteRectForChildViewAt(int i) {
        View childAt = this.mRecView.getChildAt(i);
        Rect rect = new Rect();
        childAt.getHitRect(rect);
        rect.left += this.mRecView.computeHorizontalScrollOffset();
        rect.right += this.mRecView.computeHorizontalScrollOffset();
        rect.top += this.mRecView.computeVerticalScrollOffset();
        rect.bottom += this.mRecView.computeVerticalScrollOffset();
        return rect;
    }

    @Override
    public int getVisibleChildCount() {
        return this.mRecView.getChildCount();
    }

    @Override
    public int getColumnCount() {
        RecyclerView.LayoutManager layoutManager = this.mRecView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        }
        return 1;
    }

    @Override
    public int getHeight() {
        return this.mRecView.getHeight();
    }

    @Override
    public void invalidateView() {
        this.mRecView.invalidate();
    }

    @Override
    public void runAtNextFrame(Runnable runnable) {
        this.mRecView.postOnAnimation(runnable);
    }

    @Override
    public void removeCallback(Runnable runnable) {
        this.mRecView.removeCallbacks(runnable);
    }

    @Override
    public void scrollBy(int i) {
        this.mRecView.scrollBy(0, i);
    }

    @Override
    public void showBand(Rect rect) {
        this.mBand.setBounds(rect);
        if (!this.mIsOverlayShown) {
            this.mRecView.getOverlay().add(this.mBand);
        }
    }

    @Override
    public void hideBand() {
        this.mRecView.getOverlay().remove(this.mBand);
    }

    @Override
    public boolean hasView(int i) {
        return this.mRecView.findViewHolderForAdapterPosition(i) != null;
    }
}
