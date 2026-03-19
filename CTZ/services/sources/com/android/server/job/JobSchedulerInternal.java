package com.android.server.job;

import android.app.job.JobInfo;
import com.android.server.slice.SliceClientPermissions;
import java.util.List;

public interface JobSchedulerInternal {
    void addBackingUpUid(int i);

    long baseHeartbeatForApp(String str, int i, int i2);

    void cancelJobsForUid(int i, String str);

    void clearAllBackingUpUids();

    long currentHeartbeat();

    JobStorePersistStats getPersistStats();

    List<JobInfo> getSystemScheduledPendingJobs();

    long nextHeartbeatForBucket(int i);

    void noteJobStart(String str, int i);

    void removeBackingUpUid(int i);

    void reportAppUsage(String str, int i);

    public static class JobStorePersistStats {
        public int countAllJobsLoaded;
        public int countAllJobsSaved;
        public int countSystemServerJobsLoaded;
        public int countSystemServerJobsSaved;
        public int countSystemSyncManagerJobsLoaded;
        public int countSystemSyncManagerJobsSaved;

        public JobStorePersistStats() {
            this.countAllJobsLoaded = -1;
            this.countSystemServerJobsLoaded = -1;
            this.countSystemSyncManagerJobsLoaded = -1;
            this.countAllJobsSaved = -1;
            this.countSystemServerJobsSaved = -1;
            this.countSystemSyncManagerJobsSaved = -1;
        }

        public JobStorePersistStats(JobStorePersistStats jobStorePersistStats) {
            this.countAllJobsLoaded = -1;
            this.countSystemServerJobsLoaded = -1;
            this.countSystemSyncManagerJobsLoaded = -1;
            this.countAllJobsSaved = -1;
            this.countSystemServerJobsSaved = -1;
            this.countSystemSyncManagerJobsSaved = -1;
            this.countAllJobsLoaded = jobStorePersistStats.countAllJobsLoaded;
            this.countSystemServerJobsLoaded = jobStorePersistStats.countSystemServerJobsLoaded;
            this.countSystemSyncManagerJobsLoaded = jobStorePersistStats.countSystemSyncManagerJobsLoaded;
            this.countAllJobsSaved = jobStorePersistStats.countAllJobsSaved;
            this.countSystemServerJobsSaved = jobStorePersistStats.countSystemServerJobsSaved;
            this.countSystemSyncManagerJobsSaved = jobStorePersistStats.countSystemSyncManagerJobsSaved;
        }

        public String toString() {
            return "FirstLoad: " + this.countAllJobsLoaded + SliceClientPermissions.SliceAuthority.DELIMITER + this.countSystemServerJobsLoaded + SliceClientPermissions.SliceAuthority.DELIMITER + this.countSystemSyncManagerJobsLoaded + " LastSave: " + this.countAllJobsSaved + SliceClientPermissions.SliceAuthority.DELIMITER + this.countSystemServerJobsSaved + SliceClientPermissions.SliceAuthority.DELIMITER + this.countSystemSyncManagerJobsSaved;
        }
    }
}
