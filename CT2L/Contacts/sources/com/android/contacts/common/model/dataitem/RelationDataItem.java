package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

public class RelationDataItem extends DataItem {
    RelationDataItem(ContentValues values) {
        super(values);
    }

    public String getName() {
        return getContentValues().getAsString("data1");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }

    @Override
    public boolean shouldCollapseWith(DataItem t, Context context) {
        if (!(t instanceof RelationDataItem) || this.mKind == null || t.getDataKind() == null) {
            return false;
        }
        RelationDataItem that = (RelationDataItem) t;
        if (!TextUtils.equals(getName(), that.getName())) {
            return false;
        }
        if (!hasKindTypeColumn(this.mKind) || !that.hasKindTypeColumn(that.getDataKind())) {
            return hasKindTypeColumn(this.mKind) == that.hasKindTypeColumn(that.getDataKind());
        }
        if (getKindTypeColumn(this.mKind) == that.getKindTypeColumn(that.getDataKind())) {
            return getKindTypeColumn(this.mKind) != 0 || TextUtils.equals(getLabel(), that.getLabel());
        }
        return false;
    }
}
