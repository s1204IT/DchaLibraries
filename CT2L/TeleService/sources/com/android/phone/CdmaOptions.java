package com.android.phone;

import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class CdmaOptions {
    private PreferenceScreen mButtonAPNExpand;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;
    private CdmaSystemSelectListPreference mButtonCdmaSystemSelect;
    private Phone mPhone;
    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;

    public CdmaOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, Phone phone) {
        this.mPrefActivity = prefActivity;
        this.mPrefScreen = prefScreen;
        this.mPhone = phone;
        create();
    }

    protected void create() {
        Preference pref;
        this.mPrefActivity.addPreferencesFromResource(R.xml.cdma_options);
        this.mButtonAPNExpand = (PreferenceScreen) this.mPrefScreen.findPreference("button_apn_key_cdma");
        boolean removedAPNExpand = false;
        Resources res = this.mPrefActivity.getResources();
        if (!res.getBoolean(R.bool.config_show_apn_setting_cdma) && this.mButtonAPNExpand != null) {
            this.mPrefScreen.removePreference(this.mButtonAPNExpand);
            removedAPNExpand = true;
        }
        if (!removedAPNExpand) {
            this.mButtonAPNExpand.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("android.settings.APN_SETTINGS");
                    intent.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent.putExtra("sub_id", CdmaOptions.this.mPhone.getSubId());
                    CdmaOptions.this.mPrefActivity.startActivity(intent);
                    return true;
                }
            });
        }
        this.mButtonCdmaSystemSelect = (CdmaSystemSelectListPreference) this.mPrefScreen.findPreference("cdma_system_select_key");
        this.mButtonCdmaSubscription = (CdmaSubscriptionListPreference) this.mPrefScreen.findPreference("cdma_subscription_key");
        this.mButtonCdmaSystemSelect.setEnabled(true);
        if (deviceSupportsNvAndRuim()) {
            log("Both NV and Ruim supported, ENABLE subscription type selection");
            this.mButtonCdmaSubscription.setEnabled(true);
        } else {
            log("Both NV and Ruim NOT supported, REMOVE subscription type selection");
            this.mPrefScreen.removePreference(this.mPrefScreen.findPreference("cdma_subscription_key"));
        }
        boolean voiceCapable = this.mPrefActivity.getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable);
        boolean isLTE = this.mPhone.getLteOnCdmaMode() == 1;
        if (voiceCapable || isLTE) {
            this.mPrefScreen.removePreference(this.mPrefScreen.findPreference("cdma_activate_device_key"));
        }
        boolean isCarrierSettingsEnabled = this.mPrefActivity.getResources().getBoolean(R.bool.config_carrier_settings_enable);
        if (!isCarrierSettingsEnabled && (pref = this.mPrefScreen.findPreference("carrier_settings_key")) != null) {
            this.mPrefScreen.removePreference(pref);
        }
    }

    private boolean deviceSupportsNvAndRuim() {
        String subscriptionsSupported = SystemProperties.get("ril.subscription.types");
        boolean nvSupported = false;
        boolean ruimSupported = false;
        log("deviceSupportsnvAnRum: prop=" + subscriptionsSupported);
        if (!TextUtils.isEmpty(subscriptionsSupported)) {
            String[] arr$ = subscriptionsSupported.split(",");
            for (String str : arr$) {
                String subscriptionType = str.trim();
                if (subscriptionType.equalsIgnoreCase("NV")) {
                    nvSupported = true;
                }
                if (subscriptionType.equalsIgnoreCase("RUIM")) {
                    ruimSupported = true;
                }
            }
        }
        log("deviceSupportsnvAnRum: nvSupported=" + nvSupported + " ruimSupported=" + ruimSupported);
        return nvSupported && ruimSupported;
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("cdma_system_select_key")) {
            log("preferenceTreeClick: return BUTTON_CDMA_ROAMING_KEY true");
            return true;
        }
        if (preference.getKey().equals("cdma_subscription_key")) {
            log("preferenceTreeClick: return CDMA_SUBSCRIPTION_KEY true");
            return true;
        }
        return false;
    }

    public void showDialog(Preference preference) {
        if (preference.getKey().equals("cdma_system_select_key")) {
            this.mButtonCdmaSystemSelect.showDialog(null);
        } else if (preference.getKey().equals("cdma_subscription_key")) {
            this.mButtonCdmaSubscription.showDialog(null);
        }
    }

    protected void log(String s) {
        Log.d("CdmaOptions", s);
    }
}
