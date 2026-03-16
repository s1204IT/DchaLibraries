package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class WebsiteDataItem extends DataItem {
    WebsiteDataItem(ContentValues values) {
        super(values);
    }

    public String getUrl() {
        return getContentValues().getAsString("data1");
    }
}
