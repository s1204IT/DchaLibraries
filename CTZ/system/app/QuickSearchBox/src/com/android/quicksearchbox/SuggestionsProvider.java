package com.android.quicksearchbox;

/* loaded from: classes.dex */
public interface SuggestionsProvider {
    void close();

    Suggestions getSuggestions(String str, Source source);
}
