package com.android.contacts.util;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.os.BuildCompat;
import com.android.contacts.R;

@TargetApi(26)
public class ContactsNotificationChannelsUtil {
    public static String DEFAULT_CHANNEL = "DEFAULT_CHANNEL";

    public static void createDefaultChannel(Context context) {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel(DEFAULT_CHANNEL, context.getString(R.string.contacts_default_notification_channel), 2));
    }
}
