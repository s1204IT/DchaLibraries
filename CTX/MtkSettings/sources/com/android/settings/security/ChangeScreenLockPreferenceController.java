package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

public class ChangeScreenLockPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, GearPreference.OnGearClickListener {
    protected final DevicePolicyManager mDPM;
    protected final SecuritySettings mHost;
    protected final LockPatternUtils mLockPatternUtils;
    protected RestrictedPreference mPreference;
    protected final int mProfileChallengeUserId;
    protected final UserManager mUm;
    protected final int mUserId;

    public ChangeScreenLockPreferenceController(Context context, SecuritySettings securitySettings) {
        super(context);
        this.mUserId = UserHandle.myUserId();
        this.mUm = (UserManager) context.getSystemService("user");
        this.mDPM = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mLockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
        this.mHost = securitySettings;
        this.mProfileChallengeUserId = Utils.getManagedProfileId(this.mUm, this.mUserId);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_unlock_set_or_change);
    }

    @Override
    public String getPreferenceKey() {
        return "unlock_set_or_change";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (RestrictedPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mPreference != null && (this.mPreference instanceof GearPreference)) {
            if (this.mLockPatternUtils.isSecure(this.mUserId) || !this.mLockPatternUtils.isLockScreenDisabled(this.mUserId)) {
                ((GearPreference) this.mPreference).setOnGearClickListener(this);
            } else {
                ((GearPreference) this.mPreference).setOnGearClickListener(null);
            }
        }
        updateSummary(preference, this.mUserId);
        disableIfPasswordQualityManaged(this.mUserId);
        if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId)) {
            disableIfPasswordQualityManaged(this.mProfileChallengeUserId);
        }
    }

    @Override
    public void onGearClick(GearPreference gearPreference) {
        if (TextUtils.equals(gearPreference.getKey(), getPreferenceKey())) {
            new SubSettingLauncher(this.mContext).setDestination(ScreenLockSettings.class.getName()).setSourceMetricsCategory(this.mHost.getMetricsCategory()).launch();
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        if (this.mProfileChallengeUserId != -10000 && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId) && StorageManager.isFileEncryptedNativeOnly() && Utils.startQuietModeDialogIfNecessary(this.mContext, this.mUm, this.mProfileChallengeUserId)) {
            return false;
        }
        new SubSettingLauncher(this.mContext).setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName()).setTitle(R.string.lock_settings_picker_title).setSourceMetricsCategory(this.mHost.getMetricsCategory()).launch();
        return true;
    }

    protected void updateSummary(Preference preference, int i) {
        if (!this.mLockPatternUtils.isSecure(i)) {
            if (i == this.mProfileChallengeUserId || this.mLockPatternUtils.isLockScreenDisabled(i)) {
                preference.setSummary(R.string.unlock_set_unlock_mode_off);
            } else {
                preference.setSummary(R.string.unlock_set_unlock_mode_none);
            }
        } else {
            int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(i);
            if (keyguardStoredPasswordQuality == 65536) {
                preference.setSummary(R.string.unlock_set_unlock_mode_pattern);
            } else if (keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608) {
                preference.setSummary(R.string.unlock_set_unlock_mode_pin);
            } else if (keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288) {
                preference.setSummary(R.string.unlock_set_unlock_mode_password);
            }
        }
        this.mPreference.setEnabled(true);
    }

    void disableIfPasswordQualityManaged(int i) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfPasswordQualityIsSet = RestrictedLockUtils.checkIfPasswordQualityIsSet(this.mContext, i);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (enforcedAdminCheckIfPasswordQualityIsSet != null && devicePolicyManager.getPasswordQuality(enforcedAdminCheckIfPasswordQualityIsSet.component, i) == 524288) {
            this.mPreference.setDisabledByAdmin(enforcedAdminCheckIfPasswordQualityIsSet);
        }
    }
}
