package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Action extends Property {
    public Action(String str) {
        super("ACTION", str);
        LogUtil.d("Action", "Constructor: ACTION property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("Action", "toAlarmsContentValue: begin");
        super.writeInfoToContentValues(contentValues);
        if ("VALARM".equals(this.mComponent.getName()) && "VEVENT".equals(this.mComponent.getParent().getName())) {
            contentValues.put("method", Integer.valueOf(getMethod(this.mValue)));
        }
    }

    private int getMethod(String str) {
        if ("AUDIO".equals(str)) {
            return 1;
        }
        if ("EMAIL".equals(str)) {
            return 2;
        }
        if ("X-SMS".equals(str)) {
            return 3;
        }
        return 0;
    }
}
