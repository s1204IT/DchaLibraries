package com.android.settingslib;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

public class TwoTargetPreference extends Preference {
    private int mMediumIconSize;
    private int mSmallIconSize;

    public TwoTargetPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        init(context);
    }

    public TwoTargetPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    private void init(Context context) {
        setLayoutResource(R.layout.preference_two_target);
        this.mSmallIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_small_icon_size);
        this.mMediumIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_medium_icon_size);
        int secondTargetResId = getSecondTargetResId();
        if (secondTargetResId != 0) {
            setWidgetLayoutResource(secondTargetResId);
        }
    }

    protected int getSecondTargetResId() {
        return 0;
    }
}
