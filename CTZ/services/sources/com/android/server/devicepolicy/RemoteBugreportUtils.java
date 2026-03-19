package com.android.server.devicepolicy;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.BenesseExtension;
import android.os.UserHandle;
import com.android.internal.notification.SystemNotificationChannels;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class RemoteBugreportUtils {
    static final String BUGREPORT_MIMETYPE = "application/vnd.android.bugreport";
    static final String CTL_STOP = "ctl.stop";
    static final int NOTIFICATION_ID = 678432343;
    static final String REMOTE_BUGREPORT_SERVICE = "bugreportremote";
    static final long REMOTE_BUGREPORT_TIMEOUT_MILLIS = 600000;

    @Retention(RetentionPolicy.SOURCE)
    @interface RemoteBugreportNotificationType {
    }

    RemoteBugreportUtils() {
    }

    static Notification buildNotification(Context context, int i) {
        Intent intent = new Intent("android.settings.SHOW_REMOTE_BUGREPORT_DIALOG");
        intent.addFlags(268468224);
        intent.putExtra("android.app.extra.bugreport_notification_type", i);
        PendingIntent activityAsUser = PendingIntent.getActivityAsUser(context, i, intent, 0, null, UserHandle.CURRENT);
        if (BenesseExtension.getDchaState() != 0) {
            activityAsUser = null;
        }
        Notification.Builder color = new Notification.Builder(context, SystemNotificationChannels.DEVELOPER).setSmallIcon(R.drawable.pointer_hand_large_icon).setOngoing(true).setLocalOnly(true).setContentIntent(activityAsUser).setColor(context.getColor(R.color.car_colorPrimary));
        if (i == 2) {
            color.setContentTitle(context.getString(R.string.managed_profile_label_badge)).setProgress(0, 0, true);
        } else if (i == 1) {
            color.setContentTitle(context.getString(R.string.mediasize_na_legal)).setProgress(0, 0, true);
        } else if (i == 3) {
            color.addAction(new Notification.Action.Builder((Icon) null, context.getString(R.string.battery_saver_charged_notification_summary), PendingIntent.getBroadcast(context, NOTIFICATION_ID, new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED"), 268435456)).build()).addAction(new Notification.Action.Builder((Icon) null, context.getString(R.string.low_internal_storage_view_text_no_boot), PendingIntent.getBroadcast(context, NOTIFICATION_ID, new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED"), 268435456)).build()).setContentTitle(context.getString(R.string.low_memory)).setContentText(context.getString(R.string.low_internal_storage_view_title)).setStyle(new Notification.BigTextStyle().bigText(context.getString(R.string.low_internal_storage_view_title)));
        }
        return color.build();
    }
}
