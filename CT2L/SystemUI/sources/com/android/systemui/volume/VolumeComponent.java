package com.android.systemui.volume;

import com.android.systemui.DemoMode;
import com.android.systemui.statusbar.policy.ZenModeController;

public interface VolumeComponent extends DemoMode {
    void dismissNow();

    ZenModeController getZenController();
}
