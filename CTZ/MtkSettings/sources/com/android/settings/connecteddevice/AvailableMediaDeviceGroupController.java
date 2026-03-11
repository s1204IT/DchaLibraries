package com.android.settings.connecteddevice;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class AvailableMediaDeviceGroupController extends BasePreferenceController implements DevicePreferenceCallback, BluetoothCallback, LifecycleObserver, OnStart, OnStop {
    private static final String KEY = "available_device_list";
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private final LocalBluetoothManager mLocalBluetoothManager;
    PreferenceGroup mPreferenceGroup;

    public AvailableMediaDeviceGroupController(Context context) {
        super(context, KEY);
        this.mLocalBluetoothManager = Utils.getLocalBtManager(this.mContext);
    }

    @Override
    public void onStart() {
        this.mBluetoothDeviceUpdater.registerCallback();
        this.mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    public void onStop() {
        this.mBluetoothDeviceUpdater.unregisterCallback();
        this.mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreferenceGroup = (PreferenceGroup) preferenceScreen.findPreference(KEY);
            this.mPreferenceGroup.setVisible(false);
            updateTitle();
            this.mBluetoothDeviceUpdater.setPrefContext(preferenceScreen.getContext());
            this.mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            return 0;
        }
        return 2;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (this.mPreferenceGroup.getPreferenceCount() == 0) {
            this.mPreferenceGroup.setVisible(true);
        }
        this.mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        this.mPreferenceGroup.removePreference(preference);
        if (this.mPreferenceGroup.getPreferenceCount() == 0) {
            this.mPreferenceGroup.setVisible(false);
        }
    }

    public void init(DashboardFragment dashboardFragment) {
        this.mBluetoothDeviceUpdater = new AvailableMediaBluetoothDeviceUpdater(dashboardFragment.getContext(), dashboardFragment, this);
    }

    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        this.mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @Override
    public void onBluetoothStateChanged(int i) {
    }

    @Override
    public void onScanningStateChanged(boolean z) {
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onAudioModeChanged() {
        updateTitle();
    }

    private void updateTitle() {
        if (com.android.settingslib.Utils.isAudioModeOngoingCall(this.mContext)) {
            this.mPreferenceGroup.setTitle(this.mContext.getString(R.string.connected_device_available_call_title));
        } else {
            this.mPreferenceGroup.setTitle(this.mContext.getString(R.string.connected_device_available_media_title));
        }
    }
}
