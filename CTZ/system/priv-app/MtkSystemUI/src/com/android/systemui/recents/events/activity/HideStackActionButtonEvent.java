package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
/* loaded from: classes.dex */
public class HideStackActionButtonEvent extends EventBus.Event {
    public final boolean translate;

    public HideStackActionButtonEvent() {
        this(true);
    }

    public HideStackActionButtonEvent(boolean z) {
        this.translate = z;
    }
}
