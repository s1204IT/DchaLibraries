package com.android.deskclock;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.BuildCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NotificationChannelManager {
    private static final String PREFS_FILENAME = "ClockNotificationChannel";
    private static final String PREF_NEED_FIRST_INIT = "NeedInit";
    private static final String[] allChannels = {Channel.EVENT_EXPIRED, Channel.DEFAULT_NOTIFICATION};
    private static NotificationChannelManager instance;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Channel {
        public static final String DEFAULT_NOTIFICATION = "defaultNotif";
        public static final String EVENT_EXPIRED = "eventExpire";
    }

    public static NotificationChannelManager getInstance() {
        LogUtils.d("getInstance: instance: " + instance, new Object[0]);
        if (instance == null) {
            instance = new NotificationChannelManager();
        }
        return instance;
    }

    @TargetApi(26)
    public static void applyChannel(@NonNull NotificationCompat.Builder builder, @NonNull Context context, String str) {
        LogUtils.d("applyChannel: : context: " + context + " channelId: " + str, new Object[0]);
        if (BuildCompat.isAtLeastO()) {
            builder.setChannelId(getInstance().getChannel(context, str).getId());
        }
    }

    public void firstInitIfNeeded(@NonNull Context context) {
        firstInitIfNeededSync(context);
    }

    private boolean firstInitIfNeededSync(@NonNull Context context) {
        LogUtils.d("firstInitIfNeededSync: context: " + context, new Object[0]);
        if (!needsFirstInit(context)) {
            return false;
        }
        LogUtils.d("firstInitIfNeededSync: needsFirstInit true ", new Object[0]);
        initChannels(context);
        return true;
    }

    public boolean needsFirstInit(@NonNull Context context) {
        return BuildCompat.isAtLeastO() && getSharedPreferences(context).getBoolean(PREF_NEED_FIRST_INIT, true);
    }

    @RequiresApi(24)
    private SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS_FILENAME, 0);
    }

    @TargetApi(26)
    public void initChannels(@NonNull Context context) {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        LogUtils.d("Mahesh NotificationChannelManager.initChannels", new Object[0]);
        for (String str : allChannels) {
            getChannel(context, str);
        }
        getSharedPreferences(context).edit().putBoolean(PREF_NEED_FIRST_INIT, false).apply();
    }

    @NonNull
    @RequiresApi(26)
    private NotificationChannel getChannel(@NonNull Context context, String str) {
        LogUtils.d("getChannel..channelId: " + str, new Object[0]);
        NotificationChannel notificationChannel = getNotificationManager(context).getNotificationChannel(str);
        if (notificationChannel == null) {
            return createChannel(context, str);
        }
        return notificationChannel;
    }

    @RequiresApi(26)
    private NotificationChannel createChannel(Context context, String str) {
        byte b;
        String str2;
        int i;
        LogUtils.d("createChannel..channelId: " + str, new Object[0]);
        Uri uri = Uri.EMPTY;
        int iHashCode = str.hashCode();
        if (iHashCode != -666089105) {
            b = (iHashCode == -290029479 && str.equals(Channel.EVENT_EXPIRED)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(Channel.DEFAULT_NOTIFICATION)) {
            b = 1;
        }
        switch (b) {
            case 0:
                str2 = "Event Expire Notifications";
                i = 5;
                break;
            case 1:
                str2 = "Default Notifications";
                i = 2;
                break;
            default:
                throw new IllegalArgumentException("Unknown channel: " + str);
        }
        NotificationChannel notificationChannel = new NotificationChannel(str, str2, i);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.setSound(null, null);
        notificationChannel.enableLights(false);
        notificationChannel.setBypassDnd(true);
        getNotificationManager(context).createNotificationChannel(notificationChannel);
        LogUtils.d("createChannel ends.channel: " + notificationChannel, new Object[0]);
        return notificationChannel;
    }

    private static NotificationManager getNotificationManager(@NonNull Context context) {
        return (NotificationManager) context.getSystemService(NotificationManager.class);
    }
}
