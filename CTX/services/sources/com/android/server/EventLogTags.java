package com.android.server;

import android.util.EventLog;

public class EventLogTags {
    public static final int AUTO_BRIGHTNESS_ADJ = 35000;
    public static final int BACKUP_AGENT_FAILURE = 2823;
    public static final int BACKUP_DATA_CHANGED = 2820;
    public static final int BACKUP_INITIALIZE = 2827;
    public static final int BACKUP_PACKAGE = 2824;
    public static final int BACKUP_QUOTA_EXCEEDED = 2829;
    public static final int BACKUP_REQUESTED = 2828;
    public static final int BACKUP_RESET = 2826;
    public static final int BACKUP_START = 2821;
    public static final int BACKUP_SUCCESS = 2825;
    public static final int BACKUP_TRANSPORT_CONNECTION = 2851;
    public static final int BACKUP_TRANSPORT_FAILURE = 2822;
    public static final int BACKUP_TRANSPORT_LIFECYCLE = 2850;
    public static final int BATTERY_DISCHARGE = 2730;
    public static final int BATTERY_LEVEL = 2722;
    public static final int BATTERY_SAVER_MODE = 2739;
    public static final int BATTERY_SAVER_SETTING = 27392;
    public static final int BATTERY_SAVING_STATS = 27390;
    public static final int BATTERY_STATUS = 2723;
    public static final int BOOT_PROGRESS_PMS_DATA_SCAN_START = 3080;
    public static final int BOOT_PROGRESS_PMS_READY = 3100;
    public static final int BOOT_PROGRESS_PMS_SCAN_END = 3090;
    public static final int BOOT_PROGRESS_PMS_START = 3060;
    public static final int BOOT_PROGRESS_PMS_SYSTEM_SCAN_START = 3070;
    public static final int BOOT_PROGRESS_SYSTEM_RUN = 3010;
    public static final int CACHE_FILE_DELETED = 2748;
    public static final int CAMERA_GESTURE_TRIGGERED = 40100;
    public static final int CONFIG_INSTALL_FAILED = 51300;
    public static final int CONNECTIVITY_STATE_CHANGED = 50020;
    public static final int DEVICE_IDLE = 34000;
    public static final int DEVICE_IDLE_LIGHT = 34009;
    public static final int DEVICE_IDLE_LIGHT_STEP = 34010;
    public static final int DEVICE_IDLE_OFF_COMPLETE = 34008;
    public static final int DEVICE_IDLE_OFF_PHASE = 34007;
    public static final int DEVICE_IDLE_OFF_START = 34006;
    public static final int DEVICE_IDLE_ON_COMPLETE = 34005;
    public static final int DEVICE_IDLE_ON_PHASE = 34004;
    public static final int DEVICE_IDLE_ON_START = 34003;
    public static final int DEVICE_IDLE_STEP = 34001;
    public static final int DEVICE_IDLE_WAKE_FROM_IDLE = 34002;
    public static final int FSTRIM_FINISH = 2756;
    public static final int FSTRIM_START = 2755;
    public static final int FULL_BACKUP_AGENT_FAILURE = 2841;
    public static final int FULL_BACKUP_CANCELLED = 2846;
    public static final int FULL_BACKUP_PACKAGE = 2840;
    public static final int FULL_BACKUP_QUOTA_EXCEEDED = 2845;
    public static final int FULL_BACKUP_SUCCESS = 2843;
    public static final int FULL_BACKUP_TRANSPORT_FAILURE = 2842;
    public static final int FULL_RESTORE_PACKAGE = 2844;
    public static final int IDLE_MAINTENANCE_WINDOW_FINISH = 51501;
    public static final int IDLE_MAINTENANCE_WINDOW_START = 51500;
    public static final int IFW_INTENT_MATCHED = 51400;
    public static final int IMF_FORCE_RECONNECT_IME = 32000;
    public static final int JOB_DEFERRED_EXECUTION = 8000;
    public static final int LOCKDOWN_VPN_CONNECTED = 51201;
    public static final int LOCKDOWN_VPN_CONNECTING = 51200;
    public static final int LOCKDOWN_VPN_ERROR = 51202;
    public static final int NETSTATS_MOBILE_SAMPLE = 51100;
    public static final int NETSTATS_WIFI_SAMPLE = 51101;
    public static final int NOTIFICATION_ACTION_CLICKED = 27521;
    public static final int NOTIFICATION_ALERT = 27532;
    public static final int NOTIFICATION_AUTOGROUPED = 27533;
    public static final int NOTIFICATION_CANCEL = 2751;
    public static final int NOTIFICATION_CANCELED = 27530;
    public static final int NOTIFICATION_CANCEL_ALL = 2752;
    public static final int NOTIFICATION_CLICKED = 27520;
    public static final int NOTIFICATION_ENQUEUE = 2750;
    public static final int NOTIFICATION_EXPANSION = 27511;
    public static final int NOTIFICATION_PANEL_HIDDEN = 27501;
    public static final int NOTIFICATION_PANEL_REVEALED = 27500;
    public static final int NOTIFICATION_UNAUTOGROUPED = 275534;
    public static final int NOTIFICATION_VISIBILITY = 27531;
    public static final int NOTIFICATION_VISIBILITY_CHANGED = 27510;
    public static final int PM_CRITICAL_INFO = 3120;
    public static final int PM_PACKAGE_STATS = 3121;
    public static final int POWER_PARTIAL_WAKE_STATE = 2729;
    public static final int POWER_SCREEN_BROADCAST_DONE = 2726;
    public static final int POWER_SCREEN_BROADCAST_SEND = 2725;
    public static final int POWER_SCREEN_BROADCAST_STOP = 2727;
    public static final int POWER_SCREEN_STATE = 2728;
    public static final int POWER_SLEEP_REQUESTED = 2724;
    public static final int POWER_SOFT_SLEEP_REQUESTED = 2731;
    public static final int RESCUE_FAILURE = 2903;
    public static final int RESCUE_LEVEL = 2901;
    public static final int RESCUE_NOTE = 2900;
    public static final int RESCUE_SUCCESS = 2902;
    public static final int RESTORE_AGENT_FAILURE = 2832;
    public static final int RESTORE_PACKAGE = 2833;
    public static final int RESTORE_START = 2830;
    public static final int RESTORE_SUCCESS = 2834;
    public static final int RESTORE_TRANSPORT_FAILURE = 2831;
    public static final int STORAGE_STATE = 2749;
    public static final int STREAM_DEVICES_CHANGED = 40001;
    public static final int TIMEZONE_INSTALL_COMPLETE = 51612;
    public static final int TIMEZONE_INSTALL_STARTED = 51611;
    public static final int TIMEZONE_NOTHING_COMPLETE = 51631;
    public static final int TIMEZONE_REQUEST_INSTALL = 51610;
    public static final int TIMEZONE_REQUEST_NOTHING = 51630;
    public static final int TIMEZONE_REQUEST_UNINSTALL = 51620;
    public static final int TIMEZONE_TRIGGER_CHECK = 51600;
    public static final int TIMEZONE_UNINSTALL_COMPLETE = 51622;
    public static final int TIMEZONE_UNINSTALL_STARTED = 51621;
    public static final int UNKNOWN_SOURCES_ENABLED = 3110;
    public static final int USER_ACTIVITY_TIMEOUT_OVERRIDE = 27391;
    public static final int VOLUME_CHANGED = 40000;
    public static final int WATCHDOG = 2802;
    public static final int WATCHDOG_HARD_RESET = 2805;
    public static final int WATCHDOG_MEMINFO = 2809;
    public static final int WATCHDOG_PROC_PSS = 2803;
    public static final int WATCHDOG_PROC_STATS = 2807;
    public static final int WATCHDOG_PSS_STATS = 2806;
    public static final int WATCHDOG_REQUESTED_REBOOT = 2811;
    public static final int WATCHDOG_SCHEDULED_REBOOT = 2808;
    public static final int WATCHDOG_SOFT_RESET = 2804;
    public static final int WATCHDOG_VMSTAT = 2810;
    public static final int WM_BOOT_ANIMATION_DONE = 31007;
    public static final int WM_HOME_STACK_MOVED = 31005;
    public static final int WM_NO_SURFACE_MEMORY = 31000;
    public static final int WM_STACK_CREATED = 31004;
    public static final int WM_STACK_REMOVED = 31006;
    public static final int WM_TASK_CREATED = 31001;
    public static final int WM_TASK_MOVED = 31002;
    public static final int WM_TASK_REMOVED = 31003;
    public static final int WP_WALLPAPER_CRASHED = 33000;

    private EventLogTags() {
    }

    public static void writeBatteryLevel(int i, int i2, int i3) {
        EventLog.writeEvent(BATTERY_LEVEL, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeBatteryStatus(int i, int i2, int i3, int i4, String str) {
        EventLog.writeEvent(BATTERY_STATUS, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), str);
    }

    public static void writeBatteryDischarge(long j, int i, int i2) {
        EventLog.writeEvent(BATTERY_DISCHARGE, Long.valueOf(j), Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writePowerSleepRequested(int i) {
        EventLog.writeEvent(POWER_SLEEP_REQUESTED, i);
    }

    public static void writePowerScreenBroadcastSend(int i) {
        EventLog.writeEvent(POWER_SCREEN_BROADCAST_SEND, i);
    }

    public static void writePowerScreenBroadcastDone(int i, long j, int i2) {
        EventLog.writeEvent(POWER_SCREEN_BROADCAST_DONE, Integer.valueOf(i), Long.valueOf(j), Integer.valueOf(i2));
    }

    public static void writePowerScreenBroadcastStop(int i, int i2) {
        EventLog.writeEvent(POWER_SCREEN_BROADCAST_STOP, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writePowerScreenState(int i, int i2, long j, int i3, int i4) {
        EventLog.writeEvent(POWER_SCREEN_STATE, Integer.valueOf(i), Integer.valueOf(i2), Long.valueOf(j), Integer.valueOf(i3), Integer.valueOf(i4));
    }

    public static void writePowerPartialWakeState(int i, String str) {
        EventLog.writeEvent(POWER_PARTIAL_WAKE_STATE, Integer.valueOf(i), str);
    }

    public static void writePowerSoftSleepRequested(long j) {
        EventLog.writeEvent(POWER_SOFT_SLEEP_REQUESTED, j);
    }

    public static void writeBatterySaverMode(int i, int i2, int i3, String str, int i4) {
        EventLog.writeEvent(BATTERY_SAVER_MODE, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str, Integer.valueOf(i4));
    }

    public static void writeBatterySavingStats(int i, int i2, int i3, long j, int i4, int i5, long j2, int i6, int i7) {
        EventLog.writeEvent(BATTERY_SAVING_STATS, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Long.valueOf(j), Integer.valueOf(i4), Integer.valueOf(i5), Long.valueOf(j2), Integer.valueOf(i6), Integer.valueOf(i7));
    }

    public static void writeUserActivityTimeoutOverride(long j) {
        EventLog.writeEvent(USER_ACTIVITY_TIMEOUT_OVERRIDE, j);
    }

    public static void writeBatterySaverSetting(int i) {
        EventLog.writeEvent(BATTERY_SAVER_SETTING, i);
    }

    public static void writeCacheFileDeleted(String str) {
        EventLog.writeEvent(CACHE_FILE_DELETED, str);
    }

    public static void writeStorageState(String str, int i, int i2, long j, long j2) {
        EventLog.writeEvent(STORAGE_STATE, str, Integer.valueOf(i), Integer.valueOf(i2), Long.valueOf(j), Long.valueOf(j2));
    }

    public static void writeNotificationEnqueue(int i, int i2, String str, int i3, String str2, int i4, String str3, int i5) {
        EventLog.writeEvent(NOTIFICATION_ENQUEUE, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2, Integer.valueOf(i4), str3, Integer.valueOf(i5));
    }

    public static void writeNotificationCancel(int i, int i2, String str, int i3, String str2, int i4, int i5, int i6, int i7, String str3) {
        EventLog.writeEvent(NOTIFICATION_CANCEL, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), str2, Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), Integer.valueOf(i7), str3);
    }

    public static void writeNotificationCancelAll(int i, int i2, String str, int i3, int i4, int i5, int i6, String str2) {
        EventLog.writeEvent(NOTIFICATION_CANCEL_ALL, Integer.valueOf(i), Integer.valueOf(i2), str, Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), str2);
    }

    public static void writeNotificationPanelRevealed(int i) {
        EventLog.writeEvent(NOTIFICATION_PANEL_REVEALED, i);
    }

    public static void writeNotificationPanelHidden() {
        EventLog.writeEvent(NOTIFICATION_PANEL_HIDDEN, new Object[0]);
    }

    public static void writeNotificationVisibilityChanged(String str, String str2) {
        EventLog.writeEvent(NOTIFICATION_VISIBILITY_CHANGED, str, str2);
    }

    public static void writeNotificationExpansion(String str, int i, int i2, int i3, int i4, int i5) {
        EventLog.writeEvent(NOTIFICATION_EXPANSION, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    public static void writeNotificationClicked(String str, int i, int i2, int i3, int i4, int i5) {
        EventLog.writeEvent(NOTIFICATION_CLICKED, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    public static void writeNotificationActionClicked(String str, int i, int i2, int i3, int i4, int i5, int i6) {
        EventLog.writeEvent(NOTIFICATION_ACTION_CLICKED, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6));
    }

    public static void writeNotificationCanceled(String str, int i, int i2, int i3, int i4, int i5, int i6, String str2) {
        EventLog.writeEvent(NOTIFICATION_CANCELED, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), str2);
    }

    public static void writeNotificationVisibility(String str, int i, int i2, int i3, int i4, int i5) {
        EventLog.writeEvent(NOTIFICATION_VISIBILITY, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    public static void writeNotificationAlert(String str, int i, int i2, int i3) {
        EventLog.writeEvent(NOTIFICATION_ALERT, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeNotificationAutogrouped(String str) {
        EventLog.writeEvent(NOTIFICATION_AUTOGROUPED, str);
    }

    public static void writeNotificationUnautogrouped(String str) {
        EventLog.writeEvent(NOTIFICATION_UNAUTOGROUPED, str);
    }

    public static void writeWatchdog(String str) {
        EventLog.writeEvent(WATCHDOG, str);
    }

    public static void writeWatchdogProcPss(String str, int i, int i2) {
        EventLog.writeEvent(WATCHDOG_PROC_PSS, str, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeWatchdogSoftReset(String str, int i, int i2, int i3, String str2) {
        EventLog.writeEvent(WATCHDOG_SOFT_RESET, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str2);
    }

    public static void writeWatchdogHardReset(String str, int i, int i2, int i3) {
        EventLog.writeEvent(WATCHDOG_HARD_RESET, str, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeWatchdogPssStats(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
        EventLog.writeEvent(WATCHDOG_PSS_STATS, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), Integer.valueOf(i7), Integer.valueOf(i8), Integer.valueOf(i9), Integer.valueOf(i10), Integer.valueOf(i11));
    }

    public static void writeWatchdogProcStats(int i, int i2, int i3, int i4, int i5) {
        EventLog.writeEvent(WATCHDOG_PROC_STATS, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    public static void writeWatchdogScheduledReboot(long j, int i, int i2, int i3, String str) {
        EventLog.writeEvent(WATCHDOG_SCHEDULED_REBOOT, Long.valueOf(j), Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), str);
    }

    public static void writeWatchdogMeminfo(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
        EventLog.writeEvent(WATCHDOG_MEMINFO, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), Integer.valueOf(i7), Integer.valueOf(i8), Integer.valueOf(i9), Integer.valueOf(i10), Integer.valueOf(i11));
    }

    public static void writeWatchdogVmstat(long j, int i, int i2, int i3, int i4, int i5) {
        EventLog.writeEvent(WATCHDOG_VMSTAT, Long.valueOf(j), Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    public static void writeWatchdogRequestedReboot(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        EventLog.writeEvent(WATCHDOG_REQUESTED_REBOOT, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), Integer.valueOf(i7));
    }

    public static void writeRescueNote(int i, int i2, long j) {
        EventLog.writeEvent(RESCUE_NOTE, Integer.valueOf(i), Integer.valueOf(i2), Long.valueOf(j));
    }

    public static void writeRescueLevel(int i, int i2) {
        EventLog.writeEvent(RESCUE_LEVEL, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeRescueSuccess(int i) {
        EventLog.writeEvent(RESCUE_SUCCESS, i);
    }

    public static void writeRescueFailure(int i, String str) {
        EventLog.writeEvent(RESCUE_FAILURE, Integer.valueOf(i), str);
    }

    public static void writeBackupDataChanged(String str) {
        EventLog.writeEvent(BACKUP_DATA_CHANGED, str);
    }

    public static void writeBackupStart(String str) {
        EventLog.writeEvent(BACKUP_START, str);
    }

    public static void writeBackupTransportFailure(String str) {
        EventLog.writeEvent(BACKUP_TRANSPORT_FAILURE, str);
    }

    public static void writeBackupAgentFailure(String str, String str2) {
        EventLog.writeEvent(BACKUP_AGENT_FAILURE, str, str2);
    }

    public static void writeBackupPackage(String str, int i) {
        EventLog.writeEvent(BACKUP_PACKAGE, str, Integer.valueOf(i));
    }

    public static void writeBackupSuccess(int i, int i2) {
        EventLog.writeEvent(BACKUP_SUCCESS, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeBackupReset(String str) {
        EventLog.writeEvent(BACKUP_RESET, str);
    }

    public static void writeBackupInitialize() {
        EventLog.writeEvent(BACKUP_INITIALIZE, new Object[0]);
    }

    public static void writeBackupRequested(int i, int i2, int i3) {
        EventLog.writeEvent(BACKUP_REQUESTED, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeBackupQuotaExceeded(String str) {
        EventLog.writeEvent(BACKUP_QUOTA_EXCEEDED, str);
    }

    public static void writeRestoreStart(String str, long j) {
        EventLog.writeEvent(RESTORE_START, str, Long.valueOf(j));
    }

    public static void writeRestoreTransportFailure() {
        EventLog.writeEvent(RESTORE_TRANSPORT_FAILURE, new Object[0]);
    }

    public static void writeRestoreAgentFailure(String str, String str2) {
        EventLog.writeEvent(RESTORE_AGENT_FAILURE, str, str2);
    }

    public static void writeRestorePackage(String str, int i) {
        EventLog.writeEvent(RESTORE_PACKAGE, str, Integer.valueOf(i));
    }

    public static void writeRestoreSuccess(int i, int i2) {
        EventLog.writeEvent(RESTORE_SUCCESS, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeFullBackupPackage(String str) {
        EventLog.writeEvent(FULL_BACKUP_PACKAGE, str);
    }

    public static void writeFullBackupAgentFailure(String str, String str2) {
        EventLog.writeEvent(FULL_BACKUP_AGENT_FAILURE, str, str2);
    }

    public static void writeFullBackupTransportFailure() {
        EventLog.writeEvent(FULL_BACKUP_TRANSPORT_FAILURE, new Object[0]);
    }

    public static void writeFullBackupSuccess(String str) {
        EventLog.writeEvent(FULL_BACKUP_SUCCESS, str);
    }

    public static void writeFullRestorePackage(String str) {
        EventLog.writeEvent(FULL_RESTORE_PACKAGE, str);
    }

    public static void writeFullBackupQuotaExceeded(String str) {
        EventLog.writeEvent(FULL_BACKUP_QUOTA_EXCEEDED, str);
    }

    public static void writeFullBackupCancelled(String str, String str2) {
        EventLog.writeEvent(FULL_BACKUP_CANCELLED, str, str2);
    }

    public static void writeBackupTransportLifecycle(String str, int i) {
        EventLog.writeEvent(BACKUP_TRANSPORT_LIFECYCLE, str, Integer.valueOf(i));
    }

    public static void writeBackupTransportConnection(String str, int i) {
        EventLog.writeEvent(BACKUP_TRANSPORT_CONNECTION, str, Integer.valueOf(i));
    }

    public static void writeBootProgressSystemRun(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_SYSTEM_RUN, j);
    }

    public static void writeBootProgressPmsStart(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_PMS_START, j);
    }

    public static void writeBootProgressPmsSystemScanStart(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, j);
    }

    public static void writeBootProgressPmsDataScanStart(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_PMS_DATA_SCAN_START, j);
    }

    public static void writeBootProgressPmsScanEnd(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_PMS_SCAN_END, j);
    }

    public static void writeBootProgressPmsReady(long j) {
        EventLog.writeEvent(BOOT_PROGRESS_PMS_READY, j);
    }

    public static void writeUnknownSourcesEnabled(int i) {
        EventLog.writeEvent(UNKNOWN_SOURCES_ENABLED, i);
    }

    public static void writePmCriticalInfo(String str) {
        EventLog.writeEvent(PM_CRITICAL_INFO, str);
    }

    public static void writePmPackageStats(long j, long j2, long j3, long j4, long j5, long j6) {
        EventLog.writeEvent(PM_PACKAGE_STATS, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), Long.valueOf(j5), Long.valueOf(j6));
    }

    public static void writeWmNoSurfaceMemory(String str, int i, String str2) {
        EventLog.writeEvent(WM_NO_SURFACE_MEMORY, str, Integer.valueOf(i), str2);
    }

    public static void writeWmTaskCreated(int i, int i2) {
        EventLog.writeEvent(WM_TASK_CREATED, Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeWmTaskMoved(int i, int i2, int i3) {
        EventLog.writeEvent(WM_TASK_MOVED, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeWmTaskRemoved(int i, String str) {
        EventLog.writeEvent(WM_TASK_REMOVED, Integer.valueOf(i), str);
    }

    public static void writeWmStackCreated(int i) {
        EventLog.writeEvent(WM_STACK_CREATED, i);
    }

    public static void writeWmHomeStackMoved(int i) {
        EventLog.writeEvent(WM_HOME_STACK_MOVED, i);
    }

    public static void writeWmStackRemoved(int i) {
        EventLog.writeEvent(WM_STACK_REMOVED, i);
    }

    public static void writeWmBootAnimationDone(long j) {
        EventLog.writeEvent(WM_BOOT_ANIMATION_DONE, j);
    }

    public static void writeImfForceReconnectIme(Object[] objArr, long j, int i) {
        EventLog.writeEvent(IMF_FORCE_RECONNECT_IME, objArr, Long.valueOf(j), Integer.valueOf(i));
    }

    public static void writeWpWallpaperCrashed(String str) {
        EventLog.writeEvent(WP_WALLPAPER_CRASHED, str);
    }

    public static void writeDeviceIdle(int i, String str) {
        EventLog.writeEvent(DEVICE_IDLE, Integer.valueOf(i), str);
    }

    public static void writeDeviceIdleStep() {
        EventLog.writeEvent(DEVICE_IDLE_STEP, new Object[0]);
    }

    public static void writeDeviceIdleWakeFromIdle(int i, String str) {
        EventLog.writeEvent(DEVICE_IDLE_WAKE_FROM_IDLE, Integer.valueOf(i), str);
    }

    public static void writeDeviceIdleOnStart() {
        EventLog.writeEvent(DEVICE_IDLE_ON_START, new Object[0]);
    }

    public static void writeDeviceIdleOnPhase(String str) {
        EventLog.writeEvent(DEVICE_IDLE_ON_PHASE, str);
    }

    public static void writeDeviceIdleOnComplete() {
        EventLog.writeEvent(DEVICE_IDLE_ON_COMPLETE, new Object[0]);
    }

    public static void writeDeviceIdleOffStart(String str) {
        EventLog.writeEvent(DEVICE_IDLE_OFF_START, str);
    }

    public static void writeDeviceIdleOffPhase(String str) {
        EventLog.writeEvent(DEVICE_IDLE_OFF_PHASE, str);
    }

    public static void writeDeviceIdleOffComplete() {
        EventLog.writeEvent(DEVICE_IDLE_OFF_COMPLETE, new Object[0]);
    }

    public static void writeDeviceIdleLight(int i, String str) {
        EventLog.writeEvent(DEVICE_IDLE_LIGHT, Integer.valueOf(i), str);
    }

    public static void writeDeviceIdleLightStep() {
        EventLog.writeEvent(DEVICE_IDLE_LIGHT_STEP, new Object[0]);
    }

    public static void writeAutoBrightnessAdj(float f, float f2, float f3, float f4) {
        EventLog.writeEvent(AUTO_BRIGHTNESS_ADJ, Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(f4));
    }

    public static void writeConnectivityStateChanged(int i, int i2, int i3) {
        EventLog.writeEvent(CONNECTIVITY_STATE_CHANGED, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeNetstatsMobileSample(long j, long j2, long j3, long j4, long j5, long j6, long j7, long j8, long j9, long j10, long j11, long j12, long j13) {
        EventLog.writeEvent(NETSTATS_MOBILE_SAMPLE, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), Long.valueOf(j5), Long.valueOf(j6), Long.valueOf(j7), Long.valueOf(j8), Long.valueOf(j9), Long.valueOf(j10), Long.valueOf(j11), Long.valueOf(j12), Long.valueOf(j13));
    }

    public static void writeNetstatsWifiSample(long j, long j2, long j3, long j4, long j5, long j6, long j7, long j8, long j9, long j10, long j11, long j12, long j13) {
        EventLog.writeEvent(NETSTATS_WIFI_SAMPLE, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Long.valueOf(j4), Long.valueOf(j5), Long.valueOf(j6), Long.valueOf(j7), Long.valueOf(j8), Long.valueOf(j9), Long.valueOf(j10), Long.valueOf(j11), Long.valueOf(j12), Long.valueOf(j13));
    }

    public static void writeLockdownVpnConnecting(int i) {
        EventLog.writeEvent(LOCKDOWN_VPN_CONNECTING, i);
    }

    public static void writeLockdownVpnConnected(int i) {
        EventLog.writeEvent(LOCKDOWN_VPN_CONNECTED, i);
    }

    public static void writeLockdownVpnError(int i) {
        EventLog.writeEvent(LOCKDOWN_VPN_ERROR, i);
    }

    public static void writeConfigInstallFailed(String str) {
        EventLog.writeEvent(CONFIG_INSTALL_FAILED, str);
    }

    public static void writeIfwIntentMatched(int i, String str, int i2, int i3, String str2, String str3, String str4, String str5, int i4) {
        EventLog.writeEvent(IFW_INTENT_MATCHED, Integer.valueOf(i), str, Integer.valueOf(i2), Integer.valueOf(i3), str2, str3, str4, str5, Integer.valueOf(i4));
    }

    public static void writeIdleMaintenanceWindowStart(long j, long j2, int i, int i2) {
        EventLog.writeEvent(IDLE_MAINTENANCE_WINDOW_START, Long.valueOf(j), Long.valueOf(j2), Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeIdleMaintenanceWindowFinish(long j, long j2, int i, int i2) {
        EventLog.writeEvent(IDLE_MAINTENANCE_WINDOW_FINISH, Long.valueOf(j), Long.valueOf(j2), Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void writeFstrimStart(long j) {
        EventLog.writeEvent(FSTRIM_START, j);
    }

    public static void writeFstrimFinish(long j) {
        EventLog.writeEvent(FSTRIM_FINISH, j);
    }

    public static void writeJobDeferredExecution(long j) {
        EventLog.writeEvent(JOB_DEFERRED_EXECUTION, j);
    }

    public static void writeVolumeChanged(int i, int i2, int i3, int i4, String str) {
        EventLog.writeEvent(VOLUME_CHANGED, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), str);
    }

    public static void writeStreamDevicesChanged(int i, int i2, int i3) {
        EventLog.writeEvent(STREAM_DEVICES_CHANGED, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
    }

    public static void writeCameraGestureTriggered(long j, long j2, long j3, int i) {
        EventLog.writeEvent(CAMERA_GESTURE_TRIGGERED, Long.valueOf(j), Long.valueOf(j2), Long.valueOf(j3), Integer.valueOf(i));
    }

    public static void writeTimezoneTriggerCheck(String str) {
        EventLog.writeEvent(TIMEZONE_TRIGGER_CHECK, str);
    }

    public static void writeTimezoneRequestInstall(String str) {
        EventLog.writeEvent(TIMEZONE_REQUEST_INSTALL, str);
    }

    public static void writeTimezoneInstallStarted(String str) {
        EventLog.writeEvent(TIMEZONE_INSTALL_STARTED, str);
    }

    public static void writeTimezoneInstallComplete(String str, int i) {
        EventLog.writeEvent(TIMEZONE_INSTALL_COMPLETE, str, Integer.valueOf(i));
    }

    public static void writeTimezoneRequestUninstall(String str) {
        EventLog.writeEvent(TIMEZONE_REQUEST_UNINSTALL, str);
    }

    public static void writeTimezoneUninstallStarted(String str) {
        EventLog.writeEvent(TIMEZONE_UNINSTALL_STARTED, str);
    }

    public static void writeTimezoneUninstallComplete(String str, int i) {
        EventLog.writeEvent(TIMEZONE_UNINSTALL_COMPLETE, str, Integer.valueOf(i));
    }

    public static void writeTimezoneRequestNothing(String str) {
        EventLog.writeEvent(TIMEZONE_REQUEST_NOTHING, str);
    }

    public static void writeTimezoneNothingComplete(String str) {
        EventLog.writeEvent(TIMEZONE_NOTHING_COMPLETE, str);
    }
}
