package com.android.calendar.alerts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class AlertService extends Service {
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;
    static final String[] ALERT_PROJECTION = {"_id", "event_id", "state", "title", "eventLocation", "selfAttendeeStatus", "allDay", "alarmTime", "minutes", "begin", "end", "description"};
    private static final String[] ACTIVE_ALERTS_SELECTION_ARGS = {Integer.toString(1), Integer.toString(0)};
    private static Boolean sReceivedProviderReminderBroadcast = null;

    public static class NotificationWrapper {
        long mBegin;
        long mEnd;
        long mEventId;
        Notification mNotification;
        ArrayList<NotificationWrapper> mNw;

        public NotificationWrapper(Notification n, int notificationId, long eventId, long startMillis, long endMillis, boolean doPopup) {
            this.mNotification = n;
            this.mEventId = eventId;
            this.mBegin = startMillis;
            this.mEnd = endMillis;
        }

        public NotificationWrapper(Notification n) {
            this.mNotification = n;
        }

        public void add(NotificationWrapper nw) {
            if (this.mNw == null) {
                this.mNw = new ArrayList<>();
            }
            this.mNw.add(nw);
        }
    }

    public static class NotificationMgrWrapper extends NotificationMgr {
        NotificationManager mNm;

        public NotificationMgrWrapper(NotificationManager nm) {
            this.mNm = nm;
        }

        @Override
        public void cancel(int id) {
            this.mNm.cancel(id);
        }

        @Override
        public void notify(int id, NotificationWrapper nw) {
            this.mNm.notify(id, nw.mNotification);
        }
    }

    void processMessage(Message msg) {
        Bundle bundle = (Bundle) msg.obj;
        String action = bundle.getString("action");
        Log.d("AlertService", bundle.getLong("alarmTime") + " Action = " + action);
        boolean providerReminder = action.equals("android.intent.action.EVENT_REMINDER");
        if (providerReminder) {
            if (sReceivedProviderReminderBroadcast == null) {
                sReceivedProviderReminderBroadcast = Boolean.valueOf(Utils.getSharedPreference((Context) this, "preference_received_provider_reminder_broadcast", false));
            }
            if (!sReceivedProviderReminderBroadcast.booleanValue()) {
                sReceivedProviderReminderBroadcast = true;
                Log.d("AlertService", "Setting key preference_received_provider_reminder_broadcast to: true");
                Utils.setSharedPreference((Context) this, "preference_received_provider_reminder_broadcast", true);
            }
        }
        if (providerReminder || action.equals("android.intent.action.PROVIDER_CHANGED") || action.equals("android.intent.action.EVENT_REMINDER") || action.equals("com.android.calendar.EVENT_REMINDER_APP") || action.equals("android.intent.action.LOCALE_CHANGED")) {
            if (action.equals("android.intent.action.PROVIDER_CHANGED")) {
                try {
                    Thread.sleep(5000L);
                } catch (Exception e) {
                }
            }
            GlobalDismissManager.syncSenderDismissCache(this);
            updateAlertNotification(this);
        } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Intent intent = new Intent();
            intent.setClass(this, InitAlarmsService.class);
            startService(intent);
        } else if (action.equals("android.intent.action.TIME_SET")) {
            doTimeChanged();
        } else if (action.equals("removeOldReminders")) {
            dismissOldAlerts(this);
        } else {
            Log.w("AlertService", "Invalid action: " + action);
        }
        if (sReceivedProviderReminderBroadcast == null || !sReceivedProviderReminderBroadcast.booleanValue()) {
            Log.d("AlertService", "Scheduling next alarm with AlarmScheduler. sEventReminderReceived: " + sReceivedProviderReminderBroadcast);
            AlarmScheduler.scheduleNextAlarm(this);
        }
    }

    static void dismissOldAlerts(Context context) {
        ContentResolver cr = context.getContentResolver();
        long currentTime = System.currentTimeMillis();
        ContentValues vals = new ContentValues();
        vals.put("state", (Integer) 2);
        cr.update(CalendarContract.CalendarAlerts.CONTENT_URI, vals, "end<? AND state=?", new String[]{Long.toString(currentTime), Integer.toString(0)});
    }

    static boolean updateAlertNotification(Context context) {
        ContentResolver cr = context.getContentResolver();
        NotificationMgr nm = new NotificationMgrWrapper((NotificationManager) context.getSystemService("notification"));
        long currentTime = System.currentTimeMillis();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        Log.d("AlertService", "Beginning updateAlertNotification");
        if (!prefs.getBoolean("preferences_alerts", true)) {
            Log.d("AlertService", "alert preference is OFF");
            nm.cancelAll();
            return true;
        }
        GlobalDismissManager.syncReceiverDismissCache(context);
        Cursor alertCursor = cr.query(CalendarContract.CalendarAlerts.CONTENT_URI, ALERT_PROJECTION, "(state=? OR state=?) AND alarmTime<=" + currentTime, ACTIVE_ALERTS_SELECTION_ARGS, "begin DESC, end DESC");
        if (alertCursor == null || alertCursor.getCount() == 0) {
            if (alertCursor != null) {
                alertCursor.close();
            }
            Log.d("AlertService", "No fired or scheduled alerts");
            nm.cancelAll();
            return false;
        }
        return generateAlerts(context, nm, AlertUtils.createAlarmManager(context), prefs, alertCursor, currentTime, 20);
    }

    public static boolean generateAlerts(Context context, NotificationMgr nm, AlarmManagerInterface alarmMgr, SharedPreferences prefs, Cursor alertCursor, long currentTime, int maxNotifications) {
        NotificationWrapper notification;
        Log.d("AlertService", "alertCursor count:" + alertCursor.getCount());
        ArrayList<NotificationInfo> highPriorityEvents = new ArrayList<>();
        ArrayList<NotificationInfo> mediumPriorityEvents = new ArrayList<>();
        ArrayList<NotificationInfo> lowPriorityEvents = new ArrayList<>();
        int numFired = processQuery(alertCursor, context, currentTime, highPriorityEvents, mediumPriorityEvents, lowPriorityEvents);
        if (highPriorityEvents.size() + mediumPriorityEvents.size() + lowPriorityEvents.size() == 0) {
            nm.cancelAll();
            return true;
        }
        long nextRefreshTime = Long.MAX_VALUE;
        int currentNotificationId = 1;
        NotificationPrefs notificationPrefs = new NotificationPrefs(context, prefs, numFired == 0);
        redistributeBuckets(highPriorityEvents, mediumPriorityEvents, lowPriorityEvents, maxNotifications);
        int i = 0;
        while (i < highPriorityEvents.size()) {
            NotificationInfo info = highPriorityEvents.get(i);
            String summaryText = AlertUtils.formatTimeLocation(context, info.startMillis, info.allDay, info.location);
            postNotification(info, summaryText, context, true, notificationPrefs, nm, currentNotificationId);
            nextRefreshTime = Math.min(nextRefreshTime, getNextRefreshTime(info, currentTime));
            i++;
            currentNotificationId++;
        }
        int i2 = mediumPriorityEvents.size() - 1;
        int currentNotificationId2 = currentNotificationId;
        while (i2 >= 0) {
            NotificationInfo info2 = mediumPriorityEvents.get(i2);
            String summaryText2 = AlertUtils.formatTimeLocation(context, info2.startMillis, info2.allDay, info2.location);
            postNotification(info2, summaryText2, context, false, notificationPrefs, nm, currentNotificationId2);
            nextRefreshTime = Math.min(nextRefreshTime, getNextRefreshTime(info2, currentTime));
            i2--;
            currentNotificationId2++;
        }
        int numLowPriority = lowPriorityEvents.size();
        if (numLowPriority > 0) {
            String expiredDigestTitle = getDigestTitle(lowPriorityEvents);
            if (numLowPriority == 1) {
                NotificationInfo info3 = lowPriorityEvents.get(0);
                String summaryText3 = AlertUtils.formatTimeLocation(context, info3.startMillis, info3.allDay, info3.location);
                notification = AlertReceiver.makeBasicNotification(context, info3.eventName, summaryText3, info3.startMillis, info3.endMillis, info3.eventId, 0, false, -2);
            } else {
                notification = AlertReceiver.makeDigestNotification(context, lowPriorityEvents, expiredDigestTitle, false);
            }
            addNotificationOptions(notification, true, expiredDigestTitle, notificationPrefs.getDefaultVibrate(), notificationPrefs.getRingtoneAndSilence(), false);
            Log.d("AlertService", "Quietly posting digest alarm notification, numEvents:" + numLowPriority + ", notificationId:0");
            nm.notify(0, notification);
        } else {
            nm.cancel(0);
            Log.d("AlertService", "No low priority events, canceling the digest notification.");
        }
        if (currentNotificationId2 <= maxNotifications) {
            nm.cancelAllBetween(currentNotificationId2, maxNotifications);
            Log.d("AlertService", "Canceling leftover notification IDs " + currentNotificationId2 + "-" + maxNotifications);
        }
        if (nextRefreshTime < Long.MAX_VALUE && nextRefreshTime > currentTime) {
            AlertUtils.scheduleNextNotificationRefresh(context, alarmMgr, nextRefreshTime);
            long minutesBeforeRefresh = (nextRefreshTime - currentTime) / 60000;
            Time time = new Time();
            time.set(nextRefreshTime);
            String msg = String.format("Scheduling next notification refresh in %d min at: %d:%02d", Long.valueOf(minutesBeforeRefresh), Integer.valueOf(time.hour), Integer.valueOf(time.minute));
            Log.d("AlertService", msg);
        } else if (nextRefreshTime < currentTime) {
            Log.e("AlertService", "Illegal state: next notification refresh time found to be in the past.");
        }
        AlertUtils.flushOldAlertsFromInternalStorage(context);
        return true;
    }

    static void redistributeBuckets(ArrayList<NotificationInfo> highPriorityEvents, ArrayList<NotificationInfo> mediumPriorityEvents, ArrayList<NotificationInfo> lowPriorityEvents, int maxNotifications) {
        if (highPriorityEvents.size() > maxNotifications) {
            lowPriorityEvents.addAll(0, mediumPriorityEvents);
            List<NotificationInfo> itemsToMoveSublist = highPriorityEvents.subList(0, highPriorityEvents.size() - maxNotifications);
            lowPriorityEvents.addAll(0, itemsToMoveSublist);
            logEventIdsBumped(mediumPriorityEvents, itemsToMoveSublist);
            mediumPriorityEvents.clear();
            itemsToMoveSublist.clear();
        }
        if (mediumPriorityEvents.size() + highPriorityEvents.size() > maxNotifications) {
            int spaceRemaining = maxNotifications - highPriorityEvents.size();
            List<NotificationInfo> itemsToMoveSublist2 = mediumPriorityEvents.subList(spaceRemaining, mediumPriorityEvents.size());
            lowPriorityEvents.addAll(0, itemsToMoveSublist2);
            logEventIdsBumped(itemsToMoveSublist2, null);
            itemsToMoveSublist2.clear();
        }
    }

    private static void logEventIdsBumped(List<NotificationInfo> list1, List<NotificationInfo> list2) {
        StringBuilder ids = new StringBuilder();
        if (list1 != null) {
            for (NotificationInfo info : list1) {
                ids.append(info.eventId);
                ids.append(",");
            }
        }
        if (list2 != null) {
            for (NotificationInfo info2 : list2) {
                ids.append(info2.eventId);
                ids.append(",");
            }
        }
        if (ids.length() > 0 && ids.charAt(ids.length() - 1) == ',') {
            ids.setLength(ids.length() - 1);
        }
        if (ids.length() > 0) {
            Log.d("AlertService", "Reached max postings, bumping event IDs {" + ids.toString() + "} to digest.");
        }
    }

    private static long getNextRefreshTime(NotificationInfo info, long currentTime) {
        long startAdjustedForAllDay = info.startMillis;
        long endAdjustedForAllDay = info.endMillis;
        if (info.allDay) {
            Time t = new Time();
            startAdjustedForAllDay = Utils.convertAlldayUtcToLocal(t, info.startMillis, Time.getCurrentTimezone());
            endAdjustedForAllDay = Utils.convertAlldayUtcToLocal(t, info.startMillis, Time.getCurrentTimezone());
        }
        long nextRefreshTime = Long.MAX_VALUE;
        long gracePeriodCutoff = startAdjustedForAllDay + getGracePeriodMs(startAdjustedForAllDay, endAdjustedForAllDay, info.allDay);
        if (gracePeriodCutoff > currentTime) {
            nextRefreshTime = Math.min(Long.MAX_VALUE, gracePeriodCutoff);
        }
        if (endAdjustedForAllDay > currentTime && endAdjustedForAllDay > gracePeriodCutoff) {
            return Math.min(nextRefreshTime, endAdjustedForAllDay);
        }
        return nextRefreshTime;
    }

    static int processQuery(Cursor alertCursor, Context context, long currentTime, ArrayList<NotificationInfo> highPriorityEvents, ArrayList<NotificationInfo> mediumPriorityEvents, ArrayList<NotificationInfo> lowPriorityEvents) {
        boolean dropOld;
        String skipRemindersPref = Utils.getSharedPreference(context, "preferences_reminders_responded", "");
        boolean remindRespondedOnly = skipRemindersPref.equals(context.getResources().getStringArray(R.array.preferences_skip_reminders_values)[1]);
        boolean useQuietHours = Utils.getSharedPreference(context, "preferences_reminders_quiet_hours", false);
        int quietHoursStartHour = 22;
        int quietHoursStartMinute = 0;
        int quietHoursEndHour = 8;
        int quietHoursEndMinute = 0;
        if (useQuietHours) {
            quietHoursStartHour = Utils.getSharedPreference(context, "preferences_reminders_quiet_hours_start_hour", 22);
            quietHoursStartMinute = Utils.getSharedPreference(context, "preferences_reminders_quiet_hours_start_minute", 0);
            quietHoursEndHour = Utils.getSharedPreference(context, "preferences_reminders_quiet_hours_end_hour", 8);
            quietHoursEndMinute = Utils.getSharedPreference(context, "preferences_reminders_quiet_hours_end_minute", 0);
        }
        Time time = new Time();
        ContentResolver cr = context.getContentResolver();
        HashMap<Long, NotificationInfo> eventIds = new HashMap<>();
        int numFired = 0;
        while (alertCursor.moveToNext()) {
            try {
                long alertId = alertCursor.getLong(0);
                long eventId = alertCursor.getLong(1);
                int minutes = alertCursor.getInt(8);
                String eventName = alertCursor.getString(3);
                String description = alertCursor.getString(11);
                String location = alertCursor.getString(4);
                int status = alertCursor.getInt(5);
                boolean declined = status == 2;
                boolean responded = (status == 0 || status == 3) ? false : true;
                long beginTime = alertCursor.getLong(9);
                long endTime = alertCursor.getLong(10);
                Uri alertUri = ContentUris.withAppendedId(CalendarContract.CalendarAlerts.CONTENT_URI, alertId);
                long alarmTime = alertCursor.getLong(7);
                boolean forceQuiet = false;
                if (useQuietHours) {
                    time.set(alarmTime);
                    boolean alarmAfterQuietHoursStart = time.hour > quietHoursStartHour || (time.hour == quietHoursStartHour && time.minute >= quietHoursStartMinute);
                    boolean alarmBeforeQuietHoursEnd = time.hour < quietHoursEndHour || (time.hour == quietHoursEndHour && time.minute <= quietHoursEndMinute);
                    boolean quietHoursCrossesMidnight = quietHoursStartHour > quietHoursEndHour || (quietHoursStartHour == quietHoursEndHour && quietHoursStartMinute > quietHoursEndMinute);
                    if (quietHoursCrossesMidnight) {
                        if (alarmAfterQuietHoursStart || alarmBeforeQuietHoursEnd) {
                            forceQuiet = true;
                        }
                    } else if (alarmAfterQuietHoursStart && alarmBeforeQuietHoursEnd) {
                        forceQuiet = true;
                    }
                }
                int state = alertCursor.getInt(2);
                boolean allDay = alertCursor.getInt(6) != 0;
                boolean newAlertOverride = false;
                if (AlertUtils.BYPASS_DB && (currentTime - alarmTime) / 60000 < 1) {
                    boolean alreadyFired = AlertUtils.hasAlertFiredInSharedPrefs(context, eventId, beginTime, alarmTime);
                    if (!alreadyFired) {
                        newAlertOverride = true;
                    }
                }
                StringBuilder msgBuilder = new StringBuilder();
                msgBuilder.append("alertCursor result: alarmTime:").append(alarmTime).append(" alertId:").append(alertId).append(" eventId:").append(eventId).append(" state: ").append(state).append(" minutes:").append(minutes).append(" declined:").append(declined).append(" responded:").append(responded).append(" beginTime:").append(beginTime).append(" endTime:").append(endTime).append(" allDay:").append(allDay).append(" alarmTime:").append(alarmTime).append(" forceQuiet:").append(forceQuiet);
                if (AlertUtils.BYPASS_DB) {
                    msgBuilder.append(" newAlertOverride: " + newAlertOverride);
                }
                Log.d("AlertService", msgBuilder.toString());
                ContentValues values = new ContentValues();
                int newState = -1;
                boolean newAlert = false;
                boolean sendAlert = !declined;
                if (remindRespondedOnly) {
                    sendAlert = sendAlert && responded;
                }
                if (sendAlert) {
                    if (state == 0 || newAlertOverride) {
                        newState = 1;
                        numFired++;
                        if (!forceQuiet) {
                            newAlert = true;
                        }
                        values.put("receivedTime", Long.valueOf(currentTime));
                    }
                } else {
                    newState = 2;
                }
                if (newState != -1) {
                    values.put("state", Integer.valueOf(newState));
                    state = newState;
                    if (AlertUtils.BYPASS_DB) {
                        AlertUtils.setAlertFiredInSharedPrefs(context, eventId, beginTime, alarmTime);
                    }
                }
                if (state == 1) {
                    values.put("notifyTime", Long.valueOf(currentTime));
                }
                if (values.size() > 0) {
                    cr.update(alertUri, values, null, null);
                }
                if (state == 1) {
                    NotificationInfo newInfo = new NotificationInfo(eventName, location, description, beginTime, endTime, eventId, allDay, newAlert);
                    long beginTimeAdjustedForAllDay = beginTime;
                    String tz = null;
                    if (allDay) {
                        tz = TimeZone.getDefault().getID();
                        beginTimeAdjustedForAllDay = Utils.convertAlldayUtcToLocal(null, beginTime, tz);
                    }
                    if (eventIds.containsKey(Long.valueOf(eventId))) {
                        NotificationInfo oldInfo = eventIds.get(Long.valueOf(eventId));
                        long oldBeginTimeAdjustedForAllDay = oldInfo.startMillis;
                        if (allDay) {
                            oldBeginTimeAdjustedForAllDay = Utils.convertAlldayUtcToLocal(null, oldInfo.startMillis, tz);
                        }
                        long oldStartInterval = oldBeginTimeAdjustedForAllDay - currentTime;
                        long newStartInterval = beginTimeAdjustedForAllDay - currentTime;
                        if (newStartInterval >= 0 || oldStartInterval <= 0) {
                            dropOld = Math.abs(newStartInterval) < Math.abs(oldStartInterval);
                        } else {
                            dropOld = Math.abs(newStartInterval) < 900000;
                        }
                        if (dropOld) {
                            highPriorityEvents.remove(oldInfo);
                            mediumPriorityEvents.remove(oldInfo);
                            Log.d("AlertService", "Dropping alert for recurring event ID:" + oldInfo.eventId + ", startTime:" + oldInfo.startMillis + " in favor of startTime:" + newInfo.startMillis);
                        }
                    }
                    eventIds.put(Long.valueOf(eventId), newInfo);
                    long highPriorityCutoff = currentTime - getGracePeriodMs(beginTime, endTime, allDay);
                    if (beginTimeAdjustedForAllDay > highPriorityCutoff) {
                        highPriorityEvents.add(newInfo);
                    } else if (allDay && tz != null && DateUtils.isToday(beginTimeAdjustedForAllDay)) {
                        mediumPriorityEvents.add(newInfo);
                    } else {
                        lowPriorityEvents.add(newInfo);
                    }
                }
            } finally {
                if (alertCursor != null) {
                    alertCursor.close();
                }
            }
        }
        GlobalDismissManager.processEventIds(context, eventIds.keySet());
        return numFired;
    }

    private static long getGracePeriodMs(long beginTime, long endTime, boolean allDay) {
        if (allDay) {
            return 900000L;
        }
        return Math.max(900000L, (endTime - beginTime) / 4);
    }

    private static String getDigestTitle(ArrayList<NotificationInfo> events) {
        StringBuilder digestTitle = new StringBuilder();
        for (NotificationInfo eventInfo : events) {
            if (!TextUtils.isEmpty(eventInfo.eventName)) {
                if (digestTitle.length() > 0) {
                    digestTitle.append(", ");
                }
                digestTitle.append(eventInfo.eventName);
            }
        }
        return digestTitle.toString();
    }

    private static void postNotification(NotificationInfo info, String summaryText, Context context, boolean highPriority, NotificationPrefs prefs, NotificationMgr notificationMgr, int notificationId) {
        int priorityVal = 0;
        if (highPriority) {
            priorityVal = 1;
        }
        String tickerText = getTickerText(info.eventName, info.location);
        NotificationWrapper notification = AlertReceiver.makeExpandingNotification(context, info.eventName, summaryText, info.description, info.startMillis, info.endMillis, info.eventId, notificationId, prefs.getDoPopup(), priorityVal);
        boolean quietUpdate = true;
        String ringtone = "";
        if (info.newAlert) {
            quietUpdate = prefs.quietUpdate;
            ringtone = prefs.getRingtoneAndSilence();
        }
        addNotificationOptions(notification, quietUpdate, tickerText, prefs.getDefaultVibrate(), ringtone, true);
        notificationMgr.notify(notificationId, notification);
        Log.d("AlertService", "Posting individual alarm notification, eventId:" + info.eventId + ", notificationId:" + notificationId + (TextUtils.isEmpty(ringtone) ? ", quiet" : ", LOUD") + (highPriority ? ", high-priority" : ""));
    }

    private static String getTickerText(String eventName, String location) {
        if (TextUtils.isEmpty(location)) {
            return eventName;
        }
        String tickerText = eventName + " - " + location;
        return tickerText;
    }

    static class NotificationInfo {
        boolean allDay;
        String description;
        long endMillis;
        long eventId;
        String eventName;
        String location;
        boolean newAlert;
        long startMillis;

        NotificationInfo(String eventName, String location, String description, long startMillis, long endMillis, long eventId, boolean allDay, boolean newAlert) {
            this.eventName = eventName;
            this.location = location;
            this.description = description;
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.eventId = eventId;
            this.newAlert = newAlert;
            this.allDay = allDay;
        }
    }

    private static void addNotificationOptions(NotificationWrapper nw, boolean quietUpdate, String tickerText, boolean defaultVibrate, String reminderRingtone, boolean showLights) {
        Notification notification = nw.mNotification;
        if (showLights) {
            notification.flags |= 1;
            notification.defaults |= 4;
        }
        if (!quietUpdate) {
            if (!TextUtils.isEmpty(tickerText)) {
                notification.tickerText = tickerText;
            }
            if (defaultVibrate) {
                notification.defaults |= 2;
            }
            notification.sound = TextUtils.isEmpty(reminderRingtone) ? null : Uri.parse(reminderRingtone);
        }
    }

    static class NotificationPrefs {
        private Context context;
        private SharedPreferences prefs;
        boolean quietUpdate;
        private int doPopup = -1;
        private int defaultVibrate = -1;
        private String ringtone = null;

        NotificationPrefs(Context context, SharedPreferences prefs, boolean quietUpdate) {
            this.context = context;
            this.prefs = prefs;
            this.quietUpdate = quietUpdate;
        }

        private boolean getDoPopup() {
            if (this.doPopup < 0) {
                if (this.prefs.getBoolean("preferences_alerts_popup", false)) {
                    this.doPopup = 1;
                } else {
                    this.doPopup = 0;
                }
            }
            return this.doPopup == 1;
        }

        private boolean getDefaultVibrate() {
            if (this.defaultVibrate < 0) {
                this.defaultVibrate = Utils.getDefaultVibrate(this.context, this.prefs) ? 1 : 0;
            }
            return this.defaultVibrate == 1;
        }

        private String getRingtoneAndSilence() {
            if (this.ringtone == null) {
                if (this.quietUpdate) {
                    this.ringtone = "";
                } else {
                    this.ringtone = Utils.getRingTonePreference(this.context);
                }
            }
            String retVal = this.ringtone;
            this.ringtone = "";
            return retVal;
        }
    }

    private void doTimeChanged() {
        ContentResolver cr = getContentResolver();
        rescheduleMissedAlarms(cr, this, AlertUtils.createAlarmManager(this));
        updateAlertNotification(this);
    }

    private static final void rescheduleMissedAlarms(ContentResolver cr, Context context, AlarmManagerInterface manager) {
        long now = System.currentTimeMillis();
        long ancient = now - 86400000;
        String[] projection = {"alarmTime"};
        Cursor cursor = cr.query(CalendarContract.CalendarAlerts.CONTENT_URI, projection, "state=0 AND alarmTime<? AND alarmTime>? AND end>=?", new String[]{Long.toString(now), Long.toString(ancient), Long.toString(now)}, "alarmTime ASC");
        if (cursor != null) {
            Log.d("AlertService", "missed alarms found: " + cursor.getCount());
            long alarmTime = -1;
            while (cursor.moveToNext()) {
                try {
                    long newAlarmTime = cursor.getLong(0);
                    if (alarmTime != newAlarmTime) {
                        Log.w("AlertService", "rescheduling missed alarm. alarmTime: " + newAlarmTime);
                        AlertUtils.scheduleAlarm(context, manager, newAlarmTime);
                        alarmTime = newAlarmTime;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            AlertService.this.processMessage(msg);
            AlertReceiver.finishStartingService(AlertService.this, msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("AlertService", 10);
        thread.start();
        this.mServiceLooper = thread.getLooper();
        this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
        AlertUtils.flushOldAlertsFromInternalStorage(getApplication());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Message msg = this.mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent.getExtras();
            this.mServiceHandler.sendMessage(msg);
            return 3;
        }
        return 3;
    }

    @Override
    public void onDestroy() {
        this.mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
