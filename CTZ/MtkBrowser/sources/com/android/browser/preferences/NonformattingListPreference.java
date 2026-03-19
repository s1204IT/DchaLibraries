package com.android.browser.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class NonformattingListPreference extends ListPreference {
    private CharSequence mSummary;

    public NonformattingListPreference(Context context) {
        super(context);
    }

    public NonformattingListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setSummary(CharSequence charSequence) {
        this.mSummary = charSequence;
        super.setSummary(charSequence);
    }

    @Override
    public CharSequence getSummary() {
        if (this.mSummary != null) {
            return this.mSummary;
        }
        return super.getSummary();
    }
}
