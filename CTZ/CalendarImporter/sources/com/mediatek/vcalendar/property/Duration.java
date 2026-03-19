package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.DDuration;

public class Duration extends Property {
    private static final String TAG = "Duration";

    public Duration(String str) {
        super("DURATION", str);
        LogUtil.d(TAG, "Constructor: DURATION property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues(): duration=" + this.mValue);
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("duration", this.mValue);
        }
    }

    public long getValueMillis() {
        return DDuration.getDurationMillis(this.mValue);
    }
}
