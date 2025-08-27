package com.android.settings.widget;

import android.R;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.settingslib.RestrictedSwitchPreference;

/* loaded from: classes.dex */
public class FilterTouchesRestrictedSwitchPreference extends RestrictedSwitchPreference {
    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.RestrictedSwitchPreference, android.support.v14.preference.SwitchPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.switch_widget);
        if (viewFindViewById != null) {
            viewFindViewById.getRootView().setFilterTouchesWhenObscured(true);
        }
    }
}
