package com.android.settings.connecteddevice;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class PreviouslyConnectedDevicePreferenceController extends BasePreferenceController implements DevicePreferenceCallback, LifecycleObserver, OnStart, OnStop {
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Preference mPreference;
    private int mPreferenceSize;

    public PreviouslyConnectedDevicePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            return 0;
        }
        return 1;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
            this.mBluetoothDeviceUpdater.setPrefContext(preferenceScreen.getContext());
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mBluetoothDeviceUpdater.registerCallback();
        updatePreferenceOnSizeChanged();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mBluetoothDeviceUpdater.unregisterCallback();
    }

    public void init(DashboardFragment dashboardFragment) {
        this.mBluetoothDeviceUpdater = new SavedBluetoothDeviceUpdater(dashboardFragment.getContext(), dashboardFragment, this);
    }

    @Override // com.android.settings.connecteddevice.DevicePreferenceCallback
    public void onDeviceAdded(Preference preference) {
        this.mPreferenceSize++;
        updatePreferenceOnSizeChanged();
    }

    @Override // com.android.settings.connecteddevice.DevicePreferenceCallback
    public void onDeviceRemoved(Preference preference) {
        this.mPreferenceSize--;
        updatePreferenceOnSizeChanged();
    }

    void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        this.mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    void setPreferenceSize(int i) {
        this.mPreferenceSize = i;
    }

    void setPreference(Preference preference) {
        this.mPreference = preference;
    }

    private void updatePreferenceOnSizeChanged() {
        if (isAvailable()) {
            this.mPreference.setEnabled(this.mPreferenceSize != 0);
        }
    }
}
