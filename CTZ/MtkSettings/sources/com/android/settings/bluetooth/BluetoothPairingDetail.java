package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.search.Indexable;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.widget.FooterPreference;

public class BluetoothPairingDetail extends DeviceListPreferenceFragment implements Indexable {
    static final String KEY_AVAIL_DEVICES = "available_devices";
    static final String KEY_FOOTER_PREF = "footer_preference";
    AlwaysDiscoverable mAlwaysDiscoverable;
    BluetoothProgressCategory mAvailableDevicesCategory;
    FooterPreference mFooterPreference;
    private boolean mInitialScanStarted;

    public BluetoothPairingDetail() {
        super("no_config_bluetooth");
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mInitialScanStarted = false;
        this.mAlwaysDiscoverable = new AlwaysDiscoverable(getContext(), this.mLocalAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mLocalManager == null) {
            Log.e("BluetoothPairingDetail", "Bluetooth is not supported on this device");
        } else {
            updateBluetooth();
            this.mAvailableDevicesCategory.setProgress(this.mLocalAdapter.isDiscovering());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((BluetoothDeviceRenamePreferenceController) use(BluetoothDeviceRenamePreferenceController.class)).setFragment(this);
    }

    void updateBluetooth() {
        if (this.mLocalAdapter.isEnabled()) {
            updateContent(this.mLocalAdapter.getBluetoothState());
        } else {
            this.mLocalAdapter.enable();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mLocalManager == null) {
            Log.e("BluetoothPairingDetail", "Bluetooth is not supported on this device");
        } else {
            this.mAlwaysDiscoverable.stop();
            disableScanning();
        }
    }

    @Override
    void initPreferencesFromPreferenceScreen() {
        this.mAvailableDevicesCategory = (BluetoothProgressCategory) findPreference(KEY_AVAIL_DEVICES);
        this.mFooterPreference = (FooterPreference) findPreference(KEY_FOOTER_PREF);
        this.mFooterPreference.setSelectable(false);
    }

    @Override
    public int getMetricsCategory() {
        return 1018;
    }

    @Override
    void enableScanning() {
        if (!this.mInitialScanStarted) {
            if (this.mAvailableDevicesCategory != null) {
                removeAllDevices();
            }
            this.mLocalManager.getCachedDeviceManager().clearNonBondedDevices();
            this.mInitialScanStarted = true;
        }
        super.enableScanning();
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference bluetoothDevicePreference) {
        disableScanning();
        super.onDevicePreferenceClick(bluetoothDevicePreference);
    }

    @Override
    public void onScanningStateChanged(boolean z) {
        super.onScanningStateChanged(z);
        this.mAvailableDevicesCategory.setProgress(z | this.mScanEnabled);
    }

    void updateContent(int i) {
        if (i == 10) {
            finish();
            return;
        }
        if (i == 12) {
            this.mDevicePreferenceMap.clear();
            this.mLocalAdapter.setBluetoothEnabled(true);
            addDeviceCategory(this.mAvailableDevicesCategory, R.string.bluetooth_preference_found_media_devices, BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, this.mInitialScanStarted);
            updateFooterPreference(this.mFooterPreference);
            this.mAlwaysDiscoverable.start();
            enableScanning();
        }
    }

    @Override
    public void onBluetoothStateChanged(int i) {
        super.onBluetoothStateChanged(i);
        updateContent(i);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        BluetoothDevice device;
        if (i == 12) {
            finish();
            return;
        }
        if (this.mSelectedDevice != null && cachedBluetoothDevice != null && (device = cachedBluetoothDevice.getDevice()) != null && this.mSelectedDevice.equals(device) && i == 10) {
            enableScanning();
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    @Override
    protected String getLogTag() {
        return "BluetoothPairingDetail";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_pairing_detail;
    }

    @Override
    public String getDeviceListKey() {
        return KEY_AVAIL_DEVICES;
    }
}
