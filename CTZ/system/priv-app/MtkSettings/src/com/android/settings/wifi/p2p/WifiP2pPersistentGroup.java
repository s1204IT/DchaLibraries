package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.support.v7.preference.Preference;

/* loaded from: classes.dex */
public class WifiP2pPersistentGroup extends Preference {
    public WifiP2pGroup mGroup;

    public WifiP2pPersistentGroup(Context context, WifiP2pGroup wifiP2pGroup) {
        super(context);
        this.mGroup = wifiP2pGroup;
        setTitle(this.mGroup.getNetworkName());
    }

    int getNetworkId() {
        return this.mGroup.getNetworkId();
    }

    String getGroupName() {
        return this.mGroup.getNetworkName();
    }
}
