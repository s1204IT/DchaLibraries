package com.android.settings.accessibility;

import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.view.View;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SeekBarPreference;
import com.android.settings.widget.SwitchBar;

public class ToggleAutoclickPreferenceFragment extends ToggleFeaturePreferenceFragment implements SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener {
    private static final int[] mAutoclickPreferenceSummaries = {R.plurals.accessibilty_autoclick_preference_subtitle_extremely_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_very_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_long_delay, R.plurals.accessibilty_autoclick_preference_subtitle_very_long_delay};
    private SeekBarPreference mDelay;

    static CharSequence getAutoclickPreferenceSummary(Resources resources, int delay) {
        int summaryIndex = getAutoclickPreferenceSummaryIndex(delay);
        return resources.getQuantityString(mAutoclickPreferenceSummaries[summaryIndex], delay, Integer.valueOf(delay));
    }

    private static int getAutoclickPreferenceSummaryIndex(int delay) {
        if (delay <= 200) {
            return 0;
        }
        if (delay >= 1000) {
            return mAutoclickPreferenceSummaries.length - 1;
        }
        int rangeSize = 800 / (mAutoclickPreferenceSummaries.length - 1);
        return (delay - 200) / rangeSize;
    }

    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), preferenceKey, enabled ? 1 : 0);
        this.mDelay.setEnabled(enabled);
    }

    @Override
    protected int getMetricsCategory() {
        return 335;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.accessibility_autoclick_settings);
        int delay = Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_delay", 600);
        this.mDelay = (SeekBarPreference) findPreference("autoclick_delay");
        this.mDelay.setMax(delayToSeekBarProgress(1000));
        this.mDelay.setProgress(delayToSeekBarProgress(delay));
        this.mDelay.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        int value = Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_enabled", 0);
        this.mSwitchBar.setCheckedInternal(value == 1);
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mDelay.setEnabled(value == 1);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled("accessibility_autoclick_enabled", isChecked);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getString(R.string.accessibility_autoclick_preference_title));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mDelay && (newValue instanceof Integer)) {
            Settings.Secure.putInt(getContentResolver(), "accessibility_autoclick_delay", seekBarProgressToDelay(((Integer) newValue).intValue()));
            return true;
        }
        return false;
    }

    private int seekBarProgressToDelay(int progress) {
        return (progress * 100) + 200;
    }

    private int delayToSeekBarProgress(int delay) {
        return (delay - 200) / 100;
    }
}
