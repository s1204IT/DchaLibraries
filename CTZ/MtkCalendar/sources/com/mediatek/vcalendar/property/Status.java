package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Status extends Property {
    public Status(String str) {
        super("STATUS", str);
        LogUtil.d("Status", "STATUS property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("eventStatus", Integer.valueOf(getStatusType(this.mValue)));
        }
    }

    private int getStatusType(String str) {
        if ("CONFIRMED".equals(str)) {
            return 1;
        }
        if ("CANCELLED".equals(str)) {
            return 2;
        }
        return 0;
    }
}
