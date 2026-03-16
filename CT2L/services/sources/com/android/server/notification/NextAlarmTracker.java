package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

public class NextAlarmTracker {
    private static final String ACTION_TRIGGER = "NextAlarmTracker.trigger";
    private static final long EARLY = 5000;
    private static final String EXTRA_TRIGGER = "trigger";
    private static final long MINUTES = 60000;
    private static final long NEXT_ALARM_UPDATE_DELAY = 1000;
    private static final int REQUEST_CODE = 100;
    private static final long SECONDS = 1000;
    private static final long WAIT_AFTER_BOOT = 20000;
    private static final long WAIT_AFTER_INIT = 300000;
    private AlarmManager mAlarmManager;
    private long mBootCompleted;
    private final Context mContext;
    private int mCurrentUserId;
    private long mInit;
    private boolean mRegistered;
    private long mScheduledAlarmTime;
    private PowerManager.WakeLock mWakeLock;
    private static final String TAG = "NextAlarmTracker";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final H mHandler = new H();
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NextAlarmTracker.DEBUG) {
                Slog.d(NextAlarmTracker.TAG, "onReceive " + action);
            }
            long delay = 0;
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                delay = 1000;
                if (NextAlarmTracker.DEBUG) {
                    Slog.d(NextAlarmTracker.TAG, String.format("  next alarm for user %s: %s", Integer.valueOf(NextAlarmTracker.this.mCurrentUserId), NextAlarmTracker.this.formatAlarmDebug(NextAlarmTracker.this.mAlarmManager.getNextAlarmClock(NextAlarmTracker.this.mCurrentUserId))));
                }
            } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                NextAlarmTracker.this.mBootCompleted = System.currentTimeMillis();
            }
            NextAlarmTracker.this.mHandler.postEvaluate(delay);
            NextAlarmTracker.this.mWakeLock.acquire(NextAlarmTracker.EARLY + delay);
        }
    };

    public interface Callback {
        void onEvaluate(AlarmManager.AlarmClockInfo alarmClockInfo, long j, boolean z);
    }

    public NextAlarmTracker(Context context) {
        this.mContext = context;
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    NextAlarmTracker:");
        pw.print("      len(mCallbacks)=");
        pw.println(this.mCallbacks.size());
        pw.print("      mRegistered=");
        pw.println(this.mRegistered);
        pw.print("      mInit=");
        pw.println(this.mInit);
        pw.print("      mBootCompleted=");
        pw.println(this.mBootCompleted);
        pw.print("      mCurrentUserId=");
        pw.println(this.mCurrentUserId);
        pw.print("      mScheduledAlarmTime=");
        pw.println(formatAlarmDebug(this.mScheduledAlarmTime));
        pw.print("      mWakeLock=");
        pw.println(this.mWakeLock);
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public int getCurrentUserId() {
        return this.mCurrentUserId;
    }

    public AlarmManager.AlarmClockInfo getNextAlarm() {
        return this.mAlarmManager.getNextAlarmClock(this.mCurrentUserId);
    }

    public void onUserSwitched() {
        reset();
    }

    public void init() {
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        PowerManager p = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = p.newWakeLock(1, TAG);
        this.mInit = System.currentTimeMillis();
        reset();
    }

    public void reset() {
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction(ACTION_TRIGGER);
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiverAsUser(this.mReceiver, new UserHandle(this.mCurrentUserId), filter, null, null);
        this.mRegistered = true;
        evaluate();
    }

    public void destroy() {
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mRegistered = false;
        }
    }

    public void evaluate() {
        this.mHandler.postEvaluate(0L);
    }

    private void fireEvaluate(AlarmManager.AlarmClockInfo nextAlarm, long wakeupTime, boolean booted) {
        for (Callback callback : this.mCallbacks) {
            callback.onEvaluate(nextAlarm, wakeupTime, booted);
        }
    }

    private void handleEvaluate() {
        AlarmManager.AlarmClockInfo nextAlarm = this.mAlarmManager.getNextAlarmClock(this.mCurrentUserId);
        long triggerTime = getEarlyTriggerTime(nextAlarm);
        long now = System.currentTimeMillis();
        boolean alarmUpcoming = triggerTime > now;
        boolean booted = isDoneWaitingAfterBoot(now);
        if (DEBUG) {
            Slog.d(TAG, "handleEvaluate nextAlarm=" + formatAlarmDebug(triggerTime) + " alarmUpcoming=" + alarmUpcoming + " booted=" + booted);
        }
        fireEvaluate(nextAlarm, triggerTime, booted);
        if (!booted) {
            if (this.mBootCompleted > 0) {
                now = this.mBootCompleted;
            }
            long recheckTime = now + WAIT_AFTER_BOOT;
            rescheduleAlarm(recheckTime);
            return;
        }
        if (alarmUpcoming) {
            rescheduleAlarm(triggerTime);
        }
    }

    public static long getEarlyTriggerTime(AlarmManager.AlarmClockInfo alarm) {
        if (alarm != null) {
            return alarm.getTriggerTime() - EARLY;
        }
        return 0L;
    }

    private boolean isDoneWaitingAfterBoot(long time) {
        return this.mBootCompleted > 0 ? time - this.mBootCompleted > WAIT_AFTER_BOOT : this.mInit <= 0 || time - this.mInit > WAIT_AFTER_INIT;
    }

    public static String formatDuration(long millis) {
        StringBuilder sb = new StringBuilder();
        TimeUtils.formatDuration(millis, sb);
        return sb.toString();
    }

    public String formatAlarm(AlarmManager.AlarmClockInfo alarm) {
        if (alarm != null) {
            return formatAlarm(alarm.getTriggerTime());
        }
        return null;
    }

    private String formatAlarm(long time) {
        return formatAlarm(time, "Hm", "hma");
    }

    private String formatAlarm(long time, String skeleton24, String skeleton12) {
        String skeleton = DateFormat.is24HourFormat(this.mContext) ? skeleton24 : skeleton12;
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, time).toString();
    }

    public String formatAlarmDebug(AlarmManager.AlarmClockInfo alarm) {
        return formatAlarmDebug(alarm != null ? alarm.getTriggerTime() : 0L);
    }

    public String formatAlarmDebug(long time) {
        return time <= 0 ? Long.toString(time) : String.format("%s (%s)", Long.valueOf(time), formatAlarm(time, "Hms", "hmsa"));
    }

    private void rescheduleAlarm(long time) {
        if (DEBUG) {
            Slog.d(TAG, "rescheduleAlarm " + time);
        }
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 100, new Intent(ACTION_TRIGGER).addFlags(268435456).putExtra(EXTRA_TRIGGER, time), 134217728);
        alarms.cancel(pendingIntent);
        this.mScheduledAlarmTime = time;
        if (time > 0) {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling alarm for %s (in %s)", formatAlarmDebug(time), formatDuration(time - System.currentTimeMillis())));
            }
            alarms.setExact(0, time, pendingIntent);
        }
    }

    private class H extends Handler {
        private static final int MSG_EVALUATE = 1;

        private H() {
        }

        public void postEvaluate(long delay) {
            removeMessages(1);
            sendEmptyMessageDelayed(1, delay);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                NextAlarmTracker.this.handleEvaluate();
            }
        }
    }
}
