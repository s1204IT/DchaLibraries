package com.android.settings.accessibility;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.view.Menu;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.instrumentation.Instrumentable;

/* loaded from: classes.dex */
public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {
    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        super.onCreate(bundle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override // com.android.settings.SettingsActivity, android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putCharSequence("activity_title", getTitle());
        super.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        setTitle(bundle.getCharSequence("activity_title"));
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override // com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    public boolean onNavigateUp() {
        onBackPressed();
        getWindow().getDecorView().sendAccessibilityEvent(32);
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: android.support.v14.preference.PreferenceFragment */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.settings.SettingsActivity, android.support.v14.preference.PreferenceFragment.OnPreferenceStartFragmentCallback
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        Bundle extras = preference.getExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        extras.putInt("help_uri_resource", 0);
        extras.putBoolean("need_search_icon_in_action_bar", false);
        new SubSettingLauncher(this).setDestination(preference.getFragment()).setArguments(extras).setSourceMetricsCategory(preferenceFragment instanceof Instrumentable ? ((Instrumentable) preferenceFragment).getMetricsCategory() : 0).launch();
        return true;
    }
}
