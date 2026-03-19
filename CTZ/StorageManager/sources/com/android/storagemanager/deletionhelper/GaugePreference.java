package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import com.android.storagemanager.R;

public class GaugePreference extends Preference {
    public GaugePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.informational_preference);
    }
}
