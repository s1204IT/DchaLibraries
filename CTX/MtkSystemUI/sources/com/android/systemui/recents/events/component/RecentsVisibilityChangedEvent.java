package com.android.systemui.recents.events.component;

import android.content.Context;
import com.android.systemui.recents.events.EventBus;

public class RecentsVisibilityChangedEvent extends EventBus.Event {
    public final Context applicationContext;
    public final boolean visible;

    public RecentsVisibilityChangedEvent(Context context, boolean z) {
        this.applicationContext = context.getApplicationContext();
        this.visible = z;
    }
}
