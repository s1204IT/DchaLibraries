package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.Recur;

public class RRule extends Property {
    private static final String TAG = "RRule";

    public RRule(String str) {
        super("RRULE", str);
        LogUtil.d(TAG, "Constructor : RRULE property created");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            if (this.mValue == null || this.mValue.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                LogUtil.d(TAG, "no rrule, return");
            } else {
                contentValues.put("rrule", Recur.updateRRuleToRfc5545Version(this.mValue));
            }
        }
    }
}
