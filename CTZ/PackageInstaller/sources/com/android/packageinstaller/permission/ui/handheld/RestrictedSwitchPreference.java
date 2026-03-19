package com.android.packageinstaller.permission.ui.handheld;

import android.R;
import android.content.Context;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedSwitchPreference extends MultiTargetSwitchPreference {
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private final int mSwitchWidgetResId;

    @Override
    public void setChecked(boolean z) {
        super.setChecked(z);
    }

    @Override
    public void setCheckedOverride(boolean z) {
        super.setCheckedOverride(z);
    }

    @Override
    public void setSwitchOnClickListener(View.OnClickListener onClickListener) {
        super.setSwitchOnClickListener(onClickListener);
    }

    public RestrictedSwitchPreference(Context context) {
        super(context);
        this.mSwitchWidgetResId = getWidgetLayoutResource();
        this.mContext = context;
    }

    @Override
    public void onBindView(View view) {
        TextView textView;
        super.onBindView(view);
        if (this.mDisabledByAdmin) {
            view.setEnabled(true);
        }
        if (this.mDisabledByAdmin && (textView = (TextView) view.findViewById(R.id.summary)) != null) {
            textView.setText(isChecked() ? com.android.packageinstaller.R.string.enabled_by_admin : com.android.packageinstaller.R.string.disabled_by_admin);
            textView.setVisibility(0);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        if (z && this.mDisabledByAdmin) {
            setDisabledByAdmin(null);
        } else {
            super.setEnabled(z);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        boolean z;
        if (enforcedAdmin == null) {
            z = false;
        } else {
            z = true;
        }
        this.mEnforcedAdmin = enforcedAdmin;
        if (this.mDisabledByAdmin != z) {
            this.mDisabledByAdmin = z;
            setWidgetLayoutResource(z ? com.android.packageinstaller.R.layout.restricted_icon : this.mSwitchWidgetResId);
            setEnabled(!z);
        }
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
        } else {
            super.performClick(preferenceScreen);
        }
    }
}
