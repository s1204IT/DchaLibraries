package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class P2pThisDevicePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private Preference mPreference;

    public P2pThisDevicePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "p2p_this_device";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    public void setEnabled(boolean z) {
        if (this.mPreference != null) {
            this.mPreference.setEnabled(z);
        }
    }

    public void updateDeviceName(WifiP2pDevice wifiP2pDevice) {
        if (this.mPreference != null && wifiP2pDevice != null) {
            if (TextUtils.isEmpty(wifiP2pDevice.deviceName)) {
                this.mPreference.setTitle(wifiP2pDevice.deviceAddress);
            } else {
                this.mPreference.setTitle(wifiP2pDevice.deviceName);
            }
        }
    }
}
