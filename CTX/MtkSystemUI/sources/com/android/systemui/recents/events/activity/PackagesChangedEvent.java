package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;

public class PackagesChangedEvent extends EventBus.Event {
    public final String packageName;
    public final int userId;

    public PackagesChangedEvent(String str, int i) {
        this.packageName = str;
        this.userId = i;
    }
}
