package com.android.systemui.recents.model;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.android.systemui.recents.misc.Utilities;
import java.util.Objects;

public class Task {
    public Drawable activityIcon;
    public String activityLabel;
    public Drawable applicationIcon;
    public int colorPrimary;
    public TaskGrouping group;
    public Bitmap icon;
    public String iconFilename;
    public boolean isActive;
    public boolean isLaunchTarget;
    public TaskKey key;
    public boolean lockToTaskEnabled;
    public boolean lockToThisTask;
    TaskCallbacks mCb;
    public int taskAffiliation;
    public int taskAffiliationColor;
    public Bitmap thumbnail;
    public boolean useLightOnPrimaryColor;

    public interface TaskCallbacks {
        void onTaskDataLoaded();

        void onTaskDataUnloaded();
    }

    public static class ComponentNameKey {
        final ComponentName component;
        final int userId;

        public ComponentNameKey(ComponentName cn, int user) {
            this.component = cn;
            this.userId = user;
        }

        public int hashCode() {
            return Objects.hash(this.component, Integer.valueOf(this.userId));
        }

        public boolean equals(Object o) {
            if (o instanceof ComponentNameKey) {
                return this.component.equals(((ComponentNameKey) o).component) && this.userId == ((ComponentNameKey) o).userId;
            }
            return false;
        }
    }

    public static class TaskKey {
        public final Intent baseIntent;
        public long firstActiveTime;
        public final int id;
        public long lastActiveTime;
        final ComponentNameKey mComponentNameKey;
        public final int userId;

        public TaskKey(int id, Intent intent, int userId, long firstActiveTime, long lastActiveTime) {
            this.mComponentNameKey = new ComponentNameKey(intent.getComponent(), userId);
            this.id = id;
            this.baseIntent = intent;
            this.userId = userId;
            this.firstActiveTime = firstActiveTime;
            this.lastActiveTime = lastActiveTime;
        }

        public ComponentNameKey getComponentNameKey() {
            return this.mComponentNameKey;
        }

        public boolean equals(Object o) {
            if (o instanceof TaskKey) {
                return this.id == ((TaskKey) o).id && this.userId == ((TaskKey) o).userId;
            }
            return false;
        }

        public int hashCode() {
            return (this.id << 5) + this.userId;
        }

        public String toString() {
            return "Task.Key: " + this.id + ", u: " + this.userId + ", lat: " + this.lastActiveTime + ", " + this.baseIntent.getComponent().getPackageName();
        }
    }

    public Task() {
    }

    public Task(TaskKey key, boolean isActive, int taskAffiliation, int taskAffiliationColor, String activityTitle, Drawable activityIcon, int colorPrimary, boolean lockToThisTask, boolean lockToTaskEnabled, Bitmap icon, String iconFilename) {
        boolean isInAffiliationGroup = taskAffiliation != key.id;
        boolean hasAffiliationGroupColor = isInAffiliationGroup && taskAffiliationColor != 0;
        this.key = key;
        this.taskAffiliation = taskAffiliation;
        this.taskAffiliationColor = taskAffiliationColor;
        this.activityLabel = activityTitle;
        this.activityIcon = activityIcon;
        this.colorPrimary = hasAffiliationGroupColor ? taskAffiliationColor : colorPrimary;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) > 3.0f;
        this.isActive = isActive;
        this.lockToThisTask = lockToTaskEnabled && lockToThisTask;
        this.lockToTaskEnabled = lockToTaskEnabled;
        this.icon = icon;
        this.iconFilename = iconFilename;
    }

    public void copyFrom(Task o) {
        this.key = o.key;
        this.taskAffiliation = o.taskAffiliation;
        this.taskAffiliationColor = o.taskAffiliationColor;
        this.activityLabel = o.activityLabel;
        this.activityIcon = o.activityIcon;
        this.colorPrimary = o.colorPrimary;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.isActive = o.isActive;
        this.lockToThisTask = o.lockToThisTask;
        this.lockToTaskEnabled = o.lockToTaskEnabled;
    }

    public void setCallbacks(TaskCallbacks cb) {
        this.mCb = cb;
    }

    public void setGroup(TaskGrouping group) {
        if (group != null && this.group != null) {
            throw new RuntimeException("This task is already assigned to a group.");
        }
        this.group = group;
    }

    public void notifyTaskDataLoaded(Bitmap thumbnail, Drawable applicationIcon) {
        this.applicationIcon = applicationIcon;
        this.thumbnail = thumbnail;
        if (this.mCb != null) {
            this.mCb.onTaskDataLoaded();
        }
    }

    public void notifyTaskDataUnloaded(Bitmap defaultThumbnail, Drawable defaultApplicationIcon) {
        this.applicationIcon = defaultApplicationIcon;
        this.thumbnail = defaultThumbnail;
        if (this.mCb != null) {
            this.mCb.onTaskDataUnloaded();
        }
    }

    public boolean equals(Object o) {
        Task t = (Task) o;
        return this.key.equals(t.key);
    }

    public String toString() {
        String groupAffiliation = "no group";
        if (this.group != null) {
            groupAffiliation = Integer.toString(this.group.affiliation);
        }
        return "Task (" + groupAffiliation + "): " + this.key.baseIntent.getComponent().getPackageName() + " [" + super.toString() + "]";
    }
}
