package com.android.settings.deviceinfo;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.SpannedString;
import com.android.settings.bluetooth.BluetoothLengthDeviceNameFilter;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

/* loaded from: classes.dex */
public class DeviceNamePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener, ValidatedEditTextPreference.Validator, LifecycleObserver, OnCreate, OnSaveInstanceState {
    public static final int DEVICE_NAME_SET_WARNING_ID = 1;
    private static final String KEY_PENDING_DEVICE_NAME = "key_pending_device_name";
    private static final String PREF_KEY = "device_name";
    private LocalBluetoothManager mBluetoothManager;
    private String mDeviceName;
    private DeviceNamePreferenceHost mHost;
    private String mPendingDeviceName;
    private ValidatedEditTextPreference mPreference;
    private final WifiDeviceNameTextValidator mWifiDeviceNameTextValidator;
    protected WifiManager mWifiManager;

    public interface DeviceNamePreferenceHost {
        void showDeviceNameWarningDialog(String str);
    }

    public DeviceNamePreferenceController(Context context) {
        super(context, PREF_KEY);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();
        initializeDeviceName();
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ValidatedEditTextPreference) preferenceScreen.findPreference(PREF_KEY);
        CharSequence summary = getSummary();
        this.mPreference.setSummary(summary);
        this.mPreference.setText(summary.toString());
        this.mPreference.setValidator(this);
    }

    private void initializeDeviceName() {
        this.mDeviceName = Settings.Global.getString(this.mContext.getContentResolver(), PREF_KEY);
        if (this.mDeviceName == null) {
            this.mDeviceName = Build.MODEL;
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return this.mDeviceName;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mPendingDeviceName = (String) obj;
        if (this.mHost != null) {
            this.mHost.showDeviceNameWarningDialog(this.mPendingDeviceName);
            return true;
        }
        return true;
    }

    @Override // com.android.settings.widget.ValidatedEditTextPreference.Validator
    public boolean isTextValid(String str) {
        return this.mWifiDeviceNameTextValidator.isTextValid(str);
    }

    public void setLocalBluetoothManager(LocalBluetoothManager localBluetoothManager) {
        this.mBluetoothManager = localBluetoothManager;
    }

    public void confirmDeviceName() {
        if (this.mPendingDeviceName != null) {
            setDeviceName(this.mPendingDeviceName);
        }
    }

    public void setHost(DeviceNamePreferenceHost deviceNamePreferenceHost) {
        this.mHost = deviceNamePreferenceHost;
    }

    private void setDeviceName(String str) {
        this.mDeviceName = str;
        setSettingsGlobalDeviceName(str);
        setBluetoothDeviceName(str);
        setTetherSsidName(str);
        this.mPreference.setSummary(getSummary());
    }

    private void setSettingsGlobalDeviceName(String str) {
        Settings.Global.putString(this.mContext.getContentResolver(), PREF_KEY, str);
    }

    private void setBluetoothDeviceName(String str) {
        LocalBluetoothAdapter bluetoothAdapter;
        if (this.mBluetoothManager != null && (bluetoothAdapter = this.mBluetoothManager.getBluetoothAdapter()) != null) {
            bluetoothAdapter.setName(getFilteredBluetoothString(str));
        }
    }

    private static final String getFilteredBluetoothString(String str) {
        CharSequence charSequenceFilter = new BluetoothLengthDeviceNameFilter().filter(str, 0, str.length(), new SpannedString(""), 0, 0);
        if (charSequenceFilter == null) {
            return str;
        }
        return charSequenceFilter.toString();
    }

    private void setTetherSsidName(String str) {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        wifiApConfiguration.SSID = str;
        this.mWifiManager.setWifiApConfiguration(wifiApConfiguration);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreate
    public void onCreate(Bundle bundle) {
        if (bundle != null) {
            this.mPendingDeviceName = bundle.getString(KEY_PENDING_DEVICE_NAME, null);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(KEY_PENDING_DEVICE_NAME, this.mPendingDeviceName);
    }
}
