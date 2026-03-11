package com.android.quicksearchbox.ui;

import android.view.View;
import android.widget.AbsListView;

public interface SuggestionsListView<A> {
    long getSelectedItemId();

    SuggestionsAdapter<A> getSuggestionsAdapter();

    void setOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener);

    void setOnKeyListener(View.OnKeyListener onKeyListener);

    void setOnScrollListener(AbsListView.OnScrollListener onScrollListener);

    void setSuggestionsAdapter(SuggestionsAdapter<A> suggestionsAdapter);
}
