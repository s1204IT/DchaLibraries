package com.android.server.devicepolicy;

import android.R;
import android.annotation.IntDef;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.BenesseExtension;
import android.os.UserHandle;
import com.android.server.notification.ZenModeHelper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class RemoteBugreportUtils {
    static final String BUGREPORT_MIMETYPE = "application/vnd.android.bugreport";
    static final String CTL_STOP = "ctl.stop";
    static final int NOTIFICATION_ID = 678432343;
    static final String REMOTE_BUGREPORT_SERVICE = "bugreportremote";
    static final long REMOTE_BUGREPORT_TIMEOUT_MILLIS = 600000;

    @IntDef({ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS, ZenModeHelper.SUPPRESSED_EFFECT_CALLS, ZenModeHelper.SUPPRESSED_EFFECT_ALL})
    @Retention(RetentionPolicy.SOURCE)
    @interface RemoteBugreportNotificationType {
    }

    RemoteBugreportUtils() {
    }

    static Notification buildNotification(Context context, int type) {
        Intent dialogIntent = new Intent("android.settings.SHOW_REMOTE_BUGREPORT_DIALOG");
        dialogIntent.addFlags(268468224);
        dialogIntent.putExtra("android.app.extra.bugreport_notification_type", type);
        PendingIntent pendingDialogIntent = PendingIntent.getActivityAsUser(context, type, dialogIntent, 0, null, UserHandle.CURRENT);
        if (BenesseExtension.getDchaState() != 0) {
            pendingDialogIntent = null;
        }
        Notification.Builder builder = new Notification.Builder(context).setSmallIcon(R.drawable.list_selector_background_longpress_light).setOngoing(true).setLocalOnly(true).setPriority(1).setContentIntent(pendingDialogIntent).setColor(context.getColor(R.color.system_accent3_600));
        if (type == 2) {
            builder.setContentTitle(context.getString(R.string.factorytest_failed)).setProgress(0, 0, true);
        } else if (type == 1) {
            builder.setContentTitle(context.getString(R.string.factory_reset_message)).setProgress(0, 0, true);
        } else if (type == 3) {
            PendingIntent pendingIntentAccept = PendingIntent.getBroadcast(context, NOTIFICATION_ID, new Intent("com.android.server.action.BUGREPORT_SHARING_ACCEPTED"), 268435456);
            PendingIntent pendingIntentDecline = PendingIntent.getBroadcast(context, NOTIFICATION_ID, new Intent("com.android.server.action.BUGREPORT_SHARING_DECLINED"), 268435456);
            builder.addAction(new Notification.Action.Builder((Icon) null, context.getString(R.string.factorytest_reboot), pendingIntentDecline).build()).addAction(new Notification.Action.Builder((Icon) null, context.getString(R.string.factorytest_not_system), pendingIntentAccept).build()).setContentTitle(context.getString(R.string.factory_reset_warning)).setContentText(context.getString(R.string.factorytest_no_action)).setStyle(new Notification.BigTextStyle().bigText(context.getString(R.string.factorytest_no_action)));
        }
        return builder.build();
    }
}
