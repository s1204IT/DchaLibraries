package com.android.internal.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.media.AudioAttributes;
import android.os.RemoteException;
import android.provider.Settings;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.Arrays;

public class SystemNotificationChannels {
    public static String VIRTUAL_KEYBOARD = "VIRTUAL_KEYBOARD";
    public static String PHYSICAL_KEYBOARD = "PHYSICAL_KEYBOARD";
    public static String SECURITY = "SECURITY";
    public static String CAR_MODE = "CAR_MODE";
    public static String ACCOUNT = "ACCOUNT";
    public static String DEVELOPER = "DEVELOPER";
    public static String UPDATES = "UPDATES";
    public static String NETWORK_STATUS = "NETWORK_STATUS";
    public static String NETWORK_ALERTS = "NETWORK_ALERTS";
    public static String NETWORK_AVAILABLE = "NETWORK_AVAILABLE";
    public static String VPN = "VPN";
    public static String DEVICE_ADMIN = "DEVICE_ADMIN";
    public static String ALERTS = "ALERTS";
    public static String RETAIL_MODE = "RETAIL_MODE";
    public static String USB = "USB";
    public static String FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static String HEAVY_WEIGHT_APP = "HEAVY_WEIGHT_APP";
    public static String SYSTEM_CHANGES = "SYSTEM_CHANGES";
    public static String DO_NOT_DISTURB = "DO_NOT_DISTURB";

    public static void createAll(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        ArrayList arrayList = new ArrayList();
        NotificationChannel notificationChannel = new NotificationChannel(VIRTUAL_KEYBOARD, context.getString(R.string.notification_channel_virtual_keyboard), 2);
        notificationChannel.setBlockableSystem(true);
        arrayList.add(notificationChannel);
        NotificationChannel notificationChannel2 = new NotificationChannel(PHYSICAL_KEYBOARD, context.getString(R.string.notification_channel_physical_keyboard), 3);
        notificationChannel2.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        notificationChannel2.setBlockableSystem(true);
        arrayList.add(notificationChannel2);
        arrayList.add(new NotificationChannel(SECURITY, context.getString(R.string.notification_channel_security), 2));
        NotificationChannel notificationChannel3 = new NotificationChannel(CAR_MODE, context.getString(R.string.notification_channel_car_mode), 2);
        notificationChannel3.setBlockableSystem(true);
        arrayList.add(notificationChannel3);
        arrayList.add(newAccountChannel(context));
        NotificationChannel notificationChannel4 = new NotificationChannel(DEVELOPER, context.getString(R.string.notification_channel_developer), 2);
        notificationChannel4.setBlockableSystem(true);
        arrayList.add(notificationChannel4);
        arrayList.add(new NotificationChannel(UPDATES, context.getString(R.string.notification_channel_updates), 2));
        arrayList.add(new NotificationChannel(NETWORK_STATUS, context.getString(R.string.notification_channel_network_status), 2));
        NotificationChannel notificationChannel5 = new NotificationChannel(NETWORK_ALERTS, context.getString(R.string.notification_channel_network_alerts), 4);
        notificationChannel5.setBlockableSystem(true);
        arrayList.add(notificationChannel5);
        NotificationChannel notificationChannel6 = new NotificationChannel(NETWORK_AVAILABLE, context.getString(R.string.notification_channel_network_available), 2);
        notificationChannel6.setBlockableSystem(true);
        arrayList.add(notificationChannel6);
        arrayList.add(new NotificationChannel(VPN, context.getString(R.string.notification_channel_vpn), 2));
        arrayList.add(new NotificationChannel(DEVICE_ADMIN, context.getString(R.string.notification_channel_device_admin), 2));
        arrayList.add(new NotificationChannel(ALERTS, context.getString(R.string.notification_channel_alerts), 3));
        arrayList.add(new NotificationChannel(RETAIL_MODE, context.getString(R.string.notification_channel_retail_mode), 2));
        arrayList.add(new NotificationChannel(USB, context.getString(R.string.notification_channel_usb), 1));
        NotificationChannel notificationChannel7 = new NotificationChannel(FOREGROUND_SERVICE, context.getString(R.string.notification_channel_foreground_service), 2);
        notificationChannel7.setBlockableSystem(true);
        arrayList.add(notificationChannel7);
        NotificationChannel notificationChannel8 = new NotificationChannel(HEAVY_WEIGHT_APP, context.getString(R.string.notification_channel_heavy_weight_app), 3);
        notificationChannel8.setShowBadge(false);
        notificationChannel8.setSound(null, new AudioAttributes.Builder().setContentType(4).setUsage(10).build());
        arrayList.add(notificationChannel8);
        arrayList.add(new NotificationChannel(SYSTEM_CHANGES, context.getString(R.string.notification_channel_system_changes), 2));
        arrayList.add(new NotificationChannel(DO_NOT_DISTURB, context.getString(R.string.notification_channel_do_not_disturb), 2));
        notificationManager.createNotificationChannels(arrayList);
    }

    public static void createAccountChannelForPackage(String str, int i, Context context) {
        try {
            NotificationManager.getService().createNotificationChannelsForPackage(str, i, new ParceledListSlice(Arrays.asList(newAccountChannel(context))));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static NotificationChannel newAccountChannel(Context context) {
        return new NotificationChannel(ACCOUNT, context.getString(R.string.notification_channel_account), 2);
    }

    private SystemNotificationChannels() {
    }
}
