package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NightDisplayFooterPreferenceController extends BasePreferenceController {
    public NightDisplayFooterPreferenceController(Context context) {
        super(context, "footer_preference");
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayController.isAvailable(this.mContext) ? 0 : 2;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setTitle(R.string.night_display_text);
    }
}
