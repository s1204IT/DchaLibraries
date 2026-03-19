package com.android.documentsui.dirlist;

import android.support.v4.util.Preconditions;
import com.android.documentsui.selection.SelectionHelper;
import java.util.List;

public final class DocsStableIdProvider extends SelectionHelper.StableIdProvider {
    private final DocumentsAdapter mAdapter;

    public DocsStableIdProvider(DocumentsAdapter documentsAdapter) {
        Preconditions.checkArgument(documentsAdapter != null);
        this.mAdapter = documentsAdapter;
    }

    @Override
    public String getStableId(int i) {
        return this.mAdapter.getStableId(i);
    }

    @Override
    public int getPosition(String str) {
        return this.mAdapter.getPosition(str);
    }

    @Override
    public List<String> getStableIds() {
        return this.mAdapter.getStableIds();
    }
}
