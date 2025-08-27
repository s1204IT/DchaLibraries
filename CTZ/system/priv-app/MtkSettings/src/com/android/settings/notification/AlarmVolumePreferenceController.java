package com.android.settings.notification;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;

/* loaded from: classes.dex */
public class AlarmVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private static final String KEY_ALARM_VOLUME = "alarm_volume";

    public AlarmVolumePreferenceController(Context context) {
        super(context, KEY_ALARM_VOLUME);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return (!this.mContext.getResources().getBoolean(R.bool.config_show_alarm_volume) || this.mHelper.isSingleVolume()) ? 2 : 0;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_ALARM_VOLUME);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_ALARM_VOLUME;
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getAudioStream() {
        return 4;
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getMuteIcon() {
        return android.R.drawable.dropdown_ic_arrow_focused_holo_dark;
    }
}
