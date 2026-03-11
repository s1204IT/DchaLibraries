package com.android.settings.fingerprint;

import android.content.Intent;
import com.android.settings.SetupWizardUtils;

public class SetupFingerprintEnrollEnrolling extends FingerprintEnrollEnrolling {
    @Override
    protected Intent getFinishIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollFinish.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    public int getMetricsCategory() {
        return 246;
    }
}
