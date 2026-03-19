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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TimeZone;

public class EditEventHelper {
    protected boolean mEventOk;
    private EventRecurrence mEventRecurrence;
    private final AsyncQueryService mService;
    public static final String[] EVENT_PROJECTION = {"_id", "title", "description", "eventLocation", "allDay", "hasAlarm", "calendar_id", "dtstart", "dtend", "duration", "eventTimezone", "rrule", "_sync_id", "availability", "accessLevel", "ownerAccount", "hasAttendeeData", "original_sync_id", "organizer", "guestsCanModify", "original_id", "eventStatus", "calendar_color", "eventColor", "eventColor_index", "isLunar"};
    static final String[] REMINDERS_PROJECTION = {"_id", "minutes", "method"};
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

        public AttendeeItem(CalendarEventModel.Attendee attendee, Drawable drawable) {
            this.mAttendee = attendee;
            this.mBadge = drawable;
        }
    }

    public EditEventHelper(Context context) {
        this.mEventRecurrence = new EventRecurrence();
        this.mEventOk = true;
        this.mService = ((AbstractCalendarActivity) context).getAsyncQueryService();
    }

    public EditEventHelper(Context context, CalendarEventModel calendarEventModel) {
        this(context);
    }

    public boolean saveEvent(CalendarEventModel calendarEventModel, CalendarEventModel calendarEventModel2, int i) {
        int size;
        boolean z;
        Object[] objArr;
        ArrayList<CalendarEventModel.ReminderEntry> arrayList;
        int i2;
        boolean z2;
        String attendeesString;
        LinkedHashMap<String, CalendarEventModel.Attendee> linkedHashMap;
        ContentProviderOperation.Builder builderWithValues;
        ContentProviderOperation.Builder builderWithValues2;
        if (!this.mEventOk) {
            return false;
        }
        if (calendarEventModel == null) {
            Log.e("EditEventHelper", "Attempted to save null model.");
            return false;
        }
        if (!calendarEventModel.isValid()) {
            Log.e("EditEventHelper", "Attempted to save invalid model.");
            return false;
        }
        if (calendarEventModel2 != null && !isSameEvent(calendarEventModel, calendarEventModel2)) {
            Log.e("EditEventHelper", "Attempted to update existing event but models didn't refer to the same event.");
            return false;
        }
        if (calendarEventModel2 != null && calendarEventModel.isUnchanged(calendarEventModel2)) {
            return false;
        }
        ArrayList<ContentProviderOperation> arrayList2 = new ArrayList<>();
        ContentValues contentValuesFromModel = getContentValuesFromModel(calendarEventModel);
        if (calendarEventModel.mUri != null && calendarEventModel2 == null) {
            Log.e("EditEventHelper", "Existing event but no originalModel provided. Aborting save.");
            return false;
        }
        Uri uri = null;
        if (calendarEventModel.mUri != null) {
            uri = Uri.parse(calendarEventModel.mUri);
        }
        Uri uri2 = uri;
        ArrayList<CalendarEventModel.ReminderEntry> arrayList3 = calendarEventModel.mReminders;
        contentValuesFromModel.put("hasAlarm", Integer.valueOf(arrayList3.size() > 0 ? 1 : 0));
        if (uri2 == null) {
            contentValuesFromModel.put("hasAttendeeData", (Integer) 1);
            contentValuesFromModel.put("eventStatus", (Integer) 1);
            size = arrayList2.size();
            arrayList2.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(contentValuesFromModel).build());
        } else {
            if (TextUtils.isEmpty(calendarEventModel.mRrule) && TextUtils.isEmpty(calendarEventModel2.mRrule)) {
                checkTimeDependentFields(calendarEventModel2, calendarEventModel, contentValuesFromModel, i);
                arrayList2.add(ContentProviderOperation.newUpdate(uri2).withValues(contentValuesFromModel).build());
            } else if (TextUtils.isEmpty(calendarEventModel2.mRrule)) {
                arrayList2.add(ContentProviderOperation.newUpdate(uri2).withValues(contentValuesFromModel).build());
            } else if (i != 1) {
                if (i != 2) {
                    if (i == 3) {
                        if (TextUtils.isEmpty(calendarEventModel.mRrule)) {
                            arrayList2.add(ContentProviderOperation.newDelete(uri2).build());
                            size = arrayList2.size();
                            arrayList2.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(contentValuesFromModel).build());
                        } else {
                            checkTimeDependentFields(calendarEventModel2, calendarEventModel, contentValuesFromModel, i);
                            arrayList2.add(ContentProviderOperation.newUpdate(uri2).withValues(contentValuesFromModel).build());
                        }
                    }
                } else if (TextUtils.isEmpty(calendarEventModel.mRrule)) {
                    if (isFirstEventInSeries(calendarEventModel, calendarEventModel2)) {
                        arrayList2.add(ContentProviderOperation.newDelete(uri2).build());
                    } else {
                        updatePastEvents(arrayList2, calendarEventModel2, calendarEventModel.mOriginalStart);
                    }
                    size = arrayList2.size();
                    contentValuesFromModel.put("eventStatus", Integer.valueOf(calendarEventModel2.mEventStatus));
                    arrayList2.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(contentValuesFromModel).build());
                } else if (isFirstEventInSeries(calendarEventModel, calendarEventModel2)) {
                    checkTimeDependentFields(calendarEventModel2, calendarEventModel, contentValuesFromModel, i);
                    arrayList2.add(ContentProviderOperation.newUpdate(uri2).withValues(contentValuesFromModel).build());
                    size = -1;
                } else {
                    String strUpdatePastEvents = updatePastEvents(arrayList2, calendarEventModel2, calendarEventModel.mOriginalStart);
                    if (calendarEventModel.mRrule.equals(calendarEventModel2.mRrule)) {
                        contentValuesFromModel.put("rrule", strUpdatePastEvents);
                    }
                    size = arrayList2.size();
                    contentValuesFromModel.put("eventStatus", Integer.valueOf(calendarEventModel2.mEventStatus));
                    arrayList2.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(contentValuesFromModel).build());
                }
            } else {
                long j = calendarEventModel.mOriginalStart;
                contentValuesFromModel.put("original_sync_id", calendarEventModel2.mSyncId);
                contentValuesFromModel.put("originalInstanceTime", Long.valueOf(j));
                contentValuesFromModel.put("originalAllDay", Integer.valueOf(calendarEventModel2.mAllDay ? 1 : 0));
                contentValuesFromModel.put("eventStatus", Integer.valueOf(calendarEventModel2.mEventStatus));
                size = arrayList2.size();
                arrayList2.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(contentValuesFromModel).build());
            }
            z = false;
            size = -1;
            objArr = size == -1;
            if (calendarEventModel2 == null) {
                arrayList = calendarEventModel2.mReminders;
            } else {
                arrayList = new ArrayList<>();
            }
            ArrayList<CalendarEventModel.ReminderEntry> arrayList4 = arrayList;
            if (objArr == false) {
                saveRemindersWithBackRef(arrayList2, size, arrayList3, arrayList4, z);
            } else {
                if (uri2 != null) {
                    i2 = -1;
                    saveReminders(arrayList2, ContentUris.parseId(uri2), arrayList3, arrayList4, z);
                }
                z2 = calendarEventModel.mHasAttendeeData;
                if (z2 && calendarEventModel.mOwnerAttendeeId == i2) {
                    String str = calendarEventModel.mOwnerAccount;
                    if (calendarEventModel.mAttendeesList.size() != 0 && Utils.isValidEmail(str)) {
                        contentValuesFromModel.clear();
                        contentValuesFromModel.put("attendeeEmail", str);
                        contentValuesFromModel.put("attendeeRelationship", (Integer) 2);
                        contentValuesFromModel.put("attendeeType", (Integer) 1);
                        contentValuesFromModel.put("attendeeStatus", (Integer) 1);
                        if (objArr != false) {
                            builderWithValues2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(contentValuesFromModel);
                            builderWithValues2.withValueBackReference("event_id", size);
                        } else {
                            contentValuesFromModel.put("event_id", Long.valueOf(calendarEventModel.mId));
                            builderWithValues2 = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(contentValuesFromModel);
                        }
                        arrayList2.add(builderWithValues2.build());
                    }
                } else if (z2 && calendarEventModel.mSelfAttendeeStatus != calendarEventModel2.mSelfAttendeeStatus && calendarEventModel.mOwnerAttendeeId != i2) {
                    Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, calendarEventModel.mOwnerAttendeeId);
                    contentValuesFromModel.clear();
                    contentValuesFromModel.put("attendeeStatus", Integer.valueOf(calendarEventModel.mSelfAttendeeStatus));
                    contentValuesFromModel.put("event_id", Long.valueOf(calendarEventModel.mId));
                    arrayList2.add(ContentProviderOperation.newUpdate(uriWithAppendedId).withValues(contentValuesFromModel).build());
                }
                if (z2 && (objArr != false || uri2 != null)) {
                    String attendeesString2 = calendarEventModel.getAttendeesString();
                    if (calendarEventModel2 == null) {
                        attendeesString = calendarEventModel2.getAttendeesString();
                    } else {
                        attendeesString = "";
                    }
                    if (objArr == false || !TextUtils.equals(attendeesString, attendeesString2)) {
                        linkedHashMap = calendarEventModel.mAttendeesList;
                        LinkedList<String> linkedList = new LinkedList();
                        long id = uri2 == null ? ContentUris.parseId(uri2) : -1L;
                        if (objArr == false) {
                            linkedList.clear();
                            for (String str2 : calendarEventModel2.mAttendeesList.keySet()) {
                                if (linkedHashMap.containsKey(str2)) {
                                    linkedHashMap.remove(str2);
                                } else {
                                    linkedList.add(str2);
                                }
                            }
                            if (linkedList.size() > 0) {
                                ContentProviderOperation.Builder builderNewDelete = ContentProviderOperation.newDelete(CalendarContract.Attendees.CONTENT_URI);
                                String[] strArr = new String[linkedList.size() + 1];
                                strArr[0] = Long.toString(id);
                                StringBuilder sb = new StringBuilder("event_id=? AND attendeeEmail IN (");
                                int i3 = 1;
                                for (String str3 : linkedList) {
                                    if (i3 > 1) {
                                        sb.append(",");
                                    }
                                    sb.append("?");
                                    strArr[i3] = str3;
                                    i3++;
                                }
                                sb.append(")");
                                builderNewDelete.withSelection(sb.toString(), strArr);
                                arrayList2.add(builderNewDelete.build());
                            }
                        }
                        if (linkedHashMap.size() > 0) {
                            for (CalendarEventModel.Attendee attendee : linkedHashMap.values()) {
                                contentValuesFromModel.clear();
                                contentValuesFromModel.put("attendeeName", attendee.mName);
                                contentValuesFromModel.put("attendeeEmail", attendee.mEmail);
                                contentValuesFromModel.put("attendeeRelationship", (Integer) 1);
                                contentValuesFromModel.put("attendeeType", (Integer) 1);
                                contentValuesFromModel.put("attendeeStatus", (Integer) 0);
                                if (objArr != false) {
                                    builderWithValues = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(contentValuesFromModel);
                                    builderWithValues.withValueBackReference("event_id", size);
                                } else {
                                    contentValuesFromModel.put("event_id", Long.valueOf(id));
                                    builderWithValues = ContentProviderOperation.newInsert(CalendarContract.Attendees.CONTENT_URI).withValues(contentValuesFromModel);
                                }
                                arrayList2.add(builderWithValues.build());
                            }
                        }
                    }
                }
                this.mService.startBatch(this.mService.getNextToken(), null, "com.android.calendar", arrayList2, 0L);
                return true;
            }
            i2 = -1;
            z2 = calendarEventModel.mHasAttendeeData;
            if (z2) {
                if (z2) {
                    Uri uriWithAppendedId2 = ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, calendarEventModel.mOwnerAttendeeId);
                    contentValuesFromModel.clear();
                    contentValuesFromModel.put("attendeeStatus", Integer.valueOf(calendarEventModel.mSelfAttendeeStatus));
                    contentValuesFromModel.put("event_id", Long.valueOf(calendarEventModel.mId));
                    arrayList2.add(ContentProviderOperation.newUpdate(uriWithAppendedId2).withValues(contentValuesFromModel).build());
                }
            }
            if (z2) {
                String attendeesString22 = calendarEventModel.getAttendeesString();
                if (calendarEventModel2 == null) {
                }
                if (objArr == false) {
                    linkedHashMap = calendarEventModel.mAttendeesList;
                    LinkedList<String> linkedList2 = new LinkedList();
                    if (uri2 == null) {
                    }
                    if (objArr == false) {
                    }
                    if (linkedHashMap.size() > 0) {
                    }
                }
            }
            this.mService.startBatch(this.mService.getNextToken(), null, "com.android.calendar", arrayList2, 0L);
            return true;
        }
        z = true;
        if (size == -1) {
        }
        if (calendarEventModel2 == null) {
        }
        ArrayList<CalendarEventModel.ReminderEntry> arrayList42 = arrayList;
        if (objArr == false) {
        }
        i2 = -1;
        z2 = calendarEventModel.mHasAttendeeData;
        if (z2) {
        }
        if (z2) {
        }
        this.mService.startBatch(this.mService.getNextToken(), null, "com.android.calendar", arrayList2, 0L);
        return true;
    }

    public static LinkedHashSet<Rfc822Token> getAddressesFromList(String str, Rfc822Validator rfc822Validator) {
        LinkedHashSet<Rfc822Token> linkedHashSet = new LinkedHashSet<>();
        Rfc822Tokenizer.tokenize(str, linkedHashSet);
        if (rfc822Validator == null) {
            return linkedHashSet;
        }
        Iterator<Rfc822Token> it = linkedHashSet.iterator();
        while (it.hasNext()) {
            if (!rfc822Validator.isValid(it.next().getAddress())) {
                it.remove();
            }
        }
        return linkedHashSet;
    }

    protected long constructDefaultStartTime(long j) {
        Time time = new Time();
        time.set(j);
        time.second = 0;
        time.minute = 30;
        long millis = time.toMillis(false);
        if (j < millis) {
            return millis;
        }
        return millis + 1800000;
    }

    protected long constructDefaultEndTime(long j) {
        return j + 3600000;
    }

    void checkTimeDependentFields(CalendarEventModel calendarEventModel, CalendarEventModel calendarEventModel2, ContentValues contentValues, int i) {
        long j = calendarEventModel2.mOriginalStart;
        long j2 = calendarEventModel2.mOriginalEnd;
        boolean z = calendarEventModel.mAllDay;
        String str = calendarEventModel.mRrule;
        String str2 = calendarEventModel.mTimezone;
        long j3 = calendarEventModel2.mStart;
        long j4 = calendarEventModel2.mEnd;
        boolean z2 = calendarEventModel2.mAllDay;
        String str3 = calendarEventModel2.mRrule;
        String str4 = calendarEventModel2.mTimezone;
        if (j == j3 && j2 == j4 && z == z2 && TextUtils.equals(str, str3) && TextUtils.equals(str2, str4)) {
            contentValues.remove("dtstart");
            contentValues.remove("dtend");
            contentValues.remove("duration");
            contentValues.remove("allDay");
            contentValues.remove("rrule");
            contentValues.remove("eventTimezone");
            return;
        }
        if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str3) && i == 3) {
            long millis = calendarEventModel.mStart;
            if (j != j3) {
                millis += j3 - j;
            }
            if (z2) {
                Time time = new Time("UTC");
                time.set(millis);
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                millis = time.toMillis(false);
            }
            contentValues.put("dtstart", Long.valueOf(millis));
        }
    }

    public String updatePastEvents(ArrayList<ContentProviderOperation> arrayList, CalendarEventModel calendarEventModel, long j) {
        boolean z = calendarEventModel.mAllDay;
        String string = calendarEventModel.mRrule;
        EventRecurrence eventRecurrence = new EventRecurrence();
        eventRecurrence.parse(string);
        long j2 = calendarEventModel.mStart;
        Time time = new Time();
        time.timezone = calendarEventModel.mTimezone;
        time.set(j2);
        ContentValues contentValues = new ContentValues();
        if (eventRecurrence.count > 0) {
            try {
                long[] jArrExpand = new RecurrenceProcessor().expand(time, new RecurrenceSet(calendarEventModel.mRrule, null, null, null), j2, j);
                if (jArrExpand.length == 0) {
                    throw new RuntimeException("can't use this method on first instance");
                }
                EventRecurrence eventRecurrence2 = new EventRecurrence();
                eventRecurrence2.parse(string);
                eventRecurrence2.count -= jArrExpand.length;
                string = eventRecurrence2.toString();
                eventRecurrence.count = jArrExpand.length;
            } catch (DateException e) {
                throw new RuntimeException(e);
            }
        } else {
            Time time2 = new Time();
            time2.timezone = "UTC";
            time2.set(j - 1000);
            if (z) {
                time2.hour = 0;
                time2.minute = 0;
                time2.second = 0;
                time2.allDay = true;
                time2.normalize(false);
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                time.allDay = true;
                time.timezone = "UTC";
            }
            eventRecurrence.until = time2.format2445();
        }
        contentValues.put("rrule", eventRecurrence.toString());
        contentValues.put("dtstart", Long.valueOf(time.normalize(true)));
        arrayList.add(ContentProviderOperation.newUpdate(Uri.parse(calendarEventModel.mUri)).withValues(contentValues).build());
        return string;
    }

    public static boolean isSameEvent(CalendarEventModel calendarEventModel, CalendarEventModel calendarEventModel2) {
        if (calendarEventModel2 == null) {
            return true;
        }
        return calendarEventModel.mCalendarId == calendarEventModel2.mCalendarId && calendarEventModel.mId == calendarEventModel2.mId;
    }

    public static boolean updateModifyTime(ArrayList<ContentProviderOperation> arrayList, Uri uri) {
        if (uri == null || arrayList == null) {
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.clear();
        contentValues.put("modifyTime", Long.valueOf(System.currentTimeMillis()));
        arrayList.add(ContentProviderOperation.newUpdate(uri).withValues(contentValues).build());
        return true;
    }

    public static boolean saveReminders(ArrayList<ContentProviderOperation> arrayList, long j, ArrayList<CalendarEventModel.ReminderEntry> arrayList2, ArrayList<CalendarEventModel.ReminderEntry> arrayList3, boolean z) {
        if (arrayList2.equals(arrayList3) && !z) {
            return false;
        }
        String[] strArr = {Long.toString(j)};
        ContentProviderOperation.Builder builderNewDelete = ContentProviderOperation.newDelete(CalendarContract.Reminders.CONTENT_URI);
        builderNewDelete.withSelection("event_id=?", strArr);
        arrayList.add(builderNewDelete.build());
        ArrayList arrayList4 = new ArrayList();
        for (CalendarEventModel.ReminderEntry reminderEntry : arrayList3) {
            if (!arrayList2.contains(reminderEntry)) {
                arrayList4.add(reminderEntry);
            }
        }
        if (!arrayList4.isEmpty()) {
            Iterator it = arrayList4.iterator();
            while (it.hasNext()) {
                String[] strArr2 = {Long.toString(j), Integer.toString(((CalendarEventModel.ReminderEntry) it.next()).getMinutes())};
                ContentProviderOperation.Builder builderNewDelete2 = ContentProviderOperation.newDelete(CalendarContract.CalendarAlerts.CONTENT_URI);
                builderNewDelete2.withSelection("event_id=? AND minutes=?", strArr2);
                arrayList.add(builderNewDelete2.build());
            }
        }
        ContentValues contentValues = new ContentValues();
        int size = arrayList2.size();
        for (int i = 0; i < size; i++) {
            CalendarEventModel.ReminderEntry reminderEntry2 = arrayList2.get(i);
            contentValues.clear();
            contentValues.put("minutes", Integer.valueOf(reminderEntry2.getMinutes()));
            contentValues.put("method", Integer.valueOf(reminderEntry2.getMethod()));
            contentValues.put("event_id", Long.valueOf(j));
            arrayList.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(contentValues).build());
        }
        updateModifyTime(arrayList, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j));
        return true;
    }

    public static boolean saveRemindersWithBackRef(ArrayList<ContentProviderOperation> arrayList, int i, ArrayList<CalendarEventModel.ReminderEntry> arrayList2, ArrayList<CalendarEventModel.ReminderEntry> arrayList3, boolean z) {
        if (arrayList2.equals(arrayList3) && !z) {
            return false;
        }
        ContentProviderOperation.Builder builderNewDelete = ContentProviderOperation.newDelete(CalendarContract.Reminders.CONTENT_URI);
        builderNewDelete.withSelection("event_id=?", new String[1]);
        builderNewDelete.withSelectionBackReference(0, i);
        arrayList.add(builderNewDelete.build());
        ContentValues contentValues = new ContentValues();
        int size = arrayList2.size();
        for (int i2 = 0; i2 < size; i2++) {
            CalendarEventModel.ReminderEntry reminderEntry = arrayList2.get(i2);
            contentValues.clear();
            contentValues.put("minutes", Integer.valueOf(reminderEntry.getMinutes()));
            contentValues.put("method", Integer.valueOf(reminderEntry.getMethod()));
            ContentProviderOperation.Builder builderWithValues = ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI).withValues(contentValues);
            builderWithValues.withValueBackReference("event_id", i);
            arrayList.add(builderWithValues.build());
        }
        return true;
    }

    static boolean isFirstEventInSeries(CalendarEventModel calendarEventModel, CalendarEventModel calendarEventModel2) {
        return calendarEventModel.mOriginalStart == calendarEventModel2.mStart;
    }

    void addRecurrenceRule(ContentValues contentValues, CalendarEventModel calendarEventModel) {
        contentValues.put("rrule", calendarEventModel.mRrule);
        long j = calendarEventModel.mEnd;
        long j2 = calendarEventModel.mStart;
        String str = calendarEventModel.mDuration;
        boolean z = calendarEventModel.mAllDay;
        if (j >= j2) {
            if (z) {
                str = "P" + ((((j - j2) + 86400000) - 1) / 86400000) + "D";
            } else {
                str = "P" + ((j - j2) / 1000) + "S";
            }
        } else if (TextUtils.isEmpty(str)) {
            if (z) {
                str = "P1D";
            } else {
                str = "P3600S";
            }
        }
        contentValues.put("duration", str);
        contentValues.put("dtend", (Long) null);
    }

    static void updateRecurrenceRule(int i, CalendarEventModel calendarEventModel, int i2) {
        EventRecurrence eventRecurrence = new EventRecurrence();
        if (i == 0) {
            calendarEventModel.mRrule = null;
            return;
        }
        if (i == 7) {
            return;
        }
        if (i == 1) {
            eventRecurrence.freq = 4;
        } else if (i == 2) {
            eventRecurrence.freq = 5;
            int[] iArr = {131072, 262144, 524288, 1048576, 2097152};
            int[] iArr2 = new int[5];
            for (int i3 = 0; i3 < 5; i3++) {
                iArr2[i3] = 0;
            }
            eventRecurrence.byday = iArr;
            eventRecurrence.bydayNum = iArr2;
            eventRecurrence.bydayCount = 5;
        } else if (i == 3) {
            eventRecurrence.freq = 5;
            Time time = new Time(calendarEventModel.mTimezone);
            time.set(calendarEventModel.mStart);
            eventRecurrence.byday = new int[]{EventRecurrence.timeDay2Day(time.weekDay)};
            eventRecurrence.bydayNum = new int[]{0};
            eventRecurrence.bydayCount = 1;
        } else if (i != 5) {
            if (i == 4) {
                eventRecurrence.freq = 6;
                eventRecurrence.bydayCount = 1;
                eventRecurrence.bymonthdayCount = 0;
                int[] iArr3 = new int[1];
                int[] iArr4 = new int[1];
                Time time2 = new Time(calendarEventModel.mTimezone);
                time2.set(calendarEventModel.mStart);
                int i4 = 1 + ((time2.monthDay - 1) / 7);
                if (i4 == 5) {
                    i4 = -1;
                }
                iArr4[0] = i4;
                iArr3[0] = EventRecurrence.timeDay2Day(time2.weekDay);
                eventRecurrence.byday = iArr3;
                eventRecurrence.bydayNum = iArr4;
            } else if (i == 6) {
                eventRecurrence.freq = 7;
            }
        } else {
            eventRecurrence.freq = 6;
            eventRecurrence.bydayCount = 0;
            eventRecurrence.bymonthdayCount = 1;
            Time time3 = new Time(calendarEventModel.mTimezone);
            time3.set(calendarEventModel.mStart);
            eventRecurrence.bymonthday = new int[]{time3.monthDay};
        }
        eventRecurrence.wkst = EventRecurrence.calendarDay2Day(i2);
        calendarEventModel.mRrule = eventRecurrence.toString();
    }

    public static void setModelFromCursor(CalendarEventModel calendarEventModel, Cursor cursor) {
        int i;
        if (calendarEventModel == null || cursor == null || cursor.getCount() != 1) {
            Log.wtf("EditEventHelper", "Attempted to build non-existent model or from an incorrect query.");
            return;
        }
        calendarEventModel.clear();
        cursor.moveToFirst();
        calendarEventModel.mId = cursor.getInt(0);
        calendarEventModel.mTitle = cursor.getString(1);
        calendarEventModel.mDescription = cursor.getString(2);
        calendarEventModel.mLocation = cursor.getString(3);
        calendarEventModel.mAllDay = cursor.getInt(4) != 0;
        calendarEventModel.mHasAlarm = cursor.getInt(5) != 0;
        calendarEventModel.mCalendarId = cursor.getInt(6);
        calendarEventModel.mStart = cursor.getLong(7);
        String string = cursor.getString(10);
        if (!TextUtils.isEmpty(string)) {
            calendarEventModel.mTimezone = string;
        }
        calendarEventModel.mRrule = cursor.getString(11);
        calendarEventModel.mSyncId = cursor.getString(12);
        calendarEventModel.mAvailability = cursor.getInt(13);
        int i2 = cursor.getInt(14);
        calendarEventModel.mOwnerAccount = cursor.getString(15);
        calendarEventModel.mHasAttendeeData = cursor.getInt(16) != 0;
        calendarEventModel.mOriginalSyncId = cursor.getString(17);
        calendarEventModel.mOriginalId = cursor.getLong(20);
        calendarEventModel.mOrganizer = cursor.getString(18);
        calendarEventModel.mIsOrganizer = calendarEventModel.mOwnerAccount.equalsIgnoreCase(calendarEventModel.mOrganizer);
        calendarEventModel.mGuestsCanModify = cursor.getInt(19) != 0;
        if (cursor.isNull(23)) {
            i = cursor.getInt(22);
        } else {
            i = cursor.getInt(23);
        }
        calendarEventModel.setEventColor(Utils.getDisplayColorFromColor(i));
        calendarEventModel.mIsLunar = cursor.getInt(25) != 0;
        if (i2 > 0) {
            i2--;
        }
        calendarEventModel.mAccessLevel = i2;
        calendarEventModel.mEventStatus = cursor.getInt(21);
        if (!TextUtils.isEmpty(r2)) {
            calendarEventModel.mDuration = cursor.getString(9);
        } else {
            calendarEventModel.mEnd = cursor.getLong(8);
        }
        calendarEventModel.mModelUpdatedWithEventCursor = true;
    }

    public static boolean setModelFromCalendarCursor(CalendarEventModel calendarEventModel, Cursor cursor) {
        if (calendarEventModel == null || cursor == null) {
            Log.wtf("EditEventHelper", "Attempted to build non-existent model or from an incorrect query.");
            return false;
        }
        if (calendarEventModel.mCalendarId == -1) {
            return false;
        }
        if (!calendarEventModel.mModelUpdatedWithEventCursor) {
            Log.wtf("EditEventHelper", "Can't update model with a Calendar cursor until it has seen an Event cursor.");
            return false;
        }
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (calendarEventModel.mCalendarId == cursor.getInt(0)) {
                calendarEventModel.mOrganizerCanRespond = cursor.getInt(4) != 0;
                calendarEventModel.mCalendarAccessLevel = cursor.getInt(5);
                calendarEventModel.mCalendarDisplayName = cursor.getString(1);
                calendarEventModel.setCalendarColor(Utils.getDisplayColorFromColor(cursor.getInt(3)));
                calendarEventModel.mCalendarAccountName = cursor.getString(11);
                calendarEventModel.mCalendarAccountType = cursor.getString(12);
                calendarEventModel.mCalendarMaxReminders = cursor.getInt(7);
                calendarEventModel.mCalendarAllowedReminders = cursor.getString(8);
                calendarEventModel.mCalendarAllowedAttendeeTypes = cursor.getString(9);
                calendarEventModel.mCalendarAllowedAvailability = cursor.getString(10);
                calendarEventModel.mAccountType = cursor.getString(12);
                return true;
            }
        }
        return false;
    }

    public static boolean canModifyEvent(CalendarEventModel calendarEventModel) {
        return canModifyCalendar(calendarEventModel) && (calendarEventModel.mIsOrganizer || calendarEventModel.mGuestsCanModify);
    }

    public static boolean canModifyCalendar(CalendarEventModel calendarEventModel) {
        return calendarEventModel.mCalendarAccessLevel >= 500 || calendarEventModel.mCalendarId == -1;
    }

    public static boolean canAddReminders(CalendarEventModel calendarEventModel) {
        return calendarEventModel.mCalendarAccessLevel >= 200;
    }

    public static boolean canRespond(CalendarEventModel calendarEventModel) {
        if (!canModifyCalendar(calendarEventModel)) {
            return false;
        }
        if (!calendarEventModel.mIsOrganizer) {
            return true;
        }
        if (calendarEventModel.mOrganizerCanRespond) {
            return ((calendarEventModel.mHasAttendeeData && calendarEventModel.mAttendeesList.size() == 0) || "LOCAL".equals(calendarEventModel.mAccountType)) ? false : true;
        }
        return false;
    }

    ContentValues getContentValuesFromModel(CalendarEventModel calendarEventModel) {
        long millis;
        long millis2;
        String str = calendarEventModel.mTitle;
        boolean z = calendarEventModel.mAllDay;
        String str2 = calendarEventModel.mRrule;
        String id = calendarEventModel.mTimezone;
        if (id == null) {
            id = TimeZone.getDefault().getID();
        }
        Time time = new Time(id);
        Time time2 = new Time(id);
        time.set(calendarEventModel.mStart);
        time2.set(calendarEventModel.mEnd);
        offsetStartTimeIfNecessary(time, time2, str2, calendarEventModel);
        ContentValues contentValues = new ContentValues();
        long j = calendarEventModel.mCalendarId;
        if (z) {
            id = "UTC";
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            time.timezone = "UTC";
            millis = time.normalize(true);
            time2.hour = 0;
            time2.minute = 0;
            time2.second = 0;
            time2.timezone = "UTC";
            millis2 = time2.normalize(true);
            long j2 = 86400000 + millis;
            if (millis2 < j2) {
                millis2 = j2;
            }
        } else {
            millis = time.toMillis(true);
            millis2 = time2.toMillis(true);
        }
        contentValues.put("calendar_id", Long.valueOf(j));
        contentValues.put("eventTimezone", id);
        contentValues.put("title", str);
        contentValues.put("allDay", Integer.valueOf(z ? 1 : 0));
        contentValues.put("dtstart", Long.valueOf(millis));
        contentValues.put("rrule", str2);
        if (!TextUtils.isEmpty(str2)) {
            addRecurrenceRule(contentValues, calendarEventModel);
        } else {
            contentValues.put("duration", (String) null);
            contentValues.put("dtend", Long.valueOf(millis2));
        }
        if (calendarEventModel.mDescription != null) {
            contentValues.put("description", calendarEventModel.mDescription.trim());
        } else {
            contentValues.put("description", (String) null);
        }
        if (calendarEventModel.mLocation != null) {
            contentValues.put("eventLocation", calendarEventModel.mLocation.trim());
        } else {
            contentValues.put("eventLocation", (String) null);
        }
        contentValues.put("availability", Integer.valueOf(calendarEventModel.mAvailability));
        contentValues.put("hasAttendeeData", Integer.valueOf(calendarEventModel.mHasAttendeeData ? 1 : 0));
        int i = calendarEventModel.mAccessLevel;
        if (i > 0) {
            i++;
        }
        contentValues.put("accessLevel", Integer.valueOf(i));
        contentValues.put("eventStatus", Integer.valueOf(calendarEventModel.mEventStatus));
        if (calendarEventModel.isEventColorInitialized()) {
            if (calendarEventModel.getEventColor() == calendarEventModel.getCalendarColor()) {
                contentValues.put("eventColor_index", "");
            } else {
                contentValues.put("eventColor_index", Integer.valueOf(calendarEventModel.getEventColorKey()));
            }
        }
        contentValues.put("isLunar", Integer.valueOf(calendarEventModel.mIsLunar ? 1 : 0));
        if ("LOCAL".equals(calendarEventModel.mAccountType)) {
            calendarEventModel.mHasAttendeeData = false;
        }
        return contentValues;
    }

    private void offsetStartTimeIfNecessary(Time time, Time time2, String str, CalendarEventModel calendarEventModel) {
        if (str == null || str.isEmpty()) {
            return;
        }
        this.mEventRecurrence.parse(str);
        if (this.mEventRecurrence.freq != 5 || this.mEventRecurrence.byday == null || this.mEventRecurrence.byday.length > this.mEventRecurrence.bydayCount) {
            return;
        }
        int iDay2TimeDay = EventRecurrence.day2TimeDay(this.mEventRecurrence.wkst);
        int i = time.weekDay;
        int i2 = Integer.MAX_VALUE;
        for (int i3 = 0; i3 < this.mEventRecurrence.bydayCount; i3++) {
            int iDay2TimeDay2 = EventRecurrence.day2TimeDay(this.mEventRecurrence.byday[i3]);
            if (iDay2TimeDay2 == i) {
                return;
            }
            if (iDay2TimeDay2 < iDay2TimeDay) {
                iDay2TimeDay2 += 7;
            }
            if (iDay2TimeDay2 > i && (iDay2TimeDay2 < i2 || i2 < i)) {
                i2 = iDay2TimeDay2;
            }
            if ((i2 == Integer.MAX_VALUE || i2 < i) && iDay2TimeDay2 < i2) {
                i2 = iDay2TimeDay2;
            }
        }
        if (i2 < i) {
            i2 += 7;
        }
        int i4 = i2 - i;
        time.monthDay += i4;
        time2.monthDay += i4;
        long jNormalize = time.normalize(true);
        long jNormalize2 = time2.normalize(true);
        calendarEventModel.mStart = jNormalize;
        calendarEventModel.mEnd = jNormalize2;
    }

    public static String extractDomain(String str) {
        int i;
        int iLastIndexOf = str.lastIndexOf(64);
        if (iLastIndexOf != -1 && (i = iLastIndexOf + 1) < str.length()) {
            return str.substring(i);
        }
        return null;
    }
}
