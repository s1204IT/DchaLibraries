package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class SipAddressDataItem extends DataItem {
    SipAddressDataItem(ContentValues values) {
        super(values);
    }

    public String getSipAddress() {
        return getContentValues().getAsString("data1");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }
}
