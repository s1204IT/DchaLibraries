package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class NoteDataItem extends DataItem {
    NoteDataItem(ContentValues values) {
        super(values);
    }

    public String getNote() {
        return getContentValues().getAsString("data1");
    }
}
