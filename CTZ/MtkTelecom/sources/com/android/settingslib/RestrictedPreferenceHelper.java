package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedPreferenceHelper {
    private String mAttrUserRestriction;
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private final Preference mPreference;
    private boolean mUseAdminDisabledSummary;

    public RestrictedPreferenceHelper(Context context, Preference preference, AttributeSet attributeSet) {
        CharSequence text;
        this.mAttrUserRestriction = null;
        boolean z = false;
        this.mUseAdminDisabledSummary = false;
        this.mContext = context;
        this.mPreference = preference;
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RestrictedPreference);
            TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(R.styleable.RestrictedPreference_userRestriction);
            if (typedValuePeekValue != null && typedValuePeekValue.type == 3) {
                if (typedValuePeekValue.resourceId != 0) {
                    text = context.getText(typedValuePeekValue.resourceId);
                } else {
                    text = typedValuePeekValue.string;
                }
            } else {
                text = null;
            }
            this.mAttrUserRestriction = text == null ? null : text.toString();
            if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, this.mAttrUserRestriction, UserHandle.myUserId())) {
                this.mAttrUserRestriction = null;
                return;
            }
            TypedValue typedValuePeekValue2 = typedArrayObtainStyledAttributes.peekValue(R.styleable.RestrictedPreference_useAdminDisabledSummary);
            if (typedValuePeekValue2 != null) {
                if (typedValuePeekValue2.type == 18 && typedValuePeekValue2.data != 0) {
                    z = true;
                }
                this.mUseAdminDisabledSummary = z;
            }
        }
    }

    public void useAdminDisabledSummary(boolean z) {
        this.mUseAdminDisabledSummary = z;
    }

    public boolean performClick() {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
            return true;
        }
        return false;
    }
}
