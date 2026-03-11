package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class AppProgressPreference extends TintablePreference {
    private int mProgress;

    public AppProgressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app);
        setWidgetLayoutResource(R.layout.widget_progress_bar);
    }

    public void setProgress(int amount) {
        this.mProgress = amount;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        progress.setProgress(this.mProgress);
    }
}
