package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;

public class HbpcdLookupProvider extends ContentProvider {
    private static final HashMap<String, String> sArbitraryProjectionMap;
    private static final HashMap<String, String> sConflictProjectionMap;
    private static final HashMap<String, String> sIddProjectionMap;
    private static final HashMap<String, String> sLookupProjectionMap;
    private static final HashMap<String, String> sNanpProjectionMap;
    private static final HashMap<String, String> sRangeProjectionMap;
    private HbpcdLookupDatabaseHelper mDbHelper;
    private static boolean DBG = false;
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);

    static {
        sURIMatcher.addURI("hbpcd_lookup", "idd", 1);
        sURIMatcher.addURI("hbpcd_lookup", "lookup", 2);
        sURIMatcher.addURI("hbpcd_lookup", "conflict", 3);
        sURIMatcher.addURI("hbpcd_lookup", "range", 4);
        sURIMatcher.addURI("hbpcd_lookup", "nanp", 5);
        sURIMatcher.addURI("hbpcd_lookup", "arbitrary", 6);
        sURIMatcher.addURI("hbpcd_lookup", "idd/#", 8);
        sURIMatcher.addURI("hbpcd_lookup", "lookup/#", 9);
        sURIMatcher.addURI("hbpcd_lookup", "conflict/#", 10);
        sURIMatcher.addURI("hbpcd_lookup", "range/#", 11);
        sURIMatcher.addURI("hbpcd_lookup", "nanp/#", 12);
        sURIMatcher.addURI("hbpcd_lookup", "arbitrary/#", 13);
        sIddProjectionMap = new HashMap<>();
        sIddProjectionMap.put("_id", "_id");
        sIddProjectionMap.put("MCC", "MCC");
        sIddProjectionMap.put("IDD", "IDD");
        sLookupProjectionMap = new HashMap<>();
        sLookupProjectionMap.put("_id", "_id");
        sLookupProjectionMap.put("MCC", "MCC");
        sLookupProjectionMap.put("Country_Code", "Country_Code");
        sLookupProjectionMap.put("Country_Name", "Country_Name");
        sLookupProjectionMap.put("NDD", "NDD");
        sLookupProjectionMap.put("NANPS", "NANPS");
        sLookupProjectionMap.put("GMT_Offset_Low", "GMT_Offset_Low");
        sLookupProjectionMap.put("GMT_Offset_High", "GMT_Offset_High");
        sLookupProjectionMap.put("GMT_DST_Low", "GMT_DST_Low");
        sLookupProjectionMap.put("GMT_DST_High", "GMT_DST_High");
        sConflictProjectionMap = new HashMap<>();
        sConflictProjectionMap.put("GMT_Offset_Low", "mcc_lookup_table.GMT_Offset_Low");
        sConflictProjectionMap.put("GMT_Offset_High", "mcc_lookup_table.GMT_Offset_High");
        sConflictProjectionMap.put("GMT_DST_Low", "mcc_lookup_table.GMT_DST_Low");
        sConflictProjectionMap.put("GMT_DST_High", "mcc_lookup_table.GMT_DST_High");
        sConflictProjectionMap.put("MCC", "mcc_sid_conflict.MCC");
        sConflictProjectionMap.put("SID_Conflict", "mcc_sid_conflict.SID_Conflict");
        sRangeProjectionMap = new HashMap<>();
        sRangeProjectionMap.put("_id", "_id");
        sRangeProjectionMap.put("MCC", "MCC");
        sRangeProjectionMap.put("SID_Range_Low", "SID_Range_Low");
        sRangeProjectionMap.put("SID_Range_High", "SID_Range_High");
        sNanpProjectionMap = new HashMap<>();
        sNanpProjectionMap.put("_id", "_id");
        sNanpProjectionMap.put("Area_Code", "Area_Code");
        sArbitraryProjectionMap = new HashMap<>();
        sArbitraryProjectionMap.put("_id", "_id");
        sArbitraryProjectionMap.put("MCC", "MCC");
        sArbitraryProjectionMap.put("SID", "SID");
    }

    @Override
    public boolean onCreate() {
        if (DBG) {
            Log.d("HbpcdLookupProvider", "onCreate");
        }
        this.mDbHelper = new HbpcdLookupDatabaseHelper(getContext());
        this.mDbHelper.getReadableDatabase();
        return true;
    }

    @Override
    public String getType(Uri uri) {
        if (DBG) {
            Log.d("HbpcdLookupProvider", "getType");
            return null;
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        String str4;
        Cursor cursorQuery;
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        boolean zIsEmpty = TextUtils.isEmpty(str2);
        String str5 = null;
        switch (sURIMatcher.match(uri)) {
            case 1:
                sQLiteQueryBuilder.setTables("mcc_idd");
                sQLiteQueryBuilder.setProjectionMap(sIddProjectionMap);
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                        cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 2:
                sQLiteQueryBuilder.setTables("mcc_lookup_table");
                sQLiteQueryBuilder.setProjectionMap(sLookupProjectionMap);
                if (zIsEmpty) {
                    str5 = "MCC ASC";
                }
                str4 = "Country_Name";
                str3 = str5;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 3:
                sQLiteQueryBuilder.setTables("mcc_lookup_table INNER JOIN mcc_sid_conflict ON (mcc_lookup_table.MCC = mcc_sid_conflict.MCC)");
                sQLiteQueryBuilder.setProjectionMap(sConflictProjectionMap);
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 4:
                sQLiteQueryBuilder.setTables("mcc_sid_range");
                sQLiteQueryBuilder.setProjectionMap(sRangeProjectionMap);
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 5:
                sQLiteQueryBuilder.setTables("nanp_area_code");
                sQLiteQueryBuilder.setProjectionMap(sNanpProjectionMap);
                if (zIsEmpty) {
                    str3 = "Area_Code ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 6:
                sQLiteQueryBuilder.setTables("arbitrary_mcc_sid_match");
                sQLiteQueryBuilder.setProjectionMap(sArbitraryProjectionMap);
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 7:
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
            case 8:
                sQLiteQueryBuilder.setTables("mcc_idd");
                sQLiteQueryBuilder.setProjectionMap(sIddProjectionMap);
                sQLiteQueryBuilder.appendWhere("mcc_idd._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 9:
                sQLiteQueryBuilder.setTables("mcc_lookup_table");
                sQLiteQueryBuilder.setProjectionMap(sLookupProjectionMap);
                sQLiteQueryBuilder.appendWhere("mcc_lookup_table._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 10:
                sQLiteQueryBuilder.setTables("mcc_sid_conflict");
                sQLiteQueryBuilder.appendWhere("mcc_sid_conflict._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 11:
                sQLiteQueryBuilder.setTables("mcc_sid_range");
                sQLiteQueryBuilder.setProjectionMap(sRangeProjectionMap);
                sQLiteQueryBuilder.appendWhere("mcc_sid_range._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 12:
                sQLiteQueryBuilder.setTables("nanp_area_code");
                sQLiteQueryBuilder.setProjectionMap(sNanpProjectionMap);
                sQLiteQueryBuilder.appendWhere("nanp_area_code._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "Area_Code ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 13:
                sQLiteQueryBuilder.setTables("arbitrary_mcc_sid_match");
                sQLiteQueryBuilder.setProjectionMap(sArbitraryProjectionMap);
                sQLiteQueryBuilder.appendWhere("arbitrary_mcc_sid_match._id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                if (zIsEmpty) {
                    str3 = "MCC ASC";
                    str4 = null;
                    cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                    if (cursorQuery != null) {
                    }
                    return cursorQuery;
                }
                str3 = null;
                str4 = null;
                cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, str4, null, zIsEmpty ? str2 : str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("Cannot delete URL: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        if (sURIMatcher.match(uri) == 2) {
            return writableDatabase.update("mcc_lookup_table", contentValues, str, strArr);
        }
        throw new UnsupportedOperationException("Cannot update URL: " + uri);
    }
}
