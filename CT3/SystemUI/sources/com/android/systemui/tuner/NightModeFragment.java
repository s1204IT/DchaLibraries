package com.android.systemui.tuner;

import android.app.UiModeManager;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NightModeController;
import com.android.systemui.tuner.TunerService;

public class NightModeFragment extends PreferenceFragment implements TunerService.Tunable, NightModeController.Listener, Preference.OnPreferenceChangeListener {
    private SwitchPreference mAdjustBrightness;
    private SwitchPreference mAdjustTint;
    private SwitchPreference mAutoSwitch;
    private NightModeController mNightModeController;
    private Switch mSwitch;
    private UiModeManager mUiModeManager;
    private static final CharSequence KEY_AUTO = "auto";
    private static final CharSequence KEY_ADJUST_TINT = "adjust_tint";
    private static final CharSequence KEY_ADJUST_BRIGHTNESS = "adjust_brightness";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mNightModeController = new NightModeController(getContext());
        this.mUiModeManager = (UiModeManager) getContext().getSystemService(UiModeManager.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.night_mode_settings, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().getContext();
        addPreferencesFromResource(R.xml.night_mode);
        this.mAutoSwitch = (SwitchPreference) findPreference(KEY_AUTO);
        this.mAutoSwitch.setOnPreferenceChangeListener(this);
        this.mAdjustTint = (SwitchPreference) findPreference(KEY_ADJUST_TINT);
        this.mAdjustTint.setOnPreferenceChangeListener(this);
        this.mAdjustBrightness = (SwitchPreference) findPreference(KEY_ADJUST_BRIGHTNESS);
        this.mAdjustBrightness.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View switchBar = view.findViewById(R.id.switch_bar);
        this.mSwitch = (Switch) switchBar.findViewById(android.R.id.switch_widget);
        this.mSwitch.setChecked(this.mNightModeController.isEnabled());
        switchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !NightModeFragment.this.mNightModeController.isEnabled();
                MetricsLogger.action(NightModeFragment.this.getContext(), 309, newState);
                NightModeFragment.this.mNightModeController.setNightMode(newState);
                NightModeFragment.this.mSwitch.setChecked(newState);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visibility(getContext(), 308, true);
        this.mNightModeController.addListener(this);
        TunerService.get(getContext()).addTunable(this, "brightness_use_twilight", "tuner_night_mode_adjust_tint");
        calculateDisabled();
    }

    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), 308, false);
        this.mNightModeController.removeListener(this);
        TunerService.get(getContext()).removeTunable(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean value = (Boolean) newValue;
        if (this.mAutoSwitch == preference) {
            MetricsLogger.action(getContext(), 310, value.booleanValue());
            this.mNightModeController.setAuto(value.booleanValue());
        } else if (this.mAdjustTint == preference) {
            MetricsLogger.action(getContext(), 312, value.booleanValue());
            this.mNightModeController.setAdjustTint(value);
            postCalculateDisabled();
        } else {
            if (this.mAdjustBrightness != preference) {
                return false;
            }
            MetricsLogger.action(getContext(), 313, value.booleanValue());
            TunerService.get(getContext()).setValue("brightness_use_twilight", value.booleanValue() ? 1 : 0);
            postCalculateDisabled();
        }
        return true;
    }

    private void postCalculateDisabled() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                NightModeFragment.this.calculateDisabled();
            }
        });
    }

    public void calculateDisabled() {
        int enabledCount = (this.mAdjustTint.isChecked() ? 1 : 0) + (this.mAdjustBrightness.isChecked() ? 1 : 0);
        if (enabledCount == 1) {
            if (this.mAdjustTint.isChecked()) {
                this.mAdjustTint.setEnabled(false);
                return;
            } else {
                this.mAdjustBrightness.setEnabled(false);
                return;
            }
        }
        this.mAdjustTint.setEnabled(true);
        this.mAdjustBrightness.setEnabled(true);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if ("brightness_use_twilight".equals(key)) {
            this.mAdjustBrightness.setChecked((newValue == null || Integer.parseInt(newValue) == 0) ? false : true);
            return;
        }
        if (!"tuner_night_mode_adjust_tint".equals(key)) {
            return;
        }
        SwitchPreference switchPreference = this.mAdjustTint;
        if (newValue != null && Integer.parseInt(newValue) == 0) {
            z = false;
        }
        switchPreference.setChecked(z);
    }

    @Override
    public void onNightModeChanged() {
        this.mSwitch.setChecked(this.mNightModeController.isEnabled());
    }
}
