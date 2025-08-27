package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.usb.UsbConnectionBroadcastReceiver;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;

/* loaded from: classes.dex */
public class ConnectedUsbDeviceUpdater {
    private DevicePreferenceCallback mDevicePreferenceCallback;
    private DashboardFragment mFragment;
    private UsbBackend mUsbBackend;
    UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener;
    Preference mUsbPreference;
    UsbConnectionBroadcastReceiver mUsbReceiver;

    public static /* synthetic */ void lambda$new$0(ConnectedUsbDeviceUpdater connectedUsbDeviceUpdater, boolean z, long j, int i, int i2) {
        if (z) {
            connectedUsbDeviceUpdater.mUsbPreference.setSummary(getSummary(connectedUsbDeviceUpdater.mUsbBackend.getCurrentFunctions(), connectedUsbDeviceUpdater.mUsbBackend.getPowerRole()));
            connectedUsbDeviceUpdater.mDevicePreferenceCallback.onDeviceAdded(connectedUsbDeviceUpdater.mUsbPreference);
        } else {
            connectedUsbDeviceUpdater.mDevicePreferenceCallback.onDeviceRemoved(connectedUsbDeviceUpdater.mUsbPreference);
        }
    }

    public ConnectedUsbDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback) {
        this(context, dashboardFragment, devicePreferenceCallback, new UsbBackend(context));
    }

    ConnectedUsbDeviceUpdater(Context context, DashboardFragment dashboardFragment, DevicePreferenceCallback devicePreferenceCallback, UsbBackend usbBackend) {
        this.mUsbConnectionListener = new UsbConnectionBroadcastReceiver.UsbConnectionListener() { // from class: com.android.settings.connecteddevice.usb.-$$Lambda$ConnectedUsbDeviceUpdater$8_8ZhYJMgn-zGVqi-7esENaXwOM
            @Override // com.android.settings.connecteddevice.usb.UsbConnectionBroadcastReceiver.UsbConnectionListener
            public final void onUsbConnectionChanged(boolean z, long j, int i, int i2) {
                ConnectedUsbDeviceUpdater.lambda$new$0(this.f$0, z, j, i, i2);
            }
        };
        this.mFragment = dashboardFragment;
        this.mDevicePreferenceCallback = devicePreferenceCallback;
        this.mUsbBackend = usbBackend;
        this.mUsbReceiver = new UsbConnectionBroadcastReceiver(context, this.mUsbConnectionListener, this.mUsbBackend);
    }

    public void registerCallback() {
        this.mUsbReceiver.register();
    }

    public void unregisterCallback() {
        this.mUsbReceiver.unregister();
    }

    public void initUsbPreference(Context context) {
        this.mUsbPreference = new Preference(context, null);
        this.mUsbPreference.setTitle(R.string.usb_pref);
        this.mUsbPreference.setIcon(R.drawable.ic_usb);
        this.mUsbPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.connecteddevice.usb.-$$Lambda$ConnectedUsbDeviceUpdater$qas_74KUD2s0js4DMK034hpC0Q4
            @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference) {
                return ConnectedUsbDeviceUpdater.lambda$initUsbPreference$1(this.f$0, preference);
            }
        });
        forceUpdate();
    }

    public static /* synthetic */ boolean lambda$initUsbPreference$1(ConnectedUsbDeviceUpdater connectedUsbDeviceUpdater, Preference preference) {
        new SubSettingLauncher(connectedUsbDeviceUpdater.mFragment.getContext()).setDestination(UsbDetailsFragment.class.getName()).setTitle(R.string.device_details_title).setSourceMetricsCategory(connectedUsbDeviceUpdater.mFragment.getMetricsCategory()).launch();
        return true;
    }

    private void forceUpdate() {
        this.mUsbReceiver.register();
    }

    public static int getSummary(long j, int i) {
        switch (i) {
            case 1:
                if (j != 4) {
                    if (j != 32) {
                        if (j != 16) {
                            if (j == 8) {
                            }
                        }
                    }
                }
                break;
            case 2:
                if (j != 4) {
                    if (j != 32) {
                        if (j != 16) {
                            if (j == 8) {
                            }
                        }
                    }
                }
                break;
        }
        return R.string.usb_summary_charging_only;
    }
}
