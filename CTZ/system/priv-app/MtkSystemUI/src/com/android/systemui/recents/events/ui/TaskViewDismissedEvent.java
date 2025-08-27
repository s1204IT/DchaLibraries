package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.utilities.AnimationProps;

/* loaded from: classes.dex */
public class TaskViewDismissedEvent extends EventBus.Event {
    public final AnimationProps animation;
    public final Task task;
    public final TaskView taskView;

    public TaskViewDismissedEvent(Task task, TaskView taskView, AnimationProps animationProps) {
        this.task = task;
        this.taskView = taskView;
        this.animation = animationProps;
    }
}
