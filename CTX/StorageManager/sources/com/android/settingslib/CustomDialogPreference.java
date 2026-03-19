package com.android.settingslib;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

public class CustomDialogPreference extends DialogPreference {
    public CustomDialogPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public CustomDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}
