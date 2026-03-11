package com.android.settings.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public final class DockEventReceiver extends BroadcastReceiver {
    private static PowerManager.WakeLock sStartingService;
    private static final Object sStartingServiceSync = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1234));
        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if ("android.intent.action.DOCK_EVENT".equals(intent.getAction()) || "com.android.settings.bluetooth.action.DOCK_SHOW_UI".endsWith(intent.getAction())) {
            if (device == null) {
                if (!"com.android.settings.bluetooth.action.DOCK_SHOW_UI".endsWith(intent.getAction())) {
                    if (state != 0 && state != 3) {
                        return;
                    }
                } else {
                    return;
                }
            }
            switch (state) {
                case DefaultWfcSettingsExt.RESUME:
                case DefaultWfcSettingsExt.PAUSE:
                case DefaultWfcSettingsExt.CREATE:
                case DefaultWfcSettingsExt.DESTROY:
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    Intent i = new Intent(intent);
                    i.setClass(context, DockService.class);
                    beginStartingService(context, i);
                    break;
                default:
                    Log.e("DockEventReceiver", "Unknown state: " + state);
                    break;
            }
            return;
        }
        if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction()) || "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
            int newState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 2);
            int oldState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            if (device == null || newState != 0 || oldState == 3) {
                return;
            }
            Intent i2 = new Intent(intent);
            i2.setClass(context, DockService.class);
            beginStartingService(context, i2);
            return;
        }
        if (!"android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
            return;
        }
        int btState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
        if (btState == 11) {
            return;
        }
        Intent i3 = new Intent(intent);
        i3.setClass(context, DockService.class);
        beginStartingService(context, i3);
    }

    private static void beginStartingService(Context context, Intent intent) {
        synchronized (sStartingServiceSync) {
            if (sStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService("power");
                sStartingService = pm.newWakeLock(1, "StartingDockService");
            }
            sStartingService.acquire();
            try {
                if (context.startService(intent) == null) {
                    Log.e("DockEventReceiver", "Can't start DockService");
                }
            } catch (SecurityException e) {
                Log.e("DockEventReceiver", "Caller app is killed because of LMK,so app handles this JE");
            }
        }
    }

    public static void finishStartingService(Service service, int startId) {
        synchronized (sStartingServiceSync) {
            if (sStartingService != null && service.stopSelfResult(startId)) {
                Log.d("DockEventReceiver", "finishStartingService: stopping service");
                sStartingService.release();
            }
        }
    }
}
