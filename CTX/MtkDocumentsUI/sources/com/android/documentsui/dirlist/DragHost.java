package com.android.documentsui.dirlist;

import android.app.Activity;
import android.content.ClipData;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.AbstractActionHandler.CommonAddons;
import com.android.documentsui.AbstractDragHost;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.DragAndDropManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.ui.DialogController;
import java.util.Objects;
import java.util.function.Predicate;

class DragHost<T extends Activity & AbstractActionHandler.CommonAddons> extends AbstractDragHost {
    static final boolean $assertionsDisabled = false;
    private final ActionHandler mActions;
    private final T mActivity;
    private final Lookup<View, DocumentInfo> mDestinationLookup;
    private final DialogController mDialogs;
    private final Lookup<View, DocumentHolder> mHolderLookup;
    private final Predicate<View> mIsDocumentView;
    private final SelectionHelper mSelectionMgr;
    private final State mState;

    DragHost(T t, DragAndDropManager dragAndDropManager, SelectionHelper selectionHelper, ActionHandler actionHandler, State state, DialogController dialogController, Predicate<View> predicate, Lookup<View, DocumentHolder> lookup, Lookup<View, DocumentInfo> lookup2) {
        super(dragAndDropManager);
        this.mActivity = t;
        this.mSelectionMgr = selectionHelper;
        this.mActions = actionHandler;
        this.mState = state;
        this.mDialogs = dialogController;
        this.mIsDocumentView = predicate;
        this.mHolderLookup = lookup;
        this.mDestinationLookup = lookup2;
    }

    void dragStopped(boolean z) {
        if (z) {
            this.mSelectionMgr.clearSelection();
        }
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        this.mActivity.runOnUiThread(runnable);
    }

    @Override
    public void setDropTargetHighlight(View view, boolean z) {
    }

    @Override
    public void onViewHovered(View view) {
        if (this.mIsDocumentView.test(view)) {
            this.mActions.springOpenDirectory(this.mDestinationLookup.lookup(view));
        }
        this.mActivity.setRootsDrawerOpen(false);
    }

    @Override
    public void onDragEntered(View view) {
        this.mActivity.setRootsDrawerOpen(false);
        this.mDragAndDropManager.updateState(view, this.mState.stack.getRoot(), this.mDestinationLookup.lookup(view));
    }

    boolean canSpringOpen(View view) {
        DocumentInfo documentInfoLookup = this.mDestinationLookup.lookup(view);
        return documentInfoLookup != null && this.mDragAndDropManager.canSpringOpen(this.mState.stack.getRoot(), documentInfoLookup);
    }

    boolean handleDropEvent(View view, DragEvent dragEvent) {
        DocumentStack documentStack;
        this.mActivity.setRootsDrawerOpen(false);
        dragEvent.getClipData();
        DocumentInfo documentInfoLookup = this.mDestinationLookup.lookup(view);
        if (documentInfoLookup == null) {
            if (SharedMinimal.DEBUG) {
                Log.d("dirlist.DragHost", "Invalid destination. Ignoring.");
            }
            return false;
        }
        if (documentInfoLookup.equals(this.mState.stack.peek())) {
            documentStack = this.mState.stack;
        } else {
            documentStack = new DocumentStack(this.mState.stack, documentInfoLookup);
        }
        DragAndDropManager dragAndDropManager = this.mDragAndDropManager;
        ClipData clipData = dragEvent.getClipData();
        Object localState = dragEvent.getLocalState();
        DialogController dialogController = this.mDialogs;
        Objects.requireNonNull(dialogController);
        return dragAndDropManager.drop(clipData, localState, documentStack, new $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs(dialogController));
    }
}
