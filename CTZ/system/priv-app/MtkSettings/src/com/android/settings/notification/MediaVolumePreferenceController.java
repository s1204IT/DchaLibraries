package com.android.settings.notification;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;

/* loaded from: classes.dex */
public class MediaVolumePreferenceController extends VolumeSeekBarPreferenceController {
    private static final String KEY_MEDIA_VOLUME = "media_volume";

    public MediaVolumePreferenceController(Context context) {
        super(context, KEY_MEDIA_VOLUME);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_media_volume)) {
            return 0;
        }
        return 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_MEDIA_VOLUME);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_MEDIA_VOLUME;
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getAudioStream() {
        return 3;
    }

    @Override // com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getMuteIcon() {
        return R.drawable.ic_media_stream_off;
    }
}
