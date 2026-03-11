package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;

public class ShowStackActionButtonEvent extends EventBus.Event {
    public final boolean translate;

    public ShowStackActionButtonEvent(boolean z) {
        this.translate = z;
    }
}
