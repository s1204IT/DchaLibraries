package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

/* loaded from: classes.dex */
public class NightDisplayIntensityPreferenceController extends SliderPreferenceController {
    private ColorDisplayController mController;

    public NightDisplayIntensityPreferenceController(Context context, String str) {
        super(context, str);
        this.mController = new ColorDisplayController(context);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (!ColorDisplayController.isAvailable(this.mContext)) {
            return 2;
        }
        if (!this.mController.isActivated()) {
            return 4;
        }
        return 0;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "night_display_temperature");
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        SeekBarPreference seekBarPreference = (SeekBarPreference) preferenceScreen.findPreference(getPreferenceKey());
        seekBarPreference.setContinuousUpdates(true);
        seekBarPreference.setMax(getMaxSteps());
    }

    @Override // com.android.settings.core.SliderPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public final void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(this.mController.isActivated());
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public int getSliderPosition() {
        return convertTemperature(this.mController.getColorTemperature());
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public boolean setSliderPosition(int i) {
        return this.mController.setColorTemperature(convertTemperature(i));
    }

    @Override // com.android.settings.core.SliderPreferenceController
    public int getMaxSteps() {
        return convertTemperature(this.mController.getMinimumColorTemperature());
    }

    private int convertTemperature(int i) {
        return this.mController.getMaximumColorTemperature() - i;
    }
}
