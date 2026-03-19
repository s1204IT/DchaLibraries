package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.Recur;

public class RRule extends Property {
    public RRule(String str) {
        super("RRULE", str);
        LogUtil.d("RRule", "Constructor : RRULE property created");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("RRule", "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            if (this.mValue == null || this.mValue.equals("")) {
                LogUtil.d("RRule", "no rrule, return");
            } else {
                contentValues.put("rrule", Recur.updateRRuleToRfc5545Version(this.mValue));
            }
        }
    }
}
