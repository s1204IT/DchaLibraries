package com.android.systemui.recents.events.ui.dragndrop;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.DropTarget;

public class DragDropTargetChangedEvent extends EventBus.AnimatedEvent {
    public final DropTarget dropTarget;
    public final Task task;

    public DragDropTargetChangedEvent(Task task, DropTarget dropTarget) {
        this.task = task;
        this.dropTarget = dropTarget;
    }
}
