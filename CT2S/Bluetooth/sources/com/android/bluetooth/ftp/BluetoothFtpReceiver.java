package com.android.bluetooth.ftp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.bluetooth.btservice.AdapterService;

public class BluetoothFtpReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothFtpReceiver";
    private static final boolean V = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "BluetoothFtpReceiver onReceive :" + intent.getAction());
        Intent in = new Intent();
        in.putExtras(intent);
        in.setClass(context, BluetoothFtpService.class);
        String action = intent.getAction();
        in.putExtra(AdapterService.EXTRA_ACTION, action);
        boolean startService = true;
        if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
            int state = in.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            in.putExtra("android.bluetooth.adapter.extra.STATE", state);
            if (state == 11 || state == 10) {
                startService = false;
            }
        }
        if (startService) {
            context.startService(in);
        }
    }
}
