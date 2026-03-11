package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuInflater;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

public final class DevicePickerFragment extends DeviceListPreferenceFragment {
    BluetoothProgressCategory mAvailableDevicesCategory;
    private String mLaunchClass;
    private String mLaunchPackage;
    private boolean mNeedAuth;
    private ProgressCategory mProgressCategory;
    private boolean mScanAllowed;

    public DevicePickerFragment() {
        super(null);
    }

    @Override
    void initPreferencesFromPreferenceScreen() {
        Intent intent = getActivity().getIntent();
        this.mNeedAuth = intent.getBooleanExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
        setFilter(intent.getIntExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0));
        this.mLaunchPackage = intent.getStringExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE");
        this.mLaunchClass = intent.getStringExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS");
        this.mAvailableDevicesCategory = (BluetoothProgressCategory) findPreference("bt_device_list");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public int getMetricsCategory() {
        return 25;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(getString(R.string.device_picker));
        this.mScanAllowed = !((UserManager) getSystemService("user")).hasUserRestriction("no_config_bluetooth");
        setHasOptionsMenu(true);
        this.mProgressCategory = (ProgressCategory) findPreference("bt_device_list");
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mProgressCategory.setNoDeviceFoundAdded(false);
        removeAllDevices();
        addCachedDevices();
        this.mSelectedDevice = null;
        if (this.mScanAllowed) {
            enableScanning();
            this.mAvailableDevicesCategory.setProgress(this.mLocalAdapter.isDiscovering());
        }
    }

    @Override
    public void onStop() {
        disableScanning();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mSelectedDevice == null) {
            sendDevicePickedIntent(null);
        }
        if (this.mProgressCategory != null) {
            this.mProgressCategory.removeAll();
        }
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference bluetoothDevicePreference) {
        disableScanning();
        LocalBluetoothPreferences.persistSelectedDeviceInPicker(getActivity(), this.mSelectedDevice.getAddress());
        if (bluetoothDevicePreference.getCachedDevice().getBondState() == 12 || !this.mNeedAuth) {
            sendDevicePickedIntent(this.mSelectedDevice);
            finish();
        } else {
            super.onDevicePreferenceClick(bluetoothDevicePreference);
        }
    }

    @Override
    public void onScanningStateChanged(boolean z) {
        super.onScanningStateChanged(z);
        this.mAvailableDevicesCategory.setProgress(z | this.mScanEnabled);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        if (!device.equals(this.mSelectedDevice)) {
            return;
        }
        if (i == 12) {
            sendDevicePickedIntent(device);
            finish();
        } else if (i == 10) {
            enableScanning();
        }
    }

    @Override
    public void onBluetoothStateChanged(int i) {
        super.onBluetoothStateChanged(i);
        if (i == 12) {
            enableScanning();
        }
    }

    @Override
    protected String getLogTag() {
        return "DevicePickerFragment";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public String getDeviceListKey() {
        return "bt_device_list";
    }

    private void sendDevicePickedIntent(BluetoothDevice bluetoothDevice) {
        Intent intent = new Intent("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        if (this.mLaunchPackage != null && this.mLaunchClass != null) {
            intent.setClassName(this.mLaunchPackage, this.mLaunchClass);
        }
        getActivity().sendBroadcast(intent);
    }
}
