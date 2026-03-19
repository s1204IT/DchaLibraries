package com.mediatek.camera.feature.setting.iso;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import com.mediatek.camera.R;

public class RadioPreference extends CheckBoxPreference {
    public RadioPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.iso_radio_preference_widget);
        setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                return ((Boolean) obj).booleanValue();
            }
        });
    }
}
