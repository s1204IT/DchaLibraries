package com.android.settings.widget;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.view.View;
import com.android.settings.R;

/* loaded from: classes.dex */
public class AppSwitchPreference extends SwitchPreference {
    public AppSwitchPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }

    @Override // android.support.v14.preference.SwitchPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.findViewById(R.id.summary_container).setVisibility(TextUtils.isEmpty(getSummary()) ? 8 : 0);
        View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.switch_widget);
        if (viewFindViewById != null) {
            viewFindViewById.getRootView().setFilterTouchesWhenObscured(true);
        }
    }
}
