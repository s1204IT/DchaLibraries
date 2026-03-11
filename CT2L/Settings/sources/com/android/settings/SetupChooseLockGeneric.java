package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.android.settings.ChooseLockGeneric;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

public class SetupChooseLockGeneric extends ChooseLockGeneric implements SetupWizardNavBar.NavigationBarListener {
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
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent(), resid), first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
        bar.getNextButton().setEnabled(false);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGeneric.ChooseLockGenericFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.setup_preference, container, false);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            View title = view.findViewById(R.id.title);
            if (title == null) {
                View header = inflater.inflate(R.layout.setup_wizard_header, (ViewGroup) list, false);
                list.addHeaderView(header, null, false);
            }
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(), R.drawable.setup_illustration_lock_screen);
            SetupWizardUtils.setHeaderText(getActivity(), getActivity().getTitle());
        }

        @Override
        protected void disableUnusablePreferences(int quality, MutableBoolean allowBiometric) {
            int newQuality = Math.max(quality, 65536);
            super.disableUnusablePreferencesImpl(newQuality, allowBiometric, true);
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, boolean isFallback, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            Intent intent = SetupChooseLockPassword.createIntent(context, quality, isFallback, minLength, maxLength, requirePasswordToDecrypt, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, boolean isFallback, boolean requirePassword, boolean confirmCredentials) {
            Intent intent = SetupChooseLockPattern.createIntent(context, isFallback, requirePassword, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required) {
            Intent intent = SetupEncryptionInterstitial.createStartIntent(context, quality, required);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }
    }
}
