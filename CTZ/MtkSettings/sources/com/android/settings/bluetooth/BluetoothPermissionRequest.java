package com.android.settings.bluetooth;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

public final class BluetoothPermissionRequest extends BroadcastReceiver {
    Context mContext;
    BluetoothDevice mDevice;
    private NotificationChannel mNotificationChannel = null;
    int mRequestType;

    @Override
    public void onReceive(Context context, Intent intent) {
        String string;
        String string2;
        this.mContext = context;
        String action = intent.getAction();
        if (!action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST")) {
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL")) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                this.mRequestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                notificationManager.cancel(getNotificationTag(this.mRequestType), R.drawable.stat_sys_data_bluetooth);
                return;
            }
            return;
        }
        if (((UserManager) context.getSystemService("user")).isManagedProfile()) {
            return;
        }
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        this.mRequestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 1);
        if (checkUserChoice()) {
            return;
        }
        Intent intent2 = new Intent(action);
        intent2.setClass(context, BluetoothPermissionActivity.class);
        intent2.setFlags(402653184);
        intent2.setType(Integer.toString(this.mRequestType));
        intent2.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        intent2.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        String address = this.mDevice != null ? this.mDevice.getAddress() : null;
        String name = this.mDevice != null ? this.mDevice.getName() : null;
        if (((PowerManager) context.getSystemService("power")).isScreenOn() && LocalBluetoothPreferences.shouldShowDialogInForeground(context, address, name)) {
            context.startActivity(intent2);
            return;
        }
        Intent intent3 = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intent3.setPackage("com.android.bluetooth");
        intent3.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent3.putExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2);
        intent3.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        String strCreateRemoteName = Utils.createRemoteName(context, this.mDevice);
        switch (this.mRequestType) {
            case 2:
                string = context.getString(com.android.settings.R.string.bluetooth_phonebook_request);
                string2 = context.getString(com.android.settings.R.string.bluetooth_phonebook_access_notification_content);
                break;
            case 3:
                string = context.getString(com.android.settings.R.string.bluetooth_map_request);
                string2 = context.getString(com.android.settings.R.string.bluetooth_message_access_notification_content);
                break;
            case 4:
                string = context.getString(com.android.settings.R.string.bluetooth_sap_request);
                string2 = context.getString(com.android.settings.R.string.bluetooth_sap_acceptance_dialog_text, strCreateRemoteName, strCreateRemoteName);
                break;
            default:
                string = context.getString(com.android.settings.R.string.bluetooth_connection_permission_request);
                string2 = context.getString(com.android.settings.R.string.bluetooth_connection_dialog_text, strCreateRemoteName, strCreateRemoteName);
                break;
        }
        NotificationManager notificationManager2 = (NotificationManager) context.getSystemService("notification");
        if (this.mNotificationChannel == null) {
            this.mNotificationChannel = new NotificationChannel("bluetooth_notification_channel", context.getString(com.android.settings.R.string.bluetooth), 4);
            notificationManager2.createNotificationChannel(this.mNotificationChannel);
        }
        Notification notificationBuild = new Notification.Builder(context, "bluetooth_notification_channel").setContentTitle(string).setTicker(string2).setContentText(string2).setStyle(new Notification.BigTextStyle().bigText(string2)).setSmallIcon(R.drawable.stat_sys_data_bluetooth).setAutoCancel(true).setPriority(2).setOnlyAlertOnce(false).setDefaults(-1).setContentIntent(PendingIntent.getActivity(context, 0, intent2, 0)).setDeleteIntent(PendingIntent.getBroadcast(context, 0, intent3, 0)).setColor(context.getColor(R.color.car_colorPrimary)).setLocalOnly(true).build();
        notificationBuild.flags |= 32;
        notificationManager2.notify(getNotificationTag(this.mRequestType), R.drawable.stat_sys_data_bluetooth, notificationBuild);
    }

    private String getNotificationTag(int i) {
        if (i == 2) {
            return "Phonebook Access";
        }
        if (this.mRequestType == 3) {
            return "Message Access";
        }
        if (this.mRequestType == 4) {
            return "SIM Access";
        }
        return null;
    }

    private boolean checkUserChoice() {
        int simPermissionChoice;
        if (this.mRequestType != 2 && this.mRequestType != 3 && this.mRequestType != 4) {
            return false;
        }
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(this.mContext);
        CachedBluetoothDeviceManager cachedDeviceManager = localBtManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedBluetoothDeviceFindDevice = cachedDeviceManager.findDevice(this.mDevice);
        if (cachedBluetoothDeviceFindDevice == null) {
            cachedBluetoothDeviceFindDevice = cachedDeviceManager.addDevice(localBtManager.getBluetoothAdapter(), localBtManager.getProfileManager(), this.mDevice);
        }
        if (this.mRequestType == 2) {
            int phonebookPermissionChoice = cachedBluetoothDeviceFindDevice.getPhonebookPermissionChoice();
            if (phonebookPermissionChoice == 0) {
                return false;
            }
            if (phonebookPermissionChoice == 1) {
                sendReplyIntentToReceiver(true);
            } else if (phonebookPermissionChoice == 2) {
                sendReplyIntentToReceiver(false);
            } else {
                Log.e("BluetoothPermissionRequest", "Bad phonebookPermission: " + phonebookPermissionChoice);
                return false;
            }
            return true;
        }
        if (this.mRequestType == 3) {
            int messagePermissionChoice = cachedBluetoothDeviceFindDevice.getMessagePermissionChoice();
            if (messagePermissionChoice == 0) {
                return false;
            }
            if (messagePermissionChoice == 1) {
                sendReplyIntentToReceiver(true);
            } else if (messagePermissionChoice == 2) {
                sendReplyIntentToReceiver(false);
            } else {
                Log.e("BluetoothPermissionRequest", "Bad messagePermission: " + messagePermissionChoice);
                return false;
            }
            return true;
        }
        if (this.mRequestType != 4 || (simPermissionChoice = cachedBluetoothDeviceFindDevice.getSimPermissionChoice()) == 0) {
            return false;
        }
        if (simPermissionChoice == 1) {
            sendReplyIntentToReceiver(true);
        } else if (simPermissionChoice == 2) {
            sendReplyIntentToReceiver(false);
        } else {
            Log.e("BluetoothPermissionRequest", "Bad simPermission: " + simPermissionChoice);
            return false;
        }
        return true;
    }

    private void sendReplyIntentToReceiver(boolean z) {
        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intent.putExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", z ? 1 : 2);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
    }
}
