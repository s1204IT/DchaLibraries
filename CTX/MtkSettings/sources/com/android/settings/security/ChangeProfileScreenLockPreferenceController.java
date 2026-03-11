package com.android.settings.security;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;

public class ChangeProfileScreenLockPreferenceController extends ChangeScreenLockPreferenceController {
    public ChangeProfileScreenLockPreferenceController(Context context, SecuritySettings securitySettings) {
        super(context, securitySettings);
    }

    @Override
    public boolean isAvailable() {
        int keyguardStoredPasswordQuality;
        if (this.mProfileChallengeUserId == -10000 || !this.mLockPatternUtils.isSeparateProfileChallengeAllowed(this.mProfileChallengeUserId)) {
            return false;
        }
        return !this.mLockPatternUtils.isSecure(this.mProfileChallengeUserId) || (keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mProfileChallengeUserId)) == 65536 || keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608 || keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288;
    }

    @Override
    public String getPreferenceKey() {
        return "unlock_set_or_change_profile";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey()) || Utils.startQuietModeDialogIfNecessary(this.mContext, this.mUm, this.mProfileChallengeUserId)) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("android.intent.extra.USER_ID", this.mProfileChallengeUserId);
        new SubSettingLauncher(this.mContext).setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName()).setTitle(R.string.lock_settings_picker_title_profile).setSourceMetricsCategory(this.mHost.getMetricsCategory()).setArguments(bundle).launch();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, this.mProfileChallengeUserId);
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId)) {
            this.mPreference.setSummary(this.mContext.getString(R.string.lock_settings_profile_unified_summary));
            this.mPreference.setEnabled(false);
            return;
        }
        disableIfPasswordQualityManaged(this.mProfileChallengeUserId);
    }
}
