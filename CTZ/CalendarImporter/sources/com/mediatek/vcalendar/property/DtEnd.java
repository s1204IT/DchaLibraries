package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.parameter.TzId;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.Utility;

public class DtEnd extends Property {
    private static final String TAG = "DtEnd";

    public DtEnd(String str) {
        super("DTEND", str);
        LogUtil.d(TAG, "Constructor: DTEND property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("dtend", Long.valueOf(getValueMillis()));
        }
    }

    public long getValueMillis() throws VCalendarException {
        return Utility.getTimeInMillis((TzId) getFirstParameter("TZID"), this.mValue);
    }
}
