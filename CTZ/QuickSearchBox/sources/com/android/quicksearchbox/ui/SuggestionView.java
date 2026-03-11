package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Suggestion;

public interface SuggestionView {
    void bindAdapter(SuggestionsAdapter<?> suggestionsAdapter, long j);

    void bindAsSuggestion(Suggestion suggestion, String str);
}
