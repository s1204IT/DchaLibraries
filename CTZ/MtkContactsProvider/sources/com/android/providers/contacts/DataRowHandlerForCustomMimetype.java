package com.android.providers.contacts;

import android.content.Context;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForCustomMimetype extends DataRowHandler {
    public DataRowHandlerForCustomMimetype(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, String str) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, str);
    }
}
