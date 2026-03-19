package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Uid extends Property {
    private static final String TAG = "Uid";

    public Uid(String str) {
        super(Property.UID, str);
        LogUtil.d(TAG, "Constructor: Uid Property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("_id", (String) null);
        }
    }
}
