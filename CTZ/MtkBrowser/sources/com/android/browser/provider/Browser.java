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

    public static final void addSearchUrl(ContentResolver contentResolver, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("search", str);
        contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
        contentResolver.insert(BrowserContract.Searches.CONTENT_URI, contentValues);
    }

    public static final void clearHistory(ContentResolver contentResolver) throws Throwable {
        deleteHistoryWhere(contentResolver, null);
    }

    public static final void clearSearches(ContentResolver contentResolver) {
        try {
            contentResolver.delete(BrowserContract.Searches.CONTENT_URI, null, null);
        } catch (IllegalStateException e) {
            Log.e("browser", "clearSearches", e);
        }
    }

    public static final void deleteFromHistory(ContentResolver contentResolver, String str) {
        contentResolver.delete(BrowserContract.History.CONTENT_URI, "url=?", new String[]{str});
    }

    private static final void deleteHistoryWhere(ContentResolver contentResolver, String str) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"url"}, str, null, null);
            try {
                if (cursorQuery.moveToFirst()) {
                    contentResolver.delete(BrowserContract.History.CONTENT_URI, str, null);
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (IllegalStateException e) {
                e = e;
                try {
                    Log.e("browser", "deleteHistoryWhere", e);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    cursorQuery = cursor;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                }
                throw th;
            }
        } catch (IllegalStateException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = cursor;
            if (cursorQuery != null) {
            }
            throw th;
        }
    }

    @Deprecated
    public static final String[] getVisitedHistory(ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        String[] strArr;
        String[] strArr2;
        Cursor cursor = null;
        try {
            cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"url"}, "visits > 0", null, null);
        } catch (IllegalStateException e) {
            e = e;
            cursorQuery = null;
        } catch (Throwable th) {
            th = th;
            if (cursor != null) {
            }
            throw th;
        }
        if (cursorQuery == null) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return new String[0];
        }
        try {
            try {
                strArr2 = new String[cursorQuery.getCount()];
                int i = 0;
                while (cursorQuery.moveToNext()) {
                    strArr2[i] = cursorQuery.getString(0);
                    i++;
                }
            } catch (IllegalStateException e2) {
                e = e2;
                Log.e("browser", "getVisitedHistory", e);
                strArr = new String[0];
                if (cursorQuery == null) {
                    return strArr;
                }
            }
            if (cursorQuery == null) {
                return strArr2;
            }
            strArr = strArr2;
            cursorQuery.close();
            return strArr;
        } catch (Throwable th2) {
            th = th2;
            cursor = cursorQuery;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    public static final void saveBookmark(Context context, String str, String str2) {
        Intent intent = new Intent("android.intent.action.INSERT", BOOKMARKS_URI);
        intent.putExtra("title", str);
        intent.putExtra("url", str2);
        context.startActivity(intent);
    }

    public static final void sendString(Context context, String str, String str2) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", str);
        try {
            Intent intentCreateChooser = Intent.createChooser(intent, str2);
            intentCreateChooser.setFlags(268435456);
            context.startActivity(intentCreateChooser);
        } catch (ActivityNotFoundException e) {
        }
    }

    public static final void truncateHistory(ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "url", "date"}, null, null, "date ASC");
        } catch (IllegalStateException e) {
            e = e;
            cursorQuery = null;
        } catch (Throwable th) {
            th = th;
            cursorQuery = cursor;
            if (cursorQuery != null) {
            }
            throw th;
        }
        try {
            if (cursorQuery.moveToFirst() && cursorQuery.getCount() >= 250) {
                for (int i = 0; i < 5; i++) {
                    contentResolver.delete(ContentUris.withAppendedId(BrowserContract.History.CONTENT_URI, cursorQuery.getLong(0)), null, null);
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (IllegalStateException e2) {
            e = e2;
            try {
                Log.e("browser", "truncateHistory", e);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
                cursor = cursorQuery;
                cursorQuery = cursor;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            if (cursorQuery != null) {
            }
            throw th;
        }
    }
}
