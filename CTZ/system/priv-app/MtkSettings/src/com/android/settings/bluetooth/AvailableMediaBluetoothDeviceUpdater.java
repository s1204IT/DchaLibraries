package com.android.settings.bluetooth;

import android.content.Context;
import android.media.AudioManager;
import android.support.v7.preference.Preference;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class AvailableMediaBluetoothDeviceUpdater extends BluetoothDeviceUpdater implements Preference.OnPreferenceClickListener {
    private final AudioManager mAudioManager;

    public AvailableMediaBluetoothDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback) {
        super(context, dashboardFragment, devicePreferenceCallback);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    AvailableMediaBluetoothDeviceUpdater(DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localBluetoothManager) {
        super(dashboardFragment, devicePreferenceCallback, localBluetoothManager);
        this.mAudioManager = (AudioManager) dashboardFragment.getContext().getSystemService("audio");
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater, com.android.settingslib.bluetooth.BluetoothCallback
    public void onAudioModeChanged() {
        forceUpdate();
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater, com.android.settingslib.bluetooth.BluetoothCallback
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
        if (i == 2) {
            if (isFilterMatched(cachedBluetoothDevice)) {
                addPreference(cachedBluetoothDevice);
                return;
            } else {
                removePreference(cachedBluetoothDevice);
                return;
            }
        }
        if (i == 0) {
            removePreference(cachedBluetoothDevice);
        }
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater
    public boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice) {
        int mode = this.mAudioManager.getMode();
        char c = 2;
        if (mode == 1 || mode == 2 || mode == 3) {
            c = 1;
        }
        if (!isDeviceConnected(cachedBluetoothDevice)) {
            return false;
        }
        switch (c) {
            case 1:
                return cachedBluetoothDevice.isHfpDevice();
            case 2:
                return cachedBluetoothDevice.isA2dpDevice();
            default:
                return false;
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        return ((BluetoothDevicePreference) preference).getBluetoothDevice().setActive();
    }
}
