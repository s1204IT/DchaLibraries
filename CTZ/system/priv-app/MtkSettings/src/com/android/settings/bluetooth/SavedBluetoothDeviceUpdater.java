package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class SavedBluetoothDeviceUpdater extends BluetoothDeviceUpdater implements Preference.OnPreferenceClickListener {
    public SavedBluetoothDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback) {
        super(context, dashboardFragment, devicePreferenceCallback);
    }

    SavedBluetoothDeviceUpdater(DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localBluetoothManager) {
        super(dashboardFragment, devicePreferenceCallback, localBluetoothManager);
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater, com.android.settingslib.bluetooth.BluetoothCallback
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
        if (i == 2) {
            removePreference(cachedBluetoothDevice);
        } else if (i == 0) {
            addPreference(cachedBluetoothDevice);
        }
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater
    public boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice) {
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        return device.getBondState() == 12 && !device.isConnected();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        ((BluetoothDevicePreference) preference).getBluetoothDevice().connect(true);
        return true;
    }
}
