package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForNote extends DataRowHandler {
    public DataRowHandlerForNote(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/note");
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
