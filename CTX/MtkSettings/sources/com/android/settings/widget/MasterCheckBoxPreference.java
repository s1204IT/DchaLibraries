package com.android.settings.widget;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;

public class MasterCheckBoxPreference extends TwoTargetPreference {
    private CheckBox mCheckBox;
    private boolean mChecked;
    private boolean mEnableCheckBox;

    public MasterCheckBoxPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mEnableCheckBox = true;
    }

    public MasterCheckBoxPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mEnableCheckBox = true;
    }

    public MasterCheckBoxPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnableCheckBox = true;
    }

    public MasterCheckBoxPreference(Context context) {
        super(context);
        this.mEnableCheckBox = true;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_master_checkbox;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.widget_frame);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MasterCheckBoxPreference.this.mCheckBox == null || MasterCheckBoxPreference.this.mCheckBox.isEnabled()) {
                        MasterCheckBoxPreference.this.setChecked(!MasterCheckBoxPreference.this.mChecked);
                        if (!MasterCheckBoxPreference.this.callChangeListener(Boolean.valueOf(MasterCheckBoxPreference.this.mChecked))) {
                            MasterCheckBoxPreference.this.setChecked(!MasterCheckBoxPreference.this.mChecked);
                        } else {
                            MasterCheckBoxPreference.this.persistBoolean(MasterCheckBoxPreference.this.mChecked);
                        }
                    }
                }
            });
        }
        this.mCheckBox = (CheckBox) preferenceViewHolder.findViewById(R.id.checkboxWidget);
        if (this.mCheckBox != null) {
            this.mCheckBox.setContentDescription(getTitle());
            this.mCheckBox.setChecked(this.mChecked);
            this.mCheckBox.setEnabled(this.mEnableCheckBox);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        setCheckBoxEnabled(z);
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        if (this.mCheckBox != null) {
            this.mCheckBox.setChecked(z);
        }
    }

    public void setCheckBoxEnabled(boolean z) {
        this.mEnableCheckBox = z;
        if (this.mCheckBox != null) {
            this.mCheckBox.setEnabled(z);
        }
    }
}
