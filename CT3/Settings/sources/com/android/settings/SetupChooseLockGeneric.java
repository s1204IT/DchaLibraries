package com.android.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.fingerprint.SetupSkipDialog;
import com.android.setupwizardlib.SetupWizardPreferenceLayout;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupChooseLockGeneric extends ChooseLockGeneric {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    @Override
    Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        int resid2 = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid2, first);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGeneric.ChooseLockGenericFragment implements NavigationBar.NavigationBarListener {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setImmersiveMode(getActivity());
            SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) view;
            layout.setDividerInset(getContext().getResources().getDimensionPixelSize(R.dimen.suw_items_text_divider_inset));
            NavigationBar navigationBar = layout.getNavigationBar();
            Button nextButton = navigationBar.getNextButton();
            nextButton.setText((CharSequence) null);
            nextButton.setEnabled(false);
            navigationBar.setNavigationBarListener(this);
            layout.setIllustration(R.drawable.setup_illustration_lock_screen, R.drawable.setup_illustration_horizontal_tile);
            if (!this.mForFingerprint) {
                layout.setHeaderText(R.string.setup_lock_settings_picker_title);
            } else {
                layout.setHeaderText(R.string.lock_settings_picker_title);
            }
            setDivider(null);
        }

        @Override
        protected void addHeaderView() {
            if (this.mForFingerprint) {
                setHeaderView(R.layout.setup_choose_lock_generic_fingerprint_header);
            } else {
                setHeaderView(R.layout.setup_choose_lock_generic_header);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == 0) {
                return;
            }
            if (data == null) {
                data = new Intent();
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            data.putExtra(":settings:password_quality", lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.SetupRedactionInterstitial");
            packageManager.setComponentEnabledSetting(componentName, 1, 1);
            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        @Override
        protected boolean canRunBeforeDeviceProvisioned() {
            return true;
        }

        @Override
        protected void disableUnusablePreferences(int quality, boolean hideDisabled) {
            int newQuality = Math.max(quality, 65536);
            super.disableUnusablePreferencesImpl(newQuality, true);
        }

        @Override
        protected void addPreferences() {
            if (this.mForFingerprint) {
                super.addPreferences();
            } else {
                addPreferencesFromResource(R.xml.setup_security_settings_picker);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if ("unlock_set_do_later".equals(key)) {
                SetupSkipDialog dialog = SetupSkipDialog.newInstance(getActivity().getIntent().getBooleanExtra(":settings:frp_supported", false));
                dialog.show(getFragmentManager());
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge, int userId) {
            Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
            Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, boolean requirePassword, long challenge, int userId) {
            Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, boolean requirePassword, String pattern, int userId) {
            Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword, pattern);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required, Intent unlockMethodIntent) {
            Intent intent = SetupEncryptionInterstitial.createStartIntent(context, quality, required, unlockMethodIntent);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.onBackPressed();
        }

        @Override
        public void onNavigateNext() {
        }
    }
}
