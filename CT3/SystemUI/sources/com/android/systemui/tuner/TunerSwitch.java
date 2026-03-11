package com.android.systemui.tuner;

import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;

public class TunerSwitch extends SwitchPreference implements TunerService.Tunable {
    private final int mAction;
    private final boolean mDefault;

    public TunerSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TunerSwitch);
        this.mDefault = a.getBoolean(0, false);
        this.mAction = a.getInt(1, -1);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        TunerService.get(getContext()).addTunable(this, getKey().split(","));
    }

    @Override
    public void onDetached() {
        TunerService.get(getContext()).removeTunable(this);
        super.onDetached();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean z = false;
        if (newValue == null) {
            z = this.mDefault;
        } else if (Integer.parseInt(newValue) != 0) {
            z = true;
        }
        setChecked(z);
    }

    @Override
    protected void onClick() {
        super.onClick();
        if (this.mAction == -1) {
            return;
        }
        MetricsLogger.action(getContext(), this.mAction, isChecked());
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        for (String key : getKey().split(",")) {
            Settings.Secure.putString(getContext().getContentResolver(), key, value ? "1" : "0");
        }
        return true;
    }
}
