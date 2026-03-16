package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

public class MtpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            Intent usbState = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
            if (usbState != null) {
                handleUsbState(context, usbState);
                return;
            }
            return;
        }
        if ("android.hardware.usb.action.USB_STATE".equals(action)) {
            handleUsbState(context, intent);
        }
    }

    private void handleUsbState(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        boolean connected = extras.getBoolean("configured");
        boolean mtpEnabled = extras.getBoolean("mtp");
        boolean ptpEnabled = extras.getBoolean("ptp");
        if (connected && (mtpEnabled || ptpEnabled)) {
            Intent intent2 = new Intent(context, (Class<?>) MtpService.class);
            if (ptpEnabled) {
                intent2.putExtra("ptp", true);
            }
            context.startService(intent2);
            context.getContentResolver().insert(Uri.parse("content://media/none/mtp_connected"), null);
            return;
        }
        context.stopService(new Intent(context, (Class<?>) MtpService.class));
        context.getContentResolver().delete(Uri.parse("content://media/none/mtp_connected"), null, null);
    }
}
