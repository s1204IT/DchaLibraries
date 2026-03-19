package com.android.phone;

import android.content.Intent;
import android.os.BenesseExtension;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.Phone;
import com.android.settingslib.RestrictedLockUtils;

public class CdmaOptions {
    private static final String BUTTON_APN_EXPAND_KEY = "button_cdma_apn_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String CATEGORY_APN_EXPAND_KEY = "category_cdma_apn_key";
    private static final String LOG_TAG = "CdmaOptions";
    private RestrictedPreference mButtonAPNExpand;
    private Preference mButtonCarrierSettings;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;
    private CdmaSystemSelectListPreference mButtonCdmaSystemSelect;
    private Preference mCategoryAPNExpand;
    private Phone mPhone;
    private PreferenceFragment mPrefFragment;
    private PreferenceScreen mPrefScreen;

    @VisibleForTesting
    public CdmaOptions(Phone phone) {
        this.mPhone = phone;
    }

    public CdmaOptions(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen, Phone phone) {
        this.mPrefFragment = preferenceFragment;
        this.mPrefScreen = preferenceScreen;
        this.mPrefFragment.addPreferencesFromResource(R.xml.cdma_options);
        this.mButtonCdmaSystemSelect = (CdmaSystemSelectListPreference) this.mPrefScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
        this.mButtonCdmaSubscription = (CdmaSubscriptionListPreference) this.mPrefScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY);
        this.mButtonCarrierSettings = this.mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        this.mButtonAPNExpand = (RestrictedPreference) this.mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        this.mCategoryAPNExpand = this.mPrefScreen.findPreference(CATEGORY_APN_EXPAND_KEY);
        update(phone);
        this.mButtonCdmaSystemSelect.setPhone(this.mPhone);
        this.mButtonCdmaSubscription.setPhone(this.mPhone);
    }

    protected void update(Phone phone) {
        Intent intent;
        RestrictedLockUtils.EnforcedAdmin deviceOwner;
        this.mPhone = phone;
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        boolean zDeviceSupportsNvAndRuim = deviceSupportsNvAndRuim();
        boolean z = carrierConfigForSubId.getBoolean("carrier_settings_enable_bool");
        this.mPrefScreen.addPreference(this.mButtonCdmaSystemSelect);
        this.mButtonCdmaSystemSelect.setEnabled(true);
        if (!isWorldMode() || this.mButtonAPNExpand == null) {
            log("update: addAPNExpand");
            RestrictedPreference restrictedPreference = this.mButtonAPNExpand;
            if (MobileNetworkSettings.isDpcApnEnforced(this.mButtonAPNExpand.getContext())) {
                deviceOwner = RestrictedLockUtils.getDeviceOwner(this.mButtonAPNExpand.getContext());
            } else {
                deviceOwner = null;
            }
            restrictedPreference.setDisabledByAdmin(deviceOwner);
            this.mButtonAPNExpand.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (BenesseExtension.getDchaState() != 0) {
                        return true;
                    }
                    MetricsLogger.action(CdmaOptions.this.mButtonAPNExpand.getContext(), 1212);
                    Intent intent2 = new Intent("android.settings.APN_SETTINGS");
                    intent2.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent2.putExtra(GsmUmtsOptions.EXTRA_SUB_ID, CdmaOptions.this.mPhone.getSubId());
                    CdmaOptions.this.mPrefFragment.startActivity(intent2);
                    return true;
                }
            });
            this.mPrefScreen.addPreference(this.mCategoryAPNExpand);
        } else {
            this.mPrefScreen.removePreference(this.mCategoryAPNExpand);
        }
        if (zDeviceSupportsNvAndRuim) {
            log("Both NV and Ruim supported, ENABLE subscription type selection");
            this.mPrefScreen.addPreference(this.mButtonCdmaSubscription);
            this.mButtonCdmaSubscription.setEnabled(true);
        } else {
            log("Both NV and Ruim NOT supported, REMOVE subscription type selection");
            this.mPrefScreen.removePreference(this.mButtonCdmaSubscription);
        }
        if (z && ((intent = this.mButtonCarrierSettings.getIntent()) == null || intent.getComponent() == null || TextUtils.isEmpty(intent.getComponent().getPackageName()) || TextUtils.isEmpty(intent.getComponent().getClassName()))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Don't add carrier setting for intent=");
            Object obj = intent;
            if (intent == null) {
                obj = "null";
            }
            sb.append(obj);
            log(sb.toString());
            z = false;
        }
        if (z) {
            this.mPrefScreen.addPreference(this.mButtonCarrierSettings);
        } else {
            this.mPrefScreen.removePreference(this.mButtonCarrierSettings);
        }
    }

    @VisibleForTesting
    public boolean shouldAddApnExpandPreference(PersistableBundle persistableBundle) {
        if (this.mPhone.getPhoneType() == 2 && persistableBundle.getBoolean("show_apn_setting_cdma_bool")) {
            return true;
        }
        return false;
    }

    private boolean deviceSupportsNvAndRuim() {
        boolean z;
        boolean z2;
        String str = SystemProperties.get("ril.subscription.types");
        log("deviceSupportsnvAnRum: prop=" + str);
        if (!TextUtils.isEmpty(str)) {
            z = false;
            z2 = false;
            for (String str2 : str.split(",")) {
                String strTrim = str2.trim();
                if (strTrim.equalsIgnoreCase("NV")) {
                    z = true;
                }
                if (strTrim.equalsIgnoreCase("RUIM")) {
                    z2 = true;
                }
            }
        } else {
            z = false;
            z2 = false;
        }
        log("deviceSupportsnvAnRum: nvSupported=" + z + " ruimSupported=" + z2);
        return z && z2;
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        }
        if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
        return false;
    }

    public void showDialog(Preference preference) {
        try {
            if (preference.getKey().equals(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
                this.mButtonCdmaSystemSelect.showDialog(null);
            } else if (preference.getKey().equals(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
                this.mButtonCdmaSubscription.showDialog(null);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Preference not available");
        }
    }

    protected void log(String str) {
        Log.d(LOG_TAG, str);
    }

    private boolean isWorldMode() {
        String[] strArrSplit;
        TelephonyManager telephonyManager = (TelephonyManager) this.mPrefFragment.getContext().getSystemService("phone");
        String string = this.mPrefFragment.getResources().getString(R.string.config_world_mode);
        boolean z = false;
        if (!TextUtils.isEmpty(string) && (strArrSplit = string.split(";")) != null && ((strArrSplit.length == 1 && strArrSplit[0].equalsIgnoreCase("true")) || (strArrSplit.length == 2 && !TextUtils.isEmpty(strArrSplit[1]) && telephonyManager != null && strArrSplit[1].equalsIgnoreCase(telephonyManager.getGroupIdLevel1())))) {
            z = true;
        }
        log("isWorldMode=" + z);
        return z;
    }
}
