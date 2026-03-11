package com.android.settings.connecteddevice;

import android.support.v7.preference.Preference;

public interface DevicePreferenceCallback {
    void onDeviceAdded(Preference preference);

    void onDeviceRemoved(Preference preference);
}
