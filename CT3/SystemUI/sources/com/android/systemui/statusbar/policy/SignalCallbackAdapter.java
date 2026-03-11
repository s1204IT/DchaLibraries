package com.android.systemui.statusbar.policy;

import android.telephony.SubscriptionInfo;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.List;

public class SignalCallbackAdapter implements NetworkController.SignalCallback {
    @Override
    public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
    }

    @Override
    public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int networkIcon, int volteType, int qsType, boolean activityIn, boolean activityOut, String typeContentDescription, String description, boolean isWide, int subId) {
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
    }

    @Override
    public void setNoSims(boolean show) {
    }

    @Override
    public void setEthernetIndicators(NetworkController.IconState icon) {
    }

    @Override
    public void setIsAirplaneMode(NetworkController.IconState icon) {
    }

    @Override
    public void setMobileDataEnabled(boolean enabled) {
    }
}
