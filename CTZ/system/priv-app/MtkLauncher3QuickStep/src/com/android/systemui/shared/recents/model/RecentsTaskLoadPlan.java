package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class RecentsTaskLoadPlan {
    private final Context mContext;
    private final KeyguardManager mKeyguardManager;
    private List<ActivityManager.RecentTaskInfo> mRawTasks;
    private TaskStack mStack;
    private final SparseBooleanArray mTmpLockedUsers = new SparseBooleanArray();

    public static class Options {
        public int runningTaskId = -1;
        public boolean loadIcons = true;
        public boolean loadThumbnails = false;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int numVisibleTasks = 0;
        public int numVisibleTaskThumbnails = 0;
    }

    public static class PreloadOptions {
        public boolean loadTitles = true;
    }

    public RecentsTaskLoadPlan(Context context) {
        this.mContext = context;
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
    }

    public void preloadPlan(PreloadOptions opts, RecentsTaskLoader loader, int runningTaskId, int currentUserId) {
        boolean z;
        Drawable andUpdateActivityIcon;
        Resources res;
        int taskCount;
        PreloadOptions preloadOptions = opts;
        RecentsTaskLoader recentsTaskLoader = loader;
        Resources res2 = this.mContext.getResources();
        ArrayList<Task> allTasks = new ArrayList<>();
        if (this.mRawTasks == null) {
            this.mRawTasks = ActivityManagerWrapper.getInstance().getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), currentUserId);
            Collections.reverse(this.mRawTasks);
        }
        int taskCount2 = this.mRawTasks.size();
        int i = 0;
        while (i < taskCount2) {
            ActivityManager.RecentTaskInfo t = this.mRawTasks.get(i);
            int windowingMode = t.configuration.windowConfiguration.getWindowingMode();
            Task.TaskKey taskKey = new Task.TaskKey(t.persistentId, windowingMode, t.baseIntent, t.userId, t.lastActiveTime);
            boolean isFreeformTask = windowingMode == 5;
            boolean isStackTask = !isFreeformTask;
            boolean isLaunchTarget = taskKey.id == runningTaskId;
            ActivityInfo info = recentsTaskLoader.getAndUpdateActivityInfo(taskKey);
            if (info == null) {
                res = res2;
                taskCount = taskCount2;
            } else {
                String title = preloadOptions.loadTitles ? recentsTaskLoader.getAndUpdateActivityTitle(taskKey, t.taskDescription) : "";
                String titleDescription = preloadOptions.loadTitles ? recentsTaskLoader.getAndUpdateContentDescription(taskKey, t.taskDescription) : "";
                if (isStackTask) {
                    z = false;
                    andUpdateActivityIcon = recentsTaskLoader.getAndUpdateActivityIcon(taskKey, t.taskDescription, false);
                } else {
                    z = false;
                    andUpdateActivityIcon = null;
                }
                Drawable icon = andUpdateActivityIcon;
                ThumbnailData thumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey, z, z);
                int activityColor = recentsTaskLoader.getActivityPrimaryColor(t.taskDescription);
                int backgroundColor = recentsTaskLoader.getActivityBackgroundColor(t.taskDescription);
                boolean isSystemApp = (info == null || (info.applicationInfo.flags & 1) == 0) ? false : true;
                res = res2;
                if (this.mTmpLockedUsers.indexOfKey(t.userId) < 0) {
                    taskCount = taskCount2;
                    this.mTmpLockedUsers.put(t.userId, this.mKeyguardManager.isDeviceLocked(t.userId));
                } else {
                    taskCount = taskCount2;
                }
                boolean isLocked = this.mTmpLockedUsers.get(t.userId);
                Task task = new Task(taskKey, icon, thumbnail, title, titleDescription, activityColor, backgroundColor, isLaunchTarget, isStackTask, isSystemApp, t.supportsSplitScreenMultiWindow, t.taskDescription, t.resizeMode, t.topActivity, isLocked);
                allTasks.add(task);
            }
            i++;
            res2 = res;
            taskCount2 = taskCount;
            preloadOptions = opts;
            recentsTaskLoader = loader;
        }
        this.mStack = new TaskStack();
        this.mStack.setTasks((List<Task>) allTasks, false);
    }

    public void executePlan(Options opts, RecentsTaskLoader loader) {
        this.mContext.getResources();
        ArrayList<Task> tasks = this.mStack.getTasks();
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
                    task.icon = loader.getAndUpdateActivityIcon(taskKey, task.taskDescription, true);
                }
                if (opts.loadThumbnails && isVisibleThumbnail) {
                    task.thumbnail = loader.getAndUpdateThumbnail(taskKey, true, true);
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
}
