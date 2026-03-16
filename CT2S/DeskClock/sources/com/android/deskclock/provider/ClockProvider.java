package com.android.deskclock.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.android.deskclock.LogUtils;
import com.android.deskclock.provider.ClockContract;

public class ClockProvider extends ContentProvider {
    private static final UriMatcher sURLMatcher = new UriMatcher(-1);
    private ClockDatabaseHelper mOpenHelper;

    static {
        sURLMatcher.addURI("com.android.deskclock", "alarms", 1);
        sURLMatcher.addURI("com.android.deskclock", "alarms/#", 2);
        sURLMatcher.addURI("com.android.deskclock", "instances", 3);
        sURLMatcher.addURI("com.android.deskclock", "instances/#", 4);
        sURLMatcher.addURI("com.android.deskclock", "cities", 5);
        sURLMatcher.addURI("com.android.deskclock", "cities/*", 6);
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new ClockDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = sURLMatcher.match(uri);
        switch (match) {
            case 1:
                qb.setTables("alarm_templates");
                break;
            case 2:
                qb.setTables("alarm_templates");
                qb.appendWhere("_id=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case 3:
                qb.setTables("alarm_instances");
                break;
            case 4:
                qb.setTables("alarm_instances");
                qb.appendWhere("_id=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            case 5:
                qb.setTables("selected_cities");
                break;
            case 6:
                qb.setTables("selected_cities");
                qb.appendWhere("city_id=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
        if (ret == null) {
            LogUtils.e("Alarms.query: failed", new Object[0]);
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return ret;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
            case 1:
                return "vnd.android.cursor.dir/alarms";
            case 2:
                return "vnd.android.cursor.item/alarms";
            case 3:
                return "vnd.android.cursor.dir/instances";
            case 4:
                return "vnd.android.cursor.item/instances";
            case 5:
                return "vnd.android.cursor.dir/cities";
            case 6:
                return "vnd.android.cursor.item/cities";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        String alarmId;
        int count;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case 2:
                alarmId = uri.getLastPathSegment();
                count = db.update("alarm_templates", values, "_id=" + alarmId, null);
                break;
            case 3:
            case 5:
            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
            case 4:
                alarmId = uri.getLastPathSegment();
                count = db.update("alarm_instances", values, "_id=" + alarmId, null);
                break;
            case 6:
                alarmId = uri.getLastPathSegment();
                count = db.update("selected_cities", values, "city_id=" + alarmId, null);
                break;
        }
        LogUtils.v("*** notifyChange() id: " + alarmId + " url " + uri, new Object[0]);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case 1:
                rowId = this.mOpenHelper.fixAlarmInsert(initialValues);
                break;
            case 2:
            case 4:
            default:
                throw new IllegalArgumentException("Cannot insert from URL: " + uri);
            case 3:
                rowId = db.insert("alarm_instances", null, initialValues);
                break;
            case 5:
                rowId = db.insert("selected_cities", null, initialValues);
                break;
        }
        Uri uriResult = ContentUris.withAppendedId(ClockContract.AlarmsColumns.CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(uriResult, null);
        return uriResult;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        String where2;
        int count;
        String where3;
        String where4;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case 1:
                count = db.delete("alarm_templates", where, whereArgs);
                break;
            case 2:
                String primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where4 = "_id=" + primaryKey;
                } else {
                    where4 = "_id=" + primaryKey + " AND (" + where + ")";
                }
                count = db.delete("alarm_templates", where4, whereArgs);
                break;
            case 3:
                count = db.delete("alarm_instances", where, whereArgs);
                break;
            case 4:
                String primaryKey2 = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where3 = "_id=" + primaryKey2;
                } else {
                    where3 = "_id=" + primaryKey2 + " AND (" + where + ")";
                }
                count = db.delete("alarm_instances", where3, whereArgs);
                break;
            case 5:
                count = db.delete("selected_cities", where, whereArgs);
                break;
            case 6:
                String primaryKey3 = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where2 = "city_id=" + primaryKey3;
                } else {
                    where2 = "city_id=" + primaryKey3 + " AND (" + where + ")";
                }
                count = db.delete("selected_cities", where2, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
