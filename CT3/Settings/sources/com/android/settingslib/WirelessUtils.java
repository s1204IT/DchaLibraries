package com.android.settingslib;

import android.content.Context;
import android.provider.Settings;

public class WirelessUtils {
    public static boolean isRadioAllowed(Context context, String type) {
        if (!isAirplaneModeOn(context)) {
            return true;
        }
        String toggleable = Settings.Global.getString(context.getContentResolver(), "airplane_mode_toggleable_radios");
        if (toggleable != null) {
            return toggleable.contains(type);
        }
        return false;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }
}
