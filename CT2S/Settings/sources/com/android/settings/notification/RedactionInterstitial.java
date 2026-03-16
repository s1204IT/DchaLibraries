package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

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

    public static Intent createStartIntent(Context ctx) {
        return new Intent(ctx, (Class<?>) RedactionInterstitial.class).putExtra("extra_prefs_show_button_bar", true).putExtra("extra_prefs_set_back_text", (String) null).putExtra("extra_prefs_set_next_text", ctx.getString(R.string.app_notifications_dialog_done));
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment implements View.OnClickListener {
        private RadioButton mHideAllButton;
        private RadioButton mRedactSensitiveButton;
        private RadioButton mShowAllButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.redaction_interstitial, container, false);
            this.mShowAllButton = (RadioButton) view.findViewById(R.id.show_all);
            this.mRedactSensitiveButton = (RadioButton) view.findViewById(R.id.redact_sensitive);
            this.mHideAllButton = (RadioButton) view.findViewById(R.id.hide_all);
            this.mShowAllButton.setOnClickListener(this);
            this.mRedactSensitiveButton.setOnClickListener(this);
            this.mHideAllButton.setOnClickListener(this);
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadFromSettings();
        }

        private void loadFromSettings() {
            boolean enabled = Settings.Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
            boolean show = Settings.Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 1) != 0;
            this.mShowAllButton.setChecked(enabled && show);
            this.mRedactSensitiveButton.setChecked(enabled && !show);
            this.mHideAllButton.setChecked(enabled ? false : true);
        }

        @Override
        public void onClick(View v) {
            boolean show = v == this.mShowAllButton;
            boolean enabled = v != this.mHideAllButton;
            Settings.Secure.putInt(getContentResolver(), "lock_screen_allow_private_notifications", show ? 1 : 0);
            Settings.Secure.putInt(getContentResolver(), "lock_screen_show_notifications", enabled ? 1 : 0);
        }
    }
}
