package com.android.phone;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedSwitchPreference extends SwitchPreference {
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private final int mSwitchWidgetResId;

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mSwitchWidgetResId = getWidgetLayoutResource();
        this.mContext = context;
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.switchPreferenceStyle);
    }

    public RestrictedSwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        if (this.mDisabledByAdmin) {
            view.setEnabled(true);
        }
        TextView textView = (TextView) view.findViewById(android.R.id.summary);
        if (textView != null && this.mDisabledByAdmin) {
            textView.setText(isChecked() ? R.string.enabled_by_admin : R.string.disabled_by_admin);
            textView.setVisibility(0);
        }
    }

    public void checkRestrictionAndSetDisabled(String str) {
        UserManager userManager = UserManager.get(this.mContext);
        UserHandle userHandleOf = UserHandle.of(userManager.getUserHandle());
        setDisabledByAdmin(userManager.hasUserRestriction(str, userHandleOf) && !userManager.hasBaseUserRestriction(str, userHandleOf));
    }

    @Override
    public void setEnabled(boolean z) {
        if (z && this.mDisabledByAdmin) {
            setDisabledByAdmin(false);
        } else {
            super.setEnabled(z);
        }
    }

    public void setDisabledByAdmin(boolean z) {
        if (this.mDisabledByAdmin != z) {
            this.mDisabledByAdmin = z;
            setWidgetLayoutResource(z ? R.layout.restricted_icon : this.mSwitchWidgetResId);
            setEnabled(!z);
        }
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, new RestrictedLockUtils.EnforcedAdmin());
        } else {
            super.performClick(preferenceScreen);
        }
    }
}
