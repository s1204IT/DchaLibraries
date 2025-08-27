package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class NightDisplayCustomStartTimePreferenceController extends BasePreferenceController {
    private ColorDisplayController mController;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayCustomStartTimePreferenceController(Context context, String str) {
        super(context, str);
        this.mController = new ColorDisplayController(context);
        this.mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return ColorDisplayController.isAvailable(this.mContext) ? 0 : 2;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public final void updateState(Preference preference) {
        preference.setVisible(this.mController.getAutoMode() == 1);
        preference.setSummary(this.mTimeFormatter.getFormattedTimeString(this.mController.getCustomStartTime()));
    }
}
