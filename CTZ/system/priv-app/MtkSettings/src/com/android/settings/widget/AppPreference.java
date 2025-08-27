package com.android.settings.widget;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import com.android.settings.R;

/* loaded from: classes.dex */
public class AppPreference extends Preference {
    private int mProgress;
    private boolean mProgressVisible;

    public AppPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }

    public AppPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.preference_app);
    }

    public void setProgress(int i) {
        this.mProgress = i;
        this.mProgressVisible = true;
        notifyChanged();
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.findViewById(R.id.summary_container).setVisibility(TextUtils.isEmpty(getSummary()) ? 8 : 0);
        ProgressBar progressBar = (ProgressBar) preferenceViewHolder.findViewById(android.R.id.progress);
        if (this.mProgressVisible) {
            progressBar.setProgress(this.mProgress);
            progressBar.setVisibility(0);
        } else {
            progressBar.setVisibility(8);
        }
    }
}
