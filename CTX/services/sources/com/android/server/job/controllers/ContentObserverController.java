package com.android.server.job.controllers;

import android.app.job.JobInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public final class ContentObserverController extends StateController {
    private static final boolean DEBUG;
    private static final int MAX_URIS_REPORTED = 50;
    private static final String TAG = "JobScheduler.ContentObserver";
    private static final int URIS_URGENT_THRESHOLD = 40;
    final Handler mHandler;
    final SparseArray<ArrayMap<JobInfo.TriggerContentUri, ObserverInstance>> mObservers;
    private final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public ContentObserverController(JobSchedulerService jobSchedulerService) {
        super(jobSchedulerService);
        this.mTrackedTasks = new ArraySet<>();
        this.mObservers = new SparseArray<>();
        this.mHandler = new Handler(this.mContext.getMainLooper());
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus.hasContentTriggerConstraint()) {
            if (jobStatus.contentObserverJobInstance == null) {
                jobStatus.contentObserverJobInstance = new JobInstance(jobStatus);
            }
            if (DEBUG) {
                Slog.i(TAG, "Tracking content-trigger job " + jobStatus);
            }
            this.mTrackedTasks.add(jobStatus);
            jobStatus.setTrackingController(4);
            boolean z = false;
            if (jobStatus.contentObserverJobInstance.mChangedAuthorities != null) {
                z = true;
            }
            if (jobStatus.changedAuthorities != null) {
                if (jobStatus.contentObserverJobInstance.mChangedAuthorities == null) {
                    jobStatus.contentObserverJobInstance.mChangedAuthorities = new ArraySet<>();
                }
                Iterator<String> it = jobStatus.changedAuthorities.iterator();
                while (it.hasNext()) {
                    jobStatus.contentObserverJobInstance.mChangedAuthorities.add(it.next());
                }
                if (jobStatus.changedUris != null) {
                    if (jobStatus.contentObserverJobInstance.mChangedUris == null) {
                        jobStatus.contentObserverJobInstance.mChangedUris = new ArraySet<>();
                    }
                    Iterator<Uri> it2 = jobStatus.changedUris.iterator();
                    while (it2.hasNext()) {
                        jobStatus.contentObserverJobInstance.mChangedUris.add(it2.next());
                    }
                }
                jobStatus.changedAuthorities = null;
                jobStatus.changedUris = null;
                z = true;
            }
            jobStatus.changedAuthorities = null;
            jobStatus.changedUris = null;
            jobStatus.setContentTriggerConstraintSatisfied(z);
        }
        if (jobStatus2 != null && jobStatus2.contentObserverJobInstance != null) {
            jobStatus2.contentObserverJobInstance.detachLocked();
            jobStatus2.contentObserverJobInstance = null;
        }
    }

    @Override
    public void prepareForExecutionLocked(JobStatus jobStatus) {
        if (jobStatus.hasContentTriggerConstraint() && jobStatus.contentObserverJobInstance != null) {
            jobStatus.changedUris = jobStatus.contentObserverJobInstance.mChangedUris;
            jobStatus.changedAuthorities = jobStatus.contentObserverJobInstance.mChangedAuthorities;
            jobStatus.contentObserverJobInstance.mChangedUris = null;
            jobStatus.contentObserverJobInstance.mChangedAuthorities = null;
        }
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus jobStatus2, boolean z) {
        if (jobStatus.clearTrackingController(4)) {
            this.mTrackedTasks.remove(jobStatus);
            if (jobStatus.contentObserverJobInstance != null) {
                jobStatus.contentObserverJobInstance.unscheduleLocked();
                if (jobStatus2 != null) {
                    if (jobStatus.contentObserverJobInstance != null && jobStatus.contentObserverJobInstance.mChangedAuthorities != null) {
                        if (jobStatus2.contentObserverJobInstance == null) {
                            jobStatus2.contentObserverJobInstance = new JobInstance(jobStatus2);
                        }
                        jobStatus2.contentObserverJobInstance.mChangedAuthorities = jobStatus.contentObserverJobInstance.mChangedAuthorities;
                        jobStatus2.contentObserverJobInstance.mChangedUris = jobStatus.contentObserverJobInstance.mChangedUris;
                        jobStatus.contentObserverJobInstance.mChangedAuthorities = null;
                        jobStatus.contentObserverJobInstance.mChangedUris = null;
                    }
                } else {
                    jobStatus.contentObserverJobInstance.detachLocked();
                    jobStatus.contentObserverJobInstance = null;
                }
            }
            if (DEBUG) {
                Slog.i(TAG, "No longer tracking job " + jobStatus);
            }
        }
    }

    @Override
    public void rescheduleForFailureLocked(JobStatus jobStatus, JobStatus jobStatus2) {
        if (jobStatus2.hasContentTriggerConstraint() && jobStatus.hasContentTriggerConstraint()) {
            jobStatus.changedAuthorities = jobStatus2.changedAuthorities;
            jobStatus.changedUris = jobStatus2.changedUris;
        }
    }

    final class ObserverInstance extends ContentObserver {
        final ArraySet<JobInstance> mJobs;
        final JobInfo.TriggerContentUri mUri;
        final int mUserId;

        public ObserverInstance(Handler handler, JobInfo.TriggerContentUri triggerContentUri, int i) {
            super(handler);
            this.mJobs = new ArraySet<>();
            this.mUri = triggerContentUri;
            this.mUserId = i;
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (ContentObserverController.DEBUG) {
                Slog.i(ContentObserverController.TAG, "onChange(self=" + z + ") for " + uri + " when mUri=" + this.mUri + " mUserId=" + this.mUserId);
            }
            synchronized (ContentObserverController.this.mLock) {
                int size = this.mJobs.size();
                for (int i = 0; i < size; i++) {
                    JobInstance jobInstanceValueAt = this.mJobs.valueAt(i);
                    if (jobInstanceValueAt.mChangedUris == null) {
                        jobInstanceValueAt.mChangedUris = new ArraySet<>();
                    }
                    if (jobInstanceValueAt.mChangedUris.size() < 50) {
                        jobInstanceValueAt.mChangedUris.add(uri);
                    }
                    if (jobInstanceValueAt.mChangedAuthorities == null) {
                        jobInstanceValueAt.mChangedAuthorities = new ArraySet<>();
                    }
                    jobInstanceValueAt.mChangedAuthorities.add(uri.getAuthority());
                    jobInstanceValueAt.scheduleLocked();
                }
            }
        }
    }

    static final class TriggerRunnable implements Runnable {
        final JobInstance mInstance;

        TriggerRunnable(JobInstance jobInstance) {
            this.mInstance = jobInstance;
        }

        @Override
        public void run() {
            this.mInstance.trigger();
        }
    }

    final class JobInstance {
        ArraySet<String> mChangedAuthorities;
        ArraySet<Uri> mChangedUris;
        final JobStatus mJobStatus;
        boolean mTriggerPending;
        final ArrayList<ObserverInstance> mMyObservers = new ArrayList<>();
        final Runnable mExecuteRunner = new TriggerRunnable(this);
        final Runnable mTimeoutRunner = new TriggerRunnable(this);

        JobInstance(JobStatus jobStatus) {
            this.mJobStatus = jobStatus;
            JobInfo.TriggerContentUri[] triggerContentUris = jobStatus.getJob().getTriggerContentUris();
            int sourceUserId = jobStatus.getSourceUserId();
            ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap = ContentObserverController.this.mObservers.get(sourceUserId);
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
                ContentObserverController.this.mObservers.put(sourceUserId, arrayMap);
            }
            if (triggerContentUris != null) {
                for (JobInfo.TriggerContentUri triggerContentUri : triggerContentUris) {
                    ObserverInstance observerInstance = arrayMap.get(triggerContentUri);
                    if (observerInstance != null) {
                        if (ContentObserverController.DEBUG) {
                            Slog.v(ContentObserverController.TAG, "Reusing existing observer " + observerInstance + " for " + triggerContentUri.getUri() + " andDescendants=" + ((triggerContentUri.getFlags() & 1) != 0));
                        }
                    } else {
                        observerInstance = ContentObserverController.this.new ObserverInstance(ContentObserverController.this.mHandler, triggerContentUri, jobStatus.getSourceUserId());
                        arrayMap.put(triggerContentUri, observerInstance);
                        boolean z = (triggerContentUri.getFlags() & 1) != 0;
                        if (ContentObserverController.DEBUG) {
                            Slog.v(ContentObserverController.TAG, "New observer " + observerInstance + " for " + triggerContentUri.getUri() + " andDescendants=" + z + " sourceUserId=" + sourceUserId);
                        }
                        ContentObserverController.this.mContext.getContentResolver().registerContentObserver(triggerContentUri.getUri(), z, observerInstance, sourceUserId);
                    }
                    observerInstance.mJobs.add(this);
                    this.mMyObservers.add(observerInstance);
                }
            }
        }

        void trigger() {
            boolean z;
            synchronized (ContentObserverController.this.mLock) {
                if (this.mTriggerPending) {
                    z = this.mJobStatus.setContentTriggerConstraintSatisfied(true);
                    unscheduleLocked();
                }
            }
            if (z) {
                ContentObserverController.this.mStateChangedListener.onControllerStateChanged();
            }
        }

        void scheduleLocked() {
            if (!this.mTriggerPending) {
                this.mTriggerPending = true;
                ContentObserverController.this.mHandler.postDelayed(this.mTimeoutRunner, this.mJobStatus.getTriggerContentMaxDelay());
            }
            ContentObserverController.this.mHandler.removeCallbacks(this.mExecuteRunner);
            if (this.mChangedUris.size() >= 40) {
                ContentObserverController.this.mHandler.post(this.mExecuteRunner);
            } else {
                ContentObserverController.this.mHandler.postDelayed(this.mExecuteRunner, this.mJobStatus.getTriggerContentUpdateDelay());
            }
        }

        void unscheduleLocked() {
            if (this.mTriggerPending) {
                ContentObserverController.this.mHandler.removeCallbacks(this.mExecuteRunner);
                ContentObserverController.this.mHandler.removeCallbacks(this.mTimeoutRunner);
                this.mTriggerPending = false;
            }
        }

        void detachLocked() {
            int size = this.mMyObservers.size();
            for (int i = 0; i < size; i++) {
                ObserverInstance observerInstance = this.mMyObservers.get(i);
                observerInstance.mJobs.remove(this);
                if (observerInstance.mJobs.size() == 0) {
                    if (ContentObserverController.DEBUG) {
                        Slog.i(ContentObserverController.TAG, "Unregistering observer " + observerInstance + " for " + observerInstance.mUri.getUri());
                    }
                    ContentObserverController.this.mContext.getContentResolver().unregisterContentObserver(observerInstance);
                    ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap = ContentObserverController.this.mObservers.get(observerInstance.mUserId);
                    if (arrayMap != null) {
                        arrayMap.remove(observerInstance.mUri);
                    }
                }
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter indentingPrintWriter, Predicate<JobStatus> predicate) {
        boolean z;
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus jobStatusValueAt = this.mTrackedTasks.valueAt(i);
            if (predicate.test(jobStatusValueAt)) {
                indentingPrintWriter.print("#");
                jobStatusValueAt.printUniqueId(indentingPrintWriter);
                indentingPrintWriter.print(" from ");
                UserHandle.formatUid(indentingPrintWriter, jobStatusValueAt.getSourceUid());
                indentingPrintWriter.println();
            }
        }
        indentingPrintWriter.println();
        int size = this.mObservers.size();
        if (size > 0) {
            indentingPrintWriter.println("Observers:");
            indentingPrintWriter.increaseIndent();
            for (int i2 = 0; i2 < size; i2++) {
                ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap = this.mObservers.get(this.mObservers.keyAt(i2));
                int size2 = arrayMap.size();
                for (int i3 = 0; i3 < size2; i3++) {
                    ObserverInstance observerInstanceValueAt = arrayMap.valueAt(i3);
                    int size3 = observerInstanceValueAt.mJobs.size();
                    int i4 = 0;
                    while (true) {
                        if (i4 < size3) {
                            if (!predicate.test(observerInstanceValueAt.mJobs.valueAt(i4).mJobStatus)) {
                                i4++;
                            } else {
                                z = true;
                                break;
                            }
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (z) {
                        JobInfo.TriggerContentUri triggerContentUriKeyAt = arrayMap.keyAt(i3);
                        indentingPrintWriter.print(triggerContentUriKeyAt.getUri());
                        indentingPrintWriter.print(" 0x");
                        indentingPrintWriter.print(Integer.toHexString(triggerContentUriKeyAt.getFlags()));
                        indentingPrintWriter.print(" (");
                        indentingPrintWriter.print(System.identityHashCode(observerInstanceValueAt));
                        indentingPrintWriter.println("):");
                        indentingPrintWriter.increaseIndent();
                        indentingPrintWriter.println("Jobs:");
                        indentingPrintWriter.increaseIndent();
                        for (int i5 = 0; i5 < size3; i5++) {
                            JobInstance jobInstanceValueAt = observerInstanceValueAt.mJobs.valueAt(i5);
                            indentingPrintWriter.print("#");
                            jobInstanceValueAt.mJobStatus.printUniqueId(indentingPrintWriter);
                            indentingPrintWriter.print(" from ");
                            UserHandle.formatUid(indentingPrintWriter, jobInstanceValueAt.mJobStatus.getSourceUid());
                            if (jobInstanceValueAt.mChangedAuthorities != null) {
                                indentingPrintWriter.println(":");
                                indentingPrintWriter.increaseIndent();
                                if (jobInstanceValueAt.mTriggerPending) {
                                    indentingPrintWriter.print("Trigger pending: update=");
                                    TimeUtils.formatDuration(jobInstanceValueAt.mJobStatus.getTriggerContentUpdateDelay(), indentingPrintWriter);
                                    indentingPrintWriter.print(", max=");
                                    TimeUtils.formatDuration(jobInstanceValueAt.mJobStatus.getTriggerContentMaxDelay(), indentingPrintWriter);
                                    indentingPrintWriter.println();
                                }
                                indentingPrintWriter.println("Changed Authorities:");
                                for (int i6 = 0; i6 < jobInstanceValueAt.mChangedAuthorities.size(); i6++) {
                                    indentingPrintWriter.println(jobInstanceValueAt.mChangedAuthorities.valueAt(i6));
                                }
                                if (jobInstanceValueAt.mChangedUris != null) {
                                    indentingPrintWriter.println("          Changed URIs:");
                                    for (int i7 = 0; i7 < jobInstanceValueAt.mChangedUris.size(); i7++) {
                                        indentingPrintWriter.println(jobInstanceValueAt.mChangedUris.valueAt(i7));
                                    }
                                }
                                indentingPrintWriter.decreaseIndent();
                            } else {
                                indentingPrintWriter.println();
                            }
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.decreaseIndent();
                    }
                }
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    @Override
    public void dumpControllerStateLocked(ProtoOutputStream protoOutputStream, long j, Predicate<JobStatus> predicate) {
        boolean z;
        long j2;
        long j3;
        ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap;
        int i;
        ObserverInstance observerInstance;
        int i2;
        ContentObserverController contentObserverController = this;
        Predicate<JobStatus> predicate2 = predicate;
        long jStart = protoOutputStream.start(j);
        long jStart2 = protoOutputStream.start(1146756268036L);
        for (int i3 = 0; i3 < contentObserverController.mTrackedTasks.size(); i3++) {
            JobStatus jobStatusValueAt = contentObserverController.mTrackedTasks.valueAt(i3);
            if (predicate2.test(jobStatusValueAt)) {
                long jStart3 = protoOutputStream.start(2246267895809L);
                jobStatusValueAt.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, jobStatusValueAt.getSourceUid());
                protoOutputStream.end(jStart3);
            }
        }
        int size = contentObserverController.mObservers.size();
        int i4 = 0;
        while (i4 < size) {
            int i5 = size;
            long jStart4 = protoOutputStream.start(2246267895810L);
            int iKeyAt = contentObserverController.mObservers.keyAt(i4);
            protoOutputStream.write(1120986464257L, iKeyAt);
            ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap2 = contentObserverController.mObservers.get(iKeyAt);
            int size2 = arrayMap2.size();
            int i6 = 0;
            while (i6 < size2) {
                ObserverInstance observerInstanceValueAt = arrayMap2.valueAt(i6);
                int size3 = observerInstanceValueAt.mJobs.size();
                int i7 = 0;
                while (true) {
                    if (i7 < size3) {
                        if (!predicate2.test(observerInstanceValueAt.mJobs.valueAt(i7).mJobStatus)) {
                            i7++;
                        } else {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    j2 = jStart;
                    j3 = jStart2;
                    arrayMap = arrayMap2;
                    i = size2;
                } else {
                    j2 = jStart;
                    j3 = jStart2;
                    long jStart5 = protoOutputStream.start(2246267895810L);
                    JobInfo.TriggerContentUri triggerContentUriKeyAt = arrayMap2.keyAt(i6);
                    Uri uri = triggerContentUriKeyAt.getUri();
                    if (uri != null) {
                        protoOutputStream.write(1138166333441L, uri.toString());
                    }
                    protoOutputStream.write(1120986464258L, triggerContentUriKeyAt.getFlags());
                    int i8 = 0;
                    while (i8 < size3) {
                        long jStart6 = protoOutputStream.start(2246267895811L);
                        JobInstance jobInstanceValueAt = observerInstanceValueAt.mJobs.valueAt(i8);
                        ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> arrayMap3 = arrayMap2;
                        int i9 = size2;
                        jobInstanceValueAt.mJobStatus.writeToShortProto(protoOutputStream, 1146756268033L);
                        protoOutputStream.write(1120986464258L, jobInstanceValueAt.mJobStatus.getSourceUid());
                        if (jobInstanceValueAt.mChangedAuthorities == null) {
                            protoOutputStream.end(jStart6);
                            observerInstance = observerInstanceValueAt;
                            i2 = size3;
                        } else {
                            if (jobInstanceValueAt.mTriggerPending) {
                                observerInstance = observerInstanceValueAt;
                                i2 = size3;
                                protoOutputStream.write(1112396529667L, jobInstanceValueAt.mJobStatus.getTriggerContentUpdateDelay());
                                protoOutputStream.write(1112396529668L, jobInstanceValueAt.mJobStatus.getTriggerContentMaxDelay());
                            } else {
                                observerInstance = observerInstanceValueAt;
                                i2 = size3;
                            }
                            for (int i10 = 0; i10 < jobInstanceValueAt.mChangedAuthorities.size(); i10++) {
                                protoOutputStream.write(2237677961221L, jobInstanceValueAt.mChangedAuthorities.valueAt(i10));
                            }
                            if (jobInstanceValueAt.mChangedUris != null) {
                                for (int i11 = 0; i11 < jobInstanceValueAt.mChangedUris.size(); i11++) {
                                    Uri uriValueAt = jobInstanceValueAt.mChangedUris.valueAt(i11);
                                    if (uriValueAt != null) {
                                        protoOutputStream.write(2237677961222L, uriValueAt.toString());
                                    }
                                }
                            }
                            protoOutputStream.end(jStart6);
                        }
                        i8++;
                        arrayMap2 = arrayMap3;
                        size2 = i9;
                        observerInstanceValueAt = observerInstance;
                        size3 = i2;
                    }
                    arrayMap = arrayMap2;
                    i = size2;
                    protoOutputStream.end(jStart5);
                }
                i6++;
                jStart = j2;
                jStart2 = j3;
                arrayMap2 = arrayMap;
                size2 = i;
                predicate2 = predicate;
            }
            protoOutputStream.end(jStart4);
            i4++;
            size = i5;
            contentObserverController = this;
            predicate2 = predicate;
        }
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }
}
