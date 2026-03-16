package com.android.server.notification;

import android.R;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ConditionProviderService;
import android.service.notification.IConditionProvider;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.notification.NextAlarmTracker;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;

public class NextAlarmConditionProvider extends ConditionProviderService {
    private static final long BAD_CONDITION = -1;
    private static final long HOURS = 3600000;
    private static final long MINUTES = 60000;
    private static final long SECONDS = 1000;
    private boolean mConnected;
    private long mLookaheadThreshold;
    private boolean mRequesting;
    private final NextAlarmTracker mTracker;
    private static final String TAG = "NextAlarmConditions";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    public static final ComponentName COMPONENT = new ComponentName("android", NextAlarmConditionProvider.class.getName());
    private final Context mContext = this;
    private final ArraySet<Uri> mSubscriptions = new ArraySet<>();
    private final NextAlarmTracker.Callback mTrackerCallback = new NextAlarmTracker.Callback() {
        @Override
        public void onEvaluate(AlarmManager.AlarmClockInfo nextAlarm, long wakeupTime, boolean booted) {
            NextAlarmConditionProvider.this.onEvaluate(nextAlarm, wakeupTime, booted);
        }
    };

    public NextAlarmConditionProvider(NextAlarmTracker tracker) {
        if (DEBUG) {
            Slog.d(TAG, "new NextAlarmConditionProvider()");
        }
        this.mTracker = tracker;
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    NextAlarmConditionProvider:");
        pw.print("      mConnected=");
        pw.println(this.mConnected);
        pw.print("      mLookaheadThreshold=");
        pw.print(this.mLookaheadThreshold);
        pw.print(" (");
        TimeUtils.formatDuration(this.mLookaheadThreshold, pw);
        pw.println(")");
        pw.print("      mSubscriptions=");
        pw.println(this.mSubscriptions);
        pw.print("      mRequesting=");
        pw.println(this.mRequesting);
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mLookaheadThreshold = ((long) PropConfig.getInt(this.mContext, "nextalarm.condition.lookahead", R.integer.config_displayWhiteBalanceDecreaseDebounce)) * HOURS;
        this.mConnected = true;
        this.mTracker.addCallback(this.mTrackerCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Slog.d(TAG, "onDestroy");
        }
        this.mTracker.removeCallback(this.mTrackerCallback);
        this.mConnected = false;
    }

    @Override
    public void onRequestConditions(int relevance) {
        if (DEBUG) {
            Slog.d(TAG, "onRequestConditions relevance=" + relevance);
        }
        if (this.mConnected) {
            this.mRequesting = (relevance & 1) != 0;
            this.mTracker.evaluate();
        }
    }

    @Override
    public void onSubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + conditionId);
        }
        if (tryParseNextAlarmCondition(conditionId) == -1) {
            notifyCondition(conditionId, null, 0, "badCondition");
        } else {
            this.mSubscriptions.add(conditionId);
            this.mTracker.evaluate();
        }
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + conditionId);
        }
        this.mSubscriptions.remove(conditionId);
    }

    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    public IConditionProvider asInterface() {
        return onBind(null);
    }

    private boolean isWithinLookaheadThreshold(AlarmManager.AlarmClockInfo alarm) {
        if (alarm == null) {
            return false;
        }
        long delta = NextAlarmTracker.getEarlyTriggerTime(alarm) - System.currentTimeMillis();
        if (delta > 0) {
            return this.mLookaheadThreshold <= 0 || delta < this.mLookaheadThreshold;
        }
        return false;
    }

    private void notifyCondition(Uri id, AlarmManager.AlarmClockInfo alarm, int state, String reason) {
        String formattedAlarm = alarm == null ? "" : this.mTracker.formatAlarm(alarm);
        if (DEBUG) {
            Slog.d(TAG, "notifyCondition " + Condition.stateToString(state) + " alarm=" + formattedAlarm + " reason=" + reason);
        }
        notifyCondition(new Condition(id, this.mContext.getString(R.string.network_switch_metered_detail, formattedAlarm), this.mContext.getString(R.string.network_switch_metered_toast), formattedAlarm, 0, state, 1));
    }

    private Uri newConditionId(AlarmManager.AlarmClockInfo nextAlarm) {
        return new Uri.Builder().scheme("condition").authority("android").appendPath("next_alarm").appendPath(Integer.toString(this.mTracker.getCurrentUserId())).appendPath(Long.toString(nextAlarm.getTriggerTime())).build();
    }

    private long tryParseNextAlarmCondition(Uri conditionId) {
        if (conditionId != null && conditionId.getScheme().equals("condition") && conditionId.getAuthority().equals("android") && conditionId.getPathSegments().size() == 3 && conditionId.getPathSegments().get(0).equals("next_alarm") && conditionId.getPathSegments().get(1).equals(Integer.toString(this.mTracker.getCurrentUserId()))) {
            return tryParseLong(conditionId.getPathSegments().get(2), -1L);
        }
        return -1L;
    }

    private static long tryParseLong(String value, long defValue) {
        if (!TextUtils.isEmpty(value)) {
            try {
                return Long.valueOf(value).longValue();
            } catch (NumberFormatException e) {
                return defValue;
            }
        }
        return defValue;
    }

    private void onEvaluate(AlarmManager.AlarmClockInfo nextAlarm, long wakeupTime, boolean booted) {
        boolean withinThreshold = isWithinLookaheadThreshold(nextAlarm);
        long nextAlarmTime = nextAlarm != null ? nextAlarm.getTriggerTime() : 0L;
        if (DEBUG) {
            Slog.d(TAG, "onEvaluate mSubscriptions=" + this.mSubscriptions + " nextAlarmTime=" + this.mTracker.formatAlarmDebug(nextAlarmTime) + " nextAlarmWakeup=" + this.mTracker.formatAlarmDebug(wakeupTime) + " withinThreshold=" + withinThreshold + " booted=" + booted);
        }
        ArraySet<Uri> conditions = this.mSubscriptions;
        if (this.mRequesting && nextAlarm != null && withinThreshold) {
            Uri id = newConditionId(nextAlarm);
            if (!conditions.contains(id)) {
                ArraySet<Uri> conditions2 = new ArraySet<>(conditions);
                conditions2.add(id);
                conditions = conditions2;
            }
        }
        for (Uri conditionId : conditions) {
            long time = tryParseNextAlarmCondition(conditionId);
            if (time == -1) {
                notifyCondition(conditionId, nextAlarm, 0, "badCondition");
            } else if (booted) {
                if (time != nextAlarmTime) {
                    notifyCondition(conditionId, nextAlarm, 0, "changed");
                } else if (!withinThreshold) {
                    notifyCondition(conditionId, nextAlarm, 0, "!within");
                } else {
                    notifyCondition(conditionId, nextAlarm, 1, "within");
                }
            } else if (this.mSubscriptions.contains(conditionId)) {
                notifyCondition(conditionId, nextAlarm, 2, "!booted");
            }
        }
    }
}
