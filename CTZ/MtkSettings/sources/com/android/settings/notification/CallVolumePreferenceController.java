package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import com.android.settings.R;

public class CallVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private AudioManager mAudioManager;

    public CallVolumePreferenceController(Context context, String str) {
        super(context, str);
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return (!this.mContext.getResources().getBoolean(R.bool.config_show_call_volume) || this.mHelper.isSingleVolume()) ? 2 : 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "call_volume");
    }

    @Override
    public int getAudioStream() {
        if (this.mAudioManager.isBluetoothScoOn()) {
            return 6;
        }
        return 0;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_local_phone_24_lib;
    }
}
