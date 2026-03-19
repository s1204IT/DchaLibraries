package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.android.bluetooth.R;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardConfig;
import java.util.regex.Pattern;

public class BluetoothOppReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothOppReceiver";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;

    @Override
    public void onReceive(Context context, Intent intent) {
        String string;
        String action = intent.getAction();
        if (action.equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
            BluetoothOppManager bluetoothOppManager = BluetoothOppManager.getInstance(context);
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (D) {
                Log.d(TAG, "Received BT device selected intent, bt device: " + bluetoothDevice);
            }
            if (bluetoothDevice == null) {
                bluetoothOppManager.cleanUpSendingFileInfo();
                return;
            }
            if (BluetoothOppLauncherActivity.sSendingFileFlag.compareAndSet(true, false)) {
                bluetoothOppManager.startTransfer(bluetoothDevice);
                if (V) {
                    Log.d(TAG, "sSendingFileFlag : " + BluetoothOppLauncherActivity.sSendingFileFlag.get());
                }
                String deviceName = bluetoothOppManager.getDeviceName(bluetoothDevice);
                int batchSize = bluetoothOppManager.getBatchSize();
                if (bluetoothOppManager.mMultipleFlag) {
                    string = context.getString(R.string.bt_toast_5, Integer.toString(batchSize), deviceName);
                } else {
                    string = context.getString(R.string.bt_toast_4, deviceName);
                }
                Toast.makeText(context, string, 0).show();
                return;
            }
            if (V) {
                Log.d(TAG, "Ignore other selected devices : " + bluetoothDevice);
                return;
            }
            return;
        }
        if (action.equals("android.btopp.intent.action.CONFIRM")) {
            if (V) {
                Log.v(TAG, "Receiver ACTION_INCOMING_FILE_CONFIRM");
            }
            Uri data = intent.getData();
            Intent intent2 = new Intent(context, (Class<?>) BluetoothOppIncomingFileConfirmActivity.class);
            intent2.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent2.setDataAndNormalize(data);
            context.startActivity(intent2);
            return;
        }
        String string2 = null;
        if (action.equals("android.btopp.intent.action.DECLINE")) {
            if (V) {
                Log.v(TAG, "Receiver ACTION_DECLINE");
            }
            Uri data2 = intent.getData();
            ContentValues contentValues = new ContentValues();
            contentValues.put("confirm", (Integer) 3);
            context.getContentResolver().update(data2, contentValues, null, null);
            cancelNotification(context, BluetoothOppNotification.NOTIFICATION_ID_PROGRESS);
            return;
        }
        if (action.equals("android.btopp.intent.action.ACCEPT")) {
            if (V) {
                Log.v(TAG, "Receiver ACTION_ACCEPT");
            }
            Uri data3 = intent.getData();
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("confirm", (Integer) 1);
            context.getContentResolver().update(data3, contentValues2, null, null);
            return;
        }
        if (action.equals("android.btopp.intent.action.OPEN") || action.equals("android.btopp.intent.action.LIST")) {
            if (V) {
                if (action.equals("android.btopp.intent.action.OPEN")) {
                    Log.v(TAG, "Receiver open for " + intent.getData());
                } else {
                    Log.v(TAG, "Receiver list for " + intent.getData());
                }
            }
            new BluetoothOppTransferInfo();
            Uri data4 = intent.getData();
            BluetoothOppTransferInfo bluetoothOppTransferInfoQueryRecord = BluetoothOppUtility.queryRecord(context, data4);
            if (bluetoothOppTransferInfoQueryRecord == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            if (bluetoothOppTransferInfoQueryRecord.mDirection == 1 && BluetoothShare.isStatusSuccess(bluetoothOppTransferInfoQueryRecord.mStatus)) {
                BluetoothOppUtility.openReceivedFile(context, bluetoothOppTransferInfoQueryRecord.mFileName, bluetoothOppTransferInfoQueryRecord.mFileType, bluetoothOppTransferInfoQueryRecord.mTimeStamp, data4);
                BluetoothOppUtility.updateVisibilityToHidden(context, data4);
                return;
            } else {
                Intent intent3 = new Intent(context, (Class<?>) BluetoothOppTransferActivity.class);
                intent3.setFlags(335544320);
                intent3.setDataAndNormalize(data4);
                context.startActivity(intent3);
                return;
            }
        }
        if (action.equals("android.btopp.intent.action.OPEN_OUTBOUND")) {
            if (V) {
                Log.v(TAG, "Received ACTION_OPEN_OUTBOUND_TRANSFER.");
            }
            Intent intent4 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            intent4.setFlags(335544320);
            intent4.putExtra(BluetoothShare.DIRECTION, 0);
            context.startActivity(intent4);
            return;
        }
        if (action.equals("android.btopp.intent.action.OPEN_INBOUND")) {
            if (V) {
                Log.v(TAG, "Received ACTION_OPEN_INBOUND_TRANSFER.");
            }
            Intent intent5 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            intent5.setFlags(335544320);
            intent5.putExtra(BluetoothShare.DIRECTION, 1);
            context.startActivity(intent5);
            return;
        }
        if (action.equals("android.btopp.intent.action.OPEN_RECEIVED_FILES")) {
            if (V) {
                Log.v(TAG, "Received ACTION_OPEN_RECEIVED_FILES.");
            }
            Intent intent6 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            intent6.setFlags(335544320);
            intent6.putExtra(BluetoothShare.DIRECTION, 1);
            intent6.putExtra("android.btopp.intent.extra.SHOW_ALL", true);
            context.startActivity(intent6);
            return;
        }
        if (action.equals("android.btopp.intent.action.HIDE")) {
            if (V) {
                Log.v(TAG, "Receiver hide for " + intent.getData());
            }
            Cursor cursorQuery = context.getContentResolver().query(intent.getData(), null, null, null, null);
            if (cursorQuery != null) {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow(BluetoothShare.VISIBILITY));
                    if (cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("confirm")) == 0 && i == 0) {
                        ContentValues contentValues3 = new ContentValues();
                        contentValues3.put(BluetoothShare.VISIBILITY, (Integer) 1);
                        context.getContentResolver().update(intent.getData(), contentValues3, null, null);
                        if (V) {
                            Log.v(TAG, "Action_hide received and db updated");
                        }
                    }
                }
                cursorQuery.close();
                return;
            }
            return;
        }
        if (action.equals("android.btopp.intent.action.HIDE_COMPLETE")) {
            if (V) {
                Log.v(TAG, "Receiver ACTION_COMPLETE_HIDE");
            }
            ContentValues contentValues4 = new ContentValues();
            contentValues4.put(BluetoothShare.VISIBILITY, (Integer) 1);
            context.getContentResolver().update(BluetoothShare.CONTENT_URI, contentValues4, "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5')", null);
            return;
        }
        if (action.equals(BluetoothShare.TRANSFER_COMPLETED_ACTION)) {
            if (V) {
                Log.v(TAG, "Receiver Transfer Complete Intent for " + intent.getData());
            }
            new BluetoothOppTransferInfo();
            BluetoothOppTransferInfo bluetoothOppTransferInfoQueryRecord2 = BluetoothOppUtility.queryRecord(context, intent.getData());
            if (bluetoothOppTransferInfoQueryRecord2 == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            if (bluetoothOppTransferInfoQueryRecord2.mHandoverInitiated) {
                Intent intent7 = new Intent("android.nfc.handover.intent.action.TRANSFER_DONE");
                if (bluetoothOppTransferInfoQueryRecord2.mDirection == 1) {
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_DIRECTION", 0);
                } else {
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_DIRECTION", 1);
                }
                intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_ID", bluetoothOppTransferInfoQueryRecord2.mID);
                intent7.putExtra("android.nfc.handover.intent.extra.ADDRESS", bluetoothOppTransferInfoQueryRecord2.mDestAddr);
                if (BluetoothShare.isStatusSuccess(bluetoothOppTransferInfoQueryRecord2.mStatus)) {
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_STATUS", 0);
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_URI", bluetoothOppTransferInfoQueryRecord2.mFileName);
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_MIME_TYPE", bluetoothOppTransferInfoQueryRecord2.mFileType);
                } else {
                    intent7.putExtra("android.nfc.handover.intent.extra.TRANSFER_STATUS", 1);
                }
                context.sendBroadcast(intent7, "android.permission.NFC_HANDOVER_STATUS");
                return;
            }
            if (BluetoothShare.isStatusSuccess(bluetoothOppTransferInfoQueryRecord2.mStatus)) {
                if (bluetoothOppTransferInfoQueryRecord2.mDirection == 0) {
                    string2 = context.getString(R.string.notification_sent, bluetoothOppTransferInfoQueryRecord2.mFileName);
                } else if (bluetoothOppTransferInfoQueryRecord2.mDirection == 1) {
                    string2 = context.getString(R.string.notification_received, bluetoothOppTransferInfoQueryRecord2.mFileName);
                }
            } else if (BluetoothShare.isStatusError(bluetoothOppTransferInfoQueryRecord2.mStatus)) {
                if (bluetoothOppTransferInfoQueryRecord2.mDirection == 0) {
                    string2 = context.getString(R.string.notification_sent_fail, bluetoothOppTransferInfoQueryRecord2.mFileName);
                } else if (bluetoothOppTransferInfoQueryRecord2.mDirection == 1) {
                    string2 = context.getString(R.string.download_fail_line1);
                }
            }
            if (V) {
                Log.v(TAG, "Toast msg == " + string2);
            }
            if (string2 != null) {
                Toast.makeText(context, string2, 0).show();
            }
            cancelNotification(context, BluetoothOppNotification.NOTIFICATION_ID_PROGRESS);
            return;
        }
        if (action.equals("android.intent.action.MEDIA_EJECT")) {
            if (V) {
                Log.i(TAG, "received Intent.ACTION_MEDIA_EJECT");
            }
            Cursor cursorQuery2 = context.getContentResolver().query(BluetoothShare.CONTENT_URI, new String[]{BluetoothShare._DATA, "uri", BluetoothShare.DIRECTION}, "status == 192", null, null);
            if (cursorQuery2 == null) {
                Log.d(TAG, "cursor == null !");
                return;
            }
            if (cursorQuery2.moveToFirst()) {
                if (V) {
                    Log.i(TAG, "cursor != null, cursor.count = " + cursorQuery2.getCount());
                }
                int i2 = cursorQuery2.getInt(cursorQuery2.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
                String string3 = cursorQuery2.getString(cursorQuery2.getColumnIndexOrThrow("uri"));
                if (V) {
                    Log.d(TAG, "uriStr = " + string3);
                }
                String string4 = cursorQuery2.getString(cursorQuery2.getColumnIndexOrThrow(BluetoothShare._DATA));
                cursorQuery2.close();
                if (V) {
                    Log.d(TAG, "direction = " + i2 + " filepath = " + string4);
                }
                if (i2 == -1 || string4 == null) {
                    return;
                }
                String string5 = intent.getData().getPath().toString();
                if (V) {
                    Log.d(TAG, "path = " + string5);
                }
                BluetoothOppService bluetoothOppService = BluetoothOppService.getInstance();
                if (isSDCardRoot(string5) && string4.contains(string5) && bluetoothOppService != null) {
                    if (V) {
                        Log.i(TAG, "sdcard is removed, stop task");
                    }
                    if (i2 == 1) {
                        if (bluetoothOppService.mServerTransfer != null) {
                            bluetoothOppService.mServerTransfer.stop();
                        }
                    } else if (i2 == 0 && bluetoothOppService.mTransfer != null) {
                        bluetoothOppService.mTransfer.interrupt(BluetoothShare.STATUS_FILE_ERROR);
                    }
                    bluetoothOppService.mBatches.clear();
                    return;
                }
                return;
            }
            cursorQuery2.close();
            if (V) {
                Log.d(TAG, "there is no running task");
            }
        }
    }

    private void cancelNotification(Context context, int i) {
        if (V) {
            Log.v(TAG, "cancelNotification id = " + i);
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION);
        if (notificationManager == null) {
            return;
        }
        notificationManager.cancel(i);
        if (V) {
            Log.v(TAG, "notMgr.cancel called");
        }
    }

    private boolean isSDCardRoot(String str) {
        if (str == null) {
            return false;
        }
        String[] strArrSplit = str.split("/");
        Pattern patternCompile = Pattern.compile("[0-9ABCDEF]{4}-[0-9ABCDEF]{4}");
        for (String str2 : strArrSplit) {
            if (patternCompile.matcher(str2).lookingAt()) {
                return true;
            }
        }
        return false;
    }
}
