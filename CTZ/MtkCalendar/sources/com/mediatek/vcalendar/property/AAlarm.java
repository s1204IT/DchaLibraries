package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.Utility;
import java.util.LinkedList;

public class AAlarm extends Property {
    public AAlarm(String str) {
        super("AALARM", str);
        LogUtil.d("AAlarm", "Constructor: AAlarm property created.");
    }

    public void writeInfoToContentValues(LinkedList<ContentValues> linkedList, long j) throws VCalendarException {
        LogUtil.d("AAlarm", "writeInfoToContentValues()");
        if (linkedList == null) {
            LogUtil.e("AAlarm", "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
        if ("VEVENT".equals(this.mComponent.getName())) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("minutes", Long.valueOf(((-1) * (Utility.getTimeInMillis(null, this.mValue) - j)) / 60000));
            contentValues.put("method", (Integer) 1);
            linkedList.add(contentValues);
        }
    }
}
