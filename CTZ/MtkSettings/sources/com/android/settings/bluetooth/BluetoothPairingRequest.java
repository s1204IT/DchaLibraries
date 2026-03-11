package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;

public final class BluetoothPairingRequest extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            return;
        }
        Intent pairingDialogIntent = BluetoothPairingService.getPairingDialogIntent(context, intent);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        boolean zShouldShowDialogInForeground = LocalBluetoothPreferences.shouldShowDialogInForeground(context, bluetoothDevice != null ? bluetoothDevice.getAddress() : null, bluetoothDevice != null ? bluetoothDevice.getName() : null);
        if (powerManager.isInteractive() && zShouldShowDialogInForeground) {
            context.startActivityAsUser(pairingDialogIntent, UserHandle.CURRENT);
        } else {
            intent.setClass(context, BluetoothPairingService.class);
            context.startServiceAsUser(intent, UserHandle.CURRENT);
        }
    }
}
