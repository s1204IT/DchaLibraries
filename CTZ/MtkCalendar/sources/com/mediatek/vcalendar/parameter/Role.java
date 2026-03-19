package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;

public class Role extends Parameter {
    public Role(String str) {
        super("ROLE", str);
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

    private int getRoleType(String str) {
        if (str.equals("REQ-PARTICIPANT")) {
            return 1;
        }
        if (str.equals("OPT-PARTICIPANT")) {
            return 2;
        }
        return (str.equals("CHAIR") || str.equals("NON-PARTICIPANT")) ? 0 : 1;
    }

    private int getRationshipType(String str) {
        if (str.equals("CHAIR")) {
            return 2;
        }
        if (str.equals("OPT-PARTICIPANT") || str.equals("NON-PARTICIPANT")) {
            return 0;
        }
        return 1;
    }
}
