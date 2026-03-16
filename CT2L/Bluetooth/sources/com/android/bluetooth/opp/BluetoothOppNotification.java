package com.android.bluetooth.opp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import com.android.bluetooth.R;
import java.util.HashMap;

class BluetoothOppNotification {
    private static final int NOTIFICATION_ID_INBOUND = -1000006;
    private static final int NOTIFICATION_ID_OUTBOUND = -1000005;
    private static final int NOTIFY = 0;
    private static final String TAG = "BluetoothOppNotification";
    private static final boolean V = false;
    static final String WHERE_COMPLETED = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5')";
    private static final String WHERE_COMPLETED_INBOUND = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5') AND (direction == 1)";
    private static final String WHERE_COMPLETED_OUTBOUND = "status >= '200' AND (visibility IS NULL OR visibility == '0') AND (confirm != '5') AND (direction == 0)";
    static final String WHERE_CONFIRM_PENDING = "confirm == '0' AND (visibility IS NULL OR visibility == '0')";
    static final String WHERE_RUNNING = "(status == '192') AND (visibility IS NULL OR visibility == '0') AND (confirm == '1' OR confirm == '2' OR confirm == '5')";
    static final String confirm = "(confirm == '1' OR confirm == '2' OR confirm == '5')";
    static final String not_through_handover = "(confirm != '5')";
    static final String status = "(status == '192')";
    static final String visible = "(visibility IS NULL OR visibility == '0')";
    private Context mContext;
    public NotificationManager mNotificationMgr;
    private NotificationUpdateThread mUpdateNotificationThread;
    private int mPendingUpdate = 0;
    private boolean mUpdateCompleteNotification = true;
    private int mActiveNotificationId = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (BluetoothOppNotification.this) {
                        if (BluetoothOppNotification.this.mPendingUpdate <= 0 || BluetoothOppNotification.this.mUpdateNotificationThread != null) {
                            if (BluetoothOppNotification.this.mPendingUpdate > 0) {
                                BluetoothOppNotification.this.mHandler.sendMessageDelayed(BluetoothOppNotification.this.mHandler.obtainMessage(0), 1000L);
                            }
                        } else {
                            BluetoothOppNotification.this.mUpdateNotificationThread = BluetoothOppNotification.this.new NotificationUpdateThread();
                            BluetoothOppNotification.this.mUpdateNotificationThread.start();
                            BluetoothOppNotification.this.mHandler.sendMessageDelayed(BluetoothOppNotification.this.mHandler.obtainMessage(0), 1000L);
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private HashMap<String, NotificationItem> mNotifications = new HashMap<>();

    static class NotificationItem {
        String description;
        String destination;
        int direction;
        int id;
        int totalCurrent = 0;
        int totalTotal = 0;
        long timeStamp = 0;
        boolean handoverInitiated = BluetoothOppNotification.V;

        NotificationItem() {
        }
    }

    BluetoothOppNotification(Context ctx) {
        this.mContext = ctx;
        this.mNotificationMgr = (NotificationManager) this.mContext.getSystemService("notification");
    }

    public void updateNotification() {
        synchronized (this) {
            this.mPendingUpdate++;
            if (this.mPendingUpdate <= 1) {
                if (!this.mHandler.hasMessages(0)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
                }
            }
        }
    }

    private class NotificationUpdateThread extends Thread {
        public NotificationUpdateThread() {
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
        float progress;
        Cursor cursor = this.mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null, WHERE_RUNNING, null, "_id");
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                this.mUpdateCompleteNotification = V;
            } else {
                this.mUpdateCompleteNotification = true;
            }
            int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");
            int directionIndex = cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION);
            int idIndex = cursor.getColumnIndexOrThrow("_id");
            int totalBytesIndex = cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES);
            int currentBytesIndex = cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES);
            int dataIndex = cursor.getColumnIndexOrThrow(BluetoothShare._DATA);
            int filenameHintIndex = cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT);
            int confirmIndex = cursor.getColumnIndexOrThrow(BluetoothShare.USER_CONFIRMATION);
            int destinationIndex = cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION);
            this.mNotifications.clear();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long timeStamp = cursor.getLong(timestampIndex);
                int dir = cursor.getInt(directionIndex);
                int id = cursor.getInt(idIndex);
                int total = cursor.getInt(totalBytesIndex);
                int current = cursor.getInt(currentBytesIndex);
                int confirmation = cursor.getInt(confirmIndex);
                String destination = cursor.getString(destinationIndex);
                String fileName = cursor.getString(dataIndex);
                if (fileName == null) {
                    fileName = cursor.getString(filenameHintIndex);
                }
                if (fileName == null) {
                    fileName = this.mContext.getString(R.string.unknown_file);
                }
                String batchID = Long.toString(timeStamp);
                if (!this.mNotifications.containsKey(batchID)) {
                    NotificationItem item = new NotificationItem();
                    item.timeStamp = timeStamp;
                    item.id = id;
                    item.direction = dir;
                    if (item.direction == 0) {
                        item.description = this.mContext.getString(R.string.notification_sending, fileName);
                    } else if (item.direction == 1) {
                        item.description = this.mContext.getString(R.string.notification_receiving, fileName);
                    }
                    item.totalCurrent = current;
                    item.totalTotal = total;
                    item.handoverInitiated = confirmation == 5 ? true : V;
                    item.destination = destination;
                    this.mNotifications.put(batchID, item);
                }
                cursor.moveToNext();
            }
            cursor.close();
            for (NotificationItem item2 : this.mNotifications.values()) {
                if (item2.handoverInitiated) {
                    if (item2.totalTotal == -1) {
                        progress = -1.0f;
                    } else {
                        progress = item2.totalCurrent / item2.totalTotal;
                    }
                    Intent intent = new Intent(Constants.ACTION_BT_OPP_TRANSFER_PROGRESS);
                    if (item2.direction == 1) {
                        intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION, 0);
                    } else {
                        intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_DIRECTION, 1);
                    }
                    intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_ID, item2.id);
                    intent.putExtra(Constants.EXTRA_BT_OPP_TRANSFER_PROGRESS, progress);
                    intent.putExtra(Constants.EXTRA_BT_OPP_ADDRESS, item2.destination);
                    this.mContext.sendBroadcast(intent, Constants.HANDOVER_STATUS_PERMISSION);
                } else {
                    Notification.Builder b = new Notification.Builder(this.mContext);
                    b.setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
                    b.setContentTitle(item2.description);
                    b.setContentInfo(BluetoothOppUtility.formatProgressText(item2.totalTotal, item2.totalCurrent));
                    b.setProgress(item2.totalTotal, item2.totalCurrent, item2.totalTotal == -1 ? true : V);
                    b.setWhen(item2.timeStamp);
                    if (item2.direction == 0) {
                        b.setSmallIcon(android.R.drawable.stat_sys_upload);
                    } else if (item2.direction == 1) {
                        b.setSmallIcon(android.R.drawable.stat_sys_download);
                    }
                    b.setOngoing(true);
                    Intent intent2 = new Intent(Constants.ACTION_LIST);
                    intent2.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                    intent2.setDataAndNormalize(Uri.parse(BluetoothShare.CONTENT_URI + "/" + item2.id));
                    b.setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, intent2, 0));
                    this.mNotificationMgr.notify(item2.id, b.getNotification());
                    this.mActiveNotificationId = item2.id;
                }
            }
        }
    }

    private void updateCompletedNotification() {
        long timeStamp = 0;
        int outboundSuccNumber = 0;
        int outboundFailNumber = 0;
        int inboundSuccNumber = 0;
        int inboundFailNumber = 0;
        if (this.mUpdateCompleteNotification) {
            if (this.mNotificationMgr != null && this.mActiveNotificationId != 0) {
                this.mNotificationMgr.cancel(this.mActiveNotificationId);
            }
            Cursor cursor = this.mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null, WHERE_COMPLETED_OUTBOUND, null, "timestamp DESC");
            if (cursor != null) {
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");
                int statusIndex = cursor.getColumnIndexOrThrow(BluetoothShare.STATUS);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    if (cursor.isFirst()) {
                        timeStamp = cursor.getLong(timestampIndex);
                    }
                    int status2 = cursor.getInt(statusIndex);
                    if (BluetoothShare.isStatusError(status2)) {
                        outboundFailNumber++;
                    } else {
                        outboundSuccNumber++;
                    }
                    cursor.moveToNext();
                }
                cursor.close();
                int outboundNum = outboundSuccNumber + outboundFailNumber;
                if (outboundNum > 0) {
                    Notification outNoti = new Notification();
                    outNoti.icon = android.R.drawable.stat_sys_upload_done;
                    String title = this.mContext.getString(R.string.outbound_noti_title);
                    String caption = this.mContext.getString(R.string.noti_caption, Integer.valueOf(outboundSuccNumber), Integer.valueOf(outboundFailNumber));
                    Intent intent = new Intent(Constants.ACTION_OPEN_OUTBOUND_TRANSFER);
                    intent.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                    outNoti.color = this.mContext.getResources().getColor(android.R.color.system_accent3_600);
                    outNoti.setLatestEventInfo(this.mContext, title, caption, PendingIntent.getBroadcast(this.mContext, 0, intent, 0));
                    Intent intent2 = new Intent(Constants.ACTION_COMPLETE_HIDE);
                    intent2.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                    outNoti.deleteIntent = PendingIntent.getBroadcast(this.mContext, 0, intent2, 0);
                    outNoti.when = timeStamp;
                    this.mNotificationMgr.notify(NOTIFICATION_ID_OUTBOUND, outNoti);
                } else if (this.mNotificationMgr != null) {
                    this.mNotificationMgr.cancel(NOTIFICATION_ID_OUTBOUND);
                }
                Cursor cursor2 = this.mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null, WHERE_COMPLETED_INBOUND, null, "timestamp DESC");
                if (cursor2 != null) {
                    cursor2.moveToFirst();
                    while (!cursor2.isAfterLast()) {
                        if (cursor2.isFirst()) {
                            timeStamp = cursor2.getLong(timestampIndex);
                        }
                        int status3 = cursor2.getInt(statusIndex);
                        if (BluetoothShare.isStatusError(status3)) {
                            inboundFailNumber++;
                        } else {
                            inboundSuccNumber++;
                        }
                        cursor2.moveToNext();
                    }
                    cursor2.close();
                    int inboundNum = inboundSuccNumber + inboundFailNumber;
                    if (inboundNum > 0) {
                        Notification inNoti = new Notification();
                        inNoti.icon = android.R.drawable.stat_sys_download_done;
                        String title2 = this.mContext.getString(R.string.inbound_noti_title);
                        String caption2 = this.mContext.getString(R.string.noti_caption, Integer.valueOf(inboundSuccNumber), Integer.valueOf(inboundFailNumber));
                        Intent intent3 = new Intent(Constants.ACTION_OPEN_INBOUND_TRANSFER);
                        intent3.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                        inNoti.color = this.mContext.getResources().getColor(android.R.color.system_accent3_600);
                        inNoti.setLatestEventInfo(this.mContext, title2, caption2, PendingIntent.getBroadcast(this.mContext, 0, intent3, 0));
                        Intent intent4 = new Intent(Constants.ACTION_COMPLETE_HIDE);
                        intent4.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                        inNoti.deleteIntent = PendingIntent.getBroadcast(this.mContext, 0, intent4, 0);
                        inNoti.when = timeStamp;
                        this.mNotificationMgr.notify(NOTIFICATION_ID_INBOUND, inNoti);
                        return;
                    }
                    if (this.mNotificationMgr != null) {
                        this.mNotificationMgr.cancel(NOTIFICATION_ID_INBOUND);
                    }
                }
            }
        }
    }

    private void updateIncomingFileConfirmNotification() {
        Cursor cursor = this.mContext.getContentResolver().query(BluetoothShare.CONTENT_URI, null, WHERE_CONFIRM_PENDING, null, "_id");
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CharSequence title = this.mContext.getText(R.string.incoming_file_confirm_Notification_title);
                CharSequence caption = this.mContext.getText(R.string.incoming_file_confirm_Notification_caption);
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                long timeStamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + id);
                Notification n = new Notification();
                n.icon = R.drawable.bt_incomming_file_notification;
                n.flags |= 8;
                n.flags |= 2;
                n.defaults = 1;
                n.tickerText = title;
                Intent intent = new Intent(Constants.ACTION_INCOMING_FILE_CONFIRM);
                intent.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                intent.setDataAndNormalize(contentUri);
                n.when = timeStamp;
                n.color = this.mContext.getResources().getColor(android.R.color.system_accent3_600);
                n.setLatestEventInfo(this.mContext, title, caption, PendingIntent.getBroadcast(this.mContext, 0, intent, 0));
                Intent intent2 = new Intent(Constants.ACTION_HIDE);
                intent2.setClassName("com.android.bluetooth", BluetoothOppReceiver.class.getName());
                intent2.setDataAndNormalize(contentUri);
                n.deleteIntent = PendingIntent.getBroadcast(this.mContext, 0, intent2, 0);
                this.mNotificationMgr.notify(id, n);
                cursor.moveToNext();
            }
            cursor.close();
        }
    }
}
