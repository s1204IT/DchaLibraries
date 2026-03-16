package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class EmailDataItem extends DataItem {
    EmailDataItem(ContentValues values) {
        super(values);
    }

    public String getAddress() {
        return getContentValues().getAsString("data1");
    }

    public String getData() {
        return getContentValues().getAsString("data1");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }
}
