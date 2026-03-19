package com.android.internal.telephony.util;

import android.R;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import java.util.Arrays;

public class NotificationChannelController {
    public static final String CHANNEL_ID_ALERT = "alert";
    public static final String CHANNEL_ID_CALL_FORWARD = "callForward";
    private static final String CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED = "mobileDataAlert";
    public static final String CHANNEL_ID_MOBILE_DATA_STATUS = "mobileDataAlertNew";
    public static final String CHANNEL_ID_SIM = "sim";
    public static final String CHANNEL_ID_SMS = "sms";
    public static final String CHANNEL_ID_VOICE_MAIL = "voiceMail";
    public static final String CHANNEL_ID_WFC = "wfc";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                NotificationChannelController.createAll(context);
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && -1 != SubscriptionManager.getDefaultSubscriptionId()) {
                NotificationChannelController.migrateVoicemailNotificationSettings(context);
            }
        }
    };

    private static void createAll(Context context) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_ALERT, context.getText(R.string.face_acquired_tilt_too_extreme), 3);
        notificationChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, new AudioAttributes.Builder().setUsage(5).build());
        notificationChannel.setBlockableSystem(true);
        NotificationChannel notificationChannel2 = new NotificationChannel(CHANNEL_ID_MOBILE_DATA_STATUS, context.getText(R.string.face_acquired_sensor_dirty), 2);
        notificationChannel2.setBlockableSystem(true);
        NotificationChannel notificationChannel3 = new NotificationChannel(CHANNEL_ID_SIM, context.getText(R.string.face_acquired_too_left), 2);
        notificationChannel3.setSound(null, null);
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannels(Arrays.asList(new NotificationChannel(CHANNEL_ID_CALL_FORWARD, context.getText(R.string.face_acquired_mouth_covering_detected_alt), 2), new NotificationChannel(CHANNEL_ID_SMS, context.getText(R.string.face_acquired_too_low), 4), new NotificationChannel(CHANNEL_ID_WFC, context.getText(R.string.face_dangling_notification_msg), 2), notificationChannel, notificationChannel2, notificationChannel3));
        if (getChannel(CHANNEL_ID_VOICE_MAIL, context) != null) {
            migrateVoicemailNotificationSettings(context);
        }
        if (getChannel(CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED, context) != null) {
            ((NotificationManager) context.getSystemService(NotificationManager.class)).deleteNotificationChannel(CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED);
        }
    }

    public NotificationChannelController(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        createAll(context);
    }

    public static NotificationChannel getChannel(String str, Context context) {
        return ((NotificationManager) context.getSystemService(NotificationManager.class)).getNotificationChannel(str);
    }

    private static void migrateVoicemailNotificationSettings(Context context) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_VOICE_MAIL, context.getText(R.string.face_authenticated_confirmation_required), 3);
        notificationChannel.enableVibration(VoicemailNotificationSettingsUtil.getVibrationPreference(context));
        Uri ringTonePreference = VoicemailNotificationSettingsUtil.getRingTonePreference(context);
        if (ringTonePreference == null) {
            ringTonePreference = Settings.System.DEFAULT_NOTIFICATION_URI;
        }
        notificationChannel.setSound(ringTonePreference, new AudioAttributes.Builder().setUsage(5).build());
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
    }
}
