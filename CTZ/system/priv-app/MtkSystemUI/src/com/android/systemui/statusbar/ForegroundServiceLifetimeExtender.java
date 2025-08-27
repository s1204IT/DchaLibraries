package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationLifetimeExtender;

/* loaded from: classes.dex */
public class ForegroundServiceLifetimeExtender implements NotificationLifetimeExtender {

    @VisibleForTesting
    static final int MIN_FGS_TIME_MS = 5000;
    private NotificationLifetimeExtender.NotificationSafeToRemoveCallback mNotificationSafeToRemoveCallback;
    private ArraySet<NotificationData.Entry> mManagedEntries = new ArraySet<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    ForegroundServiceLifetimeExtender() {
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public void setCallback(NotificationLifetimeExtender.NotificationSafeToRemoveCallback notificationSafeToRemoveCallback) {
        this.mNotificationSafeToRemoveCallback = notificationSafeToRemoveCallback;
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public boolean shouldExtendLifetime(NotificationData.Entry entry) {
        return (entry.notification.getNotification().flags & 64) != 0 && System.currentTimeMillis() - entry.notification.getPostTime() < 5000;
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public boolean shouldExtendLifetimeForPendingNotification(NotificationData.Entry entry) {
        return shouldExtendLifetime(entry);
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public void setShouldManageLifetime(final NotificationData.Entry entry, boolean z) {
        if (!z) {
            this.mManagedEntries.remove(entry);
            return;
        }
        this.mManagedEntries.add(entry);
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.statusbar.-$$Lambda$ForegroundServiceLifetimeExtender$Mvrg70o5Dvq2zdoQZB_HrCnGC_w
            @Override // java.lang.Runnable
            public final void run() {
                ForegroundServiceLifetimeExtender.lambda$setShouldManageLifetime$0(this.f$0, entry);
            }
        }, 5000 - (System.currentTimeMillis() - entry.notification.getPostTime()));
    }

    public static /* synthetic */ void lambda$setShouldManageLifetime$0(ForegroundServiceLifetimeExtender foregroundServiceLifetimeExtender, NotificationData.Entry entry) {
        if (foregroundServiceLifetimeExtender.mManagedEntries.contains(entry)) {
            foregroundServiceLifetimeExtender.mManagedEntries.remove(entry);
            if (foregroundServiceLifetimeExtender.mNotificationSafeToRemoveCallback != null) {
                foregroundServiceLifetimeExtender.mNotificationSafeToRemoveCallback.onSafeToRemove(entry.key);
            }
        }
    }
}
