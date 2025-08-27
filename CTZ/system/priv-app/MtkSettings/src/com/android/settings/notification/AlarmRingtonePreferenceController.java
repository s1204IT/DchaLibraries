package com.android.settings.notification;

import android.content.Context;

/* loaded from: classes.dex */
public class AlarmRingtonePreferenceController extends RingtonePreferenceControllerBase {
    public AlarmRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "alarm_ringtone";
    }

    @Override // com.android.settings.notification.RingtonePreferenceControllerBase
    public int getRingtoneType() {
        return 4;
    }
}
