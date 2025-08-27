package com.android.settings.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

/* loaded from: classes.dex */
public final class BluetoothPairingService extends Service {
    private BluetoothDevice mDevice;
    private boolean mRegistered = false;
    private final BroadcastReceiver mCancelReceiver = new BroadcastReceiver() { // from class: com.android.settings.bluetooth.BluetoothPairingService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", AccessPoint.UNREACHABLE_RSSI);
                if (intExtra != 10 && intExtra != 12) {
                    return;
                }
            } else if (action.equals("com.android.settings.bluetooth.ACTION_DISMISS_PAIRING")) {
                Log.d("BluetoothPairingService", "Notification cancel " + BluetoothPairingService.this.mDevice.getAddress() + " (" + BluetoothPairingService.this.mDevice.getName() + ")");
                BluetoothPairingService.this.mDevice.cancelPairingUserInput();
            } else {
                Log.d("BluetoothPairingService", "Dismiss pairing for " + BluetoothPairingService.this.mDevice.getAddress() + " (" + BluetoothPairingService.this.mDevice.getName() + "), BondState: " + intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", AccessPoint.UNREACHABLE_RSSI));
            }
            BluetoothPairingService.this.stopForeground(true);
            BluetoothPairingService.this.stopSelf();
        }
    };

    public static Intent getPairingDialogIntent(Context context, Intent intent) {
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        int intExtra = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", AccessPoint.UNREACHABLE_RSSI);
        Intent intent2 = new Intent();
        intent2.setClass(context, BluetoothPairingDialog.class);
        intent2.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent2.putExtra("android.bluetooth.device.extra.PAIRING_VARIANT", intExtra);
        if (intExtra == 2 || intExtra == 4 || intExtra == 5) {
            intent2.putExtra("android.bluetooth.device.extra.PAIRING_KEY", intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", AccessPoint.UNREACHABLE_RSSI));
        }
        intent2.setAction("android.bluetooth.device.action.PAIRING_REQUEST");
        intent2.setFlags(268435456);
        return intent2;
    }

    @Override // android.app.Service
    public void onCreate() {
        ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel("bluetooth_notification_channel", getString(R.string.bluetooth), 4));
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent == null) {
            Log.e("BluetoothPairingService", "Can't start: null intent!");
            stopSelf();
            return 2;
        }
        Resources resources = getResources();
        Notification.Builder localOnly = new Notification.Builder(this, "bluetooth_notification_channel").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setTicker(resources.getString(R.string.bluetooth_notif_ticker)).setLocalOnly(true);
        PendingIntent activity = PendingIntent.getActivity(this, 0, getPairingDialogIntent(this, intent), 1073741824);
        PendingIntent broadcast = PendingIntent.getBroadcast(this, 0, new Intent("com.android.settings.bluetooth.ACTION_DISMISS_PAIRING"), 1073741824);
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (this.mDevice != null && this.mDevice.getBondState() != 11) {
            Log.w("BluetoothPairingService", "Device " + this.mDevice + " not bonding: " + this.mDevice.getBondState());
            stopSelf();
            return 2;
        }
        String stringExtra = intent.getStringExtra("android.bluetooth.device.extra.NAME");
        if (TextUtils.isEmpty(stringExtra)) {
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            stringExtra = bluetoothDevice != null ? bluetoothDevice.getAliasName() : resources.getString(android.R.string.unknownName);
        }
        Log.d("BluetoothPairingService", "Show pairing notification for " + this.mDevice.getAddress() + " (" + stringExtra + ")");
        localOnly.setContentTitle(resources.getString(R.string.bluetooth_notif_title)).setContentText(resources.getString(R.string.bluetooth_notif_message, stringExtra)).setContentIntent(activity).setDefaults(1).setColor(getColor(android.R.color.car_cyan_400)).addAction(new Notification.Action.Builder(0, resources.getString(R.string.bluetooth_device_context_pair_connect), activity).build()).addAction(new Notification.Action.Builder(0, resources.getString(android.R.string.cancel), broadcast).build());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.PAIRING_CANCEL");
        intentFilter.addAction("com.android.settings.bluetooth.ACTION_DISMISS_PAIRING");
        registerReceiver(this.mCancelReceiver, intentFilter);
        this.mRegistered = true;
        startForeground(android.R.drawable.stat_sys_data_bluetooth, localOnly.getNotification());
        return 3;
    }

    @Override // android.app.Service
    public void onDestroy() {
        if (this.mRegistered) {
            unregisterReceiver(this.mCancelReceiver);
            this.mRegistered = false;
        }
        stopForeground(true);
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }
}
