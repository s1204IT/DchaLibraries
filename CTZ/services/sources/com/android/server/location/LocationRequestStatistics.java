package com.android.server.location;

import android.os.SystemClock;
import android.util.Log;
import com.android.server.job.controllers.JobStatus;
import java.util.HashMap;

public class LocationRequestStatistics {
    private static final String TAG = "LocationStats";
    public final HashMap<PackageProviderKey, PackageStatistics> statistics = new HashMap<>();

    public void startRequesting(String str, String str2, long j, boolean z) {
        PackageProviderKey packageProviderKey = new PackageProviderKey(str, str2);
        PackageStatistics packageStatistics = this.statistics.get(packageProviderKey);
        if (packageStatistics == null) {
            packageStatistics = new PackageStatistics();
            this.statistics.put(packageProviderKey, packageStatistics);
        }
        packageStatistics.startRequesting(j);
        packageStatistics.updateForeground(z);
    }

    public void stopRequesting(String str, String str2) {
        PackageStatistics packageStatistics = this.statistics.get(new PackageProviderKey(str, str2));
        if (packageStatistics == null) {
            return;
        }
        packageStatistics.stopRequesting();
    }

    public void updateForeground(String str, String str2, boolean z) {
        PackageStatistics packageStatistics = this.statistics.get(new PackageProviderKey(str, str2));
        if (packageStatistics == null) {
            return;
        }
        packageStatistics.updateForeground(z);
    }

    public static class PackageProviderKey {
        public final String packageName;
        public final String providerName;

        public PackageProviderKey(String str, String str2) {
            this.packageName = str;
            this.providerName = str2;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PackageProviderKey)) {
                return false;
            }
            PackageProviderKey packageProviderKey = (PackageProviderKey) obj;
            return this.packageName.equals(packageProviderKey.packageName) && this.providerName.equals(packageProviderKey.providerName);
        }

        public int hashCode() {
            return this.packageName.hashCode() + (31 * this.providerName.hashCode());
        }
    }

    public static class PackageStatistics {
        private long mFastestIntervalMs;
        private long mForegroundDurationMs;
        private final long mInitialElapsedTimeMs;
        private long mLastActivitationElapsedTimeMs;
        private long mLastForegroundElapsedTimeMs;
        private int mNumActiveRequests;
        private long mSlowestIntervalMs;
        private long mTotalDurationMs;

        private PackageStatistics() {
            this.mInitialElapsedTimeMs = SystemClock.elapsedRealtime();
            this.mNumActiveRequests = 0;
            this.mTotalDurationMs = 0L;
            this.mFastestIntervalMs = JobStatus.NO_LATEST_RUNTIME;
            this.mSlowestIntervalMs = 0L;
            this.mForegroundDurationMs = 0L;
            this.mLastForegroundElapsedTimeMs = 0L;
        }

        private void startRequesting(long j) {
            if (this.mNumActiveRequests == 0) {
                this.mLastActivitationElapsedTimeMs = SystemClock.elapsedRealtime();
            }
            if (j < this.mFastestIntervalMs) {
                this.mFastestIntervalMs = j;
            }
            if (j > this.mSlowestIntervalMs) {
                this.mSlowestIntervalMs = j;
            }
            this.mNumActiveRequests++;
        }

        private void updateForeground(boolean z) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (this.mLastForegroundElapsedTimeMs != 0) {
                this.mForegroundDurationMs += jElapsedRealtime - this.mLastForegroundElapsedTimeMs;
            }
            if (!z) {
                jElapsedRealtime = 0;
            }
            this.mLastForegroundElapsedTimeMs = jElapsedRealtime;
        }

        private void stopRequesting() {
            if (this.mNumActiveRequests <= 0) {
                Log.e(LocationRequestStatistics.TAG, "Reference counting corrupted in usage statistics.");
                return;
            }
            this.mNumActiveRequests--;
            if (this.mNumActiveRequests == 0) {
                this.mTotalDurationMs += SystemClock.elapsedRealtime() - this.mLastActivitationElapsedTimeMs;
                updateForeground(false);
            }
        }

        public long getDurationMs() {
            long j = this.mTotalDurationMs;
            if (this.mNumActiveRequests > 0) {
                return j + (SystemClock.elapsedRealtime() - this.mLastActivitationElapsedTimeMs);
            }
            return j;
        }

        public long getForegroundDurationMs() {
            long j = this.mForegroundDurationMs;
            if (this.mLastForegroundElapsedTimeMs != 0) {
                return j + (SystemClock.elapsedRealtime() - this.mLastForegroundElapsedTimeMs);
            }
            return j;
        }

        public long getTimeSinceFirstRequestMs() {
            return SystemClock.elapsedRealtime() - this.mInitialElapsedTimeMs;
        }

        public long getFastestIntervalMs() {
            return this.mFastestIntervalMs;
        }

        public long getSlowestIntervalMs() {
            return this.mSlowestIntervalMs;
        }

        public boolean isActive() {
            return this.mNumActiveRequests > 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.mFastestIntervalMs == this.mSlowestIntervalMs) {
                sb.append("Interval ");
                sb.append(this.mFastestIntervalMs / 1000);
                sb.append(" seconds");
            } else {
                sb.append("Min interval ");
                sb.append(this.mFastestIntervalMs / 1000);
                sb.append(" seconds");
                sb.append(": Max interval ");
                sb.append(this.mSlowestIntervalMs / 1000);
                sb.append(" seconds");
            }
            sb.append(": Duration requested ");
            sb.append((getDurationMs() / 1000) / 60);
            sb.append(" total, ");
            sb.append((getForegroundDurationMs() / 1000) / 60);
            sb.append(" foreground, out of the last ");
            sb.append((getTimeSinceFirstRequestMs() / 1000) / 60);
            sb.append(" minutes");
            if (isActive()) {
                sb.append(": Currently active");
            }
            return sb.toString();
        }
    }
}
