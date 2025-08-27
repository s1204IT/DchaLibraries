package com.android.systemui.recents.events.ui.dragndrop;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.DropTarget;
import com.android.systemui.shared.recents.model.Task;

/* loaded from: classes.dex */
public class DragDropTargetChangedEvent extends EventBus.AnimatedEvent {
    public final DropTarget dropTarget;
    public final Task task;

    public DragDropTargetChangedEvent(Task task, DropTarget dropTarget) {
        this.task = task;
        this.dropTarget = dropTarget;
    }
}
