package com.android.calendar.alerts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.PermissionDeniedActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.mediatek.calendar.LogUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class AlertService extends JobIntentService {
    public static boolean isEventAlreadyFired;
    static final String[] ALERT_PROJECTION = {"_id", "event_id", "state", "title", "eventLocation", "selfAttendeeStatus", "allDay", "alarmTime", "minutes", "begin", "end", "description"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private static HashMap<Long, Integer> sEventIdToNotificationIdMap = new HashMap<>();
    private static final String[] ACTIVE_ALERTS_SELECTION_ARGS = {Integer.toString(1), Integer.toString(0)};
    private static Boolean sReceivedProviderReminderBroadcast = null;

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    public static class NotificationWrapper {
        long mBegin;
        long mEnd;
        long mEventId;
        Notification mNotification;
        ArrayList<NotificationWrapper> mNw;

        public NotificationWrapper(Notification notification, int i, long j, long j2, long j3, boolean z) {
            this.mNotification = notification;
            this.mEventId = j;
            this.mBegin = j2;
            this.mEnd = j3;
        }

        public NotificationWrapper(Notification notification) {
            this.mNotification = notification;
        }

        public void add(NotificationWrapper notificationWrapper) {
            if (this.mNw == null) {
                this.mNw = new ArrayList<>();
            }
            this.mNw.add(notificationWrapper);
        }
    }

    public static class NotificationMgrWrapper extends NotificationMgr {
        private static NotificationChannel mChannelDefault = new NotificationChannel("calendar_notif_channel_default", "Default channel", 4);
        private static NotificationChannel mChannelOfUnreadExpiredEvents;
        NotificationManager mNm;

        static {
            mChannelDefault.enableLights(true);
            mChannelOfUnreadExpiredEvents = new NotificationChannel("calendar_notif_channel_of_fired_notification", "Mute channel", 2);
        }

        public NotificationMgrWrapper(NotificationManager notificationManager) {
            this.mNm = notificationManager;
            notificationManager.createNotificationChannel(mChannelDefault);
            notificationManager.createNotificationChannel(mChannelOfUnreadExpiredEvents);
        }

        @Override
        public void cancel(int i) {
            this.mNm.cancel(i);
        }

        @Override
        public void notify(int i, NotificationWrapper notificationWrapper) {
            this.mNm.notify(i, notificationWrapper.mNotification);
        }
    }

    @Override
    protected void onHandleWork(Intent intent) throws Throwable {
        Log.i("AlertService", "Handling work: " + intent);
        if (!checkPermissions()) {
            Log.d("AlertService", "Permission required");
            Intent intent2 = new Intent(this, (Class<?>) PermissionDeniedActivity.class);
            intent2.addFlags(268435456);
            startActivity(intent2);
            return;
        }
        String action = intent.getAction();
        boolean zEquals = action.equals("android.intent.action.EVENT_REMINDER");
        if (zEquals) {
            if (sReceivedProviderReminderBroadcast == null) {
                sReceivedProviderReminderBroadcast = Boolean.valueOf(Utils.getSharedPreference((Context) this, "preference_received_provider_reminder_broadcast", false));
            }
            if (!sReceivedProviderReminderBroadcast.booleanValue()) {
                sReceivedProviderReminderBroadcast = true;
                Log.d("AlertService", "Setting key preference_received_provider_reminder_broadcast to: true");
                Utils.setSharedPreference((Context) this, "preference_received_provider_reminder_broadcast", true);
            }
        }
        if (zEquals || action.equals("android.intent.action.PROVIDER_CHANGED") || action.equals("android.intent.action.EVENT_REMINDER") || action.equals("com.android.calendar.EVENT_REMINDER_APP") || action.equals("android.intent.action.LOCALE_CHANGED")) {
            if (action.equals("android.intent.action.PROVIDER_CHANGED") || action.equals("android.intent.action.EVENT_REMINDER")) {
                try {
                    Thread.sleep(5000L);
                } catch (Exception e) {
                }
            }
            GlobalDismissManager.syncSenderDismissCache(this);
            updateAlertNotification(this);
        } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(this, (Class<?>) InitAlarmsService.class));
            builder.setMinimumLatency(1000L);
            builder.setOverrideDeadline(3000L);
            ((JobScheduler) getSystemService(JobScheduler.class)).schedule(builder.build());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.EVENT_REMINDER");
            getApplicationContext().registerReceiver(new AlertReceiver(), intentFilter);
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
        ContentResolver contentResolver = context.getContentResolver();
        long jCurrentTimeMillis = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put("state", (Integer) 2);
        if (contentResolver.update(CalendarContract.CalendarAlerts.CONTENT_URI, contentValues, "end<? AND state=?", new String[]{Long.toString(jCurrentTimeMillis), Integer.toString(0)}) <= 0) {
            updateAlertNotification(context);
        }
    }

    static boolean updateAlertNotification(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        NotificationMgrWrapper notificationMgrWrapper = new NotificationMgrWrapper((NotificationManager) context.getSystemService("notification"));
        long jCurrentTimeMillis = System.currentTimeMillis();
        SharedPreferences sharedPreferences = GeneralPreferences.getSharedPreferences(context);
        Log.d("AlertService", "Beginning updateAlertNotification");
        if (!sharedPreferences.getBoolean("preferences_alerts", true)) {
            Log.d("AlertService", "alert preference is OFF");
            notificationMgrWrapper.cancelAll();
            return true;
        }
        GlobalDismissManager.syncReceiverDismissCache(context);
        Cursor cursorQuery = contentResolver.query(CalendarContract.CalendarAlerts.CONTENT_URI, ALERT_PROJECTION, "(state=? OR state=?) AND alarmTime<=" + jCurrentTimeMillis, ACTIVE_ALERTS_SELECTION_ARGS, "begin DESC, end DESC");
        if (cursorQuery == null || cursorQuery.getCount() == 0) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Log.d("AlertService", "No fired or scheduled alerts");
            notificationMgrWrapper.cancelAll();
            AlertUtils.postUnreadNumber(context);
            return false;
        }
        return generateAlerts(context, notificationMgrWrapper, AlertUtils.createAlarmManager(context), sharedPreferences, cursorQuery, jCurrentTimeMillis, 18);
    }

    public static boolean generateAlerts(Context context, NotificationMgr notificationMgr, AlarmManagerInterface alarmManagerInterface, SharedPreferences sharedPreferences, Cursor cursor, long j, int i) throws Throwable {
        long j2;
        int i2;
        int i3;
        ?? r18;
        long j3;
        String str;
        boolean z;
        int i4;
        NotificationWrapper notificationWrapperMakeDigestNotification;
        Log.d("AlertService", "alertCursor count:" + cursor.getCount());
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        int iProcessQuery = processQuery(cursor, context, j, arrayList, arrayList2, arrayList3);
        AlertUtils.postUnreadNumber(context);
        if (arrayList.size() + arrayList2.size() + arrayList3.size() == 0) {
            notificationMgr.cancelAll();
            return true;
        }
        sEventIdToNotificationIdMap.clear();
        NotificationPrefs notificationPrefs = new NotificationPrefs(context, sharedPreferences, iProcessQuery == 0);
        redistributeBuckets(arrayList, arrayList2, arrayList3, i);
        LogUtil.d("AlertService", "highPriorityEvents.size:" + arrayList.size() + ",mediumPriorityEvents.size:" + arrayList2.size() + ",lowPriorityEvents.size:" + arrayList3.size() + ",numFired:" + iProcessQuery);
        int i5 = 1;
        long jMin = Long.MAX_VALUE;
        int i6 = 0;
        while (i6 < arrayList.size()) {
            NotificationInfo notificationInfo = (NotificationInfo) arrayList.get(i6);
            int i7 = i5 + 1;
            postNotification(notificationInfo, AlertUtils.formatTimeLocation(context, notificationInfo.startMillis, notificationInfo.allDay, notificationInfo.location), context, true, notificationPrefs, notificationMgr, i5);
            sEventIdToNotificationIdMap.put(Long.valueOf(notificationInfo.eventId), Integer.valueOf(i7 - 1));
            jMin = Math.min(jMin, getNextRefreshTime(notificationInfo, j));
            i6++;
            i5 = i7;
            arrayList = arrayList;
            arrayList2 = arrayList2;
            notificationPrefs = notificationPrefs;
        }
        NotificationPrefs notificationPrefs2 = notificationPrefs;
        ArrayList arrayList4 = arrayList2;
        LogUtil.d("AlertService", "mediumPriorityEvents postNotification,size:" + arrayList4.size());
        long jMin2 = jMin;
        int i8 = i5;
        int size = arrayList4.size() - 1;
        while (size >= 0) {
            NotificationInfo notificationInfo2 = (NotificationInfo) arrayList4.get(size);
            int i9 = i8 + 1;
            postNotification(notificationInfo2, AlertUtils.formatTimeLocation(context, notificationInfo2.startMillis, notificationInfo2.allDay, notificationInfo2.location), context, false, notificationPrefs2, notificationMgr, i8);
            sEventIdToNotificationIdMap.put(Long.valueOf(notificationInfo2.eventId), Integer.valueOf(i9 - 1));
            jMin2 = Math.min(jMin2, getNextRefreshTime(notificationInfo2, j));
            size--;
            i8 = i9;
        }
        int size2 = arrayList3.size();
        if (size2 > 0) {
            String digestTitle = getDigestTitle(arrayList3);
            if (size2 == 1) {
                NotificationInfo notificationInfo3 = (NotificationInfo) arrayList3.get(0);
                i3 = 0;
                j2 = jMin2;
                str = digestTitle;
                z = true;
                i4 = size2;
                i2 = i8;
                notificationWrapperMakeDigestNotification = AlertReceiver.makeBasicNotification(context, notificationInfo3.eventName, AlertUtils.formatTimeLocation(context, notificationInfo3.startMillis, notificationInfo3.allDay, notificationInfo3.location), notificationInfo3.startMillis, notificationInfo3.endMillis, notificationInfo3.eventId, 0, false, -2);
            } else {
                j2 = jMin2;
                str = digestTitle;
                z = true;
                i4 = size2;
                i2 = i8;
                i3 = 0;
                notificationWrapperMakeDigestNotification = AlertReceiver.makeDigestNotification(context, arrayList3, str, false);
            }
            addNotificationOptions(notificationWrapperMakeDigestNotification, true, str, notificationPrefs2.getDefaultVibrate(), notificationPrefs2.getRingtoneAndSilence(), false);
            Log.d("AlertService", "Quietly posting digest alarm notification, numEvents:" + i4 + ", notificationId:" + i3);
            StringBuilder sb = new StringBuilder();
            sb.append("lowPriorityEvents notify,notification:");
            sb.append(notificationWrapperMakeDigestNotification.mNotification);
            LogUtil.d("AlertService", sb.toString());
            notificationMgr.notify(i3, notificationWrapperMakeDigestNotification);
            r18 = z;
        } else {
            j2 = jMin2;
            i2 = i8;
            i3 = 0;
            r18 = 1;
            LogUtil.d("AlertService", "lowPriorityEvents cancel notify,numLowPriority:" + size2);
            notificationMgr.cancel(0);
            Log.d("AlertService", "No low priority events, canceling the digest notification.");
        }
        int i10 = i2;
        if (i10 <= i) {
            notificationMgr.cancelAllBetween(i10, i);
            Log.d("AlertService", "Canceling leftover notification IDs " + i10 + "-" + i);
        }
        long j4 = j2;
        if (j4 >= Long.MAX_VALUE) {
            j3 = j;
        } else {
            int i11 = i3;
            j3 = j;
            if (j4 > j3) {
                AlertUtils.scheduleNextNotificationRefresh(context, alarmManagerInterface, j4);
                long j5 = (j4 - j3) / 60000;
                Time time = new Time();
                time.set(j4);
                Object[] objArr = new Object[3];
                objArr[i11] = Long.valueOf(j5);
                objArr[r18] = Integer.valueOf(time.hour);
                objArr[2] = Integer.valueOf(time.minute);
                Log.d("AlertService", String.format("Scheduling next notification refresh in %d min at: %d:%02d", objArr));
            }
            AlertUtils.flushOldAlertsFromInternalStorage(context);
            return r18;
        }
        if (j4 < j3) {
            Log.e("AlertService", "Illegal state: next notification refresh time found to be in the past.");
        }
        AlertUtils.flushOldAlertsFromInternalStorage(context);
        return r18;
    }

    static void redistributeBuckets(ArrayList<NotificationInfo> arrayList, ArrayList<NotificationInfo> arrayList2, ArrayList<NotificationInfo> arrayList3, int i) {
        if (arrayList.size() > i) {
            arrayList3.addAll(0, arrayList2);
            List<NotificationInfo> listSubList = arrayList.subList(0, arrayList.size() - i);
            arrayList3.addAll(0, listSubList);
            logEventIdsBumped(arrayList2, listSubList);
            arrayList2.clear();
            listSubList.clear();
        }
        if (arrayList2.size() + arrayList.size() > i) {
            List<NotificationInfo> listSubList2 = arrayList2.subList(i - arrayList.size(), arrayList2.size());
            arrayList3.addAll(0, listSubList2);
            logEventIdsBumped(listSubList2, null);
            listSubList2.clear();
        }
    }

    private static void logEventIdsBumped(List<NotificationInfo> list, List<NotificationInfo> list2) {
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            Iterator<NotificationInfo> it = list.iterator();
            while (it.hasNext()) {
                sb.append(it.next().eventId);
                sb.append(",");
            }
        }
        if (list2 != null) {
            Iterator<NotificationInfo> it2 = list2.iterator();
            while (it2.hasNext()) {
                sb.append(it2.next().eventId);
                sb.append(",");
            }
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        if (sb.length() > 0) {
            Log.d("AlertService", "Reached max postings, bumping event IDs {" + sb.toString() + "} to digest.");
        }
    }

    private static long getNextRefreshTime(NotificationInfo notificationInfo, long j) {
        long j2 = notificationInfo.startMillis;
        long j3 = notificationInfo.endMillis;
        if (notificationInfo.allDay) {
            Time time = new Time();
            long jConvertAlldayUtcToLocal = Utils.convertAlldayUtcToLocal(time, notificationInfo.startMillis, Time.getCurrentTimezone());
            long jConvertAlldayUtcToLocal2 = Utils.convertAlldayUtcToLocal(time, notificationInfo.startMillis, Time.getCurrentTimezone());
            j2 = jConvertAlldayUtcToLocal;
            j3 = jConvertAlldayUtcToLocal2;
        }
        long gracePeriodMs = j2 + getGracePeriodMs(j2, j3, notificationInfo.allDay);
        long jMin = gracePeriodMs > j ? Math.min(Long.MAX_VALUE, gracePeriodMs) : Long.MAX_VALUE;
        if (j3 > j && j3 > gracePeriodMs) {
            return Math.min(jMin, j3);
        }
        return jMin;
    }

    static int processQuery(Cursor cursor, Context context, long j, ArrayList<NotificationInfo> arrayList, ArrayList<NotificationInfo> arrayList2, ArrayList<NotificationInfo> arrayList3) throws Throwable {
        int i;
        int sharedPreference;
        int sharedPreference2;
        int i2;
        Uri uri;
        boolean z;
        int i3;
        boolean z2;
        boolean z3;
        int i4;
        Time time;
        int i5;
        boolean z4;
        int i6;
        HashMap map;
        int i7;
        ContentResolver contentResolver;
        Uri uri2;
        int i8;
        int i9;
        boolean z5;
        int i10;
        boolean z6;
        ContentValues contentValues;
        long j2;
        long j3;
        ArrayList<ContentProviderOperation> arrayList4;
        long j4;
        String id;
        long jConvertAlldayUtcToLocal;
        HashMap map2;
        ArrayList<ContentProviderOperation> arrayList5;
        String str;
        int i11;
        ArrayList<NotificationInfo> arrayList6;
        ArrayList<NotificationInfo> arrayList7;
        boolean z7;
        ContentValues contentValues2;
        Cursor cursor2 = cursor;
        Context context2 = context;
        boolean zEquals = Utils.getSharedPreference(context2, "preferences_reminders_responded", "").equals(context.getResources().getStringArray(R.array.preferences_skip_reminders_values)[1]);
        int i12 = 0;
        boolean sharedPreference3 = Utils.getSharedPreference(context2, "preferences_reminders_quiet_hours", false);
        if (sharedPreference3) {
            int sharedPreference4 = Utils.getSharedPreference(context2, "preferences_reminders_quiet_hours_start_hour", 22);
            int sharedPreference5 = Utils.getSharedPreference(context2, "preferences_reminders_quiet_hours_start_minute", 0);
            i = sharedPreference4;
            sharedPreference = Utils.getSharedPreference(context2, "preferences_reminders_quiet_hours_end_hour", 8);
            i2 = sharedPreference5;
            sharedPreference2 = Utils.getSharedPreference(context2, "preferences_reminders_quiet_hours_end_minute", 0);
        } else {
            i = 22;
            sharedPreference = 8;
            sharedPreference2 = 0;
            i2 = 0;
        }
        Time time2 = new Time();
        ContentResolver contentResolver2 = context.getContentResolver();
        HashMap map3 = new HashMap();
        ArrayList<ContentProviderOperation> arrayList8 = new ArrayList<>();
        int i13 = 0;
        while (cursor.moveToNext()) {
            try {
                try {
                    long j5 = cursor2.getLong(i12);
                    ArrayList<ContentProviderOperation> arrayList9 = arrayList8;
                    boolean z8 = zEquals;
                    long j6 = cursor2.getLong(1);
                    HashMap map4 = map3;
                    int i14 = cursor2.getInt(8);
                    String string = cursor2.getString(3);
                    String string2 = cursor2.getString(11);
                    String string3 = cursor2.getString(4);
                    int i15 = cursor2.getInt(5);
                    boolean z9 = i15 == 2;
                    boolean z10 = (i15 == 0 || i15 == 3) ? false : true;
                    long j7 = cursor2.getLong(9);
                    long j8 = cursor2.getLong(10);
                    Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.CalendarAlerts.CONTENT_URI, j5);
                    ContentResolver contentResolver3 = contentResolver2;
                    long j9 = cursor2.getLong(7);
                    if (sharedPreference3) {
                        time2.set(j9);
                        boolean z11 = time2.hour > i || (time2.hour == i && time2.minute >= i2);
                        uri = uriWithAppendedId;
                        boolean z12 = time2.hour < sharedPreference || (time2.hour == sharedPreference && time2.minute <= sharedPreference2);
                        if (i > sharedPreference || (i == sharedPreference && i2 > sharedPreference2)) {
                            z = z11 || z12;
                        } else if (z11 && z12) {
                        }
                        boolean z13 = z10;
                        i3 = cursor2.getInt(2);
                        z2 = cursor2.getInt(6) == 0;
                        if (AlertUtils.BYPASS_DB || (j - j9) / 60000 >= 1) {
                            z3 = z2;
                            i4 = sharedPreference2;
                            time = time2;
                            i5 = i2;
                            z4 = z;
                            i6 = i;
                            map = map4;
                            i7 = i14;
                            contentResolver = contentResolver3;
                            uri2 = uri;
                            i8 = 2;
                            i9 = sharedPreference;
                        } else {
                            z3 = z2;
                            contentResolver = contentResolver3;
                            Context context3 = context2;
                            i4 = sharedPreference2;
                            time = time2;
                            i5 = i2;
                            i9 = sharedPreference;
                            z4 = z;
                            i6 = i;
                            i8 = 2;
                            map = map4;
                            i7 = i14;
                            uri2 = uri;
                            try {
                                boolean z14 = AlertUtils.hasAlertFiredInSharedPrefs(context3, j6, j7, j9) ? false : true;
                                StringBuilder sb = new StringBuilder();
                                sb.append("alertCursor result: alarmTime:");
                                sb.append(j9);
                                sb.append(" alertId:");
                                sb.append(j5);
                                sb.append(" eventId:");
                                sb.append(j6);
                                sb.append(" state: ");
                                sb.append(i3);
                                sb.append(" minutes:");
                                sb.append(i7);
                                sb.append(" declined:");
                                boolean z15 = z9;
                                sb.append(z15);
                                sb.append(" responded:");
                                sb.append(z13);
                                sb.append(" beginTime:");
                                sb.append(j7);
                                sb.append(" endTime:");
                                sb.append(j8);
                                sb.append(" allDay:");
                                sb.append(z3);
                                sb.append(" alarmTime:");
                                sb.append(j9);
                                sb.append(" forceQuiet:");
                                sb.append(z4);
                                if (AlertUtils.BYPASS_DB) {
                                    sb.append(" newAlertOverride: " + z14);
                                }
                                Log.d("AlertService", sb.toString());
                                ContentValues contentValues3 = new ContentValues();
                                z5 = !z15;
                                if (z8) {
                                    z5 = z5 && z13;
                                }
                                if (z5) {
                                    if (i3 != 0 && !z14) {
                                        i10 = -1;
                                    }
                                    i13++;
                                    boolean z16 = !z4;
                                    contentValues3.put("receivedTime", Long.valueOf(j));
                                    z6 = z16;
                                    i10 = 1;
                                    if (i10 == -1) {
                                        contentValues3.put("state", Integer.valueOf(i10));
                                        if (AlertUtils.BYPASS_DB) {
                                            contentValues2 = contentValues3;
                                            j3 = j8;
                                            j2 = j7;
                                            AlertUtils.setAlertFiredInSharedPrefs(context, j6, j7, j9);
                                        } else {
                                            contentValues2 = contentValues3;
                                            j2 = j7;
                                            j3 = j8;
                                        }
                                        contentValues = contentValues2;
                                        i3 = i10;
                                    } else {
                                        contentValues = contentValues3;
                                        j2 = j7;
                                        j3 = j8;
                                    }
                                    if (i10 == 1) {
                                        contentValues.put("notifyTime", Long.valueOf(j));
                                    }
                                    if (contentValues.size() <= 0) {
                                        ContentProviderOperation contentProviderOperationBuild = ContentProviderOperation.newUpdate(uri2).withValues(contentValues).build();
                                        arrayList4 = arrayList9;
                                        arrayList4.add(contentProviderOperationBuild);
                                    } else {
                                        arrayList4 = arrayList9;
                                    }
                                    if (i3 == 1) {
                                        arrayList5 = arrayList4;
                                        i11 = i6;
                                        map2 = map;
                                    } else {
                                        NotificationInfo notificationInfo = new NotificationInfo(string, string3, string2, j2, j3, j6, z3, z6);
                                        if (z3) {
                                            id = TimeZone.getDefault().getID();
                                            j4 = j2;
                                            jConvertAlldayUtcToLocal = Utils.convertAlldayUtcToLocal(null, j4, id);
                                        } else {
                                            j4 = j2;
                                            id = null;
                                            jConvertAlldayUtcToLocal = j4;
                                        }
                                        map2 = map;
                                        if (map2.containsKey(Long.valueOf(j6))) {
                                            NotificationInfo notificationInfo2 = (NotificationInfo) map2.get(Long.valueOf(j6));
                                            String str2 = id;
                                            long jConvertAlldayUtcToLocal2 = notificationInfo2.startMillis;
                                            if (z3) {
                                                i11 = i6;
                                                str = str2;
                                                jConvertAlldayUtcToLocal2 = Utils.convertAlldayUtcToLocal(null, notificationInfo2.startMillis, str);
                                            } else {
                                                i11 = i6;
                                                str = str2;
                                            }
                                            long j10 = jConvertAlldayUtcToLocal2 - j;
                                            long j11 = jConvertAlldayUtcToLocal - j;
                                            if (j11 >= 0 || j10 <= 0) {
                                                z7 = Math.abs(j11) < Math.abs(j10);
                                            } else if (Math.abs(j11) < 900000) {
                                            }
                                            if (z7) {
                                                arrayList.remove(notificationInfo2);
                                                arrayList2.remove(notificationInfo2);
                                                StringBuilder sb2 = new StringBuilder();
                                                arrayList5 = arrayList4;
                                                sb2.append("Dropping alert for recurring event ID:");
                                                sb2.append(notificationInfo2.eventId);
                                                sb2.append(", startTime:");
                                                sb2.append(notificationInfo2.startMillis);
                                                sb2.append(" in favor of startTime:");
                                                sb2.append(notificationInfo.startMillis);
                                                Log.d("AlertService", sb2.toString());
                                            } else {
                                                arrayList5 = arrayList4;
                                            }
                                        } else {
                                            arrayList5 = arrayList4;
                                            str = id;
                                            i11 = i6;
                                        }
                                        map2.put(Long.valueOf(j6), notificationInfo);
                                        if (jConvertAlldayUtcToLocal > j - getGracePeriodMs(j4, j3, z3)) {
                                            arrayList6 = arrayList;
                                            arrayList6.add(notificationInfo);
                                            arrayList7 = arrayList2;
                                        } else {
                                            arrayList6 = arrayList;
                                            if (z3 && str != null && DateUtils.isToday(jConvertAlldayUtcToLocal)) {
                                                arrayList7 = arrayList2;
                                                arrayList7.add(notificationInfo);
                                            } else {
                                                arrayList7 = arrayList2;
                                                arrayList3.add(notificationInfo);
                                            }
                                        }
                                        map3 = map2;
                                        time2 = time;
                                        zEquals = z8;
                                        sharedPreference2 = i4;
                                        i2 = i5;
                                        sharedPreference = i9;
                                        contentResolver2 = contentResolver;
                                        i = i11;
                                        arrayList8 = arrayList5;
                                        context2 = context;
                                        cursor2 = cursor;
                                        i12 = 0;
                                    }
                                    arrayList7 = arrayList2;
                                    arrayList6 = arrayList;
                                    map3 = map2;
                                    time2 = time;
                                    zEquals = z8;
                                    sharedPreference2 = i4;
                                    i2 = i5;
                                    sharedPreference = i9;
                                    contentResolver2 = contentResolver;
                                    i = i11;
                                    arrayList8 = arrayList5;
                                    context2 = context;
                                    cursor2 = cursor;
                                    i12 = 0;
                                } else {
                                    i10 = i8;
                                }
                                z6 = false;
                                if (i10 == -1) {
                                }
                                if (i10 == 1) {
                                }
                                if (contentValues.size() <= 0) {
                                }
                                if (i3 == 1) {
                                }
                                arrayList7 = arrayList2;
                                arrayList6 = arrayList;
                                map3 = map2;
                                time2 = time;
                                zEquals = z8;
                                sharedPreference2 = i4;
                                i2 = i5;
                                sharedPreference = i9;
                                contentResolver2 = contentResolver;
                                i = i11;
                                arrayList8 = arrayList5;
                                context2 = context;
                                cursor2 = cursor;
                                i12 = 0;
                            } catch (OperationApplicationException e) {
                                e = e;
                                cursor2 = cursor;
                                Log.i("AlertService", e.toString());
                                if (cursor2 != null) {
                                    cursor.close();
                                }
                                return i13;
                            } catch (RemoteException e2) {
                                e = e2;
                                cursor2 = cursor;
                                Log.i("AlertService", e.toString());
                                if (cursor2 != null) {
                                }
                                return i13;
                            } catch (Throwable th) {
                                th = th;
                                cursor2 = cursor;
                                if (cursor2 != null) {
                                    cursor.close();
                                }
                                throw th;
                            }
                        }
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("alertCursor result: alarmTime:");
                        sb3.append(j9);
                        sb3.append(" alertId:");
                        sb3.append(j5);
                        sb3.append(" eventId:");
                        sb3.append(j6);
                        sb3.append(" state: ");
                        sb3.append(i3);
                        sb3.append(" minutes:");
                        sb3.append(i7);
                        sb3.append(" declined:");
                        boolean z152 = z9;
                        sb3.append(z152);
                        sb3.append(" responded:");
                        sb3.append(z13);
                        sb3.append(" beginTime:");
                        sb3.append(j7);
                        sb3.append(" endTime:");
                        sb3.append(j8);
                        sb3.append(" allDay:");
                        sb3.append(z3);
                        sb3.append(" alarmTime:");
                        sb3.append(j9);
                        sb3.append(" forceQuiet:");
                        sb3.append(z4);
                        if (AlertUtils.BYPASS_DB) {
                        }
                        Log.d("AlertService", sb3.toString());
                        ContentValues contentValues32 = new ContentValues();
                        z5 = !z152;
                        if (z8) {
                        }
                        if (z5) {
                        }
                        z6 = false;
                        if (i10 == -1) {
                        }
                        if (i10 == 1) {
                        }
                        if (contentValues.size() <= 0) {
                        }
                        if (i3 == 1) {
                        }
                        arrayList7 = arrayList2;
                        arrayList6 = arrayList;
                        map3 = map2;
                        time2 = time;
                        zEquals = z8;
                        sharedPreference2 = i4;
                        i2 = i5;
                        sharedPreference = i9;
                        contentResolver2 = contentResolver;
                        i = i11;
                        arrayList8 = arrayList5;
                        context2 = context;
                        cursor2 = cursor;
                        i12 = 0;
                    } else {
                        uri = uriWithAppendedId;
                    }
                    boolean z132 = z10;
                    i3 = cursor2.getInt(2);
                    if (cursor2.getInt(6) == 0) {
                    }
                    if (AlertUtils.BYPASS_DB) {
                        z3 = z2;
                        i4 = sharedPreference2;
                        time = time2;
                        i5 = i2;
                        z4 = z;
                        i6 = i;
                        map = map4;
                        i7 = i14;
                        contentResolver = contentResolver3;
                        uri2 = uri;
                        i8 = 2;
                        i9 = sharedPreference;
                    }
                    StringBuilder sb32 = new StringBuilder();
                    sb32.append("alertCursor result: alarmTime:");
                    sb32.append(j9);
                    sb32.append(" alertId:");
                    sb32.append(j5);
                    sb32.append(" eventId:");
                    sb32.append(j6);
                    sb32.append(" state: ");
                    sb32.append(i3);
                    sb32.append(" minutes:");
                    sb32.append(i7);
                    sb32.append(" declined:");
                    boolean z1522 = z9;
                    sb32.append(z1522);
                    sb32.append(" responded:");
                    sb32.append(z132);
                    sb32.append(" beginTime:");
                    sb32.append(j7);
                    sb32.append(" endTime:");
                    sb32.append(j8);
                    sb32.append(" allDay:");
                    sb32.append(z3);
                    sb32.append(" alarmTime:");
                    sb32.append(j9);
                    sb32.append(" forceQuiet:");
                    sb32.append(z4);
                    if (AlertUtils.BYPASS_DB) {
                    }
                    Log.d("AlertService", sb32.toString());
                    ContentValues contentValues322 = new ContentValues();
                    z5 = !z1522;
                    if (z8) {
                    }
                    if (z5) {
                    }
                    z6 = false;
                    if (i10 == -1) {
                    }
                    if (i10 == 1) {
                    }
                    if (contentValues.size() <= 0) {
                    }
                    if (i3 == 1) {
                    }
                    arrayList7 = arrayList2;
                    arrayList6 = arrayList;
                    map3 = map2;
                    time2 = time;
                    zEquals = z8;
                    sharedPreference2 = i4;
                    i2 = i5;
                    sharedPreference = i9;
                    contentResolver2 = contentResolver;
                    i = i11;
                    arrayList8 = arrayList5;
                    context2 = context;
                    cursor2 = cursor;
                    i12 = 0;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (OperationApplicationException e3) {
                e = e3;
            } catch (RemoteException e4) {
                e = e4;
            }
        }
        ContentResolver contentResolver4 = contentResolver2;
        ArrayList<ContentProviderOperation> arrayList10 = arrayList8;
        GlobalDismissManager.processEventIds(context, map3.keySet());
        if (arrayList10.size() > 0) {
            contentResolver4.applyBatch("com.android.calendar", arrayList10);
        }
        if (cursor != null) {
            cursor.close();
        }
        return i13;
    }

    private static long getGracePeriodMs(long j, long j2, boolean z) {
        if (z) {
            return 900000L;
        }
        return Math.max(900000L, (j2 - j) / 4);
    }

    private static String getDigestTitle(ArrayList<NotificationInfo> arrayList) {
        StringBuilder sb = new StringBuilder();
        for (NotificationInfo notificationInfo : arrayList) {
            if (!TextUtils.isEmpty(notificationInfo.eventName)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(notificationInfo.eventName);
            }
        }
        return sb.toString();
    }

    private static void postNotification(NotificationInfo notificationInfo, String str, Context context, boolean z, NotificationPrefs notificationPrefs, NotificationMgr notificationMgr, int i) {
        int i2;
        String ringtoneAndSilence;
        boolean z2;
        if (!z) {
            i2 = 0;
        } else {
            i2 = 1;
        }
        if (!notificationInfo.newAlert) {
            isEventAlreadyFired = true;
        } else {
            isEventAlreadyFired = false;
        }
        Log.d("AlertService", "eventId: " + notificationInfo.eventId + "isEventAlreadyFired: " + isEventAlreadyFired);
        String tickerText = getTickerText(notificationInfo.eventName, notificationInfo.location);
        NotificationWrapper notificationWrapperMakeExpandingNotification = AlertReceiver.makeExpandingNotification(context, notificationInfo.eventName, str, notificationInfo.description, notificationInfo.startMillis, notificationInfo.endMillis, notificationInfo.eventId, i, notificationPrefs.getDoPopup(), i2);
        if (!notificationInfo.newAlert) {
            ringtoneAndSilence = "";
            z2 = true;
        } else {
            z2 = notificationPrefs.quietUpdate;
            ringtoneAndSilence = notificationPrefs.getRingtoneAndSilence();
        }
        addNotificationOptions(notificationWrapperMakeExpandingNotification, z2, tickerText, notificationPrefs.getDefaultVibrate(), ringtoneAndSilence, true);
        notificationMgr.notify(i, notificationWrapperMakeExpandingNotification);
        LogUtil.d("AlertService", "postNotification(), notify notification:" + notificationWrapperMakeExpandingNotification.mNotification + ",quietUpdate:" + z2 + ",newAlert:" + notificationInfo.newAlert + ",ringtone:" + ringtoneAndSilence);
        StringBuilder sb = new StringBuilder();
        sb.append("Posting individual alarm notification, eventId:");
        sb.append(notificationInfo.eventId);
        sb.append(", notificationId:");
        sb.append(i);
        sb.append(TextUtils.isEmpty(ringtoneAndSilence) ? ", quiet" : ", LOUD");
        sb.append(z ? ", high-priority" : "");
        Log.d("AlertService", sb.toString());
    }

    private static String getTickerText(String str, String str2) {
        if (!TextUtils.isEmpty(str2)) {
            return str + " - " + str2;
        }
        return str;
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

        NotificationInfo(String str, String str2, String str3, long j, long j2, long j3, boolean z, boolean z2) {
            this.eventName = str;
            this.location = str2;
            this.description = str3;
            this.startMillis = j;
            this.endMillis = j2;
            this.eventId = j3;
            this.newAlert = z2;
            this.allDay = z;
        }
    }

    private static void addNotificationOptions(NotificationWrapper notificationWrapper, boolean z, String str, boolean z2, String str2, boolean z3) {
        Notification notification = notificationWrapper.mNotification;
        if (z3) {
            notification.flags |= 1;
            notification.defaults |= 4;
        }
        if (!z) {
            if (!TextUtils.isEmpty(str)) {
                notification.tickerText = str;
            }
            if (z2) {
                notification.defaults |= 2;
            }
            notification.sound = TextUtils.isEmpty(str2) ? null : Uri.parse(str2);
        }
    }

    static class NotificationPrefs {
        private Context context;
        private SharedPreferences prefs;
        boolean quietUpdate;
        private int doPopup = -1;
        private int defaultVibrate = -1;
        private String ringtone = null;

        NotificationPrefs(Context context, SharedPreferences sharedPreferences, boolean z) {
            this.context = context;
            this.prefs = sharedPreferences;
            this.quietUpdate = z;
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
            String str = this.ringtone;
            this.ringtone = "";
            return str;
        }
    }

    private void doTimeChanged() {
        rescheduleMissedAlarms(getContentResolver(), this, AlertUtils.createAlarmManager(this));
        updateAlertNotification(this);
    }

    private static final void rescheduleMissedAlarms(ContentResolver contentResolver, Context context, AlarmManagerInterface alarmManagerInterface) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        Cursor cursorQuery = contentResolver.query(CalendarContract.CalendarAlerts.CONTENT_URI, new String[]{"alarmTime"}, "state=0 AND alarmTime<? AND alarmTime>? AND end>=?", new String[]{Long.toString(jCurrentTimeMillis), Long.toString(jCurrentTimeMillis - 86400000), Long.toString(jCurrentTimeMillis)}, "alarmTime ASC");
        if (cursorQuery == null) {
            return;
        }
        Log.d("AlertService", "missed alarms found: " + cursorQuery.getCount());
        long j = -1;
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                if (j != j2) {
                    Log.w("AlertService", "rescheduling missed alarm. alarmTime: " + j2);
                    AlertUtils.scheduleAlarm(context, alarmManagerInterface, j2);
                    j = j2;
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    public static HashMap<Long, Integer> getEventIdToNotificationIdMap() {
        return sEventIdToNotificationIdMap;
    }
}
