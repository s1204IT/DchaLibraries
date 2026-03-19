package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Duration extends Property {
    public Duration(String str) {
        super("DURATION", str);
        LogUtil.d("Duration", "Constructor: DURATION property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("Duration", "writeInfoToContentValues(): duration=" + this.mValue);
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("duration", this.mValue);
        }
    }
}
