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
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = null;
        String groupBy = null;
        boolean useDefaultOrder = TextUtils.isEmpty(sortOrder);
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
                qb.setTables("mcc_idd");
                qb.setProjectionMap(sIddProjectionMap);
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 2:
                qb.setTables("mcc_lookup_table");
                qb.setProjectionMap(sLookupProjectionMap);
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                groupBy = "Country_Name";
                break;
            case 3:
                qb.setTables("mcc_lookup_table INNER JOIN mcc_sid_conflict ON (mcc_lookup_table.MCC = mcc_sid_conflict.MCC)");
                qb.setProjectionMap(sConflictProjectionMap);
                break;
            case 4:
                qb.setTables("mcc_sid_range");
                qb.setProjectionMap(sRangeProjectionMap);
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 5:
                qb.setTables("nanp_area_code");
                qb.setProjectionMap(sNanpProjectionMap);
                if (useDefaultOrder) {
                    orderBy = "Area_Code ASC";
                }
                break;
            case 6:
                qb.setTables("arbitrary_mcc_sid_match");
                qb.setProjectionMap(sArbitraryProjectionMap);
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 7:
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
            case 8:
                qb.setTables("mcc_idd");
                qb.setProjectionMap(sIddProjectionMap);
                qb.appendWhere("mcc_idd._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 9:
                qb.setTables("mcc_lookup_table");
                qb.setProjectionMap(sLookupProjectionMap);
                qb.appendWhere("mcc_lookup_table._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 10:
                qb.setTables("mcc_sid_conflict");
                qb.appendWhere("mcc_sid_conflict._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 11:
                qb.setTables("mcc_sid_range");
                qb.setProjectionMap(sRangeProjectionMap);
                qb.appendWhere("mcc_sid_range._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
            case 12:
                qb.setTables("nanp_area_code");
                qb.setProjectionMap(sNanpProjectionMap);
                qb.appendWhere("nanp_area_code._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "Area_Code ASC";
                }
                break;
            case 13:
                qb.setTables("arbitrary_mcc_sid_match");
                qb.setProjectionMap(sArbitraryProjectionMap);
                qb.appendWhere("arbitrary_mcc_sid_match._id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                if (useDefaultOrder) {
                    orderBy = "MCC ASC";
                }
                break;
        }
        if (!useDefaultOrder) {
            orderBy = sortOrder;
        }
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, groupBy, null, orderBy);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Cannot delete URL: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 2:
                int count = db.update("mcc_lookup_table", values, selection, selectionArgs);
                return count;
            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
        }
    }
}
