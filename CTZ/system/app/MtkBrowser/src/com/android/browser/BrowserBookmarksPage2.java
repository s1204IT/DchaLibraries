package com.android.browser;

import android.database.Cursor;

/* compiled from: BrowserBookmarksPage.java */
/* renamed from: com.android.browser.BookmarksPageCallbacks, reason: use source file name */
/* loaded from: classes.dex */
interface BrowserBookmarksPage2 {
    boolean onBookmarkSelected(Cursor cursor, boolean z);

    boolean onOpenInNewWindow(String... strArr);
}
