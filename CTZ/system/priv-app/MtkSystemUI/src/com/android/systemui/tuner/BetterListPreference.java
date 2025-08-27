package com.android.systemui.tuner;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

/* loaded from: classes.dex */
public class BetterListPreference extends ListPreference {
    private CharSequence mSummary;

    public BetterListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.support.v7.preference.ListPreference, android.support.v7.preference.Preference
    public void setSummary(CharSequence charSequence) {
        super.setSummary(charSequence);
        this.mSummary = charSequence;
    }

    @Override // android.support.v7.preference.ListPreference, android.support.v7.preference.Preference
    public CharSequence getSummary() {
        return this.mSummary;
    }
}
