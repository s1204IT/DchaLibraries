package com.android.server.telecom;

import android.content.Context;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public class SystemSettingsUtil {
    public boolean isTheaterModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "theater_mode_on", 0) == 1;
    }

    public boolean canVibrateWhenRinging(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "vibrate_when_ringing", 0) != 0;
    }
}
