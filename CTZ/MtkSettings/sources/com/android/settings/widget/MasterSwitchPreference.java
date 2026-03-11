package com.android.settings.widget;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.TwoTargetPreference;

public class MasterSwitchPreference extends TwoTargetPreference {
    private boolean mChecked;
    private boolean mEnableSwitch;
    private Switch mSwitch;

    public MasterSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mEnableSwitch = true;
    }

    public MasterSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mEnableSwitch = true;
    }

    public MasterSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnableSwitch = true;
    }

    public MasterSwitchPreference(Context context) {
        super(context);
        this.mEnableSwitch = true;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_master_switch;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.widget_frame);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MasterSwitchPreference.this.mSwitch == null || MasterSwitchPreference.this.mSwitch.isEnabled()) {
                        MasterSwitchPreference.this.setChecked(!MasterSwitchPreference.this.mChecked);
                        if (!MasterSwitchPreference.this.callChangeListener(Boolean.valueOf(MasterSwitchPreference.this.mChecked))) {
                            MasterSwitchPreference.this.setChecked(!MasterSwitchPreference.this.mChecked);
                        } else {
                            MasterSwitchPreference.this.persistBoolean(MasterSwitchPreference.this.mChecked);
                        }
                    }
                }
            });
        }
        this.mSwitch = (Switch) preferenceViewHolder.findViewById(R.id.switchWidget);
        if (this.mSwitch != null) {
            this.mSwitch.setContentDescription(getTitle());
            this.mSwitch.setChecked(this.mChecked);
            this.mSwitch.setEnabled(this.mEnableSwitch);
        }
    }

    public boolean isChecked() {
        return this.mSwitch != null && this.mChecked;
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        if (this.mSwitch != null) {
            this.mSwitch.setChecked(z);
        }
    }

    public void setSwitchEnabled(boolean z) {
        this.mEnableSwitch = z;
        if (this.mSwitch != null) {
            this.mSwitch.setEnabled(z);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        setSwitchEnabled(enforcedAdmin == null);
    }
}
