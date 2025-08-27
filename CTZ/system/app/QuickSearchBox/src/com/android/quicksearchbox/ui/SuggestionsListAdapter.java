package com.android.quicksearchbox.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;

/* loaded from: classes.dex */
public class SuggestionsListAdapter extends SuggestionsAdapterBase<ListAdapter> {
    private Adapter mAdapter;

    public SuggestionsListAdapter(SuggestionViewFactory suggestionViewFactory) {
        super(suggestionViewFactory);
        this.mAdapter = new Adapter();
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapterBase, com.android.quicksearchbox.ui.SuggestionsAdapter
    public SuggestionPosition getSuggestion(long j) {
        return new SuggestionPosition(getCurrentSuggestions(), (int) j);
    }

    /* JADX DEBUG: Method merged with bridge method: getListAdapter()Ljava/lang/Object; */
    @Override // com.android.quicksearchbox.ui.SuggestionsAdapterBase, com.android.quicksearchbox.ui.SuggestionsAdapter
    public BaseAdapter getListAdapter() {
        return this.mAdapter;
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapterBase
    public void notifyDataSetChanged() {
        this.mAdapter.notifyDataSetChanged();
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapterBase
    public void notifyDataSetInvalidated() {
        this.mAdapter.notifyDataSetInvalidated();
    }

    class Adapter extends BaseAdapter {
        Adapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            SuggestionCursor currentSuggestions = SuggestionsListAdapter.this.getCurrentSuggestions();
            if (currentSuggestions == null) {
                return 0;
            }
            return currentSuggestions.getCount();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return SuggestionsListAdapter.this.getSuggestion(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            return SuggestionsListAdapter.this.getView(SuggestionsListAdapter.this.getCurrentSuggestions(), i, i, view, viewGroup);
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getItemViewType(int i) {
            return SuggestionsListAdapter.this.getSuggestionViewType(SuggestionsListAdapter.this.getCurrentSuggestions(), i);
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getViewTypeCount() {
            return SuggestionsListAdapter.this.getSuggestionViewTypeCount();
        }
    }
}
