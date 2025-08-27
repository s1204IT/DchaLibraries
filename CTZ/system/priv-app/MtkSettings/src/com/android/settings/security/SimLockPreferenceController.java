package com.android.settings.security;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.settings.core.BasePreferenceController;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class SimLockPreferenceController extends BasePreferenceController {
    private static final String KEY_SIM_LOCK = "sim_lock_settings";
    private final CarrierConfigManager mCarrierConfigManager;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;

    public SimLockPreferenceController(Context context) {
        super(context, KEY_SIM_LOCK);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        this.mSubscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        PersistableBundle config = this.mCarrierConfigManager.getConfig();
        if (!this.mUserManager.isAdminUser() || !isSimIccReady() || config.getBoolean("hide_sim_lock_settings_bool")) {
            return 3;
        }
        return 0;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey());
        if (preferenceFindPreference == null) {
            return;
        }
        preferenceFindPreference.setEnabled(isSimReady());
    }

    private boolean isSimReady() {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
            while (it.hasNext()) {
                int simState = this.mTelephonyManager.getSimState(it.next().getSimSlotIndex());
                if (simState != 1 && simState != 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean isSimIccReady() {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
            while (it.hasNext()) {
                if (this.mTelephonyManager.hasIccCard(it.next().getSimSlotIndex())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
