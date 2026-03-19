package com.android.internal.util;

import android.app.Notification;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import java.util.Objects;

public class NotificationMessagingUtil {
    private static final String DEFAULT_SMS_APP_SETTING = "sms_default_application";
    private final Context mContext;
    private ArrayMap<Integer, String> mDefaultSmsApp = new ArrayMap<>();
    private final ContentObserver mSmsContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean z, Uri uri, int i) {
            if (Settings.Secure.getUriFor("sms_default_application").equals(uri)) {
                NotificationMessagingUtil.this.cacheDefaultSmsApp(i);
            }
        }
    };

    public NotificationMessagingUtil(Context context) {
        this.mContext = context;
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("sms_default_application"), false, this.mSmsContentObserver);
    }

    public boolean isImportantMessaging(StatusBarNotification statusBarNotification, int i) {
        if (i < 2) {
            return false;
        }
        return hasMessagingStyle(statusBarNotification) || (isCategoryMessage(statusBarNotification) && isDefaultMessagingApp(statusBarNotification));
    }

    public boolean isMessaging(StatusBarNotification statusBarNotification) {
        return hasMessagingStyle(statusBarNotification) || isDefaultMessagingApp(statusBarNotification) || isCategoryMessage(statusBarNotification);
    }

    private boolean isDefaultMessagingApp(StatusBarNotification statusBarNotification) {
        int userId = statusBarNotification.getUserId();
        if (userId == -10000 || userId == -1) {
            return false;
        }
        if (this.mDefaultSmsApp.get(Integer.valueOf(userId)) == null) {
            cacheDefaultSmsApp(userId);
        }
        return Objects.equals(this.mDefaultSmsApp.get(Integer.valueOf(userId)), statusBarNotification.getPackageName());
    }

    private void cacheDefaultSmsApp(int i) {
        this.mDefaultSmsApp.put(Integer.valueOf(i), Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "sms_default_application", i));
    }

    private boolean hasMessagingStyle(StatusBarNotification statusBarNotification) {
        return Notification.MessagingStyle.class.equals(statusBarNotification.getNotification().getNotificationStyle());
    }

    private boolean isCategoryMessage(StatusBarNotification statusBarNotification) {
        return Notification.CATEGORY_MESSAGE.equals(statusBarNotification.getNotification().category);
    }
}
