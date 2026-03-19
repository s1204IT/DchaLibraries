package com.android.providers.media;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;

public class MtpReceiver extends BroadcastReceiver {
    private static final String TAG = MtpReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!"android.intent.action.BOOT_COMPLETED".equals(action)) {
            if ("android.hardware.usb.action.USB_STATE".equals(action)) {
                handleUsbState(context, intent);
            }
        } else {
            Intent intentRegisterReceiver = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
            if (intentRegisterReceiver != null) {
                handleUsbState(context, intentRegisterReceiver);
            }
        }
    }

    private void handleUsbState(Context context, Intent intent) {
        boolean z;
        Bundle extras = intent.getExtras();
        boolean z2 = extras.getBoolean("configured");
        boolean z3 = extras.getBoolean("connected");
        boolean z4 = extras.getBoolean("mtp");
        boolean z5 = extras.getBoolean("ptp");
        boolean z6 = extras.getBoolean("unlocked");
        if (UserHandle.myUserId() != ActivityManager.getCurrentUser()) {
            z = false;
        } else {
            z = true;
        }
        if (z2 && (z4 || z5)) {
            if (z) {
                context.getContentResolver().insert(Uri.parse("content://media/none/mtp_connected"), null);
                Intent intent2 = new Intent(context, (Class<?>) MtpService.class);
                intent2.putExtra("unlocked", z6);
                if (z5) {
                    intent2.putExtra("ptp", true);
                }
                context.startService(intent2);
                return;
            }
            return;
        }
        if (!z3 || (!z4 && !z5)) {
            context.stopService(new Intent(context, (Class<?>) MtpService.class));
            context.getContentResolver().delete(Uri.parse("content://media/none/mtp_connected"), null, null);
        }
    }
}
