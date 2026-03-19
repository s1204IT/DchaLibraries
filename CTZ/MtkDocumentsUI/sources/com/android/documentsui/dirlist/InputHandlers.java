package com.android.documentsui.dirlist;

import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.KeyInputHandler;
import com.android.documentsui.selection.GestureSelectionHelper;
import com.android.documentsui.selection.ItemDetailsLookup;
import com.android.documentsui.selection.MouseInputHandler;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.selection.TouchInputHandler;

final class InputHandlers {
    private ActionHandler mActions;
    private ItemDetailsLookup mDetailsLookup;
    private FocusHandler mFocusHandler;
    private RecyclerView mRecView;
    private SelectionHelper mSelectionHelper;
    private SelectionHelper.SelectionPredicate mSelectionPredicate;
    private State mState;

    InputHandlers(ActionHandler actionHandler, SelectionHelper selectionHelper, SelectionHelper.SelectionPredicate selectionPredicate, ItemDetailsLookup itemDetailsLookup, FocusHandler focusHandler, RecyclerView recyclerView, State state) {
        Preconditions.checkArgument(actionHandler != null);
        Preconditions.checkArgument(selectionHelper != null);
        Preconditions.checkArgument(selectionPredicate != null);
        Preconditions.checkArgument(itemDetailsLookup != null);
        Preconditions.checkArgument(focusHandler != null);
        Preconditions.checkArgument(recyclerView != null);
        Preconditions.checkArgument(state != null);
        this.mActions = actionHandler;
        this.mSelectionHelper = selectionHelper;
        this.mSelectionPredicate = selectionPredicate;
        this.mDetailsLookup = itemDetailsLookup;
        this.mFocusHandler = focusHandler;
        this.mRecView = recyclerView;
        this.mState = state;
    }

    KeyInputHandler createKeyHandler() {
        return new KeyInputHandler(this.mSelectionHelper, this.mSelectionPredicate, new KeyInputHandler.Callbacks() {
            @Override
            public boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, KeyEvent keyEvent) {
                int keyCode = keyEvent.getKeyCode();
                if (keyCode != 23) {
                    if (keyCode == 62) {
                        return InputHandlers.this.mActions.openItem(itemDetails, 2, 0);
                    }
                    if (keyCode != 66 && keyCode != 96) {
                        return false;
                    }
                }
                return InputHandlers.this.mActions.openItem(itemDetails, 1, 2);
            }

            @Override
            public boolean onFocusItem(ItemDetailsLookup.ItemDetails itemDetails, int i, KeyEvent keyEvent) {
                RecyclerView.ViewHolder viewHolderFindViewHolderForAdapterPosition = InputHandlers.this.mRecView.findViewHolderForAdapterPosition(itemDetails.getPosition());
                if (viewHolderFindViewHolderForAdapterPosition instanceof DocumentHolder) {
                    return InputHandlers.this.mFocusHandler.handleKey((DocumentHolder) viewHolderFindViewHolderForAdapterPosition, i, keyEvent);
                }
                return false;
            }

            @Override
            public void onPerformHapticFeedback() {
                InputHandlers.this.mRecView.performHapticFeedback(0);
            }
        });
    }

    MouseInputHandler createMouseHandler(final EventHandler<MotionEvent> eventHandler) {
        Preconditions.checkArgument(eventHandler != null);
        return new MouseInputHandler(this.mSelectionHelper, this.mDetailsLookup, new MouseInputHandler.Callbacks() {
            @Override
            public boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent) {
                return InputHandlers.this.mActions.openItem(itemDetails, 1, 2);
            }

            @Override
            public boolean onContextClick(MotionEvent motionEvent) {
                return eventHandler.accept(motionEvent);
            }

            @Override
            public void onPerformHapticFeedback() {
                InputHandlers.this.mRecView.performHapticFeedback(0);
            }

            @Override
            public void focusItem(ItemDetailsLookup.ItemDetails itemDetails) {
                InputHandlers.this.mFocusHandler.focusDocument(itemDetails.getStableId());
            }

            @Override
            public void clearFocus() {
                InputHandlers.this.mFocusHandler.clearFocus();
            }

            @Override
            public boolean hasFocusedItem() {
                return InputHandlers.this.mFocusHandler.hasFocusedItem();
            }

            @Override
            public int getFocusedPosition() {
                return InputHandlers.this.mFocusHandler.getFocusPosition();
            }
        });
    }

    TouchInputHandler createTouchHandler(GestureSelectionHelper gestureSelectionHelper, final DragStartListener dragStartListener) {
        Preconditions.checkArgument(dragStartListener != null);
        return new TouchInputHandler(this.mSelectionHelper, this.mDetailsLookup, this.mSelectionPredicate, gestureSelectionHelper, new TouchInputHandler.Callbacks() {
            @Override
            public boolean onItemActivated(ItemDetailsLookup.ItemDetails itemDetails, MotionEvent motionEvent) {
                return InputHandlers.this.mActions.openItem(itemDetails, 2, 1);
            }

            @Override
            public boolean onDragInitiated(MotionEvent motionEvent) {
                return dragStartListener.onTouchDragEvent(motionEvent);
            }

            @Override
            public void onPerformHapticFeedback() {
                InputHandlers.this.mRecView.performHapticFeedback(0);
            }

            @Override
            public void focusItem(ItemDetailsLookup.ItemDetails itemDetails) {
                InputHandlers.this.mFocusHandler.focusDocument(itemDetails.getStableId());
            }

            @Override
            public void clearFocus() {
                InputHandlers.this.mFocusHandler.clearFocus();
            }
        });
    }
}
