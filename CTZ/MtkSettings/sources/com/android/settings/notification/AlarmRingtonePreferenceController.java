package com.android.settings.notification;

import android.content.Context;

public class AlarmRingtonePreferenceController extends RingtonePreferenceControllerBase {
    public AlarmRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "alarm_ringtone";
    }

    @Override
    public int getRingtoneType() {
        return 4;
    }
}
