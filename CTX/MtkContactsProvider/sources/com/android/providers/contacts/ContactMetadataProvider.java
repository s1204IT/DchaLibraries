package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;
import com.android.common.content.ProjectionMap;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.UserUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ContactMetadataProvider extends ContentProvider {
    private static final Map<String, String> sMetadataProjectionMap;
    private static final Map<String, String> sSyncStateProjectionMap;
    private String mAllowedPackage;
    private ContactsProvider2 mContactsProvider;
    private ContactsDatabaseHelper mDbHelper;
    private static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactMetadata", 2);
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);

    static {
        sURIMatcher.addURI("com.android.contacts.metadata", "metadata_sync", 1);
        sURIMatcher.addURI("com.android.contacts.metadata", "metadata_sync/#", 2);
        sURIMatcher.addURI("com.android.contacts.metadata", "metadata_sync_state", 3);
        sMetadataProjectionMap = ProjectionMap.builder().add("_id").add("raw_contact_backup_id").add("account_type").add("account_name").add("data_set").add("data").add("deleted").build();
        sSyncStateProjectionMap = ProjectionMap.builder().add("_id").add("account_type").add("account_name").add("data_set").add("state").build();
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        this.mDbHelper = getDatabaseHelper(context);
        this.mContactsProvider = (ContactsProvider2) ContentProvider.coerceToLocalContentProvider(context.getContentResolver().acquireProvider("com.android.contacts"));
        this.mAllowedPackage = getContext().getResources().getString(R.string.metadata_sync_pacakge);
        return true;
    }

    protected ContactsDatabaseHelper getDatabaseHelper(Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    protected void setDatabaseHelper(ContactsDatabaseHelper contactsDatabaseHelper) {
        this.mDbHelper = contactsDatabaseHelper;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        ensureCaller();
        if (VERBOSE_LOGGING) {
            StringBuilder sb = new StringBuilder();
            sb.append("query: uri=");
            sb.append(uri);
            sb.append("  projection=");
            sb.append(Arrays.toString(strArr));
            sb.append("  selection=[");
            sb.append(str);
            sb.append("]  args=");
            sb.append(Arrays.toString(strArr2));
            sb.append("  order=[");
            str3 = str2;
            sb.append(str3);
            sb.append("] CPID=");
            sb.append(Binder.getCallingPid());
            sb.append(" User=");
            sb.append(UserUtils.getCurrentUserHandle(getContext()));
            Log.v("ContactMetadata", sb.toString());
        } else {
            str3 = str2;
        }
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        String limit = ContactsProvider2.getLimit(uri);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        switch (sURIMatcher.match(uri)) {
            case 1:
                setTablesAndProjectionMapForMetadata(sQLiteQueryBuilder);
                break;
            case 2:
                setTablesAndProjectionMapForMetadata(sQLiteQueryBuilder);
                selectionBuilder.addClause(DbQueryUtils.getEqualityClause("_id", ContentUris.parseId(uri)));
                break;
            case 3:
                setTablesAndProjectionMapForSyncState(sQLiteQueryBuilder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        return sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, selectionBuilder.build(), strArr2, null, null, str3, limit);
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case 1:
                return "vnd.android.cursor.dir/contact_metadata";
            case 2:
                return "vnd.android.cursor.item/contact_metadata";
            case 3:
                return "vnd.android.cursor.dir/contact_metadata_sync_state";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        ensureCaller();
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            int iMatch = sURIMatcher.match(uri);
            if (iMatch == 1) {
                long jUpdateOrInsertDataToMetadataSync = updateOrInsertDataToMetadataSync(writableDatabase, uri, contentValues);
                writableDatabase.setTransactionSuccessful();
                return ContentUris.withAppendedId(uri, jUpdateOrInsertDataToMetadataSync);
            }
            if (iMatch == 3) {
                replaceAccountInfoByAccountId(uri, contentValues);
                Long lValueOf = Long.valueOf(writableDatabase.replace("metadata_sync_state", "account_id", contentValues));
                writableDatabase.setTransactionSuccessful();
                return ContentUris.withAppendedId(uri, lValueOf.longValue());
            }
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Calling contact metadata insert on an unknown/invalid URI", uri));
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        Cursor cursorQuery;
        ensureCaller();
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            int iMatch = sURIMatcher.match(uri);
            if (iMatch == 1) {
                cursorQuery = writableDatabase.query("view_metadata_sync", new String[]{"_id"}, str, strArr, null, null, null);
                int iDelete = 0;
                while (cursorQuery.moveToNext()) {
                    try {
                        iDelete += writableDatabase.delete("metadata_sync", "_id=" + cursorQuery.getLong(0), null);
                    } finally {
                    }
                }
                cursorQuery.close();
                writableDatabase.setTransactionSuccessful();
                return iDelete;
            }
            if (iMatch == 3) {
                cursorQuery = writableDatabase.query("view_metadata_sync_state", new String[]{"_id"}, str, strArr, null, null, null);
                int iDelete2 = 0;
                while (cursorQuery.moveToNext()) {
                    try {
                        iDelete2 += writableDatabase.delete("metadata_sync_state", "_id=" + cursorQuery.getLong(0), null);
                    } finally {
                    }
                }
                cursorQuery.close();
                writableDatabase.setTransactionSuccessful();
                return iDelete2;
            }
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Calling contact metadata delete on an unknown/invalid URI", uri));
        } finally {
        }
        writableDatabase.endTransaction();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        ensureCaller();
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            if (sURIMatcher.match(uri) == 3) {
                Long lReplaceAccountInfoByAccountId = replaceAccountInfoByAccountId(uri, contentValues);
                if (lReplaceAccountInfoByAccountId == null) {
                    throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Invalid identifier is found for accountId", uri));
                }
                contentValues.put("account_id", lReplaceAccountInfoByAccountId);
                writableDatabase.replace("metadata_sync_state", null, contentValues);
                writableDatabase.setTransactionSuccessful();
                return 1;
            }
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Calling contact metadata update on an unknown/invalid URI", uri));
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        ensureCaller();
        if (VERBOSE_LOGGING) {
            Log.v("ContactMetadata", "applyBatch: " + arrayList.size() + " ops");
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            ContentProviderResult[] contentProviderResultArrApplyBatch = super.applyBatch(arrayList);
            writableDatabase.setTransactionSuccessful();
            return contentProviderResultArrApplyBatch;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        ensureCaller();
        if (VERBOSE_LOGGING) {
            Log.v("ContactMetadata", "bulkInsert: " + contentValuesArr.length + " inserts");
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            int iBulkInsert = super.bulkInsert(uri, contentValuesArr);
            writableDatabase.setTransactionSuccessful();
            return iBulkInsert;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private void setTablesAndProjectionMapForMetadata(SQLiteQueryBuilder sQLiteQueryBuilder) {
        sQLiteQueryBuilder.setTables("view_metadata_sync");
        sQLiteQueryBuilder.setProjectionMap(sMetadataProjectionMap);
        sQLiteQueryBuilder.setStrict(true);
    }

    private void setTablesAndProjectionMapForSyncState(SQLiteQueryBuilder sQLiteQueryBuilder) {
        sQLiteQueryBuilder.setTables("view_metadata_sync_state");
        sQLiteQueryBuilder.setProjectionMap(sSyncStateProjectionMap);
        sQLiteQueryBuilder.setStrict(true);
    }

    private long updateOrInsertDataToMetadataSync(SQLiteDatabase sQLiteDatabase, Uri uri, ContentValues contentValues) throws Throwable {
        if (sURIMatcher.match(uri) != 1) {
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Calling contact metadata insert or update on an unknown/invalid URI", uri));
        }
        Integer asInteger = contentValues.getAsInteger("deleted");
        if (asInteger != null && asInteger.intValue() != 0) {
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Cannot insert or update deleted metadata:" + contentValues.toString(), uri));
        }
        String asString = contentValues.getAsString("data");
        if (TextUtils.isEmpty(asString)) {
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Data column cannot be empty.", uri));
        }
        Long lReplaceAccountInfoByAccountId = replaceAccountInfoByAccountId(uri, contentValues);
        String asString2 = contentValues.getAsString("raw_contact_backup_id");
        if (lReplaceAccountInfoByAccountId == null) {
            return 0L;
        }
        if (asString2 != null) {
            long jUpsertMetadataSync = this.mDbHelper.upsertMetadataSync(asString2, lReplaceAccountInfoByAccountId, asString, 0);
            if (jUpsertMetadataSync <= 0) {
                throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Metadata upsertion failed. Values= " + contentValues.toString(), uri));
            }
            this.mContactsProvider.updateFromMetaDataEntry(sQLiteDatabase, MetadataEntryParser.parseDataToMetaDataEntry(asString));
            return jUpsertMetadataSync;
        }
        throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Invalid identifier is found: accountId=" + lReplaceAccountInfoByAccountId + "; rawContactBackupId=" + asString2, uri));
    }

    private Long replaceAccountInfoByAccountId(Uri uri, ContentValues contentValues) {
        String asString = contentValues.getAsString("account_name");
        String asString2 = contentValues.getAsString("account_type");
        String asString3 = contentValues.getAsString("data_set");
        if (TextUtils.isEmpty(asString) ^ TextUtils.isEmpty(asString2)) {
            throw new IllegalArgumentException(this.mDbHelper.exceptionMessage("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
        }
        Long accountIdOrNull = this.mDbHelper.getAccountIdOrNull(AccountWithDataSet.get(asString, asString2, asString3));
        if (accountIdOrNull == null) {
            return null;
        }
        contentValues.put("account_id", accountIdOrNull);
        contentValues.remove("account_name");
        contentValues.remove("account_type");
        contentValues.remove("data_set");
        return accountIdOrNull;
    }

    void ensureCaller() {
        String callingPackage = getCallingPackage();
        if (this.mAllowedPackage.equals(callingPackage)) {
            return;
        }
        throw new SecurityException("Caller " + callingPackage + " can't access ContactMetadataProvider");
    }
}
