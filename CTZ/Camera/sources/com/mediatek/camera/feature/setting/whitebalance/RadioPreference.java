package com.mediatek.camera.feature.setting.whitebalance;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import com.mediatek.camera.R;

public class RadioPreference extends CheckBoxPreference {
    public RadioPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.white_balance_preference_widget);
        setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                return ((Boolean) obj).booleanValue();
            }
        });
    }
}
