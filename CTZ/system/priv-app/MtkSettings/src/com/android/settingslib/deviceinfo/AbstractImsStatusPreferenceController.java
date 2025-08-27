package com.android.settingslib.deviceinfo;

import android.os.PersistableBundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.settingslib.R;

/* loaded from: classes.dex */
public abstract class AbstractImsStatusPreferenceController extends AbstractConnectivityPreferenceController {
    private static final String[] CONNECTIVITY_INTENTS = {"android.bluetooth.adapter.action.STATE_CHANGED", "android.net.conn.CONNECTIVITY_CHANGE", "android.net.wifi.LINK_CONFIGURATION_CHANGED", "android.net.wifi.STATE_CHANGE"};
    static final String KEY_IMS_REGISTRATION_STATE = "ims_reg_state";
    private Preference mImsStatus;

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService(CarrierConfigManager.class);
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (carrierConfigManager != null) {
            configForSubId = carrierConfigManager.getConfigForSubId(defaultDataSubscriptionId);
        } else {
            configForSubId = null;
        }
        return configForSubId != null && configForSubId.getBoolean("show_ims_registration_status_bool");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_IMS_REGISTRATION_STATE;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mImsStatus = preferenceScreen.findPreference(KEY_IMS_REGISTRATION_STATE);
        updateConnectivity();
    }

    @Override // com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController
    protected String[] getConnectivityIntents() {
        return CONNECTIVITY_INTENTS;
    }

    @Override // com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController
    protected void updateConnectivity() {
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (this.mImsStatus != null) {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            this.mImsStatus.setSummary((telephonyManager == null || !telephonyManager.isImsRegistered(defaultDataSubscriptionId)) ? R.string.ims_reg_status_not_registered : R.string.ims_reg_status_registered);
        }
    }
}
