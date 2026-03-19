package com.android.documentsui.selection;

import android.util.Log;
import android.view.MotionEvent;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MotionInputHandler;
import com.android.documentsui.selection.SelectionHelper;

public final class TouchInputHandler extends MotionInputHandler {
    private final Callbacks mCallbacks;
    private final Runnable mGestureKicker;
    private final SelectionHelper.SelectionPredicate mSelectionPredicate;

    public TouchInputHandler(SelectionHelper selectionHelper, ItemDetailsLookup itemDetailsLookup, SelectionHelper.SelectionPredicate selectionPredicate, Runnable runnable, Callbacks callbacks) {
        super(selectionHelper, itemDetailsLookup, callbacks);
        this.mSelectionPredicate = selectionPredicate;
        this.mGestureKicker = runnable;
        this.mCallbacks = callbacks;
    }

    public TouchInputHandler(SelectionHelper selectionHelper, ItemDetailsLookup itemDetailsLookup, SelectionHelper.SelectionPredicate selectionPredicate, final GestureSelectionHelper gestureSelectionHelper, Callbacks callbacks) {
        this(selectionHelper, itemDetailsLookup, selectionPredicate, new Runnable() {
            @Override
            public void run() {
                gestureSelectionHelper.start();
            }
        }, callbacks);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (!this.mDetailsLookup.overStableItem(motionEvent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("TouchInputDelegate", "Tap not associated w/ model item. Clearing selection.");
            }
            this.mSelectionHelper.clearSelection();
            return false;
        }
        ItemDetailsLookup.ItemDetails itemDetails = this.mDetailsLookup.getItemDetails(motionEvent);
        if (this.mSelectionHelper.hasSelection()) {
            if (isRangeExtension(motionEvent)) {
                extendSelectionRange(itemDetails);
                return true;
            }
            if (this.mSelectionHelper.isSelected(itemDetails.getStableId())) {
                this.mSelectionHelper.deselect(itemDetails.getStableId());
                return true;
            }
            selectItem(itemDetails);
            return true;
        }
        if (itemDetails.inSelectionHotspot(motionEvent)) {
            return selectItem(itemDetails);
        }
        return this.mCallbacks.onItemActivated(itemDetails, motionEvent);
    }

    @Override
    public final void onLongPress(MotionEvent motionEvent) {
        if (!this.mDetailsLookup.overStableItem(motionEvent)) {
            if (SharedMinimal.DEBUG) {
                Log.d("TouchInputDelegate", "Ignoring LongPress on non-model-backed item.");
                return;
            }
            return;
        }
        ItemDetailsLookup.ItemDetails itemDetails = this.mDetailsLookup.getItemDetails(motionEvent);
        boolean z = true;
        if (isRangeExtension(motionEvent)) {
            extendSelectionRange(itemDetails);
        } else if (!this.mSelectionHelper.isSelected(itemDetails.getStableId()) && this.mSelectionPredicate.canSetStateForId(itemDetails.getStableId(), true)) {
            if (selectItem(itemDetails)) {
                if (this.mSelectionPredicate.canSelectMultiple()) {
                    this.mGestureKicker.run();
                }
            } else {
                z = false;
            }
        } else {
            this.mCallbacks.onDragInitiated(motionEvent);
        }
        if (z) {
            this.mCallbacks.onPerformHapticFeedback();
        }
    }

    public static abstract class Callbacks extends MotionInputHandler.Callbacks {
        public abstract boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent);

        public boolean onDragInitiated(MotionEvent motionEvent) {
            return false;
        }
    }
}
