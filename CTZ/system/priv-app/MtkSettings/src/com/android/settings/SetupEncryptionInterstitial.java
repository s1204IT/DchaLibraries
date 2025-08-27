package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.android.settings.EncryptionInterstitial;

/* loaded from: classes.dex */
public class SetupEncryptionInterstitial extends EncryptionInterstitial {

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitial.EncryptionInterstitialFragment {
    }

    public static Intent createStartIntent(Context context, int i, boolean z, Intent intent) {
        Intent intentCreateStartIntent = EncryptionInterstitial.createStartIntent(context, i, z, intent);
        intentCreateStartIntent.setClass(context, SetupEncryptionInterstitial.class);
        intentCreateStartIntent.putExtra("extra_prefs_show_button_bar", false).putExtra(":settings:show_fragment_title_resid", -1);
        return intentCreateStartIntent;
    }

    @Override // com.android.settings.EncryptionInterstitial, com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", SetupEncryptionInterstitialFragment.class.getName());
        return intent;
    }

    @Override // com.android.settings.EncryptionInterstitial, com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(str);
    }

    @Override // com.android.settings.EncryptionInterstitial, com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }
}
