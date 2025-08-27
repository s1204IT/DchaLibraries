package com.mediatek.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.Iterator;
import java.util.List;
import mediatek.net.wifi.HotspotClient;
import mediatek.net.wifi.WifiHotspotManager;

/* loaded from: classes.dex */
public class WifiTetherUserPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private static final IntentFilter WIFI_TETHER_USER_CHANGED_FILTER = new IntentFilter("android.net.wifi.WIFI_HOTSPOT_CLIENTS_IP_READY");
    private Preference mBlockedPrefer;
    private Preference mConnectedPrefer;
    private final WifiHotspotManager mHotspotManager;
    final BroadcastReceiver mReceiver;

    static {
        WIFI_TETHER_USER_CHANGED_FILTER.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
    }

    public WifiTetherUserPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mReceiver = new BroadcastReceiver() { // from class: com.mediatek.settings.wifi.tether.WifiTetherUserPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                WifiTetherUserPreferenceController.this.handleWifiApClientsChanged();
            }
        };
        this.mHotspotManager = ((WifiManager) context.getSystemService("wifi")).getWifiHotspotManager();
        lifecycle.addObserver(this);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mConnectedPrefer = preferenceScreen.findPreference("wifi_tether_connected_user");
        this.mBlockedPrefer = preferenceScreen.findPreference("wifi_tether_blocked_user");
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        this.mContext.registerReceiver(this.mReceiver, WIFI_TETHER_USER_CHANGED_FILTER);
        handleWifiApClientsChanged();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void handleWifiApClientsChanged() {
        int i;
        int i2;
        List hotspotClients = this.mHotspotManager.getHotspotClients();
        if (hotspotClients != null) {
            Iterator it = hotspotClients.iterator();
            i = 0;
            i2 = 0;
            while (it.hasNext()) {
                if (((HotspotClient) it.next()).isBlocked) {
                    i++;
                } else {
                    i2++;
                }
            }
        } else {
            i = 0;
            i2 = 0;
        }
        if (this.mBlockedPrefer != null) {
            this.mBlockedPrefer.setSummary(String.valueOf(i));
            if (i == 0) {
                this.mBlockedPrefer.setEnabled(false);
            } else {
                this.mBlockedPrefer.setEnabled(true);
            }
        }
        if (this.mConnectedPrefer != null) {
            this.mConnectedPrefer.setSummary(String.valueOf(i2));
            if (i2 == 0) {
                this.mConnectedPrefer.setEnabled(false);
            } else {
                this.mConnectedPrefer.setEnabled(true);
            }
        }
    }
}
