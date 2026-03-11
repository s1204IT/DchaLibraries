package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SuggestionsView extends ListView implements SuggestionsListView<ListAdapter> {
    private SuggestionsAdapter<ListAdapter> mSuggestionsAdapter;

    public SuggestionsView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public SuggestionsAdapter<ListAdapter> getSuggestionsAdapter() {
        return this.mSuggestionsAdapter;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setItemsCanFocus(true);
    }

    @Override
    public void setSuggestionsAdapter(SuggestionsAdapter<ListAdapter> suggestionsAdapter) {
        super.setAdapter(suggestionsAdapter == null ? null : suggestionsAdapter.getListAdapter());
        this.mSuggestionsAdapter = suggestionsAdapter;
    }
}
