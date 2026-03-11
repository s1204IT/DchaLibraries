package com.android.settings.fingerprint;

import android.content.Context;

public class FingerprintProfileStatusPreferenceController extends FingerprintStatusPreferenceController {
    public static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings_profile";

    public FingerprintProfileStatusPreferenceController(Context context) {
        super(context, KEY_FINGERPRINT_SETTINGS);
    }

    @Override
    protected boolean isUserSupported() {
        return this.mProfileChallengeUserId != -10000 && this.mLockPatternUtils.isSeparateProfileChallengeAllowed(this.mProfileChallengeUserId);
    }

    @Override
    protected int getUserId() {
        return this.mProfileChallengeUserId;
    }
}
