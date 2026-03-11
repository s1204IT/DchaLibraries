package com.android.settings;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;

public class EncryptionInterstitial extends SettingsActivity {
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

    public static Intent createStartIntent(Context ctx, int quality, boolean requirePasswordDefault) {
        return new Intent(ctx, (Class<?>) EncryptionInterstitial.class).putExtra("extra_prefs_show_button_bar", true).putExtra("extra_prefs_set_back_text", (String) null).putExtra("extra_prefs_set_next_text", ctx.getString(R.string.encryption_continue_button)).putExtra("extra_password_quality", quality).putExtra(":settings:show_fragment_title_resid", R.string.encryption_interstitial_header).putExtra("extra_require_password", requirePasswordDefault);
    }

    public static class EncryptionInterstitialFragment extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, View.OnClickListener {
        private RadioButton mDontRequirePasswordToDecryptButton;
        private TextView mEncryptionMessage;
        private boolean mPasswordRequired;
        private RadioButton mRequirePasswordToDecryptButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int msgId;
            int enableId;
            int disableId;
            View view = inflater.inflate(R.layout.encryption_interstitial, container, false);
            this.mRequirePasswordToDecryptButton = (RadioButton) view.findViewById(R.id.encrypt_require_password);
            this.mDontRequirePasswordToDecryptButton = (RadioButton) view.findViewById(R.id.encrypt_dont_require_password);
            this.mEncryptionMessage = (TextView) view.findViewById(R.id.encryption_message);
            int quality = getActivity().getIntent().getIntExtra("extra_password_quality", 0);
            switch (quality) {
                case 65536:
                    msgId = R.string.encryption_interstitial_message_pattern;
                    enableId = R.string.encrypt_require_pattern;
                    disableId = R.string.encrypt_dont_require_pattern;
                    break;
                case 131072:
                case 196608:
                    msgId = R.string.encryption_interstitial_message_pin;
                    enableId = R.string.encrypt_require_pin;
                    disableId = R.string.encrypt_dont_require_pin;
                    break;
                default:
                    msgId = R.string.encryption_interstitial_message_password;
                    enableId = R.string.encrypt_require_password;
                    disableId = R.string.encrypt_dont_require_password;
                    break;
            }
            this.mEncryptionMessage.setText(msgId);
            this.mRequirePasswordToDecryptButton.setOnClickListener(this);
            this.mRequirePasswordToDecryptButton.setText(enableId);
            this.mDontRequirePasswordToDecryptButton.setOnClickListener(this);
            this.mDontRequirePasswordToDecryptButton.setText(disableId);
            setRequirePasswordState(getActivity().getIntent().getBooleanExtra("extra_require_password", true));
            return view;
        }

        @Override
        public void onClick(View v) {
            if (v == this.mRequirePasswordToDecryptButton) {
                boolean accEn = AccessibilityManager.getInstance(getActivity()).isEnabled();
                if (accEn && !this.mPasswordRequired) {
                    setRequirePasswordState(false);
                    showDialog(1);
                    return;
                } else {
                    setRequirePasswordState(true);
                    return;
                }
            }
            setRequirePasswordState(false);
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            int titleId;
            int messageId;
            CharSequence exampleAccessibility;
            switch (dialogId) {
                case 1:
                    int quality = new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality();
                    switch (quality) {
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
                    if (list.isEmpty()) {
                        exampleAccessibility = "";
                    } else {
                        exampleAccessibility = list.get(0).getResolveInfo().loadLabel(getPackageManager());
                    }
                    return new AlertDialog.Builder(getActivity()).setTitle(titleId).setMessage(getString(messageId, new Object[]{exampleAccessibility})).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                default:
                    throw new IllegalArgumentException();
            }
        }

        private void setRequirePasswordState(boolean required) {
            this.mPasswordRequired = required;
            this.mRequirePasswordToDecryptButton.setChecked(required);
            this.mDontRequirePasswordToDecryptButton.setChecked(!required);
            SettingsActivity sa = (SettingsActivity) getActivity();
            Intent resultIntentData = sa.getResultIntentData();
            if (resultIntentData == null) {
                resultIntentData = new Intent();
                sa.setResultIntentData(resultIntentData);
            }
            resultIntentData.putExtra("extra_require_password", this.mPasswordRequired);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                setRequirePasswordState(true);
            } else if (which == -2) {
                setRequirePasswordState(false);
            }
        }
    }
}
