package com.android.systemui;

import android.service.notification.StatusBarNotification;
import android.util.ArraySet;

/* loaded from: classes.dex */
public interface ForegroundServiceController {
    void addNotification(StatusBarNotification statusBarNotification, int i);

    ArraySet<Integer> getAppOps(int i, String str);

    String getStandardLayoutKey(int i, String str);

    boolean isDungeonNeededForUser(int i);

    boolean isDungeonNotification(StatusBarNotification statusBarNotification);

    boolean isSystemAlertNotification(StatusBarNotification statusBarNotification);

    boolean isSystemAlertWarningNeeded(int i, String str);

    void onAppOpChanged(int i, int i2, String str, boolean z);

    boolean removeNotification(StatusBarNotification statusBarNotification);

    void updateNotification(StatusBarNotification statusBarNotification, int i);
}
