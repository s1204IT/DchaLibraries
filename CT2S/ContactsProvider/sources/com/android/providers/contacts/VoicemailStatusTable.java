package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.VoicemailContract;
import com.android.common.content.ProjectionMap;
import com.android.providers.contacts.VoicemailContentProvider;
import com.android.providers.contacts.VoicemailTable;
import com.android.providers.contacts.util.DbQueryUtils;

public class VoicemailStatusTable implements VoicemailTable.Delegate {
    private static final ProjectionMap sStatusProjectionMap = new ProjectionMap.Builder().add("_id").add("configuration_state").add("data_channel_state").add("notification_channel_state").add("settings_uri").add("source_package").add("voicemail_access_uri").build();
    private final Context mContext;
    private final SQLiteOpenHelper mDbHelper;
    private final VoicemailTable.DelegateHelper mDelegateHelper;
    private final String mTableName;

    public VoicemailStatusTable(String tableName, Context context, SQLiteOpenHelper dbHelper, VoicemailTable.DelegateHelper delegateHelper) {
        this.mTableName = tableName;
        this.mContext = context;
        this.mDbHelper = dbHelper;
        this.mDelegateHelper = delegateHelper;
    }

    @Override
    public Uri insert(VoicemailContentProvider.UriData uriData, ContentValues values) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        ContentValues copiedValues = new ContentValues(values);
        this.mDelegateHelper.checkAndAddSourcePackageIntoValues(uriData, copiedValues);
        long rowId = getDatabaseModifier(db).insert(this.mTableName, null, copiedValues);
        if (rowId > 0) {
            return ContentUris.withAppendedId(uriData.getUri(), rowId);
        }
        return null;
    }

    @Override
    public int delete(VoicemailContentProvider.UriData uriData, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause());
        return getDatabaseModifier(db).delete(this.mTableName, combinedClause, selectionArgs);
    }

    @Override
    public Cursor query(VoicemailContentProvider.UriData uriData, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(this.mTableName);
        qb.setProjectionMap(sStatusProjectionMap);
        qb.setStrict(true);
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause());
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, combinedClause, selectionArgs, null, null, sortOrder);
        if (c != null) {
            c.setNotificationUri(this.mContext.getContentResolver(), VoicemailContract.Status.CONTENT_URI);
        }
        return c;
    }

    @Override
    public int update(VoicemailContentProvider.UriData uriData, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause());
        return getDatabaseModifier(db).update(this.mTableName, values, combinedClause, selectionArgs);
    }

    @Override
    public String getType(VoicemailContentProvider.UriData uriData) {
        return uriData.hasId() ? "vnd.android.cursor.item/voicemail.source.status" : "vnd.android.cursor.dir/voicemail.source.status";
    }

    @Override
    public ParcelFileDescriptor openFile(VoicemailContentProvider.UriData uriData, String mode) {
        throw new UnsupportedOperationException("File operation is not supported for status table");
    }

    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification(this.mTableName, db, this.mContext);
    }
}
