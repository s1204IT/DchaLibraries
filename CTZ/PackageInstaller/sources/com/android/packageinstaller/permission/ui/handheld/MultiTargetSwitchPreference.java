package com.android.packageinstaller.permission.ui.handheld;

import android.R;
import android.content.Context;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Switch;

class MultiTargetSwitchPreference extends SwitchPreference {
    private View.OnClickListener mSwitchOnClickLister;

    public MultiTargetSwitchPreference(Context context) {
        super(context);
    }

    public void setCheckedOverride(boolean z) {
        super.setChecked(z);
    }

    @Override
    public void setChecked(boolean z) {
        if (this.mSwitchOnClickLister == null) {
            super.setChecked(z);
        }
    }

    public void setSwitchOnClickListener(View.OnClickListener onClickListener) {
        this.mSwitchOnClickLister = onClickListener;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch r0 = (Switch) view.findViewById(R.id.switch_widget);
        if (this.mSwitchOnClickLister != null) {
            r0.setOnClickListener(this.mSwitchOnClickLister);
            int measuredHeight = (int) (((view.getMeasuredHeight() - r0.getMeasuredHeight()) / 2) + 0.5f);
            r0.setPadding(measuredHeight, measuredHeight, 0, measuredHeight);
        }
    }
}
