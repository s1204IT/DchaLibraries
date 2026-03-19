package com.android.server.job.controllers;

import android.app.job.JobInfo;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ContentObserverController extends StateController {
    private static final boolean DEBUG = false;
    private static final int MAX_URIS_REPORTED = 50;
    private static final String TAG = "JobScheduler.Content";
    private static final int URIS_URGENT_THRESHOLD = 40;
    private static volatile ContentObserverController sController;
    private static final Object sCreationLock = new Object();
    final Handler mHandler;
    SparseArray<ArrayMap<JobInfo.TriggerContentUri, ObserverInstance>> mObservers;
    private final List<JobStatus> mTrackedTasks;

    public static ContentObserverController get(JobSchedulerService taskManagerService) {
        synchronized (sCreationLock) {
            if (sController == null) {
                sController = new ContentObserverController(taskManagerService, taskManagerService.getContext(), taskManagerService.getLock());
            }
        }
        return sController;
    }

    public static ContentObserverController getForTesting(StateChangedListener stateChangedListener, Context context) {
        return new ContentObserverController(stateChangedListener, context, new Object());
    }

    private ContentObserverController(StateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        this.mTrackedTasks = new ArrayList();
        this.mObservers = new SparseArray<>();
        this.mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (taskStatus.hasContentTriggerConstraint()) {
            if (taskStatus.contentObserverJobInstance == null) {
                taskStatus.contentObserverJobInstance = new JobInstance(this, taskStatus);
            }
            this.mTrackedTasks.add(taskStatus);
            boolean havePendingUris = false;
            if (taskStatus.contentObserverJobInstance.mChangedAuthorities != null) {
                havePendingUris = true;
            }
            if (taskStatus.changedAuthorities != null) {
                havePendingUris = true;
                if (taskStatus.contentObserverJobInstance.mChangedAuthorities == null) {
                    taskStatus.contentObserverJobInstance.mChangedAuthorities = new ArraySet<>();
                }
                for (String auth : taskStatus.changedAuthorities) {
                    taskStatus.contentObserverJobInstance.mChangedAuthorities.add(auth);
                }
                if (taskStatus.changedUris != null) {
                    if (taskStatus.contentObserverJobInstance.mChangedUris == null) {
                        taskStatus.contentObserverJobInstance.mChangedUris = new ArraySet<>();
                    }
                    for (Uri uri : taskStatus.changedUris) {
                        taskStatus.contentObserverJobInstance.mChangedUris.add(uri);
                    }
                }
                taskStatus.changedAuthorities = null;
                taskStatus.changedUris = null;
            }
            taskStatus.changedAuthorities = null;
            taskStatus.changedUris = null;
            taskStatus.setContentTriggerConstraintSatisfied(havePendingUris);
        }
        if (lastJob == null || lastJob.contentObserverJobInstance == null) {
            return;
        }
        lastJob.contentObserverJobInstance.detachLocked();
        lastJob.contentObserverJobInstance = null;
    }

    @Override
    public void prepareForExecutionLocked(JobStatus taskStatus) {
        if (!taskStatus.hasContentTriggerConstraint() || taskStatus.contentObserverJobInstance == null) {
            return;
        }
        taskStatus.changedUris = taskStatus.contentObserverJobInstance.mChangedUris;
        taskStatus.changedAuthorities = taskStatus.contentObserverJobInstance.mChangedAuthorities;
        taskStatus.contentObserverJobInstance.mChangedUris = null;
        taskStatus.contentObserverJobInstance.mChangedAuthorities = null;
    }

    @Override
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (!taskStatus.hasContentTriggerConstraint()) {
            return;
        }
        if (taskStatus.contentObserverJobInstance != null) {
            taskStatus.contentObserverJobInstance.unscheduleLocked();
            if (incomingJob != null) {
                if (taskStatus.contentObserverJobInstance != null && taskStatus.contentObserverJobInstance.mChangedAuthorities != null) {
                    if (incomingJob.contentObserverJobInstance == null) {
                        incomingJob.contentObserverJobInstance = new JobInstance(this, incomingJob);
                    }
                    incomingJob.contentObserverJobInstance.mChangedAuthorities = taskStatus.contentObserverJobInstance.mChangedAuthorities;
                    incomingJob.contentObserverJobInstance.mChangedUris = taskStatus.contentObserverJobInstance.mChangedUris;
                    taskStatus.contentObserverJobInstance.mChangedAuthorities = null;
                    taskStatus.contentObserverJobInstance.mChangedUris = null;
                }
            } else {
                taskStatus.contentObserverJobInstance.detachLocked();
                taskStatus.contentObserverJobInstance = null;
            }
        }
        this.mTrackedTasks.remove(taskStatus);
    }

    @Override
    public void rescheduleForFailure(JobStatus newJob, JobStatus failureToReschedule) {
        if (!failureToReschedule.hasContentTriggerConstraint() || !newJob.hasContentTriggerConstraint()) {
            return;
        }
        synchronized (this.mLock) {
            newJob.changedAuthorities = failureToReschedule.changedAuthorities;
            newJob.changedUris = failureToReschedule.changedUris;
        }
    }

    final class ObserverInstance extends ContentObserver {
        final ArraySet<JobInstance> mJobs;
        final JobInfo.TriggerContentUri mUri;
        final int mUserId;

        public ObserverInstance(Handler handler, JobInfo.TriggerContentUri uri, int userId) {
            super(handler);
            this.mJobs = new ArraySet<>();
            this.mUri = uri;
            this.mUserId = userId;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (ContentObserverController.this.mLock) {
                int N = this.mJobs.size();
                for (int i = 0; i < N; i++) {
                    JobInstance inst = this.mJobs.valueAt(i);
                    if (inst.mChangedUris == null) {
                        inst.mChangedUris = new ArraySet<>();
                    }
                    if (inst.mChangedUris.size() < 50) {
                        inst.mChangedUris.add(uri);
                    }
                    if (inst.mChangedAuthorities == null) {
                        inst.mChangedAuthorities = new ArraySet<>();
                    }
                    inst.mChangedAuthorities.add(uri.getAuthority());
                    inst.scheduleLocked();
                }
            }
        }
    }

    static final class TriggerRunnable implements Runnable {
        final JobInstance mInstance;

        TriggerRunnable(JobInstance instance) {
            this.mInstance = instance;
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
        final ContentObserverController this$0;
        final ArrayList<ObserverInstance> mMyObservers = new ArrayList<>();
        final Runnable mExecuteRunner = new TriggerRunnable(this);
        final Runnable mTimeoutRunner = new TriggerRunnable(this);

        JobInstance(ContentObserverController this$0, JobStatus jobStatus) {
            this.this$0 = this$0;
            this.mJobStatus = jobStatus;
            JobInfo.TriggerContentUri[] uris = jobStatus.getJob().getTriggerContentUris();
            int sourceUserId = jobStatus.getSourceUserId();
            ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> observersOfUser = this$0.mObservers.get(sourceUserId);
            if (observersOfUser == null) {
                observersOfUser = new ArrayMap<>();
                this$0.mObservers.put(sourceUserId, observersOfUser);
            }
            if (uris == null) {
                return;
            }
            for (JobInfo.TriggerContentUri uri : uris) {
                ObserverInstance obs = observersOfUser.get(uri);
                if (obs == null) {
                    obs = this$0.new ObserverInstance(this$0.mHandler, uri, jobStatus.getSourceUserId());
                    observersOfUser.put(uri, obs);
                    boolean andDescendants = (uri.getFlags() & 1) != 0;
                    this$0.mContext.getContentResolver().registerContentObserver(uri.getUri(), andDescendants, obs, sourceUserId);
                }
                obs.mJobs.add(this);
                this.mMyObservers.add(obs);
            }
        }

        void trigger() {
            boolean reportChange = false;
            synchronized (this.this$0.mLock) {
                if (this.mTriggerPending) {
                    if (this.mJobStatus.setContentTriggerConstraintSatisfied(true)) {
                        reportChange = true;
                    }
                    unscheduleLocked();
                }
            }
            if (!reportChange) {
                return;
            }
            this.this$0.mStateChangedListener.onControllerStateChanged();
        }

        void scheduleLocked() {
            if (!this.mTriggerPending) {
                this.mTriggerPending = true;
                this.this$0.mHandler.postDelayed(this.mTimeoutRunner, this.mJobStatus.getTriggerContentMaxDelay());
            }
            this.this$0.mHandler.removeCallbacks(this.mExecuteRunner);
            if (this.mChangedUris.size() >= 40) {
                this.this$0.mHandler.post(this.mExecuteRunner);
            } else {
                this.this$0.mHandler.postDelayed(this.mExecuteRunner, this.mJobStatus.getTriggerContentUpdateDelay());
            }
        }

        void unscheduleLocked() {
            if (!this.mTriggerPending) {
                return;
            }
            this.this$0.mHandler.removeCallbacks(this.mExecuteRunner);
            this.this$0.mHandler.removeCallbacks(this.mTimeoutRunner);
            this.mTriggerPending = false;
        }

        void detachLocked() {
            int N = this.mMyObservers.size();
            for (int i = 0; i < N; i++) {
                ObserverInstance obs = this.mMyObservers.get(i);
                obs.mJobs.remove(this);
                if (obs.mJobs.size() == 0) {
                    this.this$0.mContext.getContentResolver().unregisterContentObserver(obs);
                    ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> observerOfUser = this.this$0.mObservers.get(obs.mUserId);
                    if (observerOfUser != null) {
                        observerOfUser.remove(obs.mUri);
                    }
                }
            }
        }
    }

    @Override
    public void dumpControllerStateLocked(PrintWriter pw, int filterUid) {
        pw.println("Content:");
        for (JobStatus js : this.mTrackedTasks) {
            if (js.shouldDump(filterUid)) {
                pw.print("  #");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                pw.println();
            }
        }
        int N = this.mObservers.size();
        if (N <= 0) {
            return;
        }
        pw.println("  Observers:");
        for (int userIdx = 0; userIdx < N; userIdx++) {
            int userId = this.mObservers.keyAt(userIdx);
            ArrayMap<JobInfo.TriggerContentUri, ObserverInstance> observersOfUser = this.mObservers.get(userId);
            int numbOfObserversPerUser = observersOfUser.size();
            for (int observerIdx = 0; observerIdx < numbOfObserversPerUser; observerIdx++) {
                ObserverInstance obs = observersOfUser.valueAt(observerIdx);
                int M = obs.mJobs.size();
                boolean shouldDump = false;
                int j = 0;
                while (true) {
                    if (j >= M) {
                        break;
                    }
                    if (!obs.mJobs.valueAt(j).mJobStatus.shouldDump(filterUid)) {
                        j++;
                    } else {
                        shouldDump = true;
                        break;
                    }
                }
                if (shouldDump) {
                    pw.print("    ");
                    JobInfo.TriggerContentUri trigger = observersOfUser.keyAt(observerIdx);
                    pw.print(trigger.getUri());
                    pw.print(" 0x");
                    pw.print(Integer.toHexString(trigger.getFlags()));
                    pw.print(" (");
                    pw.print(System.identityHashCode(obs));
                    pw.println("):");
                    pw.println("      Jobs:");
                    for (int j2 = 0; j2 < M; j2++) {
                        JobInstance inst = obs.mJobs.valueAt(j2);
                        pw.print("        #");
                        inst.mJobStatus.printUniqueId(pw);
                        pw.print(" from ");
                        UserHandle.formatUid(pw, inst.mJobStatus.getSourceUid());
                        if (inst.mChangedAuthorities != null) {
                            pw.println(":");
                            if (inst.mTriggerPending) {
                                pw.print("          Trigger pending: update=");
                                TimeUtils.formatDuration(inst.mJobStatus.getTriggerContentUpdateDelay(), pw);
                                pw.print(", max=");
                                TimeUtils.formatDuration(inst.mJobStatus.getTriggerContentMaxDelay(), pw);
                                pw.println();
                            }
                            pw.println("          Changed Authorities:");
                            for (int k = 0; k < inst.mChangedAuthorities.size(); k++) {
                                pw.print("          ");
                                pw.println(inst.mChangedAuthorities.valueAt(k));
                            }
                            if (inst.mChangedUris != null) {
                                pw.println("          Changed URIs:");
                                for (int k2 = 0; k2 < inst.mChangedUris.size(); k2++) {
                                    pw.print("          ");
                                    pw.println(inst.mChangedUris.valueAt(k2));
                                }
                            }
                        } else {
                            pw.println();
                        }
                    }
                }
            }
        }
    }
}
