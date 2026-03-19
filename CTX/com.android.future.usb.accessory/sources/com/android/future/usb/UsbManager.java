package com.android.future.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.IUsbManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class UsbManager {
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED";
    public static final String ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";
    public static final String EXTRA_PERMISSION_GRANTED = "permission";
    private static final String TAG = "UsbManager";
    private final Context mContext;
    private final IUsbManager mService;

    private UsbManager(Context context, IUsbManager iUsbManager) {
        this.mContext = context;
        this.mService = iUsbManager;
    }

    public static UsbManager getInstance(Context context) {
        return new UsbManager(context, IUsbManager.Stub.asInterface(ServiceManager.getService("usb")));
    }

    public static UsbAccessory getAccessory(Intent intent) {
        android.hardware.usb.UsbAccessory usbAccessory = (android.hardware.usb.UsbAccessory) intent.getParcelableExtra("accessory");
        if (usbAccessory == null) {
            return null;
        }
        return new UsbAccessory(usbAccessory);
    }

    public UsbAccessory[] getAccessoryList() {
        try {
            android.hardware.usb.UsbAccessory currentAccessory = this.mService.getCurrentAccessory();
            if (currentAccessory == null) {
                return null;
            }
            return new UsbAccessory[]{new UsbAccessory(currentAccessory)};
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getAccessoryList", e);
            return null;
        }
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory usbAccessory) {
        try {
            return this.mService.openAccessory(new android.hardware.usb.UsbAccessory(usbAccessory.getManufacturer(), usbAccessory.getModel(), usbAccessory.getDescription(), usbAccessory.getVersion(), usbAccessory.getUri(), usbAccessory.getSerial()));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openAccessory", e);
            return null;
        }
    }

    public boolean hasPermission(UsbAccessory usbAccessory) {
        try {
            return this.mService.hasAccessoryPermission(new android.hardware.usb.UsbAccessory(usbAccessory.getManufacturer(), usbAccessory.getModel(), usbAccessory.getDescription(), usbAccessory.getVersion(), usbAccessory.getUri(), usbAccessory.getSerial()));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in hasPermission", e);
            return false;
        }
    }

    public void requestPermission(UsbAccessory usbAccessory, PendingIntent pendingIntent) {
        try {
            this.mService.requestAccessoryPermission(new android.hardware.usb.UsbAccessory(usbAccessory.getManufacturer(), usbAccessory.getModel(), usbAccessory.getDescription(), usbAccessory.getVersion(), usbAccessory.getUri(), usbAccessory.getSerial()), this.mContext.getPackageName(), pendingIntent);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in requestPermission", e);
        }
    }
}
