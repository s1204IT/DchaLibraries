package com.android.systemui.statusbar.policy;

import com.android.systemui.DemoMode;
import com.android.systemui.Dumpable;

/* loaded from: classes.dex */
public interface BatteryController extends DemoMode, Dumpable, CallbackController<BatteryStateChangeCallback> {
    boolean isPowerSave();

    void setPowerSaveMode(boolean z);

    default boolean isAodPowerSave() {
        return isPowerSave();
    }

    public interface BatteryStateChangeCallback {
        default void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        }

        default void onPowerSaveChanged(boolean z) {
        }
    }
}
