package com.android.settingslib.inputmethod;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;

/* loaded from: classes.dex */
public class SwitchWithNoTextPreference extends SwitchPreference {
    public SwitchWithNoTextPreference(Context context) {
        super(context);
        setSwitchTextOn("");
        setSwitchTextOff("");
    }
}
