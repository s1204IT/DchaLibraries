package com.mediatek.settingslib;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.R$attr;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.R$id;
import com.android.settingslib.R$layout;
import com.android.settingslib.R$string;
import com.android.settingslib.R$styleable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;

public class RestrictedListPreference extends ListPreference {
    RestrictedPreferenceHelper mHelper;
    String mRestrictedSwitchSummary;
    boolean mUseAdditionalSummary;

    public RestrictedListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mUseAdditionalSummary = false;
        this.mRestrictedSwitchSummary = null;
        setWidgetLayoutResource(R$layout.restricted_icon);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R$styleable.RestrictedSwitchPreference);
            TypedValue useAdditionalSummary = attributes.peekValue(R$styleable.RestrictedSwitchPreference_useAdditionalSummary);
            if (useAdditionalSummary != null) {
                boolean z = useAdditionalSummary.type == 18 && useAdditionalSummary.data != 0;
                this.mUseAdditionalSummary = z;
            }
            TypedValue restrictedSwitchSummary = attributes.peekValue(R$styleable.RestrictedSwitchPreference_restrictedSwitchSummary);
            CharSequence data = null;
            if (restrictedSwitchSummary != null && restrictedSwitchSummary.type == 3) {
                data = restrictedSwitchSummary.resourceId != 0 ? context.getString(restrictedSwitchSummary.resourceId) : restrictedSwitchSummary.string;
            }
            this.mRestrictedSwitchSummary = data == null ? null : data.toString();
        }
        if (this.mRestrictedSwitchSummary == null) {
            this.mRestrictedSwitchSummary = context.getString(R$string.disabled_by_admin);
        }
        if (!this.mUseAdditionalSummary) {
            return;
        }
        setLayoutResource(R$layout.restricted_switch_preference);
        useAdminDisabledSummary(false);
    }

    public RestrictedListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R$attr.preferenceStyle, R.attr.preferenceStyle));
    }

    public RestrictedListPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        this.mHelper.onBindViewHolder(holder);
        View restrictedIcon = holder.findViewById(R$id.restricted_icon);
        if (restrictedIcon != null) {
            restrictedIcon.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
        if (this.mUseAdditionalSummary) {
            TextView additionalSummaryView = (TextView) holder.findViewById(R$id.additional_summary);
            if (additionalSummaryView == null) {
                return;
            }
            if (isDisabledByAdmin()) {
                additionalSummaryView.setText(this.mRestrictedSwitchSummary);
                additionalSummaryView.setVisibility(0);
                return;
            } else {
                additionalSummaryView.setVisibility(8);
                return;
            }
        }
        TextView summaryView = (TextView) holder.findViewById(R.id.summary);
        if (summaryView == null || !isDisabledByAdmin()) {
            return;
        }
        summaryView.setText(this.mRestrictedSwitchSummary);
        summaryView.setVisibility(0);
    }

    @Override
    public void performClick() {
        if (this.mHelper.performClick()) {
            return;
        }
        super.performClick();
    }

    public void useAdminDisabledSummary(boolean useSummary) {
        this.mHelper.useAdminDisabledSummary(useSummary);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && isDisabledByAdmin()) {
            this.mHelper.setDisabledByAdmin(null);
        } else {
            super.setEnabled(enabled);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        if (!this.mHelper.setDisabledByAdmin(admin)) {
            return;
        }
        notifyChanged();
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }
}
