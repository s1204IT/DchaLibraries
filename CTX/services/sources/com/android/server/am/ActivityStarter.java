package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Pools;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.am.LaunchParamsController;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.DumpState;
import com.android.server.pm.InstantAppResolver;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

class ActivityStarter {
    private static final int INVALID_LAUNCH_MODE = -1;
    private boolean mAddingToTask;
    private boolean mAvoidMoveToFront;
    private int mCallingUid;
    private final ActivityStartController mController;
    private boolean mDoResume;
    private TaskRecord mInTask;
    private Intent mIntent;
    private boolean mIntentDelivered;
    private final ActivityStartInterceptor mInterceptor;
    private boolean mKeepCurTransition;
    private int mLastStartActivityResult;
    private long mLastStartActivityTimeMs;
    private String mLastStartReason;
    private int mLaunchFlags;
    private int mLaunchMode;
    private boolean mLaunchTaskBehind;
    private boolean mMovedToFront;
    private ActivityInfo mNewTaskInfo;
    private Intent mNewTaskIntent;
    private boolean mNoAnimation;
    private ActivityRecord mNotTop;
    private ActivityOptions mOptions;
    private int mPreferredDisplayId;
    private TaskRecord mReuseTask;
    private final ActivityManagerService mService;
    private ActivityRecord mSourceRecord;
    private ActivityStack mSourceStack;
    private ActivityRecord mStartActivity;
    private int mStartFlags;
    private final ActivityStackSupervisor mSupervisor;
    private ActivityStack mTargetStack;
    private IVoiceInteractor mVoiceInteractor;
    private IVoiceInteractionSession mVoiceSession;
    private static final String TAG = "ActivityManager";
    private static final String TAG_RESULTS = TAG + ActivityManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_FOCUS = TAG + ActivityManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_USER_LEAVING = TAG + ActivityManagerDebugConfig.POSTFIX_USER_LEAVING;
    private LaunchParamsController.LaunchParams mLaunchParams = new LaunchParamsController.LaunchParams();
    private final ActivityRecord[] mLastStartActivityRecord = new ActivityRecord[1];
    private Request mRequest = new Request();

    @VisibleForTesting
    interface Factory {
        ActivityStarter obtain();

        void recycle(ActivityStarter activityStarter);

        void setController(ActivityStartController activityStartController);
    }

    static class DefaultFactory implements Factory {
        private ActivityStartController mController;
        private ActivityStartInterceptor mInterceptor;
        private ActivityManagerService mService;
        private ActivityStackSupervisor mSupervisor;
        private final int MAX_STARTER_COUNT = 3;
        private Pools.SynchronizedPool<ActivityStarter> mStarterPool = new Pools.SynchronizedPool<>(3);

        DefaultFactory(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, ActivityStartInterceptor activityStartInterceptor) {
            this.mService = activityManagerService;
            this.mSupervisor = activityStackSupervisor;
            this.mInterceptor = activityStartInterceptor;
        }

        @Override
        public void setController(ActivityStartController activityStartController) {
            this.mController = activityStartController;
        }

        @Override
        public ActivityStarter obtain() {
            ActivityStarter activityStarter = (ActivityStarter) this.mStarterPool.acquire();
            if (activityStarter == null) {
                return new ActivityStarter(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
            }
            return activityStarter;
        }

        @Override
        public void recycle(ActivityStarter activityStarter) {
            activityStarter.reset(true);
            this.mStarterPool.release(activityStarter);
        }
    }

    private static class Request {
        private static final int DEFAULT_CALLING_PID = 0;
        private static final int DEFAULT_CALLING_UID = -1;
        static final int DEFAULT_REAL_CALLING_PID = 0;
        static final int DEFAULT_REAL_CALLING_UID = -10000;
        ActivityInfo activityInfo;
        SafeActivityOptions activityOptions;
        boolean allowPendingRemoteAnimationRegistryLookup;
        boolean avoidMoveToFront;
        IApplicationThread caller;
        String callingPackage;
        boolean componentSpecified;
        Intent ephemeralIntent;
        int filterCallingUid;
        Configuration globalConfig;
        boolean ignoreTargetSecurity;
        TaskRecord inTask;
        Intent intent;
        boolean mayWait;
        ActivityRecord[] outActivity;
        ProfilerInfo profilerInfo;
        String reason;
        int requestCode;
        ResolveInfo resolveInfo;
        String resolvedType;
        IBinder resultTo;
        String resultWho;
        int startFlags;
        int userId;
        IVoiceInteractor voiceInteractor;
        IVoiceInteractionSession voiceSession;
        WaitResult waitResult;
        int callingPid = 0;
        int callingUid = -1;
        int realCallingPid = 0;
        int realCallingUid = DEFAULT_REAL_CALLING_UID;

        Request() {
            reset();
        }

        void reset() {
            this.caller = null;
            this.intent = null;
            this.ephemeralIntent = null;
            this.resolvedType = null;
            this.activityInfo = null;
            this.resolveInfo = null;
            this.voiceSession = null;
            this.voiceInteractor = null;
            this.resultTo = null;
            this.resultWho = null;
            this.requestCode = 0;
            this.callingPid = 0;
            this.callingUid = -1;
            this.callingPackage = null;
            this.realCallingPid = 0;
            this.realCallingUid = DEFAULT_REAL_CALLING_UID;
            this.startFlags = 0;
            this.activityOptions = null;
            this.ignoreTargetSecurity = false;
            this.componentSpecified = false;
            this.outActivity = null;
            this.inTask = null;
            this.reason = null;
            this.profilerInfo = null;
            this.globalConfig = null;
            this.userId = 0;
            this.waitResult = null;
            this.mayWait = false;
            this.avoidMoveToFront = false;
            this.allowPendingRemoteAnimationRegistryLookup = true;
            this.filterCallingUid = DEFAULT_REAL_CALLING_UID;
        }

        void set(Request request) {
            this.caller = request.caller;
            this.intent = request.intent;
            this.ephemeralIntent = request.ephemeralIntent;
            this.resolvedType = request.resolvedType;
            this.activityInfo = request.activityInfo;
            this.resolveInfo = request.resolveInfo;
            this.voiceSession = request.voiceSession;
            this.voiceInteractor = request.voiceInteractor;
            this.resultTo = request.resultTo;
            this.resultWho = request.resultWho;
            this.requestCode = request.requestCode;
            this.callingPid = request.callingPid;
            this.callingUid = request.callingUid;
            this.callingPackage = request.callingPackage;
            this.realCallingPid = request.realCallingPid;
            this.realCallingUid = request.realCallingUid;
            this.startFlags = request.startFlags;
            this.activityOptions = request.activityOptions;
            this.ignoreTargetSecurity = request.ignoreTargetSecurity;
            this.componentSpecified = request.componentSpecified;
            this.outActivity = request.outActivity;
            this.inTask = request.inTask;
            this.reason = request.reason;
            this.profilerInfo = request.profilerInfo;
            this.globalConfig = request.globalConfig;
            this.userId = request.userId;
            this.waitResult = request.waitResult;
            this.mayWait = request.mayWait;
            this.avoidMoveToFront = request.avoidMoveToFront;
            this.allowPendingRemoteAnimationRegistryLookup = request.allowPendingRemoteAnimationRegistryLookup;
            this.filterCallingUid = request.filterCallingUid;
        }
    }

    ActivityStarter(ActivityStartController activityStartController, ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, ActivityStartInterceptor activityStartInterceptor) {
        this.mController = activityStartController;
        this.mService = activityManagerService;
        this.mSupervisor = activityStackSupervisor;
        this.mInterceptor = activityStartInterceptor;
        reset(true);
    }

    void set(ActivityStarter activityStarter) {
        this.mStartActivity = activityStarter.mStartActivity;
        this.mIntent = activityStarter.mIntent;
        this.mCallingUid = activityStarter.mCallingUid;
        this.mOptions = activityStarter.mOptions;
        this.mLaunchTaskBehind = activityStarter.mLaunchTaskBehind;
        this.mLaunchFlags = activityStarter.mLaunchFlags;
        this.mLaunchMode = activityStarter.mLaunchMode;
        this.mLaunchParams.set(activityStarter.mLaunchParams);
        this.mNotTop = activityStarter.mNotTop;
        this.mDoResume = activityStarter.mDoResume;
        this.mStartFlags = activityStarter.mStartFlags;
        this.mSourceRecord = activityStarter.mSourceRecord;
        this.mPreferredDisplayId = activityStarter.mPreferredDisplayId;
        this.mInTask = activityStarter.mInTask;
        this.mAddingToTask = activityStarter.mAddingToTask;
        this.mReuseTask = activityStarter.mReuseTask;
        this.mNewTaskInfo = activityStarter.mNewTaskInfo;
        this.mNewTaskIntent = activityStarter.mNewTaskIntent;
        this.mSourceStack = activityStarter.mSourceStack;
        this.mTargetStack = activityStarter.mTargetStack;
        this.mMovedToFront = activityStarter.mMovedToFront;
        this.mNoAnimation = activityStarter.mNoAnimation;
        this.mKeepCurTransition = activityStarter.mKeepCurTransition;
        this.mAvoidMoveToFront = activityStarter.mAvoidMoveToFront;
        this.mVoiceSession = activityStarter.mVoiceSession;
        this.mVoiceInteractor = activityStarter.mVoiceInteractor;
        this.mIntentDelivered = activityStarter.mIntentDelivered;
        this.mRequest.set(activityStarter.mRequest);
    }

    ActivityRecord getStartActivity() {
        return this.mStartActivity;
    }

    boolean relatedToPackage(String str) {
        return (this.mLastStartActivityRecord[0] != null && str.equals(this.mLastStartActivityRecord[0].packageName)) || (this.mStartActivity != null && str.equals(this.mStartActivity.packageName));
    }

    int execute() {
        try {
            if (this.mRequest.mayWait) {
                return startActivityMayWait(this.mRequest.caller, this.mRequest.callingUid, this.mRequest.callingPackage, this.mRequest.realCallingPid, this.mRequest.realCallingUid, this.mRequest.intent, this.mRequest.resolvedType, this.mRequest.voiceSession, this.mRequest.voiceInteractor, this.mRequest.resultTo, this.mRequest.resultWho, this.mRequest.requestCode, this.mRequest.startFlags, this.mRequest.profilerInfo, this.mRequest.waitResult, this.mRequest.globalConfig, this.mRequest.activityOptions, this.mRequest.ignoreTargetSecurity, this.mRequest.userId, this.mRequest.inTask, this.mRequest.reason, this.mRequest.allowPendingRemoteAnimationRegistryLookup);
            }
            return startActivity(this.mRequest.caller, this.mRequest.intent, this.mRequest.ephemeralIntent, this.mRequest.resolvedType, this.mRequest.activityInfo, this.mRequest.resolveInfo, this.mRequest.voiceSession, this.mRequest.voiceInteractor, this.mRequest.resultTo, this.mRequest.resultWho, this.mRequest.requestCode, this.mRequest.callingPid, this.mRequest.callingUid, this.mRequest.callingPackage, this.mRequest.realCallingPid, this.mRequest.realCallingUid, this.mRequest.startFlags, this.mRequest.activityOptions, this.mRequest.ignoreTargetSecurity, this.mRequest.componentSpecified, this.mRequest.outActivity, this.mRequest.inTask, this.mRequest.reason, this.mRequest.allowPendingRemoteAnimationRegistryLookup);
        } finally {
            onExecutionComplete();
        }
    }

    int startResolvedActivity(ActivityRecord activityRecord, ActivityRecord activityRecord2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i, boolean z, ActivityOptions activityOptions, TaskRecord taskRecord, ActivityRecord[] activityRecordArr) {
        try {
            return startActivity(activityRecord, activityRecord2, iVoiceInteractionSession, iVoiceInteractor, i, z, activityOptions, taskRecord, activityRecordArr);
        } finally {
            onExecutionComplete();
        }
    }

    private int startActivity(IApplicationThread iApplicationThread, Intent intent, Intent intent2, String str, ActivityInfo activityInfo, ResolveInfo resolveInfo, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, IBinder iBinder, String str2, int i, int i2, int i3, String str3, int i4, int i5, int i6, SafeActivityOptions safeActivityOptions, boolean z, boolean z2, ActivityRecord[] activityRecordArr, TaskRecord taskRecord, String str4, boolean z3) {
        if (TextUtils.isEmpty(str4)) {
            throw new IllegalArgumentException("Need to specify a reason.");
        }
        this.mLastStartReason = str4;
        this.mLastStartActivityTimeMs = System.currentTimeMillis();
        this.mLastStartActivityRecord[0] = null;
        this.mLastStartActivityResult = startActivity(iApplicationThread, intent, intent2, str, activityInfo, resolveInfo, iVoiceInteractionSession, iVoiceInteractor, iBinder, str2, i, i2, i3, str3, i4, i5, i6, safeActivityOptions, z, z2, this.mLastStartActivityRecord, taskRecord, z3);
        if (activityRecordArr != null) {
            activityRecordArr[0] = this.mLastStartActivityRecord[0];
        }
        return getExternalResult(this.mLastStartActivityResult);
    }

    static int getExternalResult(int i) {
        if (i != 102) {
            return i;
        }
        return 0;
    }

    private void onExecutionComplete() {
        this.mController.onExecutionComplete(this);
    }

    private int startActivity(IApplicationThread iApplicationThread, Intent intent, Intent intent2, String str, ActivityInfo activityInfo, ResolveInfo resolveInfo, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, IBinder iBinder, String str2, int i, int i2, int i3, String str3, int i4, int i5, int i6, SafeActivityOptions safeActivityOptions, boolean z, boolean z2, ActivityRecord[] activityRecordArr, TaskRecord taskRecord, boolean z3) {
        int i7;
        int i8;
        int i9;
        ProcessRecord processRecord;
        int userId;
        boolean z4;
        ActivityRecord activityRecordIsInAnyStackLocked;
        ActivityRecord activityRecord;
        String str4;
        int i10;
        String str5;
        ActivityRecord activityRecord2;
        int i11;
        ActivityStack stack;
        ProcessRecord processRecord2;
        ActivityInfo activityInfo2;
        ActivityOptions activityOptionsOverrideOptionsIfNeeded;
        int i12;
        ResolveInfo resolveInfo2;
        TaskRecord taskRecord2;
        String str6;
        ActivityOptions activityOptions;
        int i13;
        ActivityInfo activityInfo3;
        int i14;
        int i15;
        ?? r12;
        ActivityRecord activityRecord3;
        String str7;
        int i16;
        ?? r9;
        String str8;
        int i17;
        String str9;
        ActivityInfo activityInfoResolveActivity;
        ?? r31;
        int i18;
        ActivityRecord activityRecord4;
        Intent intent3 = intent;
        Bundle bundlePopAppVerificationBundle = safeActivityOptions != null ? safeActivityOptions.popAppVerificationBundle() : null;
        if (iApplicationThread != null) {
            ProcessRecord recordForAppLocked = this.mService.getRecordForAppLocked(iApplicationThread);
            if (recordForAppLocked != null) {
                i8 = recordForAppLocked.pid;
                processRecord = recordForAppLocked;
                i7 = recordForAppLocked.info.uid;
                i9 = 0;
            } else {
                Slog.w(TAG, "Unable to find app for caller " + iApplicationThread + " (pid=" + i2 + ") when starting: " + intent.toString());
                i9 = -94;
                i7 = i3;
                i8 = i2;
                processRecord = recordForAppLocked;
            }
        } else {
            i7 = i3;
            i8 = i2;
            i9 = 0;
            processRecord = null;
        }
        if (activityInfo != null && activityInfo.applicationInfo != null) {
            userId = UserHandle.getUserId(activityInfo.applicationInfo.uid);
        } else {
            userId = 0;
        }
        if (i9 == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("START u");
            sb.append(userId);
            sb.append(" {");
            z4 = true;
            sb.append(intent3.toShortString(true, true, true, false));
            sb.append("} from uid ");
            sb.append(i7);
            Slog.i(TAG, sb.toString());
        } else {
            z4 = true;
        }
        if (iBinder != null) {
            activityRecordIsInAnyStackLocked = this.mSupervisor.isInAnyStackLocked(iBinder);
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                Slog.v(TAG_RESULTS, "Will send result to " + iBinder + " " + activityRecordIsInAnyStackLocked);
            }
            if (activityRecordIsInAnyStackLocked == null || i < 0 || activityRecordIsInAnyStackLocked.finishing) {
                activityRecord = activityRecordIsInAnyStackLocked;
                activityRecordIsInAnyStackLocked = null;
            } else {
                activityRecord = activityRecordIsInAnyStackLocked;
            }
        } else {
            activityRecordIsInAnyStackLocked = null;
            activityRecord = null;
        }
        int flags = intent.getFlags();
        if ((33554432 & flags) == 0 || activityRecord == null) {
            str4 = str2;
            i10 = i;
        } else {
            if (i >= 0) {
                SafeActivityOptions.abort(safeActivityOptions);
                return -93;
            }
            activityRecordIsInAnyStackLocked = activityRecord.resultTo;
            if (activityRecordIsInAnyStackLocked != null && !activityRecordIsInAnyStackLocked.isInStackLocked()) {
                activityRecordIsInAnyStackLocked = null;
            }
            String str10 = activityRecord.resultWho;
            int i19 = activityRecord.requestCode;
            activityRecord.resultTo = null;
            if (activityRecordIsInAnyStackLocked != null) {
                activityRecordIsInAnyStackLocked.removeResultsLocked(activityRecord, str10, i19);
            }
            if (activityRecord.launchedFromUid == i7) {
                str4 = str10;
                i10 = i19;
                str5 = activityRecord.launchedFromPackage;
                activityRecord2 = activityRecordIsInAnyStackLocked;
                if (i9 == 0 && intent.getComponent() == null) {
                    i9 = -91;
                }
                if (i9 == 0 && activityInfo == null) {
                    i9 = -92;
                }
                i11 = -97;
                if (i9 == 0 && activityRecord != null && activityRecord.getTask().voiceSession != null && (268435456 & flags) == 0 && activityRecord.info.applicationInfo.uid != activityInfo.applicationInfo.uid) {
                    try {
                        intent3.addCategory("android.intent.category.VOICE");
                        if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent3, str)) {
                            Slog.w(TAG, "Activity being started in current voice task does not support voice: " + intent3);
                            i9 = -97;
                        }
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failure checking voice capabilities", e);
                        i9 = -97;
                    }
                }
                if (i9 != 0 && iVoiceInteractionSession != null) {
                    try {
                        if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent3, str)) {
                            Slog.w(TAG, "Activity being started in new voice task does not support: " + intent3);
                        } else {
                            i11 = i9;
                        }
                    } catch (RemoteException e2) {
                        Slog.w(TAG, "Failure checking voice capabilities", e2);
                    }
                } else {
                    i11 = i9;
                }
                if (activityRecord2 == null) {
                    stack = activityRecord2.getStack();
                } else {
                    stack = null;
                }
                if (i11 == 0) {
                    if (activityRecord2 != null) {
                        stack.sendActivityResultLocked(-1, activityRecord2, str4, i10, 0, null);
                    }
                    SafeActivityOptions.abort(safeActivityOptions);
                    return i11;
                }
                int i20 = userId;
                ProcessRecord processRecord3 = processRecord;
                int i21 = i7;
                ActivityRecord activityRecord5 = activityRecord;
                int i22 = z4;
                int i23 = !this.mSupervisor.checkStartAnyActivityPermission(intent3, activityInfo, str4, i10, i8, i21, str5, z, taskRecord != null ? z4 : false, processRecord3, activityRecord2, stack) ? 1 : 0;
                String str11 = str5;
                int i24 = i23 | ((this.mService.mIntentFirewall.checkStartActivity(intent3, i21, i8, str, activityInfo.applicationInfo) ? 1 : 0) ^ i22);
                if (safeActivityOptions != null) {
                    processRecord2 = processRecord3;
                    activityInfo2 = activityInfo;
                    activityOptionsOverrideOptionsIfNeeded = safeActivityOptions.getOptions(intent3, activityInfo2, processRecord2, this.mSupervisor);
                } else {
                    processRecord2 = processRecord3;
                    activityInfo2 = activityInfo;
                    activityOptionsOverrideOptionsIfNeeded = null;
                }
                if (z3) {
                    activityOptionsOverrideOptionsIfNeeded = this.mService.getActivityStartController().getPendingRemoteAnimationRegistry().overrideOptionsIfNeeded(str11, activityOptionsOverrideOptionsIfNeeded);
                }
                ActivityOptions activityOptions2 = activityOptionsOverrideOptionsIfNeeded;
                if (this.mService.mController != null) {
                    try {
                        i12 = i24 | ((this.mService.mController.activityStarting(intent.cloneFilter(), activityInfo2.applicationInfo.packageName) ? 1 : 0) ^ i22);
                    } catch (RemoteException e3) {
                        this.mService.mController = null;
                        i12 = i24;
                    }
                    this.mInterceptor.setStates(i20, i4, i5, i6, str11);
                    ProcessRecord processRecord4 = processRecord2;
                    if (this.mInterceptor.intercept(intent3, resolveInfo, activityInfo, str, taskRecord, i8, i21, activityOptions2)) {
                        resolveInfo2 = resolveInfo;
                        taskRecord2 = taskRecord;
                        str6 = str;
                        activityOptions = activityOptions2;
                        i13 = i21;
                        activityInfo3 = activityInfo;
                    } else {
                        Intent intent4 = this.mInterceptor.mIntent;
                        ResolveInfo resolveInfo3 = this.mInterceptor.mRInfo;
                        activityInfo3 = this.mInterceptor.mAInfo;
                        str6 = this.mInterceptor.mResolvedType;
                        taskRecord2 = this.mInterceptor.mInTask;
                        int i25 = this.mInterceptor.mCallingPid;
                        int i26 = this.mInterceptor.mCallingUid;
                        activityOptions = this.mInterceptor.mActivityOptions;
                        i8 = i25;
                        i13 = i26;
                        intent3 = intent4;
                        resolveInfo2 = resolveInfo3;
                    }
                    if (i12 == 0) {
                        if (activityRecord2 != null) {
                            stack.sendActivityResultLocked(-1, activityRecord2, str4, i10, 0, null);
                        }
                        ActivityOptions.abort(activityOptions);
                        return HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION;
                    }
                    if (this.mService.mPermissionReviewRequired && activityInfo3 != null && this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(activityInfo3.packageName, i20)) {
                        ActivityManagerService activityManagerService = this.mService;
                        Intent[] intentArr = new Intent[i22];
                        intentArr[0] = intent3;
                        String[] strArr = new String[i22];
                        strArr[0] = str6;
                        IIntentSender intentSenderLocked = activityManagerService.getIntentSenderLocked(2, str11, i13, i20, null, null, 0, intentArr, strArr, 1342177280, null);
                        int flags2 = intent3.getFlags();
                        ?? intent5 = new Intent("android.intent.action.REVIEW_PERMISSIONS");
                        intent5.setFlags(flags2 | DumpState.DUMP_VOLUMES);
                        intent5.putExtra("android.intent.extra.PACKAGE_NAME", activityInfo3.packageName);
                        intent5.putExtra("android.intent.extra.INTENT", new IntentSender(intentSenderLocked));
                        if (activityRecord2 != null) {
                            intent5.putExtra("android.intent.extra.RESULT_NEEDED", i22);
                        }
                        i14 = i20;
                        i15 = i6;
                        ?? r122 = i22;
                        activityRecord3 = activityRecord5;
                        ResolveInfo resolveInfoResolveIntent = this.mSupervisor.resolveIntent(intent5, null, i14, 0, computeResolveFilterUid(i5, i5, this.mRequest.filterCallingUid));
                        str7 = null;
                        ActivityInfo activityInfoResolveActivity2 = this.mSupervisor.resolveActivity(intent5, resolveInfoResolveIntent, i15, null);
                        if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("START u");
                            sb2.append(i14);
                            sb2.append(" {");
                            sb2.append(intent5.toShortString(r122, r122, r122, false));
                            sb2.append("} from uid ");
                            sb2.append(i5);
                            sb2.append(" on display ");
                            sb2.append(this.mSupervisor.mFocusedStack == null ? 0 : this.mSupervisor.mFocusedStack.mDisplayId);
                            Slog.i(TAG, sb2.toString());
                        }
                        i8 = i4;
                        str8 = null;
                        resolveInfo2 = resolveInfoResolveIntent;
                        i16 = i5;
                        i13 = i16;
                        activityInfo3 = activityInfoResolveActivity2;
                        r9 = intent5;
                        r12 = r122;
                    } else {
                        i14 = i20;
                        i15 = i6;
                        r12 = i22;
                        activityRecord3 = activityRecord5;
                        str7 = null;
                        i16 = i5;
                        String str12 = str6;
                        r9 = intent3;
                        str8 = str12;
                    }
                    if (resolveInfo2 == null || resolveInfo2.auxiliaryInfo == null) {
                        i17 = i16;
                        str9 = str8;
                        activityInfoResolveActivity = activityInfo3;
                        r31 = r9;
                        i18 = i13;
                    } else {
                        i18 = i16;
                        Intent intentCreateLaunchIntent = createLaunchIntent(resolveInfo2.auxiliaryInfo, intent2, str11, bundlePopAppVerificationBundle, str8, i14);
                        i8 = i4;
                        activityInfoResolveActivity = this.mSupervisor.resolveActivity(intentCreateLaunchIntent, resolveInfo2, i15, str7);
                        r31 = intentCreateLaunchIntent;
                        i17 = i18;
                        str9 = str7;
                    }
                    ActivityRecord activityRecord6 = new ActivityRecord(this.mService, processRecord4, i8, i18, str11, r31, str9, activityInfoResolveActivity, this.mService.getGlobalConfiguration(), activityRecord2, str4, i10, z2, iVoiceInteractionSession != null ? r12 : 0, this.mSupervisor, activityOptions, activityRecord3);
                    if (activityRecordArr != null) {
                        activityRecordArr[0] = activityRecord6;
                    }
                    if (activityRecord6.appTimeTracker == null) {
                        activityRecord4 = activityRecord3;
                        if (activityRecord4 != null) {
                            activityRecord6.appTimeTracker = activityRecord4.appTimeTracker;
                        }
                    } else {
                        activityRecord4 = activityRecord3;
                    }
                    ActivityStack activityStack = this.mSupervisor.mFocusedStack;
                    if (iVoiceInteractionSession == null && ((activityStack.getResumedActivity() == null || activityStack.getResumedActivity().info.applicationInfo.uid != i17) && !this.mService.checkAppSwitchAllowedLocked(i8, i18, i4, i17, "Activity start"))) {
                        this.mController.addPendingActivityLaunch(new ActivityStackSupervisor.PendingActivityLaunch(activityRecord6, activityRecord4, i15, activityStack, processRecord4));
                        ActivityOptions.abort(activityOptions);
                        return 100;
                    }
                    if (this.mService.mDidAppSwitch) {
                        this.mService.mAppSwitchesAllowedTime = 0L;
                    } else {
                        this.mService.mDidAppSwitch = r12;
                    }
                    this.mController.doPendingActivityLaunches(false);
                    return startActivity(activityRecord6, activityRecord4, iVoiceInteractionSession, iVoiceInteractor, i15, true, activityOptions, taskRecord2, activityRecordArr);
                }
                i12 = i24;
                this.mInterceptor.setStates(i20, i4, i5, i6, str11);
                ProcessRecord processRecord42 = processRecord2;
                if (this.mInterceptor.intercept(intent3, resolveInfo, activityInfo, str, taskRecord, i8, i21, activityOptions2)) {
                }
                if (i12 == 0) {
                }
            } else {
                str4 = str10;
                i10 = i19;
            }
        }
        str5 = str3;
        activityRecord2 = activityRecordIsInAnyStackLocked;
        if (i9 == 0) {
            i9 = -91;
        }
        if (i9 == 0) {
            i9 = -92;
        }
        i11 = -97;
        if (i9 == 0) {
            intent3.addCategory("android.intent.category.VOICE");
            if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent3, str)) {
            }
        }
        if (i9 != 0) {
            i11 = i9;
        }
        if (activityRecord2 == null) {
        }
        if (i11 == 0) {
        }
    }

    private Intent createLaunchIntent(AuxiliaryResolveInfo auxiliaryResolveInfo, Intent intent, String str, Bundle bundle, String str2, int i) {
        Intent intent2;
        ComponentName componentName;
        if (auxiliaryResolveInfo != null && auxiliaryResolveInfo.needsPhaseTwo) {
            this.mService.getPackageManagerInternalLocked().requestInstantAppResolutionPhaseTwo(auxiliaryResolveInfo, intent, str2, str, bundle, i);
        }
        Intent intentSanitizeIntent = InstantAppResolver.sanitizeIntent(intent);
        List list = null;
        if (auxiliaryResolveInfo != null) {
            intent2 = auxiliaryResolveInfo.failureIntent;
        } else {
            intent2 = null;
        }
        if (auxiliaryResolveInfo != null) {
            componentName = auxiliaryResolveInfo.installFailureActivity;
        } else {
            componentName = null;
        }
        String str3 = auxiliaryResolveInfo == null ? null : auxiliaryResolveInfo.token;
        boolean z = auxiliaryResolveInfo != null && auxiliaryResolveInfo.needsPhaseTwo;
        if (auxiliaryResolveInfo != null) {
            list = auxiliaryResolveInfo.filters;
        }
        return InstantAppResolver.buildEphemeralInstallerIntent(intent, intentSanitizeIntent, intent2, str, bundle, str2, i, componentName, str3, z, list);
    }

    void postStartActivityProcessing(ActivityRecord activityRecord, int i, ActivityStack activityStack) {
        if (ActivityManager.isStartResultFatalError(i)) {
        }
        this.mSupervisor.reportWaitingActivityLaunchedIfNeeded(activityRecord, i);
        ActivityStack stack = activityRecord.getStack();
        if (stack == null) {
            if (this.mTargetStack == null) {
                stack = null;
            } else {
                stack = activityStack;
            }
        }
        if (stack == null) {
            return;
        }
        boolean z = (this.mLaunchFlags & 268468224) == 268468224 && this.mReuseTask != null;
        if (i == 2 || i == 3 || z) {
            switch (stack.getWindowingMode()) {
                case 2:
                    this.mService.mTaskChangeNotificationController.notifyPinnedActivityRestartAttempt(z);
                    break;
                case 3:
                    ActivityStack activityStack2 = this.mSupervisor.mHomeStack;
                    if (activityStack2 != null && activityStack2.shouldBeVisible(null)) {
                        this.mService.mWindowManager.showRecentApps();
                        break;
                    }
                    break;
            }
        }
    }

    private int startActivityMayWait(IApplicationThread iApplicationThread, int i, String str, int i2, int i3, Intent intent, String str2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, IBinder iBinder, String str3, int i4, int i5, ProfilerInfo profilerInfo, WaitResult waitResult, Configuration configuration, SafeActivityOptions safeActivityOptions, boolean z, int i6, TaskRecord taskRecord, String str4, boolean z2) throws Throwable {
        int callingPid;
        int callingUid;
        int i7;
        int i8;
        boolean z3;
        ResolveInfo resolveInfo;
        ActivityStack activityStack;
        ActivityManagerService activityManagerService;
        ActivityInfo activityInfo;
        ?? r2;
        int i9;
        long j;
        boolean z4;
        int i10;
        String str5;
        ActivityInfo activityInfoForUser;
        ?? r3;
        boolean z5;
        IApplicationThread iApplicationThread2;
        int i11;
        int i12;
        ResolveInfo resolveInfo2;
        ActivityStack activityStack2;
        ?? r22;
        ?? r32;
        ProcessRecord processRecord;
        Intent intent2;
        UserInfo userInfo;
        boolean z6;
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching();
        boolean z7 = intent.getComponent() != null;
        if (i2 == 0) {
            callingPid = Binder.getCallingPid();
        } else {
            callingPid = i2;
        }
        if (i3 == -10000) {
            callingUid = Binder.getCallingUid();
        } else {
            callingUid = i3;
        }
        if (i < 0) {
            if (iApplicationThread == null) {
                i7 = callingUid;
                i8 = callingPid;
            } else {
                i7 = -1;
                i8 = -1;
            }
        } else {
            i7 = i;
            i8 = -1;
        }
        Intent intent3 = new Intent(intent);
        Intent intent4 = new Intent(intent);
        if (!z7 || (("android.intent.action.VIEW".equals(intent4.getAction()) && intent4.getData() == null) || "android.intent.action.INSTALL_INSTANT_APP_PACKAGE".equals(intent4.getAction()) || "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE".equals(intent4.getAction()) || !this.mService.getPackageManagerInternalLocked().isInstantAppInstallerComponent(intent4.getComponent()))) {
            z3 = z7;
        } else {
            intent4.setComponent(null);
            z3 = false;
        }
        ?? r26 = intent4;
        ResolveInfo resolveInfoResolveIntent = this.mSupervisor.resolveIntent(intent4, str2, i6, 0, computeResolveFilterUid(i7, callingUid, this.mRequest.filterCallingUid));
        if (resolveInfoResolveIntent == null && (userInfo = this.mSupervisor.getUserInfo(i6)) != null && userInfo.isManagedProfile()) {
            UserManager userManager = UserManager.get(this.mService.mContext);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UserInfo profileParent = userManager.getProfileParent(i6);
                if (profileParent != null && userManager.isUserUnlockingOrUnlocked(profileParent.id)) {
                    if (!userManager.isUserUnlockingOrUnlocked(i6)) {
                        z6 = true;
                    }
                    if (z6) {
                    }
                } else {
                    z6 = false;
                    if (z6) {
                        resolveInfoResolveIntent = this.mSupervisor.resolveIntent(r26, str2, i6, 786432, computeResolveFilterUid(i7, callingUid, this.mRequest.filterCallingUid));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        ActivityInfo activityInfoResolveActivity = this.mSupervisor.resolveActivity(r26, resolveInfoResolveIntent, i5, profilerInfo);
        ActivityManagerService activityManagerService2 = this.mService;
        synchronized (activityManagerService2) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActivityStack activityStack3 = this.mSupervisor.mFocusedStack;
                    activityStack3.mConfigWillChange = (configuration == null || this.mService.getGlobalConfiguration().diff(configuration) == 0) ? false : true;
                    if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                        String str6 = TAG_CONFIGURATION;
                        StringBuilder sb = new StringBuilder();
                        resolveInfo = resolveInfoResolveIntent;
                        sb.append("Starting activity when config will change = ");
                        sb.append(activityStack3.mConfigWillChange);
                        Slog.v(str6, sb.toString());
                    } else {
                        resolveInfo = resolveInfoResolveIntent;
                    }
                    long jClearCallingIdentity2 = Binder.clearCallingIdentity();
                    if (activityInfoResolveActivity == null || (activityInfoResolveActivity.applicationInfo.privateFlags & 2) == 0 || !this.mService.mHasHeavyWeightFeature) {
                        activityStack = activityStack3;
                        activityManagerService = activityManagerService2;
                        activityInfo = activityInfoResolveActivity;
                        r2 = r26;
                        i9 = callingUid;
                        j = jClearCallingIdentity2;
                    } else {
                        if (activityInfoResolveActivity.processName.equals(activityInfoResolveActivity.applicationInfo.packageName) && (processRecord = this.mService.mHeavyWeightProcess) != null && (processRecord.info.uid != activityInfoResolveActivity.applicationInfo.uid || !processRecord.processName.equals(activityInfoResolveActivity.processName))) {
                            if (iApplicationThread != null) {
                                ProcessRecord recordForAppLocked = this.mService.getRecordForAppLocked(iApplicationThread);
                                if (recordForAppLocked != null) {
                                    i7 = recordForAppLocked.info.uid;
                                } else {
                                    Slog.w(TAG, "Unable to find app for caller " + iApplicationThread + " (pid=" + i8 + ") when starting: " + r26.toString());
                                    SafeActivityOptions.abort(safeActivityOptions);
                                }
                            }
                            ?? r11 = {r26};
                            activityStack = activityStack3;
                            activityManagerService = activityManagerService2;
                            j = jClearCallingIdentity2;
                            i9 = callingUid;
                            i10 = 1;
                            IIntentSender intentSenderLocked = this.mService.getIntentSenderLocked(2, PackageManagerService.PLATFORM_PACKAGE_NAME, i7, i6, null, null, 0, r11, new String[]{str2}, 1342177280, null);
                            Intent intent5 = new Intent();
                            if (i4 >= 0) {
                                intent5.putExtra("has_result", true);
                            }
                            intent5.putExtra("intent", new IntentSender(intentSenderLocked));
                            if (processRecord.activities.size() > 0) {
                                z4 = false;
                                ActivityRecord activityRecord = processRecord.activities.get(0);
                                intent5.putExtra("cur_app", activityRecord.packageName);
                                intent5.putExtra("cur_task", activityRecord.getTask().taskId);
                            } else {
                                z4 = false;
                            }
                            intent5.putExtra("new_app", activityInfoResolveActivity.packageName);
                            intent5.setFlags(r26.getFlags());
                            intent5.setClassName(PackageManagerService.PLATFORM_PACKAGE_NAME, HeavyWeightSwitcherActivity.class.getName());
                            int callingUid2 = Binder.getCallingUid();
                            int callingPid2 = Binder.getCallingPid();
                            ResolveInfo resolveInfoResolveIntent2 = this.mSupervisor.resolveIntent(intent5, null, i6, 0, computeResolveFilterUid(callingUid2, i9, this.mRequest.filterCallingUid));
                            ActivityInfo activityInfo2 = resolveInfoResolveIntent2 != null ? resolveInfoResolveIntent2.activityInfo : null;
                            if (activityInfo2 != null) {
                                intent2 = intent5;
                                i11 = callingUid2;
                                z5 = true;
                                iApplicationThread2 = null;
                                str5 = null;
                                resolveInfo2 = resolveInfoResolveIntent2;
                                activityInfoForUser = this.mService.getActivityInfoForUser(activityInfo2, i6);
                            } else {
                                intent2 = intent5;
                                i11 = callingUid2;
                                z5 = true;
                                iApplicationThread2 = null;
                                str5 = null;
                                resolveInfo2 = resolveInfoResolveIntent2;
                                activityInfoForUser = activityInfo2;
                            }
                            i12 = callingPid2;
                            r3 = intent2;
                            ActivityRecord[] activityRecordArr = new ActivityRecord[i10];
                            int iStartActivity = startActivity(iApplicationThread2, r3, intent3, str5, activityInfoForUser, resolveInfo2, iVoiceInteractionSession, iVoiceInteractor, iBinder, str3, i4, i12, i11, str, callingPid, i9, i5, safeActivityOptions, z, z5, activityRecordArr, taskRecord, str4, z2);
                            Binder.restoreCallingIdentity(j);
                            activityStack2 = activityStack;
                            if (!activityStack2.mConfigWillChange) {
                                ?? r23 = this;
                                r23.mService.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                                r32 = 0;
                                activityStack2.mConfigWillChange = false;
                                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                    Slog.v(TAG_CONFIGURATION, "Updating to new configuration after starting activity.");
                                }
                                r23.mService.updateConfigurationLocked(configuration, null, false);
                                r22 = r23;
                            } else {
                                r22 = this;
                                r32 = 0;
                            }
                            if (waitResult != null) {
                                waitResult.result = iStartActivity;
                                ActivityRecord activityRecord2 = activityRecordArr[r32];
                                if (iStartActivity == 0) {
                                    r22.mSupervisor.mWaitingActivityLaunched.add(waitResult);
                                    do {
                                        try {
                                            r22.mService.wait();
                                        } catch (InterruptedException e) {
                                        }
                                        if (waitResult.result == 2 || waitResult.timeout) {
                                            break;
                                        }
                                    } while (waitResult.who == null);
                                    if (waitResult.result == 2) {
                                        iStartActivity = 2;
                                    }
                                } else {
                                    switch (iStartActivity) {
                                        case 2:
                                            if (activityRecord2.nowVisible && activityRecord2.isState(ActivityStack.ActivityState.RESUMED)) {
                                                waitResult.timeout = r32;
                                                waitResult.who = activityRecord2.realActivity;
                                                waitResult.totalTime = 0L;
                                                waitResult.thisTime = 0L;
                                                break;
                                            } else {
                                                waitResult.thisTime = SystemClock.uptimeMillis();
                                                r22.mSupervisor.waitActivityVisible(activityRecord2.realActivity, waitResult);
                                                do {
                                                    try {
                                                        r22.mService.wait();
                                                        break;
                                                    } catch (InterruptedException e2) {
                                                    }
                                                    if (waitResult.timeout) {
                                                        break;
                                                    }
                                                } while (waitResult.who == null);
                                            }
                                            break;
                                        case 3:
                                            waitResult.timeout = r32;
                                            waitResult.who = activityRecord2.realActivity;
                                            waitResult.totalTime = 0L;
                                            waitResult.thisTime = 0L;
                                            break;
                                    }
                                }
                            }
                            r22.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(iStartActivity, activityRecordArr[r32]);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return iStartActivity;
                        }
                        activityManagerService = activityManagerService2;
                        activityInfo = activityInfoResolveActivity;
                        r2 = r26;
                        i9 = callingUid;
                        j = jClearCallingIdentity2;
                        activityStack = activityStack3;
                    }
                    z4 = false;
                    i10 = 1;
                    str5 = str2;
                    activityInfoForUser = activityInfo;
                    r3 = r2;
                    z5 = z3;
                    iApplicationThread2 = iApplicationThread;
                    i11 = i7;
                    i12 = i8;
                    resolveInfo2 = resolveInfo;
                    ActivityRecord[] activityRecordArr2 = new ActivityRecord[i10];
                    int iStartActivity2 = startActivity(iApplicationThread2, r3, intent3, str5, activityInfoForUser, resolveInfo2, iVoiceInteractionSession, iVoiceInteractor, iBinder, str3, i4, i12, i11, str, callingPid, i9, i5, safeActivityOptions, z, z5, activityRecordArr2, taskRecord, str4, z2);
                    Binder.restoreCallingIdentity(j);
                    activityStack2 = activityStack;
                    if (!activityStack2.mConfigWillChange) {
                    }
                    if (waitResult != null) {
                    }
                    r22.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(iStartActivity2, activityRecordArr2[r32]);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return iStartActivity2;
                } catch (Throwable th) {
                    th = th;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                r26 = activityManagerService2;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return -94;
    }

    static int computeResolveFilterUid(int i, int i2, int i3) {
        if (i3 != -10000) {
            return i3;
        }
        return i >= 0 ? i : i2;
    }

    private int startActivity(ActivityRecord activityRecord, ActivityRecord activityRecord2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i, boolean z, ActivityOptions activityOptions, TaskRecord taskRecord, ActivityRecord[] activityRecordArr) {
        try {
            this.mService.mWindowManager.deferSurfaceLayout();
            int iStartActivityUnchecked = startActivityUnchecked(activityRecord, activityRecord2, iVoiceInteractionSession, iVoiceInteractor, i, z, activityOptions, taskRecord, activityRecordArr);
            ActivityStack stack = this.mStartActivity.getStack();
            if (!ActivityManager.isStartResultSuccessful(iStartActivityUnchecked) && stack != null) {
                stack.finishActivityLocked(this.mStartActivity, 0, null, "startActivity", true);
            }
            this.mService.mWindowManager.continueSurfaceLayout();
            postStartActivityProcessing(activityRecord, iStartActivityUnchecked, this.mTargetStack);
            return iStartActivityUnchecked;
        } catch (Throwable th) {
            ActivityStack stack2 = this.mStartActivity.getStack();
            if (!ActivityManager.isStartResultSuccessful(-96) && stack2 != null) {
                stack2.finishActivityLocked(this.mStartActivity, 0, null, "startActivity", true);
            }
            this.mService.mWindowManager.continueSurfaceLayout();
            throw th;
        }
    }

    private int startActivityUnchecked(ActivityRecord activityRecord, ActivityRecord activityRecord2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i, boolean z, ActivityOptions activityOptions, TaskRecord taskRecord, ActivityRecord[] activityRecordArr) {
        int launchWindowingMode;
        int launchDisplayId;
        TaskRecord task;
        int taskFromInTask;
        boolean z2;
        ActivityRecord activityRecord3;
        setInitialState(activityRecord, activityOptions, taskRecord, z, i, activityRecord2, iVoiceInteractionSession, iVoiceInteractor);
        computeLaunchingTaskFlags();
        computeSourceStack();
        this.mIntent.setFlags(this.mLaunchFlags);
        ActivityRecord reusableIntentActivity = getReusableIntentActivity();
        if (this.mOptions != null) {
            launchWindowingMode = this.mOptions.getLaunchWindowingMode();
            launchDisplayId = this.mOptions.getLaunchDisplayId();
        } else {
            launchWindowingMode = 0;
            launchDisplayId = 0;
        }
        if (!this.mLaunchParams.isEmpty()) {
            if (this.mLaunchParams.hasPreferredDisplay()) {
                launchDisplayId = this.mLaunchParams.mPreferredDisplayId;
            }
            if (this.mLaunchParams.hasWindowingMode()) {
                launchWindowingMode = this.mLaunchParams.mWindowingMode;
            }
        }
        if (reusableIntentActivity != null) {
            if (this.mService.getLockTaskController().isLockTaskModeViolation(reusableIntentActivity.getTask(), (this.mLaunchFlags & 268468224) == 268468224)) {
                Slog.e(TAG, "startActivityUnchecked: Attempt to violate Lock Task Mode");
                return 101;
            }
            boolean z3 = (this.mLaunchFlags & 69206016) == 69206016 && this.mLaunchMode == 0;
            if (this.mStartActivity.getTask() == null && !z3) {
                this.mStartActivity.setTask(reusableIntentActivity.getTask());
            }
            if (reusableIntentActivity.getTask().intent == null) {
                reusableIntentActivity.getTask().setIntent(this.mStartActivity);
            }
            if ((this.mLaunchFlags & 67108864) != 0 || isDocumentLaunchesIntoExisting(this.mLaunchFlags) || isLaunchModeOneOf(3, 2)) {
                TaskRecord task2 = reusableIntentActivity.getTask();
                ActivityRecord activityRecordPerformClearTaskForReuseLocked = task2.performClearTaskForReuseLocked(this.mStartActivity, this.mLaunchFlags);
                if (reusableIntentActivity.getTask() == null) {
                    reusableIntentActivity.setTask(task2);
                }
                if (activityRecordPerformClearTaskForReuseLocked != null) {
                    if (activityRecordPerformClearTaskForReuseLocked.frontOfTask) {
                        activityRecordPerformClearTaskForReuseLocked.getTask().setIntent(this.mStartActivity);
                    }
                    deliverNewIntent(activityRecordPerformClearTaskForReuseLocked);
                }
            }
            this.mSupervisor.sendPowerHintForLaunchStartIfNeeded(false, reusableIntentActivity);
            ActivityRecord targetStackAndMoveToFrontIfNeeded = setTargetStackAndMoveToFrontIfNeeded(reusableIntentActivity);
            if (activityRecordArr != null && activityRecordArr.length > 0) {
                activityRecord3 = activityRecordArr[0];
            } else {
                activityRecord3 = null;
            }
            if (activityRecord3 != null && (activityRecord3.finishing || activityRecord3.noDisplay)) {
                activityRecordArr[0] = targetStackAndMoveToFrontIfNeeded;
            }
            if ((this.mStartFlags & 1) != 0) {
                resumeTargetStackIfNeeded();
                return 1;
            }
            if (targetStackAndMoveToFrontIfNeeded != null) {
                setTaskFromIntentActivity(targetStackAndMoveToFrontIfNeeded);
                if (!this.mAddingToTask && this.mReuseTask == null) {
                    resumeTargetStackIfNeeded();
                    if (activityRecordArr != null && activityRecordArr.length > 0) {
                        activityRecordArr[0] = targetStackAndMoveToFrontIfNeeded;
                    }
                    return this.mMovedToFront ? 2 : 3;
                }
            }
        }
        if (this.mStartActivity.packageName == null) {
            ActivityStack stack = this.mStartActivity.resultTo != null ? this.mStartActivity.resultTo.getStack() : null;
            if (stack != null) {
                stack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            }
            ActivityOptions.abort(this.mOptions);
            return -92;
        }
        ActivityStack activityStack = this.mSupervisor.mFocusedStack;
        ActivityRecord topActivity = activityStack.getTopActivity();
        ActivityRecord activityRecord4 = activityStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        if (activityRecord4 != null && this.mStartActivity.resultTo == null && activityRecord4.realActivity.equals(this.mStartActivity.realActivity) && activityRecord4.userId == this.mStartActivity.userId && activityRecord4.app != null && activityRecord4.app.thread != null && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2))) {
            activityStack.mLastPausedActivity = null;
            if (this.mDoResume) {
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
            }
            ActivityOptions.abort(this.mOptions);
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            deliverNewIntent(activityRecord4);
            this.mSupervisor.handleNonResizableTaskIfNeeded(activityRecord4.getTask(), launchWindowingMode, launchDisplayId, activityStack);
            return 3;
        }
        if (this.mLaunchTaskBehind && this.mSourceRecord != null) {
            task = this.mSourceRecord.getTask();
        } else {
            task = null;
        }
        if (this.mStartActivity.resultTo == null && this.mInTask == null && !this.mAddingToTask && (this.mLaunchFlags & 268435456) != 0) {
            taskFromInTask = setTaskFromReuseOrCreateNewTask(task, activityStack);
            z2 = true;
        } else {
            if (this.mSourceRecord != null) {
                taskFromInTask = setTaskFromSourceRecord();
            } else if (this.mInTask != null) {
                taskFromInTask = setTaskFromInTask();
            } else {
                setTaskToCurrentTopOrCreateNewTask();
                taskFromInTask = 0;
                z2 = false;
            }
            z2 = false;
        }
        if (taskFromInTask != 0) {
            return taskFromInTask;
        }
        this.mService.grantUriPermissionFromIntentLocked(this.mCallingUid, this.mStartActivity.packageName, this.mIntent, this.mStartActivity.getUriPermissionsLocked(), this.mStartActivity.userId);
        this.mService.grantEphemeralAccessLocked(this.mStartActivity.userId, this.mIntent, this.mStartActivity.appInfo.uid, UserHandle.getAppId(this.mCallingUid));
        if (z2) {
            EventLog.writeEvent(EventLogTags.AM_CREATE_TASK, Integer.valueOf(this.mStartActivity.userId), Integer.valueOf(this.mStartActivity.getTask().taskId));
        }
        ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, this.mStartActivity, this.mStartActivity.getTask());
        this.mTargetStack.mLastPausedActivity = null;
        this.mSupervisor.sendPowerHintForLaunchStartIfNeeded(false, this.mStartActivity);
        this.mTargetStack.startActivityLocked(this.mStartActivity, topActivity, z2, this.mKeepCurTransition, this.mOptions);
        if (this.mDoResume) {
            ActivityRecord activityRecord5 = this.mStartActivity.getTask().topRunningActivityLocked();
            if (!this.mTargetStack.isFocusable() || (activityRecord5 != null && activityRecord5.mTaskOverlay && this.mStartActivity != activityRecord5)) {
                this.mTargetStack.ensureActivitiesVisibleLocked(null, 0, false);
                this.mService.mWindowManager.executeAppTransition();
            } else {
                if (this.mTargetStack.isFocusable() && !this.mSupervisor.isFocusedStack(this.mTargetStack)) {
                    this.mTargetStack.moveToFront("startActivityUnchecked");
                }
                this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, this.mStartActivity, this.mOptions);
            }
        } else if (this.mStartActivity != null) {
            this.mSupervisor.mRecentTasks.add(this.mStartActivity.getTask());
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
        this.mSupervisor.handleNonResizableTaskIfNeeded(this.mStartActivity.getTask(), launchWindowingMode, launchDisplayId, this.mTargetStack);
        return 0;
    }

    void reset(boolean z) {
        this.mStartActivity = null;
        this.mIntent = null;
        this.mCallingUid = -1;
        this.mOptions = null;
        this.mLaunchTaskBehind = false;
        this.mLaunchFlags = 0;
        this.mLaunchMode = -1;
        this.mLaunchParams.reset();
        this.mNotTop = null;
        this.mDoResume = false;
        this.mStartFlags = 0;
        this.mSourceRecord = null;
        this.mPreferredDisplayId = -1;
        this.mInTask = null;
        this.mAddingToTask = false;
        this.mReuseTask = null;
        this.mNewTaskInfo = null;
        this.mNewTaskIntent = null;
        this.mSourceStack = null;
        this.mTargetStack = null;
        this.mMovedToFront = false;
        this.mNoAnimation = false;
        this.mKeepCurTransition = false;
        this.mAvoidMoveToFront = false;
        this.mVoiceSession = null;
        this.mVoiceInteractor = null;
        this.mIntentDelivered = false;
        if (z) {
            this.mRequest.reset();
        }
    }

    private void setInitialState(ActivityRecord activityRecord, ActivityOptions activityOptions, TaskRecord taskRecord, boolean z, int i, ActivityRecord activityRecord2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor) {
        ActivityRecord activityRecord3 = activityRecord2;
        reset(false);
        this.mStartActivity = activityRecord;
        this.mIntent = activityRecord.intent;
        this.mOptions = activityOptions;
        this.mCallingUid = activityRecord.launchedFromUid;
        this.mSourceRecord = activityRecord3;
        this.mVoiceSession = iVoiceInteractionSession;
        this.mVoiceInteractor = iVoiceInteractor;
        this.mPreferredDisplayId = getPreferedDisplayId(this.mSourceRecord, this.mStartActivity, activityOptions);
        this.mLaunchParams.reset();
        this.mSupervisor.getLaunchParamsController().calculate(taskRecord, null, activityRecord, activityRecord3, activityOptions, this.mLaunchParams);
        this.mLaunchMode = activityRecord.launchMode;
        this.mLaunchFlags = adjustLaunchFlagsToDocumentMode(activityRecord, 3 == this.mLaunchMode, 2 == this.mLaunchMode, this.mIntent.getFlags());
        this.mLaunchTaskBehind = (!activityRecord.mLaunchTaskBehind || isLaunchModeOneOf(2, 3) || (this.mLaunchFlags & DumpState.DUMP_FROZEN) == 0) ? false : true;
        sendNewTaskResultRequestIfNeeded();
        if ((this.mLaunchFlags & DumpState.DUMP_FROZEN) != 0 && activityRecord.resultTo == null) {
            this.mLaunchFlags |= 268435456;
        }
        if ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchTaskBehind || activityRecord.info.documentLaunchMode == 2)) {
            this.mLaunchFlags |= 134217728;
        }
        this.mSupervisor.mUserLeaving = (this.mLaunchFlags & DumpState.DUMP_DOMAIN_PREFERRED) == 0;
        if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
            Slog.v(TAG_USER_LEAVING, "startActivity() => mUserLeaving=" + this.mSupervisor.mUserLeaving);
        }
        this.mDoResume = z;
        if (!z || !activityRecord.okToShowLocked()) {
            activityRecord.delayedResume = true;
            this.mDoResume = false;
        }
        if (this.mOptions != null) {
            if (this.mOptions.getLaunchTaskId() != -1 && this.mOptions.getTaskOverlay()) {
                activityRecord.mTaskOverlay = true;
                if (!this.mOptions.canTaskOverlayResume()) {
                    TaskRecord taskRecordAnyTaskForIdLocked = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
                    ActivityRecord topActivity = taskRecordAnyTaskForIdLocked != null ? taskRecordAnyTaskForIdLocked.getTopActivity() : null;
                    if (topActivity != null && !topActivity.isState(ActivityStack.ActivityState.RESUMED)) {
                        this.mDoResume = false;
                        this.mAvoidMoveToFront = true;
                    }
                }
            } else if (this.mOptions.getAvoidMoveToFront()) {
                this.mDoResume = false;
                this.mAvoidMoveToFront = true;
            }
        }
        this.mNotTop = (this.mLaunchFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? activityRecord : null;
        this.mInTask = taskRecord;
        if (taskRecord != null && !taskRecord.inRecents) {
            Slog.w(TAG, "Starting activity in task not in recents: " + taskRecord);
            this.mInTask = null;
        }
        this.mStartFlags = i;
        if ((i & 1) != 0) {
            if (activityRecord3 == null) {
                activityRecord3 = this.mSupervisor.mFocusedStack.topRunningNonDelayedActivityLocked(this.mNotTop);
            }
            if (!activityRecord3.realActivity.equals(activityRecord.realActivity)) {
                this.mStartFlags &= -2;
            }
        }
        this.mNoAnimation = (this.mLaunchFlags & 65536) != 0;
    }

    private void sendNewTaskResultRequestIfNeeded() {
        ActivityStack stack;
        if (this.mStartActivity.resultTo != null) {
            stack = this.mStartActivity.resultTo.getStack();
        } else {
            stack = null;
        }
        if (stack != null && (this.mLaunchFlags & 268435456) != 0) {
            Slog.w(TAG, "Activity is launching as a new task, so cancelling activity result.");
            stack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            this.mStartActivity.resultTo = null;
        }
    }

    private void computeLaunchingTaskFlags() {
        if (this.mSourceRecord == null && this.mInTask != null && this.mInTask.getStack() != null) {
            Intent baseIntent = this.mInTask.getBaseIntent();
            ActivityRecord rootActivity = this.mInTask.getRootActivity();
            if (baseIntent == null) {
                ActivityOptions.abort(this.mOptions);
                throw new IllegalArgumentException("Launching into task without base intent: " + this.mInTask);
            }
            if (isLaunchModeOneOf(3, 2)) {
                if (!baseIntent.getComponent().equals(this.mStartActivity.intent.getComponent())) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Trying to launch singleInstance/Task " + this.mStartActivity + " into different task " + this.mInTask);
                }
                if (rootActivity != null) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Caller with mInTask " + this.mInTask + " has root " + rootActivity + " but target is singleInstance/Task");
                }
            }
            if (rootActivity == null) {
                this.mLaunchFlags = (baseIntent.getFlags() & 403185664) | (this.mLaunchFlags & (-403185665));
                this.mIntent.setFlags(this.mLaunchFlags);
                this.mInTask.setIntent(this.mStartActivity);
                this.mAddingToTask = true;
            } else if ((this.mLaunchFlags & 268435456) != 0) {
                this.mAddingToTask = false;
            } else {
                this.mAddingToTask = true;
            }
            this.mReuseTask = this.mInTask;
        } else {
            this.mInTask = null;
            if ((this.mStartActivity.isResolverActivity() || this.mStartActivity.noDisplay) && this.mSourceRecord != null && this.mSourceRecord.inFreeformWindowingMode()) {
                this.mAddingToTask = true;
            }
        }
        if (this.mInTask == null) {
            if (this.mSourceRecord == null) {
                if ((this.mLaunchFlags & 268435456) == 0 && this.mInTask == null) {
                    Slog.w(TAG, "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
                    this.mLaunchFlags = this.mLaunchFlags | 268435456;
                    return;
                }
                return;
            }
            if (this.mSourceRecord.launchMode == 3) {
                this.mLaunchFlags |= 268435456;
            } else if (isLaunchModeOneOf(3, 2)) {
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
            this.mSourceStack = this.mSourceRecord.getStack();
            return;
        }
        if ((this.mLaunchFlags & 268435456) == 0) {
            Slog.w(TAG, "startActivity called from finishing " + this.mSourceRecord + "; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
            this.mLaunchFlags = this.mLaunchFlags | 268435456;
            this.mNewTaskInfo = this.mSourceRecord.info;
            TaskRecord task = this.mSourceRecord.getTask();
            this.mNewTaskIntent = task != null ? task.intent : null;
        }
        this.mSourceRecord = null;
        this.mSourceStack = null;
    }

    private ActivityRecord getReusableIntentActivity() {
        boolean z = (((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchFlags & 134217728) == 0) || isLaunchModeOneOf(3, 2)) & (this.mInTask == null && this.mStartActivity.resultTo == null);
        if (this.mOptions != null && this.mOptions.getLaunchTaskId() != -1) {
            TaskRecord taskRecordAnyTaskForIdLocked = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
            if (taskRecordAnyTaskForIdLocked != null) {
                return taskRecordAnyTaskForIdLocked.getTopActivity();
            }
            return null;
        }
        if (!z) {
            return null;
        }
        if (3 == this.mLaunchMode) {
            return this.mSupervisor.findActivityLocked(this.mIntent, this.mStartActivity.info, this.mStartActivity.isActivityTypeHome());
        }
        if ((this.mLaunchFlags & 4096) != 0) {
            return this.mSupervisor.findActivityLocked(this.mIntent, this.mStartActivity.info, 2 != this.mLaunchMode);
        }
        return this.mSupervisor.findTaskLocked(this.mStartActivity, this.mPreferredDisplayId);
    }

    private int getPreferedDisplayId(ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions) {
        if (activityRecord2 != null && activityRecord2.requestedVrComponent != null) {
            return 0;
        }
        int i = this.mService.mVr2dDisplayId;
        if (i != -1) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG, "getSourceDisplayId :" + i);
            }
            return i;
        }
        int launchDisplayId = activityOptions != null ? activityOptions.getLaunchDisplayId() : -1;
        if (launchDisplayId != -1) {
            return launchDisplayId;
        }
        int displayId = activityRecord != null ? activityRecord.getDisplayId() : -1;
        if (displayId == -1) {
            return 0;
        }
        return displayId;
    }

    private ActivityRecord setTargetStackAndMoveToFrontIfNeeded(ActivityRecord activityRecord) {
        ActivityRecord activityRecord2;
        this.mTargetStack = activityRecord.getStack();
        this.mTargetStack.mLastPausedActivity = null;
        ActivityStack focusedStack = this.mSupervisor.getFocusedStack();
        if (focusedStack != null) {
            activityRecord2 = focusedStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        } else {
            activityRecord2 = null;
        }
        TaskRecord task = activityRecord2 != null ? activityRecord2.getTask() : null;
        if (task != null && ((task != activityRecord.getTask() || task != focusedStack.topTask()) && !this.mAvoidMoveToFront)) {
            this.mStartActivity.intent.addFlags(DumpState.DUMP_CHANGES);
            if (this.mSourceRecord == null || (this.mSourceStack.getTopActivity() != null && this.mSourceStack.getTopActivity().getTask() == this.mSourceRecord.getTask())) {
                if (this.mLaunchTaskBehind && this.mSourceRecord != null) {
                    activityRecord.setTaskToAffiliateWith(this.mSourceRecord.getTask());
                }
                if (!((this.mLaunchFlags & 268468224) == 268468224)) {
                    ActivityStack launchStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.getTask(), this.mOptions);
                    TaskRecord task2 = activityRecord.getTask();
                    if (launchStack == null || launchStack == this.mTargetStack) {
                        this.mTargetStack.moveTaskToFrontLocked(task2, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringingFoundTaskToFront");
                        this.mMovedToFront = true;
                    } else if (launchStack.inSplitScreenWindowingMode()) {
                        if ((this.mLaunchFlags & 4096) != 0) {
                            task2.reparent(launchStack, true, 0, true, true, "launchToSide");
                        } else {
                            this.mTargetStack.moveTaskToFrontLocked(task2, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringToFrontInsteadOfAdjacentLaunch");
                        }
                        this.mMovedToFront = launchStack != launchStack.getDisplay().getTopStackInWindowingMode(launchStack.getWindowingMode());
                    } else if (launchStack.mDisplayId != this.mTargetStack.mDisplayId) {
                        activityRecord.getTask().reparent(launchStack, true, 0, true, true, "reparentToDisplay");
                        this.mMovedToFront = true;
                    } else if (launchStack.isActivityTypeHome() && !this.mTargetStack.isActivityTypeHome()) {
                        activityRecord.getTask().reparent(launchStack, true, 0, true, true, "reparentingHome");
                        this.mMovedToFront = true;
                    }
                    this.mOptions = null;
                    activityRecord.showStartingWindow(null, false, true);
                }
            }
        }
        this.mTargetStack = activityRecord.getStack();
        if (!this.mMovedToFront && this.mDoResume) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.d(ActivityStackSupervisor.TAG_TASKS, "Bring to front target: " + this.mTargetStack + " from " + activityRecord);
            }
            this.mTargetStack.moveToFront("intentActivityFound");
        }
        this.mSupervisor.handleNonResizableTaskIfNeeded(activityRecord.getTask(), 0, 0, this.mTargetStack);
        if ((this.mLaunchFlags & DumpState.DUMP_COMPILER_STATS) != 0) {
            return this.mTargetStack.resetTaskIfNeededLocked(activityRecord, this.mStartActivity);
        }
        return activityRecord;
    }

    private void setTaskFromIntentActivity(ActivityRecord activityRecord) {
        if ((this.mLaunchFlags & 268468224) == 268468224) {
            TaskRecord task = activityRecord.getTask();
            task.performClearTaskLocked();
            this.mReuseTask = task;
            this.mReuseTask.setIntent(this.mStartActivity);
            return;
        }
        if ((this.mLaunchFlags & 67108864) != 0 || isLaunchModeOneOf(3, 2)) {
            if (activityRecord.getTask().performClearTaskLocked(this.mStartActivity, this.mLaunchFlags) == null) {
                this.mAddingToTask = true;
                this.mStartActivity.setTask(null);
                this.mSourceRecord = activityRecord;
                TaskRecord task2 = this.mSourceRecord.getTask();
                if (task2 != null && task2.getStack() == null) {
                    this.mTargetStack = computeStackFocus(this.mSourceRecord, false, this.mLaunchFlags, this.mOptions);
                    this.mTargetStack.addTask(task2, true ^ this.mLaunchTaskBehind, "startActivityUnchecked");
                    return;
                }
                return;
            }
            return;
        }
        if (this.mStartActivity.realActivity.equals(activityRecord.getTask().realActivity)) {
            if (((this.mLaunchFlags & 536870912) != 0 || 1 == this.mLaunchMode) && activityRecord.realActivity.equals(this.mStartActivity.realActivity)) {
                if (activityRecord.frontOfTask) {
                    activityRecord.getTask().setIntent(this.mStartActivity);
                }
                deliverNewIntent(activityRecord);
                return;
            } else {
                if (!activityRecord.getTask().isSameIntentFilter(this.mStartActivity)) {
                    this.mAddingToTask = true;
                    this.mSourceRecord = activityRecord;
                    return;
                }
                return;
            }
        }
        if ((this.mLaunchFlags & DumpState.DUMP_COMPILER_STATS) == 0) {
            this.mAddingToTask = true;
            this.mSourceRecord = activityRecord;
        } else if (!activityRecord.getTask().rootWasReset) {
            activityRecord.getTask().setIntent(this.mStartActivity);
        }
    }

    private void resumeTargetStackIfNeeded() {
        if (this.mDoResume) {
            this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, null, this.mOptions);
        } else {
            ActivityOptions.abort(this.mOptions);
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
    }

    private int setTaskFromReuseOrCreateNewTask(TaskRecord taskRecord, ActivityStack activityStack) {
        this.mTargetStack = computeStackFocus(this.mStartActivity, true, this.mLaunchFlags, this.mOptions);
        if (this.mReuseTask == null) {
            addOrReparentStartingActivity(this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mNewTaskInfo != null ? this.mNewTaskInfo : this.mStartActivity.info, this.mNewTaskIntent != null ? this.mNewTaskIntent : this.mIntent, this.mVoiceSession, this.mVoiceInteractor, !this.mLaunchTaskBehind, this.mStartActivity, this.mSourceRecord, this.mOptions), "setTaskFromReuseOrCreateNewTask - mReuseTask");
            updateBounds(this.mStartActivity.getTask(), this.mLaunchParams.mBounds);
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in new task " + this.mStartActivity.getTask());
            }
        } else {
            addOrReparentStartingActivity(this.mReuseTask, "setTaskFromReuseOrCreateNewTask");
        }
        if (taskRecord != null) {
            this.mStartActivity.setTaskToAffiliateWith(taskRecord);
        }
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mStartActivity.getTask())) {
            Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
            return 101;
        }
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("reuseOrNewTask");
            return 0;
        }
        return 0;
    }

    private void deliverNewIntent(ActivityRecord activityRecord) {
        if (this.mIntentDelivered) {
            return;
        }
        ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, activityRecord, activityRecord.getTask());
        activityRecord.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
        this.mIntentDelivered = true;
    }

    private int setTaskFromSourceRecord() {
        ActivityRecord activityRecordFindActivityInHistoryLocked;
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mSourceRecord.getTask())) {
            Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
            return 101;
        }
        TaskRecord task = this.mSourceRecord.getTask();
        ActivityStack stack = this.mSourceRecord.getStack();
        int i = this.mTargetStack != null ? this.mTargetStack.mDisplayId : stack.mDisplayId;
        if ((stack.topTask() == task && this.mStartActivity.canBeLaunchedOnDisplay(i)) ? false : true) {
            this.mTargetStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.getTask(), this.mOptions);
            if (this.mTargetStack == null && i != stack.mDisplayId) {
                this.mTargetStack = this.mService.mStackSupervisor.getValidLaunchStackOnDisplay(stack.mDisplayId, this.mStartActivity);
            }
            if (this.mTargetStack == null) {
                this.mTargetStack = this.mService.mStackSupervisor.getNextValidLaunchStackLocked(this.mStartActivity, -1);
            }
        }
        if (this.mTargetStack == null) {
            this.mTargetStack = stack;
        } else if (this.mTargetStack != stack) {
            task.reparent(this.mTargetStack, true, 0, false, true, "launchToSide");
        }
        if (this.mTargetStack.topTask() != task && !this.mAvoidMoveToFront) {
            this.mTargetStack.moveTaskToFrontLocked(task, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "sourceTaskToFront");
        } else if (this.mDoResume) {
            this.mTargetStack.moveToFront("sourceStackToFront");
        }
        if (!this.mAddingToTask && (this.mLaunchFlags & 67108864) != 0) {
            ActivityRecord activityRecordPerformClearTaskLocked = task.performClearTaskLocked(this.mStartActivity, this.mLaunchFlags);
            this.mKeepCurTransition = true;
            if (activityRecordPerformClearTaskLocked != null) {
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, activityRecordPerformClearTaskLocked.getTask());
                deliverNewIntent(activityRecordPerformClearTaskLocked);
                this.mTargetStack.mLastPausedActivity = null;
                if (this.mDoResume) {
                    this.mSupervisor.resumeFocusedStackTopActivityLocked();
                }
                ActivityOptions.abort(this.mOptions);
                return 3;
            }
        } else if (!this.mAddingToTask && (this.mLaunchFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0 && (activityRecordFindActivityInHistoryLocked = task.findActivityInHistoryLocked(this.mStartActivity)) != null) {
            TaskRecord task2 = activityRecordFindActivityInHistoryLocked.getTask();
            task2.moveActivityToFrontLocked(activityRecordFindActivityInHistoryLocked);
            activityRecordFindActivityInHistoryLocked.updateOptionsLocked(this.mOptions);
            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, task2);
            deliverNewIntent(activityRecordFindActivityInHistoryLocked);
            this.mTargetStack.mLastPausedActivity = null;
            if (this.mDoResume) {
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
            }
            return 3;
        }
        addOrReparentStartingActivity(task, "setTaskFromSourceRecord");
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in existing task " + this.mStartActivity.getTask() + " from source " + this.mSourceRecord);
        }
        return 0;
    }

    private int setTaskFromInTask() {
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mInTask)) {
            Slog.e(TAG, "Attempted Lock Task Mode violation mStartActivity=" + this.mStartActivity);
            return 101;
        }
        this.mTargetStack = this.mInTask.getStack();
        ActivityRecord topActivity = this.mInTask.getTopActivity();
        if (topActivity != null && topActivity.realActivity.equals(this.mStartActivity.realActivity) && topActivity.userId == this.mStartActivity.userId && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2))) {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            deliverNewIntent(topActivity);
            return 3;
        }
        if (!this.mAddingToTask) {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            ActivityOptions.abort(this.mOptions);
            return 2;
        }
        if (!this.mLaunchParams.mBounds.isEmpty()) {
            ActivityStack launchStack = this.mSupervisor.getLaunchStack(null, null, this.mInTask, true);
            if (launchStack != this.mInTask.getStack()) {
                this.mInTask.reparent(launchStack, true, 1, false, true, "inTaskToFront");
                this.mTargetStack = this.mInTask.getStack();
            }
            updateBounds(this.mInTask, this.mLaunchParams.mBounds);
        }
        this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
        addOrReparentStartingActivity(this.mInTask, "setTaskFromInTask");
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in explicit task " + this.mStartActivity.getTask());
            return 0;
        }
        return 0;
    }

    @VisibleForTesting
    void updateBounds(TaskRecord taskRecord, Rect rect) {
        if (rect.isEmpty()) {
            return;
        }
        ActivityStack stack = taskRecord.getStack();
        if (stack != null && stack.resizeStackWithLaunchBounds()) {
            this.mService.resizeStack(stack.mStackId, rect, true, false, true, -1);
        } else {
            taskRecord.updateOverrideConfiguration(rect);
        }
    }

    private void setTaskToCurrentTopOrCreateNewTask() {
        this.mTargetStack = computeStackFocus(this.mStartActivity, false, this.mLaunchFlags, this.mOptions);
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("addingToTopTask");
        }
        ActivityRecord topActivity = this.mTargetStack.getTopActivity();
        TaskRecord task = topActivity != null ? topActivity.getTask() : this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mStartActivity.info, this.mIntent, null, null, true, this.mStartActivity, this.mSourceRecord, this.mOptions);
        addOrReparentStartingActivity(task, "setTaskToCurrentTopOrCreateNewTask");
        this.mTargetStack.positionChildWindowContainerAtTop(task);
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            Slog.v(ActivityStackSupervisor.TAG_TASKS, "Starting new activity " + this.mStartActivity + " in new guessed " + this.mStartActivity.getTask());
        }
    }

    private void addOrReparentStartingActivity(TaskRecord taskRecord, String str) {
        if (this.mStartActivity.getTask() == null || this.mStartActivity.getTask() == taskRecord) {
            taskRecord.addActivityToTop(this.mStartActivity);
        } else {
            this.mStartActivity.reparent(taskRecord, taskRecord.mActivities.size(), str);
        }
    }

    private int adjustLaunchFlagsToDocumentMode(ActivityRecord activityRecord, boolean z, boolean z2, int i) {
        if ((i & DumpState.DUMP_FROZEN) != 0 && (z || z2)) {
            Slog.i(TAG, "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
            return i & (-134742017);
        }
        switch (activityRecord.info.documentLaunchMode) {
            case 0:
            default:
                return i;
            case 1:
                return i | DumpState.DUMP_FROZEN;
            case 2:
                return i | DumpState.DUMP_FROZEN;
            case 3:
                return i & (-134217729);
        }
    }

    private ActivityStack computeStackFocus(ActivityRecord activityRecord, boolean z, int i, ActivityOptions activityOptions) {
        TaskRecord task = activityRecord.getTask();
        ActivityStack launchStack = getLaunchStack(activityRecord, i, task, activityOptions);
        if (launchStack != null) {
            return launchStack;
        }
        ActivityStack stack = task != null ? task.getStack() : null;
        if (stack != null) {
            if (this.mSupervisor.mFocusedStack != stack) {
                if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    Slog.d(TAG_FOCUS, "computeStackFocus: Setting focused stack to r=" + activityRecord + " task=" + task);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG_FOCUS, "computeStackFocus: Focused stack already=" + this.mSupervisor.mFocusedStack);
            }
            return stack;
        }
        if (canLaunchIntoFocusedStack(activityRecord, z)) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG_FOCUS, "computeStackFocus: Have a focused stack=" + this.mSupervisor.mFocusedStack);
            }
            return this.mSupervisor.mFocusedStack;
        }
        if (this.mPreferredDisplayId != 0 && (launchStack = this.mSupervisor.getValidLaunchStackOnDisplay(this.mPreferredDisplayId, activityRecord)) == null) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                Slog.d(TAG_FOCUS, "computeStackFocus: Can't launch on mPreferredDisplayId=" + this.mPreferredDisplayId + ", looking on all displays.");
            }
            launchStack = this.mSupervisor.getNextValidLaunchStackLocked(activityRecord, this.mPreferredDisplayId);
        }
        if (launchStack == null) {
            ActivityDisplay defaultDisplay = this.mSupervisor.getDefaultDisplay();
            for (int childCount = defaultDisplay.getChildCount() - 1; childCount >= 0; childCount--) {
                ActivityStack childAt = defaultDisplay.getChildAt(childCount);
                if (!childAt.isOnHomeDisplay()) {
                    if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                        Slog.d(TAG_FOCUS, "computeStackFocus: Setting focused stack=" + childAt);
                    }
                    return childAt;
                }
            }
            launchStack = this.mSupervisor.getLaunchStack(activityRecord, activityOptions, task, true);
        }
        if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
            Slog.d(TAG_FOCUS, "computeStackFocus: New stack r=" + activityRecord + " stackId=" + launchStack.mStackId);
        }
        return launchStack;
    }

    private boolean canLaunchIntoFocusedStack(ActivityRecord activityRecord, boolean z) {
        boolean zSupportsSplitScreenWindowingMode;
        ActivityStack activityStack = this.mSupervisor.mFocusedStack;
        if (activityStack.isActivityTypeAssistant()) {
            zSupportsSplitScreenWindowingMode = activityRecord.isActivityTypeAssistant();
        } else {
            int windowingMode = activityStack.getWindowingMode();
            if (windowingMode != 1) {
                switch (windowingMode) {
                    case 3:
                    case 4:
                        zSupportsSplitScreenWindowingMode = activityRecord.supportsSplitScreenWindowingMode();
                        break;
                    case 5:
                        zSupportsSplitScreenWindowingMode = activityRecord.supportsFreeform();
                        break;
                    default:
                        zSupportsSplitScreenWindowingMode = !activityStack.isOnHomeDisplay() && activityRecord.canBeLaunchedOnDisplay(activityStack.mDisplayId);
                        break;
                }
            }
        }
        return zSupportsSplitScreenWindowingMode && !z && this.mPreferredDisplayId == activityStack.mDisplayId;
    }

    private ActivityStack getLaunchStack(ActivityRecord activityRecord, int i, TaskRecord taskRecord, ActivityOptions activityOptions) {
        if (this.mReuseTask != null) {
            return this.mReuseTask.getStack();
        }
        if ((i & 4096) == 0 || this.mPreferredDisplayId != 0) {
            return this.mSupervisor.getLaunchStack(activityRecord, activityOptions, taskRecord, true, this.mPreferredDisplayId != 0 ? this.mPreferredDisplayId : -1);
        }
        ActivityStack stack = taskRecord != null ? taskRecord.getStack() : this.mSupervisor.mFocusedStack;
        if (stack != this.mSupervisor.mFocusedStack) {
            return stack;
        }
        if (this.mSupervisor.mFocusedStack != null && taskRecord == this.mSupervisor.mFocusedStack.topTask()) {
            return this.mSupervisor.mFocusedStack;
        }
        if (stack != null && stack.inSplitScreenPrimaryWindowingMode()) {
            return stack.getDisplay().getOrCreateStack(4, this.mSupervisor.resolveActivityType(activityRecord, this.mOptions, taskRecord), true);
        }
        ActivityStack splitScreenPrimaryStack = this.mSupervisor.getDefaultDisplay().getSplitScreenPrimaryStack();
        if (splitScreenPrimaryStack != null && !splitScreenPrimaryStack.shouldBeVisible(activityRecord)) {
            return this.mSupervisor.getLaunchStack(activityRecord, activityOptions, taskRecord, true);
        }
        return splitScreenPrimaryStack;
    }

    private boolean isLaunchModeOneOf(int i, int i2) {
        return i == this.mLaunchMode || i2 == this.mLaunchMode;
    }

    static boolean isDocumentLaunchesIntoExisting(int i) {
        return (524288 & i) != 0 && (i & 134217728) == 0;
    }

    ActivityStarter setIntent(Intent intent) {
        this.mRequest.intent = intent;
        return this;
    }

    @VisibleForTesting
    Intent getIntent() {
        return this.mRequest.intent;
    }

    ActivityStarter setReason(String str) {
        this.mRequest.reason = str;
        return this;
    }

    ActivityStarter setCaller(IApplicationThread iApplicationThread) {
        this.mRequest.caller = iApplicationThread;
        return this;
    }

    ActivityStarter setEphemeralIntent(Intent intent) {
        this.mRequest.ephemeralIntent = intent;
        return this;
    }

    ActivityStarter setResolvedType(String str) {
        this.mRequest.resolvedType = str;
        return this;
    }

    ActivityStarter setActivityInfo(ActivityInfo activityInfo) {
        this.mRequest.activityInfo = activityInfo;
        return this;
    }

    ActivityStarter setResolveInfo(ResolveInfo resolveInfo) {
        this.mRequest.resolveInfo = resolveInfo;
        return this;
    }

    ActivityStarter setVoiceSession(IVoiceInteractionSession iVoiceInteractionSession) {
        this.mRequest.voiceSession = iVoiceInteractionSession;
        return this;
    }

    ActivityStarter setVoiceInteractor(IVoiceInteractor iVoiceInteractor) {
        this.mRequest.voiceInteractor = iVoiceInteractor;
        return this;
    }

    ActivityStarter setResultTo(IBinder iBinder) {
        this.mRequest.resultTo = iBinder;
        return this;
    }

    ActivityStarter setResultWho(String str) {
        this.mRequest.resultWho = str;
        return this;
    }

    ActivityStarter setRequestCode(int i) {
        this.mRequest.requestCode = i;
        return this;
    }

    ActivityStarter setCallingPid(int i) {
        this.mRequest.callingPid = i;
        return this;
    }

    ActivityStarter setCallingUid(int i) {
        this.mRequest.callingUid = i;
        return this;
    }

    ActivityStarter setCallingPackage(String str) {
        this.mRequest.callingPackage = str;
        return this;
    }

    ActivityStarter setRealCallingPid(int i) {
        this.mRequest.realCallingPid = i;
        return this;
    }

    ActivityStarter setRealCallingUid(int i) {
        this.mRequest.realCallingUid = i;
        return this;
    }

    ActivityStarter setStartFlags(int i) {
        this.mRequest.startFlags = i;
        return this;
    }

    ActivityStarter setActivityOptions(SafeActivityOptions safeActivityOptions) {
        this.mRequest.activityOptions = safeActivityOptions;
        return this;
    }

    ActivityStarter setActivityOptions(Bundle bundle) {
        return setActivityOptions(SafeActivityOptions.fromBundle(bundle));
    }

    ActivityStarter setIgnoreTargetSecurity(boolean z) {
        this.mRequest.ignoreTargetSecurity = z;
        return this;
    }

    ActivityStarter setFilterCallingUid(int i) {
        this.mRequest.filterCallingUid = i;
        return this;
    }

    ActivityStarter setComponentSpecified(boolean z) {
        this.mRequest.componentSpecified = z;
        return this;
    }

    ActivityStarter setOutActivity(ActivityRecord[] activityRecordArr) {
        this.mRequest.outActivity = activityRecordArr;
        return this;
    }

    ActivityStarter setInTask(TaskRecord taskRecord) {
        this.mRequest.inTask = taskRecord;
        return this;
    }

    ActivityStarter setWaitResult(WaitResult waitResult) {
        this.mRequest.waitResult = waitResult;
        return this;
    }

    ActivityStarter setProfilerInfo(ProfilerInfo profilerInfo) {
        this.mRequest.profilerInfo = profilerInfo;
        return this;
    }

    ActivityStarter setGlobalConfiguration(Configuration configuration) {
        this.mRequest.globalConfig = configuration;
        return this;
    }

    ActivityStarter setUserId(int i) {
        this.mRequest.userId = i;
        return this;
    }

    ActivityStarter setMayWait(int i) {
        this.mRequest.mayWait = true;
        this.mRequest.userId = i;
        return this;
    }

    ActivityStarter setAllowPendingRemoteAnimationRegistryLookup(boolean z) {
        this.mRequest.allowPendingRemoteAnimationRegistryLookup = z;
        return this;
    }

    void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.print(str2);
        printWriter.print("mCurrentUser=");
        printWriter.println(this.mSupervisor.mCurrentUser);
        printWriter.print(str2);
        printWriter.print("mLastStartReason=");
        printWriter.println(this.mLastStartReason);
        printWriter.print(str2);
        printWriter.print("mLastStartActivityTimeMs=");
        printWriter.println(DateFormat.getDateTimeInstance().format(new Date(this.mLastStartActivityTimeMs)));
        printWriter.print(str2);
        printWriter.print("mLastStartActivityResult=");
        printWriter.println(this.mLastStartActivityResult);
        ActivityRecord activityRecord = this.mLastStartActivityRecord[0];
        if (activityRecord != null) {
            printWriter.print(str2);
            printWriter.println("mLastStartActivityRecord:");
            activityRecord.dump(printWriter, str2 + "  ");
        }
        if (this.mStartActivity != null) {
            printWriter.print(str2);
            printWriter.println("mStartActivity:");
            this.mStartActivity.dump(printWriter, str2 + "  ");
        }
        if (this.mIntent != null) {
            printWriter.print(str2);
            printWriter.print("mIntent=");
            printWriter.println(this.mIntent);
        }
        if (this.mOptions != null) {
            printWriter.print(str2);
            printWriter.print("mOptions=");
            printWriter.println(this.mOptions);
        }
        printWriter.print(str2);
        printWriter.print("mLaunchSingleTop=");
        printWriter.print(1 == this.mLaunchMode);
        printWriter.print(" mLaunchSingleInstance=");
        printWriter.print(3 == this.mLaunchMode);
        printWriter.print(" mLaunchSingleTask=");
        printWriter.println(2 == this.mLaunchMode);
        printWriter.print(str2);
        printWriter.print("mLaunchFlags=0x");
        printWriter.print(Integer.toHexString(this.mLaunchFlags));
        printWriter.print(" mDoResume=");
        printWriter.print(this.mDoResume);
        printWriter.print(" mAddingToTask=");
        printWriter.println(this.mAddingToTask);
    }
}
