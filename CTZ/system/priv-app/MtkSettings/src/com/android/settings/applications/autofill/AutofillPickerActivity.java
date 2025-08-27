package com.android.settings.applications.autofill;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.defaultapps.DefaultAutofillPicker;

/* loaded from: classes.dex */
public class AutofillPickerActivity extends SettingsActivity {
    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        Intent intent = getIntent();
        String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
        intent.putExtra(":settings:show_fragment", DefaultAutofillPicker.class.getName());
        intent.putExtra(":settings:show_fragment_title_resid", R.string.autofill_app);
        intent.putExtra("package_name", schemeSpecificPart);
        super.onCreate(bundle);
    }

    @Override // com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return super.isValidFragment(str) || DefaultAutofillPicker.class.getName().equals(str);
    }
}
