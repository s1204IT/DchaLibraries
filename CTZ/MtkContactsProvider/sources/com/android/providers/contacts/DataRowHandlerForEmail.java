package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForEmail extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForEmail(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/email_v2", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        String asString = contentValues.getAsString("data1");
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j);
        if (this.mDbHelper.insertNameLookupForEmail(j, jInsert, asString) != null) {
            triggerAggregation(transactionContext, j);
        }
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        if (!super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2)) {
            return false;
        }
        if (contentValues.containsKey("data1")) {
            long j = cursor.getLong(0);
            long j2 = cursor.getLong(1);
            String asString = contentValues.getAsString("data1");
            this.mDbHelper.deleteNameLookup(j);
            this.mDbHelper.insertNameLookupForEmail(j2, j, asString);
            fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
            triggerAggregation(transactionContext, j2);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(2);
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        this.mDbHelper.deleteNameLookup(j);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        triggerAggregation(transactionContext, j2);
        return iDelete;
    }

    @Override
    protected int getTypeRank(int i) {
        switch (i) {
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
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        indexBuilder.appendContentFromColumn("data1");
    }
}
