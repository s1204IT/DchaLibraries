package com.android.browser;

import android.database.Cursor;

interface BookmarksPageCallbacks {
    boolean onBookmarkSelected(Cursor cursor, boolean z);

    boolean onOpenInNewWindow(String... strArr);
}
