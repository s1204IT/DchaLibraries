package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.RestrictedRadioButton;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

public class RedactionInterstitial extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", RedactionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return RedactionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx, int userId) {
        return new Intent(ctx, (Class<?>) RedactionInterstitial.class).putExtra("extra_prefs_show_button_bar", true).putExtra("extra_prefs_set_back_text", (String) null).putExtra("extra_prefs_set_next_text", ctx.getString(R.string.app_notifications_dialog_done)).putExtra(":settings:show_fragment_title_resid", Utils.isManagedProfile(UserManager.get(ctx), userId) ? R.string.lock_screen_notifications_interstitial_title_profile : R.string.lock_screen_notifications_interstitial_title).putExtra("android.intent.extra.USER_ID", userId);
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment implements RadioGroup.OnCheckedChangeListener {
        private RadioGroup mRadioGroup;
        private RestrictedRadioButton mRedactSensitiveButton;
        private RestrictedRadioButton mShowAllButton;
        private int mUserId;

        @Override
        protected int getMetricsCategory() {
            return 74;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.redaction_interstitial, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            this.mShowAllButton = (RestrictedRadioButton) view.findViewById(R.id.show_all);
            this.mRedactSensitiveButton = (RestrictedRadioButton) view.findViewById(R.id.redact_sensitive);
            this.mRadioGroup.setOnCheckedChangeListener(this);
            this.mUserId = Utils.getUserIdFromBundle(getContext(), getActivity().getIntent().getExtras());
            if (!Utils.isManagedProfile(UserManager.get(getContext()), this.mUserId)) {
                return;
            }
            ((TextView) view.findViewById(R.id.message)).setText(R.string.lock_screen_notifications_interstitial_message_profile);
            this.mShowAllButton.setText(R.string.lock_screen_notifications_summary_show_profile);
            this.mRedactSensitiveButton.setText(R.string.lock_screen_notifications_summary_hide_profile);
            ((RadioButton) view.findViewById(R.id.hide_all)).setText(R.string.lock_screen_notifications_summary_disable_profile);
        }

        @Override
        public void onResume() {
            super.onResume();
            checkNotificationFeaturesAndSetDisabled(this.mShowAllButton, 12);
            checkNotificationFeaturesAndSetDisabled(this.mRedactSensitiveButton, 4);
            loadFromSettings();
        }

        private void checkNotificationFeaturesAndSetDisabled(RestrictedRadioButton button, int keyguardNotifications) {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(getActivity(), keyguardNotifications, this.mUserId);
            button.setDisabledByAdmin(admin);
        }

        private void loadFromSettings() {
            boolean enabled = Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_show_notifications", 0, this.mUserId) != 0;
            boolean show = Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_allow_private_notifications", 1, this.mUserId) != 0;
            int checkedButtonId = R.id.hide_all;
            if (enabled) {
                if (show && !this.mShowAllButton.isDisabledByAdmin()) {
                    checkedButtonId = R.id.show_all;
                } else if (!this.mRedactSensitiveButton.isDisabledByAdmin()) {
                    checkedButtonId = R.id.redact_sensitive;
                }
            }
            this.mRadioGroup.check(checkedButtonId);
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            boolean show = checkedId == R.id.show_all;
            boolean enabled = checkedId != R.id.hide_all;
            Settings.Secure.putIntForUser(getContentResolver(), "lock_screen_allow_private_notifications", show ? 1 : 0, this.mUserId);
            Settings.Secure.putIntForUser(getContentResolver(), "lock_screen_show_notifications", enabled ? 1 : 0, this.mUserId);
        }
    }
}
