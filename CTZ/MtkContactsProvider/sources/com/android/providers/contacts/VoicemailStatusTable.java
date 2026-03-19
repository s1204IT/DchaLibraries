package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.VoicemailContract;
import android.util.ArraySet;
import com.android.common.content.ProjectionMap;
import com.android.providers.contacts.VoicemailContentProvider;
import com.android.providers.contacts.VoicemailTable;
import com.android.providers.contacts.util.DbQueryUtils;

public class VoicemailStatusTable implements VoicemailTable.Delegate {
    private final Context mContext;
    private final CallLogDatabaseHelper mDbHelper;
    private final VoicemailTable.DelegateHelper mDelegateHelper;
    private final String mTableName;
    private static final ProjectionMap sStatusProjectionMap = new ProjectionMap.Builder().add("_id").add("phone_account_component_name").add("phone_account_id").add("configuration_state").add("data_channel_state").add("notification_channel_state").add("settings_uri").add("source_package").add("voicemail_access_uri").add("quota_occupied").add("quota_total").add("source_type").build();
    private static final Object DATABASE_LOCK = new Object();

    public VoicemailStatusTable(String str, Context context, CallLogDatabaseHelper callLogDatabaseHelper, VoicemailTable.DelegateHelper delegateHelper) {
        this.mTableName = str;
        this.mContext = context;
        this.mDbHelper = callLogDatabaseHelper;
        this.mDelegateHelper = delegateHelper;
    }

    @Override
    public Uri insert(VoicemailContentProvider.UriData uriData, ContentValues contentValues) {
        synchronized (DATABASE_LOCK) {
            SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
            String whereClause = uriData.getWhereClause();
            if (createDatabaseModifier(writableDatabase).update(uriData.getUri(), this.mTableName, contentValues, whereClause, null) != 0) {
                Cursor cursorQuery = writableDatabase.query(this.mTableName, new String[]{"_id"}, whereClause, null, null, null, null);
                cursorQuery.moveToFirst();
                int i = cursorQuery.getInt(0);
                cursorQuery.close();
                return ContentUris.withAppendedId(uriData.getUri(), i);
            }
            ContentValues contentValues2 = new ContentValues(contentValues);
            this.mDelegateHelper.checkAndAddSourcePackageIntoValues(uriData, contentValues2);
            long jInsert = createDatabaseModifier(writableDatabase).insert(this.mTableName, null, contentValues2);
            if (jInsert <= 0) {
                return null;
            }
            return ContentUris.withAppendedId(uriData.getUri(), jInsert);
        }
    }

    @Override
    public int bulkInsert(VoicemailContentProvider.UriData uriData, ContentValues[] contentValuesArr) {
        int i = 0;
        for (ContentValues contentValues : contentValuesArr) {
            if (insert(uriData, contentValues) != null) {
                i++;
            }
        }
        return i;
    }

    @Override
    public int delete(VoicemailContentProvider.UriData uriData, String str, String[] strArr) {
        int iDelete;
        synchronized (DATABASE_LOCK) {
            iDelete = createDatabaseModifier(this.mDbHelper.getWritableDatabase()).delete(this.mTableName, DbQueryUtils.concatenateClauses(str, uriData.getWhereClause()), strArr);
        }
        return iDelete;
    }

    @Override
    public Cursor query(VoicemailContentProvider.UriData uriData, String[] strArr, String str, String[] strArr2, String str2) {
        Cursor cursorQuery;
        synchronized (DATABASE_LOCK) {
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            sQLiteQueryBuilder.setTables(this.mTableName);
            sQLiteQueryBuilder.setProjectionMap(sStatusProjectionMap);
            sQLiteQueryBuilder.setStrict(true);
            cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, DbQueryUtils.concatenateClauses(str, uriData.getWhereClause()), strArr2, null, null, str2);
            if (cursorQuery != null) {
                cursorQuery.setNotificationUri(this.mContext.getContentResolver(), VoicemailContract.Status.CONTENT_URI);
            }
        }
        return cursorQuery;
    }

    @Override
    public int update(VoicemailContentProvider.UriData uriData, ContentValues contentValues, String str, String[] strArr) {
        int iUpdate;
        synchronized (DATABASE_LOCK) {
            iUpdate = createDatabaseModifier(this.mDbHelper.getWritableDatabase()).update(uriData.getUri(), this.mTableName, contentValues, DbQueryUtils.concatenateClauses(str, uriData.getWhereClause()), strArr);
        }
        return iUpdate;
    }

    @Override
    public String getType(VoicemailContentProvider.UriData uriData) {
        if (uriData.hasId()) {
            return "vnd.android.cursor.item/voicemail.source.status";
        }
        return "vnd.android.cursor.dir/voicemail.source.status";
    }

    @Override
    public ParcelFileDescriptor openFile(VoicemailContentProvider.UriData uriData, String str) {
        throw new UnsupportedOperationException("File operation is not supported for status table");
    }

    private DatabaseModifier createDatabaseModifier(SQLiteDatabase sQLiteDatabase) {
        return new DbModifierWithNotification(this.mTableName, sQLiteDatabase, this.mContext);
    }

    @Override
    public ArraySet<String> getSourcePackages() {
        return this.mDbHelper.selectDistinctColumn(this.mTableName, "source_package");
    }
}
