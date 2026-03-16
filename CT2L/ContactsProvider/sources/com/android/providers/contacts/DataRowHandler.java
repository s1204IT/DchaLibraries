package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public abstract class DataRowHandler {
    protected final ContactAggregator mContactAggregator;
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

    public DataRowHandler(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, String mimetype) {
        this.mContext = context;
        this.mDbHelper = dbHelper;
        this.mContactAggregator = aggregator;
        this.mMimetype = mimetype;
    }

    protected long getMimeTypeId() {
        if (this.mMimetypeId == 0) {
            this.mMimetypeId = this.mDbHelper.getMimeTypeId(this.mMimetype);
        }
        return this.mMimetypeId;
    }

    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        long dataId = db.insert("data", null, values);
        Integer primary = values.getAsInteger("is_primary");
        Integer superPrimary = values.getAsInteger("is_super_primary");
        if ((primary != null && primary.intValue() != 0) || (superPrimary != null && superPrimary.intValue() != 0)) {
            long mimeTypeId = getMimeTypeId();
            this.mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);
            if (superPrimary != null) {
                if (superPrimary.intValue() != 0) {
                    this.mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                } else {
                    this.mDbHelper.clearSuperPrimary(rawContactId, mimeTypeId);
                }
            } else if (this.mDbHelper.rawContactHasSuperPrimary(rawContactId, mimeTypeId)) {
                this.mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
            }
        }
        if (containsSearchableColumns(values)) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }
        return dataId;
    }

    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(1);
        handlePrimaryAndSuperPrimary(values, dataId, rawContactId);
        if (values.size() > 0) {
            this.mSelectionArgs1[0] = String.valueOf(dataId);
            db.update("data", values, "_id =?", this.mSelectionArgs1);
        }
        if (containsSearchableColumns(values)) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }
        txContext.markRawContactDirtyAndChanged(rawContactId, callerIsSyncAdapter);
        return true;
    }

    public boolean hasSearchableData() {
        return false;
    }

    public boolean containsSearchableColumns(ContentValues values) {
        return false;
    }

    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
    }

    private void handlePrimaryAndSuperPrimary(ContentValues values, long dataId, long rawContactId) {
        boolean hasPrimary = values.getAsInteger("is_primary") != null;
        boolean hasSuperPrimary = values.getAsInteger("is_super_primary") != null;
        if (hasPrimary || hasSuperPrimary) {
            long mimeTypeId = getMimeTypeId();
            boolean clearPrimary = hasPrimary && values.getAsInteger("is_primary").intValue() == 0;
            boolean clearSuperPrimary = hasSuperPrimary && values.getAsInteger("is_super_primary").intValue() == 0;
            if (clearPrimary || clearSuperPrimary) {
                this.mSelectionArgs1[0] = String.valueOf(dataId);
                String[] cols = {"is_primary", "is_super_primary"};
                Cursor c = this.mDbHelper.getReadableDatabase().query("data", cols, "_id=?", this.mSelectionArgs1, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        boolean isPrimary = c.getInt(0) != 0;
                        boolean isSuperPrimary = c.getInt(1) != 0;
                        if (isSuperPrimary) {
                            this.mDbHelper.clearSuperPrimary(rawContactId, mimeTypeId);
                        }
                        if (clearPrimary && isPrimary) {
                            this.mDbHelper.setIsPrimary(rawContactId, -1L, mimeTypeId);
                        }
                    }
                } finally {
                    c.close();
                }
            } else {
                boolean setPrimary = hasPrimary && values.getAsInteger("is_primary").intValue() != 0;
                boolean setSuperPrimary = hasSuperPrimary && values.getAsInteger("is_super_primary").intValue() != 0;
                if (setSuperPrimary) {
                    this.mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                    this.mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);
                } else if (setPrimary) {
                    if (this.mDbHelper.rawContactHasSuperPrimary(rawContactId, mimeTypeId)) {
                        this.mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                    }
                    this.mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);
                }
            }
            values.remove("is_super_primary");
            values.remove("is_primary");
        }
    }

    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(2);
        boolean primary = c.getInt(3) != 0;
        this.mSelectionArgs1[0] = String.valueOf(dataId);
        int count = db.delete("data", "_id=?", this.mSelectionArgs1);
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        db.delete("presence", "presence_raw_contact_id=?", this.mSelectionArgs1);
        if (count != 0 && primary) {
            fixPrimary(db, rawContactId);
        }
        if (hasSearchableData()) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }
        return count;
    }

    private void fixPrimary(SQLiteDatabase db, long rawContactId) {
        long mimeTypeId = getMimeTypeId();
        int primaryType = -1;
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query("data JOIN mimetypes ON (data.mimetype_id = mimetypes._id)", DataDeleteQuery.CONCRETE_COLUMNS, "raw_contact_id=? AND mimetype_id=" + mimeTypeId, this.mSelectionArgs1, null, null, null);
        long primaryId = -1;
        while (c.moveToNext()) {
            try {
                long dataId = c.getLong(0);
                int type = c.getInt(4);
                if (primaryType == -1 || getTypeRank(type) < getTypeRank(primaryType)) {
                    primaryId = dataId;
                    primaryType = type;
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        if (primaryId != -1) {
            this.mDbHelper.setIsPrimary(rawContactId, primaryId, mimeTypeId);
        }
    }

    protected int getTypeRank(int type) {
        return 0;
    }

    protected void fixRawContactDisplayName(SQLiteDatabase db, TransactionContext txContext, long rawContactId) throws Throwable {
        if (!isNewRawContact(txContext, rawContactId)) {
            this.mDbHelper.updateRawContactDisplayName(db, rawContactId);
            this.mContactAggregator.updateDisplayNameForRawContact(db, rawContactId);
        }
    }

    private boolean isNewRawContact(TransactionContext txContext, long rawContactId) {
        return txContext.isNewRawContact(rawContactId);
    }

    public ContentValues getAugmentedValues(SQLiteDatabase db, long dataId, ContentValues update) {
        boolean changing = false;
        ContentValues values = new ContentValues();
        this.mSelectionArgs1[0] = String.valueOf(dataId);
        Cursor cursor = db.query("data", null, "_id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String key = cursor.getColumnName(i);
                    String value = cursor.getString(i);
                    if (!changing && update.containsKey(key)) {
                        Object newValue = update.get(key);
                        String newString = newValue == null ? null : newValue.toString();
                        changing |= !TextUtils.equals(newString, value);
                    }
                    values.put(key, value);
                }
            }
            if (!changing) {
                return null;
            }
            values.putAll(update);
            return values;
        } finally {
            cursor.close();
        }
    }

    public void triggerAggregation(TransactionContext txContext, long rawContactId) {
        this.mContactAggregator.triggerAggregation(txContext, rawContactId);
    }

    public boolean areAllEmpty(ContentValues values, String[] keys) {
        for (String key : keys) {
            if (!TextUtils.isEmpty(values.getAsString(key))) {
                return false;
            }
        }
        return true;
    }

    public boolean areAnySpecified(ContentValues values, String[] keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
