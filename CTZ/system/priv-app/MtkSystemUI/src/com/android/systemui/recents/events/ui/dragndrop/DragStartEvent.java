package com.android.systemui.recents.events.ui.dragndrop;

import android.graphics.Point;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

/* loaded from: classes.dex */
public class DragStartEvent extends EventBus.Event {
    public final boolean isUserTouchInitiated;
    public final Task task;
    public final TaskView taskView;
    public final Point tlOffset;

    public DragStartEvent(Task task, TaskView taskView, Point point) {
        this(task, taskView, point, true);
    }

    public DragStartEvent(Task task, TaskView taskView, Point point, boolean z) {
        this.task = task;
        this.taskView = taskView;
        this.tlOffset = point;
        this.isUserTouchInitiated = z;
    }
}
