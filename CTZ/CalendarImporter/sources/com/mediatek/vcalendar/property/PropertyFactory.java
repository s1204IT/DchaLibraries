package com.mediatek.vcalendar.property;

import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Locale;

public final class PropertyFactory {
    private static final boolean DEBUG = false;
    private static final String TAG = "PropertyFactory";

    private PropertyFactory() {
    }

    public static Property createProperty(String str, String str2) {
        if (str == null) {
            LogUtil.e(TAG, "createProperty(): Cannot create a property without giving defined name");
            return null;
        }
        String upperCase = str.toUpperCase(Locale.US);
        if (Property.ACTION.equals(upperCase)) {
            return new Action(str2);
        }
        if (Property.ATTENDEE.equals(upperCase)) {
            return new Attendee(str2);
        }
        if (Property.DESCRIPTION.equals(upperCase)) {
            return new Description(str2);
        }
        if ("DTEND".equals(upperCase)) {
            return new DtEnd(upperCase);
        }
        if (Property.DTSTAMP.equals(upperCase)) {
            return new DtStamp(str2);
        }
        if ("DTSTART".equals(upperCase)) {
            return new DtStart(str2);
        }
        if ("DURATION".equals(upperCase)) {
            return new Duration(str2);
        }
        if (Property.LOCATION.equals(upperCase)) {
            return new Location(str2);
        }
        if (Property.PRODID.equals(upperCase)) {
            return new ProdId();
        }
        if ("RRULE".equals(upperCase)) {
            return new RRule(str2);
        }
        if (Property.STATUS.equals(upperCase)) {
            return new Status(str2);
        }
        if (Property.SUMMARY.equals(upperCase)) {
            return new Summary(str2);
        }
        if (Property.TRIGGER.equals(upperCase)) {
            return new Trigger(str2);
        }
        if (Property.UID.equals(upperCase)) {
            return new Uid(str2);
        }
        if (Property.VERSION.equals(upperCase)) {
            return new Version();
        }
        if (Property.AALARM.equals(upperCase)) {
            return new AAlarm(str2);
        }
        if (Property.DALARM.equals(upperCase)) {
            return new DAlarm(str2);
        }
        return new Property(upperCase, str2);
    }
}
