package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public abstract class DataRowHandler {
    private static final String[] HASH_INPUT_COLUMNS = {"data1", "data2"};
    protected final AbstractContactAggregator mContactAggregator;
    protected final Context mContext;
    protected final ContactsDatabaseHelper mDbHelper;
    protected final String mMimetype;
    protected long mMimetypeId;
    protected String[] mSelectionArgs1 = new String[1];

    public interface DataDeleteQuery {
        public static final String[] CONCRETE_COLUMNS = {"data._id", "mimetype", "raw_contact_id", "is_primary", "data1"};
        public static final String[] COLUMNS = {"_id", "mimetype", "raw_contact_id", "is_primary", "data1"};
    }

    public interface DataUpdateQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "mimetype"};
    }

    public DataRowHandler(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, String str) {
        this.mContext = context;
        this.mDbHelper = contactsDatabaseHelper;
        this.mContactAggregator = abstractContactAggregator;
        this.mMimetype = str;
    }

    protected long getMimeTypeId() {
        if (this.mMimetypeId == 0) {
            this.mMimetypeId = this.mDbHelper.getMimeTypeId(this.mMimetype);
        }
        return this.mMimetypeId;
    }

    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        handleHashIdForInsert(contentValues);
        long jInsert = sQLiteDatabase.insert("data", null, contentValues);
        Integer asInteger = contentValues.getAsInteger("is_primary");
        Integer asInteger2 = contentValues.getAsInteger("is_super_primary");
        if ((asInteger != null && asInteger.intValue() != 0) || (asInteger2 != null && asInteger2.intValue() != 0)) {
            long mimeTypeId = getMimeTypeId();
            this.mDbHelper.setIsPrimary(j, jInsert, mimeTypeId);
            transactionContext.markRawContactMetadataDirty(j, false);
            if (asInteger2 != null) {
                if (asInteger2.intValue() != 0) {
                    this.mDbHelper.setIsSuperPrimary(j, jInsert, mimeTypeId);
                } else {
                    this.mDbHelper.clearSuperPrimary(j, mimeTypeId);
                }
            } else if (this.mDbHelper.rawContactHasSuperPrimary(j, mimeTypeId)) {
                this.mDbHelper.setIsSuperPrimary(j, jInsert, mimeTypeId);
            }
        }
        if (containsSearchableColumns(contentValues)) {
            transactionContext.invalidateSearchIndexForRawContact(j);
        }
        return jInsert;
    }

    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        long j;
        long j2 = cursor.getLong(0);
        long j3 = cursor.getLong(1);
        handlePrimaryAndSuperPrimary(transactionContext, contentValues, j2, j3, z2);
        handleHashIdForUpdate(contentValues, j2);
        if (contentValues.size() > 0) {
            this.mSelectionArgs1[0] = String.valueOf(j2);
            sQLiteDatabase.update("data", contentValues, "_id =?", this.mSelectionArgs1);
        }
        if (containsSearchableColumns(contentValues)) {
            j = j3;
            transactionContext.invalidateSearchIndexForRawContact(j);
        } else {
            j = j3;
        }
        transactionContext.markRawContactDirtyAndChanged(j, z);
        return true;
    }

    public boolean hasSearchableData() {
        return false;
    }

    public boolean containsSearchableColumns(ContentValues contentValues) {
        return false;
    }

    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
    }

    public void handleHashIdForInsert(ContentValues contentValues) {
        String asString = contentValues.getAsString("data1");
        String asString2 = contentValues.getAsString("data2");
        String photoHashId = this.mDbHelper.getPhotoHashId();
        if (!"vnd.android.cursor.item/photo".equals(this.mMimetype)) {
            if (!TextUtils.isEmpty(asString) || !TextUtils.isEmpty(asString2)) {
                photoHashId = this.mDbHelper.generateHashId(asString, asString2);
            } else {
                photoHashId = null;
            }
        }
        if (TextUtils.isEmpty(photoHashId)) {
            contentValues.putNull("hash_id");
        } else {
            contentValues.put("hash_id", photoHashId);
        }
    }

    private void handleHashIdForUpdate(ContentValues contentValues, long j) {
        if (!"vnd.android.cursor.item/photo".equals(this.mMimetype)) {
            if (contentValues.containsKey("data1") || contentValues.containsKey("data2")) {
                String asString = contentValues.getAsString("data1");
                String asString2 = contentValues.getAsString("data2");
                this.mSelectionArgs1[0] = String.valueOf(j);
                Cursor cursorQuery = this.mDbHelper.getReadableDatabase().query("data", HASH_INPUT_COLUMNS, "_id=?", this.mSelectionArgs1, null, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        if (!contentValues.containsKey("data1")) {
                            asString = cursorQuery.getString(0);
                        }
                        if (!contentValues.containsKey("data2")) {
                            asString2 = cursorQuery.getString(1);
                        }
                    }
                    cursorQuery.close();
                    String strGenerateHashId = this.mDbHelper.generateHashId(asString, asString2);
                    if (TextUtils.isEmpty(strGenerateHashId)) {
                        contentValues.putNull("hash_id");
                    } else {
                        contentValues.put("hash_id", strGenerateHashId);
                    }
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
        }
    }

    private void handlePrimaryAndSuperPrimary(TransactionContext transactionContext, ContentValues contentValues, long j, long j2, boolean z) {
        boolean z2 = contentValues.getAsInteger("is_primary") != null;
        boolean z3 = contentValues.getAsInteger("is_super_primary") != null;
        if (z2 || z3) {
            transactionContext.markRawContactMetadataDirty(j2, z);
            long mimeTypeId = getMimeTypeId();
            boolean z4 = z2 && contentValues.getAsInteger("is_primary").intValue() == 0;
            boolean z5 = z3 && contentValues.getAsInteger("is_super_primary").intValue() == 0;
            if (z4 || z5) {
                this.mSelectionArgs1[0] = String.valueOf(j);
                Cursor cursorQuery = this.mDbHelper.getReadableDatabase().query("data", new String[]{"is_primary", "is_super_primary"}, "_id=?", this.mSelectionArgs1, null, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        boolean z6 = cursorQuery.getInt(0) != 0;
                        if (cursorQuery.getInt(1) == 0) {
                            z = false;
                        }
                        if (z) {
                            this.mDbHelper.clearSuperPrimary(j2, mimeTypeId);
                        }
                        if (z4 && z6) {
                            this.mDbHelper.setIsPrimary(j2, -1L, mimeTypeId);
                        }
                    }
                } finally {
                    cursorQuery.close();
                }
            } else {
                boolean z7 = z2 && contentValues.getAsInteger("is_primary").intValue() != 0;
                if (z3 && contentValues.getAsInteger("is_super_primary").intValue() != 0) {
                    this.mDbHelper.setIsSuperPrimary(j2, j, mimeTypeId);
                    this.mDbHelper.setIsPrimary(j2, j, mimeTypeId);
                } else if (z7) {
                    if (this.mDbHelper.rawContactHasSuperPrimary(j2, mimeTypeId)) {
                        this.mDbHelper.setIsSuperPrimary(j2, j, mimeTypeId);
                    }
                    this.mDbHelper.setIsPrimary(j2, j, mimeTypeId);
                }
            }
            contentValues.remove("is_super_primary");
            contentValues.remove("is_primary");
        }
    }

    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(2);
        boolean z = cursor.getInt(3) != 0;
        this.mSelectionArgs1[0] = String.valueOf(j);
        int iDelete = sQLiteDatabase.delete("data", "_id=?", this.mSelectionArgs1);
        this.mSelectionArgs1[0] = String.valueOf(j2);
        sQLiteDatabase.delete("presence", "presence_raw_contact_id=?", this.mSelectionArgs1);
        if (iDelete != 0 && z) {
            fixPrimary(sQLiteDatabase, j2);
            transactionContext.markRawContactMetadataDirty(j2, false);
        }
        if (hasSearchableData()) {
            transactionContext.invalidateSearchIndexForRawContact(j2);
        }
        return iDelete;
    }

    private void fixPrimary(SQLiteDatabase sQLiteDatabase, long j) {
        long mimeTypeId = getMimeTypeId();
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("data JOIN mimetypes ON (data.mimetype_id = mimetypes._id)", DataDeleteQuery.CONCRETE_COLUMNS, "raw_contact_id=? AND mimetype_id=" + mimeTypeId, this.mSelectionArgs1, null, null, null);
        int i = -1;
        long j2 = -1L;
        while (cursorQuery.moveToNext()) {
            try {
                long j3 = cursorQuery.getLong(0);
                int i2 = cursorQuery.getInt(4);
                if (i == -1 || getTypeRank(i2) < getTypeRank(i)) {
                    j2 = j3;
                    i = i2;
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        if (j2 != -1) {
            this.mDbHelper.setIsPrimary(j, j2, mimeTypeId);
        }
    }

    protected int getTypeRank(int i) {
        return 0;
    }

    protected void fixRawContactDisplayName(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j) {
        if (!isNewRawContact(transactionContext, j)) {
            this.mDbHelper.updateRawContactDisplayName(sQLiteDatabase, j);
            this.mContactAggregator.updateDisplayNameForRawContact(sQLiteDatabase, j);
        }
    }

    private boolean isNewRawContact(TransactionContext transactionContext, long j) {
        return transactionContext.isNewRawContact(j);
    }

    public ContentValues getAugmentedValues(SQLiteDatabase sQLiteDatabase, long j, ContentValues contentValues) {
        boolean z;
        String string;
        ContentValues contentValues2 = new ContentValues();
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("data", null, "_id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                z = false;
                for (int i = 0; i < cursorQuery.getColumnCount(); i++) {
                    String columnName = cursorQuery.getColumnName(i);
                    String string2 = cursorQuery.getString(i);
                    if (!z && contentValues.containsKey(columnName)) {
                        Object obj = contentValues.get(columnName);
                        if (obj != null) {
                            string = obj.toString();
                        } else {
                            string = null;
                        }
                        z |= !TextUtils.equals(string, string2);
                    }
                    contentValues2.put(columnName, string2);
                }
            } else {
                z = false;
            }
            if (!z) {
                return null;
            }
            contentValues2.putAll(contentValues);
            return contentValues2;
        } finally {
            cursorQuery.close();
        }
    }

    public void triggerAggregation(TransactionContext transactionContext, long j) {
        this.mContactAggregator.triggerAggregation(transactionContext, j);
    }

    public boolean areAllEmpty(ContentValues contentValues, String[] strArr) {
        for (String str : strArr) {
            if (!TextUtils.isEmpty(contentValues.getAsString(str))) {
                return false;
            }
        }
        return true;
    }

    public boolean areAnySpecified(ContentValues contentValues, String[] strArr) {
        for (String str : strArr) {
            if (contentValues.containsKey(str)) {
                return true;
            }
        }
        return false;
    }
}
