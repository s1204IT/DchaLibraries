package com.android.documentsui.dirlist;

import android.database.Cursor;
import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import com.android.documentsui.ActivityConfig;
import com.android.documentsui.Model;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.selection.SelectionHelper;

final class DocsSelectionPredicate extends SelectionHelper.SelectionPredicate {
    static final boolean $assertionsDisabled = false;
    private ActivityConfig mConfig;
    private Model mModel;
    private RecyclerView mRecView;
    private State mState;

    DocsSelectionPredicate(ActivityConfig activityConfig, State state, Model model, RecyclerView recyclerView) {
        Preconditions.checkArgument(activityConfig != null);
        Preconditions.checkArgument(state != null);
        Preconditions.checkArgument(model != null);
        Preconditions.checkArgument(recyclerView != null);
        this.mConfig = activityConfig;
        this.mState = state;
        this.mModel = model;
        this.mRecView = recyclerView;
    }

    @Override
    public boolean canSetStateForId(String str, boolean z) {
        if (z) {
            Cursor item = this.mModel.getItem(str);
            if (item == null) {
                Log.w("DirectoryFragment", "Couldn't obtain cursor for id: " + str);
                return false;
            }
            return this.mConfig.canSelectType(DocumentInfo.getCursorString(item, "mime_type"), DocumentInfo.getCursorInt(item, "flags"), this.mState);
        }
        return true;
    }

    @Override
    public boolean canSetStateAtPosition(int i, boolean z) {
        return ModelBackedDocumentsAdapter.isContentType(this.mRecView.findViewHolderForAdapterPosition(i).getItemViewType());
    }

    @Override
    public boolean canSelectMultiple() {
        return this.mState.allowMultiple;
    }
}
