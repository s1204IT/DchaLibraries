package com.mediatek.browser.ext;

import android.content.Context;
import android.content.SharedPreferences;

public interface IBrowserRegionalPhoneExt {
    String getSearchEngine(SharedPreferences sharedPreferences, Context context);

    void updateBookmarks(Context context);
}
