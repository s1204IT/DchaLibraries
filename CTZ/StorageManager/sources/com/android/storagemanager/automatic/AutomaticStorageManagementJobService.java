package com.android.storagemanager.automatic;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.Utils;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;
import com.android.storagemanager.overlay.FeatureFactory;
import com.android.storagemanager.overlay.StorageManagementJobProvider;

public class AutomaticStorageManagementJobService extends JobService {
    private Clock mClock;
    private StorageManagementJobProvider mProvider;
    private StorageVolumeProvider mVolumeProvider;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (!preconditionsFulfilled()) {
            jobFinished(jobParameters, true);
            return false;
        }
        this.mProvider = FeatureFactory.getFactory(this).getStorageManagementJobProvider();
        if (maybeDisableDueToPolicy(this.mProvider, this, getClock())) {
            jobFinished(jobParameters, false);
            return false;
        }
        if (!volumeNeedsManagement()) {
            Log.i("AsmJobService", "Skipping automatic storage management.");
            Settings.Secure.putLong(getContentResolver(), "automatic_storage_manager_last_run", System.currentTimeMillis());
            jobFinished(jobParameters, false);
            return false;
        }
        if (!Utils.isStorageManagerEnabled(getApplicationContext())) {
            Intent intent = new Intent("com.android.storagemanager.automatic.show_notification");
            intent.setClass(getApplicationContext(), NotificationController.class);
            getApplicationContext().sendBroadcast(intent);
            jobFinished(jobParameters, false);
            return false;
        }
        if (this.mProvider != null) {
            return this.mProvider.onStartJob(this, jobParameters, getDaysToRetain());
        }
        jobFinished(jobParameters, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (this.mProvider != null) {
            return this.mProvider.onStopJob(this, jobParameters);
        }
        return false;
    }

    private int getDaysToRetain() {
        return Settings.Secure.getInt(getContentResolver(), "automatic_storage_manager_days_to_retain", Utils.getDefaultStorageManagerDaysToRetain(getResources()));
    }

    private boolean volumeNeedsManagement() {
        if (this.mVolumeProvider == null) {
            this.mVolumeProvider = new StorageManagerVolumeProvider((StorageManager) getSystemService(StorageManager.class));
        }
        PrivateStorageInfo privateStorageInfo = PrivateStorageInfo.getPrivateStorageInfo(this.mVolumeProvider);
        return privateStorageInfo.freeBytes < (privateStorageInfo.totalBytes * 15) / 100;
    }

    private boolean preconditionsFulfilled() {
        return JobPreconditions.isCharging(getApplicationContext());
    }

    @VisibleForTesting
    static boolean maybeDisableDueToPolicy(StorageManagementJobProvider storageManagementJobProvider, Context context, Clock clock) {
        ContentResolver contentResolver = context.getContentResolver();
        if (storageManagementJobProvider == null || contentResolver == null) {
            return false;
        }
        long disableThresholdMillis = storageManagementJobProvider.getDisableThresholdMillis(context);
        if (disableThresholdMillis < 0) {
            return false;
        }
        long jCurrentTimeMillis = clock.currentTimeMillis();
        boolean z = Settings.Secure.getInt(contentResolver, "automatic_storage_manager_turned_off_by_policy", 0) != 0;
        if (jCurrentTimeMillis <= disableThresholdMillis || z) {
            return false;
        }
        Settings.Secure.putInt(contentResolver, "automatic_storage_manager_turned_off_by_policy", 1);
        Settings.Secure.putInt(contentResolver, "automatic_storage_manager_enabled", 0);
        return true;
    }

    private Clock getClock() {
        if (this.mClock == null) {
            this.mClock = new Clock();
        }
        return this.mClock;
    }

    @VisibleForTesting
    void setClock(Clock clock) {
        this.mClock = clock;
    }

    protected static class Clock {
        protected Clock() {
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}
