package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RecentsTaskLoadPlan {
    HashMap<Task.ComponentNameKey, ActivityInfoHandle> mActivityInfoCache = new HashMap<>();
    RecentsConfiguration mConfig;
    Context mContext;
    List<ActivityManager.RecentTaskInfo> mRawTasks;
    TaskStack mStack;
    SystemServicesProxy mSystemServicesProxy;
    static String TAG = "RecentsTaskLoadPlan";
    static boolean DEBUG = false;

    public static class Options {
        public int runningTaskId = -1;
        public boolean loadIcons = true;
        public boolean loadThumbnails = true;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int numVisibleTasks = 0;
        public int numVisibleTaskThumbnails = 0;
    }

    RecentsTaskLoadPlan(Context context, RecentsConfiguration config, SystemServicesProxy ssp) {
        this.mContext = context;
        this.mConfig = config;
        this.mSystemServicesProxy = ssp;
    }

    public synchronized void preloadRawTasks(boolean isTopTaskHome) {
        this.mRawTasks = this.mSystemServicesProxy.getRecentTasks(this.mConfig.maxNumTasksToLoad, UserHandle.CURRENT.getIdentifier(), isTopTaskHome);
        Collections.reverse(this.mRawTasks);
        if (DEBUG) {
            Log.d(TAG, "preloadRawTasks, tasks: " + this.mRawTasks.size());
        }
    }

    synchronized void preloadPlan(RecentsTaskLoader loader, boolean isTopTaskHome) {
        ActivityInfoHandle infoHandle;
        if (DEBUG) {
            Log.d(TAG, "preloadPlan");
        }
        this.mActivityInfoCache.clear();
        this.mStack = new TaskStack();
        Resources res = this.mContext.getResources();
        ArrayList<Task> loadedTasks = new ArrayList<>();
        if (this.mRawTasks == null) {
            preloadRawTasks(isTopTaskHome);
        }
        int taskCount = this.mRawTasks.size();
        int i = 0;
        while (i < taskCount) {
            ActivityManager.RecentTaskInfo t = this.mRawTasks.get(i);
            Task.TaskKey taskKey = new Task.TaskKey(t.persistentId, t.baseIntent, t.userId, t.firstActiveTime, t.lastActiveTime);
            Task.ComponentNameKey cnKey = taskKey.getComponentNameKey();
            boolean hadCachedActivityInfo = false;
            if (this.mActivityInfoCache.containsKey(cnKey)) {
                infoHandle = this.mActivityInfoCache.get(cnKey);
                hadCachedActivityInfo = true;
            } else {
                infoHandle = new ActivityInfoHandle();
            }
            String activityLabel = loader.getAndUpdateActivityLabel(taskKey, t.taskDescription, this.mSystemServicesProxy, infoHandle);
            Drawable activityIcon = loader.getAndUpdateActivityIcon(taskKey, t.taskDescription, this.mSystemServicesProxy, res, infoHandle, false);
            int activityColor = loader.getActivityPrimaryColor(t.taskDescription, this.mConfig);
            if (!hadCachedActivityInfo && infoHandle.info != null) {
                this.mActivityInfoCache.put(cnKey, infoHandle);
            }
            Bitmap icon = t.taskDescription != null ? t.taskDescription.getInMemoryIcon() : null;
            String iconFilename = t.taskDescription != null ? t.taskDescription.getIconFilename() : null;
            Task task = new Task(taskKey, t.id != RecentsTaskLoader.INVALID_TASK_ID, t.affiliatedTaskId, t.affiliatedTaskColor, activityLabel, activityIcon, activityColor, i == taskCount + (-1), this.mConfig.lockToAppEnabled, icon, iconFilename);
            task.thumbnail = loader.getAndUpdateThumbnail(taskKey, this.mSystemServicesProxy, false);
            if (DEBUG) {
                Log.d(TAG, "\tthumbnail: " + taskKey + ", " + task.thumbnail);
            }
            loadedTasks.add(task);
            i++;
        }
        this.mStack.setTasks(loadedTasks);
        this.mStack.createAffiliatedGroupings(this.mConfig);
        if (this.mStack.getTaskCount() != this.mRawTasks.size()) {
            throw new RuntimeException("Loading failed");
        }
    }

    synchronized void executePlan(Options opts, RecentsTaskLoader loader, TaskResourceLoadQueue loadQueue) {
        ActivityInfoHandle infoHandle;
        if (DEBUG) {
            Log.d(TAG, "executePlan, # tasks: " + opts.numVisibleTasks + ", # thumbnails: " + opts.numVisibleTaskThumbnails + ", running task id: " + opts.runningTaskId);
        }
        Resources res = this.mContext.getResources();
        ArrayList<Task> tasks = this.mStack.getTasks();
        int taskCount = tasks.size();
        int i = 0;
        while (i < taskCount) {
            ActivityManager.RecentTaskInfo t = this.mRawTasks.get(i);
            Task task = tasks.get(i);
            Task.TaskKey taskKey = task.key;
            Task.ComponentNameKey cnKey = taskKey.getComponentNameKey();
            boolean hadCachedActivityInfo = false;
            if (this.mActivityInfoCache.containsKey(cnKey)) {
                infoHandle = this.mActivityInfoCache.get(cnKey);
                hadCachedActivityInfo = true;
            } else {
                infoHandle = new ActivityInfoHandle();
            }
            boolean isRunningTask = task.key.id == opts.runningTaskId;
            boolean isVisibleTask = i >= taskCount - opts.numVisibleTasks;
            boolean isVisibleThumbnail = i >= taskCount - opts.numVisibleTaskThumbnails;
            if (!opts.onlyLoadPausedActivities || !isRunningTask) {
                if (opts.loadIcons && ((isRunningTask || isVisibleTask) && task.activityIcon == null)) {
                    if (DEBUG) {
                        Log.d(TAG, "\tLoading icon: " + taskKey);
                    }
                    task.activityIcon = loader.getAndUpdateActivityIcon(taskKey, t.taskDescription, this.mSystemServicesProxy, res, infoHandle, true);
                }
                if (opts.loadThumbnails && ((isRunningTask || isVisibleThumbnail) && (task.thumbnail == null || isRunningTask))) {
                    if (DEBUG) {
                        Log.d(TAG, "\tLoading thumbnail: " + taskKey);
                    }
                    if (this.mConfig.svelteLevel <= 1) {
                        task.thumbnail = loader.getAndUpdateThumbnail(taskKey, this.mSystemServicesProxy, true);
                    } else if (this.mConfig.svelteLevel == 2) {
                        loadQueue.addTask(task);
                    }
                }
                if (!hadCachedActivityInfo && infoHandle.info != null) {
                    this.mActivityInfoCache.put(cnKey, infoHandle);
                }
            }
            i++;
        }
    }

    public TaskStack getTaskStack() {
        return this.mStack;
    }

    public SpaceNode getSpaceNode() {
        SpaceNode node = new SpaceNode();
        node.setStack(this.mStack);
        return node;
    }
}
