package com.android.deskclock.widget.sgv;

import android.graphics.Point;
import android.util.Log;
import android.view.View;
import com.android.deskclock.widget.sgv.StaggeredGridView;

public final class ReorderHelper {
    private ReorderView mCurrentDraggedOverChild;
    private ReorderView mDraggedChild;
    private long mDraggedChildId;
    private boolean mEnableUpdatesOnDrag;
    private final StaggeredGridView mParentView;
    private final StaggeredGridView.ReorderListener mReorderListener;

    public boolean handleDrop(Point p) {
        View reorderTarget = null;
        if (this.mCurrentDraggedOverChild != null) {
            reorderTarget = getReorderableChildAtCoordinate(p);
        } else {
            Log.w("DeskClock", "Current dragged over child does not exist");
        }
        if (reorderTarget != null) {
            StaggeredGridView.LayoutParams lp = (StaggeredGridView.LayoutParams) reorderTarget.getLayoutParams();
            if (lp.position != this.mCurrentDraggedOverChild.position) {
                updateDraggedOverChild(reorderTarget);
            }
        }
        if (this.mCurrentDraggedOverChild != null && this.mDraggedChild.position != this.mCurrentDraggedOverChild.position) {
            return this.mReorderListener.onReorder(this.mDraggedChild.target, this.mDraggedChild.id, this.mDraggedChild.position, this.mCurrentDraggedOverChild.position);
        }
        this.mReorderListener.onDrop(this.mDraggedChild.target, this.mDraggedChild.position, this.mCurrentDraggedOverChild.position);
        return false;
    }

    public void handleDragCancelled(View draggedView) {
        this.mReorderListener.onCancelDrag(draggedView);
    }

    public void handleDragStart(View view, int pos, long id, Point p) {
        this.mDraggedChild = new ReorderView(view, pos, id);
        this.mDraggedChildId = id;
        this.mCurrentDraggedOverChild = new ReorderView(view, pos, id);
        this.mReorderListener.onPickedUp(this.mDraggedChild.target);
    }

    public void handleDrag(Point p) {
        if (p == null || (p.y < 0 && p.y > this.mParentView.getHeight())) {
            handleDrop(p);
            return;
        }
        if (this.mEnableUpdatesOnDrag) {
            View reorderTarget = null;
            if (this.mCurrentDraggedOverChild != null) {
                reorderTarget = getReorderableChildAtCoordinate(p);
            } else {
                Log.w("DeskClock", "Current dragged over child does not exist");
            }
            if (reorderTarget != null) {
                StaggeredGridView.LayoutParams lp = (StaggeredGridView.LayoutParams) reorderTarget.getLayoutParams();
                if (lp.position != this.mCurrentDraggedOverChild.position) {
                    updateDraggedOverChild(reorderTarget);
                    this.mReorderListener.onEnterReorderArea(reorderTarget, lp.position);
                }
            }
        }
    }

    public void enableUpdatesOnDrag(boolean enabled) {
        this.mEnableUpdatesOnDrag = enabled;
    }

    public void clearDraggedOverChild() {
        this.mCurrentDraggedOverChild = null;
    }

    public boolean isOverReorderingArea() {
        return this.mCurrentDraggedOverChild != null;
    }

    public long getDraggedChildId() {
        return this.mDraggedChildId;
    }

    public View getDraggedChild() {
        if (this.mDraggedChild != null) {
            return this.mDraggedChild.target;
        }
        return null;
    }

    public void clearDraggedChild() {
        this.mDraggedChild = null;
    }

    public void clearDraggedChildId() {
        this.mDraggedChildId = -1L;
    }

    public int getDraggedChildPosition() {
        if (this.mDraggedChild != null) {
            return this.mDraggedChild.position;
        }
        return -2;
    }

    public void updateDraggedChildView(View v) {
        if (this.mDraggedChild != null && v != this.mDraggedChild.target) {
            this.mDraggedChild.target = v;
        }
    }

    public void updateDraggedOverChildView(View v) {
        if (this.mCurrentDraggedOverChild != null && v != this.mCurrentDraggedOverChild.target) {
            this.mCurrentDraggedOverChild.target = v;
        }
    }

    private void updateDraggedOverChild(View child) {
        StaggeredGridView.LayoutParams childLayoutParam = (StaggeredGridView.LayoutParams) child.getLayoutParams();
        this.mCurrentDraggedOverChild = new ReorderView(child, childLayoutParam.position, childLayoutParam.id);
    }

    public View getReorderableChildAtCoordinate(Point p) {
        if (p == null || p.y < 0) {
            return null;
        }
        int count = this.mParentView.getChildCount();
        for (int i = 0; i < count; i++) {
            if (this.mParentView.isChildReorderable(i)) {
                View childView = this.mParentView.getChildAt(i);
                if (p.x >= childView.getLeft() && p.x < childView.getRight() && p.y >= childView.getTop() && p.y < childView.getBottom()) {
                    return childView;
                }
            }
        }
        return null;
    }

    public boolean hasReorderListener() {
        return this.mReorderListener != null;
    }

    private class ReorderView {
        final long id;
        final int position;
        View target;

        public ReorderView(View v, int pos, long i) {
            this.target = v;
            this.position = pos;
            this.id = i;
        }
    }
}
