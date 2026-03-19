package com.mediatek.camera.common.preference;

import android.content.Context;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;

public class Preference extends android.preference.Preference {
    private CharSequence mContentDescription;
    private int mID;
    private boolean mRemoved;
    private PreferenceScreen mRootPreference;

    public Preference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mID = -1;
        this.mRemoved = false;
    }

    public Preference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mID = -1;
        this.mRemoved = false;
    }

    public Preference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mID = -1;
        this.mRemoved = false;
    }

    public Preference(Context context) {
        super(context);
        this.mID = -1;
        this.mRemoved = false;
    }

    @Override
    protected void onBindView(View view) {
        if (this.mContentDescription != null) {
            view.setContentDescription(this.mContentDescription);
        }
        if (this.mID != -1) {
            view.setId(this.mID);
        }
        super.onBindView(view);
    }

    @Override
    public void setEnabled(boolean z) {
        if (z) {
            this.mRootPreference.addPreference(this);
            this.mRemoved = false;
        } else if (!this.mRemoved) {
            this.mRootPreference.removePreference(this);
            this.mRemoved = true;
        }
    }

    public void setContentDescription(CharSequence charSequence) {
        this.mContentDescription = charSequence;
    }

    public void setId(int i) {
        this.mID = i;
    }

    public void setRootPreference(PreferenceScreen preferenceScreen) {
        this.mRootPreference = preferenceScreen;
    }
}
