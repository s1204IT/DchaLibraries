package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class BluetoothDeviceUpdater implements BluetoothCallback, LocalBluetoothProfileManager.ServiceListener {
    protected final DevicePreferenceCallback mDevicePreferenceCallback;
    final GearPreference.OnGearClickListener mDeviceProfilesListener;
    protected DashboardFragment mFragment;
    protected final LocalBluetoothManager mLocalManager;
    protected Context mPrefContext;
    protected final Map<BluetoothDevice, Preference> mPreferenceMap;
    private final boolean mShowDeviceWithoutNames;

    public abstract boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice);

    public BluetoothDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback) {
        this(dashboardFragment, devicePreferenceCallback, Utils.getLocalBtManager(context));
    }

    BluetoothDeviceUpdater(DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localBluetoothManager) {
        this.mDeviceProfilesListener = new GearPreference.OnGearClickListener() {
            @Override
            public final void onGearClick(GearPreference gearPreference) {
                this.f$0.launchDeviceDetails(gearPreference);
            }
        };
        this.mFragment = dashboardFragment;
        this.mDevicePreferenceCallback = devicePreferenceCallback;
        this.mShowDeviceWithoutNames = SystemProperties.getBoolean("persist.bluetooth.showdeviceswithoutnames", false);
        this.mPreferenceMap = new HashMap();
        this.mLocalManager = localBluetoothManager;
    }

    public void registerCallback() {
        this.mLocalManager.setForegroundActivity(this.mFragment.getContext());
        this.mLocalManager.getEventManager().registerCallback(this);
        this.mLocalManager.getProfileManager().addServiceListener(this);
        forceUpdate();
    }

    public void unregisterCallback() {
        this.mLocalManager.setForegroundActivity(null);
        this.mLocalManager.getEventManager().unregisterCallback(this);
        this.mLocalManager.getProfileManager().removeServiceListener(this);
    }

    public void forceUpdate() {
        Iterator<CachedBluetoothDevice> it = this.mLocalManager.getCachedDeviceManager().getCachedDevicesCopy().iterator();
        while (it.hasNext()) {
            update(it.next());
        }
    }

    @Override
    public void onBluetoothStateChanged(int i) {
        forceUpdate();
    }

    @Override
    public void onScanningStateChanged(boolean z) {
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        update(cachedBluetoothDevice);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
        removePreference(cachedBluetoothDevice);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        update(cachedBluetoothDevice);
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

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
    }

    @Override
    public void onServiceConnected() {
        forceUpdate();
    }

    @Override
    public void onServiceDisconnected() {
    }

    public void setPrefContext(Context context) {
        this.mPrefContext = context;
    }

    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        if (isFilterMatched(cachedBluetoothDevice)) {
            addPreference(cachedBluetoothDevice);
        } else {
            removePreference(cachedBluetoothDevice);
        }
    }

    protected void addPreference(CachedBluetoothDevice cachedBluetoothDevice) {
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        if (!this.mPreferenceMap.containsKey(device)) {
            BluetoothDevicePreference bluetoothDevicePreference = new BluetoothDevicePreference(this.mPrefContext, cachedBluetoothDevice, this.mShowDeviceWithoutNames);
            bluetoothDevicePreference.setOnGearClickListener(this.mDeviceProfilesListener);
            if (this instanceof Preference.OnPreferenceClickListener) {
                bluetoothDevicePreference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener) this);
            }
            this.mPreferenceMap.put(device, bluetoothDevicePreference);
            this.mDevicePreferenceCallback.onDeviceAdded(bluetoothDevicePreference);
        }
    }

    protected void removePreference(CachedBluetoothDevice cachedBluetoothDevice) {
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        if (this.mPreferenceMap.containsKey(device)) {
            this.mDevicePreferenceCallback.onDeviceRemoved(this.mPreferenceMap.get(device));
            this.mPreferenceMap.remove(device);
        }
    }

    public void launchDeviceDetails(Preference preference) {
        CachedBluetoothDevice bluetoothDevice = ((BluetoothDevicePreference) preference).getBluetoothDevice();
        if (bluetoothDevice == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("device_address", bluetoothDevice.getDevice().getAddress());
        new SubSettingLauncher(this.mFragment.getContext()).setDestination(BluetoothDeviceDetailsFragment.class.getName()).setArguments(bundle).setTitle(R.string.device_details_title).setSourceMetricsCategory(this.mFragment.getMetricsCategory()).launch();
    }

    public boolean isDeviceConnected(CachedBluetoothDevice cachedBluetoothDevice) {
        if (cachedBluetoothDevice == null) {
            return false;
        }
        BluetoothDevice device = cachedBluetoothDevice.getDevice();
        return device.getBondState() == 12 && device.isConnected();
    }
}
