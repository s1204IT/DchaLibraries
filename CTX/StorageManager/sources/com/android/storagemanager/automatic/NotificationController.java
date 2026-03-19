package com.android.storagemanager.automatic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.BenesseExtension;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v4.os.BuildCompat;
import com.android.storagemanager.R;
import java.util.concurrent.TimeUnit;

public class NotificationController extends BroadcastReceiver {
    static final String INTENT_ACTION_TAP = "com.android.storagemanager.automatic.SHOW_SETTINGS";
    private Clock mClock;
    private static final long DISMISS_DELAY = TimeUnit.DAYS.toMillis(14);
    private static final long NO_THANKS_DELAY = TimeUnit.DAYS.toMillis(90);

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case "com.android.storagemanager.automatic.ACTIVATE":
                Settings.Secure.putInt(context.getContentResolver(), "automatic_storage_manager_enabled", 1);
                if (!SystemProperties.getBoolean("ro.storage_manager.enabled", false)) {
                    context.startActivity(new Intent(context, (Class<?>) WarningDialogActivity.class));
                    break;
                }
                break;
            case "com.android.storagemanager.automatic.NO_THANKS":
                delayNextNotification(context, NO_THANKS_DELAY);
                break;
            case "com.android.storagemanager.automatic.DISMISS":
                delayNextNotification(context, DISMISS_DELAY);
                break;
            case "com.android.storagemanager.automatic.show_notification":
                maybeShowNotification(context);
                return;
            case "com.android.storagemanager.automatic.DEBUG_SHOW_NOTIFICATION":
                showNotification(context);
                return;
            case "com.android.storagemanager.automatic.SHOW_SETTINGS":
                if (BenesseExtension.getDchaState() == 0) {
                    Intent intent2 = new Intent("android.settings.INTERNAL_STORAGE_SETTINGS");
                    intent2.setFlags(268435456);
                    context.startActivity(intent2);
                    break;
                }
                break;
        }
        cancelNotification(context, intent);
    }

    private void maybeShowNotification(Context context) {
        if (shouldShowNotification(context)) {
            showNotification(context);
        }
    }

    private boolean shouldShowNotification(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationController", 0);
        int i = sharedPreferences.getInt("notification_shown_count", 0);
        int i2 = sharedPreferences.getInt("notification_dismiss_count", 0);
        if (i >= 4 || i2 >= 9) {
            return false;
        }
        return getCurrentTime() >= sharedPreferences.getLong("notification_next_show_time", 0L);
    }

    private void showNotification(Context context) {
        Notification.Builder builder;
        Resources resources = context.getResources();
        Intent baseIntent = getBaseIntent(context, "com.android.storagemanager.automatic.NO_THANKS");
        baseIntent.putExtra("id", 0);
        Notification.Action.Builder builder2 = new Notification.Action.Builder((Icon) null, resources.getString(R.string.automatic_storage_manager_cancel_button), PendingIntent.getBroadcast(context, 0, baseIntent, 134217728));
        Intent baseIntent2 = getBaseIntent(context, "com.android.storagemanager.automatic.ACTIVATE");
        baseIntent2.putExtra("id", 0);
        Notification.Action.Builder builder3 = new Notification.Action.Builder((Icon) null, resources.getString(R.string.automatic_storage_manager_activate_button), PendingIntent.getBroadcast(context, 0, baseIntent2, 134217728));
        Intent baseIntent3 = getBaseIntent(context, "com.android.storagemanager.automatic.DISMISS");
        baseIntent3.putExtra("id", 0);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, baseIntent3, 1073741824);
        Intent baseIntent4 = getBaseIntent(context, INTENT_ACTION_TAP);
        baseIntent4.putExtra("id", 0);
        PendingIntent broadcast2 = PendingIntent.getBroadcast(context, 0, baseIntent4, 1073741824);
        if (BuildCompat.isAtLeastO()) {
            makeNotificationChannel(context);
            builder = new Notification.Builder(context, "storage");
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setSmallIcon(R.drawable.ic_settings_24dp).setContentTitle(resources.getString(R.string.automatic_storage_manager_notification_title)).setContentText(resources.getString(R.string.automatic_storage_manager_notification_summary)).setStyle(new Notification.BigTextStyle().bigText(resources.getString(R.string.automatic_storage_manager_notification_summary))).addAction(builder2.build()).addAction(builder3.build()).setContentIntent(broadcast2).setDeleteIntent(broadcast).setLocalOnly(true);
        ((NotificationManager) context.getSystemService("notification")).notify(0, builder.build());
    }

    private void makeNotificationChannel(Context context) {
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel("storage", context.getString(R.string.app_name), 2));
    }

    private void cancelNotification(Context context, Intent intent) {
        if (intent.getAction() == "com.android.storagemanager.automatic.DISMISS") {
            incrementNotificationDismissedCount(context);
        } else {
            incrementNotificationShownCount(context);
        }
        int intExtra = intent.getIntExtra("id", -1);
        if (intExtra == -1) {
            return;
        }
        ((NotificationManager) context.getSystemService("notification")).cancel(intExtra);
    }

    private void incrementNotificationShownCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationController", 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putInt("notification_shown_count", sharedPreferences.getInt("notification_shown_count", 0) + 1);
        editorEdit.apply();
    }

    private void incrementNotificationDismissedCount(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationController", 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putInt("notification_dismiss_count", sharedPreferences.getInt("notification_dismiss_count", 0) + 1);
        editorEdit.apply();
    }

    private void delayNextNotification(Context context, long j) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("NotificationController", 0).edit();
        editorEdit.putLong("notification_next_show_time", getCurrentTime() + j);
        editorEdit.apply();
    }

    private long getCurrentTime() {
        if (this.mClock == null) {
            this.mClock = new Clock();
        }
        return this.mClock.currentTimeMillis();
    }

    Intent getBaseIntent(Context context, String str) {
        return new Intent(context, (Class<?>) NotificationController.class).setAction(str);
    }

    protected static class Clock {
        protected Clock() {
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}
