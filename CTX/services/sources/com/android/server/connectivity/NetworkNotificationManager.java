package com.android.server.connectivity;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.os.BenesseExtension;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;

public class NetworkNotificationManager {
    private static final boolean DBG = true;
    private static final String TAG = NetworkNotificationManager.class.getSimpleName();
    private static final boolean VDBG = false;
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final SparseIntArray mNotificationTypeMap = new SparseIntArray();
    private final TelephonyManager mTelephonyManager;

    public enum NotificationType {
        LOST_INTERNET(742),
        NETWORK_SWITCH(743),
        NO_INTERNET(741),
        SIGN_IN(740);

        public final int eventId;

        NotificationType(int i) {
            this.eventId = i;
            Holder.sIdToTypeMap.put(i, this);
        }

        private static class Holder {
            private static SparseArray<NotificationType> sIdToTypeMap = new SparseArray<>();

            private Holder() {
            }
        }

        public static NotificationType getFromId(int i) {
            return (NotificationType) Holder.sIdToTypeMap.get(i);
        }
    }

    public NetworkNotificationManager(Context context, TelephonyManager telephonyManager, NotificationManager notificationManager) {
        this.mContext = context;
        this.mTelephonyManager = telephonyManager;
        this.mNotificationManager = notificationManager;
    }

    private static int getFirstTransportType(NetworkAgentInfo networkAgentInfo) {
        for (int i = 0; i < 64; i++) {
            if (networkAgentInfo.networkCapabilities.hasTransport(i)) {
                return i;
            }
        }
        return -1;
    }

    private static String getTransportName(int i) {
        Resources system = Resources.getSystem();
        try {
            return system.getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnessesStrong)[i];
        } catch (IndexOutOfBoundsException e) {
            return system.getString(R.string.ext_media_status_missing);
        }
    }

    private static int getIcon(int i) {
        if (i == 1) {
            return R.drawable.pointer_hand_large;
        }
        return R.drawable.pointer_grabbing_vector;
    }

    public void showNotification(int i, NotificationType notificationType, NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2, PendingIntent pendingIntent, boolean z) {
        String extraInfo;
        int firstTransportType;
        String string;
        String string2;
        String strTagFor = tagFor(i);
        int i2 = notificationType.eventId;
        boolean z2 = false;
        if (networkAgentInfo != null) {
            firstTransportType = getFirstTransportType(networkAgentInfo);
            extraInfo = networkAgentInfo.networkInfo.getExtraInfo();
            if (TextUtils.isEmpty(extraInfo)) {
                extraInfo = networkAgentInfo.networkCapabilities.getSSID();
            }
            if (!networkAgentInfo.networkCapabilities.hasCapability(12)) {
                return;
            }
        } else {
            extraInfo = null;
            firstTransportType = 0;
        }
        NotificationType fromId = NotificationType.getFromId(this.mNotificationTypeMap.get(i));
        if (priority(fromId) > priority(notificationType)) {
            Slog.d(TAG, String.format("ignoring notification %s for network %s with existing notification %s", notificationType, Integer.valueOf(i), fromId));
            return;
        }
        clearNotification(i);
        Slog.d(TAG, String.format("showNotification tag=%s event=%s transport=%s name=%s highPriority=%s", strTagFor, nameOf(i2), getTransportName(firstTransportType), extraInfo, Boolean.valueOf(z)));
        Resources system = Resources.getSystem();
        int icon = getIcon(firstTransportType);
        if (notificationType == NotificationType.NO_INTERNET && firstTransportType == 1) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            string2 = system.getString(R.string.notification_channel_security, 0);
            string = system.getString(R.string.notification_channel_sim);
        } else if (notificationType == NotificationType.LOST_INTERNET && firstTransportType == 1) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            string2 = system.getString(R.string.notification_channel_security, 0);
            string = system.getString(R.string.notification_channel_sim);
        } else if (notificationType == NotificationType.SIGN_IN) {
            switch (firstTransportType) {
                case 0:
                    string2 = system.getString(R.string.ext_media_nomedia_notification_title, 0);
                    string = this.mTelephonyManager.getNetworkOperatorName();
                    break;
                case 1:
                    if (BenesseExtension.getDchaState() != 0) {
                        return;
                    }
                    String string3 = system.getString(R.string.notification_channel_emergency_callback, 0);
                    string = system.getString(R.string.ext_media_ready_notification_message, WifiInfo.removeDoubleQuotes(networkAgentInfo.networkCapabilities.getSSID()));
                    string2 = string3;
                    break;
                default:
                    string2 = system.getString(R.string.ext_media_nomedia_notification_title, 0);
                    string = system.getString(R.string.ext_media_ready_notification_message, extraInfo);
                    break;
            }
        } else if (notificationType == NotificationType.NETWORK_SWITCH) {
            String transportName = getTransportName(firstTransportType);
            String transportName2 = getTransportName(getFirstTransportType(networkAgentInfo2));
            String string4 = system.getString(R.string.ext_media_status_checking, transportName2);
            string = system.getString(R.string.ext_media_status_ejecting, transportName2, transportName);
            string2 = string4;
        } else {
            Slog.wtf(TAG, "Unknown notification type " + notificationType + " on network transport " + getTransportName(firstTransportType));
            return;
        }
        String str = z ? SystemNotificationChannels.NETWORK_ALERTS : SystemNotificationChannels.NETWORK_STATUS;
        Notification.Builder when = new Notification.Builder(this.mContext, str).setWhen(System.currentTimeMillis());
        if (notificationType == NotificationType.NETWORK_SWITCH) {
            z2 = true;
        }
        Notification.Builder onlyAlertOnce = when.setShowWhen(z2).setSmallIcon(icon).setAutoCancel(true).setTicker(string2).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setContentTitle(string2).setContentIntent(pendingIntent).setLocalOnly(true).setOnlyAlertOnce(true);
        if (notificationType == NotificationType.NETWORK_SWITCH) {
            onlyAlertOnce.setStyle(new Notification.BigTextStyle().bigText(string));
        } else {
            onlyAlertOnce.setContentText(string);
        }
        if (notificationType == NotificationType.SIGN_IN) {
            onlyAlertOnce.extend(new Notification.TvExtender().setChannelId(str));
        }
        Notification notificationBuild = onlyAlertOnce.build();
        this.mNotificationTypeMap.put(i, i2);
        try {
            this.mNotificationManager.notifyAsUser(strTagFor, i2, notificationBuild, UserHandle.ALL);
        } catch (NullPointerException e) {
            Slog.d(TAG, "setNotificationVisible: visible notificationManager error", e);
        }
    }

    public void clearNotification(int i) {
        if (this.mNotificationTypeMap.indexOfKey(i) < 0) {
            return;
        }
        String strTagFor = tagFor(i);
        int i2 = this.mNotificationTypeMap.get(i);
        Slog.d(TAG, String.format("clearing notification tag=%s event=%s", strTagFor, nameOf(i2)));
        try {
            this.mNotificationManager.cancelAsUser(strTagFor, i2, UserHandle.ALL);
        } catch (NullPointerException e) {
            Slog.d(TAG, String.format("failed to clear notification tag=%s event=%s", strTagFor, nameOf(i2)), e);
        }
        this.mNotificationTypeMap.delete(i);
    }

    public void setProvNotificationVisible(boolean z, int i, String str) {
        if (z) {
            showNotification(i, NotificationType.SIGN_IN, null, null, PendingIntent.getBroadcast(this.mContext, 0, new Intent(str), 0), false);
            return;
        }
        clearNotification(i);
    }

    public void showToast(NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2) {
        Toast.makeText(this.mContext, this.mContext.getResources().getString(R.string.ext_media_status_formatting, getTransportName(getFirstTransportType(networkAgentInfo)), getTransportName(getFirstTransportType(networkAgentInfo2))), 1).show();
    }

    @VisibleForTesting
    static String tagFor(int i) {
        return String.format("ConnectivityNotification:%d", Integer.valueOf(i));
    }

    @VisibleForTesting
    static String nameOf(int i) {
        NotificationType fromId = NotificationType.getFromId(i);
        return fromId != null ? fromId.name() : "UNKNOWN";
    }

    private static int priority(NotificationType notificationType) {
        if (notificationType == null) {
            return 0;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[notificationType.ordinal()]) {
        }
        return 0;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType = new int[NotificationType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NotificationType.SIGN_IN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NotificationType.NO_INTERNET.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NotificationType.NETWORK_SWITCH.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NotificationType.LOST_INTERNET.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }
}
