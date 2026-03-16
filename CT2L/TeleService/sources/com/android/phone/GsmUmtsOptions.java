package com.android.phone;

import android.content.Intent;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.telephony.PhoneFactory;

public class GsmUmtsOptions {
    private PreferenceScreen mButtonAPNExpand;
    private PreferenceScreen mButtonOperatorSelectionExpand;
    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;
    private int mSubId;

    public GsmUmtsOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, int subId) {
        this.mPrefActivity = prefActivity;
        this.mPrefScreen = prefScreen;
        this.mSubId = subId;
        create();
    }

    protected void create() {
        Preference pref;
        this.mPrefActivity.addPreferencesFromResource(R.xml.gsm_umts_options);
        this.mButtonAPNExpand = (PreferenceScreen) this.mPrefScreen.findPreference("button_apn_key");
        boolean removedAPNExpand = false;
        boolean removeOperatorSelectionExpand = false;
        this.mButtonOperatorSelectionExpand = (PreferenceScreen) this.mPrefScreen.findPreference("button_carrier_sel_key");
        if (PhoneFactory.getDefaultPhone().getPhoneType() != 1) {
            log("Not a GSM phone");
            this.mButtonAPNExpand.setEnabled(false);
            this.mButtonOperatorSelectionExpand.setEnabled(false);
        } else {
            log("Not a CDMA phone");
            Resources res = this.mPrefActivity.getResources();
            if (!res.getBoolean(R.bool.config_apn_expand) && this.mButtonAPNExpand != null) {
                this.mPrefScreen.removePreference(this.mButtonAPNExpand);
                removedAPNExpand = true;
            }
            if (!res.getBoolean(R.bool.config_operator_selection_expand)) {
                this.mPrefScreen.removePreference(this.mPrefScreen.findPreference("button_carrier_sel_key"));
                removeOperatorSelectionExpand = true;
            }
            if (res.getBoolean(R.bool.csp_enabled)) {
                if (PhoneFactory.getDefaultPhone().isCspPlmnEnabled()) {
                    log("[CSP] Enabling Operator Selection menu.");
                    this.mButtonOperatorSelectionExpand.setEnabled(true);
                } else {
                    log("[CSP] Disabling Operator Selection menu.");
                    this.mPrefScreen.removePreference(this.mPrefScreen.findPreference("button_carrier_sel_key"));
                    removeOperatorSelectionExpand = true;
                }
            }
            boolean isCarrierSettingsEnabled = this.mPrefActivity.getResources().getBoolean(R.bool.config_carrier_settings_enable);
            if (!isCarrierSettingsEnabled && (pref = this.mPrefScreen.findPreference("carrier_settings_key")) != null) {
                this.mPrefScreen.removePreference(pref);
            }
        }
        if (!removedAPNExpand) {
            this.mButtonAPNExpand.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent("android.settings.APN_SETTINGS");
                    intent.putExtra(":settings:show_fragment_as_subsetting", true);
                    intent.putExtra("sub_id", GsmUmtsOptions.this.mSubId);
                    GsmUmtsOptions.this.mPrefActivity.startActivity(intent);
                    return true;
                }
            });
        }
        if (!removeOperatorSelectionExpand) {
            Intent intent = this.mButtonOperatorSelectionExpand.getIntent();
            intent.putExtra("sub_id", this.mSubId);
            this.mButtonOperatorSelectionExpand.setIntent(intent);
        }
    }

    public boolean preferenceTreeClick(Preference preference) {
        log("preferenceTreeClick: return false");
        return false;
    }

    protected void log(String s) {
        Log.d("GsmUmtsOptions", s);
    }
}
