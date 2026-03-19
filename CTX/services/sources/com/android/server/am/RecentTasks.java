package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.am.TaskRecord;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class RecentTasks {
    private static final int DEFAULT_INITIAL_CAPACITY = 5;
    private static final boolean MOVE_AFFILIATED_TASKS_TO_FRONT = false;
    private static final boolean TRIMMED = true;
    private long mActiveTasksSessionDurationMs;
    private final ArrayList<Callbacks> mCallbacks;
    private int mGlobalMaxNumTasks;
    private boolean mHasVisibleRecentTasks;
    private int mMaxNumVisibleTasks;
    private int mMinNumVisibleTasks;
    private final SparseArray<SparseBooleanArray> mPersistedTaskIds;
    private ComponentName mRecentsComponent;
    private int mRecentsUid;
    private final ActivityManagerService mService;
    private final TaskPersister mTaskPersister;
    private final ArrayList<TaskRecord> mTasks;
    private final HashMap<ComponentName, ActivityInfo> mTmpAvailActCache;
    private final HashMap<String, ApplicationInfo> mTmpAvailAppCache;
    private final SparseBooleanArray mTmpQuietProfileUserIds;
    private final ArrayList<TaskRecord> mTmpRecents;
    private final TaskRecord.TaskActivitiesReport mTmpReport;
    private final UserController mUserController;
    private final SparseBooleanArray mUsersWithRecentsLoaded;
    private static final String TAG = "ActivityManager";
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static final Comparator<TaskRecord> TASK_ID_COMPARATOR = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return RecentTasks.lambda$static$0((TaskRecord) obj, (TaskRecord) obj2);
        }
    };
    private static final ActivityInfo NO_ACTIVITY_INFO_TOKEN = new ActivityInfo();
    private static final ApplicationInfo NO_APPLICATION_INFO_TOKEN = new ApplicationInfo();

    interface Callbacks {
        void onRecentTaskAdded(TaskRecord taskRecord);

        void onRecentTaskRemoved(TaskRecord taskRecord, boolean z);
    }

    static int lambda$static$0(TaskRecord taskRecord, TaskRecord taskRecord2) {
        return taskRecord2.taskId - taskRecord.taskId;
    }

    @VisibleForTesting
    RecentTasks(ActivityManagerService activityManagerService, TaskPersister taskPersister, UserController userController) {
        this.mRecentsUid = -1;
        this.mRecentsComponent = null;
        this.mUsersWithRecentsLoaded = new SparseBooleanArray(5);
        this.mPersistedTaskIds = new SparseArray<>(5);
        this.mTasks = new ArrayList<>();
        this.mCallbacks = new ArrayList<>();
        this.mTmpRecents = new ArrayList<>();
        this.mTmpAvailActCache = new HashMap<>();
        this.mTmpAvailAppCache = new HashMap<>();
        this.mTmpQuietProfileUserIds = new SparseBooleanArray();
        this.mTmpReport = new TaskRecord.TaskActivitiesReport();
        this.mService = activityManagerService;
        this.mUserController = userController;
        this.mTaskPersister = taskPersister;
        this.mGlobalMaxNumTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mHasVisibleRecentTasks = true;
    }

    RecentTasks(ActivityManagerService activityManagerService, ActivityStackSupervisor activityStackSupervisor) {
        this.mRecentsUid = -1;
        this.mRecentsComponent = null;
        this.mUsersWithRecentsLoaded = new SparseBooleanArray(5);
        this.mPersistedTaskIds = new SparseArray<>(5);
        this.mTasks = new ArrayList<>();
        this.mCallbacks = new ArrayList<>();
        this.mTmpRecents = new ArrayList<>();
        this.mTmpAvailActCache = new HashMap<>();
        this.mTmpAvailAppCache = new HashMap<>();
        this.mTmpQuietProfileUserIds = new SparseBooleanArray();
        this.mTmpReport = new TaskRecord.TaskActivitiesReport();
        File dataSystemDirectory = Environment.getDataSystemDirectory();
        Resources resources = activityManagerService.mContext.getResources();
        this.mService = activityManagerService;
        this.mUserController = activityManagerService.mUserController;
        this.mTaskPersister = new TaskPersister(dataSystemDirectory, activityStackSupervisor, activityManagerService, this);
        this.mGlobalMaxNumTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mHasVisibleRecentTasks = resources.getBoolean(R.^attr-private.itemLayout);
        loadParametersFromResources(resources);
    }

    @VisibleForTesting
    void setParameters(int i, int i2, long j) {
        this.mMinNumVisibleTasks = i;
        this.mMaxNumVisibleTasks = i2;
        this.mActiveTasksSessionDurationMs = j;
    }

    @VisibleForTesting
    void setGlobalMaxNumTasks(int i) {
        this.mGlobalMaxNumTasks = i;
    }

    @VisibleForTesting
    void loadParametersFromResources(Resources resources) {
        long millis;
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mMinNumVisibleTasks = resources.getInteger(R.integer.config_defaultNotificationVibrationIntensity);
            this.mMaxNumVisibleTasks = resources.getInteger(R.integer.config_defaultMediaVibrationIntensity);
        } else if (SystemProperties.getBoolean("ro.recents.grid", false)) {
            this.mMinNumVisibleTasks = resources.getInteger(R.integer.config_defaultNotificationLedOn);
            this.mMaxNumVisibleTasks = resources.getInteger(R.integer.config_defaultMaxDurationBetweenUndimsMillis);
        } else {
            this.mMinNumVisibleTasks = resources.getInteger(R.integer.config_defaultNotificationLedOff);
            this.mMaxNumVisibleTasks = resources.getInteger(R.integer.config_defaultKeyboardVibrationIntensity);
        }
        int integer = resources.getInteger(R.integer.auto_data_switch_validation_max_retry);
        if (integer > 0) {
            millis = TimeUnit.HOURS.toMillis(integer);
        } else {
            millis = -1;
        }
        this.mActiveTasksSessionDurationMs = millis;
    }

    void loadRecentsComponent(Resources resources) {
        ComponentName componentNameUnflattenFromString;
        String string = resources.getString(R.string.app_blocked_message);
        if (!TextUtils.isEmpty(string) && (componentNameUnflattenFromString = ComponentName.unflattenFromString(string)) != null) {
            try {
                ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(componentNameUnflattenFromString.getPackageName(), 0, this.mService.mContext.getUserId());
                if (applicationInfo != null) {
                    this.mRecentsUid = applicationInfo.uid;
                    this.mRecentsComponent = componentNameUnflattenFromString;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not load application info for recents component: " + componentNameUnflattenFromString);
            }
        }
    }

    boolean isCallerRecents(int i) {
        return UserHandle.isSameApp(i, this.mRecentsUid);
    }

    boolean isRecentsComponent(ComponentName componentName, int i) {
        return componentName.equals(this.mRecentsComponent) && UserHandle.isSameApp(i, this.mRecentsUid);
    }

    boolean isRecentsComponentHomeActivity(int i) {
        ComponentName defaultHomeActivity = this.mService.getPackageManagerInternalLocked().getDefaultHomeActivity(i);
        return (defaultHomeActivity == null || this.mRecentsComponent == null || !defaultHomeActivity.getPackageName().equals(this.mRecentsComponent.getPackageName())) ? false : true;
    }

    ComponentName getRecentsComponent() {
        return this.mRecentsComponent;
    }

    int getRecentsComponentUid() {
        return this.mRecentsUid;
    }

    void registerCallback(Callbacks callbacks) {
        this.mCallbacks.add(callbacks);
    }

    void unregisterCallback(Callbacks callbacks) {
        this.mCallbacks.remove(callbacks);
    }

    private void notifyTaskAdded(TaskRecord taskRecord) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onRecentTaskAdded(taskRecord);
        }
    }

    private void notifyTaskRemoved(TaskRecord taskRecord, boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onRecentTaskRemoved(taskRecord, z);
        }
    }

    void loadUserRecentsLocked(int i) {
        if (this.mUsersWithRecentsLoaded.get(i)) {
            return;
        }
        loadPersistedTaskIdsForUserLocked(i);
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        for (TaskRecord taskRecord : this.mTasks) {
            if (taskRecord.userId == i && shouldPersistTaskLocked(taskRecord)) {
                sparseBooleanArray.put(taskRecord.taskId, true);
            }
        }
        Slog.i(TAG, "Loading recents for user " + i + " into memory.");
        this.mTasks.addAll(this.mTaskPersister.restoreTasksForUserLocked(i, sparseBooleanArray));
        cleanupLocked(i);
        this.mUsersWithRecentsLoaded.put(i, true);
        if (sparseBooleanArray.size() > 0) {
            syncPersistentTaskIdsLocked();
        }
    }

    private void loadPersistedTaskIdsForUserLocked(int i) {
        if (this.mPersistedTaskIds.get(i) == null) {
            this.mPersistedTaskIds.put(i, this.mTaskPersister.loadPersistedTaskIdsForUser(i));
            Slog.i(TAG, "Loaded persisted task ids for user " + i);
        }
    }

    boolean containsTaskId(int i, int i2) {
        loadPersistedTaskIdsForUserLocked(i2);
        return this.mPersistedTaskIds.get(i2).get(i);
    }

    SparseBooleanArray getTaskIdsForUser(int i) {
        loadPersistedTaskIdsForUserLocked(i);
        return this.mPersistedTaskIds.get(i);
    }

    void notifyTaskPersisterLocked(TaskRecord taskRecord, boolean z) {
        ActivityStack stack = taskRecord != null ? taskRecord.getStack() : null;
        if (stack != null && stack.isHomeOrRecentsStack()) {
            return;
        }
        syncPersistentTaskIdsLocked();
        this.mTaskPersister.wakeup(taskRecord, z);
    }

    private void syncPersistentTaskIdsLocked() {
        for (int size = this.mPersistedTaskIds.size() - 1; size >= 0; size--) {
            if (this.mUsersWithRecentsLoaded.get(this.mPersistedTaskIds.keyAt(size))) {
                this.mPersistedTaskIds.valueAt(size).clear();
            }
        }
        for (int size2 = this.mTasks.size() - 1; size2 >= 0; size2--) {
            TaskRecord taskRecord = this.mTasks.get(size2);
            if (shouldPersistTaskLocked(taskRecord)) {
                if (this.mPersistedTaskIds.get(taskRecord.userId) == null) {
                    Slog.wtf(TAG, "No task ids found for userId " + taskRecord.userId + ". task=" + taskRecord + " mPersistedTaskIds=" + this.mPersistedTaskIds);
                    this.mPersistedTaskIds.put(taskRecord.userId, new SparseBooleanArray());
                }
                this.mPersistedTaskIds.get(taskRecord.userId).put(taskRecord.taskId, true);
            }
        }
    }

    private static boolean shouldPersistTaskLocked(TaskRecord taskRecord) {
        ActivityStack stack = taskRecord.getStack();
        return taskRecord.isPersistable && (stack == null || !stack.isHomeOrRecentsStack());
    }

    void onSystemReadyLocked() {
        loadRecentsComponent(this.mService.mContext.getResources());
        this.mTasks.clear();
        this.mTaskPersister.startPersisting();
    }

    Bitmap getTaskDescriptionIcon(String str) {
        return this.mTaskPersister.getTaskDescriptionIcon(str);
    }

    void saveImage(Bitmap bitmap, String str) {
        this.mTaskPersister.saveImage(bitmap, str);
    }

    void flush() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                syncPersistentTaskIdsLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mTaskPersister.flush();
    }

    int[] usersWithRecentsLoadedLocked() {
        int[] iArr = new int[this.mUsersWithRecentsLoaded.size()];
        int i = 0;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            int iKeyAt = this.mUsersWithRecentsLoaded.keyAt(i2);
            if (this.mUsersWithRecentsLoaded.valueAt(i2)) {
                iArr[i] = iKeyAt;
                i++;
            }
        }
        if (i < iArr.length) {
            return Arrays.copyOf(iArr, i);
        }
        return iArr;
    }

    void unloadUserDataFromMemoryLocked(int i) {
        if (this.mUsersWithRecentsLoaded.get(i)) {
            Slog.i(TAG, "Unloading recents for user " + i + " from memory.");
            this.mUsersWithRecentsLoaded.delete(i);
            removeTasksForUserLocked(i);
        }
        this.mPersistedTaskIds.delete(i);
        this.mTaskPersister.unloadUserDataFromMemory(i);
    }

    private void removeTasksForUserLocked(int i) {
        if (i <= 0) {
            Slog.i(TAG, "Can't remove recent task on user " + i);
            return;
        }
        for (int size = this.mTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTasks.get(size);
            if (taskRecord.userId == i) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.i(TAG_TASKS, "remove RecentTask " + taskRecord + " when finishing user" + i);
                }
                remove(this.mTasks.get(size));
            }
        }
    }

    void onPackagesSuspendedChanged(String[] strArr, boolean z, int i) {
        HashSet hashSetNewHashSet = Sets.newHashSet(strArr);
        for (int size = this.mTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTasks.get(size);
            if (taskRecord.realActivity != null && hashSetNewHashSet.contains(taskRecord.realActivity.getPackageName()) && taskRecord.userId == i && taskRecord.realActivitySuspended != z) {
                taskRecord.realActivitySuspended = z;
                if (z) {
                    this.mService.mStackSupervisor.removeTaskByIdLocked(taskRecord.taskId, false, true, "suspended-package");
                }
                notifyTaskPersisterLocked(taskRecord, false);
            }
        }
    }

    void onLockTaskModeStateChanged(int i, int i2) {
        if (i == 1) {
            for (int size = this.mTasks.size() - 1; size >= 0; size--) {
                TaskRecord taskRecord = this.mTasks.get(size);
                if (taskRecord.userId == i2 && !this.mService.getLockTaskController().isTaskWhitelisted(taskRecord)) {
                    remove(taskRecord);
                }
            }
        }
    }

    void removeTasksByPackageName(String str, int i) {
        for (int size = this.mTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTasks.get(size);
            String packageName = taskRecord.getBaseIntent().getComponent().getPackageName();
            if (taskRecord.userId == i && packageName.equals(str)) {
                this.mService.mStackSupervisor.removeTaskByIdLocked(taskRecord.taskId, true, true, "remove-package-task");
            }
        }
    }

    void cleanupDisabledPackageTasksLocked(String str, Set<String> set, int i) {
        for (int size = this.mTasks.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = this.mTasks.get(size);
            if (i == -1 || taskRecord.userId == i) {
                ComponentName component = taskRecord.intent != null ? taskRecord.intent.getComponent() : null;
                if (component != null && component.getPackageName().equals(str) && (set == null || set.contains(component.getClassName()))) {
                    this.mService.mStackSupervisor.removeTaskByIdLocked(taskRecord.taskId, false, true, "disabled-package");
                }
            }
        }
    }

    void cleanupLocked(int i) {
        int iProcessNextAffiliateChainLocked;
        int size = this.mTasks.size();
        if (size == 0) {
            return;
        }
        this.mTmpAvailActCache.clear();
        this.mTmpAvailAppCache.clear();
        IPackageManager packageManager = AppGlobals.getPackageManager();
        int i2 = size - 1;
        while (true) {
            iProcessNextAffiliateChainLocked = 0;
            if (i2 < 0) {
                break;
            }
            TaskRecord taskRecord = this.mTasks.get(i2);
            if (i == -1 || taskRecord.userId == i) {
                if (taskRecord.autoRemoveRecents && taskRecord.getTopActivity() == null) {
                    this.mTasks.remove(i2);
                    notifyTaskRemoved(taskRecord, false);
                    Slog.w(TAG, "Removing auto-remove without activity: " + taskRecord);
                } else if (taskRecord.realActivity != null) {
                    ActivityInfo activityInfo = this.mTmpAvailActCache.get(taskRecord.realActivity);
                    if (activityInfo == null) {
                        try {
                            activityInfo = packageManager.getActivityInfo(taskRecord.realActivity, 268436480, i);
                            if (activityInfo == null) {
                                activityInfo = NO_ACTIVITY_INFO_TOKEN;
                            }
                            this.mTmpAvailActCache.put(taskRecord.realActivity, activityInfo);
                            if (activityInfo != NO_ACTIVITY_INFO_TOKEN) {
                                ApplicationInfo applicationInfo = this.mTmpAvailAppCache.get(taskRecord.realActivity.getPackageName());
                                if (applicationInfo == null) {
                                    try {
                                        applicationInfo = packageManager.getApplicationInfo(taskRecord.realActivity.getPackageName(), 8192, i);
                                        if (applicationInfo == null) {
                                            applicationInfo = NO_APPLICATION_INFO_TOKEN;
                                        }
                                        this.mTmpAvailAppCache.put(taskRecord.realActivity.getPackageName(), applicationInfo);
                                        if (applicationInfo != NO_APPLICATION_INFO_TOKEN || (applicationInfo.flags & DumpState.DUMP_VOLUMES) == 0) {
                                            this.mTasks.remove(i2);
                                            notifyTaskRemoved(taskRecord, false);
                                            Slog.w(TAG, "Removing no longer valid recent: " + taskRecord);
                                        } else {
                                            if (ActivityManagerDebugConfig.DEBUG_RECENTS && taskRecord.isAvailable) {
                                                Slog.d(TAG_RECENTS, "Making recent unavailable: " + taskRecord);
                                            }
                                            taskRecord.isAvailable = false;
                                        }
                                    } catch (RemoteException e) {
                                    }
                                } else if (applicationInfo != NO_APPLICATION_INFO_TOKEN) {
                                    this.mTasks.remove(i2);
                                    notifyTaskRemoved(taskRecord, false);
                                    Slog.w(TAG, "Removing no longer valid recent: " + taskRecord);
                                }
                            } else if (!activityInfo.enabled || !activityInfo.applicationInfo.enabled || (activityInfo.applicationInfo.flags & DumpState.DUMP_VOLUMES) == 0) {
                                if (ActivityManagerDebugConfig.DEBUG_RECENTS && taskRecord.isAvailable) {
                                    Slog.d(TAG_RECENTS, "Making recent unavailable: " + taskRecord + " (enabled=" + activityInfo.enabled + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.applicationInfo.enabled + " flags=" + Integer.toHexString(activityInfo.applicationInfo.flags) + ")");
                                }
                                taskRecord.isAvailable = false;
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_RECENTS && !taskRecord.isAvailable) {
                                    Slog.d(TAG_RECENTS, "Making recent available: " + taskRecord);
                                }
                                taskRecord.isAvailable = true;
                            }
                        } catch (RemoteException e2) {
                        }
                    } else if (activityInfo != NO_ACTIVITY_INFO_TOKEN) {
                    }
                }
            }
            i2--;
        }
        int size2 = this.mTasks.size();
        while (iProcessNextAffiliateChainLocked < size2) {
            iProcessNextAffiliateChainLocked = processNextAffiliateChainLocked(iProcessNextAffiliateChainLocked);
        }
    }

    private boolean canAddTaskWithoutTrim(TaskRecord taskRecord) {
        return findRemoveIndexForAddTask(taskRecord) == -1;
    }

    ArrayList<IBinder> getAppTasksList(int i, String str) {
        Intent baseIntent;
        ArrayList<IBinder> arrayList = new ArrayList<>();
        int size = this.mTasks.size();
        for (int i2 = 0; i2 < size; i2++) {
            TaskRecord taskRecord = this.mTasks.get(i2);
            if (taskRecord.effectiveUid == i && (baseIntent = taskRecord.getBaseIntent()) != null && str.equals(baseIntent.getComponent().getPackageName())) {
                arrayList.add(new AppTaskImpl(this.mService, createRecentTaskInfo(taskRecord).persistentId, i).asBinder());
            }
        }
        return arrayList;
    }

    ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int i, int i2, boolean z, boolean z2, int i3, int i4) {
        boolean z3 = (i2 & 1) != 0;
        if (!this.mService.isUserRunning(i3, 4)) {
            Slog.i(TAG, "user " + i3 + " is still locked. Cannot load recents");
            return ParceledListSlice.emptyList();
        }
        loadUserRecentsLocked(i3);
        Set<Integer> profileIds = this.mUserController.getProfileIds(i3);
        profileIds.add(Integer.valueOf(i3));
        ArrayList arrayList = new ArrayList();
        int size = this.mTasks.size();
        int i5 = 0;
        for (int i6 = 0; i6 < size; i6++) {
            TaskRecord taskRecord = this.mTasks.get(i6);
            if (isVisibleRecentTask(taskRecord)) {
                i5++;
                if (isInVisibleRange(taskRecord, i5) && arrayList.size() < i) {
                    if (!profileIds.contains(Integer.valueOf(taskRecord.userId))) {
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, not user: " + taskRecord);
                        }
                    } else if (taskRecord.realActivitySuspended) {
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, activity suspended: " + taskRecord);
                        }
                    } else if (i6 == 0 || z3 || taskRecord.intent == null || (taskRecord.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0) {
                        if (!z && !taskRecord.isActivityTypeHome() && taskRecord.effectiveUid != i4) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, not allowed: " + taskRecord);
                            }
                        } else if (taskRecord.autoRemoveRecents && taskRecord.getTopActivity() == null) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, auto-remove without activity: " + taskRecord);
                            }
                        } else if ((i2 & 2) != 0 && !taskRecord.isAvailable) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, unavail real act: " + taskRecord);
                            }
                        } else if (!taskRecord.mUserSetupComplete) {
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "Skipping, user setup not complete: " + taskRecord);
                            }
                        } else {
                            ActivityManager.RecentTaskInfo recentTaskInfoCreateRecentTaskInfo = createRecentTaskInfo(taskRecord);
                            if (!z2) {
                                recentTaskInfoCreateRecentTaskInfo.baseIntent.replaceExtras((Bundle) null);
                            }
                            arrayList.add(recentTaskInfoCreateRecentTaskInfo);
                        }
                    }
                }
            }
        }
        return new ParceledListSlice<>(arrayList);
    }

    void getPersistableTaskIds(ArraySet<Integer> arraySet) {
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            TaskRecord taskRecord = this.mTasks.get(i);
            ActivityStack stack = taskRecord.getStack();
            if ((taskRecord.isPersistable || taskRecord.inRecents) && (stack == null || !stack.isHomeOrRecentsStack())) {
                arraySet.add(Integer.valueOf(taskRecord.taskId));
            }
        }
    }

    @VisibleForTesting
    ArrayList<TaskRecord> getRawTasks() {
        return this.mTasks;
    }

    SparseBooleanArray getRecentTaskIds() {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        int size = this.mTasks.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            TaskRecord taskRecord = this.mTasks.get(i2);
            if (isVisibleRecentTask(taskRecord)) {
                i++;
                if (isInVisibleRange(taskRecord, i)) {
                    sparseBooleanArray.put(taskRecord.taskId, true);
                }
            }
        }
        return sparseBooleanArray;
    }

    TaskRecord getTask(int i) {
        int size = this.mTasks.size();
        for (int i2 = 0; i2 < size; i2++) {
            TaskRecord taskRecord = this.mTasks.get(i2);
            if (taskRecord.taskId == i) {
                return taskRecord;
            }
        }
        return null;
    }

    void add(TaskRecord taskRecord) {
        boolean z;
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "add: task=" + taskRecord);
        }
        boolean z2 = true;
        boolean z3 = (taskRecord.mAffiliatedTaskId == taskRecord.taskId && taskRecord.mNextAffiliateTaskId == -1 && taskRecord.mPrevAffiliateTaskId == -1) ? false : true;
        int size = this.mTasks.size();
        if (taskRecord.voiceSession != null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: not adding voice interaction " + taskRecord);
                return;
            }
            return;
        }
        if (!z3 && size > 0 && this.mTasks.get(0) == taskRecord) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: already at top: " + taskRecord);
                return;
            }
            return;
        }
        if (z3 && size > 0 && taskRecord.inRecents && taskRecord.mAffiliatedTaskId == this.mTasks.get(0).mAffiliatedTaskId) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: affiliated " + this.mTasks.get(0) + " at top when adding " + taskRecord);
                return;
            }
            return;
        }
        if (taskRecord.inRecents) {
            int iIndexOf = this.mTasks.indexOf(taskRecord);
            if (iIndexOf >= 0) {
                this.mTasks.remove(iIndexOf);
                this.mTasks.add(0, taskRecord);
                notifyTaskPersisterLocked(taskRecord, false);
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: moving to top " + taskRecord + " from " + iIndexOf);
                    return;
                }
                return;
            }
            Slog.wtf(TAG, "Task with inRecent not in recents: " + taskRecord);
            z = true;
        } else {
            z = false;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: trimming tasks for " + taskRecord);
        }
        removeForAddTask(taskRecord);
        taskRecord.inRecents = true;
        if (!z3 || z) {
            this.mTasks.add(0, taskRecord);
            notifyTaskAdded(taskRecord);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: adding " + taskRecord);
            }
        } else {
            if (z3) {
                TaskRecord taskRecord2 = taskRecord.mNextAffiliate;
                if (taskRecord2 == null) {
                    taskRecord2 = taskRecord.mPrevAffiliate;
                }
                if (taskRecord2 != null) {
                    int iIndexOf2 = this.mTasks.indexOf(taskRecord2);
                    if (iIndexOf2 >= 0) {
                        if (taskRecord2 == taskRecord.mNextAffiliate) {
                            iIndexOf2++;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "addRecent: new affiliated task added at " + iIndexOf2 + ": " + taskRecord);
                        }
                        this.mTasks.add(iIndexOf2, taskRecord);
                        notifyTaskAdded(taskRecord);
                        if (moveAffiliatedTasksToFront(taskRecord, iIndexOf2)) {
                            return;
                        }
                    } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: couldn't find other affiliation " + taskRecord2);
                    }
                } else if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: adding affiliated task without next/prev:" + taskRecord);
                }
            }
            if (z2) {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: regrouping affiliations");
                }
                cleanupLocked(taskRecord.userId);
            }
            trimInactiveRecentTasks();
        }
        z2 = z;
        if (z2) {
        }
        trimInactiveRecentTasks();
    }

    boolean addToBottom(TaskRecord taskRecord) {
        if (!canAddTaskWithoutTrim(taskRecord)) {
            return false;
        }
        add(taskRecord);
        return true;
    }

    void remove(TaskRecord taskRecord) {
        this.mTasks.remove(taskRecord);
        notifyTaskRemoved(taskRecord, false);
    }

    private void trimInactiveRecentTasks() {
        int size = this.mTasks.size();
        while (size > this.mGlobalMaxNumTasks) {
            TaskRecord taskRecordRemove = this.mTasks.remove(size - 1);
            notifyTaskRemoved(taskRecordRemove, true);
            size--;
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming over max-recents task=" + taskRecordRemove + " max=" + this.mGlobalMaxNumTasks);
            }
        }
        int[] currentProfileIds = this.mUserController.getCurrentProfileIds();
        this.mTmpQuietProfileUserIds.clear();
        for (int i : currentProfileIds) {
            UserInfo userInfo = this.mUserController.getUserInfo(i);
            if (userInfo != null && userInfo.isManagedProfile() && userInfo.isQuietModeEnabled()) {
                this.mTmpQuietProfileUserIds.put(i, true);
            }
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "User: " + userInfo + " quiet=" + this.mTmpQuietProfileUserIds.get(i));
            }
        }
        int i2 = 0;
        int i3 = 0;
        while (i2 < this.mTasks.size()) {
            TaskRecord taskRecord = this.mTasks.get(i2);
            if (isActiveRecentTask(taskRecord, this.mTmpQuietProfileUserIds)) {
                if (!this.mHasVisibleRecentTasks) {
                    i2++;
                } else if (!isVisibleRecentTask(taskRecord)) {
                    i2++;
                } else {
                    i3++;
                    if (isInVisibleRange(taskRecord, i3) || !isTrimmable(taskRecord)) {
                        i2++;
                    } else if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                        Slog.d(TAG, "Trimming out-of-range visible task=" + taskRecord);
                    }
                }
            } else if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming inactive task=" + taskRecord);
            }
            this.mTasks.remove(taskRecord);
            notifyTaskRemoved(taskRecord, true);
            notifyTaskPersisterLocked(taskRecord, false);
        }
    }

    private boolean isActiveRecentTask(TaskRecord taskRecord, SparseBooleanArray sparseBooleanArray) {
        TaskRecord task;
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "isActiveRecentTask: task=" + taskRecord + " globalMax=" + this.mGlobalMaxNumTasks);
        }
        if (sparseBooleanArray.get(taskRecord.userId)) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "\tisQuietProfileTask=true");
            }
            return false;
        }
        if (taskRecord.mAffiliatedTaskId != -1 && taskRecord.mAffiliatedTaskId != taskRecord.taskId && (task = getTask(taskRecord.mAffiliatedTaskId)) != null && !isActiveRecentTask(task, sparseBooleanArray)) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "\taffiliatedWithTask=" + task + " is not active");
            }
            return false;
        }
        return true;
    }

    private boolean isVisibleRecentTask(TaskRecord taskRecord) {
        if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "isVisibleRecentTask: task=" + taskRecord + " minVis=" + this.mMinNumVisibleTasks + " maxVis=" + this.mMaxNumVisibleTasks + " sessionDuration=" + this.mActiveTasksSessionDurationMs + " inactiveDuration=" + taskRecord.getInactiveDuration() + " activityType=" + taskRecord.getActivityType() + " windowingMode=" + taskRecord.getWindowingMode() + " intentFlags=" + taskRecord.getBaseIntent().getFlags());
        }
        switch (taskRecord.getActivityType()) {
            case 2:
            case 3:
                return false;
            case 4:
                if ((taskRecord.getBaseIntent().getFlags() & DumpState.DUMP_VOLUMES) == 8388608) {
                    return false;
                }
                break;
        }
        switch (taskRecord.getWindowingMode()) {
            case 2:
                return false;
            case 3:
                if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    Slog.d(TAG, "\ttop=" + taskRecord.getStack().topTask());
                }
                ActivityStack stack = taskRecord.getStack();
                if (stack != null && stack.topTask() == taskRecord) {
                    return false;
                }
                break;
        }
        return taskRecord != this.mService.getLockTaskController().getRootTask();
    }

    private boolean isInVisibleRange(TaskRecord taskRecord, int i) {
        if ((taskRecord.getBaseIntent().getFlags() & DumpState.DUMP_VOLUMES) == 8388608) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "\texcludeFromRecents=true");
            }
            return i == 1;
        }
        if (this.mMinNumVisibleTasks < 0 || i > this.mMinNumVisibleTasks) {
            return this.mMaxNumVisibleTasks >= 0 ? i <= this.mMaxNumVisibleTasks : this.mActiveTasksSessionDurationMs > 0 && taskRecord.getInactiveDuration() <= this.mActiveTasksSessionDurationMs;
        }
        return true;
    }

    protected boolean isTrimmable(TaskRecord taskRecord) {
        ActivityStack stack = taskRecord.getStack();
        ActivityStack activityStack = this.mService.mStackSupervisor.mHomeStack;
        if (stack == null) {
            return true;
        }
        if (stack.getDisplay() != activityStack.getDisplay()) {
            return false;
        }
        ActivityDisplay display = stack.getDisplay();
        return display.getIndexOf(stack) < display.getIndexOf(activityStack);
    }

    private void removeForAddTask(TaskRecord taskRecord) {
        int iFindRemoveIndexForAddTask = findRemoveIndexForAddTask(taskRecord);
        if (iFindRemoveIndexForAddTask == -1) {
            return;
        }
        TaskRecord taskRecordRemove = this.mTasks.remove(iFindRemoveIndexForAddTask);
        if (taskRecordRemove != taskRecord) {
            notifyTaskRemoved(taskRecordRemove, false);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming task=" + taskRecordRemove + " for addition of task=" + taskRecord);
            }
        }
        notifyTaskPersisterLocked(taskRecordRemove, false);
    }

    private int findRemoveIndexForAddTask(TaskRecord taskRecord) {
        boolean z;
        int size = this.mTasks.size();
        Intent intent = taskRecord.intent;
        boolean z2 = intent != null && intent.isDocument();
        int i = taskRecord.maxRecents - 1;
        for (int i2 = 0; i2 < size; i2++) {
            TaskRecord taskRecord2 = this.mTasks.get(i2);
            if (taskRecord != taskRecord2) {
                if (hasCompatibleActivityTypeAndWindowingMode(taskRecord, taskRecord2) && taskRecord.userId == taskRecord2.userId) {
                    Intent intent2 = taskRecord2.intent;
                    boolean z3 = taskRecord.affinity != null && taskRecord.affinity.equals(taskRecord2.affinity);
                    boolean z4 = intent != null && intent.filterEquals(intent2);
                    int flags = intent.getFlags();
                    if ((268959744 & flags) == 0 || (flags & 134217728) == 0) {
                        z = false;
                    } else {
                        z = true;
                    }
                    boolean z5 = intent2 != null && intent2.isDocument();
                    boolean z6 = z2 && z5;
                    if (z3 || z4 || z6) {
                        if (z6) {
                            if (!((taskRecord.realActivity == null || taskRecord2.realActivity == null || !taskRecord.realActivity.equals(taskRecord2.realActivity)) ? false : true)) {
                                continue;
                            } else if (i > 0) {
                                i--;
                                if (!z4 || z) {
                                }
                            }
                        } else if (z2 || z5) {
                        }
                    }
                }
            }
            return i2;
        }
        return -1;
    }

    private int processNextAffiliateChainLocked(int i) {
        int i2;
        TaskRecord taskRecord = this.mTasks.get(i);
        int i3 = taskRecord.mAffiliatedTaskId;
        if (taskRecord.taskId == i3 && taskRecord.mPrevAffiliate == null && taskRecord.mNextAffiliate == null) {
            taskRecord.inRecents = true;
            return i + 1;
        }
        this.mTmpRecents.clear();
        for (int size = this.mTasks.size() - 1; size >= i; size--) {
            TaskRecord taskRecord2 = this.mTasks.get(size);
            if (taskRecord2.mAffiliatedTaskId == i3) {
                this.mTasks.remove(size);
                this.mTmpRecents.add(taskRecord2);
            }
        }
        Collections.sort(this.mTmpRecents, TASK_ID_COMPARATOR);
        TaskRecord taskRecord3 = this.mTmpRecents.get(0);
        taskRecord3.inRecents = true;
        if (taskRecord3.mNextAffiliate != null) {
            Slog.w(TAG, "Link error 1 first.next=" + taskRecord3.mNextAffiliate);
            taskRecord3.setNextAffiliate(null);
            notifyTaskPersisterLocked(taskRecord3, false);
        }
        int size2 = this.mTmpRecents.size();
        int i4 = 0;
        while (true) {
            i2 = size2 - 1;
            if (i4 >= i2) {
                break;
            }
            TaskRecord taskRecord4 = this.mTmpRecents.get(i4);
            i4++;
            TaskRecord taskRecord5 = this.mTmpRecents.get(i4);
            if (taskRecord4.mPrevAffiliate != taskRecord5) {
                Slog.w(TAG, "Link error 2 next=" + taskRecord4 + " prev=" + taskRecord4.mPrevAffiliate + " setting prev=" + taskRecord5);
                taskRecord4.setPrevAffiliate(taskRecord5);
                notifyTaskPersisterLocked(taskRecord4, false);
            }
            if (taskRecord5.mNextAffiliate != taskRecord4) {
                Slog.w(TAG, "Link error 3 prev=" + taskRecord5 + " next=" + taskRecord5.mNextAffiliate + " setting next=" + taskRecord4);
                taskRecord5.setNextAffiliate(taskRecord4);
                notifyTaskPersisterLocked(taskRecord5, false);
            }
            taskRecord5.inRecents = true;
        }
        TaskRecord taskRecord6 = this.mTmpRecents.get(i2);
        if (taskRecord6.mPrevAffiliate != null) {
            Slog.w(TAG, "Link error 4 last.prev=" + taskRecord6.mPrevAffiliate);
            taskRecord6.setPrevAffiliate(null);
            notifyTaskPersisterLocked(taskRecord6, false);
        }
        this.mTasks.addAll(i, this.mTmpRecents);
        this.mTmpRecents.clear();
        return i + size2;
    }

    private boolean moveAffiliatedTasksToFront(TaskRecord taskRecord, int i) {
        TaskRecord taskRecord2;
        int size = this.mTasks.size();
        TaskRecord taskRecord3 = taskRecord;
        int i2 = i;
        while (taskRecord3.mNextAffiliate != null && i2 > 0) {
            taskRecord3 = taskRecord3.mNextAffiliate;
            i2--;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: adding affilliates starting at " + i2 + " from intial " + i);
        }
        boolean z = taskRecord3.mAffiliatedTaskId == taskRecord.mAffiliatedTaskId;
        TaskRecord taskRecord4 = taskRecord3;
        int i3 = i2;
        while (true) {
            if (i3 >= size) {
                break;
            }
            taskRecord2 = this.mTasks.get(i3);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: looking at next chain @" + i3 + " " + taskRecord2);
            }
            if (taskRecord2 == taskRecord3) {
                if (taskRecord2.mNextAffiliate != null || taskRecord2.mNextAffiliateTaskId != -1) {
                    break;
                }
                if (taskRecord2.mPrevAffiliateTaskId != -1) {
                    if (taskRecord2.mPrevAffiliate != null) {
                        Slog.wtf(TAG, "Bad chain @" + i3 + ": last task " + taskRecord2 + " has previous affiliate " + taskRecord2.mPrevAffiliate);
                        z = false;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: end of chain @" + i3);
                    }
                } else {
                    if (taskRecord2.mPrevAffiliate == null) {
                        Slog.wtf(TAG, "Bad chain @" + i3 + ": task " + taskRecord2 + " has previous affiliate " + taskRecord2.mPrevAffiliate + " but should be id " + taskRecord2.mPrevAffiliate);
                        break;
                    }
                    if (taskRecord2.mAffiliatedTaskId != taskRecord.mAffiliatedTaskId) {
                        Slog.wtf(TAG, "Bad chain @" + i3 + ": task " + taskRecord2 + " has affiliated id " + taskRecord2.mAffiliatedTaskId + " but should be " + taskRecord.mAffiliatedTaskId);
                        break;
                    }
                    i3++;
                    if (i3 < size) {
                        taskRecord4 = taskRecord2;
                    } else {
                        Slog.wtf(TAG, "Bad chain ran off index " + i3 + ": last task " + taskRecord2);
                        break;
                    }
                }
            } else {
                if (taskRecord2.mNextAffiliate != taskRecord4 || taskRecord2.mNextAffiliateTaskId != taskRecord4.taskId) {
                    break;
                }
                if (taskRecord2.mPrevAffiliateTaskId != -1) {
                }
            }
        }
        Slog.wtf(TAG, "Bad chain @" + i3 + ": middle task " + taskRecord2 + " @" + i3 + " has bad next affiliate " + taskRecord2.mNextAffiliate + " id " + taskRecord2.mNextAffiliateTaskId + ", expected " + taskRecord4);
        z = false;
        if (z && i3 < i) {
            Slog.wtf(TAG, "Bad chain @" + i3 + ": did not extend to task " + taskRecord + " @" + i);
            z = false;
        }
        if (!z) {
            return false;
        }
        for (int i4 = i2; i4 <= i3; i4++) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: moving affiliated " + taskRecord + " from " + i4 + " to " + (i4 - i2));
            }
            this.mTasks.add(i4 - i2, this.mTasks.remove(i4));
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: done moving tasks  " + i2 + " to " + i3);
        }
        return true;
    }

    void dump(PrintWriter printWriter, boolean z, String str) {
        printWriter.println("ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)");
        printWriter.println("mRecentsUid=" + this.mRecentsUid);
        printWriter.println("mRecentsComponent=" + this.mRecentsComponent);
        if (this.mTasks.isEmpty()) {
            return;
        }
        int size = this.mTasks.size();
        boolean z2 = false;
        boolean z3 = false;
        for (int i = 0; i < size; i++) {
            TaskRecord taskRecord = this.mTasks.get(i);
            if (str == null || (taskRecord.realActivity != null && str.equals(taskRecord.realActivity.getPackageName()))) {
                if (!z3) {
                    printWriter.println("  Recent tasks:");
                    z2 = true;
                    z3 = true;
                }
                printWriter.print("  * Recent #");
                printWriter.print(i);
                printWriter.print(": ");
                printWriter.println(taskRecord);
                if (z) {
                    taskRecord.dump(printWriter, "    ");
                }
            }
        }
        if (!z2) {
            printWriter.println("  (nothing)");
        }
    }

    ActivityManager.RecentTaskInfo createRecentTaskInfo(TaskRecord taskRecord) {
        ActivityManager.RecentTaskInfo recentTaskInfo = new ActivityManager.RecentTaskInfo();
        recentTaskInfo.id = taskRecord.getTopActivity() == null ? -1 : taskRecord.taskId;
        recentTaskInfo.persistentId = taskRecord.taskId;
        recentTaskInfo.baseIntent = new Intent(taskRecord.getBaseIntent());
        recentTaskInfo.origActivity = taskRecord.origActivity;
        recentTaskInfo.realActivity = taskRecord.realActivity;
        recentTaskInfo.description = taskRecord.lastDescription;
        recentTaskInfo.stackId = taskRecord.getStackId();
        recentTaskInfo.userId = taskRecord.userId;
        recentTaskInfo.taskDescription = new ActivityManager.TaskDescription(taskRecord.lastTaskDescription);
        recentTaskInfo.lastActiveTime = taskRecord.lastActiveTime;
        recentTaskInfo.affiliatedTaskId = taskRecord.mAffiliatedTaskId;
        recentTaskInfo.affiliatedTaskColor = taskRecord.mAffiliatedTaskColor;
        recentTaskInfo.numActivities = 0;
        if (!taskRecord.matchParentBounds()) {
            recentTaskInfo.bounds = new Rect(taskRecord.getOverrideBounds());
        }
        recentTaskInfo.supportsSplitScreenMultiWindow = taskRecord.supportsSplitScreenWindowingMode();
        recentTaskInfo.resizeMode = taskRecord.mResizeMode;
        recentTaskInfo.configuration.setTo(taskRecord.getConfiguration());
        taskRecord.getNumRunningActivities(this.mTmpReport);
        recentTaskInfo.numActivities = this.mTmpReport.numActivities;
        recentTaskInfo.baseActivity = this.mTmpReport.base != null ? this.mTmpReport.base.intent.getComponent() : null;
        recentTaskInfo.topActivity = this.mTmpReport.top != null ? this.mTmpReport.top.intent.getComponent() : null;
        return recentTaskInfo;
    }

    private boolean hasCompatibleActivityTypeAndWindowingMode(TaskRecord taskRecord, TaskRecord taskRecord2) {
        int activityType = taskRecord.getActivityType();
        int windowingMode = taskRecord.getWindowingMode();
        boolean z = activityType == 0;
        boolean z2 = windowingMode == 0;
        int activityType2 = taskRecord2.getActivityType();
        int windowingMode2 = taskRecord2.getWindowingMode();
        return (activityType == activityType2 || z || (activityType2 == 0)) && (windowingMode == windowingMode2 || z2 || (windowingMode2 == 0));
    }
}
