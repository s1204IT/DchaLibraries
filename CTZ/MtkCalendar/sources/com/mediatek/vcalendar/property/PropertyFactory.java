package com.mediatek.vcalendar.property;

import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Locale;

public final class PropertyFactory {
    public static Property createProperty(String str, String str2) {
        if (str == null) {
            LogUtil.e("PropertyFactory", "createProperty(): Cannot create a property without giving defined name");
            return null;
        }
        String upperCase = str.toUpperCase(Locale.US);
        if ("ACTION".equals(upperCase)) {
            return new Action(str2);
        }
        if ("ATTENDEE".equals(upperCase)) {
            return new Attendee(str2);
        }
        if ("DESCRIPTION".equals(upperCase)) {
            return new Description(str2);
        }
        if ("DTEND".equals(upperCase)) {
            return new DtEnd(upperCase);
        }
        if ("DTSTAMP".equals(upperCase)) {
            return new DtStamp(str2);
        }
        if ("DTSTART".equals(upperCase)) {
            return new DtStart(str2);
        }
        if ("DURATION".equals(upperCase)) {
            return new Duration(str2);
        }
        if ("LOCATION".equals(upperCase)) {
            return new Location(str2);
        }
        if ("PRODID".equals(upperCase)) {
            return new ProdId();
        }
        if ("RRULE".equals(upperCase)) {
            return new RRule(str2);
        }
        if ("STATUS".equals(upperCase)) {
            return new Status(str2);
        }
        if ("SUMMARY".equals(upperCase)) {
            return new Summary(str2);
        }
        if ("TRIGGER".equals(upperCase)) {
            return new Trigger(str2);
        }
        if ("UID".equals(upperCase)) {
            return new Uid(str2);
        }
        if ("VERSION".equals(upperCase)) {
            return new Version();
        }
        if ("AALARM".equals(upperCase)) {
            return new AAlarm(str2);
        }
        if ("DALARM".equals(upperCase)) {
            return new DAlarm(str2);
        }
        return new Property(upperCase, str2);
    }
}
