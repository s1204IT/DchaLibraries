package com.android.documentsui.selection.demo;

import android.support.v4.util.Preconditions;
import com.android.documentsui.selection.SelectionHelper;
import java.util.List;

final class DemoStableIdProvider extends SelectionHelper.StableIdProvider {
    private final SelectionDemoAdapter mAdapter;

    public DemoStableIdProvider(SelectionDemoAdapter selectionDemoAdapter) {
        Preconditions.checkArgument(selectionDemoAdapter != null);
        this.mAdapter = selectionDemoAdapter;
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
