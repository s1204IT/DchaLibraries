package com.android.browser.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class NonformattingListPreference extends ListPreference {
    private CharSequence mSummary;

    public NonformattingListPreference(Context context) {
        super(context);
    }

    public NonformattingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSummary(CharSequence summary) {
        this.mSummary = summary;
        super.setSummary(summary);
    }

    @Override
    public CharSequence getSummary() {
        return this.mSummary != null ? this.mSummary : super.getSummary();
    }
}
