package com.android.server.wm;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import com.android.server.pm.Settings;
import com.android.server.policy.IconUtilities;

class AlertWindowNotification {
    private static final String CHANNEL_PREFIX = "com.android.server.wm.AlertWindowNotification - ";
    private static final int NOTIFICATION_ID = 0;
    private static NotificationChannelGroup sChannelGroup;
    private static int sNextRequestCode = 0;
    private IconUtilities mIconUtilities;
    private final NotificationManager mNotificationManager;
    private String mNotificationTag;
    private final String mPackageName;
    private boolean mPosted;
    private final int mRequestCode;
    private final WindowManagerService mService;

    AlertWindowNotification(WindowManagerService windowManagerService, String str) {
        this.mService = windowManagerService;
        this.mPackageName = str;
        this.mNotificationManager = (NotificationManager) this.mService.mContext.getSystemService("notification");
        this.mNotificationTag = CHANNEL_PREFIX + this.mPackageName;
        int i = sNextRequestCode;
        sNextRequestCode = i + 1;
        this.mRequestCode = i;
        this.mIconUtilities = new IconUtilities(this.mService.mContext);
    }

    void post() {
        this.mService.mH.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.onPostNotification();
            }
        });
    }

    void cancel(final boolean z) {
        this.mService.mH.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.onCancelNotification(z);
            }
        });
    }

    private void onCancelNotification(boolean z) {
        if (!this.mPosted) {
            return;
        }
        this.mPosted = false;
        this.mNotificationManager.cancel(this.mNotificationTag, 0);
        if (z) {
            this.mNotificationManager.deleteNotificationChannel(this.mNotificationTag);
        }
    }

    private void onPostNotification() {
        Drawable applicationIcon;
        if (this.mPosted) {
            return;
        }
        this.mPosted = true;
        Context context = this.mService.mContext;
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = getApplicationInfo(packageManager, this.mPackageName);
        String string = applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo).toString() : this.mPackageName;
        createNotificationChannel(context, string);
        String string2 = context.getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_SUCCESS, string);
        Bundle bundle = new Bundle();
        bundle.putStringArray("android.foregroundApps", new String[]{this.mPackageName});
        Notification.Builder contentIntent = new Notification.Builder(context, this.mNotificationTag).setOngoing(true).setContentTitle(context.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_ENTRY, string)).setContentText(string2).setSmallIcon(R.drawable.$loader_horizontal_watch__5).setColor(context.getColor(R.color.car_colorPrimary)).setStyle(new Notification.BigTextStyle().bigText(string2)).setLocalOnly(true).addExtras(bundle).setContentIntent(getContentIntent(context, this.mPackageName));
        if (applicationInfo != null && (applicationIcon = packageManager.getApplicationIcon(applicationInfo)) != null) {
            contentIntent.setLargeIcon(this.mIconUtilities.createIconBitmap(applicationIcon));
        }
        this.mNotificationManager.notify(this.mNotificationTag, 0, contentIntent.build());
    }

    private PendingIntent getContentIntent(Context context, String str) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.fromParts(Settings.ATTR_PACKAGE, str, null));
        intent.setFlags(268468224);
        return PendingIntent.getActivity(context, this.mRequestCode, intent, 268435456);
    }

    private void createNotificationChannel(Context context, String str) {
        if (sChannelGroup == null) {
            sChannelGroup = new NotificationChannelGroup(CHANNEL_PREFIX, this.mService.mContext.getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_IN_PROGRESS));
            this.mNotificationManager.createNotificationChannelGroup(sChannelGroup);
        }
        String string = context.getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_SUCCESS, str);
        if (this.mNotificationManager.getNotificationChannel(this.mNotificationTag) != null) {
            return;
        }
        NotificationChannel notificationChannel = new NotificationChannel(this.mNotificationTag, string, 1);
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(false);
        notificationChannel.setBlockableSystem(true);
        notificationChannel.setGroup(sChannelGroup.getId());
        notificationChannel.setBypassDnd(true);
        this.mNotificationManager.createNotificationChannel(notificationChannel);
    }

    private ApplicationInfo getApplicationInfo(PackageManager packageManager, String str) {
        try {
            return packageManager.getApplicationInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
