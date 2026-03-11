package com.android.systemui.recents;

import android.content.Context;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DebugFlagsChangedEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.tuner.TunerService;

public class RecentsDebugFlags implements TunerService.Tunable {
    public RecentsDebugFlags(Context context) {
    }

    public boolean isFastToggleRecentsEnabled() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        return (ssp.hasFreeformWorkspaceSupport() || ssp.isTouchExplorationEnabled()) ? false : false;
    }

    public boolean isPagingEnabled() {
        return false;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        EventBus.getDefault().send(new DebugFlagsChangedEvent());
    }
}
