package com.android.server.am;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.am.ActivityStackSupervisor;
import com.android.server.am.ActivityStarter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ActivityStartController {
    private static final int DO_PENDING_ACTIVITY_LAUNCHES_MSG = 1;
    private static final String TAG = "ActivityManager";
    private final ActivityStarter.Factory mFactory;
    private final Handler mHandler;
    private ActivityRecord mLastHomeActivityStartRecord;
    private int mLastHomeActivityStartResult;
    private ActivityStarter mLastStarter;
    private final ArrayList<ActivityStackSupervisor.PendingActivityLaunch> mPendingActivityLaunches;
    private final PendingRemoteAnimationRegistry mPendingRemoteAnimationRegistry;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mSupervisor;
    private ActivityRecord[] tmpOutRecord;

    private final class StartHandler extends Handler {
        public StartHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                synchronized (ActivityStartController.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        ActivityStartController.this.doPendingActivityLaunches(true);
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    ActivityStartController(ActivityManagerService activityManagerService) {
        this(activityManagerService, activityManagerService.mStackSupervisor, new ActivityStarter.DefaultFactory(activityManagerService, activityManagerService.mStackSupervisor, new ActivityStartInterceptor(activityManagerService, activityManagerService.mStackSupervisor)));
    }

    @VisibleForTesting
    ActivityStartController(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor, ActivityStarter.Factory factory) {
        this.tmpOutRecord = new ActivityRecord[1];
        this.mPendingActivityLaunches = new ArrayList<>();
        this.mService = activityManagerService;
        this.mSupervisor = activityStackSupervisor;
        this.mHandler = new StartHandler(this.mService.mHandlerThread.getLooper());
        this.mFactory = factory;
        this.mFactory.setController(this);
        this.mPendingRemoteAnimationRegistry = new PendingRemoteAnimationRegistry(activityManagerService, activityManagerService.mHandler);
    }

    ActivityStarter obtainStarter(Intent intent, String str) {
        return this.mFactory.obtain().setIntent(intent).setReason(str);
    }

    void onExecutionComplete(ActivityStarter activityStarter) {
        if (this.mLastStarter == null) {
            this.mLastStarter = this.mFactory.obtain();
        }
        this.mLastStarter.set(activityStarter);
        this.mFactory.recycle(activityStarter);
    }

    void postStartActivityProcessingForLastStarter(ActivityRecord activityRecord, int i, ActivityStack activityStack) {
        if (this.mLastStarter == null) {
            return;
        }
        this.mLastStarter.postStartActivityProcessing(activityRecord, i, activityStack);
    }

    void startHomeActivity(Intent intent, ActivityInfo activityInfo, String str) {
        this.mSupervisor.moveHomeStackTaskToTop(str);
        this.mLastHomeActivityStartResult = obtainStarter(intent, "startHomeActivity: " + str).setOutActivity(this.tmpOutRecord).setCallingUid(0).setActivityInfo(activityInfo).execute();
        this.mLastHomeActivityStartRecord = this.tmpOutRecord[0];
        if (this.mSupervisor.inResumeTopActivity) {
            this.mSupervisor.scheduleResumeTopActivities();
        }
    }

    void startSetupActivity() {
        String string;
        if (this.mService.getCheckedForSetup()) {
            return;
        }
        ContentResolver contentResolver = this.mService.mContext.getContentResolver();
        if (this.mService.mFactoryTest != 1 && Settings.Global.getInt(contentResolver, "device_provisioned", 0) != 0) {
            this.mService.setCheckedForSetup(true);
            Intent intent = new Intent("android.intent.action.UPGRADE_SETUP");
            List<ResolveInfo> listQueryIntentActivities = this.mService.mContext.getPackageManager().queryIntentActivities(intent, 1049728);
            if (!listQueryIntentActivities.isEmpty()) {
                ResolveInfo resolveInfo = listQueryIntentActivities.get(0);
                if (resolveInfo.activityInfo.metaData != null) {
                    string = resolveInfo.activityInfo.metaData.getString("android.SETUP_VERSION");
                } else {
                    string = null;
                }
                if (string == null && resolveInfo.activityInfo.applicationInfo.metaData != null) {
                    string = resolveInfo.activityInfo.applicationInfo.metaData.getString("android.SETUP_VERSION");
                }
                String string2 = Settings.Secure.getString(contentResolver, "last_setup_shown");
                if (string != null && !string.equals(string2)) {
                    intent.setFlags(268435456);
                    intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    obtainStarter(intent, "startSetupActivity").setCallingUid(0).setActivityInfo(resolveInfo.activityInfo).execute();
                }
            }
        }
    }

    int checkTargetUser(int i, boolean z, int i2, int i3, String str) {
        if (z) {
            return this.mService.mUserController.handleIncomingUser(i2, i3, i, false, 2, str, null);
        }
        this.mService.mUserController.ensureNotSpecialUser(i);
        return i;
    }

    final int startActivityInPackage(int i, int i2, int i3, String str, Intent intent, String str2, IBinder iBinder, String str3, int i4, int i5, SafeActivityOptions safeActivityOptions, int i6, TaskRecord taskRecord, String str4, boolean z) {
        return obtainStarter(intent, str4).setCallingUid(i).setRealCallingPid(i2).setRealCallingUid(i3).setCallingPackage(str).setResolvedType(str2).setResultTo(iBinder).setResultWho(str3).setRequestCode(i4).setStartFlags(i5).setActivityOptions(safeActivityOptions).setMayWait(checkTargetUser(i6, z, i2, i3, str4)).setInTask(taskRecord).execute();
    }

    final int startActivitiesInPackage(int i, String str, Intent[] intentArr, String[] strArr, IBinder iBinder, SafeActivityOptions safeActivityOptions, int i2, boolean z) {
        return startActivitiesInPackage(i, 0, -10000, str, intentArr, strArr, iBinder, safeActivityOptions, i2, z);
    }

    final int startActivitiesInPackage(int i, int i2, int i3, String str, Intent[] intentArr, String[] strArr, IBinder iBinder, SafeActivityOptions safeActivityOptions, int i4, boolean z) {
        return startActivities(null, i, i2, i3, str, intentArr, strArr, iBinder, safeActivityOptions, checkTargetUser(i4, z, Binder.getCallingPid(), Binder.getCallingUid(), "startActivityInPackage"), "startActivityInPackage");
    }

    int startActivities(IApplicationThread iApplicationThread, int i, int i2, int i3, String str, Intent[] intentArr, String[] strArr, IBinder iBinder, SafeActivityOptions safeActivityOptions, int i4, String str2) throws Throwable {
        int i5;
        int i6;
        ActivityManagerService activityManagerService;
        ActivityManagerService activityManagerService2;
        Intent intent;
        int i7;
        ActivityRecord[] activityRecordArr;
        boolean z;
        int i8;
        ActivityRecord[] activityRecordArr2;
        long j;
        ActivityStartController activityStartController = this;
        long j2 = intentArr;
        String[] strArr2 = strArr;
        if (j2 == 0) {
            throw new NullPointerException("intents is null");
        }
        if (strArr2 == null) {
            throw new NullPointerException("resolvedTypes is null");
        }
        if (j2.length != strArr2.length) {
            throw new IllegalArgumentException("intents are length different than resolvedTypes");
        }
        int callingPid = i2 != 0 ? i2 : Binder.getCallingPid();
        int callingUid = i3;
        if (callingUid == -10000) {
            callingUid = Binder.getCallingUid();
        }
        try {
            try {
                if (i >= 0) {
                    i5 = i;
                } else if (iApplicationThread == null) {
                    i6 = callingPid;
                    i5 = callingUid;
                    int iComputeResolveFilterUid = ActivityStarter.computeResolveFilterUid(i5, callingUid, -10000);
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    activityManagerService = activityStartController.mService;
                    synchronized (activityManagerService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ActivityRecord[] activityRecordArr3 = new ActivityRecord[1];
                            IBinder iBinder2 = iBinder;
                            int i9 = 0;
                            boolean z2 = false;
                            Object[] objArr = j2;
                            while (i9 < objArr.length) {
                                LabeledIntent labeledIntent = objArr[i9];
                                IBinder iBinder3 = null;
                                if (labeledIntent == 0) {
                                    iBinder3 = iBinder2;
                                    activityManagerService2 = activityManagerService;
                                    j = jClearCallingIdentity;
                                    z = true;
                                    i8 = i9;
                                    activityRecordArr2 = activityRecordArr3;
                                } else {
                                    if (labeledIntent != 0 && labeledIntent.hasFileDescriptors()) {
                                        throw new IllegalArgumentException("File descriptors passed in Intent");
                                    }
                                    boolean z3 = labeledIntent.getComponent() != null;
                                    long j3 = jClearCallingIdentity;
                                    try {
                                        intent = new Intent(labeledIntent);
                                        if (z2) {
                                            intent.addFlags(268435456);
                                        }
                                        i7 = i9;
                                        activityRecordArr = activityRecordArr3;
                                        z = true;
                                        activityManagerService2 = activityManagerService;
                                    } catch (Throwable th) {
                                        th = th;
                                        activityManagerService2 = activityManagerService;
                                    }
                                    try {
                                        ActivityInfo activityInfoForUser = activityStartController.mService.getActivityInfoForUser(activityStartController.mSupervisor.resolveActivity(intent, strArr2[i9], 0, null, i4, iComputeResolveFilterUid), i4);
                                        if (activityInfoForUser != null && (activityInfoForUser.applicationInfo.privateFlags & 2) != 0) {
                                            throw new IllegalArgumentException("FLAG_CANT_SAVE_STATE not supported here");
                                        }
                                        i8 = i7;
                                        boolean z4 = i8 == objArr.length + (-1);
                                        ActivityStarter componentSpecified = activityStartController.obtainStarter(intent, str2).setCaller(iApplicationThread).setResolvedType(strArr2[i8]).setActivityInfo(activityInfoForUser).setResultTo(iBinder2).setRequestCode(-1).setCallingPid(i6).setCallingUid(i5).setCallingPackage(str).setRealCallingPid(callingPid).setRealCallingUid(callingUid).setActivityOptions(z4 ? safeActivityOptions : null).setComponentSpecified(z3);
                                        activityRecordArr2 = activityRecordArr;
                                        int iExecute = componentSpecified.setOutActivity(activityRecordArr2).setAllowPendingRemoteAnimationRegistryLookup(z4).execute();
                                        if (iExecute < 0) {
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            Binder.restoreCallingIdentity(j3);
                                            return iExecute;
                                        }
                                        j = j3;
                                        ActivityRecord activityRecord = activityRecordArr2[0];
                                        if (activityRecord == null || activityRecord.getUid() != iComputeResolveFilterUid) {
                                            z2 = true;
                                        } else {
                                            iBinder3 = activityRecord.appToken;
                                            z2 = false;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                int i10 = i8 + 1;
                                jClearCallingIdentity = j;
                                activityRecordArr3 = activityRecordArr2;
                                iBinder2 = iBinder3;
                                activityManagerService = activityManagerService2;
                                objArr = intentArr;
                                strArr2 = strArr;
                                i9 = i10;
                                activityStartController = this;
                            }
                            long j4 = jClearCallingIdentity;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(j4);
                            return 0;
                        } catch (Throwable th3) {
                            th = th3;
                            activityManagerService2 = activityManagerService;
                        }
                    }
                } else {
                    i5 = -1;
                }
                synchronized (activityManagerService) {
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Throwable th5) {
            th = th5;
            Binder.restoreCallingIdentity(j2);
            throw th;
        }
        i6 = -1;
        int iComputeResolveFilterUid2 = ActivityStarter.computeResolveFilterUid(i5, callingUid, -10000);
        long jClearCallingIdentity2 = Binder.clearCallingIdentity();
        activityManagerService = activityStartController.mService;
    }

    void schedulePendingActivityLaunches(long j) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), j);
    }

    void doPendingActivityLaunches(boolean z) {
        while (!this.mPendingActivityLaunches.isEmpty()) {
            boolean z2 = false;
            ActivityStackSupervisor.PendingActivityLaunch pendingActivityLaunchRemove = this.mPendingActivityLaunches.remove(0);
            if (z && this.mPendingActivityLaunches.isEmpty()) {
                z2 = true;
            }
            try {
                obtainStarter(null, "pendingActivityLaunch").startResolvedActivity(pendingActivityLaunchRemove.r, pendingActivityLaunchRemove.sourceRecord, null, null, pendingActivityLaunchRemove.startFlags, z2, null, null, null);
            } catch (Exception e) {
                Slog.e(TAG, "Exception during pending activity launch pal=" + pendingActivityLaunchRemove, e);
                pendingActivityLaunchRemove.sendErrorResult(e.getMessage());
            }
        }
    }

    void addPendingActivityLaunch(ActivityStackSupervisor.PendingActivityLaunch pendingActivityLaunch) {
        this.mPendingActivityLaunches.add(pendingActivityLaunch);
    }

    boolean clearPendingActivityLaunches(String str) {
        int size = this.mPendingActivityLaunches.size();
        for (int i = size - 1; i >= 0; i--) {
            ActivityRecord activityRecord = this.mPendingActivityLaunches.get(i).r;
            if (activityRecord != null && activityRecord.packageName.equals(str)) {
                this.mPendingActivityLaunches.remove(i);
            }
        }
        return this.mPendingActivityLaunches.size() < size;
    }

    void registerRemoteAnimationForNextActivityStart(String str, RemoteAnimationAdapter remoteAnimationAdapter) {
        this.mPendingRemoteAnimationRegistry.addPendingAnimation(str, remoteAnimationAdapter);
    }

    PendingRemoteAnimationRegistry getPendingRemoteAnimationRegistry() {
        return this.mPendingRemoteAnimationRegistry;
    }

    void dump(PrintWriter printWriter, String str, String str2) {
        printWriter.print(str);
        printWriter.print("mLastHomeActivityStartResult=");
        printWriter.println(this.mLastHomeActivityStartResult);
        if (this.mLastHomeActivityStartRecord != null) {
            printWriter.print(str);
            printWriter.println("mLastHomeActivityStartRecord:");
            this.mLastHomeActivityStartRecord.dump(printWriter, str + "  ");
        }
        boolean z = false;
        boolean z2 = str2 != null;
        if (this.mLastStarter != null) {
            if (!z2 || this.mLastStarter.relatedToPackage(str2) || (this.mLastHomeActivityStartRecord != null && str2.equals(this.mLastHomeActivityStartRecord.packageName))) {
                z = true;
            }
            if (z) {
                printWriter.print(str);
                this.mLastStarter.dump(printWriter, str + "  ");
                if (z2) {
                    return;
                }
            }
        }
        if (z2) {
            printWriter.print(str);
            printWriter.println("(nothing)");
        }
    }
}
