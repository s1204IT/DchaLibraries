package com.android.common;

import android.app.SearchableInfo;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.android.common.speech.LoggingEvents;

public class Search {
    public static final String SOURCE = "source";
    public static final String SUGGEST_COLUMN_LAST_ACCESS_HINT = "suggest_last_access_hint";

    private Search() {
    }

    public static Cursor getSuggestions(Context context, SearchableInfo searchableInfo, String str) {
        return getSuggestions(context, searchableInfo, str, -1);
    }

    public static Cursor getSuggestions(Context context, SearchableInfo searchableInfo, String str, int i) {
        String suggestAuthority;
        String[] strArr = null;
        if (searchableInfo == null || (suggestAuthority = searchableInfo.getSuggestAuthority()) == null) {
            return null;
        }
        Uri.Builder builderFragment = new Uri.Builder().scheme("content").authority(suggestAuthority).query(LoggingEvents.EXTRA_CALLING_APP_NAME).fragment(LoggingEvents.EXTRA_CALLING_APP_NAME);
        String suggestPath = searchableInfo.getSuggestPath();
        if (suggestPath != null) {
            builderFragment.appendEncodedPath(suggestPath);
        }
        builderFragment.appendPath("search_suggest_query");
        String suggestSelection = searchableInfo.getSuggestSelection();
        if (suggestSelection != null) {
            strArr = new String[]{str};
        } else {
            builderFragment.appendPath(str);
        }
        String[] strArr2 = strArr;
        if (i > 0) {
            builderFragment.appendQueryParameter("limit", String.valueOf(i));
        }
        return context.getContentResolver().query(builderFragment.build(), null, suggestSelection, strArr2, null);
    }
}
