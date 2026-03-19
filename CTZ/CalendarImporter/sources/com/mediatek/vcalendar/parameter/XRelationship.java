package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class XRelationship extends Parameter {
    private static final String ATTENDEE = "ATTENDEE";
    private static final String NONE = "NONE";
    public static final String ORGANIZER = "ORGANIZER";
    private static final String PERFORMER = "PERFORMER";
    private static final String SPEAKER = "SPEAKER";
    private static final String TAG = "XRelationship";

    public XRelationship(String str) {
        super(Parameter.X_RELATIONSHIP, str);
        LogUtil.d(TAG, "Constructor : X-RELATIONSHIP paratmeter created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeRelationship", Integer.valueOf(getXRelationshipType(this.mValue)));
        }
    }

    public static String getXRelationshipString(int i) {
        switch (i) {
            case 0:
                return NONE;
            case 1:
                return "ATTENDEE";
            case 2:
                return "ORGANIZER";
            case 3:
                return PERFORMER;
            case 4:
                return SPEAKER;
            default:
                return "ATTENDEE";
        }
    }

    private int getXRelationshipType(String str) {
        if (str.equals(NONE)) {
            return 0;
        }
        if (str.equals("ORGANIZER")) {
            return 2;
        }
        if (str.equals(PERFORMER)) {
            return 3;
        }
        if (str.equals(SPEAKER)) {
            return 4;
        }
        return 1;
    }
}
