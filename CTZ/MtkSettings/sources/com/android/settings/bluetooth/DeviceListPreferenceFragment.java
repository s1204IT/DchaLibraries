package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.BidiFormatter;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import java.util.Iterator;
import java.util.WeakHashMap;

public abstract class DeviceListPreferenceFragment extends RestrictedDashboardFragment implements BluetoothCallback {
    PreferenceGroup mDeviceListGroup;
    final WeakHashMap<CachedBluetoothDevice, BluetoothDevicePreference> mDevicePreferenceMap;
    private BluetoothDeviceFilter.Filter mFilter;
    LocalBluetoothAdapter mLocalAdapter;
    LocalBluetoothManager mLocalManager;
    boolean mScanEnabled;
    BluetoothDevice mSelectedDevice;
    boolean mShowDevicesWithoutNames;

    public abstract String getDeviceListKey();

    abstract void initPreferencesFromPreferenceScreen();

    DeviceListPreferenceFragment(String str) {
        super(str);
        this.mDevicePreferenceMap = new WeakHashMap<>();
        this.mFilter = BluetoothDeviceFilter.ALL_FILTER;
    }

    final void setFilter(BluetoothDeviceFilter.Filter filter) {
        this.mFilter = filter;
    }

    final void setFilter(int i) {
        this.mFilter = BluetoothDeviceFilter.getFilter(i);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mLocalManager = Utils.getLocalBtManager(getActivity());
        if (this.mLocalManager == null) {
            Log.e("DeviceListPreferenceFragment", "Bluetooth is not supported on this device");
            return;
        }
        this.mLocalAdapter = this.mLocalManager.getBluetoothAdapter();
        this.mShowDevicesWithoutNames = SystemProperties.getBoolean("persist.bluetooth.showdeviceswithoutnames", false);
        initPreferencesFromPreferenceScreen();
        this.mDeviceListGroup = (PreferenceCategory) findPreference(getDeviceListKey());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mLocalManager == null || isUiRestricted()) {
            return;
        }
        this.mLocalManager.setForegroundActivity(getActivity());
        this.mLocalManager.getEventManager().registerCallback(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mLocalManager == null || isUiRestricted()) {
            return;
        }
        removeAllDevices();
        this.mLocalManager.setForegroundActivity(null);
        this.mLocalManager.getEventManager().unregisterCallback(this);
    }

    void removeAllDevices() {
        this.mDevicePreferenceMap.clear();
        this.mDeviceListGroup.removeAll();
    }

    void addCachedDevices() {
        Iterator<CachedBluetoothDevice> it = this.mLocalManager.getCachedDeviceManager().getCachedDevicesCopy().iterator();
        while (it.hasNext()) {
            onDeviceAdded(it.next());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if ("bt_scan".equals(preference.getKey())) {
            this.mLocalAdapter.startScanning(true);
            return true;
        }
        if (preference instanceof BluetoothDevicePreference) {
            BluetoothDevicePreference bluetoothDevicePreference = (BluetoothDevicePreference) preference;
            this.mSelectedDevice = bluetoothDevicePreference.getCachedDevice().getDevice();
            onDevicePreferenceClick(bluetoothDevicePreference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    void onDevicePreferenceClick(BluetoothDevicePreference bluetoothDevicePreference) {
        bluetoothDevicePreference.onClicked();
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        Log.d("DeviceListPreferenceFragment", "onDeviceAdded, Device name is " + cachedBluetoothDevice.getName());
        if (this.mDevicePreferenceMap.get(cachedBluetoothDevice) != null) {
            Log.d("DeviceListPreferenceFragment", "Device name " + cachedBluetoothDevice.getName() + " already have preference");
            return;
        }
        if (this.mLocalAdapter.getBluetoothState() == 12 && this.mFilter.matches(cachedBluetoothDevice.getDevice())) {
            Log.d("DeviceListPreferenceFragment", "Device name " + cachedBluetoothDevice.getName() + " create new preference");
            createDevicePreference(cachedBluetoothDevice);
        }
    }

    void createDevicePreference(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mDeviceListGroup == null) {
            Log.w("DeviceListPreferenceFragment", "Trying to create a device preference before the list group/category exists!");
            return;
        }
        String address = cachedBluetoothDevice.getDevice().getAddress();
        BluetoothDevicePreference bluetoothDevicePreference = (BluetoothDevicePreference) getCachedPreference(address);
        if (bluetoothDevicePreference == null) {
            bluetoothDevicePreference = new BluetoothDevicePreference(getPrefContext(), cachedBluetoothDevice, this.mShowDevicesWithoutNames);
            bluetoothDevicePreference.setKey(address);
            this.mDeviceListGroup.addPreference(bluetoothDevicePreference);
        } else {
            bluetoothDevicePreference.rebind();
        }
        initDevicePreference(bluetoothDevicePreference);
        this.mDevicePreferenceMap.put(cachedBluetoothDevice, bluetoothDevicePreference);
    }

    void initDevicePreference(BluetoothDevicePreference bluetoothDevicePreference) {
    }

    void updateFooterPreference(Preference preference) {
        preference.setTitle(getString(R.string.bluetooth_footer_mac_message, new Object[]{BidiFormatter.getInstance().unicodeWrap(this.mLocalAdapter.getAddress())}));
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
        BluetoothDevicePreference bluetoothDevicePreferenceRemove = this.mDevicePreferenceMap.remove(cachedBluetoothDevice);
        if (bluetoothDevicePreferenceRemove != null) {
            this.mDeviceListGroup.removePreference(bluetoothDevicePreferenceRemove);
        }
    }

    void enableScanning() {
        this.mLocalAdapter.startScanning(true);
        this.mScanEnabled = true;
    }

    void disableScanning() {
        this.mLocalAdapter.stopScanning();
        this.mScanEnabled = false;
    }

    public void onScanningStateChanged(boolean z) {
        Log.d("DeviceListPreferenceFragment", "onScanningStateChanged " + z);
        if (!z && this.mScanEnabled) {
            this.mLocalAdapter.startScanning(true);
        }
    }

    public void onBluetoothStateChanged(int i) {
    }

    public void addDeviceCategory(PreferenceGroup preferenceGroup, int i, BluetoothDeviceFilter.Filter filter, boolean z) {
        cacheRemoveAllPrefs(preferenceGroup);
        preferenceGroup.setTitle(i);
        this.mDeviceListGroup = preferenceGroup;
        setFilter(filter);
        if (z) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
        removeCachedPrefs(preferenceGroup);
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onAudioModeChanged() {
    }
}
