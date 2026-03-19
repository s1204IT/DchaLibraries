package com.android.calendarcommon2;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.ICalendar;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.parameter.Value;
import com.mediatek.vcalendar.valuetype.DateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RecurrenceSet {
    private static final String FOLDING_SEPARATOR = "\n ";
    private static final String RULE_SEPARATOR = "\n";
    private static final String TAG = "RecurrenceSet";
    private static final Pattern IGNORABLE_ICAL_WHITESPACE_RE = Pattern.compile("(?:\\r\\n?|\\n)[ \t]");
    private static final Pattern FOLD_RE = Pattern.compile(".{75}");
    public EventRecurrence[] rrules = null;
    public long[] rdates = null;
    public EventRecurrence[] exrules = null;
    public long[] exdates = null;

    public RecurrenceSet(ContentValues contentValues) throws EventRecurrence.InvalidFormatException {
        init(contentValues.getAsString("rrule"), contentValues.getAsString("rdate"), contentValues.getAsString("exrule"), contentValues.getAsString("exdate"));
    }

    public RecurrenceSet(Cursor cursor) throws EventRecurrence.InvalidFormatException {
        init(cursor.getString(cursor.getColumnIndex("rrule")), cursor.getString(cursor.getColumnIndex("rdate")), cursor.getString(cursor.getColumnIndex("exrule")), cursor.getString(cursor.getColumnIndex("exdate")));
    }

    public RecurrenceSet(String str, String str2, String str3, String str4) throws EventRecurrence.InvalidFormatException {
        init(str, str2, str3, str4);
    }

    private void init(String str, String str2, String str3, String str4) throws EventRecurrence.InvalidFormatException {
        if (!TextUtils.isEmpty(str) || !TextUtils.isEmpty(str2)) {
            this.rrules = parseMultiLineRecurrenceRules(str);
            this.rdates = parseMultiLineRecurrenceDates(str2);
            this.exrules = parseMultiLineRecurrenceRules(str3);
            this.exdates = parseMultiLineRecurrenceDates(str4);
        }
    }

    private EventRecurrence[] parseMultiLineRecurrenceRules(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split(RULE_SEPARATOR);
        EventRecurrence[] eventRecurrenceArr = new EventRecurrence[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(strArrSplit[i]);
            eventRecurrenceArr[i] = eventRecurrence;
        }
        return eventRecurrenceArr;
    }

    private long[] parseMultiLineRecurrenceDates(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (String str2 : str.split(RULE_SEPARATOR)) {
            for (long j : parseRecurrenceDates(str2)) {
                arrayList.add(Long.valueOf(j));
            }
        }
        long[] jArr = new long[arrayList.size()];
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            jArr[i] = ((Long) arrayList.get(i)).longValue();
        }
        return jArr;
    }

    public boolean hasRecurrence() {
        return (this.rrules == null && this.rdates == null) ? false : true;
    }

    public static long[] parseRecurrenceDates(String str) throws EventRecurrence.InvalidFormatException {
        String strSubstring = DateTime.UTC;
        int iIndexOf = str.indexOf(";");
        if (iIndexOf != -1) {
            strSubstring = str.substring(0, iIndexOf);
            str = str.substring(iIndexOf + 1);
        }
        Time time = new Time(strSubstring);
        String[] strArrSplit = str.split(",");
        int length = strArrSplit.length;
        long[] jArr = new long[length];
        for (int i = 0; i < length; i++) {
            try {
                time.parse(strArrSplit[i]);
                jArr[i] = time.toMillis(false);
                time.timezone = strSubstring;
            } catch (TimeFormatException e) {
                throw new EventRecurrence.InvalidFormatException("TimeFormatException thrown when parsing time " + strArrSplit[i] + " in recurrence " + str);
            }
        }
        return jArr;
    }

    public static boolean populateContentValues(ICalendar.Component component, ContentValues contentValues) {
        try {
            ICalendar.Property firstProperty = component.getFirstProperty("DTSTART");
            String value = firstProperty.getValue();
            ICalendar.Parameter firstParameter = firstProperty.getFirstParameter("TZID");
            String str = firstParameter == null ? null : firstParameter.value;
            Time time = new Time(firstParameter == null ? DateTime.UTC : str);
            boolean z = time.parse(value);
            boolean z2 = time.allDay;
            if (z || z2) {
                str = DateTime.UTC;
            }
            String strComputeDuration = computeDuration(time, component);
            String strFlattenProperties = flattenProperties(component, "RRULE");
            String strExtractDates = extractDates(component.getFirstProperty("RDATE"));
            String strFlattenProperties2 = flattenProperties(component, "EXRULE");
            String strExtractDates2 = extractDates(component.getFirstProperty("EXDATE"));
            if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(strComputeDuration) && (!TextUtils.isEmpty(strFlattenProperties) || !TextUtils.isEmpty(strExtractDates))) {
                if (z2) {
                    time.timezone = DateTime.UTC;
                }
                long millis = time.toMillis(false);
                contentValues.put("dtstart", Long.valueOf(millis));
                if (millis == -1) {
                    return false;
                }
                contentValues.put("rrule", strFlattenProperties);
                contentValues.put("rdate", strExtractDates);
                contentValues.put("exrule", strFlattenProperties2);
                contentValues.put("exdate", strExtractDates2);
                contentValues.put("eventTimezone", str);
                contentValues.put("duration", strComputeDuration);
                contentValues.put("allDay", Integer.valueOf(z2 ? 1 : 0));
                return true;
            }
            return false;
        } catch (TimeFormatException e) {
            Log.i(TAG, "Failed to parse event: " + component.toString());
            return false;
        }
    }

    public static boolean populateComponent(Cursor cursor, ICalendar.Component component) {
        long j;
        Time time;
        int columnIndex = cursor.getColumnIndex("dtstart");
        int columnIndex2 = cursor.getColumnIndex("duration");
        int columnIndex3 = cursor.getColumnIndex("eventTimezone");
        int columnIndex4 = cursor.getColumnIndex("rrule");
        int columnIndex5 = cursor.getColumnIndex("rdate");
        int columnIndex6 = cursor.getColumnIndex("exrule");
        int columnIndex7 = cursor.getColumnIndex("exdate");
        int columnIndex8 = cursor.getColumnIndex("allDay");
        if (!cursor.isNull(columnIndex)) {
            j = cursor.getLong(columnIndex);
        } else {
            j = -1;
        }
        String string = cursor.getString(columnIndex2);
        String string2 = cursor.getString(columnIndex3);
        String string3 = cursor.getString(columnIndex4);
        String string4 = cursor.getString(columnIndex5);
        String string5 = cursor.getString(columnIndex6);
        String string6 = cursor.getString(columnIndex7);
        boolean z = cursor.getInt(columnIndex8) == 1;
        if (j == -1 || TextUtils.isEmpty(string) || (TextUtils.isEmpty(string3) && TextUtils.isEmpty(string4))) {
            return false;
        }
        ICalendar.Property property = new ICalendar.Property("DTSTART");
        if (!TextUtils.isEmpty(string2)) {
            if (!z) {
                property.addParameter(new ICalendar.Parameter("TZID", string2));
            }
            time = new Time(string2);
        } else {
            time = new Time(DateTime.UTC);
        }
        time.set(j);
        if (z) {
            property.addParameter(new ICalendar.Parameter(Parameter.VALUE, Value.DATE));
            time.allDay = true;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
        }
        property.setValue(time.format2445());
        component.addProperty(property);
        ICalendar.Property property2 = new ICalendar.Property("DURATION");
        property2.setValue(string);
        component.addProperty(property2);
        addPropertiesForRuleStr(component, "RRULE", string3);
        addPropertyForDateStr(component, "RDATE", string4);
        addPropertiesForRuleStr(component, "EXRULE", string5);
        addPropertyForDateStr(component, "EXDATE", string6);
        return true;
    }

    public static boolean populateComponent(ContentValues contentValues, ICalendar.Component component) {
        long jLongValue;
        Time time;
        if (contentValues.containsKey("dtstart")) {
            jLongValue = contentValues.getAsLong("dtstart").longValue();
        } else {
            jLongValue = -1;
        }
        String asString = contentValues.getAsString("duration");
        String asString2 = contentValues.getAsString("eventTimezone");
        String asString3 = contentValues.getAsString("rrule");
        String asString4 = contentValues.getAsString("rdate");
        String asString5 = contentValues.getAsString("exrule");
        String asString6 = contentValues.getAsString("exdate");
        Integer asInteger = contentValues.getAsInteger("allDay");
        boolean z = asInteger != null && asInteger.intValue() == 1;
        if (jLongValue == -1 || TextUtils.isEmpty(asString) || (TextUtils.isEmpty(asString3) && TextUtils.isEmpty(asString4))) {
            return false;
        }
        ICalendar.Property property = new ICalendar.Property("DTSTART");
        if (!TextUtils.isEmpty(asString2)) {
            if (!z) {
                property.addParameter(new ICalendar.Parameter("TZID", asString2));
            }
            time = new Time(asString2);
        } else {
            time = new Time(DateTime.UTC);
        }
        time.set(jLongValue);
        if (z) {
            property.addParameter(new ICalendar.Parameter(Parameter.VALUE, Value.DATE));
            time.allDay = true;
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
        }
        property.setValue(time.format2445());
        component.addProperty(property);
        ICalendar.Property property2 = new ICalendar.Property("DURATION");
        property2.setValue(asString);
        component.addProperty(property2);
        addPropertiesForRuleStr(component, "RRULE", asString3);
        addPropertyForDateStr(component, "RDATE", asString4);
        addPropertiesForRuleStr(component, "EXRULE", asString5);
        addPropertyForDateStr(component, "EXDATE", asString6);
        return true;
    }

    public static void addPropertiesForRuleStr(ICalendar.Component component, String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return;
        }
        for (String str3 : getRuleStrings(str2)) {
            ICalendar.Property property = new ICalendar.Property(str);
            property.setValue(str3);
            component.addProperty(property);
        }
    }

    private static String[] getRuleStrings(String str) {
        if (str == null) {
            return new String[0];
        }
        String[] strArrSplit = unfold(str).split(RULE_SEPARATOR);
        int length = strArrSplit.length;
        for (int i = 0; i < length; i++) {
            strArrSplit[i] = fold(strArrSplit[i]);
        }
        return strArrSplit;
    }

    public static String fold(String str) {
        return FOLD_RE.matcher(str).replaceAll("$0\r\n ");
    }

    public static String unfold(String str) {
        return IGNORABLE_ICAL_WHITESPACE_RE.matcher(str).replaceAll(LoggingEvents.EXTRA_CALLING_APP_NAME);
    }

    public static void addPropertyForDateStr(ICalendar.Component component, String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return;
        }
        ICalendar.Property property = new ICalendar.Property(str);
        String strSubstring = null;
        int iIndexOf = str2.indexOf(";");
        if (iIndexOf != -1) {
            strSubstring = str2.substring(0, iIndexOf);
            str2 = str2.substring(iIndexOf + 1);
        }
        if (!TextUtils.isEmpty(strSubstring)) {
            property.addParameter(new ICalendar.Parameter("TZID", strSubstring));
        }
        property.setValue(str2);
        component.addProperty(property);
    }

    private static String computeDuration(Time time, ICalendar.Component component) {
        ICalendar.Property firstProperty = component.getFirstProperty("DURATION");
        if (firstProperty != null) {
            return firstProperty.getValue();
        }
        ICalendar.Property firstProperty2 = component.getFirstProperty("DTEND");
        if (firstProperty2 == null) {
            return "+P0S";
        }
        ICalendar.Parameter firstParameter = firstProperty2.getFirstParameter("TZID");
        Time time2 = new Time(firstParameter == null ? time.timezone : firstParameter.value);
        time2.parse(firstProperty2.getValue());
        long millis = (time2.toMillis(false) - time.toMillis(false)) / 1000;
        if (time.allDay && millis % 86400 == 0) {
            return "P" + (millis / 86400) + "D";
        }
        return "P" + millis + "S";
    }

    private static String flattenProperties(ICalendar.Component component, String str) {
        List<ICalendar.Property> properties = component.getProperties(str);
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        boolean z = true;
        if (properties.size() == 1) {
            return properties.get(0).getValue();
        }
        StringBuilder sb = new StringBuilder();
        for (ICalendar.Property property : component.getProperties(str)) {
            if (!z) {
                sb.append(RULE_SEPARATOR);
            } else {
                z = false;
            }
            sb.append(property.getValue());
        }
        return sb.toString();
    }

    private static String extractDates(ICalendar.Property property) {
        if (property == null) {
            return null;
        }
        ICalendar.Parameter firstParameter = property.getFirstParameter("TZID");
        if (firstParameter != null) {
            return firstParameter.value + ";" + property.getValue();
        }
        return property.getValue();
    }
}
