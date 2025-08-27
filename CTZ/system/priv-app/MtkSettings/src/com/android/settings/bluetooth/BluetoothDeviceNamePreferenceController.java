package com.android.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class BluetoothDeviceNamePreferenceController extends BasePreferenceController implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "BluetoothNamePrefCtrl";
    protected LocalBluetoothAdapter mLocalAdapter;
    private LocalBluetoothManager mLocalManager;
    Preference mPreference;
    final BroadcastReceiver mReceiver;

    public BluetoothDeviceNamePreferenceController(Context context, String str) {
        super(context, str);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.settings.bluetooth.BluetoothDeviceNamePreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (TextUtils.equals(action, "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED")) {
                    if (BluetoothDeviceNamePreferenceController.this.mPreference != null && BluetoothDeviceNamePreferenceController.this.mLocalAdapter != null && BluetoothDeviceNamePreferenceController.this.mLocalAdapter.isEnabled()) {
                        BluetoothDeviceNamePreferenceController.this.updatePreferenceState(BluetoothDeviceNamePreferenceController.this.mPreference);
                        return;
                    }
                    return;
                }
                if (TextUtils.equals(action, "android.bluetooth.adapter.action.STATE_CHANGED")) {
                    BluetoothDeviceNamePreferenceController.this.updatePreferenceState(BluetoothDeviceNamePreferenceController.this.mPreference);
                }
            }
        };
        this.mLocalManager = Utils.getLocalBtManager(context);
        if (this.mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
        } else {
            this.mLocalAdapter = this.mLocalManager.getBluetoothAdapter();
        }
    }

    BluetoothDeviceNamePreferenceController(Context context, LocalBluetoothAdapter localBluetoothAdapter, String str) {
        super(context, str);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.settings.bluetooth.BluetoothDeviceNamePreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (TextUtils.equals(action, "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED")) {
                    if (BluetoothDeviceNamePreferenceController.this.mPreference != null && BluetoothDeviceNamePreferenceController.this.mLocalAdapter != null && BluetoothDeviceNamePreferenceController.this.mLocalAdapter.isEnabled()) {
                        BluetoothDeviceNamePreferenceController.this.updatePreferenceState(BluetoothDeviceNamePreferenceController.this.mPreference);
                        return;
                    }
                    return;
                }
                if (TextUtils.equals(action, "android.bluetooth.adapter.action.STATE_CHANGED")) {
                    BluetoothDeviceNamePreferenceController.this.updatePreferenceState(BluetoothDeviceNamePreferenceController.this.mPreference);
                }
            }
        };
        this.mLocalAdapter = localBluetoothAdapter;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        super.displayPreference(preferenceScreen);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return this.mLocalAdapter != null ? 0 : 2;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        updatePreferenceState(preference);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        String deviceName = getDeviceName();
        if (TextUtils.isEmpty(deviceName)) {
            return super.getSummary();
        }
        return TextUtils.expandTemplate(this.mContext.getText(R.string.bluetooth_device_name_summary), BidiFormatter.getInstance().unicodeWrap(deviceName)).toString();
    }

    public Preference createBluetoothDeviceNamePreference(PreferenceScreen preferenceScreen, int i) {
        this.mPreference = new Preference(preferenceScreen.getContext());
        this.mPreference.setOrder(i);
        this.mPreference.setKey(getPreferenceKey());
        preferenceScreen.addPreference(this.mPreference);
        return this.mPreference;
    }

    protected void updatePreferenceState(Preference preference) {
        preference.setSelectable(false);
        preference.setSummary(getSummary());
    }

    protected String getDeviceName() {
        return this.mLocalAdapter.getName();
    }
}
