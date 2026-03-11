package com.android.settings.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.widget.SeekBarPreference;

public abstract class SliderPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    public abstract int getMaxSteps();

    public abstract int getSliderPosition();

    public abstract boolean setSliderPosition(int i);

    public SliderPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return setSliderPosition(((Integer) obj).intValue());
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof SeekBarPreference) {
            ((SeekBarPreference) preference).setProgress(getSliderPosition());
        } else if (preference instanceof android.support.v7.preference.SeekBarPreference) {
            ((android.support.v7.preference.SeekBarPreference) preference).setValue(getSliderPosition());
        }
    }

    @Override
    public int getSliceType() {
        return 2;
    }
}
