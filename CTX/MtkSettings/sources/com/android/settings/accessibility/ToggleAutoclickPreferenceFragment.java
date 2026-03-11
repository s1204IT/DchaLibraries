package com.android.settings.accessibility;

import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.widget.SwitchBar;

public class ToggleAutoclickPreferenceFragment extends ToggleFeaturePreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static final int[] mAutoclickPreferenceSummaries = {R.plurals.accessibilty_autoclick_preference_subtitle_extremely_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_very_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_short_delay, R.plurals.accessibilty_autoclick_preference_subtitle_long_delay, R.plurals.accessibilty_autoclick_preference_subtitle_very_long_delay};
    private SeekBarPreference mDelay;

    static CharSequence getAutoclickPreferenceSummary(Resources resources, int i) {
        return resources.getQuantityString(mAutoclickPreferenceSummaries[getAutoclickPreferenceSummaryIndex(i)], i, Integer.valueOf(i));
    }

    private static int getAutoclickPreferenceSummaryIndex(int i) {
        if (i <= 200) {
            return 0;
        }
        if (i >= 1000) {
            return mAutoclickPreferenceSummaries.length - 1;
        }
        return (i - 200) / (800 / (mAutoclickPreferenceSummaries.length - 1));
    }

    @Override
    protected void onPreferenceToggled(String str, boolean z) {
        Settings.Secure.putInt(getContentResolver(), str, z ? 1 : 0);
        this.mDelay.setEnabled(z);
    }

    @Override
    public int getMetricsCategory() {
        return 335;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_autoclick;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_autoclick_settings;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int i = Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_delay", 600);
        this.mDelay = (SeekBarPreference) findPreference("autoclick_delay");
        this.mDelay.setMax(delayToSeekBarProgress(1000));
        this.mDelay.setProgress(delayToSeekBarProgress(i));
        this.mDelay.setOnPreferenceChangeListener(this);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.accessibility_autoclick_description);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        int i = Settings.Secure.getInt(getContentResolver(), "accessibility_autoclick_enabled", 0);
        this.mSwitchBar.setCheckedInternal(i == 1);
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mDelay.setEnabled(i == 1);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch r1, boolean z) {
        onPreferenceToggled("accessibility_autoclick_enabled", z);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mDelay && (obj instanceof Integer)) {
            Settings.Secure.putInt(getContentResolver(), "accessibility_autoclick_delay", seekBarProgressToDelay(((Integer) obj).intValue()));
            return true;
        }
        return false;
    }

    private int seekBarProgressToDelay(int i) {
        return (i * 100) + 200;
    }

    private int delayToSeekBarProgress(int i) {
        return (i - 200) / 100;
    }
}
