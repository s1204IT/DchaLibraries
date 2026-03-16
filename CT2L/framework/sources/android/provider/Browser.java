package android.provider;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BrowserContract;
import android.util.Log;
import android.webkit.WebIconDatabase;
import com.android.internal.R;

public class Browser {
    public static final String EXTRA_APPLICATION_ID = "com.android.browser.application_id";
    public static final String EXTRA_CREATE_NEW_TAB = "create_new_tab";
    public static final String EXTRA_HEADERS = "com.android.browser.headers";
    public static final String EXTRA_SHARE_FAVICON = "share_favicon";
    public static final String EXTRA_SHARE_SCREENSHOT = "share_screenshot";
    public static final int HISTORY_PROJECTION_BOOKMARK_INDEX = 4;
    public static final int HISTORY_PROJECTION_DATE_INDEX = 3;
    public static final int HISTORY_PROJECTION_FAVICON_INDEX = 6;
    public static final int HISTORY_PROJECTION_ID_INDEX = 0;
    public static final int HISTORY_PROJECTION_THUMBNAIL_INDEX = 7;
    public static final int HISTORY_PROJECTION_TITLE_INDEX = 5;
    public static final int HISTORY_PROJECTION_TOUCH_ICON_INDEX = 8;
    public static final int HISTORY_PROJECTION_URL_INDEX = 1;
    public static final int HISTORY_PROJECTION_VISITS_INDEX = 2;
    public static final String INITIAL_ZOOM_LEVEL = "browser.initialZoomLevel";
    private static final String LOGTAG = "browser";
    private static final int MAX_HISTORY_COUNT = 250;
    public static final int SEARCHES_PROJECTION_DATE_INDEX = 2;
    public static final int SEARCHES_PROJECTION_SEARCH_INDEX = 1;
    public static final int TRUNCATE_HISTORY_PROJECTION_ID_INDEX = 0;
    public static final int TRUNCATE_N_OLDEST = 5;
    public static final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");
    public static final String[] HISTORY_PROJECTION = {"_id", "url", "visits", "date", "bookmark", "title", "favicon", "thumbnail", "touch_icon", "user_entered"};
    public static final String[] TRUNCATE_HISTORY_PROJECTION = {"_id", "date"};
    public static final Uri SEARCHES_URI = Uri.parse("content://browser/searches");
    public static final String[] SEARCHES_PROJECTION = {"_id", "search", "date"};

    public static class BookmarkColumns implements BaseColumns {
        public static final String BOOKMARK = "bookmark";
        public static final String CREATED = "created";
        public static final String DATE = "date";
        public static final String FAVICON = "favicon";
        public static final String THUMBNAIL = "thumbnail";
        public static final String TITLE = "title";
        public static final String TOUCH_ICON = "touch_icon";
        public static final String URL = "url";
        public static final String USER_ENTERED = "user_entered";
        public static final String VISITS = "visits";
    }

    public static class SearchColumns implements BaseColumns {
        public static final String DATE = "date";
        public static final String SEARCH = "search";

        @Deprecated
        public static final String URL = "url";
    }

    public static final void saveBookmark(Context c, String title, String url) {
        Intent i = new Intent("android.intent.action.INSERT", BOOKMARKS_URI);
        i.putExtra("title", title);
        i.putExtra("url", url);
        c.startActivity(i);
    }

    public static final void sendString(Context context, String string) {
        sendString(context, string, context.getString(R.string.sendText));
    }

    public static final void sendString(Context c, String stringToSend, String chooserDialogTitle) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, stringToSend);
        try {
            Intent i = Intent.createChooser(send, chooserDialogTitle);
            i.setFlags(268435456);
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
        }
    }

    public static final Cursor getAllBookmarks(ContentResolver cr) throws IllegalStateException {
        return cr.query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"url"}, "folder = 0", null, null);
    }

    public static final Cursor getAllVisitedUrls(ContentResolver cr) throws IllegalStateException {
        return cr.query(BrowserContract.Combined.CONTENT_URI, new String[]{"url"}, null, null, "created ASC");
    }

    private static final void addOrUrlEquals(StringBuilder sb) {
        sb.append(" OR url = ");
    }

    private static final Cursor getVisitedLike(ContentResolver cr, String url) {
        StringBuilder whereClause;
        boolean secure = false;
        String compareString = url;
        if (compareString.startsWith("http://")) {
            compareString = compareString.substring(7);
        } else if (compareString.startsWith("https://")) {
            compareString = compareString.substring(8);
            secure = true;
        }
        if (compareString.startsWith("www.")) {
            compareString = compareString.substring(4);
        }
        if (secure) {
            whereClause = new StringBuilder("url = ");
            DatabaseUtils.appendEscapedSQLString(whereClause, "https://" + compareString);
            addOrUrlEquals(whereClause);
            DatabaseUtils.appendEscapedSQLString(whereClause, "https://www." + compareString);
        } else {
            whereClause = new StringBuilder("url = ");
            DatabaseUtils.appendEscapedSQLString(whereClause, compareString);
            addOrUrlEquals(whereClause);
            String wwwString = "www." + compareString;
            DatabaseUtils.appendEscapedSQLString(whereClause, wwwString);
            addOrUrlEquals(whereClause);
            DatabaseUtils.appendEscapedSQLString(whereClause, "http://" + compareString);
            addOrUrlEquals(whereClause);
            DatabaseUtils.appendEscapedSQLString(whereClause, "http://" + wwwString);
        }
        return cr.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "visits"}, whereClause.toString(), null, null);
    }

    public static final void updateVisitedHistory(ContentResolver cr, String url, boolean real) {
        int visits;
        int user_entered;
        long now = System.currentTimeMillis();
        Cursor c = null;
        try {
            try {
                Cursor c2 = getVisitedLike(cr, url);
                if (c2.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    if (real) {
                        values.put("visits", Integer.valueOf(c2.getInt(1) + 1));
                    } else {
                        values.put("user_entered", (Integer) 1);
                    }
                    values.put("date", Long.valueOf(now));
                    cr.update(ContentUris.withAppendedId(BrowserContract.History.CONTENT_URI, c2.getLong(0)), values, null, null);
                } else {
                    truncateHistory(cr);
                    ContentValues values2 = new ContentValues();
                    if (real) {
                        visits = 1;
                        user_entered = 0;
                    } else {
                        visits = 0;
                        user_entered = 1;
                    }
                    values2.put("url", url);
                    values2.put("visits", Integer.valueOf(visits));
                    values2.put("date", Long.valueOf(now));
                    values2.put("title", url);
                    values2.put("created", (Integer) 0);
                    values2.put("user_entered", Integer.valueOf(user_entered));
                    cr.insert(BrowserContract.History.CONTENT_URI, values2);
                }
                if (c2 != null) {
                    c2.close();
                }
            } catch (IllegalStateException e) {
                Log.e("browser", "updateVisitedHistory", e);
                if (0 != 0) {
                    c.close();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                c.close();
            }
            throw th;
        }
    }

    public static final String[] getVisitedHistory(ContentResolver cr) {
        String[] str;
        Cursor c = null;
        try {
            try {
                String[] projection = {"url"};
                c = cr.query(BrowserContract.History.CONTENT_URI, projection, "visits > 0", null, null);
            } catch (IllegalStateException e) {
                Log.e("browser", "getVisitedHistory", e);
                str = new String[0];
                if (c != null) {
                    c.close();
                }
            }
            if (c == null) {
                String[] strArr = new String[0];
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
        } finally {
            if (c != null) {
                c.close();
            }
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

    public static final boolean canClearHistory(ContentResolver cr) {
        Cursor cursor = null;
        boolean ret = false;
        try {
            try {
                cursor = cr.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "visits"}, null, null, null);
                ret = cursor.getCount() > 0;
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e) {
                Log.e("browser", "canClearHistory", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return ret;
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

    public static final void deleteHistoryTimeFrame(ContentResolver cr, long begin, long end) {
        String whereClause;
        if (-1 != begin) {
            whereClause = -1 == end ? "date >= " + Long.toString(begin) : "date >= " + Long.toString(begin) + " AND date < " + Long.toString(end);
        } else if (-1 != end) {
            whereClause = "date < " + Long.toString(end);
        } else {
            clearHistory(cr);
            return;
        }
        deleteHistoryWhere(cr, whereClause);
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

    public static final void requestAllIcons(ContentResolver cr, String where, WebIconDatabase.IconListener listener) {
    }
}
