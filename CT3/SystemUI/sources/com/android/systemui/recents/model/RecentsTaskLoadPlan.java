package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTaskLoadPlan {
    private static int MIN_NUM_TASKS = 5;
    private static int SESSION_BEGIN_TIME = 21600000;
    Context mContext;
    ArraySet<Integer> mCurrentQuietProfiles = new ArraySet<>();
    List<ActivityManager.RecentTaskInfo> mRawTasks;
    TaskStack mStack;

    public static class Options {
        public int runningTaskId = -1;
        public boolean loadIcons = true;
        public boolean loadThumbnails = true;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int numVisibleTasks = 0;
        public int numVisibleTaskThumbnails = 0;
    }

    RecentsTaskLoadPlan(Context context) {
        this.mContext = context;
    }

    private void updateCurrentQuietProfilesCache(int currentUserId) {
        this.mCurrentQuietProfiles.clear();
        if (currentUserId == -2) {
            currentUserId = ActivityManager.getCurrentUser();
        }
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        List<UserInfo> profiles = userManager.getProfiles(currentUserId);
        if (profiles == null) {
            return;
        }
        for (int i = 0; i < profiles.size(); i++) {
            UserInfo user = profiles.get(i);
            if (user.isManagedProfile() && user.isQuietModeEnabled()) {
                this.mCurrentQuietProfiles.add(Integer.valueOf(user.id));
            }
        }
    }

    public synchronized void preloadRawTasks(boolean includeFrontMostExcludedTask) {
        updateCurrentQuietProfilesCache(-2);
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mRawTasks = ssp.getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), -2, includeFrontMostExcludedTask, this.mCurrentQuietProfiles);
        Collections.reverse(this.mRawTasks);
    }

    public synchronized void preloadPlan(RecentsTaskLoader loader, int runningTaskId, boolean includeFrontMostExcludedTask) {
        boolean isStackTask;
        Drawable andUpdateActivityIcon;
        boolean isSystemApp;
        Resources res = this.mContext.getResources();
        ArrayList<Task> allTasks = new ArrayList<>();
        if (this.mRawTasks == null) {
            preloadRawTasks(includeFrontMostExcludedTask);
        }
        SparseArray<Task.TaskKey> affiliatedTasks = new SparseArray<>();
        SparseIntArray affiliatedTaskCounts = new SparseIntArray();
        String dismissDescFormat = this.mContext.getString(R.string.accessibility_recents_item_will_be_dismissed);
        String appInfoDescFormat = this.mContext.getString(R.string.accessibility_recents_item_open_app_info);
        long lastStackActiveTime = Prefs.getLong(this.mContext, "OverviewLastStackTaskActiveTime", 0L);
        long newLastStackActiveTime = -1;
        int taskCount = this.mRawTasks.size();
        int i = 0;
        while (i < taskCount) {
            ActivityManager.RecentTaskInfo t = this.mRawTasks.get(i);
            Task.TaskKey taskKey = new Task.TaskKey(t.persistentId, t.stackId, t.baseIntent, t.userId, t.firstActiveTime, t.lastActiveTime);
            boolean isFreeformTask = SystemServicesProxy.isFreeformStack(t.stackId);
            if (isFreeformTask || !isHistoricalTask(t)) {
                isStackTask = true;
            } else {
                isStackTask = t.lastActiveTime >= lastStackActiveTime && i >= taskCount - MIN_NUM_TASKS;
            }
            boolean isLaunchTarget = taskKey.id == runningTaskId;
            if (isStackTask && newLastStackActiveTime < 0) {
                newLastStackActiveTime = t.lastActiveTime;
            }
            ActivityInfo info = loader.getAndUpdateActivityInfo(taskKey);
            String title = loader.getAndUpdateActivityTitle(taskKey, t.taskDescription);
            String titleDescription = loader.getAndUpdateContentDescription(taskKey, res);
            String dismissDescription = String.format(dismissDescFormat, titleDescription);
            String appInfoDescription = String.format(appInfoDescFormat, titleDescription);
            if (isStackTask) {
                andUpdateActivityIcon = loader.getAndUpdateActivityIcon(taskKey, t.taskDescription, res, false);
            } else {
                andUpdateActivityIcon = null;
            }
            Bitmap thumbnail = loader.getAndUpdateThumbnail(taskKey, false);
            int activityColor = loader.getActivityPrimaryColor(t.taskDescription);
            int backgroundColor = loader.getActivityBackgroundColor(t.taskDescription);
            if (info == null) {
                isSystemApp = false;
            } else {
                isSystemApp = (info.applicationInfo.flags & 1) != 0;
            }
            Task task = new Task(taskKey, t.affiliatedTaskId, t.affiliatedTaskColor, andUpdateActivityIcon, thumbnail, title, titleDescription, dismissDescription, appInfoDescription, activityColor, backgroundColor, isLaunchTarget, isStackTask, isSystemApp, t.isDockable, t.bounds, t.taskDescription, t.resizeMode, t.topActivity);
            allTasks.add(task);
            affiliatedTaskCounts.put(taskKey.id, affiliatedTaskCounts.get(taskKey.id, 0) + 1);
            affiliatedTasks.put(taskKey.id, taskKey);
            i++;
        }
        if (newLastStackActiveTime != -1) {
            Prefs.putLong(this.mContext, "OverviewLastStackTaskActiveTime", newLastStackActiveTime);
        }
        this.mStack = new TaskStack();
        this.mStack.setTasks(this.mContext, allTasks, false);
    }

    public synchronized void executePlan(Options opts, RecentsTaskLoader loader, TaskResourceLoadQueue loadQueue) {
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = this.mContext.getResources();
        ArrayList<Task> tasks = this.mStack.getStackTasks();
        int taskCount = tasks.size();
        int i = 0;
        while (i < taskCount) {
            Task task = tasks.get(i);
            Task.TaskKey taskKey = task.key;
            boolean isRunningTask = task.key.id == opts.runningTaskId;
            boolean isVisibleTask = i >= taskCount - opts.numVisibleTasks;
            boolean isVisibleThumbnail = i >= taskCount - opts.numVisibleTaskThumbnails;
            if (!opts.onlyLoadPausedActivities || !isRunningTask) {
                if (opts.loadIcons && ((isRunningTask || isVisibleTask) && task.icon == null)) {
                    task.icon = loader.getAndUpdateActivityIcon(taskKey, task.taskDescription, res, true);
                }
                if (opts.loadThumbnails && ((isRunningTask || isVisibleThumbnail) && (task.thumbnail == null || isRunningTask))) {
                    if (config.svelteLevel <= 1) {
                        task.thumbnail = loader.getAndUpdateThumbnail(taskKey, true);
                    } else if (config.svelteLevel == 2) {
                        loadQueue.addTask(task);
                    }
                }
            }
            i++;
        }
    }

    public TaskStack getTaskStack() {
        return this.mStack;
    }

    public boolean hasTasks() {
        return this.mStack != null && this.mStack.getTaskCount() > 0;
    }

    private boolean isHistoricalTask(ActivityManager.RecentTaskInfo t) {
        return t.lastActiveTime < System.currentTimeMillis() - ((long) SESSION_BEGIN_TIME);
    }
}
