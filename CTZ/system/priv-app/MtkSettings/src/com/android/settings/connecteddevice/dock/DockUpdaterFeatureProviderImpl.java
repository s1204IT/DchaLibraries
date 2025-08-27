package com.android.settings.connecteddevice.dock;

import android.content.Context;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.overlay.DockUpdaterFeatureProvider;

/* loaded from: classes.dex */
public class DockUpdaterFeatureProviderImpl implements DockUpdaterFeatureProvider {
    @Override // com.android.settings.overlay.DockUpdaterFeatureProvider
    public DockUpdater getConnectedDockUpdater(Context context, DevicePreferenceCallback devicePreferenceCallback) {
        return new DockUpdater() { // from class: com.android.settings.connecteddevice.dock.DockUpdaterFeatureProviderImpl.1
        };
    }

    @Override // com.android.settings.overlay.DockUpdaterFeatureProvider
    public DockUpdater getSavedDockUpdater(Context context, DevicePreferenceCallback devicePreferenceCallback) {
        return new DockUpdater() { // from class: com.android.settings.connecteddevice.dock.DockUpdaterFeatureProviderImpl.2
        };
    }
}
