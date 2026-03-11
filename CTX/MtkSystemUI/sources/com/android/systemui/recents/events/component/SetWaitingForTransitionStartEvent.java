package com.android.systemui.recents.events.component;

import com.android.systemui.recents.events.EventBus;

public class SetWaitingForTransitionStartEvent extends EventBus.Event {
    public final boolean waitingForTransitionStart;

    public SetWaitingForTransitionStartEvent(boolean z) {
        this.waitingForTransitionStart = z;
    }
}
