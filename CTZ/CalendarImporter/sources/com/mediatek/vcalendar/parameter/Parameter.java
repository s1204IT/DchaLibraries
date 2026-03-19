package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.utils.LogUtil;

public class Parameter {
    public static final String ABBREV = "ABBREV";
    public static final String ALTREP = "ALTREP";
    public static final String CHARSET = "CHARSET";
    public static final String CN = "CN";
    public static final String CUTYPE = "CUTYPE";
    public static final String DELEGATED_FROM = "DELEGATED-FROM";
    public static final String DELEGATED_TO = "DELEGATED-TO";
    public static final String DIR = "DIR";
    public static final String ENCODING = "ENCODING";
    public static final String EXPERIMENTAL_PREFIX = "X-";
    public static final String FBTYPE = "FBTYPE";
    public static final String FMTTYPE = "FMTTYPE";
    public static final String LANGUAGE = "LANGUAGE";
    public static final String MEMBER = "MEMBER";
    public static final String PARTSTAT = "PARTSTAT";
    public static final String RANGE = "RANGE";
    public static final String RELATED = "RELATED";
    public static final String RELTYPE = "RELTYPE";
    public static final String ROLE = "ROLE";
    public static final String RSVP = "RSVP";
    public static final String SCHEDULE_AGENT = "SCHEDULE-AGENT";
    public static final String SCHEDULE_STATUS = "SCHEDULE-STATUS";
    public static final String SENT_BY = "SENT-BY";
    private static final String TAG = "Parameter";
    public static final String TYPE = "TYPE";
    public static final String TZID = "TZID";
    public static final String VALUE = "VALUE";
    public static final String VVENUE = "VVENUE";
    public static final String X_RELATIONSHIP = "X-RELATIONSHIP";
    protected Component mComponent;
    protected String mName;
    protected String mValue;

    public Parameter(String str) {
        this.mName = str;
    }

    public Parameter(String str, String str2) {
        this.mName = str;
        this.mValue = str2;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public void setValue(String str) {
        this.mValue = str;
    }

    public void setComponent(Component component) {
        this.mComponent = component;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(this.mName);
        sb.append("=");
        sb.append(this.mValue);
    }

    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        if (contentValues == null) {
            LogUtil.e(TAG, "toAttendeesContentValue: the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }
}
