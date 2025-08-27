package com.android.settings.fingerprint;

import android.content.Context;

/* loaded from: classes.dex */
public class FingerprintProfileStatusPreferenceController extends FingerprintStatusPreferenceController {
    public static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings_profile";

    public FingerprintProfileStatusPreferenceController(Context context) {
        super(context, KEY_FINGERPRINT_SETTINGS);
    }

    @Override // com.android.settings.fingerprint.FingerprintStatusPreferenceController
    protected boolean isUserSupported() {
        return this.mProfileChallengeUserId != -10000 && this.mLockPatternUtils.isSeparateProfileChallengeAllowed(this.mProfileChallengeUserId);
    }

    @Override // com.android.settings.fingerprint.FingerprintStatusPreferenceController
    protected int getUserId() {
        return this.mProfileChallengeUserId;
    }
}
