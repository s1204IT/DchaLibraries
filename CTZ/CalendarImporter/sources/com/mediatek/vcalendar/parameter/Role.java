package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;

public class Role extends Parameter {
    public static final String CHAIR = "CHAIR";
    private static final String NON_PARTICIPANT = "NON-PARTICIPANT";
    private static final String OPT_PARTICIPANT = "OPT-PARTICIPANT";
    private static final String REQ_PARTICIPANT = "REQ-PARTICIPANT";

    public Role(String str) {
        super(Parameter.ROLE, str);
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeType", Integer.valueOf(getRoleType(this.mValue)));
            if (!contentValues.containsKey("attendeeRelationship")) {
                contentValues.put("attendeeRelationship", Integer.valueOf(getRationshipType(this.mValue)));
            }
        }
    }

    public static String getRoleString(int i) {
        switch (i) {
            case 0:
                return NON_PARTICIPANT;
            case 1:
                return REQ_PARTICIPANT;
            case 2:
                return OPT_PARTICIPANT;
            default:
                return REQ_PARTICIPANT;
        }
    }

    private int getRoleType(String str) {
        if (str.equals(REQ_PARTICIPANT)) {
            return 1;
        }
        if (str.equals(OPT_PARTICIPANT)) {
            return 2;
        }
        return (str.equals("CHAIR") || str.equals(NON_PARTICIPANT)) ? 0 : 1;
    }

    private int getRationshipType(String str) {
        if (str.equals("CHAIR")) {
            return 2;
        }
        if (str.equals(OPT_PARTICIPANT) || str.equals(NON_PARTICIPANT)) {
            return 0;
        }
        return 1;
    }
}
