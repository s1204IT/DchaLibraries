package com.android.phone;

import android.content.Intent;
import android.os.BenesseExtension;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.Phone;
import com.android.settingslib.RestrictedLockUtils;

public class GsmUmtsOptions {
    private static final String BUTTON_APN_EXPAND_KEY = "button_gsm_apn_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String CATEGORY_APN_EXPAND_KEY = "category_gsm_apn_key";
    public static final String EXTRA_SUB_ID = "sub_id";
    private static final String LOG_TAG = "GsmUmtsOptions";
    private RestrictedPreference mButtonAPNExpand;
    Preference mCarrierSettingPref;
    private Preference mCategoryAPNExpand;
    private NetworkOperators mNetworkOperator;
    private PreferenceFragment mPrefFragment;
    private PreferenceScreen mPrefScreen;

    public GsmUmtsOptions(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen, int i, INetworkQueryService iNetworkQueryService) {
        this.mPrefFragment = preferenceFragment;
        this.mPrefScreen = preferenceScreen;
        this.mPrefFragment.addPreferencesFromResource(R.xml.gsm_umts_options);
        this.mButtonAPNExpand = (RestrictedPreference) this.mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        this.mCategoryAPNExpand = this.mPrefScreen.findPreference(CATEGORY_APN_EXPAND_KEY);
        this.mNetworkOperator = (NetworkOperators) this.mPrefScreen.findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        this.mCarrierSettingPref = this.mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        this.mNetworkOperator.initialize();
        update(i, iNetworkQueryService);
    }

    protected void update(final int i, INetworkQueryService iNetworkQueryService) {
        boolean z;
        boolean z2;
        Intent intent;
        RestrictedLockUtils.EnforcedAdmin deviceOwner;
        Phone phone = PhoneGlobals.getPhone(i);
        if (phone == null) {
            return;
        }
        boolean z3 = true;
        if (phone.getPhoneType() != 1) {
            log("Not a GSM phone");
            this.mNetworkOperator.setEnabled(false);
            z2 = true;
            z = false;
        } else {
            log("Not a CDMA phone");
            PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(i);
            z = carrierConfigForSubId.getBoolean("apn_expand_bool") || this.mCategoryAPNExpand == null;
            boolean z4 = carrierConfigForSubId.getBoolean("operator_selection_expand_bool");
            if (!carrierConfigForSubId.getBoolean("csp_enabled_bool")) {
                z3 = z4;
                z2 = carrierConfigForSubId.getBoolean("carrier_settings_enable_bool");
            } else if (phone.isCspPlmnEnabled()) {
                log("[CSP] Enabling Operator Selection menu.");
                this.mNetworkOperator.setEnabled(true);
                z3 = z4;
                z2 = carrierConfigForSubId.getBoolean("carrier_settings_enable_bool");
            } else {
                log("[CSP] Disabling Operator Selection menu.");
                z3 = false;
                z2 = carrierConfigForSubId.getBoolean("carrier_settings_enable_bool");
            }
        }
        if (z) {
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
                    MetricsLogger.action(GsmUmtsOptions.this.mButtonAPNExpand.getContext(), 1212);
                    Intent intent2 = new Intent("android.settings.APN_SETTINGS");
                    intent2.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent2.putExtra(GsmUmtsOptions.EXTRA_SUB_ID, i);
                    GsmUmtsOptions.this.mPrefFragment.startActivity(intent2);
                    return true;
                }
            });
            this.mPrefScreen.addPreference(this.mCategoryAPNExpand);
        } else {
            this.mPrefScreen.removePreference(this.mCategoryAPNExpand);
        }
        if (z3) {
            this.mPrefScreen.addPreference(this.mNetworkOperator);
            this.mNetworkOperator.update(i, iNetworkQueryService);
        } else {
            this.mPrefScreen.removePreference(this.mNetworkOperator);
        }
        if (z2 && ((intent = this.mCarrierSettingPref.getIntent()) == null || intent.getComponent() == null || TextUtils.isEmpty(intent.getComponent().getPackageName()) || TextUtils.isEmpty(intent.getComponent().getClassName()))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Don't add carrier setting for intent=");
            Object obj = intent;
            if (intent == null) {
                obj = "null";
            }
            sb.append(obj);
            log(sb.toString());
            z2 = false;
        }
        if (z2) {
            this.mPrefScreen.addPreference(this.mCarrierSettingPref);
        } else {
            this.mPrefScreen.removePreference(this.mCarrierSettingPref);
        }
    }

    protected boolean preferenceTreeClick(Preference preference) {
        return this.mNetworkOperator.preferenceTreeClick(preference);
    }

    protected void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
