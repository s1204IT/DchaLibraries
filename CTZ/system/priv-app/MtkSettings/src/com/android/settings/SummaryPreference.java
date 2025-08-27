package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.TextView;

/* loaded from: classes.dex */
public class SummaryPreference extends Preference {
    private String mAmount;
    private boolean mChartEnabled;
    private String mEndLabel;
    private float mLeftRatio;
    private float mMiddleRatio;
    private float mRightRatio;
    private String mStartLabel;
    private String mUnits;

    public SummaryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChartEnabled = true;
        setLayoutResource(R.layout.settings_summary_preference);
    }

    public void setChartEnabled(boolean z) {
        if (this.mChartEnabled != z) {
            this.mChartEnabled = z;
            notifyChanged();
        }
    }

    public void setAmount(String str) {
        this.mAmount = str;
        if (this.mAmount == null || this.mUnits == null) {
            return;
        }
        setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large), this.mAmount, this.mUnits));
    }

    public void setUnits(String str) {
        this.mUnits = str;
        if (this.mAmount == null || this.mUnits == null) {
            return;
        }
        setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large), this.mAmount, this.mUnits));
    }

    public void setLabels(String str, String str2) {
        this.mStartLabel = str;
        this.mEndLabel = str2;
        notifyChanged();
    }

    public void setRatios(float f, float f2, float f3) {
        this.mLeftRatio = f;
        this.mMiddleRatio = f2;
        this.mRightRatio = f3;
        notifyChanged();
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ProgressBar progressBar = (ProgressBar) preferenceViewHolder.itemView.findViewById(R.id.color_bar);
        if (this.mChartEnabled) {
            progressBar.setVisibility(0);
            int i = (int) (this.mLeftRatio * 100.0f);
            progressBar.setProgress(i);
            progressBar.setSecondaryProgress(i + ((int) (this.mMiddleRatio * 100.0f)));
        } else {
            progressBar.setVisibility(8);
        }
        if (this.mChartEnabled && (!TextUtils.isEmpty(this.mStartLabel) || !TextUtils.isEmpty(this.mEndLabel))) {
            preferenceViewHolder.findViewById(R.id.label_bar).setVisibility(0);
            ((TextView) preferenceViewHolder.findViewById(android.R.id.text1)).setText(this.mStartLabel);
            ((TextView) preferenceViewHolder.findViewById(android.R.id.text2)).setText(this.mEndLabel);
            return;
        }
        preferenceViewHolder.findViewById(R.id.label_bar).setVisibility(8);
    }
}
