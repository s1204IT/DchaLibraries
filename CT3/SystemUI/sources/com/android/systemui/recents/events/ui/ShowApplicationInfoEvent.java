package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.model.Task;

public class ShowApplicationInfoEvent extends EventBus.Event {
    public final Task task;

    public ShowApplicationInfoEvent(Task task) {
        this.task = task;
    }
}
