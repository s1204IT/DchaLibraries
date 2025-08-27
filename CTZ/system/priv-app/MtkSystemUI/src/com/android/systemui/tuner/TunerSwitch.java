package com.android.systemui.tuner;

import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;

/* loaded from: classes.dex */
public class TunerSwitch extends SwitchPreference implements TunerService.Tunable {
    private final int mAction;
    private final boolean mDefault;

    public TunerSwitch(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TunerSwitch);
        this.mDefault = typedArrayObtainStyledAttributes.getBoolean(0, false);
        this.mAction = typedArrayObtainStyledAttributes.getInt(1, -1);
    }

    @Override // android.support.v7.preference.Preference
    public void onAttached() {
        super.onAttached();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, getKey().split(","));
    }

    @Override // android.support.v7.preference.Preference
    public void onDetached() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        super.onDetached();
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        setChecked(str2 != null ? Integer.parseInt(str2) != 0 : this.mDefault);
    }

    @Override // android.support.v7.preference.TwoStatePreference, android.support.v7.preference.Preference
    protected void onClick() {
        super.onClick();
        if (this.mAction != -1) {
            MetricsLogger.action(getContext(), this.mAction, isChecked());
        }
    }

    @Override // android.support.v7.preference.Preference
    protected boolean persistBoolean(boolean z) {
        for (String str : getKey().split(",")) {
            Settings.Secure.putString(getContext().getContentResolver(), str, z ? "1" : "0");
        }
        return true;
    }
}
