package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

public class ImDataItem extends DataItem {
    private final boolean mCreatedFromEmail;

    ImDataItem(ContentValues values) {
        super(values);
        this.mCreatedFromEmail = false;
    }

    public String getData() {
        return this.mCreatedFromEmail ? getContentValues().getAsString("data1") : getContentValues().getAsString("data1");
    }

    public Integer getProtocol() {
        return getContentValues().getAsInteger("data5");
    }

    public boolean isProtocolValid() {
        return getProtocol() != null;
    }

    public String getCustomProtocol() {
        return getContentValues().getAsString("data6");
    }

    public int getChatCapability() {
        Integer result = getContentValues().getAsInteger("chat_capability");
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    public boolean isCreatedFromEmail() {
        return this.mCreatedFromEmail;
    }

    @Override
    public boolean shouldCollapseWith(DataItem t, Context context) {
        if (!(t instanceof ImDataItem) || this.mKind == null || t.getDataKind() == null) {
            return false;
        }
        ImDataItem that = (ImDataItem) t;
        if (!getData().equals(that.getData())) {
            return false;
        }
        if (!isProtocolValid() || !that.isProtocolValid()) {
            if (isProtocolValid()) {
                return getProtocol().intValue() == -1;
            }
            if (that.isProtocolValid()) {
                return that.getProtocol().intValue() == -1;
            }
            return true;
        }
        if (getProtocol() == that.getProtocol()) {
            return getProtocol().intValue() != -1 || TextUtils.equals(getCustomProtocol(), that.getCustomProtocol());
        }
        return false;
    }
}
