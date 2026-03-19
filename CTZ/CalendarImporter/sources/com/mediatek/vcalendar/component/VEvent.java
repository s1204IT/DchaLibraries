package com.mediatek.vcalendar.component;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import com.mediatek.vcalendar.ComponentPreviewInfo;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.SingleComponentCursorInfo;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.parameter.Encoding;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.parameter.Value;
import com.mediatek.vcalendar.property.AAlarm;
import com.mediatek.vcalendar.property.Attendee;
import com.mediatek.vcalendar.property.DAlarm;
import com.mediatek.vcalendar.property.Description;
import com.mediatek.vcalendar.property.DtEnd;
import com.mediatek.vcalendar.property.DtStamp;
import com.mediatek.vcalendar.property.DtStart;
import com.mediatek.vcalendar.property.Duration;
import com.mediatek.vcalendar.property.Location;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.property.RRule;
import com.mediatek.vcalendar.property.Status;
import com.mediatek.vcalendar.property.Summary;
import com.mediatek.vcalendar.property.Uid;
import com.mediatek.vcalendar.property.Version;
import com.mediatek.vcalendar.utils.CursorUtil;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;
import com.mediatek.vcalendar.utils.Utility;
import com.mediatek.vcalendar.valuetype.CalAddress;
import com.mediatek.vcalendar.valuetype.Charset;
import com.mediatek.vcalendar.valuetype.DDuration;
import com.mediatek.vcalendar.valuetype.DateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VEvent extends Component {
    public static final String CREATE_TIME_COLUMN_NAME = "createTime";
    public static final String MODIFY_TIME_COLUMN_NAME = "modifyTime";
    private static final String TAG = "VEvent";
    public static final String VEVENT_BEGIN = "BEGIN:VEVENT";
    public static final String VEVENT_END = "END:VEVENT";

    public VEvent() {
        super("VEVENT", null);
        LogUtil.d(TAG, "Constructor: VEvent component created!");
    }

    @Override
    public void compose(SingleComponentCursorInfo singleComponentCursorInfo) throws VCalendarException {
        if (singleComponentCursorInfo == null) {
            throw new VCalendarException("compose(): SingleComponentCursorInfo is null in component: " + this.mName);
        }
        compose(singleComponentCursorInfo.cursor);
        singleComponentCursorInfo.cursor.close();
        Cursor cursor = singleComponentCursorInfo.remindersCursor;
        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                VAlarm vAlarm = new VAlarm(this);
                MatrixCursor matrixCursorCopyCurrentRow = CursorUtil.copyCurrentRow(cursor);
                vAlarm.compose(matrixCursorCopyCurrentRow);
                matrixCursorCopyCurrentRow.close();
                addChild(vAlarm);
                cursor.moveToNext();
            }
            cursor.close();
        }
        Cursor cursor2 = singleComponentCursorInfo.attendeesCursor;
        if (cursor2 != null) {
            cursor2.moveToFirst();
            for (int i2 = 0; i2 < cursor2.getCount(); i2++) {
                Attendee attendee = new Attendee(null);
                MatrixCursor matrixCursorCopyCurrentRow2 = CursorUtil.copyCurrentRow(cursor2);
                attendee.compose(matrixCursorCopyCurrentRow2, this);
                matrixCursorCopyCurrentRow2.close();
                addProperty(attendee);
                cursor2.moveToNext();
            }
            cursor2.close();
        }
    }

    @Override
    protected void compose(Cursor cursor) throws VCalendarException {
        String userCalAddress;
        LogUtil.i(TAG, "compose(): compose a VEVENT.");
        super.compose(cursor);
        Parameter parameter = new Parameter(Parameter.ENCODING, Encoding.QUOTED_PRINTABLE);
        Parameter parameter2 = new Parameter(Parameter.CHARSET, Charset.UTF8);
        String string = cursor.getString(cursor.getColumnIndex("_id"));
        if (!StringUtil.isNullOrEmpty(string)) {
            addProperty(new Uid(string));
        }
        String string2 = cursor.getString(cursor.getColumnIndex("title"));
        if (!StringUtil.isNullOrEmpty(string2)) {
            Summary summary = new Summary(string2);
            if (Utility.needQpEncode()) {
                summary.addParameter(parameter);
            }
            summary.addParameter(parameter2);
            addProperty(summary);
        }
        if (Build.VERSION.SDK_INT <= 15) {
            LogUtil.i(TAG, "compose(): OS Version is <=15.");
        } else {
            LogUtil.i(TAG, "compose(): OS Version is > 15.");
            if (cursor.getColumnIndex(CREATE_TIME_COLUMN_NAME) < 0) {
                throw new VCalendarException("Cannot create DtStamp, the needed \"createTime\"  does not exist in DB.");
            }
        }
        String string3 = cursor.getString(cursor.getColumnIndex(MODIFY_TIME_COLUMN_NAME));
        if (string3 == null) {
            string3 = cursor.getString(cursor.getColumnIndex(CREATE_TIME_COLUMN_NAME));
        }
        if (!StringUtil.isNullOrEmpty(string3)) {
            addProperty(new DtStamp(DateTime.getUtcTimeString(Long.parseLong(string3))));
        }
        String statusString = Status.getStatusString(cursor.getInt(cursor.getColumnIndex("eventStatus")));
        if (statusString != null) {
            addProperty(new Status(statusString));
        }
        String string4 = cursor.getString(cursor.getColumnIndex("organizer"));
        if (string4 != null && (userCalAddress = CalAddress.getUserCalAddress(string4)) != null) {
            addProperty(new Property("ORGANIZER", userCalAddress));
        }
        String string5 = cursor.getString(cursor.getColumnIndex("eventLocation"));
        if (!StringUtil.isNullOrEmpty(string5)) {
            Location location = new Location(string5);
            if (Utility.needQpEncode()) {
                location.addParameter(parameter);
            }
            location.addParameter(parameter2);
            addProperty(location);
        }
        String string6 = cursor.getString(cursor.getColumnIndex("description"));
        if (!StringUtil.isNullOrEmpty(string6)) {
            Description description = new Description(string6.replaceAll(Component.NEWLINE, "\n"));
            if (Utility.needQpEncode()) {
                description.addParameter(parameter);
            }
            description.addParameter(parameter2);
            addProperty(description);
        }
        boolean z = cursor.getInt(cursor.getColumnIndex("allDay")) == 1;
        String string7 = cursor.getString(cursor.getColumnIndex("allDay"));
        if (!StringUtil.isNullOrEmpty(string7)) {
            addProperty(new Property(Property.X_ALLDAY, string7));
        }
        String string8 = cursor.getString(cursor.getColumnIndex("eventTimezone"));
        if (!StringUtil.isNullOrEmpty(string8)) {
            addProperty(new Property(Property.X_TIMEZONE, string8));
        } else {
            string8 = DateTime.UTC;
        }
        int columnIndex = cursor.getColumnIndex("dtstart");
        if (cursor.isNull(columnIndex)) {
            throw new VCalendarException("Cannot create DtStart, the needed \"DtStart\" does not exist in DB.");
        }
        DtStart dtStart = new DtStart(DateTime.getUtcTimeString(cursor.getLong(columnIndex)));
        if (!z && Utility.needTzIdParameter()) {
            dtStart.addParameter(new Parameter("TZID", string8));
        }
        addProperty(dtStart);
        String string9 = cursor.getString(cursor.getColumnIndex("duration"));
        if (!StringUtil.isNullOrEmpty(string9)) {
            addProperty(new Duration(string9));
        }
        if (!this.mPropsMap.containsKey("DURATION")) {
            int columnIndex2 = cursor.getColumnIndex("dtend");
            if (!cursor.isNull(columnIndex2)) {
                DtEnd dtEnd = new DtEnd(DateTime.getUtcTimeString(cursor.getLong(columnIndex2)));
                if (!z && Utility.needTzIdParameter()) {
                    dtEnd.addParameter(new Parameter("TZID", string8));
                }
                addProperty(dtEnd);
            }
        }
        String string10 = cursor.getString(cursor.getColumnIndex("rrule"));
        if (!StringUtil.isNullOrEmpty(string10)) {
            addProperty(new RRule(string10));
        }
    }

    @Override
    public void fillPreviewInfo(ComponentPreviewInfo componentPreviewInfo) throws VCalendarException {
        LogUtil.i(TAG, "fillPreviewInfo()");
        if (componentPreviewInfo == null) {
            throw new VCalendarException("fillPreviewInfo(): ComponentPreviewInfo is null.");
        }
        componentPreviewInfo.eventSummary = getTitle();
        componentPreviewInfo.eventOrganizer = getOrganizer();
        componentPreviewInfo.eventStartTime = getDtStart();
        componentPreviewInfo.eventDuration = getTime(this.mContext);
    }

    @Override
    public void writeInfoToContentValues(SingleComponentContentValues singleComponentContentValues) throws VCalendarException {
        LogUtil.i(TAG, "writeInfoToContentValues()");
        if (singleComponentContentValues == null) {
            throw new VCalendarException("writeInfoToContentValues(): SingleComponentContentValues is null.");
        }
        singleComponentContentValues.componentType = "VEVENT";
        String organizer = getOrganizer();
        if (organizer != null) {
            singleComponentContentValues.contentValues.put("organizer", organizer);
        }
        writeInfoToContentValues(singleComponentContentValues.contentValues);
        writeAlarmsContentValues(singleComponentContentValues.alarmValuesList);
        writeAttendeesContentValues(singleComponentContentValues.attendeeValuesList);
    }

    @Override
    protected void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if (!this.mPropsMap.containsKey(Property.UID)) {
            LogUtil.w(TAG, "VEVENT did not contains the required UID!!");
        }
        if (!this.mPropsMap.containsKey("DTSTART")) {
            throw new VCalendarException("VEVENT did not contains the required DTSTART");
        }
        if (!VCalendar.getVCalendarVersion().contains(Version.VERSION10) && this.mPropsMap.containsKey("DTEND") && this.mPropsMap.containsKey("DURATION")) {
            LogUtil.e(TAG, "writeInfoToContentValues(): DTEND DURATION cannot exist at the same VEvent");
            throw new VCalendarException("DTEND, DURATION cannot exist at the same VEvent");
        }
        for (String str : getPropertyNames()) {
            LogUtil.d(TAG, "writeInfoToContentValues(): propertyName = " + str);
            List<Property> properties = getProperties(str);
            LogUtil.d(TAG, "writeInfoToContentValues(): " + str + "'s count = " + properties.size());
            Iterator<Property> it = properties.iterator();
            while (it.hasNext()) {
                it.next().writeInfoToContentValues(contentValues);
            }
        }
        if (isAllDayEvent()) {
            contentValues.put("allDay", (Integer) 1);
        }
        if (this.mPropsMap.containsKey("DTSTART") && !this.mPropsMap.containsKey("DTEND") && !this.mPropsMap.containsKey("DURATION")) {
            DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
            if (dtStart == null) {
                throw new VCalendarException("DTSTART time is needed!");
            }
            contentValues.put("dtend", Long.valueOf(DateTime.getUtcDateMillis(dtStart.getValue()) + 86400000));
            LogUtil.d(TAG, "writeInfoToContentValues(): DTSTART value: " + dtStart);
        }
        Iterator<Component> it2 = getComponents().iterator();
        while (it2.hasNext()) {
            it2.next().writeInfoToContentValues(contentValues);
        }
        if (this.mPropsMap.containsKey(Property.X_TIMEZONE)) {
            Property firstProperty = getFirstProperty(Property.X_TIMEZONE);
            if (firstProperty == null || StringUtil.isNullOrEmpty(firstProperty.getValue())) {
                contentValues.put("eventTimezone", DateTime.UTC);
            } else {
                contentValues.put("eventTimezone", firstProperty.getValue());
            }
        } else if (!contentValues.containsKey("eventTimezone")) {
            contentValues.put("eventTimezone", DateTime.UTC);
        }
        LogUtil.d(TAG, "writeInfoToContentValues(): event's EVENT_TIMEZONE:" + contentValues.getAsString("eventTimezone"));
        if (contentValues.containsKey("duration")) {
            LogUtil.d(TAG, "writeInfoToContentValues(): Remove DTEND when event has DURATION:" + contentValues.getAsString("duration"));
            contentValues.remove("dtend");
        }
        if (contentValues.containsKey("rrule") && contentValues.containsKey("dtend")) {
            contentValues.remove("dtend");
            if (!contentValues.containsKey("duration")) {
                contentValues.put("duration", DDuration.getDurationString((getDtEnd() - getDtStart()) / DDuration.MILLIS_IN_MIN));
            }
        }
    }

    private void writeAlarmsContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            throw new VCalendarException("writeAlarmsContentValues(): ContentValues list is null.");
        }
        if (VCalendar.getVCalendarVersion().contains(Version.VERSION10)) {
            long dtStart = getDtStart();
            LogUtil.d(TAG, "writeAlarmsContentValues(): version 1.0 ");
            List<Property> properties = getProperties(Property.AALARM);
            Iterator<Property> it = properties.iterator();
            while (it.hasNext()) {
                ((AAlarm) it.next()).writeInfoToContentValues(linkedList, dtStart);
            }
            for (Property property : getProperties(Property.DALARM)) {
                String value = property.getValue();
                Iterator<Property> it2 = properties.iterator();
                while (it2.hasNext() && !it2.next().getValue().equalsIgnoreCase(value)) {
                    ((DAlarm) property).writeInfoToContentValues(linkedList, dtStart);
                }
            }
            return;
        }
        Iterator<Component> it3 = getComponents().iterator();
        while (it3.hasNext()) {
            it3.next().writeInfoToContentValues(linkedList);
        }
    }

    private void writeAttendeesContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            throw new VCalendarException("writeAttendeesContentValues(): ContentValues list is null.");
        }
        Iterator<Property> it = getProperties(Property.ATTENDEE).iterator();
        while (it.hasNext()) {
            ((Attendee) it.next()).writeInfoToContentValues(linkedList);
        }
    }

    @Override
    public String getTitle() {
        Summary summary = (Summary) getFirstProperty(Property.SUMMARY);
        if (summary == null) {
            return null;
        }
        return summary.getValue();
    }

    public String getOrganizer() {
        LogUtil.d(TAG, "getOrganizer(): sVersion = " + VCalendar.getVCalendarVersion());
        Property firstProperty = getFirstProperty("ORGANIZER");
        if (firstProperty != null) {
            return CalAddress.getUserMail(firstProperty.getValue());
        }
        List<Property> properties = getProperties(Property.ATTENDEE);
        if (properties.isEmpty()) {
            LogUtil.d(TAG, "getOrganizer(): no attendee property.");
            return null;
        }
        for (Property property : properties) {
            Parameter firstParameter = property.getFirstParameter(Parameter.ROLE);
            if (firstParameter != null && ("CHAIR".equalsIgnoreCase(firstParameter.getValue()) || "ORGANIZER".equals(firstParameter.getValue()))) {
                return CalAddress.getUserMail(property.getValue());
            }
        }
        return null;
    }

    public String getTime(Context context) throws VCalendarException {
        int i;
        if (isAllDayEvent()) {
            i = 8210;
        } else {
            i = 17;
            if (DateFormat.is24HourFormat(context)) {
                i = 145;
            }
        }
        return DateUtils.formatDateRange(context, getDtStart(), getDtEnd(), i);
    }

    @Override
    public long getDtEnd() throws VCalendarException {
        DtEnd dtEnd = (DtEnd) getFirstProperty("DTEND");
        long valueMillis = -1;
        if (dtEnd != null) {
            try {
                return dtEnd.getValueMillis();
            } catch (VCalendarException e) {
                LogUtil.e(TAG, "getDtEnd(): get end time failed", e);
                return -1L;
            }
        }
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart == null) {
            return -1L;
        }
        Duration duration = (Duration) getFirstProperty("DURATION");
        if (duration != null) {
            LogUtil.i(TAG, "getDtEnd(): Can not get DtEnd, return value based on the duration.");
            try {
                valueMillis = dtStart.getValueMillis();
            } catch (VCalendarException e2) {
                LogUtil.e(TAG, "getDtEnd(): get duration failed", e2);
            }
            return valueMillis + DDuration.getDurationMillis(duration.getValue());
        }
        LogUtil.i(TAG, "getDtEnd(): Can not get DtEnd & Duration, return value based on the dtstart.");
        return DateTime.getUtcDateMillis(dtStart.getValue()) + 86400000;
    }

    @Override
    public long getDtStart() {
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart != null) {
            try {
                return dtStart.getValueMillis();
            } catch (VCalendarException e) {
                LogUtil.e(TAG, "getDtStart(): get duration failed", e);
            }
        }
        return -1L;
    }

    private boolean isAllDayEvent() {
        Value value;
        LogUtil.d(TAG, "isAllDayEvent(): sVersion = " + VCalendar.getVCalendarVersion());
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart != null && (value = (Value) dtStart.getFirstParameter(Parameter.VALUE)) != null && Value.DATE.equals(value.getValue())) {
            LogUtil.d(TAG, "isAllDayEvent(): TRUE.");
            return true;
        }
        Property firstProperty = getFirstProperty(Property.X_ALLDAY);
        if (firstProperty != null && "1".equals(firstProperty.getValue())) {
            LogUtil.d(TAG, "isAllDayEvent(): TRUE.");
            return true;
        }
        return false;
    }
}
