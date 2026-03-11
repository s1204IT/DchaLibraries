package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.shared.recents.model.Task;

public class CancelEnterRecentsWindowAnimationEvent extends EventBus.Event {
    public final Task launchTask;

    public CancelEnterRecentsWindowAnimationEvent(Task task) {
        this.launchTask = task;
    }
}
