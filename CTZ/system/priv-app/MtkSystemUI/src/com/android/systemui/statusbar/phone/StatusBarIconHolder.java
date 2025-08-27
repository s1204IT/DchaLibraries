package com.android.systemui.statusbar.phone;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;

/* loaded from: classes.dex */
public class StatusBarIconHolder {
    private StatusBarIcon mIcon;
    private StatusBarSignalPolicy.MobileIconState mMobileState;
    private StatusBarSignalPolicy.WifiIconState mWifiState;
    private int mType = 0;
    private int mTag = 0;
    private boolean mVisible = true;

    public static StatusBarIconHolder fromIcon(StatusBarIcon statusBarIcon) {
        StatusBarIconHolder statusBarIconHolder = new StatusBarIconHolder();
        statusBarIconHolder.mIcon = statusBarIcon;
        return statusBarIconHolder;
    }

    public static StatusBarIconHolder fromWifiIconState(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        StatusBarIconHolder statusBarIconHolder = new StatusBarIconHolder();
        statusBarIconHolder.mWifiState = wifiIconState;
        statusBarIconHolder.mType = 1;
        return statusBarIconHolder;
    }

    public static StatusBarIconHolder fromMobileIconState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        StatusBarIconHolder statusBarIconHolder = new StatusBarIconHolder();
        statusBarIconHolder.mMobileState = mobileIconState;
        statusBarIconHolder.mType = 2;
        statusBarIconHolder.mTag = mobileIconState.subId;
        return statusBarIconHolder;
    }

    public int getType() {
        return this.mType;
    }

    public StatusBarIcon getIcon() {
        return this.mIcon;
    }

    public StatusBarSignalPolicy.WifiIconState getWifiState() {
        return this.mWifiState;
    }

    public void setWifiState(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        this.mWifiState = wifiIconState;
    }

    public StatusBarSignalPolicy.MobileIconState getMobileState() {
        return this.mMobileState;
    }

    public void setMobileState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        this.mMobileState = mobileIconState;
    }

    public boolean isVisible() {
        switch (this.mType) {
            case 0:
                return this.mIcon.visible;
            case 1:
                return this.mWifiState.visible;
            case 2:
                return this.mMobileState.visible;
            default:
                return true;
        }
    }

    public void setVisible(boolean z) {
        if (isVisible() == z) {
        }
        switch (this.mType) {
            case 0:
                this.mIcon.visible = z;
                break;
            case 1:
                this.mWifiState.visible = z;
                break;
            case 2:
                this.mMobileState.visible = z;
                break;
        }
    }

    public int getTag() {
        return this.mTag;
    }
}
