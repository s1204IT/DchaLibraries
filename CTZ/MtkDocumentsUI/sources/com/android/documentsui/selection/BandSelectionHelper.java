package com.android.documentsui.selection;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import com.android.documentsui.selection.GridModel;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.selection.ViewAutoScroller;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BandSelectionHelper implements RecyclerView.OnItemTouchListener {
    static final boolean $assertionsDisabled = false;
    private final RecyclerView.Adapter<?> mAdapter;
    private final BandPredicate mBandPredicate;
    private final List<Runnable> mBandStartedListeners = new ArrayList();
    private Rect mBounds;
    private Point mCurrentPosition;
    private final GridModel.SelectionObserver mGridObserver;
    private final BandHost mHost;
    private final ContentLock mLock;
    private GridModel mModel;
    private Point mOrigin;
    private final SelectionHelper mSelectionHelper;
    private final SelectionHelper.SelectionPredicate mSelectionPredicate;
    private final SelectionHelper.StableIdProvider mStableIds;
    private final Runnable mViewScroller;

    public static abstract class BandHost extends ViewAutoScroller.ScrollerCallbacks {
        public abstract void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener);

        public abstract Point createAbsolutePoint(Point point);

        public abstract Rect getAbsoluteRectForChildViewAt(int i);

        public abstract int getAdapterPositionAt(int i);

        public abstract int getColumnCount();

        public abstract int getHeight();

        public abstract int getVisibleChildCount();

        public abstract boolean hasView(int i);

        public abstract void hideBand();

        public abstract void invalidateView();

        public abstract void removeOnScrollListener(RecyclerView.OnScrollListener onScrollListener);

        public abstract void showBand(Rect rect);
    }

    public BandSelectionHelper(BandHost bandHost, RecyclerView.Adapter<?> adapter, SelectionHelper.StableIdProvider stableIdProvider, SelectionHelper selectionHelper, SelectionHelper.SelectionPredicate selectionPredicate, BandPredicate bandPredicate, ContentLock contentLock) {
        Preconditions.checkArgument(bandHost != null);
        Preconditions.checkArgument(adapter != null);
        Preconditions.checkArgument(stableIdProvider != null);
        Preconditions.checkArgument(selectionHelper != null);
        Preconditions.checkArgument(selectionPredicate != null);
        Preconditions.checkArgument(bandPredicate != null);
        Preconditions.checkArgument(contentLock != null);
        this.mHost = bandHost;
        this.mStableIds = stableIdProvider;
        this.mAdapter = adapter;
        this.mSelectionHelper = selectionHelper;
        this.mSelectionPredicate = selectionPredicate;
        this.mBandPredicate = bandPredicate;
        this.mLock = contentLock;
        this.mHost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                BandSelectionHelper.this.onScrolled(recyclerView, i, i2);
            }
        });
        this.mViewScroller = new ViewAutoScroller(new ViewAutoScroller.ScrollHost() {
            @Override
            public Point getCurrentPosition() {
                return BandSelectionHelper.this.mCurrentPosition;
            }

            @Override
            public int getViewHeight() {
                return BandSelectionHelper.this.mHost.getHeight();
            }

            @Override
            public boolean isActive() {
                return BandSelectionHelper.this.isActive();
            }
        }, bandHost);
        this.mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            static final boolean $assertionsDisabled = false;

            @Override
            public void onChanged() {
                if (BandSelectionHelper.this.isActive()) {
                    BandSelectionHelper.this.endBandSelect();
                }
            }

            @Override
            public void onItemRangeChanged(int i, int i2, Object obj) {
            }

            @Override
            public void onItemRangeInserted(int i, int i2) {
                if (BandSelectionHelper.this.isActive()) {
                    BandSelectionHelper.this.endBandSelect();
                }
            }

            @Override
            public void onItemRangeRemoved(int i, int i2) {
            }
        });
        this.mGridObserver = new GridModel.SelectionObserver() {
            @Override
            public void onSelectionChanged(Set<String> set) {
                BandSelectionHelper.this.mSelectionHelper.setProvisionalSelection(set);
            }
        };
    }

    boolean isActive() {
        boolean z = this.mModel != null;
        if (Build.IS_DEBUGGABLE && z) {
            this.mLock.checkLocked();
        }
        return z;
    }

    public void addOnBandStartedListener(Runnable runnable) {
        Preconditions.checkArgument(runnable != null);
        this.mBandStartedListeners.add(runnable);
    }

    public void removeOnBandStartedListener(Runnable runnable) {
        this.mBandStartedListeners.remove(runnable);
    }

    public void reset() {
        if (!isActive()) {
            return;
        }
        this.mHost.hideBand();
        this.mModel.stopCapturing();
        this.mModel.onDestroy();
        this.mModel = null;
        this.mOrigin = null;
        this.mLock.unblock();
    }

    boolean shouldStart(MotionEvent motionEvent) {
        if (!MotionEvents.isPrimaryButtonPressed(motionEvent)) {
            return false;
        }
        if (!isActive() || MotionEvents.isMouseEvent(motionEvent)) {
            return !isActive() && MotionEvents.isActionMove(motionEvent) && !this.mStableIds.getStableIds().isEmpty() && this.mBandPredicate.canInitiate(motionEvent);
        }
        endBandSelect();
        return false;
    }

    public boolean shouldStop(MotionEvent motionEvent) {
        return isActive() && MotionEvents.isMouseEvent(motionEvent) && (MotionEvents.isActionUp(motionEvent) || MotionEvents.isActionPointerUp(motionEvent) || MotionEvents.isActionCancel(motionEvent));
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (shouldStart(motionEvent)) {
            if (!MotionEvents.isCtrlKeyPressed(motionEvent)) {
                this.mSelectionHelper.clearSelection();
            }
            startBandSelect(MotionEvents.getOrigin(motionEvent));
            return isActive();
        }
        if (shouldStop(motionEvent)) {
            endBandSelect();
            Preconditions.checkState(this.mModel == null);
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (shouldStop(motionEvent)) {
            endBandSelect();
        } else {
            if (!isActive()) {
                return;
            }
            this.mCurrentPosition = MotionEvents.getOrigin(motionEvent);
            this.mModel.resizeSelection(this.mCurrentPosition);
            scrollViewIfNecessary();
            resizeBand();
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean z) {
    }

    private void startBandSelect(Point point) {
        reset();
        this.mModel = new GridModel(this.mHost, this.mStableIds, this.mSelectionPredicate);
        this.mModel.addOnSelectionChangedListener(this.mGridObserver);
        this.mLock.block();
        notifyBandStarted();
        this.mOrigin = point;
        this.mModel.startCapturing(this.mOrigin);
    }

    private void notifyBandStarted() {
        Iterator<Runnable> it = this.mBandStartedListeners.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
    }

    private void scrollViewIfNecessary() {
        this.mHost.removeCallback(this.mViewScroller);
        this.mViewScroller.run();
        this.mHost.invalidateView();
    }

    private void resizeBand() {
        this.mBounds = new Rect(Math.min(this.mOrigin.x, this.mCurrentPosition.x), Math.min(this.mOrigin.y, this.mCurrentPosition.y), Math.max(this.mOrigin.x, this.mCurrentPosition.x), Math.max(this.mOrigin.y, this.mCurrentPosition.y));
        this.mHost.showBand(this.mBounds);
    }

    private void endBandSelect() {
        int positionNearestOrigin = this.mModel.getPositionNearestOrigin();
        if (positionNearestOrigin != -1 && this.mSelectionHelper.isSelected(this.mStableIds.getStableId(positionNearestOrigin))) {
            this.mSelectionHelper.anchorRange(positionNearestOrigin);
        }
        this.mSelectionHelper.mergeProvisionalSelection();
        reset();
    }

    private void onScrolled(RecyclerView recyclerView, int i, int i2) {
        if (!isActive()) {
            return;
        }
        this.mOrigin.y -= i2;
        resizeBand();
    }
}
