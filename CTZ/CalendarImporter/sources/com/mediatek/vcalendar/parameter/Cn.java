package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Cn extends Parameter {
    private static final String TAG = "Cn";

    public Cn(String str) {
        super(Parameter.CN, str);
        LogUtil.d(TAG, "Constructor: CN parameter started");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeName", this.mValue);
        }
    }
}
