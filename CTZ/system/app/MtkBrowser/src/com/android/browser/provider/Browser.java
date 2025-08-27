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

/* loaded from: classes.dex */
public class Browser {
    public static final Uri BOOKMARKS_URI = Uri.parse("content://MtkBrowserProvider/bookmarks");
    public static final String[] HISTORY_PROJECTION = {"_id", "url", "visits", "date", "bookmark", "title", "favicon", "thumbnail", "touch_icon", "user_entered"};
    public static final String[] TRUNCATE_HISTORY_PROJECTION = {"_id", "date"};
    public static final Uri SEARCHES_URI = Uri.parse("content://MtkBrowserProvider/searches");
    public static final String[] SEARCHES_PROJECTION = {"_id", "search", "date"};

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

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [395=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:26:0x0052 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:17:0x003a A[PHI: r1 r10
  0x003a: PHI (r1v5 java.lang.String[]) = (r1v3 java.lang.String[]), (r1v8 java.lang.String[]) binds: [B:23:0x004e, B:16:0x0038] A[DONT_GENERATE, DONT_INLINE]
  0x003a: PHI (r10v6 android.database.Cursor) = (r10v5 android.database.Cursor), (r10v7 android.database.Cursor) binds: [B:23:0x004e, B:16:0x0038] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0055  */
    /* JADX WARN: Type inference failed for: r10v0, types: [android.content.ContentResolver] */
    /* JADX WARN: Type inference failed for: r10v1 */
    /* JADX WARN: Type inference failed for: r10v4, types: [android.database.Cursor] */
    @Deprecated
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static final String[] getVisitedHistory(ContentResolver contentResolver) throws Throwable {
        IllegalStateException e;
        Cursor cursorQuery;
        String[] strArr;
        try {
            try {
                cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"url"}, "visits > 0", null, null);
            } catch (IllegalStateException e2) {
                e = e2;
                cursorQuery = null;
            } catch (Throwable th) {
                th = th;
                contentResolver = 0;
                if (contentResolver != 0) {
                }
                throw th;
            }
            try {
            } catch (IllegalStateException e3) {
                e = e3;
                Log.e("browser", "getVisitedHistory", e);
                strArr = new String[0];
                if (cursorQuery != null) {
                }
                return strArr;
            }
            if (cursorQuery == null) {
                String[] strArr2 = new String[0];
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return strArr2;
            }
            strArr = new String[cursorQuery.getCount()];
            int i = 0;
            while (cursorQuery.moveToNext()) {
                strArr[i] = cursorQuery.getString(0);
                i++;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return strArr;
        } catch (Throwable th2) {
            th = th2;
            if (contentResolver != 0) {
                contentResolver.close();
            }
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [431=4] */
    public static final void truncateHistory(ContentResolver contentResolver) throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "url", "date"}, null, null, "date ASC");
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
                } catch (IllegalStateException e) {
                    e = e;
                    cursor = cursorQuery;
                    Log.e("browser", "truncateHistory", e);
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IllegalStateException e2) {
            e = e2;
        }
    }

    public static final void clearHistory(ContentResolver contentResolver) throws Throwable {
        deleteHistoryWhere(contentResolver, null);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [492=5] */
    private static final void deleteHistoryWhere(ContentResolver contentResolver, String str) throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = contentResolver.query(BrowserContract.History.CONTENT_URI, new String[]{"url"}, str, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        contentResolver.delete(BrowserContract.History.CONTENT_URI, str, null);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (IllegalStateException e) {
                    e = e;
                    cursor = cursorQuery;
                    Log.e("browser", "deleteHistoryWhere", e);
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (IllegalStateException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static final void deleteFromHistory(ContentResolver contentResolver, String str) {
        contentResolver.delete(BrowserContract.History.CONTENT_URI, "url=?", new String[]{str});
    }

    public static final void addSearchUrl(ContentResolver contentResolver, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("search", str);
        contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
        contentResolver.insert(BrowserContract.Searches.CONTENT_URI, contentValues);
    }

    public static final void clearSearches(ContentResolver contentResolver) {
        try {
            contentResolver.delete(BrowserContract.Searches.CONTENT_URI, null, null);
        } catch (IllegalStateException e) {
            Log.e("browser", "clearSearches", e);
        }
    }
}
