package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityContainer;
import android.app.IActivityContainerCallback;
import android.app.IActivityManager;
import android.app.ProfilerInfo;
import android.app.ResultInfo;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManagerInternal;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.TransactionTooLargeException;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.InputEvent;
import android.view.Surface;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.TransferPipe;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.am.ActivityStack;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.am.AMEventHookData;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.server.am.AMEventHook;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ActivityStackSupervisor implements DisplayManager.DisplayListener {
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    static final int CONTAINER_CALLBACK_TASK_LIST_EMPTY = 111;
    static final int CONTAINER_CALLBACK_VISIBILITY = 108;
    static final boolean CREATE_IF_NEEDED = true;
    static final boolean DEFER_RESUME = true;
    private static final int FIT_WITHIN_BOUNDS_DIVIDER = 3;
    static final boolean FORCE_FOCUS = true;
    static final int HANDLE_DISPLAY_ADDED = 105;
    static final int HANDLE_DISPLAY_CHANGED = 106;
    static final int HANDLE_DISPLAY_REMOVED = 107;
    static final int IDLE_NOW_MSG = 101;
    static final int IDLE_TIMEOUT = 10000;
    static final int IDLE_TIMEOUT_MSG = 100;
    static final int LAUNCH_TASK_BEHIND_COMPLETE = 112;
    static final int LAUNCH_TIMEOUT = 10000;
    static final int LAUNCH_TIMEOUT_MSG = 104;
    static final int LOCK_TASK_END_MSG = 110;
    static final int LOCK_TASK_START_MSG = 109;
    private static final String LOCK_TASK_TAG = "Lock-to-App";
    private static final int MAX_TASK_IDS_PER_USER = 100000;
    static final boolean MOVING = true;
    static final boolean ON_TOP = true;
    static final boolean PRESERVE_WINDOWS = true;
    static final int REPORT_MULTI_WINDOW_MODE_CHANGED_MSG = 114;
    static final int REPORT_PIP_MODE_CHANGED_MSG = 115;
    static final boolean RESTORE_FROM_RECENTS = true;
    static final int RESUME_TOP_ACTIVITY_MSG = 102;
    static final int SHOW_LOCK_TASK_ESCAPE_MESSAGE_MSG = 113;
    static final int SLEEP_TIMEOUT = 5000;
    static final int SLEEP_TIMEOUT_MSG = 103;
    static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private static final String VIRTUAL_DISPLAY_BASE_NAME = "ActivityViewVirtualDisplay";
    boolean inResumeTopActivity;
    final ActivityMetricsLogger mActivityMetricsLogger;
    boolean mAppVisibilitiesChangedSinceLastPause;
    int mCurrentUser;
    private IDevicePolicyManager mDevicePolicyManager;
    DisplayManager mDisplayManager;
    ActivityStack mFocusedStack;
    PowerManager.WakeLock mGoingToSleep;
    final ActivityStackSupervisorHandler mHandler;
    ActivityStack mHomeStack;
    InputManagerInternal mInputManagerInternal;
    boolean mIsDockMinimized;
    private ActivityStack mLastFocusedStack;
    PowerManager.WakeLock mLaunchingActivity;
    private int mLockTaskModeState;
    private LockTaskNotify mLockTaskNotify;
    private RecentTasks mRecentTasks;
    private final ResizeDockedStackTimeout mResizeDockedStackTimeout;
    final ActivityManagerService mService;
    private IStatusBarService mStatusBarService;
    WindowManagerService mWindowManager;
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONTAINERS = TAG + ActivityManagerDebugConfig.POSTFIX_CONTAINERS;
    private static final String TAG_IDLE = TAG + ActivityManagerDebugConfig.POSTFIX_IDLE;
    private static final String TAG_LOCKTASK = TAG + ActivityManagerDebugConfig.POSTFIX_LOCKTASK;
    private static final String TAG_PAUSE = TAG + ActivityManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_RELEASE = TAG + ActivityManagerDebugConfig.POSTFIX_RELEASE;
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static final String TAG_VISIBLE_BEHIND = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBLE_BEHIND;
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION = new ArrayMap<>();
    private IBinder mToken = new Binder();
    private int mNextFreeStackId = 5;
    private final SparseIntArray mCurTaskIdForUser = new SparseIntArray(20);
    final ArrayList<ActivityRecord> mWaitingVisibleActivities = new ArrayList<>();
    final ArrayList<IActivityManager.WaitResult> mWaitingActivityVisible = new ArrayList<>();
    final ArrayList<IActivityManager.WaitResult> mWaitingActivityLaunched = new ArrayList<>();
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mGoingToSleepActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mMultiWindowModeChangedActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mPipModeChangedActivities = new ArrayList<>();
    final ArrayList<UserState> mStartingUsers = new ArrayList<>();
    boolean mUserLeaving = false;
    boolean mSleepTimeout = false;
    SparseIntArray mUserStackInFront = new SparseIntArray(2);
    private SparseArray<ActivityContainer> mActivityContainers = new SparseArray<>();
    private final SparseArray<ActivityDisplay> mActivityDisplays = new SparseArray<>();
    ArrayList<TaskRecord> mLockTaskModeTasks = new ArrayList<>();
    private final Rect tempRect = new Rect();
    private final Rect tempRect2 = new Rect();
    private final SparseArray<Configuration> mTmpConfigs = new SparseArray<>();
    private final SparseArray<Rect> mTmpBounds = new SparseArray<>();
    private final SparseArray<Rect> mTmpInsetBounds = new SparseArray<>();
    int mDefaultMinSizeOfResizeableTask = -1;
    private boolean mTaskLayersChanged = true;
    private final FindTaskResult mTmpFindTaskResult = new FindTaskResult();
    private final ArraySet<Integer> mResizingTasksDuringAnimation = new ArraySet<>();
    private boolean mAllowDockedStackResize = true;
    SimpleActivityInfo mLastResumedActivity = new SimpleActivityInfo();

    static {
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.IMAGE_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.VIDEO_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.intent.action.CALL", "android.permission.CALL_PHONE");
    }

    static class FindTaskResult {
        boolean matchedByRootAffinity;
        ActivityRecord r;

        FindTaskResult() {
        }
    }

    static class PendingActivityLaunch {
        final ProcessRecord callerApp;
        final ActivityRecord r;
        final ActivityRecord sourceRecord;
        final ActivityStack stack;
        final int startFlags;

        PendingActivityLaunch(ActivityRecord _r, ActivityRecord _sourceRecord, int _startFlags, ActivityStack _stack, ProcessRecord _callerApp) {
            this.r = _r;
            this.sourceRecord = _sourceRecord;
            this.startFlags = _startFlags;
            this.stack = _stack;
            this.callerApp = _callerApp;
        }

        void sendErrorResult(String message) {
            try {
                if (this.callerApp.thread == null) {
                    return;
                }
                this.callerApp.thread.scheduleCrash(message);
            } catch (RemoteException e) {
                Slog.e(ActivityStackSupervisor.TAG, "Exception scheduling crash of failed activity launcher sourceRecord=" + this.sourceRecord, e);
            }
        }
    }

    public ActivityStackSupervisor(ActivityManagerService service) {
        this.mService = service;
        this.mHandler = new ActivityStackSupervisorHandler(this.mService.mHandler.getLooper());
        this.mActivityMetricsLogger = new ActivityMetricsLogger(this, this.mService.mContext);
        this.mResizeDockedStackTimeout = new ResizeDockedStackTimeout(service, this, this.mHandler);
    }

    void setRecentTasks(RecentTasks recentTasks) {
        this.mRecentTasks = recentTasks;
    }

    void initPowerManagement() {
        PowerManager pm = (PowerManager) this.mService.mContext.getSystemService("power");
        this.mGoingToSleep = pm.newWakeLock(1, "ActivityManager-Sleep");
        this.mLaunchingActivity = pm.newWakeLock(1, "*launch*");
        this.mLaunchingActivity.setReferenceCounted(false);
    }

    private IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStatusBarService == null) {
                    this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
                    if (this.mStatusBarService == null) {
                        Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
                    }
                }
                iStatusBarService = this.mStatusBarService;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return iStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        IDevicePolicyManager iDevicePolicyManager;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mDevicePolicyManager == null) {
                    this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
                    if (this.mDevicePolicyManager == null) {
                        Slog.w(TAG, "warning: no DEVICE_POLICY_SERVICE");
                    }
                }
                iDevicePolicyManager = this.mDevicePolicyManager;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return iDevicePolicyManager;
    }

    void setWindowManager(WindowManagerService wm) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mWindowManager = wm;
                this.mDisplayManager = (DisplayManager) this.mService.mContext.getSystemService("display");
                this.mDisplayManager.registerDisplayListener(this, null);
                Display[] displays = this.mDisplayManager.getDisplays();
                for (int displayNdx = displays.length - 1; displayNdx >= 0; displayNdx--) {
                    int displayId = displays[displayNdx].getDisplayId();
                    ActivityDisplay activityDisplay = new ActivityDisplay(displayId);
                    if (activityDisplay.mDisplay == null) {
                        throw new IllegalStateException("Default Display does not exist");
                    }
                    this.mActivityDisplays.put(displayId, activityDisplay);
                    calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
                }
                ActivityStack stack = getStack(0, true, true);
                this.mLastFocusedStack = stack;
                this.mFocusedStack = stack;
                this.mHomeStack = stack;
                this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void notifyActivityDrawnForKeyguard() {
        if (ActivityManagerDebugConfig.DEBUG_LOCKSCREEN) {
            this.mService.logLockScreen("");
        }
        this.mWindowManager.notifyActivityDrawnForKeyguard();
    }

    ActivityStack getFocusedStack() {
        return this.mFocusedStack;
    }

    ActivityStack getLastStack() {
        return this.mLastFocusedStack;
    }

    boolean isFocusedStack(ActivityStack stack) {
        if (stack == null) {
            return false;
        }
        ActivityRecord parent = stack.mActivityContainer.mParentActivity;
        if (parent != null) {
            stack = parent.task.stack;
        }
        return stack == this.mFocusedStack;
    }

    boolean isFrontStack(ActivityStack stack) {
        if (stack == null) {
            return false;
        }
        ActivityRecord parent = stack.mActivityContainer.mParentActivity;
        if (parent != null) {
            stack = parent.task.stack;
        }
        return stack == this.mHomeStack.mStacks.get(this.mHomeStack.mStacks.size() + (-1));
    }

    void setFocusStackUnchecked(String reason, ActivityStack focusCandidate) {
        if (!focusCandidate.isFocusable()) {
            focusCandidate = focusCandidate.getNextFocusableStackLocked();
        }
        if (focusCandidate != this.mFocusedStack) {
            this.mLastFocusedStack = this.mFocusedStack;
            this.mFocusedStack = focusCandidate;
            EventLogTags.writeAmFocusedStack(this.mCurrentUser, this.mFocusedStack == null ? -1 : this.mFocusedStack.getStackId(), this.mLastFocusedStack != null ? this.mLastFocusedStack.getStackId() : -1, reason);
        }
        ActivityRecord r = topRunningActivityLocked();
        if (!this.mService.mDoingSetFocusedActivity && this.mService.mFocusedActivity != r) {
            this.mService.setFocusedActivityLocked(r, reason + " setFocusStack");
        }
        if ((!this.mService.mBooting && this.mService.mBooted) || r == null || !r.idle) {
            return;
        }
        checkFinishBootingLocked();
    }

    void moveHomeStackToFront(String reason) {
        this.mHomeStack.moveToFront(reason);
    }

    boolean moveHomeStackTaskToTop(int homeStackTaskType, String reason) {
        if (homeStackTaskType == 2) {
            this.mWindowManager.showRecentApps(false);
            return false;
        }
        this.mHomeStack.moveHomeStackTaskToTop(homeStackTaskType);
        ActivityRecord top = getHomeActivity();
        if (top == null) {
            return false;
        }
        this.mService.setFocusedActivityLocked(top, reason);
        return true;
    }

    boolean resumeHomeStackTask(int homeStackTaskType, ActivityRecord prev, String reason) {
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        if (homeStackTaskType == 2) {
            this.mWindowManager.showRecentApps(false);
            return false;
        }
        if (prev != null) {
            prev.task.setTaskToReturnTo(0);
        }
        this.mHomeStack.moveHomeStackTaskToTop(homeStackTaskType);
        ActivityRecord r = getHomeActivity();
        String myReason = reason + " resumeHomeStackTask";
        if (r != null && !r.finishing) {
            this.mService.setFocusedActivityLocked(r, myReason);
            return resumeFocusedStackTopActivityLocked(this.mHomeStack, prev, null);
        }
        return this.mService.startHomeActivityLocked(this.mCurrentUser, myReason);
    }

    TaskRecord anyTaskForIdLocked(int id) {
        return anyTaskForIdLocked(id, true, -1);
    }

    TaskRecord anyTaskForIdLocked(int id, boolean restoreFromRecents, int stackId) {
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                TaskRecord task = stack.taskForIdLocked(id);
                if (task != null) {
                    return task;
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Looking for task id=" + id + " in recents");
        }
        TaskRecord task2 = this.mRecentTasks.taskForIdLocked(id);
        if (task2 == null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "\tDidn't find task id=" + id + " in recents");
            }
            return null;
        }
        if (!restoreFromRecents) {
            return task2;
        }
        if (!restoreRecentTaskLocked(task2, stackId)) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.w(TAG_RECENTS, "Couldn't restore task id=" + id + " found in recents");
            }
            return null;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.w(TAG_RECENTS, "Restored task id=" + id + " from in recents");
        }
        return task2;
    }

    ActivityRecord isInAnyStackLocked(IBinder token) {
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord r = stacks.get(stackNdx).isInStackLocked(token);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    boolean isUserLockedProfile(int userId) {
        if (!this.mService.mUserController.shouldConfirmCredentials(userId)) {
            return false;
        }
        ActivityStack[] activityStacks = {getStack(3), getStack(2), getStack(1)};
        for (ActivityStack activityStack : activityStacks) {
            if (activityStack != null && activityStack.topRunningActivityLocked() != null && activityStack.getStackVisibilityLocked(null) != 0 && (!activityStack.isDockedStack() || !this.mIsDockMinimized)) {
                if (activityStack.mStackId == 2) {
                    List<TaskRecord> tasks = activityStack.getAllTasks();
                    int size = tasks.size();
                    for (int i = 0; i < size; i++) {
                        if (taskContainsActivityFromUser(tasks.get(i), userId)) {
                            return true;
                        }
                    }
                } else {
                    TaskRecord topTask = activityStack.topTask();
                    if (topTask != null && taskContainsActivityFromUser(topTask, userId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean taskContainsActivityFromUser(TaskRecord task, int userId) {
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord activityRecord = task.mActivities.get(i);
            if (activityRecord.userId == userId) {
                return true;
            }
        }
        return false;
    }

    void setNextTaskIdForUserLocked(int taskId, int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, -1);
        if (taskId <= currentTaskId) {
            return;
        }
        this.mCurTaskIdForUser.put(userId, taskId);
    }

    int getNextTaskIdForUserLocked(int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, userId * MAX_TASK_IDS_PER_USER);
        int candidateTaskId = currentTaskId;
        do {
            if (this.mRecentTasks.taskIdTakenForUserLocked(candidateTaskId, userId) || anyTaskForIdLocked(candidateTaskId, false, -1) != null) {
                candidateTaskId++;
                if (candidateTaskId == (userId + 1) * MAX_TASK_IDS_PER_USER) {
                    candidateTaskId -= MAX_TASK_IDS_PER_USER;
                }
            } else {
                this.mCurTaskIdForUser.put(userId, candidateTaskId);
                return candidateTaskId;
            }
        } while (candidateTaskId != currentTaskId);
        throw new IllegalStateException("Cannot get an available task id. Reached limit of 100000 running tasks per user.");
    }

    ActivityRecord resumedAppLocked() {
        ActivityStack stack = this.mFocusedStack;
        if (stack == null) {
            return null;
        }
        ActivityRecord resumedActivity = stack.mResumedActivity;
        if (resumedActivity == null || resumedActivity.app == null) {
            ActivityRecord resumedActivity2 = stack.mPausingActivity;
            if (resumedActivity2 == null || resumedActivity2.app == null) {
                return stack.topRunningActivityLocked();
            }
            return resumedActivity2;
        }
        return resumedActivity;
    }

    boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
        ActivityRecord hr;
        String processName = app.processName;
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (isFocusedStack(stack) && (hr = stack.topRunningActivityLocked()) != null && hr.app == null && app.uid == hr.info.applicationInfo.uid && processName.equals(hr.processName)) {
                    try {
                        if (realStartActivityLocked(hr, app, true, true)) {
                            didSomething = true;
                        }
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Exception in new application when starting activity " + hr.intent.getComponent().flattenToShortString(), e);
                        throw e;
                    }
                }
            }
        }
        if (!didSomething) {
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        return didSomething;
    }

    boolean allResumedActivitiesIdle() {
        ActivityRecord resumedActivity;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (isFocusedStack(stack) && stack.numActivities() != 0 && ((resumedActivity = stack.mResumedActivity) == null || !resumedActivity.idle)) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "allResumedActivitiesIdle: stack=" + stack.mStackId + " " + resumedActivity + " not idle");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    boolean allResumedActivitiesComplete() {
        ActivityRecord r;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (isFocusedStack(stack) && (r = stack.mResumedActivity) != null && r.state != ActivityStack.ActivityState.RESUMED) {
                    return false;
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "allResumedActivitiesComplete: mLastFocusedStack changing from=" + this.mLastFocusedStack + " to=" + this.mFocusedStack);
        }
        this.mLastFocusedStack = this.mFocusedStack;
        return true;
    }

    boolean allResumedActivitiesVisible() {
        boolean foundResumed = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ActivityRecord r = stack.mResumedActivity;
                if (r != null) {
                    if (!r.nowVisible || this.mWaitingVisibleActivities.contains(r)) {
                        return false;
                    }
                    foundResumed = true;
                }
            }
        }
        return foundResumed;
    }

    boolean pauseBackStacks(boolean userLeaving, boolean resuming, boolean dontWait) {
        boolean someActivityPaused = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (!isFocusedStack(stack) && stack.mResumedActivity != null) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "pauseBackStacks: stack=" + stack + " mResumedActivity=" + stack.mResumedActivity);
                    }
                    someActivityPaused |= stack.startPausingLocked(userLeaving, false, resuming, dontWait);
                }
            }
        }
        return someActivityPaused;
    }

    boolean allPausedActivitiesComplete() {
        boolean pausing = true;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ActivityRecord r = stack.mPausingActivity;
                if (r != null && r.state != ActivityStack.ActivityState.PAUSED && r.state != ActivityStack.ActivityState.STOPPED && r.state != ActivityStack.ActivityState.STOPPING) {
                    if (!ActivityManagerDebugConfig.DEBUG_STATES) {
                        return false;
                    }
                    Slog.d(TAG_STATES, "allPausedActivitiesComplete: r=" + r + " state=" + r.state);
                    pausing = false;
                }
            }
        }
        return pausing;
    }

    void pauseChildStacks(ActivityRecord parent, boolean userLeaving, boolean uiSleeping, boolean resuming, boolean dontWait) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (stack.mResumedActivity != null && stack.mActivityContainer.mParentActivity == parent) {
                    stack.startPausingLocked(userLeaving, uiSleeping, resuming, dontWait);
                }
            }
        }
    }

    void cancelInitializingActivities() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                stacks.get(stackNdx).cancelInitializingActivities();
            }
        }
    }

    void reportActivityVisibleLocked(ActivityRecord r) {
        sendWaitingVisibleReportLocked(r);
    }

    void sendWaitingVisibleReportLocked(ActivityRecord r) {
        boolean changed = false;
        for (int i = this.mWaitingActivityVisible.size() - 1; i >= 0; i--) {
            IActivityManager.WaitResult w = this.mWaitingActivityVisible.get(i);
            if (w.who == null) {
                changed = true;
                w.timeout = false;
                if (r != null) {
                    w.who = new ComponentName(r.info.packageName, r.info.name);
                }
                w.totalTime = SystemClock.uptimeMillis() - w.thisTime;
                w.thisTime = w.totalTime;
            }
        }
        if (!changed) {
            return;
        }
        this.mService.notifyAll();
    }

    void reportTaskToFrontNoLaunch(ActivityRecord r) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            IActivityManager.WaitResult w = this.mWaitingActivityLaunched.remove(i);
            if (w.who == null) {
                changed = true;
                w.result = 2;
            }
        }
        if (!changed) {
            return;
        }
        this.mService.notifyAll();
    }

    void reportActivityLaunchedLocked(boolean timeout, ActivityRecord r, long thisTime, long totalTime) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            IActivityManager.WaitResult w = this.mWaitingActivityLaunched.remove(i);
            if (w.who == null) {
                changed = true;
                w.timeout = timeout;
                if (r != null) {
                    w.who = new ComponentName(r.info.packageName, r.info.name);
                }
                w.thisTime = thisTime;
                w.totalTime = totalTime;
            }
        }
        if (!changed) {
            return;
        }
        this.mService.notifyAll();
    }

    ActivityRecord topRunningActivityLocked() {
        ActivityRecord r;
        ActivityStack focusedStack = this.mFocusedStack;
        ActivityRecord r2 = focusedStack.topRunningActivityLocked();
        if (r2 != null) {
            return r2;
        }
        ArrayList<ActivityStack> stacks = this.mHomeStack.mStacks;
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = stacks.get(stackNdx);
            if (stack != focusedStack && isFrontStack(stack) && stack.isFocusable() && (r = stack.topRunningActivityLocked()) != null) {
                return r;
            }
        }
        return null;
    }

    void getTasksLocked(int maxNum, List<ActivityManager.RunningTaskInfo> list, int callingUid, boolean allowed) {
        ArrayList<ArrayList<ActivityManager.RunningTaskInfo>> runningTaskLists = new ArrayList<>();
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ArrayList<ActivityManager.RunningTaskInfo> stackTaskList = new ArrayList<>();
                runningTaskLists.add(stackTaskList);
                stack.getTasksLocked(stackTaskList, callingUid, allowed);
            }
        }
        while (maxNum > 0) {
            long mostRecentActiveTime = Long.MIN_VALUE;
            ArrayList<ActivityManager.RunningTaskInfo> selectedStackList = null;
            int numTaskLists = runningTaskLists.size();
            for (int stackNdx2 = 0; stackNdx2 < numTaskLists; stackNdx2++) {
                ArrayList<ActivityManager.RunningTaskInfo> stackTaskList2 = runningTaskLists.get(stackNdx2);
                if (!stackTaskList2.isEmpty()) {
                    long lastActiveTime = stackTaskList2.get(0).lastActiveTime;
                    if (lastActiveTime > mostRecentActiveTime) {
                        mostRecentActiveTime = lastActiveTime;
                        selectedStackList = stackTaskList2;
                    }
                }
            }
            if (selectedStackList == null) {
                return;
            }
            list.add(selectedStackList.remove(0));
            maxNum--;
        }
    }

    ActivityInfo resolveActivity(Intent intent, ResolveInfo rInfo, int startFlags, ProfilerInfo profilerInfo) {
        ActivityInfo aInfo = rInfo != null ? rInfo.activityInfo : null;
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            if (!aInfo.processName.equals("system")) {
                if ((startFlags & 2) != 0) {
                    this.mService.setDebugApp(aInfo.processName, true, false);
                }
                if ((startFlags & 8) != 0) {
                    this.mService.setNativeDebuggingAppLocked(aInfo.applicationInfo, aInfo.processName);
                }
                if ((startFlags & 4) != 0) {
                    this.mService.setTrackAllocationApp(aInfo.applicationInfo, aInfo.processName);
                }
                if (profilerInfo != null) {
                    this.mService.setProfileApp(aInfo.applicationInfo, aInfo.processName, profilerInfo);
                }
            }
        }
        return aInfo;
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId) {
        return resolveIntent(intent, resolvedType, userId, 0);
    }

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags) {
        try {
            return AppGlobals.getPackageManager().resolveIntent(intent, resolvedType, 65536 | flags | 1024, userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    ActivityInfo resolveActivity(Intent intent, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId) {
        ResolveInfo rInfo = resolveIntent(intent, resolvedType, userId);
        return resolveActivity(intent, rInfo, startFlags, profilerInfo);
    }

    final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig) throws RemoteException {
        if (!allPausedActivitiesComplete()) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_PAUSE, "realStartActivityLocked: Skipping start of r=" + r + " some activities pausing...");
                return false;
            }
            return false;
        }
        if (andResume) {
            r.startFreezingScreenLocked(app, 0);
            this.mWindowManager.setAppVisibility(r.appToken, true);
            r.startLaunchTickingLocked();
        }
        if (checkConfig) {
            Configuration config = this.mWindowManager.updateOrientationFromAppTokens(this.mService.mConfiguration, r.mayFreezeScreenLocked(app) ? r.appToken : null);
            this.mService.updateConfigurationLocked(config, r, false);
        }
        r.app = app;
        app.waitingToKill = null;
        r.launchCount++;
        r.lastLaunchTime = SystemClock.uptimeMillis();
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "ACT-Launching: " + r);
        }
        int idx = app.activities.indexOf(r);
        if (idx < 0) {
            app.activities.add(r);
        }
        this.mService.updateLruProcessLocked(app, true, null);
        this.mService.updateOomAdjLocked();
        TaskRecord task = r.task;
        if (task.mLockTaskAuth == 2 || task.mLockTaskAuth == 4) {
            setLockTaskModeLocked(task, 1, "mLockTaskAuth==LAUNCHABLE", false);
        }
        ActivityStack stack = task.stack;
        try {
            if (app.thread == null) {
                throw new RemoteException();
            }
            List<ResultInfo> results = null;
            List<ReferrerIntent> newIntents = null;
            if (andResume) {
                results = r.results;
                newIntents = r.newIntents;
            }
            if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Launching: " + r + " icicle=" + r.icicle + " with results=" + results + " newIntents=" + newIntents + " andResume=" + andResume);
            }
            if (andResume) {
                EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(task.taskId), r.shortComponentName);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG, "ACT-AM_RESTART_ACTIVITY " + r + " Task:" + r.task.taskId);
                }
            }
            if (r.isHomeActivity()) {
                this.mService.mHomeProcess = task.mActivities.get(0).app;
            }
            this.mService.notifyPackageUse(r.intent.getComponent().getPackageName(), 0);
            r.sleeping = false;
            r.forceNewConfig = false;
            this.mService.showUnsupportedZoomDialogIfNeededLocked(r);
            this.mService.showAskCompatModeDialogLocked(r);
            r.compat = this.mService.compatibilityInfoForPackageLocked(r.info.applicationInfo);
            ProfilerInfo profilerInfo = null;
            if (this.mService.mProfileApp != null && this.mService.mProfileApp.equals(app.processName) && (this.mService.mProfileProc == null || this.mService.mProfileProc == app)) {
                this.mService.mProfileProc = app;
                String profileFile = this.mService.mProfileFile;
                if (profileFile != null) {
                    ParcelFileDescriptor profileFd = this.mService.mProfileFd;
                    if (profileFd != null) {
                        try {
                            profileFd = profileFd.dup();
                        } catch (IOException e) {
                            if (profileFd != null) {
                                try {
                                    profileFd.close();
                                } catch (IOException e2) {
                                }
                                profileFd = null;
                            }
                        }
                    }
                    profilerInfo = new ProfilerInfo(profileFile, profileFd, this.mService.mSamplingInterval, this.mService.mAutoStopProfiler);
                }
            }
            if (andResume) {
                app.hasShownUi = true;
                app.pendingUiClean = true;
            }
            app.forceProcessStateUpTo(this.mService.mTopProcessState);
            app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken, System.identityHashCode(r), r.info, new Configuration(this.mService.mConfiguration), new Configuration(task.mOverrideConfig), r.compat, r.launchedFromPackage, task.voiceInteractor, app.repProcState, r.icicle, r.persistentState, results, newIntents, !andResume, this.mService.isNextTransitionForward(), profilerInfo);
            if ((app.info.privateFlags & 2) != 0 && app.processName.equals(app.info.packageName)) {
                if (this.mService.mHeavyWeightProcess != null && this.mService.mHeavyWeightProcess != app) {
                    Slog.w(TAG, "Starting new heavy weight process " + app + " when already running " + this.mService.mHeavyWeightProcess);
                }
                this.mService.mHeavyWeightProcess = app;
                Message msg = this.mService.mHandler.obtainMessage(24);
                msg.obj = r;
                this.mService.mHandler.sendMessage(msg);
            }
            r.launchFailed = false;
            if (stack.updateLRUListLocked(r)) {
                Slog.w(TAG, "Activity " + r + " being launched, but already in LRU list");
            }
            if (andResume) {
                stack.minimalResumeActivityLocked(r);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to PAUSED: " + r + " (starting in paused state)");
                }
                r.state = ActivityStack.ActivityState.PAUSED;
                ProcessRecord appProc = r.app;
                if (appProc != null) {
                    AMEventHookData.AfterActivityPaused eventData = AMEventHookData.AfterActivityPaused.createInstance();
                    eventData.set(new Object[]{Integer.valueOf(appProc.pid), r.info.name, r.info.packageName});
                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_AfterActivityPaused, eventData);
                }
            }
            if (isFocusedStack(stack)) {
                this.mService.startSetupActivityLocked();
            }
            if (r.app != null) {
                this.mService.mServices.updateServiceConnectionActivitiesLocked(r.app);
                return true;
            }
            return true;
        } catch (RemoteException e3) {
            if (r.launchFailed) {
                Slog.e(TAG, "Second failure launching " + r.intent.getComponent().flattenToShortString() + ", giving up", e3);
                this.mService.appDiedLocked(app);
                stack.requestFinishActivityLocked(r.appToken, 0, null, "2nd-crash", false);
                return false;
            }
            app.activities.remove(r);
            throw e3;
        }
    }

    void startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig) {
        ProcessRecord app = this.mService.getProcessRecordLocked(r.processName, r.info.applicationInfo.uid, true);
        r.task.stack.setLaunchTime(r);
        if (app != null && app.thread != null) {
            try {
                if ((r.info.flags & 1) == 0 || !"android".equals(r.info.packageName)) {
                    app.addPackage(r.info.packageName, r.info.applicationInfo.versionCode, this.mService.mProcessStats);
                }
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity " + r.intent.getComponent().flattenToShortString(), e);
            }
        }
        this.mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0, "activity", r.intent.getComponent(), false, false, true);
    }

    boolean checkStartAnyActivityPermission(Intent intent, ActivityInfo aInfo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, boolean ignoreTargetSecurity, ProcessRecord callerApp, ActivityRecord resultRecord, ActivityStack resultStack, ActivityOptions options) {
        int startAnyPerm = this.mService.checkPermission("android.permission.START_ANY_ACTIVITY", callingPid, callingUid);
        if (startAnyPerm == 0) {
            return true;
        }
        int componentRestriction = getComponentRestrictionForCallingPackage(aInfo, callingPackage, callingPid, callingUid, ignoreTargetSecurity);
        int actionRestriction = getActionRestrictionForCallingPackage(intent.getAction(), callingPackage, callingPid, callingUid);
        if (componentRestriction == 1 || actionRestriction == 1) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            String msg = actionRestriction == 1 ? "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with revoked permission " + ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction()) : !aInfo.exported ? "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") not exported from uid " + aInfo.applicationInfo.uid : "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires " + aInfo.permission;
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        if (actionRestriction == 2) {
            String message = "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires " + AppOpsManager.permissionToOp(ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction()));
            Slog.w(TAG, message);
            return false;
        }
        if (componentRestriction == 2) {
            String message2 = "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires appop " + AppOpsManager.permissionToOp(aInfo.permission);
            Slog.w(TAG, message2);
            return false;
        }
        if (options == null || options.getLaunchTaskId() == -1) {
            return true;
        }
        int startInTaskPerm = this.mService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid);
        if (startInTaskPerm == 0) {
            return true;
        }
        String msg2 = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchTaskId=" + options.getLaunchTaskId();
        Slog.w(TAG, msg2);
        throw new SecurityException(msg2);
    }

    UserInfo getUserInfo(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return UserManager.get(this.mService.mContext).getUserInfo(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String callingPackage, int callingPid, int callingUid, boolean ignoreTargetSecurity) {
        int opCode;
        if (ignoreTargetSecurity || this.mService.checkComponentPermission(activityInfo.permission, callingPid, callingUid, activityInfo.applicationInfo.uid, activityInfo.exported) != -1) {
            return (activityInfo.permission == null || (opCode = AppOpsManager.permissionToOpCode(activityInfo.permission)) == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0 || ignoreTargetSecurity) ? 0 : 2;
        }
        return 1;
    }

    private int getActionRestrictionForCallingPackage(String action, String callingPackage, int callingPid, int callingUid) {
        String permission;
        if (action == null || (permission = ACTION_TO_RUNTIME_PERMISSION.get(action)) == null) {
            return 0;
        }
        try {
            PackageInfo packageInfo = this.mService.mContext.getPackageManager().getPackageInfo(callingPackage, 4096);
            if (!ArrayUtils.contains(packageInfo.requestedPermissions, permission)) {
                return 0;
            }
            if (this.mService.checkPermission(permission, callingPid, callingUid) == -1) {
                return 1;
            }
            int opCode = AppOpsManager.permissionToOpCode(permission);
            return (opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) == 0) ? 0 : 2;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Cannot find package info for " + callingPackage);
            return 0;
        }
    }

    boolean moveActivityStackToFront(ActivityRecord r, String reason) {
        if (r == null) {
            return false;
        }
        TaskRecord task = r.task;
        if (task == null || task.stack == null) {
            Slog.w(TAG, "Can't move stack to front for r=" + r + " task=" + task);
            return false;
        }
        task.stack.moveToFront(reason, task);
        return true;
    }

    void setLaunchSource(int uid) {
        this.mLaunchingActivity.setWorkSource(new WorkSource(uid));
    }

    void acquireLaunchWakelock() {
        this.mLaunchingActivity.acquire();
        if (this.mHandler.hasMessages(104)) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(104, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private boolean checkFinishBootingLocked() {
        boolean booting = this.mService.mBooting;
        boolean enableScreen = false;
        this.mService.mBooting = false;
        if (!this.mService.mBooted) {
            this.mService.mBooted = true;
            enableScreen = true;
        }
        if (booting || enableScreen) {
            this.mService.postFinishBooting(booting, enableScreen);
        }
        return booting;
    }

    final ActivityRecord activityIdleInternalLocked(IBinder token, boolean fromTimeout, Configuration config) {
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity idle: " + token);
        }
        ArrayList<ActivityRecord> finishes = null;
        ArrayList<UserState> startingUsers = null;
        boolean booting = false;
        boolean activityRemoved = false;
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r != null) {
            if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                Slog.d(TAG_IDLE, "activityIdleInternalLocked: Callers=" + Debug.getCallers(4));
            }
            this.mHandler.removeMessages(100, r);
            r.finishLaunchTickingLocked();
            if (fromTimeout) {
                reportActivityLaunchedLocked(fromTimeout, r, -1L, -1L);
            }
            if (config != null) {
                r.configuration = config;
            }
            r.idle = true;
            if (isFocusedStack(r.task.stack) || fromTimeout) {
                booting = checkFinishBootingLocked();
            }
        }
        if (allResumedActivitiesIdle()) {
            if (r != null) {
                this.mService.scheduleAppGcsLocked();
            }
            if (this.mLaunchingActivity.isHeld()) {
                this.mHandler.removeMessages(104);
                this.mLaunchingActivity.release();
            }
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        ArrayList<ActivityRecord> stops = processStoppingActivitiesLocked(true);
        int NS = stops != null ? stops.size() : 0;
        int NF = this.mFinishingActivities.size();
        if (NF > 0) {
            finishes = new ArrayList<>(this.mFinishingActivities);
            this.mFinishingActivities.clear();
        }
        if (this.mStartingUsers.size() > 0) {
            startingUsers = new ArrayList<>(this.mStartingUsers);
            this.mStartingUsers.clear();
        }
        for (int i = 0; i < NS; i++) {
            r = stops.get(i);
            ActivityStack stack = r.task.stack;
            if (stack != null) {
                if (r.finishing) {
                    stack.finishCurrentActivityLocked(r, 0, false);
                } else {
                    stack.stopActivityLocked(r);
                }
            }
        }
        for (int i2 = 0; i2 < NF; i2++) {
            r = finishes.get(i2);
            ActivityStack stack2 = r.task.stack;
            if (stack2 != null) {
                activityRemoved |= stack2.destroyActivityLocked(r, true, "finish-idle");
            }
        }
        if (!booting && startingUsers != null) {
            for (int i3 = 0; i3 < startingUsers.size(); i3++) {
                this.mService.mUserController.finishUserSwitch(startingUsers.get(i3));
            }
        }
        this.mService.trimApplications();
        if (activityRemoved) {
            resumeFocusedStackTopActivityLocked();
        }
        return r;
    }

    boolean handleAppDiedLocked(ProcessRecord app) {
        boolean hasVisibleActivities = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                hasVisibleActivities |= stacks.get(stackNdx).handleAppDiedLocked(app);
            }
        }
        return hasVisibleActivities;
    }

    void closeSystemDialogsLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                stacks.get(stackNdx).closeSystemDialogsLocked();
            }
        }
    }

    void removeUserLocked(int userId) {
        this.mUserStackInFront.delete(userId);
    }

    void updateUserStackLocked(int userId, ActivityStack stack) {
        if (userId == this.mCurrentUser) {
            return;
        }
        this.mUserStackInFront.put(userId, stack != null ? stack.getStackId() : 0);
    }

    boolean finishDisabledPackageActivitiesLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId) {
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (stack.finishDisabledPackageActivitiesLocked(packageName, filterByClasses, doit, evenPersistent, userId)) {
                    didSomething = true;
                }
            }
        }
        return didSomething;
    }

    void updatePreviousProcessLocked(ActivityRecord r) {
        ProcessRecord fgApp = null;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int stackNdx = stacks.size() - 1;
            while (true) {
                if (stackNdx >= 0) {
                    ActivityStack stack = stacks.get(stackNdx);
                    if (!isFocusedStack(stack)) {
                        stackNdx--;
                    } else if (stack.mResumedActivity != null) {
                        fgApp = stack.mResumedActivity.app;
                    } else if (stack.mPausingActivity != null) {
                        fgApp = stack.mPausingActivity.app;
                    }
                }
            }
        }
        if (r.app == null || fgApp == null || r.app == fgApp || r.lastVisibleTime <= this.mService.mPreviousProcessVisibleTime || r.app == this.mService.mHomeProcess) {
            return;
        }
        this.mService.mPreviousProcess = r.app;
        this.mService.mPreviousProcessVisibleTime = r.lastVisibleTime;
    }

    boolean resumeFocusedStackTopActivityLocked() {
        return resumeFocusedStackTopActivityLocked(null, null, null);
    }

    boolean resumeFocusedStackTopActivityLocked(ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
        if (targetStack != null && isFocusedStack(targetStack)) {
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
        }
        ActivityRecord r = this.mFocusedStack.topRunningActivityLocked();
        if (r == null || r.state != ActivityStack.ActivityState.RESUMED) {
            this.mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
            return false;
        }
        return false;
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo aInfo) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                stacks.get(stackNdx).updateActivityApplicationInfoLocked(aInfo);
            }
        }
    }

    TaskRecord finishTopRunningActivityLocked(ProcessRecord app, String reason) {
        TaskRecord finishedTask = null;
        ActivityStack focusedStack = getFocusedStack();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                TaskRecord t = stack.finishTopRunningActivityLocked(app, reason);
                if (stack == focusedStack || finishedTask == null) {
                    finishedTask = t;
                }
            }
        }
        return finishedTask;
    }

    void finishVoiceTask(IVoiceInteractionSession session) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.finishVoiceTask(session);
            }
        }
    }

    void findTaskToMoveToFrontLocked(TaskRecord task, int flags, ActivityOptions options, String reason, boolean forceNonResizeable) {
        if ((flags & 2) == 0) {
            this.mUserLeaving = true;
        }
        if ((flags & 1) != 0) {
            task.setTaskToReturnTo(1);
        }
        if (task.stack == null) {
            Slog.e(TAG, "findTaskToMoveToFrontLocked: can't move task=" + task + " to front. Stack is null");
            return;
        }
        if (task.isResizeable() && options != null) {
            int stackId = options.getLaunchStackId();
            if (canUseActivityOptionsLaunchBounds(options, stackId)) {
                Rect bounds = TaskRecord.validateBounds(options.getLaunchBounds());
                task.updateOverrideConfiguration(bounds);
                if (stackId == -1) {
                    stackId = task.getLaunchStackId();
                }
                if (stackId != task.stack.mStackId) {
                    ActivityStack stack = moveTaskToStackUncheckedLocked(task, stackId, true, false, reason);
                    stackId = stack.mStackId;
                }
                if (ActivityManager.StackId.resizeStackWithLaunchBounds(stackId)) {
                    resizeStackLocked(stackId, bounds, null, null, false, true, false);
                } else {
                    this.mWindowManager.resizeTask(task.taskId, task.mBounds, task.mOverrideConfig, false, false);
                }
            }
        }
        ActivityRecord r = task.getTopActivity();
        task.stack.moveTaskToFrontLocked(task, false, options, r == null ? null : r.appTimeTracker, reason);
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "findTaskToMoveToFront: moved to front of stack=" + task.stack);
        }
        handleNonResizableTaskIfNeeded(task, -1, task.stack.mStackId, forceNonResizeable);
    }

    boolean canUseActivityOptionsLaunchBounds(ActivityOptions options, int launchStackId) {
        if (options.getLaunchBounds() == null) {
            return false;
        }
        if (this.mService.mSupportsPictureInPicture && launchStackId == 4) {
            return true;
        }
        return this.mService.mSupportsFreeformWindowManagement;
    }

    ActivityStack getStack(int stackId) {
        return getStack(stackId, false, false);
    }

    ActivityStack getStack(int stackId, boolean createStaticStackIfNeeded, boolean createOnTop) {
        ActivityContainer activityContainer = this.mActivityContainers.get(stackId);
        if (activityContainer != null) {
            return activityContainer.mStack;
        }
        if (createStaticStackIfNeeded && ActivityManager.StackId.isStaticStack(stackId)) {
            return createStackOnDisplay(stackId, 0, createOnTop);
        }
        return null;
    }

    ArrayList<ActivityStack> getStacks() {
        ArrayList<ActivityStack> allStacks = new ArrayList<>();
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            allStacks.addAll(this.mActivityDisplays.valueAt(displayNdx).mStacks);
        }
        return allStacks;
    }

    IBinder getHomeActivityToken() {
        ActivityRecord homeActivity = getHomeActivity();
        if (homeActivity != null) {
            return homeActivity.appToken;
        }
        return null;
    }

    ActivityRecord getHomeActivity() {
        return getHomeActivityForUser(this.mCurrentUser);
    }

    ActivityRecord getHomeActivityForUser(int userId) {
        ArrayList<TaskRecord> tasks = this.mHomeStack.getAllTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = tasks.get(taskNdx);
            if (task.isHomeTask()) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = activities.get(activityNdx);
                    if (r.isHomeActivity() && (userId == -1 || r.userId == userId)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    boolean isStackDockedInEffect(int stackId) {
        if (stackId != 3) {
            return ActivityManager.StackId.isResizeableByDockedStack(stackId) && getStack(3) != null;
        }
        return true;
    }

    ActivityContainer createVirtualActivityContainer(ActivityRecord parentActivity, IActivityContainerCallback callback) {
        ActivityContainer activityContainer = new VirtualActivityContainer(parentActivity, callback);
        this.mActivityContainers.put(activityContainer.mStackId, activityContainer);
        if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
            Slog.d(TAG_CONTAINERS, "createActivityContainer: " + activityContainer);
        }
        parentActivity.mChildContainers.add(activityContainer);
        return activityContainer;
    }

    void removeChildActivityContainers(ActivityRecord parentActivity) {
        ArrayList<ActivityContainer> childStacks = parentActivity.mChildContainers;
        for (int containerNdx = childStacks.size() - 1; containerNdx >= 0; containerNdx--) {
            ActivityContainer container = childStacks.remove(containerNdx);
            if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
                Slog.d(TAG_CONTAINERS, "removeChildActivityContainers: removing " + container);
            }
            container.release();
        }
    }

    void deleteActivityContainer(IActivityContainer container) {
        ActivityContainer activityContainer = (ActivityContainer) container;
        if (activityContainer == null) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
            Slog.d(TAG_CONTAINERS, "deleteActivityContainer: callers=" + Debug.getCallers(4));
        }
        int stackId = activityContainer.mStackId;
        this.mActivityContainers.remove(stackId);
        this.mWindowManager.removeStack(stackId);
    }

    void resizeStackLocked(int stackId, Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds, boolean preserveWindows, boolean allowResizeInDockedMode, boolean deferResume) {
        if (stackId == 3) {
            resizeDockedStackLocked(bounds, tempTaskBounds, tempTaskInsetBounds, null, null, preserveWindows);
            return;
        }
        ActivityStack stack = getStack(stackId);
        if (stack == null) {
            Slog.w(TAG, "resizeStack: stackId " + stackId + " not found.");
            return;
        }
        if (!allowResizeInDockedMode && getStack(3) != null) {
            return;
        }
        Trace.traceBegin(64L, "am.resizeStack_" + stackId);
        this.mWindowManager.deferSurfaceLayout();
        try {
            resizeStackUncheckedLocked(stack, bounds, tempTaskBounds, tempTaskInsetBounds);
            if (!deferResume) {
                stack.ensureVisibleActivitiesConfigurationLocked(stack.topRunningActivityLocked(), preserveWindows);
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
    }

    void deferUpdateBounds(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack == null) {
            return;
        }
        stack.deferUpdateBounds();
    }

    void continueUpdateBounds(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack == null) {
            return;
        }
        stack.continueUpdateBounds();
    }

    void notifyAppTransitionDone() {
        continueUpdateBounds(0);
        for (int i = this.mResizingTasksDuringAnimation.size() - 1; i >= 0; i--) {
            int taskId = this.mResizingTasksDuringAnimation.valueAt(i).intValue();
            if (anyTaskForIdLocked(taskId, false, -1) != null) {
                this.mWindowManager.setTaskDockedResizing(taskId, false);
            }
        }
        this.mResizingTasksDuringAnimation.clear();
    }

    void resizeStackUncheckedLocked(ActivityStack stack, Rect bounds, Rect tempTaskBounds, Rect tempTaskInsetBounds) {
        Rect bounds2 = TaskRecord.validateBounds(bounds);
        if (!stack.updateBoundsAllowed(bounds2, tempTaskBounds, tempTaskInsetBounds)) {
            return;
        }
        this.mTmpBounds.clear();
        this.mTmpConfigs.clear();
        this.mTmpInsetBounds.clear();
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        Rect taskBounds = tempTaskBounds != null ? tempTaskBounds : bounds2;
        Rect insetBounds = tempTaskInsetBounds != null ? tempTaskInsetBounds : taskBounds;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskRecord task = tasks.get(i);
            if (task.isResizeable()) {
                if (stack.mStackId == 2) {
                    this.tempRect2.set(task.mBounds);
                    fitWithinBounds(this.tempRect2, bounds2);
                    task.updateOverrideConfiguration(this.tempRect2);
                } else {
                    task.updateOverrideConfiguration(taskBounds, insetBounds);
                }
            }
            this.mTmpConfigs.put(task.taskId, task.mOverrideConfig);
            this.mTmpBounds.put(task.taskId, task.mBounds);
            if (tempTaskInsetBounds != null) {
                this.mTmpInsetBounds.put(task.taskId, tempTaskInsetBounds);
            }
        }
        this.mWindowManager.prepareFreezingTaskBounds(stack.mStackId);
        stack.mFullscreen = this.mWindowManager.resizeStack(stack.mStackId, bounds2, this.mTmpConfigs, this.mTmpBounds, this.mTmpInsetBounds);
        stack.setBounds(bounds2);
    }

    void moveTasksToFullscreenStackLocked(int fromStackId, boolean onTop) {
        ActivityStack stack = getStack(fromStackId);
        if (stack == null) {
            return;
        }
        this.mWindowManager.deferSurfaceLayout();
        if (fromStackId == 3) {
            for (int i = 0; i <= 4; i++) {
                try {
                    if (ActivityManager.StackId.isResizeableByDockedStack(i)) {
                        ActivityStack otherStack = getStack(i);
                        if (otherStack != null) {
                            resizeStackLocked(i, null, null, null, true, true, true);
                        }
                    }
                } finally {
                    this.mAllowDockedStackResize = true;
                    this.mWindowManager.continueSurfaceLayout();
                }
            }
            this.mAllowDockedStackResize = false;
        }
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        int size = tasks.size();
        if (onTop) {
            for (int i2 = 0; i2 < size; i2++) {
                moveTaskToStackLocked(tasks.get(i2).taskId, 1, onTop, onTop, "moveTasksToFullscreenStack", true, true);
            }
            ensureActivitiesVisibleLocked(null, 0, true);
            resumeFocusedStackTopActivityLocked();
        } else {
            for (int i3 = size - 1; i3 >= 0; i3--) {
                if (!MultiWindowManager.isSupported() || tasks.get(i3) == null || !tasks.get(i3).mLastTaskInFreeformStack) {
                    positionTaskInStackLocked(tasks.get(i3).taskId, 1, 0);
                }
            }
        }
    }

    void moveProfileTasksFromFreeformToFullscreenStackLocked(int userId) {
        ActivityStack stack = getStack(2);
        if (stack == null) {
            return;
        }
        this.mWindowManager.deferSurfaceLayout();
        try {
            ArrayList<TaskRecord> tasks = stack.getAllTasks();
            int size = tasks.size();
            for (int i = size - 1; i >= 0; i--) {
                if (taskContainsActivityFromUser(tasks.get(i), userId)) {
                    positionTaskInStackLocked(tasks.get(i).taskId, 1, 0);
                }
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void resizeDockedStackLocked(Rect dockedBounds, Rect tempDockedTaskBounds, Rect tempDockedTaskInsetBounds, Rect tempOtherTaskBounds, Rect tempOtherTaskInsetBounds, boolean preserveWindows) {
        if (!this.mAllowDockedStackResize) {
            return;
        }
        ActivityStack stack = getStack(3);
        if (stack == null) {
            Slog.w(TAG, "resizeDockedStackLocked: docked stack not found");
            return;
        }
        Trace.traceBegin(64L, "am.resizeDockedStack");
        this.mWindowManager.deferSurfaceLayout();
        try {
            this.mAllowDockedStackResize = false;
            ActivityRecord r = stack.topRunningActivityLocked();
            resizeStackUncheckedLocked(stack, dockedBounds, tempDockedTaskBounds, tempDockedTaskInsetBounds);
            if (stack.mFullscreen || (dockedBounds == null && !stack.isAttached())) {
                moveTasksToFullscreenStackLocked(3, true);
                r = null;
            } else {
                this.mWindowManager.getStackDockedModeBounds(0, this.tempRect, true);
                for (int i = 0; i <= 4; i++) {
                    if (ActivityManager.StackId.isResizeableByDockedStack(i) && getStack(i) != null) {
                        resizeStackLocked(i, this.tempRect, tempOtherTaskBounds, tempOtherTaskInsetBounds, preserveWindows, true, false);
                    }
                }
            }
            stack.ensureVisibleActivitiesConfigurationLocked(r, preserveWindows);
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
            ResizeDockedStackTimeout resizeDockedStackTimeout = this.mResizeDockedStackTimeout;
            boolean z = (tempDockedTaskBounds == null && tempDockedTaskInsetBounds == null && tempOtherTaskBounds == null && tempOtherTaskInsetBounds == null) ? false : true;
            resizeDockedStackTimeout.notifyResizing(dockedBounds, z);
        } catch (Throwable th) {
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
            throw th;
        }
    }

    void resizePinnedStackLocked(Rect pinnedBounds, Rect tempPinnedTaskBounds) {
        ActivityStack stack = getStack(4);
        if (stack == null) {
            Slog.w(TAG, "resizePinnedStackLocked: pinned stack not found");
            return;
        }
        Trace.traceBegin(64L, "am.resizePinnedStack");
        this.mWindowManager.deferSurfaceLayout();
        try {
            ActivityRecord r = stack.topRunningActivityLocked();
            resizeStackUncheckedLocked(stack, pinnedBounds, tempPinnedTaskBounds, null);
            stack.ensureVisibleActivitiesConfigurationLocked(r, false);
        } finally {
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
    }

    boolean resizeTaskLocked(TaskRecord task, Rect bounds, int resizeMode, boolean preserveWindow, boolean deferResume) {
        boolean needRelaunch;
        ActivityRecord r;
        if (!task.isResizeable()) {
            Slog.w(TAG, "resizeTask: task " + task + " not resizeable.");
            return true;
        }
        if (!MultiWindowManager.isSupported() || task.stack == null || task.stack.mStackId != 1 || preserveWindow) {
            needRelaunch = false;
        } else {
            needRelaunch = Objects.equals(task.mBounds, bounds);
        }
        if (MultiWindowManager.isSupported() && needRelaunch) {
            Slog.d(TAG, "[BMW] move task from freeform stack to fullscreen stack, need to restart");
        }
        boolean forced = (resizeMode & 2) != 0;
        if (Objects.equals(task.mBounds, bounds) && !forced && !needRelaunch) {
            return true;
        }
        Rect bounds2 = TaskRecord.validateBounds(bounds);
        if (!this.mWindowManager.isValidTaskId(task.taskId)) {
            task.updateOverrideConfiguration(bounds2);
            if (task.stack != null && task.stack.mStackId != 2) {
                restoreRecentTaskLocked(task, 2);
                return true;
            }
            return true;
        }
        Trace.traceBegin(64L, "am.resizeTask_" + task.taskId);
        Configuration overrideConfig = task.updateOverrideConfiguration(bounds2);
        boolean kept = true;
        if ((overrideConfig != null || needRelaunch) && (r = task.topRunningActivityLocked()) != null) {
            if (needRelaunch) {
                r.forceNewConfig = true;
            }
            ActivityStack stack = task.stack;
            kept = stack.ensureActivityConfigurationLocked(r, 0, preserveWindow);
            if (!deferResume) {
                ensureActivitiesVisibleLocked(r, 0, false);
                if (!kept) {
                    resumeFocusedStackTopActivityLocked();
                }
            }
        }
        this.mWindowManager.resizeTask(task.taskId, task.mBounds, task.mOverrideConfig, kept, forced);
        Trace.traceEnd(64L);
        return kept;
    }

    ActivityStack createStackOnDisplay(int stackId, int displayId, boolean onTop) {
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
        if (activityDisplay == null) {
            return null;
        }
        ActivityContainer activityContainer = new ActivityContainer(stackId);
        this.mActivityContainers.put(stackId, activityContainer);
        activityContainer.attachToDisplayLocked(activityDisplay, onTop);
        return activityContainer.mStack;
    }

    int getNextStackId() {
        while (true) {
            if (this.mNextFreeStackId < 5 || getStack(this.mNextFreeStackId) != null) {
                this.mNextFreeStackId++;
            } else {
                return this.mNextFreeStackId;
            }
        }
    }

    private boolean restoreRecentTaskLocked(TaskRecord task, int stackId) {
        if (stackId == -1) {
            stackId = task.getLaunchStackId();
            if (MultiWindowManager.isSupported() && stackId == 2 && findStack(3) != null) {
                stackId = 1;
            }
        } else if (stackId == 3 && !task.canGoInDockedStack()) {
            stackId = 1;
        } else if (stackId == 2 && this.mService.mUserController.shouldConfirmCredentials(task.userId)) {
            stackId = 1;
        }
        if (task.stack != null) {
            if (task.stack.mStackId == stackId) {
                return true;
            }
            task.stack.removeTask(task, "restoreRecentTaskLocked", 1);
        }
        ActivityStack stack = getStack(stackId, true, false);
        if (stack == null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.v(TAG_RECENTS, "Unable to find/create stack to restore recent task=" + task);
            }
            return false;
        }
        stack.addTask(task, false, "restoreRecentTask");
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Added restored task=" + task + " to stack=" + stack);
        }
        ArrayList<ActivityRecord> activities = task.mActivities;
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            stack.addConfigOverride(activities.get(activityNdx), task);
        }
        return true;
    }

    ActivityStack moveTaskToStackUncheckedLocked(TaskRecord task, int stackId, boolean toTop, boolean forceFocus, String reason) {
        if (ActivityManager.StackId.isMultiWindowStack(stackId) && !this.mService.mSupportsMultiWindow) {
            throw new IllegalStateException("moveTaskToStackUncheckedLocked: Device doesn't support multi-window task=" + task + " to stackId=" + stackId);
        }
        if (ActivityManagerDebugConfig.DEBUG_MULTIWINDOW) {
            Slog.d(TAG, "moveTaskToStackUncheckedLocked: " + task + ", t.uid=" + task.mCallingUid + ", t.pkg=" + task.mCallingPackage + ", t.userid=" + task.userId + ", t.intent=" + task.intent + ", stackid=" + stackId + ", toTop=" + toTop + ", focus= " + forceFocus + ", reason=" + reason, new Throwable());
        }
        ActivityRecord r = task.topRunningActivityLocked();
        ActivityStack prevStack = task.stack;
        boolean wasFocused = isFocusedStack(prevStack) && topRunningActivityLocked() == r;
        boolean wasResumed = prevStack.mResumedActivity == r;
        boolean wasFront = isFrontStack(prevStack) && prevStack.topRunningActivityLocked() == r;
        if (stackId == 3 && !task.isResizeable()) {
            stackId = prevStack != null ? prevStack.mStackId : 1;
            Slog.w(TAG, "Can not move unresizeable task=" + task + " to docked stack. Moving to stackId=" + stackId + " instead.");
        }
        if (stackId == 2 && this.mService.mUserController.shouldConfirmCredentials(task.userId)) {
            stackId = prevStack != null ? prevStack.mStackId : 1;
            Slog.w(TAG, "Can not move locked profile task=" + task + " to freeform stack. Moving to stackId=" + stackId + " instead.");
        }
        task.mTemporarilyUnresizable = true;
        ActivityStack stack = getStack(stackId, true, toTop);
        task.mTemporarilyUnresizable = false;
        this.mWindowManager.moveTaskToStack(task.taskId, stack.mStackId, toTop);
        stack.addTask(task, toTop, reason);
        if (forceFocus || wasFocused) {
            wasFront = true;
        }
        stack.moveToFrontAndResumeStateIfNeeded(r, wasFront, wasResumed, reason);
        return stack;
    }

    boolean moveTaskToStackLocked(int taskId, int stackId, boolean toTop, boolean forceFocus, String reason, boolean animate) {
        return moveTaskToStackLocked(taskId, stackId, toTop, forceFocus, reason, animate, false);
    }

    boolean moveTaskToStackLocked(int taskId, int stackId, boolean toTop, boolean forceFocus, String reason, boolean animate, boolean deferResume) {
        TaskRecord task = anyTaskForIdLocked(taskId);
        if (task == null) {
            Slog.w(TAG, "moveTaskToStack: no task for id=" + taskId);
            return false;
        }
        if (task.stack != null && task.stack.mStackId == stackId) {
            Slog.i(TAG, "moveTaskToStack: taskId=" + taskId + " already in stackId=" + stackId);
            return true;
        }
        if (stackId == 2 && !this.mService.mSupportsFreeformWindowManagement) {
            throw new IllegalArgumentException("moveTaskToStack:Attempt to move task " + taskId + " to unsupported freeform stack");
        }
        if (MultiWindowManager.isSupported() && stackId == 3) {
            Slog.d(TAG, "moveTaskToStack: moveTasksToFullscreenStack");
            if (task != null && task.stack.mStackId == 2) {
                task.mLastTaskInFreeformStack = true;
            }
            this.mService.moveTasksToFullscreenStack(2, false);
            if (task.mLastTaskInFreeformStack) {
                task.mLastTaskInFreeformStack = false;
            }
        }
        if (MultiWindowManager.isSupported() && stackId != 2 && task.mSticky) {
            this.mService.stickWindow(task, false);
        }
        ActivityRecord topActivity = task.getTopActivity();
        int sourceStackId = task.stack != null ? task.stack.mStackId : -1;
        boolean mightReplaceWindow = ActivityManager.StackId.replaceWindowsOnTaskMove(sourceStackId, stackId) && topActivity != null;
        if (mightReplaceWindow) {
            this.mWindowManager.setReplacingWindow(topActivity.appToken, animate);
        }
        this.mWindowManager.deferSurfaceLayout();
        boolean kept = true;
        try {
            ActivityStack stack = moveTaskToStackUncheckedLocked(task, stackId, toTop, forceFocus, reason + " moveTaskToStack");
            int stackId2 = stack.mStackId;
            if (!animate) {
                stack.mNoAnimActivities.add(topActivity);
            }
            this.mWindowManager.prepareFreezingTaskBounds(stack.mStackId);
            if (stackId2 == 1 && task.mBounds != null) {
                kept = resizeTaskLocked(task, stack.mBounds, 0, !mightReplaceWindow, deferResume);
            } else if (stackId2 == 2) {
                Rect bounds = task.getLaunchBounds();
                if (bounds == null) {
                    stack.layoutTaskInStack(task, null);
                    bounds = task.mBounds;
                }
                kept = resizeTaskLocked(task, bounds, 2, !mightReplaceWindow, deferResume);
            } else if (stackId2 == 3 || stackId2 == 4) {
                kept = resizeTaskLocked(task, stack.mBounds, 0, !mightReplaceWindow, deferResume);
            }
            if (mightReplaceWindow) {
                this.mWindowManager.scheduleClearReplacingWindowIfNeeded(topActivity.appToken, !kept);
            }
            if (!deferResume) {
                ensureActivitiesVisibleLocked(null, 0, !mightReplaceWindow);
                resumeFocusedStackTopActivityLocked();
            }
            handleNonResizableTaskIfNeeded(task, stackId, stackId2);
            return stackId == stackId2;
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    boolean moveTopStackActivityToPinnedStackLocked(int stackId, Rect bounds) {
        ActivityStack stack = getStack(stackId, false, false);
        if (stack == null) {
            throw new IllegalArgumentException("moveTopStackActivityToPinnedStackLocked: Unknown stackId=" + stackId);
        }
        ActivityRecord r = stack.topRunningActivityLocked();
        if (r == null) {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: No top running activity in stack=" + stack);
            return false;
        }
        if (!this.mService.mForceResizableActivities && !r.supportsPictureInPicture()) {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: Picture-In-Picture not supported for  r=" + r);
            return false;
        }
        moveActivityToPinnedStackLocked(r, "moveTopActivityToPinnedStack", bounds);
        return true;
    }

    void moveActivityToPinnedStackLocked(ActivityRecord r, String reason, Rect bounds) {
        this.mWindowManager.deferSurfaceLayout();
        try {
            TaskRecord task = r.task;
            if (r == task.stack.getVisibleBehindActivity()) {
                requestVisibleBehindLocked(r, false);
            }
            ActivityStack stack = getStack(4, true, true);
            resizeStackLocked(4, task.mBounds, null, null, false, true, false);
            if (task.mActivities.size() == 1) {
                if (task.getTaskToReturnTo() == 1) {
                    moveHomeStackToFront(reason);
                }
                moveTaskToStackLocked(task.taskId, 4, true, true, reason, false);
            } else {
                stack.moveActivityToStack(r);
            }
            this.mWindowManager.continueSurfaceLayout();
            ensureActivitiesVisibleLocked(null, 0, false);
            resumeFocusedStackTopActivityLocked();
            this.mWindowManager.animateResizePinnedStack(bounds, -1);
            this.mService.notifyActivityPinnedLocked();
        } catch (Throwable th) {
            this.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    void positionTaskInStackLocked(int taskId, int stackId, int position) {
        TaskRecord task = anyTaskForIdLocked(taskId);
        if (task == null) {
            Slog.w(TAG, "positionTaskInStackLocked: no task for id=" + taskId);
            return;
        }
        ActivityStack stack = getStack(stackId, true, false);
        task.updateOverrideConfigurationForStack(stack);
        this.mWindowManager.positionTaskInStack(taskId, stackId, position, task.mBounds, task.mOverrideConfig);
        stack.positionTask(task, position);
        stack.ensureActivitiesVisibleLocked(null, 0, false);
        resumeFocusedStackTopActivityLocked();
    }

    ActivityRecord findTaskLocked(ActivityRecord r) {
        this.mTmpFindTaskResult.r = null;
        this.mTmpFindTaskResult.matchedByRootAffinity = false;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG_TASKS, "Looking for task of " + r);
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (!r.isApplicationActivity() && !stack.isHomeStack()) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Skipping stack: (home activity) " + stack);
                    }
                } else if (!stack.mActivityContainer.isEligibleForNewTasks()) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Skipping stack: (new task not allowed) " + stack);
                    }
                } else {
                    stack.findTaskLocked(r, this.mTmpFindTaskResult);
                    if (this.mTmpFindTaskResult.r != null && !this.mTmpFindTaskResult.matchedByRootAffinity) {
                        return this.mTmpFindTaskResult.r;
                    }
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TASKS && this.mTmpFindTaskResult.r == null) {
            Slog.d(TAG_TASKS, "No task found");
        }
        return this.mTmpFindTaskResult.r;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord ar = stacks.get(stackNdx).findActivityLocked(intent, info, compareIntentFilters);
                if (ar != null) {
                    return ar;
                }
            }
        }
        return null;
    }

    void goingToSleepLocked() {
        scheduleSleepTimeout();
        if (!this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.acquire();
            if (this.mLaunchingActivity.isHeld()) {
                this.mLaunchingActivity.release();
                this.mService.mHandler.removeMessages(104);
            }
        }
        checkReadyForSleepLocked();
    }

    boolean shutdownLocked(int timeout) {
        goingToSleepLocked();
        boolean timedout = false;
        long endTime = System.currentTimeMillis() + ((long) timeout);
        while (true) {
            boolean cantShutdown = false;
            for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    cantShutdown |= stacks.get(stackNdx).checkReadyForSleepLocked();
                }
            }
            if (!cantShutdown) {
                break;
            }
            long timeRemaining = endTime - System.currentTimeMillis();
            if (timeRemaining > 0) {
                try {
                    this.mService.wait(timeRemaining);
                } catch (InterruptedException e) {
                }
            } else {
                Slog.w(TAG, "Activity manager shutdown timed out");
                timedout = true;
                break;
            }
        }
        this.mSleepTimeout = true;
        checkReadyForSleepLocked();
        return timedout;
    }

    void comeOutOfSleepIfNeededLocked() {
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.awakeFromSleepingLocked();
                if (isFocusedStack(stack)) {
                    resumeFocusedStackTopActivityLocked();
                }
            }
        }
        this.mGoingToSleepActivities.clear();
    }

    void activitySleptLocked(ActivityRecord r) {
        this.mGoingToSleepActivities.remove(r);
        checkReadyForSleepLocked();
    }

    void checkReadyForSleepLocked() {
        if (!this.mService.isSleepingOrShuttingDownLocked()) {
            return;
        }
        if (!this.mSleepTimeout) {
            boolean dontSleep = false;
            for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    dontSleep |= stacks.get(stackNdx).checkReadyForSleepLocked();
                }
            }
            if (this.mStoppingActivities.size() > 0) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Sleep still need to stop " + this.mStoppingActivities.size() + " activities");
                }
                scheduleIdleLocked();
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG, "ACT-IDLE_NOW_MSG from checkReadyForSleepLocked size > 0");
                }
                dontSleep = true;
            }
            if (this.mGoingToSleepActivities.size() > 0) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Sleep still need to sleep " + this.mGoingToSleepActivities.size() + " activities");
                }
                dontSleep = true;
            }
            if (dontSleep) {
                return;
            }
        }
        for (int displayNdx2 = this.mActivityDisplays.size() - 1; displayNdx2 >= 0; displayNdx2--) {
            ArrayList<ActivityStack> stacks2 = this.mActivityDisplays.valueAt(displayNdx2).mStacks;
            for (int stackNdx2 = stacks2.size() - 1; stackNdx2 >= 0; stackNdx2--) {
                stacks2.get(stackNdx2).goToSleep();
            }
        }
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
        if (!this.mService.mShuttingDown) {
            return;
        }
        this.mService.notifyAll();
    }

    boolean reportResumedActivityLocked(ActivityRecord r) {
        ActivityStack stack = r.task.stack;
        if (isFocusedStack(stack)) {
            this.mService.updateUsageStats(r, true);
        }
        if (!allResumedActivitiesComplete()) {
            return false;
        }
        ensureActivitiesVisibleLocked(null, 0, false);
        this.mWindowManager.executeAppTransition();
        return true;
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                stacks.get(stackNdx).handleAppCrashLocked(app);
            }
        }
    }

    boolean requestVisibleBehindLocked(ActivityRecord r, boolean visible) {
        ActivityRecord next;
        ActivityStack stack = r.task.stack;
        if (stack == null) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind: r=" + r + " visible=" + visible + " stack is null");
            }
            return false;
        }
        if (visible && !ActivityManager.StackId.activitiesCanRequestVisibleBehind(stack.mStackId)) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind: r=" + r + " visible=" + visible + " stackId=" + stack.mStackId + " can't contain visible behind activities");
            }
            return false;
        }
        boolean isVisible = stack.hasVisibleBehindActivity();
        if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
            Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind r=" + r + " visible=" + visible + " isVisible=" + isVisible);
        }
        ActivityRecord top = topRunningActivityLocked();
        if (top == null || top == r || visible == isVisible) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind: quick return");
            }
            if (!visible) {
                r = null;
            }
            stack.setVisibleBehindActivity(r);
            return true;
        }
        if (visible && top.fullscreen) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind: returning top.fullscreen=" + top.fullscreen + " top.state=" + top.state + " top.app=" + top.app + " top.app.thread=" + top.app.thread);
            }
            return false;
        }
        if (!visible && stack.getVisibleBehindActivity() != r) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBLE_BEHIND) {
                Slog.d(TAG_VISIBLE_BEHIND, "requestVisibleBehind: returning visible=" + visible + " stack.getVisibleBehindActivity()=" + stack.getVisibleBehindActivity() + " r=" + r);
            }
            return false;
        }
        stack.setVisibleBehindActivity(visible ? r : null);
        if (!visible && (next = stack.findNextTranslucentActivity(r)) != null && next.isHomeActivity()) {
            this.mService.convertFromTranslucent(next.appToken);
        }
        if (top.app != null && top.app.thread != null) {
            try {
                top.app.thread.scheduleBackgroundVisibleBehindChanged(top.appToken, visible);
            } catch (RemoteException e) {
            }
        }
        return true;
    }

    void handleLaunchTaskBehindCompleteLocked(ActivityRecord r) {
        TaskRecord task = r.task;
        ActivityStack stack = task.stack;
        r.mLaunchTaskBehind = false;
        task.setLastThumbnailLocked(stack.screenshotActivitiesLocked(r));
        this.mRecentTasks.addLocked(task);
        this.mService.notifyTaskStackChangedLocked();
        this.mWindowManager.setAppVisibility(r.appToken, false);
        ActivityRecord top = stack.topActivity();
        if (top == null) {
            return;
        }
        top.task.touchActiveTime();
    }

    void scheduleLaunchTaskBehindComplete(IBinder token) {
        this.mHandler.obtainMessage(112, token).sendToTarget();
    }

    void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int topStackNdx = stacks.size() - 1;
            for (int stackNdx = topStackNdx; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.ensureActivitiesVisibleLocked(starting, configChanges, preserveWindows);
            }
        }
    }

    void invalidateTaskLayers() {
        this.mTaskLayersChanged = true;
    }

    void rankTaskLayersIfNeeded() {
        if (!this.mTaskLayersChanged) {
            return;
        }
        this.mTaskLayersChanged = false;
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int baseLayer = 0;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                baseLayer += stacks.get(stackNdx).rankTaskLayers(baseLayer);
            }
        }
    }

    void clearOtherAppTimeTrackers(AppTimeTracker except) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int topStackNdx = stacks.size() - 1;
            for (int stackNdx = topStackNdx; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.clearOtherAppTimeTrackers(except);
            }
        }
    }

    void scheduleDestroyAllActivities(ProcessRecord app, String reason) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.scheduleDestroyActivities(app, reason);
            }
        }
    }

    void releaseSomeActivitiesLocked(ProcessRecord app, String reason) {
        TaskRecord firstTask = null;
        ArraySet<TaskRecord> tasks = null;
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + app);
        }
        for (int i = 0; i < app.activities.size(); i++) {
            ActivityRecord r = app.activities.get(i);
            if (r.finishing || r.state == ActivityStack.ActivityState.DESTROYING || r.state == ActivityStack.ActivityState.DESTROYED) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Abort release; already destroying: " + r);
                    return;
                }
                return;
            }
            if (r.visible || !r.stopped || !r.haveState || r.state == ActivityStack.ActivityState.RESUMED || r.state == ActivityStack.ActivityState.PAUSING || r.state == ActivityStack.ActivityState.PAUSED || r.state == ActivityStack.ActivityState.STOPPING) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Not releasing in-use activity: " + r);
                }
            } else if (r.task != null) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Collecting release task " + r.task + " from " + r);
                }
                if (firstTask == null) {
                    firstTask = r.task;
                } else if (firstTask != r.task) {
                    if (tasks == null) {
                        tasks = new ArraySet<>();
                        tasks.add(firstTask);
                    }
                    tasks.add(r.task);
                }
            }
        }
        if (tasks == null) {
            if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                Slog.d(TAG_RELEASE, "Didn't find two or more tasks to release");
                return;
            }
            return;
        }
        int numDisplays = this.mActivityDisplays.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = 0; stackNdx < stacks.size(); stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                if (stack.releaseSomeActivitiesLocked(app, tasks, reason) > 0) {
                    return;
                }
            }
        }
    }

    boolean switchUserLocked(int userId, UserState uss) {
        int focusStackId = this.mFocusedStack.getStackId();
        moveTasksToFullscreenStackLocked(3, focusStackId == 3);
        this.mUserStackInFront.put(this.mCurrentUser, focusStackId);
        int restoreStackId = this.mUserStackInFront.get(userId, 0);
        this.mCurrentUser = userId;
        this.mStartingUsers.add(uss);
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.switchUserLocked(userId);
                TaskRecord task = stack.topTask();
                if (task != null) {
                    this.mWindowManager.moveTaskToTop(task.taskId);
                }
            }
        }
        ActivityStack stack2 = getStack(restoreStackId);
        if (stack2 == null) {
            stack2 = this.mHomeStack;
        }
        boolean homeInFront = stack2.isHomeStack();
        if (stack2.isOnHomeDisplay()) {
            stack2.moveToFront("switchUserOnHomeDisplay");
        } else {
            resumeHomeStackTask(1, null, "switchUserOnOtherDisplay");
        }
        return homeInFront;
    }

    boolean isCurrentProfileLocked(int userId) {
        if (userId == this.mCurrentUser) {
            return true;
        }
        return this.mService.mUserController.isCurrentProfileLocked(userId);
    }

    boolean okToShowLocked(ActivityRecord r) {
        if (r == null) {
            return false;
        }
        if ((r.info.flags & 1024) == 0) {
            return isCurrentProfileLocked(r.userId) && !this.mService.mUserController.isUserStoppingOrShuttingDownLocked(r.userId);
        }
        return true;
    }

    final ArrayList<ActivityRecord> processStoppingActivitiesLocked(boolean remove) {
        ArrayList<ActivityRecord> stops = null;
        boolean nowVisible = allResumedActivitiesVisible();
        for (int activityNdx = this.mStoppingActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord s = this.mStoppingActivities.get(activityNdx);
            forceStopWaitingVisibleActivitiesLocked(activityNdx, s);
            boolean waitingVisible = this.mWaitingVisibleActivities.contains(s);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG, "Stopping " + s + ": nowVisible=" + nowVisible + " waitingVisible=" + waitingVisible + " finishing=" + s.finishing);
            }
            if (waitingVisible && nowVisible) {
                this.mWaitingVisibleActivities.remove(s);
                if (s.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.v(TAG, "Before stopping, can hide: " + s);
                    }
                    this.mWindowManager.setAppVisibility(s.appToken, false);
                }
            }
            if ((!waitingVisible || this.mService.isSleepingOrShuttingDownLocked()) && remove) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG, "Ready to stop: " + s);
                }
                if (stops == null) {
                    stops = new ArrayList<>();
                }
                stops.add(s);
                this.mStoppingActivities.remove(activityNdx);
            }
        }
        return stops;
    }

    void validateTopActivitiesLocked() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ActivityRecord r = stack.topRunningActivityLocked();
                ActivityStack.ActivityState state = r == null ? ActivityStack.ActivityState.DESTROYED : r.state;
                if (isFocusedStack(stack)) {
                    if (r == null) {
                        Slog.e(TAG, "validateTop...: null top activity, stack=" + stack);
                    } else {
                        ActivityRecord pausing = stack.mPausingActivity;
                        if (pausing != null && pausing == r) {
                            Slog.e(TAG, "validateTop...: top stack has pausing activity r=" + r + " state=" + state);
                        }
                        if (state != ActivityStack.ActivityState.INITIALIZING && state != ActivityStack.ActivityState.RESUMED) {
                            Slog.e(TAG, "validateTop...: activity in front not resumed r=" + r + " state=" + state);
                        }
                    }
                } else {
                    ActivityRecord resumed = stack.mResumedActivity;
                    if (resumed != null && resumed == r) {
                        Slog.e(TAG, "validateTop...: back stack has resumed activity r=" + r + " state=" + state);
                    }
                    if (r != null && (state == ActivityStack.ActivityState.INITIALIZING || state == ActivityStack.ActivityState.RESUMED)) {
                        Slog.e(TAG, "validateTop...: activity in back resumed r=" + r + " state=" + state);
                    }
                }
            }
        }
    }

    private String lockTaskModeToString() {
        switch (this.mLockTaskModeState) {
            case 0:
                return "NONE";
            case 1:
                return "LOCKED";
            case 2:
                return "PINNED";
            default:
                return "unknown=" + this.mLockTaskModeState;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mFocusedStack=" + this.mFocusedStack);
        pw.print(" mLastFocusedStack=");
        pw.println(this.mLastFocusedStack);
        pw.print(prefix);
        pw.println("mSleepTimeout=" + this.mSleepTimeout);
        pw.print(prefix);
        pw.println("mCurTaskIdForUser=" + this.mCurTaskIdForUser);
        pw.print(prefix);
        pw.println("mUserStackInFront=" + this.mUserStackInFront);
        pw.print(prefix);
        pw.println("mActivityContainers=" + this.mActivityContainers);
        pw.print(prefix);
        pw.print("mLockTaskModeState=" + lockTaskModeToString());
        SparseArray<String[]> packages = this.mService.mLockTaskPackages;
        if (packages.size() > 0) {
            pw.println(" mLockTaskPackages (userId:packages)=");
            for (int i = 0; i < packages.size(); i++) {
                pw.print(prefix);
                pw.print(prefix);
                pw.print(packages.keyAt(i));
                pw.print(":");
                pw.println(Arrays.toString(packages.valueAt(i)));
            }
        }
        pw.println(" mLockTaskModeTasks" + this.mLockTaskModeTasks);
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name) {
        return this.mFocusedStack.getDumpActivitiesLocked(name);
    }

    static boolean printThisActivity(PrintWriter pw, ActivityRecord activity, String dumpPackage, boolean needSep, String prefix) {
        if (activity != null) {
            if (dumpPackage == null || dumpPackage.equals(activity.packageName)) {
                if (needSep) {
                    pw.println();
                }
                pw.print(prefix);
                pw.println(activity);
                return true;
            }
            return false;
        }
        return false;
    }

    boolean dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        boolean printed = false;
        boolean needSep = false;
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ActivityDisplay activityDisplay = this.mActivityDisplays.valueAt(displayNdx);
            pw.print("Display #");
            pw.print(activityDisplay.mDisplayId);
            pw.println(" (activities from top to bottom):");
            ArrayList<ActivityStack> stacks = activityDisplay.mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                StringBuilder stackHeader = new StringBuilder(128);
                stackHeader.append("  Stack #");
                stackHeader.append(stack.mStackId);
                stackHeader.append(":");
                stackHeader.append("\n");
                stackHeader.append("  mFullscreen=").append(stack.mFullscreen);
                stackHeader.append("\n");
                stackHeader.append("  mBounds=").append(stack.mBounds);
                printed = printed | stack.dumpActivitiesLocked(fd, pw, dumpAll, dumpClient, dumpPackage, needSep, stackHeader.toString()) | dumpHistoryList(fd, pw, stack.mLRUActivities, "    ", "Run", false, !dumpAll, false, dumpPackage, true, "    Running activities (most recent first):", null);
                boolean needSep2 = printed;
                boolean pr = printThisActivity(pw, stack.mPausingActivity, dumpPackage, printed, "    mPausingActivity: ");
                if (pr) {
                    printed = true;
                    needSep2 = false;
                }
                boolean pr2 = printThisActivity(pw, stack.mResumedActivity, dumpPackage, needSep2, "    mResumedActivity: ");
                if (pr2) {
                    printed = true;
                    needSep2 = false;
                }
                if (dumpAll) {
                    boolean pr3 = printThisActivity(pw, stack.mLastPausedActivity, dumpPackage, needSep2, "    mLastPausedActivity: ");
                    if (pr3) {
                        printed = true;
                        needSep2 = true;
                    }
                    printed |= printThisActivity(pw, stack.mLastNoHistoryActivity, dumpPackage, needSep2, "    mLastNoHistoryActivity: ");
                }
                needSep = printed;
            }
        }
        return printed | dumpHistoryList(fd, pw, this.mFinishingActivities, "  ", "Fin", false, !dumpAll, false, dumpPackage, true, "  Activities waiting to finish:", null) | dumpHistoryList(fd, pw, this.mStoppingActivities, "  ", "Stop", false, !dumpAll, false, dumpPackage, true, "  Activities waiting to stop:", null) | dumpHistoryList(fd, pw, this.mWaitingVisibleActivities, "  ", "Wait", false, !dumpAll, false, dumpPackage, true, "  Activities waiting for another to become visible:", null) | dumpHistoryList(fd, pw, this.mGoingToSleepActivities, "  ", "Sleep", false, !dumpAll, false, dumpPackage, true, "  Activities waiting to sleep:", null) | dumpHistoryList(fd, pw, this.mGoingToSleepActivities, "  ", "Sleep", false, !dumpAll, false, dumpPackage, true, "  Activities waiting to sleep:", null);
    }

    static boolean dumpHistoryList(FileDescriptor fd, PrintWriter pw, List<ActivityRecord> list, String prefix, String label, boolean complete, boolean brief, boolean client, String dumpPackage, boolean needNL, String header1, String header2) {
        TaskRecord lastTask = null;
        String innerPrefix = null;
        String[] args = null;
        boolean printed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            ActivityRecord r = list.get(i);
            if (dumpPackage == null || dumpPackage.equals(r.packageName)) {
                if (innerPrefix == null) {
                    innerPrefix = prefix + "      ";
                    args = new String[0];
                }
                printed = true;
                boolean full = !brief && (complete || !r.isInHistory());
                if (needNL) {
                    pw.println("");
                    needNL = false;
                }
                if (header1 != null) {
                    pw.println(header1);
                    header1 = null;
                }
                if (header2 != null) {
                    pw.println(header2);
                    header2 = null;
                }
                if (lastTask != r.task) {
                    lastTask = r.task;
                    pw.print(prefix);
                    pw.print(full ? "* " : "  ");
                    pw.println(lastTask);
                    if (full) {
                        lastTask.dump(pw, prefix + "  ");
                    } else if (complete && lastTask.intent != null) {
                        pw.print(prefix);
                        pw.print("  ");
                        pw.println(lastTask.intent.toInsecureStringWithClip());
                    }
                }
                pw.print(prefix);
                pw.print(full ? "  * " : "    ");
                pw.print(label);
                pw.print(" #");
                pw.print(i);
                pw.print(": ");
                pw.println(r);
                if (full) {
                    r.dump(pw, innerPrefix);
                } else if (complete) {
                    pw.print(innerPrefix);
                    pw.println(r.intent.toInsecureString());
                    if (r.app != null) {
                        pw.print(innerPrefix);
                        pw.println(r.app);
                    }
                }
                if (client && r.app != null && r.app.thread != null) {
                    pw.flush();
                    try {
                        TransferPipe tp = new TransferPipe();
                        try {
                            r.app.thread.dumpActivity(tp.getWriteFd().getFileDescriptor(), r.appToken, innerPrefix, args);
                            tp.go(fd, 2000L);
                            tp.kill();
                        } catch (Throwable th) {
                            tp.kill();
                            throw th;
                        }
                    } catch (RemoteException e) {
                        pw.println(innerPrefix + "Got a RemoteException while dumping the activity");
                    } catch (IOException e2) {
                        pw.println(innerPrefix + "Failure while dumping the activity: " + e2);
                    }
                    needNL = true;
                }
            }
        }
        return printed;
    }

    void scheduleIdleTimeoutLocked(ActivityRecord next) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "scheduleIdleTimeoutLocked: Callers=" + Debug.getCallers(4));
        }
        Message msg = this.mHandler.obtainMessage(100, next);
        this.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    final void scheduleIdleLocked() {
        this.mHandler.sendEmptyMessage(101);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord r) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "removeTimeoutsForActivity: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.removeMessages(100, r);
    }

    final void scheduleResumeTopActivities() {
        if (this.mHandler.hasMessages(102)) {
            return;
        }
        this.mHandler.sendEmptyMessage(102);
    }

    void removeSleepTimeouts() {
        this.mSleepTimeout = false;
        this.mHandler.removeMessages(103);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(103, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display added displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(105, displayId, 0));
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display removed displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(107, displayId, 0));
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display changed displayId=" + displayId);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(106, displayId, 0));
    }

    private void handleDisplayAdded(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                boolean newDisplay = this.mActivityDisplays.get(displayId) == null;
                if (newDisplay) {
                    ActivityDisplay activityDisplay = new ActivityDisplay(displayId);
                    if (activityDisplay.mDisplay == null) {
                        Slog.w(TAG, "Display " + displayId + " gone before initialization complete");
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    } else {
                        this.mActivityDisplays.put(displayId, activityDisplay);
                        calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (newDisplay) {
                    this.mWindowManager.onDisplayAdded(displayId);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void calculateDefaultMinimalSizeOfResizeableTasks(ActivityDisplay display) {
        this.mDefaultMinSizeOfResizeableTask = this.mService.mContext.getResources().getDimensionPixelSize(R.dimen.car_title2_size);
        if (!"1".equals(SystemProperties.get("ro.mtk_res_switch"))) {
            return;
        }
        this.mDefaultMinSizeOfResizeableTask /= 2;
    }

    private void handleDisplayRemoved(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    ArrayList<ActivityStack> stacks = activityDisplay.mStacks;
                    for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                        stacks.get(stackNdx).mActivityContainer.detachLocked();
                    }
                    this.mActivityDisplays.remove(displayId);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mWindowManager.onDisplayRemoved(displayId);
    }

    private void handleDisplayChanged(int displayId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mWindowManager.onDisplayChanged(displayId);
    }

    private ActivityManager.StackInfo getStackInfoLocked(ActivityStack stack) {
        int iIndexOf;
        String strFlattenToString;
        ActivityDisplay display = this.mActivityDisplays.get(0);
        ActivityManager.StackInfo info = new ActivityManager.StackInfo();
        this.mWindowManager.getStackBounds(stack.mStackId, info.bounds);
        info.displayId = 0;
        info.stackId = stack.mStackId;
        info.userId = stack.mCurrentUser;
        info.visible = stack.getStackVisibilityLocked(null) == 1;
        if (display != null) {
            iIndexOf = display.mStacks.indexOf(stack);
        } else {
            iIndexOf = 0;
        }
        info.position = iIndexOf;
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        int numTasks = tasks.size();
        int[] taskIds = new int[numTasks];
        String[] taskNames = new String[numTasks];
        Rect[] taskBounds = new Rect[numTasks];
        int[] taskUserIds = new int[numTasks];
        for (int i = 0; i < numTasks; i++) {
            TaskRecord task = tasks.get(i);
            taskIds[i] = task.taskId;
            if (task.origActivity != null) {
                strFlattenToString = task.origActivity.flattenToString();
            } else if (task.realActivity != null) {
                strFlattenToString = task.realActivity.flattenToString();
            } else {
                strFlattenToString = task.getTopActivity() != null ? task.getTopActivity().packageName : "unknown";
            }
            taskNames[i] = strFlattenToString;
            taskBounds[i] = new Rect();
            this.mWindowManager.getTaskBounds(task.taskId, taskBounds[i]);
            taskUserIds[i] = task.userId;
        }
        info.taskIds = taskIds;
        info.taskNames = taskNames;
        info.taskBounds = taskBounds;
        info.taskUserIds = taskUserIds;
        ActivityRecord top = stack.topRunningActivityLocked();
        info.topActivity = top != null ? top.intent.getComponent() : null;
        return info;
    }

    ActivityManager.StackInfo getStackInfoLocked(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            return getStackInfoLocked(stack);
        }
        return null;
    }

    ArrayList<ActivityManager.StackInfo> getAllStackInfosLocked() {
        ArrayList<ActivityManager.StackInfo> list = new ArrayList<>();
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int ndx = stacks.size() - 1; ndx >= 0; ndx--) {
                list.add(getStackInfoLocked(stacks.get(ndx)));
            }
        }
        return list;
    }

    TaskRecord getLockedTaskLocked() {
        int top = this.mLockTaskModeTasks.size() - 1;
        if (top >= 0) {
            return this.mLockTaskModeTasks.get(top);
        }
        return null;
    }

    boolean isLockedTask(TaskRecord task) {
        return this.mLockTaskModeTasks.contains(task);
    }

    boolean isLastLockedTask(TaskRecord task) {
        if (this.mLockTaskModeTasks.size() == 1) {
            return this.mLockTaskModeTasks.contains(task);
        }
        return false;
    }

    void removeLockedTaskLocked(TaskRecord task) {
        if (!this.mLockTaskModeTasks.remove(task)) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, "removeLockedTaskLocked: removed " + task);
        }
        if (!this.mLockTaskModeTasks.isEmpty()) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.d(TAG_LOCKTASK, "removeLockedTask: task=" + task + " last task, reverting locktask mode. Callers=" + Debug.getCallers(3));
        }
        Message lockTaskMsg = Message.obtain();
        lockTaskMsg.arg1 = task.userId;
        lockTaskMsg.what = 110;
        this.mHandler.sendMessage(lockTaskMsg);
    }

    void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredStackId, int actualStackId) {
        handleNonResizableTaskIfNeeded(task, preferredStackId, actualStackId, false);
    }

    void handleNonResizableTaskIfNeeded(TaskRecord task, int preferredStackId, int actualStackId, boolean forceNonResizable) {
        if ((!isStackDockedInEffect(actualStackId) && preferredStackId != 3) || task.isHomeTask()) {
            return;
        }
        ActivityRecord topActivity = task.getTopActivity();
        if (!task.canGoInDockedStack() || forceNonResizable) {
            this.mService.mHandler.sendEmptyMessage(68);
            moveTasksToFullscreenStackLocked(3, actualStackId == 3);
        } else {
            if (topActivity == null || !topActivity.isNonResizableOrForced() || topActivity.noDisplay) {
                return;
            }
            String packageName = topActivity.appInfo.packageName;
            this.mService.mHandler.obtainMessage(67, task.taskId, 0, packageName).sendToTarget();
        }
    }

    void showLockTaskToast() {
        if (this.mLockTaskNotify == null) {
            return;
        }
        this.mLockTaskNotify.showToast(this.mLockTaskModeState);
    }

    void showLockTaskEscapeMessageLocked(TaskRecord task) {
        if (!this.mLockTaskModeTasks.contains(task)) {
            return;
        }
        this.mHandler.sendEmptyMessage(113);
    }

    void setLockTaskModeLocked(TaskRecord task, int lockTaskModeState, String reason, boolean andResume) {
        if (task == null) {
            TaskRecord lockedTask = getLockedTaskLocked();
            if (lockedTask != null) {
                removeLockedTaskLocked(lockedTask);
                if (!this.mLockTaskModeTasks.isEmpty()) {
                    if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                        Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Tasks remaining, can't unlock");
                    }
                    lockedTask.performClearTaskLocked();
                    resumeFocusedStackTopActivityLocked();
                    return;
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: No tasks to unlock. Callers=" + Debug.getCallers(4));
                return;
            }
            return;
        }
        if (task.mLockTaskAuth == 0) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Can't lock due to auth");
                return;
            }
            return;
        }
        if (isLockTaskModeViolation(task)) {
            Slog.e(TAG_LOCKTASK, "setLockTaskMode: Attempt to start an unauthorized lock task.");
            return;
        }
        if (this.mLockTaskModeTasks.isEmpty()) {
            Message lockTaskMsg = Message.obtain();
            lockTaskMsg.obj = task.intent.getComponent().getPackageName();
            lockTaskMsg.arg1 = task.userId;
            lockTaskMsg.what = 109;
            lockTaskMsg.arg2 = lockTaskModeState;
            this.mHandler.sendMessage(lockTaskMsg);
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, "setLockTaskModeLocked: Locking to " + task + " Callers=" + Debug.getCallers(4));
        }
        this.mLockTaskModeTasks.remove(task);
        this.mLockTaskModeTasks.add(task);
        if (task.mLockTaskUid == -1) {
            task.mLockTaskUid = task.effectiveUid;
        }
        if (andResume) {
            findTaskToMoveToFrontLocked(task, 0, null, reason, lockTaskModeState != 0);
            resumeFocusedStackTopActivityLocked();
        } else {
            if (lockTaskModeState == 0) {
                return;
            }
            handleNonResizableTaskIfNeeded(task, -1, task.stack.mStackId, true);
        }
    }

    boolean isLockTaskModeViolation(TaskRecord task) {
        return isLockTaskModeViolation(task, false);
    }

    boolean isLockTaskModeViolation(TaskRecord task, boolean isNewClearTask) {
        if (getLockedTaskLocked() == task && !isNewClearTask) {
            return false;
        }
        int lockTaskAuth = task.mLockTaskAuth;
        switch (lockTaskAuth) {
            case 0:
                return !this.mLockTaskModeTasks.isEmpty();
            case 1:
                return !this.mLockTaskModeTasks.isEmpty();
            case 2:
            case 3:
            case 4:
                return false;
            default:
                Slog.w(TAG, "isLockTaskModeViolation: invalid lockTaskAuth value=" + lockTaskAuth);
                return true;
        }
    }

    void onLockTaskPackagesUpdatedLocked() {
        boolean didSomething = false;
        for (int taskNdx = this.mLockTaskModeTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord lockedTask = this.mLockTaskModeTasks.get(taskNdx);
            boolean wasWhitelisted = lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3;
            lockedTask.setLockTaskAuth();
            boolean isWhitelisted = lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3;
            if (wasWhitelisted && !isWhitelisted) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: removing " + lockedTask + " mLockTaskAuth=" + lockedTask.lockTaskAuthToString());
                }
                removeLockedTaskLocked(lockedTask);
                lockedTask.performClearTaskLocked();
                didSomething = true;
            }
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.onLockTaskPackagesUpdatedLocked();
            }
        }
        ActivityRecord r = topRunningActivityLocked();
        TaskRecord taskRecord = r != null ? r.task : null;
        if (this.mLockTaskModeTasks.isEmpty() && taskRecord != null && taskRecord.mLockTaskAuth == 2) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: starting new locktask task=" + taskRecord);
            }
            setLockTaskModeLocked(taskRecord, 1, "package updated", false);
            didSomething = true;
        }
        if (!didSomething) {
            return;
        }
        resumeFocusedStackTopActivityLocked();
    }

    int getLockTaskModeState() {
        return this.mLockTaskModeState;
    }

    void activityRelaunchedLocked(IBinder token) {
        this.mWindowManager.notifyAppRelaunchingFinished(token);
    }

    void activityRelaunchingLocked(ActivityRecord r) {
        this.mWindowManager.notifyAppRelaunching(r.appToken);
    }

    void logStackState() {
        this.mActivityMetricsLogger.logWindowState();
    }

    void scheduleReportMultiWindowModeChanged(TaskRecord task) {
        for (int i = task.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = task.mActivities.get(i);
            if (r.app != null && r.app.thread != null) {
                this.mMultiWindowModeChangedActivities.add(r);
            }
        }
        if (this.mHandler.hasMessages(114)) {
            return;
        }
        this.mHandler.sendEmptyMessage(114);
    }

    void scheduleReportPictureInPictureModeChangedIfNeeded(TaskRecord task, ActivityStack prevStack) {
        ActivityStack stack = task.stack;
        if (prevStack != null && prevStack != stack) {
            if (prevStack.mStackId != 4 && stack.mStackId != 4) {
                return;
            }
            for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                ActivityRecord r = task.mActivities.get(i);
                if (r.app != null && r.app.thread != null) {
                    this.mPipModeChangedActivities.add(r);
                }
            }
            if (this.mHandler.hasMessages(115)) {
                return;
            }
            this.mHandler.sendEmptyMessage(115);
        }
    }

    void setDockedStackMinimized(boolean minimized) {
        ActivityStack dockedStack;
        ActivityRecord top;
        this.mIsDockMinimized = minimized;
        if (minimized || (dockedStack = getStack(3)) == null || (top = dockedStack.topRunningActivityLocked()) == null || !this.mService.mUserController.shouldConfirmCredentials(top.userId)) {
            return;
        }
        this.mService.mActivityStarter.showConfirmDeviceCredential(top.userId);
    }

    private final class ActivityStackSupervisorHandler extends Handler {
        public ActivityStackSupervisorHandler(Looper looper) {
            super(looper);
        }

        void activityIdleInternal(ActivityRecord r) {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.activityIdleInternalLocked(r != null ? r.appToken : null, true, null);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 100:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_TIMEOUT_MSG: r=" + msg.obj);
                        }
                        if (ActivityStackSupervisor.this.mService.mDidDexOpt) {
                            ActivityStackSupervisor.this.mService.mDidDexOpt = false;
                            Message nmsg = ActivityStackSupervisor.this.mHandler.obtainMessage(100);
                            nmsg.obj = msg.obj;
                            ActivityStackSupervisor.this.mHandler.sendMessageDelayed(nmsg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                            return;
                        }
                        activityIdleInternal((ActivityRecord) msg.obj);
                        return;
                    case 101:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_NOW_MSG: r=" + msg.obj);
                        }
                        activityIdleInternal((ActivityRecord) msg.obj);
                        return;
                    case 102:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ActivityStackSupervisor.this.resumeFocusedStackTopActivityLocked();
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        return;
                    case 103:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityStackSupervisor.this.mService.isSleepingOrShuttingDownLocked()) {
                                    Slog.w(ActivityStackSupervisor.TAG, "Sleep timeout!  Sleeping now.");
                                    ActivityStackSupervisor.this.mSleepTimeout = true;
                                    ActivityStackSupervisor.this.checkReadyForSleepLocked();
                                }
                            } catch (Throwable th2) {
                                throw th2;
                            }
                            break;
                        }
                        return;
                    case 104:
                        if (ActivityStackSupervisor.this.mService.mDidDexOpt) {
                            ActivityStackSupervisor.this.mService.mDidDexOpt = false;
                            ActivityStackSupervisor.this.mHandler.sendEmptyMessageDelayed(104, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                            return;
                        }
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityStackSupervisor.this.mLaunchingActivity.isHeld()) {
                                    Slog.w(ActivityStackSupervisor.TAG, "Launch timeout has expired, giving up wake lock!");
                                    ActivityStackSupervisor.this.mLaunchingActivity.release();
                                }
                            } catch (Throwable th3) {
                                throw th3;
                            }
                            break;
                        }
                        return;
                    case 105:
                        ActivityStackSupervisor.this.handleDisplayAdded(msg.arg1);
                        return;
                    case 106:
                        ActivityStackSupervisor.this.handleDisplayChanged(msg.arg1);
                        return;
                    case 107:
                        ActivityStackSupervisor.this.handleDisplayRemoved(msg.arg1);
                        return;
                    case 108:
                        ActivityContainer container = (ActivityContainer) msg.obj;
                        IActivityContainerCallback callback = container.mCallback;
                        if (callback == null) {
                            return;
                        }
                        try {
                            callback.setVisible(container.asBinder(), msg.arg1 == 1);
                            return;
                        } catch (RemoteException e) {
                            return;
                        }
                    case 109:
                        try {
                            if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                                ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                            }
                            ActivityStackSupervisor.this.mLockTaskNotify.show(true);
                            ActivityStackSupervisor.this.mLockTaskModeState = msg.arg2;
                            if (ActivityStackSupervisor.this.getStatusBarService() != null) {
                                int flags = 0;
                                if (ActivityStackSupervisor.this.mLockTaskModeState == 1) {
                                    flags = 62849024;
                                } else if (ActivityStackSupervisor.this.mLockTaskModeState == 2) {
                                    flags = 43974656;
                                }
                                ActivityStackSupervisor.this.getStatusBarService().disable(flags, ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.this.mService.mContext.getPackageName());
                            }
                            ActivityStackSupervisor.this.mWindowManager.disableKeyguard(ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.LOCK_TASK_TAG);
                            if (ActivityStackSupervisor.this.getDevicePolicyManager() == null) {
                                return;
                            }
                            ActivityStackSupervisor.this.getDevicePolicyManager().notifyLockTaskModeChanged(true, (String) msg.obj, msg.arg1);
                            return;
                        } catch (RemoteException ex) {
                            throw new RuntimeException(ex);
                        }
                    case 110:
                        try {
                            if (ActivityStackSupervisor.this.getStatusBarService() != null) {
                                ActivityStackSupervisor.this.getStatusBarService().disable(0, ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.this.mService.mContext.getPackageName());
                            }
                            ActivityStackSupervisor.this.mWindowManager.reenableKeyguard(ActivityStackSupervisor.this.mToken);
                            if (ActivityStackSupervisor.this.getDevicePolicyManager() != null) {
                                ActivityStackSupervisor.this.getDevicePolicyManager().notifyLockTaskModeChanged(false, (String) null, msg.arg1);
                            }
                            if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                                ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                            }
                            ActivityStackSupervisor.this.mLockTaskNotify.show(false);
                            if (ActivityStackSupervisor.this.mLockTaskModeState == 2 && ActivityStackSupervisor.this.shouldLockKeyguard()) {
                                ActivityStackSupervisor.this.mWindowManager.lockNow(null);
                                ActivityStackSupervisor.this.mWindowManager.dismissKeyguard();
                                new LockPatternUtils(ActivityStackSupervisor.this.mService.mContext).requireCredentialEntry(-1);
                                break;
                            }
                            return;
                        } catch (RemoteException ex2) {
                            throw new RuntimeException(ex2);
                        }
                    case 111:
                        ActivityContainer container2 = (ActivityContainer) msg.obj;
                        IActivityContainerCallback callback2 = container2.mCallback;
                        if (callback2 == null) {
                            return;
                        }
                        try {
                            callback2.onAllActivitiesComplete(container2.asBinder());
                            return;
                        } catch (RemoteException e2) {
                            return;
                        }
                    case 112:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ActivityRecord r = ActivityRecord.forTokenLocked((IBinder) msg.obj);
                                if (r != null) {
                                    ActivityStackSupervisor.this.handleLaunchTaskBehindCompleteLocked(r);
                                }
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        return;
                    case 113:
                        if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                            ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                        }
                        ActivityStackSupervisor.this.mLockTaskNotify.showToast(2);
                        return;
                    case 114:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                for (int i = ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.size() - 1; i >= 0; i--) {
                                    ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.remove(i).scheduleMultiWindowModeChanged();
                                }
                            } catch (Throwable th4) {
                                throw th4;
                            }
                        }
                        return;
                    case 115:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                for (int i2 = ActivityStackSupervisor.this.mPipModeChangedActivities.size() - 1; i2 >= 0; i2--) {
                                    ActivityStackSupervisor.this.mPipModeChangedActivities.remove(i2).schedulePictureInPictureModeChanged();
                                }
                            } catch (Throwable th5) {
                                throw th5;
                            }
                        }
                        return;
                    default:
                        return;
                }
            } finally {
                ActivityStackSupervisor.this.mLockTaskModeState = 0;
            }
            ActivityStackSupervisor.this.mLockTaskModeState = 0;
        }
    }

    private boolean shouldLockKeyguard() {
        try {
            return Settings.Secure.getInt(this.mService.mContext.getContentResolver(), "lock_to_app_exit_locked") != 0;
        } catch (Settings.SettingNotFoundException e) {
            EventLog.writeEvent(1397638484, "127605586", -1, "");
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this.mService.mContext);
            return lockPatternUtils.isSecure(this.mCurrentUser);
        }
    }

    class ActivityContainer extends IActivityContainer.Stub {
        static final int CONTAINER_STATE_FINISHING = 2;
        static final int CONTAINER_STATE_HAS_SURFACE = 0;
        static final int CONTAINER_STATE_NO_SURFACE = 1;
        static final int FORCE_NEW_TASK_FLAGS = 402718720;
        ActivityDisplay mActivityDisplay;
        String mIdString;
        final ActivityStack mStack;
        final int mStackId;
        IActivityContainerCallback mCallback = null;
        ActivityRecord mParentActivity = null;
        boolean mVisible = true;
        int mContainerState = 0;

        ActivityContainer(int stackId) {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mStackId = stackId;
                    this.mStack = new ActivityStack(this, ActivityStackSupervisor.this.mRecentTasks);
                    this.mIdString = "ActivtyContainer{" + this.mStackId + "}";
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(ActivityStackSupervisor.TAG_STACK, "Creating " + this);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void attachToDisplayLocked(ActivityDisplay activityDisplay, boolean onTop) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(ActivityStackSupervisor.TAG_STACK, "attachToDisplayLocked: " + this + " to display=" + activityDisplay + " onTop=" + onTop);
            }
            this.mActivityDisplay = activityDisplay;
            this.mStack.attachDisplay(activityDisplay, onTop);
            activityDisplay.attachActivities(this.mStack, onTop);
        }

        public void attachToDisplay(int displayId) {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityDisplay activityDisplay = (ActivityDisplay) ActivityStackSupervisor.this.mActivityDisplays.get(displayId);
                    if (activityDisplay == null) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    } else {
                        attachToDisplayLocked(activityDisplay, true);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        public int getDisplayId() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (this.mActivityDisplay == null) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return -1;
                    }
                    int i = this.mActivityDisplay.mDisplayId;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return i;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        public int getStackId() {
            int i;
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    i = this.mStackId;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return i;
        }

        public boolean injectEvent(InputEvent event) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (ActivityStackSupervisor.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (this.mActivityDisplay != null) {
                            return ActivityStackSupervisor.this.mInputManagerInternal.injectInputEvent(event, this.mActivityDisplay.mDisplayId, 0);
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return false;
                    } finally {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void release() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (this.mContainerState == 2) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mContainerState = 2;
                    long origId = Binder.clearCallingIdentity();
                    try {
                        this.mStack.finishAllActivitiesLocked(false);
                        ActivityStackSupervisor.this.mService.mActivityStarter.removePendingActivityLaunchesLocked(this.mStack);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        protected void detachLocked() {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(ActivityStackSupervisor.TAG_STACK, "detachLocked: " + this + " from display=" + this.mActivityDisplay + " Callers=" + Debug.getCallers(2));
            }
            if (this.mActivityDisplay != null) {
                this.mActivityDisplay.detachActivitiesLocked(this.mStack);
                this.mActivityDisplay = null;
                this.mStack.detachDisplay();
            }
        }

        public final int startActivity(Intent intent) {
            return ActivityStackSupervisor.this.mService.startActivity(intent, this);
        }

        public final int startActivityIntentSender(IIntentSender intentSender) throws TransactionTooLargeException {
            ActivityStackSupervisor.this.mService.enforceNotIsolatedCaller("ActivityContainer.startActivityIntentSender");
            if (!(intentSender instanceof PendingIntentRecord)) {
                throw new IllegalArgumentException("Bad PendingIntent object");
            }
            int userId = ActivityStackSupervisor.this.mService.mUserController.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), ActivityStackSupervisor.this.mCurrentUser, false, 2, "ActivityContainer", null);
            PendingIntentRecord pendingIntent = (PendingIntentRecord) intentSender;
            checkEmbeddedAllowedInner(userId, pendingIntent.key.requestIntent, pendingIntent.key.requestResolvedType);
            return pendingIntent.sendInner(0, null, null, null, null, null, null, 0, FORCE_NEW_TASK_FLAGS, FORCE_NEW_TASK_FLAGS, null, this);
        }

        void checkEmbeddedAllowedInner(int userId, Intent intent, String resolvedType) {
            ActivityInfo aInfo = ActivityStackSupervisor.this.resolveActivity(intent, resolvedType, 0, null, userId);
            if (aInfo == null || (aInfo.flags & Integer.MIN_VALUE) != 0) {
            } else {
                throw new SecurityException("Attempt to embed activity that has not set allowEmbedded=\"true\"");
            }
        }

        public IBinder asBinder() {
            return this;
        }

        public void setSurface(Surface surface, int width, int height, int density) {
            ActivityStackSupervisor.this.mService.enforceNotIsolatedCaller("ActivityContainer.attachToSurface");
        }

        ActivityStackSupervisor getOuter() {
            return ActivityStackSupervisor.this;
        }

        boolean isAttachedLocked() {
            return this.mActivityDisplay != null;
        }

        void setVisible(boolean visible) {
            if (this.mVisible == visible) {
                return;
            }
            this.mVisible = visible;
            if (this.mCallback != null) {
                ActivityStackSupervisor.this.mHandler.obtainMessage(108, visible ? 1 : 0, 0, this).sendToTarget();
            }
        }

        void setDrawn() {
        }

        boolean isEligibleForNewTasks() {
            return true;
        }

        void onTaskListEmptyLocked() {
            detachLocked();
            ActivityStackSupervisor.this.deleteActivityContainer(this);
            ActivityStackSupervisor.this.mHandler.obtainMessage(111, this).sendToTarget();
        }

        public String toString() {
            return this.mIdString + (this.mActivityDisplay == null ? "N" : "A");
        }
    }

    private class VirtualActivityContainer extends ActivityContainer {
        boolean mDrawn;
        Surface mSurface;

        VirtualActivityContainer(ActivityRecord parent, IActivityContainerCallback callback) {
            super(ActivityStackSupervisor.this.getNextStackId());
            this.mDrawn = false;
            this.mParentActivity = parent;
            this.mCallback = callback;
            this.mContainerState = 1;
            this.mIdString = "VirtualActivityContainer{" + this.mStackId + ", parent=" + this.mParentActivity + "}";
        }

        @Override
        public void setSurface(Surface surface, int width, int height, int density) {
            super.setSurface(surface, width, height, density);
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    long origId = Binder.clearCallingIdentity();
                    try {
                        setSurfaceLocked(surface, width, height, density);
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        private void setSurfaceLocked(Surface surface, int width, int height, int density) {
            if (this.mContainerState == 2) {
                return;
            }
            VirtualActivityDisplay virtualActivityDisplay = (VirtualActivityDisplay) this.mActivityDisplay;
            if (virtualActivityDisplay == null) {
                virtualActivityDisplay = ActivityStackSupervisor.this.new VirtualActivityDisplay(width, height, density);
                this.mActivityDisplay = virtualActivityDisplay;
                ActivityStackSupervisor.this.mActivityDisplays.put(virtualActivityDisplay.mDisplayId, virtualActivityDisplay);
                attachToDisplayLocked(virtualActivityDisplay, true);
            }
            if (this.mSurface != null) {
                this.mSurface.release();
            }
            this.mSurface = surface;
            if (surface != null) {
                ActivityStackSupervisor.this.resumeFocusedStackTopActivityLocked();
            } else {
                this.mContainerState = 1;
                ((VirtualActivityDisplay) this.mActivityDisplay).setSurface(null);
                if (this.mStack.mPausingActivity == null && this.mStack.mResumedActivity != null) {
                    this.mStack.startPausingLocked(false, true, false, false);
                }
            }
            setSurfaceIfReadyLocked();
            if (!ActivityManagerDebugConfig.DEBUG_STACK) {
                return;
            }
            Slog.d(ActivityStackSupervisor.TAG_STACK, "setSurface: " + this + " to display=" + virtualActivityDisplay);
        }

        @Override
        boolean isAttachedLocked() {
            if (this.mSurface != null) {
                return super.isAttachedLocked();
            }
            return false;
        }

        @Override
        void setDrawn() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mDrawn = true;
                    setSurfaceIfReadyLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        boolean isEligibleForNewTasks() {
            return false;
        }

        private void setSurfaceIfReadyLocked() {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(ActivityStackSupervisor.TAG_STACK, "setSurfaceIfReadyLocked: mDrawn=" + this.mDrawn + " mContainerState=" + this.mContainerState + " mSurface=" + this.mSurface);
            }
            if (this.mDrawn && this.mSurface != null && this.mContainerState == 1) {
                ((VirtualActivityDisplay) this.mActivityDisplay).setSurface(this.mSurface);
                this.mContainerState = 0;
            }
        }
    }

    class ActivityDisplay {
        Display mDisplay;
        int mDisplayId;
        DisplayInfo mDisplayInfo = new DisplayInfo();
        final ArrayList<ActivityStack> mStacks = new ArrayList<>();
        ActivityRecord mVisibleBehindActivity;

        ActivityDisplay() {
        }

        ActivityDisplay(int displayId) {
            Display display = ActivityStackSupervisor.this.mDisplayManager.getDisplay(displayId);
            if (display == null) {
                return;
            }
            init(display);
        }

        void init(Display display) {
            this.mDisplay = display;
            this.mDisplayId = display.getDisplayId();
            this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        }

        void attachActivities(ActivityStack stack, boolean onTop) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(ActivityStackSupervisor.TAG_STACK, "attachActivities: attaching " + stack + " to displayId=" + this.mDisplayId + " onTop=" + onTop);
            }
            if (onTop) {
                this.mStacks.add(stack);
            } else {
                this.mStacks.add(0, stack);
            }
        }

        void detachActivitiesLocked(ActivityStack stack) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(ActivityStackSupervisor.TAG_STACK, "detachActivitiesLocked: detaching " + stack + " from displayId=" + this.mDisplayId);
            }
            this.mStacks.remove(stack);
        }

        void setVisibleBehindActivity(ActivityRecord r) {
            this.mVisibleBehindActivity = r;
        }

        boolean hasVisibleBehindActivity() {
            return this.mVisibleBehindActivity != null;
        }

        public String toString() {
            return "ActivityDisplay={" + this.mDisplayId + " numStacks=" + this.mStacks.size() + "}";
        }
    }

    class VirtualActivityDisplay extends ActivityDisplay {
        VirtualDisplay mVirtualDisplay;

        VirtualActivityDisplay(int width, int height, int density) {
            super();
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            this.mVirtualDisplay = dm.createVirtualDisplay(ActivityStackSupervisor.this.mService.mContext, (MediaProjection) null, ActivityStackSupervisor.VIRTUAL_DISPLAY_BASE_NAME, width, height, density, (Surface) null, 9, (VirtualDisplay.Callback) null, (Handler) null);
            init(this.mVirtualDisplay.getDisplay());
            ActivityStackSupervisor.this.mWindowManager.handleDisplayAdded(this.mDisplayId);
        }

        void setSurface(Surface surface) {
            if (this.mVirtualDisplay == null) {
                return;
            }
            this.mVirtualDisplay.setSurface(surface);
        }

        @Override
        void detachActivitiesLocked(ActivityStack stack) {
            super.detachActivitiesLocked(stack);
            if (this.mVirtualDisplay == null) {
                return;
            }
            this.mVirtualDisplay.release();
            this.mVirtualDisplay = null;
        }

        @Override
        public String toString() {
            return "VirtualActivityDisplay={" + this.mDisplayId + "}";
        }
    }

    private static void fitWithinBounds(Rect bounds, Rect stackBounds) {
        if (stackBounds == null || stackBounds.contains(bounds)) {
            return;
        }
        if (bounds.left < stackBounds.left || bounds.right > stackBounds.right) {
            int maxRight = stackBounds.right - (stackBounds.width() / 3);
            int horizontalDiff = stackBounds.left - bounds.left;
            if ((horizontalDiff < 0 && bounds.left >= maxRight) || bounds.left + horizontalDiff >= maxRight) {
                horizontalDiff = maxRight - bounds.left;
            }
            bounds.left += horizontalDiff;
            bounds.right += horizontalDiff;
        }
        if (bounds.top >= stackBounds.top && bounds.bottom <= stackBounds.bottom) {
            return;
        }
        int maxBottom = stackBounds.bottom - (stackBounds.height() / 3);
        int verticalDiff = stackBounds.top - bounds.top;
        if ((verticalDiff < 0 && bounds.top >= maxBottom) || bounds.top + verticalDiff >= maxBottom) {
            verticalDiff = maxBottom - bounds.top;
        }
        bounds.top += verticalDiff;
        bounds.bottom += verticalDiff;
    }

    ActivityStack findStackBehind(ActivityStack stack) {
        ActivityDisplay display = this.mActivityDisplays.get(0);
        if (display == null) {
            return null;
        }
        ArrayList<ActivityStack> stacks = display.mStacks;
        for (int i = stacks.size() - 1; i >= 0; i--) {
            if (stacks.get(i) == stack && i > 0) {
                return stacks.get(i - 1);
            }
        }
        throw new IllegalStateException("Failed to find a stack behind stack=" + stack + " in=" + stacks);
    }

    private void setResizingDuringAnimation(int taskId) {
        this.mResizingTasksDuringAnimation.add(Integer.valueOf(taskId));
        this.mWindowManager.setTaskDockedResizing(taskId, true);
    }

    final int startActivityFromRecentsInner(int taskId, Bundle bOptions) {
        ActivityOptions activityOptions = bOptions != null ? new ActivityOptions(bOptions) : null;
        int launchStackId = activityOptions != null ? activityOptions.getLaunchStackId() : -1;
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_MULTIWINDOW) {
            Slog.d(TAG, "startActivityFromRecentsInner: launchStackId=" + launchStackId + " taskId=" + taskId);
        }
        if (launchStackId == 0) {
            throw new IllegalArgumentException("startActivityFromRecentsInner: Task " + taskId + " can't be launch in the home stack.");
        }
        if (launchStackId == 3) {
            this.mWindowManager.setDockedStackCreateState(activityOptions.getDockCreateMode(), null);
            deferUpdateBounds(0);
            this.mWindowManager.prepareAppTransition(19, false);
        }
        TaskRecord task = anyTaskForIdLocked(taskId, true, launchStackId);
        if (task == null) {
            continueUpdateBounds(0);
            this.mWindowManager.executeAppTransition();
            throw new IllegalArgumentException("startActivityFromRecentsInner: Task " + taskId + " not found.");
        }
        ActivityStack focusedStack = getFocusedStack();
        ActivityRecord activityRecord = focusedStack != null ? focusedStack.topActivity() : null;
        if (launchStackId != -1 && task.stack.mStackId != launchStackId) {
            moveTaskToStackLocked(taskId, launchStackId, true, true, "startActivityFromRecents", true);
        }
        if (!this.mService.mUserController.shouldConfirmCredentials(task.userId) && task.getRootActivity() != null) {
            this.mActivityMetricsLogger.notifyActivityLaunching();
            this.mService.moveTaskToFrontLocked(task.taskId, 0, bOptions);
            this.mActivityMetricsLogger.notifyActivityLaunched(2, task.getTopActivity());
            if (launchStackId == 3) {
                setResizingDuringAnimation(taskId);
            }
            this.mService.mActivityStarter.postStartActivityUncheckedProcessing(task.getTopActivity(), 2, activityRecord != null ? activityRecord.task.stack.mStackId : -1, activityRecord, task.stack);
            if (!ActivityManagerDebugConfig.DEBUG_MULTIWINDOW) {
                return 2;
            }
            Slog.d(TAG, "startActivityFromRecentsInner: START_TASK_TO_FRONT , t.uid=" + task.mCallingUid + ", t.pkg=" + task.mCallingPackage + ", t.userid=" + task.userId + ", t.intent=" + task.intent, new Throwable());
            return 2;
        }
        int callingUid = task.mCallingUid;
        String callingPackage = task.mCallingPackage;
        Intent intent = task.intent;
        intent.addFlags(PackageManagerService.DumpState.DUMP_DEXOPT);
        int userId = task.userId;
        int result = this.mService.startActivityInPackage(callingUid, callingPackage, intent, null, null, null, 0, 0, bOptions, userId, null, task);
        if (launchStackId == 3) {
            setResizingDuringAnimation(task.taskId);
        }
        if (ActivityManagerDebugConfig.DEBUG_MULTIWINDOW) {
            Slog.d(TAG, "startActivityFromRecentsInner: result=" + result + ", t.uid=" + task.mCallingUid + ", t.pkg=" + task.mCallingPackage + ", t.userid=" + task.userId + ", t.intent=" + task.intent, new Throwable());
        }
        return result;
    }

    public List<IBinder> getTopVisibleActivities() {
        ActivityRecord top;
        ActivityDisplay display = this.mActivityDisplays.get(0);
        if (display == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<IBinder> topActivityTokens = new ArrayList<>();
        ArrayList<ActivityStack> stacks = display.mStacks;
        for (int i = stacks.size() - 1; i >= 0; i--) {
            ActivityStack stack = stacks.get(i);
            if (stack.getStackVisibilityLocked(null) == 1 && (top = stack.topActivity()) != null) {
                if (stack == this.mFocusedStack) {
                    topActivityTokens.add(0, top.appToken);
                } else {
                    topActivityTokens.add(top.appToken);
                }
            }
        }
        return topActivityTokens;
    }

    class SimpleActivityInfo {
        String packageName = null;
        String activityName = null;
        int activityType = 0;

        SimpleActivityInfo() {
        }
    }

    public boolean isUpdatedLastActivityWhenStartHome(String packageName, String activityName) {
        if (this.mLastResumedActivity.activityType != 1 || packageName == null || activityName == null) {
            return false;
        }
        return (packageName.equals(this.mLastResumedActivity.packageName) && activityName.equals(this.mLastResumedActivity.activityName)) ? false : true;
    }

    public ActivityStack findStack(int stackId) {
        ActivityDisplay display = this.mActivityDisplays.get(0);
        if (display == null) {
            return null;
        }
        ArrayList<ActivityStack> stacks = display.mStacks;
        if (MultiWindowManager.DEBUG) {
            Slog.d(TAG_STACK, "findStack, stacks = " + stacks);
        }
        for (ActivityStack stack : stacks) {
            if (stack.mStackId == stackId) {
                return stack;
            }
        }
        return null;
    }

    private void forceStopWaitingVisibleActivitiesLocked(int stopIndex, ActivityRecord s) {
        if (this.mStoppingActivities.size() <= ActivityStack.getMaxStoppingToForce()) {
            return;
        }
        int forceIndex = (this.mStoppingActivities.size() - 1) - ActivityStack.getMaxStoppingToForce();
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.d(TAG, "Force stop waitingVisible activity? [" + stopIndex + "," + forceIndex + "]");
        }
        if (stopIndex > forceIndex) {
            return;
        }
        boolean contain = this.mWaitingVisibleActivities.remove(s);
        if (!contain || !ActivityManagerDebugConfig.DEBUG_STATES) {
            return;
        }
        Slog.d(TAG, "Force stop waitingVisible activity: " + s);
    }
}
