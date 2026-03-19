package com.android.server.am;

import android.util.EventLog;

public class EventLogTags {
    public static final int AM_ACTIVITY_FULLY_DRAWN_TIME = 30042;
    public static final int AM_ACTIVITY_LAUNCH_TIME = 30009;
    public static final int AM_ANR = 30008;
    public static final int AM_BROADCAST_DISCARD_APP = 30025;
    public static final int AM_BROADCAST_DISCARD_FILTER = 30024;
    public static final int AM_CRASH = 30039;
    public static final int AM_CREATE_ACTIVITY = 30005;
    public static final int AM_CREATE_SERVICE = 30030;
    public static final int AM_CREATE_TASK = 30004;
    public static final int AM_DESTROY_ACTIVITY = 30018;
    public static final int AM_DESTROY_SERVICE = 30031;
    public static final int AM_DROP_PROCESS = 30033;
    public static final int AM_FAILED_TO_PAUSE = 30012;
    public static final int AM_FINISH_ACTIVITY = 30001;
    public static final int AM_FOCUSED_STACK = 30044;
    public static final int AM_KILL = 30023;
    public static final int AM_LOW_MEMORY = 30017;
    public static final int AM_MEMINFO = 30046;
    public static final int AM_MEM_FACTOR = 30050;
    public static final int AM_NEW_INTENT = 30003;
    public static final int AM_ON_ACTIVITY_RESULT_CALLED = 30062;
    public static final int AM_ON_CREATE_CALLED = 30057;
    public static final int AM_ON_DESTROY_CALLED = 30060;
    public static final int AM_ON_PAUSED_CALLED = 30021;
    public static final int AM_ON_RESTART_CALLED = 30058;
    public static final int AM_ON_RESUME_CALLED = 30022;
    public static final int AM_ON_START_CALLED = 30059;
    public static final int AM_ON_STOP_CALLED = 30049;
    public static final int AM_PAUSE_ACTIVITY = 30013;
    public static final int AM_PRE_BOOT = 30045;
    public static final int AM_PROCESS_CRASHED_TOO_MUCH = 30032;
    public static final int AM_PROCESS_START_TIMEOUT = 30037;
    public static final int AM_PROC_BAD = 30015;
    public static final int AM_PROC_BOUND = 30010;
    public static final int AM_PROC_DIED = 30011;
    public static final int AM_PROC_GOOD = 30016;
    public static final int AM_PROC_START = 30014;
    public static final int AM_PROVIDER_LOST_PROCESS = 30036;
    public static final int AM_PSS = 30047;
    public static final int AM_RELAUNCH_ACTIVITY = 30020;
    public static final int AM_RELAUNCH_RESUME_ACTIVITY = 30019;
    public static final int AM_REMOVE_TASK = 30061;
    public static final int AM_RESTART_ACTIVITY = 30006;
    public static final int AM_RESUME_ACTIVITY = 30007;
    public static final int AM_SCHEDULE_SERVICE_RESTART = 30035;
    public static final int AM_SERVICE_CRASHED_TOO_MUCH = 30034;
    public static final int AM_SET_RESUMED_ACTIVITY = 30043;
    public static final int AM_STOP_ACTIVITY = 30048;
    public static final int AM_STOP_IDLE_SERVICE = 30056;
    public static final int AM_SWITCH_USER = 30041;
    public static final int AM_TASK_TO_FRONT = 30002;
    public static final int AM_UID_ACTIVE = 30054;
    public static final int AM_UID_IDLE = 30055;
    public static final int AM_UID_RUNNING = 30052;
    public static final int AM_UID_STOPPED = 30053;
    public static final int AM_USER_STATE_CHANGED = 30051;
    public static final int AM_WTF = 30040;
    public static final int BOOT_PROGRESS_AMS_READY = 3040;
    public static final int BOOT_PROGRESS_ENABLE_SCREEN = 3050;
    public static final int CONFIGURATION_CHANGED = 2719;
    public static final int CPU = 2721;

    private EventLogTags() {
    }

    public static void writeConfigurationChanged(int i) {
        EventLog.writeEvent(CONFIGURATION_CHANGED, i);
    }

    public static void writeCpu(int i, int i2, int i3, int i4, int i5, int i6) {
        EventLog.writeEvent(CPU, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6));
    }

    public static void writeBootProgressAmsReady(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_AMS_READY, j);
    }

    public static void writeBootProgressEnableScreen(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_ENABLE_SCREEN, j);
    }

    public static void writeAmFinishActivity(int i, int i2, int i3, String str, String str2) {
        EventLog.writeEvent(AM_FINISH_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, str2);
    }

    public static void writeAmTaskToFront(int i, int i2) {
        EventLog.writeEvent(AM_TASK_TO_FRONT, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeAmNewIntent(int i, int i2, int i3, String str, String str2, String str3, String str4, int i4) {
        EventLog.writeEvent(AM_NEW_INTENT, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, str2, str3, str4, Integer.valueOf(i4));
    }

    public static void writeAmCreateTask(int i, int i2) {
        EventLog.writeEvent(AM_CREATE_TASK, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeAmCreateActivity(int i, int i2, int i3, String str, String str2, String str3, String str4, int i4) {
        EventLog.writeEvent(AM_CREATE_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, str2, str3, str4, Integer.valueOf(i4));
    }

    public static void writeAmRestartActivity(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_RESTART_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmResumeActivity(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_RESUME_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmAnr(int i, int i2, String str, int i3, String str2) {
        EventLog.writeEvent(AM_ANR, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2);
    }

    public static void writeAmActivityLaunchTime(int i, int i2, String str, long j) {
        EventLog.writeEvent(AM_ACTIVITY_LAUNCH_TIME, Integer.valueOf(i), Integer.valueOf(i2), str, Long.valueOf(j));
    }

    public static void writeAmProcBound(int i, int i2, String str) {
        EventLog.writeEvent(AM_PROC_BOUND, Integer.valueOf(i), Integer.valueOf(i2), str);
    }

    public static void writeAmProcDied(int i, int i2, String str, int i3, int i4) {
        EventLog.writeEvent(AM_PROC_DIED, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), Integer.valueOf(i4));
    }

    public static void writeAmFailedToPause(int i, int i2, String str, String str2) {
        EventLog.writeEvent(AM_FAILED_TO_PAUSE, Integer.valueOf(i), Integer.valueOf(i2), str, str2);
    }

    public static void writeAmPauseActivity(int i, int i2, String str, String str2) {
        EventLog.writeEvent(AM_PAUSE_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), str, str2);
    }

    public static void writeAmProcStart(int i, int i2, int i3, String str, String str2, String str3) {
        EventLog.writeEvent(AM_PROC_START, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, str2, str3);
    }

    public static void writeAmProcBad(int i, int i2, String str) {
        EventLog.writeEvent(AM_PROC_BAD, Integer.valueOf(i), Integer.valueOf(i2), str);
    }

    public static void writeAmProcGood(int i, int i2, String str) {
        EventLog.writeEvent(AM_PROC_GOOD, Integer.valueOf(i), Integer.valueOf(i2), str);
    }

    public static void writeAmLowMemory(int i) {
        EventLog.writeEvent(AM_LOW_MEMORY, i);
    }

    public static void writeAmDestroyActivity(int i, int i2, int i3, String str, String str2) {
        EventLog.writeEvent(AM_DESTROY_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, str2);
    }

    public static void writeAmRelaunchResumeActivity(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_RELAUNCH_RESUME_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmRelaunchActivity(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_RELAUNCH_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmOnPausedCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_PAUSED_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmOnResumeCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_RESUME_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmKill(int i, int i2, String str, int i3, String str2) {
        EventLog.writeEvent(AM_KILL, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2);
    }

    public static void writeAmBroadcastDiscardFilter(int i, int i2, String str, int i3, int i4) {
        EventLog.writeEvent(AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), Integer.valueOf(i4));
    }

    public static void writeAmBroadcastDiscardApp(int i, int i2, String str, int i3, String str2) {
        EventLog.writeEvent(AM_BROADCAST_DISCARD_APP, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2);
    }

    public static void writeAmCreateService(int i, int i2, String str, int i3, int i4) {
        EventLog.writeEvent(AM_CREATE_SERVICE, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), Integer.valueOf(i4));
    }

    public static void writeAmDestroyService(int i, int i2, int i3) {
        EventLog.writeEvent(AM_DESTROY_SERVICE, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeAmProcessCrashedTooMuch(int i, String str, int i2) {
        EventLog.writeEvent(AM_PROCESS_CRASHED_TOO_MUCH, Integer.valueOf(i), str, Integer.valueOf(i2));
    }

    public static void writeAmDropProcess(int i) {
        EventLog.writeEvent(AM_DROP_PROCESS, i);
    }

    public static void writeAmServiceCrashedTooMuch(int i, int i2, String str, int i3) {
        EventLog.writeEvent(AM_SERVICE_CRASHED_TOO_MUCH, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3));
    }

    public static void writeAmScheduleServiceRestart(int i, String str, long j) {
        EventLog.writeEvent(AM_SCHEDULE_SERVICE_RESTART, Integer.valueOf(i), str, Long.valueOf(j));
    }

    public static void writeAmProviderLostProcess(int i, String str, int i2, String str2) {
        EventLog.writeEvent(AM_PROVIDER_LOST_PROCESS, Integer.valueOf(i), str, Integer.valueOf(i2), str2);
    }

    public static void writeAmProcessStartTimeout(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_PROCESS_START_TIMEOUT, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmCrash(int i, int i2, String str, int i3, String str2, String str3, String str4, int i4) {
        EventLog.writeEvent(AM_CRASH, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2, str3, str4, Integer.valueOf(i4));
    }

    public static void writeAmWtf(int i, int i2, String str, int i3, String str2, String str3) {
        EventLog.writeEvent(AM_WTF, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2, str3);
    }

    public static void writeAmSwitchUser(int i) {
        EventLog.writeEvent(AM_SWITCH_USER, i);
    }

    public static void writeAmActivityFullyDrawnTime(int i, int i2, String str, long j) {
        EventLog.writeEvent(AM_ACTIVITY_FULLY_DRAWN_TIME, Integer.valueOf(i), Integer.valueOf(i2), str, Long.valueOf(j));
    }

    public static void writeAmSetResumedActivity(int i, String str, String str2) {
        EventLog.writeEvent(AM_SET_RESUMED_ACTIVITY, Integer.valueOf(i), str, str2);
    }

    public static void writeAmFocusedStack(int i, int i2, int i3, String str) {
        EventLog.writeEvent(AM_FOCUSED_STACK, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeAmPreBoot(int i, String str) {
        EventLog.writeEvent(AM_PRE_BOOT, Integer.valueOf(i), str);
    }

    public static void writeAmMeminfo(long j, long j2, long j3, long j4, long j5) {
        EventLog.writeEvent(AM_MEMINFO, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), Long.valueOf(j5));
    }

    public static void writeAmPss(int i, int i2, String str, long j, long j2, long j3, long j4, int i3, int i4, long j5) {
        EventLog.writeEvent(AM_PSS, Integer.valueOf(i), Integer.valueOf(i2), str, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), Integer.valueOf(i3), Integer.valueOf(i4), Long.valueOf(j5));
    }

    public static void writeAmStopActivity(int i, int i2, String str) {
        EventLog.writeEvent(AM_STOP_ACTIVITY, Integer.valueOf(i), Integer.valueOf(i2), str);
    }

    public static void writeAmOnStopCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_STOP_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmMemFactor(int i, int i2) {
        EventLog.writeEvent(AM_MEM_FACTOR, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeAmUserStateChanged(int i, int i2) {
        EventLog.writeEvent(AM_USER_STATE_CHANGED, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeAmUidRunning(int i) {
        EventLog.writeEvent(AM_UID_RUNNING, i);
    }

    public static void writeAmUidStopped(int i) {
        EventLog.writeEvent(AM_UID_STOPPED, i);
    }

    public static void writeAmUidActive(int i) {
        EventLog.writeEvent(AM_UID_ACTIVE, i);
    }

    public static void writeAmUidIdle(int i) {
        EventLog.writeEvent(AM_UID_IDLE, i);
    }

    public static void writeAmStopIdleService(int i, String str) {
        EventLog.writeEvent(AM_STOP_IDLE_SERVICE, Integer.valueOf(i), str);
    }

    public static void writeAmOnCreateCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_CREATE_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmOnRestartCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_RESTART_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmOnStartCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_START_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmOnDestroyCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_DESTROY_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmOnActivityResultCalled(int i, String str, String str2) {
        EventLog.writeEvent(AM_ON_ACTIVITY_RESULT_CALLED, Integer.valueOf(i), str, str2);
    }

    public static void writeAmRemoveTask(int i, int i2) {
        EventLog.writeEvent(AM_REMOVE_TASK, Integer.valueOf(i), Integer.valueOf(i2));
    }
}
