package com.android.settings.connecteddevice;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/* loaded from: classes.dex */
public class ConnectedDeviceGroupController extends BasePreferenceController implements DevicePreferenceCallback, PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final String KEY = "connected_device_list";
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DockUpdater mConnectedDockUpdater;
    private ConnectedUsbDeviceUpdater mConnectedUsbDeviceUpdater;
    PreferenceGroup mPreferenceGroup;

    public ConnectedDeviceGroupController(Context context) {
        super(context, KEY);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mBluetoothDeviceUpdater.registerCallback();
        this.mConnectedUsbDeviceUpdater.registerCallback();
        this.mConnectedDockUpdater.registerCallback();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mConnectedUsbDeviceUpdater.unregisterCallback();
        this.mBluetoothDeviceUpdater.unregisterCallback();
        this.mConnectedDockUpdater.unregisterCallback();
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreferenceGroup = (PreferenceGroup) preferenceScreen.findPreference(KEY);
            this.mPreferenceGroup.setVisible(false);
            this.mBluetoothDeviceUpdater.setPrefContext(preferenceScreen.getContext());
            this.mBluetoothDeviceUpdater.forceUpdate();
            this.mConnectedUsbDeviceUpdater.initUsbPreference(preferenceScreen.getContext());
            this.mConnectedDockUpdater.forceUpdate();
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            return 0;
        }
        return 2;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY;
    }

    @Override // com.android.settings.connecteddevice.DevicePreferenceCallback
    public void onDeviceAdded(Preference preference) {
        if (this.mPreferenceGroup.getPreferenceCount() == 0) {
            this.mPreferenceGroup.setVisible(true);
        }
        this.mPreferenceGroup.addPreference(preference);
    }

    @Override // com.android.settings.connecteddevice.DevicePreferenceCallback
    public void onDeviceRemoved(Preference preference) {
        this.mPreferenceGroup.removePreference(preference);
        if (this.mPreferenceGroup.getPreferenceCount() == 0) {
            this.mPreferenceGroup.setVisible(false);
        }
    }

    public void init(BluetoothDeviceUpdater bluetoothDeviceUpdater, ConnectedUsbDeviceUpdater connectedUsbDeviceUpdater, DockUpdater dockUpdater) {
        this.mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
        this.mConnectedUsbDeviceUpdater = connectedUsbDeviceUpdater;
        this.mConnectedDockUpdater = dockUpdater;
    }

    public void init(DashboardFragment dashboardFragment) {
        Context context = dashboardFragment.getContext();
        init(new ConnectedBluetoothDeviceUpdater(context, dashboardFragment, this), new ConnectedUsbDeviceUpdater(context, dashboardFragment, this), FeatureFactory.getFactory(context).getDockUpdaterFeatureProvider().getConnectedDockUpdater(context, this));
    }
}
