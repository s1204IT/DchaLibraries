package com.android.quicksearchbox.google;

import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;

public interface GoogleSource extends Source {
    SourceResult queryExternal(String str);

    SuggestionCursor refreshShortcut(String str, String str2);
}
