package com.android.settings.accessibility;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;

/* loaded from: classes.dex */
public class DividerAllowedBelowPreference extends Preference {
    public DividerAllowedBelowPreference(Context context) {
        super(context);
    }

    public DividerAllowedBelowPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DividerAllowedBelowPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.setDividerAllowedBelow(true);
    }
}
