package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.EncryptionInterstitial;
import com.android.setupwizardlib.SetupWizardPreferenceLayout;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupEncryptionInterstitial extends EncryptionInterstitial {
    public static Intent createStartIntent(Context ctx, int quality, boolean requirePasswordDefault, Intent unlockMethodIntent) {
        Intent startIntent = EncryptionInterstitial.createStartIntent(ctx, quality, requirePasswordDefault, unlockMethodIntent);
        startIntent.setClass(ctx, SetupEncryptionInterstitial.class);
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
        int resid2 = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid2, first);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitial.EncryptionInterstitialFragment implements NavigationBar.NavigationBarListener {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) view;
            layout.setDividerInset(getContext().getResources().getDimensionPixelSize(R.dimen.suw_items_icon_divider_inset));
            layout.setIllustration(R.drawable.setup_illustration_lock_screen, R.drawable.setup_illustration_horizontal_tile);
            NavigationBar navigationBar = layout.getNavigationBar();
            navigationBar.setNavigationBarListener(this);
            Button nextButton = navigationBar.getNextButton();
            nextButton.setText((CharSequence) null);
            nextButton.setEnabled(false);
            layout.setHeaderText(R.string.encryption_interstitial_header);
            Activity activity = getActivity();
            if (activity != null) {
                SetupWizardUtils.setImmersiveMode(activity);
            }
            setDivider(null);
        }

        @Override
        protected TextView createHeaderView() {
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.setup_encryption_interstitial_header, (ViewGroup) null, false);
            return message;
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            SetupWizardPreferenceLayout layout = (SetupWizardPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
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
