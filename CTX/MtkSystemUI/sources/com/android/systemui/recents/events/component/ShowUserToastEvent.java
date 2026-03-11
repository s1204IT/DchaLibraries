package com.android.systemui.recents.events.component;

import com.android.systemui.recents.events.EventBus;

public class ShowUserToastEvent extends EventBus.Event {
    public final int msgLength;
    public final int msgResId;

    public ShowUserToastEvent(int i, int i2) {
        this.msgResId = i;
        this.msgLength = i2;
    }
}
