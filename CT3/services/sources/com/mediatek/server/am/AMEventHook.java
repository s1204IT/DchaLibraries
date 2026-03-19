package com.mediatek.server.am;

import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerService;
import com.mediatek.aal.AalUtils;
import com.mediatek.aee.ExceptionLog;
import com.mediatek.alarm.PowerOffAlarmUtility;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.apm.frc.FocusRelationshipChainPolicy;
import com.mediatek.apm.suppression.SuppressionAction;
import com.mediatek.appworkingset.AWSManager;
import com.mediatek.ipomanager.ActivityManagerPlus;
import com.mediatek.perfservice.PerfServiceWrapper;
import com.mediatek.runningbooster.RunningBoosterService;
import com.mediatek.stk.IdleScreen;
import java.util.ArrayList;

public final class AMEventHook {

    private static final int[] f12commediatekserveramAMEventHook$EventSwitchesValues = null;
    private static final boolean IS_USER_BUILD;
    private static final String TAG = "AMEventHook";
    private static boolean DEBUG = false;
    private static boolean DEBUG_FLOW = false;
    private static boolean DEBUG_EVENT_DETAIL = false;
    private static final boolean IS_USER_DEBUG_BUILD = "userdebug".equals(Build.TYPE);
    private ExceptionLog exceptionLog = null;
    private PowerOffAlarmUtility mPowerOffAlarmUtility = null;
    private IdleScreen mIdleScreen = null;
    private PerfServiceWrapper mPerfService = null;
    private ActivityManagerPlus mActivityManagerPlus = null;
    FocusRelationshipChainPolicy frcPolicy = null;
    SuppressionAction suppressionAction = null;
    RunningBoosterService runningBoosterService = null;
    AWSManager mAWSManager = null;

    private static int[] m3463getcommediatekserveramAMEventHook$EventSwitchesValues() {
        if (f12commediatekserveramAMEventHook$EventSwitchesValues != null) {
            return f12commediatekserveramAMEventHook$EventSwitchesValues;
        }
        int[] iArr = new int[Event.valuesCustom().length];
        try {
            iArr[Event.AM_ActivityThreadResumedDone.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Event.AM_AfterActivityDestroyed.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Event.AM_AfterActivityPaused.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Event.AM_AfterActivityResumed.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Event.AM_AfterActivityStopped.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Event.AM_AfterPostEnableScreenAfterBoot.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Event.AM_BeforeActivitySwitch.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Event.AM_BeforeGoHomeWhenNoActivities.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Event.AM_BeforeSendBootCompleted.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[Event.AM_BeforeSendBroadcast.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[Event.AM_BeforeShowAppErrorDialog.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[Event.AM_EndOfAMSCtor.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[Event.AM_EndOfActivityIdle.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[Event.AM_EndOfErrorDumpThread.ordinal()] = 14;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[Event.AM_PackageStoppedStatusChanged.ordinal()] = 15;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[Event.AM_ReadyToGetProvider.ordinal()] = 16;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[Event.AM_ReadyToStartComponent.ordinal()] = 17;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[Event.AM_ReadyToStartDynamicReceiver.ordinal()] = 18;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[Event.AM_ReadyToStartService.ordinal()] = 19;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[Event.AM_ReadyToStartStaticReceiver.ordinal()] = 20;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[Event.AM_SkipStartActivity.ordinal()] = 21;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[Event.AM_StartProcessForActivity.ordinal()] = 22;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[Event.AM_SystemReady.ordinal()] = 23;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[Event.AM_SystemUserUnlock.ordinal()] = 24;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[Event.AM_UpdateSleep.ordinal()] = 25;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[Event.AM_WakefulnessChanged.ordinal()] = 26;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[Event.AM_WindowsVisible.ordinal()] = 27;
        } catch (NoSuchFieldError e27) {
        }
        f12commediatekserveramAMEventHook$EventSwitchesValues = iArr;
        return iArr;
    }

    static {
        IS_USER_BUILD = !"user".equals(Build.TYPE) ? IS_USER_DEBUG_BUILD : true;
    }

    public static AMEventHook createInstance() {
        AMEventHook aMEventHook;
        synchronized (AMEventHook.class) {
            aMEventHook = new AMEventHook();
        }
        return aMEventHook;
    }

    public static void setDebug(boolean on) {
        DEBUG = on;
        DEBUG_FLOW = on;
    }

    public static void setEventDetailDebug(boolean on) {
        DEBUG = on;
        DEBUG_FLOW = on;
        DEBUG_EVENT_DETAIL = on;
    }

    private void showLogForBeforeActivitySwitch(AMEventHookData.BeforeActivitySwitch data) {
        String lastResumedActivityName = data.getString(AMEventHookData.BeforeActivitySwitch.Index.lastResumedActivityName);
        String nextResumedActivityName = data.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedActivityName);
        String lastResumedPackageName = data.getString(AMEventHookData.BeforeActivitySwitch.Index.lastResumedPackageName);
        String nextResumedPackageName = data.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedPackageName);
        int lastResumedActivityType = data.getInt(AMEventHookData.BeforeActivitySwitch.Index.lastResumedActivityType);
        int nextResumedActivityType = data.getInt(AMEventHookData.BeforeActivitySwitch.Index.nextResumedActivityType);
        boolean isNeedToPauseActivityFirst = data.getBoolean(AMEventHookData.BeforeActivitySwitch.Index.isNeedToPauseActivityFirst);
        ArrayList<String> nextTaskPackageList = (ArrayList) data.get(AMEventHookData.BeforeActivitySwitch.Index.nextTaskPackageList);
        Slog.v(TAG, "onBeforeActivitySwitch, from: (" + lastResumedPackageName + ", " + lastResumedActivityName + ", " + lastResumedActivityType + "), to: (" + nextResumedPackageName + ", " + nextResumedActivityName + ", " + nextResumedActivityType + "), isNeedToPauseActivityFirst: " + isNeedToPauseActivityFirst);
        if (nextTaskPackageList != null) {
            for (int i = 0; i < nextTaskPackageList.size(); i++) {
                Slog.v(TAG, "onBeforeActivitySwitch, nextTaskPackageList[" + i + "] = " + nextTaskPackageList.get(i));
            }
        }
    }

    private void showLogForWakefulnessChanged(AMEventHookData.WakefulnessChanged data) {
        int wakefulness = data.getInt(AMEventHookData.WakefulnessChanged.Index.wakefulness);
        Slog.v(TAG, "onWakefulnessChanged, wakefulness: " + wakefulness);
    }

    private void showLogForAfterActivityResumed(AMEventHookData.AfterActivityResumed data) {
        int pid = data.getInt(AMEventHookData.AfterActivityResumed.Index.pid);
        String activityName = data.getString(AMEventHookData.AfterActivityResumed.Index.activityName);
        String pkgName = data.getString(AMEventHookData.AfterActivityResumed.Index.packageName);
        int activityType = data.getInt(AMEventHookData.AfterActivityResumed.Index.activityType);
        Slog.v(TAG, "onAfterActivityResumed, Activity:(" + pid + ", " + pkgName + " " + activityName + ", " + activityType + ")");
    }

    private void showLogForActivityThreadResumedDone(AMEventHookData.ActivityThreadResumedDone data) {
        String pkgName = data.getString(AMEventHookData.ActivityThreadResumedDone.Index.packageName);
        Slog.v(TAG, "onActivityThreadResumedDone, Activity package: " + pkgName);
    }

    private void showLogForSystemUserUnlock(AMEventHookData.SystemUserUnlock data) {
        int uid = data.getInt(AMEventHookData.SystemUserUnlock.Index.uid);
        Slog.v(TAG, "onSystemUserUnlock, uid is " + uid);
    }

    private void showStartProcessForActivity(AMEventHookData.StartProcessForActivity data) {
        String reason = data.getString(AMEventHookData.StartProcessForActivity.Index.reason);
        String packageName = data.getString(AMEventHookData.StartProcessForActivity.Index.packageName);
        Slog.v(TAG, "onStartProcessForActivity, reason is " + reason + " package: " + packageName);
    }

    boolean isDebuggableMessage(Event event) {
        switch (m3463getcommediatekserveramAMEventHook$EventSwitchesValues()[event.ordinal()]) {
            case 1:
                break;
            case 4:
                break;
            case 15:
                break;
            case 16:
                break;
            case 17:
                break;
            case 18:
                break;
            case 19:
                break;
            case 20:
                break;
            case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                break;
            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                break;
        }
        return false;
    }

    public AMEventHookResult hook(Event event, Object data) {
        return onEvent(event, data);
    }

    public enum Event {
        AM_EndOfAMSCtor,
        AM_EndOfErrorDumpThread,
        AM_BeforeSendBootCompleted,
        AM_SystemReady,
        AM_AfterPostEnableScreenAfterBoot,
        AM_SkipStartActivity,
        AM_BeforeGoHomeWhenNoActivities,
        AM_EndOfActivityIdle,
        AM_BeforeShowAppErrorDialog,
        AM_BeforeSendBroadcast,
        AM_BeforeActivitySwitch,
        AM_AfterActivityResumed,
        AM_AfterActivityPaused,
        AM_AfterActivityStopped,
        AM_AfterActivityDestroyed,
        AM_WindowsVisible,
        AM_WakefulnessChanged,
        AM_ReadyToStartService,
        AM_ReadyToGetProvider,
        AM_ReadyToStartDynamicReceiver,
        AM_ReadyToStartStaticReceiver,
        AM_ReadyToStartComponent,
        AM_PackageStoppedStatusChanged,
        AM_ActivityThreadResumedDone,
        AM_SystemUserUnlock,
        AM_UpdateSleep,
        AM_StartProcessForActivity;

        public static Event[] valuesCustom() {
            return values();
        }
    }

    public AMEventHook() {
        if (DEBUG_FLOW) {
            Slog.d(TAG, "AMEventHook()", new Throwable());
        } else {
            if (!DEBUG) {
                return;
            }
            Slog.d(TAG, "AMEventHook()");
        }
    }

    private AMEventHookResult onEvent(Event event, Object data) {
        if (DEBUG_FLOW) {
            Slog.d(TAG, "onEvent: " + event, new Throwable());
        } else if (DEBUG || (!IS_USER_BUILD && isDebuggableMessage(event))) {
            Slog.d(TAG, "onEvent: " + event);
        }
        AMEventHookResult result = null;
        switch (m3463getcommediatekserveramAMEventHook$EventSwitchesValues()[event.ordinal()]) {
            case 1:
                result = onActivityThreadResumedDone((AMEventHookData.ActivityThreadResumedDone) data);
                break;
            case 2:
                result = onAfterActivityDestroyed((AMEventHookData.AfterActivityDestroyed) data);
                break;
            case 3:
                result = onAfterActivityPaused((AMEventHookData.AfterActivityPaused) data);
                break;
            case 4:
                result = onAfterActivityResumed((AMEventHookData.AfterActivityResumed) data);
                break;
            case 5:
                result = onAfterActivityStopped((AMEventHookData.AfterActivityStopped) data);
                break;
            case 6:
                result = onAfterPostEnableScreenAfterBoot((AMEventHookData.AfterPostEnableScreenAfterBoot) data);
                break;
            case 7:
                result = onBeforeActivitySwitch((AMEventHookData.BeforeActivitySwitch) data);
                break;
            case 8:
                result = onBeforeGoHomeWhenNoActivities((AMEventHookData.BeforeGoHomeWhenNoActivities) data);
                break;
            case 9:
                result = onBeforeSendBootCompleted((AMEventHookData.BeforeSendBootCompleted) data);
                break;
            case 10:
                result = onBeforeSendBroadcast((AMEventHookData.BeforeSendBroadcast) data);
                break;
            case 11:
                result = onBeforeShowAppErrorDialog((AMEventHookData.BeforeShowAppErrorDialog) data);
                break;
            case 12:
                result = onEndOfAMSCtor((AMEventHookData.EndOfAMSCtor) data);
                break;
            case 13:
                result = onEndOfActivityIdle((AMEventHookData.EndOfActivityIdle) data);
                break;
            case 14:
                result = onEndOfErrorDumpThread((AMEventHookData.EndOfErrorDumpThread) data);
                break;
            case 15:
                result = onPackageStoppedStatusChanged((AMEventHookData.PackageStoppedStatusChanged) data);
                break;
            case 16:
                result = onReadyToGetProvider((AMEventHookData.ReadyToGetProvider) data);
                break;
            case 17:
                result = onReadyToStartComponent((AMEventHookData.ReadyToStartComponent) data);
                break;
            case 18:
                result = onReadyToStartDynamicReceiver((AMEventHookData.ReadyToStartDynamicReceiver) data);
                break;
            case 19:
                result = onReadyToStartService((AMEventHookData.ReadyToStartService) data);
                break;
            case 20:
                result = onReadyToStartStaticReceiver((AMEventHookData.ReadyToStartStaticReceiver) data);
                break;
            case WindowManagerService.H.DRAG_END_TIMEOUT:
                result = onSkipStartActivity((AMEventHookData.SkipStartActivity) data);
                break;
            case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                result = onStartProcessForActivity((AMEventHookData.StartProcessForActivity) data);
                break;
            case WindowManagerService.H.BOOT_TIMEOUT:
                result = onSystemReady((AMEventHookData.SystemReady) data);
                break;
            case WindowManagerService.H.WAITING_FOR_DRAWN_TIMEOUT:
                result = onSystemUserUnlock((AMEventHookData.SystemUserUnlock) data);
                break;
            case 25:
                result = onUpdateSleep((AMEventHookData.UpdateSleep) data);
                break;
            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                result = onWakefulnessChanged((AMEventHookData.WakefulnessChanged) data);
                break;
            case WindowManagerService.H.DO_DISPLAY_ADDED:
                result = onWindowsVisible((AMEventHookData.WindowsVisible) data);
                break;
            default:
                Slog.w(TAG, "Unknown event: " + event);
                break;
        }
        if (DEBUG) {
            Slog.d(TAG, "onEvent result: " + result);
        }
        return result;
    }

    private AMEventHookResult onEndOfAMSCtor(AMEventHookData.EndOfAMSCtor data) {
        if (this.exceptionLog == null && SystemProperties.get("ro.have_aee_feature").equals("1")) {
            this.exceptionLog = new ExceptionLog();
        }
        return null;
    }

    private AMEventHookResult onEndOfErrorDumpThread(AMEventHookData.EndOfErrorDumpThread data) {
        if (this.exceptionLog != null) {
            this.exceptionLog.onEndOfErrorDumpThread(data);
        }
        return null;
    }

    private AMEventHookResult onBeforeSendBootCompleted(AMEventHookData.BeforeSendBootCompleted data) {
        AMEventHookResult result = new AMEventHookResult();
        if (PowerOffAlarmUtility.isAlarmBoot()) {
            result.addAction(AMEventHookAction.AM_Interrupt);
        }
        return result;
    }

    private AMEventHookResult onSystemReady(AMEventHookData.SystemReady data) {
        AMEventHookResult result = new AMEventHookResult();
        if (this.mPowerOffAlarmUtility == null) {
            this.mPowerOffAlarmUtility = PowerOffAlarmUtility.getInstance(data);
        }
        this.mPowerOffAlarmUtility.onSystemReady(data, result);
        if (this.mActivityManagerPlus == null) {
            this.mActivityManagerPlus = ActivityManagerPlus.getInstance(data);
        }
        if (this.mIdleScreen == null) {
            this.mIdleScreen = new IdleScreen();
        }
        this.mIdleScreen.onSystemReady(data);
        if (this.mPerfService == null) {
            this.mPerfService = new PerfServiceWrapper((Context) null);
        }
        if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
            if (this.frcPolicy == null) {
                this.frcPolicy = FocusRelationshipChainPolicy.getInstance();
            }
            if (this.suppressionAction == null) {
                this.suppressionAction = SuppressionAction.getInstance((Context) data.get(AMEventHookData.SystemReady.Index.context));
            }
        }
        if ("1".equals(SystemProperties.get("persist.runningbooster.support"))) {
            this.runningBoosterService = RunningBoosterService.getInstance(data);
        }
        if (SystemProperties.get("ro.mtk_aws_support").equals("1") && this.mAWSManager == null) {
            this.mAWSManager = AWSManager.getInstance(data);
        }
        return result;
    }

    private AMEventHookResult onAfterPostEnableScreenAfterBoot(AMEventHookData.AfterPostEnableScreenAfterBoot data) {
        AMEventHookResult result = new AMEventHookResult();
        if (this.mPowerOffAlarmUtility != null) {
            this.mPowerOffAlarmUtility.onAfterPostEnableScreenAfterBoot(data, result);
        }
        return result;
    }

    private AMEventHookResult onSkipStartActivity(AMEventHookData.SkipStartActivity data) {
        AMEventHookResult result = new AMEventHookResult();
        if (PowerOffAlarmUtility.isAlarmBoot()) {
            Slog.d(TAG, "Skip by alarm boot");
            result.addAction(AMEventHookAction.AM_SkipStartActivity);
        }
        return result;
    }

    private AMEventHookResult onBeforeGoHomeWhenNoActivities(AMEventHookData.BeforeGoHomeWhenNoActivities data) {
        AMEventHookResult result = new AMEventHookResult();
        if (PowerOffAlarmUtility.isAlarmBoot()) {
            Slog.v(TAG, "Skip to resume home activity!!");
            result.addAction(AMEventHookAction.AM_SkipHomeActivityLaunching);
        }
        return result;
    }

    private AMEventHookResult onEndOfActivityIdle(AMEventHookData.EndOfActivityIdle data) {
        if (this.mIdleScreen != null) {
            this.mIdleScreen.onEndOfActivityIdle(data);
        }
        return null;
    }

    private AMEventHookResult onBeforeShowAppErrorDialog(AMEventHookData.BeforeShowAppErrorDialog data) {
        AMEventHookResult result = new AMEventHookResult();
        PackageManagerInternal pkgMgrInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        pkgMgrInternal.initMtkPermErrorDialog(data, result);
        return result;
    }

    private AMEventHookResult onBeforeSendBroadcast(AMEventHookData.BeforeSendBroadcast data) {
        AMEventHookResult result = new AMEventHookResult();
        if (this.mActivityManagerPlus != null) {
            result = this.mActivityManagerPlus.filterBroadcast(data, result);
        }
        if (("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) && this.suppressionAction != null) {
            return this.suppressionAction.onBeforeSendBroadcast(data, result);
        }
        return result;
    }

    private AMEventHookResult onBeforeActivitySwitch(AMEventHookData.BeforeActivitySwitch data) {
        if (DEBUG_EVENT_DETAIL) {
            showLogForBeforeActivitySwitch(data);
        }
        if (this.mPerfService != null) {
            this.mPerfService.amsBoostResume(data);
        }
        if (this.runningBoosterService != null) {
            this.runningBoosterService.onBeforeActivitySwitch(data);
        }
        if (this.mAWSManager != null) {
            this.mAWSManager.onBeforeActivitySwitch(data);
        }
        if (this.frcPolicy != null) {
            this.frcPolicy.onStartActivity(data);
        }
        return null;
    }

    private AMEventHookResult onAfterActivityResumed(AMEventHookData.AfterActivityResumed data) {
        if (DEBUG_EVENT_DETAIL) {
            showLogForAfterActivityResumed(data);
        }
        if (this.mPerfService != null) {
            this.mPerfService.onAfterActivityResumed(data);
        }
        if (this.runningBoosterService != null) {
            this.runningBoosterService.onAfterActivityResumed(data);
        }
        if (AalUtils.isSupported()) {
            AalUtils.getInstance(true).onAfterActivityResumed(data);
        }
        return null;
    }

    private AMEventHookResult onAfterActivityPaused(AMEventHookData.AfterActivityPaused data) {
        if (this.mPerfService != null) {
            this.mPerfService.onAfterActivityPaused(data);
        }
        return null;
    }

    private AMEventHookResult onAfterActivityStopped(AMEventHookData.AfterActivityStopped data) {
        if (this.mPerfService != null) {
            this.mPerfService.onAfterActivityStopped(data);
        }
        return null;
    }

    private AMEventHookResult onAfterActivityDestroyed(AMEventHookData.AfterActivityDestroyed data) {
        if (this.mPerfService != null) {
            this.mPerfService.onAfterActivityDestroyed(data);
        }
        return null;
    }

    private AMEventHookResult onWindowsVisible(AMEventHookData.WindowsVisible data) {
        if (this.mPerfService != null) {
            this.mPerfService.amsBoostStop();
        }
        return null;
    }

    private AMEventHookResult onWakefulnessChanged(AMEventHookData.WakefulnessChanged data) {
        if (DEBUG_EVENT_DETAIL) {
            showLogForWakefulnessChanged(data);
        }
        if (this.runningBoosterService != null) {
            this.runningBoosterService.onWakefulnessChanged(data);
        }
        return null;
    }

    private AMEventHookResult onReadyToStartService(AMEventHookData.ReadyToStartService data) {
        if (this.frcPolicy != null) {
            this.frcPolicy.onStartService(data);
        }
        return null;
    }

    private AMEventHookResult onReadyToGetProvider(AMEventHookData.ReadyToGetProvider data) {
        if (this.frcPolicy != null) {
            this.frcPolicy.onStartProvider(data);
        }
        return null;
    }

    private AMEventHookResult onReadyToStartDynamicReceiver(AMEventHookData.ReadyToStartDynamicReceiver data) {
        if (this.frcPolicy != null) {
            this.frcPolicy.onStartDynamicReceiver(data);
        }
        return null;
    }

    private AMEventHookResult onReadyToStartStaticReceiver(AMEventHookData.ReadyToStartStaticReceiver data) {
        if (this.frcPolicy != null) {
            this.frcPolicy.onStartStaticReceiver(data);
        }
        return null;
    }

    private AMEventHookResult onReadyToStartComponent(AMEventHookData.ReadyToStartComponent data) {
        if (("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) && this.suppressionAction != null) {
            this.suppressionAction.onReadyToStartComponent(data);
        }
        return null;
    }

    private AMEventHookResult onPackageStoppedStatusChanged(AMEventHookData.PackageStoppedStatusChanged data) {
        if (("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) && this.suppressionAction != null) {
            this.suppressionAction.onPackageStoppedStatusChanged(data);
        }
        return null;
    }

    private AMEventHookResult onActivityThreadResumedDone(AMEventHookData.ActivityThreadResumedDone data) {
        if (DEBUG_EVENT_DETAIL) {
            showLogForActivityThreadResumedDone(data);
        }
        if (this.runningBoosterService != null) {
            this.runningBoosterService.onActivityThreadResumedDone(data);
        }
        return null;
    }

    private AMEventHookResult onSystemUserUnlock(AMEventHookData.SystemUserUnlock data) {
        if (DEBUG_EVENT_DETAIL) {
            showLogForSystemUserUnlock(data);
        }
        if (this.runningBoosterService != null) {
            this.runningBoosterService.onSystemUserUnlock(data);
        }
        return null;
    }

    private AMEventHookResult onUpdateSleep(AMEventHookData.UpdateSleep data) {
        if (AalUtils.isSupported()) {
            AalUtils.getInstance(true).onUpdateSleep(data);
        }
        return null;
    }

    private AMEventHookResult onStartProcessForActivity(AMEventHookData.StartProcessForActivity data) {
        if (DEBUG_EVENT_DETAIL) {
            showStartProcessForActivity(data);
        }
        if (this.mPerfService != null) {
            this.mPerfService.amsBoostProcessCreate(data);
        }
        return null;
    }
}
