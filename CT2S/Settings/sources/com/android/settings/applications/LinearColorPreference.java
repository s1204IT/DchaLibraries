package com.android.settings.applications;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
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

    public LinearColorPreference(Context context) {
        super(context);
        this.mRedColor = -5615568;
        this.mYellowColor = -5592528;
        this.mGreenColor = -13587888;
        this.mColoredRegions = 7;
        setLayoutResource(R.layout.preference_linearcolor);
    }

    public void setRatios(float red, float yellow, float green) {
        this.mRedRatio = red;
        this.mYellowRatio = yellow;
        this.mGreenRatio = green;
        notifyChanged();
    }

    public void setColors(int red, int yellow, int green) {
        this.mRedColor = red;
        this.mYellowColor = yellow;
        this.mGreenColor = green;
        notifyChanged();
    }

    public void setColoredRegions(int regions) {
        this.mColoredRegions = regions;
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        LinearColorBar colors = (LinearColorBar) view.findViewById(R.id.linear_color_bar);
        colors.setShowIndicator(false);
        colors.setColors(this.mRedColor, this.mYellowColor, this.mGreenColor);
        colors.setRatios(this.mRedRatio, this.mYellowRatio, this.mGreenRatio);
        colors.setColoredRegions(this.mColoredRegions);
        colors.setOnRegionTappedListener(this.mOnRegionTappedListener);
    }
}
