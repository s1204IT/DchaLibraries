package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.PostalSplitter;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForStructuredPostal extends DataRowHandler {
    private final String[] STRUCTURED_FIELDS;
    private final PostalSplitter mSplitter;

    public DataRowHandlerForStructuredPostal(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, PostalSplitter splitter) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/postal-address_v2");
        this.STRUCTURED_FIELDS = new String[]{"data4", "data5", "data6", "data7", "data8", "data9", "data10"};
        this.mSplitter = splitter;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        fixStructuredPostalComponents(values, values);
        return super.insert(db, txContext, rawContactId, values);
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(0);
        ContentValues augmented = getAugmentedValues(db, dataId, values);
        if (augmented == null) {
            return false;
        }
        fixStructuredPostalComponents(augmented, values);
        super.update(db, txContext, values, c, callerIsSyncAdapter);
        return true;
    }

    private void fixStructuredPostalComponents(ContentValues augmented, ContentValues update) {
        String unstruct = update.getAsString("data1");
        boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
        boolean touchedStruct = !areAllEmpty(update, this.STRUCTURED_FIELDS);
        PostalSplitter.Postal postal = new PostalSplitter.Postal();
        if (touchedUnstruct && !touchedStruct) {
            this.mSplitter.split(postal, unstruct);
            postal.toValues(update);
        } else {
            if (touchedUnstruct) {
                return;
            }
            if (touchedStruct || areAnySpecified(update, this.STRUCTURED_FIELDS)) {
                postal.fromValues(augmented);
                String joined = this.mSplitter.join(postal);
                update.put("data1", joined);
            }
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
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
