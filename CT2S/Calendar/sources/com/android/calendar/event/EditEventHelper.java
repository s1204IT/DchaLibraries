package com.android.calendar.event;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.View;
import com.android.calendar.AbstractCalendarActivity;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.Utils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.common.Rfc822Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TimeZone;

public class EditEventHelper {
    protected boolean mEventOk;
    private EventRecurrence mEventRecurrence;
    private final AsyncQueryService mService;
    public static final String[] EVENT_PROJECTION = {"_id", "title", "description", "eventLocation", "allDay", "hasAlarm", "calendar_id", "dtstart", "dtend", "duration", "eventTimezone", "rrule", "_sync_id", "availability", "accessLevel", "ownerAccount", "hasAttendeeData", "original_sync_id", "organizer", "guestsCanModify", "original_id", "eventStatus", "calendar_color", "eventColor", "eventColor_index"};
    public static final String[] REMINDERS_PROJECTION = {"_id", "minutes", "method"};
    public static final int[] ATTENDEE_VALUES = {0, 1, 4, 2};
    static final String[] CALENDARS_PROJECTION = {"_id", "calendar_displayName", "ownerAccount", "calendar_color", "canOrganizerRespond", "calendar_access_level", "visible", "maxReminders", "allowedReminders", "allowedAttendeeTypes", "allowedAvailability", "account_name", "account_type"};
    static final String[] COLORS_PROJECTION = {"_id", "account_name", "account_type", "color", "color_index"};
    static final String[] ATTENDEES_PROJECTION = {"_id", "attendeeName", "attendeeEmail", "attendeeRelationship", "attendeeStatus"};

    public interface EditDoneRunnable extends Runnable {
        void setDoneCode(int i);
    }

    public static class AttendeeItem {
        public CalendarEventModel.Attendee mAttendee;
        public Drawable mBadge;
        public Uri mContactLookupUri;
        public boolean mRemoved;
        public int mUpdateCounts;
        public View mView;

        public AttendeeItem(CalendarEventModel.Attendee attendee, Drawable badge) {
            this.mAttendee = attendee;
            this.mBadge = badge;
        }
    }

    public EditEventHelper(Context context) {
        this.mEventRecurrence = new EventRecurrence();
        this.mEventOk = true;
        this.mService = ((AbstractCalendarActivity) context).getAsyncQueryService();
    }

    public EditEventHelper(Context context, CalendarEventModel model) {
        this(context);
    }

    public boolean saveEvent(CalendarEventModel model, CalendarEventModel originalModel, int modifyWhich) {
        ArrayList<CalendarEventModel.ReminderEntry> originalReminders;
        String originalAttendeesString;
        ContentProviderOperation.Builder b;
        ContentProviderOperation.Builder b2;
        boolean forceSaveReminders = false;
        if (!this.mEventOk) {
            return false;
        }
        if (model == null) {
            Log.e("EditEventHelper", "Attempted to save null model.");
            return false;
        }
        if (!model.isValid()) {
            Log.e("EditEventHelper", "Attempted to save invalid model.");
            return false;
        }
        if (originalModel != null && !isSameEvent(model, originalModel)) {
            Log.e("EditEventHelper", "Attempted to update existing event but models didn't refer to the same event.");
            return false;
        }
        if (originalModel != null && model.isUnchanged(originalModel)) {
            return false;
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int eventIdIndex = -1;
        ContentValues values = getContentValuesFromModel(model);
        if (model.mUri != null && originalModel == null) {
            Log.e("EditEventHelper", "Existing event but no originalModel provided. Aborting save.");
            return false;
        }
        Uri uri = null;
        if (model.mUri != null) {
            uri = Uri.parse(model.mUri);
        }
        ArrayList<CalendarEventModel.ReminderEntry> reminders = model.mReminders;
        int len = reminders.size();
        values.put("hasAlarm", Integer.valueOf(len > 0 ? 1 : 0));
        if (uri == null) {
            values.put("hasAttendeeData", (Integer) 1);
            values.put("eventStatus", (Integer) 1);
            eventIdIndex = ops.size();
            ContentProviderOperation.Builder b3 = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values);
            ops.add(b3.build());
            forceSaveReminders = true;
        } else if (TextUtils.isEmpty(model.mRrule) && TextUtils.isEmpty(originalModel.mRrule)) {
            checkTimeDependentFields(originalModel, model, values, modifyWhich);
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
        } else if (TextUtils.isEmpty(originalModel.mRrule)) {
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
        } else if (modifyWhich == 1) {
            long begin = model.mOriginalStart;
            values.put("original_sync_id", originalModel.mSyncId);
            values.put("originalInstanceTime", Long.valueOf(begin));
            boolean allDay = originalModel.mAllDay;
            values.put("originalAllDay", Integer.valueOf(allDay ? 1 : 0));
            values.put("eventStatus", Integer.valueOf(originalModel.mEventStatus));
            eventIdIndex = ops.size();
            ContentProviderOperation.Builder b4 = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values);
            ops.add(b4.build());
            forceSaveReminders = true;
        } else if (modifyWhich == 2) {
            if (TextUtils.isEmpty(model.mRrule)) {
                if (isFirstEventInSeries(model, originalModel)) {
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                } else {
                    updatePastEvents(ops, originalModel, model.mOriginalStart);
                }
                eventIdIndex = ops.size();
                values.put("eventStatus", Integer.valueOf(originalModel.mEventStatus));
                ops.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values).build());
            } else if (isFirstEventInSeries(model, originalModel)) {
                checkTimeDependentFields(originalModel, model, values, modifyWhich);
                ContentProviderOperation.Builder b5 = ContentProviderOperation.newUpdate(uri).withValues(values);
                ops.add(b5.build());
            } else {
                String newRrule = updatePastEvents(ops, originalModel, model.mOriginalStart);
                if (model.mRrule.equals(originalModel.mRrule)) {
                    values.put("rrule", newRrule);
                }
                eventIdIndex = ops.size();
                values.put("eventStatus", Integer.valueOf(originalModel.mEventStatus));
                ops.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values).build());
            }
            forceSaveReminders = true;
        } else if (modifyWhich == 3) {
            if (TextUtils.isEmpty(model.mRrule)) {
                ops.add(ContentProviderOperation.newDelete(uri).build());
                eventIdIndex = ops.size();
                ops.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(values).build());
                forceSaveReminders = true;
            } else {
                checkTimeDependentFields(originalModel, model, values, modifyWhich);
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
            }
        }
        boolean newEvent = eventIdIndex != -1;
        if (originalModel != null) {
            originalReminders = originalModel.mReminders;
        } else {
            originalReminders = new ArrayList<>();
        }
        if (newEvent) {
            saveRemindersWithBackRef(ops, eventIdIndex, reminders, originalReminders, forceSaveReminders);
        } else if (uri != null) {
            saveReminders(ops, ContentUris.parseId(uri), reminders, originalReminders, forceSaveReminders);
        }
        boolean hasAttendeeData = model.mHasAttendeeData;
        if (hasAttendeeData && model.mOwnerAttendeeId == -1) {
            String ownerEmail = model.mOwnerAccount;
            if (model.mAttendeesList.size() != 0 && Utils.isValidEmail(ownerEmail)) {
                values.clear();
                values.put("attendeeEmail", ownerEmail);
                values.put("attendeeRelationship", (Integer) 2);
                values.put("attendeeType", (Integer) 1);
                values.put("attendeeStatus", (Integer) 1);
                if (newEvent) {
                    b2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(values);
                    b2.withValueBackReference("event_id", eventIdIndex);
                } else {
                    values.put("event_id", Long.valueOf(model.mId));
                    b2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(values);
                }
                ops.add(b2.build());
            }
        } else if (hasAttendeeData && model.mSelfAttendeeStatus != originalModel.mSelfAttendeeStatus && model.mOwnerAttendeeId != -1) {
            Uri attUri = ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, model.mOwnerAttendeeId);
            values.clear();
            values.put("attendeeStatus", Integer.valueOf(model.mSelfAttendeeStatus));
            values.put("event_id", Long.valueOf(model.mId));
            ContentProviderOperation.Builder b6 = ContentProviderOperation.newUpdate(attUri).withValues(values);
            ops.add(b6.build());
        }
        if (hasAttendeeData && (newEvent || uri != null)) {
            String attendees = model.getAttendeesString();
            if (originalModel != null) {
                originalAttendeesString = originalModel.getAttendeesString();
            } else {
                originalAttendeesString = "";
            }
            if (newEvent || !TextUtils.equals(originalAttendeesString, attendees)) {
                HashMap<String, CalendarEventModel.Attendee> newAttendees = model.mAttendeesList;
                LinkedList<String> removedAttendees = new LinkedList<>();
                long eventId = uri != null ? ContentUris.parseId(uri) : -1L;
                if (!newEvent) {
                    removedAttendees.clear();
                    HashMap<String, CalendarEventModel.Attendee> originalAttendees = originalModel.mAttendeesList;
                    for (String originalEmail : originalAttendees.keySet()) {
                        if (newAttendees.containsKey(originalEmail)) {
                            newAttendees.remove(originalEmail);
                        } else {
                            removedAttendees.add(originalEmail);
                        }
                    }
                    if (removedAttendees.size() > 0) {
                        ContentProviderOperation.Builder b7 = ContentProviderOperation.newDelete(CalendarContract.Attendees.CONTENT_URI);
                        String[] args = new String[removedAttendees.size() + 1];
                        args[0] = Long.toString(eventId);
                        int i = 1;
                        StringBuilder deleteWhere = new StringBuilder("event_id=? AND attendeeEmail IN (");
                        for (String removedAttendee : removedAttendees) {
                            if (i > 1) {
                                deleteWhere.append(",");
                            }
                            deleteWhere.append("?");
                            args[i] = removedAttendee;
                            i++;
                        }
                        deleteWhere.append(")");
                        b7.withSelection(deleteWhere.toString(), args);
                        ops.add(b7.build());
                    }
                }
                if (newAttendees.size() > 0) {
                    for (CalendarEventModel.Attendee attendee : newAttendees.values()) {
                        values.clear();
                        values.put("attendeeName", attendee.mName);
                        values.put("attendeeEmail", attendee.mEmail);
                        values.put("attendeeRelationship", (Integer) 1);
                        values.put("attendeeType", (Integer) 1);
                        values.put("attendeeStatus", (Integer) 0);
                        if (newEvent) {
                            b = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(values);
                            b.withValueBackReference("event_id", eventIdIndex);
                        } else {
                            values.put("event_id", Long.valueOf(eventId));
                            b = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(values);
                        }
                        ops.add(b.build());
                    }
                }
            }
        }
        this.mService.startBatch(this.mService.getNextToken(), null, "com.android.calendar", ops, 0L);
        return true;
    }

    public static LinkedHashSet<Rfc822Token> getAddressesFromList(String list, Rfc822Validator validator) {
        LinkedHashSet<Rfc822Token> addresses = new LinkedHashSet<>();
        Rfc822Tokenizer.tokenize(list, addresses);
        if (validator != null) {
            Iterator<Rfc822Token> addressIterator = addresses.iterator();
            while (addressIterator.hasNext()) {
                Rfc822Token address = addressIterator.next();
                if (!validator.isValid(address.getAddress())) {
                    Log.v("EditEventHelper", "Dropping invalid attendee email address: " + address.getAddress());
                    addressIterator.remove();
                }
            }
        }
        return addresses;
    }

    protected long constructDefaultStartTime(long now) {
        Time defaultStart = new Time();
        defaultStart.set(now);
        defaultStart.second = 0;
        defaultStart.minute = 30;
        long defaultStartMillis = defaultStart.toMillis(false);
        return now < defaultStartMillis ? defaultStartMillis : defaultStartMillis + 1800000;
    }

    protected long constructDefaultEndTime(long startTime) {
        return 3600000 + startTime;
    }

    void checkTimeDependentFields(CalendarEventModel originalModel, CalendarEventModel model, ContentValues values, int modifyWhich) {
        long oldBegin = model.mOriginalStart;
        long oldEnd = model.mOriginalEnd;
        boolean oldAllDay = originalModel.mAllDay;
        String oldRrule = originalModel.mRrule;
        String oldTimezone = originalModel.mTimezone;
        long newBegin = model.mStart;
        long newEnd = model.mEnd;
        boolean newAllDay = model.mAllDay;
        String newRrule = model.mRrule;
        String newTimezone = model.mTimezone;
        if (oldBegin == newBegin && oldEnd == newEnd && oldAllDay == newAllDay && TextUtils.equals(oldRrule, newRrule) && TextUtils.equals(oldTimezone, newTimezone)) {
            values.remove("dtstart");
            values.remove("dtend");
            values.remove("duration");
            values.remove("allDay");
            values.remove("rrule");
            values.remove("eventTimezone");
            return;
        }
        if (!TextUtils.isEmpty(oldRrule) && !TextUtils.isEmpty(newRrule) && modifyWhich == 3) {
            long oldStartMillis = originalModel.mStart;
            if (oldBegin != newBegin) {
                long offset = newBegin - oldBegin;
                oldStartMillis += offset;
            }
            if (newAllDay) {
                Time time = new Time("UTC");
                time.set(oldStartMillis);
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                oldStartMillis = time.toMillis(false);
            }
            values.put("dtstart", Long.valueOf(oldStartMillis));
        }
    }

    public String updatePastEvents(ArrayList<ContentProviderOperation> ops, CalendarEventModel originalModel, long endTimeMillis) {
        boolean origAllDay = originalModel.mAllDay;
        String origRrule = originalModel.mRrule;
        String newRrule = origRrule;
        EventRecurrence origRecurrence = new EventRecurrence();
        origRecurrence.parse(origRrule);
        long startTimeMillis = originalModel.mStart;
        Time dtstart = new Time();
        dtstart.timezone = originalModel.mTimezone;
        dtstart.set(startTimeMillis);
        ContentValues updateValues = new ContentValues();
        if (origRecurrence.count > 0) {
            RecurrenceSet recurSet = new RecurrenceSet(originalModel.mRrule, null, null, null);
            RecurrenceProcessor recurProc = new RecurrenceProcessor();
            try {
                long[] recurrences = recurProc.expand(dtstart, recurSet, startTimeMillis, endTimeMillis);
                if (recurrences.length == 0) {
                    throw new RuntimeException("can't use this method on first instance");
                }
                EventRecurrence excepRecurrence = new EventRecurrence();
                excepRecurrence.parse(origRrule);
                excepRecurrence.count -= recurrences.length;
                newRrule = excepRecurrence.toString();
                origRecurrence.count = recurrences.length;
            } catch (DateException de) {
                throw new RuntimeException(de);
            }
        } else {
            Time untilTime = new Time();
            untilTime.timezone = "UTC";
            untilTime.set(endTimeMillis - 1000);
            if (origAllDay) {
                untilTime.hour = 0;
                untilTime.minute = 0;
                untilTime.second = 0;
                untilTime.allDay = true;
                untilTime.normalize(false);
                dtstart.hour = 0;
                dtstart.minute = 0;
                dtstart.second = 0;
                dtstart.allDay = true;
                dtstart.timezone = "UTC";
            }
            origRecurrence.until = untilTime.format2445();
        }
        updateValues.put("rrule", origRecurrence.toString());
        updateValues.put("dtstart", Long.valueOf(dtstart.normalize(true)));
        ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(Uri.parse(originalModel.mUri)).withValues(updateValues);
        ops.add(b.build());
        return newRrule;
    }

    public static boolean isSameEvent(CalendarEventModel model, CalendarEventModel originalModel) {
        if (originalModel == null) {
            return true;
        }
        return model.mCalendarId == originalModel.mCalendarId && model.mId == originalModel.mId;
    }

    public static boolean saveReminders(ArrayList<ContentProviderOperation> ops, long eventId, ArrayList<CalendarEventModel.ReminderEntry> reminders, ArrayList<CalendarEventModel.ReminderEntry> originalReminders, boolean forceSave) {
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }
        String[] args = {Long.toString(eventId)};
        ContentProviderOperation.Builder b = ContentProviderOperation.newDelete(CalendarContract.Reminders.CONTENT_URI);
        b.withSelection("event_id=?", args);
        ops.add(b.build());
        ContentValues values = new ContentValues();
        int len = reminders.size();
        for (int i = 0; i < len; i++) {
            CalendarEventModel.ReminderEntry re = reminders.get(i);
            values.clear();
            values.put("minutes", Integer.valueOf(re.getMinutes()));
            values.put("method", Integer.valueOf(re.getMethod()));
            values.put("event_id", Long.valueOf(eventId));
            ops.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(values).build());
        }
        return true;
    }

    public static boolean saveRemindersWithBackRef(ArrayList<ContentProviderOperation> ops, int eventIdIndex, ArrayList<CalendarEventModel.ReminderEntry> reminders, ArrayList<CalendarEventModel.ReminderEntry> originalReminders, boolean forceSave) {
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }
        ContentProviderOperation.Builder b = ContentProviderOperation.newDelete(CalendarContract.Reminders.CONTENT_URI);
        b.withSelection("event_id=?", new String[1]);
        b.withSelectionBackReference(0, eventIdIndex);
        ops.add(b.build());
        ContentValues values = new ContentValues();
        int len = reminders.size();
        for (int i = 0; i < len; i++) {
            CalendarEventModel.ReminderEntry re = reminders.get(i);
            values.clear();
            values.put("minutes", Integer.valueOf(re.getMinutes()));
            values.put("method", Integer.valueOf(re.getMethod()));
            ContentProviderOperation.Builder b2 = ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(values);
            b2.withValueBackReference("event_id", eventIdIndex);
            ops.add(b2.build());
        }
        return true;
    }

    static boolean isFirstEventInSeries(CalendarEventModel model, CalendarEventModel originalModel) {
        return model.mOriginalStart == originalModel.mStart;
    }

    void addRecurrenceRule(ContentValues values, CalendarEventModel model) {
        String rrule = model.mRrule;
        values.put("rrule", rrule);
        long end = model.mEnd;
        long start = model.mStart;
        String duration = model.mDuration;
        boolean isAllDay = model.mAllDay;
        if (end >= start) {
            if (isAllDay) {
                long days = (((end - start) + 86400000) - 1) / 86400000;
                duration = "P" + days + "D";
            } else {
                long seconds = (end - start) / 1000;
                duration = "P" + seconds + "S";
            }
        } else if (TextUtils.isEmpty(duration)) {
            if (isAllDay) {
                duration = "P1D";
            } else {
                duration = "P3600S";
            }
        }
        values.put("duration", duration);
        values.put("dtend", (Long) null);
    }

    static void updateRecurrenceRule(int selection, CalendarEventModel model, int weekStart) {
        EventRecurrence eventRecurrence = new EventRecurrence();
        if (selection == 0) {
            model.mRrule = null;
            return;
        }
        if (selection != 7) {
            if (selection == 1) {
                eventRecurrence.freq = 4;
            } else if (selection == 2) {
                eventRecurrence.freq = 5;
                int[] bydayNum = new int[5];
                int[] byday = {131072, 262144, 524288, 1048576, 2097152};
                for (int day = 0; day < 5; day++) {
                    bydayNum[day] = 0;
                }
                eventRecurrence.byday = byday;
                eventRecurrence.bydayNum = bydayNum;
                eventRecurrence.bydayCount = 5;
            } else if (selection == 3) {
                eventRecurrence.freq = 5;
                Time startTime = new Time(model.mTimezone);
                startTime.set(model.mStart);
                int[] days = {EventRecurrence.timeDay2Day(startTime.weekDay)};
                int[] dayNum = {0};
                eventRecurrence.byday = days;
                eventRecurrence.bydayNum = dayNum;
                eventRecurrence.bydayCount = 1;
            } else if (selection == 5) {
                eventRecurrence.freq = 6;
                eventRecurrence.bydayCount = 0;
                eventRecurrence.bymonthdayCount = 1;
                Time startTime2 = new Time(model.mTimezone);
                startTime2.set(model.mStart);
                int[] bymonthday = {startTime2.monthDay};
                eventRecurrence.bymonthday = bymonthday;
            } else if (selection == 4) {
                eventRecurrence.freq = 6;
                eventRecurrence.bydayCount = 1;
                eventRecurrence.bymonthdayCount = 0;
                int[] byday2 = new int[1];
                int[] bydayNum2 = new int[1];
                Time startTime3 = new Time(model.mTimezone);
                startTime3.set(model.mStart);
                int dayCount = ((startTime3.monthDay - 1) / 7) + 1;
                if (dayCount == 5) {
                    dayCount = -1;
                }
                bydayNum2[0] = dayCount;
                byday2[0] = EventRecurrence.timeDay2Day(startTime3.weekDay);
                eventRecurrence.byday = byday2;
                eventRecurrence.bydayNum = bydayNum2;
            } else if (selection == 6) {
                eventRecurrence.freq = 7;
            }
            eventRecurrence.wkst = EventRecurrence.calendarDay2Day(weekStart);
            model.mRrule = eventRecurrence.toString();
        }
    }

    public static void setModelFromCursor(CalendarEventModel model, Cursor cursor) {
        int rawEventColor;
        if (model == null || cursor == null || cursor.getCount() != 1) {
            Log.wtf("EditEventHelper", "Attempted to build non-existent model or from an incorrect query.");
            return;
        }
        model.clear();
        cursor.moveToFirst();
        model.mId = cursor.getInt(0);
        model.mTitle = cursor.getString(1);
        model.mDescription = cursor.getString(2);
        model.mLocation = cursor.getString(3);
        model.mAllDay = cursor.getInt(4) != 0;
        model.mHasAlarm = cursor.getInt(5) != 0;
        model.mCalendarId = cursor.getInt(6);
        model.mStart = cursor.getLong(7);
        String tz = cursor.getString(10);
        if (!TextUtils.isEmpty(tz)) {
            model.mTimezone = tz;
        }
        String rRule = cursor.getString(11);
        model.mRrule = rRule;
        model.mSyncId = cursor.getString(12);
        model.mAvailability = cursor.getInt(13);
        int accessLevel = cursor.getInt(14);
        model.mOwnerAccount = cursor.getString(15);
        model.mHasAttendeeData = cursor.getInt(16) != 0;
        model.mOriginalSyncId = cursor.getString(17);
        model.mOriginalId = cursor.getLong(20);
        model.mOrganizer = cursor.getString(18);
        model.mIsOrganizer = model.mOwnerAccount.equalsIgnoreCase(model.mOrganizer);
        model.mGuestsCanModify = cursor.getInt(19) != 0;
        if (cursor.isNull(23)) {
            rawEventColor = cursor.getInt(22);
        } else {
            rawEventColor = cursor.getInt(23);
        }
        model.setEventColor(Utils.getDisplayColorFromColor(rawEventColor));
        if (accessLevel > 0) {
            accessLevel--;
        }
        model.mAccessLevel = accessLevel;
        model.mEventStatus = cursor.getInt(21);
        boolean hasRRule = !TextUtils.isEmpty(rRule);
        if (hasRRule) {
            model.mDuration = cursor.getString(9);
        } else {
            model.mEnd = cursor.getLong(8);
        }
        model.mModelUpdatedWithEventCursor = true;
    }

    public static boolean setModelFromCalendarCursor(CalendarEventModel model, Cursor cursor) {
        if (model == null || cursor == null) {
            Log.wtf("EditEventHelper", "Attempted to build non-existent model or from an incorrect query.");
            return false;
        }
        if (model.mCalendarId == -1) {
            return false;
        }
        if (!model.mModelUpdatedWithEventCursor) {
            Log.wtf("EditEventHelper", "Can't update model with a Calendar cursor until it has seen an Event cursor.");
            return false;
        }
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (model.mCalendarId == cursor.getInt(0)) {
                model.mOrganizerCanRespond = cursor.getInt(4) != 0;
                model.mCalendarAccessLevel = cursor.getInt(5);
                model.mCalendarDisplayName = cursor.getString(1);
                model.setCalendarColor(Utils.getDisplayColorFromColor(cursor.getInt(3)));
                model.mCalendarAccountName = cursor.getString(11);
                model.mCalendarAccountType = cursor.getString(12);
                model.mCalendarMaxReminders = cursor.getInt(7);
                model.mCalendarAllowedReminders = cursor.getString(8);
                model.mCalendarAllowedAttendeeTypes = cursor.getString(9);
                model.mCalendarAllowedAvailability = cursor.getString(10);
                return true;
            }
        }
        return false;
    }

    public static boolean canModifyEvent(CalendarEventModel model) {
        return canModifyCalendar(model) && (model.mIsOrganizer || model.mGuestsCanModify);
    }

    public static boolean canModifyCalendar(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= 500 || model.mCalendarId == -1;
    }

    public static boolean canAddReminders(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= 200;
    }

    public static boolean canRespond(CalendarEventModel model) {
        if (!canModifyCalendar(model)) {
            return false;
        }
        if (!model.mIsOrganizer) {
            return true;
        }
        if (model.mOrganizerCanRespond) {
            return (model.mHasAttendeeData && model.mAttendeesList.size() == 0) ? false : true;
        }
        return false;
    }

    ContentValues getContentValuesFromModel(CalendarEventModel model) {
        long startMillis;
        long endMillis;
        String title = model.mTitle;
        boolean isAllDay = model.mAllDay;
        String rrule = model.mRrule;
        String timezone = model.mTimezone;
        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
        }
        Time startTime = new Time(timezone);
        Time endTime = new Time(timezone);
        startTime.set(model.mStart);
        endTime.set(model.mEnd);
        offsetStartTimeIfNecessary(startTime, endTime, rrule, model);
        ContentValues values = new ContentValues();
        long calendarId = model.mCalendarId;
        if (isAllDay) {
            timezone = "UTC";
            startTime.hour = 0;
            startTime.minute = 0;
            startTime.second = 0;
            startTime.timezone = "UTC";
            startMillis = startTime.normalize(true);
            endTime.hour = 0;
            endTime.minute = 0;
            endTime.second = 0;
            endTime.timezone = "UTC";
            endMillis = endTime.normalize(true);
            if (endMillis < 86400000 + startMillis) {
                endMillis = startMillis + 86400000;
            }
        } else {
            startMillis = startTime.toMillis(true);
            endMillis = endTime.toMillis(true);
        }
        values.put("calendar_id", Long.valueOf(calendarId));
        values.put("eventTimezone", timezone);
        values.put("title", title);
        values.put("allDay", Integer.valueOf(isAllDay ? 1 : 0));
        values.put("dtstart", Long.valueOf(startMillis));
        values.put("rrule", rrule);
        if (!TextUtils.isEmpty(rrule)) {
            addRecurrenceRule(values, model);
        } else {
            values.put("duration", (String) null);
            values.put("dtend", Long.valueOf(endMillis));
        }
        if (model.mDescription != null) {
            values.put("description", model.mDescription.trim());
        } else {
            values.put("description", (String) null);
        }
        if (model.mLocation != null) {
            values.put("eventLocation", model.mLocation.trim());
        } else {
            values.put("eventLocation", (String) null);
        }
        values.put("availability", Integer.valueOf(model.mAvailability));
        values.put("hasAttendeeData", Integer.valueOf(model.mHasAttendeeData ? 1 : 0));
        int accessLevel = model.mAccessLevel;
        if (accessLevel > 0) {
            accessLevel++;
        }
        values.put("accessLevel", Integer.valueOf(accessLevel));
        values.put("eventStatus", Integer.valueOf(model.mEventStatus));
        if (model.isEventColorInitialized()) {
            if (model.getEventColor() == model.getCalendarColor()) {
                values.put("eventColor_index", "");
            } else {
                values.put("eventColor_index", Integer.valueOf(model.getEventColorKey()));
            }
        }
        return values;
    }

    private void offsetStartTimeIfNecessary(Time startTime, Time endTime, String rrule, CalendarEventModel model) {
        if (rrule != null && !rrule.isEmpty()) {
            this.mEventRecurrence.parse(rrule);
            if (this.mEventRecurrence.freq == 5 && this.mEventRecurrence.byday != null && this.mEventRecurrence.byday.length <= this.mEventRecurrence.bydayCount) {
                int closestWeekday = Integer.MAX_VALUE;
                int weekstart = EventRecurrence.day2TimeDay(this.mEventRecurrence.wkst);
                int startDay = startTime.weekDay;
                for (int i = 0; i < this.mEventRecurrence.bydayCount; i++) {
                    int day = EventRecurrence.day2TimeDay(this.mEventRecurrence.byday[i]);
                    if (day != startDay) {
                        if (day < weekstart) {
                            day += 7;
                        }
                        if (day > startDay && (day < closestWeekday || closestWeekday < startDay)) {
                            closestWeekday = day;
                        }
                        if ((closestWeekday == Integer.MAX_VALUE || closestWeekday < startDay) && day < closestWeekday) {
                            closestWeekday = day;
                        }
                    } else {
                        return;
                    }
                }
                if (closestWeekday < startDay) {
                    closestWeekday += 7;
                }
                int daysOffset = closestWeekday - startDay;
                startTime.monthDay += daysOffset;
                endTime.monthDay += daysOffset;
                long newStartTime = startTime.normalize(true);
                long newEndTime = endTime.normalize(true);
                model.mStart = newStartTime;
                model.mEnd = newEndTime;
            }
        }
    }

    public static String extractDomain(String email) {
        int separator;
        int separator2 = email.lastIndexOf(64);
        if (separator2 == -1 || (separator = separator2 + 1) >= email.length()) {
            return null;
        }
        return email.substring(separator);
    }
}
