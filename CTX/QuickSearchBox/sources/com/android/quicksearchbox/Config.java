package com.android.quicksearchbox;

import android.content.Context;
import android.net.Uri;

public class Config {
    private final Context mContext;

    public Config(Context context) {
        this.mContext = context;
    }

    public void close() {
    }

    public Uri getHelpUrl(String str) {
        return null;
    }

    public int getHttpConnectTimeout() {
        return 4000;
    }

    public int getMaxPromotedResults() {
        return this.mContext.getResources().getInteger(2131361794);
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

    public String getUserAgent() {
        return "Android/1.0";
    }

    public boolean showScrollingResults() {
        return this.mContext.getResources().getBoolean(2131427331);
    }

    public boolean showSuggestionsForZeroQuery() {
        return this.mContext.getResources().getBoolean(2131427328);
    }
}
