package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;

public class DismissRecentsToHomeAnimationStarted extends EventBus.AnimatedEvent {
    public final boolean animated;

    public DismissRecentsToHomeAnimationStarted(boolean z) {
        this.animated = z;
    }
}
