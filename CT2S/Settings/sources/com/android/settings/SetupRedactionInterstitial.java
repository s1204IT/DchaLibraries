package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.notification.RedactionInterstitial;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

public class SetupRedactionInterstitial extends RedactionInterstitial implements SetupWizardNavBar.NavigationBarListener {
    public static Intent createStartIntent(Context ctx) {
        Intent startIntent = RedactionInterstitial.createStartIntent(ctx);
        startIntent.setClass(ctx, SetupRedactionInterstitial.class);
        startIntent.putExtra("extra_prefs_show_button_bar", false).putExtra(":settings:show_fragment_title_resid", -1);
        return startIntent;
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", SetupEncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent(), resid), first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        setResult(-1, getResultIntentData());
        finish();
    }

    public static class SetupEncryptionInterstitialFragment extends RedactionInterstitial.RedactionInterstitialFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.setup_template, container, false);
            ViewGroup setupContent = (ViewGroup) view.findViewById(R.id.setup_content);
            View content = super.onCreateView(inflater, setupContent, savedInstanceState);
            setupContent.addView(content);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(), R.drawable.setup_illustration_lock_screen);
            SetupWizardUtils.setHeaderText(getActivity(), R.string.notification_section_header);
        }
    }
}
