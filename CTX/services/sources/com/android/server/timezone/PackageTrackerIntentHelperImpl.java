package com.android.server.timezone;

import android.app.timezone.RulesUpdaterContract;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.pm.Settings;

final class PackageTrackerIntentHelperImpl implements PackageTrackerIntentHelper {
    private static final String TAG = "timezone.PackageTrackerIntentHelperImpl";
    private final Context mContext;
    private String mUpdaterAppPackageName;

    PackageTrackerIntentHelperImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public void initialize(String str, String str2, PackageTracker packageTracker) {
        this.mUpdaterAppPackageName = str;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        intentFilter.addDataSchemeSpecificPart(str, 0);
        intentFilter.addDataSchemeSpecificPart(str2, 0);
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiverAsUser(new Receiver(packageTracker), UserHandle.SYSTEM, intentFilter, null, null);
    }

    @Override
    public void sendTriggerUpdateCheck(CheckToken checkToken) {
        RulesUpdaterContract.sendBroadcast(this.mContext, this.mUpdaterAppPackageName, checkToken.toByteArray());
        EventLogTags.writeTimezoneTriggerCheck(checkToken.toString());
    }

    @Override
    public synchronized void scheduleReliabilityTrigger(long j) {
        TimeZoneUpdateIdler.schedule(this.mContext, j);
    }

    @Override
    public synchronized void unscheduleReliabilityTrigger() {
        TimeZoneUpdateIdler.unschedule(this.mContext);
    }

    private static class Receiver extends BroadcastReceiver {
        private final PackageTracker mPackageTracker;

        private Receiver(PackageTracker packageTracker) {
            this.mPackageTracker = packageTracker;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.d(PackageTrackerIntentHelperImpl.TAG, "Received intent: " + intent.toString());
            this.mPackageTracker.triggerUpdateIfNeeded(true);
        }
    }
}
