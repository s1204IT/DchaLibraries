package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.util.XmlUtils;
import com.android.server.am.ActivityStack;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.pm.DumpState;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.wm.AppWindowContainerController;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.StackWindowController;
import com.android.server.wm.TaskWindowContainerController;
import com.android.server.wm.TaskWindowContainerListener;
import com.android.server.wm.WindowManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class TaskRecord extends ConfigurationContainer implements TaskWindowContainerListener {
    private static final String ATTR_AFFINITY = "affinity";
    private static final String ATTR_ASKEDCOMPATMODE = "asked_compat_mode";
    private static final String ATTR_AUTOREMOVERECENTS = "auto_remove_recents";
    private static final String ATTR_CALLING_PACKAGE = "calling_package";
    private static final String ATTR_CALLING_UID = "calling_uid";
    private static final String ATTR_EFFECTIVE_UID = "effective_uid";
    private static final String ATTR_LASTDESCRIPTION = "last_description";
    private static final String ATTR_LASTTIMEMOVED = "last_time_moved";
    private static final String ATTR_MIN_HEIGHT = "min_height";
    private static final String ATTR_MIN_WIDTH = "min_width";
    private static final String ATTR_NEVERRELINQUISH = "never_relinquish_identity";
    private static final String ATTR_NEXT_AFFILIATION = "next_affiliation";
    private static final String ATTR_NON_FULLSCREEN_BOUNDS = "non_fullscreen_bounds";
    private static final String ATTR_ORIGACTIVITY = "orig_activity";
    private static final String ATTR_PERSIST_TASK_VERSION = "persist_task_version";
    private static final String ATTR_PREV_AFFILIATION = "prev_affiliation";
    private static final String ATTR_REALACTIVITY = "real_activity";
    private static final String ATTR_REALACTIVITY_SUSPENDED = "real_activity_suspended";
    private static final String ATTR_RESIZE_MODE = "resize_mode";
    private static final String ATTR_ROOTHASRESET = "root_has_reset";
    private static final String ATTR_ROOT_AFFINITY = "root_affinity";
    private static final String ATTR_SUPPORTS_PICTURE_IN_PICTURE = "supports_picture_in_picture";
    private static final String ATTR_TASKID = "task_id";

    @Deprecated
    private static final String ATTR_TASKTYPE = "task_type";
    private static final String ATTR_TASK_AFFILIATION = "task_affiliation";
    private static final String ATTR_TASK_AFFILIATION_COLOR = "task_affiliation_color";
    private static final String ATTR_USERID = "user_id";
    private static final String ATTR_USER_SETUP_COMPLETE = "user_setup_complete";
    private static final int INVALID_MIN_SIZE = -1;
    static final int INVALID_TASK_ID = -1;
    static final int LOCK_TASK_AUTH_DONT_LOCK = 0;
    static final int LOCK_TASK_AUTH_LAUNCHABLE = 2;
    static final int LOCK_TASK_AUTH_LAUNCHABLE_PRIV = 4;
    static final int LOCK_TASK_AUTH_PINNABLE = 1;
    static final int LOCK_TASK_AUTH_WHITELISTED = 3;
    private static final int PERSIST_TASK_VERSION = 1;
    static final int REPARENT_KEEP_STACK_AT_FRONT = 1;
    static final int REPARENT_LEAVE_STACK_IN_PLACE = 2;
    static final int REPARENT_MOVE_STACK_TO_FRONT = 0;
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_AFFINITYINTENT = "affinity_intent";
    private static final String TAG_INTENT = "intent";
    private static TaskRecordFactory sTaskRecordFactory;
    String affinity;
    Intent affinityIntent;
    boolean askedCompatMode;
    boolean autoRemoveRecents;
    int effectiveUid;
    boolean hasBeenVisible;
    boolean inRecents;
    Intent intent;
    boolean isAvailable;
    boolean isPersistable;
    long lastActiveTime;
    CharSequence lastDescription;
    ActivityManager.TaskDescription lastTaskDescription;
    final ArrayList<ActivityRecord> mActivities;
    int mAffiliatedTaskColor;
    int mAffiliatedTaskId;
    String mCallingPackage;
    int mCallingUid;
    Rect mLastNonFullscreenBounds;
    long mLastTimeMoved;
    int mLayerRank;
    int mLockTaskAuth;
    int mLockTaskUid;
    int mMinHeight;
    int mMinWidth;
    private boolean mNeverRelinquishIdentity;
    TaskRecord mNextAffiliate;
    int mNextAffiliateTaskId;
    TaskRecord mPrevAffiliate;
    int mPrevAffiliateTaskId;
    int mResizeMode;
    private boolean mReuseTask;
    private ProcessRecord mRootProcess;
    final ActivityManagerService mService;
    private ActivityStack mStack;
    private boolean mSupportsPictureInPicture;
    private Configuration mTmpConfig;
    private final Rect mTmpNonDecorBounds;
    private final Rect mTmpRect;
    private final Rect mTmpStableBounds;
    boolean mUserSetupComplete;
    private TaskWindowContainerController mWindowContainerController;
    int maxRecents;
    int numFullscreen;
    ComponentName origActivity;
    ComponentName realActivity;
    boolean realActivitySuspended;
    String rootAffinity;
    boolean rootWasReset;
    String stringName;
    final int taskId;
    int userId;
    final IVoiceInteractor voiceInteractor;
    final IVoiceInteractionSession voiceSession;
    private static final String TAG = "ActivityManager";
    private static final String TAG_ADD_REMOVE = TAG + ActivityManagerDebugConfig.POSTFIX_ADD_REMOVE;
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_LOCKTASK = TAG + ActivityManagerDebugConfig.POSTFIX_LOCKTASK;
    private static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;

    @Retention(RetentionPolicy.SOURCE)
    @interface ReparentMoveStackMode {
    }

    TaskRecord(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
        this.mLockTaskAuth = 1;
        this.mLockTaskUid = -1;
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mNeverRelinquishIdentity = true;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mTmpStableBounds = new Rect();
        this.mTmpNonDecorBounds = new Rect();
        this.mTmpRect = new Rect();
        this.mLastNonFullscreenBounds = null;
        this.mLayerRank = -1;
        this.mTmpConfig = new Configuration();
        this.mService = activityManagerService;
        this.userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        this.taskId = i;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.mAffiliatedTaskId = i;
        this.voiceSession = iVoiceInteractionSession;
        this.voiceInteractor = iVoiceInteractor;
        this.isAvailable = true;
        this.mActivities = new ArrayList<>();
        this.mCallingUid = activityInfo.applicationInfo.uid;
        this.mCallingPackage = activityInfo.packageName;
        setIntent(intent, activityInfo);
        setMinDimensions(activityInfo);
        touchActiveTime();
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(i, this.realActivity);
    }

    TaskRecord(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, ActivityManager.TaskDescription taskDescription) {
        this.mLockTaskAuth = 1;
        this.mLockTaskUid = -1;
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mNeverRelinquishIdentity = true;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mTmpStableBounds = new Rect();
        this.mTmpNonDecorBounds = new Rect();
        this.mTmpRect = new Rect();
        this.mLastNonFullscreenBounds = null;
        this.mLayerRank = -1;
        this.mTmpConfig = new Configuration();
        this.mService = activityManagerService;
        this.userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        this.taskId = i;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.mAffiliatedTaskId = i;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.isAvailable = true;
        this.mActivities = new ArrayList<>();
        this.mCallingUid = activityInfo.applicationInfo.uid;
        this.mCallingPackage = activityInfo.packageName;
        setIntent(intent, activityInfo);
        setMinDimensions(activityInfo);
        this.isPersistable = true;
        this.maxRecents = Math.min(Math.max(activityInfo.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
        this.lastTaskDescription = taskDescription;
        touchActiveTime();
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(i, this.realActivity);
    }

    TaskRecord(ActivityManagerService activityManagerService, int i, Intent intent, Intent intent2, String str, String str2, ComponentName componentName, ComponentName componentName2, boolean z, boolean z2, boolean z3, int i2, int i3, String str3, ArrayList<ActivityRecord> arrayList, long j, boolean z4, ActivityManager.TaskDescription taskDescription, int i4, int i5, int i6, int i7, int i8, String str4, int i9, boolean z5, boolean z6, boolean z7, int i10, int i11) {
        this.mLockTaskAuth = 1;
        this.mLockTaskUid = -1;
        this.lastTaskDescription = new ActivityManager.TaskDescription();
        this.isPersistable = false;
        this.mLastTimeMoved = System.currentTimeMillis();
        this.mNeverRelinquishIdentity = true;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mTmpStableBounds = new Rect();
        this.mTmpNonDecorBounds = new Rect();
        this.mTmpRect = new Rect();
        this.mLastNonFullscreenBounds = null;
        this.mLayerRank = -1;
        this.mTmpConfig = new Configuration();
        this.mService = activityManagerService;
        this.taskId = i;
        this.intent = intent;
        this.affinityIntent = intent2;
        this.affinity = str;
        this.rootAffinity = str2;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.realActivity = componentName;
        this.realActivitySuspended = z6;
        this.origActivity = componentName2;
        this.rootWasReset = z;
        this.isAvailable = true;
        this.autoRemoveRecents = z2;
        this.askedCompatMode = z3;
        this.userId = i2;
        this.mUserSetupComplete = z7;
        this.effectiveUid = i3;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.lastDescription = str3;
        this.mActivities = arrayList;
        this.mLastTimeMoved = j;
        this.mNeverRelinquishIdentity = z4;
        this.lastTaskDescription = taskDescription;
        this.mAffiliatedTaskId = i4;
        this.mAffiliatedTaskColor = i7;
        this.mPrevAffiliateTaskId = i5;
        this.mNextAffiliateTaskId = i6;
        this.mCallingUid = i8;
        this.mCallingPackage = str4;
        this.mResizeMode = i9;
        this.mSupportsPictureInPicture = z5;
        this.mMinWidth = i10;
        this.mMinHeight = i11;
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(i, this.realActivity);
    }

    TaskWindowContainerController getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void createWindowContainer(boolean z, boolean z2) {
        if (this.mWindowContainerController != null) {
            throw new IllegalArgumentException("Window container=" + this.mWindowContainerController + " already created for task=" + this);
        }
        setWindowContainerController(new TaskWindowContainerController(this.taskId, this, getStack().getWindowContainerController(), this.userId, updateOverrideConfigurationFromLaunchBounds(), this.mResizeMode, this.mSupportsPictureInPicture, z, z2, this.lastTaskDescription));
    }

    @VisibleForTesting
    protected void setWindowContainerController(TaskWindowContainerController taskWindowContainerController) {
        if (this.mWindowContainerController != null) {
            throw new IllegalArgumentException("Window container=" + this.mWindowContainerController + " already created for task=" + this);
        }
        this.mWindowContainerController = taskWindowContainerController;
    }

    void removeWindowContainer() {
        this.mService.getLockTaskController().clearLockedTask(this);
        this.mWindowContainerController.removeContainer();
        if (!getWindowConfiguration().persistTaskBounds()) {
            updateOverrideConfiguration(null);
        }
        this.mService.mTaskChangeNotificationController.notifyTaskRemoved(this.taskId);
        this.mWindowContainerController = null;
    }

    @Override
    public void onSnapshotChanged(ActivityManager.TaskSnapshot taskSnapshot) {
        this.mService.mTaskChangeNotificationController.notifyTaskSnapshotChanged(this.taskId, taskSnapshot);
    }

    void setResizeMode(int i) {
        if (this.mResizeMode == i) {
            return;
        }
        this.mResizeMode = i;
        this.mWindowContainerController.setResizeable(i);
        this.mService.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
        this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
    }

    void setTaskDockedResizing(boolean z) {
        this.mWindowContainerController.setTaskDockedResizing(z);
    }

    @Override
    public void requestResize(Rect rect, int i) {
        this.mService.resizeTask(this.taskId, rect, i);
    }

    boolean resize(Rect rect, int i, boolean z, boolean z2) {
        ActivityRecord activityRecord;
        this.mService.mWindowManager.deferSurfaceLayout();
        try {
            boolean zEnsureActivityConfiguration = true;
            if (!isResizeable()) {
                Slog.w(TAG, "resizeTask: task " + this + " not resizeable.");
                return true;
            }
            boolean z3 = (i & 2) != 0;
            if (equivalentOverrideBounds(rect) && !z3) {
                return true;
            }
            if (this.mWindowContainerController == null) {
                updateOverrideConfiguration(rect);
                if (!inFreeformWindowingMode()) {
                    this.mService.mStackSupervisor.restoreRecentTaskLocked(this, null, false);
                }
                return true;
            }
            if (!canResizeToBounds(rect)) {
                throw new IllegalArgumentException("resizeTask: Can not resize task=" + this + " to bounds=" + rect + " resizeMode=" + this.mResizeMode);
            }
            Trace.traceBegin(64L, "am.resizeTask_" + this.taskId);
            if (updateOverrideConfiguration(rect) && (activityRecord = topRunningActivityLocked()) != null && !z2) {
                zEnsureActivityConfiguration = activityRecord.ensureActivityConfiguration(0, z);
                this.mService.mStackSupervisor.ensureActivitiesVisibleLocked(activityRecord, 0, false);
                if (!zEnsureActivityConfiguration) {
                    this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                }
            }
            this.mWindowContainerController.resize(zEnsureActivityConfiguration, z3);
            Trace.traceEnd(64L);
            return zEnsureActivityConfiguration;
        } finally {
            this.mService.mWindowManager.continueSurfaceLayout();
        }
    }

    void resizeWindowContainer() {
        this.mWindowContainerController.resize(false, false);
    }

    void getWindowContainerBounds(Rect rect) {
        this.mWindowContainerController.getBounds(rect);
    }

    boolean reparent(ActivityStack activityStack, boolean z, int i, boolean z2, boolean z3, String str) {
        return reparent(activityStack, z ? Integer.MAX_VALUE : 0, i, z2, z3, true, str);
    }

    boolean reparent(ActivityStack activityStack, boolean z, int i, boolean z2, boolean z3, boolean z4, String str) {
        return reparent(activityStack, z ? Integer.MAX_VALUE : 0, i, z2, z3, z4, str);
    }

    boolean reparent(ActivityStack activityStack, int i, int i2, boolean z, boolean z2, String str) {
        return reparent(activityStack, i, i2, z, z2, true, str);
    }

    boolean reparent(ActivityStack activityStack, int i, int i2, boolean z, boolean z2, boolean z3, String str) throws Throwable {
        WindowManagerService windowManagerService;
        ActivityRecord activityRecord;
        WindowManagerService windowManagerService2;
        boolean z4;
        boolean z5;
        boolean z6;
        ActivityStack activityStack2;
        ?? r21;
        ActivityRecord activityRecord2;
        boolean z7;
        int i3;
        boolean zResize;
        ?? r2;
        int i4;
        ?? r13 = this.mService.mStackSupervisor;
        WindowManagerService windowManagerService3 = this.mService.mWindowManager;
        ?? stack = getStack();
        ActivityStack reparentTargetStack = r13.getReparentTargetStack(this, activityStack, i == Integer.MAX_VALUE);
        if (reparentTargetStack == stack || !canBeLaunchedOnDisplay(reparentTargetStack.mDisplayId)) {
            return false;
        }
        int windowingMode = reparentTargetStack.getWindowingMode();
        ActivityRecord topActivity = getTopActivity();
        boolean z8 = topActivity != null && replaceWindowsOnTaskMove(getWindowingMode(), windowingMode);
        if (z8) {
            windowManagerService3.setWillReplaceWindow(topActivity.appToken, z);
        }
        windowManagerService3.deferSurfaceLayout();
        try {
            ActivityRecord activityRecord3 = topRunningActivityLocked();
            boolean z9 = activityRecord3 != null && r13.isFocusedStack(stack) && topRunningActivityLocked() == activityRecord3;
            boolean z10 = activityRecord3 != null && stack.getResumedActivity() == activityRecord3;
            if (activityRecord3 != null) {
                activityRecord = topActivity;
                boolean z11 = stack.mPausingActivity == activityRecord3;
                boolean z12 = activityRecord3 == null && stack.isTopStackOnDisplay() && stack.topRunningActivityLocked() == activityRecord3;
                int adjustedPositionForTask = reparentTargetStack.getAdjustedPositionForTask(this, i, null);
                TaskWindowContainerController taskWindowContainerController = this.mWindowContainerController;
                StackWindowController windowContainerController = reparentTargetStack.getWindowContainerController();
                if (i2 != 0) {
                    windowManagerService2 = windowManagerService3;
                    z4 = true;
                } else {
                    windowManagerService2 = windowManagerService3;
                    z4 = false;
                }
                try {
                    taskWindowContainerController.reparent(windowContainerController, adjustedPositionForTask, z4);
                    if (i2 == 0) {
                        z5 = true;
                        if (i2 != 1 || (!z9 && !z12)) {
                            z6 = false;
                        }
                        stack.removeTask(this, str, z6 ? 2 : z5);
                        reparentTargetStack.addTask(this, adjustedPositionForTask, false, str);
                        if (z3) {
                            r13.scheduleUpdatePictureInPictureModeIfNeeded(this, stack);
                        }
                        if (this.voiceSession != null) {
                            try {
                                this.voiceSession.taskStarted(this.intent, this.taskId);
                            } catch (RemoteException e) {
                            }
                        }
                        if (activityRecord3 != null) {
                            activityRecord2 = activityRecord;
                            i3 = windowingMode;
                            activityStack2 = reparentTargetStack;
                            r21 = r13;
                            z7 = true;
                            reparentTargetStack.moveToFrontAndResumeStateIfNeeded(activityRecord3, z6, z10, z11, str);
                        } else {
                            activityStack2 = reparentTargetStack;
                            r21 = r13;
                            activityRecord2 = activityRecord;
                            z7 = z5;
                            i3 = windowingMode;
                        }
                        if (!z) {
                            this.mService.mStackSupervisor.mNoAnimActivities.add(activityRecord2);
                        }
                        activityStack2.prepareFreezingTaskBounds();
                        boolean z13 = i3 == 3 ? z7 : false;
                        Rect overrideBounds = getOverrideBounds();
                        if ((i3 != z7 && i3 != 4) || Objects.equals(overrideBounds, activityStack2.getOverrideBounds())) {
                            if (i3 == 5) {
                                Rect launchBounds = getLaunchBounds();
                                if (launchBounds == null) {
                                    this.mService.mStackSupervisor.getLaunchParamsController().layoutTask(this, null);
                                    launchBounds = overrideBounds;
                                }
                                zResize = resize(launchBounds, 2, !z8, z2);
                            } else if (z13 || i3 == 2) {
                                if (z13 && i2 == z7) {
                                    this.mService.mStackSupervisor.moveRecentsStackToFront(str);
                                }
                                zResize = resize(activityStack2.getOverrideBounds(), 0, !z8, z2);
                            } else {
                                zResize = z7;
                            }
                        }
                        WindowManagerService windowManagerService4 = windowManagerService2;
                        windowManagerService4.continueSurfaceLayout();
                        if (z8) {
                            windowManagerService4.scheduleClearWillReplaceWindows(activityRecord2.appToken, !zResize ? z7 : false);
                        }
                        if (z2) {
                            r2 = r21;
                            i4 = 0;
                        } else {
                            ?? r22 = r21;
                            i4 = 0;
                            r22.ensureActivitiesVisibleLocked(null, 0, !z8);
                            r22.resumeFocusedStackTopActivityLocked();
                            r2 = r22;
                        }
                        r2.handleNonResizableTaskIfNeeded(this, activityStack.getWindowingMode(), i4, activityStack2);
                        return activityStack == activityStack2 ? z7 : i4;
                    }
                    z5 = true;
                    z6 = z5;
                    stack.removeTask(this, str, z6 ? 2 : z5);
                    reparentTargetStack.addTask(this, adjustedPositionForTask, false, str);
                    if (z3) {
                    }
                    if (this.voiceSession != null) {
                    }
                    if (activityRecord3 != null) {
                    }
                    if (!z) {
                    }
                    activityStack2.prepareFreezingTaskBounds();
                    if (i3 == 3) {
                    }
                    Rect overrideBounds2 = getOverrideBounds();
                    zResize = i3 != z7 ? resize(activityStack2.getOverrideBounds(), 0, !z8, z2) : resize(activityStack2.getOverrideBounds(), 0, !z8, z2);
                    WindowManagerService windowManagerService42 = windowManagerService2;
                    windowManagerService42.continueSurfaceLayout();
                    if (z8) {
                    }
                    if (z2) {
                    }
                    r2.handleNonResizableTaskIfNeeded(this, activityStack.getWindowingMode(), i4, activityStack2);
                    if (activityStack == activityStack2) {
                    }
                } catch (Throwable th) {
                    th = th;
                    windowManagerService = windowManagerService2;
                }
            } else {
                activityRecord = topActivity;
            }
            if (activityRecord3 == null) {
                int adjustedPositionForTask2 = reparentTargetStack.getAdjustedPositionForTask(this, i, null);
                TaskWindowContainerController taskWindowContainerController2 = this.mWindowContainerController;
                StackWindowController windowContainerController2 = reparentTargetStack.getWindowContainerController();
                if (i2 != 0) {
                }
                taskWindowContainerController2.reparent(windowContainerController2, adjustedPositionForTask2, z4);
                if (i2 == 0) {
                }
                z6 = z5;
                stack.removeTask(this, str, z6 ? 2 : z5);
                reparentTargetStack.addTask(this, adjustedPositionForTask2, false, str);
                if (z3) {
                }
                if (this.voiceSession != null) {
                }
                if (activityRecord3 != null) {
                }
                if (!z) {
                }
                activityStack2.prepareFreezingTaskBounds();
                if (i3 == 3) {
                }
                Rect overrideBounds22 = getOverrideBounds();
                if (i3 != z7) {
                }
                WindowManagerService windowManagerService422 = windowManagerService2;
                windowManagerService422.continueSurfaceLayout();
                if (z8) {
                }
                if (z2) {
                }
                r2.handleNonResizableTaskIfNeeded(this, activityStack.getWindowingMode(), i4, activityStack2);
                if (activityStack == activityStack2) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            windowManagerService = windowManagerService3;
        }
        windowManagerService.continueSurfaceLayout();
        throw th;
    }

    private static boolean replaceWindowsOnTaskMove(int i, int i2) {
        return i == 5 || i2 == 5;
    }

    void cancelWindowTransition() {
        this.mWindowContainerController.cancelWindowTransition();
    }

    ActivityManager.TaskSnapshot getSnapshot(boolean z) {
        return this.mService.mWindowManager.getTaskSnapshot(this.taskId, this.userId, z);
    }

    void touchActiveTime() {
        this.lastActiveTime = SystemClock.elapsedRealtime();
    }

    long getInactiveDuration() {
        return SystemClock.elapsedRealtime() - this.lastActiveTime;
    }

    void setIntent(ActivityRecord activityRecord) {
        this.mCallingUid = activityRecord.launchedFromUid;
        this.mCallingPackage = activityRecord.launchedFromPackage;
        setIntent(activityRecord.intent, activityRecord.info);
        setLockTaskAuth(activityRecord);
    }

    private void setIntent(Intent intent, ActivityInfo activityInfo) {
        int flags;
        if (this.intent == null) {
            this.mNeverRelinquishIdentity = (activityInfo.flags & 4096) == 0;
        } else if (this.mNeverRelinquishIdentity) {
            return;
        }
        this.affinity = activityInfo.taskAffinity;
        if (this.intent == null) {
            this.rootAffinity = this.affinity;
        }
        this.effectiveUid = activityInfo.applicationInfo.uid;
        this.stringName = null;
        if (activityInfo.targetActivity == null) {
            if (intent != null && (intent.getSelector() != null || intent.getSourceBounds() != null)) {
                Intent intent2 = new Intent(intent);
                intent2.setSelector(null);
                intent2.setSourceBounds(null);
                intent = intent2;
            }
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.v(TAG_TASKS, "Setting Intent of " + this + " to " + intent);
            }
            this.intent = intent;
            this.realActivity = intent != null ? intent.getComponent() : null;
            this.origActivity = null;
        } else {
            ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
            if (intent != null) {
                Intent intent3 = new Intent(intent);
                intent3.setComponent(componentName);
                intent3.setSelector(null);
                intent3.setSourceBounds(null);
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.v(TAG_TASKS, "Setting Intent of " + this + " to target " + intent3);
                }
                this.intent = intent3;
                this.realActivity = componentName;
                this.origActivity = intent.getComponent();
            } else {
                this.intent = null;
                this.realActivity = componentName;
                this.origActivity = new ComponentName(activityInfo.packageName, activityInfo.name);
            }
        }
        if (this.intent != null) {
            flags = this.intent.getFlags();
        } else {
            flags = 0;
        }
        if ((2097152 & flags) != 0) {
            this.rootWasReset = true;
        }
        this.userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        this.mUserSetupComplete = Settings.Secure.getIntForUser(this.mService.mContext.getContentResolver(), ATTR_USER_SETUP_COMPLETE, 0, this.userId) != 0;
        if ((activityInfo.flags & 8192) != 0) {
            this.autoRemoveRecents = true;
        } else if ((flags & 532480) != 524288 || activityInfo.documentLaunchMode != 0) {
            this.autoRemoveRecents = false;
        } else {
            this.autoRemoveRecents = true;
        }
        this.mResizeMode = activityInfo.resizeMode;
        this.mSupportsPictureInPicture = activityInfo.supportsPictureInPicture();
    }

    private void setMinDimensions(ActivityInfo activityInfo) {
        if (activityInfo != null && activityInfo.windowLayout != null) {
            this.mMinWidth = activityInfo.windowLayout.minWidth;
            this.mMinHeight = activityInfo.windowLayout.minHeight;
        } else {
            this.mMinWidth = -1;
            this.mMinHeight = -1;
        }
    }

    boolean isSameIntentFilter(ActivityRecord activityRecord) {
        Intent intent = new Intent(activityRecord.intent);
        intent.setComponent(activityRecord.realActivity);
        return intent.filterEquals(this.intent);
    }

    boolean returnsToHomeStack() {
        return this.intent != null && (this.intent.getFlags() & 268451840) == 268451840;
    }

    void setPrevAffiliate(TaskRecord taskRecord) {
        this.mPrevAffiliate = taskRecord;
        this.mPrevAffiliateTaskId = taskRecord == null ? -1 : taskRecord.taskId;
    }

    void setNextAffiliate(TaskRecord taskRecord) {
        this.mNextAffiliate = taskRecord;
        this.mNextAffiliateTaskId = taskRecord == null ? -1 : taskRecord.taskId;
    }

    <T extends ActivityStack> T getStack() {
        return (T) this.mStack;
    }

    void setStack(ActivityStack activityStack) {
        if (activityStack != null && !activityStack.isInStackLocked(this)) {
            throw new IllegalStateException("Task must be added as a Stack child first.");
        }
        ActivityStack activityStack2 = this.mStack;
        this.mStack = activityStack;
        if (activityStack2 != this.mStack) {
            for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityRecord childAt = getChildAt(childCount);
                if (activityStack2 != null) {
                    activityStack2.onActivityRemovedFromStack(childAt);
                }
                if (this.mStack != null) {
                    activityStack.onActivityAddedToStack(childAt);
                }
            }
        }
        onParentChanged();
    }

    int getStackId() {
        if (this.mStack != null) {
            return this.mStack.mStackId;
        }
        return -1;
    }

    @Override
    protected int getChildCount() {
        return this.mActivities.size();
    }

    @Override
    protected ActivityRecord getChildAt(int i) {
        return this.mActivities.get(i);
    }

    @Override
    protected ConfigurationContainer getParent() {
        return this.mStack;
    }

    @Override
    protected void onParentChanged() {
        super.onParentChanged();
        this.mService.mStackSupervisor.updateUIDsPresentOnDisplay();
    }

    private void closeRecentsChain() {
        if (this.mPrevAffiliate != null) {
            this.mPrevAffiliate.setNextAffiliate(this.mNextAffiliate);
        }
        if (this.mNextAffiliate != null) {
            this.mNextAffiliate.setPrevAffiliate(this.mPrevAffiliate);
        }
        setPrevAffiliate(null);
        setNextAffiliate(null);
    }

    void removedFromRecents() {
        closeRecentsChain();
        if (this.inRecents) {
            this.inRecents = false;
            this.mService.notifyTaskPersisterLocked(this, false);
        }
        clearRootProcess();
        this.mService.mWindowManager.notifyTaskRemovedFromRecents(this.taskId, this.userId);
    }

    void setTaskToAffiliateWith(TaskRecord taskRecord) {
        closeRecentsChain();
        this.mAffiliatedTaskId = taskRecord.mAffiliatedTaskId;
        this.mAffiliatedTaskColor = taskRecord.mAffiliatedTaskColor;
        while (true) {
            if (taskRecord.mNextAffiliate == null) {
                break;
            }
            TaskRecord taskRecord2 = taskRecord.mNextAffiliate;
            if (taskRecord2.mAffiliatedTaskId == this.mAffiliatedTaskId) {
                taskRecord = taskRecord2;
            } else {
                Slog.e(TAG, "setTaskToAffiliateWith: nextRecents=" + taskRecord2 + " affilTaskId=" + taskRecord2.mAffiliatedTaskId + " should be " + this.mAffiliatedTaskId);
                if (taskRecord2.mPrevAffiliate == taskRecord) {
                    taskRecord2.setPrevAffiliate(null);
                }
                taskRecord.setNextAffiliate(null);
            }
        }
        taskRecord.setNextAffiliate(this);
        setPrevAffiliate(taskRecord);
        setNextAffiliate(null);
    }

    Intent getBaseIntent() {
        return this.intent != null ? this.intent : this.affinityIntent;
    }

    ActivityRecord getRootActivity() {
        for (int i = 0; i < this.mActivities.size(); i++) {
            ActivityRecord activityRecord = this.mActivities.get(i);
            if (!activityRecord.finishing) {
                return activityRecord;
            }
        }
        return null;
    }

    ActivityRecord getTopActivity() {
        return getTopActivity(true);
    }

    ActivityRecord getTopActivity(boolean z) {
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = this.mActivities.get(size);
            if (!activityRecord.finishing && (z || !activityRecord.mTaskOverlay)) {
                return activityRecord;
            }
        }
        return null;
    }

    ActivityRecord topRunningActivityLocked() {
        if (this.mStack != null) {
            for (int size = this.mActivities.size() - 1; size >= 0; size--) {
                ActivityRecord activityRecord = this.mActivities.get(size);
                if (!activityRecord.finishing && activityRecord.okToShowLocked()) {
                    return activityRecord;
                }
            }
            return null;
        }
        return null;
    }

    boolean isVisible() {
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            if (this.mActivities.get(size).visible) {
                return true;
            }
        }
        return false;
    }

    void getAllRunningVisibleActivitiesLocked(ArrayList<ActivityRecord> arrayList) {
        if (this.mStack != null) {
            for (int size = this.mActivities.size() - 1; size >= 0; size--) {
                ActivityRecord activityRecord = this.mActivities.get(size);
                if (!activityRecord.finishing && activityRecord.okToShowLocked() && activityRecord.visibleIgnoringKeyguard) {
                    arrayList.add(activityRecord);
                }
            }
        }
    }

    ActivityRecord topRunningActivityWithStartingWindowLocked() {
        if (this.mStack != null) {
            for (int size = this.mActivities.size() - 1; size >= 0; size--) {
                ActivityRecord activityRecord = this.mActivities.get(size);
                if (activityRecord.mStartingWindowState == 1 && !activityRecord.finishing && activityRecord.okToShowLocked()) {
                    return activityRecord;
                }
            }
            return null;
        }
        return null;
    }

    void getNumRunningActivities(TaskActivitiesReport taskActivitiesReport) {
        taskActivitiesReport.reset();
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = this.mActivities.get(size);
            if (!activityRecord.finishing) {
                taskActivitiesReport.base = activityRecord;
                taskActivitiesReport.numActivities++;
                if (taskActivitiesReport.top == null || taskActivitiesReport.top.isState(ActivityStack.ActivityState.INITIALIZING)) {
                    taskActivitiesReport.top = activityRecord;
                    taskActivitiesReport.numRunning = 0;
                }
                if (activityRecord.app != null && activityRecord.app.thread != null) {
                    taskActivitiesReport.numRunning++;
                }
            }
        }
    }

    boolean okToShowLocked() {
        return this.mService.mStackSupervisor.isCurrentProfileLocked(this.userId) || topRunningActivityLocked() != null;
    }

    final void setFrontOfTask() {
        int size = this.mActivities.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            ActivityRecord activityRecord = this.mActivities.get(i);
            if (z || activityRecord.finishing) {
                activityRecord.frontOfTask = false;
            } else {
                activityRecord.frontOfTask = true;
                z = true;
            }
        }
        if (!z && size > 0) {
            this.mActivities.get(0).frontOfTask = true;
        }
    }

    final void moveActivityToFrontLocked(ActivityRecord activityRecord) {
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            Slog.i(TAG_ADD_REMOVE, "Removing and adding activity " + activityRecord + " to stack at top callers=" + Debug.getCallers(4));
        }
        this.mActivities.remove(activityRecord);
        this.mActivities.add(activityRecord);
        this.mWindowContainerController.positionChildAtTop(activityRecord.mWindowContainerController);
        updateEffectiveIntent();
        setFrontOfTask();
    }

    void addActivityAtBottom(ActivityRecord activityRecord) {
        addActivityAtIndex(0, activityRecord);
    }

    void addActivityToTop(ActivityRecord activityRecord) {
        addActivityAtIndex(this.mActivities.size(), activityRecord);
    }

    @Override
    public int getActivityType() {
        int activityType = super.getActivityType();
        if (activityType != 0 || this.mActivities.isEmpty()) {
            return activityType;
        }
        return this.mActivities.get(0).getActivityType();
    }

    void addActivityAtIndex(int i, ActivityRecord activityRecord) {
        TaskRecord task = activityRecord.getTask();
        if (task != null && task != this) {
            throw new IllegalArgumentException("Can not add r= to task=" + this + " current parent=" + task);
        }
        activityRecord.setTask(this);
        if (!this.mActivities.remove(activityRecord) && activityRecord.fullscreen) {
            this.numFullscreen++;
        }
        if (this.mActivities.isEmpty()) {
            if (activityRecord.getActivityType() == 0) {
                activityRecord.setActivityType(1);
            }
            setActivityType(activityRecord.getActivityType());
            this.isPersistable = activityRecord.isPersistable();
            this.mCallingUid = activityRecord.launchedFromUid;
            this.mCallingPackage = activityRecord.launchedFromPackage;
            this.maxRecents = Math.min(Math.max(activityRecord.info.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
        } else {
            activityRecord.setActivityType(getActivityType());
        }
        int size = this.mActivities.size();
        if (i == size && size > 0 && this.mActivities.get(size - 1).mTaskOverlay) {
            i--;
        }
        int iMin = Math.min(size, i);
        this.mActivities.add(iMin, activityRecord);
        updateEffectiveIntent();
        if (activityRecord.isPersistable()) {
            this.mService.notifyTaskPersisterLocked(this, false);
        }
        updateOverrideConfigurationFromLaunchBounds();
        AppWindowContainerController windowContainerController = activityRecord.getWindowContainerController();
        if (windowContainerController != null) {
            this.mWindowContainerController.positionChildAt(windowContainerController, iMin);
        }
        this.mService.mStackSupervisor.updateUIDsPresentOnDisplay();
    }

    boolean removeActivity(ActivityRecord activityRecord) {
        return removeActivity(activityRecord, false);
    }

    boolean removeActivity(ActivityRecord activityRecord, boolean z) {
        if (activityRecord.getTask() != this) {
            throw new IllegalArgumentException("Activity=" + activityRecord + " does not belong to task=" + this);
        }
        activityRecord.setTask(null, z);
        if (this.mActivities.remove(activityRecord) && activityRecord.fullscreen) {
            this.numFullscreen--;
        }
        if (activityRecord.isPersistable()) {
            this.mService.notifyTaskPersisterLocked(this, false);
        }
        if (inPinnedWindowingMode()) {
            this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
        }
        if (this.mActivities.isEmpty()) {
            return !this.mReuseTask;
        }
        updateEffectiveIntent();
        return false;
    }

    boolean onlyHasTaskOverlayActivities(boolean z) {
        int i = 0;
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = this.mActivities.get(size);
            if (!z || !activityRecord.finishing) {
                if (!activityRecord.mTaskOverlay) {
                    return false;
                }
                i++;
            }
        }
        return i > 0;
    }

    boolean autoRemoveFromRecents() {
        return this.autoRemoveRecents || (this.mActivities.isEmpty() && !this.hasBeenVisible);
    }

    final void performClearTaskAtIndexLocked(int i, boolean z, String str) {
        int size = this.mActivities.size();
        while (i < size) {
            ActivityRecord activityRecord = this.mActivities.get(i);
            if (!activityRecord.finishing) {
                if (this.mStack == null) {
                    activityRecord.takeFromHistory();
                    this.mActivities.remove(i);
                    i--;
                    size--;
                } else if (this.mStack.finishActivityLocked(activityRecord, 0, null, str, false, z)) {
                    i--;
                    size--;
                }
            }
            i++;
        }
    }

    void performClearTaskLocked() {
        this.mReuseTask = true;
        performClearTaskAtIndexLocked(0, false, "clear-task-all");
        this.mReuseTask = false;
    }

    ActivityRecord performClearTaskForReuseLocked(ActivityRecord activityRecord, int i) {
        this.mReuseTask = true;
        ActivityRecord activityRecordPerformClearTaskLocked = performClearTaskLocked(activityRecord, i);
        this.mReuseTask = false;
        return activityRecordPerformClearTaskLocked;
    }

    final ActivityRecord performClearTaskLocked(ActivityRecord activityRecord, int i) {
        int size = this.mActivities.size();
        int i2 = size - 1;
        while (i2 >= 0) {
            ActivityRecord activityRecord2 = this.mActivities.get(i2);
            if (activityRecord2.finishing || !activityRecord2.realActivity.equals(activityRecord.realActivity)) {
                i2--;
            } else {
                while (true) {
                    i2++;
                    if (i2 >= size) {
                        break;
                    }
                    ActivityRecord activityRecord3 = this.mActivities.get(i2);
                    if (!activityRecord3.finishing) {
                        ActivityOptions activityOptionsTakeOptionsLocked = activityRecord3.takeOptionsLocked();
                        if (activityOptionsTakeOptionsLocked != null) {
                            activityRecord2.updateOptionsLocked(activityOptionsTakeOptionsLocked);
                        }
                        if (this.mStack != null && this.mStack.finishActivityLocked(activityRecord3, 0, null, "clear-task-stack", false)) {
                            i2--;
                            size--;
                        }
                    }
                }
                if (activityRecord2.launchMode == 0 && (536870912 & i) == 0 && !ActivityStarter.isDocumentLaunchesIntoExisting(i) && !activityRecord2.finishing) {
                    if (this.mStack != null) {
                        this.mStack.finishActivityLocked(activityRecord2, 0, null, "clear-task-top", false);
                    }
                    return null;
                }
                return activityRecord2;
            }
        }
        return null;
    }

    void removeTaskActivitiesLocked(boolean z, String str) {
        performClearTaskAtIndexLocked(0, z, str);
    }

    String lockTaskAuthToString() {
        switch (this.mLockTaskAuth) {
            case 0:
                return "LOCK_TASK_AUTH_DONT_LOCK";
            case 1:
                return "LOCK_TASK_AUTH_PINNABLE";
            case 2:
                return "LOCK_TASK_AUTH_LAUNCHABLE";
            case 3:
                return "LOCK_TASK_AUTH_WHITELISTED";
            case 4:
                return "LOCK_TASK_AUTH_LAUNCHABLE_PRIV";
            default:
                return "unknown=" + this.mLockTaskAuth;
        }
    }

    void setLockTaskAuth() {
        setLockTaskAuth(getRootActivity());
    }

    private void setLockTaskAuth(ActivityRecord activityRecord) {
        int i = 1;
        if (activityRecord == null) {
            this.mLockTaskAuth = 1;
            return;
        }
        String packageName = this.realActivity != null ? this.realActivity.getPackageName() : null;
        LockTaskController lockTaskController = this.mService.getLockTaskController();
        switch (activityRecord.lockTaskLaunchMode) {
            case 0:
                if (lockTaskController.isPackageWhitelisted(this.userId, packageName)) {
                    i = 3;
                }
                this.mLockTaskAuth = i;
                break;
            case 1:
                this.mLockTaskAuth = 0;
                break;
            case 2:
                this.mLockTaskAuth = 4;
                break;
            case 3:
                if (lockTaskController.isPackageWhitelisted(this.userId, packageName)) {
                    i = 2;
                }
                this.mLockTaskAuth = i;
                break;
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.d(TAG_LOCKTASK, "setLockTaskAuth: task=" + this + " mLockTaskAuth=" + lockTaskAuthToString());
        }
    }

    private boolean isResizeable(boolean z) {
        return this.mService.mForceResizableActivities || ActivityInfo.isResizeableMode(this.mResizeMode) || (z && this.mSupportsPictureInPicture);
    }

    boolean isResizeable() {
        return isResizeable(true);
    }

    @Override
    public boolean supportsSplitScreenWindowingMode() {
        if (super.supportsSplitScreenWindowingMode() && this.mService.mSupportsSplitScreenMultiWindow) {
            return this.mService.mForceResizableActivities || (isResizeable(false) && !ActivityInfo.isPreserveOrientationMode(this.mResizeMode));
        }
        return false;
    }

    boolean canBeLaunchedOnDisplay(int i) {
        return this.mService.mStackSupervisor.canPlaceEntityOnDisplay(i, isResizeable(false), -1, -1, null);
    }

    private boolean canResizeToBounds(Rect rect) {
        if (rect == null || !inFreeformWindowingMode()) {
            return true;
        }
        boolean z = rect.width() > rect.height();
        Rect overrideBounds = getOverrideBounds();
        if (this.mResizeMode != 7) {
            return !(this.mResizeMode == 6 && z) && (this.mResizeMode != 5 || z);
        }
        if (overrideBounds.isEmpty()) {
            return true;
        }
        return z == (overrideBounds.width() > overrideBounds.height());
    }

    boolean isClearingToReuseTask() {
        return this.mReuseTask;
    }

    final ActivityRecord findActivityInHistoryLocked(ActivityRecord activityRecord) {
        ComponentName componentName = activityRecord.realActivity;
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord2 = this.mActivities.get(size);
            if (!activityRecord2.finishing && activityRecord2.realActivity.equals(componentName)) {
                return activityRecord2;
            }
        }
        return null;
    }

    void updateTaskDescription() {
        int size = this.mActivities.size();
        boolean z = true;
        boolean z2 = (size == 0 || (this.mActivities.get(0).info.flags & 4096) == 0) ? false : true;
        int iMin = Math.min(size, 1);
        while (true) {
            if (iMin >= size) {
                break;
            }
            ActivityRecord activityRecord = this.mActivities.get(iMin);
            if (z2 && (activityRecord.info.flags & 4096) == 0) {
                iMin++;
                break;
            } else if (activityRecord.intent != null && (activityRecord.intent.getFlags() & DumpState.DUMP_FROZEN) != 0) {
                break;
            } else {
                iMin++;
            }
        }
        if (iMin > 0) {
            int iconResource = -1;
            int primaryColor = 0;
            int backgroundColor = 0;
            int statusBarColor = 0;
            int navigationBarColor = 0;
            String label = null;
            String iconFilename = null;
            for (int i = iMin - 1; i >= 0; i--) {
                ActivityRecord activityRecord2 = this.mActivities.get(i);
                if (!activityRecord2.mTaskOverlay) {
                    if (activityRecord2.taskDescription != null) {
                        if (label == null) {
                            label = activityRecord2.taskDescription.getLabel();
                        }
                        if (iconResource == -1) {
                            iconResource = activityRecord2.taskDescription.getIconResource();
                        }
                        if (iconFilename == null) {
                            iconFilename = activityRecord2.taskDescription.getIconFilename();
                        }
                        if (primaryColor == 0) {
                            primaryColor = activityRecord2.taskDescription.getPrimaryColor();
                        }
                        if (z) {
                            backgroundColor = activityRecord2.taskDescription.getBackgroundColor();
                            statusBarColor = activityRecord2.taskDescription.getStatusBarColor();
                            navigationBarColor = activityRecord2.taskDescription.getNavigationBarColor();
                        }
                    }
                    z = false;
                }
            }
            this.lastTaskDescription = new ActivityManager.TaskDescription(label, null, iconResource, iconFilename, primaryColor, backgroundColor, statusBarColor, navigationBarColor);
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.setTaskDescription(this.lastTaskDescription);
            }
            if (this.taskId == this.mAffiliatedTaskId) {
                this.mAffiliatedTaskColor = this.lastTaskDescription.getPrimaryColor();
            }
        }
    }

    int findEffectiveRootIndex() {
        int size = this.mActivities.size() - 1;
        int i = 0;
        for (int i2 = 0; i2 <= size; i2++) {
            ActivityRecord activityRecord = this.mActivities.get(i2);
            if (!activityRecord.finishing) {
                if ((activityRecord.info.flags & 4096) == 0) {
                    return i2;
                }
                i = i2;
            }
        }
        return i;
    }

    void updateEffectiveIntent() {
        setIntent(this.mActivities.get(findEffectiveRootIndex()));
        updateTaskDescription();
    }

    private void adjustForMinimalTaskDimensions(Rect rect) {
        if (rect == null) {
            return;
        }
        int i = this.mMinWidth;
        int i2 = this.mMinHeight;
        if (!inPinnedWindowingMode()) {
            if (i == -1) {
                i = this.mService.mStackSupervisor.mDefaultMinSizeOfResizeableTask;
            }
            if (i2 == -1) {
                i2 = this.mService.mStackSupervisor.mDefaultMinSizeOfResizeableTask;
            }
        }
        boolean z = i > rect.width();
        boolean z2 = i2 > rect.height();
        if (!z && !z2) {
            return;
        }
        Rect overrideBounds = getOverrideBounds();
        if (z) {
            if (!overrideBounds.isEmpty() && rect.right == overrideBounds.right) {
                rect.left = rect.right - i;
            } else {
                rect.right = rect.left + i;
            }
        }
        if (z2) {
            if (!overrideBounds.isEmpty() && rect.bottom == overrideBounds.bottom) {
                rect.top = rect.bottom - i2;
            } else {
                rect.bottom = rect.top + i2;
            }
        }
    }

    Configuration computeNewOverrideConfigurationForBounds(Rect rect, Rect rect2) {
        Configuration configuration = new Configuration();
        if (rect != null) {
            configuration.setTo(getOverrideConfiguration());
            this.mTmpRect.set(rect);
            adjustForMinimalTaskDimensions(this.mTmpRect);
            computeOverrideConfiguration(configuration, this.mTmpRect, rect2, this.mTmpRect.right != rect.right, this.mTmpRect.bottom != rect.bottom);
        }
        return configuration;
    }

    boolean updateOverrideConfiguration(Rect rect) {
        return updateOverrideConfiguration(rect, null);
    }

    boolean updateOverrideConfiguration(Rect rect, Rect rect2) {
        if (equivalentOverrideBounds(rect)) {
            return false;
        }
        Rect overrideBounds = getOverrideBounds();
        this.mTmpConfig.setTo(getOverrideConfiguration());
        Configuration overrideConfiguration = getOverrideConfiguration();
        boolean z = rect == null || rect.isEmpty();
        boolean zPersistTaskBounds = getWindowConfiguration().persistTaskBounds();
        if (z) {
            if (!overrideBounds.isEmpty() && zPersistTaskBounds) {
                this.mLastNonFullscreenBounds = overrideBounds;
            }
            setBounds(null);
            overrideConfiguration.unset();
        } else {
            this.mTmpRect.set(rect);
            adjustForMinimalTaskDimensions(this.mTmpRect);
            setBounds(this.mTmpRect);
            if (this.mStack == null || zPersistTaskBounds) {
                this.mLastNonFullscreenBounds = getOverrideBounds();
            }
            computeOverrideConfiguration(overrideConfiguration, this.mTmpRect, rect2, this.mTmpRect.right != rect.right, this.mTmpRect.bottom != rect.bottom);
        }
        onOverrideConfigurationChanged(overrideConfiguration);
        return !this.mTmpConfig.equals(overrideConfiguration);
    }

    void onActivityStateChanged(ActivityRecord activityRecord, ActivityStack.ActivityState activityState, String str) {
        ActivityStack stack = getStack();
        if (stack != null) {
            stack.onActivityStateChanged(activityRecord, activityState, str);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        boolean zInMultiWindowMode = inMultiWindowMode();
        super.onConfigurationChanged(configuration);
        if (zInMultiWindowMode != inMultiWindowMode()) {
            this.mService.mStackSupervisor.scheduleUpdateMultiWindowMode(this);
        }
    }

    void computeOverrideConfiguration(Configuration configuration, Rect rect, Rect rect2, boolean z, boolean z2) {
        int i;
        this.mTmpNonDecorBounds.set(rect);
        this.mTmpStableBounds.set(rect);
        configuration.unset();
        Configuration configuration2 = getParent().getConfiguration();
        float f = configuration2.densityDpi * 0.00625f;
        if (this.mStack != null) {
            this.mStack.getWindowContainerController().adjustConfigurationForBounds(rect, rect2, this.mTmpNonDecorBounds, this.mTmpStableBounds, z, z2, f, configuration, configuration2, getWindowingMode());
            if (configuration.screenWidthDp <= configuration.screenHeightDp) {
                i = 1;
            } else {
                i = 2;
            }
            configuration.orientation = i;
            int iWidth = (int) (this.mTmpNonDecorBounds.width() / f);
            int iHeight = (int) (this.mTmpNonDecorBounds.height() / f);
            configuration.screenLayout = Configuration.reduceScreenLayout(configuration2.screenLayout & 63, Math.max(iHeight, iWidth), Math.min(iHeight, iWidth));
            return;
        }
        throw new IllegalArgumentException("Expected stack when calculating override config");
    }

    Rect updateOverrideConfigurationFromLaunchBounds() {
        Rect launchBounds = getLaunchBounds();
        updateOverrideConfiguration(launchBounds);
        if (launchBounds != null && !launchBounds.isEmpty()) {
            launchBounds.set(getOverrideBounds());
        }
        return launchBounds;
    }

    void updateOverrideConfigurationForStack(ActivityStack activityStack) {
        if (this.mStack != null && this.mStack == activityStack) {
            return;
        }
        if (activityStack.inFreeformWindowingMode()) {
            if (!isResizeable()) {
                throw new IllegalArgumentException("Can not position non-resizeable task=" + this + " in stack=" + activityStack);
            }
            if (!matchParentBounds()) {
                return;
            }
            if (this.mLastNonFullscreenBounds != null) {
                updateOverrideConfiguration(this.mLastNonFullscreenBounds);
                return;
            } else {
                this.mService.mStackSupervisor.getLaunchParamsController().layoutTask(this, null);
                return;
            }
        }
        updateOverrideConfiguration(activityStack.getOverrideBounds());
    }

    Rect getLaunchBounds() {
        if (this.mStack == null) {
            return null;
        }
        int windowingMode = getWindowingMode();
        if (!isActivityTypeStandardOrUndefined() || windowingMode == 1 || (windowingMode == 3 && !isResizeable())) {
            if (isResizeable()) {
                return this.mStack.getOverrideBounds();
            }
            return null;
        }
        if (!getWindowConfiguration().persistTaskBounds()) {
            return this.mStack.getOverrideBounds();
        }
        return this.mLastNonFullscreenBounds;
    }

    void addStartingWindowsForVisibleActivities(boolean z) {
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord = this.mActivities.get(size);
            if (activityRecord.visible) {
                activityRecord.showStartingWindow(null, false, z);
            }
        }
    }

    void setRootProcess(ProcessRecord processRecord) {
        clearRootProcess();
        if (this.intent != null && (this.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0) {
            this.mRootProcess = processRecord;
            processRecord.recentTasks.add(this);
        }
    }

    void clearRootProcess() {
        if (this.mRootProcess != null) {
            this.mRootProcess.recentTasks.remove(this);
            this.mRootProcess = null;
        }
    }

    void clearAllPendingOptions() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            getChildAt(childCount).clearOptionsLocked(false);
        }
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("userId=");
        printWriter.print(this.userId);
        printWriter.print(" effectiveUid=");
        UserHandle.formatUid(printWriter, this.effectiveUid);
        printWriter.print(" mCallingUid=");
        UserHandle.formatUid(printWriter, this.mCallingUid);
        printWriter.print(" mUserSetupComplete=");
        printWriter.print(this.mUserSetupComplete);
        printWriter.print(" mCallingPackage=");
        printWriter.println(this.mCallingPackage);
        if (this.affinity != null || this.rootAffinity != null) {
            printWriter.print(str);
            printWriter.print("affinity=");
            printWriter.print(this.affinity);
            if (this.affinity == null || !this.affinity.equals(this.rootAffinity)) {
                printWriter.print(" root=");
                printWriter.println(this.rootAffinity);
            } else {
                printWriter.println();
            }
        }
        if (this.voiceSession != null || this.voiceInteractor != null) {
            printWriter.print(str);
            printWriter.print("VOICE: session=0x");
            printWriter.print(Integer.toHexString(System.identityHashCode(this.voiceSession)));
            printWriter.print(" interactor=0x");
            printWriter.println(Integer.toHexString(System.identityHashCode(this.voiceInteractor)));
        }
        if (this.intent != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(str);
            sb.append("intent={");
            this.intent.toShortString(sb, false, true, false, true);
            sb.append('}');
            printWriter.println(sb.toString());
        }
        if (this.affinityIntent != null) {
            StringBuilder sb2 = new StringBuilder(128);
            sb2.append(str);
            sb2.append("affinityIntent={");
            this.affinityIntent.toShortString(sb2, false, true, false, true);
            sb2.append('}');
            printWriter.println(sb2.toString());
        }
        if (this.origActivity != null) {
            printWriter.print(str);
            printWriter.print("origActivity=");
            printWriter.println(this.origActivity.flattenToShortString());
        }
        if (this.realActivity != null) {
            printWriter.print(str);
            printWriter.print("realActivity=");
            printWriter.println(this.realActivity.flattenToShortString());
        }
        if (this.autoRemoveRecents || this.isPersistable || !isActivityTypeStandard() || this.numFullscreen != 0) {
            printWriter.print(str);
            printWriter.print("autoRemoveRecents=");
            printWriter.print(this.autoRemoveRecents);
            printWriter.print(" isPersistable=");
            printWriter.print(this.isPersistable);
            printWriter.print(" numFullscreen=");
            printWriter.print(this.numFullscreen);
            printWriter.print(" activityType=");
            printWriter.println(getActivityType());
        }
        if (this.rootWasReset || this.mNeverRelinquishIdentity || this.mReuseTask || this.mLockTaskAuth != 1) {
            printWriter.print(str);
            printWriter.print("rootWasReset=");
            printWriter.print(this.rootWasReset);
            printWriter.print(" mNeverRelinquishIdentity=");
            printWriter.print(this.mNeverRelinquishIdentity);
            printWriter.print(" mReuseTask=");
            printWriter.print(this.mReuseTask);
            printWriter.print(" mLockTaskAuth=");
            printWriter.println(lockTaskAuthToString());
        }
        if (this.mAffiliatedTaskId != this.taskId || this.mPrevAffiliateTaskId != -1 || this.mPrevAffiliate != null || this.mNextAffiliateTaskId != -1 || this.mNextAffiliate != null) {
            printWriter.print(str);
            printWriter.print("affiliation=");
            printWriter.print(this.mAffiliatedTaskId);
            printWriter.print(" prevAffiliation=");
            printWriter.print(this.mPrevAffiliateTaskId);
            printWriter.print(" (");
            if (this.mPrevAffiliate == null) {
                printWriter.print("null");
            } else {
                printWriter.print(Integer.toHexString(System.identityHashCode(this.mPrevAffiliate)));
            }
            printWriter.print(") nextAffiliation=");
            printWriter.print(this.mNextAffiliateTaskId);
            printWriter.print(" (");
            if (this.mNextAffiliate == null) {
                printWriter.print("null");
            } else {
                printWriter.print(Integer.toHexString(System.identityHashCode(this.mNextAffiliate)));
            }
            printWriter.println(")");
        }
        printWriter.print(str);
        printWriter.print("Activities=");
        printWriter.println(this.mActivities);
        if (!this.askedCompatMode || !this.inRecents || !this.isAvailable) {
            printWriter.print(str);
            printWriter.print("askedCompatMode=");
            printWriter.print(this.askedCompatMode);
            printWriter.print(" inRecents=");
            printWriter.print(this.inRecents);
            printWriter.print(" isAvailable=");
            printWriter.println(this.isAvailable);
        }
        if (this.lastDescription != null) {
            printWriter.print(str);
            printWriter.print("lastDescription=");
            printWriter.println(this.lastDescription);
        }
        if (this.mRootProcess != null) {
            printWriter.print(str);
            printWriter.print("mRootProcess=");
            printWriter.println(this.mRootProcess);
        }
        printWriter.print(str);
        printWriter.print("stackId=");
        printWriter.println(getStackId());
        printWriter.print(str + "hasBeenVisible=" + this.hasBeenVisible);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(" mResizeMode=");
        sb3.append(ActivityInfo.resizeModeToString(this.mResizeMode));
        printWriter.print(sb3.toString());
        printWriter.print(" mSupportsPictureInPicture=" + this.mSupportsPictureInPicture);
        printWriter.print(" isResizeable=" + isResizeable());
        printWriter.print(" lastActiveTime=" + this.lastActiveTime);
        printWriter.println(" (inactive for " + (getInactiveDuration() / 1000) + "s)");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (this.stringName != null) {
            sb.append(this.stringName);
            sb.append(" U=");
            sb.append(this.userId);
            sb.append(" StackId=");
            sb.append(getStackId());
            sb.append(" sz=");
            sb.append(this.mActivities.size());
            sb.append('}');
            return sb.toString();
        }
        sb.append("TaskRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        sb.append(this.taskId);
        if (this.affinity != null) {
            sb.append(" A=");
            sb.append(this.affinity);
        } else if (this.intent != null) {
            sb.append(" I=");
            sb.append(this.intent.getComponent().flattenToShortString());
        } else if (this.affinityIntent != null && this.affinityIntent.getComponent() != null) {
            sb.append(" aI=");
            sb.append(this.affinityIntent.getComponent().flattenToShortString());
        } else {
            sb.append(" ??");
        }
        this.stringName = sb.toString();
        return toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L, false);
        protoOutputStream.write(1120986464258L, this.taskId);
        for (int size = this.mActivities.size() - 1; size >= 0; size--) {
            this.mActivities.get(size).writeToProto(protoOutputStream, 2246267895811L);
        }
        protoOutputStream.write(1120986464260L, this.mStack.mStackId);
        if (this.mLastNonFullscreenBounds != null) {
            this.mLastNonFullscreenBounds.writeToProto(protoOutputStream, 1146756268037L);
        }
        if (this.realActivity != null) {
            protoOutputStream.write(1138166333446L, this.realActivity.flattenToShortString());
        }
        if (this.origActivity != null) {
            protoOutputStream.write(1138166333447L, this.origActivity.flattenToShortString());
        }
        protoOutputStream.write(1120986464264L, getActivityType());
        protoOutputStream.write(1120986464265L, this.mResizeMode);
        protoOutputStream.write(1133871366154L, matchParentBounds());
        if (!matchParentBounds()) {
            getOverrideBounds().writeToProto(protoOutputStream, 1146756268043L);
        }
        protoOutputStream.write(1120986464268L, this.mMinWidth);
        protoOutputStream.write(1120986464269L, this.mMinHeight);
        protoOutputStream.end(jStart);
    }

    static class TaskActivitiesReport {
        ActivityRecord base;
        int numActivities;
        int numRunning;
        ActivityRecord top;

        TaskActivitiesReport() {
        }

        void reset() {
            this.numActivities = 0;
            this.numRunning = 0;
            this.base = null;
            this.top = null;
        }
    }

    void saveToXml(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.i(TAG_RECENTS, "Saving task=" + this);
        }
        xmlSerializer.attribute(null, ATTR_TASKID, String.valueOf(this.taskId));
        if (this.realActivity != null) {
            xmlSerializer.attribute(null, ATTR_REALACTIVITY, this.realActivity.flattenToShortString());
        }
        xmlSerializer.attribute(null, ATTR_REALACTIVITY_SUSPENDED, String.valueOf(this.realActivitySuspended));
        if (this.origActivity != null) {
            xmlSerializer.attribute(null, ATTR_ORIGACTIVITY, this.origActivity.flattenToShortString());
        }
        if (this.affinity != null) {
            xmlSerializer.attribute(null, ATTR_AFFINITY, this.affinity);
            if (!this.affinity.equals(this.rootAffinity)) {
                xmlSerializer.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
            }
        } else if (this.rootAffinity != null) {
            xmlSerializer.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
        }
        xmlSerializer.attribute(null, ATTR_ROOTHASRESET, String.valueOf(this.rootWasReset));
        xmlSerializer.attribute(null, ATTR_AUTOREMOVERECENTS, String.valueOf(this.autoRemoveRecents));
        xmlSerializer.attribute(null, ATTR_ASKEDCOMPATMODE, String.valueOf(this.askedCompatMode));
        xmlSerializer.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        xmlSerializer.attribute(null, ATTR_USER_SETUP_COMPLETE, String.valueOf(this.mUserSetupComplete));
        xmlSerializer.attribute(null, ATTR_EFFECTIVE_UID, String.valueOf(this.effectiveUid));
        xmlSerializer.attribute(null, ATTR_LASTTIMEMOVED, String.valueOf(this.mLastTimeMoved));
        xmlSerializer.attribute(null, ATTR_NEVERRELINQUISH, String.valueOf(this.mNeverRelinquishIdentity));
        if (this.lastDescription != null) {
            xmlSerializer.attribute(null, ATTR_LASTDESCRIPTION, this.lastDescription.toString());
        }
        if (this.lastTaskDescription != null) {
            this.lastTaskDescription.saveToXml(xmlSerializer);
        }
        xmlSerializer.attribute(null, ATTR_TASK_AFFILIATION_COLOR, String.valueOf(this.mAffiliatedTaskColor));
        xmlSerializer.attribute(null, ATTR_TASK_AFFILIATION, String.valueOf(this.mAffiliatedTaskId));
        xmlSerializer.attribute(null, ATTR_PREV_AFFILIATION, String.valueOf(this.mPrevAffiliateTaskId));
        xmlSerializer.attribute(null, ATTR_NEXT_AFFILIATION, String.valueOf(this.mNextAffiliateTaskId));
        xmlSerializer.attribute(null, ATTR_CALLING_UID, String.valueOf(this.mCallingUid));
        xmlSerializer.attribute(null, ATTR_CALLING_PACKAGE, this.mCallingPackage == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mCallingPackage);
        xmlSerializer.attribute(null, ATTR_RESIZE_MODE, String.valueOf(this.mResizeMode));
        xmlSerializer.attribute(null, ATTR_SUPPORTS_PICTURE_IN_PICTURE, String.valueOf(this.mSupportsPictureInPicture));
        if (this.mLastNonFullscreenBounds != null) {
            xmlSerializer.attribute(null, ATTR_NON_FULLSCREEN_BOUNDS, this.mLastNonFullscreenBounds.flattenToString());
        }
        xmlSerializer.attribute(null, ATTR_MIN_WIDTH, String.valueOf(this.mMinWidth));
        xmlSerializer.attribute(null, ATTR_MIN_HEIGHT, String.valueOf(this.mMinHeight));
        xmlSerializer.attribute(null, ATTR_PERSIST_TASK_VERSION, String.valueOf(1));
        if (this.affinityIntent != null) {
            xmlSerializer.startTag(null, TAG_AFFINITYINTENT);
            this.affinityIntent.saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_AFFINITYINTENT);
        }
        if (this.intent != null) {
            xmlSerializer.startTag(null, TAG_INTENT);
            this.intent.saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_INTENT);
        }
        ArrayList<ActivityRecord> arrayList = this.mActivities;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ActivityRecord activityRecord = arrayList.get(i);
            if (activityRecord.info.persistableMode != 0 && activityRecord.isPersistable()) {
                if (((activityRecord.intent.getFlags() & DumpState.DUMP_FROZEN) | 8192) != 524288 || i <= 0) {
                    xmlSerializer.startTag(null, TAG_ACTIVITY);
                    activityRecord.saveToXml(xmlSerializer);
                    xmlSerializer.endTag(null, TAG_ACTIVITY);
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    @VisibleForTesting
    static TaskRecordFactory getTaskRecordFactory() {
        if (sTaskRecordFactory == null) {
            setTaskRecordFactory(new TaskRecordFactory());
        }
        return sTaskRecordFactory;
    }

    static void setTaskRecordFactory(TaskRecordFactory taskRecordFactory) {
        sTaskRecordFactory = taskRecordFactory;
    }

    static TaskRecord create(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
        return getTaskRecordFactory().create(activityManagerService, i, activityInfo, intent, iVoiceInteractionSession, iVoiceInteractor);
    }

    static TaskRecord create(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, ActivityManager.TaskDescription taskDescription) {
        return getTaskRecordFactory().create(activityManagerService, i, activityInfo, intent, taskDescription);
    }

    static TaskRecord restoreFromXml(XmlPullParser xmlPullParser, ActivityStackSupervisor activityStackSupervisor) throws XmlPullParserException, IOException {
        return getTaskRecordFactory().restoreFromXml(xmlPullParser, activityStackSupervisor);
    }

    static class TaskRecordFactory {
        TaskRecordFactory() {
        }

        TaskRecord create(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
            return new TaskRecord(activityManagerService, i, activityInfo, intent, iVoiceInteractionSession, iVoiceInteractor);
        }

        TaskRecord create(ActivityManagerService activityManagerService, int i, ActivityInfo activityInfo, Intent intent, ActivityManager.TaskDescription taskDescription) {
            return new TaskRecord(activityManagerService, i, activityInfo, intent, taskDescription);
        }

        TaskRecord create(ActivityManagerService activityManagerService, int i, Intent intent, Intent intent2, String str, String str2, ComponentName componentName, ComponentName componentName2, boolean z, boolean z2, boolean z3, int i2, int i3, String str3, ArrayList<ActivityRecord> arrayList, long j, boolean z4, ActivityManager.TaskDescription taskDescription, int i4, int i5, int i6, int i7, int i8, String str4, int i9, boolean z5, boolean z6, boolean z7, int i10, int i11) {
            return new TaskRecord(activityManagerService, i, intent, intent2, str, str2, componentName, componentName2, z, z2, z3, i2, i3, str3, arrayList, j, z4, taskDescription, i4, i5, i6, i7, i8, str4, i9, z5, z6, z7, i10, i11);
        }

        TaskRecord restoreFromXml(XmlPullParser xmlPullParser, ActivityStackSupervisor activityStackSupervisor) throws XmlPullParserException, IOException {
            int i;
            int i2;
            int i3;
            boolean z;
            int size;
            int i4;
            byte b;
            ActivityManager.TaskDescription taskDescription;
            ArrayList<ActivityRecord> arrayList = new ArrayList<>();
            int depth = xmlPullParser.getDepth();
            ActivityManager.TaskDescription taskDescription2 = new ActivityManager.TaskDescription();
            int attributeCount = xmlPullParser.getAttributeCount() - 1;
            String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            long j = 0;
            boolean z2 = true;
            boolean z3 = true;
            int i5 = -1;
            int i6 = 0;
            int i7 = 4;
            boolean z4 = false;
            String str2 = null;
            String str3 = null;
            int i8 = -1;
            int i9 = 0;
            ComponentName componentNameUnflattenFromString = null;
            ComponentName componentNameUnflattenFromString2 = null;
            boolean z5 = false;
            int i10 = -1;
            int i11 = -1;
            int i12 = -1;
            int i13 = 0;
            int i14 = -1;
            String str4 = null;
            boolean zBooleanValue = false;
            int i15 = -1;
            int i16 = -1;
            Rect rectUnflattenFromString = null;
            boolean z6 = false;
            boolean z7 = false;
            boolean z8 = false;
            int i17 = 0;
            while (attributeCount >= 0) {
                String attributeName = xmlPullParser.getAttributeName(attributeCount);
                String attributeValue = xmlPullParser.getAttributeValue(attributeCount);
                switch (attributeName.hashCode()) {
                    case -2134816935:
                        b = attributeName.equals(TaskRecord.ATTR_ASKEDCOMPATMODE) ? (byte) 8 : (byte) -1;
                        break;
                    case -1556983798:
                        if (attributeName.equals(TaskRecord.ATTR_LASTTIMEMOVED)) {
                            b = 14;
                            break;
                        }
                        break;
                    case -1537240555:
                        if (attributeName.equals(TaskRecord.ATTR_TASKID)) {
                            b = 0;
                            break;
                        }
                        break;
                    case -1494902876:
                        if (attributeName.equals(TaskRecord.ATTR_NEXT_AFFILIATION)) {
                            b = 18;
                            break;
                        }
                        break;
                    case -1292777190:
                        if (attributeName.equals(TaskRecord.ATTR_TASK_AFFILIATION_COLOR)) {
                            b = 19;
                            break;
                        }
                        break;
                    case -1138503444:
                        if (attributeName.equals(TaskRecord.ATTR_REALACTIVITY_SUSPENDED)) {
                            b = 2;
                            break;
                        }
                        break;
                    case -1124927690:
                        if (attributeName.equals(TaskRecord.ATTR_TASK_AFFILIATION)) {
                            b = UsbDescriptor.DESCRIPTORTYPE_CAPABILITY;
                            break;
                        }
                        break;
                    case -974080081:
                        if (attributeName.equals(TaskRecord.ATTR_USER_SETUP_COMPLETE)) {
                            b = 10;
                            break;
                        }
                        break;
                    case -929566280:
                        if (attributeName.equals(TaskRecord.ATTR_EFFECTIVE_UID)) {
                            b = 11;
                            break;
                        }
                        break;
                    case -865458610:
                        if (attributeName.equals(TaskRecord.ATTR_RESIZE_MODE)) {
                            b = 22;
                            break;
                        }
                        break;
                    case -826243148:
                        if (attributeName.equals(TaskRecord.ATTR_MIN_HEIGHT)) {
                            b = 26;
                            break;
                        }
                        break;
                    case -707249465:
                        if (attributeName.equals(TaskRecord.ATTR_NON_FULLSCREEN_BOUNDS)) {
                            b = 24;
                            break;
                        }
                        break;
                    case -705269939:
                        if (attributeName.equals(TaskRecord.ATTR_ORIGACTIVITY)) {
                            b = 3;
                            break;
                        }
                        break;
                    case -502399667:
                        if (attributeName.equals(TaskRecord.ATTR_AUTOREMOVERECENTS)) {
                            b = 7;
                            break;
                        }
                        break;
                    case -360792224:
                        if (attributeName.equals(TaskRecord.ATTR_SUPPORTS_PICTURE_IN_PICTURE)) {
                            b = 23;
                            break;
                        }
                        break;
                    case -162744347:
                        if (attributeName.equals(TaskRecord.ATTR_ROOT_AFFINITY)) {
                            b = 5;
                            break;
                        }
                        break;
                    case -147132913:
                        if (attributeName.equals(TaskRecord.ATTR_USERID)) {
                            b = 9;
                            break;
                        }
                        break;
                    case -132216235:
                        if (attributeName.equals(TaskRecord.ATTR_CALLING_UID)) {
                            b = 20;
                            break;
                        }
                        break;
                    case 180927924:
                        if (attributeName.equals(TaskRecord.ATTR_TASKTYPE)) {
                            b = 12;
                            break;
                        }
                        break;
                    case 331206372:
                        if (attributeName.equals(TaskRecord.ATTR_PREV_AFFILIATION)) {
                            b = 17;
                            break;
                        }
                        break;
                    case 541503897:
                        if (attributeName.equals(TaskRecord.ATTR_MIN_WIDTH)) {
                            b = 25;
                            break;
                        }
                        break;
                    case 605497640:
                        if (attributeName.equals(TaskRecord.ATTR_AFFINITY)) {
                            b = 4;
                            break;
                        }
                        break;
                    case 869221331:
                        if (attributeName.equals(TaskRecord.ATTR_LASTDESCRIPTION)) {
                            b = UsbACInterface.ACI_SAMPLE_RATE_CONVERTER;
                            break;
                        }
                        break;
                    case 1007873193:
                        if (attributeName.equals(TaskRecord.ATTR_PERSIST_TASK_VERSION)) {
                            b = 27;
                            break;
                        }
                        break;
                    case 1081438155:
                        if (attributeName.equals(TaskRecord.ATTR_CALLING_PACKAGE)) {
                            b = 21;
                            break;
                        }
                        break;
                    case 1457608782:
                        if (attributeName.equals(TaskRecord.ATTR_NEVERRELINQUISH)) {
                            b = UsbDescriptor.DESCRIPTORTYPE_BOS;
                            break;
                        }
                        break;
                    case 1539554448:
                        if (attributeName.equals(TaskRecord.ATTR_REALACTIVITY)) {
                            b = 1;
                            break;
                        }
                        break;
                    case 2023391309:
                        if (attributeName.equals(TaskRecord.ATTR_ROOTHASRESET)) {
                            b = 6;
                            break;
                        }
                        break;
                }
                switch (b) {
                    case 0:
                        if (i5 == -1) {
                            i5 = Integer.parseInt(attributeValue);
                        }
                        break;
                    case 1:
                        componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue);
                        break;
                    case 2:
                        zBooleanValue = Boolean.valueOf(attributeValue).booleanValue();
                        break;
                    case 3:
                        componentNameUnflattenFromString2 = ComponentName.unflattenFromString(attributeValue);
                        break;
                    case 4:
                        str2 = attributeValue;
                        break;
                    case 5:
                        str3 = attributeValue;
                        taskDescription = taskDescription2;
                        z4 = true;
                        continue;
                        attributeCount--;
                        taskDescription2 = taskDescription;
                        break;
                    case 6:
                        z6 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 7:
                        z7 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 8:
                        z8 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 9:
                        i17 = Integer.parseInt(attributeValue);
                        break;
                    case 10:
                        z3 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 11:
                        i8 = Integer.parseInt(attributeValue);
                        break;
                    case 12:
                        i6 = Integer.parseInt(attributeValue);
                        break;
                    case 13:
                        str4 = attributeValue;
                        break;
                    case 14:
                        j = Long.parseLong(attributeValue);
                        break;
                    case 15:
                        z2 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 16:
                        i10 = Integer.parseInt(attributeValue);
                        break;
                    case 17:
                        i11 = Integer.parseInt(attributeValue);
                        break;
                    case 18:
                        i12 = Integer.parseInt(attributeValue);
                        break;
                    case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                        i13 = Integer.parseInt(attributeValue);
                        break;
                    case 20:
                        i14 = Integer.parseInt(attributeValue);
                        break;
                    case BackupHandler.MSG_OP_COMPLETE:
                        str = attributeValue;
                        break;
                    case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                        i7 = Integer.parseInt(attributeValue);
                        break;
                    case WindowManagerService.H.BOOT_TIMEOUT:
                        z5 = Boolean.parseBoolean(attributeValue);
                        break;
                    case 24:
                        rectUnflattenFromString = Rect.unflattenFromString(attributeValue);
                        break;
                    case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                        i15 = Integer.parseInt(attributeValue);
                        break;
                    case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                        i16 = Integer.parseInt(attributeValue);
                        break;
                    case 27:
                        i9 = Integer.parseInt(attributeValue);
                        break;
                    default:
                        if (attributeName.startsWith("task_description_")) {
                            taskDescription2.restoreFromXml(attributeName, attributeValue);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            taskDescription = taskDescription2;
                            sb.append("TaskRecord: Unknown attribute=");
                            sb.append(attributeName);
                            Slog.w(TaskRecord.TAG, sb.toString());
                            attributeCount--;
                            taskDescription2 = taskDescription;
                        }
                        break;
                }
                taskDescription = taskDescription2;
                attributeCount--;
                taskDescription2 = taskDescription;
            }
            ActivityManager.TaskDescription taskDescription3 = taskDescription2;
            Intent intentRestoreFromXml = null;
            Intent intentRestoreFromXml2 = null;
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1 && (next != 3 || xmlPullParser.getDepth() >= depth)) {
                    if (next == 2) {
                        String name = xmlPullParser.getName();
                        if (TaskRecord.TAG_AFFINITYINTENT.equals(name)) {
                            intentRestoreFromXml2 = Intent.restoreFromXml(xmlPullParser);
                        } else if (TaskRecord.TAG_INTENT.equals(name)) {
                            intentRestoreFromXml = Intent.restoreFromXml(xmlPullParser);
                        } else if (TaskRecord.TAG_ACTIVITY.equals(name)) {
                            ActivityRecord activityRecordRestoreFromXml = ActivityRecord.restoreFromXml(xmlPullParser, activityStackSupervisor);
                            if (activityRecordRestoreFromXml != null) {
                                arrayList.add(activityRecordRestoreFromXml);
                            }
                        } else {
                            handleUnknownTag(name, xmlPullParser);
                        }
                    }
                }
            }
            String str5 = !z4 ? str2 : "@".equals(str3) ? null : str3;
            if (i8 <= 0) {
                Intent intent = intentRestoreFromXml != null ? intentRestoreFromXml : intentRestoreFromXml2;
                if (intent != null) {
                    try {
                        i = i17;
                        try {
                            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(intent.getComponent().getPackageName(), 8704, i);
                            i4 = applicationInfo != null ? applicationInfo.uid : 0;
                        } catch (RemoteException e) {
                            i4 = 0;
                        }
                    } catch (RemoteException e2) {
                        i = i17;
                    }
                    Slog.w(TaskRecord.TAG, "Updating task #" + i5 + " for " + intent + ": effectiveUid=" + i4);
                    i2 = i4;
                } else {
                    i = i17;
                }
                i4 = 0;
                Slog.w(TaskRecord.TAG, "Updating task #" + i5 + " for " + intent + ": effectiveUid=" + i4);
                i2 = i4;
            } else {
                i = i17;
                i2 = i8;
            }
            if (i9 < 1) {
                if (i6 == 1 && i7 == 2) {
                    i3 = 1;
                }
                z = z5;
                TaskRecord taskRecordCreate = create(activityStackSupervisor.mService, i5, intentRestoreFromXml, intentRestoreFromXml2, str2, str5, componentNameUnflattenFromString, componentNameUnflattenFromString2, z6, z7, z8, i, i2, str4, arrayList, j, z2, taskDescription3, i10, i11, i12, i13, i14, str, i3, z, zBooleanValue, z3, i15, i16);
                Rect rect = rectUnflattenFromString;
                taskRecordCreate.mLastNonFullscreenBounds = rect;
                taskRecordCreate.setBounds(rect);
                for (size = arrayList.size() - 1; size >= 0; size--) {
                    arrayList.get(size).setTask(taskRecordCreate);
                }
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TaskRecord.TAG_RECENTS, "Restored task=" + taskRecordCreate);
                }
                return taskRecordCreate;
            }
            if (i7 == 3) {
                i3 = 2;
                z = true;
                TaskRecord taskRecordCreate2 = create(activityStackSupervisor.mService, i5, intentRestoreFromXml, intentRestoreFromXml2, str2, str5, componentNameUnflattenFromString, componentNameUnflattenFromString2, z6, z7, z8, i, i2, str4, arrayList, j, z2, taskDescription3, i10, i11, i12, i13, i14, str, i3, z, zBooleanValue, z3, i15, i16);
                Rect rect2 = rectUnflattenFromString;
                taskRecordCreate2.mLastNonFullscreenBounds = rect2;
                taskRecordCreate2.setBounds(rect2);
                while (size >= 0) {
                }
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                }
                return taskRecordCreate2;
            }
            i3 = i7;
            z = z5;
            TaskRecord taskRecordCreate22 = create(activityStackSupervisor.mService, i5, intentRestoreFromXml, intentRestoreFromXml2, str2, str5, componentNameUnflattenFromString, componentNameUnflattenFromString2, z6, z7, z8, i, i2, str4, arrayList, j, z2, taskDescription3, i10, i11, i12, i13, i14, str, i3, z, zBooleanValue, z3, i15, i16);
            Rect rect22 = rectUnflattenFromString;
            taskRecordCreate22.mLastNonFullscreenBounds = rect22;
            taskRecordCreate22.setBounds(rect22);
            while (size >= 0) {
            }
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            }
            return taskRecordCreate22;
        }

        void handleUnknownTag(String str, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            Slog.e(TaskRecord.TAG, "restoreTask: Unexpected name=" + str);
            XmlUtils.skipCurrentTag(xmlPullParser);
        }
    }
}
