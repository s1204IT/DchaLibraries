package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.android.settings.notification.RedactionInterstitial;

public class SetupRedactionInterstitial extends RedactionInterstitial {

    public static class SetupRedactionInterstitialFragment extends RedactionInterstitial.RedactionInterstitialFragment {
    }

    public static void setEnabled(Context context, boolean z) {
        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, (Class<?>) SetupRedactionInterstitial.class), z ? 1 : 2, 1);
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", SetupRedactionInterstitialFragment.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return SetupRedactionInterstitialFragment.class.getName().equals(str);
    }
}
