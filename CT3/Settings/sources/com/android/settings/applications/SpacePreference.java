package com.android.settings.applications;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class SpacePreference extends Preference {
    private int mHeight;

    public SpacePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public SpacePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SpacePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(com.android.settings.R.layout.space_preference);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{R.attr.layout_height}, defStyleAttr, defStyleRes);
        this.mHeight = a.getDimensionPixelSize(0, 0);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(-1, this.mHeight);
        view.itemView.setLayoutParams(params);
    }
}
