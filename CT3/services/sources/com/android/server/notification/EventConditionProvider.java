package com.android.server.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.notification.CalendarTracker;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class EventConditionProvider extends SystemConditionProviderService {
    private static final long CHANGE_DELAY = 2000;
    private static final String EXTRA_TIME = "time";
    private static final String NOT_SHOWN = "...";
    private static final int REQUEST_CODE_EVALUATE = 1;
    private static final String TAG = "ConditionProviders.ECP";
    private boolean mBootComplete;
    private boolean mConnected;
    private long mNextAlarmTime;
    private boolean mRegistered;
    private final HandlerThread mThread;
    private final Handler mWorker;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    public static final ComponentName COMPONENT = new ComponentName("android", EventConditionProvider.class.getName());
    private static final String SIMPLE_NAME = EventConditionProvider.class.getSimpleName();
    private static final String ACTION_EVALUATE = SIMPLE_NAME + ".EVALUATE";
    private final Context mContext = this;
    private final ArraySet<Uri> mSubscriptions = new ArraySet<>();
    private final SparseArray<CalendarTracker> mTrackers = new SparseArray<>();
    private final CalendarTracker.Callback mTrackerCallback = new CalendarTracker.Callback() {
        @Override
        public void onChanged() {
            if (EventConditionProvider.DEBUG) {
                Slog.d(EventConditionProvider.TAG, "mTrackerCallback.onChanged");
            }
            EventConditionProvider.this.mWorker.removeCallbacks(EventConditionProvider.this.mEvaluateSubscriptionsW);
            EventConditionProvider.this.mWorker.postDelayed(EventConditionProvider.this.mEvaluateSubscriptionsW, EventConditionProvider.CHANGE_DELAY);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (EventConditionProvider.DEBUG) {
                Slog.d(EventConditionProvider.TAG, "onReceive " + intent.getAction());
            }
            EventConditionProvider.this.evaluateSubscriptions();
        }
    };
    private final Runnable mEvaluateSubscriptionsW = new Runnable() {
        @Override
        public void run() {
            EventConditionProvider.this.evaluateSubscriptionsW();
        }
    };

    public EventConditionProvider() {
        if (DEBUG) {
            Slog.d(TAG, "new " + SIMPLE_NAME + "()");
        }
        this.mThread = new HandlerThread(TAG, 10);
        this.mThread.start();
        this.mWorker = new Handler(this.mThread.getLooper());
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }

    @Override
    public boolean isValidConditionId(Uri id) {
        return ZenModeConfig.isValidEventConditionId(id);
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
        pw.print("      mBootComplete=");
        pw.println(this.mBootComplete);
        dumpUpcomingTime(pw, "mNextAlarmTime", this.mNextAlarmTime, System.currentTimeMillis());
        synchronized (this.mSubscriptions) {
            pw.println("      mSubscriptions=");
            for (Uri conditionId : this.mSubscriptions) {
                pw.print("        ");
                pw.println(conditionId);
            }
        }
        pw.println("      mTrackers=");
        for (int i = 0; i < this.mTrackers.size(); i++) {
            pw.print("        user=");
            pw.println(this.mTrackers.keyAt(i));
            this.mTrackers.valueAt(i).dump("          ", pw);
        }
    }

    @Override
    public void onBootComplete() {
        if (DEBUG) {
            Slog.d(TAG, "onBootComplete");
        }
        if (this.mBootComplete) {
            return;
        }
        this.mBootComplete = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                EventConditionProvider.this.reloadTrackers();
            }
        }, filter);
        reloadTrackers();
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mConnected = true;
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
        if (!ZenModeConfig.isValidEventConditionId(conditionId)) {
            notifyCondition(createCondition(conditionId, 0));
            return;
        }
        synchronized (this.mSubscriptions) {
            if (this.mSubscriptions.add(conditionId)) {
                evaluateSubscriptions();
            }
        }
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + conditionId);
        }
        synchronized (this.mSubscriptions) {
            if (this.mSubscriptions.remove(conditionId)) {
                evaluateSubscriptions();
            }
        }
    }

    @Override
    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    @Override
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    private void reloadTrackers() {
        if (DEBUG) {
            Slog.d(TAG, "reloadTrackers");
        }
        for (int i = 0; i < this.mTrackers.size(); i++) {
            this.mTrackers.valueAt(i).setCallback(null);
        }
        this.mTrackers.clear();
        for (UserHandle user : UserManager.get(this.mContext).getUserProfiles()) {
            Context context = user.isSystem() ? this.mContext : getContextForUser(this.mContext, user);
            if (context == null) {
                Slog.w(TAG, "Unable to create context for user " + user.getIdentifier());
            } else {
                this.mTrackers.put(user.getIdentifier(), new CalendarTracker(this.mContext, context));
            }
        }
        evaluateSubscriptions();
    }

    private void evaluateSubscriptions() {
        if (this.mWorker.hasCallbacks(this.mEvaluateSubscriptionsW)) {
            return;
        }
        this.mWorker.post(this.mEvaluateSubscriptionsW);
    }

    private void evaluateSubscriptionsW() {
        if (DEBUG) {
            Slog.d(TAG, "evaluateSubscriptions");
        }
        if (!this.mBootComplete) {
            if (DEBUG) {
                Slog.d(TAG, "Skipping evaluate before boot complete");
                return;
            }
            return;
        }
        long now = System.currentTimeMillis();
        List<Condition> conditionsToNotify = new ArrayList<>();
        synchronized (this.mSubscriptions) {
            for (int i = 0; i < this.mTrackers.size(); i++) {
                this.mTrackers.valueAt(i).setCallback(this.mSubscriptions.isEmpty() ? null : this.mTrackerCallback);
            }
            setRegistered(!this.mSubscriptions.isEmpty());
            long reevaluateAt = 0;
            for (Uri conditionId : this.mSubscriptions) {
                ZenModeConfig.EventInfo event = ZenModeConfig.tryParseEventConditionId(conditionId);
                if (event == null) {
                    conditionsToNotify.add(createCondition(conditionId, 0));
                } else {
                    CalendarTracker.CheckEventResult result = null;
                    if (event.calendar == null) {
                        for (int i2 = 0; i2 < this.mTrackers.size(); i2++) {
                            CalendarTracker.CheckEventResult r = this.mTrackers.valueAt(i2).checkEvent(event, now);
                            if (result == null) {
                                result = r;
                            } else {
                                result.inEvent |= r.inEvent;
                                result.recheckAt = Math.min(result.recheckAt, r.recheckAt);
                            }
                        }
                    } else {
                        int userId = ZenModeConfig.EventInfo.resolveUserId(event.userId);
                        CalendarTracker tracker = this.mTrackers.get(userId);
                        if (tracker == null) {
                            Slog.w(TAG, "No calendar tracker found for user " + userId);
                            conditionsToNotify.add(createCondition(conditionId, 0));
                        } else {
                            result = tracker.checkEvent(event, now);
                        }
                    }
                    if (result.recheckAt != 0 && (reevaluateAt == 0 || result.recheckAt < reevaluateAt)) {
                        reevaluateAt = result.recheckAt;
                    }
                    if (!result.inEvent) {
                        conditionsToNotify.add(createCondition(conditionId, 0));
                    } else {
                        conditionsToNotify.add(createCondition(conditionId, 1));
                    }
                }
            }
            rescheduleAlarm(now, reevaluateAt);
        }
        for (Condition condition : conditionsToNotify) {
            if (condition != null) {
                notifyCondition(condition);
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "evaluateSubscriptions took " + (System.currentTimeMillis() - now));
        }
    }

    private void rescheduleAlarm(long now, long time) {
        this.mNextAlarmTime = time;
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_EVALUATE).addFlags(268435456).putExtra(EXTRA_TIME, time), 134217728);
        alarms.cancel(pendingIntent);
        if (time == 0 || time < now) {
            if (DEBUG) {
                Slog.d(TAG, "Not scheduling evaluate: " + (time == 0 ? "no time specified" : "specified time in the past"));
            }
        } else {
            if (DEBUG) {
                Slog.d(TAG, String.format("Scheduling evaluate for %s, in %s, now=%s", ts(time), formatDuration(time - now), ts(now)));
            }
            alarms.setExact(0, time, pendingIntent);
        }
    }

    private Condition createCondition(Uri id, int state) {
        return new Condition(id, NOT_SHOWN, NOT_SHOWN, NOT_SHOWN, 0, state, 2);
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
            registerReceiver(this.mReceiver, filter);
            return;
        }
        unregisterReceiver(this.mReceiver);
    }

    private static Context getContextForUser(Context context, UserHandle user) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
