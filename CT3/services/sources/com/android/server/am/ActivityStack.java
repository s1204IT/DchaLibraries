package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.ResultInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.TaskGroup;
import com.android.server.wm.WindowManagerService;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.am.IAWSProcessRecord;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.server.am.AMEventHook;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class ActivityStack {

    private static final int[] f2comandroidserveramActivityStack$ActivityStateSwitchesValues = null;
    static final long ACTIVITY_INACTIVE_RESET_TIME = 0;
    static final int DESTROY_ACTIVITIES_MSG = 105;
    static final int DESTROY_TIMEOUT = 10000;
    static final int DESTROY_TIMEOUT_MSG = 102;
    static final int FINISH_AFTER_PAUSE = 1;
    static final int FINISH_AFTER_VISIBLE = 2;
    static final int FINISH_IMMEDIATELY = 0;
    static final int LAUNCH_TICK = 500;
    static final int LAUNCH_TICK_MSG = 103;
    private static final int MAX_STOPPING_TO_FORCE = 3;
    static final int PAUSE_TIMEOUT = 500;
    static final int PAUSE_TIMEOUT_MSG = 101;
    static final int RELEASE_BACKGROUND_RESOURCES_TIMEOUT_MSG = 107;
    static final int REMOVE_TASK_MODE_DESTROYING = 0;
    static final int REMOVE_TASK_MODE_MOVING = 1;
    static final int REMOVE_TASK_MODE_MOVING_TO_TOP = 2;
    static final boolean SHOW_APP_STARTING_PREVIEW = true;
    static final int STACK_INVISIBLE = 0;
    static final int STACK_VISIBLE = 1;
    static final int STACK_VISIBLE_ACTIVITY_BEHIND = 2;
    static final long START_WARN_TIME = 5000;
    static final int STOP_TIMEOUT = 10000;
    static final int STOP_TIMEOUT_MSG = 104;
    private static final String TAG = "ActivityManager";
    private static final String TAG_ADD_REMOVE = TAG + ActivityManagerDebugConfig.POSTFIX_ADD_REMOVE;
    private static final String TAG_APP = TAG + ActivityManagerDebugConfig.POSTFIX_APP;
    private static final String TAG_CLEANUP = TAG + ActivityManagerDebugConfig.POSTFIX_CLEANUP;
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_CONTAINERS = TAG + ActivityManagerDebugConfig.POSTFIX_CONTAINERS;
    private static final String TAG_PAUSE = TAG + ActivityManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RELEASE = TAG + ActivityManagerDebugConfig.POSTFIX_RELEASE;
    private static final String TAG_RESULTS = TAG + ActivityManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_SAVED_STATE = TAG + ActivityManagerDebugConfig.POSTFIX_SAVED_STATE;
    private static final String TAG_SCREENSHOTS = TAG + ActivityManagerDebugConfig.POSTFIX_SCREENSHOTS;
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static final String TAG_TRANSITION = TAG + ActivityManagerDebugConfig.POSTFIX_TRANSITION;
    private static final String TAG_USER_LEAVING = TAG + ActivityManagerDebugConfig.POSTFIX_USER_LEAVING;
    private static final String TAG_VISIBILITY = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBILITY;
    static final long TRANSLUCENT_CONVERSION_TIMEOUT = 2000;
    static final int TRANSLUCENT_TIMEOUT_MSG = 106;
    private static final boolean VALIDATE_TOKENS = false;
    final ActivityStackSupervisor.ActivityContainer mActivityContainer;
    boolean mConfigWillChange;
    int mCurrentUser;
    int mDisplayId;
    final Handler mHandler;
    private final RecentTasks mRecentTasks;
    final ActivityManagerService mService;
    final int mStackId;
    final ActivityStackSupervisor mStackSupervisor;
    ArrayList<ActivityStack> mStacks;
    private final LaunchingTaskPositioner mTaskPositioner;
    boolean mUpdateBoundsDeferred;
    boolean mUpdateBoundsDeferredCalled;
    final WindowManagerService mWindowManager;
    private final ArrayList<TaskRecord> mTaskHistory = new ArrayList<>();
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
    boolean mFullscreen = true;
    Rect mBounds = null;
    final Rect mDeferredBounds = new Rect();
    final Rect mDeferredTaskBounds = new Rect();
    final Rect mDeferredTaskInsetBounds = new Rect();
    long mLaunchStartTime = 0;
    long mFullyDrawnStartTime = 0;

    private static int[] m925x775af271() {
        if (f2comandroidserveramActivityStack$ActivityStateSwitchesValues != null) {
            return f2comandroidserveramActivityStack$ActivityStateSwitchesValues;
        }
        int[] iArr = new int[ActivityState.valuesCustom().length];
        try {
            iArr[ActivityState.DESTROYED.ordinal()] = 7;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ActivityState.DESTROYING.ordinal()] = 8;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ActivityState.FINISHING.ordinal()] = 9;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ActivityState.INITIALIZING.ordinal()] = 1;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[ActivityState.PAUSED.ordinal()] = 2;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[ActivityState.PAUSING.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[ActivityState.RESUMED.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[ActivityState.STOPPED.ordinal()] = 5;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[ActivityState.STOPPING.ordinal()] = 6;
        } catch (NoSuchFieldError e9) {
        }
        f2comandroidserveramActivityStack$ActivityStateSwitchesValues = iArr;
        return iArr;
    }

    enum ActivityState {
        INITIALIZING,
        RESUMED,
        PAUSING,
        PAUSED,
        STOPPING,
        STOPPED,
        FINISHING,
        DESTROYING,
        DESTROYED;

        public static ActivityState[] valuesCustom() {
            return values();
        }
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
                    Slog.w(ActivityStack.TAG, "Activity pause timeout for " + r);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r.app != null) {
                                ActivityStack.this.mService.logAppTooSlow(r.app, r.pauseTime, "pausing " + r);
                            }
                            ActivityStack.this.activityPausedLocked(r.appToken, true);
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    return;
                case 102:
                    ActivityRecord r2 = (ActivityRecord) msg.obj;
                    Slog.w(ActivityStack.TAG, "Activity destroy timeout for " + r2);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.activityDestroyedLocked(r2 != null ? r2.appToken : null, "destroyTimeout");
                        } catch (Throwable th2) {
                            throw th2;
                        }
                    }
                    return;
                case 103:
                    ActivityRecord r3 = (ActivityRecord) msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r3.continueLaunchTickingLocked()) {
                                ActivityStack.this.mService.logAppTooSlow(r3.app, r3.launchTickTime, "launching " + r3);
                            }
                        } catch (Throwable th3) {
                            throw th3;
                        }
                        break;
                    }
                    return;
                case 104:
                    ActivityRecord r4 = (ActivityRecord) msg.obj;
                    Slog.w(ActivityStack.TAG, "Activity stop timeout for " + r4);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (r4.isInHistory()) {
                                ActivityStack.this.activityStoppedLocked(r4, null, null, null);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    return;
                case 105:
                    ScheduleDestroyArgs args = (ScheduleDestroyArgs) msg.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.destroyActivitiesLocked(args.mOwner, args.mReason);
                        } catch (Throwable th4) {
                            throw th4;
                        }
                    }
                    return;
                case 106:
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.notifyActivityDrawnLocked(null);
                        } catch (Throwable th5) {
                            throw th5;
                        }
                    }
                    return;
                case 107:
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord r5 = ActivityStack.this.getVisibleBehindActivity();
                            Slog.e(ActivityStack.TAG, "Timeout waiting for cancelVisibleBehind player=" + r5);
                            if (r5 != null) {
                                ActivityStack.this.mService.killAppAtUsersRequest(r5.app, null);
                            }
                        } catch (Throwable th6) {
                            throw th6;
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

    ActivityStack(ActivityStackSupervisor.ActivityContainer activityContainer, RecentTasks recentTasks) {
        this.mActivityContainer = activityContainer;
        this.mStackSupervisor = activityContainer.getOuter();
        this.mService = this.mStackSupervisor.mService;
        this.mHandler = new ActivityStackHandler(this.mService.mHandler.getLooper());
        this.mWindowManager = this.mService.mWindowManager;
        this.mStackId = activityContainer.mStackId;
        this.mCurrentUser = this.mService.mUserController.getCurrentUserIdLocked();
        this.mRecentTasks = recentTasks;
        this.mTaskPositioner = this.mStackId == 2 ? new LaunchingTaskPositioner() : null;
    }

    void attachDisplay(ActivityStackSupervisor.ActivityDisplay activityDisplay, boolean onTop) {
        this.mDisplayId = activityDisplay.mDisplayId;
        this.mStacks = activityDisplay.mStacks;
        this.mBounds = this.mWindowManager.attachStack(this.mStackId, activityDisplay.mDisplayId, onTop);
        this.mFullscreen = this.mBounds == null;
        if (this.mTaskPositioner != null) {
            this.mTaskPositioner.setDisplay(activityDisplay.mDisplay);
            this.mTaskPositioner.configure(this.mBounds);
        }
        if (this.mStackId != 3) {
            return;
        }
        this.mStackSupervisor.resizeDockedStackLocked(this.mBounds, null, null, null, null, true);
    }

    void detachDisplay() {
        this.mDisplayId = -1;
        this.mStacks = null;
        if (this.mTaskPositioner != null) {
            this.mTaskPositioner.reset();
        }
        this.mWindowManager.detachStack(this.mStackId);
        if (this.mStackId != 3) {
            return;
        }
        this.mStackSupervisor.resizeDockedStackLocked(null, null, null, null, null, true);
    }

    public void getDisplaySize(Point out) {
        this.mActivityContainer.mActivityDisplay.mDisplay.getSize(out);
    }

    void deferUpdateBounds() {
        if (this.mUpdateBoundsDeferred) {
            return;
        }
        this.mUpdateBoundsDeferred = true;
        this.mUpdateBoundsDeferredCalled = false;
    }

    void continueUpdateBounds() {
        boolean wasDeferred = this.mUpdateBoundsDeferred;
        this.mUpdateBoundsDeferred = false;
        if (!wasDeferred || !this.mUpdateBoundsDeferredCalled) {
            return;
        }
        this.mStackSupervisor.resizeStackUncheckedLocked(this, this.mDeferredBounds.isEmpty() ? null : this.mDeferredBounds, this.mDeferredTaskBounds.isEmpty() ? null : this.mDeferredTaskBounds, this.mDeferredTaskInsetBounds.isEmpty() ? null : this.mDeferredTaskInsetBounds);
    }

    boolean updateBoundsAllowed(Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds) {
        if (!this.mUpdateBoundsDeferred) {
            return true;
        }
        if (bounds != null) {
            this.mDeferredBounds.set(bounds);
        } else {
            this.mDeferredBounds.setEmpty();
        }
        if (tempTaskBounds != null) {
            this.mDeferredTaskBounds.set(tempTaskBounds);
        } else {
            this.mDeferredTaskBounds.setEmpty();
        }
        if (tempTaskInsetBounds != null) {
            this.mDeferredTaskInsetBounds.set(tempTaskInsetBounds);
        } else {
            this.mDeferredTaskInsetBounds.setEmpty();
        }
        this.mUpdateBoundsDeferredCalled = true;
        return false;
    }

    void setBounds(Rect bounds) {
        this.mBounds = this.mFullscreen ? null : new Rect(bounds);
        if (this.mTaskPositioner == null) {
            return;
        }
        this.mTaskPositioner.configure(bounds);
    }

    boolean okToShowLocked(ActivityRecord r) {
        return this.mStackSupervisor.okToShowLocked(r);
    }

    final ActivityRecord topRunningActivityLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            if (MultiWindowManager.isSupported()) {
                TaskRecord task = this.mTaskHistory.get(taskNdx);
                if (!task.mSticky || this.mService.mFocusedActivity == null || task.taskId == this.mService.mFocusedActivity.task.taskId) {
                    ActivityRecord r = this.mTaskHistory.get(taskNdx).topRunningActivityLocked();
                    if (r != null) {
                        return r;
                    }
                }
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
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        return isInStackLocked(r);
    }

    ActivityRecord isInStackLocked(ActivityRecord r) {
        TaskRecord task;
        if (r == null || (task = r.task) == null || task.stack == null || !task.mActivities.contains(r) || !this.mTaskHistory.contains(task)) {
            return null;
        }
        if (task.stack != this) {
            Slog.w(TAG, "Illegal state! task does not point to stack it is in.");
        }
        return r;
    }

    final boolean updateLRUListLocked(ActivityRecord r) {
        boolean hadit = this.mLRUActivities.remove(r);
        this.mLRUActivities.add(r);
        return hadit;
    }

    final boolean isHomeStack() {
        return this.mStackId == 0;
    }

    final boolean isDockedStack() {
        return this.mStackId == 3;
    }

    final boolean isPinnedStack() {
        return this.mStackId == 4;
    }

    final boolean isOnHomeDisplay() {
        return isAttached() && this.mActivityContainer.mActivityDisplay.mDisplayId == 0;
    }

    void moveToFront(String reason) {
        moveToFront(reason, null);
    }

    void moveToFront(String reason, TaskRecord task) {
        if (!isAttached()) {
            return;
        }
        this.mStacks.remove(this);
        int addIndex = this.mStacks.size();
        if (addIndex > 0) {
            ActivityStack topStack = this.mStacks.get(addIndex - 1);
            if (ActivityManager.StackId.isAlwaysOnTop(topStack.mStackId) && topStack != this) {
                addIndex--;
            }
        }
        this.mStacks.add(addIndex, this);
        if (isOnHomeDisplay()) {
            this.mStackSupervisor.setFocusStackUnchecked(reason, this);
        }
        if (task != null) {
            insertTaskAtTop(task, null);
        } else {
            task = topTask();
        }
        if (task != null) {
            this.mWindowManager.moveTaskToTop(task.taskId);
        }
        if (!MultiWindowManager.isSupported()) {
            return;
        }
        restoreStickyTaskLocked(task);
        keepStickyTaskLocked();
    }

    boolean isFocusable() {
        if (ActivityManager.StackId.canReceiveKeys(this.mStackId)) {
            return true;
        }
        ActivityRecord r = topRunningActivityLocked();
        if (r != null) {
            return r.isFocusable();
        }
        return false;
    }

    final boolean isAttached() {
        return this.mStacks != null;
    }

    void findTaskLocked(ActivityRecord target, ActivityStackSupervisor.FindTaskResult result) {
        boolean taskIsDocument;
        Uri data;
        Intent intent = target.intent;
        ActivityInfo info = target.info;
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        boolean isDocument = (intent != null) & intent.isDocument();
        Uri data2 = isDocument ? intent.getData() : null;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG_TASKS, "Looking for task of " + target + " in " + this);
        }
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.voiceSession != null) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "Skipping " + task + ": voice session");
                }
            } else if (task.userId == userId) {
                ActivityRecord r = task.getTopActivity();
                if (r == null || r.finishing || r.userId != userId || r.launchMode == 3) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Skipping " + task + ": mismatch root " + r);
                    }
                } else if (r.mActivityType == target.mActivityType) {
                    Intent taskIntent = task.intent;
                    Intent affinityIntent = task.affinityIntent;
                    if (taskIntent != null && taskIntent.isDocument()) {
                        taskIsDocument = true;
                        data = taskIntent.getData();
                    } else if (affinityIntent == null || !affinityIntent.isDocument()) {
                        taskIsDocument = false;
                        data = null;
                    } else {
                        taskIsDocument = true;
                        data = affinityIntent.getData();
                    }
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Comparing existing cls=" + taskIntent.getComponent().flattenToShortString() + "/aff=" + r.task.rootAffinity + " to new cls=" + intent.getComponent().flattenToShortString() + "/aff=" + info.taskAffinity);
                    }
                    if (taskIntent != null && taskIntent.getComponent() != null && taskIntent.getComponent().compareTo(cls) == 0 && Objects.equals(data2, data)) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "Found matching class!");
                        }
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "For Intent " + intent + " bringing to top: " + r.intent);
                        }
                        result.r = r;
                        result.matchedByRootAffinity = false;
                        return;
                    }
                    if (affinityIntent != null && affinityIntent.getComponent() != null && affinityIntent.getComponent().compareTo(cls) == 0 && Objects.equals(data2, data)) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "Found matching class!");
                        }
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "For Intent " + intent + " bringing to top: " + r.intent);
                        }
                        result.r = r;
                        result.matchedByRootAffinity = false;
                        return;
                    }
                    if (isDocument || taskIsDocument || result.r != null || !task.canMatchRootAffinity()) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "Not a match: " + task);
                        }
                    } else if (task.rootAffinity.equals(target.taskAffinity)) {
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.d(TAG_TASKS, "Found matching affinity candidate!");
                        }
                        result.r = r;
                        result.matchedByRootAffinity = true;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "Skipping " + task + ": mismatch activity type");
                }
            } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG_TASKS, "Skipping " + task + ": different user");
            }
        }
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            boolean notCurrentUserTask = !this.mStackSupervisor.isCurrentProfileLocked(task.userId);
            ArrayList<ActivityRecord> activities = task.mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if ((!notCurrentUserTask || (r.info.flags & 1024) != 0) && !r.finishing && r.userId == userId) {
                    if (compareIntentFilters) {
                        if (r.intent.filterEquals(intent)) {
                            return r;
                        }
                    } else if (r.intent.getComponent().equals(cls)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    final void switchUserLocked(int userId) {
        if (this.mCurrentUser == userId) {
            return;
        }
        this.mCurrentUser = userId;
        int index = this.mTaskHistory.size();
        int i = 0;
        while (i < index) {
            TaskRecord task = this.mTaskHistory.get(i);
            if (this.mStackSupervisor.isCurrentProfileLocked(task.userId) || task.topRunningActivityLocked() != null) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "switchUserLocked: stack=" + getStackId() + " moving " + task + " to top");
                }
                this.mTaskHistory.remove(i);
                this.mTaskHistory.add(task);
                index--;
            } else {
                i++;
            }
        }
    }

    void minimalResumeActivityLocked(ActivityRecord r) {
        r.state = ActivityState.RESUMED;
        ProcessRecord appProc = r.app;
        if (appProc != null) {
            AMEventHookData.AfterActivityResumed eventData = AMEventHookData.AfterActivityResumed.createInstance();
            eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName, Integer.valueOf(r.mActivityType)});
            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityResumed, eventData);
        }
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to RESUMED: " + r + " (starting new instance) callers=" + Debug.getCallers(5));
        }
        this.mResumedActivity = r;
        r.task.touchActiveTime();
        this.mRecentTasks.addLocked(r.task);
        completeResumeLocked(r);
        this.mStackSupervisor.checkReadyForSleepLocked();
        setLaunchTime(r);
        if (!ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            return;
        }
        Slog.i(TAG_SAVED_STATE, "Launch completed; removing icicle of " + r.icicle);
    }

    void addRecentActivityLocked(ActivityRecord r) {
        if (r == null) {
            return;
        }
        this.mRecentTasks.addLocked(r.task);
        r.task.touchActiveTime();
    }

    private void startLaunchTraces(String packageName) {
        if (this.mFullyDrawnStartTime != 0) {
            Trace.asyncTraceEnd(64L, "drawing", 0);
        }
        Trace.asyncTraceBegin(64L, "launching: " + packageName, 0);
        Trace.asyncTraceBegin(64L, "drawing", 0);
    }

    private void stopFullyDrawnTraceIfNeeded() {
        if (this.mFullyDrawnStartTime == 0 || this.mLaunchStartTime != 0) {
            return;
        }
        Trace.asyncTraceEnd(64L, "drawing", 0);
        this.mFullyDrawnStartTime = 0L;
    }

    void setLaunchTime(ActivityRecord r) {
        if (r.displayStartTime != 0) {
            if (this.mLaunchStartTime != 0) {
                return;
            }
            startLaunchTraces(r.packageName);
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mFullyDrawnStartTime = jUptimeMillis;
            this.mLaunchStartTime = jUptimeMillis;
            return;
        }
        long jUptimeMillis2 = SystemClock.uptimeMillis();
        r.displayStartTime = jUptimeMillis2;
        r.fullyDrawnStartTime = jUptimeMillis2;
        if (this.mLaunchStartTime != 0) {
            return;
        }
        startLaunchTraces(r.packageName);
        long j = r.displayStartTime;
        this.mFullyDrawnStartTime = j;
        this.mLaunchStartTime = j;
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
        if (this.mPausingActivity == null) {
            return;
        }
        Slog.d(TAG, "awakeFromSleepingLocked: previously pausing activity didn't pause");
        activityPausedLocked(this.mPausingActivity.appToken, true);
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo aInfo) {
        String packageName = aInfo.packageName;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            List<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                if (packageName.equals(activities.get(activityNdx).packageName)) {
                    activities.get(activityNdx).info.applicationInfo = aInfo;
                }
            }
        }
    }

    boolean checkReadyForSleepLocked() {
        if (this.mResumedActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Sleep needs to pause " + this.mResumedActivity);
            }
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(TAG_USER_LEAVING, "Sleep => pause with userLeaving=false");
            }
            startPausingLocked(false, true, false, false);
            return true;
        }
        if (this.mPausingActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Sleep still waiting to pause " + this.mPausingActivity);
            }
            return true;
        }
        if (!hasVisibleBehindActivity()) {
            return false;
        }
        ActivityRecord r = getVisibleBehindActivity();
        this.mStackSupervisor.mStoppingActivities.add(r);
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Sleep still waiting to stop visible behind " + r);
        }
        return true;
    }

    void goToSleep() {
        ensureActivitiesVisibleLocked(null, 0, false);
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.state == ActivityState.STOPPING || r.state == ActivityState.STOPPED || r.state == ActivityState.PAUSED || r.state == ActivityState.PAUSING) {
                    r.setSleeping(true);
                }
            }
        }
    }

    public final Bitmap screenshotActivitiesLocked(ActivityRecord who) {
        if (ActivityManagerDebugConfig.DEBUG_SCREENSHOTS) {
            Slog.d(TAG_SCREENSHOTS, "screenshotActivitiesLocked: " + who);
        }
        if (who.noDisplay) {
            if (ActivityManagerDebugConfig.DEBUG_SCREENSHOTS) {
                Slog.d(TAG_SCREENSHOTS, "\tNo display");
            }
            return null;
        }
        if (isHomeStack()) {
            if (ActivityManagerDebugConfig.DEBUG_SCREENSHOTS) {
                Slog.d(TAG_SCREENSHOTS, "\tHome stack");
            }
            return null;
        }
        int w = this.mService.mThumbnailWidth;
        int h = this.mService.mThumbnailHeight;
        if (w > 0) {
            if (ActivityManagerDebugConfig.DEBUG_SCREENSHOTS) {
                Slog.d(TAG_SCREENSHOTS, "\tTaking screenshot");
            }
            float scale = this.mService.mFullscreenThumbnailScale;
            return this.mWindowManager.screenshotApplications(who.appToken, 0, -1, -1, scale);
        }
        Slog.e(TAG, "Invalid thumbnail dimensions: " + w + "x" + h);
        return null;
    }

    final boolean startPausingLocked(boolean userLeaving, boolean uiSleeping, boolean resuming, boolean dontWait) {
        if (this.mPausingActivity != null) {
            Slog.e(TAG, "Going to pause when pause is already pending for " + this.mPausingActivity + " state=" + this.mPausingActivity.state, new RuntimeException("here").fillInStackTrace());
            if (!this.mService.isSleepingLocked()) {
                completePauseLocked(false);
            }
        }
        ActivityRecord prev = this.mResumedActivity;
        if (prev == null) {
            if (resuming) {
                return false;
            }
            Slog.e(TAG, "Trying to pause when nothing is resumed", new RuntimeException("here").fillInStackTrace());
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            return false;
        }
        if (this.mActivityContainer.mParentActivity == null) {
            this.mStackSupervisor.pauseChildStacks(prev, userLeaving, uiSleeping, resuming, dontWait);
        }
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to PAUSING: " + prev);
        } else if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Start pausing: " + prev);
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
            prev.mUpdateTaskThumbnailWhenHidden = true;
        }
        stopFullyDrawnTraceIfNeeded();
        this.mService.updateCpuStats();
        if (prev.app == null || prev.app.thread == null) {
            this.mPausingActivity = null;
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Enqueueing pending pause: " + prev);
            }
            try {
                EventLog.writeEvent(EventLogTags.AM_PAUSE_ACTIVITY, Integer.valueOf(prev.userId), Integer.valueOf(System.identityHashCode(prev)), prev.shortComponentName);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "ACT-AM_PAUSE_ACTIVITY " + prev);
                }
                this.mService.updateUsageStats(prev, false);
                prev.app.thread.schedulePauseActivity(prev.appToken, prev.finishing, userLeaving, prev.configChangeFlags, dontWait);
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown during pause", e);
                this.mPausingActivity = null;
                this.mLastPausedActivity = null;
                this.mLastNoHistoryActivity = null;
            }
        }
        if (!uiSleeping && !this.mService.isSleepingOrShuttingDownLocked()) {
            this.mStackSupervisor.acquireLaunchWakelock();
        }
        if (this.mPausingActivity == null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Activity not running, resuming next.");
            }
            if (resuming) {
                return false;
            }
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            return false;
        }
        if (!uiSleeping) {
            prev.pauseKeyDispatchingLocked();
        } else if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Key dispatch not paused for screen off");
        }
        if (dontWait) {
            completePauseLocked(false);
            return false;
        }
        Message msg = this.mHandler.obtainMessage(101);
        msg.obj = prev;
        prev.pauseTime = SystemClock.uptimeMillis();
        this.mHandler.sendMessageDelayed(msg, 500L);
        if (!ActivityManagerDebugConfig.DEBUG_PAUSE) {
            return true;
        }
        Slog.v(TAG_PAUSE, "Waiting for pause to complete...");
        return true;
    }

    final void activityPausedLocked(IBinder token, boolean timeout) {
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Activity paused: token=" + token + ", timeout=" + timeout);
        }
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-paused: token=" + token + ", timeout=" + timeout);
        }
        ActivityRecord r = isInStackLocked(token);
        if (r != null) {
            this.mHandler.removeMessages(101, r);
            if (this.mPausingActivity == r) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to PAUSED: " + r + (timeout ? " (due to timeout)" : " (pause complete)"));
                }
                completePauseLocked(true);
                return;
            }
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(r.userId);
            objArr[1] = Integer.valueOf(System.identityHashCode(r));
            objArr[2] = r.shortComponentName;
            objArr[3] = this.mPausingActivity != null ? this.mPausingActivity.shortComponentName : "(none)";
            EventLog.writeEvent(EventLogTags.AM_FAILED_TO_PAUSE, objArr);
            if (r.state == ActivityState.PAUSING) {
                r.state = ActivityState.PAUSED;
                ProcessRecord appProc = r.app;
                if (appProc != null) {
                    AMEventHookData.AfterActivityPaused eventData = AMEventHookData.AfterActivityPaused.createInstance();
                    eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityPaused, eventData);
                }
                if (r.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG, "Executing finish of failed to pause activity: " + r);
                    }
                    finishCurrentActivityLocked(r, 2, false);
                }
            }
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "ACT-AM_FAILED_TO_PAUSE " + r + " PausingActivity:" + this.mPausingActivity);
            }
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    final void activityResumedLocked(IBinder token) {
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            Slog.i(TAG_STATES, "Resumed activity; dropping state of: " + r);
        }
        r.icicle = null;
        r.haveState = false;
    }

    final void activityStoppedLocked(ActivityRecord r, Bundle icicle, PersistableBundle persistentState, CharSequence description) {
        if (r.state != ActivityState.STOPPING) {
            Slog.i(TAG, "Activity reported stop, but no longer stopping: " + r);
            this.mHandler.removeMessages(104, r);
            return;
        }
        if (persistentState != null) {
            r.persistentState = persistentState;
            this.mService.notifyTaskPersisterLocked(r.task, false);
        }
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            Slog.i(TAG_SAVED_STATE, "Saving icicle of " + r + ": " + icicle);
        }
        if (icicle != null) {
            r.icicle = icicle;
            r.haveState = true;
            r.launchCount = 0;
            r.updateThumbnailLocked(null, description);
        }
        if (r.stopped) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to STOPPED: " + r + " (stop complete)");
        }
        this.mHandler.removeMessages(104, r);
        r.stopped = true;
        r.state = ActivityState.STOPPED;
        ProcessRecord appProc = r.app;
        if (appProc != null) {
            AMEventHookData.AfterActivityStopped eventData = AMEventHookData.AfterActivityStopped.createInstance();
            eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityStopped, eventData);
        }
        this.mWindowManager.notifyAppStopped(r.appToken, true);
        if (getVisibleBehindActivity() == r) {
            this.mStackSupervisor.requestVisibleBehindLocked(r, false);
        }
        if (r.finishing) {
            r.clearOptionsLocked();
        } else if (r.deferRelaunchUntilPaused) {
            destroyActivityLocked(r, true, "stop-config");
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        } else {
            this.mStackSupervisor.updatePreviousProcessLocked(r);
        }
    }

    private void completePauseLocked(boolean resumeNext) {
        ActivityRecord prev = this.mPausingActivity;
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Complete pause: " + prev);
        }
        if (prev != null) {
            boolean wasStopping = prev.state == ActivityState.STOPPING;
            prev.state = ActivityState.PAUSED;
            ProcessRecord appProc = prev.app;
            if (appProc != null) {
                AMEventHookData.AfterActivityPaused eventData = AMEventHookData.AfterActivityPaused.createInstance();
                eventData.set(new Object[]{Integer.valueOf(appProc.pid), prev.info.name, prev.info.packageName});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityPaused, eventData);
            }
            if (prev.finishing) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Executing finish of activity: " + prev);
                }
                prev = finishCurrentActivityLocked(prev, 2, false);
            } else if (prev.app != null) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Enqueue pending stop if needed: " + prev + " wasStopping=" + wasStopping + " visible=" + prev.visible);
                }
                if (this.mStackSupervisor.mWaitingVisibleActivities.remove(prev) && (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE)) {
                    Slog.v(TAG_PAUSE, "Complete pause, no longer waiting: " + prev);
                }
                if (prev.deferRelaunchUntilPaused) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Re-launching after pause: " + prev);
                    }
                    relaunchActivityLocked(prev, prev.configChangeFlags, false, prev.preserveWindowOnDeferredRelaunch);
                } else if (wasStopping) {
                    prev.state = ActivityState.STOPPING;
                } else if ((!prev.visible && !hasVisibleBehindActivity()) || this.mService.isSleepingOrShuttingDownLocked()) {
                    addToStopping(prev, true);
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "App died during pause, not stopping: " + prev);
                }
                prev = null;
            }
            if (prev != null) {
                prev.stopFreezingScreenLocked(true);
            }
            this.mPausingActivity = null;
        }
        if (resumeNext) {
            ActivityStack topStack = this.mStackSupervisor.getFocusedStack();
            if (this.mService.isSleepingOrShuttingDownLocked()) {
                this.mStackSupervisor.checkReadyForSleepLocked();
                ActivityRecord top = topStack.topRunningActivityLocked();
                if (top == null || (prev != null && top != prev)) {
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            } else {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked(topStack, prev, null);
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
        if (this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause) {
            this.mService.notifyTaskStackChangedLocked();
            this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = false;
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private void addToStopping(ActivityRecord r, boolean immediate) {
        boolean forceIdle;
        if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
            this.mStackSupervisor.mStoppingActivities.add(r);
        }
        if (this.mStackSupervisor.mStoppingActivities.size() > 3) {
            forceIdle = true;
        } else {
            forceIdle = r.frontOfTask && this.mTaskHistory.size() <= 1;
        }
        if (immediate || forceIdle) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Scheduling idle now: forceIdle=" + forceIdle + "immediate=" + immediate);
            }
            this.mStackSupervisor.scheduleIdleLocked();
            return;
        }
        this.mStackSupervisor.checkReadyForSleepLocked();
    }

    private void completeResumeLocked(ActivityRecord next) {
        ProcessRecord app;
        next.visible = true;
        next.idle = false;
        next.results = null;
        next.newIntents = null;
        next.stopped = false;
        if (next.isHomeActivity() && (app = next.task.mActivities.get(0).app) != null && app != this.mService.mHomeProcess) {
            this.mService.mHomeProcess = app;
        }
        if (next.nowVisible) {
            this.mStackSupervisor.reportActivityVisibleLocked(next);
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
        if (getVisibleBehindActivity() != next) {
            return;
        }
        setVisibleBehindActivity(null);
    }

    private void setVisible(ActivityRecord r, boolean visible) {
        r.visible = visible;
        if (!visible && r.mUpdateTaskThumbnailWhenHidden) {
            r.updateThumbnailLocked(r.task.stack.screenshotActivitiesLocked(r), null);
            r.mUpdateTaskThumbnailWhenHidden = false;
        }
        this.mWindowManager.setAppVisibility(r.appToken, visible);
        ArrayList<ActivityStackSupervisor.ActivityContainer> containers = r.mChildContainers;
        for (int containerNdx = containers.size() - 1; containerNdx >= 0; containerNdx--) {
            ActivityStackSupervisor.ActivityContainer container = containers.get(containerNdx);
            container.setVisible(visible);
        }
        this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
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
            ActivityStack historyStack = this.mStacks.get(stackNdx);
            ArrayList<TaskRecord> tasks = historyStack.mTaskHistory;
            int numTasks = tasks.size();
            while (taskNdx < numTasks) {
                TaskRecord currentTask = tasks.get(taskNdx);
                ArrayList<ActivityRecord> activities = currentTask.mActivities;
                int numActivities = activities.size();
                while (activityNdx < numActivities) {
                    ActivityRecord activity = activities.get(activityNdx);
                    if (!activity.finishing) {
                        if (historyStack.mFullscreen && currentTask.mFullscreen && activity.fullscreen) {
                            return null;
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

    ActivityStack getNextFocusableStackLocked() {
        ArrayList<ActivityStack> stacks = this.mStacks;
        ActivityRecord parent = this.mActivityContainer.mParentActivity;
        if (parent != null) {
            stacks = parent.task.stack.mStacks;
        }
        if (stacks != null) {
            for (int i = stacks.size() - 1; i >= 0; i--) {
                ActivityStack stack = stacks.get(i);
                if (stack != this && stack.isFocusable() && stack.getStackVisibilityLocked(null) != 0) {
                    return stack;
                }
            }
        }
        return null;
    }

    private boolean hasFullscreenTask() {
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            TaskRecord task = this.mTaskHistory.get(i);
            if (task.mFullscreen) {
                return true;
            }
        }
        return false;
    }

    private boolean isStackTranslucent(ActivityRecord starting, int stackBehindId) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ArrayList<ActivityRecord> activities = task.mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing && (r.visible || r == starting)) {
                    if (r.fullscreen) {
                        return false;
                    }
                    if (!isHomeStack() && r.frontOfTask && task.isOverHomeStack() && stackBehindId != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    int getStackVisibilityLocked(ActivityRecord starting) {
        if (!isAttached()) {
            return 0;
        }
        if (this.mStackSupervisor.isFrontStack(this) || this.mStackSupervisor.isFocusedStack(this)) {
            return 1;
        }
        int stackIndex = this.mStacks.indexOf(this);
        if (stackIndex == this.mStacks.size() - 1) {
            Slog.wtf(TAG, "Stack=" + this + " isn't front stack but is at the top of the stack list");
            return 0;
        }
        boolean isLockscreenShown = this.mService.mLockScreenShown == 2;
        if (isLockscreenShown && !ActivityManager.StackId.isAllowedOverLockscreen(this.mStackId)) {
            return 0;
        }
        ActivityStack focusedStack = this.mStackSupervisor.getFocusedStack();
        int focusedStackId = focusedStack.mStackId;
        if (this.mStackId == 1 && hasVisibleBehindActivity() && focusedStackId == 0 && !focusedStack.topActivity().fullscreen) {
            return 2;
        }
        if (this.mStackId == 3) {
            ActivityRecord r = focusedStack.topRunningActivityLocked();
            TaskRecord task = r != null ? r.task : null;
            return (task == null || task.canGoInDockedStack() || task.isHomeTask()) ? 1 : 0;
        }
        int stackBehindFocusedIndex = this.mStacks.indexOf(focusedStack) - 1;
        while (stackBehindFocusedIndex >= 0 && this.mStacks.get(stackBehindFocusedIndex).topRunningActivityLocked() == null) {
            stackBehindFocusedIndex--;
        }
        if ((focusedStackId == 3 || focusedStackId == 4) && stackIndex == stackBehindFocusedIndex) {
            return 1;
        }
        int stackBehindFocusedId = stackBehindFocusedIndex >= 0 ? this.mStacks.get(stackBehindFocusedIndex).mStackId : -1;
        if (focusedStackId == 1 && focusedStack.isStackTranslucent(starting, stackBehindFocusedId)) {
            if (stackIndex == stackBehindFocusedIndex) {
                return 1;
            }
            if (stackBehindFocusedIndex >= 0 && ((stackBehindFocusedId == 3 || stackBehindFocusedId == 4) && stackIndex == stackBehindFocusedIndex - 1)) {
                return 1;
            }
        }
        if (ActivityManager.StackId.isStaticStack(this.mStackId)) {
            return 0;
        }
        for (int i = stackIndex + 1; i < this.mStacks.size(); i++) {
            ActivityStack stack = this.mStacks.get(i);
            if ((stack.mFullscreen || stack.hasFullscreenTask()) && (!ActivityManager.StackId.isDynamicStacksVisibleBehindAllowed(stack.mStackId) || !stack.isStackTranslucent(starting, -1))) {
                return 0;
            }
        }
        return 1;
    }

    final int rankTaskLayers(int baseLayer) {
        int layer;
        int taskNdx = this.mTaskHistory.size() - 1;
        int layer2 = 0;
        while (taskNdx >= 0) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ActivityRecord r = task.topRunningActivityLocked();
            if (r == null || r.finishing || !r.visible) {
                task.mLayerRank = -1;
                layer = layer2;
            } else {
                layer = layer2 + 1;
                task.mLayerRank = baseLayer + layer2;
            }
            taskNdx--;
            layer2 = layer;
        }
        return layer2;
    }

    final void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        ActivityRecord top = topRunningActivityLocked();
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "ensureActivitiesVisible behind " + top + " configChanges=0x" + Integer.toHexString(configChanges));
        }
        if (top != null) {
            checkTranslucentActivityWaiting(top);
        }
        boolean aboveTop = top != null;
        int stackVisibility = getStackVisibilityLocked(starting);
        boolean stackInvisible = stackVisibility != 1;
        boolean stackVisibleBehind = stackVisibility == 2;
        boolean behindFullscreenActivity = stackInvisible;
        boolean resumeNextActivity = this.mStackSupervisor.isFocusedStack(this) && isInStackLocked(starting) == null;
        boolean behindTranslucentActivity = false;
        ActivityRecord visibleBehind = getVisibleBehindActivity();
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ArrayList<ActivityRecord> activities = task.mActivities;
            int activityNdx = activities.size() - 1;
            while (activityNdx >= 0) {
                ActivityRecord r = activities.get(activityNdx);
                if (!r.finishing) {
                    boolean isTop = r == top;
                    if (!aboveTop || isTop) {
                        aboveTop = false;
                        if (shouldBeVisible(r, behindTranslucentActivity, stackVisibleBehind, visibleBehind, behindFullscreenActivity)) {
                            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.v(TAG_VISIBILITY, "Make visible? " + r + " finishing=" + r.finishing + " state=" + r.state);
                            }
                            if (r != starting) {
                                ensureActivityConfigurationLocked(r, 0, preserveWindows);
                            }
                            if (r.app == null || r.app.thread == null) {
                                if (makeVisibleAndRestartIfNeeded(starting, configChanges, isTop, resumeNextActivity, r)) {
                                    if (activityNdx >= activities.size()) {
                                        activityNdx = activities.size() - 1;
                                    } else {
                                        resumeNextActivity = false;
                                    }
                                }
                            } else if (r.visible) {
                                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                    Slog.v(TAG_VISIBILITY, "Skipping: already visible at " + r);
                                }
                                if (handleAlreadyVisible(r)) {
                                    resumeNextActivity = false;
                                }
                            } else {
                                makeVisibleIfNeeded(starting, r);
                            }
                            configChanges |= r.configChangeFlags;
                            behindFullscreenActivity = updateBehindFullscreen(stackInvisible, behindFullscreenActivity, task, r);
                            if (behindFullscreenActivity && !r.fullscreen) {
                                behindTranslucentActivity = true;
                            }
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                Slog.v(TAG_VISIBILITY, "Make invisible? " + r + " finishing=" + r.finishing + " state=" + r.state + " stackInvisible=" + stackInvisible + " behindFullscreenActivity=" + behindFullscreenActivity + " mLaunchTaskBehind=" + r.mLaunchTaskBehind);
                            }
                            makeInvisible(r, visibleBehind);
                        }
                    }
                } else if (r.mUpdateTaskThumbnailWhenHidden) {
                    r.updateThumbnailLocked(screenshotActivitiesLocked(r), null);
                    r.mUpdateTaskThumbnailWhenHidden = false;
                }
                activityNdx--;
            }
            if (this.mStackId == 2) {
                behindFullscreenActivity = stackVisibility == 0;
            } else if (this.mStackId == 0) {
                if (task.isHomeTask()) {
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "Home task: at " + task + " stackInvisible=" + stackInvisible + " behindFullscreenActivity=" + behindFullscreenActivity);
                    }
                    behindFullscreenActivity = true;
                } else if (task.isRecentsTask() && task.getTaskToReturnTo() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "Recents task returning to app: at " + task + " stackInvisible=" + stackInvisible + " behindFullscreenActivity=" + behindFullscreenActivity);
                    }
                    behindFullscreenActivity = true;
                }
            }
        }
        if (this.mTranslucentActivityWaiting == null || !this.mUndrawnActivitiesBelowTopTranslucent.isEmpty()) {
            return;
        }
        notifyActivityDrawnLocked(null);
    }

    private boolean shouldBeVisible(ActivityRecord r, boolean behindTranslucentActivity, boolean stackVisibleBehind, ActivityRecord visibleBehind, boolean behindFullscreenActivity) {
        if (!okToShowLocked(r)) {
            return false;
        }
        boolean activityVisibleBehind = (behindTranslucentActivity || stackVisibleBehind) && visibleBehind == r;
        boolean z = (!behindFullscreenActivity || r.mLaunchTaskBehind) ? true : activityVisibleBehind;
        if (this.mService.mSupportsLeanbackOnly && z && r.isRecentsActivity()) {
            if (this.mStackSupervisor.getStack(3) != null) {
                return true;
            }
            return this.mStackSupervisor.isFocusedStack(this);
        }
        return z;
    }

    private void checkTranslucentActivityWaiting(ActivityRecord top) {
        if (this.mTranslucentActivityWaiting == top) {
            return;
        }
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        if (this.mTranslucentActivityWaiting != null) {
            notifyActivityDrawnLocked(null);
            this.mTranslucentActivityWaiting = null;
        }
        this.mHandler.removeMessages(106);
    }

    private boolean makeVisibleAndRestartIfNeeded(ActivityRecord starting, int configChanges, boolean isTop, boolean andResume, ActivityRecord r) {
        if (skipStartActivityIfNeeded()) {
            return false;
        }
        if (isTop || !r.visible) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Start and freeze screen for " + r);
            }
            if (r != starting) {
                r.startFreezingScreenLocked(r.app, configChanges);
            }
            if (!r.visible || r.mLaunchTaskBehind) {
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG_VISIBILITY, "Starting and making visible: " + r);
                }
                setVisible(r, true);
            }
            if (r != starting) {
                this.mStackSupervisor.startSpecificActivityLocked(r, andResume, false);
                return true;
            }
        }
        return false;
    }

    private void makeInvisible(ActivityRecord r, ActivityRecord visibleBehind) {
        if (!r.visible) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Already invisible: " + r);
            }
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making invisible: " + r + " " + r.state);
        }
        try {
            setVisible(r, false);
            switch (m925x775af271()[r.state.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                    if (visibleBehind == r) {
                        releaseBackgroundResources(r);
                    } else {
                        addToStopping(r, true);
                    }
                    break;
                case 5:
                case 6:
                    if (r.app != null && r.app.thread != null) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG_VISIBILITY, "Scheduling invisibility: " + r);
                        }
                        r.app.thread.scheduleWindowVisibility(r.appToken, false);
                        break;
                    }
                    break;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making hidden: " + r.intent.getComponent(), e);
        }
    }

    private boolean updateBehindFullscreen(boolean stackInvisible, boolean behindFullscreenActivity, TaskRecord task, ActivityRecord r) {
        if (r.fullscreen) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Fullscreen: at " + r + " stackInvisible=" + stackInvisible + " behindFullscreenActivity=" + behindFullscreenActivity);
            }
            return true;
        }
        if (!isHomeStack() && r.frontOfTask && task.isOverHomeStack()) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Showing home: at " + r + " stackInvisible=" + stackInvisible + " behindFullscreenActivity=" + behindFullscreenActivity);
            }
            return true;
        }
        return behindFullscreenActivity;
    }

    private void makeVisibleIfNeeded(ActivityRecord starting, ActivityRecord r) {
        if (r.state == ActivityState.RESUMED || r == starting) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.d(TAG_VISIBILITY, "Not making visible, r=" + r + " state=" + r.state + " starting=" + starting);
                return;
            }
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making visible and scheduling visibility: " + r);
        }
        try {
            if (this.mTranslucentActivityWaiting != null) {
                r.updateOptionsLocked(r.returningOptions);
                this.mUndrawnActivitiesBelowTopTranslucent.add(r);
            }
            setVisible(r, true);
            r.sleeping = false;
            r.app.pendingUiClean = true;
            r.app.thread.scheduleWindowVisibility(r.appToken, true);
            this.mStackSupervisor.mStoppingActivities.remove(r);
            this.mStackSupervisor.mGoingToSleepActivities.remove(r);
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making visibile: " + r.intent.getComponent(), e);
        }
        handleAlreadyVisible(r);
    }

    private boolean handleAlreadyVisible(ActivityRecord r) {
        r.stopFreezingScreenLocked(false);
        try {
            if (r.returningOptions != null) {
                r.app.thread.scheduleOnNewActivityOptions(r.appToken, r.returningOptions);
            }
        } catch (RemoteException e) {
        }
        return r.state == ActivityState.RESUMED;
    }

    void convertActivityToTranslucent(ActivityRecord r) {
        this.mTranslucentActivityWaiting = r;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.sendEmptyMessageDelayed(106, TRANSLUCENT_CONVERSION_TIMEOUT);
    }

    void clearOtherAppTimeTrackers(AppTimeTracker except) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            ArrayList<ActivityRecord> activities = task.mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.appTimeTracker != except) {
                    r.appTimeTracker = null;
                }
            }
        }
    }

    void notifyActivityDrawnLocked(ActivityRecord r) {
        this.mActivityContainer.setDrawn();
        if (r != null && (!this.mUndrawnActivitiesBelowTopTranslucent.remove(r) || !this.mUndrawnActivitiesBelowTopTranslucent.isEmpty())) {
            return;
        }
        ActivityRecord waitingActivity = this.mTranslucentActivityWaiting;
        this.mTranslucentActivityWaiting = null;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.removeMessages(106);
        if (waitingActivity == null) {
            return;
        }
        this.mWindowManager.setWindowOpaque(waitingActivity.appToken, false);
        if (waitingActivity.app == null || waitingActivity.app.thread == null) {
            return;
        }
        try {
            waitingActivity.app.thread.scheduleTranslucentConversionComplete(waitingActivity.appToken, r != null);
        } catch (RemoteException e) {
        }
    }

    void cancelInitializingActivities() {
        boolean z;
        ActivityRecord topActivity = topRunningActivityLocked();
        boolean aboveTop = true;
        boolean behindFullscreenActivity = false;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (aboveTop) {
                    if (r == topActivity) {
                        aboveTop = false;
                    }
                    z = r.fullscreen;
                } else {
                    if (r.state == ActivityState.INITIALIZING && r.mStartingWindowState == 1 && behindFullscreenActivity) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.w(TAG_VISIBILITY, "Found orphaned starting window " + r);
                        }
                        r.mStartingWindowState = 2;
                        this.mWindowManager.removeAppStartingWindow(r.appToken);
                    }
                    z = r.fullscreen;
                }
                behindFullscreenActivity |= z;
            }
        }
    }

    boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
        if (this.mStackSupervisor.inResumeTopActivity) {
            return false;
        }
        try {
            this.mStackSupervisor.inResumeTopActivity = true;
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

    private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) throws Throwable {
        int size;
        ArrayList<IAWSProcessRecord> runningProcRecords;
        if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
            this.mService.logLockScreen("");
        }
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        ActivityRecord parent = this.mActivityContainer.mParentActivity;
        if ((parent != null && parent.state != ActivityState.RESUMED) || !this.mActivityContainer.isAttachedLocked()) {
            return false;
        }
        this.mStackSupervisor.cancelInitializingActivities();
        ActivityRecord next = topRunningActivityLocked();
        boolean userLeaving = this.mStackSupervisor.mUserLeaving;
        this.mStackSupervisor.mUserLeaving = false;
        TaskRecord taskRecord = prev != null ? prev.task : null;
        if (next == null) {
            int returnTaskType = (taskRecord == null || !taskRecord.isOverHomeStack()) ? 1 : taskRecord.getTaskToReturnTo();
            if (!this.mFullscreen && adjustFocusToNextFocusableStackLocked(returnTaskType, "noMoreActivities")) {
                return this.mStackSupervisor.resumeFocusedStackTopActivityLocked(this.mStackSupervisor.getFocusedStack(), prev, null);
            }
            ActivityOptions.abort(options);
            AMEventHookResult eventResult = this.mService.getAMEventHook().hook(AMEventHook.Event.AM_BeforeGoHomeWhenNoActivities, AMEventHookData.BeforeGoHomeWhenNoActivities.createInstance());
            if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_SkipHomeActivityLaunching)) {
                Slog.v(TAG, "Skip to resume home activity!!");
                return false;
            }
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: No more activities go home");
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            if (isOnHomeDisplay()) {
                return this.mStackSupervisor.resumeHomeStackTask(returnTaskType, prev, "noMoreActivities");
            }
            return false;
        }
        next.delayedResume = false;
        if (this.mResumedActivity == next && next.state == ActivityState.RESUMED && this.mStackSupervisor.allResumedActivitiesComplete()) {
            this.mWindowManager.executeAppTransition();
            this.mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed " + next);
            }
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return false;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        TaskRecord nextTask = next.task;
        if (taskRecord != null && taskRecord.stack == this && taskRecord.isOverHomeStack() && prev.finishing && prev.frontOfTask) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            if (taskRecord == nextTask) {
                taskRecord.setFrontOfTask();
            } else if (taskRecord != topTask()) {
                int taskNdx = this.mTaskHistory.indexOf(taskRecord) + 1;
                this.mTaskHistory.get(taskNdx).setTaskToReturnTo(1);
            } else {
                if (!isOnHomeDisplay()) {
                    return false;
                }
                if (!isHomeStack()) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "resumeTopActivityLocked: Launching home next");
                    }
                    int returnTaskType2 = (taskRecord == null || !taskRecord.isOverHomeStack()) ? 1 : taskRecord.getTaskToReturnTo();
                    if (isOnHomeDisplay()) {
                        return this.mStackSupervisor.resumeHomeStackTask(returnTaskType2, prev, "prevFinished");
                    }
                    return false;
                }
            }
        }
        if (this.mService.isSleepingOrShuttingDownLocked() && this.mLastPausedActivity == next && this.mStackSupervisor.allPausedActivitiesComplete()) {
            this.mWindowManager.executeAppTransition();
            this.mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Going to sleep and all paused");
            }
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return false;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        if (!this.mService.mUserController.hasStartedUserState(next.userId)) {
            Slog.w(TAG, "Skipping resume of top activity " + next + ": user " + next.userId + " is stopped");
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return false;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        this.mStackSupervisor.mStoppingActivities.remove(next);
        this.mStackSupervisor.mGoingToSleepActivities.remove(next);
        next.sleeping = false;
        this.mStackSupervisor.mWaitingVisibleActivities.remove(next);
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "Resuming " + next);
        }
        if (!this.mStackSupervisor.allPausedActivitiesComplete()) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_PAUSE, "resumeTopActivityLocked: Skip resume: some activity pausing.");
            }
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return false;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        this.mStackSupervisor.setLaunchSource(next.info.applicationInfo.uid);
        boolean dontWaitForPause = (next.info.flags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0;
        boolean pausing = this.mStackSupervisor.pauseBackStacks(userLeaving, true, dontWaitForPause);
        if (this.mResumedActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Pausing " + this.mResumedActivity);
            }
            pausing |= startPausingLocked(userLeaving, false, true, dontWaitForPause);
        }
        if (next.info.packageName != this.mStackSupervisor.mLastResumedActivity.packageName || next.info.name != this.mStackSupervisor.mLastResumedActivity.activityName) {
            AMEventHookData.BeforeActivitySwitch eventData = AMEventHookData.BeforeActivitySwitch.createInstance();
            ArrayList<String> taskPkgList = null;
            if (("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) && !pausing && nextTask != null) {
                taskPkgList = new ArrayList<>();
                for (int i = 0; i < nextTask.mActivities.size(); i++) {
                    ActivityRecord taskActivity = nextTask.mActivities.get(i);
                    if (taskActivity.packageName != null) {
                        taskPkgList.add(taskActivity.packageName);
                    }
                }
            }
            int waitProcessPid = -1;
            ArrayList<IAWSProcessRecord> runningProcRecords2 = null;
            if (SystemProperties.get("ro.mtk_aws_support").equals("1")) {
                if (next.resultTo != null && next.resultTo.app != null) {
                    waitProcessPid = next.resultTo.app.pid;
                }
                synchronized (this.mService.mPidsSelfLocked) {
                    try {
                        size = this.mService.mPidsSelfLocked.size();
                    } catch (Throwable th) {
                        th = th;
                    }
                    if (size != 0) {
                        int i2 = 0;
                        while (true) {
                            runningProcRecords = runningProcRecords2;
                            if (i2 >= size) {
                                break;
                            }
                            if (runningProcRecords == null) {
                                try {
                                    runningProcRecords2 = new ArrayList<>();
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } else {
                                runningProcRecords2 = runningProcRecords;
                            }
                            ProcessRecord proc = this.mService.mPidsSelfLocked.valueAt(i2);
                            if (proc != null) {
                                IAWSProcessRecord pr = ActivityManagerService.convertProcessRecord(proc);
                                runningProcRecords2.add(pr);
                            }
                            i2++;
                            throw th;
                        }
                        runningProcRecords2 = runningProcRecords;
                    }
                }
            }
            eventData.set(new Object[]{this.mStackSupervisor.mLastResumedActivity.activityName, next.info.name, this.mStackSupervisor.mLastResumedActivity.packageName, next.info.packageName, Integer.valueOf(this.mStackSupervisor.mLastResumedActivity.activityType), Integer.valueOf(next.mActivityType), Boolean.valueOf(pausing), taskPkgList, Integer.valueOf(waitProcessPid), runningProcRecords2});
            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_BeforeActivitySwitch, eventData);
            if (!pausing) {
                this.mStackSupervisor.mLastResumedActivity.packageName = next.info.packageName;
                this.mStackSupervisor.mLastResumedActivity.activityName = next.info.name;
                this.mStackSupervisor.mLastResumedActivity.activityType = next.mActivityType;
            }
        }
        if (pausing) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "resumeTopActivityLocked: Skip resume: need to start pausing");
            }
            if (next.app != null && next.app.thread != null) {
                this.mService.updateLruProcessLocked(next.app, true, null);
            }
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return true;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return true;
        }
        if (this.mResumedActivity == next && next.state == ActivityState.RESUMED && this.mStackSupervisor.allResumedActivitiesComplete()) {
            this.mWindowManager.executeAppTransition();
            this.mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed (dontWaitForPause) " + next);
            }
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return true;
            }
            this.mStackSupervisor.validateTopActivitiesLocked();
            return true;
        }
        if (this.mService.isSleepingLocked() && this.mLastNoHistoryActivity != null && !this.mLastNoHistoryActivity.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "no-history finish of " + this.mLastNoHistoryActivity + " on new resume");
            }
            requestFinishActivityLocked(this.mLastNoHistoryActivity.appToken, 0, null, "resume-no-history", false);
            this.mLastNoHistoryActivity = null;
        }
        if (prev != null && prev != next) {
            if (!this.mStackSupervisor.mWaitingVisibleActivities.contains(prev) && next != null && !next.nowVisible) {
                this.mStackSupervisor.mWaitingVisibleActivities.add(prev);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Resuming top, waiting visible to hide: " + prev);
                }
            } else if (prev.finishing) {
                this.mWindowManager.setAppVisibility(prev.appToken, false);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Not waiting for visible to hide: " + prev + ", waitingVisible=" + this.mStackSupervisor.mWaitingVisibleActivities.contains(prev) + ", nowVisible=" + next.nowVisible);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Previous already visible but still waiting to hide: " + prev + ", waitingVisible=" + this.mStackSupervisor.mWaitingVisibleActivities.contains(prev) + ", nowVisible=" + next.nowVisible);
            }
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(next.packageName, false, next.userId);
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Failed trying to unstop package " + next.packageName + ": " + e2);
        }
        if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
            AMEventHookData.PackageStoppedStatusChanged eventData1 = AMEventHookData.PackageStoppedStatusChanged.createInstance();
            eventData1.set(new Object[]{next.packageName, 0, "resumeTopActivityInnerLocked"});
            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData1);
        }
        boolean anim = true;
        if (prev == null) {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: no previous");
            }
            if (this.mNoAnimActivities.contains(next)) {
                anim = false;
                this.mWindowManager.prepareAppTransition(0, false);
            } else {
                this.mWindowManager.prepareAppTransition(6, false);
            }
        } else if (prev.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare close transition: prev=" + prev);
            }
            if (this.mNoAnimActivities.contains(prev)) {
                anim = false;
                this.mWindowManager.prepareAppTransition(0, false);
            } else {
                this.mWindowManager.prepareAppTransition(prev.task == next.task ? 7 : 9, false);
            }
            this.mWindowManager.setAppVisibility(prev.appToken, false);
        } else {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: prev=" + prev);
            }
            if (this.mNoAnimActivities.contains(next)) {
                anim = false;
                this.mWindowManager.prepareAppTransition(0, false);
            } else {
                this.mWindowManager.prepareAppTransition(prev.task == next.task ? 6 : next.mLaunchTaskBehind ? 16 : 8, false);
            }
        }
        if (anim) {
            ActivityOptions opts = next.getOptionsForTargetActivityLocked();
            resumeAnimOptions = opts != null ? opts.toBundle() : null;
            next.applyOptionsLocked();
        } else {
            next.clearOptionsLocked();
        }
        ActivityStack lastStack = this.mStackSupervisor.getLastStack();
        if (next.app == null || next.app.thread == null) {
            if (next.hasBeenLaunched) {
                next.showStartingWindow(null, true);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Restarting: " + next);
                }
            } else {
                next.hasBeenLaunched = true;
            }
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Restarting " + next);
            }
            this.mStackSupervisor.startSpecificActivityLocked(next, true, true);
        } else {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Resume running: " + next + " stopped=" + next.stopped + " visible=" + next.visible);
            }
            boolean lastActivityTranslucent = lastStack != null ? lastStack.mFullscreen ? (lastStack.mLastPausedActivity == null || lastStack.mLastPausedActivity.fullscreen) ? false : true : true : false;
            if (!next.visible || next.stopped || lastActivityTranslucent) {
                this.mWindowManager.setAppVisibility(next.appToken, true);
            }
            next.startLaunchTickingLocked();
            ActivityRecord activityRecord = lastStack == null ? null : lastStack.mResumedActivity;
            ActivityState lastState = next.state;
            this.mService.updateCpuStats();
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to RESUMED: " + next + " (in existing)");
            }
            next.state = ActivityState.RESUMED;
            ProcessRecord appProc = next.app;
            if (appProc != null) {
                AMEventHookData.AfterActivityResumed aarEventData = AMEventHookData.AfterActivityResumed.createInstance();
                aarEventData.set(new Object[]{Integer.valueOf(appProc.pid), next.info.name, next.info.packageName, Integer.valueOf(next.mActivityType)});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityResumed, aarEventData);
            }
            this.mResumedActivity = next;
            next.task.touchActiveTime();
            this.mRecentTasks.addLocked(next.task);
            this.mService.updateLruProcessLocked(next.app, true, null);
            updateLRUListLocked(next);
            this.mService.updateOomAdjLocked();
            boolean notUpdated = true;
            if (this.mStackSupervisor.isFocusedStack(this)) {
                Configuration config = this.mWindowManager.updateOrientationFromAppTokens(this.mService.mConfiguration, next.mayFreezeScreenLocked(next.app) ? next.appToken : null);
                if (config != null) {
                    next.frozenBeforeDestroy = true;
                }
                notUpdated = !this.mService.updateConfigurationLocked(config, next, false);
            }
            if (notUpdated) {
                ActivityRecord nextNext = topRunningActivityLocked();
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.i(TAG_STATES, "Activity config changed during resume: " + next + ", new next: " + nextNext);
                }
                if (nextNext != next) {
                    this.mStackSupervisor.scheduleResumeTopActivities();
                }
                if (!this.mStackSupervisor.reportResumedActivityLocked(next)) {
                    if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                        return false;
                    }
                    this.mStackSupervisor.validateTopActivitiesLocked();
                    return false;
                }
                this.mNoAnimActivities.clear();
                if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                    return true;
                }
                this.mStackSupervisor.validateTopActivitiesLocked();
                return true;
            }
            try {
                ArrayList<ResultInfo> a = next.results;
                if (a != null) {
                    int N = a.size();
                    if (!next.finishing && N > 0) {
                        if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                            Slog.v(TAG_RESULTS, "Delivering results to " + next + ": " + a);
                        }
                        next.app.thread.scheduleSendResult(next.appToken, a);
                    }
                }
                if (next.newIntents != null) {
                    next.app.thread.scheduleNewIntent(next.newIntents, next.appToken);
                }
                this.mWindowManager.notifyAppStopped(next.appToken, false);
                EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, Integer.valueOf(next.userId), Integer.valueOf(System.identityHashCode(next)), Integer.valueOf(next.task.taskId), next.shortComponentName);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "ACT-AM_RESUME_ACTIVITY " + next + " task:" + next.task.taskId);
                }
                next.sleeping = false;
                this.mService.showUnsupportedZoomDialogIfNeededLocked(next);
                this.mService.showAskCompatModeDialogLocked(next);
                next.app.pendingUiClean = true;
                next.app.forceProcessStateUpTo(this.mService.mTopProcessState);
                next.clearOptionsLocked();
                next.app.thread.scheduleResumeActivity(next.appToken, next.app.repProcState, this.mService.isNextTransitionForward(), resumeAnimOptions);
                this.mStackSupervisor.checkReadyForSleepLocked();
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.d(TAG_STATES, "resumeTopActivityLocked: Resumed " + next);
                }
                try {
                    completeResumeLocked(next);
                } catch (Exception e3) {
                    Slog.w(TAG, "Exception thrown during resume of " + next, e3);
                    requestFinishActivityLocked(next.appToken, 0, null, "resume-exception", true);
                    if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                        return true;
                    }
                    this.mStackSupervisor.validateTopActivitiesLocked();
                    return true;
                }
            } catch (Exception e4) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Resume failed; resetting state to " + lastState + ": " + next);
                }
                next.state = lastState;
                if (lastStack != null) {
                    lastStack.mResumedActivity = activityRecord;
                }
                Slog.i(TAG, "Restarting because process died: " + next);
                if (!next.hasBeenLaunched) {
                    next.hasBeenLaunched = true;
                } else if (lastStack != null && this.mStackSupervisor.isFrontStack(lastStack)) {
                    next.showStartingWindow(null, true);
                }
                this.mStackSupervisor.startSpecificActivityLocked(next, true, false);
                if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                    return true;
                }
                this.mStackSupervisor.validateTopActivitiesLocked();
                return true;
            }
        }
        if (!ActivityManagerDebugConfig.DEBUG_STACK) {
            return true;
        }
        this.mStackSupervisor.validateTopActivitiesLocked();
        return true;
    }

    private TaskRecord getNextTask(TaskRecord targetTask) {
        int index = this.mTaskHistory.indexOf(targetTask);
        if (index >= 0) {
            int numTasks = this.mTaskHistory.size();
            for (int i = index + 1; i < numTasks; i++) {
                TaskRecord task = this.mTaskHistory.get(i);
                if (task.userId == targetTask.userId) {
                    return task;
                }
            }
            return null;
        }
        return null;
    }

    private void insertTaskAtPosition(TaskRecord task, int position) {
        if (position >= this.mTaskHistory.size()) {
            insertTaskAtTop(task, null);
            return;
        }
        int maxPosition = this.mTaskHistory.size();
        if (!this.mStackSupervisor.isCurrentProfileLocked(task.userId) && task.topRunningActivityLocked() == null) {
            while (maxPosition > 0) {
                TaskRecord tmpTask = this.mTaskHistory.get(maxPosition - 1);
                if (!this.mStackSupervisor.isCurrentProfileLocked(tmpTask.userId) || tmpTask.topRunningActivityLocked() == null) {
                    break;
                } else {
                    maxPosition--;
                }
            }
        }
        int position2 = Math.min(position, maxPosition);
        this.mTaskHistory.remove(task);
        this.mTaskHistory.add(position2, task);
        updateTaskMovement(task, true);
    }

    private void insertTaskAtTop(TaskRecord task, ActivityRecord newActivity) {
        TaskRecord tmpTask;
        TaskRecord nextTask;
        if (task.isOverHomeStack() && (nextTask = getNextTask(task)) != null) {
            nextTask.setTaskToReturnTo(task.getTaskToReturnTo());
        }
        if (isOnHomeDisplay()) {
            ActivityStack lastStack = this.mStackSupervisor.getLastStack();
            boolean fromHome = lastStack.isHomeStack();
            if (ActivityManagerDebugConfig.DEBUG_TASK_RETURNTO) {
                Slog.d(TAG, "insertTaskAtTop() task " + task + " fromHome=" + fromHome + " isHomeStack=" + isHomeStack() + " topTask=" + topTask());
                if (lastStack != null) {
                    TaskRecord top = lastStack.topTask();
                    Slog.d(TAG, "lastStack=" + lastStack + " lastTop=" + top);
                    if (top != null) {
                        Slog.d(TAG, "lastTopType=" + top.taskType);
                    }
                }
            }
            if (!isHomeStack() && (fromHome || topTask() != task)) {
                int returnToType = 0;
                if (fromHome && ActivityManager.StackId.allowTopTaskToReturnHome(this.mStackId)) {
                    returnToType = lastStack.topTask() == null ? 1 : lastStack.topTask().taskType;
                }
                task.setTaskToReturnTo(returnToType);
            }
        } else {
            task.setTaskToReturnTo(0);
        }
        this.mTaskHistory.remove(task);
        int taskNdx = this.mTaskHistory.size();
        boolean notShownWhenLocked = (newActivity == null || (newActivity.info.flags & 1024) != 0) ? newActivity == null && task.topRunningActivityLocked() == null : true;
        if (!this.mStackSupervisor.isCurrentProfileLocked(task.userId) && notShownWhenLocked) {
            do {
                taskNdx--;
                if (taskNdx < 0) {
                    break;
                }
                tmpTask = this.mTaskHistory.get(taskNdx);
                if (!this.mStackSupervisor.isCurrentProfileLocked(tmpTask.userId)) {
                    break;
                }
            } while (tmpTask.topRunningActivityLocked() != null);
            taskNdx++;
        }
        this.mTaskHistory.add(taskNdx, task);
        updateTaskMovement(task, true);
    }

    final void startActivityLocked(ActivityRecord r, boolean newTask, boolean keepCurTransition, ActivityOptions options) {
        int i;
        TaskRecord rTask = r.task;
        int taskId = rTask.taskId;
        if (!r.mLaunchTaskBehind && (taskForIdLocked(taskId) == null || newTask)) {
            insertTaskAtTop(rTask, r);
            this.mWindowManager.moveTaskToTop(taskId);
        }
        TaskRecord task = null;
        if (!newTask) {
            boolean startIt = true;
            int taskNdx = this.mTaskHistory.size() - 1;
            while (true) {
                if (taskNdx < 0) {
                    break;
                }
                task = this.mTaskHistory.get(taskNdx);
                if (task.getTopActivity() != null) {
                    if (task == r.task) {
                        if (!startIt) {
                            if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                Slog.i(TAG, "Adding activity " + r + " to task " + task, new RuntimeException("here").fillInStackTrace());
                            }
                            task.addActivityToTop(r);
                            r.putInHistory();
                            addConfigOverride(r, task);
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
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(TAG_USER_LEAVING, "startActivity() behind front, mUserLeaving=false");
            }
        }
        TaskRecord task2 = r.task;
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i(TAG, "Adding activity " + r + " to stack to task " + task2, new RuntimeException("here").fillInStackTrace());
        }
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
                showStartingIcon = true;
            }
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: starting " + r);
            }
            if ((r.intent.getFlags() & PackageManagerService.DumpState.DUMP_INSTALLS) != 0) {
                this.mWindowManager.prepareAppTransition(0, keepCurTransition);
                this.mNoAnimActivities.add(r);
            } else {
                WindowManagerService windowManagerService = this.mWindowManager;
                if (newTask) {
                    if (r.mLaunchTaskBehind) {
                        i = 16;
                    } else {
                        i = 8;
                    }
                } else {
                    i = 6;
                }
                windowManagerService.prepareAppTransition(i, keepCurTransition);
                this.mNoAnimActivities.remove(r);
            }
            addConfigOverride(r, task2);
            boolean doShow = true;
            if (newTask) {
                if ((r.intent.getFlags() & 2097152) != 0) {
                    resetTaskIfNeededLocked(r, r);
                    doShow = topRunningNonDelayedActivityLocked(null) == r;
                }
            } else if (options != null && options.getAnimationType() == 5) {
                doShow = false;
            }
            if (r.mLaunchTaskBehind) {
                this.mWindowManager.setAppVisibility(r.appToken, true);
                ensureActivitiesVisibleLocked(null, 0, false);
                return;
            } else {
                if (!doShow) {
                    return;
                }
                ActivityRecord prev = r.task.topRunningActivityWithStartingWindowLocked();
                if (prev != null && (prev.task != r.task || prev.nowVisible)) {
                    prev = null;
                }
                r.showStartingWindow(prev, showStartingIcon);
                return;
            }
        }
        addConfigOverride(r, task2);
        ActivityOptions.abort(options);
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
        boolean canMoveOptions = true;
        ArrayList<ActivityRecord> activities = task.mActivities;
        int numActivities = activities.size();
        int rootActivityNdx = task.findEffectiveRootIndex();
        for (int i = numActivities - 1; i > rootActivityNdx; i--) {
            ActivityRecord target = activities.get(i);
            if (target.frontOfTask) {
                break;
            }
            int flags = target.info.flags;
            boolean finishOnTaskLaunch = (flags & 2) != 0;
            boolean allowTaskReparenting = (flags & 64) != 0;
            boolean clearWhenTaskReset = (target.intent.getFlags() & PackageManagerService.DumpState.DUMP_FROZEN) != 0;
            if (!finishOnTaskLaunch && !clearWhenTaskReset && target.resultTo != null) {
                if (replyChainEnd < 0) {
                    replyChainEnd = i;
                }
            } else if (!finishOnTaskLaunch && !clearWhenTaskReset && allowTaskReparenting && target.taskAffinity != null && !target.taskAffinity.equals(task.affinity)) {
                ActivityRecord bottom = (this.mTaskHistory.isEmpty() || this.mTaskHistory.get(0).mActivities.isEmpty()) ? null : this.mTaskHistory.get(0).mActivities.get(0);
                if (bottom != null && target.taskAffinity != null && target.taskAffinity.equals(bottom.task.affinity)) {
                    targetTask = bottom.task;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Start pushing activity " + target + " out to bottom task " + bottom.task);
                    }
                } else {
                    targetTask = createTaskRecord(this.mStackSupervisor.getNextTaskIdForUserLocked(target.userId), target.info, null, null, null, false);
                    targetTask.affinityIntent = target.intent;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Start pushing activity " + target + " out to new task " + target.task);
                    }
                }
                setAppTask(target, targetTask);
                boolean noOptions = canMoveOptions;
                int start = replyChainEnd < 0 ? i : replyChainEnd;
                for (int srcPos = start; srcPos >= i; srcPos--) {
                    ActivityRecord p = activities.get(srcPos);
                    if (!p.finishing) {
                        canMoveOptions = false;
                        if (noOptions && topOptions == null && (topOptions = p.takeOptionsLocked()) != null) {
                            noOptions = false;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                            Slog.i(TAG_ADD_REMOVE, "Removing activity " + p + " from task=" + task + " adding to task=" + targetTask + " Callers=" + Debug.getCallers(4));
                        }
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.v(TAG_TASKS, "Pushing next activity " + p + " out to target's task " + target.task);
                        }
                        p.setTask(targetTask, null);
                        targetTask.addActivityAtBottom(p);
                        setAppTask(p, targetTask);
                    }
                }
                this.mWindowManager.moveTaskToBottom(targetTask.taskId);
                replyChainEnd = -1;
            } else if (forceReset || finishOnTaskLaunch || clearWhenTaskReset) {
                if (clearWhenTaskReset) {
                    end = activities.size() - 1;
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
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.w(TAG_TASKS, "resetTaskIntendedTask: calling finishActivity on " + p2);
                        }
                        if (finishActivityLocked(p2, 0, null, "reset-task", false)) {
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
            boolean finishOnTaskLaunch = (flags & 2) != 0;
            boolean allowTaskReparenting = (flags & 64) != 0;
            if (target.resultTo != null) {
                if (replyChainEnd < 0) {
                    replyChainEnd = i;
                }
            } else if (topTaskIsHigher && allowTaskReparenting && taskAffinity != null && taskAffinity.equals(target.taskAffinity)) {
                if (forceReset || finishOnTaskLaunch) {
                    int start = replyChainEnd >= 0 ? replyChainEnd : i;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Finishing task at index " + start + " to " + i);
                    }
                    for (int srcPos = start; srcPos >= i; srcPos--) {
                        ActivityRecord p = activities.get(srcPos);
                        if (!p.finishing) {
                            finishActivityLocked(p, 0, null, "move-affinity", false);
                        }
                    }
                } else {
                    if (taskInsertionPoint < 0) {
                        taskInsertionPoint = task.mActivities.size();
                    }
                    int start2 = replyChainEnd >= 0 ? replyChainEnd : i;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Reparenting from task=" + affinityTask + ":" + start2 + "-" + i + " to task=" + task + ":" + taskInsertionPoint);
                    }
                    for (int srcPos2 = start2; srcPos2 >= i; srcPos2--) {
                        ActivityRecord p2 = activities.get(srcPos2);
                        p2.setTask(task, null);
                        task.addActivityAtIndex(taskInsertionPoint, p2);
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                            Slog.i(TAG_ADD_REMOVE, "Removing and adding activity " + p2 + " to stack at " + task + " callers=" + Debug.getCallers(3));
                        }
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.v(TAG_TASKS, "Pulling activity " + p2 + " from " + srcPos2 + " in to resetting task " + task);
                        }
                        setAppTask(p2, task);
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
        boolean forceReset = (newActivity.info.flags & 4) != 0;
        TaskRecord task = taskTop.task;
        boolean taskFound = false;
        ActivityOptions topOptions = null;
        int reparentInsertionPoint = -1;
        for (int i = this.mTaskHistory.size() - 1; i >= 0; i--) {
            TaskRecord targetTask = this.mTaskHistory.get(i);
            if (targetTask == task) {
                topOptions = resetTargetTaskIfNeededLocked(task, forceReset);
                taskFound = true;
            } else {
                reparentInsertionPoint = resetAffinityTaskIfNeededLocked(targetTask, task, taskFound, forceReset, reparentInsertionPoint);
            }
        }
        int taskNdx = this.mTaskHistory.indexOf(task);
        if (taskNdx >= 0) {
            while (true) {
                int taskNdx2 = taskNdx - 1;
                taskTop = this.mTaskHistory.get(taskNdx).getTopActivity();
                if (taskTop != null || taskNdx2 < 0) {
                    break;
                }
                taskNdx = taskNdx2;
            }
        }
        if (topOptions != null) {
            if (taskTop != null) {
                taskTop.updateOptionsLocked(topOptions);
            } else {
                topOptions.abort();
            }
        }
        return taskTop;
    }

    void sendActivityResultLocked(int callingUid, ActivityRecord r, String resultWho, int requestCode, int resultCode, Intent data) {
        if (callingUid > 0) {
            this.mService.grantUriPermissionFromIntentLocked(callingUid, r.packageName, data, r.getUriPermissionsLocked(), r.userId);
        }
        if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG, "Send activity result to " + r + " : who=" + resultWho + " req=" + requestCode + " res=" + resultCode + " data=" + data);
        }
        if (this.mResumedActivity == r && r.app != null && r.app.thread != null) {
            try {
                ArrayList<ResultInfo> list = new ArrayList<>();
                list.add(new ResultInfo(resultWho, requestCode, resultCode, data));
                r.app.thread.scheduleSendResult(r.appToken, list);
                return;
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown sending result to " + r, e);
            }
        }
        r.addResultLocked(null, resultWho, requestCode, resultCode, data);
    }

    private void adjustFocusedActivityLocked(ActivityRecord r, String reason) {
        if (this.mStackSupervisor.isFocusedStack(this) && this.mService.mFocusedActivity == r) {
            ActivityRecord next = topRunningActivityLocked();
            String myReason = reason + " adjustFocus";
            if (next != r) {
                if (MultiWindowManager.isSupported()) {
                    if (MultiWindowManager.DEBUG) {
                        Slog.d(TAG, "adjustFocusedActivityLocked, r = " + r + ", r.task = " + r.task + ", topTask() = " + topTask() + ", r.frontOfTask" + r.frontOfTask);
                    }
                    if (r.task.mSticky && r.frontOfTask && r.task == topTask()) {
                        this.mService.stickWindow(r.task, false);
                    }
                }
                if (next != null && ActivityManager.StackId.keepFocusInStackIfPossible(this.mStackId) && isFocusable()) {
                    this.mService.setFocusedActivityLocked(next, myReason);
                    return;
                }
                TaskRecord task = r.task;
                if (r.frontOfTask && task == topTask() && task.isOverHomeStack()) {
                    int taskToReturnTo = task.getTaskToReturnTo();
                    if ((!this.mFullscreen && adjustFocusToNextFocusableStackLocked(taskToReturnTo, myReason)) || this.mStackSupervisor.moveHomeStackTaskToTop(taskToReturnTo, myReason)) {
                        return;
                    }
                }
            }
            this.mService.setFocusedActivityLocked(this.mStackSupervisor.topRunningActivityLocked(), myReason);
        }
    }

    private boolean adjustFocusToNextFocusableStackLocked(int taskToReturnTo, String reason) {
        ActivityStack stack = getNextFocusableStackLocked();
        String myReason = reason + " adjustFocusToNextFocusableStack";
        if (stack == null) {
            return false;
        }
        ActivityRecord top = stack.topRunningActivityLocked();
        if (stack.isHomeStack() && (top == null || !top.visible)) {
            return this.mStackSupervisor.moveHomeStackTaskToTop(taskToReturnTo, reason);
        }
        return this.mService.setFocusedActivityLocked(top, myReason);
    }

    final void stopActivityLocked(ActivityRecord r) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Stopping: " + r);
        }
        if (((r.intent.getFlags() & 1073741824) != 0 || (r.info.flags & 128) != 0) && !r.finishing) {
            if (!this.mService.isSleepingLocked()) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.d(TAG_STATES, "no-history finish of " + r);
                }
                if (requestFinishActivityLocked(r.appToken, 0, null, "stop-no-history", false)) {
                    adjustFocusedActivityLocked(r, "stopActivityFinished");
                    r.resumeKeyDispatchingLocked();
                    return;
                }
            } else if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "Not finishing noHistory " + r + " on stop because we're just sleeping");
            }
        }
        if (r.app == null || r.app.thread == null) {
            return;
        }
        adjustFocusedActivityLocked(r, "stopActivity");
        r.resumeKeyDispatchingLocked();
        try {
            r.stopped = false;
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to STOPPING: " + r + " (stop requested)");
            }
            r.state = ActivityState.STOPPING;
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Stopping visible=" + r.visible + " for " + r);
            }
            if (!r.visible) {
                this.mWindowManager.setAppVisibility(r.appToken, false);
            }
            EventLogTags.writeAmStopActivity(r.userId, System.identityHashCode(r), r.shortComponentName);
            r.app.thread.scheduleStopActivity(r.appToken, r.visible, r.configChangeFlags);
            if (this.mService.isSleepingOrShuttingDownLocked()) {
                r.setSleeping(true);
            }
            Message msg = this.mHandler.obtainMessage(104, r);
            this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown during pause", e);
            r.stopped = true;
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Stop failed; moving to STOPPED: " + r);
            }
            r.state = ActivityState.STOPPED;
            ProcessRecord appProc = r.app;
            if (appProc != null) {
                AMEventHookData.AfterActivityStopped eventData = AMEventHookData.AfterActivityStopped.createInstance();
                eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityStopped, eventData);
            }
            if (!r.deferRelaunchUntilPaused) {
                return;
            }
            destroyActivityLocked(r, true, "stop-except");
        }
    }

    final boolean requestFinishActivityLocked(IBinder token, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        ActivityRecord r = isInStackLocked(token);
        if (ActivityManagerDebugConfig.DEBUG_RESULTS || ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Finishing activity token=" + token + " r=, result=" + resultCode + ", data=" + resultData + ", reason=" + reason);
        }
        if (r == null) {
            return false;
        }
        finishActivityLocked(r, resultCode, resultData, reason, oomAdj);
        return true;
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

    final TaskRecord finishTopRunningActivityLocked(ProcessRecord app, String reason) {
        ActivityRecord r = topRunningActivityLocked();
        if (r == null || r.app != app) {
            return null;
        }
        Slog.w(TAG, "  Force finishing activity " + r.intent.getComponent().flattenToShortString());
        int taskNdx = this.mTaskHistory.indexOf(r.task);
        int activityNdx = r.task.mActivities.indexOf(r);
        finishActivityLocked(r, 0, null, reason, false);
        TaskRecord finishedTask = r.task;
        int activityNdx2 = activityNdx - 1;
        if (activityNdx2 < 0) {
            do {
                taskNdx--;
                if (taskNdx < 0) {
                    break;
                }
                activityNdx2 = this.mTaskHistory.get(taskNdx).mActivities.size() - 1;
            } while (activityNdx2 < 0);
        }
        if (activityNdx2 >= 0) {
            ActivityRecord r2 = this.mTaskHistory.get(taskNdx).mActivities.get(activityNdx2);
            if ((r2.state == ActivityState.RESUMED || r2.state == ActivityState.PAUSING || r2.state == ActivityState.PAUSED) && (!r2.isHomeActivity() || this.mService.mHomeProcess != r2.app)) {
                Slog.w(TAG, "  Force finishing activity " + r2.intent.getComponent().flattenToShortString());
                finishActivityLocked(r2, 0, null, reason, false);
            }
        }
        return finishedTask;
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
                        didOne = true;
                    }
                }
            } else {
                int activityNdx2 = tr.mActivities.size() - 1;
                while (true) {
                    if (activityNdx2 >= 0) {
                        ActivityRecord r2 = tr.mActivities.get(activityNdx2);
                        if (r2.voiceSession != null && r2.voiceSession.asBinder() == sessionBinder) {
                            r2.clearVoiceSessionLocked();
                            try {
                                r2.app.thread.scheduleLocalVoiceInteractionStarted(r2.appToken, (IVoiceInteractor) null);
                            } catch (RemoteException e) {
                            }
                            this.mService.finishRunningVoiceLocked();
                            break;
                        }
                        activityNdx2--;
                    }
                }
            }
        }
        if (!didOne) {
            return;
        }
        this.mService.updateOomAdjLocked();
    }

    final boolean finishActivityAffinityLocked(ActivityRecord r) {
        ArrayList<ActivityRecord> activities = r.task.mActivities;
        for (int index = activities.indexOf(r); index >= 0; index--) {
            ActivityRecord cur = activities.get(index);
            if (!Objects.equals(cur.taskAffinity, r.taskAffinity)) {
                break;
            }
            finishActivityLocked(cur, 0, null, "request-affinity", true);
        }
        return true;
    }

    final void finishActivityResultsLocked(ActivityRecord r, int resultCode, Intent resultData) {
        ActivityRecord resultTo = r.resultTo;
        if (resultTo != null) {
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                Slog.v(TAG_RESULTS, "Adding result to " + resultTo + " who=" + r.resultWho + " req=" + r.requestCode + " res=" + resultCode + " data=" + resultData);
            }
            if (resultTo.userId != r.userId && resultData != null) {
                resultData.prepareToLeaveUser(r.userId);
            }
            if (r.info.applicationInfo.uid > 0) {
                this.mService.grantUriPermissionFromIntentLocked(r.info.applicationInfo.uid, resultTo.packageName, resultData, resultTo.getUriPermissionsLocked(), resultTo.userId);
            }
            resultTo.addResultLocked(r, r.resultWho, r.requestCode, resultCode, resultData);
            r.resultTo = null;
        } else if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG_RESULTS, "No result destination from " + r);
        }
        r.results = null;
        r.pendingResults = null;
        r.newIntents = null;
        r.icicle = null;
    }

    final boolean finishActivityLocked(ActivityRecord r, int resultCode, Intent resultData, String reason, boolean oomAdj) {
        if (r.finishing) {
            Slog.w(TAG, "Duplicate finish request for " + r);
            return false;
        }
        r.makeFinishingLocked();
        TaskRecord task = r.task;
        EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName, reason);
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-AM_FINISH_ACTIVITY " + r + " task:" + r.task + " " + reason);
        }
        ArrayList<ActivityRecord> activities = task.mActivities;
        int index = activities.indexOf(r);
        if (index < activities.size() - 1) {
            task.setFrontOfTask();
            if ((r.intent.getFlags() & PackageManagerService.DumpState.DUMP_FROZEN) != 0) {
                activities.get(index + 1).intent.addFlags(PackageManagerService.DumpState.DUMP_FROZEN);
            }
        }
        r.pauseKeyDispatchingLocked();
        adjustFocusedActivityLocked(r, "finishActivity");
        finishActivityResultsLocked(r, resultCode, resultData);
        boolean endTask = index <= 0;
        int transit = endTask ? 9 : 7;
        if (this.mResumedActivity == r) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY || ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare close transition: finishing " + r);
            }
            this.mWindowManager.prepareAppTransition(transit, false);
            this.mWindowManager.setAppVisibility(r.appToken, false);
            if (this.mPausingActivity == null) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Finish needs to pause: " + r);
                }
                if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                    Slog.v(TAG_USER_LEAVING, "finish() => pause with userLeaving=false");
                }
                startPausingLocked(false, false, false, false);
                ActivityRecord next = this.mStackSupervisor.getFocusedStack().topRunningActivityLocked();
                if (next != null && next.info != null && (next.info.packageName != this.mStackSupervisor.mLastResumedActivity.packageName || next.info.name != this.mStackSupervisor.mLastResumedActivity.activityName)) {
                    AMEventHookData.BeforeActivitySwitch eventData = AMEventHookData.BeforeActivitySwitch.createInstance();
                    eventData.set(new Object[]{this.mStackSupervisor.mLastResumedActivity.activityName, next.info.name, this.mStackSupervisor.mLastResumedActivity.packageName, next.info.packageName, Integer.valueOf(this.mStackSupervisor.mLastResumedActivity.activityType), Integer.valueOf(next.mActivityType), true, null, null, null});
                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_BeforeActivitySwitch, eventData);
                }
            }
            if (endTask) {
                this.mStackSupervisor.removeLockedTaskLocked(task);
                return false;
            }
            return false;
        }
        if (r.state != ActivityState.PAUSING) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Finish not pausing: " + r);
            }
            if (r.visible) {
                this.mWindowManager.prepareAppTransition(transit, false);
                this.mWindowManager.setAppVisibility(r.appToken, false);
                this.mWindowManager.executeAppTransition();
                if (!this.mStackSupervisor.mWaitingVisibleActivities.contains(r)) {
                    this.mStackSupervisor.mWaitingVisibleActivities.add(r);
                }
            }
            return finishCurrentActivityLocked(r, (r.visible || r.nowVisible) ? 2 : 1, oomAdj) == null;
        }
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Finish waiting for pause of: " + r);
            return false;
        }
        return false;
    }

    final ActivityRecord finishCurrentActivityLocked(ActivityRecord r, int mode, boolean oomAdj) {
        ActivityRecord next = this.mStackSupervisor.topRunningActivityLocked();
        if (mode == 2 && ((r.visible || r.nowVisible) && next != null && !next.nowVisible)) {
            if (!this.mStackSupervisor.mStoppingActivities.contains(r)) {
                addToStopping(r, false);
            }
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to STOPPING: " + r + " (finish requested)");
            }
            r.state = ActivityState.STOPPING;
            if (oomAdj) {
                this.mService.updateOomAdjLocked();
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
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to FINISHING: " + r);
        }
        r.state = ActivityState.FINISHING;
        if (mode == 0 || ((prevState == ActivityState.PAUSED && (mode == 1 || mode == 2 || this.mStackId == 4)) || prevState == ActivityState.STOPPED || prevState == ActivityState.INITIALIZING)) {
            r.makeFinishingLocked();
            boolean activityRemoved = destroyActivityLocked(r, true, "finish-imm");
            if (prevState == ActivityState.PAUSED && mode == 2) {
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
            }
            if (activityRemoved) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
            if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
                Slog.d(TAG_CONTAINERS, "destroyActivityLocked: finishCurrentActivityLocked r=" + r + " destroy returned removed=" + activityRemoved);
            }
            if (activityRemoved) {
                return null;
            }
            return r;
        }
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Enqueueing pending finish: " + r);
        }
        this.mStackSupervisor.mFinishingActivities.add(r);
        r.resumeKeyDispatchingLocked();
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return r;
    }

    void finishAllActivitiesLocked(boolean immediately) {
        boolean noActivitiesInStack = true;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                noActivitiesInStack = false;
                if (!r.finishing || immediately) {
                    Slog.d(TAG, "finishAllActivitiesLocked: finishing " + r + " immediately");
                    finishCurrentActivityLocked(r, 0, false);
                }
            }
        }
        if (!noActivitiesInStack) {
            return;
        }
        this.mActivityContainer.onTaskListEmptyLocked();
    }

    final boolean shouldUpRecreateTaskLocked(ActivityRecord srec, String destAffinity) {
        if (srec == null || srec.task.affinity == null || !srec.task.affinity.equals(destAffinity)) {
            return true;
        }
        if (srec.frontOfTask && srec.task != null && srec.task.getBaseIntent() != null && srec.task.getBaseIntent().isDocument()) {
            if (srec.task.getTaskToReturnTo() != 0) {
                return true;
            }
            int taskIdx = this.mTaskHistory.indexOf(srec.task);
            if (taskIdx <= 0) {
                Slog.w(TAG, "shouldUpRecreateTask: task not in history for " + srec);
                return false;
            }
            if (taskIdx == 0) {
                return true;
            }
            TaskRecord prevTask = this.mTaskHistory.get(taskIdx);
            if (!srec.task.affinity.equals(prevTask.affinity)) {
                return true;
            }
        }
        return false;
    }

    final boolean navigateUpToLocked(ActivityRecord srec, Intent destIntent, int resultCode, Intent resultData) {
        ActivityRecord next;
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
                    foundParentInTask = true;
                    break;
                }
                i--;
            }
        }
        IActivityController controller = this.mService.mController;
        if (controller != null && (next = topRunningActivityLocked(srec.appToken, 0)) != null) {
            boolean resumeOK = true;
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
            requestFinishActivityLocked(activities.get(i2).appToken, resultCode, resultData, "navigate-up", true);
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
                    int res = this.mService.mActivityStarter.startActivityLocked(srec.app.thread, destIntent, null, null, aInfo, null, null, null, parent.appToken, null, 0, -1, parent.launchedFromUid, parent.launchedFromPackage, -1, parent.launchedFromUid, 0, null, false, true, null, null, null);
                    foundParentInTask = res == 0;
                } catch (RemoteException e2) {
                    foundParentInTask = false;
                }
                requestFinishActivityLocked(parent.appToken, resultCode, resultData, "navigate-top", true);
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
        this.mService.resetFocusedActivityIfNeededLocked(r);
        r.deferRelaunchUntilPaused = false;
        r.frozenBeforeDestroy = false;
        if (setState) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to DESTROYED: " + r + " (cleaning up)");
            }
            r.state = ActivityState.DESTROYED;
            ProcessRecord appProc = r.app;
            if (appProc != null) {
                AMEventHookData.AfterActivityDestroyed eventData = AMEventHookData.AfterActivityDestroyed.createInstance();
                eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityDestroyed, eventData);
            }
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Clearing app during cleanUp for activity " + r);
            }
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
        if (getVisibleBehindActivity() != r) {
            return;
        }
        this.mStackSupervisor.requestVisibleBehindLocked(r, false);
    }

    private void removeTimeoutsForActivityLocked(ActivityRecord r) {
        this.mStackSupervisor.removeTimeoutsForActivityLocked(r);
        this.mHandler.removeMessages(101, r);
        this.mHandler.removeMessages(104, r);
        this.mHandler.removeMessages(102, r);
        r.finishLaunchTickingLocked();
    }

    private void removeActivityFromHistoryLocked(ActivityRecord r, TaskRecord oldTop, String reason) {
        this.mStackSupervisor.removeChildActivityContainers(r);
        finishActivityResultsLocked(r, 0, null);
        r.makeFinishingLocked();
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i(TAG_ADD_REMOVE, "Removing activity " + r + " from stack callers=" + Debug.getCallers(5));
        }
        r.takeFromHistory();
        removeTimeoutsForActivityLocked(r);
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to DESTROYED: " + r + " (removed from history)");
        }
        r.state = ActivityState.DESTROYED;
        ProcessRecord appProc = r.app;
        if (appProc != null) {
            AMEventHookData.AfterActivityDestroyed eventData = AMEventHookData.AfterActivityDestroyed.createInstance();
            eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityDestroyed, eventData);
        }
        if (ActivityManagerDebugConfig.DEBUG_APP) {
            Slog.v(TAG_APP, "Clearing app during remove for activity " + r);
        }
        r.app = null;
        this.mWindowManager.removeAppToken(r.appToken);
        TaskRecord task = r.task;
        TaskRecord topTask = oldTop != null ? oldTop : topTask();
        if (task != null && task.removeActivity(r)) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG_STACK, "removeActivityFromHistoryLocked: last activity removed from " + this);
            }
            if (this.mStackSupervisor.isFocusedStack(this) && task == topTask && task.isOverHomeStack()) {
                if (MultiWindowManager.isSupported() && task.mSticky) {
                    this.mService.stickWindow(task, false);
                }
                this.mStackSupervisor.moveHomeStackTaskToTop(task.getTaskToReturnTo(), reason);
            }
            removeTask(task, reason);
        }
        cleanUpActivityServicesLocked(r);
        r.removeUriPermissionsLocked();
    }

    final void cleanUpActivityServicesLocked(ActivityRecord r) {
        if (r.connections == null) {
            return;
        }
        for (ConnectionRecord c : r.connections) {
            this.mService.mServices.removeConnectionLocked(c, null, r);
        }
        r.connections = null;
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
                        lastIsOpaque = true;
                    }
                    if ((owner == null || r.app == owner) && lastIsOpaque && r.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                            Slog.v(TAG_SWITCH, "Destroying " + r + " in state " + r.state + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + reason);
                        }
                        if (destroyActivityLocked(r, true, reason)) {
                            activityRemoved = true;
                        }
                    }
                }
            }
        }
        if (activityRemoved) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    final boolean safelyDestroyActivityLocked(ActivityRecord r, String reason) {
        if (!r.isDestroyable()) {
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "Destroying " + r + " in state " + r.state + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + reason);
        }
        return destroyActivityLocked(r, true, reason);
    }

    final int releaseSomeActivitiesLocked(ProcessRecord app, ArraySet<TaskRecord> tasks, String reason) {
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + app);
        }
        int maxTasks = tasks.size() / 4;
        if (maxTasks < 1) {
            maxTasks = 1;
        }
        int numReleased = 0;
        int taskNdx = 0;
        while (taskNdx < this.mTaskHistory.size() && maxTasks > 0) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (tasks.contains(task)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Looking for activities to release in " + task);
                }
                int curNum = 0;
                ArrayList<ActivityRecord> activities = task.mActivities;
                int actNdx = 0;
                while (actNdx < activities.size()) {
                    ActivityRecord activity = activities.get(actNdx);
                    if (activity.app == app && activity.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                            Slog.v(TAG_RELEASE, "Destroying " + activity + " in state " + activity.state + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + reason);
                        }
                        destroyActivityLocked(activity, true, reason);
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
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Done releasing: did " + numReleased + " activities");
        }
        return numReleased;
    }

    final boolean destroyActivityLocked(ActivityRecord r, boolean removeFromApp, String reason) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_SWITCH, "Removing activity from " + reason + ": token=" + r + ", app=" + (r.app != null ? r.app.processName : "(null)"));
        }
        EventLog.writeEvent(EventLogTags.AM_DESTROY_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName, reason);
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-Removing activity from " + reason + ": token=" + r + ", app=" + (r.app != null ? r.app.processName : "(null)"));
        }
        boolean removedFromHistory = false;
        TaskRecord topTask = topTask();
        cleanUpActivityLocked(r, false, false);
        boolean hadApp = r.app != null;
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
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.i(TAG_SWITCH, "Destroying: " + r);
                }
                r.app.thread.scheduleDestroyActivity(r.appToken, r.finishing, r.configChangeFlags);
            } catch (Exception e) {
                if (r.finishing) {
                    removeActivityFromHistoryLocked(r, topTask, reason + " exceptionInScheduleDestroy");
                    removedFromHistory = true;
                    skipDestroy = true;
                }
            }
            r.nowVisible = false;
            if (r.finishing && !skipDestroy) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to DESTROYING: " + r + " (destroy requested)");
                }
                r.state = ActivityState.DESTROYING;
                Message msg = this.mHandler.obtainMessage(102, r);
                this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to DESTROYED: " + r + " (destroy skipped)");
                }
                r.state = ActivityState.DESTROYED;
                ProcessRecord appProc = r.app;
                if (appProc != null) {
                    AMEventHookData.AfterActivityDestroyed eventData = AMEventHookData.AfterActivityDestroyed.createInstance();
                    eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityDestroyed, eventData);
                }
                if (ActivityManagerDebugConfig.DEBUG_APP) {
                    Slog.v(TAG_APP, "Clearing app during destroy for activity " + r);
                }
                r.app = null;
            }
        } else if (r.finishing) {
            removeActivityFromHistoryLocked(r, topTask, reason + " hadNoApp");
            removedFromHistory = true;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to DESTROYED: " + r + " (no app)");
            }
            r.state = ActivityState.DESTROYED;
            ProcessRecord appProc2 = r.app;
            if (appProc2 != null) {
                AMEventHookData.AfterActivityDestroyed eventData2 = AMEventHookData.AfterActivityDestroyed.createInstance();
                eventData2.set(new Object[]{Integer.valueOf(appProc2.pid), r.info.name, r.info.packageName});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityDestroyed, eventData2);
            }
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Clearing app during destroy for activity " + r);
            }
            r.app = null;
        }
        r.configChangeFlags = 0;
        if (!this.mLRUActivities.remove(r) && hadApp) {
            Slog.w(TAG, "Activity " + r + " being finished, but not in LRU list");
        }
        return removedFromHistory;
    }

    final void activityDestroyedLocked(IBinder token, String reason) {
        long origId = Binder.clearCallingIdentity();
        try {
            ActivityRecord r = ActivityRecord.forTokenLocked(token);
            if (r != null) {
                this.mHandler.removeMessages(102, r);
            }
            if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
                Slog.d(TAG_CONTAINERS, "activityDestroyedLocked: r=" + r);
            }
            if (isInStackLocked(r) != null && r.state == ActivityState.DESTROYING) {
                cleanUpActivityLocked(r, true, false);
                removeActivityFromHistoryLocked(r, null, reason);
            }
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void releaseBackgroundResources(ActivityRecord r) {
        if (!hasVisibleBehindActivity() || this.mHandler.hasMessages(107)) {
            return;
        }
        if (r == topRunningActivityLocked() && getStackVisibilityLocked(null) == 1) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.d(TAG_STATES, "releaseBackgroundResources activtyDisplay=" + this.mActivityContainer.mActivityDisplay + " visibleBehind=" + r + " app=" + r.app + " thread=" + r.app.thread);
        }
        if (r == null || r.app == null || r.app.thread == null) {
            Slog.e(TAG, "releaseBackgroundResources: activity " + r + " no longer running");
            backgroundResourcesReleased();
        } else {
            try {
                r.app.thread.scheduleCancelVisibleBehind(r.appToken);
            } catch (RemoteException e) {
            }
            this.mHandler.sendEmptyMessageDelayed(107, 500L);
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
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
    }

    boolean hasVisibleBehindActivity() {
        if (isAttached()) {
            return this.mActivityContainer.mActivityDisplay.hasVisibleBehindActivity();
        }
        return false;
    }

    void setVisibleBehindActivity(ActivityRecord r) {
        if (!isAttached()) {
            return;
        }
        this.mActivityContainer.mActivityDisplay.setVisibleBehindActivity(r);
    }

    ActivityRecord getVisibleBehindActivity() {
        if (isAttached()) {
            return this.mActivityContainer.mActivityDisplay.mVisibleBehindActivity;
        }
        return null;
    }

    private void removeHistoryRecordsForAppLocked(ArrayList<ActivityRecord> list, ProcessRecord app, String listName) {
        int i = list.size();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_CLEANUP, "Removing app " + app + " from list " + listName + " with " + i + " entries");
        }
        while (i > 0) {
            i--;
            ActivityRecord r = list.get(i);
            if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_CLEANUP, "Record #" + i + " " + r);
            }
            if (r.app == app) {
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(TAG_CLEANUP, "---> REMOVING this entry!");
                }
                list.remove(i);
                this.mWindowManager.notifyAppRelaunchingCleared(r.appToken);
                removeTimeoutsForActivityLocked(r);
            }
        }
    }

    boolean removeHistoryRecordsForAppLocked(ProcessRecord app) {
        removeHistoryRecordsForAppLocked(this.mLRUActivities, app, "mLRUActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mStoppingActivities, app, "mStoppingActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mGoingToSleepActivities, app, "mGoingToSleepActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mWaitingVisibleActivities, app, "mWaitingVisibleActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mFinishingActivities, app, "mFinishingActivities");
        boolean hasVisibleActivities = false;
        int i = numActivities();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_CLEANUP, "Removing app " + app + " from history with " + i + " entries");
        }
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                i--;
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(TAG_CLEANUP, "Record #" + i + " " + r + ": app=" + r.app);
                }
                if (r.app == app) {
                    if (r.visible) {
                        hasVisibleActivities = true;
                    }
                    boolean remove = ((r.haveState || r.stateNotNeeded) && !r.finishing) ? !r.visible && r.launchCount > 2 && r.lastLaunchTime > SystemClock.uptimeMillis() - 60000 : true;
                    if (remove) {
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                            Slog.i(TAG_ADD_REMOVE, "Removing activity " + r + " from stack at " + i + ": haveState=" + r.haveState + " stateNotNeeded=" + r.stateNotNeeded + " finishing=" + r.finishing + " state=" + r.state + " callers=" + Debug.getCallers(5));
                        }
                        if (!r.finishing) {
                            Slog.w(TAG, "Force removing " + r + ": app died, no saved state");
                            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName, "proc died without state saved");
                            if (r.state == ActivityState.RESUMED) {
                                this.mService.updateUsageStats(r, false);
                            }
                        }
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_ALL) {
                            Slog.v(TAG, "Keeping entry, setting app to null");
                        }
                        if (ActivityManagerDebugConfig.DEBUG_APP) {
                            Slog.v(TAG_APP, "Clearing app during removeHistory for activity " + r);
                        }
                        r.app = null;
                        r.nowVisible = r.visible;
                        if (!r.haveState) {
                            if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
                                Slog.i(TAG_SAVED_STATE, "App died, clearing saved state of " + r);
                            }
                            r.icicle = null;
                        }
                    }
                    cleanUpActivityLocked(r, true, true);
                    if (remove) {
                        removeActivityFromHistoryLocked(r, null, "appDied");
                    }
                }
            }
        }
        return hasVisibleActivities;
    }

    final void updateTransitLocked(int transit, ActivityOptions options) {
        if (options != null) {
            ActivityRecord r = topRunningActivityLocked();
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
        this.mStackSupervisor.invalidateTaskLayers();
    }

    void moveHomeStackTaskToTop(int homeStackTaskType) {
        int top = this.mTaskHistory.size() - 1;
        for (int taskNdx = top; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.taskType == homeStackTaskType) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG_STACK, "moveHomeStackTaskToTop: moving " + task);
                }
                this.mTaskHistory.remove(taskNdx);
                this.mTaskHistory.add(top, task);
                updateTaskMovement(task, true);
                return;
            }
        }
    }

    final void moveTaskToFrontLocked(TaskRecord tr, boolean noAnimation, ActivityOptions options, AppTimeTracker timeTracker, String reason) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "moveTaskToFront: " + tr);
        }
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS || ActivityManagerDebugConfig.DEBUG_MULTIWINDOW) {
            Slog.d(TAG, "ACT-moveTaskToFront: " + tr + " mStackId=" + this.mStackId + " reason=" + reason + " " + Debug.getCallers(5));
        }
        int numTasks = this.mTaskHistory.size();
        int index = this.mTaskHistory.indexOf(tr);
        if (numTasks == 0 || index < 0) {
            if (noAnimation) {
                ActivityOptions.abort(options);
                return;
            } else {
                updateTransitLocked(10, options);
                return;
            }
        }
        if (timeTracker != null) {
            for (int i = tr.mActivities.size() - 1; i >= 0; i--) {
                tr.mActivities.get(i).appTimeTracker = timeTracker;
            }
        }
        insertTaskAtTop(tr, null);
        ActivityRecord top = tr.getTopActivity();
        if (!okToShowLocked(top)) {
            addRecentActivityLocked(top);
            ActivityOptions.abort(options);
            return;
        }
        ActivityRecord r = topRunningActivityLocked();
        this.mService.setFocusedActivityLocked(r, reason);
        if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
            Slog.v(TAG_TRANSITION, "Prepare to front transition: task=" + tr);
        }
        if (noAnimation) {
            this.mWindowManager.prepareAppTransition(0, false);
            if (r != null) {
                this.mNoAnimActivities.add(r);
            }
            ActivityOptions.abort(options);
        } else {
            updateTransitLocked(10, options);
        }
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        EventLog.writeEvent(EventLogTags.AM_TASK_TO_FRONT, Integer.valueOf(tr.userId), Integer.valueOf(tr.taskId));
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-AM_TASK_TO_FRONT: " + tr);
        }
    }

    final boolean moveTaskToBackLocked(int taskId) {
        ActivityStack fullscreenStack;
        TaskRecord tr = taskForIdLocked(taskId);
        if (tr == null) {
            Slog.i(TAG, "moveTaskToBack: bad taskId=" + taskId);
            return false;
        }
        Slog.i(TAG, "moveTaskToBack: " + tr);
        this.mStackSupervisor.removeLockedTaskLocked(tr);
        if (this.mStackSupervisor.isFrontStack(this) && this.mService.mController != null) {
            ActivityRecord next = topRunningActivityLocked(null, taskId);
            if (next == null) {
                next = topRunningActivityLocked(null, 0);
            }
            if (next != null) {
                boolean moveOK = true;
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
        if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
            Slog.v(TAG_TRANSITION, "Prepare to back transition: task=" + taskId);
        }
        if (this.mStackId == 0 && topTask().isHomeTask() && (fullscreenStack = this.mStackSupervisor.getStack(1)) != null && fullscreenStack.hasVisibleBehindActivity()) {
            ActivityRecord visibleBehind = fullscreenStack.getVisibleBehindActivity();
            this.mService.setFocusedActivityLocked(visibleBehind, "moveTaskToBack");
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            return true;
        }
        boolean prevIsHome = false;
        boolean canGoHome = !tr.isHomeTask() ? tr.isOverHomeStack() : false;
        if (canGoHome) {
            TaskRecord nextTask = getNextTask(tr);
            if (nextTask != null) {
                nextTask.setTaskToReturnTo(tr.getTaskToReturnTo());
            } else {
                prevIsHome = true;
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
        TaskRecord task2 = this.mResumedActivity != null ? this.mResumedActivity.task : null;
        if (prevIsHome || ((task2 == tr && canGoHome) || (numTasks <= 1 && isOnHomeDisplay()))) {
            if (!this.mService.mBooting && !this.mService.mBooted) {
                return false;
            }
            int taskToReturnTo = tr.getTaskToReturnTo();
            tr.setTaskToReturnTo(0);
            return this.mStackSupervisor.resumeHomeStackTask(taskToReturnTo, null, "moveTaskToBack");
        }
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return true;
    }

    static final void logStartActivity(int tag, ActivityRecord r, TaskRecord task) {
        Uri data = r.intent.getData();
        EventLog.writeEvent(tag, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName, r.intent.getAction(), r.intent.getType(), data != null ? data.toSafeString() : null, Integer.valueOf(r.intent.getFlags()));
    }

    void ensureVisibleActivitiesConfigurationLocked(ActivityRecord start, boolean preserveWindow) {
        if (start == null || !start.visible) {
            return;
        }
        TaskRecord startTask = start.task;
        boolean behindFullscreen = false;
        boolean updatedConfig = false;
        for (int taskIndex = this.mTaskHistory.indexOf(startTask); taskIndex >= 0; taskIndex--) {
            TaskRecord task = this.mTaskHistory.get(taskIndex);
            ArrayList<ActivityRecord> activities = task.mActivities;
            int activityIndex = start.task == task ? activities.indexOf(start) : activities.size() - 1;
            while (true) {
                if (activityIndex < 0) {
                    break;
                }
                ActivityRecord r = activities.get(activityIndex);
                if (r.visible || !r.stopped || r.state != ActivityState.STOPPED) {
                    updatedConfig |= ensureActivityConfigurationLocked(r, 0, preserveWindow);
                    if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.v(TAG_CONFIGURATION, "ensureVisibleActivitiesConfigurationLocked: updatedConfig=" + updatedConfig + " fullscreen=" + r.fullscreen + " visible=" + r.visible + " state=" + r.state + " stopped=" + r.stopped + " r=" + r + " task=" + task + " start=" + start + " preserveWindow=" + preserveWindow + " called by " + Debug.getCallers(4));
                    }
                    if (r.fullscreen) {
                        behindFullscreen = true;
                        break;
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION || !ActivityManagerService.IS_USER_BUILD) {
                    Slog.v(TAG_CONFIGURATION, "ensureVisibleActivitiesConfigurationLocked: skip r=" + r + " task=" + task + " start=" + start + " called by " + Debug.getCallers(4));
                }
                activityIndex--;
            }
            if (behindFullscreen) {
                break;
            }
        }
        if (updatedConfig) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    boolean ensureActivityConfigurationLocked(ActivityRecord r, int globalChanges, boolean preserveWindow) {
        if (this.mConfigWillChange) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check (will change): " + r);
            }
            return true;
        }
        if (this.mService.isSleepingOrShuttingDownLocked() && r.state == ActivityState.STOPPED) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Skipping config check (stopped while sleeping): " + r);
            }
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Ensuring correct configuration: " + r);
        }
        Configuration newConfig = this.mService.mConfiguration;
        r.task.sanitizeOverrideConfiguration(newConfig);
        Configuration taskConfig = r.task.mOverrideConfig;
        if (r.configuration.equals(newConfig) && r.taskConfigOverride.equals(taskConfig) && !r.forceNewConfig) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration unchanged in " + r);
            }
            return true;
        }
        if (r.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration doesn't matter in finishing " + r);
            }
            r.stopFreezingScreenLocked(false);
            return true;
        }
        Configuration oldConfig = r.configuration;
        Configuration oldTaskOverride = r.taskConfigOverride;
        r.configuration = newConfig;
        r.taskConfigOverride = taskConfig;
        int taskChanges = getTaskConfigurationChanges(r, taskConfig, oldTaskOverride);
        int changes = oldConfig.diff(newConfig) | taskChanges;
        if (changes == 0 && !r.forceNewConfig) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration no differences in " + r);
            }
            r.scheduleConfigurationChanged(taskConfig, true);
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Configuration changes for " + r + " ; taskChanges=" + Configuration.configurationDiffToString(taskChanges) + ", allChanges=" + Configuration.configurationDiffToString(changes));
        }
        if (r.app == null || r.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Configuration doesn't matter not running " + r);
            }
            r.stopFreezingScreenLocked(false);
            r.forceNewConfig = false;
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
            Slog.v(TAG_CONFIGURATION, "Checking to restart " + r.info.name + ": changed=0x" + Integer.toHexString(changes) + ", handles=0x" + Integer.toHexString(r.info.getRealConfigChanged()) + ", newConfig=" + newConfig + ", taskConfig=" + taskConfig);
        }
        if (((~r.info.getRealConfigChanged()) & changes) == 0 && !r.forceNewConfig) {
            r.scheduleConfigurationChanged(taskConfig, true);
            r.stopFreezingScreenLocked(false);
            return true;
        }
        r.configChangeFlags |= changes;
        r.startFreezingScreenLocked(r.app, globalChanges);
        r.forceNewConfig = false;
        boolean preserveWindow2 = preserveWindow & isResizeOnlyChange(changes);
        if (r.app == null || r.app.thread == null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                Slog.v(TAG_CONFIGURATION, "Config is destroying non-running " + r);
            }
            destroyActivityLocked(r, true, "config");
        } else {
            if (r.state == ActivityState.PAUSING) {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Config is skipping already pausing " + r);
                }
                r.deferRelaunchUntilPaused = true;
                r.preserveWindowOnDeferredRelaunch = preserveWindow2;
                return true;
            }
            if (r.state == ActivityState.RESUMED) {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Config is relaunching resumed " + r);
                }
                if (ActivityManagerDebugConfig.DEBUG_STATES && !r.visible) {
                    Slog.v(TAG_STATES, "Config is relaunching resumed invisible activity " + r + " called by " + Debug.getCallers(4));
                }
                relaunchActivityLocked(r, r.configChangeFlags, true, preserveWindow2);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Config is relaunching non-resumed " + r);
                }
                relaunchActivityLocked(r, r.configChangeFlags, false, preserveWindow2);
            }
        }
        return false;
    }

    private int getTaskConfigurationChanges(ActivityRecord record, Configuration taskConfig, Configuration oldTaskOverride) {
        boolean crosses;
        if (Configuration.EMPTY.equals(oldTaskOverride) && !Configuration.EMPTY.equals(taskConfig)) {
            oldTaskOverride = record.task.extractOverrideConfig(record.configuration);
        }
        if (Configuration.EMPTY.equals(taskConfig) && !Configuration.EMPTY.equals(oldTaskOverride)) {
            taskConfig = record.task.extractOverrideConfig(record.configuration);
        }
        int taskChanges = oldTaskOverride.diff(taskConfig);
        if ((taskChanges & 1024) != 0) {
            if (record.crossesHorizontalSizeThreshold(oldTaskOverride.screenWidthDp, taskConfig.screenWidthDp)) {
                crosses = true;
            } else {
                crosses = record.crossesVerticalSizeThreshold(oldTaskOverride.screenHeightDp, taskConfig.screenHeightDp);
            }
            if (!crosses) {
                taskChanges &= -1025;
            }
        }
        if ((taskChanges & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
            int oldSmallest = oldTaskOverride.smallestScreenWidthDp;
            int newSmallest = taskConfig.smallestScreenWidthDp;
            if (!record.crossesSmallestSizeThreshold(oldSmallest, newSmallest)) {
                taskChanges &= -2049;
            }
        }
        return catchConfigChangesFromUnset(taskConfig, oldTaskOverride, taskChanges);
    }

    private static int catchConfigChangesFromUnset(Configuration taskConfig, Configuration oldTaskOverride, int taskChanges) {
        if (taskChanges == 0) {
            if (oldTaskOverride.orientation != taskConfig.orientation) {
                taskChanges |= 128;
            }
            int oldHeight = oldTaskOverride.screenHeightDp;
            int newHeight = taskConfig.screenHeightDp;
            if ((oldHeight == 0 && newHeight != 0) || (oldHeight != 0 && newHeight == 0)) {
                taskChanges |= 1024;
            }
            int oldWidth = oldTaskOverride.screenWidthDp;
            int newWidth = taskConfig.screenWidthDp;
            if ((oldWidth == 0 && newWidth != 0) || (oldWidth != 0 && newWidth == 0)) {
                taskChanges |= 1024;
            }
            int oldSmallest = oldTaskOverride.smallestScreenWidthDp;
            int newSmallest = taskConfig.smallestScreenWidthDp;
            if ((oldSmallest == 0 && newSmallest != 0) || (oldSmallest != 0 && newSmallest == 0)) {
                taskChanges |= PackageManagerService.DumpState.DUMP_VERIFIERS;
            }
            int oldLayout = oldTaskOverride.screenLayout;
            int newLayout = taskConfig.screenLayout;
            if ((oldLayout == 0 && newLayout != 0) || (oldLayout != 0 && newLayout == 0)) {
                return taskChanges | 256;
            }
            return taskChanges;
        }
        return taskChanges;
    }

    private static boolean isResizeOnlyChange(int change) {
        return (change & (-3457)) == 0;
    }

    private void relaunchActivityLocked(ActivityRecord r, int changes, boolean andResume, boolean preserveWindow) {
        if (this.mService.mSuppressResizeConfigChanges && preserveWindow) {
            r.configChangeFlags = 0;
            return;
        }
        List<ResultInfo> results = null;
        List<ReferrerIntent> newIntents = null;
        if (andResume) {
            results = r.results;
            newIntents = r.newIntents;
        }
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || !ActivityManagerService.IS_USER_BUILD) {
            Slog.v(TAG_SWITCH, "ACT-Relaunching: " + r + " with results=" + results + " newIntents=" + newIntents + " andResume=" + andResume + " preserveWindow=" + preserveWindow);
        }
        EventLog.writeEvent(andResume ? EventLogTags.AM_RELAUNCH_RESUME_ACTIVITY : EventLogTags.AM_RELAUNCH_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName);
        r.startFreezingScreenLocked(r.app, 0);
        this.mStackSupervisor.removeChildActivityContainers(r);
        try {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.i(TAG_SWITCH, "Moving to " + (andResume ? "RESUMED" : "PAUSED") + " Relaunching " + r + " callers=" + Debug.getCallers(6));
            }
            r.forceNewConfig = false;
            this.mStackSupervisor.activityRelaunchingLocked(r);
            r.app.thread.scheduleRelaunchActivity(r.appToken, results, newIntents, changes, !andResume, new Configuration(this.mService.mConfiguration), new Configuration(r.task.mOverrideConfig), preserveWindow);
        } catch (RemoteException e) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.i(TAG_SWITCH, "Relaunch failed", e);
            }
        }
        if (andResume) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "Resumed after relaunch " + r);
            }
            r.state = ActivityState.RESUMED;
            ProcessRecord appProc = r.app;
            if (appProc != null) {
                AMEventHookData.AfterActivityResumed eventData = AMEventHookData.AfterActivityResumed.createInstance();
                eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName, Integer.valueOf(r.mActivityType)});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityResumed, eventData);
            }
            if (!r.visible || r.stopped) {
                this.mWindowManager.setAppVisibility(r.appToken, true);
                completeResumeLocked(r);
            } else {
                r.results = null;
                r.newIntents = null;
            }
            this.mService.showUnsupportedZoomDialogIfNeededLocked(r);
            this.mService.showAskCompatModeDialogLocked(r);
        } else {
            this.mHandler.removeMessages(101, r);
            r.state = ActivityState.PAUSED;
            ProcessRecord appProc2 = r.app;
            if (appProc2 != null) {
                AMEventHookData.AfterActivityPaused eventData2 = AMEventHookData.AfterActivityPaused.createInstance();
                eventData2.set(new Object[]{Integer.valueOf(appProc2.pid), r.info.name, r.info.packageName});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityPaused, eventData2);
            }
        }
        r.configChangeFlags = 0;
        r.deferRelaunchUntilPaused = false;
        r.preserveWindowOnDeferredRelaunch = false;
    }

    boolean willActivityBeVisibleLocked(IBinder token) {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.appToken == token) {
                    return true;
                }
                if (r.fullscreen && !r.finishing) {
                    return false;
                }
            }
        }
        ActivityRecord r2 = ActivityRecord.forTokenLocked(token);
        if (r2 == null) {
            return false;
        }
        if (r2.finishing) {
            Slog.e(TAG, "willActivityBeVisibleLocked: Returning false, would have returned true for r=" + r2);
        }
        return !r2.finishing;
    }

    void closeSystemDialogsLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = activities.get(activityNdx);
                if ((r.info.flags & 256) != 0) {
                    finishActivityLocked(r, 0, null, "close-sys", true);
                }
            }
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId) {
        boolean sameComponent;
        boolean didSomething = false;
        TaskRecord lastTask = null;
        ComponentName homeActivity = null;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            int numActivities = activities.size();
            int activityNdx = 0;
            while (activityNdx < numActivities) {
                ActivityRecord r = activities.get(activityNdx);
                if (r.packageName.equals(packageName) && (filterByClasses == null || filterByClasses.contains(r.realActivity.getClassName()))) {
                    sameComponent = true;
                } else {
                    sameComponent = packageName == null && r.userId == userId;
                }
                if ((userId == -1 || r.userId == userId) && ((sameComponent || r.task == lastTask) && (r.app == null || evenPersistent || !r.app.persistent))) {
                    if (!doit) {
                        if (!r.finishing) {
                            return true;
                        }
                    } else if (r.isHomeActivity()) {
                        if (homeActivity != null && homeActivity.equals(r.realActivity)) {
                            Slog.i(TAG, "Skip force-stop again " + r);
                        } else {
                            homeActivity = r.realActivity;
                            didSomething = true;
                            Slog.i(TAG, "  Force finishing activity " + r);
                            if (sameComponent) {
                            }
                            lastTask = r.task;
                            if (!finishActivityLocked(r, 0, null, "force-stop", true)) {
                            }
                        }
                    } else {
                        didSomething = true;
                        Slog.i(TAG, "  Force finishing activity " + r);
                        if (sameComponent) {
                            if (r.app != null) {
                                r.app.removed = true;
                            }
                            r.app = null;
                        }
                        lastTask = r.task;
                        if (!finishActivityLocked(r, 0, null, "force-stop", true)) {
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
        boolean focusedStack = this.mStackSupervisor.getFocusedStack() == this;
        boolean topTask = true;
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = this.mTaskHistory.get(taskNdx);
            if (task.getTopActivity() != null) {
                ActivityRecord r = null;
                ActivityRecord top = null;
                int numActivities = 0;
                int numRunning = 0;
                ArrayList<ActivityRecord> activities = task.mActivities;
                if (allowed || task.isHomeTask() || task.effectiveUid == callingUid) {
                    for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                        ActivityRecord tmp = activities.get(activityNdx);
                        if (!tmp.finishing) {
                            r = tmp;
                            if (top == null || top.state == ActivityState.INITIALIZING) {
                                top = tmp;
                                numRunning = 0;
                                numActivities = 0;
                            }
                            numActivities++;
                            if (tmp.app != null && tmp.app.thread != null) {
                                numRunning++;
                            }
                            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                                Slog.v(TAG, tmp.intent.getComponent().flattenToShortString() + ": task=" + tmp.task);
                            }
                        }
                    }
                    ActivityManager.RunningTaskInfo ci = new ActivityManager.RunningTaskInfo();
                    ci.id = task.taskId;
                    ci.stackId = this.mStackId;
                    ci.baseActivity = r.intent.getComponent();
                    ci.topActivity = top.intent.getComponent();
                    ci.lastActiveTime = task.lastActiveTime;
                    if (focusedStack && topTask) {
                        ci.lastActiveTime = System.currentTimeMillis();
                        topTask = false;
                    }
                    if (top.task != null) {
                        ci.description = top.task.lastDescription;
                    }
                    ci.numActivities = numActivities;
                    ci.numRunning = numRunning;
                    ci.isDockable = task.canGoInDockedStack();
                    ci.resizeMode = task.mResizeMode;
                    list.add(ci);
                }
            }
        }
    }

    public void unhandledBackLocked() {
        int top = this.mTaskHistory.size() - 1;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Performing unhandledBack(): top activity at " + top);
        }
        if (top < 0) {
            return;
        }
        ArrayList<ActivityRecord> activities = this.mTaskHistory.get(top).mActivities;
        int activityTop = activities.size() - 1;
        if (activityTop <= 0) {
            return;
        }
        finishActivityLocked(activities.get(activityTop), 0, null, "unhandled-back", true);
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        if (this.mPausingActivity != null && this.mPausingActivity.app == app) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_PAUSE, "App died while pausing: " + this.mPausingActivity);
            }
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
                    Slog.w(TAG, "  Force finishing activity " + r.intent.getComponent().flattenToShortString());
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
            printed |= ActivityStackSupervisor.dumpHistoryList(fd, pw, this.mTaskHistory.get(taskNdx).mActivities, "    ", "Hist", true, !dumpAll, dumpClient, dumpPackage, needSep, header, "    Task id #" + task.taskId + "\n    mFullscreen=" + task.mFullscreen + "\n    mBounds=" + task.mBounds + "\n    mMinWidth=" + task.mMinWidth + "\n    mMinHeight=" + task.mMinHeight + "\n    mLastNonFullscreenBounds=" + task.mLastNonFullscreenBounds);
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
        ActivityRecord starting = topRunningActivityLocked();
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            ArrayList<ActivityRecord> activities = this.mTaskHistory.get(taskNdx).mActivities;
            for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord a = activities.get(activityNdx);
                if (a.info.packageName.equals(packageName)) {
                    a.forceNewConfig = true;
                    if (starting != null && a == starting && a.visible) {
                        a.startFreezingScreenLocked(starting.app, 256);
                    }
                }
            }
        }
        return starting;
    }

    void removeTask(TaskRecord task, String reason) {
        removeTask(task, reason, 0);
    }

    void removeTask(TaskRecord task, String reason, int mode) {
        if (mode == 0) {
            this.mStackSupervisor.removeLockedTaskLocked(task);
            this.mWindowManager.removeTask(task.taskId);
            if (!ActivityManager.StackId.persistTaskBounds(this.mStackId)) {
                task.updateOverrideConfiguration(null);
            }
        }
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
        updateTaskMovement(task, true);
        if (mode == 0 && task.mActivities.isEmpty()) {
            boolean isVoiceSession = task.voiceSession != null;
            if (isVoiceSession) {
                try {
                    task.voiceSession.taskFinished(task.intent, task.taskId);
                } catch (RemoteException e) {
                }
            }
            if (task.autoRemoveFromRecents() || isVoiceSession) {
                this.mRecentTasks.remove(task);
                task.removedFromRecents();
            }
        }
        if (this.mTaskHistory.isEmpty()) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG_STACK, "removeTask: removing stack=" + this);
            }
            if (isOnHomeDisplay() && mode != 2 && this.mStackSupervisor.isFocusedStack(this)) {
                String myReason = reason + " leftTaskHistoryEmpty";
                if (this.mFullscreen || !adjustFocusToNextFocusableStackLocked(task.getTaskToReturnTo(), myReason)) {
                    this.mStackSupervisor.moveHomeStackToFront(myReason);
                }
            }
            if (this.mStacks != null) {
                this.mStacks.remove(this);
                this.mStacks.add(0, this);
            }
            if (!isHomeStack()) {
                this.mActivityContainer.onTaskListEmptyLocked();
            }
        }
        task.stack = null;
    }

    TaskRecord createTaskRecord(int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean toTop) {
        TaskRecord task = new TaskRecord(this.mService, taskId, info, intent, voiceSession, voiceInteractor);
        addTask(task, toTop, "createTaskRecord");
        boolean isLockscreenShown = this.mService.mLockScreenShown == 2;
        boolean showForAllUsers = (info.flags & 1024) != 0;
        if (!layoutTaskInStack(task, info.windowLayout) && this.mBounds != null && task.isResizeable() && (!isLockscreenShown || (isLockscreenShown && !showForAllUsers))) {
            task.updateOverrideConfiguration(this.mBounds);
        } else if (ActivityManagerDebugConfig.DEBUG_TASKS || !ActivityManagerService.IS_USER_BUILD) {
            Slog.d(TAG, "createTaskRecord: skip updateOverrideConfiguration mBounds=" + this.mBounds + " taskId=" + taskId + " " + task + " " + info + " isLockscreenShown=" + isLockscreenShown + " showForAllUsers=" + showForAllUsers + " callers=" + Debug.getCallers(4));
        }
        return task;
    }

    boolean layoutTaskInStack(TaskRecord task, ActivityInfo.WindowLayout windowLayout) {
        if (this.mTaskPositioner == null) {
            return false;
        }
        this.mTaskPositioner.updateDefaultBounds(task, this.mTaskHistory, windowLayout);
        return true;
    }

    ArrayList<TaskRecord> getAllTasks() {
        return new ArrayList<>(this.mTaskHistory);
    }

    void addTask(TaskRecord task, boolean toTop, String reason) {
        ActivityStack prevStack = preAddTask(task, reason, toTop);
        task.stack = this;
        if (toTop) {
            insertTaskAtTop(task, null);
        } else {
            this.mTaskHistory.add(0, task);
            updateTaskMovement(task, false);
        }
        postAddTask(task, prevStack);
    }

    void positionTask(TaskRecord task, int position) {
        ActivityRecord topRunningActivity = task.topRunningActivityLocked();
        boolean wasResumed = topRunningActivity == task.stack.mResumedActivity;
        ActivityStack prevStack = preAddTask(task, "positionTask", false);
        task.stack = this;
        insertTaskAtPosition(task, position);
        postAddTask(task, prevStack);
        if (!wasResumed) {
            return;
        }
        if (this.mResumedActivity != null) {
            Log.wtf(TAG, "mResumedActivity was already set when moving mResumedActivity from other stack to this stack mResumedActivity=" + this.mResumedActivity + " other mResumedActivity=" + topRunningActivity);
        }
        this.mResumedActivity = topRunningActivity;
    }

    private ActivityStack preAddTask(TaskRecord task, String reason, boolean toTop) {
        ActivityStack prevStack = task.stack;
        if (prevStack != null && prevStack != this) {
            prevStack.removeTask(task, reason, toTop ? 2 : 1);
        }
        return prevStack;
    }

    private void postAddTask(TaskRecord task, ActivityStack prevStack) {
        if (prevStack != null) {
            this.mStackSupervisor.scheduleReportPictureInPictureModeChangedIfNeeded(task, prevStack);
        } else {
            if (task.voiceSession == null) {
                return;
            }
            try {
                task.voiceSession.taskStarted(task.intent, task.taskId);
            } catch (RemoteException e) {
            }
        }
    }

    void addConfigOverride(ActivityRecord r, TaskRecord task) {
        Rect bounds = task.updateOverrideConfigurationFromLaunchBounds();
        this.mWindowManager.addAppToken(task.mActivities.indexOf(r), r.appToken, r.task.taskId, this.mStackId, r.info.screenOrientation, r.fullscreen, (r.info.flags & 1024) != 0, r.userId, r.info.configChanges, task.voiceSession != null, r.mLaunchTaskBehind, bounds, task.mOverrideConfig, task.mResizeMode, r.isAlwaysFocusable(), task.isHomeTask(), r.appInfo.targetSdkVersion);
        r.taskConfigOverride = task.mOverrideConfig;
    }

    void moveToFrontAndResumeStateIfNeeded(ActivityRecord r, boolean moveToFront, boolean setResume, String reason) {
        if (!moveToFront) {
            return;
        }
        if (setResume) {
            this.mResumedActivity = r;
        }
        moveToFront(reason);
    }

    void moveActivityToStack(ActivityRecord r) {
        boolean wasFocused = false;
        ActivityStack prevStack = r.task.stack;
        if (prevStack.mStackId == this.mStackId) {
            return;
        }
        if (this.mStackSupervisor.isFocusedStack(prevStack) && this.mStackSupervisor.topRunningActivityLocked() == r) {
            wasFocused = true;
        }
        boolean wasResumed = wasFocused && prevStack.mResumedActivity == r;
        TaskRecord task = createTaskRecord(this.mStackSupervisor.getNextTaskIdForUserLocked(r.userId), r.info, r.intent, null, null, true);
        r.setTask(task, null);
        task.addActivityToTop(r);
        setAppTask(r, task);
        this.mStackSupervisor.scheduleReportPictureInPictureModeChangedIfNeeded(task, prevStack);
        moveToFrontAndResumeStateIfNeeded(r, wasFocused, wasResumed, "moveActivityToStack");
        if (!wasResumed) {
            return;
        }
        prevStack.mResumedActivity = null;
    }

    private void setAppTask(ActivityRecord r, TaskRecord task) {
        Rect bounds = task.updateOverrideConfigurationFromLaunchBounds();
        this.mWindowManager.setAppTask(r.appToken, task.taskId, this.mStackId, bounds, task.mOverrideConfig, task.mResizeMode, task.isHomeTask());
        r.taskConfigOverride = task.mOverrideConfig;
    }

    public int getStackId() {
        return this.mStackId;
    }

    public String toString() {
        return "ActivityStack{" + Integer.toHexString(System.identityHashCode(this)) + " stackId=" + this.mStackId + ", " + this.mTaskHistory.size() + " tasks}";
    }

    void onLockTaskPackagesUpdatedLocked() {
        for (int taskNdx = this.mTaskHistory.size() - 1; taskNdx >= 0; taskNdx--) {
            this.mTaskHistory.get(taskNdx).setLockTaskAuth();
        }
    }

    private boolean skipStartActivityIfNeeded() {
        AMEventHookData.SkipStartActivity eventData = AMEventHookData.SkipStartActivity.createInstance();
        AMEventHookResult eventResult = this.mService.getAMEventHook().hook(AMEventHook.Event.AM_SkipStartActivity, eventData);
        if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_SkipStartActivity)) {
            return true;
        }
        return false;
    }

    void keepStickyTaskLocked() {
        ActivityStack stack = this.mStackSupervisor.findStack(2);
        if (MultiWindowManager.DEBUG) {
            Slog.d(TAG_STACK, "keepStickyTaskLocked, stack = " + stack);
        }
        if (stack == null || this != stack) {
            return;
        }
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        for (int i = 0; i < tasks.size(); i++) {
            TaskRecord task = tasks.get(i);
            if (MultiWindowManager.DEBUG) {
                Slog.d(TAG_STACK, "keepStickyTaskLocked, task = " + task);
            }
            if (task != null && task.mSticky) {
                insertTaskAtTop(task, null);
                this.mWindowManager.moveTaskToTop(task.taskId);
            }
        }
    }

    void restoreStickyTaskLocked(TaskRecord topTask) {
        ActivityStack stack = this.mStackSupervisor.findStack(2);
        if (MultiWindowManager.DEBUG) {
            Slog.d(TAG_STACK, "restoreStickyTaskLocked, topTask = " + topTask + ", stack = " + stack);
        }
        if (topTask == null || stack == null || topTask.stack == stack) {
            return;
        }
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskRecord task = tasks.get(i);
            if (MultiWindowManager.DEBUG) {
                Slog.d(TAG_STACK, "restoreStickyTaskLocked, task = " + task);
            }
            if (task != null && task.mSticky) {
                this.mService.stickWindow(task, false);
            }
        }
    }

    static int getMaxStoppingToForce() {
        return 3;
    }
}
