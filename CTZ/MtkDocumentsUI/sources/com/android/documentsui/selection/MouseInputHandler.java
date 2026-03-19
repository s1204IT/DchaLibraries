package com.android.documentsui.selection;

import android.util.Log;
import android.view.MotionEvent;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MotionInputHandler;

public final class MouseInputHandler extends MotionInputHandler {
    private final Callbacks mCallbacks;
    private boolean mHandledOnDown;
    private boolean mHandledTapUp;

    public MouseInputHandler(SelectionHelper selectionHelper, ItemDetailsLookup itemDetailsLookup, Callbacks callbacks) {
        super(selectionHelper, itemDetailsLookup, callbacks);
        this.mCallbacks = callbacks;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        if ((MotionEvents.isAltKeyPressed(motionEvent) && MotionEvents.isPrimaryButtonPressed(motionEvent)) || MotionEvents.isSecondaryButtonPressed(motionEvent)) {
            this.mHandledOnDown = true;
            return onRightClick(motionEvent);
        }
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return !MotionEvents.isTouchpadScroll(motionEvent2);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (this.mHandledOnDown) {
            this.mHandledOnDown = false;
            return false;
        }
        if (!this.mDetailsLookup.overStableItem(motionEvent)) {
            this.mSelectionHelper.clearSelection();
            this.mCallbacks.clearFocus();
            return false;
        }
        if (MotionEvents.isTertiaryButtonPressed(motionEvent)) {
            return false;
        }
        ItemDetailsLookup.ItemDetails itemDetails = this.mDetailsLookup.getItemDetails(motionEvent);
        if (!this.mSelectionHelper.hasSelection()) {
            return false;
        }
        if (isRangeExtension(motionEvent)) {
            extendSelectionRange(itemDetails);
        } else {
            if (shouldClearSelection(motionEvent, itemDetails)) {
                this.mSelectionHelper.clearSelection();
            }
            if (this.mSelectionHelper.isSelected(itemDetails.getStableId())) {
                if (this.mSelectionHelper.deselect(itemDetails.getStableId())) {
                    this.mCallbacks.clearFocus();
                }
            } else {
                selectOrFocusItem(itemDetails, motionEvent);
            }
        }
        this.mHandledTapUp = true;
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (this.mHandledTapUp) {
            this.mHandledTapUp = false;
            return false;
        }
        if (this.mSelectionHelper.hasSelection() || !this.mDetailsLookup.overItem(motionEvent) || MotionEvents.isTertiaryButtonPressed(motionEvent)) {
            return false;
        }
        ItemDetailsLookup.ItemDetails itemDetails = this.mDetailsLookup.getItemDetails(motionEvent);
        if (itemDetails == null || !itemDetails.hasStableId()) {
            Log.w("MouseInputDelegate", "Ignoring Confirmed Tap. No document details associated w/ event.");
            return false;
        }
        if (this.mCallbacks.hasFocusedItem() && MotionEvents.isShiftKeyPressed(motionEvent)) {
            this.mSelectionHelper.startRange(this.mCallbacks.getFocusedPosition());
            this.mSelectionHelper.extendRange(itemDetails.getPosition());
            return true;
        }
        selectOrFocusItem(itemDetails, motionEvent);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        ItemDetailsLookup.ItemDetails itemDetails;
        this.mHandledTapUp = false;
        if (!this.mDetailsLookup.overStableItem(motionEvent) || MotionEvents.isTertiaryButtonPressed(motionEvent) || (itemDetails = this.mDetailsLookup.getItemDetails(motionEvent)) == null) {
            return false;
        }
        return this.mCallbacks.onItemActivated(itemDetails, motionEvent);
    }

    private boolean onRightClick(MotionEvent motionEvent) {
        ItemDetailsLookup.ItemDetails itemDetails;
        if (this.mDetailsLookup.overStableItem(motionEvent) && (itemDetails = this.mDetailsLookup.getItemDetails(motionEvent)) != null && !this.mSelectionHelper.isSelected(itemDetails.getStableId())) {
            this.mSelectionHelper.clearSelection();
            selectItem(itemDetails);
        }
        return this.mCallbacks.onContextClick(motionEvent);
    }

    private void selectOrFocusItem(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent) {
        if (itemDetails.inSelectionHotspot(motionEvent) || MotionEvents.isCtrlKeyPressed(motionEvent)) {
            selectItem(itemDetails);
        } else {
            focusItem(itemDetails);
        }
    }

    public static abstract class Callbacks extends MotionInputHandler.Callbacks {
        public abstract boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent);

        public boolean onContextClick(MotionEvent motionEvent) {
            return false;
        }

        public boolean hasFocusedItem() {
            return false;
        }

        public int getFocusedPosition() {
            return -1;
        }
    }
}
