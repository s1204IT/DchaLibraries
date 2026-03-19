package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.CalAddress;
import java.util.LinkedList;

public class Attendee extends Property {
    public Attendee(String str) {
        super("ATTENDEE", str);
        LogUtil.d("Attendee", "Constructor: ATTENDEE property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        String userMail;
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            Parameter firstParameter = getFirstParameter("ROLE");
            if (firstParameter != null && firstParameter.getValue().equals("CHAIR") && (userMail = CalAddress.getUserMail(this.mValue)) != null) {
                contentValues.put("organizer", userMail);
            }
            contentValues.put("hasAttendeeData", (Integer) 1);
        }
    }

    @Override
    public void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        Parameter firstParameter;
        Parameter firstParameter2;
        LogUtil.d("Attendee", "writeInfoToContentValues(): started.");
        super.writeInfoToContentValues(linkedList);
        if ("VEVENT".equals(this.mComponent.getName()) && CalAddress.getUserMail(this.mValue) != null) {
            ContentValues contentValues = new ContentValues();
            if (this.mParamsMap.containsKey("X-RELATIONSHIP") && (firstParameter2 = getFirstParameter("X-RELATIONSHIP")) != null) {
                firstParameter2.writeInfoToContentValues(contentValues);
            }
            for (String str : getParameterNames()) {
                if (!str.equals("X-RELATIONSHIP") && (firstParameter = getFirstParameter(str)) != null) {
                    firstParameter.writeInfoToContentValues(contentValues);
                }
            }
            if (!this.mParamsMap.containsKey("X-RELATIONSHIP") && !this.mParamsMap.containsKey("ROLE")) {
                contentValues.put("attendeeRelationship", (Integer) 1);
                contentValues.put("attendeeType", (Integer) 1);
            }
            contentValues.put("attendeeEmail", CalAddress.getUserMail(this.mValue));
            linkedList.add(contentValues);
        }
    }
}
