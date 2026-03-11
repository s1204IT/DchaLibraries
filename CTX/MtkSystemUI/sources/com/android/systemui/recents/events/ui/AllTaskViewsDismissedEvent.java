package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;

public class AllTaskViewsDismissedEvent extends EventBus.Event {
    public final int msgResId;

    public AllTaskViewsDismissedEvent(int i) {
        this.msgResId = i;
    }
}
