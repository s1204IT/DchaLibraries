package com.android.browser.provider;

import android.content.ContentUris;
import android.net.Uri;

public class BrowserContract {
    public static final Uri AUTHORITY_URI = Uri.parse("content://com.android.browser.provider");

    public static final class Accounts {
        public static final Uri CONTENT_URI = BrowserContract.AUTHORITY_URI.buildUpon().appendPath("accounts").build();
    }

    public static final class Bookmarks {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "bookmarks");
        public static final Uri CONTENT_URI_DEFAULT_FOLDER = Uri.withAppendedPath(CONTENT_URI, "folder");

        public static final Uri buildFolderUri(long j) {
            return ContentUris.withAppendedId(CONTENT_URI_DEFAULT_FOLDER, j);
        }
    }

    public static final class Combined {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "combined");
    }

    public static final class History {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "history");
    }

    public static final class Images {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "images");
    }

    public static final class Searches {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "searches");
    }
}
