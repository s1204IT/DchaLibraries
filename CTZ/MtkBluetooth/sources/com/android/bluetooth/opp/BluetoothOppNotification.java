package com.android.bluetooth.opp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.format.Formatter;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.HashMap;

class BluetoothOppNotification {
    static final String CONFIRM = "(confirm == '1' OR confirm == '2' OR confirm == '5')";
    private static final int NOTIFICATION_ID_INBOUND_COMPLETE = -1000006;
    private static final int NOTIFICATION_ID_OUTBOUND_COMPLETE = -1000005;
    public static final int NOTIFICATION_ID_PROGRESS = -1000004;
    private static final int NOTIFY = 0;
    static final String NOT_THROUGH_HANDOVER = "(confirm != '5')";
    private static final String OPP_NOTIFICATION_CHANNEL = "opp_notification_channel";
    static final String STATUS = "(status == '192')";
    private static final String TAG = "BluetoothOppNotification";
    private static final boolean V = Constants.VERBOSE;
    static final String VISIBLE = "(visibility IS NULL OR visibility == '0')";
    static final String WHERE_COMPLETED = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5')";
    private static final String WHERE_COMPLETED_INBOUND = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5') AND (direction == 1)";
    private static final String WHERE_COMPLETED_OUTBOUND = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5') AND (direction == 0)";
    static final String WHERE_CONFIRM_PENDING = "confirm == '0' AND (visibility IS NULL OR visibility == '0')";
    static final String WHERE_RUNNING = "(status == '192') AND (visibility IS NULL OR visibility == '0') AND (confirm == '1' OR confirm == '2' OR confirm == '5')";
    private ContentResolver mContentResolver;
    private Context mContext;
    private NotificationChannel mNotificationChannel;
    public NotificationManager mNotificationMgr;
    private HashMap<String, NotificationItem> mNotifications;
    private NotificationUpdateThread mUpdateNotificationThread;
    private int mPendingUpdate = 0;
    private boolean mUpdateCompleteNotification = true;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                synchronized (BluetoothOppNotification.this) {
                    if (BluetoothOppNotification.this.mPendingUpdate <= 0 || BluetoothOppNotification.this.mUpdateNotificationThread != null) {
                        if (BluetoothOppNotification.this.mPendingUpdate > 0) {
                            if (BluetoothOppNotification.V) {
                                Log.v(BluetoothOppNotification.TAG, "previous thread is not finished yet");
                            }
                            BluetoothOppNotification.this.mHandler.sendMessageDelayed(BluetoothOppNotification.this.mHandler.obtainMessage(0), 1000L);
                        }
                    } else {
                        if (BluetoothOppNotification.V) {
                            Log.v(BluetoothOppNotification.TAG, "new notify threadi!");
                        }
                        BluetoothOppNotification.this.mUpdateNotificationThread = BluetoothOppNotification.this.new NotificationUpdateThread();
                        BluetoothOppNotification.this.mUpdateNotificationThread.start();
                        if (BluetoothOppNotification.V) {
                            Log.v(BluetoothOppNotification.TAG, "send delay message");
                        }
                        BluetoothOppNotification.this.mHandler.sendMessageDelayed(BluetoothOppNotification.this.mHandler.obtainMessage(0), 1000L);
                    }
                }
            }
        }
    };

    static class NotificationItem {
        public String description;
        public String destination;
        public int direction;
        public int id;
        public long totalCurrent = 0;
        public long totalTotal = 0;
        public long timeStamp = 0;
        public boolean handoverInitiated = false;

        NotificationItem() {
        }
    }

    BluetoothOppNotification(Context context) {
        this.mContentResolver = null;
        this.mContext = context;
        this.mNotificationMgr = (NotificationManager) this.mContext.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION);
        this.mNotificationChannel = new NotificationChannel(OPP_NOTIFICATION_CHANNEL, this.mContext.getString(R.string.opp_notification_group), 4);
        this.mNotificationMgr.createNotificationChannel(this.mNotificationChannel);
        this.mNotifications = new HashMap<>();
        this.mContentResolver = this.mContext.getContentResolver();
    }

    public void updateNotification() {
        synchronized (this) {
            this.mPendingUpdate++;
            if (this.mPendingUpdate > 1) {
                if (V) {
                    Log.v(TAG, "update too frequent, put in queue");
                }
            } else {
                if (!this.mHandler.hasMessages(0)) {
                    if (V) {
                        Log.v(TAG, "send message");
                    }
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
                }
            }
        }
    }

    private class NotificationUpdateThread extends Thread {
        NotificationUpdateThread() {
            super("Notification Update Thread");
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            synchronized (BluetoothOppNotification.this) {
                if (BluetoothOppNotification.this.mUpdateNotificationThread == this) {
                    BluetoothOppNotification.this.mPendingUpdate = 0;
                } else {
                    throw new IllegalStateException("multiple UpdateThreads in BluetoothOppNotification");
                }
            }
            BluetoothOppNotification.this.updateActiveNotification();
            BluetoothOppNotification.this.updateCompletedNotification();
            BluetoothOppNotification.this.updateIncomingFileConfirmNotification();
            synchronized (BluetoothOppNotification.this) {
                BluetoothOppNotification.this.mUpdateNotificationThread = null;
            }
        }
    }

    private void updateActiveNotification() {
        Cursor cursorQuery;
        float f;
        int i;
        int i2;
        if (V) {
            Log.v(TAG, "updateActiveNotification ++");
        }
        try {
            cursorQuery = this.mContentResolver.query(BluetoothShare.CONTENT_URI, null, WHERE_RUNNING, null, "_id");
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
            cursorQuery = null;
        }
        if (cursorQuery == null) {
            return;
        }
        if (cursorQuery.getCount() > 0) {
            this.mUpdateCompleteNotification = false;
        } else {
            this.mUpdateCompleteNotification = true;
        }
        if (V) {
            Log.v(TAG, "mUpdateCompleteNotification = " + this.mUpdateCompleteNotification);
        }
        int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("timestamp");
        int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow(BluetoothShare.DIRECTION);
        int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow4 = cursorQuery.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES);
        int columnIndexOrThrow5 = cursorQuery.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES);
        int columnIndexOrThrow6 = cursorQuery.getColumnIndexOrThrow(BluetoothShare._DATA);
        int columnIndexOrThrow7 = cursorQuery.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT);
        int columnIndexOrThrow8 = cursorQuery.getColumnIndexOrThrow("confirm");
        int columnIndexOrThrow9 = cursorQuery.getColumnIndexOrThrow(BluetoothShare.DESTINATION);
        this.mNotifications.clear();
        cursorQuery.moveToFirst();
        while (!cursorQuery.isAfterLast()) {
            long j = cursorQuery.getLong(columnIndexOrThrow);
            int i3 = cursorQuery.getInt(columnIndexOrThrow2);
            int i4 = cursorQuery.getInt(columnIndexOrThrow3);
            int i5 = columnIndexOrThrow2;
            long j2 = cursorQuery.getLong(columnIndexOrThrow4);
            int i6 = columnIndexOrThrow3;
            int i7 = columnIndexOrThrow4;
            long j3 = cursorQuery.getLong(columnIndexOrThrow5);
            int i8 = columnIndexOrThrow;
            int i9 = cursorQuery.getInt(columnIndexOrThrow8);
            int i10 = columnIndexOrThrow5;
            String string = cursorQuery.getString(columnIndexOrThrow9);
            String string2 = cursorQuery.getString(columnIndexOrThrow6);
            if (string2 == null) {
                string2 = cursorQuery.getString(columnIndexOrThrow7);
            }
            if (string2 == null) {
                i = columnIndexOrThrow6;
                i2 = columnIndexOrThrow7;
                string2 = this.mContext.getString(R.string.unknown_file);
            } else {
                i = columnIndexOrThrow6;
                i2 = columnIndexOrThrow7;
            }
            String string3 = Long.toString(j);
            if (!this.mNotifications.containsKey(string3)) {
                NotificationItem notificationItem = new NotificationItem();
                notificationItem.timeStamp = j;
                notificationItem.id = i4;
                notificationItem.direction = i3;
                if (notificationItem.direction == 0) {
                    notificationItem.description = this.mContext.getString(R.string.notification_sending, string2);
                } else if (notificationItem.direction == 1) {
                    notificationItem.description = this.mContext.getString(R.string.notification_receiving, string2);
                } else if (V) {
                    Log.v(TAG, "mDirection ERROR!");
                }
                notificationItem.totalCurrent = j3;
                notificationItem.totalTotal = j2;
                notificationItem.handoverInitiated = i9 == 5;
                notificationItem.destination = string;
                this.mNotifications.put(string3, notificationItem);
                if (V) {
                    Log.v(TAG, "ID=" + notificationItem.id + "; batchID=" + string3 + "; totoalCurrent" + notificationItem.totalCurrent + "; totalTotal=" + notificationItem.totalTotal);
                }
            }
            cursorQuery.moveToNext();
            columnIndexOrThrow2 = i5;
            columnIndexOrThrow3 = i6;
            columnIndexOrThrow4 = i7;
            columnIndexOrThrow = i8;
            columnIndexOrThrow5 = i10;
            columnIndexOrThrow6 = i;
            columnIndexOrThrow7 = i2;
        }
        cursorQuery.close();
        for (NotificationItem notificationItem2 : this.mNotifications.values()) {
            if (notificationItem2.handoverInitiated) {
                if (notificationItem2.totalTotal == -1) {
                    f = -1.0f;
                } else {
                    f = notificationItem2.totalCurrent / notificationItem2.totalTotal;
                }
                Intent intent = new Intent("android.nfc.handover.intent.action.TRANSFER_PROGRESS");
                if (notificationItem2.direction == 1) {
                    intent.putExtra("android.nfc.handover.intent.extra.TRANSFER_DIRECTION", 0);
                } else {
                    intent.putExtra("android.nfc.handover.intent.extra.TRANSFER_DIRECTION", 1);
                }
                intent.putExtra("android.nfc.handover.intent.extra.TRANSFER_ID", notificationItem2.id);
                intent.putExtra("android.nfc.handover.intent.extra.TRANSFER_PROGRESS", f);
                intent.putExtra("android.nfc.handover.intent.extra.ADDRESS", notificationItem2.destination);
                this.mContext.sendBroadcast(intent, "android.permission.NFC_HANDOVER_STATUS");
            } else {
                Notification.Builder builder = new Notification.Builder(this.mContext, OPP_NOTIFICATION_CHANNEL);
                builder.setOnlyAlertOnce(true);
                builder.setColor(this.mContext.getResources().getColor(android.R.color.car_colorPrimary, this.mContext.getTheme()));
                builder.setContentTitle(notificationItem2.description);
                builder.setSubText(BluetoothOppUtility.formatProgressText(notificationItem2.totalTotal, notificationItem2.totalCurrent));
                if (notificationItem2.totalTotal != 0) {
                    if (V) {
                        Log.v(TAG, "mCurrentBytes: " + notificationItem2.totalCurrent + " mTotalBytes: " + notificationItem2.totalTotal + " (" + ((int) ((notificationItem2.totalCurrent * 100) / notificationItem2.totalTotal)) + " %)");
                    }
                    builder.setProgress(100, (int) ((notificationItem2.totalCurrent * 100) / notificationItem2.totalTotal), notificationItem2.totalTotal == -1);
                } else {
                    builder.setProgress(100, 100, notificationItem2.totalTotal == -1);
                }
                builder.setWhen(notificationItem2.timeStamp);
                if (notificationItem2.direction == 0) {
                    builder.setSmallIcon(android.R.drawable.stat_sys_upload);
                } else if (notificationItem2.direction == 1) {
                    builder.setSmallIcon(android.R.drawable.stat_sys_download);
                } else if (V) {
                    Log.v(TAG, "mDirection ERROR!");
                }
                builder.setOngoing(true);
                builder.setLocalOnly(true);
                Intent intent2 = new Intent("android.btopp.intent.action.LIST");
                intent2.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                intent2.setDataAndNormalize(Uri.parse(BluetoothShare.CONTENT_URI + "/" + notificationItem2.id));
                builder.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent2, 0));
                this.mNotificationMgr.notify(NOTIFICATION_ID_PROGRESS, builder.build());
            }
        }
    }

    private void updateCompletedNotification() {
        Cursor cursorQuery;
        Cursor cursorQuery2;
        try {
            cursorQuery = this.mContentResolver.query(BluetoothShare.CONTENT_URI, null, WHERE_COMPLETED_OUTBOUND, null, "timestamp DESC");
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
            cursorQuery = null;
        }
        Cursor cursor = cursorQuery;
        if (cursor == null) {
            return;
        }
        if (V) {
            Log.v(TAG, "Completed counter = " + cursor.getCount());
        }
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("timestamp");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("status");
        cursor.moveToFirst();
        int i = 0;
        long j = 0;
        int i2 = 0;
        while (!cursor.isAfterLast()) {
            if (cursor.isFirst()) {
                j = cursor.getLong(columnIndexOrThrow);
            }
            if (BluetoothShare.isStatusError(cursor.getInt(columnIndexOrThrow2))) {
                i2++;
            } else {
                i++;
            }
            cursor.moveToNext();
        }
        if (V) {
            Log.v(TAG, "outbound: succ-" + i + "  fail-" + i2);
        }
        cursor.close();
        if (i + i2 <= 0) {
            if (this.mNotificationMgr != null) {
                this.mNotificationMgr.cancel(NOTIFICATION_ID_OUTBOUND_COMPLETE);
                if (V) {
                    Log.v(TAG, "outbound notification was removed.");
                }
            }
        } else {
            this.mNotificationMgr.notify(NOTIFICATION_ID_OUTBOUND_COMPLETE, new Notification.Builder(this.mContext, OPP_NOTIFICATION_CHANNEL).setOnlyAlertOnce(true).setContentTitle(this.mContext.getString(R.string.outbound_noti_title)).setContentText(this.mContext.getResources().getQuantityString(R.plurals.noti_caption_success, i, Integer.valueOf(i), this.mContext.getResources().getQuantityString(R.plurals.noti_caption_unsuccessful, i2, Integer.valueOf(i2)))).setSmallIcon(android.R.drawable.stat_sys_upload_done).setColor(this.mContext.getResources().getColor(android.R.color.car_colorPrimary, this.mContext.getTheme())).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent("android.btopp.intent.action.OPEN_OUTBOUND").setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName()), 0)).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent("android.btopp.intent.action.HIDE_COMPLETE").setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName()), 0)).setWhen(j).setLocalOnly(true).build());
        }
        try {
            cursorQuery2 = this.mContentResolver.query(BluetoothShare.CONTENT_URI, null, WHERE_COMPLETED_INBOUND, null, "timestamp DESC");
        } catch (Exception e2) {
            Log.e(TAG, "SQLite exception occur : " + e2.toString());
            cursorQuery2 = cursor;
        }
        if (cursorQuery2 == null) {
            return;
        }
        cursorQuery2.moveToFirst();
        int i3 = 0;
        int i4 = 0;
        while (!cursorQuery2.isAfterLast()) {
            if (cursorQuery2.isFirst()) {
                j = cursorQuery2.getLong(columnIndexOrThrow);
            }
            if (BluetoothShare.isStatusError(cursorQuery2.getInt(columnIndexOrThrow2))) {
                i4++;
            } else {
                i3++;
            }
            cursorQuery2.moveToNext();
        }
        if (V) {
            Log.v(TAG, "inbound: succ-" + i3 + "  fail-" + i4);
        }
        cursorQuery2.close();
        if (i3 + i4 > 0) {
            this.mNotificationMgr.notify(NOTIFICATION_ID_INBOUND_COMPLETE, new Notification.Builder(this.mContext, OPP_NOTIFICATION_CHANNEL).setOnlyAlertOnce(true).setContentTitle(this.mContext.getString(R.string.inbound_noti_title)).setContentText(this.mContext.getResources().getQuantityString(R.plurals.noti_caption_success, i3, Integer.valueOf(i3), this.mContext.getResources().getQuantityString(R.plurals.noti_caption_unsuccessful, i4, Integer.valueOf(i4)))).setSmallIcon(android.R.drawable.stat_sys_download_done).setColor(this.mContext.getResources().getColor(android.R.color.car_colorPrimary, this.mContext.getTheme())).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent("android.btopp.intent.action.OPEN_INBOUND").setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName()), 0)).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent("android.btopp.intent.action.HIDE_COMPLETE").setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName()), 0)).setWhen(j).setLocalOnly(true).build());
        } else if (this.mNotificationMgr != null) {
            this.mNotificationMgr.cancel(NOTIFICATION_ID_INBOUND_COMPLETE);
            if (V) {
                Log.v(TAG, "inbound notification was removed.");
            }
        }
    }

    private void updateIncomingFileConfirmNotification() {
        if (V) {
            Log.v(TAG, "updateIncomingFileConfirmNotification ++");
        }
        Cursor cursorQuery = null;
        try {
            cursorQuery = this.mContentResolver.query(BluetoothShare.CONTENT_URI, null, WHERE_CONFIRM_PENDING, null, "_id");
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
        }
        if (cursorQuery == null) {
            return;
        }
        cursorQuery.moveToFirst();
        while (!cursorQuery.isAfterLast()) {
            BluetoothOppTransferInfo bluetoothOppTransferInfo = new BluetoothOppTransferInfo();
            BluetoothOppUtility.fillRecord(this.mContext, cursorQuery, bluetoothOppTransferInfo);
            Intent className = new Intent().setDataAndNormalize(Uri.parse(BluetoothShare.CONTENT_URI + "/" + bluetoothOppTransferInfo.mID)).setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
            this.mNotificationMgr.notify(NOTIFICATION_ID_PROGRESS, new Notification.Builder(this.mContext, OPP_NOTIFICATION_CHANNEL).setOnlyAlertOnce(true).setOngoing(true).setWhen(bluetoothOppTransferInfo.mTimeStamp.longValue()).addAction(new Notification.Action.Builder(R.drawable.ic_decline, this.mContext.getText(R.string.incoming_file_confirm_cancel), PendingIntent.getBroadcast(this.mContext, 0, new Intent(className).setAction("android.btopp.intent.action.DECLINE"), 0)).build()).addAction(new Notification.Action.Builder(R.drawable.ic_accept, this.mContext.getText(R.string.incoming_file_confirm_ok), PendingIntent.getBroadcast(this.mContext, 0, new Intent(className).setAction("android.btopp.intent.action.ACCEPT"), 0)).build()).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(className).setAction("android.btopp.intent.action.CONFIRM"), 0)).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(className).setAction("android.btopp.intent.action.HIDE"), 0)).setColor(this.mContext.getResources().getColor(android.R.color.car_colorPrimary, this.mContext.getTheme())).setContentTitle(this.mContext.getText(R.string.incoming_file_confirm_Notification_title)).setContentText(bluetoothOppTransferInfo.mFileName).setStyle(new Notification.BigTextStyle().bigText(this.mContext.getString(R.string.incoming_file_confirm_Notification_content, bluetoothOppTransferInfo.mDeviceName, bluetoothOppTransferInfo.mFileName))).setContentInfo(Formatter.formatFileSize(this.mContext, bluetoothOppTransferInfo.mTotalBytes)).setSmallIcon(R.drawable.bt_incomming_file_notification).setLocalOnly(true).build());
            cursorQuery.moveToNext();
        }
        cursorQuery.close();
    }

    void cancelNotifications() {
        if (V) {
            Log.v(TAG, "cancelNotifications ");
        }
        this.mHandler.removeCallbacksAndMessages(null);
        this.mNotificationMgr.cancelAll();
    }
}
