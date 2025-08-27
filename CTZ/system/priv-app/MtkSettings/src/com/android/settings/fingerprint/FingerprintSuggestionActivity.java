package com.android.settings.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class FingerprintSuggestionActivity extends SetupFingerprintEnrollIntroduction {
    @Override // com.android.settings.fingerprint.SetupFingerprintEnrollIntroduction, com.android.settings.fingerprint.FingerprintEnrollIntroduction, com.android.settings.fingerprint.FingerprintEnrollBase
    protected void initViews() {
        super.initViews();
        ((Button) findViewById(R.id.fingerprint_cancel_button)).setText(R.string.security_settings_fingerprint_enroll_introduction_cancel);
    }

    @Override // android.app.Activity
    public void finish() {
        setResult(0);
        super.finish();
    }

    public static boolean isSuggestionComplete(Context context) {
        return (Utils.hasFingerprintHardware(context) && isFingerprintEnabled(context) && !isNotSingleFingerprintEnrolled(context)) ? false : true;
    }

    private static boolean isNotSingleFingerprintEnrolled(Context context) {
        FingerprintManager fingerprintManagerOrNull = Utils.getFingerprintManagerOrNull(context);
        return fingerprintManagerOrNull == null || fingerprintManagerOrNull.getEnrolledFingerprints().size() != 1;
    }

    static boolean isFingerprintEnabled(Context context) {
        return (((DevicePolicyManager) context.getSystemService("device_policy")).getKeyguardDisabledFeatures(null, context.getUserId()) & 32) == 0;
    }
}
