package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.DDuration;

public class Trigger extends Property {
    public Trigger(String str) {
        super("TRIGGER", str);
        LogUtil.d("Trigger", "Constructor: TRIGGER property created");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("Trigger", "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VALARM".equals(this.mComponent.getName()) && "VEVENT".equals(this.mComponent.getParent().getName())) {
            contentValues.put("minutes", Long.valueOf(((-1) * DDuration.getDurationMillis(this.mValue)) / 60000));
        }
    }
}
