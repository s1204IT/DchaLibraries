package com.android.launcher3.badge;

import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.List;

public class BadgeInfo {
    public static final int MAX_COUNT = 999;
    private List<NotificationKeyData> mNotificationKeys = new ArrayList();
    private PackageUserKey mPackageUserKey;
    private int mTotalCount;

    public BadgeInfo(PackageUserKey packageUserKey) {
        this.mPackageUserKey = packageUserKey;
    }

    public boolean addOrUpdateNotificationKey(NotificationKeyData notificationKeyData) {
        int iIndexOf = this.mNotificationKeys.indexOf(notificationKeyData);
        NotificationKeyData notificationKeyData2 = iIndexOf == -1 ? null : this.mNotificationKeys.get(iIndexOf);
        if (notificationKeyData2 != null) {
            if (notificationKeyData2.count == notificationKeyData.count) {
                return false;
            }
            this.mTotalCount -= notificationKeyData2.count;
            this.mTotalCount += notificationKeyData.count;
            notificationKeyData2.count = notificationKeyData.count;
            return true;
        }
        boolean zAdd = this.mNotificationKeys.add(notificationKeyData);
        if (zAdd) {
            this.mTotalCount += notificationKeyData.count;
        }
        return zAdd;
    }

    public boolean removeNotificationKey(NotificationKeyData notificationKeyData) {
        boolean zRemove = this.mNotificationKeys.remove(notificationKeyData);
        if (zRemove) {
            this.mTotalCount -= notificationKeyData.count;
        }
        return zRemove;
    }

    public List<NotificationKeyData> getNotificationKeys() {
        return this.mNotificationKeys;
    }

    public int getNotificationCount() {
        return Math.min(this.mTotalCount, MAX_COUNT);
    }

    public boolean shouldBeInvalidated(BadgeInfo badgeInfo) {
        return this.mPackageUserKey.equals(badgeInfo.mPackageUserKey) && getNotificationCount() != badgeInfo.getNotificationCount();
    }
}
