package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Action extends Property {
    public static final String AUDIO = "AUDIO";
    public static final String DISPLAY = "DISPLAY";
    public static final String EMAIL = "EMAIL";
    private static final String TAG = "Action";
    public static final String X_SMS = "X-SMS";

    public Action(String str) {
        super(Property.ACTION, str);
        LogUtil.d(TAG, "Constructor: ACTION property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "toAlarmsContentValue: begin");
        super.writeInfoToContentValues(contentValues);
        if ("VALARM".equals(this.mComponent.getName()) && "VEVENT".equals(this.mComponent.getParent().getName())) {
            contentValues.put(LoggingEvents.VoiceIme.EXTRA_START_METHOD, Integer.valueOf(getMethod(this.mValue)));
        }
    }

    public static String getActionString(int i) {
        switch (i) {
            case 2:
                return EMAIL;
            case 3:
                return X_SMS;
            default:
                return AUDIO;
        }
    }

    private int getMethod(String str) {
        if (AUDIO.equals(str)) {
            return 1;
        }
        if (EMAIL.equals(str)) {
            return 2;
        }
        if (X_SMS.equals(str)) {
            return 3;
        }
        return 0;
    }
}
