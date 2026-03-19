package com.android.quicksearchbox.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;

public class SuggestionsListAdapter extends SuggestionsAdapterBase<ListAdapter> {
    private Adapter mAdapter;

    public SuggestionsListAdapter(SuggestionViewFactory suggestionViewFactory) {
        super(suggestionViewFactory);
        this.mAdapter = new Adapter();
    }

    @Override
    public SuggestionPosition getSuggestion(long j) {
        return new SuggestionPosition(getCurrentSuggestions(), (int) j);
    }

    @Override
    public BaseAdapter getListAdapter() {
        return this.mAdapter;
    }

    @Override
    public void notifyDataSetChanged() {
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        this.mAdapter.notifyDataSetInvalidated();
    }

    class Adapter extends BaseAdapter {
        Adapter() {
        }

        @Override
        public int getCount() {
            SuggestionCursor currentSuggestions = SuggestionsListAdapter.this.getCurrentSuggestions();
            if (currentSuggestions == null) {
                return 0;
            }
            return currentSuggestions.getCount();
        }

        @Override
        public Object getItem(int i) {
            return SuggestionsListAdapter.this.getSuggestion(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return SuggestionsListAdapter.this.getView(SuggestionsListAdapter.this.getCurrentSuggestions(), i, i, view, viewGroup);
        }

        @Override
        public int getItemViewType(int i) {
            return SuggestionsListAdapter.this.getSuggestionViewType(SuggestionsListAdapter.this.getCurrentSuggestions(), i);
        }

        @Override
        public int getViewTypeCount() {
            return SuggestionsListAdapter.this.getSuggestionViewTypeCount();
        }
    }
}
