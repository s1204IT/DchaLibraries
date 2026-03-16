package com.android.server.notification;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ConditionProviderService;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.notification.NextAlarmTracker;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.ZenModeHelper;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DowntimeConditionProvider extends ConditionProviderService {
    private static final String ENTER_ACTION = "DowntimeConditions.enter";
    private static final int ENTER_CODE = 100;
    private static final String EXIT_ACTION = "DowntimeConditions.exit";
    private static final int EXIT_CODE = 101;
    private static final String EXTRA_TIME = "time";
    private static final long HOURS = 3600000;
    private static final long MINUTES = 60000;
    private static final long SECONDS = 1000;
    private boolean mConditionClearing;
    private final ConditionProviders mConditionProviders;
    private ZenModeConfig mConfig;
    private boolean mConnected;
    private boolean mDowntimed;
    private long mLookaheadThreshold;
    private boolean mRequesting;
    private final NextAlarmTracker mTracker;
    private final ZenModeHelper mZenModeHelper;
    private static final String TAG = "DowntimeConditions";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    public static final ComponentName COMPONENT = new ComponentName("android", DowntimeConditionProvider.class.getName());
    private final Context mContext = this;
    private final DowntimeCalendar mCalendar = new DowntimeCalendar();
    private final FiredAlarms mFiredAlarms = new FiredAlarms();
    private final ArraySet<Uri> mSubscriptions = new ArraySet<>();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long now = System.currentTimeMillis();
            if (DowntimeConditionProvider.ENTER_ACTION.equals(action) || DowntimeConditionProvider.EXIT_ACTION.equals(action)) {
                long schTime = intent.getLongExtra(DowntimeConditionProvider.EXTRA_TIME, 0L);
                if (DowntimeConditionProvider.DEBUG) {
                    Slog.d(DowntimeConditionProvider.TAG, String.format("%s scheduled for %s, fired at %s, delta=%s", action, DowntimeConditionProvider.ts(schTime), DowntimeConditionProvider.ts(now), Long.valueOf(now - schTime)));
                }
                if (DowntimeConditionProvider.ENTER_ACTION.equals(action)) {
                    DowntimeConditionProvider.this.evaluateAutotrigger();
                } else {
                    DowntimeConditionProvider.this.mDowntimed = false;
                }
                DowntimeConditionProvider.this.mFiredAlarms.clear();
            } else if ("android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                if (DowntimeConditionProvider.DEBUG) {
                    Slog.d(DowntimeConditionProvider.TAG, "timezone changed to " + TimeZone.getDefault());
                }
                DowntimeConditionProvider.this.mCalendar.setTimeZone(TimeZone.getDefault());
                DowntimeConditionProvider.this.mFiredAlarms.clear();
            } else if ("android.intent.action.TIME_SET".equals(action)) {
                if (DowntimeConditionProvider.DEBUG) {
                    Slog.d(DowntimeConditionProvider.TAG, "time changed to " + now);
                }
                DowntimeConditionProvider.this.mFiredAlarms.clear();
            } else if (DowntimeConditionProvider.DEBUG) {
                Slog.d(DowntimeConditionProvider.TAG, action + " fired at " + now);
            }
            DowntimeConditionProvider.this.evaluateSubscriptions();
            DowntimeConditionProvider.this.updateAlarms();
        }
    };
    private final NextAlarmTracker.Callback mTrackerCallback = new NextAlarmTracker.Callback() {
        @Override
        public void onEvaluate(AlarmManager.AlarmClockInfo nextAlarm, long wakeupTime, boolean booted) {
            DowntimeConditionProvider.this.onEvaluateNextAlarm(nextAlarm, wakeupTime, booted);
        }
    };
    private final ZenModeHelper.Callback mZenCallback = new ZenModeHelper.Callback() {
        @Override
        void onZenModeChanged() {
            if (DowntimeConditionProvider.this.mConditionClearing && DowntimeConditionProvider.this.isZenOff()) {
                DowntimeConditionProvider.this.evaluateAutotrigger();
            }
            DowntimeConditionProvider.this.mConditionClearing = false;
            DowntimeConditionProvider.this.evaluateSubscriptions();
        }
    };

    public DowntimeConditionProvider(ConditionProviders conditionProviders, NextAlarmTracker tracker, ZenModeHelper zenModeHelper) {
        if (DEBUG) {
            Slog.d(TAG, "new DowntimeConditionProvider()");
        }
        this.mConditionProviders = conditionProviders;
        this.mTracker = tracker;
        this.mZenModeHelper = zenModeHelper;
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    DowntimeConditionProvider:");
        pw.print("      mConnected=");
        pw.println(this.mConnected);
        pw.print("      mSubscriptions=");
        pw.println(this.mSubscriptions);
        pw.print("      mLookaheadThreshold=");
        pw.print(this.mLookaheadThreshold);
        pw.print(" (");
        TimeUtils.formatDuration(this.mLookaheadThreshold, pw);
        pw.println(")");
        pw.print("      mCalendar=");
        pw.println(this.mCalendar);
        pw.print("      mFiredAlarms=");
        pw.println(this.mFiredAlarms);
        pw.print("      mDowntimed=");
        pw.println(this.mDowntimed);
        pw.print("      mConditionClearing=");
        pw.println(this.mConditionClearing);
        pw.print("      mRequesting=");
        pw.println(this.mRequesting);
    }

    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    public IConditionProvider asInterface() {
        return onBind(null);
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mConnected = true;
        this.mLookaheadThreshold = ((long) PropConfig.getInt(this.mContext, "downtime.condition.lookahead", R.integer.config_displayWhiteBalanceDisplayNominalWhiteCct)) * HOURS;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ENTER_ACTION);
        filter.addAction(EXIT_ACTION);
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
        this.mTracker.addCallback(this.mTrackerCallback);
        this.mZenModeHelper.addCallback(this.mZenCallback);
        init();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Slog.d(TAG, "onDestroy");
        }
        this.mTracker.removeCallback(this.mTrackerCallback);
        this.mZenModeHelper.removeCallback(this.mZenCallback);
        this.mConnected = false;
    }

    @Override
    public void onRequestConditions(int relevance) {
        if (DEBUG) {
            Slog.d(TAG, "onRequestConditions relevance=" + relevance);
        }
        if (this.mConnected) {
            this.mRequesting = (relevance & 1) != 0;
            evaluateSubscriptions();
        }
    }

    @Override
    public void onSubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe conditionId=" + conditionId);
        }
        ZenModeConfig.DowntimeInfo downtime = ZenModeConfig.tryParseDowntimeConditionId(conditionId);
        if (downtime != null) {
            this.mFiredAlarms.clear();
            this.mSubscriptions.add(conditionId);
            notifyCondition(downtime);
        }
    }

    private boolean shouldShowCondition() {
        long now = System.currentTimeMillis();
        if (DEBUG) {
            Slog.d(TAG, "shouldShowCondition now=" + this.mCalendar.isInDowntime(now) + " lookahead=" + (this.mCalendar.nextDowntimeStart(now) <= this.mLookaheadThreshold + now));
        }
        return this.mCalendar.isInDowntime(now) || this.mCalendar.nextDowntimeStart(now) <= this.mLookaheadThreshold + now;
    }

    private void notifyCondition(ZenModeConfig.DowntimeInfo downtime) {
        if (this.mConfig == null) {
            notifyCondition(createCondition(downtime, 2));
            return;
        }
        if (!downtime.equals(this.mConfig.toDowntimeInfo())) {
            notifyCondition(createCondition(downtime, 0));
            return;
        }
        if (!shouldShowCondition()) {
            notifyCondition(createCondition(downtime, 0));
        } else if (isZenNone() && this.mFiredAlarms.findBefore(System.currentTimeMillis())) {
            notifyCondition(createCondition(downtime, 0));
        } else {
            notifyCondition(createCondition(downtime, 1));
        }
    }

    private boolean isZenNone() {
        return this.mZenModeHelper.getZenMode() == 2;
    }

    private boolean isZenOff() {
        return this.mZenModeHelper.getZenMode() == 0;
    }

    private void evaluateSubscriptions() {
        ArraySet<Uri> conditions = this.mSubscriptions;
        if (this.mConfig != null && this.mRequesting && shouldShowCondition()) {
            Uri id = ZenModeConfig.toDowntimeConditionId(this.mConfig.toDowntimeInfo());
            if (!conditions.contains(id)) {
                ArraySet<Uri> conditions2 = new ArraySet<>(conditions);
                conditions2.add(id);
                conditions = conditions2;
            }
        }
        for (Uri conditionId : conditions) {
            ZenModeConfig.DowntimeInfo downtime = ZenModeConfig.tryParseDowntimeConditionId(conditionId);
            if (downtime != null) {
                notifyCondition(downtime);
            }
        }
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
        boolean current = this.mSubscriptions.contains(conditionId);
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe conditionId=" + conditionId + " current=" + current);
        }
        this.mSubscriptions.remove(conditionId);
        this.mFiredAlarms.clear();
    }

    public void setConfig(ZenModeConfig config) {
        if (!Objects.equals(this.mConfig, config)) {
            boolean downtimeChanged = this.mConfig == null || config == null || !this.mConfig.toDowntimeInfo().equals(config.toDowntimeInfo());
            this.mConfig = config;
            if (DEBUG) {
                Slog.d(TAG, "setConfig downtimeChanged=" + downtimeChanged);
            }
            if (this.mConnected && downtimeChanged) {
                this.mDowntimed = false;
                init();
            }
            if (this.mConfig != null && this.mConfig.exitCondition != null && ZenModeConfig.isValidDowntimeConditionId(this.mConfig.exitCondition.id)) {
                this.mDowntimed = true;
            }
        }
    }

    public void onManualConditionClearing() {
        this.mConditionClearing = true;
    }

    private Condition createCondition(ZenModeConfig.DowntimeInfo downtime, int state) {
        if (downtime == null) {
            return null;
        }
        Uri id = ZenModeConfig.toDowntimeConditionId(downtime);
        String skeleton = DateFormat.is24HourFormat(this.mContext) ? "Hm" : "hma";
        Locale locale = Locale.getDefault();
        String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        long now = System.currentTimeMillis();
        long endTime = this.mCalendar.getNextTime(now, downtime.endHour, downtime.endMinute);
        if (isZenNone()) {
            AlarmManager.AlarmClockInfo nextAlarm = this.mTracker.getNextAlarm();
            long nextAlarmTime = nextAlarm != null ? nextAlarm.getTriggerTime() : 0L;
            if (nextAlarmTime > now && nextAlarmTime < endTime) {
                endTime = nextAlarmTime;
            }
        }
        String formatted = new SimpleDateFormat(pattern, locale).format(new Date(endTime));
        String summary = this.mContext.getString(R.string.network_logging_notification_text, formatted);
        String line1 = this.mContext.getString(R.string.network_logging_notification_title);
        return new Condition(id, summary, line1, formatted, 0, state, 1);
    }

    private void init() {
        this.mCalendar.setDowntimeInfo(this.mConfig != null ? this.mConfig.toDowntimeInfo() : null);
        evaluateSubscriptions();
        updateAlarms();
        evaluateAutotrigger();
    }

    private void updateAlarms() {
        if (this.mConfig != null) {
            updateAlarm(ENTER_ACTION, 100, this.mConfig.sleepStartHour, this.mConfig.sleepStartMinute);
            updateAlarm(EXIT_ACTION, 101, this.mConfig.sleepEndHour, this.mConfig.sleepEndMinute);
        }
    }

    private void updateAlarm(String action, int requestCode, int hr, int min) {
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        long now = System.currentTimeMillis();
        long time = this.mCalendar.getNextTime(now, hr, min);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, requestCode, new Intent(action).addFlags(268435456).putExtra(EXTRA_TIME, time), 134217728);
        alarms.cancel(pendingIntent);
        if (this.mConfig.sleepMode != null) {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling %s for %s, in %s, now=%s", action, ts(time), NextAlarmTracker.formatDuration(time - now), ts(now)));
            }
            alarms.setExact(0, time, pendingIntent);
        }
    }

    private static String ts(long time) {
        return new Date(time) + " (" + time + ")";
    }

    private void onEvaluateNextAlarm(AlarmManager.AlarmClockInfo nextAlarm, long wakeupTime, boolean booted) {
        if (booted) {
            if (DEBUG) {
                Slog.d(TAG, "onEvaluateNextAlarm " + this.mTracker.formatAlarmDebug(nextAlarm));
            }
            if (nextAlarm != null && wakeupTime > 0 && System.currentTimeMillis() > wakeupTime) {
                if (DEBUG) {
                    Slog.d(TAG, "Alarm fired: " + this.mTracker.formatAlarmDebug(wakeupTime));
                }
                this.mFiredAlarms.add(wakeupTime);
            }
            evaluateSubscriptions();
        }
    }

    private void evaluateAutotrigger() {
        String skipReason = null;
        if (this.mConfig == null) {
            skipReason = "no config";
        } else if (this.mDowntimed) {
            skipReason = "already downtimed";
        } else if (this.mZenModeHelper.getZenMode() != 0) {
            skipReason = "already in zen";
        } else if (!this.mCalendar.isInDowntime(System.currentTimeMillis())) {
            skipReason = "not in downtime";
        }
        if (skipReason != null) {
            ZenLog.traceDowntimeAutotrigger("Autotrigger skipped: " + skipReason);
            return;
        }
        ZenLog.traceDowntimeAutotrigger("Autotrigger fired");
        this.mZenModeHelper.setZenMode(this.mConfig.sleepNone ? 2 : 1, "downtime");
        Condition condition = createCondition(this.mConfig.toDowntimeInfo(), 1);
        this.mConditionProviders.setZenModeCondition(condition, "downtime");
    }

    private class FiredAlarms {
        private final ArraySet<Long> mFiredAlarms;

        private FiredAlarms() {
            this.mFiredAlarms = new ArraySet<>();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.mFiredAlarms.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(DowntimeConditionProvider.this.mTracker.formatAlarmDebug(this.mFiredAlarms.valueAt(i).longValue()));
            }
            return sb.toString();
        }

        public void add(long firedAlarm) {
            this.mFiredAlarms.add(Long.valueOf(firedAlarm));
        }

        public void clear() {
            this.mFiredAlarms.clear();
        }

        public boolean findBefore(long time) {
            for (int i = 0; i < this.mFiredAlarms.size(); i++) {
                if (this.mFiredAlarms.valueAt(i).longValue() < time) {
                    return true;
                }
            }
            return false;
        }
    }
}
