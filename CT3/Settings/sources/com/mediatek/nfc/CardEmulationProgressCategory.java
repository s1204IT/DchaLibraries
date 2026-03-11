package com.mediatek.nfc;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.R;

class CardEmulationProgressCategory extends PreferenceCategory {
    private boolean mProgress;

    public CardEmulationProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View progressBar = holder.findViewById(R.id.scanning_progress);
        progressBar.setVisibility(this.mProgress ? 0 : 8);
    }

    public void setProgress(boolean progressOn) {
        this.mProgress = progressOn;
        notifyChanged();
    }
}
