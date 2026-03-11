package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;

public class DraggingInRecentsEndedEvent extends EventBus.Event {
    public final float velocity;

    public DraggingInRecentsEndedEvent(float f) {
        this.velocity = f;
    }
}
