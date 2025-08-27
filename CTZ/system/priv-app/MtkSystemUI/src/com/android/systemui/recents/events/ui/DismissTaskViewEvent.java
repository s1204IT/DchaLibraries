package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;

/* loaded from: classes.dex */
public class DismissTaskViewEvent extends EventBus.AnimatedEvent {
    public final TaskView taskView;

    public DismissTaskViewEvent(TaskView taskView) {
        this.taskView = taskView;
    }
}
