package com.android.quicksearchbox.ui;

import android.database.DataSetObserver;
import android.view.View;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

public class DelayingSuggestionsAdapter<A> implements SuggestionsAdapter<A> {
    private final SuggestionsAdapterBase<A> mDelayedAdapter;
    private DataSetObserver mPendingDataSetObserver;
    private Suggestions mPendingSuggestions;

    public DelayingSuggestionsAdapter(SuggestionsAdapterBase<A> delayed) {
        this.mDelayedAdapter = delayed;
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        if (suggestions == null) {
            this.mDelayedAdapter.setSuggestions(null);
            setPendingSuggestions(null);
        } else if (shouldPublish(suggestions)) {
            this.mDelayedAdapter.setSuggestions(suggestions);
            setPendingSuggestions(null);
        } else {
            setPendingSuggestions(suggestions);
        }
    }

    private boolean shouldPublish(Suggestions suggestions) {
        if (suggestions.isDone()) {
            return true;
        }
        SuggestionCursor cursor = suggestions.getResult();
        return cursor != null && cursor.getCount() > 0;
    }

    private void setPendingSuggestions(Suggestions suggestions) {
        if (this.mPendingSuggestions != suggestions) {
            if (this.mDelayedAdapter.isClosed()) {
                if (suggestions != null) {
                    suggestions.release();
                    return;
                }
                return;
            }
            if (this.mPendingDataSetObserver == null) {
                this.mPendingDataSetObserver = new PendingSuggestionsObserver();
            }
            if (this.mPendingSuggestions != null) {
                this.mPendingSuggestions.unregisterDataSetObserver(this.mPendingDataSetObserver);
                if (this.mPendingSuggestions != getSuggestions()) {
                    this.mPendingSuggestions.release();
                }
            }
            this.mPendingSuggestions = suggestions;
            if (this.mPendingSuggestions != null) {
                this.mPendingSuggestions.registerDataSetObserver(this.mPendingDataSetObserver);
            }
        }
    }

    protected void onPendingSuggestionsChanged() {
        if (shouldPublish(this.mPendingSuggestions)) {
            this.mDelayedAdapter.setSuggestions(this.mPendingSuggestions);
            setPendingSuggestions(null);
        }
    }

    private class PendingSuggestionsObserver extends DataSetObserver {
        private PendingSuggestionsObserver() {
        }

        @Override
        public void onChanged() {
            DelayingSuggestionsAdapter.this.onPendingSuggestionsChanged();
        }
    }

    @Override
    public A getListAdapter() {
        return this.mDelayedAdapter.getListAdapter();
    }

    @Override
    public Suggestions getSuggestions() {
        return this.mDelayedAdapter.getSuggestions();
    }

    @Override
    public SuggestionPosition getSuggestion(long suggestionId) {
        return this.mDelayedAdapter.getSuggestion(suggestionId);
    }

    @Override
    public void onSuggestionClicked(long suggestionId) {
        this.mDelayedAdapter.onSuggestionClicked(suggestionId);
    }

    @Override
    public void onSuggestionQueryRefineClicked(long suggestionId) {
        this.mDelayedAdapter.onSuggestionQueryRefineClicked(suggestionId);
    }

    @Override
    public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
        this.mDelayedAdapter.setOnFocusChangeListener(l);
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener listener) {
        this.mDelayedAdapter.setSuggestionClickListener(listener);
    }
}
