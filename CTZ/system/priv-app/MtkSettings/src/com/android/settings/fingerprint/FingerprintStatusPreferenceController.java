package com.android.settings.fingerprint;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import java.util.List;

/* loaded from: classes.dex */
public class FingerprintStatusPreferenceController extends BasePreferenceController {
    private static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";
    protected final FingerprintManager mFingerprintManager;
    protected final LockPatternUtils mLockPatternUtils;
    protected final int mProfileChallengeUserId;
    protected final UserManager mUm;
    protected final int mUserId;

    public FingerprintStatusPreferenceController(Context context) {
        this(context, KEY_FINGERPRINT_SETTINGS);
    }

    public FingerprintStatusPreferenceController(Context context, String str) {
        super(context, str);
        this.mUserId = UserHandle.myUserId();
        this.mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
        this.mUm = (UserManager) context.getSystemService("user");
        this.mLockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
        this.mProfileChallengeUserId = Utils.getManagedProfileId(this.mUm, this.mUserId);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mFingerprintManager == null || !this.mFingerprintManager.isHardwareDetected()) {
            return 2;
        }
        if (isUserSupported()) {
            return 0;
        }
        return 3;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        final String name;
        if (!isAvailable()) {
            if (preference != null) {
                preference.setVisible(false);
                return;
            }
            return;
        }
        preference.setVisible(true);
        final int userId = getUserId();
        List enrolledFingerprints = this.mFingerprintManager.getEnrolledFingerprints(userId);
        int size = enrolledFingerprints != null ? enrolledFingerprints.size() : 0;
        if (size > 0) {
            preference.setSummary(this.mContext.getResources().getQuantityString(R.plurals.security_settings_fingerprint_preference_summary, size, Integer.valueOf(size)));
            name = FingerprintSettings.class.getName();
        } else {
            preference.setSummary(R.string.security_settings_fingerprint_preference_summary_none);
            name = FingerprintEnrollIntroduction.class.getName();
        }
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.fingerprint.-$$Lambda$FingerprintStatusPreferenceController$KyR_IBe4qHxX_KvME9NBxSJFxFQ
            @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
            public final boolean onPreferenceClick(Preference preference2) {
                return FingerprintStatusPreferenceController.lambda$updateState$0(userId, name, preference2);
            }
        });
    }

    static /* synthetic */ boolean lambda$updateState$0(int i, String str, Preference preference) {
        Context context = preference.getContext();
        if (Utils.startQuietModeDialogIfNecessary(context, UserManager.get(context), i)) {
            return false;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", str);
        intent.putExtra("android.intent.extra.USER_ID", i);
        context.startActivity(intent);
        return true;
    }

    protected int getUserId() {
        return this.mUserId;
    }

    protected boolean isUserSupported() {
        return true;
    }
}
