package jp.co.benesse.touch.sbox;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import java.util.Map;
import jp.co.benesse.touch.util.Logger;

/* loaded from: classes.dex */
public class SboxProvider extends ContentProvider {
    private static final int SBOX_ID = 2;
    private static final int UNSPECIFIED_ID = 0;
    private static final int WIPE_ID = 1;
    protected SboxDbHelper mDbHelper;
    protected KeyStoreAdapter mKeyStoreAdapter;
    private static final String TAG = SboxProvider.class.getSimpleName();
    private static final String AUTHORITY = SboxProvider.class.getPackage().getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, "/", UNSPECIFIED_ID);
        sUriMatcher.addURI(AUTHORITY, "cmd/wipe", 1);
        sUriMatcher.addURI(AUTHORITY, "*", 2);
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        String[] strArrAddAppidToselectionArgs;
        try {
            int iMatch = sUriMatcher.match(uri);
            String strAddAppidToSelection = null;
            if (iMatch == 2) {
                strAddAppidToSelection = addAppidToSelection(str);
                strArrAddAppidToselectionArgs = addAppidToselectionArgs(uri, strArr);
            } else {
                if (iMatch != 1) {
                    throw new IllegalArgumentException("Unknown URI " + uri);
                }
                strArrAddAppidToselectionArgs = null;
            }
            return this.mDbHelper.getWritableDatabase().delete(SboxDbHelper.TABLE_NAME, strAddAppidToSelection, strArrAddAppidToselectionArgs);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return UNSPECIFIED_ID;
        }
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        try {
            if (sUriMatcher.match(uri) == 2) {
                return "vnd.android.cursor.item/vnd." + AUTHORITY;
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return null;
        }
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        try {
            if (sUriMatcher.match(uri) == 2) {
                contentValues.remove(SboxColumns.APP_ID);
                contentValues.put(SboxColumns.APP_ID, uri.getLastPathSegment());
                for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
                    if (TextUtils.equals(SboxColumns.VALUE, entry.getKey())) {
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            entry.setValue(this.mKeyStoreAdapter.encryptString((String) value));
                        }
                    }
                }
                long jReplace = this.mDbHelper.getWritableDatabase().replace(SboxDbHelper.TABLE_NAME, null, contentValues);
                if (jReplace >= 0) {
                    return ContentUris.withAppendedId(uri, jReplace);
                }
                throw new IllegalArgumentException("Failed to insert row into " + uri);
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return null;
        }
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        this.mDbHelper = new SboxDbHelper(getContext());
        this.mDbHelper.setWriteAheadLoggingEnabled(false);
        this.mKeyStoreAdapter = new KeyStoreAdapter(getContext());
        return true;
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:18:0x0076 */
    /* JADX WARN: Type inference failed for: r7v0 */
    /* JADX WARN: Type inference failed for: r7v1 */
    /* JADX WARN: Type inference failed for: r7v4, types: [int] */
    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws Throwable {
        ?? Match;
        MatrixCursor matrixCursor;
        Cursor cursorQuery;
        try {
            Match = sUriMatcher.match(uri);
        } catch (Exception e) {
            e = e;
            Match = 0;
        }
        if (Match == 2) {
            try {
                String strAddAppidToSelection = addAppidToSelection(str);
                String[] strArrAddAppidToselectionArgs = addAppidToselectionArgs(uri, strArr2);
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                sQLiteQueryBuilder.setTables(SboxDbHelper.TABLE_NAME);
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, strAddAppidToSelection, strArrAddAppidToselectionArgs, null, null, str2);
            } catch (Exception e2) {
                e = e2;
                Logger.e(TAG, e);
                matrixCursor = Match;
                return matrixCursor;
            }
            try {
                MatrixCursor matrixCursor2 = new MatrixCursor(new String[]{SboxColumns.KEY, SboxColumns.VALUE});
                while (cursorQuery.moveToNext()) {
                    try {
                        matrixCursor2.addRow(new String[]{cursorQuery.getString(cursorQuery.getColumnIndex(SboxColumns.KEY)), this.mKeyStoreAdapter.decryptString(cursorQuery.getString(cursorQuery.getColumnIndex(SboxColumns.VALUE)))});
                    } catch (Throwable th) {
                        th = th;
                        cursorQuery.close();
                        throw th;
                    }
                }
                cursorQuery.close();
                matrixCursor = matrixCursor2;
                return matrixCursor;
            } catch (Throwable th2) {
                th = th2;
            }
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        try {
            if (sUriMatcher.match(uri) == 2) {
                String strAddAppidToSelection = addAppidToSelection(str);
                String[] strArrAddAppidToselectionArgs = addAppidToselectionArgs(uri, strArr);
                for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
                    if (TextUtils.equals(SboxColumns.VALUE, entry.getKey())) {
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            entry.setValue(this.mKeyStoreAdapter.encryptString((String) value));
                        }
                    }
                }
                return this.mDbHelper.getWritableDatabase().update(SboxDbHelper.TABLE_NAME, contentValues, strAddAppidToSelection, strArrAddAppidToselectionArgs);
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return UNSPECIFIED_ID;
        }
    }

    private static String addAppidToSelection(String str) {
        if (TextUtils.isEmpty(str)) {
            return "appid = ? ";
        }
        return "appid = ? AND " + str;
    }

    private static String[] addAppidToselectionArgs(Uri uri, String[] strArr) {
        String[] strArr2;
        if (strArr == null || strArr.length <= 0) {
            strArr2 = new String[1];
        } else {
            strArr2 = new String[strArr.length + 1];
            System.arraycopy(strArr, UNSPECIFIED_ID, strArr2, 1, strArr.length);
        }
        strArr2[UNSPECIFIED_ID] = uri.getLastPathSegment();
        return strArr2;
    }
}
