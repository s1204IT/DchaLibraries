package com.android.systemui.statusbar.car;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.BluetoothController;

/* loaded from: classes.dex */
public class ConnectedDeviceSignalController extends BroadcastReceiver implements BluetoothController.Callback {
    private static final int[] SIGNAL_STRENGTH_ICONS = {0, 0, 1, 2, 3, 4};
    private BluetoothHeadsetClient mBluetoothHeadsetClient;
    private final ImageView mNetworkSignalView;
    private final SignalDrawable mSignalDrawable;
    private final View mSignalsView;

    @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
    public void onBluetoothDevicesChanged() {
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
    public void onBluetoothStateChange(boolean z) {
        if (StatusBar.DEBUG) {
            Log.d("DeviceSignalCtlr", "onBluetoothStateChange(). enabled: " + z);
        }
        if (!z) {
            this.mNetworkSignalView.setVisibility(8);
            this.mSignalsView.setVisibility(8);
        }
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (StatusBar.DEBUG) {
            Log.d("DeviceSignalCtlr", "onReceive(). action: " + action);
        }
        if ("android.bluetooth.headsetclient.profile.action.AG_EVENT".equals(action)) {
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "Received ACTION_AG_EVENT");
            }
            processActionAgEvent(intent);
        } else if ("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "ACTION_CONNECTION_STATE_CHANGED event: " + intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1) + " -> " + intExtra);
            }
            updateViewVisibility((BluetoothDevice) intent.getExtra("android.bluetooth.device.extra.DEVICE"), intExtra);
        }
    }

    private void processActionAgEvent(Intent intent) {
        int intExtra = intent.getIntExtra("android.bluetooth.headsetclient.extra.NETWORK_STATUS", -1);
        if (intExtra != -1) {
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "EXTRA_NETWORK_STATUS:  " + intExtra);
            }
            if (intExtra == 0) {
                setNetworkSignalIcon(R.drawable.stat_sys_signal_null);
            }
        }
        int intExtra2 = intent.getIntExtra("android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH", -1);
        if (intExtra2 != -1) {
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "EXTRA_NETWORK_SIGNAL_STRENGTH: " + intExtra2);
            }
            setNetworkSignalIcon(SIGNAL_STRENGTH_ICONS[intExtra2]);
        }
        int intExtra3 = intent.getIntExtra("android.bluetooth.headsetclient.extra.NETWORK_ROAMING", -1);
        if (intExtra3 != -1 && StatusBar.DEBUG) {
            Log.d("DeviceSignalCtlr", "EXTRA_NETWORK_ROAMING: " + intExtra3);
        }
    }

    private void setNetworkSignalIcon(int i) {
        this.mSignalsView.setVisibility(0);
        this.mSignalDrawable.setLevel(SignalDrawable.getState(i, 5, false));
        this.mNetworkSignalView.setVisibility(0);
    }

    private void updateViewVisibility(BluetoothDevice bluetoothDevice, int i) {
        Bundle currentAgEvents;
        int i2;
        if (i == 2) {
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "Device connected");
            }
            if (this.mBluetoothHeadsetClient != null && bluetoothDevice != null && (currentAgEvents = this.mBluetoothHeadsetClient.getCurrentAgEvents(bluetoothDevice)) != null && (i2 = currentAgEvents.getInt("android.bluetooth.headsetclient.extra.NETWORK_SIGNAL_STRENGTH", -1)) != -1) {
                if (StatusBar.DEBUG) {
                    Log.d("DeviceSignalCtlr", "EXTRA_NETWORK_SIGNAL_STRENGTH: " + i2);
                }
                setNetworkSignalIcon(SIGNAL_STRENGTH_ICONS[i2]);
                return;
            }
            return;
        }
        if (i == 0) {
            if (StatusBar.DEBUG) {
                Log.d("DeviceSignalCtlr", "Device disconnected");
            }
            this.mNetworkSignalView.setVisibility(8);
            this.mSignalsView.setVisibility(8);
        }
    }
}
