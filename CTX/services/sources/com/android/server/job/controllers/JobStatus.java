package com.android.server.job.controllers;

import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.net.Network;
import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.format.Time;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.job.GrantedUriPermissions;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public final class JobStatus {
    static final int CONSTRAINTS_OF_INTEREST = -1811939313;
    static final int CONSTRAINT_BACKGROUND_NOT_RESTRICTED = 4194304;
    static final int CONSTRAINT_BATTERY_NOT_LOW = 2;
    static final int CONSTRAINT_CHARGING = 1;
    static final int CONSTRAINT_CONNECTIVITY = 268435456;
    static final int CONSTRAINT_CONTENT_TRIGGER = 67108864;
    static final int CONSTRAINT_DEADLINE = 1073741824;
    static final int CONSTRAINT_DEVICE_NOT_DOZING = 33554432;
    static final int CONSTRAINT_IDLE = 4;
    static final int CONSTRAINT_STORAGE_NOT_LOW = 8;
    static final int CONSTRAINT_TIMING_DELAY = Integer.MIN_VALUE;
    static final boolean DEBUG = JobSchedulerService.DEBUG;
    static final boolean DEBUG_PREPARE = true;
    public static final long DEFAULT_TRIGGER_MAX_DELAY = 120000;
    public static final long DEFAULT_TRIGGER_UPDATE_DELAY = 10000;
    public static final int INTERNAL_FLAG_HAS_FOREGROUND_EXEMPTION = 1;
    public static final long MIN_TRIGGER_MAX_DELAY = 1000;
    public static final long MIN_TRIGGER_UPDATE_DELAY = 500;
    public static final long NO_EARLIEST_RUNTIME = 0;
    public static final long NO_LATEST_RUNTIME = Long.MAX_VALUE;
    public static final int OVERRIDE_FULL = 2;
    public static final int OVERRIDE_SOFT = 1;
    static final int SOFT_OVERRIDE_CONSTRAINTS = -2147483633;
    static final String TAG = "JobSchedulerService";
    public static final int TRACKING_BATTERY = 1;
    public static final int TRACKING_CONNECTIVITY = 2;
    public static final int TRACKING_CONTENT = 4;
    public static final int TRACKING_IDLE = 8;
    public static final int TRACKING_STORAGE = 16;
    public static final int TRACKING_TIME = 32;
    private final long baseHeartbeat;
    final String batteryName;
    final int callingUid;
    public ArraySet<String> changedAuthorities;
    public ArraySet<Uri> changedUris;
    ContentObserverController.JobInstance contentObserverJobInstance;
    public boolean dozeWhitelisted;
    private final long earliestRunTimeElapsedMillis;
    public long enqueueTime;
    public ArrayList<JobWorkItem> executingWork;
    final JobInfo job;
    public int lastEvaluatedPriority;
    private final long latestRunTimeElapsedMillis;
    private int mInternalFlags;
    private long mLastFailedRunTime;
    private long mLastSuccessfulRunTime;
    private Pair<Long, Long> mPersistedUtcTimes;
    public long madeActive;
    public long madePending;
    public Network network;
    public int nextPendingWorkId;
    private final int numFailures;
    public int overrideState;
    public ArrayList<JobWorkItem> pendingWork;
    private boolean prepared;
    final int requiredConstraints;
    int satisfiedConstraints;
    final String sourcePackageName;
    final String sourceTag;
    final int sourceUid;
    final int sourceUserId;
    private int standbyBucket;
    final String tag;
    final int targetSdkVersion;
    private long totalNetworkBytes;
    private int trackingControllers;
    public boolean uidActive;
    private Throwable unpreparedPoint;
    private GrantedUriPermissions uriPerms;
    private long whenStandbyDeferred;

    public int getServiceToken() {
        return this.callingUid;
    }

    private JobStatus(JobInfo jobInfo, int i, int i2, String str, int i3, int i4, long j, String str2, int i5, long j2, long j3, long j4, long j5, int i6) {
        int packageUid;
        String strFlattenToShortString;
        this.unpreparedPoint = null;
        this.satisfiedConstraints = 0;
        this.nextPendingWorkId = 1;
        this.overrideState = 0;
        this.totalNetworkBytes = -1L;
        this.job = jobInfo;
        this.callingUid = i;
        this.targetSdkVersion = i2;
        this.standbyBucket = i4;
        this.baseHeartbeat = j;
        if (i3 != -1 && str != null) {
            try {
                packageUid = AppGlobals.getPackageManager().getPackageUid(str, 0, i3);
            } catch (RemoteException e) {
                packageUid = -1;
            }
        } else {
            packageUid = -1;
        }
        if (packageUid == -1) {
            this.sourceUid = i;
            this.sourceUserId = UserHandle.getUserId(i);
            this.sourcePackageName = jobInfo.getService().getPackageName();
            this.sourceTag = null;
        } else {
            this.sourceUid = packageUid;
            this.sourceUserId = i3;
            this.sourcePackageName = str;
            this.sourceTag = str2;
        }
        if (this.sourceTag != null) {
            strFlattenToShortString = this.sourceTag + ":" + jobInfo.getService().getPackageName();
        } else {
            strFlattenToShortString = jobInfo.getService().flattenToShortString();
        }
        this.batteryName = strFlattenToShortString;
        this.tag = "*job*/" + this.batteryName;
        this.earliestRunTimeElapsedMillis = j2;
        this.latestRunTimeElapsedMillis = j3;
        this.numFailures = i5;
        int constraintFlags = jobInfo.getConstraintFlags();
        constraintFlags = jobInfo.getRequiredNetwork() != null ? constraintFlags | CONSTRAINT_CONNECTIVITY : constraintFlags;
        constraintFlags = j2 != 0 ? constraintFlags | Integer.MIN_VALUE : constraintFlags;
        constraintFlags = j3 != NO_LATEST_RUNTIME ? constraintFlags | CONSTRAINT_DEADLINE : constraintFlags;
        this.requiredConstraints = jobInfo.getTriggerContentUris() != null ? constraintFlags | CONSTRAINT_CONTENT_TRIGGER : constraintFlags;
        this.mLastSuccessfulRunTime = j4;
        this.mLastFailedRunTime = j5;
        this.mInternalFlags = i6;
        updateEstimatedNetworkBytesLocked();
        if (jobInfo.getRequiredNetwork() != null) {
            jobInfo.getRequiredNetwork().networkCapabilities.setSingleUid(this.sourceUid);
        }
    }

    public JobStatus(JobStatus jobStatus) {
        this(jobStatus.getJob(), jobStatus.getUid(), jobStatus.targetSdkVersion, jobStatus.getSourcePackageName(), jobStatus.getSourceUserId(), jobStatus.getStandbyBucket(), jobStatus.getBaseHeartbeat(), jobStatus.getSourceTag(), jobStatus.getNumFailures(), jobStatus.getEarliestRunTime(), jobStatus.getLatestRunTimeElapsed(), jobStatus.getLastSuccessfulRunTime(), jobStatus.getLastFailedRunTime(), jobStatus.getInternalFlags());
        this.mPersistedUtcTimes = jobStatus.mPersistedUtcTimes;
        if (jobStatus.mPersistedUtcTimes != null && DEBUG) {
            Slog.i(TAG, "Cloning job with persisted run times", new RuntimeException("here"));
        }
    }

    public JobStatus(JobInfo jobInfo, int i, String str, int i2, int i3, long j, String str2, long j2, long j3, long j4, long j5, Pair<Long, Long> pair, int i4) {
        this(jobInfo, i, resolveTargetSdkVersion(jobInfo), str, i2, i3, j, str2, 0, j2, j3, j4, j5, i4);
        this.mPersistedUtcTimes = pair;
        if (pair != null && DEBUG) {
            Slog.i(TAG, "+ restored job with RTC times because of bad boot clock");
        }
    }

    public JobStatus(JobStatus jobStatus, long j, long j2, long j3, int i, long j4, long j5) {
        this(jobStatus.job, jobStatus.getUid(), resolveTargetSdkVersion(jobStatus.job), jobStatus.getSourcePackageName(), jobStatus.getSourceUserId(), jobStatus.getStandbyBucket(), j, jobStatus.getSourceTag(), i, j2, j3, j4, j5, jobStatus.getInternalFlags());
    }

    public static JobStatus createFromJobInfo(JobInfo jobInfo, int i, String str, int i2, String str2) {
        long minLatencyMillis;
        long flexMillis;
        long maxExecutionDelayMillis;
        String packageName;
        long jBaseHeartbeatForApp;
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (jobInfo.isPeriodic()) {
            long intervalMillis = jobInfo.getIntervalMillis() + jMillis;
            maxExecutionDelayMillis = intervalMillis;
            flexMillis = intervalMillis - jobInfo.getFlexMillis();
        } else {
            if (jobInfo.hasEarlyConstraint()) {
                minLatencyMillis = jobInfo.getMinLatencyMillis() + jMillis;
            } else {
                minLatencyMillis = 0;
            }
            flexMillis = minLatencyMillis;
            maxExecutionDelayMillis = jobInfo.hasLateConstraint() ? jobInfo.getMaxExecutionDelayMillis() + jMillis : NO_LATEST_RUNTIME;
        }
        if (str == null) {
            packageName = jobInfo.getService().getPackageName();
        } else {
            packageName = str;
        }
        int iStandbyBucketForPackage = JobSchedulerService.standbyBucketForPackage(packageName, i2, jMillis);
        JobSchedulerInternal jobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        if (jobSchedulerInternal != null) {
            jBaseHeartbeatForApp = jobSchedulerInternal.baseHeartbeatForApp(packageName, i2, iStandbyBucketForPackage);
        } else {
            jBaseHeartbeatForApp = 0;
        }
        return new JobStatus(jobInfo, i, resolveTargetSdkVersion(jobInfo), str, i2, iStandbyBucketForPackage, jBaseHeartbeatForApp, str2, 0, flexMillis, maxExecutionDelayMillis, 0L, 0L, 0);
    }

    public void enqueueWorkLocked(IActivityManager iActivityManager, JobWorkItem jobWorkItem) {
        if (this.pendingWork == null) {
            this.pendingWork = new ArrayList<>();
        }
        jobWorkItem.setWorkId(this.nextPendingWorkId);
        this.nextPendingWorkId++;
        if (jobWorkItem.getIntent() != null && GrantedUriPermissions.checkGrantFlags(jobWorkItem.getIntent().getFlags())) {
            jobWorkItem.setGrants(GrantedUriPermissions.createFromIntent(iActivityManager, jobWorkItem.getIntent(), this.sourceUid, this.sourcePackageName, this.sourceUserId, toShortString()));
        }
        this.pendingWork.add(jobWorkItem);
        updateEstimatedNetworkBytesLocked();
    }

    public JobWorkItem dequeueWorkLocked() {
        if (this.pendingWork != null && this.pendingWork.size() > 0) {
            JobWorkItem jobWorkItemRemove = this.pendingWork.remove(0);
            if (jobWorkItemRemove != null) {
                if (this.executingWork == null) {
                    this.executingWork = new ArrayList<>();
                }
                this.executingWork.add(jobWorkItemRemove);
                jobWorkItemRemove.bumpDeliveryCount();
            }
            updateEstimatedNetworkBytesLocked();
            return jobWorkItemRemove;
        }
        return null;
    }

    public boolean hasWorkLocked() {
        return (this.pendingWork != null && this.pendingWork.size() > 0) || hasExecutingWorkLocked();
    }

    public boolean hasExecutingWorkLocked() {
        return this.executingWork != null && this.executingWork.size() > 0;
    }

    private static void ungrantWorkItem(IActivityManager iActivityManager, JobWorkItem jobWorkItem) {
        if (jobWorkItem.getGrants() != null) {
            ((GrantedUriPermissions) jobWorkItem.getGrants()).revoke(iActivityManager);
        }
    }

    public boolean completeWorkLocked(IActivityManager iActivityManager, int i) {
        if (this.executingWork != null) {
            int size = this.executingWork.size();
            for (int i2 = 0; i2 < size; i2++) {
                JobWorkItem jobWorkItem = this.executingWork.get(i2);
                if (jobWorkItem.getWorkId() == i) {
                    this.executingWork.remove(i2);
                    ungrantWorkItem(iActivityManager, jobWorkItem);
                    return true;
                }
            }
        }
        return false;
    }

    private static void ungrantWorkList(IActivityManager iActivityManager, ArrayList<JobWorkItem> arrayList) {
        if (arrayList != null) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ungrantWorkItem(iActivityManager, arrayList.get(i));
            }
        }
    }

    public void stopTrackingJobLocked(IActivityManager iActivityManager, JobStatus jobStatus) {
        if (jobStatus != null) {
            if (this.executingWork != null && this.executingWork.size() > 0) {
                jobStatus.pendingWork = this.executingWork;
            }
            if (jobStatus.pendingWork == null) {
                jobStatus.pendingWork = this.pendingWork;
            } else if (this.pendingWork != null && this.pendingWork.size() > 0) {
                jobStatus.pendingWork.addAll(this.pendingWork);
            }
            this.pendingWork = null;
            this.executingWork = null;
            jobStatus.nextPendingWorkId = this.nextPendingWorkId;
            jobStatus.updateEstimatedNetworkBytesLocked();
        } else {
            ungrantWorkList(iActivityManager, this.pendingWork);
            this.pendingWork = null;
            ungrantWorkList(iActivityManager, this.executingWork);
            this.executingWork = null;
        }
        updateEstimatedNetworkBytesLocked();
    }

    public void prepareLocked(IActivityManager iActivityManager) {
        if (this.prepared) {
            Slog.wtf(TAG, "Already prepared: " + this);
            return;
        }
        this.prepared = true;
        this.unpreparedPoint = null;
        ClipData clipData = this.job.getClipData();
        if (clipData != null) {
            this.uriPerms = GrantedUriPermissions.createFromClip(iActivityManager, clipData, this.sourceUid, this.sourcePackageName, this.sourceUserId, this.job.getClipGrantFlags(), toShortString());
        }
    }

    public void unprepareLocked(IActivityManager iActivityManager) {
        if (!this.prepared) {
            Slog.wtf(TAG, "Hasn't been prepared: " + this);
            if (this.unpreparedPoint != null) {
                Slog.e(TAG, "Was already unprepared at ", this.unpreparedPoint);
                return;
            }
            return;
        }
        this.prepared = false;
        this.unpreparedPoint = new Throwable().fillInStackTrace();
        if (this.uriPerms != null) {
            this.uriPerms.revoke(iActivityManager);
            this.uriPerms = null;
        }
    }

    public boolean isPreparedLocked() {
        return this.prepared;
    }

    public JobInfo getJob() {
        return this.job;
    }

    public int getJobId() {
        return this.job.getId();
    }

    public int getTargetSdkVersion() {
        return this.targetSdkVersion;
    }

    public void printUniqueId(PrintWriter printWriter) {
        UserHandle.formatUid(printWriter, this.callingUid);
        printWriter.print(SliceClientPermissions.SliceAuthority.DELIMITER);
        printWriter.print(this.job.getId());
    }

    public int getNumFailures() {
        return this.numFailures;
    }

    public ComponentName getServiceComponent() {
        return this.job.getService();
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public int getSourceUid() {
        return this.sourceUid;
    }

    public int getSourceUserId() {
        return this.sourceUserId;
    }

    public int getUserId() {
        return UserHandle.getUserId(this.callingUid);
    }

    public int getStandbyBucket() {
        return this.standbyBucket;
    }

    public long getBaseHeartbeat() {
        return this.baseHeartbeat;
    }

    public void setStandbyBucket(int i) {
        this.standbyBucket = i;
    }

    public long getWhenStandbyDeferred() {
        return this.whenStandbyDeferred;
    }

    public void setWhenStandbyDeferred(long j) {
        this.whenStandbyDeferred = j;
    }

    public String getSourceTag() {
        return this.sourceTag;
    }

    public int getUid() {
        return this.callingUid;
    }

    public String getBatteryName() {
        return this.batteryName;
    }

    public String getTag() {
        return this.tag;
    }

    public int getPriority() {
        return this.job.getPriority();
    }

    public int getFlags() {
        return this.job.getFlags();
    }

    public int getInternalFlags() {
        return this.mInternalFlags;
    }

    public void addInternalFlags(int i) {
        this.mInternalFlags = i | this.mInternalFlags;
    }

    public void maybeAddForegroundExemption(Predicate<Integer> predicate) {
        if (!this.job.hasEarlyConstraint() && !this.job.hasLateConstraint() && (this.mInternalFlags & 1) == 0 && predicate.test(Integer.valueOf(getSourceUid()))) {
            addInternalFlags(1);
        }
    }

    private void updateEstimatedNetworkBytesLocked() {
        this.totalNetworkBytes = computeEstimatedNetworkBytesLocked();
    }

    private long computeEstimatedNetworkBytesLocked() {
        long estimatedNetworkBytes = this.job.getEstimatedNetworkBytes();
        if (estimatedNetworkBytes == -1) {
            return -1L;
        }
        long j = 0 + estimatedNetworkBytes;
        if (this.pendingWork != null) {
            for (int i = 0; i < this.pendingWork.size(); i++) {
                long estimatedNetworkBytes2 = this.pendingWork.get(i).getEstimatedNetworkBytes();
                if (estimatedNetworkBytes2 == -1) {
                    return -1L;
                }
                j += estimatedNetworkBytes2;
            }
        }
        return j;
    }

    public long getEstimatedNetworkBytes() {
        return this.totalNetworkBytes;
    }

    public boolean hasConnectivityConstraint() {
        return (this.requiredConstraints & CONSTRAINT_CONNECTIVITY) != 0;
    }

    public boolean hasChargingConstraint() {
        return (this.requiredConstraints & 1) != 0;
    }

    public boolean hasBatteryNotLowConstraint() {
        return (this.requiredConstraints & 2) != 0;
    }

    public boolean hasPowerConstraint() {
        return (this.requiredConstraints & 3) != 0;
    }

    public boolean hasStorageNotLowConstraint() {
        return (this.requiredConstraints & 8) != 0;
    }

    public boolean hasTimingDelayConstraint() {
        return (this.requiredConstraints & Integer.MIN_VALUE) != 0;
    }

    public boolean hasDeadlineConstraint() {
        return (this.requiredConstraints & CONSTRAINT_DEADLINE) != 0;
    }

    public boolean hasIdleConstraint() {
        return (this.requiredConstraints & 4) != 0;
    }

    public boolean hasContentTriggerConstraint() {
        return (this.requiredConstraints & CONSTRAINT_CONTENT_TRIGGER) != 0;
    }

    public long getTriggerContentUpdateDelay() {
        long triggerContentUpdateDelay = this.job.getTriggerContentUpdateDelay();
        if (triggerContentUpdateDelay < 0) {
            return DEFAULT_TRIGGER_UPDATE_DELAY;
        }
        return Math.max(triggerContentUpdateDelay, 500L);
    }

    public long getTriggerContentMaxDelay() {
        long triggerContentMaxDelay = this.job.getTriggerContentMaxDelay();
        if (triggerContentMaxDelay < 0) {
            return DEFAULT_TRIGGER_MAX_DELAY;
        }
        return Math.max(triggerContentMaxDelay, 1000L);
    }

    public boolean isPersisted() {
        return this.job.isPersisted();
    }

    public long getEarliestRunTime() {
        return this.earliestRunTimeElapsedMillis;
    }

    public long getLatestRunTimeElapsed() {
        return this.latestRunTimeElapsedMillis;
    }

    public float getFractionRunTime() {
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (this.earliestRunTimeElapsedMillis == 0 && this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            return 1.0f;
        }
        if (this.earliestRunTimeElapsedMillis == 0) {
            return jMillis >= this.latestRunTimeElapsedMillis ? 1.0f : 0.0f;
        }
        if (this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            return jMillis >= this.earliestRunTimeElapsedMillis ? 1.0f : 0.0f;
        }
        if (jMillis <= this.earliestRunTimeElapsedMillis) {
            return 0.0f;
        }
        if (jMillis >= this.latestRunTimeElapsedMillis) {
            return 1.0f;
        }
        return (jMillis - this.earliestRunTimeElapsedMillis) / (this.latestRunTimeElapsedMillis - this.earliestRunTimeElapsedMillis);
    }

    public Pair<Long, Long> getPersistedUtcTimes() {
        return this.mPersistedUtcTimes;
    }

    public void clearPersistedUtcTimes() {
        this.mPersistedUtcTimes = null;
    }

    boolean setChargingConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(1, z);
    }

    boolean setBatteryNotLowConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(2, z);
    }

    boolean setStorageNotLowConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(8, z);
    }

    boolean setTimingDelayConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(Integer.MIN_VALUE, z);
    }

    boolean setDeadlineConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(CONSTRAINT_DEADLINE, z);
    }

    boolean setIdleConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(4, z);
    }

    boolean setConnectivityConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(CONSTRAINT_CONNECTIVITY, z);
    }

    boolean setContentTriggerConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(CONSTRAINT_CONTENT_TRIGGER, z);
    }

    boolean setDeviceNotDozingConstraintSatisfied(boolean z, boolean z2) {
        this.dozeWhitelisted = z2;
        return setConstraintSatisfied(CONSTRAINT_DEVICE_NOT_DOZING, z);
    }

    boolean setBackgroundNotRestrictedConstraintSatisfied(boolean z) {
        return setConstraintSatisfied(4194304, z);
    }

    boolean setUidActive(boolean z) {
        if (z != this.uidActive) {
            this.uidActive = z;
            return true;
        }
        return false;
    }

    boolean setConstraintSatisfied(int i, boolean z) {
        if (((this.satisfiedConstraints & i) != 0) == z) {
            return false;
        }
        int i2 = this.satisfiedConstraints & (~i);
        if (!z) {
            i = 0;
        }
        this.satisfiedConstraints = i | i2;
        return true;
    }

    boolean isConstraintSatisfied(int i) {
        return (i & this.satisfiedConstraints) != 0;
    }

    boolean clearTrackingController(int i) {
        if ((this.trackingControllers & i) != 0) {
            this.trackingControllers = (~i) & this.trackingControllers;
            return true;
        }
        return false;
    }

    void setTrackingController(int i) {
        this.trackingControllers = i | this.trackingControllers;
    }

    public long getLastSuccessfulRunTime() {
        return this.mLastSuccessfulRunTime;
    }

    public long getLastFailedRunTime() {
        return this.mLastFailedRunTime;
    }

    public boolean isReady() {
        return (isConstraintsSatisfied() || (!this.job.isPeriodic() && hasDeadlineConstraint() && (this.satisfiedConstraints & CONSTRAINT_DEADLINE) != 0)) && ((this.satisfiedConstraints & CONSTRAINT_DEVICE_NOT_DOZING) != 0 || (this.job.getFlags() & 1) != 0) && ((this.satisfiedConstraints & 4194304) != 0);
    }

    public boolean isConstraintsSatisfied() {
        if (this.overrideState == 2) {
            return true;
        }
        int i = this.requiredConstraints & CONSTRAINTS_OF_INTEREST;
        int i2 = CONSTRAINTS_OF_INTEREST & this.satisfiedConstraints;
        if (this.overrideState == 1) {
            i2 |= this.requiredConstraints & SOFT_OVERRIDE_CONSTRAINTS;
        }
        return (i2 & i) == i;
    }

    public boolean matches(int i, int i2) {
        return this.job.getId() == i2 && this.callingUid == i;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("JobStatus{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        sb.append(" u=");
        sb.append(getUserId());
        sb.append(" s=");
        sb.append(getSourceUid());
        if (this.earliestRunTimeElapsedMillis != 0 || this.latestRunTimeElapsedMillis != NO_LATEST_RUNTIME) {
            long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            sb.append(" TIME=");
            formatRunTime(sb, this.earliestRunTimeElapsedMillis, 0L, jMillis);
            sb.append(":");
            formatRunTime(sb, this.latestRunTimeElapsedMillis, NO_LATEST_RUNTIME, jMillis);
        }
        if (this.job.getRequiredNetwork() != null) {
            sb.append(" NET");
        }
        if (this.job.isRequireCharging()) {
            sb.append(" CHARGING");
        }
        if (this.job.isRequireBatteryNotLow()) {
            sb.append(" BATNOTLOW");
        }
        if (this.job.isRequireStorageNotLow()) {
            sb.append(" STORENOTLOW");
        }
        if (this.job.isRequireDeviceIdle()) {
            sb.append(" IDLE");
        }
        if (this.job.isPeriodic()) {
            sb.append(" PERIODIC");
        }
        if (this.job.isPersisted()) {
            sb.append(" PERSISTED");
        }
        if ((this.satisfiedConstraints & CONSTRAINT_DEVICE_NOT_DOZING) == 0) {
            sb.append(" WAIT:DEV_NOT_DOZING");
        }
        if (this.job.getTriggerContentUris() != null) {
            sb.append(" URIS=");
            sb.append(Arrays.toString(this.job.getTriggerContentUris()));
        }
        if (this.numFailures != 0) {
            sb.append(" failures=");
            sb.append(this.numFailures);
        }
        if (isReady()) {
            sb.append(" READY");
        }
        sb.append("}");
        return sb.toString();
    }

    private void formatRunTime(PrintWriter printWriter, long j, long j2, long j3) {
        if (j == j2) {
            printWriter.print("none");
        } else {
            TimeUtils.formatDuration(j - j3, printWriter);
        }
    }

    private void formatRunTime(StringBuilder sb, long j, long j2, long j3) {
        if (j == j2) {
            sb.append("none");
        } else {
            TimeUtils.formatDuration(j - j3, sb);
        }
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        return sb.toString();
    }

    public String toShortStringExceptUniqueId() {
        return Integer.toHexString(System.identityHashCode(this)) + ' ' + this.batteryName;
    }

    public void writeToShortProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.callingUid);
        protoOutputStream.write(1120986464258L, this.job.getId());
        protoOutputStream.write(1138166333443L, this.batteryName);
        protoOutputStream.end(jStart);
    }

    void dumpConstraints(PrintWriter printWriter, int i) {
        if ((i & 1) != 0) {
            printWriter.print(" CHARGING");
        }
        if ((i & 2) != 0) {
            printWriter.print(" BATTERY_NOT_LOW");
        }
        if ((i & 8) != 0) {
            printWriter.print(" STORAGE_NOT_LOW");
        }
        if ((Integer.MIN_VALUE & i) != 0) {
            printWriter.print(" TIMING_DELAY");
        }
        if ((CONSTRAINT_DEADLINE & i) != 0) {
            printWriter.print(" DEADLINE");
        }
        if ((i & 4) != 0) {
            printWriter.print(" IDLE");
        }
        if ((CONSTRAINT_CONNECTIVITY & i) != 0) {
            printWriter.print(" CONNECTIVITY");
        }
        if ((CONSTRAINT_CONTENT_TRIGGER & i) != 0) {
            printWriter.print(" CONTENT_TRIGGER");
        }
        if ((CONSTRAINT_DEVICE_NOT_DOZING & i) != 0) {
            printWriter.print(" DEVICE_NOT_DOZING");
        }
        if ((4194304 & i) != 0) {
            printWriter.print(" BACKGROUND_NOT_RESTRICTED");
        }
        if (i != 0) {
            printWriter.print(" [0x");
            printWriter.print(Integer.toHexString(i));
            printWriter.print("]");
        }
    }

    void dumpConstraints(ProtoOutputStream protoOutputStream, long j, int i) {
        if ((i & 1) != 0) {
            protoOutputStream.write(j, 1);
        }
        if ((i & 2) != 0) {
            protoOutputStream.write(j, 2);
        }
        if ((i & 8) != 0) {
            protoOutputStream.write(j, 3);
        }
        if ((Integer.MIN_VALUE & i) != 0) {
            protoOutputStream.write(j, 4);
        }
        if ((CONSTRAINT_DEADLINE & i) != 0) {
            protoOutputStream.write(j, 5);
        }
        if ((i & 4) != 0) {
            protoOutputStream.write(j, 6);
        }
        if ((CONSTRAINT_CONNECTIVITY & i) != 0) {
            protoOutputStream.write(j, 7);
        }
        if ((CONSTRAINT_CONTENT_TRIGGER & i) != 0) {
            protoOutputStream.write(j, 8);
        }
        if ((i & CONSTRAINT_DEVICE_NOT_DOZING) != 0) {
            protoOutputStream.write(j, 9);
        }
    }

    private void dumpJobWorkItem(PrintWriter printWriter, String str, JobWorkItem jobWorkItem, int i) {
        printWriter.print(str);
        printWriter.print("  #");
        printWriter.print(i);
        printWriter.print(": #");
        printWriter.print(jobWorkItem.getWorkId());
        printWriter.print(" ");
        printWriter.print(jobWorkItem.getDeliveryCount());
        printWriter.print("x ");
        printWriter.println(jobWorkItem.getIntent());
        if (jobWorkItem.getGrants() != null) {
            printWriter.print(str);
            printWriter.println("  URI grants:");
            ((GrantedUriPermissions) jobWorkItem.getGrants()).dump(printWriter, str + "    ");
        }
    }

    private void dumpJobWorkItem(ProtoOutputStream protoOutputStream, long j, JobWorkItem jobWorkItem) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, jobWorkItem.getWorkId());
        protoOutputStream.write(1120986464258L, jobWorkItem.getDeliveryCount());
        if (jobWorkItem.getIntent() != null) {
            jobWorkItem.getIntent().writeToProto(protoOutputStream, 1146756268035L);
        }
        Object grants = jobWorkItem.getGrants();
        if (grants != null) {
            ((GrantedUriPermissions) grants).dump(protoOutputStream, 1146756268036L);
        }
        protoOutputStream.end(jStart);
    }

    private String bucketName(int i) {
        switch (i) {
            case 0:
                return "ACTIVE";
            case 1:
                return "WORKING_SET";
            case 2:
                return "FREQUENT";
            case 3:
                return "RARE";
            case 4:
                return "NEVER";
            default:
                return "Unknown: " + i;
        }
    }

    private static int resolveTargetSdkVersion(JobInfo jobInfo) {
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageTargetSdkVersion(jobInfo.getService().getPackageName());
    }

    public void dump(PrintWriter printWriter, String str, boolean z, long j) {
        printWriter.print(str);
        UserHandle.formatUid(printWriter, this.callingUid);
        printWriter.print(" tag=");
        printWriter.println(this.tag);
        printWriter.print(str);
        printWriter.print("Source: uid=");
        UserHandle.formatUid(printWriter, getSourceUid());
        printWriter.print(" user=");
        printWriter.print(getSourceUserId());
        printWriter.print(" pkg=");
        printWriter.println(getSourcePackageName());
        if (z) {
            printWriter.print(str);
            printWriter.println("JobInfo:");
            printWriter.print(str);
            printWriter.print("  Service: ");
            printWriter.println(this.job.getService().flattenToShortString());
            if (this.job.isPeriodic()) {
                printWriter.print(str);
                printWriter.print("  PERIODIC: interval=");
                TimeUtils.formatDuration(this.job.getIntervalMillis(), printWriter);
                printWriter.print(" flex=");
                TimeUtils.formatDuration(this.job.getFlexMillis(), printWriter);
                printWriter.println();
            }
            if (this.job.isPersisted()) {
                printWriter.print(str);
                printWriter.println("  PERSISTED");
            }
            if (this.job.getPriority() != 0) {
                printWriter.print(str);
                printWriter.print("  Priority: ");
                printWriter.println(this.job.getPriority());
            }
            if (this.job.getFlags() != 0) {
                printWriter.print(str);
                printWriter.print("  Flags: ");
                printWriter.println(Integer.toHexString(this.job.getFlags()));
            }
            if (getInternalFlags() != 0) {
                printWriter.print(str);
                printWriter.print("  Internal flags: ");
                printWriter.print(Integer.toHexString(getInternalFlags()));
                if ((getInternalFlags() & 1) != 0) {
                    printWriter.print(" HAS_FOREGROUND_EXEMPTION");
                }
                printWriter.println();
            }
            printWriter.print(str);
            printWriter.print("  Requires: charging=");
            printWriter.print(this.job.isRequireCharging());
            printWriter.print(" batteryNotLow=");
            printWriter.print(this.job.isRequireBatteryNotLow());
            printWriter.print(" deviceIdle=");
            printWriter.println(this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                printWriter.print(str);
                printWriter.println("  Trigger content URIs:");
                for (int i = 0; i < this.job.getTriggerContentUris().length; i++) {
                    JobInfo.TriggerContentUri triggerContentUri = this.job.getTriggerContentUris()[i];
                    printWriter.print(str);
                    printWriter.print("    ");
                    printWriter.print(Integer.toHexString(triggerContentUri.getFlags()));
                    printWriter.print(' ');
                    printWriter.println(triggerContentUri.getUri());
                }
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    printWriter.print(str);
                    printWriter.print("  Trigger update delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentUpdateDelay(), printWriter);
                    printWriter.println();
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    printWriter.print(str);
                    printWriter.print("  Trigger max delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentMaxDelay(), printWriter);
                    printWriter.println();
                }
            }
            if (this.job.getExtras() != null && !this.job.getExtras().maybeIsEmpty()) {
                printWriter.print(str);
                printWriter.print("  Extras: ");
                printWriter.println(this.job.getExtras().toShortString());
            }
            if (this.job.getTransientExtras() != null && !this.job.getTransientExtras().maybeIsEmpty()) {
                printWriter.print(str);
                printWriter.print("  Transient extras: ");
                printWriter.println(this.job.getTransientExtras().toShortString());
            }
            if (this.job.getClipData() != null) {
                printWriter.print(str);
                printWriter.print("  Clip data: ");
                StringBuilder sb = new StringBuilder(128);
                this.job.getClipData().toShortString(sb);
                printWriter.println(sb);
            }
            if (this.uriPerms != null) {
                printWriter.print(str);
                printWriter.println("  Granted URI permissions:");
                this.uriPerms.dump(printWriter, str + "  ");
            }
            if (this.job.getRequiredNetwork() != null) {
                printWriter.print(str);
                printWriter.print("  Network type: ");
                printWriter.println(this.job.getRequiredNetwork());
            }
            if (this.totalNetworkBytes != -1) {
                printWriter.print(str);
                printWriter.print("  Network bytes: ");
                printWriter.println(this.totalNetworkBytes);
            }
            if (this.job.getMinLatencyMillis() != 0) {
                printWriter.print(str);
                printWriter.print("  Minimum latency: ");
                TimeUtils.formatDuration(this.job.getMinLatencyMillis(), printWriter);
                printWriter.println();
            }
            if (this.job.getMaxExecutionDelayMillis() != 0) {
                printWriter.print(str);
                printWriter.print("  Max execution delay: ");
                TimeUtils.formatDuration(this.job.getMaxExecutionDelayMillis(), printWriter);
                printWriter.println();
            }
            printWriter.print(str);
            printWriter.print("  Backoff: policy=");
            printWriter.print(this.job.getBackoffPolicy());
            printWriter.print(" initial=");
            TimeUtils.formatDuration(this.job.getInitialBackoffMillis(), printWriter);
            printWriter.println();
            if (this.job.hasEarlyConstraint()) {
                printWriter.print(str);
                printWriter.println("  Has early constraint");
            }
            if (this.job.hasLateConstraint()) {
                printWriter.print(str);
                printWriter.println("  Has late constraint");
            }
        }
        printWriter.print(str);
        printWriter.print("Required constraints:");
        dumpConstraints(printWriter, this.requiredConstraints);
        printWriter.println();
        if (z) {
            printWriter.print(str);
            printWriter.print("Satisfied constraints:");
            dumpConstraints(printWriter, this.satisfiedConstraints);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("Unsatisfied constraints:");
            dumpConstraints(printWriter, this.requiredConstraints & (~this.satisfiedConstraints));
            printWriter.println();
            if (this.dozeWhitelisted) {
                printWriter.print(str);
                printWriter.println("Doze whitelisted: true");
            }
            if (this.uidActive) {
                printWriter.print(str);
                printWriter.println("Uid: active");
            }
        }
        if (this.trackingControllers != 0) {
            printWriter.print(str);
            printWriter.print("Tracking:");
            if ((this.trackingControllers & 1) != 0) {
                printWriter.print(" BATTERY");
            }
            if ((this.trackingControllers & 2) != 0) {
                printWriter.print(" CONNECTIVITY");
            }
            if ((this.trackingControllers & 4) != 0) {
                printWriter.print(" CONTENT");
            }
            if ((this.trackingControllers & 8) != 0) {
                printWriter.print(" IDLE");
            }
            if ((this.trackingControllers & 16) != 0) {
                printWriter.print(" STORAGE");
            }
            if ((this.trackingControllers & 32) != 0) {
                printWriter.print(" TIME");
            }
            printWriter.println();
        }
        if (this.changedAuthorities != null) {
            printWriter.print(str);
            printWriter.println("Changed authorities:");
            for (int i2 = 0; i2 < this.changedAuthorities.size(); i2++) {
                printWriter.print(str);
                printWriter.print("  ");
                printWriter.println(this.changedAuthorities.valueAt(i2));
            }
            if (this.changedUris != null) {
                printWriter.print(str);
                printWriter.println("Changed URIs:");
                for (int i3 = 0; i3 < this.changedUris.size(); i3++) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.println(this.changedUris.valueAt(i3));
                }
            }
        }
        if (this.network != null) {
            printWriter.print(str);
            printWriter.print("Network: ");
            printWriter.println(this.network);
        }
        if (this.pendingWork != null && this.pendingWork.size() > 0) {
            printWriter.print(str);
            printWriter.println("Pending work:");
            for (int i4 = 0; i4 < this.pendingWork.size(); i4++) {
                dumpJobWorkItem(printWriter, str, this.pendingWork.get(i4), i4);
            }
        }
        if (this.executingWork != null && this.executingWork.size() > 0) {
            printWriter.print(str);
            printWriter.println("Executing work:");
            for (int i5 = 0; i5 < this.executingWork.size(); i5++) {
                dumpJobWorkItem(printWriter, str, this.executingWork.get(i5), i5);
            }
        }
        printWriter.print(str);
        printWriter.print("Standby bucket: ");
        printWriter.println(bucketName(this.standbyBucket));
        if (this.standbyBucket > 0) {
            printWriter.print(str);
            printWriter.print("Base heartbeat: ");
            printWriter.println(this.baseHeartbeat);
        }
        if (this.whenStandbyDeferred != 0) {
            printWriter.print(str);
            printWriter.print("  Deferred since: ");
            TimeUtils.formatDuration(this.whenStandbyDeferred, j, printWriter);
            printWriter.println();
        }
        printWriter.print(str);
        printWriter.print("Enqueue time: ");
        TimeUtils.formatDuration(this.enqueueTime, j, printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("Run time: earliest=");
        formatRunTime(printWriter, this.earliestRunTimeElapsedMillis, 0L, j);
        printWriter.print(", latest=");
        formatRunTime(printWriter, this.latestRunTimeElapsedMillis, NO_LATEST_RUNTIME, j);
        printWriter.println();
        if (this.numFailures != 0) {
            printWriter.print(str);
            printWriter.print("Num failures: ");
            printWriter.println(this.numFailures);
        }
        Time time = new Time();
        if (this.mLastSuccessfulRunTime != 0) {
            printWriter.print(str);
            printWriter.print("Last successful run: ");
            time.set(this.mLastSuccessfulRunTime);
            printWriter.println(time.format("%Y-%m-%d %H:%M:%S"));
        }
        if (this.mLastFailedRunTime != 0) {
            printWriter.print(str);
            printWriter.print("Last failed run: ");
            time.set(this.mLastFailedRunTime);
            printWriter.println(time.format("%Y-%m-%d %H:%M:%S"));
        }
    }

    public void dump(ProtoOutputStream protoOutputStream, long j, boolean z, long j2) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.callingUid);
        protoOutputStream.write(1138166333442L, this.tag);
        protoOutputStream.write(1120986464259L, getSourceUid());
        protoOutputStream.write(1120986464260L, getSourceUserId());
        protoOutputStream.write(1138166333445L, getSourcePackageName());
        protoOutputStream.write(1112396529688L, getInternalFlags());
        if (z) {
            long jStart2 = protoOutputStream.start(1146756268038L);
            this.job.getService().writeToProto(protoOutputStream, 1146756268033L);
            protoOutputStream.write(1133871366146L, this.job.isPeriodic());
            protoOutputStream.write(1112396529667L, this.job.getIntervalMillis());
            protoOutputStream.write(1112396529668L, this.job.getFlexMillis());
            protoOutputStream.write(1133871366149L, this.job.isPersisted());
            protoOutputStream.write(1172526071814L, this.job.getPriority());
            protoOutputStream.write(1120986464263L, this.job.getFlags());
            protoOutputStream.write(1133871366152L, this.job.isRequireCharging());
            protoOutputStream.write(1133871366153L, this.job.isRequireBatteryNotLow());
            protoOutputStream.write(1133871366154L, this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                for (int i = 0; i < this.job.getTriggerContentUris().length; i++) {
                    long jStart3 = protoOutputStream.start(2246267895819L);
                    JobInfo.TriggerContentUri triggerContentUri = this.job.getTriggerContentUris()[i];
                    protoOutputStream.write(1120986464257L, triggerContentUri.getFlags());
                    Uri uri = triggerContentUri.getUri();
                    if (uri != null) {
                        protoOutputStream.write(1138166333442L, uri.toString());
                    }
                    protoOutputStream.end(jStart3);
                }
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    protoOutputStream.write(1112396529676L, this.job.getTriggerContentUpdateDelay());
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    protoOutputStream.write(1112396529677L, this.job.getTriggerContentMaxDelay());
                }
            }
            if (this.job.getExtras() != null && !this.job.getExtras().maybeIsEmpty()) {
                this.job.getExtras().writeToProto(protoOutputStream, 1146756268046L);
            }
            if (this.job.getTransientExtras() != null && !this.job.getTransientExtras().maybeIsEmpty()) {
                this.job.getTransientExtras().writeToProto(protoOutputStream, 1146756268047L);
            }
            if (this.job.getClipData() != null) {
                this.job.getClipData().writeToProto(protoOutputStream, 1146756268048L);
            }
            if (this.uriPerms != null) {
                this.uriPerms.dump(protoOutputStream, 1146756268049L);
            }
            if (this.job.getRequiredNetwork() != null) {
                this.job.getRequiredNetwork().writeToProto(protoOutputStream, 1146756268050L);
            }
            if (this.totalNetworkBytes != -1) {
                protoOutputStream.write(1112396529683L, this.totalNetworkBytes);
            }
            protoOutputStream.write(1112396529684L, this.job.getMinLatencyMillis());
            protoOutputStream.write(1112396529685L, this.job.getMaxExecutionDelayMillis());
            long jStart4 = protoOutputStream.start(1146756268054L);
            protoOutputStream.write(1159641169921L, this.job.getBackoffPolicy());
            protoOutputStream.write(1112396529666L, this.job.getInitialBackoffMillis());
            protoOutputStream.end(jStart4);
            protoOutputStream.write(1133871366167L, this.job.hasEarlyConstraint());
            protoOutputStream.write(1133871366168L, this.job.hasLateConstraint());
            protoOutputStream.end(jStart2);
        }
        dumpConstraints(protoOutputStream, 2259152797703L, this.requiredConstraints);
        if (z) {
            dumpConstraints(protoOutputStream, 2259152797704L, this.satisfiedConstraints);
            dumpConstraints(protoOutputStream, 2259152797705L, this.requiredConstraints & (~this.satisfiedConstraints));
            protoOutputStream.write(1133871366154L, this.dozeWhitelisted);
        }
        if ((this.trackingControllers & 1) != 0) {
            protoOutputStream.write(2259152797707L, 0);
        }
        if ((this.trackingControllers & 2) != 0) {
            protoOutputStream.write(2259152797707L, 1);
        }
        if ((this.trackingControllers & 4) != 0) {
            protoOutputStream.write(2259152797707L, 2);
        }
        if ((this.trackingControllers & 8) != 0) {
            protoOutputStream.write(2259152797707L, 3);
        }
        if ((this.trackingControllers & 16) != 0) {
            protoOutputStream.write(2259152797707L, 4);
        }
        if ((this.trackingControllers & 32) != 0) {
            protoOutputStream.write(2259152797707L, 5);
        }
        if (this.changedAuthorities != null) {
            for (int i2 = 0; i2 < this.changedAuthorities.size(); i2++) {
                protoOutputStream.write(2237677961228L, this.changedAuthorities.valueAt(i2));
            }
        }
        if (this.changedUris != null) {
            for (int i3 = 0; i3 < this.changedUris.size(); i3++) {
                protoOutputStream.write(2237677961229L, this.changedUris.valueAt(i3).toString());
            }
        }
        if (this.network != null) {
            this.network.writeToProto(protoOutputStream, 1146756268046L);
        }
        if (this.pendingWork != null && this.pendingWork.size() > 0) {
            for (int i4 = 0; i4 < this.pendingWork.size(); i4++) {
                dumpJobWorkItem(protoOutputStream, 2246267895823L, this.pendingWork.get(i4));
            }
        }
        if (this.executingWork != null && this.executingWork.size() > 0) {
            for (int i5 = 0; i5 < this.executingWork.size(); i5++) {
                dumpJobWorkItem(protoOutputStream, 2246267895824L, this.executingWork.get(i5));
            }
        }
        protoOutputStream.write(1159641169937L, this.standbyBucket);
        protoOutputStream.write(1112396529682L, j2 - this.enqueueTime);
        if (this.earliestRunTimeElapsedMillis == 0) {
            protoOutputStream.write(1176821039123L, 0);
        } else {
            protoOutputStream.write(1176821039123L, this.earliestRunTimeElapsedMillis - j2);
        }
        if (this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            protoOutputStream.write(1176821039124L, 0);
        } else {
            protoOutputStream.write(1176821039124L, this.latestRunTimeElapsedMillis - j2);
        }
        protoOutputStream.write(1120986464277L, this.numFailures);
        protoOutputStream.write(1112396529686L, this.mLastSuccessfulRunTime);
        protoOutputStream.write(1112396529687L, this.mLastFailedRunTime);
        protoOutputStream.end(jStart);
    }
}
