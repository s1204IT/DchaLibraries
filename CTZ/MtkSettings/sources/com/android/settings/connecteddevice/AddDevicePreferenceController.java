package com.android.settings.connecteddevice;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class AddDevicePreferenceController extends BasePreferenceController implements LifecycleObserver, OnStart, OnStop {
    private BluetoothAdapter mBluetoothAdapter;
    private IntentFilter mIntentFilter;
    private Preference mPreference;
    private final BroadcastReceiver mReceiver;

    public AddDevicePreferenceController(Context context, String str) {
        super(context, str);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                AddDevicePreferenceController.this.updateState();
            }
        };
        this.mIntentFilter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onStart() {
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    @Override
    public void onStop() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
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
    public CharSequence getSummary() {
        if (this.mBluetoothAdapter != null && this.mBluetoothAdapter.isEnabled()) {
            return "";
        }
        return this.mContext.getString(R.string.connected_device_add_device_summary);
    }

    void updateState() {
        updateState(this.mPreference);
    }
}
