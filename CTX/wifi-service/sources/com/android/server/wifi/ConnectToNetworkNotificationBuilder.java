package com.android.server.wifi;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;

public class ConnectToNetworkNotificationBuilder {
    public static final String ACTION_CONNECT_TO_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.CONNECT_TO_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.PICK_WIFI_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE = "com.android.server.wifi.ConnectToNetworkNotification.PICK_NETWORK_AFTER_FAILURE";
    public static final String ACTION_USER_DISMISSED_NOTIFICATION = "com.android.server.wifi.ConnectToNetworkNotification.USER_DISMISSED_NOTIFICATION";
    public static final String AVAILABLE_NETWORK_NOTIFIER_TAG = "com.android.server.wifi.ConnectToNetworkNotification.AVAILABLE_NETWORK_NOTIFIER_TAG";
    private Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private Resources mResources;

    public ConnectToNetworkNotificationBuilder(Context context, FrameworkFacade frameworkFacade) {
        this.mContext = context;
        this.mResources = context.getResources();
        this.mFrameworkFacade = frameworkFacade;
    }

    public Notification createConnectToAvailableNetworkNotification(String str, ScanResult scanResult) {
        byte b;
        CharSequence text;
        int iHashCode = str.hashCode();
        if (iHashCode != 594918769) {
            b = (iHashCode == 2017428693 && str.equals(OpenNetworkNotifier.TAG)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(CarrierNetworkNotifier.TAG)) {
            b = 1;
        }
        switch (b) {
            case 0:
                text = this.mContext.getText(R.string.notification_channel_foreground_service);
                break;
            case 1:
                text = this.mContext.getText(R.string.notification_channel_device_admin);
                break;
            default:
                Log.wtf("ConnectToNetworkNotificationBuilder", "Unknown network notifier." + str);
                return null;
        }
        return createNotificationBuilder(text, scanResult.SSID, str).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, str)).addAction(new Notification.Action.Builder((Icon) null, this.mResources.getText(R.string.notification_channel_developer_important), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, str)).build()).addAction(new Notification.Action.Builder((Icon) null, this.mResources.getText(R.string.notification_channel_developer), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, str)).build()).build();
    }

    public Notification createNetworkConnectingNotification(String str, ScanResult scanResult) {
        return createNotificationBuilder(this.mContext.getText(R.string.notification_channel_mobile_data_status), scanResult.SSID, str).setProgress(0, 0, true).build();
    }

    public Notification createNetworkConnectedNotification(String str, ScanResult scanResult) {
        return createNotificationBuilder(this.mContext.getText(R.string.notification_channel_heavy_weight_app), scanResult.SSID, str).build();
    }

    public Notification createNetworkFailedNotification(String str) {
        return createNotificationBuilder(this.mContext.getText(R.string.notification_channel_network_alert), this.mContext.getText(R.string.notification_channel_display), str).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE, str)).setAutoCancel(true).build();
    }

    private int getNotifierRequestCode(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 594918769) {
            b = (iHashCode == 2017428693 && str.equals(OpenNetworkNotifier.TAG)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(CarrierNetworkNotifier.TAG)) {
            b = 1;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            default:
                return 0;
        }
    }

    private Notification.Builder createNotificationBuilder(CharSequence charSequence, CharSequence charSequence2, String str) {
        return this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_AVAILABLE).setSmallIcon(R.drawable.pointer_hand_large).setTicker(charSequence).setContentTitle(charSequence).setContentText(charSequence2).setDeleteIntent(getPrivateBroadcast(ACTION_USER_DISMISSED_NOTIFICATION, str)).setShowWhen(false).setLocalOnly(true).setColor(this.mResources.getColor(R.color.car_colorPrimary, this.mContext.getTheme()));
    }

    private PendingIntent getPrivateBroadcast(String str, String str2) {
        int notifierRequestCode;
        Intent intent = new Intent(str).setPackage("android");
        if (str2 != null) {
            intent.putExtra(AVAILABLE_NETWORK_NOTIFIER_TAG, str2);
            notifierRequestCode = getNotifierRequestCode(str2);
        } else {
            notifierRequestCode = 0;
        }
        return this.mFrameworkFacade.getBroadcast(this.mContext, notifierRequestCode, intent, 134217728);
    }
}
