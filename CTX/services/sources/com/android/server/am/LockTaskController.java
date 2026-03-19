package com.android.server.am;

import android.app.admin.IDevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.am.LockTaskController;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class LockTaskController {
    private static final String LOCK_TASK_TAG = "Lock-to-App";

    @VisibleForTesting
    static final int STATUS_BAR_MASK_LOCKED = 61210624;

    @VisibleForTesting
    static final int STATUS_BAR_MASK_PINNED = 43974656;
    private final Context mContext;

    @VisibleForTesting
    IDevicePolicyManager mDevicePolicyManager;
    private final Handler mHandler;

    @VisibleForTesting
    LockPatternUtils mLockPatternUtils;

    @VisibleForTesting
    IStatusBarService mStatusBarService;
    private final ActivityStackSupervisor mSupervisor;

    @VisibleForTesting
    TelecomManager mTelecomManager;

    @VisibleForTesting
    WindowManagerService mWindowManager;
    private static final String TAG = "ActivityManager";
    private static final String TAG_LOCKTASK = TAG + ActivityManagerDebugConfig.POSTFIX_LOCKTASK;
    private static final SparseArray<Pair<Integer, Integer>> STATUS_BAR_FLAG_MAP_LOCKED = new SparseArray<>();
    private final IBinder mToken = new Binder();
    private final ArrayList<TaskRecord> mLockTaskModeTasks = new ArrayList<>();
    private final SparseArray<String[]> mLockTaskPackages = new SparseArray<>();
    private final SparseIntArray mLockTaskFeatures = new SparseIntArray();
    private int mLockTaskModeState = 0;

    static {
        STATUS_BAR_FLAG_MAP_LOCKED.append(1, new Pair<>(Integer.valueOf(DumpState.DUMP_VOLUMES), 2));
        STATUS_BAR_FLAG_MAP_LOCKED.append(2, new Pair<>(393216, 4));
        STATUS_BAR_FLAG_MAP_LOCKED.append(4, new Pair<>(Integer.valueOf(DumpState.DUMP_COMPILER_STATS), 0));
        STATUS_BAR_FLAG_MAP_LOCKED.append(8, new Pair<>(Integer.valueOf(DumpState.DUMP_SERVICE_PERMISSIONS), 0));
        STATUS_BAR_FLAG_MAP_LOCKED.append(16, new Pair<>(0, 8));
    }

    LockTaskController(Context context, ActivityStackSupervisor activityStackSupervisor, Handler handler) {
        this.mContext = context;
        this.mSupervisor = activityStackSupervisor;
        this.mHandler = handler;
    }

    void setWindowManager(WindowManagerService windowManagerService) {
        this.mWindowManager = windowManagerService;
    }

    int getLockTaskModeState() {
        return this.mLockTaskModeState;
    }

    @VisibleForTesting
    boolean isTaskLocked(TaskRecord taskRecord) {
        return this.mLockTaskModeTasks.contains(taskRecord);
    }

    private boolean isRootTask(TaskRecord taskRecord) {
        return this.mLockTaskModeTasks.indexOf(taskRecord) == 0;
    }

    boolean activityBlockedFromFinish(ActivityRecord activityRecord) {
        TaskRecord task = activityRecord.getTask();
        if (activityRecord == task.getRootActivity() && activityRecord == task.getTopActivity() && task.mLockTaskAuth != 4 && isRootTask(task)) {
            Slog.i(TAG, "Not finishing task in lock task mode");
            showLockTaskToast();
            return true;
        }
        return false;
    }

    boolean canMoveTaskToBack(TaskRecord taskRecord) {
        if (isRootTask(taskRecord)) {
            showLockTaskToast();
            return false;
        }
        return true;
    }

    boolean isTaskWhitelisted(TaskRecord taskRecord) {
        switch (taskRecord.mLockTaskAuth) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    boolean isLockTaskModeViolation(TaskRecord taskRecord) {
        return isLockTaskModeViolation(taskRecord, false);
    }

    boolean isLockTaskModeViolation(TaskRecord taskRecord, boolean z) {
        if (isLockTaskModeViolationInternal(taskRecord, z)) {
            showLockTaskToast();
            return true;
        }
        return false;
    }

    TaskRecord getRootTask() {
        if (this.mLockTaskModeTasks.isEmpty()) {
            return null;
        }
        return this.mLockTaskModeTasks.get(0);
    }

    private boolean isLockTaskModeViolationInternal(TaskRecord taskRecord, boolean z) {
        if (isTaskLocked(taskRecord) && !z) {
            return false;
        }
        if (taskRecord.isActivityTypeRecents() && isRecentsAllowed(taskRecord.userId)) {
            return false;
        }
        return ((isKeyguardAllowed(taskRecord.userId) && isEmergencyCallTask(taskRecord)) || isTaskWhitelisted(taskRecord) || this.mLockTaskModeTasks.isEmpty()) ? false : true;
    }

    private boolean isRecentsAllowed(int i) {
        return (getLockTaskFeaturesForUser(i) & 8) != 0;
    }

    private boolean isKeyguardAllowed(int i) {
        return (getLockTaskFeaturesForUser(i) & 32) != 0;
    }

    private boolean isEmergencyCallTask(TaskRecord taskRecord) {
        Intent intent = taskRecord.intent;
        if (intent == null) {
            return false;
        }
        if (TelecomManager.EMERGENCY_DIALER_COMPONENT.equals(intent.getComponent()) || "android.intent.action.CALL_EMERGENCY".equals(intent.getAction())) {
            return true;
        }
        TelecomManager telecomManager = getTelecomManager();
        String systemDialerPackage = telecomManager != null ? telecomManager.getSystemDialerPackage() : null;
        return systemDialerPackage != null && systemDialerPackage.equals(intent.getComponent().getPackageName());
    }

    void stopLockTaskMode(TaskRecord taskRecord, boolean z, int i) {
        if (this.mLockTaskModeState == 0) {
            return;
        }
        if (z) {
            if (this.mLockTaskModeState == 2) {
                clearLockedTasks("stopAppPinning");
                return;
            } else {
                Slog.e(TAG_LOCKTASK, "Attempted to stop LockTask with isSystemCaller=true");
                showLockTaskToast();
                return;
            }
        }
        if (taskRecord == null) {
            throw new IllegalArgumentException("can't stop LockTask for null task");
        }
        if (i != taskRecord.mLockTaskUid && (taskRecord.mLockTaskUid != 0 || i != taskRecord.effectiveUid)) {
            throw new SecurityException("Invalid uid, expected " + taskRecord.mLockTaskUid + " callingUid=" + i + " effectiveUid=" + taskRecord.effectiveUid);
        }
        clearLockedTask(taskRecord);
    }

    void clearLockedTasks(String str) {
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.i(TAG_LOCKTASK, "clearLockedTasks: " + str);
        }
        if (!this.mLockTaskModeTasks.isEmpty()) {
            clearLockedTask(this.mLockTaskModeTasks.get(0));
        }
    }

    void clearLockedTask(TaskRecord taskRecord) {
        if (taskRecord == null || this.mLockTaskModeTasks.isEmpty()) {
            return;
        }
        if (taskRecord == this.mLockTaskModeTasks.get(0)) {
            for (int size = this.mLockTaskModeTasks.size() - 1; size > 0; size--) {
                clearLockedTask(this.mLockTaskModeTasks.get(size));
            }
        }
        removeLockedTask(taskRecord);
        if (this.mLockTaskModeTasks.isEmpty()) {
            return;
        }
        taskRecord.performClearTaskLocked();
        this.mSupervisor.resumeFocusedStackTopActivityLocked();
    }

    private void removeLockedTask(final TaskRecord taskRecord) {
        if (!this.mLockTaskModeTasks.remove(taskRecord)) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.d(TAG_LOCKTASK, "removeLockedTask: removed " + taskRecord);
        }
        if (this.mLockTaskModeTasks.isEmpty()) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.d(TAG_LOCKTASK, "removeLockedTask: task=" + taskRecord + " last task, reverting locktask mode. Callers=" + Debug.getCallers(3));
            }
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.performStopLockTask(taskRecord.userId);
                }
            });
        }
    }

    private void performStopLockTask(int i) {
        try {
            try {
                setStatusBarState(0, i);
                setKeyguardState(0, i);
                if (this.mLockTaskModeState == 2) {
                    lockKeyguardIfNeeded(i);
                }
                if (getDevicePolicyManager() != null) {
                    getDevicePolicyManager().notifyLockTaskModeChanged(false, (String) null, i);
                }
                if (this.mLockTaskModeState == 2) {
                    getStatusBarService().showPinningEnterExitToast(false);
                }
                this.mWindowManager.onLockTaskStateChanged(0);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } finally {
            this.mLockTaskModeState = 0;
        }
    }

    void showLockTaskToast() {
        if (this.mLockTaskModeState == 2) {
            try {
                getStatusBarService().showPinningEscapeToast();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to send pinning escape toast", e);
            }
        }
    }

    void startLockTaskMode(TaskRecord taskRecord, boolean z, int i) {
        if (!z) {
            taskRecord.mLockTaskUid = i;
            if (taskRecord.mLockTaskAuth == 1) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.w(TAG_LOCKTASK, "Mode default, asking user");
                }
                StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                if (statusBarManagerInternal != null) {
                    statusBarManagerInternal.showScreenPinningRequest(taskRecord.taskId);
                    return;
                }
                return;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, z ? "Locking pinned" : "Locking fully");
        }
        setLockTaskMode(taskRecord, z ? 2 : 1, "startLockTask", true);
    }

    private void setLockTaskMode(final TaskRecord taskRecord, final int i, String str, boolean z) {
        if (taskRecord.mLockTaskAuth == 0) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.w(TAG_LOCKTASK, "setLockTaskMode: Can't lock due to auth");
                return;
            }
            return;
        }
        if (isLockTaskModeViolation(taskRecord)) {
            Slog.e(TAG_LOCKTASK, "setLockTaskMode: Attempt to start an unauthorized lock task.");
            return;
        }
        final Intent intent = taskRecord.intent;
        if (this.mLockTaskModeTasks.isEmpty() && intent != null) {
            this.mSupervisor.mRecentTasks.onLockTaskModeStateChanged(i, taskRecord.userId);
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.performStartLockTask(intent.getComponent().getPackageName(), taskRecord.userId, i);
                }
            });
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            Slog.w(TAG_LOCKTASK, "setLockTaskMode: Locking to " + taskRecord + " Callers=" + Debug.getCallers(4));
        }
        if (!this.mLockTaskModeTasks.contains(taskRecord)) {
            this.mLockTaskModeTasks.add(taskRecord);
        }
        if (taskRecord.mLockTaskUid == -1) {
            taskRecord.mLockTaskUid = taskRecord.effectiveUid;
        }
        if (z) {
            this.mSupervisor.findTaskToMoveToFront(taskRecord, 0, null, str, i != 0);
            this.mSupervisor.resumeFocusedStackTopActivityLocked();
            this.mWindowManager.executeAppTransition();
        } else if (i != 0) {
            this.mSupervisor.handleNonResizableTaskIfNeeded(taskRecord, 0, 0, taskRecord.getStack(), true);
        }
    }

    private void performStartLockTask(String str, int i, int i2) {
        if (i2 == 2) {
            try {
                getStatusBarService().showPinningEnterExitToast(true);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        this.mWindowManager.onLockTaskStateChanged(i2);
        this.mLockTaskModeState = i2;
        setStatusBarState(i2, i);
        setKeyguardState(i2, i);
        if (getDevicePolicyManager() != null) {
            getDevicePolicyManager().notifyLockTaskModeChanged(true, str, i);
        }
    }

    void updateLockTaskPackages(int i, String[] strArr) {
        this.mLockTaskPackages.put(i, strArr);
        boolean z = true;
        boolean z2 = false;
        for (int size = this.mLockTaskModeTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mLockTaskModeTasks.get(size);
            boolean z3 = taskRecord.mLockTaskAuth == 2 || taskRecord.mLockTaskAuth == 3;
            taskRecord.setLockTaskAuth();
            boolean z4 = taskRecord.mLockTaskAuth == 2 || taskRecord.mLockTaskAuth == 3;
            if (this.mLockTaskModeState == 1 && taskRecord.userId == i && z3 && !z4) {
                if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                    Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: removing " + taskRecord + " mLockTaskAuth()=" + taskRecord.lockTaskAuthToString());
                }
                removeLockedTask(taskRecord);
                taskRecord.performClearTaskLocked();
                z2 = true;
            }
        }
        for (int childCount = this.mSupervisor.getChildCount() - 1; childCount >= 0; childCount--) {
            this.mSupervisor.getChildAt(childCount).onLockTaskPackagesUpdated();
        }
        ActivityRecord activityRecord = this.mSupervisor.topRunningActivityLocked();
        TaskRecord task = activityRecord != null ? activityRecord.getTask() : null;
        if (this.mLockTaskModeTasks.isEmpty() && task != null && task.mLockTaskAuth == 2) {
            if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
                Slog.d(TAG_LOCKTASK, "onLockTaskPackagesUpdated: starting new locktask task=" + task);
            }
            setLockTaskMode(task, 1, "package updated", false);
        } else {
            z = z2;
        }
        if (z) {
            this.mSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    boolean isPackageWhitelisted(int i, String str) {
        String[] strArr;
        if (str == null || (strArr = this.mLockTaskPackages.get(i)) == null) {
            return false;
        }
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    void updateLockTaskFeatures(final int i, int i2) {
        if (i2 == getLockTaskFeaturesForUser(i)) {
            return;
        }
        this.mLockTaskFeatures.put(i, i2);
        if (!this.mLockTaskModeTasks.isEmpty() && i == this.mLockTaskModeTasks.get(0).userId) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    LockTaskController.lambda$updateLockTaskFeatures$2(this.f$0, i);
                }
            });
        }
    }

    public static void lambda$updateLockTaskFeatures$2(LockTaskController lockTaskController, int i) {
        if (lockTaskController.mLockTaskModeState == 1) {
            lockTaskController.setStatusBarState(lockTaskController.mLockTaskModeState, i);
            lockTaskController.setKeyguardState(lockTaskController.mLockTaskModeState, i);
        }
    }

    private void setStatusBarState(int i, int i2) {
        int iIntValue;
        IStatusBarService statusBarService = getStatusBarService();
        if (statusBarService == null) {
            Slog.e(TAG, "Can't find StatusBarService");
            return;
        }
        int iIntValue2 = 0;
        if (i == 2) {
            iIntValue2 = STATUS_BAR_MASK_PINNED;
            iIntValue = 0;
        } else if (i == 1) {
            Pair<Integer, Integer> statusBarDisableFlags = getStatusBarDisableFlags(getLockTaskFeaturesForUser(i2));
            iIntValue2 = ((Integer) statusBarDisableFlags.first).intValue();
            iIntValue = ((Integer) statusBarDisableFlags.second).intValue();
        } else {
            iIntValue = 0;
        }
        try {
            statusBarService.disable(iIntValue2, this.mToken, this.mContext.getPackageName());
            statusBarService.disable2(iIntValue, this.mToken, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set status bar flags", e);
        }
    }

    private void setKeyguardState(int i, int i2) {
        if (i == 0) {
            this.mWindowManager.reenableKeyguard(this.mToken);
            return;
        }
        if (i == 1) {
            if (isKeyguardAllowed(i2)) {
                this.mWindowManager.reenableKeyguard(this.mToken);
                return;
            } else if (this.mWindowManager.isKeyguardLocked() && !this.mWindowManager.isKeyguardSecure()) {
                this.mWindowManager.dismissKeyguard(new AnonymousClass1(), null);
                return;
            } else {
                this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG);
                return;
            }
        }
        this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG);
    }

    class AnonymousClass1 extends IKeyguardDismissCallback.Stub {
        AnonymousClass1() {
        }

        public void onDismissError() throws RemoteException {
            Slog.i(LockTaskController.TAG, "setKeyguardState: failed to dismiss keyguard");
        }

        public void onDismissSucceeded() throws RemoteException {
            LockTaskController.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    LockTaskController.AnonymousClass1 anonymousClass1 = this.f$0;
                    LockTaskController.this.mWindowManager.disableKeyguard(LockTaskController.this.mToken, LockTaskController.LOCK_TASK_TAG);
                }
            });
        }

        public void onDismissCancelled() throws RemoteException {
            Slog.i(LockTaskController.TAG, "setKeyguardState: dismiss cancelled");
        }
    }

    private void lockKeyguardIfNeeded(int i) {
        if (shouldLockKeyguard(i)) {
            this.mWindowManager.lockNow(null);
            this.mWindowManager.dismissKeyguard(null, null);
            getLockPatternUtils().requireCredentialEntry(-1);
        }
    }

    private boolean shouldLockKeyguard(int i) {
        try {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_to_app_exit_locked", -2) != 0;
        } catch (Settings.SettingNotFoundException e) {
            EventLog.writeEvent(1397638484, "127605586", -1, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            return getLockPatternUtils().isSecure(i);
        }
    }

    @VisibleForTesting
    Pair<Integer, Integer> getStatusBarDisableFlags(int i) {
        int i2 = 67043328;
        int i3 = 31;
        for (int size = STATUS_BAR_FLAG_MAP_LOCKED.size() - 1; size >= 0; size--) {
            Pair<Integer, Integer> pairValueAt = STATUS_BAR_FLAG_MAP_LOCKED.valueAt(size);
            if ((STATUS_BAR_FLAG_MAP_LOCKED.keyAt(size) & i) != 0) {
                i2 &= ~((Integer) pairValueAt.first).intValue();
                i3 &= ~((Integer) pairValueAt.second).intValue();
            }
        }
        return new Pair<>(Integer.valueOf(STATUS_BAR_MASK_LOCKED & i2), Integer.valueOf(i3));
    }

    private int getLockTaskFeaturesForUser(int i) {
        return this.mLockTaskFeatures.get(i, 0);
    }

    private IStatusBarService getStatusBarService() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
            if (this.mStatusBarService == null) {
                Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
            }
        }
        return this.mStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
            if (this.mDevicePolicyManager == null) {
                Slog.w(TAG, "warning: no DEVICE_POLICY_SERVICE");
            }
        }
        return this.mDevicePolicyManager;
    }

    private LockPatternUtils getLockPatternUtils() {
        if (this.mLockPatternUtils == null) {
            return new LockPatternUtils(this.mContext);
        }
        return this.mLockPatternUtils;
    }

    private TelecomManager getTelecomManager() {
        if (this.mTelecomManager == null) {
            return (TelecomManager) this.mContext.getSystemService(TelecomManager.class);
        }
        return this.mTelecomManager;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "LockTaskController");
        String str2 = str + "  ";
        printWriter.println(str2 + "mLockTaskModeState=" + lockTaskModeToString());
        StringBuilder sb = new StringBuilder();
        sb.append(str2);
        sb.append("mLockTaskModeTasks=");
        printWriter.println(sb.toString());
        for (int i = 0; i < this.mLockTaskModeTasks.size(); i++) {
            printWriter.println(str2 + "  #" + i + " " + this.mLockTaskModeTasks.get(i));
        }
        printWriter.println(str2 + "mLockTaskPackages (userId:packages)=");
        for (int i2 = 0; i2 < this.mLockTaskPackages.size(); i2++) {
            printWriter.println(str2 + "  u" + this.mLockTaskPackages.keyAt(i2) + ":" + Arrays.toString(this.mLockTaskPackages.valueAt(i2)));
        }
    }

    private String lockTaskModeToString() {
        switch (this.mLockTaskModeState) {
            case 0:
                return "NONE";
            case 1:
                return "LOCKED";
            case 2:
                return "PINNED";
            default:
                return "unknown=" + this.mLockTaskModeState;
        }
    }
}
