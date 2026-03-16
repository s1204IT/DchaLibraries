package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.ResultInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.TaskGroup;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ActivityStack {
    static final long ACTIVITY_INACTIVE_RESET_TIME = 0;
    static final int DESTROY_ACTIVITIES_MSG = 105;
    static final int DESTROY_TIMEOUT = 10000;
    static final int DESTROY_TIMEOUT_MSG = 102;
    static final int FINISH_AFTER_PAUSE = 1;
    static final int FINISH_AFTER_VISIBLE = 2;
    static final int FINISH_IMMEDIATELY = 0;
    static final int LAUNCH_TICK = 500;
    static final int LAUNCH_TICK_MSG = 103;
    static final int PAUSE_TIMEOUT = 500;
    static final int PAUSE_TIMEOUT_MSG = 101;
    static final int RELEASE_BACKGROUND_RESOURCES_TIMEOUT_MSG = 107;
    static final boolean SCREENSHOT_FORCE_565 = ActivityManager.isLowRamDeviceStatic();
    static final boolean SHOW_APP_STARTING_PREVIEW = true;
    static final long START_WARN_TIME = 5000;
    static final int STOP_TIMEOUT = 10000;
    static final int STOP_TIMEOUT_MSG = 104;
    static final long TRANSLUCENT_CONVERSION_TIMEOUT = 2000;
    static final int TRANSLUCENT_TIMEOUT_MSG = 106;
    final ActivityStackSupervisor.ActivityContainer mActivityContainer;
    boolean mConfigWillChange;
    int mCurrentUser;
    int mDisplayId;
    final Handler mHandler;
    final ActivityManagerService mService;
    final int mStackId;
    final ActivityStackSupervisor mStackSupervisor;
    ArrayList<ActivityStack> mStacks;
    final WindowManagerService mWindowManager;
    private ArrayList<TaskRecord> mTaskHistory = new ArrayList<>();
    final ArrayList<TaskGroup> mValidateAppTokens = new ArrayList<>();
    final ArrayList<ActivityRecord> mLRUActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mNoAnimActivities = new ArrayList<>();
    ActivityRecord mPausingActivity = null;
    ActivityRecord mLastPausedActivity = null;
    ActivityRecord mLastNoHistoryActivity = null;
    ActivityRecord mResumedActivity = null;
    ActivityRecord mLastStartedActivity = null;
    ActivityRecord mTranslucentActivityWaiting = null;
    private ArrayList<ActivityRecord> mUndrawnActivitiesBelowTopTranslucent = new ArrayList<>();
    long mLaunchStartTime = 0;
    long mFullyDrawnStartTime = 0;

    enum ActivityState {
        INITIALIZING,
        RESUMED,
        PAUSING,
        PAUSED,
        STOPPING,
        STOPPED,
        FINISHING,
        DESTROYING,
        DESTROYED
    }

    static class ScheduleDestroyArgs {
        final ProcessRecord mOwner;
        final String mReason;

        ScheduleDestroyArgs(ProcessRecord owner, String reason) {
            this.mOwner = owner;
            this.mReason = reason;
        }
    }

    final class ActivityStackHandler extends Handler {
        ActivityStackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    ActivityRecord r = (ActivityRecord) msg.obj;
                    Slog.w("ActivityManager", "Activity pause timeout for " + r);
                    synchronized (ActivityStack.this.mService) {
                        if (r.app != null) {
                            ActivityStack.this.mService.logAppTooSlow(r.app, r.pauseTime, "pausing " + r);
                        }
                        ActivityStack.this.activityPausedLocked(r.appToken, ActivityStack.SHOW_APP_STARTING_PREVIEW);
                        break;
                    }
                    return;
                case 102:
                    ActivityRecord r2 = (ActivityRecord) msg.obj;
                    Slog.w("ActivityManager", "Activity destroy timeout for " + r2);
                    synchronized (ActivityStack.this.mService) {
                        ActivityStack.this.activityDestroyedLocked(r2 != null ? r2.appToken : null, "destroyTimeout");
                        break;
                    }
                    return;
                case 103:
                    ActivityRecord r3 = (ActivityRecord) msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        if (r3.continueLaunchTickingLocked()) {
                            ActivityStack.this.mService.logAppTooSlow(r3.app, r3.launchTickTime, "launching " + r3);
                        }
                        break;
                    }
                    return;
                case 104:
                    ActivityRecord r4 = (ActivityRecord) msg.obj;
                    Slog.w("ActivityManager", "Activity stop timeout for " + r4);
                    synchronized (ActivityStack.this.mService) {
                        if (r4.isInHistory()) {
                            ActivityStack.this.activityStoppedLocked(r4, null, null, null);
                        }
                        break;
                    }
                    return;
                case 105:
                    ScheduleDestroyArgs args = (ScheduleDestroyArgs) msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        ActivityStack.this.destroyActivitiesLocked(args.mOwner, args.mReason);
                        break;
                    }
                    return;
                case 106:
                    synchronized (ActivityStack.this.mService) {
                        ActivityStack.this.notifyActivityDrawnLocked(null);
                        break;
                    }
                    return;
                case 107:
                    synchronized (ActivityStack.this.mService) {
                        ActivityRecord r5 = ActivityStack.this.getVisibleBehindActivity();
                        Slog.e("ActivityManager", "Timeout waiting for cancelVisibleBehind player=" + r5);
                        if (r5 != null) {
                            ActivityStack.this.mService.killAppAtUsersRequest(r5.app, null);
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    int numActivities() {
        int count = 0;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            count += this.mTaskHistory.get(taskNdx).mActivities.size();
        }
        return count;
    }

    ActivityStack(ActivityStackSupervisor.ActivityContainer activityContainer) {
        this.mActivityContainer = activityContainer;
        this.mStackSupervisor = activityContainer.getOuter();
        this.mService = this.mStackSupervisor.mService;
        this.mHandler = new ActivityStackHandler(this.mService.mHandler.getLooper());
        this.mWindowManager = this.mService.mWindowManager;
        this.mStackId = activityContainer.mStackId;
        this.mCurrentUser = this.mService.mCurrentUserId;
    }

    private boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUser) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        for (int i = 0; i < this.mService.mCurrentProfileIds.length; i++) {
            if (this.mService.mCurrentProfileIds[i] == userId) {
                return SHOW_APP_STARTING_PREVIEW;
            }
        }
        return false;
    }

    boolean okToShowLocked(ActivityRecord r) {
        if (isCurrentProfileLocked(r.userId) || (r.info.flags & 1024) != 0) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    final ActivityRecord topRunningActivityLocked(ActivityRecord notTop) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ActivityRecord r = this.mTaskHistory.get(taskNdx).topRunningActivityLocked(notTop);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    final ActivityRecord topRunningNonDelayedActivityLocked(ActivityRecord notTop) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ArrayList<ActivityRecord> activities = task.mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing && !r.delayedResume && r != notTop && okToShowLocked(r)) {
                    return r;
                }
            }
        }
        return null;
    }

    final ActivityRecord topRunningActivityLocked(IBinder token, int taskId) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.taskId != taskId) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int i = activities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = activities.get(i);
                    if (!r.finishing && token != r.appToken && okToShowLocked(r)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    final ActivityRecord topActivity() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing) {
                    return r;
                }
            }
        }
        return null;
    }

    final TaskRecord topTask() {
        int size = this.mTaskHistory.size();
        if (size > 0) {
            return this.mTaskHistory.get(size - 1);
        }
        return null;
    }

    TaskRecord taskForIdLocked(int id) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.taskId == id) {
                return task;
            }
        }
        return null;
    }

    ActivityRecord isInStackLocked(IBinder token) {
        TaskRecord task;
        ActivityRecord r = ActivityRecord.forToken(token);
        if (r == null || (task = r.task) == null || !task.mActivities.contains(r) || !this.mTaskHistory.contains(task)) {
            return null;
        }
        if (task.stack != this) {
            Slog.w("ActivityManager", "Illegal state! task does not point to stack it is in.");
            return r;
        }
        return r;
    }

    final boolean updateLRUListLocked(ActivityRecord r) {
        boolean hadit = this.mLRUActivities.remove(r);
        this.mLRUActivities.add(r);
        return hadit;
    }

    final boolean isHomeStack() {
        if (this.mStackId == 0) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    final boolean isOnHomeDisplay() {
        if (isAttached() && this.mActivityContainer.mActivityDisplay.mDisplayId == 0) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    final void moveToFront(String reason) {
        if (isAttached()) {
            if (isOnHomeDisplay()) {
                this.mStackSupervisor.moveHomeStack(isHomeStack(), reason);
            }
            this.mStacks.remove(this);
            this.mStacks.add(this);
            TaskRecord task = topTask();
            if (task != null) {
                this.mWindowManager.moveTaskToTop(task.taskId);
            }
        }
    }

    final boolean isAttached() {
        if (this.mStacks != null) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    ActivityRecord findTaskLocked(ActivityRecord target) {
        ActivityRecord r;
        boolean taskIsDocument;
        Uri taskDocumentData;
        Intent intent = target.intent;
        ActivityInfo info = target.info;
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        boolean isDocument = (intent != null ? SHOW_APP_STARTING_PREVIEW : false) & intent.isDocument();
        Uri documentData = isDocument ? intent.getData() : null;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.voiceSession == null && task.userId == userId && (r = task.getTopActivity()) != null && !r.finishing && r.userId == userId && r.launchMode != 3) {
                Intent taskIntent = task.intent;
                Intent affinityIntent = task.affinityIntent;
                if (taskIntent != null && taskIntent.isDocument()) {
                    taskIsDocument = SHOW_APP_STARTING_PREVIEW;
                    taskDocumentData = taskIntent.getData();
                } else if (affinityIntent != null && affinityIntent.isDocument()) {
                    taskIsDocument = SHOW_APP_STARTING_PREVIEW;
                    taskDocumentData = affinityIntent.getData();
                } else {
                    taskIsDocument = false;
                    taskDocumentData = null;
                }
                if (!isDocument && !taskIsDocument && task.rootAffinity != null) {
                    if (task.rootAffinity.equals(target.taskAffinity)) {
                        return r;
                    }
                } else if (taskIntent == null || taskIntent.getComponent() == null || taskIntent.getComponent().compareTo(cls) != 0 || !Objects.equals(documentData, taskDocumentData)) {
                    if (affinityIntent != null && affinityIntent.getComponent() != null && affinityIntent.getComponent().compareTo(cls) == 0 && Objects.equals(documentData, taskDocumentData)) {
                        return r;
                    }
                } else {
                    return r;
                }
            }
        }
        return null;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info) {
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (!isCurrentProfileLocked(task.userId)) {
                return null;
            }
            ArrayList<ActivityRecord> activities = task.mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing && r.intent.getComponent().equals(cls) && r.userId == userId) {
                    return r;
                }
            }
        }
        return null;
    }

    final void switchUserLocked(int userId) {
        if (this.mCurrentUser != userId) {
            this.mCurrentUser = userId;
            int index = this.mTaskHistory.size();
            int i = 0;
            while (i < index) {
                TaskRecord task = this.mTaskHistory.get(i);
                if (isCurrentProfileLocked(task.userId)) {
                    this.mTaskHistory.remove(i);
                    this.mTaskHistory.add(task);
                    index--;
                } else {
                    i++;
                }
            }
        }
    }

    void minimalResumeActivityLocked(ActivityRecord r) {
        r.state = ActivityState.RESUMED;
        r.stopped = false;
        this.mResumedActivity = r;
        r.task.touchActiveTime();
        this.mService.addRecentTaskLocked(r.task);
        completeResumeLocked(r);
        this.mStackSupervisor.checkReadyForSleepLocked();
        setLaunchTime(r);
    }

    private void startLaunchTraces() {
        if (this.mFullyDrawnStartTime != 0) {
            Trace.asyncTraceEnd(64L, "drawing", 0);
        }
        Trace.asyncTraceBegin(64L, "launching", 0);
        Trace.asyncTraceBegin(64L, "drawing", 0);
    }

    private void stopFullyDrawnTraceIfNeeded() {
        if (this.mFullyDrawnStartTime != 0 && this.mLaunchStartTime == 0) {
            Trace.asyncTraceEnd(64L, "drawing", 0);
            this.mFullyDrawnStartTime = 0L;
        }
    }

    void setLaunchTime(ActivityRecord r) {
        if (r.displayStartTime != 0) {
            if (this.mLaunchStartTime == 0) {
                startLaunchTraces();
                long jUptimeMillis = SystemClock.uptimeMillis();
                this.mFullyDrawnStartTime = jUptimeMillis;
                this.mLaunchStartTime = jUptimeMillis;
                return;
            }
            return;
        }
        long jUptimeMillis2 = SystemClock.uptimeMillis();
        r.displayStartTime = jUptimeMillis2;
        r.fullyDrawnStartTime = jUptimeMillis2;
        if (this.mLaunchStartTime == 0) {
            startLaunchTraces();
            long j = r.displayStartTime;
            this.mFullyDrawnStartTime = j;
            this.mLaunchStartTime = j;
        }
    }

    void clearLaunchTime(ActivityRecord r) {
        if (this.mStackSupervisor.mWaitingActivityLaunched.isEmpty()) {
            r.fullyDrawnStartTime = 0L;
            r.displayStartTime = 0L;
        } else {
            this.mStackSupervisor.removeTimeoutsForActivityLocked(r);
            this.mStackSupervisor.scheduleIdleTimeoutLocked(r);
        }
    }

    void awakeFromSleepingLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                activities.get(activityNdx).setSleeping(false);
            }
        }
        if (this.mPausingActivity != null) {
            Slog.d("ActivityManager", "awakeFromSleepingLocked: previously pausing activity didn't pause");
            activityPausedLocked(this.mPausingActivity.appToken, SHOW_APP_STARTING_PREVIEW);
        }
    }

    boolean checkReadyForSleepLocked() {
        if (this.mResumedActivity != null) {
            startPausingLocked(false, SHOW_APP_STARTING_PREVIEW, false, false);
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (this.mPausingActivity == null) {
            return false;
        }
        return SHOW_APP_STARTING_PREVIEW;
    }

    void goToSleep() {
        ensureActivitiesVisibleLocked(null, 0);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.state == ActivityState.STOPPING || r.state == ActivityState.STOPPED) {
                    r.setSleeping(SHOW_APP_STARTING_PREVIEW);
                }
            }
        }
    }

    public final Bitmap screenshotActivities(ActivityRecord who) {
        if (who.noDisplay || isHomeStack()) {
            return null;
        }
        int w = this.mService.mThumbnailWidth;
        int h = this.mService.mThumbnailHeight;
        if (w > 0) {
            return this.mWindowManager.screenshotApplications(who.appToken, 0, w, h, SCREENSHOT_FORCE_565);
        }
        Slog.e("ActivityManager", "Invalid thumbnail dimensions: " + w + "x" + h);
        return null;
    }

    final boolean startPausingLocked(boolean userLeaving, boolean uiSleeping, boolean resuming, boolean dontWait) {
        if (this.mPausingActivity != null) {
            Slog.wtf("ActivityManager", "Going to pause when pause is already pending for " + this.mPausingActivity);
            completePauseLocked(false);
        }
        ActivityRecord prev = this.mResumedActivity;
        if (prev == null) {
            if (!resuming) {
                Slog.wtf("ActivityManager", "Trying to pause when nothing is resumed");
                this.mStackSupervisor.resumeTopActivitiesLocked();
            }
            return false;
        }
        if (this.mActivityContainer.mParentActivity == null) {
            this.mStackSupervisor.pauseChildStacks(prev, userLeaving, uiSleeping, resuming, dontWait);
        }
        this.mResumedActivity = null;
        this.mPausingActivity = prev;
        this.mLastPausedActivity = prev;
        this.mLastNoHistoryActivity = ((prev.intent.getFlags() & 1073741824) == 0 && (prev.info.flags & 128) == 0) ? null : prev;
        prev.state = ActivityState.PAUSING;
        prev.task.touchActiveTime();
        clearLaunchTime(prev);
        ActivityRecord next = this.mStackSupervisor.topRunningActivityLocked();
        if (this.mService.mHasRecents && (next == null || next.noDisplay || next.task != prev.task || uiSleeping)) {
            prev.updateThumbnailLocked(screenshotActivities(prev), null);
        }
        stopFullyDrawnTraceIfNeeded();
        this.mService.updateCpuStats();
        if (prev.app != null && prev.app.thread != null) {
            try {
                EventLog.writeEvent(EventLogTags.AM_PAUSE_ACTIVITY, Integer.valueOf(prev.userId), Integer.valueOf(System.identityHashCode(prev)), prev.shortComponentName);
                this.mService.updateUsageStats(prev, false);
                prev.app.thread.schedulePauseActivity(prev.appToken, prev.finishing, userLeaving, prev.configChangeFlags, dontWait);
            } catch (Exception e) {
                Slog.w("ActivityManager", "Exception thrown during pause", e);
                this.mPausingActivity = null;
                this.mLastPausedActivity = null;
                this.mLastNoHistoryActivity = null;
            }
        } else {
            this.mPausingActivity = null;
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        }
        if (!this.mService.isSleepingOrShuttingDown()) {
            this.mStackSupervisor.acquireLaunchWakelock();
        }
        if (this.mPausingActivity != null) {
            if (!uiSleeping) {
                prev.pauseKeyDispatchingLocked();
            }
            if (dontWait) {
                completePauseLocked(false);
                return false;
            }
            Message msg = this.mHandler.obtainMessage(101);
            msg.obj = prev;
            prev.pauseTime = SystemClock.uptimeMillis();
            this.mHandler.sendMessageDelayed(msg, 500L);
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (!resuming) {
            this.mStackSupervisor.getFocusedStack().resumeTopActivityLocked(null);
        }
        return false;
    }

    final void activityPausedLocked(IBinder token, boolean timeout) {
        ActivityRecord r = isInStackLocked(token);
        if (r != null) {
            this.mHandler.removeMessages(101, r);
            if (this.mPausingActivity == r) {
                completePauseLocked(SHOW_APP_STARTING_PREVIEW);
                return;
            }
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(r.userId);
            objArr[1] = Integer.valueOf(System.identityHashCode(r));
            objArr[2] = r.shortComponentName;
            objArr[3] = this.mPausingActivity != null ? this.mPausingActivity.shortComponentName : "(none)";
            EventLog.writeEvent(EventLogTags.AM_FAILED_TO_PAUSE, objArr);
        }
    }

    final void activityStoppedLocked(ActivityRecord r, Bundle icicle, PersistableBundle persistentState, CharSequence description) {
        if (r.state != ActivityState.STOPPING) {
            Slog.i("ActivityManager", "Activity reported stop, but no longer stopping: " + r);
            this.mHandler.removeMessages(104, r);
            return;
        }
        if (persistentState != null) {
            r.persistentState = persistentState;
            this.mService.notifyTaskPersisterLocked(r.task, false);
        }
        if (icicle != null) {
            r.icicle = icicle;
            r.haveState = SHOW_APP_STARTING_PREVIEW;
            r.launchCount = 0;
            r.updateThumbnailLocked(null, description);
        }
        if (!r.stopped) {
            this.mHandler.removeMessages(104, r);
            r.stopped = SHOW_APP_STARTING_PREVIEW;
            r.state = ActivityState.STOPPED;
            if (this.mActivityContainer.mActivityDisplay.mVisibleBehindActivity == r) {
                this.mStackSupervisor.requestVisibleBehindLocked(r, false);
            }
            if (r.finishing) {
                r.clearOptionsLocked();
            } else if (r.configDestroy) {
                destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, "stop-config");
                this.mStackSupervisor.resumeTopActivitiesLocked();
            } else {
                this.mStackSupervisor.updatePreviousProcessLocked(r);
            }
        }
    }

    private void completePauseLocked(boolean resumeNext) {
        ActivityRecord prev = this.mPausingActivity;
        if (prev != null) {
            prev.state = ActivityState.PAUSED;
            if (prev.finishing) {
                prev = finishCurrentActivityLocked(prev, 2, false);
            } else if (prev.app != null) {
                if (prev.waitingVisible) {
                    prev.waitingVisible = false;
                    this.mStackSupervisor.mWaitingVisibleActivities.remove(prev);
                }
                if (prev.configDestroy) {
                    destroyActivityLocked(prev, SHOW_APP_STARTING_PREVIEW, "pause-config");
                } else if (!hasVisibleBehindActivity()) {
                    this.mStackSupervisor.mStoppingActivities.add(prev);
                    if (this.mStackSupervisor.mStoppingActivities.size() > 3 || (prev.frontOfTask && this.mTaskHistory.size() <= 1)) {
                        this.mStackSupervisor.scheduleIdleLocked();
                    } else {
                        this.mStackSupervisor.checkReadyForSleepLocked();
                    }
                }
            } else {
                prev = null;
            }
            this.mPausingActivity = null;
        }
        if (resumeNext) {
            ActivityStack topStack = this.mStackSupervisor.getFocusedStack();
            if (!this.mService.isSleepingOrShuttingDown()) {
                this.mStackSupervisor.resumeTopActivitiesLocked(topStack, prev, null);
            } else {
                this.mStackSupervisor.checkReadyForSleepLocked();
                ActivityRecord top = topStack.topRunningActivityLocked(null);
                if (top == null || (prev != null && top != prev)) {
                    this.mStackSupervisor.resumeTopActivitiesLocked(topStack, null, null);
                }
            }
        }
        if (prev != null) {
            prev.resumeKeyDispatchingLocked();
            if (prev.app != null && prev.cpuTimeAtResume > 0 && this.mService.mBatteryStatsService.isOnBattery()) {
                long diff = this.mService.mProcessCpuTracker.getCpuTimeForPid(prev.app.pid) - prev.cpuTimeAtResume;
                if (diff > 0) {
                    BatteryStatsImpl bsi = this.mService.mBatteryStatsService.getActiveStatistics();
                    synchronized (bsi) {
                        BatteryStatsImpl.Uid.Proc ps = bsi.getProcessStatsLocked(prev.info.applicationInfo.uid, prev.info.packageName);
                        if (ps != null) {
                            ps.addForegroundTimeLocked(diff);
                        }
                    }
                }
            }
            prev.cpuTimeAtResume = 0L;
        }
        this.mService.notifyTaskStackChangedLocked();
    }

    private void completeResumeLocked(ActivityRecord next) {
        ProcessRecord app;
        next.idle = false;
        next.results = null;
        next.newIntents = null;
        if (next.isHomeActivity() && next.isNotResolverActivity() && (app = next.task.mActivities.get(0).app) != null && app != this.mService.mHomeProcess) {
            this.mService.mHomeProcess = app;
        }
        if (next.nowVisible) {
            this.mStackSupervisor.notifyActivityDrawnForKeyguard();
        }
        this.mStackSupervisor.scheduleIdleTimeoutLocked(next);
        this.mStackSupervisor.reportResumedActivityLocked(next);
        next.resumeKeyDispatchingLocked();
        this.mNoAnimActivities.clear();
        if (next.app != null) {
            next.cpuTimeAtResume = this.mService.mProcessCpuTracker.getCpuTimeForPid(next.app.pid);
        } else {
            next.cpuTimeAtResume = 0L;
        }
        next.returningOptions = null;
        if (this.mActivityContainer.mActivityDisplay.mVisibleBehindActivity == next) {
            this.mActivityContainer.mActivityDisplay.setVisibleBehindActivity(null);
        }
    }

    private void setVisibile(ActivityRecord r, boolean visible) {
        r.visible = visible;
        this.mWindowManager.setAppVisibility(r.appToken, visible);
        ArrayList<ActivityStackSupervisor.ActivityContainer> containers = r.mChildContainers;
        for (int containerNdx = containers.size() - 1; containerNdx >= 0; containerNdx--) {
            ActivityStackSupervisor.ActivityContainer container = containers.get(containerNdx);
            container.setVisible(visible);
        }
    }

    ActivityRecord findNextTranslucentActivity(ActivityRecord r) {
        ActivityStack stack;
        TaskRecord task = r.task;
        if (task == null || (stack = task.stack) == null) {
            return null;
        }
        int taskNdx = stack.mTaskHistory.indexOf(task);
        int activityNdx = task.mActivities.indexOf(r) + 1;
        int numStacks = this.mStacks.size();
        for (int stackNdx = this.mStacks.indexOf(stack); stackNdx < numStacks; stackNdx++) {
            ArrayList<TaskRecord> tasks = this.mStacks.get(stackNdx).mTaskHistory;
            int numTasks = tasks.size();
            while (taskNdx < numTasks) {
                ArrayList<ActivityRecord> activities = tasks.get(taskNdx).mActivities;
                int numActivities = activities.size();
                while (activityNdx < numActivities) {
                    ActivityRecord activity = activities.get(activityNdx);
                    if (!activity.finishing) {
                        if (activity.fullscreen) {
                            activity = null;
                        }
                        return activity;
                    }
                    activityNdx++;
                }
                activityNdx = 0;
                taskNdx++;
            }
            taskNdx = 0;
        }
        return null;
    }

    private boolean isStackVisible() {
        if (!isAttached()) {
            return false;
        }
        if (this.mStackSupervisor.isFrontStack(this)) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        for (int i = this.mStacks.indexOf(this) + 1; i < this.mStacks.size(); i++) {
            ArrayList<TaskRecord> tasks = this.mStacks.get(i).getAllTasks();
            for (int taskNdx = 0; taskNdx < tasks.size(); taskNdx++) {
                TaskRecord task = tasks.get(taskNdx);
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = 0; activityNdx < activities.size(); activityNdx++) {
                    ActivityRecord r = activities.get(activityNdx);
                    if (!r.finishing && r.visible && (r.fullscreen || (!isHomeStack() && r.frontOfTask && task.isOverHomeStack()))) {
                        return false;
                    }
                }
            }
        }
        return SHOW_APP_STARTING_PREVIEW;
    }

    final void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges) {
        ActivityRecord top = topRunningActivityLocked(null);
        if (top != null) {
            if (this.mTranslucentActivityWaiting != top) {
                this.mUndrawnActivitiesBelowTopTranslucent.clear();
                if (this.mTranslucentActivityWaiting != null) {
                    notifyActivityDrawnLocked(null);
                    this.mTranslucentActivityWaiting = null;
                }
                this.mHandler.removeMessages(106);
            }
            boolean aboveTop = SHOW_APP_STARTING_PREVIEW;
            boolean behindFullscreen = !isStackVisible() ? SHOW_APP_STARTING_PREVIEW : false;
            for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
                TaskRecord task = this.mTaskHistory.get(taskNdx);
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = activities.get(activityNdx);
                    if (!r.finishing && (!aboveTop || r == top)) {
                        aboveTop = false;
                        if (!behindFullscreen || r.mLaunchTaskBehind) {
                            if (r != starting) {
                                ensureActivityConfigurationLocked(r, 0);
                            }
                            if (r.app == null || r.app.thread == null) {
                                if (r != starting) {
                                    r.startFreezingScreenLocked(r.app, configChanges);
                                }
                                if (!r.visible || r.mLaunchTaskBehind) {
                                    setVisibile(r, SHOW_APP_STARTING_PREVIEW);
                                }
                                if (r != starting) {
                                    this.mStackSupervisor.startSpecificActivityLocked(r, false, false);
                                }
                            } else if (r.visible) {
                                r.stopFreezingScreenLocked(false);
                                try {
                                    if (r.returningOptions != null) {
                                        r.app.thread.scheduleOnNewActivityOptions(r.appToken, r.returningOptions);
                                    }
                                } catch (RemoteException e) {
                                }
                            } else {
                                r.visible = SHOW_APP_STARTING_PREVIEW;
                                if (r.state != ActivityState.RESUMED && r != starting) {
                                    try {
                                        if (this.mTranslucentActivityWaiting != null) {
                                            r.updateOptionsLocked(r.returningOptions);
                                            this.mUndrawnActivitiesBelowTopTranslucent.add(r);
                                        }
                                        setVisibile(r, SHOW_APP_STARTING_PREVIEW);
                                        r.sleeping = false;
                                        r.app.pendingUiClean = SHOW_APP_STARTING_PREVIEW;
                                        r.app.thread.scheduleWindowVisibility(r.appToken, SHOW_APP_STARTING_PREVIEW);
                                        r.stopFreezingScreenLocked(false);
                                    } catch (Exception e2) {
                                        Slog.w("ActivityManager", "Exception thrown making visibile: " + r.intent.getComponent(), e2);
                                    }
                                }
                            }
                            configChanges |= r.configChangeFlags;
                            if (r.fullscreen) {
                                behindFullscreen = SHOW_APP_STARTING_PREVIEW;
                            } else if (!isHomeStack() && r.frontOfTask && task.isOverHomeStack()) {
                                behindFullscreen = SHOW_APP_STARTING_PREVIEW;
                            }
                        } else if (r.visible) {
                            try {
                                setVisibile(r, false);
                                switch (r.state) {
                                    case STOPPING:
                                    case STOPPED:
                                        if (r.app != null && r.app.thread != null) {
                                            r.app.thread.scheduleWindowVisibility(r.appToken, false);
                                        }
                                        break;
                                    case INITIALIZING:
                                    case RESUMED:
                                    case PAUSING:
                                    case PAUSED:
                                        if (getVisibleBehindActivity() == r) {
                                            releaseBackgroundResources();
                                        } else {
                                            if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
                                                this.mStackSupervisor.mStoppingActivities.add(r);
                                            }
                                            this.mStackSupervisor.scheduleIdleLocked();
                                        }
                                        break;
                                }
                            } catch (Exception e3) {
                                Slog.w("ActivityManager", "Exception thrown making hidden: " + r.intent.getComponent(), e3);
                            }
                        }
                    }
                }
            }
            if (this.mTranslucentActivityWaiting != null && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty()) {
                notifyActivityDrawnLocked(null);
            }
        }
    }

    void convertToTranslucent(ActivityRecord r) {
        this.mTranslucentActivityWaiting = r;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.sendEmptyMessageDelayed(106, TRANSLUCENT_CONVERSION_TIMEOUT);
    }

    void notifyActivityDrawnLocked(ActivityRecord r) {
        this.mActivityContainer.setDrawn();
        if (r == null || (this.mUndrawnActivitiesBelowTopTranslucent.remove(r) && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty())) {
            ActivityRecord waitingActivity = this.mTranslucentActivityWaiting;
            this.mTranslucentActivityWaiting = null;
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            this.mHandler.removeMessages(106);
            if (waitingActivity != null) {
                this.mWindowManager.setWindowOpaque(waitingActivity.appToken, false);
                if (waitingActivity.app != null && waitingActivity.app.thread != null) {
                    try {
                        waitingActivity.app.thread.scheduleTranslucentConversionComplete(waitingActivity.appToken, r != null ? SHOW_APP_STARTING_PREVIEW : false);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    void cancelInitializingActivities() {
        ActivityRecord topActivity = topRunningActivityLocked(null);
        boolean aboveTop = SHOW_APP_STARTING_PREVIEW;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (aboveTop) {
                    if (r == topActivity) {
                        aboveTop = false;
                    }
                } else if (r.state == ActivityState.INITIALIZING && r.mStartingWindowShown) {
                    r.mStartingWindowShown = false;
                    this.mWindowManager.removeAppStartingWindow(r.appToken);
                }
            }
        }
    }

    final boolean resumeTopActivityLocked(ActivityRecord prev) {
        return resumeTopActivityLocked(prev, null);
    }

    final boolean resumeTopActivityLocked(ActivityRecord prev, Bundle options) {
        if (this.mStackSupervisor.inResumeTopActivity) {
            return false;
        }
        try {
            this.mStackSupervisor.inResumeTopActivity = SHOW_APP_STARTING_PREVIEW;
            if (this.mService.mLockScreenShown == 1) {
                this.mService.mLockScreenShown = 0;
                this.mService.updateSleepIfNeededLocked();
            }
            boolean result = resumeTopActivityInnerLocked(prev, options);
            return result;
        } finally {
            this.mStackSupervisor.inResumeTopActivity = false;
        }
    }

    final boolean resumeTopActivityInnerLocked(ActivityRecord prev, Bundle options) {
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        ActivityRecord parent = this.mActivityContainer.mParentActivity;
        if ((parent != null && parent.state != ActivityState.RESUMED) || !this.mActivityContainer.isAttachedLocked()) {
            return false;
        }
        cancelInitializingActivities();
        ActivityRecord next = topRunningActivityLocked(null);
        boolean userLeaving = this.mStackSupervisor.mUserLeaving;
        this.mStackSupervisor.mUserLeaving = false;
        TaskRecord prevTask = prev != null ? prev.task : null;
        if (next == null) {
            ActivityOptions.abort(options);
            int returnTaskType = (prevTask == null || !prevTask.isOverHomeStack()) ? 1 : prevTask.getTaskToReturnTo();
            if (isOnHomeDisplay() && this.mStackSupervisor.resumeHomeStackTask(returnTaskType, prev, "noMoreActivities")) {
                return SHOW_APP_STARTING_PREVIEW;
            }
            return false;
        }
        next.delayedResume = false;
        if (this.mResumedActivity == next && next.state == ActivityState.RESUMED && this.mStackSupervisor.allResumedActivitiesComplete()) {
            this.mWindowManager.executeAppTransition();
            this.mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            return false;
        }
        TaskRecord nextTask = next.task;
        if (prevTask != null && prevTask.stack == this && prevTask.isOverHomeStack() && prev.finishing && prev.frontOfTask) {
            if (prevTask == nextTask) {
                prevTask.setFrontOfTask();
            } else if (prevTask != topTask()) {
                int taskNdx = this.mTaskHistory.indexOf(prevTask) + 1;
                this.mTaskHistory.get(taskNdx).setTaskToReturnTo(1);
            } else {
                if (!isOnHomeDisplay()) {
                    return false;
                }
                if (!isHomeStack()) {
                    int returnTaskType2 = (prevTask == null || !prevTask.isOverHomeStack()) ? 1 : prevTask.getTaskToReturnTo();
                    return this.mStackSupervisor.resumeHomeStackTask(returnTaskType2, prev, "prevFinished");
                }
            }
        }
        if (this.mService.isSleepingOrShuttingDown() && this.mLastPausedActivity == next && this.mStackSupervisor.allPausedActivitiesComplete()) {
            this.mWindowManager.executeAppTransition();
            this.mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            return false;
        }
        if (this.mService.mStartedUsers.get(next.userId) == null) {
            Slog.w("ActivityManager", "Skipping resume of top activity " + next + ": user " + next.userId + " is stopped");
            return false;
        }
        this.mStackSupervisor.mStoppingActivities.remove(next);
        this.mStackSupervisor.mGoingToSleepActivities.remove(next);
        next.sleeping = false;
        this.mStackSupervisor.mWaitingVisibleActivities.remove(next);
        if (!this.mStackSupervisor.allPausedActivitiesComplete()) {
            return false;
        }
        boolean dontWaitForPause = (next.info.flags & 16384) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
        boolean pausing = this.mStackSupervisor.pauseBackStacks(userLeaving, SHOW_APP_STARTING_PREVIEW, dontWaitForPause);
        if (this.mResumedActivity != null) {
            pausing |= startPausingLocked(userLeaving, false, SHOW_APP_STARTING_PREVIEW, dontWaitForPause);
        }
        if (pausing) {
            if (next.app != null && next.app.thread != null) {
                this.mService.updateLruProcessLocked(next.app, SHOW_APP_STARTING_PREVIEW, null);
            }
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (this.mService.isSleeping() && this.mLastNoHistoryActivity != null && !this.mLastNoHistoryActivity.finishing) {
            requestFinishActivityLocked(this.mLastNoHistoryActivity.appToken, 0, null, "no-history", false);
            this.mLastNoHistoryActivity = null;
        }
        if (prev != null && prev != next) {
            if (!prev.waitingVisible && next != null && !next.nowVisible) {
                prev.waitingVisible = SHOW_APP_STARTING_PREVIEW;
                this.mStackSupervisor.mWaitingVisibleActivities.add(prev);
            } else if (prev.finishing) {
                this.mWindowManager.setAppVisibility(prev.appToken, false);
            }
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(next.packageName, false, next.userId);
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w("ActivityManager", "Failed trying to unstop package " + next.packageName + ": " + e2);
        }
        boolean anim = SHOW_APP_STARTING_PREVIEW;
        if (prev != null) {
            if (prev.finishing) {
                if (this.mNoAnimActivities.contains(prev)) {
                    anim = false;
                    this.mWindowManager.prepareAppTransition(0, false);
                } else {
                    this.mWindowManager.prepareAppTransition(prev.task == next.task ? 7 : 9, false);
                }
                this.mWindowManager.setAppWillBeHidden(prev.appToken);
                this.mWindowManager.setAppVisibility(prev.appToken, false);
            } else if (this.mNoAnimActivities.contains(next)) {
                anim = false;
                this.mWindowManager.prepareAppTransition(0, false);
            } else {
                this.mWindowManager.prepareAppTransition(prev.task == next.task ? 6 : next.mLaunchTaskBehind ? 16 : 8, false);
            }
        } else if (this.mNoAnimActivities.contains(next)) {
            anim = false;
            this.mWindowManager.prepareAppTransition(0, false);
        } else {
            this.mWindowManager.prepareAppTransition(6, false);
        }
        Bundle resumeAnimOptions = null;
        if (anim) {
            ActivityOptions opts = next.getOptionsForTargetActivityLocked();
            if (opts != null) {
                resumeAnimOptions = opts.toBundle();
            }
            next.applyOptionsLocked();
        } else {
            next.clearOptionsLocked();
        }
        ActivityStack lastStack = this.mStackSupervisor.getLastStack();
        if (next.app != null && next.app.thread != null) {
            this.mWindowManager.setAppVisibility(next.appToken, SHOW_APP_STARTING_PREVIEW);
            next.startLaunchTickingLocked();
            ActivityRecord lastResumedActivity = lastStack == null ? null : lastStack.mResumedActivity;
            ActivityState lastState = next.state;
            this.mService.updateCpuStats();
            next.state = ActivityState.RESUMED;
            this.mResumedActivity = next;
            next.task.touchActiveTime();
            this.mService.addRecentTaskLocked(next.task);
            this.mService.updateLruProcessLocked(next.app, SHOW_APP_STARTING_PREVIEW, null);
            updateLRUListLocked(next);
            this.mService.updateOomAdjLocked();
            boolean notUpdated = SHOW_APP_STARTING_PREVIEW;
            if (this.mStackSupervisor.isFrontStack(this)) {
                Configuration config = this.mWindowManager.updateOrientationFromAppTokens(this.mService.mConfiguration, next.mayFreezeScreenLocked(next.app) ? next.appToken : null);
                if (config != null) {
                    next.frozenBeforeDestroy = SHOW_APP_STARTING_PREVIEW;
                }
                notUpdated = !this.mService.updateConfigurationLocked(config, next, false, false) ? SHOW_APP_STARTING_PREVIEW : false;
            }
            if (notUpdated) {
                ActivityRecord nextNext = topRunningActivityLocked(null);
                if (nextNext != next) {
                    this.mStackSupervisor.scheduleResumeTopActivities();
                }
                if (this.mStackSupervisor.reportResumedActivityLocked(next)) {
                    this.mNoAnimActivities.clear();
                    return SHOW_APP_STARTING_PREVIEW;
                }
                return false;
            }
            try {
                ArrayList<ResultInfo> a = next.results;
                if (a != null) {
                    int N = a.size();
                    if (!next.finishing && N > 0) {
                        next.app.thread.scheduleSendResult(next.appToken, a);
                    }
                }
                if (next.newIntents != null) {
                    next.app.thread.scheduleNewIntent(next.newIntents, next.appToken);
                }
                EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, Integer.valueOf(next.userId), Integer.valueOf(System.identityHashCode(next)), Integer.valueOf(next.task.taskId), next.shortComponentName);
                next.sleeping = false;
                this.mService.showAskCompatModeDialogLocked(next);
                next.app.pendingUiClean = SHOW_APP_STARTING_PREVIEW;
                next.app.forceProcessStateUpTo(2);
                next.clearOptionsLocked();
                next.app.thread.scheduleResumeActivity(next.appToken, next.app.repProcState, this.mService.isNextTransitionForward(), resumeAnimOptions);
                this.mStackSupervisor.checkReadyForSleepLocked();
                try {
                    next.visible = SHOW_APP_STARTING_PREVIEW;
                    completeResumeLocked(next);
                    next.stopped = false;
                } catch (Exception e3) {
                    Slog.w("ActivityManager", "Exception thrown during resume of " + next, e3);
                    requestFinishActivityLocked(next.appToken, 0, null, "resume-exception", SHOW_APP_STARTING_PREVIEW);
                    return SHOW_APP_STARTING_PREVIEW;
                }
            } catch (Exception e4) {
                next.state = lastState;
                if (lastStack != null) {
                    lastStack.mResumedActivity = lastResumedActivity;
                }
                Slog.i("ActivityManager", "Restarting because process died: " + next);
                if (!next.hasBeenLaunched) {
                    next.hasBeenLaunched = SHOW_APP_STARTING_PREVIEW;
                } else if (lastStack != null && this.mStackSupervisor.isFrontStack(lastStack)) {
                    this.mWindowManager.setAppStartingWindow(next.appToken, next.packageName, next.theme, this.mService.compatibilityInfoForPackageLocked(next.info.applicationInfo), next.nonLocalizedLabel, next.labelRes, next.icon, next.logo, next.windowFlags, null, SHOW_APP_STARTING_PREVIEW);
                }
                this.mStackSupervisor.startSpecificActivityLocked(next, SHOW_APP_STARTING_PREVIEW, false);
                return SHOW_APP_STARTING_PREVIEW;
            }
        } else {
            if (!next.hasBeenLaunched) {
                next.hasBeenLaunched = SHOW_APP_STARTING_PREVIEW;
            } else {
                this.mWindowManager.setAppStartingWindow(next.appToken, next.packageName, next.theme, this.mService.compatibilityInfoForPackageLocked(next.info.applicationInfo), next.nonLocalizedLabel, next.labelRes, next.icon, next.logo, next.windowFlags, null, SHOW_APP_STARTING_PREVIEW);
            }
            this.mStackSupervisor.startSpecificActivityLocked(next, SHOW_APP_STARTING_PREVIEW, SHOW_APP_STARTING_PREVIEW);
        }
        return SHOW_APP_STARTING_PREVIEW;
    }

    private void insertTaskAtTop(TaskRecord task) {
        int i = 0;
        if (isOnHomeDisplay()) {
            ActivityStack lastStack = this.mStackSupervisor.getLastStack();
            boolean fromHome = lastStack.isHomeStack();
            if (!isHomeStack() && (fromHome || topTask() != task)) {
                if (fromHome) {
                    i = lastStack.topTask() == null ? 1 : lastStack.topTask().taskType;
                }
                task.setTaskToReturnTo(i);
            }
        } else {
            task.setTaskToReturnTo(0);
        }
        this.mTaskHistory.remove(task);
        int taskNdx = this.mTaskHistory.size();
        if (!isCurrentProfileLocked(task.userId)) {
            do {
                taskNdx--;
                if (taskNdx < 0) {
                    break;
                }
            } while (isCurrentProfileLocked(this.mTaskHistory.get(taskNdx).userId));
            taskNdx++;
        }
        this.mTaskHistory.add(taskNdx, task);
        updateTaskMovement(task, SHOW_APP_STARTING_PREVIEW);
    }

    final void startActivityLocked(ActivityRecord r, boolean newTask, boolean doResume, boolean keepCurTransition, Bundle options) {
        int i;
        TaskRecord rTask = r.task;
        int taskId = rTask.taskId;
        if (!r.mLaunchTaskBehind && (taskForIdLocked(taskId) == null || newTask)) {
            insertTaskAtTop(rTask);
            this.mWindowManager.moveTaskToTop(taskId);
        }
        TaskRecord task = null;
        if (!newTask) {
            boolean startIt = SHOW_APP_STARTING_PREVIEW;
            int taskNdx = this.mTaskHistory.size() - 1;
            while (true) {
                if (taskNdx < 0) {
                    break;
                }
                task = this.mTaskHistory.get(taskNdx);
                if (task.getTopActivity() != null) {
                    if (task == r.task) {
                        if (!startIt) {
                            task.addActivityToTop(r);
                            r.putInHistory();
                            this.mWindowManager.addAppToken(task.mActivities.indexOf(r), r.appToken, r.task.taskId, this.mStackId, r.info.screenOrientation, r.fullscreen, (r.info.flags & 1024) != 0 ? SHOW_APP_STARTING_PREVIEW : false, r.userId, r.info.configChanges, task.voiceSession != null ? SHOW_APP_STARTING_PREVIEW : false, r.mLaunchTaskBehind);
                            ActivityOptions.abort(options);
                            return;
                        }
                    } else if (task.numFullscreen > 0) {
                        startIt = false;
                    }
                }
                taskNdx--;
            }
        }
        if (task == r.task && this.mTaskHistory.indexOf(task) != this.mTaskHistory.size() - 1) {
            this.mStackSupervisor.mUserLeaving = false;
        }
        TaskRecord task2 = r.task;
        task2.addActivityToTop(r);
        task2.setFrontOfTask();
        r.putInHistory();
        if (!isHomeStack() || numActivities() > 0) {
            boolean showStartingIcon = newTask;
            ProcessRecord proc = r.app;
            if (proc == null) {
                proc = (ProcessRecord) this.mService.mProcessNames.get(r.processName, r.info.applicationInfo.uid);
            }
            if (proc == null || proc.thread == null) {
                showStartingIcon = SHOW_APP_STARTING_PREVIEW;
            }
            if ((r.intent.getFlags() & 65536) != 0) {
                this.mWindowManager.prepareAppTransition(0, keepCurTransition);
                this.mNoAnimActivities.add(r);
            } else {
                WindowManagerService windowManagerService = this.mWindowManager;
                if (newTask) {
                    i = r.mLaunchTaskBehind ? 16 : 8;
                } else {
                    i = 6;
                }
                windowManagerService.prepareAppTransition(i, keepCurTransition);
                this.mNoAnimActivities.remove(r);
            }
            this.mWindowManager.addAppToken(task2.mActivities.indexOf(r), r.appToken, r.task.taskId, this.mStackId, r.info.screenOrientation, r.fullscreen, (r.info.flags & 1024) != 0 ? SHOW_APP_STARTING_PREVIEW : false, r.userId, r.info.configChanges, task2.voiceSession != null ? SHOW_APP_STARTING_PREVIEW : false, r.mLaunchTaskBehind);
            boolean doShow = SHOW_APP_STARTING_PREVIEW;
            if (newTask) {
                if ((r.intent.getFlags() & 2097152) != 0) {
                    resetTaskIfNeededLocked(r, r);
                    doShow = topRunningNonDelayedActivityLocked(null) == r ? SHOW_APP_STARTING_PREVIEW : false;
                }
            } else if (options != null && new ActivityOptions(options).getAnimationType() == 5) {
                doShow = false;
            }
            if (r.mLaunchTaskBehind) {
                this.mWindowManager.setAppVisibility(r.appToken, SHOW_APP_STARTING_PREVIEW);
                ensureActivitiesVisibleLocked(null, 0);
            } else if (doShow) {
                ActivityRecord prev = this.mResumedActivity;
                if (prev != null && (prev.task != r.task || prev.nowVisible)) {
                    prev = null;
                }
                this.mWindowManager.setAppStartingWindow(r.appToken, r.packageName, r.theme, this.mService.compatibilityInfoForPackageLocked(r.info.applicationInfo), r.nonLocalizedLabel, r.labelRes, r.icon, r.logo, r.windowFlags, prev != null ? prev.appToken : null, showStartingIcon);
                r.mStartingWindowShown = SHOW_APP_STARTING_PREVIEW;
            }
        } else {
            this.mWindowManager.addAppToken(task2.mActivities.indexOf(r), r.appToken, r.task.taskId, this.mStackId, r.info.screenOrientation, r.fullscreen, (r.info.flags & 1024) != 0 ? SHOW_APP_STARTING_PREVIEW : false, r.userId, r.info.configChanges, task2.voiceSession != null ? SHOW_APP_STARTING_PREVIEW : false, r.mLaunchTaskBehind);
            ActivityOptions.abort(options);
            options = null;
        }
        if (doResume) {
            this.mStackSupervisor.resumeTopActivitiesLocked(this, r, options);
        }
    }

    final void validateAppTokensLocked() {
        this.mValidateAppTokens.clear();
        this.mValidateAppTokens.ensureCapacity(numActivities());
        int numTasks = this.mTaskHistory.size();
        for (int taskNdx = 0; taskNdx < numTasks; taskNdx++) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ArrayList<ActivityRecord> activities = task.mActivities;
            if (!activities.isEmpty()) {
                TaskGroup group = new TaskGroup();
                group.taskId = task.taskId;
                this.mValidateAppTokens.add(group);
                int numActivities = activities.size();
                for (int activityNdx = 0; activityNdx < numActivities; activityNdx++) {
                    ActivityRecord r = activities.get(activityNdx);
                    group.tokens.add(r.appToken);
                }
            }
        }
        this.mWindowManager.validateAppTokens(this.mStackId, this.mValidateAppTokens);
    }

    final ActivityOptions resetTargetTaskIfNeededLocked(TaskRecord task, boolean forceReset) {
        int end;
        TaskRecord targetTask;
        ActivityOptions topOptions = null;
        int replyChainEnd = -1;
        boolean canMoveOptions = SHOW_APP_STARTING_PREVIEW;
        ArrayList<ActivityRecord> activities = task.mActivities;
        int numActivities = activities.size();
        int rootActivityNdx = task.findEffectiveRootIndex();
        for (int i = numActivities - 1; i > rootActivityNdx; i--) {
            ActivityRecord target = activities.get(i);
            if (target.frontOfTask) {
                break;
            }
            int flags = target.info.flags;
            boolean finishOnTaskLaunch = (flags & 2) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
            boolean allowTaskReparenting = (flags & 64) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
            boolean clearWhenTaskReset = (target.intent.getFlags() & 524288) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
            if (!finishOnTaskLaunch && !clearWhenTaskReset && target.resultTo != null) {
                if (replyChainEnd < 0) {
                    replyChainEnd = i;
                }
            } else if (!finishOnTaskLaunch && !clearWhenTaskReset && allowTaskReparenting && target.taskAffinity != null && !target.taskAffinity.equals(task.affinity)) {
                ActivityRecord bottom = (this.mTaskHistory.isEmpty() || this.mTaskHistory.get(0).mActivities.isEmpty()) ? null : this.mTaskHistory.get(0).mActivities.get(0);
                if (bottom != null && target.taskAffinity != null && target.taskAffinity.equals(bottom.task.affinity)) {
                    targetTask = bottom.task;
                } else {
                    targetTask = createTaskRecord(this.mStackSupervisor.getNextTaskId(), target.info, null, null, null, false);
                    targetTask.affinityIntent = target.intent;
                }
                int targetTaskId = targetTask.taskId;
                this.mWindowManager.setAppGroupId(target.appToken, targetTaskId);
                boolean noOptions = canMoveOptions;
                int start = replyChainEnd < 0 ? i : replyChainEnd;
                for (int srcPos = start; srcPos >= i; srcPos--) {
                    ActivityRecord p = activities.get(srcPos);
                    if (!p.finishing) {
                        canMoveOptions = false;
                        if (noOptions && topOptions == null && (topOptions = p.takeOptionsLocked()) != null) {
                            noOptions = false;
                        }
                        p.setTask(targetTask, null);
                        targetTask.addActivityAtBottom(p);
                        this.mWindowManager.setAppGroupId(p.appToken, targetTaskId);
                    }
                }
                this.mWindowManager.moveTaskToBottom(targetTaskId);
                replyChainEnd = -1;
            } else if (forceReset || finishOnTaskLaunch || clearWhenTaskReset) {
                if (clearWhenTaskReset) {
                    end = numActivities - 1;
                } else if (replyChainEnd < 0) {
                    end = i;
                } else {
                    end = replyChainEnd;
                }
                boolean noOptions2 = canMoveOptions;
                int srcPos2 = i;
                while (srcPos2 <= end) {
                    ActivityRecord p2 = activities.get(srcPos2);
                    if (!p2.finishing) {
                        canMoveOptions = false;
                        if (noOptions2 && topOptions == null && (topOptions = p2.takeOptionsLocked()) != null) {
                            noOptions2 = false;
                        }
                        if (finishActivityLocked(p2, 0, null, "reset", false)) {
                            end--;
                            srcPos2--;
                        }
                    }
                    srcPos2++;
                }
                replyChainEnd = -1;
            } else {
                replyChainEnd = -1;
            }
        }
        return topOptions;
    }

    private int resetAffinityTaskIfNeededLocked(TaskRecord affinityTask, TaskRecord task, boolean topTaskIsHigher, boolean forceReset, int taskInsertionPoint) {
        ArrayList<ActivityRecord> taskActivities;
        int targetNdx;
        int replyChainEnd = -1;
        int taskId = task.taskId;
        String taskAffinity = task.affinity;
        ArrayList<ActivityRecord> activities = affinityTask.mActivities;
        int numActivities = activities.size();
        int rootActivityNdx = affinityTask.findEffectiveRootIndex();
        for (int i = numActivities - 1; i > rootActivityNdx; i--) {
            ActivityRecord target = activities.get(i);
            if (target.frontOfTask) {
                break;
            }
            int flags = target.info.flags;
            boolean finishOnTaskLaunch = (flags & 2) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
            boolean allowTaskReparenting = (flags & 64) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
            if (target.resultTo != null) {
                if (replyChainEnd < 0) {
                    replyChainEnd = i;
                }
            } else if (topTaskIsHigher && allowTaskReparenting && taskAffinity != null && taskAffinity.equals(target.taskAffinity)) {
                if (forceReset || finishOnTaskLaunch) {
                    int start = replyChainEnd >= 0 ? replyChainEnd : i;
                    for (int srcPos = start; srcPos >= i; srcPos--) {
                        ActivityRecord p = activities.get(srcPos);
                        if (!p.finishing) {
                            finishActivityLocked(p, 0, null, "reset", false);
                        }
                    }
                } else {
                    if (taskInsertionPoint < 0) {
                        taskInsertionPoint = task.mActivities.size();
                    }
                    int start2 = replyChainEnd >= 0 ? replyChainEnd : i;
                    for (int srcPos2 = start2; srcPos2 >= i; srcPos2--) {
                        ActivityRecord p2 = activities.get(srcPos2);
                        p2.setTask(task, null);
                        task.addActivityAtIndex(taskInsertionPoint, p2);
                        this.mWindowManager.setAppGroupId(p2.appToken, taskId);
                    }
                    this.mWindowManager.moveTaskToTop(taskId);
                    if (target.info.launchMode == 1 && (targetNdx = (taskActivities = task.mActivities).indexOf(target)) > 0) {
                        ActivityRecord p3 = taskActivities.get(targetNdx - 1);
                        if (p3.intent.getComponent().equals(target.intent.getComponent())) {
                            finishActivityLocked(p3, 0, null, "replace", false);
                        }
                    }
                }
                replyChainEnd = -1;
            }
        }
        return taskInsertionPoint;
    }

    final ActivityRecord resetTaskIfNeededLocked(ActivityRecord taskTop, ActivityRecord newActivity) {
        ActivityRecord taskTop2;
        boolean forceReset = (newActivity.info.flags & 4) != 0 ? SHOW_APP_STARTING_PREVIEW : false;
        TaskRecord task = taskTop.task;
        boolean taskFound = false;
        ActivityOptions topOptions = null;
        int reparentInsertionPoint = -1;
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            TaskRecord targetTask = this.mTaskHistory.get(i);
            if (targetTask == task) {
                topOptions = resetTargetTaskIfNeededLocked(task, forceReset);
                taskFound = SHOW_APP_STARTING_PREVIEW;
            } else {
                reparentInsertionPoint = resetAffinityTaskIfNeededLocked(targetTask, task, taskFound, forceReset, reparentInsertionPoint);
            }
        }
        int taskNdx = this.mTaskHistory.indexOf(task);
        while (true) {
            int taskNdx2 = taskNdx - 1;
            taskTop2 = this.mTaskHistory.get(taskNdx).getTopActivity();
            if (taskTop2 != null || taskNdx2 < 0) {
                break;
            }
            taskNdx = taskNdx2;
        }
        if (topOptions != null) {
            if (taskTop2 != null) {
                taskTop2.updateOptionsLocked(topOptions);
            } else {
                topOptions.abort();
            }
        }
        return taskTop2;
    }

    void sendActivityResultLocked(int callingUid, ActivityRecord r, String resultWho, int requestCode, int resultCode, Intent data) {
        if (callingUid > 0) {
            this.mService.grantUriPermissionFromIntentLocked(callingUid, r.packageName, data, r.getUriPermissionsLocked(), r.userId);
        }
        if (this.mResumedActivity == r && r.app != null && r.app.thread != null) {
            try {
                ArrayList<ResultInfo> list = new ArrayList<>();
                list.add(new ResultInfo(resultWho, requestCode, resultCode, data));
                r.app.thread.scheduleSendResult(r.appToken, list);
                return;
            } catch (Exception e) {
                Slog.w("ActivityManager", "Exception thrown sending result to " + r, e);
            }
        }
        r.addResultLocked(null, resultWho, requestCode, resultCode, data);
    }

    private void adjustFocusedActivityLocked(ActivityRecord r, String reason) {
        if (this.mStackSupervisor.isFrontStack(this) && this.mService.mFocusedActivity == r) {
            ActivityRecord next = topRunningActivityLocked(null);
            if (next != r) {
                TaskRecord task = r.task;
                if (r.frontOfTask && task == topTask() && task.isOverHomeStack()) {
                    this.mStackSupervisor.moveHomeStackTaskToTop(task.getTaskToReturnTo(), reason + " adjustFocus");
                }
            }
            ActivityRecord top = this.mStackSupervisor.topRunningActivityLocked();
            if (top != null) {
                this.mService.setFocusedActivityLocked(top, reason + " adjustTopFocus");
            }
        }
    }

    final void stopActivityLocked(ActivityRecord r) {
        if (((r.intent.getFlags() & 1073741824) != 0 || (r.info.flags & 128) != 0) && !r.finishing && !this.mService.isSleeping()) {
            requestFinishActivityLocked(r.appToken, 0, null, "no-history", false);
        }
        if (r.app != null && r.app.thread != null) {
            adjustFocusedActivityLocked(r, "stopActivity");
            r.resumeKeyDispatchingLocked();
            try {
                r.stopped = false;
                r.state = ActivityState.STOPPING;
                if (!r.visible) {
                    this.mWindowManager.setAppVisibility(r.appToken, false);
                }
                r.app.thread.scheduleStopActivity(r.appToken, r.visible, r.configChangeFlags);
                if (this.mService.isSleepingOrShuttingDown()) {
                    r.setSleeping(SHOW_APP_STARTING_PREVIEW);
                }
                Message msg = this.mHandler.obtainMessage(104, r);
                this.mHandler.sendMessageDelayed(msg, 10000L);
            } catch (Exception e) {
                Slog.w("ActivityManager", "Exception thrown during pause", e);
                r.stopped = SHOW_APP_STARTING_PREVIEW;
                r.state = ActivityState.STOPPED;
                if (r.configDestroy) {
                    destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, "stop-except");
                }
            }
        }
    }

    final boolean requestFinishActivityLocked(IBinder token, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        ActivityRecord r = isInStackLocked(token);
        if (r == null) {
            return false;
        }
        finishActivityLocked(r, resultCode, resultData, reason, oomAdj);
        return SHOW_APP_STARTING_PREVIEW;
    }

    final void finishSubActivityLocked(ActivityRecord self, String resultWho, int requestCode) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.resultTo == self && r.requestCode == requestCode && ((r.resultWho == null && resultWho == null) || (r.resultWho != null && r.resultWho.equals(resultWho)))) {
                    finishActivityLocked(r, 0, null, "request-sub", false);
                }
            }
        }
        this.mService.updateOomAdjLocked();
    }

    final void finishTopRunningActivityLocked(ProcessRecord app) {
        ActivityRecord r = topRunningActivityLocked(null);
        if (r != null && r.app == app) {
            Slog.w("ActivityManager", "  Force finishing activity 1 " + r.intent.getComponent().flattenToShortString());
            int taskNdx = this.mTaskHistory.indexOf(r.task);
            int activityNdx = r.task.mActivities.indexOf(r);
            finishActivityLocked(r, 0, null, "crashed", false);
            int activityNdx2 = activityNdx - 1;
            if (activityNdx2 < 0) {
                do {
                    taskNdx--;
                    if (taskNdx < 0) {
                        break;
                    } else {
                        activityNdx2 = this.mTaskHistory.get(taskNdx).mActivities.size() - 1;
                    }
                } while (activityNdx2 < 0);
            }
            if (activityNdx2 >= 0) {
                ActivityRecord r2 = this.mTaskHistory.get(taskNdx).mActivities.get(activityNdx2);
                if (r2.state == ActivityState.RESUMED || r2.state == ActivityState.PAUSING || r2.state == ActivityState.PAUSED) {
                    if (!r2.isHomeActivity() || this.mService.mHomeProcess != r2.app) {
                        Slog.w("ActivityManager", "  Force finishing activity 2 " + r2.intent.getComponent().flattenToShortString());
                        finishActivityLocked(r2, 0, null, "crashed", false);
                    }
                }
            }
        }
    }

    final void finishVoiceTask(IVoiceInteractionSession session) {
        IBinder sessionBinder = session.asBinder();
        boolean didOne = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord tr = this.mTaskHistory.get(taskNdx);
            if (tr.voiceSession != null && tr.voiceSession.asBinder() == sessionBinder) {
                for (int activityNdx = tr.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = tr.mActivities.get(activityNdx);
                    if (!r.finishing) {
                        finishActivityLocked(r, 0, null, "finish-voice", false);
                        didOne = SHOW_APP_STARTING_PREVIEW;
                    }
                }
            }
        }
        if (didOne) {
            this.mService.updateOomAdjLocked();
        }
    }

    final boolean finishActivityAffinityLocked(ActivityRecord r) {
        ArrayList<ActivityRecord> activities = r.task.mActivities;
        for (int index = activities.indexOf(r); index >= 0; index--) {
            ActivityRecord cur = activities.get(index);
            if (!Objects.equals(cur.taskAffinity, r.taskAffinity)) {
                break;
            }
            finishActivityLocked(cur, 0, null, "request-affinity", SHOW_APP_STARTING_PREVIEW);
        }
        return SHOW_APP_STARTING_PREVIEW;
    }

    final void finishActivityResultsLocked(ActivityRecord r, int resultCode, Intent resultData) {
        ActivityRecord resultTo = r.resultTo;
        if (resultTo != null) {
            if (resultTo.userId != r.userId && resultData != null) {
                resultData.setContentUserHint(r.userId);
            }
            if (r.info.applicationInfo.uid > 0) {
                this.mService.grantUriPermissionFromIntentLocked(r.info.applicationInfo.uid, resultTo.packageName, resultData, resultTo.getUriPermissionsLocked(), resultTo.userId);
            }
            resultTo.addResultLocked(r, r.resultWho, r.requestCode, resultCode, resultData);
            r.resultTo = null;
        }
        r.results = null;
        r.pendingResults = null;
        r.newIntents = null;
        r.icicle = null;
    }

    final boolean finishActivityLocked(ActivityRecord r, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        if (r.finishing) {
            Slog.w("ActivityManager", "Duplicate finish request for " + r);
            return false;
        }
        r.makeFinishing();
        TaskRecord task = r.task;
        EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName, reason);
        ArrayList<ActivityRecord> activities = task.mActivities;
        int index = activities.indexOf(r);
        if (index < activities.size() - 1) {
            task.setFrontOfTask();
            if ((r.intent.getFlags() & 524288) != 0) {
                ActivityRecord next = activities.get(index + 1);
                next.intent.addFlags(524288);
            }
        }
        r.pauseKeyDispatchingLocked();
        adjustFocusedActivityLocked(r, "finishActivity");
        finishActivityResultsLocked(r, resultCode, resultData);
        if (this.mResumedActivity == r) {
            boolean endTask = index <= 0 ? SHOW_APP_STARTING_PREVIEW : false;
            this.mWindowManager.prepareAppTransition(endTask ? 9 : 7, false);
            this.mWindowManager.setAppVisibility(r.appToken, false);
            if (this.mPausingActivity == null) {
                startPausingLocked(false, false, false, false);
            }
            if (endTask) {
                this.mStackSupervisor.endLockTaskModeIfTaskEnding(task);
            }
        } else {
            if (r.state == ActivityState.PAUSING || finishCurrentActivityLocked(r, 1, oomAdj) != null) {
                return false;
            }
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    final ActivityRecord finishCurrentActivityLocked(ActivityRecord r, int mode, boolean oomAdj) {
        if (mode == 2 && r.nowVisible) {
            if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
                this.mStackSupervisor.mStoppingActivities.add(r);
                if (this.mStackSupervisor.mStoppingActivities.size() > 3 || (r.frontOfTask && this.mTaskHistory.size() <= 1)) {
                    this.mStackSupervisor.scheduleIdleLocked();
                } else {
                    this.mStackSupervisor.checkReadyForSleepLocked();
                }
            }
            r.state = ActivityState.STOPPING;
            if (oomAdj) {
                this.mService.updateOomAdjLocked();
                return r;
            }
            return r;
        }
        this.mStackSupervisor.mStoppingActivities.remove(r);
        this.mStackSupervisor.mGoingToSleepActivities.remove(r);
        this.mStackSupervisor.mWaitingVisibleActivities.remove(r);
        if (this.mResumedActivity == r) {
            this.mResumedActivity = null;
        }
        ActivityState prevState = r.state;
        r.state = ActivityState.FINISHING;
        if (mode == 0 || prevState == ActivityState.STOPPED || prevState == ActivityState.INITIALIZING) {
            r.makeFinishing();
            boolean activityRemoved = destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, "finish-imm");
            if (activityRemoved) {
                this.mStackSupervisor.resumeTopActivitiesLocked();
            }
            if (activityRemoved) {
                return null;
            }
            return r;
        }
        this.mStackSupervisor.mFinishingActivities.add(r);
        r.resumeKeyDispatchingLocked();
        this.mStackSupervisor.getFocusedStack().resumeTopActivityLocked(null);
        return r;
    }

    void finishAllActivitiesLocked(boolean immediately) {
        boolean noActivitiesInStack = SHOW_APP_STARTING_PREVIEW;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                noActivitiesInStack = false;
                if (!r.finishing || immediately) {
                    Slog.d("ActivityManager", "finishAllActivitiesLocked: finishing " + r + " immediately");
                    finishCurrentActivityLocked(r, 0, false);
                }
            }
        }
        if (noActivitiesInStack) {
            this.mActivityContainer.onTaskListEmptyLocked();
        }
    }

    final boolean shouldUpRecreateTaskLocked(ActivityRecord srec, String destAffinity) {
        if (srec == null || srec.task.affinity == null || !srec.task.affinity.equals(destAffinity)) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (srec.frontOfTask && srec.task != null && srec.task.getBaseIntent() != null && srec.task.getBaseIntent().isDocument()) {
            if (srec.task.getTaskToReturnTo() != 0) {
                return SHOW_APP_STARTING_PREVIEW;
            }
            int taskIdx = this.mTaskHistory.indexOf(srec.task);
            if (taskIdx <= 0) {
                Slog.w("ActivityManager", "shouldUpRecreateTask: task not in history for " + srec);
                return false;
            }
            if (taskIdx == 0) {
                return SHOW_APP_STARTING_PREVIEW;
            }
            TaskRecord prevTask = this.mTaskHistory.get(taskIdx);
            if (!srec.task.affinity.equals(prevTask.affinity)) {
                return SHOW_APP_STARTING_PREVIEW;
            }
        }
        return false;
    }

    final boolean navigateUpToLocked(IBinder token, Intent destIntent, int resultCode, Intent resultData) {
        ActivityRecord next;
        ActivityRecord srec = ActivityRecord.forToken(token);
        TaskRecord task = srec.task;
        ArrayList<ActivityRecord> activities = task.mActivities;
        int start = activities.indexOf(srec);
        if (!this.mTaskHistory.contains(task) || start < 0) {
            return false;
        }
        int finishTo = start - 1;
        ActivityRecord parent = finishTo < 0 ? null : activities.get(finishTo);
        boolean foundParentInTask = false;
        ComponentName dest = destIntent.getComponent();
        if (start > 0 && dest != null) {
            int i = finishTo;
            while (true) {
                if (i < 0) {
                    break;
                }
                ActivityRecord r = activities.get(i);
                if (r.info.packageName.equals(dest.getPackageName()) && r.info.name.equals(dest.getClassName())) {
                    finishTo = i;
                    parent = r;
                    foundParentInTask = SHOW_APP_STARTING_PREVIEW;
                    break;
                }
                i--;
            }
        }
        IActivityController controller = this.mService.mController;
        if (controller != null && (next = topRunningActivityLocked(srec.appToken, 0)) != null) {
            boolean resumeOK = SHOW_APP_STARTING_PREVIEW;
            try {
                resumeOK = controller.activityResuming(next.packageName);
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
            if (!resumeOK) {
                return false;
            }
        }
        long origId = Binder.clearCallingIdentity();
        for (int i2 = start; i2 > finishTo; i2--) {
            requestFinishActivityLocked(activities.get(i2).appToken, resultCode, resultData, "navigate-up", SHOW_APP_STARTING_PREVIEW);
            resultCode = 0;
            resultData = null;
        }
        if (parent != null && foundParentInTask) {
            int parentLaunchMode = parent.info.launchMode;
            int destIntentFlags = destIntent.getFlags();
            if (parentLaunchMode == 3 || parentLaunchMode == 2 || parentLaunchMode == 1 || (67108864 & destIntentFlags) != 0) {
                parent.deliverNewIntentLocked(srec.info.applicationInfo.uid, destIntent, srec.packageName);
            } else {
                try {
                    ActivityInfo aInfo = AppGlobals.getPackageManager().getActivityInfo(destIntent.getComponent(), 0, srec.userId);
                    int res = this.mStackSupervisor.startActivityLocked(srec.app.thread, destIntent, null, aInfo, null, null, parent.appToken, null, 0, -1, parent.launchedFromUid, parent.launchedFromPackage, -1, parent.launchedFromUid, 0, null, SHOW_APP_STARTING_PREVIEW, null, null, null);
                    foundParentInTask = res == 0 ? SHOW_APP_STARTING_PREVIEW : false;
                } catch (RemoteException e2) {
                    foundParentInTask = false;
                }
                requestFinishActivityLocked(parent.appToken, resultCode, resultData, "navigate-up", SHOW_APP_STARTING_PREVIEW);
            }
        }
        Binder.restoreCallingIdentity(origId);
        return foundParentInTask;
    }

    final void cleanUpActivityLocked(ActivityRecord r, boolean cleanServices, boolean setState) {
        if (this.mResumedActivity == r) {
            this.mResumedActivity = null;
        }
        if (this.mPausingActivity == r) {
            this.mPausingActivity = null;
        }
        this.mService.clearFocusedActivity(r);
        r.configDestroy = false;
        r.frozenBeforeDestroy = false;
        if (setState) {
            r.state = ActivityState.DESTROYED;
            r.app = null;
        }
        this.mStackSupervisor.mFinishingActivities.remove(r);
        this.mStackSupervisor.mWaitingVisibleActivities.remove(r);
        if (r.finishing && r.pendingResults != null) {
            for (WeakReference<PendingIntentRecord> apr : r.pendingResults) {
                PendingIntentRecord rec = apr.get();
                if (rec != null) {
                    this.mService.cancelIntentSenderLocked(rec, false);
                }
            }
            r.pendingResults = null;
        }
        if (cleanServices) {
            cleanUpActivityServicesLocked(r);
        }
        removeTimeoutsForActivityLocked(r);
        if (getVisibleBehindActivity() == r) {
            this.mStackSupervisor.requestVisibleBehindLocked(r, false);
        }
    }

    private void removeTimeoutsForActivityLocked(ActivityRecord r) {
        this.mStackSupervisor.removeTimeoutsForActivityLocked(r);
        this.mHandler.removeMessages(101, r);
        this.mHandler.removeMessages(104, r);
        this.mHandler.removeMessages(102, r);
        r.finishLaunchTickingLocked();
    }

    private void removeActivityFromHistoryLocked(ActivityRecord r, String reason) {
        this.mStackSupervisor.removeChildActivityContainers(r);
        finishActivityResultsLocked(r, 0, null);
        r.makeFinishing();
        r.takeFromHistory();
        removeTimeoutsForActivityLocked(r);
        r.state = ActivityState.DESTROYED;
        r.app = null;
        this.mWindowManager.removeAppToken(r.appToken);
        TaskRecord task = r.task;
        if (task != null && task.removeActivity(r)) {
            if (this.mStackSupervisor.isFrontStack(this) && task == topTask() && task.isOverHomeStack()) {
                this.mStackSupervisor.moveHomeStackTaskToTop(task.getTaskToReturnTo(), reason);
            }
            removeTask(task, reason);
        }
        cleanUpActivityServicesLocked(r);
        r.removeUriPermissionsLocked();
    }

    final void cleanUpActivityServicesLocked(ActivityRecord r) {
        if (r.connections != null) {
            for (ConnectionRecord c : r.connections) {
                this.mService.mServices.removeConnectionLocked(c, null, r);
            }
            r.connections = null;
        }
    }

    final void scheduleDestroyActivities(ProcessRecord owner, String reason) {
        Message msg = this.mHandler.obtainMessage(105);
        msg.obj = new ScheduleDestroyArgs(owner, reason);
        this.mHandler.sendMessage(msg);
    }

    final void destroyActivitiesLocked(ProcessRecord owner, String reason) {
        boolean lastIsOpaque = false;
        boolean activityRemoved = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing) {
                    if (r.fullscreen) {
                        lastIsOpaque = SHOW_APP_STARTING_PREVIEW;
                    }
                    if ((owner == null || r.app == owner) && lastIsOpaque && r.isDestroyable() && destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, reason)) {
                        activityRemoved = SHOW_APP_STARTING_PREVIEW;
                    }
                }
            }
        }
        if (activityRemoved) {
            this.mStackSupervisor.resumeTopActivitiesLocked();
        }
    }

    final boolean safelyDestroyActivityLocked(ActivityRecord r, String reason) {
        if (r.isDestroyable()) {
            return destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, reason);
        }
        return false;
    }

    final int releaseSomeActivitiesLocked(ProcessRecord app, ArraySet<TaskRecord> tasks, String reason) {
        int maxTasks = tasks.size() / 4;
        if (maxTasks < 1) {
            maxTasks = 1;
        }
        int numReleased = 0;
        int taskNdx = 0;
        while (taskNdx < this.mTaskHistory.size() && maxTasks > 0) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (tasks.contains(task)) {
                int curNum = 0;
                ArrayList<ActivityRecord> activities = task.mActivities;
                int actNdx = 0;
                while (actNdx < activities.size()) {
                    ActivityRecord activity = activities.get(actNdx);
                    if (activity.app == app && activity.isDestroyable()) {
                        destroyActivityLocked(activity, SHOW_APP_STARTING_PREVIEW, reason);
                        if (activities.get(actNdx) != activity) {
                            actNdx--;
                        }
                        curNum++;
                    }
                    actNdx++;
                }
                if (curNum > 0) {
                    numReleased += curNum;
                    maxTasks--;
                    if (this.mTaskHistory.get(taskNdx) != task) {
                        taskNdx--;
                    }
                }
            }
            taskNdx++;
        }
        return numReleased;
    }

    final boolean destroyActivityLocked(ActivityRecord r, boolean removeFromApp, String reason) {
        boolean hadApp = SHOW_APP_STARTING_PREVIEW;
        EventLog.writeEvent(EventLogTags.AM_DESTROY_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName, reason);
        boolean removedFromHistory = false;
        cleanUpActivityLocked(r, false, false);
        if (r.app == null) {
            hadApp = false;
        }
        if (hadApp) {
            if (removeFromApp) {
                r.app.activities.remove(r);
                if (this.mService.mHeavyWeightProcess == r.app && r.app.activities.size() <= 0) {
                    this.mService.mHeavyWeightProcess = null;
                    this.mService.mHandler.sendEmptyMessage(25);
                }
                if (r.app.activities.isEmpty()) {
                    this.mService.mServices.updateServiceConnectionActivitiesLocked(r.app);
                    this.mService.updateLruProcessLocked(r.app, false, null);
                    this.mService.updateOomAdjLocked();
                }
            }
            boolean skipDestroy = false;
            try {
                r.app.thread.scheduleDestroyActivity(r.appToken, r.finishing, r.configChangeFlags);
            } catch (Exception e) {
                if (r.finishing) {
                    removeActivityFromHistoryLocked(r, reason + " exceptionInScheduleDestroy");
                    removedFromHistory = SHOW_APP_STARTING_PREVIEW;
                    skipDestroy = SHOW_APP_STARTING_PREVIEW;
                }
            }
            r.nowVisible = false;
            if (r.finishing && !skipDestroy) {
                r.state = ActivityState.DESTROYING;
                Message msg = this.mHandler.obtainMessage(102, r);
                this.mHandler.sendMessageDelayed(msg, 10000L);
            } else {
                r.state = ActivityState.DESTROYED;
                r.app = null;
            }
        } else if (r.finishing) {
            removeActivityFromHistoryLocked(r, reason + " hadNoApp");
            removedFromHistory = SHOW_APP_STARTING_PREVIEW;
        } else {
            r.state = ActivityState.DESTROYED;
            r.app = null;
        }
        r.configChangeFlags = 0;
        if (!this.mLRUActivities.remove(r) && hadApp) {
            Slog.w("ActivityManager", "Activity " + r + " being finished, but not in LRU list");
        }
        return removedFromHistory;
    }

    final void activityDestroyedLocked(IBinder token, String reason) {
        long origId = Binder.clearCallingIdentity();
        try {
            ActivityRecord r = ActivityRecord.forToken(token);
            if (r != null) {
                this.mHandler.removeMessages(102, r);
            }
            if (isInStackLocked(token) != null && r.state == ActivityState.DESTROYING) {
                cleanUpActivityLocked(r, SHOW_APP_STARTING_PREVIEW, false);
                removeActivityFromHistoryLocked(r, reason);
            }
            this.mStackSupervisor.resumeTopActivitiesLocked();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void releaseBackgroundResources() {
        ActivityRecord r;
        if (hasVisibleBehindActivity() && !this.mHandler.hasMessages(107) && (r = getVisibleBehindActivity()) != topRunningActivityLocked(null)) {
            if (r != null && r.app != null && r.app.thread != null) {
                try {
                    r.app.thread.scheduleCancelVisibleBehind(r.appToken);
                } catch (RemoteException e) {
                }
                this.mHandler.sendEmptyMessageDelayed(107, 500L);
            } else {
                Slog.e("ActivityManager", "releaseBackgroundResources: activity " + r + " no longer running");
                backgroundResourcesReleased();
            }
        }
    }

    final void backgroundResourcesReleased() {
        this.mHandler.removeMessages(107);
        ActivityRecord r = getVisibleBehindActivity();
        if (r != null) {
            this.mStackSupervisor.mStoppingActivities.add(r);
            setVisibleBehindActivity(null);
            this.mStackSupervisor.scheduleIdleTimeoutLocked(null);
        }
        this.mStackSupervisor.resumeTopActivitiesLocked();
    }

    boolean hasVisibleBehindActivity() {
        if (isAttached() && this.mActivityContainer.mActivityDisplay.hasVisibleBehindActivity()) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        return false;
    }

    void setVisibleBehindActivity(ActivityRecord r) {
        if (isAttached()) {
            this.mActivityContainer.mActivityDisplay.setVisibleBehindActivity(r);
        }
    }

    ActivityRecord getVisibleBehindActivity() {
        if (isAttached()) {
            return this.mActivityContainer.mActivityDisplay.mVisibleBehindActivity;
        }
        return null;
    }

    private void removeHistoryRecordsForAppLocked(ArrayList<ActivityRecord> list, ProcessRecord app, String listName) {
        int i = list.size();
        while (i > 0) {
            i--;
            ActivityRecord r = list.get(i);
            if (r.app == app) {
                list.remove(i);
                removeTimeoutsForActivityLocked(r);
            }
        }
    }

    boolean removeHistoryRecordsForAppLocked(ProcessRecord app) {
        boolean remove;
        removeHistoryRecordsForAppLocked(this.mLRUActivities, app, "mLRUActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mStoppingActivities, app, "mStoppingActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mGoingToSleepActivities, app, "mGoingToSleepActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mWaitingVisibleActivities, app, "mWaitingVisibleActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mFinishingActivities, app, "mFinishingActivities");
        boolean hasVisibleActivities = false;
        int i = numActivities();
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                i--;
                if (r.app == app) {
                    if ((!r.haveState && !r.stateNotNeeded) || r.finishing) {
                        remove = SHOW_APP_STARTING_PREVIEW;
                    } else if (r.launchCount > 2 && r.lastLaunchTime > SystemClock.uptimeMillis() - 60000) {
                        remove = SHOW_APP_STARTING_PREVIEW;
                    } else {
                        remove = false;
                    }
                    if (remove) {
                        if (!r.finishing) {
                            Slog.w("ActivityManager", "Force removing " + r + ": app died, no saved state");
                            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName, "proc died without state saved");
                            if (r.state == ActivityState.RESUMED) {
                                this.mService.updateUsageStats(r, false);
                            }
                        }
                        removeActivityFromHistoryLocked(r, "appDied");
                    } else {
                        if (r.visible) {
                            hasVisibleActivities = SHOW_APP_STARTING_PREVIEW;
                        }
                        r.app = null;
                        r.nowVisible = false;
                        if (!r.haveState) {
                            r.icicle = null;
                        }
                    }
                    cleanUpActivityLocked(r, SHOW_APP_STARTING_PREVIEW, SHOW_APP_STARTING_PREVIEW);
                }
            }
        }
        return hasVisibleActivities;
    }

    final void updateTransitLocked(int transit, Bundle options) {
        if (options != null) {
            ActivityRecord r = topRunningActivityLocked(null);
            if (r != null && r.state != ActivityState.RESUMED) {
                r.updateOptionsLocked(options);
            } else {
                ActivityOptions.abort(options);
            }
        }
        this.mWindowManager.prepareAppTransition(transit, false);
    }

    void updateTaskMovement(TaskRecord task, boolean toFront) {
        if (task.isPersistable) {
            task.mLastTimeMoved = System.currentTimeMillis();
            if (!toFront) {
                task.mLastTimeMoved *= -1;
            }
        }
    }

    void moveHomeStackTaskToTop(int homeStackTaskType) {
        int top = this.mTaskHistory.size() - 1;
        for (int taskNdx = top; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.taskType == homeStackTaskType) {
                this.mTaskHistory.remove(taskNdx);
                this.mTaskHistory.add(top, task);
                updateTaskMovement(task, SHOW_APP_STARTING_PREVIEW);
                this.mWindowManager.moveTaskToTop(task.taskId);
                return;
            }
        }
    }

    final void moveTaskToFrontLocked(TaskRecord tr, ActivityRecord source, Bundle options, String reason) {
        int numTasks = this.mTaskHistory.size();
        int index = this.mTaskHistory.indexOf(tr);
        if (numTasks == 0 || index < 0) {
            if (source != null && (source.intent.getFlags() & 65536) != 0) {
                ActivityOptions.abort(options);
                return;
            } else {
                updateTransitLocked(10, options);
                return;
            }
        }
        insertTaskAtTop(tr);
        moveToFront(reason);
        if (source != null && (source.intent.getFlags() & 65536) != 0) {
            this.mWindowManager.prepareAppTransition(0, false);
            ActivityRecord r = topRunningActivityLocked(null);
            if (r != null) {
                this.mNoAnimActivities.add(r);
            }
            ActivityOptions.abort(options);
        } else {
            updateTransitLocked(10, options);
        }
        this.mStackSupervisor.resumeTopActivitiesLocked();
        EventLog.writeEvent(EventLogTags.AM_TASK_TO_FRONT, Integer.valueOf(tr.userId), Integer.valueOf(tr.taskId));
    }

    final boolean moveTaskToBackLocked(int taskId) {
        TaskRecord tr = taskForIdLocked(taskId);
        if (tr == null) {
            Slog.i("ActivityManager", "moveTaskToBack: bad taskId=" + taskId);
            return false;
        }
        Slog.i("ActivityManager", "moveTaskToBack: " + tr);
        this.mStackSupervisor.endLockTaskModeIfTaskEnding(tr);
        if (this.mStackSupervisor.isFrontStack(this) && this.mService.mController != null) {
            ActivityRecord next = topRunningActivityLocked(null, taskId);
            if (next == null) {
                next = topRunningActivityLocked(null, 0);
            }
            if (next != null) {
                boolean moveOK = SHOW_APP_STARTING_PREVIEW;
                try {
                    moveOK = this.mService.mController.activityResuming(next.packageName);
                } catch (RemoteException e) {
                    this.mService.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                }
                if (!moveOK) {
                    return false;
                }
            }
        }
        this.mTaskHistory.remove(tr);
        this.mTaskHistory.add(0, tr);
        updateTaskMovement(tr, false);
        int numTasks = this.mTaskHistory.size();
        for (int taskNdx = numTasks - 1; taskNdx >= 1; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.isOverHomeStack()) {
                break;
            }
            if (taskNdx == 1) {
                task.setTaskToReturnTo(1);
            }
        }
        this.mWindowManager.prepareAppTransition(11, false);
        this.mWindowManager.moveTaskToBottom(taskId);
        if (((this.mResumedActivity != null ? this.mResumedActivity.task : null) == tr && tr.isOverHomeStack()) || (numTasks <= 1 && isOnHomeDisplay())) {
            if (!this.mService.mBooting && !this.mService.mBooted) {
                return false;
            }
            int taskToReturnTo = tr.getTaskToReturnTo();
            tr.setTaskToReturnTo(0);
            return this.mStackSupervisor.resumeHomeStackTask(taskToReturnTo, null, "moveTaskToBack");
        }
        this.mStackSupervisor.resumeTopActivitiesLocked();
        return SHOW_APP_STARTING_PREVIEW;
    }

    static final void logStartActivity(int tag, ActivityRecord r, TaskRecord task) {
        Uri data = r.intent.getData();
        String strData = data != null ? data.toSafeString() : null;
        EventLog.writeEvent(tag, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName, r.intent.getAction(), r.intent.getType(), strData, Integer.valueOf(r.intent.getFlags()));
    }

    final boolean ensureActivityConfigurationLocked(ActivityRecord r, int globalChanges) {
        if (this.mConfigWillChange) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        Configuration newConfig = this.mService.mConfiguration;
        if (r.configuration == newConfig && !r.forceNewConfig) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (r.finishing) {
            r.stopFreezingScreenLocked(false);
            return SHOW_APP_STARTING_PREVIEW;
        }
        Configuration oldConfig = r.configuration;
        r.configuration = newConfig;
        int changes = oldConfig.diff(newConfig);
        if (changes == 0 && !r.forceNewConfig) {
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (r.app == null || r.app.thread == null) {
            r.stopFreezingScreenLocked(false);
            r.forceNewConfig = false;
            return SHOW_APP_STARTING_PREVIEW;
        }
        if (((r.info.getRealConfigChanged() ^ (-1)) & changes) != 0 || r.forceNewConfig) {
            r.configChangeFlags |= changes;
            r.startFreezingScreenLocked(r.app, globalChanges);
            r.forceNewConfig = false;
            if (r.app == null || r.app.thread == null) {
                destroyActivityLocked(r, SHOW_APP_STARTING_PREVIEW, "config");
            } else {
                if (r.state == ActivityState.PAUSING) {
                    r.configDestroy = SHOW_APP_STARTING_PREVIEW;
                    return SHOW_APP_STARTING_PREVIEW;
                }
                if (r.state == ActivityState.RESUMED) {
                    relaunchActivityLocked(r, r.configChangeFlags, SHOW_APP_STARTING_PREVIEW);
                    r.configChangeFlags = 0;
                } else {
                    relaunchActivityLocked(r, r.configChangeFlags, false);
                    r.configChangeFlags = 0;
                }
            }
            return false;
        }
        if (r.app != null && r.app.thread != null) {
            try {
                r.app.thread.scheduleActivityConfigurationChanged(r.appToken);
            } catch (RemoteException e) {
            }
        }
        r.stopFreezingScreenLocked(false);
        return SHOW_APP_STARTING_PREVIEW;
    }

    private boolean relaunchActivityLocked(ActivityRecord r, int changes, boolean andResume) {
        List<ResultInfo> results = null;
        List<ReferrerIntent> newIntents = null;
        if (andResume) {
            results = r.results;
            newIntents = r.newIntents;
        }
        EventLog.writeEvent(andResume ? EventLogTags.AM_RELAUNCH_RESUME_ACTIVITY : EventLogTags.AM_RELAUNCH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName);
        r.startFreezingScreenLocked(r.app, 0);
        this.mStackSupervisor.removeChildActivityContainers(r);
        try {
            r.forceNewConfig = false;
            r.app.thread.scheduleRelaunchActivity(r.appToken, results, newIntents, changes, andResume ? false : true, new Configuration(this.mService.mConfiguration));
        } catch (RemoteException e) {
        }
        if (andResume) {
            r.results = null;
            r.newIntents = null;
            r.state = ActivityState.RESUMED;
        } else {
            this.mHandler.removeMessages(101, r);
            r.state = ActivityState.PAUSED;
        }
        return SHOW_APP_STARTING_PREVIEW;
    }

    boolean willActivityBeVisibleLocked(IBinder token) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.appToken == token) {
                    return SHOW_APP_STARTING_PREVIEW;
                }
                if (r.fullscreen && !r.finishing) {
                    return false;
                }
            }
        }
        ActivityRecord r2 = ActivityRecord.forToken(token);
        if (r2 == null) {
            return false;
        }
        if (r2.finishing) {
            Slog.e("ActivityManager", "willActivityBeVisibleLocked: Returning false, would have returned true for r=" + r2);
        }
        return !r2.finishing;
    }

    void closeSystemDialogsLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if ((r.info.flags & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                    finishActivityLocked(r, 0, null, "close-sys", SHOW_APP_STARTING_PREVIEW);
                }
            }
        }
    }

    boolean forceStopPackageLocked(String name, boolean doit, boolean evenPersistent, int userId) {
        boolean didSomething = false;
        TaskRecord lastTask = null;
        ComponentName homeActivity = null;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            int numActivities = activities.size();
            int activityNdx = 0;
            while (activityNdx < numActivities) {
                ActivityRecord r = activities.get(activityNdx);
                boolean samePackage = (r.packageName.equals(name) || (name == null && r.userId == userId)) ? SHOW_APP_STARTING_PREVIEW : false;
                if ((userId == -1 || r.userId == userId) && ((samePackage || r.task == lastTask) && (r.app == null || evenPersistent || !r.app.persistent))) {
                    if (!doit) {
                        if (!r.finishing) {
                            return SHOW_APP_STARTING_PREVIEW;
                        }
                    } else if (r.isHomeActivity()) {
                        if (homeActivity != null && homeActivity.equals(r.realActivity)) {
                            Slog.i("ActivityManager", "Skip force-stop again " + r);
                        } else {
                            homeActivity = r.realActivity;
                            didSomething = SHOW_APP_STARTING_PREVIEW;
                            Slog.i("ActivityManager", "  Force finishing activity 3 " + r);
                            if (samePackage) {
                            }
                            lastTask = r.task;
                            if (!finishActivityLocked(r, 0, null, "force-stop", SHOW_APP_STARTING_PREVIEW)) {
                            }
                        }
                    } else {
                        didSomething = SHOW_APP_STARTING_PREVIEW;
                        Slog.i("ActivityManager", "  Force finishing activity 3 " + r);
                        if (samePackage) {
                            if (r.app != null) {
                                r.app.removed = SHOW_APP_STARTING_PREVIEW;
                            }
                            r.app = null;
                        }
                        lastTask = r.task;
                        if (!finishActivityLocked(r, 0, null, "force-stop", SHOW_APP_STARTING_PREVIEW)) {
                            numActivities--;
                            activityNdx--;
                        }
                    }
                }
                activityNdx++;
            }
        }
        return didSomething;
    }

    void getTasksLocked(List<ActivityManager.RunningTaskInfo> list, int callingUid, boolean allowed) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ActivityRecord r = null;
            ActivityRecord top = null;
            int numActivities = 0;
            int numRunning = 0;
            ArrayList<ActivityRecord> activities = task.mActivities;
            if (!activities.isEmpty() && (allowed || task.isHomeTask() || task.effectiveUid == callingUid)) {
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r2 = activities.get(activityNdx);
                    r = r2;
                    if (top == null || top.state == ActivityState.INITIALIZING) {
                        top = r;
                        numRunning = 0;
                        numActivities = 0;
                    }
                    numActivities++;
                    if (r.app != null && r.app.thread != null) {
                        numRunning++;
                    }
                }
                ActivityManager.RunningTaskInfo ci = new ActivityManager.RunningTaskInfo();
                ci.id = task.taskId;
                ci.baseActivity = r.intent.getComponent();
                ci.topActivity = top.intent.getComponent();
                ci.lastActiveTime = task.lastActiveTime;
                if (top.task != null) {
                    ci.description = top.task.lastDescription;
                }
                ci.numActivities = numActivities;
                ci.numRunning = numRunning;
                list.add(ci);
            }
        }
    }

    public void unhandledBackLocked() {
        int top = this.mTaskHistory.size() - 1;
        if (top >= 0) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(top).mActivities;
            int activityTop = activities.size() - 1;
            if (activityTop > 0) {
                finishActivityLocked(activities.get(activityTop), 0, null, "unhandled-back", SHOW_APP_STARTING_PREVIEW);
            }
        }
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        if (this.mPausingActivity != null && this.mPausingActivity.app == app) {
            this.mPausingActivity = null;
        }
        if (this.mLastPausedActivity != null && this.mLastPausedActivity.app == app) {
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        }
        return removeHistoryRecordsForAppLocked(app);
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.app == app) {
                    Slog.w("ActivityManager", "  Force finishing activity 4 " + r.intent.getComponent().flattenToShortString());
                    r.app = null;
                    finishCurrentActivityLocked(r, 0, false);
                }
            }
        }
    }

    boolean dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage, boolean needSep, String header) {
        boolean printed = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            printed |= ActivityStackSupervisor.dumpHistoryList(fd, pw, this.mTaskHistory.get(taskNdx).mActivities, "    ", "Hist", SHOW_APP_STARTING_PREVIEW, !dumpAll ? SHOW_APP_STARTING_PREVIEW : false, dumpClient, dumpPackage, needSep, header, "    Task id #" + task.taskId);
            if (printed) {
                header = null;
            }
        }
        return printed;
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name) {
        ArrayList<ActivityRecord> activities = new ArrayList<>();
        if ("all".equals(name)) {
            for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
                activities.addAll(this.mTaskHistory.get(taskNdx).mActivities);
            }
        } else if ("top".equals(name)) {
            int top = this.mTaskHistory.size() - 1;
            if (top >= 0) {
                ArrayList<ActivityRecord> list = this.mTaskHistory.get(top).mActivities;
                int listTop = list.size() - 1;
                if (listTop >= 0) {
                    activities.add(list.get(listTop));
                }
            }
        } else {
            ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();
            matcher.build(name);
            for (int taskNdx2 = this.mTaskHistory.size() - 1; taskNdx2 >= 0; taskNdx2--) {
                for (ActivityRecord r1 : this.mTaskHistory.get(taskNdx2).mActivities) {
                    if (matcher.match(r1, r1.intent.getComponent())) {
                        activities.add(r1);
                    }
                }
            }
        }
        return activities;
    }

    ActivityRecord restartPackage(String packageName) {
        ActivityRecord starting = topRunningActivityLocked(null);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord a = activities.get(activityNdx);
                if (a.info.packageName.equals(packageName)) {
                    a.forceNewConfig = SHOW_APP_STARTING_PREVIEW;
                    if (starting != null && a == starting && a.visible) {
                        a.startFreezingScreenLocked(starting.app, PackageManagerService.DumpState.DUMP_VERIFIERS);
                    }
                }
            }
        }
        return starting;
    }

    void removeTask(TaskRecord task, String reason) {
        boolean z = SHOW_APP_STARTING_PREVIEW;
        this.mStackSupervisor.endLockTaskModeIfTaskEnding(task);
        this.mWindowManager.removeTask(task.taskId);
        ActivityRecord r = this.mResumedActivity;
        if (r != null && r.task == task) {
            this.mResumedActivity = null;
        }
        int taskNdx = this.mTaskHistory.indexOf(task);
        int topTaskNdx = this.mTaskHistory.size() - 1;
        if (task.isOverHomeStack() && taskNdx < topTaskNdx) {
            TaskRecord nextTask = this.mTaskHistory.get(taskNdx + 1);
            if (!nextTask.isOverHomeStack()) {
                nextTask.setTaskToReturnTo(1);
            }
        }
        this.mTaskHistory.remove(task);
        updateTaskMovement(task, SHOW_APP_STARTING_PREVIEW);
        if (task.mActivities.isEmpty()) {
            boolean isVoiceSession = task.voiceSession != null;
            if (isVoiceSession) {
                try {
                    task.voiceSession.taskFinished(task.intent, task.taskId);
                } catch (RemoteException e) {
                }
            }
            if (task.autoRemoveFromRecents() || isVoiceSession) {
                this.mService.mRecentTasks.remove(task);
                task.removedFromRecents();
            }
        }
        if (this.mTaskHistory.isEmpty()) {
            if (isOnHomeDisplay()) {
                ActivityStackSupervisor activityStackSupervisor = this.mStackSupervisor;
                if (isHomeStack()) {
                    z = false;
                }
                activityStackSupervisor.moveHomeStack(z, reason + " leftTaskHistoryEmpty");
            }
            if (this.mStacks != null) {
                this.mStacks.remove(this);
                this.mStacks.add(0, this);
            }
            this.mActivityContainer.onTaskListEmptyLocked();
        }
    }

    TaskRecord createTaskRecord(int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean toTop) {
        TaskRecord task = new TaskRecord(this.mService, taskId, info, intent, voiceSession, voiceInteractor);
        addTask(task, toTop, false);
        return task;
    }

    ArrayList<TaskRecord> getAllTasks() {
        return new ArrayList<>(this.mTaskHistory);
    }

    void addTask(TaskRecord task, boolean toTop, boolean moving) {
        task.stack = this;
        if (toTop) {
            insertTaskAtTop(task);
        } else {
            this.mTaskHistory.add(0, task);
            updateTaskMovement(task, false);
        }
        if (!moving && task.voiceSession != null) {
            try {
                task.voiceSession.taskStarted(task.intent, task.taskId);
            } catch (RemoteException e) {
            }
        }
    }

    public int getStackId() {
        return this.mStackId;
    }

    public String toString() {
        return "ActivityStack{" + Integer.toHexString(System.identityHashCode(this)) + " stackId=" + this.mStackId + ", " + this.mTaskHistory.size() + " tasks}";
    }
}
