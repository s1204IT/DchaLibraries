package com.mediatek.settings.vtss;

import android.content.Intent;
import android.preference.Preference;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;

public class GsmUmtsVTCFOptions extends GsmUmtsVTOptions {
    @Override
    protected void setActionBar(SubscriptionInfoHelper subscriptionInfoHelper) {
        subscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_forwarding_settings_with_label);
    }

    @Override
    protected void init(SubscriptionInfoHelper subscriptionInfoHelper) {
        addPreferencesFromResource(R.xml.mtk_gsm_umts_vt_cf_options);
        Preference preferenceFindPreference = getPreferenceScreen().findPreference("voice_key");
        Intent intent = subscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
        GsmUmtsVTUtils.setServiceClass(intent, 1);
        preferenceFindPreference.setIntent(intent);
        Preference preferenceFindPreference2 = getPreferenceScreen().findPreference("video_key");
        Intent intent2 = subscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
        GsmUmtsVTUtils.setServiceClass(intent2, 512);
        preferenceFindPreference2.setIntent(intent2);
    }
}
