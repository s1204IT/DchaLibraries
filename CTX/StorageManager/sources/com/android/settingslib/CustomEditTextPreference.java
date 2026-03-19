package com.android.settingslib;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

public class CustomEditTextPreference extends EditTextPreference {
    public CustomEditTextPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public CustomEditTextPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}
