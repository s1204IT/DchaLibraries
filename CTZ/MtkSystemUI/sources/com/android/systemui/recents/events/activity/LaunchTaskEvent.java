package com.android.systemui.recents.events.activity;

import android.graphics.Rect;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class LaunchTaskEvent extends EventBus.Event {
    public final boolean screenPinningRequested;
    public final int targetActivityType;
    public final Rect targetTaskBounds;
    public final int targetWindowingMode;
    public final Task task;
    public final TaskView taskView;

    public LaunchTaskEvent(TaskView taskView, Task task, Rect rect, boolean z) {
        this(taskView, task, rect, z, 0, 0);
    }

    public LaunchTaskEvent(TaskView taskView, Task task, Rect rect, boolean z, int i, int i2) {
        this.taskView = taskView;
        this.task = task;
        this.targetTaskBounds = rect;
        this.targetWindowingMode = i;
        this.targetActivityType = i2;
        this.screenPinningRequested = z;
    }
}
