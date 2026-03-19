package com.android.server.am;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.ResultInfo;
import android.app.WindowConfiguration;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.app.servertransaction.DestroyActivityItem;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.app.servertransaction.StopActivityItem;
import android.app.servertransaction.WindowVisibilityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.StackWindowController;
import com.android.server.wm.StackWindowListener;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class ActivityStack<T extends StackWindowController> extends ConfigurationContainer implements StackWindowListener {
    static final int DESTROY_ACTIVITIES_MSG = 105;
    private static final int DESTROY_TIMEOUT = 10000;
    static final int DESTROY_TIMEOUT_MSG = 102;
    static final int FINISH_AFTER_PAUSE = 1;
    static final int FINISH_AFTER_VISIBLE = 2;
    static final int FINISH_IMMEDIATELY = 0;
    private static final int FIT_WITHIN_BOUNDS_DIVIDER = 3;
    static final int LAUNCH_TICK = 500;
    static final int LAUNCH_TICK_MSG = 103;
    private static final int MAX_STOPPING_TO_FORCE = 3;
    private static final int PAUSE_TIMEOUT = 500;
    static final int PAUSE_TIMEOUT_MSG = 101;

    @VisibleForTesting
    protected static final int REMOVE_TASK_MODE_DESTROYING = 0;
    static final int REMOVE_TASK_MODE_MOVING = 1;
    static final int REMOVE_TASK_MODE_MOVING_TO_TOP = 2;
    private static final boolean SHOW_APP_STARTING_PREVIEW = true;
    private static final int STOP_TIMEOUT = 11000;
    static final int STOP_TIMEOUT_MSG = 104;
    private static final String TAG = "ActivityManager";
    private static final String TAG_ADD_REMOVE = TAG + ActivityManagerDebugConfig.POSTFIX_ADD_REMOVE;
    private static final String TAG_APP = TAG + ActivityManagerDebugConfig.POSTFIX_APP;
    private static final String TAG_CLEANUP = TAG + ActivityManagerDebugConfig.POSTFIX_CLEANUP;
    private static final String TAG_CONTAINERS = TAG + ActivityManagerDebugConfig.POSTFIX_CONTAINERS;
    private static final String TAG_PAUSE = TAG + ActivityManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RELEASE = TAG + ActivityManagerDebugConfig.POSTFIX_RELEASE;
    private static final String TAG_RESULTS = TAG + ActivityManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_SAVED_STATE = TAG + ActivityManagerDebugConfig.POSTFIX_SAVED_STATE;
    private static final String TAG_STACK = TAG + ActivityManagerDebugConfig.POSTFIX_STACK;
    private static final String TAG_STATES = TAG + ActivityManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static final String TAG_TRANSITION = TAG + ActivityManagerDebugConfig.POSTFIX_TRANSITION;
    private static final String TAG_USER_LEAVING = TAG + ActivityManagerDebugConfig.POSTFIX_USER_LEAVING;
    private static final String TAG_VISIBILITY = TAG + ActivityManagerDebugConfig.POSTFIX_VISIBILITY;
    private static final long TRANSLUCENT_CONVERSION_TIMEOUT = 2000;
    static final int TRANSLUCENT_TIMEOUT_MSG = 106;
    boolean mConfigWillChange;
    int mCurrentUser;
    int mDisplayId;
    final Handler mHandler;
    final ActivityManagerService mService;
    final int mStackId;
    protected final ActivityStackSupervisor mStackSupervisor;
    private boolean mTopActivityOccludesKeyguard;
    private ActivityRecord mTopDismissingKeyguardActivity;
    private boolean mUpdateBoundsDeferred;
    private boolean mUpdateBoundsDeferredCalled;
    T mWindowContainerController;
    private final WindowManagerService mWindowManager;
    private final ArrayList<TaskRecord> mTaskHistory = new ArrayList<>();
    final ArrayList<ActivityRecord> mLRUActivities = new ArrayList<>();
    ActivityRecord mPausingActivity = null;
    ActivityRecord mLastPausedActivity = null;
    ActivityRecord mLastNoHistoryActivity = null;
    ActivityRecord mResumedActivity = null;
    ActivityRecord mTranslucentActivityWaiting = null;
    ArrayList<ActivityRecord> mUndrawnActivitiesBelowTopTranslucent = new ArrayList<>();
    boolean mForceHidden = false;
    private final Rect mDeferredBounds = new Rect();
    private final Rect mDeferredTaskBounds = new Rect();
    private final Rect mDeferredTaskInsetBounds = new Rect();
    private final SparseArray<Rect> mTmpBounds = new SparseArray<>();
    private final SparseArray<Rect> mTmpInsetBounds = new SparseArray<>();
    private final Rect mTmpRect2 = new Rect();
    private final ActivityOptions mTmpOptions = ActivityOptions.makeBasic();
    private final ArrayList<ActivityRecord> mTmpActivities = new ArrayList<>();

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

    @Override
    protected int getChildCount() {
        return this.mTaskHistory.size();
    }

    @Override
    protected ConfigurationContainer getChildAt(int i) {
        return this.mTaskHistory.get(i);
    }

    @Override
    protected ConfigurationContainer getParent() {
        return getDisplay();
    }

    @Override
    protected void onParentChanged() {
        super.onParentChanged();
        this.mStackSupervisor.updateUIDsPresentOnDisplay();
    }

    private static class ScheduleDestroyArgs {
        final ProcessRecord mOwner;
        final String mReason;

        ScheduleDestroyArgs(ProcessRecord processRecord, String str) {
            this.mOwner = processRecord;
            this.mReason = str;
        }
    }

    private class ActivityStackHandler extends Handler {
        ActivityStackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 101:
                    ActivityRecord activityRecord = (ActivityRecord) message.obj;
                    Slog.w(ActivityStack.TAG, "Activity pause timeout for " + activityRecord);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (activityRecord.app != null) {
                                ActivityStack.this.mService.logAppTooSlow(activityRecord.app, activityRecord.pauseTime, "pausing " + activityRecord);
                            }
                            ActivityStack.this.activityPausedLocked(activityRecord.appToken, true);
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 102:
                    ActivityRecord activityRecord2 = (ActivityRecord) message.obj;
                    Slog.w(ActivityStack.TAG, "Activity destroy timeout for " + activityRecord2);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.activityDestroyedLocked((IBinder) (activityRecord2 != null ? activityRecord2.appToken : null), "destroyTimeout");
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 103:
                    ActivityRecord activityRecord3 = (ActivityRecord) message.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (activityRecord3.continueLaunchTickingLocked()) {
                                ActivityStack.this.mService.logAppTooSlow(activityRecord3.app, activityRecord3.launchTickTime, "launching " + activityRecord3);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 104:
                    ActivityRecord activityRecord4 = (ActivityRecord) message.obj;
                    Slog.w(ActivityStack.TAG, "Activity stop timeout for " + activityRecord4);
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (activityRecord4.isInHistory()) {
                                activityRecord4.activityStoppedLocked(null, null, null);
                            }
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 105:
                    ScheduleDestroyArgs scheduleDestroyArgs = (ScheduleDestroyArgs) message.obj;
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.destroyActivitiesLocked(scheduleDestroyArgs.mOwner, scheduleDestroyArgs.mReason);
                        } finally {
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 106:
                    synchronized (ActivityStack.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityStack.this.notifyActivityDrawnLocked(null);
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

    int numActivities() {
        int size = 0;
        for (int size2 = this.mTaskHistory.size() - 1; size2 >= 0; size2--) {
            size += this.mTaskHistory.get(size2).mActivities.size();
        }
        return size;
    }

    ActivityStack(ActivityDisplay activityDisplay, int i, ActivityStackSupervisor activityStackSupervisor, int i2, int i3, boolean z) {
        this.mStackSupervisor = activityStackSupervisor;
        this.mService = activityStackSupervisor.mService;
        this.mHandler = new ActivityStackHandler(this.mService.mHandler.getLooper());
        this.mWindowManager = this.mService.mWindowManager;
        this.mStackId = i;
        this.mCurrentUser = this.mService.mUserController.getCurrentUserId();
        this.mTmpRect2.setEmpty();
        this.mDisplayId = activityDisplay.mDisplayId;
        setActivityType(i3);
        setWindowingMode(i2);
        this.mWindowContainerController = (T) createStackWindowController(activityDisplay.mDisplayId, z, this.mTmpRect2);
        postAddToDisplay(activityDisplay, this.mTmpRect2.isEmpty() ? null : this.mTmpRect2, z);
    }

    T createStackWindowController(int i, boolean z, Rect rect) {
        return (T) new StackWindowController(this.mStackId, this, i, z, rect, this.mStackSupervisor.mWindowManager);
    }

    T getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void onActivityStateChanged(ActivityRecord activityRecord, ActivityState activityState, String str) {
        if (activityRecord == this.mResumedActivity && activityState != ActivityState.RESUMED) {
            setResumedActivity(null, str + " - onActivityStateChanged");
        }
        if (activityState == ActivityState.RESUMED) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.v(TAG_STACK, "set resumed activity to:" + activityRecord + " reason:" + str);
            }
            setResumedActivity(activityRecord, str + " - onActivityStateChanged");
            this.mService.setResumedActivityUncheckLocked(activityRecord, str);
            this.mStackSupervisor.mRecentTasks.add(activityRecord.getTask());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        int windowingMode = getWindowingMode();
        super.onConfigurationChanged(configuration);
        ActivityDisplay display = getDisplay();
        if (display != null && windowingMode != getWindowingMode()) {
            display.onStackWindowingModeChanged(this);
        }
    }

    @Override
    public void setWindowingMode(int i) {
        setWindowingMode(i, false, false, false, false);
    }

    void setWindowingMode(int i, boolean z, boolean z2, boolean z3, boolean z4) {
        int iResolveWindowingMode;
        Rect launchBounds;
        boolean z5 = this.mWindowContainerController == null;
        int windowingMode = getWindowingMode();
        ActivityDisplay display = getDisplay();
        TaskRecord taskRecord = topTask();
        ActivityStack splitScreenPrimaryStack = display.getSplitScreenPrimaryStack();
        this.mTmpOptions.setLaunchWindowingMode(i);
        if (!z5) {
            iResolveWindowingMode = display.resolveWindowingMode(null, this.mTmpOptions, taskRecord, getActivityType());
        } else {
            iResolveWindowingMode = i;
        }
        if (splitScreenPrimaryStack == this && iResolveWindowingMode == 4) {
            iResolveWindowingMode = 1;
        }
        boolean zHasSplitScreenPrimaryStack = display.hasSplitScreenPrimaryStack();
        boolean z6 = !z3;
        if (zHasSplitScreenPrimaryStack && iResolveWindowingMode == 1 && z6 && isActivityTypeStandardOrUndefined()) {
            if ((i == 3 || i == 4) || z5) {
                this.mService.mTaskChangeNotificationController.notifyActivityDismissingDockedStack();
                display.getSplitScreenPrimaryStack().setWindowingMode(1, false, false, false, true);
            }
        }
        if (windowingMode == iResolveWindowingMode) {
            return;
        }
        WindowManagerService windowManagerService = this.mService.mWindowManager;
        ActivityRecord topActivity = getTopActivity();
        if (z6 && iResolveWindowingMode != 1 && topActivity != null && topActivity.isNonResizableOrForcedResizable() && !topActivity.noDisplay) {
            this.mService.mTaskChangeNotificationController.notifyActivityForcedResizable(taskRecord.taskId, 1, topActivity.appInfo.packageName);
        }
        windowManagerService.deferSurfaceLayout();
        if (!z && topActivity != null) {
            try {
                this.mStackSupervisor.mNoAnimActivities.add(topActivity);
            } finally {
                if (z2 && !zHasSplitScreenPrimaryStack && this.mDisplayId == 0 && iResolveWindowingMode == 3) {
                    display.getOrCreateStack(4, 3, true).moveToFront("setWindowingMode");
                    this.mService.mWindowManager.showRecentApps();
                }
                windowManagerService.continueSurfaceLayout();
            }
        }
        super.setWindowingMode(iResolveWindowingMode);
        if (z5) {
            return;
        }
        if (iResolveWindowingMode == 2 || windowingMode == 2) {
            throw new IllegalArgumentException("Changing pinned windowing mode not currently supported");
        }
        if (iResolveWindowingMode == 3 && splitScreenPrimaryStack != null) {
            throw new IllegalArgumentException("Setting primary split-screen windowing mode while there is already one isn't currently supported");
        }
        this.mTmpRect2.setEmpty();
        if (iResolveWindowingMode != 1) {
            this.mWindowContainerController.getRawBounds(this.mTmpRect2);
            if (iResolveWindowingMode == 5 && taskRecord != null && (launchBounds = topTask().getLaunchBounds()) != null) {
                this.mTmpRect2.set(launchBounds);
            }
        }
        if (!Objects.equals(getOverrideBounds(), this.mTmpRect2)) {
            resize(this.mTmpRect2, null, null);
        }
        if (z2 && !zHasSplitScreenPrimaryStack && this.mDisplayId == 0 && iResolveWindowingMode == 3) {
            display.getOrCreateStack(4, 3, true).moveToFront("setWindowingMode");
            this.mService.mWindowManager.showRecentApps();
        }
        windowManagerService.continueSurfaceLayout();
        if (!z4) {
            this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, true);
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    @Override
    public boolean isCompatible(int i, int i2) {
        if (i2 == 0) {
            i2 = 1;
        }
        ActivityDisplay display = getDisplay();
        if (display != null && i2 == 1 && i == 0) {
            i = display.getWindowingMode();
        }
        return super.isCompatible(i, i2);
    }

    void reparent(ActivityDisplay activityDisplay, boolean z) {
        removeFromDisplay();
        this.mTmpRect2.setEmpty();
        this.mWindowContainerController.reparent(activityDisplay.mDisplayId, this.mTmpRect2, z);
        postAddToDisplay(activityDisplay, this.mTmpRect2.isEmpty() ? null : this.mTmpRect2, z);
        adjustFocusToNextFocusableStack("reparent", true);
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private void postAddToDisplay(ActivityDisplay activityDisplay, Rect rect, boolean z) {
        this.mDisplayId = activityDisplay.mDisplayId;
        setBounds(rect);
        onParentChanged();
        activityDisplay.addChild(this, z ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        if (inSplitScreenPrimaryWindowingMode()) {
            this.mStackSupervisor.resizeDockedStackLocked(getOverrideBounds(), null, null, null, null, true);
        }
    }

    private void removeFromDisplay() {
        ActivityDisplay display = getDisplay();
        if (display != null) {
            display.removeChild(this);
        }
        this.mDisplayId = -1;
    }

    void remove() {
        removeFromDisplay();
        this.mWindowContainerController.removeContainer();
        this.mWindowContainerController = null;
        onParentChanged();
    }

    ActivityDisplay getDisplay() {
        return this.mStackSupervisor.getActivityDisplay(this.mDisplayId);
    }

    void getStackDockedModeBounds(Rect rect, Rect rect2, Rect rect3, boolean z) {
        this.mWindowContainerController.getStackDockedModeBounds(rect, rect2, rect3, z);
    }

    void prepareFreezingTaskBounds() {
        this.mWindowContainerController.prepareFreezingTaskBounds();
    }

    void getWindowContainerBounds(Rect rect) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.getBounds(rect);
        } else {
            rect.setEmpty();
        }
    }

    void getBoundsForNewConfiguration(Rect rect) {
        this.mWindowContainerController.getBoundsForNewConfiguration(rect);
    }

    void positionChildWindowContainerAtTop(TaskRecord taskRecord) {
        this.mWindowContainerController.positionChildAtTop(taskRecord.getWindowContainerController(), true);
    }

    boolean deferScheduleMultiWindowModeChanged() {
        return false;
    }

    void deferUpdateBounds() {
        if (!this.mUpdateBoundsDeferred) {
            this.mUpdateBoundsDeferred = true;
            this.mUpdateBoundsDeferredCalled = false;
        }
    }

    void continueUpdateBounds() {
        Rect rect;
        Rect rect2;
        boolean z = this.mUpdateBoundsDeferred;
        this.mUpdateBoundsDeferred = false;
        if (z && this.mUpdateBoundsDeferredCalled) {
            if (!this.mDeferredBounds.isEmpty()) {
                rect = this.mDeferredBounds;
            } else {
                rect = null;
            }
            if (!this.mDeferredTaskBounds.isEmpty()) {
                rect2 = this.mDeferredTaskBounds;
            } else {
                rect2 = null;
            }
            resize(rect, rect2, this.mDeferredTaskInsetBounds.isEmpty() ? null : this.mDeferredTaskInsetBounds);
        }
    }

    boolean updateBoundsAllowed(Rect rect, Rect rect2, Rect rect3) {
        if (!this.mUpdateBoundsDeferred) {
            return true;
        }
        if (rect != null) {
            this.mDeferredBounds.set(rect);
        } else {
            this.mDeferredBounds.setEmpty();
        }
        if (rect2 != null) {
            this.mDeferredTaskBounds.set(rect2);
        } else {
            this.mDeferredTaskBounds.setEmpty();
        }
        if (rect3 != null) {
            this.mDeferredTaskInsetBounds.set(rect3);
        } else {
            this.mDeferredTaskInsetBounds.setEmpty();
        }
        this.mUpdateBoundsDeferredCalled = true;
        return false;
    }

    @Override
    public int setBounds(Rect rect) {
        if (!inMultiWindowMode()) {
            rect = null;
        }
        return super.setBounds(rect);
    }

    ActivityRecord topRunningActivityLocked() {
        return topRunningActivityLocked(false);
    }

    void getAllRunningVisibleActivitiesLocked(ArrayList<ActivityRecord> arrayList) {
        arrayList.clear();
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            this.mTaskHistory.get(size).getAllRunningVisibleActivitiesLocked(arrayList);
        }
    }

    private ActivityRecord topRunningActivityLocked(boolean z) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = this.mTaskHistory.get(size).topRunningActivityLocked();
            if (activityRecord != null && (!z || activityRecord.isFocusable())) {
                return activityRecord;
            }
        }
        return null;
    }

    ActivityRecord topRunningNonOverlayTaskActivity() {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (!activityRecord.finishing && !activityRecord.mTaskOverlay) {
                    return activityRecord;
                }
            }
        }
        return null;
    }

    ActivityRecord topRunningNonDelayedActivityLocked(ActivityRecord activityRecord) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord2 = arrayList.get(size2);
                if (!activityRecord2.finishing && !activityRecord2.delayedResume && activityRecord2 != activityRecord && activityRecord2.okToShowLocked()) {
                    return activityRecord2;
                }
            }
        }
        return null;
    }

    final ActivityRecord topRunningActivityLocked(IBinder iBinder, int i) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (taskRecord.taskId != i) {
                ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
                for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                    ActivityRecord activityRecord = arrayList.get(size2);
                    if (!activityRecord.finishing && iBinder != activityRecord.appToken && activityRecord.okToShowLocked()) {
                        return activityRecord;
                    }
                }
            }
        }
        return null;
    }

    ActivityRecord getTopActivity() {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ActivityRecord topActivity = this.mTaskHistory.get(size).getTopActivity();
            if (topActivity != null) {
                return topActivity;
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

    private TaskRecord bottomTask() {
        if (this.mTaskHistory.isEmpty()) {
            return null;
        }
        return this.mTaskHistory.get(0);
    }

    TaskRecord taskForIdLocked(int i) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (taskRecord.taskId == i) {
                return taskRecord;
            }
        }
        return null;
    }

    ActivityRecord isInStackLocked(IBinder iBinder) {
        return isInStackLocked(ActivityRecord.forTokenLocked(iBinder));
    }

    ActivityRecord isInStackLocked(ActivityRecord activityRecord) {
        if (activityRecord == null) {
            return null;
        }
        TaskRecord task = activityRecord.getTask();
        ActivityStack<T> stack = activityRecord.getStack();
        if (stack == null || !task.mActivities.contains(activityRecord) || !this.mTaskHistory.contains(task)) {
            return null;
        }
        if (stack != this) {
            Slog.w(TAG, "Illegal state! task does not point to stack it is in.");
        }
        return activityRecord;
    }

    boolean isInStackLocked(TaskRecord taskRecord) {
        return this.mTaskHistory.contains(taskRecord);
    }

    boolean isUidPresent(int i) {
        Iterator<TaskRecord> it = this.mTaskHistory.iterator();
        while (it.hasNext()) {
            Iterator<ActivityRecord> it2 = it.next().mActivities.iterator();
            while (it2.hasNext()) {
                if (it2.next().getUid() == i) {
                    return true;
                }
            }
        }
        return false;
    }

    void getPresentUIDs(IntArray intArray) {
        Iterator<TaskRecord> it = this.mTaskHistory.iterator();
        while (it.hasNext()) {
            Iterator<ActivityRecord> it2 = it.next().mActivities.iterator();
            while (it2.hasNext()) {
                intArray.add(it2.next().getUid());
            }
        }
    }

    final void removeActivitiesFromLRUListLocked(TaskRecord taskRecord) {
        Iterator<ActivityRecord> it = taskRecord.mActivities.iterator();
        while (it.hasNext()) {
            this.mLRUActivities.remove(it.next());
        }
    }

    final boolean updateLRUListLocked(ActivityRecord activityRecord) {
        boolean zRemove = this.mLRUActivities.remove(activityRecord);
        this.mLRUActivities.add(activityRecord);
        return zRemove;
    }

    final boolean isHomeOrRecentsStack() {
        return isActivityTypeHome() || isActivityTypeRecents();
    }

    final boolean isOnHomeDisplay() {
        return this.mDisplayId == 0;
    }

    private boolean returnsToHomeStack() {
        return (inMultiWindowMode() || this.mTaskHistory.isEmpty() || !this.mTaskHistory.get(0).returnsToHomeStack()) ? false : true;
    }

    void moveToFront(String str) {
        moveToFront(str, null);
    }

    void moveToFront(String str, TaskRecord taskRecord) {
        ActivityStack topStackInWindowingMode;
        if (!isAttached()) {
            return;
        }
        ActivityDisplay display = getDisplay();
        if (inSplitScreenSecondaryWindowingMode() && (topStackInWindowingMode = display.getTopStackInWindowingMode(1)) != null) {
            ActivityStack splitScreenPrimaryStack = display.getSplitScreenPrimaryStack();
            if (display.getIndexOf(topStackInWindowingMode) > display.getIndexOf(splitScreenPrimaryStack)) {
                splitScreenPrimaryStack.moveToFront(str + " splitScreenToTop");
            }
        }
        if (!isActivityTypeHome() && returnsToHomeStack()) {
            this.mStackSupervisor.moveHomeStackToFront(str + " returnToHome");
        }
        display.positionChildAtTop(this);
        this.mStackSupervisor.setFocusStackUnchecked(str, this);
        if (taskRecord != null) {
            insertTaskAtTop(taskRecord, null);
        }
    }

    void moveToBack(String str, TaskRecord taskRecord) {
        if (!isAttached()) {
            return;
        }
        if (getWindowingMode() == 3) {
            setWindowingMode(1);
        }
        getDisplay().positionChildAtBottom(this);
        this.mStackSupervisor.setFocusStackUnchecked(str, getDisplay().getTopStack());
        if (taskRecord != null) {
            insertTaskAtBottom(taskRecord);
        }
    }

    boolean isFocusable() {
        ActivityRecord activityRecord = topRunningActivityLocked();
        return this.mStackSupervisor.isFocusable(this, activityRecord != null && activityRecord.isFocusable());
    }

    final boolean isAttached() {
        return getParent() != null;
    }

    void findTaskLocked(ActivityRecord activityRecord, ActivityStackSupervisor.FindTaskResult findTaskResult) {
        int i;
        boolean z;
        boolean z2;
        boolean z3;
        Uri uri;
        Uri data;
        ActivityStack<T> activityStack = this;
        Intent intent = activityRecord.intent;
        ActivityInfo activityInfo = activityRecord.info;
        ComponentName component = intent.getComponent();
        if (activityInfo.targetActivity != null) {
            component = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
        }
        int userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        boolean z4 = false;
        boolean z5 = true;
        boolean zIsDocument = (intent != null) & intent.isDocument();
        Uri data2 = zIsDocument ? intent.getData() : null;
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG_TASKS, "Looking for task of " + activityRecord + " in " + activityStack);
        }
        int size = activityStack.mTaskHistory.size() - 1;
        while (size >= 0) {
            TaskRecord taskRecord = activityStack.mTaskHistory.get(size);
            if (taskRecord.voiceSession != null) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "Skipping " + taskRecord + ": voice session");
                }
            } else if (taskRecord.userId == userId) {
                ActivityRecord topActivity = taskRecord.getTopActivity(z4);
                if (topActivity == null || topActivity.finishing || topActivity.userId != userId || topActivity.launchMode == 3) {
                    i = userId;
                    z = z4;
                    z2 = z5;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.d(TAG_TASKS, "Skipping " + taskRecord + ": mismatch root " + topActivity);
                    }
                } else if (topActivity.hasCompatibleActivityType(activityRecord)) {
                    Intent intent2 = taskRecord.intent;
                    Intent intent3 = taskRecord.affinityIntent;
                    if (intent2 != null && intent2.isDocument()) {
                        data = intent2.getData();
                    } else if (intent3 == null || !intent3.isDocument()) {
                        z3 = z4;
                        uri = null;
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            i = userId;
                        } else {
                            String str = TAG_TASKS;
                            StringBuilder sb = new StringBuilder();
                            i = userId;
                            sb.append("Comparing existing cls=");
                            sb.append(intent2.getComponent().flattenToShortString());
                            sb.append("/aff=");
                            sb.append(topActivity.getTask().rootAffinity);
                            sb.append(" to new cls=");
                            sb.append(intent.getComponent().flattenToShortString());
                            sb.append("/aff=");
                            sb.append(activityInfo.taskAffinity);
                            Slog.d(str, sb.toString());
                        }
                        if (intent2 == null && intent2.getComponent() != null && intent2.getComponent().compareTo(component) == 0 && Objects.equals(data2, uri)) {
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "Found matching class!");
                            }
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "For Intent " + intent + " bringing to top: " + topActivity.intent);
                            }
                            findTaskResult.r = topActivity;
                            findTaskResult.matchedByRootAffinity = false;
                            return;
                        }
                        if (intent3 == null && intent3.getComponent() != null && intent3.getComponent().compareTo(component) == 0 && Objects.equals(data2, uri)) {
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "Found matching class!");
                            }
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "For Intent " + intent + " bringing to top: " + topActivity.intent);
                            }
                            findTaskResult.r = topActivity;
                            findTaskResult.matchedByRootAffinity = false;
                            return;
                        }
                        z = false;
                        if (!zIsDocument || z3 || findTaskResult.r != null || taskRecord.rootAffinity == null) {
                            z2 = true;
                            if (!ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "Not a match: " + taskRecord);
                            }
                        } else if (taskRecord.rootAffinity.equals(activityRecord.taskAffinity)) {
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.d(TAG_TASKS, "Found matching affinity candidate!");
                            }
                            findTaskResult.r = topActivity;
                            z2 = true;
                            findTaskResult.matchedByRootAffinity = true;
                        } else {
                            z2 = true;
                        }
                    } else {
                        data = intent3.getData();
                    }
                    Uri uri2 = data;
                    z3 = z5;
                    uri = uri2;
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    }
                    if (intent2 == null) {
                    }
                    if (intent3 == null) {
                    }
                    z = false;
                    if (zIsDocument) {
                        z2 = true;
                        if (!ActivityManagerDebugConfig.DEBUG_TASKS) {
                        }
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "Skipping " + taskRecord + ": mismatch activity type");
                }
                size--;
                z4 = z;
                z5 = z2;
                userId = i;
                activityStack = this;
            } else if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG_TASKS, "Skipping " + taskRecord + ": different user");
            }
            i = userId;
            z = z4;
            z2 = z5;
            size--;
            z4 = z;
            z5 = z2;
            userId = i;
            activityStack = this;
        }
    }

    ActivityRecord findActivityLocked(Intent intent, ActivityInfo activityInfo, boolean z) {
        ComponentName component = intent.getComponent();
        if (activityInfo.targetActivity != null) {
            component = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
        }
        int userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (activityRecord.okToShowLocked() && !activityRecord.finishing && activityRecord.userId == userId) {
                    if (z) {
                        if (activityRecord.intent.filterEquals(intent)) {
                            return activityRecord;
                        }
                    } else if (activityRecord.intent.getComponent().equals(component)) {
                        return activityRecord;
                    }
                }
            }
        }
        return null;
    }

    final void switchUserLocked(int i) {
        if (this.mCurrentUser == i) {
            return;
        }
        this.mCurrentUser = i;
        int size = this.mTaskHistory.size();
        int i2 = 0;
        while (i2 < size) {
            TaskRecord taskRecord = this.mTaskHistory.get(i2);
            if (taskRecord.okToShowLocked()) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG_TASKS, "switchUserLocked: stack=" + getStackId() + " moving " + taskRecord + " to top");
                }
                this.mTaskHistory.remove(i2);
                this.mTaskHistory.add(taskRecord);
                size--;
            } else {
                i2++;
            }
        }
    }

    void minimalResumeActivityLocked(ActivityRecord activityRecord) {
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to RESUMED: " + activityRecord + " (starting new instance) callers=" + Debug.getCallers(5));
        }
        activityRecord.setState(ActivityState.RESUMED, "minimalResumeActivityLocked");
        activityRecord.completeResumeLocked();
        this.mStackSupervisor.getLaunchTimeTracker().setLaunchTime(activityRecord);
        if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
            Slog.i(TAG_SAVED_STATE, "Launch completed; removing icicle of " + activityRecord.icicle);
        }
        this.mService.mAmsExt.onAfterActivityResumed(activityRecord);
    }

    private void clearLaunchTime(ActivityRecord activityRecord) {
        if (this.mStackSupervisor.mWaitingActivityLaunched.isEmpty()) {
            activityRecord.fullyDrawnStartTime = 0L;
            activityRecord.displayStartTime = 0L;
        } else {
            this.mStackSupervisor.removeTimeoutsForActivityLocked(activityRecord);
            this.mStackSupervisor.scheduleIdleTimeoutLocked(activityRecord);
        }
    }

    void awakeFromSleepingLocked() {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                arrayList.get(size2).setSleeping(false);
            }
        }
        if (this.mPausingActivity != null) {
            Slog.d(TAG, "awakeFromSleepingLocked: previously pausing activity didn't pause");
            activityPausedLocked(this.mPausingActivity.appToken, true);
        }
    }

    void updateActivityApplicationInfoLocked(ApplicationInfo applicationInfo) {
        String str = applicationInfo.packageName;
        int userId = UserHandle.getUserId(applicationInfo.uid);
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (userId == activityRecord.userId && str.equals(activityRecord.packageName)) {
                    activityRecord.updateApplicationInfo(applicationInfo);
                }
            }
        }
    }

    void checkReadyForSleep() {
        if (shouldSleepActivities() && goToSleepIfPossible(false)) {
            this.mStackSupervisor.checkReadyForSleepLocked(true);
        }
    }

    boolean goToSleepIfPossible(boolean z) {
        boolean z2 = true;
        if (this.mResumedActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Sleep needs to pause " + this.mResumedActivity);
            }
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(TAG_USER_LEAVING, "Sleep => pause with userLeaving=false");
            }
            startPausingLocked(false, true, null, false);
        } else {
            if (this.mPausingActivity != null) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Sleep still waiting to pause " + this.mPausingActivity);
                }
            }
            if (!z) {
                if (containsActivityFromStack(this.mStackSupervisor.mStoppingActivities)) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Sleep still need to stop " + this.mStackSupervisor.mStoppingActivities.size() + " activities");
                    }
                    this.mStackSupervisor.scheduleIdleLocked();
                    z2 = false;
                }
                if (containsActivityFromStack(this.mStackSupervisor.mGoingToSleepActivities)) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Sleep still need to sleep " + this.mStackSupervisor.mGoingToSleepActivities.size() + " activities");
                    }
                    z2 = false;
                }
            }
            if (z2) {
                goToSleep();
            }
            return z2;
        }
        z2 = false;
        if (!z) {
        }
        if (z2) {
        }
        return z2;
    }

    void goToSleep() {
        ensureActivitiesVisibleLocked(null, 0, false);
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (activityRecord.isState(ActivityState.STOPPING, ActivityState.STOPPED, ActivityState.PAUSED, ActivityState.PAUSING)) {
                    activityRecord.setSleeping(true);
                }
            }
        }
    }

    private boolean containsActivityFromStack(List<ActivityRecord> list) {
        Iterator<ActivityRecord> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().getStack() == this) {
                return true;
            }
        }
        return false;
    }

    private void schedulePauseTimeout(ActivityRecord activityRecord) {
        Message messageObtainMessage = this.mHandler.obtainMessage(101);
        messageObtainMessage.obj = activityRecord;
        activityRecord.pauseTime = SystemClock.uptimeMillis();
        this.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Waiting for pause to complete...");
        }
    }

    final boolean startPausingLocked(boolean z, boolean z2, ActivityRecord activityRecord, boolean z3) {
        if (this.mPausingActivity != null) {
            Slog.wtf(TAG, "Going to pause when pause is already pending for " + this.mPausingActivity + " state=" + this.mPausingActivity.getState());
            if (!shouldSleepActivities()) {
                completePauseLocked(false, activityRecord);
            }
        }
        ActivityRecord activityRecord2 = this.mResumedActivity;
        if (activityRecord2 == null) {
            if (activityRecord == null) {
                Slog.wtf(TAG, "Trying to pause when nothing is resumed");
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
            return false;
        }
        if (activityRecord2 == activityRecord) {
            Slog.wtf(TAG, "Trying to pause activity that is in process of being resumed");
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to PAUSING: " + activityRecord2);
        } else if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Start pausing: " + activityRecord2);
        }
        this.mPausingActivity = activityRecord2;
        this.mLastPausedActivity = activityRecord2;
        this.mLastNoHistoryActivity = ((activityRecord2.intent.getFlags() & 1073741824) == 0 && (activityRecord2.info.flags & 128) == 0) ? null : activityRecord2;
        activityRecord2.setState(ActivityState.PAUSING, "startPausingLocked");
        activityRecord2.getTask().touchActiveTime();
        clearLaunchTime(activityRecord2);
        this.mStackSupervisor.getLaunchTimeTracker().stopFullyDrawnTraceIfNeeded(getWindowingMode());
        this.mService.updateCpuStats();
        if (activityRecord2.app != null && activityRecord2.app.thread != null) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Enqueueing pending pause: " + activityRecord2);
            }
            try {
                EventLogTags.writeAmPauseActivity(activityRecord2.userId, System.identityHashCode(activityRecord2), activityRecord2.shortComponentName, "userLeaving=" + z);
                this.mService.updateUsageStats(activityRecord2, false);
                this.mService.getLifecycleManager().scheduleTransaction(activityRecord2.app.thread, (IBinder) activityRecord2.appToken, (ActivityLifecycleItem) PauseActivityItem.obtain(activityRecord2.finishing, z, activityRecord2.configChangeFlags, z3));
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown during pause", e);
                this.mPausingActivity = null;
                this.mLastPausedActivity = null;
                this.mLastNoHistoryActivity = null;
            }
        } else {
            this.mPausingActivity = null;
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        }
        if (!z2 && !this.mService.isSleepingOrShuttingDownLocked()) {
            this.mStackSupervisor.acquireLaunchWakelock();
        }
        if (this.mPausingActivity != null) {
            if (!z2) {
                activityRecord2.pauseKeyDispatchingLocked();
            } else if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                Slog.v(TAG_PAUSE, "Key dispatch not paused for screen off");
            }
            if (z3) {
                completePauseLocked(false, activityRecord);
                return false;
            }
            schedulePauseTimeout(activityRecord2);
            return true;
        }
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Activity not running, resuming next.");
        }
        if (activityRecord == null) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
        return false;
    }

    final void activityPausedLocked(IBinder iBinder, boolean z) {
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Activity paused: token=" + iBinder + ", timeout=" + z);
        }
        ActivityRecord activityRecordIsInStackLocked = isInStackLocked(iBinder);
        if (activityRecordIsInStackLocked != null) {
            this.mHandler.removeMessages(101, activityRecordIsInStackLocked);
            if (this.mPausingActivity == activityRecordIsInStackLocked) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    String str = TAG_STATES;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Moving to PAUSED: ");
                    sb.append(activityRecordIsInStackLocked);
                    sb.append(z ? " (due to timeout)" : " (pause complete)");
                    Slog.v(str, sb.toString());
                }
                this.mService.mWindowManager.deferSurfaceLayout();
                try {
                    completePauseLocked(true, null);
                    return;
                } finally {
                    this.mService.mWindowManager.continueSurfaceLayout();
                }
            }
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(activityRecordIsInStackLocked.userId);
            objArr[1] = Integer.valueOf(System.identityHashCode(activityRecordIsInStackLocked));
            objArr[2] = activityRecordIsInStackLocked.shortComponentName;
            objArr[3] = this.mPausingActivity != null ? this.mPausingActivity.shortComponentName : "(none)";
            EventLog.writeEvent(EventLogTags.AM_FAILED_TO_PAUSE, objArr);
            if (activityRecordIsInStackLocked.isState(ActivityState.PAUSING)) {
                activityRecordIsInStackLocked.setState(ActivityState.PAUSED, "activityPausedLocked");
                if (activityRecordIsInStackLocked.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG, "Executing finish of failed to pause activity: " + activityRecordIsInStackLocked);
                    }
                    finishCurrentActivityLocked(activityRecordIsInStackLocked, 2, false, "activityPausedLocked");
                }
            }
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
    }

    private void completePauseLocked(boolean z, ActivityRecord activityRecord) {
        ActivityRecord activityRecordFinishCurrentActivityLocked = this.mPausingActivity;
        if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
            Slog.v(TAG_PAUSE, "Complete pause: " + activityRecordFinishCurrentActivityLocked);
        }
        if (activityRecordFinishCurrentActivityLocked != null) {
            activityRecordFinishCurrentActivityLocked.setWillCloseOrEnterPip(false);
            boolean zIsState = activityRecordFinishCurrentActivityLocked.isState(ActivityState.STOPPING);
            activityRecordFinishCurrentActivityLocked.setState(ActivityState.PAUSED, "completePausedLocked");
            if (activityRecordFinishCurrentActivityLocked.finishing) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Executing finish of activity: " + activityRecordFinishCurrentActivityLocked);
                }
                activityRecordFinishCurrentActivityLocked = finishCurrentActivityLocked(activityRecordFinishCurrentActivityLocked, 2, false, "completedPausedLocked");
            } else if (activityRecordFinishCurrentActivityLocked.app != null) {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Enqueue pending stop if needed: " + activityRecordFinishCurrentActivityLocked + " wasStopping=" + zIsState + " visible=" + activityRecordFinishCurrentActivityLocked.visible);
                }
                if (this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(activityRecordFinishCurrentActivityLocked) && (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE)) {
                    Slog.v(TAG_PAUSE, "Complete pause, no longer waiting: " + activityRecordFinishCurrentActivityLocked);
                }
                if (activityRecordFinishCurrentActivityLocked.deferRelaunchUntilPaused) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Re-launching after pause: " + activityRecordFinishCurrentActivityLocked);
                    }
                    activityRecordFinishCurrentActivityLocked.relaunchActivityLocked(false, activityRecordFinishCurrentActivityLocked.preserveWindowOnDeferredRelaunch);
                } else if (zIsState) {
                    activityRecordFinishCurrentActivityLocked.setState(ActivityState.STOPPING, "completePausedLocked");
                } else if (!activityRecordFinishCurrentActivityLocked.visible || shouldSleepOrShutDownActivities()) {
                    activityRecordFinishCurrentActivityLocked.setDeferHidingClient(false);
                    addToStopping(activityRecordFinishCurrentActivityLocked, true, false);
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "App died during pause, not stopping: " + activityRecordFinishCurrentActivityLocked);
                }
                activityRecordFinishCurrentActivityLocked = null;
            }
            if (activityRecordFinishCurrentActivityLocked != null) {
                activityRecordFinishCurrentActivityLocked.stopFreezingScreenLocked(true);
            }
            this.mPausingActivity = null;
        }
        if (z) {
            ActivityStack focusedStack = this.mStackSupervisor.getFocusedStack();
            if (!focusedStack.shouldSleepOrShutDownActivities()) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked(focusedStack, activityRecordFinishCurrentActivityLocked, null);
            } else {
                checkReadyForSleep();
                ActivityRecord activityRecord2 = focusedStack.topRunningActivityLocked();
                if (activityRecord2 == null || (activityRecordFinishCurrentActivityLocked != null && activityRecord2 != activityRecordFinishCurrentActivityLocked)) {
                    this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            }
        }
        if (activityRecordFinishCurrentActivityLocked != null) {
            activityRecordFinishCurrentActivityLocked.resumeKeyDispatchingLocked();
            if (activityRecordFinishCurrentActivityLocked.app != null && activityRecordFinishCurrentActivityLocked.cpuTimeAtResume > 0 && this.mService.mBatteryStatsService.isOnBattery()) {
                long cpuTimeForPid = this.mService.mProcessCpuTracker.getCpuTimeForPid(activityRecordFinishCurrentActivityLocked.app.pid) - activityRecordFinishCurrentActivityLocked.cpuTimeAtResume;
                if (cpuTimeForPid > 0) {
                    BatteryStatsImpl activeStatistics = this.mService.mBatteryStatsService.getActiveStatistics();
                    synchronized (activeStatistics) {
                        BatteryStatsImpl.Uid.Proc processStatsLocked = activeStatistics.getProcessStatsLocked(activityRecordFinishCurrentActivityLocked.info.applicationInfo.uid, activityRecordFinishCurrentActivityLocked.info.packageName);
                        if (processStatsLocked != null) {
                            processStatsLocked.addForegroundTimeLocked(cpuTimeForPid);
                        }
                    }
                }
            }
            activityRecordFinishCurrentActivityLocked.cpuTimeAtResume = 0L;
        }
        if (this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause || (getDisplay() != null && getDisplay().hasPinnedStack())) {
            this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
            this.mStackSupervisor.mAppVisibilitiesChangedSinceLastPause = false;
        }
        this.mStackSupervisor.ensureActivitiesVisibleLocked(activityRecord, 0, false);
    }

    void addToStopping(ActivityRecord activityRecord, boolean z, boolean z2) {
        if (!this.mStackSupervisor.mStoppingActivities.contains(activityRecord)) {
            this.mStackSupervisor.mStoppingActivities.add(activityRecord);
        }
        boolean z3 = true;
        if (this.mStackSupervisor.mStoppingActivities.size() <= 3 && (!activityRecord.frontOfTask || this.mTaskHistory.size() > 1)) {
            z3 = false;
        }
        if (z || z3) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                String str = TAG_PAUSE;
                StringBuilder sb = new StringBuilder();
                sb.append("Scheduling idle now: forceIdle=");
                sb.append(z3);
                sb.append("immediate=");
                sb.append(!z2);
                Slog.v(str, sb.toString());
            }
            if (!z2) {
                this.mStackSupervisor.scheduleIdleLocked();
                return;
            } else {
                this.mStackSupervisor.scheduleIdleTimeoutLocked(activityRecord);
                return;
            }
        }
        checkReadyForSleep();
    }

    @VisibleForTesting
    boolean isStackTranslucent(ActivityRecord activityRecord) {
        if (!isAttached() || this.mForceHidden) {
            return true;
        }
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord2 = arrayList.get(size2);
                if (!activityRecord2.finishing && ((activityRecord2.visibleIgnoringKeyguard || activityRecord2 == activityRecord) && (activityRecord2.fullscreen || activityRecord2.hasWallpaper))) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean isTopStackOnDisplay() {
        ActivityDisplay display = getDisplay();
        return display != null && display.isTopStack(this);
    }

    boolean isTopActivityVisible() {
        ActivityRecord topActivity = getTopActivity();
        return topActivity != null && topActivity.visible;
    }

    boolean shouldBeVisible(ActivityRecord activityRecord) {
        if (!isAttached() || this.mForceHidden) {
            return false;
        }
        if (this.mStackSupervisor.isFocusedStack(this)) {
            return true;
        }
        if (topRunningActivityLocked() == null && isInStackLocked(activityRecord) == null && !isTopStackOnDisplay()) {
            return false;
        }
        ConfigurationContainer display = getDisplay();
        int windowingMode = getWindowingMode();
        boolean zIsActivityTypeAssistant = isActivityTypeAssistant();
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        for (int childCount = display.getChildCount() - 1; childCount >= 0; childCount--) {
            ActivityStack childAt = display.getChildAt(childCount);
            if (childAt == this) {
                return true;
            }
            int windowingMode2 = childAt.getWindowingMode();
            if (windowingMode2 == 1) {
                int activityType = childAt.getActivityType();
                if (windowingMode == 3 && (activityType == 2 || (activityType == 4 && this.mWindowManager.getRecentsAnimationController() != null))) {
                    return true;
                }
                if (!childAt.isStackTranslucent(activityRecord)) {
                    return false;
                }
            } else {
                if (windowingMode2 == 3 && !z) {
                    z = !childAt.isStackTranslucent(activityRecord);
                    if (windowingMode == 3 && z) {
                        return false;
                    }
                } else {
                    if (windowingMode2 == 4 && !z2) {
                        z2 = !childAt.isStackTranslucent(activityRecord);
                        if (windowingMode == 4 && z2) {
                            return false;
                        }
                    }
                    if (!z && z2) {
                        return false;
                    }
                    if (zIsActivityTypeAssistant && z3) {
                        return false;
                    }
                }
                z3 = true;
                if (!z) {
                }
                if (zIsActivityTypeAssistant) {
                    continue;
                }
            }
        }
        return true;
    }

    final int rankTaskLayers(int i) {
        int i2 = 0;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            ActivityRecord activityRecord = taskRecord.topRunningActivityLocked();
            if (activityRecord == null || activityRecord.finishing || !activityRecord.visible) {
                taskRecord.mLayerRank = -1;
            } else {
                taskRecord.mLayerRank = i2 + i;
                i2++;
            }
        }
        return i2;
    }

    final void ensureActivitiesVisibleLocked(ActivityRecord activityRecord, int i, boolean z) {
        ensureActivitiesVisibleLocked(activityRecord, i, z, true);
    }

    final void ensureActivitiesVisibleLocked(ActivityRecord activityRecord, int i, boolean z, boolean z2) {
        ArrayList<ActivityRecord> arrayList;
        TaskRecord taskRecord;
        boolean z3;
        boolean z4;
        ActivityRecord activityRecord2;
        int size;
        ActivityRecord activityRecord3 = activityRecord;
        boolean z5 = z2;
        boolean z6 = false;
        this.mTopActivityOccludesKeyguard = false;
        this.mTopDismissingKeyguardActivity = null;
        this.mStackSupervisor.getKeyguardController().beginActivityVisibilityUpdate();
        try {
            ActivityRecord activityRecord4 = topRunningActivityLocked();
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "ensureActivitiesVisible behind " + activityRecord4 + " configChanges=0x" + Integer.toHexString(i));
            }
            if (activityRecord4 != null) {
                checkTranslucentActivityWaiting(activityRecord4);
            }
            boolean z7 = true;
            boolean z8 = activityRecord4 != null;
            boolean zShouldBeVisible = shouldBeVisible(activityRecord);
            boolean zUpdateBehindFullscreen = !zShouldBeVisible;
            boolean z9 = this.mStackSupervisor.isFocusedStack(this) && isInStackLocked(activityRecord) == null;
            boolean z10 = isAttached() && getDisplay().isTopNotPinnedStack(this);
            int size2 = this.mTaskHistory.size() - 1;
            boolean z11 = z9;
            int i2 = i;
            while (size2 >= 0) {
                TaskRecord taskRecord2 = this.mTaskHistory.get(size2);
                ArrayList<ActivityRecord> arrayList2 = taskRecord2.mActivities;
                boolean z12 = z11;
                int size3 = arrayList2.size() - 1;
                int i3 = i2;
                while (size3 >= 0) {
                    ActivityRecord activityRecord5 = arrayList2.get(size3);
                    if (!activityRecord5.finishing) {
                        boolean z13 = activityRecord5 == activityRecord4 ? z7 : z6;
                        if (!z8 || z13) {
                            boolean zShouldBeVisibleIgnoringKeyguard = activityRecord5.shouldBeVisibleIgnoringKeyguard(zUpdateBehindFullscreen);
                            activityRecord5.visibleIgnoringKeyguard = zShouldBeVisibleIgnoringKeyguard;
                            if (z13 && z10) {
                                z6 = z7;
                            }
                            boolean zCheckKeyguardVisibility = checkKeyguardVisibility(activityRecord5, zShouldBeVisibleIgnoringKeyguard, z6);
                            if (zShouldBeVisibleIgnoringKeyguard) {
                                zUpdateBehindFullscreen = updateBehindFullscreen(!zShouldBeVisible, zUpdateBehindFullscreen, activityRecord5);
                            }
                            if (zCheckKeyguardVisibility) {
                                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                    Slog.v(TAG_VISIBILITY, "Make visible? " + activityRecord5 + " finishing=" + activityRecord5.finishing + " state=" + activityRecord5.getState());
                                }
                                if (activityRecord5 != activityRecord3 && z5) {
                                    activityRecord5.ensureActivityConfiguration(0, z, true);
                                }
                                if (activityRecord5.app == null || activityRecord5.app.thread == null) {
                                    z4 = zUpdateBehindFullscreen;
                                    activityRecord2 = activityRecord5;
                                    size = size3;
                                    boolean z14 = z13;
                                    arrayList = arrayList2;
                                    taskRecord = taskRecord2;
                                    if (!makeVisibleAndRestartIfNeeded(activityRecord3, i3, z14, z12, activityRecord2)) {
                                        z3 = true;
                                    } else if (size >= arrayList.size()) {
                                        z3 = true;
                                        size = arrayList.size() - 1;
                                    } else {
                                        z3 = true;
                                        size3 = size;
                                        z12 = false;
                                        i3 |= activityRecord2.configChangeFlags;
                                    }
                                    size3 = size;
                                    i3 |= activityRecord2.configChangeFlags;
                                } else {
                                    if (activityRecord5.visible) {
                                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                            Slog.v(TAG_VISIBILITY, "Skipping: already visible at " + activityRecord5);
                                        }
                                        if (activityRecord5.mClientVisibilityDeferred && z5) {
                                            activityRecord5.makeClientVisible();
                                        }
                                        if (activityRecord5.handleAlreadyVisible()) {
                                            z4 = zUpdateBehindFullscreen;
                                            activityRecord2 = activityRecord5;
                                            arrayList = arrayList2;
                                            taskRecord = taskRecord2;
                                            z3 = true;
                                            z12 = false;
                                            i3 |= activityRecord2.configChangeFlags;
                                        }
                                    } else {
                                        activityRecord5.makeVisibleIfNeeded(activityRecord3, z5);
                                    }
                                    z4 = zUpdateBehindFullscreen;
                                    activityRecord2 = activityRecord5;
                                    size = size3;
                                    arrayList = arrayList2;
                                    taskRecord = taskRecord2;
                                    z3 = true;
                                    size3 = size;
                                    i3 |= activityRecord2.configChangeFlags;
                                }
                            } else {
                                int i4 = size3;
                                arrayList = arrayList2;
                                taskRecord = taskRecord2;
                                z3 = z7;
                                z4 = zUpdateBehindFullscreen;
                                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                                    Slog.v(TAG_VISIBILITY, "Make invisible? " + activityRecord5 + " finishing=" + activityRecord5.finishing + " state=" + activityRecord5.getState() + " stackShouldBeVisible=" + zShouldBeVisible + " behindFullscreenActivity=" + z4 + " mLaunchTaskBehind=" + activityRecord5.mLaunchTaskBehind);
                                }
                                makeInvisible(activityRecord5);
                                size3 = i4;
                            }
                            zUpdateBehindFullscreen = z4;
                            z8 = false;
                        }
                        size3--;
                        z7 = z3;
                        taskRecord2 = taskRecord;
                        arrayList2 = arrayList;
                        activityRecord3 = activityRecord;
                        z5 = z2;
                        z6 = false;
                    }
                    arrayList = arrayList2;
                    taskRecord = taskRecord2;
                    z3 = z7;
                    size3--;
                    z7 = z3;
                    taskRecord2 = taskRecord;
                    arrayList2 = arrayList;
                    activityRecord3 = activityRecord;
                    z5 = z2;
                    z6 = false;
                }
                TaskRecord taskRecord3 = taskRecord2;
                boolean z15 = z7;
                if (getWindowingMode() == 5) {
                    zUpdateBehindFullscreen = !zShouldBeVisible;
                } else if (isActivityTypeHome()) {
                    if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                        Slog.v(TAG_VISIBILITY, "Home task: at " + taskRecord3 + " stackShouldBeVisible=" + zShouldBeVisible + " behindFullscreenActivity=" + zUpdateBehindFullscreen);
                    }
                    zUpdateBehindFullscreen = z15;
                }
                size2--;
                z7 = z15;
                i2 = i3;
                z11 = z12;
                activityRecord3 = activityRecord;
                z5 = z2;
                z6 = false;
            }
            if (this.mTranslucentActivityWaiting != null && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty()) {
                notifyActivityDrawnLocked(null);
            }
        } finally {
            this.mStackSupervisor.getKeyguardController().endActivityVisibilityUpdate();
        }
    }

    void addStartingWindowsForVisibleActivities(boolean z) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            this.mTaskHistory.get(size).addStartingWindowsForVisibleActivities(z);
        }
    }

    boolean topActivityOccludesKeyguard() {
        return this.mTopActivityOccludesKeyguard;
    }

    boolean resizeStackWithLaunchBounds() {
        return inPinnedWindowingMode();
    }

    @Override
    public boolean supportsSplitScreenWindowingMode() {
        TaskRecord taskRecord = topTask();
        return super.supportsSplitScreenWindowingMode() && (taskRecord == null || taskRecord.supportsSplitScreenWindowingMode());
    }

    boolean affectedBySplitScreenResize() {
        int windowingMode;
        return (!supportsSplitScreenWindowingMode() || (windowingMode = getWindowingMode()) == 5 || windowingMode == 2) ? false : true;
    }

    ActivityRecord getTopDismissingKeyguardActivity() {
        return this.mTopDismissingKeyguardActivity;
    }

    boolean checkKeyguardVisibility(ActivityRecord activityRecord, boolean z, boolean z2) {
        boolean zIsKeyguardOrAodShowing = this.mStackSupervisor.getKeyguardController().isKeyguardOrAodShowing(this.mDisplayId != -1 ? this.mDisplayId : 0);
        boolean zIsKeyguardLocked = this.mStackSupervisor.getKeyguardController().isKeyguardLocked();
        boolean zCanShowWhenLocked = activityRecord.canShowWhenLocked();
        boolean zHasDismissKeyguardWindows = activityRecord.hasDismissKeyguardWindows();
        if (z) {
            if (zHasDismissKeyguardWindows && this.mTopDismissingKeyguardActivity == null) {
                this.mTopDismissingKeyguardActivity = activityRecord;
            }
            if (z2) {
                this.mTopActivityOccludesKeyguard |= zCanShowWhenLocked;
            }
            if (canShowWithInsecureKeyguard() && this.mStackSupervisor.getKeyguardController().canDismissKeyguard()) {
                return true;
            }
        }
        if (zIsKeyguardOrAodShowing) {
            return z && this.mStackSupervisor.getKeyguardController().canShowActivityWhileKeyguardShowing(activityRecord, zHasDismissKeyguardWindows);
        }
        if (zIsKeyguardLocked) {
            return z && this.mStackSupervisor.getKeyguardController().canShowWhileOccluded(zHasDismissKeyguardWindows, zCanShowWhenLocked);
        }
        return z;
    }

    private boolean canShowWithInsecureKeyguard() {
        ActivityDisplay display = getDisplay();
        if (display != null) {
            return (display.mDisplay.getFlags() & 32) != 0;
        }
        throw new IllegalStateException("Stack is not attached to any display, stackId=" + this.mStackId);
    }

    private void checkTranslucentActivityWaiting(ActivityRecord activityRecord) {
        if (this.mTranslucentActivityWaiting != activityRecord) {
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            if (this.mTranslucentActivityWaiting != null) {
                notifyActivityDrawnLocked(null);
                this.mTranslucentActivityWaiting = null;
            }
            this.mHandler.removeMessages(106);
        }
    }

    private boolean makeVisibleAndRestartIfNeeded(ActivityRecord activityRecord, int i, boolean z, boolean z2, ActivityRecord activityRecord2) {
        if (z || !activityRecord2.visible) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Start and freeze screen for " + activityRecord2);
            }
            if (activityRecord2 != activityRecord) {
                activityRecord2.startFreezingScreenLocked(activityRecord2.app, i);
            }
            if (!activityRecord2.visible || activityRecord2.mLaunchTaskBehind) {
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG_VISIBILITY, "Starting and making visible: " + activityRecord2);
                }
                activityRecord2.setVisible(true);
            }
            if (activityRecord2 != activityRecord) {
                this.mStackSupervisor.startSpecificActivityLocked(activityRecord2, z2, false);
                return true;
            }
        }
        return false;
    }

    private void makeInvisible(ActivityRecord activityRecord) {
        if (!activityRecord.visible) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Already invisible: " + activityRecord);
            }
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making invisible: " + activityRecord + " " + activityRecord.getState());
        }
        try {
            boolean zCheckEnterPictureInPictureState = activityRecord.checkEnterPictureInPictureState("makeInvisible", true);
            activityRecord.setDeferHidingClient(zCheckEnterPictureInPictureState && !activityRecord.isState(ActivityState.STOPPING, ActivityState.STOPPED, ActivityState.PAUSED));
            activityRecord.setVisible(false);
            switch (AnonymousClass1.$SwitchMap$com$android$server$am$ActivityStack$ActivityState[activityRecord.getState().ordinal()]) {
                case 1:
                case 2:
                    if (activityRecord.app != null && activityRecord.app.thread != null) {
                        if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                            Slog.v(TAG_VISIBILITY, "Scheduling invisibility: " + activityRecord);
                        }
                        this.mService.getLifecycleManager().scheduleTransaction(activityRecord.app.thread, (IBinder) activityRecord.appToken, (ClientTransactionItem) WindowVisibilityItem.obtain(false));
                    }
                    activityRecord.supportsEnterPipOnTaskSwitch = false;
                    break;
                case 3:
                case 4:
                case 5:
                case 6:
                    addToStopping(activityRecord, true, zCheckEnterPictureInPictureState);
                    break;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making hidden: " + activityRecord.intent.getComponent(), e);
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$am$ActivityStack$ActivityState = new int[ActivityState.values().length];

        static {
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.STOPPING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.STOPPED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.INITIALIZING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.RESUMED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.PAUSING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$am$ActivityStack$ActivityState[ActivityState.PAUSED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private boolean updateBehindFullscreen(boolean z, boolean z2, ActivityRecord activityRecord) {
        if (activityRecord.fullscreen) {
            if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Fullscreen: at " + activityRecord + " stackInvisible=" + z + " behindFullscreenActivity=" + z2);
            }
            return true;
        }
        return z2;
    }

    void convertActivityToTranslucent(ActivityRecord activityRecord) {
        this.mTranslucentActivityWaiting = activityRecord;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.sendEmptyMessageDelayed(106, TRANSLUCENT_CONVERSION_TIMEOUT);
    }

    void clearOtherAppTimeTrackers(AppTimeTracker appTimeTracker) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (activityRecord.appTimeTracker != appTimeTracker) {
                    activityRecord.appTimeTracker = null;
                }
            }
        }
    }

    void notifyActivityDrawnLocked(ActivityRecord activityRecord) {
        if (activityRecord == null || (this.mUndrawnActivitiesBelowTopTranslucent.remove(activityRecord) && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty())) {
            ActivityRecord activityRecord2 = this.mTranslucentActivityWaiting;
            this.mTranslucentActivityWaiting = null;
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            this.mHandler.removeMessages(106);
            if (activityRecord2 != null) {
                this.mWindowManager.setWindowOpaque(activityRecord2.appToken, false);
                if (activityRecord2.app != null && activityRecord2.app.thread != null) {
                    try {
                        activityRecord2.app.thread.scheduleTranslucentConversionComplete(activityRecord2.appToken, activityRecord != null);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    void cancelInitializingActivities() {
        boolean z;
        boolean z2;
        boolean z3;
        ActivityRecord activityRecord = topRunningActivityLocked();
        if (shouldBeVisible(null)) {
            z = false;
            z2 = true;
        } else {
            z2 = false;
            z = true;
        }
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord2 = arrayList.get(size2);
                if (z2) {
                    if (activityRecord2 == activityRecord) {
                        z2 = false;
                    }
                    z3 = activityRecord2.fullscreen;
                } else {
                    activityRecord2.removeOrphanedStartingWindow(z);
                    z3 = activityRecord2.fullscreen;
                }
                z |= z3;
            }
        }
    }

    @GuardedBy("mService")
    boolean resumeTopActivityUncheckedLocked(ActivityRecord activityRecord, ActivityOptions activityOptions) {
        if (this.mStackSupervisor.inResumeTopActivity) {
            return false;
        }
        try {
            this.mStackSupervisor.inResumeTopActivity = true;
            boolean zResumeTopActivityInnerLocked = resumeTopActivityInnerLocked(activityRecord, activityOptions);
            ActivityRecord activityRecord2 = topRunningActivityLocked(true);
            if (activityRecord2 == null || !activityRecord2.canTurnScreenOn()) {
                checkReadyForSleep();
            }
            return zResumeTopActivityInnerLocked;
        } finally {
            this.mStackSupervisor.inResumeTopActivity = false;
        }
    }

    protected ActivityRecord getResumedActivity() {
        return this.mResumedActivity;
    }

    private void setResumedActivity(ActivityRecord activityRecord, String str) {
        if (this.mResumedActivity == activityRecord) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_STACK, "setResumedActivity stack:" + this + " + from: " + this.mResumedActivity + " to:" + activityRecord + " reason:" + str);
        }
        this.mResumedActivity = activityRecord;
    }

    @GuardedBy("mService")
    private boolean resumeTopActivityInnerLocked(ActivityRecord activityRecord, ActivityOptions activityOptions) {
        boolean z;
        ActivityRecord activityRecord2;
        boolean z2;
        ActivityRecord activityRecord3;
        boolean z3;
        int i;
        if (!this.mService.mBooting && !this.mService.mBooted) {
            return false;
        }
        ActivityRecord activityRecord4 = topRunningActivityLocked(true);
        boolean z4 = activityRecord4 != null;
        if (z4 && !isAttached()) {
            return false;
        }
        this.mStackSupervisor.cancelInitializingActivities();
        boolean z5 = this.mStackSupervisor.mUserLeaving;
        this.mStackSupervisor.mUserLeaving = false;
        if (!z4) {
            return resumeTopActivityInNextFocusableStack(activityRecord, activityOptions, "noMoreActivities");
        }
        activityRecord4.delayedResume = false;
        if (this.mResumedActivity == activityRecord4 && activityRecord4.isState(ActivityState.RESUMED) && this.mStackSupervisor.allResumedActivitiesComplete()) {
            executeAppTransition(activityOptions);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed " + activityRecord4);
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            return false;
        }
        if (shouldSleepOrShutDownActivities() && this.mLastPausedActivity == activityRecord4 && this.mStackSupervisor.allPausedActivitiesComplete()) {
            executeAppTransition(activityOptions);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Going to sleep and all paused");
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            return false;
        }
        if (!this.mService.mUserController.hasStartedUserState(activityRecord4.userId)) {
            Slog.w(TAG, "Skipping resume of top activity " + activityRecord4 + ": user " + activityRecord4.userId + " is stopped");
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            return false;
        }
        this.mStackSupervisor.mStoppingActivities.remove(activityRecord4);
        this.mStackSupervisor.mGoingToSleepActivities.remove(activityRecord4);
        activityRecord4.sleeping = false;
        this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(activityRecord4);
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "Resuming " + activityRecord4);
        }
        if (!this.mStackSupervisor.allPausedActivitiesComplete()) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_PAUSE, "resumeTopActivityLocked: Skip resume: some activity pausing.");
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            return false;
        }
        this.mStackSupervisor.setLaunchSource(activityRecord4.info.applicationInfo.uid);
        ActivityStack lastStack = this.mStackSupervisor.getLastStack();
        if (lastStack == null || lastStack == this) {
            z = false;
            activityRecord2 = null;
        } else {
            activityRecord2 = lastStack.mResumedActivity;
            if (z5 && inMultiWindowMode() && lastStack.shouldBeVisible(activityRecord4)) {
                if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                    Slog.i(TAG_USER_LEAVING, "Overriding userLeaving to false next=" + activityRecord4 + " lastResumed=" + activityRecord2);
                }
                z5 = false;
            }
            z = activityRecord2 != null && activityRecord2.checkEnterPictureInPictureState("resumeTopActivity", z5);
        }
        boolean z6 = ((activityRecord4.info.flags & 16384) == 0 || z) ? false : true;
        boolean zPauseBackStacks = this.mStackSupervisor.pauseBackStacks(z5, activityRecord4, false);
        if (this.mResumedActivity != null) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Pausing " + this.mResumedActivity);
            }
            zPauseBackStacks |= startPausingLocked(z5, false, activityRecord4, false);
        }
        this.mService.mAmsExt.onBeforeActivitySwitch(this.mService.mLastResumedActivity, activityRecord4, zPauseBackStacks, activityRecord4.getActivityType());
        if (zPauseBackStacks && !z6) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "resumeTopActivityLocked: Skip resume: need to start pausing");
            }
            if (activityRecord4.app != null && activityRecord4.app.thread != null) {
                this.mService.updateLruProcessLocked(activityRecord4.app, true, null);
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            if (activityRecord2 != null) {
                activityRecord2.setWillCloseOrEnterPip(true);
            }
            return true;
        }
        if (this.mResumedActivity == activityRecord4 && activityRecord4.isState(ActivityState.RESUMED) && this.mStackSupervisor.allResumedActivitiesComplete()) {
            executeAppTransition(activityOptions);
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed (dontWaitForPause) " + activityRecord4);
            }
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                this.mStackSupervisor.validateTopActivitiesLocked();
            }
            return true;
        }
        if (shouldSleepActivities() && this.mLastNoHistoryActivity != null && !this.mLastNoHistoryActivity.finishing) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "no-history finish of " + this.mLastNoHistoryActivity + " on new resume");
            }
            requestFinishActivityLocked(this.mLastNoHistoryActivity.appToken, 0, null, "resume-no-history", false);
            this.mLastNoHistoryActivity = null;
        }
        if (activityRecord != null && activityRecord != activityRecord4) {
            if (!this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord) && activityRecord4 != null && !activityRecord4.nowVisible) {
                this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.add(activityRecord);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Resuming top, waiting visible to hide: " + activityRecord);
                }
            } else if (activityRecord.finishing) {
                activityRecord.setVisibility(false);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Not waiting for visible to hide: " + activityRecord + ", waitingVisible=" + this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord) + ", nowVisible=" + activityRecord4.nowVisible);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Previous already visible but still waiting to hide: " + activityRecord + ", waitingVisible=" + this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord) + ", nowVisible=" + activityRecord4.nowVisible);
            }
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(activityRecord4.packageName, false, activityRecord4.userId);
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Failed trying to unstop package " + activityRecord4.packageName + ": " + e2);
        }
        int i2 = 6;
        if (activityRecord != null) {
            if (activityRecord.finishing) {
                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                    Slog.v(TAG_TRANSITION, "Prepare close transition: prev=" + activityRecord);
                }
                if (this.mStackSupervisor.mNoAnimActivities.contains(activityRecord)) {
                    this.mWindowManager.prepareAppTransition(0, false);
                    z2 = false;
                } else {
                    WindowManagerService windowManagerService = this.mWindowManager;
                    if (activityRecord.getTask() == activityRecord4.getTask()) {
                        i = 7;
                    } else {
                        i = 9;
                    }
                    windowManagerService.prepareAppTransition(i, false);
                    z2 = true;
                }
                activityRecord.setVisibility(false);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                    Slog.v(TAG_TRANSITION, "Prepare open transition: prev=" + activityRecord);
                }
                if (this.mStackSupervisor.mNoAnimActivities.contains(activityRecord4)) {
                    this.mWindowManager.prepareAppTransition(0, false);
                    z2 = false;
                } else {
                    WindowManagerService windowManagerService2 = this.mWindowManager;
                    if (activityRecord.getTask() != activityRecord4.getTask()) {
                        if (activityRecord4.mLaunchTaskBehind) {
                            i2 = 16;
                        } else {
                            i2 = 8;
                        }
                    }
                    windowManagerService2.prepareAppTransition(i2, false);
                    z2 = true;
                }
            }
        } else {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: no previous");
            }
            if (!this.mStackSupervisor.mNoAnimActivities.contains(activityRecord4)) {
                this.mWindowManager.prepareAppTransition(6, false);
                z2 = true;
            } else {
                this.mWindowManager.prepareAppTransition(0, false);
                z2 = false;
            }
        }
        if (z2) {
            activityRecord4.applyOptionsLocked();
        } else {
            activityRecord4.clearOptionsLocked();
        }
        this.mStackSupervisor.mNoAnimActivities.clear();
        ActivityStack lastStack2 = this.mStackSupervisor.getLastStack();
        if (activityRecord4.app != null && activityRecord4.app.thread != null) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Resume running: " + activityRecord4 + " stopped=" + activityRecord4.stopped + " visible=" + activityRecord4.visible);
            }
            boolean z7 = lastStack2 != null && (lastStack2.inMultiWindowMode() || !(lastStack2.mLastPausedActivity == null || lastStack2.mLastPausedActivity.fullscreen));
            synchronized (this.mWindowManager.getWindowManagerLock()) {
                if (!activityRecord4.visible || activityRecord4.stopped || z7) {
                    activityRecord4.setVisibility(true);
                }
                activityRecord4.startLaunchTickingLocked();
                if (lastStack2 != null) {
                    activityRecord3 = lastStack2.mResumedActivity;
                } else {
                    activityRecord3 = null;
                }
                ActivityState state = activityRecord4.getState();
                this.mService.updateCpuStats();
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to RESUMED: " + activityRecord4 + " (in existing)");
                }
                activityRecord4.setState(ActivityState.RESUMED, "resumeTopActivityInnerLocked");
                this.mService.mAmsExt.onAfterActivityResumed(activityRecord4);
                this.mService.updateLruProcessLocked(activityRecord4.app, true, null);
                updateLRUListLocked(activityRecord4);
                ActivityManagerService.MainHandler mainHandler = this.mService.mHandler;
                ActivityManagerService activityManagerService = this.mService;
                Objects.requireNonNull(activityManagerService);
                mainHandler.post(new $$Lambda$ejtzn5TCL2GSsOkwaLFeot_Ozqg(activityManagerService));
                if (!this.mStackSupervisor.isFocusedStack(this)) {
                    z3 = true;
                } else {
                    z3 = !this.mStackSupervisor.ensureVisibilityAndConfig(activityRecord4, this.mDisplayId, true, false);
                }
                if (z3) {
                    ActivityRecord activityRecord5 = topRunningActivityLocked();
                    if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.i(TAG_STATES, "Activity config changed during resume: " + activityRecord4 + ", new next: " + activityRecord5);
                    }
                    if (activityRecord5 != activityRecord4) {
                        this.mStackSupervisor.scheduleResumeTopActivities();
                    }
                    if (!activityRecord4.visible || activityRecord4.stopped) {
                        activityRecord4.setVisibility(true);
                    }
                    activityRecord4.completeResumeLocked();
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return true;
                }
                try {
                    ClientTransaction clientTransactionObtain = ClientTransaction.obtain(activityRecord4.app.thread, activityRecord4.appToken);
                    ArrayList<ResultInfo> arrayList = activityRecord4.results;
                    if (arrayList != null) {
                        int size = arrayList.size();
                        if (!activityRecord4.finishing && size > 0) {
                            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                                Slog.v(TAG_RESULTS, "Delivering results to " + activityRecord4 + ": " + arrayList);
                            }
                            clientTransactionObtain.addCallback(ActivityResultItem.obtain(arrayList));
                        }
                    }
                    if (activityRecord4.newIntents != null) {
                        clientTransactionObtain.addCallback(NewIntentItem.obtain(activityRecord4.newIntents, false));
                    }
                    activityRecord4.notifyAppResumed(activityRecord4.stopped);
                    EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, Integer.valueOf(activityRecord4.userId), Integer.valueOf(System.identityHashCode(activityRecord4)), Integer.valueOf(activityRecord4.getTask().taskId), activityRecord4.shortComponentName);
                    activityRecord4.sleeping = false;
                    this.mService.getAppWarningsLocked().onResumeActivity(activityRecord4);
                    this.mService.showAskCompatModeDialogLocked(activityRecord4);
                    activityRecord4.app.pendingUiClean = true;
                    activityRecord4.app.forceProcessStateUpTo(this.mService.mTopProcessState);
                    activityRecord4.clearOptionsLocked();
                    clientTransactionObtain.setLifecycleStateRequest(ResumeActivityItem.obtain(activityRecord4.app.repProcState, this.mService.isNextTransitionForward()));
                    this.mService.getLifecycleManager().scheduleTransaction(clientTransactionObtain);
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.d(TAG_STATES, "resumeTopActivityLocked: Resumed " + activityRecord4);
                    }
                    try {
                        activityRecord4.completeResumeLocked();
                    } catch (Exception e3) {
                        Slog.w(TAG, "Exception thrown during resume of " + activityRecord4, e3);
                        requestFinishActivityLocked(activityRecord4.appToken, 0, null, "resume-exception", true);
                        if (ActivityManagerDebugConfig.DEBUG_STACK) {
                            this.mStackSupervisor.validateTopActivitiesLocked();
                        }
                        return true;
                    }
                } catch (Exception e4) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                        Slog.v(TAG_STATES, "Resume failed; resetting state to " + state + ": " + activityRecord4);
                    }
                    activityRecord4.setState(state, "resumeTopActivityInnerLocked");
                    if (activityRecord3 != null) {
                        activityRecord3.setState(ActivityState.RESUMED, "resumeTopActivityInnerLocked");
                    }
                    Slog.i(TAG, "Restarting because process died: " + activityRecord4);
                    if (!activityRecord4.hasBeenLaunched) {
                        activityRecord4.hasBeenLaunched = true;
                    } else if (lastStack2 != null && lastStack2.isTopStackOnDisplay()) {
                        activityRecord4.showStartingWindow(null, false, false);
                    }
                    this.mStackSupervisor.startSpecificActivityLocked(activityRecord4, true, false);
                    if (ActivityManagerDebugConfig.DEBUG_STACK) {
                        this.mStackSupervisor.validateTopActivitiesLocked();
                    }
                    return true;
                }
            }
        } else {
            if (!activityRecord4.hasBeenLaunched) {
                activityRecord4.hasBeenLaunched = true;
            } else {
                activityRecord4.showStartingWindow(null, false, false);
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.v(TAG_SWITCH, "Restarting: " + activityRecord4);
                }
            }
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "resumeTopActivityLocked: Restarting " + activityRecord4);
            }
            this.mStackSupervisor.startSpecificActivityLocked(activityRecord4, true, true);
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            this.mStackSupervisor.validateTopActivitiesLocked();
        }
        return true;
    }

    private boolean resumeTopActivityInNextFocusableStack(ActivityRecord activityRecord, ActivityOptions activityOptions, String str) {
        if (adjustFocusToNextFocusableStack(str)) {
            return this.mStackSupervisor.resumeFocusedStackTopActivityLocked(this.mStackSupervisor.getFocusedStack(), activityRecord, null);
        }
        ActivityOptions.abort(activityOptions);
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.d(TAG_STATES, "resumeTopActivityInNextFocusableStack: " + str + ", go home");
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            this.mStackSupervisor.validateTopActivitiesLocked();
        }
        return isOnHomeDisplay() && this.mStackSupervisor.resumeHomeStackTask(activityRecord, str);
    }

    private TaskRecord getNextTask(TaskRecord taskRecord) {
        TaskRecord taskRecord2;
        int iIndexOf = this.mTaskHistory.indexOf(taskRecord);
        if (iIndexOf >= 0) {
            int size = this.mTaskHistory.size();
            do {
                iIndexOf++;
                if (iIndexOf < size) {
                    taskRecord2 = this.mTaskHistory.get(iIndexOf);
                } else {
                    return null;
                }
            } while (taskRecord2.userId != taskRecord.userId);
            return taskRecord2;
        }
        return null;
    }

    int getAdjustedPositionForTask(TaskRecord taskRecord, int i, ActivityRecord activityRecord) {
        int size = this.mTaskHistory.size();
        if ((activityRecord != null && activityRecord.okToShowLocked()) || (activityRecord == null && taskRecord.okToShowLocked())) {
            return Math.min(i, size);
        }
        while (size > 0) {
            TaskRecord taskRecord2 = this.mTaskHistory.get(size - 1);
            if (!this.mStackSupervisor.isCurrentProfileLocked(taskRecord2.userId) || taskRecord2.topRunningActivityLocked() == null) {
                break;
            }
            size--;
        }
        return Math.min(i, size);
    }

    private void insertTaskAtPosition(TaskRecord taskRecord, int i) {
        if (i >= this.mTaskHistory.size()) {
            insertTaskAtTop(taskRecord, null);
            return;
        }
        if (i <= 0) {
            insertTaskAtBottom(taskRecord);
            return;
        }
        int adjustedPositionForTask = getAdjustedPositionForTask(taskRecord, i, null);
        this.mTaskHistory.remove(taskRecord);
        this.mTaskHistory.add(adjustedPositionForTask, taskRecord);
        this.mWindowContainerController.positionChildAt(taskRecord.getWindowContainerController(), adjustedPositionForTask);
        updateTaskMovement(taskRecord, true);
    }

    private void insertTaskAtTop(TaskRecord taskRecord, ActivityRecord activityRecord) {
        this.mTaskHistory.remove(taskRecord);
        this.mTaskHistory.add(getAdjustedPositionForTask(taskRecord, this.mTaskHistory.size(), activityRecord), taskRecord);
        updateTaskMovement(taskRecord, true);
        this.mWindowContainerController.positionChildAtTop(taskRecord.getWindowContainerController(), true);
    }

    private void insertTaskAtBottom(TaskRecord taskRecord) {
        this.mTaskHistory.remove(taskRecord);
        this.mTaskHistory.add(getAdjustedPositionForTask(taskRecord, 0, null), taskRecord);
        updateTaskMovement(taskRecord, true);
        this.mWindowContainerController.positionChildAtBottom(taskRecord.getWindowContainerController(), true);
    }

    void startActivityLocked(ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, boolean z2, ActivityOptions activityOptions) {
        TaskRecord taskRecord;
        boolean z3;
        TaskRecord task = activityRecord.getTask();
        int i = task.taskId;
        if (!activityRecord.mLaunchTaskBehind && (taskForIdLocked(i) == null || z)) {
            insertTaskAtTop(task, activityRecord);
        }
        ActivityRecord activityRecord3 = null;
        if (!z) {
            int size = this.mTaskHistory.size() - 1;
            taskRecord = null;
            boolean z4 = true;
            while (true) {
                if (size < 0) {
                    break;
                }
                taskRecord = this.mTaskHistory.get(size);
                if (taskRecord.getTopActivity() != null) {
                    if (taskRecord == task) {
                        if (!z4) {
                            if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                Slog.i(TAG, "Adding activity " + activityRecord + " to task " + taskRecord, new RuntimeException("here").fillInStackTrace());
                            }
                            activityRecord.createWindowContainer();
                            ActivityOptions.abort(activityOptions);
                            return;
                        }
                    } else if (taskRecord.numFullscreen > 0) {
                        z4 = false;
                    }
                }
                size--;
            }
        } else {
            taskRecord = null;
        }
        TaskRecord task2 = activityRecord.getTask();
        if (taskRecord == task2 && this.mTaskHistory.indexOf(taskRecord) != this.mTaskHistory.size() - 1) {
            this.mStackSupervisor.mUserLeaving = false;
            if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(TAG_USER_LEAVING, "startActivity() behind front, mUserLeaving=false");
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i(TAG, "Adding activity " + activityRecord + " to stack to task " + task2, new RuntimeException("here").fillInStackTrace());
        }
        if (activityRecord.getWindowContainerController() == null) {
            activityRecord.createWindowContainer();
        }
        task2.setFrontOfTask();
        if (!isHomeOrRecentsStack() || numActivities() > 0) {
            if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: starting " + activityRecord);
            }
            if ((activityRecord.intent.getFlags() & 65536) != 0) {
                this.mWindowManager.prepareAppTransition(0, z2);
                this.mStackSupervisor.mNoAnimActivities.add(activityRecord);
            } else {
                int i2 = 6;
                if (z) {
                    if (activityRecord.mLaunchTaskBehind) {
                        i2 = 16;
                    } else {
                        if (canEnterPipOnTaskSwitch(activityRecord2, null, activityRecord, activityOptions)) {
                            activityRecord2.supportsEnterPipOnTaskSwitch = true;
                        }
                        i2 = 8;
                    }
                }
                this.mWindowManager.prepareAppTransition(i2, z2);
                this.mStackSupervisor.mNoAnimActivities.remove(activityRecord);
            }
            if (z) {
                if ((activityRecord.intent.getFlags() & DumpState.DUMP_COMPILER_STATS) != 0) {
                    resetTaskIfNeededLocked(activityRecord, activityRecord);
                    if (topRunningNonDelayedActivityLocked(null) != activityRecord) {
                        z3 = false;
                    }
                }
                z3 = true;
            } else if (activityOptions == null || activityOptions.getAnimationType() != 5) {
                z3 = true;
            }
            if (activityRecord.mLaunchTaskBehind) {
                activityRecord.setVisibility(true);
                ensureActivitiesVisibleLocked(null, 0, false);
                return;
            } else {
                if (z3) {
                    TaskRecord task3 = activityRecord.getTask();
                    ActivityRecord activityRecord4 = task3.topRunningActivityWithStartingWindowLocked();
                    if (activityRecord4 == null || (activityRecord4.getTask() == task3 && !activityRecord4.nowVisible)) {
                        activityRecord3 = activityRecord4;
                    }
                    activityRecord.showStartingWindow(activityRecord3, z, isTaskSwitch(activityRecord, activityRecord2));
                    return;
                }
                return;
            }
        }
        ActivityOptions.abort(activityOptions);
    }

    private boolean canEnterPipOnTaskSwitch(ActivityRecord activityRecord, TaskRecord taskRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions) {
        if ((activityOptions != null && activityOptions.disallowEnterPictureInPictureWhileLaunching()) || activityRecord == null || activityRecord.inPinnedWindowingMode()) {
            return false;
        }
        ActivityStack stack = taskRecord != null ? taskRecord.getStack() : activityRecord2.getStack();
        if (stack != null && stack.isActivityTypeAssistant()) {
            return false;
        }
        return true;
    }

    private boolean isTaskSwitch(ActivityRecord activityRecord, ActivityRecord activityRecord2) {
        return (activityRecord2 == null || activityRecord.getTask() == activityRecord2.getTask()) ? false : true;
    }

    private ActivityOptions resetTargetTaskIfNeededLocked(TaskRecord taskRecord, boolean z) {
        ActivityOptions activityOptions;
        boolean z2;
        ActivityRecord activityRecord;
        TaskRecord taskRecordCreateTaskRecord;
        ?? r12;
        ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
        int size = arrayList.size();
        int iFindEffectiveRootIndex = taskRecord.findEffectiveRootIndex();
        int i = size - 1;
        ?? r17 = 1;
        ActivityOptions activityOptionsTakeOptionsLocked = null;
        int i2 = -1;
        while (i > iFindEffectiveRootIndex) {
            ActivityRecord activityRecord2 = arrayList.get(i);
            if (activityRecord2.frontOfTask) {
                break;
            }
            int i3 = activityRecord2.info.flags;
            boolean z3 = (i3 & 2) != 0;
            boolean z4 = (i3 & 64) != 0;
            boolean z5 = (activityRecord2.intent.getFlags() & DumpState.DUMP_FROZEN) != 0;
            if (z3 || z5 || activityRecord2.resultTo == null) {
                if (z3 || z5 || !z4 || activityRecord2.taskAffinity == null || activityRecord2.taskAffinity.equals(taskRecord.affinity)) {
                    r17 = r17;
                    if (z || z3 || z5) {
                        int i4 = i;
                        ?? r5 = r17;
                        ActivityOptions activityOptions2 = activityOptionsTakeOptionsLocked;
                        int size2 = z5 ? arrayList.size() - 1 : i2 < 0 ? i : i2;
                        ActivityOptions activityOptionsTakeOptionsLocked2 = activityOptions2;
                        ?? r172 = r17;
                        while (i4 <= size2) {
                            ActivityRecord activityRecord3 = arrayList.get(i4);
                            if (!activityRecord3.finishing) {
                                if (r5 == 0 || activityOptionsTakeOptionsLocked2 != null || (activityOptionsTakeOptionsLocked2 = activityRecord3.takeOptionsLocked()) == null) {
                                    activityOptions = activityOptionsTakeOptionsLocked2;
                                    z2 = r5 == true ? 1 : 0;
                                } else {
                                    activityOptions = activityOptionsTakeOptionsLocked2;
                                    z2 = false;
                                }
                                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                    Slog.w(TAG_TASKS, "resetTaskIntendedTask: calling finishActivity on " + activityRecord3);
                                }
                                if (finishActivityLocked(activityRecord3, 0, null, "reset-task", false)) {
                                    size2--;
                                    i4--;
                                }
                                activityOptionsTakeOptionsLocked2 = activityOptions;
                                r5 = z2;
                                r172 = 0;
                            }
                            i4++;
                            r5 = r5;
                            r172 = r172;
                        }
                        activityOptionsTakeOptionsLocked = activityOptionsTakeOptionsLocked2;
                        r17 = r172;
                    }
                } else {
                    ActivityRecord activityRecord4 = (this.mTaskHistory.isEmpty() || this.mTaskHistory.get(0).mActivities.isEmpty()) ? null : this.mTaskHistory.get(0).mActivities.get(0);
                    if (activityRecord4 == null || activityRecord2.taskAffinity == null || !activityRecord2.taskAffinity.equals(activityRecord4.getTask().affinity)) {
                        boolean z6 = false;
                        activityRecord = activityRecord2;
                        taskRecordCreateTaskRecord = createTaskRecord(this.mStackSupervisor.getNextTaskIdForUserLocked(activityRecord2.userId), activityRecord2.info, null, null, null, false);
                        taskRecordCreateTaskRecord.affinityIntent = activityRecord.intent;
                        r12 = z6;
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.v(TAG_TASKS, "Start pushing activity " + activityRecord + " out to new task " + taskRecordCreateTaskRecord);
                            r12 = z6;
                        }
                    } else {
                        taskRecordCreateTaskRecord = activityRecord4.getTask();
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.v(TAG_TASKS, "Start pushing activity " + activityRecord2 + " out to bottom task " + taskRecordCreateTaskRecord);
                        }
                        r12 = 0;
                        activityRecord = activityRecord2;
                    }
                    if (i2 < 0) {
                        i2 = i;
                    }
                    int i5 = i2;
                    ?? r52 = r17;
                    ?? r173 = r17;
                    while (i5 >= i) {
                        ActivityRecord activityRecord5 = arrayList.get(i5);
                        r52 = r52;
                        r52 = r52;
                        if (!activityRecord5.finishing) {
                            if (r52 != 0 && activityOptionsTakeOptionsLocked == null) {
                                activityOptionsTakeOptionsLocked = activityRecord5.takeOptionsLocked();
                                r52 = r52;
                                if (activityOptionsTakeOptionsLocked != null) {
                                    r52 = r12;
                                }
                            }
                            if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                                Slog.i(TAG_ADD_REMOVE, "Removing activity " + activityRecord5 + " from task=" + taskRecord + " adding to task=" + taskRecordCreateTaskRecord + " Callers=" + Debug.getCallers(4));
                            }
                            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                                Slog.v(TAG_TASKS, "Pushing next activity " + activityRecord5 + " out to target's task " + activityRecord);
                            }
                            activityRecord5.reparent(taskRecordCreateTaskRecord, r12, "resetTargetTaskIfNeeded");
                            r173 = r12;
                        }
                        i5--;
                        r52 = r52;
                        r173 = r173;
                    }
                    this.mWindowContainerController.positionChildAtBottom(taskRecordCreateTaskRecord.getWindowContainerController(), r12);
                    r17 = r173;
                }
                i2 = -1;
            } else if (i2 < 0) {
                i2 = i;
            }
            i--;
            r17 = r17;
        }
        return activityOptionsTakeOptionsLocked;
    }

    private int resetAffinityTaskIfNeededLocked(TaskRecord taskRecord, TaskRecord taskRecord2, boolean z, boolean z2, int i) {
        ArrayList<ActivityRecord> arrayList;
        int iIndexOf;
        int i2 = taskRecord2.taskId;
        String str = taskRecord2.affinity;
        ArrayList<ActivityRecord> arrayList2 = taskRecord.mActivities;
        int size = arrayList2.size();
        int iFindEffectiveRootIndex = taskRecord.findEffectiveRootIndex();
        int size2 = i;
        int i3 = -1;
        for (int i4 = size - 1; i4 > iFindEffectiveRootIndex; i4--) {
            ActivityRecord activityRecord = arrayList2.get(i4);
            if (activityRecord.frontOfTask) {
                break;
            }
            int i5 = activityRecord.info.flags;
            boolean z3 = (i5 & 2) != 0;
            boolean z4 = (i5 & 64) != 0;
            if (activityRecord.resultTo != null) {
                if (i3 < 0) {
                    i3 = i4;
                }
            } else if (z && z4 && str != null && str.equals(activityRecord.taskAffinity)) {
                if (z2 || z3) {
                    if (i3 < 0) {
                        i3 = i4;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Finishing task at index " + i3 + " to " + i4);
                    }
                    while (i3 >= i4) {
                        ActivityRecord activityRecord2 = arrayList2.get(i3);
                        if (!activityRecord2.finishing) {
                            finishActivityLocked(activityRecord2, 0, null, "move-affinity", false);
                        }
                        i3--;
                    }
                } else {
                    if (size2 < 0) {
                        size2 = taskRecord2.mActivities.size();
                    }
                    if (i3 < 0) {
                        i3 = i4;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                        Slog.v(TAG_TASKS, "Reparenting from task=" + taskRecord + ":" + i3 + "-" + i4 + " to task=" + taskRecord2 + ":" + size2);
                    }
                    while (i3 >= i4) {
                        ActivityRecord activityRecord3 = arrayList2.get(i3);
                        activityRecord3.reparent(taskRecord2, size2, "resetAffinityTaskIfNeededLocked");
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
                            Slog.i(TAG_ADD_REMOVE, "Removing and adding activity " + activityRecord3 + " to stack at " + taskRecord2 + " callers=" + Debug.getCallers(3));
                        }
                        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                            Slog.v(TAG_TASKS, "Pulling activity " + activityRecord3 + " from " + i3 + " in to resetting task " + taskRecord2);
                        }
                        i3--;
                    }
                    this.mWindowContainerController.positionChildAtTop(taskRecord2.getWindowContainerController(), true);
                    if (activityRecord.info.launchMode == 1 && (iIndexOf = (arrayList = taskRecord2.mActivities).indexOf(activityRecord)) > 0) {
                        ActivityRecord activityRecord4 = arrayList.get(iIndexOf - 1);
                        if (activityRecord4.intent.getComponent().equals(activityRecord.intent.getComponent())) {
                            finishActivityLocked(activityRecord4, 0, null, "replace", false);
                        }
                    }
                }
                i3 = -1;
            }
        }
        return size2;
    }

    final ActivityRecord resetTaskIfNeededLocked(ActivityRecord activityRecord, ActivityRecord activityRecord2) {
        boolean z = (activityRecord2.info.flags & 4) != 0;
        TaskRecord task = activityRecord.getTask();
        boolean z2 = false;
        ActivityOptions activityOptionsResetTargetTaskIfNeededLocked = null;
        int iResetAffinityTaskIfNeededLocked = -1;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (taskRecord == task) {
                z2 = true;
                activityOptionsResetTargetTaskIfNeededLocked = resetTargetTaskIfNeededLocked(task, z);
            } else {
                iResetAffinityTaskIfNeededLocked = resetAffinityTaskIfNeededLocked(taskRecord, task, z2, z, iResetAffinityTaskIfNeededLocked);
            }
        }
        int iIndexOf = this.mTaskHistory.indexOf(task);
        if (iIndexOf >= 0) {
            while (true) {
                int i = iIndexOf - 1;
                activityRecord = this.mTaskHistory.get(iIndexOf).getTopActivity();
                if (activityRecord != null || i < 0) {
                    break;
                }
                iIndexOf = i;
            }
        }
        if (activityOptionsResetTargetTaskIfNeededLocked != null) {
            if (activityRecord != null) {
                activityRecord.updateOptionsLocked(activityOptionsResetTargetTaskIfNeededLocked);
            } else {
                activityOptionsResetTargetTaskIfNeededLocked.abort();
            }
        }
        return activityRecord;
    }

    void sendActivityResultLocked(int i, ActivityRecord activityRecord, String str, int i2, int i3, Intent intent) {
        if (i > 0) {
            this.mService.grantUriPermissionFromIntentLocked(i, activityRecord.packageName, intent, activityRecord.getUriPermissionsLocked(), activityRecord.userId);
        }
        if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG, "Send activity result to " + activityRecord + " : who=" + str + " req=" + i2 + " res=" + i3 + " data=" + intent);
        }
        if (this.mResumedActivity == activityRecord && activityRecord.app != null && activityRecord.app.thread != null) {
            try {
                ArrayList arrayList = new ArrayList();
                arrayList.add(new ResultInfo(str, i2, i3, intent));
                this.mService.getLifecycleManager().scheduleTransaction(activityRecord.app.thread, (IBinder) activityRecord.appToken, (ClientTransactionItem) ActivityResultItem.obtain(arrayList));
                return;
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown sending result to " + activityRecord, e);
            }
        }
        activityRecord.addResultLocked(null, str, i2, i3, intent);
    }

    private boolean isATopFinishingTask(TaskRecord taskRecord) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord2 = this.mTaskHistory.get(size);
            if (taskRecord2.topRunningActivityLocked() != null) {
                return false;
            }
            if (taskRecord2 == taskRecord) {
                return true;
            }
        }
        return false;
    }

    private void adjustFocusedActivityStack(ActivityRecord activityRecord, String str) {
        if (this.mStackSupervisor.isFocusedStack(this)) {
            if (this.mResumedActivity != activityRecord && this.mResumedActivity != null) {
                return;
            }
            ActivityRecord activityRecord2 = topRunningActivityLocked();
            String str2 = str + " adjustFocus";
            if (activityRecord2 == activityRecord) {
                this.mStackSupervisor.moveFocusableActivityStackToFrontLocked(this.mStackSupervisor.topRunningActivityLocked(), str2);
                return;
            }
            if (activityRecord2 != null && isFocusable()) {
                return;
            }
            if (activityRecord.getTask() == null) {
                throw new IllegalStateException("activity no longer associated with task:" + activityRecord);
            }
            if (adjustFocusToNextFocusableStack(str2)) {
                return;
            }
            this.mStackSupervisor.moveHomeStackTaskToTop(str2);
        }
    }

    boolean adjustFocusToNextFocusableStack(String str) {
        return adjustFocusToNextFocusableStack(str, false);
    }

    private boolean adjustFocusToNextFocusableStack(String str, boolean z) {
        ActivityStack nextFocusableStackLocked = this.mStackSupervisor.getNextFocusableStackLocked(this, !z);
        String str2 = str + " adjustFocusToNextFocusableStack";
        if (nextFocusableStackLocked == null) {
            return false;
        }
        ActivityRecord activityRecord = nextFocusableStackLocked.topRunningActivityLocked();
        if (nextFocusableStackLocked.isActivityTypeHome() && (activityRecord == null || !activityRecord.visible)) {
            return this.mStackSupervisor.moveHomeStackTaskToTop(str);
        }
        nextFocusableStackLocked.moveToFront(str2);
        return true;
    }

    final void stopActivityLocked(ActivityRecord activityRecord) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Stopping: " + activityRecord);
        }
        if (((activityRecord.intent.getFlags() & 1073741824) != 0 || (activityRecord.info.flags & 128) != 0) && !activityRecord.finishing) {
            if (!shouldSleepActivities()) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.d(TAG_STATES, "no-history finish of " + activityRecord);
                }
                if (requestFinishActivityLocked(activityRecord.appToken, 0, null, "stop-no-history", false)) {
                    activityRecord.resumeKeyDispatchingLocked();
                    return;
                }
            } else if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.d(TAG_STATES, "Not finishing noHistory " + activityRecord + " on stop because we're just sleeping");
            }
        }
        if (activityRecord.app != null && activityRecord.app.thread != null) {
            adjustFocusedActivityStack(activityRecord, "stopActivity");
            activityRecord.resumeKeyDispatchingLocked();
            try {
                activityRecord.stopped = false;
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to STOPPING: " + activityRecord + " (stop requested)");
                }
                activityRecord.setState(ActivityState.STOPPING, "stopActivityLocked");
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY) {
                    Slog.v(TAG_VISIBILITY, "Stopping visible=" + activityRecord.visible + " for " + activityRecord);
                }
                if (!activityRecord.visible) {
                    activityRecord.setVisible(false);
                }
                EventLogTags.writeAmStopActivity(activityRecord.userId, System.identityHashCode(activityRecord), activityRecord.shortComponentName);
                this.mService.getLifecycleManager().scheduleTransaction(activityRecord.app.thread, (IBinder) activityRecord.appToken, (ActivityLifecycleItem) StopActivityItem.obtain(activityRecord.visible, activityRecord.configChangeFlags));
                if (shouldSleepOrShutDownActivities()) {
                    activityRecord.setSleeping(true);
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(104, activityRecord), 11000L);
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown during pause", e);
                activityRecord.stopped = true;
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Stop failed; moving to STOPPED: " + activityRecord);
                }
                activityRecord.setState(ActivityState.STOPPED, "stopActivityLocked");
                if (activityRecord.deferRelaunchUntilPaused) {
                    destroyActivityLocked(activityRecord, true, "stop-except");
                }
            }
        }
    }

    final boolean requestFinishActivityLocked(IBinder iBinder, int i, Intent intent, String str, boolean z) {
        ActivityRecord activityRecordIsInStackLocked = isInStackLocked(iBinder);
        if (ActivityManagerDebugConfig.DEBUG_RESULTS || ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Finishing activity token=" + iBinder + " r=, result=" + i + ", data=" + intent + ", reason=" + str);
        }
        if (activityRecordIsInStackLocked == null) {
            return false;
        }
        finishActivityLocked(activityRecordIsInStackLocked, i, intent, str, z);
        return true;
    }

    final void finishSubActivityLocked(ActivityRecord activityRecord, String str, int i) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord2 = arrayList.get(size2);
                if (activityRecord2.resultTo == activityRecord && activityRecord2.requestCode == i && ((activityRecord2.resultWho == null && str == null) || (activityRecord2.resultWho != null && activityRecord2.resultWho.equals(str)))) {
                    finishActivityLocked(activityRecord2, 0, null, "request-sub", false);
                }
            }
        }
        this.mService.updateOomAdjLocked();
    }

    final TaskRecord finishTopCrashedActivityLocked(ProcessRecord processRecord, String str) {
        ActivityRecord activityRecord = topRunningActivityLocked();
        if (activityRecord == null || activityRecord.app != processRecord) {
            return null;
        }
        Slog.w(TAG, "  Force finishing activity " + activityRecord.intent.getComponent().flattenToShortString());
        TaskRecord task = activityRecord.getTask();
        int iIndexOf = this.mTaskHistory.indexOf(task);
        int iIndexOf2 = task.mActivities.indexOf(activityRecord);
        this.mWindowManager.prepareAppTransition(26, false);
        finishActivityLocked(activityRecord, 0, null, str, false);
        int size = iIndexOf2 - 1;
        if (size < 0) {
            do {
                iIndexOf--;
                if (iIndexOf < 0) {
                    break;
                }
                size = this.mTaskHistory.get(iIndexOf).mActivities.size() - 1;
            } while (size < 0);
        }
        if (size >= 0) {
            ActivityRecord activityRecord2 = this.mTaskHistory.get(iIndexOf).mActivities.get(size);
            if (activityRecord2.isState(ActivityState.RESUMED, ActivityState.PAUSING, ActivityState.PAUSED) && (!activityRecord2.isActivityTypeHome() || this.mService.mHomeProcess != activityRecord2.app)) {
                Slog.w(TAG, "  Force finishing activity " + activityRecord2.intent.getComponent().flattenToShortString());
                finishActivityLocked(activityRecord2, 0, null, str, false);
            }
        }
        return task;
    }

    final void finishVoiceTask(IVoiceInteractionSession iVoiceInteractionSession) {
        IBinder iBinderAsBinder = iVoiceInteractionSession.asBinder();
        boolean z = false;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (taskRecord.voiceSession != null && taskRecord.voiceSession.asBinder() == iBinderAsBinder) {
                for (int size2 = taskRecord.mActivities.size() - 1; size2 >= 0; size2--) {
                    ActivityRecord activityRecord = taskRecord.mActivities.get(size2);
                    if (!activityRecord.finishing) {
                        finishActivityLocked(activityRecord, 0, null, "finish-voice", false);
                        z = true;
                    }
                }
            } else {
                int size3 = taskRecord.mActivities.size() - 1;
                while (true) {
                    if (size3 >= 0) {
                        ActivityRecord activityRecord2 = taskRecord.mActivities.get(size3);
                        if (activityRecord2.voiceSession != null && activityRecord2.voiceSession.asBinder() == iBinderAsBinder) {
                            activityRecord2.clearVoiceSessionLocked();
                            try {
                                activityRecord2.app.thread.scheduleLocalVoiceInteractionStarted(activityRecord2.appToken, (IVoiceInteractor) null);
                            } catch (RemoteException e) {
                            }
                            this.mService.finishRunningVoiceLocked();
                            break;
                        }
                        size3--;
                    }
                }
            }
        }
        if (z) {
            this.mService.updateOomAdjLocked();
        }
    }

    final boolean finishActivityAffinityLocked(ActivityRecord activityRecord) {
        ArrayList<ActivityRecord> arrayList = activityRecord.getTask().mActivities;
        for (int iIndexOf = arrayList.indexOf(activityRecord); iIndexOf >= 0; iIndexOf--) {
            ActivityRecord activityRecord2 = arrayList.get(iIndexOf);
            if (Objects.equals(activityRecord2.taskAffinity, activityRecord.taskAffinity)) {
                finishActivityLocked(activityRecord2, 0, null, "request-affinity", true);
            } else {
                return true;
            }
        }
        return true;
    }

    private void finishActivityResultsLocked(ActivityRecord activityRecord, int i, Intent intent) {
        ActivityRecord activityRecord2 = activityRecord.resultTo;
        if (activityRecord2 != null) {
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                Slog.v(TAG_RESULTS, "Adding result to " + activityRecord2 + " who=" + activityRecord.resultWho + " req=" + activityRecord.requestCode + " res=" + i + " data=" + intent);
            }
            if (activityRecord2.userId != activityRecord.userId && intent != null) {
                intent.prepareToLeaveUser(activityRecord.userId);
            }
            if (activityRecord.info.applicationInfo.uid > 0) {
                this.mService.grantUriPermissionFromIntentLocked(activityRecord.info.applicationInfo.uid, activityRecord2.packageName, intent, activityRecord2.getUriPermissionsLocked(), activityRecord2.userId);
            }
            activityRecord2.addResultLocked(activityRecord, activityRecord.resultWho, activityRecord.requestCode, i, intent);
            activityRecord.resultTo = null;
        } else if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG_RESULTS, "No result destination from " + activityRecord);
        }
        activityRecord.results = null;
        activityRecord.pendingResults = null;
        activityRecord.newIntents = null;
        activityRecord.icicle = null;
    }

    final boolean finishActivityLocked(ActivityRecord activityRecord, int i, Intent intent, String str, boolean z) {
        return finishActivityLocked(activityRecord, i, intent, str, z, false);
    }

    final boolean finishActivityLocked(ActivityRecord activityRecord, int i, Intent intent, String str, boolean z, boolean z2) {
        if (activityRecord.finishing) {
            Slog.w(TAG, "Duplicate finish request for " + activityRecord);
            return false;
        }
        this.mWindowManager.deferSurfaceLayout();
        try {
            activityRecord.makeFinishingLocked();
            TaskRecord task = activityRecord.getTask();
            int i2 = 2;
            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(activityRecord.userId), Integer.valueOf(System.identityHashCode(activityRecord)), Integer.valueOf(task.taskId), activityRecord.shortComponentName, str);
            ArrayList<ActivityRecord> arrayList = task.mActivities;
            int iIndexOf = arrayList.indexOf(activityRecord);
            if (iIndexOf < arrayList.size() - 1) {
                task.setFrontOfTask();
                if ((activityRecord.intent.getFlags() & DumpState.DUMP_FROZEN) != 0) {
                    arrayList.get(iIndexOf + 1).intent.addFlags(DumpState.DUMP_FROZEN);
                }
            }
            activityRecord.pauseKeyDispatchingLocked();
            adjustFocusedActivityStack(activityRecord, "finishActivity");
            finishActivityResultsLocked(activityRecord, i, intent);
            boolean z3 = iIndexOf <= 0 && !task.isClearingToReuseTask();
            int i3 = z3 ? 9 : 7;
            if (this.mResumedActivity == activityRecord) {
                if (ActivityManagerDebugConfig.DEBUG_VISIBILITY || ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                    Slog.v(TAG_TRANSITION, "Prepare close transition: finishing " + activityRecord);
                }
                if (z3) {
                    this.mService.mTaskChangeNotificationController.notifyTaskRemovalStarted(task.taskId);
                }
                this.mWindowManager.prepareAppTransition(i3, false);
                activityRecord.setVisibility(false);
                if (this.mPausingActivity == null) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Finish needs to pause: " + activityRecord);
                    }
                    if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
                        Slog.v(TAG_USER_LEAVING, "finish() => pause with userLeaving=false");
                    }
                    startPausingLocked(false, false, null, z2);
                    ActivityRecord activityRecord2 = this.mStackSupervisor.getFocusedStack().topRunningActivityLocked();
                    if (activityRecord2 != null) {
                        this.mService.mAmsExt.onBeforeActivitySwitch(this.mService.mLastResumedActivity, activityRecord2, true, activityRecord2.getActivityType());
                    }
                }
                if (z3) {
                    this.mService.getLockTaskController().clearLockedTask(task);
                }
            } else {
                if (!activityRecord.isState(ActivityState.PAUSING)) {
                    if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                        Slog.v(TAG_PAUSE, "Finish not pausing: " + activityRecord);
                    }
                    if (activityRecord.visible) {
                        prepareActivityHideTransitionAnimation(activityRecord, i3);
                    }
                    if (!activityRecord.visible && !activityRecord.nowVisible) {
                        i2 = 1;
                    }
                    boolean z4 = finishCurrentActivityLocked(activityRecord, i2, z, "finishActivityLocked") == null;
                    if (task.onlyHasTaskOverlayActivities(true)) {
                        for (ActivityRecord activityRecord3 : task.mActivities) {
                            if (activityRecord3.mTaskOverlay) {
                                prepareActivityHideTransitionAnimation(activityRecord3, i3);
                            }
                        }
                    }
                    return z4;
                }
                if (ActivityManagerDebugConfig.DEBUG_PAUSE) {
                    Slog.v(TAG_PAUSE, "Finish waiting for pause of: " + activityRecord);
                }
            }
            return false;
        } finally {
            this.mWindowManager.continueSurfaceLayout();
        }
    }

    private void prepareActivityHideTransitionAnimation(ActivityRecord activityRecord, int i) {
        this.mWindowManager.prepareAppTransition(i, false);
        activityRecord.setVisibility(false);
        this.mWindowManager.executeAppTransition();
        if (!this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(activityRecord)) {
            this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.add(activityRecord);
        }
    }

    final ActivityRecord finishCurrentActivityLocked(ActivityRecord activityRecord, int i, boolean z, String str) {
        ActivityRecord activityRecord2 = this.mStackSupervisor.topRunningActivityLocked(true);
        if (i == 2 && ((activityRecord.visible || activityRecord.nowVisible) && activityRecord2 != null && !activityRecord2.nowVisible)) {
            if (!this.mStackSupervisor.mStoppingActivities.contains(activityRecord)) {
                addToStopping(activityRecord, false, false);
            }
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to STOPPING: " + activityRecord + " (finish requested)");
            }
            activityRecord.setState(ActivityState.STOPPING, "finishCurrentActivityLocked");
            if (z) {
                this.mService.updateOomAdjLocked();
            }
            return activityRecord;
        }
        this.mStackSupervisor.mStoppingActivities.remove(activityRecord);
        this.mStackSupervisor.mGoingToSleepActivities.remove(activityRecord);
        this.mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(activityRecord);
        ActivityState state = activityRecord.getState();
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to FINISHING: " + activityRecord);
        }
        activityRecord.setState(ActivityState.FINISHING, "finishCurrentActivityLocked");
        boolean z2 = activityRecord.getStack() != this.mStackSupervisor.getFocusedStack() && state == ActivityState.PAUSED && i == 2;
        if (i == 0 || ((state == ActivityState.PAUSED && (i == 1 || inPinnedWindowingMode())) || z2 || state == ActivityState.STOPPING || state == ActivityState.STOPPED || state == ActivityState.INITIALIZING)) {
            activityRecord.makeFinishingLocked();
            boolean zDestroyActivityLocked = destroyActivityLocked(activityRecord, true, "finish-imm:" + str);
            if (z2 && this.mDisplayId != -1) {
                this.mStackSupervisor.ensureVisibilityAndConfig(activityRecord2, this.mDisplayId, false, true);
            }
            if (zDestroyActivityLocked) {
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            }
            if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
                Slog.d(TAG_CONTAINERS, "destroyActivityLocked: finishCurrentActivityLocked r=" + activityRecord + " destroy returned removed=" + zDestroyActivityLocked);
            }
            if (zDestroyActivityLocked) {
                return null;
            }
            return activityRecord;
        }
        if (ActivityManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Enqueueing pending finish: " + activityRecord);
        }
        this.mStackSupervisor.mFinishingActivities.add(activityRecord);
        activityRecord.resumeKeyDispatchingLocked();
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return activityRecord;
    }

    void finishAllActivitiesLocked(boolean z) {
        boolean z2 = true;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            int size2 = arrayList.size() - 1;
            while (size2 >= 0) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (!activityRecord.finishing || z) {
                    Slog.d(TAG, "finishAllActivitiesLocked: finishing " + activityRecord + " immediately");
                    finishCurrentActivityLocked(activityRecord, 0, false, "finishAllActivitiesLocked");
                }
                size2--;
                z2 = false;
            }
        }
        if (z2) {
            remove();
        }
    }

    boolean inFrontOfStandardStack() {
        int indexOf;
        ActivityDisplay display = getDisplay();
        if (display == null || (indexOf = display.getIndexOf(this)) == 0) {
            return false;
        }
        return display.getChildAt(indexOf - 1).isActivityTypeStandard();
    }

    boolean shouldUpRecreateTaskLocked(ActivityRecord activityRecord, String str) {
        if (activityRecord == null || activityRecord.getTask().affinity == null || !activityRecord.getTask().affinity.equals(str)) {
            return true;
        }
        TaskRecord task = activityRecord.getTask();
        if (activityRecord.frontOfTask && task.getBaseIntent() != null && task.getBaseIntent().isDocument()) {
            if (!inFrontOfStandardStack()) {
                return true;
            }
            int iIndexOf = this.mTaskHistory.indexOf(task);
            if (iIndexOf <= 0) {
                Slog.w(TAG, "shouldUpRecreateTask: task not in history for " + activityRecord);
                return false;
            }
            if (!task.affinity.equals(this.mTaskHistory.get(iIndexOf).affinity)) {
                return true;
            }
        }
        return false;
    }

    final boolean navigateUpToLocked(ActivityRecord activityRecord, Intent intent, int i, Intent intent2) {
        ActivityRecord activityRecord2;
        int i2;
        ActivityRecord activityRecord3;
        boolean z;
        ActivityRecord activityRecord4;
        boolean zActivityResuming;
        if (activityRecord.app == null || activityRecord.app.thread == null) {
            return false;
        }
        TaskRecord task = activityRecord.getTask();
        ArrayList<ActivityRecord> arrayList = task.mActivities;
        int iIndexOf = arrayList.indexOf(activityRecord);
        if (!this.mTaskHistory.contains(task) || iIndexOf < 0) {
            return false;
        }
        int i3 = iIndexOf - 1;
        if (i3 >= 0) {
            activityRecord2 = arrayList.get(i3);
        } else {
            activityRecord2 = null;
        }
        ComponentName component = intent.getComponent();
        boolean z2 = true;
        if (iIndexOf > 0 && component != null) {
            for (int i4 = i3; i4 >= 0; i4--) {
                ActivityRecord activityRecord5 = arrayList.get(i4);
                if (activityRecord5.info.packageName.equals(component.getPackageName()) && activityRecord5.info.name.equals(component.getClassName())) {
                    i2 = i4;
                    activityRecord3 = activityRecord5;
                    z = true;
                    break;
                }
            }
            i2 = i3;
            activityRecord3 = activityRecord2;
            z = false;
        } else {
            i2 = i3;
            activityRecord3 = activityRecord2;
            z = false;
        }
        IActivityController iActivityController = this.mService.mController;
        if (iActivityController != null && (activityRecord4 = topRunningActivityLocked(activityRecord.appToken, 0)) != null) {
            try {
                zActivityResuming = iActivityController.activityResuming(activityRecord4.packageName);
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
                zActivityResuming = true;
            }
            if (!zActivityResuming) {
                return false;
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        int i5 = i;
        Intent intent3 = intent2;
        int i6 = iIndexOf;
        while (i6 > i2) {
            requestFinishActivityLocked(arrayList.get(i6).appToken, i5, intent3, "navigate-up", true);
            i6--;
            jClearCallingIdentity = jClearCallingIdentity;
            intent3 = null;
            arrayList = arrayList;
            i5 = 0;
        }
        long j = jClearCallingIdentity;
        if (activityRecord3 != null && z) {
            int i7 = activityRecord.info.applicationInfo.uid;
            int i8 = activityRecord3.info.launchMode;
            int flags = intent.getFlags();
            if (i8 == 3 || i8 == 2 || i8 == 1 || (67108864 & flags) != 0) {
                activityRecord3.deliverNewIntentLocked(i7, intent, activityRecord.packageName);
            } else {
                try {
                    if (this.mService.getActivityStartController().obtainStarter(intent, "navigateUpTo").setCaller(activityRecord.app.thread).setActivityInfo(AppGlobals.getPackageManager().getActivityInfo(intent.getComponent(), 1024, activityRecord.userId)).setResultTo(activityRecord3.appToken).setCallingPid(-1).setCallingUid(i7).setCallingPackage(activityRecord.packageName).setRealCallingPid(-1).setRealCallingUid(i7).setComponentSpecified(true).execute() != 0) {
                        z2 = false;
                    }
                    z = z2;
                } catch (RemoteException e2) {
                    z = false;
                }
                requestFinishActivityLocked(activityRecord3.appToken, i5, intent3, "navigate-top", true);
            }
        }
        Binder.restoreCallingIdentity(j);
        return z;
    }

    void onActivityRemovedFromStack(ActivityRecord activityRecord) {
        removeTimeoutsForActivityLocked(activityRecord);
        if (this.mResumedActivity != null && this.mResumedActivity == activityRecord) {
            setResumedActivity(null, "onActivityRemovedFromStack");
        }
        if (this.mPausingActivity != null && this.mPausingActivity == activityRecord) {
            this.mPausingActivity = null;
        }
    }

    void onActivityAddedToStack(ActivityRecord activityRecord) {
        if (activityRecord.getState() == ActivityState.RESUMED) {
            setResumedActivity(activityRecord, "onActivityAddedToStack");
        }
    }

    private void cleanUpActivityLocked(ActivityRecord activityRecord, boolean z, boolean z2) {
        onActivityRemovedFromStack(activityRecord);
        activityRecord.deferRelaunchUntilPaused = false;
        activityRecord.frozenBeforeDestroy = false;
        if (z2) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to DESTROYED: " + activityRecord + " (cleaning up)");
            }
            activityRecord.setState(ActivityState.DESTROYED, "cleanupActivityLocked");
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Clearing app during cleanUp for activity " + activityRecord);
            }
            activityRecord.app = null;
        }
        this.mStackSupervisor.cleanupActivity(activityRecord);
        if (activityRecord.finishing && activityRecord.pendingResults != null) {
            Iterator<WeakReference<PendingIntentRecord>> it = activityRecord.pendingResults.iterator();
            while (it.hasNext()) {
                PendingIntentRecord pendingIntentRecord = it.next().get();
                if (pendingIntentRecord != null) {
                    this.mService.cancelIntentSenderLocked(pendingIntentRecord, false);
                }
            }
            activityRecord.pendingResults = null;
        }
        if (z) {
            cleanUpActivityServicesLocked(activityRecord);
        }
        removeTimeoutsForActivityLocked(activityRecord);
        this.mWindowManager.notifyAppRelaunchesCleared(activityRecord.appToken);
    }

    void removeTimeoutsForActivityLocked(ActivityRecord activityRecord) {
        this.mStackSupervisor.removeTimeoutsForActivityLocked(activityRecord);
        this.mHandler.removeMessages(101, activityRecord);
        this.mHandler.removeMessages(104, activityRecord);
        this.mHandler.removeMessages(102, activityRecord);
        activityRecord.finishLaunchTickingLocked();
    }

    private void removeActivityFromHistoryLocked(ActivityRecord activityRecord, String str) {
        boolean zOnlyHasTaskOverlayActivities;
        finishActivityResultsLocked(activityRecord, 0, null);
        activityRecord.makeFinishingLocked();
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i(TAG_ADD_REMOVE, "Removing activity " + activityRecord + " from stack callers=" + Debug.getCallers(5));
        }
        activityRecord.takeFromHistory();
        removeTimeoutsForActivityLocked(activityRecord);
        if (ActivityManagerDebugConfig.DEBUG_STATES) {
            Slog.v(TAG_STATES, "Moving to DESTROYED: " + activityRecord + " (removed from history)");
        }
        activityRecord.setState(ActivityState.DESTROYED, "removeActivityFromHistoryLocked");
        if (ActivityManagerDebugConfig.DEBUG_APP) {
            Slog.v(TAG_APP, "Clearing app during remove for activity " + activityRecord);
        }
        activityRecord.app = null;
        activityRecord.removeWindowContainer();
        TaskRecord task = activityRecord.getTask();
        boolean zRemoveActivity = task != null ? task.removeActivity(activityRecord) : false;
        if (task != null) {
            zOnlyHasTaskOverlayActivities = task.onlyHasTaskOverlayActivities(false);
        } else {
            zOnlyHasTaskOverlayActivities = false;
        }
        if (zRemoveActivity || zOnlyHasTaskOverlayActivities) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG_STACK, "removeActivityFromHistoryLocked: last activity removed from " + this + " onlyHasTaskOverlays=" + zOnlyHasTaskOverlayActivities);
            }
            if (zOnlyHasTaskOverlayActivities) {
                this.mStackSupervisor.removeTaskByIdLocked(task.taskId, false, false, true, str);
            }
            if (zRemoveActivity) {
                removeTask(task, str, 0);
            }
        }
        cleanUpActivityServicesLocked(activityRecord);
        activityRecord.removeUriPermissionsLocked();
    }

    private void cleanUpActivityServicesLocked(ActivityRecord activityRecord) {
        if (activityRecord.connections != null) {
            Iterator<ConnectionRecord> it = activityRecord.connections.iterator();
            while (it.hasNext()) {
                this.mService.mServices.removeConnectionLocked(it.next(), null, activityRecord);
            }
            activityRecord.connections = null;
        }
    }

    final void scheduleDestroyActivities(ProcessRecord processRecord, String str) {
        Message messageObtainMessage = this.mHandler.obtainMessage(105);
        messageObtainMessage.obj = new ScheduleDestroyArgs(processRecord, str);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void destroyActivitiesLocked(ProcessRecord processRecord, String str) {
        boolean z = false;
        boolean z2 = false;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (!activityRecord.finishing) {
                    if (activityRecord.fullscreen) {
                        z2 = true;
                    }
                    if ((processRecord == null || activityRecord.app == processRecord) && z2 && activityRecord.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                            Slog.v(TAG_SWITCH, "Destroying " + activityRecord + " in state " + activityRecord.getState() + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + str);
                        }
                        if (destroyActivityLocked(activityRecord, true, str)) {
                            z = true;
                        }
                    }
                }
            }
        }
        if (z) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    final boolean safelyDestroyActivityLocked(ActivityRecord activityRecord, String str) {
        if (activityRecord.isDestroyable()) {
            if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Destroying " + activityRecord + " in state " + activityRecord.getState() + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + str);
            }
            return destroyActivityLocked(activityRecord, true, str);
        }
        return false;
    }

    final int releaseSomeActivitiesLocked(ProcessRecord processRecord, ArraySet<TaskRecord> arraySet, String str) {
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + processRecord);
        }
        int size = arraySet.size() / 4;
        if (size < 1) {
            size = 1;
        }
        int i = size;
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mTaskHistory.size() && i > 0) {
            TaskRecord taskRecord = this.mTaskHistory.get(i2);
            if (arraySet.contains(taskRecord)) {
                if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Looking for activities to release in " + taskRecord);
                }
                ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
                int i4 = 0;
                int i5 = 0;
                while (i4 < arrayList.size()) {
                    ActivityRecord activityRecord = arrayList.get(i4);
                    if (activityRecord.app == processRecord && activityRecord.isDestroyable()) {
                        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
                            Slog.v(TAG_RELEASE, "Destroying " + activityRecord + " in state " + activityRecord.getState() + " resumed=" + this.mResumedActivity + " pausing=" + this.mPausingActivity + " for reason " + str);
                        }
                        destroyActivityLocked(activityRecord, true, str);
                        if (arrayList.get(i4) != activityRecord) {
                            i4--;
                        }
                        i5++;
                    }
                    i4++;
                }
                if (i5 > 0) {
                    i3 += i5;
                    i--;
                    if (this.mTaskHistory.get(i2) != taskRecord) {
                        i2--;
                    }
                }
            }
            i2++;
        }
        if (ActivityManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Done releasing: did " + i3 + " activities");
        }
        return i3;
    }

    final boolean destroyActivityLocked(ActivityRecord activityRecord, boolean z, String str) {
        boolean z2;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            String str2 = TAG_SWITCH;
            StringBuilder sb = new StringBuilder();
            sb.append("Removing activity from ");
            sb.append(str);
            sb.append(": token=");
            sb.append(activityRecord);
            sb.append(", app=");
            sb.append(activityRecord.app != null ? activityRecord.app.processName : "(null)");
            Slog.v(str2, sb.toString());
        }
        if (activityRecord.isState(ActivityState.DESTROYING, ActivityState.DESTROYED)) {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "activity " + activityRecord + " already destroying.skipping request with reason:" + str);
            }
            return false;
        }
        boolean z3 = true;
        EventLog.writeEvent(EventLogTags.AM_DESTROY_ACTIVITY, Integer.valueOf(activityRecord.userId), Integer.valueOf(System.identityHashCode(activityRecord)), Integer.valueOf(activityRecord.getTask().taskId), activityRecord.shortComponentName, str);
        cleanUpActivityLocked(activityRecord, false, false);
        boolean z4 = activityRecord.app != null;
        if (z4) {
            if (z) {
                activityRecord.app.activities.remove(activityRecord);
                if (this.mService.mHeavyWeightProcess == activityRecord.app && activityRecord.app.activities.size() <= 0) {
                    this.mService.mHeavyWeightProcess = null;
                    this.mService.mHandler.sendEmptyMessage(25);
                }
                if (activityRecord.app.activities.isEmpty()) {
                    this.mService.mServices.updateServiceConnectionActivitiesLocked(activityRecord.app);
                    this.mService.updateLruProcessLocked(activityRecord.app, false, null);
                    this.mService.updateOomAdjLocked();
                }
            }
            try {
                if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.i(TAG_SWITCH, "Destroying: " + activityRecord);
                }
                this.mService.getLifecycleManager().scheduleTransaction(activityRecord.app.thread, (IBinder) activityRecord.appToken, (ActivityLifecycleItem) DestroyActivityItem.obtain(activityRecord.finishing, activityRecord.configChangeFlags));
            } catch (Exception e) {
                if (activityRecord.finishing) {
                    removeActivityFromHistoryLocked(activityRecord, str + " exceptionInScheduleDestroy");
                    z2 = true;
                }
                activityRecord.nowVisible = false;
                if (!activityRecord.finishing) {
                    if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    }
                    activityRecord.setState(ActivityState.DESTROYED, "destroyActivityLocked. not finishing or skipping destroy");
                    if (ActivityManagerDebugConfig.DEBUG_APP) {
                    }
                    activityRecord.app = null;
                }
                activityRecord.configChangeFlags = 0;
                if (!this.mLRUActivities.remove(activityRecord)) {
                    Slog.w(TAG, "Activity " + activityRecord + " being finished, but not in LRU list");
                }
                return z2;
            }
            z2 = false;
            z3 = false;
            activityRecord.nowVisible = false;
            if (!activityRecord.finishing && !z3) {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to DESTROYING: " + activityRecord + " (destroy requested)");
                }
                activityRecord.setState(ActivityState.DESTROYING, "destroyActivityLocked. finishing and not skipping destroy");
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(102, activityRecord), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            } else {
                if (ActivityManagerDebugConfig.DEBUG_STATES) {
                    Slog.v(TAG_STATES, "Moving to DESTROYED: " + activityRecord + " (destroy skipped)");
                }
                activityRecord.setState(ActivityState.DESTROYED, "destroyActivityLocked. not finishing or skipping destroy");
                if (ActivityManagerDebugConfig.DEBUG_APP) {
                    Slog.v(TAG_APP, "Clearing app during destroy for activity " + activityRecord);
                }
                activityRecord.app = null;
            }
        } else if (activityRecord.finishing) {
            removeActivityFromHistoryLocked(activityRecord, str + " hadNoApp");
            z2 = true;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_STATES) {
                Slog.v(TAG_STATES, "Moving to DESTROYED: " + activityRecord + " (no app)");
            }
            activityRecord.setState(ActivityState.DESTROYED, "destroyActivityLocked. not finishing and had no app");
            if (ActivityManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Clearing app during destroy for activity " + activityRecord);
            }
            activityRecord.app = null;
            z2 = false;
        }
        activityRecord.configChangeFlags = 0;
        if (!this.mLRUActivities.remove(activityRecord) && z4) {
            Slog.w(TAG, "Activity " + activityRecord + " being finished, but not in LRU list");
        }
        return z2;
    }

    final void activityDestroyedLocked(IBinder iBinder, String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            activityDestroyedLocked(ActivityRecord.forTokenLocked(iBinder), str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    final void activityDestroyedLocked(ActivityRecord activityRecord, String str) {
        if (activityRecord != null) {
            this.mHandler.removeMessages(102, activityRecord);
        }
        if (ActivityManagerDebugConfig.DEBUG_CONTAINERS) {
            Slog.d(TAG_CONTAINERS, "activityDestroyedLocked: r=" + activityRecord);
        }
        if (isInStackLocked(activityRecord) != null && activityRecord.isState(ActivityState.DESTROYING, ActivityState.DESTROYED)) {
            cleanUpActivityLocked(activityRecord, true, false);
            removeActivityFromHistoryLocked(activityRecord, str);
        }
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
    }

    private void removeHistoryRecordsForAppLocked(ArrayList<ActivityRecord> arrayList, ProcessRecord processRecord, String str) {
        int size = arrayList.size();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_CLEANUP, "Removing app " + processRecord + " from list " + str + " with " + size + " entries");
        }
        while (size > 0) {
            size--;
            ActivityRecord activityRecord = arrayList.get(size);
            if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_CLEANUP, "Record #" + size + " " + activityRecord);
            }
            if (activityRecord.app == processRecord) {
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(TAG_CLEANUP, "---> REMOVING this entry!");
                }
                arrayList.remove(size);
                removeTimeoutsForActivityLocked(activityRecord);
            }
        }
    }

    private boolean removeHistoryRecordsForAppLocked(ProcessRecord processRecord) {
        removeHistoryRecordsForAppLocked(this.mLRUActivities, processRecord, "mLRUActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mStoppingActivities, processRecord, "mStoppingActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mGoingToSleepActivities, processRecord, "mGoingToSleepActivities");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mActivitiesWaitingForVisibleActivity, processRecord, "mActivitiesWaitingForVisibleActivity");
        removeHistoryRecordsForAppLocked(this.mStackSupervisor.mFinishingActivities, processRecord, "mFinishingActivities");
        int iNumActivities = numActivities();
        if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_CLEANUP, "Removing app " + processRecord + " from history with " + iNumActivities + " entries");
        }
        boolean z = false;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            this.mTmpActivities.clear();
            this.mTmpActivities.addAll(arrayList);
            while (!this.mTmpActivities.isEmpty()) {
                int size2 = this.mTmpActivities.size() - 1;
                ActivityRecord activityRecordRemove = this.mTmpActivities.remove(size2);
                if (ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(TAG_CLEANUP, "Record #" + size2 + " " + activityRecordRemove + ": app=" + activityRecordRemove.app);
                }
                if (activityRecordRemove.app == processRecord) {
                    if (activityRecordRemove.visible) {
                        z = true;
                    }
                    boolean z2 = !(activityRecordRemove.haveState || activityRecordRemove.stateNotNeeded) || activityRecordRemove.finishing || (!activityRecordRemove.visible && activityRecordRemove.launchCount > 2 && activityRecordRemove.lastLaunchTime > SystemClock.uptimeMillis() - 60000);
                    if (z2) {
                        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                            Slog.i(TAG_ADD_REMOVE, "Removing activity " + activityRecordRemove + " from stack at " + iNumActivities + ": haveState=" + activityRecordRemove.haveState + " stateNotNeeded=" + activityRecordRemove.stateNotNeeded + " finishing=" + activityRecordRemove.finishing + " state=" + activityRecordRemove.getState() + " callers=" + Debug.getCallers(5));
                        }
                        if (!activityRecordRemove.finishing) {
                            Slog.w(TAG, "Force removing " + activityRecordRemove + ": app died, no saved state");
                            EventLog.writeEvent(EventLogTags.AM_FINISH_ACTIVITY, Integer.valueOf(activityRecordRemove.userId), Integer.valueOf(System.identityHashCode(activityRecordRemove)), Integer.valueOf(activityRecordRemove.getTask().taskId), activityRecordRemove.shortComponentName, "proc died without state saved");
                            if (activityRecordRemove.getState() == ActivityState.RESUMED) {
                                this.mService.updateUsageStats(activityRecordRemove, false);
                            }
                        }
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_ALL) {
                            Slog.v(TAG, "Keeping entry, setting app to null");
                        }
                        if (ActivityManagerDebugConfig.DEBUG_APP) {
                            Slog.v(TAG_APP, "Clearing app during removeHistory for activity " + activityRecordRemove);
                        }
                        activityRecordRemove.app = null;
                        activityRecordRemove.nowVisible = activityRecordRemove.visible;
                        if (!activityRecordRemove.haveState) {
                            if (ActivityManagerDebugConfig.DEBUG_SAVED_STATE) {
                                Slog.i(TAG_SAVED_STATE, "App died, clearing saved state of " + activityRecordRemove);
                            }
                            activityRecordRemove.icicle = null;
                        }
                    }
                    cleanUpActivityLocked(activityRecordRemove, true, true);
                    if (z2) {
                        removeActivityFromHistoryLocked(activityRecordRemove, "appDied");
                    }
                }
            }
        }
        return z;
    }

    private void updateTransitLocked(int i, ActivityOptions activityOptions) {
        if (activityOptions != null) {
            ActivityRecord activityRecord = topRunningActivityLocked();
            if (activityRecord != null && !activityRecord.isState(ActivityState.RESUMED)) {
                activityRecord.updateOptionsLocked(activityOptions);
            } else {
                ActivityOptions.abort(activityOptions);
            }
        }
        this.mWindowManager.prepareAppTransition(i, false);
    }

    private void updateTaskMovement(TaskRecord taskRecord, boolean z) {
        if (taskRecord.isPersistable) {
            taskRecord.mLastTimeMoved = System.currentTimeMillis();
            if (!z) {
                taskRecord.mLastTimeMoved *= -1;
            }
        }
        this.mStackSupervisor.invalidateTaskLayers();
    }

    void moveHomeStackTaskToTop() {
        if (!isActivityTypeHome()) {
            throw new IllegalStateException("Calling moveHomeStackTaskToTop() on non-home stack: " + this);
        }
        int size = this.mTaskHistory.size() - 1;
        if (size >= 0) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (ActivityManagerDebugConfig.DEBUG_TASKS || ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG_STACK, "moveHomeStackTaskToTop: moving " + taskRecord);
            }
            this.mTaskHistory.remove(size);
            this.mTaskHistory.add(size, taskRecord);
            updateTaskMovement(taskRecord, true);
        }
    }

    final void moveTaskToFrontLocked(TaskRecord taskRecord, boolean z, ActivityOptions activityOptions, AppTimeTracker appTimeTracker, String str) {
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "moveTaskToFront: " + taskRecord);
        }
        ActivityStack topStack = getDisplay().getTopStack();
        ActivityRecord topActivity = topStack != null ? topStack.getTopActivity() : null;
        int size = this.mTaskHistory.size();
        int iIndexOf = this.mTaskHistory.indexOf(taskRecord);
        if (size == 0 || iIndexOf < 0) {
            if (z) {
                ActivityOptions.abort(activityOptions);
                return;
            } else {
                updateTransitLocked(10, activityOptions);
                return;
            }
        }
        if (appTimeTracker != null) {
            for (int size2 = taskRecord.mActivities.size() - 1; size2 >= 0; size2--) {
                taskRecord.mActivities.get(size2).appTimeTracker = appTimeTracker;
            }
        }
        try {
            getDisplay().deferUpdateImeTarget();
            insertTaskAtTop(taskRecord, null);
            ActivityRecord topActivity2 = taskRecord.getTopActivity();
            if (topActivity2 != null && topActivity2.okToShowLocked()) {
                ActivityRecord activityRecord = topRunningActivityLocked();
                this.mStackSupervisor.moveFocusableActivityStackToFrontLocked(activityRecord, str);
                if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
                    Slog.v(TAG_TRANSITION, "Prepare to front transition: task=" + taskRecord);
                }
                if (z) {
                    this.mWindowManager.prepareAppTransition(0, false);
                    if (activityRecord != null) {
                        this.mStackSupervisor.mNoAnimActivities.add(activityRecord);
                    }
                    ActivityOptions.abort(activityOptions);
                } else {
                    updateTransitLocked(10, activityOptions);
                }
                if (canEnterPipOnTaskSwitch(topActivity, taskRecord, null, activityOptions)) {
                    topActivity.supportsEnterPipOnTaskSwitch = true;
                }
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                EventLog.writeEvent(EventLogTags.AM_TASK_TO_FRONT, Integer.valueOf(taskRecord.userId), Integer.valueOf(taskRecord.taskId));
                this.mService.mTaskChangeNotificationController.notifyTaskMovedToFront(taskRecord.taskId);
                return;
            }
            if (topActivity2 != null) {
                this.mStackSupervisor.mRecentTasks.add(topActivity2.getTask());
            }
            ActivityOptions.abort(activityOptions);
        } finally {
            getDisplay().continueUpdateImeTarget();
        }
    }

    final boolean moveTaskToBackLocked(int i) {
        boolean zActivityResuming;
        TaskRecord taskRecordTaskForIdLocked = taskForIdLocked(i);
        if (taskRecordTaskForIdLocked == null) {
            Slog.i(TAG, "moveTaskToBack: bad taskId=" + i);
            return false;
        }
        Slog.i(TAG, "moveTaskToBack: " + taskRecordTaskForIdLocked);
        if (!this.mService.getLockTaskController().canMoveTaskToBack(taskRecordTaskForIdLocked)) {
            return false;
        }
        if (isTopStackOnDisplay() && this.mService.mController != null) {
            ActivityRecord activityRecord = topRunningActivityLocked(null, i);
            if (activityRecord == null) {
                activityRecord = topRunningActivityLocked(null, 0);
            }
            if (activityRecord != null) {
                try {
                    zActivityResuming = this.mService.mController.activityResuming(activityRecord.packageName);
                } catch (RemoteException e) {
                    this.mService.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                    zActivityResuming = true;
                }
                if (!zActivityResuming) {
                    return false;
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TRANSITION) {
            Slog.v(TAG_TRANSITION, "Prepare to back transition: task=" + i);
        }
        this.mTaskHistory.remove(taskRecordTaskForIdLocked);
        this.mTaskHistory.add(0, taskRecordTaskForIdLocked);
        updateTaskMovement(taskRecordTaskForIdLocked, false);
        this.mWindowManager.prepareAppTransition(11, false);
        moveToBack("moveTaskToBackLocked", taskRecordTaskForIdLocked);
        if (inPinnedWindowingMode()) {
            this.mStackSupervisor.removeStack(this);
            return true;
        }
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        return true;
    }

    static void logStartActivity(int i, ActivityRecord activityRecord, TaskRecord taskRecord) {
        Uri data = activityRecord.intent.getData();
        EventLog.writeEvent(i, Integer.valueOf(activityRecord.userId), Integer.valueOf(System.identityHashCode(activityRecord)), Integer.valueOf(taskRecord.taskId), activityRecord.shortComponentName, activityRecord.intent.getAction(), activityRecord.intent.getType(), data != null ? data.toSafeString() : null, Integer.valueOf(activityRecord.intent.getFlags()));
    }

    void ensureVisibleActivitiesConfigurationLocked(ActivityRecord activityRecord, boolean z) {
        if (activityRecord == null || !activityRecord.visible) {
            return;
        }
        boolean zEnsureActivityConfiguration = false;
        boolean z2 = false;
        for (int iIndexOf = this.mTaskHistory.indexOf(activityRecord.getTask()); iIndexOf >= 0; iIndexOf--) {
            TaskRecord taskRecord = this.mTaskHistory.get(iIndexOf);
            ArrayList<ActivityRecord> arrayList = taskRecord.mActivities;
            int iIndexOf2 = activityRecord.getTask() == taskRecord ? arrayList.indexOf(activityRecord) : arrayList.size() - 1;
            while (true) {
                if (iIndexOf2 < 0) {
                    break;
                }
                ActivityRecord activityRecord2 = arrayList.get(iIndexOf2);
                zEnsureActivityConfiguration |= activityRecord2.ensureActivityConfiguration(0, z);
                if (activityRecord2.fullscreen) {
                    z2 = true;
                    break;
                }
                iIndexOf2--;
            }
            if (z2) {
                break;
            }
        }
        if (zEnsureActivityConfiguration) {
            this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    @Override
    public void requestResize(Rect rect) {
        this.mService.resizeStack(this.mStackId, rect, true, false, false, -1);
    }

    void resize(Rect rect, Rect rect2, Rect rect3) {
        if (!updateBoundsAllowed(rect, rect2, rect3)) {
            return;
        }
        if (rect2 == null) {
            rect2 = rect;
        }
        Rect rect4 = rect3 != null ? rect3 : rect2;
        this.mTmpBounds.clear();
        this.mTmpInsetBounds.clear();
        synchronized (this.mWindowManager.getWindowManagerLock()) {
            for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
                TaskRecord taskRecord = this.mTaskHistory.get(size);
                if (taskRecord.isResizeable()) {
                    if (inFreeformWindowingMode()) {
                        this.mTmpRect2.set(taskRecord.getOverrideBounds());
                        fitWithinBounds(this.mTmpRect2, rect);
                        taskRecord.updateOverrideConfiguration(this.mTmpRect2);
                    } else {
                        taskRecord.updateOverrideConfiguration(rect2, rect4);
                    }
                }
                this.mTmpBounds.put(taskRecord.taskId, taskRecord.getOverrideBounds());
                if (rect3 != null) {
                    this.mTmpInsetBounds.put(taskRecord.taskId, rect3);
                }
            }
            this.mWindowContainerController.resize(rect, this.mTmpBounds, this.mTmpInsetBounds);
            setBounds(rect);
        }
    }

    void onPipAnimationEndResize() {
        this.mWindowContainerController.onPipAnimationEndResize();
    }

    private static void fitWithinBounds(Rect rect, Rect rect2) {
        if (rect2 == null || rect2.isEmpty() || rect2.contains(rect)) {
            return;
        }
        if (rect.left < rect2.left || rect.right > rect2.right) {
            int iWidth = rect2.right - (rect2.width() / 3);
            int i = rect2.left - rect.left;
            if ((i < 0 && rect.left >= iWidth) || rect.left + i >= iWidth) {
                i = iWidth - rect.left;
            }
            rect.left += i;
            rect.right += i;
        }
        if (rect.top < rect2.top || rect.bottom > rect2.bottom) {
            int iHeight = rect2.bottom - (rect2.height() / 3);
            int i2 = rect2.top - rect.top;
            if ((i2 < 0 && rect.top >= iHeight) || rect.top + i2 >= iHeight) {
                i2 = iHeight - rect.top;
            }
            rect.top += i2;
            rect.bottom += i2;
        }
    }

    boolean willActivityBeVisibleLocked(IBinder iBinder) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (activityRecord.appToken == iBinder) {
                    return true;
                }
                if (activityRecord.fullscreen && !activityRecord.finishing) {
                    return false;
                }
            }
        }
        ActivityRecord activityRecordForTokenLocked = ActivityRecord.forTokenLocked(iBinder);
        if (activityRecordForTokenLocked == null) {
            return false;
        }
        if (activityRecordForTokenLocked.finishing) {
            Slog.e(TAG, "willActivityBeVisibleLocked: Returning false, would have returned true for r=" + activityRecordForTokenLocked);
        }
        return !activityRecordForTokenLocked.finishing;
    }

    void closeSystemDialogsLocked() {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if ((activityRecord.info.flags & 256) != 0) {
                    finishActivityLocked(activityRecord, 0, null, "close-sys", true);
                }
            }
        }
    }

    boolean finishDisabledPackageActivitiesLocked(String str, Set<String> set, boolean z, boolean z2, int i) {
        ComponentName componentName;
        int i2 = 0;
        int size = this.mTaskHistory.size() - 1;
        TaskRecord taskRecord = null;
        ComponentName componentName2 = null;
        boolean z3 = false;
        while (size >= 0) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            this.mTmpActivities.clear();
            this.mTmpActivities.addAll(arrayList);
            while (!this.mTmpActivities.isEmpty()) {
                ActivityRecord activityRecordRemove = this.mTmpActivities.remove(i2);
                int i3 = ((activityRecordRemove.packageName.equals(str) && (set == null || set.contains(activityRecordRemove.realActivity.getClassName()))) || (str == null && activityRecordRemove.userId == i)) ? 1 : i2;
                if ((i == -1 || activityRecordRemove.userId == i) && ((i3 != 0 || activityRecordRemove.getTask() == taskRecord) && (activityRecordRemove.app == null || z2 || !activityRecordRemove.app.persistent))) {
                    if (!z) {
                        if (!activityRecordRemove.finishing) {
                            return true;
                        }
                    } else {
                        if (!activityRecordRemove.isActivityTypeHome()) {
                            componentName = componentName2;
                        } else if (componentName2 != null && componentName2.equals(activityRecordRemove.realActivity)) {
                            Slog.i(TAG, "Skip force-stop again " + activityRecordRemove);
                        } else {
                            componentName = activityRecordRemove.realActivity;
                        }
                        Slog.i(TAG, "  Force finishing activity " + activityRecordRemove);
                        if (i3 != 0) {
                            if (activityRecordRemove.app != null) {
                                activityRecordRemove.app.removed = true;
                            }
                            activityRecordRemove.app = null;
                        }
                        TaskRecord task = activityRecordRemove.getTask();
                        finishActivityLocked(activityRecordRemove, 0, null, "force-stop", true);
                        z3 = true;
                        componentName2 = componentName;
                        taskRecord = task;
                    }
                }
                i2 = 0;
            }
            size--;
            i2 = 0;
        }
        return z3;
    }

    void getRunningTasks(List<TaskRecord> list, @WindowConfiguration.ActivityType int i, @WindowConfiguration.WindowingMode int i2, int i3, boolean z) {
        boolean z2 = true;
        boolean z3 = this.mStackSupervisor.getFocusedStack() == this;
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (taskRecord.getTopActivity() != null && ((z || taskRecord.isActivityTypeHome() || taskRecord.effectiveUid == i3) && ((i == 0 || taskRecord.getActivityType() != i) && (i2 == 0 || taskRecord.getWindowingMode() != i2)))) {
                if (z3 && z2) {
                    taskRecord.lastActiveTime = SystemClock.elapsedRealtime();
                    z2 = false;
                }
                list.add(taskRecord);
            }
        }
    }

    void unhandledBackLocked() {
        int size = this.mTaskHistory.size() - 1;
        if (ActivityManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Performing unhandledBack(): top activity at " + size);
        }
        if (size >= 0) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            int size2 = arrayList.size() - 1;
            if (size2 >= 0) {
                finishActivityLocked(arrayList.get(size2), 0, null, "unhandled-back", true);
            }
        }
    }

    boolean handleAppDiedLocked(ProcessRecord processRecord) {
        if (this.mPausingActivity != null && this.mPausingActivity.app == processRecord) {
            if (ActivityManagerDebugConfig.DEBUG_PAUSE || ActivityManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(TAG_PAUSE, "App died while pausing: " + this.mPausingActivity);
            }
            this.mPausingActivity = null;
        }
        if (this.mLastPausedActivity != null && this.mLastPausedActivity.app == processRecord) {
            this.mLastPausedActivity = null;
            this.mLastNoHistoryActivity = null;
        }
        return removeHistoryRecordsForAppLocked(processRecord);
    }

    void handleAppCrashLocked(ProcessRecord processRecord) {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord = arrayList.get(size2);
                if (activityRecord.app == processRecord) {
                    Slog.w(TAG, "  Force finishing activity " + activityRecord.intent.getComponent().flattenToShortString());
                    activityRecord.app = null;
                    this.mWindowManager.prepareAppTransition(26, false);
                    finishCurrentActivityLocked(activityRecord, 0, false, "handleAppCrashedLocked");
                }
            }
        }
    }

    boolean dumpActivitiesLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, boolean z, boolean z2, String str, boolean z3) {
        if (this.mTaskHistory.isEmpty()) {
            return false;
        }
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size += -1) {
            TaskRecord taskRecord = this.mTaskHistory.get(size);
            if (z3) {
                printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            printWriter.println("    Task id #" + taskRecord.taskId);
            printWriter.println("    mBounds=" + taskRecord.getOverrideBounds());
            printWriter.println("    mMinWidth=" + taskRecord.mMinWidth);
            printWriter.println("    mMinHeight=" + taskRecord.mMinHeight);
            printWriter.println("    mLastNonFullscreenBounds=" + taskRecord.mLastNonFullscreenBounds);
            printWriter.println("    * " + taskRecord);
            taskRecord.dump(printWriter, "      ");
            ActivityStackSupervisor.dumpHistoryList(fileDescriptor, printWriter, this.mTaskHistory.get(size).mActivities, "    ", "Hist", true, z ^ true, z2, str, false, null, taskRecord);
        }
        return true;
    }

    ArrayList<ActivityRecord> getDumpActivitiesLocked(String str) {
        ArrayList<ActivityRecord> arrayList = new ArrayList<>();
        if ("all".equals(str)) {
            for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
                arrayList.addAll(this.mTaskHistory.get(size).mActivities);
            }
        } else if ("top".equals(str)) {
            int size2 = this.mTaskHistory.size() - 1;
            if (size2 >= 0) {
                ArrayList<ActivityRecord> arrayList2 = this.mTaskHistory.get(size2).mActivities;
                int size3 = arrayList2.size() - 1;
                if (size3 >= 0) {
                    arrayList.add(arrayList2.get(size3));
                }
            }
        } else {
            ActivityManagerService.ItemMatcher itemMatcher = new ActivityManagerService.ItemMatcher();
            itemMatcher.build(str);
            for (int size4 = this.mTaskHistory.size() - 1; size4 >= 0; size4--) {
                for (ActivityRecord activityRecord : this.mTaskHistory.get(size4).mActivities) {
                    if (itemMatcher.match(activityRecord, activityRecord.intent.getComponent())) {
                        arrayList.add(activityRecord);
                    }
                }
            }
        }
        return arrayList;
    }

    ActivityRecord restartPackage(String str) {
        ActivityRecord activityRecord = topRunningActivityLocked();
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            ArrayList<ActivityRecord> arrayList = this.mTaskHistory.get(size).mActivities;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                ActivityRecord activityRecord2 = arrayList.get(size2);
                if (activityRecord2.info.packageName.equals(str)) {
                    activityRecord2.forceNewConfig = true;
                    if (activityRecord != null && activityRecord2 == activityRecord && activityRecord2.visible) {
                        activityRecord2.startFreezingScreenLocked(activityRecord.app, 256);
                    }
                }
            }
        }
        return activityRecord;
    }

    void removeTask(TaskRecord taskRecord, String str, int i) {
        Iterator<ActivityRecord> it = taskRecord.mActivities.iterator();
        while (it.hasNext()) {
            onActivityRemovedFromStack(it.next());
        }
        boolean z = false;
        if (this.mTaskHistory.remove(taskRecord)) {
            EventLog.writeEvent(EventLogTags.AM_REMOVE_TASK, Integer.valueOf(taskRecord.taskId), Integer.valueOf(getStackId()));
        }
        removeActivitiesFromLRUListLocked(taskRecord);
        updateTaskMovement(taskRecord, true);
        if (i == 0 && taskRecord.mActivities.isEmpty()) {
            if (taskRecord.voiceSession != null) {
                z = true;
            }
            if (z) {
                try {
                    taskRecord.voiceSession.taskFinished(taskRecord.intent, taskRecord.taskId);
                } catch (RemoteException e) {
                }
            }
            if (taskRecord.autoRemoveFromRecents() || z) {
                this.mStackSupervisor.mRecentTasks.remove(taskRecord);
            }
            taskRecord.removeWindowContainer();
        }
        if (this.mTaskHistory.isEmpty()) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.i(TAG_STACK, "removeTask: removing stack=" + this);
            }
            if (isOnHomeDisplay() && i != 2 && this.mStackSupervisor.isFocusedStack(this)) {
                String str2 = str + " leftTaskHistoryEmpty";
                if (!inMultiWindowMode() || !adjustFocusToNextFocusableStack(str2)) {
                    this.mStackSupervisor.moveHomeStackToFront(str2);
                }
            }
            if (isAttached()) {
                getDisplay().positionChildAtBottom(this);
            }
            if (!isActivityTypeHome()) {
                remove();
            }
        }
        taskRecord.setStack(null);
        if (inPinnedWindowingMode()) {
            this.mService.mTaskChangeNotificationController.notifyActivityUnpinned();
        }
    }

    TaskRecord createTaskRecord(int i, ActivityInfo activityInfo, Intent intent, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, boolean z) {
        return createTaskRecord(i, activityInfo, intent, iVoiceInteractionSession, iVoiceInteractor, z, null, null, null);
    }

    TaskRecord createTaskRecord(int i, ActivityInfo activityInfo, Intent intent, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, boolean z, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions) {
        TaskRecord taskRecordCreate = TaskRecord.create(this.mService, i, activityInfo, intent, iVoiceInteractionSession, iVoiceInteractor);
        addTask(taskRecordCreate, z, "createTaskRecord");
        boolean zIsKeyguardOrAodShowing = this.mService.mStackSupervisor.getKeyguardController().isKeyguardOrAodShowing(this.mDisplayId != -1 ? this.mDisplayId : 0);
        if (!this.mStackSupervisor.getLaunchParamsController().layoutTask(taskRecordCreate, activityInfo.windowLayout, activityRecord, activityRecord2, activityOptions) && !matchParentBounds() && taskRecordCreate.isResizeable() && !zIsKeyguardOrAodShowing) {
            taskRecordCreate.updateOverrideConfiguration(getOverrideBounds());
        }
        taskRecordCreate.createWindowContainer(z, (activityInfo.flags & 1024) != 0);
        return taskRecordCreate;
    }

    ArrayList<TaskRecord> getAllTasks() {
        return new ArrayList<>(this.mTaskHistory);
    }

    void addTask(TaskRecord taskRecord, boolean z, String str) {
        addTask(taskRecord, z ? Integer.MAX_VALUE : 0, true, str);
        if (z) {
            this.mWindowContainerController.positionChildAtTop(taskRecord.getWindowContainerController(), true);
        }
    }

    void addTask(TaskRecord taskRecord, int i, boolean z, String str) {
        this.mTaskHistory.remove(taskRecord);
        int adjustedPositionForTask = getAdjustedPositionForTask(taskRecord, i, null);
        boolean z2 = adjustedPositionForTask >= this.mTaskHistory.size();
        ActivityStack activityStackPreAddTask = preAddTask(taskRecord, str, z2);
        this.mTaskHistory.add(adjustedPositionForTask, taskRecord);
        taskRecord.setStack(this);
        updateTaskMovement(taskRecord, z2);
        postAddTask(taskRecord, activityStackPreAddTask, z);
    }

    void positionChildAt(TaskRecord taskRecord, int i) {
        if (taskRecord.getStack() != this) {
            throw new IllegalArgumentException("AS.positionChildAt: task=" + taskRecord + " is not a child of stack=" + this + " current parent=" + taskRecord.getStack());
        }
        taskRecord.updateOverrideConfigurationForStack(this);
        ActivityRecord activityRecord = taskRecord.topRunningActivityLocked();
        boolean z = activityRecord == taskRecord.getStack().mResumedActivity;
        insertTaskAtPosition(taskRecord, i);
        taskRecord.setStack(this);
        postAddTask(taskRecord, null, true);
        if (z) {
            if (this.mResumedActivity != null) {
                Log.wtf(TAG, "mResumedActivity was already set when moving mResumedActivity from other stack to this stack mResumedActivity=" + this.mResumedActivity + " other mResumedActivity=" + activityRecord);
            }
            activityRecord.setState(ActivityState.RESUMED, "positionChildAt");
        }
        ensureActivitiesVisibleLocked(null, 0, false);
        this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
    }

    private ActivityStack preAddTask(TaskRecord taskRecord, String str, boolean z) {
        ActivityStack<T> stack = taskRecord.getStack();
        if (stack != null && stack != this) {
            stack.removeTask(taskRecord, str, z ? 2 : 1);
        }
        return stack;
    }

    private void postAddTask(TaskRecord taskRecord, ActivityStack activityStack, boolean z) {
        if (z && activityStack != null) {
            this.mStackSupervisor.scheduleUpdatePictureInPictureModeIfNeeded(taskRecord, activityStack);
        } else if (taskRecord.voiceSession != null) {
            try {
                taskRecord.voiceSession.taskStarted(taskRecord.intent, taskRecord.taskId);
            } catch (RemoteException e) {
            }
        }
    }

    void moveToFrontAndResumeStateIfNeeded(ActivityRecord activityRecord, boolean z, boolean z2, boolean z3, String str) {
        if (!z) {
            return;
        }
        if (z2) {
            activityRecord.setState(ActivityState.RESUMED, "moveToFrontAndResumeStateIfNeeded");
            updateLRUListLocked(activityRecord);
        }
        if (z3) {
            this.mPausingActivity = activityRecord;
            schedulePauseTimeout(activityRecord);
        }
        moveToFront(str);
    }

    public int getStackId() {
        return this.mStackId;
    }

    public String toString() {
        return "ActivityStack{" + Integer.toHexString(System.identityHashCode(this)) + " stackId=" + this.mStackId + " type=" + WindowConfiguration.activityTypeToString(getActivityType()) + " mode=" + WindowConfiguration.windowingModeToString(getWindowingMode()) + " visible=" + shouldBeVisible(null) + " translucent=" + isStackTranslucent(null) + ", " + this.mTaskHistory.size() + " tasks}";
    }

    void onLockTaskPackagesUpdated() {
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            this.mTaskHistory.get(size).setLockTaskAuth();
        }
    }

    void executeAppTransition(ActivityOptions activityOptions) {
        this.mWindowManager.executeAppTransition();
        ActivityOptions.abort(activityOptions);
    }

    boolean shouldSleepActivities() {
        ActivityDisplay display = getDisplay();
        if (this.mStackSupervisor.getFocusedStack() == this && this.mStackSupervisor.getKeyguardController().isKeyguardGoingAway()) {
            return false;
        }
        return display != null ? display.isSleeping() : this.mService.isSleepingLocked();
    }

    boolean shouldSleepOrShutDownActivities() {
        return shouldSleepActivities() || this.mService.isShuttingDownLocked();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, false);
        protoOutputStream.write(1120986464258L, this.mStackId);
        for (int size = this.mTaskHistory.size() - 1; size >= 0; size--) {
            this.mTaskHistory.get(size).writeToProto(protoOutputStream, 2246267895811L);
        }
        if (this.mResumedActivity != null) {
            this.mResumedActivity.writeIdentifierToProto(protoOutputStream, 1146756268036L);
        }
        protoOutputStream.write(1120986464261L, this.mDisplayId);
        if (!matchParentBounds()) {
            getOverrideBounds().writeToProto(protoOutputStream, 1146756268039L);
        }
        protoOutputStream.write(1133871366150L, matchParentBounds());
        protoOutputStream.end(jStart);
    }
}
