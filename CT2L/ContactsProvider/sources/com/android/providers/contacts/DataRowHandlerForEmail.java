package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForEmail extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForEmail(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/email_v2", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        String email = values.getAsString("data1");
        long dataId = super.insert(db, txContext, rawContactId, values);
        fixRawContactDisplayName(db, txContext, rawContactId);
        String address = this.mDbHelper.insertNameLookupForEmail(rawContactId, dataId, email);
        if (address != null) {
            triggerAggregation(txContext, rawContactId);
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        if (values.containsKey("data1")) {
            long dataId = c.getLong(0);
            long rawContactId = c.getLong(1);
            String address = values.getAsString("data1");
            this.mDbHelper.deleteNameLookup(dataId);
            this.mDbHelper.insertNameLookupForEmail(rawContactId, dataId, address);
            fixRawContactDisplayName(db, txContext, rawContactId);
            triggerAggregation(txContext, rawContactId);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(2);
        int count = super.delete(db, txContext, c);
        this.mDbHelper.deleteNameLookup(dataId);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return count;
    }

    @Override
    protected int getTypeRank(int type) {
        switch (type) {
            case 0:
                return 2;
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 3;
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
        builder.appendContentFromColumn("data1");
    }
}
