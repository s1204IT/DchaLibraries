package com.android.settings.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.android.settingslib.wifi.AccessPoint;

public class BluetoothPairingDialog extends Activity {
    private BluetoothPairingController mBluetoothPairingController;
    private boolean mReceiverRegistered = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
                int intExtra = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", AccessPoint.UNREACHABLE_RSSI);
                if (intExtra == 12 || intExtra == 10) {
                    BluetoothPairingDialog.this.dismiss();
                    return;
                }
                return;
            }
            if ("android.bluetooth.device.action.PAIRING_CANCEL".equals(action)) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (bluetoothDevice == null || BluetoothPairingDialog.this.mBluetoothPairingController.deviceEquals(bluetoothDevice)) {
                    BluetoothPairingDialog.this.dismiss();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        boolean z;
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        this.mBluetoothPairingController = new BluetoothPairingController(getIntent(), this);
        BluetoothPairingDialogFragment bluetoothPairingDialogFragment = (BluetoothPairingDialogFragment) getFragmentManager().findFragmentByTag("bluetooth.pairing.fragment");
        if (bluetoothPairingDialogFragment != null && (bluetoothPairingDialogFragment.isPairingControllerSet() || bluetoothPairingDialogFragment.isPairingDialogActivitySet())) {
            bluetoothPairingDialogFragment.dismiss();
            bluetoothPairingDialogFragment = null;
        }
        if (bluetoothPairingDialogFragment != null) {
            z = true;
        } else {
            z = false;
            bluetoothPairingDialogFragment = new BluetoothPairingDialogFragment();
        }
        bluetoothPairingDialogFragment.setPairingController(this.mBluetoothPairingController);
        bluetoothPairingDialogFragment.setPairingDialogActivity(this);
        if (!z) {
            bluetoothPairingDialogFragment.show(getFragmentManager(), "bluetooth.pairing.fragment");
        }
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_CANCEL"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED"));
        this.mReceiverRegistered = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mReceiverRegistered) {
            this.mReceiverRegistered = false;
            unregisterReceiver(this.mReceiver);
        }
    }

    void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }
}
