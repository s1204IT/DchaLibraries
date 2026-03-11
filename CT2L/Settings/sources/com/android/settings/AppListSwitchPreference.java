package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

public class AppListSwitchPreference extends AppListPreference {
    private Checkable mSwitch;

    public AppListSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0, R.style.AppListSwitchPreference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSwitch = (Checkable) view.findViewById(android.R.id.home_panel);
        this.mSwitch.setChecked(getValue() != null);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getValue() != null) {
            if (callChangeListener(null)) {
                setValue(null);
            }
        } else {
            if (getEntryValues() == null || getEntryValues().length == 0) {
                Log.e("AppListSwitchPref", "Attempting to show dialog with zero entries: " + getKey());
                return;
            }
            if (getEntryValues().length == 1) {
                String value = getEntryValues()[0].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                    return;
                }
                return;
            }
            super.showDialog(state);
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (this.mSwitch != null) {
            this.mSwitch.setChecked(value != null);
        }
    }
}
