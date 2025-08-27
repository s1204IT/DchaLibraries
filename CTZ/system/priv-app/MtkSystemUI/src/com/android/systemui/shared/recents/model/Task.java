package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ViewDebug;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public class Task {

    @ViewDebug.ExportedProperty(category = "recents")
    public int colorBackground;

    @ViewDebug.ExportedProperty(category = "recents")
    public int colorPrimary;
    public Drawable icon;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isDockable;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLaunchTarget;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLocked;

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
    public ThumbnailData thumbnail;

    @ViewDebug.ExportedProperty(category = "recents")
    public String title;

    @ViewDebug.ExportedProperty(category = "recents")
    public String titleDescription;

    @ViewDebug.ExportedProperty(category = "recents")
    public ComponentName topActivity;

    @ViewDebug.ExportedProperty(category = "recents")
    public boolean useLightOnPrimaryColor;

    public interface TaskCallbacks {
        void onTaskDataLoaded(Task task, ThumbnailData thumbnailData);

        void onTaskDataUnloaded();
    }

    public static class TaskKey {

        @ViewDebug.ExportedProperty(category = "recents")
        public final Intent baseIntent;

        @ViewDebug.ExportedProperty(category = "recents")
        public final int id;

        @ViewDebug.ExportedProperty(category = "recents")
        public long lastActiveTime;
        private int mHashCode;
        public final ComponentName sourceComponent;

        @ViewDebug.ExportedProperty(category = "recents")
        public final int userId;

        @ViewDebug.ExportedProperty(category = "recents")
        public int windowingMode;

        public TaskKey(int i, int i2, Intent intent, ComponentName componentName, int i3, long j) {
            this.id = i;
            this.windowingMode = i2;
            this.baseIntent = intent;
            this.sourceComponent = componentName;
            this.userId = i3;
            this.lastActiveTime = j;
            updateHashCode();
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        public String getPackageName() {
            if (this.baseIntent.getComponent() != null) {
                return this.baseIntent.getComponent().getPackageName();
            }
            return this.baseIntent.getPackage();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof TaskKey)) {
                return false;
            }
            TaskKey taskKey = (TaskKey) obj;
            return this.id == taskKey.id && this.windowingMode == taskKey.windowingMode && this.userId == taskKey.userId;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        public String toString() {
            return "id=" + this.id + " windowingMode=" + this.windowingMode + " user=" + this.userId + " lastActiveTime=" + this.lastActiveTime;
        }

        private void updateHashCode() {
            this.mHashCode = Objects.hash(Integer.valueOf(this.id), Integer.valueOf(this.windowingMode), Integer.valueOf(this.userId));
        }
    }

    public Task() {
    }

    public Task(TaskKey taskKey, Drawable drawable, ThumbnailData thumbnailData, String str, String str2, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, ActivityManager.TaskDescription taskDescription, int i3, ComponentName componentName, boolean z5) {
        this.key = taskKey;
        this.icon = drawable;
        this.thumbnail = thumbnailData;
        this.title = str;
        this.titleDescription = str2;
        this.colorPrimary = i;
        this.colorBackground = i2;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) > 3.0f;
        this.taskDescription = taskDescription;
        this.isLaunchTarget = z;
        this.isStackTask = z2;
        this.isSystemApp = z3;
        this.isDockable = z4;
        this.resizeMode = i3;
        this.topActivity = componentName;
        this.isLocked = z5;
    }

    public void copyFrom(Task task) {
        this.key = task.key;
        this.icon = task.icon;
        this.thumbnail = task.thumbnail;
        this.title = task.title;
        this.titleDescription = task.titleDescription;
        this.colorPrimary = task.colorPrimary;
        this.colorBackground = task.colorBackground;
        this.useLightOnPrimaryColor = task.useLightOnPrimaryColor;
        this.taskDescription = task.taskDescription;
        this.isLaunchTarget = task.isLaunchTarget;
        this.isStackTask = task.isStackTask;
        this.isSystemApp = task.isSystemApp;
        this.isDockable = task.isDockable;
        this.resizeMode = task.resizeMode;
        this.isLocked = task.isLocked;
        this.topActivity = task.topActivity;
    }

    public void addCallback(TaskCallbacks taskCallbacks) {
        if (!this.mCallbacks.contains(taskCallbacks)) {
            this.mCallbacks.add(taskCallbacks);
        }
    }

    public void removeCallback(TaskCallbacks taskCallbacks) {
        this.mCallbacks.remove(taskCallbacks);
    }

    public void notifyTaskDataLoaded(ThumbnailData thumbnailData, Drawable drawable) {
        this.icon = drawable;
        this.thumbnail = thumbnailData;
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            this.mCallbacks.get(i).onTaskDataLoaded(this, thumbnailData);
        }
    }

    public void notifyTaskDataUnloaded(Drawable drawable) {
        this.icon = drawable;
        this.thumbnail = null;
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            this.mCallbacks.get(size).onTaskDataUnloaded();
        }
    }

    public ComponentName getTopComponent() {
        if (this.topActivity != null) {
            return this.topActivity;
        }
        return this.key.baseIntent.getComponent();
    }

    public boolean equals(Object obj) {
        return this.key.equals(((Task) obj).key);
    }

    public String toString() {
        return "[" + this.key.toString() + "] " + this.title;
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print(this.key);
        if (!this.isDockable) {
            printWriter.print(" dockable=N");
        }
        if (this.isLaunchTarget) {
            printWriter.print(" launchTarget=Y");
        }
        if (this.isLocked) {
            printWriter.print(" locked=Y");
        }
        printWriter.print(" ");
        printWriter.print(this.title);
        printWriter.println();
    }
}
