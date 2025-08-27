package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;

/* loaded from: classes.dex */
public class DraggingInRecentsEndedEvent extends EventBus.Event {
    public final float velocity;

    public DraggingInRecentsEndedEvent(float f) {
        this.velocity = f;
    }
}
