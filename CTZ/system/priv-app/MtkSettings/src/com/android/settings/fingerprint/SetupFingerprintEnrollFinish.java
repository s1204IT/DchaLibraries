package com.android.settings.fingerprint;

import android.content.Intent;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;

/* loaded from: classes.dex */
public class SetupFingerprintEnrollFinish extends FingerprintEnrollFinish {
    @Override // com.android.settings.fingerprint.FingerprintEnrollBase
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, (Class<?>) SetupFingerprintEnrollEnrolling.class);
        intent.putExtra("hw_auth_token", this.mToken);
        if (this.mUserId != -10000) {
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollBase
    protected void initViews() {
        super.initViews();
        ((Button) findViewById(R.id.next_button)).setText(R.string.next_label);
    }

    @Override // com.android.settings.fingerprint.FingerprintEnrollFinish, com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 248;
    }
}
