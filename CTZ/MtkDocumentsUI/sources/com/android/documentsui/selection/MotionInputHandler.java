package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.android.documentsui.selection.ItemDetailsLookup;

public abstract class MotionInputHandler extends GestureDetector.SimpleOnGestureListener {
    private final Callbacks mCallbacks;
    protected final ItemDetailsLookup mDetailsLookup;
    protected final SelectionHelper mSelectionHelper;

    public MotionInputHandler(SelectionHelper selectionHelper, ItemDetailsLookup itemDetailsLookup, Callbacks callbacks) {
        Preconditions.checkArgument(selectionHelper != null);
        Preconditions.checkArgument(itemDetailsLookup != null);
        Preconditions.checkArgument(callbacks != null);
        this.mSelectionHelper = selectionHelper;
        this.mDetailsLookup = itemDetailsLookup;
        this.mCallbacks = callbacks;
    }

    protected final boolean selectItem(ItemDetailsLookup.ItemDetails itemDetails) {
        Preconditions.checkArgument(itemDetails != null);
        Preconditions.checkArgument(itemDetails.hasPosition());
        Preconditions.checkArgument(itemDetails.hasStableId());
        if (this.mSelectionHelper.select(itemDetails.getStableId())) {
            this.mSelectionHelper.anchorRange(itemDetails.getPosition());
        }
        if (this.mSelectionHelper.getSelection().size() == 1) {
            this.mCallbacks.focusItem(itemDetails);
        } else {
            this.mCallbacks.clearFocus();
        }
        return true;
    }

    protected final boolean focusItem(ItemDetailsLookup.ItemDetails itemDetails) {
        Preconditions.checkArgument(itemDetails != null);
        Preconditions.checkArgument(itemDetails.hasStableId());
        this.mSelectionHelper.clearSelection();
        this.mCallbacks.focusItem(itemDetails);
        return true;
    }

    protected final void extendSelectionRange(ItemDetailsLookup.ItemDetails itemDetails) {
        Preconditions.checkArgument(itemDetails.hasPosition());
        Preconditions.checkArgument(itemDetails.hasStableId());
        this.mSelectionHelper.extendRange(itemDetails.getPosition());
        this.mCallbacks.focusItem(itemDetails);
    }

    protected final boolean isRangeExtension(MotionEvent motionEvent) {
        return MotionEvents.isShiftKeyPressed(motionEvent) && this.mSelectionHelper.isRangeActive();
    }

    protected boolean shouldClearSelection(MotionEvent motionEvent, ItemDetailsLookup.ItemDetails itemDetails) {
        return (MotionEvents.isCtrlKeyPressed(motionEvent) || itemDetails.inSelectionHotspot(motionEvent) || this.mSelectionHelper.isSelected(itemDetails.getStableId())) ? false : true;
    }

    public static abstract class Callbacks {
        public abstract void onPerformHapticFeedback();

        public void focusItem(ItemDetailsLookup.ItemDetails itemDetails) {
        }

        public void clearFocus() {
        }
    }
}
