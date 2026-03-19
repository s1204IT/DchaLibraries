package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForPhoneNumber extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForPhoneNumber(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/phone_v2", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        fillNormalizedNumber(contentValues);
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        if (contentValues.containsKey("data1")) {
            updatePhoneLookup(sQLiteDatabase, j, jInsert, contentValues.getAsString("data1"), contentValues.getAsString("data4"));
            this.mContactAggregator.updateHasPhoneNumber(sQLiteDatabase, j);
            fixRawContactDisplayName(sQLiteDatabase, transactionContext, j);
            triggerAggregation(transactionContext, j);
        }
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        fillNormalizedNumber(contentValues);
        if (!super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2)) {
            return false;
        }
        if (contentValues.containsKey("data1")) {
            long j = cursor.getLong(0);
            long j2 = cursor.getLong(1);
            updatePhoneLookup(sQLiteDatabase, j2, j, contentValues.getAsString("data1"), contentValues.getAsString("data4"));
            this.mContactAggregator.updateHasPhoneNumber(sQLiteDatabase, j2);
            fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
            triggerAggregation(transactionContext, j2);
        }
        return true;
    }

    private void fillNormalizedNumber(ContentValues contentValues) {
        if (!contentValues.containsKey("data1")) {
            contentValues.remove("data4");
            return;
        }
        String asString = contentValues.getAsString("data1");
        String asString2 = contentValues.getAsString("data4");
        if (asString != null && asString2 == null) {
            contentValues.put("data4", PhoneNumberUtils.formatNumberToE164(asString, this.mDbHelper.getCurrentCountryIso()));
        }
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(2);
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        updatePhoneLookup(sQLiteDatabase, j2, j, null, null);
        this.mContactAggregator.updateHasPhoneNumber(sQLiteDatabase, j2);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        triggerAggregation(transactionContext, j2);
        return iDelete;
    }

    private void updatePhoneLookup(SQLiteDatabase sQLiteDatabase, long j, long j2, String str, String str2) {
        this.mSelectionArgs1[0] = String.valueOf(j2);
        sQLiteDatabase.delete("phone_lookup", "data_id=?", this.mSelectionArgs1);
        if (str != null) {
            String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(str);
            if (!TextUtils.isEmpty(strNormalizeNumber)) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("raw_contact_id", Long.valueOf(j));
                contentValues.put("data_id", Long.valueOf(j2));
                contentValues.put("normalized_number", strNormalizeNumber);
                contentValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(strNormalizeNumber));
                sQLiteDatabase.insert("phone_lookup", null, contentValues);
                if (str2 != null && !str2.equals(strNormalizeNumber)) {
                    contentValues.put("normalized_number", str2);
                    contentValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(str2));
                    sQLiteDatabase.insert("phone_lookup", null, contentValues);
                }
            }
        }
    }

    @Override
    protected int getTypeRank(int i) {
        switch (i) {
            case 0:
                return 4;
            case 1:
                return 2;
            case 2:
                return 0;
            case 3:
                return 1;
            case 4:
                return 6;
            case 5:
                return 7;
            case 6:
                return 3;
            case 7:
                return 5;
            default:
                return 1000;
        }
    }

    @Override
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        String string = indexBuilder.getString("data1");
        if (TextUtils.isEmpty(string)) {
            return;
        }
        String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(string);
        if (TextUtils.isEmpty(strNormalizeNumber)) {
            return;
        }
        indexBuilder.appendToken(strNormalizeNumber);
        String numberToE164 = PhoneNumberUtils.formatNumberToE164(string, this.mDbHelper.getCurrentCountryIso());
        if (numberToE164 != null && !numberToE164.equals(strNormalizeNumber)) {
            indexBuilder.appendToken(numberToE164);
        }
    }
}
