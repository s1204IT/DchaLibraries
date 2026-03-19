package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.VoicemailContract;
import android.util.ArraySet;
import android.util.Log;
import com.android.common.content.ProjectionMap;
import com.android.providers.contacts.VoicemailContentProvider;
import com.android.providers.contacts.VoicemailTable;
import com.android.providers.contacts.util.CloseUtils;
import com.android.providers.contacts.util.DbQueryUtils;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VoicemailContentTable implements VoicemailTable.Delegate {
    private final CallLogInsertionHelper mCallLogInsertionHelper;
    private final Context mContext;
    private final CallLogDatabaseHelper mDbHelper;
    private final VoicemailTable.DelegateHelper mDelegateHelper;
    private final String mTableName;
    private final ProjectionMap mVoicemailProjectionMap;
    private static final String[] FILENAME_ONLY_PROJECTION = {"_data"};
    public static final ImmutableSet<String> ALLOWED_COLUMNS = new ImmutableSet.Builder().add("_id").add("number").add("date").add("duration").add("new").add("is_read").add("transcription").add("transcription_state").add("state").add("source_data").add("source_package").add("has_content").add("subscription_component_name").add("subscription_id").add("mime_type").add("dirty").add("deleted").add("last_modified").add("backed_up").add("restored").add("archived").add("is_omtp_voicemail").add("_display_name").add("_size").build();

    public VoicemailContentTable(String str, Context context, CallLogDatabaseHelper callLogDatabaseHelper, VoicemailTable.DelegateHelper delegateHelper, CallLogInsertionHelper callLogInsertionHelper) {
        this.mTableName = str;
        this.mContext = context;
        this.mDbHelper = callLogDatabaseHelper;
        this.mDelegateHelper = delegateHelper;
        this.mVoicemailProjectionMap = new ProjectionMap.Builder().add("_id").add("number").add("date").add("duration").add("new").add("is_read").add("transcription").add("transcription_state").add("state").add("source_data").add("source_package").add("has_content").add("mime_type").add("_data").add("subscription_component_name").add("subscription_id").add("dirty").add("deleted").add("last_modified").add("backed_up").add("restored").add("archived").add("is_omtp_voicemail").add("_display_name", createDisplayName(context)).add("_size", "NULL").build();
        this.mCallLogInsertionHelper = callLogInsertionHelper;
    }

    private static String createDisplayName(Context context) {
        return DatabaseUtils.sqlEscapeString(context.getString(R.string.voicemail_from_column)) + " || number";
    }

    @Override
    public Uri insert(VoicemailContentProvider.UriData uriData, ContentValues contentValues) {
        return insertRow(createDatabaseModifier(this.mDbHelper.getWritableDatabase()), uriData, contentValues);
    }

    @Override
    public int bulkInsert(VoicemailContentProvider.UriData uriData, ContentValues[] contentValuesArr) {
        DatabaseModifier databaseModifierCreateDatabaseModifier = createDatabaseModifier(this.mDbHelper.getWritableDatabase());
        databaseModifierCreateDatabaseModifier.startBulkOperation();
        int i = 0;
        for (ContentValues contentValues : contentValuesArr) {
            if (insertRow(databaseModifierCreateDatabaseModifier, uriData, contentValues) != null) {
                i++;
            }
            if (i % 50 == 0) {
                databaseModifierCreateDatabaseModifier.yieldBulkOperation();
            }
        }
        databaseModifierCreateDatabaseModifier.finishBulkOperation();
        return i;
    }

    private Uri insertRow(DatabaseModifier databaseModifier, VoicemailContentProvider.UriData uriData, ContentValues contentValues) {
        boolean zBooleanValue;
        DbQueryUtils.checkForSupportedColumns(this.mVoicemailProjectionMap, contentValues);
        ContentValues contentValues2 = new ContentValues(contentValues);
        checkInsertSupported(uriData);
        this.mDelegateHelper.checkAndAddSourcePackageIntoValues(uriData, contentValues2);
        this.mCallLogInsertionHelper.addComputedValues(contentValues2);
        contentValues2.put("_data", generateDataFile());
        contentValues2.put("type", (Integer) 4);
        if (contentValues.containsKey("is_read")) {
            zBooleanValue = contentValues.getAsBoolean("is_read").booleanValue();
        } else {
            zBooleanValue = false;
        }
        if (!contentValues.containsKey("new")) {
            contentValues2.put("new", Boolean.valueOf(zBooleanValue ? false : true));
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        long jInsert = databaseModifier.insert(this.mTableName, null, contentValues2);
        if (jInsert <= 0) {
            return null;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(uriData.getUri(), jInsert);
        updateVoicemailUri(writableDatabase, uriWithAppendedId);
        return uriWithAppendedId;
    }

    private void checkInsertSupported(VoicemailContentProvider.UriData uriData) {
        if (uriData.hasId()) {
            throw new UnsupportedOperationException(String.format("Cannot insert URI: %s. Inserted URIs should not contain an id.", uriData.getUri()));
        }
    }

    private String generateDataFile() {
        try {
            return File.createTempFile("voicemail", "", this.mContext.getDir("voicemail-data", 0)).getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("unable to create temp file", e);
        }
    }

    private void updateVoicemailUri(SQLiteDatabase sQLiteDatabase, Uri uri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("voicemail_uri", uri.toString());
        sQLiteDatabase.update(this.mTableName, contentValues, VoicemailContentProvider.UriData.createUriData(uri).getWhereClause(), null);
    }

    @Override
    public int delete(VoicemailContentProvider.UriData uriData, String str, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        String strConcatenateClauses = DbQueryUtils.concatenateClauses(str, uriData.getWhereClause(), getCallTypeClause());
        try {
            cursorQuery = query(uriData, FILENAME_ONLY_PROJECTION, str, strArr, null);
            while (cursorQuery.moveToNext()) {
                try {
                    String string = cursorQuery.getString(0);
                    if (string == null) {
                        Log.w("VmContentProvider", "No filename for uri " + uriData.getUri() + ", cannot delete file");
                    } else {
                        File file = new File(string);
                        if (file.exists() && !file.delete()) {
                            Log.e("VmContentProvider", "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    CloseUtils.closeQuietly(cursorQuery);
                    throw th;
                }
            }
            CloseUtils.closeQuietly(cursorQuery);
            return createDatabaseModifier(writableDatabase).delete(this.mTableName, strConcatenateClauses, strArr);
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    @Override
    public Cursor query(VoicemailContentProvider.UriData uriData, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(this.mTableName);
        sQLiteQueryBuilder.setProjectionMap(this.mVoicemailProjectionMap);
        sQLiteQueryBuilder.setStrict(true);
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, DbQueryUtils.concatenateClauses(str, uriData.getWhereClause(), getCallTypeClause()), strArr2, null, null, str2);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(this.mContext.getContentResolver(), VoicemailContract.Voicemails.CONTENT_URI);
        }
        return cursorQuery;
    }

    @Override
    public int update(VoicemailContentProvider.UriData uriData, ContentValues contentValues, String str, String[] strArr) {
        DbQueryUtils.checkForSupportedColumns(ALLOWED_COLUMNS, contentValues, "Updates are not allowed.");
        checkUpdateSupported(uriData);
        return createDatabaseModifier(this.mDbHelper.getWritableDatabase()).update(uriData.getUri(), this.mTableName, contentValues, DbQueryUtils.concatenateClauses(str, uriData.getWhereClause(), getCallTypeClause()), strArr);
    }

    private void checkUpdateSupported(VoicemailContentProvider.UriData uriData) {
        if (!uriData.hasId()) {
            throw new UnsupportedOperationException(String.format("Cannot update URI: %s.  Bulk update not supported", uriData.getUri()));
        }
    }

    @Override
    public String getType(VoicemailContentProvider.UriData uriData) {
        if (uriData.hasId()) {
            return "vnd.android.cursor.item/voicemail";
        }
        return "vnd.android.cursor.dir/voicemails";
    }

    @Override
    public ParcelFileDescriptor openFile(VoicemailContentProvider.UriData uriData, String str) throws FileNotFoundException {
        return this.mDelegateHelper.openDataFile(uriData, str);
    }

    @Override
    public ArraySet<String> getSourcePackages() {
        return this.mDbHelper.selectDistinctColumn(this.mTableName, "source_package");
    }

    private String getCallTypeClause() {
        return DbQueryUtils.getEqualityClause("type", 4L);
    }

    private DatabaseModifier createDatabaseModifier(SQLiteDatabase sQLiteDatabase) {
        return new DbModifierWithNotification(this.mTableName, sQLiteDatabase, this.mContext);
    }
}
