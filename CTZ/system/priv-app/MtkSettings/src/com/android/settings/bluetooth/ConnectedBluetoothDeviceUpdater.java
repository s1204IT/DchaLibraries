package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.support.v7.preference.Preference;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class ConnectedBluetoothDeviceUpdater extends BluetoothDeviceUpdater {
    private final AudioManager mAudioManager;

    public ConnectedBluetoothDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback) {
        super(context, dashboardFragment, devicePreferenceCallback);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    ConnectedBluetoothDeviceUpdater(DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localBluetoothManager) {
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
                return !cachedBluetoothDevice.isHfpDevice();
            case 2:
                return !cachedBluetoothDevice.isA2dpDevice();
            default:
                return false;
        }
    }

    @Override // com.android.settings.bluetooth.BluetoothDeviceUpdater
    protected void addPreference(CachedBluetoothDevice cachedBluetoothDevice) {
        super.addPreference(cachedBluetoothDevice);
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        if (this.mPreferenceMap.containsKey(device)) {
            BluetoothDevicePreference bluetoothDevicePreference = (BluetoothDevicePreference) this.mPreferenceMap.get(device);
            bluetoothDevicePreference.setOnGearClickListener(null);
            bluetoothDevicePreference.hideSecondTarget(true);
            bluetoothDevicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.bluetooth.-$$Lambda$ConnectedBluetoothDeviceUpdater$cbcA1LEPXmJVOc_WhespijdA8X8
                @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                public final boolean onPreferenceClick(Preference preference) {
                    return ConnectedBluetoothDeviceUpdater.lambda$addPreference$0(this.f$0, preference);
                }
            });
        }
    }

    public static /* synthetic */ boolean lambda$addPreference$0(ConnectedBluetoothDeviceUpdater connectedBluetoothDeviceUpdater, Preference preference) {
        connectedBluetoothDeviceUpdater.launchDeviceDetails(preference);
        return true;
    }
}
