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
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.TimeZone;

public class ScheduleConditionProvider extends SystemConditionProviderService {
    private static final String EXTRA_TIME = "time";
    private static final String NOT_SHOWN = "...";
    private static final int REQUEST_CODE_EVALUATE = 1;
    static final String TAG = "ConditionProviders.SCP";
    private AlarmManager mAlarmManager;
    private boolean mConnected;
    private long mNextAlarmTime;
    private boolean mRegistered;
    static final boolean DEBUG = true;
    public static final ComponentName COMPONENT = new ComponentName("android", ScheduleConditionProvider.class.getName());
    private static final String SIMPLE_NAME = ScheduleConditionProvider.class.getSimpleName();
    private static final String ACTION_EVALUATE = SIMPLE_NAME + ".EVALUATE";
    private final Context mContext = this;
    private final ArrayMap<Uri, ScheduleCalendar> mSubscriptions = new ArrayMap<>();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ScheduleConditionProvider.DEBUG) {
                Slog.d(ScheduleConditionProvider.TAG, "onReceive " + intent.getAction());
            }
            if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                for (Uri conditionId : ScheduleConditionProvider.this.mSubscriptions.keySet()) {
                    ScheduleCalendar cal = (ScheduleCalendar) ScheduleConditionProvider.this.mSubscriptions.get(conditionId);
                    if (cal != null) {
                        cal.setTimeZone(Calendar.getInstance().getTimeZone());
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
    public boolean isValidConditionId(Uri id) {
        return ZenModeConfig.isValidScheduleConditionId(id);
    }

    @Override
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.print("    ");
        pw.print(SIMPLE_NAME);
        pw.println(":");
        pw.print("      mConnected=");
        pw.println(this.mConnected);
        pw.print("      mRegistered=");
        pw.println(this.mRegistered);
        pw.println("      mSubscriptions=");
        long now = System.currentTimeMillis();
        for (Uri conditionId : this.mSubscriptions.keySet()) {
            pw.print("        ");
            pw.print(meetsSchedule(this.mSubscriptions.get(conditionId), now) ? "* " : "  ");
            pw.println(conditionId);
            pw.print("            ");
            pw.println(this.mSubscriptions.get(conditionId).toString());
        }
        dumpUpcomingTime(pw, "mNextAlarmTime", this.mNextAlarmTime, now);
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mConnected = true;
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
    public void onSubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + conditionId);
        }
        if (!ZenModeConfig.isValidScheduleConditionId(conditionId)) {
            notifyCondition(conditionId, 0, "badCondition");
        } else {
            this.mSubscriptions.put(conditionId, toScheduleCalendar(conditionId));
            evaluateSubscriptions();
        }
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + conditionId);
        }
        this.mSubscriptions.remove(conditionId);
        evaluateSubscriptions();
    }

    @Override
    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    @Override
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    private void evaluateSubscriptions() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        setRegistered(!this.mSubscriptions.isEmpty());
        long now = System.currentTimeMillis();
        this.mNextAlarmTime = 0L;
        long nextUserAlarmTime = getNextAlarm();
        for (Uri conditionId : this.mSubscriptions.keySet()) {
            ScheduleCalendar cal = this.mSubscriptions.get(conditionId);
            if (cal != null && cal.isInSchedule(now)) {
                notifyCondition(conditionId, 1, "meetsSchedule");
                cal.maybeSetNextAlarm(now, nextUserAlarmTime);
            } else {
                notifyCondition(conditionId, 0, "!meetsSchedule");
                if (nextUserAlarmTime == 0 && cal != null) {
                    cal.maybeSetNextAlarm(now, nextUserAlarmTime);
                }
            }
            if (cal != null) {
                long nextChangeTime = cal.getNextChangeTime(now);
                if (nextChangeTime > 0 && nextChangeTime > now && (this.mNextAlarmTime == 0 || nextChangeTime < this.mNextAlarmTime)) {
                    this.mNextAlarmTime = nextChangeTime;
                }
            }
        }
        updateAlarm(now, this.mNextAlarmTime);
    }

    private void updateAlarm(long now, long time) {
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_EVALUATE).addFlags(268435456).putExtra(EXTRA_TIME, time), 134217728);
        alarms.cancel(pendingIntent);
        if (time > now) {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling evaluate for %s, in %s, now=%s", ts(time), formatDuration(time - now), ts(now)));
            }
            alarms.setExact(0, time, pendingIntent);
        } else if (DEBUG) {
            Slog.d(TAG, "Not scheduling evaluate");
        }
    }

    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo info = this.mAlarmManager.getNextAlarmClock(ActivityManager.getCurrentUser());
        if (info != null) {
            return info.getTriggerTime();
        }
        return 0L;
    }

    private static boolean meetsSchedule(ScheduleCalendar cal, long time) {
        if (cal != null) {
            return cal.isInSchedule(time);
        }
        return false;
    }

    private static ScheduleCalendar toScheduleCalendar(Uri conditionId) {
        ZenModeConfig.ScheduleInfo schedule = ZenModeConfig.tryParseScheduleConditionId(conditionId);
        if (schedule == null || schedule.days == null || schedule.days.length == 0) {
            return null;
        }
        ScheduleCalendar sc = new ScheduleCalendar();
        sc.setSchedule(schedule);
        sc.setTimeZone(TimeZone.getDefault());
        return sc;
    }

    private void setRegistered(boolean registered) {
        if (this.mRegistered == registered) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "setRegistered " + registered);
        }
        this.mRegistered = registered;
        if (this.mRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction(ACTION_EVALUATE);
            filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
            registerReceiver(this.mReceiver, filter);
            return;
        }
        unregisterReceiver(this.mReceiver);
    }

    private void notifyCondition(Uri conditionId, int state, String reason) {
        if (DEBUG) {
            Slog.d(TAG, "notifyCondition " + conditionId + " " + Condition.stateToString(state) + " reason=" + reason);
        }
        notifyCondition(createCondition(conditionId, state));
    }

    private Condition createCondition(Uri id, int state) {
        return new Condition(id, NOT_SHOWN, NOT_SHOWN, NOT_SHOWN, 0, state, 2);
    }
}
