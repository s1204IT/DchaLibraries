package com.android.settings.security;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class LockUnificationPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final int MY_USER_ID = UserHandle.myUserId();
    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;
    private final SecuritySettings mHost;
    private final LockPatternUtils mLockPatternUtils;
    private final int mProfileChallengeUserId;
    private final UserManager mUm;
    private RestrictedSwitchPreference mUnifyProfile;

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mUnifyProfile = (RestrictedSwitchPreference) preferenceScreen.findPreference("unification");
    }

    public LockUnificationPreferenceController(Context context, SecuritySettings securitySettings) {
        super(context);
        this.mHost = securitySettings;
        this.mUm = (UserManager) context.getSystemService("user");
        this.mLockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
        this.mProfileChallengeUserId = Utils.getManagedProfileId(this.mUm, MY_USER_ID);
    }

    @Override
    public boolean isAvailable() {
        return this.mProfileChallengeUserId != -10000 && this.mLockPatternUtils.isSeparateProfileChallengeAllowed(this.mProfileChallengeUserId);
    }

    @Override
    public String getPreferenceKey() {
        return "unification";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean z = false;
        if (Utils.startQuietModeDialogIfNecessary(this.mContext, this.mUm, this.mProfileChallengeUserId)) {
            return false;
        }
        if (((Boolean) obj).booleanValue()) {
            if (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mProfileChallengeUserId) >= 65536 && this.mLockPatternUtils.isSeparateProfileChallengeAllowedToUnify(this.mProfileChallengeUserId)) {
                z = true;
            }
            UnificationConfirmationDialog.newInstance(z).show(this.mHost);
        } else {
            if (!new ChooseLockSettingsHelper(this.mHost.getActivity(), this.mHost).launchConfirmationActivity(130, this.mContext.getString(R.string.unlock_set_unlock_launch_picker_title), true, MY_USER_ID)) {
                ununifyLocks();
            }
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mUnifyProfile != null) {
            boolean zIsSeparateProfileChallengeEnabled = this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId);
            this.mUnifyProfile.setChecked(!zIsSeparateProfileChallengeEnabled);
            if (zIsSeparateProfileChallengeEnabled) {
                this.mUnifyProfile.setDisabledByAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_unified_password", this.mProfileChallengeUserId));
            }
        }
    }

    public boolean handleActivityResult(int i, int i2, Intent intent) {
        if (i == 130 && i2 == -1) {
            ununifyLocks();
            return true;
        }
        if (i == 128 && i2 == -1) {
            this.mCurrentDevicePassword = intent.getStringExtra("password");
            launchConfirmProfileLockForUnification();
            return true;
        }
        if (i == 129 && i2 == -1) {
            this.mCurrentProfilePassword = intent.getStringExtra("password");
            unifyLocks();
            return true;
        }
        return false;
    }

    private void ununifyLocks() {
        Bundle bundle = new Bundle();
        bundle.putInt("android.intent.extra.USER_ID", this.mProfileChallengeUserId);
        new SubSettingLauncher(this.mContext).setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName()).setTitle(R.string.lock_settings_picker_title_profile).setSourceMetricsCategory(this.mHost.getMetricsCategory()).setArguments(bundle).launch();
    }

    void launchConfirmDeviceLockForUnification() {
        if (!new ChooseLockSettingsHelper(this.mHost.getActivity(), this.mHost).launchConfirmationActivity(128, this.mContext.getString(R.string.unlock_set_unlock_launch_picker_title), true, MY_USER_ID)) {
            launchConfirmProfileLockForUnification();
        }
    }

    private void launchConfirmProfileLockForUnification() {
        if (!new ChooseLockSettingsHelper(this.mHost.getActivity(), this.mHost).launchConfirmationActivity(129, this.mContext.getString(R.string.unlock_set_unlock_launch_picker_title_profile), true, this.mProfileChallengeUserId)) {
            unifyLocks();
        }
    }

    private void unifyLocks() {
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mProfileChallengeUserId);
        if (keyguardStoredPasswordQuality == 65536) {
            this.mLockPatternUtils.saveLockPattern(LockPatternUtils.stringToPattern(this.mCurrentProfilePassword), this.mCurrentDevicePassword, MY_USER_ID);
        } else {
            this.mLockPatternUtils.saveLockPassword(this.mCurrentProfilePassword, this.mCurrentDevicePassword, keyguardStoredPasswordQuality, MY_USER_ID);
        }
        this.mLockPatternUtils.setSeparateProfileChallengeEnabled(this.mProfileChallengeUserId, false, this.mCurrentProfilePassword);
        this.mLockPatternUtils.setVisiblePatternEnabled(this.mLockPatternUtils.isVisiblePatternEnabled(this.mProfileChallengeUserId), MY_USER_ID);
        this.mCurrentDevicePassword = null;
        this.mCurrentProfilePassword = null;
    }

    void unifyUncompliantLocks() {
        this.mLockPatternUtils.setSeparateProfileChallengeEnabled(this.mProfileChallengeUserId, false, this.mCurrentProfilePassword);
        new SubSettingLauncher(this.mContext).setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName()).setTitle(R.string.lock_settings_picker_title).setSourceMetricsCategory(this.mHost.getMetricsCategory()).launch();
    }
}
