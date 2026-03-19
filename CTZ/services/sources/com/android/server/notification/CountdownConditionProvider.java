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
import com.android.server.backup.BackupManagerConstants;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;

public class CountdownConditionProvider extends SystemConditionProviderService {
    private static final String EXTRA_CONDITION_ID = "condition_id";
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "ConditionProviders.CCP";
    private boolean mConnected;
    private boolean mIsAlarm;
    private long mTime;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    public static final ComponentName COMPONENT = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, CountdownConditionProvider.class.getName());
    private static final String ACTION = CountdownConditionProvider.class.getName();
    private final Context mContext = this;
    private final Receiver mReceiver = new Receiver();

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
    public boolean isValidConditionId(Uri uri) {
        return ZenModeConfig.isValidCountdownConditionId(uri);
    }

    @Override
    public void attachBase(Context context) {
        attachBaseContext(context);
    }

    @Override
    public void onBootComplete() {
    }

    @Override
    public IConditionProvider asInterface() {
        return onBind(null);
    }

    @Override
    public void dump(PrintWriter printWriter, NotificationManagerService.DumpFilter dumpFilter) {
        printWriter.println("    CountdownConditionProvider:");
        printWriter.print("      mConnected=");
        printWriter.println(this.mConnected);
        printWriter.print("      mTime=");
        printWriter.println(this.mTime);
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
    public void onSubscribe(Uri uri) {
        if (DEBUG) {
            Slog.d(TAG, "onSubscribe " + uri);
        }
        this.mTime = ZenModeConfig.tryParseCountdownConditionId(uri);
        this.mIsAlarm = ZenModeConfig.isValidCountdownToAlarmConditionId(uri);
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 100, new Intent(ACTION).putExtra(EXTRA_CONDITION_ID, uri).setFlags(1073741824), 134217728);
        alarmManager.cancel(broadcast);
        if (this.mTime > 0) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            CharSequence relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(this.mTime, jCurrentTimeMillis, 60000L);
            if (this.mTime <= jCurrentTimeMillis) {
                notifyCondition(newCondition(this.mTime, this.mIsAlarm, 0));
            } else {
                alarmManager.setExact(0, this.mTime, broadcast);
            }
            if (DEBUG) {
                Object[] objArr = new Object[6];
                objArr[0] = this.mTime <= jCurrentTimeMillis ? "Not scheduling" : "Scheduling";
                objArr[1] = ACTION;
                objArr[2] = ts(this.mTime);
                objArr[3] = Long.valueOf(this.mTime - jCurrentTimeMillis);
                objArr[4] = relativeTimeSpanString;
                objArr[5] = ts(jCurrentTimeMillis);
                Slog.d(TAG, String.format("%s %s for %s, %s in the future (%s), now=%s", objArr));
            }
        }
    }

    @Override
    public void onUnsubscribe(Uri uri) {
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CountdownConditionProvider.ACTION.equals(intent.getAction())) {
                Uri uri = (Uri) intent.getParcelableExtra(CountdownConditionProvider.EXTRA_CONDITION_ID);
                boolean zIsValidCountdownToAlarmConditionId = ZenModeConfig.isValidCountdownToAlarmConditionId(uri);
                long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(uri);
                if (CountdownConditionProvider.DEBUG) {
                    Slog.d(CountdownConditionProvider.TAG, "Countdown condition fired: " + uri);
                }
                if (jTryParseCountdownConditionId > 0) {
                    CountdownConditionProvider.this.notifyCondition(CountdownConditionProvider.newCondition(jTryParseCountdownConditionId, zIsValidCountdownToAlarmConditionId, 0));
                }
            }
        }
    }

    private static final Condition newCondition(long j, boolean z, int i) {
        return new Condition(ZenModeConfig.toCountdownConditionId(j, z), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0, i, 1);
    }

    public static String tryParseDescription(Uri uri) {
        long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(uri);
        if (jTryParseCountdownConditionId == 0) {
            return null;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        return String.format("Scheduled for %s, %s in the future (%s), now=%s", ts(jTryParseCountdownConditionId), Long.valueOf(jTryParseCountdownConditionId - jCurrentTimeMillis), DateUtils.getRelativeTimeSpanString(jTryParseCountdownConditionId, jCurrentTimeMillis, 60000L), ts(jCurrentTimeMillis));
    }
}
