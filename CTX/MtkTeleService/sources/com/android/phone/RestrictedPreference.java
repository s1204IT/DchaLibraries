package com.android.phone;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictedPreference extends Preference {
    private final Context mContext;
    private boolean mDisabledByAdmin;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
        setLayoutResource(R.layout.preference_two_target);
        setWidgetLayoutResource(R.layout.restricted_icon);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RestrictedPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.preferenceStyle);
    }

    public RestrictedPreference(Context context) {
        this(context, null);
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (this.mDisabledByAdmin) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin);
        } else {
            super.performClick(preferenceScreen);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View viewFindViewById = view.findViewById(R.id.two_target_divider);
        View viewFindViewById2 = view.findViewById(android.R.id.widget_frame);
        View viewFindViewById3 = view.findViewById(R.id.restricted_icon);
        TextView textView = (TextView) view.findViewById(android.R.id.summary);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(this.mDisabledByAdmin ? 0 : 8);
        }
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(this.mDisabledByAdmin ? 0 : 8);
        }
        if (viewFindViewById3 != null) {
            viewFindViewById3.setVisibility(this.mDisabledByAdmin ? 0 : 8);
        }
        if (textView != null && this.mDisabledByAdmin) {
            textView.setText(R.string.disabled_by_admin_summary_text);
            textView.setVisibility(0);
        }
        if (this.mDisabledByAdmin) {
            view.setEnabled(true);
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
        boolean z = false;
        boolean z2 = enforcedAdmin != null;
        this.mEnforcedAdmin = enforcedAdmin;
        if (this.mDisabledByAdmin != z2) {
            this.mDisabledByAdmin = z2;
            z = true;
        }
        setEnabled(!z2);
        if (z) {
            notifyChanged();
        }
    }
}
