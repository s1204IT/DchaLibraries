package com.android.documentsui.dirlist;

import android.view.KeyEvent;
import com.android.documentsui.base.Events;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MotionInputHandler;
import com.android.documentsui.selection.SelectionHelper;

public final class KeyInputHandler extends KeyboardEventListener {
    private final Callbacks mCallbacks;
    private final SelectionHelper mSelectionHelper;
    private final SelectionHelper.SelectionPredicate mSelectionPredicate;

    public KeyInputHandler(SelectionHelper selectionHelper, SelectionHelper.SelectionPredicate selectionPredicate, Callbacks callbacks) {
        this.mSelectionHelper = selectionHelper;
        this.mSelectionPredicate = selectionPredicate;
        this.mCallbacks = callbacks;
    }

    @Override
    public boolean onKey(ItemDetailsLookup.ItemDetails itemDetails, int i, KeyEvent keyEvent) {
        int itemViewType;
        if (keyEvent.getAction() != 0 || i == 61) {
            return false;
        }
        if (itemDetails != null && ((itemViewType = itemDetails.getItemViewType()) == 2147483646 || itemViewType == 2147483645 || itemViewType == Integer.MAX_VALUE)) {
            return false;
        }
        if (this.mCallbacks.onFocusItem(itemDetails, i, keyEvent)) {
            if (shouldExtendSelection(itemDetails, keyEvent)) {
                if (!this.mSelectionHelper.isRangeActive()) {
                    this.mSelectionHelper.startRange(itemDetails.getPosition());
                }
                this.mSelectionHelper.extendRange(itemDetails.getPosition());
            } else {
                this.mSelectionHelper.endRange();
                this.mSelectionHelper.clearSelection();
            }
            return true;
        }
        if (this.mSelectionHelper.getSelection().size() > 1) {
            return false;
        }
        return this.mCallbacks.onItemActivated(itemDetails, keyEvent);
    }

    private boolean shouldExtendSelection(ItemDetailsLookup.ItemDetails itemDetails, KeyEvent keyEvent) {
        if (!Events.isNavigationKeyCode(keyEvent.getKeyCode()) || !keyEvent.isShiftPressed()) {
            return false;
        }
        return this.mSelectionPredicate.canSetStateForId(itemDetails.getStableId(), true);
    }

    public static abstract class Callbacks extends MotionInputHandler.Callbacks {
        public abstract boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, KeyEvent keyEvent);

        public boolean onFocusItem(ItemDetailsLookup.ItemDetails itemDetails, int i, KeyEvent keyEvent) {
            return true;
        }
    }
}
