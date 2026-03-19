package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

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
    public void performClick() {
        if (!this.mHelper.performClick()) {
            super.performClick();
        }
    }

    public void useAdminDisabledSummary(boolean z) {
        this.mHelper.useAdminDisabledSummary(z);
    }
}
