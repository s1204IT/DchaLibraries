package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.views.TaskView;

public class LaunchTaskStartedEvent extends EventBus.AnimatedEvent {
    public final boolean screenPinningRequested;
    public final TaskView taskView;

    public LaunchTaskStartedEvent(TaskView taskView, boolean screenPinningRequested) {
        this.taskView = taskView;
        this.screenPinningRequested = screenPinningRequested;
    }
}
