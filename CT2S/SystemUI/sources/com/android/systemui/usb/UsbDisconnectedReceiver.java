package com.android.systemui.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;

class UsbDisconnectedReceiver extends BroadcastReceiver {
    private UsbAccessory mAccessory;
    private final Activity mActivity;
    private UsbDevice mDevice;

    public UsbDisconnectedReceiver(Activity activity, UsbDevice device) {
        this.mActivity = activity;
        this.mDevice = device;
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED");
        activity.registerReceiver(this, filter);
    }

    public UsbDisconnectedReceiver(Activity activity, UsbAccessory accessory) {
        this.mActivity = activity;
        this.mAccessory = accessory;
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_ACCESSORY_DETACHED");
        activity.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbAccessory accessory;
        String action = intent.getAction();
        if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");
            if (device != null && device.equals(this.mDevice)) {
                this.mActivity.finish();
                return;
            }
            return;
        }
        if ("android.hardware.usb.action.USB_ACCESSORY_DETACHED".equals(action) && (accessory = (UsbAccessory) intent.getParcelableExtra("accessory")) != null && accessory.equals(this.mAccessory)) {
            this.mActivity.finish();
        }
    }
}
