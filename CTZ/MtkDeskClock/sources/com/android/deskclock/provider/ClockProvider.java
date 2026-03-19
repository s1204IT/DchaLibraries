package com.android.deskclock.provider;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.UserManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.ClockContract;
import java.util.Map;

public class ClockProvider extends ContentProvider {
    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final int ALARMS_WITH_INSTANCES = 5;
    private static final String ALARM_JOIN_INSTANCE_TABLE_STATEMENT = "alarm_templates LEFT JOIN alarm_instances ON (alarm_templates._id = alarm_id)";
    private static final String ALARM_JOIN_INSTANCE_WHERE_STATEMENT = "alarm_instances._id IS NULL OR alarm_instances._id = (SELECT _id FROM alarm_instances WHERE alarm_id = alarm_templates._id ORDER BY alarm_state, year, month, day LIMIT 1)";
    private static final int INSTANCES = 3;
    private static final int INSTANCES_ID = 4;
    private static final BroadcastReceiver mCompleteMigrationReceiver;
    private static final Map<String, String> sAlarmsWithInstancesProjection = new ArrayMap();
    private static final UriMatcher sURIMatcher;
    private ClockDatabaseHelper mOpenHelper;

    static {
        sAlarmsWithInstancesProjection.put("alarm_templates._id", "alarm_templates._id");
        sAlarmsWithInstancesProjection.put("alarm_templates.hour", "alarm_templates.hour");
        sAlarmsWithInstancesProjection.put("alarm_templates.minutes", "alarm_templates.minutes");
        sAlarmsWithInstancesProjection.put("alarm_templates.daysofweek", "alarm_templates.daysofweek");
        sAlarmsWithInstancesProjection.put("alarm_templates.enabled", "alarm_templates.enabled");
        sAlarmsWithInstancesProjection.put("alarm_templates.vibrate", "alarm_templates.vibrate");
        sAlarmsWithInstancesProjection.put("alarm_templates.label", "alarm_templates.label");
        sAlarmsWithInstancesProjection.put("alarm_templates.ringtone", "alarm_templates.ringtone");
        sAlarmsWithInstancesProjection.put("alarm_templates.delete_after_use", "alarm_templates.delete_after_use");
        sAlarmsWithInstancesProjection.put("alarm_instances.alarm_state", "alarm_instances.alarm_state");
        sAlarmsWithInstancesProjection.put("alarm_instances._id", "alarm_instances._id");
        sAlarmsWithInstancesProjection.put("alarm_instances.year", "alarm_instances.year");
        sAlarmsWithInstancesProjection.put("alarm_instances.month", "alarm_instances.month");
        sAlarmsWithInstancesProjection.put("alarm_instances.day", "alarm_instances.day");
        sAlarmsWithInstancesProjection.put("alarm_instances.hour", "alarm_instances.hour");
        sAlarmsWithInstancesProjection.put("alarm_instances.minutes", "alarm_instances.minutes");
        sAlarmsWithInstancesProjection.put("alarm_instances.label", "alarm_instances.label");
        sAlarmsWithInstancesProjection.put("alarm_instances.vibrate", "alarm_instances.vibrate");
        sURIMatcher = new UriMatcher(-1);
        sURIMatcher.addURI("com.android.deskclock", "alarms", 1);
        sURIMatcher.addURI("com.android.deskclock", "alarms/#", 2);
        sURIMatcher.addURI("com.android.deskclock", "instances", 3);
        sURIMatcher.addURI("com.android.deskclock", "instances/#", 4);
        sURIMatcher.addURI("com.android.deskclock", "alarms_with_instances", 5);
        mCompleteMigrationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtils.v("User locked, register receiver for migration", new Object[0]);
                if (!context.createDeviceProtectedStorageContext().moveDatabaseFrom(context, "alarms.db")) {
                    LogUtils.wtf("Failed to migrate database", new Object[0]);
                }
                LogUtils.v("[BroadcastReceiver]Migration completed successfully", new Object[0]);
                context.unregisterReceiver(ClockProvider.mCompleteMigrationReceiver);
            }
        };
    }

    @Override
    @TargetApi(24)
    public boolean onCreate() {
        Context context = getContext();
        if (Utils.isNOrLater()) {
            Context contextCreateDeviceProtectedStorageContext = context.createDeviceProtectedStorageContext();
            if (UserManager.get(context).isUserUnlocked()) {
                if (!contextCreateDeviceProtectedStorageContext.moveDatabaseFrom(context, "alarms.db")) {
                    LogUtils.wtf("Failed to migrate database: %s", "alarms.db");
                }
                LogUtils.v("[onCreate]Migration completed successfully", new Object[0]);
            } else {
                LogUtils.v("[onCreate]User locked, register receiver for migration", new Object[0]);
                context.registerReceiver(mCompleteMigrationReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
            }
            context = contextCreateDeviceProtectedStorageContext;
        }
        this.mOpenHelper = new ClockDatabaseHelper(context);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        switch (sURIMatcher.match(uri)) {
            case 1:
                sQLiteQueryBuilder.setTables("alarm_templates");
                break;
            case 2:
                sQLiteQueryBuilder.setTables("alarm_templates");
                sQLiteQueryBuilder.appendWhere("_id=");
                sQLiteQueryBuilder.appendWhere(uri.getLastPathSegment());
                break;
            case 3:
                sQLiteQueryBuilder.setTables("alarm_instances");
                break;
            case 4:
                sQLiteQueryBuilder.setTables("alarm_instances");
                sQLiteQueryBuilder.appendWhere("_id=");
                sQLiteQueryBuilder.appendWhere(uri.getLastPathSegment());
                break;
            case 5:
                sQLiteQueryBuilder.setTables(ALARM_JOIN_INSTANCE_TABLE_STATEMENT);
                sQLiteQueryBuilder.appendWhere(ALARM_JOIN_INSTANCE_WHERE_STATEMENT);
                sQLiteQueryBuilder.setProjectionMap(sAlarmsWithInstancesProjection);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2);
        if (cursorQuery == null) {
            LogUtils.e("Alarms.query: failed", new Object[0]);
        } else {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursorQuery;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/alarms";
            case 2:
                return "vnd.android.cursor.item/alarms";
            case 3:
                return "vnd.android.cursor.dir/instances";
            case 4:
                return "vnd.android.cursor.item/instances";
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String lastPathSegment;
        int iUpdate;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        if (iMatch == 2) {
            lastPathSegment = uri.getLastPathSegment();
            iUpdate = writableDatabase.update("alarm_templates", contentValues, "_id=" + lastPathSegment, null);
        } else if (iMatch == 4) {
            lastPathSegment = uri.getLastPathSegment();
            iUpdate = writableDatabase.update("alarm_instances", contentValues, "_id=" + lastPathSegment, null);
        } else {
            throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }
        LogUtils.v("*** notifyChange() id: " + lastPathSegment + " url " + uri, new Object[0]);
        notifyChange(getContext().getContentResolver(), uri);
        return iUpdate;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        long jFixAlarmInsert;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        if (iMatch == 1) {
            jFixAlarmInsert = this.mOpenHelper.fixAlarmInsert(contentValues);
        } else if (iMatch == 3) {
            jFixAlarmInsert = writableDatabase.insert("alarm_instances", null, contentValues);
        } else {
            throw new IllegalArgumentException("Cannot insert from URI: " + uri);
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jFixAlarmInsert);
        notifyChange(getContext().getContentResolver(), uriWithAppendedId);
        return uriWithAppendedId;
    }

    @Override
    public int delete(@NonNull Uri uri, String str, String[] strArr) {
        int iDelete;
        String str2;
        String str3;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        switch (sURIMatcher.match(uri)) {
            case 1:
                iDelete = writableDatabase.delete("alarm_templates", str, strArr);
                break;
            case 2:
                String lastPathSegment = uri.getLastPathSegment();
                if (TextUtils.isEmpty(str)) {
                    str2 = "_id=" + lastPathSegment;
                } else {
                    str2 = "_id=" + lastPathSegment + " AND (" + str + ")";
                }
                iDelete = writableDatabase.delete("alarm_templates", str2, strArr);
                break;
            case 3:
                iDelete = writableDatabase.delete("alarm_instances", str, strArr);
                break;
            case 4:
                String lastPathSegment2 = uri.getLastPathSegment();
                if (TextUtils.isEmpty(str)) {
                    str3 = "_id=" + lastPathSegment2;
                } else {
                    str3 = "_id=" + lastPathSegment2 + " AND (" + str + ")";
                }
                iDelete = writableDatabase.delete("alarm_instances", str3, strArr);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }
        notifyChange(getContext().getContentResolver(), uri);
        return iDelete;
    }

    private void notifyChange(ContentResolver contentResolver, Uri uri) {
        contentResolver.notifyChange(uri, null);
        int iMatch = sURIMatcher.match(uri);
        if (iMatch == 1 || iMatch == 3 || iMatch == 2 || iMatch == 4) {
            contentResolver.notifyChange(ClockContract.AlarmsColumns.ALARMS_WITH_INSTANCES_URI, null);
        }
    }
}
