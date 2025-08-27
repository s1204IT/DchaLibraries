package com.android.browser;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import com.android.browser.provider.BrowserContract;

/* loaded from: classes.dex */
public class BookmarksLoader extends CursorLoader {
    public static final String[] PROJECTION = {"_id", "url", "title", "favicon", "thumbnail", "touch_icon", "folder", "position", "parent", "type"};
    String mAccountName;
    String mAccountType;

    public BookmarksLoader(Context context, String str, String str2) {
        super(context, addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, str, str2), PROJECTION, null, null, null);
        this.mAccountType = str;
        this.mAccountName = str2;
    }

    @Override // android.content.CursorLoader
    public void setUri(Uri uri) {
        super.setUri(addAccount(uri, this.mAccountType, this.mAccountName));
    }

    static Uri addAccount(Uri uri, String str, String str2) {
        return uri.buildUpon().appendQueryParameter("acct_type", str).appendQueryParameter("acct_name", str2).build();
    }
}
