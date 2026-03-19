package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.parameter.TzId;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.Utility;
import java.util.TimeZone;

public class DtStart extends Property {
    public DtStart(String str) {
        super("DTSTART", str);
        LogUtil.d("DtStart", "Constructor: DtStart property created");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("DtStart", "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("dtstart", Long.valueOf(getValueMillis()));
            if (!contentValues.containsKey("eventTimezone")) {
                String localTimezone = Utility.getLocalTimezone((TzId) getFirstParameter("TZID"), this.mValue);
                TimeZone timeZone = TimeZone.getTimeZone(localTimezone);
                contentValues.put("eventTimezone", timeZone.getID());
                LogUtil.v("DtStart", "set a timezone, timezone.getID()=" + timeZone.getID() + ";localTimezone=" + localTimezone);
            }
        }
    }

    public long getValueMillis() throws VCalendarException {
        return Utility.getTimeInMillis((TzId) getFirstParameter("TZID"), this.mValue);
    }
}
