package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import com.android.settings.ChooseLockPassword;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

public class SetupChooseLockPassword extends ChooseLockPassword implements SetupWizardNavBar.NavigationBarListener {
    private SetupChooseLockPasswordFragment mFragment;
    private SetupWizardNavBar mNavigationBar;

    public static Intent createIntent(Context context, int quality, boolean isFallback, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, isFallback, minLength, maxLength, requirePasswordToDecrypt, confirmCredentials);
        intent.setClass(context, SetupChooseLockPassword.class);
        intent.putExtra("extra_prefs_show_button_bar", false);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent(), resid), first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        this.mNavigationBar = bar;
        SetupWizardUtils.setImmersiveMode(this, bar);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        if (this.mFragment != null) {
            this.mFragment.handleNext();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof SetupChooseLockPasswordFragment) {
            this.mFragment = (SetupChooseLockPasswordFragment) fragment;
        }
    }

    public static class SetupChooseLockPasswordFragment extends ChooseLockPassword.ChooseLockPasswordFragment implements View.OnApplyWindowInsetsListener {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.setup_template, container, false);
            View scrollView = view.findViewById(R.id.bottom_scroll_view);
            scrollView.setOnApplyWindowInsetsListener(this);
            ViewGroup setupContent = (ViewGroup) view.findViewById(R.id.setup_content);
            inflater.inflate(R.layout.setup_choose_lock_password, setupContent, true);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(), R.drawable.setup_illustration_lock_screen);
            SetupWizardUtils.setHeaderText(getActivity(), getActivity().getTitle());
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            Intent intent = SetupRedactionInterstitial.createStartIntent(context);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected void setNextEnabled(boolean enabled) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            activity.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        @Override
        protected void setNextText(int text) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            activity.mNavigationBar.getNextButton().setText(text);
        }

        @Override
        public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
            SetupChooseLockPassword activity = (SetupChooseLockPassword) getActivity();
            int bottomMargin = Math.max(insets.getSystemWindowInsetBottom() - activity.mNavigationBar.getView().getHeight(), 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottomMargin);
            view.setLayoutParams(lp);
            return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
        }
    }
}
