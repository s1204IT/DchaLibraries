package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForIm extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForIm(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/im", "data2", "data3");
    }

    @Override
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        indexBuilder.appendContent(ContactsContract.CommonDataKinds.Im.getProtocolLabel(this.mContext.getResources(), indexBuilder.getInt("data5"), indexBuilder.getString("data6")).toString());
        indexBuilder.appendContentFromColumn("data1", 2);
    }
}
