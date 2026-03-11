package com.android.settings.deviceinfo;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;

public class StorageSummaryPreference extends Preference {
    private int mPercent;

    public StorageSummaryPreference(Context context) {
        super(context);
        this.mPercent = -1;
        setLayoutResource(R.layout.storage_summary);
        setEnabled(false);
    }

    public void setPercent(int percent) {
        this.mPercent = percent;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (this.mPercent != -1) {
            progress.setVisibility(0);
            progress.setProgress(this.mPercent);
        } else {
            progress.setVisibility(8);
        }
        TextView summary = (TextView) view.findViewById(android.R.id.summary);
        summary.setTextColor(Color.parseColor("#8a000000"));
        super.onBindViewHolder(view);
    }
}
