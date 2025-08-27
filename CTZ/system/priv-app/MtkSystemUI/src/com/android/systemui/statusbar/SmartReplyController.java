package com.android.systemui.statusbar;

import android.os.RemoteException;
import android.util.ArraySet;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationData;
import java.util.Set;

/* loaded from: classes.dex */
public class SmartReplyController {
    private Set<String> mSendingKeys = new ArraySet();
    private IStatusBarService mBarService = (IStatusBarService) Dependency.get(IStatusBarService.class);

    public void smartReplySent(NotificationData.Entry entry, int i, CharSequence charSequence) {
        NotificationEntryManager notificationEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        notificationEntryManager.updateNotification(notificationEntryManager.rebuildNotificationWithRemoteInput(entry, charSequence, true), null);
        this.mSendingKeys.add(entry.key);
        try {
            this.mBarService.onNotificationSmartReplySent(entry.notification.getKey(), i);
        } catch (RemoteException e) {
        }
    }

    public boolean isSendingSmartReply(String str) {
        return this.mSendingKeys.contains(str);
    }

    public void smartRepliesAdded(NotificationData.Entry entry, int i) {
        try {
            this.mBarService.onNotificationSmartRepliesAdded(entry.notification.getKey(), i);
        } catch (RemoteException e) {
        }
    }

    public void stopSending(NotificationData.Entry entry) {
        if (entry != null) {
            this.mSendingKeys.remove(entry.notification.getKey());
        }
    }
}
