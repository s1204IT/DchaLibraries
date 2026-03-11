package com.android.settings.accessibility;

import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.view.Menu;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.instrumentation.Instrumentable;

public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putCharSequence("activity_title", getTitle());
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        setTitle(bundle.getCharSequence("activity_title"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        getWindow().getDecorView().sendAccessibilityEvent(32);
        return true;
    }

    @Override
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
