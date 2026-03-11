package com.android.browser.provider;

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Process;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import com.android.browser.BrowserSettings;
import com.android.browser.R;
import com.android.browser.search.SearchEngine;
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserProvider extends ContentProvider {
    private static final Pattern STRIP_URL_PATTERN;
    private String[] SUGGEST_ARGS;
    private BackupManager mBackupManager;
    private int mMaxSuggestionLongSize;
    private int mMaxSuggestionShortSize;
    private SQLiteOpenHelper mOpenHelper;
    private BrowserSettings mSettings;
    static final String[] TABLE_NAMES = {"bookmarks", "searches"};
    private static final String[] SUGGEST_PROJECTION = {"_id", "url", "title", "bookmark", "user_entered"};
    private static final String[] COLUMNS = {"_id", "suggest_intent_action", "suggest_intent_data", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_icon_2", "suggest_intent_query", "suggest_intent_extra_data"};
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static {
        URI_MATCHER.addURI("browser", TABLE_NAMES[0], 0);
        URI_MATCHER.addURI("browser", TABLE_NAMES[0] + "/#", 10);
        URI_MATCHER.addURI("browser", TABLE_NAMES[1], 1);
        URI_MATCHER.addURI("browser", TABLE_NAMES[1] + "/#", 11);
        URI_MATCHER.addURI("browser", "search_suggest_query", 20);
        URI_MATCHER.addURI("browser", TABLE_NAMES[0] + "/search_suggest_query", 21);
        STRIP_URL_PATTERN = Pattern.compile("^(http://)(.*?)(/$)?");
    }

    public static String getClientId(ContentResolver cr) {
        String ret = "android-google";
        Cursor legacyClientIdCursor = null;
        Cursor searchClientIdCursor = null;
        try {
            Cursor searchClientIdCursor2 = cr.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='search_client_id'", null, null);
            if (searchClientIdCursor2 != null && searchClientIdCursor2.moveToNext()) {
                ret = searchClientIdCursor2.getString(0);
            } else {
                legacyClientIdCursor = cr.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='client_id'", null, null);
                if (legacyClientIdCursor != null && legacyClientIdCursor.moveToNext()) {
                    ret = "ms-" + legacyClientIdCursor.getString(0);
                }
            }
            if (legacyClientIdCursor != null) {
                legacyClientIdCursor.close();
            }
            if (searchClientIdCursor2 != null) {
                searchClientIdCursor2.close();
            }
        } catch (RuntimeException e) {
            if (0 != 0) {
                legacyClientIdCursor.close();
            }
            if (0 != 0) {
                searchClientIdCursor.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                legacyClientIdCursor.close();
            }
            if (0 != 0) {
                searchClientIdCursor.close();
            }
            throw th;
        }
        return ret;
    }

    public static CharSequence replaceSystemPropertyInString(Context context, CharSequence srcString) {
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

    static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper(Context context) {
            super(context, "browser.db", (SQLiteDatabase.CursorFactory) null, 24);
            this.mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER);");
            CharSequence[] bookmarks = this.mContext.getResources().getTextArray(R.array.bookmarks);
            int size = bookmarks.length;
            for (int i = 0; i < size; i += 2) {
                try {
                    CharSequence bookmarkDestination = BrowserProvider.replaceSystemPropertyInString(this.mContext, bookmarks[i + 1]);
                    db.execSQL("INSERT INTO bookmarks (title, url, visits, date, created, bookmark) VALUES('" + ((Object) bookmarks[i]) + "', '" + ((Object) bookmarkDestination) + "', 0, 0, 0, 1);");
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            }
            db.execSQL("CREATE TABLE searches (_id INTEGER PRIMARY KEY,search TEXT,date LONG);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("BrowserProvider", "Upgrading database from version " + oldVersion + " to " + newVersion);
            if (oldVersion == 18) {
                db.execSQL("DROP TABLE IF EXISTS labels");
            }
            if (oldVersion <= 19) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN thumbnail BLOB DEFAULT NULL;");
            }
            if (oldVersion < 21) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN touch_icon BLOB DEFAULT NULL;");
            }
            if (oldVersion < 22) {
                db.execSQL("DELETE FROM bookmarks WHERE (bookmark = 0 AND url LIKE \"%.google.%client=ms-%\")");
                removeGears();
            }
            if (oldVersion < 23) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN user_entered INTEGER;");
            }
            if (oldVersion < 24) {
                db.execSQL("DELETE FROM bookmarks WHERE url IS NULL;");
                db.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_temp;");
                db.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER);");
                db.execSQL("INSERT INTO bookmarks SELECT * FROM bookmarks_temp;");
                db.execSQL("DROP TABLE bookmarks_temp;");
                return;
            }
            db.execSQL("DROP TABLE IF EXISTS bookmarks");
            db.execSQL("DROP TABLE IF EXISTS searches");
            onCreate(db);
        }

        private void removeGears() {
            new Thread() {
                @Override
                public void run() {
                    Process.setThreadPriority(10);
                    String browserDataDirString = DatabaseHelper.this.mContext.getApplicationInfo().dataDir;
                    File appPluginsDir = new File(browserDataDirString + File.separator + "app_plugins");
                    if (appPluginsDir.exists()) {
                        File[] gearsFiles = appPluginsDir.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                return filename.startsWith("gears");
                            }
                        });
                        for (int i = 0; i < gearsFiles.length; i++) {
                            if (gearsFiles[i].isDirectory()) {
                                deleteDirectory(gearsFiles[i]);
                            } else {
                                gearsFiles[i].delete();
                            }
                        }
                        File gearsDataDir = new File(browserDataDirString + File.separator + "gears");
                        if (gearsDataDir.exists()) {
                            deleteDirectory(gearsDataDir);
                        }
                    }
                }

                private void deleteDirectory(File currentDir) {
                    File[] files = currentDir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            deleteDirectory(files[i]);
                        }
                        files[i].delete();
                    }
                    currentDir.delete();
                }
            }.start();
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        boolean xlargeScreenSize = (context.getResources().getConfiguration().screenLayout & 15) == 4;
        boolean isPortrait = context.getResources().getConfiguration().orientation == 1;
        if (xlargeScreenSize && isPortrait) {
            this.mMaxSuggestionLongSize = 9;
            this.mMaxSuggestionShortSize = 6;
        } else {
            this.mMaxSuggestionLongSize = 6;
            this.mMaxSuggestionShortSize = 3;
        }
        this.mOpenHelper = new DatabaseHelper(context);
        this.mBackupManager = new BackupManager(context);
        this.mSettings = BrowserSettings.getInstance();
        return true;
    }

    private class MySuggestionCursor extends AbstractCursor {
        private int mHistoryCount;
        private Cursor mHistoryCursor;
        private boolean mIncludeWebSearch;
        private String mString;
        private Cursor mSuggestCursor;
        private int mSuggestIntentExtraDataId;
        private int mSuggestQueryId;
        private int mSuggestText1Id;
        private int mSuggestText2Id;
        private int mSuggestText2UrlId;
        private int mSuggestionCount;

        public MySuggestionCursor(Cursor hc, Cursor sc, String string) {
            this.mHistoryCursor = hc;
            this.mSuggestCursor = sc;
            this.mHistoryCount = hc != null ? hc.getCount() : 0;
            this.mSuggestionCount = sc != null ? sc.getCount() : 0;
            if (this.mSuggestionCount > BrowserProvider.this.mMaxSuggestionLongSize - this.mHistoryCount) {
                this.mSuggestionCount = BrowserProvider.this.mMaxSuggestionLongSize - this.mHistoryCount;
            }
            this.mString = string;
            this.mIncludeWebSearch = string.length() > 0;
            if (this.mSuggestCursor == null) {
                this.mSuggestText1Id = -1;
                this.mSuggestText2Id = -1;
                this.mSuggestText2UrlId = -1;
                this.mSuggestQueryId = -1;
                this.mSuggestIntentExtraDataId = -1;
                return;
            }
            this.mSuggestText1Id = this.mSuggestCursor.getColumnIndex("suggest_text_1");
            this.mSuggestText2Id = this.mSuggestCursor.getColumnIndex("suggest_text_2");
            this.mSuggestText2UrlId = this.mSuggestCursor.getColumnIndex("suggest_text_2_url");
            this.mSuggestQueryId = this.mSuggestCursor.getColumnIndex("suggest_intent_query");
            this.mSuggestIntentExtraDataId = this.mSuggestCursor.getColumnIndex("suggest_intent_extra_data");
        }

        @Override
        public boolean onMove(int oldPosition, int newPosition) {
            if (this.mHistoryCursor == null) {
                return false;
            }
            if (this.mIncludeWebSearch) {
                if (this.mHistoryCount == 0 && newPosition == 0) {
                    return true;
                }
                if (this.mHistoryCount > 0) {
                    if (newPosition == 0) {
                        this.mHistoryCursor.moveToPosition(0);
                        return true;
                    }
                    if (newPosition == 1) {
                        return true;
                    }
                }
                newPosition--;
            }
            if (this.mHistoryCount > newPosition) {
                this.mHistoryCursor.moveToPosition(newPosition);
            } else {
                this.mSuggestCursor.moveToPosition(newPosition - this.mHistoryCount);
            }
            return true;
        }

        @Override
        public int getCount() {
            return this.mIncludeWebSearch ? this.mHistoryCount + this.mSuggestionCount + 1 : this.mHistoryCount + this.mSuggestionCount;
        }

        @Override
        public String[] getColumnNames() {
            return BrowserProvider.COLUMNS;
        }

        @Override
        public String getString(int columnIndex) {
            if (this.mPos == -1 || this.mHistoryCursor == null) {
                return null;
            }
            int type = -1;
            if (this.mIncludeWebSearch) {
                if (this.mHistoryCount == 0 && this.mPos == 0) {
                    type = 0;
                } else if (this.mHistoryCount > 0) {
                    if (this.mPos == 0) {
                        type = 1;
                    } else if (this.mPos == 1) {
                        type = 0;
                    }
                }
                if (type == -1) {
                    type = this.mPos + (-1) < this.mHistoryCount ? 1 : 2;
                }
            } else {
                type = this.mPos < this.mHistoryCount ? 1 : 2;
            }
            switch (columnIndex) {
                case 1:
                    if (type == 1) {
                        return "android.intent.action.VIEW";
                    }
                    return "android.intent.action.SEARCH";
                case 2:
                    if (type == 1) {
                        return this.mHistoryCursor.getString(1);
                    }
                    return null;
                case 3:
                    if (type == 0) {
                        return this.mString;
                    }
                    if (type == 1) {
                        return getHistoryTitle();
                    }
                    if (this.mSuggestText1Id == -1) {
                        return null;
                    }
                    return this.mSuggestCursor.getString(this.mSuggestText1Id);
                case 4:
                    if (type == 0) {
                        return BrowserProvider.this.getContext().getString(R.string.search_the_web);
                    }
                    if (type != 1 && this.mSuggestText2Id != -1) {
                        return this.mSuggestCursor.getString(this.mSuggestText2Id);
                    }
                    return null;
                case 5:
                    if (type == 0) {
                        return null;
                    }
                    if (type == 1) {
                        return getHistoryUrl();
                    }
                    if (this.mSuggestText2UrlId == -1) {
                        return null;
                    }
                    return this.mSuggestCursor.getString(this.mSuggestText2UrlId);
                case 6:
                    if (type == 1) {
                        if (this.mHistoryCursor.getInt(3) == 1) {
                            return Integer.valueOf(R.drawable.ic_search_category_bookmark).toString();
                        }
                        return Integer.valueOf(R.drawable.ic_search_category_history).toString();
                    }
                    return Integer.valueOf(R.drawable.ic_search_category_suggest).toString();
                case 7:
                    return "0";
                case 8:
                    if (type == 0) {
                        return this.mString;
                    }
                    if (type == 1) {
                        return this.mHistoryCursor.getString(1);
                    }
                    if (this.mSuggestQueryId == -1) {
                        return null;
                    }
                    return this.mSuggestCursor.getString(this.mSuggestQueryId);
                case 9:
                    if (type != 0 && type != 1 && this.mSuggestIntentExtraDataId != -1) {
                        return this.mSuggestCursor.getString(this.mSuggestIntentExtraDataId);
                    }
                    return null;
                default:
                    return null;
            }
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
            if (this.mPos != -1 && column == 0) {
                return this.mPos;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public short getShort(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNull(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deactivate() {
            if (this.mHistoryCursor != null) {
                this.mHistoryCursor.deactivate();
            }
            if (this.mSuggestCursor != null) {
                this.mSuggestCursor.deactivate();
            }
            super.deactivate();
        }

        @Override
        public boolean requery() {
            return (this.mHistoryCursor != null ? this.mHistoryCursor.requery() : false) | (this.mSuggestCursor != null ? this.mSuggestCursor.requery() : false);
        }

        @Override
        public void close() {
            super.close();
            if (this.mHistoryCursor != null) {
                this.mHistoryCursor.close();
                this.mHistoryCursor = null;
            }
            if (this.mSuggestCursor != null) {
                this.mSuggestCursor.close();
                this.mSuggestCursor = null;
            }
        }

        private String getHistoryTitle() {
            String title = this.mHistoryCursor.getString(2);
            if (TextUtils.isEmpty(title) || TextUtils.getTrimmedLength(title) == 0) {
                return BrowserProvider.stripUrl(this.mHistoryCursor.getString(1));
            }
            return title;
        }

        private String getHistoryUrl() {
            String title = this.mHistoryCursor.getString(2);
            if (!TextUtils.isEmpty(title) && TextUtils.getTrimmedLength(title) != 0) {
                return BrowserProvider.stripUrl(this.mHistoryCursor.getString(1));
            }
            return null;
        }
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sortOrder) throws IllegalStateException {
        int match = URI_MATCHER.match(url);
        if (match == -1) {
            throw new IllegalArgumentException("Unknown URL");
        }
        if (match == 20 || match == 21) {
            return doSuggestQuery(selection, selectionArgs, match == 21);
        }
        String[] projection = null;
        if (projectionIn != null && projectionIn.length > 0) {
            projection = new String[projectionIn.length + 1];
            System.arraycopy(projectionIn, 0, projection, 0, projectionIn.length);
            projection[projectionIn.length] = "_id AS _id";
        }
        String whereClause = null;
        if (match == 10 || match == 11) {
            whereClause = "_id = " + url.getPathSegments().get(1);
        }
        Cursor c = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[match % 10], projection, DatabaseUtils.concatenateWhere(whereClause, selection), selectionArgs, null, null, sortOrder, null);
        c.setNotificationUri(getContext().getContentResolver(), url);
        return c;
    }

    private Cursor doSuggestQuery(String selection, String[] selectionArgs, boolean bookmarksOnly) {
        String[] myArgs;
        String suggestSelection;
        SearchEngine searchEngine;
        if (selectionArgs[0] == null || selectionArgs[0].equals("")) {
            return new MySuggestionCursor(null, null, "");
        }
        String like = selectionArgs[0] + "%";
        if (selectionArgs[0].startsWith("http") || selectionArgs[0].startsWith("file")) {
            myArgs = new String[]{like};
            suggestSelection = selection;
        } else {
            this.SUGGEST_ARGS[0] = "http://" + like;
            this.SUGGEST_ARGS[1] = "http://www." + like;
            this.SUGGEST_ARGS[2] = "https://" + like;
            this.SUGGEST_ARGS[3] = "https://www." + like;
            this.SUGGEST_ARGS[4] = like;
            myArgs = this.SUGGEST_ARGS;
            suggestSelection = "(url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?) AND (bookmark = 1 OR user_entered = 1)";
        }
        Cursor c = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[0], SUGGEST_PROJECTION, suggestSelection, myArgs, null, null, "visits DESC, date DESC", Integer.toString(this.mMaxSuggestionLongSize));
        if (bookmarksOnly || Patterns.WEB_URL.matcher(selectionArgs[0]).matches()) {
            return new MySuggestionCursor(c, null, "");
        }
        if (myArgs != null && myArgs.length > 1 && c.getCount() < 2 && (searchEngine = this.mSettings.getSearchEngine()) != null && searchEngine.supportsSuggestions()) {
            Cursor sc = searchEngine.getSuggestions(getContext(), selectionArgs[0]);
            return new MySuggestionCursor(c, sc, selectionArgs[0]);
        }
        return new MySuggestionCursor(c, null, selectionArgs[0]);
    }

    @Override
    public String getType(Uri url) {
        int match = URI_MATCHER.match(url);
        switch (match) {
            case 0:
                return "vnd.android.cursor.dir/bookmark";
            case 1:
                return "vnd.android.cursor.dir/searches";
            case 10:
                return "vnd.android.cursor.item/bookmark";
            case 11:
                return "vnd.android.cursor.item/searches";
            case 20:
                return "vnd.android.cursor.dir/vnd.android.search.suggest";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        boolean isBookmarkTable = false;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(url);
        Uri uri = null;
        switch (match) {
            case 0:
                long rowID = db.insert(TABLE_NAMES[0], "url", initialValues);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Browser.BOOKMARKS_URI, rowID);
                }
                isBookmarkTable = true;
                break;
            case 1:
                long rowID2 = db.insert(TABLE_NAMES[1], "url", initialValues);
                if (rowID2 > 0) {
                    uri = ContentUris.withAppendedId(Browser.SEARCHES_URI, rowID2);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Unknown URL");
        }
        getContext().getContentResolver().notifyChange(uri, null);
        if (isBookmarkTable && initialValues.containsKey("bookmark") && initialValues.getAsInteger("bookmark").intValue() != 0) {
            this.mBackupManager.dataChanged();
        }
        return uri;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(url);
        if (match == -1 || match == 20) {
            throw new IllegalArgumentException("Unknown URL");
        }
        boolean isBookmarkTable = match == 10;
        String id = null;
        if (isBookmarkTable || match == 11) {
            StringBuilder sb = new StringBuilder();
            if (where != null && where.length() > 0) {
                sb.append("( ");
                sb.append(where);
                sb.append(" ) AND ");
            }
            String id2 = url.getPathSegments().get(1);
            id = id2;
            sb.append("_id = ");
            sb.append(id);
            where = sb.toString();
        }
        ContentResolver cr = getContext().getContentResolver();
        if (isBookmarkTable) {
            Cursor cursor = cr.query(Browser.BOOKMARKS_URI, new String[]{"bookmark"}, "_id = " + id, null, null);
            if (cursor.moveToNext() && cursor.getInt(0) != 0) {
                this.mBackupManager.dataChanged();
            }
            cursor.close();
        }
        int count = db.delete(TABLE_NAMES[match % 10], where, whereArgs);
        cr.notifyChange(url, null);
        return count;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = URI_MATCHER.match(url);
        if (match == -1 || match == 20) {
            throw new IllegalArgumentException("Unknown URL");
        }
        if (match == 10 || match == 11) {
            StringBuilder sb = new StringBuilder();
            if (where != null && where.length() > 0) {
                sb.append("( ");
                sb.append(where);
                sb.append(" ) AND ");
            }
            String id = url.getPathSegments().get(1);
            sb.append("_id = ");
            sb.append(id);
            where = sb.toString();
        }
        ContentResolver cr = getContext().getContentResolver();
        if (match == 10 || match == 0) {
            boolean changingBookmarks = false;
            if (values.containsKey("bookmark")) {
                changingBookmarks = true;
            } else if ((values.containsKey("title") || values.containsKey("url")) && values.containsKey("_id")) {
                Cursor cursor = cr.query(Browser.BOOKMARKS_URI, new String[]{"bookmark"}, "_id = " + values.getAsString("_id"), null, null);
                if (cursor.moveToNext()) {
                    changingBookmarks = cursor.getInt(0) != 0;
                }
                cursor.close();
            }
            if (changingBookmarks) {
                this.mBackupManager.dataChanged();
            }
        }
        int ret = db.update(TABLE_NAMES[match % 10], values, where, whereArgs);
        cr.notifyChange(url, null);
        return ret;
    }

    public static String stripUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = STRIP_URL_PATTERN.matcher(url);
        if (m.matches() && m.groupCount() == 3) {
            return m.group(2);
        }
        return url;
    }
}
