package com.android.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TintablePreference extends Preference {
    private int mTintColor;

    public TintablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTint(int color) {
        this.mTintColor = color;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mTintColor != 0) {
            ((ImageView) view.findViewById(android.R.id.icon)).setImageTintList(ColorStateList.valueOf(this.mTintColor));
        } else {
            ((ImageView) view.findViewById(android.R.id.icon)).setImageTintList(null);
        }
    }
}
