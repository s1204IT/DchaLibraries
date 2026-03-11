package com.android.settingslib;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedPreferenceHelper {
    private String mAttrUserRestriction;
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private final Preference mPreference;
    private boolean mUseAdminDisabledSummary;

    public RestrictedPreferenceHelper(Context context, Preference preference, AttributeSet attrs) {
        this.mAttrUserRestriction = null;
        this.mUseAdminDisabledSummary = false;
        this.mContext = context;
        this.mPreference = preference;
        if (attrs == null) {
            return;
        }
        TypedArray attributes = context.obtainStyledAttributes(attrs, R$styleable.RestrictedPreference);
        TypedValue userRestriction = attributes.peekValue(R$styleable.RestrictedPreference_userRestriction);
        CharSequence data = null;
        if (userRestriction != null && userRestriction.type == 3) {
            data = userRestriction.resourceId != 0 ? context.getText(userRestriction.resourceId) : userRestriction.string;
        }
        this.mAttrUserRestriction = data == null ? null : data.toString();
        if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, this.mAttrUserRestriction, UserHandle.myUserId())) {
            this.mAttrUserRestriction = null;
            return;
        }
        TypedValue useAdminDisabledSummary = attributes.peekValue(R$styleable.RestrictedPreference_useAdminDisabledSummary);
        if (useAdminDisabledSummary == null) {
            return;
        }
        boolean z = useAdminDisabledSummary.type == 18 && useAdminDisabledSummary.data != 0;
        this.mUseAdminDisabledSummary = z;
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        TextView summaryView;
        if (this.mDisabledByAdmin) {
            holder.itemView.setEnabled(true);
        }
        if (!this.mUseAdminDisabledSummary || (summaryView = (TextView) holder.findViewById(R.id.summary)) == null) {
            return;
        }
        if (this.mDisabledByAdmin) {
            summaryView.setText(R$string.disabled_by_admin_summary_text);
            summaryView.setVisibility(0);
        } else {
            summaryView.setVisibility(8);
        }
    }

    public void useAdminDisabledSummary(boolean useSummary) {
        this.mUseAdminDisabledSummary = useSummary;
    }

    public boolean performClick() {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
            return true;
        }
        return false;
    }

    public void onAttachedToHierarchy() {
        if (this.mAttrUserRestriction == null) {
            return;
        }
        checkRestrictionAndSetDisabled(this.mAttrUserRestriction, UserHandle.myUserId());
    }

    public void checkRestrictionAndSetDisabled(String userRestriction, int userId) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, userRestriction, userId);
        setDisabledByAdmin(admin);
    }

    public boolean setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        boolean disabled = admin != null;
        this.mEnforcedAdmin = admin;
        boolean changed = false;
        if (this.mDisabledByAdmin != disabled) {
            this.mDisabledByAdmin = disabled;
            changed = true;
        }
        this.mPreference.setEnabled(!disabled);
        return changed;
    }

    public boolean isDisabledByAdmin() {
        return this.mDisabledByAdmin;
    }
}
