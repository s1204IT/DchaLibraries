package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityContainer;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.KeyguardManager;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.voice.IVoiceInteractionSession;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService;
import java.util.ArrayList;

class ActivityStarter {
    private boolean mAddingToTask;
    private boolean mAvoidMoveToFront;
    private int mCallingUid;
    private boolean mDoResume;
    private TaskRecord mInTask;
    private Intent mIntent;
    private ActivityStartInterceptor mInterceptor;
    private boolean mKeepCurTransition;
    private Rect mLaunchBounds;
    private int mLaunchFlags;
    private boolean mLaunchSingleInstance;
    private boolean mLaunchSingleTask;
    private boolean mLaunchSingleTop;
    private boolean mLaunchTaskBehind;
    private boolean mMovedOtherTask;
    private boolean mMovedToFront;
    private ActivityInfo mNewTaskInfo;
    private Intent mNewTaskIntent;
    private boolean mNoAnimation;
    private ActivityRecord mNotTop;
    private ActivityOptions mOptions;
    final ArrayList<ActivityStackSupervisor.PendingActivityLaunch> mPendingActivityLaunches = new ArrayList<>();
    private TaskRecord mReuseTask;
    private ActivityRecord mReusedActivity;
    private final ActivityManagerService mService;
    private ActivityRecord mSourceRecord;
    private ActivityStack mSourceStack;
    private ActivityRecord mStartActivity;
    private int mStartFlags;
    private final ActivityStackSupervisor mSupervisor;
    private ActivityStack mTargetStack;
    private IVoiceInteractor mVoiceInteractor;
    private IVoiceInteractionSession mVoiceSession;
    private WindowManagerService mWindowManager;
    private static final String TAG = "ActivityManager";
    private static final String TAG_RESULTS = TAG + ActivityManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_FOCUS = TAG + ActivityManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_USER_LEAVING = TAG + ActivityManagerDebugConfig.POSTFIX_USER_LEAVING;

    private void reset() {
        this.mStartActivity = null;
        this.mIntent = null;
        this.mCallingUid = -1;
        this.mOptions = null;
        this.mLaunchSingleTop = false;
        this.mLaunchSingleInstance = false;
        this.mLaunchSingleTask = false;
        this.mLaunchTaskBehind = false;
        this.mLaunchFlags = 0;
        this.mLaunchBounds = null;
        this.mNotTop = null;
        this.mDoResume = false;
        this.mStartFlags = 0;
        this.mSourceRecord = null;
        this.mInTask = null;
        this.mAddingToTask = false;
        this.mReuseTask = null;
        this.mNewTaskInfo = null;
        this.mNewTaskIntent = null;
        this.mSourceStack = null;
        this.mTargetStack = null;
        this.mMovedOtherTask = false;
        this.mMovedToFront = false;
        this.mNoAnimation = false;
        this.mKeepCurTransition = false;
        this.mAvoidMoveToFront = false;
        this.mVoiceSession = null;
        this.mVoiceInteractor = null;
    }

    ActivityStarter(ActivityManagerService service, ActivityStackSupervisor supervisor) {
        this.mService = service;
        this.mSupervisor = supervisor;
        this.mInterceptor = new ActivityStartInterceptor(this.mService, this.mSupervisor);
    }

    final int startActivityLocked(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, ActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, ActivityStackSupervisor.ActivityContainer container, TaskRecord inTask) {
        int err;
        int err2 = 0;
        ProcessRecord callerApp = null;
        if (caller != null) {
            callerApp = this.mService.getRecordForAppLocked(caller);
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                Slog.w(TAG, "Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting: " + intent.toString());
                err2 = -4;
            }
        }
        int userId = aInfo != null ? UserHandle.getUserId(aInfo.applicationInfo.uid) : 0;
        if (err2 == 0) {
            Slog.i(TAG, "START u" + userId + " {" + intent.toShortString(true, true, true, false) + "} from uid " + callingUid + " on display " + (container == null ? this.mSupervisor.mFocusedStack == null ? 0 : this.mSupervisor.mFocusedStack.mDisplayId : container.mActivityDisplay == null ? 0 : container.mActivityDisplay.mDisplayId));
        }
        ActivityRecord sourceRecord = null;
        ActivityRecord resultRecord = null;
        if (resultTo != null) {
            sourceRecord = this.mSupervisor.isInAnyStackLocked(resultTo);
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                Slog.v(TAG_RESULTS, "Will send result to " + resultTo + " " + sourceRecord);
            }
            if (sourceRecord != null && requestCode >= 0 && !sourceRecord.finishing) {
                resultRecord = sourceRecord;
            }
        }
        int launchFlags = intent.getFlags();
        if ((33554432 & launchFlags) != 0 && sourceRecord != null) {
            if (requestCode >= 0) {
                ActivityOptions.abort(options);
                return -3;
            }
            resultRecord = sourceRecord.resultTo;
            if (resultRecord != null && !resultRecord.isInStackLocked()) {
                resultRecord = null;
            }
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
        if (err2 == 0 && intent.getComponent() == null) {
            err2 = -1;
        }
        if (err2 == 0 && aInfo == null) {
            err2 = -2;
        }
        if (err2 == 0 && sourceRecord != null && sourceRecord.task.voiceSession != null && (268435456 & launchFlags) == 0 && sourceRecord.info.applicationInfo.uid != aInfo.applicationInfo.uid) {
            try {
                intent.addCategory("android.intent.category.VOICE");
                if (!AppGlobals.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                    Slog.w(TAG, "Activity being started in current voice task does not support voice: " + intent);
                    err2 = -7;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Failure checking voice capabilities", e);
                err2 = -7;
            }
        }
        if (err2 != 0 || voiceSession == null) {
            err = err2;
        } else {
            try {
                if (!AppGlobals.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                    Slog.w(TAG, "Activity being started in new voice task does not support: " + intent);
                    err2 = -7;
                }
                err = err2;
            } catch (RemoteException e2) {
                Slog.w(TAG, "Failure checking voice capabilities", e2);
                err = -7;
            }
        }
        ActivityStack activityStack = resultRecord == null ? null : resultRecord.task.stack;
        if (err != 0) {
            if (resultRecord != null) {
                activityStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            ActivityOptions.abort(options);
            return err;
        }
        boolean abort = (!this.mSupervisor.checkStartAnyActivityPermission(intent, aInfo, resultWho, requestCode, callingPid, callingUid, callingPackage, ignoreTargetSecurity, callerApp, resultRecord, activityStack, options)) | (!this.mService.mIntentFirewall.checkStartActivity(intent, callingUid, callingPid, resolvedType, aInfo.applicationInfo));
        if (this.mService.mController != null) {
            try {
                Intent watchIntent = intent.cloneFilter();
                abort |= !this.mService.mController.activityStarting(watchIntent, aInfo.applicationInfo.packageName);
            } catch (RemoteException e3) {
                this.mService.mController = null;
            }
        }
        this.mInterceptor.setStates(userId, realCallingPid, realCallingUid, startFlags, callingPackage);
        this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask, callingPid, callingUid, options);
        Intent intent2 = this.mInterceptor.mIntent;
        ResolveInfo rInfo2 = this.mInterceptor.mRInfo;
        ActivityInfo aInfo2 = this.mInterceptor.mAInfo;
        String resolvedType2 = this.mInterceptor.mResolvedType;
        TaskRecord inTask2 = this.mInterceptor.mInTask;
        int callingPid2 = this.mInterceptor.mCallingPid;
        int callingUid2 = this.mInterceptor.mCallingUid;
        ActivityOptions options2 = this.mInterceptor.mActivityOptions;
        if (abort) {
            if (resultRecord != null) {
                activityStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, 0, null);
            }
            ActivityOptions.abort(options2);
            return 0;
        }
        if (Build.isPermissionReviewRequired() && aInfo2 != null && this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(aInfo2.packageName, userId)) {
            IIntentSender target = this.mService.getIntentSenderLocked(2, callingPackage, callingUid2, userId, null, null, 0, new Intent[]{intent2}, new String[]{resolvedType2}, 1342177280, null);
            int flags = intent2.getFlags();
            Intent newIntent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            newIntent.setFlags(8388608 | flags);
            newIntent.putExtra("android.intent.extra.PACKAGE_NAME", aInfo2.packageName);
            newIntent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
            if (resultRecord != null) {
                newIntent.putExtra("android.intent.extra.RESULT_NEEDED", true);
            }
            intent2 = newIntent;
            resolvedType2 = null;
            callingUid2 = realCallingUid;
            callingPid2 = realCallingPid;
            rInfo2 = this.mSupervisor.resolveIntent(newIntent, null, userId);
            aInfo2 = this.mSupervisor.resolveActivity(newIntent, rInfo2, startFlags, null);
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW || !ActivityManagerService.IS_USER_BUILD) {
                Slog.i(TAG, "START u" + userId + " {" + newIntent.toShortString(true, true, true, false) + "} from uid " + realCallingUid + " on display " + (container == null ? this.mSupervisor.mFocusedStack == null ? 0 : this.mSupervisor.mFocusedStack.mDisplayId : container.mActivityDisplay == null ? 0 : container.mActivityDisplay.mDisplayId));
            }
        }
        if (rInfo2 != null && rInfo2.ephemeralResolveInfo != null) {
            IIntentSender failureTarget = this.mService.getIntentSenderLocked(2, callingPackage, Binder.getCallingUid(), userId, null, null, 0, new Intent[]{intent2}, new String[]{resolvedType2}, 1409286144, null);
            ephemeralIntent.setPackage(rInfo2.ephemeralResolveInfo.getPackageName());
            IIntentSender ephemeralTarget = this.mService.getIntentSenderLocked(2, callingPackage, Binder.getCallingUid(), userId, null, null, 0, new Intent[]{ephemeralIntent}, new String[]{resolvedType2}, 1409286144, null);
            int flags2 = intent2.getFlags();
            intent2 = new Intent();
            intent2.setFlags(268435456 | flags2 | 8388608);
            intent2.putExtra("android.intent.extra.PACKAGE_NAME", rInfo2.ephemeralResolveInfo.getPackageName());
            intent2.putExtra("android.intent.extra.EPHEMERAL_FAILURE", new IntentSender(failureTarget));
            intent2.putExtra("android.intent.extra.EPHEMERAL_SUCCESS", new IntentSender(ephemeralTarget));
            resolvedType2 = null;
            callingUid2 = realCallingUid;
            callingPid2 = realCallingPid;
            aInfo2 = this.mSupervisor.resolveActivity(intent2, rInfo2.ephemeralInstaller, startFlags, null);
        }
        ActivityRecord r = new ActivityRecord(this.mService, callerApp, callingUid2, callingPackage, intent2, resolvedType2, aInfo2, this.mService.mConfiguration, resultRecord, resultWho, requestCode, componentSpecified, voiceSession != null, this.mSupervisor, container, options2, sourceRecord);
        if (outActivity != null) {
            outActivity[0] = r;
        }
        if (r.appTimeTracker == null && sourceRecord != null) {
            r.appTimeTracker = sourceRecord.appTimeTracker;
        }
        ActivityStack stack = this.mSupervisor.mFocusedStack;
        if (voiceSession == null && ((stack.mResumedActivity == null || stack.mResumedActivity.info.applicationInfo.uid != callingUid2) && !this.mService.checkAppSwitchAllowedLocked(callingPid2, callingUid2, realCallingPid, realCallingUid, "Activity start"))) {
            ActivityStackSupervisor.PendingActivityLaunch pal = new ActivityStackSupervisor.PendingActivityLaunch(r, sourceRecord, startFlags, stack, callerApp);
            this.mPendingActivityLaunches.add(pal);
            ActivityOptions.abort(options2);
            return 4;
        }
        if (this.mService.mDidAppSwitch) {
            this.mService.mAppSwitchesAllowedTime = 0L;
        } else {
            this.mService.mDidAppSwitch = true;
        }
        doPendingActivityLaunchesLocked(false);
        try {
            this.mService.mWindowManager.deferSurfaceLayout();
            int err3 = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor, startFlags, true, options2, inTask2);
            this.mService.mWindowManager.continueSurfaceLayout();
            postStartActivityUncheckedProcessing(r, err3, stack.mStackId, this.mSourceRecord, this.mTargetStack);
            return err3;
        } catch (Throwable th) {
            this.mService.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    void postStartActivityUncheckedProcessing(ActivityRecord r, int result, int prevFocusedStackId, ActivityRecord sourceRecord, ActivityStack targetStack) {
        if (result < 0) {
            this.mSupervisor.notifyActivityDrawnForKeyguard();
            return;
        }
        if (result == 2 && !this.mSupervisor.mWaitingActivityLaunched.isEmpty()) {
            this.mSupervisor.reportTaskToFrontNoLaunch(this.mStartActivity);
        }
        int startedActivityStackId = -1;
        if (r.task != null && r.task.stack != null) {
            startedActivityStackId = r.task.stack.mStackId;
        } else if (this.mTargetStack != null) {
            startedActivityStackId = targetStack.mStackId;
        }
        boolean noDisplayActivityOverHome = sourceRecord != null && sourceRecord.noDisplay && sourceRecord.task.getTaskToReturnTo() == 1;
        if (startedActivityStackId == 3 && (prevFocusedStackId == 0 || noDisplayActivityOverHome)) {
            ActivityStack homeStack = this.mSupervisor.getStack(0);
            ActivityRecord topActivityHomeStack = homeStack != null ? homeStack.topRunningActivityLocked() : null;
            if (topActivityHomeStack == null || topActivityHomeStack.mActivityType != 2) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG, "Scheduling recents launch.");
                }
                this.mWindowManager.showRecentApps(true);
                return;
            }
        }
        if (startedActivityStackId != 4) {
            return;
        }
        if (result != 2 && result != 3) {
            return;
        }
        this.mService.notifyPinnedActivityRestartAttemptLocked();
    }

    void startHomeActivityLocked(Intent intent, ActivityInfo aInfo, String reason) {
        this.mSupervisor.moveHomeStackTaskToTop(1, reason);
        startActivityLocked(null, intent, null, null, aInfo, null, null, null, null, null, 0, 0, 0, null, 0, 0, 0, null, false, false, null, null, null);
        if (!this.mSupervisor.inResumeTopActivity) {
            return;
        }
        this.mSupervisor.scheduleResumeTopActivities();
    }

    void showConfirmDeviceCredential(int userId) {
        ActivityStack targetStack;
        ActivityRecord activityRecord;
        ActivityStack fullscreenStack = this.mSupervisor.getStack(1);
        ActivityStack freeformStack = this.mSupervisor.getStack(2);
        if (fullscreenStack != null && fullscreenStack.getStackVisibilityLocked(null) != 0) {
            targetStack = fullscreenStack;
        } else if (freeformStack != null && freeformStack.getStackVisibilityLocked(null) != 0) {
            targetStack = freeformStack;
        } else {
            targetStack = this.mSupervisor.getStack(0);
        }
        if (targetStack == null) {
            return;
        }
        KeyguardManager km = (KeyguardManager) this.mService.mContext.getSystemService("keyguard");
        Intent credential = km.createConfirmDeviceCredentialIntent(null, null, userId);
        if (credential == null || (activityRecord = targetStack.topRunningActivityLocked()) == null) {
            return;
        }
        IIntentSender target = this.mService.getIntentSenderLocked(2, activityRecord.launchedFromPackage, activityRecord.launchedFromUid, activityRecord.userId, null, null, 0, new Intent[]{activityRecord.intent}, new String[]{activityRecord.resolvedType}, 1409286144, null);
        credential.putExtra("android.intent.extra.INTENT", new IntentSender(target));
        startConfirmCredentialIntent(credential);
    }

    void startConfirmCredentialIntent(Intent intent) {
        intent.addFlags(276840448);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchTaskId(this.mSupervisor.getHomeActivity().task.taskId);
        this.mService.mContext.startActivityAsUser(intent, options.toBundle(), UserHandle.CURRENT);
    }

    final int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, IActivityManager.WaitResult outResult, Configuration config, Bundle bOptions, boolean ignoreTargetSecurity, int userId, IActivityContainer iContainer, TaskRecord inTask) {
        ResolveInfo rInfo;
        int callingPid;
        ActivityInfo aInfo;
        ResolveInfo rInfo2;
        Intent intent2;
        ProcessRecord heavy;
        UserInfo userInfo;
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        this.mSupervisor.mActivityMetricsLogger.notifyActivityLaunching();
        boolean componentSpecified = intent.getComponent() != null;
        Intent ephemeralIntent = new Intent(intent);
        Intent intent3 = new Intent(intent);
        ResolveInfo rInfo3 = this.mSupervisor.resolveIntent(intent3, resolvedType, userId);
        if (rInfo3 == null && (userInfo = this.mSupervisor.getUserInfo(userId)) != null && userInfo.isManagedProfile()) {
            UserManager userManager = UserManager.get(this.mService.mContext);
            long token = Binder.clearCallingIdentity();
            try {
                UserInfo parent = userManager.getProfileParent(userId);
                boolean profileLockedAndParentUnlockingOrUnlocked = (parent == null || !userManager.isUserUnlockingOrUnlocked(parent.id)) ? false : !userManager.isUserUnlockingOrUnlocked(userId);
                if (profileLockedAndParentUnlockingOrUnlocked) {
                    rInfo = this.mSupervisor.resolveIntent(intent3, resolvedType, userId, 786432);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            rInfo = rInfo3;
        }
        ActivityInfo aInfo2 = this.mSupervisor.resolveActivity(intent3, rInfo, startFlags, profilerInfo);
        ActivityOptions options = ActivityOptions.fromBundle(bOptions);
        ActivityStackSupervisor.ActivityContainer container = (ActivityStackSupervisor.ActivityContainer) iContainer;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (container != null && container.mParentActivity != null && container.mParentActivity.state != ActivityStack.ActivityState.RESUMED) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return -6;
                }
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
                ActivityStack stack = (container == null || container.mStack.isOnHomeDisplay()) ? this.mSupervisor.mFocusedStack : container.mStack;
                stack.mConfigWillChange = (config == null || this.mService.mConfiguration.diff(config) == 0) ? false : true;
                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                    Slog.v(TAG_CONFIGURATION, "Starting activity when config will change = " + stack.mConfigWillChange);
                }
                long origId = Binder.clearCallingIdentity();
                if (aInfo2 == null || (aInfo2.applicationInfo.privateFlags & 2) == 0 || !aInfo2.processName.equals(aInfo2.applicationInfo.packageName) || (heavy = this.mService.mHeavyWeightProcess) == null || (heavy.info.uid == aInfo2.applicationInfo.uid && heavy.processName.equals(aInfo2.processName))) {
                    aInfo = aInfo2;
                    rInfo2 = rInfo;
                    intent2 = intent3;
                } else {
                    int appCallingUid = callingUid;
                    if (caller != null) {
                        ProcessRecord callerApp = this.mService.getRecordForAppLocked(caller);
                        if (callerApp == null) {
                            Slog.w(TAG, "Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting: " + intent3.toString());
                            ActivityOptions.abort(options);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return -4;
                        }
                        appCallingUid = callerApp.info.uid;
                    }
                    IIntentSender target = this.mService.getIntentSenderLocked(2, "android", appCallingUid, userId, null, null, 0, new Intent[]{intent3}, new String[]{resolvedType}, 1342177280, null);
                    Intent newIntent = new Intent();
                    if (requestCode >= 0) {
                        newIntent.putExtra("has_result", true);
                    }
                    newIntent.putExtra("intent", new IntentSender(target));
                    if (heavy.activities.size() > 0) {
                        ActivityRecord hist = heavy.activities.get(0);
                        newIntent.putExtra("cur_app", hist.packageName);
                        newIntent.putExtra("cur_task", hist.task.taskId);
                    }
                    newIntent.putExtra("new_app", aInfo2.packageName);
                    newIntent.setFlags(intent3.getFlags());
                    newIntent.setClassName("android", HeavyWeightSwitcherActivity.class.getName());
                    intent2 = newIntent;
                    resolvedType = null;
                    caller = null;
                    try {
                        callingUid = Binder.getCallingUid();
                        callingPid = Binder.getCallingPid();
                        componentSpecified = true;
                        rInfo2 = this.mSupervisor.resolveIntent(newIntent, null, userId);
                        if (rInfo2 != null) {
                            try {
                                aInfo = rInfo2.activityInfo;
                            } catch (Throwable th) {
                                th = th;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            aInfo = null;
                        }
                        if (aInfo != null) {
                            try {
                                aInfo = this.mService.getActivityInfoForUser(aInfo, userId);
                            } catch (Throwable th2) {
                                th = th2;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                ActivityRecord[] outRecord = new ActivityRecord[1];
                int res = startActivityLocked(caller, intent2, ephemeralIntent, resolvedType, aInfo, rInfo2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified, outRecord, container, inTask);
                Binder.restoreCallingIdentity(origId);
                if (stack.mConfigWillChange) {
                    this.mService.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                    stack.mConfigWillChange = false;
                    if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        Slog.v(TAG_CONFIGURATION, "Updating to new configuration after starting activity.");
                    }
                    this.mService.updateConfigurationLocked(config, null, false);
                }
                if (outResult != null) {
                    outResult.result = res;
                    if (res == 0) {
                        this.mSupervisor.mWaitingActivityLaunched.add(outResult);
                        do {
                            try {
                                this.mService.wait();
                            } catch (InterruptedException e) {
                            }
                            if (outResult.result == 2 || outResult.timeout) {
                                break;
                            }
                        } while (outResult.who == null);
                        if (outResult.result == 2) {
                            res = 2;
                        }
                    }
                    if (res == 2) {
                        ActivityRecord r = stack.topRunningActivityLocked();
                        if (!r.nowVisible || r.state != ActivityStack.ActivityState.RESUMED) {
                            outResult.thisTime = SystemClock.uptimeMillis();
                            this.mSupervisor.mWaitingActivityVisible.add(outResult);
                            do {
                                try {
                                    this.mService.wait();
                                } catch (InterruptedException e2) {
                                }
                                if (outResult.timeout) {
                                    break;
                                }
                            } while (outResult.who == null);
                        } else {
                            outResult.timeout = false;
                            outResult.who = new ComponentName(r.info.packageName, r.info.name);
                            outResult.totalTime = 0L;
                            outResult.thisTime = 0L;
                        }
                    }
                }
                ActivityRecord launchedActivity = this.mReusedActivity != null ? this.mReusedActivity : outRecord[0];
                this.mSupervisor.mActivityMetricsLogger.notifyActivityLaunched(res, launchedActivity);
                ActivityManagerService.resetPriorityAfterLockedSection();
                return res;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    final int startActivities(IApplicationThread caller, int callingUid, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle bOptions, int userId) {
        int callingPid;
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
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityRecord[] outActivity = new ActivityRecord[1];
                    int i = 0;
                    while (i < intents.length) {
                        Intent intent = intents[i];
                        if (intent != null) {
                            if (intent != null && intent.hasFileDescriptors()) {
                                throw new IllegalArgumentException("File descriptors passed in Intent");
                            }
                            boolean componentSpecified = intent.getComponent() != null;
                            Intent intent2 = new Intent(intent);
                            ActivityInfo aInfo = this.mService.getActivityInfoForUser(this.mSupervisor.resolveActivity(intent2, resolvedTypes[i], 0, null, userId), userId);
                            if (aInfo != null && (aInfo.applicationInfo.privateFlags & 2) != 0) {
                                throw new IllegalArgumentException("FLAG_CANT_SAVE_STATE not supported here");
                            }
                            ActivityOptions options = ActivityOptions.fromBundle(i == intents.length + (-1) ? bOptions : null);
                            int res = startActivityLocked(caller, intent2, null, resolvedTypes[i], aInfo, null, null, null, resultTo, null, -1, callingPid, callingUid, callingPackage, callingPid, callingUid, 0, options, false, componentSpecified, outActivity, null, null);
                            if (res < 0) {
                                return res;
                            }
                            resultTo = outActivity[0] != null ? outActivity[0].appToken : null;
                        }
                        i++;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(origId);
                    return 0;
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private int startActivityUnchecked(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask) {
        boolean dontStart;
        boolean z;
        ActivityRecord top;
        setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor);
        computeLaunchingTaskFlags();
        computeSourceStack();
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "launchFlags(update): 0x" + Integer.toHexString(this.mLaunchFlags));
        }
        this.mIntent.setFlags(this.mLaunchFlags);
        this.mReusedActivity = getReusableIntentActivity();
        int preferredLaunchStackId = this.mOptions != null ? this.mOptions.getLaunchStackId() : -1;
        if (this.mReusedActivity != null) {
            if (this.mSupervisor.isLockTaskModeViolation(this.mReusedActivity.task, (this.mLaunchFlags & 268468224) == 268468224)) {
                this.mSupervisor.showLockTaskToast();
                Slog.e(TAG, "startActivityUnchecked: Attempt to violate Lock Task Mode");
                return 5;
            }
            if (this.mStartActivity.task == null) {
                this.mStartActivity.task = this.mReusedActivity.task;
            }
            if (this.mReusedActivity.task.intent == null) {
                this.mReusedActivity.task.setIntent(this.mStartActivity);
            }
            if (((this.mLaunchFlags & 67108864) != 0 || this.mLaunchSingleInstance || this.mLaunchSingleTask) && (top = this.mReusedActivity.task.performClearTaskForReuseLocked(this.mStartActivity, this.mLaunchFlags)) != null) {
                if (top.frontOfTask) {
                    top.task.setIntent(this.mStartActivity);
                }
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, top.task);
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + top.task);
                }
                top.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            }
            this.mReusedActivity = setTargetStackAndMoveToFrontIfNeeded(this.mReusedActivity);
            if ((this.mStartFlags & 1) != 0) {
                resumeTargetStackIfNeeded();
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "START_RETURN_INTENT_TO_CALLER");
                    return 1;
                }
                return 1;
            }
            setTaskFromIntentActivity(this.mReusedActivity);
            if (!this.mAddingToTask && this.mReuseTask == null) {
                resumeTargetStackIfNeeded();
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "START_TASK_TO_FRONT");
                    return 2;
                }
                return 2;
            }
        }
        if (this.mStartActivity.packageName == null) {
            if (this.mStartActivity.resultTo != null && this.mStartActivity.resultTo.task.stack != null) {
                this.mStartActivity.resultTo.task.stack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            }
            ActivityOptions.abort(this.mOptions);
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "START_CLASS_NOT_FOUND");
                return -2;
            }
            return -2;
        }
        ActivityStack topStack = this.mSupervisor.mFocusedStack;
        ActivityRecord top2 = topStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        if (top2 == null || this.mStartActivity.resultTo != null || !top2.realActivity.equals(this.mStartActivity.realActivity) || top2.userId != this.mStartActivity.userId || top2.app == null || top2.app.thread == null) {
            dontStart = false;
        } else {
            if ((this.mLaunchFlags & 536870912) != 0 || this.mLaunchSingleTop) {
                z = true;
            } else {
                z = this.mLaunchSingleTask;
            }
            dontStart = z;
        }
        if (dontStart) {
            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, top2, top2.task);
            topStack.mLastPausedActivity = null;
            if (this.mDoResume) {
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
            }
            ActivityOptions.abort(this.mOptions);
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + top2.task);
            }
            top2.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            this.mSupervisor.handleNonResizableTaskIfNeeded(top2.task, preferredLaunchStackId, topStack.mStackId);
            return 3;
        }
        boolean newTask = false;
        TaskRecord taskRecord = (!this.mLaunchTaskBehind || this.mSourceRecord == null) ? null : this.mSourceRecord.task;
        if (this.mStartActivity.resultTo == null && this.mInTask == null && !this.mAddingToTask && (this.mLaunchFlags & 268435456) != 0) {
            newTask = true;
            setTaskFromReuseOrCreateNewTask(taskRecord);
            if (this.mSupervisor.isLockTaskModeViolation(this.mStartActivity.task)) {
                this.mSupervisor.showLockTaskToast();
                Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
                return 5;
            }
            if (!this.mMovedOtherTask) {
                updateTaskReturnToType(this.mStartActivity.task, this.mLaunchFlags, topStack);
            }
        } else if (this.mSourceRecord != null) {
            if (this.mSupervisor.isLockTaskModeViolation(this.mSourceRecord.task)) {
                this.mSupervisor.showLockTaskToast();
                Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
                return 5;
            }
            int result = setTaskFromSourceRecord();
            if (result != 0) {
                return result;
            }
        } else if (this.mInTask != null) {
            if (this.mSupervisor.isLockTaskModeViolation(this.mInTask)) {
                this.mSupervisor.showLockTaskToast();
                Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
                return 5;
            }
            int result2 = setTaskFromInTask();
            if (result2 != 0) {
                return result2;
            }
        } else {
            setTaskToCurrentTopOrCreateNewTask();
        }
        this.mService.grantUriPermissionFromIntentLocked(this.mCallingUid, this.mStartActivity.packageName, this.mIntent, this.mStartActivity.getUriPermissionsLocked(), this.mStartActivity.userId);
        if (this.mSourceRecord != null && this.mSourceRecord.isRecentsActivity()) {
            this.mStartActivity.task.setTaskToReturnTo(2);
        }
        if (newTask) {
            EventLog.writeEvent(EventLogTags.AM_CREATE_TASK, Integer.valueOf(this.mStartActivity.userId), Integer.valueOf(this.mStartActivity.task.taskId));
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "ACT-AM_CREATE_TASK " + this.mStartActivity + " " + this.mStartActivity.task);
            }
        }
        ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, this.mStartActivity, this.mStartActivity.task);
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-AM_CREATE_ACTIVITY " + this.mStartActivity + " " + this.mStartActivity.task);
        }
        this.mTargetStack.mLastPausedActivity = null;
        this.mTargetStack.startActivityLocked(this.mStartActivity, newTask, this.mKeepCurTransition, this.mOptions);
        if (this.mDoResume) {
            if (!this.mLaunchTaskBehind) {
                this.mService.setFocusedActivityLocked(this.mStartActivity, "startedActivity");
            }
            ActivityRecord topTaskActivity = this.mStartActivity.task.topRunningActivityLocked();
            if (!this.mTargetStack.isFocusable() || (topTaskActivity != null && topTaskActivity.mTaskOverlay && this.mStartActivity != topTaskActivity)) {
                this.mTargetStack.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            } else {
                this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, this.mStartActivity, this.mOptions);
            }
        } else {
            this.mTargetStack.addRecentActivityLocked(this.mStartActivity);
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
        this.mSupervisor.handleNonResizableTaskIfNeeded(this.mStartActivity.task, preferredLaunchStackId, this.mTargetStack.mStackId);
        return 0;
    }

    private void setInitialState(ActivityRecord r, ActivityOptions options, TaskRecord inTask, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        reset();
        this.mStartActivity = r;
        this.mIntent = r.intent;
        this.mOptions = options;
        this.mCallingUid = r.launchedFromUid;
        this.mSourceRecord = sourceRecord;
        this.mVoiceSession = voiceSession;
        this.mVoiceInteractor = voiceInteractor;
        this.mLaunchBounds = getOverrideBounds(r, options, inTask);
        this.mLaunchSingleTop = r.launchMode == 1;
        this.mLaunchSingleInstance = r.launchMode == 3;
        this.mLaunchSingleTask = r.launchMode == 2;
        this.mLaunchFlags = adjustLaunchFlagsToDocumentMode(r, this.mLaunchSingleInstance, this.mLaunchSingleTask, this.mIntent.getFlags());
        boolean z = (!r.mLaunchTaskBehind || this.mLaunchSingleTask || this.mLaunchSingleInstance || (this.mLaunchFlags & PackageManagerService.DumpState.DUMP_FROZEN) == 0) ? false : true;
        this.mLaunchTaskBehind = z;
        sendNewTaskResultRequestIfNeeded();
        if ((this.mLaunchFlags & PackageManagerService.DumpState.DUMP_FROZEN) != 0 && r.resultTo == null) {
            this.mLaunchFlags |= 268435456;
        }
        if ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchTaskBehind || r.info.documentLaunchMode == 2)) {
            this.mLaunchFlags |= 134217728;
        }
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.d(TAG, "ACT-launchFlags(mLaunchFlags): 0x" + Integer.toHexString(this.mLaunchFlags) + ", launchMode:" + r.launchMode + ", startFlags: " + startFlags + ", doResume:" + doResume);
        }
        this.mSupervisor.mUserLeaving = (this.mLaunchFlags & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) == 0;
        if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
            Slog.v(TAG_USER_LEAVING, "startActivity() => mUserLeaving=" + this.mSupervisor.mUserLeaving);
        }
        this.mDoResume = doResume;
        if (!doResume || !this.mSupervisor.okToShowLocked(r)) {
            r.delayedResume = true;
            this.mDoResume = false;
        }
        if (this.mOptions != null && this.mOptions.getLaunchTaskId() != -1 && this.mOptions.getTaskOverlay()) {
            r.mTaskOverlay = true;
            TaskRecord task = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
            ActivityRecord top = task != null ? task.getTopActivity() : null;
            if (top != null && !top.visible) {
                this.mDoResume = false;
                this.mAvoidMoveToFront = true;
            }
        }
        this.mNotTop = (this.mLaunchFlags & 16777216) != 0 ? r : null;
        this.mInTask = inTask;
        if (inTask != null && !inTask.inRecents) {
            Slog.w(TAG, "Starting activity in task not in recents: " + inTask);
            this.mInTask = null;
        }
        this.mStartFlags = startFlags;
        if ((startFlags & 1) != 0) {
            ActivityRecord checkedCaller = sourceRecord;
            if (sourceRecord == null) {
                checkedCaller = this.mSupervisor.mFocusedStack.topRunningNonDelayedActivityLocked(this.mNotTop);
            }
            if (!checkedCaller.realActivity.equals(r.realActivity)) {
                this.mStartFlags &= -2;
            }
        }
        this.mNoAnimation = (this.mLaunchFlags & PackageManagerService.DumpState.DUMP_INSTALLS) != 0;
    }

    private void sendNewTaskResultRequestIfNeeded() {
        if (this.mStartActivity.resultTo == null || (this.mLaunchFlags & 268435456) == 0 || this.mStartActivity.resultTo.task.stack == null) {
            return;
        }
        Slog.w(TAG, "Activity is launching as a new task, so cancelling activity result.");
        this.mStartActivity.resultTo.task.stack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
        this.mStartActivity.resultTo = null;
    }

    private void computeLaunchingTaskFlags() {
        if (this.mSourceRecord != null || this.mInTask == null || this.mInTask.stack == null) {
            this.mInTask = null;
            if ((this.mStartActivity.isResolverActivity() || this.mStartActivity.noDisplay) && this.mSourceRecord != null && this.mSourceRecord.isFreeform()) {
                this.mAddingToTask = true;
            }
        } else {
            Intent baseIntent = this.mInTask.getBaseIntent();
            ActivityRecord root = this.mInTask.getRootActivity();
            if (baseIntent == null) {
                ActivityOptions.abort(this.mOptions);
                throw new IllegalArgumentException("Launching into task without base intent: " + this.mInTask);
            }
            if (this.mLaunchSingleInstance || this.mLaunchSingleTask) {
                if (!baseIntent.getComponent().equals(this.mStartActivity.intent.getComponent())) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Trying to launch singleInstance/Task " + this.mStartActivity + " into different task " + this.mInTask);
                }
                if (root != null) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Caller with mInTask " + this.mInTask + " has root " + root + " but target is singleInstance/Task");
                }
            }
            if (root == null) {
                this.mLaunchFlags = (this.mLaunchFlags & (-403185665)) | (baseIntent.getFlags() & 403185664);
                this.mIntent.setFlags(this.mLaunchFlags);
                this.mInTask.setIntent(this.mStartActivity);
                this.mAddingToTask = true;
            } else if ((this.mLaunchFlags & 268435456) != 0) {
                this.mAddingToTask = false;
            } else {
                this.mAddingToTask = true;
            }
            this.mReuseTask = this.mInTask;
        }
        if (this.mInTask == null) {
            if (this.mSourceRecord == null) {
                if ((this.mLaunchFlags & 268435456) == 0 && this.mInTask == null) {
                    Slog.w(TAG, "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
                    this.mLaunchFlags |= 268435456;
                    return;
                }
                return;
            }
            if (this.mSourceRecord.launchMode == 3) {
                this.mLaunchFlags |= 268435456;
            } else if (this.mLaunchSingleInstance || this.mLaunchSingleTask) {
                this.mLaunchFlags |= 268435456;
            }
        }
    }

    private void computeSourceStack() {
        if (this.mSourceRecord == null) {
            this.mSourceStack = null;
            return;
        }
        if (!this.mSourceRecord.finishing) {
            this.mSourceStack = this.mSourceRecord.task.stack;
            return;
        }
        if ((this.mLaunchFlags & 268435456) == 0) {
            Slog.w(TAG, "startActivity called from finishing " + this.mSourceRecord + "; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
            this.mLaunchFlags |= 268435456;
            this.mNewTaskInfo = this.mSourceRecord.info;
            this.mNewTaskIntent = this.mSourceRecord.task.intent;
        }
        this.mSourceRecord = null;
        this.mSourceStack = null;
    }

    private ActivityRecord getReusableIntentActivity() {
        boolean z;
        if (((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchFlags & 134217728) == 0) || this.mLaunchSingleInstance) {
            z = true;
        } else {
            z = this.mLaunchSingleTask;
        }
        boolean putIntoExistingTask = z & (this.mInTask == null && this.mStartActivity.resultTo == null);
        if (this.mOptions != null && this.mOptions.getLaunchTaskId() != -1) {
            TaskRecord task = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
            if (task == null) {
                return null;
            }
            ActivityRecord intentActivity = task.getTopActivity();
            return intentActivity;
        }
        if (!putIntoExistingTask) {
            return null;
        }
        if (this.mLaunchSingleInstance) {
            ActivityRecord intentActivity2 = this.mSupervisor.findActivityLocked(this.mIntent, this.mStartActivity.info, false);
            return intentActivity2;
        }
        if ((this.mLaunchFlags & 4096) != 0) {
            ActivityRecord intentActivity3 = this.mSupervisor.findActivityLocked(this.mIntent, this.mStartActivity.info, this.mLaunchSingleTask ? false : true);
            return intentActivity3;
        }
        ActivityRecord intentActivity4 = this.mSupervisor.findTaskLocked(this.mStartActivity);
        return intentActivity4;
    }

    private ActivityRecord setTargetStackAndMoveToFrontIfNeeded(ActivityRecord intentActivity) {
        this.mTargetStack = intentActivity.task.stack;
        this.mTargetStack.mLastPausedActivity = null;
        ActivityStack focusStack = this.mSupervisor.getFocusedStack();
        ActivityRecord curTop = focusStack == null ? null : focusStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        if (curTop != null && ((curTop.task != intentActivity.task || curTop.task != focusStack.topTask()) && !this.mAvoidMoveToFront)) {
            this.mStartActivity.intent.addFlags(4194304);
            if (this.mSourceRecord == null || (this.mSourceStack.topActivity() != null && this.mSourceStack.topActivity().task == this.mSourceRecord.task)) {
                if (this.mLaunchTaskBehind && this.mSourceRecord != null) {
                    intentActivity.setTaskToAffiliateWith(this.mSourceRecord.task);
                }
                this.mMovedOtherTask = true;
                boolean willClearTask = (this.mLaunchFlags & 268468224) == 268468224;
                if (!willClearTask) {
                    ActivityStack launchStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.task, this.mOptions);
                    if (launchStack == null || launchStack == this.mTargetStack) {
                        this.mTargetStack.moveTaskToFrontLocked(intentActivity.task, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringingFoundTaskToFront");
                        this.mMovedToFront = true;
                    } else if (launchStack.mStackId == 3 || launchStack.mStackId == 1) {
                        if ((this.mLaunchFlags & 4096) != 0) {
                            this.mSupervisor.moveTaskToStackLocked(intentActivity.task.taskId, launchStack.mStackId, true, true, "launchToSide", true);
                        } else {
                            this.mTargetStack.moveTaskToFrontLocked(intentActivity.task, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringToFrontInsteadOfAdjacentLaunch");
                        }
                        this.mMovedToFront = true;
                    }
                    this.mOptions = null;
                }
                updateTaskReturnToType(intentActivity.task, this.mLaunchFlags, focusStack);
            }
        }
        if (!this.mMovedToFront && this.mDoResume) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(ActivityStackSupervisor.TAG_TASKS, "Bring to front target: " + this.mTargetStack + " from " + intentActivity);
            }
            this.mTargetStack.moveToFront("intentActivityFound");
        }
        this.mSupervisor.handleNonResizableTaskIfNeeded(intentActivity.task, -1, this.mTargetStack.mStackId);
        if ((this.mLaunchFlags & 2097152) != 0) {
            return this.mTargetStack.resetTaskIfNeededLocked(intentActivity, this.mStartActivity);
        }
        return intentActivity;
    }

    private void updateTaskReturnToType(TaskRecord task, int launchFlags, ActivityStack focusedStack) {
        if ((launchFlags & 268451840) == 268451840) {
            task.setTaskToReturnTo(1);
        } else if (focusedStack == null || focusedStack.mStackId == 0) {
            task.setTaskToReturnTo(1);
        } else {
            task.setTaskToReturnTo(0);
        }
    }

    private void setTaskFromIntentActivity(ActivityRecord intentActivity) {
        if ((this.mLaunchFlags & 268468224) == 268468224) {
            this.mReuseTask = intentActivity.task;
            this.mReuseTask.performClearTaskLocked();
            this.mReuseTask.setIntent(this.mStartActivity);
            this.mMovedOtherTask = true;
            return;
        }
        if ((this.mLaunchFlags & 67108864) != 0 || this.mLaunchSingleInstance || this.mLaunchSingleTask) {
            ActivityRecord top = intentActivity.task.performClearTaskLocked(this.mStartActivity, this.mLaunchFlags);
            if (top != null) {
                return;
            }
            this.mAddingToTask = true;
            this.mSourceRecord = intentActivity;
            TaskRecord task = this.mSourceRecord.task;
            if (task != null && task.stack == null) {
                this.mTargetStack = computeStackFocus(this.mSourceRecord, false, null, this.mLaunchFlags, this.mOptions);
                this.mTargetStack.addTask(task, this.mLaunchTaskBehind ? false : true, "startActivityUnchecked");
            }
            if (ActivityManagerService.IS_USER_BUILD && !ActivityManagerDebugConfig.DEBUG_TASKS) {
                return;
            }
            Slog.d(TAG, "special case: the activity is not currently running");
            return;
        }
        if (this.mStartActivity.realActivity.equals(intentActivity.task.realActivity)) {
            if (((this.mLaunchFlags & 536870912) != 0 || this.mLaunchSingleTop) && intentActivity.realActivity.equals(this.mStartActivity.realActivity)) {
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, intentActivity.task);
                if (intentActivity.frontOfTask) {
                    intentActivity.task.setIntent(this.mStartActivity);
                }
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + intentActivity.task);
                }
                intentActivity.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
                return;
            }
            if (intentActivity.task.isSameIntentFilter(this.mStartActivity)) {
                return;
            }
            this.mAddingToTask = true;
            this.mSourceRecord = intentActivity;
            if (ActivityManagerService.IS_USER_BUILD && !ActivityManagerDebugConfig.DEBUG_TASKS) {
                return;
            }
            Slog.d(TAG, "since different intents, start new activity...");
            return;
        }
        if ((this.mLaunchFlags & 2097152) == 0) {
            this.mAddingToTask = true;
            this.mSourceRecord = intentActivity;
            if (ActivityManagerService.IS_USER_BUILD && !ActivityManagerDebugConfig.DEBUG_TASKS) {
                return;
            }
            Slog.d(TAG, "place the new activity on top of the current task...");
            return;
        }
        if (intentActivity.task.rootWasReset) {
            return;
        }
        intentActivity.task.setIntent(this.mStartActivity);
    }

    private void resumeTargetStackIfNeeded() {
        if (this.mDoResume) {
            this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, null, this.mOptions);
            if (!this.mMovedToFront) {
                this.mSupervisor.notifyActivityDrawnForKeyguard();
            }
        } else {
            ActivityOptions.abort(this.mOptions);
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
    }

    private void setTaskFromReuseOrCreateNewTask(TaskRecord taskToAffiliate) {
        this.mTargetStack = computeStackFocus(this.mStartActivity, true, this.mLaunchBounds, this.mLaunchFlags, this.mOptions);
        if (this.mReuseTask != null) {
            this.mStartActivity.setTask(this.mReuseTask, taskToAffiliate);
            return;
        }
        TaskRecord task = this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mNewTaskInfo != null ? this.mNewTaskInfo : this.mStartActivity.info, this.mNewTaskIntent != null ? this.mNewTaskIntent : this.mIntent, this.mVoiceSession, this.mVoiceInteractor, !this.mLaunchTaskBehind);
        this.mStartActivity.setTask(task, taskToAffiliate);
        if (this.mLaunchBounds != null) {
            int stackId = this.mTargetStack.mStackId;
            if (ActivityManager.StackId.resizeStackWithLaunchBounds(stackId)) {
                this.mService.resizeStack(stackId, this.mLaunchBounds, true, false, true, -1);
            } else {
                this.mStartActivity.task.updateOverrideConfiguration(this.mLaunchBounds);
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in new task " + this.mStartActivity.task);
        }
    }

    private int setTaskFromSourceRecord() {
        ActivityRecord top;
        TaskRecord sourceTask = this.mSourceRecord.task;
        boolean moveStackAllowed = sourceTask.stack.topTask() != sourceTask;
        if (moveStackAllowed) {
            this.mTargetStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.task, this.mOptions);
        }
        if (this.mTargetStack == null) {
            this.mTargetStack = sourceTask.stack;
        } else if (this.mTargetStack != sourceTask.stack) {
            this.mSupervisor.moveTaskToStackLocked(sourceTask.taskId, this.mTargetStack.mStackId, true, true, "launchToSide", false);
        }
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("sourceStackToFront");
        }
        TaskRecord topTask = this.mTargetStack.topTask();
        if (topTask != sourceTask && !this.mAvoidMoveToFront) {
            this.mTargetStack.moveTaskToFrontLocked(sourceTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "sourceTaskToFront");
        }
        if (!this.mAddingToTask && (this.mLaunchFlags & 67108864) != 0) {
            ActivityRecord top2 = sourceTask.performClearTaskLocked(this.mStartActivity, this.mLaunchFlags);
            this.mKeepCurTransition = true;
            if (top2 != null) {
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, top2.task);
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + top2.task);
                }
                top2.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
                this.mTargetStack.mLastPausedActivity = null;
                if (this.mDoResume) {
                    this.mSupervisor.resumeFocusedStackTopActivityLocked();
                }
                ActivityOptions.abort(this.mOptions);
                return 3;
            }
        } else if (!this.mAddingToTask && (this.mLaunchFlags & PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0 && (top = sourceTask.findActivityInHistoryLocked(this.mStartActivity)) != null) {
            TaskRecord task = top.task;
            task.moveActivityToFrontLocked(top);
            top.updateOptionsLocked(this.mOptions);
            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, task);
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + top.task);
            }
            top.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            this.mTargetStack.mLastPausedActivity = null;
            if (!this.mDoResume) {
                return 3;
            }
            this.mSupervisor.resumeFocusedStackTopActivityLocked();
            return 3;
        }
        this.mStartActivity.setTask(sourceTask, null);
        if (!ActivityManagerDebugConfig.DEBUG_TASKS) {
            return 0;
        }
        Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in existing task " + this.mStartActivity.task + " from source " + this.mSourceRecord);
        return 0;
    }

    private int setTaskFromInTask() {
        if (this.mLaunchBounds != null) {
            this.mInTask.updateOverrideConfiguration(this.mLaunchBounds);
            int stackId = this.mInTask.getLaunchStackId();
            if (stackId != this.mInTask.stack.mStackId) {
                ActivityStack stack = this.mSupervisor.moveTaskToStackUncheckedLocked(this.mInTask, stackId, true, false, "inTaskToFront");
                stackId = stack.mStackId;
            }
            if (ActivityManager.StackId.resizeStackWithLaunchBounds(stackId)) {
                this.mService.resizeStack(stackId, this.mLaunchBounds, true, false, true, -1);
            }
        }
        this.mTargetStack = this.mInTask.stack;
        this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
        ActivityRecord top = this.mInTask.getTopActivity();
        if (top != null && top.realActivity.equals(this.mStartActivity.realActivity) && top.userId == this.mStartActivity.userId && ((this.mLaunchFlags & 536870912) != 0 || this.mLaunchSingleTop || this.mLaunchSingleTask)) {
            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, top, top.task);
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(TAG, "ACT-AM_NEW_INTENT " + this.mStartActivity + " " + top.task);
            }
            top.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            return 3;
        }
        if (!this.mAddingToTask) {
            ActivityOptions.abort(this.mOptions);
            return 2;
        }
        this.mStartActivity.setTask(this.mInTask, null);
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in explicit task " + this.mStartActivity.task);
        }
        return 0;
    }

    private void setTaskToCurrentTopOrCreateNewTask() {
        this.mTargetStack = computeStackFocus(this.mStartActivity, false, null, this.mLaunchFlags, this.mOptions);
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("addingToTopTask");
        }
        ActivityRecord prev = this.mTargetStack.topActivity();
        TaskRecord task = prev != null ? prev.task : this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mStartActivity.info, this.mIntent, null, null, true);
        this.mStartActivity.setTask(task, null);
        this.mWindowManager.moveTaskToTop(this.mStartActivity.task.taskId);
        if (!ActivityManagerDebugConfig.DEBUG_TASKS) {
            return;
        }
        Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in new guessed " + this.mStartActivity.task);
    }

    private int adjustLaunchFlagsToDocumentMode(ActivityRecord r, boolean launchSingleInstance, boolean launchSingleTask, int launchFlags) {
        if ((launchFlags & PackageManagerService.DumpState.DUMP_FROZEN) != 0 && (launchSingleInstance || launchSingleTask)) {
            Slog.i(TAG, "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
            return launchFlags & (-134742017);
        }
        switch (r.info.documentLaunchMode) {
            case 0:
            default:
                return launchFlags;
            case 1:
                return launchFlags | PackageManagerService.DumpState.DUMP_FROZEN;
            case 2:
                return launchFlags | PackageManagerService.DumpState.DUMP_FROZEN;
            case 3:
                return launchFlags & (-134217729);
        }
    }

    final void doPendingActivityLaunchesLocked(boolean doResume) {
        while (!this.mPendingActivityLaunches.isEmpty()) {
            ActivityStackSupervisor.PendingActivityLaunch pal = this.mPendingActivityLaunches.remove(0);
            try {
                int result = startActivityUnchecked(pal.r, pal.sourceRecord, null, null, pal.startFlags, doResume ? this.mPendingActivityLaunches.isEmpty() : false, null, null);
                postStartActivityUncheckedProcessing(pal.r, result, this.mSupervisor.mFocusedStack.mStackId, this.mSourceRecord, this.mTargetStack);
            } catch (Exception e) {
                Slog.e(TAG, "Exception during pending activity launch pal=" + pal, e);
                pal.sendErrorResult(e.getMessage());
            }
        }
    }

    private ActivityStack computeStackFocus(ActivityRecord r, boolean newTask, Rect bounds, int launchFlags, ActivityOptions aOptions) {
        boolean zIsApplicationTask;
        boolean canUseFocusedStack;
        int stackId;
        TaskRecord task = r.task;
        if (r.isApplicationActivity()) {
            zIsApplicationTask = true;
        } else {
            zIsApplicationTask = task != null ? task.isApplicationTask() : false;
        }
        if (!zIsApplicationTask) {
            return this.mSupervisor.mHomeStack;
        }
        ActivityStack stack = getLaunchStack(r, launchFlags, task, aOptions);
        if (stack != null) {
            return stack;
        }
        if (task != null && task.stack != null) {
            ActivityStack stack2 = task.stack;
            if (stack2.isOnHomeDisplay()) {
                if (this.mSupervisor.mFocusedStack != stack2) {
                    if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_FOCUS, "computeStackFocus: Setting focused stack to r=" + r + " task=" + task);
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG_FOCUS, "computeStackFocus: Focused stack already=" + this.mSupervisor.mFocusedStack);
                }
            }
            return stack2;
        }
        ActivityStackSupervisor.ActivityContainer container = r.mInitialActivityContainer;
        if (container != null) {
            r.mInitialActivityContainer = null;
            return container.mStack;
        }
        int focusedStackId = this.mSupervisor.mFocusedStack.mStackId;
        if (focusedStackId == 1 || (focusedStackId == 3 && r.canGoInDockedStack())) {
            canUseFocusedStack = true;
        } else {
            canUseFocusedStack = focusedStackId == 2 ? r.isResizeableOrForced() : false;
        }
        if (canUseFocusedStack && (!newTask || this.mSupervisor.mFocusedStack.mActivityContainer.isEligibleForNewTasks())) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG_FOCUS, "computeStackFocus: Have a focused stack=" + this.mSupervisor.mFocusedStack);
            }
            return this.mSupervisor.mFocusedStack;
        }
        ArrayList<ActivityStack> homeDisplayStacks = this.mSupervisor.mHomeStack.mStacks;
        for (int stackNdx = homeDisplayStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack3 = homeDisplayStacks.get(stackNdx);
            if (!ActivityManager.StackId.isStaticStack(stack3.mStackId)) {
                if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG_FOCUS, "computeStackFocus: Setting focused stack=" + stack3);
                }
                return stack3;
            }
        }
        if (task != null) {
            stackId = task.getLaunchStackId();
        } else {
            stackId = bounds != null ? 2 : 1;
        }
        ActivityStack stack4 = this.mSupervisor.getStack(stackId, true, true);
        if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_FOCUS, "computeStackFocus: New stack r=" + r + " stackId=" + stack4.mStackId);
        }
        return stack4;
    }

    private ActivityStack getLaunchStack(ActivityRecord r, int launchFlags, TaskRecord task, ActivityOptions aOptions) {
        ActivityStack parentStack;
        if (this.mReuseTask != null) {
            return this.mReuseTask.stack;
        }
        int launchStackId = aOptions != null ? aOptions.getLaunchStackId() : -1;
        if (isValidLaunchStackId(launchStackId, r)) {
            return this.mSupervisor.getStack(launchStackId, true, true);
        }
        if (launchStackId == 3) {
            return this.mSupervisor.getStack(1, true, true);
        }
        if ((launchFlags & 4096) == 0) {
            return null;
        }
        if (task != null) {
            parentStack = task.stack;
        } else {
            parentStack = r.mInitialActivityContainer != null ? r.mInitialActivityContainer.mStack : this.mSupervisor.mFocusedStack;
        }
        if (parentStack != this.mSupervisor.mFocusedStack) {
            return parentStack;
        }
        if (this.mSupervisor.mFocusedStack != null && task == this.mSupervisor.mFocusedStack.topTask()) {
            return this.mSupervisor.mFocusedStack;
        }
        if (parentStack != null && parentStack.mStackId == 3) {
            return this.mSupervisor.getStack(1, true, true);
        }
        ActivityStack dockedStack = this.mSupervisor.getStack(3);
        if (dockedStack == null || dockedStack.getStackVisibilityLocked(r) != 0) {
            return dockedStack;
        }
        return null;
    }

    private boolean isValidLaunchStackId(int stackId, ActivityRecord r) {
        boolean supportsPip;
        if (stackId == -1 || stackId == 0 || !ActivityManager.StackId.isStaticStack(stackId)) {
            return false;
        }
        if (stackId != 1 && (!this.mService.mSupportsMultiWindow || !r.isResizeableOrForced())) {
            return false;
        }
        if (stackId == 3 && r.canGoInDockedStack()) {
            return true;
        }
        if (stackId == 2 && !this.mService.mSupportsFreeformWindowManagement) {
            return false;
        }
        if (!this.mService.mSupportsPictureInPicture) {
            supportsPip = false;
        } else {
            supportsPip = !r.supportsPictureInPicture() ? this.mService.mForceResizableActivities : true;
        }
        return stackId != 4 || supportsPip;
    }

    Rect getOverrideBounds(ActivityRecord r, ActivityOptions options, TaskRecord inTask) {
        if (options == null) {
            return null;
        }
        if ((!r.isResizeable() && (inTask == null || !inTask.isResizeable())) || !this.mSupervisor.canUseActivityOptionsLaunchBounds(options, options.getLaunchStackId())) {
            return null;
        }
        Rect newBounds = TaskRecord.validateBounds(options.getLaunchBounds());
        return newBounds;
    }

    void setWindowManager(WindowManagerService wm) {
        this.mWindowManager = wm;
    }

    void removePendingActivityLaunchesLocked(ActivityStack stack) {
        for (int palNdx = this.mPendingActivityLaunches.size() - 1; palNdx >= 0; palNdx--) {
            ActivityStackSupervisor.PendingActivityLaunch pal = this.mPendingActivityLaunches.get(palNdx);
            if (pal.stack == stack) {
                this.mPendingActivityLaunches.remove(palNdx);
            }
        }
    }
}
