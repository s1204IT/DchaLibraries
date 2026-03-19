package com.android.server.usb;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.os.UserHandle;
import com.android.internal.notification.SystemNotificationChannels;

class MtpNotificationManager {
    private static final String ACTION_OPEN_IN_APPS = "com.android.server.usb.ACTION_OPEN_IN_APPS";
    private static final int PROTOCOL_MTP = 0;
    private static final int PROTOCOL_PTP = 1;
    private static final int SUBCLASS_MTP = 255;
    private static final int SUBCLASS_STILL_IMAGE_CAPTURE = 1;
    private static final String TAG = "UsbMtpNotificationManager";
    private final Context mContext;
    private final OnOpenInAppListener mListener;

    interface OnOpenInAppListener {
        void onOpenInApp(UsbDevice usbDevice);
    }

    MtpNotificationManager(Context context, OnOpenInAppListener onOpenInAppListener) {
        this.mContext = context;
        this.mListener = onOpenInAppListener;
        context.registerReceiver(new Receiver(), new IntentFilter(ACTION_OPEN_IN_APPS));
    }

    void showNotification(UsbDevice usbDevice) {
        Resources resources = this.mContext.getResources();
        Notification.Builder category = new Notification.Builder(this.mContext, SystemNotificationChannels.USB).setContentTitle(resources.getString(R.string.miniresolver_open_in_work, usbDevice.getProductName())).setContentText(resources.getString(R.string.miniresolver_open_in_personal)).setSmallIcon(R.drawable.pointer_spot_hover_vector).setCategory("sys");
        Intent intent = new Intent(ACTION_OPEN_IN_APPS);
        intent.putExtra("device", usbDevice);
        intent.addFlags(1342177280);
        category.setContentIntent(PendingIntent.getBroadcastAsUser(this.mContext, usbDevice.getDeviceId(), intent, 134217728, UserHandle.SYSTEM));
        Notification notificationBuild = category.build();
        notificationBuild.flags |= 256;
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notify(Integer.toString(usbDevice.getDeviceId()), 25, notificationBuild);
    }

    void hideNotification(int i) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(Integer.toString(i), 25);
    }

    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            UsbDevice usbDevice = (UsbDevice) intent.getExtras().getParcelable("device");
            if (usbDevice == null) {
                return;
            }
            String action = intent.getAction();
            byte b = -1;
            if (action.hashCode() == 768361239 && action.equals(MtpNotificationManager.ACTION_OPEN_IN_APPS)) {
                b = 0;
            }
            if (b == 0) {
                MtpNotificationManager.this.mListener.onOpenInApp(usbDevice);
            }
        }
    }

    static boolean shouldShowNotification(PackageManager packageManager, UsbDevice usbDevice) {
        return !packageManager.hasSystemFeature("android.hardware.type.automotive") && isMtpDevice(usbDevice);
    }

    private static boolean isMtpDevice(UsbDevice usbDevice) {
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == 6 && usbInterface.getInterfaceSubclass() == 1 && usbInterface.getInterfaceProtocol() == 1) {
                return true;
            }
            if (usbInterface.getInterfaceClass() == 255 && usbInterface.getInterfaceSubclass() == 255 && usbInterface.getInterfaceProtocol() == 0 && "MTP".equals(usbInterface.getName())) {
                return true;
            }
        }
        return false;
    }
}
