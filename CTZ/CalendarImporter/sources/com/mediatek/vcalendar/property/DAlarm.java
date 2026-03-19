package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.Utility;
import com.mediatek.vcalendar.valuetype.DDuration;
import java.util.LinkedList;

public class DAlarm extends Property {
    private static final String TAG = "DAlarm";

    public DAlarm(String str) {
        super(Property.DALARM, str);
        LogUtil.d(TAG, "Constructor: DAlarm property created.");
    }

    public void writeInfoToContentValues(LinkedList<ContentValues> linkedList, long j) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        if (linkedList == null) {
            LogUtil.e(TAG, "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
        if ("VEVENT".equals(this.mComponent.getName())) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("minutes", Long.valueOf(((-1) * (Utility.getTimeInMillis(null, this.mValue) - j)) / DDuration.MILLIS_IN_MIN));
            contentValues.put(LoggingEvents.VoiceIme.EXTRA_START_METHOD, (Integer) 1);
            linkedList.add(contentValues);
        }
    }
}
