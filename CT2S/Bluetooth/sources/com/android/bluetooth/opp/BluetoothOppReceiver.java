package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.util.Log;
import android.widget.Toast;
import com.android.bluetooth.R;
import com.android.vcard.VCardConfig;

public class BluetoothOppReceiver extends BroadcastReceiver {
    private static final boolean D = true;
    private static final String TAG = "BluetoothOppReceiver";
    private static final boolean V = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String toastMsg;
        String action = intent.getAction();
        if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
            if (12 == intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                context.startService(new Intent(context, (Class<?>) BluetoothOppService.class));
                synchronized (this) {
                    if (BluetoothOppManager.getInstance(context).mSendingFlag && BenesseExtension.getDchaState() == 0) {
                        BluetoothOppManager.getInstance(context).mSendingFlag = false;
                        Intent in1 = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
                        in1.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false);
                        in1.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 2);
                        in1.putExtra("android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE", "com.android.bluetooth");
                        in1.putExtra("android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS", BluetoothOppReceiver.class.getName());
                        in1.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                        context.startActivity(in1);
                    }
                }
                return;
            }
            return;
        }
        if (action.equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED")) {
            BluetoothOppManager mOppManager = BluetoothOppManager.getInstance(context);
            BluetoothDevice remoteDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            mOppManager.startTransfer(remoteDevice);
            String deviceName = mOppManager.getDeviceName(remoteDevice);
            int batchSize = mOppManager.getBatchSize();
            if (mOppManager.mMultipleFlag) {
                toastMsg = context.getString(R.string.bt_toast_5, Integer.toString(batchSize), deviceName);
            } else {
                toastMsg = context.getString(R.string.bt_toast_4, deviceName);
            }
            Toast.makeText(context, toastMsg, 0).show();
            return;
        }
        if (action.equals(Constants.ACTION_INCOMING_FILE_CONFIRM)) {
            Uri uri = intent.getData();
            Intent in = new Intent(context, (Class<?>) BluetoothOppIncomingFileConfirmActivity.class);
            in.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            in.setDataAndNormalize(uri);
            context.startActivity(in);
            NotificationManager notMgr = (NotificationManager) context.getSystemService("notification");
            if (notMgr != null) {
                notMgr.cancel((int) ContentUris.parseId(intent.getData()));
                return;
            }
            return;
        }
        if (action.equals(BluetoothShare.INCOMING_FILE_CONFIRMATION_REQUEST_ACTION)) {
            Toast.makeText(context, context.getString(R.string.incoming_file_toast_msg), 0).show();
            return;
        }
        if (action.equals(Constants.ACTION_OPEN) || action.equals(Constants.ACTION_LIST)) {
            new BluetoothOppTransferInfo();
            Uri uri2 = intent.getData();
            BluetoothOppTransferInfo transInfo = BluetoothOppUtility.queryRecord(context, uri2);
            if (transInfo == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            if (transInfo.mDirection == 1 && BluetoothShare.isStatusSuccess(transInfo.mStatus)) {
                BluetoothOppUtility.openReceivedFile(context, transInfo.mFileName, transInfo.mFileType, transInfo.mTimeStamp, uri2);
                BluetoothOppUtility.updateVisibilityToHidden(context, uri2);
            } else {
                Intent in2 = new Intent(context, (Class<?>) BluetoothOppTransferActivity.class);
                in2.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                in2.setDataAndNormalize(uri2);
                context.startActivity(in2);
            }
            NotificationManager notMgr2 = (NotificationManager) context.getSystemService("notification");
            if (notMgr2 != null) {
                notMgr2.cancel((int) ContentUris.parseId(intent.getData()));
                return;
            }
            return;
        }
        if (action.equals(Constants.ACTION_OPEN_OUTBOUND_TRANSFER)) {
            Intent in3 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            in3.setFlags(335544320);
            in3.putExtra(BluetoothShare.DIRECTION, 0);
            context.startActivity(in3);
            return;
        }
        if (action.equals(Constants.ACTION_OPEN_INBOUND_TRANSFER)) {
            Intent in4 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            in4.setFlags(335544320);
            in4.putExtra(BluetoothShare.DIRECTION, 1);
            context.startActivity(in4);
            return;
        }
        if (action.equals(Constants.ACTION_OPEN_RECEIVED_FILES)) {
            Intent in5 = new Intent(context, (Class<?>) BluetoothOppTransferHistory.class);
            in5.setFlags(335544320);
            in5.putExtra(BluetoothShare.DIRECTION, 1);
            in5.putExtra(Constants.EXTRA_SHOW_ALL_FILES, true);
            context.startActivity(in5);
            return;
        }
        if (action.equals(Constants.ACTION_HIDE)) {
            Cursor cursor = context.getContentResolver().query(intent.getData(), null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int statusColumn = cursor.getColumnIndexOrThrow(BluetoothShare.STATUS);
                    cursor.getInt(statusColumn);
                    int visibilityColumn = cursor.getColumnIndexOrThrow(BluetoothShare.VISIBILITY);
                    int visibility = cursor.getInt(visibilityColumn);
                    int userConfirmationColumn = cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION);
                    int userConfirmation = cursor.getInt(userConfirmationColumn);
                    if (userConfirmation == 0 && visibility == 0) {
                        ContentValues values = new ContentValues();
                        values.put(BluetoothShare.VISIBILITY, (Integer) 1);
                        context.getContentResolver().update(intent.getData(), values, null, null);
                    }
                }
                cursor.close();
                return;
            }
            return;
        }
        if (action.equals(Constants.ACTION_COMPLETE_HIDE)) {
            ContentValues updateValues = new ContentValues();
            updateValues.put(BluetoothShare.VISIBILITY, (Integer) 1);
            context.getContentResolver().update(BluetoothShare.CONTENT_URI, updateValues, "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5')", null);
            return;
        }
        if (action.equals(BluetoothShare.TRANSFER_COMPLETED_ACTION)) {
            String toastMsg2 = null;
            new BluetoothOppTransferInfo();
            BluetoothOppTransferInfo transInfo2 = BluetoothOppUtility.queryRecord(context, intent.getData());
            if (transInfo2 == null) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            if (transInfo2.mHandoverInitiated) {
                Intent handoverIntent = new Intent(Constants.ACTION_BT_OPP_TRANSFER_DONE);
                if (transInfo2.mDirection == 1) {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION, 0);
                } else {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION, 1);
                }
                handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_ID, transInfo2.mID);
                handoverIntent.putExtra(Constants.EXTRA_BT_OPP_ADDRESS, transInfo2.mDestAddr);
                if (BluetoothShare.isStatusSuccess(transInfo2.mStatus)) {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_STATUS, 0);
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_URI, transInfo2.mFileName);
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_MIMETYPE, transInfo2.mFileType);
                } else {
                    handoverIntent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_STATUS, 1);
                }
                context.sendBroadcast(handoverIntent, Constants.HANDOVER_STATUS_PERMISSION);
                return;
            }
            if (BluetoothShare.isStatusSuccess(transInfo2.mStatus)) {
                if (transInfo2.mDirection == 0) {
                    toastMsg2 = context.getString(R.string.notification_sent, transInfo2.mFileName);
                } else if (transInfo2.mDirection == 1) {
                    toastMsg2 = context.getString(R.string.notification_received, transInfo2.mFileName);
                }
            } else if (BluetoothShare.isStatusError(transInfo2.mStatus)) {
                if (transInfo2.mDirection == 0) {
                    toastMsg2 = context.getString(R.string.notification_sent_fail, transInfo2.mFileName);
                } else if (transInfo2.mDirection == 1) {
                    toastMsg2 = context.getString(R.string.download_fail_line1);
                }
            }
            if (toastMsg2 != null) {
                Toast.makeText(context, toastMsg2, 0).show();
            }
        }
    }
}
