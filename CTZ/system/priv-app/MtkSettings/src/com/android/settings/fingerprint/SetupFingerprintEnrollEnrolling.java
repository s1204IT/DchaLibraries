package com.android.settings.fingerprint;

import android.content.Intent;
import com.android.settings.SetupWizardUtils;

/* loaded from: classes.dex */
public class SetupFingerprintEnrollEnrolling extends FingerprintEnrollEnrolling {
    @Override // com.android.settings.fingerprint.FingerprintEnrollEnrolling
    protected Intent getFinishIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollFinish.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollEnrolling, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 246;
    }
}
