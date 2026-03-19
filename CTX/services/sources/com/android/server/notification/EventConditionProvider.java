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
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

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
    public static final ComponentName COMPONENT = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, EventConditionProvider.class.getName());
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
    public boolean isValidConditionId(Uri uri) {
        return ZenModeConfig.isValidEventConditionId(uri);
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
        printWriter.print("      mBootComplete=");
        printWriter.println(this.mBootComplete);
        dumpUpcomingTime(printWriter, "mNextAlarmTime", this.mNextAlarmTime, System.currentTimeMillis());
        synchronized (this.mSubscriptions) {
            printWriter.println("      mSubscriptions=");
            for (Uri uri : this.mSubscriptions) {
                printWriter.print("        ");
                printWriter.println(uri);
            }
        }
        printWriter.println("      mTrackers=");
        for (int i = 0; i < this.mTrackers.size(); i++) {
            printWriter.print("        user=");
            printWriter.println(this.mTrackers.keyAt(i));
            this.mTrackers.valueAt(i).dump("          ", printWriter);
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                EventConditionProvider.this.reloadTrackers();
            }
        }, intentFilter);
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
    public void onSubscribe(Uri uri) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + uri);
        }
        if (!ZenModeConfig.isValidEventConditionId(uri)) {
            notifyCondition(createCondition(uri, 0));
            return;
        }
        synchronized (this.mSubscriptions) {
            if (this.mSubscriptions.add(uri)) {
                evaluateSubscriptions();
            }
        }
    }

    @Override
    public void onUnsubscribe(Uri uri) {
        if (DEBUG) {
            Slog.d(TAG, "onUnsubscribe " + uri);
        }
        synchronized (this.mSubscriptions) {
            if (this.mSubscriptions.remove(uri)) {
                evaluateSubscriptions();
            }
        }
    }

    @Override
    public void attachBase(Context context) {
        attachBaseContext(context);
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
        for (UserHandle userHandle : UserManager.get(this.mContext).getUserProfiles()) {
            Context contextForUser = userHandle.isSystem() ? this.mContext : getContextForUser(this.mContext, userHandle);
            if (contextForUser == null) {
                Slog.w(TAG, "Unable to create context for user " + userHandle.getIdentifier());
            } else {
                this.mTrackers.put(userHandle.getIdentifier(), new CalendarTracker(this.mContext, contextForUser));
            }
        }
        evaluateSubscriptions();
    }

    private void evaluateSubscriptions() {
        if (!this.mWorker.hasCallbacks(this.mEvaluateSubscriptionsW)) {
            this.mWorker.post(this.mEvaluateSubscriptionsW);
        }
    }

    private void evaluateSubscriptionsW() {
        Iterator<Uri> it;
        CalendarTracker.CheckEventResult checkEventResultCheckEvent;
        Iterator<Uri> it2;
        boolean z;
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
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArrayList<Condition> arrayList = new ArrayList();
        synchronized (this.mSubscriptions) {
            int i = 0;
            for (int i2 = 0; i2 < this.mTrackers.size(); i2++) {
                this.mTrackers.valueAt(i2).setCallback(this.mSubscriptions.isEmpty() ? null : this.mTrackerCallback);
            }
            setRegistered(!this.mSubscriptions.isEmpty());
            Iterator<Uri> it3 = this.mSubscriptions.iterator();
            long j = 0;
            while (it3.hasNext()) {
                Uri next = it3.next();
                ZenModeConfig.EventInfo eventInfoTryParseEventConditionId = ZenModeConfig.tryParseEventConditionId(next);
                if (eventInfoTryParseEventConditionId == null) {
                    arrayList.add(createCondition(next, i));
                    it = it3;
                } else {
                    if (eventInfoTryParseEventConditionId.calendar == null) {
                        int i3 = i;
                        checkEventResultCheckEvent = null;
                        while (i3 < this.mTrackers.size()) {
                            CalendarTracker.CheckEventResult checkEventResultCheckEvent2 = this.mTrackers.valueAt(i3).checkEvent(eventInfoTryParseEventConditionId, jCurrentTimeMillis);
                            if (checkEventResultCheckEvent == null) {
                                it2 = it3;
                                checkEventResultCheckEvent = checkEventResultCheckEvent2;
                            } else {
                                checkEventResultCheckEvent.inEvent |= checkEventResultCheckEvent2.inEvent;
                                it2 = it3;
                                checkEventResultCheckEvent.recheckAt = Math.min(checkEventResultCheckEvent.recheckAt, checkEventResultCheckEvent2.recheckAt);
                            }
                            i3++;
                            it3 = it2;
                        }
                        it = it3;
                    } else {
                        it = it3;
                        int iResolveUserId = ZenModeConfig.EventInfo.resolveUserId(eventInfoTryParseEventConditionId.userId);
                        CalendarTracker calendarTracker = this.mTrackers.get(iResolveUserId);
                        if (calendarTracker == null) {
                            Slog.w(TAG, "No calendar tracker found for user " + iResolveUserId);
                            arrayList.add(createCondition(next, 0));
                        } else {
                            checkEventResultCheckEvent = calendarTracker.checkEvent(eventInfoTryParseEventConditionId, jCurrentTimeMillis);
                        }
                    }
                    if (checkEventResultCheckEvent.recheckAt != 0 && (j == 0 || checkEventResultCheckEvent.recheckAt < j)) {
                        j = checkEventResultCheckEvent.recheckAt;
                    }
                    if (checkEventResultCheckEvent.inEvent) {
                        i = 0;
                        z = true;
                        arrayList.add(createCondition(next, 1));
                    } else {
                        i = 0;
                        arrayList.add(createCondition(next, 0));
                        z = true;
                    }
                    it3 = it;
                }
                it3 = it;
                i = 0;
            }
            rescheduleAlarm(jCurrentTimeMillis, j);
        }
        for (Condition condition : arrayList) {
            if (condition != null) {
                notifyCondition(condition);
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "evaluateSubscriptions took " + (System.currentTimeMillis() - jCurrentTimeMillis));
        }
    }

    private void rescheduleAlarm(long j, long j2) {
        this.mNextAlarmTime = j2;
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_EVALUATE).addFlags(268435456).putExtra(EXTRA_TIME, j2), 134217728);
        alarmManager.cancel(broadcast);
        if (j2 == 0 || j2 < j) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Not scheduling evaluate: ");
                sb.append(j2 == 0 ? "no time specified" : "specified time in the past");
                Slog.d(TAG, sb.toString());
                return;
            }
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, String.format("Scheduling evaluate for %s, in %s, now=%s", ts(j2), formatDuration(j2 - j), ts(j)));
        }
        alarmManager.setExact(0, j2, broadcast);
    }

    private Condition createCondition(Uri uri, int i) {
        return new Condition(uri, NOT_SHOWN, NOT_SHOWN, NOT_SHOWN, 0, i, 2);
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
            registerReceiver(this.mReceiver, intentFilter);
            return;
        }
        unregisterReceiver(this.mReceiver);
    }

    private static Context getContextForUser(Context context, UserHandle userHandle) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
