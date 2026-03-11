package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;

public class AccessiblePreferenceCategory extends PreferenceCategory {
    private String mContentDescription;

    public AccessiblePreferenceCategory(Context context) {
        super(context);
    }

    public void setContentDescription(String str) {
        this.mContentDescription = str;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.itemView.setContentDescription(this.mContentDescription);
    }
}
