package com.android.browser.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.BrowserSettings;
import com.android.browser.Extensions;
import com.android.browser.UrlUtils;
import com.android.browser.provider.BrowserContract;
import com.android.browser.provider.BrowserProvider;
import com.android.browser.widget.BookmarkThumbnailWidgetProvider;
import com.android.common.content.SyncStateContentProviderHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

public class BrowserProvider2 extends SQLiteContentProvider {
    DatabaseHelper mOpenHelper;
    static final Uri LEGACY_AUTHORITY_URI = new Uri.Builder().authority("MtkBrowserProvider").scheme("content").build();
    private static final String[] SUGGEST_PROJECTION = {qualifyColumn("history", "_id"), qualifyColumn("history", "url"), bookmarkOrHistoryColumn("title"), bookmarkOrHistoryLiteral("url", Integer.toString(2130837537), Integer.toString(2130837560)), qualifyColumn("history", "date")};
    public static final String[] BOOKMARK_FOLDERS_PROJECTION = {"_id", "parent_id", "folder_level", "name", "date", "visits"};
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

    final class DatabaseHelper extends SQLiteOpenHelper {
        final BrowserProvider2 this$0;

        public DatabaseHelper(BrowserProvider2 browserProvider2, Context context) {
            super(context, "browser2.db", (SQLiteDatabase.CursorFactory) null, 32);
            this.this$0 = browserProvider2;
            setWriteAheadLoggingEnabled(true);
        }

        private int addDefaultBookmarks(SQLiteDatabase sQLiteDatabase, long j, CharSequence[] charSequenceArr, TypedArray typedArray, int i) {
            byte[] raw;
            Resources resources = this.this$0.getContext().getResources();
            int length = charSequenceArr.length;
            try {
                String string = Long.toString(j);
                String string2 = Long.toString(System.currentTimeMillis());
                for (int i2 = 0; i2 < length; i2 += 2) {
                    int i3 = i2 + 1;
                    CharSequence charSequenceReplaceSystemPropertyInString = BrowserProvider2.replaceSystemPropertyInString(this.this$0.getContext(), charSequenceArr[i3]);
                    boolean z = "http://www.google.com/".equals(charSequenceReplaceSystemPropertyInString.toString());
                    StringBuilder sb = new StringBuilder();
                    sb.append("INSERT INTO bookmarks (title, url, folder,parent,position,created) VALUES ('");
                    sb.append((Object) charSequenceArr[i2]);
                    sb.append("', '");
                    sb.append((Object) charSequenceReplaceSystemPropertyInString);
                    sb.append("', 0,");
                    sb.append(string);
                    sb.append(",");
                    sb.append(z ? 1 : Integer.toString(i + i2));
                    sb.append(",");
                    sb.append(string2);
                    sb.append(");");
                    sQLiteDatabase.execSQL(sb.toString());
                    int resourceId = typedArray.getResourceId(i2, 0);
                    byte[] raw2 = null;
                    try {
                        raw = readRaw(resources, typedArray.getResourceId(i3, 0));
                    } catch (IOException e) {
                        raw = null;
                    }
                    try {
                        raw2 = readRaw(resources, resourceId);
                    } catch (IOException e2) {
                    }
                    if (raw != null || raw2 != null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("url_key", charSequenceReplaceSystemPropertyInString.toString());
                        if (raw2 != null) {
                            contentValues.put("favicon", raw2);
                        }
                        if (raw != null) {
                            contentValues.put("thumbnail", raw);
                        }
                        sQLiteDatabase.insert("images", "favicon", contentValues);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e3) {
            } catch (Throwable th) {
                typedArray.recycle();
                throw th;
            }
            typedArray.recycle();
            return length;
        }

        private void createDefaultBookmarks(SQLiteDatabase sQLiteDatabase) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", (Long) 1L);
            contentValues.put("sync3", "google_chrome_bookmarks");
            contentValues.put("title", "Bookmarks");
            contentValues.putNull("parent");
            contentValues.put("position", (Integer) 0);
            contentValues.put("folder", (Boolean) true);
            contentValues.put("dirty", (Boolean) true);
            sQLiteDatabase.insertOrThrow("bookmarks", null, contentValues);
            int iAddDefaultBookmarksForCustomer = Extensions.getBookmarkPlugin(this.this$0.getContext()).addDefaultBookmarksForCustomer(sQLiteDatabase);
            if (iAddDefaultBookmarksForCustomer == 0) {
                addDefaultBookmarks(sQLiteDatabase, 1L, addDefaultBookmarksForYahoo(sQLiteDatabase, 1L, (iAddDefaultBookmarksForCustomer <= 0 ? 2 : 1) + iAddDefaultBookmarksForCustomer));
            }
        }

        private byte[] readRaw(Resources resources, int i) throws IOException {
            if (i == 0) {
                return null;
            }
            InputStream inputStreamOpenRawResource = resources.openRawResource(i);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[4096];
                while (true) {
                    int i2 = inputStreamOpenRawResource.read(bArr);
                    if (i2 <= 0) {
                        byteArrayOutputStream.flush();
                        return byteArrayOutputStream.toByteArray();
                    }
                    byteArrayOutputStream.write(bArr, 0, i2);
                }
            } finally {
                inputStreamOpenRawResource.close();
            }
        }

        public int addDefaultBookmarks(SQLiteDatabase sQLiteDatabase, long j, int i) {
            Resources resources = this.this$0.getContext().getResources();
            CharSequence[] textArray = resources.getTextArray(2131230834);
            int length = textArray.length;
            return addDefaultBookmarks(sQLiteDatabase, j, textArray, resources.obtainTypedArray(2131230810), i);
        }

        public int addDefaultBookmarksForYahoo(SQLiteDatabase sQLiteDatabase, long j, int i) {
            Resources resources = this.this$0.getContext().getResources();
            CharSequence[] textArray = resources.getTextArray(2131230812);
            int length = textArray.length;
            return addDefaultBookmarks(sQLiteDatabase, j, textArray, resources.obtainTypedArray(2131230809), i);
        }

        void createAccountsView(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE VIEW IF NOT EXISTS v_accounts AS SELECT NULL AS account_name, NULL AS account_type, 1 AS root_id UNION ALL SELECT account_name, account_type, _id AS root_id FROM bookmarks WHERE sync3 = \"bookmark_bar\" AND deleted = 0");
        }

        void createOmniboxSuggestions(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE VIEW IF NOT EXISTS v_omnibox_suggestions  AS   SELECT _id, url, title, 1 AS bookmark, 0 AS visits, 0 AS date  FROM bookmarks   WHERE deleted = 0 AND folder = 0   UNION ALL   SELECT _id, url, title, 0 AS bookmark, visits, date   FROM history   WHERE url NOT IN (SELECT url FROM bookmarks    WHERE deleted = 0 AND folder = 0)   ORDER BY bookmark DESC, visits DESC, date DESC ");
        }

        void createThumbnails(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS thumbnails (_id INTEGER PRIMARY KEY,thumbnail BLOB NOT NULL);");
        }

        void enableSync(SQLiteDatabase sQLiteDatabase) throws Throwable {
            Account[] accountsByType;
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", "sync_enabled");
            contentValues.put("value", (Integer) 1);
            this.this$0.insertSettingsInTransaction(sQLiteDatabase, contentValues);
            AccountManager accountManager = (AccountManager) this.this$0.getContext().getSystemService("account");
            if (accountManager == null || (accountsByType = accountManager.getAccountsByType("com.google")) == null || accountsByType.length == 0) {
                return;
            }
            for (Account account : accountsByType) {
                if (ContentResolver.getIsSyncable(account, "com.android.browser.provider") == 0) {
                    ContentResolver.setIsSyncable(account, "com.android.browser.provider", 1);
                    ContentResolver.setSyncAutomatically(account, "com.android.browser.provider", true);
                }
            }
        }

        boolean importFromBrowserProvider(SQLiteDatabase sQLiteDatabase) throws Throwable {
            int i = 0;
            Cursor cursorQuery = null;
            Context context = this.this$0.getContext();
            File databasePath = context.getDatabasePath("browser.db");
            if (!databasePath.exists()) {
                return false;
            }
            BrowserProvider.DatabaseHelper databaseHelper = new BrowserProvider.DatabaseHelper(context);
            SQLiteDatabase writableDatabase = databaseHelper.getWritableDatabase();
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_id", (Long) 1L);
                contentValues.put("sync3", "google_chrome_bookmarks");
                contentValues.put("title", "Bookmarks");
                contentValues.putNull("parent");
                contentValues.put("position", (Integer) 0);
                contentValues.put("folder", (Boolean) true);
                contentValues.put("dirty", (Boolean) true);
                sQLiteDatabase.insertOrThrow("bookmarks", null, contentValues);
                String str = BrowserProvider.TABLE_NAMES[2];
                writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS bookmark_folders (_id INTEGER PRIMARY KEY,parent_id INTEGER,folder_level INTEGER,name TEXT,date LONG,visits INTEGER);");
                cursorQuery = writableDatabase.query(str, BrowserProvider2.BOOKMARK_FOLDERS_PROJECTION, null, null, null, null, "visits DESC");
                if (cursorQuery != null) {
                    int i2 = 0;
                    while (cursorQuery.moveToNext()) {
                        try {
                            i2++;
                            ContentValues contentValues2 = new ContentValues();
                            contentValues2.put("_id", Integer.valueOf(cursorQuery.getInt(0) + 1));
                            contentValues2.put("title", cursorQuery.getString(3));
                            contentValues2.put("created", Integer.valueOf(cursorQuery.getInt(4)));
                            contentValues2.put("position", Integer.valueOf(i2));
                            contentValues2.put("folder", (Boolean) true);
                            contentValues2.put("parent", Integer.valueOf(cursorQuery.getInt(1) + 1));
                            sQLiteDatabase.insertOrThrow("bookmarks", "dirty", contentValues2);
                        } catch (Throwable th) {
                            th = th;
                            cursorQuery = cursorQuery;
                            if (cursorQuery != null) {
                            }
                            writableDatabase.close();
                            databaseHelper.close();
                            throw th;
                        }
                    }
                    cursorQuery.close();
                    i = i2;
                }
                try {
                    String str2 = BrowserProvider.TABLE_NAMES[0];
                    try {
                        cursorQuery = writableDatabase.query(str2, new String[]{"url", "title", "favicon", "touch_icon", "created", "folder_id"}, "bookmark!=0", null, null, null, "visits DESC");
                        if (cursorQuery != null) {
                            while (cursorQuery.moveToNext()) {
                                String string = cursorQuery.getString(0);
                                if (!TextUtils.isEmpty(string)) {
                                    i++;
                                    ContentValues contentValues3 = new ContentValues();
                                    contentValues3.put("url", string);
                                    contentValues3.put("title", cursorQuery.getString(1));
                                    contentValues3.put("created", Integer.valueOf(cursorQuery.getInt(4)));
                                    contentValues3.put("position", Integer.valueOf(i));
                                    contentValues3.put("parent", Integer.valueOf(cursorQuery.getInt(5) + 1));
                                    ContentValues contentValues4 = new ContentValues();
                                    contentValues4.put("url_key", string);
                                    contentValues4.put("favicon", cursorQuery.getBlob(2));
                                    contentValues4.put("touch_icon", cursorQuery.getBlob(3));
                                    sQLiteDatabase.insert("images", "thumbnail", contentValues4);
                                    sQLiteDatabase.insert("bookmarks", "dirty", contentValues3);
                                }
                            }
                            cursorQuery.close();
                        }
                        cursorQuery = writableDatabase.query(str2, new String[]{"url", "title", "visits", "date", "created"}, "visits > 0 OR bookmark = 0", null, null, null, null);
                        if (cursorQuery != null) {
                            while (cursorQuery.moveToNext()) {
                                try {
                                    ContentValues contentValues5 = new ContentValues();
                                    String string2 = cursorQuery.getString(0);
                                    if (!TextUtils.isEmpty(string2)) {
                                        contentValues5.put("url", string2);
                                        contentValues5.put("title", cursorQuery.getString(1));
                                        contentValues5.put("visits", Integer.valueOf(cursorQuery.getInt(2)));
                                        contentValues5.put("date", Long.valueOf(cursorQuery.getLong(3)));
                                        contentValues5.put("created", Long.valueOf(cursorQuery.getLong(4)));
                                        sQLiteDatabase.insert("history", "favicon", contentValues5);
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    writableDatabase.close();
                                    databaseHelper.close();
                                    throw th;
                                }
                            }
                            cursorQuery.close();
                        }
                        writableDatabase.delete(str2, null, null);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        writableDatabase.close();
                        databaseHelper.close();
                        if (!databasePath.delete()) {
                            databasePath.deleteOnExit();
                        }
                        return true;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
            sQLiteDatabase.execSQL("CREATE TABLE bookmarks(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT,folder INTEGER NOT NULL DEFAULT 0,parent INTEGER,position INTEGER NOT NULL,insert_after INTEGER,deleted INTEGER NOT NULL DEFAULT 0,account_name TEXT,account_type TEXT,sourceid TEXT,version INTEGER NOT NULL DEFAULT 1,created INTEGER,modified INTEGER,dirty INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT);");
            sQLiteDatabase.execSQL("CREATE TABLE history(_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,url TEXT NOT NULL,created INTEGER,date INTEGER,visits INTEGER NOT NULL DEFAULT 0,user_entered INTEGER);");
            sQLiteDatabase.execSQL("CREATE TABLE images (url_key TEXT UNIQUE NOT NULL,favicon BLOB,thumbnail BLOB,touch_icon BLOB);");
            sQLiteDatabase.execSQL("CREATE INDEX imagesUrlIndex ON images(url_key)");
            sQLiteDatabase.execSQL("CREATE TABLE searches (_id INTEGER PRIMARY KEY AUTOINCREMENT,search TEXT,date LONG);");
            sQLiteDatabase.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY,value TEXT NOT NULL);");
            createAccountsView(sQLiteDatabase);
            createThumbnails(sQLiteDatabase);
            this.this$0.mSyncHelper.createDatabase(sQLiteDatabase);
            if (!importFromBrowserProvider(sQLiteDatabase)) {
                createDefaultBookmarks(sQLiteDatabase);
            }
            enableSync(sQLiteDatabase);
            createOmniboxSuggestions(sQLiteDatabase);
        }

        @Override
        public void onOpen(SQLiteDatabase sQLiteDatabase) {
            this.this$0.mSyncHelper.onDatabaseOpened(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
            if (i < 32) {
                createOmniboxSuggestions(sQLiteDatabase);
            }
            if (i < 31) {
                createThumbnails(sQLiteDatabase);
            }
            if (i < 30) {
                sQLiteDatabase.execSQL("DROP VIEW IF EXISTS v_snapshots_combined");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS snapshots");
            }
            if (i < 28) {
                enableSync(sQLiteDatabase);
            }
            if (i < 27) {
                createAccountsView(sQLiteDatabase);
            }
            if (i < 26) {
                sQLiteDatabase.execSQL("DROP VIEW IF EXISTS combined");
            }
            if (i < 25) {
                Log.v("browser/BrowserProvider", "onUpgrade < 25");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmarks");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS history");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS searches");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS images");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS settings");
                this.this$0.mSyncHelper.onAccountsChanged(sQLiteDatabase, new Account[0]);
                onCreate(sQLiteDatabase);
            }
        }
    }

    public interface OmniboxSuggestions {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "omnibox_suggestions");
    }

    static class SuggestionsCursor extends AbstractCursor {
        private static final String[] COLUMNS = {"_id", "suggest_intent_action", "suggest_intent_data", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_last_access_hint"};
        private final Cursor mSource;

        public SuggestionsCursor(Cursor cursor) {
            this.mSource = cursor;
        }

        @Override
        public void close() {
            this.mSource.close();
            super.close();
        }

        @Override
        public void deactivate() {
            this.mSource.deactivate();
            super.deactivate();
        }

        @Override
        public String[] getColumnNames() {
            return COLUMNS;
        }

        @Override
        public int getCount() {
            return this.mSource.getCount();
        }

        @Override
        public double getDouble(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloat(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInt(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLong(int i) {
            if (i == 0) {
                return this.mSource.getLong(0);
            }
            if (i == 7) {
                return this.mSource.getLong(4);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public short getShort(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(int i) {
            switch (i) {
                case 0:
                    return this.mSource.getString(i);
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
                    return this.mSource.getInt(3) == 1 ? Integer.toString(2130837537) : Integer.toString(2130837560);
                case 7:
                    return Long.toString(System.currentTimeMillis());
                default:
                    return null;
            }
        }

        @Override
        public boolean isClosed() {
            return this.mSource.isClosed();
        }

        @Override
        public boolean isNull(int i) {
            return this.mSource.isNull(i);
        }

        @Override
        public boolean onMove(int i, int i2) {
            return this.mSource.moveToPosition(i2);
        }
    }

    public interface Thumbnails {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BrowserContract.AUTHORITY_URI, "thumbnails");
    }

    static {
        UriMatcher uriMatcher = URI_MATCHER;
        uriMatcher.addURI("com.android.browser.provider", "accounts", 7000);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks", 1000);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks/#", 1001);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks/folder", 1002);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks/folder/#", 1003);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks/folder/id", 1005);
        uriMatcher.addURI("com.android.browser.provider", "search_suggest_query", 1004);
        uriMatcher.addURI("com.android.browser.provider", "bookmarks/search_suggest_query", 1004);
        uriMatcher.addURI("com.android.browser.provider", "history", 2000);
        uriMatcher.addURI("com.android.browser.provider", "history/#", 2001);
        uriMatcher.addURI("com.android.browser.provider", "searches", 3000);
        uriMatcher.addURI("com.android.browser.provider", "searches/#", 3001);
        uriMatcher.addURI("com.android.browser.provider", "syncstate", 4000);
        uriMatcher.addURI("com.android.browser.provider", "syncstate/#", 4001);
        uriMatcher.addURI("com.android.browser.provider", "images", 5000);
        uriMatcher.addURI("com.android.browser.provider", "combined", 6000);
        uriMatcher.addURI("com.android.browser.provider", "combined/#", 6001);
        uriMatcher.addURI("com.android.browser.provider", "settings", 8000);
        uriMatcher.addURI("com.android.browser.provider", "thumbnails", 10);
        uriMatcher.addURI("com.android.browser.provider", "thumbnails/#", 11);
        uriMatcher.addURI("com.android.browser.provider", "omnibox_suggestions", 20);
        URI_MATCHER.addURI("com.android.browser.provider", "homepage", 30);
        uriMatcher.addURI("MtkBrowserProvider", "searches", 3000);
        uriMatcher.addURI("MtkBrowserProvider", "searches/#", 3001);
        uriMatcher.addURI("MtkBrowserProvider", "bookmarks", 9000);
        uriMatcher.addURI("MtkBrowserProvider", "bookmarks/#", 9001);
        uriMatcher.addURI("MtkBrowserProvider", "search_suggest_query", 1004);
        uriMatcher.addURI("MtkBrowserProvider", "bookmarks/search_suggest_query", 1004);
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

    static final String bookmarkOrHistoryColumn(String str) {
        return "CASE WHEN bookmarks." + str + " IS NOT NULL THEN bookmarks." + str + " ELSE history." + str + " END AS " + str;
    }

    static final String bookmarkOrHistoryLiteral(String str, String str2, String str3) {
        return "CASE WHEN bookmarks." + str + " IS NOT NULL THEN \"" + str2 + "\" ELSE \"" + str3 + "\" END";
    }

    private String[] createCombinedQuery(Uri uri, String[] strArr, SQLiteQueryBuilder sQLiteQueryBuilder) {
        String[] strArr2;
        StringBuilder sb = new StringBuilder(128);
        sb.append("deleted");
        sb.append(" = 0");
        Object[] selectionWithAccounts = getSelectionWithAccounts(uri, null, null);
        String str = (String) selectionWithAccounts[0];
        String[] strArr3 = (String[]) selectionWithAccounts[1];
        if (str != null) {
            sb.append(" AND " + str);
            if (strArr3 != null) {
                String[] strArr4 = new String[strArr3.length * 2];
                System.arraycopy(strArr3, 0, strArr4, 0, strArr3.length);
                System.arraycopy(strArr3, 0, strArr4, strArr3.length, strArr3.length);
                strArr2 = strArr4;
            } else {
                strArr2 = null;
            }
        }
        String string = sb.toString();
        sQLiteQueryBuilder.setTables("bookmarks");
        sQLiteQueryBuilder.setTables(String.format("history LEFT OUTER JOIN (%s) bookmarks ON history.url = bookmarks.url LEFT OUTER JOIN images ON history.url = images.url_key", sQLiteQueryBuilder.buildQuery(null, string, null, null, null, null)));
        sQLiteQueryBuilder.setProjectionMap(COMBINED_HISTORY_PROJECTION_MAP);
        String strBuildQuery = sQLiteQueryBuilder.buildQuery(null, null, null, null, null, null);
        sQLiteQueryBuilder.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
        sQLiteQueryBuilder.setProjectionMap(COMBINED_BOOKMARK_PROJECTION_MAP);
        sQLiteQueryBuilder.setTables("(" + sQLiteQueryBuilder.buildUnionQuery(new String[]{strBuildQuery, sQLiteQueryBuilder.buildQuery(null, string + String.format(" AND %s NOT IN (SELECT %s FROM %s)", "url", "url", "history"), null, null, null, null)}, null, null) + ")");
        sQLiteQueryBuilder.setProjectionMap(null);
        return strArr2;
    }

    private Cursor doSuggestQuery(String str, String[] strArr, String str2) {
        String str3;
        String[] strArr2;
        Log.i("browser/BrowserProvider", "doSuggestQuery");
        if (TextUtils.isEmpty(strArr[0])) {
            str3 = "history.date != 0";
            strArr2 = null;
        } else {
            String str4 = strArr[0] + "%";
            if (!strArr[0].startsWith("http") && !strArr[0].startsWith("file")) {
                String[] strArr3 = {"http://" + str4, "http://www." + str4, "https://" + str4, "https://www." + str4, "%" + str4};
                StringBuilder sb = new StringBuilder();
                sb.append("doSuggestQuery, selectionArgs: ");
                sb.append(strArr3);
                Log.i("browser/BrowserProvider", sb.toString());
                SuggestionsCursor suggestionsCursor = new SuggestionsCursor(this.mOpenHelper.getReadableDatabase().query("v_omnibox_suggestions", new String[]{"_id", "url", "title", "bookmark"}, "url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?", strArr3, null, null, null, null));
                StringBuilder sb2 = new StringBuilder();
                sb2.append("doSuggestQuery, getCount: ");
                sb2.append(suggestionsCursor.getCount());
                Log.i("browser/BrowserProvider", sb2.toString());
                return suggestionsCursor;
            }
            strArr[0] = str4;
            str3 = "history." + str;
            strArr2 = strArr;
        }
        return new SuggestionsCursor(this.mOpenHelper.getReadableDatabase().query("history LEFT OUTER JOIN bookmarks ON (history.url = bookmarks.url AND bookmarks.deleted=0 AND bookmarks.folder=0)", SUGGEST_PROJECTION, str3, strArr2, null, null, null, null));
    }

    private String filterSearchClient(String str) {
        int iIndexOf = str.indexOf("client=");
        if (iIndexOf <= 0 || !str.contains(".google.")) {
            return str;
        }
        int iIndexOf2 = str.indexOf(38, iIndexOf);
        return iIndexOf2 > 0 ? str.substring(0, iIndexOf).concat(str.substring(iIndexOf2 + 1)) : str.substring(0, iIndexOf - 1);
    }

    private void filterSearchClient(String[] strArr) {
        if (strArr != null) {
            for (int i = 0; i < strArr.length; i++) {
                strArr[i] = filterSearchClient(strArr[i]);
            }
        }
    }

    private String[] getAccountNameAndType(long j) {
        Cursor cursorQuery;
        if (j <= 0 || (cursorQuery = query(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, j), new String[]{"account_name", "account_type"}, null, null, null)) == null) {
            return null;
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                return null;
            }
            String string = cursorQuery.getString(0);
            String string2 = cursorQuery.getString(1);
            cursorQuery.close();
            return new String[]{string, string2};
        } finally {
            cursorQuery.close();
        }
    }

    private static String getClientId(Context context) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor;
        String string;
        try {
            cursorQuery = context.getContentResolver().query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='client_id'", null, null);
            if (cursorQuery != null) {
                try {
                    string = cursorQuery.moveToNext() ? cursorQuery.getString(0) : "android-google";
                } catch (RuntimeException e) {
                    cursor = cursorQuery;
                    if (cursor == null) {
                        return "android-google";
                    }
                    cursor.close();
                    return "android-google";
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery == null) {
                return string;
            }
            cursorQuery.close();
            return string;
        } catch (RuntimeException e2) {
            cursor = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private String getHomePage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("homepage", BrowserSettings.getFactoryResetUrlFromRes(context));
    }

    private long insertSearchesInTransaction(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) throws Throwable {
        Cursor cursor;
        long jInsertOrThrow;
        String asString = contentValues.getAsString("search");
        if (TextUtils.isEmpty(asString)) {
            throw new IllegalArgumentException("Must include the SEARCH field");
        }
        try {
            Cursor cursorQuery = sQLiteDatabase.query("searches", new String[]{"_id"}, "search=?", new String[]{asString}, null, null, null);
            try {
                if (cursorQuery.moveToNext()) {
                    jInsertOrThrow = cursorQuery.getLong(0);
                    sQLiteDatabase.update("searches", contentValues, "_id=?", new String[]{Long.toString(jInsertOrThrow)});
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } else {
                    jInsertOrThrow = sQLiteDatabase.insertOrThrow("searches", "search", contentValues);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
                return jInsertOrThrow;
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
            cursor = null;
        }
    }

    public long insertSettingsInTransaction(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) throws Throwable {
        Cursor cursor;
        Cursor cursorQuery;
        long jInsertOrThrow;
        String asString = contentValues.getAsString("key");
        if (TextUtils.isEmpty(asString)) {
            throw new IllegalArgumentException("Must include the KEY field");
        }
        String[] strArr = {asString};
        try {
            cursorQuery = sQLiteDatabase.query("settings", new String[]{"key"}, "key=?", strArr, null, null, null);
        } catch (Throwable th) {
            th = th;
            cursor = null;
        }
        try {
            if (cursorQuery.moveToNext()) {
                jInsertOrThrow = cursorQuery.getLong(0);
                sQLiteDatabase.update("settings", contentValues, "key=?", strArr);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } else {
                jInsertOrThrow = sQLiteDatabase.insertOrThrow("settings", "value", contentValues);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
            return jInsertOrThrow;
        } catch (Throwable th2) {
            th = th2;
            cursor = cursorQuery;
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private boolean isValidAccountName(long j, String str) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        Log.e("browser/BrowserProvider", "BrowserProvider2.isValidAccountName parentId:" + j + " title:" + str);
        if (j <= 0 || str == null || str.length() == 0) {
            return true;
        }
        try {
            cursorQuery = query(BrowserContract.Bookmarks.CONTENT_URI, new String[]{"title"}, "parent = ? AND deleted = ? AND folder = ?", new String[]{j + "", "0", "1"}, null);
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
        if (cursorQuery != null) {
            try {
            } catch (IllegalStateException e2) {
                e = e2;
                try {
                    Log.e("browser/BrowserProvider", e.getMessage());
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
            if (cursorQuery.getCount() != 0) {
                while (cursorQuery.moveToNext()) {
                    if (str.equals(cursorQuery.getString(0))) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return false;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return true;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return true;
    }

    private boolean isValidParent(String str, String str2, long j) {
        String[] accountNameAndType = getAccountNameAndType(j);
        return accountNameAndType != null && TextUtils.equals(str2, accountNameAndType[0]) && TextUtils.equals(str, accountNameAndType[1]);
    }

    static final String qualifyColumn(String str, String str2) {
        return str + "." + str2 + " AS " + str2;
    }

    public static CharSequence replaceSystemPropertyInString(Context context, CharSequence charSequence) throws Throwable {
        int i;
        int i2 = 0;
        StringBuffer stringBuffer = new StringBuffer();
        String clientId = getClientId(context);
        int i3 = 0;
        while (true) {
            int i4 = i2;
            if (i4 >= charSequence.length()) {
                break;
            }
            if (charSequence.charAt(i4) == '{') {
                stringBuffer.append(charSequence.subSequence(i3, i4));
                i = i4;
                while (true) {
                    if (i >= charSequence.length()) {
                        i3 = i4;
                        i = i4;
                        break;
                    }
                    if (charSequence.charAt(i) == '}') {
                        if (charSequence.subSequence(i4 + 1, i).toString().equals("CLIENT_ID")) {
                            stringBuffer.append(clientId);
                        } else {
                            stringBuffer.append("unknown");
                        }
                        i3 = i + 1;
                    } else {
                        i++;
                    }
                }
            } else {
                i = i4;
            }
            i2 = i + 1;
        }
        if (charSequence.length() - i3 > 0) {
            stringBuffer.append(charSequence.subSequence(i3, charSequence.length()));
        }
        return stringBuffer;
    }

    private boolean setHomePage(Context context, String str) {
        if (str == null || str.length() <= 0) {
            return false;
        }
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editorEdit.putString("homepage", str);
        editorEdit.commit();
        return true;
    }

    private boolean setParentValues(long j, ContentValues contentValues) {
        String[] accountNameAndType = getAccountNameAndType(j);
        if (accountNameAndType == null) {
            return false;
        }
        contentValues.put("account_name", accountNameAndType[0]);
        contentValues.put("account_type", accountNameAndType[1]);
        return true;
    }

    private boolean shouldUpdateImages(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues) {
        Cursor cursorQuery = sQLiteDatabase.query("images", new String[]{"favicon", "thumbnail", "touch_icon"}, "url_key=?", new String[]{str}, null, null, null);
        byte[] asByteArray = contentValues.getAsByteArray("favicon");
        byte[] asByteArray2 = contentValues.getAsByteArray("thumbnail");
        byte[] asByteArray3 = contentValues.getAsByteArray("touch_icon");
        try {
            if (cursorQuery.getCount() <= 0) {
                boolean z = (asByteArray == null && asByteArray2 == null && asByteArray3 == null) ? false : true;
                cursorQuery.close();
                return z;
            }
            while (cursorQuery.moveToNext()) {
                if (asByteArray != null && !Arrays.equals(asByteArray, cursorQuery.getBlob(0))) {
                    return true;
                }
                if (asByteArray2 != null && !Arrays.equals(asByteArray2, cursorQuery.getBlob(1))) {
                    return true;
                }
                if (asByteArray3 != null && !Arrays.equals(asByteArray3, cursorQuery.getBlob(2))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursorQuery.close();
        }
    }

    String appendAccountToSelection(Uri uri, String str) {
        String queryParameter = uri.getQueryParameter("account_name");
        String queryParameter2 = uri.getQueryParameter("account_type");
        if (TextUtils.isEmpty(queryParameter) ^ TextUtils.isEmpty(queryParameter2)) {
            throw new IllegalArgumentException("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE for " + uri);
        }
        if (!(!TextUtils.isEmpty(queryParameter))) {
            return str;
        }
        StringBuilder sb = new StringBuilder("account_name=" + DatabaseUtils.sqlEscapeString(queryParameter) + " AND account_type=" + DatabaseUtils.sqlEscapeString(queryParameter2));
        if (!TextUtils.isEmpty(str)) {
            sb.append(" AND (");
            sb.append(str);
            sb.append(')');
        }
        return sb.toString();
    }

    int deleteBookmarks(String str, String[] strArr, boolean z) {
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (z) {
            return writableDatabase.delete("bookmarks", str, strArr);
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("modified", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("deleted", (Integer) 1);
        return updateBookmarksInTransaction(contentValues, str, strArr, z);
    }

    @Override
    public int deleteInTransaction(Uri uri, String str, String[] strArr, boolean z) {
        String strConcatenateWhere;
        int iDelete;
        String str2;
        Cursor cursorQuery;
        int iMatch = URI_MATCHER.match(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        switch (iMatch) {
            case 11:
                str = DatabaseUtils.concatenateWhere(str, "_id = ?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 10:
                iDelete = writableDatabase.delete("thumbnails", str, strArr);
                if (iDelete > 0) {
                    postNotifyUri(uri);
                    if (shouldNotifyLegacy(uri)) {
                        postNotifyUri(LEGACY_AUTHORITY_URI);
                    }
                }
                return iDelete;
            case 1001:
                str = DatabaseUtils.concatenateWhere(str, "bookmarks._id=?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 1000:
                Object[] selectionWithAccounts = getSelectionWithAccounts(uri, str, strArr);
                iDelete = deleteBookmarks((String) selectionWithAccounts[0], (String[]) selectionWithAccounts[1], z);
                pruneImages();
                if (iDelete > 0) {
                    refreshWidgets();
                }
                if (iDelete > 0) {
                }
                return iDelete;
            case 2001:
                str = DatabaseUtils.concatenateWhere(str, "history._id=?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                filterSearchClient(strArr);
                iDelete = writableDatabase.delete("history", str, strArr);
                pruneImages();
                if (iDelete > 0) {
                }
                return iDelete;
            case 3001:
                str = DatabaseUtils.concatenateWhere(str, "searches._id=?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 3000:
                iDelete = writableDatabase.delete("searches", str, strArr);
                if (iDelete > 0) {
                }
                return iDelete;
            case 4000:
                iDelete = this.mSyncHelper.delete(writableDatabase, str, strArr);
                if (iDelete > 0) {
                }
                return iDelete;
            case 4001:
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(ContentUris.parseId(uri));
                sb.append(" ");
                if (str == null) {
                    str2 = "";
                } else {
                    str2 = " AND (" + str + ")";
                }
                sb.append(str2);
                iDelete = this.mSyncHelper.delete(writableDatabase, sb.toString(), strArr);
                if (iDelete > 0) {
                }
                return iDelete;
            case 9000:
                strConcatenateWhere = str;
                String[] strArr2 = {"_id", "bookmark", "url"};
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                String[] strArrCreateCombinedQuery = createCombinedQuery(uri, strArr2, sQLiteQueryBuilder);
                if (strArr != null) {
                    strArrCreateCombinedQuery = DatabaseUtils.appendSelectionArgs(strArrCreateCombinedQuery, strArr);
                }
                cursorQuery = sQLiteQueryBuilder.query(writableDatabase, strArr2, strConcatenateWhere, strArrCreateCombinedQuery, null, null, null);
                iDelete = 0;
                while (cursorQuery.moveToNext()) {
                    long j = cursorQuery.getLong(0);
                    boolean z2 = cursorQuery.getInt(1) != 0;
                    String string = cursorQuery.getString(2);
                    if (z2) {
                        iDelete += deleteBookmarks("_id=?", new String[]{Long.toString(j)}, z);
                        writableDatabase.delete("history", "url=?", new String[]{string});
                    } else {
                        iDelete += writableDatabase.delete("history", "_id=?", new String[]{Long.toString(j)});
                    }
                }
                cursorQuery.close();
                if (iDelete > 0) {
                }
                return iDelete;
            case 9001:
                strConcatenateWhere = DatabaseUtils.concatenateWhere(str, "_id = CAST(? AS INTEGER)");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
                String[] strArr22 = {"_id", "bookmark", "url"};
                SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                String[] strArrCreateCombinedQuery2 = createCombinedQuery(uri, strArr22, sQLiteQueryBuilder2);
                if (strArr != null) {
                }
                cursorQuery = sQLiteQueryBuilder2.query(writableDatabase, strArr22, strConcatenateWhere, strArrCreateCombinedQuery2, null, null, null);
                iDelete = 0;
                while (cursorQuery.moveToNext()) {
                }
                cursorQuery.close();
                if (iDelete > 0) {
                }
                return iDelete;
            default:
                throw new UnsupportedOperationException("Unknown delete URI " + uri);
        }
    }

    ContentValues extractImageValues(ContentValues contentValues, String str) {
        ContentValues contentValues2;
        if (contentValues.containsKey("favicon")) {
            contentValues2 = new ContentValues();
            contentValues2.put("favicon", contentValues.getAsByteArray("favicon"));
            contentValues.remove("favicon");
        } else {
            contentValues2 = null;
        }
        if (contentValues.containsKey("thumbnail")) {
            if (contentValues2 == null) {
                contentValues2 = new ContentValues();
            }
            contentValues2.put("thumbnail", contentValues.getAsByteArray("thumbnail"));
            contentValues.remove("thumbnail");
        }
        if (contentValues.containsKey("touch_icon")) {
            if (contentValues2 == null) {
                contentValues2 = new ContentValues();
            }
            contentValues2.put("touch_icon", contentValues.getAsByteArray("touch_icon"));
            contentValues.remove("touch_icon");
        }
        if (contentValues2 != null) {
            contentValues2.put("url_key", str);
        }
        return contentValues2;
    }

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        DatabaseHelper databaseHelper;
        synchronized (this) {
            if (this.mOpenHelper == null) {
                this.mOpenHelper = new DatabaseHelper(this, context);
            }
            databaseHelper = this.mOpenHelper;
        }
        return databaseHelper;
    }

    Object[] getSelectionWithAccounts(Uri uri, String str, String[] strArr) {
        boolean z;
        String queryParameter = uri.getQueryParameter("acct_type");
        String queryParameter2 = uri.getQueryParameter("acct_name");
        if (queryParameter == null || queryParameter2 == null) {
            z = false;
        } else if (isNullAccount(queryParameter) || isNullAccount(queryParameter2)) {
            str = DatabaseUtils.concatenateWhere(str, "account_name IS NULL AND account_type IS NULL");
            z = false;
        } else {
            str = DatabaseUtils.concatenateWhere(str, "account_type=? AND account_name=? ");
            strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{queryParameter, queryParameter2});
            z = true;
        }
        return new Object[]{str, strArr, Boolean.valueOf(z)};
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
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

    int getUrlCount(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        Cursor cursorQuery = sQLiteDatabase.query(str, new String[]{"COUNT(*)"}, "url = ?", new String[]{str2}, null, null, null);
        try {
            return cursorQuery.moveToFirst() ? cursorQuery.getInt(0) : 0;
        } finally {
            cursorQuery.close();
        }
    }

    @Override
    public Uri insertInTransaction(Uri uri, ContentValues contentValues, boolean z) throws Throwable {
        long jReplaceOrThrow;
        String asString;
        int iMatch = URI_MATCHER.match(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (iMatch == 9000) {
            Integer asInteger = contentValues.getAsInteger("bookmark");
            contentValues.remove("bookmark");
            if (asInteger == null || asInteger.intValue() == 0) {
                iMatch = 2000;
            } else {
                contentValues.remove("date");
                contentValues.remove("visits");
                contentValues.remove("user_entered");
                contentValues.put("folder", (Integer) 0);
                iMatch = 1000;
            }
        }
        if (iMatch == 10) {
            jReplaceOrThrow = writableDatabase.replaceOrThrow("thumbnails", null, contentValues);
        } else if (iMatch == 1000) {
            if (contentValues.containsKey("url") && (asString = contentValues.getAsString("url")) != null) {
                contentValues.put("url", asString.trim());
            }
            if (!z) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                contentValues.put("created", Long.valueOf(jCurrentTimeMillis));
                contentValues.put("modified", Long.valueOf(jCurrentTimeMillis));
                contentValues.put("dirty", (Integer) 1);
                boolean z2 = contentValues.containsKey("account_type") || contentValues.containsKey("account_name");
                String asString2 = contentValues.getAsString("account_type");
                String asString3 = contentValues.getAsString("account_name");
                boolean zContainsKey = contentValues.containsKey("parent");
                if (!((zContainsKey && z2) ? isValidParent(asString2, asString3, contentValues.getAsLong("parent").longValue()) : (!zContainsKey || z2) ? zContainsKey : setParentValues(contentValues.getAsLong("parent").longValue(), contentValues))) {
                    contentValues.put("parent", Long.valueOf(queryDefaultFolderId(asString3, asString2)));
                }
            }
            if (contentValues.containsKey("folder") && contentValues.getAsBoolean("folder").booleanValue() && contentValues.containsKey("parent") && contentValues.containsKey("title") && !isValidAccountName(contentValues.getAsLong("parent").longValue(), contentValues.getAsString("title"))) {
                return null;
            }
            if (!contentValues.containsKey("position")) {
                contentValues.put("position", Long.toString(Long.MIN_VALUE));
            }
            String asString4 = contentValues.getAsString("url");
            ContentValues contentValuesExtractImageValues = extractImageValues(contentValues, asString4);
            Boolean asBoolean = contentValues.getAsBoolean("folder");
            if ((asBoolean == null || !asBoolean.booleanValue()) && contentValuesExtractImageValues != null && !TextUtils.isEmpty(asString4) && writableDatabase.update("images", contentValuesExtractImageValues, "url_key=?", new String[]{asString4}) == 0) {
                writableDatabase.insertOrThrow("images", "favicon", contentValuesExtractImageValues);
            }
            jReplaceOrThrow = writableDatabase.insertOrThrow("bookmarks", "dirty", contentValues);
            refreshWidgets();
        } else if (iMatch == 2000) {
            if (!contentValues.containsKey("created")) {
                contentValues.put("created", Long.valueOf(System.currentTimeMillis()));
            }
            contentValues.put("url", filterSearchClient(contentValues.getAsString("url")));
            ContentValues contentValuesExtractImageValues2 = extractImageValues(contentValues, contentValues.getAsString("url"));
            if (contentValuesExtractImageValues2 != null) {
                writableDatabase.insertOrThrow("images", "favicon", contentValuesExtractImageValues2);
            }
            jReplaceOrThrow = writableDatabase.insertOrThrow("history", "visits", contentValues);
        } else if (iMatch == 3000) {
            jReplaceOrThrow = insertSearchesInTransaction(writableDatabase, contentValues);
        } else if (iMatch == 4000) {
            jReplaceOrThrow = this.mSyncHelper.insert(writableDatabase, contentValues);
        } else {
            if (iMatch != 8000) {
                throw new UnsupportedOperationException("Unknown insert URI " + uri);
            }
            insertSettingsInTransaction(writableDatabase, contentValues);
            jReplaceOrThrow = 0;
        }
        if (jReplaceOrThrow < 0) {
            return null;
        }
        postNotifyUri(uri);
        if (shouldNotifyLegacy(uri)) {
            postNotifyUri(LEGACY_AUTHORITY_URI);
        }
        return ContentUris.withAppendedId(uri, jReplaceOrThrow);
    }

    @Override
    public boolean isCallerSyncAdapter(Uri uri) {
        return uri.getBooleanQueryParameter("caller_is_syncadapter", false);
    }

    boolean isNullAccount(String str) {
        if (str == null) {
            return true;
        }
        String strTrim = str.trim();
        return strTrim.length() == 0 || strTrim.equals("null");
    }

    @Override
    protected void onEndTransaction(boolean z) {
        super.onEndTransaction(z);
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

    int pruneImages() {
        return this.mOpenHelper.getWritableDatabase().delete("images", "url_key NOT IN (SELECT url FROM bookmarks WHERE url IS NOT NULL AND deleted == 0) AND url_key NOT IN (SELECT url FROM history WHERE url IS NOT NULL)", null);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        String[] strArr3;
        String str4;
        String[] strArr4;
        String strBuildUnionQuery;
        String[] strArrAppendSelectionArgs;
        String str5;
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        String queryParameter = uri.getQueryParameter("limit");
        String queryParameter2 = uri.getQueryParameter("groupBy");
        switch (iMatch) {
            case 11:
                str = DatabaseUtils.concatenateWhere(str, "_id = ?");
                strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 10:
                sQLiteQueryBuilder.setTables("thumbnails");
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery;
            case 20:
                sQLiteQueryBuilder.setTables("v_omnibox_suggestions");
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery2 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery2.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery2;
            case 30:
                String homePage = getHomePage(getContext());
                if (homePage == null) {
                    return null;
                }
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{"homepage"}, 1);
                matrixCursor.addRow(new String[]{homePage});
                return matrixCursor;
            case 1000:
            case 1001:
            case 1003:
                String strConcatenateWhere = !uri.getBooleanQueryParameter("show_deleted", false) ? DatabaseUtils.concatenateWhere("deleted=0", str) : str;
                if (iMatch == 1001) {
                    strConcatenateWhere = DatabaseUtils.concatenateWhere(strConcatenateWhere, "bookmarks._id=?");
                    strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
                } else if (iMatch == 1003) {
                    strConcatenateWhere = DatabaseUtils.concatenateWhere(strConcatenateWhere, "bookmarks.parent=?");
                    strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
                }
                Object[] selectionWithAccounts = getSelectionWithAccounts(uri, strConcatenateWhere, strArr2);
                String str6 = (String) selectionWithAccounts[0];
                String[] strArr5 = (String[]) selectionWithAccounts[1];
                boolean zBooleanValue = ((Boolean) selectionWithAccounts[2]).booleanValue();
                if (TextUtils.isEmpty(str2)) {
                    str2 = zBooleanValue ? "position ASC, _id ASC" : "folder DESC, position ASC, _id ASC";
                }
                sQLiteQueryBuilder.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                sQLiteQueryBuilder.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
                str3 = str2;
                strArr3 = strArr5;
                str4 = str6;
                strArr4 = strArr;
                Cursor cursorQuery22 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery22.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery22;
            case 1002:
                String queryParameter3 = uri.getQueryParameter("acct_type");
                String queryParameter4 = uri.getQueryParameter("acct_name");
                boolean z = (isNullAccount(queryParameter3) || isNullAccount(queryParameter4)) ? false : true;
                sQLiteQueryBuilder.setTables("bookmarks LEFT OUTER JOIN images ON bookmarks.url = images.url_key");
                String str7 = TextUtils.isEmpty(str2) ? z ? "position ASC, _id ASC" : "folder DESC, position ASC, _id ASC" : str2;
                if (z) {
                    sQLiteQueryBuilder.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                    String strBuildQuery = sQLiteQueryBuilder.buildQuery(strArr, DatabaseUtils.concatenateWhere("account_type=? AND account_name=? AND parent = (SELECT _id FROM bookmarks WHERE sync3='bookmark_bar' AND account_type = ? AND account_name = ?) AND deleted=0", str), null, null, null, null);
                    String[] strArrAppendSelectionArgs2 = {queryParameter3, queryParameter4, queryParameter3, queryParameter4};
                    if (strArr2 != null) {
                        strArrAppendSelectionArgs2 = DatabaseUtils.appendSelectionArgs(strArrAppendSelectionArgs2, strArr2);
                    }
                    String strConcatenateWhere2 = DatabaseUtils.concatenateWhere("account_type=? AND account_name=? AND sync3=?", str);
                    sQLiteQueryBuilder.setProjectionMap(OTHER_BOOKMARKS_PROJECTION_MAP);
                    strBuildUnionQuery = sQLiteQueryBuilder.buildUnionQuery(new String[]{strBuildQuery, sQLiteQueryBuilder.buildQuery(strArr, strConcatenateWhere2, null, null, null, null)}, str7, queryParameter);
                    strArrAppendSelectionArgs = DatabaseUtils.appendSelectionArgs(strArrAppendSelectionArgs2, new String[]{queryParameter3, queryParameter4, "other_bookmarks"});
                    if (strArr2 != null) {
                        strArrAppendSelectionArgs = DatabaseUtils.appendSelectionArgs(strArrAppendSelectionArgs, strArr2);
                    }
                } else {
                    sQLiteQueryBuilder.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                    String strConcatenateWhere3 = DatabaseUtils.concatenateWhere("parent=? AND deleted=0", str);
                    strArrAppendSelectionArgs = new String[]{Long.toString(1L)};
                    if (strArr2 != null) {
                        strArrAppendSelectionArgs = DatabaseUtils.appendSelectionArgs(strArrAppendSelectionArgs, strArr2);
                    }
                    strBuildUnionQuery = sQLiteQueryBuilder.buildQuery(strArr, strConcatenateWhere3, null, null, str7, null);
                }
                Cursor cursorRawQuery = readableDatabase.rawQuery(strBuildUnionQuery, strArrAppendSelectionArgs);
                if (cursorRawQuery == null) {
                    return cursorRawQuery;
                }
                cursorRawQuery.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorRawQuery;
            case 1004:
                return doSuggestQuery(str, strArr2, queryParameter);
            case 1005:
                long jQueryDefaultFolderId = queryDefaultFolderId(uri.getQueryParameter("acct_name"), uri.getQueryParameter("acct_type"));
                MatrixCursor matrixCursor2 = new MatrixCursor(new String[]{"_id"});
                matrixCursor2.newRow().add(Long.valueOf(jQueryDefaultFolderId));
                return matrixCursor2;
            case 2001:
                str = DatabaseUtils.concatenateWhere(str, "history._id=?");
                strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                filterSearchClient(strArr2);
                if (str2 == null) {
                    str2 = "date DESC";
                }
                sQLiteQueryBuilder.setProjectionMap(HISTORY_PROJECTION_MAP);
                sQLiteQueryBuilder.setTables("history LEFT OUTER JOIN images ON history.url = images.url_key");
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery222;
            case 3001:
                str = DatabaseUtils.concatenateWhere(str, "searches._id=?");
                strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 3000:
                sQLiteQueryBuilder.setTables("searches");
                sQLiteQueryBuilder.setProjectionMap(SEARCHES_PROJECTION_MAP);
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery2222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery2222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery2222;
            case 4000:
                return this.mSyncHelper.query(readableDatabase, strArr, str, strArr2, str2);
            case 4001:
                String strAppendAccountToSelection = appendAccountToSelection(uri, str);
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(ContentUris.parseId(uri));
                sb.append(" ");
                if (strAppendAccountToSelection == null) {
                    str5 = "";
                } else {
                    str5 = " AND (" + strAppendAccountToSelection + ")";
                }
                sb.append(str5);
                return this.mSyncHelper.query(readableDatabase, strArr, sb.toString(), strArr2, str2);
            case 5000:
                sQLiteQueryBuilder.setTables("images");
                sQLiteQueryBuilder.setProjectionMap(IMAGES_PROJECTION_MAP);
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery22222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery22222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery22222;
            case 6001:
            case 9001:
                str = DatabaseUtils.concatenateWhere(str, "_id = CAST(? AS INTEGER)");
                strArr2 = DatabaseUtils.appendSelectionArgs(strArr2, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 6000:
            case 9000:
                if ((iMatch == 9000 || iMatch == 9001) && strArr == null) {
                    strArr = Browser.HISTORY_PROJECTION;
                }
                String[] strArrCreateCombinedQuery = createCombinedQuery(uri, strArr, sQLiteQueryBuilder);
                if (strArr2 != null) {
                    strArrCreateCombinedQuery = DatabaseUtils.appendSelectionArgs(strArrCreateCombinedQuery, strArr2);
                }
                strArr2 = strArrCreateCombinedQuery;
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery222222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery222222;
            case 7000:
                sQLiteQueryBuilder.setTables("v_accounts");
                sQLiteQueryBuilder.setProjectionMap(ACCOUNTS_PROJECTION_MAP);
                if ("false".equals(uri.getQueryParameter("allowEmptyAccounts"))) {
                    str = DatabaseUtils.concatenateWhere(str, "0 < ( SELECT count(*) FROM bookmarks WHERE deleted = 0 AND folder = 0   AND (     v_accounts.account_name = bookmarks.account_name     OR (v_accounts.account_name IS NULL AND bookmarks.account_name IS NULL)   )   AND (     v_accounts.account_type = bookmarks.account_type     OR (v_accounts.account_type IS NULL AND bookmarks.account_type IS NULL)   ) )");
                }
                if (str2 == null) {
                    str2 = "account_name IS NOT NULL DESC, account_name ASC";
                }
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery2222222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery2222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery2222222;
            case 8000:
                sQLiteQueryBuilder.setTables("settings");
                sQLiteQueryBuilder.setProjectionMap(SETTINGS_PROJECTION_MAP);
                str3 = str2;
                strArr3 = strArr2;
                str4 = str;
                strArr4 = strArr;
                Cursor cursorQuery22222222 = sQLiteQueryBuilder.query(readableDatabase, strArr4, str4, strArr3, queryParameter2, null, str3, queryParameter);
                cursorQuery22222222.setNotificationUri(getContext().getContentResolver(), BrowserContract.AUTHORITY_URI);
                return cursorQuery22222222;
            default:
                throw new UnsupportedOperationException("Unknown URL " + uri.toString());
        }
    }

    long queryDefaultFolderId(String str, String str2) {
        if (!isNullAccount(str) && !isNullAccount(str2)) {
            Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query("bookmarks", new String[]{"_id"}, "sync3 = ? AND account_type = ? AND account_name = ?", new String[]{"bookmark_bar", str2, str}, null, null, null);
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getLong(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return 1L;
    }

    void refreshWidgets() {
        this.mUpdateWidgets = true;
    }

    public void setWidgetObserver(ContentObserver contentObserver) {
        this.mWidgetObserver = contentObserver;
    }

    boolean shouldNotifyLegacy(Uri uri) {
        return uri.getPathSegments().contains("history") || uri.getPathSegments().contains("bookmarks") || uri.getPathSegments().contains("searches");
    }

    @Override
    protected boolean syncToNetwork(Uri uri) {
        return ("com.android.browser.provider".equals(uri.getAuthority()) && uri.getPathSegments().contains("bookmarks")) ? this.mSyncToNetwork : "MtkBrowserProvider".equals(uri.getAuthority());
    }

    int updateBookmarksInTransaction(ContentValues contentValues, String str, String[] strArr, boolean z) {
        String str2;
        String str3;
        String string;
        String string2;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("bookmarks", new String[]{"_id", "version", "url", "title", "folder", "account_name", "account_type"}, str, strArr, null, null, null);
        boolean zContainsKey = contentValues.containsKey("parent");
        if (zContainsKey) {
            Cursor cursorQuery2 = writableDatabase.query("bookmarks", new String[]{"account_name", "account_type"}, "_id = ?", new String[]{Long.toString(contentValues.getAsLong("parent").longValue())}, null, null, null);
            if (cursorQuery2.moveToFirst()) {
                string = cursorQuery2.getString(0);
                string2 = cursorQuery2.getString(1);
            } else {
                string = null;
                string2 = null;
            }
            cursorQuery2.close();
            str2 = string2;
            str3 = string;
        } else {
            if (!contentValues.containsKey("account_name")) {
                contentValues.containsKey("account_type");
            }
            str2 = null;
            str3 = null;
        }
        try {
            String[] strArr2 = new String[1];
            if (!z) {
                contentValues.put("modified", Long.valueOf(System.currentTimeMillis()));
                contentValues.put("dirty", (Integer) 1);
            }
            boolean zContainsKey2 = contentValues.containsKey("url");
            String asString = zContainsKey2 ? contentValues.getAsString("url") : null;
            ContentValues contentValuesExtractImageValues = extractImageValues(contentValues, asString);
            int iUpdate = 0;
            int i = 0;
            while (cursorQuery.moveToNext()) {
                long j = cursorQuery.getLong(i);
                strArr2[i] = Long.toString(j);
                String string3 = cursorQuery.getString(5);
                String string4 = cursorQuery.getString(6);
                if (!zContainsKey || (TextUtils.equals(string3, str3) && TextUtils.equals(string4, str2))) {
                    if (!z) {
                        contentValues.put("version", Long.valueOf(cursorQuery.getLong(1) + 1));
                    }
                    iUpdate += writableDatabase.update("bookmarks", contentValues, "_id=?", strArr2);
                } else {
                    ContentValues contentValuesValuesFromCursor = valuesFromCursor(cursorQuery);
                    contentValuesValuesFromCursor.putAll(contentValues);
                    contentValuesValuesFromCursor.remove("_id");
                    contentValuesValuesFromCursor.remove("version");
                    contentValuesValuesFromCursor.put("account_name", str3);
                    contentValuesValuesFromCursor.put("account_type", str2);
                    long id = ContentUris.parseId(insertInTransaction(BrowserContract.Bookmarks.CONTENT_URI, contentValuesValuesFromCursor, z));
                    if (cursorQuery.getInt(4) != 0) {
                        ContentValues contentValues2 = new ContentValues(1);
                        contentValues2.put("parent", Long.valueOf(id));
                        iUpdate += updateBookmarksInTransaction(contentValues2, "parent=?", new String[]{Long.toString(j)}, z);
                    }
                    deleteInTransaction(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, j), null, null, z);
                    iUpdate++;
                }
                if (contentValuesExtractImageValues != null) {
                    if (!zContainsKey2) {
                        asString = cursorQuery.getString(2);
                        contentValuesExtractImageValues.put("url_key", asString);
                    }
                    if (!TextUtils.isEmpty(asString)) {
                        strArr2[0] = asString;
                        if (writableDatabase.update("images", contentValuesExtractImageValues, "url_key=?", strArr2) == 0) {
                            writableDatabase.insert("images", "favicon", contentValuesExtractImageValues);
                        }
                    }
                }
                i = 0;
            }
            return iUpdate;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    int updateHistoryInTransaction(ContentValues contentValues, String str, String[] strArr) {
        String string;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        filterSearchClient(strArr);
        Cursor cursorQuery = query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "url"}, str, strArr, null);
        if (cursorQuery == null) {
            return 0;
        }
        try {
            String[] strArr2 = new String[1];
            boolean zContainsKey = contentValues.containsKey("url");
            if (zContainsKey) {
                string = filterSearchClient(contentValues.getAsString("url"));
                contentValues.put("url", string);
            } else {
                string = null;
            }
            ContentValues contentValuesExtractImageValues = extractImageValues(contentValues, string);
            int iUpdate = 0;
            while (cursorQuery.moveToNext()) {
                strArr2[0] = cursorQuery.getString(0);
                iUpdate += writableDatabase.update("history", contentValues, "_id=?", strArr2);
                if (contentValuesExtractImageValues != null) {
                    if (!zContainsKey) {
                        string = cursorQuery.getString(1);
                        contentValuesExtractImageValues.put("url_key", string);
                    }
                    strArr2[0] = string;
                    if (writableDatabase.update("images", contentValuesExtractImageValues, "url_key=?", strArr2) == 0) {
                        writableDatabase.insert("images", "favicon", contentValuesExtractImageValues);
                    }
                }
            }
            return iUpdate;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z) {
        String asString;
        String str2;
        boolean zContainsKey;
        int iUpdate = 0;
        int iMatch = URI_MATCHER.match(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (iMatch == 9000 || iMatch == 9001) {
            Integer asInteger = contentValues.getAsInteger("bookmark");
            contentValues.remove("bookmark");
            if (asInteger == null || asInteger.intValue() == 0) {
                iMatch = iMatch == 9000 ? 2000 : 2001;
            } else {
                iMatch = iMatch == 9000 ? 1000 : 1001;
                contentValues.remove("date");
                contentValues.remove("visits");
                contentValues.remove("user_entered");
            }
        }
        switch (iMatch) {
            case 10:
                iUpdate = writableDatabase.update("thumbnails", contentValues, str, strArr);
                pruneImages();
                if (iUpdate <= 0) {
                    return iUpdate;
                }
                postNotifyUri(uri);
                if (!shouldNotifyLegacy(uri)) {
                    return iUpdate;
                }
                postNotifyUri(LEGACY_AUTHORITY_URI);
                return iUpdate;
            case 30:
                return (contentValues == null || (asString = contentValues.getAsString("homepage")) == null || !setHomePage(getContext(), asString)) ? 0 : 1;
            case 1001:
                str = DatabaseUtils.concatenateWhere(str, "bookmarks._id=?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 1000:
                Object[] selectionWithAccounts = getSelectionWithAccounts(uri, str, strArr);
                iUpdate = updateBookmarksInTransaction(contentValues, (String) selectionWithAccounts[0], (String[]) selectionWithAccounts[1], z);
                if (iUpdate > 0) {
                    refreshWidgets();
                }
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            case 2001:
                str = DatabaseUtils.concatenateWhere(str, "history._id=?");
                strArr = DatabaseUtils.appendSelectionArgs(strArr, new String[]{Long.toString(ContentUris.parseId(uri))});
            case 2000:
                iUpdate = updateHistoryInTransaction(contentValues, str, strArr);
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            case 3000:
                iUpdate = writableDatabase.update("searches", contentValues, str, strArr);
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            case 4000:
                iUpdate = this.mSyncHelper.update(this.mDb, contentValues, appendAccountToSelection(uri, str), strArr);
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            case 4001:
                String strAppendAccountToSelection = appendAccountToSelection(uri, str);
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(ContentUris.parseId(uri));
                sb.append(" ");
                if (strAppendAccountToSelection == null) {
                    str2 = "";
                } else {
                    str2 = " AND (" + strAppendAccountToSelection + ")";
                }
                sb.append(str2);
                iUpdate = this.mSyncHelper.update(this.mDb, contentValues, sb.toString(), strArr);
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            case 5000:
                String asString2 = contentValues.getAsString("url_key");
                if (TextUtils.isEmpty(asString2)) {
                    throw new IllegalArgumentException("Images.URL is required");
                }
                if (!shouldUpdateImages(writableDatabase, asString2, contentValues)) {
                    return 0;
                }
                int iUpdate2 = writableDatabase.update("images", contentValues, "url_key=?", new String[]{asString2});
                if (iUpdate2 == 0) {
                    writableDatabase.insertOrThrow("images", "favicon", contentValues);
                    iUpdate2 = 1;
                }
                if (getUrlCount(writableDatabase, "bookmarks", asString2) > 0) {
                    postNotifyUri(BrowserContract.Bookmarks.CONTENT_URI);
                    zContainsKey = contentValues.containsKey("favicon");
                    refreshWidgets();
                } else {
                    zContainsKey = false;
                }
                if (getUrlCount(writableDatabase, "history", asString2) > 0) {
                    postNotifyUri(BrowserContract.History.CONTENT_URI);
                    zContainsKey = contentValues.containsKey("favicon");
                }
                if (pruneImages() > 0 || zContainsKey) {
                    postNotifyUri(LEGACY_AUTHORITY_URI);
                }
                this.mSyncToNetwork = false;
                return iUpdate2;
            case 7000:
                this.mSyncHelper.onAccountsChanged(this.mDb, AccountManager.get(getContext()).getAccounts());
                pruneImages();
                if (iUpdate <= 0) {
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown update URI " + uri);
        }
    }

    ContentValues valuesFromCursor(Cursor cursor) {
        int columnCount = cursor.getColumnCount();
        ContentValues contentValues = new ContentValues(columnCount);
        String[] columnNames = cursor.getColumnNames();
        for (int i = 0; i < columnCount; i++) {
            switch (cursor.getType(i)) {
                case 1:
                    contentValues.put(columnNames[i], Long.valueOf(cursor.getLong(i)));
                    break;
                case 2:
                    contentValues.put(columnNames[i], Float.valueOf(cursor.getFloat(i)));
                    break;
                case 3:
                    contentValues.put(columnNames[i], cursor.getString(i));
                    break;
                case 4:
                    contentValues.put(columnNames[i], cursor.getBlob(i));
                    break;
            }
        }
        return contentValues;
    }
}
