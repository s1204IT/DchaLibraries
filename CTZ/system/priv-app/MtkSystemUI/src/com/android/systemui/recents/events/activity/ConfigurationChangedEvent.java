package com.android.systemui.recents.events.activity;

import com.android.systemui.recents.events.EventBus;
/* loaded from: classes.dex */
public class ConfigurationChangedEvent extends EventBus.AnimatedEvent {
    public final boolean fromDeviceOrientationChange;
    public final boolean fromDisplayDensityChange;
    public final boolean fromMultiWindow;
    public final boolean hasStackTasks;

    public ConfigurationChangedEvent(boolean z, boolean z2, boolean z3, boolean z4) {
        this.fromMultiWindow = z;
        this.fromDeviceOrientationChange = z2;
        this.fromDisplayDensityChange = z3;
        this.hasStackTasks = z4;
    }
}
