package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class PhotoDataItem extends DataItem {
    PhotoDataItem(ContentValues values) {
        super(values);
    }

    public byte[] getPhoto() {
        return getContentValues().getAsByteArray("data15");
    }
}
