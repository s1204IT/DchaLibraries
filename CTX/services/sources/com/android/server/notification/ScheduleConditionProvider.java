package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ScheduleCalendar;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class ScheduleConditionProvider extends SystemConditionProviderService {
    private static final String EXTRA_TIME = "time";
    private static final String NOT_SHOWN = "...";
    private static final int REQUEST_CODE_EVALUATE = 1;
    private static final String SCP_SETTING = "snoozed_schedule_condition_provider";
    private static final String SEPARATOR = ";";
    static final String TAG = "ConditionProviders.SCP";
    private AlarmManager mAlarmManager;
    private boolean mConnected;
    private long mNextAlarmTime;
    private boolean mRegistered;
    static final boolean DEBUG = true;
    public static final ComponentName COMPONENT = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, ScheduleConditionProvider.class.getName());
    private static final String SIMPLE_NAME = ScheduleConditionProvider.class.getSimpleName();
    private static final String ACTION_EVALUATE = SIMPLE_NAME + ".EVALUATE";
    private final Context mContext = this;
    private final ArrayMap<Uri, ScheduleCalendar> mSubscriptions = new ArrayMap<>();
    private ArraySet<Uri> mSnoozedForAlarm = new ArraySet<>();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ScheduleConditionProvider.DEBUG) {
                Slog.d(ScheduleConditionProvider.TAG, "onReceive " + intent.getAction());
            }
            if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                synchronized (ScheduleConditionProvider.this.mSubscriptions) {
                    Iterator it = ScheduleConditionProvider.this.mSubscriptions.keySet().iterator();
                    while (it.hasNext()) {
                        ScheduleCalendar scheduleCalendar = (ScheduleCalendar) ScheduleConditionProvider.this.mSubscriptions.get((Uri) it.next());
                        if (scheduleCalendar != null) {
                            scheduleCalendar.setTimeZone(Calendar.getInstance().getTimeZone());
                        }
                    }
                }
            }
            ScheduleConditionProvider.this.evaluateSubscriptions();
        }
    };

    public ScheduleConditionProvider() {
        if (DEBUG) {
            Slog.d(TAG, "new " + SIMPLE_NAME + "()");
        }
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }

    @Override
    public boolean isValidConditionId(Uri uri) {
        return ZenModeConfig.isValidScheduleConditionId(uri);
    }

    @Override
    public void dump(PrintWriter printWriter, NotificationManagerService.DumpFilter dumpFilter) {
        printWriter.print("    ");
        printWriter.print(SIMPLE_NAME);
        printWriter.println(":");
        printWriter.print("      mConnected=");
        printWriter.println(this.mConnected);
        printWriter.print("      mRegistered=");
        printWriter.println(this.mRegistered);
        printWriter.println("      mSubscriptions=");
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.mSubscriptions) {
            for (Uri uri : this.mSubscriptions.keySet()) {
                printWriter.print("        ");
                printWriter.print(meetsSchedule(this.mSubscriptions.get(uri), jCurrentTimeMillis) ? "* " : "  ");
                printWriter.println(uri);
                printWriter.print("            ");
                printWriter.println(this.mSubscriptions.get(uri).toString());
            }
        }
        printWriter.println("      snoozed due to alarm: " + TextUtils.join(SEPARATOR, this.mSnoozedForAlarm));
        dumpUpcomingTime(printWriter, "mNextAlarmTime", this.mNextAlarmTime, jCurrentTimeMillis);
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mConnected = true;
        readSnoozed();
    }

    @Override
    public void onBootComplete() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Slog.d(TAG, "onDestroy");
        }
        this.mConnected = false;
    }

    @Override
    public void onSubscribe(Uri uri) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + uri);
        }
        if (!ZenModeConfig.isValidScheduleConditionId(uri)) {
            notifyCondition(createCondition(uri, 3, "invalidId"));
            return;
        }
        synchronized (this.mSubscriptions) {
            this.mSubscriptions.put(uri, ZenModeConfig.toScheduleCalendar(uri));
        }
        evaluateSubscriptions();
    }

    @Override
    public void onUnsubscribe(Uri uri) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + uri);
        }
        synchronized (this.mSubscriptions) {
            this.mSubscriptions.remove(uri);
        }
        removeSnoozed(uri);
        evaluateSubscriptions();
    }

    @Override
    public void attachBase(Context context) {
        attachBaseContext(context);
    }

    @Override
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    private void evaluateSubscriptions() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mNextAlarmTime = 0L;
        long nextAlarm = getNextAlarm();
        ArrayList arrayList = new ArrayList();
        synchronized (this.mSubscriptions) {
            setRegistered(!this.mSubscriptions.isEmpty());
            for (Uri uri : this.mSubscriptions.keySet()) {
                Condition conditionEvaluateSubscriptionLocked = evaluateSubscriptionLocked(uri, this.mSubscriptions.get(uri), jCurrentTimeMillis, nextAlarm);
                if (conditionEvaluateSubscriptionLocked != null) {
                    arrayList.add(conditionEvaluateSubscriptionLocked);
                }
            }
        }
        notifyConditions((Condition[]) arrayList.toArray(new Condition[arrayList.size()]));
        updateAlarm(jCurrentTimeMillis, this.mNextAlarmTime);
    }

    @GuardedBy("mSubscriptions")
    @VisibleForTesting
    Condition evaluateSubscriptionLocked(Uri uri, ScheduleCalendar scheduleCalendar, long j, long j2) {
        Condition conditionCreateCondition;
        Condition conditionCreateCondition2;
        long nextChangeTime;
        if (DEBUG) {
            Slog.d(TAG, String.format("evaluateSubscriptionLocked cal=%s, now=%s, nextUserAlarmTime=%s", scheduleCalendar, ts(j), ts(j2)));
        }
        if (scheduleCalendar == null) {
            Condition conditionCreateCondition3 = createCondition(uri, 3, "!invalidId");
            removeSnoozed(uri);
            return conditionCreateCondition3;
        }
        if (scheduleCalendar.isInSchedule(j)) {
            if (conditionSnoozed(uri)) {
                conditionCreateCondition2 = createCondition(uri, 0, "snoozed");
            } else if (scheduleCalendar.shouldExitForAlarm(j)) {
                conditionCreateCondition = createCondition(uri, 0, "alarmCanceled");
                addSnoozed(uri);
            } else {
                conditionCreateCondition2 = createCondition(uri, 1, "meetsSchedule");
            }
            scheduleCalendar.maybeSetNextAlarm(j, j2);
            nextChangeTime = scheduleCalendar.getNextChangeTime(j);
            if (nextChangeTime > 0 && nextChangeTime > j && (this.mNextAlarmTime == 0 || nextChangeTime < this.mNextAlarmTime)) {
                this.mNextAlarmTime = nextChangeTime;
            }
            return conditionCreateCondition2;
        }
        conditionCreateCondition = createCondition(uri, 0, "!meetsSchedule");
        removeSnoozed(uri);
        conditionCreateCondition2 = conditionCreateCondition;
        scheduleCalendar.maybeSetNextAlarm(j, j2);
        nextChangeTime = scheduleCalendar.getNextChangeTime(j);
        if (nextChangeTime > 0) {
            this.mNextAlarmTime = nextChangeTime;
        }
        return conditionCreateCondition2;
    }

    private void updateAlarm(long j, long j2) {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_EVALUATE).addFlags(268435456).putExtra(EXTRA_TIME, j2), 134217728);
        alarmManager.cancel(broadcast);
        if (j2 > j) {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling evaluate for %s, in %s, now=%s", ts(j2), formatDuration(j2 - j), ts(j)));
            }
            alarmManager.setExact(0, j2, broadcast);
        } else if (DEBUG) {
            Slog.d(TAG, "Not scheduling evaluate");
        }
    }

    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(ActivityManager.getCurrentUser());
        if (nextAlarmClock != null) {
            return nextAlarmClock.getTriggerTime();
        }
        return 0L;
    }

    private boolean meetsSchedule(ScheduleCalendar scheduleCalendar, long j) {
        return scheduleCalendar != null && scheduleCalendar.isInSchedule(j);
    }

    private void setRegistered(boolean z) {
        if (this.mRegistered == z) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setRegistered " + z);
        }
        this.mRegistered = z;
        if (this.mRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            intentFilter.addAction(ACTION_EVALUATE);
            intentFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
            registerReceiver(this.mReceiver, intentFilter);
            return;
        }
        unregisterReceiver(this.mReceiver);
    }

    private Condition createCondition(Uri uri, int i, String str) {
        if (DEBUG) {
            Slog.d(TAG, "notifyCondition " + uri + " " + Condition.stateToString(i) + " reason=" + str);
        }
        return new Condition(uri, NOT_SHOWN, NOT_SHOWN, NOT_SHOWN, 0, i, 2);
    }

    private boolean conditionSnoozed(Uri uri) {
        boolean zContains;
        synchronized (this.mSnoozedForAlarm) {
            zContains = this.mSnoozedForAlarm.contains(uri);
        }
        return zContains;
    }

    @VisibleForTesting
    void addSnoozed(Uri uri) {
        synchronized (this.mSnoozedForAlarm) {
            this.mSnoozedForAlarm.add(uri);
            saveSnoozedLocked();
        }
    }

    private void removeSnoozed(Uri uri) {
        synchronized (this.mSnoozedForAlarm) {
            this.mSnoozedForAlarm.remove(uri);
            saveSnoozedLocked();
        }
    }

    private void saveSnoozedLocked() {
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), SCP_SETTING, TextUtils.join(SEPARATOR, this.mSnoozedForAlarm), ActivityManager.getCurrentUser());
    }

    private void readSnoozed() {
        synchronized (this.mSnoozedForAlarm) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), SCP_SETTING, ActivityManager.getCurrentUser());
                if (stringForUser != null) {
                    String[] strArrSplit = stringForUser.split(SEPARATOR);
                    for (int i = 0; i < strArrSplit.length; i++) {
                        String strTrim = strArrSplit[i];
                        if (strTrim != null) {
                            strTrim = strTrim.trim();
                        }
                        if (!TextUtils.isEmpty(strTrim)) {
                            this.mSnoozedForAlarm.add(Uri.parse(strTrim));
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }
}
