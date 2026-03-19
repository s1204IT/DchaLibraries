package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import android.database.Cursor;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.parameter.Cn;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.parameter.PartStat;
import com.mediatek.vcalendar.parameter.Role;
import com.mediatek.vcalendar.parameter.XRelationship;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;
import com.mediatek.vcalendar.valuetype.CalAddress;
import java.util.LinkedList;

public class Attendee extends Property {
    public static final String CHAIR = "CHAIR";
    public static final String NON_PARTICIPANT = "NON_PARTICIPANT";
    public static final String OPT_PARTICIPANT = "OPT_PARTICIPANT";
    public static final String REQ_PARTICIPANT = "REQ_PARTICIPANT";
    private static final String TAG = "Attendee";

    public Attendee(String str) {
        super(Property.ATTENDEE, str);
        LogUtil.d(TAG, "Constructor: ATTENDEE property created.");
    }

    @Override
    public void compose(Cursor cursor, Component component) throws VCalendarException {
        LogUtil.d(TAG, "compose()");
        super.compose(cursor, component);
        if ("VEVENT".equals(component.getName())) {
            String string = cursor.getString(cursor.getColumnIndex("attendeeName"));
            if (!StringUtil.isNullOrEmpty(string)) {
                addParameter(new Cn(string));
            }
            String string2 = cursor.getString(cursor.getColumnIndex("attendeeEmail"));
            if (!StringUtil.isNullOrEmpty(string2)) {
                this.mValue = CalAddress.getUserCalAddress(string2);
            }
            addParameter(new PartStat(PartStat.getPartstatString(cursor.getInt(cursor.getColumnIndex("attendeeStatus")))));
            String xRelationshipString = XRelationship.getXRelationshipString(cursor.getInt(cursor.getColumnIndex("attendeeRelationship")));
            if (xRelationshipString.equals("ORGANIZER")) {
                addParameter(new Role("CHAIR"));
            }
            addParameter(new XRelationship(xRelationshipString));
            String roleString = Role.getRoleString(cursor.getInt(cursor.getColumnIndex("attendeeType")));
            if (!this.mParamsMap.containsKey(Parameter.ROLE)) {
                addParameter(new Role(roleString));
            }
        }
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        String userMail;
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            Parameter firstParameter = getFirstParameter(Parameter.ROLE);
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
        LogUtil.d(TAG, "writeInfoToContentValues(): started.");
        super.writeInfoToContentValues(linkedList);
        if ("VEVENT".equals(this.mComponent.getName()) && CalAddress.getUserMail(this.mValue) != null) {
            ContentValues contentValues = new ContentValues();
            if (this.mParamsMap.containsKey(Parameter.X_RELATIONSHIP) && (firstParameter2 = getFirstParameter(Parameter.X_RELATIONSHIP)) != null) {
                firstParameter2.writeInfoToContentValues(contentValues);
            }
            for (String str : getParameterNames()) {
                if (!str.equals(Parameter.X_RELATIONSHIP) && (firstParameter = getFirstParameter(str)) != null) {
                    firstParameter.writeInfoToContentValues(contentValues);
                }
            }
            if (!this.mParamsMap.containsKey(Parameter.X_RELATIONSHIP) && !this.mParamsMap.containsKey(Parameter.ROLE)) {
                contentValues.put("attendeeRelationship", (Integer) 1);
                contentValues.put("attendeeType", (Integer) 1);
            }
            contentValues.put("attendeeEmail", CalAddress.getUserMail(this.mValue));
            linkedList.add(contentValues);
        }
    }
}
