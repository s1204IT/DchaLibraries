package com.android.quicksearchbox.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;

public class SuggestionsListAdapter extends SuggestionsAdapterBase<ListAdapter> {
    private Adapter mAdapter;

    public SuggestionsListAdapter(SuggestionViewFactory viewFactory) {
        super(viewFactory);
        this.mAdapter = new Adapter();
    }

    @Override
    public SuggestionPosition getSuggestion(long suggestionId) {
        return new SuggestionPosition(getCurrentSuggestions(), (int) suggestionId);
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
            SuggestionCursor s = SuggestionsListAdapter.this.getCurrentSuggestions();
            if (s == null) {
                return 0;
            }
            return s.getCount();
        }

        @Override
        public Object getItem(int position) {
            return SuggestionsListAdapter.this.getSuggestion(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return SuggestionsListAdapter.this.getView(SuggestionsListAdapter.this.getCurrentSuggestions(), position, position, convertView, parent);
        }

        @Override
        public int getItemViewType(int position) {
            return SuggestionsListAdapter.this.getSuggestionViewType(SuggestionsListAdapter.this.getCurrentSuggestions(), position);
        }

        @Override
        public int getViewTypeCount() {
            return SuggestionsListAdapter.this.getSuggestionViewTypeCount();
        }
    }
}
