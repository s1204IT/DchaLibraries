package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationLifetimeExtender;

public class ForegroundServiceLifetimeExtender implements NotificationLifetimeExtender {

    @VisibleForTesting
    static final int MIN_FGS_TIME_MS = 5000;
    private NotificationLifetimeExtender.NotificationSafeToRemoveCallback mNotificationSafeToRemoveCallback;
    private ArraySet<NotificationData.Entry> mManagedEntries = new ArraySet<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    ForegroundServiceLifetimeExtender() {
    }

    @Override
    public void setCallback(NotificationLifetimeExtender.NotificationSafeToRemoveCallback notificationSafeToRemoveCallback) {
        this.mNotificationSafeToRemoveCallback = notificationSafeToRemoveCallback;
    }

    @Override
    public boolean shouldExtendLifetime(NotificationData.Entry entry) {
        return (entry.notification.getNotification().flags & 64) != 0 && System.currentTimeMillis() - entry.notification.getPostTime() < 5000;
    }

    @Override
    public boolean shouldExtendLifetimeForPendingNotification(NotificationData.Entry entry) {
        return shouldExtendLifetime(entry);
    }

    @Override
    public void setShouldManageLifetime(final NotificationData.Entry entry, boolean z) {
        if (!z) {
            this.mManagedEntries.remove(entry);
            return;
        }
        this.mManagedEntries.add(entry);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                ForegroundServiceLifetimeExtender.lambda$setShouldManageLifetime$0(this.f$0, entry);
            }
        }, 5000 - (System.currentTimeMillis() - entry.notification.getPostTime()));
    }

    public static void lambda$setShouldManageLifetime$0(ForegroundServiceLifetimeExtender foregroundServiceLifetimeExtender, NotificationData.Entry entry) {
        if (foregroundServiceLifetimeExtender.mManagedEntries.contains(entry)) {
            foregroundServiceLifetimeExtender.mManagedEntries.remove(entry);
            if (foregroundServiceLifetimeExtender.mNotificationSafeToRemoveCallback != null) {
                foregroundServiceLifetimeExtender.mNotificationSafeToRemoveCallback.onSafeToRemove(entry.key);
            }
        }
    }
}
