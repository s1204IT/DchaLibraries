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

/* loaded from: classes.dex */
public class BrowserProvider extends ContentProvider {
    private static final Pattern STRIP_URL_PATTERN;
    private String[] SUGGEST_ARGS;
    private BackupManager mBackupManager;
    private int mMaxSuggestionLongSize;
    private int mMaxSuggestionShortSize;
    private SQLiteOpenHelper mOpenHelper;
    private BrowserSettings mSettings;
    static final String[] TABLE_NAMES = {"bookmarks", "searches", "bookmark_folders"};
    private static final String[] SUGGEST_PROJECTION = {"_id", "url", "title", "bookmark", "user_entered"};
    private static final String[] COLUMNS = {"_id", "suggest_intent_action", "suggest_intent_data", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_icon_2", "suggest_intent_query", "suggest_intent_extra_data"};
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static {
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[0], 0);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[0] + "/#", 10);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[1], 1);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[1] + "/#", 11);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[2], 2);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[2] + "/#", 12);
        URI_MATCHER.addURI("MtkBrowserProvider", "search_suggest_query", 20);
        URI_MATCHER.addURI("MtkBrowserProvider", TABLE_NAMES[0] + "/search_suggest_query", 21);
        STRIP_URL_PATTERN = Pattern.compile("^(http://)(.*?)(/$)?");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [217=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:45:0x001d */
    /* JADX WARN: Removed duplicated region for block: B:13:0x002e A[Catch: all -> 0x002a, RuntimeException -> 0x002c, TRY_LEAVE, TryCatch #4 {RuntimeException -> 0x002c, all -> 0x002a, blocks: (B:6:0x001d, B:8:0x0023, B:13:0x002e), top: B:45:0x001d }] */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0070 A[PHI: r0 r2
  0x0070: PHI (r0v3 java.lang.String) = (r0v1 java.lang.String), (r0v6 java.lang.String) binds: [B:38:0x0088, B:25:0x006e] A[DONT_GENERATE, DONT_INLINE]
  0x0070: PHI (r2v4 android.database.Cursor) = (r2v3 android.database.Cursor), (r2v7 android.database.Cursor) binds: [B:38:0x0088, B:25:0x006e] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0078  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x007d  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x0085  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String getClientId(ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        Cursor cursorQuery2;
        String string = "android-google";
        Cursor cursor = null;
        str = null;
        str = null;
        str = null;
        String str = null;
        Cursor cursor2 = null;
        try {
            cursorQuery = contentResolver.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='search_client_id'", null, null);
        } catch (RuntimeException e) {
            cursorQuery = null;
        } catch (Throwable th) {
            th = th;
            cursorQuery = null;
        }
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToNext()) {
                    string = cursorQuery.getString(0);
                    cursorQuery2 = null;
                } else {
                    cursorQuery2 = contentResolver.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='client_id'", null, null);
                    if (cursorQuery2 != null) {
                        try {
                            boolean zMoveToNext = cursorQuery2.moveToNext();
                            str = zMoveToNext;
                            if (zMoveToNext) {
                                String str2 = "ms-" + cursorQuery2.getString(0);
                                string = str2;
                                str = str2;
                            }
                        } catch (RuntimeException e2) {
                            cursor2 = cursorQuery2;
                            if (cursor2 != null) {
                                cursor2.close();
                            }
                            if (cursorQuery != null) {
                            }
                            return string;
                        } catch (Throwable th2) {
                            cursor = cursorQuery2;
                            th = th2;
                            if (cursor != null) {
                                cursor.close();
                            }
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    }
                }
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            } catch (RuntimeException e3) {
                cursor2 = str;
                if (cursor2 != null) {
                }
                if (cursorQuery != null) {
                }
                return string;
            } catch (Throwable th3) {
                th = th3;
                cursor = str;
                if (cursor != null) {
                }
                if (cursorQuery != null) {
                }
                throw th;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
        return string;
    }

    private static CharSequence replaceSystemPropertyInString(Context context, CharSequence charSequence) throws Throwable {
        StringBuffer stringBuffer = new StringBuffer();
        String clientId = getClientId(context.getContentResolver());
        int i = 0;
        int i2 = 0;
        while (i < charSequence.length()) {
            if (charSequence.charAt(i) == '{') {
                stringBuffer.append(charSequence.subSequence(i2, i));
                int i3 = i;
                while (true) {
                    if (i3 >= charSequence.length()) {
                        i2 = i;
                        break;
                    }
                    if (charSequence.charAt(i3) != '}') {
                        i3++;
                    } else {
                        if (charSequence.subSequence(i + 1, i3).toString().equals("CLIENT_ID")) {
                            stringBuffer.append(clientId);
                        } else {
                            stringBuffer.append("unknown");
                        }
                        int i4 = i3;
                        i2 = i3 + 1;
                        i = i4;
                    }
                }
            }
            i++;
        }
        if (charSequence.length() - i2 > 0) {
            stringBuffer.append(charSequence.subSequence(i2, charSequence.length()));
        }
        return stringBuffer;
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper(Context context) {
            super(context, "browser.db", (SQLiteDatabase.CursorFactory) null, 24);
            this.mContext = context;
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
            sQLiteDatabase.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER);");
            CharSequence[] textArray = this.mContext.getResources().getTextArray(R.array.bookmarks);
            int length = textArray.length;
            for (int i = 0; i < length; i += 2) {
                try {
                    sQLiteDatabase.execSQL("INSERT INTO bookmarks (title, url, visits, date, created, bookmark) VALUES('" + ((Object) textArray[i]) + "', '" + ((Object) BrowserProvider.replaceSystemPropertyInString(this.mContext, textArray[i + 1])) + "', 0, 0, 0, 1);");
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            }
            sQLiteDatabase.execSQL("CREATE TABLE searches (_id INTEGER PRIMARY KEY,search TEXT,date LONG);");
        }

        @Override // android.database.sqlite.SQLiteOpenHelper
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
            Log.w("BrowserProvider", "Upgrading database from version " + i + " to " + i2);
            if (i == 18) {
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS labels");
            }
            if (i <= 19) {
                sQLiteDatabase.execSQL("ALTER TABLE bookmarks ADD COLUMN thumbnail BLOB DEFAULT NULL;");
            }
            if (i < 21) {
                sQLiteDatabase.execSQL("ALTER TABLE bookmarks ADD COLUMN touch_icon BLOB DEFAULT NULL;");
            }
            if (i < 22) {
                sQLiteDatabase.execSQL("DELETE FROM bookmarks WHERE(bookmark = 0 AND url LIKE \"%.google.%client=ms-%\")");
                removeGears();
            }
            if (i < 23) {
                sQLiteDatabase.execSQL("ALTER TABLE bookmarks ADD COLUMN user_entered INTEGER;");
            }
            if (i < 24) {
                sQLiteDatabase.execSQL("DELETE FROM bookmarks WHERE url IS NULL;");
                sQLiteDatabase.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_temp;");
                sQLiteDatabase.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER,folder_id INTEGER DEFAULT 0);");
                sQLiteDatabase.execSQL("INSERT INTO bookmarks SELECT * FROM bookmarks_temp;");
                sQLiteDatabase.execSQL("DROP TABLE bookmarks_temp;");
                return;
            }
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmarks");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS searches");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmark_folders");
            onCreate(sQLiteDatabase);
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.browser.provider.BrowserProvider$DatabaseHelper$1] */
        private void removeGears() {
            new Thread() { // from class: com.android.browser.provider.BrowserProvider.DatabaseHelper.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() throws SecurityException, IllegalArgumentException {
                    Process.setThreadPriority(10);
                    String str = DatabaseHelper.this.mContext.getApplicationInfo().dataDir;
                    File file = new File(str + File.separator + "app_plugins");
                    if (!file.exists()) {
                        return;
                    }
                    File[] fileArrListFiles = file.listFiles(new FilenameFilter() { // from class: com.android.browser.provider.BrowserProvider.DatabaseHelper.1.1
                        @Override // java.io.FilenameFilter
                        public boolean accept(File file2, String str2) {
                            return str2.startsWith("gears");
                        }
                    });
                    for (int i = 0; i < fileArrListFiles.length; i++) {
                        if (fileArrListFiles[i].isDirectory()) {
                            deleteDirectory(fileArrListFiles[i]);
                        } else {
                            fileArrListFiles[i].delete();
                        }
                    }
                    File file2 = new File(str + File.separator + "gears");
                    if (!file2.exists()) {
                        return;
                    }
                    deleteDirectory(file2);
                }

                private void deleteDirectory(File file) {
                    File[] fileArrListFiles = file.listFiles();
                    for (int i = 0; i < fileArrListFiles.length; i++) {
                        if (fileArrListFiles[i].isDirectory()) {
                            deleteDirectory(fileArrListFiles[i]);
                        }
                        fileArrListFiles[i].delete();
                    }
                    file.delete();
                }
            }.start();
        }
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        Context context = getContext();
        boolean z = (context.getResources().getConfiguration().screenLayout & 15) == 4;
        boolean z2 = context.getResources().getConfiguration().orientation == 1;
        if (z && z2) {
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

        public MySuggestionCursor(Cursor cursor, Cursor cursor2, String str) {
            this.mHistoryCursor = cursor;
            this.mSuggestCursor = cursor2;
            this.mHistoryCount = cursor != null ? cursor.getCount() : 0;
            this.mSuggestionCount = cursor2 != null ? cursor2.getCount() : 0;
            if (this.mSuggestionCount > BrowserProvider.this.mMaxSuggestionLongSize - this.mHistoryCount) {
                this.mSuggestionCount = BrowserProvider.this.mMaxSuggestionLongSize - this.mHistoryCount;
            }
            this.mString = str;
            this.mIncludeWebSearch = str.length() > 0;
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

        @Override // android.database.AbstractCursor, android.database.CrossProcessCursor
        public boolean onMove(int i, int i2) {
            if (this.mHistoryCursor == null) {
                return false;
            }
            if (this.mIncludeWebSearch) {
                if (this.mHistoryCount == 0 && i2 == 0) {
                    return true;
                }
                if (this.mHistoryCount > 0) {
                    if (i2 == 0) {
                        this.mHistoryCursor.moveToPosition(0);
                        return true;
                    }
                    if (i2 == 1) {
                        return true;
                    }
                }
                i2--;
            }
            if (this.mHistoryCount > i2) {
                this.mHistoryCursor.moveToPosition(i2);
            } else {
                this.mSuggestCursor.moveToPosition(i2 - this.mHistoryCount);
            }
            return true;
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public int getCount() {
            if (this.mIncludeWebSearch) {
                return this.mHistoryCount + this.mSuggestionCount + 1;
            }
            return this.mHistoryCount + this.mSuggestionCount;
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public String[] getColumnNames() {
            return BrowserProvider.COLUMNS;
        }

        /* JADX WARN: Removed duplicated region for block: B:21:0x002b  */
        /* JADX WARN: Removed duplicated region for block: B:25:0x0035  */
        /* JADX WARN: Removed duplicated region for block: B:27:0x0038  */
        @Override // android.database.AbstractCursor, android.database.Cursor
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public String getString(int i) {
            char c;
            if (this.mPos != -1 && this.mHistoryCursor != null) {
                if (this.mIncludeWebSearch) {
                    c = 0;
                    if (this.mHistoryCount != 0 || this.mPos != 0) {
                        if (this.mHistoryCount > 0) {
                            if (this.mPos != 0) {
                                if (this.mPos != 1) {
                                }
                            } else {
                                c = 1;
                            }
                        } else {
                            c = 65535;
                        }
                    }
                    if (c == 65535) {
                        c = this.mPos - 1 < this.mHistoryCount ? (char) 1 : (char) 2;
                    }
                } else if (this.mPos < this.mHistoryCount) {
                }
                switch (i) {
                    case 1:
                        if (c == 1) {
                        }
                        break;
                    case 2:
                        if (c == 1) {
                            break;
                        }
                        break;
                    case 3:
                        if (c != 0) {
                            if (c != 1) {
                                if (this.mSuggestText1Id != -1) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                        break;
                    case 4:
                        if (c != 0) {
                            if (c != 1 && this.mSuggestText2Id != -1) {
                                break;
                            }
                        } else {
                            break;
                        }
                        break;
                    case 5:
                        if (c != 0) {
                            if (c != 1) {
                                if (this.mSuggestText2UrlId != -1) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        break;
                    case 6:
                        if (c != 1) {
                            break;
                        } else if (this.mHistoryCursor.getInt(3) != 1) {
                            break;
                        } else {
                            break;
                        }
                    case 8:
                        if (c != 0) {
                            if (c != 1) {
                                if (this.mSuggestQueryId != -1) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                        break;
                    case 9:
                        if (c != 0 && c != 1 && this.mSuggestIntentExtraDataId != -1) {
                            break;
                        }
                        break;
                }
                return null;
            }
            return null;
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public double getDouble(int i) {
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public float getFloat(int i) {
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public int getInt(int i) {
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public long getLong(int i) {
            if (this.mPos != -1 && i == 0) {
                return this.mPos;
            }
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public short getShort(int i) {
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public boolean isNull(int i) {
            throw new UnsupportedOperationException();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public void deactivate() {
            if (this.mHistoryCursor != null) {
                this.mHistoryCursor.deactivate();
            }
            if (this.mSuggestCursor != null) {
                this.mSuggestCursor.deactivate();
            }
            super.deactivate();
        }

        @Override // android.database.AbstractCursor, android.database.Cursor
        public boolean requery() {
            return (this.mHistoryCursor != null ? this.mHistoryCursor.requery() : false) | (this.mSuggestCursor != null ? this.mSuggestCursor.requery() : false);
        }

        @Override // android.database.AbstractCursor, android.database.Cursor, java.io.Closeable, java.lang.AutoCloseable
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
            String string = this.mHistoryCursor.getString(2);
            if (TextUtils.isEmpty(string) || TextUtils.getTrimmedLength(string) == 0) {
                return BrowserProvider.stripUrl(this.mHistoryCursor.getString(1));
            }
            return string;
        }

        private String getHistoryUrl() {
            String string = this.mHistoryCursor.getString(2);
            if (!TextUtils.isEmpty(string) && TextUtils.getTrimmedLength(string) != 0) {
                return BrowserProvider.stripUrl(this.mHistoryCursor.getString(1));
            }
            return null;
        }
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws IllegalStateException {
        String[] strArr3;
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == -1) {
            throw new IllegalArgumentException("Unknown URL");
        }
        if (iMatch == 20 || iMatch == 21) {
            return doSuggestQuery(str, strArr2, iMatch == 21);
        }
        String str3 = null;
        if (strArr == null || strArr.length <= 0) {
            strArr3 = null;
        } else {
            String[] strArr4 = new String[strArr.length + 1];
            System.arraycopy(strArr, 0, strArr4, 0, strArr.length);
            strArr4[strArr.length] = "_id AS _id";
            strArr3 = strArr4;
        }
        if (iMatch == 10 || iMatch == 11) {
            str3 = "_id = " + uri.getPathSegments().get(1);
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[iMatch % 10], strArr3, DatabaseUtils.concatenateWhere(str3, str), strArr2, null, null, str2, null);
        cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        return cursorQuery;
    }

    private Cursor doSuggestQuery(String str, String[] strArr, boolean z) {
        String str2;
        String[] strArr2;
        SearchEngine searchEngine;
        if (strArr[0] == null || strArr[0].equals("")) {
            return new MySuggestionCursor(null, null, "");
        }
        String str3 = strArr[0] + "%";
        if (!strArr[0].startsWith("http") && !strArr[0].startsWith("file")) {
            this.SUGGEST_ARGS[0] = "http://" + str3;
            this.SUGGEST_ARGS[1] = "http://www." + str3;
            this.SUGGEST_ARGS[2] = "https://" + str3;
            this.SUGGEST_ARGS[3] = "https://www." + str3;
            this.SUGGEST_ARGS[4] = str3;
            strArr2 = this.SUGGEST_ARGS;
            str2 = "(url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?) AND (bookmark = 1 OR user_entered = 1)";
        } else {
            str2 = str;
            strArr2 = new String[]{str3};
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[0], SUGGEST_PROJECTION, str2, strArr2, null, null, "visits DESC, date DESC", Integer.toString(this.mMaxSuggestionLongSize));
        if (z || Patterns.WEB_URL.matcher(strArr[0]).matches()) {
            return new MySuggestionCursor(cursorQuery, null, "");
        }
        if (strArr2 != null && strArr2.length > 1 && cursorQuery.getCount() < 2 && (searchEngine = this.mSettings.getSearchEngine()) != null && searchEngine.supportsSuggestions()) {
            return new MySuggestionCursor(cursorQuery, searchEngine.getSuggestions(getContext(), strArr[0]), strArr[0]);
        }
        return new MySuggestionCursor(cursorQuery, null, strArr[0]);
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
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

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri uriWithAppendedId;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        boolean z = false;
        switch (URI_MATCHER.match(uri)) {
            case 0:
                long jInsert = writableDatabase.insert(TABLE_NAMES[0], "url", contentValues);
                if (jInsert > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(Browser.BOOKMARKS_URI, jInsert);
                } else {
                    uriWithAppendedId = null;
                }
                z = true;
                break;
            case 1:
                long jInsert2 = writableDatabase.insert(TABLE_NAMES[1], "url", contentValues);
                if (jInsert2 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(Browser.SEARCHES_URI, jInsert2);
                    break;
                } else {
                    uriWithAppendedId = null;
                    break;
                }
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
        if (uriWithAppendedId == null) {
            throw new IllegalArgumentException("Unknown URL");
        }
        getContext().getContentResolver().notifyChange(uriWithAppendedId, null);
        if (z && contentValues.containsKey("bookmark") && contentValues.getAsInteger("bookmark").intValue() != 0) {
            this.mBackupManager.dataChanged();
        }
        return uriWithAppendedId;
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        String str2;
        String string;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == -1 || iMatch == 20) {
            throw new IllegalArgumentException("Unknown URL");
        }
        boolean z = iMatch == 10;
        if (z || iMatch == 11) {
            StringBuilder sb = new StringBuilder();
            if (str != null && str.length() > 0) {
                sb.append("( ");
                sb.append(str);
                sb.append(" ) AND ");
            }
            str2 = uri.getPathSegments().get(1);
            sb.append("_id = ");
            sb.append(str2);
            string = sb.toString();
        } else {
            string = str;
            str2 = null;
        }
        ContentResolver contentResolver = getContext().getContentResolver();
        if (z) {
            Cursor cursorQuery = contentResolver.query(Browser.BOOKMARKS_URI, new String[]{"bookmark"}, "_id = " + str2, null, null);
            if (cursorQuery.moveToNext() && cursorQuery.getInt(0) != 0) {
                this.mBackupManager.dataChanged();
            }
            cursorQuery.close();
        }
        int iDelete = writableDatabase.delete(TABLE_NAMES[iMatch % 10], string, strArr);
        contentResolver.notifyChange(uri, null);
        return iDelete;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String string = str;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == -1 || iMatch == 20) {
            throw new IllegalArgumentException("Unknown URL");
        }
        boolean z = true;
        if (iMatch == 10 || iMatch == 11) {
            StringBuilder sb = new StringBuilder();
            if (string != null && str.length() > 0) {
                sb.append("( ");
                sb.append(string);
                sb.append(" ) AND ");
            }
            String str2 = uri.getPathSegments().get(1);
            sb.append("_id = ");
            sb.append(str2);
            string = sb.toString();
        }
        ContentResolver contentResolver = getContext().getContentResolver();
        if (iMatch == 10 || iMatch == 0) {
            boolean z2 = false;
            if (!contentValues.containsKey("bookmark")) {
                if ((contentValues.containsKey("title") || contentValues.containsKey("url")) && contentValues.containsKey("_id")) {
                    Cursor cursorQuery = contentResolver.query(Browser.BOOKMARKS_URI, new String[]{"bookmark"}, "_id = " + contentValues.getAsString("_id"), null, null);
                    if (cursorQuery.moveToNext() && cursorQuery.getInt(0) != 0) {
                        z2 = true;
                    }
                    z = z2;
                    cursorQuery.close();
                } else {
                    z = false;
                }
            }
            if (z) {
                this.mBackupManager.dataChanged();
            }
        }
        int iUpdate = writableDatabase.update(TABLE_NAMES[iMatch % 10], contentValues, string, strArr);
        contentResolver.notifyChange(uri, null);
        return iUpdate;
    }

    private static String stripUrl(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = STRIP_URL_PATTERN.matcher(str);
        if (matcher.matches() && matcher.groupCount() == 3) {
            return matcher.group(2);
        }
        return str;
    }
}
