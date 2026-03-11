package com.android.settings.inputmethod;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import com.android.settings.SettingsActivity;

public class InputMethodAndSubtypeEnablerActivity extends SettingsActivity {
    private static final String FRAGMENT_NAME = InputMethodAndSubtypeEnabler.class.getName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        if (!intent.hasExtra(":settings:show_fragment")) {
            intent.putExtra(":settings:show_fragment", FRAGMENT_NAME);
        }
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return FRAGMENT_NAME.equals(str);
    }
}
