package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class XRelationship extends Parameter {
    public XRelationship(String str) {
        super("X-RELATIONSHIP", str);
        LogUtil.d("XRelationship", "Constructor : X-RELATIONSHIP paratmeter created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("attendeeRelationship", Integer.valueOf(getXRelationshipType(this.mValue)));
        }
    }

    private int getXRelationshipType(String str) {
        if (str.equals("NONE")) {
            return 0;
        }
        if (str.equals("ORGANIZER")) {
            return 2;
        }
        if (str.equals("PERFORMER")) {
            return 3;
        }
        if (str.equals("SPEAKER")) {
            return 4;
        }
        return 1;
    }
}
