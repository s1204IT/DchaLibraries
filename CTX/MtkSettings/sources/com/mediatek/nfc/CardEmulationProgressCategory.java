package com.mediatek.nfc;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import com.android.settings.R;

class CardEmulationProgressCategory extends PreferenceCategory {
    private boolean mProgress;

    public CardEmulationProgressCategory(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.findViewById(R.id.scanning_progress).setVisibility(this.mProgress ? 0 : 8);
    }

    public void setProgress(boolean z) {
        this.mProgress = z;
        notifyChanged();
    }
}
