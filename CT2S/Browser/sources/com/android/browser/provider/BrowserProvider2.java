package com.android.browser.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.Browser;
import android.provider.BrowserContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.R;
import com.android.browser.UrlUtils;
import com.android.browser.provider.BrowserProvider;
import com.android.browser.widget.BookmarkThumbnailWidgetProvider;
import com.android.common.content.SyncStateContentProviderHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BrowserProvider2 extends SQLiteContentProvider {
    DatabaseHelper mOpenHelper;
    static final Uri LEGACY_AUTHORITY_URI = new Uri.Builder().authority("browser").scheme("content").build();
    private static final String[] SUGGEST_PROJECTION = {qualifyColumn("history", "_id"), qualifyColumn("history", "url"), bookmarkOrHistoryColumn("title"), bookmarkOrHistoryLiteral("url", Integer.toString(R.drawable.ic_bookmark_off_holo_dark), Integer.toString(R.drawable.ic_history_holo_dark)), qualifyColumn("history", "date")};
    static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    static final HashMap<String, String> ACCOUNTS_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> BOOKMARKS_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> OTHER_BOOKMARKS_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> HISTORY_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> SYNC_STATE_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> IMAGES_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> COMBINED_HISTORY_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> COMBINED_BOOKMARK_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> SEARCHES_PROJECTION_MAP = new HashMap<>();
    static final HashMap<String, String> SETTINGS_PROJECTION_MAP = new HashMap<>();
    SyncStateContentProviderHelper mSyncHelper = new SyncStateContentProviderHelper();
    ContentObserver mWidgetObserver = null;
    boolean mUpdateWidgets = false;
    boolean mSyncToNetwork = true;

    public interface OmniboxSuggestions {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "omnibox_suggestions");
    }

    public interface Thumbnails {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "thumbnails");
    }

    static {
        UriMatcher matcher = URI_MATCHER;
        matcher.addURI("com.android.browser", "accounts", 7000);
        matcher.addURI("com.android.browser", "bookmarks", 1000);
        matcher.addURI("com.android.browser", "bookmarks/#", 1001);
        matcher.addURI("com.android.browser", "bookmarks/folder", 1002);
        matcher.addURI("com.android.browser", "bookmarks/folder/#", 1003);
        matcher.addURI("com.android.browser", "bookmarks/folder/id", 1005);
        matcher.addURI("com.android.browser", "search_suggest_query", 1004);
        matcher.addURI("com.android.browser", "bookmarks/search_suggest_query", 1004);
        matcher.addURI("com.android.browser", "history", 2000);
        matcher.addURI("com.android.browser", "history/#", 2001);
        matcher.addURI("com.android.browser", "searches", 3000);
        matcher.addURI("com.android.browser", "searches/#", 3001);
        matcher.addURI("com.android.browser", "syncstate", 4000);
        matcher.addURI("com.android.browser", "syncstate/#", 4001);
        matcher.addURI("com.android.browser", "images", 5000);
        matcher.addURI("com.android.browser", "combined", 6000);
        matcher.addURI("com.android.browser", "combined/#", 6001);
        matcher.addURI("com.android.browser", "settings", 8000);
        matcher.addURI("com.android.browser", "thumbnails", 10);
        matcher.addURI("com.android.browser", "thumbnails/#", 11);
        matcher.addURI("com.android.browser", "omnibox_suggestions", 20);
        matcher.addURI("browser", "searches", 3000);
        matcher.addURI("browser", "searches/#", 3001);
        matcher.addURI("browser", "bookmarks", 9000);
        matcher.addURI("browser", "bookmarks/#", 9001);
        matcher.addURI("browser", "search_suggest_query", 1004);
        matcher.addURI("browser", "bookmarks/search_suggest_query", 1004);
        HashMap<String, String> map = ACCOUNTS_PROJECTION_MAP;
        map.put("account_type", "account_type");
        map.put("account_name", "account_name");
        map.put("root_id", "root_id");
        HashMap<String, String> map2 = BOOKMARKS_PROJECTION_MAP;
        map2.put("_id", qualifyColumn("bookmarks", "_id"));
        map2.put("title", "title");
        map2.put("url", "url");
        map2.put("favicon", "favicon");
        map2.put("thumbnail", "thumbnail");
        map2.put("touch_icon", "touch_icon");
        map2.put("folder", "folder");
        map2.put("parent", "parent");
        map2.put("position", "position");
        map2.put("insert_after", "insert_after");
        map2.put("deleted", "deleted");
        map2.put("account_name", "account_name");
        map2.put("account_type", "account_type");
        map2.put("sourceid", "sourceid");
        map2.put("version", "version");
        map2.put("created", "created");
        map2.put("modified", "modified");
        map2.put("dirty", "dirty");
        map2.put("sync1", "sync1");
        map2.put("sync2", "sync2");
        map2.put("sync3", "sync3");
        map2.put("sync4", "sync4");
        map2.put("sync5", "sync5");
        map2.put("parent_source", "(SELECT sourceid FROM bookmarks A WHERE A._id=bookmarks.parent) AS parent_source");
        map2.put("insert_after_source", "(SELECT sourceid FROM bookmarks A WHERE A._id=bookmarks.insert_after) AS insert_after_source");
        map2.put("type", "CASE  WHEN folder=0 THEN 1 WHEN sync3='bookmark_bar' THEN 3 WHEN sync3='other_bookmarks' THEN 4 ELSE 2 END AS type");
        OTHER_BOOKMARKS_PROJECTION_MAP.putAll(BOOKMARKS_PROJECTION_MAP);
        OTHER_BOOKMARKS_PROJECTION_MAP.put("position", Long.toString(Long.MAX_VALUE) + " AS position");
        HashMap<String, String> map3 = HISTORY_PROJECTION_MAP;
        map3.put("_id", qualifyColumn("history", "_id"));
        map3.put("title", "title");
        map3.put("url", "url");
        map3.put("favicon", "favicon");
        map3.put("thumbnail", "thumbnail");
        map3.put("touch_icon", "touch_icon");
        map3.put("created", "created");
        map3.put("date", "date");
        map3.put("visits", "visits");
        map3.put("user_entered", "user_entered");
        HashMap<String, String> map4 = SYNC_STATE_PROJECTION_MAP;
        map4.put("_id", "_id");
        map4.put("account_name", "account_name");
        map4.put("account_type", "account_type");
        map4.put("data", "data");
        HashMap<String, String> map5 = IMAGES_PROJECTION_MAP;
        map5.put("url_key", "url_key");
        map5.put("favicon", "favicon");
        map5.put("thumbnail", "thumbnail");
        map5.put("touch_icon", "touch_icon");
        HashMap<String, String> map6 = COMBINED_HISTORY_PROJECTION_MAP;
        map6.put("_id", bookmarkOrHistoryColumn("_id"));
        map6.put("title", bookmarkOrHistoryColumn("title"));
        map6.put("url", qualifyColumn("history", "url"));
        map6.put("created", qualifyColumn("history", "created"));
        map6.put("date", "date");
        map6.put("bookmark", "CASE WHEN bookmarks._id IS NOT NULL THEN 1 ELSE 0 END AS bookmark");
        map6.put("visits", "visits");
        map6.put("favicon", "favicon");
        map6.put("thumbnail", "thumbnail");
        map6.put("touch_icon", "touch_icon");
        map6.put("user_entered", "NULL AS user_entered");
        HashMap<String, String> map7 = COMBINED_BOOKMARK_PROJECTION_MAP;
        map7.put("_id", "_id");
        map7.put("title", "title");
        map7.put("url", "url");
        map7.put("created", "created");
        map7.put("date", "NULL AS date");
        map7.put("bookmark", "1 AS bookmark");
        map7.put("visits", "0 AS visits");
        map7.put("favicon", "favicon");
        map7.put("thumbnail", "thumbnail");
        map7.put("touch_icon", "touch_icon");
        map7.put("user_entered", "NULL AS user_entered");
        HashMap<String, String> map8 = SEARCHES_PROJECTION_MAP;
        map8.put("_id", "_id");
        map8.put("search", "search");
        map8.put("date", "date");
        HashMap<String, String> map9 = SETTINGS_PROJECTION_MAP;
        map9.put("key", "key");
        map9.put("value", "value");
    }

    static final String bookmarkOrHistoryColumn(String column) {
        return "CASE WHEN bookmarks." + column + " IS NOT NULL THEN bookmarks." + column + " ELSE history." + column + " END AS " + column;
    }

    static final String bookmarkOrHistoryLiteral(String column, String bookmarkValue, String historyValue) {
        return "CASE WHEN bookmarks." + column + " IS NOT NULL THEN \"" + bookmarkValue + "\" ELSE \"" + historyValue + "\" END";
    }

    static final String qualifyColumn(String table, String column) {
        return table + "." + column + " AS " + column;
    }

    final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "browser2.db", (SQLiteDatabase.CursorFactory) null, 32);
            setWriteAheadLoggingEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE bookmarks(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT,folder INTEGER NOT NULL DEFAULT 0,parent INTEGER,position INTEGER NOT NULL,insert_after INTEGER,deleted INTEGER NOT NULL DEFAULT 0,account_name TEXT,account_type TEXT,sourceid TEXT,version INTEGER NOT NULL DEFAULT 1,created INTEGER,modified INTEGER,dirty INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT);");
            db.execSQL("CREATE TABLE history(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,created INTEGER,date INTEGER,visits INTEGER NOT NULL DEFAULT 0,user_entered INTEGER);");
            db.execSQL("CREATE TABLE images (url_key TEXT UNIQUE NOT NULL,favicon BLOB,thumbnail BLOB,touch_icon BLOB);");
            db.execSQL("CREATE INDEX imagesUrlIndex ON images(url_key)");
            db.execSQL("CREATE TABLE searches (_id INTEGER PRIMARY KEY AUTOINCREMENT,search TEXT,date LONG);");
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY,value TEXT NOT NULL);");
            createAccountsView(db);
            createThumbnails(db);
            BrowserProvider2.this.mSyncHelper.createDatabase(db);
            if (!importFromBrowserProvider(db)) {
                createDefaultBookmarks(db);
            }
            enableSync(db);
            createOmniboxSuggestions(db);
        }

        void createOmniboxSuggestions(SQLiteDatabase db) {
            db.execSQL("CREATE VIEW IF NOT EXISTS v_omnibox_suggestions  AS   SELECT _id, url, title, 1 AS bookmark, 0 AS visits, 0 AS date  FROM bookmarks   WHERE deleted = 0 AND folder = 0   UNION ALL   SELECT _id, url, title, 0 AS bookmark, visits, date   FROM history   WHERE url NOT IN (SELECT url FROM bookmarks    WHERE deleted = 0 AND folder = 0)   ORDER BY bookmark DESC, visits DESC, date DESC ");
        }

        void createThumbnails(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS thumbnails (_id INTEGER PRIMARY KEY,thumbnail BLOB NOT NULL);");
        }

        void enableSync(SQLiteDatabase db) {
            Account[] accounts;
            ContentValues values = new ContentValues();
            values.put("key", "sync_enabled");
            values.put("value", (Integer) 1);
            BrowserProvider2.this.insertSettingsInTransaction(db, values);
            AccountManager am = (AccountManager) BrowserProvider2.this.getContext().getSystemService("account");
            if (am != null && (accounts = am.getAccountsByType("com.google")) != null && accounts.length != 0) {
                for (Account account : accounts) {
                    if (ContentResolver.getIsSyncable(account, "com.android.browser") == 0) {
                        ContentResolver.setIsSyncable(account, "com.android.browser", 1);
                        ContentResolver.setSyncAutomatically(account, "com.android.browser", true);
                    }
                }
            }
        }

        boolean importFromBrowserProvider(SQLiteDatabase db) {
            Context context = BrowserProvider2.this.getContext();
            File oldDbFile = context.getDatabasePath("browser.db");
            if (!oldDbFile.exists()) {
                return false;
            }
            BrowserProvider.DatabaseHelper helper = new BrowserProvider.DatabaseHelper(context);
            SQLiteDatabase oldDb = helper.getWritableDatabase();
            Cursor c = null;
            try {
                String table = BrowserProvider.TABLE_NAMES[0];
                Cursor c2 = oldDb.query(table, new String[]{"url", "title", "favicon", "touch_icon", "created"}, "bookmark!=0", null, null, null, null);
                if (c2 != null) {
                    while (c2.moveToNext()) {
                        String url = c2.getString(0);
                        if (!TextUtils.isEmpty(url)) {
                            ContentValues values = new ContentValues();
                            values.put("url", url);
                            values.put("title", c2.getString(1));
                            values.put("created", Integer.valueOf(c2.getInt(4)));
                            values.put("position", (Integer) 0);
                            values.put("parent", (Long) 1L);
                            ContentValues imageValues = new ContentValues();
                            imageValues.put("url_key", url);
                            imageValues.put("favicon", c2.getBlob(2));
                            imageValues.put("touch_icon", c2.getBlob(3));
                            db.insert("images", "thumbnail", imageValues);
                            db.insert("bookmarks", "dirty", values);
                        }
                    }
                    c2.close();
                }
                c = oldDb.query(table, new String[]{"url", "title", "visits", "date", "created"}, "visits > 0 OR bookmark = 0", null, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        ContentValues values2 = new ContentValues();
                        String url2 = c.getString(0);
                        if (!TextUtils.isEmpty(url2)) {
                            values2.put("url", url2);
                            values2.put("title", c.getString(1));
                            values2.put("visits", Integer.valueOf(c.getInt(2)));
                            values2.put("date", Long.valueOf(c.getLong(3)));
                            values2.put("created", Long.valueOf(c.getLong(4)));
                            db.insert("history", "favicon", values2);
                        }
                    }
                    c.close();
                }
                oldDb.delete(table, null, null);
                if (c != null) {
                    c.close();
                }
                oldDb.close();
                helper.close();
                if (!oldDbFile.delete()) {
                    oldDbFile.deleteOnExit();
                }
                return true;
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                oldDb.close();
                helper.close();
                throw th;
            }
        }

        void createAccountsView(SQLiteDatabase db) {
            db.execSQL("CREATE VIEW IF NOT EXISTS v_accounts AS SELECT NULL AS account_name, NULL AS account_type, 1 AS root_id UNION ALL SELECT account_name, account_type, _id AS root_id FROM bookmarks WHERE sync3 = \"bookmark_bar\" AND deleted = 0");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 32) {
                createOmniboxSuggestions(db);
            }
            if (oldVersion < 31) {
                createThumbnails(db);
            }
            if (oldVersion < 30) {
                db.execSQL("DROP VIEW IF EXISTS v_snapshots_combined");
                db.execSQL("DROP TABLE IF EXISTS snapshots");
            }
            if (oldVersion < 28) {
                enableSync(db);
            }
            if (oldVersion < 27) {
                createAccountsView(db);
            }
            if (oldVersion < 26) {
                db.execSQL("DROP VIEW IF EXISTS combined");
            }
            if (oldVersion < 25) {
                db.execSQL("DROP TABLE IF EXISTS bookmarks");
                db.execSQL("DROP TABLE IF EXISTS history");
                db.execSQL("DROP TABLE IF EXISTS searches");
                db.execSQL("DROP TABLE IF EXISTS images");
                db.execSQL("DROP TABLE IF EXISTS settings");
                BrowserProvider2.this.mSyncHelper.onAccountsChanged(db, new Account[0]);
                onCreate(db);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            BrowserProvider2.this.mSyncHelper.onDatabaseOpened(db);
        }

        private void createDefaultBookmarks(SQLiteDatabase db) {
            ContentValues values = new ContentValues();
            values.put("_id", (Long) 1L);
            values.put("sync3", "google_chrome_bookmarks");
            values.put("title", "Bookmarks");
            values.putNull("parent");
            values.put("position", (Integer) 0);
            values.put("folder", (Boolean) true);
            values.put("dirty", (Boolean) true);
            db.insertOrThrow("bookmarks", null, values);
            addDefaultBookmarks(db, 1L);
        }

        private void addDefaultBookmarks(SQLiteDatabase db, long parentId) {
            Resources res = BrowserProvider2.this.getContext().getResources();
            CharSequence[] bookmarks = res.getTextArray(R.array.bookmarks);
            int size = bookmarks.length;
            TypedArray preloads = res.obtainTypedArray(R.array.bookmark_preloads);
            try {
                String parent = Long.toString(parentId);
                String now = Long.toString(System.currentTimeMillis());
                for (int i = 0; i < size; i += 2) {
                    CharSequence bookmarkDestination = replaceSystemPropertyInString(BrowserProvider2.this.getContext(), bookmarks[i + 1]);
                    db.execSQL("INSERT INTO bookmarks (title, url, folder,parent,position,created) VALUES ('" + ((Object) bookmarks[i]) + "', '" + ((Object) bookmarkDestination) + "', 0," + parent + "," + Integer.toString(i) + "," + now + ");");
                    int faviconId = preloads.getResourceId(i, 0);
                    int thumbId = preloads.getResourceId(i + 1, 0);
                    byte[] thumb = null;
                    byte[] favicon = null;
                    try {
                        thumb = readRaw(res, thumbId);
                    } catch (IOException e) {
                    }
                    try {
                        favicon = readRaw(res, faviconId);
                    } catch (IOException e2) {
                    }
                    if (thumb != null || favicon != null) {
                        ContentValues imageValues = new ContentValues();
                        imageValues.put("url_key", bookmarkDestination.toString());
                        if (favicon != null) {
                            imageValues.put("favicon", favicon);
                        }
                        if (thumb != null) {
                            imageValues.put("thumbnail", thumb);
                        }
                        db.insert("images", "favicon", imageValues);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e3) {
            } finally {
                preloads.recycle();
            }
        }

        private byte[] readRaw(Resources res, int id) throws IOException {
            if (id == 0) {
                return null;
            }
            InputStream is = res.openRawResource(id);
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                while (true) {
                    int read = is.read(buf);
                    if (read > 0) {
                        bos.write(buf, 0, read);
                    } else {
                        bos.flush();
                        return bos.toByteArray();
                    }
                }
            } finally {
                is.close();
            }
        }

        private String getClientId(ContentResolver cr) {
            String ret = "android-google";
            Cursor c = null;
            try {
                c = cr.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='client_id'", null, null);
                if (c != null && c.moveToNext()) {
                    ret = c.getString(0);
                }
                if (c != null) {
                    c.close();
                }
            } catch (RuntimeException e) {
                if (c != null) {
                    c.close();
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                throw th;
            }
            return ret;
        }

        private CharSequence replaceSystemPropertyInString(Context context, CharSequence srcString) {
            StringBuffer sb = new StringBuffer();
            int lastCharLoc = 0;
            String client_id = getClientId(context.getContentResolver());
            int i = 0;
            while (i < srcString.length()) {
                char c = srcString.charAt(i);
                if (c == '{') {
                    sb.append(srcString.subSequence(lastCharLoc, i));
                    lastCharLoc = i;
                    int j = i;
                    while (true) {
                        if (j < srcString.length()) {
                            char k = srcString.charAt(j);
                            if (k != '}') {
                                j++;
                            } else {
                                String propertyKeyValue = srcString.subSequence(i + 1, j).toString();
                                if (propertyKeyValue.equals("CLIENT_ID")) {
                                    sb.append(client_id);
                                } else {
                                    sb.append("unknown");
                                }
                                lastCharLoc = j + 1;
                                i = j;
                            }
                        }
                    }
                }
                i++;
            }
            if (srcString.length() - lastCharLoc > 0) {
                sb.append(srcString.subSequence(lastCharLoc, srcString.length()));
            }
            return sb;
        }
    }

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        DatabaseHelper databaseHelper;
        synchronized (this) {
            if (this.mOpenHelper == null) {
                this.mOpenHelper = new DatabaseHelper(context);
            }
            databaseHelper = this.mOpenHelper;
        }
        return databaseHelper;
    }

    @Override
    public boolean isCallerSyncAdapter(Uri uri) {
        return uri.getBooleanQueryParameter("caller_is_syncadapter", false);
    }

    public void setWidgetObserver(ContentObserver obs) {
        this.mWidgetObserver = obs;
    }

    void refreshWidgets() {
        this.mUpdateWidgets = true;
    }

    @Override
    protected void onEndTransaction(boolean callerIsSyncAdapter) {
        super.onEndTransaction(callerIsSyncAdapter);
        if (this.mUpdateWidgets) {
            if (this.mWidgetObserver == null) {
                BookmarkThumbnailWidgetProvider.refreshWidgets(getContext());
            } else {
                this.mWidgetObserver.dispatchChange(false);
            }
            this.mUpdateWidgets = false;
        }
        this.mSyncToNetwork = true;
    }

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case 1000:
            case 9000:
                return "vnd.android.cursor.dir/bookmark";
            case 1001:
            case 9001:
                return "vnd.android.cursor.item/bookmark";
            case 2000:
                return "vnd.android.cursor.dir/browser-history";
            case 2001:
                return "vnd.android.cursor.item/browser-history";
            case 3000:
                return "vnd.android.cursor.dir/searches";
            case 3001:
                return "vnd.android.cursor.item/searches";
            default:
                return null;
        }
    }

    boolean isNullAccount(String account) {
        if (account == null) {
            return true;
        }
        String account2 = account.trim();
        return account2.length() == 0 || account2.equals("null");
    }

    Object[] getSelectionWithAccounts(Uri uri, String selection, String[] selectionArgs) {
        String accountType = uri.getQueryParameter("acct_type");
        String accountName = uri.getQueryParameter("acct_name");
        boolean hasAccounts = false;
        if (accountType != null && accountName != null) {
            if (!isNullAccount(accountType) && !isNullAccount(accountName)) {
                selection = DatabaseUtils.concatenateWhere(selection, "account_type=? AND account_name=? ");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{accountType, accountName});
                hasAccounts = true;
            } else {
                selection = DatabaseUtils.concatenateWhere(selection, "account_name IS NULL AND account_type IS NULL");
            }
        }
        return new Object[]{selection, selectionArgs, Boolean.valueOf(hasAccounts)};
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String query;
        String[] args;
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        int match = URI_MATCHER.match(uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = uri.getQueryParameter("limit");
        String groupBy = uri.getQueryParameter("groupBy");
        switch (match) {
            case 11:
                selection = DatabaseUtils.concatenateWhere(selection, "_id = ?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 10:
                qb.setTables("thumbnails");
                Cursor cursor = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor;
            case 20:
                qb.setTables("v_omnibox_suggestions");
                Cursor cursor2 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor2.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor2;
            case 1000:
            case 1001:
            case 1003:
                if (!uri.getBooleanQueryParameter("show_deleted", false)) {
                    selection = DatabaseUtils.concatenateWhere("deleted=0", selection);
                }
                if (match == 1001) {
                    selection = DatabaseUtils.concatenateWhere(selection, "bookmarks._id=?");
                    selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
                } else if (match == 1003) {
                    selection = DatabaseUtils.concatenateWhere(selection, "bookmarks.parent=?");
                    selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
                }
                Object[] withAccount = getSelectionWithAccounts(uri, selection, selectionArgs);
                selection = (String) withAccount[0];
                selectionArgs = (String[]) withAccount[1];
                boolean hasAccounts = ((Boolean) withAccount[2]).booleanValue();
                if (TextUtils.isEmpty(sortOrder)) {
                    if (hasAccounts) {
                        sortOrder = "position ASC, _id ASC";
                    } else {
                        sortOrder = "folder DESC, position ASC, _id ASC";
                    }
                }
                qb.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                qb.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
                Cursor cursor22 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor22.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor22;
            case 1002:
                boolean useAccount = false;
                String accountType = uri.getQueryParameter("acct_type");
                String accountName = uri.getQueryParameter("acct_name");
                if (!isNullAccount(accountType) && !isNullAccount(accountName)) {
                    useAccount = true;
                }
                qb.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
                if (TextUtils.isEmpty(sortOrder)) {
                    if (useAccount) {
                        sortOrder = "position ASC, _id ASC";
                    } else {
                        sortOrder = "folder DESC, position ASC, _id ASC";
                    }
                }
                if (!useAccount) {
                    qb.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                    String where = DatabaseUtils.concatenateWhere("parent=? AND deleted=0", selection);
                    args = new String[]{Long.toString(1L)};
                    if (selectionArgs != null) {
                        args = DatabaseUtils.appendSelectionArgs(args, selectionArgs);
                    }
                    query = qb.buildQuery(projection, where, null, null, sortOrder, null);
                } else {
                    qb.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                    String where2 = DatabaseUtils.concatenateWhere("account_type=? AND account_name=? AND parent = (SELECT _id FROM bookmarks WHERE sync3='bookmark_bar' AND account_type = ? AND account_name = ?) AND deleted=0", selection);
                    String bookmarksBarQuery = qb.buildQuery(projection, where2, null, null, null, null);
                    String[] args2 = {accountType, accountName, accountType, accountName};
                    if (selectionArgs != null) {
                        args2 = DatabaseUtils.appendSelectionArgs(args2, selectionArgs);
                    }
                    String where3 = DatabaseUtils.concatenateWhere("account_type=? AND account_name=? AND sync3=?", selection);
                    qb.setProjectionMap(OTHER_BOOKMARKS_PROJECTION_MAP);
                    String otherBookmarksQuery = qb.buildQuery(projection, where3, null, null, null, null);
                    query = qb.buildUnionQuery(new String[]{bookmarksBarQuery, otherBookmarksQuery}, sortOrder, limit);
                    args = DatabaseUtils.appendSelectionArgs(args2, new String[]{accountType, accountName, "other_bookmarks"});
                    if (selectionArgs != null) {
                        args = DatabaseUtils.appendSelectionArgs(args, selectionArgs);
                    }
                }
                Cursor cursor3 = db.rawQuery(query, args);
                if (cursor3 != null) {
                    cursor3.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                    return cursor3;
                }
                return cursor3;
            case 1004:
                Cursor cursor4 = doSuggestQuery(selection, selectionArgs, limit);
                return cursor4;
            case 1005:
                long id = queryDefaultFolderId(uri.getQueryParameter("acct_name"), uri.getQueryParameter("acct_type"));
                MatrixCursor c = new MatrixCursor(new String[]{"_id"});
                c.newRow().add(Long.valueOf(id));
                return c;
            case 2001:
                selection = DatabaseUtils.concatenateWhere(selection, "history._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                filterSearchClient(selectionArgs);
                if (sortOrder == null) {
                    sortOrder = "date DESC";
                }
                qb.setProjectionMap(HISTORY_PROJECTION_MAP);
                qb.setTables("history LEFT OUTER JOIN images ON history.url = images.url_key");
                Cursor cursor222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor222;
            case 3001:
                selection = DatabaseUtils.concatenateWhere(selection, "searches._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 3000:
                qb.setTables("searches");
                qb.setProjectionMap(SEARCHES_PROJECTION_MAP);
                Cursor cursor2222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor2222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor2222;
            case 4000:
                Cursor cursor5 = this.mSyncHelper.query(db, projection, selection, selectionArgs, sortOrder);
                return cursor5;
            case 4001:
                String selection2 = appendAccountToSelection(uri, selection);
                String selectionWithId = "_id=" + ContentUris.parseId(uri) + " " + (selection2 == null ? "" : " AND (" + selection2 + ")");
                Cursor cursor6 = this.mSyncHelper.query(db, projection, selectionWithId, selectionArgs, sortOrder);
                return cursor6;
            case 5000:
                qb.setTables("images");
                qb.setProjectionMap(IMAGES_PROJECTION_MAP);
                Cursor cursor22222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor22222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor22222;
            case 6001:
            case 9001:
                selection = DatabaseUtils.concatenateWhere(selection, "_id = CAST(? AS INTEGER)");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 6000:
            case 9000:
                if ((match == 9000 || match == 9001) && projection == null) {
                    projection = Browser.HISTORY_PROJECTION;
                }
                String[] args3 = createCombinedQuery(uri, projection, qb);
                if (selectionArgs == null) {
                    selectionArgs = args3;
                } else {
                    selectionArgs = DatabaseUtils.appendSelectionArgs(args3, selectionArgs);
                }
                Cursor cursor222222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor222222;
            case 7000:
                qb.setTables("v_accounts");
                qb.setProjectionMap(ACCOUNTS_PROJECTION_MAP);
                String allowEmpty = uri.getQueryParameter("allowEmptyAccounts");
                if ("false".equals(allowEmpty)) {
                    selection = DatabaseUtils.concatenateWhere(selection, "0 < ( SELECT count(*) FROM bookmarks WHERE deleted = 0 AND folder = 0   AND (     v_accounts.account_name = bookmarks.account_name     OR (v_accounts.account_name IS NULL AND bookmarks.account_name IS NULL)   )   AND (     v_accounts.account_type = bookmarks.account_type     OR (v_accounts.account_type IS NULL AND bookmarks.account_type IS NULL)   ) )");
                }
                if (sortOrder == null) {
                    sortOrder = "account_name IS NOT NULL DESC, account_name ASC";
                }
                Cursor cursor2222222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor2222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor2222222;
            case 8000:
                qb.setTables("settings");
                qb.setProjectionMap(SETTINGS_PROJECTION_MAP);
                Cursor cursor22222222 = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
                cursor22222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursor22222222;
            default:
                throw new UnsupportedOperationException("Unknown URL " + uri.toString());
        }
    }

    private Cursor doSuggestQuery(String selection, String[] selectionArgs, String limit) {
        String selection2;
        if (TextUtils.isEmpty(selectionArgs[0])) {
            selection2 = "history.date != 0";
            selectionArgs = null;
        } else {
            String like = selectionArgs[0] + "%";
            if (selectionArgs[0].startsWith("http") || selectionArgs[0].startsWith("file")) {
                selectionArgs[0] = like;
            } else {
                selectionArgs = new String[]{"http://" + like, "http://www." + like, "https://" + like, "https://www." + like, like, like};
                selection = "history.url LIKE ? OR history.url LIKE ? OR history.url LIKE ? OR history.url LIKE ? OR history.title LIKE ? OR bookmarks.title LIKE ?";
            }
            selection2 = DatabaseUtils.concatenateWhere(selection, "deleted=0 AND folder=0");
        }
        Cursor c = this.mOpenHelper.getReadableDatabase().query("history LEFT OUTER JOIN bookmarks ON history.url = bookmarks.url", SUGGEST_PROJECTION, selection2, selectionArgs, null, null, null, null);
        return new SuggestionsCursor(c);
    }

    private String[] createCombinedQuery(Uri uri, String[] projection, SQLiteQueryBuilder qb) {
        String[] args = null;
        StringBuilder whereBuilder = new StringBuilder(128);
        whereBuilder.append("deleted");
        whereBuilder.append(" = 0");
        Object[] withAccount = getSelectionWithAccounts(uri, null, null);
        String selection = (String) withAccount[0];
        String[] selectionArgs = (String[]) withAccount[1];
        if (selection != null) {
            whereBuilder.append(" AND " + selection);
            if (selectionArgs != null) {
                args = new String[selectionArgs.length * 2];
                System.arraycopy(selectionArgs, 0, args, 0, selectionArgs.length);
                System.arraycopy(selectionArgs, 0, args, selectionArgs.length, selectionArgs.length);
            }
        }
        String where = whereBuilder.toString();
        qb.setTables("bookmarks");
        String subQuery = qb.buildQuery(null, where, null, null, null, null);
        qb.setTables(String.format("history LEFT OUTER JOIN (%s) bookmarks ON history.url = bookmarks.url LEFT OUTER JOIN images ON history.url = images.url_key", subQuery));
        qb.setProjectionMap(COMBINED_HISTORY_PROJECTION_MAP);
        String historySubQuery = qb.buildQuery(null, null, null, null, null, null);
        qb.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
        qb.setProjectionMap(COMBINED_BOOKMARK_PROJECTION_MAP);
        String bookmarksSubQuery = qb.buildQuery(null, where + String.format(" AND %s NOT IN (SELECT %s FROM %s)", "url", "url", "history"), null, null, null, null);
        String query = qb.buildUnionQuery(new String[]{historySubQuery, bookmarksSubQuery}, null, null);
        qb.setTables("(" + query + ")");
        qb.setProjectionMap(null);
        return args;
    }

    int deleteBookmarks(String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        if (callerIsSyncAdapter) {
            return db.delete("bookmarks", selection, selectionArgs);
        }
        Object[] appendedBookmarks = appendBookmarksIfFolder(selection, selectionArgs);
        String selection2 = (String) appendedBookmarks[0];
        String[] selectionArgs2 = (String[]) appendedBookmarks[1];
        ContentValues values = new ContentValues();
        values.put("modified", Long.valueOf(System.currentTimeMillis()));
        values.put("deleted", (Integer) 1);
        return updateBookmarksInTransaction(values, selection2, selectionArgs2, callerIsSyncAdapter);
    }

    private Object[] appendBookmarksIfFolder(String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        String[] bookmarksProjection = {"_id", "folder"};
        StringBuilder newSelection = new StringBuilder(selection);
        List<String> newSelectionArgs = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query("bookmarks", bookmarksProjection, selection, selectionArgs, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = Long.toString(cursor.getLong(0));
                    newSelectionArgs.add(id);
                    if (cursor.getInt(1) != 0) {
                        Object[] bookmarks = appendBookmarksIfFolder("parent=?", new String[]{id});
                        String[] bookmarkIds = (String[]) bookmarks[1];
                        if (bookmarkIds.length > 0) {
                            newSelection.append(" OR bookmarks._id IN (");
                            for (String bookmarkId : bookmarkIds) {
                                newSelection.append("?,");
                                newSelectionArgs.add(bookmarkId);
                            }
                            newSelection.deleteCharAt(newSelection.length() - 1);
                            newSelection.append(")");
                        }
                    }
                }
            }
            return new Object[]{newSelection.toString(), newSelectionArgs.toArray(new String[newSelectionArgs.size()])};
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        String[] selectionArgs2;
        int match = URI_MATCHER.match(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int deleted = 0;
        switch (match) {
            case 11:
                selection = DatabaseUtils.concatenateWhere(selection, "_id = ?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 10:
                deleted = db.delete("thumbnails", selection, selectionArgs);
                if (deleted > 0) {
                    postNotifyUri(uri);
                    if (shouldNotifyLegacy(uri)) {
                        postNotifyUri(LEGACY_AUTHORITY_URI);
                    }
                }
                return deleted;
            case 1001:
                selection = DatabaseUtils.concatenateWhere(selection, "bookmarks._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 1000:
                Object[] withAccount = getSelectionWithAccounts(uri, selection, selectionArgs);
                deleted = deleteBookmarks((String) withAccount[0], (String[]) withAccount[1], callerIsSyncAdapter);
                pruneImages();
                if (deleted > 0) {
                    refreshWidgets();
                }
                if (deleted > 0) {
                }
                return deleted;
            case 2001:
                selection = DatabaseUtils.concatenateWhere(selection, "history._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                filterSearchClient(selectionArgs);
                deleted = db.delete("history", selection, selectionArgs);
                pruneImages();
                if (deleted > 0) {
                }
                return deleted;
            case 3001:
                selection = DatabaseUtils.concatenateWhere(selection, "searches._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 3000:
                deleted = db.delete("searches", selection, selectionArgs);
                if (deleted > 0) {
                }
                return deleted;
            case 4000:
                deleted = this.mSyncHelper.delete(db, selection, selectionArgs);
                if (deleted > 0) {
                }
                return deleted;
            case 4001:
                String selectionWithId = "_id=" + ContentUris.parseId(uri) + " " + (selection == null ? "" : " AND (" + selection + ")");
                deleted = this.mSyncHelper.delete(db, selectionWithId, selectionArgs);
                if (deleted > 0) {
                }
                return deleted;
            case 9001:
                selection = DatabaseUtils.concatenateWhere(selection, "_id = CAST(? AS INTEGER)");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 9000:
                String[] projection = {"_id", "bookmark", "url"};
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                String[] args = createCombinedQuery(uri, projection, qb);
                if (selectionArgs == null) {
                    selectionArgs2 = args;
                } else {
                    selectionArgs2 = DatabaseUtils.appendSelectionArgs(args, selectionArgs);
                }
                Cursor c = qb.query(db, projection, selection, selectionArgs2, null, null, null);
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    boolean isBookmark = c.getInt(1) != 0;
                    String url = c.getString(2);
                    if (isBookmark) {
                        deleted += deleteBookmarks("_id=?", new String[]{Long.toString(id)}, callerIsSyncAdapter);
                        db.delete("history", "url=?", new String[]{url});
                    } else {
                        deleted += db.delete("history", "_id=?", new String[]{Long.toString(id)});
                    }
                }
                c.close();
                if (deleted > 0) {
                }
                return deleted;
            default:
                throw new UnsupportedOperationException("Unknown delete URI " + uri);
        }
    }

    long queryDefaultFolderId(String accountName, String accountType) {
        if (!isNullAccount(accountName) && !isNullAccount(accountType)) {
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            Cursor c = db.query("bookmarks", new String[]{"_id"}, "sync3 = ? AND account_type = ? AND account_name = ?", new String[]{"bookmark_bar", accountType, accountName}, null, null, null);
            try {
                if (c.moveToFirst()) {
                    return c.getLong(0);
                }
            } finally {
                c.close();
            }
        }
        return 1L;
    }

    @Override
    public Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        long id;
        Log.d("cjzang", "insertInTransaction------------begin callerIsSyncAdapter = " + callerIsSyncAdapter);
        int match = URI_MATCHER.match(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        Log.d("cjzang", "insertInTransaction------------1100-----values =" + values);
        if (match == 9000) {
            Integer bookmark = values.getAsInteger("bookmark");
            values.remove("bookmark");
            if (bookmark == null || bookmark.intValue() == 0) {
                match = 2000;
            } else {
                match = 1000;
                values.remove("date");
                values.remove("visits");
                values.remove("user_entered");
                values.put("folder", (Integer) 0);
            }
            Log.d("cjzang", "insertInTransaction------------00-----values =" + values);
        }
        Log.d("cjzang", "insertInTransaction-------------11-----values =" + values);
        switch (match) {
            case 10:
                id = db.replaceOrThrow("thumbnails", null, values);
                break;
            case 1000:
                Log.d("cjzang", "insertInTransaction------------10-----");
                if (!callerIsSyncAdapter) {
                    long now = System.currentTimeMillis();
                    values.put("created", Long.valueOf(now));
                    values.put("modified", Long.valueOf(now));
                    values.put("dirty", (Integer) 1);
                    boolean hasAccounts = values.containsKey("account_type") || values.containsKey("account_name");
                    String accountType = values.getAsString("account_type");
                    String accountName = values.getAsString("account_name");
                    boolean hasParent = values.containsKey("parent");
                    if (hasParent && hasAccounts) {
                        long parentId = values.getAsLong("parent").longValue();
                        hasParent = isValidParent(accountType, accountName, parentId);
                    } else if (hasParent && !hasAccounts) {
                        long parentId2 = values.getAsLong("parent").longValue();
                        hasParent = setParentValues(parentId2, values);
                    }
                    if (!hasParent) {
                        values.put("parent", Long.valueOf(queryDefaultFolderId(accountName, accountType)));
                    }
                }
                if (!values.containsKey("position")) {
                    values.put("position", Long.toString(Long.MIN_VALUE));
                }
                String url = values.getAsString("url");
                Log.d("cjzang", "insertInTransaction------------88007-----" + values + "----------url =" + url);
                ContentValues imageValues = extractImageValues(values, url);
                Log.d("cjzang", "insertInTransaction------------8800-----values =" + imageValues);
                Boolean isFolder = values.getAsBoolean("folder");
                if ((isFolder == null || !isFolder.booleanValue()) && imageValues != null && !TextUtils.isEmpty(url)) {
                    Log.d("cjzang", "insertInTransaction------------11-----");
                    int count = db.update("images", imageValues, "url_key=?", new String[]{url});
                    if (count == 0) {
                        Log.d("cjzang", "insertInTransaction------------22-----");
                        db.insertOrThrow("images", "favicon", imageValues);
                        Log.d("cjzang", "insertInTransaction------------33-----");
                    }
                }
                Log.d("cjzang", "insertInTransaction------44------values =" + values);
                id = db.insertOrThrow("bookmarks", "dirty", values);
                refreshWidgets();
                Log.d("cjzang", "insertInTransaction------------55-----");
                break;
            case 2000:
                if (!values.containsKey("created")) {
                    values.put("created", Long.valueOf(System.currentTimeMillis()));
                }
                values.put("url", filterSearchClient(values.getAsString("url")));
                ContentValues imageValues2 = extractImageValues(values, values.getAsString("url"));
                if (imageValues2 != null) {
                    db.insertOrThrow("images", "favicon", imageValues2);
                }
                id = db.insertOrThrow("history", "visits", values);
                break;
            case 3000:
                id = insertSearchesInTransaction(db, values);
                break;
            case 4000:
                id = this.mSyncHelper.insert(db, values);
                break;
            case 8000:
                id = 0;
                insertSettingsInTransaction(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown insert URI " + uri);
        }
        if (id >= 0) {
            postNotifyUri(uri);
            if (shouldNotifyLegacy(uri)) {
                postNotifyUri(LEGACY_AUTHORITY_URI);
            }
            Log.d("cjzang", "insertInTransaction------------66-----");
            return ContentUris.withAppendedId(uri, id);
        }
        Log.d("cjzang", "insertInTransaction------------77-----");
        return null;
    }

    private String[] getAccountNameAndType(long id) {
        String[] strArr = null;
        if (id > 0) {
            Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, id);
            Cursor c = query(uri, new String[]{"account_name", "account_type"}, null, null, null);
            try {
                if (c.moveToFirst()) {
                    String parentName = c.getString(0);
                    String parentType = c.getString(1);
                    strArr = new String[]{parentName, parentType};
                }
            } finally {
                c.close();
            }
        }
        return strArr;
    }

    private boolean setParentValues(long parentId, ContentValues values) {
        String[] parent = getAccountNameAndType(parentId);
        if (parent == null) {
            return false;
        }
        values.put("account_name", parent[0]);
        values.put("account_type", parent[1]);
        return true;
    }

    private boolean isValidParent(String accountType, String accountName, long parentId) {
        String[] parent = getAccountNameAndType(parentId);
        return parent != null && TextUtils.equals(accountName, parent[0]) && TextUtils.equals(accountType, parent[1]);
    }

    private void filterSearchClient(String[] selectionArgs) {
        if (selectionArgs != null) {
            for (int i = 0; i < selectionArgs.length; i++) {
                selectionArgs[i] = filterSearchClient(selectionArgs[i]);
            }
        }
    }

    private String filterSearchClient(String url) {
        int index = url.indexOf("client=");
        if (index > 0 && url.contains(".google.")) {
            int end = url.indexOf(38, index);
            if (end > 0) {
                return url.substring(0, index).concat(url.substring(end + 1));
            }
            return url.substring(0, index - 1);
        }
        return url;
    }

    private long insertSearchesInTransaction(SQLiteDatabase db, ContentValues values) {
        long id;
        String search = values.getAsString("search");
        if (TextUtils.isEmpty(search)) {
            throw new IllegalArgumentException("Must include the SEARCH field");
        }
        Cursor cursor = null;
        try {
            cursor = db.query("searches", new String[]{"_id"}, "search=?", new String[]{search}, null, null, null);
            if (cursor.moveToNext()) {
                id = cursor.getLong(0);
                db.update("searches", values, "_id=?", new String[]{Long.toString(id)});
            } else {
                id = db.insertOrThrow("searches", "search", values);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return id;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long insertSettingsInTransaction(SQLiteDatabase db, ContentValues values) {
        long id;
        String key = values.getAsString("key");
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Must include the KEY field");
        }
        String[] keyArray = {key};
        Cursor cursor = null;
        try {
            cursor = db.query("settings", new String[]{"key"}, "key=?", keyArray, null, null, null);
            if (cursor.moveToNext()) {
                id = cursor.getLong(0);
                db.update("settings", values, "key=?", keyArray);
            } else {
                id = db.insertOrThrow("settings", "value", values);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return id;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int match = URI_MATCHER.match(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        if (match == 9000 || match == 9001) {
            Integer bookmark = values.getAsInteger("bookmark");
            values.remove("bookmark");
            if (bookmark == null || bookmark.intValue() == 0) {
                if (match == 9000) {
                    match = 2000;
                } else {
                    match = 2001;
                }
            } else {
                if (match == 9000) {
                    match = 1000;
                } else {
                    match = 1001;
                }
                values.remove("date");
                values.remove("visits");
                values.remove("user_entered");
            }
        }
        int modified = 0;
        switch (match) {
            case 10:
                modified = db.update("thumbnails", values, selection, selectionArgs);
                pruneImages();
                if (modified > 0) {
                    postNotifyUri(uri);
                    if (shouldNotifyLegacy(uri)) {
                        postNotifyUri(LEGACY_AUTHORITY_URI);
                    }
                }
                return modified;
            case 1001:
                selection = DatabaseUtils.concatenateWhere(selection, "bookmarks._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 1000:
                Object[] withAccount = getSelectionWithAccounts(uri, selection, selectionArgs);
                modified = updateBookmarksInTransaction(values, (String) withAccount[0], (String[]) withAccount[1], callerIsSyncAdapter);
                if (modified > 0) {
                    refreshWidgets();
                }
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            case 2001:
                selection = DatabaseUtils.concatenateWhere(selection, "history._id=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                modified = updateHistoryInTransaction(values, selection, selectionArgs);
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            case 3000:
                modified = db.update("searches", values, selection, selectionArgs);
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            case 4000:
                modified = this.mSyncHelper.update(this.mDb, values, appendAccountToSelection(uri, selection), selectionArgs);
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            case 4001:
                String selection2 = appendAccountToSelection(uri, selection);
                String selectionWithId = "_id=" + ContentUris.parseId(uri) + " " + (selection2 == null ? "" : " AND (" + selection2 + ")");
                modified = this.mSyncHelper.update(this.mDb, values, selectionWithId, selectionArgs);
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            case 5000:
                String url = values.getAsString("url_key");
                if (TextUtils.isEmpty(url)) {
                    throw new IllegalArgumentException("Images.URL is required");
                }
                if (!shouldUpdateImages(db, url, values)) {
                    return 0;
                }
                int count = db.update("images", values, "url_key=?", new String[]{url});
                if (count == 0) {
                    db.insertOrThrow("images", "favicon", values);
                    count = 1;
                }
                boolean updatedLegacy = false;
                if (getUrlCount(db, "bookmarks", url) > 0) {
                    postNotifyUri(BrowserContract.Bookmarks.CONTENT_URI);
                    updatedLegacy = values.containsKey("favicon");
                    refreshWidgets();
                }
                if (getUrlCount(db, "history", url) > 0) {
                    postNotifyUri(BrowserContract.History.CONTENT_URI);
                    updatedLegacy = values.containsKey("favicon");
                }
                if (pruneImages() > 0 || updatedLegacy) {
                    postNotifyUri(LEGACY_AUTHORITY_URI);
                }
                this.mSyncToNetwork = false;
                return count;
            case 7000:
                Account[] accounts = AccountManager.get(getContext()).getAccounts();
                this.mSyncHelper.onAccountsChanged(this.mDb, accounts);
                pruneImages();
                if (modified > 0) {
                }
                return modified;
            default:
                throw new UnsupportedOperationException("Unknown update URI " + uri);
        }
    }

    private boolean shouldUpdateImages(SQLiteDatabase db, String url, ContentValues values) {
        String[] projection = {"favicon", "thumbnail", "touch_icon"};
        Cursor cursor = db.query("images", projection, "url_key=?", new String[]{url}, null, null, null);
        byte[] nfavicon = values.getAsByteArray("favicon");
        byte[] nthumb = values.getAsByteArray("thumbnail");
        byte[] ntouch = values.getAsByteArray("touch_icon");
        try {
            if (cursor.getCount() <= 0) {
                return (nfavicon == null && nthumb == null && ntouch == null) ? false : true;
            }
            while (cursor.moveToNext()) {
                if (nfavicon != null) {
                    byte[] cfavicon = cursor.getBlob(0);
                    if (!Arrays.equals(nfavicon, cfavicon)) {
                        return true;
                    }
                }
                if (nthumb != null) {
                    byte[] cthumb = cursor.getBlob(1);
                    if (!Arrays.equals(nthumb, cthumb)) {
                        return true;
                    }
                }
                if (ntouch != null) {
                    byte[] ctouch = cursor.getBlob(2);
                    if (!Arrays.equals(ntouch, ctouch)) {
                        return true;
                    }
                }
            }
            cursor.close();
            return false;
        } finally {
            cursor.close();
        }
    }

    int getUrlCount(SQLiteDatabase db, String table, String url) {
        Cursor c = db.query(table, new String[]{"COUNT(*)"}, "url = ?", new String[]{url}, null, null, null);
        int count = 0;
        try {
            if (c.moveToFirst()) {
                count = c.getInt(0);
            }
            return count;
        } finally {
            c.close();
        }
    }

    int updateBookmarksInTransaction(ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int count = 0;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        String[] bookmarksProjection = {"_id", "version", "url", "title", "folder", "account_name", "account_type"};
        Cursor cursor = db.query("bookmarks", bookmarksProjection, selection, selectionArgs, null, null, null);
        boolean updatingParent = values.containsKey("parent");
        String parentAccountName = null;
        String parentAccountType = null;
        if (updatingParent) {
            long parent = values.getAsLong("parent").longValue();
            Cursor c = db.query("bookmarks", new String[]{"account_name", "account_type"}, "_id = ?", new String[]{Long.toString(parent)}, null, null, null);
            if (c.moveToFirst()) {
                parentAccountName = c.getString(0);
                parentAccountType = c.getString(1);
            }
            c.close();
        } else if (values.containsKey("account_name") || values.containsKey("account_type")) {
        }
        try {
            String[] args = new String[1];
            if (!callerIsSyncAdapter) {
                values.put("modified", Long.valueOf(System.currentTimeMillis()));
                values.put("dirty", (Integer) 1);
            }
            boolean updatingUrl = values.containsKey("url");
            String url = null;
            if (updatingUrl) {
                url = values.getAsString("url");
            }
            ContentValues imageValues = extractImageValues(values, url);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                args[0] = Long.toString(id);
                String accountName = cursor.getString(5);
                String accountType = cursor.getString(6);
                if (updatingParent && (!TextUtils.equals(accountName, parentAccountName) || !TextUtils.equals(accountType, parentAccountType))) {
                    ContentValues newValues = valuesFromCursor(cursor);
                    newValues.putAll(values);
                    newValues.remove("_id");
                    newValues.remove("version");
                    newValues.put("account_name", parentAccountName);
                    newValues.put("account_type", parentAccountType);
                    Uri insertUri = insertInTransaction(BrowserContract.Bookmarks.CONTENT_URI, newValues, callerIsSyncAdapter);
                    long newId = ContentUris.parseId(insertUri);
                    if (cursor.getInt(4) != 0) {
                        ContentValues updateChildren = new ContentValues(1);
                        updateChildren.put("parent", Long.valueOf(newId));
                        count += updateBookmarksInTransaction(updateChildren, "parent=?", new String[]{Long.toString(id)}, callerIsSyncAdapter);
                    }
                    Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, id);
                    deleteInTransaction(uri, null, null, callerIsSyncAdapter);
                    count++;
                } else {
                    if (!callerIsSyncAdapter) {
                        values.put("version", Long.valueOf(cursor.getLong(1) + 1));
                    }
                    count += db.update("bookmarks", values, "_id=?", args);
                }
                if (imageValues != null) {
                    if (!updatingUrl) {
                        url = cursor.getString(2);
                        imageValues.put("url_key", url);
                    }
                    if (!TextUtils.isEmpty(url)) {
                        args[0] = url;
                        if (db.update("images", imageValues, "url_key=?", args) == 0) {
                            db.insert("images", "favicon", imageValues);
                        }
                    }
                }
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    ContentValues valuesFromCursor(Cursor c) {
        int count = c.getColumnCount();
        ContentValues values = new ContentValues(count);
        String[] colNames = c.getColumnNames();
        for (int i = 0; i < count; i++) {
            switch (c.getType(i)) {
                case 1:
                    values.put(colNames[i], Long.valueOf(c.getLong(i)));
                    break;
                case 2:
                    values.put(colNames[i], Float.valueOf(c.getFloat(i)));
                    break;
                case 3:
                    values.put(colNames[i], c.getString(i));
                    break;
                case 4:
                    values.put(colNames[i], c.getBlob(i));
                    break;
            }
        }
        return values;
    }

    int updateHistoryInTransaction(ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        filterSearchClient(selectionArgs);
        Cursor cursor = query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "url"}, selection, selectionArgs, null);
        try {
            String[] args = new String[1];
            boolean updatingUrl = values.containsKey("url");
            String url = null;
            if (updatingUrl) {
                url = filterSearchClient(values.getAsString("url"));
                values.put("url", url);
            }
            ContentValues imageValues = extractImageValues(values, url);
            while (cursor.moveToNext()) {
                args[0] = cursor.getString(0);
                count += db.update("history", values, "_id=?", args);
                if (imageValues != null) {
                    if (!updatingUrl) {
                        url = cursor.getString(1);
                        imageValues.put("url_key", url);
                    }
                    args[0] = url;
                    if (db.update("images", imageValues, "url_key=?", args) == 0) {
                        db.insert("images", "favicon", imageValues);
                    }
                }
            }
            return count;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    String appendAccountToSelection(Uri uri, String selection) {
        String accountName = uri.getQueryParameter("account_name");
        String accountType = uri.getQueryParameter("account_type");
        boolean partialUri = TextUtils.isEmpty(accountName) ^ TextUtils.isEmpty(accountType);
        if (partialUri) {
            throw new IllegalArgumentException("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE for " + uri);
        }
        boolean validAccount = !TextUtils.isEmpty(accountName);
        if (validAccount) {
            StringBuilder selectionSb = new StringBuilder("account_name=" + DatabaseUtils.sqlEscapeString(accountName) + " AND account_type=" + DatabaseUtils.sqlEscapeString(accountType));
            if (!TextUtils.isEmpty(selection)) {
                selectionSb.append(" AND (");
                selectionSb.append(selection);
                selectionSb.append(')');
            }
            return selectionSb.toString();
        }
        return selection;
    }

    ContentValues extractImageValues(ContentValues values, String url) {
        ContentValues imageValues = null;
        if (values.containsKey("favicon")) {
            imageValues = new ContentValues();
            imageValues.put("favicon", values.getAsByteArray("favicon"));
            values.remove("favicon");
        }
        if (values.containsKey("thumbnail")) {
            if (imageValues == null) {
                imageValues = new ContentValues();
            }
            imageValues.put("thumbnail", values.getAsByteArray("thumbnail"));
            values.remove("thumbnail");
        }
        if (values.containsKey("touch_icon")) {
            if (imageValues == null) {
                imageValues = new ContentValues();
            }
            imageValues.put("touch_icon", values.getAsByteArray("touch_icon"));
            values.remove("touch_icon");
        }
        if (imageValues != null) {
            imageValues.put("url_key", url);
        }
        return imageValues;
    }

    int pruneImages() {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        return db.delete("images", "url_key NOT IN (SELECT url FROM bookmarks WHERE url IS NOT NULL AND deleted == 0) AND url_key NOT IN (SELECT url FROM history WHERE url IS NOT NULL)", null);
    }

    boolean shouldNotifyLegacy(Uri uri) {
        return uri.getPathSegments().contains("history") || uri.getPathSegments().contains("bookmarks") || uri.getPathSegments().contains("searches");
    }

    @Override
    protected boolean syncToNetwork(Uri uri) {
        if ("com.android.browser".equals(uri.getAuthority()) && uri.getPathSegments().contains("bookmarks")) {
            return this.mSyncToNetwork;
        }
        if ("browser".equals(uri.getAuthority())) {
            return true;
        }
        return false;
    }

    static class SuggestionsCursor extends AbstractCursor {
        private static final String[] COLUMNS = {"_id", "suggest_intent_action", "suggest_intent_data", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_last_access_hint"};
        private final Cursor mSource;

        public SuggestionsCursor(Cursor cursor) {
            this.mSource = cursor;
        }

        @Override
        public String[] getColumnNames() {
            return COLUMNS;
        }

        @Override
        public String getString(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return this.mSource.getString(columnIndex);
                case 1:
                    return "android.intent.action.VIEW";
                case 2:
                    return this.mSource.getString(1);
                case 3:
                    return this.mSource.getString(2);
                case 4:
                case 5:
                    return UrlUtils.stripUrl(this.mSource.getString(1));
                case 6:
                    return this.mSource.getString(3);
                case 7:
                    return this.mSource.getString(4);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return this.mSource.getCount();
        }

        @Override
        public double getDouble(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloat(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInt(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLong(int column) {
            switch (column) {
                case 0:
                    return this.mSource.getLong(0);
                case 7:
                    return this.mSource.getLong(4);
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public short getShort(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNull(int column) {
            return this.mSource.isNull(column);
        }

        @Override
        public boolean onMove(int oldPosition, int newPosition) {
            return this.mSource.moveToPosition(newPosition);
        }
    }
}
