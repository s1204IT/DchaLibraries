package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityContainer;
import android.app.IActivityContainerCallback;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.ResultInfo;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManagerInternal;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.InputEvent;
import android.view.Surface;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.TransferPipe;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.am.ActivityStack;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public final class ActivityStackSupervisor implements DisplayManager.DisplayListener {
    static final int CONTAINER_CALLBACK_TASK_LIST_EMPTY = 111;
    static final int CONTAINER_CALLBACK_VISIBILITY = 108;
    static final int CONTAINER_TASK_LIST_EMPTY_TIMEOUT = 112;
    static final boolean DEBUG = false;
    static final boolean DEBUG_ADD_REMOVE = false;
    static final boolean DEBUG_APP = false;
    static final boolean DEBUG_CONTAINERS = false;
    static final boolean DEBUG_IDLE = false;
    static final boolean DEBUG_RELEASE = false;
    static final boolean DEBUG_SAVED_STATE = false;
    static final boolean DEBUG_SCREENSHOTS = false;
    static final boolean DEBUG_STATES = false;
    static final boolean DEBUG_VISIBLE_BEHIND = false;
    static final int HANDLE_DISPLAY_ADDED = 105;
    static final int HANDLE_DISPLAY_CHANGED = 106;
    static final int HANDLE_DISPLAY_REMOVED = 107;
    public static final int HOME_STACK_ID = 0;
    static final int IDLE_NOW_MSG = 101;
    static final int IDLE_TIMEOUT = 10000;
    static final int IDLE_TIMEOUT_MSG = 100;
    static final int LAUNCH_TASK_BEHIND_COMPLETE = 113;
    static final int LAUNCH_TIMEOUT = 10000;
    static final int LAUNCH_TIMEOUT_MSG = 104;
    static final int LOCK_TASK_END_MSG = 110;
    static final int LOCK_TASK_START_MSG = 109;
    private static final String LOCK_TASK_TAG = "Lock-to-App";
    static final int RESUME_TOP_ACTIVITY_MSG = 102;
    static final int SLEEP_TIMEOUT = 5000;
    static final int SLEEP_TIMEOUT_MSG = 103;
    static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private static final String VIRTUAL_DISPLAY_BASE_NAME = "ActivityViewVirtualDisplay";
    boolean inResumeTopActivity;
    private int mCurrentUser;
    private IDevicePolicyManager mDevicePolicyManager;
    DisplayManager mDisplayManager;
    private ActivityStack mFocusedStack;
    PowerManager.WakeLock mGoingToSleep;
    final ActivityStackSupervisorHandler mHandler;
    private ActivityStack mHomeStack;
    InputManagerInternal mInputManagerInternal;
    private ActivityStack mLastFocusedStack;
    PowerManager.WakeLock mLaunchingActivity;
    private boolean mLeanbackOnlyDevice;
    private boolean mLockTaskIsLocked;
    TaskRecord mLockTaskModeTask;
    private LockTaskNotify mLockTaskNotify;
    final ActivityManagerService mService;
    private IStatusBarService mStatusBarService;
    WindowManagerService mWindowManager;
    private IBinder mToken = new Binder();
    private int mLastStackId = 0;
    private int mCurTaskId = 0;
    final ArrayList<ActivityRecord> mWaitingVisibleActivities = new ArrayList<>();
    final ArrayList<IActivityManager.WaitResult> mWaitingActivityVisible = new ArrayList<>();
    final ArrayList<IActivityManager.WaitResult> mWaitingActivityLaunched = new ArrayList<>();
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mGoingToSleepActivities = new ArrayList<>();
    final ArrayList<UserStartedState> mStartingUsers = new ArrayList<>();
    final ArrayList<UserStartedState> mStartingBackgroundUsers = new ArrayList<>();
    boolean mUserLeaving = false;
    boolean mSleepTimeout = false;
    SparseIntArray mUserStackInFront = new SparseIntArray(2);
    private SparseArray<ActivityContainer> mActivityContainers = new SparseArray<>();
    private final SparseArray<ActivityDisplay> mActivityDisplays = new SparseArray<>();
    final ArrayList<PendingActivityLaunch> mPendingActivityLaunches = new ArrayList<>();

    static class PendingActivityLaunch {
        final ActivityRecord r;
        final ActivityRecord sourceRecord;
        final ActivityStack stack;
        final int startFlags;

        PendingActivityLaunch(ActivityRecord _r, ActivityRecord _sourceRecord, int _startFlags, ActivityStack _stack) {
            this.r = _r;
            this.sourceRecord = _sourceRecord;
            this.startFlags = _startFlags;
            this.stack = _stack;
        }
    }

    public ActivityStackSupervisor(ActivityManagerService service) {
        this.mService = service;
        this.mHandler = new ActivityStackSupervisorHandler(this.mService.mHandler.getLooper());
    }

    void initPowerManagement() {
        PowerManager pm = (PowerManager) this.mService.mContext.getSystemService("power");
        this.mGoingToSleep = pm.newWakeLock(1, "ActivityManager-Sleep");
        this.mLaunchingActivity = pm.newWakeLock(1, "ActivityManager-Launch");
        this.mLaunchingActivity.setReferenceCounted(false);
    }

    private IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mService) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
                if (this.mStatusBarService == null) {
                    Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
                }
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        IDevicePolicyManager iDevicePolicyManager;
        synchronized (this.mService) {
            if (this.mDevicePolicyManager == null) {
                this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
                if (this.mDevicePolicyManager == null) {
                    Slog.w("ActivityManager", "warning: no DEVICE_POLICY_SERVICE");
                }
            }
            iDevicePolicyManager = this.mDevicePolicyManager;
        }
        return iDevicePolicyManager;
    }

    void setWindowManager(WindowManagerService wm) {
        synchronized (this.mService) {
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
            }
            createStackOnDisplay(0, 0);
            ActivityStack stack = getStack(0);
            this.mLastFocusedStack = stack;
            this.mFocusedStack = stack;
            this.mHomeStack = stack;
            this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            this.mLeanbackOnlyDevice = isLeanbackOnlyDevice();
        }
    }

    void notifyActivityDrawnForKeyguard() {
        this.mWindowManager.notifyActivityDrawnForKeyguard();
    }

    ActivityStack getFocusedStack() {
        return this.mFocusedStack;
    }

    ActivityStack getLastStack() {
        return this.mLastFocusedStack;
    }

    boolean isFrontStack(ActivityStack stack) {
        ActivityRecord parent = stack.mActivityContainer.mParentActivity;
        if (parent != null) {
            stack = parent.task.stack;
        }
        ArrayList<ActivityStack> stacks = stack.mStacks;
        return (stacks == null || stacks.isEmpty() || stack != stacks.get(stacks.size() + (-1))) ? false : true;
    }

    void moveHomeStack(boolean toFront, String reason) {
        ActivityRecord r;
        ArrayList<ActivityStack> stacks = this.mHomeStack.mStacks;
        int topNdx = stacks.size() - 1;
        if (topNdx > 0) {
            ActivityStack topStack = stacks.get(topNdx);
            boolean homeInFront = topStack == this.mHomeStack;
            if (homeInFront != toFront) {
                this.mLastFocusedStack = topStack;
                stacks.remove(this.mHomeStack);
                stacks.add(toFront ? topNdx : 0, this.mHomeStack);
                this.mFocusedStack = stacks.get(topNdx);
            }
            Object[] objArr = new Object[5];
            objArr[0] = Integer.valueOf(this.mCurrentUser);
            objArr[1] = Integer.valueOf(toFront ? 1 : 0);
            objArr[2] = Integer.valueOf(stacks.get(topNdx).getStackId());
            objArr[3] = Integer.valueOf(this.mFocusedStack == null ? -1 : this.mFocusedStack.getStackId());
            objArr[4] = reason;
            EventLog.writeEvent(EventLogTags.AM_HOME_STACK_MOVED, objArr);
            if ((this.mService.mBooting || !this.mService.mBooted) && (r = topRunningActivityLocked()) != null && r.idle) {
                checkFinishBootingLocked();
            }
        }
    }

    void moveHomeStackTaskToTop(int homeStackTaskType, String reason) {
        if (homeStackTaskType == 2) {
            this.mWindowManager.showRecentApps();
        } else {
            moveHomeStack(true, reason);
            this.mHomeStack.moveHomeStackTaskToTop(homeStackTaskType);
        }
    }

    boolean resumeHomeStackTask(int homeStackTaskType, ActivityRecord prev, String reason) {
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        if (homeStackTaskType == 2) {
            this.mWindowManager.showRecentApps();
            return false;
        }
        moveHomeStackTaskToTop(homeStackTaskType, reason);
        if (prev != null) {
            prev.task.setTaskToReturnTo(0);
        }
        ActivityRecord r = this.mHomeStack.topRunningActivityLocked(null);
        if (r != null && r.isHomeActivity()) {
            this.mService.setFocusedActivityLocked(r, reason);
            return resumeTopActivitiesLocked(this.mHomeStack, prev, null);
        }
        return this.mService.startHomeActivityLocked(this.mCurrentUser, reason);
    }

    TaskRecord anyTaskForIdLocked(int id) {
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
        TaskRecord task2 = this.mService.recentTaskForIdLocked(id);
        if (task2 != null && restoreRecentTaskLocked(task2)) {
            return task2;
        }
        return null;
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

    void setNextTaskId(int taskId) {
        if (taskId > this.mCurTaskId) {
            this.mCurTaskId = taskId;
        }
    }

    int getNextTaskId() {
        do {
            this.mCurTaskId++;
            if (this.mCurTaskId <= 0) {
                this.mCurTaskId = 1;
            }
        } while (anyTaskForIdLocked(this.mCurTaskId) != null);
        return this.mCurTaskId;
    }

    ActivityRecord resumedAppLocked() {
        ActivityStack stack = getFocusedStack();
        if (stack == null) {
            return null;
        }
        ActivityRecord resumedActivity = stack.mResumedActivity;
        if (resumedActivity == null || resumedActivity.app == null) {
            ActivityRecord resumedActivity2 = stack.mPausingActivity;
            if (resumedActivity2 == null || resumedActivity2.app == null) {
                return stack.topRunningActivityLocked(null);
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
                if (isFrontStack(stack) && (hr = stack.topRunningActivityLocked(null)) != null && hr.app == null && app.uid == hr.info.applicationInfo.uid && processName.equals(hr.processName)) {
                    try {
                        if (realStartActivityLocked(hr, app, true, true)) {
                            didSomething = true;
                        }
                    } catch (RemoteException e) {
                        Slog.w("ActivityManager", "Exception in new application when starting activity " + hr.intent.getComponent().flattenToShortString(), e);
                        throw e;
                    }
                }
            }
        }
        if (!didSomething) {
            ensureActivitiesVisibleLocked(null, 0);
        }
        return didSomething;
    }

    boolean allResumedActivitiesIdle() {
        ActivityRecord resumedActivity;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (isFrontStack(stack) && stack.numActivities() != 0 && ((resumedActivity = stack.mResumedActivity) == null || !resumedActivity.idle)) {
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
                if (isFrontStack(stack) && (r = stack.mResumedActivity) != null && r.state != ActivityStack.ActivityState.RESUMED) {
                    return false;
                }
            }
        }
        this.mLastFocusedStack = this.mFocusedStack;
        return true;
    }

    boolean allResumedActivitiesVisible() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ActivityRecord r = stack.mResumedActivity;
                if (r != null && (!r.nowVisible || r.waitingVisible)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean pauseBackStacks(boolean userLeaving, boolean resuming, boolean dontWait) {
        boolean someActivityPaused = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (!isFrontStack(stack) && stack.mResumedActivity != null) {
                    someActivityPaused |= stack.startPausingLocked(userLeaving, false, resuming, dontWait);
                }
            }
        }
        return someActivityPaused;
    }

    boolean allPausedActivitiesComplete() {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                ActivityRecord r = stack.mPausingActivity;
                if (r != null && r.state != ActivityStack.ActivityState.PAUSED && r.state != ActivityStack.ActivityState.STOPPED && r.state != ActivityStack.ActivityState.STOPPING) {
                    return false;
                }
            }
        }
        return true;
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
        if (changed) {
            this.mService.notifyAll();
        }
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
        if (changed) {
            this.mService.notifyAll();
        }
    }

    ActivityRecord topRunningActivityLocked() {
        ActivityRecord r;
        ActivityStack focusedStack = getFocusedStack();
        ActivityRecord r2 = focusedStack.topRunningActivityLocked(null);
        if (r2 != null) {
            return r2;
        }
        ArrayList<ActivityStack> stacks = this.mHomeStack.mStacks;
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = stacks.get(stackNdx);
            if (stack != focusedStack && isFrontStack(stack) && (r = stack.topRunningActivityLocked(null)) != null) {
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
            if (selectedStackList != null) {
                list.add(selectedStackList.remove(0));
                maxNum--;
            } else {
                return;
            }
        }
    }

    ActivityInfo resolveActivity(Intent intent, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId) {
        ActivityInfo aInfo;
        try {
            ResolveInfo rInfo = AppGlobals.getPackageManager().resolveIntent(intent, resolvedType, 66560, userId);
            aInfo = rInfo != null ? rInfo.activityInfo : null;
        } catch (RemoteException e) {
            aInfo = null;
        }
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            if ((startFlags & 2) != 0 && !aInfo.processName.equals("system")) {
                this.mService.setDebugApp(aInfo.processName, true, false);
            }
            if ((startFlags & 4) != 0 && !aInfo.processName.equals("system")) {
                this.mService.setOpenGlTraceApp(aInfo.applicationInfo, aInfo.processName);
            }
            if (profilerInfo != null && !aInfo.processName.equals("system")) {
                this.mService.setProfileApp(aInfo.applicationInfo, aInfo.processName, profilerInfo);
            }
        }
        return aInfo;
    }

    void startHomeActivity(Intent intent, ActivityInfo aInfo, String reason) {
        moveHomeStackTaskToTop(1, reason);
        startActivityLocked(null, intent, null, aInfo, null, null, null, null, 0, 0, 0, null, 0, 0, 0, null, false, null, null, null);
    }

    final int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, IActivityManager.WaitResult outResult, Configuration config, Bundle options, int userId, IActivityContainer iContainer, TaskRecord inTask) {
        int callingPid;
        ActivityStack stack;
        ActivityInfo aInfo;
        Intent intent2;
        int res;
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        boolean componentSpecified = intent.getComponent() != null;
        Intent intent3 = new Intent(intent);
        ActivityInfo aInfo2 = resolveActivity(intent3, resolvedType, startFlags, profilerInfo, userId);
        ActivityContainer container = (ActivityContainer) iContainer;
        synchronized (this.mService) {
            try {
                try {
                    int realCallingPid = Binder.getCallingPid();
                    int realCallingUid = Binder.getCallingUid();
                    if (callingUid >= 0) {
                        callingPid = -1;
                    } else if (caller == null) {
                        callingPid = realCallingPid;
                        callingUid = realCallingUid;
                    } else {
                        callingUid = -1;
                        callingPid = -1;
                    }
                    if (container == null || container.mStack.isOnHomeDisplay()) {
                        stack = getFocusedStack();
                    } else {
                        stack = container.mStack;
                    }
                    stack.mConfigWillChange = (config == null || this.mService.mConfiguration.diff(config) == 0) ? false : true;
                    long origId = Binder.clearCallingIdentity();
                    if (aInfo2 == null || (aInfo2.applicationInfo.flags & 268435456) == 0 || !aInfo2.processName.equals(aInfo2.applicationInfo.packageName) || this.mService.mHeavyWeightProcess == null || (this.mService.mHeavyWeightProcess.info.uid == aInfo2.applicationInfo.uid && this.mService.mHeavyWeightProcess.processName.equals(aInfo2.processName))) {
                        aInfo = aInfo2;
                        intent2 = intent3;
                    } else {
                        int appCallingUid = callingUid;
                        try {
                            if (caller != null) {
                                ProcessRecord callerApp = this.mService.getRecordForAppLocked(caller);
                                if (callerApp != null) {
                                    appCallingUid = callerApp.info.uid;
                                } else {
                                    Slog.w("ActivityManager", "Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting: " + intent3.toString());
                                    ActivityOptions.abort(options);
                                    res = -4;
                                    return res;
                                }
                            }
                            callingUid = Binder.getCallingUid();
                            callingPid = Binder.getCallingPid();
                            componentSpecified = true;
                            ResolveInfo rInfo = AppGlobals.getPackageManager().resolveIntent(intent2, (String) null, 66560, userId);
                            aInfo = this.mService.getActivityInfoForUser(rInfo != null ? rInfo.activityInfo : null, userId);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                        IIntentSender target = this.mService.getIntentSenderLocked(2, "android", appCallingUid, userId, null, null, 0, new Intent[]{intent3}, new String[]{resolvedType}, 1342177280, null);
                        Intent newIntent = new Intent();
                        if (requestCode >= 0) {
                            newIntent.putExtra("has_result", true);
                        }
                        newIntent.putExtra("intent", new IntentSender(target));
                        if (this.mService.mHeavyWeightProcess.activities.size() > 0) {
                            ActivityRecord hist = this.mService.mHeavyWeightProcess.activities.get(0);
                            newIntent.putExtra("cur_app", hist.packageName);
                            newIntent.putExtra("cur_task", hist.task.taskId);
                        }
                        newIntent.putExtra("new_app", aInfo2.packageName);
                        newIntent.setFlags(intent3.getFlags());
                        newIntent.setClassName("android", HeavyWeightSwitcherActivity.class.getName());
                        intent2 = newIntent;
                        resolvedType = null;
                        caller = null;
                    }
                    res = startActivityLocked(caller, intent2, resolvedType, aInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, componentSpecified, null, container, inTask);
                    Binder.restoreCallingIdentity(origId);
                    if (stack.mConfigWillChange) {
                        this.mService.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                        stack.mConfigWillChange = false;
                        this.mService.updateConfigurationLocked(config, null, false, false);
                    }
                    if (outResult != null) {
                        outResult.result = res;
                        if (res == 0) {
                            this.mWaitingActivityLaunched.add(outResult);
                            do {
                                try {
                                    this.mService.wait();
                                } catch (InterruptedException e) {
                                }
                                if (outResult.timeout) {
                                    break;
                                }
                            } while (outResult.who == null);
                        } else if (res == 2) {
                            ActivityRecord r = stack.topRunningActivityLocked(null);
                            if (r.nowVisible && r.state == ActivityStack.ActivityState.RESUMED) {
                                outResult.timeout = false;
                                outResult.who = new ComponentName(r.info.packageName, r.info.name);
                                outResult.totalTime = 0L;
                                outResult.thisTime = 0L;
                            } else {
                                outResult.thisTime = SystemClock.uptimeMillis();
                                this.mWaitingActivityVisible.add(outResult);
                                do {
                                    try {
                                        this.mService.wait();
                                    } catch (InterruptedException e2) {
                                    }
                                    if (outResult.timeout) {
                                        break;
                                    }
                                } while (outResult.who == null);
                            }
                        }
                    }
                    return res;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    final int startActivities(IApplicationThread caller, int callingUid, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle options, int userId) {
        int callingPid;
        Bundle theseOptions;
        if (intents == null) {
            throw new NullPointerException("intents is null");
        }
        if (resolvedTypes == null) {
            throw new NullPointerException("resolvedTypes is null");
        }
        if (intents.length != resolvedTypes.length) {
            throw new IllegalArgumentException("intents are length different than resolvedTypes");
        }
        if (callingUid >= 0) {
            callingPid = -1;
        } else if (caller == null) {
            callingPid = Binder.getCallingPid();
            callingUid = Binder.getCallingUid();
        } else {
            callingUid = -1;
            callingPid = -1;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mService) {
                ActivityRecord[] outActivity = new ActivityRecord[1];
                for (int i = 0; i < intents.length; i++) {
                    Intent intent = intents[i];
                    if (intent != null) {
                        if (intent != null && intent.hasFileDescriptors()) {
                            throw new IllegalArgumentException("File descriptors passed in Intent");
                        }
                        boolean componentSpecified = intent.getComponent() != null;
                        Intent intent2 = new Intent(intent);
                        ActivityInfo aInfo = this.mService.getActivityInfoForUser(resolveActivity(intent2, resolvedTypes[i], 0, null, userId), userId);
                        if (aInfo != null && (aInfo.applicationInfo.flags & 268435456) != 0) {
                            throw new IllegalArgumentException("FLAG_CANT_SAVE_STATE not supported here");
                        }
                        if (options != null && i == intents.length - 1) {
                            theseOptions = options;
                        } else {
                            theseOptions = null;
                        }
                        int res = startActivityLocked(caller, intent2, resolvedTypes[i], aInfo, null, null, resultTo, null, -1, callingPid, callingUid, callingPackage, callingPid, callingUid, 0, theseOptions, componentSpecified, outActivity, null, null);
                        if (res >= 0) {
                            resultTo = outActivity[0] != null ? outActivity[0].appToken : null;
                        } else {
                            return res;
                        }
                    }
                }
                Binder.restoreCallingIdentity(origId);
                return 0;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app, boolean andResume, boolean checkConfig) throws RemoteException {
        r.startFreezingScreenLocked(app, 0);
        this.mWindowManager.setAppVisibility(r.appToken, true);
        r.startLaunchTickingLocked();
        if (checkConfig) {
            Configuration config = this.mWindowManager.updateOrientationFromAppTokens(this.mService.mConfiguration, r.mayFreezeScreenLocked(app) ? r.appToken : null);
            this.mService.updateConfigurationLocked(config, r, false, false);
        }
        r.app = app;
        app.waitingToKill = null;
        r.launchCount++;
        r.lastLaunchTime = SystemClock.uptimeMillis();
        int idx = app.activities.indexOf(r);
        if (idx < 0) {
            app.activities.add(r);
        }
        this.mService.updateLruProcessLocked(app, true, null);
        this.mService.updateOomAdjLocked();
        ActivityStack stack = r.task.stack;
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
            if (andResume) {
                EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY, Integer.valueOf(r.userId), Integer.valueOf(System.identityHashCode(r)), Integer.valueOf(r.task.taskId), r.shortComponentName);
            }
            if (r.isHomeActivity() && r.isNotResolverActivity()) {
                this.mService.mHomeProcess = r.task.mActivities.get(0).app;
            }
            this.mService.ensurePackageDexOpt(r.intent.getComponent().getPackageName());
            r.sleeping = false;
            r.forceNewConfig = false;
            this.mService.showAskCompatModeDialogLocked(r);
            r.compat = this.mService.compatibilityInfoForPackageLocked(r.info.applicationInfo);
            String profileFile = null;
            ParcelFileDescriptor profileFd = null;
            if (this.mService.mProfileApp != null && this.mService.mProfileApp.equals(app.processName) && (this.mService.mProfileProc == null || this.mService.mProfileProc == app)) {
                this.mService.mProfileProc = app;
                profileFile = this.mService.mProfileFile;
                profileFd = this.mService.mProfileFd;
            }
            app.hasShownUi = true;
            app.pendingUiClean = true;
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
            ProfilerInfo profilerInfo = profileFile != null ? new ProfilerInfo(profileFile, profileFd, this.mService.mSamplingInterval, this.mService.mAutoStopProfiler) : null;
            app.forceProcessStateUpTo(2);
            app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken, System.identityHashCode(r), r.info, new Configuration(this.mService.mConfiguration), r.compat, r.launchedFromPackage, r.task.voiceInteractor, app.repProcState, r.icicle, r.persistentState, results, newIntents, !andResume, this.mService.isNextTransitionForward(), profilerInfo);
            if ((app.info.flags & 268435456) != 0 && app.processName.equals(app.info.packageName)) {
                if (this.mService.mHeavyWeightProcess != null && this.mService.mHeavyWeightProcess != app) {
                    Slog.w("ActivityManager", "Starting new heavy weight process " + app + " when already running " + this.mService.mHeavyWeightProcess);
                }
                this.mService.mHeavyWeightProcess = app;
                Message msg = this.mService.mHandler.obtainMessage(24);
                msg.obj = r;
                this.mService.mHandler.sendMessage(msg);
            }
            r.launchFailed = false;
            if (stack.updateLRUListLocked(r)) {
                Slog.w("ActivityManager", "Activity " + r + " being launched, but already in LRU list");
            }
            if (andResume) {
                stack.minimalResumeActivityLocked(r);
            } else {
                r.state = ActivityStack.ActivityState.STOPPED;
                r.stopped = true;
            }
            if (isFrontStack(stack)) {
                this.mService.startSetupActivityLocked();
            }
            this.mService.mServices.updateServiceConnectionActivitiesLocked(r.app);
            return true;
        } catch (RemoteException e3) {
            if (r.launchFailed) {
                Slog.e("ActivityManager", "Second failure launching " + r.intent.getComponent().flattenToShortString() + ", giving up", e3);
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
                Slog.w("ActivityManager", "Exception when starting activity " + r.intent.getComponent().flattenToShortString(), e);
            }
        }
        this.mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0, "activity", r.intent.getComponent(), false, false, true);
    }

    final int startActivityLocked(IApplicationThread caller, Intent intent, String resolvedType, ActivityInfo aInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, Bundle options, boolean componentSpecified, ActivityRecord[] outActivity, ActivityContainer container, TaskRecord inTask) {
        String msg;
        int i;
        int err = 0;
        ProcessRecord callerApp = null;
        if (caller != null) {
            callerApp = this.mService.getRecordForAppLocked(caller);
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                Slog.w("ActivityManager", "Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting: " + intent.toString());
                err = -4;
            }
        }
        if (err == 0) {
            int userId = aInfo != null ? UserHandle.getUserId(aInfo.applicationInfo.uid) : 0;
            StringBuilder sbAppend = new StringBuilder().append("START u").append(userId).append(" {").append(intent.toShortString(true, true, true, false)).append("} from uid ").append(callingUid).append(" on display ");
            if (container == null) {
                i = this.mFocusedStack == null ? 0 : this.mFocusedStack.mDisplayId;
            } else {
                i = container.mActivityDisplay == null ? 0 : container.mActivityDisplay.mDisplayId;
            }
            Slog.i("ActivityManager", sbAppend.append(i).toString());
        }
        ActivityRecord sourceRecord = null;
        ActivityRecord resultRecord = null;
        if (resultTo != null && (sourceRecord = isInAnyStackLocked(resultTo)) != null && requestCode >= 0 && !sourceRecord.finishing) {
            resultRecord = sourceRecord;
        }
        int launchFlags = intent.getFlags();
        if ((33554432 & launchFlags) != 0 && sourceRecord != null) {
            if (requestCode >= 0) {
                ActivityOptions.abort(options);
                return -3;
            }
            resultRecord = sourceRecord.resultTo;
            resultWho = sourceRecord.resultWho;
            requestCode = sourceRecord.requestCode;
            sourceRecord.resultTo = null;
            if (resultRecord != null) {
                resultRecord.removeResultsLocked(sourceRecord, resultWho, requestCode);
            }
            if (sourceRecord.launchedFromUid == callingUid) {
                callingPackage = sourceRecord.launchedFromPackage;
            }
        }
        if (err == 0 && intent.getComponent() == null) {
            err = -1;
        }
        if (err == 0 && aInfo == null) {
            err = -2;
        }
        if (err == 0 && sourceRecord != null && sourceRecord.task.voiceSession != null && (268435456 & launchFlags) == 0 && sourceRecord.info.applicationInfo.uid != aInfo.applicationInfo.uid) {
            try {
                if (!AppGlobals.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                    err = -7;
                }
            } catch (RemoteException e) {
                err = -7;
            }
        }
        if (err == 0 && voiceSession != null) {
            try {
                if (!AppGlobals.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                    err = -7;
                }
            } catch (RemoteException e2) {
                err = -7;
            }
        }
        ActivityStack resultStack = resultRecord == null ? null : resultRecord.task.stack;
        if (err != 0) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            ActivityOptions.abort(options);
            return err;
        }
        int startAnyPerm = this.mService.checkPermission("android.permission.START_ANY_ACTIVITY", callingPid, callingUid);
        int componentPerm = this.mService.checkComponentPermission(aInfo.permission, callingPid, callingUid, aInfo.applicationInfo.uid, aInfo.exported);
        if (startAnyPerm != 0 && componentPerm != 0) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            if (!aInfo.exported) {
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") not exported from uid " + aInfo.applicationInfo.uid;
            } else {
                msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires " + aInfo.permission;
            }
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        }
        boolean abort = !this.mService.mIntentFirewall.checkStartActivity(intent, callingUid, callingPid, resolvedType, aInfo.applicationInfo);
        if (this.mService.mController != null) {
            try {
                Intent watchIntent = intent.cloneFilter();
                abort |= !this.mService.mController.activityStarting(watchIntent, aInfo.applicationInfo.packageName);
            } catch (RemoteException e3) {
                this.mService.mController = null;
            }
        }
        if (abort) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            ActivityOptions.abort(options);
            return 0;
        }
        ActivityRecord r = new ActivityRecord(this.mService, callerApp, callingUid, callingPackage, intent, resolvedType, aInfo, this.mService.mConfiguration, resultRecord, resultWho, requestCode, componentSpecified, this, container, options);
        if (outActivity != null) {
            outActivity[0] = r;
        }
        ActivityStack stack = getFocusedStack();
        if (voiceSession == null && ((stack.mResumedActivity == null || stack.mResumedActivity.info.applicationInfo.uid != callingUid) && !this.mService.checkAppSwitchAllowedLocked(callingPid, callingUid, realCallingPid, realCallingUid, "Activity start"))) {
            PendingActivityLaunch pal = new PendingActivityLaunch(r, sourceRecord, startFlags, stack);
            this.mPendingActivityLaunches.add(pal);
            ActivityOptions.abort(options);
            return 4;
        }
        if (this.mService.mDidAppSwitch) {
            this.mService.mAppSwitchesAllowedTime = 0L;
        } else {
            this.mService.mDidAppSwitch = true;
        }
        doPendingActivityLaunchesLocked(false);
        int err2 = startActivityUncheckedLocked(r, sourceRecord, voiceSession, voiceInteractor, startFlags, true, options, inTask);
        if (err2 < 0) {
            notifyActivityDrawnForKeyguard();
        }
        return err2;
    }

    ActivityStack adjustStackFocus(ActivityRecord r, boolean newTask) {
        TaskRecord task = r.task;
        if (!this.mLeanbackOnlyDevice && (r.isApplicationActivity() || (task != null && task.isApplicationTask()))) {
            if (task != null) {
                ActivityStack taskStack = task.stack;
                if (taskStack.isOnHomeDisplay() && this.mFocusedStack != taskStack) {
                    this.mFocusedStack = taskStack;
                    return taskStack;
                }
                return taskStack;
            }
            ActivityContainer container = r.mInitialActivityContainer;
            if (container != null) {
                r.mInitialActivityContainer = null;
                ActivityStack taskStack2 = container.mStack;
                return taskStack2;
            }
            if (this.mFocusedStack != this.mHomeStack && (!newTask || this.mFocusedStack.mActivityContainer.isEligibleForNewTasks())) {
                ActivityStack taskStack3 = this.mFocusedStack;
                return taskStack3;
            }
            ArrayList<ActivityStack> homeDisplayStacks = this.mHomeStack.mStacks;
            for (int stackNdx = homeDisplayStacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = homeDisplayStacks.get(stackNdx);
                if (!stack.isHomeStack()) {
                    this.mFocusedStack = stack;
                    ActivityStack taskStack4 = this.mFocusedStack;
                    return taskStack4;
                }
            }
            int stackId = createStackOnDisplay(getNextStackId(), 0);
            this.mFocusedStack = getStack(stackId);
            ActivityStack taskStack5 = this.mFocusedStack;
            return taskStack5;
        }
        ActivityStack taskStack6 = this.mHomeStack;
        return taskStack6;
    }

    void setFocusedStack(ActivityRecord r, String reason) {
        if (r != null) {
            TaskRecord task = r.task;
            boolean isHomeActivity = !r.isApplicationActivity();
            if (!isHomeActivity && task != null) {
                isHomeActivity = !task.isApplicationTask();
            }
            if (!isHomeActivity && task != null) {
                ActivityRecord parent = task.stack.mActivityContainer.mParentActivity;
                isHomeActivity = parent != null && parent.isHomeActivity();
            }
            moveHomeStack(isHomeActivity, reason);
        }
    }

    final int startActivityUncheckedLocked(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, Bundle options, TaskRecord inTask) {
        ActivityStack sourceStack;
        ActivityStack targetStack;
        ActivityRecord top;
        Intent intent = r.intent;
        int callingUid = r.launchedFromUid;
        if (inTask != null && !inTask.inRecents) {
            Slog.w("ActivityManager", "Starting activity in task not in recents: " + inTask);
            inTask = null;
        }
        boolean launchSingleTop = r.launchMode == 1;
        boolean launchSingleInstance = r.launchMode == 3;
        boolean launchSingleTask = r.launchMode == 2;
        int launchFlags = intent.getFlags();
        if ((524288 & launchFlags) != 0 && (launchSingleInstance || launchSingleTask)) {
            Slog.i("ActivityManager", "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
            launchFlags &= -134742017;
        } else {
            switch (r.info.documentLaunchMode) {
                case 1:
                    launchFlags |= 524288;
                    break;
                case 2:
                    launchFlags |= 524288;
                    break;
                case 3:
                    launchFlags &= -134217729;
                    break;
            }
        }
        boolean launchTaskBehind = (!r.mLaunchTaskBehind || launchSingleTask || launchSingleInstance || (524288 & launchFlags) == 0) ? false : true;
        if (r.resultTo != null && (268435456 & launchFlags) != 0) {
            Slog.w("ActivityManager", "Activity is launching as a new task, so cancelling activity result.");
            r.resultTo.task.stack.sendActivityResultLocked(-1, r.resultTo, r.resultWho, r.requestCode, 0, null);
            r.resultTo = null;
        }
        if ((524288 & launchFlags) != 0 && r.resultTo == null) {
            launchFlags |= 268435456;
        }
        if ((268435456 & launchFlags) != 0 && (launchTaskBehind || r.info.documentLaunchMode == 2)) {
            launchFlags |= 134217728;
        }
        this.mUserLeaving = (262144 & launchFlags) == 0;
        if (!doResume) {
            r.delayedResume = true;
        }
        ActivityRecord notTop = (16777216 & launchFlags) != 0 ? r : null;
        if ((startFlags & 1) != 0) {
            ActivityRecord checkedCaller = sourceRecord;
            if (checkedCaller == null) {
                checkedCaller = getFocusedStack().topRunningNonDelayedActivityLocked(notTop);
            }
            if (!checkedCaller.realActivity.equals(r.realActivity)) {
                startFlags &= -2;
            }
        }
        boolean addingToTask = false;
        TaskRecord reuseTask = null;
        if (sourceRecord == null && inTask != null && inTask.stack != null) {
            Intent baseIntent = inTask.getBaseIntent();
            ActivityRecord root = inTask.getRootActivity();
            if (baseIntent == null) {
                ActivityOptions.abort(options);
                throw new IllegalArgumentException("Launching into task without base intent: " + inTask);
            }
            if (launchSingleInstance || launchSingleTask) {
                if (!baseIntent.getComponent().equals(r.intent.getComponent())) {
                    ActivityOptions.abort(options);
                    throw new IllegalArgumentException("Trying to launch singleInstance/Task " + r + " into different task " + inTask);
                }
                if (root != null) {
                    ActivityOptions.abort(options);
                    throw new IllegalArgumentException("Caller with inTask " + inTask + " has root " + root + " but target is singleInstance/Task");
                }
            }
            if (root == null) {
                launchFlags = ((-403185665) & launchFlags) | (baseIntent.getFlags() & 403185664);
                intent.setFlags(launchFlags);
                inTask.setIntent(r);
                addingToTask = true;
            } else if ((268435456 & launchFlags) != 0) {
                addingToTask = false;
            } else {
                addingToTask = true;
            }
            reuseTask = inTask;
        } else {
            inTask = null;
        }
        if (inTask == null) {
            if (sourceRecord == null) {
                if ((268435456 & launchFlags) == 0 && inTask == null) {
                    Slog.w("ActivityManager", "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + intent);
                    launchFlags |= 268435456;
                }
            } else if (sourceRecord.launchMode == 3 || launchSingleInstance || launchSingleTask) {
                launchFlags |= 268435456;
            }
        }
        ActivityInfo newTaskInfo = null;
        Intent newTaskIntent = null;
        if (sourceRecord != null) {
            if (sourceRecord.finishing) {
                if ((268435456 & launchFlags) == 0) {
                    Slog.w("ActivityManager", "startActivity called from finishing " + sourceRecord + "; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + intent);
                    launchFlags |= 268435456;
                    newTaskInfo = sourceRecord.info;
                    newTaskIntent = sourceRecord.task.intent;
                }
                sourceRecord = null;
                sourceStack = null;
            } else {
                sourceStack = sourceRecord.task.stack;
            }
        } else {
            sourceStack = null;
        }
        boolean movedHome = false;
        intent.setFlags(launchFlags);
        if ((((268435456 & launchFlags) != 0 && (134217728 & launchFlags) == 0) || launchSingleInstance || launchSingleTask) && inTask == null && r.resultTo == null) {
            ActivityRecord intentActivity = !launchSingleInstance ? findTaskLocked(r) : findActivityLocked(intent, r.info);
            if (intentActivity != null) {
                if (isLockTaskModeViolation(intentActivity.task)) {
                    showLockTaskToast();
                    Slog.e("ActivityManager", "startActivityUnchecked: Attempt to violate Lock Task Mode");
                    return 5;
                }
                if (r.task == null) {
                    r.task = intentActivity.task;
                }
                ActivityStack targetStack2 = intentActivity.task.stack;
                targetStack2.mLastPausedActivity = null;
                targetStack2.moveToFront("intentActivityFound");
                if (intentActivity.task.intent == null) {
                    intentActivity.task.setIntent(r);
                }
                ActivityStack lastStack = getLastStack();
                ActivityRecord curTop = lastStack == null ? null : lastStack.topRunningNonDelayedActivityLocked(notTop);
                boolean movedToFront = false;
                if (curTop != null && (curTop.task != intentActivity.task || curTop.task != lastStack.topTask())) {
                    r.intent.addFlags(4194304);
                    if (sourceRecord == null || (sourceStack.topActivity() != null && sourceStack.topActivity().task == sourceRecord.task)) {
                        if (launchTaskBehind && sourceRecord != null) {
                            intentActivity.setTaskToAffiliateWith(sourceRecord.task);
                        }
                        movedHome = true;
                        targetStack2.moveTaskToFrontLocked(intentActivity.task, r, options, "bringingFoundTaskToFront");
                        if ((268451840 & launchFlags) == 268451840) {
                            intentActivity.task.setTaskToReturnTo(1);
                        }
                        options = null;
                        movedToFront = true;
                    }
                }
                if ((2097152 & launchFlags) != 0) {
                    intentActivity = targetStack2.resetTaskIfNeededLocked(intentActivity, r);
                }
                if ((startFlags & 1) != 0) {
                    if (doResume) {
                        resumeTopActivitiesLocked(targetStack2, null, options);
                        if (!movedToFront) {
                            notifyActivityDrawnForKeyguard();
                        }
                    } else {
                        ActivityOptions.abort(options);
                    }
                    return 1;
                }
                if ((268468224 & launchFlags) == 268468224) {
                    reuseTask = intentActivity.task;
                    reuseTask.performClearTaskLocked();
                    reuseTask.setIntent(r);
                } else if ((67108864 & launchFlags) != 0 || launchSingleInstance || launchSingleTask) {
                    ActivityRecord top2 = intentActivity.task.performClearTaskLocked(r, launchFlags);
                    if (top2 != null) {
                        if (top2.frontOfTask) {
                            top2.task.setIntent(r);
                        }
                        ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, top2.task);
                        top2.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                    } else {
                        addingToTask = true;
                        sourceRecord = intentActivity;
                    }
                } else if (r.realActivity.equals(intentActivity.task.realActivity)) {
                    if (((536870912 & launchFlags) != 0 || launchSingleTop) && intentActivity.realActivity.equals(r.realActivity)) {
                        ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, intentActivity.task);
                        if (intentActivity.frontOfTask) {
                            intentActivity.task.setIntent(r);
                        }
                        intentActivity.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                    } else if (!r.intent.filterEquals(intentActivity.task.intent)) {
                        addingToTask = true;
                        sourceRecord = intentActivity;
                    }
                } else if ((2097152 & launchFlags) == 0) {
                    addingToTask = true;
                    sourceRecord = intentActivity;
                } else if (!intentActivity.task.rootWasReset) {
                    intentActivity.task.setIntent(r);
                }
                if (!addingToTask && reuseTask == null) {
                    if (doResume) {
                        targetStack2.resumeTopActivityLocked(null, options);
                        if (!movedToFront) {
                            notifyActivityDrawnForKeyguard();
                        }
                    } else {
                        ActivityOptions.abort(options);
                    }
                    return 2;
                }
            }
        }
        if (r.packageName != null) {
            ActivityStack topStack = getFocusedStack();
            ActivityRecord top3 = topStack.topRunningNonDelayedActivityLocked(notTop);
            if (top3 != null && r.resultTo == null && top3.realActivity.equals(r.realActivity) && top3.userId == r.userId && top3.app != null && top3.app.thread != null && ((536870912 & launchFlags) != 0 || launchSingleTop || launchSingleTask)) {
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, top3, top3.task);
                topStack.mLastPausedActivity = null;
                if (doResume) {
                    resumeTopActivitiesLocked();
                }
                ActivityOptions.abort(options);
                if ((startFlags & 1) != 0) {
                    return 1;
                }
                top3.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                return 3;
            }
            boolean newTask = false;
            boolean keepCurTransition = false;
            TaskRecord taskToAffiliate = (!launchTaskBehind || sourceRecord == null) ? null : sourceRecord.task;
            if (r.resultTo == null && inTask == null && !addingToTask && (268435456 & launchFlags) != 0) {
                if (isLockTaskModeViolation(reuseTask)) {
                    Slog.e("ActivityManager", "Attempted Lock Task Mode violation r=" + r);
                    return 5;
                }
                newTask = true;
                targetStack = adjustStackFocus(r, true);
                if (!launchTaskBehind) {
                    targetStack.moveToFront("startingNewTask");
                }
                if (reuseTask == null) {
                    r.setTask(targetStack.createTaskRecord(getNextTaskId(), newTaskInfo != null ? newTaskInfo : r.info, newTaskIntent != null ? newTaskIntent : intent, voiceSession, voiceInteractor, !launchTaskBehind), taskToAffiliate);
                } else {
                    r.setTask(reuseTask, taskToAffiliate);
                }
                if (!movedHome && (268451840 & launchFlags) == 268451840) {
                    r.task.setTaskToReturnTo(1);
                }
            } else if (sourceRecord != null) {
                TaskRecord sourceTask = sourceRecord.task;
                if (isLockTaskModeViolation(sourceTask)) {
                    Slog.e("ActivityManager", "Attempted Lock Task Mode violation r=" + r);
                    return 5;
                }
                targetStack = sourceTask.stack;
                targetStack.moveToFront("sourceStackToFront");
                TaskRecord topTask = targetStack.topTask();
                if (topTask != sourceTask) {
                    targetStack.moveTaskToFrontLocked(sourceTask, r, options, "sourceTaskToFront");
                }
                if (!addingToTask && (67108864 & launchFlags) != 0) {
                    ActivityRecord top4 = sourceTask.performClearTaskLocked(r, launchFlags);
                    keepCurTransition = true;
                    if (top4 != null) {
                        ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, top4.task);
                        top4.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                        targetStack.mLastPausedActivity = null;
                        if (doResume) {
                            targetStack.resumeTopActivityLocked(null);
                        }
                        ActivityOptions.abort(options);
                        return 3;
                    }
                } else if (!addingToTask && (131072 & launchFlags) != 0 && (top = sourceTask.findActivityInHistoryLocked(r)) != null) {
                    TaskRecord task = top.task;
                    task.moveActivityToFrontLocked(top);
                    ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, task);
                    top.updateOptionsLocked(options);
                    top.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                    targetStack.mLastPausedActivity = null;
                    if (doResume) {
                        targetStack.resumeTopActivityLocked(null);
                    }
                    return 3;
                }
                r.setTask(sourceTask, null);
            } else if (inTask != null) {
                if (isLockTaskModeViolation(inTask)) {
                    Slog.e("ActivityManager", "Attempted Lock Task Mode violation r=" + r);
                    return 5;
                }
                targetStack = inTask.stack;
                targetStack.moveTaskToFrontLocked(inTask, r, options, "inTaskToFront");
                ActivityRecord top5 = inTask.getTopActivity();
                if (top5 != null && top5.realActivity.equals(r.realActivity) && top5.userId == r.userId && ((536870912 & launchFlags) != 0 || launchSingleTop || launchSingleTask)) {
                    ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, top5, top5.task);
                    if ((startFlags & 1) != 0) {
                        return 1;
                    }
                    top5.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                    return 3;
                }
                if (!addingToTask) {
                    ActivityOptions.abort(options);
                    return 2;
                }
                r.setTask(inTask, null);
            } else {
                targetStack = adjustStackFocus(r, false);
                targetStack.moveToFront("addingToTopTask");
                ActivityRecord prev = targetStack.topActivity();
                r.setTask(prev != null ? prev.task : targetStack.createTaskRecord(getNextTaskId(), r.info, intent, null, null, true), null);
                this.mWindowManager.moveTaskToTop(r.task.taskId);
            }
            this.mService.grantUriPermissionFromIntentLocked(callingUid, r.packageName, intent, r.getUriPermissionsLocked(), r.userId);
            if (sourceRecord != null && sourceRecord.isRecentsActivity()) {
                r.task.setTaskToReturnTo(2);
            }
            if (newTask) {
                EventLog.writeEvent(EventLogTags.AM_CREATE_TASK, Integer.valueOf(r.userId), Integer.valueOf(r.task.taskId));
            }
            ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, r, r.task);
            targetStack.mLastPausedActivity = null;
            targetStack.startActivityLocked(r, newTask, doResume, keepCurTransition, options);
            if (!launchTaskBehind) {
                this.mService.setFocusedActivityLocked(r, "startedActivity");
            }
            return 0;
        }
        if (r.resultTo != null) {
            r.resultTo.task.stack.sendActivityResultLocked(-1, r.resultTo, r.resultWho, r.requestCode, 0, null);
        }
        ActivityOptions.abort(options);
        return -2;
    }

    final void doPendingActivityLaunchesLocked(boolean doResume) {
        while (!this.mPendingActivityLaunches.isEmpty()) {
            PendingActivityLaunch pal = this.mPendingActivityLaunches.remove(0);
            startActivityUncheckedLocked(pal.r, pal.sourceRecord, null, null, pal.startFlags, doResume && this.mPendingActivityLaunches.isEmpty(), null, null);
        }
    }

    void removePendingActivityLaunchesLocked(ActivityStack stack) {
        for (int palNdx = this.mPendingActivityLaunches.size() - 1; palNdx >= 0; palNdx--) {
            PendingActivityLaunch pal = this.mPendingActivityLaunches.get(palNdx);
            if (pal.stack == stack) {
                this.mPendingActivityLaunches.remove(palNdx);
            }
        }
    }

    void acquireLaunchWakelock() {
        this.mLaunchingActivity.acquire();
        if (!this.mHandler.hasMessages(104)) {
            this.mHandler.sendEmptyMessageDelayed(104, 10000L);
        }
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
        ArrayList<ActivityRecord> finishes = null;
        ArrayList<UserStartedState> startingUsers = null;
        boolean booting = false;
        boolean activityRemoved = false;
        ActivityRecord r = ActivityRecord.forToken(token);
        if (r != null) {
            this.mHandler.removeMessages(100, r);
            r.finishLaunchTickingLocked();
            if (fromTimeout) {
                reportActivityLaunchedLocked(fromTimeout, r, -1L, -1L);
            }
            if (config != null) {
                r.configuration = config;
            }
            r.idle = true;
            if (isFrontStack(r.task.stack) || fromTimeout) {
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
            ensureActivitiesVisibleLocked(null, 0);
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
            if (r.finishing) {
                stack.finishCurrentActivityLocked(r, 0, false);
            } else {
                stack.stopActivityLocked(r);
            }
        }
        for (int i2 = 0; i2 < NF; i2++) {
            r = finishes.get(i2);
            activityRemoved |= r.task.stack.destroyActivityLocked(r, true, "finish-idle");
        }
        if (!booting) {
            if (startingUsers != null) {
                for (int i3 = 0; i3 < startingUsers.size(); i3++) {
                    this.mService.finishUserSwitch(startingUsers.get(i3));
                }
            }
            if (this.mStartingBackgroundUsers.size() > 0) {
                ArrayList<UserStartedState> startingUsers2 = new ArrayList<>(this.mStartingBackgroundUsers);
                this.mStartingBackgroundUsers.clear();
                for (int i4 = 0; i4 < startingUsers2.size(); i4++) {
                    this.mService.finishUserBoot(startingUsers2.get(i4));
                }
            }
        }
        this.mService.trimApplications();
        if (activityRemoved) {
            resumeTopActivitiesLocked();
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

    boolean forceStopPackageLocked(String name, boolean doit, boolean evenPersistent, int userId) {
        boolean didSomething = false;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                if (stack.forceStopPackageLocked(name, doit, evenPersistent, userId)) {
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
                    if (!isFrontStack(stack)) {
                        stackNdx--;
                    } else if (stack.mResumedActivity != null) {
                        fgApp = stack.mResumedActivity.app;
                    } else if (stack.mPausingActivity != null) {
                        fgApp = stack.mPausingActivity.app;
                    }
                }
            }
        }
        if (r.app != null && fgApp != null && r.app != fgApp && r.lastVisibleTime > this.mService.mPreviousProcessVisibleTime && r.app != this.mService.mHomeProcess) {
            this.mService.mPreviousProcess = r.app;
            this.mService.mPreviousProcessVisibleTime = r.lastVisibleTime;
        }
    }

    boolean resumeTopActivitiesLocked() {
        return resumeTopActivitiesLocked(null, null, null);
    }

    boolean resumeTopActivitiesLocked(ActivityStack targetStack, ActivityRecord target, Bundle targetOptions) {
        if (targetStack == null) {
            targetStack = getFocusedStack();
        }
        boolean result = false;
        if (isFrontStack(targetStack)) {
            result = targetStack.resumeTopActivityLocked(target, targetOptions);
        }
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if (stack != targetStack && isFrontStack(stack)) {
                    stack.resumeTopActivityLocked(null);
                }
            }
        }
        return result;
    }

    void finishTopRunningActivityLocked(ProcessRecord app) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.finishTopRunningActivityLocked(app);
            }
        }
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

    void findTaskToMoveToFrontLocked(TaskRecord task, int flags, Bundle options, String reason) {
        if ((flags & 2) == 0) {
            this.mUserLeaving = true;
        }
        if ((flags & 1) != 0) {
            task.setTaskToReturnTo(1);
        }
        task.stack.moveTaskToFrontLocked(task, null, options, reason);
    }

    ActivityStack getStack(int stackId) {
        ActivityContainer activityContainer = this.mActivityContainers.get(stackId);
        if (activityContainer != null) {
            return activityContainer.mStack;
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
        ArrayList<TaskRecord> tasks = this.mHomeStack.getAllTasks();
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = tasks.get(taskNdx);
            if (task.isHomeTask()) {
                ArrayList<ActivityRecord> activities = task.mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = activities.get(activityNdx);
                    if (r.isHomeActivity()) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    ActivityContainer createActivityContainer(ActivityRecord parentActivity, IActivityContainerCallback callback) {
        ActivityContainer activityContainer = new VirtualActivityContainer(parentActivity, callback);
        this.mActivityContainers.put(activityContainer.mStackId, activityContainer);
        parentActivity.mChildContainers.add(activityContainer);
        return activityContainer;
    }

    void removeChildActivityContainers(ActivityRecord parentActivity) {
        ArrayList<ActivityContainer> childStacks = parentActivity.mChildContainers;
        for (int containerNdx = childStacks.size() - 1; containerNdx >= 0; containerNdx--) {
            ActivityContainer container = childStacks.remove(containerNdx);
            container.release();
        }
    }

    void deleteActivityContainer(IActivityContainer container) {
        ActivityContainer activityContainer = (ActivityContainer) container;
        if (activityContainer != null) {
            int stackId = activityContainer.mStackId;
            this.mActivityContainers.remove(stackId);
            this.mWindowManager.removeStack(stackId);
        }
    }

    private int createStackOnDisplay(int stackId, int displayId) {
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
        if (activityDisplay == null) {
            return -1;
        }
        ActivityContainer activityContainer = new ActivityContainer(stackId);
        this.mActivityContainers.put(stackId, activityContainer);
        activityContainer.attachToDisplayLocked(activityDisplay);
        return stackId;
    }

    int getNextStackId() {
        do {
            int i = this.mLastStackId + 1;
            this.mLastStackId = i;
            if (i <= 0) {
                this.mLastStackId = 1;
            }
        } while (getStack(this.mLastStackId) != null);
        return this.mLastStackId;
    }

    private boolean restoreRecentTaskLocked(TaskRecord task) {
        ActivityStack stack = null;
        if (this.mLeanbackOnlyDevice) {
            stack = this.mHomeStack;
        } else {
            ArrayList<ActivityStack> homeDisplayStacks = this.mHomeStack.mStacks;
            int stackNdx = homeDisplayStacks.size() - 1;
            while (true) {
                if (stackNdx < 0) {
                    break;
                }
                ActivityStack tmpStack = homeDisplayStacks.get(stackNdx);
                if (!tmpStack.isHomeStack()) {
                    stack = tmpStack;
                    break;
                }
                stackNdx--;
            }
        }
        if (stack == null) {
            stack = getStack(createStackOnDisplay(getNextStackId(), 0));
            moveHomeStack(true, "restoreRecentTask");
        }
        if (stack == null) {
            return false;
        }
        stack.addTask(task, false, false);
        ArrayList<ActivityRecord> activities = task.mActivities;
        for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord r = activities.get(activityNdx);
            this.mWindowManager.addAppToken(0, r.appToken, task.taskId, stack.mStackId, r.info.screenOrientation, r.fullscreen, (r.info.flags & 1024) != 0, r.userId, r.info.configChanges, task.voiceSession != null, r.mLaunchTaskBehind);
        }
        return true;
    }

    void moveTaskToStackLocked(int taskId, int stackId, boolean toTop) {
        TaskRecord task = anyTaskForIdLocked(taskId);
        if (task != null) {
            ActivityStack stack = getStack(stackId);
            if (stack == null) {
                Slog.w("ActivityManager", "moveTaskToStack: no stack for id=" + stackId);
                return;
            }
            task.stack.removeTask(task, "moveTaskToStack");
            stack.addTask(task, toTop, true);
            this.mWindowManager.addTask(taskId, stackId, toTop);
            resumeTopActivitiesLocked();
        }
    }

    ActivityRecord findTaskLocked(ActivityRecord r) {
        ActivityRecord ar;
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                if ((r.isApplicationActivity() || stack.isHomeStack()) && stack.mActivityContainer.isEligibleForNewTasks() && (ar = stack.findTaskLocked(r)) != null) {
                    return ar;
                }
            }
        }
        return null;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo info) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                ActivityRecord ar = stacks.get(stackNdx).findActivityLocked(intent, info);
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
                Slog.w("ActivityManager", "Activity manager shutdown timed out");
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
                if (isFrontStack(stack)) {
                    resumeTopActivitiesLocked();
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
        if (this.mService.isSleepingOrShuttingDown()) {
            if (!this.mSleepTimeout) {
                boolean dontSleep = false;
                for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
                    ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
                    for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                        dontSleep |= stacks.get(stackNdx).checkReadyForSleepLocked();
                    }
                }
                if (this.mStoppingActivities.size() > 0) {
                    scheduleIdleLocked();
                    dontSleep = true;
                }
                if (this.mGoingToSleepActivities.size() > 0) {
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
            if (this.mService.mShuttingDown) {
                this.mService.notifyAll();
            }
        }
    }

    boolean reportResumedActivityLocked(ActivityRecord r) {
        ActivityStack stack = r.task.stack;
        if (isFrontStack(stack)) {
            this.mService.updateUsageStats(r, true);
        }
        if (!allResumedActivitiesComplete()) {
            return false;
        }
        ensureActivitiesVisibleLocked(null, 0);
        this.mWindowManager.executeAppTransition();
        return true;
    }

    void handleAppCrashLocked(ProcessRecord app) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int numStacks = stacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.handleAppCrashLocked(app);
            }
        }
    }

    boolean requestVisibleBehindLocked(ActivityRecord r, boolean visible) {
        ActivityRecord next;
        ActivityStack stack = r.task.stack;
        if (stack == null) {
            return false;
        }
        boolean isVisible = stack.hasVisibleBehindActivity();
        ActivityRecord top = topRunningActivityLocked();
        if (top == null || top == r || visible == isVisible) {
            if (!visible) {
                r = null;
            }
            stack.setVisibleBehindActivity(r);
            return true;
        }
        if (visible && top.fullscreen) {
            return false;
        }
        if (!visible && stack.getVisibleBehindActivity() != r) {
            return false;
        }
        stack.setVisibleBehindActivity(visible ? r : null);
        if (!visible && (next = stack.findNextTranslucentActivity(r)) != null) {
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
        r.mLaunchTaskBehind = false;
        TaskRecord task = r.task;
        task.setLastThumbnail(task.stack.screenshotActivities(r));
        this.mService.addRecentTaskLocked(task);
        this.mService.notifyTaskStackChangedLocked();
        this.mWindowManager.setAppVisibility(r.appToken, false);
    }

    void scheduleLaunchTaskBehindComplete(IBinder token) {
        this.mHandler.obtainMessage(113, token).sendToTarget();
    }

    void ensureActivitiesVisibleLocked(ActivityRecord starting, int configChanges) {
        for (int displayNdx = this.mActivityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            int topStackNdx = stacks.size() - 1;
            for (int stackNdx = topStackNdx; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = stacks.get(stackNdx);
                stack.ensureActivitiesVisibleLocked(starting, configChanges);
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
        for (int i = 0; i < app.activities.size(); i++) {
            ActivityRecord r = app.activities.get(i);
            if (!r.finishing && r.state != ActivityStack.ActivityState.DESTROYING && r.state != ActivityStack.ActivityState.DESTROYED) {
                if (!r.visible && r.stopped && r.haveState && r.state != ActivityStack.ActivityState.RESUMED && r.state != ActivityStack.ActivityState.PAUSING && r.state != ActivityStack.ActivityState.PAUSED && r.state != ActivityStack.ActivityState.STOPPING && r.task != null) {
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
            } else {
                return;
            }
        }
        if (tasks != null) {
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
    }

    boolean switchUserLocked(int userId, UserStartedState uss) {
        this.mUserStackInFront.put(this.mCurrentUser, getFocusedStack().getStackId());
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
            moveHomeStack(homeInFront, "switchUserOnHomeDisplay");
            TaskRecord task2 = stack2.topTask();
            if (task2 != null) {
                this.mWindowManager.moveTaskToTop(task2.taskId);
            }
        } else {
            resumeHomeStackTask(1, null, "switchUserOnOtherDisplay");
        }
        return homeInFront;
    }

    public void startBackgroundUserLocked(int userId, UserStartedState uss) {
        this.mStartingBackgroundUsers.add(uss);
    }

    final ArrayList<ActivityRecord> processStoppingActivitiesLocked(boolean remove) {
        int N = this.mStoppingActivities.size();
        if (N <= 0) {
            return null;
        }
        ArrayList<ActivityRecord> stops = null;
        boolean nowVisible = allResumedActivitiesVisible();
        int i = 0;
        while (i < N) {
            ActivityRecord s = this.mStoppingActivities.get(i);
            if (s.waitingVisible && nowVisible) {
                this.mWaitingVisibleActivities.remove(s);
                s.waitingVisible = false;
                if (s.finishing) {
                    this.mWindowManager.setAppVisibility(s.appToken, false);
                }
            }
            if ((!s.waitingVisible || this.mService.isSleepingOrShuttingDown()) && remove) {
                if (stops == null) {
                    stops = new ArrayList<>();
                }
                stops.add(s);
                this.mStoppingActivities.remove(i);
                N--;
                i--;
            }
            i++;
        }
        return stops;
    }

    void validateTopActivitiesLocked() {
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mFocusedStack=" + this.mFocusedStack);
        pw.print(" mLastFocusedStack=");
        pw.println(this.mLastFocusedStack);
        pw.print(prefix);
        pw.println("mSleepTimeout=" + this.mSleepTimeout);
        pw.print(prefix);
        pw.println("mCurTaskId=" + this.mCurTaskId);
        pw.print(prefix);
        pw.println("mUserStackInFront=" + this.mUserStackInFront);
        pw.print(prefix);
        pw.println("mActivityContainers=" + this.mActivityContainers);
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String name) {
        return getFocusedStack().getDumpActivitiesLocked(name);
    }

    static boolean printThisActivity(PrintWriter pw, ActivityRecord activity, String dumpPackage, boolean needSep, String prefix) {
        if (activity == null || !(dumpPackage == null || dumpPackage.equals(activity.packageName))) {
            return false;
        }
        if (needSep) {
            pw.println();
        }
        pw.print(prefix);
        pw.println(activity);
        return true;
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
                printed = printed | stack.dumpActivitiesLocked(fd, pw, dumpAll, dumpClient, dumpPackage, needSep, stackHeader.toString()) | dumpHistoryList(fd, pw, stack.mLRUActivities, "    ", "Run", false, !dumpAll, false, dumpPackage, true, "    Running activities (most recent first):", null);
                boolean needSep2 = printed;
                boolean pr = printThisActivity(pw, stack.mPausingActivity, dumpPackage, needSep2, "    mPausingActivity: ");
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
        Message msg = this.mHandler.obtainMessage(100, next);
        this.mHandler.sendMessageDelayed(msg, 10000L);
    }

    final void scheduleIdleLocked() {
        this.mHandler.sendEmptyMessage(101);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord r) {
        this.mHandler.removeMessages(100, r);
    }

    final void scheduleResumeTopActivities() {
        if (!this.mHandler.hasMessages(102)) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    void removeSleepTimeouts() {
        this.mSleepTimeout = false;
        this.mHandler.removeMessages(103);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(103, 5000L);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        Slog.v("ActivityManager", "Display added displayId=" + displayId);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(105, displayId, 0));
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        Slog.v("ActivityManager", "Display removed displayId=" + displayId);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(107, displayId, 0));
    }

    @Override
    public void onDisplayChanged(int displayId) {
        Slog.v("ActivityManager", "Display changed displayId=" + displayId);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(106, displayId, 0));
    }

    public void handleDisplayAddedLocked(int displayId) {
        synchronized (this.mService) {
            boolean newDisplay = this.mActivityDisplays.get(displayId) == null;
            if (newDisplay) {
                ActivityDisplay activityDisplay = new ActivityDisplay(displayId);
                if (activityDisplay.mDisplay == null) {
                    Slog.w("ActivityManager", "Display " + displayId + " gone before initialization complete");
                    return;
                }
                this.mActivityDisplays.put(displayId, activityDisplay);
            }
            if (newDisplay) {
                this.mWindowManager.onDisplayAdded(displayId);
            }
        }
    }

    public void handleDisplayRemovedLocked(int displayId) {
        synchronized (this.mService) {
            ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
            if (activityDisplay != null) {
                ArrayList<ActivityStack> stacks = activityDisplay.mStacks;
                for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
                    stacks.get(stackNdx).mActivityContainer.detachLocked();
                }
                this.mActivityDisplays.remove(displayId);
            }
        }
        this.mWindowManager.onDisplayRemoved(displayId);
    }

    public void handleDisplayChangedLocked(int displayId) {
        synchronized (this.mService) {
            ActivityDisplay activityDisplay = this.mActivityDisplays.get(displayId);
            if (activityDisplay != null) {
            }
        }
        this.mWindowManager.onDisplayChanged(displayId);
    }

    ActivityManager.StackInfo getStackInfo(ActivityStack stack) {
        String strFlattenToString;
        ActivityManager.StackInfo info = new ActivityManager.StackInfo();
        this.mWindowManager.getStackBounds(stack.mStackId, info.bounds);
        info.displayId = 0;
        info.stackId = stack.mStackId;
        ArrayList<TaskRecord> tasks = stack.getAllTasks();
        int numTasks = tasks.size();
        int[] taskIds = new int[numTasks];
        String[] taskNames = new String[numTasks];
        for (int i = 0; i < numTasks; i++) {
            TaskRecord task = tasks.get(i);
            taskIds[i] = task.taskId;
            if (task.origActivity != null) {
                strFlattenToString = task.origActivity.flattenToString();
            } else {
                strFlattenToString = task.realActivity != null ? task.realActivity.flattenToString() : task.getTopActivity() != null ? task.getTopActivity().packageName : "unknown";
            }
            taskNames[i] = strFlattenToString;
        }
        info.taskIds = taskIds;
        info.taskNames = taskNames;
        return info;
    }

    ActivityManager.StackInfo getStackInfoLocked(int stackId) {
        ActivityStack stack = getStack(stackId);
        if (stack != null) {
            return getStackInfo(stack);
        }
        return null;
    }

    ArrayList<ActivityManager.StackInfo> getAllStackInfosLocked() {
        ArrayList<ActivityManager.StackInfo> list = new ArrayList<>();
        for (int displayNdx = 0; displayNdx < this.mActivityDisplays.size(); displayNdx++) {
            ArrayList<ActivityStack> stacks = this.mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int ndx = stacks.size() - 1; ndx >= 0; ndx--) {
                list.add(getStackInfo(stacks.get(ndx)));
            }
        }
        return list;
    }

    void showLockTaskToast() {
        this.mLockTaskNotify.showToast(this.mLockTaskIsLocked);
    }

    void setLockTaskModeLocked(TaskRecord task, boolean isLocked, String reason) {
        if (task == null) {
            if (this.mLockTaskModeTask != null) {
                Message lockTaskMsg = Message.obtain();
                lockTaskMsg.arg1 = this.mLockTaskModeTask.userId;
                lockTaskMsg.what = 110;
                this.mLockTaskModeTask = null;
                this.mHandler.sendMessage(lockTaskMsg);
                return;
            }
            return;
        }
        if (isLockTaskModeViolation(task)) {
            Slog.e("ActivityManager", "setLockTaskMode: Attempt to start a second Lock Task Mode task.");
            return;
        }
        this.mLockTaskModeTask = task;
        findTaskToMoveToFrontLocked(task, 0, null, reason);
        resumeTopActivitiesLocked();
        Message lockTaskMsg2 = Message.obtain();
        lockTaskMsg2.obj = this.mLockTaskModeTask.intent.getComponent().getPackageName();
        lockTaskMsg2.arg1 = this.mLockTaskModeTask.userId;
        lockTaskMsg2.what = 109;
        lockTaskMsg2.arg2 = isLocked ? 0 : 1;
        this.mHandler.sendMessage(lockTaskMsg2);
    }

    boolean isLockTaskModeViolation(TaskRecord task) {
        return (this.mLockTaskModeTask == null || this.mLockTaskModeTask == task) ? false : true;
    }

    void endLockTaskModeIfTaskEnding(TaskRecord task) {
        if (this.mLockTaskModeTask != null && this.mLockTaskModeTask == task) {
            Message lockTaskMsg = Message.obtain();
            lockTaskMsg.arg1 = this.mLockTaskModeTask.userId;
            lockTaskMsg.what = 110;
            this.mLockTaskModeTask = null;
            this.mHandler.sendMessage(lockTaskMsg);
        }
    }

    boolean isInLockTaskMode() {
        return this.mLockTaskModeTask != null;
    }

    private final class ActivityStackSupervisorHandler extends Handler {
        public ActivityStackSupervisorHandler(Looper looper) {
            super(looper);
        }

        void activityIdleInternal(ActivityRecord r) {
            synchronized (ActivityStackSupervisor.this.mService) {
                ActivityStackSupervisor.this.activityIdleInternalLocked(r != null ? r.appToken : null, true, null);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            boolean shouldLockKeyguard;
            switch (msg.what) {
                case 100:
                    if (ActivityStackSupervisor.this.mService.mDidDexOpt) {
                        ActivityStackSupervisor.this.mService.mDidDexOpt = false;
                        Message nmsg = ActivityStackSupervisor.this.mHandler.obtainMessage(100);
                        nmsg.obj = msg.obj;
                        ActivityStackSupervisor.this.mHandler.sendMessageDelayed(nmsg, 10000L);
                        return;
                    }
                    activityIdleInternal((ActivityRecord) msg.obj);
                    return;
                case 101:
                    activityIdleInternal((ActivityRecord) msg.obj);
                    return;
                case 102:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        ActivityStackSupervisor.this.resumeTopActivitiesLocked();
                        break;
                    }
                    return;
                case 103:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        if (ActivityStackSupervisor.this.mService.isSleepingOrShuttingDown()) {
                            Slog.w("ActivityManager", "Sleep timeout!  Sleeping now.");
                            ActivityStackSupervisor.this.mSleepTimeout = true;
                            ActivityStackSupervisor.this.checkReadyForSleepLocked();
                        }
                        break;
                    }
                    return;
                case 104:
                    if (ActivityStackSupervisor.this.mService.mDidDexOpt) {
                        ActivityStackSupervisor.this.mService.mDidDexOpt = false;
                        ActivityStackSupervisor.this.mHandler.sendEmptyMessageDelayed(104, 10000L);
                        return;
                    }
                    synchronized (ActivityStackSupervisor.this.mService) {
                        if (ActivityStackSupervisor.this.mLaunchingActivity.isHeld()) {
                            Slog.w("ActivityManager", "Launch timeout has expired, giving up wake lock!");
                            ActivityStackSupervisor.this.mLaunchingActivity.release();
                        }
                        break;
                    }
                    return;
                case 105:
                    ActivityStackSupervisor.this.handleDisplayAddedLocked(msg.arg1);
                    return;
                case 106:
                    ActivityStackSupervisor.this.handleDisplayChangedLocked(msg.arg1);
                    return;
                case 107:
                    ActivityStackSupervisor.this.handleDisplayRemovedLocked(msg.arg1);
                    return;
                case 108:
                    ActivityContainer container = (ActivityContainer) msg.obj;
                    IActivityContainerCallback callback = container.mCallback;
                    if (callback != null) {
                        try {
                            IBinder iBinderAsBinder = container.asBinder();
                            shouldLockKeyguard = msg.arg1 == 1;
                            callback.setVisible(iBinderAsBinder, shouldLockKeyguard);
                            return;
                        } catch (RemoteException e) {
                            return;
                        }
                    }
                    return;
                case 109:
                    try {
                        if (ActivityStackSupervisor.this.mLockTaskNotify == null) {
                            ActivityStackSupervisor.this.mLockTaskNotify = new LockTaskNotify(ActivityStackSupervisor.this.mService.mContext);
                        }
                        ActivityStackSupervisor.this.mLockTaskNotify.show(true);
                        ActivityStackSupervisor activityStackSupervisor = ActivityStackSupervisor.this;
                        shouldLockKeyguard = msg.arg2 == 0;
                        activityStackSupervisor.mLockTaskIsLocked = shouldLockKeyguard;
                        if (ActivityStackSupervisor.this.getStatusBarService() != null) {
                            int flags = ActivityStackSupervisor.this.mLockTaskIsLocked ? 62849024 : 62849024 ^ 18874368;
                            ActivityStackSupervisor.this.getStatusBarService().disable(flags, ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.this.mService.mContext.getPackageName());
                        }
                        ActivityStackSupervisor.this.mWindowManager.disableKeyguard(ActivityStackSupervisor.this.mToken, ActivityStackSupervisor.LOCK_TASK_TAG);
                        if (ActivityStackSupervisor.this.getDevicePolicyManager() != null) {
                            ActivityStackSupervisor.this.getDevicePolicyManager().notifyLockTaskModeChanged(true, (String) msg.obj, msg.arg1);
                            return;
                        }
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
                        try {
                            shouldLockKeyguard = Settings.Secure.getInt(ActivityStackSupervisor.this.mService.mContext.getContentResolver(), "lock_to_app_exit_locked") != 0;
                            if (!ActivityStackSupervisor.this.mLockTaskIsLocked && shouldLockKeyguard) {
                                ActivityStackSupervisor.this.mWindowManager.lockNow(null);
                                ActivityStackSupervisor.this.mWindowManager.dismissKeyguard();
                                new LockPatternUtils(ActivityStackSupervisor.this.mService.mContext).requireCredentialEntry(-1);
                                return;
                            }
                            return;
                        } catch (Settings.SettingNotFoundException e2) {
                            return;
                        }
                    } catch (RemoteException ex2) {
                        throw new RuntimeException(ex2);
                    }
                case 111:
                    ActivityContainer container2 = (ActivityContainer) msg.obj;
                    IActivityContainerCallback callback2 = container2.mCallback;
                    if (callback2 != null) {
                        try {
                            callback2.onAllActivitiesComplete(container2.asBinder());
                            return;
                        } catch (RemoteException e3) {
                            return;
                        }
                    }
                    return;
                case 112:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        Slog.w("ActivityManager", "Timeout waiting for all activities in task to finish. " + msg.obj);
                        ActivityContainer container3 = (ActivityContainer) msg.obj;
                        container3.mStack.finishAllActivitiesLocked(true);
                        container3.onTaskListEmptyLocked();
                        break;
                    }
                    return;
                case 113:
                    synchronized (ActivityStackSupervisor.this.mService) {
                        ActivityRecord r = ActivityRecord.forToken((IBinder) msg.obj);
                        if (r != null) {
                            ActivityStackSupervisor.this.handleLaunchTaskBehindCompleteLocked(r);
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
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
                this.mStackId = stackId;
                this.mStack = new ActivityStack(this);
                this.mIdString = "ActivtyContainer{" + this.mStackId + "}";
            }
        }

        void attachToDisplayLocked(ActivityDisplay activityDisplay) {
            this.mActivityDisplay = activityDisplay;
            this.mStack.mDisplayId = activityDisplay.mDisplayId;
            this.mStack.mStacks = activityDisplay.mStacks;
            activityDisplay.attachActivities(this.mStack);
            ActivityStackSupervisor.this.mWindowManager.attachStack(this.mStackId, activityDisplay.mDisplayId);
        }

        public void attachToDisplay(int displayId) {
            synchronized (ActivityStackSupervisor.this.mService) {
                ActivityDisplay activityDisplay = (ActivityDisplay) ActivityStackSupervisor.this.mActivityDisplays.get(displayId);
                if (activityDisplay != null) {
                    attachToDisplayLocked(activityDisplay);
                }
            }
        }

        public int getDisplayId() {
            synchronized (ActivityStackSupervisor.this.mService) {
                if (this.mActivityDisplay != null) {
                    return this.mActivityDisplay.mDisplayId;
                }
                return -1;
            }
        }

        public boolean injectEvent(InputEvent event) {
            boolean zInjectInputEvent = false;
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (ActivityStackSupervisor.this.mService) {
                    if (this.mActivityDisplay != null) {
                        zInjectInputEvent = ActivityStackSupervisor.this.mInputManagerInternal.injectInputEvent(event, this.mActivityDisplay.mDisplayId, 0);
                    }
                }
                return zInjectInputEvent;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void release() {
            synchronized (ActivityStackSupervisor.this.mService) {
                if (this.mContainerState != 2) {
                    this.mContainerState = 2;
                    Message msg = ActivityStackSupervisor.this.mHandler.obtainMessage(112, this);
                    ActivityStackSupervisor.this.mHandler.sendMessageDelayed(msg, 2000L);
                    long origId = Binder.clearCallingIdentity();
                    try {
                        this.mStack.finishAllActivitiesLocked(false);
                        ActivityStackSupervisor.this.removePendingActivityLaunchesLocked(this.mStack);
                    } finally {
                        Binder.restoreCallingIdentity(origId);
                    }
                }
            }
        }

        protected void detachLocked() {
            if (this.mActivityDisplay != null) {
                this.mActivityDisplay.detachActivitiesLocked(this.mStack);
                this.mActivityDisplay = null;
                this.mStack.mDisplayId = -1;
                this.mStack.mStacks = null;
                ActivityStackSupervisor.this.mWindowManager.detachStack(this.mStackId);
            }
        }

        public final int startActivity(Intent intent) {
            ActivityStackSupervisor.this.mService.enforceNotIsolatedCaller("ActivityContainer.startActivity");
            int userId = ActivityStackSupervisor.this.mService.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), ActivityStackSupervisor.this.mCurrentUser, false, 2, "ActivityContainer", (String) null);
            intent.addFlags(FORCE_NEW_TASK_FLAGS);
            String mimeType = intent.getType();
            if (mimeType == null && intent.getData() != null && "content".equals(intent.getData().getScheme())) {
                mimeType = ActivityStackSupervisor.this.mService.getProviderMimeType(intent.getData(), userId);
            }
            return ActivityStackSupervisor.this.startActivityMayWait(null, -1, null, intent, mimeType, null, null, null, null, 0, 0, null, null, null, null, userId, this, null);
        }

        public final int startActivityIntentSender(IIntentSender intentSender) {
            ActivityStackSupervisor.this.mService.enforceNotIsolatedCaller("ActivityContainer.startActivityIntentSender");
            if (!(intentSender instanceof PendingIntentRecord)) {
                throw new IllegalArgumentException("Bad PendingIntent object");
            }
            return ((PendingIntentRecord) intentSender).sendInner(0, null, null, null, null, null, null, 0, FORCE_NEW_TASK_FLAGS, FORCE_NEW_TASK_FLAGS, null, this);
        }

        private void checkEmbeddedAllowedInner(Intent intent, String resolvedType) {
            int userId = ActivityStackSupervisor.this.mService.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), ActivityStackSupervisor.this.mCurrentUser, false, 2, "ActivityContainer", (String) null);
            if (resolvedType == null && (resolvedType = intent.getType()) == null && intent.getData() != null && "content".equals(intent.getData().getScheme())) {
                resolvedType = ActivityStackSupervisor.this.mService.getProviderMimeType(intent.getData(), userId);
            }
            ActivityInfo aInfo = ActivityStackSupervisor.this.resolveActivity(intent, resolvedType, 0, null, userId);
            if (aInfo != null && (aInfo.flags & SoundTriggerHelper.STATUS_ERROR) == 0) {
                throw new SecurityException("Attempt to embed activity that has not set allowEmbedded=\"true\"");
            }
        }

        public final void checkEmbeddedAllowed(Intent intent) {
            checkEmbeddedAllowedInner(intent, null);
        }

        public final void checkEmbeddedAllowedIntentSender(IIntentSender intentSender) {
            if (!(intentSender instanceof PendingIntentRecord)) {
                throw new IllegalArgumentException("Bad PendingIntent object");
            }
            PendingIntentRecord pendingIntent = (PendingIntentRecord) intentSender;
            checkEmbeddedAllowedInner(pendingIntent.key.requestIntent, pendingIntent.key.requestResolvedType);
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

        void getBounds(Point outBounds) {
            synchronized (ActivityStackSupervisor.this.mService) {
                if (this.mActivityDisplay != null) {
                    this.mActivityDisplay.getBounds(outBounds);
                } else {
                    outBounds.set(0, 0);
                }
            }
        }

        void setVisible(boolean visible) {
            if (this.mVisible != visible) {
                this.mVisible = visible;
                if (this.mCallback != null) {
                    ActivityStackSupervisor.this.mHandler.obtainMessage(108, visible ? 1 : 0, 0, this).sendToTarget();
                }
            }
        }

        void setDrawn() {
        }

        boolean isEligibleForNewTasks() {
            return true;
        }

        void onTaskListEmptyLocked() {
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
                long origId = Binder.clearCallingIdentity();
                try {
                    setSurfaceLocked(surface, width, height, density);
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
        }

        private void setSurfaceLocked(Surface surface, int width, int height, int density) {
            if (this.mContainerState != 2) {
                if (((VirtualActivityDisplay) this.mActivityDisplay) == null) {
                    VirtualActivityDisplay virtualActivityDisplay = ActivityStackSupervisor.this.new VirtualActivityDisplay(width, height, density);
                    this.mActivityDisplay = virtualActivityDisplay;
                    ActivityStackSupervisor.this.mActivityDisplays.put(virtualActivityDisplay.mDisplayId, virtualActivityDisplay);
                    attachToDisplayLocked(virtualActivityDisplay);
                }
                if (this.mSurface != null) {
                    this.mSurface.release();
                }
                this.mSurface = surface;
                if (surface != null) {
                    this.mStack.resumeTopActivityLocked(null);
                } else {
                    this.mContainerState = 1;
                    ((VirtualActivityDisplay) this.mActivityDisplay).setSurface(null);
                    if (this.mStack.mPausingActivity == null && this.mStack.mResumedActivity != null) {
                        this.mStack.startPausingLocked(false, true, false, false);
                    }
                }
                setSurfaceIfReadyLocked();
            }
        }

        @Override
        boolean isAttachedLocked() {
            return this.mSurface != null && super.isAttachedLocked();
        }

        @Override
        void setDrawn() {
            synchronized (ActivityStackSupervisor.this.mService) {
                this.mDrawn = true;
                setSurfaceIfReadyLocked();
            }
        }

        @Override
        boolean isEligibleForNewTasks() {
            return false;
        }

        @Override
        void onTaskListEmptyLocked() {
            ActivityStackSupervisor.this.mHandler.removeMessages(112, this);
            detachLocked();
            ActivityStackSupervisor.this.deleteActivityContainer(this);
            ActivityStackSupervisor.this.mHandler.obtainMessage(111, this).sendToTarget();
        }

        private void setSurfaceIfReadyLocked() {
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
            if (display != null) {
                init(display);
            }
        }

        void init(Display display) {
            this.mDisplay = display;
            this.mDisplayId = display.getDisplayId();
            this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        }

        void attachActivities(ActivityStack stack) {
            this.mStacks.add(stack);
        }

        void detachActivitiesLocked(ActivityStack stack) {
            this.mStacks.remove(stack);
        }

        void getBounds(Point bounds) {
            this.mDisplay.getDisplayInfo(this.mDisplayInfo);
            bounds.x = this.mDisplayInfo.appWidth;
            bounds.y = this.mDisplayInfo.appHeight;
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
            if (this.mVirtualDisplay != null) {
                this.mVirtualDisplay.setSurface(surface);
            }
        }

        @Override
        void detachActivitiesLocked(ActivityStack stack) {
            super.detachActivitiesLocked(stack);
            if (this.mVirtualDisplay != null) {
                this.mVirtualDisplay.release();
                this.mVirtualDisplay = null;
            }
        }

        @Override
        public String toString() {
            return "VirtualActivityDisplay={" + this.mDisplayId + "}";
        }
    }

    private boolean isLeanbackOnlyDevice() {
        try {
            boolean onLeanbackOnly = AppGlobals.getPackageManager().hasSystemFeature("android.software.leanback_only");
            return onLeanbackOnly;
        } catch (RemoteException e) {
            return false;
        }
    }
}
