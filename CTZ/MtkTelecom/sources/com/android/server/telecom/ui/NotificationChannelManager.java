package com.android.server.telecom.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.telecom.Log;
import com.android.server.telecom.CallState;
import com.android.server.telecom.R;

public class NotificationChannelManager {
    private BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(this, "Locale change; recreating channels.", new Object[0]);
            NotificationChannelManager.this.createOrUpdateAll(context);
        }
    };

    public void createChannels(Context context) {
        context.registerReceiver(this.mLocaleChangeReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        createOrUpdateAll(context);
    }

    private void createOrUpdateAll(Context context) {
        createOrUpdateChannel(context, "TelecomMissedCalls");
        createOrUpdateChannel(context, "TelecomIncomingCalls");
        createOrUpdateChannel(context, "TelecomCallBlocking");
    }

    private void createOrUpdateChannel(Context context, String str) {
        getNotificationManager(context).createNotificationChannel(createChannel(context, str));
    }

    private NotificationChannel createChannel(Context context, String str) {
        byte b;
        boolean z;
        Uri uri = Uri.parse("");
        CharSequence text = "";
        int iHashCode = str.hashCode();
        boolean z2 = true;
        boolean z3 = false;
        if (iHashCode != -950300157) {
            if (iHashCode != 850994122) {
                b = (iHashCode == 1072028952 && str.equals("TelecomIncomingCalls")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("TelecomCallBlocking")) {
                b = 2;
            }
        } else if (str.equals("TelecomMissedCalls")) {
            b = 1;
        }
        int i = 3;
        switch (b) {
            case CallState.NEW:
                text = context.getText(R.string.notification_channel_incoming_call);
                i = 5;
                z = false;
                break;
            case 1:
                text = context.getText(R.string.notification_channel_missed_call);
                z = true;
                z3 = true;
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                text = context.getText(R.string.notification_channel_call_blocking);
                uri = null;
                i = 2;
            default:
                z = false;
                z2 = false;
                break;
        }
        NotificationChannel notificationChannel = new NotificationChannel(str, text, i);
        notificationChannel.setShowBadge(z3);
        if (uri != null) {
            notificationChannel.setSound(uri, new AudioAttributes.Builder().setUsage(5).build());
        }
        notificationChannel.enableLights(z2);
        notificationChannel.enableVibration(z);
        return notificationChannel;
    }

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(NotificationManager.class);
    }
}
