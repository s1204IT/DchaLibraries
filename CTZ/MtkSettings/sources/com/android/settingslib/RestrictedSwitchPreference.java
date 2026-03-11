package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedSwitchPreference extends SwitchPreference {
    RestrictedPreferenceHelper mHelper;
    CharSequence mRestrictedSwitchSummary;
    boolean mUseAdditionalSummary;

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mUseAdditionalSummary = false;
        setWidgetLayoutResource(R.layout.restricted_switch_widget);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attributeSet);
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RestrictedSwitchPreference);
            TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(R.styleable.RestrictedSwitchPreference_useAdditionalSummary);
            if (typedValuePeekValue != null) {
                this.mUseAdditionalSummary = typedValuePeekValue.type == 18 && typedValuePeekValue.data != 0;
            }
            TypedValue typedValuePeekValue2 = typedArrayObtainStyledAttributes.peekValue(R.styleable.RestrictedSwitchPreference_restrictedSwitchSummary);
            if (typedValuePeekValue2 != null && typedValuePeekValue2.type == 3) {
                if (typedValuePeekValue2.resourceId != 0) {
                    this.mRestrictedSwitchSummary = context.getText(typedValuePeekValue2.resourceId);
                } else {
                    this.mRestrictedSwitchSummary = typedValuePeekValue2.string;
                }
            }
        }
        if (this.mUseAdditionalSummary) {
            setLayoutResource(R.layout.restricted_switch_preference);
            useAdminDisabledSummary(false);
        }
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.switchPreferenceStyle, android.R.attr.switchPreferenceStyle));
    }

    public RestrictedSwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        CharSequence text;
        super.onBindViewHolder(preferenceViewHolder);
        this.mHelper.onBindViewHolder(preferenceViewHolder);
        if (this.mRestrictedSwitchSummary == null) {
            text = getContext().getText(isChecked() ? R.string.enabled_by_admin : R.string.disabled_by_admin);
        } else {
            text = this.mRestrictedSwitchSummary;
        }
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.restricted_icon);
        View viewFindViewById2 = preferenceViewHolder.findViewById(android.R.id.switch_widget);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(isDisabledByAdmin() ? 8 : 0);
        }
        if (this.mUseAdditionalSummary) {
            TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.additional_summary);
            if (textView != null) {
                if (isDisabledByAdmin()) {
                    textView.setText(text);
                    textView.setVisibility(0);
                    return;
                } else {
                    textView.setVisibility(8);
                    return;
                }
            }
            return;
        }
        TextView textView2 = (TextView) preferenceViewHolder.findViewById(android.R.id.summary);
        if (textView2 != null && isDisabledByAdmin()) {
            textView2.setText(text);
            textView2.setVisibility(0);
        }
    }

    @Override
    public void performClick() {
        if (!this.mHelper.performClick()) {
            super.performClick();
        }
    }

    public void useAdminDisabledSummary(boolean z) {
        this.mHelper.useAdminDisabledSummary(z);
    }

    @Override
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

    @Override
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
