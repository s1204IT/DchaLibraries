package android.support.v7.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

class LayoutState {
    int mAvailable;
    int mCurrentPosition;
    int mExtra = 0;
    int mItemDirection;
    int mLayoutDirection;

    LayoutState() {
    }

    boolean hasMore(RecyclerView.State state) {
        return this.mCurrentPosition >= 0 && this.mCurrentPosition < state.getItemCount();
    }

    View next(RecyclerView.Recycler recycler) {
        View view = recycler.getViewForPosition(this.mCurrentPosition);
        this.mCurrentPosition += this.mItemDirection;
        return view;
    }
}
