package com.android.browser.provider;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.android.browser.provider.BrowserContract;

public class Browser {
    public static final Uri BOOKMARKS_URI = Uri.parse("content://MtkBrowserProvider/bookmarks");
    public static final String[] HISTORY_PROJECTION = {"_id", "url", "visits", "date", "bookmark", "title", "favicon", "thumbnail", "touch_icon", "user_entered"};
    public static final String[] TRUNCATE_HISTORY_PROJECTION = {"_id", "date"};
    public static final Uri SEARCHES_URI = Uri.parse("content://MtkBrowserProvider/searches");
    public static final String[] SEARCHES_PROJECTION = {"_id", "search", "date"};

    public static final void saveBookmark(Context c, String title, String url) {
        Intent i = new Intent("android.intent.action.INSERT", BOOKMARKS_URI);
        i.putExtra("title", title);
        i.putExtra("url", url);
        c.startActivity(i);
    }

    public static final void sendString(Context c, String stringToSend, String chooserDialogTitle) {
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        send.putExtra("android.intent.extra.TEXT", stringToSend);
        try {
            Intent i = Intent.createChooser(send, chooserDialogTitle);
            i.setFlags(268435456);
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
        }
    }

    @Deprecated
    public static final String[] getVisitedHistory(ContentResolver cr) {
        String[] str;
        Cursor c;
        Cursor cursor = null;
        try {
            try {
                String[] projection = {"url"};
                c = cr.query(BrowserContract.History.CONTENT_URI, projection, "visits > 0", null, null);
            } catch (IllegalStateException e) {
                Log.e("browser", "getVisitedHistory", e);
                str = new String[0];
                if (0 != 0) {
                    cursor.close();
                }
            }
            if (c == null) {
                String[] strArr = new String[0];
                if (c != null) {
                    c.close();
                }
                return strArr;
            }
            str = new String[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                str[i] = c.getString(0);
                i++;
            }
            if (c != null) {
                c.close();
            }
            return str;
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    public static final void truncateHistory(ContentResolver cr) {
        Cursor cursor = null;
        try {
            try {
                cursor = cr.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "url", "date"}, null, null, "date ASC");
                if (cursor.moveToFirst() && cursor.getCount() >= 250) {
                    for (int i = 0; i < 5; i++) {
                        cr.delete(ContentUris.withAppendedId(BrowserContract.History.CONTENT_URI, cursor.getLong(0)), null, null);
                        if (!cursor.moveToNext()) {
                            break;
                        }
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e) {
                Log.e("browser", "truncateHistory", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    public static final void clearHistory(ContentResolver cr) {
        deleteHistoryWhere(cr, null);
    }

    private static final void deleteHistoryWhere(ContentResolver cr, String whereClause) {
        Cursor cursor = null;
        try {
            try {
                cursor = cr.query(BrowserContract.History.CONTENT_URI, new String[]{"url"}, whereClause, null, null);
                if (cursor.moveToFirst()) {
                    cr.delete(BrowserContract.History.CONTENT_URI, whereClause, null);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e) {
                Log.e("browser", "deleteHistoryWhere", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    public static final void deleteFromHistory(ContentResolver cr, String url) {
        cr.delete(BrowserContract.History.CONTENT_URI, "url=?", new String[]{url});
    }

    public static final void addSearchUrl(ContentResolver cr, String search) {
        ContentValues values = new ContentValues();
        values.put("search", search);
        values.put("date", Long.valueOf(System.currentTimeMillis()));
        cr.insert(BrowserContract.Searches.CONTENT_URI, values);
    }

    public static final void clearSearches(ContentResolver cr) {
        try {
            cr.delete(BrowserContract.Searches.CONTENT_URI, null, null);
        } catch (IllegalStateException e) {
            Log.e("browser", "clearSearches", e);
        }
    }
}
