package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.ChooseLockPattern;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

public class SetupChooseLockPattern extends ChooseLockPattern implements SetupWizardNavBar.NavigationBarListener {
    private SetupChooseLockPatternFragment mFragment;
    private SetupWizardNavBar mNavigationBar;

    public static Intent createIntent(Context context, boolean isFallback, boolean requirePassword, boolean confirmCredentials) {
        Intent intent = ChooseLockPattern.createIntent(context, isFallback, requirePassword, confirmCredentials);
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    @Override
    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
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
            this.mFragment.handleRightButton();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ChooseLockPattern.ChooseLockPatternFragment) {
            this.mFragment = (SetupChooseLockPatternFragment) fragment;
        }
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPattern.ChooseLockPatternFragment {
        private Button mRetryButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.setup_template, container, false);
            ViewGroup setupContent = (ViewGroup) view.findViewById(R.id.setup_content);
            inflater.inflate(R.layout.setup_choose_lock_pattern, setupContent, true);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            this.mRetryButton = (Button) view.findViewById(R.id.retryButton);
            this.mRetryButton.setOnClickListener(this);
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
        public void onClick(View v) {
            if (v == this.mRetryButton) {
                handleLeftButton();
            } else {
                super.onClick(v);
            }
        }

        @Override
        protected void setRightButtonEnabled(boolean enabled) {
            SetupChooseLockPattern activity = (SetupChooseLockPattern) getActivity();
            activity.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        @Override
        protected void setRightButtonText(int text) {
            SetupChooseLockPattern activity = (SetupChooseLockPattern) getActivity();
            activity.mNavigationBar.getNextButton().setText(text);
        }

        @Override
        protected void updateStage(ChooseLockPattern.ChooseLockPatternFragment.Stage stage) {
            super.updateStage(stage);
            this.mRetryButton.setEnabled(stage == ChooseLockPattern.ChooseLockPatternFragment.Stage.FirstChoiceValid);
        }
    }
}
