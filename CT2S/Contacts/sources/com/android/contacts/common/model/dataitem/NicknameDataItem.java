package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class NicknameDataItem extends DataItem {
    public NicknameDataItem(ContentValues values) {
        super(values);
    }

    public String getName() {
        return getContentValues().getAsString("data1");
    }
}
