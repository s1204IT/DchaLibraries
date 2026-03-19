package com.android.bluetooth.opp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public final class BluetoothOppProvider extends ContentProvider {
    private static final String DB_NAME = "btopp.db";
    private static final String DB_TABLE = "btopp";
    private static final int DB_VERSION = 2;
    private static final int DB_VERSION_NOP_UPGRADE_FROM = 0;
    private static final int DB_VERSION_NOP_UPGRADE_TO = 1;
    private static final int SHARES = 1;
    private static final int SHARES_ID = 2;
    private static final String SHARE_LIST_TYPE = "vnd.android.cursor.dir/vnd.android.btopp";
    private static final String SHARE_TYPE = "vnd.android.cursor.item/vnd.android.btopp";
    private static final String TAG = "BluetoothOppProvider";
    private SQLiteOpenHelper mOpenHelper = null;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);

    static {
        sURIMatcher.addURI("com.android.bluetooth.opp", DB_TABLE, 1);
        sURIMatcher.addURI("com.android.bluetooth.opp", "btopp/#", 2);
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, BluetoothOppProvider.DB_NAME, (SQLiteDatabase.CursorFactory) null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            if (BluetoothOppProvider.V) {
                Log.v(BluetoothOppProvider.TAG, "populating new database");
            }
            BluetoothOppProvider.this.createTable(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i == 0) {
                if (i2 == 1) {
                    return;
                } else {
                    i = 1;
                }
            }
            Log.i(BluetoothOppProvider.TAG, "Upgrading downloads database from version " + i + " to " + i2 + ", which will destroy all old data");
            BluetoothOppProvider.this.dropTable(sQLiteDatabase);
            BluetoothOppProvider.this.createTable(sQLiteDatabase);
        }
    }

    private void createTable(SQLiteDatabase sQLiteDatabase) {
        try {
            try {
                sQLiteDatabase.beginTransaction();
                sQLiteDatabase.execSQL("CREATE TABLE btopp(_id INTEGER PRIMARY KEY AUTOINCREMENT,uri TEXT, hint TEXT, _data TEXT, mimetype TEXT, direction INTEGER, destination TEXT, carrier_name TEXT, visibility INTEGER, confirm INTEGER, status INTEGER, total_bytes INTEGER, current_bytes INTEGER, timestamp INTEGER,scanned INTEGER); ");
                sQLiteDatabase.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e(TAG, "createTable: Failed.");
                throw e;
            }
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private void dropTable(SQLiteDatabase sQLiteDatabase) {
        try {
            try {
                sQLiteDatabase.beginTransaction();
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS btopp");
                sQLiteDatabase.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e(TAG, "dropTable: Failed.");
                throw e;
            }
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case 1:
                return SHARE_LIST_TYPE;
            case 2:
                return SHARE_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI in getType(): " + uri);
        }
    }

    private static void copyString(String str, ContentValues contentValues, ContentValues contentValues2) {
        String asString = contentValues.getAsString(str);
        if (asString != null) {
            contentValues2.put(str, asString);
        }
    }

    private static void copyInteger(String str, ContentValues contentValues, ContentValues contentValues2) {
        Integer asInteger = contentValues.getAsInteger(str);
        if (asInteger != null) {
            contentValues2.put(str, asInteger);
        }
    }

    private static void copyLong(String str, ContentValues contentValues, ContentValues contentValues2) {
        Long asLong = contentValues.getAsLong(str);
        if (asLong != null) {
            contentValues2.put(str, asLong);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (sURIMatcher.match(uri) != 1) {
            throw new IllegalArgumentException("insert: Unknown/Invalid URI " + uri);
        }
        ContentValues contentValues2 = new ContentValues();
        copyString("uri", contentValues, contentValues2);
        copyString(BluetoothShare.FILENAME_HINT, contentValues, contentValues2);
        copyString(BluetoothShare.MIMETYPE, contentValues, contentValues2);
        copyString(BluetoothShare.DESTINATION, contentValues, contentValues2);
        copyString(BluetoothShare.CARRIER_NAME, contentValues, contentValues2);
        copyInteger(BluetoothShare.VISIBILITY, contentValues, contentValues2);
        copyLong(BluetoothShare.TOTAL_BYTES, contentValues, contentValues2);
        if (contentValues.getAsInteger(BluetoothShare.VISIBILITY) == null) {
            contentValues2.put(BluetoothShare.VISIBILITY, (Integer) 0);
        }
        Integer asInteger = contentValues.getAsInteger(BluetoothShare.DIRECTION);
        int asInteger2 = contentValues.getAsInteger("confirm");
        if (asInteger == null) {
            asInteger = 0;
        }
        if (asInteger.intValue() == 0 && asInteger2 == null) {
            asInteger2 = 2;
        }
        if (asInteger.intValue() == 1 && asInteger2 == null) {
            asInteger2 = 0;
        }
        contentValues2.put("confirm", asInteger2);
        contentValues2.put(BluetoothShare.DIRECTION, asInteger);
        contentValues2.put("status", Integer.valueOf(BluetoothShare.STATUS_PENDING));
        contentValues2.put("scanned", (Integer) 0);
        Long asLong = contentValues.getAsLong("timestamp");
        if (asLong == null) {
            asLong = Long.valueOf(System.currentTimeMillis());
        }
        contentValues2.put("timestamp", asLong);
        Context context = getContext();
        long jInsert = writableDatabase.insert(DB_TABLE, null, contentValues2);
        if (jInsert == -1) {
            Log.w(TAG, "couldn't insert " + uri + "into btopp database");
            return null;
        }
        context.getContentResolver().notifyChange(uri, null);
        return Uri.parse(BluetoothShare.CONTENT_URI + "/" + jInsert);
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setStrict(true);
        switch (sURIMatcher.match(uri)) {
            case 1:
                sQLiteQueryBuilder.setTables(DB_TABLE);
                break;
            case 2:
                sQLiteQueryBuilder.setTables(DB_TABLE);
                sQLiteQueryBuilder.appendWhere("_id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (V) {
            StringBuilder sb = new StringBuilder();
            sb.append("starting query, database is ");
            if (readableDatabase != null) {
                sb.append("not ");
            }
            sb.append("null; ");
            if (strArr == null) {
                sb.append("projection is null; ");
            } else if (strArr.length == 0) {
                sb.append("projection is empty; ");
            } else {
                for (int i = 0; i < strArr.length; i++) {
                    sb.append("projection[");
                    sb.append(i);
                    sb.append("] is ");
                    sb.append(strArr[i]);
                    sb.append("; ");
                }
            }
            sb.append("selection is ");
            sb.append(str);
            sb.append("; ");
            if (strArr2 == null) {
                sb.append("selectionArgs is null; ");
            } else if (strArr2.length == 0) {
                sb.append("selectionArgs is empty; ");
            } else {
                for (int i2 = 0; i2 < strArr2.length; i2++) {
                    sb.append("selectionArgs[");
                    sb.append(i2);
                    sb.append("] is ");
                    sb.append(strArr2[i2]);
                    sb.append("; ");
                }
            }
            sb.append("sort is ");
            sb.append(str2);
            sb.append(".");
            Log.v(TAG, sb.toString());
        }
        Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2);
        if (cursorQuery == null) {
            Log.w(TAG, "query failed in downloads database");
            return null;
        }
        cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String str2;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        switch (iMatch) {
            case 1:
            case 2:
                if (str != null) {
                    if (iMatch == 1) {
                        str2 = "( " + str + " )";
                    } else {
                        str2 = "( " + str + " ) AND ";
                    }
                } else {
                    str2 = "";
                }
                if (iMatch == 2) {
                    str2 = str2 + " ( _id = " + Long.parseLong(uri.getPathSegments().get(1)) + " ) ";
                }
                int iUpdate = 0;
                if (contentValues.size() > 0) {
                    try {
                        iUpdate = writableDatabase.update(DB_TABLE, contentValues, str2, strArr);
                    } catch (SQLiteFullException e) {
                        e.printStackTrace();
                    }
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return iUpdate;
            default:
                throw new UnsupportedOperationException("Cannot update unknown URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        String str2;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        switch (iMatch) {
            case 1:
            case 2:
                if (str != null) {
                    if (iMatch == 1) {
                        str2 = "( " + str + " )";
                    } else {
                        str2 = "( " + str + " ) AND ";
                    }
                } else {
                    str2 = "";
                }
                if (iMatch == 2) {
                    str2 = str2 + " ( _id = " + Long.parseLong(uri.getPathSegments().get(1)) + " ) ";
                }
                try {
                    try {
                        writableDatabase.beginTransaction();
                        int iDelete = writableDatabase.delete(DB_TABLE, str2, strArr);
                        writableDatabase.setTransactionSuccessful();
                        writableDatabase.endTransaction();
                        getContext().getContentResolver().notifyChange(uri, null);
                        return iDelete;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Log.e(TAG, "couldn't delete data from table");
                        throw e;
                    }
                } catch (Throwable th) {
                    writableDatabase.endTransaction();
                    throw th;
                }
            default:
                throw new UnsupportedOperationException("Cannot delete unknown URI: " + uri);
        }
    }
}
