package com.mediatek.settings.wifi.tether;

import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;

/* loaded from: classes.dex */
public class WifiTetherUserListSettingsConnected extends WifiTetherUserListSettings {
    private int mUserMode = 0;

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1014;
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_tether_user_settings_connected;
    }

    @Override // com.mediatek.settings.wifi.tether.WifiTetherUserListSettings, com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "WifiTetherUserListSettingsConnected";
    }
}
