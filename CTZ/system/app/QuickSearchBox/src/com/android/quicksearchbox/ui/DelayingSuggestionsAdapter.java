package com.android.quicksearchbox.ui;

import android.database.DataSetObserver;
import android.view.View;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

/* loaded from: classes.dex */
public class DelayingSuggestionsAdapter<A> implements SuggestionsAdapter<A> {
    private final SuggestionsAdapterBase<A> mDelayedAdapter;
    private DataSetObserver mPendingDataSetObserver;
    private Suggestions mPendingSuggestions;

    public DelayingSuggestionsAdapter(SuggestionsAdapterBase<A> suggestionsAdapterBase) {
        this.mDelayedAdapter = suggestionsAdapterBase;
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
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
        SourceResult result = suggestions.getResult();
        return result != null && result.getCount() > 0;
    }

    private void setPendingSuggestions(Suggestions suggestions) {
        if (this.mPendingSuggestions == suggestions) {
            return;
        }
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

    protected void onPendingSuggestionsChanged() {
        if (shouldPublish(this.mPendingSuggestions)) {
            this.mDelayedAdapter.setSuggestions(this.mPendingSuggestions);
            setPendingSuggestions(null);
        }
    }

    private class PendingSuggestionsObserver extends DataSetObserver {
        private PendingSuggestionsObserver() {
        }

        /* synthetic */ PendingSuggestionsObserver(DelayingSuggestionsAdapter delayingSuggestionsAdapter, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // android.database.DataSetObserver
        public void onChanged() {
            DelayingSuggestionsAdapter.this.onPendingSuggestionsChanged();
        }
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public A getListAdapter() {
        return this.mDelayedAdapter.getListAdapter();
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public Suggestions getSuggestions() {
        return this.mDelayedAdapter.getSuggestions();
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public SuggestionPosition getSuggestion(long j) {
        return this.mDelayedAdapter.getSuggestion(j);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public void onSuggestionClicked(long j) {
        this.mDelayedAdapter.onSuggestionClicked(j);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public void onSuggestionQueryRefineClicked(long j) {
        this.mDelayedAdapter.onSuggestionQueryRefineClicked(j);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public void setOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener) {
        this.mDelayedAdapter.setOnFocusChangeListener(onFocusChangeListener);
    }

    @Override // com.android.quicksearchbox.ui.SuggestionsAdapter
    public void setSuggestionClickListener(SuggestionClickListener suggestionClickListener) {
        this.mDelayedAdapter.setSuggestionClickListener(suggestionClickListener);
    }
}
