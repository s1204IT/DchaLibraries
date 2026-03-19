package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForCommonDataKind extends DataRowHandler {
    private final String mLabelColumn;
    private final String mTypeColumn;

    public DataRowHandlerForCommonDataKind(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, String str, String str2, String str3) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, str);
        this.mTypeColumn = str2;
        this.mLabelColumn = str3;
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        enforceTypeAndLabel(contentValues);
        return super.insert(sQLiteDatabase, transactionContext, j, contentValues);
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        ContentValues augmentedValues = getAugmentedValues(sQLiteDatabase, cursor.getLong(0), contentValues);
        if (augmentedValues == null) {
            return false;
        }
        enforceTypeAndLabel(augmentedValues);
        return super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2);
    }

    private void enforceTypeAndLabel(ContentValues contentValues) {
        boolean z = !TextUtils.isEmpty(contentValues.getAsString(this.mTypeColumn));
        if ((!TextUtils.isEmpty(contentValues.getAsString(this.mLabelColumn))) && !z) {
            throw new IllegalArgumentException(this.mTypeColumn + " must be specified when " + this.mLabelColumn + " is defined.");
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }
}
