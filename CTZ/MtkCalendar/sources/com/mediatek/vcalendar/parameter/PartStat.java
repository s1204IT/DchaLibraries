package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class PartStat extends Parameter {
    public PartStat(String str) {
        super("PARTSTAT", str);
        LogUtil.d("Parstat", "Constructor: PARTSTAT parameter created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("Parstat", "toAttendeesContentValue started");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeStatus", Integer.valueOf(getPartstatStatus(this.mValue)));
        }
    }

    private int getPartstatStatus(String str) {
        if (str.equals("ACCEPTED")) {
            return 1;
        }
        if (str.equals("DECCLIEND")) {
            return 2;
        }
        if (str.equals("X-INVITED")) {
            return 3;
        }
        if (str.equals("TENTATIVE")) {
            return 4;
        }
        return 0;
    }
}
