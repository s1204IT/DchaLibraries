package com.android.server.notification;

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
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;

public class CountdownConditionProvider extends SystemConditionProviderService {
    private static final String EXTRA_CONDITION_ID = "condition_id";
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "ConditionProviders.CCP";
    private boolean mConnected;
    private final Context mContext = this;
    private final Receiver mReceiver = new Receiver(this, null);
    private long mTime;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    public static final ComponentName COMPONENT = new ComponentName("android", CountdownConditionProvider.class.getName());
    private static final String ACTION = CountdownConditionProvider.class.getName();

    public CountdownConditionProvider() {
        if (DEBUG) {
            Slog.d(TAG, "new CountdownConditionProvider()");
        }
    }

    @Override
    public ComponentName getComponent() {
        return COMPONENT;
    }

    @Override
    public boolean isValidConditionId(Uri id) {
        return ZenModeConfig.isValidCountdownConditionId(id);
    }

    @Override
    public void attachBase(Context base) {
        attachBaseContext(base);
    }

    @Override
    public void onBootComplete() {
    }

    @Override
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    @Override
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    CountdownConditionProvider:");
        pw.print("      mConnected=");
        pw.println(this.mConnected);
        pw.print("      mTime=");
        pw.println(this.mTime);
    }

    @Override
    public void onConnected() {
        if (DEBUG) {
            Slog.d(TAG, "onConnected");
        }
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(ACTION));
        this.mConnected = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Slog.d(TAG, "onDestroy");
        }
        if (this.mConnected) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        this.mConnected = false;
    }

    @Override
    public void onSubscribe(Uri conditionId) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + conditionId);
        }
        this.mTime = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        AlarmManager alarms = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent intent = new Intent(ACTION).putExtra(EXTRA_CONDITION_ID, conditionId).setFlags(1073741824);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 100, intent, 134217728);
        alarms.cancel(pendingIntent);
        if (this.mTime <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        CharSequence span = DateUtils.getRelativeTimeSpanString(this.mTime, now, 60000L);
        if (this.mTime <= now) {
            notifyCondition(newCondition(this.mTime, 0));
        } else {
            alarms.setExact(0, this.mTime, pendingIntent);
        }
        if (!DEBUG) {
            return;
        }
        Object[] objArr = new Object[6];
        objArr[0] = this.mTime <= now ? "Not scheduling" : "Scheduling";
        objArr[1] = ACTION;
        objArr[2] = ts(this.mTime);
        objArr[3] = Long.valueOf(this.mTime - now);
        objArr[4] = span;
        objArr[5] = ts(now);
        Slog.d(TAG, String.format("%s %s for %s, %s in the future (%s), now=%s", objArr));
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
    }

    private final class Receiver extends BroadcastReceiver {
        Receiver(CountdownConditionProvider this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!CountdownConditionProvider.ACTION.equals(intent.getAction())) {
                return;
            }
            Uri conditionId = (Uri) intent.getParcelableExtra(CountdownConditionProvider.EXTRA_CONDITION_ID);
            long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
            if (CountdownConditionProvider.DEBUG) {
                Slog.d(CountdownConditionProvider.TAG, "Countdown condition fired: " + conditionId);
            }
            if (time <= 0) {
                return;
            }
            CountdownConditionProvider.this.notifyCondition(CountdownConditionProvider.newCondition(time, 0));
        }
    }

    private static final Condition newCondition(long time, int state) {
        return new Condition(ZenModeConfig.toCountdownConditionId(time), "", "", "", 0, state, 1);
    }

    public static String tryParseDescription(Uri conditionUri) {
        long time = ZenModeConfig.tryParseCountdownConditionId(conditionUri);
        if (time == 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        CharSequence span = DateUtils.getRelativeTimeSpanString(time, now, 60000L);
        return String.format("Scheduled for %s, %s in the future (%s), now=%s", ts(time), Long.valueOf(time - now), span, ts(now));
    }
}
