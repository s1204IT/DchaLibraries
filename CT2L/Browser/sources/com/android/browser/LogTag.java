package com.android.browser;

import android.util.EventLog;

public class LogTag {
    public static void logBookmarkAdded(String url, String where) {
        EventLog.writeEvent(70103, url + "|" + where);
    }

    public static void logPageFinishedLoading(String url, long duration) {
        EventLog.writeEvent(70104, url + "|" + duration);
    }
}
