package com.android.mtp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

class ServiceIntentSender {
    private final Context mContext;

    ServiceIntentSender(Context context) {
        this.mContext = context;
        ((NotificationManager) context.getSystemService("notification")).createNotificationChannel(new NotificationChannel("device_notification_channel", context.getResources().getString(android.R.string.bg_user_sound_notification_message), 2));
    }

    @VisibleForTesting
    protected ServiceIntentSender() {
        this.mContext = null;
    }

    void sendUpdateNotificationIntent(MtpDeviceRecord[] mtpDeviceRecordArr) {
        Preconditions.checkNotNull(mtpDeviceRecordArr);
        Intent intent = new Intent("com.android.mtp.UPDATE_NOTIFICATION");
        intent.setComponent(new ComponentName(this.mContext, (Class<?>) MtpDocumentsService.class));
        if (mtpDeviceRecordArr.length != 0) {
            int[] iArr = new int[mtpDeviceRecordArr.length];
            Notification[] notificationArr = new Notification[mtpDeviceRecordArr.length];
            for (int i = 0; i < mtpDeviceRecordArr.length; i++) {
                iArr[i] = mtpDeviceRecordArr[i].deviceId;
                notificationArr[i] = createNotification(this.mContext, mtpDeviceRecordArr[i]);
            }
            intent.putExtra("deviceIds", iArr);
            intent.putExtra("deviceNotifications", notificationArr);
            this.mContext.startForegroundService(intent);
            return;
        }
        this.mContext.startService(intent);
    }

    private static Notification createNotification(Context context, MtpDeviceRecord mtpDeviceRecord) {
        return new Notification.Builder(context, "device_notification_channel").setLocalOnly(true).setContentTitle(context.getResources().getString(R.string.accessing_notification_title, mtpDeviceRecord.name)).setSmallIcon(android.R.drawable.pointer_spot_hover_vector).setCategory("sys").setFlag(32, true).build();
    }
}
