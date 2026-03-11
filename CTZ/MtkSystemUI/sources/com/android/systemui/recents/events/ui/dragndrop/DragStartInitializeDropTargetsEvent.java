package com.android.systemui.recents.events.ui.dragndrop;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.RecentsViewTouchHandler;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.model.Task;

public class DragStartInitializeDropTargetsEvent extends EventBus.Event {
    public final RecentsViewTouchHandler handler;
    public final Task task;
    public final TaskView taskView;

    public DragStartInitializeDropTargetsEvent(Task task, TaskView taskView, RecentsViewTouchHandler recentsViewTouchHandler) {
        this.task = task;
        this.taskView = taskView;
        this.handler = recentsViewTouchHandler;
    }
}
