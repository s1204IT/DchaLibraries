package com.android.mtp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import com.android.internal.util.Preconditions;
import java.util.HashSet;

public class MtpDocumentsService extends Service {
    private NotificationManager mNotificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mNotificationManager = (NotificationManager) getSystemService(NotificationManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        int[] intArray;
        if (intent != null && !"com.android.mtp.UPDATE_NOTIFICATION".equals(intent.getAction())) {
            return 2;
        }
        Notification[] notificationArrCastToNotifications = null;
        if (intent != null && intent.hasExtra("deviceIds")) {
            intArray = intent.getExtras().getIntArray("deviceIds");
        } else {
            intArray = null;
        }
        if (intent != null && intent.hasExtra("deviceNotifications")) {
            notificationArrCastToNotifications = castToNotifications(intent.getExtras().getParcelableArray("deviceNotifications"));
        }
        return updateForegroundState(intArray, notificationArrCastToNotifications) ? 3 : 2;
    }

    private boolean updateForegroundState(int[] iArr, Notification[] notificationArr) {
        HashSet hashSet = new HashSet();
        int length = iArr != null ? iArr.length : 0;
        if (length != 0) {
            Preconditions.checkArgument(iArr != null);
            Preconditions.checkArgument(notificationArr != null);
            Preconditions.checkArgument(iArr.length == notificationArr.length);
        }
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                startForeground(iArr[i], notificationArr[i]);
            } else {
                this.mNotificationManager.notify(iArr[i], notificationArr[i]);
            }
            hashSet.add(Integer.valueOf(iArr[i]));
        }
        for (StatusBarNotification statusBarNotification : this.mNotificationManager.getActiveNotifications()) {
            if (!hashSet.contains(Integer.valueOf(statusBarNotification.getId()))) {
                this.mNotificationManager.cancel(statusBarNotification.getId());
            }
        }
        if (length != 0) {
            return true;
        }
        stopForeground(true);
        stopSelf();
        return false;
    }

    private static Notification[] castToNotifications(Parcelable[] parcelableArr) {
        Preconditions.checkNotNull(parcelableArr);
        Notification[] notificationArr = new Notification[parcelableArr.length];
        for (int i = 0; i < parcelableArr.length; i++) {
            notificationArr[i] = (Notification) parcelableArr[i];
        }
        return notificationArr;
    }
}
