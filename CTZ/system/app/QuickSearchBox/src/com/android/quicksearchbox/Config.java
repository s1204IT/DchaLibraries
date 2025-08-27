package com.android.quicksearchbox;

import android.content.Context;
import android.net.Uri;

/* loaded from: classes.dex */
public class Config {
    private final Context mContext;

    public Config(Context context) {
        this.mContext = context;
    }

    public void close() {
    }

    public int getMaxPromotedResults() {
        return this.mContext.getResources().getInteger(R.integer.max_promoted_results);
    }

    public int getMaxResultsPerSource() {
        return 50;
    }

    public int getQueryThreadPriority() {
        return 9;
    }

    public long getTypingUpdateSuggestionsDelayMillis() {
        return 100L;
    }

    public boolean showSuggestionsForZeroQuery() {
        return this.mContext.getResources().getBoolean(R.bool.show_zero_query_suggestions);
    }

    public boolean showScrollingResults() {
        return this.mContext.getResources().getBoolean(R.bool.show_scrolling_results);
    }

    public Uri getHelpUrl(String str) {
        return null;
    }

    public int getHttpConnectTimeout() {
        return 4000;
    }

    public String getUserAgent() {
        return "Android/1.0";
    }
}
