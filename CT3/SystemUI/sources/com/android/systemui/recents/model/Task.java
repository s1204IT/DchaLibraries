package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.ViewDebug;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class Task {

    @ViewDebug.ExportedProperty(category = "recents")
    public int affiliationColor;

    @ViewDebug.ExportedProperty(category = "recents")
    public int affiliationTaskId;

    @ViewDebug.ExportedProperty(category = "recents")
    public String appInfoDescription;

    @ViewDebug.ExportedProperty(category = "recents")
    public Rect bounds;

    @ViewDebug.ExportedProperty(category = "recents")
    public int colorBackground;

    @ViewDebug.ExportedProperty(category = "recents")
    public int colorPrimary;

    @ViewDebug.ExportedProperty(category = "recents")
    public String dismissDescription;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "group_")
    public TaskGrouping group;
    public Drawable icon;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isDockable;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLaunchTarget;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isStackTask;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isSystemApp;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "key_")
    public TaskKey key;
    private ArrayList<TaskCallbacks> mCallbacks = new ArrayList<>();

    @ViewDebug.ExportedProperty(category = "recents")
    public int resizeMode;
    public ActivityManager.TaskDescription taskDescription;
    public int temporarySortIndexInStack;
    public Bitmap thumbnail;

    @ViewDebug.ExportedProperty(category = "recents")
    public String title;

    @ViewDebug.ExportedProperty(category = "recents")
    public String titleDescription;

    @ViewDebug.ExportedProperty(category = "recents")
    public ComponentName topActivity;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean useLightOnPrimaryColor;

    public interface TaskCallbacks {
        void onTaskDataLoaded(Task task, ActivityManager.TaskThumbnailInfo taskThumbnailInfo);

        void onTaskDataUnloaded();

        void onTaskStackIdChanged();
    }

    public static class TaskKey {

        @ViewDebug.ExportedProperty(category = "recents")
        public final Intent baseIntent;

        @ViewDebug.ExportedProperty(category = "recents")
        public long firstActiveTime;

        @ViewDebug.ExportedProperty(category = "recents")
        public final int id;

        @ViewDebug.ExportedProperty(category = "recents")
        public long lastActiveTime;
        private int mHashCode;

        @ViewDebug.ExportedProperty(category = "recents")
        public int stackId;

        @ViewDebug.ExportedProperty(category = "recents")
        public final int userId;

        public TaskKey(int id, int stackId, Intent intent, int userId, long firstActiveTime, long lastActiveTime) {
            this.id = id;
            this.stackId = stackId;
            this.baseIntent = intent;
            this.userId = userId;
            this.firstActiveTime = firstActiveTime;
            this.lastActiveTime = lastActiveTime;
            updateHashCode();
        }

        public void setStackId(int stackId) {
            this.stackId = stackId;
            updateHashCode();
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        public boolean equals(Object o) {
            if (!(o instanceof TaskKey)) {
                return false;
            }
            TaskKey otherKey = (TaskKey) o;
            return this.id == otherKey.id && this.stackId == otherKey.stackId && this.userId == otherKey.userId;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        public String toString() {
            return "id=" + this.id + " stackId=" + this.stackId + " user=" + this.userId + " lastActiveTime=" + this.lastActiveTime + " component" + this.baseIntent.getComponent();
        }

        private void updateHashCode() {
            this.mHashCode = Objects.hash(Integer.valueOf(this.id), Integer.valueOf(this.stackId), Integer.valueOf(this.userId));
        }
    }

    public Task() {
    }

    public Task(TaskKey key, int affiliationTaskId, int affiliationColor, Drawable icon, Bitmap thumbnail, String title, String titleDescription, String dismissDescription, String appInfoDescription, int colorPrimary, int colorBackground, boolean isLaunchTarget, boolean isStackTask, boolean isSystemApp, boolean isDockable, Rect bounds, ActivityManager.TaskDescription taskDescription, int resizeMode, ComponentName topActivity) {
        boolean isInAffiliationGroup = affiliationTaskId != key.id;
        boolean hasAffiliationGroupColor = isInAffiliationGroup && affiliationColor != 0;
        this.key = key;
        this.affiliationTaskId = affiliationTaskId;
        this.affiliationColor = affiliationColor;
        this.icon = icon;
        this.thumbnail = thumbnail;
        this.title = title;
        this.titleDescription = titleDescription;
        this.dismissDescription = dismissDescription;
        this.appInfoDescription = appInfoDescription;
        this.colorPrimary = hasAffiliationGroupColor ? affiliationColor : colorPrimary;
        this.colorBackground = colorBackground;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) > 3.0f;
        this.bounds = bounds;
        this.taskDescription = taskDescription;
        this.isLaunchTarget = isLaunchTarget;
        this.isStackTask = isStackTask;
        this.isSystemApp = isSystemApp;
        this.isDockable = isDockable;
        this.resizeMode = resizeMode;
        this.topActivity = topActivity;
    }

    public void copyFrom(Task o) {
        this.key = o.key;
        this.group = o.group;
        this.affiliationTaskId = o.affiliationTaskId;
        this.affiliationColor = o.affiliationColor;
        this.icon = o.icon;
        this.thumbnail = o.thumbnail;
        this.title = o.title;
        this.titleDescription = o.titleDescription;
        this.dismissDescription = o.dismissDescription;
        this.appInfoDescription = o.appInfoDescription;
        this.colorPrimary = o.colorPrimary;
        this.colorBackground = o.colorBackground;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.bounds = o.bounds;
        this.taskDescription = o.taskDescription;
        this.isLaunchTarget = o.isLaunchTarget;
        this.isStackTask = o.isStackTask;
        this.isSystemApp = o.isSystemApp;
        this.isDockable = o.isDockable;
        this.resizeMode = o.resizeMode;
        this.topActivity = o.topActivity;
    }

    public void addCallback(TaskCallbacks cb) {
        if (this.mCallbacks.contains(cb)) {
            return;
        }
        this.mCallbacks.add(cb);
    }

    public void removeCallback(TaskCallbacks cb) {
        this.mCallbacks.remove(cb);
    }

    public void setGroup(TaskGrouping group) {
        this.group = group;
    }

    public void setStackId(int stackId) {
        this.key.setStackId(stackId);
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskStackIdChanged();
        }
    }

    public boolean isFreeformTask() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.hasFreeformWorkspaceSupport()) {
            return SystemServicesProxy.isFreeformStack(this.key.stackId);
        }
        return false;
    }

    public void notifyTaskDataLoaded(Bitmap thumbnail, Drawable applicationIcon, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        this.icon = applicationIcon;
        this.thumbnail = thumbnail;
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskDataLoaded(this, thumbnailInfo);
        }
    }

    public void notifyTaskDataUnloaded(Bitmap defaultThumbnail, Drawable defaultApplicationIcon) {
        this.icon = defaultApplicationIcon;
        this.thumbnail = defaultThumbnail;
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            this.mCallbacks.get(i).onTaskDataUnloaded();
        }
    }

    public boolean isAffiliatedTask() {
        return this.key.id != this.affiliationTaskId;
    }

    public ComponentName getTopComponent() {
        if (this.topActivity != null) {
            return this.topActivity;
        }
        return this.key.baseIntent.getComponent();
    }

    public boolean equals(Object o) {
        Task t = (Task) o;
        return this.key.equals(t.key);
    }

    public String toString() {
        return "[" + this.key.toString() + "] " + this.title;
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.print(prefix);
        writer.print(this.key);
        if (isAffiliatedTask()) {
            writer.print(" ");
            writer.print("affTaskId=" + this.affiliationTaskId);
        }
        if (!this.isDockable) {
            writer.print(" dockable=N");
        }
        if (this.isLaunchTarget) {
            writer.print(" launchTarget=Y");
        }
        if (isFreeformTask()) {
            writer.print(" freeform=Y");
        }
        writer.print(" ");
        writer.print(this.title);
        writer.println();
    }
}
