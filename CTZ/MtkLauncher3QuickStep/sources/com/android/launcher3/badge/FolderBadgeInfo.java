package com.android.launcher3.badge;

import com.android.launcher3.Utilities;

public class FolderBadgeInfo extends BadgeInfo {
    private static final int MIN_COUNT = 0;
    private int mNumNotifications;

    public FolderBadgeInfo() {
        super(null);
    }

    public void addBadgeInfo(BadgeInfo badgeInfo) {
        if (badgeInfo == null) {
            return;
        }
        this.mNumNotifications += badgeInfo.getNotificationKeys().size();
        this.mNumNotifications = Utilities.boundToRange(this.mNumNotifications, 0, BadgeInfo.MAX_COUNT);
    }

    public void subtractBadgeInfo(BadgeInfo badgeInfo) {
        if (badgeInfo == null) {
            return;
        }
        this.mNumNotifications -= badgeInfo.getNotificationKeys().size();
        this.mNumNotifications = Utilities.boundToRange(this.mNumNotifications, 0, BadgeInfo.MAX_COUNT);
    }

    @Override
    public int getNotificationCount() {
        return 0;
    }

    public boolean hasBadge() {
        return this.mNumNotifications > 0;
    }
}
