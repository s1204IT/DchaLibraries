package com.android.settings.applications.autofill;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.autofill.AutofillManager;
import com.android.settings.applications.defaultapps.DefaultAutofillPicker;

public class AutofillPickerTrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
        String defaultKey = DefaultAutofillPicker.getDefaultKey(this);
        if (defaultKey != null && defaultKey.startsWith(schemeSpecificPart)) {
            setResult(-1);
            finish();
            return;
        }
        AutofillManager autofillManager = (AutofillManager) getSystemService(AutofillManager.class);
        if (autofillManager == null || !autofillManager.hasAutofillFeature() || !autofillManager.isAutofillSupported()) {
            setResult(0);
            finish();
        } else {
            startActivity(new Intent(this, (Class<?>) AutofillPickerActivity.class).setFlags(33554432).setData(intent.getData()));
            finish();
        }
    }
}
