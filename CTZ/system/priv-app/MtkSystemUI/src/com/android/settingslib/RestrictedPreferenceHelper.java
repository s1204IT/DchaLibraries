package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;
/* loaded from: classes.dex */
public class RestrictedPreferenceHelper {
    private String mAttrUserRestriction;
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private final Preference mPreference;
    private boolean mUseAdminDisabledSummary;

    public RestrictedPreferenceHelper(Context context, Preference preference, AttributeSet attributeSet) {
        CharSequence charSequence;
        this.mAttrUserRestriction = null;
        boolean z = false;
        this.mUseAdminDisabledSummary = false;
        this.mContext = context;
        this.mPreference = preference;
        if (attributeSet != null) {
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RestrictedPreference);
            TypedValue peekValue = obtainStyledAttributes.peekValue(R.styleable.RestrictedPreference_userRestriction);
            if (peekValue != null && peekValue.type == 3) {
                if (peekValue.resourceId != 0) {
                    charSequence = context.getText(peekValue.resourceId);
                } else {
                    charSequence = peekValue.string;
                }
            } else {
                charSequence = null;
            }
            this.mAttrUserRestriction = charSequence == null ? null : charSequence.toString();
            if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, this.mAttrUserRestriction, UserHandle.myUserId())) {
                this.mAttrUserRestriction = null;
                return;
            }
            TypedValue peekValue2 = obtainStyledAttributes.peekValue(R.styleable.RestrictedPreference_useAdminDisabledSummary);
            if (peekValue2 != null) {
                if (peekValue2.type == 18 && peekValue2.data != 0) {
                    z = true;
                }
                this.mUseAdminDisabledSummary = z;
            }
        }
    }

    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        TextView textView;
        if (this.mDisabledByAdmin) {
            preferenceViewHolder.itemView.setEnabled(true);
        }
        if (this.mUseAdminDisabledSummary && (textView = (TextView) preferenceViewHolder.findViewById(16908304)) != null) {
            CharSequence text = textView.getContext().getText(R.string.disabled_by_admin_summary_text);
            if (this.mDisabledByAdmin) {
                textView.setText(text);
            } else if (TextUtils.equals(text, textView.getText())) {
                textView.setText((CharSequence) null);
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

    public void onAttachedToHierarchy() {
        if (this.mAttrUserRestriction != null) {
            checkRestrictionAndSetDisabled(this.mAttrUserRestriction, UserHandle.myUserId());
        }
    }

    public void checkRestrictionAndSetDisabled(String str, int i) {
        setDisabledByAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, str, i));
    }

    public boolean setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        boolean z = false;
        boolean z2 = enforcedAdmin != null;
        this.mEnforcedAdmin = enforcedAdmin;
        if (this.mDisabledByAdmin != z2) {
            this.mDisabledByAdmin = z2;
            z = true;
        }
        this.mPreference.setEnabled(true ^ z2);
        return z;
    }

    public boolean isDisabledByAdmin() {
        return this.mDisabledByAdmin;
    }
}
