package com.android.settingslib;

import android.content.Context;
import android.os.UserHandle;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class RestrictedPreference extends TwoTargetPreference {
    RestrictedPreferenceHelper mHelper;

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attributeSet);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle, android.R.attr.preferenceStyle));
    }

    public RestrictedPreference(Context context) {
        this(context, null);
    }

    @Override // com.android.settingslib.TwoTargetPreference
    protected int getSecondTargetResId() {
        return R.layout.restricted_icon;
    }

    @Override // com.android.settingslib.TwoTargetPreference
    protected boolean shouldHideSecondTarget() {
        return !isDisabledByAdmin();
    }

    @Override // com.android.settingslib.TwoTargetPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mHelper.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.restricted_icon);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void performClick() {
        if (!this.mHelper.performClick()) {
            super.performClick();
        }
    }

    public void useAdminDisabledSummary(boolean z) {
        this.mHelper.useAdminDisabledSummary(z);
    }

    @Override // android.support.v7.preference.Preference
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    public void checkRestrictionAndSetDisabled(String str) {
        this.mHelper.checkRestrictionAndSetDisabled(str, UserHandle.myUserId());
    }

    public void checkRestrictionAndSetDisabled(String str, int i) {
        this.mHelper.checkRestrictionAndSetDisabled(str, i);
    }

    @Override // android.support.v7.preference.Preference
    public void setEnabled(boolean z) {
        if (z && isDisabledByAdmin()) {
            this.mHelper.setDisabledByAdmin(null);
        } else {
            super.setEnabled(z);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (this.mHelper.setDisabledByAdmin(enforcedAdmin)) {
            notifyChanged();
        }
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }
}
