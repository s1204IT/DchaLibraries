package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.shared.recents.model.TaskStack;

public class MultiWindowStateChangedEvent extends EventBus.AnimatedEvent {
    public final boolean inMultiWindow;
    public final boolean showDeferredAnimation;
    public final TaskStack stack;

    public MultiWindowStateChangedEvent(boolean z, boolean z2, TaskStack taskStack) {
        this.inMultiWindow = z;
        this.showDeferredAnimation = z2;
        this.stack = taskStack;
    }
}
