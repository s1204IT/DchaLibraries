package com.android.providers.calendar;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.common.content.SyncStateContentProviderHelper;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.TimeZone;

class CalendarDatabaseHelper extends SQLiteOpenHelper {
    private static CalendarDatabaseHelper sSingleton = null;
    private DatabaseUtils.InsertHelper mAttendeesInserter;
    private DatabaseUtils.InsertHelper mCalendarAlertsInserter;
    private DatabaseUtils.InsertHelper mCalendarsInserter;
    private DatabaseUtils.InsertHelper mColorsInserter;
    private DatabaseUtils.InsertHelper mEventsInserter;
    private DatabaseUtils.InsertHelper mEventsRawTimesInserter;
    private DatabaseUtils.InsertHelper mExtendedPropertiesInserter;
    public boolean mInTestMode;
    private DatabaseUtils.InsertHelper mInstancesInserter;
    private DatabaseUtils.InsertHelper mRemindersInserter;
    private final SyncStateContentProviderHelper mSyncState;

    public long calendarsInsert(ContentValues values) {
        return this.mCalendarsInserter.insert(values);
    }

    public long colorsInsert(ContentValues values) {
        return this.mColorsInserter.insert(values);
    }

    public long eventsInsert(ContentValues values) {
        return this.mEventsInserter.insert(values);
    }

    public long eventsRawTimesReplace(ContentValues values) {
        return this.mEventsRawTimesInserter.replace(values);
    }

    public long instancesInsert(ContentValues values) {
        return this.mInstancesInserter.insert(values);
    }

    public long instancesReplace(ContentValues values) {
        return this.mInstancesInserter.replace(values);
    }

    public long attendeesInsert(ContentValues values) {
        return this.mAttendeesInserter.insert(values);
    }

    public long remindersInsert(ContentValues values) {
        return this.mRemindersInserter.insert(values);
    }

    public long calendarAlertsInsert(ContentValues values) {
        return this.mCalendarAlertsInserter.insert(values);
    }

    public long extendedPropertiesInsert(ContentValues values) {
        return this.mExtendedPropertiesInserter.insert(values);
    }

    public static synchronized CalendarDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new CalendarDatabaseHelper(context);
        }
        return sSingleton;
    }

    CalendarDatabaseHelper(Context context) {
        super(context, "calendar.db", (SQLiteDatabase.CursorFactory) null, 600);
        this.mInTestMode = false;
        this.mSyncState = new SyncStateContentProviderHelper();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        this.mSyncState.onDatabaseOpened(db);
        this.mCalendarsInserter = new DatabaseUtils.InsertHelper(db, "Calendars");
        this.mColorsInserter = new DatabaseUtils.InsertHelper(db, "Colors");
        this.mEventsInserter = new DatabaseUtils.InsertHelper(db, "Events");
        this.mEventsRawTimesInserter = new DatabaseUtils.InsertHelper(db, "EventsRawTimes");
        this.mInstancesInserter = new DatabaseUtils.InsertHelper(db, "Instances");
        this.mAttendeesInserter = new DatabaseUtils.InsertHelper(db, "Attendees");
        this.mRemindersInserter = new DatabaseUtils.InsertHelper(db, "Reminders");
        this.mCalendarAlertsInserter = new DatabaseUtils.InsertHelper(db, "CalendarAlerts");
        this.mExtendedPropertiesInserter = new DatabaseUtils.InsertHelper(db, "ExtendedProperties");
    }

    private void upgradeSyncState(SQLiteDatabase db) {
        long version = DatabaseUtils.longForQuery(db, "SELECT version FROM _sync_state_metadata", null);
        if (version == 3) {
            Log.i("CalendarDatabaseHelper", "Upgrading calendar sync state table");
            db.execSQL("CREATE TEMPORARY TABLE state_backup(_sync_account TEXT, _sync_account_type TEXT, data TEXT);");
            db.execSQL("INSERT INTO state_backup SELECT _sync_account, _sync_account_type, data FROM _sync_state WHERE _sync_account is not NULL and _sync_account_type is not NULL;");
            db.execSQL("DROP TABLE _sync_state;");
            this.mSyncState.onDatabaseOpened(db);
            db.execSQL("INSERT INTO _sync_state(account_name,account_type,data) SELECT _sync_account, _sync_account_type, data from state_backup;");
            db.execSQL("DROP TABLE state_backup;");
            return;
        }
        Log.w("CalendarDatabaseHelper", "upgradeSyncState: current version is " + version + ", skipping upgrade.");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
    }

    private void bootstrapDB(SQLiteDatabase db) {
        Log.i("CalendarDatabaseHelper", "Bootstrapping database");
        this.mSyncState.createDatabase(db);
        createColorsTable(db);
        createCalendarsTable(db);
        createEventsTable(db);
        db.execSQL("CREATE TABLE EventsRawTimes (_id INTEGER PRIMARY KEY,event_id INTEGER NOT NULL,dtstart2445 TEXT,dtend2445 TEXT,originalInstanceTime2445 TEXT,lastDate2445 TEXT,UNIQUE (event_id));");
        db.execSQL("CREATE TABLE Instances (_id INTEGER PRIMARY KEY,event_id INTEGER,begin INTEGER,end INTEGER,startDay INTEGER,endDay INTEGER,startMinute INTEGER,endMinute INTEGER,UNIQUE (event_id, begin, end));");
        db.execSQL("CREATE INDEX instancesStartDayIndex ON Instances (startDay);");
        createCalendarMetaDataTable(db);
        createCalendarCacheTable(db, null);
        db.execSQL("CREATE TABLE Attendees (_id INTEGER PRIMARY KEY,event_id INTEGER,attendeeName TEXT,attendeeEmail TEXT,attendeeStatus INTEGER,attendeeRelationship INTEGER,attendeeType INTEGER,attendeeIdentity TEXT,attendeeIdNamespace TEXT);");
        db.execSQL("CREATE INDEX attendeesEventIdIndex ON Attendees (event_id);");
        db.execSQL("CREATE TABLE Reminders (_id INTEGER PRIMARY KEY,event_id INTEGER,minutes INTEGER,method INTEGER NOT NULL DEFAULT 0);");
        db.execSQL("CREATE INDEX remindersEventIdIndex ON Reminders (event_id);");
        db.execSQL("CREATE TABLE CalendarAlerts (_id INTEGER PRIMARY KEY,event_id INTEGER,begin INTEGER NOT NULL,end INTEGER NOT NULL,alarmTime INTEGER NOT NULL,creationTime INTEGER NOT NULL DEFAULT 0,receivedTime INTEGER NOT NULL DEFAULT 0,notifyTime INTEGER NOT NULL DEFAULT 0,state INTEGER NOT NULL,minutes INTEGER,UNIQUE (alarmTime, begin, event_id));");
        db.execSQL("CREATE INDEX calendarAlertsEventIdIndex ON CalendarAlerts (event_id);");
        db.execSQL("CREATE TABLE ExtendedProperties (_id INTEGER PRIMARY KEY,event_id INTEGER,name TEXT,value TEXT);");
        db.execSQL("CREATE INDEX extendedPropertiesEventIdIndex ON ExtendedProperties (event_id);");
        createEventsView(db);
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        createColorsTriggers(db);
        db.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
        scheduleSync(null, false, null);
    }

    private void createEventsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY AUTOINCREMENT,_sync_id TEXT,dirty INTEGER,mutators TEXT,lastSynced INTEGER DEFAULT 0,calendar_id INTEGER NOT NULL,title TEXT,eventLocation TEXT,description TEXT,eventColor INTEGER,eventColor_index TEXT,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_id INTEGER,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,isOrganizer INTEGER,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,customAppPackage TEXT,customAppUri TEXT,uid2445 TEXT,sync_data1 TEXT,sync_data2 TEXT,sync_data3 TEXT,sync_data4 TEXT,sync_data5 TEXT,sync_data6 TEXT,sync_data7 TEXT,sync_data8 TEXT,sync_data9 TEXT,sync_data10 TEXT);");
        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createEventsTable307(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY AUTOINCREMENT,_sync_id TEXT,dirty INTEGER,lastSynced INTEGER DEFAULT 0,calendar_id INTEGER NOT NULL,title TEXT,eventLocation TEXT,description TEXT,eventColor INTEGER,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_id INTEGER,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,sync_data1 TEXT,sync_data2 TEXT,sync_data3 TEXT,sync_data4 TEXT,sync_data5 TEXT,sync_data6 TEXT,sync_data7 TEXT,sync_data8 TEXT,sync_data9 TEXT,sync_data10 TEXT);");
        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createEventsTable300(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,dirty INTEGER,_sync_mark INTEGER,calendar_id INTEGER NOT NULL,htmlUri TEXT,title TEXT,eventLocation TEXT,description TEXT,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,commentsUri TEXT,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,sync_data1 TEXT);");
        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createCalendarsTable303(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,dirty INTEGER,name TEXT,displayName TEXT,calendar_color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT);");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createColorsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Colors (_id INTEGER PRIMARY KEY,account_name TEXT NOT NULL,account_type TEXT NOT NULL,data TEXT,color_type INTEGER NOT NULL,color_index TEXT NOT NULL,color INTEGER NOT NULL);");
    }

    public void createColorsTriggers(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER event_color_update UPDATE OF eventColor_index ON Events WHEN new.eventColor_index NOT NULL BEGIN UPDATE Events SET eventColor=(SELECT color FROM Colors WHERE account_name=(SELECT account_name FROM Calendars WHERE _id=new.calendar_id) AND account_type=(SELECT account_type FROM Calendars WHERE _id=new.calendar_id) AND color_index=new.eventColor_index AND color_type=1)  WHERE _id=old._id; END");
        db.execSQL("CREATE TRIGGER calendar_color_update UPDATE OF calendar_color_index ON Calendars WHEN new.calendar_color_index NOT NULL BEGIN UPDATE Calendars SET calendar_color=(SELECT color FROM Colors WHERE account_name=new.account_name AND account_type=new.account_type AND color_index=new.calendar_color_index AND color_type=0)  WHERE _id=old._id; END");
    }

    private void createCalendarsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,dirty INTEGER,mutators TEXT,name TEXT,calendar_displayName TEXT,calendar_color INTEGER,calendar_color_index TEXT,calendar_access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, isPrimary INTEGER, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,canPartiallyUpdate INTEGER DEFAULT 0,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',allowedAvailability TEXT DEFAULT '0,1',allowedAttendeeTypes TEXT DEFAULT '0,1,2',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT,cal_sync7 TEXT,cal_sync8 TEXT,cal_sync9 TEXT,cal_sync10 TEXT);");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable305(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,dirty INTEGER,name TEXT,calendar_displayName TEXT,calendar_color INTEGER,calendar_access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,canPartiallyUpdate INTEGER DEFAULT 0,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT,cal_sync7 TEXT,cal_sync8 TEXT,cal_sync9 TEXT,cal_sync10 TEXT);");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable300(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,dirty INTEGER,name TEXT,displayName TEXT,calendar_color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1,2',deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT,sync6 TEXT);");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable205(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_dirty INTEGER,name TEXT,displayName TEXT,color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1, maxReminders INTEGER DEFAULT 5,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT,sync6 TEXT);");
        createCalendarsCleanup200(db);
    }

    private void createCalendarsTable202(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,_sync_dirty INTEGER,_sync_mark INTEGER,name TEXT,displayName TEXT,color INTEGER,access_level INTEGER,selected INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, organizerCanRespond INTEGER NOT NULL DEFAULT 1,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT);");
        createCalendarsCleanup200(db);
    }

    private void createCalendarsTable200(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,_sync_dirty INTEGER,_sync_mark INTEGER,name TEXT,displayName TEXT,hidden INTEGER NOT NULL DEFAULT 0,color INTEGER,access_level INTEGER,selected INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, organizerCanRespond INTEGER NOT NULL DEFAULT 1,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT);");
        createCalendarsCleanup200(db);
    }

    private void createCalendarsCleanup200(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarMetaDataTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE CalendarMetaData (_id INTEGER PRIMARY KEY,localTimezone TEXT,minInstance INTEGER,maxInstance INTEGER);");
    }

    private void createCalendarMetaDataTable59(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE CalendarMetaData (_id INTEGER PRIMARY KEY,localTimezone TEXT,minInstance INTEGER,maxInstance INTEGER);");
    }

    private void createCalendarCacheTable(SQLiteDatabase db, String oldTimezoneDbVersion) {
        db.execSQL("DROP TABLE IF EXISTS CalendarCache;");
        db.execSQL("CREATE TABLE IF NOT EXISTS CalendarCache (_id INTEGER PRIMARY KEY,key TEXT NOT NULL,value TEXT);");
        initCalendarCacheTable(db, oldTimezoneDbVersion);
        updateCalendarCacheTable(db);
    }

    private void initCalendarCacheTable(SQLiteDatabase db, String oldTimezoneDbVersion) {
        String timezoneDbVersion = oldTimezoneDbVersion != null ? oldTimezoneDbVersion : "2009s";
        db.execSQL("INSERT OR REPLACE INTO CalendarCache (_id, key, value) VALUES (" + "timezoneDatabaseVersion".hashCode() + ",'timezoneDatabaseVersion','" + timezoneDbVersion + "');");
    }

    private void updateCalendarCacheTable(SQLiteDatabase db) {
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneType".hashCode() + ",'timezoneType','auto');");
        String defaultTimezone = TimeZone.getDefault().getID();
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstances".hashCode() + ",'timezoneInstances','" + defaultTimezone + "');");
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstancesPrevious".hashCode() + ",'timezoneInstancesPrevious','" + defaultTimezone + "');");
    }

    private void initCalendarCacheTable203(SQLiteDatabase db, String oldTimezoneDbVersion) {
        String timezoneDbVersion = oldTimezoneDbVersion != null ? oldTimezoneDbVersion : "2009s";
        db.execSQL("INSERT OR REPLACE INTO CalendarCache (_id, key, value) VALUES (" + "timezoneDatabaseVersion".hashCode() + ",'timezoneDatabaseVersion','" + timezoneDbVersion + "');");
    }

    private void updateCalendarCacheTableTo203(SQLiteDatabase db) {
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneType".hashCode() + ",'timezoneType','auto');");
        String defaultTimezone = TimeZone.getDefault().getID();
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstances".hashCode() + ",'timezoneInstances','" + defaultTimezone + "');");
        db.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstancesPrevious".hashCode() + ",'timezoneInstancesPrevious','" + defaultTimezone + "');");
    }

    static void removeOrphans(SQLiteDatabase db) {
        Log.d("CalendarDatabaseHelper", "Checking for orphaned entries");
        int count = db.delete("Attendees", "event_id IN (SELECT event_id FROM Attendees LEFT OUTER JOIN Events ON event_id=Events._id WHERE Events._id IS NULL)", null);
        if (count != 0) {
            Log.i("CalendarDatabaseHelper", "Deleted " + count + " orphaned Attendees");
        }
        int count2 = db.delete("Reminders", "event_id IN (SELECT event_id FROM Reminders LEFT OUTER JOIN Events ON event_id=Events._id WHERE Events._id IS NULL)", null);
        if (count2 != 0) {
            Log.i("CalendarDatabaseHelper", "Deleted " + count2 + " orphaned Reminders");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("CalendarDatabaseHelper", "Upgrading DB from version " + oldVersion + " to " + newVersion);
        long startWhen = System.nanoTime();
        if (oldVersion < 49) {
            dropTables(db);
            bootstrapDB(db);
            return;
        }
        boolean recreateMetaDataAndInstances = oldVersion >= 59 && oldVersion <= 66;
        boolean createEventsView = false;
        if (oldVersion < 51) {
            try {
                upgradeToVersion51(db);
                oldVersion = 51;
            } catch (SQLiteException e) {
                if (this.mInTestMode) {
                    throw e;
                }
                Log.e("CalendarDatabaseHelper", "onUpgrade: SQLiteException, recreating db. ", e);
                Log.e("CalendarDatabaseHelper", "(oldVersion was " + oldVersion + ")");
                dropTables(db);
                bootstrapDB(db);
                return;
            }
        }
        if (oldVersion == 51) {
            upgradeToVersion52(db);
            oldVersion++;
        }
        if (oldVersion == 52) {
            upgradeToVersion53(db);
            oldVersion++;
        }
        if (oldVersion == 53) {
            upgradeToVersion54(db);
            oldVersion++;
        }
        if (oldVersion == 54) {
            upgradeToVersion55(db);
            oldVersion++;
        }
        if (oldVersion == 55 || oldVersion == 56) {
            upgradeResync(db);
        }
        if (oldVersion == 55) {
            upgradeToVersion56(db);
            oldVersion++;
        }
        if (oldVersion == 56) {
            upgradeToVersion57(db);
            oldVersion++;
        }
        if (oldVersion == 57) {
            oldVersion++;
        }
        if (oldVersion == 58) {
            upgradeToVersion59(db);
            oldVersion++;
        }
        if (oldVersion == 59) {
            upgradeToVersion60(db);
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 60) {
            upgradeToVersion61(db);
            oldVersion++;
        }
        if (oldVersion == 61) {
            upgradeToVersion62(db);
            oldVersion++;
        }
        if (oldVersion == 62) {
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 63) {
            upgradeToVersion64(db);
            oldVersion++;
        }
        if (oldVersion == 64) {
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 65) {
            upgradeToVersion66(db);
            oldVersion++;
        }
        if (oldVersion == 66) {
            oldVersion++;
        }
        if (recreateMetaDataAndInstances) {
            recreateMetaDataAndInstances67(db);
        }
        if (oldVersion == 67 || oldVersion == 68) {
            upgradeToVersion69(db);
            oldVersion = 69;
        }
        if (oldVersion == 69) {
            upgradeToVersion200(db);
            createEventsView = true;
            oldVersion = 200;
        }
        if (oldVersion == 70) {
            upgradeToVersion200(db);
            oldVersion = 200;
        }
        if (oldVersion == 100) {
            upgradeToVersion200(db);
            oldVersion = 200;
        }
        boolean need203Update = true;
        if (oldVersion == 101 || oldVersion == 102) {
            upgradeToVersion200(db);
            oldVersion = 200;
            need203Update = false;
        }
        if (oldVersion == 200) {
            upgradeToVersion201(db);
            oldVersion++;
        }
        if (oldVersion == 201) {
            upgradeToVersion202(db);
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 202) {
            if (need203Update) {
                upgradeToVersion203(db);
            }
            oldVersion++;
        }
        if (oldVersion == 203) {
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 206) {
            oldVersion -= 2;
        }
        if (oldVersion == 204) {
            upgradeToVersion205(db);
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 205) {
            upgradeToVersion300(db);
            createEventsView = true;
            oldVersion = 300;
        }
        if (oldVersion == 300) {
            upgradeToVersion301(db);
            createEventsView = true;
            oldVersion++;
        }
        if (oldVersion == 301) {
            upgradeToVersion302(db);
            oldVersion++;
        }
        if (oldVersion == 302) {
            upgradeToVersion303(db);
            oldVersion++;
            createEventsView = true;
        }
        if (oldVersion == 303) {
            upgradeToVersion304(db);
            oldVersion++;
            createEventsView = true;
        }
        if (oldVersion == 304) {
            upgradeToVersion305(db);
            oldVersion++;
            createEventsView = true;
        }
        if (oldVersion == 305) {
            upgradeToVersion306(db);
            scheduleSync(null, false, null);
            oldVersion++;
        }
        if (oldVersion == 306) {
            upgradeToVersion307(db);
            oldVersion++;
        }
        if (oldVersion == 307) {
            upgradeToVersion308(db);
            oldVersion++;
            createEventsView = true;
        }
        if (oldVersion == 308) {
            upgradeToVersion400(db);
            createEventsView = true;
            oldVersion = 400;
        }
        if (oldVersion == 309 || oldVersion == 400) {
            upgradeToVersion401(db);
            createEventsView = true;
            oldVersion = 401;
        }
        if (oldVersion == 401) {
            upgradeToVersion402(db);
            createEventsView = true;
            oldVersion = 402;
        }
        if (oldVersion == 402) {
            upgradeToVersion403(db);
            createEventsView = true;
            oldVersion = 403;
        }
        if (oldVersion == 403) {
            upgradeToVersion501(db);
            createEventsView = true;
            oldVersion = 501;
        }
        if (oldVersion == 501) {
            upgradeToVersion502(db);
            createEventsView = true;
            oldVersion = 502;
        }
        if (oldVersion < 600) {
            upgradeToVersion600(db);
            createEventsView = true;
            oldVersion = 600;
        }
        if (createEventsView) {
            createEventsView(db);
        }
        if (oldVersion != 600) {
            Log.e("CalendarDatabaseHelper", "Need to recreate Calendar schema because of unknown Calendar database version: " + oldVersion);
            dropTables(db);
            bootstrapDB(db);
        } else {
            removeOrphans(db);
        }
        long endWhen = System.nanoTime();
        Log.d("CalendarDatabaseHelper", "Calendar upgrade took " + ((endWhen - startWhen) / 1000000) + "ms");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("CalendarDatabaseHelper", "Can't downgrade DB from version " + oldVersion + " to " + newVersion);
        dropTables(db);
        bootstrapDB(db);
    }

    private void recreateMetaDataAndInstances67(SQLiteDatabase db) {
        db.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(db);
        db.execSQL("DELETE FROM Instances;");
    }

    private static boolean fixAllDayTime(Time time, String timezone, Long timeInMillis) {
        time.set(timeInMillis.longValue());
        if (time.hour == 0 && time.minute == 0 && time.second == 0) {
            return false;
        }
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        return true;
    }

    private void upgradeToVersion600(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN mutators TEXT;");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN mutators TEXT;");
    }

    private void upgradeToVersion501(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN isOrganizer INTEGER;");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN isPrimary INTEGER;");
    }

    private void upgradeToVersion502(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN uid2445 TEXT;");
    }

    private void upgradeToVersion403(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN customAppPackage TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN customAppUri TEXT;");
    }

    private void upgradeToVersion402(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdentity TEXT;");
        db.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdNamespace TEXT;");
    }

    private void upgradeToVersion401(SQLiteDatabase db) {
        db.execSQL("UPDATE events SET original_id=(SELECT _id FROM events inner_events WHERE inner_events._sync_id=events.original_sync_id AND inner_events.calendar_id=events.calendar_id) WHERE NOT original_id IS NULL AND (SELECT calendar_id FROM events ex_events WHERE ex_events._id=events.original_id) <> calendar_id ");
    }

    private void upgradeToVersion400(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS calendar_color_update");
        db.execSQL("CREATE TRIGGER calendar_color_update UPDATE OF calendar_color_index ON Calendars WHEN new.calendar_color_index NOT NULL BEGIN UPDATE Calendars SET calendar_color=(SELECT color FROM Colors WHERE account_name=new.account_name AND account_type=new.account_type AND color_index=new.calendar_color_index AND color_type=0)  WHERE _id=old._id; END");
        db.execSQL("DROP TRIGGER IF EXISTS event_color_update");
        db.execSQL("CREATE TRIGGER event_color_update UPDATE OF eventColor_index ON Events WHEN new.eventColor_index NOT NULL BEGIN UPDATE Events SET eventColor=(SELECT color FROM Colors WHERE account_name=(SELECT account_name FROM Calendars WHERE _id=new.calendar_id) AND account_type=(SELECT account_type FROM Calendars WHERE _id=new.calendar_id) AND color_index=new.eventColor_index AND color_type=1)  WHERE _id=old._id; END");
    }

    private void upgradeToVersion308(SQLiteDatabase db) {
        createColorsTable(db);
        db.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAvailability TEXT DEFAULT '0,1';");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAttendeeTypes TEXT DEFAULT '0,1,2';");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN calendar_color_index TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN eventColor_index TEXT;");
        db.execSQL("UPDATE Calendars SET allowedAvailability='0,1,2' WHERE _id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
        createColorsTriggers(db);
    }

    private void upgradeToVersion307(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable307(db);
        db.execSQL("INSERT INTO Events (_id, _sync_id, dirty, lastSynced,calendar_id, title, eventLocation, description, eventColor, eventStatus, selfAttendeeStatus, dtstart, dtend, eventTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, eventEndTimezone, sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10 ) SELECT _id, _sync_id, dirty, lastSynced,calendar_id, title, eventLocation, description, eventColor, eventStatus, selfAttendeeStatus, dtstart, dtend, eventTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, eventEndTimezone, sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10 FROM Events_Backup;");
        db.execSQL("DROP TABLE Events_Backup;");
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        db.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion306(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        db.execSQL("UPDATE Events SET _sync_id = REPLACE(_sync_id, '/private/full/', '/events/'), original_sync_id = REPLACE(original_sync_id, '/private/full/', '/events/') WHERE _id IN (SELECT Events._id FROM Events JOIN Calendars ON Events.calendar_id = Calendars._id WHERE account_type = 'com.google')");
        db.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
        db.execSQL("UPDATE Calendars SET canPartiallyUpdate = 1 WHERE account_type = 'com.google'");
        db.execSQL("DELETE FROM _sync_state WHERE account_type = 'com.google'");
    }

    private void upgradeToVersion305(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable305(db);
        db.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, cal_sync7, cal_sync8, dirty, name, calendar_displayName, calendar_color, calendar_access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, canPartiallyUpdate,cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6) SELECT _id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, canPartiallyUpdate,cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6 FROM Calendars_Backup;");
        db.execSQL("DROP TABLE Calendars_Backup;");
        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable307(db);
        db.execSQL("INSERT INTO Events (_id, _sync_id, sync_data4, sync_data5, sync_data2, dirty, sync_data8, calendar_id, sync_data3, title, eventLocation, description, eventStatus, selfAttendeeStatus, sync_data6, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data7,lastSynced,sync_data1) SELECT _id, _sync_id, _sync_version, _sync_time, _sync_local_id, dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data7,lastSynced,sync_data1 FROM Events_Backup;");
        db.execSQL("DROP TABLE Events_Backup;");
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        db.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion304(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN canPartiallyUpdate INTEGER DEFAULT 0;");
        db.execSQL("ALTER TABLE Events ADD COLUMN sync_data7 TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN lastSynced INTEGER DEFAULT 0;");
    }

    private void upgradeToVersion303(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable303(db);
        db.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6) SELECT _id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders,deleted, sync1, sync2, sync3, sync4,sync5,sync6 FROM Calendars_Backup;");
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion302(SQLiteDatabase db) {
        db.execSQL("UPDATE Events SET sync_data1=eventEndTimezone WHERE calendar_id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
        db.execSQL("UPDATE Events SET eventEndTimezone=NULL WHERE calendar_id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
    }

    private void upgradeToVersion301(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS original_sync_update;");
        db.execSQL("ALTER TABLE Events ADD COLUMN original_id INTEGER;");
        db.execSQL("UPDATE Events set original_id=(SELECT Events2._id FROM Events AS Events2 WHERE Events2._sync_id=Events.original_sync_id) WHERE Events.original_sync_id NOT NULL");
        db.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion300(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup;");
        createCalendarsTable300(db);
        db.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders,deleted, sync1, sync2, sync3, sync4,sync5,sync6) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, visible, sync_events, location, timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, '0,1,2,3',deleted, sync1, sync2, sync3, sync4, sync5, sync6 FROM Calendars_Backup;");
        db.execSQL("UPDATE Calendars SET allowedReminders = '0,1,2' WHERE account_type = 'com.google'");
        db.execSQL("DROP TABLE Calendars_Backup;");
        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_insert");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP INDEX IF EXISTS eventSyncAccountAndIdIndex");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable300(db);
        db.execSQL("INSERT INTO Events (_id, _sync_id, _sync_version, _sync_time, _sync_local_id, dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data1) SELECT _id, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventTimezone2, duration, allDay, visibility, transparency, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, originalEvent, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, syncAdapterData FROM Events_Backup;");
        db.execSQL("DROP TABLE Events_Backup;");
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
    }

    private void upgradeToVersion205(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable205(db);
        db.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, visible, sync_events, location, timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, deleted, sync1, sync2, sync3, sync4,sync5,sync6) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, 1, 5, deleted, sync1, sync2, sync3, sync4, sync5, _sync_mark FROM Calendars_Backup;");
        db.execSQL("UPDATE Calendars SET canModifyTimeZone=0, maxReminders=1 WHERE _sync_account_type='com.android.exchange'");
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion203(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT value FROM CalendarCache WHERE key=?", new String[]{"timezoneDatabaseVersion"});
        String oldTimezoneDbVersion = null;
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    oldTimezoneDbVersion = cursor.getString(0);
                    cursor.close();
                    cursor = null;
                    db.execSQL("DELETE FROM CalendarCache;");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        initCalendarCacheTable203(db, oldTimezoneDbVersion);
        updateCalendarCacheTableTo203(db);
    }

    private void upgradeToVersion202(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable202(db);
        db.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1, sync2, sync3, sync4,sync5) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1, sync2, sync3, sync4, hidden FROM Calendars_Backup;");
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion201(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN sync4 TEXT;");
    }

    private void upgradeToVersion200(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable200(db);
        db.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, 0, url FROM Calendars_Backup;");
        Cursor cursor = db.rawQuery("SELECT _id, url FROM Calendars_Backup WHERE _sync_account_type='com.google' AND url IS NOT NULL;", null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    Object[] bindArgs = new Object[3];
                    while (cursor.moveToNext()) {
                        Long id = Long.valueOf(cursor.getLong(0));
                        String url = cursor.getString(1);
                        String selfUrl = getSelfUrlFromEventsUrl(url);
                        String editUrl = getEditUrlFromEventsUrl(url);
                        bindArgs[0] = editUrl;
                        bindArgs[1] = selfUrl;
                        bindArgs[2] = id;
                        db.execSQL("UPDATE Calendars SET sync2=?, sync3=? WHERE _id=?;", bindArgs);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    public static void upgradeToVersion69(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT _id, dtstart, dtend, duration, dtstart2, dtend2, eventTimezone, eventTimezone2, rrule FROM Events WHERE allDay=?", new String[]{"1"});
        if (cursor != null) {
            try {
                Time time = new Time();
                while (cursor.moveToNext()) {
                    String rrule = cursor.getString(8);
                    Long id = Long.valueOf(cursor.getLong(0));
                    Long dtstart = Long.valueOf(cursor.getLong(1));
                    Long dtstart2 = null;
                    String timezone = cursor.getString(6);
                    String timezone2 = cursor.getString(7);
                    String duration = cursor.getString(3);
                    if (TextUtils.isEmpty(rrule)) {
                        Long dtend = Long.valueOf(cursor.getLong(2));
                        Long dtend2 = null;
                        if (!TextUtils.isEmpty(timezone2)) {
                            dtstart2 = Long.valueOf(cursor.getLong(4));
                            dtend2 = Long.valueOf(cursor.getLong(5));
                        }
                        boolean update = false;
                        if (!TextUtils.equals(timezone, "UTC")) {
                            update = true;
                            timezone = "UTC";
                        }
                        time.clear(timezone);
                        boolean update2 = update | fixAllDayTime(time, timezone, dtstart);
                        Long dtstart3 = Long.valueOf(time.normalize(false));
                        time.clear(timezone);
                        boolean update3 = update2 | fixAllDayTime(time, timezone, dtend);
                        Long dtend3 = Long.valueOf(time.normalize(false));
                        if (dtstart2 != null) {
                            time.clear(timezone2);
                            update3 |= fixAllDayTime(time, timezone2, dtstart2);
                            dtstart2 = Long.valueOf(time.normalize(false));
                        }
                        if (dtend2 != null) {
                            time.clear(timezone2);
                            update3 |= fixAllDayTime(time, timezone2, dtend2);
                            dtend2 = Long.valueOf(time.normalize(false));
                        }
                        if (!TextUtils.isEmpty(duration)) {
                            update3 = true;
                        }
                        if (update3) {
                            db.execSQL("UPDATE Events SET dtstart=?, dtend=?, dtstart2=?, dtend2=?, duration=?, eventTimezone=?, eventTimezone2=? WHERE _id=?", new Object[]{dtstart3, dtend3, dtstart2, dtend2, null, timezone, timezone2, id});
                        }
                    } else {
                        if (!TextUtils.isEmpty(timezone2)) {
                            dtstart2 = Long.valueOf(cursor.getLong(4));
                        }
                        boolean update4 = false;
                        if (!TextUtils.equals(timezone, "UTC")) {
                            update4 = true;
                            timezone = "UTC";
                        }
                        time.clear(timezone);
                        boolean update5 = update4 | fixAllDayTime(time, timezone, dtstart);
                        Long dtstart4 = Long.valueOf(time.normalize(false));
                        if (dtstart2 != null) {
                            time.clear(timezone2);
                            update5 |= fixAllDayTime(time, timezone2, dtstart2);
                            dtstart2 = Long.valueOf(time.normalize(false));
                        }
                        if (TextUtils.isEmpty(duration)) {
                            duration = "P1D";
                            update5 = true;
                        } else {
                            int len = duration.length();
                            if (duration.charAt(0) == 'P' && duration.charAt(len - 1) == 'S') {
                                int seconds = Integer.parseInt(duration.substring(1, len - 1));
                                int days = ((86400 + seconds) - 1) / 86400;
                                duration = "P" + days + "D";
                                update5 = true;
                            }
                        }
                        if (update5) {
                            db.execSQL("UPDATE Events SET dtstart=?, dtend=?, dtstart2=?, dtend2=?, duration=?,eventTimezone=?, eventTimezone2=? WHERE _id=?", new Object[]{dtstart4, null, dtstart2, null, duration, timezone, timezone2, id});
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion66(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN organizerCanRespond INTEGER NOT NULL DEFAULT 1;");
    }

    private void upgradeToVersion64(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN syncAdapterData TEXT;");
    }

    private void upgradeToVersion62(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN dtstart2 INTEGER;");
        db.execSQL("ALTER TABLE Events ADD COLUMN dtend2 INTEGER;");
        db.execSQL("ALTER TABLE Events ADD COLUMN eventTimezone2 TEXT;");
        String[] allDayBit = {"0"};
        db.execSQL("UPDATE Events SET dtstart2=dtstart,dtend2=dtend,eventTimezone2=eventTimezone WHERE allDay=?;", allDayBit);
        allDayBit[0] = "1";
        Cursor cursor = db.rawQuery("SELECT Events._id,dtstart,dtend,eventTimezone,timezone FROM Events INNER JOIN Calendars WHERE Events.calendar_id=Calendars._id AND allDay=?", allDayBit);
        Time oldTime = new Time();
        Time newTime = new Time();
        if (cursor != null) {
            try {
                String[] newData = new String[4];
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    long dtstart = cursor.getLong(1);
                    long dtend = cursor.getLong(2);
                    String eTz = cursor.getString(3);
                    String tz = cursor.getString(4);
                    if (eTz == null) {
                        eTz = "UTC";
                    }
                    oldTime.clear(eTz);
                    oldTime.set(dtstart);
                    newTime.clear(tz);
                    newTime.set(oldTime.monthDay, oldTime.month, oldTime.year);
                    newTime.normalize(false);
                    long dtstart2 = newTime.toMillis(false);
                    oldTime.clear(eTz);
                    oldTime.set(dtend);
                    newTime.clear(tz);
                    newTime.set(oldTime.monthDay, oldTime.month, oldTime.year);
                    newTime.normalize(false);
                    long dtend2 = newTime.toMillis(false);
                    newData[0] = String.valueOf(dtstart2);
                    newData[1] = String.valueOf(dtend2);
                    newData[2] = tz;
                    newData[3] = String.valueOf(id);
                    db.execSQL("UPDATE Events SET dtstart2=?, dtend2=?, eventTimezone2=? WHERE _id=?", newData);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion61(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS CalendarCache;");
        db.execSQL("CREATE TABLE IF NOT EXISTS CalendarCache (_id INTEGER PRIMARY KEY,key TEXT NOT NULL,value TEXT);");
        db.execSQL("INSERT INTO CalendarCache (key, value) VALUES ('timezoneDatabaseVersion','2009s');");
    }

    private void upgradeToVersion60(SQLiteDatabase db) {
        upgradeSyncState(db);
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
        db.execSQL("ALTER TABLE Events ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("DROP TRIGGER IF EXISTS events_insert");
        db.execSQL("CREATE TRIGGER events_insert AFTER INSERT ON Events BEGIN UPDATE Events SET _sync_account= (SELECT _sync_account FROM Calendars WHERE Calendars._id=new.calendar_id),_sync_account_type= (SELECT _sync_account_type FROM Calendars WHERE Calendars._id=new.calendar_id) WHERE Events._id=new._id;END");
        db.execSQL("DROP TABLE IF EXISTS DeletedEvents;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_update");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_insert");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_delete");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_update");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_insert");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_delete");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_update");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_insert");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_delete");
    }

    private void upgradeToVersion59(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS BusyBits;");
        db.execSQL("CREATE TEMPORARY TABLE CalendarMetaData_Backup(_id,localTimezone,minInstance,maxInstance);");
        db.execSQL("INSERT INTO CalendarMetaData_Backup SELECT _id,localTimezone,minInstance,maxInstance FROM CalendarMetaData;");
        db.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(db);
        db.execSQL("INSERT INTO CalendarMetaData SELECT _id,localTimezone,minInstance,maxInstance FROM CalendarMetaData_Backup;");
        db.execSQL("DROP TABLE CalendarMetaData_Backup;");
    }

    private void upgradeToVersion57(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN guestsCanModify INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE Events ADD COLUMN guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1;");
        db.execSQL("ALTER TABLE Events ADD COLUMN guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1;");
        db.execSQL("ALTER TABLE Events ADD COLUMN organizer STRING;");
        db.execSQL("UPDATE Events SET organizer=(SELECT attendeeEmail FROM Attendees WHERE Attendees.event_id=Events._id AND Attendees.attendeeRelationship=2);");
    }

    private void upgradeToVersion56(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN ownerAccount TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN hasAttendeeData INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("UPDATE Events SET _sync_dirty=0, _sync_version=NULL, _sync_id=REPLACE(_sync_id, '/private/full-selfattendance', '/private/full'),commentsUri=REPLACE(commentsUri, '/private/full-selfattendance', '/private/full');");
        db.execSQL("UPDATE Calendars SET url=REPLACE(url, '/private/full-selfattendance', '/private/full');");
        Cursor cursor = db.rawQuery("SELECT _id, url FROM Calendars", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    Long id = Long.valueOf(cursor.getLong(0));
                    String url = cursor.getString(1);
                    String owner = calendarEmailAddressFromFeedUrl(url);
                    db.execSQL("UPDATE Calendars SET ownerAccount=? WHERE _id=?", new Object[]{owner, id});
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void upgradeResync(SQLiteDatabase db) {
        db.execSQL("DELETE FROM _sync_state;");
        Cursor cursor = db.rawQuery("SELECT _sync_account,_sync_account_type,url FROM Calendars", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String accountName = cursor.getString(0);
                    String accountType = cursor.getString(1);
                    Account account = new Account(accountName, accountType);
                    String calendarUrl = cursor.getString(2);
                    scheduleSync(account, false, calendarUrl);
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void upgradeToVersion55(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN _sync_account_type TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN _sync_account_type TEXT;");
        db.execSQL("ALTER TABLE DeletedEvents ADD COLUMN _sync_account_type TEXT;");
        db.execSQL("UPDATE Calendars SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        db.execSQL("UPDATE Events SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        db.execSQL("UPDATE DeletedEvents SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        Log.w("CalendarDatabaseHelper", "re-creating eventSyncAccountAndIdIndex");
        db.execSQL("DROP INDEX eventSyncAccountAndIdIndex");
        db.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events (_sync_account_type, _sync_account, _sync_id);");
    }

    private void upgradeToVersion54(SQLiteDatabase db) {
        Log.w("CalendarDatabaseHelper", "adding eventSyncAccountAndIdIndex");
        db.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events (_sync_account, _sync_id);");
    }

    private void upgradeToVersion53(SQLiteDatabase db) {
        Log.w("CalendarDatabaseHelper", "Upgrading CalendarAlerts table");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN creationTime INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN receivedTime INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN notifyTime INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion52(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events ADD COLUMN originalAllDay INTEGER;");
        Cursor recur = db.rawQuery("SELECT _id,originalEvent FROM Events WHERE originalEvent IS NOT NULL", null);
        if (recur != null) {
            while (recur.moveToNext()) {
                try {
                    long id = recur.getLong(0);
                    String originalEvent = recur.getString(1);
                    recur = db.rawQuery("SELECT allDay FROM Events WHERE _sync_id=?", new String[]{originalEvent});
                    if (recur != null) {
                        if (recur.moveToNext()) {
                            int allDay = recur.getInt(0);
                            db.execSQL("UPDATE Events SET originalAllDay=" + allDay + " WHERE _id=" + id);
                        }
                        recur.close();
                    }
                } catch (Throwable th) {
                    throw th;
                } finally {
                    recur.close();
                }
            }
        }
    }

    private void upgradeToVersion51(SQLiteDatabase db) {
        Log.w("CalendarDatabaseHelper", "Upgrading DeletedEvents table");
        db.execSQL("ALTER TABLE DeletedEvents ADD COLUMN calendar_id INTEGER;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;DELETE FROM DeletedEvents WHERE calendar_id = old._id;END");
        db.execSQL("DROP TRIGGER IF EXISTS event_to_deleted");
    }

    private void dropTables(SQLiteDatabase db) {
        Log.i("CalendarDatabaseHelper", "Clearing database");
        String[] columns = {"type", "name"};
        Cursor cursor = db.query("sqlite_master", columns, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String name = cursor.getString(1);
                    if (!name.startsWith("sqlite_")) {
                        String sql = "DROP " + cursor.getString(0) + " IF EXISTS " + name;
                        try {
                            db.execSQL(sql);
                        } catch (SQLException e) {
                            Log.e("CalendarDatabaseHelper", "Error executing " + sql + " " + e.toString());
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db;
        db = super.getWritableDatabase();
        return db;
    }

    public SyncStateContentProviderHelper getSyncState() {
        return this.mSyncState;
    }

    void scheduleSync(Account account, boolean uploadChangesOnly, String url) {
        Bundle extras = new Bundle();
        if (uploadChangesOnly) {
            extras.putBoolean("upload", uploadChangesOnly);
        }
        if (url != null) {
            extras.putString("feed", url);
        }
        ContentResolver.requestSync(account, CalendarContract.Calendars.CONTENT_URI.getAuthority(), extras);
    }

    private static void createEventsView(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_events;");
        db.execSQL("CREATE VIEW view_events AS SELECT Events._id AS _id,title,description,eventLocation,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,duration,eventTimezone,eventEndTimezone,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,calendar_id,guestsCanInviteOthers,guestsCanModify,guestsCanSeeGuests,organizer,COALESCE(isOrganizer, organizer = ownerAccount) AS isOrganizer,customAppPackage,customAppUri,uid2445,sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10,Events.deleted AS deleted,Events._sync_id AS _sync_id,Events.dirty AS dirty,Events.mutators AS mutators,lastSynced,Calendars.account_name AS account_name,Calendars.account_type AS account_type,calendar_timezone,calendar_displayName,calendar_location,visible,calendar_color,calendar_color_index,calendar_access_level,maxReminders,allowedReminders,allowedAttendeeTypes,allowedAvailability,canOrganizerRespond,canModifyTimeZone,canPartiallyUpdate,cal_sync1,cal_sync2,cal_sync3,cal_sync4,cal_sync5,cal_sync6,cal_sync7,cal_sync8,cal_sync9,cal_sync10,ownerAccount,sync_events,ifnull(eventColor,calendar_color) AS displayColor FROM Events JOIN Calendars ON (Events.calendar_id=Calendars._id)");
    }

    public static String calendarEmailAddressFromFeedUrl(String feed) {
        String[] pathComponents = feed.split("/");
        if (pathComponents.length > 5 && "feeds".equals(pathComponents[4])) {
            try {
                return URLDecoder.decode(pathComponents[5], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("CalendarDatabaseHelper", "unable to url decode the email address in calendar " + feed);
                return null;
            }
        }
        Log.e("CalendarDatabaseHelper", "unable to find the email address in calendar " + feed);
        return null;
    }

    private static String getAllCalendarsUrlFromEventsUrl(String url) {
        if (url == null) {
            if (!Log.isLoggable("CalendarDatabaseHelper", 3)) {
                return null;
            }
            Log.d("CalendarDatabaseHelper", "Cannot get AllCalendars url from a NULL url");
            return null;
        }
        if (url.contains("/private/full")) {
            return url.replace("/private/full", "").replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        if (url.contains("/private/free-busy")) {
            return url.replace("/private/free-busy", "").replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        if (!Log.isLoggable("CalendarDatabaseHelper", 3)) {
            return null;
        }
        Log.d("CalendarDatabaseHelper", "Cannot get AllCalendars url from the following url: " + url);
        return null;
    }

    private static String getSelfUrlFromEventsUrl(String url) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(url));
    }

    private static String getEditUrlFromEventsUrl(String url) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(url));
    }

    private static String rewriteUrlFromHttpToHttps(String url) {
        if (url == null) {
            if (Log.isLoggable("CalendarDatabaseHelper", 3)) {
                Log.d("CalendarDatabaseHelper", "Cannot rewrite a NULL url");
            }
            return null;
        }
        if (!url.startsWith("https://")) {
            if (!url.startsWith("http://")) {
                throw new IllegalArgumentException("invalid url parameter, unknown scheme: " + url);
            }
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    protected void duplicateEvent(long id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            long canPartiallyUpdate = DatabaseUtils.longForQuery(db, "SELECT canPartiallyUpdate FROM view_events WHERE _id = ?", new String[]{String.valueOf(id)});
            if (canPartiallyUpdate != 0) {
                db.execSQL("INSERT INTO Events  (_sync_id,calendar_id,title,eventLocation,description,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,eventTimezone,eventEndTimezone,duration,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,guestsCanModify,guestsCanInviteOthers,guestsCanSeeGuests,organizer,isOrganizer,customAppPackage,customAppUri,uid2445,dirty,lastSynced) SELECT _sync_id,calendar_id,title,eventLocation,description,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,eventTimezone,eventEndTimezone,duration,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,guestsCanModify,guestsCanInviteOthers,guestsCanSeeGuests,organizer,isOrganizer,customAppPackage,customAppUri,uid2445, 0, 1 FROM Events WHERE _id = ? AND dirty = ?", new Object[]{Long.valueOf(id), 0});
                long newId = DatabaseUtils.longForQuery(db, "SELECT CASE changes() WHEN 0 THEN -1 ELSE last_insert_rowid() END", null);
                if (newId >= 0) {
                    if (Log.isLoggable("CalendarDatabaseHelper", 2)) {
                        Log.v("CalendarDatabaseHelper", "Duplicating event " + id + " into new event " + newId);
                    }
                    copyEventRelatedTables(db, newId, id);
                }
            }
        } catch (SQLiteDoneException e) {
        }
    }

    static void copyEventRelatedTables(SQLiteDatabase db, long newId, long id) {
        db.execSQL("INSERT INTO Reminders ( event_id, minutes,method) SELECT ?,minutes,method FROM Reminders WHERE event_id = ?", new Object[]{Long.valueOf(newId), Long.valueOf(id)});
        db.execSQL("INSERT INTO Attendees (event_id,attendeeName,attendeeEmail,attendeeStatus,attendeeRelationship,attendeeType,attendeeIdentity,attendeeIdNamespace) SELECT ?,attendeeName,attendeeEmail,attendeeStatus,attendeeRelationship,attendeeType,attendeeIdentity,attendeeIdNamespace FROM Attendees WHERE event_id = ?", new Object[]{Long.valueOf(newId), Long.valueOf(id)});
        db.execSQL("INSERT INTO ExtendedProperties (event_id,name,value) SELECT ?, name,value FROM ExtendedProperties WHERE event_id = ?", new Object[]{Long.valueOf(newId), Long.valueOf(id)});
    }

    protected void removeDuplicateEvent(long id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT _id FROM Events WHERE _sync_id = (SELECT _sync_id FROM Events WHERE _id = ?) AND lastSynced = ?", new String[]{String.valueOf(id), "1"});
        try {
            if (cursor.moveToNext()) {
                long dupId = cursor.getLong(0);
                if (Log.isLoggable("CalendarDatabaseHelper", 2)) {
                    Log.v("CalendarDatabaseHelper", "Removing duplicate event " + dupId + " of original event " + id);
                }
                db.execSQL("DELETE FROM Events WHERE _id = ?", new Object[]{Long.valueOf(dupId)});
            }
        } finally {
            cursor.close();
        }
    }
}
