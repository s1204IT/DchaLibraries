package com.android.server.am;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.dex.ArtManagerInternal;
import android.content.pm.dex.PackageOptimizationInfo;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StatsLog;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.am.MemoryStatUtil;
import java.util.ArrayList;

class ActivityMetricsLogger {
    private static final long INVALID_START_TIME = -1;
    private static final int MSG_CHECK_VISIBILITY = 0;
    private static final String TAG = "ActivityManager";
    private static final String[] TRON_WINDOW_STATE_VARZ_STRINGS = {"window_time_0", "window_time_1", "window_time_2", "window_time_3"};
    private static final int WINDOW_STATE_ASSISTANT = 3;
    private static final int WINDOW_STATE_FREEFORM = 2;
    private static final int WINDOW_STATE_INVALID = -1;
    private static final int WINDOW_STATE_SIDE_BY_SIDE = 1;
    private static final int WINDOW_STATE_STANDARD = 0;
    private ArtManagerInternal mArtManagerInternal;
    private final Context mContext;
    private int mCurrentTransitionDelayMs;
    private int mCurrentTransitionDeviceUptime;
    private final H mHandler;
    private boolean mLoggedTransitionStarting;
    private final ActivityStackSupervisor mSupervisor;
    private int mWindowState = 0;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private long mCurrentTransitionStartTime = -1;
    private long mLastTransitionStartTime = -1;
    private final SparseArray<WindowingModeTransitionInfo> mWindowingModeTransitionInfo = new SparseArray<>();
    private final SparseArray<WindowingModeTransitionInfo> mLastWindowingModeTransitionInfo = new SparseArray<>();
    private long mLastLogTimeSecs = SystemClock.elapsedRealtime() / 1000;

    private final class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                SomeArgs someArgs = (SomeArgs) message.obj;
                ActivityMetricsLogger.this.checkVisibility((TaskRecord) someArgs.arg1, (ActivityRecord) someArgs.arg2);
            }
        }
    }

    private final class WindowingModeTransitionInfo {
        private int bindApplicationDelayMs;
        private boolean currentTransitionProcessRunning;
        private ActivityRecord launchedActivity;
        private boolean loggedStartingWindowDrawn;
        private boolean loggedWindowsDrawn;
        private int reason;
        private int startResult;
        private int startingWindowDelayMs;
        private int windowsDrawnDelayMs;

        private WindowingModeTransitionInfo() {
            this.startingWindowDelayMs = -1;
            this.bindApplicationDelayMs = -1;
            this.reason = 3;
        }
    }

    private final class WindowingModeTransitionInfoSnapshot {
        private final ApplicationInfo applicationInfo;
        private final int bindApplicationDelayMs;
        private final String launchedActivityAppRecordRequiredAbi;
        private final String launchedActivityLaunchToken;
        private final String launchedActivityLaunchedFromPackage;
        private final String launchedActivityName;
        private final String packageName;
        private final String processName;
        private final ProcessRecord processRecord;
        private final int reason;
        private final int startingWindowDelayMs;
        private final int type;
        private final int windowsDrawnDelayMs;

        private WindowingModeTransitionInfoSnapshot(WindowingModeTransitionInfo windowingModeTransitionInfo) {
            String str;
            this.applicationInfo = windowingModeTransitionInfo.launchedActivity.appInfo;
            this.packageName = windowingModeTransitionInfo.launchedActivity.packageName;
            this.launchedActivityName = windowingModeTransitionInfo.launchedActivity.info.name;
            this.launchedActivityLaunchedFromPackage = windowingModeTransitionInfo.launchedActivity.launchedFromPackage;
            this.launchedActivityLaunchToken = windowingModeTransitionInfo.launchedActivity.info.launchToken;
            if (windowingModeTransitionInfo.launchedActivity.app != null) {
                str = windowingModeTransitionInfo.launchedActivity.app.requiredAbi;
            } else {
                str = null;
            }
            this.launchedActivityAppRecordRequiredAbi = str;
            this.reason = windowingModeTransitionInfo.reason;
            this.startingWindowDelayMs = windowingModeTransitionInfo.startingWindowDelayMs;
            this.bindApplicationDelayMs = windowingModeTransitionInfo.bindApplicationDelayMs;
            this.windowsDrawnDelayMs = windowingModeTransitionInfo.windowsDrawnDelayMs;
            this.type = ActivityMetricsLogger.this.getTransitionType(windowingModeTransitionInfo);
            this.processRecord = ActivityMetricsLogger.this.findProcessForActivity(windowingModeTransitionInfo.launchedActivity);
            this.processName = windowingModeTransitionInfo.launchedActivity.processName;
        }
    }

    ActivityMetricsLogger(ActivityStackSupervisor activityStackSupervisor, Context context, Looper looper) {
        this.mSupervisor = activityStackSupervisor;
        this.mContext = context;
        this.mHandler = new H(looper);
    }

    void logWindowState() {
        long jElapsedRealtime = SystemClock.elapsedRealtime() / 1000;
        if (this.mWindowState != -1) {
            MetricsLogger.count(this.mContext, TRON_WINDOW_STATE_VARZ_STRINGS[this.mWindowState], (int) (jElapsedRealtime - this.mLastLogTimeSecs));
        }
        this.mLastLogTimeSecs = jElapsedRealtime;
        this.mWindowState = -1;
        ActivityStack focusedStack = this.mSupervisor.getFocusedStack();
        if (focusedStack.isActivityTypeAssistant()) {
            this.mWindowState = 3;
            return;
        }
        int windowingMode = focusedStack.getWindowingMode();
        if (windowingMode == 2) {
            focusedStack = this.mSupervisor.findStackBehind(focusedStack);
            windowingMode = focusedStack.getWindowingMode();
        }
        if (windowingMode == 1) {
            this.mWindowState = 0;
            return;
        }
        switch (windowingMode) {
            case 3:
            case 4:
                this.mWindowState = 1;
                return;
            case 5:
                this.mWindowState = 2;
                return;
            default:
                if (windowingMode != 0) {
                    throw new IllegalStateException("Unknown windowing mode for stack=" + focusedStack + " windowingMode=" + windowingMode);
                }
                return;
        }
    }

    void notifyActivityLaunching() {
        if (!isAnyTransitionActive()) {
            if (ActivityManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "notifyActivityLaunching");
            }
            this.mCurrentTransitionStartTime = SystemClock.uptimeMillis();
            this.mLastTransitionStartTime = this.mCurrentTransitionStartTime;
        }
    }

    void notifyActivityLaunched(int i, ActivityRecord activityRecord) {
        ProcessRecord processRecordFindProcessForActivity = findProcessForActivity(activityRecord);
        boolean z = false;
        boolean z2 = processRecordFindProcessForActivity != null;
        if (processRecordFindProcessForActivity == null || !hasStartedActivity(processRecordFindProcessForActivity, activityRecord)) {
            z = true;
        }
        notifyActivityLaunched(i, activityRecord, z2, z);
    }

    private boolean hasStartedActivity(ProcessRecord processRecord, ActivityRecord activityRecord) {
        ArrayList<ActivityRecord> arrayList = processRecord.activities;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ActivityRecord activityRecord2 = arrayList.get(size);
            if (activityRecord != activityRecord2 && !activityRecord2.stopped) {
                return true;
            }
        }
        return false;
    }

    private void notifyActivityLaunched(int i, ActivityRecord activityRecord, boolean z, boolean z2) {
        int windowingMode;
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyActivityLaunched resultCode=" + i + " launchedActivity=" + activityRecord + " processRunning=" + z + " processSwitch=" + z2);
        }
        boolean z3 = false;
        if (activityRecord != null) {
            windowingMode = activityRecord.getWindowingMode();
        } else {
            windowingMode = 0;
        }
        if (this.mCurrentTransitionStartTime == -1) {
            return;
        }
        WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(windowingMode);
        if (activityRecord != null && windowingModeTransitionInfo != null) {
            windowingModeTransitionInfo.launchedActivity = activityRecord;
            return;
        }
        if (this.mWindowingModeTransitionInfo.size() > 0 && windowingModeTransitionInfo == null) {
            z3 = true;
        }
        if ((!isLoggableResultCode(i) || activityRecord == null || !z2 || windowingMode == 0) && !z3) {
            reset(true);
            return;
        }
        if (z3) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyActivityLaunched successful");
        }
        WindowingModeTransitionInfo windowingModeTransitionInfo2 = new WindowingModeTransitionInfo();
        windowingModeTransitionInfo2.launchedActivity = activityRecord;
        windowingModeTransitionInfo2.currentTransitionProcessRunning = z;
        windowingModeTransitionInfo2.startResult = i;
        this.mWindowingModeTransitionInfo.put(windowingMode, windowingModeTransitionInfo2);
        this.mLastWindowingModeTransitionInfo.put(windowingMode, windowingModeTransitionInfo2);
        this.mCurrentTransitionDeviceUptime = (int) (SystemClock.uptimeMillis() / 1000);
    }

    private boolean isLoggableResultCode(int i) {
        return i == 0 || i == 2;
    }

    void notifyWindowsDrawn(int i, long j) {
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyWindowsDrawn windowingMode=" + i);
        }
        WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(i);
        if (windowingModeTransitionInfo != null && !windowingModeTransitionInfo.loggedWindowsDrawn) {
            windowingModeTransitionInfo.windowsDrawnDelayMs = calculateDelay(j);
            windowingModeTransitionInfo.loggedWindowsDrawn = true;
            if (allWindowsDrawn() && this.mLoggedTransitionStarting) {
                reset(false);
            }
        }
    }

    void notifyStartingWindowDrawn(int i, long j) {
        WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(i);
        if (windowingModeTransitionInfo != null && !windowingModeTransitionInfo.loggedStartingWindowDrawn) {
            windowingModeTransitionInfo.loggedStartingWindowDrawn = true;
            windowingModeTransitionInfo.startingWindowDelayMs = calculateDelay(j);
        }
    }

    void notifyTransitionStarting(SparseIntArray sparseIntArray, long j) {
        if (!isAnyTransitionActive() || this.mLoggedTransitionStarting) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyTransitionStarting");
        }
        this.mCurrentTransitionDelayMs = calculateDelay(j);
        this.mLoggedTransitionStarting = true;
        for (int size = sparseIntArray.size() - 1; size >= 0; size--) {
            WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(sparseIntArray.keyAt(size));
            if (windowingModeTransitionInfo != null) {
                windowingModeTransitionInfo.reason = sparseIntArray.valueAt(size);
            }
        }
        if (allWindowsDrawn()) {
            reset(false);
        }
    }

    void notifyVisibilityChanged(ActivityRecord activityRecord) {
        WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(activityRecord.getWindowingMode());
        if (windowingModeTransitionInfo == null || windowingModeTransitionInfo.launchedActivity != activityRecord) {
            return;
        }
        TaskRecord task = activityRecord.getTask();
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = task;
        someArgsObtain.arg2 = activityRecord;
        this.mHandler.obtainMessage(0, someArgsObtain).sendToTarget();
    }

    private void checkVisibility(TaskRecord taskRecord, ActivityRecord activityRecord) {
        synchronized (this.mSupervisor.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                WindowingModeTransitionInfo windowingModeTransitionInfo = this.mWindowingModeTransitionInfo.get(activityRecord.getWindowingMode());
                if (windowingModeTransitionInfo != null && !taskRecord.isVisible()) {
                    if (ActivityManagerDebugConfig.DEBUG_METRICS) {
                        Slog.i(TAG, "notifyVisibilityChanged to invisible activity=" + activityRecord);
                    }
                    logAppTransitionCancel(windowingModeTransitionInfo);
                    this.mWindowingModeTransitionInfo.remove(activityRecord.getWindowingMode());
                    if (this.mWindowingModeTransitionInfo.size() == 0) {
                        reset(true);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void notifyBindApplication(ProcessRecord processRecord) {
        for (int size = this.mWindowingModeTransitionInfo.size() - 1; size >= 0; size--) {
            WindowingModeTransitionInfo windowingModeTransitionInfoValueAt = this.mWindowingModeTransitionInfo.valueAt(size);
            if (windowingModeTransitionInfoValueAt.launchedActivity.appInfo == processRecord.info) {
                windowingModeTransitionInfoValueAt.bindApplicationDelayMs = calculateCurrentDelay();
            }
        }
    }

    private boolean allWindowsDrawn() {
        for (int size = this.mWindowingModeTransitionInfo.size() - 1; size >= 0; size--) {
            if (!this.mWindowingModeTransitionInfo.valueAt(size).loggedWindowsDrawn) {
                return false;
            }
        }
        return true;
    }

    private boolean isAnyTransitionActive() {
        return this.mCurrentTransitionStartTime != -1 && this.mWindowingModeTransitionInfo.size() > 0;
    }

    private void reset(boolean z) {
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "reset abort=" + z);
        }
        if (!z && isAnyTransitionActive()) {
            logAppTransitionMultiEvents();
        }
        this.mCurrentTransitionStartTime = -1L;
        this.mCurrentTransitionDelayMs = -1;
        this.mLoggedTransitionStarting = false;
        this.mWindowingModeTransitionInfo.clear();
    }

    private int calculateCurrentDelay() {
        return (int) (SystemClock.uptimeMillis() - this.mCurrentTransitionStartTime);
    }

    private int calculateDelay(long j) {
        return (int) (j - this.mCurrentTransitionStartTime);
    }

    private void logAppTransitionCancel(WindowingModeTransitionInfo windowingModeTransitionInfo) {
        int transitionType = getTransitionType(windowingModeTransitionInfo);
        if (transitionType == -1) {
            return;
        }
        LogMaker logMaker = new LogMaker(1144);
        logMaker.setPackageName(windowingModeTransitionInfo.launchedActivity.packageName);
        logMaker.setType(transitionType);
        logMaker.addTaggedData(871, windowingModeTransitionInfo.launchedActivity.info.name);
        this.mMetricsLogger.write(logMaker);
        StatsLog.write(49, windowingModeTransitionInfo.launchedActivity.appInfo.uid, windowingModeTransitionInfo.launchedActivity.packageName, convertAppStartTransitionType(transitionType), windowingModeTransitionInfo.launchedActivity.info.name);
    }

    private void logAppTransitionMultiEvents() {
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "logging transition events");
        }
        for (int size = this.mWindowingModeTransitionInfo.size() - 1; size >= 0; size--) {
            WindowingModeTransitionInfo windowingModeTransitionInfoValueAt = this.mWindowingModeTransitionInfo.valueAt(size);
            if (getTransitionType(windowingModeTransitionInfoValueAt) == -1) {
                return;
            }
            final WindowingModeTransitionInfoSnapshot windowingModeTransitionInfoSnapshot = new WindowingModeTransitionInfoSnapshot(windowingModeTransitionInfoValueAt);
            final int i = this.mCurrentTransitionDeviceUptime;
            final int i2 = this.mCurrentTransitionDelayMs;
            BackgroundThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.logAppTransition(i, i2, windowingModeTransitionInfoSnapshot);
                }
            });
            windowingModeTransitionInfoValueAt.launchedActivity.info.launchToken = null;
        }
    }

    private void logAppTransition(int i, int i2, WindowingModeTransitionInfoSnapshot windowingModeTransitionInfoSnapshot) {
        PackageOptimizationInfo packageOptimizationInfoCreateWithNoInfo;
        LogMaker logMaker = new LogMaker(761);
        logMaker.setPackageName(windowingModeTransitionInfoSnapshot.packageName);
        logMaker.setType(windowingModeTransitionInfoSnapshot.type);
        logMaker.addTaggedData(871, windowingModeTransitionInfoSnapshot.launchedActivityName);
        boolean zIsInstantApp = windowingModeTransitionInfoSnapshot.applicationInfo.isInstantApp();
        if (windowingModeTransitionInfoSnapshot.launchedActivityLaunchedFromPackage != null) {
            logMaker.addTaggedData(904, windowingModeTransitionInfoSnapshot.launchedActivityLaunchedFromPackage);
        }
        String str = windowingModeTransitionInfoSnapshot.launchedActivityLaunchToken;
        if (str != null) {
            logMaker.addTaggedData(903, str);
        }
        logMaker.addTaggedData(905, Integer.valueOf(zIsInstantApp ? 1 : 0));
        logMaker.addTaggedData(325, Integer.valueOf(i));
        logMaker.addTaggedData(319, Integer.valueOf(i2));
        logMaker.setSubtype(windowingModeTransitionInfoSnapshot.reason);
        if (windowingModeTransitionInfoSnapshot.startingWindowDelayMs != -1) {
            logMaker.addTaggedData(321, Integer.valueOf(windowingModeTransitionInfoSnapshot.startingWindowDelayMs));
        }
        if (windowingModeTransitionInfoSnapshot.bindApplicationDelayMs != -1) {
            logMaker.addTaggedData(945, Integer.valueOf(windowingModeTransitionInfoSnapshot.bindApplicationDelayMs));
        }
        logMaker.addTaggedData(322, Integer.valueOf(windowingModeTransitionInfoSnapshot.windowsDrawnDelayMs));
        ArtManagerInternal artManagerInternal = getArtManagerInternal();
        if (artManagerInternal == null || windowingModeTransitionInfoSnapshot.launchedActivityAppRecordRequiredAbi == null) {
            packageOptimizationInfoCreateWithNoInfo = PackageOptimizationInfo.createWithNoInfo();
        } else {
            packageOptimizationInfoCreateWithNoInfo = artManagerInternal.getPackageOptimizationInfo(windowingModeTransitionInfoSnapshot.applicationInfo, windowingModeTransitionInfoSnapshot.launchedActivityAppRecordRequiredAbi);
        }
        logMaker.addTaggedData(1321, Integer.valueOf(packageOptimizationInfoCreateWithNoInfo.getCompilationReason()));
        logMaker.addTaggedData(1320, Integer.valueOf(packageOptimizationInfoCreateWithNoInfo.getCompilationFilter()));
        this.mMetricsLogger.write(logMaker);
        StatsLog.write(48, windowingModeTransitionInfoSnapshot.applicationInfo.uid, windowingModeTransitionInfoSnapshot.packageName, convertAppStartTransitionType(windowingModeTransitionInfoSnapshot.type), windowingModeTransitionInfoSnapshot.launchedActivityName, windowingModeTransitionInfoSnapshot.launchedActivityLaunchedFromPackage, zIsInstantApp, i * 1000, windowingModeTransitionInfoSnapshot.reason, i2, windowingModeTransitionInfoSnapshot.startingWindowDelayMs, windowingModeTransitionInfoSnapshot.bindApplicationDelayMs, windowingModeTransitionInfoSnapshot.windowsDrawnDelayMs, str, packageOptimizationInfoCreateWithNoInfo.getCompilationReason(), packageOptimizationInfoCreateWithNoInfo.getCompilationFilter());
        logAppStartMemoryStateCapture(windowingModeTransitionInfoSnapshot);
    }

    private int convertAppStartTransitionType(int i) {
        if (i == 7) {
            return 3;
        }
        if (i == 8) {
            return 1;
        }
        if (i == 9) {
            return 2;
        }
        return 0;
    }

    void logAppTransitionReportedDrawn(ActivityRecord activityRecord, boolean z) {
        int i;
        int i2;
        WindowingModeTransitionInfo windowingModeTransitionInfo = this.mLastWindowingModeTransitionInfo.get(activityRecord.getWindowingMode());
        if (windowingModeTransitionInfo == null) {
            return;
        }
        LogMaker logMaker = new LogMaker(1090);
        logMaker.setPackageName(activityRecord.packageName);
        logMaker.addTaggedData(871, activityRecord.info.name);
        long jUptimeMillis = SystemClock.uptimeMillis() - this.mLastTransitionStartTime;
        logMaker.addTaggedData(1091, Long.valueOf(jUptimeMillis));
        if (z) {
            i = 13;
        } else {
            i = 12;
        }
        logMaker.setType(i);
        logMaker.addTaggedData(324, Integer.valueOf(windowingModeTransitionInfo.currentTransitionProcessRunning ? 1 : 0));
        this.mMetricsLogger.write(logMaker);
        int i3 = windowingModeTransitionInfo.launchedActivity.appInfo.uid;
        String str = windowingModeTransitionInfo.launchedActivity.packageName;
        if (z) {
            i2 = 1;
        } else {
            i2 = 2;
        }
        StatsLog.write(50, i3, str, i2, windowingModeTransitionInfo.launchedActivity.info.name, windowingModeTransitionInfo.currentTransitionProcessRunning, jUptimeMillis);
    }

    private int getTransitionType(WindowingModeTransitionInfo windowingModeTransitionInfo) {
        if (windowingModeTransitionInfo.currentTransitionProcessRunning) {
            if (windowingModeTransitionInfo.startResult != 0) {
                if (windowingModeTransitionInfo.startResult == 2) {
                    return 9;
                }
                return -1;
            }
            return 8;
        }
        if (windowingModeTransitionInfo.startResult == 0) {
            return 7;
        }
        return -1;
    }

    private void logAppStartMemoryStateCapture(WindowingModeTransitionInfoSnapshot windowingModeTransitionInfoSnapshot) {
        if (windowingModeTransitionInfoSnapshot.processRecord != null) {
            int i = windowingModeTransitionInfoSnapshot.processRecord.pid;
            int i2 = windowingModeTransitionInfoSnapshot.applicationInfo.uid;
            MemoryStatUtil.MemoryStat memoryStatFromFilesystem = MemoryStatUtil.readMemoryStatFromFilesystem(i2, i);
            if (memoryStatFromFilesystem == null) {
                if (ActivityManagerDebugConfig.DEBUG_METRICS) {
                    Slog.i(TAG, "logAppStartMemoryStateCapture memoryStat null");
                    return;
                }
                return;
            }
            StatsLog.write(55, i2, windowingModeTransitionInfoSnapshot.processName, windowingModeTransitionInfoSnapshot.launchedActivityName, memoryStatFromFilesystem.pgfault, memoryStatFromFilesystem.pgmajfault, memoryStatFromFilesystem.rssInBytes, memoryStatFromFilesystem.cacheInBytes, memoryStatFromFilesystem.swapInBytes);
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "logAppStartMemoryStateCapture processRecord null");
        }
    }

    private ProcessRecord findProcessForActivity(ActivityRecord activityRecord) {
        if (activityRecord != null) {
            return (ProcessRecord) this.mSupervisor.mService.mProcessNames.get(activityRecord.processName, activityRecord.appInfo.uid);
        }
        return null;
    }

    private ArtManagerInternal getArtManagerInternal() {
        if (this.mArtManagerInternal == null) {
            this.mArtManagerInternal = (ArtManagerInternal) LocalServices.getService(ArtManagerInternal.class);
        }
        return this.mArtManagerInternal;
    }
}
