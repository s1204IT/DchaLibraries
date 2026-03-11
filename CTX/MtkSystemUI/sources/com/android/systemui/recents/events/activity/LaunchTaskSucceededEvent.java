package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;

public class LaunchTaskSucceededEvent extends EventBus.Event {
    public final int taskIndexFromStackFront;

    public LaunchTaskSucceededEvent(int i) {
        this.taskIndexFromStackFront = i;
    }
}
