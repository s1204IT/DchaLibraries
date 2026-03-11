package com.android.settings.wifi;

import android.content.Intent;
import android.support.v14.preference.PreferenceFragment;
import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.wifi.p2p.WifiP2pSettings;

public class WifiPickerActivity extends SettingsActivity implements ButtonBarHandler {
    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        if (!intent.hasExtra(":settings:show_fragment")) {
            intent.putExtra(":settings:show_fragment", getWifiSettingsClass().getName());
            intent.putExtra(":settings:show_fragment_title_resid", R.string.wifi_select_network);
        }
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        if (WifiSettings.class.getName().equals(str) || WifiP2pSettings.class.getName().equals(str) || SavedAccessPointsWifiSettings.class.getName().equals(str)) {
            return true;
        }
        return false;
    }

    Class<? extends PreferenceFragment> getWifiSettingsClass() {
        return WifiSettings.class;
    }
}
