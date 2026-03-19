package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class PartStat extends Parameter {
    private static final String ACCEPTED = "ACCEPTED";
    private static final String DECCLIEND = "DECCLIEND";
    private static final String DELEGATED = "DELEGATED";
    private static final String NEEDS_ACTION = "NEEDS-ACTION";
    private static final String TAG = "Parstat";
    private static final String TENTATIVE = "TENTATIVE";
    private static final String X_INVITED = "X-INVITED";

    public PartStat(String str) {
        super(Parameter.PARTSTAT, str);
        LogUtil.d(TAG, "Constructor: PARTSTAT parameter created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "toAttendeesContentValue started");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeStatus", Integer.valueOf(getPartstatStatus(this.mValue)));
        }
    }

    public static String getPartstatString(int i) {
        switch (i) {
            case 0:
                return NEEDS_ACTION;
            case 1:
                return ACCEPTED;
            case 2:
                return DECCLIEND;
            case 3:
                return X_INVITED;
            case 4:
                return "TENTATIVE";
            default:
                return NEEDS_ACTION;
        }
    }

    private int getPartstatStatus(String str) {
        if (str.equals(ACCEPTED)) {
            return 1;
        }
        if (str.equals(DECCLIEND)) {
            return 2;
        }
        if (str.equals(X_INVITED)) {
            return 3;
        }
        if (str.equals("TENTATIVE")) {
            return 4;
        }
        return 0;
    }
}
