package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Location extends Property {
    public Location(String str) {
        super("LOCATION", str);
        LogUtil.d("Location", "Constructor: Location property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("eventLocation", this.mValue);
        }
    }
}
