package jp.co.benesse.dcha.databox.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class KvsProvider extends ContentProvider {
    private static final int UNSPECIFIED_ID = 0;
    private static final int WIPE_ID = 1;
    private KvsDbHelper mDbHelper;
    public static final String AUTHORITY = KvsProvider.class.getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, ContractKvs.KVS.pathName, UNSPECIFIED_ID);
        sUriMatcher.addURI(AUTHORITY, String.valueOf(ContractKvs.KVS.pathName) + "/cmd/wipe", 1);
        sUriMatcher.addURI(AUTHORITY, String.valueOf(ContractKvs.KVS.pathName) + "/*", ContractKvs.KVS.codeForMany);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table;
        String selection2;
        String[] selectionArgs2;
        try {
            int code = sUriMatcher.match(uri);
            if (code == ContractKvs.KVS.codeForMany || code == 1) {
                if (uri.getPath().endsWith("cmd/wipe")) {
                    table = ContractKvs.KVS.pathName;
                    selection2 = null;
                    selectionArgs2 = null;
                } else {
                    table = ContractKvs.KVS.pathName;
                    selection2 = addAppidToSelection(selection);
                    selectionArgs2 = addAppidToselectionArgs(uri, selectionArgs);
                }
                SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
                int count = db.delete(table, selection2, selectionArgs2);
                return count;
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            return UNSPECIFIED_ID;
        }
    }

    @Override
    public String getType(Uri uri) {
        try {
            int code = sUriMatcher.match(uri);
            if (code == ContractKvs.KVS.codeForMany) {
                String ret = ContractKvs.KVS.metaTypeForMany;
                return ret;
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            int code = sUriMatcher.match(uri);
            if (code == ContractKvs.KVS.codeForMany) {
                String table = ContractKvs.KVS.pathName;
                values.remove(KvsColumns.APP_ID);
                values.put(KvsColumns.APP_ID, uri.getLastPathSegment());
                SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
                long rowId = db.insert(table, null, values);
                if (rowId > 0) {
                    Uri returnUri = ContentUris.withAppendedId(uri, rowId);
                    return returnUri;
                }
                throw new IllegalArgumentException("Failed to insert row into " + uri);
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        this.mDbHelper = new KvsDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int code = sUriMatcher.match(uri);
        try {
            if (code == ContractKvs.KVS.codeForMany) {
                String table = ContractKvs.KVS.pathName;
                String selection2 = addAppidToSelection(selection);
                String[] selectionArgs2 = addAppidToselectionArgs(uri, selectionArgs);
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables(table);
                SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
                Cursor cursor = queryBuilder.query(db, projection, selection2, selectionArgs2, null, null, sortOrder);
                return cursor;
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            int code = sUriMatcher.match(uri);
            if (code == ContractKvs.KVS.codeForMany) {
                String table = ContractKvs.KVS.pathName;
                String selection2 = addAppidToSelection(selection);
                String[] selectionArgs2 = addAppidToselectionArgs(uri, selectionArgs);
                SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
                int count = db.update(table, values, selection2, selectionArgs2);
                return count;
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        } catch (Exception e) {
            return UNSPECIFIED_ID;
        }
    }

    private static String addAppidToSelection(String selection) {
        if (!TextUtils.isEmpty(selection)) {
            String res = "appid = ? AND " + selection;
            return res;
        }
        return "appid = ? ";
    }

    private static String[] addAppidToselectionArgs(Uri uri, String[] selectionArgs) {
        String[] res;
        if (selectionArgs != null && selectionArgs.length > 0) {
            res = new String[selectionArgs.length + 1];
            System.arraycopy(selectionArgs, UNSPECIFIED_ID, res, 1, selectionArgs.length);
        } else {
            res = new String[1];
        }
        res[UNSPECIFIED_ID] = uri.getLastPathSegment();
        return res;
    }
}
