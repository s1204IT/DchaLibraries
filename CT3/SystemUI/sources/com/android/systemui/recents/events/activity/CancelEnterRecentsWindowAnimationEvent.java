package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.model.Task;

public class CancelEnterRecentsWindowAnimationEvent extends EventBus.Event {
    public final Task launchTask;

    public CancelEnterRecentsWindowAnimationEvent(Task launchTask) {
        this.launchTask = launchTask;
    }
}
