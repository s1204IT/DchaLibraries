package com.mediatek.settings.wifi.tether;

import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;

public class WifiTetherUserListSettingsBlocked extends WifiTetherUserListSettings {
    private int mUserMode = 0;

    @Override
    public int getMetricsCategory() {
        return 1014;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_tether_user_settings_blocked;
    }

    @Override
    protected String getLogTag() {
        return "WifiTetherUserListSettingsBlocked";
    }
}
