package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.android.settings.notification.RedactionInterstitial;

/* loaded from: classes.dex */
public class SetupRedactionInterstitial extends RedactionInterstitial {

    public static class SetupRedactionInterstitialFragment extends RedactionInterstitial.RedactionInterstitialFragment {
    }

    public static void setEnabled(Context context, boolean z) {
        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, (Class<?>) SetupRedactionInterstitial.class), z ? 1 : 2, 1);
    }

    @Override // com.android.settings.notification.RedactionInterstitial, com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", SetupRedactionInterstitialFragment.class.getName());
        return intent;
    }

    @Override // com.android.settings.notification.RedactionInterstitial, com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return SetupRedactionInterstitialFragment.class.getName().equals(str);
    }
}
