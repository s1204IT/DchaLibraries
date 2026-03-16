package com.android.settings.bluetooth;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BluetoothPairingRequest extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int type = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
            Intent pairingIntent = new Intent();
            pairingIntent.setClass(context, BluetoothPairingDialog.class);
            pairingIntent.putExtra("android.bluetooth.device.extra.DEVICE", device);
            pairingIntent.putExtra("android.bluetooth.device.extra.PAIRING_VARIANT", type);
            if (type == 2 || type == 4 || type == 5) {
                int pairingKey = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE);
                pairingIntent.putExtra("android.bluetooth.device.extra.PAIRING_KEY", pairingKey);
            }
            pairingIntent.setAction("android.bluetooth.device.action.PAIRING_REQUEST");
            pairingIntent.setFlags(268435456);
            if (device != null) {
                device.getAddress();
            }
            context.startActivity(pairingIntent);
            playPopupAlert(context);
            return;
        }
        if (action.equals("android.bluetooth.device.action.PAIRING_CANCEL")) {
            NotificationManager manager = (NotificationManager) context.getSystemService("notification");
            manager.cancel(R.drawable.stat_sys_data_bluetooth);
        } else if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
            int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
            int oldState = intent.getIntExtra("android.bluetooth.device.extra.PREVIOUS_BOND_STATE", Integer.MIN_VALUE);
            if (oldState == 11 && bondState == 10) {
                NotificationManager manager2 = (NotificationManager) context.getSystemService("notification");
                manager2.cancel(R.drawable.stat_sys_data_bluetooth);
            }
        }
    }

    private void playPopupAlert(Context context) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setDefaults(1);
        NotificationManager manager = (NotificationManager) context.getSystemService("notification");
        manager.notify(R.drawable.stat_sys_data_bluetooth, builder.getNotification());
    }
}
