package com.android.phone;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import com.android.phone.PhoneGlobals;

public class GsmUmtsCallOptions extends PreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    private static final String CALL_BARRING_KEY = "call_barring_key";
    private static final String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final String LOG_TAG = "GsmUmtsCallOptions";
    private final boolean DBG = true;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.gsm_umts_call_options);
        SubscriptionInfoHelper subscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        subscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.labelGsmMore_with_label);
        init(getPreferenceScreen(), subscriptionInfoHelper);
        if (subscriptionInfoHelper.getPhone().getPhoneType() != 1) {
            getPreferenceScreen().setEnabled(false);
        }
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static void init(PreferenceScreen preferenceScreen, SubscriptionInfoHelper subscriptionInfoHelper) {
        PersistableBundle carrierConfig;
        preferenceScreen.findPreference(CALL_FORWARDING_KEY).setIntent(subscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class));
        preferenceScreen.findPreference(ADDITIONAL_GSM_SETTINGS_KEY).setIntent(subscriptionInfoHelper.getIntent(GsmUmtsAdditionalCallOptions.class));
        Preference preferenceFindPreference = preferenceScreen.findPreference(CALL_BARRING_KEY);
        if (subscriptionInfoHelper.hasSubId()) {
            carrierConfig = PhoneGlobals.getInstance().getCarrierConfigForSubId(subscriptionInfoHelper.getSubId());
        } else {
            carrierConfig = PhoneGlobals.getInstance().getCarrierConfig();
        }
        if (carrierConfig != null && carrierConfig.getBoolean("call_barring_visibility_bool")) {
            preferenceFindPreference.setIntent(subscriptionInfoHelper.getIntent(GsmUmtsCallBarringOptions.class));
        } else {
            preferenceScreen.removePreference(preferenceFindPreference);
        }
    }

    @Override
    protected void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
