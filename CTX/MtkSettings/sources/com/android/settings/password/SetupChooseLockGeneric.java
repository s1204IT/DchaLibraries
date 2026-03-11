package com.android.settings.password;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupEncryptionInterstitial;
import com.android.settings.SetupWizardUtils;
import com.android.settings.fingerprint.SetupFingerprintEnrollFindSensor;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.utils.SettingsDividerItemDecoration;
import com.android.setupwizardlib.GlifPreferenceLayout;

public class SetupChooseLockGeneric extends ChooseLockGeneric {
    @Override
    protected boolean isValidFragment(String str) {
        return SetupChooseLockGenericFragment.class.getName().equals(str);
    }

    @Override
    Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), z);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGeneric.ChooseLockGenericFragment {
        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            GlifPreferenceLayout glifPreferenceLayout = (GlifPreferenceLayout) view;
            glifPreferenceLayout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));
            glifPreferenceLayout.setDividerInset(getContext().getResources().getDimensionPixelSize(R.dimen.suw_items_glif_text_divider_inset));
            glifPreferenceLayout.setIcon(getContext().getDrawable(R.drawable.ic_lock));
            int i = this.mForFingerprint ? R.string.lock_settings_picker_title : R.string.setup_lock_settings_picker_title;
            if (getActivity() != null) {
                getActivity().setTitle(i);
            }
            glifPreferenceLayout.setHeaderText(i);
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
        public void onActivityResult(int i, int i2, Intent intent) {
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(":settings:password_quality", new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
            super.onActivityResult(i, i2, intent);
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return ((GlifPreferenceLayout) viewGroup).onCreateRecyclerView(layoutInflater, viewGroup, bundle);
        }

        @Override
        protected boolean canRunBeforeDeviceProvisioned() {
            return true;
        }

        @Override
        protected void disableUnusablePreferences(int i, boolean z) {
            super.disableUnusablePreferencesImpl(Math.max(i, 65536), true);
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
            if ("unlock_set_do_later".equals(preference.getKey())) {
                SetupSkipDialog.newInstance(getActivity().getIntent().getBooleanExtra(":settings:frp_supported", false)).show(getFragmentManager());
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        protected Intent getLockPasswordIntent(int i, int i2, int i3) {
            Intent intentModifyIntentForSetup = SetupChooseLockPassword.modifyIntentForSetup(getContext(), super.getLockPasswordIntent(i, i2, i3));
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentModifyIntentForSetup);
            return intentModifyIntentForSetup;
        }

        @Override
        protected Intent getLockPatternIntent() {
            Intent intentModifyIntentForSetup = SetupChooseLockPattern.modifyIntentForSetup(getContext(), super.getLockPatternIntent());
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentModifyIntentForSetup);
            return intentModifyIntentForSetup;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int i, boolean z, Intent intent) {
            Intent intentCreateStartIntent = SetupEncryptionInterstitial.createStartIntent(context, i, z, intent);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentCreateStartIntent);
            return intentCreateStartIntent;
        }

        @Override
        protected Intent getFindSensorIntent(Context context) {
            Intent intent = new Intent(context, (Class<?>) SetupFingerprintEnrollFindSensor.class);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }
    }
}
