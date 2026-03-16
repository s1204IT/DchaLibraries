package com.android.providers.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.util.TimeUtils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.providers.calendar.CalendarCache;
import com.android.providers.calendar.MetaData;
import com.google.android.collect.Sets;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarProvider2 extends SQLiteContentProvider implements OnAccountsUpdateListener {
    private static final String[] DONT_CLONE_INTO_EXCEPTION;
    private static final String[] PROVIDER_WRITABLE_DEFAULT_COLUMNS;
    private static final String[] SYNC_WRITABLE_DEFAULT_COLUMNS;
    private static CalendarProvider2 mInstance;
    private static final HashMap<String, String> sAttendeesProjectionMap;
    private static final HashMap<String, String> sCalendarAlertsProjectionMap;
    private static final HashMap<String, String> sCalendarCacheProjectionMap;
    protected static final HashMap<String, String> sCalendarsProjectionMap;
    private static final HashMap<String, String> sColorsProjectionMap;
    private static final HashMap<String, String> sCountProjectionMap;
    private static final HashMap<String, String> sEventEntitiesProjectionMap;
    protected static final HashMap<String, String> sEventsProjectionMap;
    private static final HashMap<String, String> sInstancesProjectionMap;
    private static final HashMap<String, String> sRemindersProjectionMap;
    private static final UriMatcher sUriMatcher;
    protected CalendarAlarmManager mCalendarAlarm;
    CalendarCache mCalendarCache;
    private ContentResolver mContentResolver;
    private Context mContext;
    private CalendarDatabaseHelper mDbHelper;
    private CalendarInstancesHelper mInstancesHelper;
    MetaData mMetaData;
    private static final String[] ID_ONLY_PROJECTION = {"_id"};
    private static final String[] EVENTS_PROJECTION = {"_sync_id", "rrule", "rdate", "original_id", "original_sync_id"};
    private static final String[] COLORS_PROJECTION = {"account_name", "account_type", "color_type", "color_index", "color"};
    private static final String[] ACCOUNT_PROJECTION = {"account_name", "account_type"};
    private static final String[] ID_PROJECTION = {"_id", "event_id"};
    private static final String[] ALLDAY_TIME_PROJECTION = {"_id", "dtstart", "dtend", "duration"};
    private static final String[] sCalendarsIdProjection = {"_id"};
    private static final Pattern SEARCH_TOKEN_PATTERN = Pattern.compile("[^\\s\"'.?!,]+|\"([^\"]*)\"");
    private static final Pattern SEARCH_ESCAPE_PATTERN = Pattern.compile("([%_#])");
    private static final String[] SEARCH_COLUMNS = {"title", "description", "eventLocation", "group_concat(attendeeEmail)", "group_concat(attendeeName)"};
    private static final HashSet<String> ALLOWED_URI_PARAMETERS = Sets.newHashSet(new String[]{"caller_is_syncadapter", "account_name", "account_type"});
    private static final HashSet<String> ALLOWED_IN_EXCEPTION = new HashSet<>();
    private final Handler mBroadcastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Context context = CalendarProvider2.this.mContext;
            if (msg.what == 1) {
                CalendarProvider2.this.doSendUpdateNotification();
                context.stopService(new Intent(context, (Class<?>) EmptyService.class));
            }
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "onReceive() " + action);
            }
            if ("android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                CalendarProvider2.this.updateTimezoneDependentFields();
                CalendarProvider2.this.mCalendarAlarm.scheduleNextAlarm(false);
            } else if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                CalendarProvider2.this.updateTimezoneDependentFields();
                CalendarProvider2.this.mCalendarAlarm.scheduleNextAlarm(false);
            } else if ("android.intent.action.TIME_SET".equals(action)) {
                CalendarProvider2.this.mCalendarAlarm.scheduleNextAlarm(false);
            }
        }
    };

    static {
        ALLOWED_IN_EXCEPTION.add("_sync_id");
        ALLOWED_IN_EXCEPTION.add("sync_data1");
        ALLOWED_IN_EXCEPTION.add("sync_data7");
        ALLOWED_IN_EXCEPTION.add("sync_data3");
        ALLOWED_IN_EXCEPTION.add("title");
        ALLOWED_IN_EXCEPTION.add("eventLocation");
        ALLOWED_IN_EXCEPTION.add("description");
        ALLOWED_IN_EXCEPTION.add("eventColor");
        ALLOWED_IN_EXCEPTION.add("eventColor_index");
        ALLOWED_IN_EXCEPTION.add("eventStatus");
        ALLOWED_IN_EXCEPTION.add("selfAttendeeStatus");
        ALLOWED_IN_EXCEPTION.add("sync_data6");
        ALLOWED_IN_EXCEPTION.add("dtstart");
        ALLOWED_IN_EXCEPTION.add("eventTimezone");
        ALLOWED_IN_EXCEPTION.add("eventEndTimezone");
        ALLOWED_IN_EXCEPTION.add("duration");
        ALLOWED_IN_EXCEPTION.add("allDay");
        ALLOWED_IN_EXCEPTION.add("accessLevel");
        ALLOWED_IN_EXCEPTION.add("availability");
        ALLOWED_IN_EXCEPTION.add("hasAlarm");
        ALLOWED_IN_EXCEPTION.add("hasExtendedProperties");
        ALLOWED_IN_EXCEPTION.add("rrule");
        ALLOWED_IN_EXCEPTION.add("rdate");
        ALLOWED_IN_EXCEPTION.add("exrule");
        ALLOWED_IN_EXCEPTION.add("exdate");
        ALLOWED_IN_EXCEPTION.add("original_sync_id");
        ALLOWED_IN_EXCEPTION.add("originalInstanceTime");
        ALLOWED_IN_EXCEPTION.add("hasAttendeeData");
        ALLOWED_IN_EXCEPTION.add("guestsCanModify");
        ALLOWED_IN_EXCEPTION.add("guestsCanInviteOthers");
        ALLOWED_IN_EXCEPTION.add("guestsCanSeeGuests");
        ALLOWED_IN_EXCEPTION.add("organizer");
        ALLOWED_IN_EXCEPTION.add("customAppPackage");
        ALLOWED_IN_EXCEPTION.add("customAppUri");
        ALLOWED_IN_EXCEPTION.add("uid2445");
        DONT_CLONE_INTO_EXCEPTION = new String[]{"_sync_id", "sync_data1", "sync_data2", "sync_data3", "sync_data4", "sync_data5", "sync_data6", "sync_data7", "sync_data8", "sync_data9", "sync_data10"};
        SYNC_WRITABLE_DEFAULT_COLUMNS = new String[]{"dirty", "_sync_id"};
        PROVIDER_WRITABLE_DEFAULT_COLUMNS = new String[0];
        sUriMatcher = new UriMatcher(-1);
        sUriMatcher.addURI("com.android.calendar", "instances/when/*/*", 3);
        sUriMatcher.addURI("com.android.calendar", "instances/whenbyday/*/*", 15);
        sUriMatcher.addURI("com.android.calendar", "instances/search/*/*/*", 26);
        sUriMatcher.addURI("com.android.calendar", "instances/searchbyday/*/*/*", 27);
        sUriMatcher.addURI("com.android.calendar", "instances/groupbyday/*/*", 20);
        sUriMatcher.addURI("com.android.calendar", "events", 1);
        sUriMatcher.addURI("com.android.calendar", "events/#", 2);
        sUriMatcher.addURI("com.android.calendar", "event_entities", 18);
        sUriMatcher.addURI("com.android.calendar", "event_entities/#", 19);
        sUriMatcher.addURI("com.android.calendar", "calendars", 4);
        sUriMatcher.addURI("com.android.calendar", "calendars/#", 5);
        sUriMatcher.addURI("com.android.calendar", "calendar_entities", 24);
        sUriMatcher.addURI("com.android.calendar", "calendar_entities/#", 25);
        sUriMatcher.addURI("com.android.calendar", "attendees", 6);
        sUriMatcher.addURI("com.android.calendar", "attendees/#", 7);
        sUriMatcher.addURI("com.android.calendar", "reminders", 8);
        sUriMatcher.addURI("com.android.calendar", "reminders/#", 9);
        sUriMatcher.addURI("com.android.calendar", "extendedproperties", 10);
        sUriMatcher.addURI("com.android.calendar", "extendedproperties/#", 11);
        sUriMatcher.addURI("com.android.calendar", "calendar_alerts", 12);
        sUriMatcher.addURI("com.android.calendar", "calendar_alerts/#", 13);
        sUriMatcher.addURI("com.android.calendar", "calendar_alerts/by_instance", 14);
        sUriMatcher.addURI("com.android.calendar", "syncstate", 16);
        sUriMatcher.addURI("com.android.calendar", "syncstate/#", 17);
        sUriMatcher.addURI("com.android.calendar", "schedule_alarms", 21);
        sUriMatcher.addURI("com.android.calendar", "schedule_alarms_remove", 22);
        sUriMatcher.addURI("com.android.calendar", "time/#", 23);
        sUriMatcher.addURI("com.android.calendar", "time", 23);
        sUriMatcher.addURI("com.android.calendar", "properties", 28);
        sUriMatcher.addURI("com.android.calendar", "exception/#", 29);
        sUriMatcher.addURI("com.android.calendar", "exception/#/#", 30);
        sUriMatcher.addURI("com.android.calendar", "emma", 31);
        sUriMatcher.addURI("com.android.calendar", "colors", 32);
        sCountProjectionMap = new HashMap<>();
        sCountProjectionMap.put("_count", "COUNT(*)");
        sColorsProjectionMap = new HashMap<>();
        sColorsProjectionMap.put("_id", "_id");
        sColorsProjectionMap.put("data", "data");
        sColorsProjectionMap.put("account_name", "account_name");
        sColorsProjectionMap.put("account_type", "account_type");
        sColorsProjectionMap.put("color_index", "color_index");
        sColorsProjectionMap.put("color_type", "color_type");
        sColorsProjectionMap.put("color", "color");
        sCalendarsProjectionMap = new HashMap<>();
        sCalendarsProjectionMap.put("_id", "_id");
        sCalendarsProjectionMap.put("account_name", "account_name");
        sCalendarsProjectionMap.put("account_type", "account_type");
        sCalendarsProjectionMap.put("_sync_id", "_sync_id");
        sCalendarsProjectionMap.put("dirty", "dirty");
        sCalendarsProjectionMap.put("mutators", "mutators");
        sCalendarsProjectionMap.put("name", "name");
        sCalendarsProjectionMap.put("calendar_displayName", "calendar_displayName");
        sCalendarsProjectionMap.put("calendar_color", "calendar_color");
        sCalendarsProjectionMap.put("calendar_color_index", "calendar_color_index");
        sCalendarsProjectionMap.put("calendar_access_level", "calendar_access_level");
        sCalendarsProjectionMap.put("visible", "visible");
        sCalendarsProjectionMap.put("sync_events", "sync_events");
        sCalendarsProjectionMap.put("calendar_location", "calendar_location");
        sCalendarsProjectionMap.put("calendar_timezone", "calendar_timezone");
        sCalendarsProjectionMap.put("ownerAccount", "ownerAccount");
        sCalendarsProjectionMap.put("isPrimary", "COALESCE(isPrimary, ownerAccount = account_name)");
        sCalendarsProjectionMap.put("canOrganizerRespond", "canOrganizerRespond");
        sCalendarsProjectionMap.put("canModifyTimeZone", "canModifyTimeZone");
        sCalendarsProjectionMap.put("canPartiallyUpdate", "canPartiallyUpdate");
        sCalendarsProjectionMap.put("maxReminders", "maxReminders");
        sCalendarsProjectionMap.put("allowedReminders", "allowedReminders");
        sCalendarsProjectionMap.put("allowedAvailability", "allowedAvailability");
        sCalendarsProjectionMap.put("allowedAttendeeTypes", "allowedAttendeeTypes");
        sCalendarsProjectionMap.put("deleted", "deleted");
        sCalendarsProjectionMap.put("cal_sync1", "cal_sync1");
        sCalendarsProjectionMap.put("cal_sync2", "cal_sync2");
        sCalendarsProjectionMap.put("cal_sync3", "cal_sync3");
        sCalendarsProjectionMap.put("cal_sync4", "cal_sync4");
        sCalendarsProjectionMap.put("cal_sync5", "cal_sync5");
        sCalendarsProjectionMap.put("cal_sync6", "cal_sync6");
        sCalendarsProjectionMap.put("cal_sync7", "cal_sync7");
        sCalendarsProjectionMap.put("cal_sync8", "cal_sync8");
        sCalendarsProjectionMap.put("cal_sync9", "cal_sync9");
        sCalendarsProjectionMap.put("cal_sync10", "cal_sync10");
        sEventsProjectionMap = new HashMap<>();
        sEventsProjectionMap.put("account_name", "account_name");
        sEventsProjectionMap.put("account_type", "account_type");
        sEventsProjectionMap.put("title", "title");
        sEventsProjectionMap.put("eventLocation", "eventLocation");
        sEventsProjectionMap.put("description", "description");
        sEventsProjectionMap.put("eventStatus", "eventStatus");
        sEventsProjectionMap.put("eventColor", "eventColor");
        sEventsProjectionMap.put("eventColor_index", "eventColor_index");
        sEventsProjectionMap.put("selfAttendeeStatus", "selfAttendeeStatus");
        sEventsProjectionMap.put("dtstart", "dtstart");
        sEventsProjectionMap.put("dtend", "dtend");
        sEventsProjectionMap.put("eventTimezone", "eventTimezone");
        sEventsProjectionMap.put("eventEndTimezone", "eventEndTimezone");
        sEventsProjectionMap.put("duration", "duration");
        sEventsProjectionMap.put("allDay", "allDay");
        sEventsProjectionMap.put("accessLevel", "accessLevel");
        sEventsProjectionMap.put("availability", "availability");
        sEventsProjectionMap.put("hasAlarm", "hasAlarm");
        sEventsProjectionMap.put("hasExtendedProperties", "hasExtendedProperties");
        sEventsProjectionMap.put("rrule", "rrule");
        sEventsProjectionMap.put("rdate", "rdate");
        sEventsProjectionMap.put("exrule", "exrule");
        sEventsProjectionMap.put("exdate", "exdate");
        sEventsProjectionMap.put("original_sync_id", "original_sync_id");
        sEventsProjectionMap.put("original_id", "original_id");
        sEventsProjectionMap.put("originalInstanceTime", "originalInstanceTime");
        sEventsProjectionMap.put("originalAllDay", "originalAllDay");
        sEventsProjectionMap.put("lastDate", "lastDate");
        sEventsProjectionMap.put("hasAttendeeData", "hasAttendeeData");
        sEventsProjectionMap.put("calendar_id", "calendar_id");
        sEventsProjectionMap.put("guestsCanInviteOthers", "guestsCanInviteOthers");
        sEventsProjectionMap.put("guestsCanModify", "guestsCanModify");
        sEventsProjectionMap.put("guestsCanSeeGuests", "guestsCanSeeGuests");
        sEventsProjectionMap.put("organizer", "organizer");
        sEventsProjectionMap.put("isOrganizer", "isOrganizer");
        sEventsProjectionMap.put("customAppPackage", "customAppPackage");
        sEventsProjectionMap.put("customAppUri", "customAppUri");
        sEventsProjectionMap.put("uid2445", "uid2445");
        sEventsProjectionMap.put("deleted", "deleted");
        sEventsProjectionMap.put("_sync_id", "_sync_id");
        sAttendeesProjectionMap = new HashMap<>(sEventsProjectionMap);
        sRemindersProjectionMap = new HashMap<>(sEventsProjectionMap);
        sEventsProjectionMap.put("calendar_color", "calendar_color");
        sEventsProjectionMap.put("calendar_color_index", "calendar_color_index");
        sEventsProjectionMap.put("calendar_access_level", "calendar_access_level");
        sEventsProjectionMap.put("visible", "visible");
        sEventsProjectionMap.put("calendar_timezone", "calendar_timezone");
        sEventsProjectionMap.put("ownerAccount", "ownerAccount");
        sEventsProjectionMap.put("calendar_displayName", "calendar_displayName");
        sEventsProjectionMap.put("allowedReminders", "allowedReminders");
        sEventsProjectionMap.put("allowedAttendeeTypes", "allowedAttendeeTypes");
        sEventsProjectionMap.put("allowedAvailability", "allowedAvailability");
        sEventsProjectionMap.put("maxReminders", "maxReminders");
        sEventsProjectionMap.put("canOrganizerRespond", "canOrganizerRespond");
        sEventsProjectionMap.put("canModifyTimeZone", "canModifyTimeZone");
        sEventsProjectionMap.put("displayColor", "displayColor");
        sInstancesProjectionMap = new HashMap<>(sEventsProjectionMap);
        sCalendarAlertsProjectionMap = new HashMap<>(sEventsProjectionMap);
        sEventsProjectionMap.put("_id", "_id");
        sEventsProjectionMap.put("sync_data1", "sync_data1");
        sEventsProjectionMap.put("sync_data2", "sync_data2");
        sEventsProjectionMap.put("sync_data3", "sync_data3");
        sEventsProjectionMap.put("sync_data4", "sync_data4");
        sEventsProjectionMap.put("sync_data5", "sync_data5");
        sEventsProjectionMap.put("sync_data6", "sync_data6");
        sEventsProjectionMap.put("sync_data7", "sync_data7");
        sEventsProjectionMap.put("sync_data8", "sync_data8");
        sEventsProjectionMap.put("sync_data9", "sync_data9");
        sEventsProjectionMap.put("sync_data10", "sync_data10");
        sEventsProjectionMap.put("cal_sync1", "cal_sync1");
        sEventsProjectionMap.put("cal_sync2", "cal_sync2");
        sEventsProjectionMap.put("cal_sync3", "cal_sync3");
        sEventsProjectionMap.put("cal_sync4", "cal_sync4");
        sEventsProjectionMap.put("cal_sync5", "cal_sync5");
        sEventsProjectionMap.put("cal_sync6", "cal_sync6");
        sEventsProjectionMap.put("cal_sync7", "cal_sync7");
        sEventsProjectionMap.put("cal_sync8", "cal_sync8");
        sEventsProjectionMap.put("cal_sync9", "cal_sync9");
        sEventsProjectionMap.put("cal_sync10", "cal_sync10");
        sEventsProjectionMap.put("dirty", "dirty");
        sEventsProjectionMap.put("mutators", "mutators");
        sEventsProjectionMap.put("lastSynced", "lastSynced");
        sEventEntitiesProjectionMap = new HashMap<>();
        sEventEntitiesProjectionMap.put("title", "title");
        sEventEntitiesProjectionMap.put("eventLocation", "eventLocation");
        sEventEntitiesProjectionMap.put("description", "description");
        sEventEntitiesProjectionMap.put("eventStatus", "eventStatus");
        sEventEntitiesProjectionMap.put("eventColor", "eventColor");
        sEventEntitiesProjectionMap.put("eventColor_index", "eventColor_index");
        sEventEntitiesProjectionMap.put("selfAttendeeStatus", "selfAttendeeStatus");
        sEventEntitiesProjectionMap.put("dtstart", "dtstart");
        sEventEntitiesProjectionMap.put("dtend", "dtend");
        sEventEntitiesProjectionMap.put("eventTimezone", "eventTimezone");
        sEventEntitiesProjectionMap.put("eventEndTimezone", "eventEndTimezone");
        sEventEntitiesProjectionMap.put("duration", "duration");
        sEventEntitiesProjectionMap.put("allDay", "allDay");
        sEventEntitiesProjectionMap.put("accessLevel", "accessLevel");
        sEventEntitiesProjectionMap.put("availability", "availability");
        sEventEntitiesProjectionMap.put("hasAlarm", "hasAlarm");
        sEventEntitiesProjectionMap.put("hasExtendedProperties", "hasExtendedProperties");
        sEventEntitiesProjectionMap.put("rrule", "rrule");
        sEventEntitiesProjectionMap.put("rdate", "rdate");
        sEventEntitiesProjectionMap.put("exrule", "exrule");
        sEventEntitiesProjectionMap.put("exdate", "exdate");
        sEventEntitiesProjectionMap.put("original_sync_id", "original_sync_id");
        sEventEntitiesProjectionMap.put("original_id", "original_id");
        sEventEntitiesProjectionMap.put("originalInstanceTime", "originalInstanceTime");
        sEventEntitiesProjectionMap.put("originalAllDay", "originalAllDay");
        sEventEntitiesProjectionMap.put("lastDate", "lastDate");
        sEventEntitiesProjectionMap.put("hasAttendeeData", "hasAttendeeData");
        sEventEntitiesProjectionMap.put("calendar_id", "calendar_id");
        sEventEntitiesProjectionMap.put("guestsCanInviteOthers", "guestsCanInviteOthers");
        sEventEntitiesProjectionMap.put("guestsCanModify", "guestsCanModify");
        sEventEntitiesProjectionMap.put("guestsCanSeeGuests", "guestsCanSeeGuests");
        sEventEntitiesProjectionMap.put("organizer", "organizer");
        sEventEntitiesProjectionMap.put("isOrganizer", "isOrganizer");
        sEventEntitiesProjectionMap.put("customAppPackage", "customAppPackage");
        sEventEntitiesProjectionMap.put("customAppUri", "customAppUri");
        sEventEntitiesProjectionMap.put("uid2445", "uid2445");
        sEventEntitiesProjectionMap.put("deleted", "deleted");
        sEventEntitiesProjectionMap.put("_id", "_id");
        sEventEntitiesProjectionMap.put("_sync_id", "_sync_id");
        sEventEntitiesProjectionMap.put("sync_data1", "sync_data1");
        sEventEntitiesProjectionMap.put("sync_data2", "sync_data2");
        sEventEntitiesProjectionMap.put("sync_data3", "sync_data3");
        sEventEntitiesProjectionMap.put("sync_data4", "sync_data4");
        sEventEntitiesProjectionMap.put("sync_data5", "sync_data5");
        sEventEntitiesProjectionMap.put("sync_data6", "sync_data6");
        sEventEntitiesProjectionMap.put("sync_data7", "sync_data7");
        sEventEntitiesProjectionMap.put("sync_data8", "sync_data8");
        sEventEntitiesProjectionMap.put("sync_data9", "sync_data9");
        sEventEntitiesProjectionMap.put("sync_data10", "sync_data10");
        sEventEntitiesProjectionMap.put("dirty", "dirty");
        sEventEntitiesProjectionMap.put("mutators", "mutators");
        sEventEntitiesProjectionMap.put("lastSynced", "lastSynced");
        sEventEntitiesProjectionMap.put("cal_sync1", "cal_sync1");
        sEventEntitiesProjectionMap.put("cal_sync2", "cal_sync2");
        sEventEntitiesProjectionMap.put("cal_sync3", "cal_sync3");
        sEventEntitiesProjectionMap.put("cal_sync4", "cal_sync4");
        sEventEntitiesProjectionMap.put("cal_sync5", "cal_sync5");
        sEventEntitiesProjectionMap.put("cal_sync6", "cal_sync6");
        sEventEntitiesProjectionMap.put("cal_sync7", "cal_sync7");
        sEventEntitiesProjectionMap.put("cal_sync8", "cal_sync8");
        sEventEntitiesProjectionMap.put("cal_sync9", "cal_sync9");
        sEventEntitiesProjectionMap.put("cal_sync10", "cal_sync10");
        sInstancesProjectionMap.put("deleted", "Events.deleted as deleted");
        sInstancesProjectionMap.put("begin", "begin");
        sInstancesProjectionMap.put("end", "end");
        sInstancesProjectionMap.put("event_id", "Instances.event_id AS event_id");
        sInstancesProjectionMap.put("_id", "Instances._id AS _id");
        sInstancesProjectionMap.put("startDay", "startDay");
        sInstancesProjectionMap.put("endDay", "endDay");
        sInstancesProjectionMap.put("startMinute", "startMinute");
        sInstancesProjectionMap.put("endMinute", "endMinute");
        sAttendeesProjectionMap.put("event_id", "event_id");
        sAttendeesProjectionMap.put("_id", "Attendees._id AS _id");
        sAttendeesProjectionMap.put("attendeeName", "attendeeName");
        sAttendeesProjectionMap.put("attendeeEmail", "attendeeEmail");
        sAttendeesProjectionMap.put("attendeeStatus", "attendeeStatus");
        sAttendeesProjectionMap.put("attendeeRelationship", "attendeeRelationship");
        sAttendeesProjectionMap.put("attendeeType", "attendeeType");
        sAttendeesProjectionMap.put("attendeeIdentity", "attendeeIdentity");
        sAttendeesProjectionMap.put("attendeeIdNamespace", "attendeeIdNamespace");
        sAttendeesProjectionMap.put("deleted", "Events.deleted AS deleted");
        sAttendeesProjectionMap.put("_sync_id", "Events._sync_id AS _sync_id");
        sRemindersProjectionMap.put("event_id", "event_id");
        sRemindersProjectionMap.put("_id", "Reminders._id AS _id");
        sRemindersProjectionMap.put("minutes", "minutes");
        sRemindersProjectionMap.put("method", "method");
        sRemindersProjectionMap.put("deleted", "Events.deleted AS deleted");
        sRemindersProjectionMap.put("_sync_id", "Events._sync_id AS _sync_id");
        sCalendarAlertsProjectionMap.put("event_id", "event_id");
        sCalendarAlertsProjectionMap.put("_id", "CalendarAlerts._id AS _id");
        sCalendarAlertsProjectionMap.put("begin", "begin");
        sCalendarAlertsProjectionMap.put("end", "end");
        sCalendarAlertsProjectionMap.put("alarmTime", "alarmTime");
        sCalendarAlertsProjectionMap.put("notifyTime", "notifyTime");
        sCalendarAlertsProjectionMap.put("state", "state");
        sCalendarAlertsProjectionMap.put("minutes", "minutes");
        sCalendarCacheProjectionMap = new HashMap<>();
        sCalendarCacheProjectionMap.put("key", "key");
        sCalendarCacheProjectionMap.put("value", "value");
    }

    @Override
    protected CalendarDatabaseHelper getDatabaseHelper(Context context) {
        return CalendarDatabaseHelper.getInstance(context);
    }

    protected static CalendarProvider2 getInstance() {
        return mInstance;
    }

    @Override
    public void shutdown() {
        if (this.mDbHelper != null) {
            this.mDbHelper.close();
            this.mDbHelper = null;
            this.mDb = null;
        }
    }

    @Override
    public boolean onCreate() {
        super.onCreate();
        setAppOps(8, 9);
        try {
            return initialize();
        } catch (RuntimeException e) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "Cannot start provider", e);
            }
            return false;
        }
    }

    private boolean initialize() {
        mInstance = this;
        this.mContext = getContext();
        this.mContentResolver = this.mContext.getContentResolver();
        this.mDbHelper = (CalendarDatabaseHelper) getDatabaseHelper();
        this.mDb = this.mDbHelper.getWritableDatabase();
        this.mMetaData = new MetaData(this.mDbHelper);
        this.mInstancesHelper = new CalendarInstancesHelper(this.mDbHelper, this.mMetaData);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        filter.addAction("android.intent.action.TIME_SET");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        this.mCalendarCache = new CalendarCache(this.mDbHelper);
        initCalendarAlarm();
        postInitialize();
        return true;
    }

    protected void initCalendarAlarm() {
        this.mCalendarAlarm = getOrCreateCalendarAlarmManager();
    }

    synchronized CalendarAlarmManager getOrCreateCalendarAlarmManager() {
        if (this.mCalendarAlarm == null) {
            this.mCalendarAlarm = new CalendarAlarmManager(this.mContext);
            Log.i("CalendarProvider2", "Created " + this.mCalendarAlarm + "(" + this + ")");
        }
        return this.mCalendarAlarm;
    }

    protected void postInitialize() {
        Thread thread = new PostInitializeThread();
        thread.start();
    }

    private class PostInitializeThread extends Thread {
        private PostInitializeThread() {
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            CalendarProvider2.this.verifyAccounts();
            try {
                CalendarProvider2.this.doUpdateTimezoneDependentFields();
            } catch (IllegalStateException e) {
            }
        }
    }

    private void verifyAccounts() {
        AccountManager.get(getContext()).addOnAccountsUpdatedListener(this, null, false);
        removeStaleAccounts(AccountManager.get(getContext()).getAccounts());
    }

    protected void updateTimezoneDependentFields() {
        Thread thread = new TimezoneCheckerThread();
        thread.start();
    }

    private class TimezoneCheckerThread extends Thread {
        private TimezoneCheckerThread() {
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            CalendarProvider2.this.doUpdateTimezoneDependentFields();
        }
    }

    private boolean isLocalSameAsInstancesTimezone() {
        String localTimezone = TimeZone.getDefault().getID();
        return TextUtils.equals(this.mCalendarCache.readTimezoneInstances(), localTimezone);
    }

    protected void doUpdateTimezoneDependentFields() {
        try {
            String timezoneType = this.mCalendarCache.readTimezoneType();
            if (timezoneType == null || !timezoneType.equals("home")) {
                if (!isSameTimezoneDatabaseVersion()) {
                    String localTimezone = TimeZone.getDefault().getID();
                    doProcessEventRawTimes(localTimezone, TimeUtils.getTimeZoneDatabaseVersion());
                }
                if (isLocalSameAsInstancesTimezone()) {
                    this.mCalendarAlarm.rescheduleMissedAlarms();
                }
            }
        } catch (SQLException e) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "doUpdateTimezoneDependentFields() failed", e);
            }
            try {
                this.mMetaData.clearInstanceRange();
            } catch (SQLException e2) {
                if (Log.isLoggable("CalendarProvider2", 6)) {
                    Log.e("CalendarProvider2", "clearInstanceRange() also failed: " + e2);
                }
            }
        }
    }

    protected void doProcessEventRawTimes(String localTimezone, String timeZoneDatabaseVersion) {
        this.mDb.beginTransaction();
        try {
            updateEventsStartEndFromEventRawTimesLocked();
            updateTimezoneDatabaseVersion(timeZoneDatabaseVersion);
            this.mCalendarCache.writeTimezoneInstances(localTimezone);
            regenerateInstancesTable();
            this.mDb.setTransactionSuccessful();
        } finally {
            this.mDb.endTransaction();
        }
    }

    private void updateEventsStartEndFromEventRawTimesLocked() {
        Cursor cursor = this.mDb.rawQuery("SELECT event_id, dtstart2445, dtend2445, eventTimezone FROM EventsRawTimes, Events WHERE event_id = Events._id", null);
        while (cursor.moveToNext()) {
            try {
                long eventId = cursor.getLong(0);
                String dtStart2445 = cursor.getString(1);
                String dtEnd2445 = cursor.getString(2);
                String eventTimezone = cursor.getString(3);
                if (dtStart2445 == null && dtEnd2445 == null) {
                    if (Log.isLoggable("CalendarProvider2", 6)) {
                        Log.e("CalendarProvider2", "Event " + eventId + " has dtStart2445 and dtEnd2445 null at the same time in EventsRawTimes!");
                    }
                } else {
                    updateEventsStartEndLocked(eventId, eventTimezone, dtStart2445, dtEnd2445);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private long get2445ToMillis(String timezone, String dt2445) {
        if (dt2445 == null) {
            if (!Log.isLoggable("CalendarProvider2", 2)) {
                return 0L;
            }
            Log.v("CalendarProvider2", "Cannot parse null RFC2445 date");
            return 0L;
        }
        Time time = timezone != null ? new Time(timezone) : new Time();
        try {
            time.parse(dt2445);
            return time.toMillis(true);
        } catch (TimeFormatException e) {
            if (!Log.isLoggable("CalendarProvider2", 6)) {
                return 0L;
            }
            Log.e("CalendarProvider2", "Cannot parse RFC2445 date " + dt2445);
            return 0L;
        }
    }

    private void updateEventsStartEndLocked(long eventId, String timezone, String dtStart2445, String dtEnd2445) {
        ContentValues values = new ContentValues();
        values.put("dtstart", Long.valueOf(get2445ToMillis(timezone, dtStart2445)));
        values.put("dtend", Long.valueOf(get2445ToMillis(timezone, dtEnd2445)));
        int result = this.mDb.update("Events", values, "_id=?", new String[]{String.valueOf(eventId)});
        if (result == 0 && Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "Could not update Events table with values " + values);
        }
    }

    private void updateTimezoneDatabaseVersion(String timeZoneDatabaseVersion) {
        try {
            this.mCalendarCache.writeTimezoneDatabaseVersion(timeZoneDatabaseVersion);
        } catch (CalendarCache.CacheException e) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "Could not write timezone database version in the cache");
            }
        }
    }

    protected boolean isSameTimezoneDatabaseVersion() {
        String timezoneDatabaseVersion = this.mCalendarCache.readTimezoneDatabaseVersion();
        if (timezoneDatabaseVersion == null) {
            return false;
        }
        return TextUtils.equals(timezoneDatabaseVersion, TimeUtils.getTimeZoneDatabaseVersion());
    }

    protected String getTimezoneDatabaseVersion() {
        String timezoneDatabaseVersion = this.mCalendarCache.readTimezoneDatabaseVersion();
        if (timezoneDatabaseVersion == null) {
            return "";
        }
        if (Log.isLoggable("CalendarProvider2", 4)) {
            Log.i("CalendarProvider2", "timezoneDatabaseVersion = " + timezoneDatabaseVersion);
            return timezoneDatabaseVersion;
        }
        return timezoneDatabaseVersion;
    }

    private boolean isHomeTimezone() {
        String type = this.mCalendarCache.readTimezoneType();
        return "home".equals(type);
    }

    private void regenerateInstancesTable() {
        long now = System.currentTimeMillis();
        String instancesTimezone = this.mCalendarCache.readTimezoneInstances();
        Time time = new Time(instancesTimezone);
        time.set(now);
        time.monthDay = 1;
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        long begin = time.normalize(true);
        long end = begin + 5356800000L;
        Cursor cursor = null;
        try {
            cursor = handleInstanceQuery(new SQLiteQueryBuilder(), begin, end, new String[]{"_id"}, null, null, null, false, true, instancesTimezone, isHomeTimezone());
            this.mCalendarAlarm.rescheduleMissedAlarms();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void notifyChange(boolean syncToNetwork) {
        this.mContentResolver.notifyChange(CalendarContract.CONTENT_URI, (ContentObserver) null, syncToNetwork);
    }

    @Override
    protected boolean shouldSyncFor(Uri uri) {
        int match = sUriMatcher.match(uri);
        return (match == 12 || match == 13 || match == 14) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        long identity = clearCallingIdentityInternal();
        try {
            return queryInternal(uri, projection, selection, selectionArgs, sortOrder);
        } finally {
            restoreCallingIdentityInternal(identity);
        }
    }

    private Cursor queryInternal(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "query uri - " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1:
                qb.setTables("view_events");
                qb.setProjectionMap(sEventsProjectionMap);
                selection = appendLastSyncedColumnToSelection(appendAccountToSelection(uri, selection, "account_name", "account_type"), uri);
                break;
            case 2:
                qb.setTables("view_events");
                qb.setProjectionMap(sEventsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere("_id=?");
                break;
            case 3:
            case 15:
                try {
                    long begin = Long.valueOf(uri.getPathSegments().get(2)).longValue();
                    try {
                        long end = Long.valueOf(uri.getPathSegments().get(3)).longValue();
                        String instancesTimezone = this.mCalendarCache.readTimezoneInstances();
                        return handleInstanceQuery(qb, begin, end, projection, selection, selectionArgs, sortOrder, match == 15, false, instancesTimezone, isHomeTimezone());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cannot parse end " + uri.getPathSegments().get(3));
                    }
                } catch (NumberFormatException e2) {
                    throw new IllegalArgumentException("Cannot parse begin " + uri.getPathSegments().get(2));
                }
            case 4:
            case 24:
                qb.setTables("Calendars");
                qb.setProjectionMap(sCalendarsProjectionMap);
                selection = appendAccountToSelection(uri, selection, "account_name", "account_type");
                break;
            case 5:
            case 25:
                qb.setTables("Calendars");
                qb.setProjectionMap(sCalendarsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere("_id=?");
                break;
            case 6:
                qb.setTables("Attendees, Events, Calendars");
                qb.setProjectionMap(sAttendeesProjectionMap);
                qb.appendWhere("Events._id=Attendees.event_id AND Events.calendar_id=Calendars._id");
                break;
            case 7:
                qb.setTables("Attendees, Events, Calendars");
                qb.setProjectionMap(sAttendeesProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere("Attendees._id=? AND Events._id=Attendees.event_id AND Events.calendar_id=Calendars._id");
                break;
            case 8:
                qb.setTables("Reminders");
                break;
            case 9:
                qb.setTables("Reminders, Events, Calendars");
                qb.setProjectionMap(sRemindersProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb.appendWhere("Reminders._id=? AND Events._id=Reminders.event_id AND Events.calendar_id=Calendars._id");
                break;
            case 10:
                qb.setTables("ExtendedProperties");
                break;
            case 11:
                qb.setTables("ExtendedProperties");
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere("ExtendedProperties._id=?");
                break;
            case 12:
                qb.setTables("CalendarAlerts, view_events");
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                qb.appendWhere("view_events._id=CalendarAlerts.event_id");
                break;
            case 13:
                qb.setTables("CalendarAlerts, view_events");
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb.appendWhere("view_events._id=CalendarAlerts.event_id AND CalendarAlerts._id=?");
                break;
            case 14:
                qb.setTables("CalendarAlerts, view_events");
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                qb.appendWhere("view_events._id=CalendarAlerts.event_id");
                groupBy = "event_id,begin";
                break;
            case 16:
                return this.mDbHelper.getSyncState().query(db, projection, selection, selectionArgs, sortOrder);
            case 17:
                String selectionWithId = "_id=?" + (selection == null ? "" : " AND (" + selection + ")");
                return this.mDbHelper.getSyncState().query(db, projection, selectionWithId, insertSelectionArg(selectionArgs, String.valueOf(ContentUris.parseId(uri))), sortOrder);
            case 18:
                qb.setTables("view_events");
                qb.setProjectionMap(sEventEntitiesProjectionMap);
                selection = appendLastSyncedColumnToSelection(appendAccountToSelection(uri, selection, "account_name", "account_type"), uri);
                break;
            case 19:
                qb.setTables("view_events");
                qb.setProjectionMap(sEventEntitiesProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere("_id=?");
                break;
            case 20:
                try {
                    int startDay = Integer.valueOf(uri.getPathSegments().get(2)).intValue();
                    try {
                        int endDay = Integer.valueOf(uri.getPathSegments().get(3)).intValue();
                        String instancesTimezone2 = this.mCalendarCache.readTimezoneInstances();
                        return handleEventDayQuery(qb, startDay, endDay, projection, selection, instancesTimezone2, isHomeTimezone());
                    } catch (NumberFormatException e3) {
                        throw new IllegalArgumentException("Cannot parse end day " + uri.getPathSegments().get(3));
                    }
                } catch (NumberFormatException e4) {
                    throw new IllegalArgumentException("Cannot parse start day " + uri.getPathSegments().get(2));
                }
            case 21:
            case 22:
            case 23:
            case 29:
            case 30:
            case 31:
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
            case 26:
            case 27:
                try {
                    long begin2 = Long.valueOf(uri.getPathSegments().get(2)).longValue();
                    try {
                        long end2 = Long.valueOf(uri.getPathSegments().get(3)).longValue();
                        String instancesTimezone3 = this.mCalendarCache.readTimezoneInstances();
                        String query = uri.getPathSegments().get(4);
                        return handleInstanceSearchQuery(qb, begin2, end2, query, projection, selection, selectionArgs, sortOrder, match == 27, instancesTimezone3, isHomeTimezone());
                    } catch (NumberFormatException e5) {
                        throw new IllegalArgumentException("Cannot parse end " + uri.getPathSegments().get(3));
                    }
                } catch (NumberFormatException e6) {
                    throw new IllegalArgumentException("Cannot parse begin " + uri.getPathSegments().get(2));
                }
            case 28:
                qb.setTables("CalendarCache");
                qb.setProjectionMap(sCalendarCacheProjectionMap);
                break;
            case 32:
                qb.setTables("Colors");
                qb.setProjectionMap(sColorsProjectionMap);
                selection = appendAccountToSelection(uri, selection, "account_name", "account_type");
                break;
        }
        return query(db, qb, projection, selection, selectionArgs, sortOrder, groupBy, null);
    }

    private void validateUriParameters(Set<String> queryParameterNames) {
        for (String parameterName : queryParameterNames) {
            if (!ALLOWED_URI_PARAMETERS.contains(parameterName)) {
                throw new IllegalArgumentException("Invalid URI parameter: " + parameterName);
            }
        }
    }

    private Cursor query(SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection, String selection, String[] selectionArgs, String sortOrder, String groupBy, String limit) {
        if (projection != null && projection.length == 1 && "_count".equals(projection[0])) {
            qb.setProjectionMap(sCountProjectionMap);
        }
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "query sql - projection: " + Arrays.toString(projection) + " selection: " + selection + " selectionArgs: " + Arrays.toString(selectionArgs) + " sortOrder: " + sortOrder + " groupBy: " + groupBy + " limit: " + limit);
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
        if (c != null) {
            c.setNotificationUri(this.mContentResolver, CalendarContract.Events.CONTENT_URI);
        }
        return c;
    }

    private Cursor handleInstanceQuery(SQLiteQueryBuilder qb, long rangeBegin, long rangeEnd, String[] projection, String selection, String[] selectionArgs, String sort, boolean searchByDay, boolean forceExpansion, String instancesTimezone, boolean isHomeTimezone) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        qb.setTables("Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)");
        qb.setProjectionMap(sInstancesProjectionMap);
        if (searchByDay) {
            Time time = new Time(instancesTimezone);
            long beginMs = time.setJulianDay((int) rangeBegin);
            long endMs = time.setJulianDay(((int) rangeEnd) + 1);
            acquireInstanceRange(beginMs, endMs, true, forceExpansion, instancesTimezone, isHomeTimezone);
            qb.appendWhere("startDay<=? AND endDay>=?");
        } else {
            acquireInstanceRange(rangeBegin, rangeEnd, true, forceExpansion, instancesTimezone, isHomeTimezone);
            qb.appendWhere("begin<=? AND end>=?");
        }
        String[] newSelectionArgs = {String.valueOf(rangeEnd), String.valueOf(rangeBegin)};
        return qb.query(this.mDb, projection, selection, selectionArgs == null ? newSelectionArgs : (String[]) combine(newSelectionArgs, selectionArgs), null, null, sort);
    }

    private static <T> T[] combine(T[]... tArr) {
        if (tArr.length == 0) {
            throw new IllegalArgumentException("Must supply at least 1 array to combine");
        }
        int length = 0;
        for (T[] tArr2 : tArr) {
            length += tArr2.length;
        }
        T[] tArr3 = (T[]) ((Object[]) Array.newInstance(tArr[0].getClass().getComponentType(), length));
        int length2 = 0;
        for (T[] tArr4 : tArr) {
            System.arraycopy(tArr4, 0, tArr3, length2, tArr4.length);
            length2 += tArr4.length;
        }
        return tArr3;
    }

    String escapeSearchToken(String token) {
        Matcher matcher = SEARCH_ESCAPE_PATTERN.matcher(token);
        return matcher.replaceAll("#$1");
    }

    String[] tokenizeSearchQuery(String query) {
        String token;
        List<String> matchList = new ArrayList<>();
        Matcher matcher = SEARCH_TOKEN_PATTERN.matcher(query);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                token = matcher.group(1);
            } else {
                token = matcher.group();
            }
            matchList.add(escapeSearchToken(token));
        }
        return (String[]) matchList.toArray(new String[matchList.size()]);
    }

    String constructSearchWhere(String[] tokens) {
        if (tokens.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < tokens.length; j++) {
            sb.append("(");
            for (int i = 0; i < SEARCH_COLUMNS.length; i++) {
                sb.append(SEARCH_COLUMNS[i]);
                sb.append(" LIKE ? ESCAPE \"");
                sb.append("#");
                sb.append("\" ");
                if (i < SEARCH_COLUMNS.length - 1) {
                    sb.append("OR ");
                }
            }
            sb.append(")");
            if (j < tokens.length - 1) {
                sb.append(" AND ");
            }
        }
        return sb.toString();
    }

    String[] constructSearchArgs(String[] tokens, long rangeBegin, long rangeEnd) {
        int numCols = SEARCH_COLUMNS.length;
        int numArgs = (tokens.length * numCols) + 2;
        String[] selectionArgs = new String[numArgs];
        selectionArgs[0] = String.valueOf(rangeEnd);
        selectionArgs[1] = String.valueOf(rangeBegin);
        for (int j = 0; j < tokens.length; j++) {
            int start = (numCols * j) + 2;
            for (int i = start; i < start + numCols; i++) {
                selectionArgs[i] = "%" + tokens[j] + "%";
            }
        }
        return selectionArgs;
    }

    private Cursor handleInstanceSearchQuery(SQLiteQueryBuilder qb, long rangeBegin, long rangeEnd, String query, String[] projection, String selection, String[] selectionArgs, String sort, boolean searchByDay, String instancesTimezone, boolean isHomeTimezone) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        qb.setTables("(Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)) LEFT OUTER JOIN Attendees ON (Attendees.event_id=Events._id)");
        qb.setProjectionMap(sInstancesProjectionMap);
        String[] tokens = tokenizeSearchQuery(query);
        String[] newSelectionArgs = constructSearchArgs(tokens, rangeBegin, rangeEnd);
        String[] selectionArgs2 = selectionArgs == null ? newSelectionArgs : (String[]) combine(newSelectionArgs, selectionArgs);
        String searchWhere = constructSearchWhere(tokens);
        if (searchByDay) {
            Time time = new Time(instancesTimezone);
            long beginMs = time.setJulianDay((int) rangeBegin);
            long endMs = time.setJulianDay(((int) rangeEnd) + 1);
            acquireInstanceRange(beginMs, endMs, true, false, instancesTimezone, isHomeTimezone);
            qb.appendWhere("startDay<=? AND endDay>=?");
        } else {
            acquireInstanceRange(rangeBegin, rangeEnd, true, false, instancesTimezone, isHomeTimezone);
            qb.appendWhere("begin<=? AND end>=?");
        }
        return qb.query(this.mDb, projection, selection, selectionArgs2, "Instances._id", searchWhere, sort);
    }

    private Cursor handleEventDayQuery(SQLiteQueryBuilder qb, int begin, int end, String[] projection, String selection, String instancesTimezone, boolean isHomeTimezone) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        qb.setTables("Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)");
        qb.setProjectionMap(sInstancesProjectionMap);
        Time time = new Time(instancesTimezone);
        long beginMs = time.setJulianDay(begin);
        long endMs = time.setJulianDay(end + 1);
        acquireInstanceRange(beginMs, endMs, true, false, instancesTimezone, isHomeTimezone);
        qb.appendWhere("startDay<=? AND endDay>=?");
        String[] selectionArgs = {String.valueOf(end), String.valueOf(begin)};
        return qb.query(this.mDb, projection, selection, selectionArgs, "startDay", null, null);
    }

    private void acquireInstanceRange(long begin, long end, boolean useMinimumExpansionWindow, boolean forceExpansion, String instancesTimezone, boolean isHomeTimezone) {
        this.mDb.beginTransaction();
        try {
            acquireInstanceRangeLocked(begin, end, useMinimumExpansionWindow, forceExpansion, instancesTimezone, isHomeTimezone);
            this.mDb.setTransactionSuccessful();
        } finally {
            this.mDb.endTransaction();
        }
    }

    void acquireInstanceRangeLocked(long begin, long end, boolean useMinimumExpansionWindow, boolean forceExpansion, String instancesTimezone, boolean isHomeTimezone) {
        boolean timezoneChanged;
        long expandBegin = begin;
        long expandEnd = end;
        if (instancesTimezone == null) {
            Log.e("CalendarProvider2", "Cannot run acquireInstanceRangeLocked() because instancesTimezone is null");
            return;
        }
        if (useMinimumExpansionWindow) {
            long span = end - begin;
            if (span < 5356800000L) {
                long additionalRange = (5356800000L - span) / 2;
                expandBegin -= additionalRange;
                expandEnd += additionalRange;
            }
        }
        MetaData.Fields fields = this.mMetaData.getFieldsLocked();
        long maxInstance = fields.maxInstance;
        long minInstance = fields.minInstance;
        if (isHomeTimezone) {
            String previousTimezone = this.mCalendarCache.readTimezoneInstancesPrevious();
            timezoneChanged = !instancesTimezone.equals(previousTimezone);
        } else {
            String localTimezone = TimeZone.getDefault().getID();
            timezoneChanged = !instancesTimezone.equals(localTimezone);
            if (timezoneChanged) {
                instancesTimezone = localTimezone;
            }
        }
        if (maxInstance == 0 || timezoneChanged || forceExpansion) {
            this.mDb.execSQL("DELETE FROM Instances;");
            if (Log.isLoggable("CalendarProvider2", 2)) {
                Log.v("CalendarProvider2", "acquireInstanceRangeLocked() deleted Instances, timezone changed: " + timezoneChanged);
            }
            this.mInstancesHelper.expandInstanceRangeLocked(expandBegin, expandEnd, instancesTimezone);
            this.mMetaData.writeLocked(instancesTimezone, expandBegin, expandEnd);
            String timezoneType = this.mCalendarCache.readTimezoneType();
            this.mCalendarCache.writeTimezoneInstances(instancesTimezone);
            if ("auto".equals(timezoneType)) {
                String prevTZ = this.mCalendarCache.readTimezoneInstancesPrevious();
                if (TextUtils.equals("GMT", prevTZ)) {
                    this.mCalendarCache.writeTimezoneInstancesPrevious(instancesTimezone);
                    return;
                }
                return;
            }
            return;
        }
        if (begin >= minInstance && end <= maxInstance) {
            if (Log.isLoggable("CalendarProvider2", 2)) {
                Log.v("CalendarProvider2", "Canceled instance query (" + expandBegin + ", " + expandEnd + ") falls within previously expanded range.");
                return;
            }
            return;
        }
        if (begin < minInstance) {
            this.mInstancesHelper.expandInstanceRangeLocked(expandBegin, minInstance, instancesTimezone);
            minInstance = expandBegin;
        }
        if (end > maxInstance) {
            this.mInstancesHelper.expandInstanceRangeLocked(maxInstance, expandEnd, instancesTimezone);
            maxInstance = expandEnd;
        }
        this.mMetaData.writeLocked(instancesTimezone, minInstance, maxInstance);
    }

    @Override
    public String getType(Uri url) {
        int match = sUriMatcher.match(url);
        switch (match) {
            case 1:
                return "vnd.android.cursor.dir/event";
            case 2:
                return "vnd.android.cursor.item/event";
            case 3:
            case 15:
            case 20:
                return "vnd.android.cursor.dir/event-instance";
            case 4:
            case 5:
            case 6:
            case 7:
            case 10:
            case 11:
            case 16:
            case 17:
            case 18:
            case 19:
            case 21:
            case 22:
            case 24:
            case 25:
            case 26:
            case 27:
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
            case 8:
                return "vnd.android.cursor.dir/reminder";
            case 9:
                return "vnd.android.cursor.item/reminder";
            case 12:
                return "vnd.android.cursor.dir/calendar-alert";
            case 13:
                return "vnd.android.cursor.item/calendar-alert";
            case 14:
                return "vnd.android.cursor.dir/calendar-alert-by-instance";
            case 23:
                return "time/epoch";
            case 28:
                return "vnd.android.cursor.dir/property";
        }
    }

    public static boolean isRecurrenceEvent(String rrule, String rdate, String originalId, String originalSyncId) {
        return (TextUtils.isEmpty(rrule) && TextUtils.isEmpty(rdate) && TextUtils.isEmpty(originalId) && TextUtils.isEmpty(originalSyncId)) ? false : true;
    }

    private boolean fixAllDayTime(ContentValues values, ContentValues modValues) {
        int len;
        Integer allDayObj = values.getAsInteger("allDay");
        if (allDayObj == null || allDayObj.intValue() == 0) {
            return false;
        }
        boolean neededCorrection = false;
        Long dtstart = values.getAsLong("dtstart");
        Long dtend = values.getAsLong("dtend");
        String duration = values.getAsString("duration");
        Time time = new Time();
        time.clear("UTC");
        time.set(dtstart.longValue());
        if (time.hour != 0 || time.minute != 0 || time.second != 0) {
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            modValues.put("dtstart", Long.valueOf(time.toMillis(true)));
            neededCorrection = true;
        }
        if (dtend != null) {
            time.clear("UTC");
            time.set(dtend.longValue());
            if (time.hour != 0 || time.minute != 0 || time.second != 0) {
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                modValues.put("dtend", Long.valueOf(time.toMillis(true)));
                neededCorrection = true;
            }
        }
        if (duration != null && (len = duration.length()) != 0 && duration.charAt(0) == 'P' && duration.charAt(len - 1) == 'S') {
            int seconds = Integer.parseInt(duration.substring(1, len - 1));
            int days = ((86400 + seconds) - 1) / 86400;
            modValues.put("duration", "P" + days + "D");
            return true;
        }
        return neededCorrection;
    }

    private void checkAllowedInException(Set<String> keys) {
        for (String str : keys) {
            if (!ALLOWED_IN_EXCEPTION.contains(str.intern())) {
                throw new IllegalArgumentException("Exceptions can't overwrite " + str);
            }
        }
    }

    private static ContentValues setRecurrenceEnd(ContentValues values, long endTimeMillis) {
        boolean origAllDay = values.getAsBoolean("allDay").booleanValue();
        String origRrule = values.getAsString("rrule");
        EventRecurrence origRecurrence = new EventRecurrence();
        origRecurrence.parse(origRrule);
        long startTimeMillis = values.getAsLong("dtstart").longValue();
        Time dtstart = new Time();
        dtstart.timezone = values.getAsString("eventTimezone");
        dtstart.set(startTimeMillis);
        ContentValues updateValues = new ContentValues();
        if (origRecurrence.count > 0) {
            RecurrenceSet recurSet = new RecurrenceSet(values);
            RecurrenceProcessor recurProc = new RecurrenceProcessor();
            try {
                long[] recurrences = recurProc.expand(dtstart, recurSet, startTimeMillis, endTimeMillis);
                if (recurrences.length == 0) {
                    throw new RuntimeException("can't use this method on first instance");
                }
                EventRecurrence excepRecurrence = new EventRecurrence();
                excepRecurrence.parse(origRrule);
                excepRecurrence.count -= recurrences.length;
                values.put("rrule", excepRecurrence.toString());
                origRecurrence.count = recurrences.length;
            } catch (DateException de) {
                throw new RuntimeException(de);
            }
        } else {
            Time untilTime = new Time();
            untilTime.timezone = "UTC";
            untilTime.set(endTimeMillis - 1000);
            if (origAllDay) {
                untilTime.second = 0;
                untilTime.minute = 0;
                untilTime.hour = 0;
                untilTime.allDay = true;
                untilTime.normalize(false);
                dtstart.second = 0;
                dtstart.minute = 0;
                dtstart.hour = 0;
                dtstart.allDay = true;
                dtstart.timezone = "UTC";
            }
            origRecurrence.until = untilTime.format2445();
        }
        updateValues.put("rrule", origRecurrence.toString());
        updateValues.put("dtstart", Long.valueOf(dtstart.normalize(true)));
        return updateValues;
    }

    private long handleInsertException(long originalEventId, ContentValues modValues, boolean callerIsSyncAdapter) {
        long newEventId;
        long start;
        Account account;
        Long originalInstanceTime = modValues.getAsLong("originalInstanceTime");
        if (originalInstanceTime == null) {
            throw new IllegalArgumentException("Exceptions must specify originalInstanceTime");
        }
        checkAllowedInException(modValues.keySet());
        if (!callerIsSyncAdapter) {
            modValues.put("dirty", (Boolean) true);
            addMutator(modValues, "mutators");
        }
        this.mDb.beginTransaction();
        Cursor cursor = null;
        try {
            Cursor cursor2 = this.mDb.query("Events", null, "_id=?", new String[]{String.valueOf(originalEventId)}, null, null, null);
            if (cursor2.getCount() != 1) {
                Log.e("CalendarProvider2", "Original event ID " + originalEventId + " lookup failed (count is " + cursor2.getCount() + ")");
                newEventId = -1;
                if (cursor2 != null) {
                    cursor2.close();
                }
                this.mDb.endTransaction();
            } else {
                String color_index = modValues.getAsString("eventColor_index");
                if (!TextUtils.isEmpty(color_index)) {
                    int calIdCol = cursor2.getColumnIndex("calendar_id");
                    Long calId = Long.valueOf(cursor2.getLong(calIdCol));
                    String accountName = null;
                    String accountType = null;
                    if (calId != null && (account = getAccount(calId.longValue())) != null) {
                        accountName = account.name;
                        accountType = account.type;
                    }
                    verifyColorExists(accountName, accountType, color_index, 1);
                }
                cursor2.moveToFirst();
                int rruleCol = cursor2.getColumnIndex("rrule");
                if (TextUtils.isEmpty(cursor2.getString(rruleCol))) {
                    Log.e("CalendarProvider2", "Original event has no rrule");
                    newEventId = -1;
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    this.mDb.endTransaction();
                } else {
                    int originalIdCol = cursor2.getColumnIndex("original_id");
                    if (TextUtils.isEmpty(cursor2.getString(originalIdCol))) {
                        boolean createSingleException = TextUtils.isEmpty(modValues.getAsString("rrule"));
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor2, values);
                        cursor2.close();
                        cursor = null;
                        boolean createNewEvent = true;
                        if (createSingleException) {
                            String _id = values.getAsString("_id");
                            String _sync_id = values.getAsString("_sync_id");
                            boolean allDay = values.getAsBoolean("allDay").booleanValue();
                            String[] arr$ = DONT_CLONE_INTO_EXCEPTION;
                            for (String str : arr$) {
                                values.remove(str);
                            }
                            values.putAll(modValues);
                            values.put("original_id", _id);
                            values.put("original_sync_id", _sync_id);
                            values.put("originalAllDay", Boolean.valueOf(allDay));
                            if (!values.containsKey("eventStatus")) {
                                values.put("eventStatus", (Integer) 0);
                            }
                            values.remove("rrule");
                            values.remove("rdate");
                            values.remove("exrule");
                            values.remove("exdate");
                            Duration duration = new Duration();
                            String durationStr = values.getAsString("duration");
                            try {
                                duration.parse(durationStr);
                                if (modValues.containsKey("dtstart")) {
                                    start = values.getAsLong("dtstart").longValue();
                                } else {
                                    start = values.getAsLong("originalInstanceTime").longValue();
                                    values.put("dtstart", Long.valueOf(start));
                                }
                                values.put("dtend", Long.valueOf(duration.getMillis() + start));
                                values.remove("duration");
                            } catch (Exception ex) {
                                Log.w("CalendarProvider2", "Bad duration in recurring event: " + durationStr, ex);
                                newEventId = -1;
                                if (0 != 0) {
                                    cursor.close();
                                }
                                this.mDb.endTransaction();
                            }
                        } else {
                            boolean canceling = values.getAsInteger("eventStatus").intValue() == 2;
                            if (originalInstanceTime.equals(values.getAsLong("dtstart"))) {
                                if (canceling) {
                                    Log.d("CalendarProvider2", "Note: canceling entire event via exception call");
                                }
                                if (!validateRecurrenceRule(modValues)) {
                                    throw new IllegalArgumentException("Invalid recurrence rule: " + values.getAsString("rrule"));
                                }
                                modValues.remove("originalInstanceTime");
                                this.mDb.update("Events", modValues, "_id=?", new String[]{Long.toString(originalEventId)});
                                createNewEvent = false;
                            } else {
                                ContentValues splitValues = setRecurrenceEnd(values, originalInstanceTime.longValue());
                                this.mDb.update("Events", splitValues, "_id=?", new String[]{Long.toString(originalEventId)});
                                values.putAll(modValues);
                                values.remove("originalInstanceTime");
                            }
                        }
                        if (createNewEvent) {
                            values.remove("_id");
                            if (callerIsSyncAdapter) {
                                scrubEventData(values, null);
                            } else {
                                validateEventData(values);
                            }
                            newEventId = this.mDb.insert("Events", null, values);
                            if (newEventId < 0) {
                                Log.w("CalendarProvider2", "Unable to add exception to recurring event");
                                Log.w("CalendarProvider2", "Values: " + values);
                                newEventId = -1;
                                if (0 != 0) {
                                    cursor.close();
                                }
                                this.mDb.endTransaction();
                            } else {
                                this.mInstancesHelper.updateInstancesLocked(values, newEventId, true, this.mDb);
                                CalendarDatabaseHelper.copyEventRelatedTables(this.mDb, newEventId, originalEventId);
                                if (modValues.containsKey("selfAttendeeStatus")) {
                                    long calendarId = values.getAsLong("calendar_id").longValue();
                                    String accountName2 = getOwner(calendarId);
                                    if (accountName2 != null) {
                                        ContentValues attValues = new ContentValues();
                                        attValues.put("attendeeStatus", modValues.getAsString("selfAttendeeStatus"));
                                        int count = this.mDb.update("Attendees", attValues, "event_id=? AND attendeeEmail=?", new String[]{String.valueOf(newEventId), accountName2});
                                        if (count != 1 && count != 2) {
                                            Log.e("CalendarProvider2", "Attendee status update on event=" + newEventId + " touched " + count + " rows. Expected one or two rows.");
                                            throw new RuntimeException("Status update WTF");
                                        }
                                    }
                                }
                            }
                        } else {
                            this.mInstancesHelper.updateInstancesLocked(values, originalEventId, false, this.mDb);
                            newEventId = originalEventId;
                        }
                        this.mDb.setTransactionSuccessful();
                        if (0 != 0) {
                            cursor.close();
                        }
                        this.mDb.endTransaction();
                    } else {
                        Log.e("CalendarProvider2", "Original event is an exception");
                        newEventId = -1;
                        if (cursor2 != null) {
                            cursor2.close();
                        }
                        this.mDb.endTransaction();
                    }
                }
            }
            return newEventId;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            this.mDb.endTransaction();
            throw th;
        }
    }

    private void backfillExceptionOriginalIds(long id, ContentValues values) {
        String syncId = values.getAsString("_sync_id");
        String rrule = values.getAsString("rrule");
        String rdate = values.getAsString("rdate");
        String calendarId = values.getAsString("calendar_id");
        if (TextUtils.isEmpty(syncId) || TextUtils.isEmpty(calendarId)) {
            return;
        }
        if (!TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate)) {
            ContentValues originalValues = new ContentValues();
            originalValues.put("original_id", Long.valueOf(id));
            this.mDb.update("Events", originalValues, "original_sync_id=? AND calendar_id=?", new String[]{syncId, calendarId});
        }
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "insertInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(1, uri, values, callerIsSyncAdapter, match, null, null);
        this.mDb = this.mDbHelper.getWritableDatabase();
        long id = 0;
        switch (match) {
            case 1:
                if (!callerIsSyncAdapter) {
                    values.put("dirty", (Integer) 1);
                    addMutator(values, "mutators");
                }
                if (!values.containsKey("dtstart")) {
                    if (values.containsKey("original_sync_id") && values.containsKey("originalInstanceTime") && 2 == values.getAsInteger("eventStatus").intValue()) {
                        long origStart = values.getAsLong("originalInstanceTime").longValue();
                        values.put("dtstart", Long.valueOf(origStart));
                        values.put("dtend", Long.valueOf(origStart));
                        values.put("eventTimezone", "UTC");
                    } else {
                        throw new RuntimeException("DTSTART field missing from event");
                    }
                }
                ContentValues updatedValues = new ContentValues(values);
                if (callerIsSyncAdapter) {
                    scrubEventData(updatedValues, null);
                } else {
                    validateEventData(updatedValues);
                }
                ContentValues updatedValues2 = updateLastDate(updatedValues);
                if (updatedValues2 == null) {
                    throw new RuntimeException("Could not insert event.");
                }
                Long calendar_id = updatedValues2.getAsLong("calendar_id");
                if (calendar_id == null) {
                    throw new IllegalArgumentException("New events must specify a calendar id");
                }
                String color_id = updatedValues2.getAsString("eventColor_index");
                if (!TextUtils.isEmpty(color_id)) {
                    Account account = getAccount(calendar_id.longValue());
                    String accountName = null;
                    String accountType = null;
                    if (account != null) {
                        accountName = account.name;
                        accountType = account.type;
                    }
                    int color = verifyColorExists(accountName, accountType, color_id, 1);
                    updatedValues2.put("eventColor", Integer.valueOf(color));
                }
                String owner = null;
                if (!updatedValues2.containsKey("organizer") && (owner = getOwner(calendar_id.longValue())) != null) {
                    updatedValues2.put("organizer", owner);
                }
                if (updatedValues2.containsKey("original_sync_id") && !updatedValues2.containsKey("original_id")) {
                    long originalId = getOriginalId(updatedValues2.getAsString("original_sync_id"), updatedValues2.getAsString("calendar_id"));
                    if (originalId != -1) {
                        updatedValues2.put("original_id", Long.valueOf(originalId));
                    }
                } else if (!updatedValues2.containsKey("original_sync_id") && updatedValues2.containsKey("original_id")) {
                    String originalSyncId = getOriginalSyncId(updatedValues2.getAsLong("original_id").longValue());
                    if (!TextUtils.isEmpty(originalSyncId)) {
                        updatedValues2.put("original_sync_id", originalSyncId);
                    }
                }
                if (fixAllDayTime(updatedValues2, updatedValues2) && Log.isLoggable("CalendarProvider2", 5)) {
                    Log.w("CalendarProvider2", "insertInTransaction: allDay is true but sec, min, hour were not 0.");
                }
                updatedValues2.remove("hasAlarm");
                id = this.mDbHelper.eventsInsert(updatedValues2);
                if (id != -1) {
                    updateEventRawTimesLocked(id, updatedValues2);
                    this.mInstancesHelper.updateInstancesLocked(updatedValues2, id, true, this.mDb);
                    if (values.containsKey("selfAttendeeStatus")) {
                        int status = values.getAsInteger("selfAttendeeStatus").intValue();
                        if (owner == null) {
                            owner = getOwner(calendar_id.longValue());
                        }
                        createAttendeeEntry(id, status, owner);
                    }
                    backfillExceptionOriginalIds(id, values);
                    sendUpdateNotification(id, callerIsSyncAdapter);
                }
                break;
                break;
            case 2:
            case 3:
            case 9:
            case 11:
            case 13:
            case 15:
            case 20:
            case 28:
                throw new UnsupportedOperationException("Cannot insert into that URL: " + uri);
            case 4:
                Integer syncEvents = values.getAsInteger("sync_events");
                if (syncEvents != null && syncEvents.intValue() == 1) {
                    Account account2 = new Account(values.getAsString("account_name"), values.getAsString("account_type"));
                    String eventsUrl = values.getAsString("cal_sync1");
                    this.mDbHelper.scheduleSync(account2, false, eventsUrl);
                }
                String cal_color_id = values.getAsString("calendar_color_index");
                if (!TextUtils.isEmpty(cal_color_id)) {
                    int color2 = verifyColorExists(values.getAsString("account_name"), values.getAsString("account_type"), cal_color_id, 0);
                    values.put("calendar_color", Integer.valueOf(color2));
                }
                id = this.mDbHelper.calendarsInsert(values);
                sendUpdateNotification(id, callerIsSyncAdapter);
                break;
            case 5:
            case 7:
            case 14:
            case 17:
            case 18:
            case 19:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 30:
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
            case 6:
                if (!values.containsKey("event_id")) {
                    throw new IllegalArgumentException("Attendees values must contain an event_id");
                }
                if (!callerIsSyncAdapter) {
                    Long eventId = values.getAsLong("event_id");
                    this.mDbHelper.duplicateEvent(eventId.longValue());
                    setEventDirty(eventId.longValue());
                }
                id = this.mDbHelper.attendeesInsert(values);
                updateEventAttendeeStatus(this.mDb, values);
                break;
                break;
            case 8:
                Long eventIdObj = values.getAsLong("event_id");
                if (eventIdObj == null) {
                    throw new IllegalArgumentException("Reminders values must contain a numeric event_id");
                }
                if (!callerIsSyncAdapter) {
                    this.mDbHelper.duplicateEvent(eventIdObj.longValue());
                    setEventDirty(eventIdObj.longValue());
                }
                id = this.mDbHelper.remindersInsert(values);
                setHasAlarm(eventIdObj.longValue(), 1);
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "insertInternal() changing reminder");
                }
                this.mCalendarAlarm.scheduleNextAlarm(false);
                break;
                break;
            case 10:
                if (!values.containsKey("event_id")) {
                    throw new IllegalArgumentException("ExtendedProperties values must contain an event_id");
                }
                if (!callerIsSyncAdapter) {
                    Long eventId2 = values.getAsLong("event_id");
                    this.mDbHelper.duplicateEvent(eventId2.longValue());
                    setEventDirty(eventId2.longValue());
                }
                id = this.mDbHelper.extendedPropertiesInsert(values);
                break;
                break;
            case 12:
                if (!values.containsKey("event_id")) {
                    throw new IllegalArgumentException("CalendarAlerts values must contain an event_id");
                }
                id = this.mDbHelper.calendarAlertsInsert(values);
                break;
                break;
            case 16:
                id = this.mDbHelper.getSyncState().insert(this.mDb, values);
                break;
            case 29:
                long originalEventId = ContentUris.parseId(uri);
                id = handleInsertException(originalEventId, values, callerIsSyncAdapter);
                break;
            case 31:
                handleEmmaRequest(values);
                break;
            case 32:
                String accountName2 = uri.getQueryParameter("account_name");
                String accountType2 = uri.getQueryParameter("account_type");
                String colorIndex = values.getAsString("color_index");
                if (TextUtils.isEmpty(accountName2) || TextUtils.isEmpty(accountType2)) {
                    throw new IllegalArgumentException("Account name and type must be non empty parameters for " + uri);
                }
                if (TextUtils.isEmpty(colorIndex)) {
                    throw new IllegalArgumentException("COLOR_INDEX must be non empty for " + uri);
                }
                if (!values.containsKey("color_type") || !values.containsKey("color")) {
                    throw new IllegalArgumentException("New colors must contain COLOR_TYPE and COLOR");
                }
                values.put("account_name", accountName2);
                values.put("account_type", accountType2);
                Cursor c = null;
                try {
                    long colorType = values.getAsLong("color_type").longValue();
                    c = getColorByTypeIndex(accountName2, accountType2, colorType, colorIndex);
                    if (c.getCount() != 0) {
                        throw new IllegalArgumentException("color type " + colorType + " and index " + colorIndex + " already exists for account and type provided");
                    }
                    id = this.mDbHelper.colorsInsert(values);
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                break;
                break;
        }
        if (id < 0) {
            return null;
        }
        return ContentUris.withAppendedId(uri, id);
    }

    private static void handleEmmaRequest(ContentValues values) {
        String cmd = values.getAsString("cmd");
        if (cmd.equals("start")) {
            Log.d("CalendarProvider2", "Emma coverage testing started");
            return;
        }
        if (cmd.equals("stop")) {
            String filename = values.getAsString("outputFileName");
            File coverageFile = new File(filename);
            try {
                Class<?> emmaRTClass = Class.forName("com.vladium.emma.rt.RT");
                Method dumpCoverageMethod = emmaRTClass.getMethod("dumpCoverageData", coverageFile.getClass(), Boolean.TYPE, Boolean.TYPE);
                dumpCoverageMethod.invoke(null, coverageFile, false, false);
                Log.d("CalendarProvider2", "Emma coverage data written to " + filename);
            } catch (Exception e) {
                throw new RuntimeException("Emma coverage dump failed", e);
            }
        }
    }

    private boolean validateRecurrenceRule(ContentValues values) {
        String rrule = values.getAsString("rrule");
        if (!TextUtils.isEmpty(rrule)) {
            String[] ruleList = rrule.split("\n");
            for (String recur : ruleList) {
                EventRecurrence er = new EventRecurrence();
                try {
                    er.parse(recur);
                } catch (EventRecurrence.InvalidFormatException e) {
                    Log.w("CalendarProvider2", "Invalid recurrence rule: " + recur);
                    dumpEventNoPII(values);
                    return false;
                }
            }
        }
        return true;
    }

    private void dumpEventNoPII(ContentValues values) {
        if (values != null) {
            StringBuilder bob = new StringBuilder();
            bob.append("dtStart:       ").append(values.getAsLong("dtstart"));
            bob.append("\ndtEnd:         ").append(values.getAsLong("dtend"));
            bob.append("\nall_day:       ").append(values.getAsInteger("allDay"));
            bob.append("\ntz:            ").append(values.getAsString("eventTimezone"));
            bob.append("\ndur:           ").append(values.getAsString("duration"));
            bob.append("\nrrule:         ").append(values.getAsString("rrule"));
            bob.append("\nrdate:         ").append(values.getAsString("rdate"));
            bob.append("\nlast_date:     ").append(values.getAsLong("lastDate"));
            bob.append("\nid:            ").append(values.getAsLong("_id"));
            bob.append("\nsync_id:       ").append(values.getAsString("_sync_id"));
            bob.append("\nori_id:        ").append(values.getAsLong("original_id"));
            bob.append("\nori_sync_id:   ").append(values.getAsString("original_sync_id"));
            bob.append("\nori_inst_time: ").append(values.getAsLong("originalInstanceTime"));
            bob.append("\nori_all_day:   ").append(values.getAsInteger("originalAllDay"));
            Log.i("CalendarProvider2", bob.toString());
        }
    }

    private void scrubEventData(ContentValues values, ContentValues modValues) {
        boolean hasDtend = values.getAsLong("dtend") != null;
        boolean hasDuration = !TextUtils.isEmpty(values.getAsString("duration"));
        boolean hasRrule = !TextUtils.isEmpty(values.getAsString("rrule"));
        boolean hasRdate = !TextUtils.isEmpty(values.getAsString("rdate"));
        boolean hasOriginalEvent = !TextUtils.isEmpty(values.getAsString("original_sync_id"));
        boolean hasOriginalInstanceTime = values.getAsLong("originalInstanceTime") != null;
        if (hasRrule || hasRdate) {
            if (!validateRecurrenceRule(values)) {
                throw new IllegalArgumentException("Invalid recurrence rule: " + values.getAsString("rrule"));
            }
            if (hasDtend || !hasDuration || hasOriginalEvent || hasOriginalInstanceTime) {
                Log.d("CalendarProvider2", "Scrubbing DTEND, ORIGINAL_SYNC_ID, ORIGINAL_INSTANCE_TIME");
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Invalid values for recurrence: " + values);
                }
                values.remove("dtend");
                values.remove("original_sync_id");
                values.remove("originalInstanceTime");
                if (modValues != null) {
                    modValues.putNull("dtend");
                    modValues.putNull("original_sync_id");
                    modValues.putNull("originalInstanceTime");
                    return;
                }
                return;
            }
            return;
        }
        if (hasOriginalEvent || hasOriginalInstanceTime) {
            if (!hasDtend || hasDuration || !hasOriginalEvent || !hasOriginalInstanceTime) {
                Log.d("CalendarProvider2", "Scrubbing DURATION");
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Invalid values for recurrence exception: " + values);
                }
                values.remove("duration");
                if (modValues != null) {
                    modValues.putNull("duration");
                    return;
                }
                return;
            }
            return;
        }
        if (!hasDtend || hasDuration) {
            Log.d("CalendarProvider2", "Scrubbing DURATION");
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Invalid values for event: " + values);
            }
            values.remove("duration");
            if (modValues != null) {
                modValues.putNull("duration");
            }
        }
    }

    private void validateEventData(ContentValues values) {
        if (TextUtils.isEmpty(values.getAsString("calendar_id"))) {
            throw new IllegalArgumentException("Event values must include a calendar_id");
        }
        if (TextUtils.isEmpty(values.getAsString("eventTimezone"))) {
            throw new IllegalArgumentException("Event values must include an eventTimezone");
        }
        boolean hasDtstart = values.getAsLong("dtstart") != null;
        boolean hasDtend = values.getAsLong("dtend") != null;
        boolean hasDuration = !TextUtils.isEmpty(values.getAsString("duration"));
        boolean hasRrule = !TextUtils.isEmpty(values.getAsString("rrule"));
        boolean hasRdate = !TextUtils.isEmpty(values.getAsString("rdate"));
        if ((hasRrule || hasRdate) && !validateRecurrenceRule(values)) {
            throw new IllegalArgumentException("Invalid recurrence rule: " + values.getAsString("rrule"));
        }
        if (!hasDtstart) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("DTSTART cannot be empty.");
        }
        if (!hasDuration && !hasDtend) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("DTEND and DURATION cannot both be null for an event.");
        }
        if (hasDuration && hasDtend) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("Cannot have both DTEND and DURATION in an event");
        }
    }

    private void setEventDirty(long eventId) {
        String newMutators;
        String mutators = DatabaseUtils.stringForQuery(this.mDb, "SELECT mutators FROM Events WHERE _id=?", new String[]{String.valueOf(eventId)});
        String packageName = getCallingPackageName();
        if (TextUtils.isEmpty(mutators)) {
            newMutators = packageName;
        } else {
            String[] strings = mutators.split(",");
            boolean found = false;
            int len$ = strings.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String string = strings[i$];
                if (!string.equals(packageName)) {
                    i$++;
                } else {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newMutators = mutators + "," + packageName;
            } else {
                newMutators = mutators;
            }
        }
        this.mDb.execSQL("UPDATE Events SET dirty=1,mutators=?  WHERE _id=?", new Object[]{newMutators, Long.valueOf(eventId)});
    }

    private long getOriginalId(String originalSyncId, String calendarId) {
        if (TextUtils.isEmpty(originalSyncId) || TextUtils.isEmpty(calendarId)) {
            return -1L;
        }
        long originalId = -1;
        Cursor c = null;
        try {
            c = query(CalendarContract.Events.CONTENT_URI, ID_ONLY_PROJECTION, "_sync_id=? AND calendar_id=?", new String[]{originalSyncId, calendarId}, null);
            if (c != null && c.moveToFirst()) {
                originalId = c.getLong(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String getOriginalSyncId(long originalId) {
        String originalSyncId = null;
        if (originalId != -1) {
            originalSyncId = null;
            Cursor c = null;
            try {
                c = query(CalendarContract.Events.CONTENT_URI, new String[]{"_sync_id"}, "_id=?", new String[]{Long.toString(originalId)}, null);
                if (c != null && c.moveToFirst()) {
                    originalSyncId = c.getString(0);
                }
                if (c != null) {
                    c.close();
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }
        return originalSyncId;
    }

    private Cursor getColorByTypeIndex(String accountName, String accountType, long colorType, String colorIndex) {
        return this.mDb.query("Colors", COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=? AND color_index=?", new String[]{accountName, accountType, Long.toString(colorType), colorIndex}, null, null, null);
    }

    private String getOwner(long calId) {
        if (calId < 0) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "Calendar Id is not valid: " + calId);
            }
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId), new String[]{"ownerAccount"}, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Couldn't find " + calId + " in Calendars table");
                }
                return null;
            }
            String emailAddress = cursor.getString(0);
            if (cursor != null) {
                cursor.close();
                return emailAddress;
            }
            return emailAddress;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Account getAccount(long calId) {
        Cursor cursor = null;
        try {
            cursor = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId), ACCOUNT_PROJECTION, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Couldn't find " + calId + " in Calendars table");
                }
                return null;
            }
            Account account = new Account(cursor.getString(0), cursor.getString(1));
            if (cursor != null) {
                cursor.close();
            }
            return account;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void createAttendeeEntry(long eventId, int status, String emailAddress) {
        ContentValues values = new ContentValues();
        values.put("event_id", Long.valueOf(eventId));
        values.put("attendeeStatus", Integer.valueOf(status));
        values.put("attendeeType", (Integer) 0);
        values.put("attendeeRelationship", (Integer) 1);
        values.put("attendeeEmail", emailAddress);
        this.mDbHelper.attendeesInsert(values);
    }

    private void updateEventAttendeeStatus(SQLiteDatabase db, ContentValues attendeeValues) {
        Long eventIdObj = attendeeValues.getAsLong("event_id");
        if (eventIdObj == null) {
            Log.w("CalendarProvider2", "Attendee update values don't include an event_id");
            return;
        }
        long eventId = eventIdObj.longValue();
        Cursor cursor = null;
        try {
            Cursor cursor2 = query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), new String[]{"calendar_id"}, null, null, null);
            if (cursor2 == null || !cursor2.moveToFirst()) {
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Couldn't find " + eventId + " in Events table");
                }
                if (cursor2 != null) {
                    cursor2.close();
                    return;
                }
                return;
            }
            long calId = cursor2.getLong(0);
            if (cursor2 != null) {
                cursor2.close();
            }
            cursor = null;
            try {
                Cursor cursor3 = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId), new String[]{"ownerAccount"}, null, null, null);
                if (cursor3 == null || !cursor3.moveToFirst()) {
                    if (Log.isLoggable("CalendarProvider2", 3)) {
                        Log.d("CalendarProvider2", "Couldn't find " + calId + " in Calendars table");
                    }
                    if (cursor3 != null) {
                        cursor3.close();
                        return;
                    }
                    return;
                }
                String calendarEmail = cursor3.getString(0);
                if (cursor3 != null) {
                    cursor3.close();
                }
                if (calendarEmail != null) {
                    String attendeeEmail = null;
                    if (attendeeValues.containsKey("attendeeEmail")) {
                        attendeeEmail = attendeeValues.getAsString("attendeeEmail");
                    }
                    if (calendarEmail.equals(attendeeEmail)) {
                        int status = 0;
                        Integer relationObj = attendeeValues.getAsInteger("attendeeRelationship");
                        if (relationObj != null) {
                            int rel = relationObj.intValue();
                            if (rel == 2) {
                                status = 1;
                            }
                        }
                        Integer statusObj = attendeeValues.getAsInteger("attendeeStatus");
                        if (statusObj != null) {
                            status = statusObj.intValue();
                        }
                        ContentValues values = new ContentValues();
                        values.put("selfAttendeeStatus", Integer.valueOf(status));
                        db.update("Events", values, "_id=?", new String[]{String.valueOf(eventId)});
                    }
                }
            } finally {
            }
        } finally {
        }
    }

    private void setHasAlarm(long eventId, int val) {
        ContentValues values = new ContentValues();
        values.put("hasAlarm", Integer.valueOf(val));
        int count = this.mDb.update("Events", values, "_id=?", new String[]{String.valueOf(eventId)});
        if (count != 1) {
            Log.w("CalendarProvider2", "setHasAlarm on event " + eventId + " updated " + count + " rows (expected 1)");
        }
    }

    long calculateLastDate(ContentValues values) throws DateException {
        long lastMillis;
        if (!values.containsKey("dtstart")) {
            if (values.containsKey("dtend") || values.containsKey("rrule") || values.containsKey("duration") || values.containsKey("eventTimezone") || values.containsKey("rdate") || values.containsKey("exrule") || values.containsKey("exdate")) {
                throw new RuntimeException("DTSTART field missing from event");
            }
            return -1L;
        }
        long dtstartMillis = values.getAsLong("dtstart").longValue();
        Long dtEnd = values.getAsLong("dtend");
        if (dtEnd != null) {
            long lastMillis2 = dtEnd.longValue();
            return lastMillis2;
        }
        Duration duration = new Duration();
        String durationStr = values.getAsString("duration");
        if (durationStr != null) {
            duration.parse(durationStr);
        }
        try {
            RecurrenceSet recur = new RecurrenceSet(values);
            if (recur != null && recur.hasRecurrence()) {
                String tz = values.getAsString("eventTimezone");
                if (TextUtils.isEmpty(tz)) {
                    tz = "UTC";
                }
                Time dtstartLocal = new Time(tz);
                dtstartLocal.set(dtstartMillis);
                RecurrenceProcessor rp = new RecurrenceProcessor();
                lastMillis = rp.getLastOccurence(dtstartLocal, recur);
                if (lastMillis == -1) {
                    return lastMillis;
                }
            } else {
                lastMillis = dtstartMillis;
            }
            return duration.addTo(lastMillis);
        } catch (EventRecurrence.InvalidFormatException e) {
            if (!Log.isLoggable("CalendarProvider2", 5)) {
                return -1L;
            }
            Log.w("CalendarProvider2", "Could not parse RRULE recurrence string: " + values.get("rrule"), e);
            return -1L;
        }
    }

    private ContentValues updateLastDate(ContentValues values) {
        try {
            long last = calculateLastDate(values);
            if (last != -1) {
                values.put("lastDate", Long.valueOf(last));
                return values;
            }
            return values;
        } catch (DateException e) {
            if (Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "Could not calculate last date.", e);
            }
            return null;
        }
    }

    private void updateEventRawTimesLocked(long eventId, ContentValues values) {
        ContentValues rawValues = new ContentValues();
        rawValues.put("event_id", Long.valueOf(eventId));
        String timezone = values.getAsString("eventTimezone");
        boolean allDay = false;
        Integer allDayInteger = values.getAsInteger("allDay");
        if (allDayInteger != null) {
            allDay = allDayInteger.intValue() != 0;
        }
        if (allDay || TextUtils.isEmpty(timezone)) {
            timezone = "UTC";
        }
        Time time = new Time(timezone);
        time.allDay = allDay;
        Long dtstartMillis = values.getAsLong("dtstart");
        if (dtstartMillis != null) {
            time.set(dtstartMillis.longValue());
            rawValues.put("dtstart2445", time.format2445());
        }
        Long dtendMillis = values.getAsLong("dtend");
        if (dtendMillis != null) {
            time.set(dtendMillis.longValue());
            rawValues.put("dtend2445", time.format2445());
        }
        Long originalInstanceMillis = values.getAsLong("originalInstanceTime");
        if (originalInstanceMillis != null) {
            Integer allDayInteger2 = values.getAsInteger("originalAllDay");
            if (allDayInteger2 != null) {
                time.allDay = allDayInteger2.intValue() != 0;
            }
            time.set(originalInstanceMillis.longValue());
            rawValues.put("originalInstanceTime2445", time.format2445());
        }
        Long lastDateMillis = values.getAsLong("lastDate");
        if (lastDateMillis != null) {
            time.allDay = allDay;
            time.set(lastDateMillis.longValue());
            rawValues.put("lastDate2445", time.format2445());
        }
        this.mDbHelper.eventsRawTimesReplace(rawValues);
    }

    @Override
    protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "deleteInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(3, uri, null, callerIsSyncAdapter, match, selection, selectionArgs);
        this.mDb = this.mDbHelper.getWritableDatabase();
        switch (match) {
            case 1:
                int result = 0;
                Cursor cursor = this.mDb.query("view_events", ID_ONLY_PROJECTION, appendAccountToSelection(uri, selection, "account_name", "account_type"), selectionArgs, null, null, null);
                while (cursor.moveToNext()) {
                    try {
                        long id = cursor.getLong(0);
                        result += deleteEventInternal(id, callerIsSyncAdapter, true);
                    } finally {
                        cursor.close();
                    }
                }
                this.mCalendarAlarm.scheduleNextAlarm(false);
                sendUpdateNotification(callerIsSyncAdapter);
                return result;
            case 2:
                long id2 = ContentUris.parseId(uri);
                return deleteEventInternal(id2, callerIsSyncAdapter, false);
            case 3:
            case 15:
            case 20:
            case 28:
                throw new UnsupportedOperationException("Cannot delete that URL");
            case 4:
                break;
            case 5:
                StringBuilder selectionSb = new StringBuilder("_id=");
                selectionSb.append(uri.getPathSegments().get(1));
                if (!TextUtils.isEmpty(selection)) {
                    selectionSb.append(" AND (");
                    selectionSb.append(selection);
                    selectionSb.append(')');
                }
                selection = selectionSb.toString();
                break;
            case 6:
                if (callerIsSyncAdapter) {
                    return this.mDb.delete("Attendees", selection, selectionArgs);
                }
                return deleteFromEventRelatedTable("Attendees", uri, selection, selectionArgs);
            case 7:
                if (callerIsSyncAdapter) {
                    long id3 = ContentUris.parseId(uri);
                    return this.mDb.delete("Attendees", "_id=?", new String[]{String.valueOf(id3)});
                }
                return deleteFromEventRelatedTable("Attendees", uri, null, null);
            case 8:
                return deleteReminders(uri, false, selection, selectionArgs, callerIsSyncAdapter);
            case 9:
                return deleteReminders(uri, true, null, null, callerIsSyncAdapter);
            case 10:
                if (callerIsSyncAdapter) {
                    return this.mDb.delete("ExtendedProperties", selection, selectionArgs);
                }
                return deleteFromEventRelatedTable("ExtendedProperties", uri, selection, selectionArgs);
            case 11:
                if (callerIsSyncAdapter) {
                    long id4 = ContentUris.parseId(uri);
                    return this.mDb.delete("ExtendedProperties", "_id=?", new String[]{String.valueOf(id4)});
                }
                return deleteFromEventRelatedTable("ExtendedProperties", uri, null, null);
            case 12:
                if (callerIsSyncAdapter) {
                    return this.mDb.delete("CalendarAlerts", selection, selectionArgs);
                }
                return deleteFromEventRelatedTable("CalendarAlerts", uri, selection, selectionArgs);
            case 13:
                long id5 = ContentUris.parseId(uri);
                return this.mDb.delete("CalendarAlerts", "_id=?", new String[]{String.valueOf(id5)});
            case 14:
            case 18:
            case 19:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 29:
            case 31:
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
            case 16:
                return this.mDbHelper.getSyncState().delete(this.mDb, selection, selectionArgs);
            case 17:
                String selectionWithId = "_id=?" + (selection == null ? "" : " AND (" + selection + ")");
                return this.mDbHelper.getSyncState().delete(this.mDb, selectionWithId, insertSelectionArg(selectionArgs, String.valueOf(ContentUris.parseId(uri))));
            case 30:
                List<String> segments = uri.getPathSegments();
                Long.parseLong(segments.get(1));
                long excepId = Long.parseLong(segments.get(2));
                return deleteEventInternal(excepId, callerIsSyncAdapter, false);
            case 32:
                return deleteMatchingColors(appendAccountToSelection(uri, selection, "account_name", "account_type"), selectionArgs);
        }
        return deleteMatchingCalendars(appendAccountToSelection(uri, selection, "account_name", "account_type"), selectionArgs);
    }

    private int deleteEventInternal(long id, boolean callerIsSyncAdapter, boolean isBatch) {
        int result = 0;
        String[] selectionArgs = {String.valueOf(id)};
        Cursor cursor = this.mDb.query("Events", EVENTS_PROJECTION, "_id=?", selectionArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                result = 1;
                String syncId = cursor.getString(0);
                boolean emptySyncId = TextUtils.isEmpty(syncId);
                String rrule = cursor.getString(1);
                String rdate = cursor.getString(2);
                String origId = cursor.getString(3);
                String origSyncId = cursor.getString(4);
                if (isRecurrenceEvent(rrule, rdate, origId, origSyncId)) {
                    this.mMetaData.clearInstanceRange();
                }
                boolean isRecurrence = (TextUtils.isEmpty(rrule) && TextUtils.isEmpty(rdate)) ? false : true;
                if (callerIsSyncAdapter || emptySyncId) {
                    this.mDb.delete("Events", "_id=?", selectionArgs);
                    if (isRecurrence && emptySyncId) {
                        this.mDb.delete("Events", "original_id=?", selectionArgs);
                    }
                } else {
                    ContentValues values = new ContentValues();
                    values.put("deleted", (Integer) 1);
                    values.put("dirty", (Integer) 1);
                    addMutator(values, "mutators");
                    this.mDb.update("Events", values, "_id=?", selectionArgs);
                    this.mDb.delete("Events", "original_id=? AND _sync_id IS NULL", selectionArgs);
                    this.mDb.delete("Instances", "event_id=?", selectionArgs);
                    this.mDb.delete("EventsRawTimes", "event_id=?", selectionArgs);
                    this.mDb.delete("Reminders", "event_id=?", selectionArgs);
                    this.mDb.delete("CalendarAlerts", "event_id=?", selectionArgs);
                    this.mDb.delete("ExtendedProperties", "event_id=?", selectionArgs);
                }
            }
            if (!isBatch) {
                this.mCalendarAlarm.scheduleNextAlarm(false);
                sendUpdateNotification(callerIsSyncAdapter);
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    private int deleteFromEventRelatedTable(String table, Uri uri, String selection, String[] selectionArgs) {
        if (table.equals("Events")) {
            throw new IllegalArgumentException("Don't delete Events with this method (use deleteEventInternal)");
        }
        ContentValues dirtyValues = new ContentValues();
        dirtyValues.put("dirty", "1");
        addMutator(dirtyValues, "mutators");
        Cursor c = query(uri, ID_PROJECTION, selection, selectionArgs, "event_id");
        int count = 0;
        long prevEventId = -1;
        while (c.moveToNext()) {
            try {
                long id = c.getLong(0);
                long eventId = c.getLong(1);
                if (eventId != prevEventId) {
                    this.mDbHelper.duplicateEvent(eventId);
                    prevEventId = eventId;
                }
                this.mDb.delete(table, "_id=?", new String[]{String.valueOf(id)});
                if (eventId != prevEventId) {
                    this.mDb.update("Events", dirtyValues, "_id=?", new String[]{String.valueOf(eventId)});
                }
                count++;
            } finally {
                c.close();
            }
        }
        return count;
    }

    private int deleteReminders(Uri uri, boolean byId, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        long rowId = -1;
        if (byId) {
            if (!TextUtils.isEmpty(selection)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            rowId = ContentUris.parseId(uri);
            if (rowId < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
        }
        HashSet<Long> eventIdSet = new HashSet<>();
        Cursor c = query(uri, new String[]{"event_id"}, selection, selectionArgs, null);
        while (c.moveToNext()) {
            try {
                eventIdSet.add(Long.valueOf(c.getLong(0)));
            } finally {
                c.close();
            }
        }
        if (!callerIsSyncAdapter) {
            ContentValues dirtyValues = new ContentValues();
            dirtyValues.put("dirty", "1");
            addMutator(dirtyValues, "mutators");
            Iterator<Long> iter = eventIdSet.iterator();
            while (iter.hasNext()) {
                long eventId = iter.next().longValue();
                this.mDbHelper.duplicateEvent(eventId);
                this.mDb.update("Events", dirtyValues, "_id=?", new String[]{String.valueOf(eventId)});
            }
        }
        if (byId) {
            selection = "_id=?";
            selectionArgs = new String[]{String.valueOf(rowId)};
        }
        int delCount = this.mDb.delete("Reminders", selection, selectionArgs);
        ContentValues noAlarmValues = new ContentValues();
        noAlarmValues.put("hasAlarm", (Integer) 0);
        Iterator<Long> iter2 = eventIdSet.iterator();
        while (iter2.hasNext()) {
            long eventId2 = iter2.next().longValue();
            Cursor reminders = this.mDb.query("Reminders", new String[]{"_id"}, "event_id=?", new String[]{String.valueOf(eventId2)}, null, null, null);
            int reminderCount = reminders.getCount();
            reminders.close();
            if (reminderCount == 0) {
                this.mDb.update("Events", noAlarmValues, "_id=?", new String[]{String.valueOf(eventId2)});
            }
        }
        return delCount;
    }

    private int updateEventRelatedTable(Uri uri, String table, boolean byId, ContentValues updateValues, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        if (byId) {
            if (!TextUtils.isEmpty(selection)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            long rowId = ContentUris.parseId(uri);
            if (rowId < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
            selection = "_id=?";
            selectionArgs = new String[]{String.valueOf(rowId)};
        } else if (TextUtils.isEmpty(selection)) {
            throw new UnsupportedOperationException("Selection is required for " + uri);
        }
        Cursor c = this.mDb.query(table, null, selection, selectionArgs, null, null, null);
        int count = 0;
        try {
            if (c.getCount() == 0) {
                Log.d("CalendarProvider2", "No query results for " + uri + ", selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs));
                return 0;
            }
            ContentValues dirtyValues = null;
            if (!callerIsSyncAdapter) {
                dirtyValues = new ContentValues();
                dirtyValues.put("dirty", "1");
                addMutator(dirtyValues, "mutators");
            }
            int idIndex = c.getColumnIndex("_id");
            int eventIdIndex = c.getColumnIndex("event_id");
            if (idIndex < 0 || eventIdIndex < 0) {
                throw new RuntimeException("Lookup on _id/event_id failed for " + uri);
            }
            while (c.moveToNext()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                values.putAll(updateValues);
                long id = c.getLong(idIndex);
                long eventId = c.getLong(eventIdIndex);
                if (!callerIsSyncAdapter) {
                    this.mDbHelper.duplicateEvent(eventId);
                }
                this.mDb.update(table, values, "_id=?", new String[]{String.valueOf(id)});
                if (!callerIsSyncAdapter) {
                    this.mDb.update("Events", dirtyValues, "_id=?", new String[]{String.valueOf(eventId)});
                }
                count++;
                if (table.equals("Attendees")) {
                    updateEventAttendeeStatus(this.mDb, values);
                    sendUpdateNotification(eventId, callerIsSyncAdapter);
                }
            }
            c.close();
            return count;
        } finally {
            c.close();
        }
    }

    private int deleteMatchingColors(String selection, String[] selectionArgs) {
        Cursor c = this.mDb.query("Colors", COLORS_PROJECTION, selection, selectionArgs, null, null, null);
        if (c == null) {
            return 0;
        }
        Cursor c2 = null;
        while (c.moveToNext()) {
            try {
                String index = c.getString(3);
                String accountName = c.getString(0);
                String accountType = c.getString(1);
                boolean isCalendarColor = c.getInt(2) == 0;
                if (isCalendarColor) {
                    try {
                        c2 = this.mDb.query("Calendars", ID_ONLY_PROJECTION, "account_name=? AND account_type=? AND calendar_color_index=?", new String[]{accountName, accountType, index}, null, null, null);
                        if (c2.getCount() != 0) {
                            throw new UnsupportedOperationException("Cannot delete color " + index + ". Referenced by " + c2.getCount() + " calendars.");
                        }
                    } catch (Throwable th) {
                        if (c2 != null) {
                            c2.close();
                        }
                        throw th;
                    }
                } else {
                    c2 = query(CalendarContract.Events.CONTENT_URI, ID_ONLY_PROJECTION, "calendar_id in (SELECT _id from Calendars WHERE account_name=? AND account_type=?) AND eventColor_index=?", new String[]{accountName, accountType, index}, null);
                    if (c2.getCount() != 0) {
                        throw new UnsupportedOperationException("Cannot delete color " + index + ". Referenced by " + c2.getCount() + " events.");
                    }
                }
                if (c2 != null) {
                    c2.close();
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return this.mDb.delete("Colors", selection, selectionArgs);
    }

    private int deleteMatchingCalendars(String selection, String[] selectionArgs) {
        Cursor c = this.mDb.query("Calendars", sCalendarsIdProjection, selection, selectionArgs, null, null, null);
        if (c == null) {
            return 0;
        }
        while (c.moveToNext()) {
            try {
                long id = c.getLong(0);
                modifyCalendarSubscription(id, false);
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return this.mDb.delete("Calendars", selection, selectionArgs);
    }

    private boolean doesEventExistForSyncId(String syncId) {
        if (syncId == null) {
            if (!Log.isLoggable("CalendarProvider2", 5)) {
                return false;
            }
            Log.w("CalendarProvider2", "SyncID cannot be null: " + syncId);
            return false;
        }
        long count = DatabaseUtils.longForQuery(this.mDb, "SELECT COUNT(*) FROM Events WHERE _sync_id=?", new String[]{syncId});
        return count > 0;
    }

    private boolean doesStatusCancelUpdateMeanUpdate(ContentValues values, ContentValues modValues) {
        boolean isStatusCanceled = modValues.containsKey("eventStatus") && modValues.getAsInteger("eventStatus").intValue() == 2;
        if (!isStatusCanceled) {
            return true;
        }
        String originalSyncId = values.getAsString("original_sync_id");
        if (TextUtils.isEmpty(originalSyncId)) {
            return true;
        }
        return doesEventExistForSyncId(originalSyncId);
    }

    private int handleUpdateColors(ContentValues values, String selection, String[] selectionArgs) {
        Cursor c = null;
        int result = this.mDb.update("Colors", values, selection, selectionArgs);
        if (values.containsKey("color")) {
            try {
                c = this.mDb.query("Colors", COLORS_PROJECTION, selection, selectionArgs, null, null, null);
                while (c.moveToNext()) {
                    boolean calendarColor = c.getInt(2) == 0;
                    int color = c.getInt(4);
                    String[] args = {c.getString(0), c.getString(1), c.getString(3)};
                    ContentValues colorValue = new ContentValues();
                    if (calendarColor) {
                        colorValue.put("calendar_color", Integer.valueOf(color));
                        this.mDb.update("Calendars", colorValue, "account_name=? AND account_type=? AND calendar_color_index=?", args);
                    } else {
                        colorValue.put("eventColor", Integer.valueOf(color));
                        this.mDb.update("Events", colorValue, "calendar_id in (SELECT _id from Calendars WHERE account_name=? AND account_type=?) AND eventColor_index=?", args);
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return result;
    }

    private int handleUpdateEvents(Cursor cursor, ContentValues updateValues, boolean callerIsSyncAdapter) {
        updateValues.remove("hasAlarm");
        if (cursor.getCount() > 1 && Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "Performing update on " + cursor.getCount() + " events");
        }
        while (cursor.moveToNext()) {
            ContentValues modValues = new ContentValues(updateValues);
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, values);
            boolean doValidate = false;
            if (!callerIsSyncAdapter) {
                try {
                    validateEventData(values);
                    doValidate = true;
                } catch (IllegalArgumentException iae) {
                    Log.d("CalendarProvider2", "Event " + values.getAsString("_id") + " malformed, not validating update (" + iae.getMessage() + ")");
                }
            }
            values.putAll(modValues);
            String color_id = modValues.getAsString("eventColor_index");
            if (!TextUtils.isEmpty(color_id)) {
                String accountName = null;
                String accountType = null;
                Cursor c = this.mDb.query("Calendars", ACCOUNT_PROJECTION, "_id=?", new String[]{values.getAsString("calendar_id")}, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        accountName = c.getString(0);
                        accountType = c.getString(1);
                    }
                    verifyColorExists(accountName, accountType, color_id, 1);
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            if (callerIsSyncAdapter) {
                scrubEventData(values, modValues);
            }
            if (doValidate) {
                validateEventData(values);
            }
            if (modValues.containsKey("dtstart") || modValues.containsKey("dtend") || modValues.containsKey("duration") || modValues.containsKey("eventTimezone") || modValues.containsKey("rrule") || modValues.containsKey("rdate") || modValues.containsKey("exrule") || modValues.containsKey("exdate")) {
                try {
                    long newLastDate = calculateLastDate(values);
                    Long oldLastDateObj = values.getAsLong("lastDate");
                    long oldLastDate = oldLastDateObj == null ? -1L : oldLastDateObj.longValue();
                    if (oldLastDate != newLastDate) {
                        if (newLastDate < 0) {
                            modValues.putNull("lastDate");
                        } else {
                            modValues.put("lastDate", Long.valueOf(newLastDate));
                        }
                    }
                } catch (DateException de) {
                    throw new IllegalArgumentException("Unable to compute LAST_DATE", de);
                }
            }
            if (!callerIsSyncAdapter) {
                modValues.put("dirty", (Integer) 1);
                addMutator(modValues, "mutators");
            }
            if (modValues.containsKey("selfAttendeeStatus")) {
                throw new IllegalArgumentException("Updating selfAttendeeStatus in Events table is not allowed.");
            }
            if (fixAllDayTime(values, modValues) && Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "handleUpdateEvents: allDay is true but sec, min, hour were not 0.");
            }
            boolean isUpdate = doesStatusCancelUpdateMeanUpdate(values, modValues);
            long id = values.getAsLong("_id").longValue();
            if (isUpdate) {
                if (!callerIsSyncAdapter) {
                    this.mDbHelper.duplicateEvent(id);
                } else if (modValues.containsKey("dirty") && modValues.getAsInteger("dirty").intValue() == 0) {
                    modValues.put("mutators", (String) null);
                    this.mDbHelper.removeDuplicateEvent(id);
                }
                int result = this.mDb.update("Events", modValues, "_id=?", new String[]{String.valueOf(id)});
                if (result > 0) {
                    updateEventRawTimesLocked(id, modValues);
                    this.mInstancesHelper.updateInstancesLocked(modValues, id, false, this.mDb);
                    if (modValues.containsKey("dtstart") || modValues.containsKey("eventStatus")) {
                        if (modValues.containsKey("eventStatus") && modValues.getAsInteger("eventStatus").intValue() == 2) {
                            String[] args = {String.valueOf(id)};
                            this.mDb.delete("Instances", "event_id=?", args);
                        }
                        if (Log.isLoggable("CalendarProvider2", 3)) {
                            Log.d("CalendarProvider2", "updateInternal() changing event");
                        }
                        this.mCalendarAlarm.scheduleNextAlarm(false);
                    }
                    sendUpdateNotification(id, callerIsSyncAdapter);
                }
            } else {
                deleteEventInternal(id, callerIsSyncAdapter, true);
                this.mCalendarAlarm.scheduleNextAlarm(false);
                sendUpdateNotification(callerIsSyncAdapter);
            }
        }
        return cursor.getCount();
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) throws Throwable {
        Cursor events;
        long id;
        Account account;
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "updateInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(2, uri, values, callerIsSyncAdapter, match, selection, selectionArgs);
        this.mDb = this.mDbHelper.getWritableDatabase();
        switch (match) {
            case 1:
            case 2:
                Cursor events2 = null;
                try {
                    if (match == 2) {
                        long id2 = ContentUris.parseId(uri);
                        events = this.mDb.query("Events", null, "_id=?", new String[]{String.valueOf(id2)}, null, null, null);
                    } else {
                        events = this.mDb.query("Events", null, selection, selectionArgs, null, null, null);
                    }
                    if (events2.getCount() == 0) {
                        Log.i("CalendarProvider2", "No events to update: uri=" + uri + " selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs));
                    }
                    int iHandleUpdateEvents = handleUpdateEvents(events2, values, callerIsSyncAdapter);
                    if (events2 != null) {
                        events2.close();
                        return iHandleUpdateEvents;
                    }
                    return iHandleUpdateEvents;
                } finally {
                    if (events2 != null) {
                        events2.close();
                    }
                }
            case 3:
            case 10:
            case 14:
            case 15:
            case 18:
            case 19:
            case 20:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 29:
            case 30:
            case 31:
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
            case 4:
            case 5:
                if (match == 5) {
                    id = ContentUris.parseId(uri);
                } else if (selection != null && TextUtils.equals(selection, "_id=?")) {
                    id = Long.parseLong(selectionArgs[0]);
                } else if (selection != null && selection.startsWith("_id=")) {
                    id = Long.parseLong(selection.substring(4));
                } else {
                    return this.mDb.update("Calendars", values, selection, selectionArgs);
                }
                if (!callerIsSyncAdapter) {
                    values.put("dirty", (Integer) 1);
                    addMutator(values, "mutators");
                } else if (values.containsKey("dirty") && values.getAsInteger("dirty").intValue() == 0) {
                    values.put("mutators", (String) null);
                }
                Integer syncEvents = values.getAsInteger("sync_events");
                if (syncEvents != null) {
                    modifyCalendarSubscription(id, syncEvents.intValue() == 1);
                }
                String color_id = values.getAsString("calendar_color_index");
                if (!TextUtils.isEmpty(color_id)) {
                    String accountName = values.getAsString("account_name");
                    String accountType = values.getAsString("account_type");
                    if ((TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) && (account = getAccount(id)) != null) {
                        accountName = account.name;
                        accountType = account.type;
                    }
                    verifyColorExists(accountName, accountType, color_id, 0);
                }
                int result = this.mDb.update("Calendars", values, "_id=?", new String[]{String.valueOf(id)});
                if (result > 0) {
                    if (values.containsKey("visible")) {
                        this.mCalendarAlarm.scheduleNextAlarm(false);
                    }
                    sendUpdateNotification(callerIsSyncAdapter);
                    return result;
                }
                return result;
            case 6:
                return updateEventRelatedTable(uri, "Attendees", false, values, selection, selectionArgs, callerIsSyncAdapter);
            case 7:
                return updateEventRelatedTable(uri, "Attendees", true, values, null, null, callerIsSyncAdapter);
            case 8:
                return updateEventRelatedTable(uri, "Reminders", false, values, selection, selectionArgs, callerIsSyncAdapter);
            case 9:
                int count = updateEventRelatedTable(uri, "Reminders", true, values, null, null, callerIsSyncAdapter);
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "updateInternal() changing reminder");
                }
                this.mCalendarAlarm.scheduleNextAlarm(false);
                return count;
            case 11:
                return updateEventRelatedTable(uri, "ExtendedProperties", true, values, null, null, callerIsSyncAdapter);
            case 12:
                return this.mDb.update("CalendarAlerts", values, selection, selectionArgs);
            case 13:
                long id3 = ContentUris.parseId(uri);
                return this.mDb.update("CalendarAlerts", values, "_id=?", new String[]{String.valueOf(id3)});
            case 16:
                return this.mDbHelper.getSyncState().update(this.mDb, values, appendAccountToSelection(uri, selection, "account_name", "account_type"), selectionArgs);
            case 17:
                String selection2 = appendAccountToSelection(uri, selection, "account_name", "account_type");
                String selectionWithId = "_id=?" + (selection2 == null ? "" : " AND (" + selection2 + ")");
                return this.mDbHelper.getSyncState().update(this.mDb, values, selectionWithId, insertSelectionArg(selectionArgs, String.valueOf(ContentUris.parseId(uri))));
            case 21:
                this.mCalendarAlarm.scheduleNextAlarm(false);
                return 0;
            case 22:
                this.mCalendarAlarm.scheduleNextAlarm(true);
                return 0;
            case 28:
                if (!selection.equals("key=?")) {
                    throw new UnsupportedOperationException("Selection should be key=? for " + uri);
                }
                List<String> list = Arrays.asList(selectionArgs);
                if (list.contains("timezoneInstancesPrevious")) {
                    throw new UnsupportedOperationException("Invalid selection key: timezoneInstancesPrevious for " + uri);
                }
                String timezoneInstancesBeforeUpdate = this.mCalendarCache.readTimezoneInstances();
                int result2 = this.mDb.update("CalendarCache", values, selection, selectionArgs);
                if (result2 > 0) {
                    if (list.contains("timezoneType")) {
                        String value = values.getAsString("value");
                        if (value != null) {
                            if (value.equals("home")) {
                                String previousTimezone = this.mCalendarCache.readTimezoneInstancesPrevious();
                                if (previousTimezone != null) {
                                    this.mCalendarCache.writeTimezoneInstances(previousTimezone);
                                }
                                if (!timezoneInstancesBeforeUpdate.equals(previousTimezone)) {
                                    regenerateInstancesTable();
                                    sendUpdateNotification(callerIsSyncAdapter);
                                    return result2;
                                }
                                return result2;
                            }
                            if (value.equals("auto")) {
                                String localTimezone = TimeZone.getDefault().getID();
                                this.mCalendarCache.writeTimezoneInstances(localTimezone);
                                if (!timezoneInstancesBeforeUpdate.equals(localTimezone)) {
                                    regenerateInstancesTable();
                                    sendUpdateNotification(callerIsSyncAdapter);
                                    return result2;
                                }
                                return result2;
                            }
                            return result2;
                        }
                        return result2;
                    }
                    if (list.contains("timezoneInstances") && isHomeTimezone()) {
                        String timezoneInstances = this.mCalendarCache.readTimezoneInstances();
                        this.mCalendarCache.writeTimezoneInstancesPrevious(timezoneInstances);
                        if (timezoneInstancesBeforeUpdate != null && !timezoneInstancesBeforeUpdate.equals(timezoneInstances)) {
                            regenerateInstancesTable();
                            sendUpdateNotification(callerIsSyncAdapter);
                            return result2;
                        }
                        return result2;
                    }
                    return result2;
                }
                return result2;
            case 32:
                int validValues = 0;
                if (values.getAsInteger("color") != null) {
                    validValues = 0 + 1;
                }
                if (values.getAsString("data") != null) {
                    validValues++;
                }
                if (values.size() != validValues) {
                    throw new UnsupportedOperationException("You may only change the COLOR and DATA columns for an existing Colors entry.");
                }
                return handleUpdateColors(values, appendAccountToSelection(uri, selection, "account_name", "account_type"), selectionArgs);
        }
    }

    private int verifyColorExists(String accountName, String accountType, String colorIndex, int colorType) {
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            throw new IllegalArgumentException("Cannot set color. A valid account does not exist for this calendar.");
        }
        Cursor c = null;
        try {
            c = getColorByTypeIndex(accountName, accountType, colorType, colorIndex);
            if (!c.moveToFirst()) {
                throw new IllegalArgumentException("Color type: " + colorType + " and index " + colorIndex + " does not exist for account.");
            }
            int color = c.getInt(4);
            return color;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String appendLastSyncedColumnToSelection(String selection, Uri uri) {
        if (!getIsCallerSyncAdapter(uri)) {
            StringBuilder sb = new StringBuilder();
            sb.append("lastSynced").append(" = 0");
            return appendSelection(sb, selection);
        }
        return selection;
    }

    private String appendAccountToSelection(Uri uri, String selection, String accountNameColumn, String accountTypeColumn) {
        String accountName = QueryParameterUtils.getQueryParameter(uri, "account_name");
        String accountType = QueryParameterUtils.getQueryParameter(uri, "account_type");
        if (!TextUtils.isEmpty(accountName)) {
            StringBuilder sb = new StringBuilder().append(accountNameColumn).append("=").append(DatabaseUtils.sqlEscapeString(accountName)).append(" AND ").append(accountTypeColumn).append("=").append(DatabaseUtils.sqlEscapeString(accountType));
            return appendSelection(sb, selection);
        }
        return selection;
    }

    private String appendSelection(StringBuilder sb, String selection) {
        if (!TextUtils.isEmpty(selection)) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }

    private void verifyTransactionAllowed(int type, Uri uri, ContentValues values, boolean isSyncAdapter, int uriMatch, String selection, String[] selectionArgs) {
        if (type != 0) {
            if (type == 2 || type == 3) {
                if (!TextUtils.isEmpty(selection)) {
                    switch (uriMatch) {
                        case 1:
                        case 4:
                        case 6:
                        case 8:
                        case 10:
                        case 12:
                        case 16:
                        case 28:
                        case 32:
                            break;
                        default:
                            throw new IllegalArgumentException("Selection not permitted for " + uri);
                    }
                } else {
                    switch (uriMatch) {
                        case 1:
                        case 6:
                        case 8:
                        case 28:
                            throw new IllegalArgumentException("Selection must be specified for " + uri);
                    }
                }
            }
            if (!isSyncAdapter) {
                switch (uriMatch) {
                    case 10:
                    case 11:
                    case 16:
                    case 17:
                    case 32:
                        throw new IllegalArgumentException("Only sync adapters may write using " + uri);
                }
            }
            switch (type) {
                case 1:
                    if (uriMatch == 3) {
                        throw new UnsupportedOperationException("Inserting into instances not supported");
                    }
                    verifyColumns(values, uriMatch);
                    if (isSyncAdapter) {
                        verifyHasAccount(uri, selection, selectionArgs);
                        return;
                    } else {
                        verifyNoSyncColumns(values, uriMatch);
                        return;
                    }
                case 2:
                    if (uriMatch == 3) {
                        throw new UnsupportedOperationException("Updating instances not supported");
                    }
                    verifyColumns(values, uriMatch);
                    if (isSyncAdapter) {
                        verifyHasAccount(uri, selection, selectionArgs);
                        return;
                    } else {
                        verifyNoSyncColumns(values, uriMatch);
                        return;
                    }
                case 3:
                    if (uriMatch == 3) {
                        throw new UnsupportedOperationException("Deleting instances not supported");
                    }
                    if (isSyncAdapter) {
                        verifyHasAccount(uri, selection, selectionArgs);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private void verifyHasAccount(Uri uri, String selection, String[] selectionArgs) {
        String accountName = QueryParameterUtils.getQueryParameter(uri, "account_name");
        String accountType = QueryParameterUtils.getQueryParameter(uri, "account_type");
        if ((TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) && selection != null && selection.startsWith("account_name=? AND account_type=?")) {
            accountName = selectionArgs[0];
            accountType = selectionArgs[1];
        }
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            throw new IllegalArgumentException("Sync adapters must specify an account and account type: " + uri);
        }
    }

    private void verifyColumns(ContentValues values, int uriMatch) {
        String[] columns;
        if (values != null && values.size() != 0) {
            switch (uriMatch) {
                case 1:
                case 2:
                case 18:
                case 19:
                    columns = CalendarContract.Events.PROVIDER_WRITABLE_COLUMNS;
                    break;
                default:
                    columns = PROVIDER_WRITABLE_DEFAULT_COLUMNS;
                    break;
            }
            for (int i = 0; i < columns.length; i++) {
                if (values.containsKey(columns[i])) {
                    throw new IllegalArgumentException("Only the provider may write to " + columns[i]);
                }
            }
        }
    }

    private void verifyNoSyncColumns(ContentValues values, int uriMatch) {
        String[] syncColumns;
        if (values != null && values.size() != 0) {
            switch (uriMatch) {
                case 1:
                case 2:
                case 18:
                case 19:
                    syncColumns = CalendarContract.Events.SYNC_WRITABLE_COLUMNS;
                    break;
                case 4:
                case 5:
                case 24:
                case 25:
                    syncColumns = CalendarContract.Calendars.SYNC_WRITABLE_COLUMNS;
                    break;
                default:
                    syncColumns = SYNC_WRITABLE_DEFAULT_COLUMNS;
                    break;
            }
            for (int i = 0; i < syncColumns.length; i++) {
                if (values.containsKey(syncColumns[i])) {
                    throw new IllegalArgumentException("Only sync adapters may write to " + syncColumns[i]);
                }
            }
        }
    }

    private void modifyCalendarSubscription(long id, boolean syncEvents) throws Throwable {
        Cursor cursor = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, id), new String[]{"account_name", "account_type", "cal_sync1", "sync_events"}, null, null, null);
        Account account = null;
        String calendarUrl = null;
        boolean oldSyncEvents = false;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String accountName = cursor.getString(0);
                    String accountType = cursor.getString(1);
                    Account account2 = new Account(accountName, accountType);
                    try {
                        calendarUrl = cursor.getString(2);
                        oldSyncEvents = cursor.getInt(3) != 0;
                        account = account2;
                    } catch (Throwable th) {
                        th = th;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (account == null) {
            if (Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "Cannot update subscription because account is empty -- should not happen.");
            }
        } else {
            if (TextUtils.isEmpty(calendarUrl)) {
                calendarUrl = null;
            }
            if (oldSyncEvents != syncEvents) {
                this.mDbHelper.scheduleSync(account, !syncEvents, calendarUrl);
            }
        }
    }

    private void sendUpdateNotification(boolean callerIsSyncAdapter) {
        sendUpdateNotification(-1L, callerIsSyncAdapter);
    }

    private void sendUpdateNotification(long eventId, boolean callerIsSyncAdapter) {
        if (this.mBroadcastHandler.hasMessages(1)) {
            this.mBroadcastHandler.removeMessages(1);
        } else {
            this.mContext.startService(new Intent(this.mContext, (Class<?>) EmptyService.class));
        }
        long delay = callerIsSyncAdapter ? 30000L : 1000L;
        Message msg = this.mBroadcastHandler.obtainMessage(1);
        this.mBroadcastHandler.sendMessageDelayed(msg, delay);
    }

    private void doSendUpdateNotification() {
        Intent intent = new Intent("android.intent.action.PROVIDER_CHANGED", CalendarContract.CONTENT_URI);
        if (Log.isLoggable("CalendarProvider2", 4)) {
            Log.i("CalendarProvider2", "Sending notification intent: " + intent);
        }
        this.mContext.sendBroadcast(intent, null);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        Thread thread = new AccountsUpdatedThread(accounts);
        thread.start();
    }

    private class AccountsUpdatedThread extends Thread {
        private Account[] mAccounts;

        AccountsUpdatedThread(Account[] accounts) {
            this.mAccounts = accounts;
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            CalendarProvider2.this.removeStaleAccounts(this.mAccounts);
        }
    }

    private void removeStaleAccounts(Account[] accounts) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        if (this.mDb != null) {
            HashSet<Account> validAccounts = new HashSet<>();
            for (Account account : accounts) {
                validAccounts.add(new Account(account.name, account.type));
            }
            ArrayList<Account> accountsToDelete = new ArrayList<>();
            this.mDb.beginTransaction();
            Cursor c = null;
            try {
                String[] arr$ = {"Calendars", "Colors"};
                for (String table : arr$) {
                    Cursor c2 = this.mDb.rawQuery("SELECT DISTINCT account_name,account_type FROM " + table, null);
                    while (c2.moveToNext()) {
                        if (c2.getString(0) != null && c2.getString(1) != null && !TextUtils.equals(c2.getString(1), "LOCAL")) {
                            Account currAccount = new Account(c2.getString(0), c2.getString(1));
                            if (!validAccounts.contains(currAccount)) {
                                accountsToDelete.add(currAccount);
                            }
                        }
                    }
                    c2.close();
                    c = null;
                }
                for (Account account2 : accountsToDelete) {
                    if (Log.isLoggable("CalendarProvider2", 3)) {
                        Log.d("CalendarProvider2", "removing data for removed account " + account2);
                    }
                    String[] params = {account2.name, account2.type};
                    this.mDb.execSQL("DELETE FROM Calendars WHERE account_name=? AND account_type=?", params);
                    this.mDb.execSQL("DELETE FROM Colors WHERE account_name=? AND account_type=?", params);
                }
                this.mDbHelper.getSyncState().onAccountsChanged(this.mDb, accounts);
                this.mDb.setTransactionSuccessful();
                if (c != null) {
                    c.close();
                }
                this.mDb.endTransaction();
                sendUpdateNotification(false);
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                this.mDb.endTransaction();
                throw th;
            }
        }
    }

    private String[] insertSelectionArg(String[] selectionArgs, String arg) {
        if (selectionArgs == null) {
            return new String[]{arg};
        }
        int newLength = selectionArgs.length + 1;
        String[] newSelectionArgs = new String[newLength];
        newSelectionArgs[0] = arg;
        System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
        return newSelectionArgs;
    }

    private String getCallingPackageName() {
        if (getCachedCallingPackage() != null) {
            return getCachedCallingPackage();
        }
        PackageManager pm = getContext().getPackageManager();
        int uid = Binder.getCallingUid();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && packages.length == 1) {
            return packages[0];
        }
        String name = pm.getNameForUid(uid);
        return name == null ? String.valueOf(uid) : name;
    }

    private void addMutator(ContentValues values, String columnName) {
        String packageName = getCallingPackageName();
        String mutators = values.getAsString(columnName);
        if (TextUtils.isEmpty(mutators)) {
            values.put(columnName, packageName);
        } else {
            values.put(columnName, mutators + "," + packageName);
        }
    }
}
