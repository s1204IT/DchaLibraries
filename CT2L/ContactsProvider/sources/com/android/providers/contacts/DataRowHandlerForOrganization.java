package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForOrganization extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForOrganization(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/organization", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        values.getAsString("data1");
        values.getAsString("data4");
        long dataId = super.insert(db, txContext, rawContactId, values);
        fixRawContactDisplayName(db, txContext, rawContactId);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        boolean containsCompany = values.containsKey("data1");
        boolean containsTitle = values.containsKey("data4");
        if (containsCompany || containsTitle) {
            long dataId = c.getLong(0);
            long rawContactId = c.getLong(1);
            if (containsCompany) {
                values.getAsString("data1");
            } else {
                this.mSelectionArgs1[0] = String.valueOf(dataId);
                DatabaseUtils.stringForQuery(db, "SELECT data1 FROM data WHERE _id=?", this.mSelectionArgs1);
            }
            if (containsTitle) {
                values.getAsString("data4");
            } else {
                this.mSelectionArgs1[0] = String.valueOf(dataId);
                DatabaseUtils.stringForQuery(db, "SELECT data4 FROM data WHERE _id=?", this.mSelectionArgs1);
            }
            this.mDbHelper.deleteNameLookup(dataId);
            fixRawContactDisplayName(db, txContext, rawContactId);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(2);
        int count = super.delete(db, txContext, c);
        fixRawContactDisplayName(db, txContext, rawContactId);
        this.mDbHelper.deleteNameLookup(dataId);
        return count;
    }

    @Override
    protected int getTypeRank(int type) {
        switch (type) {
            case 0:
                return 1;
            case 1:
                return 0;
            case 2:
                return 2;
            default:
                return 1000;
        }
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey("data1") || values.containsKey("data5") || values.containsKey("data6") || values.containsKey("data9") || values.containsKey("data8") || values.containsKey("data7") || values.containsKey("data4");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
        builder.appendNameFromColumn("data4");
        builder.appendNameFromColumn("data1");
        builder.appendContentFromColumn("data4");
        builder.appendContentFromColumn("data1", 3);
        builder.appendContentFromColumn("data8", 1);
        builder.appendContentFromColumn("data7", 1);
        builder.appendContentFromColumn("data5", 2);
        builder.appendContentFromColumn("data9", 2);
        builder.appendContentFromColumn("data6", 2);
    }
}
