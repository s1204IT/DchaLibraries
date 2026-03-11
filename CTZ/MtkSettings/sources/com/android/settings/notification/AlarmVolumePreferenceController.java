package com.android.settings.notification;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;

public class AlarmVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private static final String KEY_ALARM_VOLUME = "alarm_volume";

    public AlarmVolumePreferenceController(Context context) {
        super(context, KEY_ALARM_VOLUME);
    }

    @Override
    public int getAvailabilityStatus() {
        return (!this.mContext.getResources().getBoolean(R.bool.config_show_alarm_volume) || this.mHelper.isSingleVolume()) ? 2 : 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_ALARM_VOLUME);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALARM_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return 4;
    }

    @Override
    public int getMuteIcon() {
        return android.R.drawable.day_picker_week_view_dayline_holo;
    }
}
