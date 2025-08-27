package com.android.settings;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.setupwizardlib.GlifLayout;
import java.util.List;

/* loaded from: classes.dex */
public class EncryptionInterstitial extends SettingsActivity {
    private static final String TAG = EncryptionInterstitial.class.getSimpleName();

    @Override // com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", EncryptionInterstitialFragment.class.getName());
        return intent;
    }

    @Override // android.app.Activity, android.view.ContextThemeWrapper
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), z);
    }

    @Override // com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return EncryptionInterstitialFragment.class.getName().equals(str);
    }

    public static Intent createStartIntent(Context context, int i, boolean z, Intent intent) {
        return new Intent(context, (Class<?>) EncryptionInterstitial.class).putExtra("extra_password_quality", i).putExtra(":settings:show_fragment_title_resid", R.string.encryption_interstitial_header).putExtra("extra_require_password", z).putExtra("extra_unlock_method_intent", intent);
    }

    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static class EncryptionInterstitialFragment extends InstrumentedFragment implements View.OnClickListener {
        private View mDontRequirePasswordToDecrypt;
        private boolean mPasswordRequired;
        private int mRequestedPasswordQuality;
        private View mRequirePasswordToDecrypt;
        private Intent mUnlockMethodIntent;

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 48;
        }

        @Override // android.app.Fragment
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.encryption_interstitial, viewGroup, false);
        }

        @Override // android.app.Fragment
        public void onViewCreated(View view, Bundle bundle) {
            int i;
            super.onViewCreated(view, bundle);
            this.mRequirePasswordToDecrypt = view.findViewById(R.id.encrypt_require_password);
            this.mDontRequirePasswordToDecrypt = view.findViewById(R.id.encrypt_dont_require_password);
            boolean booleanExtra = getActivity().getIntent().getBooleanExtra("for_fingerprint", false);
            Intent intent = getActivity().getIntent();
            this.mRequestedPasswordQuality = intent.getIntExtra("extra_password_quality", 0);
            this.mUnlockMethodIntent = (Intent) intent.getParcelableExtra("extra_unlock_method_intent");
            int i2 = this.mRequestedPasswordQuality;
            if (i2 != 65536) {
                if (i2 == 131072 || i2 == 196608) {
                    if (booleanExtra) {
                        i = R.string.encryption_interstitial_message_pin_for_fingerprint;
                    } else {
                        i = R.string.encryption_interstitial_message_pin;
                    }
                } else if (booleanExtra) {
                    i = R.string.encryption_interstitial_message_password_for_fingerprint;
                } else {
                    i = R.string.encryption_interstitial_message_password;
                }
            } else if (booleanExtra) {
                i = R.string.encryption_interstitial_message_pattern_for_fingerprint;
            } else {
                i = R.string.encryption_interstitial_message_pattern;
            }
            ((TextView) getActivity().findViewById(R.id.encryption_message)).setText(i);
            this.mRequirePasswordToDecrypt.setOnClickListener(this);
            this.mDontRequirePasswordToDecrypt.setOnClickListener(this);
            setRequirePasswordState(getActivity().getIntent().getBooleanExtra("extra_require_password", true));
            ((GlifLayout) view).setHeaderText(getActivity().getTitle());
        }

        protected void startLockIntent() {
            if (this.mUnlockMethodIntent == null) {
                Log.wtf(EncryptionInterstitial.TAG, "no unlock intent to start");
                finish();
            } else {
                this.mUnlockMethodIntent.putExtra("extra_require_password", this.mPasswordRequired);
                startActivityForResult(this.mUnlockMethodIntent, 100);
            }
        }

        @Override // android.app.Fragment
        public void onActivityResult(int i, int i2, Intent intent) {
            super.onActivityResult(i, i2, intent);
            if (i == 100 && i2 != 0) {
                getActivity().setResult(i2, intent);
                finish();
            }
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (view == this.mRequirePasswordToDecrypt) {
                if (AccessibilityManager.getInstance(getActivity()).isEnabled() && !this.mPasswordRequired) {
                    setRequirePasswordState(false);
                    AccessibilityWarningDialogFragment.newInstance(this.mRequestedPasswordQuality).show(getChildFragmentManager(), "AccessibilityWarningDialog");
                    return;
                } else {
                    setRequirePasswordState(true);
                    startLockIntent();
                    return;
                }
            }
            setRequirePasswordState(false);
            startLockIntent();
        }

        private void setRequirePasswordState(boolean z) {
            this.mPasswordRequired = z;
        }

        public void finish() {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                activity.finish();
            }
        }
    }

    public static class AccessibilityWarningDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        public static AccessibilityWarningDialogFragment newInstance(int i) {
            AccessibilityWarningDialogFragment accessibilityWarningDialogFragment = new AccessibilityWarningDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putInt("extra_password_quality", i);
            accessibilityWarningDialogFragment.setArguments(bundle);
            return accessibilityWarningDialogFragment;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            int i;
            int i2;
            CharSequence charSequenceLoadLabel;
            int i3 = getArguments().getInt("extra_password_quality");
            if (i3 == 65536) {
                i = R.string.encrypt_talkback_dialog_require_pattern;
                i2 = R.string.encrypt_talkback_dialog_message_pattern;
            } else if (i3 == 131072 || i3 == 196608) {
                i = R.string.encrypt_talkback_dialog_require_pin;
                i2 = R.string.encrypt_talkback_dialog_message_pin;
            } else {
                i = R.string.encrypt_talkback_dialog_require_password;
                i2 = R.string.encrypt_talkback_dialog_message_password;
            }
            Activity activity = getActivity();
            List<AccessibilityServiceInfo> enabledAccessibilityServiceList = AccessibilityManager.getInstance(activity).getEnabledAccessibilityServiceList(-1);
            if (enabledAccessibilityServiceList.isEmpty()) {
                charSequenceLoadLabel = "";
            } else {
                charSequenceLoadLabel = enabledAccessibilityServiceList.get(0).getResolveInfo().loadLabel(activity.getPackageManager());
            }
            return new AlertDialog.Builder(activity).setTitle(i).setMessage(getString(i2, new Object[]{charSequenceLoadLabel})).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 581;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            EncryptionInterstitialFragment encryptionInterstitialFragment = (EncryptionInterstitialFragment) getParentFragment();
            if (encryptionInterstitialFragment != null) {
                if (i == -1) {
                    encryptionInterstitialFragment.setRequirePasswordState(true);
                    encryptionInterstitialFragment.startLockIntent();
                } else if (i == -2) {
                    encryptionInterstitialFragment.setRequirePasswordState(false);
                }
            }
        }
    }
}
