package com.android.systemui.recents.events.ui.focus;

import com.android.systemui.recents.events.EventBus;

public class FocusNextTaskViewEvent extends EventBus.Event {
    public final int timerIndicatorDuration;

    public FocusNextTaskViewEvent(int timerIndicatorDuration) {
        this.timerIndicatorDuration = timerIndicatorDuration;
    }
}
