package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.RestrictedRadioButton;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

public class RedactionInterstitial extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", RedactionInterstitialFragment.class.getName());
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), z);
    }

    @Override
    protected boolean isValidFragment(String str) {
        return RedactionInterstitialFragment.class.getName().equals(str);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static Intent createStartIntent(Context context, int i) {
        int i2;
        Intent intent = new Intent(context, (Class<?>) RedactionInterstitial.class);
        if (UserManager.get(context).isManagedProfile(i)) {
            i2 = R.string.lock_screen_notifications_interstitial_title_profile;
        } else {
            i2 = R.string.lock_screen_notifications_interstitial_title;
        }
        return intent.putExtra(":settings:show_fragment_title_resid", i2).putExtra("android.intent.extra.USER_ID", i);
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
        private RadioGroup mRadioGroup;
        private RestrictedRadioButton mRedactSensitiveButton;
        private RestrictedRadioButton mShowAllButton;
        private int mUserId;

        @Override
        public int getMetricsCategory() {
            return 74;
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.redaction_interstitial, viewGroup, false);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            this.mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            this.mShowAllButton = (RestrictedRadioButton) view.findViewById(R.id.show_all);
            this.mRedactSensitiveButton = (RestrictedRadioButton) view.findViewById(R.id.redact_sensitive);
            this.mRadioGroup.setOnCheckedChangeListener(this);
            this.mUserId = Utils.getUserIdFromBundle(getContext(), getActivity().getIntent().getExtras());
            if (UserManager.get(getContext()).isManagedProfile(this.mUserId)) {
                ((TextView) view.findViewById(R.id.message)).setText(R.string.lock_screen_notifications_interstitial_message_profile);
                this.mShowAllButton.setText(R.string.lock_screen_notifications_summary_show_profile);
                this.mRedactSensitiveButton.setText(R.string.lock_screen_notifications_summary_hide_profile);
                ((RadioButton) view.findViewById(R.id.hide_all)).setVisibility(8);
            }
            ((Button) view.findViewById(R.id.redaction_done_button)).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.redaction_done_button) {
                SetupRedactionInterstitial.setEnabled(getContext(), false);
                RedactionInterstitial redactionInterstitial = (RedactionInterstitial) getActivity();
                if (redactionInterstitial != null) {
                    redactionInterstitial.setResult(-1, null);
                    finish();
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            checkNotificationFeaturesAndSetDisabled(this.mShowAllButton, 12);
            checkNotificationFeaturesAndSetDisabled(this.mRedactSensitiveButton, 4);
            loadFromSettings();
        }

        private void checkNotificationFeaturesAndSetDisabled(RestrictedRadioButton restrictedRadioButton, int i) {
            restrictedRadioButton.setDisabledByAdmin(RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(getActivity(), i, this.mUserId));
        }

        private void loadFromSettings() {
            boolean z = true;
            boolean z2 = UserManager.get(getContext()).isManagedProfile(this.mUserId) || Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_show_notifications", 0, this.mUserId) != 0;
            if (Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_allow_private_notifications", 1, this.mUserId) == 0) {
                z = false;
            }
            int i = R.id.hide_all;
            if (z2) {
                if (z && !this.mShowAllButton.isDisabledByAdmin()) {
                    i = R.id.show_all;
                } else if (!this.mRedactSensitiveButton.isDisabledByAdmin()) {
                    i = R.id.redact_sensitive;
                }
            }
            this.mRadioGroup.check(i);
        }

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            int i2 = 0;
            int i3 = i == R.id.show_all ? 1 : 0;
            if (i != R.id.hide_all) {
                i2 = 1;
            }
            Settings.Secure.putIntForUser(getContentResolver(), "lock_screen_allow_private_notifications", i3, this.mUserId);
            Settings.Secure.putIntForUser(getContentResolver(), "lock_screen_show_notifications", i2, this.mUserId);
        }
    }
}
