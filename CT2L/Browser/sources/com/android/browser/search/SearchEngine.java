package com.android.browser.search;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

public interface SearchEngine {
    void close();

    CharSequence getLabel();

    String getName();

    Cursor getSuggestions(Context context, String str);

    void startSearch(Context context, String str, Bundle bundle, String str2);

    boolean supportsSuggestions();

    boolean wantsEmptyQuery();
}
