package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import com.android.settings.R;
import com.android.settings.TetherSettings;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.TetherUtil;

public class HotspotCondition extends Condition {
    private final WifiManager mWifiManager;

    public HotspotCondition(ConditionManager manager) {
        super(manager);
        this.mWifiManager = (WifiManager) this.mManager.getContext().getSystemService(WifiManager.class);
    }

    @Override
    public void refreshState() {
        boolean wifiTetherEnabled = this.mWifiManager.isWifiApEnabled();
        setActive(wifiTetherEnabled);
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(this.mManager.getContext(), R.drawable.ic_hotspot);
    }

    private String getSsid() {
        WifiConfiguration wifiConfig = this.mWifiManager.getWifiApConfiguration();
        if (wifiConfig == null) {
            return this.mManager.getContext().getString(android.R.string.ext_media_unmount_action);
        }
        return wifiConfig.SSID;
    }

    @Override
    public CharSequence getTitle() {
        return this.mManager.getContext().getString(R.string.condition_hotspot_title);
    }

    @Override
    public CharSequence getSummary() {
        return this.mManager.getContext().getString(R.string.condition_hotspot_summary, getSsid());
    }

    @Override
    public CharSequence[] getActions() {
        Context context = this.mManager.getContext();
        if (RestrictedLockUtils.hasBaseUserRestriction(context, "no_config_tethering", UserHandle.myUserId())) {
            return new CharSequence[0];
        }
        return new CharSequence[]{context.getString(R.string.condition_turn_off)};
    }

    @Override
    public void onPrimaryClick() {
        Utils.startWithFragment(this.mManager.getContext(), TetherSettings.class.getName(), null, null, 0, R.string.tether_settings_title_all, null);
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            Context context = this.mManager.getContext();
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(context, "no_config_tethering", UserHandle.myUserId());
            if (admin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin);
                return;
            } else {
                TetherUtil.setWifiTethering(false, context);
                setActive(false);
                return;
            }
        }
        throw new IllegalArgumentException("Unexpected index " + index);
    }

    @Override
    public int getMetricsConstant() {
        return 382;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            ((HotspotCondition) ConditionManager.get(context).getCondition(HotspotCondition.class)).refreshState();
        }
    }
}
