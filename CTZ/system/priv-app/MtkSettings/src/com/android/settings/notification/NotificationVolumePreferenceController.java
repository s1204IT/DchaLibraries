package com.android.settings.notification;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class NotificationVolumePreferenceController extends RingVolumePreferenceController {
    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";

    public NotificationVolumePreferenceController(Context context) {
        super(context, KEY_NOTIFICATION_VOLUME);
    }

    @Override // com.android.settings.notification.RingVolumePreferenceController, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return (!this.mContext.getResources().getBoolean(R.bool.config_show_notification_volume) || Utils.isVoiceCapable(this.mContext) || this.mHelper.isSingleVolume()) ? 2 : 0;
    }

    @Override // com.android.settings.notification.RingVolumePreferenceController, com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_NOTIFICATION_VOLUME);
    }

    @Override // com.android.settings.notification.RingVolumePreferenceController, com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_VOLUME;
    }

    @Override // com.android.settings.notification.RingVolumePreferenceController, com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getAudioStream() {
        return 5;
    }

    @Override // com.android.settings.notification.RingVolumePreferenceController, com.android.settings.notification.VolumeSeekBarPreferenceController
    public int getMuteIcon() {
        return R.drawable.ic_notifications_off_24dp;
    }
}
