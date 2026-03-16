package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForPhoneNumber extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForPhoneNumber(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/phone_v2", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        fillNormalizedNumber(values);
        long dataId = super.insert(db, txContext, rawContactId, values);
        if (values.containsKey("data1")) {
            String number = values.getAsString("data1");
            String normalizedNumber = values.getAsString("data4");
            updatePhoneLookup(db, rawContactId, dataId, number, normalizedNumber);
            this.mContactAggregator.updateHasPhoneNumber(db, rawContactId);
            fixRawContactDisplayName(db, txContext, rawContactId);
            triggerAggregation(txContext, rawContactId);
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        fillNormalizedNumber(values);
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        if (values.containsKey("data1")) {
            long dataId = c.getLong(0);
            long rawContactId = c.getLong(1);
            updatePhoneLookup(db, rawContactId, dataId, values.getAsString("data1"), values.getAsString("data4"));
            this.mContactAggregator.updateHasPhoneNumber(db, rawContactId);
            fixRawContactDisplayName(db, txContext, rawContactId);
            triggerAggregation(txContext, rawContactId);
        }
        return true;
    }

    private void fillNormalizedNumber(ContentValues values) {
        if (!values.containsKey("data1")) {
            values.remove("data4");
            return;
        }
        String number = values.getAsString("data1");
        String numberE164 = values.getAsString("data4");
        if (number != null && numberE164 == null) {
            String newNumberE164 = PhoneNumberUtils.formatNumberToE164(number, this.mDbHelper.getCurrentCountryIso());
            values.put("data4", newNumberE164);
        }
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(2);
        int count = super.delete(db, txContext, c);
        updatePhoneLookup(db, rawContactId, dataId, null, null);
        this.mContactAggregator.updateHasPhoneNumber(db, rawContactId);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return count;
    }

    private void updatePhoneLookup(SQLiteDatabase db, long rawContactId, long dataId, String number, String numberE164) {
        this.mSelectionArgs1[0] = String.valueOf(dataId);
        db.delete("phone_lookup", "data_id=?", this.mSelectionArgs1);
        if (number != null) {
            String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
            if (!TextUtils.isEmpty(normalizedNumber)) {
                ContentValues phoneValues = new ContentValues();
                phoneValues.put("raw_contact_id", Long.valueOf(rawContactId));
                phoneValues.put("data_id", Long.valueOf(dataId));
                phoneValues.put("normalized_number", normalizedNumber);
                phoneValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
                db.insert("phone_lookup", null, phoneValues);
                if (numberE164 != null && !numberE164.equals(normalizedNumber)) {
                    phoneValues.put("normalized_number", numberE164);
                    phoneValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(numberE164));
                    db.insert("phone_lookup", null, phoneValues);
                }
            }
        }
    }

    @Override
    protected int getTypeRank(int type) {
        switch (type) {
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
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
        String number = builder.getString("data1");
        if (!TextUtils.isEmpty(number)) {
            String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
            if (!TextUtils.isEmpty(normalizedNumber)) {
                builder.appendToken(normalizedNumber);
                String numberE164 = PhoneNumberUtils.formatNumberToE164(number, this.mDbHelper.getCurrentCountryIso());
                if (numberE164 != null && !numberE164.equals(normalizedNumber)) {
                    builder.appendToken(numberE164);
                }
            }
        }
    }
}
