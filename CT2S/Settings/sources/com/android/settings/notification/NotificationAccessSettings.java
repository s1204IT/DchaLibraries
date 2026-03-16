package com.android.settings.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings;

public class NotificationAccessSettings extends ManagedServiceSettings {
    private static final String TAG = NotificationAccessSettings.class.getSimpleName();
    private static final ManagedServiceSettings.Config CONFIG = getNotificationListenerConfig();

    private static ManagedServiceSettings.Config getNotificationListenerConfig() {
        ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.setting = "enabled_notification_listeners";
        c.intentAction = "android.service.notification.NotificationListenerService";
        c.permission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
        c.noun = "notification listener";
        c.warningDialogTitle = R.string.notification_listener_security_warning_title;
        c.warningDialogSummary = R.string.notification_listener_security_warning_summary;
        c.emptyText = R.string.no_notification_listeners;
        return c;
    }

    @Override
    protected ManagedServiceSettings.Config getConfig() {
        return CONFIG;
    }

    public static int getListenersCount(PackageManager pm) {
        return getServicesCount(CONFIG, pm);
    }

    public static int getEnabledListenersCount(Context context) {
        return getEnabledServicesCount(CONFIG, context);
    }
}
