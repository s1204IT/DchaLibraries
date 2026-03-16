package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.PhoneNumberUtils;

public class PhoneDataItem extends DataItem {
    PhoneDataItem(ContentValues values) {
        super(values);
    }

    public String getNumber() {
        return getContentValues().getAsString("data1");
    }

    public String getNormalizedNumber() {
        return getContentValues().getAsString("data4");
    }

    public String getFormattedPhoneNumber() {
        return getContentValues().getAsString("formattedPhoneNumber");
    }

    public String getLabel() {
        return getContentValues().getAsString("data3");
    }

    public void computeFormattedPhoneNumber(String defaultCountryIso) {
        String phoneNumber = getNumber();
        if (phoneNumber != null) {
            String formattedPhoneNumber = PhoneNumberUtils.formatNumber(phoneNumber, getNormalizedNumber(), defaultCountryIso);
            getContentValues().put("formattedPhoneNumber", formattedPhoneNumber);
        }
    }

    @Override
    public String buildDataStringForDisplay(Context context, DataKind kind) {
        String formatted = getFormattedPhoneNumber();
        return formatted != null ? formatted : getNumber();
    }
}
