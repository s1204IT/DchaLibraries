package com.android.phone.settings;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.phone.R;

public class AccessibilitySettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.accessibility_settings_activity_title);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AccessibilitySettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
