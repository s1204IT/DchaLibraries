package com.mediatek.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.android.settings.bluetooth.DeviceProfilesSettings;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.mediatek.settingslib.bluetooth.DunServerProfile;

public class DeviceProfilesSettingsExt {
    private Activity mActivity;
    private CachedBluetoothDevice mDevice;
    private DeviceProfilesSettings mDeviceProfilesSettings;

    public DeviceProfilesSettingsExt(Activity activity, DeviceProfilesSettings deviceProfileSettings, CachedBluetoothDevice device) {
        Log.d("DeviceProfilesSettingsExt", "DeviceProfilesSettingsExt");
        this.mActivity = activity;
        this.mDeviceProfilesSettings = deviceProfileSettings;
        this.mDevice = device;
    }

    public void addPreferencesForProfiles(ViewGroup viewgroup, CachedBluetoothDevice device) {
        Log.d("DeviceProfilesSettingsExt", "addPreferencesForProfiles");
        if (viewgroup == null || device == null) {
            return;
        }
        for (LocalBluetoothProfile profile : device.getConnectableProfiles()) {
            Log.d("DeviceProfilesSettingsExt", "profile.toString()=" + profile.toString());
            if (profile instanceof DunServerProfile) {
                CheckBox pref = createProfilePreference(profile, device);
                viewgroup.addView(pref);
            }
        }
    }

    private CheckBox createProfilePreference(LocalBluetoothProfile profile, CachedBluetoothDevice device) {
        Log.d("DeviceProfilesSettingsExt", "createProfilePreference");
        CheckBox pref = new CheckBox(this.mActivity);
        pref.setTag(profile.toString());
        pref.setText(profile.getNameResource(device.getDevice()));
        pref.setOnClickListener(this.mDeviceProfilesSettings);
        refreshProfilePreference(pref, profile);
        return pref;
    }

    private void refreshProfilePreference(CheckBox profilePref, LocalBluetoothProfile profile) {
        Log.d("DeviceProfilesSettingsExt", "refreshProfilePreference");
        BluetoothDevice device = this.mDevice.getDevice();
        if (!(profile instanceof DunServerProfile)) {
            return;
        }
        Log.d("DeviceProfilesSettingsExt", "DunProfile=" + (profile.getConnectionStatus(device) == 2));
        profilePref.setChecked(profile.getConnectionStatus(device) == 2);
    }
}
