package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public final class DevicePickerFragment extends DeviceListPreferenceFragment {
    private String mLaunchClass;
    private String mLaunchPackage;
    private boolean mNeedAuth;
    private ProgressCategory mProgressCategory;
    private boolean mStartScanOnResume;

    public DevicePickerFragment() {
        super(null);
    }

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.device_picker);
        Intent intent = getActivity().getIntent();
        this.mNeedAuth = intent.getBooleanExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
        setFilter(intent.getIntExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0));
        this.mLaunchPackage = intent.getStringExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE");
        this.mLaunchClass = intent.getStringExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS");
    }

    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        preference.setWidgetLayoutResource(R.layout.preference_empty_list);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.bluetooth_search_for_devices).setEnabled(true).setShowAsAction(0);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                this.mLocalAdapter.startScanning(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 25;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getString(R.string.device_picker));
        UserManager um = (UserManager) getSystemService("user");
        if (!um.hasUserRestriction("no_config_bluetooth") && savedInstanceState == null) {
            z = true;
        }
        this.mStartScanOnResume = z;
        setHasOptionsMenu(true);
        this.mProgressCategory = (ProgressCategory) findPreference("bt_device_list");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mProgressCategory == null) {
            return;
        }
        this.mProgressCategory.removeAll();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mProgressCategory.setNoDeviceFoundAdded(false);
        removeAllDevices();
        addCachedDevices();
        if (!this.mStartScanOnResume) {
            return;
        }
        this.mLocalAdapter.startScanning(true);
        this.mStartScanOnResume = false;
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        this.mLocalAdapter.stopScanning();
        LocalBluetoothPreferences.persistSelectedDeviceInPicker(getActivity(), this.mSelectedDevice.getAddress());
        if (btPreference.getCachedDevice().getBondState() == 12 || !this.mNeedAuth) {
            sendDevicePickedIntent(this.mSelectedDevice);
            finish();
        } else {
            super.onDevicePreferenceClick(btPreference);
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        if (bondState != 12) {
            return;
        }
        BluetoothDevice device = cachedDevice.getDevice();
        if (!device.equals(this.mSelectedDevice)) {
            return;
        }
        sendDevicePickedIntent(device);
        finish();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        if (bluetoothState != 12) {
            return;
        }
        this.mLocalAdapter.startScanning(false);
    }

    private void sendDevicePickedIntent(BluetoothDevice device) {
        Intent intent = new Intent("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", device);
        if (this.mLaunchPackage != null && this.mLaunchClass != null) {
            intent.setClassName(this.mLaunchPackage, this.mLaunchClass);
        }
        getActivity().sendBroadcast(intent);
    }
}
