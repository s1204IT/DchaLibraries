package com.android.systemui.statusbar.policy;

public interface LocationController extends CallbackController<LocationChangeCallback> {
    boolean isLocationActive();

    boolean isLocationEnabled();

    boolean setLocationEnabled(boolean z);

    public interface LocationChangeCallback {
        default void onLocationActiveChanged(boolean z) {
        }

        default void onLocationSettingsChanged(boolean z) {
        }
    }
}
