package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.server.pm.PackageManagerService;
import com.google.android.collect.Sets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

class RecentTasks extends ArrayList<TaskRecord> {
    private static final int DEFAULT_INITIAL_CAPACITY = 5;
    private static final int MAX_RECENT_BITMAPS = 3;
    private static final boolean MOVE_AFFILIATED_TASKS_TO_FRONT = false;
    private static final String TAG = "ActivityManager";
    private static final String TAG_RECENTS = TAG + ActivityManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_TASKS = TAG + ActivityManagerDebugConfig.POSTFIX_TASKS;
    private static Comparator<TaskRecord> sTaskRecordComparator = new Comparator<TaskRecord>() {
        @Override
        public int compare(TaskRecord lhs, TaskRecord rhs) {
            return rhs.taskId - lhs.taskId;
        }
    };
    private final ActivityManagerService mService;
    private final TaskPersister mTaskPersister;
    private final SparseBooleanArray mUsersWithRecentsLoaded = new SparseBooleanArray(5);
    final SparseArray<SparseBooleanArray> mPersistedTaskIds = new SparseArray<>(5);
    private final ArrayList<TaskRecord> mTmpRecents = new ArrayList<>();
    private final HashMap<ComponentName, ActivityInfo> mTmpAvailActCache = new HashMap<>();
    private final HashMap<String, ApplicationInfo> mTmpAvailAppCache = new HashMap<>();
    private final ActivityInfo mTmpActivityInfo = new ActivityInfo();
    private final ApplicationInfo mTmpAppInfo = new ApplicationInfo();

    RecentTasks(ActivityManagerService service, ActivityStackSupervisor mStackSupervisor) {
        File systemDir = Environment.getDataSystemDirectory();
        this.mService = service;
        this.mTaskPersister = new TaskPersister(systemDir, mStackSupervisor, service, this);
        mStackSupervisor.setRecentTasks(this);
    }

    void loadUserRecentsLocked(int userId) {
        if (this.mUsersWithRecentsLoaded.get(userId)) {
            return;
        }
        loadPersistedTaskIdsForUserLocked(userId);
        Slog.i(TAG, "Loading recents for user " + userId + " into memory.");
        addAll(this.mTaskPersister.restoreTasksForUserLocked(userId));
        cleanupLocked(userId);
        this.mUsersWithRecentsLoaded.put(userId, true);
    }

    private void loadPersistedTaskIdsForUserLocked(int userId) {
        if (this.mPersistedTaskIds.get(userId) != null) {
            return;
        }
        this.mPersistedTaskIds.put(userId, this.mTaskPersister.loadPersistedTaskIdsForUser(userId));
        Slog.i(TAG, "Loaded persisted task ids for user " + userId);
    }

    boolean taskIdTakenForUserLocked(int taskId, int userId) {
        loadPersistedTaskIdsForUserLocked(userId);
        return this.mPersistedTaskIds.get(userId).get(taskId);
    }

    void notifyTaskPersisterLocked(TaskRecord task, boolean flush) {
        if (task != null && task.stack != null && task.stack.isHomeStack()) {
            return;
        }
        syncPersistentTaskIdsLocked();
        this.mTaskPersister.wakeup(task, flush);
    }

    private void syncPersistentTaskIdsLocked() {
        for (int i = this.mPersistedTaskIds.size() - 1; i >= 0; i--) {
            int userId = this.mPersistedTaskIds.keyAt(i);
            if (this.mUsersWithRecentsLoaded.get(userId)) {
                this.mPersistedTaskIds.valueAt(i).clear();
            }
        }
        for (int i2 = size() - 1; i2 >= 0; i2--) {
            TaskRecord task = get(i2);
            if (task.isPersistable && (task.stack == null || !task.stack.isHomeStack())) {
                if (this.mPersistedTaskIds.get(task.userId) == null) {
                    Slog.e(TAG, "No task ids found for userId " + task.userId + ". task=" + task + " mPersistedTaskIds=" + this.mPersistedTaskIds);
                    this.mPersistedTaskIds.put(task.userId, new SparseBooleanArray());
                }
                this.mPersistedTaskIds.get(task.userId).put(task.taskId, true);
            }
        }
    }

    void onSystemReadyLocked() {
        clear();
        this.mTaskPersister.startPersisting();
    }

    Bitmap getTaskDescriptionIcon(String path) {
        return this.mTaskPersister.getTaskDescriptionIcon(path);
    }

    Bitmap getImageFromWriteQueue(String path) {
        return this.mTaskPersister.getImageFromWriteQueue(path);
    }

    void saveImage(Bitmap image, String path) {
        this.mTaskPersister.saveImage(image, path);
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
        int[] usersWithRecentsLoaded = new int[this.mUsersWithRecentsLoaded.size()];
        int len = 0;
        for (int i = 0; i < usersWithRecentsLoaded.length; i++) {
            int userId = this.mUsersWithRecentsLoaded.keyAt(i);
            if (this.mUsersWithRecentsLoaded.valueAt(i)) {
                usersWithRecentsLoaded[len] = userId;
                len++;
            }
        }
        if (len < usersWithRecentsLoaded.length) {
            return Arrays.copyOf(usersWithRecentsLoaded, len);
        }
        return usersWithRecentsLoaded;
    }

    private void unloadUserRecentsLocked(int userId) {
        if (!this.mUsersWithRecentsLoaded.get(userId)) {
            return;
        }
        Slog.i(TAG, "Unloading recents for user " + userId + " from memory.");
        this.mUsersWithRecentsLoaded.delete(userId);
        removeTasksForUserLocked(userId);
    }

    void unloadUserDataFromMemoryLocked(int userId) {
        unloadUserRecentsLocked(userId);
        this.mPersistedTaskIds.delete(userId);
        this.mTaskPersister.unloadUserDataFromMemory(userId);
    }

    TaskRecord taskForIdLocked(int id) {
        int recentsCount = size();
        for (int i = 0; i < recentsCount; i++) {
            TaskRecord tr = get(i);
            if (tr.taskId == id) {
                return tr;
            }
        }
        return null;
    }

    void removeTasksForUserLocked(int userId) {
        if (userId <= 0) {
            Slog.i(TAG, "Can't remove recent task on user " + userId);
            return;
        }
        for (int i = size() - 1; i >= 0; i--) {
            TaskRecord tr = get(i);
            if (tr.userId == userId) {
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    Slog.i(TAG_TASKS, "remove RecentTask " + tr + " when finishing user" + userId);
                }
                remove(i);
                tr.removedFromRecentsImmediatly();
            }
        }
    }

    void onPackagesSuspendedChanged(String[] packages, boolean suspended, int userId) {
        Set<String> packageNames = Sets.newHashSet(packages);
        for (int i = size() - 1; i >= 0; i--) {
            TaskRecord tr = get(i);
            if (tr.realActivity != null && packageNames.contains(tr.realActivity.getPackageName()) && tr.userId == userId && tr.realActivitySuspended != suspended) {
                tr.realActivitySuspended = suspended;
                notifyTaskPersisterLocked(tr, false);
            }
        }
    }

    void cleanupLocked(int userId) {
        int recentsCount = size();
        if (recentsCount == 0) {
            return;
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        for (int i = recentsCount - 1; i >= 0; i--) {
            TaskRecord task = get(i);
            if (userId == -1 || task.userId == userId) {
                if (task.autoRemoveRecents && task.getTopActivity() == null) {
                    remove(i);
                    task.removedFromRecents();
                    Slog.w(TAG, "Removing auto-remove without activity: " + task);
                } else if (task.realActivity != null) {
                    ActivityInfo ai = this.mTmpAvailActCache.get(task.realActivity);
                    if (ai == null) {
                        try {
                            ai = pm.getActivityInfo(task.realActivity, 268435456, userId);
                            if (ai == null) {
                                ai = this.mTmpActivityInfo;
                            }
                            this.mTmpAvailActCache.put(task.realActivity, ai);
                            if (ai != this.mTmpActivityInfo) {
                                ApplicationInfo app = this.mTmpAvailAppCache.get(task.realActivity.getPackageName());
                                if (app == null) {
                                    try {
                                        app = pm.getApplicationInfo(task.realActivity.getPackageName(), PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId);
                                        if (app == null) {
                                            app = this.mTmpAppInfo;
                                        }
                                        this.mTmpAvailAppCache.put(task.realActivity.getPackageName(), app);
                                        if (app != this.mTmpAppInfo || (app.flags & 8388608) == 0) {
                                            remove(i);
                                            task.removedFromRecents();
                                            Slog.w(TAG, "Removing no longer valid recent: " + task);
                                        } else {
                                            if (ActivityManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                                                Slog.d(TAG_RECENTS, "Making recent unavailable: " + task);
                                            }
                                            task.isAvailable = false;
                                        }
                                    } catch (RemoteException e) {
                                    }
                                } else if (app != this.mTmpAppInfo) {
                                    remove(i);
                                    task.removedFromRecents();
                                    Slog.w(TAG, "Removing no longer valid recent: " + task);
                                }
                            } else if (ai.enabled && ai.applicationInfo.enabled && (ai.applicationInfo.flags & 8388608) != 0) {
                                if (ActivityManagerDebugConfig.DEBUG_RECENTS && !task.isAvailable) {
                                    Slog.d(TAG_RECENTS, "Making recent available: " + task);
                                }
                                task.isAvailable = true;
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                                    Slog.d(TAG_RECENTS, "Making recent unavailable: " + task + " (enabled=" + ai.enabled + "/" + ai.applicationInfo.enabled + " flags=" + Integer.toHexString(ai.applicationInfo.flags) + ")");
                                }
                                task.isAvailable = false;
                            }
                        } catch (RemoteException e2) {
                        }
                    } else if (ai != this.mTmpActivityInfo) {
                    }
                }
            }
        }
        int i2 = 0;
        int recentsCount2 = size();
        while (i2 < recentsCount2) {
            i2 = processNextAffiliateChainLocked(i2);
        }
    }

    private final boolean moveAffiliatedTasksToFront(TaskRecord task, int taskIndex) {
        int recentsCount = size();
        TaskRecord top = task;
        int topIndex = taskIndex;
        while (top.mNextAffiliate != null && topIndex > 0) {
            top = top.mNextAffiliate;
            topIndex--;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: adding affilliates starting at " + topIndex + " from intial " + taskIndex);
        }
        boolean sane = top.mAffiliatedTaskId == task.mAffiliatedTaskId;
        int endIndex = topIndex;
        TaskRecord prev = top;
        while (true) {
            if (endIndex >= recentsCount) {
                break;
            }
            TaskRecord cur = get(endIndex);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: looking at next chain @" + endIndex + " " + cur);
            }
            if (cur == top) {
                if (cur.mNextAffiliate != null || cur.mNextAffiliateTaskId != -1) {
                    break;
                }
                if (cur.mPrevAffiliateTaskId == -1) {
                    if (cur.mPrevAffiliate == null) {
                        Slog.wtf(TAG, "Bad chain @" + endIndex + ": task " + cur + " has previous affiliate " + cur.mPrevAffiliate + " but should be id " + cur.mPrevAffiliate);
                        sane = false;
                        break;
                    }
                    if (cur.mAffiliatedTaskId != task.mAffiliatedTaskId) {
                        Slog.wtf(TAG, "Bad chain @" + endIndex + ": task " + cur + " has affiliated id " + cur.mAffiliatedTaskId + " but should be " + task.mAffiliatedTaskId);
                        sane = false;
                        break;
                    }
                    prev = cur;
                    endIndex++;
                    if (endIndex >= recentsCount) {
                        Slog.wtf(TAG, "Bad chain ran off index " + endIndex + ": last task " + cur);
                        sane = false;
                        break;
                    }
                } else {
                    if (cur.mPrevAffiliate != null) {
                        Slog.wtf(TAG, "Bad chain @" + endIndex + ": last task " + cur + " has previous affiliate " + cur.mPrevAffiliate);
                        sane = false;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: end of chain @" + endIndex);
                    }
                }
            } else {
                if (cur.mNextAffiliate != prev || cur.mNextAffiliateTaskId != prev.taskId) {
                    break;
                }
                if (cur.mPrevAffiliateTaskId == -1) {
                }
            }
        }
        if (sane && endIndex < taskIndex) {
            Slog.wtf(TAG, "Bad chain @" + endIndex + ": did not extend to task " + task + " @" + taskIndex);
            sane = false;
        }
        if (!sane) {
            return false;
        }
        for (int i = topIndex; i <= endIndex; i++) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: moving affiliated " + task + " from " + i + " to " + (i - topIndex));
            }
            add(i - topIndex, remove(i));
        }
        if (!ActivityManagerDebugConfig.DEBUG_RECENTS) {
            return true;
        }
        Slog.d(TAG_RECENTS, "addRecent: done moving tasks  " + topIndex + " to " + endIndex);
        return true;
    }

    final void addLocked(TaskRecord task) {
        int taskIndex;
        boolean isAffiliated = (task.mAffiliatedTaskId == task.taskId && task.mNextAffiliateTaskId == -1 && task.mPrevAffiliateTaskId == -1) ? false : true;
        int recentsCount = size();
        if (task.voiceSession != null) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: not adding voice interaction " + task);
                return;
            }
            return;
        }
        if (!isAffiliated && recentsCount > 0 && get(0) == task) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: already at top: " + task);
                return;
            }
            return;
        }
        if (isAffiliated && recentsCount > 0 && task.inRecents && task.mAffiliatedTaskId == get(0).mAffiliatedTaskId) {
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: affiliated " + get(0) + " at top when adding " + task);
                return;
            }
            return;
        }
        boolean needAffiliationFix = false;
        if (task.inRecents) {
            int taskIndex2 = indexOf(task);
            if (taskIndex2 >= 0) {
                if (!isAffiliated) {
                    remove(taskIndex2);
                    add(0, task);
                    notifyTaskPersisterLocked(task, false);
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: moving to top " + task + " from " + taskIndex2);
                        return;
                    }
                    return;
                }
                if (moveAffiliatedTasksToFront(task, taskIndex2)) {
                    return;
                } else {
                    needAffiliationFix = true;
                }
            } else {
                Slog.wtf(TAG, "Task with inRecent not in recents: " + task);
                needAffiliationFix = true;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: trimming tasks for " + task);
        }
        trimForTaskLocked(task, true);
        int maxRecents = ActivityManager.getMaxRecentTasksStatic();
        for (int recentsCount2 = size(); recentsCount2 >= maxRecents; recentsCount2--) {
            TaskRecord tr = remove(recentsCount2 - 1);
            tr.removedFromRecents();
        }
        task.inRecents = true;
        if (!isAffiliated || needAffiliationFix) {
            add(0, task);
            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: adding " + task);
            }
        } else if (isAffiliated) {
            TaskRecord other = task.mNextAffiliate;
            if (other == null) {
                other = task.mPrevAffiliate;
            }
            if (other != null) {
                int otherIndex = indexOf(other);
                if (otherIndex >= 0) {
                    if (other == task.mNextAffiliate) {
                        taskIndex = otherIndex + 1;
                    } else {
                        taskIndex = otherIndex;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: new affiliated task added at " + taskIndex + ": " + task);
                    }
                    add(taskIndex, task);
                    if (moveAffiliatedTasksToFront(task, taskIndex)) {
                        return;
                    } else {
                        needAffiliationFix = true;
                    }
                } else {
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: couldn't find other affiliation " + other);
                    }
                    needAffiliationFix = true;
                }
            } else {
                if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: adding affiliated task without next/prev:" + task);
                }
                needAffiliationFix = true;
            }
        }
        if (!needAffiliationFix) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: regrouping affiliations");
        }
        cleanupLocked(task.userId);
    }

    int trimForTaskLocked(TaskRecord task, boolean doTrim) {
        boolean sameActivity;
        int recentsCount = size();
        Intent intent = task.intent;
        boolean zIsDocument = intent != null ? intent.isDocument() : false;
        int maxRecents = task.maxRecents - 1;
        int i = 0;
        while (i < recentsCount) {
            TaskRecord tr = get(i);
            if (task != tr) {
                if ((task.stack == null || tr.stack == null || task.stack == tr.stack) && task.userId == tr.userId) {
                    if (i > 3) {
                        tr.freeLastThumbnail();
                    }
                    Intent trIntent = tr.intent;
                    boolean zEquals = task.affinity != null ? task.affinity.equals(tr.affinity) : false;
                    boolean zFilterEquals = intent != null ? intent.filterEquals(trIntent) : false;
                    boolean multiTasksAllowed = false;
                    int flags = intent.getFlags();
                    if ((268959744 & flags) != 0 && (134217728 & flags) != 0) {
                        multiTasksAllowed = true;
                    }
                    boolean zIsDocument2 = trIntent != null ? trIntent.isDocument() : false;
                    boolean bothDocuments = zIsDocument ? zIsDocument2 : false;
                    if (zEquals || zFilterEquals || bothDocuments) {
                        if (bothDocuments) {
                            if (task.realActivity == null || tr.realActivity == null) {
                                sameActivity = false;
                            } else {
                                sameActivity = task.realActivity.equals(tr.realActivity);
                            }
                            if (sameActivity && zFilterEquals && !multiTasksAllowed) {
                                if (maxRecents > 0 && !doTrim) {
                                    maxRecents--;
                                }
                            }
                        } else if (zIsDocument || zIsDocument2) {
                        }
                    }
                }
            } else {
                if (!doTrim) {
                    return i;
                }
                tr.disposeThumbnail();
                remove(i);
                if (task != tr) {
                    tr.removedFromRecents();
                }
                i--;
                recentsCount--;
                if (task.intent == null) {
                    task = tr;
                }
                notifyTaskPersisterLocked(tr, false);
            }
            i++;
        }
        return -1;
    }

    private int processNextAffiliateChainLocked(int start) {
        TaskRecord startTask = get(start);
        int affiliateId = startTask.mAffiliatedTaskId;
        if (startTask.taskId == affiliateId && startTask.mPrevAffiliate == null && startTask.mNextAffiliate == null) {
            startTask.inRecents = true;
            return start + 1;
        }
        this.mTmpRecents.clear();
        for (int i = size() - 1; i >= start; i--) {
            TaskRecord task = get(i);
            if (task.mAffiliatedTaskId == affiliateId) {
                remove(i);
                this.mTmpRecents.add(task);
            }
        }
        Collections.sort(this.mTmpRecents, sTaskRecordComparator);
        TaskRecord first = this.mTmpRecents.get(0);
        first.inRecents = true;
        if (first.mNextAffiliate != null) {
            Slog.w(TAG, "Link error 1 first.next=" + first.mNextAffiliate);
            first.setNextAffiliate(null);
            notifyTaskPersisterLocked(first, false);
        }
        int tmpSize = this.mTmpRecents.size();
        for (int i2 = 0; i2 < tmpSize - 1; i2++) {
            TaskRecord next = this.mTmpRecents.get(i2);
            TaskRecord prev = this.mTmpRecents.get(i2 + 1);
            if (next.mPrevAffiliate != prev) {
                Slog.w(TAG, "Link error 2 next=" + next + " prev=" + next.mPrevAffiliate + " setting prev=" + prev);
                next.setPrevAffiliate(prev);
                notifyTaskPersisterLocked(next, false);
            }
            if (prev.mNextAffiliate != next) {
                Slog.w(TAG, "Link error 3 prev=" + prev + " next=" + prev.mNextAffiliate + " setting next=" + next);
                prev.setNextAffiliate(next);
                notifyTaskPersisterLocked(prev, false);
            }
            prev.inRecents = true;
        }
        TaskRecord last = this.mTmpRecents.get(tmpSize - 1);
        if (last.mPrevAffiliate != null) {
            Slog.w(TAG, "Link error 4 last.prev=" + last.mPrevAffiliate);
            last.setPrevAffiliate(null);
            notifyTaskPersisterLocked(last, false);
        }
        addAll(start, this.mTmpRecents);
        this.mTmpRecents.clear();
        return start + tmpSize;
    }
}
