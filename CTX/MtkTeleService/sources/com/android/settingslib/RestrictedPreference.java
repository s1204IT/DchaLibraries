package com.android.settingslib;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;

public class RestrictedPreference extends TwoTargetPreference {
    RestrictedPreferenceHelper mHelper;

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attributeSet);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle, android.R.attr.preferenceStyle));
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.restricted_icon;
    }

    @Override
    public void performClick() {
        if (!this.mHelper.performClick()) {
            super.performClick();
        }
    }
}
