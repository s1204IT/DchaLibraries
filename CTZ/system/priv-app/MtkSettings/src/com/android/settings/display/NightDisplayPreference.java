package com.android.settings.display;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import com.android.internal.app.ColorDisplayController;
import java.time.LocalTime;

/* loaded from: classes.dex */
public class NightDisplayPreference extends SwitchPreference implements ColorDisplayController.Callback {
    private ColorDisplayController mController;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mController = new ColorDisplayController(context);
        this.mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override // android.support.v7.preference.Preference
    public void onAttached() {
        super.onAttached();
        this.mController.setListener(this);
        updateSummary();
    }

    @Override // android.support.v7.preference.Preference
    public void onDetached() {
        super.onDetached();
        this.mController.setListener((ColorDisplayController.Callback) null);
    }

    public void onActivated(boolean z) {
        updateSummary();
    }

    public void onAutoModeChanged(int i) {
        updateSummary();
    }

    public void onCustomStartTimeChanged(LocalTime localTime) {
        updateSummary();
    }

    public void onCustomEndTimeChanged(LocalTime localTime) {
        updateSummary();
    }

    private void updateSummary() {
        setSummary(this.mTimeFormatter.getAutoModeTimeSummary(getContext(), this.mController));
    }
}
