package com.android.quicksearchbox;

public interface SuggestionsProvider {
    void close();

    Suggestions getSuggestions(String str, Source source);
}
