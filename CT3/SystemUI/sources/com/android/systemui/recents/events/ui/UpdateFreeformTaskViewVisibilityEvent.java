package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;

public class UpdateFreeformTaskViewVisibilityEvent extends EventBus.Event {
    public final boolean visible;

    public UpdateFreeformTaskViewVisibilityEvent(boolean visible) {
        this.visible = visible;
    }
}
