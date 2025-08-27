package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import com.android.settings.R;
import com.android.settings.TetherSettings;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class HotspotCondition extends Condition {
    private static final IntentFilter WIFI_AP_STATE_FILTER = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    private final Receiver mReceiver;
    private final WifiManager mWifiManager;

    public HotspotCondition(ConditionManager conditionManager) {
        super(conditionManager);
        this.mWifiManager = (WifiManager) this.mManager.getContext().getSystemService(WifiManager.class);
        this.mReceiver = new Receiver();
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void refreshState() {
        setActive(this.mWifiManager.isWifiApEnabled());
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    protected BroadcastReceiver getReceiver() {
        return this.mReceiver;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    protected IntentFilter getIntentFilter() {
        return WIFI_AP_STATE_FILTER;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public Drawable getIcon() {
        return this.mManager.getContext().getDrawable(R.drawable.ic_hotspot);
    }

    private String getSsid() {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        if (wifiApConfiguration == null) {
            return this.mManager.getContext().getString(android.R.string.permdesc_callCompanionApp);
        }
        return wifiApConfiguration.SSID;
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_hotspot_title);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_hotspot_summary, getSsid());
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        Context context = this.mManager.getContext();
        if (RestrictedLockUtils.hasBaseUserRestriction(context, "no_config_tethering", UserHandle.myUserId())) {
            return new CharSequence[0];
        }
        return new CharSequence[]{context.getString(R.string.condition_turn_off)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        new SubSettingLauncher(this.mManager.getContext()).setDestination(TetherSettings.class.getName()).setSourceMetricsCategory(35).setTitle(R.string.tether_settings_title_all).addFlags(268435456).launch();
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        if (i == 0) {
            Context context = this.mManager.getContext();
            RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(context, "no_config_tethering", UserHandle.myUserId());
            if (enforcedAdminCheckIfRestrictionEnforced != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, enforcedAdminCheckIfRestrictionEnforced);
                return;
            } else {
                ((ConnectivityManager) context.getSystemService("connectivity")).stopTethering(0);
                setActive(false);
                return;
            }
        }
        throw new IllegalArgumentException("Unexpected index " + i);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public int getMetricsConstant() {
        return 382;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
                ((HotspotCondition) ConditionManager.get(context).getCondition(HotspotCondition.class)).refreshState();
            }
        }
    }
}
