package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForIm extends DataRowHandlerForCommonDataKind {
    public DataRowHandlerForIm(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/im", "data2", "data3");
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey("data1");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
        int protocol = builder.getInt("data5");
        String customProtocol = builder.getString("data6");
        builder.appendContent(ContactsContract.CommonDataKinds.Im.getProtocolLabel(this.mContext.getResources(), protocol, customProtocol).toString());
        builder.appendContentFromColumn("data1", 2);
    }
}
