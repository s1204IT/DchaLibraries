package com.android.settings.accessibility;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

public class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private ListPreference mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.accessibility_daltonizer_settings);
        this.mType = (ListPreference) findPreference("type");
        initPreferences();
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer_enabled", enabled ? 1 : 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mType) {
            Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer", Integer.parseInt((String) newValue));
            preference.setSummary("%s");
            return true;
        }
        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getString(R.string.accessibility_display_daltonizer_preference_title));
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mSwitchBar.setCheckedInternal(Settings.Secure.getInt(getContentResolver(), "accessibility_display_daltonizer_enabled", 0) == 1);
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    private void initPreferences() {
        String value = Integer.toString(Settings.Secure.getInt(getContentResolver(), "accessibility_display_daltonizer", 12));
        this.mType.setValue(value);
        this.mType.setOnPreferenceChangeListener(this);
        int index = this.mType.findIndexOfValue(value);
        if (index < 0) {
            this.mType.setSummary(getString(R.string.daltonizer_type_overridden, new Object[]{getString(R.string.simulate_color_space)}));
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(this.mPreferenceKey, isChecked);
    }
}
