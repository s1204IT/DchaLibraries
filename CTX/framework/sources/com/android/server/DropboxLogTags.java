package com.android.server;

import android.util.EventLog;

public class DropboxLogTags {
    public static final int DROPBOX_FILE_COPY = 81002;

    private DropboxLogTags() {
    }

    public static void writeDropboxFileCopy(String str, int i, String str2) {
        EventLog.writeEvent(DROPBOX_FILE_COPY, str, Integer.valueOf(i), str2);
    }
}
