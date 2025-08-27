package com.android.quicksearchbox;

import com.android.quicksearchbox.SuggestionCursor;

/* loaded from: classes.dex */
public interface SuggestionCursorProvider<C extends SuggestionCursor> {
    String getName();

    C getSuggestions(String str, int i);
}
