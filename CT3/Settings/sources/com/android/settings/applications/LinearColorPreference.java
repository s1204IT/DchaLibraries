package com.android.settings.applications;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import com.android.settings.R;
import com.android.settings.applications.LinearColorBar;

public class LinearColorPreference extends Preference {
    int mColoredRegions;
    int mGreenColor;
    float mGreenRatio;
    LinearColorBar.OnRegionTappedListener mOnRegionTappedListener;
    int mRedColor;
    float mRedRatio;
    int mYellowColor;
    float mYellowRatio;

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        LinearColorBar colors = (LinearColorBar) view.findViewById(R.id.linear_color_bar);
        colors.setShowIndicator(false);
        colors.setColors(this.mRedColor, this.mYellowColor, this.mGreenColor);
        colors.setRatios(this.mRedRatio, this.mYellowRatio, this.mGreenRatio);
        colors.setColoredRegions(this.mColoredRegions);
        colors.setOnRegionTappedListener(this.mOnRegionTappedListener);
    }
}
