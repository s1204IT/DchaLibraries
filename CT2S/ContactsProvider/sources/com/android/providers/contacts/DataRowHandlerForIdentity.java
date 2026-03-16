package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForIdentity extends DataRowHandler {
    public DataRowHandlerForIdentity(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/identity");
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        long dataId = super.insert(db, txContext, rawContactId, values);
        if (values.containsKey("data1") || values.containsKey("data2")) {
            triggerAggregation(txContext, rawContactId);
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        super.update(db, txContext, values, c, callerIsSyncAdapter);
        long rawContactId = c.getLong(1);
        if (values.containsKey("data1") || values.containsKey("data2")) {
            triggerAggregation(txContext, rawContactId);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        int count = super.delete(db, txContext, c);
        long rawContactId = c.getLong(1);
        triggerAggregation(txContext, rawContactId);
        return count;
    }
}
