package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.android.settings.EncryptionInterstitial;

public class SetupEncryptionInterstitial extends EncryptionInterstitial {

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitial.EncryptionInterstitialFragment {
    }

    public static Intent createStartIntent(Context context, int i, boolean z, Intent intent) {
        Intent intentCreateStartIntent = EncryptionInterstitial.createStartIntent(context, i, z, intent);
        intentCreateStartIntent.setClass(context, SetupEncryptionInterstitial.class);
        intentCreateStartIntent.putExtra("extra_prefs_show_button_bar", false).putExtra(":settings:show_fragment_title_resid", -1);
        return intentCreateStartIntent;
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", SetupEncryptionInterstitialFragment.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(str);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }
}
