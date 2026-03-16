package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

public class EventDataItem extends DataItem {
    EventDataItem(ContentValues values) {
        super(values);
    }

    public String getStartDate() {
        return getContentValues().getAsString("data1");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }

    @Override
    public boolean shouldCollapseWith(DataItem t, Context context) {
        if (!(t instanceof EventDataItem) || this.mKind == null || t.getDataKind() == null) {
            return false;
        }
        EventDataItem that = (EventDataItem) t;
        if (!TextUtils.equals(getStartDate(), that.getStartDate())) {
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
