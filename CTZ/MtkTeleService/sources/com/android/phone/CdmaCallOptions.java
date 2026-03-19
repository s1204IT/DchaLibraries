package com.android.phone;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import com.android.phone.PhoneGlobals;

public class CdmaCallOptions extends PreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private static final String BUTTON_VP_KEY = "button_voice_privacy_key";
    private static final String LOG_TAG = "CdmaCallOptions";
    private final boolean DBG = true;
    private SwitchPreference mButtonVoicePrivacy;

    @Override
    protected void onCreate(Bundle bundle) {
        PersistableBundle carrierConfig;
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.cdma_call_privacy);
        SubscriptionInfoHelper subscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        subscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.labelCdmaMore_with_label);
        this.mButtonVoicePrivacy = (SwitchPreference) findPreference(BUTTON_VP_KEY);
        if (subscriptionInfoHelper.hasSubId()) {
            carrierConfig = PhoneGlobals.getInstance().getCarrierConfigForSubId(subscriptionInfoHelper.getSubId());
        } else {
            carrierConfig = PhoneGlobals.getInstance().getCarrierConfig();
        }
        if (subscriptionInfoHelper.getPhone().getPhoneType() != 2 || carrierConfig.getBoolean("voice_privacy_disable_ui_bool")) {
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(BUTTON_VP_KEY)) {
            return true;
        }
        return false;
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
