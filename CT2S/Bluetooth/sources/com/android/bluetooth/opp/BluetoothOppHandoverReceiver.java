package com.android.bluetooth.opp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;

public class BluetoothOppHandoverReceiver extends BroadcastReceiver {
    private static final boolean D = true;
    public static final String TAG = "BluetoothOppHandoverReceiver";
    private static final boolean V = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Constants.ACTION_HANDOVER_SEND) || action.equals(Constants.ACTION_HANDOVER_SEND_MULTIPLE)) {
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (device == null) {
                Log.d(TAG, "No device attached to handover intent.");
                return;
            }
            if (action.equals(Constants.ACTION_HANDOVER_SEND)) {
                String type = intent.getType();
                Uri stream = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
                if (stream != null && type != null) {
                    BluetoothOppManager.getInstance(context).saveSendingFileInfo(type, stream.toString(), true, true);
                } else {
                    Log.d(TAG, "No mimeType or stream attached to handover request");
                }
            } else if (action.equals(Constants.ACTION_HANDOVER_SEND_MULTIPLE)) {
                new ArrayList();
                String mimeType = intent.getType();
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                if (mimeType != null && uris != null) {
                    BluetoothOppManager.getInstance(context).saveSendingFileInfo(mimeType, uris, true, true);
                } else {
                    Log.d(TAG, "No mimeType or stream attached to handover request");
                    return;
                }
            }
            BluetoothOppManager.getInstance(context).startTransfer(device);
            return;
        }
        if (action.equals(Constants.ACTION_WHITELIST_DEVICE)) {
            BluetoothDevice device2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Log.d(TAG, "Adding " + device2 + " to whitelist");
            if (device2 != null) {
                BluetoothOppManager.getInstance(context).addToWhitelist(device2.getAddress());
                return;
            }
            return;
        }
        if (action.equals(Constants.ACTION_STOP_HANDOVER)) {
            int id = intent.getIntExtra(Constants.EXTRA_BT_OPP_TRANSFER_ID, -1);
            if (id != -1) {
                Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + id);
                Log.d(TAG, "Stopping handover transfer with Uri " + contentUri);
                context.getContentResolver().delete(contentUri, null, null);
                return;
            }
            return;
        }
        Log.d(TAG, "Unknown action: " + action);
    }
}
