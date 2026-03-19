package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForOrganization extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForOrganization(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/organization", "data2", "data3");
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        contentValues.getAsString("data1");
        contentValues.getAsString("data4");
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j);
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        if (!super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2)) {
            return false;
        }
        boolean zContainsKey = contentValues.containsKey("data1");
        boolean zContainsKey2 = contentValues.containsKey("data4");
        if (zContainsKey || zContainsKey2) {
            long j = cursor.getLong(0);
            long j2 = cursor.getLong(1);
            if (zContainsKey) {
                contentValues.getAsString("data1");
            } else {
                this.mSelectionArgs1[0] = String.valueOf(j);
                DatabaseUtils.stringForQuery(sQLiteDatabase, "SELECT data1 FROM data WHERE _id=?", this.mSelectionArgs1);
            }
            if (zContainsKey2) {
                contentValues.getAsString("data4");
            } else {
                this.mSelectionArgs1[0] = String.valueOf(j);
                DatabaseUtils.stringForQuery(sQLiteDatabase, "SELECT data4 FROM data WHERE _id=?", this.mSelectionArgs1);
            }
            this.mDbHelper.deleteNameLookup(j);
            fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(2);
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        this.mDbHelper.deleteNameLookup(j);
        return iDelete;
    }

    @Override
    protected int getTypeRank(int i) {
        switch (i) {
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
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data1") || contentValues.containsKey("data5") || contentValues.containsKey("data6") || contentValues.containsKey("data9") || contentValues.containsKey("data8") || contentValues.containsKey("data7") || contentValues.containsKey("data4");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        indexBuilder.appendNameFromColumn("data4");
        indexBuilder.appendNameFromColumn("data1");
        indexBuilder.appendContentFromColumn("data4");
        indexBuilder.appendContentFromColumn("data1", 3);
        indexBuilder.appendContentFromColumn("data8", 1);
        indexBuilder.appendContentFromColumn("data7", 1);
        indexBuilder.appendContentFromColumn("data5", 2);
        indexBuilder.appendContentFromColumn("data9", 2);
        indexBuilder.appendContentFromColumn("data6", 2);
    }
}
