package com.android.settings.bluetooth;

import android.R;
import android.app.Notification;
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
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public final class BluetoothPermissionRequest extends BroadcastReceiver {
    Context mContext;
    BluetoothDevice mDevice;
    int mRequestType;
    String mReturnPackage = null;
    String mReturnClass = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String title;
        String message;
        this.mContext = context;
        String action = intent.getAction();
        if (!action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST")) {
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL")) {
                NotificationManager manager = (NotificationManager) context.getSystemService("notification");
                this.mRequestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                manager.cancel(getNotificationTag(this.mRequestType), R.drawable.stat_sys_data_bluetooth);
                return;
            }
            return;
        }
        UserManager um = (UserManager) context.getSystemService("user");
        if (com.android.settings.Utils.isManagedProfile(um)) {
            return;
        }
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        this.mRequestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 1);
        this.mReturnPackage = intent.getStringExtra("android.bluetooth.device.extra.PACKAGE_NAME");
        this.mReturnClass = intent.getStringExtra("android.bluetooth.device.extra.CLASS_NAME");
        if (checkUserChoice()) {
            return;
        }
        Intent connectionAccessIntent = new Intent(action);
        connectionAccessIntent.setClass(context, BluetoothPermissionActivity.class);
        connectionAccessIntent.setFlags(402653184);
        connectionAccessIntent.setType(Integer.toString(this.mRequestType));
        connectionAccessIntent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        connectionAccessIntent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        connectionAccessIntent.putExtra("android.bluetooth.device.extra.PACKAGE_NAME", this.mReturnPackage);
        connectionAccessIntent.putExtra("android.bluetooth.device.extra.CLASS_NAME", this.mReturnClass);
        String address = this.mDevice != null ? this.mDevice.getAddress() : null;
        String name = this.mDevice != null ? this.mDevice.getName() : null;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (powerManager.isScreenOn() && LocalBluetoothPreferences.shouldShowDialogInForeground(context, address, name)) {
            context.startActivity(connectionAccessIntent);
            return;
        }
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(805306394, "ConnectionAccessActivity");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        Intent deleteIntent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        deleteIntent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        deleteIntent.putExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2);
        deleteIntent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        String aliasName = this.mDevice != null ? this.mDevice.getAliasName() : null;
        switch (this.mRequestType) {
            case DefaultWfcSettingsExt.CREATE:
                title = context.getString(com.android.settings.R.string.bluetooth_phonebook_request);
                message = context.getString(com.android.settings.R.string.bluetooth_pb_acceptance_dialog_text, aliasName, aliasName);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                title = context.getString(com.android.settings.R.string.bluetooth_map_request);
                message = context.getString(com.android.settings.R.string.bluetooth_map_acceptance_dialog_text, aliasName, aliasName);
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                title = context.getString(com.android.settings.R.string.bluetooth_sap_request);
                message = context.getString(com.android.settings.R.string.bluetooth_sap_acceptance_dialog_text, aliasName, aliasName);
                break;
            default:
                title = context.getString(com.android.settings.R.string.bluetooth_connection_permission_request);
                message = context.getString(com.android.settings.R.string.bluetooth_connection_dialog_text, aliasName, aliasName);
                break;
        }
        Notification notification = new Notification.Builder(context).setContentTitle(title).setTicker(message).setContentText(message).setSmallIcon(R.drawable.stat_sys_data_bluetooth).setAutoCancel(true).setPriority(2).setOnlyAlertOnce(false).setDefaults(-1).setContentIntent(PendingIntent.getActivity(context, 0, connectionAccessIntent, 0)).setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0)).setColor(context.getColor(R.color.system_accent3_600)).build();
        notification.flags |= 32;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.notify(getNotificationTag(this.mRequestType), R.drawable.stat_sys_data_bluetooth, notification);
        wakeLock.release();
    }

    private String getNotificationTag(int requestType) {
        if (requestType == 2) {
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
        int simPermission;
        if (this.mRequestType != 2 && this.mRequestType != 3 && this.mRequestType != 4) {
            return false;
        }
        LocalBluetoothManager bluetoothManager = Utils.getLocalBtManager(this.mContext);
        CachedBluetoothDeviceManager cachedDeviceManager = bluetoothManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedDevice = cachedDeviceManager.findDevice(this.mDevice);
        if (cachedDevice == null) {
            cachedDevice = cachedDeviceManager.addDevice(bluetoothManager.getBluetoothAdapter(), bluetoothManager.getProfileManager(), this.mDevice);
        }
        if (this.mRequestType == 2) {
            int phonebookPermission = cachedDevice.getPhonebookPermissionChoice();
            if (phonebookPermission == 0) {
                return false;
            }
            if (phonebookPermission == 1) {
                sendReplyIntentToReceiver(true);
                return true;
            }
            if (phonebookPermission == 2) {
                sendReplyIntentToReceiver(false);
                return true;
            }
            Log.e("BluetoothPermissionRequest", "Bad phonebookPermission: " + phonebookPermission);
            return false;
        }
        if (this.mRequestType == 3) {
            int messagePermission = cachedDevice.getMessagePermissionChoice();
            if (messagePermission == 0) {
                return false;
            }
            if (messagePermission == 1) {
                sendReplyIntentToReceiver(true);
                return true;
            }
            if (messagePermission == 2) {
                sendReplyIntentToReceiver(false);
                return true;
            }
            Log.e("BluetoothPermissionRequest", "Bad messagePermission: " + messagePermission);
            return false;
        }
        if (this.mRequestType != 4 || (simPermission = cachedDevice.getSimPermissionChoice()) == 0) {
            return false;
        }
        if (simPermission == 1) {
            sendReplyIntentToReceiver(true);
            return true;
        }
        if (simPermission == 2) {
            sendReplyIntentToReceiver(false);
            return true;
        }
        Log.e("BluetoothPermissionRequest", "Bad simPermission: " + simPermission);
        return false;
    }

    private void sendReplyIntentToReceiver(boolean allowed) {
        Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        if (this.mReturnPackage != null && this.mReturnClass != null) {
            intent.setClassName(this.mReturnPackage, this.mReturnClass);
        }
        intent.putExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", allowed ? 1 : 2);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", this.mRequestType);
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
    }
}
