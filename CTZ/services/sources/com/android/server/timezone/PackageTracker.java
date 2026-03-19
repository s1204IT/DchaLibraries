package com.android.server.timezone;

import android.app.timezone.RulesUpdaterContract;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public class PackageTracker {
    private static final String TAG = "timezone.PackageTracker";
    private int mCheckFailureCount;
    private int mCheckTimeAllowedMillis;
    private boolean mCheckTriggered;
    private final ConfigHelper mConfigHelper;
    private String mDataAppPackageName;
    private int mDelayBeforeReliabilityCheckMillis;
    private final Clock mElapsedRealtimeClock;
    private long mFailedCheckRetryCount;
    private final PackageTrackerIntentHelper mIntentHelper;
    private Long mLastTriggerTimestamp = null;
    private final PackageManagerHelper mPackageManagerHelper;
    private final PackageStatusStorage mPackageStatusStorage;
    private boolean mTrackingEnabled;
    private String mUpdateAppPackageName;

    static PackageTracker create(Context context) {
        Clock clockElapsedRealtimeClock = SystemClock.elapsedRealtimeClock();
        PackageTrackerHelperImpl packageTrackerHelperImpl = new PackageTrackerHelperImpl(context);
        return new PackageTracker(clockElapsedRealtimeClock, packageTrackerHelperImpl, packageTrackerHelperImpl, new PackageStatusStorage(FileUtils.createDir(Environment.getDataSystemDirectory(), "timezone")), new PackageTrackerIntentHelperImpl(context));
    }

    PackageTracker(Clock clock, ConfigHelper configHelper, PackageManagerHelper packageManagerHelper, PackageStatusStorage packageStatusStorage, PackageTrackerIntentHelper packageTrackerIntentHelper) {
        this.mElapsedRealtimeClock = clock;
        this.mConfigHelper = configHelper;
        this.mPackageManagerHelper = packageManagerHelper;
        this.mPackageStatusStorage = packageStatusStorage;
        this.mIntentHelper = packageTrackerIntentHelper;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected synchronized boolean start() {
        this.mTrackingEnabled = this.mConfigHelper.isTrackingEnabled();
        if (!this.mTrackingEnabled) {
            Slog.i(TAG, "Time zone updater / data package tracking explicitly disabled.");
            return false;
        }
        this.mUpdateAppPackageName = this.mConfigHelper.getUpdateAppPackageName();
        this.mDataAppPackageName = this.mConfigHelper.getDataAppPackageName();
        this.mCheckTimeAllowedMillis = this.mConfigHelper.getCheckTimeAllowedMillis();
        this.mFailedCheckRetryCount = this.mConfigHelper.getFailedCheckRetryCount();
        this.mDelayBeforeReliabilityCheckMillis = this.mCheckTimeAllowedMillis + 60000;
        throwIfDeviceSettingsOrAppsAreBad();
        this.mCheckTriggered = false;
        this.mCheckFailureCount = 0;
        try {
            this.mPackageStatusStorage.initialize();
            this.mIntentHelper.initialize(this.mUpdateAppPackageName, this.mDataAppPackageName, this);
            this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
            Slog.i(TAG, "Time zone updater / data package tracking enabled");
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "PackageTracker storage could not be initialized.", e);
            return false;
        }
    }

    private void throwIfDeviceSettingsOrAppsAreBad() {
        throwRuntimeExceptionIfNullOrEmpty(this.mUpdateAppPackageName, "Update app package name missing.");
        throwRuntimeExceptionIfNullOrEmpty(this.mDataAppPackageName, "Data app package name missing.");
        if (this.mFailedCheckRetryCount < 1) {
            throw logAndThrowRuntimeException("mFailedRetryCount=" + this.mFailedCheckRetryCount, null);
        }
        if (this.mCheckTimeAllowedMillis < 1000) {
            throw logAndThrowRuntimeException("mCheckTimeAllowedMillis=" + this.mCheckTimeAllowedMillis, null);
        }
        try {
            if (!this.mPackageManagerHelper.isPrivilegedApp(this.mUpdateAppPackageName)) {
                throw logAndThrowRuntimeException("Update app " + this.mUpdateAppPackageName + " must be a priv-app.", null);
            }
            Slog.d(TAG, "Update app " + this.mUpdateAppPackageName + " is valid.");
            try {
                if (!this.mPackageManagerHelper.isPrivilegedApp(this.mDataAppPackageName)) {
                    throw logAndThrowRuntimeException("Data app " + this.mDataAppPackageName + " must be a priv-app.", null);
                }
                Slog.d(TAG, "Data app " + this.mDataAppPackageName + " is valid.");
            } catch (PackageManager.NameNotFoundException e) {
                throw logAndThrowRuntimeException("Could not determine data app package details for " + this.mDataAppPackageName, e);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw logAndThrowRuntimeException("Could not determine update app package details for " + this.mUpdateAppPackageName, e2);
        }
    }

    public synchronized void triggerUpdateIfNeeded(boolean z) {
        if (!this.mTrackingEnabled) {
            throw new IllegalStateException("Unexpected call. Tracking is disabled.");
        }
        boolean zValidateUpdaterAppManifest = validateUpdaterAppManifest();
        boolean zValidateDataAppManifest = validateDataAppManifest();
        if (zValidateUpdaterAppManifest && zValidateDataAppManifest) {
            if (!z) {
                if (!this.mCheckTriggered) {
                    Slog.d(TAG, "triggerUpdateIfNeeded: First reliability trigger.");
                } else if (isCheckInProgress()) {
                    if (!isCheckResponseOverdue()) {
                        Slog.d(TAG, "triggerUpdateIfNeeded: checkComplete call is not yet overdue. Not triggering.");
                        this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
                        return;
                    }
                } else if (this.mCheckFailureCount > this.mFailedCheckRetryCount) {
                    Slog.i(TAG, "triggerUpdateIfNeeded: number of allowed consecutive check failures exceeded. Stopping reliability triggers until next reboot or package update.");
                    this.mIntentHelper.unscheduleReliabilityTrigger();
                    return;
                } else if (this.mCheckFailureCount == 0) {
                    Slog.i(TAG, "triggerUpdateIfNeeded: No reliability check required. Last check was successful.");
                    this.mIntentHelper.unscheduleReliabilityTrigger();
                    return;
                }
            }
            PackageVersions packageVersionsLookupInstalledPackageVersions = lookupInstalledPackageVersions();
            if (packageVersionsLookupInstalledPackageVersions == null) {
                Slog.e(TAG, "triggerUpdateIfNeeded: currentInstalledVersions was null");
                this.mIntentHelper.unscheduleReliabilityTrigger();
                return;
            }
            PackageStatus packageStatus = this.mPackageStatusStorage.getPackageStatus();
            if (packageStatus == null) {
                Slog.i(TAG, "triggerUpdateIfNeeded: No package status data found. Data check needed.");
            } else if (!packageStatus.mVersions.equals(packageVersionsLookupInstalledPackageVersions)) {
                Slog.i(TAG, "triggerUpdateIfNeeded: Stored package versions=" + packageStatus.mVersions + ", do not match current package versions=" + packageVersionsLookupInstalledPackageVersions + ". Triggering check.");
            } else {
                Slog.i(TAG, "triggerUpdateIfNeeded: Stored package versions match currently installed versions, currentInstalledVersions=" + packageVersionsLookupInstalledPackageVersions + ", packageStatus.mCheckStatus=" + packageStatus.mCheckStatus);
                if (packageStatus.mCheckStatus == 2) {
                    Slog.i(TAG, "triggerUpdateIfNeeded: Prior check succeeded. No need to trigger.");
                    this.mIntentHelper.unscheduleReliabilityTrigger();
                    return;
                }
            }
            CheckToken checkTokenGenerateCheckToken = this.mPackageStatusStorage.generateCheckToken(packageVersionsLookupInstalledPackageVersions);
            if (checkTokenGenerateCheckToken == null) {
                Slog.w(TAG, "triggerUpdateIfNeeded: Unable to generate check token. Not sending check request.");
                this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
                return;
            } else {
                this.mIntentHelper.sendTriggerUpdateCheck(checkTokenGenerateCheckToken);
                this.mCheckTriggered = true;
                setCheckInProgress();
                this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
                return;
            }
        }
        Slog.e(TAG, "No update triggered due to invalid application manifest entries. updaterApp=" + zValidateUpdaterAppManifest + ", dataApp=" + zValidateDataAppManifest);
        this.mIntentHelper.unscheduleReliabilityTrigger();
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected synchronized void recordCheckResult(CheckToken checkToken, boolean z) {
        Slog.i(TAG, "recordOperationResult: checkToken=" + checkToken + " success=" + z);
        if (!this.mTrackingEnabled) {
            if (checkToken == null) {
                Slog.d(TAG, "recordCheckResult: Tracking is disabled and no token has been provided. Resetting tracking state.");
            } else {
                Slog.w(TAG, "recordCheckResult: Tracking is disabled and a token " + checkToken + " has been unexpectedly provided. Resetting tracking state.");
            }
            this.mPackageStatusStorage.resetCheckState();
            return;
        }
        if (checkToken == null) {
            Slog.i(TAG, "recordCheckResult: Unexpectedly missing checkToken, resetting storage state.");
            this.mPackageStatusStorage.resetCheckState();
            this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
            this.mCheckFailureCount = 0;
        } else if (this.mPackageStatusStorage.markChecked(checkToken, z)) {
            setCheckComplete();
            if (z) {
                this.mIntentHelper.unscheduleReliabilityTrigger();
                this.mCheckFailureCount = 0;
            } else {
                this.mIntentHelper.scheduleReliabilityTrigger(this.mDelayBeforeReliabilityCheckMillis);
                this.mCheckFailureCount++;
            }
        } else {
            Slog.i(TAG, "recordCheckResult: could not update token=" + checkToken + " with success=" + z + ". Optimistic lock failure");
            this.mIntentHelper.scheduleReliabilityTrigger((long) this.mDelayBeforeReliabilityCheckMillis);
            this.mCheckFailureCount = this.mCheckFailureCount + 1;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    protected int getCheckFailureCountForTests() {
        return this.mCheckFailureCount;
    }

    private void setCheckInProgress() {
        this.mLastTriggerTimestamp = Long.valueOf(this.mElapsedRealtimeClock.millis());
    }

    private void setCheckComplete() {
        this.mLastTriggerTimestamp = null;
    }

    private boolean isCheckInProgress() {
        return this.mLastTriggerTimestamp != null;
    }

    private boolean isCheckResponseOverdue() {
        return this.mLastTriggerTimestamp != null && this.mElapsedRealtimeClock.millis() > this.mLastTriggerTimestamp.longValue() + ((long) this.mCheckTimeAllowedMillis);
    }

    private PackageVersions lookupInstalledPackageVersions() {
        try {
            return new PackageVersions(this.mPackageManagerHelper.getInstalledPackageVersion(this.mUpdateAppPackageName), this.mPackageManagerHelper.getInstalledPackageVersion(this.mDataAppPackageName));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "lookupInstalledPackageVersions: Unable to resolve installed package versions", e);
            return null;
        }
    }

    private boolean validateDataAppManifest() {
        if (!this.mPackageManagerHelper.contentProviderRegistered("com.android.timezone", this.mDataAppPackageName)) {
            Slog.w(TAG, "validateDataAppManifest: Data app " + this.mDataAppPackageName + " does not expose the required provider with authority=com.android.timezone");
            return false;
        }
        return true;
    }

    private boolean validateUpdaterAppManifest() {
        try {
            if (!this.mPackageManagerHelper.usesPermission(this.mUpdateAppPackageName, "android.permission.UPDATE_TIME_ZONE_RULES")) {
                Slog.w(TAG, "validateUpdaterAppManifest: Updater app " + this.mDataAppPackageName + " does not use permission=android.permission.UPDATE_TIME_ZONE_RULES");
                return false;
            }
            if (!this.mPackageManagerHelper.receiverRegistered(RulesUpdaterContract.createUpdaterIntent(this.mUpdateAppPackageName), "android.permission.TRIGGER_TIME_ZONE_RULES_CHECK")) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "validateUpdaterAppManifest: Updater app " + this.mDataAppPackageName + " does not expose the required broadcast receiver.", e);
            return false;
        }
    }

    private static void throwRuntimeExceptionIfNullOrEmpty(String str, String str2) {
        if (str == null || str.trim().isEmpty()) {
            throw logAndThrowRuntimeException(str2, null);
        }
    }

    private static RuntimeException logAndThrowRuntimeException(String str, Throwable th) {
        Slog.wtf(TAG, str, th);
        throw new RuntimeException(str, th);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("PackageTrackerState: " + toString());
        this.mPackageStatusStorage.dump(printWriter);
    }

    public String toString() {
        return "PackageTracker{mTrackingEnabled=" + this.mTrackingEnabled + ", mUpdateAppPackageName='" + this.mUpdateAppPackageName + "', mDataAppPackageName='" + this.mDataAppPackageName + "', mCheckTimeAllowedMillis=" + this.mCheckTimeAllowedMillis + ", mDelayBeforeReliabilityCheckMillis=" + this.mDelayBeforeReliabilityCheckMillis + ", mFailedCheckRetryCount=" + this.mFailedCheckRetryCount + ", mLastTriggerTimestamp=" + this.mLastTriggerTimestamp + ", mCheckTriggered=" + this.mCheckTriggered + ", mCheckFailureCount=" + this.mCheckFailureCount + '}';
    }
}
