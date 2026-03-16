package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;

public class GroupMembershipDataItem extends DataItem {
    GroupMembershipDataItem(ContentValues values) {
        super(values);
    }

    public Long getGroupRowId() {
        return getContentValues().getAsLong("data1");
    }
}
