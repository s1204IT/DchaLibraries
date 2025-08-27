package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class FingerprintEnrollFinish extends FingerprintEnrollBase {
    @Override // com.android.settings.fingerprint.FingerprintEnrollBase, com.android.settings.core.InstrumentedActivity, com.android.settingslib.core.lifecycle.ObservableActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.fingerprint_enroll_finish);
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        Button button = (Button) findViewById(R.id.add_another_button);
        FingerprintManager fingerprintManagerOrNull = Utils.getFingerprintManagerOrNull(this);
        boolean z = false;
        if (fingerprintManagerOrNull != null && fingerprintManagerOrNull.getEnrolledFingerprints(this.mUserId).size() >= getResources().getInteger(android.R.integer.config_datause_polling_period_sec)) {
            z = true;
        }
        if (z) {
            button.setVisibility(4);
        } else {
            button.setOnClickListener(this);
        }
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase
    protected void onNextButtonClick() {
        setResult(1);
        finish();
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase, android.view.View.OnClickListener
    public void onClick(View view) {
        if (view.getId() == R.id.add_another_button) {
            startActivityForResult(getEnrollingIntent(), 1);
        }
        super.onClick(view);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1 && i2 != 0) {
            setResult(i2, intent);
            finish();
        } else {
            super.onActivityResult(i, i2, intent);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 242;
    }
}
