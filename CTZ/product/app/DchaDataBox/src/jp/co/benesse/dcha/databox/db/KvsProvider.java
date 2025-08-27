package jp.co.benesse.dcha.databox.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class KvsProvider extends ContentProvider {
    private static final int UNSPECIFIED_ID = 0;
    private static final int WIPE_ID = 1;
    protected KvsDbHelper mDbHelper;
    private static final String TAG = KvsProvider.class.getSimpleName();
    public static final String AUTHORITY = KvsProvider.class.getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, ContractKvs.KVS.pathName, UNSPECIFIED_ID);
        sUriMatcher.addURI(AUTHORITY, ContractKvs.KVS.pathName + "/cmd/wipe", WIPE_ID);
        sUriMatcher.addURI(AUTHORITY, ContractKvs.KVS.pathName + "/*", ContractKvs.KVS.codeForMany);
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        String[] strArrAddAppidToselectionArgs;
        String str2;
        try {
            int iMatch = sUriMatcher.match(uri);
            if (iMatch != ContractKvs.KVS.codeForMany && iMatch != WIPE_ID) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            String strAddAppidToSelection = null;
            if (uri.getPath().endsWith("cmd/wipe")) {
                str2 = ContractKvs.KVS.pathName;
                strArrAddAppidToselectionArgs = null;
            } else {
                String str3 = ContractKvs.KVS.pathName;
                strAddAppidToSelection = addAppidToSelection(str);
                strArrAddAppidToselectionArgs = addAppidToselectionArgs(uri, strArr);
                str2 = str3;
            }
            return this.mDbHelper.getWritableDatabase().delete(str2, strAddAppidToSelection, strArrAddAppidToselectionArgs);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return UNSPECIFIED_ID;
        }
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        try {
            if (sUriMatcher.match(uri) == ContractKvs.KVS.codeForMany) {
                return ContractKvs.KVS.metaTypeForMany;
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
            if (sUriMatcher.match(uri) == ContractKvs.KVS.codeForMany) {
                String str = ContractKvs.KVS.pathName;
                contentValues.remove(KvsColumns.APP_ID);
                contentValues.put(KvsColumns.APP_ID, uri.getLastPathSegment());
                long jInsert = this.mDbHelper.getWritableDatabase().insert(str, null, contentValues);
                if (jInsert > 0) {
                    return ContentUris.withAppendedId(uri, jInsert);
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
        this.mDbHelper = new KvsDbHelper(getContext());
        this.mDbHelper.setWriteAheadLoggingEnabled(false);
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            if (sUriMatcher.match(uri) == ContractKvs.KVS.codeForMany) {
                String str3 = ContractKvs.KVS.pathName;
                String strAddAppidToSelection = addAppidToSelection(str);
                String[] strArrAddAppidToselectionArgs = addAppidToselectionArgs(uri, strArr2);
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                sQLiteQueryBuilder.setTables(str3);
                return sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, strAddAppidToSelection, strArrAddAppidToselectionArgs, null, null, str2);
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            Logger.e(TAG, e);
            return null;
        }
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        try {
            if (sUriMatcher.match(uri) != ContractKvs.KVS.codeForMany) {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
            return this.mDbHelper.getWritableDatabase().update(ContractKvs.KVS.pathName, contentValues, addAppidToSelection(str), addAppidToselectionArgs(uri, strArr));
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
            strArr2 = new String[WIPE_ID];
        } else {
            strArr2 = new String[strArr.length + WIPE_ID];
            System.arraycopy(strArr, UNSPECIFIED_ID, strArr2, WIPE_ID, strArr.length);
        }
        strArr2[UNSPECIFIED_ID] = uri.getLastPathSegment();
        return strArr2;
    }
}
