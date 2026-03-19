package com.android.documentsui.dirlist;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import com.android.documentsui.selection.ItemDetailsLookup;

final class DocsItemDetailsLookup extends ItemDetailsLookup {
    private final RecyclerView mRecView;

    public DocsItemDetailsLookup(RecyclerView recyclerView) {
        this.mRecView = recyclerView;
    }

    @Override
    public boolean overItem(MotionEvent motionEvent) {
        return getItemPosition(motionEvent) != -1;
    }

    @Override
    public boolean overStableItem(MotionEvent motionEvent) {
        ItemDetailsLookup.ItemDetails itemDetails;
        return overItem(motionEvent) && (itemDetails = getItemDetails(motionEvent)) != null && itemDetails.hasStableId();
    }

    @Override
    public boolean inItemDragRegion(MotionEvent motionEvent) {
        ItemDetailsLookup.ItemDetails itemDetails;
        return overItem(motionEvent) && (itemDetails = getItemDetails(motionEvent)) != null && itemDetails.inDragRegion(motionEvent);
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
        DocumentHolder documentHolder = getDocumentHolder(motionEvent);
        if (documentHolder == null) {
            return null;
        }
        return documentHolder.getItemDetails();
    }

    private DocumentHolder getDocumentHolder(MotionEvent motionEvent) {
        View viewFindChildViewUnder = this.mRecView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        if (viewFindChildViewUnder != null) {
            RecyclerView.ViewHolder childViewHolder = this.mRecView.getChildViewHolder(viewFindChildViewUnder);
            if (childViewHolder instanceof DocumentHolder) {
                return (DocumentHolder) childViewHolder;
            }
            return null;
        }
        return null;
    }
}
