package com.android.settings.development;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.oemlock.OemLockManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class OemUnlockPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, OnActivityResultListener {
    private final ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private final DevelopmentSettingsDashboardFragment mFragment;
    private final OemLockManager mOemLockManager;
    private RestrictedSwitchPreference mPreference;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;

    public OemUnlockPreferenceController(Context context, Activity activity, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        super(context);
        this.mOemLockManager = (OemLockManager) context.getSystemService("oem_lock");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mFragment = developmentSettingsDashboardFragment;
        if (activity != null || this.mFragment != null) {
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(activity, this.mFragment);
        } else {
            this.mChooseLockSettingsHelper = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return this.mOemLockManager != null;
    }

    @Override
    public String getPreferenceKey() {
        return "oem_unlock_enable";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (RestrictedSwitchPreference) preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (((Boolean) obj).booleanValue()) {
            if (!showKeyguardConfirmation(this.mContext.getResources(), 0)) {
                confirmEnableOemUnlock();
                return true;
            }
            return true;
        }
        this.mOemLockManager.setOemUnlockAllowedByUser(false);
        OemLockInfoDialog.show(this.mFragment);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mPreference.setChecked(isOemUnlockedAllowed());
        updateOemUnlockSettingDescription();
        this.mPreference.setDisabledByAdmin(null);
        this.mPreference.setEnabled(enableOemUnlockPreference());
        if (this.mPreference.isEnabled()) {
            this.mPreference.checkRestrictionAndSetDisabled("no_factory_reset");
        }
    }

    @Override
    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i != 0) {
            return false;
        }
        if (i2 == -1) {
            if (!this.mPreference.isChecked()) {
                this.mOemLockManager.setOemUnlockAllowedByUser(false);
                return true;
            }
            confirmEnableOemUnlock();
            return true;
        }
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        handleDeveloperOptionsToggled();
    }

    public void onOemUnlockConfirmed() {
        this.mOemLockManager.setOemUnlockAllowedByUser(true);
    }

    public void onOemUnlockDismissed() {
        if (this.mPreference == null) {
            return;
        }
        updateState(this.mPreference);
    }

    private void handleDeveloperOptionsToggled() {
        this.mPreference.setEnabled(enableOemUnlockPreference());
        if (this.mPreference.isEnabled()) {
            this.mPreference.checkRestrictionAndSetDisabled("no_factory_reset");
        }
    }

    private void updateOemUnlockSettingDescription() {
        int i;
        if (isBootloaderUnlocked()) {
            i = R.string.oem_unlock_enable_disabled_summary_bootloader_unlocked;
        } else if (isSimLockedDevice()) {
            i = R.string.oem_unlock_enable_disabled_summary_sim_locked_device;
        } else if (!isOemUnlockAllowedByUserAndCarrier()) {
            i = R.string.oem_unlock_enable_disabled_summary_connectivity_or_locked;
        } else {
            i = R.string.oem_unlock_enable_summary;
        }
        this.mPreference.setSummary(this.mContext.getResources().getString(i));
    }

    private boolean isSimLockedDevice() {
        int phoneCount = this.mTelephonyManager.getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            if (this.mTelephonyManager.getAllowedCarriers(i).size() > 0) {
                return true;
            }
        }
        return false;
    }

    boolean isBootloaderUnlocked() {
        return this.mOemLockManager.isDeviceOemUnlocked();
    }

    private boolean enableOemUnlockPreference() {
        return !isBootloaderUnlocked() && isOemUnlockAllowedByUserAndCarrier();
    }

    boolean showKeyguardConfirmation(Resources resources, int i) {
        return this.mChooseLockSettingsHelper.launchConfirmationActivity(i, resources.getString(R.string.oem_unlock_enable));
    }

    void confirmEnableOemUnlock() {
        EnableOemUnlockSettingWarningDialog.show(this.mFragment);
    }

    boolean isOemUnlockAllowedByUserAndCarrier() {
        return this.mOemLockManager.isOemUnlockAllowedByCarrier() && !this.mUserManager.hasBaseUserRestriction("no_factory_reset", UserHandle.of(UserHandle.myUserId()));
    }

    boolean isOemUnlockedAllowed() {
        return this.mOemLockManager.isOemUnlockAllowed();
    }
}
