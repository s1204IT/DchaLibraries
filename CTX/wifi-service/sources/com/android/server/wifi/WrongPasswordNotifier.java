package com.android.server.wifi;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.wifi.util.NativeUtil;

public class WrongPasswordNotifier {
    private static final long CANCEL_TIMEOUT_MILLISECONDS = 300000;

    @VisibleForTesting
    public static final int NOTIFICATION_ID = 42;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final NotificationManager mNotificationManager;
    private boolean mWrongPasswordDetected;

    public WrongPasswordNotifier(Context context, FrameworkFacade frameworkFacade) {
        this.mContext = context;
        this.mFrameworkFacade = frameworkFacade;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
    }

    public void onWrongPasswordError(String str) {
        showNotification(str);
        this.mWrongPasswordDetected = true;
    }

    public void onNewConnectionAttempt() {
        if (this.mWrongPasswordDetected) {
            dismissNotification();
            this.mWrongPasswordDetected = false;
        }
    }

    private void showNotification(String str) {
        Intent intent = new Intent("android.settings.WIFI_SETTINGS");
        intent.putExtra("wifi_start_connect_ssid", NativeUtil.removeEnclosingQuotes(str));
        Notification.Builder color = this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS).setAutoCancel(true).setTimeoutAfter(CANCEL_TIMEOUT_MILLISECONDS).setSmallIcon(R.drawable.pointer_hand_large).setContentTitle(this.mContext.getString(R.string.notification_channel_network_alert)).setContentText(str).setContentIntent(this.mFrameworkFacade.getActivity(this.mContext, 0, intent, 134217728)).setColor(this.mContext.getResources().getColor(R.color.car_colorPrimary));
        if (BenesseExtension.getDchaState() != 0) {
            color.setContentIntent(null);
        }
        this.mNotificationManager.notify(42, color.build());
    }

    private void dismissNotification() {
        this.mNotificationManager.cancel(null, 42);
    }
}
