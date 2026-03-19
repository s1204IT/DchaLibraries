package com.android.documentsui.selection.demo;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import com.android.documentsui.selection.ItemDetailsLookup;

final class DemoDetailsLookup extends ItemDetailsLookup {
    private final RecyclerView mRecView;

    public DemoDetailsLookup(RecyclerView recyclerView) {
        this.mRecView = recyclerView;
    }

    @Override
    public boolean overItem(MotionEvent motionEvent) {
        return getItemPosition(motionEvent) != -1;
    }

    @Override
    public boolean overStableItem(MotionEvent motionEvent) {
        return overItem(motionEvent) && getItemDetails(motionEvent).hasStableId();
    }

    @Override
    public boolean inItemDragRegion(MotionEvent motionEvent) {
        return overItem(motionEvent) && getItemDetails(motionEvent).inDragRegion(motionEvent);
    }

    public int getItemPosition(MotionEvent motionEvent) {
        View viewFindChildViewUnder = this.mRecView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        if (viewFindChildViewUnder != null) {
            return this.mRecView.getChildAdapterPosition(viewFindChildViewUnder);
        }
        return -1;
    }

    @Override
    public ItemDetailsLookup.ItemDetails getItemDetails(MotionEvent motionEvent) {
        DemoHolder demoHolder = getDemoHolder(motionEvent);
        if (demoHolder == null) {
            return null;
        }
        return demoHolder.getItemDetails();
    }

    private DemoHolder getDemoHolder(MotionEvent motionEvent) {
        View viewFindChildViewUnder = this.mRecView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        if (viewFindChildViewUnder != null) {
            RecyclerView.ViewHolder childViewHolder = this.mRecView.getChildViewHolder(viewFindChildViewUnder);
            if (childViewHolder instanceof DemoHolder) {
                return (DemoHolder) childViewHolder;
            }
            return null;
        }
        return null;
    }
}
