package com.android.quicksearchbox.ui;

import android.view.View;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

/* loaded from: classes.dex */
public interface SuggestionsAdapter<A> {
    A getListAdapter();

    SuggestionPosition getSuggestion(long j);

    Suggestions getSuggestions();

    void onSuggestionClicked(long j);

    void onSuggestionQueryRefineClicked(long j);

    void setOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener);

    void setSuggestionClickListener(SuggestionClickListener suggestionClickListener);

    void setSuggestions(Suggestions suggestions);
}
