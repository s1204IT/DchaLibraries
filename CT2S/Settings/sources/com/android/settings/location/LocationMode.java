package com.android.settings.location;

import android.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.location.RadioButtonPreference;

public class LocationMode extends LocationSettingsBase implements RadioButtonPreference.OnClickListener {
    private RadioButtonPreference mBatterySaving;
    private RadioButtonPreference mHighAccuracy;
    private RadioButtonPreference mSensorsOnly;

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_mode);
        PreferenceScreen root2 = getPreferenceScreen();
        this.mHighAccuracy = (RadioButtonPreference) root2.findPreference("high_accuracy");
        this.mBatterySaving = (RadioButtonPreference) root2.findPreference("battery_saving");
        this.mSensorsOnly = (RadioButtonPreference) root2.findPreference("sensors_only");
        this.mHighAccuracy.setOnClickListener(this);
        this.mBatterySaving.setOnClickListener(this);
        this.mSensorsOnly.setOnClickListener(this);
        refreshLocationMode();
        return root2;
    }

    private void updateRadioButtons(RadioButtonPreference activated) {
        if (activated == null) {
            this.mHighAccuracy.setChecked(false);
            this.mBatterySaving.setChecked(false);
            this.mSensorsOnly.setChecked(false);
            return;
        }
        if (activated == this.mHighAccuracy) {
            this.mHighAccuracy.setChecked(true);
            this.mBatterySaving.setChecked(false);
            this.mSensorsOnly.setChecked(false);
        } else if (activated == this.mBatterySaving) {
            this.mHighAccuracy.setChecked(false);
            this.mBatterySaving.setChecked(true);
            this.mSensorsOnly.setChecked(false);
        } else if (activated == this.mSensorsOnly) {
            this.mHighAccuracy.setChecked(false);
            this.mBatterySaving.setChecked(false);
            this.mSensorsOnly.setChecked(true);
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference emiter) {
        int mode = 0;
        if (emiter == this.mHighAccuracy) {
            mode = 3;
        } else if (emiter == this.mBatterySaving) {
            mode = 2;
        } else if (emiter == this.mSensorsOnly) {
            mode = 1;
        }
        setLocationMode(mode);
    }

    @Override
    public void onModeChanged(int mode, boolean restricted) {
        switch (mode) {
            case 0:
                updateRadioButtons(null);
                break;
            case 1:
                updateRadioButtons(this.mSensorsOnly);
                break;
            case 2:
                updateRadioButtons(this.mBatterySaving);
                break;
            case 3:
                updateRadioButtons(this.mHighAccuracy);
                break;
        }
        boolean enabled = (mode == 0 || restricted) ? false : true;
        this.mHighAccuracy.setEnabled(enabled);
        this.mBatterySaving.setEnabled(enabled);
        this.mSensorsOnly.setEnabled(enabled);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }
}
