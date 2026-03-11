package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.List;

public class ChooseLockGeneric extends SettingsActivity {

    public static class InternalActivity extends ChooseLockGeneric {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        String action = modIntent.getAction();
        if ("android.app.action.SET_NEW_PASSWORD".equals(action) || "android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD".equals(action)) {
            modIntent.putExtra(":settings:hide_drawer", true);
        }
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockGenericFragment.class;
    }

    public static class ChooseLockGenericFragment extends SettingsPreferenceFragment {
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private boolean mEncryptionRequestDisabled;
        private int mEncryptionRequestQuality;
        private FingerprintManager mFingerprintManager;
        private KeyStore mKeyStore;
        private LockPatternUtils mLockPatternUtils;
        private ManagedLockPasswordProvider mManagedPasswordProvider;
        private boolean mRequirePassword;
        private int mUserId;
        private String mUserPassword;
        private boolean mHasChallenge = false;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private boolean mForChangeCredRequiredForBoot = false;
        private boolean mHideDrawer = false;
        protected boolean mForFingerprint = false;

        @Override
        protected int getMetricsCategory() {
            return 27;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Activity activity = getActivity();
            if (!Utils.isDeviceProvisioned(activity) && !canRunBeforeDeviceProvisioned()) {
                activity.finish();
                return;
            }
            this.mFingerprintManager = (FingerprintManager) getActivity().getSystemService("fingerprint");
            this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
            this.mKeyStore = KeyStore.getInstance();
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            boolean confirmCredentials = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            if (getActivity() instanceof InternalActivity) {
                this.mPasswordConfirmed = !confirmCredentials;
            }
            this.mHideDrawer = getActivity().getIntent().getBooleanExtra(":settings:hide_drawer", false);
            this.mHasChallenge = getActivity().getIntent().getBooleanExtra("has_challenge", false);
            this.mChallenge = getActivity().getIntent().getLongExtra("challenge", 0L);
            this.mForFingerprint = getActivity().getIntent().getBooleanExtra("for_fingerprint", false);
            this.mForChangeCredRequiredForBoot = getArguments() != null ? getArguments().getBoolean("for_cred_req_boot") : false;
            if (savedInstanceState != null) {
                this.mPasswordConfirmed = savedInstanceState.getBoolean("password_confirmed");
                this.mWaitingForConfirmation = savedInstanceState.getBoolean("waiting_for_confirmation");
                this.mEncryptionRequestQuality = savedInstanceState.getInt("encrypt_requested_quality");
                this.mEncryptionRequestDisabled = savedInstanceState.getBoolean("encrypt_requested_disabled");
            }
            int targetUser = Utils.getSecureTargetUser(getActivity().getActivityToken(), UserManager.get(getActivity()), null, getActivity().getIntent().getExtras()).getIdentifier();
            if ("android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD".equals(getActivity().getIntent().getAction()) || !this.mLockPatternUtils.isSeparateProfileChallengeAllowed(targetUser)) {
                Bundle arguments = getArguments();
                Context context = getContext();
                if (arguments == null) {
                    arguments = getActivity().getIntent().getExtras();
                }
                this.mUserId = Utils.getUserIdFromBundle(context, arguments);
            } else {
                this.mUserId = targetUser;
            }
            if ("android.app.action.SET_NEW_PASSWORD".equals(getActivity().getIntent().getAction()) && Utils.isManagedProfile(UserManager.get(getActivity()), this.mUserId) && this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mUserId)) {
                getActivity().setTitle(R.string.lock_settings_picker_title_profile);
            }
            this.mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), this.mUserId);
            if (this.mPasswordConfirmed) {
                updatePreferencesOrFinish();
                if (this.mForChangeCredRequiredForBoot) {
                    maybeEnableEncryption(this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId), false);
                }
            } else if (!this.mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
                boolean managedProfileWithUnifiedLock = Utils.isManagedProfile(UserManager.get(getActivity()), this.mUserId) && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mUserId);
                if (managedProfileWithUnifiedLock || !helper.launchConfirmationActivity(100, getString(R.string.unlock_set_unlock_launch_picker_title), true, this.mUserId)) {
                    this.mPasswordConfirmed = true;
                    updatePreferencesOrFinish();
                } else {
                    this.mWaitingForConfirmation = true;
                }
            }
            addHeaderView();
        }

        protected boolean canRunBeforeDeviceProvisioned() {
            return false;
        }

        protected void addHeaderView() {
            if (!this.mForFingerprint) {
                return;
            }
            setHeaderView(R.layout.choose_lock_generic_fingerprint_header);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if (!isUnlockMethodSecure(key) && this.mLockPatternUtils.isSecure(this.mUserId)) {
                showFactoryResetProtectionWarningDialog(key);
                return true;
            }
            return setUnlockMethod(key);
        }

        private void maybeEnableEncryption(int quality, boolean disabled) {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
            if (UserManager.get(getActivity()).isAdminUser() && this.mUserId == UserHandle.myUserId() && LockPatternUtils.isDeviceEncryptionEnabled() && !LockPatternUtils.isFileEncryptionEnabled() && !dpm.getDoNotAskCredentialsOnBoot()) {
                this.mEncryptionRequestQuality = quality;
                this.mEncryptionRequestDisabled = disabled;
                Intent unlockMethodIntent = getIntentForUnlockMethod(quality, disabled);
                unlockMethodIntent.putExtra("for_cred_req_boot", this.mForChangeCredRequiredForBoot);
                Context context = getActivity();
                boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                boolean required = this.mLockPatternUtils.isCredentialRequiredToDecrypt(accEn ? false : true);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required, unlockMethodIntent);
                intent.putExtra("for_fingerprint", this.mForFingerprint);
                intent.putExtra(":settings:hide_drawer", this.mHideDrawer);
                startActivityForResult(intent, 101);
                return;
            }
            if (this.mForChangeCredRequiredForBoot) {
                finish();
            } else {
                this.mRequirePassword = false;
                updateUnlockMethodAndFinish(quality, disabled);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            this.mWaitingForConfirmation = false;
            if (requestCode == 100 && resultCode == -1) {
                this.mPasswordConfirmed = true;
                this.mUserPassword = data.getStringExtra("password");
                updatePreferencesOrFinish();
                if (this.mForChangeCredRequiredForBoot) {
                    if (!TextUtils.isEmpty(this.mUserPassword)) {
                        maybeEnableEncryption(this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId), false);
                    } else {
                        finish();
                    }
                }
            } else if (requestCode == 102 || requestCode == 101) {
                if (resultCode != 0 || this.mForChangeCredRequiredForBoot) {
                    getActivity().setResult(resultCode, data);
                    finish();
                }
            } else {
                getActivity().setResult(0);
                finish();
            }
            if (requestCode != 0 || !this.mForChangeCredRequiredForBoot) {
                return;
            }
            finish();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("password_confirmed", this.mPasswordConfirmed);
            outState.putBoolean("waiting_for_confirmation", this.mWaitingForConfirmation);
            outState.putInt("encrypt_requested_quality", this.mEncryptionRequestQuality);
            outState.putBoolean("encrypt_requested_disabled", this.mEncryptionRequestDisabled);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra("lockscreen.password_type", -1);
            if (quality == -1) {
                int quality2 = upgradeQuality(intent.getIntExtra("minimum_quality", -1));
                boolean hideDisabledPrefs = intent.getBooleanExtra("hide_disabled_prefs", false);
                PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferences();
                disableUnusablePreferences(quality2, hideDisabledPrefs);
                updatePreferenceText();
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
                return;
            }
            updateUnlockMethodAndFinish(quality, false);
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker);
            findPreference("unlock_set_none").setViewId(R.id.lock_none);
            findPreference("unlock_set_pin").setViewId(R.id.lock_pin);
            findPreference("unlock_set_password").setViewId(R.id.lock_password);
        }

        private void updatePreferenceText() {
            if (this.mForFingerprint) {
                Preference pattern = findPreference("unlock_set_pattern");
                pattern.setTitle(R.string.fingerprint_unlock_set_unlock_pattern);
                Preference pin = findPreference("unlock_set_pin");
                pin.setTitle(R.string.fingerprint_unlock_set_unlock_pin);
                Preference password = findPreference("unlock_set_password");
                password.setTitle(R.string.fingerprint_unlock_set_unlock_password);
            }
            if (this.mManagedPasswordProvider.isSettingManagedPasswordSupported()) {
                Preference managed = findPreference("unlock_set_managed");
                managed.setTitle(this.mManagedPasswordProvider.getPickerOptionTitle(this.mForFingerprint));
            } else {
                removePreference("unlock_set_managed");
            }
        }

        private void updateCurrentPreference() {
            String currentKey = getKeyForCurrent();
            Preference preference = findPreference(currentKey);
            if (preference == null) {
                return;
            }
            preference.setSummary(R.string.current_screen_lock);
        }

        private String getKeyForCurrent() {
            int credentialOwner = UserManager.get(getContext()).getCredentialOwnerProfile(this.mUserId);
            if (this.mLockPatternUtils.isLockScreenDisabled(credentialOwner)) {
                return "unlock_set_off";
            }
            switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(credentialOwner)) {
                case DefaultWfcSettingsExt.RESUME:
                    return "unlock_set_none";
                case 65536:
                    return "unlock_set_pattern";
                case 131072:
                case 196608:
                    return "unlock_set_pin";
                case 262144:
                case 327680:
                case 393216:
                    return "unlock_set_password";
                case 524288:
                    return "unlock_set_managed";
                default:
                    return null;
            }
        }

        private int upgradeQuality(int quality) {
            return upgradeQualityForDPM(quality);
        }

        private int upgradeQualityForDPM(int quality) {
            int minQuality = this.mDPM.getPasswordQuality(null, this.mUserId);
            if (quality < minQuality) {
                return minQuality;
            }
            return quality;
        }

        protected void disableUnusablePreferences(int quality, boolean hideDisabledPrefs) {
            disableUnusablePreferencesImpl(quality, hideDisabledPrefs);
        }

        protected void disableUnusablePreferencesImpl(int quality, boolean hideDisabled) {
            PreferenceScreen entries = getPreferenceScreen();
            int adminEnforcedQuality = this.mDPM.getPasswordQuality(null, this.mUserId);
            RestrictedLockUtils.EnforcedAdmin enforcedAdmin = RestrictedLockUtils.checkIfPasswordQualityIsSet(getActivity(), this.mUserId);
            for (int i = entries.getPreferenceCount() - 1; i >= 0; i--) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof RestrictedPreference) {
                    String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    boolean disabledByAdmin = false;
                    if ("unlock_set_off".equals(key)) {
                        enabled = quality <= 0;
                        if (getResources().getBoolean(R.bool.config_hide_none_security_option)) {
                            enabled = false;
                            visible = false;
                        }
                        disabledByAdmin = adminEnforcedQuality > 0;
                    } else if ("unlock_set_none".equals(key)) {
                        if (this.mUserId != UserHandle.myUserId()) {
                            visible = false;
                        }
                        enabled = quality <= 0;
                        disabledByAdmin = adminEnforcedQuality > 0;
                    } else if ("unlock_set_pattern".equals(key)) {
                        enabled = quality <= 65536;
                        disabledByAdmin = adminEnforcedQuality > 65536;
                    } else if ("unlock_set_pin".equals(key)) {
                        enabled = quality <= 196608;
                        disabledByAdmin = adminEnforcedQuality > 196608;
                    } else if ("unlock_set_password".equals(key)) {
                        enabled = quality <= 393216;
                        disabledByAdmin = adminEnforcedQuality > 393216;
                    } else if ("unlock_set_managed".equals(key)) {
                        if (quality > 524288) {
                            enabled = false;
                        } else {
                            enabled = this.mManagedPasswordProvider.isManagedPasswordChoosable();
                        }
                        disabledByAdmin = adminEnforcedQuality > 524288;
                    }
                    if (hideDisabled) {
                        visible = enabled;
                    }
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (disabledByAdmin && enforcedAdmin != null) {
                        ((RestrictedPreference) pref).setDisabledByAdmin(enforcedAdmin);
                    } else if (!enabled) {
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    } else {
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            if (!StorageManager.isBlockEncrypted() || StorageManager.isNonDefaultBlockEncrypted() || AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(-1).isEmpty()) {
                return;
            }
            CharSequence summary = getString(R.string.secure_lock_encryption_warning);
            PreferenceScreen screen = getPreferenceScreen();
            int preferenceCount = screen.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                Preference preference = screen.getPreference(i);
                String key = preference.getKey();
                if (key.equals("unlock_set_pattern") || key.equals("unlock_set_pin") || key.equals("unlock_set_password") || key.equals("unlock_set_managed")) {
                    preference.setSummary(summary);
                }
            }
        }

        protected Intent getLockManagedPasswordIntent(boolean requirePassword, String password) {
            return this.mManagedPasswordProvider.createIntent(requirePassword, password);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge, int userId) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge, userId);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password, userId);
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, long challenge, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, challenge, userId);
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, String pattern, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, pattern, userId);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required, Intent unlockMethodIntent) {
            return EncryptionInterstitial.createStartIntent(context, quality, required, unlockMethodIntent);
        }

        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            if (!this.mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }
            int quality2 = upgradeQuality(quality);
            Intent intent = getIntentForUnlockMethod(quality2, disabled);
            if (intent != null) {
                startActivityForResult(intent, 102);
                return;
            }
            if (quality2 == 0) {
                this.mLockPatternUtils.setSeparateProfileChallengeEnabled(this.mUserId, true, this.mUserPassword);
                this.mChooseLockSettingsHelper.utils().clearLock(this.mUserId);
                this.mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled, this.mUserId);
                removeAllFingerprintForUserAndFinish(this.mUserId);
                getActivity().setResult(-1);
                return;
            }
            removeAllFingerprintForUserAndFinish(this.mUserId);
        }

        private Intent getIntentForUnlockMethod(int quality, boolean disabled) {
            Intent intent = null;
            Context context = getActivity();
            if (quality >= 524288) {
                intent = getLockManagedPasswordIntent(this.mRequirePassword, this.mUserPassword);
            } else if (quality >= 131072) {
                int minLength = this.mDPM.getPasswordMinimumLength(null, this.mUserId);
                if (minLength < 4) {
                    minLength = 4;
                }
                int maxLength = this.mDPM.getPasswordMaximumLength(quality);
                if (this.mHasChallenge) {
                    intent = getLockPasswordIntent(context, quality, minLength, maxLength, this.mRequirePassword, this.mChallenge, this.mUserId);
                } else {
                    intent = getLockPasswordIntent(context, quality, minLength, maxLength, this.mRequirePassword, this.mUserPassword, this.mUserId);
                }
            } else if (quality == 65536) {
                if (this.mHasChallenge) {
                    intent = getLockPatternIntent(context, this.mRequirePassword, this.mChallenge, this.mUserId);
                } else {
                    intent = getLockPatternIntent(context, this.mRequirePassword, this.mUserPassword, this.mUserId);
                }
            }
            if (intent != null) {
                intent.putExtra(":settings:hide_drawer", this.mHideDrawer);
            }
            return intent;
        }

        private void removeAllFingerprintForUserAndFinish(final int userId) {
            if (this.mFingerprintManager != null && this.mFingerprintManager.isHardwareDetected()) {
                if (this.mFingerprintManager.hasEnrolledFingerprints(userId)) {
                    this.mFingerprintManager.setActiveUser(userId);
                    Fingerprint finger = new Fingerprint((CharSequence) null, userId, 0, 0L);
                    this.mFingerprintManager.remove(finger, userId, new FingerprintManager.RemovalCallback() {
                        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                            Log.v("ChooseLockGenericFragment", "Fingerprint removed: " + fp.getFingerId());
                            if (fp.getFingerId() != 0) {
                                return;
                            }
                            ChooseLockGenericFragment.this.removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                        }

                        public void onRemovalSucceeded(Fingerprint fingerprint) {
                            if (fingerprint.getFingerId() != 0) {
                                return;
                            }
                            ChooseLockGenericFragment.this.removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                        }
                    });
                    return;
                }
                removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                return;
            }
            finish();
        }

        public void removeManagedProfileFingerprintsAndFinishIfNecessary(int parentUserId) {
            this.mFingerprintManager.setActiveUser(UserHandle.myUserId());
            UserManager um = UserManager.get(getActivity());
            boolean hasChildProfile = false;
            if (!um.getUserInfo(parentUserId).isManagedProfile()) {
                List<UserInfo> profiles = um.getProfiles(parentUserId);
                int profilesSize = profiles.size();
                int i = 0;
                while (true) {
                    if (i >= profilesSize) {
                        break;
                    }
                    UserInfo userInfo = profiles.get(i);
                    if (!userInfo.isManagedProfile() || this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id)) {
                        i++;
                    } else {
                        removeAllFingerprintForUserAndFinish(userInfo.id);
                        hasChildProfile = true;
                        break;
                    }
                }
            }
            if (hasChildProfile) {
                return;
            }
            finish();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), this.mUserId);
            return isProfile ? R.string.unlock_disable_frp_warning_title_profile : R.string.unlock_disable_frp_warning_title;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = this.mFingerprintManager.hasEnrolledFingerprints(this.mUserId);
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), this.mUserId);
            switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mUserId)) {
                case 65536:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_fingerprint_profile;
                    }
                    if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_fingerprint;
                    }
                    if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_profile;
                    }
                    return R.string.unlock_disable_frp_warning_content_pattern;
                case 131072:
                case 196608:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint_profile;
                    }
                    if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint;
                    }
                    if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_profile;
                    }
                    return R.string.unlock_disable_frp_warning_content_pin;
                case 262144:
                case 327680:
                case 393216:
                case 524288:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_fingerprint_profile;
                    }
                    if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_fingerprint;
                    }
                    if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_profile;
                    }
                    return R.string.unlock_disable_frp_warning_content_password;
                default:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_fingerprint_profile;
                    }
                    if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_fingerprint;
                    }
                    if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_profile;
                    }
                    return R.string.unlock_disable_frp_warning_content_unknown;
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return ("unlock_set_off".equals(unlockMethod) || "unlock_set_none".equals(unlockMethod)) ? false : true;
        }

        public boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(90200, unlockMethod);
            if ("unlock_set_off".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, true);
            } else if ("unlock_set_none".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, false);
            } else if ("unlock_set_managed".equals(unlockMethod)) {
                maybeEnableEncryption(524288, false);
            } else if ("unlock_set_pattern".equals(unlockMethod)) {
                maybeEnableEncryption(65536, false);
            } else if ("unlock_set_pin".equals(unlockMethod)) {
                maybeEnableEncryption(131072, false);
            } else if ("unlock_set_password".equals(unlockMethod)) {
                maybeEnableEncryption(262144, false);
            } else {
                Log.e("ChooseLockGenericFragment", "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }

        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            int title = getResIdForFactoryResetProtectionWarningTitle();
            int message = getResIdForFactoryResetProtectionWarningMessage();
            FactoryResetProtectionWarningDialog dialog = FactoryResetProtectionWarningDialog.newInstance(title, message, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), "frp_warning_dialog");
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {
            public static FactoryResetProtectionWarningDialog newInstance(int titleRes, int messageRes, String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag = new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt("titleRes", titleRes);
                args.putInt("messageRes", messageRes);
                args.putString("unlockMethodToSet", unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) != null) {
                    return;
                }
                super.show(manager, tag);
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                return new AlertDialog.Builder(getActivity()).setTitle(args.getInt("titleRes")).setMessage(args.getInt("messageRes")).setPositiveButton(R.string.unlock_disable_frp_warning_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((ChooseLockGenericFragment) FactoryResetProtectionWarningDialog.this.getParentFragment()).setUnlockMethod(args.getString("unlockMethodToSet"));
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        FactoryResetProtectionWarningDialog.this.dismiss();
                    }
                }).create();
            }
        }
    }
}
