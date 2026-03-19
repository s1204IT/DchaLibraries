package com.android.bluetooth.opp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;

public class BluetoothOppHandoverReceiver extends BroadcastReceiver {
    private static final boolean D = Constants.DEBUG;
    public static final String TAG = "BluetoothOppHandoverReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final ArrayList parcelableArrayListExtra;
        String action = intent.getAction();
        if (action.equals("android.nfc.handover.intent.action.HANDOVER_SEND") || action.equals("android.nfc.handover.intent.action.HANDOVER_SEND_MULTIPLE")) {
            final BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (bluetoothDevice == null) {
                if (D) {
                    Log.d(TAG, "No device attached to handover intent.");
                    return;
                }
                return;
            }
            final String type = intent.getType();
            ArrayList arrayList = new ArrayList();
            if (!action.equals("android.nfc.handover.intent.action.HANDOVER_SEND")) {
                if (action.equals("android.nfc.handover.intent.action.HANDOVER_SEND_MULTIPLE")) {
                    parcelableArrayListExtra = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                }
                if (type == null && parcelableArrayListExtra != null && !parcelableArrayListExtra.isEmpty()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothOppManager.getInstance(context).saveSendingFileInfo(type, parcelableArrayListExtra, true, true);
                            BluetoothOppManager.getInstance(context).startTransfer(bluetoothDevice);
                        }
                    }).start();
                    return;
                } else {
                    if (!D) {
                        Log.d(TAG, "No mimeType or stream attached to handover request");
                        return;
                    }
                    return;
                }
            }
            Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
            if (uri != null) {
                arrayList.add(uri);
            }
            parcelableArrayListExtra = arrayList;
            if (type == null) {
            }
            if (!D) {
            }
        } else {
            if (action.equals("android.btopp.intent.action.WHITELIST_DEVICE")) {
                BluetoothDevice bluetoothDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (D) {
                    Log.d(TAG, "Adding " + bluetoothDevice2 + " to whitelist");
                }
                if (bluetoothDevice2 == null) {
                    return;
                }
                BluetoothOppManager.getInstance(context).addToWhitelist(bluetoothDevice2.getAddress());
                return;
            }
            if (action.equals("android.btopp.intent.action.STOP_HANDOVER_TRANSFER")) {
                int intExtra = intent.getIntExtra("android.nfc.handover.intent.extra.TRANSFER_ID", -1);
                if (intExtra != -1) {
                    Uri uri2 = Uri.parse(BluetoothShare.CONTENT_URI + "/" + intExtra);
                    if (D) {
                        Log.d(TAG, "Stopping handover transfer with Uri " + uri2);
                    }
                    context.getContentResolver().delete(uri2, null, null);
                    return;
                }
                return;
            }
            if (D) {
                Log.d(TAG, "Unknown action: " + action);
            }
        }
    }
}
