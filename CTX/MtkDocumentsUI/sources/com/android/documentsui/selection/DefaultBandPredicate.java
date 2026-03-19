package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.view.MotionEvent;
import com.android.documentsui.selection.ItemDetailsLookup;

public final class DefaultBandPredicate extends BandPredicate {
    private final ItemDetailsLookup mDetailsLookup;

    public DefaultBandPredicate(ItemDetailsLookup itemDetailsLookup) {
        Preconditions.checkArgument(itemDetailsLookup != null);
        this.mDetailsLookup = itemDetailsLookup;
    }

    @Override
    public boolean canInitiate(MotionEvent motionEvent) {
        ItemDetailsLookup.ItemDetails itemDetails = this.mDetailsLookup.getItemDetails(motionEvent);
        return itemDetails == null || !itemDetails.inDragRegion(motionEvent);
    }
}
