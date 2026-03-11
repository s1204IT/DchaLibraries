package com.android.settings;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.List;

public class EncryptionInterstitial extends SettingsActivity {
    private static final String TAG = EncryptionInterstitial.class.getSimpleName();

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", EncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return EncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx, int quality, boolean requirePasswordDefault, Intent unlockMethodIntent) {
        return new Intent(ctx, (Class<?>) EncryptionInterstitial.class).putExtra("extra_password_quality", quality).putExtra(":settings:show_fragment_title_resid", R.string.encryption_interstitial_header).putExtra("extra_require_password", requirePasswordDefault).putExtra("extra_unlock_method_intent", unlockMethodIntent);
    }

    public static class EncryptionInterstitialFragment extends SettingsPreferenceFragment implements DialogInterface.OnClickListener {
        private Preference mDontRequirePasswordToDecrypt;
        private boolean mPasswordRequired;
        private int mRequestedPasswordQuality;
        private Preference mRequirePasswordToDecrypt;
        private Intent mUnlockMethodIntent;

        @Override
        protected int getMetricsCategory() {
            return 48;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            int msgId;
            int enableId;
            int disableId;
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.security_settings_encryption_interstitial);
            findPreference("encrypt_dont_require_password").setViewId(R.id.encrypt_dont_require_password);
            this.mRequirePasswordToDecrypt = findPreference("encrypt_require_password");
            this.mDontRequirePasswordToDecrypt = findPreference("encrypt_dont_require_password");
            boolean forFingerprint = getActivity().getIntent().getBooleanExtra("for_fingerprint", false);
            Intent intent = getActivity().getIntent();
            this.mRequestedPasswordQuality = intent.getIntExtra("extra_password_quality", 0);
            this.mUnlockMethodIntent = (Intent) intent.getParcelableExtra("extra_unlock_method_intent");
            switch (this.mRequestedPasswordQuality) {
                case 65536:
                    if (forFingerprint) {
                        msgId = R.string.encryption_interstitial_message_pattern_for_fingerprint;
                    } else {
                        msgId = R.string.encryption_interstitial_message_pattern;
                    }
                    enableId = R.string.encrypt_require_pattern;
                    disableId = R.string.encrypt_dont_require_pattern;
                    break;
                case 131072:
                case 196608:
                    if (forFingerprint) {
                        msgId = R.string.encryption_interstitial_message_pin_for_fingerprint;
                    } else {
                        msgId = R.string.encryption_interstitial_message_pin;
                    }
                    enableId = R.string.encrypt_require_pin;
                    disableId = R.string.encrypt_dont_require_pin;
                    break;
                default:
                    if (forFingerprint) {
                        msgId = R.string.encryption_interstitial_message_password_for_fingerprint;
                    } else {
                        msgId = R.string.encryption_interstitial_message_password;
                    }
                    enableId = R.string.encrypt_require_password;
                    disableId = R.string.encrypt_dont_require_password;
                    break;
            }
            TextView message = createHeaderView();
            message.setText(msgId);
            setHeaderView(message);
            this.mRequirePasswordToDecrypt.setTitle(enableId);
            this.mDontRequirePasswordToDecrypt.setTitle(disableId);
            setRequirePasswordState(getActivity().getIntent().getBooleanExtra("extra_require_password", true));
        }

        protected TextView createHeaderView() {
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.encryption_interstitial_header, (ViewGroup) null, false);
            return message;
        }

        protected void startLockIntent() {
            if (this.mUnlockMethodIntent != null) {
                this.mUnlockMethodIntent.putExtra("extra_require_password", this.mPasswordRequired);
                startActivityForResult(this.mUnlockMethodIntent, 100);
            } else {
                Log.wtf(EncryptionInterstitial.TAG, "no unlock intent to start");
                finish();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode != 100 || resultCode == 0) {
                return;
            }
            getActivity().setResult(resultCode, data);
            finish();
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if (key.equals("encrypt_require_password")) {
                boolean accEn = AccessibilityManager.getInstance(getActivity()).isEnabled();
                if (accEn && !this.mPasswordRequired) {
                    setRequirePasswordState(false);
                    showDialog(1);
                } else {
                    setRequirePasswordState(true);
                    startLockIntent();
                }
            } else {
                setRequirePasswordState(false);
                startLockIntent();
            }
            return true;
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            int titleId;
            int messageId;
            switch (dialogId) {
                case DefaultWfcSettingsExt.PAUSE:
                    switch (this.mRequestedPasswordQuality) {
                        case 65536:
                            titleId = R.string.encrypt_talkback_dialog_require_pattern;
                            messageId = R.string.encrypt_talkback_dialog_message_pattern;
                            break;
                        case 131072:
                        case 196608:
                            titleId = R.string.encrypt_talkback_dialog_require_pin;
                            messageId = R.string.encrypt_talkback_dialog_message_pin;
                            break;
                        default:
                            titleId = R.string.encrypt_talkback_dialog_require_password;
                            messageId = R.string.encrypt_talkback_dialog_message_password;
                            break;
                    }
                    List<AccessibilityServiceInfo> list = AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(-1);
                    CharSequence exampleAccessibility = list.isEmpty() ? "" : list.get(0).getResolveInfo().loadLabel(getPackageManager());
                    return new AlertDialog.Builder(getActivity()).setTitle(titleId).setMessage(getString(messageId, new Object[]{exampleAccessibility})).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                default:
                    throw new IllegalArgumentException();
            }
        }

        private void setRequirePasswordState(boolean required) {
            this.mPasswordRequired = required;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                setRequirePasswordState(true);
                startLockIntent();
            } else {
                if (which != -2) {
                    return;
                }
                setRequirePasswordState(false);
            }
        }
    }
}
