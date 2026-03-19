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
import com.mediatek.providers.calendar.extension.ExtensionFactory;
import com.mediatek.providers.calendar.extension.IDatabaseUpgradeExt;
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

    public long calendarsInsert(ContentValues contentValues) {
        return this.mCalendarsInserter.insert(contentValues);
    }

    public long colorsInsert(ContentValues contentValues) {
        return this.mColorsInserter.insert(contentValues);
    }

    public long eventsInsert(ContentValues contentValues) {
        return this.mEventsInserter.insert(contentValues);
    }

    public long eventsRawTimesReplace(ContentValues contentValues) {
        return this.mEventsRawTimesInserter.replace(contentValues);
    }

    public long instancesInsert(ContentValues contentValues) {
        return this.mInstancesInserter.insert(contentValues);
    }

    public long instancesReplace(ContentValues contentValues) {
        return this.mInstancesInserter.replace(contentValues);
    }

    public long attendeesInsert(ContentValues contentValues) {
        return this.mAttendeesInserter.insert(contentValues);
    }

    public long remindersInsert(ContentValues contentValues) {
        return this.mRemindersInserter.insert(contentValues);
    }

    public long calendarAlertsInsert(ContentValues contentValues) {
        return this.mCalendarAlertsInserter.insert(contentValues);
    }

    public long extendedPropertiesInsert(ContentValues contentValues) {
        return this.mExtendedPropertiesInserter.insert(contentValues);
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
    public void onOpen(SQLiteDatabase sQLiteDatabase) {
        this.mSyncState.onDatabaseOpened(sQLiteDatabase);
        this.mCalendarsInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Calendars");
        this.mColorsInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Colors");
        this.mEventsInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Events");
        this.mEventsRawTimesInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "EventsRawTimes");
        this.mInstancesInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Instances");
        this.mAttendeesInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Attendees");
        this.mRemindersInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "Reminders");
        this.mCalendarAlertsInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "CalendarAlerts");
        this.mExtendedPropertiesInserter = new DatabaseUtils.InsertHelper(sQLiteDatabase, "ExtendedProperties");
    }

    private void upgradeSyncState(SQLiteDatabase sQLiteDatabase) {
        long jLongForQuery = DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT version FROM _sync_state_metadata", null);
        if (jLongForQuery == 3) {
            Log.i("CalendarDatabaseHelper", "Upgrading calendar sync state table");
            sQLiteDatabase.execSQL("CREATE TEMPORARY TABLE state_backup(_sync_account TEXT, _sync_account_type TEXT, data TEXT);");
            sQLiteDatabase.execSQL("INSERT INTO state_backup SELECT _sync_account, _sync_account_type, data FROM _sync_state WHERE _sync_account is not NULL and _sync_account_type is not NULL;");
            sQLiteDatabase.execSQL("DROP TABLE _sync_state;");
            this.mSyncState.onDatabaseOpened(sQLiteDatabase);
            sQLiteDatabase.execSQL("INSERT INTO _sync_state(account_name,account_type,data) SELECT _sync_account, _sync_account_type, data from state_backup;");
            sQLiteDatabase.execSQL("DROP TABLE state_backup;");
            return;
        }
        Log.w("CalendarDatabaseHelper", "upgradeSyncState: current version is " + jLongForQuery + ", skipping upgrade.");
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        bootstrapDB(sQLiteDatabase);
    }

    private void bootstrapDB(SQLiteDatabase sQLiteDatabase) {
        Log.i("CalendarDatabaseHelper", "Bootstrapping database");
        this.mSyncState.createDatabase(sQLiteDatabase);
        createColorsTable(sQLiteDatabase);
        createCalendarsTable(sQLiteDatabase);
        createEventsTable(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TABLE EventsRawTimes (_id INTEGER PRIMARY KEY,event_id INTEGER NOT NULL,dtstart2445 TEXT,dtend2445 TEXT,originalInstanceTime2445 TEXT,lastDate2445 TEXT,UNIQUE (event_id));");
        sQLiteDatabase.execSQL("CREATE TABLE Instances (_id INTEGER PRIMARY KEY,event_id INTEGER,begin INTEGER,end INTEGER,startDay INTEGER,endDay INTEGER,startMinute INTEGER,endMinute INTEGER,UNIQUE (event_id, begin, end));");
        sQLiteDatabase.execSQL("CREATE INDEX instancesStartDayIndex ON Instances (startDay);");
        createCalendarMetaDataTable(sQLiteDatabase);
        createCalendarCacheTable(sQLiteDatabase, null);
        sQLiteDatabase.execSQL("CREATE TABLE Attendees (_id INTEGER PRIMARY KEY,event_id INTEGER,attendeeName TEXT,attendeeEmail TEXT,attendeeStatus INTEGER,attendeeRelationship INTEGER,attendeeType INTEGER,attendeeIdentity TEXT,attendeeIdNamespace TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX attendeesEventIdIndex ON Attendees (event_id);");
        sQLiteDatabase.execSQL("CREATE TABLE Reminders (_id INTEGER PRIMARY KEY,event_id INTEGER,minutes INTEGER,method INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE INDEX remindersEventIdIndex ON Reminders (event_id);");
        sQLiteDatabase.execSQL("CREATE TABLE CalendarAlerts (_id INTEGER PRIMARY KEY,event_id INTEGER,begin INTEGER NOT NULL,end INTEGER NOT NULL,alarmTime INTEGER NOT NULL,creationTime INTEGER NOT NULL DEFAULT 0,receivedTime INTEGER NOT NULL DEFAULT 0,notifyTime INTEGER NOT NULL DEFAULT 0,state INTEGER NOT NULL,minutes INTEGER,UNIQUE (alarmTime, begin, event_id));");
        sQLiteDatabase.execSQL("CREATE INDEX calendarAlertsEventIdIndex ON CalendarAlerts (event_id);");
        sQLiteDatabase.execSQL("CREATE TABLE ExtendedProperties (_id INTEGER PRIMARY KEY,event_id INTEGER,name TEXT,value TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX extendedPropertiesEventIdIndex ON ExtendedProperties (event_id);");
        createEventsView(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        createColorsTriggers(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
        scheduleSync(null, false, null);
    }

    private void createEventsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY AUTOINCREMENT,_sync_id TEXT,dirty INTEGER,mutators TEXT,lastSynced INTEGER DEFAULT 0,calendar_id INTEGER NOT NULL,title TEXT,eventLocation TEXT,description TEXT,eventColor INTEGER,eventColor_index TEXT,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_id INTEGER,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,isOrganizer INTEGER,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,customAppPackage TEXT,customAppUri TEXT,uid2445 TEXT,sync_data1 TEXT,sync_data2 TEXT,sync_data3 TEXT,sync_data4 TEXT,sync_data5 TEXT,sync_data6 TEXT,sync_data7 TEXT,sync_data8 TEXT,sync_data9 TEXT,sync_data10 TEXT,createTime INTEGER,modifyTime INTEGER,isLunar INTEGER NOT NULL DEFAULT 0,lunarRrule TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createEventsTable307(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY AUTOINCREMENT,_sync_id TEXT,dirty INTEGER,lastSynced INTEGER DEFAULT 0,calendar_id INTEGER NOT NULL,title TEXT,eventLocation TEXT,description TEXT,eventColor INTEGER,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_id INTEGER,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,sync_data1 TEXT,sync_data2 TEXT,sync_data3 TEXT,sync_data4 TEXT,sync_data5 TEXT,sync_data6 TEXT,sync_data7 TEXT,sync_data8 TEXT,sync_data9 TEXT,sync_data10 TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createEventsTable300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Events (_id INTEGER PRIMARY KEY,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,dirty INTEGER,_sync_mark INTEGER,calendar_id INTEGER NOT NULL,htmlUri TEXT,title TEXT,eventLocation TEXT,description TEXT,eventStatus INTEGER,selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,commentsUri TEXT,dtstart INTEGER,dtend INTEGER,eventTimezone TEXT,duration TEXT,allDay INTEGER NOT NULL DEFAULT 0,accessLevel INTEGER NOT NULL DEFAULT 0,availability INTEGER NOT NULL DEFAULT 0,hasAlarm INTEGER NOT NULL DEFAULT 0,hasExtendedProperties INTEGER NOT NULL DEFAULT 0,rrule TEXT,rdate TEXT,exrule TEXT,exdate TEXT,original_sync_id TEXT,originalInstanceTime INTEGER,originalAllDay INTEGER,lastDate INTEGER,hasAttendeeData INTEGER NOT NULL DEFAULT 0,guestsCanModify INTEGER NOT NULL DEFAULT 0,guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,organizer STRING,deleted INTEGER NOT NULL DEFAULT 0,eventEndTimezone TEXT,sync_data1 TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createCalendarsTable303(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,dirty INTEGER,name TEXT,displayName TEXT,calendar_color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT);");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createColorsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Colors (_id INTEGER PRIMARY KEY,account_name TEXT NOT NULL,account_type TEXT NOT NULL,data TEXT,color_type INTEGER NOT NULL,color_index TEXT NOT NULL,color INTEGER NOT NULL);");
    }

    public void createColorsTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TRIGGER event_color_update UPDATE OF eventColor_index ON Events WHEN new.eventColor_index NOT NULL BEGIN UPDATE Events SET eventColor=(SELECT color FROM Colors WHERE account_name=(SELECT account_name FROM Calendars WHERE _id=new.calendar_id) AND account_type=(SELECT account_type FROM Calendars WHERE _id=new.calendar_id) AND color_index=new.eventColor_index AND color_type=1)  WHERE _id=old._id; END");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_color_update UPDATE OF calendar_color_index ON Calendars WHEN new.calendar_color_index NOT NULL BEGIN UPDATE Calendars SET calendar_color=(SELECT color FROM Colors WHERE account_name=new.account_name AND account_type=new.account_type AND color_index=new.calendar_color_index AND color_type=0)  WHERE _id=old._id; END");
    }

    private void createCalendarsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,dirty INTEGER,mutators TEXT,name TEXT,calendar_displayName TEXT,calendar_color INTEGER,calendar_color_index TEXT,calendar_access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, isPrimary INTEGER, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,canPartiallyUpdate INTEGER DEFAULT 0,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',allowedAvailability TEXT DEFAULT '0,1',allowedAttendeeTypes TEXT DEFAULT '0,1,2',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT,cal_sync7 TEXT,cal_sync8 TEXT,cal_sync9 TEXT,cal_sync10 TEXT);");
        ExtensionFactory.getCalendarsTableExt("Calendars").tableExtension(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable305(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,dirty INTEGER,name TEXT,calendar_displayName TEXT,calendar_color INTEGER,calendar_access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,canPartiallyUpdate INTEGER DEFAULT 0,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1',deleted INTEGER NOT NULL DEFAULT 0,cal_sync1 TEXT,cal_sync2 TEXT,cal_sync3 TEXT,cal_sync4 TEXT,cal_sync5 TEXT,cal_sync6 TEXT,cal_sync7 TEXT,cal_sync8 TEXT,cal_sync9 TEXT,cal_sync10 TEXT);");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,account_name TEXT,account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,dirty INTEGER,name TEXT,displayName TEXT,calendar_color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,calendar_location TEXT,calendar_timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1,maxReminders INTEGER DEFAULT 5,allowedReminders TEXT DEFAULT '0,1,2',deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT,sync6 TEXT);");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarsTable205(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_dirty INTEGER,name TEXT,displayName TEXT,color INTEGER,access_level INTEGER,visible INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, canOrganizerRespond INTEGER NOT NULL DEFAULT 1,canModifyTimeZone INTEGER DEFAULT 1, maxReminders INTEGER DEFAULT 5,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT,sync6 TEXT);");
        createCalendarsCleanup200(sQLiteDatabase);
    }

    private void createCalendarsTable202(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,_sync_dirty INTEGER,_sync_mark INTEGER,name TEXT,displayName TEXT,color INTEGER,access_level INTEGER,selected INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, organizerCanRespond INTEGER NOT NULL DEFAULT 1,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT,sync4 TEXT,sync5 TEXT);");
        createCalendarsCleanup200(sQLiteDatabase);
    }

    private void createCalendarsTable200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE Calendars (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,_sync_id TEXT,_sync_version TEXT,_sync_time TEXT,_sync_local_id INTEGER,_sync_dirty INTEGER,_sync_mark INTEGER,name TEXT,displayName TEXT,hidden INTEGER NOT NULL DEFAULT 0,color INTEGER,access_level INTEGER,selected INTEGER NOT NULL DEFAULT 1,sync_events INTEGER NOT NULL DEFAULT 0,location TEXT,timezone TEXT,ownerAccount TEXT, organizerCanRespond INTEGER NOT NULL DEFAULT 1,deleted INTEGER NOT NULL DEFAULT 0,sync1 TEXT,sync2 TEXT,sync3 TEXT);");
        createCalendarsCleanup200(sQLiteDatabase);
    }

    private void createCalendarsCleanup200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
    }

    private void createCalendarMetaDataTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE CalendarMetaData (_id INTEGER PRIMARY KEY,localTimezone TEXT,minInstance INTEGER,maxInstance INTEGER);");
    }

    private void createCalendarMetaDataTable59(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE CalendarMetaData (_id INTEGER PRIMARY KEY,localTimezone TEXT,minInstance INTEGER,maxInstance INTEGER);");
    }

    private void createCalendarCacheTable(SQLiteDatabase sQLiteDatabase, String str) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS CalendarCache;");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS CalendarCache (_id INTEGER PRIMARY KEY,key TEXT NOT NULL,value TEXT);");
        initCalendarCacheTable(sQLiteDatabase, str);
        updateCalendarCacheTable(sQLiteDatabase);
    }

    private void initCalendarCacheTable(SQLiteDatabase sQLiteDatabase, String str) {
        if (str == null) {
            str = "2009s";
        }
        sQLiteDatabase.execSQL("INSERT OR REPLACE INTO CalendarCache (_id, key, value) VALUES (" + "timezoneDatabaseVersion".hashCode() + ",'timezoneDatabaseVersion','" + str + "');");
    }

    private void updateCalendarCacheTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneType".hashCode() + ",'timezoneType','auto');");
        String id = TimeZone.getDefault().getID();
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstances".hashCode() + ",'timezoneInstances','" + id + "');");
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstancesPrevious".hashCode() + ",'timezoneInstancesPrevious','" + id + "');");
    }

    private void initCalendarCacheTable203(SQLiteDatabase sQLiteDatabase, String str) {
        if (str == null) {
            str = "2009s";
        }
        sQLiteDatabase.execSQL("INSERT OR REPLACE INTO CalendarCache (_id, key, value) VALUES (" + "timezoneDatabaseVersion".hashCode() + ",'timezoneDatabaseVersion','" + str + "');");
    }

    private void updateCalendarCacheTableTo203(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneType".hashCode() + ",'timezoneType','auto');");
        String id = TimeZone.getDefault().getID();
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstances".hashCode() + ",'timezoneInstances','" + id + "');");
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (_id, key, value) VALUES (" + "timezoneInstancesPrevious".hashCode() + ",'timezoneInstancesPrevious','" + id + "');");
    }

    static void removeOrphans(SQLiteDatabase sQLiteDatabase) {
        Log.d("CalendarDatabaseHelper", "Checking for orphaned entries");
        int iDelete = sQLiteDatabase.delete("Attendees", "event_id IN (SELECT event_id FROM Attendees LEFT OUTER JOIN Events ON event_id=Events._id WHERE Events._id IS NULL)", null);
        if (iDelete != 0) {
            Log.i("CalendarDatabaseHelper", "Deleted " + iDelete + " orphaned Attendees");
        }
        int iDelete2 = sQLiteDatabase.delete("Reminders", "event_id IN (SELECT event_id FROM Reminders LEFT OUTER JOIN Events ON event_id=Events._id WHERE Events._id IS NULL)", null);
        if (iDelete2 != 0) {
            Log.i("CalendarDatabaseHelper", "Deleted " + iDelete2 + " orphaned Reminders");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
        boolean z;
        boolean z2;
        Log.i("CalendarDatabaseHelper", "Upgrading DB from version " + i + " to " + i2);
        long jNanoTime = System.nanoTime();
        if (i < 49) {
            dropTables(sQLiteDatabase);
            bootstrapDB(sQLiteDatabase);
            return;
        }
        boolean z3 = i >= 59 && i <= 66;
        try {
            IDatabaseUpgradeExt databaseUpgradeExt = ExtensionFactory.getDatabaseUpgradeExt();
            int iDowngradeMTKVersionsIfNeeded = databaseUpgradeExt.downgradeMTKVersionsIfNeeded(i, sQLiteDatabase);
            if (iDowngradeMTKVersionsIfNeeded < 51) {
                try {
                    upgradeToVersion51(sQLiteDatabase);
                    iDowngradeMTKVersionsIfNeeded = 51;
                } catch (SQLiteException e) {
                    e = e;
                    i = iDowngradeMTKVersionsIfNeeded;
                    if (this.mInTestMode) {
                        throw e;
                    }
                    Log.e("CalendarDatabaseHelper", "onUpgrade: SQLiteException, recreating db. ", e);
                    Log.e("CalendarDatabaseHelper", "(oldVersion was " + i + ")");
                    dropTables(sQLiteDatabase);
                    bootstrapDB(sQLiteDatabase);
                    return;
                }
            }
            if (iDowngradeMTKVersionsIfNeeded == 51) {
                upgradeToVersion52(sQLiteDatabase);
                iDowngradeMTKVersionsIfNeeded++;
            }
            int iUpgradeToMTKJBVersion = iDowngradeMTKVersionsIfNeeded;
            if (iUpgradeToMTKJBVersion == 52) {
                upgradeToVersion53(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 53) {
                upgradeToVersion54(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 54) {
                upgradeToVersion55(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 55 || iUpgradeToMTKJBVersion == 56) {
                upgradeResync(sQLiteDatabase);
            }
            if (iUpgradeToMTKJBVersion == 55) {
                upgradeToVersion56(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 56) {
                upgradeToVersion57(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 57) {
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 58) {
                upgradeToVersion59(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 59) {
                upgradeToVersion60(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            } else {
                z = false;
            }
            if (iUpgradeToMTKJBVersion == 60) {
                upgradeToVersion61(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 61) {
                upgradeToVersion62(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 62) {
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 63) {
                upgradeToVersion64(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 64) {
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 65) {
                upgradeToVersion66(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 66) {
                iUpgradeToMTKJBVersion++;
            }
            if (z3) {
                recreateMetaDataAndInstances67(sQLiteDatabase);
            }
            if (iUpgradeToMTKJBVersion == 67 || iUpgradeToMTKJBVersion == 68) {
                upgradeToVersion69(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 69;
            }
            if (iUpgradeToMTKJBVersion == 69) {
                upgradeToVersion200(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 200;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 70) {
                upgradeToVersion200(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 200;
            }
            if (iUpgradeToMTKJBVersion == 100) {
                upgradeToVersion200(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 200;
            }
            if (iUpgradeToMTKJBVersion == 101 || iUpgradeToMTKJBVersion == 102) {
                upgradeToVersion200(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 200;
                z2 = false;
            } else {
                z2 = true;
            }
            if (iUpgradeToMTKJBVersion == 200) {
                upgradeToVersion201(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 201) {
                upgradeToVersion202(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 202) {
                if (z2) {
                    upgradeToVersion203(sQLiteDatabase);
                }
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 203) {
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 206) {
                iUpgradeToMTKJBVersion -= 2;
            }
            if (iUpgradeToMTKJBVersion == 204) {
                upgradeToVersion205(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 205) {
                upgradeToVersion300(sQLiteDatabase);
                z = true;
                iUpgradeToMTKJBVersion = 300;
            }
            if (iUpgradeToMTKJBVersion == 300) {
                upgradeToVersion301(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 301) {
                upgradeToVersion302(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 302) {
                upgradeToVersion303(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 303) {
                upgradeToVersion304(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 304) {
                upgradeToVersion305(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 305) {
                upgradeToVersion306(sQLiteDatabase);
                scheduleSync(null, false, null);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 306) {
                upgradeToVersion307(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
            }
            if (iUpgradeToMTKJBVersion == 307) {
                upgradeToVersion308(sQLiteDatabase);
                iUpgradeToMTKJBVersion++;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 308) {
                upgradeToVersion400(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 400;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 309 || iUpgradeToMTKJBVersion == 400) {
                upgradeToVersion401(sQLiteDatabase);
                z = true;
                iUpgradeToMTKJBVersion = 401;
            }
            if (iUpgradeToMTKJBVersion == 401) {
                upgradeToVersion402(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 402;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 402) {
                upgradeToVersion403(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 403;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 403) {
                iUpgradeToMTKJBVersion = databaseUpgradeExt.upgradeToMTKJBVersion(sQLiteDatabase);
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 404) {
                upgradeToVersion501(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 501;
                z = true;
            }
            if (iUpgradeToMTKJBVersion == 501) {
                upgradeToVersion502(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 502;
                z = true;
            }
            if (iUpgradeToMTKJBVersion < 600) {
                upgradeToVersion600(sQLiteDatabase);
                iUpgradeToMTKJBVersion = 600;
                z = true;
            }
            if (z) {
                createEventsView(sQLiteDatabase);
            }
            if (iUpgradeToMTKJBVersion != 600) {
                Log.e("CalendarDatabaseHelper", "Need to recreate Calendar schema because of unknown Calendar database version: " + iUpgradeToMTKJBVersion);
                dropTables(sQLiteDatabase);
                bootstrapDB(sQLiteDatabase);
            } else {
                removeOrphans(sQLiteDatabase);
            }
            Log.d("CalendarDatabaseHelper", "Calendar upgrade took " + ((System.nanoTime() - jNanoTime) / 1000000) + "ms");
        } catch (SQLiteException e2) {
            e = e2;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.i("CalendarDatabaseHelper", "Can't downgrade DB from version " + i + " to " + i2);
        dropTables(sQLiteDatabase);
        bootstrapDB(sQLiteDatabase);
    }

    private void recreateMetaDataAndInstances67(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(sQLiteDatabase);
        sQLiteDatabase.execSQL("DELETE FROM Instances;");
    }

    private static boolean fixAllDayTime(Time time, String str, Long l) {
        time.set(l.longValue());
        if (time.hour == 0 && time.minute == 0 && time.second == 0) {
            return false;
        }
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        return true;
    }

    private void upgradeToVersion600(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN mutators TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN mutators TEXT;");
    }

    private void upgradeToVersion501(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN isOrganizer INTEGER;");
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN isPrimary INTEGER;");
    }

    private void upgradeToVersion502(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN uid2445 TEXT;");
    }

    private void upgradeToVersion403(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN customAppPackage TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN customAppUri TEXT;");
    }

    private void upgradeToVersion402(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdentity TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdNamespace TEXT;");
    }

    private void upgradeToVersion401(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE events SET original_id=(SELECT _id FROM events inner_events WHERE inner_events._sync_id=events.original_sync_id AND inner_events.calendar_id=events.calendar_id) WHERE NOT original_id IS NULL AND (SELECT calendar_id FROM events ex_events WHERE ex_events._id=events.original_id) <> calendar_id ");
    }

    private void upgradeToVersion400(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_color_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_color_update UPDATE OF calendar_color_index ON Calendars WHEN new.calendar_color_index NOT NULL BEGIN UPDATE Calendars SET calendar_color=(SELECT color FROM Colors WHERE account_name=new.account_name AND account_type=new.account_type AND color_index=new.calendar_color_index AND color_type=0)  WHERE _id=old._id; END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS event_color_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER event_color_update UPDATE OF eventColor_index ON Events WHEN new.eventColor_index NOT NULL BEGIN UPDATE Events SET eventColor=(SELECT color FROM Colors WHERE account_name=(SELECT account_name FROM Calendars WHERE _id=new.calendar_id) AND account_type=(SELECT account_type FROM Calendars WHERE _id=new.calendar_id) AND color_index=new.eventColor_index AND color_type=1)  WHERE _id=old._id; END");
    }

    private void upgradeToVersion308(SQLiteDatabase sQLiteDatabase) {
        createColorsTable(sQLiteDatabase);
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAvailability TEXT DEFAULT '0,1';");
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAttendeeTypes TEXT DEFAULT '0,1,2';");
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN calendar_color_index TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN eventColor_index TEXT;");
        sQLiteDatabase.execSQL("UPDATE Calendars SET allowedAvailability='0,1,2' WHERE _id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
        createColorsTriggers(sQLiteDatabase);
    }

    private void upgradeToVersion307(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable307(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Events (_id, _sync_id, dirty, lastSynced,calendar_id, title, eventLocation, description, eventColor, eventStatus, selfAttendeeStatus, dtstart, dtend, eventTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, eventEndTimezone, sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10 ) SELECT _id, _sync_id, dirty, lastSynced,calendar_id, title, eventLocation, description, eventColor, eventStatus, selfAttendeeStatus, dtstart, dtend, eventTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, eventEndTimezone, sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10 FROM Events_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Events_Backup;");
        sQLiteDatabase.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        sQLiteDatabase.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion306(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        sQLiteDatabase.execSQL("UPDATE Events SET _sync_id = REPLACE(_sync_id, '/private/full/', '/events/'), original_sync_id = REPLACE(original_sync_id, '/private/full/', '/events/') WHERE _id IN (SELECT Events._id FROM Events JOIN Calendars ON Events.calendar_id = Calendars._id WHERE account_type = 'com.google')");
        sQLiteDatabase.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
        sQLiteDatabase.execSQL("UPDATE Calendars SET canPartiallyUpdate = 1 WHERE account_type = 'com.google'");
        sQLiteDatabase.execSQL("DELETE FROM _sync_state WHERE account_type = 'com.google'");
    }

    private void upgradeToVersion305(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable305(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, cal_sync7, cal_sync8, dirty, name, calendar_displayName, calendar_color, calendar_access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, canPartiallyUpdate,cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6) SELECT _id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, canPartiallyUpdate,cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6 FROM Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
        sQLiteDatabase.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable307(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Events (_id, _sync_id, sync_data4, sync_data5, sync_data2, dirty, sync_data8, calendar_id, sync_data3, title, eventLocation, description, eventStatus, selfAttendeeStatus, sync_data6, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data7,lastSynced,sync_data1) SELECT _id, _sync_id, _sync_version, _sync_time, _sync_local_id, dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_id,original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data7,lastSynced,sync_data1 FROM Events_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Events_Backup;");
        sQLiteDatabase.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        sQLiteDatabase.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion304(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN canPartiallyUpdate INTEGER DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN sync_data7 TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN lastSynced INTEGER DEFAULT 0;");
    }

    private void upgradeToVersion303(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable303(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders, deleted, cal_sync1, cal_sync2, cal_sync3, cal_sync4, cal_sync5, cal_sync6) SELECT _id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders,deleted, sync1, sync2, sync3, sync4,sync5,sync6 FROM Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion302(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE Events SET sync_data1=eventEndTimezone WHERE calendar_id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
        sQLiteDatabase.execSQL("UPDATE Events SET eventEndTimezone=NULL WHERE calendar_id IN (SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
    }

    private void upgradeToVersion301(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS original_sync_update;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN original_id INTEGER;");
        sQLiteDatabase.execSQL("UPDATE Events set original_id=(SELECT Events2._id FROM Events AS Events2 WHERE Events2._sync_id=Events.original_sync_id) WHERE Events.original_sync_id NOT NULL");
        sQLiteDatabase.execSQL("CREATE TRIGGER original_sync_update UPDATE OF _sync_id ON Events BEGIN UPDATE Events SET original_sync_id=new._sync_id WHERE original_id=old._id; END");
    }

    private void upgradeToVersion300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup;");
        createCalendarsTable300(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, account_name, account_type, _sync_id, _sync_version, _sync_time, dirty, name, displayName, calendar_color, access_level, visible, sync_events, calendar_location, calendar_timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, allowedReminders,deleted, sync1, sync2, sync3, sync4,sync5,sync6) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, visible, sync_events, location, timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, '0,1,2,3',deleted, sync1, sync2, sync3, sync4, sync5, sync6 FROM Calendars_Backup;");
        sQLiteDatabase.execSQL("UPDATE Calendars SET allowedReminders = '0,1,2' WHERE account_type = 'com.google'");
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
        sQLiteDatabase.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS eventSyncAccountAndIdIndex");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable300(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Events (_id, _sync_id, _sync_version, _sync_time, _sync_local_id, dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventEndTimezone, duration, allDay, accessLevel, availability, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, original_sync_id, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, sync_data1) SELECT _id, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, calendar_id, htmlUri, title, eventLocation, description, eventStatus, selfAttendeeStatus, commentsUri, dtstart, dtend, eventTimezone, eventTimezone2, duration, allDay, visibility, transparency, hasAlarm, hasExtendedProperties, rrule, rdate, exrule, exdate, originalEvent, originalInstanceTime, originalAllDay, lastDate, hasAttendeeData, guestsCanModify, guestsCanInviteOthers, guestsCanSeeGuests, organizer, deleted, syncAdapterData FROM Events_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Events_Backup;");
        sQLiteDatabase.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
    }

    private void upgradeToVersion205(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable205(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, visible, sync_events, location, timezone, ownerAccount, canOrganizerRespond, canModifyTimeZone, maxReminders, deleted, sync1, sync2, sync3, sync4,sync5,sync6) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_dirty, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, 1, 5, deleted, sync1, sync2, sync3, sync4, sync5, _sync_mark FROM Calendars_Backup;");
        sQLiteDatabase.execSQL("UPDATE Calendars SET canModifyTimeZone=0, maxReminders=1 WHERE _sync_account_type='com.android.exchange'");
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion203(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT value FROM CalendarCache WHERE key=?", new String[]{"timezoneDatabaseVersion"});
        String str = null;
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.moveToNext()) {
                    String string = cursorRawQuery.getString(0);
                    cursorRawQuery.close();
                    try {
                        sQLiteDatabase.execSQL("DELETE FROM CalendarCache;");
                        cursorRawQuery = null;
                        str = string;
                    } catch (Throwable th) {
                        th = th;
                        cursorRawQuery = null;
                        if (cursorRawQuery != null) {
                            cursorRawQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        initCalendarCacheTable203(sQLiteDatabase, str);
        updateCalendarCacheTableTo203(sQLiteDatabase);
    }

    private void upgradeToVersion202(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable202(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1, sync2, sync3, sync4,sync5) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1, sync2, sync3, sync4, hidden FROM Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion201(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN sync4 TEXT;");
    }

    private void upgradeToVersion200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable200(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO Calendars (_id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, deleted, sync1) SELECT _id, _sync_account, _sync_account_type, _sync_id, _sync_version, _sync_time, _sync_local_id, _sync_dirty, _sync_mark, name, displayName, color, access_level, selected, sync_events, location, timezone, ownerAccount, organizerCanRespond, 0, url FROM Calendars_Backup;");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, url FROM Calendars_Backup WHERE _sync_account_type='com.google' AND url IS NOT NULL;", null);
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.getCount() > 0) {
                    Object[] objArr = new Object[3];
                    while (cursorRawQuery.moveToNext()) {
                        Long lValueOf = Long.valueOf(cursorRawQuery.getLong(0));
                        String string = cursorRawQuery.getString(1);
                        String selfUrlFromEventsUrl = getSelfUrlFromEventsUrl(string);
                        objArr[0] = getEditUrlFromEventsUrl(string);
                        objArr[1] = selfUrlFromEventsUrl;
                        objArr[2] = lValueOf;
                        sQLiteDatabase.execSQL("UPDATE Calendars SET sync2=?, sync3=? WHERE _id=?;", objArr);
                    }
                }
            } finally {
                cursorRawQuery.close();
            }
        }
        sQLiteDatabase.execSQL("DROP TABLE Calendars_Backup;");
    }

    public static void upgradeToVersion69(SQLiteDatabase sQLiteDatabase) {
        String str;
        Long lValueOf;
        Long lValueOf2;
        boolean z;
        Object obj;
        String str2;
        Long lValueOf3;
        boolean z2;
        String str3;
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, dtstart, dtend, duration, dtstart2, dtend2, eventTimezone, eventTimezone2, rrule FROM Events WHERE allDay=?", new String[]{"1"});
        if (cursorRawQuery != null) {
            try {
                Time time = new Time();
                while (cursorRawQuery.moveToNext()) {
                    String string = cursorRawQuery.getString(8);
                    Object objValueOf = Long.valueOf(cursorRawQuery.getLong(0));
                    Long lValueOf4 = Long.valueOf(cursorRawQuery.getLong(1));
                    String string2 = cursorRawQuery.getString(6);
                    String string3 = cursorRawQuery.getString(7);
                    String string4 = cursorRawQuery.getString(3);
                    if (TextUtils.isEmpty(string)) {
                        Long lValueOf5 = Long.valueOf(cursorRawQuery.getLong(2));
                        if (!TextUtils.isEmpty(string3)) {
                            str = string4;
                            lValueOf = Long.valueOf(cursorRawQuery.getLong(4));
                            lValueOf2 = Long.valueOf(cursorRawQuery.getLong(5));
                        } else {
                            str = string4;
                            lValueOf = null;
                            lValueOf2 = null;
                        }
                        if (TextUtils.equals(string2, "UTC")) {
                            z = false;
                        } else {
                            string2 = "UTC";
                            z = true;
                        }
                        time.clear(string2);
                        boolean zFixAllDayTime = fixAllDayTime(time, string2, lValueOf4) | z;
                        Object objValueOf2 = Long.valueOf(time.normalize(false));
                        time.clear(string2);
                        boolean zFixAllDayTime2 = fixAllDayTime(time, string2, lValueOf5) | zFixAllDayTime;
                        String str4 = string2;
                        Object objValueOf3 = Long.valueOf(time.normalize(false));
                        if (lValueOf != null) {
                            str2 = string3;
                            time.clear(str2);
                            zFixAllDayTime2 |= fixAllDayTime(time, str2, lValueOf);
                            obj = objValueOf3;
                            lValueOf = Long.valueOf(time.normalize(false));
                        } else {
                            obj = objValueOf3;
                            str2 = string3;
                        }
                        if (lValueOf2 != null) {
                            time.clear(str2);
                            zFixAllDayTime2 |= fixAllDayTime(time, str2, lValueOf2);
                            lValueOf2 = Long.valueOf(time.normalize(false));
                        }
                        boolean z3 = zFixAllDayTime2;
                        if (!TextUtils.isEmpty(str)) {
                            z3 = true;
                        }
                        if (z3) {
                            sQLiteDatabase.execSQL("UPDATE Events SET dtstart=?, dtend=?, dtstart2=?, dtend2=?, duration=?, eventTimezone=?, eventTimezone2=? WHERE _id=?", new Object[]{objValueOf2, obj, lValueOf, lValueOf2, null, str4, str2, objValueOf});
                        }
                    } else {
                        if (!TextUtils.isEmpty(string3)) {
                            lValueOf3 = Long.valueOf(cursorRawQuery.getLong(4));
                        } else {
                            lValueOf3 = null;
                        }
                        if (TextUtils.equals(string2, "UTC")) {
                            z2 = false;
                        } else {
                            string2 = "UTC";
                            z2 = true;
                        }
                        time.clear(string2);
                        boolean zFixAllDayTime3 = z2 | fixAllDayTime(time, string2, lValueOf4);
                        Object objValueOf4 = Long.valueOf(time.normalize(false));
                        if (lValueOf3 != null) {
                            time.clear(string3);
                            zFixAllDayTime3 = fixAllDayTime(time, string3, lValueOf3) | zFixAllDayTime3;
                            lValueOf3 = Long.valueOf(time.normalize(false));
                        }
                        if (TextUtils.isEmpty(string4)) {
                            str3 = "P1D";
                        } else {
                            int length = string4.length();
                            if (string4.charAt(0) == 'P') {
                                int i = length - 1;
                                if (string4.charAt(i) == 'S') {
                                    str3 = "P" + (((Integer.parseInt(string4.substring(1, i)) + 86400) - 1) / 86400) + "D";
                                }
                            }
                            str3 = string4;
                            if (!zFixAllDayTime3) {
                                sQLiteDatabase.execSQL("UPDATE Events SET dtstart=?, dtend=?, dtstart2=?, dtend2=?, duration=?,eventTimezone=?, eventTimezone2=? WHERE _id=?", new Object[]{objValueOf4, null, lValueOf3, null, str3, string2, string3, objValueOf});
                            }
                        }
                        zFixAllDayTime3 = true;
                        if (!zFixAllDayTime3) {
                        }
                    }
                }
            } finally {
                cursorRawQuery.close();
            }
        }
    }

    private void upgradeToVersion66(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN organizerCanRespond INTEGER NOT NULL DEFAULT 1;");
    }

    private void upgradeToVersion64(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN syncAdapterData TEXT;");
    }

    private void upgradeToVersion62(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN dtstart2 INTEGER;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN dtend2 INTEGER;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN eventTimezone2 TEXT;");
        String[] strArr = {"0"};
        sQLiteDatabase.execSQL("UPDATE Events SET dtstart2=dtstart,dtend2=dtend,eventTimezone2=eventTimezone WHERE allDay=?;", strArr);
        strArr[0] = "1";
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT Events._id,dtstart,dtend,eventTimezone,timezone FROM Events INNER JOIN Calendars WHERE Events.calendar_id=Calendars._id AND allDay=?", strArr);
        Time time = new Time();
        Time time2 = new Time();
        if (cursorRawQuery != null) {
            int i = 4;
            try {
                Object[] objArr = new String[4];
                cursorRawQuery.moveToPosition(-1);
                while (cursorRawQuery.moveToNext()) {
                    long j = cursorRawQuery.getLong(0);
                    long j2 = cursorRawQuery.getLong(1);
                    long j3 = cursorRawQuery.getLong(2);
                    String string = cursorRawQuery.getString(3);
                    String string2 = cursorRawQuery.getString(i);
                    if (string == null) {
                        string = "UTC";
                    }
                    String str = string;
                    time.clear(str);
                    time.set(j2);
                    time2.clear(string2);
                    time2.set(time.monthDay, time.month, time.year);
                    time2.normalize(false);
                    long millis = time2.toMillis(false);
                    time.clear(str);
                    time.set(j3);
                    time2.clear(string2);
                    time2.set(time.monthDay, time.month, time.year);
                    time2.normalize(false);
                    long millis2 = time2.toMillis(false);
                    objArr[0] = String.valueOf(millis);
                    objArr[1] = String.valueOf(millis2);
                    objArr[2] = string2;
                    objArr[3] = String.valueOf(j);
                    sQLiteDatabase.execSQL("UPDATE Events SET dtstart2=?, dtend2=?, eventTimezone2=? WHERE _id=?", objArr);
                    i = 4;
                }
            } finally {
                cursorRawQuery.close();
            }
        }
    }

    private void upgradeToVersion61(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS CalendarCache;");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS CalendarCache (_id INTEGER PRIMARY KEY,key TEXT NOT NULL,value TEXT);");
        sQLiteDatabase.execSQL("INSERT INTO CalendarCache (key, value) VALUES ('timezoneDatabaseVersion','2009s');");
    }

    private void upgradeToVersion60(SQLiteDatabase sQLiteDatabase) {
        upgradeSyncState(sQLiteDatabase);
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;END");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER events_insert AFTER INSERT ON Events BEGIN UPDATE Events SET _sync_account= (SELECT _sync_account FROM Calendars WHERE Calendars._id=new.calendar_id),_sync_account_type= (SELECT _sync_account_type FROM Calendars WHERE Calendars._id=new.calendar_id) WHERE Events._id=new._id;END");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS DeletedEvents;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        sQLiteDatabase.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events BEGIN DELETE FROM Instances WHERE event_id=old._id;DELETE FROM EventsRawTimes WHERE event_id=old._id;DELETE FROM Attendees WHERE event_id=old._id;DELETE FROM Reminders WHERE event_id=old._id;DELETE FROM CalendarAlerts WHERE event_id=old._id;DELETE FROM ExtendedProperties WHERE event_id=old._id;END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS attendees_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS attendees_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS attendees_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS reminders_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS reminders_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS reminders_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS extended_properties_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS extended_properties_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS extended_properties_delete");
    }

    private void upgradeToVersion59(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS BusyBits;");
        sQLiteDatabase.execSQL("CREATE TEMPORARY TABLE CalendarMetaData_Backup(_id,localTimezone,minInstance,maxInstance);");
        sQLiteDatabase.execSQL("INSERT INTO CalendarMetaData_Backup SELECT _id,localTimezone,minInstance,maxInstance FROM CalendarMetaData;");
        sQLiteDatabase.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO CalendarMetaData SELECT _id,localTimezone,minInstance,maxInstance FROM CalendarMetaData_Backup;");
        sQLiteDatabase.execSQL("DROP TABLE CalendarMetaData_Backup;");
    }

    private void upgradeToVersion57(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN guestsCanModify INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN organizer STRING;");
        sQLiteDatabase.execSQL("UPDATE Events SET organizer=(SELECT attendeeEmail FROM Attendees WHERE Attendees.event_id=Events._id AND Attendees.attendeeRelationship=2);");
    }

    private void upgradeToVersion56(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN ownerAccount TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN hasAttendeeData INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("UPDATE Events SET _sync_dirty=0, _sync_version=NULL, _sync_id=REPLACE(_sync_id, '/private/full-selfattendance', '/private/full'),commentsUri=REPLACE(commentsUri, '/private/full-selfattendance', '/private/full');");
        sQLiteDatabase.execSQL("UPDATE Calendars SET url=REPLACE(url, '/private/full-selfattendance', '/private/full');");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, url FROM Calendars", null);
        if (cursorRawQuery != null) {
            while (cursorRawQuery.moveToNext()) {
                try {
                    sQLiteDatabase.execSQL("UPDATE Calendars SET ownerAccount=? WHERE _id=?", new Object[]{calendarEmailAddressFromFeedUrl(cursorRawQuery.getString(1)), Long.valueOf(cursorRawQuery.getLong(0))});
                } finally {
                    cursorRawQuery.close();
                }
            }
        }
    }

    private void upgradeResync(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DELETE FROM _sync_state;");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _sync_account,_sync_account_type,url FROM Calendars", null);
        if (cursorRawQuery != null) {
            while (cursorRawQuery.moveToNext()) {
                try {
                    scheduleSync(new Account(cursorRawQuery.getString(0), cursorRawQuery.getString(1)), false, cursorRawQuery.getString(2));
                } finally {
                    cursorRawQuery.close();
                }
            }
        }
    }

    private void upgradeToVersion55(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Calendars ADD COLUMN _sync_account_type TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN _sync_account_type TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE DeletedEvents ADD COLUMN _sync_account_type TEXT;");
        sQLiteDatabase.execSQL("UPDATE Calendars SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        sQLiteDatabase.execSQL("UPDATE Events SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        sQLiteDatabase.execSQL("UPDATE DeletedEvents SET _sync_account_type='com.google' WHERE _sync_account IS NOT NULL");
        Log.w("CalendarDatabaseHelper", "re-creating eventSyncAccountAndIdIndex");
        sQLiteDatabase.execSQL("DROP INDEX eventSyncAccountAndIdIndex");
        sQLiteDatabase.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events (_sync_account_type, _sync_account, _sync_id);");
    }

    private void upgradeToVersion54(SQLiteDatabase sQLiteDatabase) {
        Log.w("CalendarDatabaseHelper", "adding eventSyncAccountAndIdIndex");
        sQLiteDatabase.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events (_sync_account, _sync_id);");
    }

    private void upgradeToVersion53(SQLiteDatabase sQLiteDatabase) {
        Log.w("CalendarDatabaseHelper", "Upgrading CalendarAlerts table");
        sQLiteDatabase.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN creationTime INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN receivedTime INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN notifyTime INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion52(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN originalAllDay INTEGER;");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id,originalEvent FROM Events WHERE originalEvent IS NOT NULL", null);
        if (cursorRawQuery != null) {
            while (cursorRawQuery.moveToNext()) {
                try {
                    long j = cursorRawQuery.getLong(0);
                    cursorRawQuery = sQLiteDatabase.rawQuery("SELECT allDay FROM Events WHERE _sync_id=?", new String[]{cursorRawQuery.getString(1)});
                    if (cursorRawQuery != null) {
                        if (cursorRawQuery.moveToNext()) {
                            sQLiteDatabase.execSQL("UPDATE Events SET originalAllDay=" + cursorRawQuery.getInt(0) + " WHERE _id=" + j);
                        }
                        cursorRawQuery.close();
                    }
                } catch (Throwable th) {
                    throw th;
                } finally {
                    cursorRawQuery.close();
                }
            }
        }
    }

    private void upgradeToVersion51(SQLiteDatabase sQLiteDatabase) {
        Log.w("CalendarDatabaseHelper", "Upgrading DeletedEvents table");
        sQLiteDatabase.execSQL("ALTER TABLE DeletedEvents ADD COLUMN calendar_id INTEGER;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        sQLiteDatabase.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars BEGIN DELETE FROM Events WHERE calendar_id=old._id;DELETE FROM DeletedEvents WHERE calendar_id = old._id;END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS event_to_deleted");
    }

    private void dropTables(SQLiteDatabase sQLiteDatabase) {
        Log.i("CalendarDatabaseHelper", "Clearing database");
        Cursor cursorQuery = sQLiteDatabase.query("sqlite_master", new String[]{"type", "name"}, null, null, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        while (cursorQuery.moveToNext()) {
            try {
                String string = cursorQuery.getString(1);
                if (!string.startsWith("sqlite_")) {
                    String str = "DROP " + cursorQuery.getString(0) + " IF EXISTS " + string;
                    try {
                        sQLiteDatabase.execSQL(str);
                    } catch (SQLException e) {
                        Log.e("CalendarDatabaseHelper", "Error executing " + str + " " + e.toString());
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }

    public SyncStateContentProviderHelper getSyncState() {
        return this.mSyncState;
    }

    void scheduleSync(Account account, boolean z, String str) {
        Bundle bundle = new Bundle();
        if (z) {
            bundle.putBoolean("upload", z);
        }
        if (str != null) {
            bundle.putString("feed", str);
        }
        ContentResolver.requestSync(account, CalendarContract.Calendars.CONTENT_URI.getAuthority(), bundle);
    }

    private static void createEventsView(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_events;");
        sQLiteDatabase.execSQL("CREATE VIEW view_events AS SELECT Events._id AS _id,title,description,eventLocation,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,duration,eventTimezone,eventEndTimezone,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,calendar_id,guestsCanInviteOthers,guestsCanModify,guestsCanSeeGuests,organizer,COALESCE(isOrganizer, organizer = ownerAccount) AS isOrganizer,customAppPackage,customAppUri,uid2445,sync_data1,sync_data2,sync_data3,sync_data4,sync_data5,sync_data6,sync_data7,sync_data8,sync_data9,sync_data10,createTime,modifyTime,isLunar,lunarRrule,Events.deleted AS deleted,Events._sync_id AS _sync_id,Events.dirty AS dirty,Events.mutators AS mutators,lastSynced,Calendars.account_name AS account_name,Calendars.account_type AS account_type,calendar_timezone,calendar_displayName,calendar_location,visible,calendar_color,calendar_color_index,calendar_access_level,maxReminders,allowedReminders,allowedAttendeeTypes,allowedAvailability,canOrganizerRespond,canModifyTimeZone,canPartiallyUpdate,cal_sync1,cal_sync2,cal_sync3,cal_sync4,cal_sync5,cal_sync6,cal_sync7,cal_sync8,cal_sync9,cal_sync10,ownerAccount,sync_events,ifnull(eventColor,calendar_color) AS displayColor FROM Events JOIN Calendars ON (Events.calendar_id=Calendars._id)");
    }

    public static String calendarEmailAddressFromFeedUrl(String str) {
        String[] strArrSplit = str.split("/");
        if (strArrSplit.length > 5 && "feeds".equals(strArrSplit[4])) {
            try {
                return URLDecoder.decode(strArrSplit[5], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("CalendarDatabaseHelper", "unable to url decode the email address in calendar " + str);
                return null;
            }
        }
        Log.e("CalendarDatabaseHelper", "unable to find the email address in calendar " + str);
        return null;
    }

    private static String getAllCalendarsUrlFromEventsUrl(String str) {
        if (str == null) {
            if (Log.isLoggable("CalendarDatabaseHelper", 3)) {
                Log.d("CalendarDatabaseHelper", "Cannot get AllCalendars url from a NULL url");
            }
            return null;
        }
        if (str.contains("/private/full")) {
            return str.replace("/private/full", "").replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        if (str.contains("/private/free-busy")) {
            return str.replace("/private/free-busy", "").replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        if (Log.isLoggable("CalendarDatabaseHelper", 3)) {
            Log.d("CalendarDatabaseHelper", "Cannot get AllCalendars url from the following url: " + str);
        }
        return null;
    }

    private static String getSelfUrlFromEventsUrl(String str) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(str));
    }

    private static String getEditUrlFromEventsUrl(String str) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(str));
    }

    private static String rewriteUrlFromHttpToHttps(String str) {
        if (str == null) {
            if (Log.isLoggable("CalendarDatabaseHelper", 3)) {
                Log.d("CalendarDatabaseHelper", "Cannot rewrite a NULL url");
                return null;
            }
            return null;
        }
        if (str.startsWith("https://")) {
            return str;
        }
        if (!str.startsWith("http://")) {
            throw new IllegalArgumentException("invalid url parameter, unknown scheme: " + str);
        }
        return "https://" + str.substring("http://".length());
    }

    protected void duplicateEvent(long j) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        try {
            if (DatabaseUtils.longForQuery(writableDatabase, "SELECT canPartiallyUpdate FROM view_events WHERE _id = ?", new String[]{String.valueOf(j)}) == 0) {
                return;
            }
            writableDatabase.execSQL("INSERT INTO Events  (_sync_id,calendar_id,title,eventLocation,description,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,eventTimezone,eventEndTimezone,duration,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,guestsCanModify,guestsCanInviteOthers,guestsCanSeeGuests,organizer,isOrganizer,customAppPackage,customAppUri,uid2445,isLunar,lunarRrule,createTime,modifyTime,dirty,lastSynced) SELECT _sync_id,calendar_id,title,eventLocation,description,eventColor,eventColor_index,eventStatus,selfAttendeeStatus,dtstart,dtend,eventTimezone,eventEndTimezone,duration,allDay,accessLevel,availability,hasAlarm,hasExtendedProperties,rrule,rdate,exrule,exdate,original_sync_id,original_id,originalInstanceTime,originalAllDay,lastDate,hasAttendeeData,guestsCanModify,guestsCanInviteOthers,guestsCanSeeGuests,organizer,isOrganizer,customAppPackage,customAppUri,uid2445,isLunar,lunarRrule,createTime,modifyTime, 0, 1 FROM Events WHERE _id = ? AND dirty = ?", new Object[]{Long.valueOf(j), 0});
            long jLongForQuery = DatabaseUtils.longForQuery(writableDatabase, "SELECT CASE changes() WHEN 0 THEN -1 ELSE last_insert_rowid() END", null);
            if (jLongForQuery < 0) {
                return;
            }
            if (Log.isLoggable("CalendarDatabaseHelper", 2)) {
                Log.v("CalendarDatabaseHelper", "Duplicating event " + j + " into new event " + jLongForQuery);
            }
            copyEventRelatedTables(writableDatabase, jLongForQuery, j);
        } catch (SQLiteDoneException e) {
        }
    }

    static void copyEventRelatedTables(SQLiteDatabase sQLiteDatabase, long j, long j2) {
        sQLiteDatabase.execSQL("INSERT INTO Reminders ( event_id, minutes,method) SELECT ?,minutes,method FROM Reminders WHERE event_id = ?", new Object[]{Long.valueOf(j), Long.valueOf(j2)});
        sQLiteDatabase.execSQL("INSERT INTO Attendees (event_id,attendeeName,attendeeEmail,attendeeStatus,attendeeRelationship,attendeeType,attendeeIdentity,attendeeIdNamespace) SELECT ?,attendeeName,attendeeEmail,attendeeStatus,attendeeRelationship,attendeeType,attendeeIdentity,attendeeIdNamespace FROM Attendees WHERE event_id = ?", new Object[]{Long.valueOf(j), Long.valueOf(j2)});
        sQLiteDatabase.execSQL("INSERT INTO ExtendedProperties (event_id,name,value) SELECT ?, name,value FROM ExtendedProperties WHERE event_id = ?", new Object[]{Long.valueOf(j), Long.valueOf(j2)});
    }

    protected void removeDuplicateEvent(long j) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT _id FROM Events WHERE _sync_id = (SELECT _sync_id FROM Events WHERE _id = ?) AND lastSynced = ?", new String[]{String.valueOf(j), "1"});
        try {
            if (cursorRawQuery.moveToNext()) {
                long j2 = cursorRawQuery.getLong(0);
                if (Log.isLoggable("CalendarDatabaseHelper", 2)) {
                    Log.v("CalendarDatabaseHelper", "Removing duplicate event " + j2 + " of original event " + j);
                }
                writableDatabase.execSQL("DELETE FROM Events WHERE _id = ?", new Object[]{Long.valueOf(j2)});
            }
        } finally {
            cursorRawQuery.close();
        }
    }
}
