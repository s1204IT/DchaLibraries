package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.DDuration;

public class Trigger extends Property {
    private static final String TAG = "Trigger";

    public Trigger(String str) {
        super(Property.TRIGGER, str);
        LogUtil.d(TAG, "Constructor: TRIGGER property created");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VALARM".equals(this.mComponent.getName()) && "VEVENT".equals(this.mComponent.getParent().getName())) {
            contentValues.put("minutes", Long.valueOf(((-1) * DDuration.getDurationMillis(this.mValue)) / DDuration.MILLIS_IN_MIN));
        }
    }
}
