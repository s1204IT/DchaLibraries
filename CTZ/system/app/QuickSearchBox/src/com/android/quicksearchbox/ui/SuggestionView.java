package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Suggestion;

/* loaded from: classes.dex */
public interface SuggestionView {
    void bindAdapter(SuggestionsAdapter<?> suggestionsAdapter, long j);

    void bindAsSuggestion(Suggestion suggestion, String str);
}
