package com.android.settings.widget;

import android.R;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;

/* loaded from: classes.dex */
public class FilterTouchesSwitchPreference extends SwitchPreference {
    public FilterTouchesSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public FilterTouchesSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public FilterTouchesSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public FilterTouchesSwitchPreference(Context context) {
        super(context);
    }

    @Override // android.support.v14.preference.SwitchPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.switch_widget);
        if (viewFindViewById != null) {
            viewFindViewById.getRootView().setFilterTouchesWhenObscured(true);
        }
    }
}
