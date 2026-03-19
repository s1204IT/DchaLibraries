package com.android.mtp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import java.io.IOException;

public class UsbIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        byte b;
        UsbDevice usbDevice = (UsbDevice) intent.getExtras().getParcelable("device");
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != -2114103349) {
            b = (iHashCode == -1608292967 && action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) ? (byte) 1 : (byte) -1;
        } else if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            b = 0;
        }
        switch (b) {
            case 0:
                MtpDocumentsProvider.getInstance().resumeRootScanner();
                break;
            case 1:
                try {
                    MtpDocumentsProvider.getInstance().closeDevice(usbDevice.getDeviceId());
                } catch (IOException | InterruptedException e) {
                    Log.e("MtpDocumentsProvider", "Failed to close device", e);
                    return;
                }
                break;
        }
    }
}
