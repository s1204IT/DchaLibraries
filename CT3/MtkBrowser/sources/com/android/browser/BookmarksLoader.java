package com.android.browser;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import com.android.browser.provider.BrowserContract;

public class BookmarksLoader extends CursorLoader {
    public static final String[] PROJECTION = {"_id", "url", "title", "favicon", "thumbnail", "touch_icon", "folder", "position", "parent", "type"};
    String mAccountName;
    String mAccountType;

    public BookmarksLoader(Context context, String accountType, String accountName) {
        super(context, addAccount(BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, accountType, accountName), PROJECTION, null, null, null);
        this.mAccountType = accountType;
        this.mAccountName = accountName;
    }

    @Override
    public void setUri(Uri uri) {
        super.setUri(addAccount(uri, this.mAccountType, this.mAccountName));
    }

    static Uri addAccount(Uri uri, String accountType, String accountName) {
        return uri.buildUpon().appendQueryParameter("acct_type", accountType).appendQueryParameter("acct_name", accountName).build();
    }
}
