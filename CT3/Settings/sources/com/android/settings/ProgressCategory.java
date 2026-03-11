package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;

public class ProgressCategory extends ProgressCategoryBase {
    private int mEmptyTextRes;
    private boolean mNoDeviceFoundAdded;
    private Preference mNoDeviceFoundPreference;
    private boolean mProgress;

    public ProgressCategory(Context context) {
        this(context, null);
    }

    public ProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        this.mProgress = false;
    }

    public ProgressCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ProgressCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mProgress = false;
        setLayoutResource(R.layout.preference_progress_category);
    }

    public void setEmptyTextRes(int emptyTextRes) {
        this.mEmptyTextRes = emptyTextRes;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        boolean noDeviceFound;
        super.onBindViewHolder(view);
        View progressBar = view.findViewById(R.id.scanning_progress);
        if (getPreferenceCount() == 0) {
            noDeviceFound = true;
        } else {
            noDeviceFound = getPreferenceCount() == 1 && getPreference(0) == this.mNoDeviceFoundPreference;
        }
        progressBar.setVisibility(this.mProgress ? 0 : 8);
        if (this.mProgress || !noDeviceFound) {
            if (!this.mNoDeviceFoundAdded) {
                return;
            }
            removePreference(this.mNoDeviceFoundPreference);
            this.mNoDeviceFoundAdded = false;
            return;
        }
        if (this.mNoDeviceFoundAdded) {
            return;
        }
        if (this.mNoDeviceFoundPreference == null) {
            this.mNoDeviceFoundPreference = new Preference(getPreferenceManager().getContext());
            this.mNoDeviceFoundPreference.setLayoutResource(R.layout.preference_empty_list);
            this.mNoDeviceFoundPreference.setTitle(this.mEmptyTextRes);
            this.mNoDeviceFoundPreference.setSelectable(false);
        }
        addPreference(this.mNoDeviceFoundPreference);
        this.mNoDeviceFoundAdded = true;
    }

    public void setProgress(boolean progressOn) {
        this.mProgress = progressOn;
        notifyChanged();
    }

    public void setNoDeviceFoundAdded(boolean noDeviceFoundAdded) {
        this.mNoDeviceFoundAdded = noDeviceFoundAdded;
    }
}
