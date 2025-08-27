package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;

/* loaded from: classes.dex */
public class ProgressCategory extends ProgressCategoryBase {
    private int mEmptyTextRes;
    private boolean mNoDeviceFoundAdded;
    private Preference mNoDeviceFoundPreference;
    private boolean mProgress;

    public ProgressCategory(Context context) {
        super(context);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    public ProgressCategory(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    public ProgressCategory(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    public ProgressCategory(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    public void setEmptyTextRes(int i) {
        this.mEmptyTextRes = i;
    }

    @Override // android.support.v7.preference.PreferenceCategory, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.scanning_progress);
        boolean z = getPreferenceCount() == 0 || (getPreferenceCount() == 1 && getPreference(0) == this.mNoDeviceFoundPreference);
        viewFindViewById.setVisibility(this.mProgress ? 0 : 8);
        if (this.mProgress || !z) {
            if (this.mNoDeviceFoundAdded) {
                removePreference(this.mNoDeviceFoundPreference);
                this.mNoDeviceFoundAdded = false;
                return;
            }
            return;
        }
        if (!this.mNoDeviceFoundAdded) {
            if (this.mNoDeviceFoundPreference == null) {
                this.mNoDeviceFoundPreference = new Preference(getContext());
                this.mNoDeviceFoundPreference.setLayoutResource(R.layout.preference_empty_list);
                this.mNoDeviceFoundPreference.setTitle(this.mEmptyTextRes);
                this.mNoDeviceFoundPreference.setSelectable(false);
            }
            addPreference(this.mNoDeviceFoundPreference);
            this.mNoDeviceFoundAdded = true;
        }
    }

    public void setProgress(boolean z) {
        this.mProgress = z;
        notifyChanged();
    }

    public void setNoDeviceFoundAdded(boolean z) {
        this.mNoDeviceFoundAdded = z;
    }
}
