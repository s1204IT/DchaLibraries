package com.android.quicksearchbox.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;

public class SuggestionsListAdapter extends SuggestionsAdapterBase<ListAdapter> {
    private Adapter mAdapter;

    class Adapter extends BaseAdapter {
        final SuggestionsListAdapter this$0;

        Adapter(SuggestionsListAdapter suggestionsListAdapter) {
            this.this$0 = suggestionsListAdapter;
        }

        @Override
        public int getCount() {
            SuggestionCursor currentSuggestions = this.this$0.getCurrentSuggestions();
            if (currentSuggestions == null) {
                return 0;
            }
            return currentSuggestions.getCount();
        }

        @Override
        public Object getItem(int i) {
            return this.this$0.getSuggestion(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemViewType(int i) {
            return this.this$0.getSuggestionViewType(this.this$0.getCurrentSuggestions(), i);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return this.this$0.getView(this.this$0.getCurrentSuggestions(), i, i, view, viewGroup);
        }

        @Override
        public int getViewTypeCount() {
            return this.this$0.getSuggestionViewTypeCount();
        }
    }

    public SuggestionsListAdapter(SuggestionViewFactory suggestionViewFactory) {
        super(suggestionViewFactory);
        this.mAdapter = new Adapter(this);
    }

    @Override
    public BaseAdapter getListAdapter() {
        return this.mAdapter;
    }

    @Override
    public SuggestionPosition getSuggestion(long j) {
        return new SuggestionPosition(getCurrentSuggestions(), (int) j);
    }

    @Override
    public void notifyDataSetChanged() {
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        this.mAdapter.notifyDataSetInvalidated();
    }
}
