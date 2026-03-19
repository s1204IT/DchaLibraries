package com.mediatek.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForWebsite extends DataRowHandler {
    public DataRowHandlerForWebsite(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/website");
    }

    @Override
    public boolean hasSearchableData() {
        return true;
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
