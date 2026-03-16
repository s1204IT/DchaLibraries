package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.VoicemailContract;
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
    private final SQLiteOpenHelper mDbHelper;
    private final VoicemailTable.DelegateHelper mDelegateHelper;
    private final String mTableName;
    private final ProjectionMap mVoicemailProjectionMap;
    private static final String[] FILENAME_ONLY_PROJECTION = {"_data"};
    private static final ImmutableSet<String> ALLOWED_COLUMNS = new ImmutableSet.Builder().add("_id").add("number").add("date").add("duration").add("is_read").add("transcription").add("state").add("source_data").add("source_package").add("has_content").add("mime_type").add("_display_name").add("_size").build();

    public VoicemailContentTable(String tableName, Context context, SQLiteOpenHelper dbHelper, VoicemailTable.DelegateHelper contentProviderHelper, CallLogInsertionHelper callLogInsertionHelper) {
        this.mTableName = tableName;
        this.mContext = context;
        this.mDbHelper = dbHelper;
        this.mDelegateHelper = contentProviderHelper;
        this.mVoicemailProjectionMap = new ProjectionMap.Builder().add("_id").add("number").add("date").add("duration").add("is_read").add("transcription").add("state").add("source_data").add("source_package").add("has_content").add("mime_type").add("_data").add("_display_name", createDisplayName(context)).add("_size", "NULL").build();
        this.mCallLogInsertionHelper = callLogInsertionHelper;
    }

    private static String createDisplayName(Context context) {
        String prefix = context.getString(R.string.voicemail_from_column);
        return DatabaseUtils.sqlEscapeString(prefix) + " || number";
    }

    @Override
    public Uri insert(VoicemailContentProvider.UriData uriData, ContentValues values) {
        DbQueryUtils.checkForSupportedColumns(this.mVoicemailProjectionMap, values);
        ContentValues copiedValues = new ContentValues(values);
        checkInsertSupported(uriData);
        this.mDelegateHelper.checkAndAddSourcePackageIntoValues(uriData, copiedValues);
        this.mCallLogInsertionHelper.addComputedValues(copiedValues);
        copiedValues.put("_data", generateDataFile());
        copiedValues.put("type", (Integer) 4);
        if (!values.containsKey("new")) {
            copiedValues.put("new", (Integer) 1);
        }
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        long rowId = getDatabaseModifier(db).insert(this.mTableName, null, copiedValues);
        if (rowId <= 0) {
            return null;
        }
        Uri newUri = ContentUris.withAppendedId(uriData.getUri(), rowId);
        updateVoicemailUri(db, newUri);
        return newUri;
    }

    private void checkInsertSupported(VoicemailContentProvider.UriData uriData) {
        if (uriData.hasId()) {
            throw new UnsupportedOperationException(String.format("Cannot insert URI: %s. Inserted URIs should not contain an id.", uriData.getUri()));
        }
    }

    private String generateDataFile() {
        try {
            File dataDirectory = this.mContext.getDir("voicemail-data", 0);
            File voicemailFile = File.createTempFile("voicemail", "", dataDirectory);
            return voicemailFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("unable to create temp file", e);
        }
    }

    private void updateVoicemailUri(SQLiteDatabase db, Uri newUri) {
        ContentValues values = new ContentValues();
        values.put("voicemail_uri", newUri.toString());
        db.update(this.mTableName, values, VoicemailContentProvider.UriData.createUriData(newUri).getWhereClause(), null);
    }

    @Override
    public int delete(VoicemailContentProvider.UriData uriData, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause(), getCallTypeClause());
        Cursor cursor = null;
        try {
            cursor = query(uriData, FILENAME_ONLY_PROJECTION, selection, selectionArgs, null);
            while (cursor.moveToNext()) {
                String filename = cursor.getString(0);
                if (filename == null) {
                    Log.w("VoicemailContentProvider", "No filename for uri " + uriData.getUri() + ", cannot delete file");
                } else {
                    File file = new File(filename);
                    if (file.exists()) {
                        boolean success = file.delete();
                        if (!success) {
                            Log.e("VoicemailContentProvider", "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            CloseUtils.closeQuietly(cursor);
            return getDatabaseModifier(db).delete(this.mTableName, combinedClause, selectionArgs);
        } catch (Throwable th) {
            CloseUtils.closeQuietly(cursor);
            throw th;
        }
    }

    @Override
    public Cursor query(VoicemailContentProvider.UriData uriData, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(this.mTableName);
        qb.setProjectionMap(this.mVoicemailProjectionMap);
        qb.setStrict(true);
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause(), getCallTypeClause());
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, combinedClause, selectionArgs, null, null, sortOrder);
        if (c != null) {
            c.setNotificationUri(this.mContext.getContentResolver(), VoicemailContract.Voicemails.CONTENT_URI);
        }
        return c;
    }

    @Override
    public int update(VoicemailContentProvider.UriData uriData, ContentValues values, String selection, String[] selectionArgs) {
        DbQueryUtils.checkForSupportedColumns(ALLOWED_COLUMNS, values, "Updates are not allowed.");
        checkUpdateSupported(uriData);
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        String combinedClause = DbQueryUtils.concatenateClauses(selection, uriData.getWhereClause(), getCallTypeClause());
        return getDatabaseModifier(db).update(this.mTableName, values, combinedClause, selectionArgs);
    }

    private void checkUpdateSupported(VoicemailContentProvider.UriData uriData) {
        if (!uriData.hasId()) {
            throw new UnsupportedOperationException(String.format("Cannot update URI: %s.  Bulk update not supported", uriData.getUri()));
        }
    }

    @Override
    public String getType(VoicemailContentProvider.UriData uriData) {
        return uriData.hasId() ? "vnd.android.cursor.item/voicemail" : "vnd.android.cursor.dir/voicemails";
    }

    @Override
    public ParcelFileDescriptor openFile(VoicemailContentProvider.UriData uriData, String mode) throws FileNotFoundException {
        return this.mDelegateHelper.openDataFile(uriData, mode);
    }

    private String getCallTypeClause() {
        return DbQueryUtils.getEqualityClause("type", 4L);
    }

    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification(this.mTableName, db, this.mContext);
    }
}
