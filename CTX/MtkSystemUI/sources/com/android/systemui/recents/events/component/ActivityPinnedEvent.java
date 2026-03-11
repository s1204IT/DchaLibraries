package com.android.systemui.recents.events.component;

import com.android.systemui.recents.events.EventBus;

public class ActivityPinnedEvent extends EventBus.Event {
    public final int taskId;

    public ActivityPinnedEvent(int i) {
        this.taskId = i;
    }
}
