package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.ProfilerInfo;
import android.app.ResultInfo;
import android.app.WaitResult;
import android.app.WindowConfiguration;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.LaunchActivityItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserManager;
import android.os.WorkSource;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IntArray;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.IApplicationToken;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.TransferPipe;
import com.android.internal.os.logging.MetricsLoggerWrapper;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityStack;
import com.android.server.am.RecentTasks;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ActivityStackSupervisor extends ConfigurationContainer implements DisplayManager.DisplayListener, RecentTasks.Callbacks {
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    static final boolean CREATE_IF_NEEDED = true;
    static final boolean DEFER_RESUME = true;
    static final int HANDLE_DISPLAY_ADDED = 105;
    static final int HANDLE_DISPLAY_CHANGED = 106;
    static final int HANDLE_DISPLAY_REMOVED = 107;
    static final int IDLE_NOW_MSG = 101;
    static final int IDLE_TIMEOUT = 10000;
    static final int IDLE_TIMEOUT_MSG = 100;
    static final int LAUNCH_TASK_BEHIND_COMPLETE = 112;
    static final int LAUNCH_TIMEOUT = 10000;
    static final int LAUNCH_TIMEOUT_MSG = 104;
    static final int MATCH_TASK_IN_STACKS_ONLY = 0;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS = 1;
    static final int MATCH_TASK_IN_STACKS_OR_RECENT_TASKS_AND_RESTORE = 2;
    private static final int MAX_TASK_IDS_PER_USER = 100000;
    static final boolean ON_TOP = true;
    static final boolean PAUSE_IMMEDIATELY = true;
    static final boolean PRESERVE_WINDOWS = true;
    static final boolean REMOVE_FROM_RECENTS = true;
    static final int REPORT_MULTI_WINDOW_MODE_CHANGED_MSG = 114;
    static final int REPORT_PIP_MODE_CHANGED_MSG = 115;
    static final int RESUME_TOP_ACTIVITY_MSG = 102;
    static final int SLEEP_TIMEOUT = 5000;
    static final int SLEEP_TIMEOUT_MSG = 103;
    static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private static final String VIRTUAL_DISPLAY_BASE_NAME = "ActivityViewVirtualDisplay";
    boolean inResumeTopActivity;
    private ActivityMetricsLogger mActivityMetricsLogger;
    boolean mAppVisibilitiesChangedSinceLastPause;
    int mCurrentUser;
    private int mDeferResumeCount;
    DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    private boolean mDockedStackResizing;
    ActivityStack mFocusedStack;
    PowerManager.WakeLock mGoingToSleep;
    final ActivityStackSupervisorHandler mHandler;
    private boolean mHasPendingDockedBounds;
    ActivityStack mHomeStack;
    private boolean mInitialized;
    boolean mIsDockMinimized;
    private KeyguardController mKeyguardController;
    private ActivityStack mLastFocusedStack;
    private LaunchParamsController mLaunchParamsController;
    PowerManager.WakeLock mLaunchingActivity;
    final Looper mLooper;
    private Rect mPendingDockedBounds;
    private Rect mPendingTempDockedTaskBounds;
    private Rect mPendingTempDockedTaskInsetBounds;
    private Rect mPendingTempOtherTaskBounds;
    private Rect mPendingTempOtherTaskInsetBounds;
    Rect mPipModeChangedTargetStackBounds;
    private boolean mPowerHintSent;
    private PowerManager mPowerManager;
    RecentTasks mRecentTasks;
    private RunningTasks mRunningTasks;
    final ActivityManagerService mService;
    WindowManagerService mWindowManager;
    private static final String TAG = "ActivityManager";
    private static final String TAG_FOCUS = TAG + ActivityManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_IDLE = TAG + ActivityManagerDebugConfig.POSTFIX_IDLE;
    private static final String TAG_PAUSE = TAG + ActivityManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_RELEASE = TAG + ActivityManagerDebugConfig.POSTFIX_RELEASE;
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION = new ArrayMap<>();
    private final SparseIntArray mCurTaskIdForUser = new SparseIntArray(20);
    final ArrayList<ActivityRecord> mActivitiesWaitingForVisibleActivity = new ArrayList<>();
    private final ArrayList<WaitInfo> mWaitingForActivityVisible = new ArrayList<>();
    final ArrayList<WaitResult> mWaitingActivityLaunched = new ArrayList<>();
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mGoingToSleepActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mMultiWindowModeChangedActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mPipModeChangedActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mNoAnimActivities = new ArrayList<>();
    final ArrayList<UserState> mStartingUsers = new ArrayList<>();
    boolean mUserLeaving = false;
    final ArrayList<ActivityManagerInternal.SleepToken> mSleepTokens = new ArrayList<>();
    SparseIntArray mUserStackInFront = new SparseIntArray(2);
    private final SparseArray<ActivityDisplay> mActivityDisplays = new SparseArray<>();
    private final SparseArray<IntArray> mDisplayAccessUIDs = new SparseArray<>();
    private final Rect tempRect = new Rect();
    private final ActivityOptions mTmpOptions = ActivityOptions.makeBasic();
    int mDefaultMinSizeOfResizeableTask = -1;
    private boolean mTaskLayersChanged = true;
    private LaunchTimeTracker mLaunchTimeTracker = new LaunchTimeTracker();
    private final ArrayList<ActivityRecord> mTmpActivityList = new ArrayList<>();
    private final FindTaskResult mTmpFindTaskResult = new FindTaskResult();
    private SparseIntArray mTmpOrderedDisplayIds = new SparseIntArray();
    private final ArraySet<Integer> mResizingTasksDuringAnimation = new ArraySet<>();
    private boolean mAllowDockedStackResize = true;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AnyTaskForIdMatchTaskMode {
    }

    static {
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.IMAGE_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.media.action.VIDEO_CAPTURE", "android.permission.CAMERA");
        ACTION_TO_RUNTIME_PERMISSION.put("android.intent.action.CALL", "android.permission.CALL_PHONE");
    }

    @Override
    protected int getChildCount() {
        return this.mActivityDisplays.size();
    }

    @Override
    protected ActivityDisplay getChildAt(int i) {
        return this.mActivityDisplays.valueAt(i);
    }

    @Override
    protected ConfigurationContainer getParent() {
        return null;
    }

    Configuration getDisplayOverrideConfiguration(int i) {
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i);
        if (activityDisplayOrCreateLocked == null) {
            throw new IllegalArgumentException("No display found with id: " + i);
        }
        return activityDisplayOrCreateLocked.getOverrideConfiguration();
    }

    void setDisplayOverrideConfiguration(Configuration configuration, int i) {
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i);
        if (activityDisplayOrCreateLocked == null) {
            throw new IllegalArgumentException("No display found with id: " + i);
        }
        activityDisplayOrCreateLocked.onOverrideConfigurationChanged(configuration);
    }

    boolean canPlaceEntityOnDisplay(int i, boolean z, int i2, int i3, ActivityInfo activityInfo) {
        if (i == 0) {
            return true;
        }
        if (this.mService.mSupportsMultiDisplay) {
            return (z || displayConfigMatchesGlobal(i)) && isCallerAllowedToLaunchOnDisplay(i2, i3, i, activityInfo);
        }
        return false;
    }

    private boolean displayConfigMatchesGlobal(int i) {
        if (i == 0) {
            return true;
        }
        if (i == -1) {
            return false;
        }
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i);
        if (activityDisplayOrCreateLocked == null) {
            throw new IllegalArgumentException("No display found with id: " + i);
        }
        return getConfiguration().equals(activityDisplayOrCreateLocked.getConfiguration());
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

        PendingActivityLaunch(ActivityRecord activityRecord, ActivityRecord activityRecord2, int i, ActivityStack activityStack, ProcessRecord processRecord) {
            this.r = activityRecord;
            this.sourceRecord = activityRecord2;
            this.startFlags = i;
            this.stack = activityStack;
            this.callerApp = processRecord;
        }

        void sendErrorResult(String str) {
            try {
                if (this.callerApp.thread != null) {
                    this.callerApp.thread.scheduleCrash(str);
                }
            } catch (RemoteException e) {
                Slog.e(ActivityStackSupervisor.TAG, "Exception scheduling crash of failed activity launcher sourceRecord=" + this.sourceRecord, e);
            }
        }
    }

    public ActivityStackSupervisor(ActivityManagerService activityManagerService, Looper looper) {
        this.mService = activityManagerService;
        this.mLooper = looper;
        this.mHandler = new ActivityStackSupervisorHandler(looper);
    }

    public void initialize() {
        if (this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        this.mRunningTasks = createRunningTasks();
        this.mActivityMetricsLogger = new ActivityMetricsLogger(this, this.mService.mContext, this.mHandler.getLooper());
        this.mKeyguardController = new KeyguardController(this.mService, this);
        this.mLaunchParamsController = new LaunchParamsController(this.mService);
        this.mLaunchParamsController.registerDefaultModifiers(this);
    }

    public ActivityMetricsLogger getActivityMetricsLogger() {
        return this.mActivityMetricsLogger;
    }

    LaunchTimeTracker getLaunchTimeTracker() {
        return this.mLaunchTimeTracker;
    }

    public KeyguardController getKeyguardController() {
        return this.mKeyguardController;
    }

    void setRecentTasks(RecentTasks recentTasks) {
        this.mRecentTasks = recentTasks;
        this.mRecentTasks.registerCallback(this);
    }

    @VisibleForTesting
    RunningTasks createRunningTasks() {
        return new RunningTasks();
    }

    void initPowerManagement() {
        this.mPowerManager = (PowerManager) this.mService.mContext.getSystemService("power");
        this.mGoingToSleep = this.mPowerManager.newWakeLock(1, "ActivityManager-Sleep");
        this.mLaunchingActivity = this.mPowerManager.newWakeLock(1, "*launch*");
        this.mLaunchingActivity.setReferenceCounted(false);
    }

    void setWindowManager(WindowManagerService windowManagerService) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mWindowManager = windowManagerService;
                getKeyguardController().setWindowManager(windowManagerService);
                this.mDisplayManager = (DisplayManager) this.mService.mContext.getSystemService("display");
                this.mDisplayManager.registerDisplayListener(this, null);
                this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
                Display[] displays = this.mDisplayManager.getDisplays();
                for (int length = displays.length - 1; length >= 0; length--) {
                    Display display = displays[length];
                    ActivityDisplay activityDisplay = new ActivityDisplay(this, display);
                    this.mActivityDisplays.put(display.getDisplayId(), activityDisplay);
                    calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay);
                }
                ActivityStack orCreateStack = getDefaultDisplay().getOrCreateStack(1, 2, true);
                this.mLastFocusedStack = orCreateStack;
                this.mFocusedStack = orCreateStack;
                this.mHomeStack = orCreateStack;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    ActivityStack getFocusedStack() {
        return this.mFocusedStack;
    }

    boolean isFocusable(ConfigurationContainer configurationContainer, boolean z) {
        if (configurationContainer.inSplitScreenPrimaryWindowingMode() && this.mIsDockMinimized) {
            return false;
        }
        return configurationContainer.getWindowConfiguration().canReceiveKeys() || z;
    }

    ActivityStack getLastStack() {
        return this.mLastFocusedStack;
    }

    boolean isFocusedStack(ActivityStack activityStack) {
        return activityStack != null && activityStack == this.mFocusedStack;
    }

    void setFocusStackUnchecked(String str, ActivityStack activityStack) {
        int stackId;
        if (!activityStack.isFocusable()) {
            activityStack = getNextFocusableStackLocked(activityStack, false);
        }
        if (activityStack != this.mFocusedStack) {
            this.mLastFocusedStack = this.mFocusedStack;
            this.mFocusedStack = activityStack;
            int i = this.mCurrentUser;
            if (this.mFocusedStack != null) {
                stackId = this.mFocusedStack.getStackId();
            } else {
                stackId = -1;
            }
            EventLogTags.writeAmFocusedStack(i, stackId, this.mLastFocusedStack != null ? this.mLastFocusedStack.getStackId() : -1, str);
        }
        ActivityRecord activityRecord = topRunningActivityLocked();
        if ((this.mService.mBooting || !this.mService.mBooted) && activityRecord != null && activityRecord.idle) {
            checkFinishBootingLocked();
        }
    }

    void moveHomeStackToFront(String str) {
        this.mHomeStack.moveToFront(str);
    }

    void moveRecentsStackToFront(String str) {
        ActivityStack stack = getDefaultDisplay().getStack(0, 3);
        if (stack != null) {
            stack.moveToFront(str);
        }
    }

    boolean moveHomeStackTaskToTop(String str) {
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord homeActivity = getHomeActivity();
        if (homeActivity == null) {
            return false;
        }
        moveFocusableActivityStackToFrontLocked(homeActivity, str);
        return true;
    }

    boolean resumeHomeStackTask(ActivityRecord activityRecord, String str) {
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        this.mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord homeActivity = getHomeActivity();
        String str2 = str + " resumeHomeStackTask";
        if (homeActivity != null && !homeActivity.finishing) {
            moveFocusableActivityStackToFrontLocked(homeActivity, str2);
            return resumeFocusedStackTopActivityLocked(this.mHomeStack, activityRecord, null);
        }
        return this.mService.startHomeActivityLocked(this.mCurrentUser, str2);
    }

    TaskRecord anyTaskForIdLocked(int i) {
        return anyTaskForIdLocked(i, 2);
    }

    TaskRecord anyTaskForIdLocked(int i, int i2) {
        return anyTaskForIdLocked(i, i2, null, false);
    }

    TaskRecord anyTaskForIdLocked(int i, int i2, ActivityOptions activityOptions, boolean z) {
        ActivityStack launchStack;
        if (i2 != 2 && activityOptions != null) {
            throw new IllegalArgumentException("Should not specify activity options for non-restore lookup");
        }
        int size = this.mActivityDisplays.size();
        for (int i3 = 0; i3 < size; i3++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i3);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                TaskRecord taskRecordTaskForIdLocked = childAt.taskForIdLocked(i);
                if (taskRecordTaskForIdLocked != null) {
                    if (activityOptions != null && (launchStack = getLaunchStack(null, activityOptions, taskRecordTaskForIdLocked, z)) != null && childAt != launchStack) {
                        taskRecordTaskForIdLocked.reparent(launchStack, z, z ? 0 : 2, true, true, "anyTaskForIdLocked");
                    }
                    return taskRecordTaskForIdLocked;
                }
            }
        }
        if (i2 == 0) {
            return null;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Looking for task id=" + i + " in recents");
        }
        TaskRecord task = this.mRecentTasks.getTask(i);
        if (task == null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "\tDidn't find task id=" + i + " in recents");
            }
            return null;
        }
        if (i2 == 1) {
            return task;
        }
        if (!restoreRecentTaskLocked(task, activityOptions, z)) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.w(TAG_RECENTS, "Couldn't restore task id=" + i + " found in recents");
            }
            return null;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.w(TAG_RECENTS, "Restored task id=" + i + " from in recents");
        }
        return task;
    }

    ActivityRecord isInAnyStackLocked(IBinder iBinder) {
        int size = this.mActivityDisplays.size();
        for (int i = 0; i < size; i++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityRecord activityRecordIsInStackLocked = activityDisplayValueAt.getChildAt(childCount).isInStackLocked(iBinder);
                if (activityRecordIsInStackLocked != null) {
                    return activityRecordIsInStackLocked;
                }
            }
        }
        return null;
    }

    private boolean taskTopActivityIsUser(TaskRecord taskRecord, int i) {
        ActivityRecord topActivity = taskRecord.getTopActivity();
        ActivityRecord activityRecord = topActivity != null ? topActivity.resultTo : null;
        return (topActivity != null && topActivity.userId == i) || (activityRecord != null && activityRecord.userId == i);
    }

    void lockAllProfileTasks(int i) {
        this.mWindowManager.deferSurfaceLayout();
        try {
            for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
                ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
                for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                    ArrayList<TaskRecord> allTasks = activityDisplayValueAt.getChildAt(childCount).getAllTasks();
                    for (int size2 = allTasks.size() - 1; size2 >= 0; size2--) {
                        TaskRecord taskRecord = allTasks.get(size2);
                        if (taskTopActivityIsUser(taskRecord, i)) {
                            this.mService.mTaskChangeNotificationController.notifyTaskProfileLocked(taskRecord.taskId, i);
                        }
                    }
                }
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void setNextTaskIdForUserLocked(int i, int i2) {
        if (i > this.mCurTaskIdForUser.get(i2, -1)) {
            this.mCurTaskIdForUser.put(i2, i);
        }
    }

    static int nextTaskIdForUser(int i, int i2) {
        int i3 = i + 1;
        if (i3 == (i2 + 1) * MAX_TASK_IDS_PER_USER) {
            return i3 - MAX_TASK_IDS_PER_USER;
        }
        return i3;
    }

    int getNextTaskIdForUserLocked(int i) {
        int i2 = this.mCurTaskIdForUser.get(i, MAX_TASK_IDS_PER_USER * i);
        int iNextTaskIdForUser = nextTaskIdForUser(i2, i);
        do {
            if (this.mRecentTasks.containsTaskId(iNextTaskIdForUser, i) || anyTaskForIdLocked(iNextTaskIdForUser, 1) != null) {
                iNextTaskIdForUser = nextTaskIdForUser(iNextTaskIdForUser, i);
            } else {
                this.mCurTaskIdForUser.put(i, iNextTaskIdForUser);
                return iNextTaskIdForUser;
            }
        } while (iNextTaskIdForUser != i2);
        throw new IllegalStateException("Cannot get an available task id. Reached limit of 100000 running tasks per user.");
    }

    ActivityRecord getResumedActivityLocked() {
        ActivityStack activityStack = this.mFocusedStack;
        if (activityStack == null) {
            return null;
        }
        ActivityRecord resumedActivity = activityStack.getResumedActivity();
        if (resumedActivity == null || resumedActivity.app == null) {
            ActivityRecord activityRecord = activityStack.mPausingActivity;
            if (activityRecord == null || activityRecord.app == null) {
                return activityStack.topRunningActivityLocked();
            }
            return activityRecord;
        }
        return resumedActivity;
    }

    boolean attachApplicationLocked(ProcessRecord processRecord) throws RemoteException {
        String str = processRecord.processName;
        boolean z = false;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (isFocusedStack(childAt)) {
                    childAt.getAllRunningVisibleActivitiesLocked(this.mTmpActivityList);
                    ActivityRecord activityRecord = childAt.topRunningActivityLocked();
                    int size2 = this.mTmpActivityList.size();
                    boolean z2 = z;
                    for (int i = 0; i < size2; i++) {
                        ActivityRecord activityRecord2 = this.mTmpActivityList.get(i);
                        if (activityRecord2.app == null && processRecord.uid == activityRecord2.info.applicationInfo.uid && str.equals(activityRecord2.processName)) {
                            try {
                                if (realStartActivityLocked(activityRecord2, processRecord, activityRecord == activityRecord2, true)) {
                                    z2 = true;
                                }
                            } catch (RemoteException e) {
                                Slog.w(TAG, "Exception in new application when starting activity " + activityRecord.intent.getComponent().flattenToShortString(), e);
                                throw e;
                            }
                        }
                    }
                    z = z2;
                }
            }
        }
        if (!z) {
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        return z;
    }

    boolean allResumedActivitiesIdle() {
        ActivityRecord resumedActivity;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (isFocusedStack(childAt) && childAt.numActivities() != 0 && ((resumedActivity = childAt.getResumedActivity()) == null || !resumedActivity.idle)) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "allResumedActivitiesIdle: stack=" + childAt.mStackId + " " + resumedActivity + " not idle");
                        return false;
                    }
                    return false;
                }
            }
        }
        sendPowerHintForLaunchEndIfNeeded();
        return true;
    }

    boolean allResumedActivitiesComplete() {
        ActivityRecord resumedActivity;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (isFocusedStack(childAt) && (resumedActivity = childAt.getResumedActivity()) != null && !resumedActivity.isState(ActivityStack.ActivityState.RESUMED)) {
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

    private boolean allResumedActivitiesVisible() {
        boolean z = false;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityRecord resumedActivity = activityDisplayValueAt.getChildAt(childCount).getResumedActivity();
                if (resumedActivity != null) {
                    if (!resumedActivity.nowVisible || this.mActivitiesWaitingForVisibleActivity.contains(resumedActivity)) {
                        return false;
                    }
                    z = true;
                }
            }
        }
        return z;
    }

    boolean pauseBackStacks(boolean z, ActivityRecord activityRecord, boolean z2) {
        boolean zStartPausingLocked = false;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (!isFocusedStack(childAt) && childAt.getResumedActivity() != null) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "pauseBackStacks: stack=" + childAt + " mResumedActivity=" + childAt.getResumedActivity());
                    }
                    zStartPausingLocked |= childAt.startPausingLocked(z, false, activityRecord, z2);
                }
            }
        }
        return zStartPausingLocked;
    }

    boolean allPausedActivitiesComplete() {
        boolean z = true;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityRecord activityRecord = activityDisplayValueAt.getChildAt(childCount).mPausingActivity;
                if (activityRecord != null && !activityRecord.isState(ActivityStack.ActivityState.PAUSED, ActivityStack.ActivityState.STOPPED, ActivityStack.ActivityState.STOPPING)) {
                    if (!ActivityManagerDebugConfig.DEBUG_STATES) {
                        return false;
                    }
                    Slog.d(TAG_STATES, "allPausedActivitiesComplete: r=" + activityRecord + " state=" + activityRecord.getState());
                    z = false;
                }
            }
        }
        return z;
    }

    void cancelInitializingActivities() {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).cancelInitializingActivities();
            }
        }
    }

    void waitActivityVisible(ComponentName componentName, WaitResult waitResult) {
        this.mWaitingForActivityVisible.add(new WaitInfo(componentName, waitResult));
    }

    void cleanupActivity(ActivityRecord activityRecord) {
        this.mFinishingActivities.remove(activityRecord);
        this.mActivitiesWaitingForVisibleActivity.remove(activityRecord);
        for (int size = this.mWaitingForActivityVisible.size() - 1; size >= 0; size--) {
            if (this.mWaitingForActivityVisible.get(size).matches(activityRecord.realActivity)) {
                this.mWaitingForActivityVisible.remove(size);
            }
        }
    }

    void reportActivityVisibleLocked(ActivityRecord activityRecord) {
        sendWaitingVisibleReportLocked(activityRecord);
    }

    void sendWaitingVisibleReportLocked(ActivityRecord activityRecord) {
        boolean z = false;
        for (int size = this.mWaitingForActivityVisible.size() - 1; size >= 0; size--) {
            WaitInfo waitInfo = this.mWaitingForActivityVisible.get(size);
            if (waitInfo.matches(activityRecord.realActivity)) {
                WaitResult result = waitInfo.getResult();
                result.timeout = false;
                result.who = waitInfo.getComponent();
                result.totalTime = SystemClock.uptimeMillis() - result.thisTime;
                result.thisTime = result.totalTime;
                this.mWaitingForActivityVisible.remove(waitInfo);
                z = true;
            }
        }
        if (z) {
            this.mService.notifyAll();
        }
    }

    void reportWaitingActivityLaunchedIfNeeded(ActivityRecord activityRecord, int i) {
        if (this.mWaitingActivityLaunched.isEmpty()) {
            return;
        }
        if (i != 3 && i != 2) {
            return;
        }
        boolean z = false;
        for (int size = this.mWaitingActivityLaunched.size() - 1; size >= 0; size--) {
            WaitResult waitResultRemove = this.mWaitingActivityLaunched.remove(size);
            if (waitResultRemove.who == null) {
                waitResultRemove.result = i;
                if (i == 3) {
                    waitResultRemove.who = activityRecord.realActivity;
                }
                z = true;
            }
        }
        if (z) {
            this.mService.notifyAll();
        }
    }

    void reportActivityLaunchedLocked(boolean z, ActivityRecord activityRecord, long j, long j2) {
        boolean z2 = false;
        for (int size = this.mWaitingActivityLaunched.size() - 1; size >= 0; size--) {
            WaitResult waitResultRemove = this.mWaitingActivityLaunched.remove(size);
            if (waitResultRemove.who == null) {
                waitResultRemove.timeout = z;
                if (activityRecord != null) {
                    waitResultRemove.who = new ComponentName(activityRecord.info.packageName, activityRecord.info.name);
                }
                waitResultRemove.thisTime = j;
                waitResultRemove.totalTime = j2;
                z2 = true;
            }
        }
        if (z2) {
            this.mService.notifyAll();
        }
    }

    ActivityRecord topRunningActivityLocked() {
        return topRunningActivityLocked(false);
    }

    ActivityRecord topRunningActivityLocked(boolean z) {
        ActivityStack topStack;
        ActivityRecord activityRecord;
        ActivityStack activityStack = this.mFocusedStack;
        ActivityRecord activityRecord2 = activityStack.topRunningActivityLocked();
        if (activityRecord2 != null && isValidTopRunningActivity(activityRecord2, z)) {
            return activityRecord2;
        }
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int size = this.mTmpOrderedDisplayIds.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplay = this.mActivityDisplays.get(this.mTmpOrderedDisplayIds.get(size));
            if (activityDisplay != null && (topStack = activityDisplay.getTopStack()) != null && topStack.isFocusable() && topStack != activityStack && (activityRecord = topStack.topRunningActivityLocked()) != null && isValidTopRunningActivity(activityRecord, z)) {
                return activityRecord;
            }
        }
        return null;
    }

    private boolean isValidTopRunningActivity(ActivityRecord activityRecord, boolean z) {
        if (!z || !getKeyguardController().isKeyguardLocked()) {
            return true;
        }
        return activityRecord.canShowWhenLocked();
    }

    @VisibleForTesting
    void getRunningTasks(int i, List<ActivityManager.RunningTaskInfo> list, @WindowConfiguration.ActivityType int i2, @WindowConfiguration.WindowingMode int i3, int i4, boolean z) {
        this.mRunningTasks.getTasks(i, list, i2, i3, this.mActivityDisplays, i4, z);
    }

    ActivityInfo resolveActivity(Intent intent, ResolveInfo resolveInfo, int i, ProfilerInfo profilerInfo) {
        ActivityInfo activityInfo = resolveInfo != null ? resolveInfo.activityInfo : null;
        if (activityInfo != null) {
            intent.setComponent(new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name));
            if (!activityInfo.processName.equals("system")) {
                if ((i & 2) != 0) {
                    this.mService.setDebugApp(activityInfo.processName, true, false);
                }
                if ((i & 8) != 0) {
                    this.mService.setNativeDebuggingAppLocked(activityInfo.applicationInfo, activityInfo.processName);
                }
                if ((i & 4) != 0) {
                    this.mService.setTrackAllocationApp(activityInfo.applicationInfo, activityInfo.processName);
                }
                if (profilerInfo != null) {
                    this.mService.setProfileApp(activityInfo.applicationInfo, activityInfo.processName, profilerInfo);
                }
            }
            String launchToken = intent.getLaunchToken();
            if (activityInfo.launchToken == null && launchToken != null) {
                activityInfo.launchToken = launchToken;
            }
        }
        return activityInfo;
    }

    ResolveInfo resolveIntent(Intent intent, String str, int i, int i2, int i3) {
        ResolveInfo resolveInfoResolveIntent;
        synchronized (this.mService) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    Trace.traceBegin(64L, "resolveIntent");
                    int i4 = i2 | 65536 | 1024;
                    if (intent.isWebIntent() || (intent.getFlags() & 2048) != 0) {
                        i4 |= DumpState.DUMP_VOLUMES;
                    }
                    int i5 = i4;
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        resolveInfoResolveIntent = this.mService.getPackageManagerInternalLocked().resolveIntent(intent, str, i5, i, true, i3);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } finally {
                Trace.traceEnd(64L);
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return resolveInfoResolveIntent;
    }

    ActivityInfo resolveActivity(Intent intent, String str, int i, ProfilerInfo profilerInfo, int i2, int i3) {
        return resolveActivity(intent, resolveIntent(intent, str, i2, 0, i3), i, profilerInfo);
    }

    final boolean realStartActivityLocked(ActivityRecord activityRecord, ProcessRecord processRecord, boolean z, boolean z2) throws RemoteException {
        ActivityStack activityStack;
        ArrayList<ResultInfo> arrayList;
        ArrayList<ReferrerIntent> arrayList2;
        ProfilerInfo profilerInfo;
        ResumeActivityItem resumeActivityItemObtain;
        ActivityRecord activityRecord2 = activityRecord;
        if (!allPausedActivitiesComplete()) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_PAUSE, "realStartActivityLocked: Skipping start of r=" + activityRecord2 + " some activities pausing...");
            }
            return false;
        }
        TaskRecord task = activityRecord.getTask();
        ActivityStack stack = task.getStack();
        beginDeferResume();
        try {
            activityRecord2.startFreezingScreenLocked(processRecord, 0);
            activityRecord.startLaunchTickingLocked();
            activityRecord.setProcess(processRecord);
            if (getKeyguardController().isKeyguardLocked()) {
                activityRecord.notifyUnknownVisibilityLaunched();
            }
            if (z2) {
                ensureVisibilityAndConfig(activityRecord2, activityRecord.getDisplayId(), false, true);
            }
            if (activityRecord.getStack().checkKeyguardVisibility(activityRecord2, true, true)) {
                activityRecord2.setVisibility(true);
            }
            int i = activityRecord2.info.applicationInfo != null ? activityRecord2.info.applicationInfo.uid : -1;
            if (activityRecord2.userId != processRecord.userId || activityRecord2.appInfo.uid != i) {
                Slog.wtf(TAG, "User ID for activity changing for " + activityRecord2 + " appInfo.uid=" + activityRecord2.appInfo.uid + " info.ai.uid=" + i + " old=" + activityRecord2.app + " new=" + processRecord);
            }
            processRecord.waitingToKill = null;
            activityRecord2.launchCount++;
            activityRecord2.lastLaunchTime = SystemClock.uptimeMillis();
            if (ActivityManagerDebugConfig.DEBUG_ALL) {
                Slog.v(TAG, "Launching: " + activityRecord2);
            }
            if (processRecord.activities.indexOf(activityRecord2) < 0) {
                processRecord.activities.add(activityRecord2);
            }
            this.mService.updateLruProcessLocked(processRecord, true, null);
            ActivityManagerService.MainHandler mainHandler = this.mService.mHandler;
            ActivityManagerService activityManagerService = this.mService;
            Objects.requireNonNull(activityManagerService);
            mainHandler.post(new $$Lambda$ejtzn5TCL2GSsOkwaLFeot_Ozqg(activityManagerService));
            LockTaskController lockTaskController = this.mService.getLockTaskController();
            if (task.mLockTaskAuth == 2 || task.mLockTaskAuth == 4 || (task.mLockTaskAuth == 3 && lockTaskController.getLockTaskModeState() == 1)) {
                lockTaskController.startLockTaskMode(task, false, 0);
            }
            try {
                if (processRecord.thread == null) {
                    activityStack = stack;
                    try {
                        throw new RemoteException();
                    } catch (RemoteException e) {
                        e = e;
                    }
                } else {
                    if (z) {
                        arrayList = activityRecord2.results;
                        arrayList2 = activityRecord2.newIntents;
                    } else {
                        arrayList = null;
                        arrayList2 = null;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                        Slog.v(TAG_SWITCH, "Launching: " + activityRecord2 + " icicle=" + activityRecord2.icicle + " with results=" + arrayList + " newIntents=" + arrayList2 + " andResume=" + z);
                    }
                    EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY, Integer.valueOf(activityRecord2.userId), Integer.valueOf(System.identityHashCode(activityRecord)), Integer.valueOf(task.taskId), activityRecord2.shortComponentName);
                    if (activityRecord.isActivityTypeHome()) {
                        this.mService.mHomeProcess = task.mActivities.get(0).app;
                    }
                    this.mService.notifyPackageUse(activityRecord2.intent.getComponent().getPackageName(), 0);
                    activityRecord2.sleeping = false;
                    activityRecord2.forceNewConfig = false;
                    this.mService.getAppWarningsLocked().onStartActivity(activityRecord2);
                    this.mService.showAskCompatModeDialogLocked(activityRecord2);
                    activityRecord2.compat = this.mService.compatibilityInfoForPackageLocked(activityRecord2.info.applicationInfo);
                    if (this.mService.mProfileApp == null || !this.mService.mProfileApp.equals(processRecord.processName) || (this.mService.mProfileProc != null && this.mService.mProfileProc != processRecord)) {
                        profilerInfo = null;
                        processRecord.hasShownUi = true;
                        processRecord.pendingUiClean = true;
                        processRecord.forceProcessStateUpTo(this.mService.mTopProcessState);
                        MergedConfiguration mergedConfiguration = new MergedConfiguration(this.mService.getGlobalConfiguration(), activityRecord.getMergedOverrideConfiguration());
                        activityRecord2.setLastReportedConfiguration(mergedConfiguration);
                        logIfTransactionTooLarge(activityRecord2.intent, activityRecord2.icicle);
                        ClientTransaction clientTransactionObtain = ClientTransaction.obtain(processRecord.thread, activityRecord2.appToken);
                        try {
                            try {
                                clientTransactionObtain.addCallback(LaunchActivityItem.obtain(new Intent(activityRecord2.intent), System.identityHashCode(activityRecord), activityRecord2.info, mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration(), activityRecord2.compat, activityRecord2.launchedFromPackage, task.voiceInteractor, processRecord.repProcState, activityRecord2.icicle, activityRecord2.persistentState, arrayList, arrayList2, this.mService.isNextTransitionForward(), profilerInfo));
                                if (!z) {
                                    resumeActivityItemObtain = ResumeActivityItem.obtain(this.mService.isNextTransitionForward());
                                } else {
                                    resumeActivityItemObtain = PauseActivityItem.obtain();
                                }
                                clientTransactionObtain.setLifecycleStateRequest(resumeActivityItemObtain);
                                this.mService.getLifecycleManager().scheduleTransaction(clientTransactionObtain);
                                if ((processRecord.info.privateFlags & 2) == 0) {
                                    try {
                                        if (this.mService.mHasHeavyWeightFeature && processRecord.processName.equals(processRecord.info.packageName)) {
                                            if (this.mService.mHeavyWeightProcess != null && this.mService.mHeavyWeightProcess != processRecord) {
                                                Slog.w(TAG, "Starting new heavy weight process " + processRecord + " when already running " + this.mService.mHeavyWeightProcess);
                                            }
                                            this.mService.mHeavyWeightProcess = processRecord;
                                            Message messageObtainMessage = this.mService.mHandler.obtainMessage(24);
                                            activityRecord2 = activityRecord;
                                            messageObtainMessage.obj = activityRecord2;
                                            this.mService.mHandler.sendMessage(messageObtainMessage);
                                        } else {
                                            activityRecord2 = activityRecord;
                                        }
                                    } catch (RemoteException e2) {
                                        e = e2;
                                        activityRecord2 = activityRecord;
                                        activityStack = stack;
                                        if (!activityRecord2.launchFailed) {
                                        }
                                    }
                                }
                                endDeferResume();
                                activityRecord2.launchFailed = false;
                                if (stack.updateLRUListLocked(activityRecord2)) {
                                    Slog.w(TAG, "Activity " + activityRecord2 + " being launched, but already in LRU list");
                                }
                                if (!z && readyToResume()) {
                                    stack.minimalResumeActivityLocked(activityRecord2);
                                } else {
                                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                                        Slog.v(TAG_STATES, "Moving to PAUSED: " + activityRecord2 + " (starting in paused state)");
                                    }
                                    activityRecord2.setState(ActivityStack.ActivityState.PAUSED, "realStartActivityLocked");
                                }
                                if (isFocusedStack(stack)) {
                                    this.mService.getActivityStartController().startSetupActivity();
                                }
                                if (activityRecord2.app == null) {
                                    this.mService.mServices.updateServiceConnectionActivitiesLocked(activityRecord2.app);
                                    return true;
                                }
                                return true;
                            } catch (RemoteException e3) {
                                e = e3;
                                activityStack = stack;
                                activityRecord2 = activityRecord;
                            }
                        } catch (RemoteException e4) {
                            e = e4;
                        }
                    } else {
                        this.mService.mProfileProc = processRecord;
                        ProfilerInfo profilerInfo2 = this.mService.mProfilerInfo;
                        if (profilerInfo2 != null && profilerInfo2.profileFile != null) {
                            if (profilerInfo2.profileFd != null) {
                                try {
                                    profilerInfo2.profileFd = profilerInfo2.profileFd.dup();
                                } catch (IOException e5) {
                                    profilerInfo2.closeFd();
                                }
                            }
                            profilerInfo = new ProfilerInfo(profilerInfo2);
                        }
                        processRecord.hasShownUi = true;
                        processRecord.pendingUiClean = true;
                        processRecord.forceProcessStateUpTo(this.mService.mTopProcessState);
                        MergedConfiguration mergedConfiguration2 = new MergedConfiguration(this.mService.getGlobalConfiguration(), activityRecord.getMergedOverrideConfiguration());
                        activityRecord2.setLastReportedConfiguration(mergedConfiguration2);
                        logIfTransactionTooLarge(activityRecord2.intent, activityRecord2.icicle);
                        ClientTransaction clientTransactionObtain2 = ClientTransaction.obtain(processRecord.thread, activityRecord2.appToken);
                        clientTransactionObtain2.addCallback(LaunchActivityItem.obtain(new Intent(activityRecord2.intent), System.identityHashCode(activityRecord), activityRecord2.info, mergedConfiguration2.getGlobalConfiguration(), mergedConfiguration2.getOverrideConfiguration(), activityRecord2.compat, activityRecord2.launchedFromPackage, task.voiceInteractor, processRecord.repProcState, activityRecord2.icicle, activityRecord2.persistentState, arrayList, arrayList2, this.mService.isNextTransitionForward(), profilerInfo));
                        if (!z) {
                        }
                        clientTransactionObtain2.setLifecycleStateRequest(resumeActivityItemObtain);
                        this.mService.getLifecycleManager().scheduleTransaction(clientTransactionObtain2);
                        if ((processRecord.info.privateFlags & 2) == 0) {
                        }
                        endDeferResume();
                        activityRecord2.launchFailed = false;
                        if (stack.updateLRUListLocked(activityRecord2)) {
                        }
                        if (!z) {
                            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            }
                            activityRecord2.setState(ActivityStack.ActivityState.PAUSED, "realStartActivityLocked");
                        }
                        if (isFocusedStack(stack)) {
                        }
                        if (activityRecord2.app == null) {
                        }
                    }
                }
            } catch (RemoteException e6) {
                e = e6;
                activityStack = stack;
            }
            if (!activityRecord2.launchFailed) {
                Slog.e(TAG, "Second failure launching " + activityRecord2.intent.getComponent().flattenToShortString() + ", giving up", e);
                this.mService.appDiedLocked(processRecord);
                activityStack.requestFinishActivityLocked(activityRecord2.appToken, 0, null, "2nd-crash", false);
                endDeferResume();
                return false;
            }
            activityRecord2.launchFailed = true;
            processRecord.activities.remove(activityRecord2);
            throw e;
        } catch (Throwable th) {
            endDeferResume();
            throw th;
        }
    }

    boolean ensureVisibilityAndConfig(ActivityRecord activityRecord, int i, boolean z, boolean z2) {
        IApplicationToken.Stub stub = null;
        ensureActivitiesVisibleLocked(null, 0, false, false);
        WindowManagerService windowManagerService = this.mWindowManager;
        Configuration displayOverrideConfiguration = getDisplayOverrideConfiguration(i);
        if (activityRecord != null && activityRecord.mayFreezeScreenLocked(activityRecord.app)) {
            stub = activityRecord.appToken;
        }
        Configuration configurationUpdateOrientationFromAppTokens = windowManagerService.updateOrientationFromAppTokens(displayOverrideConfiguration, stub, i, true);
        if (activityRecord != null && z && configurationUpdateOrientationFromAppTokens != null) {
            activityRecord.frozenBeforeDestroy = true;
        }
        return this.mService.updateDisplayOverrideConfigurationLocked(configurationUpdateOrientationFromAppTokens, activityRecord, z2, i);
    }

    private void logIfTransactionTooLarge(Intent intent, Bundle bundle) {
        int size;
        Bundle extras;
        if (intent != null && (extras = intent.getExtras()) != null) {
            size = extras.getSize();
        } else {
            size = 0;
        }
        int size2 = bundle != null ? bundle.getSize() : 0;
        if (size + size2 > 200000) {
            Slog.e(TAG, "Transaction too large, intent: " + intent + ", extras size: " + size + ", icicle size: " + size2);
        }
    }

    void startSpecificActivityLocked(ActivityRecord activityRecord, boolean z, boolean z2) {
        ProcessRecord processRecordLocked = this.mService.getProcessRecordLocked(activityRecord.processName, activityRecord.info.applicationInfo.uid, true);
        getLaunchTimeTracker().setLaunchTime(activityRecord);
        if (processRecordLocked != null && processRecordLocked.thread != null) {
            try {
                if ((activityRecord.info.flags & 1) == 0 || !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(activityRecord.info.packageName)) {
                    processRecordLocked.addPackage(activityRecord.info.packageName, activityRecord.info.applicationInfo.longVersionCode, this.mService.mProcessStats);
                }
                realStartActivityLocked(activityRecord, processRecordLocked, z, z2);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity " + activityRecord.intent.getComponent().flattenToShortString(), e);
            }
        }
        this.mService.startProcessLocked(activityRecord.processName, activityRecord.info.applicationInfo, true, 0, "activity", activityRecord.intent.getComponent(), false, false, true);
    }

    void sendPowerHintForLaunchStartIfNeeded(boolean z, ActivityRecord activityRecord) {
        if (!z) {
            ActivityRecord resumedActivityLocked = getResumedActivityLocked();
            z = resumedActivityLocked == null || resumedActivityLocked.app == null || !resumedActivityLocked.app.equals(activityRecord.app);
        }
        if (z && this.mService.mLocalPowerManager != null) {
            this.mService.mLocalPowerManager.powerHint(8, 1);
            this.mPowerHintSent = true;
        }
    }

    void sendPowerHintForLaunchEndIfNeeded() {
        if (this.mPowerHintSent && this.mService.mLocalPowerManager != null) {
            this.mService.mLocalPowerManager.powerHint(8, 0);
            this.mPowerHintSent = false;
        }
    }

    boolean checkStartAnyActivityPermission(Intent intent, ActivityInfo activityInfo, String str, int i, int i2, int i3, String str2, boolean z, boolean z2, ProcessRecord processRecord, ActivityRecord activityRecord, ActivityStack activityStack) {
        String str3;
        boolean z3 = this.mService.getRecentTasks() != null && this.mService.getRecentTasks().isCallerRecents(i3);
        if (this.mService.checkPermission("android.permission.START_ANY_ACTIVITY", i2, i3) == 0 || (z3 && z2)) {
            return true;
        }
        int componentRestrictionForCallingPackage = getComponentRestrictionForCallingPackage(activityInfo, str2, i2, i3, z);
        int actionRestrictionForCallingPackage = getActionRestrictionForCallingPackage(intent.getAction(), str2, i2, i3);
        if (componentRestrictionForCallingPackage == 1 || actionRestrictionForCallingPackage == 1) {
            if (activityRecord != null) {
                activityStack.sendActivityResultLocked(-1, activityRecord, str, i, 0, null);
            }
            if (actionRestrictionForCallingPackage == 1) {
                str3 = "Permission Denial: starting " + intent.toString() + " from " + processRecord + " (pid=" + i2 + ", uid=" + i3 + ") with revoked permission " + ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction());
            } else if (!activityInfo.exported) {
                str3 = "Permission Denial: starting " + intent.toString() + " from " + processRecord + " (pid=" + i2 + ", uid=" + i3 + ") not exported from uid " + activityInfo.applicationInfo.uid;
            } else {
                str3 = "Permission Denial: starting " + intent.toString() + " from " + processRecord + " (pid=" + i2 + ", uid=" + i3 + ") requires " + activityInfo.permission;
            }
            Slog.w(TAG, str3);
            throw new SecurityException(str3);
        }
        if (actionRestrictionForCallingPackage == 2) {
            Slog.w(TAG, "Appop Denial: starting " + intent.toString() + " from " + processRecord + " (pid=" + i2 + ", uid=" + i3 + ") requires " + AppOpsManager.permissionToOp(ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction())));
            return false;
        }
        if (componentRestrictionForCallingPackage != 2) {
            return true;
        }
        Slog.w(TAG, "Appop Denial: starting " + intent.toString() + " from " + processRecord + " (pid=" + i2 + ", uid=" + i3 + ") requires appop " + AppOpsManager.permissionToOp(activityInfo.permission));
        return false;
    }

    boolean isCallerAllowedToLaunchOnDisplay(int i, int i2, int i3, ActivityInfo activityInfo) {
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "Launch on display check: displayId=" + i3 + " callingPid=" + i + " callingUid=" + i2);
        }
        if (i == -1 && i2 == -1) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: no caller info, skip check");
            }
            return true;
        }
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i3);
        if (activityDisplayOrCreateLocked == null) {
            Slog.w(TAG, "Launch on display check: display not found");
            return false;
        }
        if (this.mService.checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW", i, i2) == 0) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: allow launch any on display");
            }
            return true;
        }
        boolean zIsUidPresent = activityDisplayOrCreateLocked.isUidPresent(i2);
        int ownerUid = activityDisplayOrCreateLocked.mDisplay.getOwnerUid();
        if (activityDisplayOrCreateLocked.mDisplay.getType() == 5 && ownerUid != 1000 && ownerUid != activityInfo.applicationInfo.uid) {
            if ((activityInfo.flags & Integer.MIN_VALUE) != 0) {
                if (this.mService.checkPermission("android.permission.ACTIVITY_EMBEDDING", i, i2) == -1 && !zIsUidPresent) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG, "Launch on display check: disallow activity embedding without permission.");
                    }
                    return false;
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "Launch on display check: disallow launch on virtual display for not-embedded activity.");
                }
                return false;
            }
        }
        if (!activityDisplayOrCreateLocked.isPrivate()) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: allow launch on public display");
            }
            return true;
        }
        if (ownerUid == i2) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: allow launch for owner of the display");
            }
            return true;
        }
        if (zIsUidPresent) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "Launch on display check: allow launch for caller present on the display");
            }
            return true;
        }
        Slog.w(TAG, "Launch on display check: denied");
        return false;
    }

    void updateUIDsPresentOnDisplay() {
        this.mDisplayAccessUIDs.clear();
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            if (activityDisplayValueAt.isPrivate()) {
                this.mDisplayAccessUIDs.append(activityDisplayValueAt.mDisplayId, activityDisplayValueAt.getPresentUIDs());
            }
        }
        this.mDisplayManagerInternal.setDisplayAccessUIDs(this.mDisplayAccessUIDs);
    }

    UserInfo getUserInfo(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return UserManager.get(this.mService.mContext).getUserInfo(i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String str, int i, int i2, boolean z) {
        int iPermissionToOpCode;
        if (z || this.mService.checkComponentPermission(activityInfo.permission, i, i2, activityInfo.applicationInfo.uid, activityInfo.exported) != -1) {
            return (activityInfo.permission == null || (iPermissionToOpCode = AppOpsManager.permissionToOpCode(activityInfo.permission)) == -1 || this.mService.mAppOpsService.noteOperation(iPermissionToOpCode, i2, str) == 0 || z) ? 0 : 2;
        }
        return 1;
    }

    private int getActionRestrictionForCallingPackage(String str, String str2, int i, int i2) {
        String str3;
        if (str == null || (str3 = ACTION_TO_RUNTIME_PERMISSION.get(str)) == null) {
            return 0;
        }
        try {
            if (!ArrayUtils.contains(this.mService.mContext.getPackageManager().getPackageInfo(str2, 4096).requestedPermissions, str3)) {
                return 0;
            }
            if (this.mService.checkPermission(str3, i, i2) == -1) {
                return 1;
            }
            int iPermissionToOpCode = AppOpsManager.permissionToOpCode(str3);
            if (iPermissionToOpCode == -1 || this.mService.mAppOpsService.noteOperation(iPermissionToOpCode, i2, str2) == 0) {
                return 0;
            }
            return 2;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Cannot find package info for " + str2);
            return 0;
        }
    }

    void setLaunchSource(int i) {
        this.mLaunchingActivity.setWorkSource(new WorkSource(i));
    }

    void acquireLaunchWakelock() {
        this.mLaunchingActivity.acquire();
        if (!this.mHandler.hasMessages(104)) {
            this.mHandler.sendEmptyMessageDelayed(104, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    @GuardedBy("mService")
    private boolean checkFinishBootingLocked() {
        boolean z = this.mService.mBooting;
        boolean z2 = false;
        this.mService.mBooting = false;
        if (!this.mService.mBooted) {
            this.mService.mBooted = true;
            z2 = true;
        }
        if (z || z2) {
            this.mService.postFinishBooting(z, z2);
        }
        return z;
    }

    @GuardedBy("mService")
    final ActivityRecord activityIdleInternalLocked(IBinder iBinder, boolean z, boolean z2, Configuration configuration) {
        boolean zCheckFinishBootingLocked;
        ArrayList arrayList;
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity idle: " + iBinder);
        }
        ActivityRecord activityRecordForTokenLocked = ActivityRecord.forTokenLocked(iBinder);
        if (activityRecordForTokenLocked != null) {
            if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                Slog.d(TAG_IDLE, "activityIdleInternalLocked: Callers=" + Debug.getCallers(4));
            }
            this.mHandler.removeMessages(100, activityRecordForTokenLocked);
            activityRecordForTokenLocked.finishLaunchTickingLocked();
            if (z) {
                reportActivityLaunchedLocked(z, activityRecordForTokenLocked, -1L, -1L);
            }
            if (configuration != null) {
                activityRecordForTokenLocked.setLastReportedGlobalConfiguration(configuration);
            }
            activityRecordForTokenLocked.idle = true;
            if (isFocusedStack(activityRecordForTokenLocked.getStack()) || z) {
                zCheckFinishBootingLocked = checkFinishBootingLocked();
            }
        } else {
            zCheckFinishBootingLocked = false;
        }
        ArrayList arrayList2 = null;
        if (allResumedActivitiesIdle()) {
            if (activityRecordForTokenLocked != null) {
                this.mService.scheduleAppGcsLocked();
            }
            if (this.mLaunchingActivity.isHeld()) {
                this.mHandler.removeMessages(104);
                this.mLaunchingActivity.release();
            }
            ensureActivitiesVisibleLocked(null, 0, false);
        }
        ArrayList<ActivityRecord> arrayListProcessStoppingActivitiesLocked = processStoppingActivitiesLocked(activityRecordForTokenLocked, true, z2);
        int size = arrayListProcessStoppingActivitiesLocked != null ? arrayListProcessStoppingActivitiesLocked.size() : 0;
        int size2 = this.mFinishingActivities.size();
        if (size2 > 0) {
            arrayList = new ArrayList(this.mFinishingActivities);
            this.mFinishingActivities.clear();
        } else {
            arrayList = null;
        }
        if (this.mStartingUsers.size() > 0) {
            arrayList2 = new ArrayList(this.mStartingUsers);
            this.mStartingUsers.clear();
        }
        ActivityRecord activityRecord = activityRecordForTokenLocked;
        for (int i = 0; i < size; i++) {
            activityRecord = arrayListProcessStoppingActivitiesLocked.get(i);
            ActivityStack stack = activityRecord.getStack();
            if (stack != null) {
                if (activityRecord.finishing) {
                    stack.finishCurrentActivityLocked(activityRecord, 0, false, "activityIdleInternalLocked");
                } else {
                    stack.stopActivityLocked(activityRecord);
                }
            }
        }
        boolean zDestroyActivityLocked = false;
        for (int i2 = 0; i2 < size2; i2++) {
            activityRecord = (ActivityRecord) arrayList.get(i2);
            ActivityStack stack2 = activityRecord.getStack();
            if (stack2 != null) {
                zDestroyActivityLocked |= stack2.destroyActivityLocked(activityRecord, true, "finish-idle");
            }
        }
        if (!zCheckFinishBootingLocked && arrayList2 != null) {
            for (int i3 = 0; i3 < arrayList2.size(); i3++) {
                this.mService.mUserController.finishUserSwitch((UserState) arrayList2.get(i3));
            }
        }
        this.mService.trimApplications();
        if (zDestroyActivityLocked) {
            resumeFocusedStackTopActivityLocked();
        }
        return activityRecord;
    }

    boolean handleAppDiedLocked(ProcessRecord processRecord) {
        boolean zHandleAppDiedLocked = false;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                zHandleAppDiedLocked |= activityDisplayValueAt.getChildAt(childCount).handleAppDiedLocked(processRecord);
            }
        }
        return zHandleAppDiedLocked;
    }

    void closeSystemDialogsLocked() {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).closeSystemDialogsLocked();
            }
        }
    }

    void removeUserLocked(int i) {
        this.mUserStackInFront.delete(i);
    }

    void updateUserStackLocked(int i, ActivityStack activityStack) {
        if (i != this.mCurrentUser) {
            this.mUserStackInFront.put(i, activityStack != null ? activityStack.getStackId() : this.mHomeStack.mStackId);
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String str, Set<String> set, boolean z, boolean z2, int i) {
        boolean z3 = false;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                if (activityDisplayValueAt.getChildAt(childCount).finishDisabledPackageActivitiesLocked(str, set, z, z2, i)) {
                    z3 = true;
                }
            }
        }
        return z3;
    }

    void updatePreviousProcessLocked(ActivityRecord activityRecord) {
        ProcessRecord processRecord = null;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            int childCount = activityDisplayValueAt.getChildCount() - 1;
            while (true) {
                if (childCount >= 0) {
                    ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                    if (isFocusedStack(childAt)) {
                        ActivityRecord resumedActivity = childAt.getResumedActivity();
                        if (resumedActivity != null) {
                            processRecord = resumedActivity.app;
                        } else if (childAt.mPausingActivity != null) {
                            processRecord = childAt.mPausingActivity.app;
                        }
                    } else {
                        childCount--;
                    }
                }
            }
        }
        if (activityRecord.app != null && processRecord != null && activityRecord.app != processRecord && activityRecord.lastVisibleTime > this.mService.mPreviousProcessVisibleTime && activityRecord.app != this.mService.mHomeProcess) {
            this.mService.mPreviousProcess = activityRecord.app;
            this.mService.mPreviousProcessVisibleTime = activityRecord.lastVisibleTime;
        }
    }

    boolean resumeFocusedStackTopActivityLocked() {
        return resumeFocusedStackTopActivityLocked(null, null, null);
    }

    boolean resumeFocusedStackTopActivityLocked(ActivityStack activityStack, ActivityRecord activityRecord, ActivityOptions activityOptions) {
        if (!readyToResume()) {
            return false;
        }
        if (activityStack != null && isFocusedStack(activityStack)) {
            return activityStack.resumeTopActivityUncheckedLocked(activityRecord, activityOptions);
        }
        ActivityRecord activityRecord2 = this.mFocusedStack.topRunningActivityLocked();
        if (activityRecord2 == null || !activityRecord2.isState(ActivityStack.ActivityState.RESUMED)) {
            this.mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
        } else if (activityRecord2.isState(ActivityStack.ActivityState.RESUMED)) {
            this.mFocusedStack.executeAppTransition(activityOptions);
        }
        return false;
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo applicationInfo) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).updateActivityApplicationInfoLocked(applicationInfo);
            }
        }
    }

    TaskRecord finishTopCrashedActivitiesLocked(ProcessRecord processRecord, String str) {
        ActivityStack focusedStack = getFocusedStack();
        TaskRecord taskRecord = null;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int i = 0; i < activityDisplayValueAt.getChildCount(); i++) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(i);
                TaskRecord taskRecordFinishTopCrashedActivityLocked = childAt.finishTopCrashedActivityLocked(processRecord, str);
                if (childAt == focusedStack || taskRecord == null) {
                    taskRecord = taskRecordFinishTopCrashedActivityLocked;
                }
            }
        }
        return taskRecord;
    }

    void finishVoiceTask(IVoiceInteractionSession iVoiceInteractionSession) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            int childCount = activityDisplayValueAt.getChildCount();
            for (int i = 0; i < childCount; i++) {
                activityDisplayValueAt.getChildAt(i).finishVoiceTask(iVoiceInteractionSession);
            }
        }
    }

    void findTaskToMoveToFront(TaskRecord taskRecord, int i, ActivityOptions activityOptions, String str, boolean z) {
        AppTimeTracker appTimeTracker;
        ActivityStack stack = taskRecord.getStack();
        if (stack == null) {
            Slog.e(TAG, "findTaskToMoveToFront: can't move task=" + taskRecord + " to front. Stack is null");
            return;
        }
        if ((i & 2) == 0) {
            this.mUserLeaving = true;
        }
        ActivityRecord activityRecord = topRunningActivityLocked();
        if ((i & 1) != 0 || (activityRecord != null && activityRecord.isActivityTypeRecents())) {
            moveHomeStackToFront("findTaskToMoveToFront");
        }
        if (taskRecord.isResizeable() && canUseActivityOptionsLaunchBounds(activityOptions)) {
            Rect launchBounds = activityOptions.getLaunchBounds();
            taskRecord.updateOverrideConfiguration(launchBounds);
            ActivityStack launchStack = getLaunchStack(null, activityOptions, taskRecord, true);
            if (launchStack != stack) {
                taskRecord.reparent(launchStack, true, 1, false, true, "findTaskToMoveToFront");
                launchStack = stack;
            }
            if (launchStack.resizeStackWithLaunchBounds()) {
                resizeStackLocked(launchStack, launchBounds, null, null, false, true, false);
            } else {
                taskRecord.resizeWindowContainer();
            }
        }
        ActivityRecord topActivity = taskRecord.getTopActivity();
        if (topActivity == null) {
            appTimeTracker = null;
        } else {
            appTimeTracker = topActivity.appTimeTracker;
        }
        stack.moveTaskToFrontLocked(taskRecord, false, activityOptions, appTimeTracker, str);
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "findTaskToMoveToFront: moved to front of stack=" + stack);
        }
        handleNonResizableTaskIfNeeded(taskRecord, 0, 0, stack, z);
    }

    boolean canUseActivityOptionsLaunchBounds(ActivityOptions activityOptions) {
        if (activityOptions == null || activityOptions.getLaunchBounds() == null) {
            return false;
        }
        return (this.mService.mSupportsPictureInPicture && activityOptions.getLaunchWindowingMode() == 2) || this.mService.mSupportsFreeformWindowManagement;
    }

    LaunchParamsController getLaunchParamsController() {
        return this.mLaunchParamsController;
    }

    protected <T extends ActivityStack> T getStack(int i) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            T t = (T) this.mActivityDisplays.valueAt(size).getStack(i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private <T extends ActivityStack> T getStack(int i, int i2) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            T t = (T) this.mActivityDisplays.valueAt(size).getStack(i, i2);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    int resolveActivityType(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord) {
        int activityType = activityRecord != null ? activityRecord.getActivityType() : 0;
        if (activityType == 0 && taskRecord != null) {
            activityType = taskRecord.getActivityType();
        }
        if (activityType != 0) {
            return activityType;
        }
        if (activityOptions != null) {
            activityType = activityOptions.getLaunchActivityType();
        }
        if (activityType != 0) {
            return activityType;
        }
        return 1;
    }

    <T extends ActivityStack> T getLaunchStack(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord, boolean z) {
        return (T) getLaunchStack(activityRecord, activityOptions, taskRecord, z, -1);
    }

    <T extends ActivityStack> T getLaunchStack(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord, boolean z, int i) {
        int launchTaskId;
        int launchDisplayId;
        T t;
        T t2;
        T t3;
        if (activityOptions != null) {
            launchTaskId = activityOptions.getLaunchTaskId();
            launchDisplayId = activityOptions.getLaunchDisplayId();
        } else {
            launchTaskId = -1;
            launchDisplayId = -1;
        }
        if (launchTaskId != -1) {
            activityOptions.setLaunchTaskId(-1);
            TaskRecord taskRecordAnyTaskForIdLocked = anyTaskForIdLocked(launchTaskId, 2, activityOptions, z);
            activityOptions.setLaunchTaskId(launchTaskId);
            if (taskRecordAnyTaskForIdLocked != null) {
                return (T) taskRecordAnyTaskForIdLocked.getStack();
            }
        }
        int iResolveActivityType = resolveActivityType(activityRecord, activityOptions, taskRecord);
        if (launchDisplayId != -1) {
            i = launchDisplayId;
        }
        if (i != -1 && canLaunchOnDisplay(activityRecord, i)) {
            if (activityRecord != null && (t3 = (T) getValidLaunchStackOnDisplay(i, activityRecord)) != null) {
                return t3;
            }
            ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i);
            if (activityDisplayOrCreateLocked != null && (t2 = (T) activityDisplayOrCreateLocked.getOrCreateStack(activityRecord, activityOptions, taskRecord, iResolveActivityType, z)) != null) {
                return t2;
            }
        }
        ActivityDisplay defaultDisplay = null;
        if (taskRecord != null) {
            t = (T) taskRecord.getStack();
        } else {
            t = null;
        }
        if (t == null && activityRecord != null) {
            t = (T) activityRecord.getStack();
        }
        if (t != null && (defaultDisplay = t.getDisplay()) != null && canLaunchOnDisplay(activityRecord, defaultDisplay.mDisplayId)) {
            int iResolveWindowingMode = defaultDisplay.resolveWindowingMode(activityRecord, activityOptions, taskRecord, iResolveActivityType);
            if (t.isCompatible(iResolveWindowingMode, iResolveActivityType)) {
                return t;
            }
            if (iResolveWindowingMode == 4 && defaultDisplay.getSplitScreenPrimaryStack() == t && taskRecord == t.topTask()) {
                return t;
            }
        }
        if (defaultDisplay == null || !canLaunchOnDisplay(activityRecord, defaultDisplay.mDisplayId) || (iResolveActivityType != 1 && iResolveActivityType != 0)) {
            defaultDisplay = getDefaultDisplay();
        }
        return (T) defaultDisplay.getOrCreateStack(activityRecord, activityOptions, taskRecord, iResolveActivityType, z);
    }

    private boolean canLaunchOnDisplay(ActivityRecord activityRecord, int i) {
        if (activityRecord == null) {
            return true;
        }
        return activityRecord.canBeLaunchedOnDisplay(i);
    }

    ActivityStack getValidLaunchStackOnDisplay(int i, ActivityRecord activityRecord) {
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i);
        if (activityDisplayOrCreateLocked == null) {
            throw new IllegalArgumentException("Display with displayId=" + i + " not found.");
        }
        if (!activityRecord.canBeLaunchedOnDisplay(i)) {
            return null;
        }
        for (int childCount = activityDisplayOrCreateLocked.getChildCount() - 1; childCount >= 0; childCount--) {
            ActivityStack childAt = activityDisplayOrCreateLocked.getChildAt(childCount);
            if (isValidLaunchStack(childAt, i, activityRecord)) {
                return childAt;
            }
        }
        if (i != 0) {
            return activityDisplayOrCreateLocked.createStack(activityRecord.getWindowingMode(), activityRecord.getActivityType(), true);
        }
        Slog.w(TAG, "getValidLaunchStackOnDisplay: can't launch on displayId " + i);
        return null;
    }

    private boolean isValidLaunchStack(ActivityStack activityStack, int i, ActivityRecord activityRecord) {
        switch (activityStack.getActivityType()) {
            case 2:
                return activityRecord.isActivityTypeHome();
            case 3:
                return activityRecord.isActivityTypeRecents();
            case 4:
                return activityRecord.isActivityTypeAssistant();
            default:
                switch (activityStack.getWindowingMode()) {
                    case 1:
                        return true;
                    case 2:
                        return activityRecord.supportsPictureInPicture();
                    case 3:
                        return activityRecord.supportsSplitScreenWindowingMode();
                    case 4:
                        return activityRecord.supportsSplitScreenWindowingMode();
                    case 5:
                        return activityRecord.supportsFreeform();
                    default:
                        if (!activityStack.isOnHomeDisplay()) {
                            return activityRecord.canBeLaunchedOnDisplay(i);
                        }
                        Slog.e(TAG, "isValidLaunchStack: Unexpected stack=" + activityStack);
                        return false;
                }
        }
    }

    ActivityStack getNextFocusableStackLocked(ActivityStack activityStack, boolean z) {
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        int windowingMode = activityStack != null ? activityStack.getWindowingMode() : 0;
        ActivityStack activityStack2 = null;
        for (int size = this.mTmpOrderedDisplayIds.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(this.mTmpOrderedDisplayIds.get(size));
            if (activityDisplayOrCreateLocked != null) {
                for (int childCount = activityDisplayOrCreateLocked.getChildCount() - 1; childCount >= 0; childCount--) {
                    ActivityStack childAt = activityDisplayOrCreateLocked.getChildAt(childCount);
                    if ((!z || childAt != activityStack) && childAt.isFocusable() && childAt.shouldBeVisible(null)) {
                        if (windowingMode == 4 && activityStack2 == null && childAt.inSplitScreenPrimaryWindowingMode()) {
                            activityStack2 = childAt;
                        } else {
                            if (activityStack2 != null && childAt.inSplitScreenSecondaryWindowingMode()) {
                                return activityStack2;
                            }
                            return childAt;
                        }
                    }
                }
            }
        }
        return activityStack2;
    }

    ActivityStack getNextValidLaunchStackLocked(ActivityRecord activityRecord, int i) {
        ActivityStack validLaunchStackOnDisplay;
        this.mWindowManager.getDisplaysInFocusOrder(this.mTmpOrderedDisplayIds);
        for (int size = this.mTmpOrderedDisplayIds.size() - 1; size >= 0; size--) {
            int i2 = this.mTmpOrderedDisplayIds.get(size);
            if (i2 != i && (validLaunchStackOnDisplay = getValidLaunchStackOnDisplay(i2, activityRecord)) != null) {
                return validLaunchStackOnDisplay;
            }
        }
        return null;
    }

    ActivityRecord getHomeActivity() {
        return getHomeActivityForUser(this.mCurrentUser);
    }

    ActivityRecord getHomeActivityForUser(int i) {
        ArrayList<TaskRecord> allTasks = this.mHomeStack.getAllTasks();
        for (int size = allTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = allTasks.get(size);
            if (taskRecord.isActivityTypeHome()) {
                ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
                for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                    ActivityRecord activityRecord = arrayList.get(size2);
                    if (activityRecord.isActivityTypeHome() && (i == -1 || activityRecord.userId == i)) {
                        return activityRecord;
                    }
                }
            }
        }
        return null;
    }

    void resizeStackLocked(ActivityStack activityStack, Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, boolean z3) {
        if (activityStack.inSplitScreenPrimaryWindowingMode()) {
            resizeDockedStackLocked(rect, rect2, rect3, null, null, z, z3);
            return;
        }
        boolean zHasSplitScreenPrimaryStack = getDefaultDisplay().hasSplitScreenPrimaryStack();
        if (!z2 && !activityStack.getWindowConfiguration().tasksAreFloating() && zHasSplitScreenPrimaryStack) {
            return;
        }
        Trace.traceBegin(64L, "am.resizeStack_" + activityStack.mStackId);
        this.mWindowManager.deferSurfaceLayout();
        try {
            if (activityStack.affectedBySplitScreenResize()) {
                if (rect == null && activityStack.inSplitScreenWindowingMode()) {
                    activityStack.setWindowingMode(1);
                } else if (zHasSplitScreenPrimaryStack) {
                    activityStack.setWindowingMode(4);
                }
            }
            activityStack.resize(rect, rect2, rect3);
            if (!z3) {
                activityStack.ensureVisibleActivitiesConfigurationLocked(activityStack.topRunningActivityLocked(), z);
            }
        } finally {
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
    }

    void deferUpdateRecentsHomeStackBounds() {
        deferUpdateBounds(3);
        deferUpdateBounds(2);
    }

    void deferUpdateBounds(int i) {
        ActivityStack stack = getStack(0, i);
        if (stack != null) {
            stack.deferUpdateBounds();
        }
    }

    void continueUpdateRecentsHomeStackBounds() {
        continueUpdateBounds(3);
        continueUpdateBounds(2);
    }

    void continueUpdateBounds(int i) {
        ActivityStack stack = getStack(0, i);
        if (stack != null) {
            stack.continueUpdateBounds();
        }
    }

    void notifyAppTransitionDone() {
        continueUpdateRecentsHomeStackBounds();
        for (int size = this.mResizingTasksDuringAnimation.size() - 1; size >= 0; size--) {
            TaskRecord taskRecordAnyTaskForIdLocked = anyTaskForIdLocked(this.mResizingTasksDuringAnimation.valueAt(size).intValue(), 0);
            if (taskRecordAnyTaskForIdLocked != null) {
                taskRecordAnyTaskForIdLocked.setTaskDockedResizing(false);
            }
        }
        this.mResizingTasksDuringAnimation.clear();
    }

    private void moveTasksToFullscreenStackInSurfaceTransaction(ActivityStack activityStack, int i, boolean z) {
        ActivityDisplay activityDisplay;
        this.mWindowManager.deferSurfaceLayout();
        try {
            int windowingMode = activityStack.getWindowingMode();
            boolean z2 = windowingMode == 2;
            ActivityDisplay activityDisplay2 = getActivityDisplay(i);
            if (windowingMode == 3) {
                activityDisplay2.onExitingSplitScreenMode();
                for (int childCount = activityDisplay2.getChildCount() - 1; childCount >= 0; childCount--) {
                    ActivityStack childAt = activityDisplay2.getChildAt(childCount);
                    if (childAt.inSplitScreenSecondaryWindowingMode()) {
                        resizeStackLocked(childAt, null, null, null, true, true, true);
                    }
                }
                this.mAllowDockedStackResize = false;
            }
            ArrayList<TaskRecord> allTasks = activityStack.getAllTasks();
            if (!allTasks.isEmpty()) {
                this.mTmpOptions.setLaunchWindowingMode(1);
                int size = allTasks.size();
                int i2 = 0;
                while (i2 < size) {
                    TaskRecord taskRecord = allTasks.get(i2);
                    ActivityStack orCreateStack = activityDisplay2.getOrCreateStack(null, this.mTmpOptions, taskRecord, taskRecord.getActivityType(), z);
                    if (z) {
                        activityDisplay = activityDisplay2;
                        taskRecord.reparent(orCreateStack, true, 0, i2 == size + (-1), true, z2, "moveTasksToFullscreenStack - onTop");
                        MetricsLoggerWrapper.logPictureInPictureFullScreen(this.mService.mContext, taskRecord.effectiveUid, taskRecord.realActivity.flattenToString());
                    } else {
                        activityDisplay = activityDisplay2;
                        taskRecord.reparent(orCreateStack, true, 2, false, true, z2, "moveTasksToFullscreenStack - NOT_onTop");
                    }
                    i2++;
                    activityDisplay2 = activityDisplay;
                }
            }
            ensureActivitiesVisibleLocked(null, 0, true);
            resumeFocusedStackTopActivityLocked();
        } finally {
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    void moveTasksToFullscreenStackLocked(ActivityStack activityStack, boolean z) {
        moveTasksToFullscreenStackLocked(activityStack, 0, z);
    }

    void moveTasksToFullscreenStackLocked(final ActivityStack activityStack, final int i, final boolean z) {
        this.mWindowManager.inSurfaceTransaction(new Runnable() {
            @Override
            public final void run() {
                this.f$0.moveTasksToFullscreenStackInSurfaceTransaction(activityStack, i, z);
            }
        });
    }

    void setSplitScreenResizing(boolean z) {
        if (z == this.mDockedStackResizing) {
            return;
        }
        this.mDockedStackResizing = z;
        this.mWindowManager.setDockedStackResizing(z);
        if (!z && this.mHasPendingDockedBounds) {
            resizeDockedStackLocked(this.mPendingDockedBounds, this.mPendingTempDockedTaskBounds, this.mPendingTempDockedTaskInsetBounds, this.mPendingTempOtherTaskBounds, this.mPendingTempOtherTaskInsetBounds, true);
            this.mHasPendingDockedBounds = false;
            this.mPendingDockedBounds = null;
            this.mPendingTempDockedTaskBounds = null;
            this.mPendingTempDockedTaskInsetBounds = null;
            this.mPendingTempOtherTaskBounds = null;
            this.mPendingTempOtherTaskInsetBounds = null;
        }
    }

    void resizeDockedStackLocked(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, boolean z) {
        resizeDockedStackLocked(rect, rect2, rect3, rect4, rect5, z, false);
    }

    private void resizeDockedStackLocked(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, boolean z, boolean z2) {
        int i;
        Rect rect6;
        if (!this.mAllowDockedStackResize) {
            return;
        }
        ActivityStack splitScreenPrimaryStack = getDefaultDisplay().getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack == null) {
            Slog.w(TAG, "resizeDockedStackLocked: docked stack not found");
            return;
        }
        if (this.mDockedStackResizing) {
            this.mHasPendingDockedBounds = true;
            this.mPendingDockedBounds = Rect.copyOrNull(rect);
            this.mPendingTempDockedTaskBounds = Rect.copyOrNull(rect2);
            this.mPendingTempDockedTaskInsetBounds = Rect.copyOrNull(rect3);
            this.mPendingTempOtherTaskBounds = Rect.copyOrNull(rect4);
            this.mPendingTempOtherTaskInsetBounds = Rect.copyOrNull(rect5);
        }
        Trace.traceBegin(64L, "am.resizeDockedStack");
        this.mWindowManager.deferSurfaceLayout();
        try {
            this.mAllowDockedStackResize = false;
            ActivityRecord activityRecord = splitScreenPrimaryStack.topRunningActivityLocked();
            splitScreenPrimaryStack.resize(rect, rect2, rect3);
            if (splitScreenPrimaryStack.getWindowingMode() == 1 || (rect == null && !splitScreenPrimaryStack.isAttached())) {
                moveTasksToFullscreenStackLocked(splitScreenPrimaryStack, true);
                activityRecord = null;
            } else {
                ActivityDisplay defaultDisplay = getDefaultDisplay();
                Rect rect7 = new Rect();
                int childCount = defaultDisplay.getChildCount() - 1;
                while (childCount >= 0) {
                    ActivityStack childAt = defaultDisplay.getChildAt(childCount);
                    if (childAt.getWindowingMode() == 3 || !childAt.affectedBySplitScreenResize() || (this.mDockedStackResizing && !childAt.isTopActivityVisible())) {
                        i = childCount;
                        rect6 = rect7;
                    } else {
                        childAt.setWindowingMode(4);
                        childAt.getStackDockedModeBounds(rect4, this.tempRect, rect7, true);
                        i = childCount;
                        rect6 = rect7;
                        resizeStackLocked(childAt, !this.tempRect.isEmpty() ? this.tempRect : null, !rect7.isEmpty() ? rect7 : rect4, rect5, z, true, z2);
                    }
                    childCount = i - 1;
                    rect7 = rect6;
                }
            }
            if (!z2) {
                splitScreenPrimaryStack.ensureVisibleActivitiesConfigurationLocked(activityRecord, z);
            }
        } finally {
            this.mAllowDockedStackResize = true;
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
    }

    void resizePinnedStackLocked(Rect rect, Rect rect2) {
        PinnedActivityStack pinnedStack = getDefaultDisplay().getPinnedStack();
        if (pinnedStack == null) {
            Slog.w(TAG, "resizePinnedStackLocked: pinned stack not found");
            return;
        }
        if (pinnedStack.getWindowContainerController().pinnedStackResizeDisallowed()) {
            return;
        }
        Trace.traceBegin(64L, "am.resizePinnedStack");
        this.mWindowManager.deferSurfaceLayout();
        try {
            ActivityRecord activityRecord = pinnedStack.topRunningActivityLocked();
            Rect rect3 = null;
            if (rect2 != null && pinnedStack.isAnimatingBoundsToFullscreen()) {
                rect3 = this.tempRect;
                rect3.top = 0;
                rect3.left = 0;
                rect3.right = rect2.width();
                rect3.bottom = rect2.height();
            }
            if (rect != null && rect2 == null) {
                pinnedStack.onPipAnimationEndResize();
            }
            pinnedStack.resize(rect, rect2, rect3);
            pinnedStack.ensureVisibleActivitiesConfigurationLocked(activityRecord, false);
        } finally {
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64L);
        }
    }

    private void removeStackInSurfaceTransaction(ActivityStack activityStack) {
        ArrayList<TaskRecord> allTasks = activityStack.getAllTasks();
        if (activityStack.getWindowingMode() == 2) {
            PinnedActivityStack pinnedActivityStack = (PinnedActivityStack) activityStack;
            pinnedActivityStack.mForceHidden = true;
            pinnedActivityStack.ensureActivitiesVisibleLocked(null, 0, true);
            pinnedActivityStack.mForceHidden = false;
            activityIdleInternalLocked(null, false, true, null);
            moveTasksToFullscreenStackLocked(pinnedActivityStack, false);
            return;
        }
        for (int size = allTasks.size() - 1; size >= 0; size--) {
            removeTaskByIdLocked(allTasks.get(size).taskId, true, true, "remove-stack");
        }
    }

    void removeStack(final ActivityStack activityStack) {
        this.mWindowManager.inSurfaceTransaction(new Runnable() {
            @Override
            public final void run() {
                this.f$0.removeStackInSurfaceTransaction(activityStack);
            }
        });
    }

    void removeStacksInWindowingModes(int... iArr) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            this.mActivityDisplays.valueAt(size).removeStacksInWindowingModes(iArr);
        }
    }

    void removeStacksWithActivityTypes(int... iArr) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            this.mActivityDisplays.valueAt(size).removeStacksWithActivityTypes(iArr);
        }
    }

    boolean removeTaskByIdLocked(int i, boolean z, boolean z2, String str) {
        return removeTaskByIdLocked(i, z, z2, false, str);
    }

    boolean removeTaskByIdLocked(int i, boolean z, boolean z2, boolean z3, String str) {
        TaskRecord taskRecordAnyTaskForIdLocked = anyTaskForIdLocked(i, 1);
        if (taskRecordAnyTaskForIdLocked != null) {
            taskRecordAnyTaskForIdLocked.removeTaskActivitiesLocked(z3, str);
            cleanUpRemovedTaskLocked(taskRecordAnyTaskForIdLocked, z, z2);
            this.mService.getLockTaskController().clearLockedTask(taskRecordAnyTaskForIdLocked);
            if (taskRecordAnyTaskForIdLocked.isPersistable) {
                this.mService.notifyTaskPersisterLocked(null, true);
            }
            return true;
        }
        Slog.w(TAG, "Request to remove task ignored for non-existent task " + i);
        return false;
    }

    void cleanUpRemovedTaskLocked(TaskRecord taskRecord, boolean z, boolean z2) {
        if (z2) {
            this.mRecentTasks.remove(taskRecord);
        }
        ComponentName component = taskRecord.getBaseIntent().getComponent();
        if (component == null) {
            Slog.w(TAG, "No component for base intent of task: " + taskRecord);
            return;
        }
        this.mService.mServices.cleanUpRemovedTaskLocked(taskRecord, component, new Intent(taskRecord.getBaseIntent()));
        if (!z) {
            return;
        }
        String packageName = component.getPackageName();
        ArrayList arrayList = new ArrayList();
        ArrayMap map = this.mService.mProcessNames.getMap();
        for (int i = 0; i < map.size(); i++) {
            SparseArray sparseArray = (SparseArray) map.valueAt(i);
            for (int i2 = 0; i2 < sparseArray.size(); i2++) {
                ProcessRecord processRecord = (ProcessRecord) sparseArray.valueAt(i2);
                if (processRecord.userId == taskRecord.userId && processRecord != this.mService.mHomeProcess && processRecord.pkgList.containsKey(packageName)) {
                    for (int i3 = 0; i3 < processRecord.activities.size(); i3++) {
                        TaskRecord task = processRecord.activities.get(i3).getTask();
                        if (taskRecord.taskId != task.taskId && task.inRecents) {
                            return;
                        }
                    }
                    if (processRecord.foregroundServices) {
                        return;
                    } else {
                        arrayList.add(processRecord);
                    }
                }
            }
        }
        for (int i4 = 0; i4 < arrayList.size(); i4++) {
            ProcessRecord processRecord2 = (ProcessRecord) arrayList.get(i4);
            if (processRecord2.setSchedGroup == 0 && processRecord2.curReceivers.isEmpty()) {
                processRecord2.kill("remove task", true);
            } else {
                processRecord2.waitingToKill = "remove task";
            }
        }
    }

    boolean restoreRecentTaskLocked(TaskRecord taskRecord, ActivityOptions activityOptions, boolean z) {
        ActivityStack launchStack = getLaunchStack(null, activityOptions, taskRecord, z);
        ActivityStack stack = taskRecord.getStack();
        if (stack != null) {
            if (stack == launchStack) {
                return true;
            }
            stack.removeTask(taskRecord, "restoreRecentTaskLocked", 1);
        }
        launchStack.addTask(taskRecord, z, "restoreRecentTask");
        taskRecord.createWindowContainer(z, true);
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Added restored task=" + taskRecord + " to stack=" + launchStack);
        }
        ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            arrayList.get(size).createWindowContainer();
        }
        return true;
    }

    @Override
    public void onRecentTaskAdded(TaskRecord taskRecord) {
        taskRecord.touchActiveTime();
    }

    @Override
    public void onRecentTaskRemoved(TaskRecord taskRecord, boolean z) {
        if (z) {
            removeTaskByIdLocked(taskRecord.taskId, false, false, false, "recent-task-trimmed");
        }
        taskRecord.removedFromRecents();
    }

    void moveStackToDisplayLocked(int i, int i2, boolean z) {
        ActivityDisplay activityDisplayOrCreateLocked = getActivityDisplayOrCreateLocked(i2);
        if (activityDisplayOrCreateLocked == null) {
            throw new IllegalArgumentException("moveStackToDisplayLocked: Unknown displayId=" + i2);
        }
        ActivityStack stack = getStack(i);
        if (stack == null) {
            throw new IllegalArgumentException("moveStackToDisplayLocked: Unknown stackId=" + i);
        }
        ActivityDisplay display = stack.getDisplay();
        if (display == null) {
            throw new IllegalStateException("moveStackToDisplayLocked: Stack with stack=" + stack + " is not attached to any display.");
        }
        if (display.mDisplayId == i2) {
            throw new IllegalArgumentException("Trying to move stack=" + stack + " to its current displayId=" + i2);
        }
        stack.reparent(activityDisplayOrCreateLocked, z);
    }

    ActivityStack getReparentTargetStack(TaskRecord taskRecord, ActivityStack activityStack, boolean z) {
        ActivityStack stack = taskRecord.getStack();
        int i = activityStack.mStackId;
        boolean zInMultiWindowMode = activityStack.inMultiWindowMode();
        if (stack != null && stack.mStackId == i) {
            Slog.w(TAG, "Can not reparent to same stack, task=" + taskRecord + " already in stackId=" + i);
            return stack;
        }
        if (zInMultiWindowMode && !this.mService.mSupportsMultiWindow) {
            throw new IllegalArgumentException("Device doesn't support multi-window, can not reparent task=" + taskRecord + " to stack=" + activityStack);
        }
        if (activityStack.mDisplayId != 0 && !this.mService.mSupportsMultiDisplay) {
            throw new IllegalArgumentException("Device doesn't support multi-display, can not reparent task=" + taskRecord + " to stackId=" + i);
        }
        if (activityStack.getWindowingMode() == 5 && !this.mService.mSupportsFreeformWindowManagement) {
            throw new IllegalArgumentException("Device doesn't support freeform, can not reparent task=" + taskRecord);
        }
        if (zInMultiWindowMode && !taskRecord.isResizeable()) {
            Slog.w(TAG, "Can not move unresizeable task=" + taskRecord + " to multi-window stack=" + activityStack + " Moving to a fullscreen stack instead.");
            if (stack != null) {
                return stack;
            }
            return activityStack.getDisplay().createStack(1, activityStack.getActivityType(), z);
        }
        return activityStack;
    }

    boolean moveTopStackActivityToPinnedStackLocked(int i, Rect rect) {
        ActivityStack stack = getStack(i);
        if (stack == null) {
            throw new IllegalArgumentException("moveTopStackActivityToPinnedStackLocked: Unknown stackId=" + i);
        }
        ActivityRecord activityRecord = stack.topRunningActivityLocked();
        if (activityRecord == null) {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: No top running activity in stack=" + stack);
            return false;
        }
        if (!this.mService.mForceResizableActivities && !activityRecord.supportsPictureInPicture()) {
            Slog.w(TAG, "moveTopStackActivityToPinnedStackLocked: Picture-In-Picture not supported for  r=" + activityRecord);
            return false;
        }
        moveActivityToPinnedStackLocked(activityRecord, null, 0.0f, "moveTopActivityToPinnedStack");
        return true;
    }

    void moveActivityToPinnedStackLocked(ActivityRecord activityRecord, Rect rect, float f, String str) {
        Rect rect2;
        PinnedActivityStack pinnedActivityStack;
        this.mWindowManager.deferSurfaceLayout();
        ActivityDisplay display = activityRecord.getStack().getDisplay();
        ActivityStack pinnedStack = display.getPinnedStack();
        if (pinnedStack != null) {
            moveTasksToFullscreenStackLocked(pinnedStack, false);
        }
        PinnedActivityStack pinnedActivityStack2 = (PinnedActivityStack) display.getOrCreateStack(2, activityRecord.getActivityType(), true);
        Rect defaultPictureInPictureBounds = pinnedActivityStack2.getDefaultPictureInPictureBounds(f);
        try {
            TaskRecord task = activityRecord.getTask();
            resizeStackLocked(pinnedActivityStack2, task.getOverrideBounds(), null, null, false, true, false);
            if (task.mActivities.size() == 1) {
                rect2 = defaultPictureInPictureBounds;
                pinnedActivityStack = pinnedActivityStack2;
                task.reparent((ActivityStack) pinnedActivityStack2, true, 0, false, true, false, str);
            } else {
                rect2 = defaultPictureInPictureBounds;
                pinnedActivityStack = pinnedActivityStack2;
                TaskRecord taskRecordCreateTaskRecord = task.getStack().createTaskRecord(getNextTaskIdForUserLocked(activityRecord.userId), activityRecord.info, activityRecord.intent, null, null, true);
                activityRecord.reparent(taskRecordCreateTaskRecord, Integer.MAX_VALUE, "moveActivityToStack");
                taskRecordCreateTaskRecord.reparent((ActivityStack) pinnedActivityStack, true, 0, false, true, false, str);
            }
            activityRecord.supportsEnterPipOnTaskSwitch = false;
            this.mWindowManager.continueSurfaceLayout();
            pinnedActivityStack.animateResizePinnedStack(rect, rect2, -1, true);
            ensureActivitiesVisibleLocked(null, 0, false);
            resumeFocusedStackTopActivityLocked();
            this.mService.mTaskChangeNotificationController.notifyActivityPinned(activityRecord);
        } catch (Throwable th) {
            this.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    boolean moveFocusableActivityStackToFrontLocked(ActivityRecord activityRecord, String str) {
        if (activityRecord == null || !activityRecord.isFocusable()) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "moveActivityStackToFront: unfocusable r=" + activityRecord);
            }
            return false;
        }
        TaskRecord task = activityRecord.getTask();
        ActivityStack stack = activityRecord.getStack();
        if (stack == null) {
            Slog.w(TAG, "moveActivityStackToFront: invalid task or stack: r=" + activityRecord + " task=" + task);
            return false;
        }
        if (stack == this.mFocusedStack && stack.topRunningActivityLocked() == activityRecord) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
                Slog.d(TAG_FOCUS, "moveActivityStackToFront: already on top, r=" + activityRecord);
            }
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_FOCUS) {
            Slog.d(TAG_FOCUS, "moveActivityStackToFront: r=" + activityRecord);
        }
        stack.moveToFront(str, task);
        return true;
    }

    ActivityRecord findTaskLocked(ActivityRecord activityRecord, int i) {
        ActivityRecord activityRecord2 = null;
        this.mTmpFindTaskResult.r = null;
        this.mTmpFindTaskResult.matchedByRootAffinity = false;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG_TASKS, "Looking for task of " + activityRecord);
        }
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (!activityRecord.hasCompatibleActivityType(childAt)) {
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Skipping stack: (mismatch activity/stack) " + childAt);
                    }
                } else {
                    childAt.findTaskLocked(activityRecord, this.mTmpFindTaskResult);
                    if (this.mTmpFindTaskResult.r == null) {
                        continue;
                    } else {
                        if (!this.mTmpFindTaskResult.matchedByRootAffinity) {
                            return this.mTmpFindTaskResult.r;
                        }
                        if (this.mTmpFindTaskResult.r.getDisplayId() == i) {
                            activityRecord2 = this.mTmpFindTaskResult.r;
                        } else if (ActivityManagerDebugConfig.DEBUG_TASKS && this.mTmpFindTaskResult.matchedByRootAffinity) {
                            Slog.d(TAG_TASKS, "Skipping match on different display " + this.mTmpFindTaskResult.r.getDisplayId() + " " + i);
                        }
                    }
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TASKS && activityRecord2 == null) {
            Slog.d(TAG_TASKS, "No task found");
        }
        return activityRecord2;
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo activityInfo, boolean z) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityRecord activityRecordFindActivityLocked = activityDisplayValueAt.getChildAt(childCount).findActivityLocked(intent, activityInfo, z);
                if (activityRecordFindActivityLocked != null) {
                    return activityRecordFindActivityLocked;
                }
            }
        }
        return null;
    }

    boolean hasAwakeDisplay() {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            if (!this.mActivityDisplays.valueAt(size).shouldSleep()) {
                return true;
            }
        }
        return false;
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
        applySleepTokensLocked(false);
        checkReadyForSleepLocked(true);
    }

    void prepareForShutdownLocked() {
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            createSleepTokenLocked("shutdown", this.mActivityDisplays.keyAt(i));
        }
    }

    boolean shutdownLocked(int i) {
        boolean z;
        goingToSleepLocked();
        long jCurrentTimeMillis = System.currentTimeMillis() + ((long) i);
        while (true) {
            z = true;
            if (!putStacksToSleepLocked(true, true)) {
                long jCurrentTimeMillis2 = jCurrentTimeMillis - System.currentTimeMillis();
                if (jCurrentTimeMillis2 > 0) {
                    try {
                        this.mService.wait(jCurrentTimeMillis2);
                    } catch (InterruptedException e) {
                    }
                } else {
                    Slog.w(TAG, "Activity manager shutdown timed out");
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        checkReadyForSleepLocked(false);
        return z;
    }

    void comeOutOfSleepIfNeededLocked() {
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
    }

    void applySleepTokensLocked(boolean z) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            boolean zShouldSleep = activityDisplayValueAt.shouldSleep();
            if (zShouldSleep != activityDisplayValueAt.isSleeping()) {
                activityDisplayValueAt.setIsSleeping(zShouldSleep);
                if (z) {
                    for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                        ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                        if (zShouldSleep) {
                            childAt.goToSleepIfPossible(false);
                        } else {
                            childAt.awakeFromSleepingLocked();
                            if (isFocusedStack(childAt) && !getKeyguardController().isKeyguardOrAodShowing(activityDisplayValueAt.mDisplayId)) {
                                resumeFocusedStackTopActivityLocked();
                            }
                        }
                    }
                    if (!zShouldSleep && !this.mGoingToSleepActivities.isEmpty()) {
                        Iterator<ActivityRecord> it = this.mGoingToSleepActivities.iterator();
                        while (it.hasNext()) {
                            if (it.next().getDisplayId() == activityDisplayValueAt.mDisplayId) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    void activitySleptLocked(ActivityRecord activityRecord) {
        this.mGoingToSleepActivities.remove(activityRecord);
        ActivityStack stack = activityRecord.getStack();
        if (stack != null) {
            stack.checkReadyForSleep();
        } else {
            checkReadyForSleepLocked(true);
        }
    }

    void checkReadyForSleepLocked(boolean z) {
        if (!this.mService.isSleepingOrShuttingDownLocked() || !putStacksToSleepLocked(z, false)) {
            return;
        }
        sendPowerHintForLaunchEndIfNeeded();
        removeSleepTimeouts();
        if (this.mGoingToSleep.isHeld()) {
            this.mGoingToSleep.release();
        }
        if (this.mService.mShuttingDown) {
            this.mService.notifyAll();
        }
    }

    private boolean putStacksToSleepLocked(boolean z, boolean z2) {
        boolean zGoToSleepIfPossible = true;
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (z) {
                    zGoToSleepIfPossible &= childAt.goToSleepIfPossible(z2);
                } else {
                    childAt.goToSleep();
                }
            }
        }
        return zGoToSleepIfPossible;
    }

    boolean reportResumedActivityLocked(ActivityRecord activityRecord) {
        this.mStoppingActivities.remove(activityRecord);
        if (isFocusedStack(activityRecord.getStack())) {
            this.mService.updateUsageStats(activityRecord, true);
        }
        if (!allResumedActivitiesComplete()) {
            return false;
        }
        ensureActivitiesVisibleLocked(null, 0, false);
        this.mWindowManager.executeAppTransition();
        return true;
    }

    void handleAppCrashLocked(ProcessRecord processRecord) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).handleAppCrashLocked(processRecord);
            }
        }
    }

    private void handleLaunchTaskBehindCompleteLocked(ActivityRecord activityRecord) {
        TaskRecord task = activityRecord.getTask();
        ActivityStack stack = task.getStack();
        activityRecord.mLaunchTaskBehind = false;
        this.mRecentTasks.add(task);
        this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
        activityRecord.setVisibility(false);
        ActivityRecord topActivity = stack.getTopActivity();
        if (topActivity != null) {
            topActivity.getTask().touchActiveTime();
        }
    }

    void scheduleLaunchTaskBehindComplete(IBinder iBinder) {
        this.mHandler.obtainMessage(112, iBinder).sendToTarget();
    }

    void ensureActivitiesVisibleLocked(ActivityRecord activityRecord, int i, boolean z) {
        ensureActivitiesVisibleLocked(activityRecord, i, z, true);
    }

    void ensureActivitiesVisibleLocked(ActivityRecord activityRecord, int i, boolean z, boolean z2) {
        getKeyguardController().beginActivityVisibilityUpdate();
        try {
            for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
                ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
                for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                    activityDisplayValueAt.getChildAt(childCount).ensureActivitiesVisibleLocked(activityRecord, i, z, z2);
                }
            }
        } finally {
            getKeyguardController().endActivityVisibilityUpdate();
        }
    }

    void addStartingWindowsForVisibleActivities(boolean z) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).addStartingWindowsForVisibleActivities(z);
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
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            int iRankTaskLayers = 0;
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                iRankTaskLayers += activityDisplayValueAt.getChildAt(childCount).rankTaskLayers(iRankTaskLayers);
            }
        }
    }

    void clearOtherAppTimeTrackers(AppTimeTracker appTimeTracker) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).clearOtherAppTimeTrackers(appTimeTracker);
            }
        }
    }

    void scheduleDestroyAllActivities(ProcessRecord processRecord, String str) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).scheduleDestroyActivities(processRecord, str);
            }
        }
    }

    void releaseSomeActivitiesLocked(ProcessRecord processRecord, String str) {
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + processRecord);
        }
        ArraySet<TaskRecord> arraySet = null;
        TaskRecord taskRecord = null;
        for (int i = 0; i < processRecord.activities.size(); i++) {
            ActivityRecord activityRecord = processRecord.activities.get(i);
            if (activityRecord.finishing || activityRecord.isState(ActivityStack.ActivityState.DESTROYING, ActivityStack.ActivityState.DESTROYED)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Abort release; already destroying: " + activityRecord);
                    return;
                }
                return;
            }
            if (activityRecord.visible || !activityRecord.stopped || !activityRecord.haveState || activityRecord.isState(ActivityStack.ActivityState.RESUMED, ActivityStack.ActivityState.PAUSING, ActivityStack.ActivityState.PAUSED, ActivityStack.ActivityState.STOPPING)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Not releasing in-use activity: " + activityRecord);
                }
            } else {
                TaskRecord task = activityRecord.getTask();
                if (task != null) {
                    if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                        Slog.d(TAG_RELEASE, "Collecting release task " + task + " from " + activityRecord);
                    }
                    if (taskRecord == null) {
                        taskRecord = task;
                    } else if (taskRecord != task) {
                        if (arraySet == null) {
                            arraySet = new ArraySet<>();
                            arraySet.add(taskRecord);
                        }
                        arraySet.add(task);
                    }
                }
            }
        }
        if (arraySet == null) {
            if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                Slog.d(TAG_RELEASE, "Didn't find two or more tasks to release");
                return;
            }
            return;
        }
        int size = this.mActivityDisplays.size();
        for (int i2 = 0; i2 < size; i2++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i2);
            int childCount = activityDisplayValueAt.getChildCount();
            for (int i3 = 0; i3 < childCount; i3++) {
                if (activityDisplayValueAt.getChildAt(i3).releaseSomeActivitiesLocked(processRecord, arraySet, str) > 0) {
                    return;
                }
            }
        }
    }

    boolean switchUserLocked(int i, UserState userState) {
        int stackId = this.mFocusedStack.getStackId();
        ActivityStack splitScreenPrimaryStack = getDefaultDisplay().getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack != null) {
            moveTasksToFullscreenStackLocked(splitScreenPrimaryStack, this.mFocusedStack == splitScreenPrimaryStack);
        }
        removeStacksInWindowingModes(2);
        this.mUserStackInFront.put(this.mCurrentUser, stackId);
        int i2 = this.mUserStackInFront.get(i, this.mHomeStack.mStackId);
        this.mCurrentUser = i;
        this.mStartingUsers.add(userState);
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                childAt.switchUserLocked(i);
                TaskRecord taskRecord = childAt.topTask();
                if (taskRecord != null) {
                    childAt.positionChildWindowContainerAtTop(taskRecord);
                }
            }
        }
        ActivityStack stack = getStack(i2);
        if (stack == null) {
            stack = this.mHomeStack;
        }
        boolean zIsActivityTypeHome = stack.isActivityTypeHome();
        if (stack.isOnHomeDisplay()) {
            stack.moveToFront("switchUserOnHomeDisplay");
        } else {
            resumeHomeStackTask(null, "switchUserOnOtherDisplay");
        }
        return zIsActivityTypeHome;
    }

    boolean isCurrentProfileLocked(int i) {
        if (i == this.mCurrentUser) {
            return true;
        }
        return this.mService.mUserController.isCurrentProfile(i);
    }

    boolean isStoppingNoHistoryActivity() {
        Iterator<ActivityRecord> it = this.mStoppingActivities.iterator();
        while (it.hasNext()) {
            if (it.next().isNoHistory()) {
                return true;
            }
        }
        return false;
    }

    final ArrayList<ActivityRecord> processStoppingActivitiesLocked(ActivityRecord activityRecord, boolean z, boolean z2) {
        boolean zIsSleepingOrShuttingDownLocked;
        boolean zAllResumedActivitiesVisible = allResumedActivitiesVisible();
        ArrayList<ActivityRecord> arrayList = null;
        for (int size = this.mStoppingActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord2 = this.mStoppingActivities.get(size);
            boolean zContains = this.mActivitiesWaitingForVisibleActivity.contains(activityRecord2);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG, "Stopping " + activityRecord2 + ": nowVisible=" + zAllResumedActivitiesVisible + " waitingVisible=" + zContains + " finishing=" + activityRecord2.finishing);
            }
            if (zContains && zAllResumedActivitiesVisible) {
                this.mActivitiesWaitingForVisibleActivity.remove(activityRecord2);
                if (activityRecord2.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.v(TAG, "Before stopping, can hide: " + activityRecord2);
                    }
                    activityRecord2.setVisibility(false);
                }
                zContains = false;
            }
            if (z) {
                ActivityStack stack = activityRecord2.getStack();
                if (stack != null) {
                    zIsSleepingOrShuttingDownLocked = stack.shouldSleepOrShutDownActivities();
                } else {
                    zIsSleepingOrShuttingDownLocked = this.mService.isSleepingOrShuttingDownLocked();
                }
                if (!zContains || zIsSleepingOrShuttingDownLocked) {
                    if (!z2 && activityRecord2.isState(ActivityStack.ActivityState.PAUSING)) {
                        removeTimeoutsForActivityLocked(activityRecord);
                        scheduleIdleTimeoutLocked(activityRecord);
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_STATES) {
                            Slog.v(TAG, "Ready to stop: " + activityRecord2);
                        }
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                        }
                        arrayList.add(activityRecord2);
                        this.mStoppingActivities.remove(size);
                    }
                }
            }
        }
        return arrayList;
    }

    void validateTopActivitiesLocked() {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                ActivityRecord activityRecord = childAt.topRunningActivityLocked();
                ActivityStack.ActivityState state = activityRecord == null ? ActivityStack.ActivityState.DESTROYED : activityRecord.getState();
                if (isFocusedStack(childAt)) {
                    if (activityRecord == null) {
                        Slog.e(TAG, "validateTop...: null top activity, stack=" + childAt);
                    } else {
                        ActivityRecord activityRecord2 = childAt.mPausingActivity;
                        if (activityRecord2 != null && activityRecord2 == activityRecord) {
                            Slog.e(TAG, "validateTop...: top stack has pausing activity r=" + activityRecord + " state=" + state);
                        }
                        if (state != ActivityStack.ActivityState.INITIALIZING && state != ActivityStack.ActivityState.RESUMED) {
                            Slog.e(TAG, "validateTop...: activity in front not resumed r=" + activityRecord + " state=" + state);
                        }
                    }
                } else {
                    ActivityRecord resumedActivity = childAt.getResumedActivity();
                    if (resumedActivity != null && resumedActivity == activityRecord) {
                        Slog.e(TAG, "validateTop...: back stack has resumed activity r=" + activityRecord + " state=" + state);
                    }
                    if (activityRecord != null && (state == ActivityStack.ActivityState.INITIALIZING || state == ActivityStack.ActivityState.RESUMED)) {
                        Slog.e(TAG, "validateTop...: activity in back resumed r=" + activityRecord + " state=" + state);
                    }
                }
            }
        }
    }

    public void dumpDisplays(PrintWriter printWriter) {
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size += -1) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            printWriter.print("[id:" + activityDisplayValueAt.mDisplayId + " stacks:");
            activityDisplayValueAt.dumpStacks(printWriter);
            printWriter.print("]");
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mFocusedStack=" + this.mFocusedStack);
        printWriter.print(" mLastFocusedStack=");
        printWriter.println(this.mLastFocusedStack);
        printWriter.print(str);
        printWriter.println("mCurTaskIdForUser=" + this.mCurTaskIdForUser);
        printWriter.print(str);
        printWriter.println("mUserStackInFront=" + this.mUserStackInFront);
        for (int size = this.mActivityDisplays.size() + (-1); size >= 0; size--) {
            this.mActivityDisplays.valueAt(size).dump(printWriter, str);
        }
        if (!this.mWaitingForActivityVisible.isEmpty()) {
            printWriter.print(str);
            printWriter.println("mWaitingForActivityVisible=");
            for (int i = 0; i < this.mWaitingForActivityVisible.size(); i++) {
                printWriter.print(str);
                printWriter.print(str);
                this.mWaitingForActivityVisible.get(i).dump(printWriter, str);
            }
        }
        printWriter.print(str);
        printWriter.print("isHomeRecentsComponent=");
        printWriter.print(this.mRecentTasks.isRecentsComponentHomeActivity(this.mCurrentUser));
        getKeyguardController().dump(printWriter, str);
        this.mService.getLockTaskController().dump(printWriter, str);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, false);
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            this.mActivityDisplays.valueAt(i).writeToProto(protoOutputStream, 2246267895810L);
        }
        getKeyguardController().writeToProto(protoOutputStream, 1146756268035L);
        if (this.mFocusedStack != null) {
            protoOutputStream.write(1120986464260L, this.mFocusedStack.mStackId);
            ActivityRecord resumedActivityLocked = getResumedActivityLocked();
            if (resumedActivityLocked != null) {
                resumedActivityLocked.writeIdentifierToProto(protoOutputStream, 1146756268037L);
            }
        } else {
            protoOutputStream.write(1120986464260L, -1);
        }
        protoOutputStream.write(1133871366150L, this.mRecentTasks.isRecentsComponentHomeActivity(this.mCurrentUser));
        protoOutputStream.end(jStart);
    }

    void dumpDisplayConfigs(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.println("Display override configurations:");
        int size = this.mActivityDisplays.size();
        for (int i = 0; i < size; i++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            printWriter.print(str);
            printWriter.print("  ");
            printWriter.print(activityDisplayValueAt.mDisplayId);
            printWriter.print(": ");
            printWriter.println(activityDisplayValueAt.getOverrideConfiguration());
        }
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String str, boolean z, boolean z2) {
        if (z2) {
            return this.mFocusedStack.getDumpActivitiesLocked(str);
        }
        ArrayList<ActivityRecord> arrayList = new ArrayList<>();
        int size = this.mActivityDisplays.size();
        for (int i = 0; i < size; i++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (!z || childAt.shouldBeVisible(null)) {
                    arrayList.addAll(childAt.getDumpActivitiesLocked(str));
                }
            }
        }
        return arrayList;
    }

    static boolean printThisActivity(PrintWriter printWriter, ActivityRecord activityRecord, String str, boolean z, String str2) {
        if (activityRecord != null) {
            if (str == null || str.equals(activityRecord.packageName)) {
                if (z) {
                    printWriter.println();
                }
                printWriter.print(str2);
                printWriter.println(activityRecord);
                return true;
            }
            return false;
        }
        return false;
    }

    boolean dumpActivitiesLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, boolean z, boolean z2, String str) {
        boolean z3;
        boolean z4 = false;
        int i = 0;
        boolean z5 = false;
        while (i < this.mActivityDisplays.size()) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            printWriter.print("Display #");
            printWriter.print(activityDisplayValueAt.mDisplayId);
            printWriter.println(" (activities from top to bottom):");
            ActivityDisplay activityDisplayValueAt2 = this.mActivityDisplays.valueAt(i);
            boolean zPrintThisActivity = z4;
            int childCount = activityDisplayValueAt2.getChildCount() - 1;
            while (childCount >= 0) {
                ActivityStack childAt = activityDisplayValueAt2.getChildAt(childCount);
                printWriter.println();
                printWriter.println("  Stack #" + childAt.mStackId + ": type=" + WindowConfiguration.activityTypeToString(childAt.getActivityType()) + " mode=" + WindowConfiguration.windowingModeToString(childAt.getWindowingMode()));
                StringBuilder sb = new StringBuilder();
                sb.append("  isSleeping=");
                sb.append(childAt.shouldSleepActivities());
                printWriter.println(sb.toString());
                printWriter.println("  mBounds=" + childAt.getOverrideBounds());
                int i2 = childCount;
                ActivityDisplay activityDisplay = activityDisplayValueAt2;
                int i3 = i;
                boolean zDumpActivitiesLocked = z5 | childAt.dumpActivitiesLocked(fileDescriptor, printWriter, z, z2, str, zPrintThisActivity) | dumpHistoryList(fileDescriptor, printWriter, childAt.mLRUActivities, "    ", "Run", false, !z, false, str, true, "    Running activities (most recent first):", null);
                if (printThisActivity(printWriter, childAt.mPausingActivity, str, zDumpActivitiesLocked, "    mPausingActivity: ")) {
                    zDumpActivitiesLocked = false;
                    z3 = true;
                } else {
                    z3 = zDumpActivitiesLocked;
                }
                if (printThisActivity(printWriter, childAt.getResumedActivity(), str, zDumpActivitiesLocked, "    mResumedActivity: ")) {
                    zDumpActivitiesLocked = false;
                    z3 = true;
                }
                if (z) {
                    if (printThisActivity(printWriter, childAt.mLastPausedActivity, str, zDumpActivitiesLocked, "    mLastPausedActivity: ")) {
                        zDumpActivitiesLocked = true;
                        z3 = true;
                    }
                    zPrintThisActivity = printThisActivity(printWriter, childAt.mLastNoHistoryActivity, str, zDumpActivitiesLocked, "    mLastNoHistoryActivity: ") | z3;
                } else {
                    zPrintThisActivity = z3;
                }
                childCount = i2 - 1;
                z5 = zPrintThisActivity;
                activityDisplayValueAt2 = activityDisplay;
                i = i3;
            }
            i++;
            z4 = zPrintThisActivity;
        }
        return dumpHistoryList(fileDescriptor, printWriter, this.mGoingToSleepActivities, "  ", "Sleep", false, !z, false, str, true, "  Activities waiting to sleep:", null) | z5 | dumpHistoryList(fileDescriptor, printWriter, this.mFinishingActivities, "  ", "Fin", false, !z, false, str, true, "  Activities waiting to finish:", null) | dumpHistoryList(fileDescriptor, printWriter, this.mStoppingActivities, "  ", "Stop", false, !z, false, str, true, "  Activities waiting to stop:", null) | dumpHistoryList(fileDescriptor, printWriter, this.mActivitiesWaitingForVisibleActivity, "  ", "Wait", false, !z, false, str, true, "  Activities waiting for another to become visible:", null);
    }

    static boolean dumpHistoryList(FileDescriptor fileDescriptor, PrintWriter printWriter, List<ActivityRecord> list, String str, String str2, boolean z, boolean z2, boolean z3, String str3, boolean z4, String str4, TaskRecord taskRecord) {
        String str5 = str;
        int i = 0;
        ?? r10 = z4;
        String str6 = str4;
        TaskRecord task = taskRecord;
        int size = list.size() - 1;
        boolean z5 = false;
        String str7 = null;
        String[] strArr = null;
        while (size >= 0) {
            ActivityRecord activityRecord = list.get(size);
            if (str3 == null || str3.equals(activityRecord.packageName)) {
                if (str7 == null) {
                    str7 = str5 + "      ";
                    strArr = new String[i];
                }
                int i2 = (z2 || (!z && activityRecord.isInHistory())) ? i : 1;
                if (r10 != 0) {
                    printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    r10 = i;
                }
                if (str6 != null) {
                    printWriter.println(str6);
                    str6 = null;
                }
                if (task != activityRecord.getTask()) {
                    task = activityRecord.getTask();
                    printWriter.print(str5);
                    printWriter.print(i2 != 0 ? "* " : "  ");
                    printWriter.println(task);
                    if (i2 != 0) {
                        task.dump(printWriter, str5 + "  ");
                    } else if (z && task.intent != null) {
                        printWriter.print(str5);
                        printWriter.print("  ");
                        printWriter.println(task.intent.toInsecureStringWithClip());
                    }
                }
                printWriter.print(str5);
                printWriter.print(i2 != 0 ? "  * " : "    ");
                printWriter.print(str2);
                printWriter.print(" #");
                printWriter.print(size);
                printWriter.print(": ");
                printWriter.println(activityRecord);
                if (i2 != 0) {
                    activityRecord.dump(printWriter, str7);
                } else if (z) {
                    printWriter.print(str7);
                    printWriter.println(activityRecord.intent.toInsecureString());
                    if (activityRecord.app != null) {
                        printWriter.print(str7);
                        printWriter.println(activityRecord.app);
                    }
                }
                if (!z3 || activityRecord.app == null || activityRecord.app.thread == null) {
                    z5 = true;
                } else {
                    printWriter.flush();
                    try {
                        TransferPipe transferPipe = new TransferPipe();
                        try {
                            activityRecord.app.thread.dumpActivity(transferPipe.getWriteFd(), activityRecord.appToken, str7, strArr);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            transferPipe.go(fileDescriptor, 2000L);
                            try {
                                transferPipe.kill();
                            } catch (RemoteException e) {
                                printWriter.println(str7 + "Got a RemoteException while dumping the activity");
                            } catch (IOException e2) {
                                e = e2;
                                printWriter.println(str7 + "Failure while dumping the activity: " + e);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            transferPipe.kill();
                            throw th;
                        }
                    } catch (RemoteException e3) {
                    } catch (IOException e4) {
                        e = e4;
                    }
                    z5 = true;
                    r10 = 1;
                }
            }
            size--;
            str5 = str;
            i = 0;
            r10 = r10;
        }
        return z5;
    }

    void scheduleIdleTimeoutLocked(ActivityRecord activityRecord) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "scheduleIdleTimeoutLocked: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, activityRecord), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    final void scheduleIdleLocked() {
        this.mHandler.sendEmptyMessage(101);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord activityRecord) {
        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "removeTimeoutsForActivity: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.removeMessages(100, activityRecord);
    }

    final void scheduleResumeTopActivities() {
        if (!this.mHandler.hasMessages(102)) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    void removeSleepTimeouts() {
        this.mHandler.removeMessages(103);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(103, 5000L);
    }

    @Override
    public void onDisplayAdded(int i) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display added displayId=" + i);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(105, i, 0));
    }

    @Override
    public void onDisplayRemoved(int i) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display removed displayId=" + i);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(107, i, 0));
    }

    @Override
    public void onDisplayChanged(int i) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.v(TAG, "Display changed displayId=" + i);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(106, i, 0));
    }

    private void handleDisplayAdded(int i) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                getActivityDisplayOrCreateLocked(i);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    boolean isDisplayAdded(int i) {
        return getActivityDisplayOrCreateLocked(i) != null;
    }

    ActivityDisplay getActivityDisplay(int i) {
        return this.mActivityDisplays.get(i);
    }

    ActivityDisplay getDefaultDisplay() {
        return this.mActivityDisplays.get(0);
    }

    ActivityDisplay getActivityDisplayOrCreateLocked(int i) {
        Display display;
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(i);
        if (activityDisplay != null) {
            return activityDisplay;
        }
        if (this.mDisplayManager == null || (display = this.mDisplayManager.getDisplay(i)) == null) {
            return null;
        }
        ActivityDisplay activityDisplay2 = new ActivityDisplay(this, display);
        attachDisplay(activityDisplay2);
        calculateDefaultMinimalSizeOfResizeableTasks(activityDisplay2);
        this.mWindowManager.onDisplayAdded(i);
        return activityDisplay2;
    }

    @VisibleForTesting
    void attachDisplay(ActivityDisplay activityDisplay) {
        this.mActivityDisplays.put(activityDisplay.mDisplayId, activityDisplay);
    }

    private void calculateDefaultMinimalSizeOfResizeableTasks(ActivityDisplay activityDisplay) {
        this.mDefaultMinSizeOfResizeableTask = this.mService.mContext.getResources().getDimensionPixelSize(R.dimen.button_padding_horizontal_material);
    }

    private void handleDisplayRemoved(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Can't remove the primary display.");
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = this.mActivityDisplays.get(i);
                if (activityDisplay == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                activityDisplay.remove();
                releaseSleepTokens(activityDisplay);
                this.mActivityDisplays.remove(i);
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void handleDisplayChanged(int i) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay = this.mActivityDisplays.get(i);
                if (activityDisplay != null) {
                    if (i != 0) {
                        int state = activityDisplay.mDisplay.getState();
                        if (state == 1 && activityDisplay.mOffToken == null) {
                            activityDisplay.mOffToken = this.mService.acquireSleepToken("Display-off", i);
                        } else if (state == 2 && activityDisplay.mOffToken != null) {
                            activityDisplay.mOffToken.release();
                            activityDisplay.mOffToken = null;
                        }
                    }
                    activityDisplay.updateBounds();
                }
                this.mWindowManager.onDisplayChanged(i);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    ActivityManagerInternal.SleepToken createSleepTokenLocked(String str, int i) {
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(i);
        if (activityDisplay == null) {
            throw new IllegalArgumentException("Invalid display: " + i);
        }
        SleepTokenImpl sleepTokenImpl = new SleepTokenImpl(str, i);
        this.mSleepTokens.add(sleepTokenImpl);
        activityDisplay.mAllSleepTokens.add(sleepTokenImpl);
        return sleepTokenImpl;
    }

    private void removeSleepTokenLocked(SleepTokenImpl sleepTokenImpl) {
        this.mSleepTokens.remove(sleepTokenImpl);
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(sleepTokenImpl.mDisplayId);
        if (activityDisplay != null) {
            activityDisplay.mAllSleepTokens.remove(sleepTokenImpl);
            if (activityDisplay.mAllSleepTokens.isEmpty()) {
                this.mService.updateSleepIfNeededLocked();
            }
        }
    }

    private void releaseSleepTokens(ActivityDisplay activityDisplay) {
        if (activityDisplay.mAllSleepTokens.isEmpty()) {
            return;
        }
        Iterator<ActivityManagerInternal.SleepToken> it = activityDisplay.mAllSleepTokens.iterator();
        while (it.hasNext()) {
            this.mSleepTokens.remove(it.next());
        }
        activityDisplay.mAllSleepTokens.clear();
        this.mService.updateSleepIfNeededLocked();
    }

    private ActivityManager.StackInfo getStackInfo(ActivityStack activityStack) {
        String strFlattenToString;
        int i = activityStack.mDisplayId;
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(i);
        ActivityManager.StackInfo stackInfo = new ActivityManager.StackInfo();
        activityStack.getWindowContainerBounds(stackInfo.bounds);
        stackInfo.displayId = i;
        stackInfo.stackId = activityStack.mStackId;
        stackInfo.userId = activityStack.mCurrentUser;
        stackInfo.visible = activityStack.shouldBeVisible(null);
        stackInfo.position = activityDisplay != null ? activityDisplay.getIndexOf(activityStack) : 0;
        stackInfo.configuration.setTo(activityStack.getConfiguration());
        ArrayList<TaskRecord> allTasks = activityStack.getAllTasks();
        int size = allTasks.size();
        int[] iArr = new int[size];
        String[] strArr = new String[size];
        Rect[] rectArr = new Rect[size];
        int[] iArr2 = new int[size];
        for (int i2 = 0; i2 < size; i2++) {
            TaskRecord taskRecord = allTasks.get(i2);
            iArr[i2] = taskRecord.taskId;
            if (taskRecord.origActivity != null) {
                strFlattenToString = taskRecord.origActivity.flattenToString();
            } else if (taskRecord.realActivity != null) {
                strFlattenToString = taskRecord.realActivity.flattenToString();
            } else {
                strFlattenToString = taskRecord.getTopActivity() != null ? taskRecord.getTopActivity().packageName : UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            strArr[i2] = strFlattenToString;
            rectArr[i2] = new Rect();
            taskRecord.getWindowContainerBounds(rectArr[i2]);
            iArr2[i2] = taskRecord.userId;
        }
        stackInfo.taskIds = iArr;
        stackInfo.taskNames = strArr;
        stackInfo.taskBounds = rectArr;
        stackInfo.taskUserIds = iArr2;
        ActivityRecord activityRecord = activityStack.topRunningActivityLocked();
        stackInfo.topActivity = activityRecord != null ? activityRecord.intent.getComponent() : null;
        return stackInfo;
    }

    ActivityManager.StackInfo getStackInfo(int i) {
        ActivityStack stack = getStack(i);
        if (stack != null) {
            return getStackInfo(stack);
        }
        return null;
    }

    ActivityManager.StackInfo getStackInfo(int i, int i2) {
        ActivityStack stack = getStack(i, i2);
        if (stack != null) {
            return getStackInfo(stack);
        }
        return null;
    }

    ArrayList<ActivityManager.StackInfo> getAllStackInfosLocked() {
        ArrayList<ActivityManager.StackInfo> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mActivityDisplays.size(); i++) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(i);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                arrayList.add(getStackInfo(activityDisplayValueAt.getChildAt(childCount)));
            }
        }
        return arrayList;
    }

    void handleNonResizableTaskIfNeeded(TaskRecord taskRecord, int i, int i2, ActivityStack activityStack) {
        handleNonResizableTaskIfNeeded(taskRecord, i, i2, activityStack, false);
    }

    void handleNonResizableTaskIfNeeded(TaskRecord taskRecord, int i, int i2, ActivityStack activityStack, boolean z) {
        boolean z2 = (i2 == 0 || i2 == -1) ? false : true;
        if ((!(activityStack != null && activityStack.getDisplay().hasSplitScreenPrimaryStack()) && i != 3 && !z2) || !taskRecord.isActivityTypeStandardOrUndefined()) {
            return;
        }
        if (z2) {
            int i3 = taskRecord.getStack().mDisplayId;
            if (!taskRecord.canBeLaunchedOnDisplay(i3)) {
                throw new IllegalStateException("Task resolved to incompatible display");
            }
            this.mService.setTaskWindowingMode(taskRecord.taskId, 4, true);
            if (i2 != i3) {
                this.mService.mTaskChangeNotificationController.notifyActivityLaunchOnSecondaryDisplayFailed();
                return;
            }
        }
        if (!taskRecord.supportsSplitScreenWindowingMode() || z) {
            this.mService.mTaskChangeNotificationController.notifyActivityDismissingDockedStack();
            ActivityStack splitScreenPrimaryStack = taskRecord.getStack().getDisplay().getSplitScreenPrimaryStack();
            if (splitScreenPrimaryStack != null) {
                moveTasksToFullscreenStackLocked(splitScreenPrimaryStack, activityStack == splitScreenPrimaryStack);
                return;
            }
            return;
        }
        ActivityRecord topActivity = taskRecord.getTopActivity();
        if (topActivity != null && topActivity.isNonResizableOrForcedResizable() && !topActivity.noDisplay) {
            this.mService.mTaskChangeNotificationController.notifyActivityForcedResizable(taskRecord.taskId, z2 ? 2 : 1, topActivity.appInfo.packageName);
        }
    }

    void activityRelaunchedLocked(IBinder iBinder) {
        this.mWindowManager.notifyAppRelaunchingFinished(iBinder);
        ActivityRecord activityRecordIsInStackLocked = ActivityRecord.isInStackLocked(iBinder);
        if (activityRecordIsInStackLocked != null && activityRecordIsInStackLocked.getStack().shouldSleepOrShutDownActivities()) {
            activityRecordIsInStackLocked.setSleeping(true, true);
        }
    }

    void activityRelaunchingLocked(ActivityRecord activityRecord) {
        this.mWindowManager.notifyAppRelaunching(activityRecord.appToken);
    }

    void logStackState() {
        this.mActivityMetricsLogger.logWindowState();
    }

    void scheduleUpdateMultiWindowMode(TaskRecord taskRecord) {
        if (taskRecord.getStack().deferScheduleMultiWindowModeChanged()) {
            return;
        }
        for (int size = taskRecord.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = taskRecord.mActivities.get(size);
            if (activityRecord.app != null && activityRecord.app.thread != null) {
                this.mMultiWindowModeChangedActivities.add(activityRecord);
            }
        }
        if (!this.mHandler.hasMessages(114)) {
            this.mHandler.sendEmptyMessage(114);
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord taskRecord, ActivityStack activityStack) {
        ActivityStack stack = taskRecord.getStack();
        if (activityStack != null && activityStack != stack) {
            if (!activityStack.inPinnedWindowingMode() && !stack.inPinnedWindowingMode()) {
                return;
            }
            scheduleUpdatePictureInPictureModeIfNeeded(taskRecord, stack.getOverrideBounds());
        }
    }

    void scheduleUpdatePictureInPictureModeIfNeeded(TaskRecord taskRecord, Rect rect) {
        for (int size = taskRecord.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = taskRecord.mActivities.get(size);
            if (activityRecord.app != null && activityRecord.app.thread != null) {
                this.mPipModeChangedActivities.add(activityRecord);
                this.mMultiWindowModeChangedActivities.remove(activityRecord);
            }
        }
        this.mPipModeChangedTargetStackBounds = rect;
        if (!this.mHandler.hasMessages(115)) {
            this.mHandler.sendEmptyMessage(115);
        }
    }

    void updatePictureInPictureMode(TaskRecord taskRecord, Rect rect, boolean z) {
        this.mHandler.removeMessages(115);
        for (int size = taskRecord.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = taskRecord.mActivities.get(size);
            if (activityRecord.app != null && activityRecord.app.thread != null) {
                activityRecord.updatePictureInPictureMode(rect, z);
            }
        }
    }

    void setDockedStackMinimized(boolean z) {
        this.mIsDockMinimized = z;
        if (this.mIsDockMinimized) {
            ActivityStack focusedStack = getFocusedStack();
            if (focusedStack.inSplitScreenPrimaryWindowingMode()) {
                focusedStack.adjustFocusToNextFocusableStack("setDockedStackMinimized");
            }
        }
    }

    void wakeUp(String str) {
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server.am:TURN_ON:" + str);
    }

    private void beginDeferResume() {
        this.mDeferResumeCount++;
    }

    private void endDeferResume() {
        this.mDeferResumeCount--;
    }

    private boolean readyToResume() {
        return this.mDeferResumeCount == 0;
    }

    private final class ActivityStackSupervisorHandler extends Handler {
        public ActivityStackSupervisorHandler(Looper looper) {
            super(looper);
        }

        void activityIdleInternal(ActivityRecord activityRecord, boolean z) {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.activityIdleInternalLocked(activityRecord != null ? activityRecord.appToken : null, true, z, null);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 112) {
                switch (i) {
                    case 100:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_TIMEOUT_MSG: r=" + message.obj);
                        }
                        activityIdleInternal((ActivityRecord) message.obj, true);
                        return;
                    case 101:
                        if (ActivityManagerDebugConfig.DEBUG_IDLE) {
                            Slog.d(ActivityStackSupervisor.TAG_IDLE, "handleMessage: IDLE_NOW_MSG: r=" + message.obj);
                        }
                        activityIdleInternal((ActivityRecord) message.obj, false);
                        return;
                    case 102:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ActivityStackSupervisor.this.resumeFocusedStackTopActivityLocked();
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    case 103:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityStackSupervisor.this.mService.isSleepingOrShuttingDownLocked()) {
                                    Slog.w(ActivityStackSupervisor.TAG, "Sleep timeout!  Sleeping now.");
                                    ActivityStackSupervisor.this.checkReadyForSleepLocked(false);
                                }
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    case 104:
                        synchronized (ActivityStackSupervisor.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (ActivityStackSupervisor.this.mLaunchingActivity.isHeld()) {
                                    Slog.w(ActivityStackSupervisor.TAG, "Launch timeout has expired, giving up wake lock!");
                                    ActivityStackSupervisor.this.mLaunchingActivity.release();
                                }
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    case 105:
                        ActivityStackSupervisor.this.handleDisplayAdded(message.arg1);
                        return;
                    case 106:
                        ActivityStackSupervisor.this.handleDisplayChanged(message.arg1);
                        return;
                    case 107:
                        ActivityStackSupervisor.this.handleDisplayRemoved(message.arg1);
                        return;
                    default:
                        switch (i) {
                            case 114:
                                synchronized (ActivityStackSupervisor.this.mService) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        for (int size = ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.size() - 1; size >= 0; size--) {
                                            ActivityStackSupervisor.this.mMultiWindowModeChangedActivities.remove(size).updateMultiWindowMode();
                                        }
                                    } finally {
                                    }
                                    break;
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return;
                            case 115:
                                synchronized (ActivityStackSupervisor.this.mService) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        for (int size2 = ActivityStackSupervisor.this.mPipModeChangedActivities.size() - 1; size2 >= 0; size2--) {
                                            ActivityStackSupervisor.this.mPipModeChangedActivities.remove(size2).updatePictureInPictureMode(ActivityStackSupervisor.this.mPipModeChangedTargetStackBounds, false);
                                        }
                                    } finally {
                                    }
                                    break;
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return;
                            default:
                                return;
                        }
                }
            }
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord activityRecordForTokenLocked = ActivityRecord.forTokenLocked((IBinder) message.obj);
                    if (activityRecordForTokenLocked != null) {
                        ActivityStackSupervisor.this.handleLaunchTaskBehindCompleteLocked(activityRecordForTokenLocked);
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    ActivityStack findStackBehind(ActivityStack activityStack) {
        ActivityDisplay activityDisplay = this.mActivityDisplays.get(0);
        if (activityDisplay == null) {
            return null;
        }
        for (int childCount = activityDisplay.getChildCount() - 1; childCount >= 0; childCount--) {
            if (activityDisplay.getChildAt(childCount) == activityStack && childCount > 0) {
                return activityDisplay.getChildAt(childCount - 1);
            }
        }
        throw new IllegalStateException("Failed to find a stack behind stack=" + activityStack + " in=" + activityDisplay);
    }

    void setResizingDuringAnimation(TaskRecord taskRecord) {
        this.mResizingTasksDuringAnimation.add(Integer.valueOf(taskRecord.taskId));
        taskRecord.setTaskDockedResizing(true);
    }

    int startActivityFromRecents(int i, int i2, int i3, SafeActivityOptions safeActivityOptions) throws Throwable {
        int launchActivityType;
        int launchWindowingMode;
        int i4;
        boolean z;
        int i5;
        int i6;
        TaskRecord taskRecord = null;
        ActivityOptions options = safeActivityOptions != null ? safeActivityOptions.getOptions(this) : null;
        if (options != null) {
            launchActivityType = options.getLaunchActivityType();
            launchWindowingMode = options.getLaunchWindowingMode();
        } else {
            launchActivityType = 0;
            launchWindowingMode = 0;
        }
        if (launchActivityType == 2 || launchActivityType == 3) {
            throw new IllegalArgumentException("startActivityFromRecents: Task " + i3 + " can't be launch in the home/recents stack.");
        }
        this.mWindowManager.deferSurfaceLayout();
        if (launchWindowingMode == 3) {
            try {
                this.mWindowManager.setDockedStackCreateState(options.getSplitScreenCreateMode(), null);
                deferUpdateRecentsHomeStackBounds();
                this.mWindowManager.prepareAppTransition(19, false);
            } catch (Throwable th) {
                th = th;
                i5 = 4;
                i6 = 3;
                z = false;
                i4 = launchWindowingMode;
                if (i4 == i6) {
                    setResizingDuringAnimation(taskRecord);
                    if (taskRecord.getStack().getDisplay().getTopStackInWindowingMode(i5).isActivityTypeHome()) {
                    }
                }
                this.mWindowManager.continueSurfaceLayout();
                throw th;
            }
        }
        TaskRecord taskRecordAnyTaskForIdLocked = anyTaskForIdLocked(i3, 2, options, true);
        if (taskRecordAnyTaskForIdLocked != null) {
            if (launchWindowingMode != 3) {
                try {
                    moveHomeStackToFront("startActivityFromRecents");
                } catch (Throwable th2) {
                    th = th2;
                    taskRecord = taskRecordAnyTaskForIdLocked;
                    i5 = 4;
                    i6 = 3;
                    z = false;
                    i4 = launchWindowingMode;
                    if (i4 == i6) {
                    }
                    this.mWindowManager.continueSurfaceLayout();
                    throw th;
                }
            }
            if (!this.mService.mUserController.shouldConfirmCredentials(taskRecordAnyTaskForIdLocked.userId) && taskRecordAnyTaskForIdLocked.getRootActivity() != null) {
                ActivityRecord topActivity = taskRecordAnyTaskForIdLocked.getTopActivity();
                sendPowerHintForLaunchStartIfNeeded(true, topActivity);
                this.mActivityMetricsLogger.notifyActivityLaunching();
                try {
                    this.mService.moveTaskToFrontLocked(taskRecordAnyTaskForIdLocked.taskId, 0, safeActivityOptions, true);
                    this.mActivityMetricsLogger.notifyActivityLaunched(2, topActivity);
                    this.mService.getActivityStartController().postStartActivityProcessingForLastStarter(taskRecordAnyTaskForIdLocked.getTopActivity(), 2, taskRecordAnyTaskForIdLocked.getStack());
                    if (launchWindowingMode == 3 && taskRecordAnyTaskForIdLocked != null) {
                        setResizingDuringAnimation(taskRecordAnyTaskForIdLocked);
                        if (taskRecordAnyTaskForIdLocked.getStack().getDisplay().getTopStackInWindowingMode(4).isActivityTypeHome()) {
                            moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                            this.mWindowManager.checkSplitScreenMinimizedChanged(false);
                        }
                    }
                    this.mWindowManager.continueSurfaceLayout();
                    return 2;
                } catch (Throwable th3) {
                    this.mActivityMetricsLogger.notifyActivityLaunched(2, topActivity);
                    throw th3;
                }
            }
            String str = taskRecordAnyTaskForIdLocked.mCallingPackage;
            Intent intent = taskRecordAnyTaskForIdLocked.intent;
            intent.addFlags(DumpState.DUMP_DEXOPT);
            int i7 = launchWindowingMode;
            try {
                int iStartActivityInPackage = this.mService.getActivityStartController().startActivityInPackage(taskRecordAnyTaskForIdLocked.mCallingUid, i, i2, str, intent, null, null, null, 0, 0, safeActivityOptions, taskRecordAnyTaskForIdLocked.userId, taskRecordAnyTaskForIdLocked, "startActivityFromRecents", false);
                if (i7 == 3 && taskRecordAnyTaskForIdLocked != null) {
                    setResizingDuringAnimation(taskRecordAnyTaskForIdLocked);
                    if (taskRecordAnyTaskForIdLocked.getStack().getDisplay().getTopStackInWindowingMode(4).isActivityTypeHome()) {
                        moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                        this.mWindowManager.checkSplitScreenMinimizedChanged(false);
                    }
                }
                this.mWindowManager.continueSurfaceLayout();
                return iStartActivityInPackage;
            } catch (Throwable th4) {
                th = th4;
                taskRecord = taskRecordAnyTaskForIdLocked;
                i4 = i7;
                i6 = 3;
                i5 = 4;
                z = false;
            }
        } else {
            taskRecord = taskRecordAnyTaskForIdLocked;
            i5 = 4;
            i6 = 3;
            z = false;
            i4 = launchWindowingMode;
            try {
                continueUpdateRecentsHomeStackBounds();
                this.mWindowManager.executeAppTransition();
                throw new IllegalArgumentException("startActivityFromRecents: Task " + i3 + " not found.");
            } catch (Throwable th5) {
                th = th5;
            }
        }
        if (i4 == i6 && taskRecord != null) {
            setResizingDuringAnimation(taskRecord);
            if (taskRecord.getStack().getDisplay().getTopStackInWindowingMode(i5).isActivityTypeHome()) {
                moveHomeStackToFront("startActivityFromRecents: homeVisibleInSplitScreen");
                this.mWindowManager.checkSplitScreenMinimizedChanged(z);
            }
        }
        this.mWindowManager.continueSurfaceLayout();
        throw th;
    }

    List<IBinder> getTopVisibleActivities() {
        ActivityRecord topActivity;
        ArrayList arrayList = new ArrayList();
        for (int size = this.mActivityDisplays.size() - 1; size >= 0; size--) {
            ActivityDisplay activityDisplayValueAt = this.mActivityDisplays.valueAt(size);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = activityDisplayValueAt.getChildAt(childCount);
                if (childAt.shouldBeVisible(null) && (topActivity = childAt.getTopActivity()) != null) {
                    if (childAt == this.mFocusedStack) {
                        arrayList.add(0, topActivity.appToken);
                    } else {
                        arrayList.add(topActivity.appToken);
                    }
                }
            }
        }
        return arrayList;
    }

    static class WaitInfo {
        private final WaitResult mResult;
        private final ComponentName mTargetComponent;

        public WaitInfo(ComponentName componentName, WaitResult waitResult) {
            this.mTargetComponent = componentName;
            this.mResult = waitResult;
        }

        public boolean matches(ComponentName componentName) {
            return this.mTargetComponent == null || this.mTargetComponent.equals(componentName);
        }

        public WaitResult getResult() {
            return this.mResult;
        }

        public ComponentName getComponent() {
            return this.mTargetComponent;
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(str + "WaitInfo:");
            printWriter.println(str + "  mTargetComponent=" + this.mTargetComponent);
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("  mResult=");
            printWriter.println(sb.toString());
            this.mResult.dump(printWriter, str);
        }
    }

    private final class SleepTokenImpl extends ActivityManagerInternal.SleepToken {
        private final long mAcquireTime = SystemClock.uptimeMillis();
        private final int mDisplayId;
        private final String mTag;

        public SleepTokenImpl(String str, int i) {
            this.mTag = str;
            this.mDisplayId = i;
        }

        public void release() {
            synchronized (ActivityStackSupervisor.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStackSupervisor.this.removeSleepTokenLocked(this);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        public String toString() {
            return "{\"" + this.mTag + "\", display " + this.mDisplayId + ", acquire at " + TimeUtils.formatUptime(this.mAcquireTime) + "}";
        }
    }
}
