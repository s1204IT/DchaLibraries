package com.android.systemui.tuner;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class BetterListPreference extends ListPreference {
    private CharSequence mSummary;

    public BetterListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setSummary(CharSequence charSequence) {
        super.setSummary(charSequence);
        this.mSummary = charSequence;
    }

    @Override
    public CharSequence getSummary() {
        return this.mSummary;
    }
}
