package com.android.settings.widget;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.util.AttributeSet;
import com.android.settings.R;

public class AppCheckBoxPreference extends CheckBoxPreference {
    public AppCheckBoxPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.preference_app);
    }

    public AppCheckBoxPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }
}
