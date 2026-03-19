package com.mediatek.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForImsCall extends DataRowHandler {
    public DataRowHandlerForImsCall(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/ims");
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        indexBuilder.appendContentFromColumn("data1");
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        if (contentValues.containsKey("data1") && !PhoneNumberUtils.isUriNumber(contentValues.getAsString("data1"))) {
            contentValues.put("data1", PhoneNumberUtils.normalizeNumber(contentValues.getAsString("data1")));
        }
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        if (contentValues.containsKey("data1")) {
            updatePhoneLookup(sQLiteDatabase, j, jInsert, contentValues.getAsString("data1"));
        }
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        if (contentValues.containsKey("data1") && !PhoneNumberUtils.isUriNumber(contentValues.getAsString("data1"))) {
            contentValues.put("data1", PhoneNumberUtils.normalizeNumber(contentValues.getAsString("data1")));
        }
        if (!super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2)) {
            return false;
        }
        if (contentValues.containsKey("data1")) {
            updatePhoneLookup(sQLiteDatabase, cursor.getLong(1), cursor.getLong(0), contentValues.getAsString("data1"));
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        updatePhoneLookup(sQLiteDatabase, cursor.getLong(2), cursor.getLong(0), null);
        return iDelete;
    }

    private void updatePhoneLookup(SQLiteDatabase sQLiteDatabase, long j, long j2, String str) {
        this.mSelectionArgs1[0] = String.valueOf(j2);
        sQLiteDatabase.delete("phone_lookup", "data_id=?", this.mSelectionArgs1);
        if (!TextUtils.isEmpty(str) && PhoneNumberUtils.isGlobalPhoneNumber(str)) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("raw_contact_id", Long.valueOf(j));
            contentValues.put("data_id", Long.valueOf(j2));
            contentValues.put("normalized_number", str);
            contentValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(str));
            sQLiteDatabase.insert("phone_lookup", null, contentValues);
        }
    }
}
