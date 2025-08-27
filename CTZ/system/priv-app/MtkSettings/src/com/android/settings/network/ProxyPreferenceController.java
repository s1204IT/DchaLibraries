package com.android.settings.network;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class ProxyPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public ProxyPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("proxy_settings");
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setEnabled(((DevicePolicyManager) this.mContext.getSystemService("device_policy")).getGlobalProxyAdmin() == null);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "proxy_settings";
    }
}
