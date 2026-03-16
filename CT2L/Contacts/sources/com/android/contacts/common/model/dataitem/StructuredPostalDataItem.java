package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class StructuredPostalDataItem extends DataItem {
    StructuredPostalDataItem(ContentValues values) {
        super(values);
    }

    public String getFormattedAddress() {
        return getContentValues().getAsString("data1");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }
}
