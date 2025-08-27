package com.android.settings.widget;

import android.content.Context;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.R;
import com.android.settingslib.RestrictedPreferenceHelper;

/* loaded from: classes.dex */
public class RestrictedAppPreference extends AppPreference {
    private RestrictedPreferenceHelper mHelper;
    private String userRestriction;

    public RestrictedAppPreference(Context context, String str) {
        super(context);
        initialize(null, str);
    }

    private void initialize(AttributeSet attributeSet, String str) {
        setWidgetLayoutResource(R.layout.restricted_icon);
        this.mHelper = new RestrictedPreferenceHelper(getContext(), this, attributeSet);
        this.userRestriction = str;
    }

    @Override // com.android.settings.widget.AppPreference, android.support.v7.preference.Preference
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

    @Override // android.support.v7.preference.Preference
    public void setEnabled(boolean z) {
        if (isDisabledByAdmin() && z) {
            return;
        }
        super.setEnabled(z);
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }

    @Override // android.support.v7.preference.Preference
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    public void checkRestrictionAndSetDisabled() {
        if (TextUtils.isEmpty(this.userRestriction)) {
            return;
        }
        this.mHelper.checkRestrictionAndSetDisabled(this.userRestriction, UserHandle.myUserId());
    }
}
