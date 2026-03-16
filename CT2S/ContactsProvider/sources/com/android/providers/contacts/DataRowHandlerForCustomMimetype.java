package com.android.providers.contacts;

import android.content.Context;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForCustomMimetype extends DataRowHandler {
    public DataRowHandlerForCustomMimetype(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, String mimetype) {
        super(context, dbHelper, aggregator, mimetype);
    }
}
