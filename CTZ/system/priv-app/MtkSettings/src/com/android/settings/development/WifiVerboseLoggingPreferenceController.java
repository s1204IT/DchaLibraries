package com.android.settings.development;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
/* loaded from: classes.dex */
public class WifiVerboseLoggingPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;
    private final WifiManager mWifiManager;

    public WifiVerboseLoggingPreferenceController(Context context) {
        super(context);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_verbose_logging";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mWifiManager.enableVerboseLogging(((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(this.mWifiManager.getVerboseLoggingLevel() > 0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    public void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        this.mWifiManager.enableVerboseLogging(0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
