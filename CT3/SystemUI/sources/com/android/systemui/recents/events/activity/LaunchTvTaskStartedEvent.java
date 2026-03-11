package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.tv.views.TaskCardView;

public class LaunchTvTaskStartedEvent extends EventBus.AnimatedEvent {
    public final TaskCardView taskView;

    public LaunchTvTaskStartedEvent(TaskCardView taskView) {
        this.taskView = taskView;
    }
}
