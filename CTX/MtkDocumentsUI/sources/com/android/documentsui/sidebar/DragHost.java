package com.android.documentsui.sidebar;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import com.android.documentsui.AbstractDragHost;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.DragAndDropManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Lookup;
import java.util.function.Consumer;

class DragHost extends AbstractDragHost {
    private final ActionHandler mActions;
    private final Activity mActivity;
    private final Lookup<View, Item> mDestinationLookup;

    DragHost(Activity activity, DragAndDropManager dragAndDropManager, Lookup<View, Item> lookup, ActionHandler actionHandler) {
        super(dragAndDropManager);
        this.mActivity = activity;
        this.mDragAndDropManager = dragAndDropManager;
        this.mDestinationLookup = lookup;
        this.mActions = actionHandler;
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        this.mActivity.runOnUiThread(runnable);
    }

    @Override
    public void setDropTargetHighlight(View view, boolean z) {
        ((RootItemView) view).setHighlight(z);
    }

    @Override
    public void onViewHovered(View view) {
        ((RootItemView) view).drawRipple();
        this.mDestinationLookup.lookup(view).open();
    }

    @Override
    public void onDragEntered(final View view) {
        Item itemLookup = this.mDestinationLookup.lookup(view);
        if (!itemLookup.isDropTarget()) {
            this.mDragAndDropManager.updateStateToNotAllowed(view);
            return;
        }
        final RootItem rootItem = (RootItem) itemLookup;
        if (this.mDragAndDropManager.updateState(view, rootItem.root, null) == 0) {
            this.mActions.getRootDocument(rootItem.root, 500, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.updateDropShadow(view, rootItem, (DocumentInfo) obj);
                }
            });
        }
    }

    private void updateDropShadow(View view, RootItem rootItem, DocumentInfo documentInfo) {
        if (documentInfo == null) {
            Log.e("RootsDragHost", "Root DocumentInfo is null. Defaulting to unknown.");
        } else {
            rootItem.docInfo = documentInfo;
            this.mDragAndDropManager.updateState(view, rootItem.root, documentInfo);
        }
    }
}
