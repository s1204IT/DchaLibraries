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

/* loaded from: classes.dex */
public class SetupChooseLockGeneric extends ChooseLockGeneric {
    @Override // com.android.settings.password.ChooseLockGeneric, com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return SetupChooseLockGenericFragment.class.getName().equals(str);
    }

    @Override // com.android.settings.password.ChooseLockGeneric
    Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override // android.app.Activity, android.view.ContextThemeWrapper
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), z);
    }

    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGeneric.ChooseLockGenericFragment {
        @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
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

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected void addHeaderView() {
            if (this.mForFingerprint) {
                setHeaderView(R.layout.setup_choose_lock_generic_fingerprint_header);
            } else {
                setHeaderView(R.layout.setup_choose_lock_generic_header);
            }
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment, android.app.Fragment
        public void onActivityResult(int i, int i2, Intent intent) {
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(":settings:password_quality", new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
            super.onActivityResult(i, i2, intent);
        }

        @Override // android.support.v14.preference.PreferenceFragment
        public RecyclerView onCreateRecyclerView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return ((GlifPreferenceLayout) viewGroup).onCreateRecyclerView(layoutInflater, viewGroup, bundle);
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected boolean canRunBeforeDeviceProvisioned() {
            return true;
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected void disableUnusablePreferences(int i, boolean z) {
            super.disableUnusablePreferencesImpl(Math.max(i, 65536), true);
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected void addPreferences() {
            if (this.mForFingerprint) {
                super.addPreferences();
            } else {
                addPreferencesFromResource(R.xml.setup_security_settings_picker);
            }
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
        public boolean onPreferenceTreeClick(Preference preference) {
            if ("unlock_set_do_later".equals(preference.getKey())) {
                SetupSkipDialog.newInstance(getActivity().getIntent().getBooleanExtra(":settings:frp_supported", false)).show(getFragmentManager());
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected Intent getLockPasswordIntent(int i, int i2, int i3) {
            Intent intentModifyIntentForSetup = SetupChooseLockPassword.modifyIntentForSetup(getContext(), super.getLockPasswordIntent(i, i2, i3));
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentModifyIntentForSetup);
            return intentModifyIntentForSetup;
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected Intent getLockPatternIntent() {
            Intent intentModifyIntentForSetup = SetupChooseLockPattern.modifyIntentForSetup(getContext(), super.getLockPatternIntent());
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentModifyIntentForSetup);
            return intentModifyIntentForSetup;
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected Intent getEncryptionInterstitialIntent(Context context, int i, boolean z, Intent intent) {
            Intent intentCreateStartIntent = SetupEncryptionInterstitial.createStartIntent(context, i, z, intent);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intentCreateStartIntent);
            return intentCreateStartIntent;
        }

        @Override // com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment
        protected Intent getFindSensorIntent(Context context) {
            Intent intent = new Intent(context, (Class<?>) SetupFingerprintEnrollFindSensor.class);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }
    }
}
