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
    static final String[] TABLE_NAMES = {"bookmarks", "searches", "bookmark_folders"};
    private static final String[] SUGGEST_PROJECTION = {"_id", "url", "title", "bookmark", "user_entered"};
    private static final String[] COLUMNS = {"_id", "suggest_intent_action", "suggest_intent_data", "suggest_text_1", "suggest_text_2", "suggest_text_2_url", "suggest_icon_1", "suggest_icon_2", "suggest_intent_query", "suggest_intent_extra_data"};
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

    static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper(Context context) {
            super(context, "browser.db", (SQLiteDatabase.CursorFactory) null, 24);
            this.mContext = context;
        }

        private void removeGears() {
            new Thread(this) {
                final DatabaseHelper this$0;

                {
                    this.this$0 = this;
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

                @Override
                public void run() {
                    Process.setThreadPriority(10);
                    String str = this.this$0.mContext.getApplicationInfo().dataDir;
                    File file = new File(str + File.separator + "app_plugins");
                    if (file.exists()) {
                        File[] fileArrListFiles = file.listFiles(new FilenameFilter(this) {
                            final AnonymousClass1 this$1;

                            {
                                this.this$1 = this;
                            }

                            @Override
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
                        if (file2.exists()) {
                            deleteDirectory(file2);
                        }
                    }
                }
            }.start();
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
            sQLiteDatabase.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER);");
            CharSequence[] textArray = this.mContext.getResources().getTextArray(2131230834);
            int length = textArray.length;
            for (int i = 0; i < length; i += 2) {
                try {
                    sQLiteDatabase.execSQL("INSERT INTO bookmarks (title, url, visits, date, created, bookmark) VALUES('" + ((Object) textArray[i]) + "', '" + ((Object) BrowserProvider.replaceSystemPropertyInString(this.mContext, textArray[i + 1])) + "', 0, 0, 0, 1);");
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            }
            sQLiteDatabase.execSQL("CREATE TABLE searches (_id INTEGER PRIMARY KEY,search TEXT,date LONG);");
        }

        @Override
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
            if (i >= 24) {
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmarks");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS searches");
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmark_folders");
                onCreate(sQLiteDatabase);
                return;
            }
            sQLiteDatabase.execSQL("DELETE FROM bookmarks WHERE url IS NULL;");
            sQLiteDatabase.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_temp;");
            sQLiteDatabase.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,url TEXT NOT NULL,visits INTEGER,date LONG,created LONG,description TEXT,bookmark INTEGER,favicon BLOB DEFAULT NULL,thumbnail BLOB DEFAULT NULL,touch_icon BLOB DEFAULT NULL,user_entered INTEGER,folder_id INTEGER DEFAULT 0);");
            sQLiteDatabase.execSQL("INSERT INTO bookmarks SELECT * FROM bookmarks_temp;");
            sQLiteDatabase.execSQL("DROP TABLE bookmarks_temp;");
        }
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
        final BrowserProvider this$0;

        public MySuggestionCursor(BrowserProvider browserProvider, Cursor cursor, Cursor cursor2, String str) {
            this.this$0 = browserProvider;
            this.mHistoryCursor = cursor;
            this.mSuggestCursor = cursor2;
            this.mHistoryCount = cursor != null ? cursor.getCount() : 0;
            this.mSuggestionCount = cursor2 != null ? cursor2.getCount() : 0;
            if (this.mSuggestionCount > browserProvider.mMaxSuggestionLongSize - this.mHistoryCount) {
                this.mSuggestionCount = browserProvider.mMaxSuggestionLongSize - this.mHistoryCount;
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

        private String getHistoryTitle() {
            String string = this.mHistoryCursor.getString(2);
            return (TextUtils.isEmpty(string) || TextUtils.getTrimmedLength(string) == 0) ? BrowserProvider.stripUrl(this.mHistoryCursor.getString(1)) : string;
        }

        private String getHistoryUrl() {
            String string = this.mHistoryCursor.getString(2);
            if (TextUtils.isEmpty(string) || TextUtils.getTrimmedLength(string) == 0) {
                return null;
            }
            return BrowserProvider.stripUrl(this.mHistoryCursor.getString(1));
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
        public String[] getColumnNames() {
            return BrowserProvider.COLUMNS;
        }

        @Override
        public int getCount() {
            return this.mIncludeWebSearch ? this.mHistoryCount + this.mSuggestionCount + 1 : this.mHistoryCount + this.mSuggestionCount;
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
            if (this.mPos == -1 || i != 0) {
                throw new UnsupportedOperationException();
            }
            return this.mPos;
        }

        @Override
        public short getShort(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(int i) {
            byte b;
            if (this.mPos == -1 || this.mHistoryCursor == null) {
                return null;
            }
            if (this.mIncludeWebSearch) {
                b = 0;
                if (this.mHistoryCount != 0 || this.mPos != 0) {
                    if (this.mHistoryCount <= 0) {
                        b = -1;
                    } else if (this.mPos == 0) {
                        b = 1;
                    } else if (this.mPos != 1) {
                    }
                }
                if (b == -1) {
                    b = this.mPos + (-1) < this.mHistoryCount ? (byte) 1 : (byte) 2;
                }
            } else if (this.mPos < this.mHistoryCount) {
            }
            switch (i) {
                case 1:
                    if (b == 1) {
                    }
                    break;
                case 2:
                    if (b == 1) {
                    }
                    break;
                case 3:
                    if (b != 0) {
                        if (b != 1) {
                            if (this.mSuggestText1Id != -1) {
                            }
                        }
                    }
                    break;
                case 4:
                    if (b == 0) {
                        break;
                    } else if (b != 1 && this.mSuggestText2Id != -1) {
                        break;
                    }
                    break;
                case 5:
                    if (b != 0) {
                        if (b != 1) {
                            if (this.mSuggestText2UrlId != -1) {
                            }
                        }
                    }
                    break;
                case 6:
                    if (b != 1) {
                        Integer num = 2130837583;
                    } else if (this.mHistoryCursor.getInt(3) != 1) {
                        Integer num2 = 2130837582;
                    } else {
                        Integer num3 = 2130837580;
                    }
                    break;
                case 8:
                    if (b != 0) {
                        if (b != 1) {
                            if (this.mSuggestQueryId != -1) {
                            }
                        }
                    }
                    break;
                case 9:
                    if (b != 0 && b != 1 && this.mSuggestIntentExtraDataId != -1) {
                        break;
                    }
                    break;
            }
            return null;
        }

        @Override
        public boolean isNull(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
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

        @Override
        public boolean requery() {
            return (this.mHistoryCursor != null ? this.mHistoryCursor.requery() : false) | (this.mSuggestCursor != null ? this.mSuggestCursor.requery() : false);
        }
    }

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

    private Cursor doSuggestQuery(String str, String[] strArr, boolean z) {
        String[] strArr2;
        String str2;
        SearchEngine searchEngine;
        if (strArr[0] == null || strArr[0].equals("")) {
            return new MySuggestionCursor(this, null, null, "");
        }
        String str3 = strArr[0] + "%";
        if (strArr[0].startsWith("http") || strArr[0].startsWith("file")) {
            strArr2 = new String[]{str3};
            str2 = str;
        } else {
            this.SUGGEST_ARGS[0] = "http://" + str3;
            this.SUGGEST_ARGS[1] = "http://www." + str3;
            this.SUGGEST_ARGS[2] = "https://" + str3;
            this.SUGGEST_ARGS[3] = "https://www." + str3;
            this.SUGGEST_ARGS[4] = str3;
            strArr2 = this.SUGGEST_ARGS;
            str2 = "(url LIKE ? OR url LIKE ? OR url LIKE ? OR url LIKE ? OR title LIKE ?) AND (bookmark = 1 OR user_entered = 1)";
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[0], SUGGEST_PROJECTION, str2, strArr2, null, null, "visits DESC, date DESC", Integer.toString(this.mMaxSuggestionLongSize));
        return (z || Patterns.WEB_URL.matcher(strArr[0]).matches()) ? new MySuggestionCursor(this, cursorQuery, null, "") : (strArr2 == null || strArr2.length <= 1 || cursorQuery.getCount() >= 2 || (searchEngine = this.mSettings.getSearchEngine()) == null || !searchEngine.supportsSuggestions()) ? new MySuggestionCursor(this, cursorQuery, null, strArr[0]) : new MySuggestionCursor(this, cursorQuery, searchEngine.getSuggestions(getContext(), strArr[0]), strArr[0]);
    }

    public static String getClientId(ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor;
        Cursor cursor2;
        Cursor cursor3;
        String str;
        Cursor cursorQuery2 = null;
        String string = "android-google";
        try {
            cursorQuery = contentResolver.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='search_client_id'", null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        string = cursorQuery.getString(0);
                        str = string;
                    } else {
                        cursorQuery2 = contentResolver.query(Uri.parse("content://com.google.settings/partner"), new String[]{"value"}, "name='client_id'", null, null);
                        if (cursorQuery2 != null) {
                            try {
                                if (cursorQuery2.moveToNext()) {
                                    string = "ms-" + cursorQuery2.getString(0);
                                    str = string;
                                } else {
                                    str = "android-google";
                                }
                            } catch (RuntimeException e) {
                                cursor2 = cursorQuery;
                                cursor3 = cursorQuery2;
                                if (cursor3 != null) {
                                    cursor3.close();
                                }
                                if (cursor2 != null) {
                                    return string;
                                }
                                str = string;
                            } catch (Throwable th) {
                                th = th;
                                cursor = cursorQuery2;
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
                    if (cursorQuery == null) {
                        return str;
                    }
                    cursor2 = cursorQuery;
                } catch (RuntimeException e2) {
                    cursor2 = cursorQuery;
                    cursor3 = cursorQuery2;
                    if (cursor3 != null) {
                    }
                    if (cursor2 != null) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursor = cursorQuery2;
                    if (cursor != null) {
                    }
                    if (cursorQuery != null) {
                    }
                    throw th;
                }
            }
        } catch (RuntimeException e3) {
            cursor2 = null;
            cursor3 = null;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
            cursor = null;
        }
        cursor2.close();
        return str;
    }

    private static CharSequence replaceSystemPropertyInString(Context context, CharSequence charSequence) throws Throwable {
        int i;
        int i2 = 0;
        StringBuffer stringBuffer = new StringBuffer();
        String clientId = getClientId(context.getContentResolver());
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

    private static String stripUrl(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = STRIP_URL_PATTERN.matcher(str);
        return (matcher.matches() && matcher.groupCount() == 3) ? matcher.group(2) : str;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        String str2;
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
            String str3 = uri.getPathSegments().get(1);
            sb.append("_id = ");
            sb.append(str3);
            str = sb.toString();
            str2 = str3;
        } else {
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
        int iDelete = writableDatabase.delete(TABLE_NAMES[iMatch % 10], str, strArr);
        contentResolver.notifyChange(uri, null);
        return iDelete;
    }

    @Override
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

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri uriWithAppendedId;
        boolean z = true;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 0:
                long jInsert = writableDatabase.insert(TABLE_NAMES[0], "url", contentValues);
                uriWithAppendedId = jInsert > 0 ? ContentUris.withAppendedId(Browser.BOOKMARKS_URI, jInsert) : null;
                break;
            case 1:
                long jInsert2 = writableDatabase.insert(TABLE_NAMES[1], "url", contentValues);
                if (jInsert2 <= 0) {
                    z = false;
                    uriWithAppendedId = null;
                } else {
                    uriWithAppendedId = ContentUris.withAppendedId(Browser.SEARCHES_URI, jInsert2);
                    z = false;
                }
                break;
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

    @Override
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

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws IllegalStateException {
        String[] strArr3;
        String str3;
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == -1) {
            throw new IllegalArgumentException("Unknown URL");
        }
        if (iMatch == 20 || iMatch == 21) {
            return doSuggestQuery(str, strArr2, iMatch == 21);
        }
        if (strArr == null || strArr.length <= 0) {
            strArr3 = null;
        } else {
            strArr3 = new String[strArr.length + 1];
            System.arraycopy(strArr, 0, strArr3, 0, strArr.length);
            strArr3[strArr.length] = "_id AS _id";
        }
        if (iMatch == 10 || iMatch == 11) {
            str3 = "_id = " + uri.getPathSegments().get(1);
        } else {
            str3 = null;
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query(TABLE_NAMES[iMatch % 10], strArr3, DatabaseUtils.concatenateWhere(str3, str), strArr2, null, null, str2, null);
        cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        boolean z = true;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == -1 || iMatch == 20) {
            throw new IllegalArgumentException("Unknown URL");
        }
        if (iMatch == 10 || iMatch == 11) {
            StringBuilder sb = new StringBuilder();
            if (str != null && str.length() > 0) {
                sb.append("( ");
                sb.append(str);
                sb.append(" ) AND ");
            }
            String str2 = uri.getPathSegments().get(1);
            sb.append("_id = ");
            sb.append(str2);
            str = sb.toString();
        }
        ContentResolver contentResolver = getContext().getContentResolver();
        if (iMatch == 10 || iMatch == 0) {
            if (!contentValues.containsKey("bookmark")) {
                if ((contentValues.containsKey("title") || contentValues.containsKey("url")) && contentValues.containsKey("_id")) {
                    Cursor cursorQuery = contentResolver.query(Browser.BOOKMARKS_URI, new String[]{"bookmark"}, "_id = " + contentValues.getAsString("_id"), null, null);
                    boolean z2 = cursorQuery.moveToNext() && cursorQuery.getInt(0) != 0;
                    cursorQuery.close();
                    z = z2;
                } else {
                    z = false;
                }
            }
            if (z) {
                this.mBackupManager.dataChanged();
            }
        }
        int iUpdate = writableDatabase.update(TABLE_NAMES[iMatch % 10], contentValues, str, strArr);
        contentResolver.notifyChange(uri, null);
        return iUpdate;
    }
}
