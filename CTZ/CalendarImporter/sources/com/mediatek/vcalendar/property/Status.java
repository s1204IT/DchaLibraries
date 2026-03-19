package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Status extends Property {
    public static final String CANCELLED = "CANCELLED";
    public static final String CONFIRMED = "CONFIRMED";
    private static final String TAG = "Status";
    public static final String TENTATIVE = "TENTATIVE";

    public Status(String str) {
        super(Property.STATUS, str);
        LogUtil.d(TAG, "STATUS property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("eventStatus", Integer.valueOf(getStatusType(this.mValue)));
        }
    }

    public static String getStatusString(int i) {
        switch (i) {
            case 1:
                return CONFIRMED;
            case 2:
                return CANCELLED;
            default:
                return TENTATIVE;
        }
    }

    private int getStatusType(String str) {
        if (CONFIRMED.equals(str)) {
            return 1;
        }
        if (CANCELLED.equals(str)) {
            return 2;
        }
        return 0;
    }
}
