package com.android.server.notification;

public interface NotificationDelegate {
    void clearEffects();

    void onClearAll(int i, int i2, int i3);

    void onNotificationActionClick(int i, int i2, String str, int i3);

    void onNotificationClear(int i, int i2, String str, String str2, int i3, int i4);

    void onNotificationClick(int i, int i2, String str);

    void onNotificationError(int i, int i2, String str, String str2, int i3, int i4, int i5, String str3, int i6);

    void onNotificationExpansionChanged(String str, boolean z, boolean z2);

    void onNotificationVisibilityChanged(String[] strArr, String[] strArr2);

    void onPanelHidden();

    void onPanelRevealed(boolean z);

    void onSetDisabled(int i);
}
