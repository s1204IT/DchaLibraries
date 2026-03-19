package com.android.providers.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.PendingIntent;
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
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.SystemClock;
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
    private static final String[] SUGGESTIONS_COLUMNS;
    private static final String[] SYNC_WRITABLE_DEFAULT_COLUMNS;
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
    static final boolean DEBUG_INSTANCES = Log.isLoggable("CalendarProvider2", 3);
    private static final String[] ID_ONLY_PROJECTION = {"_id"};
    private static final String[] EVENTS_PROJECTION = {"_sync_id", "rrule", "rdate", "original_id", "original_sync_id"};
    private static final String[] COLORS_PROJECTION = {"account_name", "account_type", "color_type", "color_index", "color"};
    private static final String[] ACCOUNT_PROJECTION = {"account_name", "account_type"};
    private static final String[] ID_PROJECTION = {"_id", "event_id"};
    private static final String[] ALLDAY_TIME_PROJECTION = {"_id", "dtstart", "dtend", "duration"};
    private static final String[] sCalendarsIdProjection = {"_id"};
    private static final Pattern SEARCH_TOKEN_PATTERN = Pattern.compile("[^\\s\"'.?!,]+|\"([^\"]*)\"");
    private static final Pattern SEARCH_ESCAPE_PATTERN = Pattern.compile("([%_#])");
    private static final String[] SEARCH_PROJECTION = {"_id", "title", "description", "rrule", "eventLocation"};
    private static final String[] SEARCH_COLUMNS = {"title", "description", "eventLocation"};
    private static final HashSet<String> ALLOWED_URI_PARAMETERS = Sets.newHashSet(new String[]{"caller_is_syncadapter", "account_name", "account_type", "limit", "suggest_intent_extra_data"});
    private static final HashSet<String> ALLOWED_IN_EXCEPTION = new HashSet<>();
    private final ThreadLocal<Boolean> mCallingPackageErrorLogged = new ThreadLocal<>();
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
        sUriMatcher.addURI("com.android.calendar", "schedule_alarms_remove", 22);
        sUriMatcher.addURI("com.android.calendar", "time/#", 23);
        sUriMatcher.addURI("com.android.calendar", "time", 23);
        sUriMatcher.addURI("com.android.calendar", "properties", 28);
        sUriMatcher.addURI("com.android.calendar", "exception/#", 29);
        sUriMatcher.addURI("com.android.calendar", "exception/#/#", 30);
        sUriMatcher.addURI("com.android.calendar", "emma", 31);
        sUriMatcher.addURI("com.android.calendar", "colors", 32);
        sUriMatcher.addURI("com.android.calendar", "search_suggest_query", 50);
        sUriMatcher.addURI("com.android.calendar", "search_suggest_shortcut/#", 51);
        sCountProjectionMap = new HashMap<>();
        sCountProjectionMap.put("_count", "COUNT(*) AS _count");
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
        sCalendarsProjectionMap.put("isPrimary", "COALESCE(isPrimary, ownerAccount = account_name) AS isPrimary");
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
        sEventsProjectionMap.put("isLunar", "isLunar");
        sEventsProjectionMap.put("lunarRrule", "lunarRrule");
        sEventEntitiesProjectionMap.put("isLunar", "isLunar");
        sEventEntitiesProjectionMap.put("lunarRrule", "lunarRrule");
        sEventsProjectionMap.put("createTime", "createTime");
        sEventsProjectionMap.put("modifyTime", "modifyTime");
        sEventEntitiesProjectionMap.put("createTime", "createTime");
        sEventEntitiesProjectionMap.put("modifyTime", "modifyTime");
        SUGGESTIONS_COLUMNS = new String[]{"_id", "suggest_format", "suggest_text_1", "suggest_text_2", "suggest_icon_1", "suggest_intent_query", "suggest_shortcut_id"};
    }

    @Override
    protected CalendarDatabaseHelper getDatabaseHelper(Context context) {
        return CalendarDatabaseHelper.getInstance(context);
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
                return false;
            }
            return false;
        }
    }

    private boolean initialize() {
        this.mContext = getContext();
        this.mContentResolver = this.mContext.getContentResolver();
        this.mDbHelper = (CalendarDatabaseHelper) getDatabaseHelper();
        this.mDb = this.mDbHelper.getWritableDatabase();
        this.mMetaData = new MetaData(this.mDbHelper);
        this.mInstancesHelper = new CalendarInstancesHelper(this.mDbHelper, this.mMetaData);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.EVENT_REMINDER");
        intentFilter2.addAction("com.android.providers.calendar.intent.CalendarProvider2");
        this.mContext.registerReceiver(new CalendarProviderBroadcastReceiver(), intentFilter2);
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
        new PostInitializeThread().start();
    }

    private class PostInitializeThread extends Thread {
        private PostInitializeThread() {
        }

        @Override
        public void run() throws Throwable {
            Process.setThreadPriority(10);
            CalendarProvider2.this.verifyAccounts();
            try {
                CalendarProvider2.this.doUpdateTimezoneDependentFields();
            } catch (IllegalStateException e) {
            }
        }
    }

    private void verifyAccounts() throws Throwable {
        AccountManager.get(getContext()).addOnAccountsUpdatedListener(this, null, false);
        removeStaleAccounts(AccountManager.get(getContext()).getAccounts());
    }

    protected void updateTimezoneDependentFields() {
        new TimezoneCheckerThread().start();
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
        return TextUtils.equals(this.mCalendarCache.readTimezoneInstances(), TimeZone.getDefault().getID());
    }

    protected void doUpdateTimezoneDependentFields() {
        try {
            String timezoneType = this.mCalendarCache.readTimezoneType();
            if (timezoneType != null && timezoneType.equals("home")) {
                return;
            }
            if (!isSameTimezoneDatabaseVersion()) {
                doProcessEventRawTimes(TimeZone.getDefault().getID(), TimeUtils.getTimeZoneDatabaseVersion());
            }
            if (isLocalSameAsInstancesTimezone()) {
                this.mCalendarAlarm.rescheduleMissedAlarms();
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

    protected void doProcessEventRawTimes(String str, String str2) {
        this.mDb.beginTransaction();
        try {
            updateEventsStartEndFromEventRawTimesLocked();
            updateTimezoneDatabaseVersion(str2);
            this.mCalendarCache.writeTimezoneInstances(str);
            regenerateInstancesTable();
            this.mDb.setTransactionSuccessful();
        } finally {
            this.mDb.endTransaction();
        }
    }

    private void updateEventsStartEndFromEventRawTimesLocked() {
        Cursor cursorRawQuery = this.mDb.rawQuery("SELECT event_id, dtstart2445, dtend2445, eventTimezone FROM EventsRawTimes, Events WHERE event_id = Events._id", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                long j = cursorRawQuery.getLong(0);
                String string = cursorRawQuery.getString(1);
                String string2 = cursorRawQuery.getString(2);
                String string3 = cursorRawQuery.getString(3);
                if (string == null && string2 == null) {
                    if (Log.isLoggable("CalendarProvider2", 6)) {
                        Log.e("CalendarProvider2", "Event " + j + " has dtStart2445 and dtEnd2445 null at the same time in EventsRawTimes!");
                    }
                } else {
                    updateEventsStartEndLocked(j, string3, string, string2);
                }
            } finally {
                cursorRawQuery.close();
            }
        }
    }

    private long get2445ToMillis(String str, String str2) {
        if (str2 == null) {
            if (Log.isLoggable("CalendarProvider2", 2)) {
                Log.v("CalendarProvider2", "Cannot parse null RFC2445 date");
            }
            return 0L;
        }
        Time time = str != null ? new Time(str) : new Time();
        try {
            time.parse(str2);
            return time.toMillis(true);
        } catch (TimeFormatException e) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "Cannot parse RFC2445 date " + str2);
            }
            return 0L;
        }
    }

    private void updateEventsStartEndLocked(long j, String str, String str2, String str3) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("dtstart", Long.valueOf(get2445ToMillis(str, str2)));
        contentValues.put("dtend", Long.valueOf(get2445ToMillis(str, str3)));
        if (this.mDb.update("Events", contentValues, "_id=?", new String[]{String.valueOf(j)}) == 0 && Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "Could not update Events table with values " + contentValues);
        }
    }

    private void updateTimezoneDatabaseVersion(String str) {
        try {
            this.mCalendarCache.writeTimezoneDatabaseVersion(str);
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
        }
        return timezoneDatabaseVersion;
    }

    private boolean isHomeTimezone() {
        return "home".equals(this.mCalendarCache.readTimezoneType());
    }

    private void regenerateInstancesTable() {
        Log.v("CalendarProvider2", "re generate instance table");
        long jCurrentTimeMillis = System.currentTimeMillis();
        String timezoneInstances = this.mCalendarCache.readTimezoneInstances();
        Time time = new Time(timezoneInstances);
        time.set(jCurrentTimeMillis);
        time.monthDay = 1;
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        long jNormalize = time.normalize(true);
        Cursor cursorHandleInstanceQuery = handleInstanceQuery(new SQLiteQueryBuilder(), jNormalize, jNormalize + 5356800000L, new String[]{"_id"}, null, null, null, false, true, timezoneInstances, isHomeTimezone());
        if (cursorHandleInstanceQuery != null) {
            cursorHandleInstanceQuery.close();
        }
        this.mCalendarAlarm.rescheduleMissedAlarms();
    }

    @Override
    protected void notifyChange(boolean z) {
        this.mContentResolver.notifyChange(CalendarContract.CONTENT_URI, (ContentObserver) null, z);
    }

    @Override
    protected boolean shouldSyncFor(Uri uri) {
        int iMatch = sUriMatcher.match(uri);
        return (iMatch == 12 || iMatch == 13 || iMatch == 14) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        long jClearCallingIdentityInternal = clearCallingIdentityInternal();
        try {
            return queryInternal(uri, strArr, str, strArr2, str2);
        } finally {
            restoreCallingIdentityInternal(jClearCallingIdentityInternal);
        }
    }

    private Cursor queryInternal(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String strAppendAccountToSelection;
        String str3;
        String[] strArr3;
        String[] strArrInsertSelectionArg;
        String str4;
        String str5 = str;
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "query uri - " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch != 32) {
            switch (iMatch) {
                case 1:
                    sQLiteQueryBuilder.setTables("view_events");
                    sQLiteQueryBuilder.setProjectionMap(sEventsProjectionMap);
                    strAppendAccountToSelection = appendLastSyncedColumnToSelection(appendAccountToSelection(uri, str5, "account_name", "account_type"), uri);
                    break;
                case 2:
                    sQLiteQueryBuilder.setTables("view_events");
                    sQLiteQueryBuilder.setProjectionMap(sEventsProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                    sQLiteQueryBuilder.appendWhere("_id=?");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 3:
                case 15:
                    try {
                        try {
                            return handleInstanceQuery(sQLiteQueryBuilder, Long.valueOf(uri.getPathSegments().get(2)).longValue(), Long.valueOf(uri.getPathSegments().get(3)).longValue(), strArr, str5, strArr2, str2, iMatch == 15, false, this.mCalendarCache.readTimezoneInstances(), isHomeTimezone());
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Cannot parse end " + uri.getPathSegments().get(3));
                        }
                    } catch (NumberFormatException e2) {
                        throw new IllegalArgumentException("Cannot parse begin " + uri.getPathSegments().get(2));
                    }
                case 4:
                    sQLiteQueryBuilder.setTables("Calendars");
                    sQLiteQueryBuilder.setProjectionMap(sCalendarsProjectionMap);
                    strAppendAccountToSelection = appendAccountToSelection(uri, str5, "account_name", "account_type");
                    break;
                case 5:
                    sQLiteQueryBuilder.setTables("Calendars");
                    sQLiteQueryBuilder.setProjectionMap(sCalendarsProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                    sQLiteQueryBuilder.appendWhere("_id=?");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 6:
                    sQLiteQueryBuilder.setTables("Attendees, Events, Calendars");
                    sQLiteQueryBuilder.setProjectionMap(sAttendeesProjectionMap);
                    sQLiteQueryBuilder.appendWhere("Events._id=Attendees.event_id AND Events.calendar_id=Calendars._id");
                    str3 = null;
                    strArr3 = strArr2;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 7:
                    sQLiteQueryBuilder.setTables("Attendees, Events, Calendars");
                    sQLiteQueryBuilder.setProjectionMap(sAttendeesProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                    sQLiteQueryBuilder.appendWhere("Attendees._id=? AND Events._id=Attendees.event_id AND Events.calendar_id=Calendars._id");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 8:
                    sQLiteQueryBuilder.setTables("Reminders");
                    str3 = null;
                    strArr3 = strArr2;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 9:
                    sQLiteQueryBuilder.setTables("Reminders, Events, Calendars");
                    sQLiteQueryBuilder.setProjectionMap(sRemindersProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getLastPathSegment());
                    sQLiteQueryBuilder.appendWhere("Reminders._id=? AND Events._id=Reminders.event_id AND Events.calendar_id=Calendars._id");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 10:
                    sQLiteQueryBuilder.setTables("ExtendedProperties");
                    str3 = null;
                    strArr3 = strArr2;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 11:
                    sQLiteQueryBuilder.setTables("ExtendedProperties");
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                    sQLiteQueryBuilder.appendWhere("ExtendedProperties._id=?");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 12:
                    sQLiteQueryBuilder.setTables("CalendarAlerts, view_events");
                    sQLiteQueryBuilder.setProjectionMap(sCalendarAlertsProjectionMap);
                    sQLiteQueryBuilder.appendWhere("view_events._id=CalendarAlerts.event_id");
                    str3 = null;
                    strArr3 = strArr2;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 13:
                    sQLiteQueryBuilder.setTables("CalendarAlerts, view_events");
                    sQLiteQueryBuilder.setProjectionMap(sCalendarAlertsProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getLastPathSegment());
                    sQLiteQueryBuilder.appendWhere("view_events._id=CalendarAlerts.event_id AND CalendarAlerts._id=?");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 14:
                    sQLiteQueryBuilder.setTables("CalendarAlerts, view_events");
                    sQLiteQueryBuilder.setProjectionMap(sCalendarAlertsProjectionMap);
                    sQLiteQueryBuilder.appendWhere("view_events._id=CalendarAlerts.event_id");
                    str3 = "event_id,begin";
                    strArr3 = strArr2;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 16:
                    return this.mDbHelper.getSyncState().query(readableDatabase, strArr, str5, strArr2, str2);
                case 17:
                    StringBuilder sb = new StringBuilder();
                    sb.append("_id=?");
                    if (str5 == null) {
                        str4 = "";
                    } else {
                        str4 = " AND (" + str5 + ")";
                    }
                    sb.append(str4);
                    return this.mDbHelper.getSyncState().query(readableDatabase, strArr, sb.toString(), insertSelectionArg(strArr2, String.valueOf(ContentUris.parseId(uri))), str2);
                case 18:
                    sQLiteQueryBuilder.setTables("view_events");
                    sQLiteQueryBuilder.setProjectionMap(sEventEntitiesProjectionMap);
                    strAppendAccountToSelection = appendLastSyncedColumnToSelection(appendAccountToSelection(uri, str5, "account_name", "account_type"), uri);
                    break;
                case 19:
                    sQLiteQueryBuilder.setTables("view_events");
                    sQLiteQueryBuilder.setProjectionMap(sEventEntitiesProjectionMap);
                    strArrInsertSelectionArg = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                    sQLiteQueryBuilder.appendWhere("_id=?");
                    strArr3 = strArrInsertSelectionArg;
                    str3 = null;
                    return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                case 20:
                    try {
                        try {
                            return handleEventDayQuery(sQLiteQueryBuilder, Integer.parseInt(uri.getPathSegments().get(2)), Integer.parseInt(uri.getPathSegments().get(3)), strArr, str5, this.mCalendarCache.readTimezoneInstances(), isHomeTimezone());
                        } catch (NumberFormatException e3) {
                            throw new IllegalArgumentException("Cannot parse end day " + uri.getPathSegments().get(3));
                        }
                    } catch (NumberFormatException e4) {
                        throw new IllegalArgumentException("Cannot parse start day " + uri.getPathSegments().get(2));
                    }
                default:
                    switch (iMatch) {
                        case 24:
                            break;
                        case 25:
                            break;
                        case 26:
                        case 27:
                            try {
                                try {
                                    return handleInstanceSearchQuery(sQLiteQueryBuilder, Long.valueOf(uri.getPathSegments().get(2)).longValue(), Long.valueOf(uri.getPathSegments().get(3)).longValue(), uri.getPathSegments().get(4), strArr, str5, strArr2, str2, iMatch == 27, this.mCalendarCache.readTimezoneInstances(), isHomeTimezone());
                                } catch (NumberFormatException e5) {
                                    throw new IllegalArgumentException("Cannot parse end " + uri.getPathSegments().get(3));
                                }
                            } catch (NumberFormatException e6) {
                                throw new IllegalArgumentException("Cannot parse begin " + uri.getPathSegments().get(2));
                            }
                        case 28:
                            sQLiteQueryBuilder.setTables("CalendarCache");
                            sQLiteQueryBuilder.setProjectionMap(sCalendarCacheProjectionMap);
                            str3 = null;
                            strArr3 = strArr2;
                            return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
                        default:
                            switch (iMatch) {
                                case 50:
                                    if (strArr2 != null) {
                                        return handleSearchSuggestionQuery(readableDatabase, sQLiteQueryBuilder, strArr2[0], uri.getQueryParameter("limit"));
                                    }
                                    break;
                                case 51:
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown URL " + uri);
                            }
                            sQLiteQueryBuilder.setTables("view_events");
                            sQLiteQueryBuilder.setProjectionMap(sEventsProjectionMap);
                            String[] strArrInsertSelectionArg2 = insertSelectionArg(strArr2, uri.getPathSegments().get(1));
                            sQLiteQueryBuilder.appendWhere("_id=?");
                            Cursor cursorQuery = sQLiteQueryBuilder.query(readableDatabase, SEARCH_PROJECTION, str5, strArrInsertSelectionArg2, null, null, null, null, null);
                            MatrixCursor matrixCursor = new MatrixCursor(SUGGESTIONS_COLUMNS);
                            if (cursorQuery != null) {
                                constructSearchSuggesions(matrixCursor, cursorQuery);
                                cursorQuery.close();
                            }
                            return matrixCursor;
                    }
                    break;
            }
        } else {
            sQLiteQueryBuilder.setTables("Colors");
            sQLiteQueryBuilder.setProjectionMap(sColorsProjectionMap);
            strAppendAccountToSelection = appendAccountToSelection(uri, str5, "account_name", "account_type");
        }
        str5 = strAppendAccountToSelection;
        str3 = null;
        strArr3 = strArr2;
        return query(readableDatabase, sQLiteQueryBuilder, strArr, str5, strArr3, str2, str3, null);
    }

    private void validateUriParameters(Set<String> set) {
        for (String str : set) {
            if (!ALLOWED_URI_PARAMETERS.contains(str)) {
                throw new IllegalArgumentException("Invalid URI parameter: " + str);
            }
        }
    }

    private Cursor query(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4) {
        SQLiteQueryBuilder sQLiteQueryBuilder2;
        String str5;
        String str6;
        String str7;
        String str8;
        if (strArr != null && strArr.length == 1 && "_count".equals(strArr[0])) {
            sQLiteQueryBuilder2 = sQLiteQueryBuilder;
            sQLiteQueryBuilder2.setProjectionMap(sCountProjectionMap);
        } else {
            sQLiteQueryBuilder2 = sQLiteQueryBuilder;
        }
        if (Log.isLoggable("CalendarProvider2", 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("query sql - projection: ");
            sb.append(Arrays.toString(strArr));
            sb.append(" selection: ");
            str5 = str;
            sb.append(str5);
            sb.append(" selectionArgs: ");
            sb.append(Arrays.toString(strArr2));
            sb.append(" sortOrder: ");
            str6 = str2;
            sb.append(str6);
            sb.append(" groupBy: ");
            str7 = str3;
            sb.append(str7);
            sb.append(" limit: ");
            str8 = str4;
            sb.append(str8);
            Log.v("CalendarProvider2", sb.toString());
        } else {
            str5 = str;
            str6 = str2;
            str7 = str3;
            str8 = str4;
        }
        Cursor cursorQuery = sQLiteQueryBuilder2.query(sQLiteDatabase, strArr, str5, strArr2, str7, null, str6, str8);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(this.mContentResolver, CalendarContract.Events.CONTENT_URI);
        }
        return cursorQuery;
    }

    private Cursor handleInstanceQuery(SQLiteQueryBuilder sQLiteQueryBuilder, long j, long j2, String[] strArr, String str, String[] strArr2, String str2, boolean z, boolean z2, String str3, boolean z3) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        sQLiteQueryBuilder.setTables("Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)");
        sQLiteQueryBuilder.setProjectionMap(sInstancesProjectionMap);
        if (z) {
            Time time = new Time(str3);
            acquireInstanceRange(time.setJulianDay((int) j), time.setJulianDay(((int) j2) + 1), true, z2, str3, z3);
            sQLiteQueryBuilder.appendWhere("startDay<=? AND endDay>=?");
        } else {
            acquireInstanceRange(j, j2, true, z2, str3, z3);
            sQLiteQueryBuilder.appendWhere("begin<=? AND end>=?");
        }
        String[] strArr3 = {String.valueOf(j2), String.valueOf(j)};
        return sQLiteQueryBuilder.query(this.mDb, strArr, str, strArr2 == null ? strArr3 : (String[]) combine(strArr3, strArr2), null, null, str2);
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

    String escapeSearchToken(String str) {
        return SEARCH_ESCAPE_PATTERN.matcher(str).replaceAll("#$1");
    }

    String[] tokenizeSearchQuery(String str) {
        String strGroup;
        ArrayList arrayList = new ArrayList();
        Matcher matcher = SEARCH_TOKEN_PATTERN.matcher(str);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                strGroup = matcher.group(1);
            } else {
                strGroup = matcher.group();
            }
            arrayList.add(escapeSearchToken(strGroup));
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    String constructSearchWhere(String[] strArr) {
        if (strArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            sb.append("(");
            for (int i2 = 0; i2 < SEARCH_COLUMNS.length; i2++) {
                sb.append(SEARCH_COLUMNS[i2]);
                sb.append(" LIKE ? ESCAPE \"");
                sb.append("#");
                sb.append("\" ");
                if (i2 < SEARCH_COLUMNS.length - 1) {
                    sb.append("OR ");
                }
            }
            sb.append(")");
            if (i < strArr.length - 1) {
                sb.append(" AND ");
            }
        }
        return sb.toString();
    }

    String[] constructSearchArgs(String[] strArr) {
        int length = SEARCH_COLUMNS.length;
        String[] strArr2 = new String[strArr.length * length];
        for (int i = 0; i < strArr.length; i++) {
            int i2 = length * i;
            for (int i3 = i2; i3 < i2 + length; i3++) {
                strArr2[i3] = "%" + strArr[i] + "%";
            }
        }
        return strArr2;
    }

    private Cursor handleInstanceSearchQuery(SQLiteQueryBuilder sQLiteQueryBuilder, long j, long j2, String str, String[] strArr, String str2, String[] strArr2, String str3, boolean z, String str4, boolean z2) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        sQLiteQueryBuilder.setTables("(Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)) LEFT OUTER JOIN Attendees ON (Attendees.event_id=Events._id)");
        sQLiteQueryBuilder.setProjectionMap(sInstancesProjectionMap);
        String[] strArr3 = tokenizeSearchQuery(str);
        String[] strArrConstructSearchArgs = constructSearchArgs(strArr3);
        String[] strArr4 = {String.valueOf(j2), String.valueOf(j)};
        String[] strArr5 = strArr2 == null ? (String[]) combine(strArr4, strArrConstructSearchArgs) : (String[]) combine(strArr4, strArr2, strArrConstructSearchArgs);
        String strConstructSearchWhere = constructSearchWhere(strArr3);
        if (z) {
            Time time = new Time(str4);
            acquireInstanceRange(time.setJulianDay((int) j), time.setJulianDay(((int) j2) + 1), true, false, str4, z2);
            sQLiteQueryBuilder.appendWhere("startDay<=? AND endDay>=?");
        } else {
            acquireInstanceRange(j, j2, true, false, str4, z2);
            sQLiteQueryBuilder.appendWhere("begin<=? AND end>=?");
        }
        return sQLiteQueryBuilder.query(this.mDb, strArr, str2, strArr5, "Instances._id", strConstructSearchWhere, str3);
    }

    private Cursor handleEventDayQuery(SQLiteQueryBuilder sQLiteQueryBuilder, int i, int i2, String[] strArr, String str, String str2, boolean z) {
        this.mDb = this.mDbHelper.getWritableDatabase();
        sQLiteQueryBuilder.setTables("Instances INNER JOIN view_events AS Events ON (Instances.event_id=Events._id)");
        sQLiteQueryBuilder.setProjectionMap(sInstancesProjectionMap);
        Time time = new Time(str2);
        acquireInstanceRange(time.setJulianDay(i), time.setJulianDay(i2 + 1), true, false, str2, z);
        sQLiteQueryBuilder.appendWhere("startDay<=? AND endDay>=?");
        return sQLiteQueryBuilder.query(this.mDb, strArr, str, new String[]{String.valueOf(i2), String.valueOf(i)}, "startDay", null, null);
    }

    private void acquireInstanceRange(long j, long j2, boolean z, boolean z2, String str, boolean z3) {
        this.mDb.beginTransaction();
        try {
            acquireInstanceRangeLocked(j, j2, z, z2, str, z3);
            this.mDb.setTransactionSuccessful();
        } finally {
            this.mDb.endTransaction();
        }
    }

    void acquireInstanceRangeLocked(long j, long j2, boolean z, boolean z2, String str, boolean z3) {
        long j3;
        long j4;
        boolean z4;
        long j5;
        long j6;
        long j7;
        String str2 = str;
        if (DEBUG_INSTANCES) {
            Log.d("CalendarProvider2-i", "acquireInstanceRange begin=" + j + " end=" + j2 + " useMin=" + z + " force=" + z2);
        }
        if (str2 == null) {
            Log.e("CalendarProvider2", "Cannot run acquireInstanceRangeLocked() because instancesTimezone is null");
            return;
        }
        if (z) {
            long j8 = j2 - j;
            if (j8 < 5356800000L) {
                long j9 = (5356800000L - j8) / 2;
                j3 = j - j9;
                j4 = j9 + j2;
            } else {
                j3 = j;
                j4 = j2;
            }
        }
        MetaData.Fields fieldsLocked = this.mMetaData.getFieldsLocked();
        long j10 = fieldsLocked.maxInstance;
        long j11 = j4;
        long j12 = fieldsLocked.minInstance;
        if (z3) {
            z4 = !str2.equals(this.mCalendarCache.readTimezoneInstancesPrevious());
        } else {
            String id = TimeZone.getDefault().getID();
            z4 = !str2.equals(id);
            if (z4) {
                str2 = id;
            }
        }
        if (j10 == 0 || z4 || z2) {
            if (DEBUG_INSTANCES) {
                Log.d("CalendarProvider2-i", "Wiping instances and expanding from scratch");
            }
            this.mDb.execSQL("DELETE FROM Instances;");
            if (Log.isLoggable("CalendarProvider2", 2)) {
                Log.v("CalendarProvider2", "acquireInstanceRangeLocked() deleted Instances, timezone changed: " + z4);
            }
            this.mInstancesHelper.expandInstanceRangeLocked(j3, j11, str2);
            this.mMetaData.writeLocked(str2, j3, j11);
            String timezoneType = this.mCalendarCache.readTimezoneType();
            this.mCalendarCache.writeTimezoneInstances(str2);
            if ("auto".equals(timezoneType) && TextUtils.equals("GMT", this.mCalendarCache.readTimezoneInstancesPrevious())) {
                this.mCalendarCache.writeTimezoneInstancesPrevious(str2);
                return;
            }
            return;
        }
        if (j < j12 || j2 > j10) {
            j5 = j10;
        } else if (j10 > j12 && j10 - j12 > 172800000000L) {
            j5 = j10;
            this.mInstancesHelper.expandInstanceRangeLocked(j3, j11, str2);
            Log.w("CalendarProvider2-i", "###### instances range is already changed.");
        } else {
            if (DEBUG_INSTANCES) {
                Log.d("CalendarProvider2-i", "instances are already expanded");
            }
            if (Log.isLoggable("CalendarProvider2", 2)) {
                Log.v("CalendarProvider2", "Canceled instance query (" + j3 + ", " + j11 + ") falls within previously expanded range.");
                return;
            }
            return;
        }
        if (j < j12) {
            j6 = j11;
            this.mInstancesHelper.expandInstanceRangeLocked(j3, j12, str2);
        } else {
            j6 = j11;
            j3 = j12;
        }
        if (j2 > j5) {
            j7 = j6;
            this.mInstancesHelper.expandInstanceRangeLocked(j5, j7, str2);
        } else {
            j7 = j5;
        }
        this.mMetaData.writeLocked(str2, j3, j7);
    }

    @Override
    public String getType(Uri uri) {
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 20) {
            return "vnd.android.cursor.dir/event-instance";
        }
        if (iMatch == 23) {
            return "time/epoch";
        }
        if (iMatch != 28) {
            switch (iMatch) {
                case 1:
                    return "vnd.android.cursor.dir/event";
                case 2:
                    return "vnd.android.cursor.item/event";
                case 3:
                    return "vnd.android.cursor.dir/event-instance";
                default:
                    switch (iMatch) {
                        case 8:
                            return "vnd.android.cursor.dir/reminder";
                        case 9:
                            return "vnd.android.cursor.item/reminder";
                        default:
                            switch (iMatch) {
                                case 12:
                                    return "vnd.android.cursor.dir/calendar-alert";
                                case 13:
                                    return "vnd.android.cursor.item/calendar-alert";
                                case 14:
                                    return "vnd.android.cursor.dir/calendar-alert-by-instance";
                                case 15:
                                    return "vnd.android.cursor.dir/event-instance";
                                default:
                                    throw new IllegalArgumentException("Unknown URL " + uri);
                            }
                    }
            }
        }
        return "vnd.android.cursor.dir/property";
    }

    public static boolean isRecurrenceEvent(String str, String str2, String str3, String str4) {
        return (TextUtils.isEmpty(str) && TextUtils.isEmpty(str2) && TextUtils.isEmpty(str3) && TextUtils.isEmpty(str4)) ? false : true;
    }

    private boolean fixAllDayTime(ContentValues contentValues, ContentValues contentValues2) {
        boolean z;
        int length;
        Integer asInteger = contentValues.getAsInteger("allDay");
        if (asInteger == null || asInteger.intValue() == 0) {
            return false;
        }
        Long asLong = contentValues.getAsLong("dtstart");
        Long asLong2 = contentValues.getAsLong("dtend");
        String asString = contentValues.getAsString("duration");
        Time time = new Time();
        time.clear("UTC");
        time.set(asLong.longValue());
        if (time.hour != 0 || time.minute != 0 || time.second != 0) {
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            contentValues2.put("dtstart", Long.valueOf(time.toMillis(true)));
            z = true;
        } else {
            z = false;
        }
        if (asLong2 != null) {
            time.clear("UTC");
            time.set(asLong2.longValue());
            if (time.hour != 0 || time.minute != 0 || time.second != 0) {
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                contentValues2.put("dtend", Long.valueOf(time.toMillis(true)));
                z = true;
            }
        }
        if (asString == null || (length = asString.length()) == 0 || asString.charAt(0) != 'P') {
            return z;
        }
        int i = length - 1;
        if (asString.charAt(i) != 'S') {
            return z;
        }
        contentValues2.put("duration", "P" + (((Integer.parseInt(asString.substring(1, i)) + 86400) - 1) / 86400) + "D");
        return true;
    }

    private void checkAllowedInException(Set<String> set) {
        for (String str : set) {
            if (!ALLOWED_IN_EXCEPTION.contains(str.intern())) {
                throw new IllegalArgumentException("Exceptions can't overwrite " + str);
            }
        }
    }

    private static ContentValues setRecurrenceEnd(ContentValues contentValues, long j) {
        boolean zBooleanValue = contentValues.getAsBoolean("allDay").booleanValue();
        String asString = contentValues.getAsString("rrule");
        EventRecurrence eventRecurrence = new EventRecurrence();
        eventRecurrence.parse(asString);
        long jLongValue = contentValues.getAsLong("dtstart").longValue();
        Time time = new Time();
        time.timezone = contentValues.getAsString("eventTimezone");
        time.set(jLongValue);
        ContentValues contentValues2 = new ContentValues();
        if (eventRecurrence.count > 0) {
            try {
                long[] jArrExpand = new RecurrenceProcessor().expand(time, new RecurrenceSet(contentValues), jLongValue, j);
                if (jArrExpand.length == 0) {
                    throw new RuntimeException("can't use this method on first instance");
                }
                EventRecurrence eventRecurrence2 = new EventRecurrence();
                eventRecurrence2.parse(asString);
                eventRecurrence2.count -= jArrExpand.length;
                contentValues.put("rrule", eventRecurrence2.toString());
                eventRecurrence.count = jArrExpand.length;
            } catch (DateException e) {
                throw new RuntimeException(e);
            }
        } else {
            Time time2 = new Time();
            time2.timezone = "UTC";
            time2.set(j - 1000);
            if (zBooleanValue) {
                time2.second = 0;
                time2.minute = 0;
                time2.hour = 0;
                time2.allDay = true;
                time2.normalize(false);
                time.second = 0;
                time.minute = 0;
                time.hour = 0;
                time.allDay = true;
                time.timezone = "UTC";
            }
            eventRecurrence.until = time2.format2445();
        }
        contentValues2.put("rrule", eventRecurrence.toString());
        contentValues2.put("dtstart", Long.valueOf(time.normalize(true)));
        return contentValues2;
    }

    private long handleInsertException(long j, ContentValues contentValues, boolean z) throws Throwable {
        ?? r10;
        boolean z2;
        long jInsert;
        ?? r102;
        long jLongValue;
        ?? r103;
        String str;
        String str2;
        Account account;
        Long asLong = contentValues.getAsLong("originalInstanceTime");
        if (asLong == null) {
            throw new IllegalArgumentException("Exceptions must specify originalInstanceTime");
        }
        checkAllowedInException(contentValues.keySet());
        if (!z) {
            contentValues.put("dirty", (Boolean) true);
            addMutator(contentValues, "mutators");
        }
        this.mDb.beginTransaction();
        try {
            Cursor cursorQuery = this.mDb.query("Events", null, "_id=?", new String[]{String.valueOf(j)}, null, null, null);
            try {
                if (cursorQuery.getCount() != 1) {
                    Log.e("CalendarProvider2", "Original event ID " + j + " lookup failed (count is " + cursorQuery.getCount() + ")");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    this.mDb.endTransaction();
                    return -1L;
                }
                String asString = contentValues.getAsString("eventColor_index");
                if (!TextUtils.isEmpty(asString)) {
                    Long lValueOf = Long.valueOf(cursorQuery.getLong(cursorQuery.getColumnIndex("calendar_id")));
                    if (lValueOf == null || (account = getAccount(lValueOf.longValue())) == null) {
                        str = null;
                        str2 = null;
                    } else {
                        str2 = account.name;
                        str = account.type;
                    }
                    verifyColorExists(str2, str, asString, 1);
                }
                cursorQuery.moveToFirst();
                if (TextUtils.isEmpty(cursorQuery.getString(cursorQuery.getColumnIndex("rrule")))) {
                    Log.e("CalendarProvider2", "Original event has no rrule");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    this.mDb.endTransaction();
                    return -1L;
                }
                if (!TextUtils.isEmpty(cursorQuery.getString(cursorQuery.getColumnIndex("original_id")))) {
                    Log.e("CalendarProvider2", "Original event is an exception");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    this.mDb.endTransaction();
                    return -1L;
                }
                boolean zIsEmpty = TextUtils.isEmpty(contentValues.getAsString("rrule"));
                ContentValues contentValues2 = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorQuery, contentValues2);
                cursorQuery.close();
                try {
                    if (zIsEmpty) {
                        String asString2 = contentValues2.getAsString("_id");
                        String asString3 = contentValues2.getAsString("_sync_id");
                        boolean zBooleanValue = contentValues2.getAsBoolean("allDay").booleanValue();
                        String[] strArr = DONT_CLONE_INTO_EXCEPTION;
                        int length = strArr.length;
                        for (String str3 : strArr) {
                            contentValues2.remove(str3);
                        }
                        contentValues2.putAll(contentValues);
                        contentValues2.put("original_id", asString2);
                        contentValues2.put("original_sync_id", asString3);
                        contentValues2.put("originalAllDay", Boolean.valueOf(zBooleanValue));
                        if (!contentValues2.containsKey("eventStatus")) {
                            contentValues2.put("eventStatus", (Integer) 0);
                        }
                        contentValues2.remove("rrule");
                        contentValues2.remove("rdate");
                        contentValues2.remove("exrule");
                        contentValues2.remove("exdate");
                        Duration duration = new Duration();
                        String asString4 = contentValues2.getAsString("duration");
                        try {
                            duration.parse(asString4);
                            if (contentValues.containsKey("dtstart")) {
                                jLongValue = contentValues2.getAsLong("dtstart").longValue();
                                r103 = length;
                            } else {
                                jLongValue = contentValues2.getAsLong("originalInstanceTime").longValue();
                                Long lValueOf2 = Long.valueOf(jLongValue);
                                contentValues2.put("dtstart", lValueOf2);
                                r103 = lValueOf2;
                            }
                            contentValues2.put("dtend", Long.valueOf(jLongValue + duration.getMillis()));
                            contentValues2.remove("duration");
                            r102 = r103;
                        } catch (Exception e) {
                            Log.w("CalendarProvider2", "Bad duration in recurring event: " + asString4, e);
                            this.mDb.endTransaction();
                            return -1L;
                        }
                    } else {
                        boolean z3 = contentValues2.getAsInteger("eventStatus").intValue() == 2;
                        if (asLong.equals(contentValues2.getAsLong("dtstart"))) {
                            if (z3) {
                                Log.d("CalendarProvider2", "Note: canceling entire event via exception call");
                            }
                            if (!validateRecurrenceRule(contentValues)) {
                                throw new IllegalArgumentException("Invalid recurrence rule: " + contentValues2.getAsString("rrule"));
                            }
                            contentValues.remove("originalInstanceTime");
                            SQLiteDatabase sQLiteDatabase = this.mDb;
                            String[] strArr2 = {Long.toString(j)};
                            sQLiteDatabase.update("Events", contentValues, "_id=?", strArr2);
                            z2 = false;
                            r10 = strArr2;
                            if (z2) {
                                this.mInstancesHelper.updateInstancesLocked(contentValues2, j, false, this.mDb);
                                jInsert = j;
                            } else {
                                contentValues2.remove("_id");
                                if (z) {
                                    scrubEventData(contentValues2, null);
                                } else {
                                    validateEventData(contentValues2);
                                }
                                jInsert = this.mDb.insert("Events", null, contentValues2);
                                if (jInsert < 0) {
                                    Log.w("CalendarProvider2", "Unable to add exception to recurring event");
                                    Log.w("CalendarProvider2", "Values: " + contentValues2);
                                    this.mDb.endTransaction();
                                    return -1L;
                                }
                                this.mInstancesHelper.updateInstancesLocked(contentValues2, jInsert, true, this.mDb);
                                CalendarDatabaseHelper.copyEventRelatedTables(this.mDb, jInsert, j);
                                if (contentValues.containsKey("selfAttendeeStatus")) {
                                    String owner = getOwner(contentValues2.getAsLong("calendar_id").longValue());
                                    if (owner != null) {
                                        ContentValues contentValues3 = new ContentValues();
                                        contentValues3.put("attendeeStatus", contentValues.getAsString("selfAttendeeStatus"));
                                        int iUpdate = this.mDb.update("Attendees", contentValues3, "event_id=? AND attendeeEmail=? COLLATE NOCASE ", new String[]{String.valueOf(jInsert), owner});
                                        if (iUpdate != 1 && iUpdate != 2) {
                                            Log.e("CalendarProvider2", "Attendee status update on event=" + jInsert + " touched " + iUpdate + " rows. Expected one or two rows.");
                                            throw new RuntimeException("Status update WTF");
                                        }
                                    }
                                }
                            }
                            this.mDb.setTransactionSuccessful();
                            this.mDb.endTransaction();
                            return jInsert;
                        }
                        ContentValues recurrenceEnd = setRecurrenceEnd(contentValues2, asLong.longValue());
                        SQLiteDatabase sQLiteDatabase2 = this.mDb;
                        String string = Long.toString(j);
                        sQLiteDatabase2.update("Events", recurrenceEnd, "_id=?", new String[]{string});
                        contentValues2.putAll(contentValues);
                        contentValues2.remove("originalInstanceTime");
                        r102 = string;
                    }
                    if (z2) {
                    }
                    this.mDb.setTransactionSuccessful();
                    this.mDb.endTransaction();
                    return jInsert;
                } catch (Throwable th) {
                    th = th;
                }
                z2 = true;
                r10 = r102;
            } catch (Throwable th2) {
                th = th2;
                r10 = cursorQuery;
            }
        } catch (Throwable th3) {
            th = th3;
            r10 = 0;
        }
        if (r10 != 0) {
            r10.close();
        }
        this.mDb.endTransaction();
        throw th;
    }

    private void backfillExceptionOriginalIds(long j, ContentValues contentValues) {
        String asString = contentValues.getAsString("_sync_id");
        String asString2 = contentValues.getAsString("rrule");
        String asString3 = contentValues.getAsString("rdate");
        String asString4 = contentValues.getAsString("calendar_id");
        if (!TextUtils.isEmpty(asString) && !TextUtils.isEmpty(asString4)) {
            if (TextUtils.isEmpty(asString2) && TextUtils.isEmpty(asString3)) {
                return;
            }
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("original_id", Long.valueOf(j));
            this.mDb.update("Events", contentValues2, "original_sync_id=? AND calendar_id=?", new String[]{asString, asString4});
        }
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues contentValues, boolean z) throws Throwable {
        String owner;
        long jExtendedPropertiesInsert;
        String str;
        String str2;
        long jCalendarsInsert;
        Cursor colorByTypeIndex;
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "insertInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int iMatch = sUriMatcher.match(uri);
        verifyTransactionAllowed(1, uri, contentValues, z, iMatch, null, null);
        this.mDb = this.mDbHelper.getWritableDatabase();
        switch (iMatch) {
            case 1:
                if (!z) {
                    contentValues.put("dirty", (Integer) 1);
                    addMutator(contentValues, "mutators");
                }
                if (!contentValues.containsKey("dtstart")) {
                    if (!contentValues.containsKey("original_sync_id") || !contentValues.containsKey("originalInstanceTime") || 2 != contentValues.getAsInteger("eventStatus").intValue()) {
                        throw new RuntimeException("DTSTART field missing from event");
                    }
                    long jLongValue = contentValues.getAsLong("originalInstanceTime").longValue();
                    contentValues.put("dtstart", Long.valueOf(jLongValue));
                    contentValues.put("dtend", Long.valueOf(jLongValue));
                    contentValues.put("eventTimezone", "UTC");
                }
                ContentValues contentValues2 = new ContentValues(contentValues);
                if (z) {
                    scrubEventData(contentValues2, null);
                } else {
                    validateEventData(contentValues2);
                }
                ContentValues contentValuesUpdateLastDate = updateLastDate(contentValues2);
                if (contentValuesUpdateLastDate == null) {
                    throw new RuntimeException("Could not insert event.");
                }
                Long asLong = contentValuesUpdateLastDate.getAsLong("calendar_id");
                if (asLong == null) {
                    throw new IllegalArgumentException("New events must specify a calendar id");
                }
                String asString = contentValuesUpdateLastDate.getAsString("eventColor_index");
                if (!TextUtils.isEmpty(asString)) {
                    Account account = getAccount(asLong.longValue());
                    if (account != null) {
                        str2 = account.name;
                        str = account.type;
                    } else {
                        str = null;
                        str2 = null;
                    }
                    contentValuesUpdateLastDate.put("eventColor", Integer.valueOf(verifyColorExists(str2, str, asString, 1)));
                }
                if (contentValuesUpdateLastDate.containsKey("organizer")) {
                    owner = null;
                } else {
                    owner = getOwner(asLong.longValue());
                    if (owner != null) {
                        contentValuesUpdateLastDate.put("organizer", owner);
                    }
                }
                if (contentValuesUpdateLastDate.containsKey("original_sync_id") && !contentValuesUpdateLastDate.containsKey("original_id")) {
                    long originalId = getOriginalId(contentValuesUpdateLastDate.getAsString("original_sync_id"), contentValuesUpdateLastDate.getAsString("calendar_id"));
                    if (originalId != -1) {
                        contentValuesUpdateLastDate.put("original_id", Long.valueOf(originalId));
                    }
                } else if (!contentValuesUpdateLastDate.containsKey("original_sync_id") && contentValuesUpdateLastDate.containsKey("original_id")) {
                    String originalSyncId = getOriginalSyncId(contentValuesUpdateLastDate.getAsLong("original_id").longValue());
                    if (!TextUtils.isEmpty(originalSyncId)) {
                        contentValuesUpdateLastDate.put("original_sync_id", originalSyncId);
                    }
                }
                if (fixAllDayTime(contentValuesUpdateLastDate, contentValuesUpdateLastDate) && Log.isLoggable("CalendarProvider2", 5)) {
                    Log.w("CalendarProvider2", "insertInTransaction: allDay is true but sec, min, hour were not 0.");
                }
                contentValuesUpdateLastDate.remove("hasAlarm");
                contentValuesUpdateLastDate.put("createTime", Long.valueOf(System.currentTimeMillis()));
                limitTextLength(contentValuesUpdateLastDate);
                long jEventsInsert = this.mDbHelper.eventsInsert(contentValuesUpdateLastDate);
                if (jEventsInsert != -1) {
                    updateEventRawTimesLocked(jEventsInsert, contentValuesUpdateLastDate);
                    this.mInstancesHelper.updateInstancesLocked(contentValuesUpdateLastDate, jEventsInsert, true, this.mDb);
                    if (contentValues.containsKey("selfAttendeeStatus")) {
                        int iIntValue = contentValues.getAsInteger("selfAttendeeStatus").intValue();
                        if (owner == null) {
                            owner = getOwner(asLong.longValue());
                        }
                        createAttendeeEntry(jEventsInsert, iIntValue, owner);
                    }
                    backfillExceptionOriginalIds(jEventsInsert, contentValues);
                    sendUpdateNotification(jEventsInsert, z);
                }
                jExtendedPropertiesInsert = jEventsInsert;
                if (jExtendedPropertiesInsert >= 0) {
                    return null;
                }
                return ContentUris.withAppendedId(uri, jExtendedPropertiesInsert);
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
                Integer asInteger = contentValues.getAsInteger("sync_events");
                if (asInteger != null && asInteger.intValue() == 1) {
                    this.mDbHelper.scheduleSync(new Account(contentValues.getAsString("account_name"), contentValues.getAsString("account_type")), false, contentValues.getAsString("cal_sync1"));
                }
                String asString2 = contentValues.getAsString("calendar_color_index");
                if (!TextUtils.isEmpty(asString2)) {
                    contentValues.put("calendar_color", Integer.valueOf(verifyColorExists(contentValues.getAsString("account_name"), contentValues.getAsString("account_type"), asString2, 0)));
                }
                jCalendarsInsert = this.mDbHelper.calendarsInsert(contentValues);
                sendUpdateNotification(jCalendarsInsert, z);
                jExtendedPropertiesInsert = jCalendarsInsert;
                if (jExtendedPropertiesInsert >= 0) {
                }
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
                if (!contentValues.containsKey("event_id")) {
                    throw new IllegalArgumentException("Attendees values must contain an event_id");
                }
                if (!doesEventExist(contentValues.getAsLong("event_id").longValue())) {
                    Log.i("CalendarProvider2", "Trying to insert a attendee to a non-existent event");
                    return null;
                }
                if (!z) {
                    Long asLong2 = contentValues.getAsLong("event_id");
                    this.mDbHelper.duplicateEvent(asLong2.longValue());
                    setEventDirty(asLong2.longValue());
                }
                jCalendarsInsert = this.mDbHelper.attendeesInsert(contentValues);
                updateEventAttendeeStatus(this.mDb, contentValues);
                jExtendedPropertiesInsert = jCalendarsInsert;
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 8:
                Long asLong3 = contentValues.getAsLong("event_id");
                if (asLong3 == null) {
                    throw new IllegalArgumentException("Reminders values must contain a numeric event_id");
                }
                if (!doesEventExist(asLong3.longValue())) {
                    Log.i("CalendarProvider2", "Trying to insert a reminder to a non-existent event");
                    return null;
                }
                if (!z) {
                    this.mDbHelper.duplicateEvent(asLong3.longValue());
                    setEventDirty(asLong3.longValue());
                }
                long jRemindersInsert = this.mDbHelper.remindersInsert(contentValues);
                setHasAlarm(asLong3.longValue(), 1);
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "insertInternal() changing reminder");
                }
                this.mCalendarAlarm.scheduleNextAlarm(false);
                jExtendedPropertiesInsert = jRemindersInsert;
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 10:
                Long asLong4 = contentValues.getAsLong("event_id");
                if (asLong4 == null) {
                    throw new IllegalArgumentException("ExtendedProperties values must contain a numeric event_id");
                }
                if (!doesEventExist(asLong4.longValue())) {
                    Log.i("CalendarProvider2", "Trying to insert extended properties to a non-existent event id = " + asLong4);
                    return null;
                }
                if (!z) {
                    Long asLong5 = contentValues.getAsLong("event_id");
                    this.mDbHelper.duplicateEvent(asLong5.longValue());
                    setEventDirty(asLong5.longValue());
                }
                jExtendedPropertiesInsert = this.mDbHelper.extendedPropertiesInsert(contentValues);
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 12:
                Long asLong6 = contentValues.getAsLong("event_id");
                if (asLong6 == null) {
                    throw new IllegalArgumentException("CalendarAlerts values must contain a numeric event_id");
                }
                if (!doesEventExist(asLong6.longValue())) {
                    Log.i("CalendarProvider2", "Trying to insert an alert to a non-existent event");
                    return null;
                }
                jExtendedPropertiesInsert = this.mDbHelper.calendarAlertsInsert(contentValues);
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 16:
                jExtendedPropertiesInsert = this.mDbHelper.getSyncState().insert(this.mDb, contentValues);
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 29:
                jExtendedPropertiesInsert = handleInsertException(ContentUris.parseId(uri), contentValues, z);
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 31:
                handleEmmaRequest(contentValues);
                jExtendedPropertiesInsert = 0;
                if (jExtendedPropertiesInsert >= 0) {
                }
                break;
            case 32:
                String queryParameter = uri.getQueryParameter("account_name");
                String queryParameter2 = uri.getQueryParameter("account_type");
                String asString3 = contentValues.getAsString("color_index");
                if (TextUtils.isEmpty(queryParameter) || TextUtils.isEmpty(queryParameter2)) {
                    throw new IllegalArgumentException("Account name and type must be non empty parameters for " + uri);
                }
                if (TextUtils.isEmpty(asString3)) {
                    throw new IllegalArgumentException("COLOR_INDEX must be non empty for " + uri);
                }
                if (!contentValues.containsKey("color_type") || !contentValues.containsKey("color")) {
                    throw new IllegalArgumentException("New colors must contain COLOR_TYPE and COLOR");
                }
                contentValues.put("account_name", queryParameter);
                contentValues.put("account_type", queryParameter2);
                try {
                    long jLongValue2 = contentValues.getAsLong("color_type").longValue();
                    colorByTypeIndex = getColorByTypeIndex(queryParameter, queryParameter2, jLongValue2, asString3);
                    try {
                        if (colorByTypeIndex.getCount() != 0) {
                            throw new IllegalArgumentException("color type " + jLongValue2 + " and index " + asString3 + " already exists for account and type provided");
                        }
                        if (colorByTypeIndex != null) {
                            colorByTypeIndex.close();
                        }
                        jExtendedPropertiesInsert = this.mDbHelper.colorsInsert(contentValues);
                        if (jExtendedPropertiesInsert >= 0) {
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (colorByTypeIndex != null) {
                            colorByTypeIndex.close();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    colorByTypeIndex = null;
                }
                break;
        }
    }

    private boolean doesEventExist(long j) {
        return DatabaseUtils.queryNumEntries(this.mDb, "Events", "_id=?", new String[]{String.valueOf(j)}) > 0;
    }

    private static void handleEmmaRequest(ContentValues contentValues) {
        String asString = contentValues.getAsString("cmd");
        if (asString.equals("start")) {
            Log.d("CalendarProvider2", "Emma coverage testing started");
            return;
        }
        if (asString.equals("stop")) {
            String asString2 = contentValues.getAsString("outputFileName");
            File file = new File(asString2);
            try {
                Class.forName("com.vladium.emma.rt.RT").getMethod("dumpCoverageData", file.getClass(), Boolean.TYPE, Boolean.TYPE).invoke(null, file, false, false);
                Log.d("CalendarProvider2", "Emma coverage data written to " + asString2);
            } catch (Exception e) {
                throw new RuntimeException("Emma coverage dump failed", e);
            }
        }
    }

    private boolean validateRecurrenceRule(ContentValues contentValues) {
        String asString = contentValues.getAsString("rrule");
        if (!TextUtils.isEmpty(asString)) {
            for (String str : asString.split("\n")) {
                try {
                    new EventRecurrence().parse(str);
                } catch (EventRecurrence.InvalidFormatException e) {
                    Log.w("CalendarProvider2", "Invalid recurrence rule: " + str);
                    dumpEventNoPII(contentValues);
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private void dumpEventNoPII(ContentValues contentValues) {
        if (contentValues == null) {
            return;
        }
        Log.i("CalendarProvider2", "dtStart:       " + contentValues.getAsLong("dtstart") + "\ndtEnd:         " + contentValues.getAsLong("dtend") + "\nall_day:       " + contentValues.getAsInteger("allDay") + "\ntz:            " + contentValues.getAsString("eventTimezone") + "\ndur:           " + contentValues.getAsString("duration") + "\nrrule:         " + contentValues.getAsString("rrule") + "\nrdate:         " + contentValues.getAsString("rdate") + "\nlast_date:     " + contentValues.getAsLong("lastDate") + "\nid:            " + contentValues.getAsLong("_id") + "\nsync_id:       " + contentValues.getAsString("_sync_id") + "\nori_id:        " + contentValues.getAsLong("original_id") + "\nori_sync_id:   " + contentValues.getAsString("original_sync_id") + "\nori_inst_time: " + contentValues.getAsLong("originalInstanceTime") + "\nori_all_day:   " + contentValues.getAsInteger("originalAllDay"));
    }

    private void scrubEventData(ContentValues contentValues, ContentValues contentValues2) {
        boolean z = contentValues.getAsLong("dtend") != null;
        boolean z2 = !TextUtils.isEmpty(contentValues.getAsString("duration"));
        boolean z3 = !TextUtils.isEmpty(contentValues.getAsString("rrule"));
        boolean z4 = !TextUtils.isEmpty(contentValues.getAsString("rdate"));
        boolean z5 = !TextUtils.isEmpty(contentValues.getAsString("original_sync_id"));
        boolean z6 = contentValues.getAsLong("originalInstanceTime") != null;
        if (z3 || z4) {
            if (!validateRecurrenceRule(contentValues)) {
                throw new IllegalArgumentException("Invalid recurrence rule: " + contentValues.getAsString("rrule"));
            }
            if (z || !z2 || z5 || z6) {
                Log.d("CalendarProvider2", "Scrubbing DTEND, ORIGINAL_SYNC_ID, ORIGINAL_INSTANCE_TIME");
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Invalid values for recurrence: " + contentValues);
                }
                contentValues.remove("dtend");
                contentValues.remove("original_sync_id");
                contentValues.remove("originalInstanceTime");
                if (contentValues2 != null) {
                    contentValues2.putNull("dtend");
                    contentValues2.putNull("original_sync_id");
                    contentValues2.putNull("originalInstanceTime");
                    return;
                }
                return;
            }
            return;
        }
        if (z5 || z6) {
            if (!z || z2 || !z5 || !z6) {
                Log.d("CalendarProvider2", "Scrubbing DURATION");
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "Invalid values for recurrence exception: " + contentValues);
                }
                contentValues.remove("duration");
                if (contentValues2 != null) {
                    contentValues2.putNull("duration");
                    return;
                }
                return;
            }
            return;
        }
        if (!z || z2) {
            Log.d("CalendarProvider2", "Scrubbing DURATION");
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Invalid values for event: " + contentValues);
            }
            contentValues.remove("duration");
            if (contentValues2 != null) {
                contentValues2.putNull("duration");
            }
        }
    }

    private void validateEventData(ContentValues contentValues) {
        if (TextUtils.isEmpty(contentValues.getAsString("calendar_id"))) {
            throw new IllegalArgumentException("Event values must include a calendar_id");
        }
        if (TextUtils.isEmpty(contentValues.getAsString("eventTimezone"))) {
            throw new IllegalArgumentException("Event values must include an eventTimezone");
        }
        boolean z = contentValues.getAsLong("dtstart") != null;
        boolean z2 = contentValues.getAsLong("dtend") != null;
        boolean z3 = !TextUtils.isEmpty(contentValues.getAsString("duration"));
        boolean z4 = !TextUtils.isEmpty(contentValues.getAsString("rrule"));
        boolean zIsEmpty = true ^ TextUtils.isEmpty(contentValues.getAsString("rdate"));
        if ((z4 || zIsEmpty) && !validateRecurrenceRule(contentValues)) {
            throw new IllegalArgumentException("Invalid recurrence rule: " + contentValues.getAsString("rrule"));
        }
        if (!z) {
            dumpEventNoPII(contentValues);
            throw new IllegalArgumentException("DTSTART cannot be empty.");
        }
        if (!z3 && !z2) {
            dumpEventNoPII(contentValues);
            throw new IllegalArgumentException("DTEND and DURATION cannot both be null for an event.");
        }
        if (z3 && z2) {
            dumpEventNoPII(contentValues);
            throw new IllegalArgumentException("Cannot have both DTEND and DURATION in an event");
        }
    }

    private void setEventDirty(long j) {
        boolean z;
        String strStringForQuery = DatabaseUtils.stringForQuery(this.mDb, "SELECT mutators FROM Events WHERE _id=?", new String[]{String.valueOf(j)});
        String callingPackageName = getCallingPackageName();
        if (!TextUtils.isEmpty(strStringForQuery)) {
            String[] strArrSplit = strStringForQuery.split(",");
            int length = strArrSplit.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    if (!strArrSplit[i].equals(callingPackageName)) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                strStringForQuery = strStringForQuery + "," + callingPackageName;
            }
        } else {
            strStringForQuery = callingPackageName;
        }
        this.mDb.execSQL("UPDATE Events SET dirty=1,mutators=?  WHERE _id=?", new Object[]{strStringForQuery, Long.valueOf(j)});
    }

    private long getOriginalId(String str, String str2) throws Throwable {
        long j = -1;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return -1L;
        }
        Cursor cursor = null;
        try {
            Cursor cursorQuery = query(CalendarContract.Events.CONTENT_URI, ID_ONLY_PROJECTION, "_sync_id=? AND calendar_id=?", new String[]{str, str2}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        j = cursorQuery.getLong(0);
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return j;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private String getOriginalSyncId(long j) throws Throwable {
        Cursor cursor = null;
        string = null;
        String string = null;
        if (j == -1) {
            return null;
        }
        try {
            Cursor cursorQuery = query(CalendarContract.Events.CONTENT_URI, new String[]{"_sync_id"}, "_id=?", new String[]{Long.toString(j)}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        string = cursorQuery.getString(0);
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return string;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private Cursor getColorByTypeIndex(String str, String str2, long j, String str3) {
        return this.mDb.query("Colors", COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=? AND color_index=?", new String[]{str, str2, Long.toString(j), str3}, null, null, null);
    }

    private String getOwner(long j) throws Throwable {
        Cursor cursorQuery;
        if (j < 0) {
            if (Log.isLoggable("CalendarProvider2", 6)) {
                Log.e("CalendarProvider2", "Calendar Id is not valid: " + j);
            }
            return null;
        }
        try {
            cursorQuery = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, j), new String[]{"ownerAccount"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        String string = cursorQuery.getString(0);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Couldn't find " + j + " in Calendars table");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private Account getAccount(long j) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, j), ACCOUNT_PROJECTION, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        Account account = new Account(cursorQuery.getString(0), cursorQuery.getString(1));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return account;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Couldn't find " + j + " in Calendars table");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private void createAttendeeEntry(long j, int i, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("event_id", Long.valueOf(j));
        contentValues.put("attendeeStatus", Integer.valueOf(i));
        contentValues.put("attendeeType", (Integer) 0);
        contentValues.put("attendeeRelationship", (Integer) 1);
        contentValues.put("attendeeEmail", str);
        this.mDbHelper.attendeesInsert(contentValues);
    }

    private void updateEventAttendeeStatus(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) throws Throwable {
        Cursor cursorQuery;
        Cursor cursorQuery2;
        Long asLong = contentValues.getAsLong("event_id");
        if (asLong == null) {
            Log.w("CalendarProvider2", "Attendee update values don't include an event_id");
            return;
        }
        long jLongValue = asLong.longValue();
        try {
            cursorQuery = query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, jLongValue), new String[]{"calendar_id"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        long j = cursorQuery.getLong(0);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        try {
                            cursorQuery2 = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, j), new String[]{"ownerAccount"}, null, null, null);
                            if (cursorQuery2 != null) {
                                try {
                                    if (cursorQuery2.moveToFirst()) {
                                        String string = cursorQuery2.getString(0);
                                        if (cursorQuery2 != null) {
                                            cursorQuery2.close();
                                        }
                                        if (string == null) {
                                            return;
                                        }
                                        if (string.equals(contentValues.containsKey("attendeeEmail") ? contentValues.getAsString("attendeeEmail") : null)) {
                                            Integer asInteger = contentValues.getAsInteger("attendeeRelationship");
                                            int iIntValue = (asInteger == null || asInteger.intValue() != 2) ? 0 : 1;
                                            Integer asInteger2 = contentValues.getAsInteger("attendeeStatus");
                                            if (asInteger2 != null) {
                                                iIntValue = asInteger2.intValue();
                                            }
                                            ContentValues contentValues2 = new ContentValues();
                                            contentValues2.put("selfAttendeeStatus", Integer.valueOf(iIntValue));
                                            sQLiteDatabase.update("Events", contentValues2, "_id=?", new String[]{String.valueOf(jLongValue)});
                                            return;
                                        }
                                        return;
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    if (cursorQuery2 != null) {
                                        cursorQuery2.close();
                                    }
                                    throw th;
                                }
                            }
                            if (Log.isLoggable("CalendarProvider2", 3)) {
                                Log.d("CalendarProvider2", "Couldn't find " + j + " in Calendars table");
                            }
                            if (cursorQuery2 != null) {
                                cursorQuery2.close();
                                return;
                            }
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            cursorQuery2 = null;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (Log.isLoggable("CalendarProvider2", 3)) {
                Log.d("CalendarProvider2", "Couldn't find " + jLongValue + " in Events table");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th4) {
            th = th4;
            cursorQuery = null;
        }
    }

    private void setHasAlarm(long j, int i) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("hasAlarm", Integer.valueOf(i));
        int iUpdate = this.mDb.update("Events", contentValues, "_id=?", new String[]{String.valueOf(j)});
        if (iUpdate != 1) {
            Log.w("CalendarProvider2", "setHasAlarm on event " + j + " updated " + iUpdate + " rows (expected 1)");
        }
    }

    long calculateLastDate(ContentValues contentValues) throws DateException {
        if (!contentValues.containsKey("dtstart")) {
            if (contentValues.containsKey("dtend") || contentValues.containsKey("rrule") || contentValues.containsKey("duration") || contentValues.containsKey("eventTimezone") || contentValues.containsKey("rdate") || contentValues.containsKey("exrule") || contentValues.containsKey("exdate")) {
                throw new RuntimeException("DTSTART field missing from event");
            }
            return -1L;
        }
        long jLongValue = contentValues.getAsLong("dtstart").longValue();
        Long asLong = contentValues.getAsLong("dtend");
        if (asLong != null) {
            return asLong.longValue();
        }
        Duration duration = new Duration();
        String asString = contentValues.getAsString("duration");
        if (asString != null) {
            duration.parse(asString);
        }
        try {
            RecurrenceSet recurrenceSet = new RecurrenceSet(contentValues);
            if (recurrenceSet.hasRecurrence()) {
                String asString2 = contentValues.getAsString("eventTimezone");
                if (TextUtils.isEmpty(asString2)) {
                    asString2 = "UTC";
                }
                Time time = new Time(asString2);
                time.set(jLongValue);
                jLongValue = new RecurrenceProcessor().getLastOccurence(time, recurrenceSet);
                if (jLongValue == -1) {
                    return jLongValue;
                }
            }
            return duration.addTo(jLongValue);
        } catch (EventRecurrence.InvalidFormatException e) {
            if (Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "Could not parse RRULE recurrence string: " + contentValues.get("rrule"), e);
            }
            return -1L;
        }
    }

    private ContentValues updateLastDate(ContentValues contentValues) {
        try {
            long jCalculateLastDate = calculateLastDate(contentValues);
            if (jCalculateLastDate != -1) {
                contentValues.put("lastDate", Long.valueOf(jCalculateLastDate));
            }
            return contentValues;
        } catch (DateException e) {
            if (Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "Could not calculate last date.", e);
                return null;
            }
            return null;
        }
    }

    private void updateEventRawTimesLocked(long j, ContentValues contentValues) {
        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("event_id", Long.valueOf(j));
        String asString = contentValues.getAsString("eventTimezone");
        Integer asInteger = contentValues.getAsInteger("allDay");
        boolean z = (asInteger == null || asInteger.intValue() == 0) ? false : true;
        if (z || TextUtils.isEmpty(asString)) {
            asString = "UTC";
        }
        Time time = new Time(asString);
        time.allDay = z;
        Long asLong = contentValues.getAsLong("dtstart");
        if (asLong != null) {
            time.set(asLong.longValue());
            contentValues2.put("dtstart2445", time.format2445());
        }
        Long asLong2 = contentValues.getAsLong("dtend");
        if (asLong2 != null) {
            time.set(asLong2.longValue());
            contentValues2.put("dtend2445", time.format2445());
        }
        Long asLong3 = contentValues.getAsLong("originalInstanceTime");
        if (asLong3 != null) {
            Integer asInteger2 = contentValues.getAsInteger("originalAllDay");
            if (asInteger2 != null) {
                time.allDay = asInteger2.intValue() != 0;
            }
            time.set(asLong3.longValue());
            contentValues2.put("originalInstanceTime2445", time.format2445());
        }
        Long asLong4 = contentValues.getAsLong("lastDate");
        if (asLong4 != null) {
            time.allDay = z;
            time.set(asLong4.longValue());
            contentValues2.put("lastDate2445", time.format2445());
        }
        this.mDbHelper.eventsRawTimesReplace(contentValues2);
    }

    @Override
    protected int deleteInTransaction(Uri uri, String str, String[] strArr, boolean z) {
        String str2;
        String string = str;
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "deleteInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int iMatch = sUriMatcher.match(uri);
        verifyTransactionAllowed(3, uri, null, z, iMatch, string, strArr);
        this.mDb = this.mDbHelper.getWritableDatabase();
        if (iMatch != 20 && iMatch != 28) {
            if (iMatch == 30) {
                List<String> pathSegments = uri.getPathSegments();
                Long.parseLong(pathSegments.get(1));
                return deleteEventInternal(Long.parseLong(pathSegments.get(2)), z, false);
            }
            if (iMatch == 32) {
                return deleteMatchingColors(appendAccountToSelection(uri, string, "account_name", "account_type"), strArr);
            }
            switch (iMatch) {
                case 1:
                    Cursor cursorQuery = this.mDb.query("view_events", ID_ONLY_PROJECTION, appendAccountToSelection(uri, string, "account_name", "account_type"), strArr, null, null, null);
                    int iDeleteEventInternal = 0;
                    while (cursorQuery.moveToNext()) {
                        try {
                            iDeleteEventInternal += deleteEventInternal(cursorQuery.getLong(0), z, true);
                        } finally {
                            cursorQuery.close();
                        }
                    }
                    this.mCalendarAlarm.scheduleNextAlarm(false);
                    sendUpdateNotification(z);
                    return iDeleteEventInternal;
                case 2:
                    return deleteEventInternal(ContentUris.parseId(uri), z, false);
                case 3:
                    break;
                case 4:
                    return deleteMatchingCalendars(appendAccountToSelection(uri, string, "account_name", "account_type"), strArr);
                case 5:
                    StringBuilder sb = new StringBuilder("_id=");
                    sb.append(uri.getPathSegments().get(1));
                    if (!TextUtils.isEmpty(str)) {
                        sb.append(" AND (");
                        sb.append(string);
                        sb.append(')');
                    }
                    string = sb.toString();
                    return deleteMatchingCalendars(appendAccountToSelection(uri, string, "account_name", "account_type"), strArr);
                case 6:
                    if (z) {
                        return this.mDb.delete("Attendees", string, strArr);
                    }
                    return deleteFromEventRelatedTable("Attendees", uri, string, strArr);
                case 7:
                    if (z) {
                        return this.mDb.delete("Attendees", "_id=?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                    }
                    return deleteFromEventRelatedTable("Attendees", uri, null, null);
                case 8:
                    return deleteReminders(uri, false, string, strArr, z);
                case 9:
                    return deleteReminders(uri, true, null, null, z);
                case 10:
                    if (z) {
                        return this.mDb.delete("ExtendedProperties", string, strArr);
                    }
                    return deleteFromEventRelatedTable("ExtendedProperties", uri, string, strArr);
                case 11:
                    if (z) {
                        return this.mDb.delete("ExtendedProperties", "_id=?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                    }
                    return deleteFromEventRelatedTable("ExtendedProperties", uri, null, null);
                case 12:
                    if (z) {
                        return this.mDb.delete("CalendarAlerts", string, strArr);
                    }
                    return deleteFromEventRelatedTable("CalendarAlerts", uri, string, strArr);
                case 13:
                    return this.mDb.delete("CalendarAlerts", "_id=?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                default:
                    switch (iMatch) {
                        case 15:
                            break;
                        case 16:
                            return this.mDbHelper.getSyncState().delete(this.mDb, string, strArr);
                        case 17:
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("_id=?");
                            if (string == null) {
                                str2 = "";
                            } else {
                                str2 = " AND (" + string + ")";
                            }
                            sb2.append(str2);
                            return this.mDbHelper.getSyncState().delete(this.mDb, sb2.toString(), insertSelectionArg(strArr, String.valueOf(ContentUris.parseId(uri))));
                        default:
                            throw new IllegalArgumentException("Unknown URL " + uri);
                    }
                    break;
            }
        }
        throw new UnsupportedOperationException("Cannot delete that URL");
    }

    private int deleteEventInternal(long j, boolean z, boolean z2) {
        int i = 1;
        String[] strArr = {String.valueOf(j)};
        Cursor cursorQuery = this.mDb.query("Events", EVENTS_PROJECTION, "_id=?", strArr, null, null, null);
        try {
            if (cursorQuery.moveToNext()) {
                boolean zIsEmpty = TextUtils.isEmpty(cursorQuery.getString(0));
                String string = cursorQuery.getString(1);
                String string2 = cursorQuery.getString(2);
                if (isRecurrenceEvent(string, string2, cursorQuery.getString(3), cursorQuery.getString(4))) {
                    this.mMetaData.clearInstanceRange();
                }
                boolean z3 = (TextUtils.isEmpty(string) && TextUtils.isEmpty(string2)) ? false : true;
                if (z || zIsEmpty) {
                    this.mDb.delete("Events", "_id=?", strArr);
                    if (z3 && zIsEmpty) {
                        this.mDb.delete("Events", "original_id=?", strArr);
                    }
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("deleted", (Integer) 1);
                    contentValues.put("dirty", (Integer) 1);
                    addMutator(contentValues, "mutators");
                    this.mDb.update("Events", contentValues, "_id=?", strArr);
                    this.mDb.delete("Events", "original_id=? AND _sync_id IS NULL", strArr);
                    this.mDb.delete("Instances", "event_id=?", strArr);
                    this.mDb.delete("EventsRawTimes", "event_id=?", strArr);
                    this.mDb.delete("Reminders", "event_id=?", strArr);
                    this.mDb.delete("CalendarAlerts", "event_id=?", strArr);
                    this.mDb.delete("ExtendedProperties", "event_id=?", strArr);
                }
            } else {
                i = 0;
            }
            if (!z2) {
                this.mCalendarAlarm.scheduleNextAlarm(false);
                sendUpdateNotification(z);
            }
            return i;
        } finally {
            cursorQuery.close();
        }
    }

    private int deleteFromEventRelatedTable(String str, Uri uri, String str2, String[] strArr) {
        if (str.equals("Events")) {
            throw new IllegalArgumentException("Don't delete Events with this method (use deleteEventInternal)");
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("dirty", "1");
        addMutator(contentValues, "mutators");
        Cursor cursorQuery = query(uri, ID_PROJECTION, str2, strArr, "event_id");
        long j = -1;
        int i = 0;
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                long j3 = cursorQuery.getLong(1);
                if (j3 != j) {
                    this.mDbHelper.duplicateEvent(j3);
                }
                this.mDb.delete(str, "_id=?", new String[]{String.valueOf(j2)});
                if (j3 != j) {
                    this.mDb.update("Events", contentValues, "_id=?", new String[]{String.valueOf(j3)});
                }
                i++;
                j = j3;
            } finally {
                cursorQuery.close();
            }
        }
        return i;
    }

    private int deleteReminders(Uri uri, boolean z, String str, String[] strArr, boolean z2) {
        long id;
        String str2;
        String[] strArr2;
        if (z) {
            if (!TextUtils.isEmpty(str)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            id = ContentUris.parseId(uri);
            if (id < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
        } else {
            id = -1;
        }
        long j = id;
        HashSet hashSet = new HashSet();
        Cursor cursorQuery = query(uri, new String[]{"event_id"}, str, strArr, null);
        while (cursorQuery.moveToNext()) {
            try {
                hashSet.add(Long.valueOf(cursorQuery.getLong(0)));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        if (!z2) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("dirty", "1");
            addMutator(contentValues, "mutators");
            Iterator it = hashSet.iterator();
            while (it.hasNext()) {
                long jLongValue = ((Long) it.next()).longValue();
                this.mDbHelper.duplicateEvent(jLongValue);
                this.mDb.update("Events", contentValues, "_id=?", new String[]{String.valueOf(jLongValue)});
            }
        }
        if (z) {
            str2 = "_id=?";
            strArr2 = new String[]{String.valueOf(j)};
        } else {
            str2 = str;
            strArr2 = strArr;
        }
        int iDelete = this.mDb.delete("Reminders", str2, strArr2);
        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("hasAlarm", (Integer) 0);
        Iterator it2 = hashSet.iterator();
        while (it2.hasNext()) {
            long jLongValue2 = ((Long) it2.next()).longValue();
            Cursor cursorQuery2 = this.mDb.query("Reminders", new String[]{"_id"}, "event_id=?", new String[]{String.valueOf(jLongValue2)}, null, null, null);
            int count = cursorQuery2.getCount();
            cursorQuery2.close();
            if (count == 0) {
                this.mDb.update("Events", contentValues2, "_id=?", new String[]{String.valueOf(jLongValue2)});
            }
        }
        this.mCalendarAlarm.scheduleNextAlarm(false);
        return iDelete;
    }

    private int updateEventRelatedTable(Uri uri, String str, boolean z, ContentValues contentValues, String str2, String[] strArr, boolean z2) {
        String str3;
        String[] strArr2;
        boolean z3;
        boolean z4;
        if (z) {
            if (!TextUtils.isEmpty(str2)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            long id = ContentUris.parseId(uri);
            if (id < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
            str3 = "_id=?";
            strArr2 = new String[]{String.valueOf(id)};
        } else {
            if (TextUtils.isEmpty(str2)) {
                throw new UnsupportedOperationException("Selection is required for " + uri);
            }
            str3 = str2;
            strArr2 = strArr;
        }
        Cursor cursorQuery = this.mDb.query(str, null, str3, strArr2, null, null, null);
        try {
            if (cursorQuery.getCount() == 0) {
                Log.d("CalendarProvider2", "No query results for " + uri + ", selection=" + str3 + " selectionArgs=" + Arrays.toString(strArr2));
                return 0;
            }
            ContentValues contentValues2 = null;
            if (!z2) {
                contentValues2 = new ContentValues();
                contentValues2.put("dirty", "1");
                addMutator(contentValues2, "mutators");
            }
            int columnIndex = cursorQuery.getColumnIndex("_id");
            int columnIndex2 = cursorQuery.getColumnIndex("event_id");
            if (columnIndex < 0 || columnIndex2 < 0) {
                throw new RuntimeException("Lookup on _id/event_id failed for " + uri);
            }
            int i = 0;
            while (cursorQuery.moveToNext()) {
                ContentValues contentValues3 = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorQuery, contentValues3);
                contentValues3.putAll(contentValues);
                long j = cursorQuery.getLong(columnIndex);
                long j2 = cursorQuery.getLong(columnIndex2);
                if (!z2) {
                    this.mDbHelper.duplicateEvent(j2);
                }
                int i2 = columnIndex;
                int i3 = columnIndex2;
                this.mDb.update(str, contentValues3, "_id=?", new String[]{String.valueOf(j)});
                if (!z2) {
                    z3 = true;
                    z4 = false;
                    this.mDb.update("Events", contentValues2, "_id=?", new String[]{String.valueOf(j2)});
                } else {
                    z3 = true;
                    z4 = false;
                }
                i++;
                if (str.equals("Attendees")) {
                    updateEventAttendeeStatus(this.mDb, contentValues3);
                    sendUpdateNotification(j2, z2);
                }
                columnIndex = i2;
                columnIndex2 = i3;
            }
            return i;
        } finally {
            cursorQuery.close();
        }
    }

    private int deleteMatchingColors(String str, String[] strArr) {
        Cursor cursorQuery;
        Cursor cursorQuery2 = this.mDb.query("Colors", COLORS_PROJECTION, str, strArr, null, null, null);
        if (cursorQuery2 == null) {
            return 0;
        }
        Cursor cursor = null;
        while (cursorQuery2.moveToNext()) {
            try {
                String string = cursorQuery2.getString(3);
                String string2 = cursorQuery2.getString(0);
                String string3 = cursorQuery2.getString(1);
                if (cursorQuery2.getInt(2) == 0) {
                    try {
                        cursorQuery = this.mDb.query("Calendars", ID_ONLY_PROJECTION, "account_name=? AND account_type=? AND calendar_color_index=?", new String[]{string2, string3, string}, null, null, null);
                    } catch (Throwable th) {
                        th = th;
                        cursorQuery = cursor;
                    }
                    try {
                        if (cursorQuery.getCount() != 0) {
                            throw new UnsupportedOperationException("Cannot delete color " + string + ". Referenced by " + cursorQuery.getCount() + " calendars.");
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                } else {
                    cursorQuery = query(CalendarContract.Events.CONTENT_URI, ID_ONLY_PROJECTION, "calendar_id in (SELECT _id from Calendars WHERE account_name=? AND account_type=?) AND eventColor_index=?", new String[]{string2, string3, string}, null);
                    if (cursorQuery.getCount() != 0) {
                        throw new UnsupportedOperationException("Cannot delete color " + string + ". Referenced by " + cursorQuery.getCount() + " events.");
                    }
                }
                cursor = cursorQuery;
                if (cursor != null) {
                    cursor.close();
                }
            } finally {
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            }
        }
        return this.mDb.delete("Colors", str, strArr);
    }

    private int deleteMatchingCalendars(String str, String[] strArr) {
        Cursor cursorQuery = this.mDb.query("Calendars", sCalendarsIdProjection, str, strArr, null, null, null);
        if (cursorQuery == null) {
            return 0;
        }
        while (cursorQuery.moveToNext()) {
            try {
                modifyCalendarSubscription(cursorQuery.getLong(0), false);
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return this.mDb.delete("Calendars", str, strArr);
    }

    private boolean doesEventExistForSyncId(String str) {
        if (str != null) {
            return DatabaseUtils.longForQuery(this.mDb, "SELECT COUNT(*) FROM Events WHERE _sync_id=?", new String[]{str}) > 0;
        }
        if (Log.isLoggable("CalendarProvider2", 5)) {
            Log.w("CalendarProvider2", "SyncID cannot be null: " + str);
        }
        return false;
    }

    private boolean doesStatusCancelUpdateMeanUpdate(ContentValues contentValues, ContentValues contentValues2) {
        boolean z;
        if (!contentValues2.containsKey("eventStatus") || contentValues2.getAsInteger("eventStatus").intValue() != 2) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            String asString = contentValues.getAsString("original_sync_id");
            if (!TextUtils.isEmpty(asString)) {
                return doesEventExistForSyncId(asString);
            }
        }
        return true;
    }

    private int handleUpdateColors(ContentValues contentValues, String str, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        Throwable th;
        int iUpdate = this.mDb.update("Colors", contentValues, str, strArr);
        if (contentValues.containsKey("color")) {
            try {
                cursorQuery = this.mDb.query("Colors", COLORS_PROJECTION, str, strArr, null, null, null);
                while (cursorQuery.moveToNext()) {
                    try {
                        boolean z = cursorQuery.getInt(2) == 0;
                        int i = cursorQuery.getInt(4);
                        String[] strArr2 = {cursorQuery.getString(0), cursorQuery.getString(1), cursorQuery.getString(3)};
                        ContentValues contentValues2 = new ContentValues();
                        if (z) {
                            contentValues2.put("calendar_color", Integer.valueOf(i));
                            this.mDb.update("Calendars", contentValues2, "account_name=? AND account_type=? AND calendar_color_index=?", strArr2);
                        } else {
                            contentValues2.put("eventColor", Integer.valueOf(i));
                            this.mDb.update("Events", contentValues2, "calendar_id in (SELECT _id from Calendars WHERE account_name=? AND account_type=?) AND eventColor_index=?", strArr2);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th3) {
                cursorQuery = null;
                th = th3;
            }
        }
        return iUpdate;
    }

    private int handleUpdateEvents(Cursor cursor, ContentValues contentValues, boolean z) throws Throwable {
        boolean z2;
        long j;
        String string;
        String string2;
        contentValues.remove("hasAlarm");
        if (cursor.getCount() > 1 && Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "Performing update on " + cursor.getCount() + " events");
        }
        while (cursor.moveToNext()) {
            ContentValues contentValues2 = new ContentValues(contentValues);
            ContentValues contentValues3 = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, contentValues3);
            if (!z) {
                try {
                    validateEventData(contentValues3);
                    z2 = true;
                } catch (IllegalArgumentException e) {
                    Log.d("CalendarProvider2", "Event " + contentValues3.getAsString("_id") + " malformed, not validating update (" + e.getMessage() + ")");
                    z2 = false;
                }
            } else {
                z2 = false;
            }
            contentValues3.putAll(contentValues2);
            String asString = contentValues2.getAsString("eventColor_index");
            if (!TextUtils.isEmpty(asString)) {
                Cursor cursorQuery = this.mDb.query("Calendars", ACCOUNT_PROJECTION, "_id=?", new String[]{contentValues3.getAsString("calendar_id")}, null, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        string = cursorQuery.getString(0);
                        string2 = cursorQuery.getString(1);
                    } else {
                        string = null;
                        string2 = null;
                    }
                    verifyColorExists(string, string2, asString, 1);
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            if (z) {
                scrubEventData(contentValues3, contentValues2);
            }
            if (z2) {
                validateEventData(contentValues3);
            }
            if (contentValues2.containsKey("dtstart") || contentValues2.containsKey("dtend") || contentValues2.containsKey("duration") || contentValues2.containsKey("eventTimezone") || contentValues2.containsKey("rrule") || contentValues2.containsKey("rdate") || contentValues2.containsKey("exrule") || contentValues2.containsKey("exdate")) {
                try {
                    long jCalculateLastDate = calculateLastDate(contentValues3);
                    Long asLong = contentValues3.getAsLong("lastDate");
                    if ((asLong == null ? -1L : asLong.longValue()) != jCalculateLastDate) {
                        if (jCalculateLastDate < 0) {
                            contentValues2.putNull("lastDate");
                        } else {
                            contentValues2.put("lastDate", Long.valueOf(jCalculateLastDate));
                        }
                    }
                } catch (DateException e2) {
                    throw new IllegalArgumentException("Unable to compute LAST_DATE", e2);
                }
            }
            if (!z) {
                contentValues2.put("dirty", (Integer) 1);
                addMutator(contentValues2, "mutators");
            }
            if (contentValues2.containsKey("selfAttendeeStatus")) {
                throw new IllegalArgumentException("Updating selfAttendeeStatus in Events table is not allowed.");
            }
            if (fixAllDayTime(contentValues3, contentValues2) && Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "handleUpdateEvents: allDay is true but sec, min, hour were not 0.");
            }
            boolean zDoesStatusCancelUpdateMeanUpdate = doesStatusCancelUpdateMeanUpdate(contentValues3, contentValues2);
            long jLongValue = contentValues3.getAsLong("_id").longValue();
            if (!zDoesStatusCancelUpdateMeanUpdate) {
                deleteEventInternal(jLongValue, z, true);
                this.mCalendarAlarm.scheduleNextAlarm(false);
                sendUpdateNotification(z);
            } else {
                contentValues2.put("modifyTime", Long.valueOf(System.currentTimeMillis()));
                limitTextLength(contentValues2);
                if (!z) {
                    this.mDbHelper.duplicateEvent(jLongValue);
                } else if (contentValues2.containsKey("dirty") && contentValues2.getAsInteger("dirty").intValue() == 0) {
                    contentValues2.put("mutators", (String) null);
                    this.mDbHelper.removeDuplicateEvent(jLongValue);
                }
                if (this.mDb.update("Events", contentValues2, "_id=?", new String[]{String.valueOf(jLongValue)}) > 0) {
                    updateEventRawTimesLocked(jLongValue, contentValues2);
                    this.mInstancesHelper.updateInstancesLocked(contentValues2, jLongValue, false, this.mDb);
                    if (contentValues2.containsKey("dtstart") || contentValues2.containsKey("eventStatus")) {
                        if (contentValues2.containsKey("eventStatus") && contentValues2.getAsInteger("eventStatus").intValue() == 2) {
                            j = jLongValue;
                            this.mDb.delete("Instances", "event_id=?", new String[]{String.valueOf(j)});
                        } else {
                            j = jLongValue;
                        }
                        if (Log.isLoggable("CalendarProvider2", 3)) {
                            Log.d("CalendarProvider2", "updateInternal() changing event");
                        }
                        this.mCalendarAlarm.scheduleNextAlarm(false);
                    } else {
                        j = jLongValue;
                    }
                    sendUpdateNotification(j, z);
                }
            }
        }
        return cursor.getCount();
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z) throws Throwable {
        Cursor cursorQuery;
        long id;
        Account account;
        String str2;
        if (Log.isLoggable("CalendarProvider2", 2)) {
            Log.v("CalendarProvider2", "updateInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        int iMatch = sUriMatcher.match(uri);
        verifyTransactionAllowed(2, uri, contentValues, z, iMatch, str, strArr);
        this.mDb = this.mDbHelper.getWritableDatabase();
        if (iMatch == 22) {
            this.mCalendarAlarm.scheduleNextAlarm(true);
            return 0;
        }
        if (iMatch == 28) {
            if (!str.equals("key=?")) {
                throw new UnsupportedOperationException("Selection should be key=? for " + uri);
            }
            List listAsList = Arrays.asList(strArr);
            if (listAsList.contains("timezoneInstancesPrevious")) {
                throw new UnsupportedOperationException("Invalid selection key: timezoneInstancesPrevious for " + uri);
            }
            String timezoneInstances = this.mCalendarCache.readTimezoneInstances();
            int iUpdate = this.mDb.update("CalendarCache", contentValues, str, strArr);
            if (iUpdate > 0) {
                if (listAsList.contains("timezoneType")) {
                    String asString = contentValues.getAsString("value");
                    if (asString != null) {
                        if (asString.equals("home")) {
                            String timezoneInstancesPrevious = this.mCalendarCache.readTimezoneInstancesPrevious();
                            if (timezoneInstancesPrevious != null) {
                                this.mCalendarCache.writeTimezoneInstances(timezoneInstancesPrevious);
                            }
                            if (!timezoneInstances.equals(timezoneInstancesPrevious)) {
                                regenerateInstancesTable();
                                sendUpdateNotification(z);
                            }
                        } else if (asString.equals("auto")) {
                            String id2 = TimeZone.getDefault().getID();
                            this.mCalendarCache.writeTimezoneInstances(id2);
                            if (!timezoneInstances.equals(id2)) {
                                regenerateInstancesTable();
                                sendUpdateNotification(z);
                            }
                        }
                    }
                } else if (listAsList.contains("timezoneInstances") && isHomeTimezone()) {
                    String timezoneInstances2 = this.mCalendarCache.readTimezoneInstances();
                    this.mCalendarCache.writeTimezoneInstancesPrevious(timezoneInstances2);
                    if (timezoneInstances != null && !timezoneInstances.equals(timezoneInstances2)) {
                        regenerateInstancesTable();
                        sendUpdateNotification(z);
                    }
                }
            }
            return iUpdate;
        }
        if (iMatch == 32) {
            int i = 0;
            if (contentValues.getAsInteger("color") != null) {
                i = 1;
            }
            if (contentValues.getAsString("data") != null) {
                i++;
            }
            if (contentValues.size() == i) {
                return handleUpdateColors(contentValues, appendAccountToSelection(uri, str, "account_name", "account_type"), strArr);
            }
            throw new UnsupportedOperationException("You may only change the COLOR and DATA columns for an existing Colors entry.");
        }
        switch (iMatch) {
            case 1:
            case 2:
                try {
                    cursorQuery = iMatch == 2 ? this.mDb.query("Events", null, "_id=?", new String[]{String.valueOf(ContentUris.parseId(uri))}, null, null, null) : this.mDb.query("Events", null, str, strArr, null, null, null);
                } catch (Throwable th) {
                    th = th;
                    cursorQuery = null;
                }
                try {
                    if (cursorQuery.getCount() != 0) {
                        int iHandleUpdateEvents = handleUpdateEvents(cursorQuery, contentValues, z);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return iHandleUpdateEvents;
                    }
                    Log.i("CalendarProvider2", "No events to update: uri=" + uri + " selection=" + str + " selectionArgs=" + Arrays.toString(strArr));
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return 0;
                } catch (Throwable th2) {
                    th = th2;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            default:
                switch (iMatch) {
                    case 4:
                    case 5:
                        if (iMatch == 5) {
                            id = ContentUris.parseId(uri);
                        } else if (str != null && TextUtils.equals(str, "_id=?")) {
                            id = Long.parseLong(strArr[0]);
                        } else {
                            if (str == null || !str.startsWith("_id=")) {
                                return this.mDb.update("Calendars", contentValues, str, strArr);
                            }
                            id = Long.parseLong(str.substring(4));
                        }
                        if (!z) {
                            contentValues.put("dirty", (Integer) 1);
                            addMutator(contentValues, "mutators");
                        } else if (contentValues.containsKey("dirty") && contentValues.getAsInteger("dirty").intValue() == 0) {
                            contentValues.put("mutators", (String) null);
                        }
                        Integer asInteger = contentValues.getAsInteger("sync_events");
                        if (asInteger != null) {
                            modifyCalendarSubscription(id, asInteger.intValue() == 1);
                        }
                        String asString2 = contentValues.getAsString("calendar_color_index");
                        if (!TextUtils.isEmpty(asString2)) {
                            String asString3 = contentValues.getAsString("account_name");
                            String asString4 = contentValues.getAsString("account_type");
                            if ((TextUtils.isEmpty(asString3) || TextUtils.isEmpty(asString4)) && (account = getAccount(id)) != null) {
                                asString3 = account.name;
                                asString4 = account.type;
                            }
                            verifyColorExists(asString3, asString4, asString2, 0);
                        }
                        int iUpdate2 = this.mDb.update("Calendars", contentValues, "_id=?", new String[]{String.valueOf(id)});
                        if (iUpdate2 > 0) {
                            if (contentValues.containsKey("visible")) {
                                this.mCalendarAlarm.scheduleNextAlarm(false);
                            }
                            sendUpdateNotification(z);
                        }
                        return iUpdate2;
                    case 6:
                        return updateEventRelatedTable(uri, "Attendees", false, contentValues, str, strArr, z);
                    case 7:
                        return updateEventRelatedTable(uri, "Attendees", true, contentValues, null, null, z);
                    case 8:
                        return updateEventRelatedTable(uri, "Reminders", false, contentValues, str, strArr, z);
                    case 9:
                        int iUpdateEventRelatedTable = updateEventRelatedTable(uri, "Reminders", true, contentValues, null, null, z);
                        if (Log.isLoggable("CalendarProvider2", 3)) {
                            Log.d("CalendarProvider2", "updateInternal() changing reminder");
                        }
                        this.mCalendarAlarm.scheduleNextAlarm(false);
                        return iUpdateEventRelatedTable;
                    default:
                        switch (iMatch) {
                            case 11:
                                return updateEventRelatedTable(uri, "ExtendedProperties", true, contentValues, null, null, z);
                            case 12:
                                return this.mDb.update("CalendarAlerts", contentValues, str, strArr);
                            case 13:
                                return this.mDb.update("CalendarAlerts", contentValues, "_id=?", new String[]{String.valueOf(ContentUris.parseId(uri))});
                            default:
                                switch (iMatch) {
                                    case 16:
                                        return this.mDbHelper.getSyncState().update(this.mDb, contentValues, appendAccountToSelection(uri, str, "account_name", "account_type"), strArr);
                                    case 17:
                                        String strAppendAccountToSelection = appendAccountToSelection(uri, str, "account_name", "account_type");
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("_id=?");
                                        if (strAppendAccountToSelection == null) {
                                            str2 = "";
                                        } else {
                                            str2 = " AND (" + strAppendAccountToSelection + ")";
                                        }
                                        sb.append(str2);
                                        return this.mDbHelper.getSyncState().update(this.mDb, contentValues, sb.toString(), insertSelectionArg(strArr, String.valueOf(ContentUris.parseId(uri))));
                                    default:
                                        throw new IllegalArgumentException("Unknown URL " + uri);
                                }
                        }
                }
        }
    }

    private int verifyColorExists(String str, String str2, String str3, int i) throws Throwable {
        Cursor colorByTypeIndex;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            throw new IllegalArgumentException("Cannot set color. A valid account does not exist for this calendar.");
        }
        try {
            colorByTypeIndex = getColorByTypeIndex(str, str2, i, str3);
            try {
                if (!colorByTypeIndex.moveToFirst()) {
                    throw new IllegalArgumentException("Color type: " + i + " and index " + str3 + " does not exist for account.");
                }
                int i2 = colorByTypeIndex.getInt(4);
                if (colorByTypeIndex != null) {
                    colorByTypeIndex.close();
                }
                return i2;
            } catch (Throwable th) {
                th = th;
                if (colorByTypeIndex != null) {
                    colorByTypeIndex.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            colorByTypeIndex = null;
        }
    }

    private String appendLastSyncedColumnToSelection(String str, Uri uri) {
        if (getIsCallerSyncAdapter(uri)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("lastSynced");
        sb.append(" = 0");
        return appendSelection(sb, str);
    }

    private String appendAccountToSelection(Uri uri, String str, String str2, String str3) {
        String queryParameter = QueryParameterUtils.getQueryParameter(uri, "account_name");
        String queryParameter2 = QueryParameterUtils.getQueryParameter(uri, "account_type");
        if (!TextUtils.isEmpty(queryParameter)) {
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append("=");
            sb.append(DatabaseUtils.sqlEscapeString(queryParameter));
            sb.append(" AND ");
            sb.append(str3);
            sb.append("=");
            sb.append(DatabaseUtils.sqlEscapeString(queryParameter2));
            return appendSelection(sb, str);
        }
        return str;
    }

    private String appendSelection(StringBuilder sb, String str) {
        if (!TextUtils.isEmpty(str)) {
            sb.append(" AND (");
            sb.append(str);
            sb.append(')');
        }
        return sb.toString();
    }

    private void verifyTransactionAllowed(int i, Uri uri, ContentValues contentValues, boolean z, int i2, String str, String[] strArr) {
        if (i == 0) {
            return;
        }
        if (i == 2 || i == 3) {
            if (!TextUtils.isEmpty(str)) {
                if (i2 != 1 && i2 != 4 && i2 != 6 && i2 != 8 && i2 != 10 && i2 != 12 && i2 != 16 && i2 != 28 && i2 != 32) {
                    throw new IllegalArgumentException("Selection not permitted for " + uri);
                }
            } else if (i2 == 1 || i2 == 6 || i2 == 8 || i2 == 28) {
                throw new IllegalArgumentException("Selection must be specified for " + uri);
            }
        }
        if (!z) {
            switch (i2) {
                case 10:
                case 11:
                case 16:
                case 17:
                case 32:
                    throw new IllegalArgumentException("Only sync adapters may write using " + uri);
            }
        }
        switch (i) {
            case 1:
                if (i2 == 3) {
                    throw new UnsupportedOperationException("Inserting into instances not supported");
                }
                verifyColumns(contentValues, i2);
                if (z) {
                    verifyHasAccount(uri, str, strArr);
                    return;
                } else {
                    verifyNoSyncColumns(contentValues, i2);
                    return;
                }
            case 2:
                if (i2 == 3) {
                    throw new UnsupportedOperationException("Updating instances not supported");
                }
                verifyColumns(contentValues, i2);
                if (z) {
                    verifyHasAccount(uri, str, strArr);
                    return;
                } else {
                    verifyNoSyncColumns(contentValues, i2);
                    return;
                }
            case 3:
                if (i2 == 3) {
                    throw new UnsupportedOperationException("Deleting instances not supported");
                }
                if (z) {
                    verifyHasAccount(uri, str, strArr);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void verifyHasAccount(Uri uri, String str, String[] strArr) {
        String queryParameter = QueryParameterUtils.getQueryParameter(uri, "account_name");
        String queryParameter2 = QueryParameterUtils.getQueryParameter(uri, "account_type");
        if ((TextUtils.isEmpty(queryParameter) || TextUtils.isEmpty(queryParameter2)) && str != null && str.startsWith("account_name=? AND account_type=?")) {
            queryParameter = strArr[0];
            queryParameter2 = strArr[1];
        }
        if (TextUtils.isEmpty(queryParameter) || TextUtils.isEmpty(queryParameter2)) {
            throw new IllegalArgumentException("Sync adapters must specify an account and account type: " + uri);
        }
    }

    private void verifyColumns(ContentValues contentValues, int i) {
        String[] strArr;
        if (contentValues == null || contentValues.size() == 0) {
            return;
        }
        switch (i) {
            case 1:
            case 2:
            case 18:
            case 19:
                strArr = CalendarContract.Events.PROVIDER_WRITABLE_COLUMNS;
                break;
            default:
                strArr = PROVIDER_WRITABLE_DEFAULT_COLUMNS;
                break;
        }
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (contentValues.containsKey(strArr[i2])) {
                throw new IllegalArgumentException("Only the provider may write to " + strArr[i2]);
            }
        }
    }

    private void verifyNoSyncColumns(ContentValues contentValues, int i) {
        String[] strArr;
        if (contentValues == null || contentValues.size() == 0) {
            return;
        }
        switch (i) {
            case 1:
            case 2:
            case 18:
            case 19:
                strArr = CalendarContract.Events.SYNC_WRITABLE_COLUMNS;
                break;
            case 4:
            case 5:
            case 24:
            case 25:
                strArr = CalendarContract.Calendars.SYNC_WRITABLE_COLUMNS;
                break;
            default:
                strArr = SYNC_WRITABLE_DEFAULT_COLUMNS;
                break;
        }
        for (int i2 = 0; i2 < strArr.length; i2++) {
            if (contentValues.containsKey(strArr[i2])) {
                throw new IllegalArgumentException("Only sync adapters may write to " + strArr[i2]);
            }
        }
    }

    private void modifyCalendarSubscription(long j, boolean z) {
        String string;
        Account account;
        Cursor cursorQuery = query(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, j), new String[]{"account_name", "account_type", "cal_sync1", "sync_events"}, null, null, null);
        boolean z2 = false;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    account = new Account(cursorQuery.getString(0), cursorQuery.getString(1));
                    string = cursorQuery.getString(2);
                    if (cursorQuery.getInt(3) != 0) {
                        z2 = true;
                    }
                } else {
                    string = null;
                    account = null;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        } else {
            string = null;
            account = null;
        }
        if (account == null) {
            if (Log.isLoggable("CalendarProvider2", 5)) {
                Log.w("CalendarProvider2", "Cannot update subscription because account is empty -- should not happen.");
            }
        } else {
            String str = TextUtils.isEmpty(string) ? null : string;
            if (z2 != z) {
                this.mDbHelper.scheduleSync(account, true ^ z, str);
            }
        }
    }

    private void sendUpdateNotification(boolean z) {
        sendUpdateNotification(-1L, z);
    }

    private void sendUpdateNotification(long j, boolean z) {
        long j2;
        if (z) {
            j2 = 5000;
        } else {
            j2 = 1000;
        }
        if (Log.isLoggable("CalendarProvider2", 3)) {
            Log.d("CalendarProvider2", "sendUpdateNotification: delay=" + j2);
        }
        this.mCalendarAlarm.seto(3, SystemClock.elapsedRealtime() + j2, PendingIntent.getBroadcast(this.mContext, 0, createProviderChangedBroadcast(), 134217728));
        Intent intent = new Intent();
        intent.setAction("CalendarProvider2.intent.action.PROVIDER_CHANGED");
        this.mContext.sendBroadcast(intent);
    }

    private Intent createProviderChangedBroadcast() {
        return new Intent("android.intent.action.PROVIDER_CHANGED", CalendarContract.CONTENT_URI).addFlags(536870912);
    }

    @Override
    public void onAccountsUpdated(Account[] accountArr) {
        Log.d("CalendarProvider2", "onAccountsUpdated()");
        new AccountsUpdatedThread(accountArr).start();
    }

    private class AccountsUpdatedThread extends Thread {
        private Account[] mAccounts;

        AccountsUpdatedThread(Account[] accountArr) {
            this.mAccounts = accountArr;
        }

        @Override
        public void run() throws Throwable {
            Process.setThreadPriority(10);
            CalendarProvider2.this.removeStaleAccounts(this.mAccounts);
        }
    }

    private void removeStaleAccounts(Account[] accountArr) throws Throwable {
        Cursor cursorRawQuery;
        this.mDb = this.mDbHelper.getWritableDatabase();
        if (this.mDb == null) {
            return;
        }
        HashSet hashSet = new HashSet();
        for (Account account : accountArr) {
            hashSet.add(new Account(account.name, account.type));
        }
        ArrayList<Account> arrayList = new ArrayList();
        this.mDb.beginTransaction();
        String[] strArr = null;
        try {
            String[] strArr2 = {"Calendars", "Colors"};
            int length = strArr2.length;
            int i = 0;
            while (i < length) {
                cursorRawQuery = this.mDb.rawQuery("SELECT DISTINCT account_name,account_type FROM " + strArr2[i], strArr);
                while (cursorRawQuery.moveToNext()) {
                    try {
                        if (cursorRawQuery.getString(0) != null && cursorRawQuery.getString(1) != null && !TextUtils.equals(cursorRawQuery.getString(1).toUpperCase(), "LOCAL")) {
                            Account account2 = new Account(cursorRawQuery.getString(0), cursorRawQuery.getString(1));
                            if (!hashSet.contains(account2)) {
                                arrayList.add(account2);
                            }
                        } else if (TextUtils.equals(cursorRawQuery.getString(1), "local")) {
                            Log.d("CalendarProvider2", "update account type for 'local' account.");
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("account_type", "LOCAL");
                            if (this.mDb.update("Calendars", contentValues, "account_name=? AND account_type=?", new String[]{cursorRawQuery.getString(0), cursorRawQuery.getString(1)}) == 0) {
                                Log.v("CalendarProvider2", "Could not update Events table with values " + contentValues);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorRawQuery != null) {
                            cursorRawQuery.close();
                        }
                        this.mDb.endTransaction();
                        throw th;
                    }
                }
                cursorRawQuery.close();
                i++;
                strArr = null;
            }
            for (Account account3 : arrayList) {
                if (Log.isLoggable("CalendarProvider2", 3)) {
                    Log.d("CalendarProvider2", "removing data for removed account " + account3);
                }
                Log.d("CalendarProvider2", "removing data for removed account " + account3);
                String[] strArr3 = {account3.name, account3.type};
                this.mDb.execSQL("DELETE FROM Calendars WHERE account_name=? AND account_type=?", strArr3);
                this.mDb.execSQL("DELETE FROM Colors WHERE account_name=? AND account_type=?", strArr3);
            }
            this.mDbHelper.getSyncState().onAccountsChanged(this.mDb, accountArr);
            this.mDb.setTransactionSuccessful();
            this.mDb.endTransaction();
            if (!arrayList.isEmpty()) {
                sendUpdateNotification(false);
            }
        } catch (Throwable th2) {
            th = th2;
            cursorRawQuery = null;
        }
    }

    private String[] insertSelectionArg(String[] strArr, String str) {
        if (strArr == null) {
            return new String[]{str};
        }
        String[] strArr2 = new String[strArr.length + 1];
        strArr2[0] = str;
        System.arraycopy(strArr, 0, strArr2, 1, strArr.length);
        return strArr2;
    }

    private String getCallingPackageName() {
        if (getCachedCallingPackage() != null) {
            return getCachedCallingPackage();
        }
        if (!Boolean.TRUE.equals(this.mCallingPackageErrorLogged.get())) {
            Log.e("CalendarProvider2", "Failed to get the cached calling package.", new Throwable());
            this.mCallingPackageErrorLogged.set(Boolean.TRUE);
        }
        PackageManager packageManager = getContext().getPackageManager();
        int callingUid = Binder.getCallingUid();
        String[] packagesForUid = packageManager.getPackagesForUid(callingUid);
        if (packagesForUid != null && packagesForUid.length == 1) {
            return packagesForUid[0];
        }
        String nameForUid = packageManager.getNameForUid(callingUid);
        if (nameForUid != null) {
            return nameForUid;
        }
        return String.valueOf(callingUid);
    }

    private void addMutator(ContentValues contentValues, String str) {
        String callingPackageName = getCallingPackageName();
        String asString = contentValues.getAsString(str);
        if (TextUtils.isEmpty(asString)) {
            contentValues.put(str, callingPackageName);
            return;
        }
        contentValues.put(str, asString + "," + callingPackageName);
    }

    private void limitTextLength(ContentValues contentValues) {
        limitByColumn(contentValues, "title", 2000);
        limitByColumn(contentValues, "description", 10000);
        limitByColumn(contentValues, "eventLocation", 2000);
    }

    private String limitedString(String str, int i) {
        if (i < " ...".length()) {
            return " ...";
        }
        if (str.length() > i) {
            Log.d("CalendarProvider2", "the string is too long(" + str.length() + "), limit it to " + i);
            return ((Object) str.subSequence(0, i - " ...".length())) + " ...";
        }
        return str;
    }

    private void limitByColumn(ContentValues contentValues, String str, int i) {
        String asString;
        if (contentValues.containsKey(str) && (asString = contentValues.getAsString(str)) != null) {
            contentValues.put(str, limitedString(asString, i));
        }
    }

    private Cursor handleSearchSuggestionQuery(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String str, String str2) {
        Log.v("CalendarProvider2", "handleSearchSuggestionQuery, query = " + str);
        MatrixCursor matrixCursor = new MatrixCursor(SUGGESTIONS_COLUMNS);
        if (TextUtils.isEmpty(str)) {
            return matrixCursor;
        }
        sQLiteQueryBuilder.setTables("view_events");
        sQLiteQueryBuilder.setProjectionMap(sEventsProjectionMap);
        String[] strArr = tokenizeSearchQuery(str);
        String[] strArrConstructSuggestsSearchArgs = constructSuggestsSearchArgs(SEARCH_COLUMNS, strArr);
        String strConstructSearchWhere = constructSearchWhere(strArr);
        Log.v("CalendarProvider2", "handleSearchSuggestionQuery, searchWhere = " + strConstructSearchWhere);
        Cursor cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, SEARCH_PROJECTION, "visible=1 AND selfAttendeeStatus!=2 AND deleted!=1", strArrConstructSuggestsSearchArgs, "_id", strConstructSearchWhere, "_id", str2, null);
        if (cursorQuery != null) {
            try {
                constructSearchSuggesions(matrixCursor, cursorQuery);
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        return matrixCursor;
    }

    private void constructSearchSuggesions(MatrixCursor matrixCursor, Cursor cursor) {
        while (cursor.moveToNext()) {
            long j = cursor.getLong(0);
            String string = cursor.getString(1);
            String string2 = cursor.getString(2);
            String string3 = cursor.getString(3);
            String string4 = cursor.getString(4);
            Log.v("CalendarProvider2", "constructSearchSuggesions title = " + string + ", description = " + string2 + ", location = " + string4);
            ArrayList arrayList = new ArrayList();
            arrayList.add(Long.valueOf(j));
            arrayList.add("0");
            if (!TextUtils.isEmpty(string)) {
                arrayList.add(string);
            } else if (!TextUtils.isEmpty(string2)) {
                arrayList.add(string2);
            } else if (!TextUtils.isEmpty(string4)) {
                arrayList.add(string4);
            } else {
                Log.w("CalendarProvider2", "constructSearchSuggesions SUGGEST_COLUMN_QUERY, title, location and description are empty.");
            }
            if (!TextUtils.isEmpty(string3)) {
                arrayList.add(getContext().getString(R.string.repeated));
            } else {
                arrayList.add(getContext().getString(R.string.one_time));
            }
            arrayList.add(String.valueOf(R.drawable.app_icon));
            if (!TextUtils.isEmpty(string)) {
                arrayList.add(string);
            } else if (!TextUtils.isEmpty(string2)) {
                arrayList.add(string2);
            } else if (!TextUtils.isEmpty(string4)) {
                arrayList.add(string4);
            } else {
                Log.w("CalendarProvider2", "constructSearchSuggesions SUGGEST_COLUMN_QUERY, title, location and description are empty.");
            }
            arrayList.add(Long.valueOf(j));
            matrixCursor.addRow(arrayList);
        }
    }

    private static String[] constructSuggestsSearchArgs(String[] strArr, String[] strArr2) {
        int length = strArr.length;
        String[] strArr3 = new String[strArr2.length * length];
        for (int i = 0; i < strArr2.length; i++) {
            int i2 = length * i;
            for (int i3 = i2; i3 < i2 + length; i3++) {
                strArr3[i3] = "%" + strArr2[i] + "%";
            }
        }
        return strArr3;
    }
}
