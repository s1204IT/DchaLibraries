package com.android.settings.bluetooth;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

/* loaded from: classes.dex */
public final class DevicePickerFragment extends DeviceListPreferenceFragment {
    BluetoothProgressCategory mAvailableDevicesCategory;
    String mCallingAppPackageName;
    Context mContext;
    String mLaunchClass;
    String mLaunchPackage;
    private boolean mNeedAuth;
    private ProgressCategory mProgressCategory;
    private boolean mScanAllowed;

    public DevicePickerFragment() {
        super(null);
    }

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment
    void initPreferencesFromPreferenceScreen() {
        Intent intent = getActivity().getIntent();
        this.mNeedAuth = intent.getBooleanExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
        setFilter(intent.getIntExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0));
        this.mLaunchPackage = intent.getStringExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE");
        this.mLaunchClass = intent.getStringExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS");
        this.mAvailableDevicesCategory = (BluetoothProgressCategory) findPreference("bt_device_list");
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 25;
    }

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment, com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(getString(R.string.device_picker));
        this.mScanAllowed = !((UserManager) getSystemService("user")).hasUserRestriction("no_config_bluetooth");
        this.mCallingAppPackageName = getCallingAppPackageName(getActivity().getActivityToken());
        if (!TextUtils.equals(this.mCallingAppPackageName, this.mLaunchPackage)) {
            Log.w("DevicePickerFragment", "sendDevicePickedIntent() launch package name is not equivalent to calling package name!");
        }
        this.mContext = getContext();
        setHasOptionsMenu(true);
        this.mProgressCategory = (ProgressCategory) findPreference("bt_device_list");
    }

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment, com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
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

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment, com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        disableScanning();
        super.onStop();
    }

    @Override // com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (this.mSelectedDevice == null) {
            sendDevicePickedIntent(null);
        }
        if (this.mProgressCategory != null) {
            this.mProgressCategory.removeAll();
        }
    }

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment
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

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment, com.android.settingslib.bluetooth.BluetoothCallback
    public void onScanningStateChanged(boolean z) {
        super.onScanningStateChanged(z);
        this.mAvailableDevicesCategory.setProgress(z | this.mScanEnabled);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
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

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment, com.android.settingslib.bluetooth.BluetoothCallback
    public void onBluetoothStateChanged(int i) {
        super.onBluetoothStateChanged(i);
        if (i == 12) {
            enableScanning();
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "DevicePickerFragment";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.device_picker;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override // com.android.settings.bluetooth.DeviceListPreferenceFragment
    public String getDeviceListKey() {
        return "bt_device_list";
    }

    private void sendDevicePickedIntent(BluetoothDevice bluetoothDevice) {
        Intent intent = new Intent("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        if (this.mLaunchPackage != null && this.mLaunchClass != null && TextUtils.equals(this.mCallingAppPackageName, this.mLaunchPackage)) {
            intent.setClassName(this.mLaunchPackage, this.mLaunchClass);
        }
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
    }

    private String getCallingAppPackageName(IBinder iBinder) {
        try {
            return ActivityManager.getService().getLaunchedFromPackage(iBinder);
        } catch (RemoteException e) {
            Log.v("DevicePickerFragment", "Could not talk to activity manager.", e);
            return null;
        }
    }
}
