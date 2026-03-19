package com.mediatek.vcalendar.component;

import android.content.ContentValues;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.parameter.Value;
import com.mediatek.vcalendar.property.AAlarm;
import com.mediatek.vcalendar.property.Attendee;
import com.mediatek.vcalendar.property.DAlarm;
import com.mediatek.vcalendar.property.DtEnd;
import com.mediatek.vcalendar.property.DtStart;
import com.mediatek.vcalendar.property.Duration;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;
import com.mediatek.vcalendar.valuetype.CalAddress;
import com.mediatek.vcalendar.valuetype.DDuration;
import com.mediatek.vcalendar.valuetype.DateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VEvent extends Component {
    public VEvent() {
        super("VEVENT", null);
        LogUtil.d("VEvent", "Constructor: VEvent component created!");
    }

    @Override
    public void writeInfoToContentValues(SingleComponentContentValues singleComponentContentValues) throws VCalendarException {
        LogUtil.i("VEvent", "writeInfoToContentValues()");
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
        LogUtil.d("VEvent", "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if (!this.mPropsMap.containsKey("UID")) {
            LogUtil.w("VEvent", "VEVENT did not contains the required UID!!");
        }
        if (!this.mPropsMap.containsKey("DTSTART")) {
            throw new VCalendarException("VEVENT did not contains the required DTSTART");
        }
        if (!VCalendar.getVCalendarVersion().contains("1.0") && this.mPropsMap.containsKey("DTEND") && this.mPropsMap.containsKey("DURATION")) {
            LogUtil.e("VEvent", "writeInfoToContentValues(): DTEND DURATION cannot exist at the same VEvent");
            throw new VCalendarException("DTEND, DURATION cannot exist at the same VEvent");
        }
        for (String str : getPropertyNames()) {
            LogUtil.d("VEvent", "writeInfoToContentValues(): propertyName = " + str);
            List<Property> properties = getProperties(str);
            LogUtil.d("VEvent", "writeInfoToContentValues(): " + str + "'s count = " + properties.size());
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
            LogUtil.d("VEvent", "writeInfoToContentValues(): DTSTART value: " + dtStart);
        }
        Iterator<Component> it2 = getComponents().iterator();
        while (it2.hasNext()) {
            it2.next().writeInfoToContentValues(contentValues);
        }
        if (this.mPropsMap.containsKey("X-TIMEZONE")) {
            Property firstProperty = getFirstProperty("X-TIMEZONE");
            if (firstProperty == null || StringUtil.isNullOrEmpty(firstProperty.getValue())) {
                contentValues.put("eventTimezone", "UTC");
            } else {
                contentValues.put("eventTimezone", firstProperty.getValue());
            }
        } else if (!contentValues.containsKey("eventTimezone")) {
            contentValues.put("eventTimezone", "UTC");
        }
        LogUtil.d("VEvent", "writeInfoToContentValues(): event's EVENT_TIMEZONE:" + contentValues.getAsString("eventTimezone"));
        if (contentValues.containsKey("duration")) {
            LogUtil.d("VEvent", "writeInfoToContentValues(): Remove DTEND when event has DURATION:" + contentValues.getAsString("duration"));
            contentValues.remove("dtend");
        }
        if (contentValues.containsKey("rrule") && contentValues.containsKey("dtend")) {
            contentValues.remove("dtend");
            if (!contentValues.containsKey("duration")) {
                contentValues.put("duration", DDuration.getDurationString((getDtEnd() - getDtStart()) / 60000));
            }
        }
    }

    private void writeAlarmsContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            throw new VCalendarException("writeAlarmsContentValues(): ContentValues list is null.");
        }
        if (VCalendar.getVCalendarVersion().contains("1.0")) {
            long dtStart = getDtStart();
            LogUtil.d("VEvent", "writeAlarmsContentValues(): version 1.0 ");
            List<Property> properties = getProperties("AALARM");
            Iterator<Property> it = properties.iterator();
            while (it.hasNext()) {
                ((AAlarm) it.next()).writeInfoToContentValues(linkedList, dtStart);
            }
            for (Property property : getProperties("DALARM")) {
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
        Iterator<Property> it = getProperties("ATTENDEE").iterator();
        while (it.hasNext()) {
            ((Attendee) it.next()).writeInfoToContentValues(linkedList);
        }
    }

    public String getOrganizer() {
        LogUtil.d("VEvent", "getOrganizer(): sVersion = " + VCalendar.getVCalendarVersion());
        Property firstProperty = getFirstProperty("ORGANIZER");
        if (firstProperty != null) {
            return CalAddress.getUserMail(firstProperty.getValue());
        }
        List<Property> properties = getProperties("ATTENDEE");
        if (properties.isEmpty()) {
            LogUtil.d("VEvent", "getOrganizer(): no attendee property.");
            return null;
        }
        for (Property property : properties) {
            Parameter firstParameter = property.getFirstParameter("ROLE");
            if (firstParameter != null && ("CHAIR".equalsIgnoreCase(firstParameter.getValue()) || "ORGANIZER".equals(firstParameter.getValue()))) {
                return CalAddress.getUserMail(property.getValue());
            }
        }
        return null;
    }

    @Override
    public long getDtEnd() throws VCalendarException {
        DtEnd dtEnd = (DtEnd) getFirstProperty("DTEND");
        long valueMillis = -1;
        if (dtEnd != null) {
            try {
                return dtEnd.getValueMillis();
            } catch (VCalendarException e) {
                LogUtil.e("VEvent", "getDtEnd(): get end time failed", e);
                return -1L;
            }
        }
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart == null) {
            return -1L;
        }
        Duration duration = (Duration) getFirstProperty("DURATION");
        if (duration != null) {
            LogUtil.i("VEvent", "getDtEnd(): Can not get DtEnd, return value based on the duration.");
            try {
                valueMillis = dtStart.getValueMillis();
            } catch (VCalendarException e2) {
                LogUtil.e("VEvent", "getDtEnd(): get duration failed", e2);
            }
            return valueMillis + DDuration.getDurationMillis(duration.getValue());
        }
        LogUtil.i("VEvent", "getDtEnd(): Can not get DtEnd & Duration, return value based on the dtstart.");
        return DateTime.getUtcDateMillis(dtStart.getValue()) + 86400000;
    }

    @Override
    public long getDtStart() {
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart != null) {
            try {
                return dtStart.getValueMillis();
            } catch (VCalendarException e) {
                LogUtil.e("VEvent", "getDtStart(): get duration failed", e);
            }
        }
        return -1L;
    }

    private boolean isAllDayEvent() {
        Value value;
        LogUtil.d("VEvent", "isAllDayEvent(): sVersion = " + VCalendar.getVCalendarVersion());
        DtStart dtStart = (DtStart) getFirstProperty("DTSTART");
        if (dtStart != null && (value = (Value) dtStart.getFirstParameter("VALUE")) != null && "DATE".equals(value.getValue())) {
            LogUtil.d("VEvent", "isAllDayEvent(): TRUE.");
            return true;
        }
        Property firstProperty = getFirstProperty("X-ALLDAY");
        if (firstProperty != null && "1".equals(firstProperty.getValue())) {
            LogUtil.d("VEvent", "isAllDayEvent(): TRUE.");
            return true;
        }
        return false;
    }
}
