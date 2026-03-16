package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForCommonDataKind extends DataRowHandler {
    private final String mLabelColumn;
    private final String mTypeColumn;

    public DataRowHandlerForCommonDataKind(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, String mimetype, String typeColumn, String labelColumn) {
        super(context, dbHelper, aggregator, mimetype);
        this.mTypeColumn = typeColumn;
        this.mLabelColumn = labelColumn;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        enforceTypeAndLabel(values);
        return super.insert(db, txContext, rawContactId, values);
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(0);
        ContentValues augmented = getAugmentedValues(db, dataId, values);
        if (augmented == null) {
            return false;
        }
        enforceTypeAndLabel(augmented);
        return super.update(db, txContext, values, c, callerIsSyncAdapter);
    }

    private void enforceTypeAndLabel(ContentValues augmented) {
        boolean hasType = !TextUtils.isEmpty(augmented.getAsString(this.mTypeColumn));
        boolean hasLabel = !TextUtils.isEmpty(augmented.getAsString(this.mLabelColumn));
        if (hasLabel && !hasType) {
            throw new IllegalArgumentException(this.mTypeColumn + " must be specified when " + this.mLabelColumn + " is defined.");
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }
}
