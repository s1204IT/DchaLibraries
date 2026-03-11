package com.android.quicksearchbox.ui;

public interface SuggestionClickListener {
    void onSuggestionClicked(SuggestionsAdapter<?> suggestionsAdapter, long j);

    void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> suggestionsAdapter, long j);
}
