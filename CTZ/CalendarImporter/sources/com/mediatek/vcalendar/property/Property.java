package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import android.database.Cursor;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.Text;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Property {
    public static final String AALARM = "AALARM";
    public static final String ACTION = "ACTION";
    public static final String ATTACH = "ATTACH";
    public static final String ATTENDEE = "ATTENDEE";
    public static final String BUSYTYPE = "BUSYTYPE";
    public static final String CALSCALE = "CALSCALE";
    public static final String CATEGORIES = "CATEGORIES";
    public static final String CLASS = "CLASS";
    public static final String COMMENT = "COMMENT";
    public static final String COMPLETED = "COMPLETED";
    public static final String CONTACT = "CONTACT";
    public static final String COUNTRY = "COUNTRY";
    public static final String CREATED = "CREATED";
    public static final String DALARM = "DALARM";
    private static final boolean DEBUG = false;
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String DTEND = "DTEND";
    public static final String DTSTAMP = "DTSTAMP";
    public static final String DTSTART = "DTSTART";
    public static final String DUE = "DUE";
    public static final String DURATION = "DURATION";
    public static final String EXDATE = "EXDATE";
    public static final String EXPERIMENTAL_PREFIX = "X-";
    public static final String EXRULE = "EXRULE";
    public static final String EXTENDED_ADDRESS = "EXTENDED-ADDRESS";
    public static final String FREEBUSY = "FREEBUSY";
    public static final String GEO = "GEO";
    public static final String LAST_MODIFIED = "LAST-MODIFIED";
    public static final String LOCALITY = "LOCALITY";
    public static final String LOCATION = "LOCATION";
    public static final String LOCATION_TYPE = "LOCATION-TYPE";
    public static final String METHOD = "METHOD";
    public static final String NAME = "NAME";
    public static final String ORGANIZER = "ORGANIZER";
    public static final String PERCENT_COMPLETE = "PERCENT-COMPLETE";
    public static final String POSTALCODE = "POSTAL-CODE";
    public static final String PRIORITY = "PRIORITY";
    public static final String PRODID = "PRODID";
    public static final String RDATE = "RDATE";
    public static final String RECURRENCE_ID = "RECURRENCE-ID";
    public static final String REGION = "REGION";
    public static final String RELATED_TO = "RELATED-TO";
    public static final String REPEAT = "REPEAT";
    public static final String REQUEST_STATUS = "REQUEST-STATUS";
    public static final String RESOURCES = "RESOURCES";
    public static final String RRULE = "RRULE";
    public static final String SEQUENCE = "SEQUENCE";
    public static final String STATUS = "STATUS";
    public static final String STREET_ADDRESS = "STREET-ADDRESS";
    public static final String SUMMARY = "SUMMARY";
    private static final String TAG = "Property";
    public static final String TEL = "TEL";
    public static final String TRANSP = "TRANSP";
    public static final String TRIGGER = "TRIGGER";
    public static final String TZ = "TZ";
    public static final String TZID = "TZID";
    public static final String TZNAME = "TZNAME";
    public static final String TZOFFSETFROM = "TZOFFSETFROM";
    public static final String TZOFFSETTO = "TZOFFSETTO";
    public static final String TZURL = "TZURL";
    public static final String UID = "UID";
    public static final String URL = "URL";
    public static final String VERSION = "VERSION";
    public static final String X_ALLDAY = "X-ALLDAY";
    public static final String X_TIMEZONE = "X-TIMEZONE";
    protected Component mComponent;
    protected final String mName;
    protected LinkedHashMap<String, ArrayList<Parameter>> mParamsMap = new LinkedHashMap<>();
    protected String mValue;

    public Property(String str, String str2) {
        this.mName = str;
        this.mValue = str2;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public void setValue(String str, Parameter parameter) {
        this.mValue = str;
        if (parameter != null) {
            this.mValue = Text.decode(this.mValue, parameter.getValue());
        }
        if (SUMMARY.equals(this.mName) || DESCRIPTION.equals(this.mName) || LOCATION.equals(this.mName)) {
            handleEscapedChar();
        }
    }

    public void setComponent(Component component) {
        this.mComponent = component;
        setComponentInParams();
    }

    protected void setComponentInParams() {
        Set<String> setKeySet = this.mParamsMap.keySet();
        if (!setKeySet.isEmpty()) {
            Iterator<String> it = setKeySet.iterator();
            while (it.hasNext()) {
                ArrayList<Parameter> arrayList = this.mParamsMap.get(it.next());
                if (!arrayList.isEmpty()) {
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        arrayList.get(i).setComponent(this.mComponent);
                    }
                }
            }
        }
    }

    public void addParameter(Parameter parameter) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(parameter.getName());
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mParamsMap.put(parameter.getName(), arrayList);
        }
        arrayList.add(parameter);
        parameter.setComponent(this.mComponent);
    }

    public Set<String> getParameterNames() {
        return this.mParamsMap.keySet();
    }

    public List<Parameter> getParameters(String str) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(str);
        if (arrayList == null) {
            return new ArrayList();
        }
        return arrayList;
    }

    public Parameter getFirstParameter(String str) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(str);
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(0);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(this.mName);
        Iterator<String> it = getParameterNames().iterator();
        while (it.hasNext()) {
            for (Parameter parameter : getParameters(it.next())) {
                sb.append(";");
                parameter.toString(sb);
            }
        }
        sb.append(":");
        if (SUMMARY.equals(this.mName) || DESCRIPTION.equals(this.mName) || LOCATION.equals(this.mName)) {
            escapeChar();
        }
        String strEncoding = this.mValue;
        Parameter firstParameter = getFirstParameter(Parameter.ENCODING);
        if (firstParameter != null) {
            strEncoding = Text.encoding(this.mValue, firstParameter.getValue());
        }
        sb.append(strEncoding);
    }

    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        if (contentValues == null) {
            LogUtil.e(TAG, "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }

    public void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            LogUtil.e(TAG, "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }

    public void compose(Cursor cursor, Component component) throws VCalendarException {
        if (cursor == null || !cursor.moveToFirst()) {
            throw new VCalendarException("Expected Property Cursor queried from DB cannot be null or empty.");
        }
    }

    protected void handleEscapedChar() {
        LogUtil.d(TAG, "handleEscapedChar(),before mValue:" + this.mValue);
        this.mValue = this.mValue.replace("\\\\", "\\");
        this.mValue = this.mValue.replace("\\;", ";");
        this.mValue = this.mValue.replace("\\,", ",");
        this.mValue = this.mValue.replace("\\N", "\n");
        this.mValue = this.mValue.replace("\\n", "\n");
        LogUtil.d(TAG, "handleEscapedChar(), after mValue: " + this.mValue);
    }

    protected void escapeChar() {
        LogUtil.d(TAG, "escapeChar(), before mValue: " + this.mValue);
        this.mValue = this.mValue.replace("\\", "\\\\");
        this.mValue = this.mValue.replace(";", "\\;");
        this.mValue = this.mValue.replace(",", "\\,");
        this.mValue = this.mValue.replace("\n", "\\n");
        LogUtil.d(TAG, "escapeChar(), after mValue: " + this.mValue);
    }
}
