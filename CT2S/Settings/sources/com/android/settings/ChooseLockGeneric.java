package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.security.KeyStore;
import android.util.EventLog;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ListView;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;

public class ChooseLockGeneric extends SettingsActivity {

    public static class InternalActivity extends ChooseLockGeneric {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
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
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private boolean mEncryptionRequestDisabled;
        private int mEncryptionRequestQuality;
        private KeyStore mKeyStore;
        private LockPatternUtils mLockPatternUtils;
        private boolean mRequirePassword;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private boolean mFinishPending = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
            this.mKeyStore = KeyStore.getInstance();
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            boolean confirmCredentials = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            if (getActivity() instanceof InternalActivity) {
                this.mPasswordConfirmed = !confirmCredentials;
            }
            if (savedInstanceState != null) {
                this.mPasswordConfirmed = savedInstanceState.getBoolean("password_confirmed");
                this.mWaitingForConfirmation = savedInstanceState.getBoolean("waiting_for_confirmation");
                this.mFinishPending = savedInstanceState.getBoolean("finish_pending");
                this.mEncryptionRequestQuality = savedInstanceState.getInt("encrypt_requested_quality");
                this.mEncryptionRequestDisabled = savedInstanceState.getBoolean("encrypt_requested_disabled");
            }
            if (this.mPasswordConfirmed) {
                updatePreferencesOrFinish();
                return;
            }
            if (!this.mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
                if (!helper.launchConfirmationActivity(100, null, null)) {
                    this.mPasswordConfirmed = true;
                    updatePreferencesOrFinish();
                } else {
                    this.mWaitingForConfirmation = true;
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (this.mFinishPending) {
                this.mFinishPending = false;
                finish();
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (isUnlockMethodSecure(key) || !this.mLockPatternUtils.isSecure()) {
                return setUnlockMethod(key);
            }
            showFactoryResetProtectionWarningDialog(key);
            return true;
        }

        private void maybeEnableEncryption(int quality, boolean disabled) {
            if (Process.myUserHandle().isOwner() && LockPatternUtils.isDeviceEncryptionEnabled()) {
                this.mEncryptionRequestQuality = quality;
                this.mEncryptionRequestDisabled = disabled;
                Context context = getActivity();
                boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                boolean required = this.mLockPatternUtils.isCredentialRequiredToDecrypt(accEn ? false : true);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required);
                startActivityForResult(intent, 102);
                return;
            }
            this.mRequirePassword = false;
            updateUnlockMethodAndFinish(quality, disabled);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            boolean onlyShowFallback = getActivity().getIntent().getBooleanExtra("lockscreen.biometric_weak_fallback", false);
            if (onlyShowFallback) {
                View header = View.inflate(getActivity(), R.layout.weak_biometric_fallback_header, null);
                ((ListView) v.findViewById(android.R.id.list)).addHeaderView(header, null, false);
            }
            return v;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            this.mWaitingForConfirmation = false;
            if (requestCode == 100 && resultCode == -1) {
                this.mPasswordConfirmed = true;
                updatePreferencesOrFinish();
                return;
            }
            if (requestCode == 101) {
                this.mChooseLockSettingsHelper.utils().deleteTempGallery();
                getActivity().setResult(resultCode);
                finish();
            } else if (requestCode == 102 && resultCode == -1) {
                this.mRequirePassword = data.getBooleanExtra("extra_require_password", true);
                updateUnlockMethodAndFinish(this.mEncryptionRequestQuality, this.mEncryptionRequestDisabled);
            } else {
                getActivity().setResult(0);
                finish();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("password_confirmed", this.mPasswordConfirmed);
            outState.putBoolean("waiting_for_confirmation", this.mWaitingForConfirmation);
            outState.putBoolean("finish_pending", this.mFinishPending);
            outState.putInt("encrypt_requested_quality", this.mEncryptionRequestQuality);
            outState.putBoolean("encrypt_requested_disabled", this.mEncryptionRequestDisabled);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra("lockscreen.password_type", -1);
            if (quality == -1) {
                int quality2 = intent.getIntExtra("minimum_quality", -1);
                MutableBoolean allowBiometric = new MutableBoolean(false);
                int quality3 = upgradeQuality(quality2, allowBiometric);
                PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferencesFromResource(R.xml.security_settings_picker);
                disableUnusablePreferences(quality3, allowBiometric);
                updatePreferenceSummaryIfNeeded();
                return;
            }
            updateUnlockMethodAndFinish(quality, false);
        }

        private int upgradeQuality(int quality, MutableBoolean allowBiometric) {
            return upgradeQualityForKeyStore(upgradeQualityForDPM(quality));
        }

        private int upgradeQualityForDPM(int quality) {
            int minQuality = this.mDPM.getPasswordQuality(null);
            if (quality < minQuality) {
                return minQuality;
            }
            return quality;
        }

        private int upgradeQualityForKeyStore(int quality) {
            if (!this.mKeyStore.isEmpty() && quality < 65536) {
                return 65536;
            }
            return quality;
        }

        protected void disableUnusablePreferences(int quality, MutableBoolean allowBiometric) {
            disableUnusablePreferencesImpl(quality, allowBiometric, false);
        }

        protected void disableUnusablePreferencesImpl(int quality, MutableBoolean allowBiometric, boolean hideDisabled) {
            PreferenceScreen entries = getPreferenceScreen();
            Intent intent = getActivity().getIntent();
            boolean onlyShowFallback = intent.getBooleanExtra("lockscreen.biometric_weak_fallback", false);
            boolean weakBiometricAvailable = this.mChooseLockSettingsHelper.utils().isBiometricWeakInstalled();
            UserManager mUm = (UserManager) getSystemService("user");
            List<UserInfo> users = mUm.getUsers(true);
            boolean singleUser = users.size() == 1;
            for (int i = entries.getPreferenceCount() - 1; i >= 0; i--) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    String key = ((PreferenceScreen) pref).getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    if ("unlock_set_off".equals(key)) {
                        enabled = quality <= 0;
                        visible = singleUser;
                    } else if ("unlock_set_none".equals(key)) {
                        enabled = quality <= 0;
                    } else if ("unlock_set_biometric_weak".equals(key)) {
                        enabled = quality <= 32768 || allowBiometric.value;
                        visible = weakBiometricAvailable;
                    } else if ("unlock_set_pattern".equals(key)) {
                        enabled = quality <= 65536;
                    } else if ("unlock_set_pin".equals(key)) {
                        enabled = quality <= 196608;
                    } else if ("unlock_set_password".equals(key)) {
                        enabled = quality <= 393216;
                    }
                    if (hideDisabled) {
                        visible = visible && enabled;
                    }
                    if (!visible || (onlyShowFallback && !allowedForFallback(key))) {
                        entries.removePreference(pref);
                    } else if (!enabled) {
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            Preference preference;
            if (!LockPatternUtils.isDeviceEncrypted() && !AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(-1).isEmpty()) {
                CharSequence summary = getString(R.string.secure_lock_encryption_warning);
                PreferenceScreen screen = getPreferenceScreen();
                int preferenceCount = screen.getPreferenceCount();
                for (int i = 0; i < preferenceCount; i++) {
                    preference = screen.getPreference(i);
                    switch (preference.getKey()) {
                        case "unlock_set_pattern":
                        case "unlock_set_pin":
                        case "unlock_set_password":
                            preference.setSummary(summary);
                            break;
                    }
                }
            }
        }

        private boolean allowedForFallback(String key) {
            return "unlock_backup_info".equals(key) || "unlock_set_pattern".equals(key) || "unlock_set_pin".equals(key);
        }

        private Intent getBiometricSensorIntent() {
            Intent fallBackIntent = new Intent().setClass(getActivity(), InternalActivity.class);
            fallBackIntent.putExtra("lockscreen.biometric_weak_fallback", true);
            fallBackIntent.putExtra("confirm_credentials", false);
            fallBackIntent.putExtra(":settings:show_fragment_title", R.string.backup_lock_settings_picker_title);
            Intent intent = new Intent();
            intent.setClassName("com.android.facelock", "com.android.facelock.SetupIntro");
            intent.putExtra("showTutorial", true);
            PendingIntent pending = PendingIntent.getActivity(getActivity(), 0, fallBackIntent, 0);
            intent.putExtra("PendingIntent", pending);
            return intent;
        }

        protected Intent getLockPasswordIntent(Context context, int quality, boolean isFallback, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            return ChooseLockPassword.createIntent(context, quality, isFallback, minLength, maxLength, requirePasswordToDecrypt, confirmCredentials);
        }

        protected Intent getLockPatternIntent(Context context, boolean isFallback, boolean requirePassword, boolean confirmCredentials) {
            return ChooseLockPattern.createIntent(context, isFallback, requirePassword, confirmCredentials);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required) {
            return EncryptionInterstitial.createStartIntent(context, quality, required);
        }

        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            if (!this.mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }
            boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.biometric_weak_fallback", false);
            int quality2 = upgradeQuality(quality, null);
            Context context = getActivity();
            if (quality2 >= 131072) {
                int minLength = this.mDPM.getPasswordMinimumLength(null);
                if (minLength < 4) {
                    minLength = 4;
                }
                int maxLength = this.mDPM.getPasswordMaximumLength(quality2);
                Intent intent = getLockPasswordIntent(context, quality2, isFallback, minLength, maxLength, this.mRequirePassword, false);
                if (isFallback) {
                    startActivityForResult(intent, 101);
                    return;
                }
                this.mFinishPending = true;
                intent.addFlags(33554432);
                startActivity(intent);
                return;
            }
            if (quality2 == 65536) {
                Intent intent2 = getLockPatternIntent(context, isFallback, this.mRequirePassword, false);
                if (isFallback) {
                    startActivityForResult(intent2, 101);
                    return;
                }
                this.mFinishPending = true;
                intent2.addFlags(33554432);
                startActivity(intent2);
                return;
            }
            if (quality2 == 32768) {
                Intent intent3 = getBiometricSensorIntent();
                this.mFinishPending = true;
                startActivity(intent3);
            } else {
                if (quality2 == 0) {
                    this.mChooseLockSettingsHelper.utils().clearLock(false);
                    this.mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled);
                    getActivity().setResult(-1);
                    finish();
                    return;
                }
                finish();
            }
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case 65536:
                    return R.string.unlock_disable_lock_pattern_summary;
                case 131072:
                case 196608:
                    return R.string.unlock_disable_lock_pin_summary;
                case 262144:
                case 327680:
                case 393216:
                    return R.string.unlock_disable_lock_password_summary;
                default:
                    return R.string.unlock_disable_lock_unknown_summary;
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return ("unlock_set_off".equals(unlockMethod) || "unlock_set_none".equals(unlockMethod)) ? false : true;
        }

        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(90200, unlockMethod);
            if ("unlock_set_off".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, true);
            } else if ("unlock_set_none".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, false);
            } else if ("unlock_set_biometric_weak".equals(unlockMethod)) {
                maybeEnableEncryption(32768, false);
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
            FactoryResetProtectionWarningDialog dialog = FactoryResetProtectionWarningDialog.newInstance(title, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), "frp_warning_dialog");
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {
            public static FactoryResetProtectionWarningDialog newInstance(int title, String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag = new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt("titleRes", title);
                args.putString("unlockMethodToSet", unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                return new AlertDialog.Builder(getActivity()).setTitle(args.getInt("titleRes")).setMessage(R.string.unlock_disable_frp_warning_content).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
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
